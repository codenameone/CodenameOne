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

/// The atmosphere every draw is shaded in: fog, the sky and ground colours that
/// light a surface from above and below, and how the result is exposed and
/// tone mapped to the display.
///
/// A default environment changes nothing: no fog, the light's own ambient
/// colour and no tone mapping, so a scene that never sets one renders exactly
/// as before. Set it with `GraphicsDevice.setEnvironment`, or
/// `GameView.getEnvironment()` in a game.
///
/// ```java
/// GameView view = new GameView() {
///     protected void update(double deltaSeconds) {
///     }
/// };
/// view.getEnvironment()
///         .setFog(0xffa9b3ba, 0.035f, 0.08f)
///         .setHemisphere(0xff9fb4c8, 0xff4a4036)
///         .setToneMapping(true)
///         .setExposure(1.1f);
/// ```
///
/// Fog is exponential squared in distance from the eye, thinned with height by
/// `heightFalloff`, so a valley fills with haze while a rooftop stays clear. It
/// applies to every material type except `Material.Type.SKYBOX`.
public final class Environment {
    private int fogColor = 0xff808080;
    private float fogDensity;
    private float fogHeightFalloff;
    private float fogBaseHeight;
    private int skyColor;
    private int groundColor;
    private boolean hemisphere;
    private boolean toneMapping;
    private float exposure = 1f;
    private boolean sky;
    private int zenithColor;
    private int horizonColor;
    private int sunColor;
    private float cloudCover;
    private int version;

    /// Turns fog on. A density of 0 turns it off.
    ///
    /// #### Parameters
    ///
    /// - `color`: the colour of the fog, as ARGB
    ///
    /// - `density`: how thick it is, per world unit; a surface `1 / density`
    ///   units away is about 63% fog
    ///
    /// - `heightFalloff`: how quickly it thins with height above
    ///   `setFogBaseHeight`, per world unit; 0 is the same thickness everywhere
    ///
    /// #### Returns
    ///
    /// this environment for chaining
    public Environment setFog(int color, float density, float heightFalloff) {
        this.fogColor = color;
        this.fogDensity = Math.max(0f, density);
        this.fogHeightFalloff = Math.max(0f, heightFalloff);
        version++;
        return this;
    }

    /// Sets the height at which fog is at its full density; it thins above it.
    ///
    /// #### Parameters
    ///
    /// - `y`: the world height
    ///
    /// #### Returns
    ///
    /// this environment for chaining
    public Environment setFogBaseHeight(float y) {
        this.fogBaseHeight = y;
        version++;
        return this;
    }

    /// Lights surfaces with a sky colour from above and a ground colour from
    /// below, blended by the surface normal, in place of the light's flat
    /// ambient colour. It is what gives an outdoor scene its shape in shadow.
    ///
    /// #### Parameters
    ///
    /// - `sky`: the colour that lights upward facing surfaces, as ARGB
    ///
    /// - `ground`: the colour that lights downward facing surfaces, as ARGB
    ///
    /// #### Returns
    ///
    /// this environment for chaining
    public Environment setHemisphere(int sky, int ground) {
        this.skyColor = sky;
        this.groundColor = ground;
        this.hemisphere = true;
        version++;
        return this;
    }

    /// Goes back to the light's own flat ambient colour.
    ///
    /// #### Returns
    ///
    /// this environment for chaining
    public Environment clearHemisphere() {
        this.hemisphere = false;
        version++;
        return this;
    }

    /// Maps shaded colours through a filmic (ACES) curve, so bright light rolls
    /// off instead of clipping to white. Lit and physically based materials
    /// are affected; unlit ones, sprites and the sky are not.
    ///
    /// #### Parameters
    ///
    /// - `toneMapping`: true to tone map
    ///
    /// #### Returns
    ///
    /// this environment for chaining
    public Environment setToneMapping(boolean toneMapping) {
        this.toneMapping = toneMapping;
        version++;
        return this;
    }

