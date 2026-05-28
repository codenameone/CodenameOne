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

/// Requires the component value to parse as a number, optionally within a
/// closed range. The processor emits
/// `com.codename1.ui.validation.NumericConstraint(decimal, min, max, message)`
/// into the `Validator` returned by `Binding#getValidator()`.
///
/// ```java
/// @Bind(name="ageField") @Numeric(min = 0, max = 150)
/// private int age;
///
/// @Bind(name="priceField") @Numeric(decimal = true, min = 0.01)
/// private double price;
/// ```
///
/// Bounds are inclusive. Omitting `min` / `max` removes that side of the
/// range (the defaults are negative / positive infinity).
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Numeric {
    /// `true` allows a decimal value (parsed via `Double.parseDouble`);
    /// `false` requires an integer (parsed via `Integer.parseInt`).
    boolean decimal() default false;

    /// Inclusive lower bound. Default: no lower bound.
    double min() default Double.NEGATIVE_INFINITY;

    /// Inclusive upper bound. Default: no upper bound.
    double max() default Double.POSITIVE_INFINITY;

    /// Override the default error message
    /// (`NumericConstraint` derives one from the bounds when blank).
    String message() default "";
}
