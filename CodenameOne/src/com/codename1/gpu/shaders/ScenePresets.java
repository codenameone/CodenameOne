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
package com.codename1.gpu.shaders;

import com.codename1.gpu.Environment;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.shader.ShaderMaterial;

/// Complete looks: a sun, the sky's light, fog, tone mapping, post effects
/// and a matching `Sky` dome, tuned to work together. Start from one and
/// adjust; every part is an ordinary object you can change.
///
/// ```java
/// ScenePresets.Preset look = ScenePresets.harbourDusk();
/// look.apply(device);
/// Material dome = new Material(Material.Type.UNLIT).setShader(look.getSky()).setFogged(false);
/// // per frame, after drawing the scene:
/// device.beginPostProcess(look.getPost(), w, h);
/// ```
public final class ScenePresets {
    private ScenePresets() {
    }

    /// A light, environment, post chain and sky dome that belong together.
    public static final class Preset {
        private final Light light;
        private final Environment environment;
        private final PostProcess post;
        private final ShaderMaterial sky;
        private final int clearColor;

        Preset(Light light, Environment environment, PostProcess post, ShaderMaterial sky, int clearColor) {
            this.light = light;
            this.environment = environment;
            this.post = post;
            this.sky = sky;
            this.clearColor = clearColor;
        }

        /// The sun.
        ///
        /// #### Returns
        ///
        /// the light
        public Light getLight() {
            return light;
        }

        /// The fog, sky light and tone mapping.
        ///
        /// #### Returns
        ///
        /// the environment
        public Environment getEnvironment() {
            return environment;
        }

        /// Bloom, grade, vignette and grain.
        ///
        /// #### Returns
        ///
        /// the post chain
        public PostProcess getPost() {
            return post;
        }

        /// A `Sky.gradient` dome material set to match.
        ///
        /// #### Returns
        ///
        /// the sky shader
        public ShaderMaterial getSky() {
            return sky;
        }

        /// The colour to clear to, the horizon's, for scenes without a dome.
        ///
        /// #### Returns
        ///
        /// an ARGB colour
        public int getClearColor() {
            return clearColor;
        }

        /// Installs the light and the environment on a device.
        ///
        /// #### Parameters
        ///
        /// - `device`: the device to draw with
        public void apply(GraphicsDevice device) {
            device.setLight(light);
            device.setEnvironment(environment);
        }
    }

    private static Light sun(float x, float y, float z, int color, int ambient, float softness) {
        return new Light().setDirection(x, y, z).setColor(color).setAmbientColor(ambient).setShadows(true)
                .setShadowMap(2048, 40f).setShadowStyle(0.75f, 0.0015f).setShadowSoftness(softness);
    }

    /// Golden hour by the sea: a low warm sun across the water, a deep blue
    /// sky lighting the shadows, light haze, and a soft bloom on the
    /// highlights.
    ///
    /// #### Returns
    ///
    /// a new preset
    public static Preset harbourDusk() {
        Light l = sun(-0.75f, -0.32f, -0.55f, 0xffffc49a, 0xff2a3044, 2.5f);
        Environment e = new Environment().setHemisphere(0xff5a6c9c, 0xff3a2c28)
                .setFog(0xffc89a86, 0.012f, 0.08f).setToneMapping(true).setExposure(1.15f);
        PostProcess p = new PostProcess().setBloom(0.75f, 0.55f).setGrade(1.08f, 1.12f, 0xff2a3a5a, 0xffffc8a0)
                .setVignette(0.35f).setGrain(0.03f);
        return new Preset(l, e, p, Sky.dusk(Sky.gradient()), 0xfff0a46a);
    }

    /// Bright midday: a high white sun, blue sky light, crisp shadows.
    ///
    /// #### Returns
    ///
    /// a new preset
    public static Preset clearNoon() {
        Light l = sun(-0.3f, -0.9f, -0.25f, 0xfffff6e8, 0xff40464f, 1.2f);
        Environment e = new Environment().setHemisphere(0xff8fb4e0, 0xff5a5040)
                .setFog(0xffbcd8ec, 0.004f, 0.05f).setToneMapping(true).setExposure(1f);
        PostProcess p = new PostProcess().setBloom(0.9f, 0.25f).setGrade(1.05f, 1.08f, 0xff203048, 0xfffff0e0)
                .setVignette(0.2f);
        return new Preset(l, e, p, Sky.day(Sky.gradient()), 0xffbcd8ec);
    }

