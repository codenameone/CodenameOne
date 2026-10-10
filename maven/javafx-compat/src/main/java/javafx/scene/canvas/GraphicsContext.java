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
package javafx.scene.canvas;

import java.util.ArrayList;

import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Matrix2D;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.SvgPath;
import com.codename1.fxcompat.runtime.Units;

import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

/// Draws on a [Canvas] with immediate mode calls.
///
/// #### A display list, not pixels
///
/// Each drawing call is recorded with the state it was made in (paint,
/// line attributes, transform, global alpha, clip) and replayed through
/// the layer's renderer every time the canvas is painted. What was drawn
/// therefore persists between paints, exactly as on a JavaFX canvas.
///
/// The list is kept from growing without bound in three ways:
///
/// - `clearRect` over the whole canvas, with no clip in force, empties
///   the list. This is what an animation that redraws every frame does
///   first, so its list never holds more than one frame.
/// - Any other `clearRect` removes the recorded calls that lie wholly
///   inside the cleared rectangle. If calls remain that it only partly
///   covers, everything recorded so far is drawn once into an off screen
///   image at the display's resolution, the cleared area is made
///   transparent in it, and the image replaces the list.
/// - When more than [#CN1_MAX_RECORDED_CALLS] calls are recorded they
///   are replaced by such an image in the same way.
/// - A second image drawn over the whole canvas does the same at once. An
///   application that shows a picture it keeps computing draws one every
///   frame and clears nothing, and each recorded call would keep a frame
///   of pixels alive.
///
/// Drawing that was turned into an image is no longer redrawn sharp if
/// the display scale changes afterwards, and is not enlarged when the
/// canvas grows.
///
/// #### How calls are drawn
///
/// - Fills and strokes are exact under any transform. A gradient strokes
///   with its middle colour.
/// - Text is drawn by the platform. `fillText` with a maximum width draws
///   with a proportionally smaller font where JavaFX squeezes the glyphs.
///   **Glyph outlines are not available, so `strokeText` fills the text
///   with the stroke paint** instead of outlining it.
/// - `clip()` is exact on a port that can clip to a shape and the
///   bounding box of the path on one that cannot. Where several clips are
///   in force the last one is exact and the earlier ones are reduced to
///   their bounding boxes, because Codename One replaces a shape clip
///   rather than intersecting it.
/// - `drawImage` draws nothing for a `null` image or one that failed to
///   load.
///
/// #### Not part of this layer
///
/// Effects, blend modes, `getPixelWriter()`, font smoothing and image
/// smoothing settings.
public final class GraphicsContext {

    /// The number of recorded drawing calls above which the list is
    /// replaced by an image of what it draws.
    public static final int CN1_MAX_RECORDED_CALLS = 4096;

    private static final int FILL = 0;
    private static final int STROKE = 1;
    private static final int TEXT = 2;
    private static final int IMAGE = 3;
    private static final FxPath[] NO_CLIPS = new FxPath[0];

    /// One recorded call.
    private static final class Op {
        int kind;
        FxPath path;
        Paint paint;
        double[] matrix;
        double alpha;
        double width;
        StrokeLineCap cap;
        StrokeLineJoin join;
        double miter;
        double[] dashes;
        double dashOffset;
        String text;
        Font font;
        com.codename1.ui.Image image;
        boolean backing;
        /// Whether the backing image may be drawn on again: it was made
        /// to be drawn on and nothing replaced it by one made of pixels.
        boolean reusable;
        /// An image, not the backing one, that covers the whole canvas.
        boolean cover;
        double x;
        double y;
        double w;
        double h;
        FxPath[] clips;
        double[] bounds;
    }

    /// Everything `save()` remembers.
    private static final class State {
        double[] matrix = Matrix2D.identity();
        Paint fill = Color.BLACK;
        Paint stroke = Color.BLACK;
        double lineWidth = 1;
        StrokeLineCap cap = StrokeLineCap.SQUARE;
        StrokeLineJoin join = StrokeLineJoin.MITER;
        double miter = 10;
        double[] dashes = new double[0];
        double dashOffset;
        double alpha = 1;
        Font font = Font.getDefault();
        TextAlignment align = TextAlignment.LEFT;
        VPos baseline = VPos.BASELINE;
        FillRule rule = FillRule.NON_ZERO;
        FxPath[] clips = NO_CLIPS;

        State copy() {
            State s = new State();
            s.matrix = copyOf(matrix);
            s.fill = fill;
            s.stroke = stroke;
            s.lineWidth = lineWidth;
            s.cap = cap;
            s.join = join;
            s.miter = miter;
            s.dashes = dashes;
            s.dashOffset = dashOffset;
            s.alpha = alpha;
            s.font = font;
            s.align = align;
            s.baseline = baseline;
            s.rule = rule;
            s.clips = clips;
            return s;
        }
    }

    private final Canvas canvas;
    private final ArrayList<Op> ops = new ArrayList<Op>();
    private final ArrayList<State> saved = new ArrayList<State>();
    private State state = new State();
    private final FxPath path = new FxPath();
    /// How many recorded images cover the whole canvas.
    private int covers;

