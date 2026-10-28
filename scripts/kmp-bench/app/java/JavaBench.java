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
package com.example.javabench;

import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.InfiniteContainer;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.List;

/**
 * The Keynote application written directly against Codename One: the same
 * screens, data and benchmark steps as the Compose source in app/shared, with
 * no Kotlin, no Compose runtime and no coroutines in the binary. run_bench.sh
 * builds it as the third side of the comparison.
 */
public class JavaBench extends Lifecycle {
    static final int SESSIONS_PER_DAY = 50;

    final List<Session> sessions = Programme.generate();
    final List<Integer> favorites = new ArrayList<Integer>();
    int day = 1;
    String query = "";

    Form form;
    Tabs tabs;
    SessionList schedule;
    SessionList favoriteList;
    Insights insights;
    Marker marker;
    Container stepHost;
    Perf perf;

    @Override
    public void runApp() {
        form = new Form("KotlinConf-ish 2026", new BorderLayout());
        form.getToolbar().setTitleCentered(true);
        // Material's top app bar title: 22 sp, where the iOS theme sets a large title.
        form.getToolbar().getTitleComponent().getAllStyles().setFont(font("native:MainRegular", 22));
        tabs = new Tabs(Component.BOTTOM);
        tabs.setSwipeActivated(false);
        tabs.addTab("Schedule", FontImage.MATERIAL_DATE_RANGE, 4, scheduleTab());
        favoriteList = new SessionList(this);
        tabs.addTab("Favorites", FontImage.MATERIAL_FAVORITE, 4, favoriteList);
        insights = new Insights(this);
        tabs.addTab("Insights", FontImage.MATERIAL_INFO, 4, insights);
        tabs.addSelectionListener((old, now) -> {
            form.setTitle(now == 2 ? "Insights" : "KotlinConf-ish 2026");
            if (now == 1) {
                favoriteList.show(favoriteSessions(), "Tap the heart on a session to keep it here.");
            } else if (now == 2) {
                insights.open();
            }
        });
        form.add(BorderLayout.CENTER, tabs);
        form.show();
        // A Form focuses its first focusable component; the search field's
        // blinking caret would then repaint the transparent field, and with it
        // the whole tab, every half second. Compose does not focus it either.
        form.setFocused(null);
        CN.callSerially(() -> System.out.println("KMPBENCH first-frame " + System.currentTimeMillis()));
        if ("1".equals(System.getenv("KMPBENCH_PERF"))) {
            perf = new Perf(this);
            perf.start();
        }
    }

