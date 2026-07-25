package com.company.intelligentdiagnosis.agent.infrastructure.repository.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密字符串转换器
 * 使用 AES/GCM 算法对敏感字符串进行加密存储，确保仓库凭证安全
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);

    /**
     * 加密算法：AES/GCM/NoPadding
     */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /**
     * GCM IV 长度（字节）
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * GCM 认证标签长度（位）
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * AES 密钥长度（字节）
     */
    private static final int KEY_LENGTH = 32;

    /**
     * 环境变量名称，用于获取加密密钥
     */
    private static final String KEY_ENV = "REPOSITORY_ENCRYPTION_KEY";

    /**
     * 加密密钥（静态初始化）
     */
    private static final SecretKey SECRET_KEY;

    static {
        String key = System.getenv(KEY_ENV);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                "Repository encryption key is required. Set REPOSITORY_ENCRYPTION_KEY environment variable."
            );
        }
        byte[] keyBytes = deriveKey(key).getBytes(StandardCharsets.UTF_8);
        SECRET_KEY = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        return encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        try {
            return decrypt(dbData);
        } catch (IllegalArgumentException e) {
            return dbData;
        } catch (Exception e) {
            log.warn("Failed to decrypt repository secret, returning null", e);
            return null;
        }
    }

    /**
     * 加密明文
     *
     * @param plainText 明文
     * @return 加密后的 Base64 字符串
     */
    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt repository secret", e);
        }
    }

    /**
     * 解密密文
     *
     * @param cipherText Base64 编码的密文
     * @return 解密后的明文
     */
    private String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt repository secret", e);
        }
    }

    /**
     * 派生密钥，确保密钥长度为 32 字节
     *
     * @param key 原始密钥
     * @return 标准化后的密钥
     */
    private static String deriveKey(String key) {
        String normalized = key.trim();
        if (normalized.length() < KEY_LENGTH) {
            return String.format("%-32s", normalized).replace(' ', '0');
        }
        return normalized.substring(0, KEY_LENGTH);
    }
}
