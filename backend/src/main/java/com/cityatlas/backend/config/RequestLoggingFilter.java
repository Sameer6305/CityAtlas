package com.cityatlas.backend.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startNs = System.nanoTime();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException ex) {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.error("[REQ] {} {} failed in {}ms", method, uri, elapsedMs, ex);
            throw ex;
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        int status = response.getStatus();
        if (status >= 500) {
            log.error("[REQ] {} {} -> {} ({}ms)", method, uri, status, elapsedMs);
            return;
        }
        log.info("[REQ] {} {} -> {} ({}ms)", method, uri, status, elapsedMs);
    }
}
