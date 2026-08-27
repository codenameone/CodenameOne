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

/// App hardening build hints, checked by the compiler.
///
/// Place this on your application's main class -- the class named by
/// `codename1.mainName`. An attribute you do not set is not written at all, so
/// the build server's own default applies. The `default` clause below each
/// attribute names a constant that says nothing -- see [HintUnset] -- and this
/// package deliberately does not record what the server would do instead,
/// because that is the server's to change.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Hardening {

    /// Permits a local or source build to run with hardening requested but not
    /// applied. Without it such a build is refused, so a hardened app is never
    /// shipped from a target that can't actually harden it.
    Toggle allowUnhardenedLocalBuild() default Toggle.DEFAULT;

    /// Overrides control-flow obfuscation independently of harden.level.
    HardenControlFlow controlFlow() default HardenControlFlow.DEFAULT;

    /// Keep rules in ProGuard syntax, one per line, for classes that are resolved
    /// by name at runtime and so can't be found by the automatic analysis. Same
    /// syntax as android.proguardKeep, so existing rules port directly. Rules are
    /// separated by newlines only, because a semicolon is legal inside a rule body
    /// such as { *; }.
    @Hint(kind = HintKind.TEXT_BLOCK)
    String keep() default "";

    /// Master switch for app hardening: off, standard, aggressive or paranoid. An
    /// unrecognized value fails the build rather than being treated as off.
    HardenLevel level() default HardenLevel.DEFAULT;

    /// Overrides symbol renaming independently of harden.level.
    Toggle rename() default Toggle.DEFAULT;

    /// Overrides string obfuscation independently of harden.level: off, constants
    /// or all.
    HardenStrings strings() default HardenStrings.DEFAULT;
}
