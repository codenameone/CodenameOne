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
import com.codename1.ui.Form;
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
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;
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
/// ![The hours of a Material time picker](https://www.codenameone.com/developer-guide/img/generated/picker-material-time.png)
/// ![The minutes](https://www.codenameone.com/developer-guide/img/generated/picker-material-time-minutes.png)
/// ![Twenty four hours](https://www.codenameone.com/developer-guide/img/generated/picker-material-time-24h.png)
///
/// Under a mouse the number the pointer is over is marked. The hand follows
/// a finger as it is dragged, and turns by the shorter way round to a time
/// that is set any other way: the minutes after an hour is chosen, a time
/// typed in, a call to `setTime`.
///
/// ![The hand turning from ten to two, as four stopped frames](https://www.codenameone.com/developer-guide/img/generated/picker-material-hand-stages.png)
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
    // Two rows of one height each, so AM and PM together are as tall as
    // the hour beside them whatever a theme pads them with.
    private final Container periods = new Container(new GridLayout(2, 1));
    private final Container time = new Container();
    private final Colon separator = new Colon();
    private final Label hourCaption = new Label("", "PickerMaterialOverline");
    private final Label minuteCaption = new Label("", "PickerMaterialOverline");
    private final Dial dial = new Dial();
    private final Button modeButton = new Button("", "PickerMaterialButton");
    private final Container modeRow = new Container(new FlowLayout(Component.LEFT));
    private final EventDispatcher listeners = new EventDispatcher();

    /// A picker showing midnight on a 24 hour face.
    public MaterialTimePicker() {
        super(new BorderLayout());
        setUIID("PickerMaterial");
        overline.setText(getUIManager().localize("picker.selectTime", "Select time"));
        hourCaption.setText(getUIManager().localize("picker.hour", "Hour"));
        minuteCaption.setText(getUIManager().localize("picker.minute", "Minute"));
        time.setLayout(new TimeRow());
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
        // Two digits in a box made for them: there is nothing to scroll to,
        // and the bar of a text area was drawn down the side of the box.
        f.setScrollVisible(false);
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
        time.add(separator);
        if (inputMode) {
            time.add(minuteInput);
        } else {
            time.add(minuteField);
        }
        if (meridiem) {
            time.add(periods);
        }
        if (inputMode) {
            time.add(hourCaption);
            time.add(minuteCaption);
        }
    }

    // The row of the time: the hour and the minute in two boxes of one
    // size, the colon between them and AM and PM beside them, all of one
    // height and centred as a group.
    //
    // A flow of the four gave each its own preferred size. The hour was
    // narrower than the minute whenever it had one digit, the two periods
    // were as tall as their paddings added up to, and the colon sat
    // wherever the font put it. Here the box of the hour sets the size and
    // everything else is measured against it.
    private final class TimeRow extends Layout {
        private int boxWidth() {
            return Math.max(hourField.getPreferredW(), minuteField.getPreferredW());
        }

        private int boxHeight() {
            return Math.max(hourField.getPreferredH(), minuteField.getPreferredH());
        }

        private int captionGap() {
            return Display.getInstance().convertToPixels(0.7f);
        }

        private int periodsWidth() {
            return meridiem ? periods.getPreferredW() + Display.getInstance().convertToPixels(2f) : 0;
        }

        @Override
        public void layoutContainer(Container parent) {
            Style s = parent.getStyle();
            int fw = boxWidth();
            int fh = boxHeight();
            int sw = separator.getPreferredW();
            int pw = periodsWidth();
            int room = parent.getWidth() - s.getHorizontalPadding();
            int x = s.getPaddingLeftNoRTL() + Math.max(0, (room - 2 * fw - sw - pw) / 2);
            int y = s.getPaddingTop();
            if (inputMode) {
                padTyped(fw, fh);
            }
            place(inputMode ? (Component) hourInput : hourField, x, y, fw, fh);
            place(separator, x + fw, y, sw, fh);
            place(inputMode ? (Component) minuteInput : minuteField, x + fw + sw, y, fw, fh);
            if (meridiem) {
                int gap = pw - periods.getPreferredW();
                // The outer edges of the two are the edges of the row: what
                // a theme puts around each as a margin stays between them.
                int over = am.getStyle().getMarginTop();
                int under = pm.getStyle().getMarginBottom();
                place(periods, x + 2 * fw + sw + gap, y - over, pw - gap, fh + over + under);
            }
            if (inputMode) {
                int cy = y + fh + captionGap();
                place(hourCaption, x, cy, fw, hourCaption.getPreferredH());
                place(minuteCaption, x + fw + sw, cy, fw, minuteCaption.getPreferredH());
            }
        }

        private void place(Component c, int x, int y, int w, int h) {
            c.setX(x);
            c.setY(y);
            c.setWidth(w);
            c.setHeight(h);
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            Style s = parent.getStyle();
            int h = boxHeight();
            if (inputMode) {
                h += captionGap() + Math.max(hourCaption.getPreferredH(), minuteCaption.getPreferredH());
            }
            return new Dimension(2 * boxWidth() + separator.getPreferredW() + periodsWidth()
                    + s.getHorizontalPadding(), h + s.getVerticalPadding());
        }
    }

    // A typed field is the box of the hour with a caret in it: the same
    // type, and two digits in the middle of it. A text field draws its text
    // from its padding and has no centred alignment, so the padding is what
    // puts the digits there.
    private void padTyped(int fw, int fh) {
        Font f = getUIManager().getComponentStyle("PickerMaterialTimeField").getFont();
        int px = Math.max(0, (fw - f.stringWidth("00")) / 2);
        int py = Math.max(0, (fh - f.getHeight()) / 2);
        padTyped(hourInput, f, px, py);
        padTyped(minuteInput, f, px, py);
    }

    // Asked of the style every time and written only when it differs: a
    // theme that is refreshed gives the field a new style, and a number
    // remembered here would say the padding was still on it.
    private static void padTyped(TextField t, Font f, int px, int py) {
        Style now = t.getUnselectedStyle();
        if (now.getFont() == f && now.getPaddingLeftNoRTL() == px && now.getPaddingTop() == py) { //NOPMD CompareObjectsWithEquals - the very font
            return;
        }
        Style all = t.getAllStyles();
        all.setFont(f);
        all.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        all.setPadding(py, py, px, px);
        all.setMarginUnit(Style.UNIT_TYPE_PIXELS);
        all.setMargin(0, 0, 0, 0);
    }

    // The colon between the hour and the minute: two dots about the middle
    // of the row. The glyph of a font sits between the baseline and the
    // height of a lower case letter, which is under the middle of the
    // digits beside it.
    private static final class Colon extends Component {
        Colon() {
            setUIID("PickerMaterialTimeSeparator");
            setFocusable(false);
        }

        @Override
        protected Dimension calcPreferredSize() {
            Style s = getStyle();
            int h = s.getFont().getHeight();
            return new Dimension(Math.max(h / 3, Display.getInstance().convertToPixels(3f)) + s.getHorizontalPadding(),
                    h);
        }

        @Override
        public void paint(Graphics g) {
            Style s = getStyle();
            int d = Math.max(3, s.getFont().getHeight() / 7);
            int cx = getX() + getWidth() / 2;
            int cy = getY() + getHeight() / 2;
            int off = d * 2;
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setColor(s.getFgColor());
            g.fillArc(cx - d / 2, cy - off - d / 2, d, d, 0, 360);
            g.fillArc(cx - d / 2, cy + off - d / 2, d, d, 0, 360);
            g.setAntiAliased(aa);
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
        // The fields are written again with the time that is set.
        typedWrong = false;
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

    /// Where the hand points now, in degrees clockwise from twelve.
    float handAt() {
        return dial.handAt();
    }

    /// The number of the face the pointer is over, or -1.
    int overNumber() {
        return dial.overIndex;
    }

    Container timeRow() {
        return time;
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
            // Two digits, as the minute has: the hour is typed over, and a
            // box with one digit off its middle reads as a broken one.
            int h = meridiem ? (hour % 12 == 0 ? 12 : hour % 12) : hour;
            hourInput.setText(two(h));
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

    private static final int SWEEP_MS = 220;
    private long fixedClock = -1;

    /// Stops the clock the hand turns by at a time, for a screenshot of
    /// the turn or a test of it. A negative value returns to the clock.
    public void setAnimationTime(long millis) {
        // What changed before this is a turn that starts now.
        dial.sync();
        fixedClock = millis;
        dial.sync();
        dial.repaint();
    }

    private long clock() {
        return fixedClock >= 0 ? fixedClock : System.currentTimeMillis();
    }

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

        // The ring of the hours after noon, inside the ring at `outer`.
        // Half way to the centre, as on the platform's own dial: any
        // closer in and twelve two-digit numbers run into one another.
        private int innerRing(int outer, int k) {
            return outer - k * 3 / 2;
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
            int inner = innerRing(outer, k);
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setColor(face.getBgColor());
            g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 0, 360);

            // The hand first, then the numbers over it.
            sync();
            float p = sweep();
            double a = Math.toRadians(fromAt + turn(fromAt, toAt) * p);
            float inward = (fromIn ? 1 : 0) + ((toIn ? 1 : 0) - (fromIn ? 1 : 0)) * p;
            int ring = outer - Math.round((outer - inner) * inward);
            int hx = cx + (int) Math.round(Math.sin(a) * ring);
            int hy = cy - (int) Math.round(Math.cos(a) * ring);
            if (overIndex >= 0) {
                // Where a press would put the hand.
                double oa = Math.toRadians(overIndex * 30);
                int or = overIn ? inner : outer;
                int alpha0 = g.getAlpha();
                g.setColor(face.getFgColor());
                g.setAlpha(alpha0 * 28 / 255);
                g.fillArc(cx + (int) Math.round(Math.sin(oa) * or) - k, cy - (int) Math.round(Math.cos(oa) * or) - k,
                        2 * k, 2 * k, 0, 360);
                g.setAlpha(alpha0);
            }
            g.setColor(hand.getBgColor());
            g.drawLine(cx, cy, hx, hy);
            g.drawLine(cx + 1, cy, hx + 1, hy);
            g.fillArc(cx - k / 4, cy - k / 4, k / 2, k / 2, 0, 360);
            g.fillArc(hx - k, hy - k, 2 * k, 2 * k, 0, 360);
            // A number is written in the hand's colour while the hand is
            // on it, which during a sweep is not the number that is set:
            // that one would be white on the pale face until the hand
            // arrived.
            knobX = hx;
            knobY = hy;
            knobNear = k * k * 2 / 5;
            litColor = hand.getFgColor();
            plainColor = face.getFgColor();

            Font f = face.getFont();
            // The inner ring is shorter round, and its numbers smaller.
            Font small = meridiem || minutesShown ? f : innerFont(f);
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
                    number(g, small, two(evening), cx, cy, inner, i,
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

        private int knobX;
        private int knobY;
        private int knobNear;
        private int litColor;
        private int plainColor;
        private Font innerOf;
        private Font innerFont;

        private Font innerFont(Font f) {
            if (innerOf != f) { //NOPMD CompareObjectsWithEquals - the font the cached one was made from
                innerOf = f;
                innerFont = f.isTTFNativeFont() ? f.derive(f.getPixelSize() * 0.85f, f.getStyle()) : f;
            }
            return innerFont;
        }

        // ---- the hand turns to a time it was not dragged to ----

        private boolean aimed;
        private int aimedAt;
        private boolean held;
        private float fromAt;
        private float toAt;
        private boolean fromIn;
        private boolean toIn;
        private long sweepSince = -1;
        private boolean registered;
        private int overIndex = -1;
        private boolean overIn;

        // The shorter way round from one angle to another.
        private float turn(float from, float to) {
            float d = (to - from) % 360;
            if (d > 180) {
                d -= 360;
            } else if (d < -180) {
                d += 360;
            }
            return d;
        }

        // Points the hand at the time the picker has now.
        void sync() {
            if (minutesShown) {
                aim(minute * 6, false);
            } else {
                aim(hour * 30 % 360, !meridiem && (hour == 0 || hour > 12));
            }
        }

        float handAt() {
            sync();
            float a = (fromAt + turn(fromAt, toAt) * sweep()) % 360;
            return a < 0 ? a + 360 : a;
        }

        // How far the hand has come, from 0 to 1.
        private float sweep() {
            if (sweepSince < 0) {
                return 1;
            }
            long t = clock() - sweepSince;
            if (t >= SWEEP_MS) {
                return 1;
            }
            float p = t <= 0 ? 0 : t / (float) SWEEP_MS;
            return p * p * (3 - 2 * p);
        }

        // The time the face shows has changed. Under a finger the hand is
        // where the finger is; set any other way it turns there: the hour
        // was chosen and the minutes are asked, a field was typed in, the
        // half of the day was changed.
        private void aim(int at, boolean in) {
            if (aimed && at == aimedAt && in == toIn) {
                return;
            }
            if (!aimed || held || !isInitialized()) {
                sweepSince = -1;
            } else {
                float p = sweep();
                fromAt = fromAt + turn(fromAt, toAt) * p;
                // Halfway between the rings it starts from the nearer one.
                fromIn = p < 0.5f ? fromIn : toIn;
                sweepSince = clock();
                Form f = getComponentForm();
                if (f != null && !registered) {
                    f.registerAnimated(this);
                    registered = true;
                }
            }
            if (sweepSince < 0) {
                fromAt = at;
                fromIn = in;
            }
            // The angle as it was given, to compare the next one with: two
            // floats are not compared for equality.
            aimedAt = at;
            toAt = at;
            toIn = in;
            aimed = true;
        }

        @Override
        public boolean animate() {
            boolean busy = sweepSince >= 0 && fixedClock < 0 && clock() - sweepSince < SWEEP_MS;
            if (!busy && registered) {
                Form f = getComponentForm();
                if (f != null) {
                    f.deregisterAnimated(this);
                }
                registered = false;
            }
            return busy;
        }

        @Override
        protected void deinitialize() {
            if (registered) {
                Form f = getComponentForm();
                if (f != null) {
                    f.deregisterAnimated(this);
                }
                registered = false;
            }
            sweepSince = -1;
            held = false;
            overIndex = -1;
            super.deinitialize();
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            if (Display.getInstance().isDesktop()) {
                Form f = getComponentForm();
                if (f != null && !f.isEnableCursors()) {
                    f.setEnableCursors(true);
                }
                setCursor(HAND_CURSOR);
            }
        }

        @Override
        public void pointerHover(int[] x, int[] y) {
            int r = radius();
            if (r <= 0 || x == null || x.length == 0) {
                return;
            }
            int dx = x[0] - getAbsoluteX() - getWidth() / 2;
            int dy = y[0] - getAbsoluteY() - getHeight() / 2;
            int index = -1;
            boolean in = false;
            if (dx * dx + dy * dy <= r * r && (dx != 0 || dy != 0)) {
                double deg = Math.toDegrees(MathUtil.atan2(dx, -dy));
                if (deg < 0) {
                    deg += 360;
                }
                index = (int) Math.round(deg / 30) % 12;
                if (!minutesShown) {
                    int h;
                    if (meridiem) {
                        h = index + (hour >= 12 ? 12 : 0);
                    } else {
                        int k = knob(r);
                        int outer = r - k - Math.max(1, r / 28);
                        int inner = innerRing(outer, k);
                        in = dx * dx + dy * dy < (outer + inner) * (outer + inner) / 4;
                        h = in ? (index == 0 ? 0 : index + 12) : (index == 0 ? 12 : index);
                    }
                    if (!isEnabledHour(h)) {
                        index = -1;
                    }
                }
            }
            if (index != overIndex || in != overIn) {
                overIndex = index;
                overIn = in;
                repaint();
            }
        }

        @Override
        public void setHovered(boolean hovered) {
            super.setHovered(hovered);
            if (!hovered && overIndex >= 0) {
                overIndex = -1;
                repaint();
            }
        }

        private void number(Graphics g, Font f, String s, int cx, int cy, int ring, int i, int color) {
            double a = Math.toRadians(i * 30);
            int x = cx + (int) Math.round(Math.sin(a) * ring);
            int y = cy - (int) Math.round(Math.cos(a) * ring);
            int dx = x - knobX;
            int dy = y - knobY;
            boolean under = dx * dx + dy * dy <= knobNear;
            int shown = color;
            if (under) {
                shown = litColor;
            } else if (color == litColor) {
                shown = plainColor;
            }
            g.setColor(shown);
            g.setFont(f);
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
                int inner = innerRing(outer, k);
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
            held = true;
            overIndex = -1;
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
            // The hand is where the finger left it before anything turns it.
            sync();
            held = false;
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
        int inner = dial.innerRing(outer, k);
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
