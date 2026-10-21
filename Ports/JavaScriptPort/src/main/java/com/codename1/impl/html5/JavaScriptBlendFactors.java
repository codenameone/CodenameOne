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

/// The four WebGL blend factors of a `com.codename1.gpu.RenderState.BlendMode`
/// that blends, as `blendFuncSeparate` takes them.
///
/// The colour channels and the alpha channel are given different factors on
/// purpose. The colour is weighted by the source alpha; the alpha channel
/// accumulates coverage, `a = sa + da * (1 - sa)`, so a frame cleared opaque
/// stays opaque whatever is blended onto it.
///
/// One `blendFunc` for all four channels weights the alpha by itself
/// (`sa * sa + da * (1 - sa)`), and a quad at 40% over an opaque clear then
/// leaves 0.76 in the drawing buffer. The WebGL canvas is premultiplied and
/// is drawn onto the display canvas source-over, so a quarter of whatever was
/// there before -- the frame before this one -- stayed in every such pixel,
/// and a black tint meant to give `0.6 * background` settled at
/// `0.6 * background / 0.76` instead.
///
/// This class has no natives and no dependencies, so that it compiles and is
/// tested alone under plain JUnit (`GpuBlendContractTest`), which runs
/// the blend equation with these factors.
public final class JavaScriptBlendFactors {
    /// `gl.ZERO`.
    public static final int ZERO = 0;
    /// `gl.ONE`.
    public static final int ONE = 1;
    /// `gl.SRC_ALPHA`.
    public static final int SRC_ALPHA = 0x0302;
    /// `gl.ONE_MINUS_SRC_ALPHA`.
    public static final int ONE_MINUS_SRC_ALPHA = 0x0303;

    private JavaScriptBlendFactors() {
    }

    /// The factor of the source colour: its own alpha, in both modes.
    ///
    /// #### Parameters
    ///
    /// - `additive`: true for `BlendMode.ADDITIVE`, false for `BlendMode.ALPHA`
    public static int sourceColor(boolean additive) {
        return SRC_ALPHA;
    }

    /// The factor of the destination colour: all of it when adding light,
    /// what the source leaves uncovered otherwise.
    ///
    /// #### Parameters
    ///
    /// - `additive`: true for `BlendMode.ADDITIVE`, false for `BlendMode.ALPHA`
    public static int destinationColor(boolean additive) {
        return additive ? ONE : ONE_MINUS_SRC_ALPHA;
    }

    /// The factor of the source alpha: one, never the alpha itself.
    ///
    /// #### Parameters
    ///
    /// - `additive`: true for `BlendMode.ADDITIVE`, false for `BlendMode.ALPHA`
    public static int sourceAlpha(boolean additive) {
        return ONE;
    }

    /// The factor of the destination alpha: what the source leaves uncovered.
    ///
    /// #### Parameters
    ///
    /// - `additive`: true for `BlendMode.ADDITIVE`, false for `BlendMode.ALPHA`
    public static int destinationAlpha(boolean additive) {
        return ONE_MINUS_SRC_ALPHA;
    }
}
