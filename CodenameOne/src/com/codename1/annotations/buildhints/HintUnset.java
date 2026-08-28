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

/// Marks the enum constant that means "nothing was chosen".
///
/// An annotation member must declare a default -- Java has no null for one, and
/// a member without a default is mandatory, which would force every attribute
/// to be written. So the default has to name SOME constant, and the only honest
/// one to name is a constant that says nothing.
///
/// A hint whose attribute is left at this constant is not written into the build
/// request at all, exactly as an attribute the developer never mentioned is not.
/// The build server then applies its own default, which is where that decision
/// belongs: it is the server's to change, and an annotation that restated it
/// would be a second copy going quietly stale in every app already compiled
/// against it.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface HintUnset {
}
