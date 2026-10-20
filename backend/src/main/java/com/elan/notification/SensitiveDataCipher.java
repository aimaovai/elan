package com.elan.notification;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SensitiveDataCipher {
    private final String configuredKey;

    public SensitiveDataCipher(@Value("${elan.email.encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey == null ? "" : configuredKey.trim();
        if (!this.configuredKey.isBlank()) {
            try {
                key();
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "ELAN_NOTIFICATION_ENCRYPTION_KEY must be base64-encoded 32-byte key material.", exception);
            }
        }
    }

    public byte[] encrypt(String plaintext) {
        if (configuredKey.isBlank() || plaintext == null || plaintext.isBlank()) return null;
        try {
            byte[] nonce = new byte[12];
            new java.security.SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to protect notification data.", exception);
        }
    }

    public Optional<String> decrypt(byte[] value) {
        if (value == null || value.length < 29 || configuredKey.isBlank()) return Optional.empty();
        try {
            ByteBuffer input = ByteBuffer.wrap(value);
            byte[] nonce = new byte[12];
            input.get(nonce);
            byte[] encrypted = new byte[input.remaining()];
            input.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            return Optional.of(new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read protected notification data.", exception);
        }
    }

    private SecretKeySpec key() {
        byte[] decoded = Base64.getDecoder().decode(configuredKey);
        if (decoded.length != 32) throw new IllegalArgumentException("Expected exactly 32 bytes.");
        return new SecretKeySpec(decoded, "AES");
    }
}
