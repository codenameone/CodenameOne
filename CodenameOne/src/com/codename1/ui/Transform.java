/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.ui;

import com.codename1.impl.CodenameOneImplementation;

/**
 * Encapsulates a 3D transform that can be used in {@link com.codename1.ui.Graphics} contexts
 * or with {@link com.codename1.ui.geom.Shape}s to transform in various ways.
 * 
 * Use the {@link #isSupported} and {@link #isPerspectiveSupported} to check if transforms and 
 * perspective transforms are supported on this platform.  If they are not supported, this 
 * class will throw RuntimeExceptions if you try to use it.
 * @author shannah
 */
public class Transform {
    
    public static class NotInvertibleException extends Exception {
        
    }
    
    /**
     * Reference to the native transform.  This should only be used by the implementation.
     */
    private Object nativeTransform;
    
    /**
     * The type of transform.  This allows us to cut corners in transformation 
     * when using a special matrix like a translation, scale, or identity matrix.
     */
    private int type = TYPE_UNKNOWN;
    private Transform inverse;
    private boolean inverseDirty=true;
    
    private float translateX=0, translateY=0, translateZ=0;
    private float scaleX=1f, scaleY=1f, scaleZ=1f;
    private boolean dirty = true;
    private CodenameOneImplementation impl = null;
    
    private static class ImmutableTransform extends Transform {

        public ImmutableTransform(Object nativeTransform) {
            super(nativeTransform);
        }

        private void unsupported(){
            throw new RuntimeException("Cannot change immutable transform");
        }
        
        @Override
        public void setCamera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
            unsupported();
        }

        @Override
        public void setIdentity() {
            unsupported();
        }

        @Override
        public void setOrtho(float left, float right, float bottom, float top, float near, float far) {
            unsupported();
        }

        @Override
        public void setPerspective(float fovy, float aspect, float zNear, float zFar) {
            unsupported();
        }

        @Override
        public void setRotation(float angle, float px, float py) {
            unsupported();
        }

        @Override
        public void setTransform(Transform t) {
            unsupported();
        }

        @Override
        public void setRotation(float angle, float x, float y, float z) {
            unsupported();
        }

        @Override
        public void setTranslation(float x, float y) {
            unsupported();
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            unsupported();
        }

        @Override
        public void rotate(float angle, float px, float py) {
            unsupported();
        }

        @Override
        public void rotate(float angle, float x, float y, float z) {
            unsupported();
        }
        
        

        @Override
        public void translate(float x, float y) {
            unsupported();
        }

        @Override
        public void translate(float x, float y, float z) {
            unsupported();
        }
        
        

        @Override
        public void scale(float x, float y) {
            unsupported();
        }

