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

/// Dart's {@code Int32x4}: four 32-bit integer lanes, immutable. Arithmetic wraps
/// in each lane as Dart's does; a lane reads back as a signed 32-bit value.
public final class Int32x4 {
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

    final int x;
    final int y;
    final int z;
    final int w;

    public Int32x4(long x, long y, long z, long w) {
        this.x = (int) x;
        this.y = (int) y;
        this.z = (int) z;
        this.w = (int) w;
    }

    /// {@code Int32x4.bool(x, y, z, w)}: each true lane is all ones (-1), each false one 0.
    public static Int32x4 bool(boolean x, boolean y, boolean z, boolean w) {
        return new Int32x4(x ? -1 : 0, y ? -1 : 0, z ? -1 : 0, w ? -1 : 0);
    }

    /// {@code Int32x4.fromFloat32x4Bits(v)}: the raw bits of each float lane.
    public static Int32x4 fromFloat32x4Bits(Float32x4 v) {
        return new Int32x4(Float.floatToRawIntBits(v.x), Float.floatToRawIntBits(v.y),
                Float.floatToRawIntBits(v.z), Float.floatToRawIntBits(v.w));
    }

    int lane(int i) {
        return i == 0 ? x : i == 1 ? y : i == 2 ? z : w;
    }

    public long x() { return x; }
    public long y() { return y; }
    public long z() { return z; }
    public long w() { return w; }
    public boolean flagX() { return x != 0; }
    public boolean flagY() { return y != 0; }
    public boolean flagZ() { return z != 0; }
    public boolean flagW() { return w != 0; }

    public long signMask() {
        return (x >>> 31) | ((y >>> 31) << 1) | ((z >>> 31) << 2) | ((w >>> 31) << 3);
    }

    public Int32x4 $bitOr(Int32x4 o) { return new Int32x4(x | o.x, y | o.y, z | o.z, w | o.w); }
    public Int32x4 $bitAnd(Int32x4 o) { return new Int32x4(x & o.x, y & o.y, z & o.z, w & o.w); }
    public Int32x4 $bitXor(Int32x4 o) { return new Int32x4(x ^ o.x, y ^ o.y, z ^ o.z, w ^ o.w); }
    public Int32x4 $plus(Int32x4 o) { return new Int32x4(x + o.x, y + o.y, z + o.z, w + o.w); }
    public Int32x4 $minus(Int32x4 o) { return new Int32x4(x - o.x, y - o.y, z - o.z, w - o.w); }

    static void checkMask(long mask) {
        if (mask < 0 || mask > 255) {
            throw new dart.core.RangeError("Invalid value: Not in inclusive range 0..255: " + mask + " (mask)");
        }
    }

    public Int32x4 shuffle(long mask) {
        checkMask(mask);
        int m = (int) mask;
        return new Int32x4(lane(m & 3), lane((m >> 2) & 3), lane((m >> 4) & 3), lane((m >> 6) & 3));
    }

    /// x and y lanes from this, z and w from {@code other}.
    public Int32x4 shuffleMix(Int32x4 other, long mask) {
        checkMask(mask);
        int m = (int) mask;
        return new Int32x4(lane(m & 3), lane((m >> 2) & 3), other.lane((m >> 4) & 3), other.lane((m >> 6) & 3));
    }

    public Int32x4 withX(long v) { return new Int32x4(v, y, z, w); }
    public Int32x4 withY(long v) { return new Int32x4(x, v, z, w); }
    public Int32x4 withZ(long v) { return new Int32x4(x, y, v, w); }
    public Int32x4 withW(long v) { return new Int32x4(x, y, z, v); }
    public Int32x4 withFlagX(boolean f) { return withX(f ? -1 : 0); }
    public Int32x4 withFlagY(boolean f) { return withY(f ? -1 : 0); }
    public Int32x4 withFlagZ(boolean f) { return withZ(f ? -1 : 0); }
    public Int32x4 withFlagW(boolean f) { return withW(f ? -1 : 0); }

    /// Bitwise select: each bit set in this lane takes {@code trueValue}'s bit.
    public Float32x4 select(Float32x4 trueValue, Float32x4 falseValue) {
        Int32x4 t = fromFloat32x4Bits(trueValue);
        Int32x4 f = fromFloat32x4Bits(falseValue);
        return Float32x4.fromInt32x4Bits(new Int32x4((x & t.x) | (~x & f.x), (y & t.y) | (~y & f.y),
                (z & t.z) | (~z & f.z), (w & t.w) | (~w & f.w)));
    }

    private static String hex(int v) {
        String s = Integer.toHexString(v);
        return "00000000".substring(s.length()) + s;
    }

    @Override
    public String toString() {
        return "[" + hex(x) + ", " + hex(y) + ", " + hex(z) + ", " + hex(w) + "]";
    }
}
