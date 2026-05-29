/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Binds an interface method parameter to a `{name}` placeholder in
/// the URL path declared on the verb annotation. The parameter value
/// is URL-encoded before substitution.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Path {
    String value();
}
