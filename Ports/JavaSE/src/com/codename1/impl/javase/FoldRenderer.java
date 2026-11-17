/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

/// Draws a simulator skin as a physical device, in 3D, with the
/// `com.codename1.gpu` API: folded part way, or turning between portrait and
/// landscape.
///
/// The device (skin with the live screen composited in) is a mesh, not a
/// picture: each half is a slab whose outline follows the skin's rounded
/// corners, with a front textured by the skin, a back textured by the cover
/// display, metal side walls, and a spine that wraps the hinge as the halves
/// part. The first half (left of a vertical hinge, above a horizontal one)
/// is the reference; the second swings about the hinge toward the viewer and
/// over the first, so at 0 degrees its back, the cover display, lies on top.
///
/// What makes the motion read on a flat monitor, all of it faded in by how
/// far the device has left its resting pose, so a flat device renders
/// exactly as the flat skin:
///
/// - the whole device turns as it folds, the way a reader holds a book, so
///   neither half is ever seen edge on;
/// - a key light from the upper left, so the halves and walls shade apart;
/// - the device tips toward the viewer, so its top wall shows;
/// - a soft shadow under the device, cast from its own silhouette.
///
/// It renders through a `Target`: the GPU (`JavaSEJoglFoldTarget`, an
/// offscreen OpenGL framebuffer read back each frame) when JOGL loads, or the
/// software rasterizer (`JavaSESoftwareDevice`) otherwise and headless. Both
/// hand back a `BufferedImage`, which the Swing canvas composites and the
/// screen recorder captures.
final class FoldRenderer {

    private static final float FOV_DEGREES = 30f;
    private static final double MAX_TILT = Math.toRadians(9);
    private static final double ROTATION_TILT = Math.toRadians(14);
    /// The thickness of one half as a share of the shorter side of the skin.
    private static final float THICKNESS = 0.026f;
    private static final int CORNER_SEGMENTS = 10;
    private static final int SPINE_SEGMENTS = 12;
    private static final int WALL_COLOR = 0xff9c9ca4;

    /// Where a frame is drawn: owns a device, makes it current, runs the
    /// scene on it and returns the frame.
    interface Target {
        BufferedImage render(int width, int height, Painter painter);

        /// True for a GPU, which renders full size at animation rates.
        boolean isAccelerated();
    }

    interface Painter {
        void paint(GraphicsDevice device);
    }

    /// The software rasterizer, drawing straight into an image.
    static final class SoftwareTarget implements Target {
        private final JavaSESoftwareDevice soft = new JavaSESoftwareDevice();

        public BufferedImage render(int width, int height, Painter painter) {
            soft.resize(width, height);
            painter.paint(soft);
            return soft.getImage();
        }

        public boolean isAccelerated() {
            return false;
        }
    }

    private final Target target;
    /// The device of the frame being painted.
    private GraphicsDevice device;
    /// What this frame created on the device, released when it is done.
    private final List<Texture> frameTextures = new ArrayList<Texture>();
    private final List<VertexBuffer> frameBuffers = new ArrayList<VertexBuffer>();
    private final Camera camera = new Camera();
    private final Light light = new Light();
    private BufferedImage shadowSmall;
    private int radiusFor = -1;
    private float radius;

    // Per frame state shared by the vertex transform.
    private int w;
    private int h;
    private float split;
    private boolean verticalAxis;
    private double phi;
    /// The turn of the whole device about the hinge.
    private double bodyTurn;
    /// The turn of the whole device in the screen plane (rotation).
    private double roll;
    /// The tip of the whole device toward the viewer about the screen x axis.
    private double tilt;
    /// The uniform scale that keeps the moved device inside the frame.
    private float fit = 1;

    FoldRenderer() {
        this(new SoftwareTarget());
    }

    FoldRenderer(Target target) {
        this.target = target;
    }

    boolean isAccelerated() {
        return target.isAccelerated();
    }

