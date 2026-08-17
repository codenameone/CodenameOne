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
package com.codename1.impl.html5;

import com.codename1.html5.js.dom.CSSStyleDeclaration;
import com.codename1.html5.js.dom.HTMLDocument;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.impl.html5.HTML5Implementation.NativeFont;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders Codename One text as real DOM text above the canvas.
 *
 * <p>Canvas text is a bitmap: it cannot be selected, copied, or found with the browser's
 * find-in-page, and it rasterizes differently from the browser's own text. This layer
 * intercepts each {@code drawString} destined for the display surface and emits a positioned
 * DOM element instead, so the text the user sees is real text.</p>
 *
 * <p>Codename One remains the sole layout authority. By the time a run reaches this class the
 * framework has already broken the line and fixed its position, so each run is emitted as a
 * single {@code white-space:pre} element at an absolute coordinate. The browser is therefore
 * unable to wrap or reflow it, which is what keeps a metrics disagreement between
 * {@code measureText} and DOM text layout to a sub-pixel rendering difference rather than a
 * clipped or overflowing label.</p>
 *
 * <p>Each run is wrapped in an element clipped to the graphics clip that was in force when the
 * run was drawn. That reproduces the canvas behaviour for text inside a scrolled container --
 * the clip Codename One computed already accounts for every ancestor -- without this layer
 * needing to know anything about the component hierarchy.</p>
 *
 * <p>The layer is marked {@code aria-hidden}: assistive technology reads the semantic overlay
 * maintained by {@link JavaScriptSemanticOverlay}, so the same words are never announced
 * twice. Find-in-page and selection operate on the rendered text regardless.</p>
 *
 * <p>Runs are pooled per owning component and reused across repaints, because a scrolling list
 * repaints continuously and creating an element per run per frame would swamp the worker
 * bridge. Nothing is ever read back from the DOM.</p>
 *
 * @author Codename One
 */
public final class JavaScriptTextLayer {

    /**
     * A single pooled text run: an element clipped to the graphics clip, holding the text.
     */
    private static final class Run {
        private final HTMLElement clip;
        private final HTMLElement text;
        // Held rather than re-fetched. getStyle() is a property read, which crosses to the main
        // thread and parks the worker until it answers; doing that twice per run per frame was
        // enough to wedge the VM on a screen that repaints continuously.
        private final CSSStyleDeclaration clipStyle;
        private final CSSStyleDeclaration textStyle;
        private String clipCss;
        private String textCss;
        private String content;
        private boolean attached;
        private boolean everAttached;
        private int lastY = Integer.MIN_VALUE;
        private int lastX = Integer.MIN_VALUE;
        // The clip this run was last given, in Codename One pixels. A repaint of part of the
        // screen hands the same run a clip narrowed to the dirty region, which is not the clip
        // the run is subject to -- see promote().
        private int clipX = Integer.MIN_VALUE;
        private int clipY;
        private int clipW;
        private int clipH;
        private int claimedPass;
        // Where the run landed, in Codename One pixels, and the draw order it was promoted at.
        // Both are needed to tell whether something drawn on the canvas afterwards covers it.
        private int coverX;
        private int coverY;
        private int coverW;
        private int coverH;

        Run(HTMLElement clip, HTMLElement text) {
            this.clip = clip;
            this.text = text;
            this.clipStyle = clip.getStyle();
            this.textStyle = text.getStyle();
        }
    }

    /**
     * The pool of runs belonging to one component.
     */
    private static final class ComponentRuns {
        private final List<Run> runs = new ArrayList<Run>();
    }

    /**
     * One entry of the paint stack. Containers recurse into their children between their own
     * before/after callbacks, so the component that owns the run being drawn is the innermost
     * open frame rather than a single "current" component.
     */
    private static final class Frame {
        private Component component;
        private ComponentRuns runs;
        private int sequence;
        private int paintPass;
        private boolean cellRenderer;
        private boolean promotable;
        private boolean covering;
        private boolean clipEmpty;
    }

