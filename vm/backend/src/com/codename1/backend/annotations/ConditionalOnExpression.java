/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Constructs this bean only when a boolean expression over the configuration
/// is true. Evaluated once at start-up, like [ConditionalOnProperty].
///
/// Spring evaluates SpEL here. This is the part of it a condition is written
/// in, and nothing else -- no method calls, no bean references, no arithmetic:
///
/// - `${key}` and `${key:default}`, replaced by the configuration value (an
///   unset key with no default is the empty string);
/// - literals: `true`, `false`, and text in single quotes;
/// - `==` and `!=`, comparing text exactly;
/// - `!` or `not`, `&&` or `and`, `||` or `or`, and parentheses.
///
/// A bare value is true when it reads `true`, ignoring case, so
/// `"${feature.enabled:false}"` works on its own. Anything outside this subset
/// stops the server at start-up, naming the expression.
///
/// ```java
/// @ConditionalOnExpression("${billing.provider:none} == 'paddle' && !${billing.sandbox:false}")
/// ```
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ConditionalOnExpression {
    /// The expression; Spring's default is "true".
    String value() default "true";
}
