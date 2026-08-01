package com.company.intelligentdiagnosis.agent.infrastructure.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

/**
 * 本地物理快照存储
 * 为每个仓库的快照提供独立的目录和文件读写能力
 */
@Component
public class BackupStorage {

    private static final Logger log = LoggerFactory.getLogger(BackupStorage.class);

    private final BackupStorageProperties properties;

    public BackupStorage(BackupStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取某个快照的备份目录
     */
    public Path resolveSnapshotDir(String repositoryName, String snapshotId) {
        Path dir = properties.getRootPath()
            .resolve(sanitize(repositoryName))
            .resolve(sanitize(snapshotId));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create backup directory: " + dir, e);
        }
        return dir;
    }

    /**
     * 将文本内容写入备份目录中的指定文件
     */
    public Path writeString(String repositoryName, String snapshotId, String fileName, String content) {
        Path file = resolveSnapshotDir(repositoryName, snapshotId).resolve(fileName);
        try {
            Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Wrote backup file {} ({} bytes)", file, Files.size(file));
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write backup file: " + file, e);
        }
    }

    /**
     * 将字节内容写入备份目录中的指定文件
     */
    public Path writeBytes(String repositoryName, String snapshotId, String fileName, byte[] content) {
        Path file = resolveSnapshotDir(repositoryName, snapshotId).resolve(fileName);
        try {
            Files.write(file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Wrote backup file {} ({} bytes)", file, content.length);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write backup file: " + file, e);
        }
    }

    /**
     * 读取备份目录中的指定文件内容
     */
    public String readString(String repositoryName, String snapshotId, String fileName) {
        Path file = resolveSnapshotDir(repositoryName, snapshotId).resolve(fileName);
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read backup file: " + file, e);
        }
    }

    /**
     * 读取备份目录中的指定文件为输入流
     */
    public InputStream read(String repositoryName, String snapshotId, String fileName) {
        Path file = resolveSnapshotDir(repositoryName, snapshotId).resolve(fileName);
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read backup file: " + file, e);
        }
    }

    /**
     * 判断备份目录中是否存在指定文件
     */
    public boolean exists(String repositoryName, String snapshotId, String fileName) {
        return resolveSnapshotDir(repositoryName, snapshotId).resolve(fileName).toFile().exists();
    }

    /**
     * 列出某个仓库的所有快照备份目录
     */
    public Stream<Path> listSnapshotDirs(String repositoryName) {
        Path repoDir = properties.getRootPath().resolve(sanitize(repositoryName));
        if (!Files.isDirectory(repoDir)) {
            return Stream.empty();
        }
        try {
            return Files.list(repoDir).filter(Files::isDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list backup directories: " + repoDir, e);
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
