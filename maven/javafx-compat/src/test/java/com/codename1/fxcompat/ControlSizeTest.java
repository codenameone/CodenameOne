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
package com.codename1.fxcompat;

import java.util.ArrayList;
import java.util.List;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// The size a layout pane gives a control whose preferred size was set.
public class ControlSizeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Renderer.setTrace(null);
        Units.setScale(0);
    }

    /// The toolbar of 2048FX: buttons with no text, sized by the
    /// application and drawn as an icon by `-fx-shape`.
    @Test
    public void aButtonWithNoTextKeepsThePreferredSizeItWasGiven() {
        Button icon = new Button();
        icon.setPrefSize(40, 40);
        assertEquals(40, icon.maxWidth(-1), 0.01);
        assertEquals(40, icon.maxHeight(-1), 0.01);
        HBox bar = new HBox(icon);
        bar.resize(300, 60);
        bar.layout();
        assertEquals(40, icon.getWidth(), 0.01);
        assertEquals(40, icon.getHeight(), 0.01);
    }

    @Test
    public void aStackPaneDoesNotStretchAButtonPastItsPreferredSize() {
        Button icon = new Button();
        icon.setPrefSize(40, 30);
        StackPane pane = new StackPane(icon);
        pane.resize(200, 200);
        pane.layout();
        assertEquals(40, icon.getWidth(), 0.01);
        assertEquals(30, icon.getHeight(), 0.01);
    }

    @Test
    public void theShapeOfASizedButtonIsFilled() {
        Button icon = new Button();
        icon.setPrefSize(40, 40);
        icon.setStyle("-fx-shape: \"M0 0h4v12h-4z\"; -fx-background-color: #f9f6f2;");
        // A style is applied by the scene, at its layout.
        new javafx.scene.Scene(new HBox(icon), 300, 60).cn1Layout(300, 60);
        assertEquals(40, icon.getWidth(), 0.01);
        final List<String> drawn = new ArrayList<String>();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                drawn.add(operation + ":" + Math.round(deviceBounds[2]) + "x" + Math.round(deviceBounds[3]));
            }
        });
        icon.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(100, 100, 0).getGraphics(), 0, 0));
        assertTrue("drew " + drawn, drawn.contains("fill:40x40"));
    }

    /// A label is its text and nothing around it: the padding the native
    /// theme gives its own labels is not part of a JavaFX one, and with it
    /// every row of labels stood taller than the original.
    @Test
    public void aLabelIsAsTallAsItsLineOfText() {
        javafx.scene.control.Label label = new javafx.scene.control.Label("First Name");
        label.setFont(javafx.scene.text.Font.font(20));
        javafx.scene.text.Text text = new javafx.scene.text.Text("First Name");
        text.setFont(javafx.scene.text.Font.font(20));
        new javafx.scene.Scene(new HBox(label, text), 300, 60).cn1Layout(300, 60);
        double line = text.getLayoutBounds().getHeight();
        assertEquals(line, label.prefHeight(-1), 2.5);
    }

    /// The last letter of an italic leans past the width its text
    /// measures, and a native label clips its text to that width: a label
    /// given its preferred width is wider by the lean, and all of the
    /// letter is drawn. The display here draws a text as a box that many
    /// pixels wider than it measures, which is what a port does.
    @Test
    public void anItalicLabelHasRoomForTheLeanOfItsLastLetter() {
        boolean before = HeadlessImplementation.rasterImages;
        HeadlessImplementation.rasterImages = true;
        HeadlessImplementation.trackClip = true;
        HeadlessImplementation.textOverhang = 5;
        com.codename1.fxcompat.runtime.Fonts.flush();
        javafx.stage.Stage stage = new javafx.stage.Stage();
        try {
            javafx.scene.control.Label upright = new javafx.scene.control.Label("Small");
            javafx.scene.control.Label italic = new javafx.scene.control.Label("Small");
            italic.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontPosture.ITALIC, 12));
            italic.setTextFill(javafx.scene.paint.Color.RED);
            javafx.scene.control.Label spaced = new javafx.scene.control.Label("Small ");
            spaced.setFont(italic.getFont());
            stage.setScene(new javafx.scene.Scene(new javafx.scene.layout.VBox(upright, italic, spaced), 600, 400));
            stage.show();
            int text = 5 * HeadlessImplementation.CHAR_WIDTH;
            assertEquals(text, upright.getWidth(), 0.01);
            assertEquals(text + 5, italic.getWidth(), 0.01);
            assertEquals("nothing leans out of a space", text + HeadlessImplementation.CHAR_WIDTH, spaced.getWidth(),
                    0.01);
            com.codename1.ui.Form f = (com.codename1.ui.Form) stage.cn1Host();
            f.revalidate();
            com.codename1.ui.Image picture = com.codename1.ui.Image.createImage(HeadlessImplementation.WIDTH,
                    HeadlessImplementation.HEIGHT, 0xffffffff);
            com.codename1.ui.Graphics g = picture.getGraphics();
            g.setClip(0, 0, HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
            f.paintComponent(g);
            int[] rgb = picture.getRGB();
            int red = 0;
            int row = -1;
            for (int i = 0; i < rgb.length && (row < 0 || i / HeadlessImplementation.WIDTH == row); i++) {
                if (rgb[i] == 0xffff0000) {
                    row = i / HeadlessImplementation.WIDTH;
                    red++;
                }
            }
            assertEquals("the pixels of one row of the italic text", text + 5, red);
        } finally {
            stage.hide();
            HeadlessImplementation.textOverhang = 0;
            HeadlessImplementation.trackClip = false;
            HeadlessImplementation.rasterImages = before;
            HeadlessImplementation.resetRaster();
            com.codename1.fxcompat.runtime.Fonts.flush();
        }
    }

    /// The standard theme draws a column's header bold, and a style sheet
    /// that only changes its size keeps that.
    @Test
    public void aColumnHeaderIsBoldUntilAStyleSheetSaysOtherwise() {
        javafx.scene.control.TableView<String> table = new javafx.scene.control.TableView<String>();
        table.getColumns().add(new javafx.scene.control.TableColumn<String, String>("First Name"));
        new javafx.scene.Scene(new StackPane(table), 300, 200).cn1Layout(300, 200);
        javafx.scene.Node found = table.lookup(".column-header .label");
        assertTrue("no header label", found instanceof javafx.scene.control.Label);
        javafx.scene.control.Label header = (javafx.scene.control.Label) found;
        assertTrue("header font is " + header.getFont().getStyle(),
                header.getFont().getStyle().indexOf("Bold") >= 0);
    }

    /// Ensemble's coloured buttons: `setStyle("-fx-base: ...")` on a
    /// button that has no other style. Only the standard theme's button
    /// is coloured by that name, so the button takes that look.
    @Test
    public void anInlineBaseColourMakesAButtonOfThatColour() {
        Button plain = new Button("Plain");
        Button red = new Button("Red");
        red.setStyle("-fx-base: #ff0000;");
        new javafx.scene.Scene(new HBox(plain, red), 300, 60).cn1Layout(300, 60);
        assertTrue("a plain button is the native theme's", plain.getBackground() == null
                || plain.getBackground().getFills().isEmpty());
        javafx.scene.layout.Background drawn = red.getBackground();
        assertTrue("the red button draws itself", drawn != null && drawn.getFills().size() == 4);
        Paint body = drawn.getFills().get(3).getFill();
        assertTrue("the body is a gradient", body instanceof javafx.scene.paint.LinearGradient);
        javafx.scene.paint.Color top = ((javafx.scene.paint.LinearGradient) body).getStops().get(0).getColor();
        assertTrue("of the base colour: " + top, top.getRed() > 0.9 && top.getBlue() < 0.3);
        // Light text on a dark colour, as the theme's ladder picks it.
        assertEquals(javafx.scene.paint.Color.WHITE, red.getTextFill());
        // Two thirds of an em at either side.
        assertEquals(javafx.scene.text.Font.getDefault().getSize() * 2 / 3, red.getPadding().getLeft(), 0.1);

        red.setStyle("");
        new javafx.scene.Scene(new HBox(red), 300, 60).cn1Layout(300, 60);
        assertTrue("and the native look again without it", red.getBackground() == null
                || red.getBackground().getFills().isEmpty());
    }

    @Test
    public void aLabelGivenAWidthNarrowerThanItsTextKeepsThatWidth() {
        // The text is cut short with an ellipsis; the label is not made
        // as wide as the text, before it is shown or after.
        javafx.scene.layout.TilePane tiles = new javafx.scene.layout.TilePane(5, 5);
        javafx.scene.control.Label[] labels = new javafx.scene.control.Label[3];
        String[] texts = {"HAND", "A_VERY_LONG_NAME_THAT_DOES_NOT_FIT_AT_ALL", "MOVE"};
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new javafx.scene.control.Label(texts[i]);
            labels[i].setAlignment(javafx.geometry.Pos.CENTER);
            labels[i].setPrefSize(85, 65);
            labels[i].setStyle("-fx-border-color: #aaaaaa; -fx-background-color: #dddddd;");
            tiles.getChildren().add(labels[i]);
        }
        assertEquals(85, labels[1].prefWidth(-1), 0.01);
        // Five columns is what a tile pane asks for when it is not told.
        assertEquals(5 * 85 + 4 * 5, tiles.prefWidth(-1), 0.01);
        javafx.scene.Scene scene = new javafx.scene.Scene(tiles, 600, 400);
        scene.cn1Layout(600, 400);
        scene.cn1Layout(600, 400);
        assertEquals(85, labels[1].getWidth(), 0.01);
        assertEquals(90, labels[1].getLayoutX() - labels[0].getLayoutX(), 0.01);
        assertEquals(85, labels[1].prefWidth(-1), 0.01);
    }
}
