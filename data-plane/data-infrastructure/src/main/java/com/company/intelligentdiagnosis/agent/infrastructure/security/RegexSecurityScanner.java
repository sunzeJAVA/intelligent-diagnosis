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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于正则表达式的轻量级安全扫描器
 * <p>
 * 覆盖 OWASP Top 10 常见风险模式：硬编码凭证、SQL 注入、XSS、路径穿越、
 * 弱加密、危险反序列化/eval、不安全 TLS、日志泄露、XXE、开放重定向等。
 */
@Component
public class RegexSecurityScanner implements SecurityScanner {

    private static final Logger log = LoggerFactory.getLogger(RegexSecurityScanner.class);

    /** 仅扫描源代码与配置文件，跳过二进制/资源文件 */
    private static final Set<String> SCANNABLE_EXTENSIONS = Set.of(
        ".java", ".kt", ".scala", ".groovy",
        ".js", ".ts", ".jsx", ".tsx", ".vue", ".mjs",
        ".py", ".rb", ".php", ".go", ".rs", ".c", ".h", ".cpp", ".cc", ".hpp",
        ".cs", ".swift",
        ".xml", ".yaml", ".yml", ".json", ".properties", ".conf", ".toml",
        ".sql", ".sh", ".bash", ".env", ".cfg", ".ini"
    );

    /** 跳过超过 1MB 的文件，避免性能问题 */
    private static final long MAX_FILE_SIZE = 1L * 1024 * 1024;

    private final List<ScanRule> rules = List.of(
        new ScanRule(
            "HARDCODED_SECRET",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)(password|passwd|pwd|api[_-]?key|secret|token|access[_-]?key|private[_-]?key|client[_-]?secret)\\s*[=:]\\s*[\"'][^\"']{4,}[\"']"),
            "检测到可能的硬编码凭证或密钥"
        ),
        new ScanRule(
            "SQL_INJECTION",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)(executeQuery|executeUpdate|execute|createStatement|prepareStatement)\\s*\\(\\s*[^)]*\\+"),
            "检测到可能的 SQL 注入风险（字符串拼接 SQL）"
        ),
        new ScanRule(
            "XSS_REFLECTED",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)(innerHTML|outerHTML|document\\.write|\\.html\\()\\s*\\(\\s*[^)\"']*(\\$\\{|request|param|input)"),
            "检测到可能的 XSS 风险（用户输入直接写入 HTML）"
        ),
        new ScanRule(
            "PATH_TRAVERSAL",
            SecurityIssue.Severity.MEDIUM,
            Pattern.compile("(\\.\\.[/\\\\]){2,}|File\\s*\\(\\s*[^)]*\\+\\s*(request|param|input|args)"),
            "检测到可能的路径穿越风险"
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
            Pattern.compile("(?i)MessageDigest\\.getInstance\\s*\\(\\s*[\"'](MD5|SHA-?1|SHA-?0)[\"']\\s*\\)"),
            "检测到弱哈希算法（MD5/SHA1），建议升级至 SHA-256 或更高"
        ),
        new ScanRule(
            "WEAK_CRYPTO",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)(Cipher\\.getInstance\\s*\\(\\s*[\"'](DES|AES/ECB|RSA/ECB/PKCS1|RC4|Blowfish)[\"']|KeyPairGenerator\\.getInstance\\s*\\(\\s*[\"']DSA[\"']\\s*,?\\s*\\d{0,4})"),
            "检测到弱加密算法或 ECB 模式，建议使用 AES/GCM"
        ),
        new ScanRule(
            "DANGEROUS_EVAL",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)\\b(eval|Function|ScriptEngine|new\\s+Function)\\s*\\(\\s*[^)\"']*(\\$\\{|request|param|input|args|req\\.)"),
            "检测到危险动态代码执行（eval/Function 含用户输入）"
        ),
        new ScanRule(
            "HTTP_WITHOUT_TLS",
            SecurityIssue.Severity.LOW,
            Pattern.compile("(?i)https?://(?!localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0|schemas?\\.|www\\.w3\\.org|xmlns)"),
            "检测到非 TLS 的 HTTP 外部 URL，生产环境建议使用 HTTPS"
        ),
        new ScanRule(
            "LOG_SENSITIVE",
            SecurityIssue.Severity.MEDIUM,
            Pattern.compile("(?i)(log(?:ger)?|System\\.out|print(?:ln)?)\\s*[.(]\\s*[^)]*(password|passwd|secret|token|api[_-]?key|credential)"),
            "检测到敏感信息可能被记录到日志"
        ),
        new ScanRule(
            "TRUST_MANAGER_DISABLED",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)(X509TrustManager\\s*\\{\\s*[^}]*getAcceptedIssuers\\s*\\(\\s*\\)\\s*\\{\\s*return\\s*null|TrustAllCerts|setHostnameVerifier\\s*\\(\\s*(?:x|host|url)\\s*->\\s*true|ALLOW_ALL_HOSTNAME_VERIFIER)"),
            "检测到 TLS 证书校验被禁用（信任所有证书），存在中间人攻击风险"
        ),
        new ScanRule(
            "XXE_VULNERABLE",
            SecurityIssue.Severity.HIGH,
            Pattern.compile("(?i)(DocumentBuilderFactory|SAXParserFactory|XMLInputFactory)\\s*\\."),
            "检测到 XML 解析器使用，需确认已禁用外部实体（XXE 防护）"
        ),
        new ScanRule(
            "OPEN_REDIRECT",
            SecurityIssue.Severity.MEDIUM,
            Pattern.compile("(?i)(sendRedirect|setHeader\\s*\\(\\s*[\"']Location[\"']|ResponseEntity.*Location)\\s*[=,(]\\s*[^)\"]*(request|param|input|\\$\\{)"),
            "检测到可能的开放重定向风险（重定向 URL 来自用户输入）"
        ),
        new ScanRule(
            "HARDCODED_IP",
            SecurityIssue.Severity.LOW,
            Pattern.compile("(?i)(?<![\\d.])(?:10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.(?:1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3})(?![\\d])"),
            "检测到硬编码内网 IP 地址，建议通过配置注入"
        ),
        new ScanRule(
            "DEBUG_OUTPUT",
            SecurityIssue.Severity.LOW,
            Pattern.compile("System\\.out\\.print(?:ln)?\\s*\\(|console\\.log\\s*\\("),
            "检测到调试输出（System.out/console.log），生产环境建议移除"
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
            log.debug("Skipping security scan for non-existent file: {}", filePath);
            return;
        }
        if (!isScannable(filePath)) {
            return;
        }
        try {
            if (Files.size(filePath) > MAX_FILE_SIZE) {
                log.debug("Skipping security scan for large file (>{}) : {}", MAX_FILE_SIZE, filePath);
                return;
            }
        } catch (IOException e) {
            log.debug("Cannot determine file size, skipping: {}", filePath);
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

    private boolean isScannable(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return SCANNABLE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
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
