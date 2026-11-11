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
package com.codename1.ui.scheduler;

import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.LevelingResult;
import com.codename1.scheduling.ProjectChangeListener;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ResourceLeveler;
import com.codename1.scheduling.ResourceOverallocation;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/// A Gantt chart of a `ProjectModel`: the tree of tasks in a table on the
/// left, a bar for each against a time axis on the right, the links between
/// them, and under them how much of every resource the plan asks for.
///
/// ```java
/// WorkCalendar calendar = new WorkCalendar();
/// // A duration is working time: a day of it is the calendar's hours.
/// long day = calendar.getMillisPerDay();
/// ProjectModel project = new ProjectModel(calendar, System.currentTimeMillis());
/// GanttTask design = new GanttTask("design", "Design", 5 * day);
/// GanttTask build = new GanttTask("build", "Build", 10 * day);
/// project.addTask(design).addTask(build);
/// project.link(design, build);
/// project.update();
///
/// Form form = new Form("Plan", new BorderLayout());
/// form.add(BorderLayout.CENTER, new Gantt(project));
/// form.show();
/// ```
///
/// ![A Gantt chart with its critical path](https://www.codenameone.com/developer-guide/img/generated/gantt-critical-path.png)
///
/// The chart draws its rows and keeps no component for each, so a plan of
/// thousands of tasks scrolls as one of ten does. Time zooms with a pinch,
/// with the wheel and the control key held, or with `zoomIn`, `zoomOut` and
/// `zoomToFit`.
///
/// #### Editing
///
/// A bar is dragged to move it and by its end to change its duration. With
/// a mouse that is a plain drag; on a touch screen a drag scrolls, so a long
/// press picks the bar up. The selected task shows a marker under its bar,
/// dragged to set how much of it is done, and a dot after its end, dragged
/// onto another bar to make that one depend on it.
///
/// A task the project schedules is not placed by a drag: the drag sets a
/// "start no earlier than" constraint, and the task then starts there or
/// later if its predecessors say so. A manually scheduled task is given the
/// dates as they are. Every change is offered to the listeners first, which
/// may refuse it, and then the project is scheduled again and the bars slide
/// to where they now are. A summary task follows its children and is not
/// dragged. The table is not edited in place: a double tap is reported to
/// `GanttListener#taskOpened`, for an application's own editor.
///
/// Under a mouse the bar the pointer is over lights up together with the
/// links into and out of it, the row is marked in the table and in the
/// chart, the cursor says what a drag would do and a tooltip gives the
/// task's dates and progress. A bar that is picked up lifts over a shadow.
///
/// ![A bar moved and the tasks after it following, as four stopped frames](https://www.codenameone.com/developer-guide/img/generated/gantt-drag-stages.png)
///
/// `setAnimationTime` stops the clock these run by, for a screenshot or a
/// test of a moment in the middle of one.
///
/// #### Resources
///
/// `setLoadVisible(true)` shows the load of every resource under the chart.
/// A stretch over the resource's capacity is drawn in `GanttOverload`.
/// `level()` delays tasks until no resource is asked for more than it has,
/// as far as that can be done.
///
/// ![The load of each resource under the chart](https://www.codenameone.com/developer-guide/img/generated/gantt-load.png)
///
/// #### Styling
///
/// `GanttRow` is a row, with its selected style the selected one, and
/// `GanttSummaryRow` the row of a summary task; `GanttHeader` the time
/// axis and the titles of the table. The bars are `GanttBar`, with
/// `GanttBarProgress` the part done, `GanttBarCritical`, `GanttSummaryBar`
/// and `GanttMilestone`; `GanttBaseline` is the plan as it was captured.
/// `GanttLink` and `GanttLinkCritical` give the links their foreground
/// colour, `GanttLoad` and `GanttOverload` the resource load. A task that
/// names a UIID or a colour of its own is painted with that.
public class Gantt extends Container {
    /// The columns the table can show after the name of a task. Those that
    /// do not fit in less than half of the width are left out, last first.
    public enum Column {
        /// The day the task starts.
        START,
        /// The day the task ends.
        END,
        /// The working days the task takes.
        DURATION,
        /// How much of the task is done.
        PROGRESS
    }

