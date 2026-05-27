/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
        com.codename1.impl.ImplementationFactory.setInstance(new com.codename1.impl.ImplementationFactory());
        ParparVMBootstrap bootstrap = new ParparVMBootstrap(lifecycle);
        Display.init(bootstrap);
        bootstrap.run();
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
            + "var __cn1LifecycleMsg = {type: 'lifecycle', phase: 'started'};"
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