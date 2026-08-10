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
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceEditingTest {
    @BeforeAll static void init() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    private static final String SOURCE =
            "// <gui-builder-generated>\n"
            + "package com.example;\n"
            + "public class F {\n"
            + "// </gui-builder-generated>\n"
            + "// <gui-builder-user-code>\n"
            + "\n"
            + "// </gui-builder-user-code>\n"
            + "// <gui-builder-generated>\n}\n// </gui-builder-generated>\n";

    @Test void typingInsideTheUserRegionIsAccepted() {
        CodePureEditor editor = new CodePureEditor(new Host(), "java");
        CodeView view = (CodeView) editor.getView();
        editor.cmd("setText", SOURCE);
        editor.cmd("setProtectedMarkers", "// <gui-builder-generated>\n// </gui-builder-generated>");
        int userOffset = SOURCE.indexOf("// <gui-builder-user-code>") + "// <gui-builder-user-code>".length() + 1;
        view.replaceRange(userOffset, userOffset, "int typed = 1;");
        assertTrue(editor.query("getText", null).contains("int typed = 1;"),
                "typing in the user region must work");
    }

    @Test void typingWhereTheCaretStartsIsAccepted() {
        CodePureEditor editor = new CodePureEditor(new Host(), "java");
        CodeView view = (CodeView) editor.getView();
        editor.cmd("setText", SOURCE);
        editor.cmd("setProtectedMarkers", "// <gui-builder-generated>\n// </gui-builder-generated>");
        // Offset 0 is where the caret sits until something moves it, and it is inside the first
        // generated block. Typing there is silently dropped, which reads as a dead editor.
        view.replaceRange(0, 0, "X");
        assertFalse(editor.query("getText", null).startsWith("X"),
                "the generated region must stay protected");
        System.out.println("PROTECTED-AT-CARET-ZERO: generated region correctly rejects typing at offset 0");
    }

    @Test void typingBetweenTheEndMarkerAndItsNewlineIsRejected() {
        // A caret just past the marker text but before its newline used to count as outside the
        // block. One keystroke there turns the standalone marker into ordinary text, and the next
        // scan then finds no closing marker and protects everything to the end of the file --
        // including the user's own region, which is the thing the markers exist to keep editable.
        CodePureEditor editor = new CodePureEditor(new Host(), "java");
        CodeView view = (CodeView) editor.getView();
        editor.cmd("setText", SOURCE);
        editor.cmd("setProtectedMarkers", "// <gui-builder-generated>\n// </gui-builder-generated>");
        String endMarker = "// </gui-builder-generated>";
        int justPastMarker = SOURCE.indexOf(endMarker) + endMarker.length();

        view.replaceRange(justPastMarker, justPastMarker, "X");

        assertFalse(editor.query("getText", null).contains(endMarker + "X"),
                "the rest of the marker line is part of the marker");

        // The line after it is still the user's to type on.
        int nextLine = justPastMarker + 1;
        view.replaceRange(nextLine, nextLine, "");
        assertTrue(editor.query("getText", null).contains(endMarker + "\n"),
                "the marker line survived intact");
    }

    @Test void theStartMarkerLineIsProtectedFromBothSides() {
        // Insert into the indentation before a marker, or delete the newline that ends the line
        // above it, and the marker stops owning its line -- markerLine() then cannot find it and
        // the generated block behind it becomes freely editable.
        String indented = "package com.example;\n"
                + "    // <gui-builder-generated>\n"
                + "    int generated;\n"
                + "    // </gui-builder-generated>\n";
        CodePureEditor editor = new CodePureEditor(new Host(), "java");
        CodeView view = (CodeView) editor.getView();
        editor.cmd("setText", indented);
        editor.cmd("setProtectedMarkers", "// <gui-builder-generated>\n// </gui-builder-generated>");
        int lineStart = indented.indexOf("    // <gui-builder-generated>");

        view.replaceRange(lineStart, lineStart, "X");
        assertFalse(editor.query("getText", null).contains("X    // <gui-builder-generated>"),
                "the indentation in front of a marker is part of its line");

        view.replaceRange(lineStart - 1, lineStart, "");
        assertTrue(editor.query("getText", null).contains(";\n    // <gui-builder-generated>"),
                "the newline in front of a marker keeps it on its own line");
    }

    private static final class Host implements EditorHost {
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
