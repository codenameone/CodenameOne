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

import java.util.List;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/// A set of toggles of which at most one is selected: selecting one
/// deselects the one that was.
///
/// A toggle joins through `Toggle.setToggleGroup` or by being added to
/// [#getToggles()]; either way both sides agree afterwards.
public class ToggleGroup {

    private final ObservableList<Toggle> toggles = FXCollections.observableArrayList();
    private final ReadOnlyObjectWrapper<Toggle> selectedToggle = new ReadOnlyObjectWrapper<Toggle>(this,
            "selectedToggle");
    private Object userData;
    private ObservableMap<Object, Object> properties;

    /// Creates an empty group.
    public ToggleGroup() {
        toggles.addListener(new ListChangeListener<Toggle>() {
            @Override
            public void onChanged(Change<? extends Toggle> change) {
                togglesChanged(change);
            }
        });
    }

    private void togglesChanged(ListChangeListener.Change<? extends Toggle> change) {
        while (change.next()) {
            List<? extends Toggle> removed = change.getRemoved();
            for (int i = 0; i < removed.size(); i++) {
                Toggle t = removed.get(i);
                if (t == null || toggles.contains(t)) {
                    continue;
                }
                if (selectedToggle.get() == t) {
                    selectedToggle.set(null);
                }
                if (t.getToggleGroup() == this) {
                    t.setToggleGroup(null);
                }
            }
            List<? extends Toggle> added = change.getAddedSubList();
            for (int i = 0; i < added.size(); i++) {
                Toggle t = added.get(i);
                if (t == null) {
                    continue;
                }
                if (t.getToggleGroup() != this) {
                    t.setToggleGroup(this);
                }
                if (t.isSelected()) {
                    selectToggle(t);
                }
            }
        }
    }

    /// Returns the toggles of this group.
    public final ObservableList<Toggle> getToggles() {
        return toggles;
    }

    /// Selects a toggle, deselecting the one that was selected; `null`
    /// deselects it and leaves nothing selected. A toggle of another group
    /// is ignored.
    public final void selectToggle(Toggle value) {
        Toggle old = selectedToggle.get();
        if (old == value) {
            return;
        }
        if (value != null && value.getToggleGroup() != this) {
            return;
        }
        selectedToggle.set(value);
        if (old != null && old.getToggleGroup() == this && old.isSelected()) {
            old.setSelected(false);
        }
        if (value != null && !value.isSelected()) {
            value.setSelected(true);
        }
    }

    /// The selected toggle, or `null`.
    public final ReadOnlyObjectProperty<Toggle> selectedToggleProperty() {
        return selectedToggle.getReadOnlyProperty();
    }

    /// Returns the selected toggle, or `null`.
    public final Toggle getSelectedToggle() {
        return selectedToggle.get();
    }

    /// Forgets the selected toggle after it was deselected directly.
    final void clearSelectedToggle() {
        selectedToggle.set(null);
    }

    /// Returns the map of properties the application attached.
    public final ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableHashMap();
        }
        return properties;
    }

    /// Returns whether any property was attached.
    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }

    /// Attaches an object of the application's.
    public void setUserData(Object value) {
        userData = value;
    }

    /// Returns the object the application attached, or `null`.
    public Object getUserData() {
        return userData;
    }
}
