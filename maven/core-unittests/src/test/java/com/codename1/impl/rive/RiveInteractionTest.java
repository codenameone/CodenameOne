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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pointer events reach the state machines of nested artboards and of
/// component list rows, as rive-runtime's `HitNestedArtboard` and
/// `HitComponentList` forward them.
class RiveInteractionTest {
    private static final String DIR = "../rive-transcoder/src/test/resources/rive/";
    private static final String[][] FIXTURES = {
        {"ListClickRive", "component_list_row_click.riv"},
        {"NestedEventRive", "nested_event_test.riv"},
        {"LightSwitchRive", "light_switch.riv"},
    };

    @TempDir
    static Path temporary;
    private static URLClassLoader loader;

    @BeforeAll
    static void translateAndCompile() throws Exception {
        ArrayList<String> args = new ArrayList<String>();
        args.add("-d");
        args.add(temporary.toString());
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        File pkg = temporary.resolve("gen").toFile();
        assertTrue(pkg.mkdirs());
        for (String[] f : FIXTURES) {
            byte[] riv = Files.readAllBytes(new File(DIR + f[1]).toPath());
            File src = new File(pkg, f[0] + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "gen", f[0]).getBytes(StandardCharsets.US_ASCII));
            args.add(src.getPath());
        }
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveInteractionTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static Artboard artboard(String cls, String name) throws Exception {
        RiveFile file = (RiveFile) loader.loadClass("gen." + cls).getDeclaredConstructor().newInstance();
        String[] names = file.getArtboardNames();
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i])) {
                return file.instance(i);
            }
        }
        throw new AssertionError("no artboard " + name);
    }

    private static void advance(Artboard a, StateMachineInstance m) {
        m.advance(0.016f);
        a.advance(0.016f);
    }

    private static boolean clicked(ViewModelInstance item) {
        return ((ViewModelInstanceBoolean) item.propertyValue("clicked")).isPropertyValue();
    }

    /// component_list_test "Component List row click": a press and release
    /// over the row clicks it (the row's own state machine sets its item's
    /// `clicked`), and a release over a row that replaced the pressed one
    /// does not click the new item, while a fresh press and release does.
    @Test
    void clickReachesTheListRowsStateMachine() throws Exception {
        Artboard a = artboard("ListClickRive", "Main");
        StateMachineInstance m = new StateMachineInstance(a.getStateMachines().get(0), a);
        advance(a, m);
        ViewModelInstanceList items = (ViewModelInstanceList) a.getViewModelInstance().propertyValue("rows");
        assertEquals(1, items.getInstances().size());
        ViewModelInstance first = items.getInstances().get(0);
        assertFalse(clicked(first));
        m.pointerDown(100, 20);
        m.pointerUp(100, 20);
        advance(a, m);
        assertTrue(clicked(first));

        m.pointerDown(100, 20);
        advance(a, m);
        ViewModelInstance replacement = first.copyInstance(a.getFile(), 0);
        ((ViewModelInstanceBoolean) replacement.propertyValue("clicked")).setPropertyValue(false);
        items.removeInstance(0);
        items.addInstance(0, replacement);
        advance(a, m);
        m.pointerUp(100, 20);
        advance(a, m);
        assertFalse(clicked(replacement));

        m.pointerDown(100, 20);
        m.pointerUp(100, 20);
        advance(a, m);
        assertTrue(clicked(replacement));
    }

    /// state_machine_event_test "events from a nested artboard propagate to
    /// a listener on a parent": a press over the nested artboard's shape
    /// makes its machine report `NestedEvent`, which bubbles to the host's
    /// listener targeting that nested artboard and sets `Boolean 1`. The
    /// bubbled event is not in the host's own reported events.
    @Test
    void nestedEventReachesTheHostListener() throws Exception {
        RiveFile file = (RiveFile) loader.loadClass("gen.NestedEventRive").getDeclaredConstructor().newInstance();
        Artboard a = file.instance(0);
        assertEquals(1, a.getStateMachines().size());
        StateMachineInstance m = new StateMachineInstance(a.getStateMachines().get(0), a);
        assertFalse(m.getBoolean("Boolean 1"));
        a.advance(0);
        m.advance(0);
        com.codename1.impl.rive.NestedArtboard nested = null;
        for (com.codename1.impl.rive.Core o : a.getObjects()) {
            if (o instanceof com.codename1.impl.rive.NestedArtboard) {
                nested = (com.codename1.impl.rive.NestedArtboard) o;
            }
        }
        assertNotNull(nested);
        StateMachineInstance inner = ((com.codename1.impl.rive.NestedStateMachine) nested.getNestedAnimations().get(0))
                .getStateMachineInstance();
        assertNotNull(inner);
        assertEquals(0, inner.getReportedEvents().size());
        m.pointerDown(250, 100);
        assertEquals(1, inner.getReportedEvents().size());
        assertEquals("NestedEvent", inner.getReportedEvents().get(0).event.getName());
        a.advance(0);
        assertTrue(m.getBoolean("Boolean 1"));
        m.advance(0);
        assertEquals(0, m.getReportedEvents().size());
    }

    /// A transition that interrupts another mixes from the blend the first
    /// had reached (upstream `AnimationReset`, with `mixFrom` 0): halfway
    /// through the 200 ms mix back to On of light_switch.riv, every value keyed
    /// by the On and Off animations (each color channel
    /// by itself) sits halfway between where the
    /// interruption caught it and where On puts it. Without the reset the
    /// mix compounds frame over frame and lands far closer to On.
    @Test
    void interruptedTransitionMixesFromTheInterruptedBlend() throws Exception {
        RiveFile file = (RiveFile) loader.loadClass("gen.LightSwitchRive").getDeclaredConstructor().newInstance();
        Artboard a = file.instance(0);
        // light_switch's transitions cannot be interrupted as authored; turn
        // on each one's "enable early exit" flag (StateTransitionFlags 1 << 5,
        // as the editor writes it) so a change of input interrupts the mix.
        for (com.codename1.impl.rive.StateMachine.LayerDef l : a.getStateMachines().get(0).getLayerDefs()) {
            for (com.codename1.impl.rive.StateMachine.StateDef st : l.states) {
                for (com.codename1.impl.rive.StateMachine.TransitionDef t : st.transitions) {
                    t.transition.setFlags(t.transition.getFlags() | EARLY_EXIT);
                }
            }
        }
        StateMachineInstance m = new StateMachineInstance(a.getStateMachines().get(0), a);
        m.setBoolean("On", false);
        step(a, m, 1f, 50);
        ArrayList<com.codename1.impl.rive.Core> objs = new ArrayList<com.codename1.impl.rive.Core>();
        ArrayList<Integer> keys = new ArrayList<Integer>();
        for (com.codename1.impl.rive.LinearAnimation anim : a.getAnimations()) {
            for (int o = 0; o < anim.getKeyedObjectCount(); o++) {
                com.codename1.impl.rive.KeyedObject ko = anim.getKeyedObject(o);
                for (int k = 0; k < ko.getKeyedPropertyCount(); k++) {
                    com.codename1.impl.rive.KeyedProperty kp = ko.getKeyedProperty(k);
                    if (kp.getKeyFrame(0) instanceof com.codename1.impl.rive.KeyFrameDouble) {
                        objs.add(a.resolve(ko.getObjectId()));
                        keys.add(Integer.valueOf(kp.getPropertyKey()));
                    } else if (kp.getKeyFrame(0) instanceof com.codename1.impl.rive.KeyFrameColor) {
                        // a color is four 8 bit channels; key + 1000 * (channel + 1)
                        for (int c = 0; c < 4; c++) {
                            objs.add(a.resolve(ko.getObjectId()));
                            keys.add(Integer.valueOf(kp.getPropertyKey() + 1000 * (c + 1)));
                        }
                    }
                }
            }
        }
        float[] off = values(objs, keys);
        m.setBoolean("On", true);
        step(a, m, 1f, 50);
        float[] on = values(objs, keys);
        m.setBoolean("On", false);
        step(a, m, 0.1f, 10);
        float[] caught = values(objs, keys);
        m.setBoolean("On", true);
        step(a, m, 0.1f, 10);
        float[] half = values(objs, keys);
        int checked = 0;
        for (int i = 0; i < off.length; i++) {
            if (Math.abs(on[i] - off[i]) < 0.01f) {
                continue;
            }
            checked++;
            assertEquals((caught[i] + on[i]) / 2f, half[i], Math.abs(on[i] - off[i]) * 0.1f,
                    "property " + keys.get(i) + " of object " + i);
        }
        assertTrue(checked > 0, "the switch keys values that move");
    }

    private static final int EARLY_EXIT = 1 << 5;

    private static void step(Artboard a, StateMachineInstance m, float seconds, int frames) {
        for (int i = 0; i < frames; i++) {
            m.advance(seconds / frames);
            a.advance(seconds / frames);
        }
    }

    private static float[] values(ArrayList<com.codename1.impl.rive.Core> objs, ArrayList<Integer> keys) {
        float[] v = new float[objs.size()];
        for (int i = 0; i < v.length; i++) {
            int key = keys.get(i).intValue();
            if (key >= 1000) {
                int argb = objs.get(i).getColorProperty(key % 1000);
                v[i] = (argb >>> (8 * (key / 1000 - 1))) & 0xff;
            } else {
                v[i] = objs.get(i).getDoubleProperty(key);
            }
        }
        return v;
    }
}
