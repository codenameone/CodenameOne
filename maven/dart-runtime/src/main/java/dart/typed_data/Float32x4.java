/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.typed_data;

/// Dart's {@code Float32x4}: four single-precision lanes, immutable. Every lane is
/// rounded to a float on the way in, so a lane reads back as Dart's does.
public final class Float32x4 {
    public static final long xxxx = 0;
    public static final long xxxy = 64;
    public static final long xxxz = 128;
    public static final long xxxw = 192;
    public static final long xxyx = 16;
    public static final long xxyy = 80;
    public static final long xxyz = 144;
    public static final long xxyw = 208;
    public static final long xxzx = 32;
    public static final long xxzy = 96;
    public static final long xxzz = 160;
    public static final long xxzw = 224;
    public static final long xxwx = 48;
    public static final long xxwy = 112;
    public static final long xxwz = 176;
    public static final long xxww = 240;
    public static final long xyxx = 4;
    public static final long xyxy = 68;
    public static final long xyxz = 132;
    public static final long xyxw = 196;
    public static final long xyyx = 20;
    public static final long xyyy = 84;
    public static final long xyyz = 148;
    public static final long xyyw = 212;
    public static final long xyzx = 36;
    public static final long xyzy = 100;
    public static final long xyzz = 164;
    public static final long xyzw = 228;
    public static final long xywx = 52;
    public static final long xywy = 116;
    public static final long xywz = 180;
    public static final long xyww = 244;
    public static final long xzxx = 8;
    public static final long xzxy = 72;
    public static final long xzxz = 136;
    public static final long xzxw = 200;
    public static final long xzyx = 24;
    public static final long xzyy = 88;
    public static final long xzyz = 152;
    public static final long xzyw = 216;
    public static final long xzzx = 40;
    public static final long xzzy = 104;
    public static final long xzzz = 168;
    public static final long xzzw = 232;
    public static final long xzwx = 56;
    public static final long xzwy = 120;
    public static final long xzwz = 184;
    public static final long xzww = 248;
    public static final long xwxx = 12;
    public static final long xwxy = 76;
    public static final long xwxz = 140;
    public static final long xwxw = 204;
    public static final long xwyx = 28;
    public static final long xwyy = 92;
    public static final long xwyz = 156;
    public static final long xwyw = 220;
    public static final long xwzx = 44;
    public static final long xwzy = 108;
    public static final long xwzz = 172;
    public static final long xwzw = 236;
    public static final long xwwx = 60;
    public static final long xwwy = 124;
    public static final long xwwz = 188;
    public static final long xwww = 252;
    public static final long yxxx = 1;
    public static final long yxxy = 65;
    public static final long yxxz = 129;
    public static final long yxxw = 193;
    public static final long yxyx = 17;
    public static final long yxyy = 81;
    public static final long yxyz = 145;
    public static final long yxyw = 209;
    public static final long yxzx = 33;
    public static final long yxzy = 97;
    public static final long yxzz = 161;
    public static final long yxzw = 225;
    public static final long yxwx = 49;
    public static final long yxwy = 113;
    public static final long yxwz = 177;
    public static final long yxww = 241;
    public static final long yyxx = 5;
    public static final long yyxy = 69;
    public static final long yyxz = 133;
    public static final long yyxw = 197;
    public static final long yyyx = 21;
    public static final long yyyy = 85;
    public static final long yyyz = 149;
    public static final long yyyw = 213;
    public static final long yyzx = 37;
    public static final long yyzy = 101;
    public static final long yyzz = 165;
    public static final long yyzw = 229;
    public static final long yywx = 53;
    public static final long yywy = 117;
    public static final long yywz = 181;
    public static final long yyww = 245;
    public static final long yzxx = 9;
    public static final long yzxy = 73;
    public static final long yzxz = 137;
    public static final long yzxw = 201;
    public static final long yzyx = 25;
    public static final long yzyy = 89;
    public static final long yzyz = 153;
    public static final long yzyw = 217;
    public static final long yzzx = 41;
    public static final long yzzy = 105;
    public static final long yzzz = 169;
    public static final long yzzw = 233;
    public static final long yzwx = 57;
    public static final long yzwy = 121;
    public static final long yzwz = 185;
    public static final long yzww = 249;
    public static final long ywxx = 13;
    public static final long ywxy = 77;
    public static final long ywxz = 141;
    public static final long ywxw = 205;
    public static final long ywyx = 29;
    public static final long ywyy = 93;
    public static final long ywyz = 157;
    public static final long ywyw = 221;
    public static final long ywzx = 45;
    public static final long ywzy = 109;
    public static final long ywzz = 173;
    public static final long ywzw = 237;
    public static final long ywwx = 61;
    public static final long ywwy = 125;
    public static final long ywwz = 189;
    public static final long ywww = 253;
    public static final long zxxx = 2;
    public static final long zxxy = 66;
    public static final long zxxz = 130;
    public static final long zxxw = 194;
    public static final long zxyx = 18;
    public static final long zxyy = 82;
    public static final long zxyz = 146;
    public static final long zxyw = 210;
    public static final long zxzx = 34;
    public static final long zxzy = 98;
    public static final long zxzz = 162;
    public static final long zxzw = 226;
    public static final long zxwx = 50;
    public static final long zxwy = 114;
    public static final long zxwz = 178;
    public static final long zxww = 242;
    public static final long zyxx = 6;
    public static final long zyxy = 70;
    public static final long zyxz = 134;
    public static final long zyxw = 198;
    public static final long zyyx = 22;
    public static final long zyyy = 86;
    public static final long zyyz = 150;
    public static final long zyyw = 214;
    public static final long zyzx = 38;
    public static final long zyzy = 102;
    public static final long zyzz = 166;
    public static final long zyzw = 230;
    public static final long zywx = 54;
    public static final long zywy = 118;
    public static final long zywz = 182;
    public static final long zyww = 246;
    public static final long zzxx = 10;
    public static final long zzxy = 74;
    public static final long zzxz = 138;
    public static final long zzxw = 202;
    public static final long zzyx = 26;
    public static final long zzyy = 90;
    public static final long zzyz = 154;
    public static final long zzyw = 218;
    public static final long zzzx = 42;
    public static final long zzzy = 106;
    public static final long zzzz = 170;
    public static final long zzzw = 234;
    public static final long zzwx = 58;
    public static final long zzwy = 122;
    public static final long zzwz = 186;
    public static final long zzww = 250;
    public static final long zwxx = 14;
    public static final long zwxy = 78;
    public static final long zwxz = 142;
    public static final long zwxw = 206;
    public static final long zwyx = 30;
    public static final long zwyy = 94;
    public static final long zwyz = 158;
    public static final long zwyw = 222;
    public static final long zwzx = 46;
    public static final long zwzy = 110;
    public static final long zwzz = 174;
    public static final long zwzw = 238;
    public static final long zwwx = 62;
    public static final long zwwy = 126;
    public static final long zwwz = 190;
    public static final long zwww = 254;
    public static final long wxxx = 3;
    public static final long wxxy = 67;
    public static final long wxxz = 131;
    public static final long wxxw = 195;
    public static final long wxyx = 19;
    public static final long wxyy = 83;
    public static final long wxyz = 147;
    public static final long wxyw = 211;
    public static final long wxzx = 35;
    public static final long wxzy = 99;
    public static final long wxzz = 163;
    public static final long wxzw = 227;
    public static final long wxwx = 51;
    public static final long wxwy = 115;
    public static final long wxwz = 179;
    public static final long wxww = 243;
    public static final long wyxx = 7;
    public static final long wyxy = 71;
    public static final long wyxz = 135;
    public static final long wyxw = 199;
    public static final long wyyx = 23;
    public static final long wyyy = 87;
    public static final long wyyz = 151;
    public static final long wyyw = 215;
    public static final long wyzx = 39;
    public static final long wyzy = 103;
    public static final long wyzz = 167;
    public static final long wyzw = 231;
    public static final long wywx = 55;
    public static final long wywy = 119;
    public static final long wywz = 183;
    public static final long wyww = 247;
    public static final long wzxx = 11;
    public static final long wzxy = 75;
    public static final long wzxz = 139;
    public static final long wzxw = 203;
    public static final long wzyx = 27;
    public static final long wzyy = 91;
    public static final long wzyz = 155;
    public static final long wzyw = 219;
    public static final long wzzx = 43;
    public static final long wzzy = 107;
    public static final long wzzz = 171;
    public static final long wzzw = 235;
    public static final long wzwx = 59;
    public static final long wzwy = 123;
    public static final long wzwz = 187;
    public static final long wzww = 251;
    public static final long wwxx = 15;
    public static final long wwxy = 79;
    public static final long wwxz = 143;
    public static final long wwxw = 207;
    public static final long wwyx = 31;
    public static final long wwyy = 95;
    public static final long wwyz = 159;
    public static final long wwyw = 223;
    public static final long wwzx = 47;
    public static final long wwzy = 111;
    public static final long wwzz = 175;
    public static final long wwzw = 239;
    public static final long wwwx = 63;
    public static final long wwwy = 127;
    public static final long wwwz = 191;
    public static final long wwww = 255;

