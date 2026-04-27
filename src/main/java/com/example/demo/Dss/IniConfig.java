package com.example.demo.Dss;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class IniConfig {
    private static final String PASS = "123456";
    private static final String KEYSTORE_RESOURCE = "testsign.p12";
    private final  KeyStore keyStore;
    private final KeyMapHolder keyMapHolder;
    private final Environment environment;

    public IniConfig(@Qualifier("pkcs12")KeyStore keyStore, KeyMapHolder keyMapHolder, Environment environment) {
        this.keyStore = keyStore;
        this.keyMapHolder = keyMapHolder;
        this.environment = environment;
    }

    @PostConstruct
    public void initLoader() {
        try {
            try (var inputStream = new ClassPathResource(KEYSTORE_RESOURCE).getInputStream()) {
                keyStore.load(inputStream, PASS.toCharArray());
            }

            Map<String, String> configuredAliases = Binder.get(environment)
                    .bind("keystore", Bindable.mapOf(String.class, String.class))
                    .orElseGet(Map::of);

            Map<String, PrivateKey> loadedKeys = new HashMap<>();
            Map<String, X509Certificate> loadedCertificates = new HashMap<>();
            for (String alias : configuredAliases.values()) {
                if (!keyStore.containsAlias(alias)) {
                    throw new IllegalArgumentException("Alias not found in keystore: " + alias);
                }
                loadedKeys.put(alias, (PrivateKey) keyStore.getKey(alias, PASS.toCharArray()));
                loadedCertificates.put(alias, (X509Certificate) keyStore.getCertificate(alias));
            }

            keyMapHolder.replaceAll(loadedKeys, loadedCertificates);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize keystore", e);
        }
    }


}
