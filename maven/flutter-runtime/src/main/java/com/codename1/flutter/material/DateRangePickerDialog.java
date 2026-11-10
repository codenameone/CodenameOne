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
import com.codename1.flutter.Color;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.FontWeight;
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.navigation.Navigator;
import com.codename1.flutter.widgets.Center;
import com.codename1.flutter.widgets.Kit;
import dart.async.Future;
import dart.core.DateTime;
import dart.runtime.Funcs;

/** Flutter's Material {@code DateRangePickerDialog}: two taps choose a start and an end. */
public class DateRangePickerDialog extends StatefulWidget {
    private DateTime firstDate;
    private DateTime lastDate;
    private Object initialDateRange;
    private String helpText;
    private String cancelText;
    private String saveText;

    public void restorationId(Object v) { }
    public void firstDate(DateTime v) { this.firstDate = v; }
    public void lastDate(DateTime v) { this.lastDate = v; }
    public void currentDate(DateTime v) { }
    public void initialDateRange(Object v) { this.initialDateRange = v; }
    public void helpText(String v) { this.helpText = v; }
    public void cancelText(String v) { this.cancelText = v; }
    public void saveText(String v) { this.saveText = v; }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public State<? extends StatefulWidget> createState() {
        DateTime s = null;
        DateTime e = null;
        if (initialDateRange instanceof dart.core.DateTimeRange) {
            s = ((dart.core.DateTimeRange) initialDateRange).start();
            e = ((dart.core.DateTimeRange) initialDateRange).end();
        }
        return (State) new CalendarPickerState(true, s, e, firstDate, lastDate, helpText, cancelText,
                saveText == null ? "Save" : saveText);
    }

    /**
     * Top-level {@code showDateRangePicker(...)}: completes with the range, or null when cancelled.
     * Typed as Flutter's {@code Future<DateTimeRange?>}: a {@code Future<Object>} left
     * {@code DateTimeRange r = await showDateRangePicker(...)} with no T javac could infer.
     */
    public static Future<dart.core.DateTimeRange> show(BuildContext context, DateTime firstDate, DateTime lastDate,
            dart.core.DateTimeRange initialDateRange, DateTime currentDate, String helpText, String cancelText,
            String saveText) {
        DateRangePickerDialog d = new DateRangePickerDialog();
        d.firstDate(firstDate);
        d.lastDate(lastDate);
        d.initialDateRange(initialDateRange);
        d.helpText(helpText);
        d.cancelText(cancelText);
        d.saveText(saveText);
        return PickerKit.show(context, d);
    }
}
