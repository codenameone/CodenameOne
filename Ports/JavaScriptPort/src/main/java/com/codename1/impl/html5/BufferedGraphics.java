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

import com.codename1.impl.html5.HTML5Implementation.NativeFont;
import com.codename1.impl.html5.HTML5Implementation.NativeImage;
import com.codename1.impl.html5.graphics.ClearRect;
import com.codename1.impl.html5.graphics.ClipRect;
import com.codename1.impl.html5.graphics.ClipShape;
import com.codename1.impl.html5.graphics.DrawArc;
import com.codename1.impl.html5.graphics.DrawImage;
import com.codename1.impl.html5.graphics.DrawLine;
import com.codename1.impl.html5.graphics.DrawPolygon;
import com.codename1.impl.html5.graphics.DrawRect;
import com.codename1.impl.html5.graphics.DrawRoundRect;
import com.codename1.impl.html5.graphics.DrawShape;
import com.codename1.impl.html5.graphics.DrawString;
import com.codename1.impl.html5.graphics.ExecutableOp;
import com.codename1.impl.html5.graphics.FillArc;
import com.codename1.impl.html5.graphics.FillLinearGradient;
import com.codename1.impl.html5.graphics.FillPolygon;
import com.codename1.impl.html5.graphics.FillRadialGradient;
import com.codename1.impl.html5.graphics.FillRect;
import com.codename1.impl.html5.graphics.FillRoundRect;
import com.codename1.impl.html5.graphics.FillShape;
import com.codename1.impl.html5.graphics.SetTransform;
import com.codename1.impl.html5.graphics.TileImage;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import java.util.ArrayList;
import java.util.List;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.dom.HTMLCanvasElement;

/**
 *
 * @author shannah
 */
public class BufferedGraphics extends HTML5Graphics {
    ArrayList<ExecutableOp> upcoming = new ArrayList<ExecutableOp>();
    private Rectangle clipRect;
    private Rectangle clip=new Rectangle();
    private Rectangle clipBounds=new Rectangle();
    private boolean clipBoundsDirty=true;
    private GeneralPath clipShape = new GeneralPath();

