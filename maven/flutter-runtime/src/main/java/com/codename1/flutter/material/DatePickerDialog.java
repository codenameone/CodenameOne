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

/** Flutter's Material {@code DatePickerDialog}: a month calendar that pops the chosen date. */
public class DatePickerDialog extends StatefulWidget {
    private DateTime initialDate;
    private DateTime firstDate;
    private DateTime lastDate;
    private String helpText;
    private String cancelText;
    private String confirmText;

    public void restorationId(Object v) { }
    public void initialDate(DateTime v) { this.initialDate = v; }
    public void firstDate(DateTime v) { this.firstDate = v; }
    public void lastDate(DateTime v) { this.lastDate = v; }
    public void currentDate(DateTime v) { }
    public void helpText(String v) { this.helpText = v; }
    public void cancelText(String v) { this.cancelText = v; }
    public void confirmText(String v) { this.confirmText = v; }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public State<? extends StatefulWidget> createState() {
        return (State) new CalendarPickerState(false, initialDate, null, firstDate, lastDate, helpText,
                cancelText, confirmText);
    }

    /** Top-level {@code showDatePicker(...)}: completes with the chosen date, or null when cancelled. */
    public static Future<DateTime> show(BuildContext context, DateTime initialDate, DateTime firstDate,
            DateTime lastDate, DateTime currentDate, String helpText, String cancelText, String confirmText) {
        DatePickerDialog d = new DatePickerDialog();
        d.initialDate(initialDate);
        d.firstDate(firstDate);
        d.lastDate(lastDate);
        d.helpText(helpText);
        d.cancelText(cancelText);
        d.confirmText(confirmText);
        return PickerKit.show(context, d);
    }
}
