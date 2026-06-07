/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// HTTP `PATCH` request. See [GET] for path semantics.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface PATCH {
    String value();
}