    private boolean isClipShape;
    private boolean promotionSuspended;
    // True when the current clip encloses no area. Tracked reliably via
    // clipBoundsTracker (a clamped user-space rect intersection) because the
    // projected clip bounds are unreliable for an empty clip on the shape path
    // (an empty GeneralPath's getBounds() returns uninitialised extents, and a
    // degenerate clip path does not cull image blits/fills on the host). This is
    // the DISPLAY/screen graphics class (BufferedGraphics extends HTML5Graphics
    // and overrides the clip/draw methods); the same fix lives in both. #5263.
    private boolean clipEmpty;
    private final Rectangle clipBoundsTracker = new Rectangle();
    private Transform transform, clipTransform;
    private boolean transformApplied=false;
    private final JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp> primitiveRenderAdapter =
            new JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp>(getRenderState(),
                    new JavaScriptPrimitiveRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            addOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp> imageTransformRenderAdapter =
            new JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp>(getRenderState(),
                    new JavaScriptImageTransformRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            addOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp> shapeGradientRenderAdapter =
            new JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp>(getRenderState(),
                    new JavaScriptShapeGradientRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            addOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    
    public BufferedGraphics(HTML5Implementation impl, int width, int height) {
        // The display draws onto the well-known display surface; the host binds
        // it lazily to the output canvas. No worker-side canvas/context proxy.
        super(impl, HTML5Implementation.DISPLAY_SURFACE_ID, width, height);
        clipBoundsTracker.setBounds(0, 0, width, height);
    }

    // Single chokepoint for buffered ops. When the clip is empty, drop every
    // DRAW op -- nothing may render -- but still record clip and transform ops so
    // a later (non-empty) clip restores drawing. This is the reliable cull: a
    // degenerate empty-clip path does not cull fills or image blits on the host
    // canvas, so we must not emit the draws at all. Mirrors the GL clipBlock
    // mechanism. Issue #5263.
    private void addOp(ExecutableOp operation) {
        if (clipEmpty
                && !(operation instanceof ClipRect)
                && !(operation instanceof ClipShape)
                && !(operation instanceof SetTransform)) {
            return;
        }
        upcoming.add(operation);
    }

    /**
     * Image draws report the rectangle they land in.
     *
     * <p>Review asked for the source alpha to be taken into account, so that an image which is
     * transparent where the glyphs are does not send them back to the canvas. There is no way to
     * ask that question here without reading the image's pixels, and pixel reads are what this
     * port cannot do: every one is a round trip that parks the worker on a main-thread answer,
     * and CI has a lint whose whole purpose is to keep them out of the drawing path -- per draw,
     * per frame, it would be ruinous.</p>
     *
     * <p>So the rectangle stands, and the error it can make is the cheaper of the two. Reporting
     * a draw that turned out to be transparent costs that component its promotion: its text is
     * drawn on the canvas instead of the DOM, still in the right place, still saying the same
     * thing, no longer selectable. Not reporting one that turned out to be opaque leaves text
     * floating above an image that should have hidden it -- a frame showing something the
     * application did not draw.</p>
     */
    @Override
    public void drawImage(Object img, int x, int y) {
        // An empty clip must cull every draw; a degenerate empty-clip path on
        // the host leaks image blits, so cull here. Issue #5263.
        if (clipEmpty) { return; }
        NativeImage image = (NativeImage) img;
        noteCanvasCover(x, y, image == null ? 0 : image.getWidth(), image == null ? 0 : image.getHeight());
        imageTransformRenderAdapter.drawImage(image, x, y);
    }

    @Override
    public void drawImage(Object img, int x, int y, int w, int h) {
        if (clipEmpty) { return; }
        noteCanvasCover(x, y, w, h);
        imageTransformRenderAdapter.drawImage((NativeImage)img, x, y, w, h);
    }

    /**
     * Reports the bounds of a filled shape as covering, so promoted text underneath goes back to
     * the canvas where the shape can actually paint over it.
     */
    private void noteCanvasCover(final Shape shape) {
        if (shape == null) {
            return;
        }
        com.codename1.ui.geom.Rectangle bounds = shape.getBounds();
        if (bounds == null) {
            return;
        }
        if (shape.isRectangle()) {
            noteCanvasCover(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
            return;
        }
        // A shape reaches what it encloses, not what its bounding rectangle encloses: a filled
        // triangle drawn around a label reaches none of it, while a triangle over one corner of
        // that label reaches part of it -- and any part is enough, because the canvas would have
        // painted over that part and the DOM run cannot be painted over at all. So the question
        // asked of the shape is whether it meets the text anywhere.
        final float[][] outline = outlineOf(shape);
        final int winding = windingOf(shape);
        noteCanvasCover(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(),
                new JavaScriptTextLayer.CoverTest() {
                    @Override
                    public boolean covers(int x, int y, int w, int h) {
                        return outlineMeetsRect(outline, winding, x, y, w, h);
                    }
                });
    }

    /**
     * Reports a stroked draw. A stroke paints along its line and nowhere else, so what it covers
     * is where that line runs, widened by the stroke it is drawn with.
     */
    private void noteStrokeCover(final float[][] outline, int strokeWidth) {
        if (outline == null || outline.length == 0) {
            return;
        }
        final int reach = Math.max(1, strokeWidth) / 2 + 1;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int p = 0; p + 1 < outline.length; p += 2) {
            for (int i = 0; i < outline[p].length; i++) {
                minX = Math.min(minX, outline[p][i]);
                maxX = Math.max(maxX, outline[p][i]);
                minY = Math.min(minY, outline[p + 1][i]);
                maxY = Math.max(maxY, outline[p + 1][i]);
            }
        }
        if (maxX < minX || maxY < minY) {
            return;
        }
        noteCanvasCover((int) Math.floor(minX) - reach, (int) Math.floor(minY) - reach,
                (int) Math.ceil(maxX - minX) + 2 * reach, (int) Math.ceil(maxY - minY) + 2 * reach,
                new JavaScriptTextLayer.CoverTest() {
                    @Override
                    public boolean covers(int x, int y, int w, int h) {
                        return outlineMeetsRect(outline,
                                com.codename1.ui.geom.PathIterator.WIND_NON_ZERO, false,
                                x - reach, y - reach, w + 2 * reach, h + 2 * reach);
                    }
                });
    }

    private static float[][] segmentOutline(int x1, int y1, int x2, int y2) {
        return new float[][] { new float[] { x1, x2 }, new float[] { y1, y2 } };
    }

    private static float[][] rectOutline(int x, int y, int width, int height) {
        return new float[][] {
            new float[] { x, x + width, x + width, x },
            new float[] { y, y, y + height, y + height }
        };
    }

    /**
     * A coverage test for a rounded rectangle, whose corners are cut away.
     */
    private static JavaScriptTextLayer.CoverTest roundRectCoverTest(int x, int y, int width,
            int height, int arcWidth, int arcHeight) {
        final float[][] outline = roundRectOutline(x, y, width, height, arcWidth, arcHeight);
        if (outline == null) {
            return null;
        }
        return new JavaScriptTextLayer.CoverTest() {
            @Override
            public boolean covers(int rectX, int rectY, int rectW, int rectH) {
                return outlineMeetsRect(outline, rectX, rectY, rectW, rectH);
            }
        };
    }

    /**
     * The perimeter of a rounded rectangle, traced once round, clockwise on screen, each corner a
     * quarter ellipse that ends where the next one begins: top-right from its top point to its
     * right point, down the right side, and so on. The straight sides are the lines between one
     * corner's last point and the next corner's first. Walking the corners in any other order
     * joins points that are not neighbours and folds the outline across itself.
     *
     * @return the outline, or null when there is no rounding to speak of
     */
    private static float[][] roundRectOutline(int x, int y, int width, int height,
            int arcWidth, int arcHeight) {
        // The renderer rounds both axes by max(arcWidth, arcHeight) -- DrawRoundRect and
        // FillRoundRect both do -- so the outline has to be built from the same radius, or it
        // would describe a corner the draw does not have and miss text the draw reaches.
        int radius = Math.max(0, Math.max(arcWidth, arcHeight)) / 2;
        int rx = Math.min(radius, width / 2);
        int ry = Math.min(radius, height / 2);
        if (rx <= 0 || ry <= 0) {
            return null;
        }
        int perCorner = 6;
        int points = 4 * (perCorner + 1);
        float[][] outline = new float[][] { new float[points], new float[points] };
        int at = 0;
        int[][] corners = new int[][] {
            { x + width - rx, y + ry, 90 },
            { x + width - rx, y + height - ry, 0 },
            { x + rx, y + height - ry, -90 },
            { x + rx, y + ry, -180 }
        };
        for (int c = 0; c < corners.length; c++) {
            double cx = corners[c][0];
            double cy = corners[c][1];
            double from = corners[c][2];
            for (int i = 0; i <= perCorner; i++) {
                // Angles run counter-clockwise from three o'clock while y grows downwards, so
                // sweeping the angle down walks the perimeter clockwise on screen.
                double radians = Math.toRadians(from - (90.0 * i) / perCorner);
                outline[0][at] = (float) (cx + rx * Math.cos(radians));
                outline[1][at] = (float) (cy - ry * Math.sin(radians));
                at++;
            }
        }
        return outline;
    }

    private static float[][] polygonOutline(int[] xPoints, int[] yPoints, int nPoints) {
        if (xPoints == null || yPoints == null || nPoints <= 0) {
            return null;
        }
        int count = Math.min(nPoints, Math.min(xPoints.length, yPoints.length));
        float[][] outline = new float[][] { new float[count], new float[count] };
        for (int i = 0; i < count; i++) {
            outline[0][i] = xPoints[i];
            outline[1][i] = yPoints[i];
        }
        return outline;
    }

    /**
     * A coverage test for the sector a filled arc actually paints.
     *
     * <p>The bounding rectangle of an arc holds a good deal the arc never reaches -- the corners
     * of a full ellipse's box, and everything outside the wedge of a partial one. The sector is
     * traced as a closed outline and asked the same question as any other shape.</p>
     */
    private static JavaScriptTextLayer.CoverTest arcCoverTest(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        final float[][] outline = arcOutline(x, y, width, height, startAngle, arcAngle, true);
        if (outline == null) {
            return null;
        }
        return new JavaScriptTextLayer.CoverTest() {
            @Override
            public boolean covers(int rectX, int rectY, int rectW, int rectH) {
                return outlineMeetsRect(outline, rectX, rectY, rectW, rectH);
            }
        };
    }

    /**
     * The outline of an arc: the sector a fill paints, or just the curve a stroke follows.
     *
     * <p>The bounding rectangle of an arc holds a good deal the arc never reaches -- the corners
     * of a full ellipse's box, and everything outside the wedge of a partial one.</p>
     *
     * @param wedge true to close the sector through its centre, as a fill does
     * @return the outline, or null for a degenerate arc
     */
    private static float[][] arcOutline(int x, int y, int width, int height,
            int startAngle, int arcAngle, boolean wedge) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        double cx = x + width / 2.0;
        double cy = y + height / 2.0;
        double rx = width / 2.0;
        double ry = height / 2.0;
        int span = Math.abs(arcAngle) >= 360 ? 360 : Math.abs(arcAngle);
        int steps = Math.max(8, span / 4);
        boolean whole = span >= 360;
        boolean centre = wedge && !whole;
        int points = steps + 1 + (centre ? 1 : 0);
        float[][] outline = new float[][] { new float[points], new float[points] };
        int at = 0;
        if (centre) {
            // The straight edges of a wedge run from the centre out to each end of the arc.
            outline[0][at] = (float) cx;
            outline[1][at] = (float) cy;
            at++;
        }
        double from = arcAngle < 0 ? startAngle + arcAngle : startAngle;
        for (int i = 0; i <= steps; i++) {
            double degrees = from + (span * (double) i) / steps;
            double radians = Math.toRadians(degrees);
            // Angles run counter-clockwise from three o'clock, while y grows downwards.
            outline[0][at] = (float) (cx + rx * Math.cos(radians));
            outline[1][at] = (float) (cy - ry * Math.sin(radians));
            at++;
        }
        return outline;
    }

    /**
     * Flattens a shape's outline into one array of x coordinates and one of y coordinates per
     * subpath, so it can be asked whether it meets a rectangle.
     *
     * <p>Curves are taken through their control points. That traces slightly wide of the curve
     * itself, which errs towards saying the shape meets the text -- the same side the canvas errs
     * on, since text it may have painted over must not stay above it.</p>
     */
    private static float[][] outlineOf(Shape shape) {
        java.util.List<float[]> xs = new java.util.ArrayList<float[]>();
        java.util.List<float[]> ys = new java.util.ArrayList<float[]>();
        java.util.List<Float> curX = new java.util.ArrayList<Float>();
        java.util.List<Float> curY = new java.util.ArrayList<Float>();
        float[] coords = new float[6];
        com.codename1.ui.geom.PathIterator it = shape.getPathIterator();
        while (it != null && !it.isDone()) {
            int type = it.currentSegment(coords);
            if (type == com.codename1.ui.geom.PathIterator.SEG_MOVETO) {
                if (curX.size() > 1) {
                    xs.add(toArray(curX));
                    ys.add(toArray(curY));
                }
                curX.clear();
                curY.clear();
                curX.add(Float.valueOf(coords[0]));
                curY.add(Float.valueOf(coords[1]));
            } else if (type == com.codename1.ui.geom.PathIterator.SEG_LINETO) {
                curX.add(Float.valueOf(coords[0]));
                curY.add(Float.valueOf(coords[1]));
            } else if (type == com.codename1.ui.geom.PathIterator.SEG_QUADTO) {
                curX.add(Float.valueOf(coords[0]));
                curY.add(Float.valueOf(coords[1]));
                curX.add(Float.valueOf(coords[2]));
                curY.add(Float.valueOf(coords[3]));
            } else if (type == com.codename1.ui.geom.PathIterator.SEG_CUBICTO) {
                for (int i = 0; i < 6; i += 2) {
                    curX.add(Float.valueOf(coords[i]));
                    curY.add(Float.valueOf(coords[i + 1]));
                }
            }
            it.next();
        }
        if (curX.size() > 1) {
            xs.add(toArray(curX));
            ys.add(toArray(curY));
        }
        float[][] out = new float[xs.size() * 2][];
        for (int i = 0; i < xs.size(); i++) {
            out[i * 2] = xs.get(i);
            out[i * 2 + 1] = ys.get(i);
        }
        return out;
    }

    /**
     * The rule a shape's fill uses to decide what is inside it, which is what says whether a hole
     * is a hole.
     */
    private static int windingOf(Shape shape) {
        try {
            com.codename1.ui.geom.PathIterator it = shape.getPathIterator();
            if (it != null) {
                return it.getWindingRule();
            }
        } catch (Throwable ignored) {
            // Treated as non-zero below, which is what a plain outline fills as.
        }
        return com.codename1.ui.geom.PathIterator.WIND_NON_ZERO;
    }

    private static float[] toArray(java.util.List<Float> values) {
        float[] out = new float[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i).floatValue();
        }
        return out;
    }

    /**
     * Whether a flattened outline meets a rectangle at all -- a vertex inside it, a corner of it
     * inside the outline, or an edge crossing one of its sides.
     */
    private static boolean outlineMeetsRect(float[][] outline, int x, int y, int w, int h) {
        return outlineMeetsRect(outline, com.codename1.ui.geom.PathIterator.WIND_NON_ZERO, true,
                x, y, w, h);
    }

    private static boolean outlineMeetsRect(float[][] outline, int winding,
            int x, int y, int w, int h) {
        return outlineMeetsRect(outline, winding, true, x, y, w, h);
    }

    /**
     * @param filled true for a draw that paints what the outline encloses, false for one that
     * paints only along it -- a stroke, which leaves what it surrounds untouched
     */
    private static boolean outlineMeetsRect(float[][] outline, int winding, boolean filled,
            int x, int y, int w, int h) {
        if (outline == null || outline.length == 0) {
            // Nothing to go by, so treat the draw as reaching the text: leaving a run above a
            // draw that covered it is the error that shows on screen.
            return true;
        }
        float right = x + Math.max(0, w);
        float bottom = y + Math.max(0, h);
        for (int p = 0; p + 1 < outline.length; p += 2) {
            float[] px = outline[p];
            float[] py = outline[p + 1];
            for (int i = 0; i < px.length; i++) {
                if (px[i] >= x && px[i] <= right && py[i] >= y && py[i] <= bottom) {
                    return true;
                }
            }
            for (int i = 0, j = px.length - 1; i < px.length; j = i++) {
                if (segmentMeetsRect(px[j], py[j], px[i], py[i], x, y, right, bottom)) {
                    return true;
                }
            }
        }
        if (!filled) {
            // Nothing crosses the rectangle, and a stroke paints only where its line runs -- a
            // rectangle drawn around a label leaves the label alone.
            return false;
        }
        // No boundary passes through the rectangle, so the rectangle is either entirely painted
        // or entirely not. Which one is decided across every subpath at once, under the rule the
        // fill uses: a rectangle sitting in a hole is not painted, however far inside the outer
        // loop it lies.
        return pointInOutline(outline, winding, x, y)
                || pointInOutline(outline, winding, right, y)
                || pointInOutline(outline, winding, x, bottom)
                || pointInOutline(outline, winding, right, bottom);
    }

    private static boolean pointInOutline(float[][] outline, int winding, float x, float y) {
        int crossings = 0;
        for (int p = 0; p + 1 < outline.length; p += 2) {
            float[] px = outline[p];
            float[] py = outline[p + 1];
            for (int i = 0, j = px.length - 1; i < px.length; j = i++) {
                if ((py[i] > y) == (py[j] > y)) {
                    continue;
                }
                double crossing = (double) (px[j] - px[i]) * (y - py[i])
                        / (double) (py[j] - py[i]) + px[i];
                if (x >= crossing) {
                    continue;
                }
                if (winding == com.codename1.ui.geom.PathIterator.WIND_NON_ZERO) {
                    crossings += py[i] > py[j] ? 1 : -1;
                } else {
                    crossings++;
                }
            }
        }
        return winding == com.codename1.ui.geom.PathIterator.WIND_NON_ZERO
                ? crossings != 0
                : (crossings & 1) == 1;
    }

    private static boolean segmentMeetsRect(float x1, float y1, float x2, float y2,
            float left, float top, float right, float bottom) {
        return segmentsMeet(x1, y1, x2, y2, left, top, right, top)
                || segmentsMeet(x1, y1, x2, y2, right, top, right, bottom)
                || segmentsMeet(x1, y1, x2, y2, right, bottom, left, bottom)
                || segmentsMeet(x1, y1, x2, y2, left, bottom, left, top);
    }

    private static boolean segmentsMeet(float ax1, float ay1, float ax2, float ay2,
            float bx1, float by1, float bx2, float by2) {
        double d1 = side(bx1, by1, bx2, by2, ax1, ay1);
        double d2 = side(bx1, by1, bx2, by2, ax2, ay2);
        double d3 = side(ax1, ay1, ax2, ay2, bx1, by1);
        double d4 = side(ax1, ay1, ax2, ay2, bx2, by2);
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        return (d1 == 0 && between(bx1, by1, bx2, by2, ax1, ay1))
                || (d2 == 0 && between(bx1, by1, bx2, by2, ax2, ay2))
                || (d3 == 0 && between(ax1, ay1, ax2, ay2, bx1, by1))
                || (d4 == 0 && between(ax1, ay1, ax2, ay2, bx2, by2));
    }

    private static double side(float x1, float y1, float x2, float y2, float px, float py) {
        return (double) (x2 - x1) * (py - y1) - (double) (y2 - y1) * (px - x1);
    }

    private static boolean between(float x1, float y1, float x2, float y2, float px, float py) {
        return px >= Math.min(x1, x2) && px <= Math.max(x1, x2)
                && py >= Math.min(y1, y2) && py <= Math.max(y1, y2);
    }

    /**
     * Reports the bounds of a filled polygon as covering.
     */
    private void noteCanvasCover(final int[] xPoints, final int[] yPoints, final int nPoints) {
        if (xPoints == null || yPoints == null || nPoints <= 0) {
            return;
        }
        int minX = xPoints[0];
        int maxX = xPoints[0];
        int minY = yPoints[0];
        int maxY = yPoints[0];
        for (int i = 1; i < nPoints && i < xPoints.length && i < yPoints.length; i++) {
            minX = Math.min(minX, xPoints[i]);
            maxX = Math.max(maxX, xPoints[i]);
            minY = Math.min(minY, yPoints[i]);
            maxY = Math.max(maxY, yPoints[i]);
        }
        // Like a filled shape, a polygon reaches what it encloses rather than what its bounding
        // rectangle does -- and any part of the text it reaches is enough to send that text back
        // to the canvas.
        final float[][] outline = polygonOutline(xPoints, yPoints, nPoints);
        noteCanvasCover(minX, minY, maxX - minX, maxY - minY,
                new JavaScriptTextLayer.CoverTest() {
                    @Override
                    public boolean covers(int x, int y, int w, int h) {
                        return outlineMeetsRect(outline, x, y, w, h);
                    }
                });
    }

    /**
     * Tells the text layer that something landed on the canvas, so it can put back any text it
     * promoted this frame that the draw would have covered.
     *
     * <p>Text promoted into the DOM sits above the whole canvas. Anything drawn over it in the
     * original renderer would have hidden it -- an image, a fill, a shape -- and here it cannot,
     * so the promotion has to be given up rather than leave the frame showing something the
     * application did not draw.</p>
     */
    private void noteCanvasCover(int x, int y, int w, int h) {
        noteCanvasCover(x, y, w, h, null);
    }

    /**
     * Reports a region a draw rewrites without consulting the graphics alpha -- an erase, which
     * takes pixels away, or a backdrop effect, which samples what is there and paints the result.
     * Those count however transparent the graphics happens to be.
     */
    private void noteAlphaIndependentRegion(int x, int y, int w, int h,
            JavaScriptTextLayer.CoverTest test) {
        noteCanvasCover(x, y, w, h, test, true);
    }

    private void noteCanvasCover(int x, int y, int w, int h, JavaScriptTextLayer.CoverTest test) {
        noteCanvasCover(x, y, w, h, test, false);
    }

    private void noteCanvasCover(int x, int y, int w, int h, JavaScriptTextLayer.CoverTest test,
            boolean alphaIndependent) {
        JavaScriptTextLayer layer = impl == null ? null : impl.textLayer;
        if (layer == null || w <= 0 || h <= 0) {
            return;
        }
        // An empty clip culls the draw entirely -- addOp drops it -- so nothing is covered.
        if (clipEmpty) {
            return;
        }
        // A fully transparent draw leaves what is underneath exactly as it was, which is no
        // reason to take text off the layer. An erase and a backdrop effect are different: they
        // rewrite pixels without consulting the alpha, so they count either way.
        if (!alphaIndependent && getAlpha() <= 0) {
            return;
        }
        // Clipped first, in the coordinates the clip is kept in, and only then projected: the
        // tracker holds the clip in user space while a projected rectangle is in screen space,
        // and intersecting one with the other compares two different spaces.
        int clipLeft = clipBoundsTracker.getX();
        int clipTop = clipBoundsTracker.getY();
        int left = Math.max(x, clipLeft);
        int top = Math.max(y, clipTop);
        int right = Math.min(x + w, clipLeft + clipBoundsTracker.getWidth());
        int bottom = Math.min(y + h, clipTop + clipBoundsTracker.getHeight());
        if (right <= left || bottom <= top) {
            return;
        }
        x = left;
        y = top;
        w = right - left;
        h = bottom - top;
        if (transform == null || transform.isIdentity()) {
            layer.noteCanvasCover(x, y, w, h, test);
            return;
        }
        // Where the image lands, not where it was asked for: text is only promoted under an
        // identity transform, but a transform set afterwards moves everything drawn through it.
        // Comparing the untransformed rectangle would miss an image that ends up over the text
        // and leave the glyphs floating above it.
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float[] corner = new float[2];
        float[] out = new float[2];
        for (int i = 0; i < 4; i++) {
            corner[0] = (i == 0 || i == 3) ? x : x + w;
            corner[1] = (i < 2) ? y : y + h;
            try {
                transform.transformPoint(corner, out);
            } catch (Throwable ignored) {
                // A transform the platform will not project -- treat the draw as covering what
                // it was asked to cover, which is what the clipped rectangle says.
                layer.noteCanvasCover(x, y, w, h, test);
                return;
            }
            minX = Math.min(minX, out[0]);
            minY = Math.min(minY, out[1]);
            maxX = Math.max(maxX, out[0]);
            maxY = Math.max(maxY, out[1]);
        }
        // The outline test belongs to untransformed coordinates, so it is dropped here rather
        // than asked the wrong question: what remains is the projected bounding box, which is
        // what the draw is known to be inside.
        layer.noteCanvasCover((int) Math.floor(minX), (int) Math.floor(minY),
                (int) Math.ceil(maxX - minX), (int) Math.ceil(maxY - minY), null);
    }


    /// Buffers a blit of a raw canvas (an offscreen WebGL render target) into the
    /// display op stream. Used by the GPU compositing path so a RenderView's 3D
    /// frame is drawn onto the display surface in flushGraphics(), layering with
    /// the rest of the UI -- unlike {@link #drawImage} this takes a live canvas,
    /// not a NativeImage.
    public void drawCanvas(com.codename1.html5.js.dom.HTMLCanvasElement canvas, int x, int y, int w, int h) {
        if (canvas == null || w <= 0 || h <= 0) {
            return;
        }
        noteCanvasCover(x, y, w, h);
        upcoming.add(new com.codename1.impl.html5.graphics.DrawCanvas(canvas, x, y, w, h, 255));
    }

    @Override
    public void tileImage(Object img, int x, int y, int w, int h) {
        if (clipEmpty) { return; }
        noteCanvasCover(x, y, w, h);
        imageTransformRenderAdapter.tileImage((NativeImage)img, x, y, w, h);
    }
    
    

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        noteStrokeCover(arcOutline(x, y, width, height, startAngle, arcAngle, false), 1);
        addOp(new DrawArc(x, y, width, height, startAngle, arcAngle, getColor(), getAlpha()));
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        noteCanvasCover(x, y, width, height);
        primitiveRenderAdapter.fillRect(x, y, width, height);
    }

