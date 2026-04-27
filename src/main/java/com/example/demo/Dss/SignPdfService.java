package com.example.demo.Dss;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface SignPdfService {
    public byte[] signAddSignature(
            byte[] pdfByte,
            String providerName,
            String alias,
            float x,
            float y,
            float width,
            float hiegh,
            int page,
            String displayText,
            String contact,
            String signName,
            String location,
            String reason,
            byte[] image
    ) throws Exception;
}
