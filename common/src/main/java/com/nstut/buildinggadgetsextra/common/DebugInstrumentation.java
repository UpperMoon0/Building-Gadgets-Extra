package com.nstut.buildinggadgetsextra.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader-independent, bounded debug instrumentation with per-category token-bucket rate limiting.
 * Callers provide the actual logger sink so common code stays independent of Minecraft/logging APIs.
 */
public final class DebugInstrumentation {
    public static final int BURST = 4;
    public static final double REFILL_PER_SECOND = 2.0D;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final int MAX_CATEGORIES = 64;
    private static final String OVERFLOW_CATEGORY = "overflow";
    private static final Map<String, Bucket> BUCKETS = new ConcurrentHashMap<>();

    private DebugInstrumentation() {}

    public static void log(boolean enabled, String category, Supplier<String> message, Consumer<String> sink) {
        if (!enabled) return;
        Permit permit = acquire(category, System.nanoTime());
        if (!permit.allowed) return;

        StringBuilder line = new StringBuilder(96)
                .append("[BGE instrumentation][")
                .append(category)
                .append("] ")
                .append(message.get());
        if (permit.suppressed > 0) {
            line.append(" suppressedSinceLastLog=").append(permit.suppressed);
        }
        sink.accept(line.toString());
    }

    static Permit acquire(String requestedCategory, long nowNanos) {
        String category = requestedCategory == null || requestedCategory.trim().isEmpty()
                ? "uncategorized" : requestedCategory;
        if (!BUCKETS.containsKey(category) && BUCKETS.size() >= MAX_CATEGORIES) {
            category = OVERFLOW_CATEGORY;
        }
        Bucket bucket = BUCKETS.computeIfAbsent(category, ignored -> new Bucket(nowNanos));
        return bucket.acquire(nowNanos);
    }

    static void resetForTests() {
        BUCKETS.clear();
    }

    static final class Permit {
        final boolean allowed;
        final int suppressed;

        Permit(boolean allowed, int suppressed) {
            this.allowed = allowed;
            this.suppressed = suppressed;
        }
    }

    private static final class Bucket {
        private double tokens = BURST;
        private long lastRefillNanos;
        private int suppressed;

        private Bucket(long nowNanos) {
            this.lastRefillNanos = nowNanos;
        }

        private synchronized Permit acquire(long nowNanos) {
            long elapsed = Math.max(0L, nowNanos - lastRefillNanos);
            if (elapsed > 0L) {
                tokens = Math.min(BURST, tokens + elapsed * REFILL_PER_SECOND / NANOS_PER_SECOND);
                lastRefillNanos = nowNanos;
            }
            if (tokens < 1.0D) {
                suppressed++;
                return new Permit(false, 0);
            }
            tokens -= 1.0D;
            int previouslySuppressed = suppressed;
            suppressed = 0;
            return new Permit(true, previouslySuppressed);
        }
    }
}
