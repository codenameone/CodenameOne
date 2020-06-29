/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import com.codename1.impl.javase.JavaSEPort.Peer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/**
 *
 * @author shannah
 */
public class PeerComponentBuffer {
    private BufferedImage bufferedImage_;
    private Object lock = new Object();
    private Peer peer;
    
    public void paint(Graphics2D g, ImageObserver obs) {
        BufferedImage img;
        synchronized (lock) {
            img = bufferedImage_;
        
            if (img != null) {
                g.drawImage(img, 0, 0, obs);
            }
        }
    }
    
    
    public void setBufferedImage(BufferedImage img) {
        synchronized(lock) {
            bufferedImage_ = img;
        }
    }
    
    public BufferedImage getBufferedImage() {
        synchronized(lock) {
            return bufferedImage_;
        }
    }
    
    public void repaint() {
        if (peer != null) {
            peer.repaint();
        }
    }
    
    public void repaint(int x, int y, int w, int h) {
        if (peer != null) {
            peer.repaint(peer.getAbsoluteX()+x, peer.getAbsoluteY()+y, w+1, h+1);
            //peer.repaint(x, y, w, h);
        }
    }
    
    public void setPeer(Peer peer) {
        this.peer = peer;
    }
    
    public void modifyBuffer(Runnable r) {
        synchronized(lock) {
            r.run();
        }
    }
}
