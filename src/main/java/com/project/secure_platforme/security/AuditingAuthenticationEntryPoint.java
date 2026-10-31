package com.project.secure_platforme.security;

import com.project.secure_platforme.audit.AuditEventType;
import com.project.secure_platforme.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class AuditingAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final int MAX_PATH_LENGTH = 255;

    private final AuditService auditService;

    public AuditingAuthenticationEntryPoint(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        boolean tokenPresented = exception instanceof OAuth2AuthenticationException;
        String reason = tokenPresented ? tokenFailureReason(exception) : "missing_token";

        AuditEventType eventType = switch (reason) {
            case "expired" -> AuditEventType.TOKEN_EXPIRED;
            case "missing_token" -> AuditEventType.UNAUTHORIZED_ACCESS;
            default -> AuditEventType.TOKEN_INVALID;
        };

        auditService.record(eventType, null, Map.of(
                "reason", reason,
                "method", request.getMethod(),
                "path", truncate(request.getRequestURI())
        ));

        String detail = tokenPresented ? "Invalid or expired token" : "Authentication required";
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, tokenPresented ? "Bearer error=\"invalid_token\"" : "Bearer");
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"" + detail + "\"}");
    }

    private static String tokenFailureReason(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof JwtValidationException validation) {
                for (OAuth2Error error : validation.getErrors()) {
                    String description = error.getDescription() == null ? "" : error.getDescription().toLowerCase();
                    if (description.contains("expired")) {
                        return "expired";
                    }
                    if (description.contains("revoked")) {
                        return "revoked";
                    }
                }
            }
        }
        return "invalid";
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_PATH_LENGTH ? value : value.substring(0, MAX_PATH_LENGTH);
    }
}
