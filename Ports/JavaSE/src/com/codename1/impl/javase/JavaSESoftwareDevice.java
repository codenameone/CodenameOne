/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GpuCapabilities;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexAttribute;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;
import com.codename1.ui.Image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/// Pure Java software rasterizer that implements the Codename One 3D
/// `GraphicsDevice` for the JavaSE simulator. It renders into a `BufferedImage`
/// with a floating point depth buffer and shades each fragment according to the
/// material's lighting model. Choosing a software renderer here (rather than a
/// native GL binding) keeps the simulator dependency free and makes 3D
/// screenshots fully deterministic across machines and headless CI. The native
/// GPU backends (OpenGL ES, WebGL, Metal) are used on the respective devices.
class JavaSESoftwareDevice extends GraphicsDevice {
    private static final class TexData {
        final int w;
        final int h;
        final int[] pixels;

        TexData(int w, int h, int[] pixels) {
            this.w = w;
            this.h = h;
            this.pixels = pixels;
        }
    }

    private int width;
    private int height;
    private int[] color;
    private float[] depth;
    private BufferedImage image;
    private int vpX;
    private int vpY;
    private int vpW;
    private int vpH;

    private final GpuCapabilities caps = new GpuCapabilities(
            4096, 8, false, false, true, "Codename One Software Rasterizer (JavaSE)");

    private final float[] mvp = new float[16];
    private final float[] normalMatrix = new float[16];

    void resize(int w, int h) {
        if (w <= 0) {
            w = 1;
        }
        if (h <= 0) {
            h = 1;
        }
        if (image != null && width == w && height == h) {
            return;
        }
        width = w;
        height = h;
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        color = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        depth = new float[w * h];
        vpX = 0;
        vpY = 0;
        vpW = w;
        vpH = h;
    }

    BufferedImage getImage() {
        return image;
    }

    public GpuCapabilities getCapabilities() {
        return caps;
    }

    public Texture createTexture(Image img) {
        int w = img.getWidth();
        int h = img.getHeight();
        return createTexture(w, h, img.getRGB());
    }

    public Texture createTexture(int w, int h, int[] argb) {
        Texture t = new Texture(w, h);
        int[] copy = new int[w * h];
        System.arraycopy(argb, 0, copy, 0, Math.min(argb.length, copy.length));
        t.setHandle(new TexData(w, h, copy));
        return t;
    }

    public void clear(int argbColor, boolean clearColor, boolean clearDepth) {
        if (clearColor && color != null) {
            for (int i = 0; i < color.length; i++) {
                color[i] = argbColor;
            }
        }
        if (clearDepth && depth != null) {
            for (int i = 0; i < depth.length; i++) {
                depth[i] = Float.POSITIVE_INFINITY;
            }
        }
    }

    public void setViewport(int x, int y, int w, int h) {
        vpX = x;
        vpY = y;
        vpW = w;
        vpH = h;
    }

