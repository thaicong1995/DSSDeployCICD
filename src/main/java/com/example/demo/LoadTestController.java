//package com.example.demo;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.concurrent.atomic.AtomicLong;
//
//@RestController
//@RequestMapping("/api/test")
//@RequiredArgsConstructor
//@Slf4j
//public class LoadTestController {
//
//    private final HsmDemoService hsmDemoService;
//    private final AtomicLong requestCounter = new AtomicLong(0);
//
//    @GetMapping("/sign")
//    public ResponseEntity<String> testSigning() {
//        long reqId = requestCounter.incrementAndGet();
//        String requestId = "REQ-" + reqId;
//
//        try {
//            String result = hsmDemoService.performSigning(requestId);
//            return ResponseEntity.ok(result);
//        } catch (Exception e) {
//            log.error("Controller error for {}", requestId, e);
//            return ResponseEntity.status(503).body("HSM Overload / Timeout");
//        }
//    }
//}