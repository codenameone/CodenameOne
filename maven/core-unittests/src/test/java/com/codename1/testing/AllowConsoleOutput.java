package com.codename1.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that console output produced during a test should be allowed without causing the
 * {@link UnexpectedLogExtension} to fail the test. This can be applied to individual test methods or
 * entire test classes when console output is part of the expected behaviour under test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AllowConsoleOutput {
    /**
     * When set to {@code true}, console output is permitted. The attribute exists to allow future
     * flexibility should tests wish to dynamically enable or disable console output.
     */
    boolean value() default true;
}
