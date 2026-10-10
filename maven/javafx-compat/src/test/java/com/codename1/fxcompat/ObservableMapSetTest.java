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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

public class ObservableMapSetTest {

    private final List<String> log = new ArrayList<String>();

    private final MapChangeListener<String, Integer> mapProbe = new MapChangeListener<String, Integer>() {
        @Override
        public void onChanged(Change<? extends String, ? extends Integer> c) {
            log.add(c.getKey() + (c.wasRemoved() ? " -" + c.getValueRemoved() : "")
                    + (c.wasAdded() ? " +" + c.getValueAdded() : ""));
        }
    };

    private final SetChangeListener<String> setProbe = new SetChangeListener<String>() {
        @Override
        public void onChanged(Change<? extends String> c) {
            log.add(c.wasAdded() ? "+" + c.getElementAdded() : "-" + c.getElementRemoved());
        }
    };

    private String take() {
        String result = log.toString();
        log.clear();
        return result;
    }

    @Test
    public void mapReportsPutReplaceAndRemove() {
        ObservableMap<String, Integer> map = FXCollections.observableMap(new LinkedHashMap<String, Integer>());
        map.addListener(mapProbe);
        assertNull(map.put("a", Integer.valueOf(1)));
        assertEquals("[a +1]", take());
        assertEquals(Integer.valueOf(1), map.put("a", Integer.valueOf(2)));
        assertEquals("[a -1 +2]", take());
        map.put("a", Integer.valueOf(2));
        assertEquals("putting an equal value reports nothing", "[]", take());
        assertEquals(Integer.valueOf(2), map.remove("a"));
        assertEquals("[a -2]", take());
        assertNull(map.remove("a"));
        assertEquals("[]", take());
        Map<String, Integer> more = new LinkedHashMap<String, Integer>();
        more.put("x", Integer.valueOf(1));
        more.put("y", Integer.valueOf(2));
        map.putAll(more);
        assertEquals("[x +1, y +2]", take());
        map.clear();
        assertEquals("[x -1, y -2]", take());
        assertTrue(map.isEmpty());
    }

    @Test
    public void mapViewsReportRemovals() {
        ObservableMap<String, Integer> map = FXCollections.observableMap(new LinkedHashMap<String, Integer>());
        map.put("a", Integer.valueOf(1));
        map.put("b", Integer.valueOf(2));
        map.put("c", Integer.valueOf(3));
        map.addListener(mapProbe);
        assertTrue(map.keySet().remove("a"));
        assertEquals("[a -1]", take());
        assertTrue(map.values().remove(Integer.valueOf(2)));
        assertEquals("[b -2]", take());
        Iterator<Map.Entry<String, Integer>> entries = map.entrySet().iterator();
        Map.Entry<String, Integer> entry = entries.next();
        entry.setValue(Integer.valueOf(30));
        assertEquals("[c -3 +30]", take());
        entries.remove();
        assertEquals("[c -30]", take());
        assertEquals(0, map.size());
    }

    @Test
    public void mapInvalidationAndListenerRemoval() {
        ObservableMap<String, Integer> map = FXCollections.observableHashMap();
        Probe.Invalidations invalidations = new Probe.Invalidations();
        map.addListener(invalidations);
        map.addListener(mapProbe);
        map.put("a", Integer.valueOf(1));
        assertEquals(1, invalidations.count);
        assertSame(map, invalidations.last);
        map.removeListener(mapProbe);
        map.removeListener(invalidations);
        map.put("b", Integer.valueOf(2));
        assertEquals(1, invalidations.count);
        assertEquals("[a +1]", take());
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(Integer.valueOf(1)));
    }

    @Test
    public void unmodifiableAndEmptyMaps() {
        ObservableMap<String, Integer> map = FXCollections.observableMap(new HashMap<String, Integer>());
        ObservableMap<String, Integer> view = FXCollections.unmodifiableObservableMap(map);
        view.addListener(mapProbe);
        map.put("a", Integer.valueOf(1));
        assertEquals("[a +1]", take());
        assertEquals(Integer.valueOf(1), view.get("a"));
        try {
            view.put("b", Integer.valueOf(2));
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, view.size());
        }
        ObservableMap<String, Integer> empty = FXCollections.emptyObservableMap();
        assertTrue(empty.isEmpty());
        try {
            empty.put("a", Integer.valueOf(1));
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(empty.isEmpty());
        }
    }

    @Test
    public void setReportsAddAndRemove() {
        ObservableSet<String> set = FXCollections.observableSet(new LinkedHashSet<String>());
        set.addListener(setProbe);
        assertTrue(set.add("a"));
        assertEquals("[+a]", take());
        assertFalse(set.add("a"));
        assertEquals("[]", take());
        List<String> more = new ArrayList<String>();
        more.add("a");
        more.add("b");
        more.add("c");
        assertTrue(set.addAll(more));
        assertEquals("[+b, +c]", take());
        assertTrue(set.remove("b"));
        assertEquals("[-b]", take());
        assertFalse(set.remove("b"));
        List<String> keep = new ArrayList<String>();
        keep.add("c");
        assertTrue(set.retainAll(keep));
        assertEquals("[-a]", take());
        set.add("d");
        take();
        Iterator<String> it = set.iterator();
        it.next();
        it.remove();
        assertEquals("[-c]", take());
        set.clear();
        assertEquals("[-d]", take());
        assertTrue(set.isEmpty());
    }

    @Test
    public void setFactoriesAndViews() {
        ObservableSet<String> set = FXCollections.observableSet("a", "b");
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        ObservableSet<String> view = FXCollections.unmodifiableObservableSet(set);
        view.addListener(setProbe);
        Probe.Invalidations invalidations = new Probe.Invalidations();
        set.addListener(invalidations);
        set.add("c");
        assertEquals("[+c]", take());
        assertEquals(1, invalidations.count);
        try {
            view.add("d");
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertEquals(3, view.size());
        }
        ObservableSet<String> empty = FXCollections.emptyObservableSet();
        assertTrue(empty.isEmpty());
        set.removeListener(invalidations);
        set.add("e");
        assertEquals(1, invalidations.count);
    }
}
