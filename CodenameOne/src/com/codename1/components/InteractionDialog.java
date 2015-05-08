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

package com.codename1.components;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/**
 * Unlike a regular dialog the interaction dialog only looks like a dialog,
 * it resides in the layered pane and can be used to implement features where 
 * interaction with the background form is still required.<br />
 * Since this code is designed for interaction all "dialogs" created thru here are
 * modless and never block.
 *
 * @author Shai Almog
 */
public class InteractionDialog extends Container {
    private final Label title = new Label();
    private final Container titleArea = new Container(new BorderLayout());
    private final Container contentPane = new Container();
    private boolean animateShow = true;
    
    /**
     * Default constructor with no title
     */
    public InteractionDialog() {
        super(new BorderLayout());
        init();
    }

    /**
     * Constructor with dialog title
     * 
     * @param title the title of the dialog
     */
    public InteractionDialog(String title) {
        super(new BorderLayout());
        this.title.setText(title);
        init();
    }
    
    private void init() {
        setUIID("Dialog");
        title.setUIID("DialogTitle");
        contentPane.setUIID("DialogContentPane");
        super.addComponent(BorderLayout.NORTH, titleArea);
        titleArea.addComponent(BorderLayout.CENTER, title);
        super.addComponent(BorderLayout.CENTER, contentPane);
        setGrabsPointerEvents(true);
    }

    /**
     * Returns the body of the interaction dialog
     * @return the container where the elements of the interaction dialog are added.
     */
    public Container getContentPane() {
        return contentPane;
    }
    
    /**
     * @inheritDoc
     */
    public void setScrollable(boolean scrollable) {
        getContentPane().setScrollable(scrollable);
    }
    
    /**
     * @inheritDoc
     */
    public Layout getLayout() {
        return contentPane.getLayout();
    }

    /**
     * @inheritDoc
     */
    public String getTitle() {
        return title.getText();
    }

    /**
     * @inheritDoc
     */
    public void addComponent(Component cmp) {
        contentPane.addComponent(cmp);
    }

    /**
     * @inheritDoc
     */
    public void addComponent(Object constraints, Component cmp) {
        contentPane.addComponent(constraints, cmp);
    }

    /**
     * @inheritDoc
     */
    public void addComponent(int index, Object constraints, Component cmp) {
        contentPane.addComponent(index, constraints, cmp);
    }

    /**
     * @inheritDoc
     */
    public void addComponent(int index, Component cmp) {
        contentPane.addComponent(index, cmp);
    }

    /**
     * @inheritDoc
     */
    public void removeAll() {
        contentPane.removeAll();
    }

    /**
     * @inheritDoc
     */
    public void removeComponent(Component cmp) {
        contentPane.removeComponent(cmp);
    }


    /**
     * @inheritDoc
     */
    public Label getTitleComponent() {
        return title;
    }
    
    /**
     * @inheritDoc
     */
    public void setLayout(Layout layout) {
        contentPane.setLayout(layout);
    }
    
    /**
     * @inheritDoc
     */
    public void setTitle(String title) {
        this.title.setText(title);
    }
    
    /**
     * This method shows the form as a modal alert allowing us to produce a behavior
     * of an alert/dialog box. This method will block the calling thread even if the
     * calling thread is the EDT. Notice that this method will not release the block
     * until dispose is called even if show() from another form is called!
     * <p>Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
     * dialogs where you would want the underlying form to "peek" from behind the 
     * form. 
     * 
     * @param top space in pixels between the top of the screen and the form
     * @param bottom space in pixels between the bottom of the screen and the form
     * @param left space in pixels between the left of the screen and the form
     * @param right space in pixels between the right of the screen and the form
     */
    public void show(int top, int bottom, int left, int right) {
        Form f = Display.getInstance().getCurrent();
        f.getLayeredPane().setLayout(new BorderLayout());
        getUnselectedStyle().setMargin(TOP, top);
        getUnselectedStyle().setMargin(BOTTOM, bottom);
        getUnselectedStyle().setMargin(LEFT, left);
        getUnselectedStyle().setMargin(RIGHT, right);
        getUnselectedStyle().setMarginUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});
        f.getLayeredPane().addComponent(BorderLayout.CENTER, this);
        if(animateShow) {
            int x = left + (f.getWidth() - right - left) / 2;
            int y = top + (f.getHeight() - bottom - top) / 2;
            setX(x);
            setY(y);
            setWidth(1);
            setHeight(1);
            f.getLayeredPane().animateLayout(400);
        } else {
            f.getLayeredPane().revalidate();
        }
    }
    
    
    /**
     * @inheritDoc
     */
    public void dispose() {
        Container p = getParent();
        if(p != null) {
            if(animateShow) {
                setX(getX() + getWidth() / 2);
                setY(getY() + getHeight()/ 2);
                setWidth(1);
                setHeight(1);
                p.animateUnlayoutAndWait(400, 100);
            }
            p.removeComponent(this);
            p.revalidate();
        }
    }

    /**
     * Will return true if the dialog is currently showing
     * @return true if showing
     */
    public boolean isShowing() {
        return getParent() != null;
    }
    
    /**
     * Indicates whether show/dispose should be animated or not
     * @return the animateShow 
     */
    public boolean isAnimateShow() {
        return animateShow;
    }

    /**
     * Indicates whether show/dispose should be animated or not
     * @param animateShow the animateShow to set
     */
    public void setAnimateShow(boolean animateShow) {
        this.animateShow = animateShow;
    }
}
