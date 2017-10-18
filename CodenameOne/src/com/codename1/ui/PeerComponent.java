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
package com.codename1.ui;

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

/**
 * A peer component is essentially a "dummy" Codename One component used to calculate the position
 * of the actual native platform specific component. The behavior of a peer component is
 * very platform specific, it is meant for platforms where a native component can be
 * integrated with a Codename One component.
 * Codename One features such as glass pane, z-ordering, dialogs &amp; menus might not work
 * as expected in all situations where peer components are involved. E.g. a peer component
 * might hide itself when a menu/dialog is shown and recreate itself when it is hidden/disposed.
 *
 * @author Shai Almog
 */
public class PeerComponent extends Component {
    private Object nativePeer;
    private Rectangle lastPos = new Rectangle(-1, -1, -1, -1);
    private Image peerImage;

    /**
     * This constructor is used by the platform implementation to create instances
     * of this class use the static create method.
     *
     * @param nativePeer the native platform specific peer component.
     */
    protected PeerComponent(Object nativePeer) {
        setGrabsPointerEvents(true);
        this.nativePeer = nativePeer;
    }

    /**
     * The peer image is drawn when the component doesn't exist or during transition
     * a placeholder image can be placed in the beginning to show something while
     * the peer is being created asynchronously. This image might be replaced by
     * an internal image representing the actual content of the peer
     * @return an image
     */
    protected Image getPeerImage() {
        if(peerImage == null) {
            peerImage = generatePeerImage();
        }
        return peerImage;
    }
    
    /**
     * The native implementation should implement this method to generate a native peer
     * image representing the component
     * @return a screenshot of the component
     */
    protected Image generatePeerImage() {
        return null;
    }
    
    /**
     * The peer image is drawn when the component doesn't exist or during transition
     * a placeholder image can be placed in the beginning to show something while
     * the peer is being created asynchronously. This image might be replaced by
     * an internal image representing the actual content of the peer
     * 
     * @param i the peer image
     */
    protected void setPeerImage(Image i) {
        peerImage = i;
    }
    
    /**
     * Subclasses should return true here if the peer image should be rendered instead of the actual peer
     * @return true to render the peer image
     */
    protected boolean shouldRenderPeerImage() {
        return false;
    }
    
    /**
     * Use this method to encapsulate a native UI object
     *
     * @param nativePeer the native platform specific peer component.
     */
    public static PeerComponent create(Object nativePeer) {
        if(nativePeer == null) {
            return null;
        }
        return Display.impl.createNativePeer(nativePeer);
    }

    /**
     * Returns the native peer instance
     *
     * @return the native peer
     */
    public Object getNativePeer() {
        return nativePeer;
    }

    void setNativePeer(Object nativePeer) {
        this.nativePeer = nativePeer;
    }

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    protected Dimension calcPreferredSize() {
        return super.calcPreferredSize();
    }

    /**
     * {@inheritDoc}
     */
    protected Dimension calcScrollSize() {
        return super.calcScrollSize();
    }

    /**
     * {@inheritDoc}
     */
    protected void deinitialize() {
        Form.activePeerCount--;
        super.deinitialize();
    }

    /**
     * {@inheritDoc}
     */
    protected void fireClicked() {
    }

    /**
     * {@inheritDoc}
     */
    protected void focusGained() {
        super.focusGained();
    }

    /**
     * {@inheritDoc}
     */
    protected void focusLost() {
        super.focusLost();
    }

    /**
     * {@inheritDoc}
     */
    public boolean handlesInput() {
        return super.handlesInput();
    }

    /**
     * {@inheritDoc}
     */
    protected void initComponent() {
        Form.activePeerCount++;
        super.initComponent();
    }

    /**
     * {@inheritDoc}
     */
    public void keyPressed(int keyCode) {
    }

    /**
     * {@inheritDoc}
     */
    public void keyReleased(int keyCode) {
    }

    /**
     * {@inheritDoc}
     */
    public void keyRepeated(int keyCode) {
    }

    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {
        onPositionSizeChangeImpl();
        if(shouldRenderPeerImage() && getWidth() > 0 && getHeight() > 0) {
            Image img = getPeerImage();
            if(img != null) {
                g.drawImage(img, getX(), getY(), getWidth(), getHeight());
            }
        } else {
            super.paint(g);
            g.drawPeerComponent(this);
            
        }
    }
    
    private void onPositionSizeChangeImpl() {
        if(isInitialized() && getWidth() > 0 && getHeight() > 0) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            int x = getAbsoluteX() + scrollX;
            int y = getAbsoluteY() + scrollY;
            if(x != lastPos.getX() || y != lastPos.getY() || getWidth() != lastPos.getSize().getWidth() || getHeight() != lastPos.getSize().getHeight()) {
                lastPos.setX(x);
                lastPos.setY(y);
                lastPos.getSize().setWidth(getWidth());
                lastPos.getSize().setHeight(getHeight());
                onPositionSizeChange();
            }        
        }
    }
    
    /**
     * {@inheritDoc}
     */
    void onParentPositionChange() {        
        onPositionSizeChangeImpl();
    }

    /**
     * {@inheritDoc}
     */
    protected void paintBackground(Graphics g) {
    }

    /**
     * {@inheritDoc}
     */
    public void paintBackgrounds(Graphics g) {
    }

    /**
     * {@inheritDoc}
     */
    protected void paintBorder(Graphics g) {
    }

    /**
     * {@inheritDoc}
     */
    protected void paintScrollbarX(Graphics g) {
    }

    /**
     * {@inheritDoc}
     */
    protected void paintScrollbarY(Graphics g) {
    }

    /**
     * {@inheritDoc}
     */
    protected void paintScrollbars(Graphics g) {
    }

    /**
     * {@inheritDoc}
     */
    public void pointerDragged(int[] x, int[] y) {
    }

    /**
     * {@inheritDoc}
     */
    public void pointerDragged(int x, int y) {
    }

    /**
     * {@inheritDoc}
     */
    public void pointerHover(int[] x, int[] y) {
    }

    /**
     * {@inheritDoc}
     */
    public void pointerHoverReleased(int[] x, int[] y) {
    }

    /**
     * {@inheritDoc}
     */
    public void pointerPressed(int[] x, int[] y) {
    }

    /**
     * {@inheritDoc}
     */
    public void pointerPressed(int x, int y) {
    }

    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int[] x, int[] y) {
    }

    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int x, int y) {
    }

    /**
     * Updates the size of the component from the native widget
     */
    public void invalidate() {
        setShouldCalcPreferredSize(true);
        getComponentForm().revalidate();
    }

    /**
     * Callback useful for sublclasses that need to track the change in size/position
     * of the component (notice that this might be invoked too many times for a single change)!
     */
    protected void onPositionSizeChange() {
    }
    
    /**
     * An optional callback for peers indicating that the peer is now rendered as part of
     * a transition or some other state and is thus not truly visible. In this case the
     * peer should either make itself invisible or render itself as an image instead of
     * natively
     * 
     * @param l true to enable lightweight mode, false to disable it
     */
    protected void setLightweightMode(boolean l) {
    }
}
