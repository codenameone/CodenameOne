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
package com.codename1.ui.chart;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

/// An interactive, animated chart.
///
/// ```java
/// Chart chart = new Chart()
///         .setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"))
///         .setTitle("Revenue");
/// chart.addSeries(new BarSeries("2025").setValues(12, 18, 9, 22));
/// chart.addSeries(new LineSeries("Target").setDashed(true).setValues(15, 15, 15, 15));
/// form.add(BorderLayout.CENTER, chart);
/// ```
///
/// Layout is automatic: the chart measures every label before it draws, picks as many
/// round-number ticks as fit, thins and shortens category labels that would collide and
/// sizes the plot to whatever space is left. Data labels go through a collision layout and
/// are dropped rather than drawn over each other.
///
/// Interaction works the same with touch, mouse, trackpad and keyboard: drag to pan once
/// zoomed, pinch or scroll to zoom (spreading the fingers horizontally zooms only the x
/// axis), double tap to zoom in and double tap again to reset, tap or hover a point for its
/// tooltip, and tap a legend entry to hide or show its series. Data changes animate.
///
/// Style the component with the `Chart` UIID; its padding surrounds the plot, its font is
/// the label font and its background shows through. Colours come from `ChartTheme`.
public class Chart extends Component {
    /// No legend.
    public static final int LEGEND_NONE = 0;
    /// Legend above the plot.
    public static final int LEGEND_TOP = 1;
    /// Legend below the plot.
    public static final int LEGEND_BOTTOM = 2;
    /// Legend right of the plot.
    public static final int LEGEND_RIGHT = 3;

    /// No tooltip.
    public static final int TOOLTIP_NONE = 0;
    /// Tooltip for the single point under the finger or pointer.
    public static final int TOOLTIP_POINT = 1;
    /// Tooltip listing every series at the x nearest the finger or pointer, with a crosshair.
    public static final int TOOLTIP_SHARED = 2;

    private final List<Series> series = new ArrayList<Series>();
    private final List<Annotation> annotations = new ArrayList<Annotation>();
    private final List<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();
    private final List<RangeListener> rangeListeners = new ArrayList<RangeListener>();
    private boolean brushEnabled;
    private boolean brushZooms = true;
    private int brushFrom = -1;
    private int brushTo = -1;
    private Axis xAxis = new NumberAxis(Axis.BOTTOM);
    private Axis yAxis = new NumberAxis(Axis.LEFT);
    private Axis secondaryYAxis;
    private ChartTheme theme;
    private boolean themeSet;
    private String title;
    private int legendPosition = LEGEND_BOTTOM;
    private int tooltipMode = TOOLTIP_POINT;
    private boolean rotated;
    private boolean zoomEnabled = true;
    private int animationDuration = 450;
    private String emptyText = "No data";
    private String loadingText;
    private TooltipRenderer tooltipRenderer;

    // transition state
    private long transitionStart;
    private int transitionLength;
    private boolean inBatch;
    private boolean captured;
    private boolean entryPending = true;
    private double[] axisFrom;
    private ChartContext lastContext;

    // interaction state
    private ChartPoint selected;
    private ChartPoint hover;
    private int sharedIndex = -1;
    private int pressX;
    private int pressY;
    private int lastX;
    private int lastY;
    private long lastMoveTime;
    private float velocityX;
    private float velocityY;
    private static final int DRAG_UNDECIDED = 0;
    private static final int DRAG_PAN = 1;
    private static final int DRAG_SCRUB = 2;
    private static final int DRAG_PASS = 3;
    private static final int DRAG_BRUSH = 4;
    private int dragMode;
    private boolean pinching;
    private List<Chart> syncGroup = new ArrayList<Chart>();
    private boolean syncing;
    private double pinchDistX;
    private double pinchDistY;
    private int pinchCenterX;
    private int pinchCenterY;
    private long lastTapTime;
    private int lastTapX;
    private int lastTapY;
    private boolean flinging;
    private long flingLast;
    private final List<int[]> legendHits = new ArrayList<int[]>();
    private double[] viewportFrom;
    private double[] viewportTo;
    private long viewportStart;

    /// Creates an empty chart with numeric axes.
    public Chart() {
        setUIID("Chart");
        setFocusable(true);
        syncGroup.add(this);
    }

    /// Converts millimetres to pixels.
    static int mm(float mm) {
        return CN.convertToPixels(mm);
    }

    // ------------------------------------------------------------------ model

    /// Adds a series, animating it in when the chart is showing.
    public Chart addSeries(Series s) {
        beginChange();
        s.chart = this;
        series.add(s);
        s.visibility = s.isVisible() ? 1 : 0;
        if (isInitialized()) {
            s.captureEntryStart();
        }
        if (!inBatch) {
            startTransition();
        }
        return this;
    }

    /// Removes a series.
    public Chart removeSeries(Series s) {
        beginChange();
        series.remove(s);
        s.chart = null;
        if (!inBatch) {
            startTransition();
        }
        return this;
    }

    /// Removes every series.
    public Chart clearSeries() {
        for (Series s : series) {
            s.chart = null;
        }
        series.clear();
        repaint();
        return this;
    }

    /// The series, in paint order.
    public List<Series> getSeries() {
        return series;
    }

    /// The position of a series, which also selects its default palette colour.
    public int indexOf(Series s) {
        return series.indexOf(s);
    }

    /// Adds a reference line, band or marker.
    public Chart addAnnotation(Annotation a) {
        annotations.add(a);
        repaint();
        return this;
    }

    /// Removes an annotation.
    public Chart removeAnnotation(Annotation a) {
        annotations.remove(a);
        repaint();
        return this;
    }

    /// Runs several data changes as one animated transition.
    public void update(Runnable changes) {
        beginChange();
        boolean outer = !inBatch;
        inBatch = true;
        try {
            changes.run();
        } finally {
            if (outer) {
                inBatch = false;
                startTransition();
            }
        }
    }

    /// The x (category) axis.
    public Axis getXAxis() {
        return xAxis;
    }

    /// Replaces the x axis, for example with a `CategoryAxis` or `TimeAxis`.
    public Chart setXAxis(Axis a) {
        xAxis = a;
        repaint();
        return this;
    }

    /// The primary value axis.
    public Axis getYAxis() {
        return yAxis;
    }

    /// Replaces the primary value axis, for example with a `LogAxis`.
    public Chart setYAxis(Axis a) {
        yAxis = a;
        repaint();
        return this;
    }

    /// The secondary value axis on the right, or null.
    public Axis getSecondaryYAxis() {
        return secondaryYAxis;
    }

    /// Adds a right hand value axis for series with `setSecondaryAxis(true)`.
    public Chart setSecondaryYAxis(Axis a) {
        secondaryYAxis = a;
        if (a != null) {
            a.setPosition(Axis.RIGHT).setGridVisible(false);
        }
        repaint();
        return this;
    }

    /// The colour theme.
    public ChartTheme getTheme() {
        if (theme == null) {
            theme = ChartTheme.fromStyle(getStyle()).applyUIIDs(getUIManager());
        }
        return theme;
    }

    /// Replaces the colour theme.
    public Chart setTheme(ChartTheme theme) {
        this.theme = theme;
        themeSet = theme != null;
        repaint();
        return this;
    }

    /// The chart title, or null.
    public String getTitle() {
        return title;
    }

    /// Sets a title drawn above the plot.
    public Chart setTitle(String title) {
        this.title = title;
        repaint();
        return this;
    }

    /// One of the `LEGEND_` constants. The default is below the plot; a chart with a single
    /// series and no pie hides its legend automatically.
    public Chart setLegendPosition(int pos) {
        legendPosition = pos;
        repaint();
        return this;
    }

    /// One of the `TOOLTIP_` constants.
    public Chart setTooltipMode(int mode) {
        tooltipMode = mode;
        return this;
    }

    /// Customizes the tooltip's rows.
    public Chart setTooltipRenderer(TooltipRenderer r) {
        tooltipRenderer = r;
        return this;
    }

    /// True for a horizontal bar chart: the x (category) axis runs down the left side.
    public boolean isRotated() {
        return rotated;
    }

    /// Swaps the axes so categories run vertically and values horizontally.
    public Chart setRotated(boolean rotated) {
        this.rotated = rotated;
        repaint();
        return this;
    }

