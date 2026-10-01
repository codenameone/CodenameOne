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
///
/// #### Which theme a desktop application gets
///
/// [#themeMode()] is resolved against the machine the application starts on,
/// because one desktop binary runs on all three operating systems:
///
/// | `desktop.themeMode` | Windows | macOS | Linux / GNOME |
/// | --- | --- | --- | --- |
/// | unset, or `legacy` | *unchanged* | *unchanged* | *unchanged* |
/// | `auto`, `native`, `modern` | Windows Fluent | macOS Aqua | GNOME Adwaita |
/// | `fluent` | Windows Fluent | Windows Fluent | Windows Fluent |
/// | `aqua` | macOS Aqua | macOS Aqua | macOS Aqua |
/// | `adwaita` | GNOME Adwaita | GNOME Adwaita | GNOME Adwaita |
/// | `custom` | *none* | *none* | *none* |
///
/// *unchanged* is the default and is deliberate: it is whatever the application was
/// built and tested against before these themes existed, because flipping it would
/// move every screen of every desktop application already shipped. `custom` differs
/// from it by installing no framework theme at all.
///
/// #### How that relates to the other theme hints
///
/// Each platform has its own hint, and each governs only its own platform:
///
/// | hint | governs | see |
/// | --- | --- | --- |
/// | `desktop.themeMode` | the JavaSE desktop application, on all three desktops | [#themeMode()] |
/// | `ios.themeMode` | iOS | [Ios#themeMode()] |
/// | `and.themeMode` | Android | [Android#themeMode()] |
/// | `mac.themeMode` | the native macOS build, a separate target from the JavaSE desktop application | [Mac#themeMode()] |
/// | `nativeTheme` | the default for the three above, where they are unset | [Build#nativeTheme()] |
///
/// The one value in that last row that also reaches the desktop is
/// `nativeTheme = ThemeMode.NATIVE`, which is the single hint for "look like the
/// platform, everywhere". `ThemeMode.MODERN` reaches iOS and Android only: it
/// shipped years before the desktop themes, so an application that set it for its
/// phone builds never asked for its desktop screens to be redrawn. `themeMode` here
/// outranks both.
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

    // No javadoc link syntax in the `///` block below, and no note inside it about why.
    // An attribute's prose is harvested verbatim into the developer guide's hint table,
    // so a bracketed reference reaches a reader as literal text pointing at a symbol the
    // guide does not publish -- and a parenthetical explaining that to the next editor is
    // an internal note published in a customer-facing document. This comment is the right
    // home for both: BuildHintAnnotationReader collects only the `///` run, and only the
    // one immediately preceding the declaration, so a `//` comment ABOVE it is invisible
    // to the table. Below it would be worse than invisible: a non-`///` line discards the
    // pending comment, and the attribute would reach the guide with no description at all.
    /// Which native theme a desktop build installs, and the one hint that decides
    /// whether a desktop application looks like the platform it's running on.
    ///
    /// One desktop binary runs on Windows, macOS and Linux, so the value is resolved
    /// against the machine the application starts on rather than at build time.
    /// `auto`, `native` and `modern` are one value under three spellings and select
    /// the host's own look: Fluent, Aqua or Adwaita. Naming a theme outright with
    /// `fluent`, `aqua` or `adwaita` pins that one look on every machine instead,
    /// which is what an application with a deliberate cross-platform identity wants.
    /// `legacy`, which is also the default, keeps whatever the application was built
    /// and tested against before these themes existed, and `custom` installs no
    /// framework theme at all so the application's own is the only one loaded.
    ///
    /// The per-value and per-platform tables, and how this relates to the iOS,
    /// Android, macOS and cross-platform theme hints, are on the `@DesktopBuild`
    /// annotation itself.
    ///
    /// Read by the JavaSE port at runtime rather than by a builder, so unlike most
    /// hints here it changes what the running application does rather than what's
    /// produced for it.
    @Hint(name = "desktop.themeMode",
            valuePattern = "auto|native|modern|fluent|aqua|adwaita|legacy|custom")
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
