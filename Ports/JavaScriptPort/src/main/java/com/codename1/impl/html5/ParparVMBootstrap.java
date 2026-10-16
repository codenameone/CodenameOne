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
package com.codename1.impl.html5;

import com.codename1.io.Log;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Display;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.Event;

/**
 * Bootstrap for ParparVM JavaScript builds.
 * Uses parparvm_runtime.js for JS interop via native method bindings.
 */
public final class ParparVMBootstrap implements Runnable {
    private final Lifecycle lifecycle;

    public ParparVMBootstrap(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public static void bootstrap(Lifecycle lifecycle) {
        bootstrap(lifecycle, null);
    }

    /**
     * As {@link #bootstrap(Lifecycle)}, but runs {@code afterInit} once {@code Display} is
     * initialized and before the lifecycle's {@code init}/{@code start} callbacks. The generated
     * launcher uses this to stamp the app-hardening metadata (so {@code Hardening.isHardened()} and
     * any crash raised during {@code init}/{@code start} already see the mapping id and level).
     * The lifecycle itself then runs on the EDT, so {@code afterInit} always runs first, and this
     * call returns only once {@code start} has returned.
     *
     * @param lifecycle the application lifecycle
     * @param afterInit code to run after {@code Display.init} and before the lifecycle starts; may be null
     */
    public static void bootstrap(Lifecycle lifecycle, Runnable afterInit) {
        com.codename1.impl.ImplementationFactory.setInstance(new com.codename1.impl.ImplementationFactory());
        ParparVMBootstrap bootstrap = new ParparVMBootstrap(lifecycle);
        Display.init(bootstrap);
        if (afterInit != null) {
            afterInit.run();
        }
        // The lifecycle runs on the EDT, as it does on every other port (iOS hands its stub
        // to Display.init, which runs it there). This used to call run() inline, on the
        // worker's main thread, so init() and start() ran off the EDT: anything that checks
        // -- a transpiled Flutter app's runApp asserts it -- threw before the first frame,
        // and the transpiled Flutter gallery never got past its loading screen.
        //
        // It must still be WAITED for here, not just queued. The runtime reports the app
        // as started when this (main) thread finishes -- parparvm_runtime.js posts the
        // ``lifecycle``/``started`` message from its drain loop, and browser_bridge.js
        // turns that into the page's ``window.cn1Started``. Queuing and returning let the
        // main thread finish before init() and start() had even run, so cn1Started went
        // true while the first form was not yet shown: the Playground editor-input check
        // clicked into an empty display and the click was lost. Blocking until the
        // lifecycle has run keeps "the main thread finished" meaning "start() returned",
        // as it did when run() was called inline. run() catches everything, so the
        // wrapper always completes and this wait always ends.
        Display.getInstance().callSeriallyAndWait(bootstrap);
    }

    // ``window.cn1Initialized = true`` lands on the worker's global
    // (window === self inside the worker), but the headless test
    // harness and every other main-thread consumer reads its own
    // ``window.cn1Initialized``. The bridge (browser_bridge.js)
    // already flips its main-thread copy when ``startParparVmApp``
    // runs, so the worker side is best-effort — the real signal
    // travels through the message-passing channel instead.
    @JSBody(params = {}, script = "window.cn1Initialized = true;")
    private static native void setInitialized();

    // For ``cn1Started`` we need the same main-thread signal but
    // there's no ``startParparVmApp``-style hook on this side. The
    // worker emits a ``{type: 'lifecycle', phase: 'started'}`` VM
    // message at the same time so ``browser_bridge.js`` can flip
    // its own ``cn1Started``. Fall back gracefully when neither
    // ``parentPort`` (Node worker_threads) nor ``self.postMessage``
    // (browser Worker) is available — that path applies to direct
    // in-page invocations from the JavaScript-port simulator.
    @JSBody(params = {}, script = ""
            + "window.cn1Started = true;"
            + "var __cn1LifecycleMsg = {type: 'lifecycle', phase: 'started', source: 'bootstrap'};"
            + "if (typeof parentPort !== 'undefined' && parentPort && typeof parentPort.postMessage === 'function') {"
            + "  parentPort.postMessage(__cn1LifecycleMsg);"
            + "} else if (typeof self !== 'undefined' && self !== this && typeof self.postMessage === 'function') {"
            + "  self.postMessage(__cn1LifecycleMsg);"
            + "} else if (typeof postMessage === 'function') {"
            + "  postMessage(__cn1LifecycleMsg);"
            + "}")
    private static native void setStarted();

    @Override
    public void run() {
        try {
            HTML5Implementation.setMainClass(lifecycle);
            dispatchEvent("beforecn1init", 201);
            lifecycle.init(this);
            setInitialized();
            dispatchEvent("aftercn1init", 202);
            dispatchEvent("beforecn1start", 203);
            lifecycle.start();
            setStarted();
            dispatchEvent("aftercn1start", 204);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static void dispatchEvent(String type, int code) {
        Event evt = HTML5Implementation.createCustomEvent(type, "", code);
        Window.current().dispatchEvent(evt);
    }
}