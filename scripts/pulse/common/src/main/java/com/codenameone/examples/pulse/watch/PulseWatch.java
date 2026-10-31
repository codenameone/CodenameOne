/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.pulse.watch;

import com.codename1.l10n.SimpleDateFormat;
import com.codename1.security.Hash;
import com.codename1.security.Otp;
import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.wearable.WearableConnection;
import com.codename1.wearable.WearableDataListener;
import com.codename1.wearable.WearableMessage;
import java.util.Date;

/// The watch companion: four pages a member swipes between.
///
/// - **Next** -- the next class booked, how long until it starts, and how
///   full it is.
/// - **Door** -- the rotating door code, so the phone can stay in the bag.
/// - **Week** -- progress to the next level, the weekly streak and eight
///   weeks of visits.
/// - **Coming up** -- the next few classes booked, in order.
///
/// This is the class `codename1.watchMain` names, so the watch build starts
/// here and carries only what it reaches -- not the phone's screens or its
/// REST clients. It talks to no server: everything it shows is what
/// [WatchBridge] replicated from the phone, read when the app starts and
/// again whenever the phone changes it. The code is worked out on the watch
/// every period, the same way the phone's pass does it.
public class PulseWatch extends Lifecycle {
    private static final int LIME = 0xc6f432;
    private static final int CORAL = 0xff6b4a;
    private static final int TRACK = 0x2a2a30;

    private Form form;
    // Made in runApp, not here: the lifecycle is constructed before the
    // display exists, and a component needs its theme.
    private Label title;
    private Label until;
    private Label when;
    private Label where;
    private FillBar fill;
    private Label fillText;
    private Label code;
    private Label codeName;
    private Countdown left;
    private LevelRing ring;
    private Label levelLine;
    private Label streakLine;
    private WeekBars weeks;
    private Container agenda;
    private PageDots dots;
    private long startsAt;
    private byte[] secret;
    private int period = 30;
    private int digits = 8;

    @Override
    public void runApp() {
        form = new Form("Pulse", new BorderLayout());
        form.setUIID("SvWatchForm");
        form.getContentPane().setUIID("SvWatchForm");
        // A watch face has no room for a title bar; the eyebrows say it all.
        form.getToolbar().setHidden(true);
        form.setName("Watch");
        Tabs pages = new Tabs();
        pages.setUIID("SvWatchForm");
        pages.getContentPane().setUIID("SvWatchForm");
        pages.hideTabs();
        pages.setSwipeActivated(true);
        pages.setName("watchPages");
        pages.addTab("Next", nextPage());
        pages.addTab("Door", doorPage());
        pages.addTab("Week", weekPage());
        pages.addTab("Coming up", agendaPage());
        dots = new PageDots(pages.getTabCount());
        pages.addSelectionListener((was, now) -> dots.select(now));
        form.add(BorderLayout.CENTER, pages);
        form.add(BorderLayout.SOUTH, dots);
        WearableConnection.addDataListener(new WearableDataListener() {
            @Override
            public void dataChanged(WearableMessage data) {
                show(data);
            }

            @Override
            public void dataRemoved(String path) {
                show(new WearableMessage(path));
            }
        });
        show(stored(WatchBridge.NEXT));
        show(stored(WatchBridge.PASS));
        show(stored(WatchBridge.STATS));
        show(stored(WatchBridge.AGENDA));
        UITimer.timer(1000, true, form, this::tick);
        form.show();
    }

    private static Container page(String name, String eyebrow) {
        Container page = new Container(BoxLayout.y());
        page.setUIID("SvWatchPage");
        page.setName(name);
        page.setScrollableY(true);
        page.setScrollVisible(false);
        page.add(new Label(eyebrow, "SvWatchEyebrow"));
        return page;
    }

    private Container nextPage() {
        Container page = page("watchNext", "NEXT CLASS");
        title = new Label("", "SvWatchTitle");
        title.setName("watchClass");
        until = new Label("", "SvWatchBig");
        until.setName("watchUntil");
        when = new Label("", "SvWatchText");
        where = new Label("", "SvWatchText");
        fill = new FillBar();
        fillText = new Label("", "SvWatchSmall");
        return page.add(title).add(until).add(when).add(where).add(fill).add(fillText);
    }

    private Container doorPage() {
        Container page = page("watchDoor", "DOOR CODE");
        Container card = new Container(BoxLayout.y());
        card.setUIID("SvWatchCard");
        code = new Label("", "SvWatchCode");
        code.setName("watchCode");
        left = new Countdown();
        card.add(code).add(left);
        codeName = new Label("", "SvWatchSmallCenter");
        return page.add(card).add(codeName)
                .add(new Label("Hold to the reader", "SvWatchSmallCenter"));
    }