    /// Overcast: no hard shadows to speak of, flat grey light, thick haze.
    ///
    /// #### Returns
    ///
    /// a new preset
    public static Preset overcast() {
        Light l = sun(-0.2f, -0.95f, -0.1f, 0xff9a9ea4, 0xff70767e, 4f).setShadowStyle(0.3f, 0.0015f);
        Environment e = new Environment().setHemisphere(0xffa8aeb4, 0xff55585c)
                .setFog(0xffb4b9bd, 0.02f, 0.04f).setToneMapping(true).setExposure(1.05f);
        PostProcess p = new PostProcess().setGrade(0.95f, 0.8f, 0xff303840, 0xffe8ecf0).setVignette(0.25f);
        return new Preset(l, e, p, Sky.overcast(Sky.gradient()), 0xffb4b9bd);
    }

    /// Night: cold moonlight, deep blue shadows, strong bloom for lamps and
    /// emissive surfaces.
    ///
    /// #### Returns
    ///
    /// a new preset
    public static Preset night() {
        Light l = sun(0.4f, -0.6f, -0.5f, 0xff5a6a90, 0xff0c1222, 2f);
        Environment e = new Environment().setHemisphere(0xff1a2440, 0xff080a10)
                .setFog(0xff16233a, 0.015f, 0.06f).setToneMapping(true).setExposure(1.3f);
        PostProcess p = new PostProcess().setBloom(0.55f, 0.9f).setGrade(1.1f, 0.85f, 0xff101830, 0xffc0d0ff)
                .setVignette(0.45f).setGrain(0.05f);
        return new Preset(l, e, p, Sky.night(Sky.gradient()), 0xff16233a);
    }

    /// Desert: a harsh white sun, hot sand bouncing light into the shadows,
    /// heat haze on the horizon.
    ///
    /// #### Returns
    ///
    /// a new preset
    public static Preset desert() {
        Light l = sun(-0.4f, -0.8f, 0.3f, 0xfffff0d8, 0xff4a4036, 1.2f);
        Environment e = new Environment().setHemisphere(0xffa8c4e4, 0xffb08a5c)
                .setFog(0xffe8d4b4, 0.008f, 0.05f).setToneMapping(true).setExposure(1.05f);
        PostProcess p = new PostProcess().setBloom(0.85f, 0.3f).setGrade(1.1f, 1.05f, 0xff3a2a1a, 0xfffff0d0);
        ShaderMaterial sky = Sky.day(Sky.gradient()).setColorUniform(Sky.HORIZON, 0xe8dcc4).setUniform(Sky.CLOUDS, 0.1f);
        return new Preset(l, e, p, sky, 0xffe8d4b4);
    }

    /// Arctic: a low pale sun, blue-white snow light from everywhere, cold
    /// grade.
    ///
    /// #### Returns
    ///
    /// a new preset
    public static Preset arctic() {
        Light l = sun(-0.6f, -0.4f, -0.7f, 0xfff4f0ff, 0xff60708a, 2f);
        Environment e = new Environment().setHemisphere(0xffa8c8f0, 0xffd8e4f0)
                .setFog(0xffd8e6f2, 0.01f, 0.05f).setToneMapping(true).setExposure(0.95f);
        PostProcess p = new PostProcess().setBloom(0.85f, 0.35f).setGrade(1.05f, 0.9f, 0xff304a70, 0xfff0f4ff)
                .setVignette(0.25f);
        ShaderMaterial sky = Sky.day(Sky.gradient()).setColorUniform(Sky.HORIZON, 0xdce8f4).setUniform(Sky.CLOUDS, 0.5f);
        return new Preset(l, e, p, sky, 0xffd8e6f2);
    }
}
