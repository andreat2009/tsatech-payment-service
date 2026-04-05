package com.newproject.payment.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PaymentCredentialCryptoServiceTest {

    @Test
    void encryptsAndDecryptsWithExplicitMasterKey() {
        PaymentCredentialCryptoService service = new PaymentCredentialCryptoService(
            "explicit-master-key-1234567890",
            "",
            "",
            "",
            "jdbc:postgresql://db/payment",
            ""
        );

        String encrypted = service.encrypt("secret-value");

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("enc:v1:"));
        assertEquals("secret-value", service.decrypt(encrypted));
        assertEquals("master-key", service.keySource());
    }

    @Test
    void fallsBackToDbPasswordEnvWhenDatasourcePasswordPropertyIsBlank() {
        PaymentCredentialCryptoService service = new PaymentCredentialCryptoService(
            "",
            "",
            "",
            "db-password-from-env",
            "",
            "jdbc:postgresql://db/payment"
        );

        String encrypted = service.encrypt("secret-value");

        assertEquals("secret-value", service.decrypt(encrypted));
        assertEquals("db-password-derived", service.keySource());
    }

    @Test
    void failsClearlyWhenNoKeyMaterialIsAvailable() {
        PaymentCredentialCryptoService service = new PaymentCredentialCryptoService(
            "",
            "",
            "",
            "",
            "",
            ""
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.encrypt("secret-value"));
        assertTrue(ex.getMessage().contains("Unable to resolve payment credential encryption key")
            || ex.getMessage().contains("Unable to encrypt payment provider credentials"));
        assertEquals("missing", service.keySource());
    }
}
