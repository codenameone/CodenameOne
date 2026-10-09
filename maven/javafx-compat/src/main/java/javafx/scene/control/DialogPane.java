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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.Pane;

/// The content of a [Dialog]: a header text, a content, and a row of
/// buttons.
///
/// The content is the node set with [#setContent(Node)], or else the
/// content text. The pane makes one button for each [ButtonType] in
/// [#getButtonTypes()], with [#createButton(ButtonType)], and shows them
/// right aligned at the bottom, in the order of the list; the order of
/// the platform that JavaFX applies is not part of this layer. Invoking a
/// button answers the dialog with its type and closes it, unless a
/// handler of the button consumed the `ActionEvent` first.
///
/// The graphic is recorded and not drawn. An expandable content is shown
/// under the content by a `Show Details` link, in the window as it is: the
/// dialog does not grow for it, so it takes room from the content. The header node, the expandable
/// content and the style sheet of the JavaFX pane are absent.
///
/// ## Style
///
/// The style class is `dialog-pane`; the header is a label with the
/// class `header`, the content text one with `content`. The region
/// properties (`-fx-background-color`, `-fx-border-color`, `-fx-padding`)
/// apply to the pane.
public class DialogPane extends Pane {

    private static final double PAD = 12;
    private static final double GAP = 8;
    private static final double MIN_WIDTH = 240;

    private final ObservableList<ButtonType> buttons = FXCollections.observableArrayList();
    private final ArrayList<ButtonType> buttonKeys = new ArrayList<ButtonType>();
    private final ArrayList<Node> buttonNodes = new ArrayList<Node>();
    private final StringProperty headerText = new SimpleStringProperty(this, "headerText");
    private final StringProperty contentText = new SimpleStringProperty(this, "contentText");
    private final ObjectProperty<Node> content = new SimpleObjectProperty<Node>(this, "content");
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<Node>(this, "graphic");
    private final ObjectProperty<Node> expandableContent = new SimpleObjectProperty<Node>(this,
            "expandableContent");
    private final javafx.beans.property.BooleanProperty expanded =
            new javafx.beans.property.SimpleBooleanProperty(this, "expanded", false);
    private final Hyperlink detailsLink = new Hyperlink();
    private final javafx.scene.layout.VBox withDetails = new javafx.scene.layout.VBox(PAD);
    private final Label headerLabel = new Label();
    private final Label contentLabel = new Label();
    private Dialog<?> dialog;