    GraphicsContext(Canvas canvas) {
        this.canvas = canvas;
    }

    private static double[] copyOf(double[] a) {
        double[] out = new double[a.length];
        System.arraycopy(a, 0, out, 0, a.length);
        return out;
    }

    /// Returns the canvas this context draws on.
    public Canvas getCanvas() {
        return canvas;
    }

    // ------------------------------------------------------------- state

    /// Remembers the current state: paints, line attributes, font, text
    /// alignment, fill rule, global alpha, transform and clip. The path
    /// is not part of it.
    public void save() {
        saved.add(state.copy());
    }

    /// Returns to the state of the matching [#save()]; does nothing when
    /// there is none.
    public void restore() {
        if (!saved.isEmpty()) {
            state = saved.remove(saved.size() - 1);
        }
    }

    /// Moves the origin of the coordinates that follow.
    public void translate(double x, double y) {
        state.matrix = Matrix2D.multiply(state.matrix, new double[] {1, 0, 0, 1, x, y});
    }

    /// Scales the coordinates that follow.
    public void scale(double x, double y) {
        state.matrix = Matrix2D.multiply(state.matrix, new double[] {x, 0, 0, y, 0, 0});
    }

    /// Rotates the coordinates that follow, clockwise, in degrees.
    public void rotate(double degrees) {
        double r = Math.toRadians(degrees);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        state.matrix = Matrix2D.multiply(state.matrix, new double[] {cos, sin, -sin, cos, 0, 0});
    }

    /// Applies a matrix, given column by column, to the coordinates that
    /// follow.
    public void transform(double mxx, double myx, double mxy, double myy, double mxt, double myt) {
        state.matrix = Matrix2D.multiply(state.matrix, new double[] {mxx, myx, mxy, myy, mxt, myt});
    }

    /// Applies a transform to the coordinates that follow.
    public void transform(Affine xform) {
        if (xform != null) {
            transform(xform.getMxx(), xform.getMyx(), xform.getMxy(), xform.getMyy(), xform.getTx(), xform.getTy());
        }
    }

    /// Replaces the transform with a matrix given column by column.
    public void setTransform(double mxx, double myx, double mxy, double myy, double mxt, double myt) {
        state.matrix = new double[] {mxx, myx, mxy, myy, mxt, myt};
    }

    /// Replaces the transform.
    public void setTransform(Affine xform) {
        if (xform != null) {
            setTransform(xform.getMxx(), xform.getMyx(), xform.getMxy(), xform.getMyy(), xform.getTx(),
                    xform.getTy());
        }
    }

    /// Copies the current transform into an `Affine`, a new one when
    /// `xform` is `null`, and returns it.
    public Affine getTransform(Affine xform) {
        Affine out = xform == null ? new Affine() : xform;
        double[] m = state.matrix;
        out.setToTransform(m[0], m[2], m[4], m[1], m[3], m[5]);
        return out;
    }

    /// Returns a copy of the current transform.
    public Affine getTransform() {
        return getTransform(null);
    }

    /// Sets the opacity everything is drawn with, clamped to 0..1.
    public void setGlobalAlpha(double alpha) {
        state.alpha = alpha > 1 ? 1 : (alpha < 0 ? 0 : alpha);
    }

    /// Returns the opacity everything is drawn with.
    public double getGlobalAlpha() {
        return state.alpha;
    }

    /// Sets the paint of fills; `null` is ignored.
    public void setFill(Paint p) {
        if (p != null) {
            state.fill = p;
        }
    }

    /// Returns the paint of fills.
    public Paint getFill() {
        return state.fill;
    }

    /// Sets the paint of strokes; `null` is ignored.
    public void setStroke(Paint p) {
        if (p != null) {
            state.stroke = p;
        }
    }

    /// Returns the paint of strokes.
    public Paint getStroke() {
        return state.stroke;
    }

    /// Sets the width of strokes; a width that is not positive and
    /// finite is ignored.
    public void setLineWidth(double lw) {
        if (lw > 0 && lw < Double.POSITIVE_INFINITY) {
            state.lineWidth = lw;
        }
    }

    /// Returns the width of strokes.
    public double getLineWidth() {
        return state.lineWidth;
    }

    /// Sets how the ends of open strokes are drawn; `null` is ignored.
    public void setLineCap(StrokeLineCap cap) {
        if (cap != null) {
            state.cap = cap;
        }
    }

    /// Returns how the ends of open strokes are drawn.
    public StrokeLineCap getLineCap() {
        return state.cap;
    }

    /// Sets how corners of strokes are drawn; `null` is ignored.
    public void setLineJoin(StrokeLineJoin join) {
        if (join != null) {
            state.join = join;
        }
    }

    /// Returns how corners of strokes are drawn.
    public StrokeLineJoin getLineJoin() {
        return state.join;
    }

    /// Sets how far a mitred corner may reach, in line widths; a limit
    /// that is not positive and finite is ignored.
    public void setMiterLimit(double ml) {
        if (ml > 0 && ml < Double.POSITIVE_INFINITY) {
            state.miter = ml;
        }
    }

