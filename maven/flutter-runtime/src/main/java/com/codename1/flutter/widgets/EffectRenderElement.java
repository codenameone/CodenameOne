/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.FlutterRootLayout;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

import dart.runtime.Funcs;

/**
 * Base for widgets that change how their subtree PAINTS without changing its layout —
 * Opacity, Transform and their relatives.
 *
 * <p>The rest of the render tree is flat: every element's component is a sibling in one
 * host container, positioned absolutely. That is fast, but it means an ancestor cannot
 * wrap its descendants in a paint effect, because they are not its children. So an
 * effect element owns a nested container with its own {@link RenderHost} — the same
 * device the scrollables use — which makes its subtree genuinely nested and therefore
 * something it can paint through.</p>
 *
 * <p>Layout is untouched: the child is measured against the incoming constraints and the
 * effect element takes exactly the child's size. Flutter's Opacity and Transform do not
 * affect layout either.</p>
 */
public abstract class EffectRenderElement extends RenderElement {

    private Element content;
    private RenderHost innerHost;

    protected EffectRenderElement(Widget widget) {
        super(widget);
    }

    /** The widget this effect applies to. */
    protected abstract Widget effectChild();

    /**
     * Paints the effect's subtree into a Graphics of the caller's choosing.
     *
     * <p>The target is a parameter rather than the {@code Graphics} the effect was
     * handed, because an effect that needs a matrix has to paint the subtree into an
     * offscreen layer first - see {@link #layer}.</p>
     */
    protected interface Subtree {
        void paint(Graphics target);
    }

    /**
     * Applies the effect and paints the subtree. Implementations must leave the
     * Graphics as they found it — a frame paints many components through the same one.
     */
    protected abstract void paintWithEffect(Graphics g, Container pane, Subtree paintChildren);

    private RenderHost innerHost() {
        if (innerHost == null) {
            innerHost = new RenderHost();
            innerHost.rootSupplier(new Funcs.Func0<Element>() {
                @Override
                public Element call() {
                    return content;
                }
            });
        }
        return innerHost;
    }

    @Override
    protected RenderHost hostForChild(int slot) {
        return innerHost();
    }

    @Override
    public void mount(Element parent, int slot) {
        // The pane is a container INSIDE the same Form, so its host has to know
        // about that Form: a subtree that asks its host for the Form -- a
        // Scaffold deciding whether it owns the toolbar, a dialog looking for
        // somewhere to open -- would otherwise be told there is none simply
        // because it happens to sit under a paint effect.
        // Walk up for it. A nested host does not necessarily carry the Form -- an effect
        // inside another effect takes its parent's inner host, whose form may itself be
        // unset -- and once one link in that chain is null every host below it is too.
        // Anything that then asks its host which Form it is in gets no answer: the safe
        // area comes back as zero and the page lays out under the status bar.
        com.codename1.ui.Form form = null;
        for (Element e = parent; e != null && form == null; e = e.parent()) {
            RenderHost h = e.host();
            if (h != null) {
                form = h.form();
            }
        }
        if (form == null && host() != null) {
            form = host().form();
        }
        if (form != null) {
            innerHost().form(form);
        }
        super.mount(parent, slot);
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        EffectPane pane = new EffectPane(innerHost());
        innerHost().container(pane);
        return pane;
    }

    @Override
    protected void syncChildren() {
        content = updateChild(content, effectChild(), 0);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (content != null) {
            visitor.call(content);
        }
    }

    /// The constraints the Flutter pass last gave this effect.
    ///
    /// The nested pane carries its own {@link FlutterRootLayout}, and Codename One runs
    /// that layout independently, deriving constraints from the pane's CURRENT component
    /// size. That size is only the Flutter-assigned one after {@code position} has written
    /// it; before then it is whatever CN1 last put there — for a fresh subtree, its
    /// preferred size. So the subtree could be laid out against a height it was never
    /// given: the gallery's study card was measured at its unconstrained 272dp instead of
    /// the carousel's 240dp viewport, which is why its caption was clipped and its bottom
    /// corners came out square.
    ///
    /// Remembering the real constraints and handing them to the nested pass removes the
    /// disagreement: the Flutter pass owns this subtree's geometry, and CN1's pass must
    /// reproduce it rather than re-derive it.
    private BoxConstraints lastConstraints;

    BoxConstraints effectConstraints() {
        return lastConstraints;
    }