    private ProjectModel model;
    private final GanttChart chart;
    private final ResourceLoadView load;
    private final BarLayout bar = new BarLayout();
    private final Container toolbar = new Container(bar);
    private final HashSet<GanttTask> collapsed = new HashSet<GanttTask>();
    private final ArrayList<GanttListener> listeners = new ArrayList<GanttListener>();
    private Column[] columns = {Column.START, Column.END};
    private GanttTask selected;
    private GanttBarRenderer renderer = new DefaultGanttBarRenderer();
    private boolean showCritical;
    private boolean showBaseline;
    private boolean showLinks = true;
    private boolean editable = true;
    private boolean loadVisible;
    private boolean tableVisible = true;
    private Boolean hours24;
    private int firstDayOfWeek = 1;
    private long fixedNow = -1;
    private List<ResourceOverallocation> overallocations;
    private CheckBox criticalToggle;
    private CheckBox baselineToggle;
    private boolean listening;

    private final ProjectChangeListener modelListener = new ProjectChangeListener() {
        @Override
        public void projectChanged(ProjectModel m) {
            changed();
        }
    };

    /// A chart with no project yet.
    public Gantt() {
        this(null);
    }

    public Gantt(ProjectModel model) {
        super(new BorderLayout());
        setUIID("Gantt");
        chart = new GanttChart(this);
        load = new ResourceLoadView(this, chart);
        chart.setLoadView(load);
        toolbar.setUIID("SchedulerToolbar");
        buildToolbar();
        add(BorderLayout.NORTH, toolbar);
        add(BorderLayout.CENTER, chart);
        add(BorderLayout.SOUTH, load);
        load.setHidden(true);
        this.model = model;
    }

    // ------------------------------------------------------------ toolbar

    private Button toolButton(String text, char icon, ActionListener action) {
        Button b = new Button(text == null ? "" : text, "SchedulerViewButton");
        if (icon != 0) {
            FontImage.setMaterialIcon(b, icon);
        }
        b.addActionListener(action);
        return b;
    }

    private CheckBox toggle(String text, ActionListener action) {
        CheckBox c = new CheckBox(text);
        c.setToggle(true);
        c.setUIID("SchedulerViewButton");
        c.addActionListener(action);
        return c;
    }

