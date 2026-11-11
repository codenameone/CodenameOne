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

import com.codename1.l10n.DateFormatSymbols;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

import java.util.Calendar;
import java.util.Date;

/// A date picker in the Material 3 style: the chosen date in large type over
/// a month of days, a grid of years behind the month's name, and a text
/// field for somebody who would sooner type.
///
/// A `Picker` opens one in a dialog when the theme constant `pickerStyle` is
/// `material`. It is a component like any other and can be placed in a form
/// directly:
///
/// ```java
/// MaterialDatePicker picker = new MaterialDatePicker();
/// picker.setDate(new Date());
/// picker.addActionListener(e -> Log.p("Chosen: " + picker.getDate()));
///
/// Form form = new Form("Date", new BorderLayout());
/// form.add(BorderLayout.CENTER, picker);
/// form.show();
/// ```
///
/// The value is a `Date`. Its time of day is kept as it was given, so a date
/// chosen here does not move an appointment to midnight.
///
/// #### Styling
///
/// `PickerMaterialOverline` is the small title and `PickerMaterialHeadline`
/// the chosen date; `PickerMaterialButton` the name of the month, the arrows
/// and the pencil. `PickerMaterialWeekday` gives the initials of the days
/// their colour and `PickerMaterialDay` the days theirs: its selected style
/// is the circle of the chosen day, background and foreground, and its
/// disabled style a day outside the range. `PickerMaterialYear` is a year in
/// the grid, `PickerMaterialField` the text field and `PickerMaterialError`
/// what is said about a date that cannot be read.
public class MaterialDatePicker extends Container implements InternalPickerWidget {
    private static final int DAYS = 0;
    private static final int YEARS = 1;
    private static final int INPUT = 2;

    private int year;
    private int month;
    private int day;
    private int shownYear;
    private int shownMonth;
    // The time of day of the value, carried through a change of date.
    private int hour;
    private int minute;
    private int second;
    private int millis;
    private int minKey = Integer.MIN_VALUE;
    private int maxKey = Integer.MAX_VALUE;
    private int firstDayOfWeek = Calendar.SUNDAY;
    private int startYear = 1900;
    private int endYear = 2100;
    private int mode = DAYS;
    private int[] fieldOrder;
    private char separator = '/';

    private final Label overline = new Label("", "PickerMaterialOverline");
    private final Label headline = new Label("", "PickerMaterialHeadline");
    private final Button modeButton = new Button("", "PickerMaterialButton");
    private final Button monthButton = new Button("", "PickerMaterialButton");
    private final Button previous = new Button("", "PickerMaterialButton");
    private final Button next = new Button("", "PickerMaterialButton");
    private final Container navigation = new Container(new BorderLayout());
    private final DayPane days = new DayPane();
    private Container years;
    private final TextField input = new TextField();
    private final Label error = new Label("", "PickerMaterialError");
    private final Container inputPane = new Container(BoxLayout.y());
    private final Container body;
    private final EventDispatcher listeners = new EventDispatcher();
    private boolean updating;
    private final DateFormatSymbols symbols = new DateFormatSymbols();

