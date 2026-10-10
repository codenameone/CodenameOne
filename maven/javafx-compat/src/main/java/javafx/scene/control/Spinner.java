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
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.StringConverter;

/// A text field with two arrows that step a value up and down.
///
/// The value and how it steps belong to a [SpinnerValueFactory]; the
/// spinner shows the value in its editor through the factory's converter.
/// In an editable spinner, Enter in the editor or the editor losing the
/// focus reads the text back through the converter, and a text the
/// converter cannot read puts the value back.
///
/// The arrows are one above the other at the right of the editor unless
/// one of the `STYLE_CLASS_*` constants is among the style classes, which
/// moves them as its name says. Arrows beside the editor point left and
/// right, and the one that steps up is the right one.
public class Spinner<T> extends Control {

    /// The arrows beside each other at the right.
    public static final String STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL = "arrows-on-right-horizontal";
    /// The arrows one above the other at the left.
    public static final String STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL = "arrows-on-left-vertical";
    /// The arrows beside each other at the left.
    public static final String STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL = "arrows-on-left-horizontal";
    /// One arrow above the editor and one below it.
    public static final String STYLE_CLASS_SPLIT_ARROWS_VERTICAL = "split-arrows-vertical";
    /// One arrow at either side of the editor.
    public static final String STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL = "split-arrows-horizontal";

