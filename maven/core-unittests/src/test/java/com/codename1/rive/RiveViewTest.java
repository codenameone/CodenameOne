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

import com.codename1.impl.rive.Artboard;
import com.codename1.impl.rive.LayerState;
import com.codename1.impl.rive.StateMachineInstance;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.rive.transcoder.RiveTranscoder;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `RiveView` behaviour on a form: playing by default, pausing, inputs while
/// paused, pointer mapping through padding and letterboxing, drags and
/// hover, and events reported by the entry state.
class RiveViewTest extends UITestBase {
    private static final String FIXTURES = "../rive-transcoder/src/test/resources/rive/";
    private static final String[] FILES = {"light_switch.riv", "state_machine_triggers.riv", "events_on_states.riv", "hit_test_solos.riv",
        "component_list_row_click.riv"};

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
        File pkg = temporary.resolve("rvgen").toFile();
        assertTrue(pkg.mkdirs());
        for (int i = 0; i < FILES.length; i++) {
            byte[] riv = Files.readAllBytes(new File(FIXTURES + FILES[i]).toPath());
            File src = new File(pkg, "Rv" + i + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "rvgen", "Rv" + i).getBytes(StandardCharsets.US_ASCII));
            args.add(src.getPath());
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveViewTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static RiveFile file(int i) throws Exception {
        return (RiveFile) loader.loadClass("rvgen.Rv" + i).getDeclaredConstructor().newInstance();
    }

    private static Form form() {
        Form f = Display.getInstance().getCurrent();
        f.removeAll();
        return f;
    }

    @SuppressWarnings("unchecked")
    private static boolean registered(Form f, Object c) throws Exception {
        Field fl = Form.class.getDeclaredField("animatableComponents");
        fl.setAccessible(true);
        List<Object> l = (List<Object>) fl.get(f);
        return l != null && l.contains(c);
    }

    @FormTest
    void playsByDefaultPausesAndStopsWhenRemoved() throws Exception {
        Form f = form();
        f.setLayout(new BorderLayout());
        RiveView v = new RiveView(file(0));
        assertTrue(v.isPlaying());
        f.add(BorderLayout.CENTER, v);
        f.revalidate();
        assertTrue(registered(f, v), "a shown view runs on the form's clock");
        v.pause();
        assertFalse(v.isPlaying());
        assertFalse(registered(f, v));
        v.play();
        assertTrue(registered(f, v));
        f.removeComponent(v);
        assertFalse(registered(f, v), "a removed view stops animating");
    }

    @FormTest
    void playRewindsAFinishedOneShot() throws Exception {
        RiveView v = new RiveView(file(1));
        v.setAnimation(file(1).instance(null).getAnimations().get(0).getName());
        v.pause();
        v.advance(1000f);
        v.play();
        assertTrue(v.isPlaying());
    }

    @FormTest
    void inputsTakeEffectWhilePaused() throws Exception {
        RiveView v = new RiveView(file(0));
        v.pause();
        StateMachineInstance m = v.machineInstance();
        LayerState before = m.getLayerState(0);
        v.setBoolean("On", !m.getBoolean("On"));
        assertFalse(m.needsAdvance(), "the change was settled at once");
        assertNotSame(before, m.getLayerState(0), "the state machine moved to the other state");
    }

    @FormTest
    void tapMapsThroughPaddingAndLetterboxing() throws Exception {
        Form f = form();
        f.setLayout(new BorderLayout());
        RiveView v = new RiveView(file(0));
        v.pause();
        v.getAllStyles().setPadding(10, 20, 30, 40);
        v.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        f.add(BorderLayout.CENTER, v);
        f.revalidate();
        Artboard a = file(0).instance(null);
        int iw = v.getInnerWidth();
        int ih = v.getInnerHeight();
        float scale = Math.min(iw / a.getWidth(), ih / a.getHeight());
        int sx = Math.round(v.getAbsoluteX() + v.getStyle().getPaddingLeft(false)
                + (iw - a.getWidth() * scale) / 2f + 150 * scale);
        int sy = Math.round(v.getAbsoluteY() + v.getStyle().getPaddingTop()
                + (ih - a.getHeight() * scale) / 2f + 258 * scale);
        StateMachineInstance m = v.machineInstance();
        boolean on = m.getBoolean("On");
        v.pointerPressed(sx, sy);
        v.pointerReleased(sx, sy);
        assertEquals(!on, m.getBoolean("On"), "the tap reached the listener");
    }

    // the hover half of upstream's "hit test on solos": hovering the shape at
    // (200, 100) sets "hovered" through an enter listener, leaving clears it
    @FormTest
    void hoverFiresEnterAndLeavingFiresExit() throws Exception {
        Form f = form();
        f.setLayout(new BorderLayout());
        RiveView v = new RiveView(file(3));
        v.pause();
        f.add(BorderLayout.CENTER, v);
        f.revalidate();
        Artboard a = file(3).instance(null);
        int iw = v.getInnerWidth();
        int ih = v.getInnerHeight();
        float scale = Math.min(iw / a.getWidth(), ih / a.getHeight());
        int sx = Math.round(v.getAbsoluteX() + v.getStyle().getPaddingLeft(false)
                + (iw - a.getWidth() * scale) / 2f + 200 * scale);
        int sy = Math.round(v.getAbsoluteY() + v.getStyle().getPaddingTop()
                + (ih - a.getHeight() * scale) / 2f + 100 * scale);
        StateMachineInstance m = v.machineInstance();
        assertFalse(m.getBoolean("hovered"));
        v.pointerHover(new int[]{sx}, new int[]{sy});
        assertTrue(m.getBoolean("hovered"), "hover fired the enter listener");
        int out = v.getAbsoluteX() + v.getWidth() + 50;
        v.pointerHover(new int[]{out}, new int[]{sy});
        assertFalse(m.getBoolean("hovered"), "leaving the view fired the exit listener");
    }

