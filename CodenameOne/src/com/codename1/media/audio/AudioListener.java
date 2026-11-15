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

/// The position and orientation of the listener every [PannerNode] of a
/// context is heard from, the Web Audio `AudioListener`. Get it with
/// [BaseAudioContext#getListener()].
///
/// The coordinate system is right-handed: by default the listener is at the
/// origin facing -Z, with +Y up and +X to the right. Every coordinate is an
/// audio rate [AudioParam], so a moving listener can be automated.
public final class AudioListener {
    private final AudioParam positionX;
    private final AudioParam positionY;
    private final AudioParam positionZ;
    private final AudioParam forwardX;
    private final AudioParam forwardY;
    private final AudioParam forwardZ;
    private final AudioParam upX;
    private final AudioParam upY;
    private final AudioParam upZ;

    AudioListener(BaseAudioContext c) {
        positionX = param(c, "positionX", 0);
        positionY = param(c, "positionY", 0);
        positionZ = param(c, "positionZ", 0);
        forwardX = param(c, "forwardX", 0);
        forwardY = param(c, "forwardY", 0);
        forwardZ = param(c, "forwardZ", -1);
        upX = param(c, "upX", 0);
        upY = param(c, "upY", 1);
        upZ = param(c, "upZ", 0);
    }

    static AudioParam param(BaseAudioContext c, String name, float def) {
        return new AudioParam(c, name, def, -Float.MAX_VALUE, Float.MAX_VALUE, AutomationRate.A_RATE, false);
    }

    /// Moves the listener now.
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

    /// Turns the listener now.
    ///
    /// #### Parameters
    ///
    /// - `fx`: forward X
    ///
    /// - `fy`: forward Y
    ///
    /// - `fz`: forward Z
    ///
    /// - `ux`: up X
    ///
    /// - `uy`: up Y
    ///
    /// - `uz`: up Z
    public void setOrientation(float fx, float fy, float fz, float ux, float uy, float uz) {
        forwardX.setValue(fx);
        forwardY.setValue(fy);
        forwardZ.setValue(fz);
        upX.setValue(ux);
        upY.setValue(uy);
        upZ.setValue(uz);
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

    /// X of the direction faced, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getForwardX() {
        return forwardX;
    }

    /// Y of the direction faced, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getForwardY() {
        return forwardY;
    }

    /// Z of the direction faced, -1 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getForwardZ() {
        return forwardZ;
    }

    /// X of the top of the head, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getUpX() {
        return upX;
    }

    /// Y of the top of the head, 1 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getUpY() {
        return upY;
    }

    /// Z of the top of the head, 0 by default.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getUpZ() {
        return upZ;
    }

    // ---- render side: values for one quantum, shared by every panner ----
    float[] px;
    float[] py;
    float[] pz;
    float[] fx;
    float[] fy;
    float[] fz;
    float[] ux;
    float[] uy;
    float[] uz;
    boolean constant;
    private long rQuantum = -1;

    void compute(long q) {
        if (rQuantum == q) {
            return;
        }
        rQuantum = q;
        px = positionX.compute(q);
        py = positionY.compute(q);
        pz = positionZ.compute(q);
        fx = forwardX.compute(q);
        fy = forwardY.compute(q);
        fz = forwardZ.compute(q);
        ux = upX.compute(q);
        uy = upY.compute(q);
        uz = upZ.compute(q);
        constant = positionX.isConstant() && positionY.isConstant() && positionZ.isConstant()
                && forwardX.isConstant() && forwardY.isConstant() && forwardZ.isConstant()
                && upX.isConstant() && upY.isConstant() && upZ.isConstant();
    }
}
