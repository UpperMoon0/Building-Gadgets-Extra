package com.nstut.buildinggadgetsextra.common;

import java.nio.file.Path;

/** Client-side destination retained only while a structure Save response is expected. */
public final class PendingSaveTarget {
    private static final long TIMEOUT_MILLIS = 30_000L;

    private final Path path;
    private final long createdAt;

    public PendingSaveTarget(Path path) {
        this(path, System.currentTimeMillis());
    }

    PendingSaveTarget(Path path, long createdAt) {
        this.path = path;
        this.createdAt = createdAt;
    }

    public Path path() {
        return path;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > TIMEOUT_MILLIS;
    }
}
