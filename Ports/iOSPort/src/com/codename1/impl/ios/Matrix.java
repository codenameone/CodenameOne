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
package com.codename1.impl.ios;



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
public final class Matrix {
    
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

        public Matrix makeMatrix(float[] data) {
            Matrix m = new Matrix(data);
            m.factory = this;
            return m;
        }

        public Matrix makeIdentity() {
            Matrix out = makeMatrix(null);
            out.factory = this;
            out.type = TYPE_IDENTITY;
            return out;
        }

        public Matrix makeRotation(float angle, float x, float y, float z) {
            float[] m = new float[16];
            MatrixUtil.setRotateM(m, 0, (float) (angle * 180f / Math.PI), x, y, z);
            Matrix out = makeMatrix(m);
            out.factory = this;
            out.type = TYPE_ROTATION;
            return out;
        }
        
        

        public Matrix makeTranslation(float x, float y, float z) {
            Matrix m = makeIdentity();
            MatrixUtil.translateM(m.data, 0, x, y, z);
            m.factory = this;
            m.type = TYPE_TRANSLATION;
            return m;
        }

        public Matrix makePerspective(float fovy, float aspect, float zNear, float zFar) {
            float[] m = new float[16];
            MatrixUtil.perspectiveM(m, 0, (float) (fovy * 180f / Math.PI), aspect, zNear, zFar);
            Matrix out =  new Matrix(m);
            out.factory = this;
            return out;
        }

        public Matrix makeOrtho(float left, float right, float bottom, float top,
                float near, float far) {
            float[] m = new float[16];
            MatrixUtil.orthoM(m, 0, left, right, bottom, top, near, far);
            Matrix out = new Matrix(m);
            out.factory = this;
            return out;
        }

        public Matrix makeCamera(float eyeX, float eyeY, float eyeZ,
                float centerX, float centerY, float centerZ, float upX, float upY,
                float upZ) {

            float[] m = new float[16];
            MatrixUtil.setLookAtM(m, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
            Matrix out =  new Matrix(m);
            out.factory = this;
            return out;
        }
    }

    
    
    public static Matrix make(float[] data) {
        return Factory.getDefault().makeMatrix(data);
    }

    public static Matrix makeIdentity() {
        return Factory.getDefault().makeMatrix(null);
    }
    
    public static Matrix makeTranslation(float x, float y, float z){
        return Factory.getDefault().makeTranslation(x, y, z);
    }

    public static Matrix makeRotation(float angle, float x, float y, float z) {
        return Factory.getDefault().makeRotation(angle, x, y, z);
    }

    public static Matrix makeOrtho(float left, float right, float bottom, float top,
            float near, float far) {

        return Factory.getDefault().makeOrtho(left, right, bottom, top, near, far);
    }

    public static Matrix makePerspective(float fovy, float aspect, float zNear, float zFar) {
        return Factory.getDefault().makePerspective(fovy, aspect, zNear, zFar);
    }

