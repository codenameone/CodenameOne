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

import java.util.ArrayList;

import com.codename1.ui.Component;
import com.codename1.ui.TooltipManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/// A short text shown while the pointer rests on a node.
///
/// The text is handed to Codename One, which shows its own tooltip for
/// the component of the node: [#install(Node, Tooltip)] sets it on the
/// native component of a control, or on the peer of any other node, and
/// a later change of the text follows. So the tooltip looks and is timed
/// as Codename One's are, on the platforms that have a pointer to hover
/// with. This object is never opened as a popup itself, and the graphic,
/// the wrapping, the delays and the font of the JavaFX tooltip are not
/// part of this layer.
///
/// The first tooltip installed turns the tooltips of Codename One on.
public class Tooltip extends PopupControl {

    private static final String KEY = "cn1.fx.tooltip";
    private static boolean enabled;

    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final ArrayList<Node> nodes = new ArrayList<Node>();
    private final javafx.beans.property.ObjectProperty<Node> graphic =
            new javafx.beans.property.SimpleObjectProperty<Node>(this, "graphic");

    /// Creates a tooltip with no text.
    public Tooltip() {
        this(null);
    }

    /// Creates a tooltip with a text.
    public Tooltip(String text) {
        getStyleClass().add("tooltip");
        this.text.set(text == null ? "" : text);
        this.text.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                for (int i = 0; i < nodes.size(); i++) {
                    apply(nodes.get(i), newValue);
                }
            }
        });
    }

    /// Sets the text shown.
    public final void setText(String value) {
        text.set(value);
    }

    /// Returns the text shown.
    public final String getText() {
        return text.get();
    }

    /// The text shown.
    public final StringProperty textProperty() {
        return text;
    }

    private static Component target(Node node) {
        Component c = node instanceof Control ? ((Control) node).cn1Native() : null;
        return c != null ? c : node.cn1Peer();
    }

    private static void apply(Node node, String text) {
        Component c = target(node);
        if (c != null) {
            c.setTooltip(text == null || text.length() == 0 ? null : text);
        }
    }

    private static void enable() {
        if (!enabled) {
            enabled = true;
            TooltipManager.enableTooltips();
        }
    }

    /// Shows a tooltip for a node; it replaces one installed before.
    public static void install(Node node, Tooltip t) {
        if (node == null) {
            return;
        }
        Object old = node.getProperties().get(KEY);
        if (old instanceof Tooltip && old != t) {
            ((Tooltip) old).nodes.remove(node);
        }
        if (t == null) {
            node.getProperties().remove(KEY);
            apply(node, null);
            return;
        }
        enable();
        node.getProperties().put(KEY, t);
        if (!t.nodes.contains(node)) {
            t.nodes.add(node);
        }
        apply(node, t.getText());
    }

    /// Stops showing a tooltip for a node.
    public static void uninstall(Node node, Tooltip t) {
        if (node == null) {
            return;
        }
        Object old = node.getProperties().get(KEY);
        if (t != null) {
            t.nodes.remove(node);
        }
        if (old == t || t == null) {
            if (old instanceof Tooltip) {
                ((Tooltip) old).nodes.remove(node);
            }
            node.getProperties().remove(KEY);
            apply(node, null);
        }
    }

    @Override
    public String getTypeSelector() {
        return "Tooltip";
    }

    /// Sets the graphic; recorded, a tooltip shows its text only.
    public final void setGraphic(Node value) {
        graphic.set(value);
    }

    /// Returns the graphic.
    public final Node getGraphic() {
        return graphic.get();
    }

    /// The graphic; recorded.
    public final javafx.beans.property.ObjectProperty<Node> graphicProperty() {
        return graphic;
    }
}
