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
/// the build server's own default applies. The `default` clause below each
/// attribute names a constant that says nothing -- see [HintUnset] -- and this
/// package deliberately does not record what the server would do instead,
/// because that is the server's to change.
///
/// The platform is stated once on the annotation, not on every attribute. An
/// attribute repeats it only to disagree with it.
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

    /// Which native theme a desktop build installs, and the one hint that decides
    /// whether a desktop application looks like the platform it's running on.
    ///
    /// `auto` and `native` both mean "whatever this machine is" -- Fluent on
    /// Windows, Aqua on macOS, Adwaita on GNOME -- which is the only sensible
    /// reading on desktop, where one binary runs on all three. A theme can also be
    /// named outright with `fluent`, `aqua` or `adwaita`, which is what a build
    /// that wants one look everywhere asks for. `legacy` keeps what desktop
    /// applications have always had, and `custom` installs nothing so the
    /// application's own theme is the only one.
    ///
    /// Read by the JavaSE port at runtime rather than by a builder, so unlike most
    /// hints here it changes what the running application does rather than what's
    /// produced for it.
    @Hint(name = "desktop.themeMode",
            valuePattern = "auto|native|fluent|aqua|adwaita|legacy|custom")
    String themeMode() default "";

    /// How the desktop window is framed: native for the OS title bar and menu bar,
    /// custom for an undecorated window with a Codename One drawn title bar, or
    /// toolbar for the legacy in-app Toolbar. An unrecognized value falls back to
    /// native with a warning.
    DesktopTitleBar titleBar() default DesktopTitleBar.DEFAULT;

    /// Width in pixels for the form in desktop builds, will be doubled for retina
    /// grade displays. Defaults to 800.
    int width() default 0;
}
