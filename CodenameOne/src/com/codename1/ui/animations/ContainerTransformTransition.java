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
package com.codename1.ui.animations;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.GeneralPath;

/// A transition in which one component GROWS into the whole of the next form, the way
/// Material's container transform does: a card or a button becomes the page it opens.
///
/// The difference from [MorphTransition][MorphTransition] is what is being animated.
/// A morph moves a component from where it is in one form to where the same component is
/// in the other, so it needs a counterpart on both sides and it animates a COMPONENT.
/// This animates a SURFACE: a rounded rectangle travels from the tapped component's
/// bounds out to the full form, its corners straightening as it goes, and the two
/// contents cross-fade inside it -- the thing that was tapped fading out while the page
/// fades in. Nothing needs to exist on both sides, which is the usual case: a button does
/// not reappear on the page it opened.
///
/// Geometry follows a fast-out-slow-in curve and the cross-fade is deliberately not
/// symmetric: the outgoing content is gone by the time the incoming content begins,
/// so the two are never both half visible, which reads as a dissolve rather than a
/// transformation.
///
/// Use it where a tap on something becomes a screen:
///
/// ```java
/// tappedCard.setName("card");
/// nextForm.setTransitionInAnimator(
///         ContainerTransformTransition.create("card", 300));
/// nextForm.show();
/// ```
///
/// @author Shai Almog
public final class ContainerTransformTransition extends Transition {

    /// Material's container transform curve: fast out, slow in.
    private static final float CP0 = 0.4f;
    private static final float CP1 = 0.0f;
    private static final float CP2 = 0.2f;
    private static final float CP3 = 1.0f;


    /// Material states this transform's colour and opacity changes in fifths of the run.
    private static final float FIFTH = 0.2f;

    /// Material's scrim over the page being left: black at 54% opacity.
    private static final int SCRIM_ALPHA = 138;

    private static final int SCALE = 1000;

    private final String componentName;
    private final int duration;

    private Motion motion;
    private int progress;
    private Image sourceBuffer;
    private Image destBuffer;
    /// The tapped component on its own, so it can fade out inside the growing surface.
    private Image originBuffer;
    private int startX;
    private int startY;
    private int startW;
    private int startH;
    private int startRadius;
    private int surfaceColor;
    private int openColor;
    private GeneralPath path;

    /// Whether this instance is the CLOSE half, which runs on the mirrored curve.
    private boolean closing;

    /// Whether the thing being grown out of was round, which changes the SHAPE of the
    /// travelling surface for the whole run -- see the squash in paint.
    private boolean originRound;

    private ContainerTransformTransition(String componentName, int duration) {
        this.componentName = componentName;
        this.duration = duration;
    }

    /// Creates a transition that grows the named component into the next form.
    ///
    /// #### Parameters
    ///
    /// - `componentName`: the [Component#setName(String)][Component#setName(String)] of the
    /// component in the OUTGOING form that the next form grows out of. When no component
    /// carries that name the transition still runs, growing from the centre of the screen.
    ///
    /// - `duration`: the duration in milliseconds
    ///
    /// #### Returns
    ///
    /// the transition
    public static ContainerTransformTransition create(String componentName, int duration) {
        return new ContainerTransformTransition(componentName, duration);
    }

