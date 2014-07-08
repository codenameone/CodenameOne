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
package com.codename1.impl.android;

import android.opengl.Matrix;



/**
 * Encapsulates a 4x4 transformation matrix that can be used to apply 3D transformations
 * to a {@link com.codename1.ui.Graphics} context. This can also be used for 2D transformations,
 * by only using the upper left 3x3 grid of the matrix.
 * 
 * <h4>Internal Representation</h4>
 * 
 * <p>Although matrix data can be set in several different formats (See {@link #setData}), the internal representation
 * is always that of a 4x4 matrix stored in a 16-element {@literal float} array in <a target="_blank" href="http://en.wikipedia.org/wiki/Row-major_order">row-major order</a>.
 * If you are working with 2D transformations only, then the upper left sub-matrix will contain
 * your 3x3 affine transformation, and the 4th row and 4th columns will be zeroes, except in the lower-right most
 * column, which will be a {@literal 1}.</p>
 * 
 * @author shannah
 * @see com.codename1.ui.Graphics#setTransform
 * @see com.codename1.ui.Graphics#getTransform
 */
public final class CN1Matrix4f {
    
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_IDENTITY = 0;
    public static final int TYPE_TRANSLATION = 1;
    public static final int TYPE_ROTATION = 2;
    public static final int TYPE_SCALE = 3;
    
    
    public static final int M00=0;
    public static final int M01=4;
    public static final int M02=8;
    public static final int M03=12;
    public static final int M10=1;
    public static final int M11=5;
    public static final int M12=9;
    public static final int M13=13;
    public static final int M20=2;
    public static final int M21=6;
    public static final int M22=10;
    public static final int M23=14;
    public static final int M30=3;
    public static final int M31=7;
    public static final int M32=11;
    public static final int M33=15;

    public final float[] data;
    private int type = TYPE_UNKNOWN;
    private Factory factory;

    public static class Factory {

        private float[] sTemp = new float[32];
        private static Factory defaultFactory = null;

        public static Factory getDefault() {
            if (defaultFactory == null) {
                defaultFactory = new Factory();

            }
            return defaultFactory;
        }

        public CN1Matrix4f makeMatrix(float[] data) {
            CN1Matrix4f m = new CN1Matrix4f(data);
            m.factory = this;
            return m;
        }

        public CN1Matrix4f makeIdentity() {
            CN1Matrix4f out = makeMatrix(null);
            out.factory = this;
            out.type = TYPE_IDENTITY;
            return out;
        }

        public CN1Matrix4f makeRotation(float angle, float x, float y, float z) {
            float[] m = new float[16];
            Matrix.setRotateM(m, 0, (float) (angle * 180f / Math.PI), x, y, z);
            CN1Matrix4f out = makeMatrix(m);
            out.factory = this;
            out.type = TYPE_ROTATION;
            return out;
        }
        
        

        public CN1Matrix4f makeTranslation(float x, float y, float z) {
            CN1Matrix4f m = makeIdentity();
            Matrix.translateM(m.data, 0, x, y, z);
            m.factory = this;
            m.type = TYPE_TRANSLATION;
            return m;
        }

        public CN1Matrix4f makePerspective(float fovy, float aspect, float zNear, float zFar) {
            float[] m = new float[16];
            Matrix.perspectiveM(m, 0, (float) (fovy * 180f / Math.PI), aspect, zNear, zFar);
            CN1Matrix4f out =  new CN1Matrix4f(m);
            out.factory = this;
            return out;
        }

        public CN1Matrix4f makeOrtho(float left, float right, float bottom, float top,
                float near, float far) {
            float[] m = new float[16];
            Matrix.orthoM(m, 0, left, right, bottom, top, near, far);
            CN1Matrix4f out = new CN1Matrix4f(m);
            out.factory = this;
            return out;
        }

        public CN1Matrix4f makeCamera(float eyeX, float eyeY, float eyeZ,
                float centerX, float centerY, float centerZ, float upX, float upY,
                float upZ) {

            float[] m = new float[16];
            Matrix.setLookAtM(m, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
            CN1Matrix4f out =  new CN1Matrix4f(m);
            out.factory = this;
            return out;
        }
    }

    
    
    public static CN1Matrix4f make(float[] data) {
        return Factory.getDefault().makeMatrix(data);
    }

    public static CN1Matrix4f makeIdentity() {
        return Factory.getDefault().makeMatrix(null);
    }
    
    public static CN1Matrix4f makeTranslation(float x, float y, float z){
        return Factory.getDefault().makeTranslation(x, y, z);
    }

    public static CN1Matrix4f makeRotation(float angle, float x, float y, float z) {
        return Factory.getDefault().makeRotation(angle, x, y, z);
    }

    public static CN1Matrix4f makeOrtho(float left, float right, float bottom, float top,
            float near, float far) {

        return Factory.getDefault().makeOrtho(left, right, bottom, top, near, far);
    }

