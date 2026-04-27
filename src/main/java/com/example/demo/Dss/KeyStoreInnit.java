package com.example.demo.Dss;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.security.KeyStoreException;

@Component
public class KeyStoreInnit {
    @Bean("pkcs12")
    public KeyStore initKeyStore() throws KeyStoreException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        return ks;
    }
}
