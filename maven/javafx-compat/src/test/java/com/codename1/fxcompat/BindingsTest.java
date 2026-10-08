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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.Test;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.util.converter.IntegerStringConverter;

public class BindingsTest {

    private final IntegerProperty i = new SimpleIntegerProperty(6);
    private final LongProperty l = new SimpleLongProperty(4L);
    private final FloatProperty f = new SimpleFloatProperty(1.5f);
    private final DoubleProperty d = new SimpleDoubleProperty(2.5);

    @Test
    public void customBindingIsLazyAndCaches() {
        final int[] computed = new int[1];
        final IntegerProperty source = new SimpleIntegerProperty(2);
        IntegerBinding binding = new IntegerBinding() {
            {
                bind(source);
            }

            @Override
            protected int computeValue() {
                computed[0]++;
                return source.get() * 10;
            }
        };
        assertFalse(binding.isValid());
        assertEquals(20, binding.get());
        assertEquals(20, binding.get());
        assertEquals("the value is cached while valid", 1, computed[0]);
        assertTrue(binding.isValid());
        Probe.Invalidations probe = new Probe.Invalidations();
        binding.addListener(probe);
        source.set(3);
        source.get();
        source.set(4);
        assertEquals("one invalidation until read", 1, probe.count);
        assertFalse(binding.isValid());
        assertEquals(1, computed[0]);
        assertEquals(40, binding.get());
        assertEquals(2, computed[0]);
        binding.invalidate();
        assertFalse(binding.isValid());
        assertEquals(2, probe.count);
    }

    @Test
    public void changeListenerOnABindingReportsOldAndNew() {
        NumberBinding sum = i.add(1);
        Probe.Values<Number> changes = new Probe.Values<Number>();
        sum.addListener(changes);
        i.set(7);
        i.set(7);
        i.set(8);
        assertEquals("[7->8, 8->9]", changes.log.toString());
    }

    @Test
    public void arithmeticResultHasTheWiderType() {
        assertTrue(Bindings.add(i, i) instanceof IntegerBinding);
        assertTrue(Bindings.add(i, l) instanceof LongBinding);
        assertTrue(Bindings.add(l, f) instanceof FloatBinding);
        assertTrue(Bindings.add(f, d) instanceof DoubleBinding);
        assertTrue(Bindings.add(i, 1) instanceof IntegerBinding);
        assertTrue(Bindings.add(i, 1L) instanceof LongBinding);
        assertTrue(Bindings.add(i, 1f) instanceof FloatBinding);
        assertTrue(Bindings.multiply(2L, i) instanceof LongBinding);
        assertTrue(Bindings.subtract(d, 1) instanceof DoubleBinding);
        assertTrue(i.multiply(l) instanceof LongBinding);
        assertTrue(l.divide(2) instanceof LongBinding);
        DoubleBinding widened = Bindings.add(i, 1.0);
        assertEquals(7.0, widened.get(), 0d);
        DoubleBinding fluent = d.add(i);
        assertEquals(8.5, fluent.get(), 0d);
    }

    @Test
    public void arithmeticValues() {
        assertEquals(10, Bindings.add(i, l).intValue());
        assertEquals(2, Bindings.subtract(i, l).intValue());
        assertEquals(24, Bindings.multiply(i, l).intValue());
        assertEquals(1, Bindings.divide(i, l).intValue());
        assertEquals("integer division truncates", 1L, Bindings.divide(i, l).longValue());
        assertEquals(2.4, Bindings.divide(i, d).doubleValue(), 1e-9);
        assertEquals(4.0, Bindings.add(f, d).doubleValue(), 1e-9);
        assertEquals(3.5, Bindings.subtract(i, d).doubleValue(), 1e-9);
        assertEquals(9.0f, Bindings.multiply(i, f).floatValue(), 0f);
        assertEquals(16, Bindings.add(10, i).intValue());
        assertEquals(4, Bindings.subtract(10, i).intValue());
        assertEquals(-4, Bindings.subtract(i, 10).intValue());
        assertEquals(60, Bindings.multiply(i, 10).intValue());
        assertEquals(3, Bindings.divide(i, 2).intValue());
        assertEquals(2, Bindings.divide(12, i).intValue());
        assertEquals(1.25, Bindings.divide(d, 2).doubleValue(), 1e-9);
        assertEquals(-6, Bindings.negate(i).intValue());
        assertEquals(-2.5, d.negate().get(), 0d);
        assertTrue(i.negate() instanceof IntegerBinding);
    }

