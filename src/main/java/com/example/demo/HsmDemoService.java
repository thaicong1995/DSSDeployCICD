//package com.example.demo;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class HsmDemoService {
//
//    private final ExecutorService hsmExecutor;
//
//    public String performSigning(String requestId) {
//        long startTime = System.currentTimeMillis();
//
//        log.info("Tomcat Thread [{}] nhận request {} - Submit vào HSM Pool",
//                Thread.currentThread().getName(), requestId);
//
//        Future<String> future = hsmExecutor.submit(() -> {
//            String workerThread = Thread.currentThread().getName();
//            long queueWaitTime = System.currentTimeMillis() - startTime;
//
//            log.info("HSM Worker [{}] bắt đầu xử lý request {}. Chờ queue: {}ms",
//                    workerThread, requestId, queueWaitTime);
//
//            try {
//                // Simulate getSlot() + signing time (thay bằng code Luna thật của bạn)
//                Thread.sleep(180);   // Giả lập 180ms ký số
//
//                log.info("HSM Worker [{}] HOÀN THÀNH request {} trong {}ms tổng",
//                        workerThread, requestId, System.currentTimeMillis() - startTime);
//
//                return "Signed-" + requestId;
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                log.error("HSM Worker bị interrupt", e);
//                throw new RuntimeException(e);
//            }
//        });
//
//        try {
//            // Timeout 10 giây
//            return future.get(10, TimeUnit.SECONDS);
//        } catch (TimeoutException e) {
//            log.error("TIMEOUT - Request {} sau 10 giây. Queue đang đầy!", requestId, e);
//            future.cancel(true);
//            throw new RuntimeException("HSM Timeout");
//        } catch (Exception e) {
//            log.error("LỖI khi thực hiện signing request {}", requestId, e);
//            throw new RuntimeException(e);
//        }
//    }
//}