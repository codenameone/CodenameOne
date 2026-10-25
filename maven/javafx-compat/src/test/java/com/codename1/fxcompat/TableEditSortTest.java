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
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

/// Sorting a table by its headers, editing a cell, clipping a node and the
/// accelerators of a menu bar.
public class TableEditSortTest {

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
        Units.setScale(0);
        Platform.setImplicitExit(implicitExit);
    }

    private static final class Person {
        final SimpleStringProperty name;
        final SimpleIntegerProperty age;

        Person(String name, int age) {
            this.name = new SimpleStringProperty(name);
            this.age = new SimpleIntegerProperty(age);
        }
    }

    private static ObservableList<Person> people() {
        ObservableList<Person> list = FXCollections.observableArrayList();
        list.add(new Person("Carol", 41));
        list.add(new Person("alice", 30));
        list.add(new Person("Bob", 30));
        return list;
    }

    private static String names(List<Person> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(i == 0 ? "" : ",").append(list.get(i).name.get());
        }
        return sb.toString();
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

    private static void clickAt(Scene scene, Node node, double x) {
        Point2D p = node.localToScene(x, 4);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, p.getX(), p.getY(), MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, p.getX(), p.getY(), MouseButton.PRIMARY);
    }

    private static Label header(Parent table, String text) {
        List<Node> children = table.getChildrenUnmodifiable();
        for (int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (n instanceof Parent && n.getStyleClass().contains("column-header")) {
                Node inside = ((Parent) n).getChildrenUnmodifiable().get(0);
                if (inside instanceof Label && text.equals(((Label) inside).getText())) {
                    return (Label) inside;
                }
            }
        }
        return null;
    }

    private static TableColumn<Person, String> nameColumn() {
        TableColumn<Person, String> name = new TableColumn<Person, String>("Name");
        name.setCellValueFactory(f -> f.getValue().name);
        name.setPrefWidth(120);
        return name;
    }

    private static TableColumn<Person, Number> ageColumn() {
        TableColumn<Person, Number> age = new TableColumn<Person, Number>("Age");
        age.setCellValueFactory(f -> f.getValue().age);
        age.setPrefWidth(60);
        return age;
    }

    @Test
    public void aHeaderClickSortsAscendingThenDescendingThenNotAtAll() {
        ObservableList<Person> source = people();
        SortedList<Person> sorted = new SortedList<Person>(source);
        TableView<Person> table = new TableView<Person>();
        TableColumn<Person, String> name = nameColumn();
        TableColumn<Person, Number> age = ageColumn();
        table.getColumns().add(name);
        table.getColumns().add(age);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        Scene scene = sceneOf(table, 300, 160);
        assertNull(table.getComparator());
        assertEquals("Carol,alice,Bob", names(table.getItems()));

        // Each click well apart from the last, so none is a double click.
        assertNotNull(header(table, "Age"));
        clickAt(scene, header(table, "Age"), 4);
        assertEquals(1, table.getSortOrder().size());
        assertSame(age, table.getSortOrder().get(0));
        assertEquals(TableColumn.SortType.ASCENDING, age.getSortType());
        assertNotNull(table.getComparator());
        assertEquals("equal ages keep their order", "alice,Bob,Carol", names(table.getItems()));
        assertEquals("the source is left alone", "Carol,alice,Bob", names(source));

        scene.cn1Layout(300, 160);
        clickAt(scene, header(table, "Age"), 24);
        assertEquals(TableColumn.SortType.DESCENDING, age.getSortType());
        assertEquals("Carol,alice,Bob", names(table.getItems()));

        scene.cn1Layout(300, 160);
        clickAt(scene, header(table, "Age"), 44);
        assertTrue(table.getSortOrder().isEmpty());
        assertNull(table.getComparator());
        assertEquals("Carol,alice,Bob", names(table.getItems()));
    }

    @Test
    public void theSortOrderSortsAPlainListInPlace() {
        ObservableList<Person> list = people();
        TableView<Person> table = new TableView<Person>(list);
        TableColumn<Person, String> name = nameColumn();
        TableColumn<Person, Number> age = ageColumn();
        table.getColumns().add(name);
        table.getColumns().add(age);
        sceneOf(table, 300, 160);

        table.getSortOrder().add(name);
        assertEquals("strings sort as String.compareTo orders them", "Bob,Carol,alice", names(list));
        name.setComparator(String.CASE_INSENSITIVE_ORDER);
        assertEquals("alice,Bob,Carol", names(list));
        name.setSortType(TableColumn.SortType.DESCENDING);
        assertEquals("Carol,Bob,alice", names(list));
        table.getSortOrder().setAll(java.util.Arrays.<TableColumn<Person, ?>>asList(age, name));
        assertEquals("by age, then by name descending", "Bob,alice,Carol", names(list));
        name.setSortable(false);
        assertEquals("an unsortable column is skipped", "Bob,alice,Carol", names(list));
    }

    @Test
    public void aCellIsEditedAndTheCommitReachesTheModel() {
        final ObservableList<Person> list = people();
        TableView<Person> table = new TableView<Person>(list);
        table.setFixedCellSize(20);
        TableColumn<Person, String> name = nameColumn();
        final List<TableCell<Person, String>> cells = new ArrayList<TableCell<Person, String>>();
        final Callback<TableColumn<Person, String>, TableCell<Person, String>> factory =
                TextFieldTableCell.forTableColumn();
        name.setCellFactory(column -> {
            TableCell<Person, String> cell = factory.call(column);
            cells.add(cell);
            return cell;
        });
        table.getColumns().add(name);
        table.getColumns().add(ageColumn());
        Scene scene = sceneOf(table, 300, 160);
        TableCell<Person, String> second = null;
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).getIndex() == 1) {
                second = cells.get(i);
            }
        }
        assertNotNull(second);
        assertEquals("alice", second.getText());

        clickAt(scene, second, 4);
        clickAt(scene, second, 4);
        assertFalse("a table is not editable until it is told to be", second.isEditing());
        assertNull(table.getEditingCell());

        table.setEditable(true);
        clickAt(scene, second, 4);
        clickAt(scene, second, 4);
        assertTrue("a double click edits the cell", second.isEditing());
        assertNotNull(table.getEditingCell());
        assertEquals(1, table.getEditingCell().getRow());
        assertSame(name, table.getEditingCell().getTableColumn());
        assertTrue(second.getGraphic() instanceof TextField);
        TextField field = (TextField) second.getGraphic();
        assertEquals("alice", field.getText());
        assertNull(second.getText());

        second.cancelEdit();
        assertFalse(second.isEditing());
        assertNull(table.getEditingCell());
        assertEquals("alice", second.getText());
        assertNull(second.getGraphic());

        final String[] committed = new String[2];
        name.setOnEditCommit(event -> {
            committed[0] = event.getNewValue();
            committed[1] = event.getOldValue();
            assertSame(list.get(1), event.getRowValue());
            event.getRowValue().name.set(event.getNewValue());
        });
        table.edit(1, name);
        assertTrue(second.isEditing());
        second.commitEdit("Alicia");
        assertFalse(second.isEditing());
        assertEquals("Alicia", committed[0]);
        assertEquals("alice", committed[1]);
        assertEquals("Alicia", list.get(1).name.get());
        assertEquals("Alicia", second.getText());
        assertNull(table.getEditingCell());

        name.setEditable(false);
        table.edit(1, name);
        assertFalse(second.isEditing());
    }

    @Test
    public void withoutAHandlerACommitIsWrittenToTheObservableValue() {
        ObservableList<Person> list = people();
        TableView<Person> table = new TableView<Person>(list);
        table.setEditable(true);
        TableColumn<Person, String> name = nameColumn();
        name.setCellFactory(column -> new TextFieldTableCell<Person, String>(new DefaultStringConverter()));
        table.getColumns().add(name);
        sceneOf(table, 300, 160);
        table.edit(0, name);
        assertNotNull(table.getEditingCell());
        TableCell<Person, String> editing = null;
        List<Node> all = new ArrayList<Node>();
        collect(table, all);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof TableCell && ((TableCell<?, ?>) all.get(i)).isEditing()) {
                @SuppressWarnings("unchecked")
                TableCell<Person, String> found = (TableCell<Person, String>) all.get(i);
                editing = found;
            }
        }
        assertNotNull(editing);
        editing.commitEdit("Caroline");
        assertEquals("Caroline", list.get(0).name.get());
    }

    private static void collect(Node node, List<Node> into) {
        into.add(node);
        if (node instanceof Parent) {
            List<Node> children = ((Parent) node).getChildrenUnmodifiable();
            for (int i = 0; i < children.size(); i++) {
                collect(children.get(i), into);
            }
        }
    }

    @Test
    public void aClippedNodeIsOnlyHitInsideItsClip() {
        Rectangle box = new Rectangle(0, 0, 100, 100);
        Pane root = new Pane(box);
        Scene scene = new Scene(root, 200, 200);
        scene.cn1Layout(200, 200);
        assertSame(box, root.cn1Pick(80, 80));
        assertNull(box.getClip());
        assertNull(box.cn1ClipPath());

        Rectangle clip = new Rectangle(10, 10, 40, 30);
        box.setClip(clip);
        assertSame(clip, box.getClip());
        assertSame(box, root.cn1Pick(20, 20));
        assertFalse("outside the clip the node is not there", root.cn1Pick(80, 80) == box);
        double[] b = box.cn1ClipPath().bounds();
        assertEquals(10, b[0], 0.01);
        assertEquals(10, b[1], 0.01);
        assertEquals(50, b[2], 0.01);
        assertEquals(40, b[3], 0.01);

        box.setClip(null);
        assertSame(box, root.cn1Pick(80, 80));
    }

    private static KeyEvent press(KeyCode code, boolean shift, boolean control, boolean alt) {
        return new KeyEvent(null, null, KeyEvent.KEY_PRESSED, "", "", code, shift, control, alt, false);
    }

    @Test
    public void keyCombinationsReadAndMatch() {
        KeyCombination save = KeyCombination.valueOf("Ctrl+Shift+S");
        assertEquals(new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN), save);
        assertEquals("Shift+Ctrl+S", save.getName());
        assertEquals(KeyCombination.ModifierValue.UP, save.getAlt());
        assertTrue(save.match(press(KeyCode.S, true, true, false)));
        assertFalse("a modifier that must be up is down", save.match(press(KeyCode.S, true, true, true)));
        assertFalse(save.match(press(KeyCode.A, true, true, false)));
        KeyCombination any = KeyCombination.keyCombination("Ignore Shift+F5");
        assertEquals(KeyCombination.ModifierValue.ANY, any.getShift());
        assertTrue(any.match(press(KeyCode.F5, true, false, false)));
        assertTrue(any.match(press(KeyCode.F5, false, false, false)));
        try {
            KeyCombination.valueOf("Ctrl+Ctrl+S");
            org.junit.Assert.fail("a modifier named twice");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Duplicate"));
        }
        try {
            new KeyCodeCombination(KeyCode.SHIFT);
            org.junit.Assert.fail("a modifier as the key");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void anAcceleratorFiresItsMenuItemAndTheScenesOwnRun() {
        MenuItem plain = new MenuItem("Refresh");
        plain.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        final int[] fired = new int[2];
        plain.setOnAction(event -> fired[0]++);
        Menu menu = new Menu("View");
        menu.getItems().add(plain);
        MenuBar bar = new MenuBar();
        bar.getMenus().add(menu);
        VBox root = new VBox(bar, new Label("content"));
        Scene scene = new Scene(root, 300, 200);
        scene.cn1Layout(300, 200);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F6), () -> fired[1]++);

        scene.cn1Key(KeyEvent.KEY_PRESSED, KeyCode.F5, "");
        assertEquals(1, fired[0]);
        scene.cn1Key(KeyEvent.KEY_RELEASED, KeyCode.F5, "");
        assertEquals("only a press fires", 1, fired[0]);
        scene.cn1Key(KeyEvent.KEY_PRESSED, KeyCode.F6, "");
        assertEquals(1, fired[1]);
        plain.setDisable(true);
        scene.cn1Key(KeyEvent.KEY_PRESSED, KeyCode.F5, "");
        assertEquals("a disabled item is not fired", 1, fired[0]);
    }
}
