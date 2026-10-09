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
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;

/// A button that stays selected until it is pressed again, shown as a
/// Codename One toggle button.
///
/// In a [ToggleGroup] selecting it deselects the group's selected toggle;
/// unlike a [RadioButton] it can be deselected by pressing it, leaving
/// the group with nothing selected. The `selected` pseudo-class follows
/// the selected property.
public class ToggleButton extends ButtonBase implements Toggle {

    private static final int SELECTED = Dirty.USER;
    private static final PseudoClass SELECTED_STATE = PseudoClass.getPseudoClass("selected");

    private final BooleanProperty selected = new FxBoolean(this, "selected", false, SELECTED | Dirty.NATIVE);
    private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<ToggleGroup>(this,
            "toggleGroup") {
        private ToggleGroup joined;

        @Override
        protected void invalidated() {
            ToggleGroup now = get();
            if (now == joined) {
                return;
            }
            ToggleGroup was = joined;
            joined = now;
            if (was != null) {
                was.getToggles().remove(ToggleButton.this);
            }
            if (now != null && !now.getToggles().contains(ToggleButton.this)) {
                now.getToggles().add(ToggleButton.this);
            }
        }
    };

    /// Creates a toggle button with no text.
    public ToggleButton() {
        init();
    }

    /// Creates a toggle button with text.
    public ToggleButton(String text) {
        super(text);
        init();
    }

    /// Creates a toggle button with text and a graphic.
    public ToggleButton(String text, Node graphic) {
        super(text, graphic);
        init();
    }

    private void init() {
        getStyleClass().add("toggle-button");
        // The text of a button is centred unless the application says otherwise.
        setAlignment(Pos.CENTER);
    }

    /// Whether the theme says nothing about a component name: its style
    /// is the one a name no theme knows gets, so nothing tells such a
    /// component from the form behind it.
    static boolean unstyled(String uiid) {
        Style s = UIManager.getInstance().getComponentStyle(uiid);
        Style none = UIManager.getInstance().getComponentStyle("FxNoSuchComponent");
        return s.getBgColor() == none.getBgColor() && s.getBgTransparency() == none.getBgTransparency()
                && s.getBgImage() == none.getBgImage() && sameBorder(s.getBorder(), none.getBorder())
                && s.getPaddingTop() == none.getPaddingTop()
                && s.getPaddingLeftNoRTL() == none.getPaddingLeftNoRTL();
    }

    private static boolean sameBorder(Border a, Border b) {
        boolean noA = a == null || a.isEmptyBorder();
        boolean noB = b == null || b.isEmptyBorder();
        return a == b || (noA && noB);
    }

    @Override
    boolean cn1OwnsChrome() {
        return true;
    }

    @Override
    public String cn1ThemedStyle() {
        return cn1StandardLook(isSelected(), false);
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.CheckBox b = new com.codename1.ui.CheckBox();
        b.setToggle(true);
        if (unstyled(b.getUIID())) {
            // A theme with no look for a toggle button, as the desktop
            // themes are, would leave bare text with no sign of being
            // selected. The toggle then takes the look of a push button,
            // which Codename One draws pressed while it is selected.
            b.setUIID("Button");
        }
        b.addActionListener(cn1ActionBridge());
        return b;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c instanceof com.codename1.ui.CheckBox) {
            ((com.codename1.ui.CheckBox) c).setSelected(isSelected());
        } else if (c instanceof com.codename1.ui.RadioButton) {
            ((com.codename1.ui.RadioButton) c).setSelected(isSelected());
        }
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & SELECTED) != 0) {
            boolean now = isSelected();
            pseudoClassStateChanged(SELECTED_STATE, now);
            ToggleGroup group = getToggleGroup();
            if (group != null) {
                if (now) {
                    group.selectToggle(this);
                } else if (group.getSelectedToggle() == this) {
                    group.clearSelectedToggle();
                }
            }
        }
        super.cn1Invalidated(what);
    }

    /// Flips the selected state and fires an `ActionEvent`.
    @Override
    public void fire() {
        if (!isDisabled()) {
            setSelected(!isSelected());
            fireEvent(new ActionEvent(this, this));
        }
    }

    @Override
    public final boolean isSelected() {
        return selected.get();
    }

    @Override
    public final void setSelected(boolean value) {
        selected.set(value);
    }

    @Override
    public final BooleanProperty selectedProperty() {
        return selected;
    }

    @Override
    public final ToggleGroup getToggleGroup() {
        return toggleGroup.get();
    }

    @Override
    public final void setToggleGroup(ToggleGroup value) {
        toggleGroup.set(value);
    }

    @Override
    public final ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroup;
    }
}
