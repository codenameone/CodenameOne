/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.webview;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.flutter.Color;
import com.codename1.flutter.FlutterAssets;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.BrowserNavigationCallback;
import com.codename1.util.SuccessCallback;

import dart.async.Future;
import dart.core.ArgumentError;
import dart.core.DartUri;
import dart.core.UnsupportedError;
import dart.runtime.Funcs;
import dart.typed_data.Uint8List;

/**
 * webview_flutter's WebViewController over a {@link BrowserComponent}, created when
 * a {@link WebViewWidget} first mounts; what was loaded before that is applied
 * then. Navigation requests go through a BrowserNavigationCallback, JavaScript
 * through execute/executeAndReturnString, and each channel is a window object whose
 * postMessage reaches Java through addJSCallback, installed again after every page
 * load.
 *
 * Accepted and not applied: JavaScriptMode.disabled (a BrowserComponent always runs
 * JavaScript), a POST method and body (BrowserComponent loads a URL with GET), the
 * cache and local-storage clears and enableZoom.
 *
 * Without a display there is no browser: the controller keeps the URL and history
 * itself and consults the navigation delegate, firing onPageStarted, onProgress(100)
 * and onPageFinished for an allowed load, so app logic around it still runs.
 */
public class WebViewController {
    private BrowserComponent browser;
    private NavigationDelegate delegate;
    private JavaScriptMode jsMode = JavaScriptMode.disabled;
    private String userAgent;
    private Color background;
    private String pendingUrl;
    private Map<String, String> pendingHeaders;
    private String pendingHtml;
    private String pendingBase;
    private final List<String> history = new ArrayList<String>();
    private int index = -1;
    private final Map<String, Funcs.VoidFunc1<JavaScriptMessage>> channels =
            new LinkedHashMap<String, Funcs.VoidFunc1<JavaScriptMessage>>();
    private final Map<String, SuccessCallback<BrowserComponent.JSRef>> installed =
            new HashMap<String, SuccessCallback<BrowserComponent.JSRef>>();

    public WebViewController() {
    }

