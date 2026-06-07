/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Binds an interface method parameter to an HTTP request header.
/// `null` argument values skip the header. `Authorization` is the
/// usual carrier for the bearer token argument emitted by
/// `cn1:generate-openapi`.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Header {
    String value();
}
