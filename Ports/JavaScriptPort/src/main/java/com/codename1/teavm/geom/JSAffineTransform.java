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
        @JSBody(params={"sx", "sy"}, script="return goog.graphics.AffineTransform.getScaleInstance(sx, sy)")
        private native static JSOAffineTransform getScaleInstance(double sx, double sy);

        @JSBody(params={"tx", "ty"}, script="return goog.graphics.AffineTransform.getTranslateInstance(tx, ty)")
        private native static JSOAffineTransform getTranslateInstance(double tx, double ty);

        @JSBody(params={"shx", "shy"}, script="return goog.graphics.AffineTransform.getShearInstance(shx, shy)")
        private native static JSOAffineTransform getShearInstance(double shx, double shy);

        @JSBody(params={"theta", "x", "y"}, script="return goog.graphics.AffineTransform.getRotateInstance(theta, x, y)")
        private native static JSOAffineTransform getRotateInstance(double theta, double x, double y);
        
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