    private Container weekPage() {
        Container page = page("watchWeek", "YOUR WEEK");
        ring = new LevelRing();
        levelLine = new Label("", "SvWatchStat");
        levelLine.setName("watchStats");
        streakLine = new Label("", "SvWatchStreak");
        weeks = new WeekBars();
        // No caption under the bars: it fell off a round face, and the streak line
        // above already says they are weeks.
        return page.add(ring).add(levelLine).add(streakLine).add(weeks);
    }

    private Container agendaPage() {
        Container page = page("watchAgenda", "COMING UP");
        agenda = new Container(BoxLayout.y());
        return page.add(agenda);
    }

    private static WearableMessage stored(String path) {
        WearableMessage found = WearableConnection.getData(path);
        return found == null ? new WearableMessage(path) : found;
    }

    void show(WearableMessage data) {
        String path = data.getPath();
        if (WatchBridge.NEXT.equals(path)) {
            showNext(data);
        } else if (WatchBridge.STATS.equals(path)) {
            showStats(data);
        } else if (WatchBridge.AGENDA.equals(path)) {
            showAgenda(data);
        } else if (WatchBridge.PASS.equals(path)) {
            secret = hex(data.getString("secret", ""));
            period = Math.max(1, data.getInt("period", 30));
            digits = data.getInt("digits", 8);
            codeName.setText(data.getString("name", ""));
            tick();
        }
        if (form != null) {
            form.revalidate();
        }
    }

    private void showNext(WearableMessage data) {
        startsAt = data.getLong("startsAt", 0L);
        if (startsAt == 0L) {
            title.setText("No class booked");
            when.setText("Book one on your phone");
            where.setText("");
            fill.set(-1f, 0);
            fillText.setText("");
            until.setText("");
            return;
        }
        title.setText(data.getString("name", ""));
        when.setText(new SimpleDateFormat("EEE HH:mm").format(new Date(startsAt)));
        where.setText(room(data.getString("room", "")) + " \u00b7 "
                + data.getString("trainer", ""));
        int capacity = data.getInt("capacity", 0);
        int booked = data.getInt("booked", 0);
        fill.set(capacity <= 0 ? -1f : Math.min(1f, booked / (float) capacity),
                parse(data.getString("color", ""), LIME));
        fillText.setText(capacity <= 0 ? "" : booked + " of " + capacity + " places taken");
        tickUntil();
    }

    private void showStats(WearableMessage data) {
        int points = data.getInt("points", 0);
        int floor = data.getInt("floor", 0);
        int next = data.getInt("next", 0);
        int streak = data.getInt("streak", 0);
        ring.set(points, next > floor ? (points - floor) / (float) (next - floor) : 1f,
                data.getInt("level", 1));
        levelLine.setText(next > points ? (next - points) + " pts to Level "
                + (data.getInt("level", 1) + 1) : "Top level");
        streakLine.setText(streak > 0 ? streak + " week streak" : "Start a streak this week");
        weeks.set(ints(data.getString("weeks", "")));
    }

    private void showAgenda(WearableMessage data) {
        agenda.removeAll();
        int count = data.getInt("count", 0);
        SimpleDateFormat day = new SimpleDateFormat("EEE HH:mm");
        for (int iter = 0; iter < count; iter++) {
            Container row = new Container(BoxLayout.y());
            row.setUIID("SvWatchRow");
            row.setName("watchAgenda" + iter);
            Label at = new Label(day.format(new Date(data.getLong("at" + iter, 0L))),
                    "SvWatchEyebrowMuted");
            Label name = new Label(data.getString("name" + iter, ""), "SvWatchRowTitle");
            Label room = new Label(room(data.getString("room" + iter, "")), "SvWatchSmall");
            row.add(at).add(name).add(room);
            agenda.add(row);
        }
        if (count == 0) {
            agenda.add(new Label("Nothing booked", "SvWatchText"));
        }
    }

    /// The phone sends "location, room"; the room is what a member walks to,
    /// and the wrist has no room for both.
    private static String room(String text) {
        int comma = text.lastIndexOf(", ");
        return comma < 0 ? text : text.substring(comma + 2);
    }