    private void buildToolbar() {
        Container zoom = new Container(BoxLayout.x());
        zoom.add(toolButton(null, FontImage.MATERIAL_ZOOM_OUT, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                zoomOut();
            }
        }));
        zoom.add(toolButton(null, FontImage.MATERIAL_ZOOM_IN, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                zoomIn();
            }
        }));
        zoom.add(toolButton(null, FontImage.MATERIAL_FIT_SCREEN, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                zoomToFit();
            }
        }));
        Container options = new Container(BoxLayout.x());
        criticalToggle = toggle(localize("gantt.critical", "Critical path"), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setShowCriticalPath(criticalToggle.isSelected());
            }
        });
        baselineToggle = toggle(localize("gantt.baseline", "Baseline"), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setShowBaseline(baselineToggle.isSelected());
            }
        });
        options.add(criticalToggle);
        options.add(baselineToggle);
        options.add(toolButton(localize("gantt.level", "Level"), (char) 0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                level();
            }
        }));
        toolbar.add(zoom);
        toolbar.add(options);
    }

    /// The bar is a row where there is room for one, and two rows on a
    /// screen as narrow as a phone's. It is decided here because this is
    /// the first place the width is known.
    @Override
    public void layoutContainer() {
        if (bar.fit(toolbar, getWidth() - getStyle().getHorizontalPadding())) {
            toolbar.setShouldCalcPreferredSize(true);
        }
        super.layoutContainer();
    }

    /// Whether the bar of zoom buttons and options is shown above the chart.
    public boolean isToolbarVisible() {
        return !toolbar.isHidden();
    }

    public void setToolbarVisible(boolean v) {
        toolbar.setHidden(!v);
        revalidateLater();
    }

    // -------------------------------------------------------------- model

    public ProjectModel getModel() {
        return model;
    }

    /// Shows another project, or none when `m` is null.
    public void setModel(ProjectModel m) {
        if (model == m) { //NOPMD CompareObjectsWithEquals - the same model object, not an equal one
            return;
        }
        // A bar that is held belongs to the project that is leaving.
        chart.cancelEdit();
        if (model != null && listening) {
            model.removeChangeListener(modelListener);
        }
        model = m;
        if (model != null && listening) {
            model.addChangeListener(modelListener);
        }
        selected = null;
        collapsed.clear();
        changed();
    }

    /// Draws the project again, for a model changed in a way it could not
    /// report itself.
    public void refresh() {
        changed();
    }

    private void changed() {
        overallocations = null;
        if (selected != null && (model == null || !model.getTasks().contains(selected))) {
            selected = null;
        }
        chart.modelChanged();
        load.modelChanged();
        syncLoad();
    }

    // The load is given no room while there is no resource to show in it.
    private void syncLoad() {
        boolean show = loadVisible && model != null && !model.getResources().isEmpty();
        if (load.isHidden() == show) {
            load.setHidden(!show);
            load.setShouldCalcPreferredSize(true);
            revalidateLater();
        } else if (show) {
            load.setShouldCalcPreferredSize(true);
        }
    }

    // The listener is held only while the chart is on a form, so that a
    // chart no longer shown is not kept alive by its project.
    @Override
    protected void initComponent() {
        super.initComponent();
        if (!listening) {
            listening = true;
            if (model != null) {
                model.addChangeListener(modelListener);
            }
            changed();
        }
    }

    @Override
    protected void deinitialize() {
        if (listening) {
            listening = false;
            if (model != null) {
                model.removeChangeListener(modelListener);
            }
        }
        super.deinitialize();
    }

    boolean isOverallocated(String resourceId) {
        if (model == null) {
            return false;
        }
        if (overallocations == null) {
            overallocations = ResourceLeveler.findOverallocations(model);
        }
        for (ResourceOverallocation over : overallocations) {
            if (over.getResourceId().equals(resourceId)) {
                return true;
            }
        }
        return false;
    }

    /// Delays tasks until no resource is asked for more than its capacity,
    /// as far as the plan allows, and schedules the project again. What
    /// could not be resolved is in the result, as is the finish of the
    /// project before and after. Returns null when there is no project.
    public LevelingResult level() {
        if (model == null) {
            return null;
        }
        LevelingResult result = ResourceLeveler.level(model);
        if (result.isChanged()) {
            chart.layoutNow();
            chart.beginReflow();
            result.apply();
        }
        return result;
    }

    // ------------------------------------------------------------ options

    /// Whether the tasks and links with no slack are drawn as critical.
    public boolean isShowCriticalPath() {
        return showCritical;
    }

    public void setShowCriticalPath(boolean v) {
        showCritical = v;
        criticalToggle.setSelected(v);
        chart.repaint();
    }

    /// Whether the captured baseline of every task is drawn under its bar.
    public boolean isShowBaseline() {
        return showBaseline;
    }

    public void setShowBaseline(boolean v) {
        showBaseline = v;
        baselineToggle.setSelected(v);
        chart.repaint();
    }

    /// Whether the links between tasks are drawn. They are by default.
    public boolean isShowLinks() {
        return showLinks;
    }

    public void setShowLinks(boolean v) {
        showLinks = v;
        chart.repaint();
    }

    /// Whether bars can be dragged, progress set and links made.
    @Override
    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean v) {
        editable = v;
        chart.cancelEdit();
        chart.repaint();
    }

    /// Whether a bar is moved by a plain drag, as with a mouse, or only
    /// after a long press, which leaves a plain drag to scroll. The default
    /// follows the device.
    public void setDragEdits(boolean v) {
        chart.setDragEdits(v);
    }

    /// Whether the load of the resources is shown under the chart.
    public boolean isLoadVisible() {
        return loadVisible;
    }

    public void setLoadVisible(boolean v) {
        loadVisible = v;
        syncLoad();
    }

    /// Whether the table of tasks is shown beside the chart.
    public boolean isTableVisible() {
        return tableVisible;
    }

    public void setTableVisible(boolean v) {
        tableVisible = v;
        chart.modelChanged();
        load.modelChanged();
    }

    /// The columns of the table after the name, in order.
    public void setColumns(Column... c) {
        columns = new Column[c == null ? 0 : c.length];
        if (c != null) {
            System.arraycopy(c, 0, columns, 0, c.length);
        }
        chart.modelChanged();
    }

    Column[] columns() {
        return columns;
    }

    public boolean is24Hours() {
        return hours24 == null ? Dates.deviceUses24Hours() : hours24.booleanValue();
    }

    public void set24Hours(boolean v) {
        hours24 = v ? Boolean.TRUE : Boolean.FALSE;
        chart.modelChanged();
    }

    /// The ISO day weeks start on in the time axis: 1 is Monday.
    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    public void setFirstDayOfWeek(int isoDay) {
        if (isoDay < 1 || isoDay > 7) {
            throw new IllegalArgumentException("isoDay");
        }
        firstDayOfWeek = isoDay;
        chart.modelChanged();
    }

    public GanttBarRenderer getBarRenderer() {
        return renderer;
    }

    public void setBarRenderer(GanttBarRenderer r) {
        if (r == null) {
            throw new IllegalArgumentException("renderer");
        }
        renderer = r;
        chart.repaint();
    }

    /// Fixes what the chart takes the current time to be, for a screenshot
    /// or a test. A negative value returns to the clock.
    public void setCurrentTime(long millis) {
        fixedNow = millis;
        chart.modelChanged();
    }

    /// Stops the clock the chart animates by at a time, for a screenshot of
    /// an animation or a test of one: the light of a hover, the rise of
    /// an item that is picked up and the slide of
    /// the bars to their new dates are drawn as far along as that time
    /// makes them. A negative value returns to the clock.
    ///
    /// ```java
    /// gantt.setAnimationTime(1000);  // nothing has started
    /// // ... press, drag, release ...
    /// gantt.setAnimationTime(1100);  // 100 milliseconds into what followed
    /// ```
    public void setAnimationTime(long millis) {
        chart.setFixedClock(millis);
        load.setFixedClock(millis);
    }

    long now() {
        return fixedNow >= 0 ? fixedNow : System.currentTimeMillis();
    }

    String localize(String key, String value) {
        return UIManager.getInstance().localize(key, value);
    }

    // --------------------------------------------------------------- tree

    /// Whether the children of a summary task are hidden.
    public boolean isCollapsed(GanttTask task) {
        return collapsed.contains(task);
    }

    public void setCollapsed(GanttTask task, boolean v) {
        boolean did = v ? collapsed.add(task) : collapsed.remove(task);
        if (did) {
            chart.modelChanged();
        }
    }

    public void expandAll() {
        collapsed.clear();
        chart.modelChanged();
    }

    public void collapseAll() {
        if (model == null) {
            return;
        }
        List<GanttTask> all = model.getTasks();
        for (GanttTask each : all) {
            if (each.isSummary()) {
                collapsed.add(each);
            }
        }
        chart.modelChanged();
    }

    // --------------------------------------------------------------- view

    public void zoomIn() {
        chart.zoom(1.5f);
    }

    public void zoomOut() {
        chart.zoom(1 / 1.5f);
    }

    /// Shows the whole project in the width there is.
    public void zoomToFit() {
        chart.zoomToFit();
    }

    /// Brings a task's row and the start of its bar into view, opening the
    /// summary tasks it is under.
    public void scrollToTask(GanttTask task) {
        for (GanttTask p = task.getParent(); p != null; p = p.getParent()) {
            collapsed.remove(p);
        }
        chart.modelChanged();
        chart.scrollToTask(task);
    }

    public void scrollToDate(long time) {
        chart.scrollToTime(time);
    }

    // ---------------------------------------------------------- selection

    public GanttTask getSelectedTask() {
        return selected;
    }

    /// Selects a task, or nothing when `task` is null. The listeners are
    /// not told: this is the application's own doing.
    public void setSelectedTask(GanttTask task) {
        selected = task;
        chart.repaint();
    }

    public void addGanttListener(GanttListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeGanttListener(GanttListener l) {
        listeners.remove(l);
    }

    // The listeners as they are now: one that removes itself while it is
    // told must not make the next one miss the event, or its say on it.
    private GanttListener[] told() {
        return listeners.toArray(new GanttListener[listeners.size()]);
    }

    void viewSelected(GanttTask task) {
        if (selected == task) { //NOPMD CompareObjectsWithEquals - the selection is this very object
            return;
        }
        selected = task;
        for (GanttListener l : told()) {
            l.taskSelected(this, task);
        }
        chart.repaint();
    }

    void viewOpened(GanttTask task) {
        for (GanttListener l : told()) {
            l.taskOpened(this, task);
        }
    }

    boolean viewChanging(GanttTask task, long start, long end) {
        for (GanttListener l : told()) {
            if (!l.taskChanging(this, task, start, end)) {
                return false;
            }
        }
        return true;
    }

    void viewChanged(GanttTask task) {
        for (GanttListener l : told()) {
            l.taskChanged(this, task);
        }
    }

    boolean viewLinking(GanttTask predecessor, GanttTask successor) {
        for (GanttListener l : told()) {
            if (!l.linkCreating(this, predecessor, successor)) {
                return false;
            }
        }
        return true;
    }

    GanttChart surface() {
        return chart;
    }

    ResourceLoadView loadSurface() {
        return load;
    }
}
