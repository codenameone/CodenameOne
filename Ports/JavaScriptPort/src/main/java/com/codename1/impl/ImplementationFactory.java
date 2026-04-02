/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl;

import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.impl.html5.JavaScriptPortBootstrap;
import com.codename1.impl.html5.URLProxifier;
import com.codename1.ui.Display;

public class ImplementationFactory {
    private static ImplementationFactory instance = new ImplementationFactory();

    public ImplementationFactory() {
    }

    public static ImplementationFactory getInstance() {
        return instance;
    }

    public static void setInstance(ImplementationFactory factory) {
        instance = factory;
    }

    public Object createImplementation() {
        HTML5Implementation implementation = new HTML5Implementation();
        implementation.setUrlProxifier(new URLProxifier() {
            @Override
            public String proxifyURL(String url) {
                return JavaScriptPortBootstrap.proxifyUrl(Display.getInstance(), url);
            }
        });
        return implementation;
    }
}