    @Test
    public void arithmeticFollowsItsOperands() {
        NumberBinding expression = i.multiply(2).add(l).subtract(d);
        assertEquals(13.5, expression.doubleValue(), 1e-9);
        i.set(1);
        assertEquals(3.5, expression.doubleValue(), 1e-9);
        l.set(10);
        d.set(0.5);
        assertEquals(11.5, expression.doubleValue(), 1e-9);
        Probe.Invalidations probe = new Probe.Invalidations();
        expression.addListener(probe);
        i.set(2);
        assertEquals(1, probe.count);
    }

    @Test
    public void integerDivisionByZeroThrows() {
        IntegerProperty zero = new SimpleIntegerProperty(0);
        NumberBinding quotient = Bindings.divide(i, zero);
        try {
            quotient.intValue();
            fail("expected ArithmeticException");
        } catch (ArithmeticException expected) {
            assertTrue(true);
        }
        assertTrue(Double.isInfinite(Bindings.divide(d, zero).doubleValue()));
    }

    @Test
    public void minAndMax() {
        assertEquals(4, Bindings.min(i, l).intValue());
        assertEquals(6, Bindings.max(i, l).intValue());
        assertTrue(Bindings.max(i, l) instanceof LongBinding);
        assertEquals(2.5, Bindings.min(i, d).doubleValue(), 0d);
        assertEquals(6, Bindings.min(i, 100).intValue());
        assertEquals(100, Bindings.max(i, 100).intValue());
        assertEquals(1, Bindings.min(1, i).intValue());
        assertEquals(6.5, Bindings.max(6.5, i).doubleValue(), 0d);
        NumberBinding max = Bindings.max(i, l);
        l.set(50);
        assertEquals(50, max.intValue());
    }

    @Test
    public void numericEquality() {
        assertFalse(Bindings.equal(i, l).get());
        l.set(6);
        assertTrue(Bindings.equal(i, l).get());
        assertFalse(Bindings.notEqual(i, l).get());
        assertTrue(Bindings.equal(i, 6).get());
        assertTrue(Bindings.equal(6L, i).get());
        assertTrue(Bindings.notEqual(i, 7).get());
        assertTrue(Bindings.equal(d, 2.6, 0.2).get());
        assertFalse(Bindings.equal(d, 2.6, 0.05).get());
        assertTrue(Bindings.notEqual(d, 2.6, 0.05).get());
        assertTrue(Bindings.equal(2.4, d, 0.2).get());
        assertTrue(Bindings.equal(i, d, 4.0).get());
        assertTrue(i.isEqualTo(6).get());
        assertTrue(i.isNotEqualTo(5).get());
        assertTrue(d.isEqualTo(2.5, 0.0).get());
        assertTrue(i.isEqualTo(l).get());
        BooleanBinding equal = i.isEqualTo(l);
        i.set(1);
        assertFalse(equal.get());
    }

    @Test
    public void numericOrdering() {
        assertTrue(Bindings.greaterThan(i, l).get());
        assertFalse(Bindings.lessThan(i, l).get());
        assertTrue(Bindings.greaterThanOrEqual(i, 6).get());
        assertTrue(Bindings.lessThanOrEqual(i, 6).get());
        assertFalse(Bindings.greaterThan(i, 6).get());
        assertFalse(Bindings.lessThan(i, 6).get());
        assertTrue(Bindings.lessThan(1, i).get());
        assertTrue(Bindings.greaterThan(6.5, i).get());
        assertTrue(Bindings.lessThanOrEqual(2.5, d).get());
        assertTrue(Bindings.greaterThanOrEqual(2.5f, d).get());
        assertTrue(i.greaterThan(l).get());
        assertTrue(i.greaterThanOrEqualTo(6).get());
        assertTrue(l.lessThan(i).get());
        assertTrue(l.lessThanOrEqualTo(4L).get());
        assertTrue(d.lessThan(2.6).get());
        BooleanBinding greater = i.greaterThan(l);
        l.set(100);
        assertFalse(greater.get());
    }

