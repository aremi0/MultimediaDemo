package com.aremi.common.logging.annotation;

import java.lang.annotation.*;

/**
 * Annotazione per abilitare il monitoraggio/logging su una classe o metodo.
 *
 * <p>Può essere applicata a livello di classe (tutti i metodi pubblici vengono intercettati)
 * oppure a livello di singolo metodo.</p>
 *
 * <p>Singoli metodi possono essere esclusi, nel caso di monitoraggio di classe, settando enable=false
 * sullo specifico metodo.</p>
 *
 * <p>Parametri:
 * <ul>
 *   <li>{@code enable} - abilita o disabilita il monitoraggio (default: true)</li>
 *   <li>{@code subjectType} - indica il tipo di componente monitorato (es. CONTROLLER, SERVICE)</li>
 * </ul>
 * </p>
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Monitor {
    boolean enable() default true;
    SubjectType subjectType() default SubjectType.CONTROLLER;
}
