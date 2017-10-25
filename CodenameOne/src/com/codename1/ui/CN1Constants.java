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

/**
 * Common constants for Display and CN
 *
 * @author Shai Almog
 */
public class CN1Constants {
    /**
     * Very Low Density 176x220 And Smaller
     */
    public static final int DENSITY_VERY_LOW = 10;

    /**
     * Low Density Up To 240x320
     */
    public static final int DENSITY_LOW = 20;

    /**
     * Medium Density Up To 360x480
     */
    public static final int DENSITY_MEDIUM = 30;

    /**
     * Hi Density Up To 480x854
     */
    public static final int DENSITY_HIGH = 40;

    /**
     * Very Hi Density Up To 1440x720
     */
    public static final int DENSITY_VERY_HIGH = 50;

    /**
     * HD Up To 1920x1080
     */
    public static final int DENSITY_HD = 60;

    /**
     * Intermediate density for screens that sit somewhere between HD to 2HD
     */
    public static final int DENSITY_560 = 65;
    
    /**
     * Double the HD level density
     */
    public static final int DENSITY_2HD = 70;

    /**
     * 4K level density 
     */
    public static final int DENSITY_4K = 80;


    /**
     * Date native picker type, it returns a java.util.Date result.
     */
    public static final int PICKER_TYPE_DATE = 1;

    /**
     * Time native picker type, it returns an integer with minutes since midnight.
     */
    public static final int PICKER_TYPE_TIME = 2;

    /**
     * Date and time native picker type, it returns a java.util.Date result.
     */
    public static final int PICKER_TYPE_DATE_AND_TIME = 3;

    /**
     * Strings native picker type, it returns a String result and accepts a String array.
     */
    public static final int PICKER_TYPE_STRINGS = 4;
    
    /**
     * Duration picker type.  It returns Long result (milliseconds).
     */
    public static final int PICKER_TYPE_DURATION = 5;
    
    public static final int PICKER_TYPE_DURATION_HOURS = 6;
    
    public static final int PICKER_TYPE_DURATION_MINUTES = 7;

    /**
     * Used by getSMSSupport to indicate that SMS is not supported
     */
    public static final int SMS_NOT_SUPPORTED = 1;
    
    /**
     * Used by getSMSSupport to indicate that SMS is sent in the background without a compose UI
     */
    public static final int SMS_SEAMLESS = 2;
    
    /**
     * Used by getSMSSupport to indicate that SMS triggers the native SMS app which will show a compose UI
     */
    public static final int SMS_INTERACTIVE = 3;
    
    /**
     * Used by getSMSSupport to indicate that SMS can be sent in either seamless or interactive mode
     */
    public static final int SMS_BOTH = 4;
    
    /**
     * Used by openGallery 
     */
    public static final int GALLERY_IMAGE = 0;
    
    /**
     * Used by openGallery 
     */
    public static final int GALLERY_VIDEO = 1;

    /**
     * Used by openGallery 
     */
    public static final int GALLERY_ALL = 2;
}
