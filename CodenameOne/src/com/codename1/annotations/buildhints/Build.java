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

/// Build hints that are not specific to one platform.
///
/// Place this on your application's main class -- the class named by
/// `codename1.mainName`. An attribute you do not set is not written at all, so
/// the builder's own default applies; the values shown here are that default,
/// for reference.
///
/// Generated from com.codename1.build.shared.BuildHints by
/// BuildHintCodeGenerator. Do not edit by hand -- edit the catalog and
/// re-run scripts/gen-build-hint-annotations.sh.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Build {

    /// The application ID for an app that requires native Facebook login
    /// integration, this defaults to null which means native Facebook support
    /// shouldn't be in the app
    String facebookAppId() default "706695982682332";

    /// The Android/chrome push identifier, see the push section for more details
    String gcmSenderId() default "";

    /// `modern`, `legacy`, `custom` (default unset). Cross-platform override that
    /// sets both `ios.themeMode` and `and.themeMode` together when those aren't set
    /// explicitly. `modern` = liquid glass + Material 3, `legacy` = iOS 7 flat +
    /// Holo Light, `custom` disables the framework native theme entirely. The
    /// legacy alias `cn1.nativeTheme` is still accepted.
    NativeThemeMode nativeTheme() default NativeThemeMode.MODERN;

    /// true/false (defaults to false). Blocks codename one from injecting its own
    /// resources when set to true, the only effect this has is in slightly reducing
    /// archive size. This might have adverse effects on some features of Codename
    /// One so it isn't recommended.
    boolean noExtraResources() default false;
}
