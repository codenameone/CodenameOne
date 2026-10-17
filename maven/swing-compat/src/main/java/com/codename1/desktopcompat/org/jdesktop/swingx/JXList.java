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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.AbstractListModel;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JList;
import com.codename1.desktopcompat.javax.swing.ListCellRenderer;
import com.codename1.desktopcompat.javax.swing.ListModel;
import com.codename1.desktopcompat.javax.swing.RowFilter;
import com.codename1.desktopcompat.javax.swing.RowSorter;
import com.codename1.desktopcompat.javax.swing.SortOrder;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.ListDataEvent;
import com.codename1.desktopcompat.javax.swing.event.ListDataListener;
import com.codename1.desktopcompat.javax.swing.event.RowSorterEvent;
import com.codename1.desktopcompat.javax.swing.event.RowSorterListener;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ComponentAdapter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.CompoundHighlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.Highlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.DefaultListRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValues;
import com.codename1.desktopcompat.org.jdesktop.swingx.rollover.RolloverProducer;
import com.codename1.desktopcompat.org.jdesktop.swingx.sort.ListSortController;
import com.codename1.desktopcompat.org.jdesktop.swingx.sort.SortController;
import com.codename1.desktopcompat.org.jdesktop.swingx.sort.StringValueRegistry;
import java.util.Comparator;
import java.util.Vector;

/// A list with highlighters, sorting and filtering, and rollover.
///
/// ## Sorting and filtering
///
/// A row sorter stands between the model and what the list shows. It is
/// off until [#setAutoCreateRowSorter(boolean)] or
/// [#setRowSorter(RowSorter)] gives the list one. From then on the
/// indexes of the list -- of its selection, of [#getElementAt(int)], of
/// a cell renderer -- are view indexes, [#getElementCount()] is the
/// number of elements shown, and [#convertIndexToModel(int)] leads back
/// to the model, which [#getModel()] still answers unchanged.
///
/// The selection follows its elements through a change of order or
/// filter; elements that are filtered away leave it.
///
/// ## Highlighters
///
/// The renderer set is wrapped: [#getCellRenderer()] answers the wrapper
/// that applies the highlighters and [#getWrappedCellRenderer()] the
/// renderer that was set. The default is a SwingX renderer.
///
/// ## Not here
///
/// Searching and the look and feel hooks are absent.
public class JXList extends JList {

    public static final String EXECUTE_BUTTON_ACTIONCOMMAND = "executeButtonAction";

    protected CompoundHighlighter compoundHighlighter;
    protected ComponentAdapter dataAdapter;

    private boolean cn1Ready;
    private ViewModel cn1View;
    private ListModel cn1Model;
    private RowSorter<? extends ListModel> cn1Sorter;
    private boolean cn1AutoSorter;
    private boolean cn1Sortable;
    private boolean cn1SortsOnUpdates;
    private SortOrder[] cn1SortCycle;
    private Comparator<?> cn1Comparator;
    private RowFilter<?, ?> cn1RowFilter;
    private boolean cn1InModelEvent;
    private StringValueRegistry cn1Registry;
    private ChangeListener cn1HighlighterListener;
    private DelegatingRenderer cn1Renderer;
    private boolean cn1Rollover;
    private RolloverProducer cn1RolloverProducer;

    private final ListDataListener cn1ModelListener = new ListDataListener() {
        @Override
        public void intervalAdded(ListDataEvent e) {
            cn1ModelChanged(e);
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            cn1ModelChanged(e);
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            cn1ModelChanged(e);
        }
    };

    private final RowSorterListener cn1SorterListener = new RowSorterListener() {
        @Override
        public void sorterChanged(RowSorterEvent e) {
            cn1SorterChanged(e);
        }
    };

    public JXList() {
        this(false);
    }

    public JXList(ListModel dataModel) {
        this(dataModel, false);
    }

    public JXList(Object[] listData) {
        this(listData, false);
    }

    public JXList(Vector<?> listData) {
        this(listData, false);
    }

    @SuppressWarnings("unchecked")
    public JXList(boolean autoCreateRowSorter) {
        super(new ViewModel());
        cn1Init(autoCreateRowSorter);
    }

    @SuppressWarnings("unchecked")
    public JXList(ListModel dataModel, boolean autoCreateRowSorter) {
        super(new ViewModel());
        if (dataModel == null) {
            throw new IllegalArgumentException("dataModel must be non null");
        }
        cn1Init(autoCreateRowSorter);
        setModel(dataModel);
    }