    private final ReadOnlyObjectWrapper<T> value = new ReadOnlyObjectWrapper<T>(this, "value");
    private final ObjectProperty<SpinnerValueFactory<T>> valueFactory =
            new SimpleObjectProperty<SpinnerValueFactory<T>>(this, "valueFactory");
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false);
    private final TextField editor = new TextField();
    private final Arrow up = new Arrow(true);
    private final Arrow down = new Arrow(false);
    private final VBox pair = new VBox();
    private final HBox row = new HBox();
    private final VBox box = new VBox();
    private int arranged = -1;
    private final ChangeListener<T> stepped = new ChangeListener<T>() {
        @Override
        public void changed(ObservableValue<? extends T> observable, T was, T now) {
            value.set(now);
            show();
        }
    };

    /// One of the two arrows: a region with the standard theme's button
    /// look and a triangle on it.
    private static final class Arrow extends StackPane {
        private final boolean up;
        private final Polygon mark = new Polygon();

        Arrow(boolean up) {
            this.up = up;
            getStyleClass().add(up ? "increment-arrow-button" : "decrement-arrow-button");
            mark.setFill(Color.gray(0.25));
            mark.setMouseTransparent(true);
            getChildren().add(mark);
            point(false);
        }

        /// Points the triangle up or down, or right or left for arrows
        /// that are beside each other.
        void point(boolean sideways) {
            Double[] p;
            if (sideways) {
                p = up ? corners(0, 0, 4, 4, 0, 8) : corners(4, 0, 0, 4, 4, 8);
            } else {
                p = up ? corners(0, 4, 4, 0, 8, 4) : corners(0, 0, 8, 0, 4, 4);
            }
            mark.getPoints().setAll(p);
        }

        private static Double[] corners(double... at) {
            Double[] out = new Double[at.length];
            for (int i = 0; i < at.length; i++) {
                out[i] = Double.valueOf(at[i]);
            }
            return out;
        }

        @Override
        public String cn1DefaultStyle() {
            return "-fx-background-color: -fx-outer-border, linear-gradient(to bottom, derive(-fx-color, 8%),"
                    + " derive(-fx-color, -8%)); -fx-background-insets: 0, 1; -fx-padding: 3 6 3 6;";
        }
    }

    /// Creates a spinner with no value factory.
    public Spinner() {
        getStyleClass().add("spinner");
        editor.setPrefColumnCount(6);
        editor.setMinWidth(0);
        editor.setEditable(false);
        VBox.setVgrow(up, Priority.ALWAYS);
        VBox.setVgrow(down, Priority.ALWAYS);
        HBox.setHgrow(editor, Priority.ALWAYS);
        getStyleClass().addListener((ListChangeListener<String>) change -> arrange());
        up.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (!isDisabled()) {
                increment();
                e.consume();
            }
        });
        down.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (!isDisabled()) {
                decrement();
                e.consume();
            }
        });
        editor.setOnAction(e -> commitValue());
        editor.focusedProperty().addListener((observable, was, now) -> {
            if (!now.booleanValue()) {
                commitValue();
            }
        });
        editable.addListener((observable, was, now) -> editor.setEditable(now.booleanValue()));
        valueFactory.addListener((observable, was, now) -> {
            if (was != null) {
                was.valueProperty().removeListener(stepped);
            }
            if (now != null) {
                now.valueProperty().addListener(stepped);
                value.set(now.getValue());
            } else {
                value.set(null);
            }
            show();
        });
        arrange();
        cn1MadeOf(box);
    }

    /// Puts the editor and the arrows where the style classes say.
    private void arrange() {
        ObservableList<String> classes = getStyleClass();
        int now = 0;
        if (classes.contains(STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL)) {
            now = 1;
        } else if (classes.contains(STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL)) {
            now = 2;
        } else if (classes.contains(STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL)) {
            now = 3;
        } else if (classes.contains(STYLE_CLASS_SPLIT_ARROWS_VERTICAL)) {
            now = 4;
        } else if (classes.contains(STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL)) {
            now = 5;
        }
        if (now == arranged) {
            return;
        }
        arranged = now;
        box.getChildren().clear();
        row.getChildren().clear();
        pair.getChildren().clear();
        boolean sideways = now == 1 || now == 3 || now == 5;
        up.point(sideways);
        down.point(sideways);
        if (now == 4) {
            box.getChildren().addAll(up, editor, down);
        } else if (now == 5) {
            row.getChildren().addAll(down, editor, up);
            box.getChildren().add(row);
        } else if (now == 1) {
            row.getChildren().addAll(editor, down, up);
            box.getChildren().add(row);
        } else if (now == 3) {
            row.getChildren().addAll(down, up, editor);
            box.getChildren().add(row);
        } else {
            pair.getChildren().addAll(up, down);
            if (now == 2) {
                row.getChildren().addAll(pair, editor);
            } else {
                row.getChildren().addAll(editor, pair);
            }
            box.getChildren().add(row);
        }
        requestLayout();
    }

    /// A spinner is as wide as its editor and its arrows unless it is
    /// given a width, and the editor then takes what the arrows leave.
    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    /// Creates a spinner of whole numbers that steps by one.
    public Spinner(int min, int max, int initialValue) {
        this(min, max, initialValue, 1);
    }

    /// Creates a spinner of whole numbers.
    @SuppressWarnings("unchecked")
    public Spinner(int min, int max, int initialValue, int amountToStepBy) {
        this();
        Object factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initialValue, amountToStepBy);
        setValueFactory((SpinnerValueFactory<T>) factory);
    }

    /// Creates a spinner of decimal numbers that steps by one.
    public Spinner(double min, double max, double initialValue) {
        this(min, max, initialValue, 1);
    }

    /// Creates a spinner of decimal numbers.
    @SuppressWarnings("unchecked")
    public Spinner(double min, double max, double initialValue, double amountToStepBy) {
        this();
        Object factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initialValue, amountToStepBy);
        setValueFactory((SpinnerValueFactory<T>) factory);
    }

    /// Creates a spinner of the items of a list.
    public Spinner(ObservableList<T> items) {
        this();
        setValueFactory(new SpinnerValueFactory.ListSpinnerValueFactory<T>(items));
    }

    /// Creates a spinner of what a factory steps through.
    public Spinner(SpinnerValueFactory<T> valueFactory) {
        this();
        setValueFactory(valueFactory);
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    private void show() {
        SpinnerValueFactory<T> f = getValueFactory();
        StringConverter<T> c = f == null ? null : f.getConverter();
        T v = getValue();
        String text = v == null ? "" : (c == null ? v.toString() : c.toString(v));
        editor.setText(text == null ? "" : text);
    }

    /// Reads the text of the editor into the value, when the spinner is
    /// editable and the text is one the converter reads; any other text
    /// is replaced by the value.
    public final void commitValue() {
        SpinnerValueFactory<T> f = getValueFactory();
        StringConverter<T> c = f == null ? null : f.getConverter();
        if (!isEditable() || c == null) {
            return;
        }
        T read = null;
        try {
            read = c.fromString(editor.getText());
        } catch (RuntimeException notAValue) {
            read = null;
        }
        if (read != null) {
            f.setValue(read);
        }
        show();
    }

    /// Puts the value back into the editor, dropping what was typed.
    public final void cancelEdit() {
        show();
    }

    /// Steps the value up once.
    public void increment() {
        increment(1);
    }

    /// Steps the value up.
    public void increment(int steps) {
        SpinnerValueFactory<T> f = getValueFactory();
        if (f == null) {
            throw new IllegalStateException("Can't increment Spinner with a null SpinnerValueFactory");
        }
        commitValue();
        f.increment(steps);
    }

    /// Steps the value down once.
    public void decrement() {
        decrement(1);
    }

    /// Steps the value down.
    public void decrement(int steps) {
        SpinnerValueFactory<T> f = getValueFactory();
        if (f == null) {
            throw new IllegalStateException("Can't decrement Spinner with a null SpinnerValueFactory");
        }
        commitValue();
        f.decrement(steps);
    }

    /// Returns the value, which is the factory's.
    public final T getValue() {
        return value.get();
    }

    /// The value.
    public final ReadOnlyObjectProperty<T> valueProperty() {
        return value.getReadOnlyProperty();
    }

    /// Returns what the spinner steps through.
    public final SpinnerValueFactory<T> getValueFactory() {
        return valueFactory.get();
    }

    /// Sets what the spinner steps through.
    public final void setValueFactory(SpinnerValueFactory<T> newValue) {
        valueFactory.set(newValue);
    }

    /// What the spinner steps through.
    public final ObjectProperty<SpinnerValueFactory<T>> valueFactoryProperty() {
        return valueFactory;
    }

    /// Returns whether a value can be typed.
    public final boolean isEditable() {
        return editable.get();
    }

    /// Sets whether a value can be typed.
    public final void setEditable(boolean newValue) {
        editable.set(newValue);
    }

    /// Whether a value can be typed.
    public final BooleanProperty editableProperty() {
        return editable;
    }

    /// Returns the text field the value is shown in.
    public final TextField getEditor() {
        return editor;
    }

    /// The text field the value is shown in.
    public final ReadOnlyObjectProperty<TextField> editorProperty() {
        return new ReadOnlyObjectWrapper<TextField>(this, "editor", editor).getReadOnlyProperty();
    }
}
