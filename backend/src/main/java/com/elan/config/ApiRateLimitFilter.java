package com.elan.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_ENTRIES = 25_000;
    private static final long RETAIN_MILLIS = 360_000;
    private record Window(long startsAt, int count) {}
    private record Limit(int requests, long windowMillis) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public ApiRateLimitFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limit limit = limitFor(request);
        if (limit == null || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        long now = System.currentTimeMillis();
        String key = request.getRemoteAddr() + ":" + bucket(request);
        if (!windows.containsKey(key) && windows.size() >= MAX_ENTRIES) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startsAt > RETAIN_MILLIS);
            if (!windows.containsKey(key) && windows.size() >= MAX_ENTRIES) key = "overflow:" + bucket(request);
        }
        String rateLimitKey = key;
        Window window = windows.compute(rateLimitKey, (ignored, current) -> {
            if (current == null || now - current.startsAt >= limit.windowMillis) return new Window(now, 1);
            return new Window(current.startsAt, current.count + 1);
        });
        if (window.count > limit.requests) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(Math.max(1,
                    (limit.windowMillis - (now - window.startsAt) + 999) / 1000)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), Map.of("code", "RATE_LIMITED",
                    "message", "Too many requests. Please wait and try again.", "timestamp", Instant.now().toString()));
            return;
        }
        chain.doFilter(request, response);
    }

    private Limit limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.equals("/api/v1/admin/auth/login") && "POST".equals(request.getMethod()))
            return new Limit(5, 60_000);
        if (path.equals("/api/v1/reservations") && "POST".equals(request.getMethod()))
            return new Limit(10, 60_000);
        if (path.equals("/api/v1/reservations/availability")) return new Limit(60, 60_000);
        if (path.equals("/api/v1/reservations/manage")) return new Limit(10, 60_000);
        if (path.equals("/api/v1/private-dining/inquiries") && "POST".equals(request.getMethod()))
            return new Limit(3, 60_000);
        return null;
    }

    private String bucket(HttpServletRequest request) {
        return request.getMethod() + ":" + request.getRequestURI();
    }
}
