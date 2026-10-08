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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Locale;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulElement;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextDirection;
import com.codename1.flutter.Widget;
import com.codename1.flutter.l10n.GlobalWidgetsLocalizations;
import com.codename1.flutter.l10n.LocalizationsDelegate;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Directionality;
import com.codename1.flutter.widgets.Localizations;
import com.codename1.flutter.widgets.Positioned;
import com.codename1.flutter.widgets.PositionedDirectional;
import com.codename1.flutter.widgets.WidgetsLocalizations;

import dart.async.Completer;
import dart.async.Future;
import dart.core.DartIterable;
import dart.core.DartList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Directionality is an inherited scope MaterialApp roots in the resolved locale; the
 * locale falls back to Flutter's basicLocaleListResolution; and a localizations
 * delegate whose load completes LATER still reaches the readers that asked for it.
 */
class DirectionalityAndLocaleTest {

    /** Records the ambient direction, and what a PositionedDirectional resolves to. */
    static final class DirectionProbe extends StatelessWidget {
        TextDirection seen;
        Positioned resolved;
        int builds;

        @Override
        public Widget build(BuildContext context) {
            builds++;
            seen = Directionality.of(context);
            PositionedDirectional pd = new PositionedDirectional();
            pd.start(5);
            pd.child(new ProbeBox(1, 1));
            resolved = (Positioned) pd.build(context);
            return new ProbeBox(1, 1);
        }
    }

    static Directionality dir(TextDirection d, Widget child) {
        Directionality w = new Directionality();
        w.textDirection(d);
        w.child(child);
        return w;
    }

    @Test
    @DisplayName("Directionality.of answers the nearest enclosing direction")
    void nearestDirectionWins() {
        DirectionProbe probe = new DirectionProbe();
        FlutterUI.mount(dir(TextDirection.ltr, dir(TextDirection.rtl, probe)), new RenderHost(),
                new BuildOwner());
        assertEquals(TextDirection.rtl, probe.seen);
        // start is the RIGHT edge right-to-left.
        assertEquals(Double.valueOf(5), probe.resolved.getRight());
        assertNull(probe.resolved.getLeft());
    }

    @Test
    @DisplayName("with no Directionality above, of() answers LTR")
    void noAncestorIsLeftToRight() {
        DirectionProbe probe = new DirectionProbe();
        FlutterUI.mount(probe, new RenderHost(), new BuildOwner());
        assertEquals(TextDirection.ltr, probe.seen);
        assertEquals(Double.valueOf(5), probe.resolved.getLeft());
    }

    /** Flips the direction above a retained reader. */
    static final class Flipper extends StatefulWidget {
        final DirectionProbe probe;

        Flipper(DirectionProbe probe) {
            this.probe = probe;
        }

        @Override
        public State createState() {
            return new FlipperState();
        }

        final class FlipperState extends State<Flipper> {
            TextDirection d = TextDirection.ltr;

            void flip() {
                setState(() -> d = TextDirection.rtl);
            }

            @Override
            public Widget build(BuildContext context) {
                return dir(d, widget().probe);
            }
        }
    }

    @Test
    @DisplayName("a changed direction rebuilds a retained reader")
    void aDirectionChangeRebuildsItsReaders() {
        DirectionProbe probe = new DirectionProbe();
        BuildOwner owner = new BuildOwner();
        StatefulElement root = (StatefulElement) FlutterUI.mount(new Flipper(probe), new RenderHost(), owner);
        assertEquals(TextDirection.ltr, probe.seen);
        ((Flipper.FlipperState) root.state()).flip();
        owner.flushSync();
        assertEquals(TextDirection.rtl, probe.seen);
    }

    static TextDirection appDirection(String language, boolean globalWidgets) {
        DirectionProbe probe = new DirectionProbe();
        MaterialApp app = new MaterialApp();
        app.locale(new Locale(language, null));
        app.localizationsDelegates(globalWidgets
                ? DartList.of((Object) GlobalWidgetsLocalizations.delegate)
                : DartList.<Object>of());
        app.home(probe);
        FlutterUI.mount(app, new RenderHost(), new BuildOwner());
        return probe.seen;
    }

