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
package javafx.scene.control;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.spinner.Picker;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;

/// A control that holds a date and opens a chooser to pick another.
///
/// It is shown by a Codename One `Picker` in date mode: a button with the
/// date, written as the device writes short dates, which opens the date
/// picker of the platform when it is pressed. The prompt text shows while
/// there is no date. A change of the value, by the user or from code,
/// sends an `ActionEvent`.
///
/// The date is a day of the calendar of the device's time zone. The
/// chronology, the converter, the day cell factory, the editor and the
/// week numbers of JavaFX are not part of this layer.
///
/// ## Style
///
/// The style classes are `date-picker` and `combo-box-base`. The region
/// properties apply around the picker; the picker itself is drawn by the
/// Codename One theme.
public class DatePicker extends ComboBoxBase<LocalDate> {

    private boolean syncing;

    /// Creates a date picker with no date.
    public DatePicker() {
        this(null);
    }

    /// Creates a date picker holding a date.
    public DatePicker(LocalDate localDate) {
        getStyleClass().add("date-picker");
        valueRef().set(localDate);
        showingRef().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue != null && newValue.booleanValue()) {
                    open();
                } else {
                    Component c = cn1NativeIfCreated();
                    if (c instanceof Picker && ((Picker) c).isEditing()) {
                        ((Picker) c).stopEditing(null);
                    }
                }
            }
        });
    }

    private static Date toDate(LocalDate d) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, d.getYear());
        c.set(Calendar.MONTH, d.getMonthValue() - 1);
        c.set(Calendar.DAY_OF_MONTH, d.getDayOfMonth());
        // Noon, so that a change of the clock cannot move the day.
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static LocalDate toLocal(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return LocalDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private void open() {
        Component c = cn1NativeIfCreated();
        Scene s = getScene();
        boolean onScreen = s != null && s.getWindow() != null && s.getWindow().isShowing();
        if (onScreen && c instanceof Picker && !((Picker) c).isEditing()) {
            ((Picker) c).startEditingAsync();
        }
    }

    @Override
    void chooserReopen() {
        open();
    }

    @Override
    protected Component cn1CreateNative() {
        final Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        p.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                picked(p, evt);
            }
        });
        return p;
    }

    private void picked(Picker p, ActionEvent evt) {
        if (syncing) {
            return;
        }
        boolean committed = evt.getX() == -99 && evt.getY() == -99;
        if (!committed && p.isEditing()) {
            // The picker just opened; what it holds is not chosen yet.
            chooserShown(true);
            return;
        }
        Date d = p.getDate();
        LocalDate picked = d == null ? null : toLocal(d);
        LocalDate current = getValue();
        if (picked == null ? current != null : !picked.equals(current)) {
            setValue(picked);
        }
        chooserShown(false);
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c instanceof Picker) {
            Picker p = (Picker) c;
            syncing = true;
            try {
                LocalDate v = getValue();
                p.setDate(v == null ? null : toDate(v));
                if (v == null) {
                    String prompt = getPromptText();
                    p.setText(prompt == null ? "" : prompt);
                }
            } finally {
                syncing = false;
            }
        }
    }
}
