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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.PropertyAccess;
import com.codename1.fxcompat.runtime.Units;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;

/// `PropertyValueFactory` over an installed accessor -- the build
/// generates the application's -- and the check box cell.
public class PropertyValueFactoryTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// A row of the table.
    public static final class Person {
        final StringProperty name = new SimpleStringProperty("Ann");
        final BooleanProperty active = new SimpleBooleanProperty(false);
    }

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
        // What the build generates for a class like Person.
        PropertyAccess.install(new PropertyAccess() {
            @Override
            public Object call(Object bean, String method) {
                if (bean instanceof Person) {
                    Person p = (Person) bean;
                    if ("nameProperty".equals(method)) {
                        return p.name;
                    }
                    if ("getName".equals(method)) {
                        return p.name.get();
                    }
                    if ("getAge".equals(method)) {
                        return Integer.valueOf(41);
                    }
                    if ("isActive".equals(method)) {
                        return Boolean.valueOf(p.active.get());
                    }
                    if ("getNothing".equals(method)) {
                        return null;
                    }
                }
                return NONE;
            }

            @Override
            public boolean knows(Object bean) {
                return bean instanceof Person;
            }
        });
    }

    @After
    public void tearDown() {
        PropertyAccess.install(null);
        Units.setScale(0);
    }

    private static <T> ObservableValue<T> value(String property, Object row) {
        TableColumn<Object, T> column = new TableColumn<Object, T>(property);
        column.setCellValueFactory(new PropertyValueFactory<Object, T>(property));
        return column.getCellValueFactory().call(new TableColumn.CellDataFeatures<Object, T>(null, column, row));
    }

    @Test
    public void aPropertyMethodIsPreferredAndAGetterIsWrapped() {
        Person ann = new Person();
        assertSame("the cell follows the property itself", ann.name, value("name", ann));
        ObservableValue<Integer> age = value("age", ann);
        assertEquals(Integer.valueOf(41), age.getValue());
        ObservableValue<Boolean> active = value("active", ann);
        assertEquals(Boolean.FALSE, active.getValue());
        ObservableValue<Object> nothing = value("nothing", ann);
        assertNotNull("a getter that returns null is still a property", nothing);
        assertNull(nothing.getValue());
        assertEquals("name", new PropertyValueFactory<Object, String>("name").getProperty());
    }

    @Test
    public void anUnknownPropertyIsAnErrorThatNamesTheClassAndTheProperty() {
        try {
            value("nmae", new Person());
            fail("Person has no such property");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(Person.class.getName()));
            assertTrue(e.getMessage(), e.getMessage().contains("nmaeProperty()"));
        }
        try {
            value("length", "a string");
            fail("Nothing is generated for String");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("java.lang.String"));
            assertTrue(e.getMessage(), e.getMessage().contains("\"length\""));
            assertTrue(e.getMessage(), e.getMessage().contains("lambda"));
        }
    }

    @Test
    public void aCheckBoxCellWritesThePropertyItShows() {
        Person ann = new Person();
        TableView<Person> table = new TableView<Person>(FXCollections.observableArrayList(ann));
        table.setEditable(true);
        TableColumn<Person, Boolean> column = new TableColumn<Person, Boolean>("Active");
        column.setCellValueFactory(f -> f.getValue().active);
        final List<CheckBoxTableCell<Person, Boolean>> cells = new ArrayList<CheckBoxTableCell<Person, Boolean>>();
        column.setCellFactory(c -> {
            CheckBoxTableCell<Person, Boolean> cell = new CheckBoxTableCell<Person, Boolean>();
            cells.add(cell);
            return cell;
        });
        table.getColumns().add(column);
        Pane root = new Pane();
        root.getChildren().add(table);
        table.setPrefSize(200, 120);
        Scene scene = new Scene(root, 200, 120);
        scene.cn1Layout(200, 120);

        CheckBox box = null;
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).getIndex() == 0 && cells.get(i).getGraphic() instanceof CheckBox) {
                box = (CheckBox) cells.get(i).getGraphic();
            }
        }
        assertNotNull("the row shows a check box", box);
        assertFalse(box.isSelected());
        assertFalse(box.isDisabled());
        box.setSelected(true);
        assertTrue("the box writes the property", ann.active.get());
        ann.active.set(false);
        assertFalse("and follows it", box.isSelected());
        assertNotNull(CheckBoxTableCell.forTableColumn(column).call(column));
    }
}
