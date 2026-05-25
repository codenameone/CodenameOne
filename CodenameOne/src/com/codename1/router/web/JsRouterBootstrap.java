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
package com.codename1.router.web;

import com.codename1.router.BrowserHistoryBridge;
import com.codename1.router.Location;
import com.codename1.router.LocationListener;
import com.codename1.router.Router;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.MessageEvent;

/// Wires `Router` to the browser's `window.history` on the JavaScript port.
///
/// Pair this class with the small JS shim `cn1-router-history.js` that ships
/// alongside it. All messages between app and shim flow through CN1's
/// `MessageEvent` mechanism using the integer code `#MESSAGE_CODE` and a
/// message payload of the form `verb:path`:
///
/// ```text
///   push:/path     // app -> shim: history.pushState(/path)
///   replace:/path  // app -> shim: history.replaceState(/path)
///   pop:/path      // shim -> app: browser back; path is the new top
///   push:/path     // shim -> app: a JS-side navigation we should mirror
/// ```
///
/// Usage in a CN1 app's `init` (JS port only -- wrap in a platform check):
///
/// ```java
/// if ("HTML5".equals(Display.getInstance().getPlatformName())) {
///     JsRouterBootstrap.install();
/// }
/// ```
public final class JsRouterBootstrap {

    /// Integer code carried on every router-history `MessageEvent`. The JS shim
    /// filters incoming events by this code and the Java side filters incoming
    /// messages from the shim by the same code.
    public static final int MESSAGE_CODE = 0x43524831; // "CRH1"

    private static boolean installed;

    private JsRouterBootstrap() {
    }

    /// Installs the bridge. Safe to call multiple times; subsequent calls are
    /// no-ops.
    public static void install() {
        if (installed) {
            return;
        }
        installed = true;

        final Router router = Router.getInstance();

        router.setBrowserHistoryBridge(new BrowserHistoryBridge() {
            @Override
            public void onPush(Location loc) {
                send("push:" + loc.getPath());
            }
            @Override
            public void onReplace(Location loc) {
                send("replace:" + loc.getPath());
            }
            @Override
            public void onPop(Location current) {
                // Browser-back navigates the browser history itself -- when the
                // router pops for any other reason we still align the JS URL.
                send("replace:" + current.getPath());
            }
            @Override
            public String getInitialPath() {
                return Display.getInstance().getProperty("AppArg", null);
            }
        });

        Display.getInstance().addMessageListener(new ActionListener<MessageEvent>() {
            @Override
            public void actionPerformed(MessageEvent e) {
                if (e.getCode() != MESSAGE_CODE) {
                    return;
                }
                String payload = e.getMessage();
                if (payload == null) {
                    return;
                }
                int colon = payload.indexOf(':');
                if (colon < 0) {
                    return;
                }
                String verb = payload.substring(0, colon);
                String path = payload.substring(colon + 1);
                if ("pop".equals(verb)) {
                    router.onBrowserNavigated(path, LocationListener.Kind.POP);
                } else if ("push".equals(verb)) {
                    router.onBrowserNavigated(path, LocationListener.Kind.PUSH);
                } else if ("replace".equals(verb)) {
                    router.onBrowserNavigated(path, LocationListener.Kind.REPLACE);
                }
            }
        });
    }

    private static void send(String payload) {
        Display.getInstance().dispatchMessage(new MessageEvent(Router.class, payload, MESSAGE_CODE));
    }
}
