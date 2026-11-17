package com.wayfarer.travel.auth;

import com.wayfarer.travel.api.ApiFailure;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class AbuseGuard {
    private static final int MAX_BUCKETS = 20_000;
    private record Bucket(long expiresAt, int count) {}
    private final Map<String, Bucket> buckets = new HashMap<>();
    private long nextCleanup;

    public synchronized void take(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        if (now >= nextCleanup || buckets.size() >= MAX_BUCKETS) {
            buckets.values().removeIf(bucket -> bucket.expiresAt() <= now);
            nextCleanup = now + 60_000;
        }
        Bucket bucket = buckets.get(key);
        if (bucket == null || bucket.expiresAt() <= now) {
            if (bucket == null && buckets.size() >= MAX_BUCKETS) throw throttled();
            buckets.put(key, new Bucket(now + window.toMillis(), 1));
            return;
        }
        if (bucket.count() >= limit) throw throttled();
        buckets.put(key, new Bucket(bucket.expiresAt(), bucket.count() + 1));
    }

    private static ApiFailure throttled() {
        return new ApiFailure(429, "RATE_LIMIT", "操作频率过高，请稍后重试。", true);
    }
}
