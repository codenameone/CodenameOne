/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
