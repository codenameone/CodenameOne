/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.system.Lifecycle;

/**
 * Default JavaScript-port entrypoint.
 */
public class Stub {
    public static void main(String[] args) {
        String lifecycleClass = System.getProperty(JavaScriptBootstrapCoordinator.APP_CLASS_PROPERTY, "com.codename1.system.Lifecycle");
        Lifecycle lifecycle = JavaScriptBootstrapCoordinator.createLifecycle(lifecycleClass);
        JavaScriptPortBootstrap.bootstrap(lifecycle);
    }
}
