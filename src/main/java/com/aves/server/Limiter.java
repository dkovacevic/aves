package com.aves.server;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Limiter {
    private static final ConcurrentHashMap<String, Rates> paths = new ConcurrentHashMap<>();

    public static boolean rate(String path, UUID userId, int limit) {
        final Rates rates = paths.computeIfAbsent(path, x -> new Rates());
        final Requests req = rates.computeIfAbsent(userId, x -> new Requests());

        if (req.isClear()) {
            rates.remove(userId);
            return false;
        }

        return req.incrementAndCheck(limit);
    }

    private static class Requests {
        private final Date date = new Date();
        private final AtomicInteger counter = new AtomicInteger();

        boolean isClear() {
            final Date now = new Date();
            final long elapsed = now.getTime() - date.getTime();
            return elapsed > TimeUnit.SECONDS.toMillis(60);
        }

        boolean incrementAndCheck(int limit) {
            return counter.incrementAndGet() > limit;
        }
    }

    private static class Rates extends ConcurrentHashMap<UUID, Requests> {

    }
}
