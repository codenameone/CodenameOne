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
package com.codename1.desktopcompat.java.awt.geom;

import com.codename1.desktopcompat.java.awt.Shape;

/// A linear mapping of the plane that keeps straight lines straight and
/// parallel lines parallel: any mix of translation, scaling, rotation and
/// shearing.
///
/// A point `(x, y)` maps to `(m00*x + m01*y + m02, m10*x + m11*y + m12)`.
/// Rotations by an exact multiple of a quarter turn store exact zeros and
/// ones, so [#getType()] reports them as quadrant rotations.
public class AffineTransform implements Cloneable {

    public static final int TYPE_IDENTITY = 0;
    public static final int TYPE_TRANSLATION = 1;
    public static final int TYPE_UNIFORM_SCALE = 2;
    public static final int TYPE_GENERAL_SCALE = 4;
    public static final int TYPE_MASK_SCALE = TYPE_UNIFORM_SCALE | TYPE_GENERAL_SCALE;
    public static final int TYPE_FLIP = 64;
    public static final int TYPE_QUADRANT_ROTATION = 8;
    public static final int TYPE_GENERAL_ROTATION = 16;
    public static final int TYPE_MASK_ROTATION = TYPE_QUADRANT_ROTATION | TYPE_GENERAL_ROTATION;
    public static final int TYPE_GENERAL_TRANSFORM = 32;

    private double m00;
    private double m10;
    private double m01;
    private double m11;
    private double m02;
    private double m12;

    public AffineTransform() {
        m00 = 1.0;
        m11 = 1.0;
    }

    public AffineTransform(AffineTransform tx) {
        this(tx.m00, tx.m10, tx.m01, tx.m11, tx.m02, tx.m12);
    }