        @Override
        public void scale(float x, float y, float z) {
            unsupported();
        }
        
    }
    
    
    /**
     * Constant for transform type. Transform is not a special matrix.
     */
    public static final int TYPE_UNKNOWN=-1;
    
    /**
     * Constant for transform type.  Transform is the identity transform.
     */
    public static final int TYPE_IDENTITY=0;
    
    /**
     * Constant for transform type.  Transform is a translation transform
     * only.  
     */
    public static final int TYPE_TRANSLATION=1;
    
    /**
     * Constant for transform type.  Transform is a scale transform only.
     */
    public static final int TYPE_SCALE=2;
    
    private static Transform _IDENTITY;
    public static Transform IDENTITY(){
        if ( _IDENTITY == null ){
            _IDENTITY = new ImmutableTransform(Display.impl.makeTransformIdentity());
            _IDENTITY.type = TYPE_IDENTITY;
        }
        return _IDENTITY;
    }
    
    /**
     * Private constructor
     * @param nativeTransform 
     */
    private Transform(Object nativeTransform){
        this.nativeTransform = nativeTransform;
        impl();
        
    }
    
    
    private CodenameOneImplementation impl(){
        if ( impl == null ){
            impl = Display.impl;
        }
        return impl;
    }
    
    /**
     * Initializes the native transform with appropriate values.  For efficiency,
     * some special kinds of transforms don't keep the native transform in sync
     * (or even created at all).  Before accessing, the native transform from the
     * implementation, the native transform needs to be initialized.  This method
     * is called internally in the appropriate places to ensure that the native
     * transform is kept in sync when it is needed.
     */
    private void initNativeTransform(){
        if ( nativeTransform == null ){
            nativeTransform = impl.makeTransformIdentity();
            if ( type == TYPE_TRANSLATION ){
                impl.setTransformTranslation(nativeTransform, translateX, translateY, translateZ);
            } else if ( type == TYPE_SCALE ){
                impl.setTransformScale(nativeTransform, scaleX, scaleY, scaleZ);
            }  
        } else  {
            switch (type){
                case TYPE_TRANSLATION:
                    impl.setTransformTranslation(nativeTransform, translateX, translateY, translateZ);
                    break;
                case TYPE_SCALE:
                    impl.setTransformScale(nativeTransform, scaleX, scaleY, scaleZ);
                    break;
                case TYPE_IDENTITY:
                    impl.setTransformIdentity(nativeTransform);
                    break;     
            }

        }
        dirty = false;
    }
    
    /**
     * Makes a new identity transform.
     * @return An identity transform.
     */
    public static Transform makeIdentity(){
        Transform out = new Transform(null);
        out.type = TYPE_IDENTITY;
        return out;
    }
    
    /**
     * Checks if this transform is the identity transform.
     * @return True if the transform is the identity.
     */
    public boolean isIdentity(){
        if (type == TYPE_IDENTITY) return true;
        if ( this.equals(IDENTITY())){
            setIdentity();
            return true;
        }
        return false;
    }
    
    
    /**
     * Checks if this transform is a translation transform.  
     * @return True if this transform performs translation only.  Note that this
     * will return false if the transform is the identity (i.e. is actually 
     * a translation of (0,0,0).
     */
    public boolean isTranslation(){
        return (type == TYPE_TRANSLATION);
    }
    
    /**
     * Gets the x scale factor of this transformation.  This value is only reliable
     * if the transform is a scale transform.
     * @return The x scale factor of this transformation.
     * @see #isScale() 
     * @see #setScale()
     */
    public float getScaleX(){
        return scaleX;
    }
    
    /**
     * Gets the y scale factor of this transformation.  This value is only reliable
     * if the transform is a scale transform.
     * @return The y scale factor of this transformation.
     * @see #isScale() 
     * @see #setScale()
     * @return 
     */
    public float getScaleY(){
        return scaleY;
    }
    
    /**
     * Gets the z scale factor of this transformation.  This value is only reliable
     * if the transform is a scale transform.
     * @return The z scale factor of this transformation.
     * @see #isScale() 
     * @see #setScale()
     * @return 
     */
    public float getScaleZ(){
        return scaleZ;
    }
    
    /**
     * Gets the x translation of this transformation.  This value is only reliable
     * if the transform is a translation transform.
     * @return The x translation of this transform.
     * @see #isTranslation()
     * @see #setTranslation()
     * @see #translate()
     */
    public float getTranslateX(){
        return translateX;
    }
    
    /**
     * Gets the y translation of this transformation.  This value is only reliable
     * if the transform is a translation transform.
     * @return The y translation of this transform.
     * @see #isTranslation()
     * @see #setTranslation()
     * @see #translate()
     */
    public float getTranslateY(){
        return translateY;
    }
    
    /**
     * Gets the z translation of this transformation.  This value is only reliable
     * if the transform is a translation transform.
     * @return The z translation of this transform.
     * @see #isTranslation()
     * @see #setTranslation()
     * @see #translate()
     */
    public float getTranslateZ(){
        return translateZ;
    }
    
    /**
     * Checks if this transform is a scale transformation .
     * @return Returns true if and only if this is a non-identity scale transformation.  
     */
    public boolean isScale(){
        return (type == TYPE_SCALE);
    }
    
    /**
     * Makes a new rotation transformation.
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param angle The angle of the rotation in radians.
     * @param x The x component of the vector around which the rotation occurs.
     * @param y The y component of the vector around which the rotation occurs.
     * @param z The z component of the vector around which the rotation occurs.
     * @return A transform that makes the appropriate rotation.
     * @throws RuntimeException If {@link #isSupported()} is false.
     */
    public static Transform makeRotation(float angle, float x, float y, float z){
        Object t = Display.impl.makeTransformRotation(angle, x, y, z);
        Transform out = new Transform(t);
        return out;
    }
    
    public static Transform makeRotation(float angle, float x, float y){
        Transform t = makeTranslation(x, y, 0);
        t.rotate(angle, 0, 0, 1);
        t.translate(-x, -y, 0);
        return t;
        
    }
    
    /**
     * Makes a new translation transformation.
     * @param x The x component of the translation.
     * @param y The y component of the translation.
     * @param z The z component of the translation.
     * @return A transform that makes the specified translation.
     */
    public static Transform makeTranslation(float x, float y, float z){
        Transform out = new Transform(null);
        out.translateX = x;
        out.translateY = y;
        out.translateZ = z;
        out.type = TYPE_TRANSLATION;
        return out;
    }
    
    public static Transform makeTranslation(float x, float y){
        return makeTranslation(x, y, 0);
    }
    
    /**
     * Makes a new scale transformation.
     * @param x The x scale factor.
     * @param y The y scale factor.
     * @param z The z scale factor.
     * @return A transform that scales values according to the provided scale factors.
     */
    public static Transform makeScale(float x, float y, float z){
        if (x==1 && y == 1 && z == 1) {
            return makeIdentity();
        }
        Transform out = new Transform(null);
        out.scaleX = x;
        out.scaleY = y;
        out.scaleZ = z;
        out.type = TYPE_SCALE;
        return out;
    }
    
    /**
     * Creates a new scale transform.
     * @param x Factor to scale in x axis.
     * @param y Factor to scale by in y axis.
     * @return A new transform with the specified scale.
     */
    public static Transform makeScale(float x, float y){
        return makeScale(x, y, 1);
    }
    
    /**
     * Resets the transformation to a scale transformation.
     * @param x x-axis scaling
     * @param y y-axis scaling
     * @param z z-axis scaling
     */
    public void setScale(float x, float y, float z) {
        if (x==1 && y == 1 && z == 1) {
            setIdentity();
            return;
        }
        Transform out = this;
        out.scaleX = x;
        out.scaleY = y;
        out.scaleZ = z;
        out.type = TYPE_SCALE;
    }
    
    /**
     * Resets the transformation to scale transform.
     * @param x x-axis scaling.
     * @param y y-axis scaling.
     */
    public void setScale(float x, float y) {
        setScale(x, y, 1);
    }
    
    /**
     * Makes a new perspective transform.
     * <p>Note: If {@link #isPerspectiveSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param fovy The y field of view angle.
     * @param aspect The aspect ratio.
     * @param zNear The nearest visible z coordinate.
     * @param zFar The farthest z coordinate.
     * @return A transform for the given perspective.
     */
    public static Transform makePerspective(float fovy, float aspect, float zNear, float zFar) {
        Object t = Display.impl.makeTransformPerspective(fovy, aspect, zNear, zFar);
        Transform out = new Transform(t);
        return out;
    }
    
    /**
     * Makes a new orthographic projection transform.
     * <p>Note: If {@link #isPerspectiveSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param left x-coordinate that is the left edge of the view.
     * @param right The x-coordinate that is the right edge of the view.
     * @param bottom The y-coordinate that is the bottom edge of the view.
     * @param top The y-coordinate that is the top edge of the view.
     * @param near The nearest visible z-coordinate.
     * @param far The farthest visible z-coordinate.
     * @return A transform with the provided orthographic projection.
     */
    public static Transform makeOrtho(float left, float right, float bottom, float top,
                float near, float far){
        Object t = Display.impl.makeTransformOrtho(left, right, bottom, top, near, far);
        Transform out = new Transform(t);
        return out;
    }
    
    /**
     * Makes a transform to simulate a camera's perspective at a given location.
     * <p>Note: If {@link #isPerspectiveSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param eyeX The x-coordinate of the camera's eye.
     * @param eyeY The y-coordinate of the camera's eye.
     * @param eyeZ The z-coordinate of the camera's eye.
     * @param centerX The center x coordinate of the view.
     * @param centerY The center y coordinate of the view.
     * @param centerZ The center z coordinate of the view.
     * @param upX The x-coordinate of the up vector for the camera.
     * @param upY The y-coordinate of the up vector for the camera.
     * @param upZ The z-coordinate of the up vector for the camera.
     * @return A transform with the provided camera's view perspective.
     */
    public static Transform makeCamera(float eyeX, float eyeY, float eyeZ,
                float centerX, float centerY, float centerZ, float upX, float upY,
                float upZ){
        Object t = Display.impl.makeTransformCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        Transform out = new Transform(t);
        return out;
    }
    
    /**
     * Rotates the current transform.
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param angle The angle to rotate in radians.
     * @param x The x-coordinate of the vector around which to rotate.
     * @param y The y-coordinate of the vector around which to rotate.
     * @param z  The z-coordinate of the vector around which to rotate.
     * @see #setRotation
     */
    public void rotate(float angle, float x, float y, float z){
        initNativeTransform();
        Display.impl.transformRotate(nativeTransform, angle, x, y, z);
        type = TYPE_UNKNOWN;
    }
    
    public void rotate(float angle, float px, float py){
        translate(px, py, 0);
        rotate(angle, 0, 0, 1);
        translate(-px, -py, 0);
    }
    
    /**
     * Sets the transform to be the provided rotation. This replaces the current transform
     * whereas {@link #rotate()} further rotates the current transform.
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param angle The angle to rotate in radians.
     * @param x The x-coordinate of the vector around which to rotate.
     * @param y The y-coordinate of the vector around which to rotate.
     * @param z  The z-coordinate of the vector around which to rotate.
     * @see #rotate()
     */
    public void setRotation(float angle, float x, float y, float z){
        initNativeTransform();
        setTransform(makeRotation(angle, x, y, z));
        type = TYPE_UNKNOWN;
    }
    
    public void setRotation(float angle, float px, float py){
        initNativeTransform();
        setTransform(makeRotation(angle, px, py));
        type = TYPE_UNKNOWN;
    }
    
    
    /**
     * Sets the transform to the identity transform.
     */
    public void setIdentity(){
        type = TYPE_IDENTITY;
        scaleX = 1f; scaleY = 1f; scaleZ = 1f;
        translateX = 0f; translateY = 0f; translateZ = 0f;
        dirty = true;
    }
    
    public String toString(){
        getNativeTransform();
        return ""+nativeTransform;
    }
    
    /**
     * Translates the transform by the specified amounts.  This adds additional 
     * translations to whereas {@link #setTranslation()} replaces the transform
     * with the specified translation.
     * <p>Note: If {@link #isSupported()} is false, then this may throw a Runtime Exception.</p>
     * @param x The x translation.
     * @param y The y translation.
     * @param z The z translation.
     * @see #setTranslation()
     */
    public void translate(float x, float y, float z){
        if ( type == TYPE_IDENTITY ){
            type = TYPE_TRANSLATION;
        }
        if ( type == TYPE_TRANSLATION){
            translateX += x;
            translateY += y;
            translateZ += z;
            if ( translateX == 0 && translateY == 0 && translateZ == 0 ){
                type = TYPE_IDENTITY;
            }
            dirty = true;
        } else {
            initNativeTransform();
            type = TYPE_UNKNOWN;
            impl.transformTranslate(nativeTransform, x, y, z);
        }
    }
    
    public void translate(float x, float y){
        translate(x, y, 0);
    }
    
    /**
     * Sets the current transform to be the specified translation.  This replaces the current
     * transform with the given translation whereas {@link #translate()} adds additional translation
     * to the existing translation.
     * 
     * @param x The x translation.
     * @param y The y translation.
     * @param z The z translation.
     * @see #translate()
     */
    public void setTranslation(float x, float y, float z){
        type = TYPE_TRANSLATION;
        
        if ( type == TYPE_TRANSLATION){
            translateX = x;
            translateY = y;
            translateZ = z;
            if ( translateX == 0 && translateY == 0 && translateZ == 0 ){
                type = TYPE_IDENTITY;
            }
            dirty = true;
        }
        
    }
    
    public void setTranslation(float x, float y){
        setTranslation(x, y, 0);
    }
    
    /**
     * Scales the current transform by the provide scale factors.  Not to be confused with
     * {@link #setScale()} which replaces the transform.
     * <p>Note: If {@link #isSupported()} is false, then this may throw a Runtime Exception.</p>
     * @param x The x-scale factor
     * @param y The y-scale factor
     * @param z The z-scale factor
     * @see #setScale()
     */
    public void scale(float x, float y, float z){
        if ( type == TYPE_IDENTITY ){
            type = TYPE_SCALE;
        }
        if ( type == TYPE_SCALE ){
            scaleX *= x;
            scaleY *= y;
            scaleZ *= z;
            
            if ( scaleZ == 1f && scaleY == 1f && scaleZ == 1f ){
                type = TYPE_IDENTITY;
                
            }
            dirty = true;
        } else {
            initNativeTransform();
            type = TYPE_UNKNOWN;
            impl.transformScale(nativeTransform, x, y, z);
        }
        
    }
    
    public void scale(float x, float y){
        scale(x, y, 1);
    }
    
    /**
     * Gets the inverse transformation for this transform.
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @return The inverse transform.
     * @deprecated Use {@link #getInverse(com.codename1.ui.Transform) } instead.
     */
    public Transform getInverse(){
        return makeInverse();
    }
    
    
    private Transform makeInverse() {
        if ( type == TYPE_IDENTITY ){
            return makeIdentity();
        } else if ( type == TYPE_TRANSLATION ){
            return makeTranslation(-translateX, -translateY, -translateZ);
        } else if ( type == TYPE_SCALE ){
            return makeScale(1f/scaleX, 1f/scaleY, 1f/scaleZ);
        } else {
            initNativeTransform();
            Object t = impl.makeTransformInverse(nativeTransform);
            Transform out = new Transform(t);
            return out;
        }
    }
    
    public void getInverse(Transform inverseOut) throws NotInvertibleException {
        if (inverse == null) {
            inverse = makeInverse();
            inverseDirty = false;
        } else if (inverseDirty) {
            inverse.setTransform(this);
            inverse.invert();
            inverseDirty = false;
        }
        inverseOut.setTransform(inverse);
    }
    
    public void invert() throws NotInvertibleException {
        if ( type == TYPE_IDENTITY ){
            // Do nothing
        } else if ( type == TYPE_TRANSLATION ){
            setTranslation(-translateX, -translateY, -translateZ);
        } else if ( type == TYPE_SCALE ){
            setScale(1f/scaleX, 1f/scaleY, 1f/scaleZ);
        } else {
            initNativeTransform();
            impl.setTransformInverse(nativeTransform);
        }
    }
    
    /**
     * Sets the current transform to be identical to the provided transform.
     * <p>Note: If {@link #isSupported()} is false, then this will may throw a Runtime Exception.</p>
     * @param t A transform to copy into the current transform.
     */
    public void setTransform(Transform t){
        type = t.type;
        scaleX = t.scaleX;
        scaleY = t.scaleY;
        scaleZ = t.scaleZ;
        translateX = t.translateX;
        translateY = t.translateY;
        translateZ = t.translateZ;
        
        switch (type){
            case TYPE_IDENTITY:
            case TYPE_TRANSLATION:
            case TYPE_SCALE:
                // do nothing here
                dirty = true;
                break;
            default:
                initNativeTransform();
                t.initNativeTransform();
                impl.copyTransform(t.nativeTransform, nativeTransform);
                break;
        }
        
    }
    
    /**
     * Sets the current transform to be the concatenation of the current transform and
     * the provided transform.
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param t The transform to concatenate to this one.
     */
    public void concatenate(Transform t){
        impl.concatenateTransform(getNativeTransform(), t.getNativeTransform());
        type = TYPE_UNKNOWN;
    }
    
    /**
     * Sets the transform to be the specified perspective transformation.
     * <p>Note: If {@link #isPerspectiveSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param fovy Y-field of view angle.
     * @param aspect Apspect ratio of the view window.
     * @param zNear Nearest visible z-coordinate.
     * @param zFar Farthest visible z-coordinate.
     * @see #makePerspective()
     */
    public void setPerspective(float fovy, float aspect, float zNear, float zFar){
        type = TYPE_UNKNOWN;
        impl.setTransformPerspective(getNativeTransform(), fovy, aspect, zNear, zFar);
    }
    
    /**
     * Sets the transform to be the specified orthogonal view.
     * <p>Note: If {@link #isPerspectiveSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param left Left x-coord of view.
     * @param right Right x-coord of view.
     * @param bottom Bottom y-coord of view.
     * @param top Top y-coord of view.
     * @param near Nearest visible z-coordinate
     * @param far Farthest visible z-coordinate
     */
    public void setOrtho(float left, float right, float bottom, float top,
                float near, float far){
        type = TYPE_UNKNOWN;
        impl.setTransformOrtho(getNativeTransform(), left, right, bottom, top, near, far);
    }
    
    
    /**
     * Sets the transform to the specified camera's perspective.
     * <p>Note: If {@link #isPerspectiveSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param eyeX The x-coordinate of the camera's eye.
     * @param eyeY The y-coordinate of the camera's eye.
     * @param eyeZ The z-coordinate of the camera's eye.
     * @param centerX The center x coordinate of the view.
     * @param centerY The center y coordinate of the view.
     * @param centerZ The center z coordinate of the view.
     * @param upX The x-coordinate of the up vector for the camera.
     * @param upY The y-coordinate of the up vector for the camera.
     * @param upZ The z-coordinate of the up vector for the camera.
     */
    public void setCamera(float eyeX, float eyeY, float eyeZ,
                float centerX, float centerY, float centerZ, float upX, float upY,
                float upZ){
        setTransform(makeCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ));
    }
    
    /**
     * Transforms a set of points using the current transform.
     * @param pointSize The size of the points to transform (2 or 3)
     * @param in Input array of points.
     * @param srcPos Start position in input array
     * @param out Output array of points
     * @param destPos Start position in output array
     * @param numPoints Number of points to transform.
     */
    public void transformPoints(int pointSize, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        switch (type) {
            case TYPE_TRANSLATION:
                impl.translatePoints(pointSize, translateX, translateY, translateZ, in, srcPos, out, destPos, numPoints);
                break;
            case TYPE_SCALE:
                impl.scalePoints(pointSize, scaleX, scaleY, scaleZ, in, srcPos, out, destPos, numPoints);
                break;
            case TYPE_IDENTITY:
                System.arraycopy(in, srcPos, out, destPos, numPoints * pointSize);
                break;
            default :
                impl.transformPoints(getNativeTransform(), pointSize, in, srcPos, out, destPos, numPoints);
                break;
        }
    }
    
    /**
     * Transforms a provided point. 
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param point 2 or 3 element array representing either an (x,y) or (x,y,z) tuple.
     * @return A 3-element array representing transformed (x,y,z) tuple.
     */
    public float[] transformPoint(float[] point){
        float[] out = new float[3];
        transformPoint(point, out);
        return out;
    }
    
    /**
     * Transforms a provided point and places the result in the provided array.
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @param in A 2 or 3 element array representing either an (x,y) or (x,y,z) tuple.
     * @param out A 2 or 3 element array in which the transformed point will be stored.  Should match the length of the in array.
     */
    public void transformPoint(float[] in, float[] out){
        int len = in.length;
        int olen = out.length;
        switch (type){
            case TYPE_TRANSLATION:
                
                out[0] = in[0]+translateX;
                out[1] = in[1]+translateY;
                if ( len > 2 ){
                    out[2] = in[1]+translateZ;
                } else if (olen > 2){
                    out[2] = 0;
                }
                break;
            case TYPE_SCALE:
                out[0] = in[0]*scaleX;
                out[1] = in[1]*scaleY;
                if ( len > 2 ){
                    out[2] = in[2]*scaleZ;
                } else if ( olen > 2){
                    out[2] = 0;
                }
                break;
            case TYPE_IDENTITY:
                System.arraycopy(in,0,out,0,len);
                if ( len <= 2 && olen > 2 ){
                    out[2] = 0;
                }
                break;
            default:
                impl.transformPoint(getNativeTransform(), in, out);
                
        }
        
    }
    
    
    /**
     * Gets the native transform object.  This object is implementation dependent so this
     * method should really only be used by the implementation.
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @return The native transform object.
     */
    public Object getNativeTransform(){
        if ( dirty ){
            initNativeTransform();
        }
        return nativeTransform;
    }
    
    
    /**
     * Creates a copy of the current transform.
     * <p>Note: If {@link #isSupported()} is false, then this will throw a Runtime Exception.</p>
     * @return A copy of the current transform.
     */
    public Transform copy(){
        Transform out = new Transform(null);
        out.setTransform(this);
        return out;
        
    }
    
    /**
     * Checks if transforms are supported on this platform.  If this returns false,
     * you cannot use this class.
     * @return True if and only if this platform supports transforms.
     */
    public static boolean isSupported(){
        return Display.impl.isTransformSupported();
    }
    
    /**
     * Checks if perspective transforms are supported on this platform.  If this returns false,
     * you cannot use this class.
     * @return True if and only if this platform supports transforms.
     */
    public static boolean isPerspectiveSupported(){
        return Display.impl.isPerspectiveTransformSupported();
    }
    
    
    public boolean equals(Transform t2){
        if ( type == TYPE_IDENTITY && t2.type == TYPE_IDENTITY ){
            return true;
        } 
        boolean out = impl.transformEqualsImpl(this, t2);
        return out;
    }
    
}
