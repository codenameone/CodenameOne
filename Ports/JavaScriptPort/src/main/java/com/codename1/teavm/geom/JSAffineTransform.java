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

package com.codename1.teavm.geom;

import java.util.Objects;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

import com.codename1.html5.js.JSBody;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.typedarrays.Float64Array;

/// ParparVM's @JSBody receives JSO-interface parameters wrapped in a Java
/// object where the real JS value lives under __jsValue, so calling
/// `jso.someMethod()` from Java (or `t.someMethod()` inside a @JSBody script)
/// dispatches to the wrapper — which doesn't define those methods — and
/// silently returns undefined / no-op. Every helper in this file therefore
/// avoids JSO-method dispatch and either (a) reads/writes the m00..m12
/// fields directly on the unwrapped JS value or (b) routes through a
/// @JSBody that unwraps __jsValue before using it.
public class JSAffineTransform {
    static interface JSOAffineTransform extends JSObject {
    }


    static class JSOFactory {
        @JSBody(params={"m00", "m10", "m01", "m11", "m02", "m12"},
                script="var root = typeof window !== 'undefined' ? window : globalThis;\n"
                + "var make = root.__cn1AffineTransformFactory;\n"
                + "if (!make) {\n"
                + "  root.__cn1AffineTransformFactory = make = function(m00, m10, m01, m11, m02, m12) {\n"
                + "    return { m00: m00, m10: m10, m01: m01, m11: m11, m02: m02, m12: m12 };\n"
                + "  };\n"
                + "}\n"
                + "return make(m00, m10, m01, m11, m02, m12);")
        private native static JSOAffineTransform createTransform(double m00, double m10, double m01, double m11, double m02, double m12);

        private static JSOAffineTransform getScaleInstance(double sx, double sy) {
            return createTransform(sx, 0, 0, sy, 0, 0);
        }

        private static JSOAffineTransform getTranslateInstance(double tx, double ty) {
            return createTransform(1, 0, 0, 1, tx, ty);
        }

        private static JSOAffineTransform getShearInstance(double shx, double shy) {
            return createTransform(1, shy, shx, 1, 0, 0);
        }

        private static JSOAffineTransform getRotateInstance(double theta, double x, double y) {
            return rotateJso(getTranslateInstance(0, 0), theta, x, y);
        }

        @JSBody(params={"context", "t"}, script=
                "var jv = (t && t.__jsValue) ? t.__jsValue : t;\n"
                + "context.setTransform(jv.m00, jv.m10, jv.m01, jv.m11, jv.m02, jv.m12);")
        private native static void setTransform(CanvasRenderingContext2D context, JSOAffineTransform t);

        @JSBody(params={"context", "t"}, script=
                "var jv = (t && t.__jsValue) ? t.__jsValue : t;\n"
                + "context.transform(jv.m00, jv.m10, jv.m01, jv.m11, jv.m02, jv.m12);")
        private native static void transform(CanvasRenderingContext2D context, JSOAffineTransform t);
    }

    // Pure-JS helpers that operate on the underlying matrix object. All accept
    // either the Java-wrapped JSO (with .__jsValue) or the raw JS matrix.
    private static final String UNWRAP_JV = "var jv = (t && t.__jsValue) ? t.__jsValue : t;\n";
    private static final String UNWRAP_OTHER = "var ov = (o && o.__jsValue) ? o.__jsValue : o;\n";

    @JSBody(params = {"t"}, script = UNWRAP_JV + "return jv.m00;")
    private static native double jsoScaleX(JSOAffineTransform t);
    @JSBody(params = {"t"}, script = UNWRAP_JV + "return jv.m11;")
    private static native double jsoScaleY(JSOAffineTransform t);
    @JSBody(params = {"t"}, script = UNWRAP_JV + "return jv.m01;")
    private static native double jsoShearX(JSOAffineTransform t);
    @JSBody(params = {"t"}, script = UNWRAP_JV + "return jv.m10;")
    private static native double jsoShearY(JSOAffineTransform t);
    @JSBody(params = {"t"}, script = UNWRAP_JV + "return jv.m02;")
    private static native double jsoTranslateX(JSOAffineTransform t);
    @JSBody(params = {"t"}, script = UNWRAP_JV + "return jv.m12;")
    private static native double jsoTranslateY(JSOAffineTransform t);

