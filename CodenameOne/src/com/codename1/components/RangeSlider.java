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
package com.codename1.components;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

/// A slider with two thumbs that selects a range, such as a price filter or
/// a time window.
///
/// ![A price range slider with both thumbs](https://www.codenameone.com/developer-guide/img/generated/components-range-slider.png)
///
/// ```java
/// RangeSlider price = new RangeSlider(0, 500, 50, 300);
/// price.setStep(10);
/// Label shown = new Label("$50 - $300");
/// price.addActionListener(e -> shown.setText(
///         "$" + price.getLowerValue() + " - $" + price.getUpperValue()));
/// form.add(price).add(shown);
/// ```
///
/// Dragging moves whichever thumb is nearer the touch; the thumbs never cross.
/// Listeners fire while the value changes and once more when the drag ends,
/// with `#isAdjusting()` false for that last event.
///
/// UIIDs: `RangeSlider` for the component. The fill takes the foreground of
/// `RangeSliderFill` and the track the background of `RangeSliderTrack` when
/// the theme defines them, and otherwise follow the theme's `Slider` colours.
public class RangeSlider extends Component {
    private static final float TRACK_MM = 1f;
    private static final float THUMB_MM = 4.5f;

    private int minValue;
    private int maxValue;
    private int lowerValue;
    private int upperValue;
    private int step = 1;
    private boolean adjusting;
    private boolean changedWhileAdjusting;
    private int dragging = -1;
    private final EventDispatcher listeners = new EventDispatcher();

    /// A slider over 0 to 100 selecting all of it.
    public RangeSlider() {
        this(0, 100, 0, 100);
    }

    /// A slider over `min` to `max` selecting `lower` to `upper`.
    ///
    /// #### Parameters
    ///
    /// - `min`: the smallest value
    ///
    /// - `max`: the largest value, greater than `min`
    ///
    /// - `lower`: the start of the selected range
    ///
    /// - `upper`: the end of the selected range
    public RangeSlider(int min, int max, int lower, int upper) {
        if (max <= min) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        setUIID("RangeSlider");
        setFocusable(true);
        minValue = min;
        maxValue = max;
        setRange(lower, upper);
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }

    /// The smallest value.
    ///
    /// #### Returns
    ///
    /// the minimum
    public int getMinValue() {
        return minValue;
    }

    /// The largest value.
    ///
    /// #### Returns
    ///
    /// the maximum
    public int getMaxValue() {
        return maxValue;
    }

    /// The start of the selected range.
    ///
    /// #### Returns
    ///
    /// the lower value
    public int getLowerValue() {
        return lowerValue;
    }

    /// The end of the selected range.
    ///
    /// #### Returns
    ///
    /// the upper value
    public int getUpperValue() {
        return upperValue;
    }

    /// Sets the selected range. Values are clamped to the slider's bounds and
    /// snapped to the step, and swapped if given in the wrong order.
    ///
    /// #### Parameters
    ///
    /// - `lower`: the start of the range
    ///
    /// - `upper`: the end of the range
    public final void setRange(int lower, int upper) {
        int lo = snap(Math.min(lower, upper));
        int hi = snap(Math.max(lower, upper));
        if (lo != lowerValue || hi != upperValue) {
            lowerValue = lo;
            upperValue = hi;
            repaint();
            fire();
        }
    }

    /// Sets the slider's bounds, keeping the selection inside them.
    ///
    /// #### Parameters
    ///
    /// - `min`: the smallest value
    ///
    /// - `max`: the largest value, greater than `min`
    public void setBounds(int min, int max) {
        if (max <= min) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        minValue = min;
        maxValue = max;
        int lo = lowerValue;
        int hi = upperValue;
        lowerValue = Integer.MIN_VALUE;
        setRange(lo, hi);
    }

    /// The increment the values snap to.
    ///
    /// #### Returns
    ///
    /// the step, 1 by default
    public int getStep() {
        return step;
    }

    /// Sets the increment the values snap to, counted from the minimum.
    ///
    /// #### Parameters
    ///
    /// - `step`: a positive step
    public void setStep(int step) {
        if (step < 1) {
            throw new IllegalArgumentException("step must be positive");
        }
        this.step = step;
        int lo = lowerValue;
        int hi = upperValue;
        lowerValue = Integer.MIN_VALUE;
        setRange(lo, hi);
    }

    /// True while the user is dragging a thumb. Listeners hear every change
    /// during a drag with this true, then once more with it false when the
    /// finger lifts; a listener that runs a search can ignore the first kind.
    ///
    /// #### Returns
    ///
    /// whether a drag is in progress
    public boolean isAdjusting() {
        return adjusting;
    }

