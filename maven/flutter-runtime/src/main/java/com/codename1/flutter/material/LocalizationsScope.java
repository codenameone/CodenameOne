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

import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.provider.SingleChildWidget;

import java.util.List;

/**
 * Publishes the localized-resource objects loaded from a MaterialApp's
 * {@code localizationsDelegates} to its subtree, keyed by runtime type. This is
 * how {@code GalleryLocalizations.of(context)} (which resolves through
 * {@link com.codename1.flutter.widgets.Localizations#of}) finds its instance:
 * {@code providedValueFor} returns the first loaded object assignable to the
 * requested type.
 */
public class LocalizationsScope extends SingleChildWidget implements InheritedValueProvider {

    private List<Object> resources;
    private final dart.runtime.Funcs.Func0<List<Object>> supplier;
    private com.codename1.flutter.Locale locale;
    private Object delegates;

    /**
     * What the resources were loaded from: the app's explicit locale and its delegates.
     * When a rebuild changes either, the readers below rebuild (see
     * {@link LocalizationsScopeElement}).
     */
    public void reloadKey(com.codename1.flutter.Locale locale, Object delegates) {
        this.locale = locale;
        this.delegates = delegates;
    }

    /**
     * Whether {@code old} published the same resources -- Flutter's test in
     * Localizations.didUpdateWidget: the same locale, and as many delegates of the same
     * types. (A delegate list is usually a new list on every build, so it is not
     * compared by identity.)
     */
    boolean sameResourcesAs(LocalizationsScope old) {
        if (old.locale == null ? locale != null : !old.locale.equals(locale)) {
            return false;
        }
        if (old.delegates == delegates) {
            return true;
        }
        if (!(old.delegates instanceof Iterable) || !(delegates instanceof Iterable)) {
            return false;
        }
        java.util.Iterator<?> a = ((Iterable<?>) old.delegates).iterator();
        java.util.Iterator<?> b = ((Iterable<?>) delegates).iterator();
        while (a.hasNext() && b.hasNext()) {
            Object x = a.next();
            Object y = b.next();
            if (x == null ? y != null : y == null || x.getClass() != y.getClass()) {
                return false;
            }
        }
        return !a.hasNext() && !b.hasNext();
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new LocalizationsScopeElement(this);
    }

    public LocalizationsScope(List<Object> resources) {
        this.resources = resources;
        this.supplier = null;
    }

    /**
     * A scope whose resources load on first lookup. Deferring matters: loading
     * during the app's build reads the delegate list at the earliest possible
     * moment, which on a lazily-initialised backend can be before the class
     * holding it has run its static initialiser.
     */
    public LocalizationsScope(dart.runtime.Funcs.Func0<List<Object>> supplier) {
        this.supplier = supplier;
    }

    private List<Object> resources() {
        if (resources == null && supplier != null) {
            resources = supplier.call();
            if (resources != null && resources.isEmpty()) {
                try {
                    com.codename1.io.Log.p("Flutter runtime: no localizations resolved for this app; "
                            + "every Foo.of(context) below will be null");
                } catch (Throwable ignore) {
                    // headless: Log has no storage backend
                }
            }
        }
        return resources;
    }

    /// Loads begun by the supplier that had not completed when it returned.
    private int pendingLoads;
    /// Readers whose lookup came up empty while a load was pending; rebuilt when it lands.
    private List<com.codename1.flutter.Element> waiting;

    /**
     * Adopts a delegate's load that has not completed yet. When it does, its value joins
     * the published resources and every reader that looked one up in the meantime is
     * rebuilt, so {@code Foo.of(context)} answers once the load is done instead of
     * staying null for the life of the scope.
     *
     * <p>Flutter's Localizations instead withholds its whole subtree until every load is
     * in. This scope loads lazily, on the first lookup -- from inside the subtree's own
     * build (see MaterialApp.wrapWithLocalizations for why) -- so by the time a load is
     * known to be pending the subtree is already being built, and the readers are
     * rebuilt instead. Delegates in this runtime almost always answer synchronously
     * (a SynchronousFuture, or an async load whose awaits block), so this is the rare
     * path.</p>
     */
    @SuppressWarnings("unchecked")
    public void loadLater(dart.async.Future<?> load) {
        pendingLoads++;
        ((dart.async.Future<Object>) load).then(new dart.runtime.Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object value) {
                settled(value);
                return null;
            }
        }, new dart.runtime.Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object error) {
                try {
                    com.codename1.io.Log.p("Flutter runtime: a localizations delegate's load failed: "
                            + error);
                } catch (Throwable ignore) {
                    // headless: Log has no storage backend
                }
                settled(null);
            }
        });
    }

    private void settled(Object value) {
        pendingLoads--;
        if (value != null) {
            if (resources == null) {
                resources = new java.util.ArrayList<Object>();
            }
            resources.add(value);
        }
        List<com.codename1.flutter.Element> readers = waiting;
        if (pendingLoads == 0) {
            waiting = null;
        }
        if (value == null || readers == null) {
            return;
        }
        for (int i = 0; i < readers.size(); i++) {
            com.codename1.flutter.Element e = readers.get(i);
            if (e.isMounted()) {
                e.didChangeDependencies();
            }
        }
    }

    /**
     * Called by {@code Localizations.of} when a lookup found nothing: if the nearest
     * scope still has a load in flight, the reader is rebuilt when it lands. A plain
     * miss (no such delegate at all) costs one ancestor search, which the miss already
     * pays for its diagnostic.
     */
    public static void rebuildWhenLoaded(com.codename1.flutter.BuildContext context) {
        if (!(context instanceof com.codename1.flutter.Element)) {
            return;
        }
        LocalizationsScope scope = context.findAncestorWidgetOfExactType(LocalizationsScope.class);
        if (scope == null || scope.pendingLoads <= 0) {
            return;
        }
        if (scope.waiting == null) {
            scope.waiting = new java.util.ArrayList<com.codename1.flutter.Element>();
        }
        com.codename1.flutter.Element e = (com.codename1.flutter.Element) context;
        if (!scope.waiting.contains(e)) {
            scope.waiting.add(e);
        }
    }

    @Override
    public Object providedValueFor(Class<?> type) {
        List<Object> rs = resources();
        if (rs != null && type != null) {
            for (Object r : rs) {
                if (com.codename1.flutter.Element.isInstanceOf(type, r)) {
                    return r;
                }
            }
        }
        return null;
    }
}
