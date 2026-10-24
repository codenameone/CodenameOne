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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.spinner.Picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.util.StringConverter;

/// What a [ComboBox] and a [ChoiceBox] share: the value and the selection
/// kept in step, and the Codename One picker that shows the choice and
/// lets the user change it.
///
/// The picker lists the items as text, made by the converter or by
/// `toString()`. It tells rows apart by that text, so of two items with
/// the same text the user can only pick the first.
final class ChoiceCore<T> {

    /// What the core tells its control.
    interface Host {

        /// The picker opened or closed.
        void shown(boolean showing);
    }

    private final Control owner;
    private final ObjectProperty<ObservableList<T>> items;
    private final ObjectProperty<T> value;
    private final ObjectProperty<SingleSelectionModel<T>> model;
    private final ObjectProperty<StringConverter<T>> converter;
    private final StringProperty prompt;
    private final Host host;
    private final boolean strict;
    private boolean syncing;

    private final ListChangeListener<T> itemsListener = new ListChangeListener<T>() {
        @Override
        public void onChanged(Change<? extends T> change) {
            itemsChanged();
        }
    };

    private final ChangeListener<T> selectedListener = new ChangeListener<T>() {
        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            value.set(newValue);
        }
    };

    /// Creates the core of a control. A strict core clears a value that
    /// is not among the items; the other keeps it with no row selected.
    ChoiceCore(Control owner, ObjectProperty<ObservableList<T>> items, ObjectProperty<T> value,
            ObjectProperty<SingleSelectionModel<T>> model, ObjectProperty<StringConverter<T>> converter,
            StringProperty prompt, ReadOnlyBooleanProperty showing, Host host, boolean strict) {
        this.owner = owner;
        this.items = items;
        this.value = value;
        this.model = model;
        this.converter = converter;
        this.prompt = prompt;
        this.host = host;
        this.strict = strict;
        items.addListener(new ChangeListener<ObservableList<T>>() {
            @Override
            public void changed(ObservableValue<? extends ObservableList<T>> observable, ObservableList<T> oldValue,
                    ObservableList<T> newValue) {
                if (oldValue != null) {
                    oldValue.removeListener(itemsListener);
                }
                if (newValue != null) {
                    newValue.addListener(itemsListener);
                }
                itemsChanged();
            }
        });
        model.addListener(new ChangeListener<SingleSelectionModel<T>>() {
            @Override
            public void changed(ObservableValue<? extends SingleSelectionModel<T>> observable,
                    SingleSelectionModel<T> oldValue, SingleSelectionModel<T> newValue) {
                if (oldValue != null) {
                    oldValue.selectedItemProperty().removeListener(selectedListener);
                }
                if (newValue != null) {
                    newValue.selectedItemProperty().addListener(selectedListener);
                    applyValue(ChoiceCore.this.value.get());
                }
                refresh();
            }
        });
        value.addListener(new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                applyValue(newValue);
                refresh();
            }
        });
        converter.addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                refresh();
            }
        });
        if (prompt != null) {
            prompt.addListener(new ChangeListener<Object>() {
                @Override
                public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                    refresh();
                }
            });
        }
        showing.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                showingChanged(newValue != null && newValue.booleanValue());
            }
        });
    }

    private void applyValue(T v) {
        SingleSelectionModel<T> m = model.get();
        if (m == null) {
            return;
        }
        T selected = m.getSelectedItem();
        if (v == null ? selected == null : v.equals(selected)) {
            return;
        }
        if (v == null) {
            m.clearSelection();
        } else {
            m.select(v);
        }
    }

    private void itemsChanged() {
        SingleSelectionModel<T> m = model.get();
        T v = value.get();
        if (m != null && v != null) {
            ObservableList<T> list = items.get();
            int index = list == null ? -1 : list.indexOf(v);
            if (index >= 0) {
                if (m.getSelectedIndex() != index) {
                    m.select(index);
                }
            } else if (strict) {
                m.clearSelection();
            } else if (m.getSelectedIndex() != -1) {
                // The value stays, on no row.
                m.select(v);
            }
        }
        refresh();
    }

    private void refresh() {
        owner.cn1Invalidated(Dirty.NATIVE);
        owner.cn1NativeSizeChanged();
    }

    /// Returns the text an item is listed as.
    String label(T item) {
        StringConverter<T> c = converter.get();
        String s = c != null ? c.toString(item) : (item == null ? null : item.toString());
        return s == null ? "" : s;
    }

    /// Creates the picker of the control.
    Picker create() {
        final Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_STRINGS);
        // The arrow that says a list drops down, after the text.
        p.setMaterialIcon(FontImage.MATERIAL_ARROW_DROP_DOWN);
        p.setTextPosition(Component.LEFT);
        p.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                picked(p, evt);
            }
        });
        return p;
    }

    private void picked(Picker p, ActionEvent evt) {
        if (syncing) {
            return;
        }
        boolean committed = evt.getX() == -99 && evt.getY() == -99;
        if (!committed && p.isEditing()) {
            // The picker just opened; what it holds is not chosen yet.
            host.shown(true);
            return;
        }
        int index = p.getSelectedStringIndex();
        SingleSelectionModel<T> m = model.get();
        if (m != null && index >= 0 && index != m.getSelectedIndex()) {
            m.select(index);
        }
        host.shown(false);
    }

    /// Copies the items and the choice into the picker.
    void sync(Picker p) {
        syncing = true;
        try {
            ObservableList<T> list = items.get();
            int n = list == null ? 0 : list.size();
            String[] labels = new String[n];
            for (int i = 0; i < n; i++) {
                labels[i] = label(list.get(i));
            }
            p.setStrings(labels);
            SingleSelectionModel<T> m = model.get();
            int index = m == null ? -1 : m.getSelectedIndex();
            T v = value.get();
            if (index >= 0 && index < n) {
                p.setSelectedStringIndex(index);
            } else {
                p.setSelectedString(null);
                String text = prompt == null ? null : prompt.get();
                p.setText(v != null ? label(v) : (text == null ? "" : text));
            }
        } finally {
            syncing = false;
        }
    }

    /// Opens the picker if it is not open.
    void reopen() {
        showingChanged(true);
    }

    private void showingChanged(boolean showing) {
        Object c = owner.cn1NativeIfCreated();
        if (!(c instanceof Picker)) {
            return;
        }
        Picker p = (Picker) c;
        if (showing) {
            Scene s = owner.getScene();
            ObservableList<T> list = items.get();
            boolean onScreen = s != null && s.getWindow() != null && s.getWindow().isShowing();
            if (onScreen && list != null && !list.isEmpty() && !p.isEditing()) {
                p.startEditingAsync();
            }
        } else if (p.isEditing()) {
            p.stopEditing(null);
        }
    }
}