    /**
     * The render element of the single child, or null before it is mounted.
     *
     * <p>Exposed so a subclass can lay the child out on its own terms -- a
     * {@link FittedBoxRenderElement} has to measure it UNBOUNDED, which is the
     * whole difference between text that shrinks to fit and text that wraps.</p>
     */
    protected final RenderElement effectRenderChild() {
        return findRenderElement(content);
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        lastConstraints = constraints;
        RenderElement c = findRenderElement(content);
        if (c == null) {
            return constraints.smallest();
        }
        Size cs = c.layout(constraints);
        c.position(0, 0);   // inside our pane, the child sits at the origin
        return constraints.constrain(cs);
    }

    /**
     * An effect is a PAINT wrapper: {@link #performLayout} takes its size straight
     * from the child, so no configuration of the effect itself - a Transform's
     * scale, a Material's colour or elevation - can move anything.
     *
     * <p>This matters most where it is animated. The gallery's carousel rebuilds a
     * Transform per card per scroll frame; treating that as a layout change marked
     * every ancestor up to the Scaffold dirty and relayed out the whole page on
     * each frame, which is what made dragging the carousel stutter.</p>
     */
    @Override
    protected boolean updateAffectsLayout() {
        return false;
    }

    @Override
    protected void positionChildren(int x, int y) {
        // The pane's own layout places the subtree in pane coordinates; nothing to do
        // here, and positioning the child again in host coordinates would double-offset it.
    }

    /// The offscreen this effect's subtree is rendered into, reused across frames.
    private com.codename1.ui.Image layerImage;

    /// Renders the subtree into an offscreen image the size of {@code pane}, and hands
    /// it back for the caller to draw wherever the effect wants it.
    ///
    /// An effect that needs a MATRIX -- a scale or a rotation -- cannot simply set one
    /// on the Graphics and let Codename One walk the subtree, because the two disagree
    /// about units once a matrix is in play. Component bounds are device pixels, but
    /// `Graphics#getClipX` reports the clip in the matrix's own coordinates, and
    /// `Container#paint`'s `g.translate(getX(), getY())` moves the origin by
    /// `getX() / scale` user units rather than by `getX()`. `Component`'s
    /// paint-time cull compares those two directly, so a child can be dropped for
    /// being outside a clip it is in fact inside: the gallery's carousel scales the
    /// card either side of the current page, and the 1019px translate to reach the
    /// next card became 1171 user units, moving the clip clear of the card's bounds.
    /// The card was not clipped or misplaced -- it was never painted at all, which is
    /// why the next card never peeked in the way it does in the reference.
    ///
    /// Rendering to a layer and transforming the RESULT sidesteps that entirely: the
    /// subtree paints through an untransformed Graphics, so every unit downstream is
    /// the device pixel Codename One expects, and a pure scale then needs no matrix
    /// support at all -- it is one `drawImage` into a destination rectangle. It is
    /// also what Flutter does, where Transform is a layer rather than a paint mode.
    ///
    /// The buffer is kept and cleared rather than reallocated: a carousel drag scales
    /// a card on every frame, and a fresh full-size ARGB image per frame is exactly
    /// the allocation rate that drives a collection mid-drag.
    ///
    /// @return the layer, or null when the pane has no area to render into
    protected final com.codename1.ui.Image layer(Container pane, Subtree subtree) {
        return layer(pane, subtree, pane.getWidth(), pane.getHeight());
    }

    /// As {@link #layer(Container, Subtree)}, for an effect whose subtree does not have
    /// the pane's own shape. A quarter-turned box is the case that needs it: its pane
    /// reports the child's footprint with the axes swapped, so a layer the size of the
    /// pane would cut the child in half before it was ever turned.
    protected final com.codename1.ui.Image layer(Container pane, Subtree subtree, int w, int h) {
        if (w <= 0 || h <= 0 || !(pane instanceof EffectPane)) {
            return null;
        }
        if (layerImage == null || layerImage.getWidth() != w || layerImage.getHeight() != h) {
            layerImage = com.codename1.ui.Image.createImage(w, h, 0);
        } else {
            layerImage.getGraphics().clearRect(0, 0, w, h);
        }
        // The subtree paints with the pane parked at the origin, so what lands in the
        // image is exactly the pane's own box -- no translate to unwind afterwards.
        ((EffectPane) pane).paintAtOrigin(layerImage.getGraphics());
        return layerImage;
    }

