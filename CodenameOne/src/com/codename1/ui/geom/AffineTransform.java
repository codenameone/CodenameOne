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
package com.codename1.ui.geom;

import com.codename1.ui.Transform;

/**
 * A utility class for expressing 2-D affine transforms in Codename One.
 * @author shannah
 * @since 7.0
 */
public class AffineTransform {
    private double m00, m10, m01, m11, m02, m12;
    
    /**
     * Creates identity transform.
     */
    public AffineTransform() {
        m00 = m11 = 1.0;
    }
    
    private void check() {
        if (m10 != 0 || m01 != 0) {
            throw new IllegalArgumentException("Shearing not currently supported in AffineTransform");
        }
        
    }
    /**
     * Creates new affine transform as a copy of the given transform.
     * @param Tx Transform to copy.
     */
    public AffineTransform(AffineTransform Tx) {
        m00 = Tx.m00;
        m10 = Tx.m10;
        m01 = Tx.m01;
        m11 = Tx.m11;
        m02 = Tx.m02;
        m12 = Tx.m12;
        check();
        
    }
    
    /**
     * Creates a new AffineTransform.
     * @param m00 the X coordinate scaling element of the 3x3 matrix
     * @param m10 the Y coordinate shearing element of the 3x3 matrix
     * @param m01 the X coordinate shearing element of the 3x3 matrix
     * @param m11 the Y coordinate scaling element of the 3x3 matrix
     * @param m02 the X coordinate translation element of the 3x3 matrix
     * @param m12 the Y coordinate translation element of the 3x3 matrix
     */
    public AffineTransform(float m00,
                                          float m10,
                                          float m01,
                                          float m11,
                                          float m02,
                                          float m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        check();
    }
    
    /**
     * Creates a new AffineTransform.
     * @param m the float array containing the values to be set
     * in the new <code>AffineTransform</code> object. The length of the
     * array is assumed to be at least 4. If the length of the array is
     * less than 6, only the first 4 values are taken. If the length of
     * the array is greater than 6, the first 6 values are taken.
     */
    public AffineTransform(float[] m) {
        int len = m.length;
        m00 = m[0];
        m10 = m[1];
        m01 = m[2];
        m11 = m[3];
        if (len >= 6) {
            m02 = m[4];
            m12 = m[5];
        }
        check();
    }
    
    /**
     * Creates a new AffineTransform.
     *  @param m00 the X coordinate scaling element of the 3x3 matrix
     * @param m10 the Y coordinate shearing element of the 3x3 matrix
     * @param m01 the X coordinate shearing element of the 3x3 matrix
     * @param m11 the Y coordinate scaling element of the 3x3 matrix
     * @param m02 the X coordinate translation element of the 3x3 matrix
     * @param m12 the Y coordinate translation element of the 3x3 matrix
     */
    public AffineTransform(
            double m00,
            double m10,
            double m01,
            double m11,
            double m02,
            double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        check();
    }
    
    /**
     * Creates a new AffineTransform.
     * @param m the double array containing the values to be set
     * in the new <code>AffineTransform</code> object. The length of the
     * array is assumed to be at least 4. If the length of the array is
     * less than 6, only the first 4 values are taken. If the length of
     * the array is greater than 6, the first 6 values are taken.
     */
    public AffineTransform(double[] m) {
        int len = m.length;
        m00 = m[0];
        m10 = m[1];
        m01 = m[2];
        m11 = m[3];
        if (len >= 6) {
            m02 = m[4];
            m12 = m[5];
        }
        check();
    }
    
    /**
     * Converts the transform to a {@link Transform}
     * @return 
     */
    public Transform toTransform() {
        Transform t = Transform.makeIdentity();
        t.translate((float)m02, (float)m12);
        t.scale((float)m00, (float)m11);
        return t;
    }

    @Override
    public String toString() {
        return "AffineTransform{"+m00+","+m10+","+m01+","+m11+","+m02+","+m12+"}";
    }
    
    
    
    
}
