/*
 * Test stub of com.codename1.annotations.Route. Mirrors the runtime annotation
 * so the JavaCompiler under test can compile @Route-annotated fixtures against
 * the plugin's test classpath.
 */
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Route {
    String value();
    String name() default "";

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface Routes {
        Route[] value();
    }
}
