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
package com.example.fxgallery.fxml;

import java.net.URL;
import java.util.ResourceBundle;

import com.example.fxgallery.control.StatusBadge;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

/**
 * The controller of {@code profile.fxml}.
 */
public class ProfileController {

    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> roleBox;
    @FXML
    private CheckBox activeBox;
    @FXML
    private ToggleGroup planGroup;
    @FXML
    private Button greetButton;
    @FXML
    private Label greeting;
    @FXML
    private StatusBadge badge;
    @FXML
    private AddressController addressController;

    private int greetings;

    @FXML
    private void initialize() {
        roleBox.getItems().addAll("Developer", "Designer", "Manager");
        roleBox.getSelectionModel().selectFirst();
        greetButton.disableProperty().bind(nameField.textProperty().isEmpty());
        activeBox.selectedProperty().addListener((observable, was, is) -> badge.setLevel(is ? 2 : 0));
        greeting.setText(resources.getString("profile.hint"));
    }

    @FXML
    private void onGreet(ActionEvent event) {
        greetings++;
        String plan = planGroup.getSelectedToggle() == null ? "?" : String.valueOf(planGroup.getSelectedToggle().getUserData());
        greeting.setText(resources.getString("profile.hello") + " " + nameField.getText()
                + " (" + roleBox.getValue() + ", " + plan + ") from " + addressController.getCity());
        badge.setText(resources.getString("badge.greeted") + " " + greetings);
        badge.setLevel(1);
    }

    @FXML
    private void onReset() {
        nameField.clear();
        addressController.clear();
        greeting.setText(resources.getString("profile.hint"));
        badge.setText(resources.getString("badge.ready"));
        badge.setLevel(activeBox.isSelected() ? 2 : 0);
    }

    public int getGreetings() {
        return greetings;
    }

    public URL getLocation() {
        return location;
    }
}
