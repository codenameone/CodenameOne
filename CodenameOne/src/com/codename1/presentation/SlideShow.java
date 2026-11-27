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
package com.codename1.presentation;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.wordprocessing.DocumentLayout;
import com.codename1.wordprocessing.LayoutPainter;
import java.util.Map;

/// Plays a presentation: slide transitions, builds, and charts that animate in as their
/// slide appears.
///
/// Tap the right two thirds, swipe left, or press right, down, space or fire to advance; tap
/// the left third, swipe right, or press left or up to go back. Hidden slides are skipped.
/// In presenter mode the slide shares the screen with the next slide, the speaker notes and
/// the elapsed time, for a second display or a rehearsal.
///
/// ```java
/// void play(byte[] bytes) throws IOException {
///     Form f = new Form("Deck", new BorderLayout());
///     SlideShow show = new SlideShow(Presentation.read(bytes));
///     show.addSlideListener(e -> Log.p("now on slide " + show.getSlideIndex()));
///     f.add(BorderLayout.CENTER, show);
///     f.show();
/// }
/// ```
///
/// Transitions and builds move on `com.codename1.ui.animations.Motion` against
/// `AnimationTime`, and the frames are painted by `SlideAnimation`, so a frame at any
/// progress can be rendered exactly. Charts on screen are live `com.codename1.ui.chart.Chart`
/// components that play their own entrance animation.
public class SlideShow extends Container {
    private static final int BACKDROP = 0x000000;
    private static final int PANEL = 0x1b1f24;
    private static final int PANEL_TEXT = 0xe6e9ed;

    private final SlideRenderer renderer = new SlideRenderer();
    private final EventDispatcher listeners = new EventDispatcher();
    private Presentation presentation;
    private int index = -1;
    private int step;
    private Slide from;
    private Map<String, SlideRenderer.ElementState> fromStates;
    private Motion transition;
    private Motion stepMotion;
    private long slideShownAt;
    private long startedAt;
    private boolean presenter;
    private boolean liveCharts = true;
    private boolean registered;
    private int pressX = -1;
    private int pressY = -1;

    /// A slide show of a presentation, on its first visible slide.
    ///
    /// #### Parameters
    ///
    /// - `presentation`: the presentation
    public SlideShow(Presentation presentation) {
        super(new ChartPlacement());
        setUIIDFinal("SlideShow");
        setFocusable(true);
        setGrabsPointerEvents(true);
        setPresentation(presentation);
    }

    /// Shows another presentation from its first visible slide.
    ///
    /// #### Parameters
    ///
    /// - `value`: the presentation
    public final void setPresentation(Presentation value) {
        if (value == null) {
            throw new IllegalArgumentException("presentation");
        }
        presentation = value;
        index = -1;
        from = null;
        transition = null;
        int first = nextVisible(-1, 1);
        show(first < 0 ? 0 : first, 0, false);
    }

    /// The presentation.
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation getPresentation() {
        return presentation;
    }

    /// The index of the slide showing.
    ///
    /// #### Returns
    ///
    /// the index into `Presentation.getSlides()`
    public int getSlideIndex() {
        return index;
    }

    /// How many click steps of the current slide's builds have played.
    ///
    /// #### Returns
    ///
    /// the step
    public int getBuildStep() {
        return step;
    }

    /// The renderer slides are painted with.
    ///
    /// #### Returns
    ///
    /// the renderer
    public SlideRenderer getRenderer() {
        return renderer;
    }

    /// Turns presenter mode on or off: the next slide, the notes and a timer beside the
    /// slide.
    ///
    /// #### Parameters
    ///
    /// - `value`: true for presenter mode
    public void setPresenterMode(boolean value) {
        presenter = value;
        startedAt = AnimationTime.now();
        setShouldLayout(true);
        revalidateLater();
        repaint();
    }

    /// True in presenter mode.
    ///
    /// #### Returns
    ///
    /// true when presenting
    public boolean isPresenterMode() {
        return presenter;
    }

    /// Chooses whether charts are live components that animate in, the default, or painted
    /// as still pictures.
    ///
    /// #### Parameters
    ///
    /// - `value`: false for still charts
    public void setLiveCharts(boolean value) {
        liveCharts = value;
        syncCharts();
    }

