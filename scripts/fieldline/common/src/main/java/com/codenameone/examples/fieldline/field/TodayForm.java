package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.maps.LatLng;
import com.codename1.maps.Marker;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.TechnicianDto;
import com.codenameone.examples.fieldline.api.ClockDto;
import com.codenameone.examples.fieldline.api.TodayDto;
import com.codenameone.examples.fieldline.live.LiveChannel;
import com.codenameone.examples.fieldline.map.MapStage;
import com.codenameone.examples.fieldline.map.Maps;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.Menu;
import com.codenameone.examples.fieldline.ui.Nav;
import com.codenameone.examples.fieldline.ui.StatRow;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/// A technician's day: the route on the map with a numbered pin per stop,
/// and a sheet over it with what is next, how far through the day they are,
/// and every job in the order the planner put them.
///
/// The one button on the sheet is always the next thing to do -- leave for
/// the next job, say you have arrived, open the job you are at -- so a
/// technician with gloves on never hunts for it. The screen listens on the
/// live channel and is rebuilt when dispatch moves a job or the planner
/// re-optimizes, which is how a covered absence lands on somebody's phone.
public final class TodayForm implements Nav.Home {
    /// Where the map starts before the day is known: downtown Portland.
    static final LatLng CITY = new LatLng(45.5152, -122.6784);

