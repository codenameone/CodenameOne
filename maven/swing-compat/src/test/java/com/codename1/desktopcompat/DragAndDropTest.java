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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.datatransfer.DataFlavor;
import com.codename1.desktopcompat.java.awt.datatransfer.StringSelection;
import com.codename1.desktopcompat.java.awt.datatransfer.Transferable;
import com.codename1.desktopcompat.java.awt.datatransfer.UnsupportedFlavorException;
import com.codename1.desktopcompat.java.awt.dnd.DnDConstants;
import com.codename1.desktopcompat.java.awt.dnd.DropTarget;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetAdapter;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetDragEvent;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetDropEvent;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.DropMode;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.TransferHandler;
import com.codename1.desktopcompat.rt.Dnd;
import com.codename1.desktopcompat.rt.EventBridge;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/// Drag and drop: a drop target is told of a drag from outside the
/// application as it enters, moves and is let go, and gets its files; a
/// transfer handler turns a drag of a table's row into an import at the
/// place the row was let go.
public class DragAndDropTest extends WindowsTestBase {

    @After
    public void noDrag() {
        Dnd.reset();
    }

    /// The files of a drag from the desktop.
    private static final class Files implements Transferable {

        private final List<File> files;

        Files(File... files) {
            this.files = Arrays.asList(files);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return files;
        }
    }

    /// The place of a point of `c` in its window.
    private static int[] inWindow(Window w, Component c, int x, int y) {
        int wx = x;
        int wy = y;
        for (Component p = c; p != null && p != w; p = p.getParent()) {
            wx += p.getX();
            wy += p.getY();
        }
        return new int[]{wx, wy};
    }

