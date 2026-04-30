/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.push.PushCallback;
import com.codename1.system.Lifecycle;

/**
 * Minimal bootstrap coordination shared between TeaVM-backed startup and the
 * ParparVM-compatible runtime surface.
 */
public final class JavaScriptBootstrapCoordinator {
    public static final String APP_CLASS_PROPERTY = "codename1.javascript.appClass";

    private JavaScriptBootstrapCoordinator() {
    }

    public interface PushCallbackRegistrar {
        void register(PushCallback callback);
    }

    public static Lifecycle createLifecycle(String className) {
        try {
            return (Lifecycle) Class.forName(className).newInstance();
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to instantiate application lifecycle " + className, ex);
        }
    }

    public static void bindMainClass(Object main, PushCallbackRegistrar... registrars) {
        if (!(main instanceof PushCallback)) {
            return;
        }
        PushCallback callback = (PushCallback) main;
        if (registrars == null) {
            return;
        }
        for (PushCallbackRegistrar registrar : registrars) {
            if (registrar != null) {
                registrar.register(callback);
            }
        }
    }
}