    @Override
    public void blurRegion(int x, int y, int width, int height, float radius, float cornerRadius) {
        // A blur rewrites what is under it. Promoted text is not under it -- it is above the
        // canvas entirely -- so it would come out unblurred beside everything else. Back to the
        // canvas it goes.
        noteAlphaIndependentRegion(x, y, width, height, cornerRadius > 0
                ? roundRectCoverTest(x, y, width, height, (int) (cornerRadius * 2), (int) (cornerRadius * 2))
                : null);
        // Route through addOp (this class's chokepoint) so the empty-clip cull
        // applies; the base class records into its own immediate context.
        addOp(new com.codename1.impl.html5.graphics.BlurRegion(x, y, width, height, radius, cornerRadius));
    }

    @Override
    public void glassRegion(int x, int y, int width, int height, float radius, float cornerRadius,
            float saturation, float scale, float offset, float refraction, float specular) {
        // Glass samples what is behind it and draws the result. Promoted text is not behind it,
        // so the material would be made from a backdrop the text is missing from, while the text
        // itself floated over the finished glass.
        noteAlphaIndependentRegion(x, y, width, height, cornerRadius > 0
                ? roundRectCoverTest(x, y, width, height, (int) (cornerRadius * 2), (int) (cornerRadius * 2))
                : null);
        addOp(new com.codename1.impl.html5.graphics.GlassRegion(x, y, width, height,
                radius, cornerRadius, saturation, scale, offset, refraction, specular));
    }