    public Future<Object> loadRequest(DartUri uri, LoadRequestMethod method, Object headers, Uint8List body) {
        String url = String.valueOf(uri);
        if (method == LoadRequestMethod.post && Display.isInitialized()) {
            Log.p("webview_flutter: BrowserComponent loads with GET; the POST method and body are not sent",
                    Log.WARNING);
        }
        Map<String, String> h = null;
        if (headers instanceof Map) {
            h = new HashMap<String, String>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) headers).entrySet()) {
                h.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        return load(url, h);
    }

    private Future<Object> load(String url, Map<String, String> headers) {
        if (browser == null) {
            pendingUrl = url;
            pendingHeaders = headers;
            pendingHtml = null;
            navigateHeadless(url);
        } else if (headers != null && !headers.isEmpty()) {
            browser.setURL(url, headers);
        } else {
            browser.setURL(url);
        }
        return Future.value(null);
    }

    public Future<Object> loadHtmlString(String html, String baseUrl) {
        if (browser == null) {
            pendingHtml = html == null ? "" : html;
            pendingBase = baseUrl;
            pendingUrl = null;
            navigateHeadless(baseUrl == null ? "about:blank" : baseUrl);
        } else {
            browser.setPage(html == null ? "" : html, baseUrl);
        }
        return Future.value(null);
    }

    public Future<Object> loadFlutterAsset(String key) {
        FlutterAssets.Resolved r = FlutterAssets.open(WebViewController.class, key);
        if (r == null || r.stream() == null) {
            return Future.error(new ArgumentError("asset not found: " + key));
        }
        InputStream in = r.stream();
        try {
            return loadHtmlString(new String(Util.readInputStream(in), "UTF-8"), null);
        } catch (IOException e) {
            return Future.error(e);
        } finally {
            Util.cleanup(in);
        }
    }

    public Future<Object> loadFile(String absoluteFilePath) {
        String p = absoluteFilePath == null ? "" : absoluteFilePath;
        return load(p.startsWith("file:") ? p : "file://" + p, null);
    }

    public Future<Object> setJavaScriptMode(JavaScriptMode mode) {
        jsMode = mode;
        return Future.value(null);
    }

    public JavaScriptMode javaScriptMode() {
        return jsMode;
    }

    public Future<Object> setNavigationDelegate(NavigationDelegate d) {
        delegate = d;
        return Future.value(null);
    }

    public Future<Object> runJavaScript(String js) {
        if (browser != null && js != null) {
            browser.execute(js);
        }
        return Future.value(null);
    }

    public Future<Object> runJavaScriptReturningResult(String js) {
        if (browser == null) {
            return Future.error(new UnsupportedError("no web view is attached to evaluate JavaScript"));
        }
        return Future.<Object>value(browser.executeAndReturnString(js));
    }

    public Future<Object> addJavaScriptChannel(String name, Funcs.VoidFunc1<JavaScriptMessage> onMessageReceived) {
        channels.put(name, onMessageReceived);
        install(name);
        return Future.value(null);
    }

    public Future<Object> removeJavaScriptChannel(String name) {
        channels.remove(name);
        SuccessCallback<BrowserComponent.JSRef> cb = installed.remove(name);
        if (browser != null && cb != null) {
            browser.removeJSCallback(cb);
            browser.execute("try { delete window[" + quote(name) + "]; } catch (e) {}");
        }
        return Future.value(null);
    }

    /** The registered channel names, in registration order. */
    public List<String> channelNames() {
        return new ArrayList<String>(channels.keySet());
    }

    public Future<Object> reload() {
        if (browser != null) {
            browser.reload();
        } else if (index >= 0) {
            pageLoaded(history.get(index));
        }
        return Future.value(null);
    }

    public Future<Object> goBack() {
        if (browser != null) {
            browser.back();
        } else if (index > 0) {
            index--;
            pageLoaded(history.get(index));
        }
        return Future.value(null);
    }

    public Future<Object> goForward() {
        if (browser != null) {
            browser.forward();
        } else if (index < history.size() - 1) {
            index++;
            pageLoaded(history.get(index));
        }
        return Future.value(null);
    }

    public Future<Boolean> canGoBack() {
        return Future.value(Boolean.valueOf(browser != null ? browser.hasBack() : index > 0));
    }

    public Future<Boolean> canGoForward() {
        return Future.value(Boolean.valueOf(browser != null ? browser.hasForward() : index < history.size() - 1));
    }

    public Future<String> currentUrl() {
        if (browser != null) {
            return Future.value(browser.getURL());
        }
        return Future.value(index >= 0 ? history.get(index) : null);
    }

    public Future<String> getTitle() {
        if (browser == null) {
            return Future.value(null);
        }
        return Future.value(browser.executeAndReturnString("document.title"));
    }

    public Future<Object> setBackgroundColor(Color color) {
        background = color;
        applyBackground();
        return Future.value(null);
    }

    public Future<Object> setUserAgent(String ua) {
        userAgent = ua;
        if (browser != null && ua != null) {
            browser.setProperty("UserAgent", ua);
        }
        return Future.value(null);
    }

    /** Accepted; BrowserComponent offers no cache clear. */
    public Future<Object> clearCache() {
        return Future.value(null);
    }

    /** Accepted; BrowserComponent offers no storage clear. */
    public Future<Object> clearLocalStorage() {
        return Future.value(null);
    }

    /** Accepted and ignored. */
    public Future<Object> enableZoom(boolean enabled) {
        return Future.value(null);
    }

    // --- headless --------------------------------------------------------

    private boolean allowed(String url) {
        if (delegate == null || delegate.onNavigationRequest == null) {
            return true;
        }
        Object d = delegate.onNavigationRequest.call(new NavigationRequest(url));
        // A Future answer cannot be waited for inside a synchronous
        // BrowserNavigationCallback, so only a NavigationDecision value can prevent.
        return d != NavigationDecision.prevent;
    }

    private void navigateHeadless(String url) {
        if (!allowed(url)) {
            return;
        }
        while (history.size() > index + 1) {
            history.remove(history.size() - 1);
        }
        history.add(url);
        index = history.size() - 1;
        pageLoaded(url);
    }

    private void pageLoaded(String url) {
        if (delegate == null) {
            return;
        }
        if (delegate.onPageStarted != null) {
            delegate.onPageStarted.call(url);
        }
        if (delegate.onProgress != null) {
            delegate.onProgress.call(Long.valueOf(100));
        }
        if (delegate.onPageFinished != null) {
            delegate.onPageFinished.call(url);
        }
    }

    // --- the peer --------------------------------------------------------

    /** Creates the BrowserComponent on first use and applies what was loaded before it. */
    BrowserComponent attach() {
        if (browser != null || !Display.isInitialized()) {
            return browser;
        }
        browser = new BrowserComponent();
        browser.setBrowserNavigationCallback(new BrowserNavigationCallback() {
            @Override
            public boolean shouldNavigate(String url) {
                return allowed(url);
            }
        });
        browser.addWebEventListener(BrowserComponent.onStart, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (delegate != null && delegate.onPageStarted != null) {
                    delegate.onPageStarted.call(eventUrl(evt));
                }
            }
        });
        browser.addWebEventListener(BrowserComponent.onLoad, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                installed.clear();
                for (String name : channels.keySet()) {
                    install(name);
                }
                if (delegate != null && delegate.onProgress != null) {
                    delegate.onProgress.call(Long.valueOf(100));
                }
                if (delegate != null && delegate.onPageFinished != null) {
                    delegate.onPageFinished.call(eventUrl(evt));
                }
            }
        });
        browser.addWebEventListener(BrowserComponent.onError, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (delegate != null && delegate.onWebResourceError != null) {
                    Object src = evt.getSource();
                    delegate.onWebResourceError.call(new WebResourceError(-1,
                            src instanceof String ? (String) src : "the page failed to load", browser.getURL()));
                }
            }
        });
        if (userAgent != null) {
            browser.setProperty("UserAgent", userAgent);
        }
        applyBackground();
        if (pendingHtml != null) {
            browser.setPage(pendingHtml, pendingBase);
        } else if (pendingUrl != null) {
            load(pendingUrl, pendingHeaders);
        }
        return browser;
    }

    private String eventUrl(ActionEvent evt) {
        Object src = evt.getSource();
        return src instanceof String ? (String) src : browser.getURL();
    }

    private void applyBackground() {
        if (browser != null && background != null) {
            browser.getAllStyles().setBgColor(background.argb() & 0xffffff);
            browser.getAllStyles().setBgTransparency(background.alpha());
        }
    }

    private void install(final String name) {
        if (browser == null || installed.containsKey(name)) {
            return;
        }
        SuccessCallback<BrowserComponent.JSRef> cb = new SuccessCallback<BrowserComponent.JSRef>() {
            @Override
            public void onSucess(BrowserComponent.JSRef value) {
                Funcs.VoidFunc1<JavaScriptMessage> h = channels.get(name);
                if (h != null) {
                    h.call(new JavaScriptMessage(value == null ? "" : value.getValue()));
                }
            }
        };
        installed.put(name, cb);
        browser.addJSCallback("window[" + quote(name) + "] = {postMessage: function(m) { callback.onSuccess(String(m)); }};", cb);
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("'");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '\'') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.append('\'').toString();
    }
}