    private final HTMLDocument document;
    private final HTMLElement container;
    private final Map<Component, ComponentRuns> byComponent = new HashMap<Component, ComponentRuns>();
    private final List<Frame> stack = new ArrayList<Frame>();
    private int depth;
    private int cellRendererDepth;
    private boolean suspended;
    private int drawSequence;
    private int paintPass;
    /**
     * Components whose text has been found underneath something drawn on the canvas. The layer
     * is above the canvas as a whole, so a promoted run cannot be covered by a later canvas draw
     * the way canvas text would have been -- the only faithful answer is to leave that
     * component's text on the canvas from then on.
     */
    private final Set<Component> canvasOnly = new HashSet<Component>();
    /**
     * The region the frame being painted was asked to repaint, in Codename One pixels.
     */
    private int frameDirtyX;
    private int frameDirtyY;
    private int frameDirtyW = Integer.MAX_VALUE;
    private int frameDirtyH = Integer.MAX_VALUE;

    private boolean reattachedThisFrame;

    /**
     * Creates a text layer.
     *
     * @param document the host document used to create elements
     * @param container the layer root, already attached to the document
     */
    public JavaScriptTextLayer(HTMLDocument document, HTMLElement container) {
        this.document = document;
        this.container = container;
    }

    /**
     * Suspends promotion. While suspended every run stays on the canvas and the layer is
     * hidden, which is what keeps form transitions coherent: a transition paints two
     * pre-rendered offscreen buffers rather than painting components, so the text baked into
     * those buffers must be the only text on screen.
     *
     * @param value true to route text back to the canvas
     */
    public void setSuspended(boolean value) {
        if (suspended == value) {
            return;
        }
        suspended = value;
        container.getStyle().setProperty("display", value ? "none" : "block");
    }

    /**
     * Returns true while promotion is suspended.
     *
     * @return true when text is being left on the canvas
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Reports, and clears, whether a run was attached after having been detached.
     *
     * <p>Only repainted runs get a fresh stacking index, so after a re-attach the frame holds a
     * mix of old and new indices. The caller repaints the whole form to put them back in
     * agreement.</p>
     *
     * @return true when a run was re-attached since the last call
     */
    public boolean consumeReattachFlag() {
        boolean value = reattachedThisFrame;
        reattachedThisFrame = false;
        return value;
    }

    /**
     * Returns true while a component paint is open.
     *
     * <p>Callers use this to recognise the start of a frame, which is the only point at which
     * suspension may change: flipping it once painting has begun would apply to part of a frame
     * only.</p>
     *
     * @return true when at least one component paint is in progress
     */
    public boolean isPainting() {
        return depth > 0;
    }

    /**
     * Marks the start of a component's paint. Runs are keyed by component and by their order
     * within that component's paint, which is what lets them be reused across repaints instead
     * of being recreated.
     *
     * @param component the component about to paint
     */
    public void beginComponent(Component component, boolean covering, boolean editing,
            boolean clipEmpty, int clipX, int clipY, int clipW, int clipH) {
        if (depth == 0) {
            // The outermost paint of a frame carries the region the framework decided to
            // repaint. Every clip inside the frame is that region intersected with whatever the
            // components impose, which is what lets a clip narrowed by the region be told from
            // one narrowed by a container that really did get smaller.
            frameDirtyX = clipX;
            frameDirtyY = clipY;
            frameDirtyW = clipW;
            frameDirtyH = clipH;
        }
        while (stack.size() <= depth) {
            stack.add(new Frame());
        }
        Frame frame = stack.get(depth);
        frame.component = component;
        frame.sequence = 0;
        // Identifies this paint, so a run matched by one line of it cannot be matched again by
        // the next line of the same paint.
        frame.paintPass = ++paintPass;
        frame.runs = component == null ? null : byComponent.get(component);
        frame.cellRenderer = component != null && component.isCellRenderer();
        // Resolved here rather than at flush time: painting happens before the frame is
        // drained, so a value cached per drain would not be set yet on the very first paint and
        // that frame's text would fall back to the canvas and then be promoted on top of itself.
        // Peers are painted behind the canvas and pointer routing decides between them by
        // probing the canvas alpha, so a glyph moved into the DOM stops contributing hit-test
        // pixels: a transparent link or label over a video would let clicks through to the peer
        // beneath it. While a form has peers its text stays on the canvas.
        frame.promotable = component != null && !editing
                && com.codename1.ui.Accessor.getActivePeerCount() == 0
                && component.getComponentForm() == Display.getInstance().getCurrent();
        frame.covering = covering;
        frame.clipEmpty = clipEmpty;
        if (frame.cellRenderer) {
            cellRendererDepth++;
        }
        depth++;
    }

