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

import com.codename1.ui.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The timing of a slide's builds and the frames of its transitions, as pure functions of
/// progress. `SlideShow` plays them against the animation clock; a test or a thumbnail strip
/// renders any frame of them deterministically.
///
/// A slide's builds play in *steps*: each `Build.ON_CLICK` build starts a step, and the
/// `WITH_PREVIOUS` and `AFTER_PREVIOUS` builds after it belong to the same step. Builds
/// before the first click build form step 0, which plays as soon as the slide appears.
public final class SlideAnimation {
    private SlideAnimation() {
    }

    /// The number of steps a slide's builds take, not counting the automatic first one when
    /// it is empty.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide
    ///
    /// #### Returns
    ///
    /// the click count to get through every build
    public static int clickSteps(Slide slide) {
        int n = 0;
        for (Build b : slide.builds) {
            if (Build.ON_CLICK.equals(b.trigger)) {
                n++;
            }
        }
        return n;
    }

    /// Which step every build is in, 0 being the automatic one.
    static int[] steps(Slide slide) {
        int[] s = new int[slide.builds.size()];
        int step = 0;
        for (int i = 0; i < s.length; i++) {
            if (Build.ON_CLICK.equals(slide.builds.get(i).trigger)) {
                step++;
            }
            s[i] = step;
        }
        return s;
    }

    /// When each build starts within its step, in milliseconds.
    static int[] starts(Slide slide) {
        int[] st = new int[slide.builds.size()];
        int prevStart = 0;
        int prevEnd = 0;
        for (int i = 0; i < st.length; i++) {
            Build b = slide.builds.get(i);
            int start;
            if (Build.ON_CLICK.equals(b.trigger) || i == 0) {
                start = 0;
                prevEnd = 0;
            } else if (Build.WITH_PREVIOUS.equals(b.trigger)) {
                start = prevStart;
            } else {
                start = prevEnd;
            }
            start += Math.max(0, b.delay);
            st[i] = start;
            prevStart = start;
            prevEnd = Math.max(prevEnd, start + Math.max(0, b.duration));
        }
        return st;
    }

