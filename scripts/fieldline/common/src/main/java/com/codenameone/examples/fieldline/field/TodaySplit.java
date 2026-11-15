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
package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.Marker;
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
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.TodayDto;
import com.codenameone.examples.fieldline.live.LiveChannel;
import com.codenameone.examples.fieldline.map.Maps;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.Menu;
import com.codenameone.examples.fieldline.ui.Nav;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// A technician's day on a tablet: the route and the job at once.
///
/// A phone shows the route and opens each job over it. A tablet is held on
/// site, often in a case on the dashboard or the bench, and has the room for
/// both, so nothing is ever a screen away:
///
/// - On its side: the day down the left -- how far through it the technician
///   is, the stops in order, and a map of the route under them -- and the
///   job picked from it on the right, its cards in two columns over a bar
///   with the one thing to do next.
/// - Upright: the route across the top, the map beside the stops, and the
///   job under it.
///
/// The job is [JobForm]'s own content, so the checklist, photos, diagnosis
/// and signature work exactly as on a phone.
public final class TodaySplit implements Nav.Home {
    private static final float ROUTE_MM = 92f;
    private static final float MAP_MM = 48f;
    /// A job pane at least this wide puts its cards in two columns.
    private static final float TWO_COLUMNS_MM = 170f;

    /// The stop last picked, kept across a rebuild when the tablet turns.
    private static String picked;

    private final Form form = Ui.home("Today");
    private final boolean landscape = Layouts.wide();
    private final Container root = new Container(new BorderLayout());
    private final Container header = new Container(BoxLayout.y());
    private final Container stops = new Container(BoxLayout.y());
    private final Container job = new Container(new BorderLayout());
    private final List<Marker> markers = new ArrayList<Marker>();
    private MapView map;
    private Maps.Line route;
    private TodayDto day;
    private String shown;

    /// Whether the split day fits: any tablet, and any window as wide as one
    /// on its side.
    public static boolean fits() {
        return CN.isTablet() || Layouts.wide();
    }

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

