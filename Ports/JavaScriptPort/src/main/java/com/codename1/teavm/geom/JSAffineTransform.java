/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.geom;

import java.util.Objects;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

import com.codename1.html5.js.JSBody;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.typedarrays.Float64Array;

/**
 *
 * @author shannah
 */
public class JSAffineTransform {
    static interface JSOAffineTransform extends JSObject {

        public String stringValue();
        public boolean isIdentity();
        public JSOAffineTransform cloneTransform();
        public JSOAffineTransform setTransform(double m00, double m10, double m01, double m11, double m02, double m12);
        public JSOAffineTransform copyFrom(JSOAffineTransform t);
        public JSOAffineTransform scale(double sx, double sy);
        public JSOAffineTransform translate(double tx, double ty);
        public JSOAffineTransform rotate(double theta, double x, double y);
        public JSOAffineTransform shear(double shx, double shy);
        public double getScaleX();
        public double getScaleY();
        public double getTranslateX();
        public double getTranslateY();
        public double getShearX();
        public double getShearY();
        public JSOAffineTransform concatenate(JSOAffineTransform t);
        public JSOAffineTransform preConcatenate(JSOAffineTransform t);
        public void transform(Float64Array src, int srcOff, Float64Array dst, int dstOff, int numPts);
        public double getDeterminant();
        public boolean isInvertible();
        public JSOAffineTransform createInverse();
        public JSOAffineTransform setToScale(double sx, double sy);
        public JSOAffineTransform setToTranslation(double tx, double ty);
        public JSOAffineTransform setToShear(double shx, double shy);
        public JSOAffineTransform setToRotation(double theta, double x, double y);
        public boolean isEqualTo(JSOAffineTransform t);
    }
    
    
    static class JSOFactory {
        @JSBody(params={"m00", "m10", "m01", "m11", "m02", "m12"},
                script="var root = typeof window !== 'undefined' ? window : globalThis;\n"
                + "var make = root.__cn1AffineTransformFactory;\n"
                + "if (!make) {\n"
                + "  root.__cn1AffineTransformFactory = make = function(m00, m10, m01, m11, m02, m12) {\n"
                + "    function multiply(left, right) {\n"
                + "      return {\n"
                + "        m00: left.m00 * right.m00 + left.m01 * right.m10,\n"
                + "        m10: left.m10 * right.m00 + left.m11 * right.m10,\n"
                + "        m01: left.m00 * right.m01 + left.m01 * right.m11,\n"
                + "        m11: left.m10 * right.m01 + left.m11 * right.m11,\n"
                + "        m02: left.m00 * right.m02 + left.m01 * right.m12 + left.m02,\n"
                + "        m12: left.m10 * right.m02 + left.m11 * right.m12 + left.m12\n"
                + "      };\n"
                + "    }\n"
                + "    function inverseOf(t) {\n"
                + "      var det = t.m00 * t.m11 - t.m01 * t.m10;\n"
                + "      if (!det) {\n"
                + "        return null;\n"
                + "      }\n"
                + "      var invDet = 1 / det;\n"
                + "      return {\n"
                + "        m00: t.m11 * invDet,\n"
                + "        m10: -t.m10 * invDet,\n"
                + "        m01: -t.m01 * invDet,\n"
                + "        m11: t.m00 * invDet,\n"
                + "        m02: (t.m01 * t.m12 - t.m11 * t.m02) * invDet,\n"
                + "        m12: (t.m10 * t.m02 - t.m00 * t.m12) * invDet\n"
                + "      };\n"
                + "    }\n"
                + "    function copyInto(target, values) {\n"
                + "      target.m00 = values.m00; target.m10 = values.m10; target.m01 = values.m01;\n"
                + "      target.m11 = values.m11; target.m02 = values.m02; target.m12 = values.m12;\n"
                + "      return target;\n"
                + "    }\n"
                + "    var t = {\n"
                + "      m00: m00, m10: m10, m01: m01, m11: m11, m02: m02, m12: m12,\n"
                + "      stringValue: function() { return '[' + this.m00 + ',' + this.m10 + ',' + this.m01 + ',' + this.m11 + ',' + this.m02 + ',' + this.m12 + ']'; },\n"
                + "      isIdentity: function() { return this.m00 === 1 && this.m10 === 0 && this.m01 === 0 && this.m11 === 1 && this.m02 === 0 && this.m12 === 0; },\n"
                + "      cloneTransform: function() { return make(this.m00, this.m10, this.m01, this.m11, this.m02, this.m12); },\n"
                + "      setTransform: function(a, b, c, d, e, f) { this.m00 = a; this.m10 = b; this.m01 = c; this.m11 = d; this.m02 = e; this.m12 = f; return this; },\n"
                + "      copyFrom: function(other) { return this.setTransform(other.m00, other.m10, other.m01, other.m11, other.m02, other.m12); },\n"
                + "      scale: function(sx, sy) { return this.concatenate(make(sx, 0, 0, sy, 0, 0)); },\n"
                + "      translate: function(tx, ty) { return this.concatenate(make(1, 0, 0, 1, tx, ty)); },\n"
                + "      rotate: function(theta, x, y) {\n"
                + "        x = x || 0; y = y || 0;\n"
                + "        var cos = Math.cos(theta), sin = Math.sin(theta);\n"
                + "        return this.concatenate(make(1, 0, 0, 1, x, y)).concatenate(make(cos, sin, -sin, cos, 0, 0)).concatenate(make(1, 0, 0, 1, -x, -y));\n"
                + "      },\n"
                + "      shear: function(shx, shy) { return this.concatenate(make(1, shy, shx, 1, 0, 0)); },\n"
                + "      getScaleX: function() { return this.m00; },\n"
                + "      getScaleY: function() { return this.m11; },\n"
                + "      getTranslateX: function() { return this.m02; },\n"
                + "      getTranslateY: function() { return this.m12; },\n"
                + "      getShearX: function() { return this.m01; },\n"
                + "      getShearY: function() { return this.m10; },\n"
                + "      concatenate: function(other) { return copyInto(this, multiply(this, other)); },\n"
                + "      preConcatenate: function(other) { return copyInto(this, multiply(other, this)); },\n"
                + "      transform: function(src, srcOff, dst, dstOff, numPts) {\n"
                + "        for (var i = 0; i < numPts; i++) {\n"
                + "          var x = src[srcOff + i * 2];\n"
                + "          var y = src[srcOff + i * 2 + 1];\n"
                + "          dst[dstOff + i * 2] = this.m00 * x + this.m01 * y + this.m02;\n"
                + "          dst[dstOff + i * 2 + 1] = this.m10 * x + this.m11 * y + this.m12;\n"
                + "        }\n"
                + "      },\n"
                + "      getDeterminant: function() { return this.m00 * this.m11 - this.m01 * this.m10; },\n"
                + "      isInvertible: function() { return !!this.getDeterminant(); },\n"
                + "      createInverse: function() {\n"
                + "        var inv = inverseOf(this);\n"
                + "        return inv ? make(inv.m00, inv.m10, inv.m01, inv.m11, inv.m02, inv.m12) : make(NaN, NaN, NaN, NaN, NaN, NaN);\n"
                + "      },\n"
                + "      setToScale: function(sx, sy) { return this.setTransform(sx, 0, 0, sy, 0, 0); },\n"
                + "      setToTranslation: function(tx, ty) { return this.setTransform(1, 0, 0, 1, tx, ty); },\n"
                + "      setToShear: function(shx, shy) { return this.setTransform(1, shy, shx, 1, 0, 0); },\n"
                + "      setToRotation: function(theta, x, y) {\n"
                + "        x = x || 0; y = y || 0;\n"
                + "        var cos = Math.cos(theta), sin = Math.sin(theta);\n"
                + "        return this.setTransform(cos, sin, -sin, cos, x - cos * x + sin * y, y - sin * x - cos * y);\n"
                + "      },\n"
                + "      isEqualTo: function(other) {\n"
                + "        return !!other && this.m00 === other.m00 && this.m10 === other.m10 && this.m01 === other.m01 && this.m11 === other.m11 && this.m02 === other.m02 && this.m12 === other.m12;\n"
                + "      }\n"
                + "    };\n"
                + "    return t;\n"
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
            return createTransform(1, 0, 0, 1, 0, 0).rotate(theta, x, y);
        }
        
