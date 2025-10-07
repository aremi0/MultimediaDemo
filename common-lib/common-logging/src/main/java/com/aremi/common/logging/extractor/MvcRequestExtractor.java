package com.aremi.common.logging.extractor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public class MvcRequestExtractor implements RequestExtractor {

    @Override
    public String getMethod(Object[] args) {
        HttpServletRequest req = extract(args);
        return req != null ? req.getMethod() : "unknown";
    }

    @Override
    public String getUri(Object[] args) {
        HttpServletRequest req = extract(args);
        return req != null ? req.getRequestURI() : "unknown";
    }

    @Override
    public String getProtocol(Object[] args) {
        HttpServletRequest req = extract(args);
        return req != null ? req.getProtocol() : "unknown";
    }

    @Override
    public String getClientIp(Object[] args) {
        HttpServletRequest req = extract(args);
        if (req == null) return "unknown";
        String xfHeader = req.getHeader("X-Real-IP");
        return xfHeader != null ? xfHeader.split(",")[0].trim() : req.getRemoteAddr();
    }

    @Override
    public String getStatus(Object result) {
        if (result instanceof ResponseEntity<?> response) {
            return String.valueOf(response.getStatusCode().value());
        }
        return "unknown";
    }

    private HttpServletRequest extract(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest req) {
                return req;
            }
        }
        return null;
    }
}
