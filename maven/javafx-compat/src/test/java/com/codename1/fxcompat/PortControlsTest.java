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
import com.codename1.fxcompat.runtime.StageForm;
import com.codename1.fxcompat.runtime.Units;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/// The controls under the keyboard and the pointer of a port: what a key
/// does to the control with the focus, to the default and the cancel
/// button and to an open menu, and what a pointer does to the controls
/// that keep a state between two presses.
///
/// Every event enters through the form on screen, as a port delivers it.
/// Each test here was a difference between the layer and JavaFX found by
/// playing one script of real input against both.
public class PortControlsTest {

    private static final int FIRE = -90;
    private static final int DOWN = -92;
    private static final int ESCAPE = 27;
    private static final int TAB = 9;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private boolean implicit;
    private Stage stage;
    private StageForm form;
    private final List<String> seen = new ArrayList<String>();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        // A desk: the form moves the focus on Tab itself, and a mouse is no finger.
        HeadlessImplementation.setDesktop(true);
        Units.setScale(1);
        implicit = Platform.isImplicitExit();
        Platform.setImplicitExit(false);
    }

    @After
    public void tearDown() {
        if (stage != null && stage.isShowing()) {
            stage.hide();
        }
        Platform.setImplicitExit(implicit);
        HeadlessImplementation.setDesktop(false);
        Units.setScale(0);
    }

    private void show(Parent root) {
        stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
        form = (StageForm) stage.cn1Host();
        form.revalidate();
    }

    private int px(double sceneX) {
        return form.getContentPane().getAbsoluteX() + Units.toPixels(sceneX);
    }

    private int py(double sceneY) {
        return form.getContentPane().getAbsoluteY() + Units.toPixels(sceneY);
    }

    private void click(double sceneX, double sceneY) {
        form.pointerPressed(new int[] {px(sceneX)}, new int[] {py(sceneY)});
        form.pointerReleased(new int[] {px(sceneX)}, new int[] {py(sceneY)});
        MainThreadRule.drain();
    }

    private void key(int code) {
        form.keyPressed(code);
        form.keyReleased(code);
        MainThreadRule.drain();
    }

    private <T extends Region> T at(Pane root, T node, double x, double y) {
        node.relocate(x, y);
        node.setPrefSize(80, 30);
        node.setMinSize(80, 30);
        node.setMaxSize(80, 30);
        root.getChildren().add(node);
        return node;
    }

    private <T extends ButtonBase> T reporting(T button, final String name) {
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                seen.add(name);
            }
        });
        return button;
    }

    /// Two radio buttons of a group, pressed in turn more than once: each
    /// press of the one that is not selected selects it, the third as the
    /// first.
    @Test
    public void aRadioButtonIsSelectedAgainAfterItsGroupMovedOn() {
        Pane root = new Pane();
        final RadioButton one = at(root, new RadioButton("One"), 20, 20);
        final RadioButton two = at(root, new RadioButton("Two"), 120, 20);
        ToggleGroup group = new ToggleGroup();
        one.setToggleGroup(group);
        two.setToggleGroup(group);
        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> o, Toggle was, Toggle now) {
                seen.add(now == one ? "one" : now == two ? "two" : "none");
            }
        });
        show(root);
        click(40, 35);
        click(140, 35);
        click(40, 35);
        assertSame(one, stage.getScene().getFocusOwner());
        click(140, 35);
        assertEquals("[one, two, one, two]", seen.toString());
        assertTrue(two.isSelected());
        assertFalse(one.isSelected());
    }

    /// A slider dragged along its track: its value is changing from the
    /// drag until the pointer is let go of, and the value the application
    /// reads when that ends is where the pointer was let go of.
    @Test
    public void aSliderDraggedSaysItsValueIsChangingUntilItIsLetGoOf() {
        Pane root = new Pane();
        final Slider slider = new Slider(0, 100, 0);
        slider.relocate(20, 20);
        slider.setPrefSize(300, 30);
        slider.setMinSize(300, 30);
        slider.setMaxSize(300, 30);
        root.getChildren().add(slider);
        slider.valueChangingProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> o, Boolean was, Boolean now) {
                seen.add(now.booleanValue() ? "changing" : (slider.getValue() > 60 ? "high" : "low"));
            }
        });
        show(root);
        form.pointerPressed(new int[] {px(28)}, new int[] {py(35)});
        for (int x = 48; x <= 288; x += 20) {
            form.pointerDragged(new int[] {px(x)}, new int[] {py(35)});
            MainThreadRule.drain();
        }
        assertTrue(slider.isValueChanging());
        form.pointerReleased(new int[] {px(288)}, new int[] {py(35)});
        MainThreadRule.drain();
        assertFalse(slider.isValueChanging());
        assertEquals("[changing, high]", seen.toString());
    }

    /// Space and Enter on the control with the focus do once what a click
    /// on it does, whatever kind of button it is.
    @Test
    public void theKeyThatFiresActsOnceOnTheButtonWithTheFocus() {
        Pane root = new Pane();
        at(root, reporting(new Button("b"), "button"), 20, 20);
        ToggleButton toggle = at(root, reporting(new ToggleButton("t"), "toggle"), 120, 20);
        CheckBox check = at(root, reporting(new CheckBox("c"), "check"), 220, 20);
        at(root, reporting(new Hyperlink("h"), "link"), 20, 80);
        show(root);
        click(40, 35);
        seen.clear();
        key(FIRE);
        assertEquals("[button]", seen.toString());
        key(TAB);
        assertSame(toggle, stage.getScene().getFocusOwner());
        key(FIRE);
        assertTrue(toggle.isSelected());
        key(' ');
        assertFalse(toggle.isSelected());
        key(TAB);
        assertSame(check, stage.getScene().getFocusOwner());
        key(FIRE);
        assertTrue(check.isSelected());
        key(TAB);
        key(FIRE);
        assertEquals("[button, toggle, toggle, check, link]", seen.toString());
    }

    /// Tab moves the focus one control on in the order of the scene graph
    /// and no further, and a key no control makes anything of leaves the
    /// focus where it is: the form under the scene is not asked.
    @Test
    public void theFocusMovesOnTabOnlyAndInTheOrderOfTheScene() {
        Pane root = new Pane();
        // Laid out against the order they were added in.
        Button first = at(root, reporting(new Button("1"), "first"), 220, 80);
        Button second = at(root, reporting(new Button("2"), "second"), 20, 20);
        Button third = at(root, reporting(new Button("3"), "third"), 120, 20);
        show(root);
        click(240, 95);
        seen.clear();
        key(TAB);
        assertSame(second, stage.getScene().getFocusOwner());
        key(TAB);
        assertSame(third, stage.getScene().getFocusOwner());
        key('q');
        key(16);
        key(113);
        assertSame(third, stage.getScene().getFocusOwner());
        key(FIRE);
        assertEquals("[third]", seen.toString());
        key(TAB);
        assertSame(first, stage.getScene().getFocusOwner());
    }

    /// An arrow key on a button moves the focus to the nearest control in
    /// that direction, and stays where it is when there is none.
    @Test
    public void anArrowOnAButtonMovesTheFocusThatWay() {
        Pane root = new Pane();
        Button a = at(root, new Button("a"), 20, 20);
        Button b = at(root, new Button("b"), 120, 20);
        Button c = at(root, new Button("c"), 20, 80);
        show(root);
        click(40, 35);
        key(-94);
        assertSame(b, stage.getScene().getFocusOwner());
        key(-94);
        assertSame(b, stage.getScene().getFocusOwner());
        key(-93);
        assertSame(a, stage.getScene().getFocusOwner());
        key(DOWN);
        assertSame(c, stage.getScene().getFocusOwner());
        key(-91);
        assertSame(a, stage.getScene().getFocusOwner());
    }

    /// The press of the key arms the button and the release fires it, so
    /// a key held down fires once.
    @Test
    public void aKeyHeldDownOnAButtonFiresItWhenItIsLetGoOf() {
        Pane root = new Pane();
        Button b = at(root, reporting(new Button("b"), "button"), 20, 20);
        show(root);
        click(40, 35);
        seen.clear();
        form.keyPressed(FIRE);
        MainThreadRule.drain();
        assertTrue(b.isArmed());
        assertEquals("[]", seen.toString());
        form.keyReleased(FIRE);
        MainThreadRule.drain();
        assertFalse(b.isArmed());
        assertEquals("[button]", seen.toString());
    }

    /// Enter with the focus on nothing that takes it fires the default
    /// button, and Escape the cancel button.
    @Test
    public void enterFiresTheDefaultButtonAndEscapeTheCancelButton() {
        Pane root = new Pane();
        // The first control of a scene has the focus, and this one makes nothing of Enter.
        at(root, new Slider(0, 100, 0), 20, 100);
        CheckBox check = at(root, reporting(new CheckBox("c"), "check"), 220, 20);
        Button ok = at(root, reporting(new Button("ok"), "ok"), 20, 20);
        ok.setDefaultButton(true);
        Button cancel = at(root, reporting(new Button("cancel"), "cancel"), 120, 20);
        cancel.setCancelButton(true);
        show(root);
        key(FIRE);
        assertEquals("[ok]", seen.toString());
        key(ESCAPE);
        assertEquals("[ok, cancel]", seen.toString());
        // The control with the focus comes first: the key ticks the box.
        click(240, 35);
        seen.clear();
        key(FIRE);
        assertEquals("[check]", seen.toString());
        assertFalse(check.isSelected());
        key(ESCAPE);
        assertEquals("[check, cancel]", seen.toString());
        // A button that is not shown is not fired.
        seen.clear();
        cancel.setVisible(false);
        key(ESCAPE);
        assertEquals("[]", seen.toString());
    }

    /// A key handler of the scene that consumes the key keeps it from the
    /// default button.
    @Test
    public void aKeyTheApplicationConsumedFiresNoButton() {
        Pane root = new Pane();
        Label label = new Label("nothing");
        label.relocate(20, 100);
        root.getChildren().add(label);
        Button ok = new Button("ok");
        ok.relocate(20, 20);
        ok.setFocusTraversable(false);
        reporting(ok, "ok").setDefaultButton(true);
        root.getChildren().add(ok);
        show(root);
        stage.getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    seen.add("mine");
                    event.consume();
                }
            }
        });
        key(FIRE);
        assertEquals("[mine]", seen.toString());
    }

    private MenuItem item(final String name) {
        MenuItem item = new MenuItem(name);
        item.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                seen.add(name);
            }
        });
        return item;
    }

    /// An open context menu has the keyboard: Down moves through its
    /// items, Enter chooses the one moved to and closes the menu, and none
    /// of it reaches the scene behind.
    @Test
    public void anOpenMenuIsWalkedAndChosenFromWithTheKeyboard() {
        Pane root = new Pane();
        Button ok = at(root, reporting(new Button("ok"), "ok"), 20, 20);
        ok.setDefaultButton(true);
        Label anchor = new Label("anchor");
        anchor.relocate(200, 100);
        root.getChildren().add(anchor);
        MenuItem off = item("off");
        off.setDisable(true);
        ContextMenu menu = new ContextMenu(item("first"), off, item("third"));
        show(root);
        menu.show(anchor, 200, 100);
        MainThreadRule.drain();
        assertTrue(menu.isShowing());
        key(FIRE);
        assertTrue(menu.isShowing());
        assertEquals("[]", seen.toString());
        key(DOWN);
        key(DOWN);
        key(FIRE);
        assertEquals("[third]", seen.toString());
        assertFalse(menu.isShowing());
        menu.show(anchor, 200, 100);
        MainThreadRule.drain();
        key(ESCAPE);
        assertFalse(menu.isShowing());
        assertEquals("[third]", seen.toString());
        // With the menu gone the keys are the scene's again.
        key(FIRE);
        assertEquals("[third, ok]", seen.toString());
    }

    /// A button a little narrower than its text keeps the text where it is
    /// under the pointer and with the focus. The native label scrolled it
    /// back and forth, which is what a phone does and JavaFX never does.
    @Test
    public void theTextOfANarrowButtonDoesNotScroll() {
        Pane root = new Pane();
        Button narrow = new Button("A button with more text than room");
        narrow.resizeRelocate(10, 10, 60, 30);
        narrow.setManaged(false);
        root.getChildren().add(narrow);
        show(root);
        assertTrue(narrow.cn1Native() instanceof com.codename1.ui.Label);
        com.codename1.ui.Label label = (com.codename1.ui.Label) narrow.cn1Native();
        form.pointerHover(new int[] {px(30)}, new int[] {py(25)});
        narrow.requestFocus();
        label.requestFocus();
        MainThreadRule.drain();
        form.pointerHover(new int[] {px(32)}, new int[] {py(25)});
        MainThreadRule.drain();
        assertFalse(label.isTickerRunning());
    }

    /// A theme of a desktop port has a style for a component under the
    /// pointer, which `getAllStyles()` leaves out. The font and the fill
    /// of a control are the font and the fill of that one too: the text of
    /// a hovered button was drawn in the theme's font, wider than the room
    /// the button was given for its own.
    @Test
    public void aHoveredButtonKeepsItsFontAndItsFill() {
        java.util.Hashtable<String, Object> theme = new java.util.Hashtable<String, Object>();
        theme.put("Button.hover#fgColor", "ff0000");
        com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(theme);
        Pane root = new Pane();
        Button b = new Button("Click me");
        b.setFont(javafx.scene.text.Font.font(21));
        b.setTextFill(javafx.scene.paint.Color.BLUE);
        b.relocate(10, 10);
        root.getChildren().add(b);
        show(root);
        MainThreadRule.drain();
        com.codename1.ui.Component c = b.cn1Native();
        com.codename1.ui.plaf.Style hover = c.getHoverStyle();
        assertTrue("the theme has a hover style", hover != null);
        assertEquals(0x0000ff, hover.getFgColor());
        assertSame(c.getUnselectedStyle().getFont(), hover.getFont());
    }

    /// The menu of a menu bar, opened, is walked the same way.
    @Test
    public void theMenuOfAMenuBarIsChosenFromWithTheKeyboard() {
        Pane root = new Pane();
        Menu file = new Menu("File", null, item("open"), item("save"));
        MenuBar bar = new MenuBar(file);
        bar.setPrefWidth(400);
        root.getChildren().add(bar);
        show(root);
        file.show();
        MainThreadRule.drain();
        assertTrue(file.isShowing());
        key(DOWN);
        key(DOWN);
        key(FIRE);
        assertEquals("[save]", seen.toString());
        assertFalse(file.isShowing());
    }
}