    /// Renders `front` (the open device, `W x H`) folded to `angle` degrees.
    ///
    /// - `back`: the image for the outside of the swinging panel (the cover
    ///   display with its skin), or null for a plain dark back.
    /// - `hinge`: the hinge in `front` pixels; null folds at the centre.
    /// - `verticalAxis`: true when the hinge runs top to bottom.
    ///
    /// Returns an image the size of `front`, transparent around the device.
    /// The returned image is reused by the next call.
    BufferedImage render(BufferedImage front, BufferedImage back, Rectangle hinge,
            boolean verticalAxis, double angle) {
        double clamped = Math.max(0, Math.min(180, angle));
        // How far into the fold, 0 flat .. 1 at half open and beyond; every
        // effect is scaled by it, eased so none of them starts abruptly.
        // They fade out again over the last 45 degrees of closing: a closed
        // device faces the viewer squarely, and its cover must show in its
        // own colors.
        double t = Math.min(1, (180 - clamped) / 90.0) * Math.min(1, clamped / 45.0);
        float fold = (float) (t * t * (3 - 2 * t));
        phi = Math.toRadians(180 - clamped);
        // Back toward facing the viewer as it closes, where the cover shows.
        bodyTurn = -phi / 2 * Math.min(1, clamped / 90.0);
        roll = 0;
        tilt = MAX_TILT * fold;
        return draw(front, back, hinge, verticalAxis, fold, front.getWidth(), front.getHeight());
    }

    /// Renders the flat device `front` turned by `rollDegrees` in the screen
    /// plane (negative is clockwise) and lifted toward the viewer by `lift`
    /// (0 .. 1), centered in a `outW x outH` image: one frame of the device
    /// rotating between portrait and landscape.
    BufferedImage renderRotation(BufferedImage front, double rollDegrees, double lift, int outW, int outH) {
        return renderRotation(front, null, null, true, 180, rollDegrees, lift, outW, outH);
    }

    /// As above for a device held folded to `angle` (see `render`), which
    /// turns as it is, still folded, rather than snapping flat first.
    BufferedImage renderRotation(BufferedImage front, BufferedImage back, Rectangle hinge,
            boolean verticalAxis, double angle, double rollDegrees, double lift, int outW, int outH) {
        float l = (float) Math.max(0, Math.min(1, lift));
        double clamped = Math.max(0, Math.min(180, angle));
        double t = Math.min(1, (180 - clamped) / 90.0) * Math.min(1, clamped / 45.0);
        float fold = (float) (t * t * (3 - 2 * t));
        phi = Math.toRadians(180 - clamped);
        bodyTurn = -phi / 2 * Math.min(1, clamped / 90.0);
        roll = Math.toRadians(rollDegrees);
        tilt = Math.max(MAX_TILT * fold, ROTATION_TILT * l);
        return draw(front, back, hinge, verticalAxis, Math.max(fold, l), outW, outH);
    }

    private BufferedImage draw(BufferedImage front, BufferedImage back, Rectangle hinge,
            boolean vertical, float effect, int outW, int outH) {
        w = front.getWidth();
        h = front.getHeight();
        this.verticalAxis = vertical;
        if (vertical) {
            split = hinge != null ? hinge.x + hinge.width / 2f : w / 2f;
        } else {
            split = hinge != null ? hinge.y + hinge.height / 2f : h / 2f;
        }
        updateRadius(front);

        float dist = (float) (outH / 2f / Math.tan(Math.toRadians(FOV_DEGREES / 2)));
        camera.setPerspective(FOV_DEGREES, dist * 0.05f, dist * 4f)
                .setAspect(outW / (float) outH)
                .setPosition(0, 0, dist)
                .setTarget(0, 0, 0)
                .setUp(0, 1, 0);

        // Head on and neutral at rest (a facing panel is lit at exactly
        // 1.0: diffuse 0.75 + ambient 0.25), a key light from the upper left
        // with deeper shadows as the device moves.
        light.setDirection(0.45f * effect, -0.35f * effect, -1);
        int diffuse = 0xc0 + Math.round(0x18 * effect);
        int ambient = 0x40 - Math.round(0x06 * effect);
        light.setColor(0xff000000 | diffuse << 16 | diffuse << 8 | diffuse);
        light.setAmbientColor(0xff000000 | ambient << 16 | ambient << 8 | ambient);

        float thick = Math.min(w, h) * THICKNESS;
        fit = 1;
        fit = fitScale(thick, dist, outW, outH) * (1 - 0.07f * effect);

        final float thickness = thick;
        final BufferedImage frontImage = front;
        final BufferedImage backImage = back;
        BufferedImage img = target.render(outW, outH, new Painter() {
            public void paint(GraphicsDevice d) {
                device = d;
                try {
                    d.setLight(light);
                    d.setCamera(camera);
                    d.clear(0x00000000, true, true);
                    paintScene(frontImage, backImage, thickness);
                } finally {
                    for (Texture t : frameTextures) {
                        d.dispose(t);
                    }
                    for (VertexBuffer b : frameBuffers) {
                        d.dispose(b);
                    }
                    frameTextures.clear();
                    frameBuffers.clear();
                    device = null;
                }
            }
        });
        return effect > 0.01f ? withShadow(img, effect, Math.min(outW, outH)) : img;
    }

