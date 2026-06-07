/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks an interface method parameter as the request body. The
/// processor serialises the argument via `Mappers.toJson(...)` and
/// attaches it with `Content-Type: application/json`. At most one
/// `@Body`-annotated parameter is allowed per method.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Body {
}
