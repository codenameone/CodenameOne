/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ui.spinner;

import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Container;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;

import java.util.Calendar;
import java.util.Date;

/// A date and a time in one picker: the Material date picker and the
/// Material time picker behind two tabs. The value is a `Date`.
class MaterialDateTimePicker extends Container implements InternalPickerWidget {
    private final MaterialDatePicker date = new MaterialDatePicker();
    private final MaterialTimePicker time = new MaterialTimePicker();
    private final RadioButton dateTab = new RadioButton("");
    private final RadioButton timeTab = new RadioButton("");

    MaterialDateTimePicker() {
        super(new BorderLayout());
        dateTab.setText(getUIManager().localize("picker.date", "Date"));
        timeTab.setText(getUIManager().localize("picker.time", "Time"));
        ButtonGroup g = new ButtonGroup();
        tab(dateTab, g);
        tab(timeTab, g);
        dateTab.setSelected(true);
        dateTab.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showTime(false);
            }
        });
        timeTab.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showTime(true);
            }
        });
        add(BorderLayout.NORTH, GridLayout.encloseIn(2, dateTab, timeTab));
        add(BorderLayout.CENTER, date);
    }

    private static void tab(RadioButton b, ButtonGroup g) {
        b.setToggle(true);
        b.setUIID("PickerMaterialTab");
        g.add(b);
    }

    MaterialDatePicker datePicker() {
        return date;
    }

    MaterialTimePicker timePicker() {
        return time;
    }

    void showTime(boolean v) {
        dateTab.setSelected(!v);
        timeTab.setSelected(v);
        if (v == contains(time)) {
            return;
        }
        if (v) {
            date.remove();
            add(BorderLayout.CENTER, time);
        } else {
            time.remove();
            add(BorderLayout.CENTER, date);
        }
        revalidateLater();
    }

    @Override
    public Object getValue() {
        Calendar c = Calendar.getInstance();
        c.setTime(date.getDate());
        c.set(Calendar.HOUR_OF_DAY, time.getHour());
        c.set(Calendar.MINUTE, time.getMinute());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Date) {
            Calendar c = Calendar.getInstance();
            c.setTime((Date) value);
            date.setDate((Date) value);
            time.setTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }
    }
}
