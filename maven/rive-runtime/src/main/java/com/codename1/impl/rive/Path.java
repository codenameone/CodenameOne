/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import com.codename1.util.MathUtil;

import java.util.ArrayList;

/// A contour of a shape built from its vertices, in the path's own space.
/// Port of rive-runtime's `Path::buildPath`, including rounded corners.
public class Path extends PathBase {
    protected final ArrayList<PathVertex> vertices = new ArrayList<PathVertex>();
    protected final RivePath rawPath = new RivePath();
    protected Shape shape;
    private boolean shapeNotified;

    public void addVertex(PathVertex v) {
        vertices.add(v);
    }

    public ArrayList<PathVertex> getVertices() {
        return vertices;
    }

    public RivePath getRawPath() {
        return rawPath;
    }

    public Shape getShape() {
        return shape;
    }

    public boolean isPathClosed() {
        return true;
    }

    /// The transform that takes this path's points to world space.
    public Mat2D getPathTransform() {
        return worldTransform;
    }

    @Override
    public int onAddedClean(Artboard context) {
        int code = super.onAddedClean(context);
        if (code != STATUS_OK) {
            return code;
        }
        for (ContainerComponent p = parent; p != null; p = p.getParent()) {
            if (p instanceof Shape) {
                shape = (Shape) p;
                shape.addPath(this);
                break;
            }
        }
        return STATUS_OK;
    }

    @Override
    public void buildDependencies() {
        super.buildDependencies();
        for (int i = 0; i < vertices.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            vertices.get(i).addDependent(this);
        }
    }

    public void markPathDirty() {
        addDirt(DIRT_PATH);
        notifyShape();
    }

    private void notifyShape() {
        if (shapeNotified || shape == null) {
            return;
        }
        shapeNotified = true;
        shape.pathChanged();
    }

    @Override
    protected void onDirty(int value) {
        if (hasDirt(value, DIRT_WORLD_TRANSFORM)) {
            notifyShape();
        }
    }

    @Override
    public void update(int value) {
        shapeNotified = false;
        super.update(value);
        if (hasDirt(value, DIRT_PATH)) {
            rawPath.rewind();
            buildPath(rawPath, vertices, isPathClosed(), corner);
        }
    }

    @Override
    protected void isHoleChanged() {
        markPathDirty();
    }

    private static float outX(PathVertex v) {
        return v instanceof CubicVertex ? ((CubicVertex) v).getRenderOutX() : v.getRenderX();
    }

    private static float outY(PathVertex v) {
        return v instanceof CubicVertex ? ((CubicVertex) v).getRenderOutY() : v.getRenderY();
    }

    private static float inX(PathVertex v) {
        return v instanceof CubicVertex ? ((CubicVertex) v).getRenderInX() : v.getRenderX();
    }

    private static float inY(PathVertex v) {
        return v instanceof CubicVertex ? ((CubicVertex) v).getRenderInY() : v.getRenderY();
    }

    static float idealControlPointDistance(float px, float py, float nx, float ny, float radius) {
        float angle = Math.abs((float) MathUtil.atan2(px * ny - py * nx, px * nx + py * ny));
        double pi = Math.PI;
        float k = (float) ((4.0 / 3.0) * Math.tan(pi / (2.0 * ((2.0 * pi) / angle))) * radius
                * (angle < pi / 2 ? 1 + Math.cos(angle) : 2.0 - Math.sin(angle)));
        return Math.min(radius, k);
    }

    // corner-rounding scratch, per path so two paths never share it
    private final float[] corner = new float[8];

