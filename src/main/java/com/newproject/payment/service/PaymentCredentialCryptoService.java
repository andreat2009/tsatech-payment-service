package com.newproject.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentCredentialCryptoService {
    private static final String PREFIX = "enc:v1";

    private final String explicitMasterKey;
    private final String envMasterKey;
    private final String datasourcePassword;
    private final String envDatasourcePassword;
    private final String datasourceUrl;
    private final String envDatasourceUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PaymentCredentialCryptoService(
        @Value("${payment.config.master-key:}") String explicitMasterKey,
        @Value("${PAYMENT_CONFIG_MASTER_KEY:}") String envMasterKey,
        @Value("${spring.datasource.password:}") String datasourcePassword,
        @Value("${DB_PASSWORD:}") String envDatasourcePassword,
        @Value("${spring.datasource.url:}") String datasourceUrl,
        @Value("${SPRING_DATASOURCE_URL:}") String envDatasourceUrl
    ) {
        this.explicitMasterKey = explicitMasterKey;
        this.envMasterKey = envMasterKey;
        this.datasourcePassword = datasourcePassword;
        this.envDatasourcePassword = envDatasourcePassword;
        this.datasourceUrl = datasourceUrl;
        this.envDatasourceUrl = envDatasourceUrl;
    }

    public String encrypt(String plainText) {
        if (!hasText(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, resolveKey(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + ':' + Base64.getEncoder().encodeToString(iv) + ':' + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encrypt payment provider credentials", ex);
        }
    }

    public String decrypt(String encryptedValue) {
        if (!hasText(encryptedValue)) {
            return null;
        }
        if (!encryptedValue.startsWith(PREFIX + ':')) {
            return encryptedValue;
        }
        try {
            String encodedPayload = encryptedValue.substring((PREFIX + ':').length());
            String[] parts = encodedPayload.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("Invalid encrypted credential payload");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] payload = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, resolveKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to decrypt payment provider credentials", ex);
        }
    }

    public String keySource() {
        if (hasText(explicitMasterKey) || hasText(envMasterKey)) {
            return "master-key";
        }
        return hasText(datasourcePassword) || hasText(envDatasourcePassword) ? "db-password-derived" : "missing";
    }

    private SecretKeySpec resolveKey() {
        try {
            byte[] keyMaterial;
            String effectiveMasterKey = firstNonBlank(explicitMasterKey, envMasterKey);
            String effectiveDatasourcePassword = firstNonBlank(datasourcePassword, envDatasourcePassword);
            String effectiveDatasourceUrl = firstNonBlank(datasourceUrl, envDatasourceUrl, "jdbc:unknown");
            if (hasText(effectiveMasterKey)) {
                keyMaterial = decodeKey(effectiveMasterKey.trim());
            } else if (hasText(effectiveDatasourcePassword)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyMaterial = digest.digest(("tsa-payment-config|" + effectiveDatasourceUrl + '|' + effectiveDatasourcePassword).getBytes(StandardCharsets.UTF_8));
            } else {
                throw new IllegalStateException("No master key or datasource password available for payment credential encryption");
            }
            return new SecretKeySpec(Arrays.copyOf(keyMaterial, 32), "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to resolve payment credential encryption key", ex);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private byte[] decodeKey(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ignored) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
