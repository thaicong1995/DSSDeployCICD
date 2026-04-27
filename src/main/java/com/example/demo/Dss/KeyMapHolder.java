package com.example.demo.Dss;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Component
@Getter
public class KeyMapHolder {
    private volatile Map<String, PrivateKey> privateKeys = Map.of();
    private volatile Map<String, X509Certificate> certificates = Map.of();

    public void replaceAll(Map<String, PrivateKey> keys, Map<String, X509Certificate> certs) {
        this.privateKeys = Map.copyOf(new HashMap<>(keys));
        this.certificates = Map.copyOf(new HashMap<>(certs));
    }

    public PrivateKey get(String alias) {
        PrivateKey privateKey = privateKeys.get(alias);
        if (privateKey != null) {
            return privateKey;
        }
        throw new IllegalArgumentException("Not found private key by alias: " + alias);
    }

    public X509Certificate getCertificate(String alias) {
        X509Certificate certificate = certificates.get(alias);
        if (certificate != null) {
            return certificate;
        }
        throw new IllegalArgumentException("Not found certificate by alias: " + alias);
    }
}