    /**
     * Marks the end of a component's paint and releases any runs it no longer draws.
     *
     * @param component the component that finished painting
     */
    public void endComponent(Component component) {
        if (depth == 0) {
            return;
        }
        depth--;
        Frame frame = stack.get(depth);
        if (frame.cellRenderer) {
            cellRendererDepth--;
            frame.cellRenderer = false;
        }
        frame.promotable = false;
        // Only when the paint could see the whole component does drawing fewer runs mean the
        // rest are stale. A partial repaint hands it a clip that covers part of it -- or none of
        // it, since the hooks still run when nothing intersects -- and releasing the tail then
        // would detach text outside the dirty region whose pixels were never repainted, making
        // unrelated labels, or the other lines of a multiline component, disappear.
        if (frame.runs != null && frame.component == component) {
            if (frame.clipEmpty) {
                // Scrolled out of its container, so it drew nothing and will not draw again
                // until it returns. Its text has to go now or it would hang outside the
                // container it belongs to.
                releaseFrom(frame.runs, 0);
            } else if (frame.covering) {
                releaseFrom(frame.runs, frame.sequence);
            }
        }
        frame.covering = false;
        frame.clipEmpty = false;
        frame.component = null;
        frame.runs = null;
        frame.sequence = 0;
    }

    /**
     * Promotes a text run to the DOM.
     *
     * @param str the text to draw
     * @param x the absolute x coordinate of the left edge, in Codename One pixels
     * @param y the absolute y coordinate of the top of the text, in Codename One pixels
     * @param clipX clip rectangle x, in Codename One pixels
     * @param clipY clip rectangle y, in Codename One pixels
     * @param clipW clip rectangle width, in Codename One pixels
     * @param clipH clip rectangle height, in Codename One pixels
     * @param color the text colour as a packed RGB value
     * @param alpha the text alpha, 0 to 255
     * @param font the resolved font, may be null
     * @param ratio device pixel ratio used to convert to CSS pixels
     * @return true when the run was taken over by this layer and must not be drawn on canvas
     */
    public boolean promote(String str, int x, int y, int clipX, int clipY, int clipW, int clipH,
            int color, int alpha, NativeFont font, double ratio) {
        if (suspended || depth == 0 || font == null || str == null || str.length() == 0) {
            return false;
        }
        // A cell renderer is one component instance stamped at many positions, so runs cannot be
        // keyed by it: every row would overwrite the previous row's element and only the last
        // would survive. Renderer subtrees keep their text on the canvas.
        if (cellRendererDepth > 0) {
            return false;
        }
        Frame frame = stack.get(depth - 1);
        if (!frame.promotable) {
            // The layer sits above the canvas as a whole, so nothing drawn on the canvas after a
            // run can cover it. A modal dialog paints the form beneath it as its own backdrop;
            // promoting that form's text would float it over the dialog. Anything outside the
            // displayed form therefore stays on the canvas, where paint order still applies.
            return false;
        }
        if (clipW <= 0 || clipH <= 0) {
            return false;
        }
        if (canvasOnly.contains(frame.component)) {
            return false;
        }
        if (frame.runs == null) {
            frame.runs = new ComponentRuns();
            byComponent.put(frame.component, frame.runs);
        }
        // A paint that can see the whole component draws its runs in order, so position in that
        // order identifies them. A clipped paint draws only the lines the clip reaches, so the
        // same position would mean a different line -- a repaint reaching only the second line
        // of a text area would write it into the first line's slot. There the run is matched by
        // what it says instead, which is stable across partial paints, and the run is still
        // updated so a component scrolling across the viewport edge keeps up rather than
        // freezing at its last fully visible position.
        Run run = frame.covering ? obtain(frame.runs, frame.sequence)
                : obtainClipped(frame.runs, str, x, y, frame.paintPass);
        run.claimedPass = frame.paintPass;
        frame.sequence++;

        double scale = ratio <= 0 ? 1 : ratio;
        // A repaint of part of the screen redraws whatever the dirty region touches, through a
        // clip narrowed to that region. The run is not subject to that clip -- it is subject to
        // whatever its container imposes -- and narrowing the element to the dirty rectangle
        // would cut off every glyph outside it, in an area nothing repainted. So a clip that
        // sits inside the one this run already has is only taken when the run has moved: a
        // component scrolling out of view narrows its clip for real, and that always moves it.
        int useClipX = clipX;
        int useClipY = clipY;
        int useClipW = clipW;
        int useClipH = clipH;
        if (run.clipX != Integer.MIN_VALUE && run.lastX == x && run.lastY == y
                && isRegionNarrowing(run, clipX, clipY, clipW, clipH)) {
            useClipX = run.clipX;
            useClipY = run.clipY;
            useClipW = run.clipW;
            useClipH = run.clipH;
        }
        run.clipX = useClipX;
        run.clipY = useClipY;
        run.clipW = useClipW;
        run.clipH = useClipH;
        // pointer-events stays off so the layer cannot intercept input destined for the canvas,
        // which still owns all hit testing. Find-in-page and assistive technology do not depend
        // on hit testing; drag-selection does, and is deliberately not enabled here.
        StringBuilder clipCss = new StringBuilder(
                "position:absolute;overflow:hidden;pointer-events:none;");
        clipCss.append("left:").append(useClipX / scale).append("px;");
        clipCss.append("top:").append(useClipY / scale).append("px;");
        clipCss.append("width:").append(useClipW / scale).append("px;");
        clipCss.append("height:").append(useClipH / scale).append("px;");
        // Stacking follows draw order rather than DOM insertion order, so a run that is hidden
        // and shown again does not jump to the top of the stack.
        //
        // The counter is monotonic and is NOT reset per frame. Resetting cannot order a partial
        // repaint correctly: only the components that repainted would be renumbered, so a dirty
        // component starting again from 1 could fall beneath untouched runs still carrying
        // higher numbers from an earlier full frame. Left monotonic, every index remains
        // comparable with every other, and the most recently painted run is on top -- which is
        // what the canvas would have done.
        drawSequence++;
        if (drawSequence == Integer.MAX_VALUE) {
            // Unreachable in practice; renumber from a clean slate rather than wrap.
            drawSequence = 1;
            reattachedThisFrame = true;
        }
        clipCss.append("z-index:").append(drawSequence).append(";");
        String clipDeclaration = clipCss.toString();
        if (!clipDeclaration.equals(run.clipCss)) {
            run.clipStyle.setCssText(clipDeclaration);
            run.clipCss = clipDeclaration;
        }

        // The run is positioned relative to its clip element, so the two move together and a
        // scroll only has to rewrite coordinates rather than restructure anything.
        StringBuilder textCss = new StringBuilder("position:absolute;white-space:pre;");
        textCss.append("left:").append((x - useClipX) / scale).append("px;");
        textCss.append("top:").append((y - useClipY) / scale).append("px;");
        // The font shorthand carries its own line-height ("18.9px/1.0"), so it has to be
        // written before the explicit line-height or it would reset it.
        textCss.append("font:").append(font.getScaledCSS()).append(";");
        // Codename One lays text out against fontHeight(), so using it as the line box keeps
        // the DOM run on the same vertical rhythm as the canvas text it replaces.
        textCss.append("line-height:").append(font.fontHeight() / scale).append("px;");
        textCss.append("color:").append(HTML5Graphics.color(color)).append(";");
        if (alpha < 255) {
            textCss.append("opacity:").append(alpha / 255.0).append(";");
        }
        // Comparing against the last applied declaration keeps a repaint that changes nothing --
        // the common case while scrolling a list whose rows are unchanged -- from writing
        // anything at all across the bridge.
        String textDeclaration = textCss.toString();
        if (!textDeclaration.equals(run.textCss)) {
            run.textStyle.setCssText(textDeclaration);
            run.textCss = textDeclaration;
        }

        if (!str.equals(run.content)) {
            run.text.setTextContent(str);
            run.content = str;
        }
        run.lastY = y;
        run.lastX = x;
        // The glyphs, not the clip: a component that draws text and then an image somewhere
        // else inside the same clip would otherwise look like it had covered its own text, and
        // the text would be dropped for nothing. The font measures worker-side from a cache, so
        // asking costs nothing on the bridge.
        int textWidth = font.stringWidth(str);
        int textHeight = font.fontHeight();
        int coverLeft = Math.max(x, useClipX);
        int coverTop = Math.max(y, useClipY);
        int coverRight = Math.min(x + textWidth, useClipX + useClipW);
        int coverBottom = Math.min(y + textHeight, useClipY + useClipH);
        run.coverX = coverLeft;
        run.coverY = coverTop;
        run.coverW = Math.max(0, coverRight - coverLeft);
        run.coverH = Math.max(0, coverBottom - coverTop);
        if (!run.attached) {
            container.appendChild(run.clip);
            run.attached = true;
            if (run.everAttached) {
                // Previously attached, so other runs are still carrying stacking indices from
                // an earlier frame. Ask for a full repaint to bring them all into one pass.
                reattachedThisFrame = true;
            }
            run.everAttached = true;
        }
        return true;
    }

