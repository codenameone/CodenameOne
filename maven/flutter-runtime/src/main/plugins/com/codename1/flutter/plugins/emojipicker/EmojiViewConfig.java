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

/// emoji_picker_flutter's `EmojiViewConfig`: held for [Config], not yet applied by the picker.
public class EmojiViewConfig {
    private Object columns;
    private Object emojiSizeMax;
    private Object backgroundColor;
    private Object verticalSpacing;
    private Object horizontalSpacing;
    private Object gridPadding;
    private Object recentsLimit;
    private Object noRecents;
    private Object buttonMode;
    private Object loadingIndicator;
    private Object replaceEmojiOnLimitExceed;

    public EmojiViewConfig() {
    }

    public void columns(Object v) {
        columns = v;
    }

    public Object getColumns() {
        return columns;
    }

    public void emojiSizeMax(Object v) {
        emojiSizeMax = v;
    }

    public Object getEmojiSizeMax() {
        return emojiSizeMax;
    }

    public void backgroundColor(Object v) {
        backgroundColor = v;
    }

    public Object getBackgroundColor() {
        return backgroundColor;
    }

    public void verticalSpacing(Object v) {
        verticalSpacing = v;
    }

    public Object getVerticalSpacing() {
        return verticalSpacing;
    }

    public void horizontalSpacing(Object v) {
        horizontalSpacing = v;
    }

    public Object getHorizontalSpacing() {
        return horizontalSpacing;
    }

    public void gridPadding(Object v) {
        gridPadding = v;
    }

    public Object getGridPadding() {
        return gridPadding;
    }

    public void recentsLimit(Object v) {
        recentsLimit = v;
    }

    public Object getRecentsLimit() {
        return recentsLimit;
    }

    public void noRecents(Object v) {
        noRecents = v;
    }

    public Object getNoRecents() {
        return noRecents;
    }

    public void buttonMode(Object v) {
        buttonMode = v;
    }

    public Object getButtonMode() {
        return buttonMode;
    }

    public void loadingIndicator(Object v) {
        loadingIndicator = v;
    }

    public Object getLoadingIndicator() {
        return loadingIndicator;
    }

    public void replaceEmojiOnLimitExceed(Object v) {
        replaceEmojiOnLimitExceed = v;
    }

    public Object getReplaceEmojiOnLimitExceed() {
        return replaceEmojiOnLimitExceed;
    }
}