    private static Component findByName(Container root, String name) {
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component c = root.getComponentAt(iter);
            String n = c.getName();
            if (n != null && n.equals(name)) {
                return c;
            }
            if (c instanceof Container) {
                Component child = findByName((Container) c, name);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    @Override
    public void initTransition() {
        Component source = getSource();
        Component destination = getDestination();
        // Cleared first: a transition object can be reused, and an early return must not
        // leave the previous run's motion to be replayed.
        motion = null;
        // The Transition contract allows no source -- the first Form shown has nothing to
        // transition from -- and dereferencing it here made that first show throw. With
        // either side missing there is nothing to transform between, so no animation.
        if (source == null || destination == null) {
            return;
        }
        int w = destination.getWidth();
        int h = destination.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        // The SAME curve both ways. Closing is the open progress run backwards (see
        // paint), and 1 - curve(elapsed) is already the mirrored easing -- selecting a
        // mirrored curve here as well would mirror it twice and give
        // curve(1 - elapsed), which is a different motion.
        motion = Motion.createCubicBezierMotion(0, SCALE, duration, CP0, CP1, CP2, CP3);
        motion.start();
        progress = 0;

        sourceBuffer = Image.createImage(source.getWidth(), source.getHeight());
        source.paintComponent(sourceBuffer.getGraphics(), true);
        destBuffer = Image.createImage(w, h);
        destination.paintComponent(destBuffer.getGraphics(), true);

        // The thing the surface grows out of lives on whichever page is NOT the one
        // travelling. Opening, that is the page being left; CLOSING, it is the page being
        // returned to -- so looking on the source form either way found nothing on the
        // way back, and the transform fell through to its "no origin" guess and played
        // the opening animation out of the middle of the screen. Going back looked
        // nothing like the way in.
        Component anchorOn = closing ? destination : source;
        Form anchorForm = anchorOn.getComponentForm();
        Component origin = anchorForm == null || componentName == null
                ? null : findByName(anchorForm, componentName);
        if (origin == null) {
            // Nothing to grow from. The middle of the screen is a poor guess but it is a
            // transition rather than nothing at all, and the caller still gets the fade.
            startW = Math.max(1, w / 8);
            startH = startW;
            startX = (w - startW) / 2;
            startY = (h - startH) / 2;
            startRadius = startW / 2;
            surfaceColor = openPage().getStyle().getBgColor();
            openColor = surfaceColor;
        } else {
            startX = origin.getAbsoluteX();
            startY = origin.getAbsoluteY();
            startW = Math.max(1, origin.getWidth());
            startH = Math.max(1, origin.getHeight());
            // A round thing stays round while it grows; anything else keeps its corners.
            startRadius = Math.min(startW, startH) / 2;
            // Square to within a pixel or two IS the test for round: a circular button is
            // the only thing that can have been one, and the squash below would be wrong
            // for a card.
            originRound = Math.abs(startW - startH) <= Math.max(2, startW / 16);
            surfaceColor = origin.getStyle().getBgColor();
            openColor = openPage().getStyle().getBgColor();
            // Cut out of the PAGE's snapshot, not painted from the component.
            //
            // The named component is the tapped surface itself, and what is drawn on top
            // of it -- a glyph, a label, a whole row -- can be a separate component beside
            // it rather than a child of it. Painting the component alone therefore gave a
            // bare capsule: the compose button's pencil was simply absent from the card
            // for the whole transform, where the reference carries it the entire way.
            // The page has already been photographed a few lines above, and in that
            // photograph the button is whole.
            Image anchorShot = closing ? destBuffer : sourceBuffer;
            originBuffer = Image.createImage(startW, startH, 0);
            originBuffer.getGraphics().drawImage(anchorShot, -startX, -startY);
            // The commonest colour in it, not the middle pixel: the middle of a button is
            // usually its glyph, and taking that made the growing surface the colour of
            // the icon instead of the colour of the button.
            surfaceColor = dominantColor(originBuffer, origin.getStyle().getBgColor());
            // ...and then keep only what was drawn ON the button.
            //
            // A cut-out of the page brings the page with it: the compose button sits in
            // the notch of the bottom bar, so its rectangle is mostly dark bar, and scaled
            // four times into the card that read as a black frame around the glyph. What
            // belongs to the button is what lies inside its outline and is not its own
            // colour -- which is exactly its content, and the card is already painting the
            // colour underneath it.
            originBuffer = maskToContent(originBuffer, startW, startH, surfaceColor, originRound);
        }
    }

    /// The page that TRAVELS: the one growing out of the origin, or shrinking back into
    /// it. Opening it is the destination; closing it is the source.
    private Component openPage() {
        return closing ? getSource() : getDestination();
    }

    /// The snapshot of the page that travels.
    private Image openBuffer() {
        return closing ? sourceBuffer : destBuffer;
    }

    /// The snapshot of the page that stays put underneath.
    private Image staticBuffer() {
        return closing ? destBuffer : sourceBuffer;
    }

    @Override
    public boolean animate() {
        if (motion == null) {
            return false;
        }
        progress = motion.getValue();
        return !motion.isFinished();
    }

    @Override
    public void paint(Graphics g) {
        if (motion == null || openBuffer() == null) {
            return;
        }
        // Geometry follows the curve; everything else does not. Material drives the
        // rectangle off a fast-out-slow-in animation and the colours and opacities off
        // the RAW one, in fifths: the page behind dims over the first fifth, then the
        // surface colour and the incoming content cross over during the second, and the
        // rest of the run is the page settling into place.
        //
        // Both are OPEN progress -- 0 is folded into the origin, 1 is the full page --
        // and closing runs them backwards. Everything below is written once, for the way
        // in, and the way out is the same transform played in reverse: the rectangle
        // shrinks back into what was tapped, the scrim lifts, and the contents cross
        // over the other way. Without this the close ran the OPENING animation, so a
        // page folded away by growing out of its button a second time.
        float t = ((float) progress) / SCALE;
        float linear = motion.getDuration() <= 0 ? 1f
                : Math.min(1f, ((float) motion.getCurrentMotionTime()) / motion.getDuration());
        // Only the GEOMETRY reverses. The fifths that govern the colours and the two
        // contents are measured from the start of whichever run is playing, so closing
        // crosses them over at the same point in its own run rather than at the mirrored
        // point -- it just crosses them the other way round, which is the swap below.
        if (closing) {
            t = 1f - t;
        }
        float cross = crossover(linear);
        Component dest = getDestination();
        int fullW = dest.getWidth();
        int fullH = dest.getHeight();

        // What we came from, unchanged and underneath: the page being left does not move
        // in a container transform, it is covered.
        if (staticBuffer() != null) {
            g.drawImage(staticBuffer(), 0, 0);
        }
        // ...and dimmed. Without the scrim the whole background stays at full brightness
        // through the transition, which is most of the screen disagreeing with the
        // reference for most of the run -- far more pixels than the surface itself.
        // Off the CURVED progress, not the raw clock -- unlike the opacities and the
        // surface colour below, which Material does drive off the raw one. Getting this
        // one wrong is not a subtle shading difference: the scrim covers the whole
        // screen, so while it is ramping, every pixel is at the wrong brightness. It
        // cost a single frame 83% wrong pixels against the reference, between two
        // neighbours at 5% and 13%, because the raw clock reaches full dim more than
        // twice as fast as the curve does.
        //
        // Measured at the 50ms frame of a 300ms run, mean luma over the screen:
        // raw predicts 116.6 and we rendered 117.3; the curve predicts 163.4 and the
        // reference rendered 163.2.
        // Opening, the scrim arrives over the first fifth and then stands. Closing, it
        // does NOT mirror that: it lifts smoothly across the whole run, in proportion to
        // how much of the transform is left. Mirroring the fifths instead held it at
        // full black over the middle of the run and then dropped it in one step -- the
        // page behind stayed dark almost until the surface had gone, where the reference
        // has it brightening the whole way.
        int scrim = closing
                ? (int) (SCRIM_ALPHA * t)
                : (int) (SCRIM_ALPHA * Math.min(1f, t / FIFTH));
        if (scrim > 0) {
            int old = g.getAlpha();
            g.setAlpha(scrim);
            g.setColor(0);
            g.fillRect(0, 0, fullW, fullH);
            g.setAlpha(old);
        }

        int x = lerp(startX, 0, t);
        int y = lerp(startY, 0, t);
        int w = lerp(startW, fullW, t);
        int h = lerp(startH, fullH, t);

        // The SHAPE is not the rectangle. Material lerps the tapped thing's outline into
        // the page's, and a circle squashes the rectangle toward a square about its centre
        // as it goes -- that is what keeps a round button looking round instead of
        // stretching into a lozenge the instant it starts to grow.
        //
        // It is most of the geometry, not a rounding detail. Measured against the
        // reference at the middle of the close, the rectangle is 685 x 1392 and the
        // painted surface 685 x 1066: the same box and the same centre, 326 pixels
        // shorter. Painting the rectangle itself put a third of the card's height in the
        // wrong place for the whole run.
        float circularity = originRound ? 1f - t : 0f;
        int px = x;
        int py = y;
        int pw = w;
        int ph = h;
        if (circularity > 0) {
            if (w < h) {
                int d = (int) (circularity * (h - w) / 2f);
                py += d;
                ph -= 2 * d;
            } else {
                int d = (int) (circularity * (w - h) / 2f);
                px += d;
                pw -= 2 * d;
            }
        }
        // Off the UNADJUSTED box, as Flutter's _adjustBorderRadius is: the radius that
        // makes the squashed box a circle is half the short side of the box it came from.
        int radius = originRound
                ? (int) (circularity * Math.min(w, h) / 2f)
                : lerp(startRadius, 0, t);

        int[] clip = g.getClip();
        if (radius > 0 && g.isShapeClipSupported()) {
            g.setClip(roundRect(px, py, pw, ph, radius));
        } else {
            g.setClip(px, py, pw, ph);
        }

        // The surface holds the tapped thing's colour for the first fifth, crosses to the
        // page's over the second, and is the page's thereafter.
        // Opening runs the tapped thing's colour to the page's; closing runs it back.
        g.setColor(closing ? blend(openColor, surfaceColor, cross)
                : blend(surfaceColor, openColor, cross));
        g.fillRect(px, py, pw, ph);

        // Both contents are drawn at their OWN size scaled to the box's WIDTH, anchored
        // at its top-left corner.
        //
        // This is the part that makes it a transform rather than a window. Drawn at 1:1
        // and clipped, the page inside a half-sized box is the page's top-left QUARTER,
        // so folding the compose page away showed a crop of its header sliding about
        // while the reference shows the whole page shrinking into the button. Width, not
        // height: the aspect ratios of a button and a page have nothing to do with each
        // other, and fitting the width is what keeps the text at the size the box implies.
        //
        // The tapped thing stays FULLY OPAQUE the whole way and is simply covered as the
        // page arrives over it. That is what the fade variant of the transform does -- its
        // closed content has a constant opacity of 1 and only the page's opacity moves --
        // and it is the visible difference between a button that becomes the page and two
        // pictures dissolving into each other. Measured on the reference at the middle of
        // the close, the pencil inside the shrinking card is pure black, not a tint.
        if (originBuffer != null) {
            drawFittedToWidth(g, originBuffer, x, y, w);
        }

        float open = closing ? 1f - cross : cross;
        if (open > 0) {
            int old = g.getAlpha();
            g.setAlpha((int) (255 * open));
            drawFittedToWidth(g, openBuffer(), x, y, w);
            g.setAlpha(old);
        }
        g.setClip(clip[0], clip[1], clip[2], clip[3]);
    }

    /// Draws an image scaled so its WIDTH is {@code w}, anchored at {@code x, y}, with
    /// its aspect ratio kept. The caller's clip decides how much of it is seen.
    private static void drawFittedToWidth(Graphics g, Image img, int x, int y, int w) {
        if (img == null || img.getWidth() <= 0) {
            return;
        }
        int h = Math.max(1, (int) ((long) img.getHeight() * w / img.getWidth()));
        g.drawImage(img, x, y, Math.max(1, w), h);
    }

    /// 0 before the second fifth, 1 after it, and the crossing in between.
    private static float crossover(float linear) {
        if (linear <= FIFTH) {
            return 0f;
        }
        if (linear >= FIFTH * 2) {
            return 1f;
        }
        return (linear - FIFTH) / FIFTH;
    }

    private GeneralPath roundRect(int x, int y, int w, int h, int r) {
        if (path == null) {
            path = new GeneralPath();
        }
        path.reset();
        int rad = Math.min(r, Math.min(w, h) / 2);
        // CLOCKWISE, explicitly. GeneralPath.arcTo defaults to counter-clockwise, and the
        // rectangle below is walked clockwise, so every corner took the long way round --
        // a 270 degree sweep that bulges back into the box instead of a 90 degree one.
        // With a small radius that reads as a slightly soft corner; with a large one the
        // shape is unrecognisable, and this transition's corners reach half the short side
        // at the start of the run.
        path.moveTo(x + rad, y);
        path.lineTo(x + w - rad, y);
        path.arcTo(x + w - rad, y + rad, x + w, y + rad, true);
        path.lineTo(x + w, y + h - rad);
        path.arcTo(x + w - rad, y + h - rad, x + w - rad, y + h, true);
        path.lineTo(x + rad, y + h);
        path.arcTo(x + rad, y + h - rad, x, y + h - rad, true);
        path.lineTo(x, y + rad);
        path.arcTo(x + rad, y + rad, x + rad, y, true);
        path.closePath();
        return path;
    }

    private static int lerp(int from, int to, float t) {
        return from + (int) ((to - from) * t);
    }

    /// Clears everything outside the tapped thing's outline, and everything inside it that
    /// is the thing's own colour, leaving its content on transparency.
    private static Image maskToContent(Image img, int w, int h, int surface, boolean round) {
        try {
            int[] px = img.getRGB();
            int cx = w / 2;
            int cy = h / 2;
            // Inside the outline by a few pixels. The edge of a round button is
            // anti-aliased against whatever is behind it, so the outermost ring is neither
            // the button's colour nor its content -- and magnified four times it drew a
            // pale arc across the card that belongs to nothing.
            int rad = Math.min(w, h) / 2;
            rad -= Math.max(2, rad / 12);
            int radSq = rad * rad;
            int sr = (surface >> 16) & 0xff;
            int sg = (surface >> 8) & 0xff;
            int sb = surface & 0xff;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    if (round) {
                        int dx = x - cx;
                        int dy = y - cy;
                        if (dx * dx + dy * dy > radSq) {
                            px[i] = 0;
                            continue;
                        }
                    }
                    int p = px[i];
                    if (Math.abs(((p >> 16) & 0xff) - sr) <= TOLERANCE
                            && Math.abs(((p >> 8) & 0xff) - sg) <= TOLERANCE
                            && Math.abs((p & 0xff) - sb) <= TOLERANCE) {
                        px[i] = 0;
                    }
                }
            }
            return Image.createImage(px, w, h);
        } catch (Throwable t) {
            return img;
        }
    }

