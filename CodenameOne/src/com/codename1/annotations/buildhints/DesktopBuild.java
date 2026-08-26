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

/// Desktop build hints, checked by the compiler.
///
/// Place this on your application's main class -- the class named by
/// `codename1.mainName`. An attribute you do not set is not written at all, so
/// the builder's own default applies. Each attribute's `@Hint(def)` records what
/// that default is; the `default` clause below it is a neutral placeholder with
/// no meaning at runtime.
///
/// The platform and the builders that read these hints are stated once on the
/// annotation, not on every attribute. An attribute repeats one only to
/// disagree with it.
@Hint(platform = "desktop")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DesktopBuild {

    /// Boolean true/false defaults to true. When set to true some values will ve
    /// implicitly doubled to deal with retina displays and icons etc. Will use
    /// higher DPI's
    Toggle adaptToRetina() default Toggle.DEFAULT;

    /// Starts the desktop build in full-screen mode.
    Toggle fullscreen() default Toggle.DEFAULT;

    /// Height in pixels for the form in desktop builds, will be doubled for retina
    /// grade displays. Defaults to 600.
    int height() default 0;

    /// Enables grab-able, click-to-page desktop scrollbars.
    Toggle interactiveScrollbars() default Toggle.DEFAULT;

    /// Boolean true/false defaults to true. Indicates whether the UI in the
    /// desktop build is resizable
    Toggle resizable() default Toggle.DEFAULT;

    /// How the desktop window is framed: native for the OS title bar and menu bar,
    /// custom for an undecorated window with a Codename One drawn title bar, or
    /// toolbar for the legacy in-app Toolbar. An unrecognized value falls back to
    /// native with a warning.
    DesktopTitleBar titleBar() default DesktopTitleBar.DEFAULT;

    /// Width in pixels for the form in desktop builds, will be doubled for retina
    /// grade displays. Defaults to 800.
    int width() default 0;
}
