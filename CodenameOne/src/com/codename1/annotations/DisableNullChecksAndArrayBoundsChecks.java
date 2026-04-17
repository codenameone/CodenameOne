package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a method so ParparVM omits emitted null and array bounds checks.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface DisableNullChecksAndArrayBoundsChecks {
}
