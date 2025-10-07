package com.aremi.common.logging.extractor;

public interface RequestExtractor {
    String getMethod(Object[] args);
    String getUri(Object[] args);
    String getProtocol(Object[] args);
    String getClientIp(Object[] args);
    String getStatus(Object result);
}
