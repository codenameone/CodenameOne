package com.codenameone.examples.fieldline.dispatch;

import com.codename1.appkit.Net;
import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.Marker;
import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.scheduler.Gantt;
import com.codename1.ui.scheduler.GanttAdapter;
import com.codename1.ui.scheduler.Scheduler;
import com.codename1.ui.scheduler.SchedulerAdapter;
import com.codenameone.examples.fieldline.Telemetry;
import com.codenameone.examples.fieldline.api.BoardDto;
import com.codenameone.examples.fieldline.api.BreakDto;
import com.codenameone.examples.fieldline.api.ChangeDto;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.KpiDto;
import com.codenameone.examples.fieldline.api.MoveDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.api.TechnicianDto;
import com.codenameone.examples.fieldline.api.TimeOffDto;
import com.codenameone.examples.fieldline.api.ValidationDto;
import com.codenameone.examples.fieldline.field.AssistantForm;
import com.codenameone.examples.fieldline.live.LiveChannel;
import com.codenameone.examples.fieldline.map.Maps;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.BottomSheet;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.Menu;
import com.codenameone.examples.fieldline.ui.Nav;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The dispatcher's board.
///
/// Three views of one plan, switched at the top:
///
/// - **Crew** -- a row per technician, time running across, jobs as blocks
///   and time off hatched. Dragging a block to another time or another row
///   is a proposal: the server checks it against skills, windows, hours and
///   travel and says what it would cost before anything changes.
/// - **Gantt** -- each technician's route as a summary bar over its jobs,
///   with the last run's plan drawn as a baseline so the optimizer's moves
///   are visible at a glance.
/// - **Map** -- where everybody is, live, and the jobs nobody has yet.
///
/// Over all three sit today's figures and the optimizer. "Optimize" runs
/// the same search the server runs on every change, on the whole horizon,
/// and answers with what it did.
public final class BoardForm implements Nav.Home {
    /// The colour the plan's breaks are drawn in.
    private static final int BREAK_COLOR = 0xf2b33d;
    private static final String CREW = "crew";
    private static final String GANTT = "gantt";
    private static final String MAP = "map";
    private static final String UNASSIGNED_ROW = "unassigned";

    /// The width of the inspector a wide board keeps beside the plan.
    private static final float INSPECTOR_MM = 92f;

    private final Form form;
    /// Whether this board is the console's: the plan with an inspector docked
    /// beside it, where a phone's slides up a sheet over it.
    private final boolean desk;
    private final Container root = new Container(new BorderLayout());
    private final Container inspector = new Container(BoxLayout.y());
    private Runnable newJob;
    private Runnable reviewTimeOff;
    private final Container kpis = new Container(new GridLayout(1, 5));
    private final Container banners = new Container(BoxLayout.y());
    private final Container content = new Container(new BorderLayout());
    private final BottomSheet sheet = new BottomSheet();
    private String view = CREW;
    private BoardDto board;
    private MapView map;
    private final Map<String, Marker> people = new HashMap<String, Marker>();
    private final List<Marker> open = new ArrayList<Marker>();
    private boolean busy;
    /// Whether the inspector shows something picked, not the overview.
    private boolean shown;

    /// The phone's board, a screen of its own.
    public BoardForm() {
        form = Ui.home("Board");
        desk = false;
    }

    /// The console's board, on `on`.
    ///
    /// @param newJob what "New job" opens
    /// @param reviewTimeOff where the inspector's time-off notice leads
    BoardForm(Form on, Runnable newJob, Runnable reviewTimeOff) {
        form = on;
        desk = true;
        this.newJob = newJob;
        this.reviewTimeOff = reviewTimeOff;
    }

    @Override
    public Form form() {
        return form;
    }

    /// The board as the console shows it: everything below the window's title.
    Component page() {
        return root;
    }

