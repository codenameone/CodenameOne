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
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.Style;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;

/// A box that is checked or not, shown as a Codename One `CheckBox`.
///
/// With `allowIndeterminate` a press cycles unchecked, indeterminate,
/// checked; without it a press flips checked and clears indeterminate.
/// The native check box has no third look, so this one adds it: an
/// indeterminate box is the empty box of the theme with a dash in it.
/// The pseudo-classes `selected`, `indeterminate` and `determinate`
/// follow the state.
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

    /// The native check box with the third look it lacks: the empty box
    /// the theme draws, and a dash in it while the state is undetermined.
    private static final class Box extends com.codename1.ui.CheckBox {

        private boolean dash;

        void dash(boolean on) {
            if (dash != on) {
                dash = on;
                repaint();
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (!dash || isToggle() || isOppositeSide()) {
                return;
            }
            // The box is where the look and feel puts it: against the
            // leading padding, as high as the image the theme draws for
            // it -- as the text when the theme has none -- and centred on
            // the height the padding leaves.
            Style s = getStyle();
            int side = s.getFont().getHeight();
            LookAndFeel laf = getUIManager().getLookAndFeel();
            if (laf instanceof DefaultLookAndFeel) {
                Image[] images = ((DefaultLookAndFeel) laf).getCheckBoxImages();
                if (images != null && images.length > 0 && images[0] != null) {
                    side = images[0].getWidth();
                }
            }
            int lead = s.getPaddingLeft(isRTL());
            int x = isRTL() ? getX() + getWidth() - lead - side : getX() + lead;
            int top = s.getPaddingTop();
            int cy = getY() + top + (getHeight() - top - s.getPaddingBottom()) / 2;
            int w = Math.max(4, side / 2);
            int h = Math.max(2, side / 8);
            int color = g.getColor();
            g.setColor(s.getFgColor());
            g.fillRect(x + (side - w) / 2, cy - h / 2, w, h);
            g.setColor(color);
        }
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.CheckBox b = new Box();
        b.addActionListener(cn1ActionBridge());
        return b;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c instanceof Box) {
            ((Box) c).setSelected(isSelected() && !isIndeterminate());
            ((Box) c).dash(isIndeterminate());
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
