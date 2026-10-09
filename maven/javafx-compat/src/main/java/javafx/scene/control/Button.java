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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;

/// A push button, shown as a Codename One `Button`.
///
/// A button with a background or a border of its own, set by the
/// application or by a style sheet, is drawn by those alone: the native
/// button under them gives up the background and the border of the
/// theme and keeps its text. Without either it looks as the theme makes
/// a button look.
///
/// The default and cancel flags are recorded; Enter and Escape are not
/// routed to such a button by this layer.
public class Button extends ButtonBase {

    private final BooleanProperty defaultButton = new SimpleBooleanProperty(this, "defaultButton", false);
    private final BooleanProperty cancelButton = new SimpleBooleanProperty(this, "cancelButton", false);
    private boolean ownChrome;
    private boolean ownPadding;

    {
        InvalidationListener chrome = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                cn1Invalidated(Dirty.NATIVE);
            }
        };
        backgroundProperty().addListener(chrome);
        borderProperty().addListener(chrome);
        paddingProperty().addListener(chrome);
        // ":default" and ":cancel" are how a style sheet marks the two.
        defaultButton.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("default"), defaultButton.get());
            }
        });
        cancelButton.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("cancel"), cancelButton.get());
            }
        });
        // The text of a button is centred unless the application says otherwise.
        setAlignment(javafx.geometry.Pos.CENTER);
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

    /// Creates a button with no text.
    public Button() {
        getStyleClass().add("button");
    }

    /// Creates a button with text.
    public Button(String text) {
        super(text);
        getStyleClass().add("button");
    }

    /// Creates a button with text and a graphic.
    public Button(String text, Node graphic) {
        super(text, graphic);
        getStyleClass().add("button");
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.Button b = new com.codename1.ui.Button();
        b.addActionListener(cn1ActionBridge());
        return b;
    }

    @Override
    public void fire() {
        if (!isDisabled()) {
            fireEvent(new ActionEvent(this, this));
        }
    }

    /// Returns whether this is the default button of its window.
    public final boolean isDefaultButton() {
        return defaultButton.get();
    }

    /// Sets whether this is the default button of its window.
    public final void setDefaultButton(boolean value) {
        defaultButton.set(value);
    }

    /// Whether this is the default button of its window.
    public final BooleanProperty defaultButtonProperty() {
        return defaultButton;
    }

    /// Returns whether this is the cancel button of its window.
    public final boolean isCancelButton() {
        return cancelButton.get();
    }

    /// Sets whether this is the cancel button of its window.
    public final void setCancelButton(boolean value) {
        cancelButton.set(value);
    }

    /// Whether this is the cancel button of its window.
    public final BooleanProperty cancelButtonProperty() {
        return cancelButton;
    }
}
