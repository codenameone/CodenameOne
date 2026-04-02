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

/**
 * Bootstrap for ParparVM JavaScript builds.
 * This is a minimal bootstrap that doesn't depend on TeaVM JSO classes.
 * The browser runtime handles JS interop via parparvm_runtime.js and browser_bridge.js.
 */
public final class ParparVMBootstrap implements Runnable {
    private final Lifecycle lifecycle;

    public ParparVMBootstrap(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public static void bootstrap(Lifecycle lifecycle) {
        // Note: ImplementationFactory is set by the translated app's static initializers
        // For ParparVM, native bindings are provided by parparvm_runtime.js
        ParparVMBootstrap bootstrap = new ParparVMBootstrap(lifecycle);
        Display.init(bootstrap);
        Display.getInstance().callSerially(bootstrap);
    }

    @Override
    public void run() {
        try {
            lifecycle.init(this);
            lifecycle.start();
        } catch (Throwable t) {
            Log.e(t);
        }
    }
}