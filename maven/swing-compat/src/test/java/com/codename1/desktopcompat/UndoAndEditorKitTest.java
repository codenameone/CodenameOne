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
package com.codename1.desktopcompat;

import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.JTextArea;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.event.UndoableEditEvent;
import com.codename1.desktopcompat.javax.swing.event.UndoableEditListener;
import com.codename1.desktopcompat.javax.swing.text.DefaultEditorKit;
import com.codename1.desktopcompat.javax.swing.text.Document;
import com.codename1.desktopcompat.javax.swing.text.PlainDocument;
import com.codename1.desktopcompat.javax.swing.text.Segment;
import com.codename1.desktopcompat.javax.swing.text.TextAction;
import com.codename1.desktopcompat.javax.swing.undo.AbstractUndoableEdit;
import com.codename1.desktopcompat.javax.swing.undo.CannotRedoException;
import com.codename1.desktopcompat.javax.swing.undo.CannotUndoException;
import com.codename1.desktopcompat.javax.swing.undo.CompoundEdit;
import com.codename1.desktopcompat.javax.swing.undo.UndoManager;
import com.codename1.desktopcompat.javax.swing.undo.UndoableEditSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Undo over a document, the editor kit's actions and the pieces of
/// `javax.swing.undo` an editor is written against.
public class UndoAndEditorKitTest extends KernelTestBase {

    /// An edit that records what was done to it.
    private static final class Marked extends AbstractUndoableEdit {
        private final String name;
        private final List<String> log;

        Marked(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public void undo() {
            super.undo();
            log.add("undo " + name);
        }

        @Override
        public void redo() {
            super.redo();
            log.add("redo " + name);
        }

        @Override
        public String getPresentationName() {
            return name;
        }
    }

    @Test
    public void aManagerUndoesTheLastEditFirstAndRedoesInOrder() {
        List<String> log = new ArrayList<String>();
        UndoManager m = new UndoManager();
        assertFalse(m.canUndo());
        assertFalse(m.canRedo());
        m.addEdit(new Marked("a", log));
        m.addEdit(new Marked("b", log));
        assertTrue(m.canUndo());
        assertEquals("Undo b", m.getUndoPresentationName());
        m.undo();
        assertEquals("Redo b", m.getRedoPresentationName());
        assertEquals("Undo a", m.getUndoPresentationName());
        m.undo();
        assertFalse(m.canUndo());
        m.redo();
        m.redo();
        assertFalse(m.canRedo());
        assertEquals("[undo b, undo a, redo a, redo b]", log.toString());
    }

    @Test
    public void undoingWithNothingToUndoThrows() {
        UndoManager m = new UndoManager();
        try {
            m.undo();
            fail("nothing to undo");
        } catch (CannotUndoException expected) {
            assertFalse(m.canUndo());
        }
        try {
            m.redo();
            fail("nothing to redo");
        } catch (CannotRedoException expected) {
            assertFalse(m.canRedo());
        }
    }

    @Test
    public void aNewEditDropsWhatCouldBeRedone() {
        List<String> log = new ArrayList<String>();
        UndoManager m = new UndoManager();
        m.addEdit(new Marked("a", log));
        m.addEdit(new Marked("b", log));
        m.undo();
        m.addEdit(new Marked("c", log));
        assertFalse(m.canRedo());
        assertEquals("Undo c", m.getUndoPresentationName());
    }

    @Test
    public void theLimitKeepsTheNewestEdits() {
        List<String> log = new ArrayList<String>();
        UndoManager m = new UndoManager();
        m.setLimit(2);
        m.addEdit(new Marked("a", log));
        m.addEdit(new Marked("b", log));
        m.addEdit(new Marked("c", log));
        m.undo();
        m.undo();
        assertFalse(m.canUndo());
        assertEquals("[undo c, undo b]", log.toString());
        m.discardAllEdits();
        assertFalse(m.canRedo());
    }

    @Test
    public void aCompoundEditUndoesItsPartsBackwardsOnceEnded() {
        List<String> log = new ArrayList<String>();
        CompoundEdit c = new CompoundEdit();
        c.addEdit(new Marked("a", log));
        c.addEdit(new Marked("b", log));
        assertTrue(c.isInProgress());
        assertFalse(c.canUndo());
        c.end();
        assertFalse(c.addEdit(new Marked("late", log)));
        assertTrue(c.canUndo());
        c.undo();
        c.redo();
        assertEquals("[undo b, undo a, redo a, redo b]", log.toString());
        assertEquals("b", c.getPresentationName());
    }

