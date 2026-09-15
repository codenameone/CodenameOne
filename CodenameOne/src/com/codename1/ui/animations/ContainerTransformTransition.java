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
import com.codename1.ui.Display;
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
public class ContainerTransformTransition extends Transition {

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
            surfaceColor = origin.getStyle().getBgColor();
            openColor = openPage().getStyle().getBgColor();
            // WITH its background. A button's colour usually comes from its border or a
            // painter rather than from bgColor, so a snapshot without the background is a
            // bare glyph and the style's colour is whatever the theme happened to set --
            // which is how the surface came out pale where the reference's button is
            // still its own colour for the first fifth of the run.
            originBuffer = Image.createImage(startW, startH, 0);
            origin.paintComponent(originBuffer.getGraphics(), true);
            surfaceColor = centreColor(originBuffer, origin.getStyle().getBgColor());
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
        int radius = lerp(startRadius, 0, t);

        int[] clip = g.getClip();
        if (radius > 0 && g.isShapeClipSupported()) {
            g.setClip(roundRect(x, y, w, h, radius));
        } else {
            g.setClip(x, y, w, h);
        }

        // The surface holds the tapped thing's colour for the first fifth, crosses to the
        // page's over the second, and is the page's thereafter.
        // Opening runs the tapped thing's colour to the page's; closing runs it back.
        g.setColor(closing ? blend(openColor, surfaceColor, cross)
                : blend(surfaceColor, openColor, cross));
        g.fillRect(x, y, w, h);

        // The tapped content stays fully opaque and is simply covered as the page arrives
        // over it, which is what the fade variant of the transform does.
        if (originBuffer != null) {
            g.drawImage(originBuffer, x + (w - originBuffer.getWidth()) / 2,
                    y + (h - originBuffer.getHeight()) / 2);
        }

        float open = closing ? 1f - cross : cross;
        if (open > 0) {
            int old = g.getAlpha();
            g.setAlpha((int) (255 * open));
            // Anchored to the surface, not to the screen: the page grows with the box out
            // of the corner it started in, which is what makes it read as the same object
            // rather than a page revealed through a window.
            g.drawImage(openBuffer(), x, y);
            g.setAlpha(old);
        }
        g.setClip(clip[0], clip[1], clip[2], clip[3]);
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
        path.moveTo(x + rad, y);
        path.lineTo(x + w - rad, y);
        path.arcTo(x + w - rad, y + rad, x + w, y + rad);
        path.lineTo(x + w, y + h - rad);
        path.arcTo(x + w - rad, y + h - rad, x + w - rad, y + h);
        path.lineTo(x + rad, y + h);
        path.arcTo(x + rad, y + h - rad, x, y + h - rad);
        path.lineTo(x, y + rad);
        path.arcTo(x + rad, y + rad, x + rad, y);
        path.closePath();
        return path;
    }

    private static int lerp(int from, int to, float t) {
        return from + (int) ((to - from) * t);
    }

    /// The colour at the middle of a snapshot, which is the surface colour of whatever
    /// was tapped however it came to be painted. Falls back to {@code fallback} where the
    /// middle pixel is transparent.
    private static int centreColor(Image img, int fallback) {
        try {
            int[] rgb = img.getRGB();
            int px = rgb[(img.getHeight() / 2) * img.getWidth() + img.getWidth() / 2];
            return ((px >>> 24) & 0xff) < 128 ? fallback : (px & 0xffffff);
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
