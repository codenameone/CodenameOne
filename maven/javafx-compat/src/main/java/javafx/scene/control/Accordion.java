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

import com.codename1.ui.Component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// A column of [TitledPane]s of which at most one is open.
///
/// Opening a panel closes the one that was open, and
/// [#expandedPaneProperty()] names the open one or is `null`. The open
/// panel takes the height the others leave. A panel is closed as it is
/// added unless it is the one the accordion was told to expand.
public class Accordion extends Control {

    private final ObservableList<TitledPane> panes = FXCollections.observableArrayList();
    private final ObjectProperty<TitledPane> expandedPane = new SimpleObjectProperty<TitledPane>(this,
            "expandedPane");
    private final VBox column = new VBox();
    private final ChangeListener<Boolean> opened = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean was, Boolean now) {
            for (int i = 0; i < panes.size(); i++) {
                TitledPane p = panes.get(i);
                if (p.expandedProperty() == observable) {
                    if (now.booleanValue()) {
                        expandedPane.set(p);
                    } else if (expandedPane.get() == p) {
                        expandedPane.set(null);
                    }
                }
            }
        }
    };

    /// Creates an accordion without panels.
    public Accordion() {
        this((TitledPane[]) null);
    }

    /// Creates an accordion of panels.
    public Accordion(TitledPane... titledPanes) {
        getStyleClass().add("accordion");
        setFocusTraversable(false);
        panes.addListener((ListChangeListener<TitledPane>) change -> {
            while (change.next()) {
                List<? extends TitledPane> gone = change.getRemoved();
                for (int i = 0; i < gone.size(); i++) {
                    gone.get(i).expandedProperty().removeListener(opened);
                    if (expandedPane.get() == gone.get(i)) {
                        expandedPane.set(null);
                    }
                }
                List<? extends TitledPane> come = change.getAddedSubList();
                for (int i = 0; i < come.size(); i++) {
                    TitledPane p = come.get(i);
                    // As in JavaFX, a panel is open only when it is the one
                    // the accordion was told to expand.
                    p.setExpanded(p == expandedPane.get());
                    p.expandedProperty().addListener(opened);
                }
            }
            column.getChildren().setAll(panes);
            one();
        });
        expandedPane.addListener((observable, was, now) -> one());
        cn1MadeOf(column);
        if (titledPanes != null) {
            panes.addAll(titledPanes);
        }
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    /// Leaves only the expanded panel open and gives it the spare height.
    private void one() {
        TitledPane open = expandedPane.get();
        for (int i = 0; i < panes.size(); i++) {
            TitledPane p = panes.get(i);
            if (p != open && p.isExpanded()) {
                p.setExpanded(false);
            }
            VBox.setVgrow(p, p == open ? Priority.ALWAYS : Priority.NEVER);
        }
        if (open != null && !open.isExpanded()) {
            open.setExpanded(true);
        }
        requestLayout();
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    /// Returns the panels, from the top.
    public final ObservableList<TitledPane> getPanes() {
        return panes;
    }

    /// Returns the open panel, or `null` when all are closed.
    public final TitledPane getExpandedPane() {
        return expandedPane.get();
    }

    /// Opens a panel and closes the others; `null` closes them all.
    public final void setExpandedPane(TitledPane value) {
        expandedPane.set(value);
    }

    /// The open panel.
    public final ObjectProperty<TitledPane> expandedPaneProperty() {
        return expandedPane;
    }
}