    @Override
    public void start() {
        if (desk) {
            startDesk();
            return;
        }
        Menu menu = new Menu(form, Nav.DISPATCH);
        menu.item(FontImage.MATERIAL_ADD_TASK, "New job", "newJob",
                () -> NewJobForm.show(form, this::load));
        menu.item(FontImage.MATERIAL_BEACH_ACCESS, "Time-off requests", "timeOffQueue",
                () -> TimeOffQueueForm.show(form, this::load));
        menu.item(FontImage.MATERIAL_HANDYMAN, "Jobs", "jobs",
                () -> Console.phone(form, "Jobs", Console.JOBS));
        menu.item(FontImage.MATERIAL_HISTORY, "Planner runs", "runs", () -> runs());
        menu.item(FontImage.MATERIAL_INSERT_CHART, "Staffing", "reports",
                () -> Console.phone(form, "Staffing", Console.REPORTS));
        com.codenameone.examples.fieldline.api.UserDto user =
                com.codenameone.examples.fieldline.net.Session.user();
        if (user != null && user.admin()) {
            menu.item(FontImage.MATERIAL_ENGINEERING, "Crew", "crew",
                    () -> Console.phone(form, "Crew", Console.CREW));
            menu.item(FontImage.MATERIAL_PUNCH_CLOCK, "Timesheets", "timesheets",
                    () -> Console.phone(form, "Timesheets", Console.TIMESHEETS));
            menu.item(FontImage.MATERIAL_PAYMENTS, "Labour rates", "rates",
                    () -> Console.phone(form, "Labour rates", Console.RATES));
            menu.item(FontImage.MATERIAL_TUNE, "Company settings", "rules",
                    () -> Console.phone(form, "Company settings", Console.RULES));
            menu.item(FontImage.MATERIAL_MANAGE_ACCOUNTS, "Accounts", "accounts",
                    () -> Console.phone(form, "Accounts", Console.ACCOUNTS));
        }
        menu.item(FontImage.MATERIAL_AUTO_AWESOME, "Assistant", "assistant",
                () -> AssistantForm.show(form, null));

        Container top = new Container(BoxLayout.y());
        top.setUIID("FlBoardTop");
        top.setSafeArea(true);
        Container bar = new Container(new BorderLayout());
        bar.setUIID("FlBoardBar");
        bar.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(menu.flatButton()));
        Container title = new Container(BoxLayout.y());
        title.add(Ui.label("Dispatch", "FlBoardTitle"));
        title.add(Ui.label(Ui.day(System.currentTimeMillis()), "FlBoardDate"));
        bar.add(BorderLayout.CENTER, title);
        // One action in the bar, the one a dispatcher reaches for: a new job
        // and the assistant are in the menu, and a phone has no room for a
        // title between four buttons.
        Container actions = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        Button optimize = Ui.primary("Optimize", "optimize", e -> optimize());
        FontImage.setMaterialIcon(optimize, FontImage.MATERIAL_AUTO_FIX_HIGH, 3.4f);
        optimize.setUIID("FlOptimize");
        actions.add(optimize);
        bar.add(BorderLayout.EAST, actions);
        top.add(bar);
        kpis.setUIID("FlKpis");
        top.add(kpis);
        top.add(Ui.choice("boardView", new String[] {"Crew", "Gantt", "Map"},
                new String[] {CREW, GANTT, MAP}, view, v -> {
                    view = v;
                    render();
                }));
        top.add(banners);

