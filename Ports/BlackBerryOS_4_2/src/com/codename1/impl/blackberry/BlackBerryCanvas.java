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

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.plaf.UIManager;
import java.util.Vector;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Characters;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.container.FullScreen;

class BlackBerryCanvas extends FullScreen {
    private boolean painted;
    private Bitmap screen;
    private Graphics globalGraphics;
    BlackBerryImplementation impl;
    private int screenWidth;
    private int screenHeight;
    private Vector commands;

    static String debug = "";

    private int lastNavigationDx;
    private int lastNavigationDy;
    boolean lastNavigationCharged;

    /**
     * If a peer component is the current focused component it should receive all
     * events directly from the device 
     */
    Field eventTarget;
    private Menu m;
 
    public int getPreferredHeight() {
        return net.rim.device.api.ui.Graphics.getScreenHeight();
    }

    public int getPreferredWidth() {
        return net.rim.device.api.ui.Graphics.getScreenWidth();
    }

    protected BlackBerryCanvas(BlackBerryImplementation impl) {
        super(new Manager(NO_VERTICAL_SCROLL | NO_VERTICAL_SCROLLBAR | NO_HORIZONTAL_SCROLL | NO_HORIZONTAL_SCROLLBAR) {
            private void updateLayout(Component c, Field f) {
                int x = c.getAbsoluteX() + c.getScrollX();
                int y = c.getAbsoluteY() + c.getScrollY();
                int w = c.getWidth();
                int h = c.getHeight();
                if(w < 1 || h < 1) {
                    w = f.getPreferredWidth();
                    h = f.getPreferredHeight();
                }
                setPositionChild(f, x, y);
                layoutChild(f, w, h);
            }
            protected void sublayout(int width, int height) {
                int n = getFieldCount();
                Form currentForm = Display.getInstance().getCurrent();
                for(int iter = 0 ; iter < n ; iter++) {
                    // find peer for field
                    Field f = getField(iter);
                    if(currentForm != null) {
                        PeerComponent p = findPeer(currentForm.getContentPane(), f);
                        if(p != null) {
                            updateLayout(p, f);
                            if(p.getPreferredH() < f.getHeight() || p.getPreferredW() < f.getWidth()) {
                                // blackberry reports a false size until the actual layout occurs we need to revalidate
                                p.invalidate();
                            }
                        } else {
                            Component cmp = currentForm.getFocused();
                            // we are now editing an edit field
                            if(cmp != null && cmp instanceof TextArea && cmp.hasFocus() && f instanceof EditField) {
                                updateLayout(cmp, f);
                            } 
                        }
                    } else {
                        setPositionChild(f, 0, 0);
                        layoutChild(f, getPreferredWidth(), getPreferredHeight());
                    }
                }
                setExtent(width, height);
            }
            

            public int getPreferredHeight() {
                return net.rim.device.api.ui.Graphics.getScreenHeight();
            }

            public int getPreferredWidth() {
                return net.rim.device.api.ui.Graphics.getScreenWidth();
            }
        }, DEFAULT_MENU | NO_VERTICAL_SCROLL | NO_VERTICAL_SCROLLBAR | NO_HORIZONTAL_SCROLL | NO_HORIZONTAL_SCROLLBAR);
        this.impl = impl;

        // the alternative undeprecated API requires a signature
        screenWidth = net.rim.device.api.ui.Graphics.getScreenWidth();
        screenHeight = net.rim.device.api.ui.Graphics.getScreenHeight();
        screen = new Bitmap(Bitmap.ROWWISE_16BIT_COLOR, screenWidth, screenHeight);
        globalGraphics = new Graphics(screen);        
    }

    static PeerComponent findPeer(Container c, Field f) {
        PeerComponent p = (PeerComponent)BlackBerryImplementation.fieldComponentMap.get(f);
        if(p != null) {
            return p;
        }
        return findPeerImpl(c, f);
    }

    private static PeerComponent findPeerImpl(Container c, Field f) {
        for(int iter = 0 ; iter < c.getComponentCount() ; iter++) {
            Component current = c.getComponentAt(iter);
            if(current instanceof PeerComponent) {
                if(((PeerComponent)current).getNativePeer() == f) {
                    return (PeerComponent)current;
                }
                continue;
            }

            if(current instanceof Container) {
                PeerComponent p = findPeerImpl((Container)current, f);
                if(p != null) {
                    return p;
                }
            }
        }
        return null;
    }
    