    @Test
    public void aDropTargetGetsTheFilesLetGoOverIt() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JPanel target = new JPanel();
        f.add(target, BorderLayout.CENTER);
        final List<String> seen = new ArrayList<String>();
        final List<Object> dropped = new ArrayList<Object>();
        DropTarget dt = new DropTarget(target, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent e) {
                seen.add("enter");
                assertTrue(e.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
                e.acceptDrag(DnDConstants.ACTION_COPY);
            }

            @Override
            public void dragOver(DropTargetDragEvent e) {
                seen.add("over " + e.getLocation().x + "," + e.getLocation().y);
            }

            @Override
            public void dragExit(DropTargetEvent e) {
                seen.add("exit");
            }

            @Override
            public void drop(DropTargetDropEvent e) {
                seen.add("drop");
                e.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    dropped.add(e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
                    e.dropComplete(true);
                } catch (UnsupportedFlavorException | IOException bad) {
                    e.dropComplete(false);
                }
            }
        });
        show(f);
        assertSame(dt, target.getDropTarget());
        assertSame(target, dt.getComponent());
        Files files = new Files(new File("a.txt"), new File("b.txt"));
        int[] p = inWindow(f, target, 10, 12);
        int both = DnDConstants.ACTION_COPY_OR_MOVE;
        assertEquals(DnDConstants.ACTION_COPY, Dnd.over(f, p[0], p[1], files, both, DnDConstants.ACTION_COPY, false));
        assertEquals(DnDConstants.ACTION_COPY, Dnd.over(f, p[0] + 5, p[1], files, both, DnDConstants.ACTION_COPY,
                false));
        assertEquals(Arrays.asList("enter", "over 15,12"), seen);
        assertEquals(DnDConstants.ACTION_COPY, Dnd.drop(f, p[0] + 5, p[1], files, both, DnDConstants.ACTION_COPY,
                false));
        assertEquals("drop", seen.get(seen.size() - 1));
        assertEquals(1, dropped.size());
        assertEquals(Arrays.asList(new File("a.txt"), new File("b.txt")), dropped.get(0));
    }

    @Test
    public void aRefusedDragIsNotDropped() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JPanel target = new JPanel();
        f.add(target, BorderLayout.CENTER);
        final List<String> seen = new ArrayList<String>();
        new DropTarget(target, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent e) {
                e.rejectDrag();
            }

            @Override
            public void dragOver(DropTargetDragEvent e) {
                e.rejectDrag();
            }

            @Override
            public void dragExit(DropTargetEvent e) {
                seen.add("exit");
            }

            @Override
            public void drop(DropTargetDropEvent e) {
                seen.add("drop");
            }
        });
        show(f);
        int[] p = inWindow(f, target, 10, 12);
        Files files = new Files(new File("a.txt"));
        assertEquals(DnDConstants.ACTION_NONE, Dnd.over(f, p[0], p[1], files, DnDConstants.ACTION_COPY,
                DnDConstants.ACTION_COPY, false));
        assertEquals(DnDConstants.ACTION_NONE, Dnd.drop(f, p[0], p[1], files, DnDConstants.ACTION_COPY,
                DnDConstants.ACTION_COPY, false));
        assertEquals(Arrays.asList("exit"), seen);
    }

    @Test
    public void anInactiveTargetAndATargetThatAllowsAnotherActionSeeNothing() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JPanel target = new JPanel();
        f.add(target, BorderLayout.CENTER);
        final int[] entered = {0};
        DropTarget dt = new DropTarget(target, DnDConstants.ACTION_MOVE, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent e) {
                entered[0]++;
            }

            @Override
            public void drop(DropTargetDropEvent e) {
                e.rejectDrop();
            }
        });
        show(f);
        int[] p = inWindow(f, target, 10, 12);
        Files files = new Files(new File("a.txt"));
        // The source allows a copy alone, the target a move alone.
        assertEquals(DnDConstants.ACTION_NONE, Dnd.over(f, p[0], p[1], files, DnDConstants.ACTION_COPY,
                DnDConstants.ACTION_COPY, false));
        Dnd.exit();
        dt.setActive(false);
        int before = entered[0];
        assertEquals(DnDConstants.ACTION_NONE, Dnd.over(f, p[0], p[1], files, DnDConstants.ACTION_MOVE,
                DnDConstants.ACTION_MOVE, false));
        assertEquals(before, entered[0]);
    }

    /// Moves rows the way an application reorders a table.
    private static final class Rows extends TransferHandler {

        final List<String> log = new ArrayList<String>();
        JTable.DropLocation at;
        Object hovering;

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(String.valueOf(((JTable) c).getSelectedRow()));
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (support.isDrop() && support.getComponent() instanceof JTable) {
                hovering = support.getDropLocation();
            }
            return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            TransferHandler.DropLocation where = support.getDropLocation();
            if (!(where instanceof JTable.DropLocation)) {
                return false;
            }
            at = (JTable.DropLocation) where;
            try {
                log.add("import " + support.getTransferable().getTransferData(DataFlavor.stringFlavor) + " "
                        + support.getDropAction());
            } catch (UnsupportedFlavorException | IOException bad) {
                return false;
            }
            return true;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            log.add("done " + action);
        }
    }

    private static void pointer(JFrame f, int id, Component c, int x, int y) {
        int[] p = at(c, x, y);
        EventBridge.pointerEvent(f, id, p[0], p[1]);
    }

    @Test
    public void aRowDraggedInATableIsImportedWhereItIsLetGo() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JTable table = new JTable(new Object[][]{{"a"}, {"b"}, {"c"}, {"d"}, {"e"}}, new Object[]{"name"});
        Rows rows = new Rows();
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(rows);
        f.add(table, BorderLayout.CENTER);
        show(f);
        assertEquals(DropMode.INSERT_ROWS, table.getDropMode());
        assertSame(rows, table.getTransferHandler());
        assertNotNull("a transfer handler gives the component a drop target", table.getDropTarget());
        table.setRowSelectionInterval(0, 0);
        Rectangle from = table.getCellRect(0, 0, true);
        Rectangle to = table.getCellRect(3, 0, true);
        int x = from.x + 20;
        pointer(f, MouseEvent.MOUSE_PRESSED, table, x, from.y + from.height / 2);
        assertFalse(Dnd.dragging());
        pointer(f, MouseEvent.MOUSE_DRAGGED, table, x, from.y + from.height / 2 + 12);
        assertTrue("a drag past the threshold starts dragging the row", Dnd.dragging());
        // The upper part of the fourth row: before it.
        pointer(f, MouseEvent.MOUSE_DRAGGED, table, x, to.y + 2);
        assertTrue(rows.hovering instanceof JTable.DropLocation);
        assertNotNull("the place is shown while the drag is over it", table.getDropLocation());
        assertEquals(3, table.getDropLocation().getRow());
        pointer(f, MouseEvent.MOUSE_RELEASED, table, x, to.y + 2);
        assertFalse(Dnd.dragging());
        assertNull(table.getDropLocation());
        assertEquals(Arrays.asList("import 0 " + TransferHandler.MOVE, "done " + TransferHandler.MOVE), rows.log);
        assertTrue(rows.at.isInsertRow());
        assertFalse(rows.at.isInsertColumn());
        assertEquals(3, rows.at.getRow());
        assertEquals("the selection is what was dragged, not what it was dragged over", 0, table.getSelectedRow());
    }

    @Test
    public void aDropOnACellNamesTheCell() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JTable table = new JTable(new Object[][]{{"a"}, {"b"}, {"c"}}, new Object[]{"name"});
        Rows rows = new Rows();
        table.setDropMode(DropMode.ON);
        table.setTransferHandler(rows);
        f.add(table, BorderLayout.CENTER);
        show(f);
        Rectangle cell = table.getCellRect(1, 0, true);
        int[] p = inWindow(f, table, cell.x + 10, cell.y + cell.height / 2);
        StringSelection data = new StringSelection("x");
        assertEquals(DnDConstants.ACTION_COPY, Dnd.drop(f, p[0], p[1], data, DnDConstants.ACTION_COPY,
                DnDConstants.ACTION_COPY, false));
        assertEquals(1, rows.at.getRow());
        assertEquals(0, rows.at.getColumn());
        assertFalse(rows.at.isInsertRow());
        assertEquals(Arrays.asList("import x " + TransferHandler.COPY), rows.log);
    }

    @Test
    public void theModifierKeysNameTheAction() {
        int all = DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK;
        assertEquals(DnDConstants.ACTION_MOVE, Dnd.userAction(0, all));
        assertEquals(DnDConstants.ACTION_COPY, Dnd.userAction(0, DnDConstants.ACTION_COPY));
        assertEquals(DnDConstants.ACTION_COPY, Dnd.userAction(MouseEvent.CTRL_DOWN_MASK, all));
        assertEquals(DnDConstants.ACTION_LINK,
                Dnd.userAction(MouseEvent.CTRL_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK, all));
        assertEquals(DnDConstants.ACTION_NONE, Dnd.userAction(MouseEvent.CTRL_DOWN_MASK, DnDConstants.ACTION_MOVE));
    }
}
