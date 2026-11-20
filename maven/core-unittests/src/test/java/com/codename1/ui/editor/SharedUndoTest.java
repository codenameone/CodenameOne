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
package com.codename1.ui.editor;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphNode;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.graph.NodeGraph;
import com.codename1.ui.kanban.KanbanBoard;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.UndoHistory;

import static org.junit.jupiter.api.Assertions.*;

/// One history serving an editor and a graph: the application's single "Undo".
class SharedUndoTest extends UITestBase {
    private static final class Host implements EditorHost {
        public boolean isTextInputSupported() {
            return false;
        }

        public Object startTextInput(TextInputClient client, TextInputConfig config) {
            return null;
        }

        public void updateTextInputState(Object handle, TextInputState state) {
        }

        public void stopTextInput(Object handle) {
        }

        public void editorChanged() {
        }

        public void fireEditorEvent(String type, String value) {
        }
    }

    private UndoHistory history;
    private EditorView editor;
    private NodeGraph graph;

    private void show() {
        history = new UndoHistory();
        Form f = new Form("shared", BoxLayout.y());
        editor = new EditorView(new Host(), false);
        editor.setUndoHistory(history);
        DefaultGraphModel m = new DefaultGraphModel();
        m.addNode(new GraphNode("a", "A", 20, 20, 100, 50));
        graph = new NodeGraph(m);
        graph.setUndoHistory(history);
        graph.setPreferredH(300);
        f.add(editor);
        f.add(graph);
        f.show();
        for (int i = 0; i < 4; i++) {
            flushSerialCalls();
        }
        editor.setText("");
    }

    private void type(String s) {
        for (int i = 0; i < s.length(); i++) {
            editor.insertText(s.substring(i, i + 1));
        }
    }

    private void duplicateNode() {
        graph.select(graph.getModel().getNode("a"));
        assertTrue(graph.duplicateSelection());
    }

    @FormTest
    void everyComponentRecordsIntoTheApplicationsHistoryByDefault() {
        assertSame(UndoHistory.getGlobal(), UndoHistory.getGlobal());
        assertSame(UndoHistory.getGlobal(), new EditorView(new Host(), false).getUndoHistory());
        assertSame(UndoHistory.getGlobal(), new NodeGraph().getUndoHistory());
        assertSame(UndoHistory.getGlobal(), new KanbanBoard(new DefaultKanbanModel()).getUndoHistory());
    }

    @FormTest
    void oneUndoTakesBackTheLastChangeWhereverItWasMade() {
        show();
        type("abc");
        assertEquals(1, history.getUndoCount(), "a run of typing is one step");
        duplicateNode();
        assertEquals(2, graph.getModel().getNodes().size());
        assertEquals(2, history.getUndoCount());

        // the editor's own undo key takes back the graph's change: it was last
        assertTrue(editor.getText().length() == 3);
        editor.performUndo();
        assertEquals(1, graph.getModel().getNodes().size());
        assertEquals("abc", editor.getText());

        history.undo();
        assertEquals("", editor.getText());
        history.redo();
        history.redo();
        assertEquals("abc", editor.getText());
        assertEquals(2, graph.getModel().getNodes().size());
    }

    @FormTest
    void typingAfterAnotherComponentsChangeIsANewStep() {
        show();
        type("ab");
        duplicateNode();
        type("cd");
        assertEquals(3, history.getUndoCount());
        history.undo();
        assertEquals("ab", editor.getText(), "only the typing after the graph's change");
        history.undo();
        assertEquals(1, graph.getModel().getNodes().size());
        assertEquals("ab", editor.getText());
    }

    @FormTest
    void replacingOneComponentsDocumentKeepsTheOthersSteps() {
        show();
        type("ab");
        duplicateNode();
        editor.setText("fresh");
        assertEquals(1, history.getUndoCount(), "the replaced text's step went");
        assertTrue(history.undo());
        assertEquals(1, graph.getModel().getNodes().size());
        assertEquals("fresh", editor.getText());

        type("x");
        graph.setModel(new DefaultGraphModel());
        assertEquals(1, history.getUndoCount(), "the replaced model's steps went");
        history.undo();
        assertEquals("fresh", editor.getText());
    }

    @FormTest
    void undoingAChangeBringsItsComponentBackIntoFocus() {
        show();
        graph.setFocusable(true);
        editor.setFocusable(true);
        type("ab");
        duplicateNode();
        graph.requestFocus();
        history.undo();
        history.undo();
        assertSame(editor, Display.getInstance().getCurrent().getFocused());
    }

    @FormTest
    void anEditorWithNoHistoryUndoesOnItsOwn() {
        show();
        editor.setUndoHistory(null);
        type("ab");
        duplicateNode();
        editor.performUndo();
        assertEquals("", editor.getText());
        assertEquals(2, graph.getModel().getNodes().size());
        assertEquals(1, history.getUndoCount());
    }
}
