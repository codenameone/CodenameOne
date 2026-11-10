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

/**
 * The month calendar of the Material date pickers: one date for {@link DatePickerDialog},
 * a start and an end for {@link DateRangePickerDialog}. Days outside the first and last
 * dates are shown but cannot be chosen.
 */
final class CalendarPickerState extends State<StatefulWidget> {
    private final boolean range;
    private final DateTime first;
    private final DateTime last;
    private final String help;
    private final String cancel;
    private final String confirm;
    private long year;
    private long month;
    private DateTime start;
    private DateTime end;

    CalendarPickerState(boolean range, DateTime initial, DateTime initialEnd, DateTime first,
            DateTime last, String help, String cancel, String confirm) {
        this.range = range;
        this.first = first;
        this.last = last;
        this.help = help;
        this.cancel = cancel;
        this.confirm = confirm;
        this.start = initial;
        this.end = initialEnd;
        DateTime shown = initial != null ? initial : (first != null ? first : DateTime.now());
        year = shown.year();
        month = shown.month();
    }

    private boolean selectable(long k) {
        return (first == null || k >= PickerKit.key(first)) && (last == null || k <= PickerKit.key(last));
    }

    private void shift(final int by) {
        setState(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                month += by;
                if (month < 1) {
                    month = 12;
                    year--;
                } else if (month > 12) {
                    month = 1;
                    year++;
                }
            }
        });
    }

    private void pick(final DateTime d) {
        setState(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (!range || start == null || end != null || PickerKit.key(d) < PickerKit.key(start)) {
                    start = d;
                    end = null;
                } else {
                    end = d;
                }
            }
        });
    }

    private Widget day(final long d) {
        long k = year * 10000 + month * 100 + d;
        boolean can = selectable(k);
        boolean edge = k == PickerKit.key(start) || k == PickerKit.key(end);
        boolean inside = range && start != null && end != null && k > PickerKit.key(start)
                && k < PickerKit.key(end);
        Color fg = edge ? PickerKit.ON_PRIMARY : (can ? PickerKit.TEXT : PickerKit.MUTED);
        Color bg = edge ? PickerKit.PRIMARY : (inside ? new Color(0xFFE8DEF8L) : null);
        Widget cell = Kit.box(bg, 20, null, 0, Kit.sized(Double.valueOf(40), Double.valueOf(40),
                PickerKit.centered(Kit.text(String.valueOf(d), PickerKit.style(14, fg, edge)))));
        if (!can) {
            return Kit.expanded(1, PickerKit.centered(cell));
        }
        final DateTime date = PickerKit.date(year, month, d);
        return Kit.expanded(1, PickerKit.centered(Kit.tap(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                pick(date);
            }
        }, cell)));
    }

    @Override
    public Widget build(BuildContext context) {
        java.util.List<Widget> rows = new java.util.ArrayList<Widget>();
        rows.add(Kit.row(MainAxisAlignment.spaceBetween,
                Kit.text(PickerKit.MONTHS[(int) month - 1] + " " + year, PickerKit.style(14, PickerKit.TEXT, true)),
                Kit.row(MainAxisAlignment.end,
                        PickerKit.button("<", new Funcs.VoidFunc0() {
                            @Override
                            public void call() {
                                shift(-1);
                            }
                        }),
                        PickerKit.button(">", new Funcs.VoidFunc0() {
                            @Override
                            public void call() {
                                shift(1);
                            }
                        }))));
        Widget[] heads = new Widget[7];
        for (int i = 0; i < 7; i++) {
            heads[i] = Kit.expanded(1, PickerKit.centered(Kit.text(PickerKit.SHORT_DAYS[i].substring(0, 1),
                    PickerKit.style(12, PickerKit.MUTED, false))));
        }
        rows.add(Kit.row(MainAxisAlignment.start, heads));
        int lead = PickerKit.dayOfWeek(year, month, 1);
        int days = PickerKit.daysIn(year, month);
        for (int cell = 0; cell < lead + days; cell += 7) {
            Widget[] week = new Widget[7];
            for (int i = 0; i < 7; i++) {
                int d = cell + i - lead + 1;
                week[i] = d < 1 || d > days ? Kit.expanded(1, Kit.sized(null, Double.valueOf(40), null))
                        : day(d);
            }
            rows.add(Kit.row(MainAxisAlignment.start, week));
        }
        Widget body = Kit.col(CrossAxisAlignment.stretch, rows.toArray(new Widget[rows.size()]));
        String headline = range ? PickerKit.label(start) + " - " + PickerKit.label(end) : PickerKit.label(start);
        boolean ready = range ? start != null && end != null : start != null;
        return PickerKit.surface(help != null ? help : (range ? "Select range" : "Select date"), headline, body,
                PickerKit.actions(context, cancel, confirm, !ready ? null : new Funcs.Func0<Object>() {
                    @Override
                    public Object call() {
                        return range ? new dart.core.DateTimeRange(start, end) : start;
                    }
                }));
    }
}