    @Test
    public void booleanOperators() {
        BooleanProperty a = new SimpleBooleanProperty(true);
        BooleanProperty b = new SimpleBooleanProperty(false);
        BooleanBinding and = Bindings.and(a, b);
        BooleanBinding or = Bindings.or(a, b);
        BooleanBinding not = Bindings.not(a);
        assertFalse(and.get());
        assertTrue(or.get());
        assertFalse(not.get());
        b.set(true);
        assertTrue(and.get());
        a.set(false);
        assertFalse(and.get());
        assertTrue(or.get());
        assertTrue(not.get());
        b.set(false);
        assertFalse(or.get());
        assertTrue(a.not().get());
        assertFalse(a.and(b).get());
        assertTrue(a.or(b.not()).get());
        assertTrue(Bindings.equal(a, b).get());
        assertFalse(Bindings.notEqual(a, b).get());
        assertTrue(a.isEqualTo(b).get());
        a.set(true);
        assertTrue(a.isNotEqualTo(b).get());
        assertEquals("true", a.asString().get());
        assertEquals(Boolean.TRUE, a.asObject().get());
    }

    @Test
    public void booleanBindingInvalidatesWhenAnOperandChanges() {
        BooleanProperty a = new SimpleBooleanProperty(true);
        BooleanProperty b = new SimpleBooleanProperty(true);
        BooleanBinding and = a.and(b);
        assertTrue(and.get());
        Probe.Values<Boolean> changes = new Probe.Values<Boolean>();
        and.addListener(changes);
        b.set(false);
        b.set(true);
        assertEquals("[true->false, false->true]", changes.log.toString());
    }

    @Test
    public void whenThenOtherwise() {
        BooleanProperty condition = new SimpleBooleanProperty(true);
        NumberBinding number = Bindings.when(condition).then(i).otherwise(l);
        assertEquals(6, number.intValue());
        assertTrue(number instanceof LongBinding);
        condition.set(false);
        assertEquals(4, number.intValue());
        l.set(40);
        assertEquals(40, number.intValue());
        condition.set(true);
        i.set(60);
        assertEquals(60, number.intValue());

        NumberBinding constants = Bindings.when(condition).then(1).otherwise(2);
        assertTrue(constants instanceof IntegerBinding);
        assertEquals(1, constants.intValue());
        condition.set(false);
        assertEquals(2, constants.intValue());
        DoubleBinding mixed = Bindings.when(condition).then(i).otherwise(0.5);
        assertEquals(0.5, mixed.get(), 0d);

        StringProperty yes = new SimpleStringProperty("yes");
        StringBinding text = Bindings.when(condition).then(yes).otherwise("no");
        assertEquals("no", text.get());
        condition.set(true);
        assertEquals("yes", text.get());
        yes.set("YES");
        assertEquals("YES", text.get());
        assertEquals("a", Bindings.when(condition).then("a").otherwise("b").get());

        BooleanBinding flag = Bindings.when(condition).then(false).otherwise(true);
        assertFalse(flag.get());
        BooleanProperty other = new SimpleBooleanProperty(true);
        assertTrue(Bindings.when(condition).then(other).otherwise(false).get());

        ObjectProperty<List<String>> list = new SimpleObjectProperty<List<String>>(new ArrayList<String>());
        ObjectBinding<List<String>> object = Bindings.when(condition).then(list).otherwise((List<String>) null);
        assertSame(list.get(), object.get());
        condition.set(false);
        assertNull(object.get());
    }

    @Test
    public void whenInvalidatesOnConditionChange() {
        BooleanProperty condition = new SimpleBooleanProperty(true);
        NumberBinding binding = Bindings.when(condition).then(1).otherwise(2);
        binding.intValue();
        Probe.Invalidations probe = new Probe.Invalidations();
        binding.addListener(probe);
        condition.set(false);
        assertEquals(1, probe.count);
    }

    @Test
    public void stringConcatAndConvert() {
        StringProperty name = new SimpleStringProperty("World");
        StringExpression greeting = Bindings.concat("Hello ", name, "! ", i);
        assertEquals("Hello World! 6", greeting.get());
        name.set("FX");
        i.set(7);
        assertEquals("Hello FX! 7", greeting.get());
        assertEquals("FX7", name.concat(i).get());
        assertEquals("FX-", name.concat("-").get());
        assertEquals("7", Bindings.convert(i).get());
        assertEquals("7", i.asString().get());
        StringExpression converted = Bindings.convert(d);
        d.set(1.25);
        assertEquals("1.25", converted.get());
        name.set(null);
        assertEquals("null-", name.concat("-").get());
        assertEquals("", name.getValueSafe());
    }

    @Test
    public void stringFormat() {
        StringProperty name = new SimpleStringProperty("x");
        StringExpression formatted = Bindings.format("%s=%d", name, i);
        assertEquals("x=6", formatted.get());
        i.set(8);
        assertEquals("x=8", formatted.get());
        assertEquals("[8]", i.asString("[%d]").get());
    }

