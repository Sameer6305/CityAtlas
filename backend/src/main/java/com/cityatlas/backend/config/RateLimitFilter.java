package com.cityatlas.backend.config;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMIT_PER_MINUTE = 60;
    private static final long WINDOW_SECONDS = 60L;
    private static final long TRACK_TTL_SECONDS = 300L;
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final long CLEANUP_EVERY_N_REQUESTS = 200L;

    private final Map<String, Deque<Long>> requestsByIp = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeenByKey = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        if (!isRateLimitedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = Instant.now().getEpochSecond();
        String clientIp = resolveClientIp(request);
        String key = clientIp + "|" + path;

        runPeriodicCleanup(now);
        if (!requestsByIp.containsKey(key) && requestsByIp.size() >= MAX_TRACKED_KEYS) {
            cleanupStaleEntries(now);
            if (!requestsByIp.containsKey(key) && requestsByIp.size() >= MAX_TRACKED_KEYS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limiter capacity reached\"}");
                return;
            }
        }

        Deque<Long> timestamps = requestsByIp.computeIfAbsent(key, k -> new ArrayDeque<>());
        lastSeenByKey.put(key, now);

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_SECONDS) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= LIMIT_PER_MINUTE) {
                long retryAfter = WINDOW_SECONDS - (now - timestamps.peekFirst());
                // RATE LIMIT: 60 req/min per IP - adjust in RateLimitFilter
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(Math.max(retryAfter, 1)));
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
                return;
            }

            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path) {
        return path.startsWith("/weather/")
                || path.startsWith("/air-quality/")
                || path.startsWith("/cities/")
                || path.startsWith("/auth/");
    }

    private void runPeriodicCleanup(long nowEpochSec) {
        long count = requestCounter.incrementAndGet();
        if (count % CLEANUP_EVERY_N_REQUESTS == 0) {
            cleanupStaleEntries(nowEpochSec);
        }
    }

    private void cleanupStaleEntries(long nowEpochSec) {
        Iterator<Map.Entry<String, Long>> it = lastSeenByKey.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (nowEpochSec - entry.getValue() >= TRACK_TTL_SECONDS) {
                String key = entry.getKey();
                requestsByIp.remove(key);
                it.remove();
            }
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