    public void draw(Mesh mesh, Material material, float[] modelMatrix) {
        if (image == null) {
            return;
        }
        PrimitiveType type = mesh.getPrimitiveType();
        if (type != PrimitiveType.TRIANGLES && type != PrimitiveType.TRIANGLE_STRIP) {
            // Lines and points are not rasterized by the software backend.
            return;
        }
        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        Camera cam = getCamera();
        float[] vp = cam != null ? cam.getViewProjection() : Matrix4.identity();
        Matrix4.multiply(vp, model, mvp);
        float[] nm = Matrix4.normalMatrix(model);
        for (int i = 0; i < 16; i++) {
            normalMatrix[i] = nm[i];
        }

        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();
        float[] data = vb.getData();
        int stride = fmt.getFloatsPerVertex();
        int posOff = offsetOf(fmt, VertexAttribute.Usage.POSITION);
        int normOff = offsetOf(fmt, VertexAttribute.Usage.NORMAL);
        int uvOff = offsetOf(fmt, VertexAttribute.Usage.TEXCOORD);

        int[] indices;
        int count;
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            short[] sd = ib.getData();
            count = ib.getIndexCount();
            indices = null;
            rasterizeIndexed(sd, count, type, material, model,
                    data, stride, posOff, normOff, uvOff);
        } else {
            count = vb.getVertexCount();
            rasterizeSequential(count, type, material, model,
                    data, stride, posOff, normOff, uvOff);
        }
    }

    private void rasterizeIndexed(short[] sd, int count, PrimitiveType type, Material material,
                                  float[] model, float[] data, int stride,
                                  int posOff, int normOff, int uvOff) {
        if (type == PrimitiveType.TRIANGLES) {
            for (int i = 0; i + 2 < count; i += 3) {
                tri(sd[i] & 0xffff, sd[i + 1] & 0xffff, sd[i + 2] & 0xffff,
                        material, model, data, stride, posOff, normOff, uvOff);
            }
        } else {
            for (int i = 0; i + 2 < count; i++) {
                int a = sd[i] & 0xffff;
                int b = sd[i + 1] & 0xffff;
                int c = sd[i + 2] & 0xffff;
                if ((i & 1) == 0) {
                    tri(a, b, c, material, model, data, stride, posOff, normOff, uvOff);
                } else {
                    tri(b, a, c, material, model, data, stride, posOff, normOff, uvOff);
                }
            }
        }
    }

    private void rasterizeSequential(int count, PrimitiveType type, Material material,
                                     float[] model, float[] data, int stride,
                                     int posOff, int normOff, int uvOff) {
        if (type == PrimitiveType.TRIANGLES) {
            for (int i = 0; i + 2 < count; i += 3) {
                tri(i, i + 1, i + 2, material, model, data, stride, posOff, normOff, uvOff);
            }
        } else {
            for (int i = 0; i + 2 < count; i++) {
                if ((i & 1) == 0) {
                    tri(i, i + 1, i + 2, material, model, data, stride, posOff, normOff, uvOff);
                } else {
                    tri(i + 1, i, i + 2, material, model, data, stride, posOff, normOff, uvOff);
                }
            }
        }
    }

    // Scratch per-vertex working storage for the three triangle corners.
    private final float[][] clip = new float[3][4];
    private final float[][] world = new float[3][3];
    private final float[][] norm = new float[3][3];
    private final float[][] uv = new float[3][2];
    private final float[] sx = new float[3];
    private final float[] sy = new float[3];
    private final float[] sz = new float[3];
    private final float[] iw = new float[3];

    private void tri(int i0, int i1, int i2, Material material, float[] model,
                     float[] data, int stride, int posOff, int normOff, int uvOff) {
        loadVertex(0, i0, data, stride, posOff, normOff, uvOff, model);
        loadVertex(1, i1, data, stride, posOff, normOff, uvOff, model);
        loadVertex(2, i2, data, stride, posOff, normOff, uvOff, model);

        // Reject triangles that cross or sit behind the camera plane; the
        // software backend does not clip against the near plane.
        for (int v = 0; v < 3; v++) {
            if (clip[v][3] <= 0.0001f) {
                return;
            }
        }

        for (int v = 0; v < 3; v++) {
            float w = clip[v][3];
            iw[v] = 1.0f / w;
            float ndcx = clip[v][0] * iw[v];
            float ndcy = clip[v][1] * iw[v];
            float ndcz = clip[v][2] * iw[v];
            sx[v] = vpX + (ndcx * 0.5f + 0.5f) * vpW;
            sy[v] = vpY + (1.0f - (ndcy * 0.5f + 0.5f)) * vpH;
            sz[v] = ndcz * 0.5f + 0.5f;
        }

        float area = (sx[1] - sx[0]) * (sy[2] - sy[0]) - (sx[2] - sx[0]) * (sy[1] - sy[0]);
        if (area == 0.0f) {
            return;
        }
        RenderState rs = material.getRenderState();
        boolean frontFacing = area < 0.0f;
        RenderState.CullMode cull = rs.getCullMode();
        if (cull == RenderState.CullMode.BACK && !frontFacing) {
            return;
        }
        if (cull == RenderState.CullMode.FRONT && frontFacing) {
            return;
        }

        int minX = (int) Math.floor(Math.min(sx[0], Math.min(sx[1], sx[2])));
        int maxX = (int) Math.ceil(Math.max(sx[0], Math.max(sx[1], sx[2])));
        int minY = (int) Math.floor(Math.min(sy[0], Math.min(sy[1], sy[2])));
        int maxY = (int) Math.ceil(Math.max(sy[0], Math.max(sy[1], sy[2])));
        if (minX < 0) {
            minX = 0;
        }
        if (minY < 0) {
            minY = 0;
        }
        if (maxX > width) {
            maxX = width;
        }
        if (maxY > height) {
            maxY = height;
        }

        float invArea = 1.0f / area;
        boolean lit = material.getType() == Material.Type.LAMBERT
                || material.getType() == Material.Type.PHONG;
        boolean phong = material.getType() == Material.Type.PHONG;
        boolean blend = rs.getBlendMode() == RenderState.BlendMode.ALPHA
                || rs.getBlendMode() == RenderState.BlendMode.ADDITIVE;
        boolean additive = rs.getBlendMode() == RenderState.BlendMode.ADDITIVE;
        TexData tex = material.getTexture() != null
                ? (TexData) material.getTexture().getHandle() : null;
        boolean bilinear = material.getTexture() != null
                && material.getTexture().getFilter() == Texture.Filter.LINEAR;
        boolean repeat = material.getTexture() != null
                && material.getTexture().getWrap() == Texture.Wrap.REPEAT;

        Light light = getLight();
        float lx = -light.getDirectionX();
        float ly = -light.getDirectionY();
        float lz = -light.getDirectionZ();
        float ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (ll > 0) {
            lx /= ll;
            ly /= ll;
            lz /= ll;
        }
        float lr = ((light.getColor() >> 16) & 0xff) / 255.0f;
        float lg = ((light.getColor() >> 8) & 0xff) / 255.0f;
        float lb = (light.getColor() & 0xff) / 255.0f;
        float ar = ((light.getAmbientColor() >> 16) & 0xff) / 255.0f;
        float ag = ((light.getAmbientColor() >> 8) & 0xff) / 255.0f;
        float ab = (light.getAmbientColor() & 0xff) / 255.0f;

        Camera cam = getCamera();
        float eyeX = cam != null ? cam.getEyeX() : 0;
        float eyeY = cam != null ? cam.getEyeY() : 0;
        float eyeZ = cam != null ? cam.getEyeZ() : 0;

        int mcA = (material.getColor() >>> 24) & 0xff;
        float mcR = ((material.getColor() >> 16) & 0xff) / 255.0f;
        float mcG = ((material.getColor() >> 8) & 0xff) / 255.0f;
        float mcB = (material.getColor() & 0xff) / 255.0f;
        float mcAf = mcA / 255.0f;
        float shininess = material.getShininess();

        for (int y = minY; y < maxY; y++) {
            float py = y + 0.5f;
            for (int x = minX; x < maxX; x++) {
                float px = x + 0.5f;
                float w0 = edge(sx[1], sy[1], sx[2], sy[2], px, py) * invArea;
                float w1 = edge(sx[2], sy[2], sx[0], sy[0], px, py) * invArea;
                float w2 = edge(sx[0], sy[0], sx[1], sy[1], px, py) * invArea;
                if (w0 < 0 || w1 < 0 || w2 < 0) {
                    continue;
                }
                float z = w0 * sz[0] + w1 * sz[1] + w2 * sz[2];
                int di = y * width + x;
                if (rs.isDepthTest() && z >= depth[di]) {
                    continue;
                }

                float pw = w0 * iw[0] + w1 * iw[1] + w2 * iw[2];
                float invPw = 1.0f / pw;
                float b0 = w0 * iw[0] * invPw;
                float b1 = w1 * iw[1] * invPw;
                float b2 = w2 * iw[2] * invPw;

                float r = mcR;
                float g = mcG;
                float b = mcB;
                float a = mcAf;

                if (tex != null) {
                    float u = b0 * uv[0][0] + b1 * uv[1][0] + b2 * uv[2][0];
                    float vtex = b0 * uv[0][1] + b1 * uv[1][1] + b2 * uv[2][1];
                    int sample = sampleTexture(tex, u, vtex, bilinear, repeat);
                    float ta = ((sample >>> 24) & 0xff) / 255.0f;
                    r *= ((sample >> 16) & 0xff) / 255.0f;
                    g *= ((sample >> 8) & 0xff) / 255.0f;
                    b *= (sample & 0xff) / 255.0f;
                    a *= ta;
                }

                if (lit) {
                    float nx = b0 * norm[0][0] + b1 * norm[1][0] + b2 * norm[2][0];
                    float ny = b0 * norm[0][1] + b1 * norm[1][1] + b2 * norm[2][1];
                    float nz = b0 * norm[0][2] + b1 * norm[1][2] + b2 * norm[2][2];
                    float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (nl > 0) {
                        nx /= nl;
                        ny /= nl;
                        nz /= nl;
                    }
                    float ndotl = nx * lx + ny * ly + nz * lz;
                    if (ndotl < 0) {
                        ndotl = 0;
                    }
                    float litR = ar + lr * ndotl;
                    float litG = ag + lg * ndotl;
                    float litB = ab + lb * ndotl;
                    r *= litR;
                    g *= litG;
                    b *= litB;

                    if (phong && ndotl > 0) {
                        float wx = b0 * world[0][0] + b1 * world[1][0] + b2 * world[2][0];
                        float wy = b0 * world[0][1] + b1 * world[1][1] + b2 * world[2][1];
                        float wz = b0 * world[0][2] + b1 * world[1][2] + b2 * world[2][2];
                        float vx = eyeX - wx;
                        float vy = eyeY - wy;
                        float vz = eyeZ - wz;
                        float vlen = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
                        if (vlen > 0) {
                            vx /= vlen;
                            vy /= vlen;
                            vz /= vlen;
                        }
                        float hx = lx + vx;
                        float hy = ly + vy;
                        float hz = lz + vz;
                        float hlen = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
                        if (hlen > 0) {
                            hx /= hlen;
                            hy /= hlen;
                            hz /= hlen;
                        }
                        float ndoth = nx * hx + ny * hy + nz * hz;
                        if (ndoth > 0) {
                            float spec = (float) Math.pow(ndoth, shininess);
                            r += lr * spec;
                            g += lg * spec;
                            b += lb * spec;
                        }
                    }
                }

                int out = packColor(r, g, b, a);
                if (blend) {
                    out = blendPixel(color[di], out, additive, a);
                } else if (a < 1.0f && material.getType() != Material.Type.UNLIT) {
                    // opaque pipeline ignores alpha
                    out = packColor(r, g, b, 1.0f);
                }
                color[di] = out;
                if (rs.isDepthWrite()) {
                    depth[di] = z;
                }
            }
        }
    }

    private void loadVertex(int slot, int idx, float[] data, int stride,
                            int posOff, int normOff, int uvOff, float[] model) {
        int base = idx * stride;
        float ox = data[base + posOff];
        float oy = data[base + posOff + 1];
        float oz = data[base + posOff + 2];
        // clip = mvp * position
        clip[slot][0] = mvp[0] * ox + mvp[4] * oy + mvp[8] * oz + mvp[12];
        clip[slot][1] = mvp[1] * ox + mvp[5] * oy + mvp[9] * oz + mvp[13];
        clip[slot][2] = mvp[2] * ox + mvp[6] * oy + mvp[10] * oz + mvp[14];
        clip[slot][3] = mvp[3] * ox + mvp[7] * oy + mvp[11] * oz + mvp[15];
        // world position = model * position
        world[slot][0] = model[0] * ox + model[4] * oy + model[8] * oz + model[12];
        world[slot][1] = model[1] * ox + model[5] * oy + model[9] * oz + model[13];
        world[slot][2] = model[2] * ox + model[6] * oy + model[10] * oz + model[14];
        if (normOff >= 0) {
            float nx = data[base + normOff];
            float ny = data[base + normOff + 1];
            float nz = data[base + normOff + 2];
            norm[slot][0] = normalMatrix[0] * nx + normalMatrix[4] * ny + normalMatrix[8] * nz;
            norm[slot][1] = normalMatrix[1] * nx + normalMatrix[5] * ny + normalMatrix[9] * nz;
            norm[slot][2] = normalMatrix[2] * nx + normalMatrix[6] * ny + normalMatrix[10] * nz;
        } else {
            norm[slot][0] = 0;
            norm[slot][1] = 0;
            norm[slot][2] = 1;
        }
        if (uvOff >= 0) {
            uv[slot][0] = data[base + uvOff];
            uv[slot][1] = data[base + uvOff + 1];
        } else {
            uv[slot][0] = 0;
            uv[slot][1] = 0;
        }
    }

    private static float edge(float ax, float ay, float bx, float by, float px, float py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    private int sampleTexture(TexData tex, float u, float v, boolean bilinear, boolean repeat) {
        if (!bilinear) {
            int xi = wrapCoord((int) Math.floor(u * tex.w), tex.w, repeat);
            int yi = wrapCoord((int) Math.floor(v * tex.h), tex.h, repeat);
            return tex.pixels[yi * tex.w + xi];
        }
        float fx = u * tex.w - 0.5f;
        float fy = v * tex.h - 0.5f;
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        float dx = fx - x0;
        float dy = fy - y0;
        int x0c = wrapCoord(x0, tex.w, repeat);
        int x1c = wrapCoord(x0 + 1, tex.w, repeat);
        int y0c = wrapCoord(y0, tex.h, repeat);
        int y1c = wrapCoord(y0 + 1, tex.h, repeat);
        int c00 = tex.pixels[y0c * tex.w + x0c];
        int c10 = tex.pixels[y0c * tex.w + x1c];
        int c01 = tex.pixels[y1c * tex.w + x0c];
        int c11 = tex.pixels[y1c * tex.w + x1c];
        return lerpColor(lerpColor(c00, c10, dx), lerpColor(c01, c11, dx), dy);
    }

    private static int wrapCoord(int c, int size, boolean repeat) {
        if (repeat) {
            c %= size;
            if (c < 0) {
                c += size;
            }
            return c;
        }
        if (c < 0) {
            return 0;
        }
        if (c >= size) {
            return size - 1;
        }
        return c;
    }

    private static int lerpColor(int c0, int c1, float t) {
        int a = (int) (((c0 >>> 24) & 0xff) + (((c1 >>> 24) & 0xff) - ((c0 >>> 24) & 0xff)) * t);
        int r = (int) (((c0 >> 16) & 0xff) + (((c1 >> 16) & 0xff) - ((c0 >> 16) & 0xff)) * t);
        int g = (int) (((c0 >> 8) & 0xff) + (((c1 >> 8) & 0xff) - ((c0 >> 8) & 0xff)) * t);
        int b = (int) ((c0 & 0xff) + ((c1 & 0xff) - (c0 & 0xff)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int packColor(float r, float g, float b, float a) {
        int ri = clamp255((int) (r * 255.0f + 0.5f));
        int gi = clamp255((int) (g * 255.0f + 0.5f));
        int bi = clamp255((int) (b * 255.0f + 0.5f));
        int ai = clamp255((int) (a * 255.0f + 0.5f));
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static int blendPixel(int dst, int src, boolean additive, float srcAlpha) {
        int sr = (src >> 16) & 0xff;
        int sg = (src >> 8) & 0xff;
        int sb = src & 0xff;
        int dr = (dst >> 16) & 0xff;
        int dg = (dst >> 8) & 0xff;
        int db = dst & 0xff;
        if (additive) {
            return (0xff << 24)
                    | (clamp255(dr + sr) << 16)
                    | (clamp255(dg + sg) << 8)
                    | clamp255(db + sb);
        }
        float sa = srcAlpha;
        float ia = 1.0f - sa;
        int rr = clamp255((int) (sr * sa + dr * ia + 0.5f));
        int rg = clamp255((int) (sg * sa + dg * ia + 0.5f));
        int rb = clamp255((int) (sb * sa + db * ia + 0.5f));
        return (0xff << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static int clamp255(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > 255) {
            return 255;
        }
        return v;
    }

    private static int offsetOf(VertexFormat fmt, VertexAttribute.Usage usage) {
        for (int i = 0; i < fmt.getAttributeCount(); i++) {
            if (fmt.getAttribute(i).getUsage() == usage) {
                return fmt.getAttributeOffset(i);
            }
        }
        return -1;
    }

    public void dispose(VertexBuffer buffer) {
        buffer.setHandle(null);
    }

    public void dispose(IndexBuffer buffer) {
        buffer.setHandle(null);
    }

    public void dispose(Texture texture) {
        texture.setHandle(null);
    }
}
