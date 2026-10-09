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

import com.codename1.ui.Component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/// A panel with a title that opens and closes it.
///
/// The title is a bar with an arrow, the graphic and the text; a press on
/// it opens or closes the panel when it is collapsible. A style sheet
/// reaches the bar as `.titled-pane > .title`, the arrow in it as
/// `.arrow` and the area of the content as `.titled-pane > .content`. The
/// pseudo-classes `expanded` and `collapsed` follow the state.
///
/// The panel opens and closes at once: `animated` is recorded and nothing
/// slides.
public class TitledPane extends Labeled {

    private static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass COLLAPSED = PseudoClass.getPseudoClass("collapsed");

    private final ObjectProperty<Node> content = new SimpleObjectProperty<Node>(this, "content");
    private final BooleanProperty expanded = new SimpleBooleanProperty(this, "expanded", true);
    private final BooleanProperty animated = new SimpleBooleanProperty(this, "animated", true);
    private final BooleanProperty collapsible = new SimpleBooleanProperty(this, "collapsible", true);
    private final Label title = new Label();
    private final Polygon arrow = new Polygon(0, 0, 8, 4, 0, 8);
    private final HBox bar = new Bar();
    private final StackPane area = new Area();

    /// The title bar, drawn as the standard theme draws it.
    private static final class Bar extends HBox {
        Bar() {
            super(6);
        }

        @Override
        public String cn1DefaultStyle() {
            return "-fx-background-color: -fx-box-border, derive(-fx-color, 50%), linear-gradient(to bottom,"
                    + " derive(-fx-color, 8%), derive(-fx-color, -8%)); -fx-background-insets: 0, 1, 2;"
                    + " -fx-padding: 0.3333em 0.75em 0.3333em 0.75em;";
        }
    }

    /// What holds the content, with the border around three sides.
    private static final class Area extends StackPane {
        @Override
        public String cn1DefaultStyle() {
            return "-fx-background-color: -fx-box-border, -fx-background; -fx-background-insets: 0, 0 1 1 1;"
                    + " -fx-padding: 0.167em;";
        }
    }
    private final VBox whole = new VBox(bar, area);

    /// Creates a panel with no title and no content.
    public TitledPane() {
        getStyleClass().add("titled-pane");
        setFocusTraversable(false);
        setMnemonicParsing(false);
        bar.getStyleClass().add("title");
        bar.setAlignment(Pos.CENTER_LEFT);
        arrow.getStyleClass().add("arrow");
        arrow.setFill(Color.gray(0.2));
        arrow.setRotate(90);
        title.textProperty().bind(textProperty());
        title.graphicProperty().bind(graphicProperty());
        bar.getChildren().addAll(arrow, title);
        area.getStyleClass().add("content");
        VBox.setVgrow(area, Priority.ALWAYS);
        bar.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (isCollapsible() && !isDisabled()) {
                setExpanded(!isExpanded());
                e.consume();
            }
        });
        content.addListener((observable, was, now) -> {
            if (now == null) {
                area.getChildren().clear();
            } else {
                area.getChildren().setAll(now);
            }
        });
        expanded.addListener((observable, was, now) -> shown());
        collapsible.addListener((observable, was, now) -> shown());
        shown();
        cn1MadeOf(whole);
    }

    /// Creates a panel with a title and a content.
    public TitledPane(String title, Node content) {
        this();
        setText(title);
        setContent(content);
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    /// The graphic is shown by the title, not beside a text of the
    /// panel's own.
    @Override
    boolean ownsGraphic() {
        return true;
    }

    private void shown() {
        boolean open = isExpanded();
        pseudoClassStateChanged(EXPANDED, open);
        pseudoClassStateChanged(COLLAPSED, !open);
        arrow.setRotate(open ? 90 : 0);
        arrow.setVisible(isCollapsible());
        arrow.setManaged(isCollapsible());
        area.setVisible(open);
        area.setManaged(open);
        requestLayout();
        if (getParent() != null) {
            getParent().requestLayout();
        }
    }

    /// A panel takes the room it is given; closed, it is as tall as its
    /// title.
    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return isExpanded() ? Double.MAX_VALUE : computePrefHeight(width);
    }

    /// Returns what the panel shows below its title.
    public final Node getContent() {
        return content.get();
    }

    /// Sets what the panel shows below its title.
    public final void setContent(Node value) {
        content.set(value);
    }

    /// What the panel shows below its title.
    public final ObjectProperty<Node> contentProperty() {
        return content;
    }

    /// Returns whether the panel is open.
    public final boolean isExpanded() {
        return expanded.get();
    }

    /// Opens or closes the panel.
    public final void setExpanded(boolean value) {
        expanded.set(value);
    }

    /// Whether the panel is open.
    public final BooleanProperty expandedProperty() {
        return expanded;
    }

    /// Returns whether opening and closing is asked to be animated.
    public final boolean isAnimated() {
        return animated.get();
    }

    /// Records whether opening and closing is to be animated; it is not.
    public final void setAnimated(boolean value) {
        animated.set(value);
    }

    /// Whether opening and closing is asked to be animated.
    public final BooleanProperty animatedProperty() {
        return animated;
    }

    /// Returns whether a press on the title opens and closes the panel.
    public final boolean isCollapsible() {
        return collapsible.get();
    }

    /// Sets whether a press on the title opens and closes the panel.
    public final void setCollapsible(boolean value) {
        collapsible.set(value);
    }

    /// Whether a press on the title opens and closes the panel.
    public final BooleanProperty collapsibleProperty() {
        return collapsible;
    }
}
