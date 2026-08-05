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

package com.codename1.guibuilder;

import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.editor.CodePureEditor;
import com.codename1.ui.editor.CodeView;
import com.codename1.ui.editor.EditorHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorInteractionTest {
    @Test
    void pureEditorAcceptsUserEditsUndoRedoAndProtectsOnlyGeneratedBlocks() {
        CodePureEditor editor = new CodePureEditor(new TestEditorHost(), "code");
        CodeView view = (CodeView) editor.getView();
        String source = "// <generated>\nfinal int locked = 1;\n// </generated>\n"
                + "// <user>\nvoid handler() { }\n// </user>\n";
        editor.cmd("setText", source);
        editor.cmd("setProtectedMarkers", "// <generated>\n// </generated>");

        int userOffset = source.indexOf("void handler");
        view.replaceRange(userOffset, userOffset, "public ");
        assertTrue(editor.query("getText", null).contains("public void handler"),
                "typing in the user region must mutate the document");

        editor.cmd("undo", null);
        assertFalse(editor.query("getText", null).contains("public void handler"),
                "source and CSS edits must enter the editor-local undo buffer");
        editor.cmd("redo", null);
        assertTrue(editor.query("getText", null).contains("public void handler"));

        String beforeProtectedAttempt = editor.query("getText", null);
        int generatedOffset = beforeProtectedAttempt.indexOf("final int");
        view.replaceRange(generatedOffset, generatedOffset, "BROKEN ");
        assertEquals(beforeProtectedAttempt, editor.query("getText", null),
                "generated regions alone must reject edits");
    }

    @Test
    void guiBuilderCanMoveThePureEditorCaretToAnExactSourceOffset() {
        CodePureEditor editor = new CodePureEditor(new TestEditorHost(), "code");
        editor.cmd("setText", "alpha beta gamma");
        editor.cmd("setCursor", "11");
        assertEquals("11", editor.query("getCursor", null));

        editor.cmd("setCursor", "999");
        assertEquals("16", editor.query("getCursor", null), "caret must clamp to the document");
    }

    private static final class TestEditorHost implements EditorHost {
        @Override
        public boolean isTextInputSupported() {
            return false;
        }

        @Override
        public Object startTextInput(TextInputClient client, TextInputConfig config) {
            return null;
        }

        @Override
        public void updateTextInputState(Object handle, TextInputState state) {
        }

        @Override
        public void stopTextInput(Object handle) {
        }

        @Override
        public void editorChanged() {
        }

        @Override
        public void fireEditorEvent(String type, String value) {
        }
    }
}
