/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Flutter focuses a {@code TextField} only on {@code autofocus: true} or an
 * explicit {@code FocusNode} request -- never just because it is the first
 * field on the page. Codename One's own {@code Form} does the opposite: it
 * gives its initial focus to the first focusable component on arrival, which
 * is why Shrine's Username field used to show a caret at rest where the
 * reference draws none.
 *
 * <p>{@code TextFieldRenderElement.releaseUnrequestedFocus} exists specifically
 * to hand that unrequested focus back, but {@code apply(TextField)} -- its only
 * caller -- runs from {@code createComponent()}, which {@code RenderElement.mount}
 * calls strictly BEFORE it attaches the component to this host's container (see
 * {@code mount}: {@code component = createComponent();} then, only after that
 * returns, {@code host.attach(this)}). So the FIRST call always finds {@code
 * tf.getComponentForm() == null} and used to just give up silently -- fine for
 * a field inside a widget that rebuilds (the next {@code updateComponent} tries
 * again), but Shrine's login never rebuilds once shown, so the release never
 * happened. This mirrors {@code focusField(int)}'s already-existing retry for
 * the opposite problem (focus requested before the field has a Form).</p>
 */
class TextFieldArrivalFocusTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    /**
     * Runs every queued {@code callSerially} runnable synchronously, exactly
     * once. {@code Display.processSerialCalls()} is package-private (it is CN1's
     * own EDT-loop plumbing, never meant for outside callers), and {@code
     * RasterDisplay} already reaches into this exact class's internals with
     * reflection for the same reason: there is no public, synchronous way to
     * pump the serial-call queue in a headless test with no running event loop.
     */
    private static void pumpOnePendingSerialCall() throws Exception {
        Method m = Display.class.getDeclaredMethod("processSerialCalls");
        m.setAccessible(true);
        m.invoke(Display.getInstance());
    }

    @Test
    void theArrivalFocusIsHandedBackEvenOnAStaticPageThatNeverRebuilds() throws Exception {
        // A real Form with a real content pane, wired as this mount's host --
        // reproducing the exact sequence that broke: createComponent() (and its
        // one-shot releaseUnrequestedFocus call) runs before host.attach() ever
        // puts the TextField's component on this Form.
        Form form = new Form();
        RenderHost host = new RenderHost();
        host.form(form);
        host.container(form.getContentPane());

        TextField w = new TextField();
        Element root = FlutterUI.mount(w, host, new BuildOwner());
        assertNotNull(root instanceof TextFieldRenderElement ? root : null,
                "a bare TextField widget mounts straight to its render element");
        TextFieldRenderElement el = (TextFieldRenderElement) root;
        com.codename1.ui.TextField tf = (com.codename1.ui.TextField) el.component();
        assertNotNull(tf, "RasterDisplay gives createComponent a real TextField");
        assertSame(form, tf.getComponentForm(),
                "host.attach() must have run by the time mount() returns");

        // The one thing this test cannot get from the real pipeline: CN1 Forms do
        // not auto-focus a bare, un-shown Form the way show() would on a real
        // Display, so the arrival focus is simulated explicitly -- the exact
        // hand-off releaseUnrequestedFocus exists to undo.
        form.setFocused(tf);
        assertSame(tf, form.getFocused());

        // The retry releaseUnrequestedFocus's first (pre-attach) call scheduled
        // is still sitting in the serial-call queue; run it.
        pumpOnePendingSerialCall();

        assertNull(form.getFocused(),
                "the unrequested arrival focus must be handed back once the field "
                        + "actually has a Form, even though nothing rebuilt it");
    }

    @Test
    void aRealUserTapIsNeverUndoneByTheRelease() throws Exception {
        Form form = new Form();
        RenderHost host = new RenderHost();
        host.form(form);
        host.container(form.getContentPane());

        TextField w = new TextField();
        Element root = FlutterUI.mount(w, host, new BuildOwner());
        TextFieldRenderElement el = (TextFieldRenderElement) root;
        com.codename1.ui.TextField tf = (com.codename1.ui.TextField) el.component();

        // A real tap, simulated the way the field's own pointerPressed listener
        // sees it, BEFORE the queued retry runs.
        tf.pointerPressed(0, 0);
        form.setFocused(tf);

        pumpOnePendingSerialCall();

        assertSame(tf, form.getFocused(),
                "touched -- a field the user actually tapped keeps its focus");
    }
}