    public static CN1Matrix4f makePerspective(float fovy, float aspect, float zNear, float zFar) {
        return Factory.getDefault().makePerspective(fovy, aspect, zNear, zFar);
    }

    public static CN1Matrix4f makeCamera(float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ, float upX, float upY,
            float upZ) {
        return Factory.getDefault().makeCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    public void rotate(float a, float x, float y, float z) {

        Matrix.setRotateM(factory.sTemp, 0, (float) (a * 180f / Math.PI), x, y, z);
        Matrix.multiplyMM(factory.sTemp, 16, data, 0, factory.sTemp, 0);
        System.arraycopy(factory.sTemp, 16, data, 0, 16);
        if ( type == TYPE_IDENTITY ){
            type = TYPE_ROTATION;
        } else {
            type = TYPE_UNKNOWN;
        }
    }

    public void translate(float x, float y, float z) {
        Matrix.translateM(data, 0, x, y, z);
        if ( type == TYPE_IDENTITY || type == TYPE_TRANSLATION ){
            type = TYPE_TRANSLATION;
        } else {
            type = TYPE_UNKNOWN;
        }
    }

    public void scale(float x, float y, float z) {
        Matrix.scaleM(data, 0, x, y, z);
        if ( type == TYPE_IDENTITY || type == TYPE_SCALE ){
            type = TYPE_SCALE;
        } else {
            type = TYPE_UNKNOWN;
        }
    }

    public void setPerspective(float fovy, float aspect, float zNear, float zFar) {
        Matrix.perspectiveM(data, 0, (float) (fovy * 180f / Math.PI), aspect, zNear, zFar);
    }

    public void setOrtho(float left, float right, float bottom, float top,
            float near, float far) {
        Matrix.orthoM(data, 0, left, right, bottom, top, near, far);
    }

    public void setCamera(float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ, float upX, float upY,
            float upZ) {
        Matrix.setLookAtM(data, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    public void setIdentity() {
        reset();
    }

    
    
    public void transformCoord(float[] pIn, float[] pOut) {
        //Log.p("Transforming "+pIn[0]+","+pIn[1]);
        //Log.p("Transform is "+this);
        int len = pIn.length;
        factory.sTemp[2] = 0;
        factory.sTemp[3] = 1f;
        
        
        System.arraycopy(pIn, 0, factory.sTemp, 0, len);
        
        Matrix.multiplyMV(factory.sTemp, 4, data, 0, factory.sTemp, 0);
        float w = factory.sTemp[7];
        if ( w != 1 && w != 0 ){
            
            for ( int i=4; i<7; i++){
                factory.sTemp[i] = factory.sTemp[i]/w;
            }
        }
       
        //len = pOut.length;
        System.arraycopy(factory.sTemp, 4, pOut, 0, len);
       

    }
    
    

    public String toString() {
        //StringBuilder sb = new StringBuilder();
        return "[[" + data[0] + "," + data[4] + "," + data[8] + "," + data[12] + "]\n"
                + "[" + data[1] + "," + data[5] + "," + data[9] + "," + data[13] + "]\n"
                + "[" + data[2] + "," + data[6] + "," + data[10] + "," + data[14] + "]\n"
                + "[" + data[3] + "," + data[7] + "," + data[11] + "," + data[15] + "]";

    }

    public boolean isIdentity() {
        for (int i = 0; i < 16; i++) {
            if (i % 5 == 0 && data[i] != 1f) {
                return false;
            } else if (i % 5 != 0 && data[i] != 0f) {
                return false;
            }
        }
        return true;
    }

    public boolean invert() {
        boolean res = Matrix.invertM(factory.sTemp, 0, data, 0);
        if (!res) {
            return res;
        } else {
            System.arraycopy(factory.sTemp, 0, data, 0, 16);
            return res;
        }

    }

    /**
     * Constructor. Copies data from the provided data array. See
     * {@link #setData} documentation for information acceptable formats for the
     * {@literal m} array.
     *
     * @param m An array containing data for the matrix. This can be in several
     * different formats. See {@link #setData} for a list of acceptable formats.
     *
     * @see #setData
     */
    private CN1Matrix4f(float[] m) {
        //Log.p("Creating new matrix");
        if (m == null) {
            m = new float[]{1f};
        }
        if (m.length == 16) {
            data = m;
        } else {
            data = new float[16];
            setData(m);
        }
        //Log.p("Exiting CN1Matrix4f constructor");
    }

    public void concatenate(CN1Matrix4f m){
        //Matrix.setRotateM(factory.sTemp, 0, (float) (a * 180f / Math.PI), x, y, z);
        Matrix.multiplyMM(factory.sTemp, 16, data, 0, m.data, 0);
        System.arraycopy(factory.sTemp, 16, data, 0, 16);
        type = TYPE_UNKNOWN;
    }
    /**
     * Resets the transformation to the identify matrix
     */
    public void reset() {
        for (int i = 0; i < 16; i++) {
            data[i] = 0;
        }
        data[0] = data[5] = data[10] = data[15] = 1;

    }

    /**
     * Obtains a reference to the 4x4 matrix cell data in <a target="_blank"
     * href="http://en.wikipedia.org/wiki/Row-major_order">row-major order</a>.
     *
     * @return A 16-element{@literal float} array representing the 4x4 matrix
     * data in row-major order.
     */
    public float[] getData() {
        return data;
    }

    /**
     * Sets the matrix data. This will accept the data in several different
     * formats to facilitate the creation of common matrix use-cases.
     * <h5>Acceptable Formats</h5>
     *
     * <table>
     * <tr><th>Array
     * Length</th><th>Interpretation</th><th>Example</th><th>Resulting 4x4
     * CN1Matrix4f</th></tr>
     * <tr>
     * <td>1</td>
     * <td>Apply both {@literal x} and {@literal y} scaling with a single
     * value.</td>
     * <td>{@code setData(new float[]{2f});}</td>
     * <td><pre>{@literal
     *  [2,0,0,0],
     * [0,2,0,0],
     * [0,0,1,0],
     * [0,0,0,1]}</pre></td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>Applies {@literal x} and {@literal y} scaling. First element is
     * {@literal x} scale. Second element is {@literal y} scale.</td>
     * <td>{@code setData(new float[]{2f, 3f});}</td>
     * <td><pre>{@literal
     *  [2,0,0,0],
     * [0,3,0,0],
     * [0,0,1,0],
     * [0,0,0,1]}</pre></td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>Recognized as a 2x2 2D transformation matrix.</td>
     * <td>{@code setData(new float[]{1f, 2f, 3f, 4f});}</td>
     * <td><pre>{@literal
     *  [1,2,0,0],
     * [3,4,0,0],
     * [0,0,1,0],
     * [0,0,0,1]}</pre></td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td>An affine transformation.</td>
     * <td>{@code setData(new float[]{1f, 2f, 3f, 4f, 5f, 6f});}</td>
     * <td><pre>{@literal
     *  [1,2,3,0],
     * [4,5,6,0],
     * [0,0,1,0],
     * [0,0,0,1]}</pre></td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td>A 3x3 matrix.</td>
     * <td>{@code setData(new float[]{1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f});}</td>
     * <td><pre>{@literal
     *  [1,2,3,0],
     * [4,5,6,0],
     * [7,8,9,0],
     * [0,0,0,1]}</pre></td>
     * </tr>
     * <tr>
     * <td>12</td>
     * <td>The top 3 rows of the 4x4 matrix. This is all the information
     * necessary for a 3D transformation since the last row is always
     * [0,0,0,1].</td>
     * <td>{@code setData(new float[]{1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f});}</td>
     * <td><pre>{@literal
     *  [ 1, 2, 3, 4],
     * [ 5, 6, 7, 8],
     * [ 9, 10,11,12],
     * [ 0, 0, 0, 1]}</pre></td>
     * </tr>
     * <tr>
     * <td>16</td>
     * <td>A 4x4 transformation matrix.</td>
     * <td>{@code setData(new float[]{1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f});}</td>
     * <td><pre>{@literal
     *  [ 1, 2, 3, 4],
     * [ 5, 6, 7, 8],
     * [ 9,10,11,12],
     * [13,14,15,16]}</pre></td>
     * </tr>
     * </table>
     *
     * @param m The data to populate the matrix. This will always replace the
     * matrix data in full.
     */
    public void setData(float[] m) {
        if (m == null) {
            reset();
            return;
        }
        switch (m.length) {
            case 1:
                reset();
                data[0] = m[0];
                data[5] = m[0];

                break;

            case 2:
                reset();
                data[0] = m[0];
                data[5] = m[1];

                break;
            case 4:
                // This is just a 2D transformation
                reset();
                data[0] = m[0];
                data[1] = m[1];
                data[4] = m[2];
                data[5] = m[3];

                break;
            case 6:
                reset();
                data[0] = m[0];
                data[1] = m[1];
                data[2] = m[2];
                data[4] = m[3];
                data[5] = m[4];
                data[6] = m[5];

                break;
            case 9:
                reset();
                data[0] = m[0];
                data[1] = m[1];
                data[2] = m[2];
                data[4] = m[3];
                data[5] = m[4];
                data[6] = m[5];
                data[8] = m[6];
                data[9] = m[7];
                data[10] = m[8];

                break;
            case 12:
                reset();
                System.arraycopy(m, 0, data, 0, 12);

                break;
            case 16:
                System.arraycopy(m, 0, data, 0, 16);
                break;
            default:
                throw new IllegalArgumentException("Transforms must be array of length 1, 2, 4, 6, 9, 12, or 16");
        }
    }

    public CN1Matrix4f copy() {
        float[] data = new float[16];
        System.arraycopy(this.data, 0, data, 0, 16);
        return CN1Matrix4f.make(data);
    }

  
    
  
}
