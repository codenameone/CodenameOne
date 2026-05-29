/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Binds an interface method parameter to a URL query-string entry.
/// `null` arguments are skipped; collections are appended as repeated
/// `name=value` pairs.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Query {
    String value();
}