    @FormTest
    void dragInsideDoesNotScrollTheParent() throws Exception {
        // control: the same drag over a plain row does scroll in this harness
        Container plain = scrollingForm(null);
        drag(plain, plain.getAbsoluteY() + plain.getHeight() * 3 / 4);
        assertTrue(plain.getScrollY() > 0, "a drag over a plain row scrolls");

        RiveView v = new RiveView(file(0));
        v.pause();
        Container content = scrollingForm(v);
        drag(content, v.getAbsoluteY() + v.getHeight() * 3 / 4);
        assertEquals(0, content.getScrollY(), "the state machine owns the drag");
    }

    @FormTest
    void dragOverARowListenerDoesNotScrollTheParent() throws Exception {
        // the fixture's own machine has no listener; only its list rows do
        com.codename1.impl.rive.Artboard ab = file(4).instance((String) null);
        com.codename1.impl.rive.StateMachineInstance top = new com.codename1.impl.rive.StateMachineInstance(
                ab.getStateMachines().get(0), ab);
        top.advance(0);
        assertTrue(top.getStateMachine().getListenerDefs().isEmpty(), "top-level machine has no listeners");
        assertTrue(top.hasPointerListeners(), "a row machine's listener counts");
        RiveView v = new RiveView(file(4));
        v.pause();
        Container content = scrollingForm(v);
        drag(content, v.getAbsoluteY() + v.getHeight() * 3 / 4);
        assertEquals(0, content.getScrollY(), "a row machine's listener owns the drag");
    }

    private Container scrollingForm(RiveView v) {
        Form f = new Form("drag", BoxLayout.y());
        Container content = f.getContentPane();
        content.setScrollableY(true);
        if (v != null) {
            v.setPreferredH(content.getHeight() > 0 ? content.getHeight() : 800);
            content.add(v);
        }
        for (int i = 0; i < 200; i++) {
            content.add(new Label("Row " + i));
        }
        f.show();
        f.revalidate();
        flushSerialCalls();
        if (v != null) {
            v.setPreferredH(content.getHeight() * 9 / 10);
            f.revalidate();
        }
        return content;
    }

    // as PointerCancelTest drives a scroll: the press through Display's queue
    private void drag(Container content, int y) {
        int x = content.getAbsoluteX() + content.getWidth() / 2;
        display.pointerPressed(new int[]{x}, new int[]{y});
        flushSerialCalls();
        implementation.setHasDragStarted(true);
        for (int i = 1; i <= 10; i++) {
            implementation.dispatchPointerDrag(x, y - i * content.getHeight() / 20);
        }
        flushSerialCalls();
        implementation.setHasDragStarted(false);
    }

    @FormTest
    void entryEventIsDelivered() throws Exception {
        // events_on_states reports "First" from its entry state
        RiveView v = new RiveView(file(2));
        v.pause();
        final List<String> names = new ArrayList<String>();
        v.addEventListener(new RiveEventListener() {
            @Override
            public void onRiveEvent(RiveEvent e) {
                names.add(e.getName());
            }
        });
        v.advance(0);
        assertTrue(names.size() >= 1, "entry event lost");
        assertEquals("First", names.get(0));
    }

    @FormTest
    void entryEventIsHeldAfterSetStateMachine() throws Exception {
        // events_on_states reports "First" from its entry state; choosing the
        // machine again with nobody listening must hold it for the first listener
        RiveView v = new RiveView(file(2));
        v.pause();
        RiveEventListener drain = new RiveEventListener() {
            @Override
            public void onRiveEvent(RiveEvent e) {
            }
        };
        v.addEventListener(drain);
        v.advance(0);
        v.removeEventListener(drain);
        v.setStateMachine("State Machine 1");
        final List<String> names = new ArrayList<String>();
        v.addEventListener(new RiveEventListener() {
            @Override
            public void onRiveEvent(RiveEvent e) {
                names.add(e.getName());
            }
        });
        v.advance(0);
        assertTrue(names.size() >= 1, "entry event lost after setStateMachine");
        assertEquals("First", names.get(0));
    }

    @FormTest
    void entryEventsAreDelivered() throws Exception {
        for (int i = 0; i < FILES.length; i++) {
            Artboard a = file(i).instance(null);
            int expected = 0;
            if (!a.getStateMachines().isEmpty()) {
                StateMachineInstance raw = new StateMachineInstance(a.getStateMachines().get(0), a);
                raw.advance(0);
                expected = raw.getReportedEvents().size();
            }
            RiveView v = new RiveView(file(i));
            v.pause();
            final int[] got = new int[1];
            v.addEventListener(new RiveEventListener() {
                @Override
                public void onRiveEvent(RiveEvent e) {
                    got[0]++;
                }
            });
            v.advance(0);
            assertTrue(got[0] >= expected, FILES[i] + ": entry events lost");
        }
    }

    @FormTest
    void nullNamesThrow() throws Exception {
        final RiveView v = new RiveView(file(0));
        assertThrows(IllegalArgumentException.class, () -> v.setAnimation(null));
        assertThrows(IllegalArgumentException.class, () -> v.setStateMachine(null));
    }
}
