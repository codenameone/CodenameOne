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
package com.codename1.desktopcompat.javax.swing.text;

/// Sees every change before it reaches a document and may let it through,
/// change it or drop it. Text typed into the Codename One widget behind a
/// text component passes through the filter too: what the filter refuses
/// is taken back out of the widget.
public class DocumentFilter {

    public DocumentFilter() {
    }

    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        fb.remove(offset, length);
    }

    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        fb.insertString(offset, string, attr);
    }

    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        fb.replace(offset, length, text, attrs);
    }

    /// The way around the filter, for the filter itself.
    public abstract static class FilterBypass {

        public FilterBypass() {
        }

        public abstract Document getDocument();

        public abstract void remove(int offset, int length) throws BadLocationException;

        public abstract void insertString(int offset, String string, AttributeSet attr) throws BadLocationException;

        public abstract void replace(int offset, int length, String string, AttributeSet attrs)
                throws BadLocationException;
    }
}
