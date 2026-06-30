/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.ui.animations;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/// A transition inspired by the Android L release morph activity effect allowing
/// a set of components in one form/container to morph into another in a different
/// container/form.
///
/// Beyond the original position / size morph, this transition can also tween the
/// **opacity**, **rotation** and **scale** of each morphed target and can morph
/// **arbitrary rendered elements** (an `Image`, a rendered SVG, a snapshot of any
/// `Component`, ...) that don't have to be named components living in a `Form` --
/// see `#morph(MorphElement)`.
///
/// The transition is also **scrubbable**: instead of letting the framework drive
/// it from the animation clock you can step it to an arbitrary normalized
/// position `t` in `[0, 1]` with `#setProgress(double)`. Scrubbing is
/// deterministic (it never reads the wall clock), is repeatable, and can move
/// backwards, which makes it compose cleanly with a frame-export loop that renders
/// the same animation at a set of progress fractions.
///
/// @author Shai Almog
public final class MorphTransition extends Transition {
    private final HashMap<String, String> fromTo = new HashMap<String, String>();

    /// Optional per-named-target opacity tween, keyed by the source component
    /// name. The value is `{fromOpacity, toOpacity}` in the `[0, 1]` range.
    /// Applied when drawing the captured snapshots (see `#snapshotMode(boolean)`).
    private final HashMap<String, float[]> opacityByName = new HashMap<String, float[]>();
    /// Optional per-named-target rotation tween, keyed by source name. Value is
    /// `{fromDegrees, toDegrees}`.
    private final HashMap<String, float[]> rotationByName = new HashMap<String, float[]>();
    /// Optional per-named-target scale tween, keyed by source name. Value is
    /// `{fromScale, toScale}` (a multiplier about the target center, `1` = no
    /// scaling beyond the interpolated bounds).
    private final HashMap<String, float[]> scaleByName = new HashMap<String, float[]>();

    /// Arbitrary rendered elements morphed independently of the named-component
    /// pairs. See `#morph(MorphElement)`.
    private final ArrayList<MorphElement> elements = new ArrayList<MorphElement>();

    private int duration;
    private CC[] fromToComponents;
    private Motion animationMotion;
    private boolean finished;
    /// Set once the layered-pane surgery from `#initTransition()` has been undone
    /// (either by the `animate()` terminal frame or by `#cleanup()` in scrub
    /// mode). Guards against a double restore.
    private boolean restored;

    /// Opt-in snapshot mode -- when on, source / destination components are
    /// captured as clipped `Image`s at `initTransition()` time and the tween
    /// draws those images rather than re-painting the live components every
    /// frame. See `#snapshotMode(boolean)`.
    private boolean snapshotMode;

    /// True once `#setProgress(double)` has been used to drive the transition.
    /// In scrub mode the cross-fade alpha and tweened values come from the
    /// normalized progress rather than the animation clock, and the terminal
    /// teardown in `animate()` is suppressed so the transition can be stepped
    /// repeatedly (and backwards).
    private boolean scrubbing;
    /// Last normalized progress (`0..1`) requested through `#setProgress(double)`.
    private double progress;
    /// Cross-fade alpha (`0..255`) when scrubbing; `-1` means "derive from the
    /// animation clock" (the legacy time-driven behaviour).
    private int currentAlpha = -1;

    private MorphTransition() {
    }

    /// Enables the image-snapshot path. Each `(source, dest)` pair is rendered
    /// once into an `Image` at `initTransition()` (clipped to the component's
    /// own bounds; off-viewport children do not contribute pixels), then the
    /// tween draws those images at the interpolated `(x, y, w, h)`.
    ///
    /// Use this when:
    ///
    /// - The source lives inside a scrolling container whose
    ///   `scrollX`/`scrollY` would otherwise leak off-viewport child pixels
    ///   into the morph (the cross-form morph clipping artifact).
    /// - The source has children with dynamic content (a `BrowserComponent`,
    ///   a video frame, a custom-painted background) that should be frozen
    ///   visually for the duration of the animation.
    /// - The source's parent applies a clip that the layered pane wouldn't
    ///   replicate.
    ///
    /// Snapshot mode is also required for the per-named-target `#rotation`,
    /// `#scale` and `#opacity` tweens to have a visible effect, since those
    /// transforms are applied while drawing the captured image.
    ///
    /// Default is **off** to preserve back-compat with the legacy live-paint
    /// path. Always pair with a screenshot regression test (see
    /// `scripts/hellocodenameone/.../MorphTransitionTest`).
    ///
    /// #### Parameters
    ///
    /// - `enabled`: `true` to snapshot, `false` for the legacy live-paint mode
    ///
    /// #### Returns
    ///
    /// this transition (for chaining with `#morph(String)` etc.)
    public MorphTransition snapshotMode(boolean enabled) {
        this.snapshotMode = enabled;
        return this;
    }

