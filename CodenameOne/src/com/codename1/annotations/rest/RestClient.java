/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks an interface as a REST client that the build-time
/// annotation processor wires up to a generated network implementation.
/// Companion HTTP-verb annotations ([GET], [POST], [PUT], [DELETE],
/// [PATCH]) on each method carry the path; parameter annotations
/// ([Path], [Query], [Header], [Body]) describe how each argument is
/// attached to the request.
///
/// The processor emits a `<Tag>ApiImpl` class in generated-sources and
/// registers it with [com.codename1.io.rest.RestClients] so the
/// interface's `static T of(String baseUrl)` factory can return an
/// instance without the project source referencing the impl directly.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface RestClient {
}
