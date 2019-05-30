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
        //if (m10 != 0 || m01 != 0) {
        //    throw new IllegalArgumentException("Shearing not currently supported in AffineTransform");
        //}
        
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
     * Sets transform to a scale transform.
     * @param sx X-scale factor
     * @param sy Y-scale factor
     */
    public void setToScale(double sx, double sy) {
        
        m00 = sx;
        m10 = 0.0;
        m01 = 0.0;
        m11 = sy;
        m02 = 0.0;
        m12 = 0.0;
    }
    
    /**
     * Sets transform to a shear transform.
     * @param shx The shear-x
     * @param shy The shear-y
     */
    public void setToShear(double shx, double shy) {
        m00 = 1.0;
        m01 = shx;
        m10 = shy;
        m11 = 1.0;
        m02 = 0.0;
        m12 = 0.0;
        
    }
    
    /**
     * Sets transform to a rotation transform.
     * @param vecx x-coordinate of rotation vector.
     * @param vecy y-coordinate of rotation vector.
     */
    public void setToRotation(double vecx, double vecy) {
        double sin, cos;
        if (vecy == 0) {
            sin = 0.0;
            if (vecx < 0.0) {
                cos = -1.0;
                
            } else {
                cos = 1.0;
                
            }
        } else if (vecx == 0) {
            cos = 0.0;
            sin = (vecy > 0.0) ? 1.0 : -1.0;
            
        } else {
            double len = Math.sqrt(vecx * vecx + vecy * vecy);
            cos = vecx / len;
            sin = vecy / len;
            
        }
        m00 =  cos;
        m10 =  sin;
        m01 = -sin;
        m11 =  cos;
        m02 =  0.0;
        m12 =  0.0;
    }
    
    /**
     * Sets the transform to a rotation transform.
     * @param vecx x-coordinate of rotation vector.
     * @param vecy y-coordinate of rotation vector
     * @param anchorx Anchor point x-coordinate
     * @param anchory Anchor point y-coordinate
     */
    public void setToRotation(double vecx, double vecy,
                              double anchorx, double anchory)
    {
        setToRotation(vecx, vecy);
        double sin = m10;
        double oneMinusCos = 1.0 - m00;
        m02 = anchorx * oneMinusCos + anchory * sin;
        m12 = anchory * oneMinusCos - anchorx * sin;
    }
    
    /**
     * Set to the identity matrix.
     */
    public void setToIdentity() {
        m00 = m11 = 1.0;
        m10 = m01 = m02 = m12 = 0.0;
    }
    
    /**
     * Sets transform to a translation transform.
     * @param tx x-translation 
     * @param ty y-translation
     */
    public void setToTranslation(double tx, double ty) {
        m00 = 1.0;
        m10 = 0.0;
        m01 = 0.0;
        m11 = 1.0;
        m02 = tx;
        m12 = ty;
    }
    
    /**
     * Gets a rotation transform
     * @param theta Radian rotation angle.
     * @return 
     */
    public static AffineTransform getRotateInstance(double theta) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(theta);
        return Tx;
    }
    
    /**
     * Gets a rotation transform.
     * @param theta Radian rotation angle.
     * @param anchorx Anchor point x-coord.
     * @param anchory Anchor point y-coord.
     * @return 
     */
    public static AffineTransform getRotateInstance(double theta,
                                                    double anchorx,
                                                    double anchory)
    {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(theta, anchorx, anchory);
        return Tx;
    }
    
    /**
     * Sets to a rotation transform.
     * @param theta Radian rotation angle.
     * @param anchorx Anchor point x-coord.
     * @param anchory Anchor point y-coord.
     */
    public void setToRotation(double theta, double anchorx, double anchory) {
        setToRotation(theta);
        double sin = m10;
        double oneMinusCos = 1.0 - m00;
        m02 = anchorx * oneMinusCos + anchory * sin;
        m12 = anchory * oneMinusCos - anchorx * sin;
    }
    
    /**
     * Sets to a rotation transform.
     * @param theta Rotation angle in radians.
     */
    public void setToRotation(double theta) {
        double sin = Math.sin(theta);
        double cos;
        if (sin == 1.0 || sin == -1.0) {
            cos = 0.0;
        } else {
            cos = Math.cos(theta);
            if (cos == -1.0) {
                sin = 0.0;
                
            } else if (cos == 1.0) {
                sin = 0.0;
                
            }
        }
        m00 =  cos;
        m10 =  sin;
        m01 = -sin;
        m11 =  cos;
        m02 =  0.0;
        m12 =  0.0;
    }
    
    /**
     * Sets the transform to the given double coords.
     * @param m00 the X coordinate scaling element of the 3x3 matrix
     * @param m10 the Y coordinate shearing element of the 3x3 matrix
     * @param m01 the X coordinate shearing element of the 3x3 matrix
     * @param m11 the Y coordinate scaling element of the 3x3 matrix
     * @param m02 the X coordinate translation element of the 3x3 matrix
     * @param m12 the Y coordinate translation element of the 3x3 matrix
     */
    public void setTransform(double m00, double m10,
                             double m01, double m11,
                             double m02, double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        
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
        return Transform.makeAffine(m00, m10, m01, m11, m02, m12);
    }

    @Override
    public String toString() {
        return "AffineTransform{"+m00+","+m10+","+m01+","+m11+","+m02+","+m12+"}";
    }
    
    
    
    
}