    @Test
    public void stringComparisons() {
        StringProperty a = new SimpleStringProperty("apple");
        StringProperty b = new SimpleStringProperty("Banana");
        assertFalse(Bindings.equal(a, b).get());
        assertTrue(Bindings.notEqual(a, b).get());
        assertTrue(Bindings.equal(a, "apple").get());
        assertTrue(Bindings.equal("apple", a).get());
        assertTrue(Bindings.equalIgnoreCase(a, "APPLE").get());
        assertFalse(Bindings.notEqualIgnoreCase(a, "APPLE").get());
        assertTrue(Bindings.notEqualIgnoreCase(a, b).get());
        assertTrue(Bindings.greaterThan(a, b).get());
        assertTrue(Bindings.lessThan(b, a).get());
        assertTrue(Bindings.greaterThanOrEqual(a, "apple").get());
        assertTrue(Bindings.lessThanOrEqual("apple", a).get());
        assertTrue(a.isEqualTo("apple").get());
        assertTrue(a.isNotEqualTo(b).get());
        assertTrue(a.isEqualToIgnoreCase("Apple").get());
        assertTrue(a.isNotEqualToIgnoreCase(b).get());
        assertTrue(a.greaterThan("aardvark").get());
        assertTrue(a.lessThan("b").get());
        assertTrue(a.greaterThanOrEqualTo(a).get());
        assertTrue(a.lessThanOrEqualTo("apple").get());
        StringProperty empty = new SimpleStringProperty();
        assertTrue("null counts as empty", Bindings.equal(empty, "").get());
        BooleanBinding equal = a.isEqualTo(b);
        b.set("apple");
        assertTrue(equal.get());
    }

    @Test
    public void stringLengthAndEmptiness() {
        StringProperty s = new SimpleStringProperty();
        assertTrue(s.isNull().get());
        assertFalse(s.isNotNull().get());
        assertTrue(s.isEmpty().get());
        assertFalse(s.isNotEmpty().get());
        assertEquals(0, s.length().get());
        IntegerBinding length = s.length();
        BooleanBinding isEmpty = s.isEmpty();
        s.set("abc");
        assertEquals(3, length.get());
        assertFalse(isEmpty.get());
        assertTrue(s.isNotNull().get());
        assertTrue(s.isNotEmpty().get());
        s.set("");
        assertTrue(isEmpty.get());
        assertFalse(s.isNull().get());
    }

    @Test
    public void objectComparisonsAndNullChecks() {
        ObjectProperty<String> a = new SimpleObjectProperty<String>("x");
        ObjectProperty<String> b = new SimpleObjectProperty<String>("y");
        assertFalse(Bindings.equal(a, b).get());
        assertTrue(Bindings.notEqual(a, b).get());
        assertTrue(Bindings.equal(a, "x").get());
        assertTrue(Bindings.equal("y", b).get());
        assertTrue(Bindings.notEqual(a, "z").get());
        assertTrue(Bindings.notEqual("z", a).get());
        assertFalse(Bindings.isNull(a).get());
        assertTrue(Bindings.isNotNull(a).get());
        BooleanBinding isNull = a.isNull();
        BooleanBinding equal = a.isEqualTo(b);
        a.set(null);
        assertTrue(isNull.get());
        assertFalse(a.isNotNull().get());
        assertFalse(equal.get());
        b.set(null);
        assertTrue("two nulls are equal", equal.get());
        assertTrue(a.isEqualTo((Object) null).get());
        assertTrue(a.isNotEqualTo("x").get());
        a.set("v");
        assertEquals("v", a.asString().get());
    }

