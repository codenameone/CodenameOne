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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.spinner.DurationSpinner3D;

import dart.core.Duration;

/**
 * {@link CupertinoTimerPicker}: Codename One's duration wheel, in hours, minutes and
 * seconds -- which is what the Cupertino timer picker's default mode shows.
 */
class CupertinoTimerPickerRenderElement extends CupertinoWheelRenderElement {

    private static final long MILLIS_PER_MICRO = 1000;

    CupertinoTimerPickerRenderElement(Widget widget) {
        super(widget);
    }

    private CupertinoTimerPicker picker() {
        return (CupertinoTimerPicker) widget();
    }

    @Override
    protected Container createWheel() {
        DurationSpinner3D d = new DurationSpinner3D(DurationSpinner3D.FIELD_HOUR
                | DurationSpinner3D.FIELD_MINUTE | DurationSpinner3D.FIELD_SECOND);
        Duration initial = picker().getInitialTimerDuration();
        if (initial != null) {
            d.setValue(Long.valueOf(initial.inMicroseconds() / MILLIS_PER_MICRO));
        }
        return d;
    }

    @Override
    protected void report(Object value) {
        dart.runtime.Funcs.VoidFunc1<Duration> f = picker().getOnTimerDurationChanged();
        if (f == null || !(value instanceof Long)) {
            return;
        }
        f.call(Duration.ofMicroseconds(((Long) value).longValue() * MILLIS_PER_MICRO));
    }
}