    /// Adds a listener told after every change of slide or build step.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addSlideListener(ActionListener<ActionEvent> l) {
        listeners.addListener(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeSlideListener(ActionListener<ActionEvent> l) {
        listeners.removeListener(l);
    }

    private int nextVisible(int from, int dir) {
        int n = presentation.slides.size();
        for (int i = from + dir; i >= 0 && i < n; i += dir) {
            if (!presentation.slides.get(i).hidden) {
                return i;
            }
        }
        return -1;
    }

    private Slide current() {
        return index >= 0 && index < presentation.slides.size()
                ? presentation.slides.get(index) : null;
    }

    /// Plays the next build step, or moves to the next slide when every build has played.
    ///
    /// #### Returns
    ///
    /// false at the end of the presentation
    public boolean next() {
        Slide s = current();
        if (s == null) {
            return false;
        }
        finishMotions();
        if (step < SlideAnimation.clickSteps(s)) {
            step++;
            startStep();
            fire();
            repaint();
            return true;
        }
        int n = nextVisible(index, 1);
        if (n < 0) {
            return false;
        }
        show(n, 0, true);
        return true;
    }

    /// Undoes the last build step, or moves to the previous slide with all of its builds
    /// played.
    ///
    /// #### Returns
    ///
    /// false at the start of the presentation
    public boolean previous() {
        Slide s = current();
        if (s == null) {
            return false;
        }
        finishMotions();
        if (step > 0) {
            step--;
            stepMotion = null;
            fire();
            repaint();
            return true;
        }
        int n = nextVisible(index, -1);
        if (n < 0) {
            return false;
        }
        show(n, SlideAnimation.clickSteps(presentation.slides.get(n)), false);
        return true;
    }

    /// Jumps to a slide with no transition and none of its click builds played.
    ///
    /// #### Parameters
    ///
    /// - `slideIndex`: the slide
    public void goTo(int slideIndex) {
        if (slideIndex < 0 || slideIndex >= presentation.slides.size()) {
            throw new IndexOutOfBoundsException("slide " + slideIndex);
        }
        finishMotions();
        show(slideIndex, 0, false);
    }

    private void finishMotions() {
        transition = null;
        from = null;
        stepMotion = null;
    }

    private void show(int i, int atStep, boolean animate) {
        Slide old = current();
        index = i;
        step = atStep;
        Slide s = current();
        if (animate && old != null && s != null && !Transition.NONE.equals(s.transition.type)
                && s.transition.duration > 0) {
            from = old;
            fromStates = SlideAnimation.states(old, SlideAnimation.clickSteps(old),
                    Long.MAX_VALUE / 4);
            transition = motion(s.transition.duration);
        } else {
            from = null;
            transition = null;
        }
        slideShownAt = AnimationTime.now();
        // the automatic step plays once the slide is in
        stepMotion = null;
        if (s != null && step == 0 && SlideAnimation.stepDuration(s, 0) > 0) {
            startStep();
        }
        syncCharts();
        fire();
        repaint();
    }

    private static Motion motion(int duration) {
        Motion m = Motion.createLinearMotion(0, 1000, Math.max(1, duration));
        m.start();
        return m;
    }

    private void startStep() {
        Slide s = current();
        int d = s == null ? 0 : SlideAnimation.stepDuration(s, step);
        stepMotion = d > 0 ? motion(d) : null;
        syncCharts();
    }

    private void fire() {
        if (listeners.hasListeners()) {
            listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Other));
        }
    }

    // ------------------------------------------------------------- live charts

    /// Puts a live chart over every chart element of the current slide that no build or
    /// transition is animating, and takes the others away.
    private void syncCharts() {
        removeAll();
        Slide s = current();
        if (s != null && liveCharts && transition == null) {
            java.util.List<String> built = SlideAnimation.builtIds(s);
            for (SlideElement e : s.elements) {
                if (e instanceof ChartElement && !built.contains(e.id) && e.rotation == 0) {
                    Chart c = ((ChartElement) e).toChart();
                    c.putClientProperty("cn1$slideElement", e);
                    add(c);
                }
            }
        }
        setShouldLayout(true);
        revalidateLater();
    }

    private Map<String, SlideRenderer.ElementState> statesNow(Slide s) {
        long elapsed = stepMotion == null ? Long.MAX_VALUE / 4 : stepElapsed(s);
        Map<String, SlideRenderer.ElementState> m = SlideAnimation.states(s, step, elapsed);
        // a live chart draws itself
        for (int i = 0; i < getComponentCount(); i++) {
            Object e = getComponentAt(i).getClientProperty("cn1$slideElement");
            if (e instanceof SlideElement) {
                SlideRenderer.ElementState st = new SlideRenderer.ElementState();
                st.hidden = true;
                m.put(((SlideElement) e).id, st);
            }
        }
        return m;
    }

    private long stepElapsed(Slide s) {
        return Math.round(stepMotion.getValue() / 1000.0
                * SlideAnimation.stepDuration(s, step));
    }

    // ------------------------------------------------------------- geometry

    /// The slide area in this component's coordinates: x, y, width, height and the scale.
    float[] slideArea() {
        int w = getWidth();
        int h = getHeight();
        int availW = presenter ? Math.round(w * 0.64f) : w;
        float scale = Math.min(availW / presentation.width, h / presentation.height);
        float sw = presentation.width * scale;
        float sh = presentation.height * scale;
        float sx = presenter ? Math.max(0, (availW - sw) / 2) : (w - sw) / 2;
        float sy = (h - sh) / 2;
        return new float[] {sx, sy, sw, sh, scale};
    }

    /// Sizes a live chart's text and padding in the proportion the painted chart has.
    ///
    /// `SlideRenderer.chartImage` draws a chart at the size it has on a slide shown at its
    /// real size and scales the picture into place, because a chart sizes its text for the
    /// device. A live chart placed at screen pixels did not: on a phone, where a slide is
    /// shown at well under its real size, the title and legend kept their full size and
    /// left the plot a sliver, and every chart visibly jumped when the painted picture
    /// handed over to the live one at the end of a transition. Scaling the font and padding
    /// by the same ratio makes the two agree. A font that is not a TrueType font cannot be
    /// derived to an arbitrary size and is left alone.
    static void fitText(Component c, float scale) {
        float ratio = scale / SlideRenderer.chartPixelsPerPoint();
        // Each state keeps its own font and padding (a selected style may be bold), so each
        // is scaled from its own base.
        fitStyle(c, c.getUnselectedStyle(), "u", ratio);
        fitStyle(c, c.getSelectedStyle(), "s", ratio);
        fitStyle(c, c.getPressedStyle(), "p", ratio);
        fitStyle(c, c.getDisabledStyle(), "d", ratio);
        c.putClientProperty("cn1$textRatio", Float.valueOf(ratio));
    }

    /// Scales one style from its base. The base is the style as the application left it:
    /// when the style no longer holds what the last fit wrote, the application changed it,
    /// and that becomes the new base instead of being overwritten by a stale one.
    private static void fitStyle(Component c, Style s, String key, float ratio) {
        Object saved = c.getClientProperty("cn1$textBase" + key);
        Object[] base = saved instanceof Object[] ? (Object[]) saved : null;
        int[] now = {s.getPaddingTop(), s.getPaddingBottom(), s.getPaddingLeftNoRTL(),
            s.getPaddingRightNoRTL()};
        // Identity on purpose: the font this method wrote last time, not an equal one.
        if (base == null || base[2] != s.getFont() || !samePadding((int[]) base[3], now)) { // NOPMD CompareObjectsWithEquals
            base = new Object[] {s.getFont(), now, null, null};
        } else if (base[4] instanceof Float
                && Math.abs(((Float) base[4]).floatValue() - ratio) < 0.005f) {
            return;
        }
        Font font = (Font) base[0];
        if (font != null && font.isTTFNativeFont()) {
            s.setFont(font.derive(Math.max(1f, font.getPixelSize() * ratio), font.getStyle()));
        }
        int[] pad = (int[]) base[1];
        s.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS,
                Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        s.setPadding(Math.round(pad[0] * ratio), Math.round(pad[1] * ratio),
                Math.round(pad[2] * ratio), Math.round(pad[3] * ratio));
        int[] written = {s.getPaddingTop(), s.getPaddingBottom(), s.getPaddingLeftNoRTL(),
            s.getPaddingRightNoRTL()};
        c.putClientProperty("cn1$textBase" + key,
                new Object[] {base[0], base[1], s.getFont(), written, Float.valueOf(ratio)});
    }

    private static boolean samePadding(int[] a, int[] b) {
        for (int i = 0; i < 4; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /// Places each live chart over its element.
    static final class ChartPlacement extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            if (!(parent instanceof SlideShow)) {
                return;
            }
            float[] a = ((SlideShow) parent).slideArea();
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponentAt(i);
                Object o = c.getClientProperty("cn1$slideElement");
                if (o instanceof SlideElement) {
                    SlideElement e = (SlideElement) o;
                    c.setX(Math.round(a[0] + e.x * a[4]));
                    c.setY(Math.round(a[1] + e.y * a[4]));
                    c.setWidth(Math.max(1, Math.round(e.width * a[4])));
                    c.setHeight(Math.max(1, Math.round(e.height * a[4])));
                    fitText(c, a[4]);
                }
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(Display.getInstance().convertToPixels(80),
                    Display.getInstance().convertToPixels(45));
        }
    }

    // ------------------------------------------------------------- painting

    @Override
    public void paintBackground(Graphics g) {
        g.setColor(presenter ? PANEL : BACKDROP);
        g.fillRect(getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public void paint(Graphics g) {
        Slide s = current();
        if (s == null) {
            return;
        }
        float[] a = slideArea();
        int x = getX() + Math.round(a[0]);
        int y = getY() + Math.round(a[1]);
        if (transition != null && from != null) {
            float t = transition.getValue() / 1000f;
            SlideAnimation.paintTransition(g, renderer, presentation, from, fromStates, s,
                    statesNow(s), x, y, a[4], t);
        } else {
            renderer.paint(g, presentation, s, x, y, a[4], statesNow(s));
        }
        if (presenter) {
            paintPresenter(g);
        }
        // the live charts
        super.paint(g);
    }

    private void paintPresenter(Graphics g) {
        int left = getX() + Math.round(getWidth() * 0.64f) + 8;
        int w = getX() + getWidth() - left - 8;
        if (w < 20) {
            return;
        }
        int top = getY() + 8;
        Font f = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(f);
        g.setColor(PANEL_TEXT);
        long secs = (AnimationTime.now() - startedAt) / 1000;
        String clock = (secs / 60) + ":" + (secs % 60 < 10 ? "0" : "") + (secs % 60) + "   "
                + (index + 1) + " / " + presentation.slides.size();
        g.drawString(clock, left, top);
        top += f.getHeight() + 6;
        int n = nextVisible(index, 1);
        if (n >= 0) {
            float scale = w / presentation.width;
            renderer.paint(g, presentation, presentation.slides.get(n), left, top, scale);
            top += Math.round(presentation.height * scale) + 10;
        }
        Slide s = current();
        if (s != null && !s.notes.isEmpty()) {
            float pts = w / 1.5f;
            TextFrame notes = new TextFrame(s.notes);
            DocumentLayout l = new DocumentLayout(notes.toDocument(pts, 10000),
                    renderer.getMeasurer());
            if (l.getPageCount() > 0) {
                g.setColor(0xffffff);
                g.fillRect(left, top, w, getY() + getHeight() - top - 8);
                g.pushClip();
                g.clipRect(left, top, w, getY() + getHeight() - top - 8);
                new LayoutPainter(renderer.getMeasurer()).paint(g, l.getPage(0).getItems(), left,
                        top, 1.5f);
                g.popClip();
            }
        }
    }

    // ------------------------------------------------------------- animation and input

    @Override
    protected void initComponent() {
        super.initComponent();
        Form f = getComponentForm();
        if (f != null && !registered) {
            f.registerAnimated(this);
            registered = true;
        }
        if (startedAt == 0) {
            // only the first time: re-adding the show (a rotation, a form switch) keeps the
            // presenter's clock running
            startedAt = AnimationTime.now();
        }
    }

    @Override
    protected void deinitialize() {
        Form f = getComponentForm();
        if (f != null && registered) {
            f.deregisterAnimated(this);
        }
        registered = false;
        super.deinitialize();
    }

    @Override
    public boolean animate() {
        boolean changed = false;
        if (transition != null) {
            changed = true;
            if (transition.isFinished()) {
                transition = null;
                from = null;
                syncCharts();
            }
        }
        if (stepMotion != null) {
            changed = true;
            if (stepMotion.isFinished()) {
                stepMotion = null;
            }
        }
        Slide s = current();
        if (s != null && transition == null && stepMotion == null && s.transition != null) {
            // advance after a time, once every build is done
            int after = s.transition.advanceAfter;
            if (after >= 0 && step >= SlideAnimation.clickSteps(s)
                    && AnimationTime.now() - slideShownAt >= after) {
                if (!next()) {
                    slideShownAt = Long.MAX_VALUE / 4;
                }
                changed = true;
            }
        }
        if (presenter) {
            changed = true;
        }
        return super.animate() || changed;
    }

    @Override
    public void keyReleased(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        if (game == Display.GAME_RIGHT || game == Display.GAME_DOWN || game == Display.GAME_FIRE
                || keyCode == ' ') {
            next();
        } else if (game == Display.GAME_LEFT || game == Display.GAME_UP) {
            previous();
        } else {
            super.keyReleased(keyCode);
        }
    }

    @Override
    public void pointerPressed(int x, int y) {
        pressX = x;
        pressY = y;
        super.pointerPressed(x, y);
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if (pressX < 0) {
            return;
        }
        int dx = x - pressX;
        int dy = y - pressY;
        int threshold = Display.getInstance().convertToPixels(6);
        pressX = -1;
        if (Math.abs(dx) > threshold && Math.abs(dx) > Math.abs(dy)) {
            if (dx < 0) {
                next();
            } else {
                previous();
            }
        } else if (Math.abs(dx) <= threshold && Math.abs(dy) <= threshold) {
            if (x - getAbsoluteX() < getWidth() / 3) {
                previous();
            } else {
                next();
            }
        }
    }
}
