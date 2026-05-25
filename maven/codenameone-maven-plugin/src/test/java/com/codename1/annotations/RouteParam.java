/*
 * Test stub of com.codename1.annotations.RouteParam mirroring the runtime
 * annotation so JavaCompiler under test can compile fixtures.
 */
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface RouteParam {
    String value();
    boolean required() default true;
}