    @JSBody(params = {"t"}, script = UNWRAP_JV
            + "return jv.m00 === 1 && jv.m10 === 0 && jv.m01 === 0 && jv.m11 === 1 && jv.m02 === 0 && jv.m12 === 0;")
    private static native boolean jsoIsIdentity(JSOAffineTransform t);

    @JSBody(params = {"t"}, script = UNWRAP_JV
            + "return jv.m00 * jv.m11 - jv.m01 * jv.m10;")
    private static native double jsoDeterminant(JSOAffineTransform t);

    @JSBody(params = {"t"}, script = UNWRAP_JV
            + "return '[' + jv.m00 + ',' + jv.m10 + ',' + jv.m01 + ',' + jv.m11 + ',' + jv.m02 + ',' + jv.m12 + ']';")
    private static native String jsoStringValue(JSOAffineTransform t);

    @JSBody(params = {"a", "b"}, script =
            "var av = (a && a.__jsValue) ? a.__jsValue : a;\n"
            + "var bv = (b && b.__jsValue) ? b.__jsValue : b;\n"
            + "return av.m00 === bv.m00 && av.m10 === bv.m10 && av.m01 === bv.m01\n"
            + "    && av.m11 === bv.m11 && av.m02 === bv.m02 && av.m12 === bv.m12;")
    private static native boolean jsoEquals(JSOAffineTransform a, JSOAffineTransform b);

    @JSBody(params = {"t"}, script = UNWRAP_JV
            + "return { m00: jv.m00, m10: jv.m10, m01: jv.m01, m11: jv.m11, m02: jv.m02, m12: jv.m12 };")
    private static native JSOAffineTransform jsoClone(JSOAffineTransform t);

    @JSBody(params = {"t", "a", "b", "c", "d", "e", "f"}, script = UNWRAP_JV
            + "jv.m00 = a; jv.m10 = b; jv.m01 = c; jv.m11 = d; jv.m02 = e; jv.m12 = f;")
    private static native void jsoSet(JSOAffineTransform t, double a, double b, double c, double d, double e, double f);

    @JSBody(params = {"t", "o"}, script = UNWRAP_JV + UNWRAP_OTHER
            + "jv.m00 = ov.m00; jv.m10 = ov.m10; jv.m01 = ov.m01;\n"
            + "jv.m11 = ov.m11; jv.m02 = ov.m02; jv.m12 = ov.m12;")
    private static native void jsoCopy(JSOAffineTransform t, JSOAffineTransform o);

    /// Compute (this * other) in-place. Equivalent to canvas's
    /// context.transform — appends `other` in the local frame.
    @JSBody(params = {"t", "o"}, script = UNWRAP_JV + UNWRAP_OTHER
            + "var m00 = jv.m00 * ov.m00 + jv.m01 * ov.m10;\n"
            + "var m10 = jv.m10 * ov.m00 + jv.m11 * ov.m10;\n"
            + "var m01 = jv.m00 * ov.m01 + jv.m01 * ov.m11;\n"
            + "var m11 = jv.m10 * ov.m01 + jv.m11 * ov.m11;\n"
            + "var m02 = jv.m00 * ov.m02 + jv.m01 * ov.m12 + jv.m02;\n"
            + "var m12 = jv.m10 * ov.m02 + jv.m11 * ov.m12 + jv.m12;\n"
            + "jv.m00 = m00; jv.m10 = m10; jv.m01 = m01; jv.m11 = m11; jv.m02 = m02; jv.m12 = m12;")
    private static native void jsoConcat(JSOAffineTransform t, JSOAffineTransform o);

    /// Compute (other * this) in-place.
    @JSBody(params = {"t", "o"}, script = UNWRAP_JV + UNWRAP_OTHER
            + "var m00 = ov.m00 * jv.m00 + ov.m01 * jv.m10;\n"
            + "var m10 = ov.m10 * jv.m00 + ov.m11 * jv.m10;\n"
            + "var m01 = ov.m00 * jv.m01 + ov.m01 * jv.m11;\n"
            + "var m11 = ov.m10 * jv.m01 + ov.m11 * jv.m11;\n"
            + "var m02 = ov.m00 * jv.m02 + ov.m01 * jv.m12 + ov.m02;\n"
            + "var m12 = ov.m10 * jv.m02 + ov.m11 * jv.m12 + ov.m12;\n"
            + "jv.m00 = m00; jv.m10 = m10; jv.m01 = m01; jv.m11 = m11; jv.m02 = m02; jv.m12 = m12;")
    private static native void jsoPreConcat(JSOAffineTransform t, JSOAffineTransform o);

