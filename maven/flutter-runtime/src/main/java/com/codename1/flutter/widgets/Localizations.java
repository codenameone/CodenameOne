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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Locale;

/**
 * Flutter's {@code Localizations} inherited-widget lookup helpers.
 *
 * <p>The app uses only the static lookups: {@code Localizations.of<T>(context,
 * type)} to reach a localizations object published up the tree, and
 * {@code Localizations.localeOf(context)} for the ambient {@link Locale}. The
 * transpiler threads the requested {@code T} as a trailing {@code Class<T>}
 * witness for {@code of}.</p>
 */
public final class Localizations {

    private Localizations() {
    }

    /**
     * {@code Localizations.of<T>(context, type)}. Returns the nearest inherited
     * localizations object of the requested type, or {@code null} when absent.
     */
    public static <T> T of(BuildContext context, Object type, Class<T> witness) {
        if (context == null || witness == null) {
            return null;
        }
        T value;
        try {
            value = context.read(witness);
        } catch (Throwable t) {
            report("Localizations.of(" + witness.getName() + ") threw: " + t);
            return null;
        }
        if (value == null) {
            report("Localizations.of(" + witness.getName() + ") found nothing; the app's "
                    + "localizationsDelegates produced no matching object");
        }
        return value;
    }

    private static int reports;

    /**
     * Dart writes {@code Foo.of(context)!}, so a null here becomes a null-check
     * TypeError elsewhere with no hint of which lookup failed. Capped, because
     * a missing localization is missing on every build.
     */
    private static void report(String message) {
        if (reports >= 5) {
            return;
        }
        reports++;
        try {
            com.codename1.io.Log.p("Flutter runtime: " + message);
        } catch (Throwable ignore) {
            // headless: Log has no storage backend
        }
    }

    /** {@code Localizations.localeOf(context)} — the ambient locale. */
    public static Locale localeOf(BuildContext context) {
        if (context != null) {
            Object l = context.providerValueOfType(Locale.class);
            if (l instanceof Locale) {
                return (Locale) l;
            }
        }
        return new Locale("en", "US");
    }
}