    @Test
    @DisplayName("MaterialApp roots the direction in the locale, as WidgetsApp does")
    void theAppRootFollowsTheLocale() {
        assertEquals(TextDirection.rtl, appDirection("he", true));
        assertEquals(TextDirection.rtl, appDirection("ar", true));
        assertEquals(TextDirection.ltr, appDirection("en", true));
        // Flutter's default WidgetsLocalizations are left-to-right for every locale: an
        // app that never loaded GlobalWidgetsLocalizations stays LTR in Hebrew too.
        assertEquals(TextDirection.ltr, appDirection("he", false));
    }

    // --- basicLocaleListResolution, in Flutter's order -------------------------

    static Locale resolve(DartList<Locale> preferred, Locale... supported) {
        return WidgetsLocalizations.basicLocaleListResolution(preferred,
                DartIterable.wrap(DartList.of(supported)));
    }

    static DartList<Locale> prefs(Locale... ls) {
        return DartList.of(ls);
    }

    static Locale l(String lang, String country) {
        return new Locale(lang, country);
    }

    @Test
    @DisplayName("basicLocaleListResolution follows Flutter's precedence")
    void basicResolutionOrder() {
        // Exact match.
        assertEquals(l("fr", "CA"), resolve(prefs(l("fr", "CA")), l("fr", "FR"), l("fr", "CA")));
        // Language and country beats an earlier language-only candidate.
        assertEquals(l("en", "GB"), resolve(prefs(l("en", "GB")), l("en", "US"), l("en", "GB")));
        // Language only, first preference, next preference a different language.
        assertEquals(l("fr", null), resolve(prefs(l("fr", "BE"), l("de", "DE")),
                l("en", null), l("fr", null), l("de", "DE")));
        // Language-only match deferred when the next preference shares the language,
        // which then matches exactly.
        assertEquals(l("pt", "BR"), resolve(prefs(l("pt", "PT"), l("pt", "BR")),
                l("pt", null), l("pt", "BR")));
        // A deferred language match is taken at the next preference that matches no
        // better -- before a later preference's exact match (de_DE) is considered.
        assertEquals(l("es", null), resolve(prefs(l("es", "MX"), l("es", "AR"), l("de", "DE")),
                l("es", null), l("de", "DE")));
        // Country only, when no language matches anywhere.
        assertEquals(l("de", "CH"), resolve(prefs(l("rm", "CH")), l("en", "US"), l("de", "CH")));
        // Nothing at all: the first supported locale.
        assertEquals(l("en", "US"), resolve(prefs(l("ja", "JP")), l("en", "US"), l("de", "DE")));
        // No preferences: the first supported locale.
        assertEquals(l("en", "US"), resolve(prefs(), l("en", "US"), l("de", "DE")));
    }

    // --- a delegate that loads asynchronously ---------------------------------

    static final class Strings {
        final String text;

        Strings(String text) {
            this.text = text;
        }
    }

    static final class LateDelegate extends LocalizationsDelegate<Strings> {
        final Completer<Strings> completer = new Completer<Strings>();

        @Override
        public Future<Strings> load(Locale locale) {
            return completer.future();
        }
    }

    static final class Page extends StatelessWidget {
        String seen;
        int builds;

        @Override
        public Widget build(BuildContext context) {
            builds++;
            Strings s = Localizations.of(context, null, Strings.class);
            seen = s == null ? null : s.text;
            return new ProbeBox(1, 1);
        }
    }

    @Test
    @DisplayName("a delegate whose load completes later reaches the readers that asked")
    void aLateLoadReachesItsReaders() {
        LateDelegate delegate = new LateDelegate();
        Page page = new Page();
        MaterialApp app = new MaterialApp();
        app.locale(new Locale("en", null));
        app.localizationsDelegates(DartList.of((Object) delegate));
        app.home(page);
        BuildOwner owner = new BuildOwner();
        FlutterUI.mount(app, new RenderHost(), owner);
        assertNull(page.seen, "not loaded yet");

        delegate.completer.complete(new Strings("hello"));
        owner.flushSync();
        assertEquals("hello", page.seen, "the reader must be rebuilt once the load lands");
    }
}
