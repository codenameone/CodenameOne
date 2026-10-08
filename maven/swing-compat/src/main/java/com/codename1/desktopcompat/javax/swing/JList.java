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

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.event.ListDataEvent;
import com.codename1.desktopcompat.javax.swing.event.ListDataListener;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/// A list of rows, each painted by a cell renderer.
///
/// ## How the rows are painted
///
/// The list draws itself: for every row in the clip it asks the renderer
/// for a component, gives it the bounds of the row and paints it there, as
/// the desktop does. The renderer's component is never added to the list,
/// so any component works, including one that draws in `paintComponent`.
///
/// ## What differs from the desktop
///
/// A row is selected by a click, not by the press that starts it, because
/// on a touch screen a press is also how a scroll begins.
///
/// ## Layout orientations
///
/// `VERTICAL` is one column of rows, each as high as its renderer asks.
/// The two wrapping orientations are a grid of cells of one size, the
/// widest by the highest of the renderer's sizes or the fixed cell size:
/// `HORIZONTAL_WRAP` fills a row and goes on in the next, `VERTICAL_WRAP`
/// fills a column and goes on in the next. A visible row count above zero
/// is the number of rows of the grid; with zero or less the grid takes as
/// many columns (or rows) as the list's width (or height) holds, and a
/// scroll pane then scrolls it only the other way. Left and right move
/// the selection by a column in a grid, up and down by a row.
public class JList<E> extends JComponent implements Scrollable {

    public static final int VERTICAL = 0;

    public static final int VERTICAL_WRAP = 1;

    public static final int HORIZONTAL_WRAP = 2;

    private ListModel<E> dataModel;
    private ListSelectionModel selectionModel;
    private ListCellRenderer<? super E> cellRenderer = new DefaultListCellRenderer.UIResource();
    private final ArrayList<ListSelectionListener> selectionListeners = new ArrayList<ListSelectionListener>();
    private int fixedCellWidth = -1;
    private int fixedCellHeight = -1;
    private int visibleRowCount = 8;
    private int layoutOrientation = VERTICAL;
    private E prototypeCellValue;
    private Color selectionForeground;
    private Color selectionBackground;

    /// The top of every row and, last, the bottom of the list; `null` when
    /// the rows have to be measured again.
    private int[] rowTops;
    private int cellWidth;
    /// The height of the highest row, the height of a cell of the grid.
    private int cellHeight;

    private final ListDataListener dataHandler = new ListDataListener() {
        @Override
        public void intervalAdded(ListDataEvent e) {
            int min = e.getIndex0();
            int max = e.getIndex1();
            if (min >= 0 && max >= min) {
                selectionModel.insertIndexInterval(min, max - min + 1, true);
            }
            cn1RowsChanged();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            if (e.getIndex0() >= 0) {
                selectionModel.removeIndexInterval(e.getIndex0(), e.getIndex1());
            }
            cn1RowsChanged();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            cn1RowsChanged();
        }
    };

