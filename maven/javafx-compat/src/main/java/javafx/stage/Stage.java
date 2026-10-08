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
package javafx.stage;

import com.codename1.ui.Display;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.Scene;

/// The top level container of a JavaFX application.
///
/// The primary stage, the one handed to `Application.start`, is a
/// Codename One form. A further stage is a desktop window where the
/// device has those; elsewhere a modal stage is a Codename One dialog and
/// any other a form shown on top of the previous one, with a back
/// command that asks the stage to close.
///
/// Full screen, iconified and maximized are recorded and, where the
/// stage is a desktop window, not otherwise acted on. The icons are
/// recorded and not shown: the icon of an application is the one its
/// Codename One project is built with.
public class Stage extends Window {

    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private final BooleanProperty resizable = new SimpleBooleanProperty(this, "resizable", true);
    private final BooleanProperty fullScreen = new SimpleBooleanProperty(this, "fullScreen", false);
    private final BooleanProperty maximized = new SimpleBooleanProperty(this, "maximized", false);
    private final BooleanProperty iconified = new SimpleBooleanProperty(this, "iconified", false);
    private final BooleanProperty alwaysOnTop = new SimpleBooleanProperty(this, "alwaysOnTop", false);
    private final ObservableList<Image> icons = FXCollections.observableArrayList();
    private StageStyle style;
    private Modality modality = Modality.NONE;
    private Window owner;
    private boolean primary;
    private double minWidth;
    private double minHeight;
    private double maxWidth = Double.MAX_VALUE;
    private double maxHeight = Double.MAX_VALUE;

    /// Creates a decorated stage.
    public Stage() {
        this(StageStyle.DECORATED);
    }