    /**
     * Records which form is on screen and drops the runs that no longer belong on it.
     *
     * <p>Text cannot outlive the component that drew it: a component that has been removed never
     * paints again, so nothing else would ever release its elements. The same applies to a form
     * that is no longer displayed -- its text goes back to the canvas, so any elements it left
     * behind have to go.</p>
     *
     * @param form the form currently displayed, may be null
     */
    public void syncToForm(Form form) {
        for (Iterator<Map.Entry<Component, ComponentRuns>> it = byComponent.entrySet().iterator();
                it.hasNext();) {
            Map.Entry<Component, ComponentRuns> entry = it.next();
            Component component = entry.getKey();
            // Still on the displayed form AND still able to paint. Hiding a component -- or any
            // ancestor of it -- stops it painting without detaching it, so its runs would never
            // be refreshed or released again and would sit above the canvas indefinitely, even
            // though the parent's repaint has already cleared the pixels underneath.
            if (component.getComponentForm() == form && form != null
                    && com.codename1.ui.Accessor.isDisplayable(component)) {
                continue;
            }
            releaseFrom(entry.getValue(), 0);
            it.remove();
        }
        // The ban outlives the runs, so it has to be let go of here as well: a component that
        // has left the form will never paint again, and holding it would keep its whole subtree
        // alive for as long as the layer exists.
        for (Iterator<Component> it = canvasOnly.iterator(); it.hasNext();) {
            Component component = it.next();
            if (component.getComponentForm() != form || form == null
                    || !com.codename1.ui.Accessor.isDisplayable(component)) {
                it.remove();
            }
        }
    }

