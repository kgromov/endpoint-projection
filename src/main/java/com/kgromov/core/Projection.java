package com.kgromov.core;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Alternative to Mapper interface:
 * TODO: add aspect to process
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface Projection {
}
