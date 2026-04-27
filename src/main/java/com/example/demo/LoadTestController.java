package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class LoadTestController {

    private static final int MAX_SECONDS = 300;
    private static final Pattern CPUSET_SEGMENT_PATTERN = Pattern.compile("(\\d+)(?:-(\\d+))?");

    @GetMapping("/cpu")
    public ResponseEntity<String> cpu(
            @RequestParam(defaultValue = "30") int seconds,
            @RequestParam(defaultValue = "0") int workers) {
        int boundedSeconds = Math.max(1, Math.min(seconds, MAX_SECONDS));
        int effectiveProcessors = detectEffectiveProcessors();
        int requestedWorkers = workers <= 0 ? effectiveProcessors : workers;
        int boundedWorkers = Math.max(1, Math.min(requestedWorkers, effectiveProcessors));

        Thread[] threads = new Thread[boundedWorkers];
        long stopAt = System.nanoTime() + boundedSeconds * 1_000_000_000L;

        for (int i = 0; i < boundedWorkers; i++) {
            threads[i] = new Thread(() -> burnCpu(stopAt), "cpu-load-" + i);
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResponseEntity.internalServerError().body("Interrupted while generating CPU load");
            }
        }

        return ResponseEntity.ok(
                "CPU load completed: " + boundedWorkers + " workers for " + boundedSeconds
                        + "s (effective pod CPUs=" + effectiveProcessors + ")");
    }

    private void burnCpu(long stopAt) {
        double value = 0.0001d;
        while (System.nanoTime() < stopAt) {
            value += Math.sqrt(value + 123.456d);
            if (value > 1_000_000d) {
                value = value / 3.0d;
            }
        }
    }

    private int detectEffectiveProcessors() {
        int runtimeProcessors = Math.max(1, Runtime.getRuntime().availableProcessors());
        int cpusetProcessors = readCpusetProcessors();
        double quotaProcessors = readCpuQuotaProcessors();

        int effectiveFromQuota = quotaProcessors > 0 ? Math.max(1, (int) Math.ceil(quotaProcessors)) : runtimeProcessors;
        int effective = Math.min(runtimeProcessors, effectiveFromQuota);
        if (cpusetProcessors > 0) {
            effective = Math.min(effective, cpusetProcessors);
        }
        return Math.max(1, effective);
    }

    private int readCpusetProcessors() {
        String cpuset = readFirstAvailable(
                "/sys/fs/cgroup/cpuset.cpus.effective",
                "/sys/fs/cgroup/cpuset/cpuset.cpus");
        if (cpuset == null || cpuset.isBlank()) {
            return -1;
        }

        int count = 0;
        for (String segment : cpuset.trim().split(",")) {
            Matcher matcher = CPUSET_SEGMENT_PATTERN.matcher(segment.trim());
            if (!matcher.matches()) {
                continue;
            }
            int start = Integer.parseInt(matcher.group(1));
            String endGroup = matcher.group(2);
            int end = endGroup == null ? start : Integer.parseInt(endGroup);
            count += Math.max(0, end - start + 1);
        }
        return count > 0 ? count : -1;
    }

    private double readCpuQuotaProcessors() {
        String cpuMax = readFirstAvailable("/sys/fs/cgroup/cpu.max");
        if (cpuMax != null && !cpuMax.isBlank()) {
            String[] parts = cpuMax.trim().split("\\s+");
            if (parts.length >= 2 && !"max".equals(parts[0])) {
                double quota = Double.parseDouble(parts[0]);
                double period = Double.parseDouble(parts[1]);
                if (quota > 0 && period > 0) {
                    return quota / period;
                }
            }
        }

        String quota = readFirstAvailable("/sys/fs/cgroup/cpu/cpu.cfs_quota_us");
        String period = readFirstAvailable("/sys/fs/cgroup/cpu/cpu.cfs_period_us");
        if (quota == null || period == null) {
            return -1;
        }

        long quotaValue = Long.parseLong(quota.trim());
        long periodValue = Long.parseLong(period.trim());
        if (quotaValue <= 0 || periodValue <= 0) {
            return -1;
        }
        return (double) quotaValue / periodValue;
    }

    private String readFirstAvailable(String... paths) {
        for (String path : paths) {
            try {
                return Files.readString(Path.of(path)).trim();
            } catch (IOException ignored) {
                // Fall back to the next cgroup path.
            }
        }
        return null;
    }
}
