package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.AuthType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Git 仓库提供者
 * 实现 Git 仓库的克隆、拉取、变更检测等操作
 */
@Component
public class GitRepositoryProvider implements RepositoryProvider {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryProvider.class);

    /**
     * Git 凭证工厂
     */
    private final GitCredentialFactory credentialFactory;

    /**
     * 同步超时时间（秒）
     */
    @Value("${repository.sync.timeout-seconds:600}")
    private int timeoutSeconds;

    /**
     * 创建实例
     *
     * @param credentialFactory Git 凭证工厂
     */
    public GitRepositoryProvider(GitCredentialFactory credentialFactory) {
        this.credentialFactory = credentialFactory;
    }

    @Override
    public boolean supports(RepositoryType type) {
        return type == RepositoryType.GIT
            || type == RepositoryType.GITHUB
            || type == RepositoryType.GITLAB
            || type == RepositoryType.GITEE
            || type == RepositoryType.BITBUCKET;
    }

    @Override
    public RepositorySyncResult sync(RepositoryConfigEntity config) {
        Path localPath = Path.of(config.getLocalPath());
        CredentialsProvider credentials = credentialFactory.create(config);

        try {
            boolean freshClone = !Files.exists(localPath.resolve(".git"));
            String previousCommit = freshClone ? null : getLatestCommit(config);

            if (freshClone) {
                log.info("Cloning repository {} from {} to {}", config.getName(), config.getUrl(), localPath);
                cloneRepository(config, localPath, credentials);
            } else {
                log.info("Pulling repository {} at {}", config.getName(), localPath);
                pullRepository(config, localPath, credentials);
            }

            String latestCommit = getLatestCommit(config);
            List<String> changedFiles = detectChangedFiles(config, previousCommit, latestCommit);

            return new RepositorySyncResult(latestCommit, previousCommit, changedFiles, freshClone);
        } catch (Exception e) {
            throw new RepositorySyncException(
                "Failed to sync repository: " + config.getName(), e
            );
        }
    }

    @Override
    public List<String> detectChangedFiles(RepositoryConfigEntity config, String baseCommit, String headCommit) {
        if (baseCommit == null || baseCommit.isBlank()) {
            return List.of();
        }

        Path localPath = Path.of(config.getLocalPath());
        try (Repository repository = openRepository(localPath);
             RevWalk walk = new RevWalk(repository)) {

            ObjectId baseId = repository.resolve(baseCommit);
            ObjectId headId = repository.resolve(headCommit);
            if (baseId == null || headId == null) {
                return List.of();
            }

            RevCommit base = walk.parseCommit(baseId);
            RevCommit head = walk.parseCommit(headId);

            List<String> changedFiles = new ArrayList<>();
            var diffs = Git.wrap(repository).diff()
                .setOldTree(prepareTreeParser(walk, base))
                .setNewTree(prepareTreeParser(walk, head))
                .call();
            for (var diff : diffs) {
                if (diff.getNewPath() != null && !diff.getNewPath().equals("/dev/null")) {
                    changedFiles.add(diff.getNewPath());
                } else if (diff.getOldPath() != null) {
                    changedFiles.add(diff.getOldPath());
                }
            }
            return changedFiles;
        } catch (Exception e) {
            log.warn("Failed to detect changed files for repository {}: {}", config.getName(), e.getMessage());
            return List.of();
        }
    }

    @Override
    public String getLatestCommit(RepositoryConfigEntity config) {
        Path localPath = Path.of(config.getLocalPath());
        try (Repository repository = openRepository(localPath)) {
            ObjectId head = repository.resolve("HEAD");
            return head != null ? head.getName() : null;
        } catch (IOException e) {
            throw new RepositorySyncException(
                "Failed to get latest commit for repository: " + config.getName(), e
            );
        }
    }

    /**
     * 克隆仓库
     *
     * @param config      仓库配置
     * @param localPath   本地路径
     * @param credentials 凭证提供者
     */
    private void cloneRepository(RepositoryConfigEntity config, Path localPath, CredentialsProvider credentials) {
        boolean directoryExisted = Files.exists(localPath);
        var cloneCommand = Git.cloneRepository()
            .setURI(config.getUrl())
            .setDirectory(localPath.toFile())
            .setBranch(config.getBranch())
            .setCloneAllBranches(false)
            .setCloneSubmodules(false)
            .setTimeout(timeoutSeconds);

        SshSessionFactory sshFactory = configureAuthentication(cloneCommand, config, credentials);
        try (Git git = cloneCommand.call()) {
            try {
                ObjectId head = git.getRepository().resolve("HEAD");
                log.info("Successfully cloned repository {} at commit {}",
                    config.getName(), head != null ? head.getName() : "unknown");
            } catch (Exception e) {
                log.warn("Failed to resolve HEAD after cloning repository {}", config.getName(), e);
            }
        } catch (Exception e) {
            if (!directoryExisted) {
                deleteRecursively(localPath);
            }
            throw new RepositorySyncException("Failed to clone repository: " + config.getName(), e);
        } finally {
            closeQuietly(sshFactory);
        }
    }

    /**
     * 拉取仓库更新
     *
     * @param config      仓库配置
     * @param localPath   本地路径
     * @param credentials 凭证提供者
     */
    private void pullRepository(RepositoryConfigEntity config, Path localPath, CredentialsProvider credentials)
        throws GitAPIException, IOException {
        try (Git git = Git.open(localPath.toFile())) {
            var pullCommand = git.pull()
                .setRemote("origin")
                .setRemoteBranchName(config.getBranch())
                .setTimeout(timeoutSeconds);

            SshSessionFactory sshFactory = configureAuthentication(pullCommand, config, credentials);
            try {
                pullCommand.call();
            } finally {
                closeQuietly(sshFactory);
            }
        }
    }

    /**
     * 配置 Git 传输命令的认证方式
     *
     * @param command     Git 传输命令
     * @param config      仓库配置
     * @param credentials 凭证提供者
     * @return SSH 会话工厂，如果不需要则返回 null
     */
    private SshSessionFactory configureAuthentication(TransportCommand<?, ?> command,
                                                      RepositoryConfigEntity config,
                                                      CredentialsProvider credentials) {
        if (config.getAuthType() == AuthType.SSH_KEY) {
            SshSessionFactory factory = createSshSessionFactory(config);
            command.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport sshTransport) {
                    sshTransport.setSshSessionFactory(factory);
                }
            });
            return factory;
        }
        if (credentials != null) {
            command.setCredentialsProvider(credentials);
        }
        return null;
    }

    /**
     * 创建 SSH 会话工厂
     *
     * @param config 仓库配置
     * @return SSH 会话工厂
     */
    private SshSessionFactory createSshSessionFactory(RepositoryConfigEntity config) {
        String keyPath = config.getAuthSshKeyPath();
        if (keyPath == null || keyPath.isBlank()) {
            throw new RepositorySyncException(
                "SSH key path is required for SSH authentication: " + config.getName()
            );
        }

        Path resolvedKeyPath = Path.of(keyPath);
        if (!Files.exists(resolvedKeyPath)) {
            throw new RepositorySyncException(
                "SSH key file not found: " + resolvedKeyPath
            );
        }

        return new SshdSessionFactory() {
            @Override
            protected List<Path> getDefaultIdentities(File sshDir) {
                return List.of(resolvedKeyPath.toAbsolutePath());
            }
        };
    }

    /**
     * 安静关闭 SSH 会话工厂
     *
     * @param factory SSH 会话工厂
     */
    private void closeQuietly(SshSessionFactory factory) {
        if (factory instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.warn("Failed to close SSH session factory", e);
            }
        }
    }

    /**
     * 递归删除目录
     *
     * @param path 目录路径
     */
    private void deleteRecursively(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete {} during cleanup", p, e);
                        }
                    });
            }
        } catch (IOException e) {
            log.warn("Failed to clean up directory {}", path, e);
        }
    }

    /**
     * 打开 Git 仓库
     *
     * @param localPath 本地路径
     * @return Git 仓库对象
     */
    private Repository openRepository(Path localPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(localPath.resolve(".git").toFile())
            .readEnvironment()
            .findGitDir()
            .build();
    }

    /**
     * 准备树解析器
     *
     * @param walk   RevWalk 对象
     * @param commit 提交对象
     * @return 树解析器
     */
    private org.eclipse.jgit.treewalk.CanonicalTreeParser prepareTreeParser(RevWalk walk, RevCommit commit) {
        try {
            ObjectId treeId = commit.getTree().getId();
            org.eclipse.jgit.treewalk.CanonicalTreeParser treeParser = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
            var reader = walk.getObjectReader();
            treeParser.reset(reader, treeId);
            return treeParser;
        } catch (IOException e) {
            throw new RepositorySyncException("Failed to prepare tree parser", e);
        }
    }
}
