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

import org.junit.Test;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/// `map`, `flatMap` and `orElse` observe their source only while observed.
public class DerivedValueTest {

    /// A property that counts the listeners on it.
    private static final class Counted extends SimpleStringProperty {
        int listeners;

        Counted(String v) {
            super(v);
        }

        @Override
        public void addListener(InvalidationListener l) {
            listeners++;
            super.addListener(l);
        }

        @Override
        public void removeListener(InvalidationListener l) {
            listeners--;
            super.removeListener(l);
        }
    }

    @Test
    public void unobservedMapAddsNothingToTheSource() {
        Counted source = new Counted("a");
        for (int i = 0; i < 10; i++) {
            ObservableValue<Integer> length = source.map(String::length);
            assertEquals(Integer.valueOf(1), length.getValue());
        }
        assertEquals(0, source.listeners);
    }

    @Test
    public void observedMapFollowsAndLetsGo() {
        Counted source = new Counted("a");
        ObservableValue<Integer> length = source.map(String::length);
        final int[] seen = new int[1];
        ChangeListener<Integer> l = (obs, o, n) -> seen[0] = n;
        length.addListener(l);
        assertEquals(1, source.listeners);
        source.set("abc");
        assertEquals(3, seen[0]);
        length.removeListener(l);
        assertEquals(0, source.listeners);
    }

    @Test
    public void flatMapReadsTheInnerValueWhileUnobserved() {
        final SimpleStringProperty inner = new SimpleStringProperty("x");
        Counted source = new Counted("a");
        ObservableValue<String> flat = source.flatMap(s -> inner);
        assertEquals("x", flat.getValue());
        inner.set("y");
        assertEquals("y", flat.getValue());
        assertEquals(0, source.listeners);
        InvalidationListener l = (Observable o) -> { };
        flat.addListener(l);
        assertEquals(1, source.listeners);
        flat.removeListener(l);
        assertEquals(0, source.listeners);
    }
}