    /// A picker showing today.
    public MaterialDatePicker() {
        super(new BorderLayout());
        setUIID("PickerMaterial");
        overline.setText(getUIManager().localize("picker.selectDate", "Select date"));
        Calendar c = Calendar.getInstance();
        read(c);
        shownYear = year;
        shownMonth = month;

        Container head = new Container(new BorderLayout());
        head.setUIID("PickerMaterialHeader");
        head.add(BorderLayout.NORTH, overline);
        head.add(BorderLayout.CENTER, headline);
        head.add(BorderLayout.EAST, modeButton);
        modeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setInputMode(mode != INPUT);
            }
        });

        monthButton.setTextPosition(Component.LEFT);
        FontImage.setMaterialIcon(monthButton, FontImage.MATERIAL_ARROW_DROP_DOWN);
        monthButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                show(mode == YEARS ? DAYS : YEARS);
            }
        });
        FontImage.setMaterialIcon(previous, FontImage.MATERIAL_CHEVRON_LEFT);
        FontImage.setMaterialIcon(next, FontImage.MATERIAL_CHEVRON_RIGHT);
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                page(-1);
            }
        });
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                page(1);
            }
        });
        Container arrows = new Container(BoxLayout.x());
        arrows.add(previous).add(next);
        navigation.add(BorderLayout.WEST, monthButton);
        navigation.add(BorderLayout.EAST, arrows);

        // Whatever the body shows, it asks for the room the month takes, so
        // that the dialog around it does not change its size.
        body = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension d = days.getPreferredSize();
                return new Dimension(d.getWidth(), d.getHeight() + navigation.getPreferredH());
            }
        };
        body.add(BorderLayout.NORTH, navigation);
        body.add(BorderLayout.CENTER, days);

        input.setUIID("PickerMaterialField");
        input.setConstraint(TextArea.NUMERIC);
        input.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                typed();
            }
        });
        inputPane.add(input).add(error);

        add(BorderLayout.NORTH, head);
        add(BorderLayout.CENTER, body);
        refresh();
    }

    // -------------------------------------------------------------- value

    private void read(Calendar c) {
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);
        hour = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);
        second = c.get(Calendar.SECOND);
        millis = c.get(Calendar.MILLISECOND);
    }

    private static int key(int y, int m, int d) {
        return y * 10000 + m * 100 + d;
    }

    private static int key(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return key(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    static int daysIn(int y, int m) {
        if (m == 1) {
            return y % 4 == 0 && (y % 100 != 0 || y % 400 == 0) ? 29 : 28;
        }
        return m == 3 || m == 5 || m == 8 || m == 10 ? 30 : 31;
    }

    /// The chosen date, at the time of day of the date last set.
    public Date getDate() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, millis);
        return c.getTime();
    }

    /// Chooses a date and shows its month. A date outside the range is
    /// brought to the nearest end of it. A null date is ignored.
    public void setDate(Date date) {
        if (date == null) {
            return;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        read(c);
        clamp();
        // The years on offer reach the date that is set, or its own month
        // could not be paged to and its own text would not be a date.
        reachYear();
        shownYear = year;
        shownMonth = month;
        refresh();
    }

    private void clamp() {
        int k = key(year, month, day);
        if (k < minKey) {
            k = minKey;
        } else if (k > maxKey) {
            k = maxKey;
        } else {
            return;
        }
        year = k / 10000;
        month = k / 100 % 100;
        day = k % 100;
    }

    @Override
    public Object getValue() {
        return getDate();
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Date) {
            setDate((Date) value);
        }
    }

    /// The first and the last day that can be chosen. Either may be null
    /// for no limit on that side.
    public void setDateRange(Date first, Date last) {
        minKey = first == null ? Integer.MIN_VALUE : key(first);
        maxKey = last == null ? Integer.MAX_VALUE : key(last);
        if (maxKey < minKey) {
            maxKey = minKey;
        }
        clamp();
        newYears();
        refresh();
    }

    private void reachYear() {
        if (year < startYear) {
            startYear = year;
            newYears();
        } else if (year > endYear) {
            endYear = year;
            newYears();
        }
    }

    // The grid of years is built for the limits it had; one that is on
    // screen is built again for the new ones.
    private void newYears() {
        years = null;
        if (mode == YEARS) {
            mode = DAYS;
            show(YEARS);
        }
    }

    /// Whether what is typed is a date that can be chosen. When it is not,
    /// the message under the field says so and the date is the one from
    /// before, which is not what the field reads.
    boolean acceptInput() {
        return mode != INPUT || error.getText().length() == 0;
    }

    /// The years the grid of years offers, 1900 to 2100 unless set. A date
    /// range narrows them further.
    public void setYearRange(int first, int last) {
        startYear = first;
        endYear = Math.max(first, last);
        reachYear();
        shownYear = Math.max(startYear, Math.min(endYear, shownYear));
        newYears();
        refresh();
    }

    /// The day a week starts on, as `java.util.Calendar` numbers them:
    /// `Calendar.SUNDAY` by default, `Calendar.MONDAY` for most of Europe.
    public void setFirstDayOfWeek(int calendarDay) {
        if (calendarDay < Calendar.SUNDAY || calendarDay > Calendar.SATURDAY) {
            throw new IllegalArgumentException("calendarDay");
        }
        firstDayOfWeek = calendarDay;
        days.repaint();
    }

    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    /// The small title over the date: "Select date" unless set.
    public void setTitle(String title) {
        overline.setText(title == null ? "" : title);
    }

    public String getTitle() {
        return overline.getText();
    }

    /// Told when the user chooses another date.
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    boolean isEnabledDay(int y, int m, int d) {
        int k = key(y, m, d);
        return k >= minKey && k <= maxKey;
    }

    /// Chooses a day as a tap on it does.
    void choose(int y, int m, int d) {
        if (!isEnabledDay(y, m, d)) {
            return;
        }
        boolean differs = y != year || m != month || d != day;
        year = y;
        month = m;
        day = d;
        shownYear = y;
        shownMonth = m;
        refresh();
        if (differs) {
            listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Other));
        }
    }

    // --------------------------------------------------------------- view

    int getShownYear() {
        return shownYear;
    }

    int getShownMonth() {
        return shownMonth;
    }

    Component dayPane() {
        return days;
    }

    TextField inputField() {
        return input;
    }

    String errorText() {
        return error.getText();
    }

    /// Shows the month before or after the one shown.
    void page(int months) {
        int m = shownYear * 12 + shownMonth + months;
        int lo = Math.max(startYear * 12, minKey == Integer.MIN_VALUE ? Integer.MIN_VALUE : minKey / 10000 * 12 + minKey / 100 % 100);
        int hi = Math.min(endYear * 12 + 11, maxKey == Integer.MAX_VALUE ? Integer.MAX_VALUE : maxKey / 10000 * 12 + maxKey / 100 % 100);
        m = Math.max(lo, Math.min(hi, m));
        shownYear = m / 12;
        shownMonth = m % 12;
        refresh();
    }

    /// Whether the date is typed and not picked from the month.
    public boolean isInputMode() {
        return mode == INPUT;
    }

    public void setInputMode(boolean v) {
        show(v ? INPUT : DAYS);
    }

    private void show(int m) {
        if (m == mode) {
            return;
        }
        mode = m;
        body.removeAll();
        if (m == INPUT) {
            body.add(BorderLayout.NORTH, inputPane);
            error.setText("");
            updating = true;
            input.setText(typedForm(year, month, day));
            input.setHint(hint());
            updating = false;
        } else {
            body.add(BorderLayout.NORTH, navigation);
            if (m == YEARS) {
                body.add(BorderLayout.CENTER, yearPane());
            } else {
                body.add(BorderLayout.CENTER, days);
            }
        }
        refresh();
        body.revalidateLater();
    }

    private void refresh() {
        DateFormatSymbols s = symbols;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        headline.setText(s.getShortWeekdays()[c.get(Calendar.DAY_OF_WEEK) - 1] + ", "
                + s.getShortMonths()[month] + " " + day);
        monthButton.setText(s.getMonths()[shownMonth] + " " + shownYear);
        FontImage.setMaterialIcon(modeButton, mode == INPUT ? FontImage.MATERIAL_CALENDAR_TODAY : FontImage.MATERIAL_EDIT);
        previous.setVisible(mode == DAYS);
        next.setVisible(mode == DAYS);
        days.repaint();
    }

    private Container yearPane() {
        if (years == null) {
            int first = Math.max(startYear, minKey == Integer.MIN_VALUE ? startYear : minKey / 10000);
            int last = Math.min(endYear, maxKey == Integer.MAX_VALUE ? endYear : maxKey / 10000);
            last = Math.max(first, last);
            Container grid = new Container(new GridLayout((last - first + 3) / 3, 3));
            grid.setScrollableY(true);
            for (int y = first; y <= last; y++) {
                final int value = y;
                Button b = new Button(String.valueOf(y), "PickerMaterialYear");
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        shownYear = value;
                        page(0);
                        show(DAYS);
                    }
                });
                grid.add(b);
            }
            years = grid;
        }
        // The year shown is the one to start from, not 1900.
        int count = years.getComponentCount();
        for (int i = 0; i < count; i++) {
            Button b = (Button) years.getComponentAt(i);
            boolean current = b.getText().equals(String.valueOf(shownYear));
            b.setUIID(current ? "PickerMaterialYearSelected" : "PickerMaterialYear");
            if (current) {
                Display.getInstance().callSerially(new Reveal(years, b));
            }
        }
        return years;
    }

    // -------------------------------------------------------------- typed

    // The order a short date is written in here: which of the three numbers
    // is the year, the month and the day. Read off a date whose three parts
    // cannot be mistaken for one another.
    private int[] order() {
        if (fieldOrder == null) {
            int[] o = {0, 1, 2};
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, 1999);
            c.set(Calendar.MONTH, 10);
            c.set(Calendar.DAY_OF_MONTH, 22);
            String s = L10NManager.getInstance().formatDateShortStyle(c.getTime());
            int y = s.indexOf("99");
            int m = s.indexOf("11");
            int d = s.indexOf("22");
            if (y >= 0 && m >= 0 && d >= 0) {
                // Each field's place among the three: 0 year, 1 month, 2 day.
                o[0] = (y > m ? 1 : 0) + (y > d ? 1 : 0);
                o[1] = (m > y ? 1 : 0) + (m > d ? 1 : 0);
                o[2] = (d > y ? 1 : 0) + (d > m ? 1 : 0);
                for (int i = Math.min(m, d); i < s.length(); i++) {
                    char ch = s.charAt(i);
                    if (ch < '0' || ch > '9') {
                        separator = ch;
                        break;
                    }
                }
            }
            fieldOrder = o;
        }
        return fieldOrder;
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    String typedForm(int y, int m, int d) {
        int[] o = order();
        String[] parts = new String[3];
        parts[o[0]] = String.valueOf(y);
        parts[o[1]] = two(m + 1);
        parts[o[2]] = two(d);
        return parts[0] + separator + parts[1] + separator + parts[2];
    }

    private String hint() {
        int[] o = order();
        String[] parts = new String[3];
        parts[o[0]] = "yyyy";
        parts[o[1]] = "mm";
        parts[o[2]] = "dd";
        return parts[0] + separator + parts[1] + separator + parts[2];
    }

    private void typed() {
        if (updating || mode != INPUT) {
            return;
        }
        String text = input.getText();
        int[] numbers = new int[3];
        int count = 0;
        boolean inNumber = false;
        boolean bad = false;
        for (int i = 0; text != null && i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '0' && ch <= '9') {
                if (!inNumber) {
                    if (count == 3) {
                        bad = true;
                        break;
                    }
                    count++;
                    inNumber = true;
                }
                numbers[count - 1] = Math.min(100000, numbers[count - 1] * 10 + (ch - '0'));
            } else {
                inNumber = false;
            }
        }
        int[] o = order();
        if (!bad && count == 1 && text.length() == 8) {
            // A keyboard of digits only has no separator to type: eight
            // digits are the three parts, the year four of them.
            boolean digits = true;
            for (int i = 0; i < 8; i++) {
                char ch = text.charAt(i);
                digits = digits && ch >= '0' && ch <= '9';
            }
            if (digits) {
                int at = 0;
                for (int part = 0; part < 3; part++) {
                    int width = part == o[0] ? 4 : 2;
                    int v = 0;
                    for (int i = 0; i < width; i++) {
                        v = v * 10 + (text.charAt(at + i) - '0');
                    }
                    numbers[part] = v;
                    at += width;
                }
                count = 3;
            }
        }
        int y = numbers[o[0]];
        int m = numbers[o[1]] - 1;
        int d = numbers[o[2]];
        if (bad || count != 3 || m < 0 || m > 11 || d < 1 || d > daysIn(y, m) || y < startYear || y > endYear) {
            error.setText(getUIManager().localize("picker.invalidDate", "Invalid date") + ": " + hint());
            return;
        }
        if (!isEnabledDay(y, m, d)) {
            error.setText(getUIManager().localize("picker.outOfRange", "Out of range"));
            return;
        }
        error.setText("");
        updating = true;
        choose(y, m, d);
        updating = false;
    }

    // ---------------------------------------------------------- the month

    /// The days of the month shown, painted and not built of components: a
    /// row of initials and six weeks.
    private final class DayPane extends Component {
        private int pressedDay;

        DayPane() {
            setUIID("PickerMaterialDays");
            setFocusable(false);
        }

        @Override
        protected Dimension calcPreferredSize() {
            int cell = Math.max(Display.getInstance().convertToPixels(6.2f),
                    getUIManager().getComponentStyle("PickerMaterialDay").getFont().getHeight() * 2);
            Style s = getStyle();
            return new Dimension(cell * 7 + s.getHorizontalPadding(), cell * 7 + s.getVerticalPadding());
        }

        // Which cell of the grid the first of the month is in.
        private int lead() {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.YEAR, shownYear);
            c.set(Calendar.MONTH, shownMonth);
            int l = c.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek;
            return l < 0 ? l + 7 : l;
        }

        @Override
        public void paint(Graphics g) {
            Style st = getStyle();
            int x = getX() + st.getPaddingLeftNoRTL();
            int y = getY() + st.getPaddingTop();
            int w = getWidth() - st.getHorizontalPadding();
            int h = getHeight() - st.getVerticalPadding();
            int cw = w / 7;
            int chh = h / 7;
            x += (w - cw * 7) / 2;
            Style dayStyle = getUIManager().getComponentStyle("PickerMaterialDay");
            Style chosen = getUIManager().getComponentSelectedStyle("PickerMaterialDay");
            Style off = getUIManager().getComponentCustomStyle("PickerMaterialDay", "dis");
            Style week = getUIManager().getComponentStyle("PickerMaterialWeekday");
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            String[] names = symbols.getShortWeekdays();
            Font wf = week.getFont();
            g.setFont(wf);
            g.setColor(week.getFgColor());
            for (int i = 0; i < 7; i++) {
                String n = names[(firstDayOfWeek - 1 + i) % 7];
                String initial = n == null || n.length() == 0 ? "" : n.substring(0, 1);
                g.drawString(initial, x + i * cw + (cw - wf.stringWidth(initial)) / 2, y + (chh - wf.getHeight()) / 2);
            }
            Calendar now = Calendar.getInstance();
            int today = shownYear == now.get(Calendar.YEAR) && shownMonth == now.get(Calendar.MONTH)
                    ? now.get(Calendar.DAY_OF_MONTH) : -1;
            int lead = lead();
            int count = daysIn(shownYear, shownMonth);
            int r = Math.min(cw, chh) / 2 - 1;
            Font f = dayStyle.getFont();
            g.setFont(f);
            int alpha = g.getAlpha();
            for (int d = 1; d <= count; d++) {
                int cell = lead + d - 1;
                int cx = x + cell % 7 * cw + cw / 2;
                int cy = y + (cell / 7 + 1) * chh + chh / 2;
                boolean isChosen = d == day && shownMonth == month && shownYear == year;
                boolean enabled = isEnabledDay(shownYear, shownMonth, d);
                if (isChosen) {
                    g.setColor(chosen.getBgColor());
                    g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 0, 360);
                    g.setColor(chosen.getFgColor());
                } else if (d == today) {
                    g.setColor(chosen.getBgColor());
                    g.drawArc(cx - r, cy - r, 2 * r - 1, 2 * r - 1, 0, 360);
                } else if (!enabled) {
                    g.setColor(off.getFgColor());
                    g.setAlpha(alpha * 97 / 255);
                } else {
                    g.setColor(dayStyle.getFgColor());
                }
                String s = String.valueOf(d);
                g.drawString(s, cx - f.stringWidth(s) / 2, cy - f.getHeight() / 2);
                g.setAlpha(alpha);
            }
            g.setAntiAliased(aa);
        }

        private int dayAt(int px, int py) {
            Style st = getStyle();
            int w = getWidth() - st.getHorizontalPadding();
            int h = getHeight() - st.getVerticalPadding();
            int cw = Math.max(1, w / 7);
            int chh = Math.max(1, h / 7);
            int x = px - getAbsoluteX() - st.getPaddingLeftNoRTL() - (w - cw * 7) / 2;
            int y = py - getAbsoluteY() - st.getPaddingTop();
            if (x < 0 || y < chh || x >= cw * 7 || y >= chh * 7) {
                return -1;
            }
            int d = (y / chh - 1) * 7 + x / cw - lead() + 1;
            return d < 1 || d > daysIn(shownYear, shownMonth) ? -1 : d;
        }

        @Override
        public void pointerPressed(int x, int y) {
            pressedDay = dayAt(x, y);
        }

        @Override
        public void pointerReleased(int x, int y) {
            int d = dayAt(x, y);
            if (d > 0 && d == pressedDay) {
                choose(shownYear, shownMonth, d);
            }
            pressedDay = -1;
        }
    }

    /// The point of a day of the month shown, for a test to tap.
    int[] pointOfDay(int d) {
        Style st = days.getStyle();
        int w = days.getWidth() - st.getHorizontalPadding();
        int h = days.getHeight() - st.getVerticalPadding();
        int cw = w / 7;
        int chh = h / 7;
        int cell = days.lead() + d - 1;
        return new int[]{
            days.getAbsoluteX() + st.getPaddingLeftNoRTL() + (w - cw * 7) / 2 + cell % 7 * cw + cw / 2,
            days.getAbsoluteY() + st.getPaddingTop() + (cell / 7 + 1) * chh + chh / 2
        };
    }

    // Scrolls the year shown into view once the grid of years has a layout.
    private static final class Reveal implements Runnable {
        private final Container pane;
        private final Component target;

        Reveal(Container pane, Component target) {
            this.pane = pane;
            this.target = target;
        }

        @Override
        public void run() {
            pane.scrollComponentToVisible(target);
        }
    }
}
