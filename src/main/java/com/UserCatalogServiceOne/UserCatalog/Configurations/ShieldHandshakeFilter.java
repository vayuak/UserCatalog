package com.UserCatalogServiceOne.UserCatalog.Configurations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class ShieldHandshakeFilter extends OncePerRequestFilter {

    @Value("${ghost.shield.key:PermanentSecret999}")
    private String expectedClientKey;

    @Value("${ghost.gateway.secret:CryptographicGhostShieldInternalTokenSignature7350_465}")
    private String expectedGatewaySecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        AntPathMatcher pathMatcher = new AntPathMatcher();

        return pathMatcher.match("/api/users/register/**", path)
                || pathMatcher.match("/api/users/login/**", path)
                || pathMatcher.match("/api/users/verify-otp/**", path)
                || pathMatcher.match("/api/users/check-username/**", path)
                || pathMatcher.match("/api/users/forgot-password/**", path)
                || pathMatcher.match("/api/users/reset-password/**", path)
                || pathMatcher.match("/api/users/internal/**", path)
                || pathMatcher.match("/actuator/**", path)
                || pathMatcher.match("/eureka/**", path)
                || pathMatcher.match("/error", path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientKey = request.getHeader("X-Ghost-Shield-Key");
        if (clientKey == null) {
            clientKey = request.getHeader("x-ghost-shield-key");
        }
        if (clientKey != null) {
            clientKey = clientKey.trim();
        }

        String gatewaySecret = request.getHeader("X-Gateway-Secret");
        if (gatewaySecret != null) {
            gatewaySecret = gatewaySecret.trim();
        }

        boolean isClientKeyValid = clientKey != null && clientKey.contains(expectedClientKey);
        boolean isGatewaySecretValid = gatewaySecret != null && gatewaySecret.contains(expectedGatewaySecret);

        if (isClientKeyValid || isGatewaySecretValid) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("🚨 Access Denied: Missing or invalid Shield/Gateway headers for path: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Access Denied: Secure Handshake Failed.");
        }
    }
}