    private void tick() {
        tickUntil();
        if (code == null) {
            return;
        }
        if (secret == null || secret.length == 0) {
            code.setText("Open Pulse on your phone");
            return;
        }
        long seconds = System.currentTimeMillis() / 1000L;
        long counter = seconds / period;
        left.set(1f - (float) (seconds % period) / period);
        String now = Otp.hotp(secret, counter, digits <= 0 ? 8 : digits, Hash.SHA256);
        if (now.length() > 5) {
            // Grouped in two, as a door keypad code is read out.
            now = now.substring(0, now.length() / 2) + " " + now.substring(now.length() / 2);
        }
        if (!now.equals(code.getText())) {
            code.setText(now);
            code.repaint();
        }
    }

    /// "in 2h 15m", counting down to the class; "Now" once it started. A day or more
    /// out it rounds to days: the date line under it already says when.
    private void tickUntil() {
        if (until == null || startsAt == 0L) {
            return;
        }
        long minutes = (startsAt - System.currentTimeMillis()) / 60000L;
        String text;
        if (minutes <= 0) {
            text = "Now";
        } else if (minutes < 60) {
            text = "in " + minutes + " min";
        } else if (minutes < 24 * 60) {
            text = "in " + minutes / 60 + "h " + minutes % 60 + "m";
        } else {
            long days = (minutes + 12 * 60) / (24 * 60);
            text = days == 1 ? "in 1 day" : "in " + days + " days";
        }
        if (!text.equals(until.getText())) {
            until.setText(text);
        }
    }

    private static int[] ints(String csv) {
        if (csv == null || csv.length() == 0) {
            return new int[0];
        }
        int count = 1;
        for (int iter = 0; iter < csv.length(); iter++) {
            if (csv.charAt(iter) == ',') {
                count++;
            }
        }
        int[] out = new int[count];
        int index = 0;
        int value = 0;
        for (int iter = 0; iter < csv.length(); iter++) {
            char c = csv.charAt(iter);
            if (c == ',') {
                out[index++] = value;
                value = 0;
            } else if (c >= '0' && c <= '9') {
                value = value * 10 + c - '0';
            }
        }
        out[index] = value;
        return out;
    }

    /// `#RRGGBB` to a colour, or `fallback` when it is not one.
    static int parse(String color, int fallback) {
        if (color == null || color.length() != 7 || color.charAt(0) != '#') {
            return fallback;
        }
        int out = 0;
        for (int iter = 1; iter < 7; iter++) {
            out = (out << 4) | nibble(color.charAt(iter));
        }
        return out;
    }

    /// How full the next class is, in the class's own colour.
    static final class FillBar extends Component {
        private float fraction = -1f;
        private int color = LIME;

        FillBar() {
            setUIID("SvWatchBar");
        }

