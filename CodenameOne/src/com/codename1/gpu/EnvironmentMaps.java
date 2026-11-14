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
package com.codename1.gpu;

import com.codename1.ui.Image;

/// Bakes the environment map that image based lighting reads
/// (`Environment.setEnvironmentMap`): what a shiny or rough surface reflects,
/// and the soft light every surface receives from all around it.
///
/// The bake runs once, on the CPU, into an ordinary RGB texture every backend
/// samples the same way -- no cube maps and no float textures, so it works on
/// the oldest GL ES and WebGL devices too. The texture is `WIDTH` texels wide
/// and holds `BANDS` equirectangular bands of `BAND_HEIGHT` texels stacked
/// top to bottom: the sharp environment, three progressively blurred copies
/// for rougher surfaces, the roughest, and last the diffuse irradiance. Colours
/// are stored at half intensity, so an environment may be up to twice as
/// bright as white.
///
/// ```java
/// import com.codename1.gpu.*;
/// import com.codename1.gpu.shaders.ScenePresets;
///
/// void light(GraphicsDevice device, ScenePresets.Preset look, Light light) {
///     Environment env = look.getEnvironment();
///     env.setEnvironmentMap(EnvironmentMaps.fromSky(device, env, light));
/// }
/// ```
///
/// Rebake when the sky changes; a bake costs a few tens of milliseconds.
public final class EnvironmentMaps {
    /// The width of the baked texture in texels.
    public static final int WIDTH = 128;

    /// The height of one band in texels.
    public static final int BAND_HEIGHT = 64;

    /// The number of bands: five roughness levels and the irradiance.
    public static final int BANDS = 6;

    // the blur of each band, in radians: 0 for the sharp copy, then rougher
    // levels, then a wide lobe standing in for the cosine of irradiance
    private static final float[] BLUR = {0f, 0.07f, 0.17f, 0.38f, 0.7f, 0.95f};

    private EnvironmentMaps() {
    }

    /// Bakes the procedural sky of an environment (`Environment.setSky`), lit
    /// by a light's sun, into an environment map texture. Below the horizon
    /// it reflects the hemisphere ground colour, or a darkened horizon when
    /// the environment has none.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device the texture is created on
    ///
    /// - `env`: the environment whose sky colours to bake
    ///
    /// - `light`: the light whose direction places the sun, or null for none
    ///
    /// #### Returns
    ///
    /// the environment map texture
    public static Texture fromSky(GraphicsDevice device, Environment env, Light light) {
        return upload(device, bake(skyRadiance(env, light)));
    }

    /// Bakes an equirectangular panorama -- an image twice as wide as it is
    /// tall whose x runs once around the horizon and whose y runs from
    /// straight up to straight down -- into an environment map texture.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device the texture is created on
    ///
    /// - `panorama`: the panorama
    ///
    /// - `intensity`: how bright the environment is; 1 keeps the image's own
    ///   colours, up to 2
    ///
    /// #### Returns
    ///
    /// the environment map texture
    public static Texture fromImage(GraphicsDevice device, Image panorama, float intensity) {
        return upload(device, bake(imageRadiance(panorama, intensity)));
    }

    private static Texture upload(GraphicsDevice device, int[] argb) {
        Texture t = device.createTexture(WIDTH, BAND_HEIGHT * BANDS, argb);
        // clamped: the texture is not a power of two, which GL ES 2 and WebGL 1
        // cannot repeat; every lookup keeps half a texel inside its band and
        // inside the seam behind the viewer
        t.setWrap(Texture.Wrap.CLAMP);
        t.setFilter(Texture.Filter.LINEAR);
        return t;
    }

