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
package com.codename1.media.audio;

import com.codename1.util.MathUtil;

/// Positions a source in 3D space relative to the context's [AudioListener],
/// the Web Audio `PannerNode`. The output is stereo.
///
/// Azimuth and elevation, distance attenuation and the sound cone follow the
/// specification's algorithms exactly. [PanningModel#EQUALPOWER] (the default)
/// pans by azimuth; [PanningModel#HRTF] renders binaurally for headphones.
///
/// ```java
/// PannerNode p = ctx.createPanner();
/// p.setPanningModel(PanningModel.HRTF);
/// p.setDistanceModel(DistanceModelType.INVERSE);
/// p.setRefDistance(1);
/// p.setPosition(3, 0, -2);           // ahead and to the right
/// footsteps.connect(p).connect(ctx.getDestination());
/// ctx.getListener().setPosition(0, 0, 0);
/// ```
///
/// Positions and orientation are audio rate [AudioParam]s and can be
/// automated: `p.getPositionX().linearRampToValueAtTime(-3, now + 4)` moves the
/// source across the listener. Attributes such as the distance model are
/// applied from the next render quantum.
public final class PannerNode extends AudioNode {
    private final AudioParam positionX;
    private final AudioParam positionY;
    private final AudioParam positionZ;
    private final AudioParam orientationX;
    private final AudioParam orientationY;
    private final AudioParam orientationZ;
    private PanningModel panningModel = PanningModel.EQUALPOWER;
    private DistanceModelType distanceModel = DistanceModelType.INVERSE;
    private double refDistance = 1;
    private double maxDistance = 10000;
    private double rolloffFactor = 1;
    private double coneInnerAngle = 360;
    private double coneOuterAngle = 360;
    private double coneOuterGain;

    // ---- render side ----
    private PanningModel rPanning = PanningModel.EQUALPOWER;
    private DistanceModelType rDistance = DistanceModelType.INVERSE;
    private double rRef = 1;
    private double rMax = 10000;
    private double rRolloff = 1;
    private double rInner = 360;
    private double rOuter = 360;
    private double rOuterGain;
    private Hrtf rHrtf;
    private final float[] gains = new float[BaseAudioContext.RENDER_QUANTUM];
    private final float[] azimuths = new float[BaseAudioContext.RENDER_QUANTUM];
    private final float[] mono = new float[BaseAudioContext.RENDER_QUANTUM];
    private final double[] angles = new double[2];

    PannerNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.CLAMPED_MAX, ChannelInterpretation.SPEAKERS);
        positionX = AudioListener.param(context, "positionX", 0);
        positionY = AudioListener.param(context, "positionY", 0);
        positionZ = AudioListener.param(context, "positionZ", 0);
        orientationX = AudioListener.param(context, "orientationX", 1);
        orientationY = AudioListener.param(context, "orientationY", 0);
        orientationZ = AudioListener.param(context, "orientationZ", 0);
    }

    @Override
    void checkChannelCount(int count) {
        if (count < 1 || count > 2) {
            throw new IllegalArgumentException("A panner takes 1 or 2 channels: " + count);
        }
    }

    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode == ChannelCountMode.MAX) {
            throw new IllegalArgumentException("A panner cannot use ChannelCountMode.MAX");
        }
    }

    /// Moves the source now.
    ///
    /// #### Parameters
    ///
    /// - `x`: position on X
    ///
    /// - `y`: position on Y
    ///
    /// - `z`: position on Z
    public void setPosition(float x, float y, float z) {
        positionX.setValue(x);
        positionY.setValue(y);
        positionZ.setValue(z);
    }

    /// Points the source now; only matters with a cone narrower than 360 degrees.
    ///
    /// #### Parameters
    ///
    /// - `x`: direction X
    ///
    /// - `y`: direction Y
    ///
    /// - `z`: direction Z
    public void setOrientation(float x, float y, float z) {
        orientationX.setValue(x);
        orientationY.setValue(y);
        orientationZ.setValue(z);
    }

    /// Position on X, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getPositionX() {
        return positionX;
    }

    /// Position on Y, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getPositionY() {
        return positionY;
    }

    /// Position on Z, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getPositionZ() {
        return positionZ;
    }

    /// X of the direction the source faces, 1 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getOrientationX() {
        return orientationX;
    }

    /// Y of the direction the source faces, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getOrientationY() {
        return orientationY;
    }

    /// Z of the direction the source faces, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getOrientationZ() {
        return orientationZ;
    }

    /// The panning model.
    ///
    /// #### Returns
    ///
    /// the model
    public PanningModel getPanningModel() {
        return panningModel;
    }

    /// Sets the panning model.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model
    public void setPanningModel(final PanningModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model is null");
        }
        panningModel = model;
        context.post(new Runnable() {
            @Override
            public void run() {
                rPanning = model;
            }
        });
    }

    /// The distance model.
    ///
    /// #### Returns
    ///
    /// the model
    public DistanceModelType getDistanceModel() {
        return distanceModel;
    }

    /// Sets the distance model.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model
    public void setDistanceModel(final DistanceModelType model) {
        if (model == null) {
            throw new IllegalArgumentException("model is null");
        }
        distanceModel = model;
        context.post(new Runnable() {
            @Override
            public void run() {
                rDistance = model;
            }
        });
    }

    /// The distance at which attenuation starts, 1 by default.
    ///
    /// #### Returns
    ///
    /// the distance
    public double getRefDistance() {
        return refDistance;
    }

    /// Sets the distance at which attenuation starts.
    ///
    /// #### Parameters
    ///
    /// - `refDistance`: 0 or more
    public void setRefDistance(final double refDistance) {
        if (!BaseAudioContext.nonNegativeFinite(refDistance)) {
            throw new IllegalArgumentException("refDistance must be 0 or more: " + refDistance);
        }
        this.refDistance = refDistance;
        context.post(new Runnable() {
            @Override
            public void run() {
                rRef = refDistance;
            }
        });
    }

    /// The distance past which the linear model attenuates no further, 10000 by
    /// default.
    ///
    /// #### Returns
    ///
    /// the distance
    public double getMaxDistance() {
        return maxDistance;
    }

    /// Sets the distance past which the linear model attenuates no further.
    ///
    /// #### Parameters
    ///
    /// - `maxDistance`: more than 0
    public void setMaxDistance(final double maxDistance) {
        if (!BaseAudioContext.positiveFinite(maxDistance)) {
            throw new IllegalArgumentException("maxDistance must be more than 0: " + maxDistance);
        }
        this.maxDistance = maxDistance;
        context.post(new Runnable() {
            @Override
            public void run() {
                rMax = maxDistance;
            }
        });
    }

    /// How fast the volume falls with distance, 1 by default.
    ///
    /// #### Returns
    ///
    /// the factor
    public double getRolloffFactor() {
        return rolloffFactor;
    }

    /// Sets how fast the volume falls with distance.
    ///
    /// #### Parameters
    ///
    /// - `rolloffFactor`: 0 or more; the linear model clamps it to 1
    public void setRolloffFactor(final double rolloffFactor) {
        if (!BaseAudioContext.nonNegativeFinite(rolloffFactor)) {
            throw new IllegalArgumentException("rolloffFactor must be 0 or more: " + rolloffFactor);
        }
        this.rolloffFactor = rolloffFactor;
        context.post(new Runnable() {
            @Override
            public void run() {
                rRolloff = rolloffFactor;
            }
        });
    }

    /// The angle, in degrees, of the cone inside which there is no cone
    /// attenuation; 360 by default.
    ///
    /// #### Returns
    ///
    /// degrees
    public double getConeInnerAngle() {
        return coneInnerAngle;
    }

    /// Sets the inner cone angle.
    ///
    /// #### Parameters
    ///
    /// - `angle`: degrees
    public void setConeInnerAngle(final double angle) {
        coneInnerAngle = angle;
        context.post(new Runnable() {
            @Override
            public void run() {
                rInner = angle;
            }
        });
    }

    /// The angle, in degrees, outside which the gain is [#getConeOuterGain()];
    /// 360 by default.
    ///
    /// #### Returns
    ///
    /// degrees
    public double getConeOuterAngle() {
        return coneOuterAngle;
    }

    /// Sets the outer cone angle.
    ///
    /// #### Parameters
    ///
    /// - `angle`: degrees
    public void setConeOuterAngle(final double angle) {
        coneOuterAngle = angle;
        context.post(new Runnable() {
            @Override
            public void run() {
                rOuter = angle;
            }
        });
    }

    /// The gain outside the outer cone, 0 by default.
    ///
    /// #### Returns
    ///
    /// the gain
    public double getConeOuterGain() {
        return coneOuterGain;
    }

    /// Sets the gain outside the outer cone.
    ///
    /// #### Parameters
    ///
    /// - `gain`: 0 to 1
    public void setConeOuterGain(final double gain) {
        if (Double.isNaN(gain) || gain < 0 || gain > 1) {
            throw new IllegalArgumentException("coneOuterGain must be between 0 and 1: " + gain);
        }
        coneOuterGain = gain;
        context.post(new Runnable() {
            @Override
            public void run() {
                rOuterGain = gain;
            }
        });
    }

    // ---------------------------------------------------------------- render

    @Override
    void process(long q) {
        AudioListener l = context.getListener();
        l.compute(q);
        float[] sx = positionX.compute(q);
        float[] sy = positionY.compute(q);
        float[] sz = positionZ.compute(q);
        float[] ox = orientationX.compute(q);
        float[] oy = orientationY.compute(q);
        float[] oz = orientationZ.compute(q);
        boolean constant = l.constant && positionX.isConstant() && positionY.isConstant()
                && positionZ.isConstant() && orientationX.isConstant() && orientationY.isConstant()
                && orientationZ.isConstant();
        double elevation = 0;
        for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
            if (i > 0 && constant) {
                azimuths[i] = azimuths[0];
                gains[i] = gains[0];
                continue;
            }
            double dx = sx[i] - l.px[i];
            double dy = sy[i] - l.py[i];
            double dz = sz[i] - l.pz[i];
            angles(dx, dy, dz, l.fx[i], l.fy[i], l.fz[i], l.ux[i], l.uy[i], l.uz[i], angles);
            azimuths[i] = (float) angles[0];
            if (i == BaseAudioContext.RENDER_QUANTUM - 1 || constant) {
                elevation = angles[1];
            }
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            gains[i] = (float) (distanceGain(rDistance, dist, rRef, rMax, rRolloff)
                    * coneGain(-dx, -dy, -dz, ox[i], oy[i], oz[i], rInner, rOuter, rOuterGain));
        }
        AudioBus in = inBuses[0];
        AudioBus out = outBuses[0];
        out.setChannels(2);
        float[] left = out.data[0];
        float[] right = out.data[1];
        if (rPanning == PanningModel.HRTF) {
            if (rHrtf == null) {
                rHrtf = new Hrtf(context.getSampleRate());
            }
            float[] m = mono;
            if (in.channels == 1) {
                System.arraycopy(in.data[0], 0, m, 0, BaseAudioContext.RENDER_QUANTUM);
            } else {
                float[] a = in.data[0];
                float[] b = in.data[1];
                for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
                    m[i] = 0.5f * (a[i] + b[i]);
                }
            }
            rHrtf.process(m, left, right, azimuths[BaseAudioContext.RENDER_QUANTUM - 1], elevation, gains);
            return;
        }
        boolean monoIn = in.channels == 1;
        float[] inL = in.data[0];
        float[] inR = monoIn ? inL : in.data[1];
        double gl = 0;
        double gr = 0;
        double az = 0;
        for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
            if (i == 0 || !constant) {
                az = azimuths[i];
                if (az < -90) {
                    az = -180 - az;
                } else if (az > 90) {
                    az = 180 - az;
                }
                double x = monoIn ? (az + 90) / 180 : (az <= 0 ? (az + 90) / 90 : az / 90);
                gl = Math.cos(x * Math.PI / 2);
                gr = Math.sin(x * Math.PI / 2);
            }
            double g = gains[i];
            if (monoIn) {
                left[i] = (float) (inL[i] * gl * g);
                right[i] = (float) (inL[i] * gr * g);
            } else if (az <= 0) {
                left[i] = (float) ((inL[i] + inR[i] * gl) * g);
                right[i] = (float) (inR[i] * gr * g);
            } else {
                left[i] = (float) (inL[i] * gl * g);
                right[i] = (float) ((inR[i] + inL[i] * gr) * g);
            }
        }
    }

    /// Azimuth and elevation in degrees of the source-listener vector `d` for a
    /// listener facing `f` with up `u`, exactly as the specification computes them.
    static void angles(double dx, double dy, double dz, double fx, double fy, double fz,
                       double ux, double uy, double uz, double[] out) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) {
            out[0] = 0;
            out[1] = 0;
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        // right = forward x up
        double rx = fy * uz - fz * uy;
        double ry = fz * ux - fx * uz;
        double rz = fx * uy - fy * ux;
        double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
        double fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (rl == 0 || fl == 0) {
            out[0] = 0;
            out[1] = 0;
            return;
        }
        rx /= rl;
        ry /= rl;
        rz /= rl;
        fx /= fl;
        fy /= fl;
        fz /= fl;
        // up = right x forward
        double vx = ry * fz - rz * fy;
        double vy = rz * fx - rx * fz;
        double vz = rx * fy - ry * fx;
        double upProj = dx * vx + dy * vy + dz * vz;
        double px = dx - upProj * vx;
        double py = dy - upProj * vy;
        double pz = dz - upProj * vz;
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        double azimuth;
        if (pl == 0) {
            azimuth = 0;
        } else {
            px /= pl;
            py /= pl;
            pz /= pl;
            azimuth = Math.toDegrees(MathUtil.acos(clamp1(px * rx + py * ry + pz * rz)));
            double frontBack = px * fx + py * fy + pz * fz;
            if (frontBack < 0) {
                azimuth = 360 - azimuth;
            }
            if (azimuth >= 0 && azimuth <= 270) {
                azimuth = 90 - azimuth;
            } else {
                azimuth = 450 - azimuth;
            }
        }
        double elevation = 90 - Math.toDegrees(MathUtil.acos(clamp1(upProj)));
        if (elevation > 90) {
            elevation = 180 - elevation;
        } else if (elevation < -90) {
            elevation = -180 - elevation;
        }
        out[0] = azimuth;
        out[1] = elevation;
    }

    private static double clamp1(double v) {
        return v < -1 ? -1 : v > 1 ? 1 : v;
    }

    static double distanceGain(DistanceModelType model, double d, double ref, double max, double rolloff) {
        switch (model) {
            case LINEAR: {
                double dd = Math.max(Math.min(d, Math.max(ref, max)), Math.min(ref, max));
                double r = Math.min(rolloff, 1);
                if (max == ref) {
                    return 1 - r;
                }
                return 1 - r * (dd - ref) / (max - ref);
            }
            case EXPONENTIAL: {
                if (ref == 0) {
                    return 0;
                }
                double dd = Math.max(d, ref);
                return MathUtil.pow(dd / ref, -rolloff);
            }
            default: {
                double dd = Math.max(d, ref);
                double den = ref + rolloff * (dd - ref);
                return den == 0 ? 1 : ref / den;
            }
        }
    }

    /// The cone gain for a source facing `o`, with `t` the vector from the
    /// source to the listener.
    static double coneGain(double tx, double ty, double tz, double ox, double oy, double oz,
                           double inner, double outer, double outerGain) {
        double ol = Math.sqrt(ox * ox + oy * oy + oz * oz);
        if (ol == 0 || (inner == 360 && outer == 360)) {
            return 1;
        }
        double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (tl == 0) {
            return 1;
        }
        double cos = (tx * ox + ty * oy + tz * oz) / (tl * ol);
        double angle = Math.abs(Math.toDegrees(MathUtil.acos(clamp1(cos))));
        double absInner = Math.abs(inner) / 2;
        double absOuter = Math.abs(outer) / 2;
        if (angle <= absInner) {
            return 1;
        }
        if (angle >= absOuter) {
            return outerGain;
        }
        double x = (angle - absInner) / (absOuter - absInner);
        return (1 - x) + outerGain * x;
    }
}