    @Test
    public void editSupportGroupsAnUpdateIntoOneEdit() {
        final List<UndoableEditEvent> heard = new ArrayList<UndoableEditEvent>();
        UndoableEditSupport s = new UndoableEditSupport(this);
        s.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                heard.add(e);
            }
        });
        List<String> log = new ArrayList<String>();
        s.postEdit(new Marked("alone", log));
        assertEquals(1, heard.size());
        assertSame(this, heard.get(0).getSource());
        s.beginUpdate();
        s.postEdit(new Marked("a", log));
        s.postEdit(new Marked("b", log));
        assertEquals(1, heard.size());
        s.endUpdate();
        assertEquals(2, heard.size());
        assertTrue(heard.get(1).getEdit() instanceof CompoundEdit);
    }

    @Test
    public void aDocumentChangeIsAnEditThatTakesTheTextBack() throws Exception {
        PlainDocument doc = new PlainDocument();
        UndoManager m = new UndoManager();
        doc.addUndoableEditListener(m);
        doc.insertString(0, "hello", null);
        doc.insertString(5, " world", null);
        doc.remove(0, 6);
        assertEquals("world", doc.getText(0, doc.getLength()));
        assertEquals("Undo deletion", m.getUndoPresentationName());
        m.undo();
        assertEquals("hello world", doc.getText(0, doc.getLength()));
        assertEquals("Undo addition", m.getUndoPresentationName());
        m.undo();
        assertEquals("hello", doc.getText(0, doc.getLength()));
        m.undo();
        assertEquals(0, doc.getLength());
        assertFalse(m.canUndo());
        m.redo();
        m.redo();
        m.redo();
        assertEquals("world", doc.getText(0, doc.getLength()));
        assertFalse(m.canRedo());
    }

    @Test
    public void undoingPostsNoEditOfItsOwnAndStopsWithTheListener() throws Exception {
        PlainDocument doc = new PlainDocument();
        final int[] heard = new int[1];
        UndoableEditListener count = new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                heard[0]++;
            }
        };
        UndoManager m = new UndoManager();
        doc.addUndoableEditListener(m);
        doc.addUndoableEditListener(count);
        doc.insertString(0, "abc", null);
        m.undo();
        m.redo();
        assertEquals(1, heard[0]);
        assertEquals(2, doc.getUndoableEditListeners().length);
        doc.removeUndoableEditListener(count);
        doc.insertString(0, "x", null);
        assertEquals(1, heard[0]);
    }

    @Test
    public void typingInATextAreaIsUndoneInTheWidgetToo() throws Exception {
        JTextArea area = new JTextArea();
        UndoManager m = new UndoManager();
        area.getDocument().addUndoableEditListener(m);
        area.setText("one");
        area.append(" two");
        assertEquals("one two", area.getText());
        m.undo();
        assertEquals("one", area.getText());
    }

    @Test
    public void aSegmentIsFilledWithTheRangeAskedFor() throws Exception {
        Document doc = new PlainDocument();
        doc.insertString(0, "0123456789", null);
        Segment s = new Segment();
        doc.getText(2, 5, s);
        assertEquals("23456", s.toString());
        assertEquals(5, s.length());
        assertEquals('3', s.charAt(1));
        assertEquals("34", s.subSequence(1, 3).toString());
        assertEquals("bc", new Segment("abcd".toCharArray(), 1, 2).toString());
        assertEquals("", new Segment().toString());
    }

    private static Action named(Action[] all, String name) {
        for (Action a : all) {
            if (name.equals(a.getValue(Action.NAME))) {
                return a;
            }
        }
        return null;
    }

    @Test
    public void aTextComponentAnswersItsEditingActionsByName() {
        JTextArea area = new JTextArea("some text");
        Action[] all = area.getActions();
        assertNotNull(named(all, DefaultEditorKit.cutAction));
        assertNotNull(named(all, DefaultEditorKit.copyAction));
        assertNotNull(named(all, DefaultEditorKit.pasteAction));
        assertNotNull(named(all, DefaultEditorKit.selectAllAction));
        assertNotNull(named(all, DefaultEditorKit.insertBreakAction));
        assertNotNull(named(all, DefaultEditorKit.insertTabAction));
    }

    @Test
    public void cutCopyAndPasteWorkOnTheComponentTheEventNames() {
        JTextArea from = new JTextArea("alpha beta");
        JTextArea to = new JTextArea("");
        Action[] all = from.getActions();
        from.select(0, 5);
        named(all, DefaultEditorKit.copyAction).actionPerformed(new ActionEvent(from, ActionEvent.ACTION_PERFORMED, ""));
        assertEquals("alpha beta", from.getText());
        named(all, DefaultEditorKit.pasteAction).actionPerformed(new ActionEvent(to, ActionEvent.ACTION_PERFORMED, ""));
        assertEquals("alpha", to.getText());
        from.select(5, 10);
        named(all, DefaultEditorKit.cutAction).actionPerformed(new ActionEvent(from, ActionEvent.ACTION_PERFORMED, ""));
        assertEquals("alpha", from.getText());
        named(all, DefaultEditorKit.selectAllAction).actionPerformed(new ActionEvent(to, ActionEvent.ACTION_PERFORMED, ""));
        assertEquals("alpha", to.getSelectedText());
    }

    @Test
    public void insertBreakAndTabReplaceTheSelection() {
        JTextArea area = new JTextArea("ab");
        Action[] all = area.getActions();
        area.select(1, 1);
        named(all, DefaultEditorKit.insertBreakAction).actionPerformed(new ActionEvent(area, ActionEvent.ACTION_PERFORMED, ""));
        assertEquals("a\nb", area.getText());
        area.select(0, 1);
        named(all, DefaultEditorKit.insertTabAction).actionPerformed(new ActionEvent(area, ActionEvent.ACTION_PERFORMED, ""));
        assertEquals("\t\nb", area.getText());
    }

    @Test
    public void augmentingAListReplacesActionsOfTheSameName() {
        Action[] kit = new DefaultEditorKit().getActions();
        Action mine = new DefaultEditorKit.CopyAction();
        Action[] merged = TextAction.augmentList(kit, new Action[] {mine});
        assertEquals(kit.length, merged.length);
        assertSame(mine, named(merged, DefaultEditorKit.copyAction));
    }

    @Test
    public void dragEnabledIsAPropertyOfTheComponentsThatHaveIt() {
        JTextArea area = new JTextArea();
        assertFalse(area.getDragEnabled());
        area.setDragEnabled(true);
        assertTrue(area.getDragEnabled());
        JTree tree = new JTree();
        assertFalse(tree.getDragEnabled());
        tree.setDragEnabled(true);
        assertTrue(tree.getDragEnabled());
    }
}
