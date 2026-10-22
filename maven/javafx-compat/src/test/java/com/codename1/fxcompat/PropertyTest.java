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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;

public class PropertyTest {

    @Test
    public void defaultsAndInitialValues() {
        assertEquals(0, new SimpleIntegerProperty().get());
        assertEquals(0L, new SimpleLongProperty().get());
        assertEquals(0f, new SimpleFloatProperty().get(), 0f);
        assertEquals(0d, new SimpleDoubleProperty().get(), 0d);
        assertFalse(new SimpleBooleanProperty().get());
        assertNull(new SimpleStringProperty().get());
        assertNull(new SimpleObjectProperty<Object>().get());
        assertEquals(7, new SimpleIntegerProperty(7).get());
        assertEquals(7L, new SimpleLongProperty(7L).get());
        assertEquals(1.5f, new SimpleFloatProperty(1.5f).get(), 0f);
        assertEquals(2.5, new SimpleDoubleProperty(2.5).get(), 0d);
        assertTrue(new SimpleBooleanProperty(true).get());
        assertEquals("x", new SimpleStringProperty("x").get());
        assertEquals("o", new SimpleObjectProperty<String>("o").get());
    }

    @Test
    public void beanAndName() {
        Object bean = new Object();
        IntegerProperty p = new SimpleIntegerProperty(bean, "width", 3);
        assertSame(bean, p.getBean());
        assertEquals("width", p.getName());
        assertEquals(3, p.get());
        IntegerProperty anonymous = new SimpleIntegerProperty();
        assertNull(anonymous.getBean());
        assertEquals("", anonymous.getName());
        assertEquals("", new SimpleStringProperty(bean, null).getName());
        assertEquals("s", new SimpleStringProperty(bean, "s", "v").getName());
        assertEquals("d", new SimpleDoubleProperty(bean, "d").getName());
    }

    @Test
    public void typedValueAccessors() {
        IntegerProperty i = new SimpleIntegerProperty(5);
        assertEquals(Integer.valueOf(5), i.getValue());
        assertEquals(5L, i.longValue());
        assertEquals(5f, i.floatValue(), 0f);
        assertEquals(5d, i.doubleValue(), 0d);
        assertEquals(5, i.intValue());
        i.setValue(9);
        assertEquals(9, i.get());
        DoubleProperty d = new SimpleDoubleProperty(2.75);
        assertEquals(2, d.intValue());
        assertEquals(2L, d.longValue());
        assertEquals(Double.valueOf(2.75), d.getValue());
        d.setValue(4);
        assertEquals(4.0, d.get(), 0d);
        LongProperty l = new SimpleLongProperty(1L << 40);
        assertEquals(Long.valueOf(1L << 40), l.getValue());
        FloatProperty f = new SimpleFloatProperty(0.5f);
        assertEquals(Float.valueOf(0.5f), f.getValue());
        BooleanProperty b = new SimpleBooleanProperty();
        b.setValue(Boolean.TRUE);
        assertTrue(b.get());
        assertEquals(Boolean.TRUE, b.getValue());
        StringProperty s = new SimpleStringProperty();
        s.setValue("hello");
        assertEquals("hello", s.getValue());
    }

    @Test
    public void invalidationIsLazy() {
        IntegerProperty p = new SimpleIntegerProperty(1);
        Probe.Invalidations probe = new Probe.Invalidations();
        p.addListener(probe);
        p.set(2);
        assertEquals(1, probe.count);
        assertSame(p, probe.last);
        p.set(3);
        p.set(4);
        assertEquals("no second invalidation until the value is read", 1, probe.count);
        assertEquals(4, p.get());
        p.set(5);
        assertEquals(2, probe.count);
        p.get();
        p.set(5);
        assertEquals("setting the same value does not invalidate", 2, probe.count);
    }

