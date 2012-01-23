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
package com.codename1.impl.midp;

import com.codename1.ui.Display;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

/**
 * An implementation of Codename One based on game canvas, this is the default implementation
 * class for Codename One which most customizers should extend to enhance.
 *
 * @author Shai Almog
 */
public class CanvasImplementation extends GameCanvasImplementation {
    private class C extends Canvas  implements CommandListener, Runnable {
        private javax.microedition.lcdui.Image backBuffer;
        private boolean done;

        C() {
            setFullScreenMode(true);
        }
        

        public void run() {
            while (!done) {
                synchronized (getDisplayLock()) {
                    try {
                        getDisplayLock().wait(50);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        public void setDone(boolean done) {
            this.done = done;
            synchronized (getDisplayLock()) {
                getDisplayLock().notify();
            }
        }

        public void commandAction(Command c, Displayable d) {
            if (d == currentTextBox) {
                display.setCurrent(this);
                if (c == CONFIRM_COMMAND) {
                    // confirm
                    String text = currentTextBox.getString();
                    Display.getInstance().onEditingComplete(currentTextComponent, text);
                }

                currentTextBox = null;
                setDone(true);
            }
        }

        public javax.microedition.lcdui.Graphics getGraphics() {
            if(backBuffer == null || backBuffer.getWidth() != getWidth() || backBuffer.getHeight() != getHeight()) {
                backBuffer = javax.microedition.lcdui.Image.createImage(getWidth(), getHeight());
            }
            return backBuffer.getGraphics();
        }

        public void paint(javax.microedition.lcdui.Graphics g) {
            if(backBuffer != null) {
                g.drawImage(backBuffer, 0, 0, javax.microedition.lcdui.Graphics.TOP | javax.microedition.lcdui.Graphics.LEFT);
            }
        }
        
        protected void keyPressed(final int keyCode){
            CanvasImplementation.this.keyPressed(keyCode);
        }

        protected  void keyReleased(final int keyCode){
            CanvasImplementation.this.keyReleased(keyCode);
        }

        protected  void pointerDragged(final int x, final int y){
            CanvasImplementation.this.pointerDragged(x, y);
        }

        protected  void pointerPressed(final int x,final int y){
            CanvasImplementation.this.pointerPressed(x, y);
        }

        protected  void pointerReleased(final int x,final int y){
            CanvasImplementation.this.pointerReleased(x, y);
        }

        protected void sizeChanged(int w, int h){
            CanvasImplementation.this.sizeChanged(w, h);
        }
    }
    
    /**
     * @inheritDoc
     */
    protected Canvas createCanvas() {
        return new C();
    }

    /**
     * @inheritDoc
     */
    public void flushGraphics(int x, int y, int width, int height) {
        getCanvas().repaint(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void flushGraphics() {
        getCanvas().repaint();
    }
    
    /**
     * @inheritDoc
     */
    public Object getNativeGraphics() {
        return ((C)getCanvas()).getGraphics();
    }
}