    public AffineTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
        this((double) m00, (double) m10, (double) m01, (double) m11, (double) m02, (double) m12);
    }

    /// Reads four or six values in the order m00, m10, m01, m11, m02, m12.
    public AffineTransform(float[] flatmatrix) {
        m00 = flatmatrix[0];
        m10 = flatmatrix[1];
        m01 = flatmatrix[2];
        m11 = flatmatrix[3];
        if (flatmatrix.length > 5) {
            m02 = flatmatrix[4];
            m12 = flatmatrix[5];
        }
    }

    public AffineTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    /// Reads four or six values in the order m00, m10, m01, m11, m02, m12.
    public AffineTransform(double[] flatmatrix) {
        m00 = flatmatrix[0];
        m10 = flatmatrix[1];
        m01 = flatmatrix[2];
        m11 = flatmatrix[3];
        if (flatmatrix.length > 5) {
            m02 = flatmatrix[4];
            m12 = flatmatrix[5];
        }
    }

    public static AffineTransform getTranslateInstance(double tx, double ty) {
        AffineTransform t = new AffineTransform();
        t.setToTranslation(tx, ty);
        return t;
    }

    public static AffineTransform getRotateInstance(double theta) {
        AffineTransform t = new AffineTransform();
        t.setToRotation(theta);
        return t;
    }

    public static AffineTransform getRotateInstance(double theta, double anchorx, double anchory) {
        AffineTransform t = new AffineTransform();
        t.setToRotation(theta, anchorx, anchory);
        return t;
    }

    public static AffineTransform getRotateInstance(double vecx, double vecy) {
        AffineTransform t = new AffineTransform();
        t.setToRotation(vecx, vecy);
        return t;
    }

    public static AffineTransform getRotateInstance(double vecx, double vecy, double anchorx, double anchory) {
        AffineTransform t = new AffineTransform();
        t.setToRotation(vecx, vecy, anchorx, anchory);
        return t;
    }

    public static AffineTransform getQuadrantRotateInstance(int numquadrants) {
        AffineTransform t = new AffineTransform();
        t.setToQuadrantRotation(numquadrants);
        return t;
    }

    public static AffineTransform getQuadrantRotateInstance(int numquadrants, double anchorx, double anchory) {
        AffineTransform t = new AffineTransform();
        t.setToQuadrantRotation(numquadrants, anchorx, anchory);
        return t;
    }

    public static AffineTransform getScaleInstance(double sx, double sy) {
        AffineTransform t = new AffineTransform();
        t.setToScale(sx, sy);
        return t;
    }

    public static AffineTransform getShearInstance(double shx, double shy) {
        AffineTransform t = new AffineTransform();
        t.setToShear(shx, shy);
        return t;
    }

    /// Classifies the matrix as a combination of the `TYPE_` flags. The
    /// answer is computed from the six values on every call.
    ///
    /// As in the JDK, a mirrored rotation that is not a plain flip of one
    /// axis always carries a scale flag, uniform when nothing else applies,
    /// and `TYPE_GENERAL_TRANSFORM` is reported on its own, without the
    /// translation flag.
    ///
    /// One deliberate difference: a rotation whose sine and cosine squares
    /// sum to one only within rounding is a pure rotation here however the
    /// matrix was made. The JDK remembers how a transform was built, so it
    /// says the same of one made by a rotation method but reports a uniform
    /// scale for the identical values passed to a constructor.
    public int getType() {
        if (m00 * m01 + m10 * m11 != 0.0) {
            // the images of the two axes are no longer perpendicular
            return TYPE_GENERAL_TRANSFORM;
        }
        int type = (m02 != 0.0 || m12 != 0.0) ? TYPE_TRANSLATION : TYPE_IDENTITY;
        if (m01 == 0.0 && m10 == 0.0) {
            boolean xPositive = m00 >= 0.0;
            if (xPositive == (m11 >= 0.0)) {
                if (!xPositive) {
                    // both axes reversed: a half turn
                    type |= TYPE_QUADRANT_ROTATION;
                }
                type |= scaleFlags(m00 != m11, m00);
            } else {
                type |= TYPE_FLIP | scaleFlags(m00 != -m11, m00);
            }
        } else if (m00 == 0.0 && m11 == 0.0) {
            type |= TYPE_QUADRANT_ROTATION;
            if ((m01 >= 0.0) != (m10 >= 0.0)) {
                type |= scaleFlags(m01 != -m10, m01);
            } else {
                type |= TYPE_FLIP | (m01 != m10 ? TYPE_GENERAL_SCALE : TYPE_UNIFORM_SCALE);
            }
        } else {
            type |= TYPE_GENERAL_ROTATION;
            if ((m00 >= 0.0) == (m11 >= 0.0)) {
                if (m00 != m11 || m01 != -m10) {
                    type |= TYPE_GENERAL_SCALE;
                } else if (Math.abs(m00 * m11 - m01 * m10 - 1.0) > ROUNDING) {
                    type |= TYPE_UNIFORM_SCALE;
                }
            } else {
                type |= TYPE_FLIP | (m00 != -m11 || m01 != m10 ? TYPE_GENERAL_SCALE : TYPE_UNIFORM_SCALE);
            }
        }
        return type;
    }

    /// How far the determinant of a rotation may stray from one through
    /// rounding of its sine and cosine alone.
    private static final double ROUNDING = 1e-14;

    private static int scaleFlags(boolean uneven, double factor) {
        if (uneven) {
            return TYPE_GENERAL_SCALE;
        }
        return factor != 1.0 && factor != -1.0 ? TYPE_UNIFORM_SCALE : 0;
    }

    public double getDeterminant() {
        return m00 * m11 - m01 * m10;
    }

    /// Stores m00, m10, m01, m11 and, when the array has room for six
    /// values, m02 and m12.
    public void getMatrix(double[] flatmatrix) {
        flatmatrix[0] = m00;
        flatmatrix[1] = m10;
        flatmatrix[2] = m01;
        flatmatrix[3] = m11;
        if (flatmatrix.length > 5) {
            flatmatrix[4] = m02;
            flatmatrix[5] = m12;
        }
    }

    public double getScaleX() {
        return m00;
    }

    public double getScaleY() {
        return m11;
    }

    public double getShearX() {
        return m01;
    }

    public double getShearY() {
        return m10;
    }

    public double getTranslateX() {
        return m02;
    }

    public double getTranslateY() {
        return m12;
    }

    public void translate(double tx, double ty) {
        m02 = tx * m00 + ty * m01 + m02;
        m12 = tx * m10 + ty * m11 + m12;
    }

    public void rotate(double theta) {
        rotateBy(sine(theta), cosine(theta));
    }

    public void rotate(double theta, double anchorx, double anchory) {
        translate(anchorx, anchory);
        rotate(theta);
        translate(-anchorx, -anchory);
    }

    /// Rotates by the angle of the vector; a zero vector leaves the
    /// transform unchanged.
    public void rotate(double vecx, double vecy) {
        double len = vectorLength(vecx, vecy);
        if (len != 0.0) {
            rotateBy(vectorSine(vecx, vecy, len), vectorCosine(vecx, vecy, len));
        }
    }

    public void rotate(double vecx, double vecy, double anchorx, double anchory) {
        translate(anchorx, anchory);
        rotate(vecx, vecy);
        translate(-anchorx, -anchory);
    }

    public void quadrantRotate(int numquadrants) {
        rotateBy(QUADRANT_SINE[numquadrants & 3], QUADRANT_SINE[(numquadrants + 1) & 3]);
    }

    public void quadrantRotate(int numquadrants, double anchorx, double anchory) {
        translate(anchorx, anchory);
        quadrantRotate(numquadrants);
        translate(-anchorx, -anchory);
    }

    public void scale(double sx, double sy) {
        m00 *= sx;
        m10 *= sx;
        m01 *= sy;
        m11 *= sy;
    }

    public void shear(double shx, double shy) {
        double a = m00;
        double b = m01;
        m00 = a + b * shy;
        m01 = a * shx + b;
        a = m10;
        b = m11;
        m10 = a + b * shy;
        m11 = a * shx + b;
    }

    public void setToIdentity() {
        setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0);
    }

    public void setToTranslation(double tx, double ty) {
        setTransform(1.0, 0.0, 0.0, 1.0, tx, ty);
    }

    public void setToRotation(double theta) {
        double sin = sine(theta);
        double cos = cosine(theta);
        setTransform(cos, sin, -sin, cos, 0.0, 0.0);
    }

    public void setToRotation(double theta, double anchorx, double anchory) {
        setToRotation(theta);
        anchor(anchorx, anchory);
    }

    /// Sets a rotation by the angle of the vector; a zero vector gives the
    /// identity.
    public void setToRotation(double vecx, double vecy) {
        double len = vectorLength(vecx, vecy);
        if (len == 0.0) {
            setToIdentity();
        } else {
            double sin = vectorSine(vecx, vecy, len);
            double cos = vectorCosine(vecx, vecy, len);
            setTransform(cos, sin, -sin, cos, 0.0, 0.0);
        }
    }

    public void setToRotation(double vecx, double vecy, double anchorx, double anchory) {
        setToRotation(vecx, vecy);
        anchor(anchorx, anchory);
    }

    public void setToQuadrantRotation(int numquadrants) {
        double sin = QUADRANT_SINE[numquadrants & 3];
        double cos = QUADRANT_SINE[(numquadrants + 1) & 3];
        setTransform(cos, sin, -sin, cos, 0.0, 0.0);
    }

    public void setToQuadrantRotation(int numquadrants, double anchorx, double anchory) {
        setToQuadrantRotation(numquadrants);
        anchor(anchorx, anchory);
    }

    public void setToScale(double sx, double sy) {
        setTransform(sx, 0.0, 0.0, sy, 0.0, 0.0);
    }

    public void setToShear(double shx, double shy) {
        setTransform(1.0, shy, shx, 1.0, 0.0, 0.0);
    }

    public void setTransform(AffineTransform tx) {
        setTransform(tx.m00, tx.m10, tx.m01, tx.m11, tx.m02, tx.m12);
    }

    public void setTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    /// Makes this transform apply `tx` first and then what it did before.
    public void concatenate(AffineTransform tx) {
        setTransform(
                m00 * tx.m00 + m01 * tx.m10,
                m10 * tx.m00 + m11 * tx.m10,
                m00 * tx.m01 + m01 * tx.m11,
                m10 * tx.m01 + m11 * tx.m11,
                m00 * tx.m02 + m01 * tx.m12 + m02,
                m10 * tx.m02 + m11 * tx.m12 + m12);
    }

    /// Makes this transform do what it did before and then apply `tx`.
    public void preConcatenate(AffineTransform tx) {
        setTransform(
                tx.m00 * m00 + tx.m01 * m10,
                tx.m10 * m00 + tx.m11 * m10,
                tx.m00 * m01 + tx.m01 * m11,
                tx.m10 * m01 + tx.m11 * m11,
                tx.m00 * m02 + tx.m01 * m12 + tx.m02,
                tx.m10 * m02 + tx.m11 * m12 + tx.m12);
    }

    public AffineTransform createInverse() throws NoninvertibleTransformException {
        AffineTransform t = new AffineTransform(this);
        t.invert();
        return t;
    }

    public void invert() throws NoninvertibleTransformException {
        double det = invertibleDeterminant();
        setTransform(
                m11 / det,
                -m10 / det,
                -m01 / det,
                m00 / det,
                (m01 * m12 - m11 * m02) / det,
                (m10 * m02 - m00 * m12) / det);
    }

    private double invertibleDeterminant() throws NoninvertibleTransformException {
        double det = getDeterminant();
        if (det == 0.0 || Double.isNaN(det)) {
            throw new NoninvertibleTransformException("Determinant is " + det);
        }
        return det;
    }

    /// Maps a point. When `ptDst` is null a new point is created, double
    /// precision if the source is and single precision otherwise.
    public Point2D transform(Point2D ptSrc, Point2D ptDst) {
        if (ptDst == null) {
            ptDst = newPointLike(ptSrc);
        }
        double x = ptSrc.getX();
        double y = ptSrc.getY();
        ptDst.setLocation(x * m00 + y * m01 + m02, x * m10 + y * m11 + m12);
        return ptDst;
    }

    private static Point2D newPointLike(Point2D p) {
        if (p instanceof Point2D.Double) {
            return new Point2D.Double();
        }
        return new Point2D.Float();
    }

    public void transform(Point2D[] ptSrc, int srcOff, Point2D[] ptDst, int dstOff, int numPts) {
        for (int i = 0; i < numPts; i++) {
            Point2D src = ptSrc[srcOff + i];
            ptDst[dstOff + i] = transform(src, ptDst[dstOff + i]);
        }
    }

    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
        if (dstPts == srcPts && dstOff > srcOff && dstOff < srcOff + numPts * 2) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            srcOff = dstOff;
        }
        for (int i = 0; i < numPts; i++) {
            double x = srcPts[srcOff++];
            double y = srcPts[srcOff++];
            dstPts[dstOff++] = (float) (x * m00 + y * m01 + m02);
            dstPts[dstOff++] = (float) (x * m10 + y * m11 + m12);
        }
    }

    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        if (dstPts == srcPts && dstOff > srcOff && dstOff < srcOff + numPts * 2) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            srcOff = dstOff;
        }
        for (int i = 0; i < numPts; i++) {
            double x = srcPts[srcOff++];
            double y = srcPts[srcOff++];
            dstPts[dstOff++] = x * m00 + y * m01 + m02;
            dstPts[dstOff++] = x * m10 + y * m11 + m12;
        }
    }

    public void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        for (int i = 0; i < numPts; i++) {
            double x = srcPts[srcOff++];
            double y = srcPts[srcOff++];
            dstPts[dstOff++] = x * m00 + y * m01 + m02;
            dstPts[dstOff++] = x * m10 + y * m11 + m12;
        }
    }

    public void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
        for (int i = 0; i < numPts; i++) {
            double x = srcPts[srcOff++];
            double y = srcPts[srcOff++];
            dstPts[dstOff++] = (float) (x * m00 + y * m01 + m02);
            dstPts[dstOff++] = (float) (x * m10 + y * m11 + m12);
        }
    }

    public Point2D inverseTransform(Point2D ptSrc, Point2D ptDst) throws NoninvertibleTransformException {
        double det = invertibleDeterminant();
        if (ptDst == null) {
            ptDst = newPointLike(ptSrc);
        }
        double x = ptSrc.getX() - m02;
        double y = ptSrc.getY() - m12;
        ptDst.setLocation((x * m11 - y * m01) / det, (y * m00 - x * m10) / det);
        return ptDst;
    }

    public void inverseTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts)
            throws NoninvertibleTransformException {
        double det = invertibleDeterminant();
        if (dstPts == srcPts && dstOff > srcOff && dstOff < srcOff + numPts * 2) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            srcOff = dstOff;
        }
        for (int i = 0; i < numPts; i++) {
            double x = srcPts[srcOff++] - m02;
            double y = srcPts[srcOff++] - m12;
            dstPts[dstOff++] = (x * m11 - y * m01) / det;
            dstPts[dstOff++] = (y * m00 - x * m10) / det;
        }
    }

    /// Maps a vector: like [#transform(Point2D, Point2D)] without the
    /// translation.
    public Point2D deltaTransform(Point2D ptSrc, Point2D ptDst) {
        if (ptDst == null) {
            ptDst = newPointLike(ptSrc);
        }
        double x = ptSrc.getX();
        double y = ptSrc.getY();
        ptDst.setLocation(x * m00 + y * m01, x * m10 + y * m11);
        return ptDst;
    }

    public void deltaTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        if (dstPts == srcPts && dstOff > srcOff && dstOff < srcOff + numPts * 2) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            srcOff = dstOff;
        }
        for (int i = 0; i < numPts; i++) {
            double x = srcPts[srcOff++];
            double y = srcPts[srcOff++];
            dstPts[dstOff++] = x * m00 + y * m01;
            dstPts[dstOff++] = x * m10 + y * m11;
        }
    }

    /// Returns the transformed outline of a shape as a double precision
    /// path, or null for a null shape.
    public Shape createTransformedShape(Shape pSrc) {
        if (pSrc == null) {
            return null;
        }
        return new Path2D.Double(pSrc, this);
    }

    @Override
    public String toString() {
        return "AffineTransform[[" + m00 + ", " + m01 + ", " + m02 + "], ["
                + m10 + ", " + m11 + ", " + m12 + "]]";
    }

    public boolean isIdentity() {
        return m00 == 1.0 && m11 == 1.0 && m01 == 0.0 && m10 == 0.0 && m02 == 0.0 && m12 == 0.0;
    }

    @Override
    public Object clone() {
        return new AffineTransform(this);
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(m00);
        bits = bits * 31 + Double.doubleToLongBits(m01);
        bits = bits * 31 + Double.doubleToLongBits(m02);
        bits = bits * 31 + Double.doubleToLongBits(m10);
        bits = bits * 31 + Double.doubleToLongBits(m11);
        bits = bits * 31 + Double.doubleToLongBits(m12);
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AffineTransform)) {
            return false;
        }
        AffineTransform a = (AffineTransform) obj;
        return m00 == a.m00 && m01 == a.m01 && m02 == a.m02 && m10 == a.m10 && m11 == a.m11 && m12 == a.m12;
    }

    /// Sines of 0, 1, 2 and 3 quarter turns; the cosine of n quarter turns is
    /// the sine of n + 1.
    private static final double[] QUADRANT_SINE = {0.0, 1.0, 0.0, -1.0};

    /// The sine of an angle, exactly zero when the angle is a half turn as
    /// closely as a double can say.
    private static double sine(double theta) {
        double sin = Math.sin(theta);
        if (sin != 1.0 && sin != -1.0) {
            double cos = Math.cos(theta);
            if (cos == 1.0 || cos == -1.0) {
                return 0.0;
            }
        }
        return sin;
    }

    /// The cosine of an angle, exactly zero when its sine is exactly one.
    private static double cosine(double theta) {
        double sin = Math.sin(theta);
        if (sin == 1.0 || sin == -1.0) {
            return 0.0;
        }
        return Math.cos(theta);
    }

    /// The length of a direction vector, or zero for a vector that names no
    /// direction. An axis aligned vector has length one whatever its size, so
    /// the sine and cosine derived from it are exact.
    private static double vectorLength(double vecx, double vecy) {
        if (vecy == 0.0) {
            return vecx < 0.0 ? 1.0 : (vecx > 0.0 ? 1.0 : 0.0);
        }
        if (vecx == 0.0) {
            return 1.0;
        }
        return Math.sqrt(vecx * vecx + vecy * vecy);
    }

    private static double vectorSine(double vecx, double vecy, double len) {
        if (vecy == 0.0) {
            return 0.0;
        }
        if (vecx == 0.0) {
            return vecy > 0.0 ? 1.0 : -1.0;
        }
        return vecy / len;
    }

    private static double vectorCosine(double vecx, double vecy, double len) {
        if (vecx == 0.0) {
            return 0.0;
        }
        if (vecy == 0.0) {
            return vecx > 0.0 ? 1.0 : -1.0;
        }
        return vecx / len;
    }

    /// Concatenates a rotation given by its sine and cosine.
    private void rotateBy(double sin, double cos) {
        double a = m00;
        double b = m01;
        m00 = cos * a + sin * b;
        m01 = -sin * a + cos * b;
        a = m10;
        b = m11;
        m10 = cos * a + sin * b;
        m11 = -sin * a + cos * b;
    }

    /// Moves the fixed point of the current rotation from the origin to the
    /// given anchor.
    private void anchor(double x, double y) {
        m02 = x - (m00 * x + m01 * y);
        m12 = y - (m10 * x + m11 * y);
    }
}
