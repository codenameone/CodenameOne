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
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.List;

/// A vertical list of events joined by a line: order tracking ("Placed",
/// "Packed", "Out for delivery"), an activity feed or a history.
///
/// ![An order timeline with done, current and pending steps](https://www.codenameone.com/developer-guide/img/generated/components-activity-timeline.png)
///
/// ```java
/// ActivityTimeline order = new ActivityTimeline();
/// order.addStep("Order placed", "We received your order", "9:02", ActivityTimeline.DONE);
/// order.addStep("Preparing", "The kitchen is on it", "9:05", ActivityTimeline.DONE);
/// order.addStep("On the way", "Dana is 6 minutes away", "9:21", ActivityTimeline.CURRENT);
/// order.addStep("Delivered", null, null, ActivityTimeline.PENDING);
/// form.add(order);
/// ```
///
/// A step is `#DONE`, `#CURRENT` or `#PENDING`. Done steps and the line up to
/// the current one take the accent colour; the current step is a ring; pending
/// steps and the line after the current one are faded. Change a step later with
/// `#setState(int, int)`.
///
/// It is unrelated to `com.codename1.ui.animations.Timeline`, which sequences
/// animation frames.
///
/// UIIDs: `ActivityTimeline` for the container, `ActivityTimelineTitle`,
/// `ActivityTimelineText` and `ActivityTimelineTime` for a step's texts,
/// falling back to `Label`. The marker colours follow the theme's `Slider`
/// colours unless `ActivityTimelineMarker` is styled.
public class ActivityTimeline extends Container {
    /// A step that has happened.
    public static final int DONE = 0;
    /// The step happening now.
    public static final int CURRENT = 1;
    /// A step that has not happened yet.
    public static final int PENDING = 2;

    private final List<Marker> markers = new ArrayList<Marker>();

    /// An empty timeline.
    public ActivityTimeline() {
        super(BoxLayout.y());
        setUIID("ActivityTimeline");
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }

    private String themed(String uiid, String fallback) {
        return getUIManager().isThemeStyleDefined(uiid) ? uiid : fallback;
    }

    /// Adds a step at the end.
    ///
    /// #### Parameters
    ///
    /// - `title`: the step's title
    ///
    /// - `text`: a line of detail, or null
    ///
    /// - `time`: when it happened or is due, or null
    ///
    /// - `state`: `#DONE`, `#CURRENT` or `#PENDING`
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public ActivityTimeline addStep(String title, String text, String time, int state) {
        Container content = new Container(BoxLayout.y());
        Label titleLabel = new Label(title == null ? "" : title, themed("ActivityTimelineTitle", "Label"));
        if (time != null) {
            Label timeLabel = new Label(time, themed("ActivityTimelineTime", "Label"));
            content.add(BorderLayout.centerEastWest(titleLabel, timeLabel, null));
        } else {
            content.add(titleLabel);
        }
        if (text != null) {
            SpanLabel detail = new SpanLabel(text);
            detail.setUIID("Container");
            detail.setTextUIID(themed("ActivityTimelineText", "Label"));
            content.add(detail);
        }
        Marker marker = new Marker(state);
        Container row = BorderLayout.center(content).add(BorderLayout.WEST, marker);
        add(row);
        markers.add(marker);
        refreshMarkers();
        return this;
    }

    /// Removes every step.
    public void clearSteps() {
        removeAll();
        markers.clear();
    }

    /// The number of steps.
    ///
    /// #### Returns
    ///
    /// the step count
    public int getStepCount() {
        return markers.size();
    }

    /// The state of a step.
    ///
    /// #### Parameters
    ///
    /// - `index`: the step's index
    ///
    /// #### Returns
    ///
    /// `#DONE`, `#CURRENT` or `#PENDING`
    public int getState(int index) {
        return marker(index).state;
    }

    /// Changes the state of a step, such as moving an order on.
    ///
    /// #### Parameters
    ///
    /// - `index`: the step's index
    ///
    /// - `state`: `#DONE`, `#CURRENT` or `#PENDING`
    public void setState(int index, int state) {
        marker(index).state = state;
        refreshMarkers();
        repaint();
    }

    private Marker marker(int index) {
        return markers.get(index);
    }

    private void refreshMarkers() {
        int n = markers.size();
        for (int i = 0; i < n; i++) {
            Marker m = marker(i);
            m.first = i == 0;
            m.last = i == n - 1;
            m.nextReached = !m.last && marker(i + 1).state != PENDING;
        }
    }

    /// The dot and the connecting line beside a step.
    private final class Marker extends Component {
        int state;
        boolean first;
        boolean last;
        boolean nextReached;

        Marker(int state) {
            this.state = state;
            setUIID("Container");
            getAllStyles().setBgTransparency(0);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(8), CN.convertToPixels(6));
        }

        @Override
        public void paint(Graphics g) {
            int dot = CN.convertToPixels(3);
            int line = Math.max(1, CN.convertToPixels(0.5f));
            int cx = getX() + getWidth() / 2;
            int top = getY() + CN.convertToPixels(1);
            int cy = top + dot / 2;
            int accent = Palette.accent(ActivityTimeline.this, "ActivityTimelineMarker");
            int faded = Palette.track(ActivityTimeline.this, "ActivityTimelineMarker");
            int color = g.getColor();
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            if (!first) {
                g.setColor(state == PENDING ? faded : accent);
                g.fillRect(cx - line / 2, getY(), line, cy - getY());
            }
            if (!last) {
                g.setColor(nextReached ? accent : faded);
                g.fillRect(cx - line / 2, cy, line, getY() + getHeight() - cy);
            }
            if (state == DONE) {
                g.setColor(accent);
                g.fillArc(cx - dot / 2, cy - dot / 2, dot, dot, 0, 360);
            } else if (state == CURRENT) {
                g.setColor(accent);
                g.fillArc(cx - dot / 2, cy - dot / 2, dot, dot, 0, 360);
                int inner = dot / 2;
                g.setColor(Palette.background(ActivityTimeline.this));
                g.fillArc(cx - inner / 2, cy - inner / 2, inner, inner, 0, 360);
            } else {
                g.setColor(faded);
                g.fillArc(cx - dot / 2, cy - dot / 2, dot, dot, 0, 360);
            }
            g.setColor(color);
            g.setAntiAliased(aa);
        }
    }
}
