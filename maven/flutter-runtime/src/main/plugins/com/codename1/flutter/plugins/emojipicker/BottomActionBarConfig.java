/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.emojipicker;

/// emoji_picker_flutter's `BottomActionBarConfig`: held for [Config], not yet applied by the picker.
public class BottomActionBarConfig {
    private Object enabled;
    private Object showBackspaceButton;
    private Object showSearchViewButton;
    private Object backgroundColor;
    private Object buttonColor;
    private Object buttonIconColor;
    private Object customBottomActionBar;

    public BottomActionBarConfig() {
    }

    public void enabled(Object v) {
        enabled = v;
    }

    public Object getEnabled() {
        return enabled;
    }

    public void showBackspaceButton(Object v) {
        showBackspaceButton = v;
    }

    public Object getShowBackspaceButton() {
        return showBackspaceButton;
    }

    public void showSearchViewButton(Object v) {
        showSearchViewButton = v;
    }

    public Object getShowSearchViewButton() {
        return showSearchViewButton;
    }

    public void backgroundColor(Object v) {
        backgroundColor = v;
    }

    public Object getBackgroundColor() {
        return backgroundColor;
    }

    public void buttonColor(Object v) {
        buttonColor = v;
    }

    public Object getButtonColor() {
        return buttonColor;
    }

    public void buttonIconColor(Object v) {
        buttonIconColor = v;
    }

    public Object getButtonIconColor() {
        return buttonIconColor;
    }

    public void customBottomActionBar(Object v) {
        customBottomActionBar = v;
    }

    public Object getCustomBottomActionBar() {
        return customBottomActionBar;
    }
}
