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
        return ((dart.runtime.Funcs.Func3<com.codename1.flutter.Widget, Animation<Double>,
                Animation<Double>, com.codename1.flutter.Widget>) transitionBuilder)
                .call(child, e.primary(), e.secondary());
    }
}