    /// Whether pinch, wheel, double tap and drag zoom and pan the chart.
    public Chart setZoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
        return this;
    }

    /// Transition length in milliseconds; 0 applies data changes without animating.
    public Chart setAnimationDuration(int ms) {
        animationDuration = Math.max(0, ms);
        return this;
    }

    /// Shows that data is on its way: with no data yet the text replaces the empty state, and over
    /// existing data the chart is dimmed under it until the new data arrives.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text to show, or null to stop showing it
    ///
    /// #### Returns
    ///
    /// this chart
    public Chart setLoading(String text) {
        loadingText = text;
        repaint();
        return this;
    }

    /// Whether `setLoading` is showing a loading state.
    public boolean isLoading() {
        return loadingText != null;
    }

    /// Makes a drag inside the plot select a range of x values instead of scrubbing or panning;
    /// pinch, the wheel and the keyboard still zoom and pan. The selection is reported to
    /// `addRangeListener` and, unless `setBrushZooms(false)`, the chart zooms to it.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: true to brush
    ///
    /// #### Returns
    ///
    /// this chart
    public Chart setBrushEnabled(boolean enabled) {
        brushEnabled = enabled;
        return this;
    }

    /// Whether a brush selection also zooms the chart to it; true by default.
    ///
    /// #### Parameters
    ///
    /// - `zooms`: false to only report the selection
    ///
    /// #### Returns
    ///
    /// this chart
    public Chart setBrushZooms(boolean zooms) {
        brushZooms = zooms;
        return this;
    }

    /// Adds a listener for brush selections.
    public void addRangeListener(RangeListener l) {
        rangeListeners.add(l);
    }

    /// Removes a brush selection listener.
    public void removeRangeListener(RangeListener l) {
        rangeListeners.remove(l);
    }

    /// Text shown when there is nothing to plot.
    public Chart setEmptyText(String text) {
        emptyText = text;
        repaint();
        return this;
    }

    /// Listens for taps on data points.
    public void addSelectionListener(SelectionListener l) {
        selectionListeners.add(l);
    }

    /// Stops listening.
    public void removeSelectionListener(SelectionListener l) {
        selectionListeners.remove(l);
    }

    /// The selected point, or null.
    public ChartPoint getSelection() {
        return selected;
    }

    /// The frame last painted; hit testing and transitions read its geometry.
    public ChartContext getLastContext() {
        return lastContext;
    }

    /// Renders the chart into an image, for sharing or export.
    public Image toImage(int width, int height) {
        Image img = Image.createImage(width, height, 0xff000000 | getStyle().getBgColor());
        Graphics g = img.getGraphics();
        ChartContext keepContext = lastContext;
        List<int[]> keepHits = new ArrayList<int[]>(legendHits);
        int ox = getX();
        int oy = getY();
        int ow = getWidth();
        int oh = getHeight();
        setX(0);
        setY(0);
        setWidth(width);
        setHeight(height);
        try {
            paintChart(g, 0, 0, width, height);
        } finally {
            setX(ox);
            setY(oy);
            setWidth(ow);
            setHeight(oh);
            lastContext = keepContext;
            legendHits.clear();
            legendHits.addAll(keepHits);
        }
        return img;
    }

    // ------------------------------------------------------------------ transitions

    /// Snapshots what is on screen as the start of the next transition. Called by series
    /// before they mutate; applications use `update(Runnable)` instead.
    public void beginChange() {
        if (captured) {
            return;
        }
        captured = true;
        if (!isInitialized() || entryPending) {
            return;
        }
        for (Series s : series) {
            s.captureTransitionStart();
            s.visFrom = s.visibility;
        }
        Axis[] axes = valueAxes();
        axisFrom = new double[axes.length * 2 + 2];
        axisFrom[0] = xAxis.getVisibleMin();
        axisFrom[1] = xAxis.getVisibleMax();
        for (int i = 0; i < axes.length; i++) {
            axisFrom[2 + i * 2] = axes[i].getVisibleMin();
            axisFrom[3 + i * 2] = axes[i].getVisibleMax();
        }
        if (xAxis.isZoomed() || yAxis.isZoomed()) {
            // the user's viewport stays put; only the data tweens
            axisFrom = null;
        }
    }

    void seriesDataChanged(Series s) {
        if (!inBatch) {
            startTransition();
        }
    }

    void visibilityChanged(Series s) {
        beginChange();
        if (!inBatch) {
            startTransition();
        }
    }

    private void startTransition() {
        captured = false;
        if (!isInitialized() || animationDuration == 0) {
            finishTransition();
            repaint();
            return;
        }
        transitionStart = System.currentTimeMillis();
        transitionLength = animationDuration;
        repaint();
    }

    private void finishTransition() {
        transitionLength = 0;
        axisFrom = null;
        for (Series s : series) {
            s.visibility = s.isVisible() ? 1 : 0;
            s.visFrom = s.visibility;
        }
    }

    private float progress() {
        if (transitionLength <= 0) {
            return 1;
        }
        float t = (System.currentTimeMillis() - transitionStart) / (float) transitionLength;
        if (t >= 1) {
            return 1;
        }
        float u = 1 - t;
        return 1 - u * u * u;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (!themeSet) {
            // follow the style: the theme may have changed since the last time we showed
            theme = ChartTheme.fromStyle(getStyle()).applyUIIDs(getUIManager());
        }
        Form f = getComponentForm();
        if (f != null) {
            f.registerAnimated(this);
        }
        if (entryPending) {
            entryPending = false;
            for (Series s : series) {
                s.captureEntryStart();
            }
            axisFrom = null;
            captured = false;
            transitionStart = System.currentTimeMillis();
            transitionLength = animationDuration > 0 ? (int) (animationDuration * 1.6f) : 0;
        }
    }

    @Override
    protected void deinitialize() {
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimated(this);
        }
        super.deinitialize();
    }

    @Override
    public boolean animate() {
        boolean changed = super.animate();
        if (transitionLength > 0) {
            if (System.currentTimeMillis() - transitionStart >= transitionLength) {
                finishTransition();
            }
            changed = true;
        }
        if (viewportTo != null) {
            stepViewport();
            changed = true;
        }
        if (flinging) {
            stepFling();
            changed = true;
        }
        return changed;
    }

    // ------------------------------------------------------------------ layout and paint

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(mm(60), mm(45));
    }

    private Axis[] valueAxes() {
        if (secondaryYAxis != null) {
            return new Axis[]{yAxis, secondaryYAxis};
        }
        return new Axis[]{yAxis};
    }

    private boolean hasCartesian() {
        for (Series s : series) {
            if (s.isCartesian() && (s.isVisible() || s.visibility > 0)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasData() {
        for (Series s : series) {
            if (s.isVisible() || s.visibility > 0) {
                return true;
            }
        }
        return false;
    }

    private void prepareAxes() {
        double xmin = Double.MAX_VALUE;
        double xmax = -Double.MAX_VALUE;
        double[] ymin = {Double.MAX_VALUE, Double.MAX_VALUE};
        double[] ymax = {-Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Series s : series) {
            if (!s.isCartesian() || !s.isVisible()) {
                continue;
            }
            double[] xb = s.getXBounds();
            double[] yb = s.getYBounds();
            if (xb != null) {
                xmin = Math.min(xmin, xb[0]);
                xmax = Math.max(xmax, xb[1]);
            }
            if (yb != null) {
                int k = s.isSecondaryAxis() && secondaryYAxis != null ? 1 : 0;
                ymin[k] = Math.min(ymin[k], yb[0]);
                ymax[k] = Math.max(ymax[k], yb[1]);
            }
        }
        for (Annotation a : annotations) {
            double v = a.getValueExtent();
            if (v == v && a.isOnValueAxis()) {
                ymin[0] = Math.min(ymin[0], v);
                ymax[0] = Math.max(ymax[0], a.getValueExtentMax());
            }
        }
        if (xmin > xmax) {
            xmin = 0;
            xmax = 1;
        }
        xAxis.setDataBounds(xmin, xmax);
        Axis[] va = valueAxes();
        for (int i = 0; i < va.length; i++) {
            if (ymin[i] > ymax[i]) {
                ymin[i] = 0;
                ymax[i] = 1;
            }
            va[i].setDataBounds(ymin[i], ymax[i]);
        }
    }

    @Override
    public void paint(Graphics g) {
        Style st = getStyle();
        int x = getX() + st.getPaddingLeftNoRTL();
        int y = getY() + st.getPaddingTop();
        int w = getWidth() - st.getHorizontalPadding();
        int h = getHeight() - st.getVerticalPadding();
        paintChart(g, x, y, w, h);
    }

    private void paintChart(Graphics g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Font font = getStyle().getFont();
        if (font == null) {
            font = Font.getDefaultFont();
        }
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setFont(font);
        int fh = font.getHeight();
        int gap = Math.max(2, mm(1));
        float p = progress();

        ChartContext ctx = new ChartContext();
        ctx.graphics = g;
        ctx.chart = this;
        ctx.font = font;
        ctx.progress = p;
        ctx.originX = getX();
        ctx.originY = getY();
        for (Series s : series) {
            float target = s.isVisible() ? 1 : 0;
            s.visibility = s.visFrom + (target - s.visFrom) * p;
        }

        if (title != null && title.length() > 0) {
            Font tf = font.isTTFNativeFont() ? font.derive(font.getPixelSize() * 1.2f, Font.STYLE_BOLD)
                    : Font.createSystemFont(font.getFace(), Font.STYLE_BOLD, Font.SIZE_LARGE);
            g.setFont(tf);
            g.setColor(getTheme().getTextColor());
            String t = LabelLayout.ellipsize(tf, title, w);
            g.drawString(t, x + (w - tf.stringWidth(t)) / 2, y);
            y += tf.getHeight() + gap;
            h -= tf.getHeight() + gap;
            g.setFont(font);
        }

        if (!hasData()) {
            String msg = loadingText != null ? loadingText : emptyText;
            g.setColor(getTheme().getSecondaryTextColor());
            g.drawString(msg, x + (w - font.stringWidth(msg)) / 2, y + (h - fh) / 2);
            lastContext = ctx;
            g.setAntiAliased(aa);
            return;
        }

        // legend
        legendHits.clear();
        int legendPos = effectiveLegendPosition();
        if (legendPos != LEGEND_NONE) {
            int[] used = paintLegend(g, ctx, x, y, w, legendPos, false, w);
            if (legendPos == LEGEND_TOP) {
                paintLegend(g, ctx, x, y, w, legendPos, true, w);
                y += used[1] + gap;
                h -= used[1] + gap;
            } else if (legendPos == LEGEND_BOTTOM) {
                paintLegend(g, ctx, x, y + h - used[1], w, legendPos, true, w);
                h -= used[1] + gap;
            } else {
                paintLegend(g, ctx, x + w - used[0], y, used[0], legendPos, true, w);
                w -= used[0] + gap;
            }
        }

        int plotX = x;
        int plotY = y;
        int plotW = w;
        int plotH = h;
        AxisLabels xl = null;
        AxisLabels yl = null;
        AxisLabels y2l = null;
        boolean cartesian = hasCartesian();
        if (cartesian) {
            prepareAxes();
            applyAxisTransition(p);
            Axis horizontal = rotated ? yAxis : xAxis;
            Axis vertical = rotated ? xAxis : yAxis;
            Axis secondary = rotated ? null : secondaryYAxis;
            int titleRow = 0;
            if (vertical.getTitle() != null || secondary != null && secondary.getTitle() != null) {
                titleRow = fh + gap / 2;
            }
            int bottomTitle = horizontal.getTitle() != null ? fh + gap / 2 : 0;
            int estPlotH = h - fh - gap * 2 - titleRow - bottomTitle;
            yl = layoutVertical(vertical, font, estPlotH, w);
            if (secondary != null) {
                y2l = layoutVertical(secondary, font, estPlotH, w);
            }
            int left = vertical.isVisible() ? yl.thickness + gap : 0;
            int right = y2l != null && secondary.isVisible() ? y2l.thickness + gap : 0;
            plotW = w - left - right;
            xl = layoutHorizontal(horizontal, font, plotW);
            int bottom = horizontal.isVisible() ? xl.thickness + gap : 0;
            plotX = x + left;
            plotY = y + titleRow + fh / 2;
            plotH = h - bottom - titleRow - bottomTitle - fh / 2;
            if (plotH < fh * 2 || plotW < fh * 2) {
                // too small for axes: drop them and plot what fits
                plotX = x;
                plotY = y;
                plotW = w;
                plotH = h;
                xl = null;
                yl = null;
                y2l = null;
            } else if (yl.tickCount() > Math.max(2, plotH / (fh * 2))) {
                yl = layoutVertical(vertical, font, plotH, w);
            }
            if (titleRow > 0) {
                g.setColor(getTheme().getSecondaryTextColor());
                if (vertical.getTitle() != null) {
                    g.drawString(LabelLayout.ellipsize(font, vertical.getTitle(), w / 2), x, y);
                }
                if (secondary != null && secondary.getTitle() != null) {
                    String t2 = LabelLayout.ellipsize(font, secondary.getTitle(), w / 2);
                    g.drawString(t2, x + w - font.stringWidth(t2), y);
                }
            }
            if (bottomTitle > 0 && xl != null) {
                g.setColor(getTheme().getSecondaryTextColor());
                String bt = LabelLayout.ellipsize(font, horizontal.getTitle(), plotW);
                g.drawString(bt, plotX + (plotW - font.stringWidth(bt)) / 2, y + h - fh);
            }
        }
        ctx.plotX = plotX;
        ctx.plotY = plotY;
        ctx.plotW = plotW;
        ctx.plotH = plotH;
        ctx.labels = new LabelLayout(plotX, plotY - fh / 2, plotW, plotH + fh / 2);
        lastContext = ctx;

        if (cartesian && xl != null) {
            paintGrid(g, ctx, xl, yl);
        }
        // every annotation label claims its place before any data label is laid out
        for (Annotation a : annotations) {
            a.layoutLabel(ctx);
        }
        for (Annotation a : annotations) {
            if (a.isBehindSeries()) {
                a.paint(ctx);
            }
        }
        int[] clip = g.getClip();
        if (cartesian) {
            g.clipRect(plotX, plotY - fh / 2, plotW, plotH + fh / 2);
        }
        for (Series s : series) {
            if (s.visibility > 0) {
                s.paint(ctx);
            }
        }
        g.setClip(clip);
        for (Annotation a : annotations) {
            if (!a.isBehindSeries()) {
                a.paint(ctx);
            }
        }
        if (cartesian && xl != null) {
            paintAxes(g, ctx, xl, yl, y2l, font, gap);
        }
        if (brushFrom >= 0 && brushTo >= 0) {
            paintBrush(g, ctx);
        }
        paintTooltip(g, ctx, font);
        if (loadingText != null) {
            paintLoading(g, font, x, y, w, h);
        }
        g.setAntiAliased(aa);
    }

    private void paintLoading(Graphics g, Font font, int x, int y, int w, int h) {
        int alpha = g.getAlpha();
        g.setAlpha(alpha * 160 / 255);
        g.setColor(getStyle().getBgColor());
        g.fillRect(x, y, w, h);
        g.setAlpha(alpha);
        g.setColor(getTheme().getTextColor());
        g.drawString(loadingText, x + (w - font.stringWidth(loadingText)) / 2, y + (h - font.getHeight()) / 2);
    }

    private void applyAxisTransition(float p) {
        if (axisFrom == null || p >= 1) {
            return;
        }
        Axis[] va = valueAxes();
        lerpAxis(xAxis, axisFrom[0], axisFrom[1], p);
        for (int i = 0; i < va.length && 3 + i * 2 < axisFrom.length; i++) {
            lerpAxis(va[i], axisFrom[2 + i * 2], axisFrom[3 + i * 2], p);
        }
    }

    private static void lerpAxis(Axis a, double fromMin, double fromMax, float p) {
        double toMin = a.getDataMin();
        double toMax = a.getDataMax();
        a.setDisplayRange(fromMin + (toMin - fromMin) * p, fromMax + (toMax - fromMax) * p);
    }

    private int effectiveLegendPosition() {
        if (legendPosition == LEGEND_NONE) {
            return LEGEND_NONE;
        }
        int entries = 0;
        for (Series s : series) {
            entries += s.getLegendEntryCount();
        }
        return entries < 2 ? LEGEND_NONE : legendPosition;
    }

    /// Measures (and with `paint` draws) the legend; returns the `{width, height}` used.
    private int[] paintLegend(Graphics g, ChartContext ctx, int x, int y, int w, int pos, boolean paint,
            int fullWidth) {
        Font f = ctx.font;
        int fh = f.getHeight();
        int sw = Math.max(6, fh * 2 / 3);
        int itemGap = mm(3);
        int rowGap = mm(1);
        boolean vertical = pos == LEGEND_RIGHT;
        boolean rtl = isRTL();
        int maxItemW = vertical ? Math.min(fullWidth / 3, mm(35)) : w;
        int cx = x;
        int cy = y;
        int usedW = 0;
        int rowH = Math.max(fh, sw);
        if (paint && !vertical) {
            // centre each row: measure rows first
            cx = x + (w - rowWidth(f, sw, itemGap, maxItemW, w, 0)) / 2;
        }
        int entryIndex = 0;
        for (Series s : series) {
            int n = s.getLegendEntryCount();
            for (int i = 0; i < n; i++) {
                String name = s.getLegendEntryName(i);
                if (name == null) {
                    name = "";
                }
                name = LabelLayout.ellipsize(f, name, maxItemW - sw - mm(1));
                int iw = sw + mm(1) + f.stringWidth(name);
                if (!vertical && cx + iw > x + w && cx > x) {
                    cy += rowH + rowGap;
                    cx = paint ? x + (w - rowWidth(f, sw, itemGap, maxItemW, w, entryIndex)) / 2 : x;
                }
                if (paint) {
                    boolean on = s.isLegendEntryVisible(i);
                    int alpha = g.getAlpha();
                    if (!on) {
                        g.setAlpha(alpha * 2 / 5);
                    }
                    // right to left: entries run from the right and each symbol follows its text
                    int ix = rtl ? (vertical ? x + w - iw : 2 * x + w - cx - iw) : cx;
                    int symX = rtl ? ix + iw - sw : ix;
                    int textX = rtl ? ix : ix + sw + mm(1);
                    s.paintLegendEntrySymbol(g, i, symX, cy + (rowH - sw) / 2, sw);
                    g.setColor(getTheme().getLegendTextColor());
                    g.drawString(name, textX, cy + (rowH - fh) / 2);
                    g.setAlpha(alpha);
                    legendHits.add(new int[]{ix - getX(), cy - getY(), iw, rowH, indexOf(s), i});
                }
                usedW = Math.max(usedW, vertical ? iw : cx + iw - x);
                if (vertical) {
                    cy += rowH + rowGap;
                } else {
                    cx += iw + itemGap;
                }
                entryIndex++;
            }
        }
        int usedH = vertical ? cy - y : cy - y + rowH;
        return new int[]{usedW, usedH};
    }

    private int rowWidth(Font f, int sw, int itemGap, int maxItemW, int w, int startEntry) {
        int idx = 0;
        int rw = 0;
        for (Series s : series) {
            int n = s.getLegendEntryCount();
            for (int i = 0; i < n; i++, idx++) {
                if (idx < startEntry) {
                    continue;
                }
                String name = LabelLayout.ellipsize(f, s.getLegendEntryName(i), maxItemW - sw - mm(1));
                int iw = sw + mm(1) + f.stringWidth(name);
                if (rw > 0 && rw + itemGap + iw > w) {
                    return rw;
                }
                rw += (rw > 0 ? itemGap : 0) + iw;
            }
        }
        return rw;
    }

    /// Tick values with their final label text and the thickness the axis needs.
    static final class AxisLabels {
        Axis axis;
        double[] ticks;
        String[] labels;
        int thickness;
        double step;

        int tickCount() {
            return ticks == null ? 0 : ticks.length;
        }
    }

    private AxisLabels layoutVertical(Axis a, Font f, int length, int chartWidth) {
        AxisLabels al = new AxisLabels();
        al.axis = a;
        int fh = f.getHeight();
        int max = Math.max(2, length / Math.max(1, fh * 2));
        al.ticks = a.ticks(max);
        al.step = al.ticks.length > 1 ? Math.abs(al.ticks[1] - al.ticks[0]) : 0;
        int cap = chartWidth * 2 / 5;
        al.labels = new String[al.ticks.length];
        int widest = 0;
        for (int i = 0; i < al.ticks.length; i++) {
            al.labels[i] = LabelLayout.ellipsize(f, a.format(al.ticks[i], al.step), cap);
            widest = Math.max(widest, f.stringWidth(al.labels[i]));
        }
        al.thickness = a.isVisible() ? widest : 0;
        return al;
    }

    private AxisLabels layoutHorizontal(Axis a, Font f, int length) {
        AxisLabels al = new AxisLabels();
        al.axis = a;
        int fh = f.getHeight();
        int gap = mm(2);
        int max = Math.max(2, length / Math.max(1, fh * 3));
        // shrink the tick count until the measured labels fit side by side
        for (int attempt = 0; attempt < 12; attempt++) {
            double[] t = a.ticks(max);
            double step = t.length > 1 ? Math.abs(t[1] - t[0]) : 0;
            String[] l = new String[t.length];
            int widest = 0;
            for (int i = 0; i < t.length; i++) {
                l[i] = a.format(t[i], step);
                widest = Math.max(widest, f.stringWidth(l[i]));
            }
            al.ticks = t;
            al.labels = l;
            al.step = step;
            int slot = t.length > 0 ? length / Math.max(1, t.length) : length;
            if (widest + gap <= slot || t.length <= 2 && !(a instanceof CategoryAxis) || max <= 1) {
                break;
            }
            int fit = Math.max(1, length / (widest + gap));
            max = Math.min(max - 1, fit);
            if (a instanceof CategoryAxis && t.length <= 1) {
                break;
            }
        }
        // whatever still does not fit its slot is shortened, never overlapped
        int slot = length / Math.max(1, al.ticks.length) - gap;
        if (a instanceof CategoryAxis) {
            double every = al.step > 0 ? al.step : 1;
            slot = (int) (length * every / Math.max(1e-9, a.getVisibleMax() - a.getVisibleMin())) - gap;
        }
        for (int i = 0; i < al.labels.length; i++) {
            al.labels[i] = LabelLayout.ellipsize(f, al.labels[i], Math.max(slot, f.stringWidth("...")));
        }
        al.thickness = a.isVisible() ? fh : 0;
        return al;
    }

    private int verticalPixel(ChartContext ctx, Axis a, double v) {
        double n = a.toNormalized(v);
        if (isTopDown(a)) {
            return ctx.plotY + (int) Math.round(n * ctx.plotH);
        }
        return ctx.plotY + ctx.plotH - (int) Math.round(n * ctx.plotH);
    }

    private void paintGrid(Graphics g, ChartContext ctx, AxisLabels xl, AxisLabels yl) {
        g.setColor(getTheme().getGridColor());
        Axis vertical = yl.axis;
        if (vertical.isGridVisible()) {
            for (double t : yl.ticks) {
                int py = verticalPixel(ctx, vertical, t);
                g.drawLine(ctx.plotX, py, ctx.plotX + ctx.plotW, py);
            }
        }
        Axis horizontal = xl.axis;
        if (horizontal.isGridVisible()) {
            for (double t : xl.ticks) {
                int px = ctx.plotX + (int) Math.round(horizontal.toNormalized(t) * ctx.plotW);
                g.drawLine(px, ctx.plotY, px, ctx.plotY + ctx.plotH);
            }
        }
    }

    private void paintAxes(Graphics g, ChartContext ctx, AxisLabels xl, AxisLabels yl, AxisLabels y2l, Font f, int gap) {
        int fh = f.getHeight();
        g.setFont(f);
        // baseline at zero (or the bottom when zero is off screen)
        Axis value = rotated ? xl.axis : yl.axis;
        g.setColor(getTheme().getAxisColor());
        // rows of categories (a heatmap) have no zero to mark; draw the baseline at the edge
        boolean categorical = value instanceof CategoryAxis;
        if (!rotated) {
            double zn = categorical ? -1 : value.toNormalized(0);
            int py = zn >= 0 && zn <= 1 ? ctx.plotY + ctx.plotH - (int) Math.round(zn * ctx.plotH) : ctx.plotY + ctx.plotH;
            g.drawLine(ctx.plotX, py, ctx.plotX + ctx.plotW, py);
        } else {
            double zn = categorical ? -1 : value.toNormalized(0);
            int px = zn >= 0 && zn <= 1 ? ctx.plotX + (int) Math.round(zn * ctx.plotW) : ctx.plotX;
            g.drawLine(px, ctx.plotY, px, ctx.plotY + ctx.plotH);
        }
        g.setColor(getTheme().getSecondaryTextColor());
        if (xl.axis.isVisible()) {
            int prevRight = Integer.MIN_VALUE;
            for (int i = 0; i < xl.ticks.length; i++) {
                double n = xl.axis.toNormalized(xl.ticks[i]);
                if (n < -0.001 || n > 1.001) {
                    continue;
                }
                int px = ctx.plotX + (int) Math.round(n * ctx.plotW);
                String s = xl.labels[i];
                int sw = f.stringWidth(s);
                int lx = Math.max(ctx.plotX - gap, Math.min(px - sw / 2, ctx.plotX + ctx.plotW - sw));
                if (lx < prevRight + gap / 2) {
                    continue;
                }
                g.drawString(s, lx, ctx.plotY + ctx.plotH + gap);
                ctx.labels.reserve(lx, ctx.plotY + ctx.plotH + gap, sw, fh);
                prevRight = lx + sw;
            }
        }
        if (yl.axis.isVisible()) {
            paintVerticalLabels(g, ctx, yl, f, ctx.plotX - gap, true);
        }
        if (y2l != null && y2l.axis.isVisible()) {
            paintVerticalLabels(g, ctx, y2l, f, ctx.plotX + ctx.plotW + gap, false);
        }
    }

    private void paintVerticalLabels(Graphics g, ChartContext ctx, AxisLabels al, Font f, int edge, boolean rightAligned) {
        int fh = f.getHeight();
        int prevTop = isTopDown(al.axis) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int i = 0; i < al.ticks.length; i++) {
            double n = al.axis.toNormalized(al.ticks[i]);
            if (n < -0.001 || n > 1.001) {
                continue;
            }
            int py = verticalPixel(ctx, al.axis, al.ticks[i]) - fh / 2;
            if (isTopDown(al.axis) ? py < prevTop : py + fh > prevTop) {
                continue;
            }
            String s = al.labels[i];
            int sw = f.stringWidth(s);
            g.drawString(s, rightAligned ? edge - sw : edge, py);
            prevTop = isTopDown(al.axis) ? py + fh : py;
        }
    }

    // ------------------------------------------------------------------ tooltip

    private void paintTooltip(Graphics g, ChartContext ctx, Font f) {
        if (tooltipMode == TOOLTIP_NONE) {
            return;
        }
        List<ChartPoint> rows = new ArrayList<ChartPoint>();
        int anchorX;
        int anchorY;
        if (tooltipMode == TOOLTIP_SHARED && sharedIndex >= 0 && hasCartesian()) {
            for (Series s : series) {
                if (s.isVisible() && s.isCartesian() && sharedIndex < s.getPointCount()) {
                    ChartPoint cp = s.getPoint(ctx, sharedIndex);
                    if (cp != null) {
                        rows.add(cp);
                    }
                }
            }
            if (rows.isEmpty()) {
                return;
            }
            anchorX = rows.get(0).getScreenX() + getX();
            anchorY = ctx.plotY + ctx.plotH / 3;
            g.setColor(getTheme().getAxisColor());
            if (rotated) {
                int py = rows.get(0).getScreenY() + getY();
                g.drawLine(ctx.plotX, py, ctx.plotX + ctx.plotW, py);
                anchorX = ctx.plotX + ctx.plotW / 2;
                anchorY = py;
            } else {
                g.drawLine(anchorX, ctx.plotY, anchorX, ctx.plotY + ctx.plotH);
            }
            for (ChartPoint cp : rows) {
                int d = mm(1.8f);
                g.setColor(getStyle().getBgColor());
                g.fillArc(cp.getScreenX() + getX() - d / 2 - 1, cp.getScreenY() + getY() - d / 2 - 1, d + 2, d + 2, 0, 360);
                g.setColor(cp.getSeries().getColor());
                g.fillArc(cp.getScreenX() + getX() - d / 2, cp.getScreenY() + getY() - d / 2, d, d, 0, 360);
            }
        } else {
            ChartPoint cp = hover != null ? hover : selected;
            if (cp == null || cp.getSeries().chart != this || !cp.getSeries().isVisible()) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                return;
            }
            rows.add(cp);
            anchorX = cp.getScreenX() + getX();
            anchorY = cp.getScreenY() + getY();
        }
        String header = null;
        String[] lines = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            ChartPoint cp = rows.get(i);
            if (tooltipRenderer != null) {
                lines[i] = tooltipRenderer.formatRow(cp);
                if (i == 0) {
                    header = tooltipRenderer.formatHeader(cp);
                }
                continue;
            }
            Series s = cp.getSeries();
            lines[i] = s.formatTooltip(ctx, cp);
            if (i == 0 && s.isCartesian()) {
                header = xAxis.format(cp.getX(), 0);
            }
        }
        int fh = f.getHeight();
        int pad = mm(1.5f);
        int sw = fh / 2;
        int boxW = header == null ? 0 : f.stringWidth(header);
        for (String l : lines) {
            boxW = Math.max(boxW, sw + pad + f.stringWidth(l));
        }
        boxW += pad * 2;
        int boxH = pad * 2 + fh * lines.length + (header == null ? 0 : fh);
        int left = getX() + getStyle().getPaddingLeftNoRTL();
        int top = getY() + getStyle().getPaddingTop();
        int right = getX() + getWidth() - getStyle().getPaddingRightNoRTL();
        int bottom = getY() + getHeight() - getStyle().getPaddingBottom();
        boxW = Math.min(boxW, right - left);
        int off = mm(3);
        int bx = anchorX + off;
        if (bx + boxW > right) {
            bx = anchorX - off - boxW;
        }
        bx = Math.max(left, bx);
        int by = anchorY - boxH - off;
        if (by < top) {
            by = anchorY + off;
        }
        by = Math.max(top, Math.min(by, bottom - boxH));
        int alpha = g.getAlpha();
        g.setColor(getTheme().getTooltipBackground());
        g.setAlpha(alpha * 235 / 255);
        g.fillRoundRect(bx, by, boxW, boxH, mm(1.5f), mm(1.5f));
        g.setAlpha(alpha);
        g.setColor(getTheme().getTooltipText());
        int ty = by + pad;
        if (header != null) {
            g.drawString(LabelLayout.ellipsize(f, header, boxW - pad * 2), bx + pad, ty);
            ty += fh;
        }
        for (int i = 0; i < lines.length; i++) {
            g.setColor(rows.get(i).getSeries().getColor());
            if (rows.get(i).getLabel() == null || rows.size() > 1 || !(rows.get(i).getSeries().getLegendEntryCount() > 1)) { //NOPMD LogicInversion - the negated comparison is also true for NaN
                g.fillRoundRect(bx + pad, ty + (fh - sw) / 2, sw, sw, sw / 2, sw / 2);
            }
            g.setColor(getTheme().getTooltipText());
            g.drawString(LabelLayout.ellipsize(f, lines[i], boxW - pad * 3 - sw), bx + pad * 2 + sw, ty);
            ty += fh;
        }
    }

    // ------------------------------------------------------------------ accessibility

    /// The chart's data as rows of `{series, item, value}`, in series order. Hidden series are
    /// included: the table is the complete data, not what happens to be on screen.
    ///
    /// #### Returns
    ///
    /// a new list of rows
    public List<String[]> getTableRows() {
        ArrayList<String[]> rows = new ArrayList<String[]>();
        for (Series s : series) {
            s.collectTableRows(rows);
        }
        return rows;
    }

    /// The chart's data as a table model with the columns Series, Item and Value, for a data
    /// table view, export, or a screen reader friendly alternative to the picture.
    ///
    /// #### Returns
    ///
    /// a new, read only table model
    public com.codename1.ui.table.TableModel toTableModel() {
        List<String[]> rows = getTableRows();
        Object[][] data = new Object[rows.size()][];
        for (int i = 0; i < data.length; i++) {
            String[] r = rows.get(i);
            data[i] = new Object[]{r[0], r[1], r[2]};
        }
        return new com.codename1.ui.table.DefaultTableModel(new String[]{"Series", "Item", "Value"}, data, false);
    }

    /// A table component showing the chart's data; offer it beside or instead of the chart for
    /// readers who cannot see it.
    ///
    /// #### Returns
    ///
    /// a new table
    public com.codename1.ui.table.Table toTable() {
        return new com.codename1.ui.table.Table(toTableModel());
    }

    /// A one paragraph summary of the chart for screen readers: its title and, for each series,
    /// how many values it has and their range.
    ///
    /// #### Returns
    ///
    /// the summary
    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (title != null) {
            sb.append(title).append(". ");
        }
        if (loadingText != null) {
            sb.append(loadingText).append(". ");
        }
        if (series.isEmpty()) {
            sb.append(emptyText == null ? "No data." : emptyText);
            return sb.toString();
        }
        for (int i = 0; i < series.size(); i++) {
            Series s = series.get(i);
            sb.append(s.getName() == null ? "Series " + (i + 1) : s.getName()).append(": ");
            double[] yb = valueRange(s);
            if (yb != null) {
                // an x/y series is summarized from its numbers; formatting every point of a long
                // series on each accessibility query would be wasted work
                sb.append(((com.codename1.ui.chart.series.XYSeries) s).size()).append(" values from ").append(s.formatValue(null, yb[0]))
                        .append(" to ").append(s.formatValue(null, yb[1]));
            } else {
                ArrayList<String[]> rows = new ArrayList<String[]>();
                s.collectTableRows(rows);
                int shown = Math.min(rows.size(), 6);
                for (int k = 0; k < shown; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append(rows.get(k)[1]).append(' ').append(rows.get(k)[2]);
                }
                if (rows.size() > shown) {
                    sb.append(", and ").append(rows.size() - shown).append(" more");
                }
            }
            sb.append(". ");
        }
        return sb.toString().trim();
    }

    // the smallest and largest data values, without the baseline a bar or area adds to its bounds
    private static double[] valueRange(Series s) {
        if (!(s instanceof com.codename1.ui.chart.series.XYSeries)) {
            return null;
        }
        com.codename1.ui.chart.series.XYSeries xy = (com.codename1.ui.chart.series.XYSeries) s;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < xy.size(); i++) {
            double v = xy.getY(i);
            if (v == v) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }
        return min > max ? null : new double[]{min, max};
    }

    /// The accessibility text set on the chart, or a generated summary of its data.
    @Override
    public String getAccessibilityText() {
        String t = super.getAccessibilityText();
        return t != null ? t : describe();
    }

    // ------------------------------------------------------------------ hit testing

    /// Finds the point at a chart relative position, the topmost series first.
    public ChartPoint hitTest(int x, int y) {
        ChartContext ctx = lastContext;
        if (ctx == null) {
            return null;
        }
        int radius = mm(4);
        for (int i = series.size() - 1; i >= 0; i--) {
            Series s = series.get(i);
            if (!s.isVisible()) {
                continue;
            }
            ChartPoint p = s.hitTest(ctx, x, y, radius);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    private int nearestIndex(int relX, int relY) {
        ChartContext ctx = lastContext;
        if (ctx == null) {
            return -1;
        }
        // the first visible cartesian series with points is the reference for shared tooltips
        Series s = null;
        for (Series candidate : series) {
            if (candidate.isVisible() && candidate.isCartesian() && candidate.getPointCount() > 0) {
                s = candidate;
                break;
            }
        }
        if (s == null) {
            return -1;
        }
        int best = -1;
        int bestD = Integer.MAX_VALUE;
        for (int i = 0; i < s.getPointCount(); i++) {
            ChartPoint p = s.getPoint(ctx, i);
            if (p == null) {
                continue;
            }
            int d = rotated ? Math.abs(p.getScreenY() - relY) : Math.abs(p.getScreenX() - relX);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private boolean insidePlot(int x, int y) {
        ChartContext c = lastContext;
        return c != null && x >= c.plotX && x <= c.plotX + c.plotW && y >= c.plotY && y <= c.plotY + c.plotH;
    }

    private void updatePointer(int x, int y, boolean hovering) {
        int rx = x - getX();
        int ry = y - getY();
        if (tooltipMode == TOOLTIP_SHARED && insidePlot(x, y)) {
            int idx = nearestIndex(rx, ry);
            if (idx != sharedIndex) {
                sharedIndex = idx;
                fireCrosshair();
                repaint();
            }
            return;
        }
        ChartPoint p = hitTest(rx, ry);
        if (hovering) {
            if (!same(p, hover)) {
                hover = p;
                repaint();
            }
        }
    }

    private static boolean same(ChartPoint a, ChartPoint b) {
        if (a == null || b == null) {
            return a == b; //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
        }
        return a.getSeries() == b.getSeries() && a.getIndex() == b.getIndex(); //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
    }

    // ------------------------------------------------------------------ gestures
    //
    // Pointer events arrive in absolute screen coordinates; painting (getX(), plotX and the
    // context) uses the parent's coordinates. Every handler converts once, at entry, with
    // toPaintX/toPaintY, and everything below works in paint coordinates.

    private int toPaintX(int absX) {
        return absX - getAbsoluteX() + getX();
    }

    private int toPaintY(int absY) {
        return absY - getAbsoluteY() + getY();
    }

    /// The axis drawn horizontally.
    private Axis horizontalAxis() {
        return rotated ? yAxis : xAxis;
    }

    /// The axis drawn vertically.
    private Axis verticalAxis() {
        return rotated ? xAxis : yAxis;
    }

    /// True when larger values of the vertical axis are drawn lower: the category axis of a
    /// rotated chart reads top to bottom, as a ranked list does.
    boolean isTopDown(Axis a) {
        return rotated && a == xAxis; //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
    }

    /// The normalized position of a paint y within the plot along the vertical axis.
    private double verticalAnchor(int paintY) {
        ChartContext c = lastContext;
        double n = (paintY - c.plotY) / (double) c.plotH;
        return clamp01(isTopDown(verticalAxis()) ? n : 1 - n);
    }

    @Override
    public void pointerPressed(int x, int y) {
        flinging = false;
        pressX = toPaintX(x);
        pressY = toPaintY(y);
        lastX = pressX;
        lastY = pressY;
        lastMoveTime = System.currentTimeMillis();
        velocityX = 0;
        velocityY = 0;
        dragMode = DRAG_UNDECIDED;
        pinching = false;
        super.pointerPressed(x, y);
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        if (x.length > 1) {
            if (!zoomEnabled || !hasCartesian() || dragMode == DRAG_PASS) {
                super.pointerDragged(x, y);
                return;
            }
            dragMode = DRAG_PAN;
            handlePinch(toPaintX(x[0]), toPaintY(y[0]), toPaintX(x[1]), toPaintY(y[1]));
            return;
        }
        int px = toPaintX(x[0]);
        int py = toPaintY(y[0]);
        if (pinching) {
            // a finger lifted: continue as a pan from here
            pinching = false;
            lastX = px;
            lastY = py;
            return;
        }
        if (dragMode == DRAG_UNDECIDED) {
            int slop = mm(1.5f);
            if (Math.abs(px - pressX) < slop && Math.abs(py - pressY) < slop) {
                return;
            }
            // decided once per gesture and latched, so a scroll stays a scroll
            boolean zoomed = horizontalAxis().isZoomed() || verticalAxis().isZoomed();
            boolean horizontalIntent = Math.abs(px - pressX) > Math.abs(py - pressY);
            if (brushEnabled && hasCartesian() && insidePlot(pressX, pressY)) {
                dragMode = DRAG_BRUSH;
                brushFrom = rotated ? pressY : pressX;
            } else if (zoomEnabled && zoomed && hasCartesian()) {
                dragMode = DRAG_PAN;
            } else if (horizontalIntent && hasCartesian() && tooltipMode != TOOLTIP_NONE && insidePlot(pressX, pressY)) {
                dragMode = DRAG_SCRUB;
            } else {
                dragMode = DRAG_PASS;
            }
        }
        if (dragMode == DRAG_PASS) {
            super.pointerDragged(x, y);
            return;
        }
        if (dragMode == DRAG_BRUSH) {
            brushTo = rotated ? py : px;
            repaint();
        } else if (dragMode == DRAG_PAN) {
            long now = System.currentTimeMillis();
            int dx = px - lastX;
            int dy = py - lastY;
            long dt = Math.max(1, now - lastMoveTime);
            velocityX = velocityX * 0.4f + 0.6f * dx / dt;
            velocityY = velocityY * 0.4f + 0.6f * dy / dt;
            lastMoveTime = now;
            panBy(dx, dy);
        } else {
            scrubTo(px, py);
        }
        lastX = px;
        lastY = py;
    }

    private void scrubTo(int px, int py) {
        if (tooltipMode == TOOLTIP_SHARED) {
            updatePointer(px, py, false);
            return;
        }
        ChartPoint p = nearestPoint(px - getX(), py - getY());
        if (!same(p, hover)) {
            hover = p;
            repaint();
        }
    }

    private ChartPoint nearestPoint(int rx, int ry) {
        int idx = nearestIndex(rx, ry);
        if (idx < 0) {
            return hitTest(rx, ry);
        }
        ChartPoint best = null;
        int bestD = Integer.MAX_VALUE;
        for (Series s : series) {
            if (!s.isVisible() || !s.isCartesian() || idx >= s.getPointCount()) {
                continue;
            }
            ChartPoint p = s.getPoint(lastContext, idx);
            if (p != null) {
                int d = rotated ? Math.abs(p.getScreenX() - rx) : Math.abs(p.getScreenY() - ry);
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        return best;
    }

    private Axis[] verticalAxes() {
        if (rotated) {
            return new Axis[]{xAxis};
        }
        return valueAxes();
    }

    private Axis[] horizontalAxes() {
        if (rotated) {
            return valueAxes();
        }
        return new Axis[]{xAxis};
    }

    private void panBy(int dx, int dy) {
        ChartContext c = lastContext;
        if (c == null || c.plotW <= 0 || c.plotH <= 0) {
            return;
        }
        for (Axis h : horizontalAxes()) {
            if (h.isZoomed()) {
                h.pan(-dx / (double) c.plotW);
            }
        }
        for (Axis v : verticalAxes()) {
            if (v.isZoomed()) {
                v.pan((isTopDown(v) ? -dy : dy) / (double) c.plotH);
            }
        }
        fireViewportChanged();
        repaint();
    }

    private void handlePinch(int x0, int y0, int x1, int y1) {
        ChartContext c = lastContext;
        if (c == null) {
            return;
        }
        double dx = Math.max(mm(4), Math.abs(x1 - x0));
        double dy = Math.max(mm(4), Math.abs(y1 - y0));
        int cx = (x0 + x1) / 2;
        int cy = (y0 + y1) / 2;
        if (!pinching) {
            pinching = true;
            hover = null;
            pinchDistX = dx;
            pinchDistY = dy;
            pinchCenterX = cx;
            pinchCenterY = cy;
            return;
        }
        // fingers spread mostly sideways zoom x only, mostly vertically zoom y only
        double angle = MathUtil.atan2(Math.abs(y1 - y0), Math.abs(x1 - x0));
        boolean zx = angle < Math.PI * 0.35;
        boolean zy = angle > Math.PI * 0.15;
        double ax = clamp01((cx - c.plotX) / (double) c.plotW);
        double ay = verticalAnchor(cy);
        if (zx) {
            for (Axis h : horizontalAxes()) {
                h.zoom(dx / pinchDistX, ax);
            }
        }
        if (zy) {
            for (Axis v : verticalAxes()) {
                v.zoom(dy / pinchDistY, ay);
            }
        }
        panBy(cx - pinchCenterX, cy - pinchCenterY);
        pinchDistX = dx;
        pinchDistY = dy;
        pinchCenterX = cx;
        pinchCenterY = cy;
        repaint();
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    @Override
    protected void pinchReleased(int x, int y) {
        pinching = false;
        lastX = toPaintX(x);
        lastY = toPaintY(y);
    }

    @Override
    public void pointerReleased(int x, int y) {
        int mode = dragMode;
        dragMode = DRAG_UNDECIDED;
        if (mode == DRAG_PAN) {
            if (System.currentTimeMillis() - lastMoveTime < 80
                    && (Math.abs(velocityX) > 0.3f || Math.abs(velocityY) > 0.3f)) {
                flinging = true;
                flingLast = System.currentTimeMillis();
            }
            super.pointerReleased(x, y);
            return;
        }
        if (mode == DRAG_BRUSH) {
            finishBrush();
            super.pointerReleased(x, y);
            return;
        }
        if (mode == DRAG_SCRUB || mode == DRAG_PASS) {
            if (tooltipMode != TOOLTIP_SHARED) {
                hover = null;
            }
            repaint();
            super.pointerReleased(x, y);
            return;
        }
        super.pointerReleased(x, y);
        handleTap(toPaintX(x), toPaintY(y));
    }

    private void handleTap(int x, int y) {
        int rx = x - getX();
        int ry = y - getY();
        for (int[] hit : legendHits) {
            if (rx >= hit[0] && rx <= hit[0] + hit[2] && ry >= hit[1] - mm(1) && ry <= hit[1] + hit[3] + mm(1)) {
                Series s = series.get(hit[4]);
                s.toggleLegendEntry(hit[5]);
                return;
            }
        }
        long now = System.currentTimeMillis();
        boolean doubleTap = now - lastTapTime < 350 && Math.abs(x - lastTapX) < mm(6) && Math.abs(y - lastTapY) < mm(6);
        lastTapTime = doubleTap ? 0 : now;
        lastTapX = x;
        lastTapY = y;
        if (doubleTap && zoomEnabled && hasCartesian() && insidePlot(x, y)) {
            if (horizontalAxis().isZoomed() || verticalAxis().isZoomed()) {
                resetZoom();
            } else {
                zoomAt(x, y, 2.5, true);
            }
            return;
        }
        if (tooltipMode == TOOLTIP_SHARED && insidePlot(x, y)) {
            updatePointer(x, y, false);
        }
        ChartPoint p = hitTest(rx, ry);
        if (p != null && p.getSeries().handleTap(p)) {
            // the series consumed the tap, e.g. a treemap drilling into a node
            selected = null;
            repaint();
            return;
        }
        ChartPoint next = same(p, selected) ? null : p;
        if (next != null || selected != null) {
            selected = next;
            for (SelectionListener l : new ArrayList<SelectionListener>(selectionListeners)) {
                l.selectionChanged(this, next);
            }
        }
        repaint();
    }

    private void zoomAt(int x, int y, double factor, boolean animate) {
        ChartContext c = lastContext;
        if (c == null) {
            return;
        }
        double ax = clamp01((x - c.plotX) / (double) c.plotW);
        double ay = verticalAnchor(y);
        // category and time charts zoom along x only: their values keep their scale
        boolean valueZoom = !(xAxis instanceof CategoryAxis) && !isTimeLike(xAxis);
        Axis[] axes = allAxes();
        double[] before = snapshot(axes);
        for (Axis h : horizontalAxes()) {
            if (h == xAxis || valueZoom) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                h.zoom(factor, ax);
            }
        }
        for (Axis v : verticalAxes()) {
            if (v == xAxis || valueZoom) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                v.zoom(factor, ay);
            }
        }
        if (!animate) {
            fireViewportChanged();
            repaint();
            return;
        }
        double[] after = snapshot(axes);
        restore(axes, before);
        animateViewport(after);
    }

    private Axis[] allAxes() {
        if (secondaryYAxis != null) {
            return new Axis[]{xAxis, yAxis, secondaryYAxis};
        }
        return new Axis[]{xAxis, yAxis};
    }

    private static double[] snapshot(Axis[] axes) {
        double[] r = new double[axes.length * 3];
        for (int i = 0; i < axes.length; i++) {
            r[i * 3] = axes[i].getVisibleMin();
            r[i * 3 + 1] = axes[i].getVisibleMax();
            r[i * 3 + 2] = axes[i].isZoomed() ? 1 : 0;
        }
        return r;
    }

    private static void restore(Axis[] axes, double[] r) {
        for (int i = 0; i < axes.length; i++) {
            if (r[i * 3 + 2] == 0) {
                axes[i].resetViewport();
            } else {
                axes[i].setVisibleRange(r[i * 3], r[i * 3 + 1]);
            }
        }
    }

    private static boolean isTimeLike(Axis a) {
        return a instanceof com.codename1.ui.chart.axis.TimeAxis;
    }

    /// Returns to the full data range with an animation.
    public void resetZoom() {
        Axis[] axes = allAxes();
        double[] target = new double[axes.length * 3];
        for (int i = 0; i < axes.length; i++) {
            target[i * 3] = axes[i].getDataMin();
            target[i * 3 + 1] = axes[i].getDataMax();
        }
        animateViewport(target);
    }

    // the x value under a screen position along the brushed (x) axis
    private double brushValue(int pos) {
        ChartContext ctx = lastContext;
        double n = rotated ? (pos - ctx.plotY) / (double) ctx.plotH : (pos - ctx.plotX) / (double) ctx.plotW;
        return xAxis.fromNormalized(Math.max(0, Math.min(1, n)));
    }

    private void finishBrush() {
        int from = brushFrom;
        int to = brushTo;
        brushFrom = -1;
        brushTo = -1;
        repaint();
        if (from < 0 || to < 0 || lastContext == null || Math.abs(to - from) < mm(2)) {
            return;
        }
        double a = brushValue(from);
        double b = brushValue(to);
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        for (RangeListener l : new ArrayList<RangeListener>(rangeListeners)) {
            l.rangeSelected(this, lo, hi);
        }
        if (brushZooms && zoomEnabled) {
            Axis[] axes = allAxes();
            double[] target = snapshot(axes);
            target[0] = lo;
            target[1] = hi;
            target[2] = 1;
            animateViewport(target);
        }
    }

    private void paintBrush(Graphics g, ChartContext ctx) {
        int a = Math.min(brushFrom, brushTo);
        int b = Math.max(brushFrom, brushTo);
        int alpha = g.getAlpha();
        g.setColor(getTheme().getSeriesColor(0));
        g.setAlpha(alpha * 50 / 255);
        if (rotated) {
            a = Math.max(a, ctx.plotY);
            b = Math.min(b, ctx.plotY + ctx.plotH);
            g.fillRect(ctx.plotX, a, ctx.plotW, b - a);
        } else {
            a = Math.max(a, ctx.plotX);
            b = Math.min(b, ctx.plotX + ctx.plotW);
            g.fillRect(a, ctx.plotY, b - a, ctx.plotH);
        }
        g.setAlpha(alpha);
    }

    private void animateViewport(double[] target) {
        viewportFrom = snapshot(allAxes());
        viewportTo = target;
        viewportStart = System.currentTimeMillis();
        repaint();
    }

    private void stepViewport() {
        Axis[] axes = allAxes();
        float t = (System.currentTimeMillis() - viewportStart) / 300f;
        if (t >= 1 || viewportFrom.length != viewportTo.length || viewportTo.length != axes.length * 3) {
            restore(axes, viewportTo);
            viewportTo = null;
            fireViewportChanged();
            return;
        }
        float u = 1 - t;
        double e = 1 - u * u * u;
        for (int i = 0; i < axes.length; i++) {
            double lo = viewportFrom[i * 3] + (viewportTo[i * 3] - viewportFrom[i * 3]) * e;
            double hi = viewportFrom[i * 3 + 1] + (viewportTo[i * 3 + 1] - viewportFrom[i * 3 + 1]) * e;
            axes[i].setDisplayRange(lo, hi);
        }
    }

    private void stepFling() {
        long now = System.currentTimeMillis();
        long dt = Math.max(1, now - flingLast);
        flingLast = now;
        panBy((int) (velocityX * dt), (int) (velocityY * dt));
        float decay = (float) MathUtil.pow(0.995, dt);
        velocityX *= decay;
        velocityY *= decay;
        if (Math.abs(velocityX) < 0.02f && Math.abs(velocityY) < 0.02f) {
            flinging = false;
        }
    }

    @Override
    protected boolean mouseWheel(WheelEvent ev) {
        if (!zoomEnabled || !hasCartesian() || lastContext == null) {
            return false;
        }
        int x = toPaintX(ev.getX());
        int y = toPaintY(ev.getY());
        if (!insidePlot(x, y)) {
            return false;
        }
        boolean zoomGesture = ev.isControlDown() || ev.isMetaDown() || !ev.isPrecise();
        if (ev.isShiftDown() || !zoomGesture) {
            // trackpad two finger scroll and shift+wheel pan; unzoomed, the page scrolls
            if (!horizontalAxis().isZoomed() && !verticalAxis().isZoomed()) {
                return false;
            }
            int dx = ev.getDeltaX();
            int dy = ev.getDeltaY();
            if (ev.isShiftDown() && dx == 0) {
                dx = dy;
                dy = 0;
            }
            panBy(-dx, -dy);
            return true;
        }
        zoomAt(x, y, MathUtil.exp(-ev.getDeltaY() / (double) mm(25)), false);
        return true;
    }

    @Override
    public void pointerHover(int[] x, int[] y) {
        super.pointerHover(x, y);
        if (tooltipMode != TOOLTIP_NONE && x.length == 1) {
            updatePointer(toPaintX(x[0]), toPaintY(y[0]), true);
        }
    }

    @Override
    public void pointerHoverReleased(int[] x, int[] y) {
        super.pointerHoverReleased(x, y);
        if (hover != null || sharedIndex >= 0) {
            hover = null;
            sharedIndex = -1;
            repaint();
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        if (zoomEnabled && hasCartesian() && lastContext != null) {
            int action = Display.getInstance().getGameAction(keyCode);
            int step = lastContext.plotW / 10;
            int cx = lastContext.plotX + lastContext.plotW / 2;
            int cy = lastContext.plotY + lastContext.plotH / 2;
            if (action == Display.GAME_LEFT && horizontalAxis().isZoomed()) {
                panBy(step, 0);
                return;
            }
            if (action == Display.GAME_RIGHT && horizontalAxis().isZoomed()) {
                panBy(-step, 0);
                return;
            }
            if (keyCode == '+' || keyCode == '=') {
                zoomAt(cx, cy, 1.5, true);
                return;
            }
            if (keyCode == '-') {
                zoomAt(cx, cy, 1 / 1.5, true);
                return;
            }
            if (keyCode == '0') {
                resetZoom();
                return;
            }
        }
        super.keyReleased(keyCode);
    }

    // ------------------------------------------------------------------ synchronization

    /// Links this chart's x axis viewport and crosshair with other charts, as a price chart
    /// and its volume or indicator panes need: panning or zooming one pans the others.
    public void syncWith(Chart other) {
        if (other == this || syncGroup.contains(other)) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
            return;
        }
        List<Chart> merged = new ArrayList<Chart>(syncGroup);
        for (Chart c : other.syncGroup) {
            if (!merged.contains(c)) {
                merged.add(c);
            }
        }
        for (Chart c : merged) {
            c.syncGroup = merged;
        }
    }

    private void fireViewportChanged() {
        if (syncing) {
            return;
        }
        for (Chart c : syncGroup) {
            if (c == this) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                continue;
            }
            c.syncing = true;
            try {
                if (xAxis.isZoomed()) {
                    c.xAxis.setVisibleRange(xAxis.getVisibleMin(), xAxis.getVisibleMax());
                } else {
                    c.xAxis.resetViewport();
                }
                c.repaint();
            } finally {
                c.syncing = false;
            }
        }
    }

    private void fireCrosshair() {
        if (syncing) {
            return;
        }
        for (Chart c : syncGroup) {
            if (c == this) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                continue;
            }
            c.syncing = true;
            try {
                if (c.sharedIndex != sharedIndex) {
                    c.sharedIndex = sharedIndex;
                    c.repaint();
                }
            } finally {
                c.syncing = false;
            }
        }
    }
}