    /**
     * Records that something opaque was drawn straight onto the canvas.
     *
     * <p>The canvas cannot cover this layer. Where the original renderer would have drawn an
     * image over a label and hidden it, a promoted run would keep showing through -- as it did
     * for a tab bar that draws its selected tab through a composited lens, leaving the plain
     * text of the pass before it floating over the finished result. A run caught underneath goes
     * back to the canvas, and its component stays there: the alternative is a frame that
     * disagrees with itself, or one that oscillates between the two.</p>
     *
     * @param x left edge of what was drawn, in Codename One pixels
     * @param y top edge
     * @param w width
     * @param h height
     */
    /**
     * Decides whether a draw really reaches a rectangle, for a draw whose shape is not the
     * rectangle it reports.
     */
    public interface CoverTest {
        /**
         * @param x left edge of the rectangle in question, in Codename One pixels
         * @param y top edge
         * @param w width
         * @param h height
         * @return true when the draw covers all of it
         */
        boolean covers(int x, int y, int w, int h);
    }

    public void noteCanvasCover(int x, int y, int w, int h) {
        noteCanvasCover(x, y, w, h, null);
    }

    public void noteCanvasCover(int x, int y, int w, int h, CoverTest test) {
        if (suspended || w <= 0 || h <= 0 || byComponent.isEmpty()) {
            return;
        }
        // Who is drawing decides what the draw means for text already in the DOM. A component
        // painting its own background covers the text it is about to promote again a moment
        // later, and so does a container painting behind the children it is about to paint --
        // neither is hiding anything. A component painting over text that belongs somewhere else
        // on the form is, and that text has to go back to the canvas where it can be covered.
        Component painter = depth == 0 ? null : stack.get(depth - 1).component;
        int painterPass = depth == 0 ? -1 : stack.get(depth - 1).paintPass;
        List<Component> covered = null;
        for (Iterator<Map.Entry<Component, ComponentRuns>> it = byComponent.entrySet().iterator();
                it.hasNext();) {
            Map.Entry<Component, ComponentRuns> entry = it.next();
            ComponentRuns runs = entry.getValue();
            for (int i = 0; i < runs.runs.size(); i++) {
                Run run = runs.runs.get(i);
                if (!run.attached) {
                    continue;
                }
                // Promoted by the paint that is drawing right now, so this draw really does come
                // after it -- a component covering its own text, which the canvas would have
                // shown and the DOM cannot.
                // At or after the painter's own pass: either the painter drew this text itself
                // and is now drawing over it, or a child of it did during this same paint and the
                // painter has come back to draw on top -- a container that calls super.paint(g)
                // and then paints over its children. Both are covered on the canvas, so both
                // belong here. A run from an earlier pass predates this paint and will be drawn
                // again by it.
                boolean drawnThisPaint = painterPass >= 0 && run.claimedPass >= painterPass;
                if (!drawnThisPaint && painter != null
                        && isSelfOrDescendant(entry.getKey(), painter)) {
                    continue;
                }
                if (run.coverW <= 0 || run.coverH <= 0
                        || run.coverX + run.coverW <= x || x + w <= run.coverX
                        || run.coverY + run.coverH <= y || y + h <= run.coverY) {
                    continue;
                }
                if (test != null && !test.covers(run.coverX, run.coverY, run.coverW, run.coverH)) {
                    // The draw's own outline does not reach this text, whatever its bounding
                    // rectangle says -- a filled triangle around a label. Nothing is hidden, so
                    // the text stays where it is.
                    continue;
                }
                if (covered == null) {
                    covered = new ArrayList<Component>();
                }
                covered.add(entry.getKey());
                break;
            }
        }
        if (covered == null) {
            return;
        }
        for (int i = 0; i < covered.size(); i++) {
            Component component = covered.get(i);
            canvasOnly.add(component);
            ComponentRuns runs = byComponent.remove(component);
            if (runs != null) {
                releaseFrom(runs, 0);
            }
        }
        // The text has to be drawn again, and this time on the canvas, so the whole form is
        // asked for rather than the dirty region: the flush already carries the reattach flag
        // to the same place.
        reattachedThisFrame = true;
    }

