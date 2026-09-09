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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.async.Future;
import dart.core.DateTime;

/**
 * A material date-range-picker dialog — Flutter's {@code DateRangePickerDialog}.
 * This milestone renders a placeholder surface; the range calendar and
 * confirm/cancel flow land in a later pass. The configured bounds are retained.
 */
public class DateRangePickerDialog extends StatelessWidget {

    private String restorationId;
    private DateTime firstDate;
    private DateTime lastDate;
    private DateTime currentDate;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void firstDate(DateTime v) {
        this.firstDate = v;
    }

    public void lastDate(DateTime v) {
        this.lastDate = v;
    }

    public void currentDate(DateTime v) {
        this.currentDate = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }

    /** Top-level {@code showDateRangePicker(...)} — shows the dialog and completes with the chosen range. */
    public static Future<Object> show(BuildContext context, DateTime firstDate, DateTime lastDate) {
        return null;
    }
}
