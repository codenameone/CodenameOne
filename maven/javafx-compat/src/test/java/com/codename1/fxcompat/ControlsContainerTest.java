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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Units;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/// The controls made of other nodes: tool bars, scroll bars, titled
/// panes and accordions, spinners, paginations, colour pickers and trees.
public class ControlsContainerTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private boolean implicitExit;

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
        implicitExit = Platform.isImplicitExit();
        Platform.setImplicitExit(false);
    }

    @After
    public void tearDown() {
        List<javafx.stage.Window> open = new ArrayList<javafx.stage.Window>(javafx.stage.Window.getWindows());
        for (int i = 0; i < open.size(); i++) {
            open.get(i).hide();
        }
        Units.setScale(0);
        Platform.setImplicitExit(implicitExit);
    }

    private static Scene sceneOf(Node control, double w, double h) {
        Pane root = new Pane();
        root.getChildren().add(control);
        if (control instanceof Region) {
            ((Region) control).setPrefSize(w, h);
        }
        Scene scene = new Scene(root, w, h);
        scene.cn1Layout(w, h);
        return scene;
    }

    private static void press(Scene scene, Node node, double x, double y) {
        Point2D p = node.localToScene(x, y);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, p.getX(), p.getY(), MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, p.getX(), p.getY(), MouseButton.PRIMARY);
    }

    private static double sceneX(Node n) {
        return n.localToScene(0, 0).getX();
    }

    private static double sceneY(Node n) {
        return n.localToScene(0, 0).getY();
    }

    @Test
    public void aToolBarPutsItsItemsInARowAndUprightInAColumn() {
        Button a = new Button("One");
        Button b = new Button("Two");
        ToolBar bar = new ToolBar(a, b);
        Scene scene = sceneOf(bar, 300, 200);
        assertTrue("the second item is right of the first", sceneX(b) >= sceneX(a) + a.getWidth());
        assertEquals("on one line", sceneY(a), sceneY(b), 0.5);
        bar.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        assertTrue("a bar is as tall as its items, not as the room", bar.prefHeight(-1) < 80);

        bar.setOrientation(Orientation.VERTICAL);
        scene.cn1Layout(300, 200);
        assertTrue("the second item is under the first", sceneY(b) >= sceneY(a) + a.getHeight());
        assertSame(a, bar.getItems().get(0));
    }

    @Test
    public void aScrollBarKeepsItsValueInRangeAndSizesItsThumbByWhatIsVisible() {
        ScrollBar bar = new ScrollBar();
        bar.setMin(0);
        bar.setMax(200);
        bar.setVisibleAmount(50);
        bar.setUnitIncrement(30);
        Scene scene = sceneOf(bar, 200, 16);
        Node thumb = bar.lookup(".thumb");
        assertNotNull(thumb);
        assertEquals("a quarter is visible", 50, thumb.getLayoutBounds().getWidth(), 1);
        assertEquals(0, thumb.getLayoutX(), 0.5);

        bar.increment();
        assertEquals(30, bar.getValue(), 0);
        for (int i = 0; i < 10; i++) {
            bar.increment();
        }
        assertEquals("the value stops at the maximum", 200, bar.getValue(), 0);
        scene.cn1Layout(200, 16);
        assertEquals("the thumb ends where the track ends", 150, thumb.getLayoutX(), 1);
        bar.decrement();
        assertEquals(170, bar.getValue(), 0);

        bar.setValue(0);
        bar.setBlockIncrement(40);
        bar.adjustValue(1);
        assertEquals("a press on the track moves one block", 40, bar.getValue(), 0);
        bar.adjustValue(0.25);
        assertEquals("and not past the place pressed", 50, bar.getValue(), 0);
    }

    @Test
    public void aTitledPaneClosesToItsTitle() {
        Label inside = new Label("content");
        inside.setPrefSize(100, 120);
        TitledPane pane = new TitledPane("Title", inside);
        Scene scene = sceneOf(pane, 200, 200);
        pane.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        double open = pane.prefHeight(-1);
        assertTrue("open, the pane holds its content", open >= 120);
        assertTrue(pane.isExpanded());

        Node title = pane.lookup(".title");
        assertNotNull(title);
        press(scene, title, 30, 4);
        assertFalse("a click on the title closes the pane", pane.isExpanded());
        assertTrue("closed, the pane is as tall as its title", pane.prefHeight(-1) < open - 100);

        pane.setCollapsible(false);
        press(scene, title, 30, 4);
        assertFalse("a pane that does not collapse stays as it is", pane.isExpanded());
    }

    @Test
    public void anAccordionKeepsOnePaneOpen() {
        TitledPane a = new TitledPane("A", new Label("a"));
        TitledPane b = new TitledPane("B", new Label("b"));
        Accordion accordion = new Accordion(a, b);
        sceneOf(accordion, 200, 300);
        assertFalse(a.isExpanded());
        assertFalse(b.isExpanded());
        assertNull(accordion.getExpandedPane());

        accordion.setExpandedPane(a);
        assertTrue(a.isExpanded());
        b.setExpanded(true);
        assertFalse("opening one pane closes the other", a.isExpanded());
        assertSame(b, accordion.getExpandedPane());
        b.setExpanded(false);
        assertNull(accordion.getExpandedPane());
    }

    @Test
    public void aSpinnerStepsThroughItsFactoryAndShowsTheValue() {
        Spinner<Integer> spinner = new Spinner<Integer>(0, 10, 9, 2);
        sceneOf(spinner, 120, 30);
        assertEquals(Integer.valueOf(9), spinner.getValue());
        assertEquals("9", spinner.getEditor().getText());
        spinner.increment();
        assertEquals("a step past the maximum stops there", Integer.valueOf(10), spinner.getValue());
        assertEquals("10", spinner.getEditor().getText());
        spinner.getValueFactory().setWrapAround(true);
        spinner.increment();
        assertEquals("and wraps when the factory says so", Integer.valueOf(1), spinner.getValue());
        spinner.decrement(1);
        assertEquals(Integer.valueOf(10), spinner.getValue());

        Spinner<String> words = new Spinner<String>(FXCollections.observableArrayList("a", "b", "c"));
        assertEquals("a", words.getValue());
        words.increment(2);
        assertEquals("c", words.getValue());
        words.increment();
        assertEquals("c", words.getValue());

        SpinnerValueFactory.DoubleSpinnerValueFactory halves =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.5, 0.25);
        Spinner<Double> decimal = new Spinner<Double>(halves);
        decimal.decrement(3);
        assertEquals(0, decimal.getValue().doubleValue(), 0);
    }

    @Test
    public void anEditableSpinnerReadsWhatWasTyped() {
        Spinner<Integer> spinner = new Spinner<Integer>(0, 100, 5);
        spinner.setEditable(true);
        sceneOf(spinner, 120, 30);
        spinner.getEditor().setText("42");
        spinner.increment();
        assertEquals("the typed value is read before the step", Integer.valueOf(43), spinner.getValue());
        spinner.getEditor().setText("nothing");
        spinner.increment();
        assertEquals("a text that is no number changes nothing", Integer.valueOf(44), spinner.getValue());
    }

    @Test
    public void aPaginationAsksItsFactoryForThePageItShows() {
        final List<Integer> asked = new ArrayList<Integer>();
        Pagination pages = new Pagination(5, 0);
        pages.setPageFactory(index -> {
            asked.add(index);
            return index.intValue() == 3 ? null : new Label("page " + index);
        });
        sceneOf(pages, 300, 200);
        assertEquals("[0]", asked.toString());
        pages.setCurrentPageIndex(2);
        assertEquals("[0, 2]", asked.toString());
        pages.setCurrentPageIndex(99);
        assertEquals("an index past the last page is the last page", 4, pages.getCurrentPageIndex());
        pages.setCurrentPageIndex(3);
        assertEquals("a page the factory has nothing for is not gone to", 4, pages.getCurrentPageIndex());
        pages.setCurrentPageIndex(-4);
        assertEquals(0, pages.getCurrentPageIndex());
    }

    @Test
    public void aColorPickerShowsItsValueAndTellsWhenItChanges() {
        ColorPicker picker = new ColorPicker(Color.RED);
        final int[] actions = new int[1];
        picker.setOnAction(e -> actions[0]++);
        sceneOf(picker, 140, 30);
        assertEquals(Color.RED, picker.getValue());
        picker.setValue(Color.BLUE);
        assertEquals(1, actions[0]);
        assertTrue("the button is wide enough for a swatch and a name", picker.prefWidth(-1) > 40);
    }

    private static <T> TreeView<T> treeOf(TreeItem<T> root, final List<TreeCell<T>> cells) {
        TreeView<T> tree = new TreeView<T>(root);
        tree.setFixedCellSize(20);
        tree.setCellFactory(view -> {
            TreeCell<T> cell = new TreeCell<T>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                }
            };
            cells.add(cell);
            return cell;
        });
        return tree;
    }

    private static <T> TreeCell<T> cellAt(List<TreeCell<T>> cells, int row) {
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).getIndex() == row && !cells.get(i).isEmpty()) {
                return cells.get(i);
            }
        }
        return null;
    }

    private static TreeItem<String> family() {
        TreeItem<String> root = new TreeItem<String>("root");
        TreeItem<String> a = new TreeItem<String>("a");
        TreeItem<String> b = new TreeItem<String>("b");
        a.getChildren().add(new TreeItem<String>("a1"));
        a.getChildren().add(new TreeItem<String>("a2"));
        root.getChildren().add(a);
        root.getChildren().add(b);
        return root;
    }

    @Test
    public void theRowsOfATreeAreTheItemsUnderOpenItems() {
        TreeItem<String> root = family();
        TreeItem<String> a = root.getChildren().get(0);
        List<TreeCell<String>> cells = new ArrayList<TreeCell<String>>();
        TreeView<String> tree = treeOf(root, cells);
        Scene scene = sceneOf(tree, 200, 200);
        assertEquals("a closed root is the only row", 1, tree.getExpandedItemCount());
        assertTrue(root.isLeaf() == false && a.getParent() == root);

        root.setExpanded(true);
        assertEquals(3, tree.getExpandedItemCount());
        a.setExpanded(true);
        assertEquals(5, tree.getExpandedItemCount());
        assertSame(a.getChildren().get(1), tree.getTreeItem(3));
        assertEquals(4, tree.getRow(root.getChildren().get(1)));
        assertEquals(2, tree.getTreeItemLevel(a.getChildren().get(0)));

        scene.cn1Layout(200, 200);
        assertEquals("a1", cellAt(cells, 2).getText());
        assertSame(a, cellAt(cells, 1).getTreeItem());

        tree.setShowRoot(false);
        assertEquals("without its root a tree starts at the children", 4, tree.getExpandedItemCount());
        assertSame(a, tree.getTreeItem(0));

        a.getChildren().remove(0);
        assertEquals(3, tree.getExpandedItemCount());
        a.getChildren().clear();
        assertTrue(a.isLeaf());
        assertEquals(2, tree.getExpandedItemCount());
    }

    @Test
    public void aPressOnTheArrowOpensAnItemAndElsewhereSelectsIt() {
        TreeItem<String> root = family();
        root.setExpanded(true);
        TreeItem<String> a = root.getChildren().get(0);
        List<TreeCell<String>> cells = new ArrayList<TreeCell<String>>();
        TreeView<String> tree = treeOf(root, cells);
        Scene scene = sceneOf(tree, 200, 200);
        TreeCell<String> cell = cellAt(cells, 1);
        assertNotNull(cell);
        // One level down: the arrow is in the second indent.
        press(scene, cell, 18 + 9, 10);
        assertTrue("the arrow opens the item", a.isExpanded());
        assertTrue("and selects nothing", tree.getSelectionModel().isEmpty());

        scene.cn1Layout(200, 200);
        press(scene, cellAt(cells, 2), 100, 10);
        assertSame(a.getChildren().get(0), tree.getSelectionModel().getSelectedItem());
        assertEquals(2, tree.getSelectionModel().getSelectedIndex());
        assertTrue(cellAt(cells, 2).isSelected());

        tree.getSelectionModel().select(root.getChildren().get(1));
        assertEquals(4, tree.getSelectionModel().getSelectedIndex());
        a.setExpanded(false);
        assertEquals("the selected item keeps its selection at its new row", 2,
                tree.getSelectionModel().getSelectedIndex());
        assertSame(root.getChildren().get(1), tree.getSelectionModel().getSelectedItem());
    }

    @Test
    public void whatHappensToAnItemIsToldToTheItemsAboveIt() {
        TreeItem<String> root = family();
        TreeItem<String> a = root.getChildren().get(0);
        final List<String> told = new ArrayList<String>();
        root.addEventHandler(TreeItem.<String>branchExpandedEvent(), e -> told.add("open " + e.getTreeItem()
                .getValue()));
        root.addEventHandler(TreeItem.<String>treeNotificationEvent(), e -> told.add("any " + e.getTreeItem()
                .getValue()));
        a.setExpanded(true);
        assertEquals("[open a, any a]", told.toString());
        told.clear();
        a.getChildren().get(0).setValue("other");
        assertEquals("[any other]", told.toString());
        told.clear();
        a.getChildren().get(0).setExpanded(true);
        assertEquals("a leaf has nothing to open", "[]", told.toString());
        assertSame(a.getChildren().get(1), a.getChildren().get(0).nextSibling());
        assertNull(a.getChildren().get(0).previousSibling());
    }

    @Test
    public void aDialogPaneShowsItsExpandableContentWhenAsked() {
        javafx.scene.control.DialogPane pane = new javafx.scene.control.DialogPane();
        pane.setContentText("message");
        Label details = new Label("details");
        details.setPrefSize(100, 60);
        double plain = pane.prefHeight(-1);
        pane.setExpandableContent(details);
        double withLink = pane.prefHeight(-1);
        assertTrue("the link to the details takes a line", withLink > plain);
        assertNull("closed, the details are not in the pane", details.getParent());
        pane.setExpanded(true);
        assertNotNull(details.getParent());
        assertTrue(pane.prefHeight(-1) >= withLink + 60);
        pane.setExpanded(false);
        assertNull(details.getParent());
    }

    @Test
    public void theDefaultTickFormatterWritesANumberBetweenItsPrefixAndSuffix() {
        javafx.scene.chart.NumberAxis axis = new javafx.scene.chart.NumberAxis();
        javafx.scene.chart.NumberAxis.DefaultFormatter money =
                new javafx.scene.chart.NumberAxis.DefaultFormatter(axis, "$", "k");
        assertEquals("$12k", money.toString(Integer.valueOf(12)));
        assertEquals("$1.5k", money.toString(Double.valueOf(1.5)));
        assertEquals(12.0, money.fromString("$12k").doubleValue(), 0);
        assertNull(money.fromString("$k"));
        assertEquals("7", new javafx.scene.chart.NumberAxis.DefaultFormatter(axis).toString(Double.valueOf(7)));
    }

    @Test
    public void aFillAndAStrokeTransitionBlendTheColoursOfAShape() {
        com.codename1.fxcompat.runtime.FrameClock.reset();
        com.codename1.fxcompat.runtime.FrameClock.setManual(true);
        try {
            javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(10, 10, Color.BLACK);
            r.setStroke(Color.WHITE);
            javafx.animation.FillTransition fill = new javafx.animation.FillTransition(
                    javafx.util.Duration.millis(1000), r, null, Color.WHITE);
            fill.setInterpolator(javafx.animation.Interpolator.LINEAR);
            javafx.animation.StrokeTransition stroke = new javafx.animation.StrokeTransition(
                    javafx.util.Duration.millis(1000), r, Color.RED, Color.BLUE);
            stroke.setInterpolator(javafx.animation.Interpolator.LINEAR);
            fill.play();
            stroke.play();
            com.codename1.fxcompat.runtime.FrameClock.advance(500);
            Color half = (Color) r.getFill();
            assertEquals("half way from the fill the shape had", 0.5, half.getRed(), 0.02);
            Color line = (Color) r.getStroke();
            assertEquals(0.5, line.getRed(), 0.02);
            assertEquals(0.5, line.getBlue(), 0.02);
            com.codename1.fxcompat.runtime.FrameClock.advance(600);
            assertEquals(Color.WHITE, r.getFill());
            assertEquals(Color.BLUE, r.getStroke());
        } finally {
            com.codename1.fxcompat.runtime.FrameClock.reset();
        }
    }
}