    /**
     * True when a component is the one painting, or somewhere inside it -- in which case the
     * paint in progress is about to draw it again.
     *
     * @param component the component owning a promoted run
     * @param painter the component whose paint is drawing
     * @return true when the run will be repainted by this paint
     */
    private boolean isSelfOrDescendant(Component component, Component painter) {
        Component walk = component;
        while (walk != null) {
            if (walk == painter) {
                return true;
            }
            walk = walk.getParent();
        }
        return false;
    }

    /**
     * Detaches every run and drops the pool.
     */
    public void clear() {
        container.setInnerHTML("");
        byComponent.clear();
        canvasOnly.clear();
        stack.clear();
        depth = 0;
        cellRendererDepth = 0;
        drawSequence = 0;
        reattachedThisFrame = false;
    }

    /**
     * True when a clip is exactly what the frame's repaint region makes of the clip this run
     * already has -- in other words, the run is subject to the same clip as before and only the
     * region being repainted is narrower.
     *
     * <p>A container that really did get smaller produces a different rectangle: its own clip
     * intersected with the region, which is smaller than the run's old clip intersected with it.
     * That one has to be taken, or promoted text would stay visible outside the container it
     * belongs to.</p>
     */
    private boolean isRegionNarrowing(Run run, int clipX, int clipY, int clipW, int clipH) {
        int left = Math.max(run.clipX, frameDirtyX);
        int top = Math.max(run.clipY, frameDirtyY);
        long right = Math.min((long) run.clipX + run.clipW, (long) frameDirtyX + frameDirtyW);
        long bottom = Math.min((long) run.clipY + run.clipH, (long) frameDirtyY + frameDirtyH);
        return clipX == left && clipY == top
                && (long) clipX + clipW == right && (long) clipY + clipH == bottom;
    }

