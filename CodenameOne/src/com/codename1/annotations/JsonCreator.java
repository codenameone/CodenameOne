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

/// The constructor, or static factory method, that builds an immutable class
/// from JSON.
///
/// A class with final fields and no constructor without arguments cannot be
/// filled in field by field, so its JSON is read into one local per member and
/// handed to this constructor at once -- as a record's canonical constructor
/// is. Every parameter carries [JsonProperty] naming the member it is read
/// from; a compiled class does not keep parameter names, so there is nothing
/// else to match them by. A member a body leaves out arrives as its type's
/// default (`null`, `0`, `false`) unless it is `required`.
///
/// ```java
/// @Mapped
/// public final class Point {
///     private final int x;
///     private final int y;
///
///     @JsonCreator
///     public Point(@JsonProperty("x") int x, @JsonProperty("y") int y) {
///         this.x = x;
///         this.y = y;
///     }
///
///     public int getX() { return x; }
///     public int getY() { return y; }
/// }
/// ```
///
/// The generated code calls it directly; nothing is looked up by name.
@com.codename1.impl.SharedWithBackend
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface JsonCreator {
}