    @JSBody(params = {"t", "tx", "ty"}, script = UNWRAP_JV
            + "jv.m02 = jv.m00 * tx + jv.m01 * ty + jv.m02;\n"
            + "jv.m12 = jv.m10 * tx + jv.m11 * ty + jv.m12;")
    private static native void jsoTranslate(JSOAffineTransform t, double tx, double ty);

    @JSBody(params = {"t", "sx", "sy"}, script = UNWRAP_JV
            + "jv.m00 *= sx; jv.m10 *= sx;\n"
            + "jv.m01 *= sy; jv.m11 *= sy;")
    private static native void jsoScale(JSOAffineTransform t, double sx, double sy);

    @JSBody(params = {"t", "shx", "shy"}, script = UNWRAP_JV
            + "var m00 = jv.m00 + jv.m01 * shy;\n"
            + "var m10 = jv.m10 + jv.m11 * shy;\n"
            + "var m01 = jv.m01 + jv.m00 * shx;\n"
            + "var m11 = jv.m11 + jv.m10 * shx;\n"
            + "jv.m00 = m00; jv.m10 = m10; jv.m01 = m01; jv.m11 = m11;")
    private static native void jsoShear(JSOAffineTransform t, double shx, double shy);

    @JSBody(params = {"t", "theta", "x", "y"}, script = UNWRAP_JV
            + "var cos = Math.cos(theta), sin = Math.sin(theta);\n"
            + "var m02 = jv.m00 * x + jv.m01 * y + jv.m02;\n"
            + "var m12 = jv.m10 * x + jv.m11 * y + jv.m12;\n"
            + "var m00 = jv.m00 * cos + jv.m01 * sin;\n"
            + "var m10 = jv.m10 * cos + jv.m11 * sin;\n"
            + "var m01 = jv.m00 * -sin + jv.m01 * cos;\n"
            + "var m11 = jv.m10 * -sin + jv.m11 * cos;\n"
            + "m02 = m02 + m00 * -x + m01 * -y;\n"
            + "m12 = m12 + m10 * -x + m11 * -y;\n"
            + "jv.m00 = m00; jv.m10 = m10; jv.m01 = m01; jv.m11 = m11; jv.m02 = m02; jv.m12 = m12;")
    private static native void jsoRotate(JSOAffineTransform t, double theta, double x, double y);

    @JSBody(params = {"t"}, script = UNWRAP_JV
            + "var det = jv.m00 * jv.m11 - jv.m01 * jv.m10;\n"
            + "if (!det) { return null; }\n"
            + "var invDet = 1 / det;\n"
            + "return { m00: jv.m11 * invDet, m10: -jv.m10 * invDet, m01: -jv.m01 * invDet,\n"
            + "    m11: jv.m00 * invDet, m02: (jv.m01 * jv.m12 - jv.m11 * jv.m02) * invDet,\n"
            + "    m12: (jv.m10 * jv.m02 - jv.m00 * jv.m12) * invDet };")
    private static native JSOAffineTransform jsoInverse(JSOAffineTransform t);

    @JSBody(params = {"t", "src", "srcOff", "dst", "dstOff", "numPts"}, script = UNWRAP_JV
            + "for (var i = 0; i < numPts; i++) {\n"
            + "  var x = src[srcOff + i * 2];\n"
            + "  var y = src[srcOff + i * 2 + 1];\n"
            + "  dst[dstOff + i * 2] = jv.m00 * x + jv.m01 * y + jv.m02;\n"
            + "  dst[dstOff + i * 2 + 1] = jv.m10 * x + jv.m11 * y + jv.m12;\n"
            + "}")
    private static native void jsoTransformPoints(JSOAffineTransform t, Float64Array src, int srcOff, Float64Array dst, int dstOff, int numPts);

    private static JSOAffineTransform rotateJso(JSOAffineTransform t, double theta, double x, double y) {
        jsoRotate(t, theta, x, y);
        return t;
    }

