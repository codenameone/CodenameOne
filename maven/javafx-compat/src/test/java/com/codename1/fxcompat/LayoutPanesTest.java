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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Units;

import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/// The layout panes: the bounds each gives its children and the sizes it
/// asks for. The expected numbers are worked out by hand from the layout
/// rules JavaFX documents, in logical pixels.
public class LayoutPanesTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
    }

    /// A region that prefers a size and may be stretched or squeezed.
    private static Region box(double w, double h) {
        Region r = new Region();
        r.setPrefSize(w, h);
        return r;
    }

    /// A region that always keeps one size.
    private static Region fixed(double w, double h) {
        Region r = box(w, h);
        r.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        r.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return r;
    }

    /// A region whose height depends on its width: 1000 / width.
    private static final class Wrapping extends Region {
        @Override
        public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override
        protected double computePrefWidth(double height) {
            return 100;
        }

        @Override
        protected double computePrefHeight(double width) {
            return width < 0 ? 10 : 1000 / width;
        }
    }

    private static void lay(Pane pane, double w, double h) {
        pane.resize(w, h);
        pane.requestLayout();
        pane.layout();
    }

    private static void assertBounds(String what, Region r, double x, double y, double w, double h) {
        assertEquals(what + " x", x, r.getLayoutX(), 1e-9);
        assertEquals(what + " y", y, r.getLayoutY(), 1e-9);
        assertEquals(what + " width", w, r.getWidth(), 1e-9);
        assertEquals(what + " height", h, r.getHeight(), 1e-9);
    }

    // ------------------------------------------------------------- HBox

    @Test
    public void hboxPlacesChildrenInARowWithSpacingAndPadding() {
        Region a = box(30, 20);
        Region b = box(50, 40);
        HBox box = new HBox(10, a, b);
        box.setPadding(new Insets(5));
        assertEquals(100, box.prefWidth(-1), 0);
        assertEquals(50, box.prefHeight(-1), 0);
        // The children can shrink to nothing: padding and spacing remain.
        assertEquals(20, box.minWidth(-1), 0);
        lay(box, 200, 100);
        // fillHeight stretches both to the 90 between the paddings.
        assertBounds("a", a, 5, 5, 30, 90);
        assertBounds("b", b, 45, 5, 50, 90);
    }

    @Test
    public void hboxAlignmentPlacesTheRowAndTheChildrenThatDoNotFill() {
        Region a = box(30, 20);
        Region b = box(50, 40);
        HBox box = new HBox(10, a, b);
        box.setPadding(new Insets(5));
        box.setFillHeight(false);
        box.setAlignment(Pos.CENTER);
        lay(box, 200, 100);
        // 190 inside, 90 of content: 50 spare on each side.
        assertBounds("a", a, 55, 40, 30, 20);
        assertBounds("b", b, 95, 30, 50, 40);
        box.setAlignment(Pos.BOTTOM_RIGHT);
        assertTrue(box.isNeedsLayout());
        box.layout();
        assertBounds("a", a, 105, 75, 30, 20);
        assertBounds("b", b, 145, 55, 50, 40);
    }

    @Test
    public void hboxGivesSpareWidthToTheChildrenThatGrow() {
        Region a = box(30, 20);
        Region b = box(50, 20);
        Region c = box(20, 20);
        HBox box = new HBox(a, b, c);
        HBox.setHgrow(b, Priority.ALWAYS);
        assertSame(Priority.ALWAYS, HBox.getHgrow(b));
        lay(box, 200, 20);
        assertBounds("a", a, 0, 0, 30, 20);
        assertBounds("b", b, 30, 0, 150, 20);
        assertBounds("c", c, 180, 0, 20, 20);

        // Two growing children share the 100 spare pixels equally.
        HBox.setHgrow(a, Priority.ALWAYS);
        lay(box, 200, 20);
        assertBounds("a", a, 0, 0, 80, 20);
        assertBounds("b", b, 80, 0, 100, 20);
        assertBounds("c", c, 180, 0, 20, 20);

        // A maximum stops one of them; the other takes what it leaves.
        a.setMaxWidth(50);
        lay(box, 200, 20);
        assertBounds("a", a, 0, 0, 50, 20);
        assertBounds("b", b, 50, 0, 130, 20);
        assertBounds("c", c, 180, 0, 20, 20);

        // SOMETIMES only gets what ALWAYS could not take.
        HBox.setHgrow(b, null);
        HBox.setHgrow(c, Priority.SOMETIMES);
        lay(box, 200, 20);
        assertBounds("a", a, 0, 0, 50, 20);
        assertBounds("b", b, 50, 0, 50, 20);
        assertBounds("c", c, 100, 0, 100, 20);

        HBox.clearConstraints(c);
        assertNull(HBox.getHgrow(c));
    }

    @Test
    public void hboxShrinksChildrenDownToTheirMinimum() {
        Region a = box(100, 20);
        a.setMinWidth(80);
        Region b = box(100, 20);
        HBox box = new HBox(a, b);
        lay(box, 150, 20);
        // 50 too narrow: a can only give 20, b gives the other 30.
        assertBounds("a", a, 0, 0, 80, 20);
        assertBounds("b", b, 80, 0, 70, 20);
        assertEquals(80, box.minWidth(-1), 0);
    }

    @Test
    public void hboxMarginSurroundsAChild() {
        Region a = box(30, 20);
        Region b = box(50, 20);
        HBox box = new HBox(a, b);
        HBox.setMargin(a, new Insets(1, 2, 3, 4));
        assertEquals(new Insets(1, 2, 3, 4), HBox.getMargin(a));
        assertEquals(86, box.prefWidth(-1), 0);
        assertEquals(24, box.prefHeight(-1), 0);
        lay(box, 200, 50);
        assertBounds("a", a, 4, 1, 30, 46);
        assertBounds("b", b, 36, 0, 50, 50);
    }

    @Test
    public void hboxBaselineAlignmentLevelsTheBottomsOfChildrenWithoutText() {
        Region a = fixed(10, 20);
        Region b = fixed(10, 50);
        HBox box = new HBox(a, b);
        box.setAlignment(Pos.BASELINE_LEFT);
        assertEquals(50, box.prefHeight(-1), 0);
        lay(box, 100, 80);
        assertBounds("a", a, 0, 30, 10, 20);
        assertBounds("b", b, 10, 0, 10, 50);
    }

    @Test
    public void hboxSkipsUnmanagedChildren() {
        Region a = box(30, 20);
        Region skipped = box(500, 500);
        skipped.setManaged(false);
        skipped.resize(7, 7);
        skipped.relocate(3, 3);
        Region b = box(50, 20);
        HBox box = new HBox(a, skipped, b);
        assertEquals(80, box.prefWidth(-1), 0);
        lay(box, 200, 20);
        assertBounds("b", b, 30, 0, 50, 20);
        assertBounds("skipped", skipped, 3, 3, 7, 7);
    }

    @Test
    public void hboxSnapsToDevicePixels() {
        Units.setScale(2);
        Region a = box(10.2, 10);
        Region b = box(10, 10);
        HBox box = new HBox(0.3, a, b);
        lay(box, 100, 10);
        // 10.2 grows to 10.5 (21 device pixels); the gap rounds to 0.5.
        assertBounds("a", a, 0, 0, 10.5, 10);
        assertBounds("b", b, 11, 0, 10, 10);
        assertEquals(21, box.prefWidth(-1), 0);
    }

    @Test
    public void hboxTakesStyleValuesAndRestoresThem() {
        HBox box = new HBox(3);
        assertTrue(box.cn1ApplyStyle("-fx-spacing", Integer.valueOf(12)));
        assertEquals(12, box.getSpacing(), 0);
        assertTrue(box.cn1ApplyStyle("-fx-spacing", null));
        assertEquals(3, box.getSpacing(), 0);
        assertFalse(box.cn1ApplyStyle("-fx-spacing", "wide"));

        assertTrue(box.cn1ApplyStyle("-fx-alignment", "Center-LEFT"));
        assertSame(Pos.CENTER_LEFT, box.getAlignment());
        assertTrue(box.cn1ApplyStyle("-fx-alignment", Pos.BOTTOM_RIGHT));
        assertSame(Pos.BOTTOM_RIGHT, box.getAlignment());
        assertFalse(box.cn1ApplyStyle("-fx-alignment", "sideways"));
        assertSame(Pos.BOTTOM_RIGHT, box.getAlignment());
        assertTrue(box.cn1ApplyStyle("-fx-alignment", null));
        assertSame(Pos.TOP_LEFT, box.getAlignment());

        assertTrue(box.cn1ApplyStyle("-fx-fill-height", Boolean.FALSE));
        assertFalse(box.isFillHeight());
        assertTrue(box.cn1ApplyStyle("-fx-fill-height", null));
        assertTrue(box.isFillHeight());
        assertFalse(box.cn1ApplyStyle("-fx-fill-width", Boolean.FALSE));
        assertFalse(box.cn1ApplyStyle("-fx-hgap", Integer.valueOf(1)));
    }

    // ------------------------------------------------------------- VBox

    @Test
    public void vboxPlacesChildrenInAColumn() {
        Region a = box(30, 20);
        Region b = box(50, 40);
        VBox box = new VBox(5, a, b);
        assertEquals(50, box.prefWidth(-1), 0);
        assertEquals(65, box.prefHeight(-1), 0);
        lay(box, 100, 165);
        assertBounds("a", a, 0, 0, 100, 20);
        assertBounds("b", b, 0, 25, 100, 40);

        box.setFillWidth(false);
        box.setAlignment(Pos.CENTER);
        box.layout();
        // 100 of spare height: 50 above; each child centered on its own.
        assertBounds("a", a, 35, 50, 30, 20);
        assertBounds("b", b, 25, 75, 50, 40);

        box.setAlignment(Pos.TOP_RIGHT);
        VBox.setVgrow(b, Priority.ALWAYS);
        VBox.setMargin(a, new Insets(2, 4, 0, 0));
        lay(box, 100, 165);
        assertBounds("a", a, 66, 2, 30, 20);
        // a's area is 22 tall, then the gap; b takes the rest.
        assertBounds("b", b, 50, 27, 50, 138);
        VBox.clearConstraints(b);
        assertNull(VBox.getVgrow(b));
    }

    @Test
    public void vboxGivesAWidthBiasedChildTheHeightItsWidthNeeds() {
        Wrapping text = new Wrapping();
        VBox box = new VBox(text);
        assertSame(Orientation.HORIZONTAL, box.getContentBias());
        assertEquals(10, box.prefHeight(-1), 0);
        assertEquals(20, box.prefHeight(50), 0);
        lay(box, 50, 100);
        assertBounds("text", text, 0, 0, 50, 20);
    }

    @Test
    public void vboxTakesStyleValuesAndRestoresThem() {
        VBox box = new VBox();
        assertTrue(box.cn1ApplyStyle("-fx-spacing", Double.valueOf(4.5)));
        assertEquals(4.5, box.getSpacing(), 0);
        assertTrue(box.cn1ApplyStyle("-fx-alignment", "bottom-center"));
        assertSame(Pos.BOTTOM_CENTER, box.getAlignment());
        assertTrue(box.cn1ApplyStyle("-fx-fill-width", Boolean.FALSE));
        assertFalse(box.isFillWidth());
        assertFalse(box.cn1ApplyStyle("-fx-fill-width", "no"));
        assertTrue(box.cn1ApplyStyle("-fx-spacing", null));
        assertTrue(box.cn1ApplyStyle("-fx-alignment", null));
        assertTrue(box.cn1ApplyStyle("-fx-fill-width", null));
        assertEquals(0, box.getSpacing(), 0);
        assertSame(Pos.TOP_LEFT, box.getAlignment());
        assertTrue(box.isFillWidth());
        assertFalse(box.cn1ApplyStyle("-fx-fill-height", Boolean.TRUE));
    }

    // -------------------------------------------------------- StackPane

    @Test
    public void stackPaneStacksChildrenInItsContentArea() {
        Region small = fixed(30, 20);
        Region big = box(50, 40);
        StackPane stack = new StackPane(big, small);
        stack.setPadding(new Insets(10));
        assertEquals(70, stack.prefWidth(-1), 0);
        assertEquals(60, stack.prefHeight(-1), 0);
        // The fixed child cannot shrink below 30 x 20.
        assertEquals(50, stack.minWidth(-1), 0);
        assertEquals(40, stack.minHeight(-1), 0);
        lay(stack, 100, 80);
        assertBounds("big", big, 10, 10, 80, 60);
        assertBounds("small", small, 35, 30, 30, 20);

        StackPane.setAlignment(small, Pos.BOTTOM_RIGHT);
        lay(stack, 100, 80);
        assertBounds("small", small, 60, 50, 30, 20);

        StackPane.setMargin(small, new Insets(0, 5, 7, 0));
        lay(stack, 100, 80);
        assertBounds("small", small, 55, 43, 30, 20);

        StackPane.clearConstraints(small);
        assertNull(StackPane.getAlignment(small));
        assertNull(StackPane.getMargin(small));
        stack.setAlignment(Pos.TOP_LEFT);
        stack.layout();
        assertBounds("small", small, 10, 10, 30, 20);
    }

    @Test
    public void stackPaneTakesStyleValuesAndRestoresThem() {
        StackPane stack = new StackPane();
        assertTrue(stack.cn1ApplyStyle("-fx-alignment", "top-right"));
        assertSame(Pos.TOP_RIGHT, stack.getAlignment());
        assertFalse(stack.cn1ApplyStyle("-fx-alignment", Integer.valueOf(3)));
        assertTrue(stack.cn1ApplyStyle("-fx-alignment", null));
        assertSame(Pos.CENTER, stack.getAlignment());
        assertFalse(stack.cn1ApplyStyle("-fx-spacing", Integer.valueOf(3)));
    }

    // ------------------------------------------------------- BorderPane

    @Test
    public void borderPanePlacesTheFivePositions() {
        Region top = box(10, 20);
        Region bottom = box(10, 30);
        Region left = box(40, 10);
        Region right = box(50, 10);
        Region center = box(5, 5);
        BorderPane pane = new BorderPane(center, top, right, bottom, left);
        assertEquals(5, pane.getChildren().size());
        assertEquals(95, pane.prefWidth(-1), 0);
        assertEquals(60, pane.prefHeight(-1), 0);
        lay(pane, 300, 200);
        assertBounds("top", top, 0, 0, 300, 20);
        assertBounds("bottom", bottom, 0, 170, 300, 30);
        assertBounds("left", left, 0, 20, 40, 150);
        assertBounds("right", right, 250, 20, 50, 150);
        assertBounds("center", center, 40, 20, 210, 150);

        BorderPane.setMargin(center, new Insets(5));
        lay(pane, 300, 200);
        assertBounds("center", center, 45, 25, 200, 140);
    }

    @Test
    public void borderPaneAlignsChildrenThatDoNotFillTheirArea() {
        Region top = fixed(100, 20);
        Region center = fixed(60, 40);
        BorderPane pane = new BorderPane();
        pane.setTop(top);
        pane.setCenter(center);
        lay(pane, 300, 200);
        assertBounds("top", top, 0, 0, 100, 20);
        // The center has 300 x 180 below the top.
        assertBounds("center", center, 120, 90, 60, 40);

        BorderPane.setAlignment(top, Pos.CENTER);
        BorderPane.setAlignment(center, Pos.TOP_LEFT);
        lay(pane, 300, 200);
        assertBounds("top", top, 100, 0, 100, 20);
        assertBounds("center", center, 0, 20, 60, 40);
        BorderPane.clearConstraints(top);
        assertNull(BorderPane.getAlignment(top));
    }

    @Test
    public void borderPaneMarginOfAnEdgeMovesTheMiddle() {
        Region top = box(10, 20);
        Region left = box(40, 10);
        BorderPane pane = new BorderPane();
        pane.setTop(top);
        pane.setLeft(left);
        BorderPane.setMargin(top, new Insets(2, 3, 4, 5));
        lay(pane, 300, 200);
        assertBounds("top", top, 5, 2, 292, 20);
        assertBounds("left", left, 0, 26, 40, 174);
    }

    @Test
    public void borderPanePropertiesKeepTheChildrenInStep() {
        Region first = box(1, 1);
        Region second = box(1, 1);
        BorderPane pane = new BorderPane(first);
        assertSame(first, pane.getCenter());
        assertSame(pane, first.getParent());
        pane.setCenter(second);
        assertNull(first.getParent());
        assertEquals(1, pane.getChildren().size());
        assertSame(second, pane.getChildren().get(0));
        pane.setTop(first);
        assertEquals(2, pane.getChildren().size());
        pane.setTop(null);
        pane.setCenter(null);
        assertTrue(pane.getChildren().isEmpty());
    }

    // ------------------------------------------------------- AnchorPane

    @Test
    public void anchorPaneKeepsEdgesAtTheirAnchors() {
        Region a = box(30, 20);
        Region b = box(30, 20);
        Region c = box(30, 20);
        AnchorPane pane = new AnchorPane(a, b, c);
        pane.setPadding(new Insets(10));
        AnchorPane.setTopAnchor(a, Double.valueOf(5));
        AnchorPane.setLeftAnchor(a, Double.valueOf(7));
        AnchorPane.setLeftAnchor(b, Double.valueOf(10));
        AnchorPane.setRightAnchor(b, Double.valueOf(20));
        AnchorPane.setTopAnchor(b, Double.valueOf(5));
        AnchorPane.setBottomAnchor(b, Double.valueOf(5));
        AnchorPane.setRightAnchor(c, Double.valueOf(0));
        AnchorPane.setBottomAnchor(c, Double.valueOf(0));
        assertEquals(Double.valueOf(20), AnchorPane.getRightAnchor(b));
        // b needs 10 + 30 + 20 across and 5 + 20 + 5 down, plus padding.
        assertEquals(80, pane.prefWidth(-1), 0);
        assertEquals(50, pane.prefHeight(-1), 0);
        lay(pane, 200, 100);
        assertBounds("a", a, 17, 15, 30, 20);
        // Anchored on both sides: stretched between them.
        assertBounds("b", b, 20, 15, 150, 70);
        assertBounds("c", c, 160, 70, 30, 20);

        AnchorPane.clearConstraints(b);
        assertNull(AnchorPane.getLeftAnchor(b));
        assertNull(AnchorPane.getTopAnchor(b));
        assertNull(AnchorPane.getRightAnchor(b));
        assertNull(AnchorPane.getBottomAnchor(b));
        lay(pane, 200, 100);
        // Without anchors it keeps its position and takes its own size.
        assertBounds("b", b, 20, 15, 30, 20);
    }

    // --------------------------------------------------------- FlowPane

    @Test
    public void flowPaneWrapsRowsAtItsWidth() {
        Region a = fixed(40, 20);
        Region b = fixed(40, 20);
        Region c = fixed(40, 20);
        Region d = fixed(40, 30);
        FlowPane flow = new FlowPane(5, 10, a, b, c, d);
        assertSame(Orientation.HORIZONTAL, flow.getContentBias());
        assertEquals(400, flow.prefWidth(-1), 0);
        flow.setPrefWrapLength(100);
        assertEquals(100, flow.prefWidth(-1), 0);
        // Two rows of 20 and 30 with a gap of 10.
        assertEquals(60, flow.prefHeight(-1), 0);
        // In 200 everything fits one row as tall as d.
        assertEquals(30, flow.prefHeight(200), 0);
        assertEquals(40, flow.minWidth(-1), 0);
        lay(flow, 100, 100);
        assertBounds("a", a, 0, 0, 40, 20);
        assertBounds("b", b, 45, 0, 40, 20);
        // c is centered in a row as tall as d.
        assertBounds("c", c, 0, 35, 40, 20);
        assertBounds("d", d, 45, 30, 40, 30);

        flow.setRowValignment(VPos.TOP);
        flow.layout();
        assertBounds("c", c, 0, 30, 40, 20);

        flow.setAlignment(Pos.CENTER);
        lay(flow, 105, 100);
        // Rows are 85 wide in 105, the content 60 tall in 100.
        assertBounds("a", a, 10, 20, 40, 20);
        assertBounds("b", b, 55, 20, 40, 20);
        assertBounds("c", c, 10, 50, 40, 20);
        assertBounds("d", d, 55, 50, 40, 30);
    }

    @Test
    public void flowPaneMarginCountsTowardsTheRow() {
        Region a = fixed(40, 20);
        Region b = fixed(40, 20);
        Region c = fixed(40, 20);
        FlowPane flow = new FlowPane(5, 10, a, b, c);
        FlowPane.setMargin(a, new Insets(0, 0, 0, 6));
        lay(flow, 135, 100);
        // 46 + 5 + 40 + 5 + 40 is one more than fits.
        assertBounds("a", a, 6, 0, 40, 20);
        assertBounds("b", b, 51, 0, 40, 20);
        assertBounds("c", c, 0, 30, 40, 20);
        FlowPane.clearConstraints(a);
        assertNull(FlowPane.getMargin(a));
        lay(flow, 135, 100);
        assertBounds("c", c, 90, 0, 40, 20);
    }

    @Test
    public void flowPaneVerticalWrapsColumnsAtItsHeight() {
        Region a = fixed(40, 20);
        Region b = fixed(40, 20);
        Region c = fixed(30, 20);
        Region d = fixed(40, 30);
        FlowPane flow = new FlowPane(Orientation.VERTICAL, 5, 10, a, b, c, d);
        assertSame(Orientation.VERTICAL, flow.getContentBias());
        lay(flow, 200, 60);
        assertBounds("a", a, 0, 0, 40, 20);
        assertBounds("b", b, 0, 30, 40, 20);
        assertBounds("c", c, 45, 0, 30, 20);
        assertBounds("d", d, 45, 30, 40, 30);
        flow.setColumnHalignment(HPos.RIGHT);
        flow.layout();
        assertBounds("c", c, 55, 0, 30, 20);
        // Two columns of 40 with a gap of 5.
        assertEquals(85, flow.prefWidth(60), 0);
        assertEquals(30, flow.minHeight(-1), 0);
    }

    @Test
    public void flowPaneTakesStyleValuesAndRestoresThem() {
        FlowPane flow = new FlowPane();
        assertTrue(flow.cn1ApplyStyle("-fx-hgap", Integer.valueOf(4)));
        assertTrue(flow.cn1ApplyStyle("-fx-vgap", Integer.valueOf(6)));
        assertTrue(flow.cn1ApplyStyle("-fx-alignment", "center"));
        assertTrue(flow.cn1ApplyStyle("-fx-orientation", "VERTICAL"));
        assertEquals(4, flow.getHgap(), 0);
        assertEquals(6, flow.getVgap(), 0);
        assertSame(Pos.CENTER, flow.getAlignment());
        assertSame(Orientation.VERTICAL, flow.getOrientation());
        assertFalse(flow.cn1ApplyStyle("-fx-orientation", "diagonal"));
        assertTrue(flow.cn1ApplyStyle("-fx-orientation", Orientation.HORIZONTAL));
        assertTrue(flow.cn1ApplyStyle("-fx-hgap", null));
        assertTrue(flow.cn1ApplyStyle("-fx-vgap", null));
        assertTrue(flow.cn1ApplyStyle("-fx-alignment", null));
        assertTrue(flow.cn1ApplyStyle("-fx-orientation", null));
        assertEquals(0, flow.getHgap(), 0);
        assertEquals(0, flow.getVgap(), 0);
        assertSame(Pos.TOP_LEFT, flow.getAlignment());
        assertSame(Orientation.HORIZONTAL, flow.getOrientation());
    }

    // --------------------------------------------------------- TilePane

    @Test
    public void tilePaneGivesEveryChildATileOfTheLargestSize() {
        Region a = fixed(30, 20);
        Region b = fixed(50, 40);
        Region c = fixed(10, 10);
        TilePane tiles = new TilePane(5, 5, a, b, c);
        assertEquals(50, tiles.getTileWidth(), 0);
        assertEquals(40, tiles.getTileHeight(), 0);
        assertEquals(50, tiles.tileWidthProperty().get(), 0);
        // Five columns by default: one row.
        assertEquals(270, tiles.prefWidth(-1), 0);
        assertEquals(40, tiles.prefHeight(-1), 0);
        tiles.setPrefColumns(2);
        assertEquals(105, tiles.prefWidth(-1), 0);
        assertEquals(85, tiles.prefHeight(-1), 0);
        assertEquals(85, tiles.prefHeight(120), 0);
        assertEquals(50, tiles.minWidth(-1), 0);
        lay(tiles, 120, 200);
        // Two tiles of 50 and a gap fit in 120; children sit centered.
        assertBounds("a", a, 10, 10, 30, 20);
        assertBounds("b", b, 55, 0, 50, 40);
        assertBounds("c", c, 20, 60, 10, 10);

        TilePane.setAlignment(a, Pos.TOP_LEFT);
        tiles.setTileAlignment(Pos.BOTTOM_RIGHT);
        lay(tiles, 120, 200);
        assertBounds("a", a, 0, 0, 30, 20);
        assertBounds("c", c, 40, 75, 10, 10);
        TilePane.clearConstraints(a);
        assertNull(TilePane.getAlignment(a));
    }

    @Test
    public void tilePaneStretchesResizableChildrenAndAlignsTheLastRow() {
        Region a = box(30, 20);
        Region b = fixed(50, 40);
        Region c = fixed(10, 10);
        TilePane tiles = new TilePane(5, 5, a, b, c);
        tiles.setAlignment(Pos.TOP_RIGHT);
        TilePane.setMargin(a, new Insets(1, 2, 3, 4));
        lay(tiles, 125, 200);
        // The grid of two columns is 105 wide: 20 spare on the left.
        assertBounds("a", a, 24, 1, 44, 36);
        assertBounds("b", b, 75, 0, 50, 40);
        // The single tile of the last row goes to the right on its own.
        assertBounds("c", c, 95, 60, 10, 10);
    }

    @Test
    public void tilePaneVerticalFillsColumnsAndHonoursPrefTileSize() {
        Region a = fixed(30, 20);
        Region b = fixed(50, 40);
        Region c = fixed(10, 10);
        TilePane tiles = new TilePane(Orientation.VERTICAL, 5, 5, a, b, c);
        tiles.setTileAlignment(Pos.TOP_LEFT);
        lay(tiles, 300, 90);
        assertBounds("a", a, 0, 0, 30, 20);
        assertBounds("b", b, 0, 45, 50, 40);
        assertBounds("c", c, 55, 0, 10, 10);
        tiles.setPrefTileWidth(60);
        tiles.setPrefTileHeight(42);
        tiles.setPrefRows(3);
        assertEquals(60, tiles.getTileWidth(), 0);
        // Three rows hold all three children in one column.
        assertEquals(60, tiles.prefWidth(-1), 0);
        assertEquals(136, tiles.prefHeight(-1), 0);
        lay(tiles, 300, 90);
        assertBounds("b", b, 0, 47, 50, 40);
        assertBounds("c", c, 65, 0, 10, 10);
    }

    @Test
    public void tilePaneTakesStyleValuesAndRestoresThem() {
        TilePane tiles = new TilePane(2, 3);
        assertTrue(tiles.cn1ApplyStyle("-fx-hgap", Integer.valueOf(4)));
        assertTrue(tiles.cn1ApplyStyle("-fx-vgap", Integer.valueOf(6)));
        assertTrue(tiles.cn1ApplyStyle("-fx-alignment", "bottom-left"));
        assertTrue(tiles.cn1ApplyStyle("-fx-orientation", "vertical"));
        assertEquals(4, tiles.getHgap(), 0);
        assertEquals(6, tiles.getVgap(), 0);
        assertSame(Pos.BOTTOM_LEFT, tiles.getAlignment());
        assertSame(Orientation.VERTICAL, tiles.getOrientation());
        assertFalse(tiles.cn1ApplyStyle("-fx-hgap", "x"));
        assertTrue(tiles.cn1ApplyStyle("-fx-hgap", null));
        assertTrue(tiles.cn1ApplyStyle("-fx-vgap", null));
        assertTrue(tiles.cn1ApplyStyle("-fx-alignment", null));
        assertTrue(tiles.cn1ApplyStyle("-fx-orientation", null));
        assertEquals(2, tiles.getHgap(), 0);
        assertEquals(3, tiles.getVgap(), 0);
        assertSame(Pos.TOP_LEFT, tiles.getAlignment());
        assertSame(Orientation.HORIZONTAL, tiles.getOrientation());
    }

    // --------------------------------------------------------- GridPane

    private static GridPane grid(Region a, Region b, Region c, Region d) {
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(10);
        grid.add(a, 0, 0);
        grid.add(b, 1, 0);
        grid.add(c, 0, 1);
        grid.add(d, 1, 1);
        return grid;
    }

    @Test
    public void gridPaneSizesRowsAndColumnsToTheirLargestChild() {
        Region a = box(30, 20);
        Region b = box(50, 40);
        Region c = box(40, 10);
        Region d = box(20, 20);
        GridPane grid = grid(a, b, c, d);
        assertEquals(2, grid.getColumnCount());
        assertEquals(2, grid.getRowCount());
        assertEquals(Integer.valueOf(1), GridPane.getColumnIndex(d));
        assertEquals(Integer.valueOf(1), GridPane.getRowIndex(d));
        assertEquals(95, grid.prefWidth(-1), 0);
        assertEquals(70, grid.prefHeight(-1), 0);
        lay(grid, 200, 200);
        // Columns of 40 and 50, rows of 40 and 20; nothing grows.
        assertBounds("a", a, 0, 0, 40, 40);
        assertBounds("b", b, 45, 0, 50, 40);
        assertBounds("c", c, 0, 50, 40, 20);
        assertBounds("d", d, 45, 50, 50, 20);
        Bounds cell = grid.getCellBounds(1, 1);
        assertEquals(45, cell.getMinX(), 0);
        assertEquals(50, cell.getMinY(), 0);
        assertEquals(50, cell.getWidth(), 0);
        assertEquals(20, cell.getHeight(), 0);

        grid.setAlignment(Pos.CENTER);
        lay(grid, 195, 170);
        assertBounds("a", a, 50, 50, 40, 40);
        assertBounds("d", d, 95, 100, 50, 20);
    }

    @Test
    public void gridPaneAlignsAndFillsInsideTheCell() {
        Region a = box(30, 20);
        Region b = box(50, 40);
        Region c = box(40, 10);
        Region d = box(20, 20);
        Region dot = fixed(10, 10);
        GridPane grid = grid(a, b, c, d);
        grid.add(dot, 0, 0);
        lay(grid, 200, 200);
        // Left and vertically centered unless told otherwise.
        assertBounds("dot", dot, 0, 15, 10, 10);
        GridPane.setHalignment(dot, HPos.RIGHT);
        GridPane.setValignment(dot, VPos.BOTTOM);
        GridPane.setFillWidth(a, Boolean.FALSE);
        GridPane.setFillHeight(a, Boolean.FALSE);
        assertSame(Boolean.FALSE, GridPane.isFillWidth(a));
        lay(grid, 200, 200);
        assertBounds("dot", dot, 30, 30, 10, 10);
        assertBounds("a", a, 0, 10, 30, 20);

        // The constraints of the column and the row apply to the rest.
        ColumnConstraints second = new ColumnConstraints();
        second.setHalignment(HPos.CENTER);
        second.setFillWidth(false);
        RowConstraints lower = new RowConstraints();
        lower.setValignment(VPos.TOP);
        lower.setFillHeight(false);
        grid.getColumnConstraints().addAll(new ColumnConstraints(), second);
        grid.getRowConstraints().addAll(new RowConstraints(), lower);
        lay(grid, 200, 200);
        assertBounds("d", d, 60, 50, 20, 20);
        assertBounds("c", c, 0, 50, 40, 10);
        assertBounds("b", b, 45, 0, 50, 40);
    }

    @Test
    public void gridPaneGrowsTheColumnsAndRowsThatAskForIt() {
        Region a = box(30, 20);
        Region b = box(50, 40);
        Region c = box(40, 10);
        Region d = box(20, 20);
        GridPane grid = grid(a, b, c, d);
        GridPane.setHgrow(b, Priority.ALWAYS);
        GridPane.setVgrow(c, Priority.ALWAYS);
        lay(grid, 200, 200);
        assertBounds("a", a, 0, 0, 40, 40);
        assertBounds("b", b, 45, 0, 155, 40);
        assertBounds("c", c, 0, 50, 40, 150);
        assertBounds("d", d, 45, 50, 155, 150);

        // A constraint decides for its column whatever the children ask.
        ColumnConstraints first = new ColumnConstraints();
        first.setHgrow(Priority.ALWAYS);
        ColumnConstraints second = new ColumnConstraints();
        second.setHgrow(Priority.NEVER);
        grid.getColumnConstraints().addAll(first, second);
        lay(grid, 200, 200);
        assertBounds("a", a, 0, 0, 145, 40);
        assertBounds("b", b, 150, 0, 50, 40);

        // Two growing columns share the 106 spare pixels.
        second.setHgrow(Priority.ALWAYS);
        assertTrue(grid.isNeedsLayout());
        lay(grid, 201, 200);
        assertBounds("a", a, 0, 0, 93, 40);
        assertBounds("b", b, 98, 0, 103, 40);
    }

    @Test
    public void gridPaneConstraintsFixTheSizeOfARowOrColumn() {
        Region a = box(30, 20);
        Region b = box(50, 40);
        Region c = box(40, 10);
        Region d = box(20, 20);
        GridPane grid = grid(a, b, c, d);
        grid.getColumnConstraints().addAll(new ColumnConstraints(100), new ColumnConstraints(60));
        grid.getRowConstraints().add(new RowConstraints(25));
        assertEquals(165, grid.prefWidth(-1), 0);
        assertEquals(55, grid.prefHeight(-1), 0);
        lay(grid, 400, 400);
        assertBounds("a", a, 0, 0, 100, 25);
        assertBounds("b", b, 105, 0, 60, 25);
        assertBounds("c", c, 0, 35, 100, 20);
        assertBounds("d", d, 105, 35, 60, 20);

        ColumnConstraints ranged = new ColumnConstraints(10, 70, 80);
        ranged.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().set(1, ranged);
        lay(grid, 400, 400);
        // It prefers 70 and may grow, but only to 80.
        assertBounds("b", b, 105, 0, 80, 25);
    }

    @Test
    public void gridPanePercentagesShareThePaneLessTheGaps() {
        Region a = box(30, 20);
        Region b = box(60, 20);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.add(a, 0, 0);
        grid.add(b, 1, 0);
        ColumnConstraints quarter = new ColumnConstraints();
        quarter.setPercentWidth(25);
        ColumnConstraints rest = new ColumnConstraints();
        rest.setPercentWidth(75);
        grid.getColumnConstraints().addAll(quarter, rest);
        RowConstraints half = new RowConstraints();
        half.setPercentHeight(50);
        grid.getRowConstraints().add(half);
        lay(grid, 210, 100);
        assertBounds("a", a, 0, 0, 50, 50);
        assertBounds("b", b, 60, 0, 150, 50);
        // A quarter must hold 30: 120 of columns, plus the gap.
        assertEquals(130, grid.prefWidth(-1), 0);
        // Half of the height must hold 20.
        assertEquals(40, grid.prefHeight(-1), 0);
    }

    @Test
    public void gridPaneSpansCoverSeveralColumns() {
        Region title = box(20, 10);
        Region a = box(30, 20);
        Region b = box(30, 20);
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.add(title, 0, 0, 2, 1);
        grid.add(a, 0, 1);
        grid.add(b, 1, 1);
        assertEquals(Integer.valueOf(2), GridPane.getColumnSpan(title));
        assertEquals(65, grid.prefWidth(-1), 0);
        lay(grid, 300, 300);
        assertBounds("title", title, 0, 0, 65, 10);
        assertBounds("b", b, 35, 10, 30, 20);

        // A span that needs more than its columns have widens them.
        title.setPrefWidth(105);
        GridPane.setColumnSpan(title, Integer.valueOf(GridPane.REMAINING));
        assertEquals(2, grid.getColumnCount());
        assertEquals(105, grid.prefWidth(-1), 0);
        lay(grid, 300, 300);
        assertBounds("title", title, 0, 0, 105, 10);
        assertBounds("a", a, 0, 10, 50, 20);
        assertBounds("b", b, 55, 10, 50, 20);
    }

    @Test
    public void gridPaneMarginSurroundsAChildInItsCell() {
        Region a = box(30, 20);
        GridPane grid = new GridPane();
        GridPane.setConstraints(a, 0, 0, 1, 1, HPos.LEFT, VPos.TOP, Priority.NEVER, Priority.NEVER,
                new Insets(2, 3, 4, 5));
        grid.getChildren().add(a);
        assertEquals(38, grid.prefWidth(-1), 0);
        assertEquals(26, grid.prefHeight(-1), 0);
        lay(grid, 100, 100);
        assertBounds("a", a, 5, 2, 30, 20);
        GridPane.clearConstraints(a);
        assertNull(GridPane.getMargin(a));
        assertNull(GridPane.getHgrow(a));
        assertNull(GridPane.getHalignment(a));
        assertNull(GridPane.getRowIndex(a));
    }

    @Test
    public void gridPaneShrinksColumnsDownToTheirMinimum() {
        Region a = box(100, 20);
        Region b = box(100, 20);
        b.setMinWidth(80);
        GridPane grid = new GridPane();
        grid.addRow(0, a, b);
        assertEquals(80, grid.minWidth(-1), 0);
        lay(grid, 150, 20);
        assertBounds("a", a, 0, 0, 70, 20);
        assertBounds("b", b, 70, 0, 80, 20);
    }

    @Test
    public void gridPaneAddRowAndAddColumnContinueAfterWhatIsThere() {
        Region a = box(1, 1);
        Region b = box(1, 1);
        Region c = box(1, 1);
        Region d = box(1, 1);
        GridPane grid = new GridPane();
        grid.addRow(0, a, b);
        grid.addRow(0, c);
        grid.addColumn(1, d);
        assertEquals(Integer.valueOf(0), GridPane.getColumnIndex(a));
        assertEquals(Integer.valueOf(1), GridPane.getColumnIndex(b));
        assertEquals(Integer.valueOf(2), GridPane.getColumnIndex(c));
        assertEquals(Integer.valueOf(0), GridPane.getRowIndex(c));
        assertEquals(Integer.valueOf(1), GridPane.getColumnIndex(d));
        assertEquals(Integer.valueOf(1), GridPane.getRowIndex(d));
        assertEquals(3, grid.getColumnCount());
        assertEquals(2, grid.getRowCount());
    }

    @Test
    public void gridPaneConstraintChangesAskForLayout() {
        GridPane grid = new GridPane();
        ColumnConstraints column = new ColumnConstraints();
        grid.getColumnConstraints().add(column);
        grid.add(box(10, 10), 0, 0);
        lay(grid, 100, 100);
        assertFalse(grid.isNeedsLayout());
        column.setPrefWidth(40);
        assertTrue(grid.isNeedsLayout());
        grid.layout();
        assertEquals(40, grid.prefWidth(-1), 0);
        grid.getColumnConstraints().clear();
        grid.layout();
        column.setPrefWidth(50);
        assertFalse(grid.isNeedsLayout());
        grid.setHgap(3);
        assertTrue(grid.isNeedsLayout());
    }

    @Test(expected = IllegalArgumentException.class)
    public void gridPaneRejectsANegativeIndex() {
        GridPane.setRowIndex(box(1, 1), Integer.valueOf(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void gridPaneRejectsAnEmptySpan() {
        GridPane.setColumnSpan(box(1, 1), Integer.valueOf(0));
    }

    @Test
    public void gridPaneTakesStyleValuesAndRestoresThem() {
        GridPane grid = new GridPane();
        assertTrue(grid.cn1ApplyStyle("-fx-hgap", Integer.valueOf(4)));
        assertTrue(grid.cn1ApplyStyle("-fx-vgap", Double.valueOf(6)));
        assertTrue(grid.cn1ApplyStyle("-fx-alignment", "BASELINE-CENTER"));
        assertEquals(4, grid.getHgap(), 0);
        assertEquals(6, grid.getVgap(), 0);
        assertSame(Pos.BASELINE_CENTER, grid.getAlignment());
        assertFalse(grid.cn1ApplyStyle("-fx-orientation", "vertical"));
        assertTrue(grid.cn1ApplyStyle("-fx-hgap", null));
        assertTrue(grid.cn1ApplyStyle("-fx-vgap", null));
        assertTrue(grid.cn1ApplyStyle("-fx-alignment", null));
        assertEquals(0, grid.getHgap(), 0);
        assertEquals(0, grid.getVgap(), 0);
        assertSame(Pos.TOP_LEFT, grid.getAlignment());
    }
}