    private final Form form = Ui.home("Today");
    private MapStage stage;
    private final List<Marker> markers = new ArrayList<Marker>();
    private Maps.Line route;
    private TodayDto day;

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        Menu menu = new Menu(form, Nav.FIELD);
        menu.item(FontImage.MATERIAL_AUTO_AWESOME, "Field assistant", "assistant",
                () -> AssistantForm.show(form, null));
        menu.item(FontImage.MATERIAL_SCHEDULE, "Working hours", "availability",
                () -> AvailabilityForm.show(form));
        menu.item(FontImage.MATERIAL_BEACH_ACCESS, "Time off", "timeOff",
                () -> TimeOffForm.show(form));
        stage = new MapStage(CITY, Maps.CITY_ZOOM, menu.button());
        form.add(stage.layers());
        menu.install();
        stage.addRound(FontImage.MATERIAL_ROUTE, "fitRoute", this::frame);
        stage.addRound(FontImage.MATERIAL_AUTO_AWESOME, "askAssistant",
                () -> AssistantForm.show(form, null));
        stage.sheet().peek().add(Ui.label("Planning your day...", "FlSheetTitle"));
        LiveChannel.listen(new LiveChannel.Adapter() {
            @Override
            public void jobChanged(String id, String status) {
                if (mine(id)) {
                    load();
                }
            }

            @Override
            public void scheduleChanged(String runId, String summary) {
                if (Nav.isHome(form)) {
                    if (summary != null && summary.length() > 0) {
                        stage.chip(summary, FontImage.MATERIAL_AUTO_FIX_HIGH);
                    }
                    load();
                }
            }
        });
        form.addShowListener(e -> load());
        // A window widened past a phone's, or a tablet: the split day.
        Layouts.onChange(form, Nav::home);
        load();
    }

    private boolean mine(String id) {
        // Any job: one that was not ours a moment ago may have become ours.
        return id != null && Nav.isHome(form);
    }

    void load() {
        Api.field().today(Net.to(this::show, Ui::fail));
    }

    private void show(TodayDto today) {
        if (!Nav.isHome(form) && form != CN.getCurrentForm()) {
            return;
        }
        day = today;
        boolean underWay = false;
        int done = 0;
        for (JobDto job : today.jobs()) {
            underWay |= "EN_ROUTE".equals(job.status()) || "ON_SITE".equals(job.status());
            if ("COMPLETED".equals(job.status())) {
                done++;
            }
        }
        Tracker.start(underWay);
        CalendarSync.sync(today.jobs());
        drawRoute(today);
        fillSheet(today, done);
        frame();
    }

    private void drawRoute(TodayDto today) {
        for (Marker m : markers) {
            stage.map.removeMarker(m);
        }
        markers.clear();
        if (route != null) {
            route.remove(stage.map);
            route = null;
        }
        TechnicianDto me = today.me();
        int color = color(me);
        List<LatLng> line = new ArrayList<LatLng>();
        line.add(new LatLng(me.homeLat(), me.homeLng()));
        JobDto next = next(today);
        int n = 0;
        for (final JobDto job : today.jobs()) {
            n++;
            LatLng at = new LatLng(job.lat(), job.lng());
            line.add(at);
            markers.add(Maps.stop(stage.map, at, String.valueOf(n), color,
                    "COMPLETED".equals(job.status()), job == next, job.title(),
                    e -> JobForm.show(form, job.id())));
        }
        if (line.size() > 1) {
            route = Maps.route(stage.map, line);
        }
        if (me.lat() != 0 || me.lng() != 0) {
            markers.add(Maps.person(stage.map, new LatLng(me.lat(), me.lng()), me.initials(),
                    color, me.name(), null));
        }
    }

    private void frame() {
        if (day == null) {
            return;
        }
        List<LatLng> all = new ArrayList<LatLng>();
        all.add(new LatLng(day.me().homeLat(), day.me().homeLng()));
        for (JobDto job : day.jobs()) {
            all.add(new LatLng(job.lat(), job.lng()));
        }
        stage.frame(all.toArray(new LatLng[all.size()]));
    }

    private void fillSheet(TodayDto today, int done) {
        Container peek = stage.sheet().peek();
        Container more = stage.sheet().more();
        stage.sheet().reset();

        Container top = new Container(new BorderLayout());
        top.setUIID("FlSheetTop");
        Container words = new Container(BoxLayout.y());
        words.add(Ui.text(greeting(today.me().name()), "FlSheetTitle"));
        words.add(Ui.label(summary(today), "FlSheetDetail"));
        top.add(BorderLayout.CENTER, words);
        DayRing ring = new DayRing();
        ring.setName("dayRing");
        ring.set(done, today.jobs().size());
        top.add(BorderLayout.EAST, ring);
        peek.add(top);
        peek.add(clockRow(today, this::show));

        JobDto next = next(today);
        if (next == null) {
            peek.add(Ui.banner(FontImage.MATERIAL_TASK_ALT,
                    today.jobs().isEmpty() ? "Nothing planned" : "All done for today",
                    today.jobs().isEmpty()
                            ? "The planner gives you jobs within the hours you set."
                            : "Every job is signed off. Nice work.", "FlBannerIconGood"));
        } else {
            peek.add(nextCard(next));
        }

        StatRow stats = Ui.stats(3);
        stats.stat(String.valueOf(today.jobs().size()), "Jobs", "statJobs");
        stats.stat(Ui.duration(today.driveMinutes()), "Driving", "statDrive");
        stats.stat(today.me().utilizationPct() + "%", "Booked", "statBooked");
        more.add(stats);
        more.add(Ui.label("Your route", "FlSectionTitle"));
        Container timeline = new Container(BoxLayout.y()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension d = super.calcPreferredSize();
                d.setHeight(Math.min(d.getHeight(), CN.getDisplayHeight() * 11 / 20));
                return d;
            }
        };
        timeline.setScrollableY(true);
        timeline.setScrollVisible(false);
        timeline.setName("timeline");
        int n = 0;
        for (JobDto job : today.jobs()) {
            n++;
            if (job.travelBefore() > 0) {
                timeline.add(travel(job.travelBefore()));
            }
            timeline.add(stop(n, job, job == next));
        }
        more.add(timeline);
        stage.sheet().ready(false);
        form.revalidate();
    }

    private Container nextCard(final JobDto job) {
        Container card = new Container(new BorderLayout());
        card.setUIID("FlNextCard");
        card.setName("nextJob");
        Container lines = new Container(BoxLayout.y());
        Label when = Ui.label(Lang.tr("Next") + Ui.DOT + Ui.clock(job.plannedStart()) + " - "
                + Ui.clock(job.plannedEnd()), "FlNextWhen");
        lines.add(when);
        lines.add(Ui.label(job.title(), "FlNextTitle"));
        lines.add(Ui.label(job.siteName() + Ui.DOT + job.address(), "FlNextDetail"));
        Container chips = new Container(new FlowLayout());
        chips.add(Ui.badge(Ui.status(job.status()), Ui.statusBadge(job.status())));
        if (job.priority() >= 4) {
            chips.add(Ui.badge("Urgent", "FlBadgeUrgent"));
        }
        chips.add(Ui.badge(kind(job.kind()), "FlBadgeKind"));
        lines.add(chips);
        card.add(BorderLayout.CENTER, lines);
        Button go = Ui.primary(action(job.status()), "nextAction", null);
        go.addActionListener(e -> JobActions.advance(form, job, this::load));
        Button open = Ui.secondary("Details", "nextOpen", e -> JobForm.show(form, job.id()));
        Container buttons = new Container(new BorderLayout());
        buttons.setUIID("FlButtonRow");
        buttons.add(BorderLayout.CENTER, go);
        buttons.add(BorderLayout.EAST, open);
        card.add(BorderLayout.SOUTH, buttons);
        return card;
    }

    private Component travel(int minutes) {
        Container row = new Container(new BorderLayout());
        row.setUIID("FlTravel");
        Label rail = new Label("", "FlRailDashed");
        row.add(BorderLayout.WEST, rail);
        row.add(BorderLayout.CENTER, Ui.chip(Lang.tr("{0} drive", Ui.duration(minutes)),
                FontImage.MATERIAL_DIRECTIONS_CAR));
        return row;
    }

    private Component stop(int number, final JobDto job, boolean current) {
        Container row = new Container(new BorderLayout());
        row.setUIID(current ? "FlStopCurrent" : "FlStop");
        Container time = new Container(BoxLayout.y());
        time.setUIID("FlStopTime");
        time.add(Ui.label(Ui.clock(job.plannedStart()), "FlStopClock"));
        time.add(Ui.label(Ui.duration(job.durationMinutes()), "FlStopLength"));
        row.add(BorderLayout.WEST, time);
        Container body = new Container(new BorderLayout());
        body.setUIID("COMPLETED".equals(job.status()) ? "FlStopCardDone" : "FlStopCard");
        Label badge = new Label(String.valueOf(number), "FlStopNumber");
        body.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(badge));
        Container lines = new Container(BoxLayout.y());
        lines.add(Ui.label(job.title(), "FlStopTitle"));
        lines.add(Ui.label(job.siteName(), "FlStopDetail"));
        body.add(BorderLayout.CENTER, lines);
        body.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(
                Ui.badge(Ui.status(job.status()), Ui.statusBadge(job.status()))));
        row.add(BorderLayout.CENTER, body);
        return Ui.tap(row, "job-" + job.id(), e -> JobForm.show(form, job.id()));
    }

    /// Clocking in and out: whether the technician is on the clock, since
    /// when, and the switch that changes it. Setting off to a job clocks them
    /// in by itself; the switch is for the start and end of the day.
    static Component clockRow(TodayDto today, final OnDay redraw) {
        final boolean in = today.clockedInAt() > 0;
        Container row = Ui.row(FontImage.MATERIAL_PUNCH_CLOCK,
                Lang.tr(in ? "On the clock" : "Off the clock"),
                in ? Lang.tr("Since {0}", Ui.clock(today.clockedInAt()))
                        : Lang.tr("Clock in when your day starts"),
                Ui.toggle("clock", in, on -> Api.field().clock(new ClockDto(on), Net.to(day -> {
                    Ui.say(Lang.tr(on ? "Clocked in" : "Clocked out"));
                    redraw.show(day);
                }, Ui::fail))));
        row.setName("clockRow");
        return row;
    }

    /// Redraws a day from a fresh answer.
    interface OnDay {
        void show(TodayDto day);
    }

    static JobDto next(TodayDto today) {
        for (JobDto job : today.jobs()) {
            if (!"COMPLETED".equals(job.status()) && !"CANCELLED".equals(job.status())) {
                return job;
            }
        }
        return null;
    }

    static String action(String status) {
        if ("EN_ROUTE".equals(status)) {
            return "I've arrived";
        }
        if ("ON_SITE".equals(status)) {
            return "Continue job";
        }
        return "Start driving";
    }

    public static String kind(String kind) {
        if ("INSPECTION".equals(kind)) {
            return "Inspection";
        }
        if ("INSTALL".equals(kind)) {
            return "Install";
        }
        if ("MAINTENANCE".equals(kind)) {
            return "Maintenance";
        }
        return "Repair";
    }

    static int color(TechnicianDto tech) {
        return parseColor(tech == null ? null : tech.color(), 0x2f6fed);
    }

    public static int parseColor(String hex, int fallback) {
        if (hex == null || hex.length() < 6) {
            return fallback;
        }
        String h = hex.charAt(0) == '#' ? hex.substring(1) : hex;
        int out = 0;
        for (int i = 0; i < h.length() && i < 6; i++) {
            int d = Character.digit(h.charAt(i), 16);
            if (d < 0) {
                return fallback;
            }
            out = out * 16 + d;
        }
        return out;
    }

    static String greeting(String name) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String first = name == null ? "" : name;
        int space = first.indexOf(' ');
        if (space > 0) {
            first = first.substring(0, space);
        }
        String hello = hour < 12 ? "Good morning, {0}" : hour < 18 ? "Good afternoon, {0}"
                : "Good evening, {0}";
        return Lang.tr(hello, first);
    }

    static String summary(TodayDto today) {
        int left = 0;
        for (JobDto job : today.jobs()) {
            if (!"COMPLETED".equals(job.status())) {
                left++;
            }
        }
        if (today.jobs().isEmpty()) {
            return Ui.day(System.currentTimeMillis());
        }
        if (!isToday(today.day())) {
            // Nothing left today: the sheet shows the next working day, and
            // says so before anything else.
            return Lang.tr("Up next: {0}", Ui.weekday(today.day()));
        }
        return Lang.tr("{0} jobs left", String.valueOf(left)) + Ui.DOT
                + Lang.tr("{0} on the road", Ui.duration(today.driveMinutes()));
    }

    private static boolean isToday(long dayStart) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTime().getTime();
        return dayStart <= 0 || (dayStart >= start - 3600000L && dayStart < start + 82800000L);
    }
}