    @Override
    public void lensRegion(int x, int y, int width, int height, float cornerRadius, float magnify,
            float aberration, int tintColor, float tintStrength) {
        // A lens magnifies, tints and aberrates what is under it. Promoted text is not under it,
        // so the selected tab's label would float over the effect untouched instead of being
        // drawn through it. Back to the canvas, where the lens can reach it.
        noteAlphaIndependentRegion(x, y, width, height, cornerRadius > 0
                ? roundRectCoverTest(x, y, width, height, (int) (cornerRadius * 2), (int) (cornerRadius * 2))
                : null);
        addOp(new com.codename1.impl.html5.graphics.LensRegion(x, y, width, height, cornerRadius,
                magnify, aberration, tintColor, tintStrength));
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        // Erasing the canvas erases nothing in the layer above it, so text promoted out of this
        // region would go on showing over pixels that were wiped. Reported whatever the alpha
        // is: a clear takes pixels away rather than painting over them.
        noteAlphaIndependentRegion(x, y, width, height, null);
        primitiveRenderAdapter.clearRect(x, y, width, height);
    }
    
    

    @Override
    public void drawRect(int x, int y, int width, int height) {
        noteStrokeCover(rectOutline(x, y, width, height), 1);
        primitiveRenderAdapter.drawRect(x, y, width, height);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        noteStrokeCover(segmentOutline(x1, y1, x2, y2), 1);
        primitiveRenderAdapter.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        noteStrokeCover(roundRectOutline(x, y, width, height, arcWidth, arcHeight), 1);
        addOp(new DrawRoundRect(x, y, width, height, arcWidth, arcHeight, getColor(), getAlpha()));
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        noteCanvasCover(x, y, width, height,
                roundRectCoverTest(x, y, width, height, arcWidth, arcHeight));
        addOp(new FillRoundRect(x, y, width, height, arcWidth, arcHeight, getColor(), getAlpha()));
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        noteStrokeCover(polygonOutline(xPoints, yPoints, nPoints), 1);
        addOp(new DrawPolygon(xPoints, yPoints, nPoints, getColor(), getAlpha()));
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        noteCanvasCover(xPoints, yPoints, nPoints);
        addOp(new FillPolygon(xPoints, yPoints, nPoints, getColor(), getAlpha()));
    }

