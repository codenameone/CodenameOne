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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.core.DateTime;
import dart.runtime.Funcs;

/**
 * The iOS date/time wheel — Flutter's {@code CupertinoDatePicker}. The picker
 * wheel is not modeled this pass; it composes an empty {@link Container}
 * placeholder while keeping the correct API shape (mode / initial value / the
 * change callback are captured).
 */
public class CupertinoDatePicker extends Widget {

    private CupertinoDatePickerMode mode;
    private DateTime initialDateTime;
    private Funcs.VoidFunc1<DateTime> onDateTimeChanged;

    public void backgroundColor(Color v) {
    }

    public void mode(CupertinoDatePickerMode v) {
        this.mode = v;
    }

    public void initialDateTime(DateTime v) {
        this.initialDateTime = v;
    }

    public void minimumDate(DateTime v) {
    }

    public void maximumDate(DateTime v) {
    }

    public void minimumYear(long v) {
    }

    public void maximumYear(long v) {
    }

    public void minuteInterval(long v) {
    }

    public void use24hFormat(boolean v) {
    }

    public void onDateTimeChanged(Funcs.VoidFunc1<DateTime> v) {
        this.onDateTimeChanged = v;
    }

    CupertinoDatePickerMode getMode() {
        return mode;
    }

    DateTime getInitialDateTime() {
        return initialDateTime;
    }

    Funcs.VoidFunc1<DateTime> getOnDateTimeChanged() {
        return onDateTimeChanged;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new CupertinoDatePickerRenderElement(this);
    }
}
