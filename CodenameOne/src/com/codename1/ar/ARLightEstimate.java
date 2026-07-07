/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ar;

/// An estimate of the real-world lighting around the device, used to shade
/// virtual content so it blends with the camera image. Poll it through
/// `ARSession#getLightEstimate()`; light changes every frame, so no events are
/// fired for it.
///
/// The ambient intensity is normalized so `1.0` means neutral indoor lighting:
/// platform backends map their native scale to this convention (ARKit reports
/// about 1000 lumens in a neutral room, ARCore reports a mean pixel intensity
/// of about 0.18 - both map to `1.0`). Values below `1.0` mean the scene is
/// darker than neutral, values above mean brighter.
public final class ARLightEstimate {
    /// An invalid estimate with neutral values, returned before the platform
    /// produces its first real estimate.
    public static final ARLightEstimate INVALID =
            new ARLightEstimate(false, 1.0f, 1.0f, 1.0f, 1.0f);

    private final boolean valid;
    private final float ambientIntensity;
    private final float colorR;
    private final float colorG;
    private final float colorB;

    /// Creates a light estimate. Intended for platform implementations and
    /// tests; applications receive estimates from the session.
    ///
    /// #### Parameters
    ///
    /// - `valid`: whether the platform produced a real estimate
    ///
    /// - `ambientIntensity`: normalized intensity, `1.0` is neutral
    ///
    /// - `colorR`, `colorG`, `colorB`: per-channel color correction scale
    ///   factors, `1.0` each is neutral white light
    public ARLightEstimate(boolean valid, float ambientIntensity,
                           float colorR, float colorG, float colorB) {
        this.valid = valid;
        this.ambientIntensity = ambientIntensity;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
    }

    /// True when the platform produced a real estimate; false for the
    /// placeholder returned before tracking starts.
    public boolean isValid() {
        return valid;
    }

    /// The normalized ambient light intensity. `1.0` is neutral indoor
    /// lighting, lower is darker, higher is brighter.
    public float getAmbientIntensity() {
        return ambientIntensity;
    }

    /// The per-channel color correction as a newly allocated `{r, g, b}` array
    /// of scale factors. `{1, 1, 1}` is neutral white light; multiply your
    /// content's color by these factors to match the scene's color cast.
    public float[] getColorCorrection() {
        return new float[]{colorR, colorG, colorB};
    }
}