    /// Creates a pane with no text and no buttons.
    public DialogPane() {
        getStyleClass().add("dialog-pane");
        headerLabel.getStyleClass().add("header");
        headerLabel.setFocusTraversable(false);
        contentLabel.getStyleClass().add("content");
        contentLabel.setFocusTraversable(false);
        ChangeListener<Object> rebuild = new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                rebuild();
            }
        };
        headerText.addListener(rebuild);
        contentText.addListener(rebuild);
        content.addListener(rebuild);
        expandableContent.addListener(rebuild);
        expanded.addListener(rebuild);
        detailsLink.getStyleClass().add("details-button");
        detailsLink.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setExpanded(!isExpanded());
            }
        });
        buttons.addListener(new ListChangeListener<ButtonType>() {
            @Override
            public void onChanged(Change<? extends ButtonType> change) {
                buttonsChanged();
            }
        });
    }

    /// Records the dialog this pane answers.
    final void attach(Dialog<?> value) {
        dialog = value;
    }

    private static boolean blank(String s) {
        return s == null || s.length() == 0;
    }

    private void buttonsChanged() {
        ArrayList<ButtonType> keys = new ArrayList<ButtonType>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        for (int i = 0; i < buttons.size(); i++) {
            ButtonType type = buttons.get(i);
            if (type == null || keys.contains(type)) {
                continue;
            }
            int old = buttonKeys.indexOf(type);
            Node node = old >= 0 ? buttonNodes.get(old) : createButton(type);
            if (node != null) {
                keys.add(type);
                nodes.add(node);
            }
        }
        buttonKeys.clear();
        buttonKeys.addAll(keys);
        buttonNodes.clear();
        buttonNodes.addAll(nodes);
        rebuild();
    }

    private Node plainBody() {
        Node c = content.get();
        if (c != null) {
            return c;
        }
        return blank(contentText.get()) ? null : contentLabel;
    }

    /// What is between the header and the buttons: the content, and with
    /// an expandable content the link that shows it and, once expanded,
    /// that content under it.
    private Node body() {
        Node plain = plainBody();
        Node more = expandableContent.get();
        if (more == null) {
            return plain;
        }
        ArrayList<Node> all = new ArrayList<Node>();
        if (plain != null) {
            all.add(plain);
        }
        detailsLink.setText(isExpanded() ? "Hide Details" : "Show Details");
        all.add(detailsLink);
        if (isExpanded()) {
            all.add(more);
        }
        if (!all.equals(withDetails.getChildren())) {
            withDetails.getChildren().setAll(all);
        }
        return withDetails;
    }

    private void rebuild() {
        headerLabel.setText(blank(headerText.get()) ? "" : headerText.get());
        contentLabel.setText(blank(contentText.get()) ? "" : contentText.get());
        ArrayList<Node> all = new ArrayList<Node>();
        if (!blank(headerText.get())) {
            all.add(headerLabel);
        }
        Node body = body();
        if (body != null && !all.contains(body)) {
            all.add(body);
        }
        for (int i = 0; i < buttonNodes.size(); i++) {
            if (!all.contains(buttonNodes.get(i))) {
                all.add(buttonNodes.get(i));
            }
        }
        cn1Children().setAll(all);
        requestLayout();
    }

    /// Returns the button types of the pane; it shows a button for each.
    public final ObservableList<ButtonType> getButtonTypes() {
        return buttons;
    }

    /// Returns the button the pane made for a button type, or `null` when
    /// the type is not among the button types.
    public final Node lookupButton(ButtonType buttonType) {
        int i = buttonKeys.indexOf(buttonType);
        return i < 0 ? null : buttonNodes.get(i);
    }

    /// Makes the button of a button type: a [Button] with the text of the
    /// type that answers the dialog with the type when it is invoked. A
    /// subclass may return another node; answering the dialog is then up
    /// to that node.
    protected Node createButton(final ButtonType buttonType) {
        Button button = new Button(buttonType.getText());
        ButtonData data = buttonType.getButtonData();
        if (data != null) {
            button.setDefaultButton(data.isDefaultButton());
            button.setCancelButton(data.isCancelButton());
        }
        button.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!event.isConsumed() && dialog != null) {
                    dialog.answer(buttonType, true);
                }
            }
        });
        return button;
    }

    /// The text above the content.
    public final StringProperty headerTextProperty() {
        return headerText;
    }

    /// Sets the text above the content.
    public final void setHeaderText(String value) {
        headerText.set(value);
    }

    /// Returns the text above the content.
    public final String getHeaderText() {
        return headerText.get();
    }

    /// The text shown as the content while there is no content node.
    public final StringProperty contentTextProperty() {
        return contentText;
    }

    /// Sets the text shown as the content.
    public final void setContentText(String value) {
        contentText.set(value);
    }

    /// Returns the text shown as the content.
    public final String getContentText() {
        return contentText.get();
    }

    /// The node shown as the content, in place of the content text.
    public final ObjectProperty<Node> contentProperty() {
        return content;
    }

    /// Sets the node shown as the content.
    public final void setContent(Node value) {
        content.set(value);
    }

    /// Returns the node shown as the content.
    public final Node getContent() {
        return content.get();
    }

    /// The graphic of the header; recorded.
    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    /// Sets the graphic of the header; recorded.
    public final void setGraphic(Node value) {
        graphic.set(value);
    }

    /// Returns the graphic of the header.
    public final Node getGraphic() {
        return graphic.get();
    }

    private boolean headerShown() {
        return !blank(headerText.get());
    }

    private double buttonsWidth() {
        double w = 0;
        for (int i = 0; i < buttonNodes.size(); i++) {
            w += buttonNodes.get(i).prefWidth(-1) + (i > 0 ? GAP : 0);
        }
        return w;
    }

    private double buttonsHeight() {
        double h = 0;
        for (int i = 0; i < buttonNodes.size(); i++) {
            h = Math.max(h, buttonNodes.get(i).prefHeight(-1));
        }
        return h;
    }

    @Override
    protected double computePrefWidth(double height) {
        double w = Math.max(MIN_WIDTH, buttonsWidth());
        if (headerShown()) {
            w = Math.max(w, headerLabel.prefWidth(-1));
        }
        Node body = body();
        if (body != null) {
            w = Math.max(w, body.prefWidth(-1));
        }
        Insets in = getInsets();
        return in.getLeft() + PAD + w + PAD + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        double h = PAD;
        if (headerShown()) {
            h += headerLabel.prefHeight(-1) + PAD;
        }
        Node body = body();
        if (body != null) {
            h += body.prefHeight(-1) + PAD;
        }
        if (!buttonNodes.isEmpty()) {
            h += buttonsHeight() + PAD;
        }
        Insets in = getInsets();
        return in.getTop() + h + in.getBottom();
    }

    @Override
    protected double computeMinWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    private static void place(Node node, double x, double y, double w, double h) {
        if (node.isResizable()) {
            node.resize(w, h);
        }
        node.relocate(x - node.getLayoutBounds().getMinX(), y - node.getLayoutBounds().getMinY());
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double x = in.getLeft() + PAD;
        double w = Math.max(0, getWidth() - x - PAD - in.getRight());
        double y = in.getTop() + PAD;
        double bottom = getHeight() - in.getBottom() - PAD;
        if (headerShown()) {
            double h = headerLabel.prefHeight(-1);
            place(headerLabel, x, y, w, h);
            y += h + PAD;
        }
        if (!buttonNodes.isEmpty()) {
            double h = buttonsHeight();
            double bx = x + w;
            for (int i = buttonNodes.size() - 1; i >= 0; i--) {
                Node b = buttonNodes.get(i);
                double bw = b.prefWidth(-1);
                bx -= bw;
                place(b, bx, bottom - h, bw, h);
                bx -= GAP;
            }
            bottom -= h + PAD;
        }
        Node body = body();
        if (body != null) {
            place(body, x, y, w, Math.max(0, bottom - y));
        }
    }

    /// What a `Show Details` link shows under the content.
    public final ObjectProperty<Node> expandableContentProperty() {
        return expandableContent;
    }

    /// Sets what a `Show Details` link shows under the content.
    public final void setExpandableContent(Node value) {
        expandableContent.set(value);
    }

    /// Returns what a `Show Details` link shows under the content.
    public final Node getExpandableContent() {
        return expandableContent.get();
    }

    /// Whether the expandable content is shown.
    public final javafx.beans.property.BooleanProperty expandedProperty() {
        return expanded;
    }

    /// Shows or hides the expandable content.
    public final void setExpanded(boolean value) {
        expanded.set(value);
    }

    /// Returns whether the expandable content is shown.
    public final boolean isExpanded() {
        return expanded.get();
    }
}