    @Test
    public void changeListenerMakesThePropertyEager() {
        IntegerProperty p = new SimpleIntegerProperty(1);
        Probe.Invalidations invalidations = new Probe.Invalidations();
        Probe.Values<Number> changes = new Probe.Values<Number>();
        p.addListener(invalidations);
        p.addListener(changes);
        p.set(2);
        p.set(3);
        p.set(3);
        assertEquals(2, invalidations.count);
        assertEquals("[1->2, 2->3]", changes.log.toString());
    }

    @Test
    public void listenersRunInRegistrationOrder() {
        final List<String> order = new ArrayList<String>();
        StringProperty p = new SimpleStringProperty("a");
        p.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                order.add("inv1");
            }
        });
        p.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> o, String oldValue, String newValue) {
                order.add("chg1");
            }
        });
        p.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                order.add("inv2");
            }
        });
        p.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> o, String oldValue, String newValue) {
                order.add("chg2");
            }
        });
        p.set("b");
        assertEquals("[inv1, inv2, chg1, chg2]", order.toString());
    }

    @Test
    public void removedListenersAreNotCalled() {
        DoubleProperty p = new SimpleDoubleProperty();
        Probe.Invalidations invalidations = new Probe.Invalidations();
        Probe.Values<Number> changes = new Probe.Values<Number>();
        p.addListener(invalidations);
        p.addListener(changes);
        p.set(1);
        p.removeListener(invalidations);
        p.removeListener(changes);
        p.set(2);
        assertEquals(1, invalidations.count);
        assertEquals(1, changes.log.size());
        p.removeListener(invalidations);
        p.removeListener(changes);
    }

    @Test
    public void listenerMayRemoveItselfWhileNotified() {
        final IntegerProperty p = new SimpleIntegerProperty();
        final int[] calls = new int[2];
        p.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                calls[0]++;
                p.removeListener(this);
            }
        });
        p.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                calls[1]++;
            }
        });
        p.set(1);
        p.get();
        p.set(2);
        assertEquals(1, calls[0]);
        assertEquals(2, calls[1]);
    }

    @Test
    public void changeListenerSeesNestedChangeInOrder() {
        final IntegerProperty p = new SimpleIntegerProperty(0);
        final Probe.Values<Number> changes = new Probe.Values<Number>();
        p.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> o, Number oldValue, Number newValue) {
                if (newValue.intValue() == 1) {
                    p.set(2);
                }
            }
        });
        p.addListener(changes);
        p.set(1);
        assertEquals(2, p.get());
        assertEquals("the nested change is delivered first, as in JavaFX", "[1->2, 0->1]",
                changes.log.toString());
    }

    @Test
    public void bindFollowsTheSource() {
        IntegerProperty source = new SimpleIntegerProperty(1);
        IntegerProperty target = new SimpleIntegerProperty(9);
        Probe.Invalidations probe = new Probe.Invalidations();
        target.addListener(probe);
        assertFalse(target.isBound());
        target.bind(source);
        assertTrue(target.isBound());
        assertEquals(1, probe.count);
        assertEquals(1, target.get());
        source.set(2);
        assertEquals(2, probe.count);
        source.set(3);
        assertEquals("lazy while unread", 2, probe.count);
        assertEquals(3, target.get());
        target.unbind();
        assertFalse(target.isBound());
        source.set(4);
        assertEquals("keeps the last bound value", 3, target.get());
        target.set(10);
        assertEquals(10, target.get());
        assertEquals(4, source.get());
    }

    @Test
    public void bindingTwiceToTheSameSourceIsANoOp() {
        StringProperty source = new SimpleStringProperty("a");
        StringProperty target = new SimpleStringProperty();
        target.bind(source);
        target.get();
        Probe.Invalidations probe = new Probe.Invalidations();
        target.addListener(probe);
        target.bind(source);
        assertEquals(0, probe.count);
        StringProperty other = new SimpleStringProperty("b");
        target.bind(other);
        assertEquals("b", target.get());
        source.set("c");
        assertEquals("the first source was released", "b", target.get());
    }

    @Test
    public void aBoundValueCannotBeSet() {
        Object bean = "owner";
        IntegerProperty source = new SimpleIntegerProperty(1);
        IntegerProperty i = new SimpleIntegerProperty(bean, "count");
        i.bind(source);
        try {
            i.set(5);
            fail("expected a RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().endsWith("A bound value cannot be set."));
            assertTrue(expected.getMessage(), expected.getMessage().indexOf("count") >= 0);
        }
        try {
            i.setValue(6);
            fail("expected a RuntimeException");
        } catch (RuntimeException expected) {
            assertEquals(1, i.get());
        }
        StringProperty s = new SimpleStringProperty();
        s.bind(new SimpleStringProperty("x"));
        try {
            s.set("y");
            fail("expected a RuntimeException");
        } catch (RuntimeException expected) {
            assertEquals("A bound value cannot be set.", expected.getMessage());
        }
        BooleanProperty b = new SimpleBooleanProperty();
        b.bind(new SimpleBooleanProperty(true));
        DoubleProperty d = new SimpleDoubleProperty();
        d.bind(source);
        LongProperty l = new SimpleLongProperty();
        l.bind(source);
        FloatProperty f = new SimpleFloatProperty();
        f.bind(source);
        ObjectProperty<String> o = new SimpleObjectProperty<String>();
        o.bind(new SimpleObjectProperty<String>("v"));
        int thrown = 0;
        try {
            b.set(false);
        } catch (RuntimeException expected) {
            thrown++;
        }
        try {
            d.set(2);
        } catch (RuntimeException expected) {
            thrown++;
        }
        try {
            l.set(2);
        } catch (RuntimeException expected) {
            thrown++;
        }
        try {
            f.set(2);
        } catch (RuntimeException expected) {
            thrown++;
        }
        try {
            o.set("w");
        } catch (RuntimeException expected) {
            thrown++;
        }
        assertEquals(5, thrown);
        assertTrue(b.get());
        assertEquals(1.0, d.get(), 0d);
        assertEquals(1L, l.get());
        assertEquals(1f, f.get(), 0f);
        assertEquals("v", o.get());
    }

    @Test(expected = NullPointerException.class)
    public void bindToNullThrows() {
        new SimpleIntegerProperty().bind(null);
    }

    @Test
    public void numericPropertyBindsToAnyNumber() {
        DoubleProperty d = new SimpleDoubleProperty();
        IntegerProperty i = new SimpleIntegerProperty(4);
        d.bind(i);
        assertEquals(4.0, d.get(), 0d);
        i.set(6);
        assertEquals(6.0, d.get(), 0d);
        IntegerProperty truncated = new SimpleIntegerProperty();
        DoubleProperty source = new SimpleDoubleProperty(2.9);
        truncated.bind(source);
        assertEquals(2, truncated.get());
    }

    @Test
    public void aBoundPropertyIsNotKeptAliveByItsSource() {
        IntegerProperty source = new SimpleIntegerProperty(1);
        IntegerProperty target = new SimpleIntegerProperty();
        target.bind(source);
        assertEquals(1, target.get());
        WeakReference ref = new WeakReference(target);
        target = null;
        assertTrue("the bound property stayed reachable from its source", Probe.collected(ref));
        source.set(2);
        source.set(3);
        assertEquals(3, source.get());
    }

    @Test
    public void bidirectionalBindingCopiesBothWays() {
        IntegerProperty a = new SimpleIntegerProperty(1);
        IntegerProperty b = new SimpleIntegerProperty(2);
        a.bindBidirectional(b);
        assertEquals("the first takes the value of the second", 2, a.get());
        a.set(5);
        assertEquals(5, b.get());
        b.set(7);
        assertEquals(7, a.get());
        a.unbindBidirectional(b);
        a.set(8);
        assertEquals(7, b.get());
        b.set(9);
        assertEquals(8, a.get());
    }

    @Test
    public void bidirectionalUnbindWorksFromEitherSide() {
        StringProperty a = new SimpleStringProperty("a");
        StringProperty b = new SimpleStringProperty("b");
        a.bindBidirectional(b);
        b.unbindBidirectional(a);
        a.set("x");
        assertEquals("b", b.get());
        ObjectProperty<String> c = new SimpleObjectProperty<String>("c");
        ObjectProperty<String> d = new SimpleObjectProperty<String>("d");
        c.bindBidirectional(d);
        c.bindBidirectional(d);
        d.set("e");
        assertEquals("e", c.get());
        c.unbindBidirectional(d);
        d.set("f");
        assertEquals("binding twice needs unbinding twice", "f", c.get());
        c.unbindBidirectional(d);
        d.set("g");
        assertEquals("f", c.get());
    }

    @Test
    public void bidirectionalBindingDoesNotKeepEitherSideAlive() {
        DoubleProperty a = new SimpleDoubleProperty(1);
        DoubleProperty b = new SimpleDoubleProperty(2);
        a.bindBidirectional(b);
        WeakReference ref = new WeakReference(b);
        b = null;
        assertTrue("the partner stayed reachable through the binding", Probe.collected(ref));
        a.set(3);
        a.set(4);
        assertEquals(4.0, a.get(), 0d);
    }

    @Test
    public void bidirectionalBindingToABoundPropertyRestoresTheValue() {
        IntegerProperty a = new SimpleIntegerProperty(1);
        IntegerProperty b = new SimpleIntegerProperty(1);
        IntegerProperty master = new SimpleIntegerProperty(1);
        a.bindBidirectional(b);
        b.bind(master);
        try {
            a.set(5);
            fail("expected a RuntimeException");
        } catch (RuntimeException expected) {
            assertEquals("the change that could not be copied is undone", 1, a.get());
        }
    }

    @Test
    public void chainOfBidirectionalBindings() {
        BooleanProperty a = new SimpleBooleanProperty();
        BooleanProperty b = new SimpleBooleanProperty();
        BooleanProperty c = new SimpleBooleanProperty();
        a.bindBidirectional(b);
        b.bindBidirectional(c);
        a.set(true);
        assertTrue(b.get());
        assertTrue(c.get());
        c.set(false);
        assertFalse(a.get());
    }

    @Test
    public void weakInvalidationListenerForwardsAndReleases() {
        IntegerProperty p = new SimpleIntegerProperty();
        Probe.Invalidations probe = new Probe.Invalidations();
        WeakInvalidationListener weak = new WeakInvalidationListener(probe);
        p.addListener(weak);
        p.set(1);
        assertEquals(1, probe.count);
        assertFalse(weak.wasGarbageCollected());
        WeakReference ref = new WeakReference(probe);
        probe = null;
        assertTrue(Probe.collected(ref));
        assertTrue(weak.wasGarbageCollected());
        p.get();
        p.set(2);
        p.get();
        p.set(3);
        assertEquals(3, p.get());
    }

    @Test
    public void weakChangeListenerForwards() {
        StringProperty p = new SimpleStringProperty("a");
        Probe.Values<String> probe = new Probe.Values<String>();
        WeakChangeListener<String> weak = new WeakChangeListener<String>(probe);
        p.addListener(weak);
        p.set("b");
        assertEquals("[a->b]", probe.log.toString());
        assertFalse(weak.wasGarbageCollected());
    }

    @Test(expected = NullPointerException.class)
    public void weakListenerRejectsNull() {
        new WeakInvalidationListener(null);
    }

    @Test
    public void readOnlyWrapperExposesAReadOnlyView() {
        ReadOnlyIntegerWrapper wrapper = new ReadOnlyIntegerWrapper(this, "size", 1);
        ReadOnlyIntegerProperty view = wrapper.getReadOnlyProperty();
        assertSame(view, wrapper.getReadOnlyProperty());
        assertFalse(view instanceof IntegerProperty);
        assertEquals(1, view.get());
        assertSame(this, view.getBean());
        assertEquals("size", view.getName());
        Probe.Invalidations invalidations = new Probe.Invalidations();
        Probe.Values<Number> changes = new Probe.Values<Number>();
        view.addListener(invalidations);
        wrapper.set(2);
        assertEquals(1, invalidations.count);
        wrapper.set(3);
        assertEquals(1, invalidations.count);
        assertEquals(3, view.get());
        view.addListener(changes);
        wrapper.set(4);
        assertEquals("[3->4]", changes.log.toString());
    }

    @Test
    public void everyWrapperTypeHasAView() {
        ReadOnlyStringWrapper s = new ReadOnlyStringWrapper("a");
        ReadOnlyStringProperty view = s.getReadOnlyProperty();
        s.set("b");
        assertEquals("b", view.get());
        ReadOnlyBooleanWrapper b = new ReadOnlyBooleanWrapper(true);
        assertTrue(b.getReadOnlyProperty().get());
        ReadOnlyDoubleWrapper d = new ReadOnlyDoubleWrapper(1.5);
        assertEquals(1.5, d.getReadOnlyProperty().get(), 0d);
        ReadOnlyObjectWrapper<String> o = new ReadOnlyObjectWrapper<String>("x");
        assertEquals("x", o.getReadOnlyProperty().get());
        IntegerProperty source = new SimpleIntegerProperty(8);
        ReadOnlyIntegerWrapper bound = new ReadOnlyIntegerWrapper();
        bound.bind(source);
        assertEquals(8, bound.getReadOnlyProperty().get());
        Probe.Invalidations probe = new Probe.Invalidations();
        bound.getReadOnlyProperty().addListener(probe);
        source.set(9);
        assertEquals(1, probe.count);
        assertEquals(9, bound.getReadOnlyProperty().get());
    }

    @Test
    public void asObjectIsALiveTwoWayView() {
        IntegerProperty i = new SimpleIntegerProperty(1);
        ObjectProperty<Integer> boxed = i.asObject();
        assertEquals(Integer.valueOf(1), boxed.get());
        i.set(2);
        assertEquals(Integer.valueOf(2), boxed.get());
        boxed.set(Integer.valueOf(3));
        assertEquals(3, i.get());
        ObjectProperty<Double> source = new SimpleObjectProperty<Double>(Double.valueOf(1.5));
        DoubleProperty d = DoubleProperty.doubleProperty(source);
        assertEquals(1.5, d.get(), 0d);
        d.set(2.5);
        assertEquals(Double.valueOf(2.5), source.get());
        source.set(Double.valueOf(4));
        assertEquals(4.0, d.get(), 0d);
    }

    @Test
    public void toStringDescribesTheProperty() {
        assertEquals("IntegerProperty [value: 3]", new SimpleIntegerProperty(3).toString());
        String named = new SimpleStringProperty("bean", "title", "t").toString();
        assertTrue(named, named.startsWith("StringProperty ["));
        assertTrue(named, named.indexOf("bean: bean") >= 0);
        assertTrue(named, named.indexOf("name: title") >= 0);
        assertTrue(named, named.endsWith("value: t]"));
        IntegerProperty bound = new SimpleIntegerProperty();
        IntegerProperty source = new SimpleIntegerProperty(4);
        bound.bind(source);
        assertTrue(bound.toString(), bound.toString().indexOf("bound") >= 0);
    }

    @Test
    public void invalidatedHookRunsBeforeListeners() {
        final List<String> order = new ArrayList<String>();
        IntegerProperty p = new SimpleIntegerProperty() {
            @Override
            protected void invalidated() {
                order.add("hook");
            }
        };
        p.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                order.add("listener");
            }
        });
        p.set(1);
        p.set(2);
        assertEquals("[hook, listener]", order.toString());
    }
}
