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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A vertical (or horizontal) sequence of {@link Step}s — Flutter's
 * {@code Stepper}. This milestone renders every step's title followed by its
 * content in a {@link Column}; the collapse-to-current-step behavior and the
 * continue/cancel controls are deferred.
 */
public class Stepper extends StatelessWidget {

    private DartList<Step> steps;
    private StepperType type;
    private long currentStep;
    private Funcs.VoidFunc1<Long> onStepTapped;
    private Funcs.VoidFunc0 onStepContinue;
    private Funcs.VoidFunc0 onStepCancel;

    public void steps(DartList<Step> v) {
        this.steps = v;
    }

    public void physics(Object v) {
    }

    public void type(StepperType v) {
        this.type = v;
    }

    public void currentStep(long v) {
        this.currentStep = v;
    }

    public void onStepTapped(Funcs.VoidFunc1<Long> v) {
        this.onStepTapped = v;
    }

    public void onStepContinue(Funcs.VoidFunc0 v) {
        this.onStepContinue = v;
    }

    public void onStepCancel(Funcs.VoidFunc0 v) {
        this.onStepCancel = v;
    }

    public void controlsBuilder(Object v) {
    }

    public void elevation(double v) {
    }

    public void margin(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                Step s = steps.get(i);
                if (s.getTitle() != null) {
                    kids.add(s.getTitle());
                }
                if (s.getSubtitle() != null) {
                    kids.add(s.getSubtitle());
                }
                if (s.getContent() != null) {
                    kids.add(s.getContent());
                }
            }
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(kids);
        return col;
    }
}
