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
package com.codename1.desktopcompat.java.beans;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// Covers the firing rules of the bound-property support: equal values are
/// not events, named listeners see only their property, and a proxy is
/// registered under the name it carries.
public class PropertyChangeSupportTest {

    private static final class Recorder implements PropertyChangeListener {
        final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();

        public void propertyChange(PropertyChangeEvent evt) {
            events.add(evt);
        }
    }

    private Object bean;

    private PropertyChangeSupport support;

    @Before
    public void setUp() {
        bean = new Object();
        support = new PropertyChangeSupport(bean);
    }

    @Test(expected = NullPointerException.class)
    public void nullSourceIsRejected() {
        new PropertyChangeSupport(null);
    }

    @Test
    public void unnamedListenerSeesEveryProperty() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener(r);
        support.firePropertyChange("a", "x", "y");
        support.firePropertyChange("b", 1, 2);
        assertEquals(2, r.events.size());
        PropertyChangeEvent e = r.events.get(0);
        assertSame(bean, e.getSource());
        assertEquals("a", e.getPropertyName());
        assertEquals("x", e.getOldValue());
        assertEquals("y", e.getNewValue());
        assertEquals(Integer.valueOf(2), r.events.get(1).getNewValue());
    }

    @Test
    public void namedListenerSeesOnlyItsProperty() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener("a", r);
        support.firePropertyChange("a", 1, 2);
        support.firePropertyChange("b", 1, 2);
        assertEquals(1, r.events.size());
        assertEquals("a", r.events.get(0).getPropertyName());
    }

    @Test
    public void noEventWhenOldAndNewAreEqual() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener(r);
        support.firePropertyChange("a", "same", new String("same"));
        support.firePropertyChange("a", 3, 3);
        support.firePropertyChange("a", true, true);
        support.fireIndexedPropertyChange("a", 0, 5, 5);
        assertTrue(r.events.isEmpty());
    }

    @Test
    public void nullOldOrNewStillFires() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener(r);
        support.firePropertyChange("a", null, "v");
        support.firePropertyChange("a", "v", null);
        support.firePropertyChange("a", null, null);
        assertEquals(3, r.events.size());
    }

    @Test
    public void primitiveOverloadsBoxTheirValues() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener(r);
        support.firePropertyChange("i", 1, 2);
        support.firePropertyChange("b", false, true);
        assertEquals(Integer.valueOf(1), r.events.get(0).getOldValue());
        assertEquals(Boolean.TRUE, r.events.get(1).getNewValue());
    }

    @Test
    public void indexedEventsCarryTheIndex() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener(r);
        support.fireIndexedPropertyChange("a", 4, "x", "y");
        support.fireIndexedPropertyChange("a", 5, 1, 2);
        support.fireIndexedPropertyChange("a", 6, false, true);
        assertEquals(3, r.events.size());
        assertEquals(4, ((IndexedPropertyChangeEvent) r.events.get(0)).getIndex());
        assertEquals(5, ((IndexedPropertyChangeEvent) r.events.get(1)).getIndex());
        assertEquals(6, ((IndexedPropertyChangeEvent) r.events.get(2)).getIndex());
    }

    @Test
    public void commonListenersFireBeforeNamedOnes() {
        final List<String> order = new ArrayList<String>();
        support.addPropertyChangeListener("a", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                order.add("named");
            }
        });
        support.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                order.add("common");
            }
        });
        support.firePropertyChange("a", 1, 2);
        assertEquals("[common, named]", order.toString());
    }

    @Test
    public void proxyRegistersUnderItsName() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener(new PropertyChangeListenerProxy("a", r));
        assertEquals(0, support.getPropertyChangeListeners("b").length);
        assertEquals(1, support.getPropertyChangeListeners("a").length);
        assertSame(r, support.getPropertyChangeListeners("a")[0]);
        support.firePropertyChange("a", 1, 2);
        support.firePropertyChange("b", 1, 2);
        assertEquals(1, r.events.size());
    }

    @Test
    public void allListenersIncludeProxiesForNamedOnes() {
        Recorder common = new Recorder();
        Recorder named = new Recorder();
        support.addPropertyChangeListener(common);
        support.addPropertyChangeListener("a", named);
        PropertyChangeListener[] all = support.getPropertyChangeListeners();
        assertEquals(2, all.length);
        assertSame(common, all[0]);
        assertTrue(all[1] instanceof PropertyChangeListenerProxy);
        PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) all[1];
        assertEquals("a", proxy.getPropertyName());
        assertSame(named, proxy.getListener());
    }

    @Test
    public void removeWorksWithAndWithoutAName() {
        Recorder common = new Recorder();
        Recorder named = new Recorder();
        support.addPropertyChangeListener(common);
        support.addPropertyChangeListener("a", named);
        support.removePropertyChangeListener(common);
        support.removePropertyChangeListener("a", named);
        support.firePropertyChange("a", 1, 2);
        assertTrue(common.events.isEmpty());
        assertTrue(named.events.isEmpty());
        assertEquals(0, support.getPropertyChangeListeners().length);
    }

    @Test
    public void removingAProxyRemovesTheNamedListener() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener("a", r);
        support.removePropertyChangeListener(new PropertyChangeListenerProxy("a", r));
        support.firePropertyChange("a", 1, 2);
        assertTrue(r.events.isEmpty());
    }

    @Test
    public void hasListenersReflectsCommonAndNamed() {
        assertFalse(support.hasListeners("a"));
        Recorder named = new Recorder();
        support.addPropertyChangeListener("a", named);
        assertTrue(support.hasListeners("a"));
        assertFalse(support.hasListeners("b"));
        assertFalse(support.hasListeners(null));
        support.addPropertyChangeListener(new Recorder());
        assertTrue(support.hasListeners("b"));
        assertTrue(support.hasListeners(null));
    }

    @Test
    public void listenerMayRemoveItselfWhileFiring() {
        final Recorder after = new Recorder();
        support.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                support.removePropertyChangeListener(this);
            }
        });
        support.addPropertyChangeListener(after);
        support.firePropertyChange("a", 1, 2);
        support.firePropertyChange("a", 2, 3);
        assertEquals(2, after.events.size());
    }

    @Test
    public void fireEventDeliversTheSameInstance() {
        Recorder r = new Recorder();
        support.addPropertyChangeListener(r);
        PropertyChangeEvent e = new PropertyChangeEvent(bean, "p", 1, 2);
        support.firePropertyChange(e);
        assertSame(e, r.events.get(0));
    }

    @Test
    public void nullListenersAndNamesAreIgnored() {
        support.addPropertyChangeListener((PropertyChangeListener) null);
        support.addPropertyChangeListener("a", null);
        support.addPropertyChangeListener(null, new Recorder());
        assertEquals(0, support.getPropertyChangeListeners().length);
        assertEquals(0, support.getPropertyChangeListeners("a").length);
        assertEquals(0, support.getPropertyChangeListeners(null).length);
    }
}
