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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.ItemSelectable;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.javax.swing.event.ListDataEvent;
import com.codename1.desktopcompat.javax.swing.event.ListDataListener;
import com.codename1.desktopcompat.rt.CellStamp;
import com.codename1.desktopcompat.rt.ComboPeer;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import java.util.ArrayList;
import java.util.Vector;

/// A box showing one item of a model, with a popup to pick another.
///
/// The widget and its popup are a Codename One combo box, so the popup is
/// whatever the platform shows for one. The model, the selection and the
/// events are this class's, and they follow the desktop: selecting through
/// [#setSelectedItem] tells the item listeners about the item given up and
/// the item taken, when they differ, and then always the action listeners.
///
/// ## What differs from the desktop
///
/// An editable combo box accepts a selected item that is not in its model,
/// but shows no text field to type one into. A popup that is open cannot
/// be closed from code.
public class JComboBox<E> extends JComponent implements ItemSelectable, ListDataListener {

    private ComboBoxModel<E> dataModel;
    private ListCellRenderer<? super E> renderer = new DefaultListCellRenderer.UIResource();
    private boolean isEditable;
    private int maximumRowCount = 8;
    private Object selectedItemReminder;
    private E prototypeDisplayValue;
    private String actionCommand = "comboBoxChanged";
    private boolean firingActionEvent;
    private boolean selectingItem;
    private final ArrayList<ItemListener> itemListeners = new ArrayList<ItemListener>();
    private final ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener>();
    private final Bridge bridge = new Bridge();
    private JList<E> rendererList;

    public JComboBox(ComboBoxModel<E> aModel) {
        setModel(aModel);
    }

    public JComboBox(E[] items) {
        setModel(new DefaultComboBoxModel<E>(items));
    }

    public JComboBox(Vector<E> items) {
        setModel(new DefaultComboBoxModel<E>(items));
    }

    public JComboBox() {
        setModel(new DefaultComboBoxModel<E>());
    }

    // ------------------------------------------------------------ peer

    /// The model of this combo box as the Codename One widget sees it.
    private final class Bridge implements com.codename1.ui.list.ListModel<Object> {
        final ArrayList<DataChangedListener> data = new ArrayList<DataChangedListener>();
        final ArrayList<SelectionListener> selection = new ArrayList<SelectionListener>();
        int shownIndex = -1;

        @Override
        public Object getItemAt(int index) {
            return index >= 0 && index < dataModel.getSize() ? dataModel.getElementAt(index) : null;
        }

        @Override
        public int getSize() {
            return dataModel.getSize();
        }

        @Override
        public int getSelectedIndex() {
            return JComboBox.this.getSelectedIndex();
        }

        /// The popup picked a row.
        @Override
        public void setSelectedIndex(int index) {
            if (index >= 0 && index < dataModel.getSize() && index != JComboBox.this.getSelectedIndex()) {
                JComboBox.this.setSelectedIndex(index);
            }
        }

        @Override
        public void addDataChangedListener(DataChangedListener l) {
            data.add(l);
        }

        @Override
        public void removeDataChangedListener(DataChangedListener l) {
            data.remove(l);
        }

        @Override
        public void addSelectionListener(SelectionListener l) {
            selection.add(l);
        }

        @Override
        public void removeSelectionListener(SelectionListener l) {
            selection.remove(l);
        }

        @Override
        public void addItem(Object item) {
            // The widget never adds rows; the model is the application's.
        }

        @Override
        public void removeItem(int index) {
        }

        void dataChanged(int type, int index) {
            DataChangedListener[] all = data.toArray(new DataChangedListener[data.size()]);
            for (int i = 0; i < all.length; i++) {
                all[i].dataChanged(type, index);
            }
        }

        void selectionMoved() {
            int now = JComboBox.this.getSelectedIndex();
            if (now == shownIndex) {
                return;
            }
            int old = shownIndex;
            shownIndex = now;
            SelectionListener[] all = selection.toArray(new SelectionListener[selection.size()]);
            for (int i = 0; i < all.length; i++) {
                all[i].selectionChanged(old, now);
            }
        }
    }

    /// Paints the rows of the Codename One widget with this combo box's
    /// renderer.
    private final class Stamps implements com.codename1.ui.list.ListCellRenderer<Object> {
        private final CellStamp stamp = new CellStamp();

        @Override
        public com.codename1.ui.Component getListCellRendererComponent(com.codename1.ui.List list, Object value,
                int index, boolean isSelected) {
            stamp.setCell(cn1Cell(value, index, isSelected));
            return stamp;
        }

        @Override
        public com.codename1.ui.Component getListFocusComponent(com.codename1.ui.List list) {
            return null;
        }
    }