    public void sublayout(int w, int h) {
        //super.sublayout(w, h);
        // the alternative undeprecated API requires a signature
        if(net.rim.device.api.ui.Graphics.getScreenWidth() != screenWidth ||
                net.rim.device.api.ui.Graphics.getScreenHeight() != screenHeight) {
            screenWidth = net.rim.device.api.ui.Graphics.getScreenWidth();
            screenHeight = net.rim.device.api.ui.Graphics.getScreenHeight();
            if(screenWidth > 0 && screenHeight > 0) {
                screen = new Bitmap(screenWidth, screenHeight);
                globalGraphics = new Graphics(screen);
                if(impl != null) {
                    impl.sizeChanged(screenWidth, screenHeight);
                }
            }
        }
        super.sublayout(w, h);
        
    }

    public void setNativeCommands(Vector commands) {
        this.commands = commands;
    }

    public Menu getMenu(int val) {
        if (Display.getInstance().getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE) {
            m = new Menu();
            if(commands != null){
                for (int iter = 0; iter < commands.size(); iter++) {
                    final Command cmd = (Command) commands.elementAt(iter);
                    String txt = UIManager.getInstance().localize(cmd.getCommandName(), cmd.getCommandName());
                    MenuItem i = new MenuItem(txt, iter, iter) {
                        public void run() {
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    impl.getCurrentForm().dispatchCommand(cmd, new ActionEvent(cmd));
                                }
                            });
                        }
                    };
                    m.add(i);
                }
            }
            return m;
        }
        return super.getMenu(val);
    }

    public boolean onMenu(int i) {
        if(Display.getInstance().getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE){
            return super.onMenu(i);
        }else{
            return true;
        }
    }

    
    protected void makeMenu(Menu menu, int instance) {
        if(Display.getInstance().getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE){
            menu = getMenu(instance);
            super.makeMenu(menu, instance);
        }
    }

    protected void onMenuDismissed(Menu arg0) {
        if(Display.getInstance().getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE || impl.nativeEdit != null || eventTarget != null) {
            super.onMenuDismissed(arg0);
        }
    }

    protected void paintBackground(Graphics arg0) {
    }

    public void flush(int x, int y, int w, int h, UiApplication app) {
        painted = false;
        invalidate(x, y, w, h);
        app.updateDisplay();
        long time = System.currentTimeMillis();
        while(!painted && app.isForeground() && !app.isPaintingSuspended()) {
            Thread.yield();

            // timeout on waiting to avoid freezing
            // http://forums.java.net/jive/thread.jspa?messageID=393145
            if(System.currentTimeMillis() - time > 150) {
                return;
            }
        }
    }

    /*public void subpaint(Graphics g) {
        paint(g);
    }*/

    /**
     * Clips the RIM native graphics based on the component hierarchy within LWUIT
     * so the native RIM component doesn't paint itself above other components such 
     * as the forms title.
     */
    private int clipOnLWUITBounds(Component lwuitComponent, Graphics rimGraphics) {
        int result = 0;
        Component parent = lwuitComponent;
        while (parent != null) {
            int x = parent.getAbsoluteX() + parent.getScrollX();
            int y = parent.getAbsoluteY() + parent.getScrollY();
            rimGraphics.pushRegion(x, y, parent.getWidth(), parent.getHeight(), 0, 0);
            rimGraphics.translate(-rimGraphics.getTranslateX(), -rimGraphics.getTranslateY());
            parent = parent.getParent();
            result++;
        }

        return result;
    }

    public void paint(Graphics g) {
        int f = getFieldCount();
        if(f > 0) {
            g.drawBitmap(0, 0, getWidth(), getHeight(), screen, 0, 0);

            Form currentForm = Display.getInstance().getCurrent();
            for(int iter = 0 ; iter < f ; iter++) {
                Field fld = getField(iter);
                int pops = 0;
                if(currentForm != null) {
                    PeerComponent p = findPeer(currentForm.getContentPane(), fld);
                    if(p != null) {
                        pops = clipOnLWUITBounds(p, g);
                    } else {
                        Component cmp = currentForm.getFocused();

                        // we are now editing an edit field
                        if(cmp != null && cmp instanceof TextArea && cmp.hasFocus() && fld instanceof EditField) {
                            pops = clipOnLWUITBounds(cmp, g);
                            int x = fld.getLeft();
                            int y = fld.getTop();
                            g.clear(x, y, Math.max(cmp.getWidth(), fld.getWidth()),
                                    Math.max(cmp.getHeight(), fld.getHeight()));
                        }
                    }
                }
                paintChild(g, fld);
                while(pops > 0) {
                    g.popContext();
                    pops--;
                }
            }
        } else {
            g.drawBitmap(0, 0, getWidth(), getHeight(), screen, 0, 0);
        }
        g.setColor(0);
        g.drawText(debug, 0, 0);
        painted = true;
    }
    
    public Graphics getGlobalGraphics() {
        return globalGraphics;
    }

    protected boolean navigationClick(int status, int time) {
        if(impl.nativeEdit != null || eventTarget != null) {
            if(impl.nativeEdit != null) {
                impl.finishEdit(false);
                return true;
            }
            return super.navigationClick(status, time);
        }
        impl.keyPressed(BlackBerryImplementation.GAME_KEY_CODE_FIRE);
        return true;
    }

    public void repeatLastNavigation() {
        if(lastNavigationCharged) {
            navigationMovement(lastNavigationDx, lastNavigationDy, 0, 0);
        }
    }

    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        lastNavigationDx = dx;
        lastNavigationDy = dy;
        if(impl.nativeEdit != null || eventTarget != null) {
            lastNavigationCharged = true;
            if(dy > 0) {
                if(impl.lightweightEdit.isSingleLineTextArea()) {
                    impl.finishEdit(false);
                    return true;
                }
                int pos = impl.nativeEdit.getCursorPosition();
                if(impl.lightweightEdit.getText().length() - impl.lightweightEdit.getTextAt(impl.lightweightEdit.getLines() - 1).length() > pos) {
                    impl.finishEdit(false);
                    return true;
                }
            } else {
                if(dy < 0) {
                    if(impl.lightweightEdit.isSingleLineTextArea()) {
                        impl.finishEdit(false);
                        return true;
                    }

                    int pos = impl.nativeEdit.getCursorPosition();
                    if(pos == 0 || impl.lightweightEdit.getTextAt(0).length() > pos) {
                        impl.finishEdit(false);
                        return true;
                    }
                }
            }
            return super.navigationMovement(dx, dy, status, time);
        }
        lastNavigationCharged = false;
        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) {
                impl.keyPressed(BlackBerryImplementation.GAME_KEY_CODE_RIGHT);
                impl.keyReleased(BlackBerryImplementation.GAME_KEY_CODE_RIGHT);
            } else {
                impl.keyPressed(BlackBerryImplementation.GAME_KEY_CODE_LEFT);
                impl.keyReleased(BlackBerryImplementation.GAME_KEY_CODE_LEFT);
            }
        } else {
            if (dy > 0) {
                impl.keyPressed(BlackBerryImplementation.GAME_KEY_CODE_DOWN);
                impl.keyReleased(BlackBerryImplementation.GAME_KEY_CODE_DOWN);
            } else {
                impl.keyPressed(BlackBerryImplementation.GAME_KEY_CODE_UP);
                impl.keyReleased(BlackBerryImplementation.GAME_KEY_CODE_UP);
            }
        }
        return true;
    }

    protected boolean navigationUnclick(int status, int time) {
        if(impl.nativeEdit != null || eventTarget != null) {
            return super.navigationUnclick(status, time);
        }
        impl.keyReleased(BlackBerryImplementation.GAME_KEY_CODE_FIRE);
        return true;
    }

    protected boolean keyControl(char c, int status, int time) {
        if(c == Characters.CONTROL_VOLUME_UP || c == Characters.CONTROL_VOLUME_DOWN) {
            int i = MMAPIPlayer.getGlobalVolume();
            if(i == -1) {
                i = 70;
            }
            if(c == Characters.CONTROL_VOLUME_UP) {
                MMAPIPlayer.setGlobalVolume(Math.min(100, i + 4));
            } else {
                MMAPIPlayer.setGlobalVolume(Math.max(0, i - 4));
            }
            if(BlackBerryImplementation.getVolumeListener() != null) {
                BlackBerryImplementation.getVolumeListener().fireActionEvent(new ActionEvent(this, c));
                return true;
            }
        }
        return super.keyControl(c, status, time);
    }

    public boolean isTouchDevice() {
        return false;
    }

    protected boolean keyStatus(int status, int time) {
        if(impl.nativeEdit != null || eventTarget != null) {
            return super.keyStatus(status, time);
        }
        return true;
    }

    protected boolean keyDown(int keyCode, int time) {
        if(Keypad.key(keyCode) == Keypad.KEY_ESCAPE) {
            if(isTouchDevice() && Display.getInstance().getDefaultVirtualKeyboard() != null){
                if(Display.getInstance().getDefaultVirtualKeyboard().isVirtualKeyboardShowing()){
                    Display.getInstance().getDefaultVirtualKeyboard().showKeyboard(false);
                }
            }

            // let the native editing code handle the escape
            if(impl.nativeEdit != null) {
                impl.finishEdit(false);
                return true;
            }
            // prevent the browser  component from "stealing" the escape key
            impl.keyPressed(Keypad.KEY_ESCAPE);
            return true;
        }
        if(Keypad.key(keyCode) == Keypad.KEY_END) {
            if(BlackBerryImplementation.isMinimizeOnEnd()) {
                Display.getInstance().minimizeApplication();
                return true;
            }
            System.exit(0);
            return true;
        }
        return super.keyDown(keyCode, time);
    }

    protected boolean keyChar(char c, int status, int time) {
        if(impl.nativeEdit != null || eventTarget != null) {
            return super.keyChar(c, status, time);
        }
        
        // HW_LAYOUT_ITUT: This means a standard phone keypad where the 
        // blackberry sends letters instead of numbers...
        if(Keypad.getHardwareLayout() != 1230263636) {
            impl.keyPressed(c);
            impl.keyReleased(c);
        } else {
            int code = Keypad.getAltedChar(c);
            //BlackBerryCanvas.debug = "K " + code + " c " + ((char)code) + " s " + status;

            switch(code) {
                case 0xa:
                    impl.keyPressed('#');
                    impl.keyReleased('#');
                    break;
                // * is also the backspace key
                //case 0x8:
                //    impl.keyPressed('*');
                //    impl.keyReleased('*');
                //    break;
                default:
                    impl.keyPressed(code);
                    impl.keyReleased(code);
                    break;
            }
        }
        return true;
    }

    protected boolean keyRepeat(int arg0, int arg1) {
        return super.keyRepeat(arg0, arg1);
    }

    protected boolean keyUp(int keyCode, int time) {
        if(Keypad.key(keyCode) == Keypad.KEY_ESCAPE) {
            // prevent the browser  component from "stealing" the escape key
            impl.keyReleased(Keypad.KEY_ESCAPE);
            return true;
        }
        if (Keypad.key(keyCode) == Keypad.KEY_MENU && Display.getInstance().getCommandBehavior() != Display.COMMAND_BEHAVIOR_NATIVE) {
            if(impl.nativeEdit == null || eventTarget == null) {
                impl.keyPressed(BlackBerryImplementation.MENU_SOFT_KEY);
                impl.keyReleased(BlackBerryImplementation.MENU_SOFT_KEY);
                return true;
            }
        }
        return super.keyUp(keyCode, time);
    }

    public void setShowVirtualKeyboard(boolean show) {
    }
    
    public boolean isVirtualKeyboardShowingSupported() {
        return false;
    }
    
    public boolean isMultiTouch() {
        return false;
    }
    
    public boolean isClickTouchScreen() {
        return false;
    }

    public boolean onClose() {
        System.exit(0);
        return true;
    }

    public void updateRIMLayout() {
        super.updateLayout();
    }
    
    protected BlackBerryImplementation getImplementation(){
        return impl;
    }

    protected void onDisplay() {
        Display.getInstance().showNotify();
    }

    protected void onUndisplay() {
        Display.getInstance().hideNotify();
    }

    protected void onExposed() {
        EventLog.getInstance().logAlwaysEvent("onExposed");
        if(impl.captureCallback != null){
            impl.captureCallback.fireActionEvent(null);
            impl.captureCallback = null;
        }
    }

    protected void onObscured() {
        EventLog.getInstance().logAlwaysEvent("onObscured");
    }
    
    
}
