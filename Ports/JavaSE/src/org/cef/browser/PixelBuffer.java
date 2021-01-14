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
package org.cef.browser;

import java.awt.image.BufferedImage;

/**
 * An abstraction of a pixel buffer that the CEF browser can paint to. 
 * @author shannah
 */
public interface PixelBuffer {

    /**
     * Gets the buffered image that is being written to.
     * @return 
     */
    public BufferedImage getBufferedImage();

    /**
     * Runs the given runnable in a pipeline where it is safe to modify the buffer.  All code that
     * modifies the buffer should run in this channel.
     * @param runnable 
     */
    public void modifyBuffer(Runnable runnable);

    /**
     * Sets the buffered image that is to be written to.
     * @param fImg 
     */
    public void setBufferedImage(BufferedImage fImg);

    /**
     * Repaints the pixel buffer
     */
    public void repaint();

    /**
     * Repaints rectangle in the pixel buffer.
     * @param x
     * @param y
     * @param width
     * @param height 
     */
    public void repaint(int x, int y, int width, int height);
    
}
