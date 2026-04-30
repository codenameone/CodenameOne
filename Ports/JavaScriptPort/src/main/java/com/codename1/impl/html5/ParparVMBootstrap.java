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

    @JSBody(params = {}, script = "window.cn1Initialized = true;")
    private static native void setInitialized();

    @JSBody(params = {}, script = "window.cn1Started = true;")
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