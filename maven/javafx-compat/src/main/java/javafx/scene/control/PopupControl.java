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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.stage.PopupWindow;

/// A popup that a style sheet can address: the base of context menus and
/// tooltips.
///
/// The id, the style classes and the inline style are recorded. There
/// are no skins in this layer, so the skin and the size properties of
/// JavaFX are absent; a popup takes the preferred size of its content.
public class PopupControl extends PopupWindow implements Styleable {

    private final StringProperty id = new SimpleStringProperty(this, "id");
    private final StringProperty style = new SimpleStringProperty(this, "style", "");
    private final ObservableList<String> styleClass = FXCollections.observableArrayList();

    /// Creates a popup that is not showing.
    public PopupControl() {
    }

    /// The id a style sheet matches.
    public final StringProperty idProperty() {
        return id;
    }

    /// Sets the id a style sheet matches.
    public final void setId(String value) {
        id.set(value);
    }

    @Override
    public final String getId() {
        return id.get();
    }

    @Override
    public final ObservableList<String> getStyleClass() {
        return styleClass;
    }

    /// Sets the inline style; recorded.
    public final void setStyle(String value) {
        style.set(value);
    }

    @Override
    public final String getStyle() {
        return style.get();
    }

    /// The inline style.
    public final StringProperty styleProperty() {
        return style;
    }

    @Override
    public String getTypeSelector() {
        return "PopupControl";
    }

    @Override
    public Styleable getStyleableParent() {
        return getOwnerNode();
    }

    @Override
    public final ObservableSet<PseudoClass> getPseudoClassStates() {
        return FXCollections.<PseudoClass>emptyObservableSet();
    }
}
