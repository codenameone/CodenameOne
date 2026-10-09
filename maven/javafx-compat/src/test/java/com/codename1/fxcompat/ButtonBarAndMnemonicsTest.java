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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Mnemonics;
import com.codename1.fxcompat.runtime.Units;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;

/// A button bar places its buttons in a row at the right; the mark of a
/// mnemonic is taken out of the text a control shows; the columns of a
/// constrained table share its width.
public class ButtonBarAndMnemonicsTest {

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

    private static Region box(double width) {
        Region r = new Region();
        r.setPrefSize(width, 20);
        return r;
    }

    @Test
    public void buttonsKeepTheirOrderAtTheRightAndShareAWidth() {
        ButtonBar bar = new ButtonBar();
        assertEquals(ButtonBar.BUTTON_ORDER_NONE, bar.getButtonOrder());
        Region ok = box(40);
        Region cancel = box(120);
        bar.getButtons().addAll(ok, cancel);
        assertEquals(250, bar.prefWidth(-1), 0.01);
        assertEquals(20, bar.prefHeight(-1), 0.01);
        bar.resize(400, 30);
        bar.layout();
        // Both are as wide as the wider, the last ends at the right edge.
        assertEquals(120, ok.getWidth(), 0.01);
        assertEquals(120, cancel.getWidth(), 0.01);
        assertEquals(280, cancel.getLayoutX(), 0.01);
        assertEquals(150, ok.getLayoutX(), 0.01);
        assertEquals(5, ok.getLayoutY(), 0.01);

        // A button that is not uniform keeps its own width.
        assertTrue(ButtonBar.isButtonUniformSize(ok));
        ButtonBar.setButtonUniformSize(ok, false);
        assertFalse(ButtonBar.isButtonUniformSize(ok));
        bar.requestLayout();
        bar.layout();
        assertEquals(40, ok.getWidth(), 0.01);
        assertEquals(230, ok.getLayoutX(), 0.01);

        bar.setButtonMinWidth(200);
        bar.layout();
        assertEquals(200, cancel.getWidth(), 0.01);
        assertEquals(40, ok.getWidth(), 0.01);
    }

    @Test
    public void anOrderSortsTheKindsAndPutsTheLeftOnesAtTheLeft() {
        ButtonBar bar = new ButtonBar(ButtonBar.BUTTON_ORDER_WINDOWS);
        Region cancel = box(50);
        Region ok = box(50);
        Region help = box(50);
        Region left = box(50);
        Region plain = box(50);
        assertNull(ButtonBar.getButtonData(ok));
        ButtonBar.setButtonData(cancel, ButtonData.CANCEL_CLOSE);
        ButtonBar.setButtonData(ok, ButtonData.OK_DONE);
        ButtonBar.setButtonData(help, ButtonData.HELP);
        ButtonBar.setButtonData(left, ButtonData.LEFT);
        assertEquals(ButtonData.OK_DONE, ButtonBar.getButtonData(ok));
        bar.getButtons().addAll(cancel, ok, help, left, plain);
        bar.resize(500, 20);
        bar.layout();
        // L_E+U+FBXI_YNOCAH_R: left, then other, ok, cancel and help.
        assertEquals(0, left.getLayoutX(), 0.01);
        assertEquals(450, help.getLayoutX(), 0.01);
        assertEquals(390, cancel.getLayoutX(), 0.01);
        assertEquals(330, ok.getLayoutX(), 0.01);
        assertEquals(270, plain.getLayoutX(), 0.01);
        ButtonBar.setButtonData(ok, null);
        assertNull(ButtonBar.getButtonData(ok));
    }

    @Test
    public void constrainedColumnsShareTheWidthOfTheTable() {
        TableView<String> table = new TableView<String>();
        assertTrue(table.getColumnResizePolicy() == TableView.UNCONSTRAINED_RESIZE_POLICY);
        TableColumn<String, String> a = new TableColumn<String, String>("A");
        TableColumn<String, String> b = new TableColumn<String, String>("B");
        TableColumn<String, String> hidden = new TableColumn<String, String>("C");
        a.setPrefWidth(100);
        b.setPrefWidth(300);
        hidden.setVisible(false);
        table.getColumns().add(a);
        table.getColumns().add(b);
        table.getColumns().add(hidden);
        table.resize(802, 200);
        table.layout();
        assertEquals(100, a.getWidth(), 0.01);
        assertEquals(300, b.getWidth(), 0.01);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.layout();
        double inner = a.getWidth() + b.getWidth();
        assertEquals(3, b.getWidth() / a.getWidth(), 0.001);
        assertTrue(inner > 780 && inner <= 802);

        // A column at its maximum keeps it and the other takes the rest.
        b.setMaxWidth(400);
        table.requestLayout();
        table.layout();
        assertEquals(400, b.getWidth(), 0.01);
        assertEquals(inner - 400, a.getWidth(), 0.01);

        TableView.ResizeFeatures<String> features = new TableView.ResizeFeatures<String>(table, a,
                Double.valueOf(5));
        assertTrue(features.getTable() == table);
        assertTrue(features.getColumn() == a);
        assertEquals(5, features.getDelta().doubleValue(), 0);
    }

    @Test
    public void theMarkOfAMnemonicIsNotShown() {
        assertNull(Mnemonics.strip(null));
        assertEquals("Save", Mnemonics.strip("_Save"));
        assertEquals("Save As", Mnemonics.strip("Save _As"));
        assertEquals("a_b", Mnemonics.strip("a__b"));
        assertEquals("Open_file", Mnemonics.strip("_Open_file"));
        assertEquals("Print", Mnemonics.strip("Print_(p)"));
        assertEquals("end_", Mnemonics.strip("end_"));
        assertEquals("plain", Mnemonics.strip("plain"));

        // Buttons parse mnemonics, labels do not, as in JavaFX.
        Button button = new Button("_Go");
        assertTrue(button.isMnemonicParsing());
        assertTrue(new CheckBox().isMnemonicParsing());
        Label label = new Label("my_file");
        assertFalse(label.isMnemonicParsing());
        assertEquals("_Go", button.getText());
        assertEquals("Go", ((com.codename1.ui.Label) button.cn1Native()).getText());
        assertEquals("my_file", ((com.codename1.ui.Label) label.cn1Native()).getText());
        button.setMnemonicParsing(false);
        assertEquals("_Go", ((com.codename1.ui.Label) button.cn1Native()).getText());

        MenuItem item = new MenuItem("_File");
        assertTrue(item.isMnemonicParsing());
        item.setMnemonicParsing(false);
        assertFalse(item.mnemonicParsingProperty().get());
    }
}
