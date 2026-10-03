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
package android.text.method;

/// Masks text: an EditText given this shows a password field.
public class PasswordTransformationMethod implements TransformationMethod {
    private static final PasswordTransformationMethod INSTANCE = new PasswordTransformationMethod();

    public static PasswordTransformationMethod getInstance() {
        return INSTANCE;
    }

    @Override
    public CharSequence getTransformation(CharSequence source, android.view.View view) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            sb.append('\u2022');
        }
        return sb.toString();
    }

    @Override
    public void onFocusChanged(android.view.View view, CharSequence sourceText, boolean focused, int direction,
                               android.graphics.Rect previouslyFocusedRect) {
    }
}
