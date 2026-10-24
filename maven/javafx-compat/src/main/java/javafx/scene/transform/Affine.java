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
package javafx.scene.transform;

import com.codename1.fxcompat.runtime.Matrix2D;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point2D;

/// A general affine transform whose six elements are properties, with
/// methods that combine it with other transforms in place.
///
/// To `append` a transform is to apply it before this one (the matrix is
/// multiplied on the right, as adding it after this one to a node's
/// list would); to `prepend` is to apply it after.
///
/// The z elements, the three dimensional overloads and the array and
/// `MatrixType` forms of JavaFX's `Affine` are absent.
public class Affine extends Transform {

    private final DoubleProperty mxx = new TDouble(this, "mxx", 1);
    private final DoubleProperty mxy = new TDouble(this, "mxy", 0);
    private final DoubleProperty tx = new TDouble(this, "tx", 0);
    private final DoubleProperty myx = new TDouble(this, "myx", 0);
    private final DoubleProperty myy = new TDouble(this, "myy", 1);
    private final DoubleProperty ty = new TDouble(this, "ty", 0);

    /// Creates the identity.
    public Affine() {
    }

    /// Creates a copy of another transform.
    public Affine(Transform transform) {
        this(transform.getMxx(), transform.getMxy(), transform.getTx(), transform.getMyx(), transform.getMyy(),
                transform.getTy());
    }

    /// Creates a transform from its six elements, row by row.
    public Affine(double mxx, double mxy, double tx, double myx, double myy, double ty) {
        this.mxx.set(mxx);
        this.mxy.set(mxy);
        this.tx.set(tx);
        this.myx.set(myx);
        this.myy.set(myy);
        this.ty.set(ty);
    }

    /// Sets the factor of x in the resulting x.
    public final void setMxx(double value) {
        mxx.set(value);
    }

    @Override
    public final double getMxx() {
        return mxx.get();
    }

    /// The factor of x in the resulting x.
    public final DoubleProperty mxxProperty() {
        return mxx;
    }

    /// Sets the factor of y in the resulting x.
    public final void setMxy(double value) {
        mxy.set(value);
    }

    @Override
    public final double getMxy() {
        return mxy.get();
    }

    /// The factor of y in the resulting x.
    public final DoubleProperty mxyProperty() {
        return mxy;
    }

    /// Sets the amount added to the resulting x.
    public final void setTx(double value) {
        tx.set(value);
    }

    @Override
    public final double getTx() {
        return tx.get();
    }

    /// The amount added to the resulting x.
    public final DoubleProperty txProperty() {
        return tx;
    }

    /// Sets the factor of x in the resulting y.
    public final void setMyx(double value) {
        myx.set(value);
    }

    @Override
    public final double getMyx() {
        return myx.get();
    }

    /// The factor of x in the resulting y.
    public final DoubleProperty myxProperty() {
        return myx;
    }

    /// Sets the factor of y in the resulting y.
    public final void setMyy(double value) {
        myy.set(value);
    }

    @Override
    public final double getMyy() {
        return myy.get();
    }

    /// The factor of y in the resulting y.
    public final DoubleProperty myyProperty() {
        return myy;
    }

    /// Sets the amount added to the resulting y.
    public final void setTy(double value) {
        ty.set(value);
    }

    @Override
    public final double getTy() {
        return ty.get();
    }

    /// The amount added to the resulting y.
    public final DoubleProperty tyProperty() {
        return ty;
    }

    private void assign(double[] m) {
        setToTransform(m[0], m[2], m[4], m[1], m[3], m[5]);
    }

    private static double[] row(double mxx, double mxy, double tx, double myx, double myy, double ty) {
        return new double[] {mxx, myx, mxy, myy, tx, ty};
    }

    private static double[] rotation(double angle, double pivotX, double pivotY) {
        double r = Math.toRadians(angle);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        return row(cos, -sin, pivotX - pivotX * cos + pivotY * sin, sin, cos, pivotY - pivotX * sin - pivotY * cos);
    }

    private static double[] scaling(double sx, double sy, double pivotX, double pivotY) {
        return row(sx, 0, (1 - sx) * pivotX, 0, sy, (1 - sy) * pivotY);
    }

    private static double[] shearing(double shx, double shy, double pivotX, double pivotY) {
        return row(1, shx, -shx * pivotY, shy, 1, -shy * pivotX);
    }

    private void appendMatrix(double[] m) {
        assign(Matrix2D.multiply(matrix(), m));
    }

    private void prependMatrix(double[] m) {
        assign(Matrix2D.multiply(m, matrix()));
    }

    /// Makes this a copy of another transform.
    public void setToTransform(Transform transform) {
        setToTransform(transform.getMxx(), transform.getMxy(), transform.getTx(), transform.getMyx(),
                transform.getMyy(), transform.getTy());
    }

    /// Sets the six elements, row by row.
    public void setToTransform(double mxx, double mxy, double tx, double myx, double myy, double ty) {
        this.mxx.set(mxx);
        this.mxy.set(mxy);
        this.tx.set(tx);
        this.myx.set(myx);
        this.myy.set(myy);
        this.ty.set(ty);
    }

