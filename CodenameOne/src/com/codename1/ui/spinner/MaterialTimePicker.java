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
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.MathUtil;

/// A time picker in the Material 3 style: the hour and the minute in large
/// type over a clock face. The hand is dragged to the hour, the face then
/// turns to minutes, and a keyboard button swaps the face for two text
/// fields.
///
/// A `Picker` opens one in a dialog when the theme constant `pickerStyle` is
/// `material`. It is a component like any other and can be placed in a form
/// directly. The value is an `Integer`, the minutes since midnight, as it is
/// for the time of a `Picker`.
///
/// With `setShowMeridiem(true)` the face has twelve hours and AM and PM
/// beside the time; without, it has two rings, the afternoon inside.
///
/// #### Styling
///
/// `PickerMaterialOverline` is the small title. `PickerMaterialTimeField` is
/// the hour and the minute, its selected style the one the face is showing,
/// and `PickerMaterialTimeSeparator` the colon between them.
/// `PickerMaterialPeriod` is AM and PM. `PickerMaterialDial` is the face:
/// its background the disc and its foreground the numbers, and its selected
/// style the hand, with the foreground the number under it. Its disabled
/// style is an hour outside the range. `PickerMaterialButton` is the
/// keyboard button and `PickerMaterialField` a text field.
public class MaterialTimePicker extends Container implements InternalPickerWidget {
    private int hour;
    private int minute;
    private boolean meridiem;
    private int minuteStep = 1;
    private int minHour = -1;
    private int maxHour = -1;
    private boolean minutesShown;
    private boolean inputMode;
    private boolean typedWrong;
    private boolean pointedHour;
    private boolean updating;

    private final Label overline = new Label("", "PickerMaterialOverline");
    private final RadioButton hourField = new RadioButton("");
    private final RadioButton minuteField = new RadioButton("");
    private final TextField hourInput = new TextField();
    private final TextField minuteInput = new TextField();
    private final RadioButton am = new RadioButton("");
    private final RadioButton pm = new RadioButton("");
    private final Container periods = new Container(BoxLayout.y());
    private final Container time = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
    private final Dial dial = new Dial();
    private final Button modeButton = new Button("", "PickerMaterialButton");
    private final Container modeRow = new Container(new FlowLayout(Component.LEFT));
    private final EventDispatcher listeners = new EventDispatcher();

