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
package com.codename1.flutter.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Locale;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulElement;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.l10n.LocalizationsDelegate;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Localizations;

import dart.async.Future;
import dart.core.DartList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code Foo.of(context)} through {@link Localizations#of} DEPENDS on the app's
 * localizations, as Flutter's does: when the app's locale changes, a page that read its
 * strings rebuilds with the new ones even though the page itself is the same retained
 * widget, which reconciliation never revisits on its own.
 */
class LocalizationsRebuildTest {

    static final class Strings {
        final String language;

        Strings(String language) {
            this.language = language;
        }
    }

    static final class StringsDelegate extends LocalizationsDelegate<Strings> {
        @Override
        public Future<Strings> load(Locale locale) {
            return Future.value(new Strings(locale.languageCode()));
        }
    }

    /** The page: reads its strings on every build. One instance, kept across app rebuilds. */
    static final class Page extends StatelessWidget {
        int builds;
        String seen;

        @Override
        public Widget build(BuildContext context) {
            builds++;
            Strings s = Localizations.of(context, null, Strings.class);
            seen = s == null ? null : s.language;
            return new ProbeBox(1, 1);
        }
    }

    /** The app-wide settings model above MaterialApp, as the gallery has. */
    static final class App extends StatefulWidget {
        final Page page;
        final StringsDelegate delegate = new StringsDelegate();

        App(Page page) {
            this.page = page;
        }

        @Override
        public State createState() {
            return new AppState();
        }

        final class AppState extends State<App> {
            String language = "en";

            void switchTo(String l) {
                setState(() -> language = l);
            }

            @Override
            public Widget build(BuildContext context) {
                MaterialApp app = new MaterialApp();
                app.locale(new Locale(language, null));
                app.localizationsDelegates(DartList.of((Object) widget().delegate));
                app.home(widget().page);
                return app;
            }
        }
    }

    @Test
    @DisplayName("changing MaterialApp.locale rebuilds a retained page that read its strings")
    void aLocaleChangeReachesTheRetainedPage() {
        Page page = new Page();
        BuildOwner owner = new BuildOwner();
        StatefulElement root = (StatefulElement) FlutterUI.mount(new App(page), new RenderHost(), owner);
        assertEquals("en", page.seen);

        ((App.AppState) root.state()).switchTo("he");
        owner.flushSync();
        assertEquals("he", page.seen, "the page must read the new locale's strings");
    }

    @Test
    @DisplayName("an app rebuild that keeps the locale does not rebuild the page")
    void anUnchangedLocaleDoesNotRebuildThePage() {
        Page page = new Page();
        BuildOwner owner = new BuildOwner();
        StatefulElement root = (StatefulElement) FlutterUI.mount(new App(page), new RenderHost(), owner);
        int builds = page.builds;

        ((App.AppState) root.state()).switchTo("en");
        owner.flushSync();
        assertEquals(builds, page.builds, "same locale and delegates: nothing to reload");
    }
}