    @Test
    public void createBindingsFromCallables() {
        final IntegerProperty source = new SimpleIntegerProperty(3);
        IntegerBinding integer = Bindings.createIntegerBinding(new Callable<Integer>() {
            @Override
            public Integer call() {
                return Integer.valueOf(source.get() * 2);
            }
        }, source);
        assertEquals(6, integer.get());
        source.set(5);
        assertEquals(10, integer.get());
        assertEquals(1, integer.getDependencies().size());
        assertSame(source, integer.getDependencies().get(0));
        DoubleBinding dbl = Bindings.createDoubleBinding(() -> source.get() / 2.0, source);
        assertEquals(2.5, dbl.get(), 0d);
        LongBinding lng = Bindings.createLongBinding(() -> (long) source.get() << 33, source);
        assertEquals(5L << 33, lng.get());
        FloatBinding flt = Bindings.createFloatBinding(() -> source.get() + 0.5f, source);
        assertEquals(5.5f, flt.get(), 0f);
        BooleanBinding bool = Bindings.createBooleanBinding(() -> source.get() > 4, source);
        assertTrue(bool.get());
        StringBinding str = Bindings.createStringBinding(() -> "n" + source.get(), source);
        assertEquals("n5", str.get());
        ObjectBinding<List<Integer>> obj = Bindings.createObjectBinding(() -> {
            List<Integer> result = new ArrayList<Integer>();
            result.add(Integer.valueOf(source.get()));
            return result;
        }, source);
        assertEquals("[5]", obj.get().toString());
        source.set(1);
        assertEquals(0.5, dbl.get(), 0d);
        assertFalse(bool.get());
        assertEquals("n1", str.get());
        assertEquals("[1]", obj.get().toString());
        assertEquals(0, Bindings.createIntegerBinding(() -> 1).getDependencies().size());
    }

    @Test
    public void disposeStopsObserving() {
        final IntegerProperty source = new SimpleIntegerProperty(3);
        IntegerBinding binding = Bindings.createIntegerBinding(() -> source.get(), source);
        assertEquals(3, binding.get());
        Probe.Invalidations probe = new Probe.Invalidations();
        binding.addListener(probe);
        binding.dispose();
        source.set(4);
        assertEquals(0, probe.count);
        assertEquals("the cached value stays", 3, binding.get());
    }

    @Test
    public void listBindings() {
        ObservableList<String> list = FXCollections.observableArrayList("a", "b");
        IntegerBinding size = Bindings.size(list);
        BooleanBinding empty = Bindings.isEmpty(list);
        BooleanBinding notEmpty = Bindings.isNotEmpty(list);
        ObjectBinding<String> second = Bindings.valueAt(list, 1);
        IntegerProperty index = new SimpleIntegerProperty(0);
        ObjectBinding<String> indexed = Bindings.valueAt(list, index);
        assertEquals(2, size.get());
        assertFalse(empty.get());
        assertTrue(notEmpty.get());
        assertEquals("b", second.get());
        assertEquals("a", indexed.get());
        index.set(1);
        assertEquals("b", indexed.get());
        index.set(5);
        assertNull("outside the list is null", indexed.get());
        list.remove(1);
        assertEquals(1, size.get());
        assertNull(second.get());
        list.clear();
        assertTrue(empty.get());
        assertFalse(notEmpty.get());
        assertEquals(0, size.get());
    }

    @Test
    public void setAndMapBindings() {
        ObservableSet<String> set = FXCollections.observableSet(new HashSet<String>());
        IntegerBinding setSize = Bindings.size(set);
        BooleanBinding setEmpty = Bindings.isEmpty(set);
        assertTrue(setEmpty.get());
        set.add("a");
        assertEquals(1, setSize.get());
        assertFalse(setEmpty.get());
        assertTrue(Bindings.isNotEmpty(set).get());

        ObservableMap<String, Integer> map = FXCollections.observableHashMap();
        IntegerBinding mapSize = Bindings.size(map);
        BooleanBinding mapEmpty = Bindings.isEmpty(map);
        ObjectBinding<Integer> value = Bindings.valueAt(map, "k");
        StringProperty key = new SimpleStringProperty("k");
        ObjectBinding<Integer> keyed = Bindings.valueAt(map, key);
        assertTrue(mapEmpty.get());
        assertNull(value.get());
        map.put("k", Integer.valueOf(1));
        map.put("j", Integer.valueOf(2));
        assertEquals(2, mapSize.get());
        assertFalse(mapEmpty.get());
        assertTrue(Bindings.isNotEmpty(map).get());
        assertEquals(Integer.valueOf(1), value.get());
        assertEquals(Integer.valueOf(1), keyed.get());
        key.set("j");
        assertEquals(Integer.valueOf(2), keyed.get());
        map.remove("k");
        assertNull(value.get());
    }

