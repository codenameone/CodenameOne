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
package com.codename1.rive;

import com.codename1.impl.rive.AnimationState;
import com.codename1.impl.rive.Artboard;
import com.codename1.impl.rive.LayerState;
import com.codename1.impl.rive.LinearAnimation;
import com.codename1.impl.rive.StateMachine;
import com.codename1.impl.rive.StateMachineInstance;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// State machine behaviour, held to the expectations of rive-runtime's own
/// unit tests (state_machine_test.cpp, state_machine_event_test.cpp) on the
/// same files, translated and compiled the way a build does it.
class RiveStateMachineTest {
    private static final String FIXTURES = "../rive-transcoder/src/test/resources/rive/";
    private static final String[] FILES = {"state_machine_triggers.riv", "light_switch.riv", "timeline_event_test.riv"};

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
        File pkg = temporary.resolve("smgen").toFile();
        assertTrue(pkg.mkdirs());
        for (int i = 0; i < FILES.length; i++) {
            byte[] riv = Files.readAllBytes(new File(FIXTURES + FILES[i]).toPath());
            File src = new File(pkg, "Sm" + i + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "smgen", "Sm" + i).getBytes(StandardCharsets.US_ASCII));
            args.add(src.getPath());
        }
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveStateMachineTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static Artboard artboard(int file, String name) throws Exception {
        RiveFile f = (RiveFile) loader.loadClass("smgen.Sm" + file).getDeclaredConstructor().newInstance();
        String[] names = f.getArtboardNames();
        for (int i = 0; i < names.length; i++) {
            if (name == null || name.equals(names[i])) {
                return f.instance(i);
            }
        }
        throw new AssertionError("no artboard " + name);
    }

    // "Triggers will only be used on allowed state changes."
    @Test
    void triggersOnlyUsedOnAllowedStateChanges() throws Exception {
        Artboard a = artboard(0, "main");
        assertEquals(1, a.getAnimations().size());
        assertEquals(1, a.getStateMachines().size());
        StateMachine sm = a.getStateMachines().get(0);
        assertEquals(1, sm.getLayerDefs().size());
        assertEquals(1, sm.getInputs().size());
        StateMachine.LayerDef layer = sm.getLayerDefs().get(0);
        assertEquals(5, layer.states.size());
        assertNotNull(layer.any);
        assertNotNull(layer.entry);
        assertNotNull(layer.exit);
        LayerState first = null;
        LayerState second = null;
        for (StateMachine.StateDef d : layer.states) {
            if (d.state instanceof AnimationState) {
                assertNotNull(d.animation);
                if (first == null) {
                    first = d.state;
                } else {
                    second = d.state;
                }
            }
        }
        assertNotNull(second);
        StateMachineInstance smi = new StateMachineInstance(sm, a);
        smi.advance(0.1f);
        a.advance(0.1f);
        assertTrue(smi.fireTrigger("Trigger 1"));
        smi.advance(0.1f);
        assertSame(second, smi.getLayerState(0));
        smi.fireTrigger("Trigger 1");
        smi.advance(0.1f);
        assertSame(first, smi.getLayerState(0));
    }

    // "hit a toggle boolean listener"
    @Test
    void pointerTogglesBooleanListener() throws Exception {
        Artboard a = artboard(1, null);
        StateMachineInstance smi = new StateMachineInstance(a.getStateMachines().get(0), a);
        a.advance(0);
        smi.advance(0);
        assertTrue(smi.getBoolean("On"));
        smi.pointerDown(150, 258);
        smi.pointerUp(150, 258);
        assertFalse(smi.getBoolean("On"));
        smi.pointerDown(150, 258);
        smi.pointerUp(150, 258);
        assertTrue(smi.getBoolean("On"));
        // A press outside the switch changes nothing.
        smi.pointerDown(1, 1);
        smi.pointerUp(1, 1);
        assertTrue(smi.getBoolean("On"));
    }

    // "timeline events load correctly and report"
    @Test
    void timelineEventsReportWithDelay() throws Exception {
        Artboard a = artboard(2, null);
        StateMachineInstance smi = new StateMachineInstance(a.getStateMachines().get(0), a);
        a.advance(0);
        smi.advance(0);
        assertEquals(0, smi.getReportedEvents().size());
        smi.advance(0.4f);
        assertEquals(0, smi.getReportedEvents().size());
        smi.advance(0.2f);
        assertEquals(1, smi.getReportedEvents().size());
        assertEquals("Half", smi.getReportedEvents().get(0).event.getName());
        assertEquals(0.1f, smi.getReportedEvents().get(0).delaySeconds, 0.001f);
    }

    // upstream LinearAnimationInstance reports the reflected leg of a
    // ping-pong advance: an advance that bounces off the end and comes back
    // past an event keyframe reports it again
    @Test
    void pingPongReportsEventsOnTheReflectedLeg() throws Exception {
        Artboard a = artboard(2, null);
        a.getAnimations().get(0).setLoopValue(LinearAnimation.LOOP_PING_PONG);
        StateMachineInstance smi = new StateMachineInstance(a.getStateMachines().get(0), a);
        a.advance(0);
        smi.advance(0);
        smi.advance(0.6f);
        assertEquals(1, smi.getReportedEvents().size());
        // 0.6 -> 1.6 reflects to 0.4: the event at 0.5 is crossed on the way back
        smi.advance(1.0f);
        assertEquals(1, smi.getReportedEvents().size());
        assertEquals("Half", smi.getReportedEvents().get(0).event.getName());
    }
}