    /// A picker showing midnight on a 24 hour face.
    public MaterialTimePicker() {
        super(new BorderLayout());
        setUIID("PickerMaterial");
        overline.setText(getUIManager().localize("picker.selectTime", "Select time"));
        String[] names = new DateFormatSymbols().getAmPmStrings();
        am.setText(names != null && names.length > 1 ? names[0] : "AM");
        pm.setText(names != null && names.length > 1 ? names[1] : "PM");

        ButtonGroup fields = new ButtonGroup();
        field(hourField, fields);
        field(minuteField, fields);
        hourField.setSelected(true);
        hourField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showMinutes(false);
            }
        });
        minuteField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showMinutes(true);
            }
        });
        typedField(hourInput);
        typedField(minuteInput);

        ButtonGroup half = new ButtonGroup();
        am.setToggle(true);
        pm.setToggle(true);
        am.setUIID("PickerMaterialPeriod");
        pm.setUIID("PickerMaterialPeriod");
        half.add(am);
        half.add(pm);
        ActionListener halfChosen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                int h = hour % 12 + (pm.isSelected() ? 12 : 0);
                if (h != hour && isEnabledHour(h)) {
                    hour = h;
                    changed();
                }
                refresh();
            }
        };
        am.addActionListener(halfChosen);
        pm.addActionListener(halfChosen);
        periods.add(am).add(pm);

        Container head = new Container(new BorderLayout());
        head.setUIID("PickerMaterialHeader");
        head.add(BorderLayout.NORTH, overline);
        head.add(BorderLayout.CENTER, time);

        FontImage.setMaterialIcon(modeButton, FontImage.MATERIAL_KEYBOARD);
        modeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setInputMode(!inputMode);
            }
        });
        modeRow.add(modeButton);

        add(BorderLayout.NORTH, head);
        add(BorderLayout.CENTER, dial);
        add(BorderLayout.SOUTH, modeRow);
        buildTime();
        refresh();
    }

    private void field(RadioButton b, ButtonGroup g) {
        b.setToggle(true);
        b.setUIID("PickerMaterialTimeField");
        g.add(b);
    }

    private void typedField(TextField f) {
        f.setUIID("PickerMaterialField");
        f.setConstraint(TextArea.NUMERIC);
        f.setColumns(2);
        f.setMaxSize(2);
        f.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                typed();
            }
        });
    }

    private void buildTime() {
        time.removeAll();
        if (inputMode) {
            time.add(hourInput);
        } else {
            time.add(hourField);
        }
        time.add(new Label(":", "PickerMaterialTimeSeparator"));
        if (inputMode) {
            time.add(minuteInput);
        } else {
            time.add(minuteField);
        }
        if (meridiem) {
            time.add(periods);
        }
    }

    // -------------------------------------------------------------- value

    /// The hour of the day, 0 to 23.
    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    /// Sets the time. The hour is of the day, 0 to 23, whatever the face
    /// shows; an hour outside the range is brought to the nearest one in it.
    /// The minute is kept as it is given: the step is where the hand stops
    /// when it is turned, and a time that is only looked at is not changed
    /// by having been shown.
    public void setTime(int hourOfDay, int minuteOfHour) {
        int h = Math.max(0, Math.min(23, hourOfDay));
        if (minHour >= 0 && maxHour > minHour) {
            h = Math.max(minHour, Math.min(maxHour - 1, h));
        }
        hour = h;
        minute = Math.max(0, Math.min(59, minuteOfHour));
        typedWrong = false;
        refresh();
    }

    /// Whether what is typed is a time. When it is not, the fields are
    /// written again with the time that is set, so that what is confirmed
    /// next is what is read there and not something that was never shown.
    boolean acceptInput() {
        if (!inputMode || !typedWrong) {
            return true;
        }
        typedWrong = false;
        refresh();
        return false;
    }

    /// The minutes since midnight, as an `Integer`.
    @Override
    public Object getValue() {
        return Integer.valueOf(hour * 60 + minute);
    }

    /// Sets the time from the minutes since midnight. Anything that is not
    /// an `Integer` is ignored.
    @Override
    public void setValue(Object value) {
        if (value instanceof Integer) {
            int v = ((Integer) value).intValue();
            setTime(v / 60, v % 60);
        }
    }

    /// Whether the face has twelve hours with AM and PM beside the time.
    public boolean isShowMeridiem() {
        return meridiem;
    }

    public void setShowMeridiem(boolean v) {
        if (meridiem != v) {
            meridiem = v;
            buildTime();
            refresh();
            time.revalidateLater();
        }
    }

    /// The minutes the hand stops at: 5 for every five minutes, 1 for all.
    public void setMinuteStep(int step) {
        minuteStep = Math.max(1, Math.min(30, step));
        refresh();
    }

    public int getMinuteStep() {
        return minuteStep;
    }

    /// The hours that can be chosen: from `min` up to but not `max`. A
    /// negative `min` removes the limit.
    public void setHourRange(int min, int max) {
        if (min < 0 || max <= min || max > 24) {
            minHour = -1;
            maxHour = -1;
        } else {
            minHour = min;
            maxHour = max;
            setTime(hour, minute);
        }
        dial.repaint();
    }

    boolean isEnabledHour(int h) {
        return minHour < 0 || (h >= minHour && h < maxHour);
    }

    /// The small title over the time: "Select time" unless set.
    public void setTitle(String title) {
        overline.setText(title == null ? "" : title);
    }

    public String getTitle() {
        return overline.getText();
    }

    /// Told when the user chooses another time.
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    private void changed() {
        listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Other));
    }

    // --------------------------------------------------------------- view

    /// Whether the time is typed and not picked from the face.
    public boolean isInputMode() {
        return inputMode;
    }

    public void setInputMode(boolean v) {
        if (inputMode == v) {
            return;
        }
        inputMode = v;
        dial.setHidden(v);
        buildTime();
        refresh();
        revalidateLater();
    }

    /// The button that swaps the face for text fields. It is under the face
    /// until something takes it: a dialog puts it beside its own buttons.
    public Button getInputModeButton() {
        return modeButton;
    }

    boolean isMinutesShown() {
        return minutesShown;
    }

    void showMinutes(boolean v) {
        minutesShown = v;
        refresh();
    }

    Component dialPane() {
        return dial;
    }

    TextField hourInputField() {
        return hourInput;
    }

    TextField minuteInputField() {
        return minuteInput;
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    private String hourText() {
        if (meridiem) {
            int h = hour % 12;
            return String.valueOf(h == 0 ? 12 : h);
        }
        return two(hour);
    }

    private void refresh() {
        hourField.setText(hourText());
        minuteField.setText(two(minute));
        hourField.setSelected(!minutesShown);
        minuteField.setSelected(minutesShown);
        am.setSelected(hour < 12);
        pm.setSelected(hour >= 12);
        FontImage.setMaterialIcon(modeButton, inputMode ? FontImage.MATERIAL_SCHEDULE : FontImage.MATERIAL_KEYBOARD);
        if (inputMode && !updating) {
            updating = true;
            hourInput.setText(hourText());
            minuteInput.setText(two(minute));
            updating = false;
        }
        dial.repaint();
    }

    private static int number(String s) {
        if (s == null || s.length() == 0 || s.length() > 2) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    private void typed() {
        if (updating || !inputMode) {
            return;
        }
        int h = number(hourInput.getText());
        int m = number(minuteInput.getText());
        typedWrong = true;
        if (meridiem) {
            if (h < 1 || h > 12) {
                return;
            }
            h = h % 12 + (hour >= 12 ? 12 : 0);
        }
        if (h < 0 || h > 23 || m < 0 || m > 59 || !isEnabledHour(h)) {
            return;
        }
        // A minute that is typed is the minute that is meant; the step is
        // for the hand.
        typedWrong = false;
        if (h != hour || m != minute) {
            hour = h;
            minute = m;
            // The fields keep what was typed: writing "07" back over a "7"
            // would move the caret under the hand that is typing.
            updating = true;
            refresh();
            updating = false;
            changed();
        }
    }

    // ----------------------------------------------------------- the face

    /// The clock face, painted.
    private final class Dial extends Component {
        Dial() {
            setUIID("PickerMaterialDialPane");
            setFocusable(false);
        }

        @Override
        protected Dimension calcPreferredSize() {
            int d = Display.getInstance().convertToPixels(40);
            Style s = getStyle();
            return new Dimension(d + s.getHorizontalPadding(), d + s.getVerticalPadding());
        }

        private int radius() {
            Style s = getStyle();
            return Math.min(getWidth() - s.getHorizontalPadding(), getHeight() - s.getVerticalPadding()) / 2;
        }

        private int knob(int r) {
            return Math.max(3, r * 19 / 100);
        }

        @Override
        public void paint(Graphics g) {
            Style face = getUIManager().getComponentStyle("PickerMaterialDial");
            Style hand = getUIManager().getComponentSelectedStyle("PickerMaterialDial");
            Style off = getUIManager().getComponentCustomStyle("PickerMaterialDial", "dis");
            int r = radius();
            if (r <= 0) {
                return;
            }
            int cx = getX() + getWidth() / 2;
            int cy = getY() + getHeight() / 2;
            int k = knob(r);
            int outer = r - k - Math.max(1, r / 28);
            int inner = outer - 2 * k + k / 5;
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setColor(face.getBgColor());
            g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 0, 360);

            // The hand first, then the numbers over it.
            int at;
            int ring = outer;
            if (minutesShown) {
                at = minute * 6;
            } else {
                at = hour * 30 % 360;
                if (!meridiem && (hour == 0 || hour > 12)) {
                    ring = inner;
                }
            }
            double a = Math.toRadians(at);
            int hx = cx + (int) Math.round(Math.sin(a) * ring);
            int hy = cy - (int) Math.round(Math.cos(a) * ring);
            g.setColor(hand.getBgColor());
            g.drawLine(cx, cy, hx, hy);
            g.drawLine(cx + 1, cy, hx + 1, hy);
            g.fillArc(cx - k / 4, cy - k / 4, k / 2, k / 2, 0, 360);
            g.fillArc(hx - k, hy - k, 2 * k, 2 * k, 0, 360);

            Font f = face.getFont();
            g.setFont(f);
            int alpha = g.getAlpha();
            boolean marked = false;
            for (int i = 0; i < 12; i++) {
                if (minutesShown) {
                    boolean under = minute == i * 5;
                    marked |= under;
                    number(g, f, two(i * 5), cx, cy, outer, i, under ? hand.getFgColor() : face.getFgColor());
                } else if (meridiem) {
                    int h = i + (hour >= 12 ? 12 : 0);
                    boolean ok = isEnabledHour(h);
                    g.setAlpha(ok ? alpha : alpha * 97 / 255);
                    number(g, f, String.valueOf(i == 0 ? 12 : i), cx, cy, outer, i,
                            hour % 12 == i ? hand.getFgColor() : ok ? face.getFgColor() : off.getFgColor());
                } else {
                    int morning = i == 0 ? 12 : i;
                    int evening = i == 0 ? 0 : i + 12;
                    g.setAlpha(isEnabledHour(morning) ? alpha : alpha * 97 / 255);
                    number(g, f, String.valueOf(morning), cx, cy, outer, i,
                            hour == morning ? hand.getFgColor() : isEnabledHour(morning) ? face.getFgColor() : off.getFgColor());
                    g.setAlpha(isEnabledHour(evening) ? alpha : alpha * 97 / 255);
                    number(g, f, two(evening), cx, cy, inner, i,
                            hour == evening ? hand.getFgColor() : isEnabledHour(evening) ? face.getFgColor() : off.getFgColor());
                }
                g.setAlpha(alpha);
            }
            if (minutesShown && !marked) {
                // Between two numbers: a dot says where exactly.
                g.setColor(hand.getFgColor());
                g.fillArc(hx - 2, hy - 2, 4, 4, 0, 360);
            }
            g.setAntiAliased(aa);
        }

        private void number(Graphics g, Font f, String s, int cx, int cy, int ring, int i, int color) {
            double a = Math.toRadians(i * 30);
            int x = cx + (int) Math.round(Math.sin(a) * ring);
            int y = cy - (int) Math.round(Math.cos(a) * ring);
            g.setColor(color);
            g.drawString(s, x - f.stringWidth(s) / 2, y - f.getHeight() / 2);
        }

        private void point(int px, int py) {
            int r = radius();
            if (r <= 0) {
                return;
            }
            int dx = px - getAbsoluteX() - getWidth() / 2;
            int dy = py - getAbsoluteY() - getHeight() / 2;
            if (dx == 0 && dy == 0) {
                return;
            }
            double deg = Math.toDegrees(MathUtil.atan2(dx, -dy));
            if (deg < 0) {
                deg += 360;
            }
            if (minutesShown) {
                // Round the face: past the last stop is the hour again, where
                // a time that is set stays at the last stop of its hour.
                int m = (int) Math.round(deg / 6) % 60;
                m = (m + minuteStep / 2) / minuteStep * minuteStep % 60;
                if (m != minute) {
                    minute = m;
                    refresh();
                    changed();
                }
                return;
            }
            int i = (int) Math.round(deg / 30) % 12;
            int h;
            if (meridiem) {
                h = i + (hour >= 12 ? 12 : 0);
            } else {
                int k = knob(r);
                int outer = r - k - Math.max(1, r / 28);
                int inner = outer - 2 * k + k / 5;
                boolean inside = dx * dx + dy * dy < (outer + inner) * (outer + inner) / 4;
                h = inside ? (i == 0 ? 0 : i + 12) : (i == 0 ? 12 : i);
            }
            pointedHour = isEnabledHour(h);
            if (h != hour && pointedHour) {
                hour = h;
                refresh();
                changed();
            }
        }

        @Override
        public void pointerPressed(int x, int y) {
            point(x, y);
        }

        @Override
        public void pointerDragged(int x, int y) {
            point(x, y);
        }

        @Override
        public void pointerReleased(int x, int y) {
            pointedHour = false;
            point(x, y);
            if (!minutesShown && pointedHour) {
                // The hour is chosen; the minute is what is asked next. An
                // hour that cannot be chosen leaves the question open.
                showMinutes(true);
            }
        }
    }

    /// The point of the face an hour of the day or a minute is at, for a
    /// test to tap.
    int[] pointOf(int value, boolean ofMinutes) {
        int r = dial.radius();
        int k = dial.knob(r);
        int outer = r - k - Math.max(1, r / 28);
        int inner = outer - 2 * k + k / 5;
        int ring = outer;
        double a;
        if (ofMinutes) {
            a = Math.toRadians(value * 6);
        } else {
            a = Math.toRadians(value * 30 % 360);
            if (!meridiem && (value == 0 || value > 12)) {
                ring = inner;
            }
        }
        return new int[]{
            dial.getAbsoluteX() + dial.getWidth() / 2 + (int) Math.round(Math.sin(a) * ring),
            dial.getAbsoluteY() + dial.getHeight() / 2 - (int) Math.round(Math.cos(a) * ring)
        };
    }
}
