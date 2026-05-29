/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Binds an interface method parameter to a `Cookie` request-header
/// entry. Multiple `@Cookie`-annotated parameters on the same method
/// are joined into a single `Cookie: a=1; b=2` header. `null`
/// arguments are skipped; values are URL-encoded.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Cookie {
    String value();
}
