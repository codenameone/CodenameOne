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
package com.codename1.designer.css;

import com.codename1.io.Log;
import com.codename1.processing.Result;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Image;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author shannah
 */
public class WebviewSnapshotter {
    private int x, y, w, h;
    private BrowserComponent web;
    final LinkedList<Runnable> eventQueue = new LinkedList<Runnable>();
    Thread eventThread;
    Runnable onDone;
    boolean done;
    BufferedImage image;
    Graphics2D imageGraphics;
    
    
    public WebviewSnapshotter(BrowserComponent web) {
        this.web = web;
    }
    
    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y =y;
        this.w = w;
        this.h = h;
    }
    
    private void fireJSSnap(int x, int y, int w, int h) {
        //System.out.println("[WebviewSnapshotter::fireJSSnap("+x+","+y+","+w+","+h);
        if (!CN.isEdt()) {
            CN.callSerially(()-> {
                fireJSSnap(x, y, w, h);
            });
            return;
        } 
        
        web.execute("window.snapper = window.snapper || {}; "
                + "window.snapper.handleJSSnap = function(scrollX, scrollY, x, y, w, h) {"
                + "  callback.onSuccess(JSON.stringify({scrollX:scrollX, scrollY:scrollY, x:x, y:y, w:w, h:h}));"
                + "}; window.getRectSnapshot("+x+","+y+","+w+","+h+");", res -> {
                    try {
                        Result data = Result.fromContent(res.toString(), Result.JSON);
                        handleJSSnap(
                                data.getAsInteger("scrollX"),
                                data.getAsInteger("scrollY"),
                                data.getAsInteger("x"),
                                data.getAsInteger("y"),
                                data.getAsInteger("w"),
                                data.getAsInteger("h")
                        );
                    } catch (Exception ex) {
                        Log.p("Failed to parse callback in fireJSSnap");
                        Log.e(ex);
                    }
                });
    }
    
    private boolean isEventThread() {
        return Thread.currentThread() == eventThread;
    }
    
    private void runLater(Runnable r) {
        synchronized(eventQueue) {
            eventQueue.add(r);
            eventQueue.notify();
        }
    }
    
    public void log(String msg) {
        System.out.println(msg);
    }
    
    public final void handleJSSnap(int scrollX, int scrollY, int x, int y, int w, int h) {
        //System.out.println("[WebviewSnapshotter::handleJSSnap("+scrollX+","+scrollY+","+x+","+y+","+w+","+h+")");
        if (!isEventThread()) {
            runLater(()-> {
                handleJSSnap(scrollX, scrollY, x, y, w, h);
            });
            return;
        }
        CN.callSerially(()-> {
            Image wi = web.captureScreenshot().get();
            BufferedImage img = (BufferedImage)wi.getImage();
            
            runLater(()-> {
                int remw = Math.min(w, 320);
                int remh = Math.min(h, 480);
                //remw = Math.min(remw, 320);
                //remh = Math.min(remh, 480);
                BufferedImage img2 = img.getSubimage(20,20, remw, remh);
                //System.out.println("Writing image at "+(x-20)+", "+(y-20)+" with width "+remw+" and height "+remh);
                imageGraphics.drawImage(img2, null, x-20, y-20);
                if (x+remw < this.x+this.w || y+remh < this.y+this.h) {
                    if (x+remw < this.x+this.w) {
                        fireJSSnap(x+remw, y, w, h);
                    } else {
                        fireJSSnap(this.x, y+remh, w, h);
                    }
                } else {
                    fireDone();
                }
            });

        });

    }
    
    public final void fireDone() {
        //System.out.println("[WebViewSnapshotter::fireDone()]");
        if (!isEventThread()) {
            runLater(()-> {
                fireDone();
            });
            return;
        } 
        //System.out.println("In fireDone()");
        imageGraphics.dispose();
        
        // Now do the fireDone
        if (onDone != null) {
            onDone.run();
        }
        done = true;
    }
    
    public void snapshot(Runnable onDone) {
        //System.out.println("[WebviewSnapshotter::snapshot()]");
        if (eventThread != null) {
            throw new RuntimeException("Snapshot event thread already created");
        }
        eventThread = new Thread(()-> {
            while (!done) {
                Runnable evt = null;
                synchronized(eventQueue) {
                    if (eventQueue.isEmpty()) {
                        try {
                            eventQueue.wait();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(WebviewSnapshotter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    evt = eventQueue.remove();
                }
                evt.run();
            }
        });
        this.onDone = onDone;
        int x = this.x;
        int y = this.y;
        int width = Math.min(this.w, 320);
        int height = Math.min(this.h, 480);
        this.image = new BufferedImage(this.w, this.h, BufferedImage.TYPE_INT_ARGB);
        this.imageGraphics = this.image.createGraphics();
        fireJSSnap(x, y, width, height);
        eventThread.start();
        
    }
    
    public void snapshotSync() {
        Object lock = new Object();
        boolean[] complete = new boolean[1];
        snapshot(()-> {
            complete[0] = true;
            synchronized(lock) {
                lock.notify();
            }
        });
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                while (!complete[0]) {
                    synchronized(lock) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(WebviewSnapshotter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        });
        
    }
    
    public BufferedImage getImage() {
        return image;
    }
}
