//package com.example.demo.AsyncConfig;
//
//import com.example.demo.AsyncConfig.DemoService;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.Executor;
//
//@RestController
//public class DemoController {
//
//    private final DemoService demoService;
//    private final Executor demoExecutor;
//
//    public DemoController(
//            DemoService demoService,
//            @Qualifier("demoExecutor") Executor demoExecutor
//    ) {
//        this.demoService = demoService;
//        this.demoExecutor = demoExecutor;
//    }
//
//    // 1) Đồng bộ hoàn toàn
//    @GetMapping("/sync")
//    public ResponseEntity<String> sync(
//            @RequestParam(defaultValue = "1") String id
//    ) {
//        log("SYNC controller START", id);
//
//        String result = demoService.doWork(id);
//
//        log("SYNC controller END", id);
//        return ResponseEntity.ok("sync -> " + result);
//    }
//
//    // 2) Có CompletableFuture nhưng vẫn join ngay
//    // => Tomcat thread vẫn chờ. Chỉ là chuyển việc sang thread pool khác rồi lại đợi.
//    @GetMapping("/cf-same-thread")
//    public ResponseEntity<String> cfSameThread(
//            @RequestParam(defaultValue = "1") String id
//    ) {
//        log("CF-SAME controller START", id);
//
//        CompletableFuture<String> future = CompletableFuture.supplyAsync(
//                () -> demoService.doWork(id),
//                demoExecutor
//        );
//
//        String result = future.join(); // Tomcat thread vẫn block ở đây
//
//        log("CF-SAME controller END", id);
//        return ResponseEntity.ok("cf-same-thread -> " + result);
//    }
//
//    // 3) Async thật ở tầng web
//    // Spring giữ request ở dạng async, Tomcat thread được nhả sớm hơn.
//    @GetMapping("/cf-async")
//    public CompletableFuture<ResponseEntity<String>> cfAsync(
//            @RequestParam(defaultValue = "1") String id
//    ) {
//        log("CF-ASYNC controller START", id);
//
//        return CompletableFuture
//                .supplyAsync(() -> demoService.doWork(id), demoExecutor)
//                .thenApply(result -> {
//                    log("CF-ASYNC controller END", id);
//                    return ResponseEntity.ok("cf-async -> " + result);
//                });
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