    /// Returns how far a mitred corner may reach.
    public double getMiterLimit() {
        return state.miter;
    }

    /// Sets the dash pattern: the lengths of dashes and gaps in turn. No
    /// lengths, `null`, a negative or non finite length or lengths that
    /// are all zero give a solid line.
    public void setLineDashes(double... dashes) {
        state.dashes = new double[0];
        if (dashes == null || dashes.length == 0) {
            return;
        }
        boolean positive = false;
        for (int i = 0; i < dashes.length; i++) {
            double d = dashes[i];
            if (!(d >= 0) || d == Double.POSITIVE_INFINITY) {
                return;
            }
            positive |= d > 0;
        }
        if (positive) {
            state.dashes = copyOf(dashes);
        }
    }

    /// Returns a copy of the dash pattern, or `null` for a solid line.
    public double[] getLineDashes() {
        return state.dashes.length == 0 ? null : copyOf(state.dashes);
    }

    /// Sets where in the dash pattern a stroke starts; a non finite
    /// offset is ignored.
    public void setLineDashOffset(double dashOffset) {
        if (dashOffset > Double.NEGATIVE_INFINITY && dashOffset < Double.POSITIVE_INFINITY) {
            state.dashOffset = dashOffset;
        }
    }

    /// Returns where in the dash pattern a stroke starts.
    public double getLineDashOffset() {
        return state.dashOffset;
    }

    /// Sets the font of text; `null` is ignored.
    public void setFont(Font f) {
        if (f != null) {
            state.font = f;
        }
    }

    /// Returns the font of text.
    public Font getFont() {
        return state.font;
    }

    /// Sets which part of a text its x is the position of: its left
    /// edge, centre or right edge. `null` is ignored.
    public void setTextAlign(TextAlignment align) {
        if (align != null) {
            state.align = align;
        }
    }

    /// Returns which part of a text its x is the position of.
    public TextAlignment getTextAlign() {
        return state.align;
    }

    /// Sets which part of a text its y is the position of: the baseline,
    /// top, centre or bottom. `null` is ignored.
    public void setTextBaseline(VPos baseline) {
        if (baseline != null) {
            state.baseline = baseline;
        }
    }

    /// Returns which part of a text its y is the position of.
    public VPos getTextBaseline() {
        return state.baseline;
    }

    /// Sets how the inside of a path or polygon is decided; `null` is
    /// ignored.
    public void setFillRule(FillRule fillRule) {
        if (fillRule != null) {
            state.rule = fillRule;
        }
    }

    /// Returns how the inside of a path or polygon is decided.
    public FillRule getFillRule() {
        return state.rule;
    }

    // --------------------------------------------------------- recording

    private Op op(int kind, double[] matrix) {
        Op o = new Op();
        o.kind = kind;
        o.matrix = copyOf(matrix);
        o.alpha = state.alpha;
        o.clips = state.clips;
        return o;
    }

    private void record(Op o) {
        ops.add(o);
        if (ops.size() > CN1_MAX_RECORDED_CALLS) {
            rasterise();
        }
        canvas.cn1Repaint();
    }

    private void recordFill(FxPath shape, double[] matrix) {
        double[] b = shape.transformed(matrix).bounds();
        if (b == null) {
            return;
        }
        Op o = op(FILL, matrix);
        o.path = shape;
        o.paint = state.fill;
        o.bounds = b;
        record(o);
    }

    private void recordStroke(FxPath shape, double[] matrix, double width) {
        double[] b = shape.transformed(matrix).bounds();
        if (b == null) {
            return;
        }
        Op o = op(STROKE, matrix);
        o.path = shape;
        o.paint = state.stroke;
        o.width = width;
        o.cap = state.cap;
        o.join = state.join;
        o.miter = state.miter;
        o.dashes = state.dashes;
        o.dashOffset = state.dashOffset;
        // Generous: a mitred corner and a square cap reach past half the
        // width, and too large a box only costs an exact clear later.
        double reach = width * Matrix2D.uniformScale(matrix)
                * Math.max(2, state.join == StrokeLineJoin.MITER ? state.miter : 1) / 2;
        o.bounds = new double[] {b[0] - reach, b[1] - reach, b[2] + reach, b[3] + reach};
        record(o);
    }

    private void fillShape(FxPath shape) {
        shape.setEvenOdd(state.rule == FillRule.EVEN_ODD);
        recordFill(shape, state.matrix);
    }

    private void strokeShape(FxPath shape) {
        recordStroke(shape, state.matrix, state.lineWidth);
    }

    /// Draws what was recorded; called by the canvas when it paints.
    void replay(Renderer r) {
        replay(r, 0);
    }

