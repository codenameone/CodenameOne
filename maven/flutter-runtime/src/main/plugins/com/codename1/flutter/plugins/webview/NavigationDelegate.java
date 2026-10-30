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

import dart.runtime.Funcs;

/**
 * webview_flutter's NavigationDelegate. onUrlChange and onHttpAuthRequest are
 * accepted and never called: BrowserComponent reports neither.
 */
public class NavigationDelegate {
    Funcs.Func1<NavigationRequest, Object> onNavigationRequest;
    Funcs.VoidFunc1<String> onPageStarted;
    Funcs.VoidFunc1<String> onPageFinished;
    Funcs.VoidFunc1<Long> onProgress;
    Funcs.VoidFunc1<WebResourceError> onWebResourceError;

    public NavigationDelegate() {
    }

    public void onNavigationRequest(Funcs.Func1<NavigationRequest, Object> v) {
        onNavigationRequest = v;
    }

    public void onPageStarted(Funcs.VoidFunc1<String> v) {
        onPageStarted = v;
    }

    public void onPageFinished(Funcs.VoidFunc1<String> v) {
        onPageFinished = v;
    }

    public void onProgress(Funcs.VoidFunc1<Long> v) {
        onProgress = v;
    }

    public void onWebResourceError(Funcs.VoidFunc1<WebResourceError> v) {
        onWebResourceError = v;
    }

    /** Accepted and ignored. */
    public void onUrlChange(Object v) {
    }

    /** Accepted and ignored. */
    public void onHttpAuthRequest(Object v) {
    }
}
