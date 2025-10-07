package com.aremi.common.logging.annotation;

/**
 * Tipi di componenti che possono essere monitorati con l'annotazione {@link Monitor}.
 */

public enum SubjectType {
    CONTROLLER,
    SERVICE,
    REPOSITORY,
    REQUEST
}
