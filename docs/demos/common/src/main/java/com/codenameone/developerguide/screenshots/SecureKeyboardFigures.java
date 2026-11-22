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
package com.codenameone.developerguide.screenshots;

import com.codename1.security.SecureKeyboard;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

/// The figures of the secure keyboard section in the security chapter: the
/// keyboard open under a field, in the layouts a field's constraint picks.
public final class SecureKeyboardFigures {
    private SecureKeyboardFigures() {
    }

    /// A fixed order, so the scrambled figure is the same picture every run.
    /// Real scrambling draws a new order from SecureRandom on every open.
    private static final String SCRAMBLED_ORDER = "7302958164";

    private static final class Fig implements GuideFigure {
        private final String id;
        private final String label;
        private final String hint;
        private final int constraint;
        private final String text;
        private final boolean scrambled;
        private SecureKeyboard keyboard;

        Fig(String id, String label, String hint, int constraint, String text, boolean scrambled) {
            this.id = id;
            this.label = label;
            this.hint = hint;
            this.constraint = constraint;
            this.text = text;
            this.scrambled = scrambled;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Form build() {
            Form f = new Form("Sign in", BoxLayout.y());
            f.add(new Label(label));
            TextField field = new TextField(text, hint, 20, constraint);
            f.add(field);
            keyboard = SecureKeyboard.install(field);
            if (scrambled) {
                keyboard.setDigitOrder(SCRAMBLED_ORDER);
            }
            f.show();
            return f;
        }

        @Override
        public void afterShow(Form form) {
            keyboard.show();
            GuideFigure.settleAnimations(300);
        }

        @Override
        public boolean fillsViewport() {
            return true;
        }
    }

    public static FigureVariant[] variants() {
        Fig text = new Fig("secure-keyboard-text", "Password", "Password",
                TextArea.PASSWORD, "hunter", false);
        return new FigureVariant[] {
            new FigureVariant(text, FigureDevice.ANDROID, false, "secure-keyboard-text.png"),
            new FigureVariant(text, FigureDevice.ANDROID, true, "secure-keyboard-text-dark.png"),
            new FigureVariant(new Fig("secure-keyboard-number", "PIN", "PIN",
                    TextArea.NUMERIC | TextArea.PASSWORD, "12", false),
                    FigureDevice.ANDROID, false, "secure-keyboard-number.png"),
            new FigureVariant(new Fig("secure-keyboard-scrambled", "PIN", "PIN",
                    TextArea.NUMERIC | TextArea.PASSWORD, "", true),
                    FigureDevice.ANDROID, false, "secure-keyboard-scrambled.png"),
            new FigureVariant(new Fig("secure-keyboard-email", "Email", "you@example.com",
                    TextArea.EMAILADDR, "", false),
                    FigureDevice.ANDROID, false, "secure-keyboard-email.png"),
        };
    }
}