    /// How close to the surface colour counts as the surface rather than its content.
    private static final int TOLERANCE = 24;

    /// The commonest opaque colour in a snapshot, which is the surface colour of whatever
    /// was tapped however it came to be painted. Falls back to {@code fallback} when the
    /// snapshot is empty or unreadable.
    private static int dominantColor(Image img, int fallback) {
        try {
            int[] rgb = img.getRGB();
            java.util.HashMap counts = new java.util.HashMap();
            int best = fallback;
            int bestN = 0;
            for (int pixel : rgb) {
                if (((pixel >>> 24) & 0xff) < 128) {
                    continue;
                }
                Integer key = Integer.valueOf(pixel & 0xffffff);
                Object prev = counts.get(key);
                // instanceof rather than a bare cast inside this try: ParparVM's
                // CHECKCAST is unchecked, so a failed cast does not throw on iOS
                // and the catch below would never see it -- the wrong object
                // would simply be read as an Integer.
                int n = prev instanceof Integer ? ((Integer) prev).intValue() + 1 : 1;
                counts.put(key, Integer.valueOf(n));
                if (n > bestN) {
                    bestN = n;
                    best = key.intValue();
                }
            }
            return bestN == 0 ? fallback : best;
        } catch (Throwable t) {
            return fallback;
        }
    }

    /// Mixes two packed RGB colours, channel by channel.
    private static int blend(int from, int to, float t) {
        int r = lerp((from >> 16) & 0xff, (to >> 16) & 0xff, t);
        int g = lerp((from >> 8) & 0xff, (to >> 8) & 0xff, t);
        int b = lerp(from & 0xff, to & 0xff, t);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void cleanup() {
        // The base releases source and destination -- both forms and their whole
        // component trees, which a caller holding this transition (from
        // Display.getRunningTransition) otherwise kept reachable after it ended.
        super.cleanup();
        sourceBuffer = null;
        destBuffer = null;
        originBuffer = null;
        motion = null;
        path = null;
    }

    @Override
    public Transition copy(boolean reverse) {
        ContainerTransformTransition t =
                new ContainerTransformTransition(componentName, duration);
        t.closing = reverse;
        return t;
    }
}
