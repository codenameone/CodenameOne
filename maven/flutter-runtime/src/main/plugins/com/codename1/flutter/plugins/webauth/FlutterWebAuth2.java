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
package com.codename1.flutter.plugins.webauth;

import com.codename1.flutter.platform.PlatformException;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.BrowserNavigationCallback;
import com.codename1.ui.layouts.BorderLayout;

import dart.async.Completer;
import dart.async.Future;

/**
 * flutter_web_auth_2's {@code FlutterWebAuth2}: the sign-in page opens in a
 * BrowserComponent on a form of its own, and the first navigation to the callback scheme
 * (for {@code https}, to {@code httpsHost} and {@code httpsPath}) completes the future with
 * that URL and closes the form. Back or Cancel fails it with
 * {@code PlatformException(code: 'CANCELED')}, as the plugin does. Without a display or a
 * native browser it fails with {@code PlatformException(code: 'UNAVAILABLE')}.
 */
public final class FlutterWebAuth2 {

    private FlutterWebAuth2() {
    }

    static boolean matches(String url, String scheme, FlutterWebAuth2Options options) {
        if (url == null || scheme == null || url.length() <= scheme.length()
                || url.charAt(scheme.length()) != ':' || !url.regionMatches(true, 0, scheme, 0, scheme.length())) {
            return false;
        }
        boolean web = "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
        if (!web) {
            return true;
        }
        // An http(s) callback is recognised by host (and path) only; matching on the
        // scheme alone would end the flow on the sign-in page itself.
        String host = options == null ? null : options.httpsHost();
        if (host == null) {
            return false;
        }
        String rest = url.substring(scheme.length() + 1);
        while (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        if (!rest.regionMatches(true, 0, host, 0, host.length())) {
            return false;
        }
        String afterHost = rest.substring(host.length());
        if (afterHost.length() > 0 && "/?#:".indexOf(afterHost.charAt(0)) < 0) {
            return false;
        }
        String path = options.httpsPath();
        return path == null || afterHost.startsWith(path.startsWith("/") ? path : "/" + path);
    }

    public static Future<String> authenticate(final String url, final String callbackUrlScheme,
            final FlutterWebAuth2Options options) {
        if (!Display.isInitialized() || !BrowserComponent.isNativeBrowserSupported()) {
            return Future.error(new PlatformException("UNAVAILABLE",
                    "No browser to show the sign-in page in", null));
        }
        final Completer<String> done = new Completer<String>();
        final Form previous = Display.getInstance().getCurrent();
        final Form form = new Form("", new BorderLayout());
        final BrowserComponent browser = new BrowserComponent();
        final Command cancel = new Command("Cancel") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                finish(done, previous, null);
            }
        };
        browser.addBrowserNavigationCallback(new BrowserNavigationCallback() {
            @Override
            public boolean shouldNavigate(final String target) {
                if (!matches(target, callbackUrlScheme, options)) {
                    return true;
                }
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        finish(done, previous, target);
                    }
                });
                return false;
            }
        });
        form.add(BorderLayout.CENTER, browser);
        form.setBackCommand(cancel);
        if (form.getToolbar() != null) {
            form.getToolbar().addCommandToLeftBar(cancel);
        } else {
            form.addCommand(cancel);
        }
        browser.setURL(url);
        form.show();
        return done.future();
    }

    static void finish(Completer<String> done, Form previous, String callback) {
        if (done.isCompleted()) {
            return;
        }
        if (callback == null) {
            done.completeError(new PlatformException("CANCELED", "User canceled login", null));
        } else {
            done.complete(callback);
        }
        if (previous != null) {
            previous.showBack();
        }
    }
}
