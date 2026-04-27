package com.example.demo.Dss;

import lombok.AllArgsConstructor;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

@AllArgsConstructor
public final class Pkcs12Utils {

    private KeyStore store;
    private Pkcs12Utils() {
    }

    public static LoadedPkcs12 load(String p12Path, String storePassword, String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream is = Pkcs12Utils.class.getClassLoader().getResourceAsStream(p12Path)) {
            if (is == null) {
                throw new IllegalArgumentException("Khong tim thay file: " + p12Path);
            }
            keyStore.load(is, storePassword.toCharArray());
        }

        String actualAlias = alias;
        if (actualAlias == null || actualAlias.isBlank()) {
            actualAlias = findFirstKeyEntryAlias(keyStore);
        }

        if (actualAlias == null || !keyStore.containsAlias(actualAlias)) {
            throw new IllegalArgumentException("Khong tim thay alias: " + actualAlias);
        }

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(actualAlias, storePassword.toCharArray());
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(actualAlias);

        if (privateKey == null) {
            throw new IllegalArgumentException("Private key null voi alias: " + actualAlias);
        }
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate null voi alias: " + actualAlias);
        }

        return new LoadedPkcs12(actualAlias, privateKey, certificate);
    }

    private static String findFirstKeyEntryAlias(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String a = aliases.nextElement();
            if (keyStore.isKeyEntry(a)) {
                return a;
            }
        }
        return null;
    }

    public record LoadedPkcs12(String alias, PrivateKey privateKey, X509Certificate certificate) {
    }
}
