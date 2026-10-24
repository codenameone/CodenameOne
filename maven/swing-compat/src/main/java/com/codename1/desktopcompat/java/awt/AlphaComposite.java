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
package com.codename1.desktopcompat.java.awt;

/// A Porter-Duff rule with an extra alpha.
///
/// Every rule can be named and carried around, but a `Graphics2D` only
/// draws with `SRC_OVER` (and `SRC` at full alpha onto an opaque surface,
/// which is the same thing); `setComposite` throws
/// `UnsupportedOperationException` for the others.
public final class AlphaComposite implements Composite {

    public static final int CLEAR = 1;
    public static final int SRC = 2;
    public static final int DST = 9;
    public static final int SRC_OVER = 3;
    public static final int DST_OVER = 4;
    public static final int SRC_IN = 5;
    public static final int DST_IN = 6;
    public static final int SRC_OUT = 7;
    public static final int DST_OUT = 8;
    public static final int SRC_ATOP = 10;
    public static final int DST_ATOP = 11;
    public static final int XOR = 12;

    public static final AlphaComposite Clear = new AlphaComposite(CLEAR, 1f);
    public static final AlphaComposite Src = new AlphaComposite(SRC, 1f);
    public static final AlphaComposite Dst = new AlphaComposite(DST, 1f);
    public static final AlphaComposite SrcOver = new AlphaComposite(SRC_OVER, 1f);
    public static final AlphaComposite DstOver = new AlphaComposite(DST_OVER, 1f);
    public static final AlphaComposite SrcIn = new AlphaComposite(SRC_IN, 1f);
    public static final AlphaComposite DstIn = new AlphaComposite(DST_IN, 1f);
    public static final AlphaComposite SrcOut = new AlphaComposite(SRC_OUT, 1f);
    public static final AlphaComposite DstOut = new AlphaComposite(DST_OUT, 1f);
    public static final AlphaComposite SrcAtop = new AlphaComposite(SRC_ATOP, 1f);
    public static final AlphaComposite DstAtop = new AlphaComposite(DST_ATOP, 1f);
    public static final AlphaComposite Xor = new AlphaComposite(XOR, 1f);

    private final int rule;
    private final float extraAlpha;

    private AlphaComposite(int rule, float alpha) {
        if (rule < CLEAR || rule > XOR) {
            throw new IllegalArgumentException("unknown composite rule");
        }
        if (!(alpha >= 0.0f && alpha <= 1.0f)) {
            throw new IllegalArgumentException("alpha value out of range");
        }
        this.rule = rule;
        this.extraAlpha = alpha;
    }

    public static AlphaComposite getInstance(int rule) {
        return new AlphaComposite(rule, 1f);
    }

    public static AlphaComposite getInstance(int rule, float alpha) {
        return new AlphaComposite(rule, alpha);
    }

    public float getAlpha() {
        return extraAlpha;
    }

    public int getRule() {
        return rule;
    }

    public AlphaComposite derive(int rule) {
        return new AlphaComposite(rule, extraAlpha);
    }

    public AlphaComposite derive(float alpha) {
        return new AlphaComposite(rule, alpha);
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(extraAlpha) * 31 + rule;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AlphaComposite)) {
            return false;
        }
        AlphaComposite ac = (AlphaComposite) obj;
        return rule == ac.rule && Float.floatToIntBits(extraAlpha) == Float.floatToIntBits(ac.extraAlpha);
    }
}