    public static class Factory {
        public static JSAffineTransform getScaleInstance(double sx, double sy) {
            return new JSAffineTransform(JSOFactory.getScaleInstance(sx, sy));
        }

        public  static JSAffineTransform getTranslateInstance(double tx, double ty) {
            return new JSAffineTransform(JSOFactory.getTranslateInstance(tx, ty));
        }

        public static JSAffineTransform getShearInstance(double shx, double shy) {
            return new JSAffineTransform(JSOFactory.getShearInstance(shx, shy));
        }

        public  static JSAffineTransform getRotateInstance(double theta, double x, double y) {
            return new JSAffineTransform(JSOFactory.getRotateInstance(theta, x, y));
        }

        public static void setTransform(CanvasRenderingContext2D context, JSAffineTransform t) {
            JSOFactory.setTransform(context, t.inner);
        }

        public static void transform(CanvasRenderingContext2D context, JSAffineTransform t) {
            JSOFactory.transform(context, t.inner);
        }

    }


    private JSOAffineTransform inner;
    /// Optional 4x4 backing (column-major, OpenGL ES convention) used for the
    /// perspective / camera / 3D-rotation path. {@code null} means this is a
    /// pure 2D affine and every operation stays on the (unchanged, JS-backed)
    /// {@code inner} matrix -- so non-perspective rendering is byte-for-byte
    /// identical to before. When non-null, {@code inner} is kept in sync with
    /// the affine projection of {@code m4} so the 2D render path (which reads
    /// the six affine accessors) still gets a sane matrix.
    private double[] m4;

    private JSAffineTransform(JSOAffineTransform jso) {
        inner = jso;
    }

    /// True when this transform carries a full 4x4 matrix (perspective/camera/3D).
    public boolean is3D() {
        return m4 != null;
    }

    /// Replace the backing with a 4x4 matrix (column-major) and sync the
    /// 2D affine projection onto {@code inner}.
    public JSAffineTransform setMatrix4(double[] m16) {
        m4 = m16;
        syncInnerFromM4();
        return this;
    }

    /// Return the 4x4 backing, promoting the current 2D affine to 4x4 first.
    public double[] getMatrix4() {
        ensure3D();
        return m4;
    }

    private void ensure3D() {
        if (m4 == null) {
            m4 = JSMatrix4.fromAffine(jsoScaleX(inner), jsoShearY(inner),
                    jsoShearX(inner), jsoScaleY(inner),
                    jsoTranslateX(inner), jsoTranslateY(inner));
        }
    }

    private void syncInnerFromM4() {
        // Affine projection of the 4x4 (drops the perspective / z rows): the
        // 2D render path consumes only these six components.
        jsoSet(inner, m4[0], m4[1], m4[4], m4[5], m4[12], m4[13]);
    }

    /// Post-multiply by another transform in 4x4 space (this = this * o).
    public JSAffineTransform concatenate3(JSAffineTransform o) {
        ensure3D();
        m4 = JSMatrix4.multiplyMM(m4, o.getMatrix4());
        syncInnerFromM4();
        return this;
    }

    public JSAffineTransform translate3(double x, double y, double z) {
        ensure3D();
        JSMatrix4.translateInPlace(m4, x, y, z);
        syncInnerFromM4();
        return this;
    }

    public JSAffineTransform scale3(double x, double y, double z) {
        ensure3D();
        JSMatrix4.scaleInPlace(m4, x, y, z);
        syncInnerFromM4();
        return this;
    }

    public JSAffineTransform rotate3(double angleRad, double x, double y, double z) {
        ensure3D();
        m4 = JSMatrix4.multiplyMM(m4, JSMatrix4.setRotate(angleRad, x, y, z));
        syncInnerFromM4();
        return this;
    }

    /// Project a point through the 4x4 matrix with the homogeneous divide.
    /// {@code out} receives x,y (and z if it has room).
    public void transformPoint3(float[] in, float[] out) {
        ensure3D();
        double[] v = new double[]{in[0], in[1], in.length > 2 ? in[2] : 0, 1};
        double[] r = JSMatrix4.multiplyMV(m4, v);
        double w = r[3];
        if (w != 0 && w != 1) {
            r[0] /= w; r[1] /= w; r[2] /= w;
        }
        out[0] = (float) r[0];
        out[1] = (float) r[1];
        if (out.length > 2) {
            out[2] = (float) r[2];
        }
    }