    /**
     * Finds the run already showing this text, or adds one.
     *
     * <p>Used when the paint is clipped and ordering cannot be trusted.</p>
     */
    private Run obtainClipped(ComponentRuns runs, String content, int x, int y, int pass) {
        // A run already matched by an earlier line of this same paint is off limits. Lines move
        // as a block when a multiline component scrolls, so the new position of one line is
        // routinely the old position of the line before it: without this the second line would
        // match -- and overwrite -- the run the first line had just taken, leaving the displaced
        // run attached with stale text and duplicating a line on screen.
        for (int i = 0; i < runs.runs.size(); i++) {
            Run candidate = runs.runs.get(i);
            // Position first: a line keeps its baseline when its text changes, so this is what
            // matches a clipped repaint of new content to the run showing the old. Both
            // coordinates, not just the baseline: a component can draw two strings side by side
            // on one line, and matching by baseline alone would move the first one's element
            // onto the second -- leaving the first nowhere and the second twice over.
            if (candidate.lastY == y && candidate.lastX == x && candidate.claimedPass != pass) {
                return candidate;
            }
        }
        // Then the baseline alone, for a run whose text has moved horizontally -- a label that
        // re-centres when its text changes -- but only where nothing else on that line is in
        // doubt, so a side-by-side pair is never confused for one another.
        Run onBaseline = null;
        for (int i = 0; i < runs.runs.size(); i++) {
            Run candidate = runs.runs.get(i);
            if (candidate.lastY == y && candidate.claimedPass != pass) {
                if (onBaseline != null) {
                    onBaseline = null;
                    break;
                }
                onBaseline = candidate;
            }
        }
        if (onBaseline != null) {
            return onBaseline;
        }
        // Then content: a line keeps its text when it scrolls, which is what moves the baseline.
        for (int i = 0; i < runs.runs.size(); i++) {
            Run candidate = runs.runs.get(i);
            if (content.equals(candidate.content) && candidate.claimedPass != pass) {
                return candidate;
            }
        }
        return obtain(runs, runs.runs.size());
    }

    private Run obtain(ComponentRuns runs, int index) {
        while (runs.runs.size() <= index) {
            HTMLElement clip = document.createElement("div");
            HTMLElement text = document.createElement("span");
            clip.appendChild(text);
            runs.runs.add(new Run(clip, text));
        }
        return runs.runs.get(index);
    }

    private void releaseFrom(ComponentRuns runs, int from) {
        for (int i = from; i < runs.runs.size(); i++) {
            Run run = runs.runs.get(i);
            if (run.attached) {
                container.removeChild(run.clip);
                run.attached = false;
            }
        }
    }

}
