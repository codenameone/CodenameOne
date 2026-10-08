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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.CalendarUtils;
import com.codename1.desktopcompat.rt.DatePickerPeer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/// A date shown as text that the user changes with the platform's date
/// chooser.
///
/// ## How it is shown
///
/// The picker is a Codename One `Picker` of dates: a button that carries
/// the date, formatted with the first of the picker's formats, and opens
/// the native date chooser (or Codename One's own wheels where there is
/// none) when tapped. Choosing a date there sets it and tells the action
/// listeners [#COMMIT_KEY].
///
/// ## What differs from SwingX
///
///  - There is no text editor, so a date cannot be typed:
///    `getEditor`, `setEditor` and the link panel are absent,
///    [#isEditValid()] is always true, and the formats after the first
///    serve only an application that parses with [#getFormats()] itself.
///  - There is no popup with the month view. The view answered by
///    [#getMonthView()] is kept in step with the picker -- it carries the
///    selection, the bounds and the time zone, and committing it sets the
///    picker's date -- but it is shown only if the application adds it
///    somewhere.
///  - A picker that is not editable does not open the chooser.
///  - The default formats are the device's long, medium and short date
///    styles.
public class JXDatePicker extends JComponent {

    public static final String uiClassID = "DatePickerUI";
    public static final String EDITOR = "editor";
    public static final String MONTH_VIEW = "monthView";
    public static final String LINK_PANEL = "linkPanel";
    public static final String COMMIT_KEY = "datePickerCommit";
    public static final String CANCEL_KEY = "datePickerCancel";
    public static final String HOME_NAVIGATE_KEY = "navigateHome";
    public static final String HOME_COMMIT_KEY = "commitHome";

    protected boolean lightWeightPopupEnabled = true;

