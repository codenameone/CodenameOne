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

import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.javax.swing.Action;

/// The names of the editing actions a text component offers, and the
/// actions themselves for the ones this layer carries out: cut, copy, paste,
/// select all and the two that insert a line break or a tab.
///
/// Reading and writing a document through the kit, the caret movement
/// actions and the view factory are not part of this layer; the platform's
/// text editor moves the caret.
public class DefaultEditorKit {

    public static final String EndOfLineStringProperty = "__EndOfLine__";

    public static final String insertContentAction = "insert-content";

    public static final String insertBreakAction = "insert-break";

    public static final String insertTabAction = "insert-tab";

    public static final String deletePrevCharAction = "delete-previous";

    public static final String deleteNextCharAction = "delete-next";

    public static final String readOnlyAction = "set-read-only";

    public static final String writableAction = "set-writable";

    public static final String cutAction = "cut-to-clipboard";

    public static final String copyAction = "copy-to-clipboard";

    public static final String pasteAction = "paste-from-clipboard";

    public static final String beepAction = "beep";

    public static final String selectAllAction = "select-all";

    public static final String defaultKeyTypedAction = "default-typed";

    public DefaultEditorKit() {
    }

    /// The actions this layer carries out, a new array each time.
    public Action[] getActions() {
        return new Action[] {
            new CutAction(), new CopyAction(), new PasteAction(), new Cn1SelectAll(),
            new InsertBreakAction(), new InsertTabAction()
        };
    }

    /// Moves the selection to the clipboard.
    public static class CutAction extends TextAction {

        public CutAction() {
            super(cutAction);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                target.cut();
            }
        }
    }

    /// Copies the selection to the clipboard.
    public static class CopyAction extends TextAction {

        public CopyAction() {
            super(copyAction);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                target.copy();
            }
        }
    }

    /// Replaces the selection with the clipboard's text.
    public static class PasteAction extends TextAction {

        public PasteAction() {
            super(pasteAction);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                target.paste();
            }
        }
    }

    /// Replaces the selection with a line break.
    public static class InsertBreakAction extends TextAction {

        public InsertBreakAction() {
            super(insertBreakAction);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null && target.isEditable() && target.isEnabled()) {
                target.replaceSelection("\n");
            }
        }
    }

    /// Replaces the selection with a tab.
    public static class InsertTabAction extends TextAction {

        public InsertTabAction() {
            super(insertTabAction);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null && target.isEditable() && target.isEnabled()) {
                target.replaceSelection("\t");
            }
        }
    }

    /// Selects everything; the JDK keeps its class for this package private
    /// too.
    static final class Cn1SelectAll extends TextAction {

        Cn1SelectAll() {
            super(selectAllAction);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                target.selectAll();
            }
        }
    }
}