    @SuppressWarnings("unchecked")
    public JXList(Object[] listData, boolean autoCreateRowSorter) {
        super(new ViewModel());
        if (listData == null) {
            throw new IllegalArgumentException("listData must be non null");
        }
        cn1Init(autoCreateRowSorter);
        setListData(listData);
    }

    @SuppressWarnings("unchecked")
    public JXList(Vector<?> listData, boolean autoCreateRowSorter) {
        super(new ViewModel());
        if (listData == null) {
            throw new IllegalArgumentException("listData must be non null");
        }
        cn1Init(autoCreateRowSorter);
        setListData(listData);
    }

    private void cn1Init(boolean autoCreateRowSorter) {
        ListModel view = super.getModel();
        if (view instanceof ViewModel) {
            cn1View = (ViewModel) view;
        }
        cn1Sortable = true;
        cn1SortsOnUpdates = true;
        cn1Ready = true;
        setModel(new EmptyModel());
        setCellRenderer(createDefaultCellRenderer());
        setAutoCreateRowSorter(autoCreateRowSorter);
    }

    private static final class EmptyModel extends AbstractListModel<Object> {
        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public Object getElementAt(int index) {
            return null;
        }
    }

    /// What the plain list underneath shows: the model through the
    /// sorter.
    private static final class ViewModel extends AbstractListModel<Object> {
        private ListModel source;
        private RowSorter<? extends ListModel> sorter;

        @Override
        public int getSize() {
            if (source == null) {
                return 0;
            }
            return sorter != null ? sorter.getViewRowCount() : source.getSize();
        }

        @Override
        public Object getElementAt(int index) {
            if (source == null) {
                return null;
            }
            int model = sorter != null ? sorter.convertRowIndexToModel(index) : index;
            return model >= 0 && model < source.getSize() ? source.getElementAt(model) : null;
        }

        void changed() {
            fireContentsChanged(this, 0, Math.max(0, getSize() - 1));
        }

        void forward(ListDataEvent e) {
            if (e.getType() == ListDataEvent.INTERVAL_ADDED) {
                fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
            } else if (e.getType() == ListDataEvent.INTERVAL_REMOVED) {
                fireIntervalRemoved(this, e.getIndex0(), e.getIndex1());
            } else {
                fireContentsChanged(this, e.getIndex0(), e.getIndex1());
            }
        }
    }

    // ------------------------------------------------------------ model

    /// The model that was set, whatever the sorter makes of it.
    @Override
    public ListModel getModel() {
        return cn1Model != null ? cn1Model : super.getModel();
    }