    /// Bakes a radiance panorama of `WIDTH` by `BAND_HEIGHT` linear RGB
    /// triples into the band layout. Public for tests and for applications
    /// that compute their own environment.
    ///
    /// #### Parameters
    ///
    /// - `radiance`: `WIDTH * BAND_HEIGHT * 3` floats, row by row from
    ///   straight up
    ///
    /// #### Returns
    ///
    /// the ARGB pixels of the texture, `WIDTH` by `BAND_HEIGHT * BANDS`
    public static int[] bake(float[] radiance) {
        int[] out = new int[WIDTH * BAND_HEIGHT * BANDS];
        float[] tmp = new float[radiance.length];
        float[] level = new float[radiance.length];
        for (int b = 0; b < BANDS; b++) {
            if (BLUR[b] <= 0f) {
                System.arraycopy(radiance, 0, level, 0, radiance.length);
            } else {
                blurRows(radiance, tmp, BLUR[b]);
                blurColumns(tmp, level, BLUR[b]);
            }
            int base = b * WIDTH * BAND_HEIGHT;
            for (int i = 0; i < WIDTH * BAND_HEIGHT; i++) {
                out[base + i] = 0xff000000 | (channel(level[i * 3]) << 16) | (channel(level[i * 3 + 1]) << 8)
                        | channel(level[i * 3 + 2]);
            }
        }
        return out;
    }

    private static int channel(float v) {
        int c = (int) (v * 0.5f * 255f + 0.5f);
        return c < 0 ? 0 : (c > 255 ? 255 : c);
    }

    // the radians one texel spans, around and down
    private static final float TEXEL = (float) (Math.PI / BAND_HEIGHT);

    // A gaussian along each row. A row near a pole is a small circle, so the
    // same angle spans more texels there; past a full turn it wraps.
    private static void blurRows(float[] src, float[] dst, float sigma) {
        float[] w = new float[WIDTH];
        for (int y = 0; y < BAND_HEIGHT; y++) {
            double lat = Math.PI * (y + 0.5) / BAND_HEIGHT;
            float ring = (float) Math.max(Math.sin(lat), 0.05);
            float s = sigma / (TEXEL * ring);
            // 2r + 1 taps at most one full turn, so no texel is counted twice
            int r = Math.min(WIDTH / 2 - 1, (int) Math.ceil(s * 2.5f));
            float sum = 0f;
            for (int k = -r; k <= r; k++) {
                w[k + r] = (float) com.codename1.util.MathUtil.exp(-0.5 * (k * k) / (s * s));
                sum += w[k + r];
            }
            int row = y * WIDTH;
            for (int x = 0; x < WIDTH; x++) {
                float cr = 0f;
                float cg = 0f;
                float cb = 0f;
                for (int k = -r; k <= r; k++) {
                    int i = (row + ((x + k) % WIDTH + WIDTH) % WIDTH) * 3;
                    float wk = w[k + r];
                    cr += src[i] * wk;
                    cg += src[i + 1] * wk;
                    cb += src[i + 2] * wk;
                }
                int o = (row + x) * 3;
                dst[o] = cr / sum;
                dst[o + 1] = cg / sum;
                dst[o + 2] = cb / sum;
            }
        }
    }

