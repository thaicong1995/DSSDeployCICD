//package com.example.demo.AsyncConfig;
//
//import org.springframework.stereotype.Service;
//
//@Service
//public class DemoService {
//
//    public String doWork(String requestId) {
//        log("START doWork", requestId);
//        sleep(3000); // giả lập tác vụ chậm 3s
//        log("END doWork", requestId);
//        return "done-" + requestId;
//    }
//
//    private void sleep(long ms) {
//        try {
//            Thread.sleep(ms);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("Interrupted", e);
//        }
//    }
//
//    private void log(String phase, String requestId) {
//        System.out.printf(
//                "[%s] thread=%s requestId=%s%n",
//                phase,
//                Thread.currentThread().getName(),
//                requestId
//        );
//    }
//}