    private Texture texture(int tw, int th, int[] argb) {
        Texture t = device.createTexture(tw, th, argb);
        frameTextures.add(t);
        return t;
    }

    private void paintScene(BufferedImage front, BufferedImage back, float thick) {
        Texture frontTex = texture(w, h, pixels(front));
        frontTex.setFilter(Texture.Filter.LINEAR);
        // The displays are emissive: never shaded, so the screen keeps its
        // real colors at every angle and stays usable while folded. Only
        // the body (walls, plain backs) takes the light.
        Material frontMat = unlit(material(frontTex, 0xffffffff, true));
        Material wallMat = material(null, WALL_COLOR, false);
        Material backMat;
        if (back != null) {
            Texture backTex = texture(back.getWidth(), back.getHeight(), pixels(back));
            backTex.setFilter(Texture.Filter.LINEAR);
            backMat = unlit(material(backTex, 0xffffffff, true));
        } else {
            backMat = null;
        }

        float[] fixedR;
        float[] swingR;
        if (verticalAxis) {
            fixedR = new float[]{0, 0, split, h};
            swingR = new float[]{split, 0, w, h};
        } else {
            fixedR = new float[]{0, 0, w, split};
            swingR = new float[]{0, split, w, h};
        }
        if (backMat == null) {
            backMat = backPlateMaterial(swingR, false);
        }
        List<float[]> fixedOutline = outline(fixedR, false);
        List<float[]> swingOutline = outline(swingR, true);
        // Opaque geometry first, so the blended faces find it in the depth
        // buffer: the walls, and the spine once the halves part.
        walls(wallMat, fixedOutline, false, thick);
        walls(wallMat, swingOutline, true, thick);
        if (phi > 0.001) {
            spine(wallMat, thick);
        }
        face(frontMat, fixedOutline, false, true, 0);
        face(frontMat, swingOutline, true, true, 0);
        plateFace = back == null;
        face(backMat, swingOutline, true, false, thick);
        plateFace = false;
        if (phi > 0.001) {
            // The fixed half's back only shows once the device turns.
            plateFace = true;
            face(backPlateMaterial(fixedR, true), fixedOutline, false, false, thick);
            plateFace = false;
        }
    }

    /// The plain back of each half, without and with the camera island.
    private final BufferedImage[] backPlates = new BufferedImage[2];

    /// The back of a half without a cover display: dark anodized glass with
    /// a soft sheen, and the camera island on the half that carries it.
    private Material backPlateMaterial(float[] r, boolean camera) {
        int bw = Math.max(8, Math.round((r[2] - r[0]) / 2));
        int bh = Math.max(8, Math.round((r[3] - r[1]) / 2));
        int k = camera ? 1 : 0;
        BufferedImage backPlate = backPlates[k];
        if (backPlate == null || backPlate.getWidth() != bw || backPlate.getHeight() != bh) {
            backPlate = paintBackPlate(bw, bh, camera, radius / 2);
            backPlates[k] = backPlate;
        }
        Texture t = texture(bw, bh, pixels(backPlate));
        t.setFilter(Texture.Filter.LINEAR);
        return material(t, 0xffffffff, false);
    }

