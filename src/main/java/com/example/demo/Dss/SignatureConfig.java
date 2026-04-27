//package com.example.demo.Dss;
//
//import eu.europa.esig.dss.pades.signature.PAdESService;
//import eu.europa.esig.dss.pdf.pdfbox.PdfBoxDefaultObjectFactory;
//import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class SignatureConfig {
//
//    @Bean
//    public CommonCertificateVerifier certificateVerifier() {
//        CommonCertificateVerifier verifier = new CommonCertificateVerifier();
//        verifier.setCheckRevocationForUntrustedChains(false);
//        verifier.setCrlSource(null);
//        verifier.setOcspSource(null);
//        return verifier;
//    }
//
//    @Bean
//    public PAdESService padesService(CommonCertificateVerifier verifier) {
//        PAdESService service = new PAdESService(verifier);
//        service.setPdfObjFactory(new PdfBoxDefaultObjectFactory());
//        return service;
//    }
//}