    final float x;
    final float y;
    final float z;
    final float w;

    public Float32x4(double x, double y, double z, double w) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.w = (float) w;
    }

    public static Float32x4 splat(double v) {
        return new Float32x4(v, v, v, v);
    }

    public static Float32x4 zero() {
        return new Float32x4(0, 0, 0, 0);
    }

    public static Float32x4 fromInt32x4Bits(Int32x4 v) {
        return new Float32x4(Float.intBitsToFloat(v.x), Float.intBitsToFloat(v.y),
                Float.intBitsToFloat(v.z), Float.intBitsToFloat(v.w));
    }

    /// x and y from {@code v}, z and w zero.
    public static Float32x4 fromFloat64x2(Float64x2 v) {
        return new Float32x4(v.x, v.y, 0, 0);
    }

    float lane(int i) {
        return i == 0 ? x : i == 1 ? y : i == 2 ? z : w;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double w() { return w; }

    public long signMask() {
        return (Float.floatToRawIntBits(x) >>> 31) | ((Float.floatToRawIntBits(y) >>> 31) << 1)
                | ((Float.floatToRawIntBits(z) >>> 31) << 2) | ((Float.floatToRawIntBits(w) >>> 31) << 3);
    }

    public Float32x4 $plus(Float32x4 o) { return new Float32x4(x + o.x, y + o.y, z + o.z, w + o.w); }
    public Float32x4 $minus(Float32x4 o) { return new Float32x4(x - o.x, y - o.y, z - o.z, w - o.w); }
    public Float32x4 $minus() { return new Float32x4(-x, -y, -z, -w); }
    public Float32x4 $times(Float32x4 o) { return new Float32x4(x * o.x, y * o.y, z * o.z, w * o.w); }
    public Float32x4 $div(Float32x4 o) { return new Float32x4(x / o.x, y / o.y, z / o.z, w / o.w); }

    public Int32x4 lessThan(Float32x4 o) { return Int32x4.bool(x < o.x, y < o.y, z < o.z, w < o.w); }
    public Int32x4 lessThanOrEqual(Float32x4 o) { return Int32x4.bool(x <= o.x, y <= o.y, z <= o.z, w <= o.w); }
    public Int32x4 greaterThan(Float32x4 o) { return Int32x4.bool(x > o.x, y > o.y, z > o.z, w > o.w); }
    public Int32x4 greaterThanOrEqual(Float32x4 o) { return Int32x4.bool(x >= o.x, y >= o.y, z >= o.z, w >= o.w); }
    public Int32x4 equal(Float32x4 o) { return Int32x4.bool(x == o.x, y == o.y, z == o.z, w == o.w); }
    public Int32x4 notEqual(Float32x4 o) { return Int32x4.bool(x != o.x, y != o.y, z != o.z, w != o.w); }

    public Float32x4 scale(double s) { return new Float32x4(x * s, y * s, z * s, w * s); }
    public Float32x4 abs() { return new Float32x4(Math.abs(x), Math.abs(y), Math.abs(z), Math.abs(w)); }

    private static float clampLane(float v, float lo, float hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    public Float32x4 clamp(Float32x4 lo, Float32x4 hi) {
        return new Float32x4(clampLane(x, lo.x, hi.x), clampLane(y, lo.y, hi.y),
                clampLane(z, lo.z, hi.z), clampLane(w, lo.w, hi.w));
    }

    public Float32x4 shuffle(long mask) {
        Int32x4.checkMask(mask);
        int m = (int) mask;
        return new Float32x4(lane(m & 3), lane((m >> 2) & 3), lane((m >> 4) & 3), lane((m >> 6) & 3));
    }

    public Float32x4 shuffleMix(Float32x4 other, long mask) {
        Int32x4.checkMask(mask);
        int m = (int) mask;
        return new Float32x4(lane(m & 3), lane((m >> 2) & 3), other.lane((m >> 4) & 3), other.lane((m >> 6) & 3));
    }

    public Float32x4 withX(double v) { return new Float32x4(v, y, z, w); }
    public Float32x4 withY(double v) { return new Float32x4(x, v, z, w); }
    public Float32x4 withZ(double v) { return new Float32x4(x, y, v, w); }
    public Float32x4 withW(double v) { return new Float32x4(x, y, z, v); }
    public Float32x4 min(Float32x4 o) { return new Float32x4(Math.min(x, o.x), Math.min(y, o.y), Math.min(z, o.z), Math.min(w, o.w)); }
    public Float32x4 max(Float32x4 o) { return new Float32x4(Math.max(x, o.x), Math.max(y, o.y), Math.max(z, o.z), Math.max(w, o.w)); }
    public Float32x4 sqrt() { return new Float32x4(Math.sqrt(x), Math.sqrt(y), Math.sqrt(z), Math.sqrt(w)); }
    public Float32x4 reciprocal() { return new Float32x4(1 / x, 1 / y, 1 / z, 1 / w); }
    public Float32x4 reciprocalSqrt() { return reciprocal().sqrt(); }

    static String lane6(double v) {
        if (Double.isNaN(v)) {
            return "NaN";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "Infinity" : "-Infinity";
        }
        // Dart prints each lane with C printf %f: six decimals of the exact value.
        java.math.BigDecimal d = new java.math.BigDecimal(v).setScale(6, java.math.BigDecimal.ROUND_HALF_EVEN);
        return d.toPlainString();
    }

    @Override
    public String toString() {
        return "[" + lane6(x) + ", " + lane6(y) + ", " + lane6(z) + ", " + lane6(w) + "]";
    }
}