    @Test
    public void bidirectionalBindingThroughAConverter() {
        StringProperty text = new SimpleStringProperty("0");
        ObjectProperty<Integer> number = new SimpleObjectProperty<Integer>(Integer.valueOf(12));
        Bindings.bindBidirectional(text, number, new IntegerStringConverter());
        assertEquals("12", text.get());
        number.set(Integer.valueOf(5));
        assertEquals("5", text.get());
        text.set("42");
        assertEquals(Integer.valueOf(42), number.get());
        Bindings.unbindBidirectional(text, number);
        text.set("1");
        assertEquals(Integer.valueOf(42), number.get());

        StringProperty fluent = new SimpleStringProperty();
        fluent.bindBidirectional(number, new IntegerStringConverter());
        assertEquals("42", fluent.get());
        fluent.set("7");
        assertEquals(Integer.valueOf(7), number.get());
        fluent.unbindBidirectional(number);
        fluent.set("8");
        assertEquals(Integer.valueOf(7), number.get());
    }

    @Test
    public void staticBidirectionalBinding() {
        StringProperty a = new SimpleStringProperty("a");
        StringProperty b = new SimpleStringProperty("b");
        Bindings.bindBidirectional(a, b);
        assertEquals("b", a.get());
        b.set("c");
        assertEquals("c", a.get());
        Bindings.unbindBidirectional(a, b);
        b.set("d");
        assertEquals("c", a.get());
        try {
            Bindings.bindBidirectional(a, a);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void bindContentOfAList() {
        ObservableList<String> source = FXCollections.observableArrayList("a", "b");
        List<String> target = new ArrayList<String>();
        target.add("stale");
        Bindings.bindContent(target, source);
        assertEquals("[a, b]", target.toString());
        source.add("c");
        source.remove("a");
        source.set(0, "B");
        assertEquals("[B, c]", target.toString());
        FXCollections.sort(source);
        assertEquals(source.toString(), target.toString());
        source.setAll("x", "y", "z");
        assertEquals("[x, y, z]", target.toString());
        source.remove(0, 2);
        assertEquals("[z]", target.toString());
        Bindings.unbindContent(target, source);
        source.add("late");
        assertEquals("[z]", target.toString());
    }

    @Test
    public void bindContentOfASetAndAMap() {
        ObservableSet<String> sourceSet = FXCollections.observableSet(new HashSet<String>());
        sourceSet.add("a");
        Set<String> targetSet = new HashSet<String>();
        targetSet.add("stale");
        Bindings.bindContent(targetSet, sourceSet);
        assertEquals(sourceSet, targetSet);
        sourceSet.add("b");
        sourceSet.remove("a");
        assertEquals(sourceSet, targetSet);
        assertEquals(1, targetSet.size());
        Bindings.unbindContent(targetSet, sourceSet);
        sourceSet.add("c");
        assertEquals(1, targetSet.size());

        ObservableMap<String, Integer> sourceMap = FXCollections.observableHashMap();
        sourceMap.put("a", Integer.valueOf(1));
        Map<String, Integer> targetMap = new HashMap<String, Integer>();
        targetMap.put("stale", Integer.valueOf(0));
        Bindings.bindContent(targetMap, sourceMap);
        assertEquals(sourceMap, targetMap);
        sourceMap.put("a", Integer.valueOf(2));
        sourceMap.put("b", Integer.valueOf(3));
        sourceMap.remove("a");
        assertEquals(sourceMap, targetMap);
        assertEquals("{b=3}", targetMap.toString());
        Bindings.unbindContent(targetMap, sourceMap);
        sourceMap.clear();
        assertEquals("{b=3}", targetMap.toString());
    }

    @Test
    public void bindContentBidirectional() {
        ObservableList<String> a = FXCollections.observableArrayList("a");
        ObservableList<String> b = FXCollections.observableArrayList("b", "c");
        Bindings.bindContentBidirectional(a, b);
        assertEquals("[b, c]", a.toString());
        a.add("d");
        assertEquals("[b, c, d]", b.toString());
        b.remove("b");
        assertEquals("[c, d]", a.toString());
        a.set(0, "C");
        assertEquals("[C, d]", b.toString());
        Bindings.unbindContentBidirectional(a, b);
        a.add("e");
        assertEquals("[C, d]", b.toString());
        b.clear();
        assertEquals("[C, d, e]", a.toString());
    }

    @Test
    public void expressionFactoriesReturnTheValueItself() {
        assertSame(i, javafx.beans.binding.IntegerExpression.integerExpression(i));
        assertSame(d, javafx.beans.binding.NumberExpressionBase.numberExpression(d));
        StringProperty s = new SimpleStringProperty("s");
        assertSame(s, StringExpression.stringExpression(s));
        assertEquals("6", StringExpression.stringExpression(i).get());
        assertEquals(Integer.valueOf(6), i.asObject().get());
    }
}
