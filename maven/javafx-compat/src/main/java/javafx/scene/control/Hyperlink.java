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

import javafx.beans.property.BooleanProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;

/// Text that is pressed like a link, shown as a Codename One `Button`
/// without a border and with underlined text.
///
/// Invoking it marks it visited and fires an `ActionEvent`; opening
/// anything is up to the handler. The `visited` pseudo-class follows the
/// visited property. The text is always underlined, whatever
/// `setUnderline` says.
public class Hyperlink extends ButtonBase {

    private static final int VISITED = Dirty.USER;
    private static final PseudoClass VISITED_STATE = PseudoClass.getPseudoClass("visited");

    private final BooleanProperty visited = new FxBoolean(this, "visited", false, VISITED);

    /// Creates a hyperlink with no text.
    public Hyperlink() {
        getStyleClass().add("hyperlink");
    }

    /// Creates a hyperlink with text.
    public Hyperlink(String text) {
        super(text);
        getStyleClass().add("hyperlink");
    }

    /// Creates a hyperlink with text and a graphic.
    public Hyperlink(String text, Node graphic) {
        super(text, graphic);
        getStyleClass().add("hyperlink");
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.Button b = new com.codename1.ui.Button();
        Style s = b.getAllStyles();
        s.setBorder(Border.createEmpty());
        s.setBgTransparency(0);
        b.addActionListener(cn1ActionBridge());
        return b;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c != null) {
            c.getAllStyles().setTextDecoration(Style.TEXT_DECORATION_UNDERLINE);
        }
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & VISITED) != 0) {
            pseudoClassStateChanged(VISITED_STATE, isVisited());
        }
        super.cn1Invalidated(what);
    }

    /// Marks the link visited and fires an `ActionEvent`.
    @Override
    public void fire() {
        if (!isDisabled()) {
            if (!visited.isBound()) {
                setVisited(true);
            }
            fireEvent(new ActionEvent(this, this));
        }
    }

    /// Returns whether the link was invoked before.
    public final boolean isVisited() {
        return visited.get();
    }

    /// Sets whether the link counts as invoked before.
    public final void setVisited(boolean value) {
        visited.set(value);
    }

    /// Whether the link was invoked before.
    public final BooleanProperty visitedProperty() {
        return visited;
    }
}