    /// Returns the current snapshot-mode setting. See `#snapshotMode(boolean)`.
    public boolean isSnapshotMode() {
        return snapshotMode;
    }

    /// Creates a transition with the given duration, this transition should be modified with the
    /// builder methods such as morph
    ///
    /// #### Parameters
    ///
    /// - `duration`: the duration of the transition
    ///
    /// #### Returns
    ///
    /// a new Morph transition instance
    public static MorphTransition create(int duration) {
        MorphTransition mt = new MorphTransition();
        mt.duration = duration;
        return mt;
    }

    private static Component findByName(Container root, String componentName) {
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component c = root.getComponentAt(iter);
            String n = c.getName();
            if (n != null && n.equals(componentName)) {
                return c;
            }
            if (c instanceof Container) {
                c = findByName((Container) c, componentName);
                if (c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public Transition copy(boolean reverse) {
        MorphTransition m = create(duration);
        m.snapshotMode = snapshotMode;
        if (reverse) {
            for (Map.Entry<String, String> entry : fromTo.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                m.fromTo.put(v, k);
                // A per-name tween was registered against the source name `k`;
                // after reversing, the new source name is `v` (what used to be
                // the destination) and the from/to endpoints swap.
                reverseAttrInto(opacityByName.get(k), v, m.opacityByName);
                reverseAttrInto(rotationByName.get(k), v, m.rotationByName);
                reverseAttrInto(scaleByName.get(k), v, m.scaleByName);
            }
            for (MorphElement e : elements) {
                m.elements.add(e.copy(true));
            }
        } else {
            m.fromTo.putAll(fromTo);
            copyAttr(opacityByName, m.opacityByName);
            copyAttr(rotationByName, m.rotationByName);
            copyAttr(scaleByName, m.scaleByName);
            for (MorphElement e : elements) {
                m.elements.add(e.copy(false));
            }
        }
        return m;
    }

    private static void copyAttr(HashMap<String, float[]> src, HashMap<String, float[]> dest) {
        for (Map.Entry<String, float[]> e : src.entrySet()) {
            dest.put(e.getKey(), new float[]{e.getValue()[0], e.getValue()[1]});
        }
    }

    private static void reverseAttrInto(float[] attr, String newKey, HashMap<String, float[]> dest) {
        if (attr != null) {
            dest.put(newKey, new float[]{attr[1], attr[0]});
        }
    }

    /// Morphs the component with the given source name in the source container hierarchy
    /// to the component with the same name in the destination hierarchy
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the compoennt name
    ///
    /// #### Returns
    ///
    /// this so morph operations can be chained as MorphTransition t = MorphTransition.create(300).morph("a").("c");
    public MorphTransition morph(String cmp) {
        fromTo.put(cmp, cmp);
        return this;
    }

    /// Morphs the component with the given source name in the source container hierarchy
    /// to the component with the given name in the destination hierarchy
    ///
    /// #### Parameters
    ///
    /// - `source`
    ///
    /// - `to`
    ///
    /// #### Returns
    ///
    /// this so morph operations can be chained as MorphTransition t = MorphTransition.create(300).morph("a", "b").("c", "d");
    public MorphTransition morph(String source, String to) {
        fromTo.put(source, to);
        return this;
    }

    /// Tweens the opacity of a named morph target from `fromOpacity` to
    /// `toOpacity` (both in the `[0, 1]` range) over the course of the
    /// transition. The named target must also be registered with `#morph(String)`
    /// / `#morph(String, String)`. The opacity is applied while drawing the
    /// captured snapshot, so this requires `#snapshotMode(boolean)` to be on to
    /// have a visible effect.
    ///
    /// #### Parameters
    ///
    /// - `sourceName`: the source-side name passed to `#morph(String)`
    /// - `fromOpacity`: the opacity at `t = 0`, `0` (transparent) to `1` (opaque)
    /// - `toOpacity`: the opacity at `t = 1`
    ///
    /// #### Returns
    ///
    /// this transition for chaining
    public MorphTransition opacity(String sourceName, float fromOpacity, float toOpacity) {
        opacityByName.put(sourceName, new float[]{fromOpacity, toOpacity});
        return this;
    }

    /// Tweens the rotation of a named morph target from `fromDegrees` to
    /// `toDegrees` about its center. Requires `#snapshotMode(boolean)` to have a
    /// visible effect and an affine-capable `Graphics` (every modern port);
    /// where affine isn't supported the rotation is silently skipped.
    ///
    /// #### Parameters
    ///
    /// - `sourceName`: the source-side name passed to `#morph(String)`
    /// - `fromDegrees`: rotation in degrees at `t = 0`
    /// - `toDegrees`: rotation in degrees at `t = 1`
    ///
    /// #### Returns
    ///
    /// this transition for chaining
    public MorphTransition rotation(String sourceName, float fromDegrees, float toDegrees) {
        rotationByName.put(sourceName, new float[]{fromDegrees, toDegrees});
        return this;
    }

    /// Tweens an extra scale multiplier (about the target center, on top of the
    /// interpolated bounds) of a named morph target from `fromScale` to
    /// `toScale`. A value of `1` leaves the interpolated size unchanged; use this
    /// for a "pop" that overshoots the target box. Requires
    /// `#snapshotMode(boolean)` to have a visible effect.
    ///
    /// #### Parameters
    ///
    /// - `sourceName`: the source-side name passed to `#morph(String)`
    /// - `fromScale`: scale multiplier at `t = 0`
    /// - `toScale`: scale multiplier at `t = 1`
    ///
    /// #### Returns
    ///
    /// this transition for chaining
    public MorphTransition scale(String sourceName, float fromScale, float toScale) {
        scaleByName.put(sourceName, new float[]{fromScale, toScale});
        return this;
    }

    /// Morphs an arbitrary rendered element (an `Image`, a rendered SVG, a
    /// snapshot of any `Component`) between two states. Unlike the named-component
    /// morph this does not require the element to be a child of the source /
    /// destination form -- the element is drawn as an overlay at the interpolated
    /// position, size, opacity, rotation and scale. Multiple elements can be
    /// registered and they compose with the named-component morphs.
    ///
    /// #### Parameters
    ///
    /// - `element`: the element description built via `MorphElement#create`
    ///
    /// #### Returns
    ///
    /// this transition for chaining
    public MorphTransition morph(MorphElement element) {
        if (element != null) {
            element.applyFraction(0f);
            elements.add(element);
        }
        return this;
    }

    /// Steps the transition to a normalized progress `t` in `[0, 1]` without
    /// reading the animation clock. `0` is the source state, `1` the destination
    /// state; intermediate values are eased with the same ease-in-out curve the
    /// clock-driven path uses. The call is deterministic, repeatable and may move
    /// backwards, so it composes with a frame-export loop that renders the
    /// animation at a set of progress fractions:
    ///
    /// ```java
    /// MorphTransition t = MorphTransition.create(600).morph(element);
    /// t.init(source, dest);
    /// t.initTransition();
    /// for (double f = 0; f <= 1.0; f += 1.0 / frames) {
    ///     t.setProgress(f);
    ///     t.paint(frameGraphics);
    /// }
    /// t.cleanup();
    /// ```
    ///
    /// `#initTransition()` must have been invoked first (the
    /// `AbstractTransitionScreenshotTest` harness does this for you). Scrubbing
    /// and the clock-driven `animate()` lifecycle are not meant to be mixed on
    /// the same instance.
    ///
    /// #### Parameters
    ///
    /// - `t`: the normalized progress, clamped to `[0, 1]`
    ///
    /// #### Returns
    ///
    /// this transition for chaining
    public MorphTransition setProgress(double t) {
        scrubbing = true;
        progress = clamp01(t);
        float f = easeInOut(progress);
        currentAlpha = Math.round(f * 255f);
        if (fromToComponents != null) {
            for (CC c : fromToComponents) {
                if (c == null) {
                    continue;
                }
                int x = Math.round(lerp(c.xMotion.getSourceValue(), c.xMotion.getDestinationValue(), f));
                int y = Math.round(lerp(c.yMotion.getSourceValue(), c.yMotion.getDestinationValue(), f));
                int w = Math.round(lerp(c.wMotion.getSourceValue(), c.wMotion.getDestinationValue(), f));
                int h = Math.round(lerp(c.hMotion.getSourceValue(), c.hMotion.getDestinationValue(), f));
                applyBounds(c, x, y, w, h);
                c.applyFraction(f);
            }
        }
        for (MorphElement e : elements) {
            e.applyFraction(f);
        }
        return this;
    }

    /// Returns the last normalized progress requested through
    /// `#setProgress(double)` (`0` until the first call).
    public double getProgress() {
        return progress;
    }

    /// Returns true once `#setProgress(double)` has been used to drive this
    /// transition manually.
    public boolean isScrubbing() {
        return scrubbing;
    }

    /// The interpolated opacity (`0..1`) of a named morph target at the current
    /// progress, or `1` when no opacity tween is registered for that name.
    /// Primarily useful for tests / custom rendering.
    public float getCurrentOpacity(String sourceName) {
        CC c = findCC(sourceName);
        return c == null ? attrCurrent(opacityByName.get(sourceName), 1f) : c.curOpacity;
    }

    /// The interpolated rotation (degrees) of a named morph target at the current
    /// progress, or `0` when no rotation tween is registered for that name.
    public float getCurrentRotation(String sourceName) {
        CC c = findCC(sourceName);
        return c == null ? attrCurrent(rotationByName.get(sourceName), 0f) : c.curRotation;
    }

    /// The interpolated scale multiplier of a named morph target at the current
    /// progress, or `1` when no scale tween is registered for that name.
    public float getCurrentScale(String sourceName) {
        CC c = findCC(sourceName);
        return c == null ? attrCurrent(scaleByName.get(sourceName), 1f) : c.curScale;
    }

    private float attrCurrent(float[] attr, float def) {
        if (attr == null) {
            return def;
        }
        return lerp(attr[0], attr[1], easeInOut(progress));
    }

    private CC findCC(String sourceName) {
        if (fromToComponents == null || sourceName == null) {
            return null;
        }
        for (CC c : fromToComponents) {
            if (c != null && sourceName.equals(c.sourceName)) {
                return c;
            }
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public void initTransition() {
        finished = false;
        restored = false;
        animationMotion = Motion.createEaseInOutMotion(0, 255, duration);
        animationMotion.start();
        Container s = (Container) getSource();
        Container d = (Container) getDestination();

        int size = fromTo.size();
        fromToComponents = new CC[size];
        Form destForm = d.getComponentForm();
        destForm.forceRevalidate();
        Form sourceForm = s.getComponentForm();
        int index = 0;
        for (Map.Entry<String, String> entry : fromTo.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            Component sourceCmp = findByName(s, k);
            Component destCmp = findByName(d, v);
            if (sourceCmp == null || destCmp == null) {
                continue;
            }
            CC cc = new CC(k, sourceCmp, destCmp, duration);
            cc.applyAttributes(opacityByName.get(k), rotationByName.get(k), scaleByName.get(k));
            // Snapshot capture happens BEFORE the layered-pane swap, so
            // the source still sits inside its original (possibly
            // scrolling, possibly clipped) parent and renders the pixels
            // the user actually sees at the moment they tap. Capturing
            // after the swap would render the layered-pane copy, which
            // has no clipping context.
            if (snapshotMode) {
                cc.sourceImage = captureSnapshot(sourceCmp);
                cc.destImage = captureSnapshot(destCmp);
            }
            fromToComponents[index] = cc;
            index++;
            cc.placeholderDest = new Label();
            cc.placeholderDest.setVisible(false);
            Container destParent = cc.dest.getParent();
            cc.placeholderDest.setX(cc.dest.getX());
            cc.placeholderDest.setY(cc.dest.getY() - destForm.getContentPane().getY());
            cc.placeholderDest.setWidth(cc.dest.getWidth());
            cc.placeholderDest.setHeight(cc.dest.getHeight());
            cc.placeholderDest.setPreferredSize(new Dimension(cc.dest.getWidth(), cc.dest.getHeight()));
            destParent.replace(cc.dest, cc.placeholderDest, null);
            destForm.getLayeredPane().addComponent(cc.dest);

            cc.placeholderSrc = new Label();
            cc.placeholderSrc.setVisible(false);
            cc.placeholderSrc.setX(cc.source.getX());
            cc.placeholderSrc.setY(cc.source.getY() - sourceForm.getContentPane().getY());
            cc.placeholderSrc.setWidth(cc.source.getWidth());
            cc.placeholderSrc.setHeight(cc.source.getHeight());
            cc.placeholderSrc.setPreferredSize(new Dimension(cc.source.getWidth(), cc.source.getHeight()));

            Container originalContainer = cc.source.getParent();
            originalContainer.replace(cc.source, cc.placeholderSrc, null);
            originalContainer.getComponentForm().getLayeredPane().addComponent(cc.source);
        }

        // Resolve element images (snapshot any component-backed elements now
        // that layout is settled) and seed their initial interpolated state.
        for (MorphElement e : elements) {
            e.resolve();
            e.applyFraction(0f);
        }
    }

    /// {@inheritDoc}
    @Override
    public boolean animate() {
        if (!finished) {
            // animate one last time
            if (!scrubbing && animationMotion != null && animationMotion.isFinished()) {
                finished = true;
                restoreComponents();
                // remove potential memory leak
                fromToComponents = null;
                return true;
            }
            float f = animationMotion != null ? animationMotion.getValue() / 255f : 0f;
            if (fromToComponents != null) {
                for (CC c : fromToComponents) {
                    if (c == null) {
                        continue;
                    }
                    int x = c.xMotion.getValue();
                    int y = c.yMotion.getValue();
                    int w = c.wMotion.getValue();
                    int h = c.hMotion.getValue();
                    applyBounds(c, x, y, w, h);
                    c.applyFraction(f);
                }
            }
            for (MorphElement e : elements) {
                e.applyFraction(f);
            }

            return true;
        }

        return false;
    }

    private static void applyBounds(CC c, int x, int y, int w, int h) {
        c.source.setX(x);
        c.source.setY(y);
        c.source.setWidth(w);
        c.source.setHeight(h);
        c.dest.setX(x);
        c.dest.setY(y);
        c.dest.setWidth(w);
        c.dest.setHeight(h);
    }

    /// Undoes the layered-pane surgery from `#initTransition()`, restoring the
    /// morphed components back into their original parents. Safe to call more
    /// than once. Invoked from the terminal `animate()` frame on the clock-driven
    /// path and from `#cleanup()` on the scrub path.
    private void restoreComponents() {
        if (restored || fromToComponents == null) {
            return;
        }
        restored = true;
        for (CC c : fromToComponents) {
            if (c == null || c.placeholderDest == null || c.placeholderSrc == null) {
                continue;
            }
            Container p = c.placeholderDest.getParent();
            if (p != null) {
                c.dest.getParent().removeComponent(c.dest);
                p.replace(c.placeholderDest, c.dest, null);
            }
            p = c.placeholderSrc.getParent();
            if (p != null) {
                c.source.getParent().removeComponent(c.source);
                p.replace(c.placeholderSrc, c.source, null);
            }
        }
    }

    /// {@inheritDoc}
    @Override
    public void cleanup() {
        // On the scrub path animate() never reaches its terminal frame, so the
        // layered-pane swap has to be undone here.
        restoreComponents();
        fromToComponents = null;
        super.cleanup();
    }

    /// {@inheritDoc}
    @Override
    public void paint(Graphics g) {
        int oldAlpha = g.getAlpha();
        int alpha = currentAlpha >= 0 ? currentAlpha : (animationMotion != null ? animationMotion.getValue() : 0);
        // In snapshot mode we hide the live morphed components on the
        // layered pane (they'd otherwise paint themselves on top of the
        // captured images), paint the source / dest forms normally, then
        // overlay the alpha-blended snapshots at the tweened bounds.
        boolean hidSnapshots = false;
        if (snapshotMode && fromToComponents != null) {
            for (CC c : fromToComponents) {
                if (c != null) {
                    c.source.setVisible(false);
                    c.dest.setVisible(false);
                }
            }
            hidSnapshots = true;
        }
        try {
            if (alpha < 255) {
                g.setAlpha(255 - alpha);
                getSource().paintComponent(g);

                g.setAlpha(alpha);
                byte bgT = getDestination().getUnselectedStyle().getBgTransparency();
                getDestination().getUnselectedStyle().setBgTransparency(0);
                getDestination().paintComponent(g, false);
                getDestination().getUnselectedStyle().setBgTransparency(bgT);
                g.setAlpha(oldAlpha);
            } else {
                getDestination().paintComponent(g);
            }
            if (snapshotMode && fromToComponents != null) {
                paintSnapshots(g, alpha);
            }
            paintElements(g);
        } finally {
            if (hidSnapshots) {
                for (CC c : fromToComponents) {
                    if (c != null) {
                        c.source.setVisible(true);
                        c.dest.setVisible(true);
                    }
                }
            }
            g.setAlpha(oldAlpha);
        }
    }

    /// Snapshot-mode draw of each morphed pair: alpha-blend the source
    /// snapshot (decreasing) on top of the destination snapshot
    /// (increasing) at the current tweened bounds. The snapshots are
    /// scaled to fit those bounds; on hi-DPI this is a nearest-neighbour
    /// stretch via `drawImage(scaled)`. Both images already represent the
    /// component clipped to its own bounds at the moment of capture, so
    /// nothing off-viewport leaks into the morph. When a per-target
    /// `#rotation` / `#scale` / `#opacity` tween was registered the captured
    /// image is additionally transformed about its center.
    ///
    /// Callers must have already invoked `#initTransition()` -- the guard
    /// at the top of the method protects against late-call paths
    /// (`finished` flush, animation cancel) where the field has been
    /// nulled out.
    private void paintSnapshots(Graphics g, int alpha) {
        CC[] pairs = fromToComponents;
        if (pairs == null) {
            return;
        }
        int oldAlpha = g.getAlpha();
        try {
            for (CC c : pairs) {
                if (c == null || c.sourceImage == null || c.destImage == null) {
                    continue;
                }
                int x = c.source.getX();
                int y = c.source.getY();
                int w = c.source.getWidth();
                int h = c.source.getHeight();
                if (w <= 0 || h <= 0) {
                    continue;
                }
                // Source fades out
                drawTransformed(g, c.sourceImage, x, y, w, h, c.curScale, c.curRotation,
                        Math.round((255 - alpha) * c.curOpacity));
                // Dest fades in
                drawTransformed(g, c.destImage, x, y, w, h, c.curScale, c.curRotation,
                        Math.round(alpha * c.curOpacity));
            }
        } finally {
            g.setAlpha(oldAlpha);
        }
    }

    /// Draws every registered `MorphElement` at its current interpolated state.
    private void paintElements(Graphics g) {
        if (elements.isEmpty()) {
            return;
        }
        int oldAlpha = g.getAlpha();
        try {
            for (MorphElement e : elements) {
                Image img = e.image;
                if (img == null) {
                    continue;
                }
                int a = Math.round(clamp01(e.curOpacity) * 255f);
                if (a <= 0) {
                    continue;
                }
                drawTransformed(g, img, Math.round(e.curX), Math.round(e.curY),
                        Math.round(e.curW), Math.round(e.curH), e.curScale, e.curRotation, a);
            }
        } finally {
            g.setAlpha(oldAlpha);
        }
    }

    /// Draws `img` into the base `(x, y, w, h)` rectangle, additionally scaled by
    /// `scaleMul` about the rectangle center, rotated `rotationDeg` about the
    /// same center, at the given `alpha`. Rotation needs an affine-capable
    /// `Graphics`; where it isn't supported the rotation is skipped (the scale /
    /// opacity still apply).
    private static void drawTransformed(Graphics g, Image img, int x, int y, int w, int h,
                                        float scaleMul, float rotationDeg, int alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0) {
            return;
        }
        // Apply the extra scale multiplier about the center via the draw rect so
        // it works on every port (no affine required for scaling).
        int sw = Math.max(1, Math.round(w * scaleMul));
        int sh = Math.max(1, Math.round(h * scaleMul));
        int sx = x + (w - sw) / 2;
        int sy = y + (h - sh) / 2;

        g.setAlpha(Math.max(0, Math.min(255, alpha)));
        boolean rotate = rotationDeg != 0f && g.isAffineSupported();
        if (rotate) {
            int tX = g.getTranslateX();
            int tY = g.getTranslateY();
            int pivotX = tX + sx + sw / 2;
            int pivotY = tY + sy + sh / 2;
            g.translate(-tX, -tY);
            g.rotate((float) Math.toRadians(rotationDeg), pivotX, pivotY);
            drawImageScaled(g, img, tX + sx, tY + sy, sw, sh);
            g.resetAffine();
            g.translate(tX, tY);
        } else {
            drawImageScaled(g, img, sx, sy, sw, sh);
        }
    }

    /// Draws `img` into the `(x, y, w, h)` rectangle. Skips a scaled copy
    /// when the image already happens to be at the target size (cheap
    /// fast-path for the first and last frames of the animation).
    private static void drawImageScaled(Graphics g, Image img, int x, int y, int w, int h) {
        if (img.getWidth() == w && img.getHeight() == h) {
            g.drawImage(img, x, y);
        } else {
            g.drawImage(img, x, y, w, h);
        }
    }

    private static double clamp01(double v) {
        if (v < 0) {
            return 0;
        }
        if (v > 1) {
            return 1;
        }
        return v;
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }

    private static float lerp(float from, float to, float f) {
        return from + (to - from) * f;
    }

    // --- ease-in-out cubic-bezier(0, 0.42, 0.58, 1) -------------------------
    // Mirrors Motion.createEaseInOutMotion so a scrubbed frame lands on the same
    // value the clock-driven path would produce at the same normalized time.

    private static float easeInOut(double t) {
        float x = (float) clamp01(t);
        if (x <= 0f) {
            return 0f;
        }
        if (x >= 1f) {
            return 1f;
        }
        // control points: P1=(0, 0.42), P2=(0.58, 1)
        float u = solveBezierForT(x, 0f, 0.58f);
        return bezierAxis(u, 0.42f, 1f);
    }

    private static float bezierAxis(float u, float c1, float c2) {
        float omu = 1f - u;
        return 3f * omu * omu * u * c1
                + 3f * omu * u * u * c2
                + u * u * u;
    }

    private static float bezierAxisDerivative(float u, float c1, float c2) {
        float omu = 1f - u;
        return 3f * omu * omu * c1
                + 6f * omu * u * (c2 - c1)
                + 3f * u * u * (1f - c2);
    }

    private static float solveBezierForT(float t, float x1, float x2) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        float cx1 = Math.max(0f, Math.min(1f, x1));
        float cx2 = Math.max(0f, Math.min(1f, x2));
        float u = t;
        for (int i = 0; i < 8; i++) {
            float bxAtU = bezierAxis(u, cx1, cx2);
            float diff = bxAtU - t;
            if (Math.abs(diff) < 1e-6f) {
                return u;
            }
            float deriv = bezierAxisDerivative(u, cx1, cx2);
            if (deriv == 0f) {
                break;
            }
            u -= diff / deriv;
            if (u < 0f) {
                u = 0f;
            } else if (u > 1f) {
                u = 1f;
            }
        }
        float lo = 0f;
        float hi = 1f;
        u = t;
        for (int i = 0; i < 16; i++) {
            float bxAtU = bezierAxis(u, cx1, cx2);
            if (Math.abs(bxAtU - t) < 1e-5f) {
                return u;
            }
            if (bxAtU < t) {
                lo = u;
            } else {
                hi = u;
            }
            u = 0.5f * (lo + hi);
        }
        return u;
    }

    static class CC {
        final String sourceName;
        Component source;
        Component dest;
        Label placeholderSrc;
        Label placeholderDest;
        Motion xMotion;
        Motion yMotion;
        Motion wMotion;
        Motion hMotion;

        // Optional opacity / rotation / scale tweens (identity by default).
        float fromOpacity = 1f, toOpacity = 1f;
        float fromRotation = 0f, toRotation = 0f;
        float fromScale = 1f, toScale = 1f;
        float curOpacity = 1f, curRotation = 0f, curScale = 1f;

        /// Snapshot-mode capture of `source` at its original bounds, clipped
        /// to its own size. Populated in `MorphTransition#captureSnapshot`
        /// when `snapshotMode == true`; null on the legacy path.
        Image sourceImage;
        /// Snapshot-mode capture of `dest` at its destination-form bounds.
        Image destImage;

        public CC(String sourceName, Component source, Component dest, int duration) {
            this.sourceName = sourceName;
            this.source = source;
            this.dest = dest;
            xMotion = Motion.createEaseInOutMotion(positionRelativeToScreen(source, false), positionRelativeToScreen(dest, false), duration);
            xMotion.start();
            yMotion = Motion.createEaseInOutMotion(positionRelativeToScreen(source, true), positionRelativeToScreen(dest, true), duration);
            yMotion.start();
            hMotion = Motion.createEaseInOutMotion(source.getHeight(), dest.getHeight(), duration);
            hMotion.start();
            wMotion = Motion.createEaseInOutMotion(source.getWidth(), dest.getWidth(), duration);
            wMotion.start();
        }

        void applyAttributes(float[] opacity, float[] rotation, float[] scale) {
            if (opacity != null) {
                fromOpacity = opacity[0];
                toOpacity = opacity[1];
            }
            if (rotation != null) {
                fromRotation = rotation[0];
                toRotation = rotation[1];
            }
            if (scale != null) {
                fromScale = scale[0];
                toScale = scale[1];
            }
            curOpacity = fromOpacity;
            curRotation = fromRotation;
            curScale = fromScale;
        }

        void applyFraction(float f) {
            curOpacity = lerp(fromOpacity, toOpacity, f);
            curRotation = lerp(fromRotation, toRotation, f);
            curScale = lerp(fromScale, toScale, f);
        }

        private int positionRelativeToScreen(Component cmp, boolean yAxis) {
            int retVal = 0;
            if (yAxis) {
                int titleHeight = cmp.getComponentForm().getContentPane().getAbsoluteY();
                retVal = cmp.getAbsoluteY() - titleHeight;
            } else {
                retVal = cmp.getAbsoluteX();
            }

            return retVal;
        }
    }

    /// Renders `cmp` into a fresh `Image` sized to its current bounds. Used
    /// by snapshot mode in `initTransition()` to freeze each endpoint
    /// visually before the tween starts; the resulting image is what the
    /// `paint()` cycle draws at the interpolated bounds.
    ///
    /// The component is painted with `paintComponent` (not `paint`) so its
    /// background + border + children are all included. The graphics is
    /// translated so the component's `(getX(), getY())` becomes `(0, 0)` in
    /// the snapshot. The image's own bounds clip everything that paints
    /// outside `(0, 0, width, height)` -- which is exactly the
    /// "off-viewport children don't leak" property the legacy live-paint
    /// path lacked.
    private static Image captureSnapshot(Component cmp) {
        int w = Math.max(1, cmp.getWidth());
        int h = Math.max(1, cmp.getHeight());
        Image img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        // paintComponent renders the component at its current screen position
        // by default; offset so the top-left of `cmp` lands at (0, 0) of the
        // image buffer. The image's bounds clip outside-of-buffer paints.
        g.translate(-cmp.getX(), -cmp.getY());
        cmp.paintComponent(g);
        return img;
    }

    /// Describes an arbitrary rendered element that the morph tweens between two
    /// states: position, size, opacity, rotation and scale. Build one with
    /// `#create(Image)` (e.g. a rendered SVG, an icon, an `EncodedImage`) or
    /// `#create(Component)` (the component is snapshotted to an image when the
    /// transition initializes), then hand it to
    /// `MorphTransition#morph(MorphElement)`.
    ///
    /// All builder setters return `this` for chaining; every state has an
    /// identity default (opacity `1`, rotation `0`, scale `1`, and the element's
    /// own bounds for position / size) so only the properties you actually want
    /// to tween need to be set.
    public static final class MorphElement {
        Image image;
        private Component component;
        private float fromX, fromY, fromW, fromH;
        private float toX, toY, toW, toH;
        private boolean fromSet, toSet;
        private float fromOpacity = 1f, toOpacity = 1f;
        private float fromRotation = 0f, toRotation = 0f;
        private float fromScale = 1f, toScale = 1f;

        // Current interpolated state, updated by the owning transition.
        float curX, curY, curW, curH;
        float curOpacity = 1f, curRotation = 0f, curScale = 1f;

        private MorphElement() {
        }

        /// Creates an element backed by a pre-rendered `Image` (a rasterized SVG,
        /// an icon, a snapshot, ...).
        public static MorphElement create(Image image) {
            MorphElement e = new MorphElement();
            e.image = image;
            return e;
        }

        /// Creates an element backed by a `Component`; the component is
        /// snapshotted to an image when the transition initializes. The
        /// component's laid-out bounds seed the source rectangle unless
        /// `#from(int, int, int, int)` is given.
        public static MorphElement create(Component cmp) {
            MorphElement e = new MorphElement();
            e.component = cmp;
            return e;
        }

        /// Sets the source rectangle (state at `t = 0`).
        public MorphElement from(int x, int y, int w, int h) {
            fromX = x;
            fromY = y;
            fromW = w;
            fromH = h;
            fromSet = true;
            return this;
        }

        /// Sets the destination rectangle (state at `t = 1`).
        public MorphElement to(int x, int y, int w, int h) {
            toX = x;
            toY = y;
            toW = w;
            toH = h;
            toSet = true;
            return this;
        }

        /// Tweens the element opacity from `from` to `to` (both `[0, 1]`).
        public MorphElement opacity(float from, float to) {
            fromOpacity = from;
            toOpacity = to;
            return this;
        }

        /// Tweens the element rotation in degrees about its center.
        public MorphElement rotation(float fromDegrees, float toDegrees) {
            fromRotation = fromDegrees;
            toRotation = toDegrees;
            return this;
        }

        /// Tweens an extra scale multiplier about the element center (on top of
        /// the interpolated size).
        public MorphElement scale(float from, float to) {
            fromScale = from;
            toScale = to;
            return this;
        }

        /// The interpolated x of the element at the current progress.
        public float getCurrentX() {
            return curX;
        }

        /// The interpolated y of the element at the current progress.
        public float getCurrentY() {
            return curY;
        }

        /// The interpolated width of the element at the current progress.
        public float getCurrentWidth() {
            return curW;
        }

        /// The interpolated height of the element at the current progress.
        public float getCurrentHeight() {
            return curH;
        }

        /// The interpolated opacity (`0..1`) at the current progress.
        public float getCurrentOpacity() {
            return curOpacity;
        }

        /// The interpolated rotation (degrees) at the current progress.
        public float getCurrentRotation() {
            return curRotation;
        }

        /// The interpolated scale multiplier at the current progress.
        public float getCurrentScale() {
            return curScale;
        }

        /// Snapshots the component (if any) and fills in default rectangles.
        /// Called by the transition during `initTransition()`.
        void resolve() {
            if (image == null && component != null) {
                image = captureSnapshot(component);
            }
            if (!fromSet) {
                if (component != null) {
                    fromX = component.getX();
                    fromY = component.getY();
                    fromW = component.getWidth();
                    fromH = component.getHeight();
                } else if (image != null) {
                    fromW = image.getWidth();
                    fromH = image.getHeight();
                }
                fromSet = true;
            }
            if (!toSet) {
                // Default the destination to the source rectangle so an element
                // that only tweens opacity / rotation / scale stays in place.
                toX = fromX;
                toY = fromY;
                toW = fromW;
                toH = fromH;
                toSet = true;
            }
        }

        void applyFraction(float f) {
            curX = lerp(fromX, toX, f);
            curY = lerp(fromY, toY, f);
            curW = lerp(fromW, toW, f);
            curH = lerp(fromH, toH, f);
            curOpacity = lerp(fromOpacity, toOpacity, f);
            curRotation = lerp(fromRotation, toRotation, f);
            curScale = lerp(fromScale, toScale, f);
        }

        MorphElement copy(boolean reverse) {
            MorphElement e = new MorphElement();
            e.image = image;
            e.component = component;
            e.fromSet = fromSet;
            e.toSet = toSet;
            if (reverse) {
                e.fromX = toX;
                e.fromY = toY;
                e.fromW = toW;
                e.fromH = toH;
                e.toX = fromX;
                e.toY = fromY;
                e.toW = fromW;
                e.toH = fromH;
                e.fromOpacity = toOpacity;
                e.toOpacity = fromOpacity;
                e.fromRotation = toRotation;
                e.toRotation = fromRotation;
                e.fromScale = toScale;
                e.toScale = fromScale;
            } else {
                e.fromX = fromX;
                e.fromY = fromY;
                e.fromW = fromW;
                e.fromH = fromH;
                e.toX = toX;
                e.toY = toY;
                e.toW = toW;
                e.toH = toH;
                e.fromOpacity = fromOpacity;
                e.toOpacity = toOpacity;
                e.fromRotation = fromRotation;
                e.toRotation = toRotation;
                e.fromScale = fromScale;
                e.toScale = toScale;
            }
            e.applyFraction(0f);
            return e;
        }
    }
}