    private void replay(Renderer r, int from) {
        for (int i = from; i < ops.size(); i++) {
            Op o = ops.get(i);
            r.save();
            for (int c = 0; c < o.clips.length; c++) {
                r.clip(o.clips[c]);
            }
            double[] m = o.matrix;
            r.concat(m[0], m[1], m[2], m[3], m[4], m[5]);
            r.multiplyOpacity(o.alpha);
            switch (o.kind) {
                case FILL: {
                    double[] b = o.path.bounds();
                    if (b != null) {
                        r.fill(o.path, o.paint, b[0], b[1], b[2] - b[0], b[3] - b[1]);
                    }
                    break;
                }
                case STROKE:
                    r.stroke(o.path, o.paint, o.width, o.cap, o.join, o.miter, o.dashes, o.dashOffset);
                    break;
                case TEXT:
                    r.drawText(o.text, o.x, o.y, o.font, o.paint);
                    break;
                default:
                    r.drawImage(o.image, o.x, o.y, o.w, o.h);
                    break;
            }
            r.restore();
        }
    }

    /// Replaces the list by one image of what it draws. Answers the
    /// operation holding that image, or `null` when there is nothing to
    /// keep.
    private Op rasterise() {
        if (ops.size() == 1 && ops.get(0).backing) {
            return ops.get(0);
        }
        double s = Units.scale();
        int pw = (int) Math.ceil(canvas.getWidth() * s - 1e-6);
        int ph = (int) Math.ceil(canvas.getHeight() * s - 1e-6);
        covers = 0;
        if (pw <= 0 || ph <= 0 || ops.isEmpty()) {
            ops.clear();
            return null;
        }
        Op first = ops.get(0);
        if (first.backing && first.reusable && first.image.getWidth() == pw && first.image.getHeight() == ph) {
            // The image of the last time is at the bottom of the list and
            // nothing else has it, so what came after is drawn onto it:
            // the same pixels as drawing both onto a new one.
            replay(new Renderer(fresh(first.image, pw, ph), 0, 0), 1);
            ops.clear();
            ops.add(first);
            return first;
        }
        com.codename1.ui.Image image = com.codename1.ui.Image.createImage(pw, ph, 0);
        replay(new Renderer(fresh(image, pw, ph), 0, 0));
        ops.clear();
        Op o = new Op();
        o.kind = IMAGE;
        o.matrix = Matrix2D.identity();
        o.alpha = 1;
        o.clips = NO_CLIPS;
        o.image = image;
        o.backing = true;
        o.reusable = true;
        o.w = pw / s;
        o.h = ph / s;
        o.bounds = new double[] {0, 0, o.w, o.h};
        ops.add(o);
        return o;
    }

    /// The graphics of an image about to be drawn on, as a new one is: no
    /// clip but the image, no matrix, opaque. A port may hand out one
    /// graphics for an image however often it is asked -- the native ports
    /// do -- and that one still has the clip and the matrix the last
    /// drawing left on it.
    private static com.codename1.ui.Graphics fresh(com.codename1.ui.Image image, int pw, int ph) {
        com.codename1.ui.Graphics g = image.getGraphics();
        if (g.isTransformSupported()) {
            g.setTransform(com.codename1.ui.Transform.makeIdentity());
        }
        g.translate(-g.getTranslateX(), -g.getTranslateY());
        g.setClip(0, 0, pw, ph);
        g.setAlpha(255);
        return g;
    }

    // -------------------------------------------------------------- text