        Container bar = new Container(new BorderLayout());
        bar.setUIID("FlBoardTop");
        bar.setSafeArea(true);
        Container inner = new Container(new BorderLayout());
        inner.setUIID("FlBoardBar");
        inner.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(menu.flatButton()));
        Container title = new Container(BoxLayout.y());
        title.add(Ui.label("My day", "FlBoardTitle"));
        title.add(Ui.label(Ui.day(System.currentTimeMillis()), "FlBoardDate"));
        inner.add(BorderLayout.CENTER, title);
        inner.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(
                Ui.flat(FontImage.MATERIAL_AUTO_AWESOME, "askAssistant",
                        e -> AssistantForm.show(form, null))));
        bar.add(BorderLayout.CENTER, inner);

        map = Maps.create(TodayForm.CITY, Maps.CITY_ZOOM);
        Component mapBox = map.asComponent();
        stops.setScrollableY(true);
        stops.setName("timeline");
        header.setUIID("FlSplitHeader");
        header.add(Ui.label("Planning your day...", "FlSheetTitle"));

        Container routeSide;
        if (landscape) {
            // The stops take their natural height and the map fills what
            // is left, so a short day shows a big map instead of a gap.
            // The stop list scrolls only once it would squeeze the map
            // below MAP_MM.
            Container mapHolder = new Container(new BorderLayout());
            mapHolder.setUIID("FlSplitMap");
            mapHolder.add(BorderLayout.CENTER, mapBox);
            final float maxStops = CN.getDisplayHeight() - CN.convertToPixels(MAP_MM + 60f);
            Container top = new Container(BoxLayout.y()) {
                @Override
                protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                    com.codename1.ui.geom.Dimension d = super.calcPreferredSize();
                    d.setHeight(Math.min(d.getHeight(), Math.max(0, (int) maxStops)));
                    return d;
                }
            };
            top.setScrollableY(true);
            stops.setScrollableY(false);
            top.add(stops);
            Container north = new Container(new BorderLayout());
            north.add(BorderLayout.NORTH, header);
            north.add(BorderLayout.CENTER, top);
            routeSide = sized(null, ROUTE_MM, 0);
            routeSide.add(BorderLayout.NORTH, north);
            routeSide.add(BorderLayout.CENTER, mapHolder);
            root.add(BorderLayout.WEST, routeSide);
        } else {
            routeSide = sized(null, 0,
                    CN.getDisplayHeight() * 100f / CN.convertToPixels(100f) * 0.4f);
            Container pair = new Container(new GridLayout(1, 2));
            Container mapHolder = new Container(new BorderLayout());
            mapHolder.setUIID("FlSplitMap");
            mapHolder.add(BorderLayout.CENTER, mapBox);
            pair.add(mapHolder).add(stops);
            routeSide.add(BorderLayout.NORTH, header);
            routeSide.add(BorderLayout.CENTER, pair);
            root.add(BorderLayout.NORTH, routeSide);
        }
        routeSide.setUIID("FlSplitRoute");
        routeSide.setName("route");
        job.setUIID("FlSplitJob");
        Container main = new Container(new BorderLayout());
        main.add(BorderLayout.NORTH, bar);
        main.add(BorderLayout.CENTER, root);
        root.add(BorderLayout.CENTER, job);
        form.add(main);
        menu.install();
        Layouts.onChange(form, Nav::home);

        LiveChannel.listen(new LiveChannel.Adapter() {
            @Override
            public void jobChanged(String id, String status) {
                if (Nav.isHome(form)) {
                    load();
                }
            }

            @Override
            public void scheduleChanged(String runId, String summary) {
                if (Nav.isHome(form)) {
                    if (summary != null && summary.length() > 0) {
                        Ui.say(summary);
                    }
                    load();
                }
            }
        });
        form.addShowListener(e -> load());
        load();
    }

    /// A container of a fixed width or height, in millimetres, holding
    /// `content` when it is not null; 0 leaves that side to the layout.
    private static Container sized(Component content, final float widthMm,
            final float heightMm) {
        Container box = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension size = super.calcPreferredSize();
                if (widthMm > 0) {
                    size.setWidth(CN.convertToPixels(widthMm));
                }
                if (heightMm > 0) {
                    size.setHeight(CN.convertToPixels(heightMm));
                }
                return size;
            }
        };
        if (content == null) {
            return box;
        }
        box.add(BorderLayout.CENTER, content);
        return box;
    }

    void load() {
        Api.field().today(Net.to(this::fill, Ui::fail));
    }

    private void fill(TodayDto today) {
        if (!Nav.isHome(form)) {
            return;
        }
        day = today;
        boolean underWay = false;
        int done = 0;
        for (JobDto j : today.jobs()) {
            underWay |= "EN_ROUTE".equals(j.status()) || "ON_SITE".equals(j.status());
            if ("COMPLETED".equals(j.status())) {
                done++;
            }
        }
        Tracker.start(underWay);
        CalendarSync.sync(today.jobs());

        header.removeAll();
        Container top = new Container(new BorderLayout());
        Container words = new Container(BoxLayout.y());
        words.add(Ui.text(TodayForm.greeting(today.me().name()), "FlSheetTitle"));
        words.add(Ui.label(TodayForm.summary(today), "FlSheetDetail"));
        top.add(BorderLayout.CENTER, words);
        DayRing ring = new DayRing();
        ring.setName("dayRing");
        ring.set(done, today.jobs().size());
        top.add(BorderLayout.EAST, ring);
        header.add(top);
        header.add(TodayForm.clockRow(today, this::fill));

        if (picked == null || find(today, picked) == null) {
            JobDto next = TodayForm.next(today);
            picked = next != null ? next.id()
                    : today.jobs().isEmpty() ? null : today.jobs().get(0).id();
        }
        stops.removeAll();
        int n = 0;
        for (final JobDto j : today.jobs()) {
            n++;
            stops.add(stop(n, j, j.id().equals(picked)));
        }
        if (today.jobs().isEmpty()) {
            stops.add(Ui.banner(FontImage.MATERIAL_TASK_ALT, "Nothing planned",
                    "The planner gives you jobs within the hours you set.",
                    "FlBannerIconGood"));
        }
        drawRoute(today);
        if (picked == null) {
            shown = null;
            job.removeAll();
            job.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_EVENT_AVAILABLE,
                    "A free day", "Jobs the planner gives you show up here."));
        } else if (!picked.equals(shown)) {
            open(picked);
        }
        form.revalidate();
    }

    private void open(String id) {
        picked = id;
        shown = id;
        job.removeAll();
        int room = CN.getDisplayWidth() - (landscape ? CN.convertToPixels(ROUTE_MM) : 0);
        job.add(BorderLayout.CENTER, JobForm.pane(form, id, this::load,
                room >= CN.convertToPixels(TWO_COLUMNS_MM)));
        form.revalidate();
    }

    private Component stop(int number, final JobDto j, boolean current) {
        Container row = new Container(new BorderLayout());
        row.setUIID(current ? "FlSplitStopPicked" : "FlSplitStop");
        Label badge = new Label(String.valueOf(number), "FlStopNumber");
        row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(badge));
        Container lines = new Container(BoxLayout.y());
        lines.add(Ui.label(j.title(), "FlStopTitle"));
        lines.add(Ui.label(Ui.clock(j.plannedStart()) + Ui.DOT + j.siteName()
                + (j.travelBefore() > 0 ? Ui.DOT + Lang.tr("{0} drive",
                        Ui.duration(j.travelBefore())) : ""), "FlStopDetail"));
        row.add(BorderLayout.CENTER, lines);
        row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(
                Ui.badge(Ui.status(j.status()), Ui.statusBadge(j.status()))));
        return Ui.tap(row, "job-" + j.id(), e -> {
            picked = j.id();
            fill(day);
        });
    }

    private void drawRoute(TodayDto today) {
        for (Marker m : markers) {
            map.removeMarker(m);
        }
        markers.clear();
        if (route != null) {
            route.remove(map);
            route = null;
        }
        int color = TodayForm.color(today.me());
        List<LatLng> line = new ArrayList<LatLng>();
        line.add(new LatLng(today.me().homeLat(), today.me().homeLng()));
        int n = 0;
        for (final JobDto j : today.jobs()) {
            n++;
            LatLng at = new LatLng(j.lat(), j.lng());
            line.add(at);
            markers.add(Maps.stop(map, at, String.valueOf(n), color,
                    "COMPLETED".equals(j.status()), j.id().equals(picked), j.title(), e -> {
                        picked = j.id();
                        fill(day);
                    }));
        }
        if (line.size() > 1) {
            route = Maps.route(map, line);
        }
        if (today.me().lat() != 0 || today.me().lng() != 0) {
            markers.add(Maps.person(map, new LatLng(today.me().lat(), today.me().lng()),
                    today.me().initials(), color, today.me().name(), null));
        }
        Maps.show(map, line.toArray(new LatLng[line.size()]));
    }

    private static JobDto find(TodayDto today, String id) {
        for (JobDto j : today.jobs()) {
            if (j.id().equals(id)) {
                return j;
            }
        }
        return null;
    }
}
