/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.impl.javase;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class SVG {

    private byte[] svgData;
    private String baseURL;
    private BufferedImage img;
    private float ratioW = 1.0f;
    private float ratioH = 1.0f;
    private int[] dpis;
    private int[] widthForDPI;
    private int[] heightForDPI;

    SVG() {}


    /**
     * @return the svgData
     */
    public byte[] getSvgData() {
        return svgData;
    }

    /**
     * @param svgData the svgData to set
     */
    void setSvgData(byte[] svgData) {
        this.svgData = svgData;
    }

    /**
     * @return the baseURL
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * @param baseURL the baseURL to set
     */
    void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * @return the img
     */
    public BufferedImage getImg() {
        return img;
    }

    /**
     * @param img the img to set
     */
    void setImg(BufferedImage img) {
        this.img = img;
    }

    /**
     * @return the ratioW
     */
    public float getRatioW() {
        return ratioW;
    }

    /**
     * @param ratioW the ratioW to set
     */
    public void setRatioW(float ratioW) {
        this.ratioW = ratioW;
    }

    /**
     * @return the ratioH
     */
    public float getRatioH() {
        return ratioH;
    }

    /**
     * @param ratioH the ratioH to set
     */
    public void setRatioH(float ratioH) {
        this.ratioH = ratioH;
    }

    /**
     * @return the dpis
     */
    public int[] getDpis() {
        return dpis;
    }

    /**
     * @param dpis the dpis to set
     */
    public void setDpis(int[] dpis) {
        this.dpis = dpis;
    }

    /**
     * @return the widthForDPI
     */
    public int[] getWidthForDPI() {
        return widthForDPI;
    }

    /**
     * @param widthForDPI the widthForDPI to set
     */
    public void setWidthForDPI(int[] widthForDPI) {
        this.widthForDPI = widthForDPI;
    }

    /**
     * @return the heightForDPI
     */
    public int[] getHeightForDPI() {
        return heightForDPI;
    }

    /**
     * @param heightForDPI the heightForDPI to set
     */
    public void setHeightForDPI(int[] heightForDPI) {
        this.heightForDPI = heightForDPI;
    }
}