    static BufferedImage paintBackPlate(int bw, int bh, boolean camera, float cornerRadius) {
        BufferedImage img = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new java.awt.GradientPaint(0, 0, new java.awt.Color(0x45454c), bw, bh,
                new java.awt.Color(0x1f1f23)));
        g.fillRect(0, 0, bw, bh);
        // A diagonal sheen, as a frosted back catches a key light.
        g.setPaint(new java.awt.GradientPaint(bw * 0.2f, 0, new java.awt.Color(255, 255, 255, 0),
                bw * 0.55f, bh * 0.35f, new java.awt.Color(255, 255, 255, 26), true));
        g.fillRect(0, 0, bw, bh);
        if (camera) {
            // Seen from behind the outer edge is on the right of the image.
            float s = Math.min(bw, bh) * 0.42f;
            float m = Math.max(4, cornerRadius * 0.45f);
            float x = bw - m - s;
            float y = m;
            g.setColor(new java.awt.Color(0x2c2c31));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(x, y, s, s, s * 0.36f, s * 0.36f));
            g.setColor(new java.awt.Color(255, 255, 255, 40));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(x, y, s, s, s * 0.36f, s * 0.36f));
            float lens = s * 0.36f;
            float[][] at = {{0.27f, 0.27f}, {0.27f, 0.73f}, {0.73f, 0.5f}};
            for (float[] a : at) {
                float cx = x + s * a[0];
                float cy = y + s * a[1];
                g.setColor(new java.awt.Color(0x0c0c0e));
                g.fill(new java.awt.geom.Ellipse2D.Float(cx - lens / 2, cy - lens / 2, lens, lens));
                g.setColor(new java.awt.Color(0x5a5a66));
                g.draw(new java.awt.geom.Ellipse2D.Float(cx - lens / 2, cy - lens / 2, lens, lens));
                g.setColor(new java.awt.Color(0x1c2a4a));
                float in = lens * 0.45f;
                g.fill(new java.awt.geom.Ellipse2D.Float(cx - in / 2, cy - in / 2, in, in));
                g.setColor(new java.awt.Color(255, 255, 255, 120));
                float hi = lens * 0.12f;
                g.fill(new java.awt.geom.Ellipse2D.Float(cx - in / 4, cy - in / 4, hi, hi));
            }
            g.setColor(new java.awt.Color(0xe8e0c8));
            float fl = s * 0.1f;
            g.fill(new java.awt.geom.Ellipse2D.Float(x + s * 0.73f - fl / 2, y + s * 0.2f - fl / 2, fl, fl));
        }
        g.dispose();
        return img;
    }

    private static int[] pixels(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_ARGB) {
            return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        }
        return img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
    }

    /// The body's corner radius, read from the skin's own alpha along the
    /// diagonal of its top left corner: the first opaque pixel lies
    /// r (1 - 1/sqrt 2) in from the corner.
    private void updateRadius(BufferedImage front) {
        int key = front.getWidth() * 31 + front.getHeight();
        if (key == radiusFor) {
            return;
        }
        radiusFor = key;
        int max = Math.min(front.getWidth(), front.getHeight()) / 4;
        int d = 0;
        while (d < max && (front.getRGB(d, d) >>> 24) < 128) {
            d++;
        }
        radius = Math.min(max, d / 0.2929f);
    }

    /// The uniform scale at which every corner of the device, moved, still
    /// projects inside the frame: x d / (d - z) for a point at depth z.
    private float fitScale(float thick, float dist, int outW, int outH) {
        float best = 1;
        float[][] rects = {{0, 0, w, h}};
        float[] zs = {0, -thick};
        for (float[] r : rects) {
            for (int i = 0; i < 4; i++) {
                float px = (i & 1) == 0 ? r[0] : r[2];
                float py = (i & 2) == 0 ? r[1] : r[3];
                for (int k = 0; k < zs.length; k++) {
                    float[] p = transform(px, py, zs[k], onSwing(px, py));
                    float ax = outW / 2f * dist / (Math.abs(p[0]) * dist + Math.max(0, p[2]) * outW / 2f);
                    float ay = outH / 2f * dist / (Math.abs(p[1]) * dist + Math.max(0, p[2]) * outH / 2f);
                    best = Math.min(best, Math.min(ax, ay));
                }
            }
        }
        return best;
    }

    private boolean onSwing(float px, float py) {
        return verticalAxis ? px > split : py > split;
    }

    private static Material unlit(Material lit) {
        Material m = new Material(Material.Type.UNLIT);
        m.setColor(lit.getColor());
        if (lit.getTexture() != null) {
            m.setTexture(lit.getTexture());
        }
        m.setRenderState(lit.getRenderState());
        return m;
    }

    private Material material(Texture tex, int color, boolean blended) {
        Material m = new Material(Material.Type.LAMBERT);
        m.setColor(color);
        if (tex != null) {
            m.setTexture(tex);
        }
        RenderState rs = blended ? RenderState.transparent().setDepthWrite(true) : RenderState.opaque();
        m.setRenderState(rs.setCullMode(RenderState.CullMode.BACK));
        return m;
    }

    /// The outline of one half in front pixels, counter clockwise on screen
    /// (y down): rounded at the body's corners, square at the hinge. Each
    /// point carries its outward normal in the screen plane, `{x, y, nx, ny}`
    /// with ny in pixel direction; the edge along the hinge is the one from
    /// the last point back to the first.
    private List<float[]> outline(float[] r, boolean swing) {
        float x0 = r[0];
        float y0 = r[1];
        float x1 = r[2];
        float y1 = r[3];
        float cr = Math.min(radius, Math.min(x1 - x0, y1 - y0) / 2f);
        // Corners in order top-left, bottom-left, bottom-right, top-right
        // (counter clockwise with y down), each rounded unless on the hinge.
        boolean[] round;
        if (verticalAxis) {
            round = swing ? new boolean[]{false, false, true, true} : new boolean[]{true, true, false, false};
        } else {
            round = swing ? new boolean[]{false, true, true, false} : new boolean[]{true, false, false, true};
        }
        float[][] corner = {{x0, y0}, {x0, y1}, {x1, y1}, {x1, y0}};
        // Arc centres step inward from each corner; start angles (y down).
        float[][] centre = {{x0 + cr, y0 + cr}, {x0 + cr, y1 - cr}, {x1 - cr, y1 - cr}, {x1 - cr, y0 + cr}};
        double[] start = {Math.PI * 1.5, Math.PI, Math.PI * 0.5, 0};
        // The hinge edge closes the loop, so start right after it.
        int first;
        if (verticalAxis) {
            first = swing ? 1 : 3;
        } else {
            first = swing ? 0 : 2;
        }
        List<float[]> out = new ArrayList<float[]>();
        for (int k = 0; k < 4; k++) {
            int c = (first + k) % 4;
            if (round[c]) {
                for (int s = 0; s <= CORNER_SEGMENTS; s++) {
                    // Counter clockwise on screen is decreasing angle with y down.
                    double a = start[c] - s * (Math.PI / 2) / CORNER_SEGMENTS;
                    float nx = (float) Math.cos(a);
                    float ny = (float) Math.sin(a);
                    out.add(new float[]{centre[c][0] + nx * cr, centre[c][1] + ny * cr, nx, ny});
                }
            } else {
                // A square hinge corner takes the normal of its other edge.
                float nx = 0;
                float ny = 0;
                if (verticalAxis) {
                    ny = c == 0 || c == 3 ? -1 : 1;
                } else {
                    nx = c == 0 || c == 1 ? -1 : 1;
                }
                out.add(new float[]{corner[c][0], corner[c][1], nx, ny});
            }
        }
        return out;
    }

    /// The front (facing +z at rest, z = 0) or back (facing -z, at depth
    /// -`depth`) of one half, a fan from its centre. The front samples its
    /// own part of the front image; the back the whole back image, mirrored
    /// along the folding axis so it reads the right way round once the half
    /// has turned over onto the other.
    /// True while drawing a plain back plate, which has square texture
    /// corners and so keeps the rounded outline as geometry.
    private boolean plateFace;

    private void face(Material mat, List<float[]> ol, boolean swing, boolean front, float depth) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (float[] p : ol) {
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        float z = front ? 0 : -depth;
        float nz = front ? 1 : -1;
        if (mat.getTexture() == null || plateFace) {
            int n = ol.size();
            float[][] c = new float[n + 1][];
            for (int i = 0; i < n; i++) {
                c[i] = vertex(ol.get(i)[0], ol.get(i)[1], z, front, minX, minY, maxX, maxY);
            }
            c[n] = vertex((minX + maxX) / 2, (minY + maxY) / 2, z, front, minX, minY, maxX, maxY);
            int[] order = new int[n * 3];
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                order[i * 3] = n;
                order[i * 3 + 1] = front ? i : j;
                order[i * 3 + 2] = front ? j : i;
            }
            emit(mat, c, order, null, 0, 0, nz, swing);
            return;
        }
        // One quad over the half's bounds, not a fan along its rounded
        // outline: the corners are the texture's own transparency (the skin
        // art is rounded), and a fan of forty sliver triangles cost the
        // rasterizer many times the face's area every frame.
        float[][] c = {
            vertex(minX, minY, z, front, minX, minY, maxX, maxY),
            vertex(minX, maxY, z, front, minX, minY, maxX, maxY),
            vertex(maxX, maxY, z, front, minX, minY, maxX, maxY),
            vertex(maxX, minY, z, front, minX, minY, maxX, maxY)
        };
        // Counter clockwise on screen (y down) for the front; the back winds
        // the other way so culling keeps it only when turned over.
        int[] order = front ? new int[]{0, 1, 2, 0, 2, 3} : new int[]{0, 2, 1, 0, 3, 2};
        emit(mat, c, order, null, 0, 0, nz, swing);
    }

    private float[] vertex(float x, float y, float z, boolean front, float minX, float minY,
            float maxX, float maxY) {
        float u;
        float v;
        if (front) {
            u = x / w;
            v = y / h;
        } else if (verticalAxis) {
            u = 1 - (x - minX) / (maxX - minX);
            v = (y - minY) / (maxY - minY);
        } else {
            u = (x - minX) / (maxX - minX);
            v = 1 - (y - minY) / (maxY - minY);
        }
        return new float[]{x, y, z, u, v};
    }

    /// The side walls of one half: a band between the front outline at z = 0
    /// and the back at z = -thick, along every edge but the hinge, with
    /// smooth normals so the rounded corners catch the light like metal.
    private void walls(Material mat, List<float[]> ol, boolean swing, float thick) {
        int n = ol.size();
        int segs = n - 1;
        float[][] c = new float[segs * 4][];
        float[][] normals = new float[segs * 4][];
        int[] order = new int[segs * 12];
        for (int i = 0; i < segs; i++) {
            float[] a = ol.get(i);
            float[] b = ol.get(i + 1);
            int o = i * 4;
            c[o] = new float[]{a[0], a[1], 0, 0, 0};
            c[o + 1] = new float[]{a[0], a[1], -thick, 0, 0};
            c[o + 2] = new float[]{b[0], b[1], -thick, 0, 0};
            c[o + 3] = new float[]{b[0], b[1], 0, 0, 0};
            // Pixel y runs down, world y up.
            normals[o] = new float[]{a[2], -a[3], 0};
            normals[o + 1] = normals[o];
            normals[o + 2] = new float[]{b[2], -b[3], 0};
            normals[o + 3] = normals[o + 2];
            int q = i * 12;
            int[] tri = {o, o + 1, o + 2, o, o + 2, o + 3};
            for (int k = 0; k < 6; k++) {
                // Both windings: a wall is seen from outside on whichever
                // side it lies, and the far ones hide behind the faces.
                order[q + k] = tri[k];
                order[q + 6 + k] = tri[5 - k];
            }
        }
        emit(mat, c, order, normals, 0, 0, 0, swing);
    }

    /// The spine: a half cylinder about the hinge line, from the fixed
    /// half's back edge to the swung half's, so the outside of the fold
    /// stays closed as the halves part.
    private void spine(Material mat, float thick) {
        float len0 = 0;
        float len1 = verticalAxis ? h : w;
        int n = SPINE_SEGMENTS;
        float[][] c = new float[(n + 1) * 2][];
        float[][] normals = new float[(n + 1) * 2][];
        for (int s = 0; s <= n; s++) {
            double a = phi * s / n;
            float d = (float) (thick * Math.sin(a));
            float z = (float) (-thick * Math.cos(a));
            // Outward: away from the hinge line, in hinge-local terms.
            float nd = (float) Math.sin(a);
            float nz = (float) -Math.cos(a);
            if (verticalAxis) {
                c[s * 2] = new float[]{split + d, len0, z, 0, 0};
                c[s * 2 + 1] = new float[]{split + d, len1, z, 0, 0};
                normals[s * 2] = new float[]{nd, 0, nz};
            } else {
                c[s * 2] = new float[]{len0, split + d, z, 0, 0};
                c[s * 2 + 1] = new float[]{len1, split + d, z, 0, 0};
                normals[s * 2] = new float[]{0, -nd, nz};
            }
            normals[s * 2 + 1] = normals[s * 2];
        }
        int[] order = new int[n * 12];
        for (int s = 0; s < n; s++) {
            int a = s * 2;
            int q = s * 12;
            int[] tri = {a, a + 1, a + 3, a, a + 3, a + 2};
            for (int k = 0; k < 6; k++) {
                order[q + k] = tri[k];
                // Both windings: which one faces out depends on the axis.
                order[q + 6 + k] = tri[5 - k];
            }
        }
        // Not swung again: its points are already placed about the hinge.
        emit(mat, c, order, normals, 0, 0, 0, false);
    }

    /// Transforms one point given in front pixels (y down) plus depth into
    /// world space (origin at the device centre, y up): the swing about the
    /// hinge, the turn of the whole device, its roll in the screen plane and
    /// its tip toward the viewer, then the fit.
    private float[] transform(float x, float y, float z, boolean swing) {
        float[] p = {x - w / 2f, h / 2f - y, z};
        float[] n = {0, 0, 0};
        move(p, n, swing);
        p[0] *= fit;
        p[1] *= fit;
        p[2] *= fit;
        return p;
    }

    private void move(float[] p, float[] n, boolean swing) {
        if (swing) {
            hinge(p, n, (float) Math.cos(phi), (float) Math.sin(phi));
        }
        if (bodyTurn != 0) {
            hinge(p, n, (float) Math.cos(bodyTurn), (float) Math.sin(bodyTurn));
        }
        if (roll != 0) {
            float c = (float) Math.cos(roll);
            float s = (float) Math.sin(roll);
            float x = p[0] * c - p[1] * s;
            p[1] = p[0] * s + p[1] * c;
            p[0] = x;
            float nx = n[0] * c - n[1] * s;
            n[1] = n[0] * s + n[1] * c;
            n[0] = nx;
        }
        if (tilt != 0) {
            float c = (float) Math.cos(tilt);
            float s = (float) Math.sin(tilt);
            float y = p[1] * c - p[2] * s;
            p[2] = p[1] * s + p[2] * c;
            p[1] = y;
            float ny = n[1] * c - n[2] * s;
            n[2] = n[1] * s + n[2] * c;
            n[1] = ny;
        }
    }

    private void emit(Material mat, float[][] c, int[] order, float[][] normals, float nx0, float ny0,
            float nz0, boolean swing) {
        float[] data = new float[order.length * 8];
        for (int i = 0; i < order.length; i++) {
            float[] v = c[order[i]];
            float[] p = {v[0] - w / 2f, h / 2f - v[1], v[2]};
            float[] nn = normals != null ? normals[order[i]] : null;
            float[] n = nn != null ? new float[]{nn[0], nn[1], nn[2]} : new float[]{nx0, ny0, nz0};
            move(p, n, swing);
            int o = i * 8;
            data[o] = p[0] * fit;
            data[o + 1] = p[1] * fit;
            data[o + 2] = p[2] * fit;
            data[o + 3] = n[0];
            data[o + 4] = n[1];
            data[o + 5] = n[2];
            data[o + 6] = v[3];
            data[o + 7] = v[4];
        }
        VertexBuffer vb = new VertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, order.length);
        vb.setData(data);
        frameBuffers.add(vb);
        device.draw(new Mesh(vb, PrimitiveType.TRIANGLES), mat, null);
    }

    /// Rotates a point and its normal about the hinge line, so that a
    /// positive angle swings the second half toward the viewer (+z).
    private void hinge(float[] p, float[] n, float cos, float sin) {
        if (verticalAxis) {
            float hx = split - w / 2f;
            float dx = p[0] - hx;
            float x = hx + dx * cos - p[2] * sin;
            p[2] = dx * sin + p[2] * cos;
            p[0] = x;
            float nx = n[0] * cos - n[2] * sin;
            n[2] = n[0] * sin + n[2] * cos;
            n[0] = nx;
        } else {
            float hy = h / 2f - split;
            float dy = p[1] - hy;
            float y = hy + dy * cos + p[2] * sin;
            p[2] = -dy * sin + p[2] * cos;
            p[1] = y;
            float ny = n[1] * cos + n[2] * sin;
            n[2] = -n[1] * sin + n[2] * cos;
            n[1] = ny;
        }
    }

    /// A soft shadow under the device, cast from its own silhouette: the
    /// rendered alpha, shrunk eight times and stretched back (which blurs
    /// it), offset away from the key light and drawn beneath the device.
    private BufferedImage withShadow(BufferedImage img, float fold, float span) {
        int fw = img.getWidth();
        int fh = img.getHeight();
        int sw = Math.max(1, fw / 8);
        int sh = Math.max(1, fh / 8);
        if (shadowSmall == null || shadowSmall.getWidth() != sw || shadowSmall.getHeight() != sh) {
            shadowSmall = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D sg = shadowSmall.createGraphics();
        sg.setComposite(AlphaComposite.Clear);
        sg.fillRect(0, 0, sw, sh);
        sg.setComposite(AlphaComposite.SrcOver);
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(img, 0, 0, sw, sh, null);
        // Keep the coverage, paint it black.
        sg.setComposite(AlphaComposite.SrcIn);
        sg.setColor(Color.BLACK);
        sg.fillRect(0, 0, sw, sh);
        sg.dispose();
        // Sampled down, a diagonal silhouette edge comes out stepped; two
        // box passes turn the steps into a penumbra before it is stretched.
        boxBlur(shadowSmall);
        boxBlur(shadowSmall);
        // A new image every frame: the one returned is kept by the window
        // and read by the recorder on another thread, and a reused buffer
        // cleared for the next frame showed up there with its shadow missing.
        BufferedImage composite = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = composite.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f * fold));
        // Placed with sub-pixel precision: offsets rounded to whole pixels,
        // stretched from a mask an eighth the size, made the shadow step and
        // shimmer while the hinge moved.
        double ox = span * 0.025 * fold;
        double oy = span * 0.03 * fold;
        double grow = span * 0.01 * fold;
        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate(ox - grow, oy - grow);
        at.scale((fw + grow * 2) / sw, (fh + grow * 2) / sh);
        g.drawImage(shadowSmall, at, null);
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return composite;
    }

    /// A 5x5 box blur of the alpha channel, in place (the colour is black).
    private static void boxBlur(BufferedImage img) {
        int iw = img.getWidth();
        int ih = img.getHeight();
        int[] a = new int[iw * ih];
        int[] px = img.getRGB(0, 0, iw, ih, null, 0, iw);
        for (int i = 0; i < a.length; i++) {
            a[i] = px[i] >>> 24;
        }
        int[] tmp = new int[a.length];
        for (int y = 0; y < ih; y++) {
            for (int x = 0; x < iw; x++) {
                int sum = 0;
                for (int k = -2; k <= 2; k++) {
                    int xx = Math.max(0, Math.min(iw - 1, x + k));
                    sum += a[y * iw + xx];
                }
                tmp[y * iw + x] = sum / 5;
            }
        }
        for (int y = 0; y < ih; y++) {
            for (int x = 0; x < iw; x++) {
                int sum = 0;
                for (int k = -2; k <= 2; k++) {
                    int yy = Math.max(0, Math.min(ih - 1, y + k));
                    sum += tmp[yy * iw + x];
                }
                px[y * iw + x] = (sum / 5) << 24;
            }
        }
        img.setRGB(0, 0, iw, ih, px, 0, iw);
    }
}