    /// Computes a rounded corner at `v`, writing the curve's start into
    /// `c4[4..5]`, its two controls into `c4[0..3]` and its end into
    /// `c4[6..7]`.
    private static void corner(PathVertex prev, StraightVertex v, PathVertex next, float[] c4) {
        float x = v.getRenderX();
        float y = v.getRenderY();
        float radius = v.getRadius();
        float tpx = outX(prev) - x;
        float tpy = outY(prev) - y;
        float tpl = (float) Math.sqrt(tpx * tpx + tpy * tpy);
        if (tpl > 0) {
            tpx /= tpl;
            tpy /= tpl;
        }
        float tnx = inX(next) - x;
        float tny = inY(next) - y;
        float tnl = (float) Math.sqrt(tnx * tnx + tny * tny);
        if (tnl > 0) {
            tnx /= tnl;
            tny /= tnl;
        }
        float r = Math.min(tpl / 2f, Math.min(tnl / 2f, Math.abs(radius)));
        float ideal = idealControlPointDistance(tpx, tpy, tnx, tny, r);
        float sx = x + tpx * r;
        float sy = y + tpy * r;
        float ox = x + tpx * (r - ideal);
        float oy = y + tpy * (r - ideal);
        float ix = x + tnx * (r - ideal);
        float iy = y + tny * (r - ideal);
        float ex = x + tnx * r;
        float ey = y + tny * r;
        if (radius < 0) {
            // rotatePoints(next = end, prev = start, point = pos)
            float v1x = sx - ex;
            float v1y = sy - ey;
            float v2x = x - ex;
            float v2y = y - ey;
            float a = (float) MathUtil.atan2(v1x * v2y - v1y * v2x, v1x * v2x + v1y * v2y);
            float s = (float) Math.sin(a * 2);
            float c = (float) Math.cos(a * 2);
            float qx = ox - sx;
            float qy = oy - sy;
            ox = qx * c - qy * s + sx;
            oy = qx * s + qy * c + sy;
            s = (float) Math.sin(-a * 2);
            c = (float) Math.cos(-a * 2);
            qx = ix - ex;
            qy = iy - ey;
            ix = qx * c - qy * s + ex;
            iy = qx * s + qy * c + ey;
        }
        c4[0] = ox;
        c4[1] = oy;
        c4[2] = ix;
        c4[3] = iy;
        c4[4] = sx;
        c4[5] = sy;
        c4[6] = ex;
        c4[7] = ey;
    }

    /// Port of `Path::buildPath`.
    public static void buildPath(RivePath out, ArrayList<? extends PathVertex> verts, boolean closed, float[] c4) {
        int length = verts.size();
        if (length < 2) {
            return;
        }
        PathVertex first = verts.get(0);
        float ox;
        float oy;
        float startX;
        float startY;
        float startInX;
        float startInY;
        boolean prevIsCubic;
        boolean startIsCubic;
        if (first instanceof CubicVertex) {
            CubicVertex c = (CubicVertex) first;
            startIsCubic = true;
            prevIsCubic = true;
            startInX = c.getRenderInX();
            startInY = c.getRenderInY();
            ox = c.getRenderOutX();
            oy = c.getRenderOutY();
            startX = c.getRenderX();
            startY = c.getRenderY();
            out.moveTo(startX, startY);
        } else {
            startIsCubic = false;
            prevIsCubic = false;
            StraightVertex p = (StraightVertex) first;
            if (p.getRadius() != 0f) {
                corner(verts.get(length - 1), p, verts.get(1), c4);
                startInX = startX = c4[4];
                startInY = startY = c4[5];
                out.moveTo(startX, startY);
                out.cubicTo(c4[0], c4[1], c4[2], c4[3], c4[6], c4[7]);
                ox = c4[6];
                oy = c4[7];
            } else {
                startInX = startX = ox = p.getRenderX();
                startInY = startY = oy = p.getRenderY();
                out.moveTo(ox, oy);
            }
        }
        for (int i = 1; i < length; i++) {
            PathVertex vertex = verts.get(i);
            if (vertex instanceof CubicVertex) {
                CubicVertex c = (CubicVertex) vertex;
                out.cubicTo(ox, oy, c.getRenderInX(), c.getRenderInY(), c.getRenderX(), c.getRenderY());
                prevIsCubic = true;
                ox = c.getRenderOutX();
                oy = c.getRenderOutY();
            } else {
                StraightVertex p = (StraightVertex) vertex;
                float px = p.getRenderX();
                float py = p.getRenderY();
                if (p.getRadius() != 0f) {
                    corner(verts.get(i - 1), p, verts.get((i + 1) % length), c4);
                    if (prevIsCubic) {
                        out.cubicTo(ox, oy, c4[4], c4[5], c4[4], c4[5]);
                    } else {
                        out.lineTo(c4[4], c4[5]);
                    }
                    out.cubicTo(c4[0], c4[1], c4[2], c4[3], c4[6], c4[7]);
                    ox = c4[6];
                    oy = c4[7];
                    prevIsCubic = false;
                } else if (prevIsCubic) {
                    out.cubicTo(ox, oy, px, py, px, py);
                    prevIsCubic = false;
                    ox = px;
                    oy = py;
                } else {
                    ox = px;
                    oy = py;
                    out.lineTo(ox, oy);
                }
            }
        }
        if (closed) {
            if (prevIsCubic || startIsCubic) {
                out.cubicTo(ox, oy, startInX, startInY, startX, startY);
            } else {
                out.lineTo(startX, startY);
            }
            out.close();
        }
    }
}