    public boolean isIdentity() {
        if (m4 != null) {
            return JSMatrix4.isIdentity(m4);
        }
        return jsoIsIdentity(inner);
    }
    public JSAffineTransform cloneTransform() {
        JSAffineTransform copy = new JSAffineTransform(jsoClone(inner));
        if (m4 != null) {
            copy.m4 = new double[16];
            System.arraycopy(m4, 0, copy.m4, 0, 16);
        }
        return copy;
    }
    public JSAffineTransform setTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        jsoSet(inner, m00, m10, m01, m11, m02, m12);
        m4 = null;
        return this;
    }
    public JSAffineTransform copyFrom(JSAffineTransform t) {
        jsoCopy(inner, t.inner);
        if (t.m4 != null) {
            m4 = new double[16];
            System.arraycopy(t.m4, 0, m4, 0, 16);
        } else {
            m4 = null;
        }
        return this;
    }
    public JSAffineTransform scale(double sx, double sy) {
        jsoScale(inner, sx, sy);
        return this;
    }
    public JSAffineTransform translate(double tx, double ty) {
        jsoTranslate(inner, tx, ty);
        return this;
    }
    public JSAffineTransform rotate(double theta, double x, double y) {
        jsoRotate(inner, theta, x, y);
        return this;
    }
    public JSAffineTransform shear(double shx, double shy) {
        jsoShear(inner, shx, shy);
        return this;
    }
    public double getScaleX() {
        return jsoScaleX(inner);
    }
    public double getScaleY() {
        return jsoScaleY(inner);
    }
    public double getTranslateX() {
        return jsoTranslateX(inner);
    }
    public double getTranslateY() {
        return jsoTranslateY(inner);
    }
    public double getShearX() {
        return jsoShearX(inner);
    }
    public double getShearY() {
        return jsoShearY(inner);
    }
    public JSAffineTransform concatenate(JSAffineTransform t) {
        jsoConcat(inner, t.inner);
        return this;
    }
    public JSAffineTransform preConcatenate(JSAffineTransform t) {
        jsoPreConcat(inner, t.inner);
        return this;
    }
    public void transform(Float64Array src, int srcOff, Float64Array dst, int dstOff, int numPts) {
        jsoTransformPoints(inner, src, srcOff, dst, dstOff, numPts);
    }
    public double getDeterminant() {
        return jsoDeterminant(inner);
    }
    public boolean isInvertible() {
        return jsoDeterminant(inner) != 0;
    }
    public JSAffineTransform createInverse() {
        JSOAffineTransform inv = jsoInverse(inner);
        if (inv == null) {
            // Match the legacy behaviour (return a NaN-filled matrix when
            // non-invertible) instead of throwing — a few callers rely on it.
            inv = JSOFactory.createTransform(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        return new JSAffineTransform(inv);
    }
    public JSAffineTransform setToScale(double sx, double sy) {
        jsoSet(inner, sx, 0, 0, sy, 0, 0);
        m4 = null;
        return this;
    }
    public JSAffineTransform setToTranslation(double tx, double ty) {
        jsoSet(inner, 1, 0, 0, 1, tx, ty);
        m4 = null;
        return this;
    }
    public JSAffineTransform setToShear(double shx, double shy) {
        jsoSet(inner, 1, shy, shx, 1, 0, 0);
        m4 = null;
        return this;
    }
    public JSAffineTransform setToRotation(double theta, double x, double y) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        jsoSet(inner, cos, sin, -sin, cos,
                x - cos * x + sin * y,
                y - sin * x - cos * y);
        m4 = null;
        return this;
    }
    public boolean isEqualTo(JSAffineTransform t) {
        if (t == null) return false;
        if (m4 != null || t.m4 != null) {
            double[] a = getMatrix4();
            double[] b = t.getMatrix4();
            for (int i = 0; i < 16; i++) {
                if (a[i] != b[i]) return false;
            }
            return true;
        }
        return jsoEquals(inner, t.inner);
    }

    @Override
    public String toString() {
        return jsoStringValue(inner);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JSAffineTransform) {
            return jsoEquals(inner, ((JSAffineTransform)obj).inner);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(toString());
        return hash;
    }
}
