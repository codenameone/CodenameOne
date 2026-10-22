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

import java.util.ArrayList;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.Matrix2D;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/// An affine transformation of the plane:
///
/// ```
/// x' = mxx * x + mxy * y + tx
/// y' = myx * x + myy * y + ty
/// ```
///
/// A node applies the transforms in its `getTransforms()` list in order,
/// the first one outermost, between its layout position and its own
/// scale and rotation.
///
/// Only two dimensions exist in this layer: the z elements of JavaFX's
/// matrix (`getMxz()`, `getTz()` and the like), the three dimensional
/// overloads, `Point3D`, `toArray`, `column`, `row`, `similarTo`,
/// `clone()` and the `TransformChangedEvent` handlers are absent.
public abstract class Transform {

    private ArrayList<Dirty.Owner> owners;

    /// Creates a transform.
    public Transform() {
    }

    /// Returns an affine transform with the six elements given column by
    /// column.
    public static Affine affine(double mxx, double myx, double mxy, double myy, double tx, double ty) {
        return new Affine(mxx, mxy, tx, myx, myy, ty);
    }

    /// Returns a translation.
    public static Translate translate(double x, double y) {
        return new Translate(x, y);
    }

    /// Returns a clockwise rotation by an angle in degrees about a pivot.
    public static Rotate rotate(double angle, double pivotX, double pivotY) {
        return new Rotate(angle, pivotX, pivotY);
    }

    /// Returns a scale about the origin.
    public static Scale scale(double x, double y) {
        return new Scale(x, y);
    }

    /// Returns a scale about a pivot.
    public static Scale scale(double x, double y, double pivotX, double pivotY) {
        return new Scale(x, y, pivotX, pivotY);
    }

    /// Returns a shear about the origin.
    public static Shear shear(double x, double y) {
        return new Shear(x, y);
    }

    /// Returns a shear about a pivot.
    public static Shear shear(double x, double y, double pivotX, double pivotY) {
        return new Shear(x, y, pivotX, pivotY);
    }

    /// Returns the factor of x in the resulting x.
    public double getMxx() {
        return 1;
    }

    /// Returns the factor of y in the resulting x.
    public double getMxy() {
        return 0;
    }

    /// Returns the amount added to the resulting x.
    public double getTx() {
        return 0;
    }

    /// Returns the factor of x in the resulting y.
    public double getMyx() {
        return 0;
    }

    /// Returns the factor of y in the resulting y.
    public double getMyy() {
        return 1;
    }

    /// Returns the amount added to the resulting y.
    public double getTy() {
        return 0;
    }

    final double[] matrix() {
        return new double[] {getMxx(), getMyx(), getMxy(), getMyy(), getTx(), getTy()};
    }

    /// Returns whether this transform changes nothing.
    public final boolean isIdentity() {
        return Matrix2D.isIdentity(matrix());
    }

    /// Returns the determinant of the matrix; zero when the transform
    /// cannot be inverted.
    public double determinant() {
        return getMxx() * getMyy() - getMxy() * getMyx();
    }

    /// Transforms a point.
    public Point2D transform(double x, double y) {
        return new Point2D(getMxx() * x + getMxy() * y + getTx(), getMyx() * x + getMyy() * y + getTy());
    }

    /// Transforms a point.
    public Point2D transform(Point2D point) {
        return transform(point.getX(), point.getY());
    }

    private static Bounds box(double[] m, Bounds bounds) {
        double[] b = Matrix2D.bounds(m, bounds.getMinX(), bounds.getMinY(), bounds.getWidth(),
                bounds.getHeight());
        return new BoundingBox(b[0], b[1], b[2] - b[0], b[3] - b[1]);
    }

    /// Returns the box around transformed bounds.
    public Bounds transform(Bounds bounds) {
        return box(matrix(), bounds);
    }

    /// Transforms a distance: the point without the translation.
    public Point2D deltaTransform(double x, double y) {
        return new Point2D(getMxx() * x + getMxy() * y, getMyx() * x + getMyy() * y);
    }

    /// Transforms a distance: the point without the translation.
    public Point2D deltaTransform(Point2D point) {
        return deltaTransform(point.getX(), point.getY());
    }

    private double[] inverse() throws NonInvertibleTransformException {
        double[] out = new double[6];
        if (!Matrix2D.invert(matrix(), out)) {
            throw new NonInvertibleTransformException("Determinant is 0");
        }
        return out;
    }

    /// Maps a transformed point back.
    public Point2D inverseTransform(double x, double y) throws NonInvertibleTransformException {
        double[] inv = inverse();
        return new Point2D(Matrix2D.x(inv, x, y), Matrix2D.y(inv, x, y));
    }

    /// Maps a transformed point back.
    public Point2D inverseTransform(Point2D point) throws NonInvertibleTransformException {
        return inverseTransform(point.getX(), point.getY());
    }

    /// Returns the box around bounds mapped back.
    public Bounds inverseTransform(Bounds bounds) throws NonInvertibleTransformException {
        return box(inverse(), bounds);
    }

    /// Maps a transformed distance back.
    public Point2D inverseDeltaTransform(double x, double y) throws NonInvertibleTransformException {
        double[] inv = inverse();
        return new Point2D(inv[0] * x + inv[2] * y, inv[1] * x + inv[3] * y);
    }

    /// Maps a transformed distance back.
    public Point2D inverseDeltaTransform(Point2D point) throws NonInvertibleTransformException {
        return inverseDeltaTransform(point.getX(), point.getY());
    }

    /// Returns this transform followed, in a node's list, by another: a
    /// point goes through `transform` first and through this one second,
    /// exactly as when both are added to `getTransforms()` in that order.
    public Transform createConcatenation(Transform transform) {
        double[] m = Matrix2D.multiply(matrix(), transform.matrix());
        return new Affine(m[0], m[2], m[4], m[1], m[3], m[5]);
    }

    /// Returns the transform that undoes this one.
    public Transform createInverse() throws NonInvertibleTransformException {
        double[] m = inverse();
        return new Affine(m[0], m[2], m[4], m[1], m[3], m[5]);
    }

    /// Tells the nodes using this transform that it changed. Called by
    /// the properties of the subclasses; a subclass of the application's
    /// own calls it when what its getters answer changed.
    protected void transformChanged() {
        if (owners != null) {
            for (int i = owners.size() - 1; i >= 0; i--) {
                if (i < owners.size()) {
                    owners.get(i).cn1Invalidated(Dirty.BOUNDS);
                }
            }
        }
    }

    /// Registers a node whose `getTransforms()` holds this transform; it
    /// is told with `Dirty.BOUNDS` when the transform changes. Registering
    /// twice counts once.
    public final void cn1Attach(Dirty.Owner node) {
        if (owners == null) {
            owners = new ArrayList<Dirty.Owner>(2);
        }
        if (!owners.contains(node)) {
            owners.add(node);
        }
    }

    /// Removes a node registered with [#cn1Attach(Dirty.Owner)].
    public final void cn1Detach(Dirty.Owner node) {
        if (owners != null) {
            owners.remove(node);
        }
    }

    /// Returns the class name and the six elements.
    @Override
    public String toString() {
        String name = getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1) + " [" + getMxx() + ", " + getMxy() + ", " + getTx() + ", "
                + getMyx() + ", " + getMyy() + ", " + getTy() + "]";
    }
}
