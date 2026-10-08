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
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.ui.Component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;

/// A box that is checked or not, shown as a Codename One `CheckBox`.
///
/// With `allowIndeterminate` a press cycles unchecked, indeterminate,
/// checked; without it a press flips checked and clears indeterminate.
/// The native check box has no third look: an indeterminate box is shown
/// unchecked, and only the `indeterminate` pseudo-class tells the two
/// apart. The pseudo-classes `selected`, `indeterminate` and
/// `determinate` follow the state.
public class CheckBox extends ButtonBase {

    private static final int STATE = Dirty.USER;
    private static final PseudoClass SELECTED_STATE = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass INDETERMINATE_STATE = PseudoClass.getPseudoClass("indeterminate");
    private static final PseudoClass DETERMINATE_STATE = PseudoClass.getPseudoClass("determinate");

    private final BooleanProperty selected = new FxBoolean(this, "selected", false, STATE | Dirty.NATIVE);
    private final BooleanProperty indeterminate = new FxBoolean(this, "indeterminate", false,
            STATE | Dirty.NATIVE);
    private final BooleanProperty allowIndeterminate = new SimpleBooleanProperty(this, "allowIndeterminate", false);

    /// Creates a check box with no text.
    public CheckBox() {
        init();
    }

    /// Creates a check box with text.
    public CheckBox(String text) {
        super(text);
        init();
    }

    private void init() {
        getStyleClass().add("check-box");
        pseudoClassStateChanged(DETERMINATE_STATE, true);
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.CheckBox b = new com.codename1.ui.CheckBox();
        b.addActionListener(cn1ActionBridge());
        return b;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c instanceof com.codename1.ui.CheckBox) {
            ((com.codename1.ui.CheckBox) c).setSelected(isSelected() && !isIndeterminate());
        }
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & STATE) != 0) {
            pseudoClassStateChanged(SELECTED_STATE, isSelected());
            pseudoClassStateChanged(INDETERMINATE_STATE, isIndeterminate());
            pseudoClassStateChanged(DETERMINATE_STATE, !isIndeterminate());
        }
        super.cn1Invalidated(what);
    }

    /// Moves to the next state and fires an `ActionEvent`.
    @Override
    public void fire() {
        if (isDisabled()) {
            return;
        }
        if (isAllowIndeterminate()) {
            if (isIndeterminate()) {
                setSelected(true);
                setIndeterminate(false);
            } else if (isSelected()) {
                setSelected(false);
            } else {
                setIndeterminate(true);
            }
        } else {
            setSelected(!isSelected());
            setIndeterminate(false);
        }
        fireEvent(new ActionEvent(this, this));
    }

    /// Returns whether the box is checked.
    public final boolean isSelected() {
        return selected.get();
    }

    /// Checks or unchecks the box.
    public final void setSelected(boolean value) {
        selected.set(value);
    }

    /// Whether the box is checked.
    public final BooleanProperty selectedProperty() {
        return selected;
    }

    /// Returns whether the box is in the third, undecided state.
    public final boolean isIndeterminate() {
        return indeterminate.get();
    }

    /// Puts the box in the third, undecided state or takes it out.
    public final void setIndeterminate(boolean value) {
        indeterminate.set(value);
    }

    /// Whether the box is in the third, undecided state.
    public final BooleanProperty indeterminateProperty() {
        return indeterminate;
    }

    /// Returns whether a press can reach the indeterminate state.
    public final boolean isAllowIndeterminate() {
        return allowIndeterminate.get();
    }

    /// Sets whether a press can reach the indeterminate state.
    public final void setAllowIndeterminate(boolean value) {
        allowIndeterminate.set(value);
    }

    /// Whether a press can reach the indeterminate state.
    public final BooleanProperty allowIndeterminateProperty() {
        return allowIndeterminate;
    }
}
