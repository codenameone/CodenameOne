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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.spinner.Picker;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import javafx.util.StringConverter;

/// The controls that show data and those that open something: lists and
/// tables with their cells and selection, combo and choice boxes, menus,
/// tooltips, dialogs and the date picker.
public class ControlsDataTest {

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
        // A failed test must not leave its popups to the next one.
        List<javafx.stage.Window> open = new ArrayList<javafx.stage.Window>(javafx.stage.Window.getWindows());
        for (int i = 0; i < open.size(); i++) {
            open.get(i).hide();
        }
        Units.setScale(0);
        Platform.setImplicitExit(implicitExit);
    }

    /// Counts the action events it is handed.
    private static final class Count implements EventHandler<ActionEvent> {
        int n;

        @Override
        public void handle(ActionEvent event) {
            n++;
        }
    }

    private static ObservableList<String> names(int n) {
        ObservableList<String> list = FXCollections.observableArrayList();
        for (int i = 0; i < n; i++) {
            list.add("item " + i);
        }
        return list;
    }

    /// Tells the listeners of a picker what it tells them when the user
    /// chose a value in its popup.
    private static void commit(Picker picker) {
        List<com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent>> all =
                new ArrayList<com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent>>(
                        picker.getListeners());
        for (int i = 0; i < all.size(); i++) {
            all.get(i).actionPerformed(new com.codename1.ui.events.ActionEvent(picker, -99, -99));
        }
    }

    private static Scene sceneOf(Node control, double w, double h) {
        Pane root = new Pane();
        root.getChildren().add(control);
        if (control instanceof Region) {
            // The pane gives its children their preferred size.
            ((Region) control).setPrefSize(w, h);
        }
        Scene scene = new Scene(root, w, h);
        scene.cn1Layout(w, h);
        return scene;
    }

    private static void click(Scene scene, Node node) {
        Point2D p = node.localToScene(4, 4);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, p.getX(), p.getY(), MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, p.getX(), p.getY(), MouseButton.PRIMARY);
    }

    private static <C extends javafx.scene.control.IndexedCell<?>> C cellAt(List<C> cells, int index) {
        C found = null;
        for (int i = 0; i < cells.size(); i++) {
            C c = cells.get(i);
            if (c.getIndex() == index && !c.isEmpty()) {
                assertNull("two cells show row " + index, found);
                found = c;
            }
        }
        return found;
    }

    private static <T> ListView<T> listOf(ObservableList<T> items, final List<ListCell<T>> cells) {
        ListView<T> list = new ListView<T>(items);
        list.setFixedCellSize(20);
        list.setCellFactory(new Callback<ListView<T>, ListCell<T>>() {
            @Override
            public ListCell<T> call(ListView<T> view) {
                ListCell<T> cell = new ListCell<T>() {
                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : "<" + item + ">");
                    }
                };
                cells.add(cell);
                return cell;
            }
        });
        return list;
    }

    @Test
    public void listViewShowsItsItemsInRecycledCells() {
        List<ListCell<String>> cells = new ArrayList<ListCell<String>>();
        ObservableList<String> items = names(100);
        ListView<String> list = listOf(items, cells);
        Scene scene = sceneOf(list, 200, 100);

        assertTrue("cells were made by the factory", cells.size() >= 5);
        assertTrue("only the rows in view have cells: " + cells.size(), cells.size() <= 8);
        int made = cells.size();
        for (int row = 0; row < 4; row++) {
            ListCell<String> cell = cellAt(cells, row);
            assertNotNull("row " + row, cell);
            assertEquals("<item " + row + ">", cell.getText());
            assertEquals("item " + row, cell.getItem());
            assertSame(list, cell.getListView());
            assertEquals(20, cell.getHeight(), 0.01);
        }
        assertEquals(20, cellAt(cells, 1).localToScene(0, 0).getY() - cellAt(cells, 0).localToScene(0, 0).getY(),
                0.01);
        assertNull(cellAt(cells, 50));

        list.scrollTo(50);
        scene.cn1Layout(200, 100);
        assertEquals("scrolling reuses the cells", made, cells.size());
        assertNull(cellAt(cells, 0));
        for (int row = 50; row < 54; row++) {
            assertEquals("<item " + row + ">", cellAt(cells, row).getText());
        }

        items.remove(50);
        scene.cn1Layout(200, 100);
        assertEquals("<item 51>", cellAt(cells, 50).getText());
        items.add(50, "new");
        scene.cn1Layout(200, 100);
        assertEquals("<new>", cellAt(cells, 50).getText());
        assertEquals("<item 51>", cellAt(cells, 51).getText());
        items.set(51, "changed");
        scene.cn1Layout(200, 100);
        assertEquals("<changed>", cellAt(cells, 51).getText());
        assertEquals(made, cells.size());

        items.setAll("a", "b");
        scene.cn1Layout(200, 100);
        assertEquals("<a>", cellAt(cells, 0).getText());
        assertEquals("<b>", cellAt(cells, 1).getText());
        assertNull(cellAt(cells, 2));
    }

    @Test
    public void listViewScrollsWithTheWheel() {
        List<ListCell<String>> cells = new ArrayList<ListCell<String>>();
        ListView<String> list = listOf(names(100), cells);
        Scene scene = sceneOf(list, 200, 100);
        assertNotNull(cellAt(cells, 0));
        scene.cn1Wheel(50, 50, 0, -60);
        scene.cn1Layout(200, 100);
        assertNull(cellAt(cells, 0));
        assertEquals("<item 3>", cellAt(cells, 3).getText());
        scene.cn1Wheel(50, 50, 0, 60);
        scene.cn1Layout(200, 100);
        assertEquals("<item 0>", cellAt(cells, 0).getText());
        scene.cn1Wheel(50, 50, 0, 60);
        scene.cn1Layout(200, 100);
        assertEquals("the top of the list does not scroll further", "<item 0>", cellAt(cells, 0).getText());
    }

    @Test
    public void listViewSelectsOnClick() {
        List<ListCell<String>> cells = new ArrayList<ListCell<String>>();
        ListView<String> list = listOf(names(100), cells);
        Scene scene = sceneOf(list, 200, 100);
        assertEquals(-1, list.getSelectionModel().getSelectedIndex());

        click(scene, cellAt(cells, 2));
        assertEquals(2, list.getSelectionModel().getSelectedIndex());
        assertEquals("item 2", list.getSelectionModel().getSelectedItem());
        assertTrue(cellAt(cells, 2).isSelected());
        assertFalse(cellAt(cells, 1).isSelected());

        click(scene, cellAt(cells, 1));
        assertEquals("[1]", list.getSelectionModel().getSelectedIndices().toString());
        assertTrue(cellAt(cells, 1).isSelected());
        assertFalse(cellAt(cells, 2).isSelected());

        list.getItems().add(0, "first");
        scene.cn1Layout(200, 100);
        assertEquals("the selection follows its item", 2, list.getSelectionModel().getSelectedIndex());
        assertEquals("item 1", list.getSelectionModel().getSelectedItem());
        assertTrue(cellAt(cells, 2).isSelected());
    }

    @Test
    public void listViewSelectsSeveralRows() {
        List<ListCell<String>> cells = new ArrayList<ListCell<String>>();
        ListView<String> list = listOf(names(100), cells);
        Scene scene = sceneOf(list, 200, 100);

        list.getSelectionModel().selectIndices(1, 3);
        assertEquals("single selection keeps one row", 1, list.getSelectionModel().getSelectedIndices().size());

        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.getSelectionModel().clearSelection();
        list.getSelectionModel().selectIndices(1, 3);
        scene.cn1Layout(200, 100);
        assertEquals("[1, 3]", list.getSelectionModel().getSelectedIndices().toString());
        assertEquals("[item 1, item 3]", list.getSelectionModel().getSelectedItems().toString());
        assertTrue(cellAt(cells, 1).isSelected());
        assertFalse(cellAt(cells, 2).isSelected());
        assertTrue(cellAt(cells, 3).isSelected());
        assertTrue(list.getSelectionModel().isSelected(3));

        list.getSelectionModel().selectRange(0, 3);
        assertEquals("[0, 1, 2, 3]", list.getSelectionModel().getSelectedIndices().toString());
        list.getSelectionModel().clearSelection(1);
        assertEquals("[0, 2, 3]", list.getSelectionModel().getSelectedIndices().toString());

        click(scene, cellAt(cells, 1));
        assertEquals("a plain click selects that row alone", "[1]",
                list.getSelectionModel().getSelectedIndices().toString());
    }

    @Test
    public void listViewShowsAPlaceholderWhileEmpty() {
        ListView<String> list = new ListView<String>();
        Label empty = new Label("nothing");
        list.setPlaceholder(empty);
        Scene scene = sceneOf(list, 200, 100);
        assertTrue(empty.isVisible());
        assertSame(list, empty.getParent());
        list.getItems().add("x");
        scene.cn1Layout(200, 100);
        assertFalse(empty.isVisible());
    }

    /// A row of the table tests.
    private static final class Person {
        final StringProperty name;
        final int age;

        Person(String name, int age) {
            this.name = new SimpleStringProperty(name);
            this.age = age;
        }
    }

    private static <T> Callback<TableColumn<Person, T>, TableCell<Person, T>> cellsInto(
            final List<TableCell<Person, T>> cells) {
        return new Callback<TableColumn<Person, T>, TableCell<Person, T>>() {
            @Override
            public TableCell<Person, T> call(TableColumn<Person, T> column) {
                TableCell<Person, T> cell = new TableCell<Person, T>() {
                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : String.valueOf(item));
                    }
                };
                cells.add(cell);
                return cell;
            }
        };
    }

    @Test
    public void tableViewShowsValuesFromTheColumnCallbacks() {
        ObservableList<Person> people = FXCollections.observableArrayList();
        for (int i = 0; i < 40; i++) {
            people.add(new Person("name " + i, 20 + i));
        }
        TableView<Person> table = new TableView<Person>(people);
        table.setFixedCellSize(20);
        TableColumn<Person, String> name = new TableColumn<Person, String>("Name");
        name.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Person, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Person, String> features) {
                assertSame(table, features.getTableView());
                return features.getValue().name;
            }
        });
        TableColumn<Person, Integer> age = new TableColumn<Person, Integer>("Age");
        age.setCellValueFactory(
                new Callback<TableColumn.CellDataFeatures<Person, Integer>, ObservableValue<Integer>>() {
                    @Override
                    public ObservableValue<Integer> call(TableColumn.CellDataFeatures<Person, Integer> features) {
                        return new ReadOnlyObjectWrapper<Integer>(Integer.valueOf(features.getValue().age));
                    }
                });
        List<TableCell<Person, String>> names = new ArrayList<TableCell<Person, String>>();
        List<TableCell<Person, Integer>> ages = new ArrayList<TableCell<Person, Integer>>();
        name.setCellFactory(cellsInto(names));
        age.setCellFactory(cellsInto(ages));
        name.setPrefWidth(100);
        age.setPrefWidth(60);
        table.getColumns().add(name);
        table.getColumns().add(age);
        Scene scene = sceneOf(table, 300, 160);

        assertSame(table, name.getTableView());
        assertEquals(100, name.getWidth(), 0.01);
        assertEquals("name 3", name.getCellData(3));
        assertEquals(Integer.valueOf(23), age.getCellData(3));
        assertTrue(names.size() >= 5 && names.size() <= 10);
        assertEquals(names.size(), ages.size());
        for (int row = 0; row < 4; row++) {
            assertEquals("name " + row, cellAt(names, row).getText());
            assertEquals(String.valueOf(20 + row), cellAt(ages, row).getText());
        }
        TableCell<Person, String> first = cellAt(names, 0);
        TableCell<Person, Integer> firstAge = cellAt(ages, 0);
        assertSame(name, first.getTableColumn());
        assertSame(table, first.getTableView());
        assertEquals(100, first.getWidth(), 0.01);
        assertEquals(60, firstAge.getWidth(), 0.01);
        assertEquals(20, first.getHeight(), 0.01);
        assertEquals(100, firstAge.localToScene(0, 0).getX() - first.localToScene(0, 0).getX(), 0.01);
        assertEquals(first.localToScene(0, 0).getY(), firstAge.localToScene(0, 0).getY(), 0.01);
        assertTrue("the rows start below the header", first.localToScene(0, 0).getY() > 5);

        people.get(2).name.set("renamed");
        assertEquals("a change of the observable value reaches the cell", "renamed", cellAt(names, 2).getText());

        name.setPrefWidth(140);
        scene.cn1Layout(300, 160);
        assertEquals(140, cellAt(names, 0).getWidth(), 0.01);
        assertEquals(140, cellAt(ages, 0).localToScene(0, 0).getX() - cellAt(names, 0).localToScene(0, 0).getX(),
                0.01);

        click(scene, cellAt(ages, 1));
        assertEquals(1, table.getSelectionModel().getSelectedIndex());
        assertSame(people.get(1), table.getSelectionModel().getSelectedItem());
        assertTrue(cellAt(names, 1).isSelected());
        assertTrue(cellAt(ages, 1).isSelected());
        assertFalse(cellAt(names, 0).isSelected());

        int made = names.size();
        table.scrollTo(30);
        scene.cn1Layout(300, 160);
        assertEquals(made, names.size());
        assertEquals("name 30", cellAt(names, 30).getText());
        assertEquals("50", cellAt(ages, 30).getText());

        people.remove(30);
        scene.cn1Layout(300, 160);
        assertEquals("name 31", cellAt(names, 30).getText());
        people.get(30).name.set("late");
        assertEquals("late", cellAt(names, 30).getText());
        table.refresh();
        scene.cn1Layout(300, 160);
        assertEquals("late", cellAt(names, 30).getText());
    }

    @Test
    public void comboBoxKeepsValueAndSelectionInStep() {
        ComboBox<String> combo = new ComboBox<String>(FXCollections.observableArrayList("a", "b", "c"));
        Count actions = new Count();
        combo.setOnAction(actions);
        assertNull(combo.getValue());
        assertEquals(-1, combo.getSelectionModel().getSelectedIndex());

        combo.setValue("b");
        assertEquals(1, combo.getSelectionModel().getSelectedIndex());
        assertEquals("b", combo.getSelectionModel().getSelectedItem());
        assertEquals(1, actions.n);

        combo.getSelectionModel().select(2);
        assertEquals("c", combo.getValue());
        assertEquals(2, actions.n);
        combo.getSelectionModel().select(2);
        assertEquals("the same value is no change", 2, actions.n);

        combo.getSelectionModel().selectFirst();
        assertEquals("a", combo.getValue());
        combo.getItems().add(0, "z");
        assertEquals("a", combo.getValue());
        assertEquals("the row of the value moved", 1, combo.getSelectionModel().getSelectedIndex());

        combo.setValue("other");
        assertEquals("other", combo.getValue());
        assertEquals(-1, combo.getSelectionModel().getSelectedIndex());

        combo.setValue(null);
        assertNull(combo.getSelectionModel().getSelectedItem());
        assertEquals(-1, combo.getSelectionModel().getSelectedIndex());

        combo.show();
        assertTrue(combo.isShowing());
        combo.hide();
        assertFalse(combo.isShowing());
        combo.setDisable(true);
        combo.show();
        assertFalse(combo.isShowing());
    }

    @Test
    public void comboBoxShowsItsValueInThePicker() {
        ComboBox<Integer> combo = new ComboBox<Integer>();
        combo.getItems().addAll(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3));
        combo.setPromptText("pick");
        combo.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object == null ? "" : "#" + object;
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        sceneOf(combo, 120, 30);
        assertTrue(combo.cn1Native() instanceof Picker);
        Picker picker = (Picker) combo.cn1Native();
        assertEquals("pick", picker.getText());
        assertEquals(3, picker.getStrings().length);
        assertEquals("#2", picker.getStrings()[1]);

        combo.setValue(Integer.valueOf(2));
        assertEquals("#2", picker.getText());
        assertEquals(1, picker.getSelectedStringIndex());

        combo.getItems().add(Integer.valueOf(4));
        assertEquals(4, picker.getStrings().length);
        assertEquals(1, picker.getSelectedStringIndex());

        // What the picker does when the user chose a row.
        picker.setSelectedStringIndex(3);
        commit(picker);
        assertEquals(Integer.valueOf(4), combo.getValue());
        assertEquals(3, combo.getSelectionModel().getSelectedIndex());
    }

    @Test
    public void choiceBoxKeepsValueAndSelectionInStep() {
        ChoiceBox<String> box = new ChoiceBox<String>();
        box.getItems().addAll("a", "b", "c");
        Count actions = new Count();
        box.setOnAction(actions);

        box.getSelectionModel().select(1);
        assertEquals("b", box.getValue());
        assertEquals(1, actions.n);
        box.setValue("c");
        assertEquals(2, box.getSelectionModel().getSelectedIndex());
        assertEquals(2, actions.n);

        sceneOf(box, 120, 30);
        Picker picker = (Picker) box.cn1Native();
        assertEquals("c", picker.getText());
        picker.setSelectedStringIndex(0);
        commit(picker);
        assertEquals("a", box.getValue());
        assertEquals(3, actions.n);

        box.getItems().remove("a");
        assertNull("a value that left the items is cleared", box.getValue());
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());

        box.show();
        assertTrue(box.isShowing());
        box.hide();
        assertFalse(box.isShowing());
    }

    private static Node rowOf(ContextMenu menu, String text) {
        Parent root = menu.getScene().getRoot();
        List<Node> rows = root.getChildrenUnmodifiable();
        for (int i = 0; i < rows.size(); i++) {
            Node n = rows.get(i);
            if (n instanceof Labeled && ((Labeled) n).getText().startsWith(text)) {
                return n;
            }
        }
        return null;
    }

    private static void clickRow(ContextMenu menu, String text) {
        Node row = rowOf(menu, text);
        assertNotNull("row " + text, row);
        click(menu.getScene(), row);
    }

    @Test
    public void menuItemsFireAndReportToTheirMenus() {
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        Menu more = new Menu("More");
        MenuItem deep = new MenuItem("Deep");
        more.getItems().add(deep);
        ContextMenu menu = new ContextMenu(copy, paste, more);
        assertSame(menu, copy.getParentPopup());
        assertSame(more, deep.getParentMenu());
        assertSame(menu, deep.getParentPopup());

        Count onCopy = new Count();
        Count onMenu = new Count();
        Count onMore = new Count();
        copy.setOnAction(onCopy);
        menu.setOnAction(onMenu);
        more.setOnAction(onMore);
        copy.fire();
        assertEquals(1, onCopy.n);
        assertEquals(1, onMenu.n);
        deep.fire();
        assertEquals("an action goes up through the menu it is in", 1, onMore.n);
        assertEquals(2, onMenu.n);

        menu.getItems().remove(copy);
        assertNull(copy.getParentPopup());
        copy.fire();
        assertEquals(2, onCopy.n);
        assertEquals(2, onMenu.n);
    }

    @Test
    public void contextMenuShowsRowsThatFireTheirItems() {
        Button owner = new Button("owner");
        Scene scene = sceneOf(owner, 100, 30);
        MenuItem copy = new MenuItem("Copy");
        MenuItem off = new MenuItem("Off");
        off.setDisable(true);
        MenuItem hidden = new MenuItem("Hidden");
        hidden.setVisible(false);
        CheckMenuItem check = new CheckMenuItem("Check");
        ContextMenu menu = new ContextMenu(copy, new SeparatorMenuItem(), off, hidden, check);
        Count onCopy = new Count();
        Count onOff = new Count();
        Count onCheck = new Count();
        copy.setOnAction(onCopy);
        off.setOnAction(onOff);
        check.setOnAction(onCheck);

        assertFalse(menu.isShowing());
        menu.show(owner, 10, 12);
        assertTrue(menu.isShowing());
        assertSame(owner, menu.getOwnerNode());
        assertEquals(10, menu.getAnchorX(), 0.01);
        assertEquals(12, menu.getAnchorY(), 0.01);
        assertNotNull(rowOf(menu, "Copy"));
        assertNull("an invisible item has no row", rowOf(menu, "Hidden"));
        assertTrue(rowOf(menu, "Off").isDisabled());
        assertTrue(rowOf(menu, "Copy").getBoundsInParent().getHeight() > 0);

        clickRow(menu, "Off");
        assertEquals("a disabled item cannot be chosen", 0, onOff.n);
        assertTrue(menu.isShowing());

        clickRow(menu, "Copy");
        assertEquals(1, onCopy.n);
        assertFalse("choosing an item hides the menu", menu.isShowing());
        assertNull(menu.getOwnerNode());

        menu.show(owner, Side.BOTTOM, 0, 0);
        assertTrue(menu.isShowing());
        assertTrue(owner.getHeight() > 0);
        assertEquals("below the owner", owner.getHeight(), menu.getAnchorY(), 0.01);
        clickRow(menu, "Check");
        assertTrue(check.isSelected());
        assertEquals(1, onCheck.n);
        assertFalse(menu.isShowing());

        menu.show(owner, 0, 0);
        menu.hide();
        assertFalse(menu.isShowing());
        assertNotNull(scene);

        new ContextMenu().show(owner, 0, 0);
    }

    @Test
    public void subMenuOpensFromItsRow() {
        Button owner = new Button("owner");
        sceneOf(owner, 100, 30);
        Menu more = new Menu("More");
        MenuItem deep = new MenuItem("Deep");
        more.getItems().add(deep);
        ContextMenu menu = new ContextMenu(new MenuItem("Top"), more);
        Count onDeep = new Count();
        deep.setOnAction(onDeep);

        menu.show(owner, 0, 0);
        assertFalse(more.isShowing());
        clickRow(menu, "More");
        assertTrue(more.isShowing());
        ContextMenu sub = null;
        List<javafx.stage.Window> windows = javafx.stage.Window.getWindows();
        for (int i = 0; i < windows.size(); i++) {
            javafx.stage.Window w = windows.get(i);
            if (w instanceof ContextMenu && w != menu) {
                sub = (ContextMenu) w;
            }
        }
        assertNotNull("the sub-menu is a popup of its own", sub);
        assertTrue(sub.isShowing());
        clickRow(sub, "Deep");
        assertEquals(1, onDeep.n);
        assertFalse(sub.isShowing());
        assertFalse(more.isShowing());
        assertFalse(menu.isShowing());
    }

    @Test
    public void controlOpensItsContextMenuWhereOneIsAskedFor() {
        Button owner = new Button("owner");
        Scene scene = sceneOf(owner, 100, 30);
        ContextMenu menu = new ContextMenu(new MenuItem("One"));
        assertNull(owner.getContextMenu());
        owner.setContextMenu(menu);
        assertSame(menu, owner.getContextMenu());
        assertSame(menu, owner.contextMenuProperty().get());

        scene.cn1ContextMenu(20, 10);
        assertTrue(menu.isShowing());
        assertSame(owner, menu.getOwnerNode());
        assertEquals(20, menu.getAnchorX(), 0.01);
        assertEquals(10, menu.getAnchorY(), 0.01);
        menu.hide();

        owner.setContextMenu(null);
        scene.cn1ContextMenu(20, 10);
        assertFalse(menu.isShowing());
    }

    @Test
    public void menuBarOpensAMenuFromItsText() {
        Menu file = new Menu("File");
        MenuItem open = new MenuItem("Open");
        file.getItems().add(open);
        Menu edit = new Menu("Edit");
        edit.getItems().add(new MenuItem("Undo"));
        MenuBar bar = new MenuBar(file, edit);
        Scene scene = sceneOf(bar, 200, 30);
        assertEquals(2, bar.getMenus().size());
        List<Node> headers = bar.getChildrenUnmodifiable();
        assertEquals(2, headers.size());
        assertEquals("File", ((Labeled) headers.get(0)).getText());
        assertTrue(headers.get(1).getBoundsInParent().getMinX() >= headers.get(0).getBoundsInParent().getMaxX() - 0.01);

        Point2D p = headers.get(0).localToScene(4, 4);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, p.getX(), p.getY(), MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, p.getX(), p.getY(), MouseButton.PRIMARY);
        assertTrue(file.isShowing());
        assertFalse(edit.isShowing());

        edit.show();
        assertTrue(edit.isShowing());
        file.hide();
        assertFalse(file.isShowing());

        ContextMenu popup = null;
        List<javafx.stage.Window> windows = javafx.stage.Window.getWindows();
        for (int i = 0; i < windows.size(); i++) {
            if (windows.get(i) instanceof ContextMenu) {
                assertNull("one popup is open: " + windows + " owners " + popup
                        + (popup == null ? "" : " " + popup.getOwnerNode() + " " + popup.isShowing()), popup);
                popup = (ContextMenu) windows.get(i);
            }
        }
        assertNotNull(popup);
        assertSame(headers.get(1), popup.getOwnerNode());
        Count undo = new Count();
        edit.getItems().get(0).setOnAction(undo);
        clickRow(popup, "Undo");
        assertEquals(1, undo.n);
        assertFalse(edit.isShowing());
        assertFalse(popup.isShowing());

        file.setDisable(true);
        file.show();
        assertFalse(file.isShowing());
    }

    @Test
    public void tooltipTextReachesTheNativeComponent() {
        Button button = new Button("b");
        sceneOf(button, 100, 30);
        Tooltip tip = new Tooltip("hint");
        button.setTooltip(tip);
        assertSame(tip, button.getTooltip());
        assertEquals("hint", button.cn1Native().getTooltip());
        tip.setText("other");
        assertEquals("other", button.cn1Native().getTooltip());
        button.setTooltip(null);
        assertNull(button.cn1Native().getTooltip());

        Label label = new Label("l");
        Tooltip.install(label, tip);
        assertEquals("other", label.cn1Native().getTooltip());
        Tooltip.uninstall(label, tip);
        assertNull(label.cn1Native().getTooltip());
        tip.setText("late");
        assertNull(label.cn1Native().getTooltip());
    }

    @Test
    public void buttonTypesSayWhatTheyAreFor() {
        assertEquals("OK", ButtonType.OK.getText());
        assertSame(ButtonData.OK_DONE, ButtonType.OK.getButtonData());
        assertSame(ButtonData.CANCEL_CLOSE, ButtonType.CANCEL.getButtonData());
        assertSame(ButtonData.CANCEL_CLOSE, ButtonType.CLOSE.getButtonData());
        assertSame(ButtonData.OTHER, new ButtonType("Mine").getButtonData());
        assertTrue(ButtonData.OK_DONE.isDefaultButton());
        assertFalse(ButtonData.OK_DONE.isCancelButton());
        assertTrue(ButtonData.CANCEL_CLOSE.isCancelButton());
        assertTrue(ButtonData.NO.isCancelButton());
        assertTrue(ButtonData.YES.isDefaultButton());
        assertEquals("C", ButtonData.CANCEL_CLOSE.getTypeCode());
    }

    @Test
    public void alertHasTheButtonsOfItsKind() {
        Alert info = new Alert(AlertType.INFORMATION);
        assertEquals("[" + ButtonType.OK + "]", info.getButtonTypes().toString());
        assertEquals("Message", info.getTitle());
        assertEquals("Message", info.getHeaderText());

        Alert confirm = new Alert(AlertType.CONFIRMATION, "Sure?");
        assertEquals(2, confirm.getButtonTypes().size());
        assertSame(ButtonType.OK, confirm.getButtonTypes().get(0));
        assertSame(ButtonType.CANCEL, confirm.getButtonTypes().get(1));
        assertEquals("Sure?", confirm.getContentText());
        assertEquals("Sure?", confirm.getDialogPane().getContentText());
        assertEquals(0, new Alert(AlertType.NONE).getButtonTypes().size());

        confirm.setAlertType(AlertType.ERROR);
        assertEquals("the buttons follow the kind", 1, confirm.getButtonTypes().size());
        assertEquals("Error", confirm.getTitle());

        Alert own = new Alert(AlertType.WARNING, "Save?", ButtonType.YES, ButtonType.NO);
        assertSame(ButtonType.YES, own.getButtonTypes().get(0));
        assertSame(ButtonType.NO, own.getButtonTypes().get(1));
        own.setAlertType(AlertType.CONFIRMATION);
        assertEquals("buttons of the application stay", 2, own.getButtonTypes().size());
        assertSame(ButtonType.YES, own.getButtonTypes().get(0));

        Node yes = own.getDialogPane().lookupButton(ButtonType.YES);
        assertTrue(yes instanceof Button);
        assertEquals("Yes", ((Button) yes).getText());
        assertTrue(((Button) yes).isDefaultButton());
        assertTrue(((Button) own.getDialogPane().lookupButton(ButtonType.NO)).isCancelButton());
        assertNull(own.getDialogPane().lookupButton(ButtonType.OK));
    }

    @Test
    public void alertAnswersWithTheButtonChosen() {
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Sure?");
        assertNull(confirm.getResult());
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.OK)).fire();
        assertSame(ButtonType.OK, confirm.getResult());

        Alert cancelled = new Alert(AlertType.CONFIRMATION, "Sure?");
        cancelled.close();
        assertSame("closing answers with the cancel button", ButtonType.CANCEL, cancelled.getResult());

        Alert set = new Alert(AlertType.CONFIRMATION, "Sure?");
        set.setResult(ButtonType.CANCEL);
        assertSame(ButtonType.CANCEL, set.getResult());
    }

    @Test
    public void dialogConvertsTheButtonIntoItsResult() {
        Dialog<String> dialog = new Dialog<String>();
        final ButtonType save = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.setHeaderText("Header");
        dialog.setContentText("Content");
        dialog.setTitle("Title");
        assertEquals("Header", dialog.getDialogPane().getHeaderText());
        assertEquals("Title", dialog.getTitle());
        dialog.setResultConverter(new Callback<ButtonType, String>() {
            @Override
            public String call(ButtonType chosen) {
                return chosen == save ? "saved" : null;
            }
        });
        final List<String> log = new ArrayList<String>();
        dialog.setOnShown(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                log.add("shown");
            }
        });
        dialog.setOnHidden(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                log.add("hidden");
            }
        });
        dialog.initModality(javafx.stage.Modality.NONE);
        dialog.show();
        assertTrue(dialog.isShowing());
        assertTrue(dialog.getDialogPane().getWidth() > 0);
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        assertTrue("the buttons are in a row", Math.abs(saveButton.getBoundsInParent().getMinY()
                - cancelButton.getBoundsInParent().getMinY()) < 0.01);
        assertTrue(saveButton.getBoundsInParent().getMaxX() <= cancelButton.getBoundsInParent().getMinX() + 0.01);

        ((Button) saveButton).fire();
        assertEquals("saved", dialog.getResult());
        assertFalse(dialog.isShowing());
        assertEquals("[shown, hidden]", log.toString());
    }

    @Test
    public void dialogClosesWithoutAResultOnlyWhenItMay() {
        Dialog<String> stuck = new Dialog<String>();
        stuck.initModality(javafx.stage.Modality.NONE);
        stuck.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.APPLY);
        stuck.show();
        stuck.close();
        assertTrue("two buttons, none of them cancels", stuck.isShowing());
        stuck.setResult("done");
        assertFalse(stuck.isShowing());
        assertEquals("done", stuck.getResult());

        Dialog<ButtonType> single = new Dialog<ButtonType>();
        single.initModality(javafx.stage.Modality.NONE);
        single.getDialogPane().getButtonTypes().add(ButtonType.OK);
        single.show();
        single.close();
        assertFalse("a single button lets it close", single.isShowing());
        assertNull(single.getResult());
    }

    @Test
    public void showAndWaitReturnsTheResultOnceTheDialogClosed() {
        final Alert alert = new Alert(AlertType.CONFIRMATION, "Sure?");
        final List<String> log = new ArrayList<String>();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                log.add("showing=" + alert.isShowing());
                ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).fire();
            }
        });
        java.util.Optional<ButtonType> answer = alert.showAndWait();
        assertEquals("[showing=true]", log.toString());
        assertTrue(answer.isPresent());
        assertSame(ButtonType.OK, answer.get());
        assertFalse(alert.isShowing());

        final Alert closed = new Alert(AlertType.NONE, "Nothing to choose");
        closed.getButtonTypes().add(new ButtonType("Fine"));
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                closed.close();
            }
        });
        assertFalse(closed.showAndWait().isPresent());
    }

    @Test
    public void inputDialogsAnswerWithWhatWasEntered() {
        TextInputDialog text = new TextInputDialog("abc");
        assertEquals("abc", text.getDefaultValue());
        assertEquals(2, text.getDialogPane().getButtonTypes().size());
        text.setContentText("Name:");
        ((Button) text.getDialogPane().lookupButton(ButtonType.OK)).fire();
        assertEquals("abc", text.getResult());

        TextInputDialog cancelled = new TextInputDialog("abc");
        ((Button) cancelled.getDialogPane().lookupButton(ButtonType.CANCEL)).fire();
        assertNull(cancelled.getResult());

        ChoiceDialog<String> choice = new ChoiceDialog<String>("b", "a", "b", "c");
        assertEquals("[a, b, c]", choice.getItems().toString());
        assertEquals("b", choice.getDefaultChoice());
        assertEquals("b", choice.getSelectedItem());
        choice.setSelectedItem("c");
        assertEquals("c", choice.selectedItemProperty().get());
        ((Button) choice.getDialogPane().lookupButton(ButtonType.OK)).fire();
        assertEquals("c", choice.getResult());

        ChoiceDialog<String> first = new ChoiceDialog<String>("missing", "a", "b");
        assertNull(first.getDefaultChoice());
        assertEquals("a", first.getSelectedItem());
        first.close();
        assertNull(first.getResult());
    }

    @Test
    public void datePickerHoldsALocalDate() {
        DatePicker picker = new DatePicker();
        Count actions = new Count();
        picker.setOnAction(actions);
        picker.setPromptText("when");
        sceneOf(picker, 120, 30);
        Picker peer = (Picker) picker.cn1Native();
        assertEquals("when", peer.getText());
        assertNull(peer.getDate());

        picker.setValue(LocalDate.of(2024, 2, 29));
        assertEquals(1, actions.n);
        Calendar c = Calendar.getInstance();
        c.setTime(peer.getDate());
        assertEquals(2024, c.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, c.get(Calendar.MONTH));
        assertEquals(29, c.get(Calendar.DAY_OF_MONTH));

        // What the picker does when the user chose a day.
        c.set(2025, Calendar.DECEMBER, 31);
        peer.setDate(c.getTime());
        commit(peer);
        assertEquals(LocalDate.of(2025, 12, 31), picker.getValue());
        assertEquals(2, actions.n);

        assertEquals(LocalDate.of(2020, 1, 2), new DatePicker(LocalDate.of(2020, 1, 2)).getValue());
    }
}
