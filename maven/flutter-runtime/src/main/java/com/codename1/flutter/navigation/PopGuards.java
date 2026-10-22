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
package com.codename1.flutter.navigation;

import java.util.ArrayList;
import java.util.List;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.PopScope;
import com.codename1.flutter.widgets.WillPopScope;

import dart.async.Completer;
import dart.async.Future;
import dart.runtime.Funcs;

/**
 * The PopScope and WillPopScope widgets in a route, asked before a {@code maybePop} or a
 * back press closes it. Both used to be accepted and ignored, so a "discard changes?"
 * guard never ran and the page closed under it.
 */
final class PopGuards {
    private PopGuards() {
    }

    /** Every PopScope and WillPopScope mounted under {@code root}, outermost first. */
    static List<Widget> collect(Element root) {
        final List<Widget> out = new ArrayList<Widget>();
        if (root != null) {
            walk(root, out);
        }
        return out;
    }

    private static void walk(Element e, final List<Widget> out) {
        Widget w = e.widget();
        if (w instanceof PopScope || w instanceof WillPopScope) {
            out.add(w);
        }
        e.visitChildren(new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element child) {
                walk(child, out);
            }
        });
    }

    /**
     * Whether the route may close: false at once if any PopScope says {@code canPop: false}
     * (each scope then hears the attempt), else the answer of every WillPopScope in turn.
     */
    static Future<Boolean> mayPop(final List<Widget> scopes, Object result) {
        boolean vetoed = false;
        for (Widget w : scopes) {
            if (w instanceof PopScope && !((PopScope<?>) w).getCanPop()) {
                vetoed = true;
            }
        }
        // The caller reports the outcome to the PopScopes exactly once, whichever guard
        // refused -- a WillPopScope veto included -- so nothing is notified here.
        if (vetoed) {
            return Future.value(Boolean.FALSE);
        }
        Completer<Boolean> done = new Completer<Boolean>();
        askWillPop(scopes, 0, done);
        return done.future();
    }

    static void notifyPopScopes(List<Widget> scopes, boolean didPop, Object result) {
        for (Widget w : scopes) {
            if (w instanceof PopScope) {
                ((PopScope<?>) w).invoked(didPop, result);
            }
        }
    }

    private static void askWillPop(final List<Widget> scopes, int from, final Completer<Boolean> done) {
        for (int i = from; i < scopes.size(); i++) {
            Widget w = scopes.get(i);
            if (!(w instanceof WillPopScope) || ((WillPopScope) w).getOnWillPop() == null) {
                continue;
            }
            Object answer = ((WillPopScope) w).getOnWillPop().call();
            final int next = i + 1;
            if (answer instanceof Future) {
                asObjectFuture(answer).then(new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object ok) {
                        if (Boolean.TRUE.equals(ok)) {
                            askWillPop(scopes, next, done);
                        } else {
                            done.complete(Boolean.FALSE);
                        }
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object err) {
                        done.complete(Boolean.FALSE);
                    }
                });
                return;
            }
            if (!Boolean.TRUE.equals(answer)) {
                done.complete(Boolean.FALSE);
                return;
            }
        }
        done.complete(Boolean.TRUE);
    }

    @SuppressWarnings("unchecked")
    private static Future<Object> asObjectFuture(Object f) {
        return (Future<Object>) f;
    }
}
