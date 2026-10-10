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
import com.codename1.ui.Component;
import com.codename1.ui.Display;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.IndexRange;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/// The basic controls: text input, toggles, ranges, and the scroll, split
/// and tab panes.
public class ControlsBasicTest {

    private static final double EPS = 0.6;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
    }

    private static boolean has(javafx.scene.Node node, String pseudoClass) {
        return node.getPseudoClassStates().contains(PseudoClass.getPseudoClass(pseudoClass));
    }

    private static void press(Control control) {
        Component c = control.cn1Native();
        assertTrue(c instanceof com.codename1.ui.Button);
        com.codename1.ui.Button b = (com.codename1.ui.Button) c;
        b.pressed();
        b.released();
    }

    private static Region box(double w, double h) {
        Region r = new Region();
        r.setMinSize(10, 10);
        r.setPrefSize(w, h);
        r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return r;
    }

    private static void assertPrefSize(Control c) {
        double w = c.prefWidth(-1);
        double h = c.prefHeight(-1);
        assertTrue(c.getClass().getName() + " pref width " + w, w > 0 && w < 2000);
        assertTrue(c.getClass().getName() + " pref height " + h, h > 0 && h < 2000);
    }

    // ---------------------------------------------------------------- text

    @Test
    public void textFieldSyncsBothWays() {
        TextField field = new TextField("one");
        Component c = field.cn1Native();
        assertTrue(c instanceof com.codename1.ui.TextField);
        com.codename1.ui.TextField nativeField = (com.codename1.ui.TextField) c;
        assertEquals("one", nativeField.getText());

        field.setText("two");
        assertEquals("two", nativeField.getText());

        final List<String> seen = new ArrayList<String>();
        field.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> o, String oldValue, String newValue) {
                seen.add(oldValue + ">" + newValue);
            }
        });
        nativeField.setText("typed");
        assertEquals("typed", field.getText());
        assertEquals("[two>typed]", seen.toString());
        assertEquals(5, field.getLength());

        field.setPromptText("hint");
        assertEquals("hint", nativeField.getHint());
        field.setEditable(false);
        assertFalse(nativeField.isEditable());
        assertTrue(has(field, "readonly"));
        field.setEditable(true);
        assertTrue(nativeField.isEditable());
    }

    @Test
    public void textEditingApi() {
        TextField field = new TextField();
        field.appendText("hello");
        field.appendText(" world");
        assertEquals("hello world", field.getText());
        assertEquals(11, field.getLength());
        assertEquals("hello", field.getText(0, 5));
        field.insertText(5, ",");
        assertEquals("hello, world", field.getText());
        field.deleteText(5, 6);
        assertEquals("hello world", field.getText());
        field.replaceText(0, 5, "HOWDY");
        assertEquals("HOWDY world", field.getText());

        field.selectRange(0, 5);
        assertEquals("HOWDY", field.getSelectedText());
        assertEquals(new IndexRange(0, 5), field.getSelection());
        assertEquals(0, field.getAnchor());
        assertEquals(5, field.getCaretPosition());
        field.replaceSelection("hi");
        assertEquals("hi world", field.getText());
        field.selectAll();
        assertEquals(8, field.getSelection().getLength());
        field.deselect();
        assertEquals(0, field.getSelection().getLength());
        field.positionCaret(2);
        assertEquals(2, field.getCaretPosition());
        field.end();
        assertEquals(8, field.getCaretPosition());
        assertTrue(field.deletePreviousChar());
        assertEquals("hi worl", field.getText());
        field.home();
        assertTrue(field.deleteNextChar());
        assertEquals("i worl", field.getText());

        field.clear();
        assertEquals("", field.getText());
        assertEquals(0, field.getLength());
        assertEquals("", ((com.codename1.ui.TextField) field.cn1Native()).getText());

        IndexRange range = IndexRange.valueOf("2, 7");
        assertEquals(2, range.getStart());
        assertEquals(7, range.getEnd());
        assertEquals(5, range.getLength());
        assertEquals(new IndexRange(3, 9), IndexRange.normalize(9, 3));
    }

    @Test
    public void textFieldActionAndColumns() {
        TextField field = new TextField();
        final int[] fired = new int[1];
        field.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                fired[0]++;
            }
        });
        Component c = field.cn1Native();
        assertTrue(c instanceof com.codename1.ui.TextField);
        final com.codename1.ui.TextField nativeField = (com.codename1.ui.TextField) c;
        if (Display.getInstance().isEdt()) {
            nativeField.fireDoneEvent();
        } else {
            // The native field hands the event over to the event thread.
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    nativeField.fireDoneEvent();
                }
            });
        }
        assertEquals(1, fired[0]);

        assertEquals(TextField.DEFAULT_PREF_COLUMN_COUNT, field.getPrefColumnCount());
        double narrow = field.prefWidth(-1);
        field.setPrefColumnCount(40);
        assertTrue(field.prefWidth(-1) > narrow);
        assertPrefSize(field);
    }

    @Test
    public void passwordFieldAndTextArea() {
        PasswordField password = new PasswordField();
        Component p = password.cn1Native();
        assertTrue(p instanceof com.codename1.ui.TextField);
        assertTrue((((com.codename1.ui.TextField) p).getConstraint() & com.codename1.ui.TextArea.PASSWORD) != 0);
        password.setText("secret");
        assertEquals("secret", ((com.codename1.ui.TextField) p).getText());
        assertPrefSize(password);

        TextArea area = new TextArea("a\nb");
        Component a = area.cn1Native();
        assertTrue(a instanceof com.codename1.ui.TextArea);
        com.codename1.ui.TextArea nativeArea = (com.codename1.ui.TextArea) a;
        assertEquals("a\nb", nativeArea.getText());
        assertFalse(nativeArea.isSingleLineTextArea());
        nativeArea.setText("changed\nthere");
        assertEquals("changed\nthere", area.getText());
        area.appendText("!");
        assertEquals("changed\nthere!", nativeArea.getText());
        area.setPrefRowCount(4);
        area.setPrefColumnCount(20);
        assertEquals(4, nativeArea.getRows());
        assertEquals(20, nativeArea.getColumns());
        area.setWrapText(true);
        assertTrue(area.isWrapText());
        assertPrefSize(area);
    }

    // ------------------------------------------------------------- toggles

    @Test
    public void checkBoxSyncAndTriStateCycle() {
        CheckBox box = new CheckBox("Agree");
        Component c = box.cn1Native();
        assertTrue(c instanceof com.codename1.ui.CheckBox);
        com.codename1.ui.CheckBox nativeBox = (com.codename1.ui.CheckBox) c;
        assertEquals("Agree", nativeBox.getText());
        assertFalse(nativeBox.isSelected());

        box.setSelected(true);
        assertTrue(nativeBox.isSelected());
        assertTrue(has(box, "selected"));

        final List<Boolean> seen = new ArrayList<Boolean>();
        final int[] actions = new int[1];
        box.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> o, Boolean oldValue, Boolean newValue) {
                seen.add(newValue);
            }
        });
        box.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                actions[0]++;
            }
        });
        press(box);
        assertFalse(box.isSelected());
        assertFalse(nativeBox.isSelected());
        assertEquals("[false]", seen.toString());
        assertEquals(1, actions[0]);
        assertFalse(has(box, "selected"));

        // unchecked -> indeterminate -> checked -> unchecked
        box.setAllowIndeterminate(true);
        box.fire();
        assertTrue(box.isIndeterminate());
        assertFalse(box.isSelected());
        assertTrue(has(box, "indeterminate"));
        assertFalse(nativeBox.isSelected());
        box.fire();
        assertFalse(box.isIndeterminate());
        assertTrue(box.isSelected());
        assertTrue(nativeBox.isSelected());
        assertFalse(has(box, "indeterminate"));
        box.fire();
        assertFalse(box.isIndeterminate());
        assertFalse(box.isSelected());
        press(box);
        assertTrue(box.isIndeterminate());
        assertPrefSize(box);
    }

    @Test
    public void toggleGroupIsExclusive() {
        ToggleGroup group = new ToggleGroup();
        RadioButton a = new RadioButton("a");
        RadioButton b = new RadioButton("b");
        ToggleButton c = new ToggleButton("c");
        a.setToggleGroup(group);
        b.setToggleGroup(group);
        c.setToggleGroup(group);
        assertEquals(3, group.getToggles().size());
        assertNull(group.getSelectedToggle());

        final List<Toggle> seen = new ArrayList<Toggle>();
        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> o, Toggle oldValue, Toggle newValue) {
                seen.add(newValue);
            }
        });
        a.setSelected(true);
        assertSame(a, group.getSelectedToggle());
        b.setSelected(true);
        assertSame(b, group.getSelectedToggle());
        assertFalse(a.isSelected());
        assertTrue(has(b, "selected"));
        assertFalse(has(a, "selected"));
        Component na = a.cn1Native();
        Component nb = b.cn1Native();
        assertTrue(na instanceof com.codename1.ui.RadioButton);
        assertFalse(((com.codename1.ui.RadioButton) na).isSelected());
        assertTrue(((com.codename1.ui.RadioButton) nb).isSelected());

        // Clicking the selected radio button of a group leaves it selected.
        press(b);
        assertTrue(b.isSelected());
        assertSame(b, group.getSelectedToggle());
        // Clicking another one moves the selection.
        press(a);
        assertTrue(a.isSelected());
        assertFalse(b.isSelected());
        assertFalse(((com.codename1.ui.RadioButton) nb).isSelected());
        assertSame(a, group.getSelectedToggle());

        // A toggle button of a group can be deselected by clicking it.
        press(c);
        assertTrue(c.isSelected());
        assertFalse(a.isSelected());
        press(c);
        assertFalse(c.isSelected());
        assertNull(group.getSelectedToggle());

        group.selectToggle(b);
        assertTrue(b.isSelected());
        b.setToggleGroup(null);
        assertEquals(2, group.getToggles().size());
        assertNull(group.getSelectedToggle());
        assertFalse(seen.isEmpty());
        assertPrefSize(a);
        assertPrefSize(c);
    }

    @Test
    public void hyperlinkVisited() {
        Hyperlink link = new Hyperlink("site");
        final int[] actions = new int[1];
        link.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                actions[0]++;
            }
        });
        assertFalse(link.isVisited());
        press(link);
        assertEquals(1, actions[0]);
        assertTrue(link.isVisited());
        assertTrue(has(link, "visited"));
        link.setVisited(false);
        assertFalse(has(link, "visited"));
        assertPrefSize(link);
    }

    @Test
    public void separatorOrientation() {
        Separator separator = new Separator();
        assertEquals(Orientation.HORIZONTAL, separator.getOrientation());
        assertTrue(has(separator, "horizontal"));
        double thin = separator.prefHeight(-1);
        assertTrue(thin > 0 && thin < 10);
        assertEquals(Double.MAX_VALUE, separator.maxWidth(-1), 0);
        separator.setOrientation(Orientation.VERTICAL);
        assertTrue(has(separator, "vertical"));
        assertFalse(has(separator, "horizontal"));
        assertTrue(separator.prefWidth(-1) > 0 && separator.prefWidth(-1) < 10);
        assertEquals(Double.MAX_VALUE, separator.maxHeight(-1), 0);
    }

    // -------------------------------------------------------------- ranges

    @Test
    public void sliderClampsAndAdjusts() {
        Slider slider = new Slider(0, 100, 50);
        Component c = slider.cn1Native();
        assertTrue(c instanceof com.codename1.ui.Slider);
        com.codename1.ui.Slider nativeSlider = (com.codename1.ui.Slider) c;
        int span = nativeSlider.getMaxValue() - nativeSlider.getMinValue();
        assertTrue(span > 0);
        assertEquals(span / 2, nativeSlider.getProgress() - nativeSlider.getMinValue());

        slider.setValue(150);
        assertEquals(100, slider.getValue(), 0);
        slider.setValue(-20);
        assertEquals(0, slider.getValue(), 0);
        assertEquals(nativeSlider.getMinValue(), nativeSlider.getProgress());

        final List<Double> seen = new ArrayList<Double>();
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> o, Number oldValue, Number newValue) {
                seen.add(Double.valueOf(newValue.doubleValue()));
            }
        });
        nativeSlider.setProgress(nativeSlider.getMinValue() + span / 4);
        assertEquals(25, slider.getValue(), 0.01);
        assertEquals(1, seen.size());

        slider.setValue(50);
        slider.increment();
        assertEquals(60, slider.getValue(), 0);
        slider.decrement();
        slider.decrement();
        assertEquals(40, slider.getValue(), 0);
        slider.adjustValue(77);
        assertEquals(77, slider.getValue(), 0);
        slider.adjustValue(500);
        assertEquals(100, slider.getValue(), 0);

        slider.setMax(60);
        assertEquals(60, slider.getValue(), 0);
        slider.setMin(70);
        assertTrue(slider.getMax() >= slider.getMin());
        assertTrue(slider.getValue() >= slider.getMin() && slider.getValue() <= slider.getMax());

        Slider snapping = new Slider(0, 100, 0);
        snapping.setMajorTickUnit(20);
        snapping.setMinorTickCount(0);
        snapping.setSnapToTicks(true);
        snapping.adjustValue(33);
        assertEquals(40, snapping.getValue(), 0);
        assertPrefSize(slider);
        snapping.setOrientation(Orientation.VERTICAL);
        assertPrefSize(snapping);
    }

    @Test
    public void progressIndeterminate() {
        ProgressBar bar = new ProgressBar();
        assertTrue(bar.isIndeterminate());
        assertEquals(ProgressIndicator.INDETERMINATE_PROGRESS, bar.getProgress(), 0);
        assertTrue(has(bar, "indeterminate"));
        // The bar draws itself; ControlLookTest covers what it draws.
        assertNull(bar.cn1Native());

        final List<Boolean> seen = new ArrayList<Boolean>();
        bar.indeterminateProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> o, Boolean oldValue, Boolean newValue) {
                seen.add(newValue);
            }
        });
        bar.setProgress(0.5);
        assertFalse(bar.isIndeterminate());
        assertEquals("[false]", seen.toString());
        assertFalse(has(bar, "indeterminate"));
        assertTrue(has(bar, "determinate"));
        bar.setProgress(-1);
        assertTrue(bar.isIndeterminate());
        assertEquals(100, bar.prefWidth(-1), 0.01);
        assertPrefSize(bar);

        ProgressIndicator indicator = new ProgressIndicator();
        assertTrue(indicator.isIndeterminate());
        indicator.setProgress(1);
        assertFalse(indicator.isIndeterminate());
        assertPrefSize(indicator);
        assertPrefSize(new ProgressIndicator(0.25));
    }

    // --------------------------------------------------------------- panes

    @Test
    public void scrollPaneViewportAndScrolling() {
        Region content = new Region();
        content.setMinSize(400, 600);
        content.setPrefSize(400, 600);
        content.setMaxSize(400, 600);
        ScrollPane pane = new ScrollPane(content);
        Pane root = new Pane();
        root.getChildren().add(pane);
        Scene scene = new Scene(root, 300, 300);
        pane.resizeRelocate(0, 0, 200, 150);
        scene.cn1Layout(300, 300);
        pane.resize(200, 150);
        pane.layout();

        // The content is a real descendant at its own size.
        assertSame(scene, content.getScene());
        assertEquals(400, content.getWidth(), EPS);
        assertEquals(600, content.getHeight(), EPS);
        Bounds viewport = pane.getViewportBounds();
        assertEquals(200, viewport.getWidth(), EPS);
        assertEquals(150, viewport.getHeight(), EPS);
        assertEquals(0, content.localToScene(0, 0).getY(), EPS);

        pane.setVvalue(1);
        pane.layout();
        assertEquals(-450, content.localToScene(0, 0).getY(), EPS);
        assertEquals(-450, pane.getViewportBounds().getMinY(), EPS);
        pane.setVvalue(0.5);
        pane.setHvalue(1);
        pane.layout();
        assertEquals(-225, content.localToScene(0, 0).getY(), EPS);
        assertEquals(-200, content.localToScene(0, 0).getX(), EPS);

        // As in JavaFX the value is not clamped, the position is.
        pane.setVvalue(7);
        pane.layout();
        assertEquals(-450, content.localToScene(0, 0).getY(), EPS);
        pane.setVvalue(-3);
        pane.layout();
        assertEquals(0, content.localToScene(0, 0).getY(), EPS);
        pane.setVvalue(0);

        // The wheel moves the value.
        pane.setHvalue(0);
        pane.layout();
        scene.cn1Wheel(50, 50, 0, -90);
        assertEquals(0.2, pane.getVvalue(), 0.01);
        scene.cn1Wheel(50, 50, 0, 9000);
        assertEquals(0, pane.getVvalue(), 0);

        // Content that fits takes the viewport when asked to.
        Region small = box(50, 40);
        pane.setContent(small);
        pane.setFitToWidth(true);
        pane.setFitToHeight(true);
        pane.layout();
        assertNull(content.getScene());
        assertEquals(200, small.getWidth(), EPS);
        assertEquals(150, small.getHeight(), EPS);

        pane.setPrefViewportWidth(120);
        pane.setPrefViewportHeight(80);
        assertTrue(pane.prefWidth(-1) >= 120);
        assertTrue(pane.prefHeight(-1) >= 80);
        assertPrefSize(pane);
    }

    @Test
    public void splitPaneDividers() {
        Region a = box(100, 100);
        Region b = box(100, 100);
        Region c = box(100, 100);
        SplitPane split = new SplitPane(a, b, c);
        assertEquals(2, split.getDividers().size());
        split.setDividerPositions(0.25, 0.75);
        split.resize(400, 200);
        split.layout();

        double[] positions = split.getDividerPositions();
        assertEquals(0.25, positions[0], 0.001);
        assertEquals(0.75, positions[1], 0.001);
        assertEquals(0, a.getBoundsInParent().getMinX(), EPS);
        assertEquals(100, a.getBoundsInParent().getMaxX(), 4);
        assertEquals(100, b.getBoundsInParent().getMinX(), 4);
        assertEquals(300, b.getBoundsInParent().getMaxX(), 4);
        assertEquals(300, c.getBoundsInParent().getMinX(), 4);
        assertEquals(400, c.getBoundsInParent().getMaxX(), EPS);
        assertEquals(200, a.getHeight(), EPS);
        // Items do not overlap and leave room for the divider bars.
        assertTrue(a.getBoundsInParent().getMaxX() <= b.getBoundsInParent().getMinX());
        assertTrue(b.getBoundsInParent().getMaxX() <= c.getBoundsInParent().getMinX());

        split.setDividerPosition(0, 0.5);
        split.layout();
        assertEquals(200, a.getBoundsInParent().getMaxX(), 4);
        split.getDividers().get(1).setPosition(0.9);
        split.layout();
        assertEquals(360, c.getBoundsInParent().getMinX(), 4);

        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.5, 0.75);
        split.resize(200, 400);
        split.layout();
        assertTrue(has(split, "vertical"));
        assertEquals(200, a.getWidth(), EPS);
        assertEquals(200, a.getBoundsInParent().getMaxY(), 4);
        assertEquals(300, c.getBoundsInParent().getMinY(), 4);

        split.getItems().remove(b);
        assertEquals(1, split.getDividers().size());
        assertNull(b.getParent());
        assertPrefSize(split);
    }

    @Test
    public void splitPaneDividerDrags() {
        Region a = box(100, 100);
        Region b = box(100, 100);
        SplitPane split = new SplitPane(a, b);
        Pane root = new Pane();
        root.getChildren().add(split);
        Scene scene = new Scene(root, 400, 200);
        scene.cn1Layout(400, 200);
        split.resizeRelocate(0, 0, 400, 200);
        split.layout();
        assertEquals(0.5, split.getDividerPositions()[0], 0.001);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 200, 100, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, 300, 100, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 300, 100, MouseButton.PRIMARY);
        assertEquals(0.75, split.getDividerPositions()[0], 0.02);
        split.layout();
        assertEquals(300, a.getBoundsInParent().getMaxX(), 6);
    }

    @Test
    public void tabPaneSelectionSwitchesContent() {
        Region first = box(80, 60);
        Region second = box(120, 90);
        Tab one = new Tab("One", first);
        Tab two = new Tab("Two", second);
        TabPane tabs = new TabPane(one, two);
        assertSame(tabs, one.getTabPane());
        assertSame(one, tabs.getSelectionModel().getSelectedItem());
        assertEquals(0, tabs.getSelectionModel().getSelectedIndex());
        assertTrue(one.isSelected());
        assertFalse(two.isSelected());

        Pane root = new Pane();
        root.getChildren().add(tabs);
        Scene scene = new Scene(root, 400, 300);
        scene.cn1Layout(400, 300);
        tabs.resizeRelocate(0, 0, 300, 200);
        tabs.layout();

        // Both contents are in the scene graph; only the selected one shows.
        assertSame(scene, first.getScene());
        assertSame(scene, second.getScene());
        assertTrue(first.getParent().isVisible());
        assertFalse(second.getParent().isVisible());
        double strip = first.localToScene(0, 0).getY();
        assertTrue("strip " + strip, strip > 4 && strip < 80);
        assertEquals(300, first.getWidth(), EPS);
        assertEquals(200 - strip, first.getHeight(), EPS);

        final List<String> log = new ArrayList<String>();
        two.setOnSelectionChanged(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                log.add("two " + two.isSelected());
            }
        });
        tabs.getSelectionModel().select(1);
        tabs.layout();
        assertTrue(two.isSelected());
        assertFalse(one.isSelected());
        assertEquals("[two true]", log.toString());
        assertFalse(first.getParent().isVisible());
        assertTrue(second.getParent().isVisible());
        assertEquals(300, second.getWidth(), EPS);
        assertEquals(strip, second.localToScene(0, 0).getY(), EPS);

        // Pressing a header selects its tab.
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 8, strip / 2, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 8, strip / 2, MouseButton.PRIMARY);
        assertTrue(one.isSelected());
        assertSame(one, tabs.getSelectionModel().getSelectedItem());

        // A disabled tab is not selected by the user.
        two.setDisable(true);
        assertTrue(two.isDisabled());
        tabs.getSelectionModel().selectNext();
        assertTrue(two.isSelected());
        tabs.getSelectionModel().selectFirst();
        two.setDisable(false);

        tabs.setSide(Side.BOTTOM);
        tabs.layout();
        assertTrue(has(tabs, "bottom"));
        assertEquals(0, first.localToScene(0, 0).getY(), EPS);
        assertEquals(200 - strip, first.getHeight(), EPS);
        tabs.setSide(Side.LEFT);
        tabs.layout();
        assertTrue(first.localToScene(0, 0).getX() > 4);
        assertEquals(200, first.getHeight(), EPS);

        assertTrue(tabs.prefWidth(-1) >= 120);
        assertTrue(tabs.prefHeight(-1) >= 90);
        assertPrefSize(tabs);
    }

    @Test
    public void tabPaneAddRemoveAndClose() {
        TabPane tabs = new TabPane();
        assertNull(tabs.getSelectionModel().getSelectedItem());
        assertEquals(-1, tabs.getSelectionModel().getSelectedIndex());
        final Tab one = new Tab("One", box(10, 10));
        final Tab two = new Tab("Two", box(10, 10));
        Tab three = new Tab("Three");
        tabs.getTabs().addAll(one, two, three);
        assertSame(one, tabs.getSelectionModel().getSelectedItem());
        tabs.getSelectionModel().select(two);
        assertEquals(1, tabs.getSelectionModel().getSelectedIndex());

        // Removing an earlier tab keeps the selected tab selected.
        tabs.getTabs().remove(one);
        assertNull(one.getTabPane());
        assertFalse(one.isSelected());
        assertSame(two, tabs.getSelectionModel().getSelectedItem());
        assertEquals(0, tabs.getSelectionModel().getSelectedIndex());

        // Removing the selected tab selects a neighbour.
        tabs.getTabs().remove(two);
        assertSame(three, tabs.getSelectionModel().getSelectedItem());
        assertTrue(three.isSelected());
        assertFalse(two.isSelected());

        tabs.getTabs().clear();
        assertNull(tabs.getSelectionModel().getSelectedItem());
        assertFalse(three.isSelected());
        assertNotNull(tabs.getTabClosingPolicy());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        assertEquals(TabPane.TabClosingPolicy.ALL_TABS, tabs.getTabClosingPolicy());
    }
}
