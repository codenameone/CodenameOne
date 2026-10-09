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
import com.codename1.ui.plaf.Style;
import com.codename1.ui.events.ActionListener;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;

/// The base of the controls that are pressed: buttons, check boxes,
/// toggles, hyperlinks.
///
/// The native component's own action, however the user triggered it,
/// calls [#fire()], which a subclass overrides to change its state and
/// then fire an `ActionEvent`.
public abstract class ButtonBase extends Labeled {

    private static final PseudoClass ARMED = PseudoClass.getPseudoClass("armed");

    private final ReadOnlyBooleanWrapper armed = new ReadOnlyBooleanWrapper(this, "armed", false);
    private boolean ownChrome;
    private boolean ownPadding;

    {
        InvalidationListener chrome = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (cn1OwnsChrome()) {
                    cn1Invalidated(Dirty.NATIVE);
                }
            }
        };
        backgroundProperty().addListener(chrome);
        borderProperty().addListener(chrome);
        paddingProperty().addListener(chrome);
    }

    /// Whether a background or a border of this button's own replaces the
    /// look the native theme gives it: so for a push button and a toggle
    /// button, whose whole look that is, and not for a check box or a
    /// link.
    boolean cn1OwnsChrome() {
        return false;
    }

    private boolean drawsItself() {
        Background b = getBackground();
        if (b != null && !b.getFills().isEmpty()) {
            return true;
        }
        Border border = getBorder();
        return border != null && !border.getStrokes().isEmpty();
    }

    @Override
    protected void cn1SyncNative() {
        if (!cn1OwnsChrome()) {
            super.cn1SyncNative();
            return;
        }
        Component c = cn1NativeIfCreated();
        boolean own = drawsItself();
        // A button that draws itself and has a padding of its own is
        // that padding around its text, as in JavaFX; the padding the
        // theme gives its button would come on top of it.
        javafx.geometry.Insets pad = getPadding();
        boolean bare = own && getGraphic() == null && pad != null
                && pad.getTop() + pad.getRight() + pad.getBottom() + pad.getLeft() > 0;
        if (c != null && ((ownChrome && !own) || (ownPadding && !bare))) {
            // Setting the UIID again brings the styles of the theme back.
            c.setUIID(c.getUIID());
        }
        ownChrome = own;
        ownPadding = bare;
        super.cn1SyncNative();
        if (c != null && own) {
            Style style = c.getAllStyles();
            style.setBgTransparency(0);
            style.setBorder(com.codename1.ui.plaf.Border.createEmpty());
            if (bare) {
                style.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
                style.setPadding(0, 0, 0, 0);
            }
        }
    }

    /// The look of the standard theme's button as a style, in the theme's
    /// own colour names so that it follows whatever redefines them.
    ///
    /// The button is four fills one inside the other -- a highlight
    /// below, the outer border, the inner border and the body, a gradient
    /// of the button's colour -- and its text is light or dark by how
    /// bright that colour is. Hovered, pressed, selected and default
    /// buttons differ in the colour alone; a focused one has the focus
    /// colour for its border and a faint ring of it outside.
    final String cn1StandardLook(boolean selected, boolean byDefault) {
        String c = "-fx-color";
        if (selected) {
            c = "derive(-fx-color, -17%)";
        } else if (isArmed()) {
            c = "-fx-pressed-base";
        } else if (isHover()) {
            c = "-fx-hover-base";
        } else if (byDefault) {
            c = "-fx-default-button";
        }
        String body = "linear-gradient(to bottom, derive(" + c + ", 8%), derive(" + c + ", -8%))";
        String fills;
        if (isFocused()) {
            fills = "-fx-background-color: -fx-faint-focus-color, -fx-focus-color, derive(" + c + ", 50%), " + body
                    + "; -fx-background-insets: -1.4, -0.2, 1, 2; -fx-background-radius: 4, 3, 2, 1;";
        } else {
            fills = "-fx-background-color: -fx-shadow-highlight-color, derive(" + c + ", -23%), derive(" + c
                    + ", 50%), " + body
                    + "; -fx-background-insets: 0 0 -1 0, 0, 1, 2; -fx-background-radius: 3, 3, 2, 1;";
        }
        return fills + " -fx-padding: 0.333333em 0.666667em 0.333333em 0.666667em; -fx-text-fill: ladder(" + c
                + ", -fx-light-text-color 45%, -fx-dark-text-color 46%, -fx-dark-text-color 59%,"
                + " -fx-mid-text-color 60%);";
    }

    /// Creates a button with no text.
    public ButtonBase() {
        setMnemonicParsing(true);
    }

    /// Creates a button with text.
    public ButtonBase(String text) {
        super(text);
        setMnemonicParsing(true);
    }

    /// Creates a button with text and a graphic.
    public ButtonBase(String text, Node graphic) {
        super(text, graphic);
        setMnemonicParsing(true);
    }

    /// Returns the listener a subclass adds to its native button: it
    /// calls [#fire()] unless the control is disabled.
    protected final ActionListener<com.codename1.ui.events.ActionEvent> cn1ActionBridge() {
        return new ActionListener<com.codename1.ui.events.ActionEvent>() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                if (!isDisabled()) {
                    fire();
                }
            }
        };
    }

    /// Returns whether a release now would fire the button.
    public final boolean isArmed() {
        return armed.get();
    }

    /// Whether a release now would fire the button.
    public final ReadOnlyBooleanProperty armedProperty() {
        return armed.getReadOnlyProperty();
    }

    /// Arms the button.
    public void arm() {
        if (!armed.get()) {
            armed.set(true);
            pseudoClassStateChanged(ARMED, true);
        }
    }

    /// Disarms the button.
    public void disarm() {
        if (armed.get()) {
            armed.set(false);
            pseudoClassStateChanged(ARMED, false);
        }
    }

    /// Invokes the button as a press and release would.
    public abstract void fire();

    /// Sets the handler of the button being invoked.
    public final void setOnAction(EventHandler<ActionEvent> value) {
        cn1Events().setSlot(ActionEvent.ACTION, "onAction", value);
    }

    /// Returns the handler of the button being invoked.
    public final EventHandler<? super ActionEvent> getOnAction() {
        return cn1Events().getSlot(ActionEvent.ACTION);
    }

    /// The handler of the button being invoked.
    public final ObjectProperty<EventHandler<? super ActionEvent>> onActionProperty() {
        return cn1Events().slot(ActionEvent.ACTION, "onAction");
    }
}
