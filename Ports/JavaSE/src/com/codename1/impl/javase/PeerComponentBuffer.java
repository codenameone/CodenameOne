/*
 * Copyright (c) 2020, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import com.codename1.impl.javase.JavaSEPort.Peer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.lang.ref.WeakReference;

public class PeerComponentBuffer {
    private BufferedImage bufferedImage_;
    private Object lock = new Object();
    private WeakReference<Peer> peerRef;
    
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
        Peer peer = peerRef.get();
        if (peer != null) {
            peer.repaint();
        }
    }
    
    public void repaint(int x, int y, int w, int h) {
        Peer peer = peerRef.get();
        if (peer != null) {
            double scale = JavaSEPort.instance.zoomLevel;
            peer.repaint((int)(peer.getAbsoluteX()+x/scale), (int)(peer.getAbsoluteY()+y/scale), (int)((w+1)/scale), (int)((h+1)/scale));
            //peer.repaint(x, y, w, h);
        }
    }
    
    public void setPeer(Peer peer) {
        this.peerRef = new WeakReference<Peer>(peer);
    }
    
    public void modifyBuffer(Runnable r) {
        synchronized(lock) {
            r.run();
        }
    }
}