    // A gaussian down each column, clamped at the poles.
    private static void blurColumns(float[] src, float[] dst, float sigma) {
        float s = sigma / TEXEL;
        int r = Math.min(BAND_HEIGHT, (int) Math.ceil(s * 2.5f));
        float[] w = new float[2 * r + 1];
        for (int k = -r; k <= r; k++) {
            w[k + r] = (float) com.codename1.util.MathUtil.exp(-0.5 * (k * k) / (s * s));
        }
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < BAND_HEIGHT; y++) {
                float cr = 0f;
                float cg = 0f;
                float cb = 0f;
                float sum = 0f;
                for (int k = -r; k <= r; k++) {
                    int yy = y + k;
                    if (yy < 0 || yy >= BAND_HEIGHT) {
                        continue;
                    }
                    int i = (yy * WIDTH + x) * 3;
                    float wk = w[k + r];
                    cr += src[i] * wk;
                    cg += src[i + 1] * wk;
                    cb += src[i + 2] * wk;
                    sum += wk;
                }
                int o = (y * WIDTH + x) * 3;
                dst[o] = cr / sum;
                dst[o + 1] = cg / sum;
                dst[o + 2] = cb / sum;
            }
        }
    }

    // The direction a texel of a band looks along: x around the horizon from
    // +x toward +z, y from straight up. Matches the lookup every generator
    // prints.
    static void direction(int x, int y, float[] out) {
        double phi = 2 * Math.PI * ((x + 0.5) / WIDTH - 0.5);
        double theta = Math.PI * (y + 0.5) / BAND_HEIGHT;
        double st = Math.sin(theta);
        out[0] = (float) (Math.cos(phi) * st);
        out[1] = (float) Math.cos(theta);
        out[2] = (float) (Math.sin(phi) * st);
    }

    static float[] skyRadiance(Environment env, Light light) {
        float[] zen = rgb(env.getZenithColor());
        float[] hor = rgb(env.getHorizonColor());
        float[] sun = rgb(env.getSunColor());
        float[] ground = env.isHemisphere() ? rgb(env.getGroundColor())
                : new float[] {hor[0] * 0.35f, hor[1] * 0.35f, hor[2] * 0.35f};
        float sx = 0f;
        float sy = 1f;
        float sz = 0f;
        if (light != null) {
            float len = (float) Math.sqrt(light.getDirectionX() * light.getDirectionX()
                    + light.getDirectionY() * light.getDirectionY()
                    + light.getDirectionZ() * light.getDirectionZ());
            if (len > 0f) {
                sx = -light.getDirectionX() / len;
                sy = -light.getDirectionY() / len;
                sz = -light.getDirectionZ() / len;
            }
        }
        float[] out = new float[WIDTH * BAND_HEIGHT * 3];
        float[] d = new float[3];
        for (int y = 0; y < BAND_HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                direction(x, y, d);
                float r;
                float g;
                float b;
                if (d[1] >= 0f) {
                    float t = (float) com.codename1.util.MathUtil.pow(d[1], 0.55);
                    r = hor[0] + (zen[0] - hor[0]) * t;
                    g = hor[1] + (zen[1] - hor[1]) * t;
                    b = hor[2] + (zen[2] - hor[2]) * t;
                    // overcast skies flatten toward a brighter grey
                    float c = env.getCloudCover() * 0.5f;
                    float grey = (r + g + b) / 3f * 1.1f;
                    r += (grey - r) * c;
                    g += (grey - g) * c;
                    b += (grey - b) * c;
                } else {
                    // the ground fades in just below the horizon
                    float t = Math.min(1f, -d[1] * 6f);
                    r = hor[0] + (ground[0] - hor[0]) * t;
                    g = hor[1] + (ground[1] - hor[1]) * t;
                    b = hor[2] + (ground[2] - hor[2]) * t;
                }
                float sd = Math.max(0f, d[0] * sx + d[1] * sy + d[2] * sz);
                float glow = (float) (com.codename1.util.MathUtil.pow(sd, 8.0) * 0.35 + com.codename1.util.MathUtil.pow(sd, 64.0) * 0.8);
                if (sd > 0.9990f) {
                    glow += 1.2f;
                }
                int o = (y * WIDTH + x) * 3;
                out[o] = r + sun[0] * glow;
                out[o + 1] = g + sun[1] * glow;
                out[o + 2] = b + sun[2] * glow;
            }
        }
        return out;
    }

    static float[] imageRadiance(Image panorama, float intensity) {
        int w = panorama.getWidth();
        int h = panorama.getHeight();
        int[] px = panorama.getRGB();
        float k = Math.max(0f, Math.min(2f, intensity)) / 255f;
        float[] out = new float[WIDTH * BAND_HEIGHT * 3];
        for (int y = 0; y < BAND_HEIGHT; y++) {
            int sy0 = y * h / BAND_HEIGHT;
            int sy1 = Math.max(sy0 + 1, (y + 1) * h / BAND_HEIGHT);
            for (int x = 0; x < WIDTH; x++) {
                int sx0 = x * w / WIDTH;
                int sx1 = Math.max(sx0 + 1, (x + 1) * w / WIDTH);
                // a box average, so a large panorama does not alias
                float r = 0f;
                float g = 0f;
                float b = 0f;
                int n = 0;
                for (int sy = sy0; sy < sy1 && sy < h; sy++) {
                    for (int sx = sx0; sx < sx1 && sx < w; sx++) {
                        int c = px[sy * w + sx];
                        r += (c >> 16) & 0xff;
                        g += (c >> 8) & 0xff;
                        b += c & 0xff;
                        n++;
                    }
                }
                int o = (y * WIDTH + x) * 3;
                float s = n == 0 ? 0f : k / n;
                out[o] = r * s;
                out[o + 1] = g * s;
                out[o + 2] = b * s;
            }
        }
        return out;
    }

    private static float[] rgb(int argb) {
        return new float[] {((argb >> 16) & 0xff) / 255f, ((argb >> 8) & 0xff) / 255f, (argb & 0xff) / 255f};
    }
}
