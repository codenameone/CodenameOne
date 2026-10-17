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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.javax.swing.text.Document;

/// A single line field that hides what is typed, shown by a Codename One
/// text field in its password mode.
///
/// The character the platform masks with is the platform's own; the echo
/// character set here is recorded, and setting it to 0 shows the text.
/// Cut and copy do nothing.
public class JPasswordField extends JTextField {

    private char echoChar = '*';
    private boolean echoCharSet;

    public JPasswordField() {
        this(null, null, 0);
    }

    public JPasswordField(String text) {
        this(null, text, 0);
    }

    public JPasswordField(int columns) {
        this(null, null, columns);
    }

    public JPasswordField(String text, int columns) {
        this(null, text, columns);
    }

    public JPasswordField(Document doc, String txt, int columns) {
        super(doc, txt, columns);
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        applyEcho();
    }

    private void applyEcho() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.TextArea) {
            ((com.codename1.ui.TextArea) p).setConstraint(echoChar != 0 ? com.codename1.ui.TextArea.PASSWORD
                    : com.codename1.ui.TextArea.ANY);
        }
    }

    public char getEchoChar() {
        return echoChar;
    }

    public void setEchoChar(char c) {
        echoChar = c;
        echoCharSet = true;
        applyEcho();
        repaint();
    }

    public boolean echoCharIsSet() {
        return echoChar != 0;
    }

    /// The characters typed. The caller may wipe the array; it is a copy.
    public char[] getPassword() {
        String s = getText();
        char[] out = new char[s.length()];
        s.getChars(0, out.length, out, 0);
        return out;
    }

    @Override
    public void cut() {
    }

    @Override
    public void copy() {
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",echoChar=" + (echoCharSet ? String.valueOf(echoChar) : "default");
    }
}
