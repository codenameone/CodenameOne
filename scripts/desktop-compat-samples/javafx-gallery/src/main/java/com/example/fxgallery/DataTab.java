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
package com.example.fxgallery;

import com.example.fxgallery.model.Person;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * A list with a cell factory and a sortable, filterable, editable table over the same people.
 */
public class DataTab extends BorderPane {

    private final ObservableList<Person> people = FXCollections.observableArrayList(
            new Person("Ada Lovelace", "London", 36, true),
            new Person("Grace Hopper", "New York", 85, true),
            new Person("Alan Turing", "Manchester", 41, false),
            new Person("Katherine Johnson", "Hampton", 101, true),
            new Person("Linus Torvalds", "Portland", 54, false));

    private final Label log = new Label("No changes yet");

    public DataTab() {
        TextField filter = new TextField();
        filter.setPromptText("Filter by name");
        HBox.setHgrow(filter, Priority.ALWAYS);

        FilteredList<Person> filtered = new FilteredList<>(people, person -> true);
        filter.textProperty().addListener((observable, before, now) -> {
            String needle = now == null ? "" : now.trim().toLowerCase();
            filtered.setPredicate(person -> needle.isEmpty() || person.getName().toLowerCase().contains(needle));
        });

        TableView<Person> table = createTable(filtered);
        ListView<Person> list = createList();

        Button add = new Button("Add");
        add.setOnAction(event -> people.add(new Person("Person " + (people.size() + 1), "Nowhere", 20, false)));
        Button remove = new Button("Remove selected");
        remove.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        remove.setOnAction(event -> people.remove(table.getSelectionModel().getSelectedItem()));

        Label count = new Label();
        count.textProperty().bind(Bindings.size(people).asString("%d people"));

        people.addListener((ListChangeListener<Person>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    log.setText("Added " + change.getAddedSubList().get(0).getName());
                } else if (change.wasRemoved()) {
                    log.setText("Removed " + change.getRemoved().get(0).getName());
                }
            }
        });

        HBox toolbar = new HBox(8, filter, add, remove, count);
        toolbar.setPadding(new Insets(8));

        SplitPane split = new SplitPane(list, table);
        split.setDividerPositions(0.3);

        setTop(toolbar);
        setCenter(split);
        setBottom(log);
        BorderPane.setMargin(log, new Insets(4, 8, 4, 8));
    }

    private ListView<Person> createList() {
        ListView<Person> list = new ListView<>(people);
        list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        list.setCellFactory(view -> new ListCell<Person>() {
            @Override
            protected void updateItem(Person person, boolean empty) {
                super.updateItem(person, empty);
                if (empty || person == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(person.getName());
                    setGraphic(new Circle(5, person.isMember() ? Color.SEAGREEN : Color.LIGHTGRAY));
                }
            }
        });

        MenuItem promote = new MenuItem("Toggle membership");
        promote.setOnAction(event -> {
            Person selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setMember(!selected.isMember());
                list.refresh();
            }
        });
        list.setContextMenu(new ContextMenu(promote));
        return list;
    }

    private TableView<Person> createTable(FilteredList<Person> filtered) {
        TableView<Person> table = new TableView<>();
        table.setEditable(true);
        table.setPlaceholder(new Label("Nobody matches"));

        TableColumn<Person, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(data -> data.getValue().nameProperty());
        name.setCellFactory(TextFieldTableCell.forTableColumn());
        name.setOnEditCommit(event -> event.getRowValue().setName(event.getNewValue()));
        name.setPrefWidth(180);

        TableColumn<Person, String> city = new TableColumn<>("City");
        city.setCellValueFactory(data -> data.getValue().cityProperty());
        city.setPrefWidth(120);

        TableColumn<Person, Number> age = new TableColumn<>("Age");
        age.setCellValueFactory(data -> data.getValue().ageProperty());
        age.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Person, Boolean> member = new TableColumn<>("Member");
        member.setCellValueFactory(data -> data.getValue().memberProperty());
        member.setSortable(false);

        table.getColumns().add(name);
        table.getColumns().add(city);
        table.getColumns().add(age);
        table.getColumns().add(member);

        SortedList<Person> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSortOrder().add(name);
        return table;
    }
}