    /// Creates a stage with a decoration.
    public Stage(StageStyle style) {
        this.style = style == null ? StageStyle.DECORATED : style;
        ChangeListener<Object> push = new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                if (cn1Host() != null) {
                    cn1Host().stageChanged();
                }
            }
        };
        title.addListener(push);
        resizable.addListener(push);
        alwaysOnTop.addListener(push);
    }

    /// Marks this stage as the primary one; called by the lifecycle.
    public final void cn1MarkPrimary() {
        primary = true;
    }

    /// Returns whether this is the stage handed to `Application.start`.
    public final boolean cn1IsPrimary() {
        return primary;
    }

    @Override
    public final void setScene(Scene value) {
        super.setScene(value);
    }

    @Override
    public final void show() {
        super.show();
    }

    /// Shows the stage and returns once it is hidden again. Events keep
    /// being handled meanwhile.
    public void showAndWait() {
        if (primary) {
            throw new IllegalStateException("Cannot call this method on primary stage");
        }
        if (isShowing()) {
            throw new IllegalStateException("Stage already visible");
        }
        if (Display.isInitialized() && !Display.getInstance().isEdt()) {
            throw new IllegalStateException("Not on FX application thread");
        }
        cn1Show(true);
    }

    /// Closes the stage; the same as [#hide()].
    public void close() {
        hide();
    }

    /// Brings the stage in front of the others.
    public void toFront() {
        if (cn1Host() != null) {
            cn1Host().toFront();
        }
    }

    /// Sends the stage behind the others; not acted on.
    public void toBack() {
    }

    /// Sets the decoration; before the stage is first shown.
    public final void initStyle(StageStyle value) {
        if (isShowing()) {
            throw new IllegalStateException("Cannot set style once stage has been set visible");
        }
        style = value == null ? StageStyle.DECORATED : value;
    }

    /// Returns the decoration.
    public final StageStyle getStyle() {
        return style;
    }

    /// Sets what the stage blocks; before the stage is first shown.
    public final void initModality(Modality value) {
        if (isShowing()) {
            throw new IllegalStateException("Cannot set modality once stage has been set visible");
        }
        if (primary) {
            throw new IllegalStateException("Cannot set modality for the primary stage");
        }
        modality = value == null ? Modality.NONE : value;
    }

    /// Returns what the stage blocks.
    public final Modality getModality() {
        return modality;
    }

    /// Sets the window this stage belongs to; before it is first shown.
    public final void initOwner(Window value) {
        if (isShowing()) {
            throw new IllegalStateException("Cannot set owner once stage has been set visible");
        }
        if (primary) {
            throw new IllegalStateException("Cannot set owner for the primary stage");
        }
        owner = value;
    }

    /// Returns the window this stage belongs to, or `null`.
    public final Window getOwner() {
        return owner;
    }

    /// Returns the icons of the window. They are recorded; the icon shown
    /// for the application is the one the project is built with.
    public final ObservableList<Image> getIcons() {
        return icons;
    }

    /// Returns the title.
    public final String getTitle() {
        return title.get();
    }

    /// Sets the title.
    public final void setTitle(String value) {
        title.set(value);
    }

    /// The title of the stage.
    public final StringProperty titleProperty() {
        return title;
    }

    /// Returns whether the user can resize the stage.
    public final boolean isResizable() {
        return resizable.get();
    }

    /// Sets whether the user can resize the stage.
    public final void setResizable(boolean value) {
        resizable.set(value);
    }

    /// Whether the user can resize the stage.
    public final BooleanProperty resizableProperty() {
        return resizable;
    }

    /// Returns whether full screen was asked for.
    public final boolean isFullScreen() {
        return fullScreen.get();
    }

    /// Asks for full screen.
    public final void setFullScreen(boolean value) {
        fullScreen.set(value);
    }

    /// Whether full screen was asked for.
    public final BooleanProperty fullScreenProperty() {
        return fullScreen;
    }

    /// Returns whether the stage was asked to be maximized.
    public final boolean isMaximized() {
        return maximized.get();
    }

    /// Asks for the stage to be maximized.
    public final void setMaximized(boolean value) {
        maximized.set(value);
    }

    /// Whether the stage was asked to be maximized.
    public final BooleanProperty maximizedProperty() {
        return maximized;
    }

    /// Returns whether the stage was asked to be iconified.
    public final boolean isIconified() {
        return iconified.get();
    }

    /// Asks for the stage to be iconified.
    public final void setIconified(boolean value) {
        iconified.set(value);
    }

    /// Whether the stage was asked to be iconified.
    public final BooleanProperty iconifiedProperty() {
        return iconified;
    }

    /// Returns whether the stage stays above other windows.
    public final boolean isAlwaysOnTop() {
        return alwaysOnTop.get();
    }

    /// Sets whether the stage stays above other windows.
    public final void setAlwaysOnTop(boolean value) {
        alwaysOnTop.set(value);
    }

    /// Whether the stage stays above other windows.
    public final BooleanProperty alwaysOnTopProperty() {
        return alwaysOnTop;
    }

    /// Returns the smallest width the user can give the stage.
    public final double getMinWidth() {
        return minWidth;
    }

    /// Sets the smallest width the user can give the stage.
    public final void setMinWidth(double value) {
        minWidth = value;
        if (cn1Host() != null) {
            cn1Host().stageChanged();
        }
    }

    /// Returns the smallest height the user can give the stage.
    public final double getMinHeight() {
        return minHeight;
    }

    /// Sets the smallest height the user can give the stage.
    public final void setMinHeight(double value) {
        minHeight = value;
        if (cn1Host() != null) {
            cn1Host().stageChanged();
        }
    }

    /// Returns the largest width the user can give the stage.
    public final double getMaxWidth() {
        return maxWidth;
    }

    /// Sets the largest width; recorded, not enforced.
    public final void setMaxWidth(double value) {
        maxWidth = value;
    }

    /// Returns the largest height the user can give the stage.
    public final double getMaxHeight() {
        return maxHeight;
    }

    /// Sets the largest height; recorded, not enforced.
    public final void setMaxHeight(double value) {
        maxHeight = value;
    }
}