    @Override
    public void setModel(ListModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model must be non null");
        }
        if (!cn1Ready || cn1View == null) {
            return;
        }
        ListModel old = cn1Model;
        if (old != null) {
            old.removeListDataListener(cn1ModelListener);
        }
        cn1Model = model;
        model.addListDataListener(cn1ModelListener);
        cn1View.source = model;
        if (cn1AutoSorter) {
            setRowSorter(createDefaultRowSorter());
        }
        clearSelection();
        cn1View.changed();
        firePropertyChange("model", old, model);
    }

    private void cn1ModelChanged(ListDataEvent e) {
        if (cn1Sorter == null) {
            cn1View.forward(e);
            return;
        }
        cn1InModelEvent = true;
        try {
            int first = e.getIndex0();
            int last = e.getIndex1();
            if (first > last) {
                int t = first;
                first = last;
                last = t;
            }
            if (e.getType() == ListDataEvent.INTERVAL_ADDED && first >= 0) {
                cn1Sorter.rowsInserted(first, last);
            } else if (e.getType() == ListDataEvent.INTERVAL_REMOVED && first >= 0) {
                cn1Sorter.rowsDeleted(first, last);
            } else {
                cn1Sorter.allRowsChanged();
            }
        } finally {
            cn1InModelEvent = false;
        }
        clearSelection();
        cn1View.changed();
    }

    private void cn1SorterChanged(RowSorterEvent e) {
        if (cn1InModelEvent || e.getType() != RowSorterEvent.Type.SORTED) {
            return;
        }
        int[] selected = getSelectedIndices();
        int[] model = new int[selected.length];
        for (int i = 0; i < selected.length; i++) {
            model[i] = e.convertPreviousRowIndexToModel(selected[i]);
        }
        clearSelection();
        cn1View.changed();
        int count = getModel().getSize();
        for (int i = 0; i < model.length; i++) {
            if (model[i] >= 0 && model[i] < count) {
                int view = cn1Sorter.convertRowIndexToView(model[i]);
                if (view >= 0) {
                    addSelectionInterval(view, view);
                }
            }
        }
    }

    /// The element shown at a view index.
    public Object getElementAt(int viewIndex) {
        return cn1View.getElementAt(viewIndex);
    }

    /// The number of elements shown.
    public int getElementCount() {
        return cn1View.getSize();
    }

    public int convertIndexToModel(int viewIndex) {
        return cn1Sorter != null ? cn1Sorter.convertRowIndexToModel(viewIndex) : viewIndex;
    }

    /// The view index of a model index, -1 when it is filtered away.
    public int convertIndexToView(int modelIndex) {
        return cn1Sorter != null ? cn1Sorter.convertRowIndexToView(modelIndex) : modelIndex;
    }

    // ------------------------------------------------------------ sorting

    public boolean getAutoCreateRowSorter() {
        return cn1AutoSorter;
    }

    /// Whether the list makes a sort controller of its own for every
    /// model it is given.
    public void setAutoCreateRowSorter(boolean autoCreateRowSorter) {
        boolean old = cn1AutoSorter;
        cn1AutoSorter = autoCreateRowSorter;
        if (autoCreateRowSorter) {
            setRowSorter(createDefaultRowSorter());
        }
        firePropertyChange("autoCreateRowSorter", old, autoCreateRowSorter);
    }

    protected RowSorter<? extends ListModel> createDefaultRowSorter() {
        return new ListSortController<ListModel>(getModel());
    }

    public RowSorter<? extends ListModel> getRowSorter() {
        return cn1Sorter;
    }

    /// Sets the sorter, which must be one of this list's model; `null`
    /// shows the model as it is.
    public void setRowSorter(RowSorter<? extends ListModel> sorter) {
        RowSorter<? extends ListModel> old = cn1Sorter;
        if (old != null) {
            old.removeRowSorterListener(cn1SorterListener);
        }
        cn1Sorter = sorter;
        if (sorter != null) {
            sorter.addRowSorterListener(cn1SorterListener);
        }
        cn1View.sorter = sorter;
        configureSorterProperties();
        clearSelection();
        cn1View.changed();
        firePropertyChange("rowSorter", old, sorter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void configureSorterProperties() {
        if (!getControlsSorterProperties()) {
            return;
        }
        SortController controller = getSortController();
        controller.setStringValueProvider(getStringValueRegistry());
        controller.setSortable(cn1Sortable);
        controller.setSortsOnUpdates(cn1SortsOnUpdates);
        controller.setComparator(0, cn1Comparator);
        if (cn1SortCycle != null) {
            controller.setSortOrderCycle(cn1SortCycle);
        }
        controller.setRowFilter(cn1RowFilter);
    }

    public void setSortable(boolean sortable) {
        boolean old = cn1Sortable;
        cn1Sortable = sortable;
        if (getControlsSorterProperties()) {
            getSortController().setSortable(sortable);
        }
        firePropertyChange("sortable", old, sortable);
    }

    public boolean isSortable() {
        return cn1Sortable;
    }

    public void setSortsOnUpdates(boolean sortsOnUpdates) {
        boolean old = cn1SortsOnUpdates;
        cn1SortsOnUpdates = sortsOnUpdates;
        if (getControlsSorterProperties()) {
            getSortController().setSortsOnUpdates(sortsOnUpdates);
        }
        firePropertyChange("sortsOnUpdates", old, sortsOnUpdates);
    }

    public boolean getSortsOnUpdates() {
        return cn1SortsOnUpdates;
    }

    public void setSortOrderCycle(SortOrder... cycle) {
        SortOrder[] copy = new SortOrder[cycle == null ? 0 : cycle.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = cycle[i];
        }
        cn1SortCycle = copy;
        if (getControlsSorterProperties()) {
            getSortController().setSortOrderCycle(copy);
        }
    }

    public SortOrder[] getSortOrderCycle() {
        SortOrder[] from = cn1SortCycle;
        if (from == null) {
            return ListSortController.getDefaultSortOrderCycle();
        }
        SortOrder[] copy = new SortOrder[from.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = from[i];
        }
        return copy;
    }

    public Comparator<?> getComparator() {
        return cn1Comparator;
    }

    /// Sets what the elements are compared by; `null` goes back to
    /// their natural order, or their strings when they have none.
    public void setComparator(Comparator<?> comparator) {
        Comparator<?> old = cn1Comparator;
        cn1Comparator = comparator;
        updateSortAfterComparatorChange();
        firePropertyChange("comparator", old, comparator);
    }

    /// Hands the comparator to the sort controller and sorts again.
    protected void updateSortAfterComparatorChange() {
        if (!getControlsSorterProperties()) {
            return;
        }
        getSortController().setComparator(0, cn1Comparator);
        RowSorter<? extends ListModel> sorter = getRowSorter();
        if (sorter != null) {
            sorter.allRowsChanged();
        }
    }

    /// Shows only the elements the filter includes; `null` shows all.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends ListModel> void setRowFilter(RowFilter<? super R, ? super Integer> filter) {
        cn1RowFilter = filter;
        if (hasSortController()) {
            SortController controller = getSortController();
            controller.setRowFilter(filter);
        }
    }

    public RowFilter<?, ?> getRowFilter() {
        return hasSortController() ? getSortController().getRowFilter() : cn1RowFilter;
    }

    public void resetSortOrder() {
        if (hasSortController()) {
            getSortController().resetSortOrders();
        }
    }

    /// Moves the list to its next sort order.
    public void toggleSortOrder() {
        if (hasSortController()) {
            getSortController().toggleSortOrder(0);
        }
    }

    public void setSortOrder(SortOrder sortOrder) {
        if (hasSortController()) {
            getSortController().setSortOrder(0, sortOrder);
        }
    }

    public SortOrder getSortOrder() {
        if (hasSortController()) {
            return getSortController().getSortOrder(0);
        }
        return SortOrder.UNSORTED;
    }

    @SuppressWarnings("unchecked")
    protected SortController<? extends ListModel> getSortController() {
        if (cn1Sorter instanceof SortController) {
            return (SortController<? extends ListModel>) cn1Sorter;
        }
        return null;
    }

    protected boolean hasSortController() {
        return cn1Sorter instanceof SortController;
    }

    protected boolean getControlsSorterProperties() {
        return hasSortController() && getAutoCreateRowSorter();
    }

    // ------------------------------------------------------------ rollover

    public void setRolloverEnabled(boolean rolloverEnabled) {
        boolean old = cn1Rollover;
        if (old == rolloverEnabled) {
            return;
        }
        cn1Rollover = rolloverEnabled;
        if (rolloverEnabled) {
            cn1RolloverProducer = createRolloverProducer();
            cn1RolloverProducer.install(this);
        } else {
            if (cn1RolloverProducer != null) {
                cn1RolloverProducer.release(this);
                cn1RolloverProducer = null;
            }
            putClientProperty(RolloverProducer.ROLLOVER_KEY, null);
            repaint();
        }
        firePropertyChange("rolloverEnabled", old, rolloverEnabled);
    }

    public boolean isRolloverEnabled() {
        return cn1Rollover;
    }

    protected RolloverProducer createRolloverProducer() {
        return new RolloverProducer() {
            @Override
            protected void updateRolloverPoint(JComponent component, Point mousePoint) {
                int row = locationToIndex(mousePoint);
                if (row >= 0) {
                    Rectangle cell = getCellBounds(row, row);
                    if (cell == null || !cell.contains(mousePoint)) {
                        row = -1;
                    }
                }
                rollover.setLocation(row >= 0 ? 0 : -1, row);
            }

            @Override
            protected void updateClientProperty(JComponent component, String property, boolean fireAlways) {
                Object before = component.getClientProperty(property);
                super.updateClientProperty(component, property, fireAlways);
                Object after = component.getClientProperty(property);
                if (before == null ? after != null : !before.equals(after)) {
                    component.repaint();
                }
            }

            @Override
            public void mouseExited(com.codename1.desktopcompat.java.awt.event.MouseEvent e) {
                super.mouseExited(e);
                repaint();
            }
        };
    }

    // ------------------------------------------------------------ highlighters

    protected ComponentAdapter getComponentAdapter() {
        if (dataAdapter == null) {
            dataAdapter = new ListAdapter(this);
        }
        return dataAdapter;
    }

    /// The adapter, pointed at a view index.
    protected ComponentAdapter getComponentAdapter(int index) {
        ComponentAdapter adapter = getComponentAdapter();
        adapter.column = 0;
        adapter.row = index;
        return adapter;
    }

    public void setHighlighters(Highlighter... highlighters) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().setHighlighters(highlighters);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    public Highlighter[] getHighlighters() {
        return getCompoundHighlighter().getHighlighters();
    }

    public void addHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().addHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    public void removeHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().removeHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    protected CompoundHighlighter getCompoundHighlighter() {
        if (compoundHighlighter == null) {
            compoundHighlighter = new CompoundHighlighter();
            compoundHighlighter.addChangeListener(getHighlighterChangeListener());
        }
        return compoundHighlighter;
    }

    protected ChangeListener getHighlighterChangeListener() {
        if (cn1HighlighterListener == null) {
            cn1HighlighterListener = createHighlighterChangeListener();
        }
        return cn1HighlighterListener;
    }

    protected ChangeListener createHighlighterChangeListener() {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        };
    }

    // ------------------------------------------------------------ strings and renderer

    protected StringValueRegistry getStringValueRegistry() {
        if (cn1Registry == null) {
            cn1Registry = createDefaultStringValueRegistry();
        }
        return cn1Registry;
    }

    protected StringValueRegistry createDefaultStringValueRegistry() {
        return new StringValueRegistry();
    }

    /// The text the list shows for the element at a view index.
    public String getStringAt(int row) {
        Object value = getElementAt(row);
        ListCellRenderer renderer = getWrappedCellRenderer();
        if (renderer instanceof StringValue) {
            return ((StringValue) renderer).getString(value);
        }
        return StringValues.TO_STRING.getString(value);
    }

    protected ListCellRenderer createDefaultCellRenderer() {
        return new DefaultListRenderer();
    }

    /// The renderer the list paints with: a wrapper that applies the
    /// highlighters to what the renderer that was set answers.
    @Override
    public ListCellRenderer getCellRenderer() {
        return cn1Renderer != null ? cn1Renderer : super.getCellRenderer();
    }

    /// The renderer that was set.
    public ListCellRenderer getWrappedCellRenderer() {
        return cn1Renderer != null ? cn1Renderer.delegate : super.getCellRenderer();
    }

    /// Sets the renderer; `null` is the default one.
    @Override
    @SuppressWarnings("unchecked")
    public void setCellRenderer(ListCellRenderer renderer) {
        if (!cn1Ready) {
            super.setCellRenderer(renderer);
            return;
        }
        ListCellRenderer delegate = renderer != null ? renderer : createDefaultCellRenderer();
        if (cn1Renderer == null) {
            cn1Renderer = new DelegatingRenderer();
        }
        cn1Renderer.delegate = delegate;
        getStringValueRegistry().setStringValue(delegate instanceof StringValue ? (StringValue) delegate : null, 0);
        super.setCellRenderer(cn1Renderer);
    }

    private final class DelegatingRenderer implements ListCellRenderer<Object> {
        private ListCellRenderer delegate;

        @Override
        @SuppressWarnings("unchecked")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            Component stamp = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (stamp != null && compoundHighlighter != null && index >= 0) {
                stamp = compoundHighlighter.highlight(stamp, getComponentAdapter(index));
            }
            return stamp;
        }
    }

    // ------------------------------------------------------------ adapter

    private static final class ListAdapter extends ComponentAdapter {

        private final JXList list;

        ListAdapter(JXList list) {
            super(list);
            this.list = list;
        }

        @Override
        public int getRowCount() {
            return list.getModel().getSize();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return list.getModel().getElementAt(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public String getStringAt(int row, int column) {
            return list.getStringValueRegistry().getStringValue(row, column).getString(getValueAt(row, column));
        }

        @Override
        public Rectangle getCellBounds() {
            return list.getCellBounds(row, row);
        }

        @Override
        public boolean hasFocus() {
            return list.isFocusOwner() && row == list.getLeadSelectionIndex();
        }

        @Override
        public boolean isSelected() {
            return list.isSelectedIndex(row);
        }

        @Override
        public boolean isEditable() {
            return false;
        }

        @Override
        public int convertRowIndexToView(int rowModelIndex) {
            return list.convertIndexToView(rowModelIndex);
        }

        @Override
        public int convertRowIndexToModel(int rowViewIndex) {
            return list.convertIndexToModel(rowViewIndex);
        }
    }
}