    /// The renderer's component for one value. The value comes back from
    /// the model this combo box handed out, so it is one of its elements.
    @SuppressWarnings("unchecked")
    private Component cn1Cell(Object value, int index, boolean selected) {
        if (renderer == null) {
            return null;
        }
        if (rendererList == null) {
            rendererList = new JList<E>();
        }
        rendererList.setFont(getFont());
        if (getForeground() != null) {
            rendererList.setForeground(getForeground());
        }
        return renderer.getListCellRendererComponent(rendererList, (E) value, index, selected, false);
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new ComboPeer(this, bridge);
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof ComboPeer) {
            ((ComboPeer) p).setRenderer(new Stamps());
        }
        bridge.shownIndex = getSelectedIndex();
    }

    private void cn1Refresh() {
        bridge.selectionMoved();
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p != null) {
            p.setShouldCalcPreferredSize(true);
            revalidate();
            repaint();
        }
    }

    // ------------------------------------------------------------ model

    public void setModel(ComboBoxModel<E> aModel) {
        ComboBoxModel<E> oldModel = dataModel;
        if (oldModel != null) {
            oldModel.removeListDataListener(this);
        }
        dataModel = aModel;
        dataModel.addListDataListener(this);
        selectedItemReminder = dataModel.getSelectedItem();
        firePropertyChange("model", oldModel, dataModel);
        bridge.dataChanged(DataChangedListener.CHANGED, -1);
        cn1Refresh();
    }

    public ComboBoxModel<E> getModel() {
        return dataModel;
    }

    public void setEditable(boolean aFlag) {
        boolean old = isEditable;
        isEditable = aFlag;
        firePropertyChange("editable", old, isEditable);
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void setMaximumRowCount(int count) {
        int old = maximumRowCount;
        maximumRowCount = count;
        firePropertyChange("maximumRowCount", old, maximumRowCount);
    }

    public int getMaximumRowCount() {
        return maximumRowCount;
    }

    public void setRenderer(ListCellRenderer<? super E> aRenderer) {
        ListCellRenderer<? super E> old = renderer;
        renderer = aRenderer;
        firePropertyChange("renderer", old, renderer);
        cn1Refresh();
    }

    public ListCellRenderer<? super E> getRenderer() {
        return renderer;
    }

    public E getPrototypeDisplayValue() {
        return prototypeDisplayValue;
    }

    public void setPrototypeDisplayValue(E prototypeDisplayValue) {
        Object old = this.prototypeDisplayValue;
        this.prototypeDisplayValue = prototypeDisplayValue;
        firePropertyChange("prototypeDisplayValue", old, prototypeDisplayValue);
    }

    // ------------------------------------------------------------ selection

    /// Selects an item. An item that is not in the model is ignored unless
    /// the combo box is editable. The action listeners hear about every
    /// call that gets as far as the model, a change or not.
    public void setSelectedItem(Object anObject) {
        Object oldSelection = selectedItemReminder;
        Object objectToSelect = anObject;
        if (oldSelection == null || !oldSelection.equals(anObject)) {
            if (anObject != null && !isEditable()) {
                boolean found = false;
                for (int i = 0; i < dataModel.getSize(); i++) {
                    E element = dataModel.getElementAt(i);
                    if (anObject.equals(element)) {
                        found = true;
                        objectToSelect = element;
                        break;
                    }
                }
                if (!found) {
                    return;
                }
            }
            selectingItem = true;
            dataModel.setSelectedItem(objectToSelect);
            selectingItem = false;
            // A model that told nobody still changed.
            if (selectedItemReminder != dataModel.getSelectedItem()) {
                selectedItemChanged();
            }
        }
        fireActionEvent();
    }

    public Object getSelectedItem() {
        return dataModel.getSelectedItem();
    }

    public void setSelectedIndex(int anIndex) {
        int size = dataModel.getSize();
        if (anIndex == -1) {
            setSelectedItem(null);
        } else if (anIndex < -1 || anIndex >= size) {
            throw new IllegalArgumentException("setSelectedIndex: " + anIndex + " out of bounds");
        } else {
            setSelectedItem(dataModel.getElementAt(anIndex));
        }
    }

    public int getSelectedIndex() {
        Object sObject = dataModel.getSelectedItem();
        for (int i = 0, c = dataModel.getSize(); i < c; i++) {
            E obj = dataModel.getElementAt(i);
            if (obj != null && obj.equals(sObject)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Object[] getSelectedObjects() {
        Object selected = getSelectedItem();
        return selected == null ? new Object[0] : new Object[]{selected};
    }

    // ------------------------------------------------------------ items

    private MutableComboBoxModel<E> mutable() {
        if (!(dataModel instanceof MutableComboBoxModel)) {
            throw new RuntimeException("Cannot use this method with a non-Mutable data model.");
        }
        return (MutableComboBoxModel<E>) dataModel;
    }

    public void addItem(E item) {
        mutable().addElement(item);
    }

    public void insertItemAt(E item, int index) {
        mutable().insertElementAt(item, index);
    }

    public void removeItem(Object anObject) {
        mutable().removeElement(anObject);
    }

    public void removeItemAt(int anIndex) {
        mutable().removeElementAt(anIndex);
    }

    public void removeAllItems() {
        MutableComboBoxModel<E> model = mutable();
        if (model instanceof DefaultComboBoxModel) {
            ((DefaultComboBoxModel<E>) model).removeAllElements();
        } else {
            int size = model.getSize();
            for (int i = 0; i < size; ++i) {
                model.removeElement(model.getElementAt(0));
            }
        }
        selectedItemReminder = null;
        cn1Refresh();
    }

    public int getItemCount() {
        return dataModel.getSize();
    }

    public E getItemAt(int index) {
        return dataModel.getElementAt(index);
    }

    // ------------------------------------------------------------ popup

    public void showPopup() {
        setPopupVisible(true);
    }

    public void hidePopup() {
        setPopupVisible(false);
    }

    /// Opens the popup after the current event; the popup is modal the way
    /// Codename One shows it, so it is not opened inside this call.
    public void setPopupVisible(boolean v) {
        final com.codename1.ui.Component p = cn1PeerOrNull();
        if (v && p instanceof ComboPeer && com.codename1.ui.Display.isInitialized()) {
            com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    ((ComboPeer) p).openPopup();
                }
            });
        }
    }

    public boolean isPopupVisible() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        return p instanceof ComboPeer && ((ComboPeer) p).isShowingPopupDialog();
    }

    // ------------------------------------------------------------ listeners

    @Override
    public void addItemListener(ItemListener aListener) {
        if (aListener != null) {
            itemListeners.add(aListener);
        }
    }

    @Override
    public void removeItemListener(ItemListener aListener) {
        int i = itemListeners.lastIndexOf(aListener);
        if (i >= 0) {
            itemListeners.remove(i);
        }
    }

    public ItemListener[] getItemListeners() {
        return itemListeners.toArray(new ItemListener[itemListeners.size()]);
    }

    public void addActionListener(ActionListener l) {
        if (l != null) {
            actionListeners.add(l);
        }
    }

    public void removeActionListener(ActionListener l) {
        int i = actionListeners.lastIndexOf(l);
        if (i >= 0) {
            actionListeners.remove(i);
        }
    }

    public ActionListener[] getActionListeners() {
        return actionListeners.toArray(new ActionListener[actionListeners.size()]);
    }

    public void setActionCommand(String aCommand) {
        actionCommand = aCommand;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    protected void fireItemStateChanged(ItemEvent e) {
        ItemListener[] all = getItemListeners();
        for (int i = all.length - 1; i >= 0; i--) {
            all[i].itemStateChanged(e);
        }
    }

    /// Tells the action listeners; a listener that selects again from
    /// inside the notification does not start a second round.
    protected void fireActionEvent() {
        if (firingActionEvent) {
            return;
        }
        firingActionEvent = true;
        try {
            ActionListener[] all = getActionListeners();
            ActionEvent e = null;
            for (int i = all.length - 1; i >= 0; i--) {
                if (e == null) {
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand());
                }
                all[i].actionPerformed(e);
            }
        } finally {
            firingActionEvent = false;
        }
    }

    /// The selection moved: the item given up is reported deselected and
    /// the item taken selected, each only when there is one.
    protected void selectedItemChanged() {
        if (selectedItemReminder != null) {
            fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, selectedItemReminder,
                    ItemEvent.DESELECTED));
        }
        selectedItemReminder = dataModel.getSelectedItem();
        if (selectedItemReminder != null) {
            fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, selectedItemReminder,
                    ItemEvent.SELECTED));
        }
        cn1Refresh();
    }

    // ------------------------------------------------------------ model events

    @Override
    public void contentsChanged(ListDataEvent e) {
        Object oldSelection = selectedItemReminder;
        Object newSelection = dataModel.getSelectedItem();
        if (oldSelection == null || !oldSelection.equals(newSelection)) {
            selectedItemChanged();
            if (!selectingItem) {
                fireActionEvent();
            }
        }
        if (e.getIndex0() >= 0) {
            bridge.dataChanged(DataChangedListener.CHANGED, e.getIndex0());
        }
        cn1Refresh();
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        // Identity on purpose, as on the desktop: the reminder is the very
        // object the model answered last time.
        Object now = dataModel.getSelectedItem();
        if (selectedItemReminder != now) {
            selectedItemChanged();
        }
        bridge.dataChanged(DataChangedListener.ADDED, e.getIndex0());
        cn1Refresh();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        Object oldSelection = selectedItemReminder;
        Object newSelection = dataModel.getSelectedItem();
        if (oldSelection == null || !oldSelection.equals(newSelection)) {
            selectedItemChanged();
            if (!selectingItem) {
                fireActionEvent();
            }
        }
        bridge.dataChanged(DataChangedListener.REMOVED, e.getIndex0());
        cn1Refresh();
    }
}