        root.add(BorderLayout.NORTH, top);
        root.add(BorderLayout.CENTER, content);
        root.add(BorderLayout.SOUTH, sheet);
        sheet.setHidden(true);
        form.add(root);
        menu.install();
        Layouts.onChange(form, () -> {
            if (Console.fits()) {
                Nav.home();
            } else {
                render();
            }
        });
        listen();
    }

    /// The console's board: the figures, the views and the actions along one
    /// header, the plan in the middle and an inspector docked on the right,
    /// where what a phone would slide up from the bottom stays in view.
    private void startDesk() {
        Container top = new Container(BoxLayout.y());
        top.setUIID("FlDeskTop");
        Container bar = new Container(new BorderLayout());
        Container title = new Container(BoxLayout.y());
        title.add(Ui.label("Dispatch", "FlBoardTitle"));
        title.add(Ui.label(Ui.day(System.currentTimeMillis()), "FlBoardDate"));
        bar.add(BorderLayout.WEST, title);
        kpis.setUIID("FlDeskKpis");
        bar.add(BorderLayout.CENTER, kpis);
        Container actions = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        Button add = Ui.secondary("New job", "newJob", e -> newJob.run());
        FontImage.setMaterialIcon(add, FontImage.MATERIAL_ADD_TASK, 3.2f);
        add.setUIID("FlDeskAction");
        Button optimize = Ui.primary("Optimize", "optimize", e -> optimize());
        FontImage.setMaterialIcon(optimize, FontImage.MATERIAL_AUTO_FIX_HIGH, 3.2f);
        optimize.setUIID("FlOptimize");
        actions.add(add).add(optimize);
        bar.add(BorderLayout.EAST, actions);
        top.add(bar);
        Container views = new Container(new BorderLayout());
        Container choice = Ui.choice("boardView", new String[] {"Crew", "Gantt", "Map"},
                new String[] {CREW, GANTT, MAP}, view, v -> {
                    view = v;
                    render();
                });
        segments(choice);
        views.add(BorderLayout.WEST, choice);
        top.add(views);

        Container side = new Container(new BorderLayout()) {
            @Override
            protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                com.codename1.ui.geom.Dimension size = super.calcPreferredSize();
                size.setWidth(CN.convertToPixels(INSPECTOR_MM));
                return size;
            }
        };
        side.setUIID("FlInspector");
        side.setName("inspector");
        inspector.setScrollableY(true);
        side.add(BorderLayout.CENTER, inspector);

        root.add(BorderLayout.NORTH, top);
        root.add(BorderLayout.CENTER, content);
        root.add(BorderLayout.EAST, side);
        listen();
    }

    private void listen() {
        LiveChannel.listen(new LiveChannel.Adapter() {
            @Override
            public void jobChanged(String id, String status) {
                if (Nav.isHome(form) && !busy) {
                    load();
                }
            }

            @Override
            public void scheduleChanged(String runId, String summary) {
                if (Nav.isHome(form) && !busy) {
                    load();
                }
            }

            @Override
            public void technicianMoved(String techId, double lat, double lng) {
                Marker m = people.get(techId);
                if (m != null && map != null && board != null) {
                    map.removeMarker(m);
                    TechnicianDto t = tech(techId);
                    if (t != null) {
                        people.put(techId, person(t, lat, lng));
                    } else {
                        people.remove(techId);
                    }
                }
            }
        });
        content.add(BorderLayout.CENTER, Ui.label("Loading the board...", "WlMuted"));
        load();
    }

    void load() {
        Api.dispatch().board(0, 0, Net.to(b -> {
            board = b;
            fillKpis(b.kpis());
            fillBanners(b);
            render();
        }, Ui::fail));
    }

    private void fillKpis(KpiDto k) {
        kpis.removeAll();
        kpis.add(kpi(String.valueOf(k.scheduled()), "Scheduled", "kpiScheduled", null));
        kpis.add(kpi(String.valueOf(k.unassigned()), "Unassigned", "kpiUnassigned",
                k.unassigned() > 0 ? "FlKpiWarn" : null));
        kpis.add(kpi(String.valueOf(k.lateJobs()), "Past SLA", "kpiLate",
                k.lateJobs() > 0 ? "FlKpiWarn" : null));
        kpis.add(kpi(Ui.duration(k.driveMinutes()), "Driving", "kpiDrive", null));
        kpis.add(kpi(k.utilizationPct() + "%", "Booked", "kpiBooked", null));
    }

    private static Component kpi(String value, String caption, String name, String accent) {
        Container box = new Container(BoxLayout.y());
        box.setUIID(accent == null ? "FlKpi" : accent);
        Label v = Ui.label(value, "FlKpiValue");
        v.setName(name);
        box.add(v);
        box.add(Ui.label(caption, "FlKpiCaption"));
        return box;
    }

    private void fillBanners(BoardDto b) {
        banners.removeAll();
        if (desk) {
            if (!busy && !shown) {
                overview();
            }
            return;
        }
        int pending = 0;
        for (TimeOffDto t : b.timeOff()) {
            if ("PENDING".equals(t.status())) {
                pending++;
            }
        }
        if (pending > 0) {
            Container row = Ui.row(FontImage.MATERIAL_BEACH_ACCESS,
                    Lang.tr("Time-off requests"),
                    Lang.tr("{0} waiting", String.valueOf(pending)), Ui.badge(
                            Lang.tr("Review"), "FlBadgeWarn"));
            row.setUIID("FlBanner");
            banners.add(Ui.tap(row, "reviewTimeOff",
                    e -> TimeOffQueueForm.show(form, this::load)));
        }
    }

    private void render() {
        if (board == null) {
            return;
        }
        content.removeAll();
        if (map != null && !MAP.equals(view)) {
            map = null;
            people.clear();
            open.clear();
        }
        if (GANTT.equals(view)) {
            content.add(BorderLayout.CENTER, gantt());
        } else if (MAP.equals(view)) {
            content.add(BorderLayout.CENTER, crewMap());
        } else {
            content.add(BorderLayout.CENTER, crew());
        }
        form.revalidate();
    }

    // ---- Crew: resource timeline -------------------------------------------

    /// The first day of the span with work in it. A board opened on a Sunday
    /// or after hours starts on the day the crew next works, where the
    /// timeline opens at the start of the morning instead of on an empty now.
    private long firstWorkDay() {
        long day = board.dayStart();
        long first = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (JobDto j : board.jobs()) {
            long end = j.plannedEnd() > 0 ? j.plannedEnd() : j.windowEnd();
            if (j.plannedStart() > 0 && end > now && j.plannedStart() < first) {
                first = j.plannedStart();
            }
        }
        if (first == Long.MAX_VALUE || first < day + 86400000L) {
            return day;
        }
        return first;
    }

    private Component crew() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addResource(new ScheduleResource(UNASSIGNED_ROW, Lang.tr("Unassigned"))
                .setColor(0xe8590c).setDetail(Lang.tr("Waiting for a technician")));
        // Each row counts the jobs the timeline shows, not just today's: on a
        // weekend today's count is zero for everyone and says nothing.
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (JobDto j : board.jobs()) {
            if (j.techId() != null && !"CANCELLED".equals(j.status())) {
                Integer c = counts.get(j.techId());
                counts.put(j.techId(), Integer.valueOf(c == null ? 1 : c.intValue() + 1));
            }
        }
        for (TechnicianDto t : board.technicians()) {
            Integer c = counts.get(t.id());
            model.addResource(new ScheduleResource(t.id(), t.name()).setColor(color(t))
                    .setDetail(Lang.count(c == null ? 0 : c.intValue(), "1 job", "{0} jobs"))
                    .setUserObject(t));
        }
        for (JobDto j : board.jobs()) {
            long start = j.plannedStart() > 0 ? j.plannedStart() : j.windowStart();
            long end = j.plannedEnd() > 0 ? j.plannedEnd()
                    : start + j.durationMinutes() * 60000L;
            String row = j.techId() == null || j.techId().length() == 0 ? UNASSIGNED_ROW
                    : j.techId();
            ScheduleItem item = new ScheduleItem(j.id(), j.title(), start, end)
                    .setResourceId(row).setDetail(j.siteName()).setUserObject(j)
                    .setColor(jobColor(j));
            boolean locked = "COMPLETED".equals(j.status()) || "ON_SITE".equals(j.status())
                    || "EN_ROUTE".equals(j.status());
            item.setMovable(!locked);
            item.setResizable(false);
            model.addItem(item);
        }
        for (TimeOffDto t : board.timeOff()) {
            if (!"APPROVED".equals(t.status())) {
                continue;
            }
            model.addItem(new ScheduleItem("off-" + t.id(), Lang.tr("Time off"), t.start(), t.end())
                    .setResourceId(t.techId()).setKind(ScheduleItem.Kind.BLOCKED)
                    .setMovable(false).setResizable(false));
        }
        // The meal breaks the plan placed: hatched like time off, but in the
        // amber of a pause, so a gap between two jobs reads as the break the
        // rules require and not as idle time to fill.
        if (board.breaks() != null) {
            int n = 0;
            for (BreakDto b : board.breaks()) {
                model.addItem(new ScheduleItem("break-" + (n++), Lang.tr("Break"), b.start(), b.end())
                        .setResourceId(b.techId()).setKind(ScheduleItem.Kind.BLOCKED)
                        .setColor(BREAK_COLOR).setMovable(false).setResizable(false));
            }
        }
        final Scheduler scheduler = new Scheduler(model);
        scheduler.setName("crewBoard");
        scheduler.setViews(Scheduler.View.RESOURCE_TIMELINE, Scheduler.View.RESOURCE_DAY);
        scheduler.setView(Scheduler.View.RESOURCE_TIMELINE);
        scheduler.setTimelineDays(Math.max(1, Math.min(board.days(), 5)));
        scheduler.setDate(firstWorkDay());
        scheduler.setInitialMinute(7 * 60);
        scheduler.setSnapMinutes(15);
        scheduler.set24Hours(true);
        scheduler.setEditable(true);
        scheduler.setDragEdits(true);
        scheduler.setOpenOnTap(true);
        scheduler.setCurrentTime(System.currentTimeMillis());
        scheduler.setCompactToolbar(true);
        scheduler.addSchedulerListener(new SchedulerAdapter() {
            @Override
            public void itemOpened(Scheduler s, ScheduleItem item) {
                if (item.getUserObject() instanceof JobDto) {
                    jobSheet((JobDto) item.getUserObject());
                }
            }

            @Override
            public boolean itemChanging(Scheduler s, ScheduleItem item, long start, long end,
                    String resourceId) {
                if (!(item.getUserObject() instanceof JobDto)) {
                    return false;
                }
                String to = UNASSIGNED_ROW.equals(resourceId) ? "" : resourceId;
                propose((JobDto) item.getUserObject(), to, start);
                return true;
            }
        });
        return scheduler;
    }

    // ---- Gantt: routes with the last run as baseline ------------------------

    private Component gantt() {
        ProjectModel model = new ProjectModel();
        model.setStart(board.dayStart());
        Map<String, Long> before = new HashMap<String, Long>();
        if (board.lastRun() != null && board.lastRun().changes() != null) {
            for (ChangeDto c : board.lastRun().changes()) {
                if (c.fromStart() > 0) {
                    before.put(c.jobId(), Long.valueOf(c.fromStart()));
                }
            }
        }
        Map<String, GanttTask> routes = new HashMap<String, GanttTask>();
        for (TechnicianDto t : board.technicians()) {
            model.addResource(new ScheduleResource(t.id(), t.name()).setColor(color(t)));
            GanttTask route = new GanttTask("route-" + t.id(), t.name());
            route.setColor(color(t));
            route.setManuallyScheduled(true);
            routes.put(t.id(), route);
        }
        for (TechnicianDto t : board.technicians()) {
            boolean any = false;
            for (JobDto j : board.jobs()) {
                if (t.id().equals(j.techId()) && j.plannedStart() > 0) {
                    any = true;
                    break;
                }
            }
            if (any) {
                model.addTask(routes.get(t.id()));
            }
        }
        for (JobDto j : board.jobs()) {
            GanttTask route = routes.get(j.techId());
            if (route == null || j.plannedStart() <= 0 || !model.contains(route)) {
                continue;
            }
            GanttTask task = new GanttTask(j.id(), j.title());
            task.setManuallyScheduled(true);
            task.setDates(j.plannedStart(), j.plannedEnd());
            task.setColor(jobColor(j));
            task.setPriority(j.priority() * 200);
            task.setPercentComplete("COMPLETED".equals(j.status()) ? 100
                    : "ON_SITE".equals(j.status()) ? 50 : 0);
            // Not assigned to the technician as a resource: the chart writes
            // the assignee after every bar, and under a route that already
            // carries the technician's name it repeated it on every job.
            task.setUserObject(j);
            Long was = before.get(j.id());
            if (was != null) {
                task.setBaseline(was.longValue(), was.longValue() + j.durationMinutes() * 60000L);
            }
            model.addTask(route, task);
            // A route is scheduled manually, so nothing rolls its children
            // up into it: left alone it stays at 0, and the time axis then
            // starts in January 1970 with every bar a sliver at the far end.
            if (route.getStart() <= 0 || j.plannedStart() < route.getStart()) {
                route.setDates(j.plannedStart(), Math.max(route.getEnd(), j.plannedEnd()));
            } else if (j.plannedEnd() > route.getEnd()) {
                route.setDates(route.getStart(), j.plannedEnd());
            }
        }
        Gantt gantt = new Gantt(model);
        gantt.setName("gantt");
        gantt.setEditable(true);
        gantt.setDragEdits(true);
        gantt.setShowBaseline(!before.isEmpty());
        gantt.setShowLinks(false);
        gantt.setLoadVisible(false);
        gantt.setTableVisible(Layouts.wide());
        gantt.set24Hours(true);
        gantt.setCurrentTime(System.currentTimeMillis());
        gantt.addGanttListener(new GanttAdapter() {
            @Override
            public void taskOpened(Gantt g, GanttTask task) {
                if (task.getUserObject() instanceof JobDto) {
                    jobSheet((JobDto) task.getUserObject());
                }
            }

            @Override
            public boolean taskChanging(Gantt g, GanttTask task, long start, long end) {
                if (!(task.getUserObject() instanceof JobDto)) {
                    return false;
                }
                JobDto j = (JobDto) task.getUserObject();
                return !"COMPLETED".equals(j.status()) && !"ON_SITE".equals(j.status());
            }

            @Override
            public void taskChanged(Gantt g, GanttTask task) {
                if (task.getUserObject() instanceof JobDto) {
                    JobDto j = (JobDto) task.getUserObject();
                    propose(j, j.techId(), task.getStart());
                }
            }
        });
        // Opens on the first morning with work, as the timeline does: on a
        // weekend "now" is an empty stretch of chart.
        // scrollToDate puts the time a quarter of the way in, about five
        // hours at this zoom: the first job plus three lands its morning
        // at the left edge.
        final long morning = firstWorkDay() + 3 * 60 * 60000L;
        gantt.scrollToDate(morning);
        // The chart opens at a scale that fits weeks, where a 90-minute job is
        // a sliver. Zooming needs the chart's width, so it waits for the
        // layout; five steps show a working day across a phone.
        final Gantt chart = gantt;
        CN.callSerially(() -> {
            for (int i = 0; i < 5; i++) {
                chart.zoomIn();
            }
            chart.scrollToDate(morning);
        });
        Container box = new Container(new BorderLayout());
        if (!before.isEmpty()) {
            box.add(BorderLayout.NORTH, Ui.label(Lang.tr("Grey bars: where the last run "
                    + "moved jobs from"), "FlBoardHint"));
        }
        box.add(BorderLayout.CENTER, gantt);
        return box;
    }

    // ---- Map: the crew, live ------------------------------------------------

    private Component crewMap() {
        map = Maps.create(new LatLng(45.5152, -122.6784), Maps.CITY_ZOOM);
        people.clear();
        open.clear();
        List<LatLng> all = new ArrayList<LatLng>();
        for (TechnicianDto t : board.technicians()) {
            double lat = t.lat() != 0 ? t.lat() : t.homeLat();
            double lng = t.lng() != 0 ? t.lng() : t.homeLng();
            people.put(t.id(), person(t, lat, lng));
            all.add(new LatLng(lat, lng));
        }
        long dayEnd = board.dayStart() + 24L * 3600000L;
        for (final JobDto j : board.jobs()) {
            boolean waiting = j.techId() == null || j.techId().length() == 0;
            boolean today = j.plannedStart() > 0 && j.plannedStart() < dayEnd;
            if (!waiting && !today) {
                continue;
            }
            LatLng at = new LatLng(j.lat(), j.lng());
            int color = waiting ? 0xe8590c : jobColor(j);
            open.add(Maps.stop(map, at, waiting ? "!" : String.valueOf(j.sequence() + 1), color,
                    "COMPLETED".equals(j.status()), false, j.title(), e -> jobSheet(j)));
        }
        Maps.show(map, all.toArray(new LatLng[all.size()]));
        Container legend = new Container(new FlowLayout());
        legend.setUIID("FlLegend");
        legend.add(Ui.chip(Lang.tr("Technicians"), FontImage.MATERIAL_PERSON_PIN_CIRCLE));
        legend.add(Ui.chip(Lang.tr("Unassigned"), FontImage.MATERIAL_ERROR_OUTLINE));
        Container box = new Container(new BorderLayout());
        box.add(BorderLayout.CENTER, map.asComponent());
        box.add(BorderLayout.SOUTH, legend);
        return box;
    }

    private Marker person(final TechnicianDto t, double lat, double lng) {
        return Maps.person(map, new LatLng(lat, lng), t.initials(), color(t), t.name(),
                e -> techSheet(t));
    }

    // ---- Sheets -------------------------------------------------------------

    private void jobSheet(final JobDto j) {
        Container peek = panel();
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Ui.label(j.title(), "FlSheetTitle"));
        head.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_CLOSE, "closeSheet",
                e -> closeSheet()));
        peek.add(head);
        peek.add(Ui.label(j.siteName() + Ui.DOT + j.address(), "FlSheetDetail"));
        Container chips = new Container(new FlowLayout());
        chips.add(Ui.badge(Ui.status(j.status()), Ui.statusBadge(j.status())));
        chips.add(Ui.badge(Lang.tr("Priority {0}", String.valueOf(j.priority())),
                j.priority() >= 4 ? "FlBadgeUrgent" : "FlBadge"));
        for (String skill : j.skills()) {
            chips.add(Ui.badge(skill, "FlBadgeKind"));
        }
        peek.add(chips);
        if (j.plannedStart() > 0) {
            peek.add(Ui.row(FontImage.MATERIAL_PERSON, j.techName(), Ui.when(j.plannedStart())
                    + " - " + Ui.clock(j.plannedEnd()) + Ui.DOT
                    + Lang.tr("{0} drive before", Ui.duration(j.travelBefore())), null));
        }
        peek.add(Ui.row(FontImage.MATERIAL_SCHEDULE, Lang.tr("Customer window"),
                Ui.when(j.windowStart()) + " - " + Ui.clock(j.windowEnd()), null));
        if (j.description() != null && j.description().length() > 0) {
            peek.add(Ui.text(j.description(), "FlCardText"));
        }
        reveal();
    }

    private void techSheet(final TechnicianDto t) {
        Container peek = panel();
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.WEST, Ui.avatar(t.name()));
        Container words = new Container(BoxLayout.y());
        words.add(Ui.label(t.name(), "FlSheetTitle"));
        words.add(Ui.label(techStatus(t.status()) + Ui.DOT + Lang.count(t.jobsToday(),
                "1 job today", "{0} jobs today"), "FlSheetDetail"));
        head.add(BorderLayout.CENTER, words);
        head.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_CLOSE, "closeSheet",
                e -> closeSheet()));
        peek.add(head);
        Container skills = new Container(new FlowLayout());
        for (String s : t.skills()) {
            skills.add(Ui.badge(s, "FlBadgeKind"));
        }
        peek.add(skills);
        Button absent = Ui.danger("Out today: cover their jobs", "markAbsent", e -> {
            long now = System.currentTimeMillis();
            long end = board.dayStart() + 24L * 3600000L;
            busy = true;
            Api.dispatch().absence(new com.codenameone.examples.fieldline.api.AbsenceDto(t.id(),
                    now, end, "SICK"), Net.to(run -> {
                        busy = false;
                        Telemetry.absenceCovered();
                        showRun(run);
                        load();
                    }, (status, message) -> {
                        busy = false;
                        Ui.fail(status, message);
                    }));
        });
        peek.add(absent);
        reveal();
    }

    /// Where something picked is shown, emptied: the sheet's peek on a
    /// phone, the inspector on a wide board.
    private Container panel() {
        if (desk) {
            inspector.removeAll();
            shown = true;
            return inspector;
        }
        sheet.reset();
        return sheet.peek();
    }

    private void reveal() {
        if (desk) {
            Layouts.dress(inspector);
        } else {
            sheet.setHidden(false);
            sheet.ready(true);
        }
        form.revalidate();
    }

    private void closeSheet() {
        if (desk) {
            shown = false;
            overview();
            return;
        }
        sheet.reset();
        sheet.setHidden(true);
        form.revalidate();
    }

    /// The inspector with nothing picked: what wants the dispatcher's
    /// attention -- requests waiting, the jobs nobody has, what the planner
    /// did last.
    private void overview() {
        inspector.removeAll();
        if (board == null) {
            return;
        }
        inspector.add(Ui.label("Needs attention", "FlInspectorTitle"));
        int pending = 0;
        for (TimeOffDto t : board.timeOff()) {
            if ("PENDING".equals(t.status())) {
                pending++;
            }
        }
        if (pending > 0) {
            Container row = Ui.row(FontImage.MATERIAL_BEACH_ACCESS,
                    Lang.tr("Time-off requests"),
                    Lang.tr("{0} waiting", String.valueOf(pending)), Ui.badge(
                            Lang.tr("Review"), "FlBadgeWarn"));
            row.setUIID("FlBanner");
            inspector.add(Ui.tap(row, "reviewTimeOff", e -> reviewTimeOff.run()));
        }
        Ui.section(inspector, "Unassigned");
        int waiting = 0;
        for (final JobDto j : board.jobs()) {
            if ((j.techId() == null || j.techId().length() == 0)
                    && !"CANCELLED".equals(j.status())) {
                waiting++;
                // Where and when, then why the planner left it: the one thing
                // to fix before it can go. The reason alone left several "No
                // cooling on 4th floor" rows that could not be told apart.
                String where = j.siteName() + Ui.DOT + Ui.when(j.windowStart());
                String why = j.unassignedReason() == null || j.unassignedReason().length() == 0
                        ? where : where + "\n" + Lang.tr(j.unassignedReason());
                inspector.add(Ui.tap(Ui.row(FontImage.MATERIAL_ERROR_OUTLINE, j.title(),
                        why, null),
                        "waiting-" + j.id(), e -> jobSheet(j)));
            }
        }
        if (waiting == 0) {
            inspector.add(Ui.label("Every job has a technician", "WlMuted"));
        }
        if (board.lastRun() != null) {
            Ui.section(inspector, "Last planner run");
            inspector.add(RunView.card(form, board.lastRun()));
        }
        inspector.add(Ui.text("Pick a job or a technician on the plan to see it here; drag "
                + "a job to propose a move.", "FlInspectorHint"));
        Layouts.dress(inspector);
        Ui.relayout(form, null);
    }

    // ---- Moves and the optimizer -------------------------------------------

    /// A drag on either view: checked by the server without changing
    /// anything, then shown with its consequences for the dispatcher to
    /// apply or put back.
    private void propose(final JobDto j, final String techId, final long start) {
        busy = true;
        Api.dispatch().move(new MoveDto(j.id(), techId, start, false), Net.to(check -> {
            busy = false;
            moveSheet(j, techId, start, check);
        }, (status, message) -> {
            busy = false;
            Ui.fail(status, message);
            render();
        }));
    }

    private void moveSheet(final JobDto j, final String techId, final long start,
            ValidationDto check) {
        Container peek = panel();
        Container head = new Container(new BorderLayout());
        Label icon = new Label("", check.ok() ? "FlMoveOk" : "FlMoveBad");
        Ui.icon(icon, check.ok() ? FontImage.MATERIAL_CHECK_CIRCLE : FontImage.MATERIAL_BLOCK, 4.5f);
        head.add(BorderLayout.WEST, icon);
        Container words = new Container(BoxLayout.y());
        words.add(Ui.label(check.ok() ? Lang.tr("Move {0}?", j.title())
                : Lang.tr("{0} cannot go there", j.title()), "FlSheetTitle"));
        words.add(Ui.text(check.message(), "FlSheetDetail"));
        head.add(BorderLayout.CENTER, words);
        peek.add(head);
        if (check.warnings() != null) {
            for (String w : check.warnings()) {
                Container row = Ui.row(FontImage.MATERIAL_WARNING_AMBER, w, null, null);
                row.setUIID("FlWarningRow");
                peek.add(row);
            }
        }
        Container buttons = new Container(new GridLayout(1, check.ok() ? 2 : 1));
        buttons.add(Ui.secondary("Put it back", "undoMove", e -> {
            closeSheet();
            render();
        }));
        if (check.ok()) {
            buttons.add(Ui.primary("Apply", "applyMove", e -> {
                busy = true;
                Api.dispatch().move(new MoveDto(j.id(), techId, start, true), Net.to(done -> {
                    busy = false;
                    Telemetry.jobMoved(true);
                    closeSheet();
                    Ui.say(Lang.tr("{0} moved", j.title()));
                    load();
                }, (status, message) -> {
                    busy = false;
                    Ui.fail(status, message);
                }));
            }));
        }
        peek.add(buttons);
        reveal();
    }

    private void optimize() {
        if (busy) {
            return;
        }
        busy = true;
        panel().add(Ui.row(FontImage.MATERIAL_AUTO_FIX_HIGH, "Optimizing the week...",
                "Adaptive search over every route, window and skill", null));
        reveal();
        Api.dispatch().optimize(Net.to(run -> {
            busy = false;
            Telemetry.optimized();
            showRun(run);
            load();
        }, (status, message) -> {
            busy = false;
            closeSheet();
            Ui.fail(status, message);
        }));
    }

    private void showRun(ScheduleRunDto run) {
        Container peek = panel();
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, RunView.card(form, run));
        peek.add(head);
        peek.add(Ui.secondary("Done", "closeRun", e -> closeSheet()));
        reveal();
    }

    private void runs() {
        Api.dispatch().runs(Net.to(list -> {
            Form screen = Ui.form("Planner runs", "Runs");
            Ui.back(screen, form);
            Container page = Ui.page();
            if (list.isEmpty()) {
                page.add(Ui.empty(FontImage.MATERIAL_HISTORY, "No runs yet",
                        "Every optimization and re-plan is kept here."));
            }
            for (final ScheduleRunDto r : list) {
                page.add(Ui.tap(Ui.row(FontImage.MATERIAL_AUTO_FIX_HIGH, RunView.kind(r.kind()),
                        Ui.when(r.at()) + Ui.DOT + r.summary(), null), "run-" + r.id(),
                        e -> RunForm.show(screen, r)));
            }
            screen.add(BorderLayout.CENTER, page);
            screen.show();
        }, Ui::fail));
    }

    // ---- Helpers ------------------------------------------------------------

    /// Gives a row of [Ui#choice] the app's own segments, which the console
    /// does not dress: a desktop theme's tabs are drawn at a phone's size.
    static void segments(Container choice) {
        choice.setUIID("FlViewSegments");
        for (int iter = 0; iter < choice.getComponentCount(); iter++) {
            choice.getComponentAt(iter).setUIID("FlViewSegment");
        }
    }

    private TechnicianDto tech(String id) {
        for (TechnicianDto t : board.technicians()) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    private static String techStatus(String status) {
        if ("OFF".equals(status)) {
            return Lang.tr("Off today");
        }
        if ("EN_ROUTE".equals(status)) {
            return Lang.tr("Driving");
        }
        if ("ON_SITE".equals(status)) {
            return Lang.tr("On site");
        }
        return Lang.tr("Available");
    }

    static int color(TechnicianDto t) {
        return com.codenameone.examples.fieldline.field.TodayForm.parseColor(t.color(), 0x2f6fed);
    }

    static int jobColor(JobDto j) {
        if ("COMPLETED".equals(j.status())) {
            return 0x8a94a3;
        }
        if (j.techId() == null || j.techId().length() == 0) {
            return 0xe8590c;
        }
        if (j.priority() >= 4) {
            return 0xd6336c;
        }
        if ("INSPECTION".equals(j.kind())) {
            return 0x0c8599;
        }
        if ("MAINTENANCE".equals(j.kind())) {
            return 0x5f3dc4;
        }
        if ("INSTALL".equals(j.kind())) {
            return 0x2b8a3e;
        }
        return 0x1c7ed6;
    }
}
