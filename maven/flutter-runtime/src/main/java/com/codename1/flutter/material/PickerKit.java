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

/** The pieces the Material picker dialogs share: their surface, header and actions. */
final class PickerKit {
    static final Color PRIMARY = new Color(0xFF6750A4L);
    static final Color ON_PRIMARY = new Color(0xFFFFFFFFL);
    static final Color SURFACE = new Color(0xFFF7F2FAL);
    static final Color MUTED = new Color(0xFF79747EL);
    static final Color TEXT = new Color(0xFF1D1B20L);
    static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December"};
    static final String[] SHORT_MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug",
        "Sep", "Oct", "Nov", "Dec"};
    static final String[] SHORT_DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private PickerKit() {
    }

    static TextStyle style(double size, Color color, boolean bold) {
        TextStyle s = new TextStyle();
        s.fontSize(size);
        s.color(color);
        if (bold) {
            s.fontWeight(FontWeight.w600);
        }
        return s;
    }

    static Widget centered(Widget w) {
        Center c = new Center();
        c.child(w);
        return c;
    }

    /** The dialog surface: header text, the body, then the action buttons. */
    static Widget surface(String help, String headline, Widget body, Widget actions) {
        return Kit.box(SURFACE, 28, null, 0, Kit.pad(24, 16, Kit.col(CrossAxisAlignment.start,
                Kit.text(help, style(12, MUTED, true)),
                Kit.pad(0, 8, Kit.text(headline, style(28, TEXT, false))),
                body, actions)));
    }

    static Widget button(String label, Funcs.VoidFunc0 onTap) {
        return Kit.tap(onTap, Kit.pad(12, 10, Kit.text(label, style(14, onTap == null ? MUTED : PRIMARY, true))));
    }

    static Widget actions(final BuildContext context, String cancel, String confirm,
            final Funcs.Func0<Object> result) {
        return Kit.row(MainAxisAlignment.end,
                button(cancel == null ? "Cancel" : cancel, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        Navigator.pop(context, null);
                    }
                }),
                button(confirm == null ? "OK" : confirm, result == null ? null : new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        Navigator.pop(context, result.call());
                    }
                }));
    }

    static int dayOfWeek(long y, long m, long d) {
        // Sakamoto's method; 0 = Monday.
        int[] t = {0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4};
        long yy = m < 3 ? y - 1 : y;
        long dow = (yy + yy / 4 - yy / 100 + yy / 400 + t[(int) m - 1] + d) % 7; // 0 = Sunday
        return (int) ((dow + 6) % 7);
    }

    static int daysIn(long y, long m) {
        if (m == 2) {
            return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 ? 29 : 28;
        }
        return m == 4 || m == 6 || m == 9 || m == 11 ? 30 : 31;
    }

    static long key(DateTime d) {
        return d == null ? -1 : d.year() * 10000 + d.month() * 100 + d.day();
    }

    static DateTime date(long y, long m, long d) {
        return new DateTime(y, m, d, 0, 0, 0, 0, 0);
    }

    static String label(DateTime d) {
        return d == null ? "--" : SHORT_DAYS[dayOfWeek(d.year(), d.month(), d.day())] + ", "
                + SHORT_MONTHS[(int) d.month() - 1] + " " + d.day();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> Future<T> show(BuildContext context, final Widget dialog) {
        return (Future) Dialogs.showDialog(context, new Funcs.Func1<BuildContext, Widget>() {
            @Override
            public Widget call(BuildContext c) {
                return centered(Kit.sized(Double.valueOf(328), null, dialog));
            }
        }, Boolean.TRUE, null, null, null, null, null);
    }
}
