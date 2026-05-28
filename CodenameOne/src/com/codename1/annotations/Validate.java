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

/// Escape hatch for the validation annotation set: points the component at a
/// hand-written `com.codename1.ui.validation.Constraint` implementation. The
/// referenced class must be public and expose a public no-argument
/// constructor; the generated binder calls `new YourConstraint()` at bind
/// time and registers it against the `Validator` exposed via
/// `Binding#getValidator()`.
///
/// ```java
/// public final class PhoneConstraint implements Constraint {
///     public boolean isValid(Object value) { ... }
///     public String getDefaultFailMessage() { return "Bad phone number"; }
/// }
///
/// @Bind(name="phoneField") @Validate(PhoneConstraint.class)
/// private String phone;
/// ```
///
/// Stacks with the canned annotations -- `@Required @Validate(MyExtra.class)`
/// composes them under a single `GroupConstraint` (first failure wins). Use
/// it when the built-ins aren't enough; reach for the canned annotations
/// first.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Validate {
    /// The `Constraint` implementation to instantiate. Must have a public
    /// no-argument constructor.
    Class<?> value();
}
