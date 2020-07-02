/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cef.browser;

import java.awt.image.BufferedImage;

/**
 *
 * @author shannah
 */
public interface PixelBuffer {

    public BufferedImage getBufferedImage();

    public void modifyBuffer(Runnable runnable);

    public void setBufferedImage(BufferedImage fImg);

    public void repaint();

    public void repaint(int x, int y, int width, int height);
    
}
