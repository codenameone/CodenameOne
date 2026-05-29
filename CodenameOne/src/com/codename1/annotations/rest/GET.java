/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// HTTP `GET` request. The value is the URL path relative to the
/// `baseUrl` passed to `<Tag>Api.of(baseUrl)`. Path placeholders such
/// as `/pet/{petId}` are substituted from method parameters annotated
/// with [Path].
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface GET {
    String value();
}
