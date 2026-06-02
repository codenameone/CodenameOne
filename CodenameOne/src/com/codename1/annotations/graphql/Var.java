/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.annotations.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Binds a [GraphQLClient] method parameter to a GraphQL operation
/// variable. The [#value()] is the variable name as it appears (without
/// the `$`) in the operation document's variable definitions. The
/// argument is serialised into the request's `variables` object:
/// strings/numbers/booleans/lists/maps pass through directly, enums
/// serialise as their `name()`, and any other object is treated as an
/// `@Mapped` business object. A null argument omits the variable.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Var {
    /// The GraphQL variable name (without the leading `$`).
    String value();
}