    private Container scheduleTab() {
        TextField search = new TextField("", "Search talks and speakers", 20, TextField.ANY);
        search.setHint("Search talks and speakers", FontImage.createMaterial(FontImage.MATERIAL_SEARCH, search.getHintLabel().getUnselectedStyle()));
        search.getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(5f).strokeColor(0x79747e).strokeOpacity(255).stroke(1, false));
        search.getAllStyles().setBgTransparency(0);
        search.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        search.getAllStyles().setPadding(0.6f, 0.6f, 2.5f, 2.5f);
        search.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
        search.getAllStyles().setMargin(1.3f, 1.3f, 2.5f, 2.5f);
        // Material's outlined field is 56 dp; the iOS theme's field is taller.
        search.setPreferredH(dp(56));
        search.addDataChangedListener((type, index) -> {
            query = search.getText().trim();
            refreshSchedule();
        });
        Container days = new Container(new FlowLayout());
        days.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        days.getAllStyles().setPadding(0, 0, 2.5f, 2.5f);
        for (int d = 1; d <= 3; d++) {
            days.add(new DayChip(this, d));
        }
        schedule = new SessionList(this);
        marker = new Marker();
        Container top = BoxLayout.encloseY(search, days);
        Container tab = new Container(new BorderLayout()) {
            @Override
            public void layoutContainer() {
                if (perf != null) {
                    perf.layoutStep();
                }
                super.layoutContainer();
            }
        };
        stepHost = tab;
        tab.add(BorderLayout.NORTH, top);
        tab.add(BorderLayout.CENTER, schedule);
        tab.add(BorderLayout.SOUTH, marker);
        refreshSchedule();
        return tab;
    }

    void setDay(int d) {
        day = d;
        refreshSchedule();
        schedule.getParent().getComponentAt(0).repaint();
    }

    void refreshSchedule() {
        List<Session> visible = new ArrayList<Session>();
        String q = query.toLowerCase();
        for (Session s : sessions) {
            if (s.day == day && (q.length() == 0 || s.title.toLowerCase().indexOf(q) >= 0
                    || s.speaker.toLowerCase().indexOf(q) >= 0)) {
                visible.add(s);
            }
        }
        schedule.show(visible, "No session matches \"" + query + "\".");
    }

    List<Session> favoriteSessions() {
        List<Session> out = new ArrayList<Session>();
        for (Session s : sessions) {
            if (favorites.contains(Integer.valueOf(s.id))) {
                out.add(s);
            }
        }
        return out;
    }

    void toggleFavorite(Session s) {
        Integer id = Integer.valueOf(s.id);
        if (!favorites.remove(id)) {
            favorites.add(id);
        }
    }

    static int dp(float v) {
        return CN.convertToPixels(v * 0.15875f);
    }

    private static final java.util.Map<String, Font> FONTS = new java.util.HashMap<String, Font>();

    /** One native font per face and size, shared by every card. */
    static Font font(String face, float sp) {
        String key = face + sp;
        Font f = FONTS.get(key);
        if (f == null) {
            f = Font.createTrueTypeFont(face, sp * 0.15875f);
            FONTS.put(key, f);
        }
        return f;
    }

    static boolean dark() {
        Boolean d = CN.isDarkMode();
        return d != null && d.booleanValue();
    }

    // --- data ---------------------------------------------------------------

    static final String[] TRACK_LABELS = {"Language", "Multiplatform", "Server", "Tooling", "UI"};
    static final int[] TRACK_COLORS = {0x7F52FF, 0x0095D5, 0x2E7D32, 0xE65100, 0xC2185B};

    static final class Session {
        int id;
        String title;
        String speaker;
        String company;
        int track;
        int day;
        int start;
        int length;
        String room;
        String description;

        String timeLabel() {
            return clock(start) + " - " + clock(start + length);
        }
    }

    static String clock(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }

    /** The same generator, seed and order as Data.kt, so both sides show the same 150 sessions. */
    static final class Programme {
        static final String[] FIRST = {"Ada", "Linus", "Grace", "Alan", "Barbara", "Ken", "Margaret", "Dennis", "Frances", "Edsger",
            "Radia", "Bjarne", "Sophie", "Guido", "Hedy", "James"};
        static final String[] LAST = {"Lovelace", "Hopper", "Turing", "Liskov", "Thompson", "Hamilton", "Ritchie", "Allen",
            "Dijkstra", "Perlman", "Wilson", "Lamarr", "Gosling", "Kay", "Knuth", "Backus"};
        static final String[] COMPANIES = {"JetBrains", "Google", "Square", "Netflix", "Touchlab", "Cash App", "Shopify", "Zalando"};
        static final String[] TOPICS = {"Coroutines", "Flows", "Compose", "K2", "Ktor", "Gradle", "Serialization", "Wasm",
            "Native interop", "Context parameters", "Value classes", "Multiplatform libraries", "Testing", "Performance",
            "Accessibility", "Animations", "State hoisting", "Server-side Kotlin", "Build speed", "Code sharing"};
        static final String[] SHAPES = {"Deep dive into %s", "%s in production", "What's new in %s", "%s: lessons learned",
            "Mastering %s", "%s from scratch", "The hidden cost of %s", "%s at scale"};
        static final String[] ROOMS = {"Auditorium", "Room 1", "Room 2", "Workshop A", "Lightning stage"};

        private static long seed;

        private static int next(int n) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            return (int) ((seed >>> 33) % n);
        }

        static List<Session> generate() {
            seed = 20261025L;
            List<Session> out = new ArrayList<Session>();
            int id = 0;
            for (int day = 1; day <= 3; day++) {
                int start = 9 * 60;
                int count = 0;
                while (count < SESSIONS_PER_DAY) {
                    for (String room : ROOMS) {
                        if (count >= SESSIONS_PER_DAY) {
                            break;
                        }
                        String topic = TOPICS[next(TOPICS.length)];
                        String title = replace(SHAPES[next(SHAPES.length)], topic);
                        String speaker = FIRST[next(FIRST.length)] + " " + LAST[next(LAST.length)];
                        String company = COMPANIES[next(COMPANIES.length)];
                        Session s = new Session();
                        s.id = id++;
                        s.title = title;
                        s.speaker = speaker;
                        s.company = company;
                        s.track = next(TRACK_LABELS.length);
                        s.day = day;
                        s.start = start;
                        s.length = "Lightning stage".equals(room) ? 15 : room.startsWith("Workshop") ? 90 : 45;
                        s.room = room;
                        s.description = title + ". " + speaker + " of " + company + " walks through " + topic
                            + ": the model behind it, the mistakes teams make with it, and the patterns that hold up in real applications. "
                            + "Bring questions; the last ten minutes are open Q&A.";
                        out.add(s);
                        count++;
                    }
                    start += 60;
                }
            }
            return out;
        }

        private static String replace(String shape, String topic) {
            int i = shape.indexOf("%s");
            return shape.substring(0, i) + topic + shape.substring(i + 2);
        }
    }

    // --- components ---------------------------------------------------------

    /** A Material filter chip: outlined, filled when selected. */
    static final class DayChip extends Button {
        private final JavaBench app;
        private final int day;

        DayChip(JavaBench app, int day) {
            super("Day " + day);
            this.app = app;
            this.day = day;
            Style s = getAllStyles();
            s.setFont(font("native:MainRegular", 14));
            s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
            s.setPadding(1, 1, 2.5f, 2.5f);
            s.setMarginUnit(Style.UNIT_TYPE_DIPS);
            s.setMargin(0, 0, 0, 1.3f);
            s.setBorder(Border.createEmpty());
            s.setBgTransparency(0);
            s.setFgColor(dark() ? 0xE6E1E5 : 0x1D1B20);
            addActionListener(e -> app.setDay(day));
        }

        @Override
        public void paintBackground(Graphics g) {
            boolean on = app.day == day;
            int r = dp(8);
            g.setAntiAliased(true);
            if (on) {
                g.setColor(dark() ? 0x3A2D6B : 0xE9E1FF);
                g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), r * 2, r * 2);
            } else {
                g.setColor(0x79747E);
                g.drawRoundRect(getX(), getY(), getWidth() - 1, getHeight() - 1, r * 2, r * 2);
            }
        }
    }

    /**
     * The scrolling list of cards: an InfiniteContainer, so cards are made a
     * batch at a time as the scroll nears the end, as LazyColumn composes only
     * what comes into view. A card, once made, is kept in a pool and rebound
     * when the day or the query changes, as a lazy list reuses its item nodes.
     */
    static final class SessionList extends InfiniteContainer {
        private static final int BATCH = 8;
        private final JavaBench app;
        private final List<Card> pool = new ArrayList<Card>();
        private final Label empty = new Label();
        private List<Session> items = new ArrayList<Session>();

        SessionList(JavaBench app) {
            super(BATCH);
            this.app = app;
            getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
            getAllStyles().setPadding(1.3f, 1.3f, 2.5f, 2.5f);
            empty.getAllStyles().setAlignment(Component.CENTER);
            empty.getAllStyles().setFgColor(0x49454F);
        }

        @Override
        public Component[] fetchComponents(int index, int amount) {
            if (items.isEmpty()) {
                return index == 0 ? new Component[] {empty} : new Component[0];
            }
            int n = Math.max(0, Math.min(amount, items.size() - index));
            Component[] out = new Component[n];
            for (int i = 0; i < n; i++) {
                while (pool.size() <= index + i) {
                    pool.add(new Card(app));
                }
                Card c = pool.get(index + i);
                c.bind(items.get(index + i));
                out[i] = c;
            }
            return out;
        }

        void show(List<Session> list, String emptyText) {
            items = list;
            empty.setText(emptyText);
            refresh();
        }

        /** The list's scrollToItem: card i at the top, made first if needed. */
        void scrollToCard(int i) {
            int y = 0;
            if (i < pool.size() && pool.get(i).getParent() == this) {
                y = pool.get(i).getY() - getStyle().getPaddingTop();
            } else {
                y = Integer.MAX_VALUE;
            }
            int max = Math.max(0, getScrollDimension().getHeight() - getHeight());
            setScrollY(Math.max(0, Math.min(y, max)));
        }
    }

    /** One session: track stripe, time and room, title, speaker, favorite heart. */
    static final class Card extends Container {
        private final JavaBench app;
        private final Label time = new Label();
        private final Label title = new Label();
        private final Label speaker = new Label();
        private final Button heart = new Button();
        private Session session;
        private boolean shownFavorite;

        Card(JavaBench app) {
            super(new BorderLayout());
            this.app = app;
            Style s = getAllStyles();
            s.setBgTransparency(0);
            s.setBorder(Border.createEmpty());
            s.setMarginUnit(Style.UNIT_TYPE_DIPS);
            s.setMargin(0, 1.9f, 0, 0);
            s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
            s.setPadding(1.9f, 1.9f, 2.9f, 0.6f);
            style(time, "native:MainRegular", 12, 0x49454F);
            style(title, "native:MainRegular", 16, 0x1D1B20);
            style(speaker, "native:MainRegular", 12, 0x49454F);
            title.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
            title.getAllStyles().setMargin(0.6f, 0.3f, 0, 0);
            heart.getAllStyles().setBgTransparency(0);
            heart.getAllStyles().setBorder(Border.createEmpty());
            heart.addActionListener(e -> {
                app.toggleFavorite(session);
                updateHeart();
            });
            add(BorderLayout.CENTER, BoxLayout.encloseY(time, title, speaker));
            add(BorderLayout.EAST, BorderLayout.centerAbsolute(heart));
            addPointerReleasedListener(e -> {
                if (session != null && !heart.contains(e.getX(), e.getY())) {
                    new Detail(app, session).show();
                }
            });
        }

        private static void style(Label l, String face, float sp, int color) {
            Style s = l.getAllStyles();
            s.setFont(font(face, sp));
            s.setFgColor(color);
            s.setPadding(0, 0, 0, 0);
            s.setMargin(0, 0, 0, 0);
            s.setBgTransparency(0);
            l.setEndsWith3Points(true);
        }

        void bind(Session s) {
            session = s;
            time.setText(s.timeLabel() + "  -  " + s.room);
            title.setText(s.title);
            speaker.setText(s.speaker + ", " + s.company);
            updateHeart();
        }

        private void updateHeart() {
            boolean fav = app.favorites.contains(Integer.valueOf(session.id));
            Style s = heart.getAllStyles();
            s.setFgColor(fav ? 0xE91E63 : 0x49454F);
            if (fav != shownFavorite || heart.getIcon() == null) {
                shownFavorite = fav;
                FontImage.setMaterialIcon(heart, fav ? FontImage.MATERIAL_FAVORITE : FontImage.MATERIAL_FAVORITE_BORDER, 3.8f);
            }
        }

        @Override
        public void paintBackground(Graphics g) {
            // The rounded card in the track colour, then the card colour over
            // all of it but a 6 dp stripe, whose inner edge stays straight.
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int r = dp(12);
            int stripe = dp(6);
            g.setAntiAliased(true);
            g.setColor(TRACK_COLORS[session == null ? 0 : session.track]);
            g.fillRoundRect(x, y, w, h, r * 2, r * 2);
            g.setColor(dark() ? 0x1D1B20 : 0xF7F2FA);
            g.fillRect(x + stripe, y, w - stripe - r, h);
            g.fillRoundRect(x + w - r * 2, y, r * 2, h, r * 2, r * 2);
        }
    }

    /** Drawn after the list in every frame; the benchmark times a step until it paints. */
    static final class Marker extends Component {
        int seq;
        int drawnSeq;
        long drawnAt;

        @Override
        public void paint(Graphics g) {
            drawnAt = System.nanoTime();
            drawnSeq = seq;
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(1, 1);
        }
    }

    /** Sessions per track for the selected day, as bars that grow in. */
    static final class Insights extends Container {
        private final JavaBench app;
        private final Label caption = new Label();
        private final Container legend = new Container(BoxLayout.y());
        private final Chart chart = new Chart();

        Insights(JavaBench app) {
            super(BoxLayout.y());
            this.app = app;
            getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
            getAllStyles().setPadding(3.2f, 3.2f, 3.2f, 3.2f);
            caption.getAllStyles().setFont(font("native:MainBold", 16));
            add(caption);
            add(chart);
            add(legend);
        }

        void open() {
            int[] counts = new int[TRACK_LABELS.length];
            for (Session s : app.sessions) {
                if (s.day == app.day) {
                    counts[s.track]++;
                }
            }
            caption.setText("Sessions per track, day " + app.day);
            legend.removeAll();
            for (int t = 0; t < counts.length; t++) {
                Label name = new Label(TRACK_LABELS[t]);
                Label n = new Label(String.valueOf(counts[t]));
                n.getAllStyles().setFont(font("native:MainBold", 14));
                legend.add(BorderLayout.centerEastWest(name, n, null));
            }
            if (!app.favorites.isEmpty()) {
                Label fav = new Label("You have " + app.favorites.size() + " favorite sessions.");
                fav.getAllStyles().setFgColor(0x7F52FF);
                legend.add(fav);
            }
            chart.start(counts);
            revalidate();
        }
    }

    static final class Chart extends Component implements Animation {
        private int[] counts = new int[0];
        private Motion grow;

        void start(int[] c) {
            counts = c;
            grow = Motion.createEaseInOutMotion(0, 1000, 700);
            grow.start();
            getComponentForm().registerAnimated(this);
        }

        @Override
        public boolean animate() {
            if (grow != null && grow.isFinished()) {
                getComponentForm().deregisterAnimated(this);
            }
            return true;
        }

        @Override
        public void paint(Graphics g) {
            int max = 1;
            for (int n : counts) {
                max = Math.max(max, n);
            }
            int gap = dp(16);
            int r = dp(8) * 2;
            int w = counts.length == 0 ? 0 : (getWidth() - gap * (counts.length - 1)) / counts.length;
            float f = grow == null ? 1 : grow.getValue() / 1000f;
            g.setAntiAliased(true);
            for (int i = 0; i < counts.length; i++) {
                int x = getX() + i * (w + gap);
                g.setColor(0xE6E0E9);
                g.fillRoundRect(x, getY(), w, getHeight(), r, r);
                int h = (int) (getHeight() * counts[i] / (float) max * f);
                g.setColor(TRACK_COLORS[i]);
                g.fillRoundRect(x, getY() + getHeight() - h, w, h, r, r);
            }
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.getDisplayWidth(), dp(220));
        }
    }

    /** A session's own screen. */
    static final class Detail extends Form {
        Detail(JavaBench app, Session s) {
            super(TRACK_LABELS[s.track], BoxLayout.y());
            Form back = app.form;
            getToolbar().setTitleCentered(true);
            getToolbar().setBackCommand("", Toolbar.BackCommandPolicy.AS_ARROW, e -> back.showBack());
            getContentPane().getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
            getContentPane().getAllStyles().setPadding(3.2f, 3.2f, 3.2f, 3.2f);
            Label title = new Label(s.title);
            title.getAllStyles().setFont(font("native:MainRegular", 24));
            add(title);
            add(new Label(s.speaker + ", " + s.company));
            add(new Label("Day " + s.day + ", " + s.timeLabel(), FontImage.createMaterial(FontImage.MATERIAL_SCHEDULE, "Label", 3.5f)));
            add(new Label(s.room, FontImage.createMaterial(FontImage.MATERIAL_PLACE, "Label", 3.5f)));
            com.codename1.components.SpanLabel description = new com.codename1.components.SpanLabel(s.description);
            add(description);
        }
    }

    /**
     * The steps of Perf.kt, timed the same way: from the state change to the
     * marker's paint, one frame between steps.
     */
    static final class Perf implements Animation {
        private final JavaBench app;
        private final List<Long> recompose = new ArrayList<Long>();
        private final List<Long> work = new ArrayList<Long>();
        private final List<Long> frames = new ArrayList<Long>();
        private int phase;
        private int step;
        private int gap;
        private boolean waiting;
        private boolean requested;
        private long t0;
        private long last;

        Perf(JavaBench app) {
            this.app = app;
        }

        void start() {
            System.out.println("KMPBENCH perf-start 0");
            gap = 10;
            app.form.registerAnimated(this);
        }

        @Override
        public boolean animate() {
            Marker m = app.marker;
            if (waiting) {
                if (m.drawnSeq < m.seq) {
                    return false;
                }
                waiting = false;
                record(m.drawnAt);
                // Perf.kt notices the paint in the next frame and steps in the
                // one after; returning here keeps the same two frames per step.
                return false;
            }
            if (gap > 0) {
                gap--;
                return false;
            }
            if (!requested) {
                // The step runs at the start of the next cycle's layout pass,
                // where a Compose frame runs: change, layout and paint then
                // share one cycle on both sides.
                requested = true;
                app.stepHost.revalidateLater();
            }
            return false;
        }

        /** Called from the schedule tab's layout. */
        void layoutStep() {
            if (requested && !waiting) {
                requested = false;
                next();
            }
        }

        private void record(long drawnAt) {
            long us = (drawnAt - t0) / 1000;
            if (phase == 0) {
                if (step > 30) {
                    recompose.add(Long.valueOf(us));
                }
            } else if (phase >= 2) {
                work.add(Long.valueOf(us));
                if (last != 0) {
                    frames.add(Long.valueOf((drawnAt - last) / 1000));
                }
                last = drawnAt;
            }
        }

        private void mark() {
            Marker m = app.marker;
            m.seq++;
            m.repaint();
            waiting = true;
        }

        private void next() {
            if (phase == 0) {
                if (step == 330) {
                    report("recompose-median-us", pct(recompose, 50));
                    report("recompose-p95-us", pct(recompose, 95));
                    phase = 1;
                    step = 0;
                    app.setDay(1);
                    gap = 2;
                    return;
                }
                step++;
                t0 = System.nanoTime();
                app.setDay(app.day % 3 + 1);
                mark();
                return;
            }
            // phase 1 scrolls day 1 as a warm-up; phases 2..4 scroll days 1..3.
            if (step == 0) {
                app.schedule.scrollToCard(0);
                app.schedule.repaint();
                last = 0;
                step = 1;
                gap = 1;
                return;
            }
            if (step < SESSIONS_PER_DAY) {
                t0 = System.nanoTime();
                app.schedule.scrollToCard(step);
                app.schedule.repaint();
                step++;
                mark();
                return;
            }
            if (phase == 4) {
                app.form.deregisterAnimated(this);
                long fm = pct(frames, 50);
                report("scroll-frames", frames.size());
                report("scroll-frame-median-us", fm);
                report("scroll-frame-p95-us", pct(frames, 95));
                int jank = 0;
                for (Long f : frames) {
                    if (f.longValue() > 2 * fm) {
                        jank++;
                    }
                }
                report("scroll-jank-frames", jank);
                report("scroll-work-median-us", pct(work, 50));
                report("scroll-work-p95-us", pct(work, 95));
                report("perf-done", 0);
                return;
            }
            if (phase == 1) {
                work.clear();
                frames.clear();
            }
            phase++;
            step = 0;
            app.setDay(phase - 1);
            gap = 2;
        }

        @Override
        public void paint(Graphics g) {
        }

        private static long pct(List<Long> v, int p) {
            Long[] s = v.toArray(new Long[v.size()]);
            java.util.Arrays.sort(s);
            return s[Math.min(s.length - 1, (s.length * p) / 100)].longValue();
        }

        private static void report(String name, long v) {
            System.out.println("KMPBENCH " + name + " " + v);
        }
    }
}