    @Override
    public void drawShape(Shape shape, Stroke stroke) {
        if (shape != null) {
            noteStrokeCover(outlineOf(shape),
                    stroke == null ? 1 : (int) Math.ceil(stroke.getLineWidth()));
        }
        shapeGradientRenderAdapter.drawShape(shape, stroke);
    }
    
    @Override
    public void fillShape(Shape shape) {
        noteCanvasCover(shape);
        shapeGradientRenderAdapter.fillShape(shape);
    }

    @Override
    public void setTransform(Transform t) {
        setTransform(t, true);
    }

    @Override
    public void setTransform(Transform t, boolean replace) {
        if (transform == null || replace) {
            transform = t;
        } else if (!replace) {
            transform.concatenate(t);
        }
        setTransformChanged();
        applyTransform();
    }

    @Override
    public void applyTransform() {
        if (!transformApplied) {
            imageTransformRenderAdapter.applyTransform(((JSAffineTransform)transform.getNativeTransform()).cloneTransform(), true);
            transformApplied = true;
        }
    }
    
    @Override
    public void setTransformChanged() {
        transformApplied = false;
        clipBoundsDirty = true;
    }
    
    @Override
    public Transform getTransform() {
        if (transform == null) {
            transform = Transform.makeIdentity();
        }
        return transform;
    }

