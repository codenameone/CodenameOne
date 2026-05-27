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
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;

import java.util.HashMap;
import java.util.Map;

/// A transition inspired by the Android L release morph activity effect allowing
/// a set of components in one form/container to morph into another in a different
/// container/form.
///
/// @author Shai Almog
public final class MorphTransition extends Transition {
    private final HashMap<String, String> fromTo = new HashMap<String, String>();
    private int duration;
    private CC[] fromToComponents;
    private Motion animationMotion;
    private boolean finished;
    /// Opt-in snapshot mode -- when on, source / destination components are
    /// captured as clipped `Image`s at `initTransition()` time and the tween
    /// draws those images rather than re-painting the live components every
    /// frame. See `#snapshotMode(boolean)`.
    private boolean snapshotMode;

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
                m.fromTo.put(entry.getValue(), entry.getKey());
            }
        } else {
            m.fromTo.putAll(fromTo);
        }
        return m;
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

    /// {@inheritDoc}
    @Override
    public void initTransition() {
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
            CC cc = new CC(sourceCmp, destCmp, duration);
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
    }

    /// {@inheritDoc}
    @Override
    public boolean animate() {
        if (!finished) {
            // animate one last time
            if (animationMotion != null && animationMotion.isFinished()) {
                finished = true;

                // restore forms to orignial states
                if (fromToComponents != null) {
                    for (CC c : fromToComponents) {
                        if (c == null) {
                            continue;
                        }
                        Container p = c.placeholderDest.getParent();
                        c.dest.getParent().removeComponent(c.dest);
                        p.replace(c.placeholderDest, c.dest, null);

                        p = c.placeholderSrc.getParent();
                        c.source.getParent().removeComponent(c.source);
                        p.replace(c.placeholderSrc, c.source, null);
                    }
                }

                // remove potential memory leak
                fromToComponents = null;

                return true;
            }
            if (fromToComponents != null) {
                for (CC c : fromToComponents) {
                    if (c == null) {
                        continue;
                    }
                    int x = c.xMotion.getValue();
                    int y = c.yMotion.getValue();
                    int w = c.wMotion.getValue();
                    int h = c.hMotion.getValue();
                    c.source.setX(x);
                    c.source.setY(y);
                    c.source.setWidth(w);
                    c.source.setHeight(h);
                    c.dest.setX(x);
                    c.dest.setY(y);
                    c.dest.setWidth(w);
                    c.dest.setHeight(h);
                }
            }

            return true;
        }

        return false;
    }

    /// {@inheritDoc}
    @Override
    public void paint(Graphics g) {
        int oldAlpha = g.getAlpha();
        int alpha = 0;
        if (animationMotion != null) {
            alpha = animationMotion.getValue();
        }
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
    /// nothing off-viewport leaks into the morph.
    private void paintSnapshots(Graphics g, int alpha) {
        int oldAlpha = g.getAlpha();
        try {
            for (CC c : fromToComponents) {
                if (c == null || c.sourceImage == null || c.destImage == null) {
                    continue;
                }
                int x = c.xMotion.getValue();
                int y = c.yMotion.getValue();
                int w = c.wMotion.getValue();
                int h = c.hMotion.getValue();
                if (w <= 0 || h <= 0) continue;
                // Source fades out
                g.setAlpha(255 - alpha);
                drawImageScaled(g, c.sourceImage, x, y, w, h);
                // Dest fades in
                g.setAlpha(alpha);
                drawImageScaled(g, c.destImage, x, y, w, h);
            }
        } finally {
            g.setAlpha(oldAlpha);
        }
    }

    /// Draws `img` into the `(x, y, w, h)` rectangle. Skips a scaled copy
    /// when the image already happens to be at the target size (cheap
    /// fast-path for the first and last frames of the animation).
    private static void drawImageScaled(Graphics g, com.codename1.ui.Image img, int x, int y, int w, int h) {
        if (img.getWidth() == w && img.getHeight() == h) {
            g.drawImage(img, x, y);
        } else {
            g.drawImage(img, x, y, w, h);
        }
    }

    static class CC {
        Component source;
        Component dest;
        Label placeholderSrc;
        Label placeholderDest;
        Motion xMotion;
        Motion yMotion;
        Motion wMotion;
        Motion hMotion;

        /// Snapshot-mode capture of `source` at its original bounds, clipped
        /// to its own size. Populated in `MorphTransition#captureSnapshot`
        /// when `snapshotMode == true`; null on the legacy path.
        com.codename1.ui.Image sourceImage;
        /// Snapshot-mode capture of `dest` at its destination-form bounds.
        com.codename1.ui.Image destImage;

        public CC(Component source, Component dest, int duration) {
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
    private static com.codename1.ui.Image captureSnapshot(Component cmp) {
        int w = Math.max(1, cmp.getWidth());
        int h = Math.max(1, cmp.getHeight());
        com.codename1.ui.Image img = com.codename1.ui.Image.createImage(w, h, 0);
        com.codename1.ui.Graphics g = img.getGraphics();
        // paintComponent renders the component at its current screen position
        // by default; offset so the top-left of `cmp` lands at (0, 0) of the
        // image buffer. The image's bounds clip outside-of-buffer paints.
        g.translate(-cmp.getX(), -cmp.getY());
        cmp.paintComponent(g);
        return img;
    }
}