    /// Scales the light reaching the camera before it is tone mapped.
    ///
    /// #### Parameters
    ///
    /// - `exposure`: the multiplier, 1 for none
    ///
    /// #### Returns
    ///
    /// this environment for chaining
    public Environment setExposure(float exposure) {
        this.exposure = Math.max(0f, exposure);
        version++;
        return this;
    }

    /// Paints `Material.Type.SKYBOX` surfaces as an open sky: a gradient from
    /// the horizon to the zenith, the sun where the light comes from with a
    /// glow around it, and soft clouds. Draw it on a large sphere around the
    /// camera, seen from inside; it is computed per pixel from the view
    /// direction, so it needs no texture and has no seams.
    ///
    /// #### Parameters
    ///
    /// - `zenith`: the colour straight up, as ARGB
    ///
    /// - `horizon`: the colour at the horizon, as ARGB; match it to the fog
    ///   colour so distant geometry melts into the sky
    ///
    /// - `sun`: the colour of the sun disc and its glow, as ARGB; black for none
    ///
    /// - `clouds`: how much of the sky is cloud, from 0 to 1
    ///
    /// #### Returns
    ///
    /// this environment for chaining
    public Environment setSky(int zenith, int horizon, int sun, float clouds) {
        this.zenithColor = zenith;
        this.horizonColor = horizon;
        this.sunColor = sun;
        this.cloudCover = Math.max(0f, Math.min(1f, clouds));
        this.sky = true;
        version++;
        return this;
    }

    /// Goes back to drawing `Material.Type.SKYBOX` surfaces in their own colour
    /// and texture.
    ///
    /// #### Returns
    ///
    /// this environment for chaining
    public Environment clearSky() {
        this.sky = false;
        version++;
        return this;
    }

    /// Returns true if skybox surfaces are painted as an open sky.
    public boolean isSky() {
        return sky;
    }

    /// Returns the sky colour straight up, as ARGB.
    public int getZenithColor() {
        return zenithColor;
    }

    /// Returns the sky colour at the horizon, as ARGB.
    public int getHorizonColor() {
        return horizonColor;
    }

    /// Returns the colour of the sun in the sky, as ARGB.
    public int getSunColor() {
        return sunColor;
    }

    /// Returns how much of the sky is cloud, from 0 to 1.
    public float getCloudCover() {
        return cloudCover;
    }

    /// Returns the fog colour as ARGB.
    public int getFogColor() {
        return fogColor;
    }

    /// Returns the fog density per world unit, 0 when there is no fog.
    public float getFogDensity() {
        return fogDensity;
    }

    /// Returns how quickly fog thins with height, per world unit.
    public float getFogHeightFalloff() {
        return fogHeightFalloff;
    }

    /// Returns the height at which fog is at its full density.
    public float getFogBaseHeight() {
        return fogBaseHeight;
    }

    /// Returns true if fog is on.
    public boolean isFog() {
        return fogDensity > 0f;
    }

    /// Returns true if surfaces are lit by the sky and ground colours.
    public boolean isHemisphere() {
        return hemisphere;
    }

    /// Returns the sky colour as ARGB.
    public int getSkyColor() {
        return skyColor;
    }

    /// Returns the ground colour as ARGB.
    public int getGroundColor() {
        return groundColor;
    }

    /// Returns true if shaded colours are tone mapped.
    public boolean isToneMapping() {
        return toneMapping;
    }

    /// Returns the exposure multiplier.
    public float getExposure() {
        return exposure;
    }

    /// Returns a number that changes whenever a setting does, so a backend can
    /// tell that a shader variant or a uniform it cached is stale.
    public int getVersion() {
        return version;
    }

    /// Returns the part of the shader variant key that depends on this
    /// environment: whether fog, hemisphere lighting, tone mapping and the
    /// open sky are on.
    /// Values such as the fog colour are uniforms and are not part of it.
    public String getShaderKey() {
        return (isFog() ? "F" : "f") + (hemisphere ? "H" : "h") + (toneMapping ? "T" : "t")
                + (sky ? "K" : "k");
    }
}
