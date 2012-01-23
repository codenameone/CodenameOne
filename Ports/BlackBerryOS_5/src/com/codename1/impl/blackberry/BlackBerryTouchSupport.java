/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.blackberry;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.TextArea;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Touchscreen;
import net.rim.device.api.ui.TouchEvent;

/**
 * Implements touch functionality related code for compatibility with older BB models
 * 
 * @author Shai Almog, Thorsten Schemm
 */
class BlackBerryTouchSupport extends BlackBerryCanvas {
    private boolean clicked;
    private boolean drawHover;
    private int hoverX;
    private int hoverY;

    public BlackBerryTouchSupport(BlackBerryImplementation impl) {
        super(impl);
        BlackBerryVirtualKeyboard vkb = new BlackBerryVirtualKeyboard(this);
        Display.getInstance().registerVirtualKeyboard(vkb);
        Display.getInstance().setDefaultVirtualKeyboard(vkb);
    }

    protected boolean touchEvent(TouchEvent e) {
        super.touchEvent(e);
        lastNavigationCharged = false;
        int x1 = e.getGlobalX(1);
        int y1 = e.getGlobalY(1);
        int x2 = e.getGlobalX(2);
        int y2 = e.getGlobalY(2);
        
        int evt = e.getEvent();

        Form f = Display.getInstance().getCurrent();
        if(f != null) {
            Component cmp = f.getComponentAt(x1, y1);
            if(cmp != null) {
                
                if(cmp instanceof PeerComponent) {
                    return false;
                }
                if(cmp instanceof TextArea && cmp.hasFocus() && impl.nativeEdit != null) {                    
                    return false;
                }
                if(impl.nativeEdit != null){
                    impl.finishEdit(true);
                }
            }
        }
        if(!isClickTouchScreen()){
            if(evt == TouchEvent.DOWN){
                evt = TouchEvent.CLICK;
            }else if(evt == TouchEvent.UP){
                evt = TouchEvent.UNCLICK;
            }
        }
        
        switch (evt) {
            case TouchEvent.UNCLICK:
                clicked = false;
                drawHover = true;
                if(x1 < 0) {
                    impl.pointerReleased(x2, y2);
                } else {
                    if(x2 < 0) {
                        impl.pointerReleased(x1, y1);
                    } else {
                        impl.pointerReleased(new int[]{x1, x2}, new int[]{y1, y2});
                    }
                }
                break;
            case TouchEvent.CLICK:
                clicked = true;
                drawHover = false;
                if(x1 < 0) {
                    impl.pointerPressed(x2, y2);
                } else {
                    if(x2 < 0) {
                        impl.pointerPressed(x1, y1);
                    } else {
                        impl.pointerPressed(new int[]{x1, x2}, new int[]{y1, y2});
                    }
                }
                break;
            case TouchEvent.MOVE:
                if(clicked) {
                    drawHover = false;
                    if(x1 < 0) {
                        impl.pointerDragged(x2, y2);
                    } else {
                        if(x2 < 0) {
                            impl.pointerDragged(x1, y1);
                        } else {
                            impl.pointerDragged(new int[]{x1, x2}, new int[]{y1, y2});
                        }
                    }
                } else {
                    drawHover = true;
                    if(x1 < 0) {
                        impl.pointerHover(x2, y2);
                    } else {
                        if(x2 < 0) {
                            impl.pointerHover(x1, y1);
                        } else {
                            impl.pointerHover(new int[]{x1, x2}, new int[]{y1, y2});
                        }
                    }
                }
                break;
            case TouchEvent.DOWN:
                if(!clicked) {
                    drawHover = true;
                    if(x1 < 0) {
                        impl.pointerHoverPressed(x2, y2);
                    } else {
                        if(x2 < 0) {
                            impl.pointerHoverPressed(x1, y1);
                        } else {
                            impl.pointerHoverPressed(new int[]{x1, x2}, new int[]{y1, y2});
                        }
                    }
                }
                break;
            case TouchEvent.UP:
                if(!clicked) {
                    drawHover = false;
                    if(x1 < 0) {
                        impl.pointerHoverReleased(x2, y2);
                    } else {
                        if(x2 < 0) {
                            impl.pointerHoverReleased(x1, y1);
                        } else {
                            impl.pointerHoverReleased(new int[]{x1, x2}, new int[]{y1, y2});
                        }
                    }
                }
                break;
        }
        hoverY = y1;
        hoverX = x1;

        return true;
    }

    public boolean isTouchDevice() {
        return Touchscreen.isSupported();
    }
        
    public boolean isMultiTouch(){
        return this.isTouchDevice();
        
    }

    /**
     * Currently we assume the 'click screen' is relevant to platform 5 and 4
     * @return
     */
    public boolean isClickTouchScreen(){

        String s = DeviceInfo.getSoftwareVersion();
        if(s.length() > 0){
            int v = Integer.parseInt(s.substring(0, 1));        
            return v <= 5;
        }
        return false;
    }

    public void paint(Graphics g) {
        super.paint(g);
        /*if(drawHover) {
            g.setColor(0xff);
            g.setGlobalAlpha(100);
            g.fillEllipse(hoverX, hoverY, hoverX + 20, hoverY, hoverX, hoverY + 20, 0, 360);
            g.setColor(0);
            g.setGlobalAlpha(255);
        }*/
    }

}