    private final ListSelectionListener selectionHandler = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            fireSelectionValueChanged(e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting());
            repaint();
        }
    };

    public JList(ListModel<E> dataModel) {
        if (dataModel == null) {
            throw new IllegalArgumentException("dataModel must be non null");
        }
        this.dataModel = dataModel;
        dataModel.addListDataListener(dataHandler);
        selectionModel = createSelectionModel();
        selectionModel.addListSelectionListener(selectionHandler);
        setOpaque(true);
        Color bg = UIManager.getColor("List.background");
        setBackground(bg == null ? Color.WHITE : bg);
        Color fg = UIManager.getColor("List.foreground");
        setForeground(fg == null ? Color.BLACK : fg);
        Color sb = UIManager.getColor("List.selectionBackground");
        selectionBackground = sb == null ? new Color(0x38, 0x75, 0xd7) : sb;
        Color sf = UIManager.getColor("List.selectionForeground");
        selectionForeground = sf == null ? Color.WHITE : sf;
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        bind(KeyEvent.VK_UP, "selectPreviousRow", Move.PREVIOUS);
        bind(KeyEvent.VK_DOWN, "selectNextRow", Move.NEXT);
        bind(KeyEvent.VK_HOME, "selectFirstRow", Move.FIRST);
        bind(KeyEvent.VK_END, "selectLastRow", Move.LAST);
        bind(KeyEvent.VK_PAGE_UP, "scrollUp", Move.PAGE_UP);
        bind(KeyEvent.VK_PAGE_DOWN, "scrollDown", Move.PAGE_DOWN);
        bind(KeyEvent.VK_LEFT, "selectPreviousColumn", Move.LEFT);
        bind(KeyEvent.VK_RIGHT, "selectNextColumn", Move.RIGHT);
    }

    private void bind(int key, String name, int kind) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new Move(this, kind));
    }

    /// Moves the selection with the keyboard: one row, one page, or to
    /// either end. The bindings are in the input map under the names the
    /// desktop uses (`selectNextRow` and so on).
    private static final class Move extends AbstractAction {

        static final int PREVIOUS = 0;
        static final int NEXT = 1;
        static final int FIRST = 2;
        static final int LAST = 3;
        static final int PAGE_UP = 4;
        static final int PAGE_DOWN = 5;
        static final int LEFT = 6;
        static final int RIGHT = 7;

        private final JList<?> list;
        private final int kind;

        Move(JList<?> list, int kind) {
            this.list = list;
            this.kind = kind;
        }

        @Override
        public void actionPerformed(com.codename1.desktopcompat.java.awt.event.ActionEvent e) {
            int n = list.getModel().getSize();
            if (n == 0) {
                return;
            }
            int lead = list.getLeadSelectionIndex();
            int page = Math.max(1, list.getLastVisibleIndex() - list.getFirstVisibleIndex());
            // In a grid one step up or down, or left or right, is this
            // many indices; in a column there are no steps sideways.
            int[] grid = list.cn1Wrapped() ? list.grid() : null;
            int down = grid == null ? 1 : list.getLayoutOrientation() == HORIZONTAL_WRAP ? grid[1] : 1;
            int across = grid == null ? 0 : list.getLayoutOrientation() == HORIZONTAL_WRAP ? 1 : grid[0];
            int to;
            switch (kind) {
                case PREVIOUS:
                    to = lead < 0 ? n - 1 : lead - down < 0 ? lead : lead - down;
                    break;
                case NEXT:
                    to = lead < 0 ? 0 : lead + down >= n ? lead : lead + down;
                    break;
                case LEFT:
                    if (across == 0) {
                        return;
                    }
                    to = lead < 0 ? 0 : lead - across < 0 ? lead : lead - across;
                    break;
                case RIGHT:
                    if (across == 0) {
                        return;
                    }
                    to = lead < 0 ? 0 : lead + across >= n ? lead : lead + across;
                    break;
                case FIRST:
                    to = 0;
                    break;
                case LAST:
                    to = n - 1;
                    break;
                case PAGE_UP:
                    to = lead < 0 ? 0 : lead - page;
                    break;
                default:
                    to = lead < 0 ? 0 : lead + page;
                    break;
            }
            to = Math.max(0, Math.min(n - 1, to));
            list.setSelectedIndex(to);
            list.ensureIndexIsVisible(to);
        }
    }

    public JList(final E[] listData) {
        this(new AbstractListModel<E>() {
            @Override
            public int getSize() {
                return listData.length;
            }

            @Override
            public E getElementAt(int i) {
                return listData[i];
            }
        });
    }

    public JList(final Vector<? extends E> listData) {
        this(new AbstractListModel<E>() {
            @Override
            public int getSize() {
                return listData.size();
            }

            @Override
            public E getElementAt(int i) {
                return listData.elementAt(i);
            }
        });
    }

    public JList() {
        this(new AbstractListModel<E>() {
            @Override
            public int getSize() {
                return 0;
            }

            @Override
            public E getElementAt(int i) {
                throw new IndexOutOfBoundsException("No Data Model");
            }
        });
    }

    // ------------------------------------------------------------ model

    public ListModel<E> getModel() {
        return dataModel;
    }

    public void setModel(ListModel<E> model) {
        if (model == null) {
            throw new IllegalArgumentException("model must be non null");
        }
        ListModel<E> old = dataModel;
        old.removeListDataListener(dataHandler);
        dataModel = model;
        model.addListDataListener(dataHandler);
        clearSelection();
        firePropertyChange("model", old, model);
        cn1RowsChanged();
    }

    public void setListData(final E[] listData) {
        setModel(new AbstractListModel<E>() {
            @Override
            public int getSize() {
                return listData.length;
            }

            @Override
            public E getElementAt(int i) {
                return listData[i];
            }
        });
    }

    public void setListData(final Vector<? extends E> listData) {
        setModel(new AbstractListModel<E>() {
            @Override
            public int getSize() {
                return listData.size();
            }

            @Override
            public E getElementAt(int i) {
                return listData.elementAt(i);
            }
        });
    }

    // ------------------------------------------------------------ rendering

    public ListCellRenderer<? super E> getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(ListCellRenderer<? super E> cellRenderer) {
        ListCellRenderer<? super E> old = this.cellRenderer;
        this.cellRenderer = cellRenderer;
        if (prototypeCellValue != null) {
            cn1MeasurePrototype();
        }
        firePropertyChange("cellRenderer", old, cellRenderer);
        cn1RowsChanged();
    }

    public E getPrototypeCellValue() {
        return prototypeCellValue;
    }

    /// Measures the renderer once with this value and uses the result as
    /// the fixed width and height of every cell.
    public void setPrototypeCellValue(E prototypeCellValue) {
        E old = this.prototypeCellValue;
        this.prototypeCellValue = prototypeCellValue;
        if (prototypeCellValue != null) {
            cn1MeasurePrototype();
        }
        firePropertyChange("prototypeCellValue", old, prototypeCellValue);
        cn1RowsChanged();
    }

    private void cn1MeasurePrototype() {
        if (cellRenderer == null) {
            return;
        }
        Component c = cellRenderer.getListCellRendererComponent(this, prototypeCellValue, 0, false, false);
        Dimension d = c.getPreferredSize();
        fixedCellWidth = d.width;
        fixedCellHeight = d.height;
    }

    public int getFixedCellWidth() {
        return fixedCellWidth;
    }

    public void setFixedCellWidth(int width) {
        int old = fixedCellWidth;
        fixedCellWidth = width;
        firePropertyChange("fixedCellWidth", old, width);
        cn1RowsChanged();
    }

    public int getFixedCellHeight() {
        return fixedCellHeight;
    }

    public void setFixedCellHeight(int height) {
        int old = fixedCellHeight;
        fixedCellHeight = height;
        firePropertyChange("fixedCellHeight", old, height);
        cn1RowsChanged();
    }

    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    public void setVisibleRowCount(int visibleRowCount) {
        int old = this.visibleRowCount;
        this.visibleRowCount = Math.max(0, visibleRowCount);
        firePropertyChange("visibleRowCount", old, visibleRowCount);
        revalidate();
    }

    public int getLayoutOrientation() {
        return layoutOrientation;
    }

    public void setLayoutOrientation(int layoutOrientation) {
        if (layoutOrientation != VERTICAL && layoutOrientation != VERTICAL_WRAP
                && layoutOrientation != HORIZONTAL_WRAP) {
            throw new IllegalArgumentException("layoutOrientation must be one of: VERTICAL, HORIZONTAL_WRAP or VERTICAL_WRAP");
        }
        int old = this.layoutOrientation;
        this.layoutOrientation = layoutOrientation;
        firePropertyChange("layoutOrientation", old, layoutOrientation);
        if (old != layoutOrientation) {
            cn1RowsChanged();
        }
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public void setSelectionForeground(Color selectionForeground) {
        Color old = this.selectionForeground;
        this.selectionForeground = selectionForeground;
        firePropertyChange("selectionForeground", old, selectionForeground);
        repaint();
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        Color old = this.selectionBackground;
        this.selectionBackground = selectionBackground;
        firePropertyChange("selectionBackground", old, selectionBackground);
        repaint();
    }

    // ------------------------------------------------------------ geometry

    private void cn1RowsChanged() {
        rowTops = null;
        revalidate();
        repaint();
    }

    /// Measures the rows if something changed since they were measured.
    private int[] rows() {
        if (rowTops != null) {
            return rowTops;
        }
        int n = dataModel.getSize();
        int[] tops = new int[n + 1];
        int widest = 0;
        int highest = 0;
        boolean measure = fixedCellHeight <= 0 || fixedCellWidth <= 0;
        int y = 0;
        for (int i = 0; i < n; i++) {
            tops[i] = y;
            int h = fixedCellHeight;
            if (measure && cellRenderer != null) {
                Component c = cellRenderer.getListCellRendererComponent(this, dataModel.getElementAt(i), i, false,
                        false);
                Dimension d = c.getPreferredSize();
                widest = Math.max(widest, d.width);
                if (fixedCellHeight <= 0) {
                    h = d.height;
                }
            }
            y += Math.max(0, h);
            highest = Math.max(highest, h);
        }
        tops[n] = y;
        cellWidth = fixedCellWidth > 0 ? fixedCellWidth : widest;
        cellHeight = fixedCellHeight > 0 ? fixedCellHeight : highest;
        rowTops = tops;
        return tops;
    }

    /// Whether the cells are laid out as a grid.
    boolean cn1Wrapped() {
        return layoutOrientation != VERTICAL;
    }

    /// The grid of a wrapping orientation: the rows in a column, then the
    /// columns. The desktop's rule: a visible row count above zero is the
    /// number of rows, and the columns follow from the number of cells;
    /// otherwise the list's own width or height says how many fit.
    int[] grid() {
        rows();
        int n = dataModel.getSize();
        Insets in = getInsets();
        int rowsPer;
        int cols;
        if (visibleRowCount > 0) {
            rowsPer = visibleRowCount;
            cols = Math.max(1, n / rowsPer);
            if (n > rowsPer && n % rowsPer != 0) {
                cols++;
            }
            if (layoutOrientation == HORIZONTAL_WRAP) {
                rowsPer = n / cols;
                if (n % cols > 0) {
                    rowsPer++;
                }
            }
        } else if (layoutOrientation == HORIZONTAL_WRAP) {
            cols = Math.max(1, (getWidth() - in.left - in.right) / Math.max(1, cellWidth));
            rowsPer = n / cols;
            if (n % cols > 0) {
                rowsPer++;
            }
        } else {
            rowsPer = Math.max(1, (getHeight() - in.top - in.bottom) / Math.max(1, cellHeight));
            cols = n / rowsPer;
            if (n % rowsPer > 0) {
                cols++;
            }
        }
        return new int[]{Math.max(1, rowsPer), Math.max(1, cols)};
    }

    /// The bounds of one cell of the grid.
    private Rectangle gridCell(int index, int[] grid) {
        Insets in = getInsets();
        int row;
        int col;
        if (layoutOrientation == HORIZONTAL_WRAP) {
            row = index / grid[1];
            col = index % grid[1];
        } else {
            col = index / grid[0];
            row = index % grid[0];
        }
        return new Rectangle(in.left + col * cellWidth, in.top + row * cellHeight, cellWidth, cellHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        int[] tops = rows();
        Insets in = getInsets();
        if (cn1Wrapped()) {
            if (tops.length == 1) {
                return new Dimension(in.left + in.right, in.top + in.bottom);
            }
            int[] grid = grid();
            return new Dimension(grid[1] * cellWidth + in.left + in.right,
                    grid[0] * cellHeight + in.top + in.bottom);
        }
        return new Dimension(cellWidth + in.left + in.right, tops[tops.length - 1] + in.top + in.bottom);
    }

    /// The row at a point of the list, or the nearest one; `-1` only when
    /// the list is empty.
    public int locationToIndex(Point location) {
        int[] tops = rows();
        int n = tops.length - 1;
        if (n == 0) {
            return -1;
        }
        if (cn1Wrapped()) {
            int[] grid = grid();
            Insets in = getInsets();
            int col = Math.max(0, Math.min(grid[1] - 1, (location.x - in.left) / Math.max(1, cellWidth)));
            int row = Math.max(0, (location.y - in.top) / Math.max(1, cellHeight));
            // The desktop's rule: below the last row of a grid filled row
            // by row is the last cell, while a grid filled column by
            // column answers the last row of the column under the point.
            if (layoutOrientation == HORIZONTAL_WRAP) {
                return Math.min(n - 1, Math.min(row, n - 1) * grid[1] + col);
            }
            return Math.min(n - 1, col * grid[0] + Math.min(row, grid[0] - 1));
        }
        int y = location.y - getInsets().top;
        if (y < 0) {
            return 0;
        }
        for (int i = 0; i < n; i++) {
            if (y < tops[i + 1]) {
                return i;
            }
        }
        return n - 1;
    }

    public Point indexToLocation(int index) {
        Rectangle r = getCellBounds(index, index);
        return r == null ? null : new Point(r.x, r.y);
    }

    /// The bounds of the rows from one index to the other, whichever is
    /// the smaller; `null` when either is not a row.
    public Rectangle getCellBounds(int index0, int index1) {
        int[] tops = rows();
        int n = tops.length - 1;
        int min = Math.min(index0, index1);
        int max = Math.max(index0, index1);
        if (min < 0 || max >= n) {
            return null;
        }
        if (cn1Wrapped()) {
            int[] grid = grid();
            Rectangle a = gridCell(min, grid);
            return min == max ? a : a.union(gridCell(max, grid));
        }
        Insets in = getInsets();
        int w = Math.max(cellWidth, getWidth() - in.left - in.right);
        return new Rectangle(in.left, in.top + tops[min], w, tops[max + 1] - tops[min]);
    }

    private Rectangle cn1VisibleRect() {
        Container p = getParent();
        if (p instanceof JViewport) {
            return ((JViewport) p).getViewRect();
        }
        return new Rectangle(0, 0, getWidth(), getHeight());
    }

    public int getFirstVisibleIndex() {
        Rectangle r = cn1VisibleRect();
        int first = locationToIndex(new Point(r.x, r.y));
        if (first >= 0) {
            Rectangle b = getCellBounds(first, first);
            if (b == null || !b.intersects(r)) {
                return -1;
            }
        }
        return first;
    }

    public int getLastVisibleIndex() {
        Rectangle r = cn1VisibleRect();
        int last = locationToIndex(new Point(r.x + r.width - 1, r.y + r.height - 1));
        if (last >= 0) {
            Rectangle b = getCellBounds(last, last);
            if (b == null || !b.intersects(r)) {
                return -1;
            }
        }
        return last;
    }

    public void ensureIndexIsVisible(int index) {
        Rectangle r = getCellBounds(index, index);
        if (r != null) {
            scrollRectToVisible(r);
        }
    }

    /// Asks the viewport this list is in, if any, to show `r`.
    public void scrollRectToVisible(Rectangle r) {
        JViewport.cn1ScrollRectToVisible(this, r);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Color bg = getBackground();
        if (isOpaque() && bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, w, h);
        }
        if (cellRenderer == null) {
            return;
        }
        int[] tops = rows();
        int n = Math.min(tops.length - 1, dataModel.getSize());
        Insets in = getInsets();
        int cw = w - in.left - in.right;
        Rectangle clip = g.getClipBounds();
        int lead = selectionModel.getLeadSelectionIndex();
        boolean focused = hasFocus();
        int[] grid = cn1Wrapped() ? grid() : null;
        int x = in.left;
        for (int i = 0; i < n; i++) {
            int y = in.top + tops[i];
            int ch = tops[i + 1] - tops[i];
            if (grid != null) {
                Rectangle cell = gridCell(i, grid);
                x = cell.x;
                y = cell.y;
                cw = cell.width;
                ch = cell.height;
                if (clip != null && !clip.intersects(cell)) {
                    continue;
                }
            } else if (clip != null) {
                if (y + ch <= clip.y) {
                    continue;
                }
                if (y >= clip.y + clip.height) {
                    break;
                }
            }
            if (ch <= 0 || cw <= 0) {
                continue;
            }
            Component c = cellRenderer.getListCellRendererComponent(this, dataModel.getElementAt(i), i,
                    selectionModel.isSelectedIndex(i), focused && i == lead);
            if (c == null) {
                continue;
            }
            c.setBounds(x, y, cw, ch);
            c.validate();
            Graphics cg = g.create(x, y, cw, ch);
            try {
                c.paint(cg);
            } finally {
                cg.dispose();
            }
        }
    }

    // ------------------------------------------------------------ input

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && isEnabled()) {
            requestFocusInWindow();
            int index = locationToIndex(e.getPoint());
            if (index >= 0) {
                int anchor = selectionModel.getAnchorSelectionIndex();
                if (e.isControlDown() || e.isMetaDown()) {
                    if (selectionModel.isSelectedIndex(index)) {
                        selectionModel.removeSelectionInterval(index, index);
                    } else {
                        selectionModel.addSelectionInterval(index, index);
                    }
                } else if (e.isShiftDown() && anchor >= 0) {
                    selectionModel.setSelectionInterval(anchor, index);
                } else {
                    selectionModel.setSelectionInterval(index, index);
                }
            }
        }
        super.processMouseEvent(e);
    }

    // ------------------------------------------------------------ selection

    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(ListSelectionModel selectionModel) {
        if (selectionModel == null) {
            throw new IllegalArgumentException("selectionModel must be non null");
        }
        ListSelectionModel old = this.selectionModel;
        old.removeListSelectionListener(selectionHandler);
        this.selectionModel = selectionModel;
        selectionModel.addListSelectionListener(selectionHandler);
        firePropertyChange("selectionModel", old, selectionModel);
        repaint();
    }

    /// Tells the listeners of the list, with the list as the source.
    protected void fireSelectionValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
        if (selectionListeners.isEmpty()) {
            return;
        }
        ListSelectionEvent e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
        ListSelectionListener[] all = getListSelectionListeners();
        for (int i = all.length - 1; i >= 0; i--) {
            all[i].valueChanged(e);
        }
    }

    public void addListSelectionListener(ListSelectionListener listener) {
        if (listener != null) {
            selectionListeners.add(listener);
        }
    }

    public void removeListSelectionListener(ListSelectionListener listener) {
        int i = selectionListeners.lastIndexOf(listener);
        if (i >= 0) {
            selectionListeners.remove(i);
        }
    }

    public ListSelectionListener[] getListSelectionListeners() {
        return selectionListeners.toArray(new ListSelectionListener[selectionListeners.size()]);
    }

    public void setSelectionMode(int selectionMode) {
        selectionModel.setSelectionMode(selectionMode);
    }

    public int getSelectionMode() {
        return selectionModel.getSelectionMode();
    }

    public int getAnchorSelectionIndex() {
        return selectionModel.getAnchorSelectionIndex();
    }

    public int getLeadSelectionIndex() {
        return selectionModel.getLeadSelectionIndex();
    }

    public int getMinSelectionIndex() {
        return selectionModel.getMinSelectionIndex();
    }

    public int getMaxSelectionIndex() {
        return selectionModel.getMaxSelectionIndex();
    }

    public boolean isSelectedIndex(int index) {
        return selectionModel.isSelectedIndex(index);
    }

    public boolean isSelectionEmpty() {
        return selectionModel.isSelectionEmpty();
    }

    public void clearSelection() {
        selectionModel.clearSelection();
    }

    public void setSelectionInterval(int anchor, int lead) {
        selectionModel.setSelectionInterval(anchor, lead);
    }

    public void addSelectionInterval(int anchor, int lead) {
        selectionModel.addSelectionInterval(anchor, lead);
    }

    public void removeSelectionInterval(int index0, int index1) {
        selectionModel.removeSelectionInterval(index0, index1);
    }

    public void setValueIsAdjusting(boolean b) {
        selectionModel.setValueIsAdjusting(b);
    }

    public boolean getValueIsAdjusting() {
        return selectionModel.getValueIsAdjusting();
    }

    public int[] getSelectedIndices() {
        int min = selectionModel.getMinSelectionIndex();
        int max = selectionModel.getMaxSelectionIndex();
        if (min < 0 || max < 0) {
            return new int[0];
        }
        int[] tmp = new int[1 + (max - min)];
        int n = 0;
        for (int i = min; i <= max; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                tmp[n++] = i;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(tmp, 0, rv, 0, n);
        return rv;
    }

    public void setSelectedIndex(int index) {
        if (index >= dataModel.getSize()) {
            return;
        }
        selectionModel.setSelectionInterval(index, index);
    }

    public void setSelectedIndices(int[] indices) {
        selectionModel.clearSelection();
        int size = dataModel.getSize();
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < size) {
                selectionModel.addSelectionInterval(indices[i], indices[i]);
            }
        }
    }

    /// The selected values as an array; [#getSelectedValuesList()] is the
    /// typed form.
    public Object[] getSelectedValues() {
        List<E> all = getSelectedValuesList();
        return all.toArray(new Object[all.size()]);
    }

    public List<E> getSelectedValuesList() {
        ArrayList<E> out = new ArrayList<E>();
        int[] indices = getSelectedIndices();
        int size = dataModel.getSize();
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < size) {
                out.add(dataModel.getElementAt(indices[i]));
            }
        }
        return out;
    }

    public int getSelectedIndex() {
        return getMinSelectionIndex();
    }

    public E getSelectedValue() {
        int i = getMinSelectionIndex();
        return i == -1 || i >= dataModel.getSize() ? null : dataModel.getElementAt(i);
    }

    public void setSelectedValue(Object anObject, boolean shouldScroll) {
        if (anObject == null) {
            setSelectedIndex(-1);
        } else if (!anObject.equals(getSelectedValue())) {
            int n = dataModel.getSize();
            for (int i = 0; i < n; i++) {
                if (anObject.equals(dataModel.getElementAt(i))) {
                    setSelectedIndex(i);
                    if (shouldScroll) {
                        ensureIndexIsVisible(i);
                    }
                    repaint();
                    return;
                }
            }
            setSelectedIndex(-1);
        }
        repaint();
    }

    // ------------------------------------------------------------ scrolling

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Insets in = getInsets();
        int dx = in.left + in.right;
        int dy = in.top + in.bottom;
        int[] tops = rows();
        int n = tops.length - 1;
        if (cn1Wrapped()) {
            // The whole grid when the row count fixes it; else one cell,
            // and the list grows the way it does not track.
            if (visibleRowCount > 0) {
                return getPreferredSize();
            }
            return new Dimension((n == 0 && fixedCellWidth <= 0 ? 256 : cellWidth) + dx,
                    (n == 0 && fixedCellHeight <= 0 ? 16 : cellHeight) + dy);
        }
        int width = n == 0 && fixedCellWidth <= 0 ? 256 : cellWidth;
        int rowHeight;
        if (fixedCellHeight > 0) {
            rowHeight = fixedCellHeight;
        } else if (n > 0) {
            rowHeight = tops[1] - tops[0];
        } else {
            rowHeight = 16;
        }
        return new Dimension(width + dx, visibleRowCount * rowHeight + dy);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation != SwingConstants.VERTICAL) {
            return cn1Wrapped() && cellWidth > 0 ? cellWidth : 10;
        }
        int row = locationToIndex(new Point(visibleRect.x, visibleRect.y));
        Rectangle r = row < 0 ? null : getCellBounds(row, row);
        if (r == null) {
            return 0;
        }
        if (direction > 0) {
            return r.height - (visibleRect.y - r.y);
        }
        if (r.y == visibleRect.y) {
            Rectangle prev = row == 0 ? null : getCellBounds(row - 1, row - 1);
            return prev == null ? 0 : prev.height;
        }
        return visibleRect.y - r.y;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? Math.max(1, visibleRect.height)
                : Math.max(1, visibleRect.width);
    }

    /// A list narrower than its viewport is stretched to fill it.
    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (layoutOrientation == HORIZONTAL_WRAP && visibleRowCount <= 0) {
            return true;
        }
        Container p = getParent();
        return p instanceof JViewport && p.getWidth() > getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (layoutOrientation == VERTICAL_WRAP && visibleRowCount <= 0) {
            return true;
        }
        Container p = getParent();
        return p instanceof JViewport && p.getHeight() > getPreferredSize().height;
    }
}