        void set(float fraction, int color) {
            this.fraction = fraction;
            this.color = color;
            setVisible(fraction >= 0f);
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            if (fraction < 0f) {
                return;
            }
            int h = getHeight() - getStyle().getVerticalPadding();
            int y = getY() + getStyle().getPaddingTop();
            int x = getX() + getStyle().getPaddingLeftNoRTL();
            int w = getWidth() - getStyle().getHorizontalPadding();
            g.setAntiAliased(true);
            g.setColor(TRACK);
            g.fillRoundRect(x, y, w, h, h, h);
            g.setColor(color);
            g.fillRoundRect(x, y, Math.max(h, (int) (w * fraction)), h, h, h);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(20),
                    CN.convertToPixels(1.4f) + getStyle().getVerticalPadding());
        }
    }

    /// The points ring: the arc is the way from this level to the next, the
    /// points in the middle.
    static final class LevelRing extends Component {
        private int points;
        private float fraction;
        private int level = 1;

        LevelRing() {
            setUIID("SvWatchRing");
        }

        void set(int points, float fraction, int level) {
            this.points = points;
            this.fraction = Math.max(0f, Math.min(1f, fraction));
            this.level = level;
            setAccessibilityText(points + " points, level " + level);
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            int size = Math.min(getWidth() - getStyle().getHorizontalPadding(),
                    getHeight() - getStyle().getVerticalPadding());
            int x = getX() + (getWidth() - size) / 2;
            int y = getY() + getStyle().getPaddingTop();
            int stroke = Math.max(4, size / 9);
            g.setAntiAliased(true);
            g.setColor(TRACK);
            g.fillArc(x, y, size, size, 0, 360);
            g.setColor(LIME);
            g.fillArc(x, y, size, size, 90, -(int) (360 * fraction));
            g.setColor(0x000000);
            g.fillArc(x + stroke, y + stroke, size - 2 * stroke, size - 2 * stroke, 0, 360);
            Font big = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
            Font small = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,
                    Font.SIZE_SMALL);
            String value = String.valueOf(points);
            String caption = "LEVEL " + level;
            int block = big.getHeight() + small.getHeight();
            int top = y + (size - block) / 2;
            g.setColor(0xffffff);
            g.setFont(big);
            g.drawString(value, x + (size - big.stringWidth(value)) / 2, top);
            g.setColor(LIME);
            g.setFont(small);
            g.drawString(caption, x + (size - small.stringWidth(caption)) / 2,
                    top + big.getHeight());
        }

        @Override
        protected Dimension calcPreferredSize() {
            int size = CN.convertToPixels(12);
            return new Dimension(size + getStyle().getHorizontalPadding(),
                    size + getStyle().getVerticalPadding());
        }
    }

    /// Eight weeks of visits as small bars, this week in lime.
    static final class WeekBars extends Component {
        private int[] values = new int[0];

        WeekBars() {
            setUIID("SvWatchBars");
        }

        void set(int[] values) {
            this.values = values;
            StringBuilder said = new StringBuilder("Visits by week:");
            for (int iter = 0; iter < values.length; iter++) {
                said.append(' ').append(values[iter]);
            }
            setAccessibilityText(said.toString());
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            int n = values.length;
            if (n == 0) {
                return;
            }
            int most = 1;
            for (int iter = 0; iter < n; iter++) {
                most = Math.max(most, values[iter]);
            }
            int h = getHeight() - getStyle().getVerticalPadding();
            int y = getY() + getStyle().getPaddingTop();
            int x = getX() + getStyle().getPaddingLeftNoRTL();
            int w = getWidth() - getStyle().getHorizontalPadding();
            int gap = Math.max(2, w / (n * 5));
            int bar = (w - gap * (n - 1)) / n;
            g.setAntiAliased(true);
            for (int iter = 0; iter < n; iter++) {
                int bx = x + iter * (bar + gap);
                int bh = Math.max(bar / 2, h * values[iter] / most);
                g.setColor(values[iter] == 0 ? TRACK : iter == n - 1 ? LIME : 0x6b7f2a);
                g.fillRoundRect(bx, y + h - bh, bar, bh, bar / 2, bar / 2);
            }
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(24),
                    CN.convertToPixels(6) + getStyle().getVerticalPadding());
        }
    }

    /// Which of the pages is showing.
    static final class PageDots extends Component {
        private final int count;
        private int selected;

        PageDots(int count) {
            this.count = count;
            setUIID("SvWatchDots");
        }

        void select(int index) {
            selected = index;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            int d = CN.convertToPixels(1f);
            int gap = d;
            int total = count * d + (count - 1) * gap + d;
            int x = getX() + (getWidth() - total) / 2;
            int y = getY() + (getHeight() - d) / 2;
            g.setAntiAliased(true);
            for (int iter = 0; iter < count; iter++) {
                boolean on = iter == selected;
                g.setColor(on ? 0xffffff : 0x55555c);
                int w = on ? 2 * d : d;
                g.fillRoundRect(x, y, w, d, d, d);
                x += w + gap;
            }
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(10), CN.convertToPixels(2.4f));
        }
    }

    /// A thin bar that empties as the code's period runs out.
    static final class Countdown extends Component {
        private float fraction = 1f;

        Countdown() {
            setUIID("SvWatchBar");
        }

        void set(float fraction) {
            if (fraction != this.fraction) {
                this.fraction = fraction;
                repaint();
            }
        }

        @Override
        public void paint(Graphics g) {
            int h = getHeight() - getStyle().getVerticalPadding();
            int y = getY() + getStyle().getPaddingTop();
            int x = getX() + getStyle().getPaddingLeftNoRTL();
            int w = getWidth() - getStyle().getHorizontalPadding();
            g.setAntiAliased(true);
            g.setColor(TRACK);
            g.fillRoundRect(x, y, w, h, h, h);
            g.setColor(fraction < 0.2f ? CORAL : LIME);
            g.fillRoundRect(x, y, Math.max(h, (int) (w * fraction)), h, h, h);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(20),
                    CN.convertToPixels(1.2f) + getStyle().getVerticalPadding());
        }
    }

    static byte[] hex(String text) {
        if (text == null) {
            return new byte[0];
        }
        byte[] out = new byte[text.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) ((nibble(text.charAt(iter * 2)) << 4)
                    | nibble(text.charAt(iter * 2 + 1)));
        }
        return out;
    }

    private static int nibble(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return 0;
    }
}
