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
package com.example.fxgallery.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A row of the table: a JavaFX bean with one property per column.
 */
public class Person {

    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final StringProperty city = new SimpleStringProperty(this, "city");
    private final IntegerProperty age = new SimpleIntegerProperty(this, "age");
    private final BooleanProperty member = new SimpleBooleanProperty(this, "member");

    public Person(String name, String city, int age, boolean member) {
        setName(name);
        setCity(city);
        setAge(age);
        setMember(member);
    }

    public final String getName() {
        return name.get();
    }

    public final void setName(String value) {
        name.set(value);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public final String getCity() {
        return city.get();
    }

    public final void setCity(String value) {
        city.set(value);
    }

    public StringProperty cityProperty() {
        return city;
    }

    public final int getAge() {
        return age.get();
    }

    public final void setAge(int value) {
        age.set(value);
    }

    public IntegerProperty ageProperty() {
        return age;
    }

    public final boolean isMember() {
        return member.get();
    }

    public final void setMember(boolean value) {
        member.set(value);
    }

    public BooleanProperty memberProperty() {
        return member;
    }

    @Override
    public String toString() {
        return getName() + " (" + getAge() + ")";
    }
}