    private void text(String text, double x, double y, double maxWidth, boolean limited, Paint paint) {
        if (text == null || text.length() == 0 || (limited && !(maxWidth > 0))) {
            return;
        }
        Font font = state.font;
        int lineCount = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineCount++;
            }
        }
        String[] lines = new String[lineCount];
        int start = 0;
        double widest = 0;
        for (int i = 0; i < lineCount; i++) {
            int feed = text.indexOf('\n', start);
            lines[i] = feed < 0 ? text.substring(start) : text.substring(start, feed);
            start = feed + 1;
            widest = Math.max(widest, Fonts.width(font, lines[i]));
        }
        if (limited && widest > maxWidth) {
            font = Font.font(font.getFamily(), font.cn1Weight(), font.cn1Posture(),
                    font.getSize() * maxWidth / widest);
        }
        double lineHeight = Fonts.lineHeight(font);
        double height = lineHeight * lineCount;
        double top = y - Fonts.ascent(font);
        if (state.baseline == VPos.TOP) {
            top = y;
        } else if (state.baseline == VPos.CENTER) {
            top = y - height / 2;
        } else if (state.baseline == VPos.BOTTOM) {
            top = y - height;
        }
        for (int i = 0; i < lineCount; i++) {
            if (lines[i].length() == 0) {
                continue;
            }
            double w = Fonts.width(font, lines[i]);
            double left = x;
            if (state.align == TextAlignment.CENTER) {
                left = x - w / 2;
            } else if (state.align == TextAlignment.RIGHT) {
                left = x - w;
            }
            Op o = op(TEXT, state.matrix);
            o.text = lines[i];
            o.font = font;
            o.paint = paint;
            o.x = left;
            o.y = top + i * lineHeight;
            o.bounds = Matrix2D.bounds(state.matrix, o.x, o.y, w, lineHeight);
            record(o);
        }
    }

    /// Draws text with the fill paint at a point; see
    /// [#setTextAlign(TextAlignment)] and [#setTextBaseline(VPos)] for
    /// what the point is. Line feeds start new lines.
    public void fillText(String text, double x, double y) {
        text(text, x, y, 0, false, state.fill);
    }

    /// Draws text with the stroke paint at a point. The text is filled
    /// rather than outlined; see the class description.
    public void strokeText(String text, double x, double y) {
        text(text, x, y, 0, false, state.stroke);
    }

    /// Draws text with the fill paint, made smaller when it is wider
    /// than a maximum width. Nothing is drawn for a maximum that is not
    /// positive.
    public void fillText(String text, double x, double y, double maxWidth) {
        text(text, x, y, maxWidth, true, state.fill);
    }

    /// Draws text with the stroke paint, made smaller when it is wider
    /// than a maximum width. The text is filled rather than outlined.
    public void strokeText(String text, double x, double y, double maxWidth) {
        text(text, x, y, maxWidth, true, state.stroke);
    }

    // -------------------------------------------------------------- path

    /// Adds a path given in the current coordinates to the context's
    /// path, which is kept in the canvas's coordinates.
    private void addToPath(FxPath user, boolean connect) {
        FxPath t = user.transformed(state.matrix);
        double[] pts = t.points();
        int p = 0;
        for (int i = 0; i < t.commandCount(); i++) {
            switch (t.command(i)) {
                case FxPath.MOVE:
                    if (i == 0 && connect && !path.isEmpty()) {
                        path.lineTo(pts[p], pts[p + 1]);
                    } else {
                        path.moveTo(pts[p], pts[p + 1]);
                    }
                    p += 2;
                    break;
                case FxPath.LINE:
                    path.lineTo(pts[p], pts[p + 1]);
                    p += 2;
                    break;
                case FxPath.QUAD:
                    path.quadTo(pts[p], pts[p + 1], pts[p + 2], pts[p + 3]);
                    p += 4;
                    break;
                case FxPath.CUBIC:
                    path.curveTo(pts[p], pts[p + 1], pts[p + 2], pts[p + 3], pts[p + 4], pts[p + 5]);
                    p += 6;
                    break;
                default:
                    path.closePath();
                    break;
            }
        }
    }

    private double tx(double x, double y) {
        return Matrix2D.x(state.matrix, x, y);
    }

    private double ty(double x, double y) {
        return Matrix2D.y(state.matrix, x, y);
    }

    /// Empties the path.
    public void beginPath() {
        path.reset();
    }

    /// Starts a new part of the path at a point.
    public void moveTo(double x0, double y0) {
        path.moveTo(tx(x0, y0), ty(x0, y0));
    }

    /// Adds a line to a point; on an empty path it only moves there.
    public void lineTo(double x1, double y1) {
        path.lineTo(tx(x1, y1), ty(x1, y1));
    }

    /// Adds a quadratic curve to a point.
    public void quadraticCurveTo(double xc, double yc, double x1, double y1) {
        path.quadTo(tx(xc, yc), ty(xc, yc), tx(x1, y1), ty(x1, y1));
    }

    /// Adds a cubic curve to a point.
    public void bezierCurveTo(double xc1, double yc1, double xc2, double yc2, double x1, double y1) {
        path.curveTo(tx(xc1, yc1), ty(xc1, yc1), tx(xc2, yc2), ty(xc2, yc2), tx(x1, y1), ty(x1, y1));
    }

    /// Rounds the corner at `(x1, y1)` on the way to `(x2, y2)`: adds a
    /// line from the current point towards the corner and an arc of a
    /// radius that touches both that line and the one from the corner to
    /// the second point. On an empty path it moves to the corner; a zero
    /// radius or three points on one line give a line to the corner.
    public void arcTo(double x1, double y1, double x2, double y2, double radius) {
        if (path.isEmpty()) {
            moveTo(x1, y1);
            return;
        }
        double[] inverse = new double[6];
        if (!(radius > 0) || !Matrix2D.invert(state.matrix, inverse)) {
            lineTo(x1, y1);
            return;
        }
        double x0 = Matrix2D.x(inverse, path.currentX(), path.currentY());
        double y0 = Matrix2D.y(inverse, path.currentX(), path.currentY());
        double ax = x0 - x1;
        double ay = y0 - y1;
        double bx = x2 - x1;
        double by = y2 - y1;
        double la = Math.sqrt(ax * ax + ay * ay);
        double lb = Math.sqrt(bx * bx + by * by);
        if (!(la > 0) || !(lb > 0)) {
            lineTo(x1, y1);
            return;
        }
        ax /= la;
        ay /= la;
        bx /= lb;
        by /= lb;
        double cross = ax * by - ay * bx;
        double dot = ax * bx + ay * by;
        if (Math.abs(cross) < 1e-9) {
            lineTo(x1, y1);
            return;
        }
        // The distance from the corner to where the circle touches: the
        // radius over the tangent of half the angle between the lines.
        double halfTan = Math.sqrt((1 - dot) / (1 + dot));
        double d = radius / halfTan;
        FxPath arc = new FxPath();
        arc.moveTo(x1 + ax * d, y1 + ay * d);
        arc.arcTo(radius, radius, 0, false, cross < 0, x1 + bx * d, y1 + by * d);
        addToPath(arc, true);
    }

    /// Adds part of an ellipse, joined to the current point by a line.
    /// Angles are in degrees, counter clockwise from the positive x
    /// axis.
    public void arc(double centerX, double centerY, double radiusX, double radiusY, double startAngle,
            double length) {
        FxPath arc = new FxPath();
        arc.addArc(centerX, centerY, radiusX, radiusY, startAngle, length, false);
        addToPath(arc, true);
    }

    /// Adds a closed rectangle as a part of its own.
    public void rect(double x, double y, double w, double h) {
        FxPath r = new FxPath();
        r.addRect(x, y, w, h);
        addToPath(r, false);
    }

    /// Adds SVG path data. On an empty path it has to start with a move;
    /// otherwise it may continue from the current point. Data that stops
    /// making sense adds what came before the error.
    public void appendSVGPath(String svgpath) {
        if (svgpath == null) {
            return;
        }
        FxPath user = new FxPath();
        double[] inverse = new double[6];
        boolean continues = !path.isEmpty() && Matrix2D.invert(state.matrix, inverse);
        if (continues) {
            user.moveTo(Matrix2D.x(inverse, path.currentX(), path.currentY()),
                    Matrix2D.y(inverse, path.currentX(), path.currentY()));
        }
        SvgPath.append(svgpath, user);
        addToPath(user, continues);
    }

    /// Closes the current part of the path.
    public void closePath() {
        path.closePath();
    }

    /// Fills the path with the fill paint.
    public void fill() {
        FxPath copy = path.copy();
        copy.setEvenOdd(state.rule == FillRule.EVEN_ODD);
        recordFill(copy, Matrix2D.identity());
    }

    /// Strokes the path with the stroke paint. The line width is scaled
    /// by the current transform.
    public void stroke() {
        recordStroke(path.copy(), Matrix2D.identity(), state.lineWidth * Matrix2D.uniformScale(state.matrix));
    }

    /// Restricts what is drawn from now on to the inside of the path, in
    /// addition to the clips already in force, until the matching
    /// [#restore()]. See the class description for how exact that is.
    public void clip() {
        FxPath copy = path.copy();
        copy.setEvenOdd(state.rule == FillRule.EVEN_ODD);
        FxPath[] clips = new FxPath[state.clips.length + 1];
        System.arraycopy(state.clips, 0, clips, 0, state.clips.length);
        clips[state.clips.length] = copy;
        state.clips = clips;
    }

    /// Returns whether a point, in the canvas's own coordinates, is
    /// inside the path.
    public boolean isPointInPath(double x, double y) {
        FxPath copy = path.copy();
        copy.setEvenOdd(state.rule == FillRule.EVEN_ODD);
        return copy.contains(x, y);
    }

    // ------------------------------------------------------------ shapes

    private static boolean inside(double[] inner, double[] outer) {
        return inner[0] >= outer[0] - 1e-9 && inner[1] >= outer[1] - 1e-9 && inner[2] <= outer[2] + 1e-9
                && inner[3] <= outer[3] + 1e-9;
    }

    private static boolean apart(double[] a, double[] b) {
        return a[2] <= b[0] || b[2] <= a[0] || a[3] <= b[1] || b[3] <= a[1];
    }

    /// Makes a rectangle transparent again. See the class description
    /// for what that does to the recorded calls. A clip in force limits
    /// what is cleared.
    public void clearRect(double x, double y, double w, double h) {
        if (Math.abs(w) <= 0 || Math.abs(h) <= 0) {
            return;
        }
        double[] m = state.matrix;
        boolean upright = Math.abs(m[1]) <= 0 && Math.abs(m[2]) <= 0;
        boolean clipped = state.clips.length > 0;
        double[] cleared = Matrix2D.bounds(m, x, y, w, h);
        double[] whole = {0, 0, Math.max(0, canvas.getWidth()), Math.max(0, canvas.getHeight())};
        if (upright && !clipped && inside(whole, cleared)) {
            ops.clear();
            covers = 0;
            canvas.cn1Repaint();
            return;
        }
        boolean partly = false;
        for (int i = ops.size() - 1; i >= 0; i--) {
            double[] b = ops.get(i).bounds;
            if (apart(b, cleared)) {
                continue;
            }
            if (upright && !clipped && inside(b, cleared)) {
                if (ops.remove(i).cover) {
                    covers--;
                }
            } else {
                partly = true;
            }
        }
        if (partly) {
            Op backing = rasterise();
            if (backing != null) {
                FxPath quad = new FxPath();
                quad.addRect(x, y, w, h);
                punch(backing, quad.transformed(m), upright && !clipped ? cleared : null);
            }
        }
        canvas.cn1Repaint();
    }

    /// Makes the pixels of the backing image inside a shape, and inside
    /// every clip in force, transparent. `box` is the shape when it is
    /// an upright rectangle with no clip, which needs no test per pixel.
    private void punch(Op backing, FxPath shape, double[] box) {
        com.codename1.ui.Image image = backing.image;
        int iw = image.getWidth();
        int ih = image.getHeight();
        if (iw <= 0 || ih <= 0 || !(backing.w > 0) || !(backing.h > 0)) {
            return;
        }
        double sx = iw / backing.w;
        double sy = ih / backing.h;
        double[] toPixels = {sx, 0, 0, sy, 0, 0};
        FxPath device = shape.transformed(toPixels).flatten(0.25);
        double[] b = device.bounds();
        if (b == null) {
            return;
        }
        int x1 = (int) Math.max(0, box != null ? Math.round(b[0]) : Math.floor(b[0]));
        int y1 = (int) Math.max(0, box != null ? Math.round(b[1]) : Math.floor(b[1]));
        int x2 = (int) Math.min(iw, box != null ? Math.round(b[2]) : Math.ceil(b[2]));
        int y2 = (int) Math.min(ih, box != null ? Math.round(b[3]) : Math.ceil(b[3]));
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        FxPath[] clips = new FxPath[state.clips.length];
        for (int i = 0; i < clips.length; i++) {
            clips[i] = state.clips[i].transformed(toPixels).flatten(0.25);
        }
        int[] rgb = image.getRGB();
        if (rgb == null || rgb.length < iw * ih) {
            return;
        }
        for (int py = y1; py < y2; py++) {
            for (int px = x1; px < x2; px++) {
                boolean clear = box != null || device.contains(px + 0.5, py + 0.5);
                for (int c = 0; clear && c < clips.length; c++) {
                    clear = clips[c].contains(px + 0.5, py + 0.5);
                }
                if (clear) {
                    rgb[py * iw + px] = 0;
                }
            }
        }
        backing.image = com.codename1.ui.Image.createImage(rgb, iw, ih);
        backing.reusable = false;
    }

    /// Fills a rectangle.
    public void fillRect(double x, double y, double w, double h) {
        FxPath p = new FxPath();
        p.addRect(x, y, w, h);
        fillShape(p);
    }

    /// Strokes the outline of a rectangle.
    public void strokeRect(double x, double y, double w, double h) {
        FxPath p = new FxPath();
        p.addRect(x, y, w, h);
        strokeShape(p);
    }

    /// Fills the ellipse inside a rectangle.
    public void fillOval(double x, double y, double w, double h) {
        FxPath p = new FxPath();
        p.addEllipse(x + w / 2, y + h / 2, w / 2, h / 2);
        fillShape(p);
    }

    /// Strokes the ellipse inside a rectangle.
    public void strokeOval(double x, double y, double w, double h) {
        FxPath p = new FxPath();
        p.addEllipse(x + w / 2, y + h / 2, w / 2, h / 2);
        strokeShape(p);
    }

    private static FxPath arcPath(double x, double y, double w, double h, double startAngle, double arcExtent,
            ArcType closure) {
        FxPath p = new FxPath();
        double cx = x + w / 2;
        double cy = y + h / 2;
        if (closure == ArcType.ROUND) {
            p.moveTo(cx, cy);
        }
        p.addArc(cx, cy, w / 2, h / 2, startAngle, arcExtent, closure == ArcType.ROUND);
        if (closure == ArcType.ROUND || closure == ArcType.CHORD) {
            p.closePath();
        }
        return p;
    }

    /// Fills part of the ellipse inside a rectangle. Angles are in
    /// degrees, counter clockwise from the positive x axis; the closure
    /// says how the ends are joined.
    public void fillArc(double x, double y, double w, double h, double startAngle, double arcExtent,
            ArcType closure) {
        if (closure != null) {
            fillShape(arcPath(x, y, w, h, startAngle, arcExtent, closure));
        }
    }

    /// Strokes part of the ellipse inside a rectangle.
    public void strokeArc(double x, double y, double w, double h, double startAngle, double arcExtent,
            ArcType closure) {
        if (closure != null) {
            strokeShape(arcPath(x, y, w, h, startAngle, arcExtent, closure));
        }
    }

    /// Fills a rectangle with rounded corners; the arc sizes are the
    /// diameters of the corners.
    public void fillRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight) {
        FxPath p = new FxPath();
        p.addRoundRect(x, y, w, h, arcWidth / 2, arcHeight / 2);
        fillShape(p);
    }

    /// Strokes the outline of a rectangle with rounded corners.
    public void strokeRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight) {
        FxPath p = new FxPath();
        p.addRoundRect(x, y, w, h, arcWidth / 2, arcHeight / 2);
        strokeShape(p);
    }

    /// Strokes a line between two points.
    public void strokeLine(double x1, double y1, double x2, double y2) {
        FxPath p = new FxPath();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        strokeShape(p);
    }

    private static FxPath polygon(double[] xPoints, double[] yPoints, int nPoints, boolean close) {
        FxPath p = new FxPath();
        if (xPoints == null || yPoints == null) {
            return p;
        }
        int n = Math.min(nPoints, Math.min(xPoints.length, yPoints.length));
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                p.moveTo(xPoints[i], yPoints[i]);
            } else {
                p.lineTo(xPoints[i], yPoints[i]);
            }
        }
        if (close && n > 0) {
            p.closePath();
        }
        return p;
    }

    /// Fills the polygon through points, using the fill rule.
    public void fillPolygon(double[] xPoints, double[] yPoints, int nPoints) {
        if (nPoints >= 3) {
            fillShape(polygon(xPoints, yPoints, nPoints, true));
        }
    }

    /// Strokes the closed outline through points.
    public void strokePolygon(double[] xPoints, double[] yPoints, int nPoints) {
        if (nPoints >= 2) {
            strokeShape(polygon(xPoints, yPoints, nPoints, true));
        }
    }

    /// Strokes the open outline through points.
    public void strokePolyline(double[] xPoints, double[] yPoints, int nPoints) {
        if (nPoints >= 2) {
            strokeShape(polygon(xPoints, yPoints, nPoints, false));
        }
    }

    // ------------------------------------------------------------ images

    private void image(com.codename1.ui.Image picture, double x, double y, double w, double h) {
        if (picture == null) {
            return;
        }
        // A negative size mirrors, as it does in JavaFX.
        double[] m = state.matrix;
        double dx = x;
        double dy = y;
        double dw = w;
        double dh = h;
        if (dw < 0 || dh < 0) {
            m = Matrix2D.multiply(m, new double[] {dw < 0 ? -1 : 1, 0, 0, dh < 0 ? -1 : 1, dw < 0 ? 2 * x + w : 0,
                dh < 0 ? 2 * y + h : 0});
            if (dw < 0) {
                dx = x + w;
                dw = -w;
            }
            if (dh < 0) {
                dy = y + h;
                dh = -h;
            }
        }
        if (!(dw > 0) || !(dh > 0)) {
            return;
        }
        Op o = op(IMAGE, m);
        o.image = picture;
        o.x = dx;
        o.y = dy;
        o.w = dw;
        o.h = dh;
        o.bounds = Matrix2D.bounds(m, dx, dy, dw, dh);
        // An image over the whole canvas hides most of what is under it,
        // and an application that shows a picture it keeps computing
        // draws one every frame without clearing. A list of them is a
        // frame of pixels kept alive per call, thousands of them before
        // the limit on calls is reached, and no collector can give back
        // what is still referenced. The second one folds the list into
        // the canvas image.
        double[] whole = {0, 0, Math.max(0, canvas.getWidth()), Math.max(0, canvas.getHeight())};
        o.cover = state.clips.length == 0 && inside(whole, o.bounds);
        if (o.cover) {
            covers++;
        }
        record(o);
        if (covers > 1) {
            rasterise();
        }
    }

    /// Draws an image at its own size with its top left corner at a
    /// point.
    public void drawImage(Image img, double x, double y) {
        if (img != null) {
            image(img.cn1Native(), x, y, img.getWidth(), img.getHeight());
        }
    }

    /// Draws an image into a rectangle.
    public void drawImage(Image img, double x, double y, double w, double h) {
        if (img != null) {
            image(img.cn1Native(), x, y, w, h);
        }
    }

    /// Draws a rectangle of an image, given in its pixels, into a
    /// rectangle of the canvas. The part of the source rectangle that
    /// lies outside the image is not drawn, unless it is less than a
    /// pixel of it.
    public void drawImage(Image img, double sx, double sy, double sw, double sh, double dx, double dy, double dw,
            double dh) {
        com.codename1.ui.Image full = img == null ? null : img.cn1Native();
        if (full == null || !(sw > 0) || !(sh > 0)) {
            return;
        }
        int x1 = (int) Math.max(0, Math.round(sx));
        int y1 = (int) Math.max(0, Math.round(sy));
        int x2 = (int) Math.min(full.getWidth(), Math.round(sx + sw));
        int y2 = (int) Math.min(full.getHeight(), Math.round(sy + sh));
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        // The destination shrinks with the part of the source that was
        // cut off -- but not for a part of less than one pixel. JavaFX
        // reads past the edge of an image by repeating its last pixel,
        // so a source rectangle a fraction wider than the image, which
        // is what `(int) width` of a size that is not whole leaves,
        // still fills its whole destination. Shrinking it left a line
        // of the canvas undrawn between this image and whatever the
        // application drew beside it.
        double kx = dw / sw;
        double ky = dh / sh;
        double left = within(x1 - sx);
        double top = within(y1 - sy);
        double right = within(sx + sw - x2);
        double bottom = within(sy + sh - y2);
        // The whole picture is the picture: cutting it out of itself made
        // a second image of the platform for every call.
        boolean all = x1 == 0 && y1 == 0 && x2 == full.getWidth() && y2 == full.getHeight();
        image(all ? full : full.subImage(x1, y1, x2 - x1, y2 - y1, true), dx + left * kx, dy + top * ky,
                dw - (left + right) * kx, dh - (top + bottom) * ky);
    }

    /// What of a source rectangle is cut off at one side, for the
    /// destination: nothing when it is less than a pixel, which the last
    /// pixel of the image covers, and nothing for a negative amount, a
    /// side that rounding moved outwards.
    private static double within(double cut) {
        return cut < 1 ? 0 : cut;
    }
}