    @Override
    public void resetAffine() {
        if (transform != null && !transform.isIdentity()) {
            transform.setIdentity();
            setTransformChanged();
            applyTransform();
        }
    }

    @JSBody(params={"str"}, script="console.log(str)")
    private native static void log(String str);
    
    
    @JSBody(params={"str"}, script="console.log(str)")
    private native static void log(JSObject str);
    
    @Override
    public void rotate(double angle) {
        if (transform == null) transform = Transform.makeIdentity();
        transform.rotate((float)angle, 0, 0);
        setTransformChanged();
        applyTransform();
    }

    @Override
    public void rotate(double angle, int pivotX, int pivotY) {
        if (transform == null) transform = Transform.makeIdentity();
        transform.rotate((float)angle, pivotX, pivotY);
        setTransformChanged();
        applyTransform();
    }

    @Override
    public void scale(double sx, double sy) {
        if (transform == null) transform = Transform.makeIdentity();
        transform.scale((float)sx, (float)sy);
        setTransformChanged();
        applyTransform();
    }

    @Override
    public void translateMatrix(double tx, double ty) {
        // Master added Graphics.translateMatrix in commit 826d60f32 / the
        // InscribedTriangleGrid test; the framework dispatches to
        // HTML5Implementation.translateMatrix which delegates to
        // ((HTML5Graphics) graphics).translateMatrix(...). Without this
        // override BufferedGraphics inherits HTML5Graphics's translateMatrix,
        // which mutates the parent class's ``transform`` field -- a
        // *different* field from the one BufferedGraphics's own
        // scale/rotate/etc. overrides use. The result: translateMatrix on
        // the form's graphics silently no-ops as far as queued ops are
        // concerned, leaving the InscribedTriangleGrid cells anchored at
        // (0,0) instead of their per-cell column/row pivots. Override here
        // so the BufferedGraphics-side ``transform`` field receives the
        // composition and the next applyTransform() submits a SetTransform
        // op carrying the right matrix.
        if (transform == null) transform = Transform.makeIdentity();
        transform.translate((float)tx, (float)ty);
        setTransformChanged();
        applyTransform();
    }

