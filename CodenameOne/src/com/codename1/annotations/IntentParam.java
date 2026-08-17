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

/// Names and describes one parameter of an [AppIntent] handler.
///
/// Every parameter of a handler needs one, except an optional leading
/// `com.codename1.intents.IntentContext`. Supported types are `String`, `int`,
/// `long`, `float`, `double`, `boolean`, `java.util.Date`, and any class
/// annotated [IntentEntity].
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface IntentParam {

    /// The parameter name used on the wire and in phrase placeholders. Required.
    String value();

    /// What the platform asks when it needs this value -- "Which playlist?".
    ///
    /// Write it as a question to the user, because on iOS that is literally what
    /// Siri says out loud. Defaults to the parameter name, which is almost never
    /// what you want a user to hear.
    String title() default "";

    /// Whether the intent can run without this value. An unfilled required
    /// parameter is what triggers the platform's own picker.
    boolean required() default true;

    /// The value substituted when an optional parameter is absent.
    String defaultValue() default "";

    /// A closed vocabulary of accepted values.
    ///
    /// The platform offers these as choices rather than asking for free text,
    /// and the framework rejects anything else before your handler runs. Use it
    /// instead of declaring an enum type: an enum would need its own resolution
    /// machinery on every platform for no gain over a checked string.
    String[] options() default {};
}
