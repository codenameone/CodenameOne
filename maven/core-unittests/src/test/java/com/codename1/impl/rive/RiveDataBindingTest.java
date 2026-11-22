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
package com.codename1.impl.rive;

import com.codename1.rive.transcoder.RiveTranscoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Data binding, with the expectations of rive-runtime's
/// `tests/unit_tests/runtime/data_binding_test.cpp` ("Range Mapper",
/// "Pad String", "Boolean Toggle") on `data_binding_test_2.riv`: the view
/// models are imported at file level, each artboard binds a copy of its
/// default instance, and changed values reach their targets through the
/// converters on the next advance.
class RiveDataBindingTest {
    private static final String DIR = "../rive-transcoder/src/test/resources/rive/";
    private static final String[][] FIXTURES = {
        {"DataBindingRive", "data_binding_test_2.riv"},
        {"AssignRive", "listener_viewmodel_assign.riv"},
        {"KeyFrameRive", "joystick_databound_keyframe_test.riv"},
        {"ScriptedKeyFrameRive", "data_bound_keyframe_test.riv"},
        {"ListRive", "component_list_row_click.riv"},
        {"IndexRive", "transition_index_condition.riv"},
    };

    @TempDir
    static Path temporary;
    private static URLClassLoader loader;

    @BeforeAll
    static void translateAndCompile() throws Exception {
        File pkg = temporary.resolve("gen").toFile();
        assertTrue(pkg.mkdirs());
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        for (String[] f : FIXTURES) {
            byte[] riv = Files.readAllBytes(new File(DIR + f[1]).toPath());
            File src = new File(pkg, f[0] + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "gen", f[0]).getBytes(StandardCharsets.US_ASCII));
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temporary.toString(),
                    "-cp", System.getProperty("java.class.path"), src.getPath()));
        }
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveDataBindingTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static Artboard artboard(String name) throws Exception {
        return artboard("DataBindingRive", name);
    }

    private static Artboard artboard(String cls, String name) throws Exception {
        RiveFile file = (RiveFile) loader.loadClass("gen." + cls).getDeclaredConstructor().newInstance();
        String[] names = file.getArtboardNames();
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return file.instance(i);
            }
        }
        throw new AssertionError("no artboard " + name);
    }

    private static StateMachineInstance advance(Artboard a, StateMachineInstance machine) {
        StateMachineInstance m = machine == null ? new StateMachineInstance(a.getStateMachines().get(0), a) : machine;
        m.advance(0);
        a.advance(0);
        return m;
    }

    private static Core find(Artboard a, String name) {
        for (Core c : a.getObjects()) {
            if (c instanceof Component && name.equals(((Component) c).getName())) {
                return c;
            }
        }
        throw new AssertionError("no object " + name);
    }

    private static float number(Artboard a, String name) {
        return ((CustomPropertyNumber) find(a, name)).getPropertyValue();
    }

    private static ViewModelInstanceValue value(Artboard a, String property) {
        ViewModelInstance vmi = a.getViewModelInstance();
        assertNotNull(vmi, "the default view model instance is bound");
        ViewModelInstanceValue v = vmi.propertyValue(property);
        assertNotNull(v, property);
        return v;
    }

    @Test
    void rangeMapper() throws Exception {
        Artboard a = artboard("artboard-2");
        StateMachineInstance m = advance(a, null);
        assertEquals(6f, number(a, "mapped-range-1"));
        assertEquals(3f, number(a, "mapped-range-2"));
        assertEquals(2f, number(a, "mapped-range-3"));
        assertEquals(2f, number(a, "mapped-range-4"));
        assertEquals(2f, number(a, "mapped-range-5"));

        ((ViewModelInstanceNumber) value(a, "map-range-num")).setPropertyValue(-1f);
        advance(a, m);
        assertEquals(1f, number(a, "mapped-range-1"));
        assertEquals(2f, number(a, "mapped-range-2"));
        assertEquals(2f, number(a, "mapped-range-3"));
        assertEquals(3f, number(a, "mapped-range-4"));
    }

    @Test
    void padString() throws Exception {
        Artboard a = artboard("artboard-3");
        advance(a, null);
        assertEquals("abcabcatext", ((CustomPropertyString) find(a, "pad-string-1")).getPropertyValue());
        assertEquals("textabcabcab", ((CustomPropertyString) find(a, "pad-string-2")).getPropertyValue());
        assertEquals("", ((CustomPropertyString) find(a, "pad-string-3")).getPropertyValue());
    }

    @Test
    void booleanToggle() throws Exception {
        Artboard a = artboard("artboard-3");
        StateMachineInstance m = advance(a, null);
        CustomPropertyBoolean negated = (CustomPropertyBoolean) find(a, "negate-bool-1");
        assertTrue(negated.isPropertyValue());
        ViewModelInstanceBoolean prop = (ViewModelInstanceBoolean) value(a, "bool-prop");
        assertEquals(false, prop.isPropertyValue());
        prop.setPropertyValue(true);
        advance(a, m);
        assertEquals(false, negated.isPropertyValue());
    }

    @Test
    void everyArtboardBindsItsOwnCopyOfTheDefaultInstance() throws Exception {
        Artboard first = artboard("artboard-2");
        Artboard second = artboard("artboard-2");
        assertNotSame(first.getViewModelInstance(), second.getViewModelInstance());
        ((ViewModelInstanceNumber) value(first, "map-range-num")).setPropertyValue(-1f);
        advance(first, null);
        advance(second, null);
        assertEquals(1f, number(first, "mapped-range-1"));
        assertEquals(6f, number(second, "mapped-range-1"));
    }

    private static float step(DataConverterInterpolator converter, float target) {
        converter.advance(1f / 60f);
        DataValue input = new DataValue();
        input.setNumber(target);
        converter.convert(input, null);
        return input.number;
    }

    private static int tweenTo(DataConverterInterpolator converter, float target) {
        float previous = step(converter, target);
        int frames = 0;
        while (previous != target && frames < 600) {
            float value = step(converter, target);
            assertNotEquals(previous, value, "frame " + frames + " must move the value");
            previous = value;
            frames++;
        }
        assertEquals(target, previous);
        return frames;
    }

    /// data_binding_converters_test.cpp, "data converter interpolator
    /// restarts without holding a frame".
    @Test
    void interpolatorRestartsWithoutHoldingAFrame() {
        DataConverterInterpolator converter = new DataConverterInterpolator();
        converter.setDuration(0.375f);
        DataValue initial = new DataValue();
        initial.setNumber(0);
        converter.convert(initial, null);
        step(converter, 0);
        step(converter, 0);
        int first = tweenTo(converter, 300);
        assertEquals(23, first);
        assertEquals(first, tweenTo(converter, 0));
        assertEquals(first, tweenTo(converter, 300));
        for (int i = 0; i < 12; i++) {
            step(converter, 0);
        }
        tweenTo(converter, 300);
    }

    @Test
    void converterCopyKeepsItsProperties() {
        DataConverterRangeMapper mapper = new DataConverterRangeMapper();
        mapper.setMinInput(2f);
        mapper.setMaxOutput(9f);
        mapper.setFlags(3);
        DataConverterRangeMapper copy = (DataConverterRangeMapper) mapper.makeCopy();
        assertNotSame(mapper, copy);
        assertEquals(2f, copy.getMinInput());
        assertEquals(9f, copy.getMaxOutput());
        assertEquals(3, copy.getFlags());
    }

    /// Upstream `DataBind::cloneWithTarget`: every bind converts through a
    /// copy of its converter, never the file's shared one.
    @Test
    void everyBindConvertsThroughItsOwnConverterCopy() throws Exception {
        Artboard a = artboard("artboard-2");
        advance(a, null);
        int converted = 0;
        for (DataBind b : a.getDataBinds()) {
            DataConverter c = b.getConverter();
            if (c == null) {
                continue;
            }
            converted++;
            assertNotSame(a.getFile().getConverter(b.getConverterId()), c);
            for (DataBind other : a.getDataBinds()) {
                if (other != b && other.getConverter() != null) {
                    assertNotSame(c, other.getConverter());
                }
            }
        }
        assertTrue(converted > 0, "artboard-2 binds through converters");
    }

    private static ViewModelInstance reference(Artboard a, String property) {
        return ((ViewModelInstanceViewModel) value(a, property)).getReference();
    }

    /// rive-runtime's `listener_viewmodel_assign_test.cpp`: a listener sets a
    /// view model property to another one ("FromProperty") and, from inside a
    /// nested artboard bound to `source`, to "My ViewModel" ("MyViewModel").
    @Test
    void listenerAssignsAViewModelProperty() throws Exception {
        for (String name : new String[]{"FromProperty", "MyViewModel"}) {
            Artboard a = artboard("AssignRive", name);
            StateMachineInstance m = advance(a, null);
            ViewModelInstance source = reference(a, "source");
            assertNotNull(source, name);
            assertNotSame(source, reference(a, "current"), name);
            ((ViewModelInstanceTrigger) value(a, "go")).fire();
            m.advance(0.016f);
            a.advance(0.016f);
            assertSame(source, reference(a, "current"), name);
        }
    }

    /// A nested artboard with a data bind path binds the instance the path
    /// names, in a context whose parent is its host's.
    @Test
    void nestedArtboardBindsTheInstanceItsPathNames() throws Exception {
        Artboard a = artboard("AssignRive", "MyViewModel");
        advance(a, null);
        NestedArtboard nested = null;
        for (Core c : a.getObjects()) {
            if (c instanceof NestedArtboard) {
                nested = (NestedArtboard) c;
            }
        }
        assertNotNull(nested);
        DataContext context = nested.getArtboardInstance().getDataContext();
        assertNotNull(context);
        assertSame(reference(a, "source"), context.getMain());
        assertSame(a.getDataContext(), context.getParent());
        assertSame(value(a, "go"), context.resolve(new int[]{a.getViewModelInstance().getViewModelId(),
                value(a, "go").getViewModelPropertyId()}));
    }

    /// Keyframe values bound to view model numbers (`yAnim` of
    /// `joystick_databound_keyframe_test.riv`): the animation runs between
    /// the bound values, not the authored ones.
    @Test
    void keyFrameValuesFollowTheirBinds() throws Exception {
        Artboard a = artboard("KeyFrameRive", "Main");
        ((ViewModelInstanceNumber) value(a, "yStartPos")).setPropertyValue(123);
        ((ViewModelInstanceNumber) value(a, "yEndPos")).setPropertyValue(321);
        a.advance(0);
        LinearAnimation anim = null;
        for (LinearAnimation l : a.getAnimations()) {
            if ("yAnim".equals(l.getName())) {
                anim = l;
            }
        }
        assertNotNull(anim);
        Shape shape = null;
        for (Core c : a.getObjects()) {
            if (c instanceof Shape && shape == null) {
                shape = (Shape) c;
            }
        }
        assertNotNull(shape);
        LinearAnimationInstance instance = new LinearAnimationInstance(anim, a);
        float[] expected = {123, 222, 321};
        float[] times = {0, 0.5f, 1};
        for (int i = 0; i < times.length; i++) {
            instance.setTime(times[i]);
            instance.apply(1);
            assertEquals(expected[i], shape.getY(), 0.001f, "t=" + times[i]);
        }
    }

    /// The printf-style specs of rive-runtime's `string_template_format.cpp`,
    /// formatted without `String.format`.
    @Test
    void stringTemplateNumberSpecs() {
        String[][] cases = {
            {"1.5", ""}, {"1.5", "bogus"}, {"-0.0", ""},
            {"3.14159", ".2f"}, {"3.14159", "%.2f"}, {"2.5", "d"}, {"-2.5", "d"},
            {"42", "05d"}, {"42", "-5d"}, {"42", "+d"}, {"255", "x"}, {"255", "#X"},
            {"8", "o"}, {"8", "#o"}, {"1234567", ",d"}, {"1234567.5", ",.1f"},
            {"12345.678", "e"}, {"0.0001234", ".3g"}, {"100000", "g"}, {"1000000", "g"},
            {"7", ".3d"}, {"1.5", "8.2f"}, {"-1.5", "08.2f"},
        };
        String[] expected = {
            "1.5", "1.5", "0",
            "3.14", "3.14", "3", "-3",
            "00042", "42   ", "+42", "ff", "0XFF",
            "10", "010", "1,234,567", "1,234,567.5",
            "1.234568e+04", "0.000123", "100000", "1e+06",
            "007", "    1.50", "-0001.50",
        };
        for (int i = 0; i < cases.length; i++) {
            assertEquals(expected[i], StringTemplateFormat.formatNumber(Float.parseFloat(cases[i][0]), cases[i][1]),
                    cases[i][0] + " " + cases[i][1]);
        }
        assertEquals("nan", StringTemplateFormat.formatNumber(Float.NaN, "d"));
        assertTrue(StringTemplateFormat.isValidNumberSpec("+08.3f"));
        assertTrue(!StringTemplateFormat.isValidNumberSpec("#d"));
        assertTrue(!StringTemplateFormat.isValidNumberSpec("123d"));
    }

    /// A template's literals, escapes and placeholders with no argument.
    @Test
    void stringTemplateLiteralsAndEscapes() {
        DataConverterStringTemplate t = new DataConverterStringTemplate();
        DataValue v = new DataValue();
        v.setString("x${a}y");
        t.convert(v, null);
        assertEquals("xy", v.string, "the default delimiters are ${ and }");
        t.setOpenDelimiter("{");
        v.setString("a\\{b}c\\\\{0:.2f}d{}");
        t.convert(v, null);
        assertEquals("a{b}c\\d", v.string);
    }

    /// A keyframe eased by a Luau-scripted interpolator keeps playing:
    /// without scripting upstream instantiates the interpolator and eases
    /// linearly, so the keyframe resolves instead of being dropped.
    @Test
    void scriptedInterpolatorKeyFrameIsKept() throws Exception {
        Artboard a = artboard("ScriptedKeyFrameRive", "child");
        a.advance(0);
        LinearAnimation anim = a.getAnimations().get(0);
        KeyedProperty kp = anim.getKeyedObject(0).getKeyedProperty(0);
        assertEquals(2, kp.getKeyFrameCount(), "both keyframes survive");
        InterpolatingKeyFrame first = (InterpolatingKeyFrame) kp.getKeyFrame(0);
        assertTrue(first.getInterpolator() instanceof ScriptedInterpolator);
        assertEquals(0.5f, first.getInterpolator().transform(0.5f), 0f, "linear without scripting");
    }

    private static ArtboardComponentList list(Artboard a) {
        for (Core c : a.getObjects()) {
            if (c instanceof ArtboardComponentList) {
                return (ArtboardComponentList) c;
            }
        }
        throw new AssertionError("no list");
    }

    /// A list builds one row per item of the bound list and rebuilds them when
    /// items are removed or inserted. Upstream's component_list_test opens
    /// this file with one row (`artboardCount() == 1`). No small asset-free
    /// upstream fixture pairs a list with a list index symbol (the ones that
    /// have one build their rows through a number-to-list converter), so the
    /// index values are not asserted here.
    @Test
    void listRowsFollowTheirItems() throws Exception {
        Artboard a = artboard("ListRive", "Main");
        a.advance(0);
        ArtboardComponentList rows = list(a);
        assertEquals(1, rows.getRows().size());
        ViewModelInstanceValue v = a.getViewModelInstance().propertyValue("rows");
        assertTrue(v instanceof ViewModelInstanceList);
        ViewModelInstanceList items = (ViewModelInstanceList) v;
        assertEquals(1, items.getInstances().size());
        ViewModelInstance first = items.getInstances().get(0);
        assertSame(first, rows.getRows().get(0).getViewModelInstance());
        items.removeInstance(0);
        a.advance(0);
        assertEquals(0, rows.getRows().size());
        items.addInstance(0, first);
        a.advance(0);
        assertEquals(1, rows.getRows().size());
        assertSame(first, rows.getRows().get(0).getViewModelInstance());
    }

    private static float rowIndex(ArtboardComponentList rows, int i) {
        ViewModelInstance item = rows.getRows().get(i).getViewModelInstance();
        assertNotNull(item, "row " + i + " is bound to its item");
        for (ViewModelInstanceValue v : item.getValues()) {
            if (v instanceof ViewModelInstanceSymbolListIndex) {
                return ((ViewModelInstanceSymbolListIndex) v).getPropertyValue();
            }
        }
        throw new AssertionError("row " + i + " has no list index");
    }

    /// A number-to-list converter builds one row per unit of its number, each
    /// told its list index. Upstream's state_machine_test ("Transition with
    /// list index can be compared to a number") binds the default instance
    /// of this file, whose "num-to-list" is 3, and matches a silver of three
    /// rows whose state machines pick a color from their index.
    @Test
    void numberToListRowsAreIndexed() throws Exception {
        Artboard a = artboard("IndexRive", "main");
        StateMachineInstance m = advance(a, null);
        ArtboardComponentList rows = list(a);
        ViewModelInstanceNumber n = (ViewModelInstanceNumber) value(a, "num-to-list");
        int count = (int) n.getPropertyValue();
        assertEquals(3, count);
        assertEquals(count, rows.getRows().size());
        for (int i = 0; i < count; i++) {
            assertEquals(i, rowIndex(rows, i), 0f);
        }
        ViewModelInstance second = rows.getRows().get(1).getViewModelInstance();
        n.setPropertyValue(5);
        advance(a, m);
        assertEquals(5, rows.getRows().size());
        assertSame(second, rows.getRows().get(1).getViewModelInstance(), "kept items keep their rows");
        for (int i = 0; i < 5; i++) {
            assertEquals(i, rowIndex(rows, i), 0f);
        }
        n.setPropertyValue(2);
        advance(a, m);
        assertEquals(2, rows.getRows().size());
        n.setPropertyValue(-1);
        advance(a, m);
        assertEquals(0, rows.getRows().size(), "a negative count is an empty list");
    }
}