    /// How long a step takes in milliseconds.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide
    ///
    /// - `step`: the step, 0 for the automatic one
    ///
    /// #### Returns
    ///
    /// the duration, 0 for a step with no builds
    public static int stepDuration(Slide slide, int step) {
        int[] steps = steps(slide);
        int[] starts = starts(slide);
        int end = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == step) {
                end = Math.max(end, starts[i] + Math.max(0, slide.builds.get(i).duration));
            }
        }
        return end;
    }

    /// The state of every built element while `step` is `elapsed` milliseconds in: the
    /// builds of earlier steps are done, later ones not yet started.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide
    ///
    /// - `step`: the step playing
    ///
    /// - `elapsed`: milliseconds into it; a large value shows the step finished
    ///
    /// #### Returns
    ///
    /// element states by id, for `SlideRenderer.paint`
    public static Map<String, SlideRenderer.ElementState> states(Slide slide, int step,
            long elapsed) {
        Map<String, SlideRenderer.ElementState> m =
                new HashMap<String, SlideRenderer.ElementState>();
        int[] steps = steps(slide);
        int[] starts = starts(slide);
        // an element's first build decides how it looks before any build ran
        for (int i = 0; i < steps.length; i++) {
            Build b = slide.builds.get(i);
            if (!m.containsKey(b.elementId)) {
                SlideRenderer.ElementState s = new SlideRenderer.ElementState();
                s.hidden = Build.ENTRANCE.equals(b.kind);
                m.put(b.elementId, s);
            }
        }
        for (int i = 0; i < steps.length; i++) {
            Build b = slide.builds.get(i);
            float t;
            if (steps[i] < step) {
                t = 1;
            } else if (steps[i] > step) {
                continue;
            } else {
                long into = elapsed - starts[i];
                if (into < 0) {
                    continue;
                }
                t = b.duration <= 0 ? 1 : Math.min(1, into / (float) b.duration);
            }
            apply(m.get(b.elementId), b, ease(t));
        }
        return m;
    }

    /// Ease in and out, the curve every build and transition moves on.
    static float ease(float t) {
        float c = Math.max(0, Math.min(1, t));
        return c * c * (3 - 2 * c);
    }

    private static void apply(SlideRenderer.ElementState s, Build b, float t) {
        boolean exit = Build.EXIT.equals(b.kind);
        float shown = exit ? 1 - t : t;
        s.hidden = false;
        s.alpha = 1;
        s.dx = 0;
        s.dy = 0;
        s.scale = 1;
        s.reveal = 1;
        if (Build.EMPHASIS.equals(b.kind)) {
            // a pulse: up and back
            s.scale = 1 + 0.12f * (float) Math.sin(Math.PI * t);
            return;
        }
        if (Build.APPEAR.equals(b.effect)) {
            s.hidden = exit ? t >= 1 : false;
        } else if (Build.FADE.equals(b.effect)) {
            s.alpha = shown;
        } else if (Build.FLY.equals(b.effect)) {
            float d = (1 - shown) * 600;
            String dir = b.direction == null ? Transition.FROM_BOTTOM : b.direction;
            if (Transition.FROM_LEFT.equals(dir)) {
                s.dx = -d;
            } else if (Transition.FROM_RIGHT.equals(dir)) {
                s.dx = d;
            } else if (Transition.FROM_TOP.equals(dir)) {
                s.dy = -d;
            } else {
                s.dy = d;
            }
            s.alpha = Math.min(1, shown * 3);
        } else if (Build.ZOOM.equals(b.effect)) {
            s.scale = 0.3f + 0.7f * shown;
            s.alpha = shown;
        } else if (Build.WIPE.equals(b.effect)) {
            s.reveal = shown;
        }
        if (s.alpha <= 0 && exit) {
            s.hidden = true;
        }
    }

    /// Paints a frame of the transition into `to`: the slide leaving is `from`, which may be
    /// null for the first slide. Both slides are drawn into a box of `w` by `h` pixels at
    /// (`x`, `y`).
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `renderer`: paints the slides
    ///
    /// - `p`: the presentation
    ///
    /// - `from`: the slide leaving, or null
    ///
    /// - `fromStates`: its element states, or null
    ///
    /// - `to`: the slide arriving, whose transition is played
    ///
    /// - `toStates`: its element states, or null
    ///
    /// - `x`: left of the slide area in pixels
    ///
    /// - `y`: top of it
    ///
    /// - `scale`: pixels per point
    ///
    /// - `progress`: 0 to 1
    public static void paintTransition(Graphics g, SlideRenderer renderer, Presentation p,
            Slide from, Map<String, SlideRenderer.ElementState> fromStates, Slide to,
            Map<String, SlideRenderer.ElementState> toStates, int x, int y, float scale,
            float progress) {
        float t = ease(progress);
        Transition tr = to.transition;
        int w = Math.round(p.width * scale);
        int h = Math.round(p.height * scale);
        if (from == null || t >= 1 || Transition.NONE.equals(tr.type)) {
            if (from != null && t < 1 && Transition.NONE.equals(tr.type)) {
                renderer.paint(g, p, from, x, y, scale, fromStates);
                return;
            }
            renderer.paint(g, p, to, x, y, scale, toStates);
            return;
        }
        int dx = 0;
        int dy = 0;
        String dir = tr.direction;
        // the direction the new slide comes from
        if (Transition.FROM_LEFT.equals(dir)) {
            dx = -w;
        } else if (Transition.FROM_TOP.equals(dir)) {
            dy = -h;
        } else if (Transition.FROM_BOTTOM.equals(dir)) {
            dy = h;
        } else {
            dx = w;
        }
        int alpha = g.getAlpha();
        g.pushClip();
        g.clipRect(x, y, w, h);
        try {
            String type = tr.type;
            if (Transition.FADE.equals(type)) {
                renderer.paint(g, p, from, x, y, scale, fromStates);
                g.setAlpha(Math.round(255 * t));
                renderer.paint(g, p, to, x, y, scale, toStates);
            } else if (Transition.PUSH.equals(type)) {
                int ox = Math.round(-dx * t);
                int oy = Math.round(-dy * t);
                renderer.paint(g, p, from, x + ox, y + oy, scale, fromStates);
                renderer.paint(g, p, to, x + ox + dx, y + oy + dy, scale, toStates);
            } else if (Transition.COVER.equals(type)) {
                renderer.paint(g, p, from, x, y, scale, fromStates);
                renderer.paint(g, p, to, x + Math.round(dx * (1 - t)),
                        y + Math.round(dy * (1 - t)), scale, toStates);
            } else if (Transition.UNCOVER.equals(type)) {
                renderer.paint(g, p, to, x, y, scale, toStates);
                renderer.paint(g, p, from, x - Math.round(dx * t), y - Math.round(dy * t), scale,
                        fromStates);
            } else if (Transition.WIPE.equals(type)) {
                renderer.paint(g, p, from, x, y, scale, fromStates);
                g.pushClip();
                int rw = Math.round(w * t);
                int rh = Math.round(h * t);
                if (dx > 0) {
                    g.clipRect(x + w - rw, y, rw, h);
                } else if (dx < 0) {
                    g.clipRect(x, y, rw, h);
                } else if (dy > 0) {
                    g.clipRect(x, y + h - rh, w, rh);
                } else {
                    g.clipRect(x, y, w, rh);
                }
                renderer.paint(g, p, to, x, y, scale, toStates);
                g.popClip();
            } else {
                // zoom: the new slide grows out of the centre over the old one
                renderer.paint(g, p, from, x, y, scale, fromStates);
                float s = scale * (0.4f + 0.6f * t);
                int zx = x + Math.round((w - p.width * s) / 2);
                int zy = y + Math.round((h - p.height * s) / 2);
                g.setAlpha(Math.round(255 * t));
                renderer.paint(g, p, to, zx, zy, s, toStates);
            }
        } finally {
            g.setAlpha(alpha);
            g.popClip();
        }
    }

    /// The ids of the elements a slide's builds animate, in order.
    static List<String> builtIds(Slide slide) {
        List<String> l = new ArrayList<String>();
        for (Build b : slide.builds) {
            if (!l.contains(b.elementId)) {
                l.add(b.elementId);
            }
        }
        return l;
    }
}
