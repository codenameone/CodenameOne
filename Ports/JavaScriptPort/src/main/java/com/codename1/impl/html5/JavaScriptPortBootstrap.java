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

public final class JavaScriptPortBootstrap implements Runnable {
    public static final String APP_CLASS_PROPERTY = JavaScriptBootstrapCoordinator.APP_CLASS_PROPERTY;
    private final Lifecycle lifecycle;

    public JavaScriptPortBootstrap(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public static void bootstrap(Lifecycle lifecycle) {
        com.codename1.impl.ImplementationFactory.setInstance(new com.codename1.impl.ImplementationFactory());
        JavaScriptPortBootstrap bootstrap = new JavaScriptPortBootstrap(lifecycle);
        Display.init(bootstrap);
        bootstrap.run();
    }

    public static Lifecycle createLifecycle(String className) {
        return JavaScriptBootstrapCoordinator.createLifecycle(className);
    }

    @JSBody(params = {}, script = "window.cn1Initialized = true;")
    private static native void setInitialized();

    @JSBody(params = {}, script = "window.cn1Started = true;")
    private static native void setStarted();

    @JSBody(params = {"url"}, script = "var l = window.location; var base=l.protocol+'//'+l.hostname+(l.port?':':'')+l.port; return url.indexOf(base)===0;")
    private static native boolean urlIsSameDomain(String url);

    public static String proxifyUrl(Display display, String url) {
        String doNotProxyList = display.getProperty("javascript.noProxyForDomains", "");
        boolean useProxyForSameDomain = "true".equals(display.getProperty("javascript.useProxyForSameDomain", "false"));
        String proxyURL = ((JSOImplementations.WindowExt)Window.current()).getCorsProxyURL();
        proxyURL = display.getProperty("javascript.proxy.url", proxyURL);
        return JavaScriptRuntimeFacade.proxifyUrl(url, doNotProxyList, useProxyForSameDomain, urlIsSameDomain(url), proxyURL, new JavaScriptRuntimeFacade.UrlEncoder() {
            @Override
            public String encode(String value) {
                return Window.encodeURIComponent(value);
            }
        });
    }

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