    /// Makes this the identity.
    public void setToIdentity() {
        setToTransform(1, 0, 0, 0, 1, 0);
    }

    /// Replaces this transform with its inverse.
    public void invert() throws NonInvertibleTransformException {
        double[] out = new double[6];
        if (!Matrix2D.invert(matrix(), out)) {
            throw new NonInvertibleTransformException("Determinant is 0");
        }
        assign(out);
    }

    /// Applies another transform before this one.
    public void append(Transform transform) {
        appendMatrix(transform.matrix());
    }

    /// Applies a transform given by its elements, row by row, before this
    /// one.
    public void append(double mxx, double mxy, double tx, double myx, double myy, double ty) {
        appendMatrix(row(mxx, mxy, tx, myx, myy, ty));
    }

    /// Applies another transform after this one.
    public void prepend(Transform transform) {
        prependMatrix(transform.matrix());
    }

    /// Applies a transform given by its elements, row by row, after this
    /// one.
    public void prepend(double mxx, double mxy, double tx, double myx, double myy, double ty) {
        prependMatrix(row(mxx, mxy, tx, myx, myy, ty));
    }

    /// Applies a translation before this transform.
    public void appendTranslation(double tx, double ty) {
        appendMatrix(row(1, 0, tx, 0, 1, ty));
    }

    /// Applies a translation after this transform.
    public void prependTranslation(double tx, double ty) {
        prependMatrix(row(1, 0, tx, 0, 1, ty));
    }

    /// Applies a scale about the origin before this transform.
    public void appendScale(double sx, double sy) {
        appendMatrix(scaling(sx, sy, 0, 0));
    }

    /// Applies a scale about a pivot before this transform.
    public void appendScale(double sx, double sy, double pivotX, double pivotY) {
        appendMatrix(scaling(sx, sy, pivotX, pivotY));
    }

    /// Applies a scale about a pivot before this transform.
    public void appendScale(double sx, double sy, Point2D pivot) {
        appendMatrix(scaling(sx, sy, pivot.getX(), pivot.getY()));
    }

    /// Applies a scale about the origin after this transform.
    public void prependScale(double sx, double sy) {
        prependMatrix(scaling(sx, sy, 0, 0));
    }

    /// Applies a scale about a pivot after this transform.
    public void prependScale(double sx, double sy, double pivotX, double pivotY) {
        prependMatrix(scaling(sx, sy, pivotX, pivotY));
    }

    /// Applies a scale about a pivot after this transform.
    public void prependScale(double sx, double sy, Point2D pivot) {
        prependMatrix(scaling(sx, sy, pivot.getX(), pivot.getY()));
    }

    /// Applies a rotation about the origin, in degrees, before this
    /// transform.
    public void appendRotation(double angle) {
        appendMatrix(rotation(angle, 0, 0));
    }

    /// Applies a rotation about a pivot before this transform.
    public void appendRotation(double angle, double pivotX, double pivotY) {
        appendMatrix(rotation(angle, pivotX, pivotY));
    }

    /// Applies a rotation about a pivot before this transform.
    public void appendRotation(double angle, Point2D pivot) {
        appendMatrix(rotation(angle, pivot.getX(), pivot.getY()));
    }

    /// Applies a rotation about the origin, in degrees, after this
    /// transform.
    public void prependRotation(double angle) {
        prependMatrix(rotation(angle, 0, 0));
    }

    /// Applies a rotation about a pivot after this transform.
    public void prependRotation(double angle, double pivotX, double pivotY) {
        prependMatrix(rotation(angle, pivotX, pivotY));
    }

    /// Applies a rotation about a pivot after this transform.
    public void prependRotation(double angle, Point2D pivot) {
        prependMatrix(rotation(angle, pivot.getX(), pivot.getY()));
    }

    /// Applies a shear about the origin before this transform.
    public void appendShear(double shx, double shy) {
        appendMatrix(shearing(shx, shy, 0, 0));
    }

    /// Applies a shear about a pivot before this transform.
    public void appendShear(double shx, double shy, double pivotX, double pivotY) {
        appendMatrix(shearing(shx, shy, pivotX, pivotY));
    }

    /// Applies a shear about a pivot before this transform.
    public void appendShear(double shx, double shy, Point2D pivot) {
        appendMatrix(shearing(shx, shy, pivot.getX(), pivot.getY()));
    }

    /// Applies a shear about the origin after this transform.
    public void prependShear(double shx, double shy) {
        prependMatrix(shearing(shx, shy, 0, 0));
    }

    /// Applies a shear about a pivot after this transform.
    public void prependShear(double shx, double shy, double pivotX, double pivotY) {
        prependMatrix(shearing(shx, shy, pivotX, pivotY));
    }

    /// Applies a shear about a pivot after this transform.
    public void prependShear(double shx, double shy, Point2D pivot) {
        prependMatrix(shearing(shx, shy, pivot.getX(), pivot.getY()));
    }
}
