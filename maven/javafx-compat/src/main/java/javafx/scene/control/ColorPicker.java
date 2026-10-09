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

import com.codename1.fxcompat.runtime.StagePopup;
import com.codename1.ui.Component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/// A button that shows a colour and opens a palette to choose another.
///
/// The button is a swatch of the value beside its name, or its
/// `#rrggbb` when it has none of the few names known here. The palette is
/// a row of greys over rows of twelve hues from light to dark, and under
/// them the colours of [#getCustomColors()]. Choosing one sets the value,
/// which fires the action handler, and closes the palette.
///
/// There is no dialog to mix a colour: custom colours are the ones the
/// application puts in the list. The style classes [#STYLE_CLASS_BUTTON]
/// and [#STYLE_CLASS_SPLIT_BUTTON] are accepted and change nothing.
public class ColorPicker extends ComboBoxBase<Color> {

    /// The style class that asks for a plain button.
    public static final String STYLE_CLASS_BUTTON = "button";
    /// The style class that asks for a button with a separate arrow.
    public static final String STYLE_CLASS_SPLIT_BUTTON = "split-button";

    private static final int COLUMNS = 12;
    private static final double SWATCH = 15;
    private static final String[] NAMES = {"#ffffff", "White", "#000000", "Black", "#ff0000", "Red", "#008000",
        "Green", "#0000ff", "Blue", "#ffff00", "Yellow", "#ffa500", "Orange", "#ffc0cb", "Pink", "#800080",
        "Purple", "#808080", "Gray", "#00ffff", "Cyan", "#ff00ff", "Magenta", "#a52a2a", "Brown", "#00ff00",
        "Lime"};

    private final ObservableList<Color> customColors = FXCollections.observableArrayList();
    private final Rectangle swatch = new Rectangle(SWATCH, SWATCH);
    private final Label name = new Label();
    private Palette palette;

    /// The window of the palette.
    private static final class Palette extends PopupControl {
        Palette(javafx.scene.Parent content) {
            setAutoHide(true);
            setScene(new Scene(content));
        }
    }

    /// Creates a picker that shows white.
    public ColorPicker() {
        this(Color.WHITE);
    }

    /// Creates a picker that shows a colour.
    public ColorPicker(Color color) {
        getStyleClass().add("color-picker");
        swatch.setStroke(Color.gray(0.4));
        // The arrow every combo box of the standard theme ends in.
        javafx.scene.shape.Polygon arrow = new javafx.scene.shape.Polygon(0, 0, 7, 0, 3.5, 4);
        arrow.setFill(Color.gray(0.25));
        HBox row = new HBox(6, swatch, name, arrow);
        HBox.setMargin(arrow, new javafx.geometry.Insets(0, 0, 0, 4));
        row.setAlignment(Pos.CENTER_LEFT);
        cn1MadeOf(row);
        super.valueProperty().addListener((observable, was, now) -> shown());
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (!isDisabled()) {
                if (isShowing()) {
                    hide();
                } else {
                    show();
                }
                e.consume();
            }
        });
        setValue(color);
        shown();
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    @Override
    public String cn1DefaultStyle() {
        return "-fx-background-color: -fx-outer-border, -fx-inner-border, linear-gradient(to bottom,"
                + " derive(-fx-color, 8%), derive(-fx-color, -8%)); -fx-background-insets: 0, 1, 2;"
                + " -fx-background-radius: 3, 2, 1; -fx-padding: 0.333333em 0.666667em 0.333333em 0.666667em;";
    }

    /// The text the button shows for a colour.
    static String describe(Color c) {
        if (c == null) {
            return "";
        }
        String hex = hex(c);
        for (int i = 0; i + 1 < NAMES.length; i += 2) {
            if (NAMES[i].equals(hex)) {
                return NAMES[i + 1];
            }
        }
        return hex;
    }

    private static String hex(Color c) {
        int rgb = c.cn1Argb() & 0xffffff;
        String digits = Integer.toHexString(0x1000000 | rgb);
        return "#" + digits.substring(1);
    }

    private void shown() {
        Color c = getValue();
        swatch.setFill(c == null ? Color.TRANSPARENT : c);
        name.setText(describe(c));
        requestLayout();
    }

    /// The colours of the palette, row after row.
    static Color[] paletteColors() {
        int rows = 9;
        Color[] out = new Color[COLUMNS * (rows + 1)];
        for (int i = 0; i < COLUMNS; i++) {
            out[i] = Color.gray(1 - i / (double) (COLUMNS - 1));
        }
        for (int r = 0; r < rows; r++) {
            // Light tints first, the pure hue in the middle, shades after.
            double saturation = r < 4 ? 0.2 + 0.2 * r : 1;
            double brightness = r <= 4 ? 1 : 1 - 0.18 * (r - 4);
            for (int i = 0; i < COLUMNS; i++) {
                out[(r + 1) * COLUMNS + i] = Color.hsb(360.0 * i / COLUMNS, saturation, brightness);
            }
        }
        return out;
    }

    private void add(GridPane grid, final Color c, int column, int row) {
        Rectangle r = new Rectangle(SWATCH, SWATCH, c);
        r.setStroke(Color.gray(0.75));
        r.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            setValue(c);
            hide();
            e.consume();
        });
        grid.add(r, column, row);
    }

    private javafx.scene.Parent content() {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        Color[] all = paletteColors();
        for (int i = 0; i < all.length; i++) {
            add(grid, all[i], i % COLUMNS, i / COLUMNS);
        }
        VBox box = new VBox(6, grid);
        if (!customColors.isEmpty()) {
            GridPane own = new GridPane();
            own.setHgap(1);
            own.setVgap(1);
            for (int i = 0; i < customColors.size(); i++) {
                add(own, customColors.get(i), i % COLUMNS, i / COLUMNS);
            }
            box.getChildren().addAll(new Label("Custom Colors"), own);
        }
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: -fx-box-border, -fx-background; -fx-background-insets: 0, 1;");
        return box;
    }

    /// Opens the palette under the button.
    @Override
    public void show() {
        if (isDisabled() || getScene() == null) {
            return;
        }
        if (palette != null) {
            palette.hide();
        }
        palette = new Palette(content());
        palette.setOnHidden(e -> chooserShown(false));
        Point2D at = StagePopup.anchor(this, 0, getHeight());
        palette.show(this, at.getX(), at.getY());
        chooserShown(true);
    }

    /// Closes the palette.
    @Override
    public void hide() {
        if (palette != null) {
            Palette open = palette;
            palette = null;
            open.hide();
        }
        chooserShown(false);
    }

    /// Returns the colours the palette shows under its own.
    public final ObservableList<Color> getCustomColors() {
        return customColors;
    }
}