    /// Paints the subtree clipped to {@code shape}.
    ///
    /// Uses `pushClip`/`popClip`, which is the idiom the ports support and the one the
    /// shaped-clipping tests in `scripts/hellocodenameone` use. Saving `getClipX/Y/W/H`
    /// and restoring with the four-int `setClip` -- which is what this used to do --
    /// cannot express a shape: it degrades whatever the ancestors had established to its
    /// BOUNDING BOX. Nest two shaped clips and the outer one stops holding, which is
    /// what made a shaped subtree look like it was layered wrongly rather than simply
    /// unclipped.
    ///
    /// @return false when the port cannot clip to a shape at all, so the caller can
    ///         report it rather than drawing square in silence
    protected final boolean paintShapeClipped(Graphics g, Container pane, Subtree subtree,
            com.codename1.ui.geom.GeneralPath shape) {
        if (shape == null) {
            return false;
        }
        // NOTE: no isShapeClipSupported gate. The subtree is rendered to a
        // buffer below and the rounding is applied to that buffer, either by
        // clipping the target graphics where the port supports it or by MASKING
        // the buffer where it does not. Gating on shape-clip support meant the
        // iOS pipeline -- which reports no support for a live paint but does
        // support it while a transition paints into an image -- drew the demo
        // cards with rounded corners during the transition and square ones the
        // moment it settled.
        // INTERSECT, do not replace. setClip(Shape) installs the shape outright, so
        // whatever the ancestors had established was simply discarded -- and the nearest
        // ancestor that matters is usually a scroller. A clipped subtree inside a list
        // therefore kept painting after it had scrolled out of the viewport: the mail
        // avatars, which are the only clipped thing in a mail row, drew over the status
        // bar as the list moved under it, and that band is not repainted by the scroll,
        // so they smeared.
        //
        // Re-intersecting with the incoming rectangle after installing the shape is what
        // clipRect is for. Note the incoming clip is read as a RECTANGLE, so a shaped
        // clip an ancestor established is honoured only to its bounding box -- weaker
        // than exact, and far better than dropping it.
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        // One application, around the whole subtree.
        //
        // NOTE: a shaped clip does NOT survive a subtree here, and that is by
        // design in Codename One -- Component.paintComponent and
        // paintInternalImpl save the clip as four ints and restore it with
        // setClip(x,y,w,h), which degrades a shape to its bounding box for
        // every sibling after the first. Making those pairs shape-aware is the
        // change that is deliberately commented out in Component, because it
        // alters the paint contract for every existing application.
        //
        // Two things were tried here and are recorded so they are not tried
        // again. Rendering the subtree to a buffer and masking it rounds
        // correctly on the desktop but fails on iOS -- a mutable-image detour
        // mid-paint leaves a stale scissor (issue #5171,
        // MutableImageClipReadbackTest) and createMask can fault for
        // image-scale buffers (SimdLargeAllocaTest). Painting each child
        // separately with the clip re-installed per child loses what
        // Container.paint does around its child loop, and measured 13.97% wrong
        // across the sweep against 2.54%.
        g.pushClip();
        try {
            g.setClip(shape);
            g.clipRect(cx, cy, cw, ch);
            subtree.paint(g);
        } finally {
            g.popClip();
        }
        return true;
    }

    /** The nested container: lays the subtree out at its own bounds and paints it through the effect. */
    private final class EffectPane extends Container {

        EffectPane(RenderHost host) {
            super(new FlutterRootLayout(host) {
                @Override
                protected BoxConstraints constraintsFor(com.codename1.ui.Container parent) {
                    BoxConstraints c = effectConstraints();
                    return c != null ? c : super.constraintsFor(parent);
                }
            });
            setUIID("FlutterEffect");
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
            getAllStyles().setBgTransparency(0);
        }

        @Override
        public void paint(final Graphics g) {
            final Container self = this;
            paintWithEffect(g, self, new Subtree() {
                @Override
                public void paint(Graphics target) {
                    EffectPane.super.paint(target);
                }
            });
        }

        /** Paints the subtree with this pane parked at the origin, for {@link #layer}. */
        void paintAtOrigin(Graphics target) {
            int x = getX();
            int y = getY();
            setX(0);
            setY(0);
            try {
                EffectPane.super.paint(target);
            } finally {
                setX(x);
                setY(y);
            }
        }
    }
}
