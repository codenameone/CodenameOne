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
package com.codename1.annotations.buildhints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// What an enum constant is called on the wire.
///
/// The builders compare against a literal, and the constant name is not it:
/// `IOS7` happens to lowercase to `ios7`, but `INTERNAL_ONLY` does not lowercase
/// to `internalOnly`, and `java.version` accepts `8`, `11`, `17` and `21`, none
/// of which can be a constant name at all. A builder handed a value it does not
/// recognize falls back to its default, so a name-based conversion fails
/// silently -- which is the failure this whole package removes.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface HintValue {

    /// The literal the builder compares against.
    String value();

    /// How the Settings editor labels this choice, when the wire value is not
    /// presentable on its own.
    String label() default "";
}
