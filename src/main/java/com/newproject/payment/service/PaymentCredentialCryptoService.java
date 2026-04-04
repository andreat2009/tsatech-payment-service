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
    private final String datasourcePassword;
    private final String datasourceUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PaymentCredentialCryptoService(
        @Value("${payment.config.master-key:}") String explicitMasterKey,
        @Value("${spring.datasource.password:}") String datasourcePassword,
        @Value("${spring.datasource.url:}") String datasourceUrl
    ) {
        this.explicitMasterKey = explicitMasterKey;
        this.datasourcePassword = datasourcePassword;
        this.datasourceUrl = datasourceUrl;
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
            String[] parts = encryptedValue.split(":", 3);
            if (parts.length != 3) {
                throw new IllegalStateException("Invalid encrypted credential payload");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] payload = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, resolveKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to decrypt payment provider credentials", ex);
        }
    }

    public String keySource() {
        return hasText(explicitMasterKey) ? "master-key" : "db-password-derived";
    }

    private SecretKeySpec resolveKey() {
        try {
            byte[] keyMaterial;
            if (hasText(explicitMasterKey)) {
                keyMaterial = decodeKey(explicitMasterKey.trim());
            } else if (hasText(datasourcePassword)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyMaterial = digest.digest(("tsa-payment-config|" + datasourceUrl + '|' + datasourcePassword).getBytes(StandardCharsets.UTF_8));
            } else {
                throw new IllegalStateException("No master key or datasource password available for payment credential encryption");
            }
            return new SecretKeySpec(Arrays.copyOf(keyMaterial, 32), "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to resolve payment credential encryption key", ex);
        }
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
