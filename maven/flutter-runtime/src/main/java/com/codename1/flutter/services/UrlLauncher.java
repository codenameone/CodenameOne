/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.services;

import com.codename1.ui.Display;

import dart.async.Future;
import dart.core.DartUri;
import dart.runtime.Funcs;

/**
 * The top-level functions of the {@code url_launcher} package new_gallery calls
 * to open external links (the About / settings pages). Mirrored onto Codename
 * One's {@link Display#execute(String)} and {@link Display#canExecute(String)}.
 *
 * <p>Each returns an already-completed {@link Future} so a non-awaited call
 * transpiles and runs to completion; {@code launchUrl} additionally fires the
 * native open. The {@code mode}/{@code webOnlyWindowName} options are accepted
 * for API shape.</p>
 */
public final class UrlLauncher {

    private UrlLauncher() {
    }

    /** {@code url_launcher}'s {@code launchUrl(url)}. */
    public static Future<Boolean> launchUrl(DartUri url, Object mode, Object webOnlyWindowName) {
        return launchUrlString(url == null ? null : url.toString(), mode, webOnlyWindowName);
    }

    /** {@code url_launcher}'s {@code canLaunchUrl(url)}. */
    public static Future<Boolean> canLaunchUrl(DartUri url) {
        return canLaunchUrlString(url == null ? null : url.toString());
    }

    /** {@code url_launcher}'s {@code launchUrlString(urlString)}. */
    public static Future<Boolean> launchUrlString(String urlString, Object mode, Object webOnlyWindowName) {
        if (urlString == null || !Display.isInitialized()) {
            return Future.value(false);
        }
        final Display d = Display.getInstance();
        return Future.value(launch(urlString, new Funcs.Func1<String, Boolean>() {
            @Override
            public Boolean call(String u) {
                return d.canExecute(u);
            }
        }, new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String u) {
                d.execute(u);
            }
        }));
    }

    /**
     * {@code launchUrl} answers true only when the URL was handed off. Display.execute
     * returns nothing and a port may decline a URL silently (an unhandled custom
     * scheme), so the port's own answer to canExecute decides first: FALSE means
     * nothing would open and the caller's fallback must run. A null answer is the
     * port saying it cannot tell, and the launch is attempted; an exception is a
     * failed launch.
     */
    static boolean launch(String url, Funcs.Func1<String, Boolean> canExecute, Funcs.VoidFunc1<String> execute) {
        try {
            Boolean can = canExecute.call(url);
            if (can != null && !can.booleanValue()) {
                return false;
            }
            execute.call(url);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** {@code url_launcher}'s {@code canLaunchUrlString(urlString)}. */
    public static Future<Boolean> canLaunchUrlString(String urlString) {
        boolean can = false;
        if (urlString != null) {
            try {
                if (Display.isInitialized()) {
                    Boolean b = Display.getInstance().canExecute(urlString);
                    // A null result means "unknown"; treat it as launchable.
                    can = b == null || b.booleanValue();
                }
            } catch (Throwable t) {
                can = false;
            }
        }
        return Future.value(can);
    }
}
