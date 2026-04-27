package com.example.demo.Dss;

import eu.europa.esig.dss.enumerations.*;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxDefaultObjectFactory;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.security.Signature;
import java.util.List;

// import ... Ex.BadRequest
// import ... SignatureFieldParameters
// import ... SignatureImageParameters
// import ... SignatureImageTextParameters

@Service
public class SignPdfServiceImpl  implements SignPdfService{

    private final PAdESService service;
    private final KeyMapHolder keyMapHolder;
    public SignPdfServiceImpl(KeyMapHolder keyMapHolder) {
        this.keyMapHolder = keyMapHolder;
        CommonCertificateVerifier verifier = new CommonCertificateVerifier();
        verifier.setCheckRevocationForUntrustedChains(false);
        verifier.setRevocationFallback(false);
        verifier.setAIASource(null);
        verifier.setCrlSource(null);
        verifier.setOcspSource(null);
        this.service = new PAdESService(verifier);
        this.service.setPdfObjFactory(new PdfBoxDefaultObjectFactory());
    }

    static {
        javax.imageio.ImageIO.setUseCache(false);
    }

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
    ) throws Exception {
        DSSDocument document = null;
        DSSDocument imageDoc = null;
        ToBeSigned dataToSign = null;
        DSSDocument signedDoc = null;

        try{
            CertificateToken signingCertificate = new CertificateToken(keyMapHolder.getCertificate(alias));
            List<CertificateToken> certTokens = List.of(signingCertificate);
            document = new InMemoryDocument(pdfByte);

            SignatureFieldParameters field = new SignatureFieldParameters();
            field.setPage(page);
            field.setOriginX(x);
            field.setOriginY(y);
            field.setHeight(hiegh);
            field.setWidth(width);

            SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
            SignatureImageParameters imageParameters = new SignatureImageParameters();
            String signerInfo = displayText;

            if (image != null && image.length > 0) {
                imageDoc = new InMemoryDocument(image, null, MimeTypeEnum.PNG);
                imageParameters.setFieldParameters(field);
                imageParameters.setImage(imageDoc);
            } else {
                textParameters.setText(signerInfo);
                textParameters.setTextColor(Color.BLACK);
                textParameters.setBackgroundColor(null);
                textParameters.setPadding(8);
                textParameters.setTextWrapping(TextWrapping.FONT_BASED);
                textParameters.setSignerTextPosition(SignerTextPosition.LEFT);
                textParameters.setSignerTextHorizontalAlignment(
                        SignerTextHorizontalAlignment.CENTER
                );
                textParameters.setSignerTextVerticalAlignment(
                        SignerTextVerticalAlignment.MIDDLE
                );

                imageParameters.setFieldParameters(field);
                imageParameters.setTextParameters(textParameters);
            }

            PAdESSignatureParameters parameters =
                    getPAdESSignatureParameters(certTokens, reason, contact, signName, location);
            parameters.setImageParameters(imageParameters);

            dataToSign = service.getDataToSign(document, parameters);

            Signature signature;
            if (providerName != null && !providerName.isBlank()) {
                signature = Signature.getInstance("SHA256withRSA", providerName);
            } else {
                signature = Signature.getInstance("SHA256withRSA");
            }
            signature.initSign(keyMapHolder.get(alias));
            signature.update(dataToSign.getBytes());
            byte[] signedValue = signature.sign();

            SignatureValue signatureValue =
                    new SignatureValue(SignatureAlgorithm.RSA_SHA256, signedValue);
             signedDoc = service.signDocument(document, parameters, signatureValue);
            return DSSUtils.toByteArray(signedDoc);
        }finally {
             document = null;
             imageDoc = null;
             dataToSign = null;
             signedDoc = null;
             pdfByte = null;
        }

    }

    private boolean isOverlap(Throwable ex) {
        while (ex != null) {
            if (ex.getMessage() != null && ex.getMessage().contains("overlaps")) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }

    private PAdESSignatureParameters getPAdESSignatureParameters(
            List<CertificateToken> certTokens,
            String reason,
            String contact,
            String signName,
            String location
    ) {
        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
        parameters.setSigningCertificate(certTokens.getFirst());
        parameters.setCertificateChain(certTokens);
        parameters.setSignerName(signName);
        parameters.setReason(reason);
        parameters.setLocation(location);
        parameters.setContactInfo(contact);
        return parameters;
    }

    private PAdESSignatureParameters getPAdESSignatureParameters(List<CertificateToken> certTokens) {
        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
        parameters.setSigningCertificate(certTokens.getFirst());
        parameters.setCertificateChain(certTokens);
        // phần còn lại bị khuất trong ảnh
        return parameters;
    }

}
