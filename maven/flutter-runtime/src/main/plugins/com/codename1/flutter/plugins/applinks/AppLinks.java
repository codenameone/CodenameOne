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
package com.codename1.flutter.plugins.applinks;

import com.codename1.ui.Display;

import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;
import dart.core.DartUri;

/**
 * app_links's {@code AppLinks}, over the {@code AppArg} property through which Codename
 * One hands an app the URL that opened it. The imported app's entry point calls
 * {@link #appStarted} from {@code Lifecycle.start()}, so a link that resumes a running
 * app arrives on the streams as well as the first one.
 */
public class AppLinks {
    private static final StreamController<String> LINKS = StreamController.broadcast();
    private static String initial;
    private static boolean initialRead;
    /// The most recent link received, launch or resume; getLatestLink answers it.
    private static String latest;

    public AppLinks() {
    }

    /** Takes the pending {@code AppArg}, if any, and publishes it. */
    public static void appStarted() {
        if (!Display.isInitialized()) {
            return;
        }
        String arg = Display.getInstance().getProperty("AppArg", null);
        // Only the first start can carry the launch link: a cold start without one
        // settles the slot, so a later resume link is never replayed as initial.
        boolean first = !initialRead;
        initialRead = true;
        if (arg == null || arg.length() == 0) {
            return;
        }
        Display.getInstance().setProperty("AppArg", null);
        if (first) {
            initial = arg;
        }
        latest = arg;
        LINKS.add(arg);
    }

    private static String initialLink() {
        if (!initialRead) {
            appStarted();
        }
        return initial;
    }

    public Future<DartUri> getInitialLink() {
        String s = initialLink();
        return Future.value(s == null ? null : DartUri.parse(s));
    }

    public Future<String> getInitialLinkString() {
        return Future.value(initialLink());
    }

    public Future<DartUri> getLatestLink() {
        initialLink();
        return Future.value(latest == null ? null : DartUri.parse(latest));
    }

    public Future<String> getLatestLinkString() {
        initialLink();
        return Future.value(latest);
    }

    public Stream<String> stringLinkStream() {
        return LINKS.stream();
    }

    public Stream<DartUri> uriLinkStream() {
        return LINKS.stream().map(new dart.runtime.Funcs.Func1<String, DartUri>() {
            @Override
            public DartUri call(String s) {
                return DartUri.parse(s);
            }
        });
    }
}
