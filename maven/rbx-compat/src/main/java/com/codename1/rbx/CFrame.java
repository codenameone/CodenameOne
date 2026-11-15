/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.rbx;

import com.codename1.luau.Interp;
import com.codename1.luau.LuaError;
import com.codename1.util.MathUtil;

/// An immutable rigid transform: a position and a 3x3 rotation, stored row
/// major. Columns are the right, up and back vectors; `LookVector` is the
/// negated third column, as in the engine this is compatible with.
public final class CFrame extends RbxType {
    public static final CFrame IDENTITY = new CFrame(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1);

    public final double x;
    public final double y;
    public final double z;
    public final double r00;
    public final double r01;
    public final double r02;
    public final double r10;
    public final double r11;
    public final double r12;
    public final double r20;
    public final double r21;
    public final double r22;

    public CFrame(double x, double y, double z, double r00, double r01, double r02, double r10, double r11,
            double r12, double r20, double r21, double r22) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r00 = r00;
        this.r01 = r01;
        this.r02 = r02;
        this.r10 = r10;
        this.r11 = r11;
        this.r12 = r12;
        this.r20 = r20;
        this.r21 = r21;
        this.r22 = r22;
    }

    public static CFrame at(double x, double y, double z) {
        return new CFrame(x, y, z, 1, 0, 0, 0, 1, 0, 0, 0, 1);
    }

    @Override
    public String typeName() {
        return "CFrame";
    }

    public Vector3 position() {
        return new Vector3(x, y, z);
    }

    public Vector3 right() {
        return new Vector3(r00, r10, r20);
    }

    public Vector3 up() {
        return new Vector3(r01, r11, r21);
    }

    public Vector3 look() {
        return new Vector3(-r02, -r12, -r22);
    }

    public CFrame mul(CFrame o) {
        return new CFrame(
                r00 * o.x + r01 * o.y + r02 * o.z + x,
                r10 * o.x + r11 * o.y + r12 * o.z + y,
                r20 * o.x + r21 * o.y + r22 * o.z + z,
                r00 * o.r00 + r01 * o.r10 + r02 * o.r20,
                r00 * o.r01 + r01 * o.r11 + r02 * o.r21,
                r00 * o.r02 + r01 * o.r12 + r02 * o.r22,
                r10 * o.r00 + r11 * o.r10 + r12 * o.r20,
                r10 * o.r01 + r11 * o.r11 + r12 * o.r21,
                r10 * o.r02 + r11 * o.r12 + r12 * o.r22,
                r20 * o.r00 + r21 * o.r10 + r22 * o.r20,
                r20 * o.r01 + r21 * o.r11 + r22 * o.r21,
                r20 * o.r02 + r21 * o.r12 + r22 * o.r22);
    }

    public Vector3 pointToWorld(Vector3 v) {
        return new Vector3(r00 * v.x + r01 * v.y + r02 * v.z + x,
                r10 * v.x + r11 * v.y + r12 * v.z + y,
                r20 * v.x + r21 * v.y + r22 * v.z + z);
    }

    public Vector3 vectorToWorld(Vector3 v) {
        return new Vector3(r00 * v.x + r01 * v.y + r02 * v.z,
                r10 * v.x + r11 * v.y + r12 * v.z,
                r20 * v.x + r21 * v.y + r22 * v.z);
    }

    public CFrame inverse() {
        return new CFrame(
                -(r00 * x + r10 * y + r20 * z),
                -(r01 * x + r11 * y + r21 * z),
                -(r02 * x + r12 * y + r22 * z),
                r00, r10, r20, r01, r11, r21, r02, r12, r22);
    }

    public Vector3 pointToObject(Vector3 v) {
        double dx = v.x - x;
        double dy = v.y - y;
        double dz = v.z - z;
        return new Vector3(r00 * dx + r10 * dy + r20 * dz, r01 * dx + r11 * dy + r21 * dz,
                r02 * dx + r12 * dy + r22 * dz);
    }

    public Vector3 vectorToObject(Vector3 v) {
        return new Vector3(r00 * v.x + r10 * v.y + r20 * v.z, r01 * v.x + r11 * v.y + r21 * v.z,
                r02 * v.x + r12 * v.y + r22 * v.z);
    }

    public CFrame translate(Vector3 v) {
        return new CFrame(x + v.x, y + v.y, z + v.z, r00, r01, r02, r10, r11, r12, r20, r21, r22);
    }

    public CFrame withPosition(double nx, double ny, double nz) {
        return new CFrame(nx, ny, nz, r00, r01, r02, r10, r11, r12, r20, r21, r22);
    }

    /// The rotation alone.
    public CFrame rotation() {
        return withPosition(0, 0, 0);
    }

    public static CFrame rotX(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new CFrame(0, 0, 0, 1, 0, 0, 0, c, -s, 0, s, c);
    }

    public static CFrame rotY(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new CFrame(0, 0, 0, c, 0, s, 0, 1, 0, -s, 0, c);
    }

    public static CFrame rotZ(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new CFrame(0, 0, 0, c, -s, 0, s, c, 0, 0, 0, 1);
    }

    /// `CFrame.Angles(rx, ry, rz)`: X, then Y, then Z, applied as Rx*Ry*Rz.
    public static CFrame angles(double rx, double ry, double rz) {
        return rotX(rx).mul(rotY(ry)).mul(rotZ(rz));
    }

    /// `CFrame.fromOrientation(rx, ry, rz)`: Ry*Rx*Rz.
    public static CFrame orientation(double rx, double ry, double rz) {
        return rotY(ry).mul(rotX(rx)).mul(rotZ(rz));
    }

    public static CFrame fromAxisAngle(Vector3 axis, double angle) {
        Vector3 u = axis.unit();
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double t = 1 - c;
        return new CFrame(0, 0, 0,
                t * u.x * u.x + c, t * u.x * u.y - s * u.z, t * u.x * u.z + s * u.y,
                t * u.x * u.y + s * u.z, t * u.y * u.y + c, t * u.y * u.z - s * u.x,
                t * u.x * u.z - s * u.y, t * u.y * u.z + s * u.x, t * u.z * u.z + c);
    }

    public static CFrame lookAt(Vector3 at, Vector3 target, Vector3 upHint) {
        Vector3 look = target.sub(at).unit();
        if (look.magnitude() == 0) {
            return at(at.x, at.y, at.z);
        }
        Vector3 back = look.negateV();
        Vector3 right = upHint.cross(back).unit();
        if (right.magnitude() < 1e-6) {
            right = Vector3.Z_AXIS.cross(back).unit();
        }
        Vector3 up = back.cross(right);
        return fromMatrix(at, right, up, back);
    }

    public static CFrame fromMatrix(Vector3 p, Vector3 vx, Vector3 vy, Vector3 vz) {
        return new CFrame(p.x, p.y, p.z, vx.x, vy.x, vz.x, vx.y, vy.y, vz.y, vx.z, vy.z, vz.z);
    }

    /// From a unit quaternion.
    public static CFrame fromQuat(double px, double py, double pz, double qx, double qy, double qz, double qw) {
        double n = Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
        if (n == 0) {
            return at(px, py, pz);
        }
        qx /= n;
        qy /= n;
        qz /= n;
        qw /= n;
        return new CFrame(px, py, pz,
                1 - 2 * (qy * qy + qz * qz), 2 * (qx * qy - qz * qw), 2 * (qx * qz + qy * qw),
                2 * (qx * qy + qz * qw), 1 - 2 * (qx * qx + qz * qz), 2 * (qy * qz - qx * qw),
                2 * (qx * qz - qy * qw), 2 * (qy * qz + qx * qw), 1 - 2 * (qx * qx + qy * qy));
    }

    /// The rotation as a quaternion {x, y, z, w}.
    public double[] quat() {
        double tr = r00 + r11 + r22;
        double qw;
        double qx;
        double qy;
        double qz;
        if (tr > 0) {
            double s = Math.sqrt(tr + 1) * 2;
            qw = 0.25 * s;
            qx = (r21 - r12) / s;
            qy = (r02 - r20) / s;
            qz = (r10 - r01) / s;
        } else if (r00 > r11 && r00 > r22) {
            double s = Math.sqrt(1 + r00 - r11 - r22) * 2;
            qw = (r21 - r12) / s;
            qx = 0.25 * s;
            qy = (r01 + r10) / s;
            qz = (r02 + r20) / s;
        } else if (r11 > r22) {
            double s = Math.sqrt(1 + r11 - r00 - r22) * 2;
            qw = (r02 - r20) / s;
            qx = (r01 + r10) / s;
            qy = 0.25 * s;
            qz = (r12 + r21) / s;
        } else {
            double s = Math.sqrt(1 + r22 - r00 - r11) * 2;
            qw = (r10 - r01) / s;
            qx = (r02 + r20) / s;
            qy = (r12 + r21) / s;
            qz = 0.25 * s;
        }
        return new double[] {qx, qy, qz, qw};
    }

    public CFrame lerp(CFrame o, double t) {
        double[] a = quat();
        double[] b = o.quat();
        double d = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
        if (d < 0) {
            d = -d;
            b = new double[] {-b[0], -b[1], -b[2], -b[3]};
        }
        double wa;
        double wb;
        if (d > 0.9995) {
            wa = 1 - t;
            wb = t;
        } else {
            double th = MathUtil.acos(d);
            double s = Math.sin(th);
            wa = Math.sin((1 - t) * th) / s;
            wb = Math.sin(t * th) / s;
        }
        return fromQuat(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t,
                wa * a[0] + wb * b[0], wa * a[1] + wb * b[1], wa * a[2] + wb * b[2], wa * a[3] + wb * b[3]);
    }

    /// Euler angles for `Angles` order (X, Y, Z).
    public double[] eulerXYZ() {
        double ry = MathUtil.asin(Math.max(-1, Math.min(1, r02)));
        double rx;
        double rz;
        if (Math.abs(r02) < 0.9999999) {
            rx = MathUtil.atan2(-r12, r22);
            rz = MathUtil.atan2(-r01, r00);
        } else {
            rx = MathUtil.atan2(r21, r11);
            rz = 0;
        }
        return new double[] {rx, ry, rz};
    }

    /// Euler angles for `fromOrientation` order (Y, X, Z).
    public double[] eulerYXZ() {
        double rx = MathUtil.asin(Math.max(-1, Math.min(1, -r12)));
        double ry;
        double rz;
        if (Math.abs(r12) < 0.9999999) {
            ry = MathUtil.atan2(r02, r22);
            rz = MathUtil.atan2(r10, r11);
        } else {
            ry = MathUtil.atan2(-r20, r00);
            rz = 0;
        }
        return new double[] {rx, ry, rz};
    }

    private static final String[] METHODS = {"Inverse", "Lerp", "ToWorldSpace", "ToObjectSpace",
        "PointToWorldSpace", "PointToObjectSpace", "VectorToWorldSpace", "VectorToObjectSpace",
        "GetComponents", "ToEulerAnglesXYZ", "ToOrientation", "ToAxisAngle", "Orthonormalize",
        "ToEulerAnglesYXZ", "components", "FuzzyEq"};
    private static final Method[] M = new Method[METHODS.length];

    static {
        for (int i = 0; i < METHODS.length; i++) {
            M[i] = new Method(METHODS[i], i);
        }
    }

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "Position":
            case "p":
                return position();
            case "X":
                return Args.n(x);
            case "Y":
                return Args.n(y);
            case "Z":
                return Args.n(z);
            case "LookVector":
            case "lookVector":
                return look();
            case "RightVector":
            case "rightVector":
            case "XVector":
                return right();
            case "UpVector":
            case "upVector":
            case "YVector":
                return up();
            case "ZVector":
                return new Vector3(r02, r12, r22);
            case "Rotation":
                return rotation();
            default:
                for (int i = 0; i < METHODS.length; i++) {
                    if (METHODS[i].equals(k)) {
                        return M[i];
                    }
                }
                return super.get(in, k);
        }
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        switch (id) {
            case 0:
                return ret(inverse());
            case 1:
                return ret(lerp(Args.cframe(a, 1, "Lerp"), Args.num(a, 2, "Lerp")));
            case 2:
                return many(a, true);
            case 3:
                return many(a, false);
            case 4:
                return ret(pointToWorld(Args.vec3(a, 1, "PointToWorldSpace")));
            case 5:
                return ret(pointToObject(Args.vec3(a, 1, "PointToObjectSpace")));
            case 6:
                return ret(vectorToWorld(Args.vec3(a, 1, "VectorToWorldSpace")));
            case 7:
                return ret(vectorToObject(Args.vec3(a, 1, "VectorToObjectSpace")));
            case 8:
            case 14:
                return new Object[] {Args.n(x), Args.n(y), Args.n(z), Args.n(r00), Args.n(r01), Args.n(r02),
                    Args.n(r10), Args.n(r11), Args.n(r12), Args.n(r20), Args.n(r21), Args.n(r22)};
            case 9: {
                double[] e = eulerXYZ();
                return new Object[] {Args.n(e[0]), Args.n(e[1]), Args.n(e[2])};
            }
            case 10:
            case 13: {
                double[] e = eulerYXZ();
                return new Object[] {Args.n(e[0]), Args.n(e[1]), Args.n(e[2])};
            }
            case 11: {
                double[] q = quat();
                double w = Math.max(-1, Math.min(1, q[3]));
                double ang = 2 * MathUtil.acos(w);
                double s = Math.sqrt(1 - w * w);
                Vector3 axis = s < 1e-6 ? Vector3.X_AXIS : new Vector3(q[0] / s, q[1] / s, q[2] / s);
                return new Object[] {axis, Args.n(ang)};
            }
            case 12: {
                double[] q = quat();
                return ret(fromQuat(x, y, z, q[0], q[1], q[2], q[3]));
            }
            case 15: {
                CFrame o = Args.cframe(a, 1, "FuzzyEq");
                double eps = Args.optNum(a, 2, 1e-5);
                return ret(Boolean.valueOf(near(o, eps)));
            }
            default:
                return super.invoke(id, in, a);
        }
    }

    private Object[] many(Object[] a, boolean world) {
        int n = Math.max(0, a.length - 1);
        Object[] r = new Object[n];
        for (int i = 0; i < n; i++) {
            CFrame c = Args.cframe(a, i + 1, world ? "ToWorldSpace" : "ToObjectSpace");
            r[i] = world ? mul(c) : inverse().mul(c);
        }
        return r;
    }

    boolean near(CFrame o, double eps) {
        return Math.abs(x - o.x) <= eps && Math.abs(y - o.y) <= eps && Math.abs(z - o.z) <= eps
                && Math.abs(r00 - o.r00) <= eps && Math.abs(r01 - o.r01) <= eps && Math.abs(r02 - o.r02) <= eps
                && Math.abs(r10 - o.r10) <= eps && Math.abs(r11 - o.r11) <= eps && Math.abs(r12 - o.r12) <= eps
                && Math.abs(r20 - o.r20) <= eps && Math.abs(r21 - o.r21) <= eps && Math.abs(r22 - o.r22) <= eps;
    }

    @Override
    public Object arith(String op, Object o, boolean selfLeft) {
        if (selfLeft) {
            if ("mul".equals(op) && o instanceof CFrame) {
                return mul((CFrame) o);
            }
            if ("mul".equals(op) && o instanceof Vector3) {
                return pointToWorld((Vector3) o);
            }
            if ("add".equals(op) && o instanceof Vector3) {
                return translate((Vector3) o);
            }
            if ("sub".equals(op) && o instanceof Vector3) {
                return translate(((Vector3) o).negateV());
            }
        }
        throw LuaError.rt("attempt to perform arithmetic (" + op + ") on CFrame and " + Interp.typeName(o));
    }

    @Override
    public boolean eq(Object o) {
        return o instanceof CFrame && near((CFrame) o, 0);
    }

    @Override
    public boolean equals(Object o) {
        return eq(o);
    }

    @Override
    public int hashCode() {
        long b = Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y) * 17 + Double.doubleToLongBits(z);
        return (int) (b ^ (b >>> 32));
    }

    @Override
    public String str() {
        StringBuilder b = new StringBuilder();
        double[] c = {x, y, z, r00, r01, r02, r10, r11, r12, r20, r21, r22};
        for (int i = 0; i < c.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(Interp.numberToString(c[i]));
        }
        return b.toString();
    }
}
