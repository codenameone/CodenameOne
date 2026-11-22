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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.rive.transcoder.RiveTranscoder;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.AnimationEvent;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.File;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `RiveImage` behaviour through the core `Image` API: playing by default,
/// pausing, inputs while paused, pointer mapping through a `Label` icon, drags
/// that do not scroll the parent, and events reported by the entry state.
class RiveImageTest extends UITestBase {
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
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveImageTest.class.getClassLoader());
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

    /// Only core API: what an application holds is an Image.
    private static Image image(int i) throws Exception {
        return new RiveImage(file(i));
    }

    @FormTest
    void playsByDefaultAndPauses() throws Exception {
        Image v = image(0);
        assertTrue(v.isAnimation());
        assertTrue(v.isAnimationPlaying());
        v.setAnimationPlaying(false);
        assertFalse(v.isAnimationPlaying());
        v.setAnimationPlaying(true);
        assertTrue(v.isAnimationPlaying());
    }

    @FormTest
    void scaledIsIndependentAndSized() throws Exception {
        Image v = image(0);
        Image s = v.scaled(123, 45);
        assertEquals(123, s.getWidth());
        assertEquals(45, s.getHeight());
        assertNotSame(((RiveImage) v).machineInstance(), ((RiveImage) s).machineInstance());
    }

    @FormTest
    void playRewindsAFinishedOneShot() throws Exception {
        // state_machine_triggers has no machine on some artboards; any file
        // with only an animation plays it once and stops
        Image v = image(1);
        v.setAnimationPlaying(false);
        v.advanceAnimation(1000f);
        v.setAnimationPlaying(true);
        assertTrue(v.isAnimationPlaying());
    }

    @FormTest
    void inputsTakeEffectWhilePaused() throws Exception {
        RiveImage v = new RiveImage(file(0));
        v.setAnimationPlaying(false);
        StateMachineInstance m = v.machineInstance();
        LayerState before = m.getLayerState(0);
        v.setAnimationInput("On", !m.getBoolean("On"));
        assertFalse(m.needsAdvance(), "the change was settled at once");
        assertNotSame(before, m.getLayerState(0), "the state machine moved to the other state");
    }

    @FormTest
    void tapOnALabelIconMapsThroughPaddingAndLetterboxing() throws Exception {
        Form f = form();
        f.setLayout(new BorderLayout());
        RiveImage img = (RiveImage) new RiveImage(file(0)).scaled(300, 200);
        img.setAnimationPlaying(false);
        assertTrue(img.isInteractive());
        Label l = new Label(img);
        l.getAllStyles().setPadding(10, 20, 30, 40);
        l.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        l.getAllStyles().setAlignment(com.codename1.ui.Component.CENTER);
        l.setGap(0);
        f.add(BorderLayout.CENTER, l);
        f.revalidate();
        // the hit test is where the icon was painted
        paintOnce(f);
        Artboard a = file(0).instance(null);
        float scale = Math.min(300 / a.getWidth(), 200 / a.getHeight());
        // the icon is centered in the label's inner area
        int iconX = 40 + (l.getWidth() - 40 - 20 - 300) / 2;
        int iconY = 10 + (l.getHeight() - 10 - 30 - 200) / 2;
        int sx = Math.round(l.getAbsoluteX() + iconX + (300 - a.getWidth() * scale) / 2f + 150 * scale);
        int sy = Math.round(l.getAbsoluteY() + iconY + (200 - a.getHeight() * scale) / 2f + 258 * scale);
        StateMachineInstance m = img.machineInstance();
        boolean on = m.getBoolean("On");
        // a tap on the padding, outside the icon, never reaches it
        l.pointerPressed(l.getAbsoluteX() + 2, l.getAbsoluteY() + 2);
        l.pointerReleased(sx, sy);
        assertEquals(on, m.getBoolean("On"), "a press outside the icon is not the icon's");
        l.pointerPressed(sx, sy);
        l.pointerReleased(sx, sy);
        assertEquals(!on, m.getBoolean("On"), "the tap reached the listener");
    }

    private static void paintOnce(Form f) {
        Image img = Image.createImage(Math.max(1, f.getWidth()), Math.max(1, f.getHeight()));
        f.paintComponent(img.getGraphics(), true);
    }

    @FormTest
    void dragInsideDoesNotScrollTheParent() throws Exception {
        // control: the same drag over a plain row does scroll in this harness
        Container plain = scrollingForm(null);
        drag(plain, plain.getAbsoluteY() + plain.getHeight() * 3 / 4);
        assertTrue(plain.getScrollY() > 0, "a drag over a plain row scrolls");

        Label v = new com.codename1.components.ScaleImageLabel(image(0));
        Container content = scrollingForm(v);
        drag(content, v.getAbsoluteY() + v.getHeight() * 3 / 4);
        assertEquals(0, content.getScrollY(), "the state machine owns the drag");
    }

    @FormTest
    void dragOverARowListenerDoesNotScrollTheParent() throws Exception {
        // the fixture's own machine has no listener; only its list rows do
        Artboard ab = file(4).instance((String) null);
        StateMachineInstance top = new StateMachineInstance(ab.getStateMachines().get(0), ab);
        top.advance(0);
        assertTrue(top.getStateMachine().getListenerDefs().isEmpty(), "top-level machine has no listeners");
        assertTrue(top.hasPointerListeners(), "a row machine's listener counts");
        Label v = new com.codename1.components.ScaleImageLabel(image(4));
        Container content = scrollingForm(v);
        drag(content, v.getAbsoluteY() + v.getHeight() * 3 / 4);
        assertEquals(0, content.getScrollY(), "a row machine's listener owns the drag");
    }

    private Container scrollingForm(Label v) {
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
        paintOnce(f);
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
        Image v = image(2);
        v.setAnimationPlaying(false);
        final List<String> names = new ArrayList<String>();
        v.addAnimationListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                names.add(((AnimationEvent) e).getName());
                assertNotNull(((AnimationEvent) e).getProperties());
            }
        });
        v.advanceAnimation(0);
        flushSerialCalls();
        assertTrue(names.size() >= 1, "entry event lost");
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
            Image v = image(i);
            v.setAnimationPlaying(false);
            final int[] got = new int[1];
            v.addAnimationListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    got[0]++;
                }
            });
            v.advanceAnimation(0);
            flushSerialCalls();
            assertTrue(got[0] >= expected, FILES[i] + ": entry events lost");
        }
    }

    @FormTest
    void plainImagesAreNotInteractive() {
        Image plain = Image.createImage(4, 4);
        assertFalse(plain.isInteractive());
        assertFalse(plain.isAnimationPlaying());
        plain.setAnimationProperty("x", Float.valueOf(1));
        assertEquals(null, plain.getAnimationProperty("x"));
    }
}
