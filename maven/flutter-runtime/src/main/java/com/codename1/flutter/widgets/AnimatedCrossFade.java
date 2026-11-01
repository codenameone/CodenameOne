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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Kit;
import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * Flutter's {@code AnimatedCrossFade}: {@code firstChild} or {@code secondChild},
 * as {@code crossFadeState} says. The switch is immediate; the fade is not animated.
 */
public class AnimatedCrossFade extends StatelessWidget {
    private Widget firstChild;
    private Widget secondChild;
    private CrossFadeState crossFadeState = CrossFadeState.showFirst;
    public void firstChild(Widget v) { this.firstChild = v; }
    public void secondChild(Widget v) { this.secondChild = v; }
    public void crossFadeState(CrossFadeState v) { if (v != null) { this.crossFadeState = v; } }
    public void duration(Object v) { }
    public void reverseDuration(Object v) { }
    public void firstCurve(Object v) { }
    public void secondCurve(Object v) { }
    public void sizeCurve(Object v) { }
    public void alignment(Object v) { }
    public void layoutBuilder(Object v) { }
    public void excludeBottomFocus(boolean v) { }
    @Override
    public Widget build(BuildContext context) {
        return crossFadeState == CrossFadeState.showSecond ? secondChild : firstChild;
    }
}
