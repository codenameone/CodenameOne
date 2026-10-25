/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Refuses a body that carries a member this class does not declare.
///
/// By default an unknown member is ignored, as a Spring Boot application
/// ignores one, so a newer client can talk to an older server. A contract that
/// would rather hear about a misspelt member than lose it silently marks the
/// class: the read then fails with the member's path, for example
/// `$.items[2].colour: unknown member`. Names a field lists in
/// [JsonProperty#aliases] are known; names of [JsonIgnore] fields are not.
@com.codename1.impl.SharedWithBackend
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface JsonStrict {
}
