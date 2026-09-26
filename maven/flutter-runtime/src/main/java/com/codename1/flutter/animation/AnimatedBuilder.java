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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Rebuilds via a {@code builder} callback whenever its {@code animation}
 * notifies — Flutter's {@code AnimatedBuilder}. The optional {@code child} is
 * an optimization handed back to the builder unchanged.
 */
public class AnimatedBuilder extends Widget {

    private com.codename1.flutter.foundation.Listenable animation;
    private Funcs.Func2<BuildContext, Widget, Widget> builder;
    private Widget child;

    public void animation(com.codename1.flutter.foundation.Listenable v) {
        this.animation = v;
    }

    public void builder(Funcs.Func2<BuildContext, Widget, Widget> v) {
        this.builder = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public com.codename1.flutter.foundation.Listenable getAnimation() {
        return animation;
    }

    public Funcs.Func2<BuildContext, Widget, Widget> getBuilder() {
        return builder;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new AnimatedBuilderElement(this);
    }
}
