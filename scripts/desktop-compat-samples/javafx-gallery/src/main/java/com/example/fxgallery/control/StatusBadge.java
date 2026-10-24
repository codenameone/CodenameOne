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
package com.example.fxgallery.control;

import java.io.IOException;
import java.io.UncheckedIOException;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * A custom control: a coloured dot and a caption, laid out by {@code status-badge.fxml} and usable
 * as an element of other FXML documents.
 */
public class StatusBadge extends HBox {

    private static final Color[] COLOURS = {Color.GRAY, Color.ORANGE, Color.LIMEGREEN};

    @FXML
    private Circle dot;
    @FXML
    private Label caption;

    private final IntegerProperty level = new SimpleIntegerProperty(this, "level", 0);

    public StatusBadge() {
        FXMLLoader loader = new FXMLLoader(StatusBadge.class.getResource("status-badge.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        level.addListener((observable, before, now) -> updateDot());
        updateDot();
    }

    private void updateDot() {
        int index = Math.max(0, Math.min(COLOURS.length - 1, getLevel()));
        dot.setFill(COLOURS[index]);
    }

    public final String getText() {
        return caption.getText();
    }

    public final void setText(String text) {
        caption.setText(text);
    }

    public final StringProperty textProperty() {
        return caption.textProperty();
    }

    public final int getLevel() {
        return level.get();
    }

    public final void setLevel(int value) {
        level.set(value);
    }

    public final IntegerProperty levelProperty() {
        return level;
    }
}