    /// Adds a listener called whenever the range changes.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    private void fire() {
        if (adjusting) {
            changedWhileAdjusting = true;
        }
        listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Change));
    }

    private int snap(int v) {
        int c = Math.max(minValue, Math.min(maxValue, v));
        int steps = (c - minValue + step / 2) / step;
        return Math.min(maxValue, minValue + steps * step);
    }

    private int thumbSize() {
        return CN.convertToPixels(THUMB_MM);
    }

    private int trackLeft() {
        return getX() + getStyle().getPaddingLeftNoRTL() + thumbSize() / 2;
    }

    private int trackWidth() {
        return Math.max(1, getWidth() - getStyle().getHorizontalPadding() - thumbSize());
    }

    /// The x coordinate of a value's thumb centre, in parent coordinates.
    int positionOf(int value) {
        float fraction = (value - minValue) / (float) (maxValue - minValue);
        if (isRTL()) {
            fraction = 1 - fraction;
        }
        return trackLeft() + Math.round(fraction * trackWidth());
    }

    /// The value at an x coordinate in parent coordinates.
    int valueAt(int x) {
        float fraction = (x - trackLeft()) / (float) trackWidth();
        fraction = Math.max(0, Math.min(1, fraction));
        if (isRTL()) {
            fraction = 1 - fraction;
        }
        return snap(minValue + Math.round(fraction * (maxValue - minValue)));
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        return new Dimension(CN.convertToPixels(40) + s.getHorizontalPadding(),
                thumbSize() + s.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Style s = getStyle();
        int thumb = thumbSize();
        int track = Math.max(1, CN.convertToPixels(TRACK_MM));
        int cy = getY() + s.getPaddingTop() + (getHeight() - s.getVerticalPadding()) / 2;
        int left = trackLeft();
        int width = trackWidth();
        int lo = positionOf(lowerValue);
        int hi = positionOf(upperValue);
        int start = Math.min(lo, hi);
        int end = Math.max(lo, hi);
        int color = g.getColor();
        int alpha = g.getAlpha();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        if (!isEnabled()) {
            g.setAlpha(alpha / 2);
        }
        g.setColor(Palette.track(this, "RangeSliderTrack"));
        g.fillRoundRect(left, cy - track / 2, width, track, track, track);
        int accent = Palette.accent(this, "RangeSliderFill");
        g.setColor(accent);
        g.fillRect(start, cy - track / 2, end - start, track);
        paintThumb(g, lo, cy, thumb, accent);
        paintThumb(g, hi, cy, thumb, accent);
        g.setAlpha(alpha);
        g.setColor(color);
        g.setAntiAliased(aa);
    }

    private void paintThumb(Graphics g, int cx, int cy, int size, int accent) {
        g.setColor(accent);
        g.fillArc(cx - size / 2, cy - size / 2, size, size, 0, 360);
        int inner = size / 3;
        g.setColor(Palette.isDark(accent) ? 0xffffff : 0x000000);
        g.fillArc(cx - inner / 2, cy - inner / 2, inner, inner, 0, 360);
    }

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        if (!isEnabled()) {
            return;
        }
        int px = x - getAbsoluteX() + getX();
        int lo = positionOf(lowerValue);
        int hi = positionOf(upperValue);
        if (lo == hi) {
            // The lower thumb is the one on the side of the smaller values,
            // which is the right in RTL.
            boolean before = isRTL() ? px > lo : px < lo;
            dragging = before ? 0 : 1;
            if (lowerValue == maxValue) {
                dragging = 0;
            } else if (upperValue == minValue) {
                dragging = 1;
            }
        } else {
            dragging = Math.abs(px - lo) <= Math.abs(px - hi) ? 0 : 1;
        }
        adjusting = true;
        changedWhileAdjusting = false;
        moveTo(px);
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (dragging < 0) {
            super.pointerDragged(x, y);
            return;
        }
        moveTo(x - getAbsoluteX() + getX());
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if (dragging >= 0) {
            dragging = -1;
            adjusting = false;
            // One more event with isAdjusting() false, for listeners that wait
            // for the final value; none when the gesture moved nothing.
            if (changedWhileAdjusting) {
                fire();
            }
        }
    }

    @Override
    protected void deinitialize() {
        // A drag that never saw its release (the form changed) must not stay open.
        dragging = -1;
        adjusting = false;
        super.deinitialize();
    }

    @Override
    protected boolean isStickyDrag() {
        return dragging >= 0;
    }

    private void moveTo(int px) {
        int v = valueAt(px);
        if (dragging == 0) {
            setRange(Math.min(v, upperValue), upperValue);
        } else {
            setRange(lowerValue, Math.max(v, lowerValue));
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        int delta = 0;
        if (game == Display.GAME_LEFT) {
            delta = isRTL() ? step : -step;
        } else if (game == Display.GAME_RIGHT) {
            delta = isRTL() ? -step : step;
        }
        if (delta == 0) {
            super.keyReleased(keyCode);
            return;
        }
        // The arrow keys move the whole window, keeping its width.
        if (lowerValue + delta >= minValue && upperValue + delta <= maxValue) {
            setRange(lowerValue + delta, upperValue + delta);
        }
    }
}
