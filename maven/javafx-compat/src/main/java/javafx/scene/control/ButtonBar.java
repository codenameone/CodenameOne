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
import java.util.List;

import com.codename1.ui.Component;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;

/// A row of buttons, and the holder of [ButtonData], which says what a
/// button is for.
///
/// The buttons are laid out in one row with a gap of ten between them.
/// Those that are to be of a uniform size, as all are unless
/// [#setButtonUniformSize(Node, boolean)] says otherwise, are given the
/// width of the widest of them and at least `buttonMinWidth`.
///
/// JavaFX orders the buttons by their [ButtonData] in the way of the
/// desktop it runs on. A device is none of those desktops, so the order
/// here is [#BUTTON_ORDER_NONE] unless the application sets one: the
/// buttons keep the order of the list and are aligned to the right. With
/// an order set, the buttons are sorted by where the letter of their kind
/// stands in it, a button without a kind counting as
/// [ButtonData#OTHER]; the kinds before the first underscore of the order
/// are placed on the left and all others on the right, and a kind the
/// order does not name is placed last. The gaps an order asks for with
/// `+` and `_` are not made: every gap is the same.
///
/// A `DialogPane` does not use this control; it shows its buttons in the
/// order they were added.
public class ButtonBar extends Control {

    /// The order of buttons on Windows.
    public static final String BUTTON_ORDER_WINDOWS = "L_E+U+FBXI_YNOCAH_R";

    /// The order of buttons on macOS.
    public static final String BUTTON_ORDER_MAC_OS = "L_HE+U+FBIX_NCYOA_R";

    /// The order of buttons on Linux.
    public static final String BUTTON_ORDER_LINUX = "L_HE+UNYACBXIO_R";

    /// No order: the buttons stay as the list has them.
    public static final String BUTTON_ORDER_NONE = "";

    private static final String DATA = "javafx.scene.control.ButtonBar.ButtonData";
    private static final String UNIFORM = "javafx.scene.control.ButtonBar.independentSize";
    private static final double GAP = 10;

    private final ObservableList<Node> buttons = FXCollections.observableArrayList();
    private final StringProperty buttonOrder = new SimpleStringProperty(this, "buttonOrder", BUTTON_ORDER_NONE);
    private final DoubleProperty buttonMinWidth = new SimpleDoubleProperty(this, "buttonMinWidth", 0);

    /// Creates a button bar that keeps the order of its list.
    public ButtonBar() {
        this(null);
    }

