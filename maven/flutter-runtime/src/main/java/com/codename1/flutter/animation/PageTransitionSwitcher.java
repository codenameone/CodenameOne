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
package com.codename1.flutter.animation;

import dart.core.Duration;

/**
 * Runs a supplied transition when its {@code child} is replaced -- the {@code animations}
 * package's {@code PageTransitionSwitcher}. The {@code transitionBuilder} is a
 * three-argument closure {@code (child, primaryAnimation, secondaryAnimation)}.
 *
 * <p>It used to host the current child directly and run nothing, which made every switch
 * a cut. That is not a small omission where the gallery uses it: Reply's search is not a
 * pushed route at all, it is this widget swapping the search page in for the mail
 * navigator, so the whole animation of opening search lived here and there was none.</p>
 */
public class PageTransitionSwitcher extends AnimatedChildWidget {

    private Duration duration;
    private boolean reverse;
    private Object transitionBuilder;

    public void duration(Duration v) {
        this.duration = v;
    }

    public void reverse(boolean v) {
        this.reverse = v;
    }

    public void transitionBuilder(dart.runtime.Funcs.Func3<com.codename1.flutter.Widget,
            Animation<Double>, Animation<Double>, com.codename1.flutter.Widget> v) {
        this.transitionBuilder = v;
    }

    public Object getTransitionBuilder() {
        return transitionBuilder;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new PageTransitionSwitcherElement(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public com.codename1.flutter.Widget build(com.codename1.flutter.BuildContext context) {
        com.codename1.flutter.Widget child = getChild();
        if (child == null || !(transitionBuilder instanceof dart.runtime.Funcs.Func3)
                || !(context instanceof PageTransitionSwitcherElement)) {
            return child;
        }
        PageTransitionSwitcherElement e = (PageTransitionSwitcherElement) context;
        e.noteChild(child, duration);
        dart.runtime.Funcs.Func3<com.codename1.flutter.Widget, Animation<Double>,
                Animation<Double>, com.codename1.flutter.Widget> build =
                (dart.runtime.Funcs.Func3<com.codename1.flutter.Widget, Animation<Double>,
                        Animation<Double>, com.codename1.flutter.Widget>) transitionBuilder;
        com.codename1.flutter.Widget incoming =
                build.call(child, e.primary(), e.secondary());
        com.codename1.flutter.Widget out = e.leaving();
        if (out == null) {
            return incoming;
        }
        // Both at once, the outgoing one UNDER the incoming one. The transition itself
        // decides what that looks like: it is handed the outgoing child as arrived
        // (primary complete) and leaving (secondary running), which is how a shared axis
        // fades one out while the other comes in.
        com.codename1.flutter.widgets.Stack stack =
                new com.codename1.flutter.widgets.Stack();
        dart.core.DartList<com.codename1.flutter.Widget> kids =
                new dart.core.DartList<com.codename1.flutter.Widget>();
        kids.add(build.call(out, e.arrived(), e.primary()));
        kids.add(incoming);
        stack.children(kids);
        return stack;
    }
}
