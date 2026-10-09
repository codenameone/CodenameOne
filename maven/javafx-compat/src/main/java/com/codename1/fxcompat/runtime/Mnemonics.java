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
package com.codename1.fxcompat.runtime;

/// Takes the mark of a mnemonic out of the text of a control.
///
/// JavaFX lets an underscore in the text of a button or a menu item mark
/// the character after it as the mnemonic, the key that with a modifier
/// fires the control, and does not show the underscore. The scene fires
/// the control when that key comes with Alt held; the character is not
/// drawn underlined. The text reads as it does in JavaFX:
///
/// - two underscores are one underscore that marks nothing;
/// - the first single underscore with a character after it is left out;
/// - the first `_(c)`, the form for a mnemonic that is not a letter of
///   the text, is left out whole;
/// - any later single underscore stays as it is, as does one that ends
///   the text.
public final class Mnemonics {

    private Mnemonics() {
    }

    /// Returns the text as it is shown when mnemonics are parsed; `null`
    /// for `null`.
    public static String strip(String text) {
        if (text == null || text.indexOf('_') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        boolean found = false;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c != '_' || i + 1 >= n) {
                out.append(c);
            } else if (text.charAt(i + 1) == '_') {
                out.append('_');
                i++;
            } else if (found) {
                out.append(c);
            } else {
                found = true;
                if (text.charAt(i + 1) == '(' && i + 3 < n && text.charAt(i + 3) == ')') {
                    i += 3;
                }
            }
        }
        return out.toString();
    }

    /// Returns the mnemonic a text marks, folded to upper case, or zero
    /// when it marks none: the character after the first single
    /// underscore, or the one in brackets after it.
    public static char key(String text) {
        if (text == null) {
            return 0;
        }
        int n = text.length();
        for (int i = 0; i + 1 < n; i++) {
            if (text.charAt(i) != '_') {
                continue;
            }
            char next = text.charAt(i + 1);
            if (next == '_') {
                i++;
                continue;
            }
            if (next == '(' && i + 3 < n && text.charAt(i + 3) == ')') {
                next = text.charAt(i + 2);
            }
            return upper(next);
        }
        return 0;
    }

    /// Folds an ASCII letter to upper case and leaves anything else. A
    /// mnemonic is matched against a key, which has no locale.
    public static char upper(char c) {
        return c >= 'a' && c <= 'z' ? (char) (c - 'a' + 'A') : c;
    }
}
