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

/** Flutter's Material {@code TimePickerDialog}: hour and minute steppers that pop the chosen time. */
public class TimePickerDialog extends StatefulWidget {
    private TimeOfDay initialTime;
    private String helpText;
    private String cancelText;
    private String confirmText;

    public void restorationId(Object v) { }
    public void initialTime(TimeOfDay v) { this.initialTime = v; }
    public void helpText(Object v) { this.helpText = v == null ? null : String.valueOf(v); }
    public void cancelText(Object v) { this.cancelText = v == null ? null : String.valueOf(v); }
    public void confirmText(Object v) { this.confirmText = v == null ? null : String.valueOf(v); }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new TimeState();
    }

    private static final class TimeState extends State<TimePickerDialog> {
        private int hour;
        private int minute;

        @Override
        public void initState() {
            super.initState();
            TimeOfDay t = widget().initialTime != null ? widget().initialTime : TimeOfDay.now();
            hour = t.hour();
            minute = t.minute();
        }

        private Widget stepper(final boolean hours, int value) {
            String v = value < 10 ? "0" + value : String.valueOf(value);
            return Kit.col(CrossAxisAlignment.center,
                    PickerKit.button("+", new Funcs.VoidFunc0() {
                        @Override
                        public void call() {
                            step(hours, 1);
                        }
                    }),
                    Kit.box(new Color(0xFFE8DEF8L), 8, null, 0, Kit.pad(16, 8,
                            Kit.text(v, PickerKit.style(48, PickerKit.TEXT, false)))),
                    PickerKit.button("-", new Funcs.VoidFunc0() {
                        @Override
                        public void call() {
                            step(hours, -1);
                        }
                    }));
        }

        private void step(final boolean hours, final int by) {
            setState(new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    if (hours) {
                        hour = (hour + by + 24) % 24;
                    } else {
                        minute = (minute + by + 60) % 60;
                    }
                }
            });
        }

        @Override
        public Widget build(BuildContext context) {
            TimePickerDialog w = widget();
            Widget body = Kit.row(MainAxisAlignment.center, stepper(true, hour),
                    Kit.pad(8, 0, Kit.text(":", PickerKit.style(48, PickerKit.TEXT, false))), stepper(false, minute));
            return PickerKit.surface(w.helpText != null ? w.helpText : "Select time", "", body,
                    PickerKit.actions(context, w.cancelText, w.confirmText, new Funcs.Func0<Object>() {
                        @Override
                        public Object call() {
                            TimeOfDay t = new TimeOfDay();
                            t.hour(hour);
                            t.minute(minute);
                            return t;
                        }
                    }));
        }
    }

    /** Top-level {@code showTimePicker(...)}: completes with the chosen time, or null when cancelled. */
    public static Future<TimeOfDay> show(BuildContext context, TimeOfDay initialTime, String helpText,
            String cancelText, String confirmText) {
        TimePickerDialog d = new TimePickerDialog();
        d.initialTime(initialTime);
        d.helpText(helpText);
        d.cancelText(cancelText);
        d.confirmText(confirmText);
        return PickerKit.show(context, d);
    }
}
