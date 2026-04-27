//package com.example.demo;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicInteger;
//
//@Configuration
//public class HsmExecutorConfig {
//
//    @Bean(name = "hsmExecutor", destroyMethod = "shutdown")
//    public ExecutorService hsmExecutor() {
//        ThreadPoolExecutor executor = new ThreadPoolExecutor(
//                60,                    // corePoolSize
//                120,                   // maxPoolSize
//                60L, TimeUnit.SECONDS,
//                new LinkedBlockingQueue<>(500),   // queue capacity
//                new ThreadFactory() {
//                    private final AtomicInteger counter = new AtomicInteger(0);
//                    @Override
//                    public Thread newThread(Runnable r) {
//                        Thread t = new Thread(r, "HSM-Worker-" + counter.incrementAndGet());
//                        t.setDaemon(true);
//                        return t;
//                    }
//                },
//                new ThreadPoolExecutor.CallerRunsPolicy()   // Quan trọng
//        );
//
//        // Monitor metrics
//        executor.setThreadFactory(new ThreadFactory() {
//            private final AtomicInteger counter = new AtomicInteger(0);
//            @Override
//            public Thread newThread(Runnable r) {
//                return new Thread(r, "HSM-Worker-" + counter.incrementAndGet());
//            }
//        });
//
//        return executor;
//    }
//}