package com.UserCatalogServiceOne.UserCatalog.Configurations;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class ShieldHandshakeFilter extends OncePerRequestFilter {

    // This should match the key in your Gateway
    private static final String SHIELD_KEY = "PermanentSecret999";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException, java.io.IOException {

        String incomingKey = request.getHeader("X-Ghost-Shield-Key");

        if (SHIELD_KEY.equals(incomingKey)) {
            // Handshake valid, proceed to the King (User Service)
            filterChain.doFilter(request, response);
        } else {
            // Intruder detected or direct bypass attempt
            log.error("UNAUTHORIZED ACCESS ATTEMPT: Direct hit to UserCatalog from IP: {}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Ghost System: Access Denied. Use the Gateway.");
        }
    }
}
