package com.example.demo.Dss;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileOutputStream;

@RestController
@RequestMapping("/api/v1/sign")
public class DSSController {

    private final SignPdfService signPdfService;

    public DSSController(SignPdfService signPdfService) {
        this.signPdfService = signPdfService;
    }

    @PostMapping(
            "/pdf-bytes"
    )
    public ResponseEntity<?> signPdf(@RequestBody SignPdfRequest request) throws Exception {
        byte[] signedPdf = signPdfService.signAddSignature(
                request.getPdfBytes(),
                request.getProviderName(),
                request.getAlias(),
                request.getX(),
                request.getY(),
                request.getWidth(),
                request.getHeight(),
                request.getPage(),
                request.getDisplayText(),
                request.getContact(),
                request.getSignName(),
                request.getLocation(),
                request.getReason(),
                request.getImageBytes()
        );
//        try(FileOutputStream fos = new FileOutputStream("/home/thai/Downloads/aaa.pdf")){
//            fos.write(signedPdf);
//        }

        return ResponseEntity.ok(signedPdf);
    }
}
