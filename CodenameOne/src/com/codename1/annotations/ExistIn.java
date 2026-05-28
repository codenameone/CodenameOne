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

/// Requires the component value to be one of an allowed list of strings. The
/// processor emits
/// `com.codename1.ui.validation.ExistInConstraint(value, caseSensitive, message)`
/// into the `Validator` returned by `Binding#getValidator()`.
///
/// ```java
/// @Bind(name="roleField") @ExistIn({"admin", "editor", "viewer"})
/// private String role;
/// ```
///
/// Use it to gate free-text fields that should accept only a known
/// vocabulary, or to gate `Picker` selections against an authoritative list
/// known at compile time.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface ExistIn {
    /// The allowed values. The component must equal one of them.
    String[] value();

    /// `true` to compare values with case-sensitivity. Default: false.
    boolean caseSensitive() default false;

    /// Override the default error message
    /// (`ExistInConstraint` derives one from the value list when blank).
    String message() default "";
}
