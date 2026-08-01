package com.company.intelligentdiagnosis.agent.infrastructure.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * 物理快照本地存储配置
 */
@ConfigurationProperties(prefix = "backup.storage")
public class BackupStorageProperties {

    /**
     * 本地备份根目录，默认放在应用工作目录下的 backups 文件夹
     */
    private String path = "backups";

    public Path getRootPath() {
        return Path.of(path).toAbsolutePath().normalize();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