    public static Matrix makeCamera(float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ, float upX, float upY,
            float upZ) {
        return Factory.getDefault().makeCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    public void rotate(float a, float x, float y, float z) {

        MatrixUtil.setRotateM(factory.sTemp, 0, (float) (a * 180f / Math.PI), x, y, z);
        MatrixUtil.multiplyMM(factory.sTemp, 16, data, 0, factory.sTemp, 0);
        System.arraycopy(factory.sTemp, 16, data, 0, 16);
        if ( type == TYPE_IDENTITY ){
            type = TYPE_ROTATION;
        } else {
            type = TYPE_UNKNOWN;
        }
    }

    public void translate(float x, float y, float z) {
        MatrixUtil.translateM(data, 0, x, y, z);
        if ( type == TYPE_IDENTITY || type == TYPE_TRANSLATION ){
            type = TYPE_TRANSLATION;
        } else {
            type = TYPE_UNKNOWN;
        }
    }

    public void scale(float x, float y, float z) {
        MatrixUtil.scaleM(data, 0, x, y, z);
        if ( type == TYPE_IDENTITY || type == TYPE_SCALE ){
            type = TYPE_SCALE;
        } else {
            type = TYPE_UNKNOWN;
        }
    }

    public void setPerspective(float fovy, float aspect, float zNear, float zFar) {
        MatrixUtil.perspectiveM(data, 0, (float) (fovy * 180f / Math.PI), aspect, zNear, zFar);
    }

    public void setOrtho(float left, float right, float bottom, float top,
            float near, float far) {
        MatrixUtil.orthoM(data, 0, left, right, bottom, top, near, far);
    }

    public void setCamera(float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ, float upX, float upY,
            float upZ) {
        MatrixUtil.setLookAtM(data, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    public void setIdentity() {
        reset();
    }

    
    
    public void transformCoord(float[] pIn, float[] pOut) {
        int len = pIn.length;
        factory.sTemp[2] = 0;
        factory.sTemp[3] = 1f;
        
        
        System.arraycopy(pIn, 0, factory.sTemp, 0, len);
        
        MatrixUtil.multiplyMV(factory.sTemp, 4, data, 0, factory.sTemp, 0);
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

    
    public boolean equals(Matrix m2){
        if ( m2 == null ){
            return false;
        }
        for ( int i=0; i<16; i++){
            if ( Math.abs(this.data[i]-m2.data[i]) > 0.0001 ){
                return false;
            }
        }
        return true;
        
    }
    
    public boolean isIdentity() {
        for (int i = 0; i < 16; i++) {
            if (i % 5 == 0 && Math.abs(data[i] - 1f) > 0.0001) {
                return false;
            } else if (i % 5 != 0 && Math.abs(data[i]) > 0.0001) {
                return false;
            }
        }
        return true;
    }

    public boolean invert() {
        boolean res = MatrixUtil.invertM(factory.sTemp, 0, data, 0);
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
    private Matrix(float[] m) {
        if (m == null) {
            m = new float[]{1f};
        }
        if (m.length == 16) {
            data = m;
        } else {
            data = new float[16];
            setData(m);
        }
    }

    public void concatenate(Matrix m){
        //MatrixUtil.setRotateM(factory.sTemp, 0, (float) (a * 180f / Math.PI), x, y, z);
        MatrixUtil.multiplyMM(factory.sTemp, 16, data, 0, m.data, 0);
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
     * Matrix</th></tr>
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

    public Matrix copy() {
        float[] data = new float[16];
        System.arraycopy(this.data, 0, data, 0, 16);
        return Matrix.make(data);
    }

    /*
     * Copyright (C) 2007 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    /**
     * Matrix math utilities. These methods operate on OpenGL ES format matrices
     * and vectors stored in float arrays.
     * <p>
     * Matrices are 4 x 4 column-vector matrices stored in column-major order:
     * <pre>
     *  m[offset +  0] m[offset +  4] m[offset +  8] m[offset + 12]
     *  m[offset +  1] m[offset +  5] m[offset +  9] m[offset + 13]
     *  m[offset +  2] m[offset +  6] m[offset + 10] m[offset + 14]
     *  m[offset +  3] m[offset +  7] m[offset + 11] m[offset + 15]</pre>
     *
     * Vectors are 4 x 1 column vectors stored in order:
     * <pre>
     * v[offset + 0]
     * v[offset + 1]
     * v[offset + 2]
     * v[offset + 3]</pre>
     */
    private static class MatrixUtil {

        private static float clamp(float val){
            float abs = Math.abs(val);
            
            if ( Math.abs(abs-Math.round(abs)) < 0.001 ){
                return Math.round(val);
            }
            return val;
        }
        
        /**
         * Temporary memory for operations that need temporary matrix data.
         */
    //private final static float[] sTemp = new float[32];
        /**
         * Multiplies two 4x4 matrices together and stores the result in a third
         * 4x4 matrix. In matrix notation: result = lhs x rhs. Due to the way
         * matrix multiplication works, the result matrix will have the same
         * effect as first multiplying by the rhs matrix, then multiplying by
         * the lhs matrix. This is the opposite of what you might expect.
         * <p>
         * The same float array may be passed for result, lhs, and/or rhs.
         * However, the result element values are undefined if the result
         * elements overlap either the lhs or rhs elements.
         *
         * @param result The float array that holds the result.
         * @param resultOffset The offset into the result array where the result
         * is stored.
         * @param lhs The float array that holds the left-hand-side matrix.
         * @param lhsOffset The offset into the lhs array where the lhs is
         * stored
         * @param rhs The float array that holds the right-hand-side matrix.
         * @param rhsOffset The offset into the rhs array where the rhs is
         * stored.
         *
         * @throws IllegalArgumentException if result, lhs, or rhs are null, or
         * if resultOffset + 16 > result.length or lhsOffset + 16 > lhs.length
         * or rhsOffset + 16 > rhs.length.
         */
        public native static void multiplyMM(float[] result, int resultOffset,
                float[] lhs, int lhsOffset, float[] rhs, int rhsOffset);/* {
                float[] tmp = result;
                float[] mata = lhs;
                float[] matb = rhs;
                int a0 = lhsOffset;
                int b0 = rhsOffset;
                //int r0 = resultOffset;
            
                
                
            tmp[M00+resultOffset] = clamp(mata[M00+a0] * matb[M00+b0] + mata[M01] * matb[M10+b0] + mata[M02+a0] * matb[M20+b0] + mata[M03+a0] * matb[M30+b0]);
            tmp[M01+resultOffset] = clamp(mata[M00+a0] * matb[M01+b0] + mata[M01] * matb[M11+b0] + mata[M02+a0] * matb[M21+b0] + mata[M03+a0] * matb[M31+b0]);
            tmp[M02+resultOffset] = clamp(mata[M00+a0] * matb[M02+b0] + mata[M01] * matb[M12+b0] + mata[M02+a0] * matb[M22+b0] + mata[M03+a0] * matb[M32+b0]);
            tmp[M03+resultOffset] = clamp(mata[M00+a0] * matb[M03+b0] + mata[M01] * matb[M13+b0] + mata[M02+a0] * matb[M23+b0] + mata[M03+a0] * matb[M33+b0]);
            tmp[M10+resultOffset] = clamp(mata[M10+a0] * matb[M00+b0] + mata[M11] * matb[M10+b0] + mata[M12+a0] * matb[M20+b0] + mata[M13+a0] * matb[M30+b0]);
            tmp[M11+resultOffset] = clamp(mata[M10+a0] * matb[M01+b0] + mata[M11] * matb[M11+b0] + mata[M12+a0] * matb[M21+b0] + mata[M13+a0] * matb[M31+b0]);
            tmp[M12+resultOffset] = clamp(mata[M10+a0] * matb[M02+b0] + mata[M11] * matb[M12+b0] + mata[M12+a0] * matb[M22+b0] + mata[M13+a0] * matb[M32+b0]);
            tmp[M13+resultOffset] = clamp(mata[M10+a0] * matb[M03+b0] + mata[M11] * matb[M13+b0] + mata[M12+a0] * matb[M23+b0] + mata[M13+a0] * matb[M33+b0]);
            tmp[M20+resultOffset] = clamp(mata[M20+a0] * matb[M00+b0] + mata[M21] * matb[M10+b0] + mata[M22+a0] * matb[M20+b0] + mata[M23+a0] * matb[M30+b0]);
            tmp[M21+resultOffset] = clamp(mata[M20+a0] * matb[M01+b0] + mata[M21] * matb[M11+b0] + mata[M22+a0] * matb[M21+b0] + mata[M23+a0] * matb[M31+b0]);
            tmp[M22+resultOffset] = clamp(mata[M20+a0] * matb[M02+b0] + mata[M21] * matb[M12+b0] + mata[M22+a0] * matb[M22+b0] + mata[M23+a0] * matb[M32+b0]);
            tmp[M23+resultOffset] = clamp(mata[M20+a0] * matb[M03+b0] + mata[M21] * matb[M13+b0] + mata[M22+a0] * matb[M23+b0] + mata[M23+a0] * matb[M33+b0]);
            tmp[M30+resultOffset] = clamp(mata[M30+a0] * matb[M00+b0] + mata[M31] * matb[M10+b0] + mata[M32+a0] * matb[M20+b0] + mata[M33+a0] * matb[M30+b0]);
            tmp[M31+resultOffset] = clamp(mata[M30+a0] * matb[M01+b0] + mata[M31] * matb[M11+b0] + mata[M32+a0] * matb[M21+b0] + mata[M33+a0] * matb[M31+b0]);
            tmp[M32+resultOffset] = clamp(mata[M30+a0] * matb[M02+b0] + mata[M31] * matb[M12+b0] + mata[M32+a0] * matb[M22+b0] + mata[M33+a0] * matb[M32+b0]);
            tmp[M33+resultOffset] = clamp(mata[M30+a0] * matb[M03+b0] + mata[M31] * matb[M13+b0] + mata[M32+a0] * matb[M23+b0] + mata[M33+a0] * matb[M33+b0]);
           
            

        }*/

        /**
         * Multiplies a 4 element vector by a 4x4 matrix and stores the result
         * in a 4-element column vector. In matrix notation: result = lhs x rhs
         * <p>
         * The same float array may be passed for resultVec, lhsMat, and/or
         * rhsVec. However, the resultVec element values are undefined if the
         * resultVec elements overlap either the lhsMat or rhsVec elements.
         *
         * @param resultVec The float array that holds the result vector.
         * @param resultVecOffset The offset into the result array where the
         * result vector is stored.
         * @param lhsMat The float array that holds the left-hand-side matrix.
         * @param lhsMatOffset The offset into the lhs array where the lhs is
         * stored
         * @param rhsVec The float array that holds the right-hand-side vector.
         * @param rhsVecOffset The offset into the rhs vector where the rhs
         * vector is stored.
         *
         * @throws IllegalArgumentException if resultVec, lhsMat, or rhsVec are
         * null, or if resultVecOffset + 4 > resultVec.length or lhsMatOffset +
         * 16 > lhsMat.length or rhsVecOffset + 4 > rhsVec.length.
         */
        public static void multiplyMV(float[] resultVec,
                int resultVecOffset, float[] lhsMat, int lhsMatOffset,
                float[] rhsVec, int rhsVecOffset) {

            
            
            resultVec[resultVecOffset] =    clamp(lhsMat[lhsMatOffset] * rhsVec[rhsVecOffset] + 
                                            lhsMat[lhsMatOffset+4] * rhsVec[rhsVecOffset+1] +
                                            lhsMat[lhsMatOffset+8] * rhsVec[rhsVecOffset+2] +
                                            lhsMat[lhsMatOffset+12] * rhsVec[rhsVecOffset+3]);
            
            
            resultVec[resultVecOffset+1] =  clamp(lhsMat[lhsMatOffset+1] * rhsVec[rhsVecOffset] + 
                                            lhsMat[lhsMatOffset+5] * rhsVec[rhsVecOffset+1] +
                                            lhsMat[lhsMatOffset+9] * rhsVec[rhsVecOffset+2] +
                                            lhsMat[lhsMatOffset+13] * rhsVec[rhsVecOffset+3]);
            
            
            resultVec[resultVecOffset+2] =  clamp(lhsMat[lhsMatOffset+2] * rhsVec[rhsVecOffset] + 
                                            lhsMat[lhsMatOffset+6] * rhsVec[rhsVecOffset+1] +
                                            lhsMat[lhsMatOffset+10] * rhsVec[rhsVecOffset+2] +
                                            lhsMat[lhsMatOffset+14] * rhsVec[rhsVecOffset+3]);
            
            resultVec[resultVecOffset+3] =  clamp(lhsMat[lhsMatOffset+3] * rhsVec[rhsVecOffset] + 
                                            lhsMat[lhsMatOffset+7] * rhsVec[rhsVecOffset+1] +
                                            lhsMat[lhsMatOffset+11] * rhsVec[rhsVecOffset+2] +
                                            lhsMat[lhsMatOffset+15] * rhsVec[rhsVecOffset+3]);
                    
            

        }

        /**
         * Transposes a 4 x 4 matrix.
         * <p>
         * mTrans and m must not overlap.
         *
         * @param mTrans the array that holds the output transposed matrix
         * @param mTransOffset an offset into mTrans where the transposed matrix
         * is stored.
         * @param m the input array
         * @param mOffset an offset into m where the input matrix is stored.
         */
        public static void transposeM(float[] mTrans, int mTransOffset, float[] m,
                int mOffset) {
            for (int i = 0; i < 4; i++) {
                int mBase = i * 4 + mOffset;
                mTrans[i + mTransOffset] = m[mBase];
                mTrans[i + 4 + mTransOffset] = m[mBase + 1];
                mTrans[i + 8 + mTransOffset] = m[mBase + 2];
                mTrans[i + 12 + mTransOffset] = m[mBase + 3];
            }
        }

        /**
         * Inverts a 4 x 4 matrix.
         * <p>
         * mInv and m must not overlap.
         *
         * @param mInv the array that holds the output inverted matrix
         * @param mInvOffset an offset into mInv where the inverted matrix is
         * stored.
         * @param m the input array
         * @param mOffset an offset into m where the input matrix is stored.
         * @return true if the matrix could be inverted, false if it could not.
         */
        public native static boolean invertM(float[] mInv, int mInvOffset, float[] m,
                int mOffset);/* {
        // Invert a 4 x 4 matrix using Cramer's Rule

            // transpose matrix
            final float src0 = m[mOffset + 0];
            final float src4 = m[mOffset + 1];
            final float src8 = m[mOffset + 2];
            final float src12 = m[mOffset + 3];

            final float src1 = m[mOffset + 4];
            final float src5 = m[mOffset + 5];
            final float src9 = m[mOffset + 6];
            final float src13 = m[mOffset + 7];

            final float src2 = m[mOffset + 8];
            final float src6 = m[mOffset + 9];
            final float src10 = m[mOffset + 10];
            final float src14 = m[mOffset + 11];

            final float src3 = m[mOffset + 12];
            final float src7 = m[mOffset + 13];
            final float src11 = m[mOffset + 14];
            final float src15 = m[mOffset + 15];

            // calculate pairs for first 8 elements (cofactors)
            final float atmp0 = src10 * src15;
            final float atmp1 = src11 * src14;
            final float atmp2 = src9 * src15;
            final float atmp3 = src11 * src13;
            final float atmp4 = src9 * src14;
            final float atmp5 = src10 * src13;
            final float atmp6 = src8 * src15;
            final float atmp7 = src11 * src12;
            final float atmp8 = src8 * src14;
            final float atmp9 = src10 * src12;
            final float atmp10 = src8 * src13;
            final float atmp11 = src9 * src12;

            // calculate first 8 elements (cofactors)
            final float dst0 = (atmp0 * src5 + atmp3 * src6 + atmp4 * src7)
                    - (atmp1 * src5 + atmp2 * src6 + atmp5 * src7);
            final float dst1 = (atmp1 * src4 + atmp6 * src6 + atmp9 * src7)
                    - (atmp0 * src4 + atmp7 * src6 + atmp8 * src7);
            final float dst2 = (atmp2 * src4 + atmp7 * src5 + atmp10 * src7)
                    - (atmp3 * src4 + atmp6 * src5 + atmp11 * src7);
            final float dst3 = (atmp5 * src4 + atmp8 * src5 + atmp11 * src6)
                    - (atmp4 * src4 + atmp9 * src5 + atmp10 * src6);
            final float dst4 = (atmp1 * src1 + atmp2 * src2 + atmp5 * src3)
                    - (atmp0 * src1 + atmp3 * src2 + atmp4 * src3);
            final float dst5 = (atmp0 * src0 + atmp7 * src2 + atmp8 * src3)
                    - (atmp1 * src0 + atmp6 * src2 + atmp9 * src3);
            final float dst6 = (atmp3 * src0 + atmp6 * src1 + atmp11 * src3)
                    - (atmp2 * src0 + atmp7 * src1 + atmp10 * src3);
            final float dst7 = (atmp4 * src0 + atmp9 * src1 + atmp10 * src2)
                    - (atmp5 * src0 + atmp8 * src1 + atmp11 * src2);

            // calculate pairs for second 8 elements (cofactors)
            final float btmp0 = src2 * src7;
            final float btmp1 = src3 * src6;
            final float btmp2 = src1 * src7;
            final float btmp3 = src3 * src5;
            final float btmp4 = src1 * src6;
            final float btmp5 = src2 * src5;
            final float btmp6 = src0 * src7;
            final float btmp7 = src3 * src4;
            final float btmp8 = src0 * src6;
            final float btmp9 = src2 * src4;
            final float btmp10 = src0 * src5;
            final float btmp11 = src1 * src4;

            // calculate second 8 elements (cofactors)
            final float dst8 = (btmp0 * src13 + btmp3 * src14 + btmp4 * src15)
                    - (btmp1 * src13 + btmp2 * src14 + btmp5 * src15);
            final float dst9 = (btmp1 * src12 + btmp6 * src14 + btmp9 * src15)
                    - (btmp0 * src12 + btmp7 * src14 + btmp8 * src15);
            final float dst10 = (btmp2 * src12 + btmp7 * src13 + btmp10 * src15)
                    - (btmp3 * src12 + btmp6 * src13 + btmp11 * src15);
            final float dst11 = (btmp5 * src12 + btmp8 * src13 + btmp11 * src14)
                    - (btmp4 * src12 + btmp9 * src13 + btmp10 * src14);
            final float dst12 = (btmp2 * src10 + btmp5 * src11 + btmp1 * src9)
                    - (btmp4 * src11 + btmp0 * src9 + btmp3 * src10);
            final float dst13 = (btmp8 * src11 + btmp0 * src8 + btmp7 * src10)
                    - (btmp6 * src10 + btmp9 * src11 + btmp1 * src8);
            final float dst14 = (btmp6 * src9 + btmp11 * src11 + btmp3 * src8)
                    - (btmp10 * src11 + btmp2 * src8 + btmp7 * src9);
            final float dst15 = (btmp10 * src10 + btmp4 * src8 + btmp9 * src9)
                    - (btmp8 * src9 + btmp11 * src10 + btmp5 * src8);

            // calculate determinant
            final float det
                    = src0 * dst0 + src1 * dst1 + src2 * dst2 + src3 * dst3;

            if (det == 0.0f) {
                return false;
            }

            // calculate matrix inverse
            final float invdet = 1.0f / det;
            mInv[     mInvOffset] = clamp(dst0 * invdet);
            mInv[ 1 + mInvOffset] = clamp(dst1 * invdet);
            mInv[ 2 + mInvOffset] = clamp(dst2 * invdet);
            mInv[ 3 + mInvOffset] = clamp(dst3 * invdet);

            mInv[ 4 + mInvOffset] = clamp(dst4 * invdet);
            mInv[ 5 + mInvOffset] = clamp(dst5 * invdet);
            mInv[ 6 + mInvOffset] = clamp(dst6 * invdet);
            mInv[ 7 + mInvOffset] = clamp(dst7 * invdet);

            mInv[ 8 + mInvOffset] = clamp(dst8 * invdet);
            mInv[ 9 + mInvOffset] = clamp(dst9 * invdet);
            mInv[10 + mInvOffset] = clamp(dst10 * invdet);
            mInv[11 + mInvOffset] = clamp(dst11 * invdet);

            mInv[12 + mInvOffset] = clamp(dst12 * invdet);
            mInv[13 + mInvOffset] = clamp(dst13 * invdet);
            mInv[14 + mInvOffset] = clamp(dst14 * invdet);
            mInv[15 + mInvOffset] = clamp(dst15 * invdet);

            return true;
        
        }*/

        /**
         * Computes an orthographic projection matrix.
         *
         * @param m returns the result
         * @param mOffset
         * @param left
         * @param right
         * @param bottom
         * @param top
         * @param near
         * @param far
         */
        public static void orthoM(float[] m, int mOffset,
                float left, float right, float bottom, float top,
                float near, float far) {
            if (left == right) {
                throw new IllegalArgumentException("left == right");
            }
            if (bottom == top) {
                throw new IllegalArgumentException("bottom == top");
            }
            if (near == far) {
                throw new IllegalArgumentException("near == far");
            }

            final float r_width = 1.0f / (right - left);
            final float r_height = 1.0f / (top - bottom);
            final float r_depth = 1.0f / (far - near);
            final float x = 2.0f * (r_width);
            final float y = 2.0f * (r_height);
            final float z = -2.0f * (r_depth);
            final float tx = -(right + left) * r_width;
            final float ty = -(top + bottom) * r_height;
            final float tz = -(far + near) * r_depth;
            m[mOffset + 0] = x;
            m[mOffset + 5] = y;
            m[mOffset + 10] = z;
            m[mOffset + 12] = tx;
            m[mOffset + 13] = ty;
            m[mOffset + 14] = tz;
            m[mOffset + 15] = 1.0f;
            m[mOffset + 1] = 0.0f;
            m[mOffset + 2] = 0.0f;
            m[mOffset + 3] = 0.0f;
            m[mOffset + 4] = 0.0f;
            m[mOffset + 6] = 0.0f;
            m[mOffset + 7] = 0.0f;
            m[mOffset + 8] = 0.0f;
            m[mOffset + 9] = 0.0f;
            m[mOffset + 11] = 0.0f;
        }

        /**
         * Defines a projection matrix in terms of six clip planes.
         *
         * @param m the float array that holds the output perspective matrix
         * @param offset the offset into float array m where the perspective
         * matrix data is written
         * @param left
         * @param right
         * @param bottom
         * @param top
         * @param near
         * @param far
         */
        public static void frustumM(float[] m, int offset,
                float left, float right, float bottom, float top,
                float near, float far) {
            if (left == right) {
                throw new IllegalArgumentException("left == right");
            }
            if (top == bottom) {
                throw new IllegalArgumentException("top == bottom");
            }
            if (near == far) {
                throw new IllegalArgumentException("near == far");
            }
            if (near <= 0.0f) {
                throw new IllegalArgumentException("near <= 0.0f");
            }
            if (far <= 0.0f) {
                throw new IllegalArgumentException("far <= 0.0f");
            }
            final float r_width = 1.0f / (right - left);
            final float r_height = 1.0f / (top - bottom);
            final float r_depth = 1.0f / (near - far);
            final float x = 2.0f * (near * r_width);
            final float y = 2.0f * (near * r_height);
            final float A = (right + left) * r_width;
            final float B = (top + bottom) * r_height;
            final float C = (far + near) * r_depth;
            final float D = 2.0f * (far * near * r_depth);
            m[offset + 0] = x;
            m[offset + 5] = y;
            m[offset + 8] = A;
            m[offset + 9] = B;
            m[offset + 10] = C;
            m[offset + 14] = D;
            m[offset + 11] = -1.0f;
            m[offset + 1] = 0.0f;
            m[offset + 2] = 0.0f;
            m[offset + 3] = 0.0f;
            m[offset + 4] = 0.0f;
            m[offset + 6] = 0.0f;
            m[offset + 7] = 0.0f;
            m[offset + 12] = 0.0f;
            m[offset + 13] = 0.0f;
            m[offset + 15] = 0.0f;
        }

        /**
         * Defines a projection matrix in terms of a field of view angle, an
         * aspect ratio, and z clip planes.
         *
         * @param m the float array that holds the perspective matrix
         * @param offset the offset into float array m where the perspective
         * matrix data is written
         * @param fovy field of view in y direction, in degrees
         * @param aspect width to height aspect ratio of the viewport
         * @param zNear
         * @param zFar
         */
        public static void perspectiveM(float[] m, int offset,
                float fovy, float aspect, float zNear, float zFar) {
            float f = 1.0f / (float) Math.tan(fovy * (Math.PI / 360.0));
            float rangeReciprocal = 1.0f / (zNear - zFar);

            m[offset + 0] = f / aspect;
            m[offset + 1] = 0.0f;
            m[offset + 2] = 0.0f;
            m[offset + 3] = 0.0f;

            m[offset + 4] = 0.0f;
            m[offset + 5] = f;
            m[offset + 6] = 0.0f;
            m[offset + 7] = 0.0f;

            m[offset + 8] = 0.0f;
            m[offset + 9] = 0.0f;
            m[offset + 10] = (zFar + zNear) * rangeReciprocal;
            m[offset + 11] = -1.0f;

            m[offset + 12] = 0.0f;
            m[offset + 13] = 0.0f;
            m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal;
            m[offset + 15] = 0.0f;
        }

        /**
         * Computes the length of a vector.
         *
         * @param x x coordinate of a vector
         * @param y y coordinate of a vector
         * @param z z coordinate of a vector
         * @return the length of a vector
         */
        public static float length(float x, float y, float z) {
            return (float) Math.sqrt(x * x + y * y + z * z);
        }

        /**
         * Sets matrix m to the identity matrix.
         *
         * @param sm returns the result
         * @param smOffset index into sm where the result matrix starts
         */
        public static void setIdentityM(float[] sm, int smOffset) {
            for (int i = 0; i < 16; i++) {
                sm[smOffset + i] = 0;
            }
            for (int i = 0; i < 16; i += 5) {
                sm[smOffset + i] = 1.0f;
            }
        }

        /**
         * Scales matrix m by x, y, and z, putting the result in sm.
         * <p>
         * m and sm must not overlap.
         *
         * @param sm returns the result
         * @param smOffset index into sm where the result matrix starts
         * @param m source matrix
         * @param mOffset index into m where the source matrix starts
         * @param x scale factor x
         * @param y scale factor y
         * @param z scale factor z
         */
        public static void scaleM(float[] sm, int smOffset,
                float[] m, int mOffset,
                float x, float y, float z) {
            for (int i = 0; i < 4; i++) {
                int smi = smOffset + i;
                int mi = mOffset + i;
                sm[     smi] = clamp(m[     mi] * x);
                sm[ 4 + smi] = clamp(m[ 4 + mi] * y);
                sm[ 8 + smi] = clamp(m[ 8 + mi] * z);
                sm[12 + smi] = clamp(m[12 + mi]);
            }
        }

        /**
         * Scales matrix m in place by sx, sy, and sz.
         *
         * @param m matrix to scale
         * @param mOffset index into m where the matrix starts
         * @param x scale factor x
         * @param y scale factor y
         * @param z scale factor z
         */
        public static void scaleM(float[] m, int mOffset,
                float x, float y, float z) {
            for (int i = 0; i < 4; i++) {
                int mi = mOffset + i;
                m[     mi] = clamp(m[     mi] * x);
                m[ 4 + mi] = clamp(m[ 4 + mi] * y);
                m[ 8 + mi] = clamp(m[ 8 + mi] * z);
            }
        }

        /**
         * Translates matrix m by x, y, and z, putting the result in tm.
         * <p>
         * m and tm must not overlap.
         *
         * @param tm returns the result
         * @param tmOffset index into sm where the result matrix starts
         * @param m source matrix
         * @param mOffset index into m where the source matrix starts
         * @param x translation factor x
         * @param y translation factor y
         * @param z translation factor z
         */
        public static void translateM(float[] tm, int tmOffset,
                float[] m, int mOffset,
                float x, float y, float z) {
            for (int i = 0; i < 12; i++) {
                tm[tmOffset + i] = m[mOffset + i];
            }
            for (int i = 0; i < 4; i++) {
                int tmi = tmOffset + i;
                int mi = mOffset + i;
                tm[12 + tmi] = clamp(m[mi] * x + m[4 + mi] * y + m[8 + mi] * z
                        + m[12 + mi]);
            }
        }

        /**
         * Translates matrix m by x, y, and z in place.
         *
         * @param m matrix
         * @param mOffset index into m where the matrix starts
         * @param x translation factor x
         * @param y translation factor y
         * @param z translation factor z
         */
        public static void translateM(
                float[] m, int mOffset,
                float x, float y, float z) {
            for (int i = 0; i < 4; i++) {
                int mi = mOffset + i;
                m[12 + mi] = clamp(m[12 + mi] + m[mi] * x + m[4 + mi] * y + m[8 + mi] * z);
            }
        }

        /**
         * Rotates matrix m by angle a (in degrees) around the axis (x, y, z).
         * <p>
         * m and rm must not overlap.
         *
         * @param rm returns the result
         * @param rmOffset index into rm where the result matrix starts
         * @param m source matrix
         * @param mOffset index into m where the source matrix starts
         * @param a angle to rotate in degrees
         * @param x X axis component
         * @param y Y axis component
         * @param z Z axis component
         *
         * public static void rotateM(float[] rm, int rmOffset, float[] m, int
         * mOffset, float a, float x, float y, float z) { synchronized(sTemp) {
         * setRotateM(sTemp, 0, a, x, y, z); multiplyMM(rm, rmOffset, m,
         * mOffset, sTemp, 0); } }
    *
         */
        /**
         * Rotates matrix m in place by angle a (in degrees) around the axis (x,
         * y, z).
         *
         * @param m source matrix
         * @param mOffset index into m where the matrix starts
         * @param a angle to rotate in degrees
         * @param x X axis component
         * @param y Y axis component
         * @param z Z axis component
         */
    //public static void rotateM(float[] m, int mOffset,
        //        float a, float x, float y, float z) {
        //    synchronized(sTemp) {
        //        setRotateM(sTemp, 0, a, x, y, z);
        //        multiplyMM(sTemp, 16, m, mOffset, sTemp, 0);
        //        System.arraycopy(sTemp, 16, m, mOffset, 16);
        //    }
        //}
        /**
         * Creates a matrix for rotation by angle a (in degrees) around the axis
         * (x, y, z).
         * <p>
         * An optimized path will be used for rotation about a major axis (e.g.
         * x=1.0f y=0.0f z=0.0f).
         *
         * @param rm returns the result
         * @param rmOffset index into rm where the result matrix starts
         * @param a angle to rotate in degrees
         * @param x X axis component
         * @param y Y axis component
         * @param z Z axis component
         */
        public static void setRotateM(float[] rm, int rmOffset,
                float a, float x, float y, float z) {
            rm[rmOffset + 3] = 0;
            rm[rmOffset + 7] = 0;
            rm[rmOffset + 11] = 0;
            rm[rmOffset + 12] = 0;
            rm[rmOffset + 13] = 0;
            rm[rmOffset + 14] = 0;
            rm[rmOffset + 15] = 1;
            a *= (float) (Math.PI / 180.0f);
            float s = (float) Math.sin(a);
            float c = (float) Math.cos(a);
            if (1.0f == x && 0.0f == y && 0.0f == z) {
                rm[rmOffset + 5] = c;
                rm[rmOffset + 10] = c;
                rm[rmOffset + 6] = s;
                rm[rmOffset + 9] = -s;
                rm[rmOffset + 1] = 0;
                rm[rmOffset + 2] = 0;
                rm[rmOffset + 4] = 0;
                rm[rmOffset + 8] = 0;
                rm[rmOffset + 0] = 1;
            } else if (0.0f == x && 1.0f == y && 0.0f == z) {
                rm[rmOffset + 0] = c;
                rm[rmOffset + 10] = c;
                rm[rmOffset + 8] = s;
                rm[rmOffset + 2] = -s;
                rm[rmOffset + 1] = 0;
                rm[rmOffset + 4] = 0;
                rm[rmOffset + 6] = 0;
                rm[rmOffset + 9] = 0;
                rm[rmOffset + 5] = 1;
            } else if (0.0f == x && 0.0f == y && 1.0f == z) {
                rm[rmOffset + 0] = c;
                rm[rmOffset + 5] = c;
                rm[rmOffset + 1] = s;
                rm[rmOffset + 4] = -s;
                rm[rmOffset + 2] = 0;
                rm[rmOffset + 6] = 0;
                rm[rmOffset + 8] = 0;
                rm[rmOffset + 9] = 0;
                rm[rmOffset + 10] = 1;
            } else {
                float len = length(x, y, z);
                if (1.0f != len) {
                    float recipLen = 1.0f / len;
                    x *= recipLen;
                    y *= recipLen;
                    z *= recipLen;
                }
                float nc = 1.0f - c;
                float xy = x * y;
                float yz = y * z;
                float zx = z * x;
                float xs = x * s;
                float ys = y * s;
                float zs = z * s;
                rm[rmOffset + 0] = x * x * nc + c;
                rm[rmOffset + 4] = xy * nc - zs;
                rm[rmOffset + 8] = zx * nc + ys;
                rm[rmOffset + 1] = xy * nc + zs;
                rm[rmOffset + 5] = y * y * nc + c;
                rm[rmOffset + 9] = yz * nc - xs;
                rm[rmOffset + 2] = zx * nc - ys;
                rm[rmOffset + 6] = yz * nc + xs;
                rm[rmOffset + 10] = z * z * nc + c;
            }
        }

        /**
         * Converts Euler angles to a rotation matrix.
         *
         * @param rm returns the result
         * @param rmOffset index into rm where the result matrix starts
         * @param x angle of rotation, in degrees
         * @param y angle of rotation, in degrees
         * @param z angle of rotation, in degrees
         */
        public static void setRotateEulerM(float[] rm, int rmOffset,
                float x, float y, float z) {
            x *= (float) (Math.PI / 180.0f);
            y *= (float) (Math.PI / 180.0f);
            z *= (float) (Math.PI / 180.0f);
            float cx = (float) Math.cos(x);
            float sx = (float) Math.sin(x);
            float cy = (float) Math.cos(y);
            float sy = (float) Math.sin(y);
            float cz = (float) Math.cos(z);
            float sz = (float) Math.sin(z);
            float cxsy = cx * sy;
            float sxsy = sx * sy;

            rm[rmOffset + 0] = cy * cz;
            rm[rmOffset + 1] = -cy * sz;
            rm[rmOffset + 2] = sy;
            rm[rmOffset + 3] = 0.0f;

            rm[rmOffset + 4] = cxsy * cz + cx * sz;
            rm[rmOffset + 5] = -cxsy * sz + cx * cz;
            rm[rmOffset + 6] = -sx * cy;
            rm[rmOffset + 7] = 0.0f;

            rm[rmOffset + 8] = -sxsy * cz + sx * sz;
            rm[rmOffset + 9] = sxsy * sz + sx * cz;
            rm[rmOffset + 10] = cx * cy;
            rm[rmOffset + 11] = 0.0f;

            rm[rmOffset + 12] = 0.0f;
            rm[rmOffset + 13] = 0.0f;
            rm[rmOffset + 14] = 0.0f;
            rm[rmOffset + 15] = 1.0f;
        }

        /**
         * Defines a viewing transformation in terms of an eye point, a center
         * of view, and an up vector.
         *
         * @param rm returns the result
         * @param rmOffset index into rm where the result matrix starts
         * @param eyeX eye point X
         * @param eyeY eye point Y
         * @param eyeZ eye point Z
         * @param centerX center of view X
         * @param centerY center of view Y
         * @param centerZ center of view Z
         * @param upX up vector X
         * @param upY up vector Y
         * @param upZ up vector Z
         */
        public static void setLookAtM(float[] rm, int rmOffset,
                float eyeX, float eyeY, float eyeZ,
                float centerX, float centerY, float centerZ, float upX, float upY,
                float upZ) {

        // See the OpenGL GLUT documentation for gluLookAt for a description
            // of the algorithm. We implement it in a straightforward way:
            float fx = centerX - eyeX;
            float fy = centerY - eyeY;
            float fz = centerZ - eyeZ;

            // Normalize f
            float rlf = 1.0f / MatrixUtil.length(fx, fy, fz);
            fx *= rlf;
            fy *= rlf;
            fz *= rlf;

            // compute s = f x up (x means "cross product")
            float sx = fy * upZ - fz * upY;
            float sy = fz * upX - fx * upZ;
            float sz = fx * upY - fy * upX;

            // and normalize s
            float rls = 1.0f / MatrixUtil.length(sx, sy, sz);
            sx *= rls;
            sy *= rls;
            sz *= rls;

            // compute u = s x f
            float ux = sy * fz - sz * fy;
            float uy = sz * fx - sx * fz;
            float uz = sx * fy - sy * fx;

            rm[rmOffset + 0] = sx;
            rm[rmOffset + 1] = ux;
            rm[rmOffset + 2] = -fx;
            rm[rmOffset + 3] = 0.0f;

            rm[rmOffset + 4] = sy;
            rm[rmOffset + 5] = uy;
            rm[rmOffset + 6] = -fy;
            rm[rmOffset + 7] = 0.0f;

            rm[rmOffset + 8] = sz;
            rm[rmOffset + 9] = uz;
            rm[rmOffset + 10] = -fz;
            rm[rmOffset + 11] = 0.0f;

            rm[rmOffset + 12] = 0.0f;
            rm[rmOffset + 13] = 0.0f;
            rm[rmOffset + 14] = 0.0f;
            rm[rmOffset + 15] = 1.0f;

            translateM(rm, rmOffset, -eyeX, -eyeY, -eyeZ);
        }

       

       
    }

    
    

  
}