    private Date date;
    private DateFormat[] formats;
    private JXMonthView monthView;
    private boolean editable = true;
    private boolean pushing;
    private final ActionListener monthViewListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (JXMonthView.COMMIT_KEY.equals(e.getActionCommand())) {
                cn1Chosen(monthView.getSelectionDate());
            } else if (JXMonthView.CANCEL_KEY.equals(e.getActionCommand())) {
                cancelEdit();
            }
        }
    };

    public JXDatePicker() {
        this(null, null);
    }

    public JXDatePicker(Date selected) {
        this(selected, null);
    }

    public JXDatePicker(Locale locale) {
        this(null, locale);
    }

    /// A picker showing `selection`, or no date for `null`. The locale is
    /// handed to the month view, which records it.
    public JXDatePicker(Date selection, Locale locale) {
        formats = new DateFormat[]{DateFormat.getDateInstance(DateFormat.LONG),
            DateFormat.getDateInstance(DateFormat.MEDIUM), DateFormat.getDateInstance(DateFormat.SHORT)};
        monthView = new JXMonthView(locale);
        monthView.setTraversable(true);
        monthView.addActionListener(monthViewListener);
        if (selection != null) {
            date = monthView.getSelectionModel().getNormalizedDate(selection);
            monthView.setSelectionDate(date);
            monthView.ensureDateVisible(date);
        }
    }

    // ------------------------------------------------------------ peer

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new DatePickerPeer(this, new DatePickerPeer.Host() {
            @Override
            public String text(Date d) {
                return cn1Format(d);
            }

            @Override
            public void valueChanged(Date d) {
                if (!pushing) {
                    cn1Chosen(d);
                }
            }
        });
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        push();
    }

    /// Writes the date, the bounds and whether the chooser may open to
    /// the picker.
    private void push() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (!(p instanceof DatePickerPeer)) {
            return;
        }
        DatePickerPeer picker = (DatePickerPeer) p;
        pushing = true;
        try {
            picker.setStartDate(monthView.getLowerBound());
            picker.setEndDate(monthView.getUpperBound());
            picker.setDate(date == null ? null : new Date(date.getTime()));
            picker.setEnabled(isEnabled() && editable);
        } finally {
            pushing = false;
        }
    }

    /// The text that shows a date: the first format's, and nothing for no
    /// date.
    private String cn1Format(Date d) {
        if (d == null || formats.length == 0) {
            return "";
        }
        return formats[0].format(d);
    }

    /// Takes a date chosen by the user and tells the action listeners.
    private void cn1Chosen(Date d) {
        Date n = d == null ? null : monthView.getSelectionModel().getNormalizedDate(d);
        if (!CalendarUtils.areEqual(n, date)) {
            setDate(n);
        }
        fireActionPerformed(COMMIT_KEY);
    }

    // ------------------------------------------------------------ date

    /// Sets the date, as the first millisecond of its day, or no date for
    /// `null`.
    public void setDate(Date date) {
        Date old = this.date;
        Date n = date == null ? null : monthView.getSelectionModel().getNormalizedDate(date);
        this.date = n;
        monthView.setSelectionDate(n);
        if (n != null) {
            monthView.ensureDateVisible(n);
        }
        push();
        if (!CalendarUtils.areEqual(old, n)) {
            firePropertyChange("date", old, getDate());
            revalidate();
            repaint();
        }
    }

    public Date getDate() {
        return date == null ? null : new Date(date.getTime());
    }

    /// Does nothing: there are no UI delegates.
    @Override
    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    // ------------------------------------------------------------ formats

    /// Sets the formats from date patterns; the first one shows the date.
    /// No patterns, or `null`, restores nothing: the picker then shows no
    /// text.
    public void setFormats(String... formats) {
        DateFormat[] made = new DateFormat[formats == null ? 0 : formats.length];
        for (int i = 0; i < made.length; i++) {
            if (formats[i] == null) {
                throw new NullPointerException("the array of format strings must not contain null elements");
            }
            made[i] = new SimpleDateFormat(formats[i]);
        }
        setFormats(made);
    }

    /// Sets the formats; the first one shows the date.
    public void setFormats(DateFormat... formats) {
        DateFormat[] made = new DateFormat[formats == null ? 0 : formats.length];
        for (int i = 0; i < made.length; i++) {
            if (formats[i] == null) {
                throw new NullPointerException("the array of formats must not contain null elements");
            }
            made[i] = formats[i];
        }
        DateFormat[] old = getFormats();
        this.formats = made;
        push();
        firePropertyChange("formats", old, getFormats());
        revalidate();
        repaint();
    }

    public DateFormat[] getFormats() {
        DateFormat[] out = new DateFormat[formats.length];
        System.arraycopy(formats, 0, out, 0, formats.length);
        return out;
    }

    // ------------------------------------------------------------ month view

    public JXMonthView getMonthView() {
        return monthView;
    }

    /// Replaces the month view. The new view is given the picker's date.
    public void setMonthView(JXMonthView monthView) {
        if (monthView == null) {
            throw new NullPointerException("monthView must not be null");
        }
        JXMonthView old = this.monthView;
        if (old == monthView) {
            return;
        }
        old.removeActionListener(monthViewListener);
        this.monthView = monthView;
        monthView.addActionListener(monthViewListener);
        Date d = date;
        date = d == null ? null : monthView.getSelectionModel().getNormalizedDate(d);
        monthView.setSelectionDate(date);
        push();
        firePropertyChange(MONTH_VIEW, old, monthView);
    }

    public TimeZone getTimeZone() {
        return monthView.getTimeZone();
    }

    /// Sets the time zone of the month view. The date keeps its instant
    /// and is read as a day of the new zone.
    public void setTimeZone(TimeZone tz) {
        TimeZone old = getTimeZone();
        Date d = date;
        monthView.setTimeZone(tz);
        if (d != null) {
            setDate(d);
        }
        firePropertyChange("timeZone", old, getTimeZone());
    }

    // ------------------------------------------------------------ editing

    /// Always `true`: there is no text to be invalid.
    public boolean isEditValid() {
        return true;
    }

    /// Takes the date the chooser holds and tells the action listeners
    /// [#COMMIT_KEY]. Nothing is parsed, so nothing is thrown.
    public void commitEdit() throws ParseException {
        com.codename1.ui.Component p = cn1PeerOrNull();
        Date d = date;
        if (p instanceof DatePickerPeer) {
            Object v = ((DatePickerPeer) p).getValue();
            d = v instanceof Date ? (Date) v : null;
        }
        cn1Chosen(d);
    }

    /// Puts the picker's date back into the chooser and the month view
    /// and tells the action listeners [#CANCEL_KEY].
    public void cancelEdit() {
        monthView.setSelectionDate(date);
        push();
        fireActionPerformed(CANCEL_KEY);
    }

    /// Whether tapping the picker opens the chooser.
    public void setEditable(boolean value) {
        boolean old = editable;
        editable = value;
        push();
        firePropertyChange("editable", old, value);
    }

    public boolean isEditable() {
        return editable;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        push();
    }

    /// Recorded only: the chooser is the platform's.
    public void setLightWeightPopupEnabled(boolean aFlag) {
        boolean old = lightWeightPopupEnabled;
        lightWeightPopupEnabled = aFlag;
        firePropertyChange("lightWeightPopupEnabled", old, aFlag);
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    // ------------------------------------------------------------ actions

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    protected void fireActionPerformed(String actionCommand) {
        ActionListener[] ls = listenerList.getListeners(ActionListener.class);
        if (ls.length == 0) {
            return;
        }
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].actionPerformed(e);
        }
    }
}
