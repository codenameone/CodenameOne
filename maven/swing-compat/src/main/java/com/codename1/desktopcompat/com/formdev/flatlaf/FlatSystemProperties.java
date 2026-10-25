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
package com.codename1.desktopcompat.com.formdev.flatlaf;

/// The names of the system properties FlatLaf reads on a desktop. They are
/// names only: this layer reads none of them.
public interface FlatSystemProperties {

    String UI_SCALE = "flatlaf.uiScale";
    String UI_SCALE_ENABLED = "flatlaf.uiScale.enabled";
    String UI_SCALE_ALLOW_SCALE_DOWN = "flatlaf.uiScale.allowScaleDown";
    String USE_UBUNTU_FONT = "flatlaf.useUbuntuFont";
    String USE_WINDOW_DECORATIONS = "flatlaf.useWindowDecorations";
    String USE_JETBRAINS_CUSTOM_DECORATIONS = "flatlaf.useJetBrainsCustomDecorations";
    String MENUBAR_EMBEDDED = "flatlaf.menuBarEmbedded";
    String ANIMATION = "flatlaf.animation";
    String USE_ROUNDED_POPUP_BORDER = "flatlaf.useRoundedPopupBorder";
    String REUSE_VISIBLE_POPUP_WINDOW = "flatlaf.reuseVisiblePopupWindow";
    String USE_TEXT_Y_CORRECTION = "flatlaf.useTextYCorrection";
    String UPDATE_UI_ON_SYSTEM_FONT_CHANGE = "flatlaf.updateUIOnSystemFontChange";
    String USE_NATIVE_LIBRARY = "flatlaf.useNativeLibrary";
    String NATIVE_LIBRARY_PATH = "flatlaf.nativeLibraryPath";
    String USE_SUB_MENU_SAFE_TRIANGLE = "flatlaf.useSubMenuSafeTriangle";
    String USE_SYSTEM_FILE_CHOOSER = "flatlaf.useSystemFileChooser";

    /// `defaultValue`: the layer reads no system property of FlatLaf.
    static boolean getBoolean(String key, boolean defaultValue) {
        return defaultValue;
    }

    /// `defaultValue`; see [#getBoolean].
    static Boolean getBooleanStrict(String key, Boolean defaultValue) {
        return defaultValue;
    }
}
