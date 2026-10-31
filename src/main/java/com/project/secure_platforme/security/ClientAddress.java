package com.project.secure_platforme.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ClientAddress {

    private ClientAddress() {
    }

    public static String current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String remoteAddress = attributes.getRequest().getRemoteAddr();
            return remoteAddress != null ? remoteAddress : "unknown";
        }
        return "unknown";
    }
}
