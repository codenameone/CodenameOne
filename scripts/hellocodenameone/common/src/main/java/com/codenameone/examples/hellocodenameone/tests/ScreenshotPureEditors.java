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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CodeEditor;
import com.codename1.ui.RichTextArea;

/**
 * Editor variants used by the screenshot suite to exercise the lightweight renderer directly.
 *
 * <p>Production editors fall back to {@code BrowserComponent} when a port does not expose the
 * low-level text-input contract.  That is required for editability on the JavaScript port, but the
 * HTML5 screenshot harness cannot capture nested iframe pixels.  These variants keep the visual
 * regression tests focused on the pure renderer without changing production backend selection.</p>
 */
final class ScreenshotPureEditors {
    private ScreenshotPureEditors() {
    }

    static final class Code extends CodeEditor {
        Code() {
        }

        Code(String language, String text) {
            super(language, text);
        }

        @Override
        public boolean isTextInputSupported() {
            return true;
        }
    }

    static final class Rich extends RichTextArea {
        Rich() {
        }

        Rich(String html) {
            super(html);
        }

        @Override
        public boolean isTextInputSupported() {
            return true;
        }
    }
}