        @JSBody(params={"context", "t"}, script="context.setTransform(t.getScaleX(), t.getShearY(), t.getShearX(), t.getScaleY(), t.getTranslateX(), t.getTranslateY())")
        private native static void setTransform(CanvasRenderingContext2D context, JSOAffineTransform t);
        
        @JSBody(params={"context", "t"}, script="context.transform(t.getScaleX(), t.getShearY(), t.getShearX(), t.getScaleY(), t.getTranslateX(), t.getTranslateY())")
        private native static void transform(CanvasRenderingContext2D context, JSOAffineTransform t);
    
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
    private JSAffineTransform(JSOAffineTransform jso) {
        inner = jso;
    }
    public boolean isIdentity() {
        return inner.isIdentity();
    }
    public JSAffineTransform cloneTransform() {
        return new JSAffineTransform(inner.cloneTransform());
    }
    public JSAffineTransform setTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        inner.setTransform(m00, m10, m01, m11, m02, m12);
        return this;
    }
    public JSAffineTransform copyFrom(JSAffineTransform t) {
        inner.copyFrom(t.inner);
        return this;
    }
    public JSAffineTransform scale(double sx, double sy) {
        inner.scale(sx, sy);
        return this;
    }
    public JSAffineTransform translate(double tx, double ty) {
        inner.translate(tx, ty);
        return this;
    }
    public JSAffineTransform rotate(double theta, double x, double y) {
        inner.rotate(theta, x, y);
        return this;
    }
    public JSAffineTransform shear(double shx, double shy) {
        inner.shear(shx, shy);
        return this;
    }
    public double getScaleX() {
        return inner.getScaleX();
    }
    public double getScaleY() {
        return inner.getScaleY();
    }
    public double getTranslateX() {
        return inner.getTranslateX();
    }
    public double getTranslateY() {
        return inner.getTranslateY();
    }
    public double getShearX() {
        return inner.getShearX();
    }
    public double getShearY() {
        return inner.getShearY();
    }
    public JSAffineTransform concatenate(JSAffineTransform t) {
        inner.concatenate(t.inner);
        return this;

    }
    public JSAffineTransform preConcatenate(JSAffineTransform t) {
        inner.preConcatenate(t.inner);
        return this;
    }
    public void transform(Float64Array src, int srcOff, Float64Array dst, int dstOff, int numPts) {
        inner.transform(src, srcOff, dst, dstOff, numPts);
    }
    public double getDeterminant() {
        return inner.getDeterminant();
    }
    public boolean isInvertible() {
        return inner.isInvertible();
    }
    public JSAffineTransform createInverse() {
        return new JSAffineTransform(inner.createInverse());
    }
    public JSAffineTransform setToScale(double sx, double sy) {
        inner.setToScale(sx, sy);
        return this;
    }
    public JSAffineTransform setToTranslation(double tx, double ty) {
        inner.setToTranslation(tx, ty);
        return this;
    }
    public JSAffineTransform setToShear(double shx, double shy) {
        inner.setToShear(shx, shy);
        return this;
    }
    public JSAffineTransform setToRotation(double theta, double x, double y) {
        inner.setToRotation(theta, x, y);
        return this;
    }
    public boolean isEqualTo(JSAffineTransform t) {
        if (t == null) return false;
        return inner.isEqualTo(t.inner);
    }

    @Override
    public String toString() {
        return inner.stringValue();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JSAffineTransform) {
            return inner.isEqualTo(((JSAffineTransform)obj).inner);
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
