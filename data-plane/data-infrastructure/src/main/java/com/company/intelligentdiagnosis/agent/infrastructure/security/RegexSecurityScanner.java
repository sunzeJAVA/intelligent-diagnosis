package com.company.intelligentdiagnosis.agent.infrastructure.security;

import com.company.intelligentdiagnosis.agent.domain.workflow.GitPushEvent;
import com.company.intelligentdiagnosis.agent.domain.workflow.SecurityIssue;
import com.company.intelligentdiagnosis.agent.domain.workflow.SecurityScanResult;
import com.company.intelligentdiagnosis.agent.domain.workflow.SecurityScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于正则表达式的轻量级安全扫描器
 */
@Component
public class RegexSecurityScanner implements SecurityScanner {

    private static final Logger log = LoggerFactory.getLogger(RegexSecurityScanner.class);

    private final List<ScanRule> rules = List.of(
        new ScanRule(
            "HARDCODED_SECRET",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)(password|passwd|pwd|api[_-]?key|secret|token|access[_-]?key)\\s*=\\s*[\"'][^\"']{4,}[\"']"),
            "检测到可能的硬编码凭证或密钥"
        ),
        new ScanRule(
            "SQL_INJECTION",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)(executeQuery|executeUpdate|execute|createStatement|prepareStatement)\\s*\\(\\s*[^)]*\\+"),
            "检测到可能的 SQL 注入风险（字符串拼接 SQL）"
        ),
        new ScanRule(
            "INSECURE_RANDOM",
            SecurityIssue.Severity.MEDIUM,
            Pattern.compile("(?<!\\w)new\\s+Random\\s*\\(", Pattern.CASE_INSENSITIVE),
            "使用 java.util.Random 生成安全敏感值，建议替换为 SecureRandom"
        ),
        new ScanRule(
            "WEAK_HASH",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)MessageDigest\\.getInstance\\s*\\(\\s*[\"'](MD5|SHA-1|SHA1)[\"']\\s*\\)"),
            "检测到弱哈希算法（MD5/SHA1），建议升级至 SHA-256 或更高"
        ),
        new ScanRule(
            "DEBUG_OUTPUT",
            SecurityIssue.Severity.LOW,
            Pattern.compile("System\\.out\\.print(?:ln)?\\s*\\("),
            "检测到 System.out.print 调试输出，生产环境建议移除"
        )
    );

    @Override
    public SecurityScanResult scan(GitPushEvent event) {
        if (event.changedFiles() == null || event.changedFiles().isEmpty()) {
            return SecurityScanResult.passed();
        }

        List<SecurityIssue> issues = new ArrayList<>();
        Path repoPath = event.repoPath() != null ? Paths.get(event.repoPath()) : Paths.get(".");

        for (String changedFile : event.changedFiles()) {
            scanFile(repoPath.resolve(changedFile), issues);
        }

        if (issues.isEmpty()) {
            return SecurityScanResult.passed();
        }

        long highCount = issues.stream().filter(i -> i.severity() == SecurityIssue.Severity.HIGH).count();
        String reason = String.format("发现 %d 个安全问题，其中 HIGH %d 个", issues.size(), highCount);
        return SecurityScanResult.failed(reason, issues);
    }

    private void scanFile(Path filePath, List<SecurityIssue> issues) {
        if (!Files.isRegularFile(filePath)) {
            log.warn("Skipping security scan for non-existent file: {}", filePath);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                for (ScanRule rule : rules) {
                    Matcher matcher = rule.pattern.matcher(line);
                    while (matcher.find()) {
                        issues.add(new SecurityIssue(
                            rule.ruleId,
                            rule.severity,
                            filePath.toString(),
                            i + 1,
                            rule.message,
                            truncate(matcher.group(), 120)
                        ));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read file for security scan {}: {}", filePath, e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private record ScanRule(
        String ruleId,
        SecurityIssue.Severity severity,
        Pattern pattern,
        String message
    ) {}
}