    /// Creates a button bar with a button order; `null` for none.
    public ButtonBar(String buttonOrder) {
        getStyleClass().add("button-bar");
        setFocusTraversable(false);
        InvalidationListener relayout = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                cn1Children().setAll(buttons);
                requestLayout();
            }
        };
        buttons.addListener(relayout);
        this.buttonOrder.addListener(relayout);
        this.buttonMinWidth.addListener(relayout);
        if (buttonOrder != null) {
            this.buttonOrder.set(buttonOrder);
        }
    }

    /// Sets what a button is for; `null` takes the kind away.
    public static void setButtonData(Node button, ButtonData buttonData) {
        if (buttonData == null) {
            button.getProperties().remove(DATA);
        } else {
            button.getProperties().put(DATA, buttonData);
        }
    }

    /// Returns what a button is for, `null` when nothing was set.
    public static ButtonData getButtonData(Node button) {
        Object data = button.hasProperties() ? button.getProperties().get(DATA) : null;
        return data instanceof ButtonData ? (ButtonData) data : null;
    }

    /// Sets whether a button takes the width the bar gives all its
    /// uniform buttons.
    public static void setButtonUniformSize(Node button, boolean uniformSize) {
        if (uniformSize) {
            button.getProperties().remove(UNIFORM);
        } else {
            button.getProperties().put(UNIFORM, Boolean.TRUE);
        }
    }

    /// Returns whether a button takes the uniform width of its bar.
    public static boolean isButtonUniformSize(Node button) {
        return !button.hasProperties() || !button.getProperties().containsKey(UNIFORM);
    }

    /// Returns the buttons of the bar.
    public final ObservableList<Node> getButtons() {
        return buttons;
    }

    /// Returns the order the kinds of button are placed in.
    public final String getButtonOrder() {
        return buttonOrder.get();
    }

    /// Sets the order the kinds of button are placed in.
    public final void setButtonOrder(String value) {
        buttonOrder.set(value);
    }

    /// The order the kinds of button are placed in.
    public final StringProperty buttonOrderProperty() {
        return buttonOrder;
    }

    /// Returns the least width of a uniform button.
    public final double getButtonMinWidth() {
        return buttonMinWidth.get();
    }

    /// Sets the least width of a uniform button.
    public final void setButtonMinWidth(double value) {
        buttonMinWidth.set(value);
    }

    /// The least width of a uniform button.
    public final DoubleProperty buttonMinWidthProperty() {
        return buttonMinWidth;
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    private static int rank(Node button, String order) {
        ButtonData data = getButtonData(button);
        String code = (data == null ? ButtonData.OTHER : data).getTypeCode();
        int at = order.indexOf(code);
        return at < 0 ? order.length() : at;
    }

    /// The managed buttons in the order they are placed in.
    private List<Node> ordered() {
        String order = getButtonOrder();
        List<Node> out = new ArrayList<Node>();
        for (int i = 0; i < buttons.size(); i++) {
            Node b = buttons.get(i);
            if (b == null || !b.isManaged()) {
                continue;
            }
            int at = out.size();
            if (order != null && order.length() > 0) {
                int rank = rank(b, order);
                while (at > 0 && rank(out.get(at - 1), order) > rank) {
                    at--;
                }
            }
            out.add(at, b);
        }
        return out;
    }

    /// How many of the ordered buttons are placed on the left.
    private int leftCount(List<Node> all) {
        String order = getButtonOrder();
        int split = order == null ? -1 : order.indexOf('_');
        int n = 0;
        if (split > 0) {
            while (n < all.size() && rank(all.get(n), order) < split) {
                n++;
            }
        }
        return n;
    }

    private double uniformWidth(List<Node> all) {
        double w = Math.max(0, getButtonMinWidth());
        for (int i = 0; i < all.size(); i++) {
            if (isButtonUniformSize(all.get(i))) {
                w = Math.max(w, all.get(i).prefWidth(-1));
            }
        }
        return w;
    }

    private static double width(Node button, double uniform) {
        return isButtonUniformSize(button) ? uniform : button.prefWidth(-1);
    }

    @Override
    protected double computePrefWidth(double height) {
        List<Node> all = ordered();
        double uniform = uniformWidth(all);
        double w = 0;
        for (int i = 0; i < all.size(); i++) {
            w += (i > 0 ? GAP : 0) + width(all.get(i), uniform);
        }
        Insets in = getInsets();
        return in.getLeft() + w + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        double h = 0;
        for (int i = 0; i < buttons.size(); i++) {
            Node b = buttons.get(i);
            if (b != null && b.isManaged()) {
                h = Math.max(h, b.prefHeight(-1));
            }
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

    @Override
    protected void layoutChildren() {
        List<Node> all = ordered();
        double uniform = uniformWidth(all);
        Insets in = getInsets();
        double top = in.getTop();
        double room = Math.max(0, getHeight() - top - in.getBottom());
        int left = leftCount(all);
        double x = in.getLeft();
        for (int i = 0; i < left; i++) {
            Node b = all.get(i);
            double w = width(b, uniform);
            double h = Math.min(room, b.prefHeight(-1));
            b.resizeRelocate(x, top + (room - h) / 2, w, h);
            x += w + GAP;
        }
        double rest = 0;
        for (int i = left; i < all.size(); i++) {
            rest += (i > left ? GAP : 0) + width(all.get(i), uniform);
        }
        // The right group never runs under the left one.
        x = Math.max(x, getWidth() - in.getRight() - rest);
        for (int i = left; i < all.size(); i++) {
            Node b = all.get(i);
            double w = width(b, uniform);
            double h = Math.min(room, b.prefHeight(-1));
            b.resizeRelocate(x, top + (room - h) / 2, w, h);
            x += w + GAP;
        }
    }

    /// What a button of a dialog is for.
    public enum ButtonData {

        /// A button placed on the left.
        LEFT("L", false, false),

        /// A button placed on the right.
        RIGHT("R", false, false),

        /// A help button.
        HELP("H", false, false),

        /// A second help button.
        HELP_2("E", false, false),

        /// The button that answers yes; a default button.
        YES("Y", false, true),

        /// The button that answers no; a cancel button.
        NO("N", true, false),

        /// The button that goes on to the next step; a default button.
        NEXT_FORWARD("X", false, true),

        /// The button that goes back a step.
        BACK_PREVIOUS("B", false, false),

        /// The button that finishes; a default button.
        FINISH("I", false, true),

        /// The button that applies.
        APPLY("A", false, false),

        /// The button that cancels or closes; a cancel button.
        CANCEL_CLOSE("C", true, false),

        /// The button that accepts; a default button.
        OK_DONE("O", false, true),

        /// A button with no meaning of its own.
        OTHER("U", false, false),

        /// A large gap between buttons.
        BIG_GAP("+", false, false),

        /// A small gap between buttons.
        SMALL_GAP("_", false, false);

        private final String typeCode;
        private final boolean cancelButton;
        private final boolean defaultButton;

        ButtonData(String type, boolean cancelButton, boolean defaultButton) {
            this.typeCode = type;
            this.cancelButton = cancelButton;
            this.defaultButton = defaultButton;
        }

        /// Returns the letter that stands for this kind in a button order.
        public String getTypeCode() {
            return typeCode;
        }

        /// Returns whether a button of this kind cancels its dialog.
        public final boolean isCancelButton() {
            return cancelButton;
        }

        /// Returns whether a button of this kind is the default button.
        public final boolean isDefaultButton() {
            return defaultButton;
        }
    }
}