    //@Override
    //public void shear(double shx, double shy) {
    //    setTransform(JSAffineTransform.Factory.getShearInstance(shx, shy), false);
    //}
    
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        noteCanvasCover(x, y, width, height, arcCoverTest(x, y, width, height, startAngle, arcAngle));
        addOp(new FillArc(x, y, width, height, startAngle, arcAngle, getColor(), getAlpha()));
    }

    @Override
    public void drawRGB(int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        if (offset != 0){
            int[] copy = new int[w*h];
            System.arraycopy(rgbData, offset, copy, 0, w*h);
            rgbData = copy;
        }
        NativeImage img = (NativeImage)impl.createImage(rgbData, w, h);
        drawImage(img, x, y, w, h);
        
    }

    @Override
    public void drawString(String str, int x, int y) {
        if (promoteToTextLayer(str, x, y)) {
            return;
        }
        primitiveRenderAdapter.drawString(str, x, y);
    }

    /**
     * Holds text on the canvas for a run whose caller draws more than the glyphs.
     *
     * <p>Underline, strike-through and overline are drawn as lines after the glyphs, over them.
     * The DOM layer sits above the whole canvas, so a promoted glyph would cover the line that
     * is meant to cross it. A decorated run keeps glyphs and lines together on the canvas,
     * where the drawing order still means what it says.</p>
     *
     * @param value true to keep text on the canvas
     */
    void setPromotionSuspended(boolean value) {
        promotionSuspended = value;
    }

    /**
     * Offers a text run to the DOM text layer, which renders it as real text above the canvas.
     *
     * <p>Only runs this class can reproduce faithfully are offered. A shape clip has no
     * {@code overflow:hidden} equivalent, and under a non-identity transform the run would have
     * to be re-projected, so both stay on the canvas. Bitmap fonts never reach here at all --
     * {@code Graphics.drawString} renders a {@code CustomFont} itself and never calls the
     * implementation.</p>
     *
     * <p>This override lives on the display graphics only. Offscreen surfaces use plain
     * {@link HTML5Graphics}, so text painted into a transition buffer, a paint lock image, a
     * {@code ComponentImage} or a screenshot is still rasterized onto its bitmap, which is what
     * those callers read back.</p>
     *
     * @return true when the layer took the run and nothing should be drawn on the canvas
     */
    private boolean promoteToTextLayer(String str, int x, int y) {
        JavaScriptTextLayer layer = impl == null ? null : impl.textLayer;
        if (layer == null || clipEmpty || isClipShape || promotionSuspended) {
            return false;
        }
        if (transform != null && !transform.isIdentity()) {
            return false;
        }
        return layer.promote(str, x, y,
                clipBoundsTracker.getX(), clipBoundsTracker.getY(),
                clipBoundsTracker.getWidth(), clipBoundsTracker.getHeight(),
                getRenderState().getColor(), getRenderState().getAlpha(),
                getRenderState().getFont(), HTML5Implementation.getDevicePixelRatio());
    }

    @Override
    void setAlpha(int alpha) {
        getRenderState().setAlpha(alpha);
    }

    @Override
    void setColor(int color) {
        getRenderState().setColor(color);
    }

    @Override
    void setFont(NativeFont font) {
        getRenderState().setFont(font);
    }
    
    List<ExecutableOp> flush(int x, int y, int width, int height){
        List<ExecutableOp> current;
        synchronized(upcoming){
            current = new ArrayList<ExecutableOp>(upcoming.size());
            for (int i = 0; i < upcoming.size(); i++) {
                current.add(upcoming.get(i));
            }
            upcoming.clear();
        }
        return current;
    }

    private Transform getInverseTransform() {
        if (transform == null) return null;
        return transform.getInverse();
    }
    
    private Shape getCurrentClipProjection() {
        if (isClipShape) {
            GeneralPath out = new GeneralPath(clipShape);
            Transform t = Transform.makeIdentity();
            if (clipTransform != null && !clipTransform.isIdentity()) {
                t.concatenate(clipTransform);
            }
            if (transform != null && !transform.isIdentity()) {
                t.concatenate(transform.getInverse());
            }
            if (!t.isIdentity()) {
                out.transform(t);
            }
            return out;
        } else {
            if (transform != null && !transform.isIdentity()) {
                GeneralPath out = new GeneralPath();
                out.setRect(clip, transform.getInverse());
                return out;
            } else {
                return clip;
            }
        }
    }
    
    @Override
    public void setClip(Shape shape) {
        // Sync the reliable user-space clip tracker; clipShape() bypasses this via
        // setClipShapeInternal so it doesn't clobber the clipEmpty/tracker that
        // clipRect computed from the exact rect intersection. Issue #5263.
        Rectangle b = shape.getBounds();
        int bw = Math.max(0, b.getWidth());
        int bh = Math.max(0, b.getHeight());
        clipBoundsTracker.setBounds(b.getX(), b.getY(), bw, bh);
        clipEmpty = bw <= 0 || bh <= 0;
        setClipShapeInternal(shape);
    }

    private void setClipShapeInternal(Shape shape) {
        clipShape.reset();
        clipShape.setShape(shape, null);
        isClipShape = true;
        clipTransform = transform == null ? null : transform.copy();
        JSAffineTransform t = null;
        if (transform != null) {
            t = (JSAffineTransform)transform.getNativeTransform();
        }
        clipBoundsDirty = true;
        imageTransformRenderAdapter.setClipShape(shape, t);
    }

    private static final class ClipFrame {
        final Rectangle rect;
        final GeneralPath shape;
        final boolean isShape;
        final Transform transform;

        ClipFrame(Rectangle rect, GeneralPath shape, boolean isShape, Transform transform) {
            this.rect = new Rectangle(rect);
            this.shape = shape == null ? null : new GeneralPath(shape);
            this.isShape = isShape;
            this.transform = transform == null ? null : transform.copy();
        }
    }

    private final java.util.ArrayList<ClipFrame> clipStack = new java.util.ArrayList<ClipFrame>();

    @Override
    public void pushClip() {
        clipStack.add(new ClipFrame(clip, clipShape, isClipShape, clipTransform));
    }

    @Override
    public void popClip() {
        if (clipStack.isEmpty()) {
            return;
        }
        ClipFrame frame = clipStack.remove(clipStack.size() - 1);
        if (frame.isShape) {
            if (frame.transform != null && !frame.transform.isIdentity()) {
                Transform savedTransform = transform;
                transform = frame.transform;
                try {
                    setClip(frame.shape);
                } finally {
                    transform = savedTransform;
                }
            } else {
                setClip(frame.shape);
            }
        } else {
            setClip(frame.rect.getX(), frame.rect.getY(), frame.rect.getWidth(), frame.rect.getHeight());
        }
    }
    
    private void clipShape(Shape shape) {
        if (!isClipShape) {
            isClipShape = true;
            clipShape.reset();
            clipShape.setShape(clip, null);
            clipTransform = null;
        }
        GeneralPath p = (GeneralPath)getCurrentClipProjection();
        p.intersect(shape);
        if (clipEmpty) {
            // Empty intersection (computed reliably by clipRect): record a
            // zero-area rect clip so the canvas culls every draw. The degenerate
            // shape path leaks fills AND image blits on the host. Issue #5263.
            clipBoundsDirty = true;
            primitiveRenderAdapter.setClipRect(0, 0, 0, 0);
            return;
        }
        setClipShapeInternal(p);
    }


    @Override
    public void setClip(int x, int y, int width, int height) {
        if (transform != null && !transform.isIdentity()) {
            setClip(new Rectangle(x, y, width, height));
            return;
        }
        isClipShape = false;
        clip.setBounds(x, y, width, height);
        clipBoundsTracker.setBounds(x, y, width, height);
        clipEmpty = width <= 0 || height <= 0;
        clipBoundsDirty = true;
        primitiveRenderAdapter.setClipRect(x, y, width, height);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        Rectangle rect = new Rectangle(x, y, width, height);
        // Track the running clip bounds in user space, clamped to >= 0, so an
        // empty intersection is detected reliably whether the clip is tracked as
        // a rect or a shape. Issue #5263.
        clipBoundsTracker.intersection(rect, clipBoundsTracker);
        clipEmpty = clipBoundsTracker.getWidth() <= 0 || clipBoundsTracker.getHeight() <= 0;
        if (isClipShape || transform != null && !transform.isIdentity()) {
            clipShape(rect);
            return;
        }

        if (rect.contains(clip)) {
            return;
        }
        clip = clip.intersection(x, y, width, height);
        clipBoundsDirty = true;
        primitiveRenderAdapter.setClipRect(clip.getX(), clip.getY(), clip.getWidth(), clip.getHeight());
    }
    
    private void calculateClipBounds() {
        if (clipBoundsDirty) {
            clipBoundsDirty = false;
            Rectangle projectedShape = getCurrentClipProjection().getBounds();
            clipBounds.setBounds(projectedShape.getX(), projectedShape.getY(), projectedShape.getWidth(), projectedShape.getHeight());
        }
    }
    
    @Override
    public int getClipHeight() {
        calculateClipBounds();
        return clipBounds.getHeight();
    }

    @Override
    public int getClipWidth() {
        calculateClipBounds();
        return clipBounds.getWidth();
    }

    @Override
    public int getClipX() {
        calculateClipBounds();
        return clipBounds.getX();
    }

    @Override
    public int getClipY() {
        calculateClipBounds();
        return clipBounds.getY();
    }
 
    
    @Override
    public void fillLinearGradient(int x, int y, int width, int height, int startColor, int endColor, boolean horizontal) {
        noteCanvasCover(x, y, width, height);
        shapeGradientRenderAdapter.fillLinearGradient(x, y, width, height, startColor, endColor, horizontal);
    }

    @Override
    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        // A radial gradient fills the oval inscribed in these bounds, or a sector of it -- not
        // the bounds themselves, whose corners it never reaches.
        noteCanvasCover(x, y, width, height, arcCoverTest(x, y, width, height, startAngle, arcAngle));
        shapeGradientRenderAdapter.fillRadialGradient(x, y, width, height, startColor, endColor, startAngle, arcAngle);
    }
    
    @Override
    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height) {
        noteCanvasCover(x, y, width, height, arcCoverTest(x, y, width, height, 0, 360));
        shapeGradientRenderAdapter.fillRadialGradient(x, y, width, height, startColor, endColor, 0, 360);
    }

    public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height,
            float relativeX, float relativeY, float relativeSize) {
        noteCanvasCover(x, y, width, height);
        shapeGradientRenderAdapter.fillRectRadialGradient(x, y, width, height, startColor, endColor, relativeX, relativeY, relativeSize);
    }
    
    @Override
    public int getAlpha() {
        return getRenderState().getAlpha();
    }

    @Override
    public int getColor() {
        return getRenderState().getColor();
    }

    @Override
    public NativeFont getFont() {
        return getRenderState().getFont();
    }
    
    
}
