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
package com.codename1.desktopcompat.java.awt;

/// The application's entry in the task bar or dock of a desktop.
///
/// No Codename One port gives an application one to draw on, so
/// [#isTaskbarSupported()] answers false and [#getTaskbar()] throws, as on
/// a desktop without a task bar. An application asks first.
public class Taskbar {

    /// What a task bar may be able to do.
    public enum Feature {
        ICON_BADGE_TEXT,
        ICON_BADGE_NUMBER,
        ICON_BADGE_IMAGE_WINDOW,
        ICON_IMAGE,
        MENU,
        PROGRESS_STATE_WINDOW,
        PROGRESS_VALUE,
        PROGRESS_VALUE_WINDOW,
        USER_ATTENTION,
        USER_ATTENTION_WINDOW
    }

    /// The kinds of progress a task bar entry can show.
    public enum State {
        OFF,
        NORMAL,
        PAUSED,
        INDETERMINATE,
        ERROR
    }

    private Taskbar() {
    }

    /// Always false.
    public static boolean isTaskbarSupported() {
        return false;
    }

    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: always; see [#isTaskbarSupported()]
    public static Taskbar getTaskbar() {
        throw new UnsupportedOperationException("Taskbar API is not supported on the current platform");
    }

    public boolean isSupported(Feature feature) {
        return false;
    }

    private static UnsupportedOperationException unsupported(Feature f) {
        return new UnsupportedOperationException("The " + f.name() + " feature is not supported on the current platform!");
    }

    public void requestUserAttention(boolean enabled, boolean critical) {
        throw unsupported(Feature.USER_ATTENTION);
    }

    public void requestWindowUserAttention(Window w) {
        throw unsupported(Feature.USER_ATTENTION_WINDOW);
    }

    public void setIconImage(Image image) {
        throw unsupported(Feature.ICON_IMAGE);
    }

    public Image getIconImage() {
        throw unsupported(Feature.ICON_IMAGE);
    }

    public void setIconBadge(String badge) {
        throw unsupported(Feature.ICON_BADGE_TEXT);
    }

    public void setWindowIconBadge(Window w, Image badge) {
        throw unsupported(Feature.ICON_BADGE_IMAGE_WINDOW);
    }

    public void setProgressValue(int value) {
        throw unsupported(Feature.PROGRESS_VALUE);
    }

    public void setWindowProgressValue(Window w, int value) {
        throw unsupported(Feature.PROGRESS_VALUE_WINDOW);
    }

    public void setWindowProgressState(Window w, State state) {
        throw unsupported(Feature.PROGRESS_STATE_WINDOW);
    }
}
