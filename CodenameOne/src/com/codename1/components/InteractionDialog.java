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

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/**
 * <p>Unlike a regular dialog the interaction dialog only looks like a dialog,
 * it resides in the layered pane and can be used to implement features where 
 * interaction with the background form is still required.<br>
 * Since this code is designed for interaction all "dialogs" created thru here are
 * modless and never block.</p>
 * 
 * <script src="https://gist.github.com/codenameone/d1db2033981c835fb925.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-interaction-dialog.png" alt="InteractionDialog Sample" />
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
     * {@inheritDoc}
     */
    public void setScrollable(boolean scrollable) {
        getContentPane().setScrollable(scrollable);
    }
    
    /**
     * {@inheritDoc}
     */
    public Layout getLayout() {
        return contentPane.getLayout();
    }

    /**
     * {@inheritDoc}
     */
    public String getTitle() {
        return title.getText();
    }

    /**
     * {@inheritDoc}
     */
    public void addComponent(Component cmp) {
        contentPane.addComponent(cmp);
    }

    /**
     * {@inheritDoc}
     */
    public void addComponent(Object constraints, Component cmp) {
        contentPane.addComponent(constraints, cmp);
    }

    /**
     * {@inheritDoc}
     */
    public void addComponent(int index, Object constraints, Component cmp) {
        contentPane.addComponent(index, constraints, cmp);
    }

    /**
     * {@inheritDoc}
     */
    public void addComponent(int index, Component cmp) {
        contentPane.addComponent(index, cmp);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        contentPane.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public void removeComponent(Component cmp) {
        contentPane.removeComponent(cmp);
    }


    /**
     * {@inheritDoc}
     */
    public Label getTitleComponent() {
        return title;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setLayout(Layout layout) {
        contentPane.setLayout(layout);
    }
    
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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

    /**
     * A popup dialog is shown with the context of a component and  its selection, it is disposed seamlessly if the back button is pressed
     * or if the user touches outside its bounds. It can optionally provide an arrow in the theme to point at the context component. The popup
     * dialog has the PopupDialog style by default.
     *
     * @param c the context component which is used to position the dialog and can also be pointed at
     */
    public void showPopupDialog(Component c) {
        Rectangle componentPos = c.getSelectedRect();
        componentPos.setX(componentPos.getX() - c.getScrollX());
        componentPos.setY(componentPos.getY() - c.getScrollY());
        
        showPopupDialog(componentPos);
    }
    
    /**
     * A popup dialog is shown with the context of a component and  its selection, it is disposed seamlessly if the back button is pressed
     * or if the user touches outside its bounds. It can optionally provide an arrow in the theme to point at the context component. The popup
     * dialog has the PopupDialog style by default.
     *
     * @param rect the screen rectangle to which the popup should point
     */
    public void showPopupDialog(Rectangle rect) {
        if(getUIID().equals("Dialog")) {
            setUIID("PopupDialog");
            if(getTitleComponent().getUIID().equals("DialogTitle")) {
                getTitleComponent().setUIID("PopupDialogTitle");
            }
            getContentPane().setUIID("PopupContentPane");
        }

        Component contentPane = getContentPane();
        Label title = getTitleComponent();

        UIManager manager = getUIManager();
        
        String dialogTitle = title.getText();

        // hide the title if no text is there to allow the styles of the dialog title to disappear, we need this code here since otherwise the
        // preferred size logic of the dialog won't work with large title borders
        if((dialogTitle != null || dialogTitle.length() == 0) && manager.isThemeConstant("hideEmptyTitleBool", false)) {
            boolean b = getTitle().length() > 0;
            titleArea.setVisible(b);
            getTitleComponent().setVisible(b);
            if(!b && manager.isThemeConstant("shrinkPopupTitleBool", true)) {
                getTitleComponent().setPreferredSize(new Dimension(0,0));
                getTitleComponent().getStyle().setBorder(null);
                titleArea.setPreferredSize(new Dimension(0,0));
                if(getContentPane().getClientProperty("$ENLARGED_POP") == null) {
                    getContentPane().putClientProperty("$ENLARGED_POP", Boolean.TRUE);
                    int cpPaddingTop = getContentPane().getStyle().getPadding(TOP);
                    int titlePT = getTitleComponent().getStyle().getPadding(TOP);
                    byte[] pu = getContentPane().getStyle().getPaddingUnit();
                    if(pu == null){
                        pu = new byte[4]; 
                   }
                    pu[0] = Style.UNIT_TYPE_PIXELS;
                    getContentPane().getStyle().setPaddingUnit(pu);
                    int pop = Display.getInstance().convertToPixels(manager.getThemeConstant("popupNoTitleAddPaddingInt", 1), false);
                    getContentPane().getStyle().setPadding(TOP, pop + cpPaddingTop + titlePT);
                }
            }
        }

        // allows a text area to recalculate its preferred size if embedded within a dialog
        revalidate();

        Style contentPaneStyle = getStyle();

        boolean restoreArrow = false;
        if(manager.isThemeConstant(getUIID()+ "ArrowBool", false)) {
            Image t = manager.getThemeImageConstant(getUIID() + "ArrowTopImage");
            Image b = manager.getThemeImageConstant(getUIID() + "ArrowBottomImage");
            Image l = manager.getThemeImageConstant(getUIID() + "ArrowLeftImage");
            Image r = manager.getThemeImageConstant(getUIID() + "ArrowRightImage");
            Border border = contentPaneStyle.getBorder();
            if(border != null) {
                border.setImageBorderSpecialTile(t, b, l, r, rect);
                restoreArrow = true;
            }
        }
        int prefHeight = getPreferredH();
        int prefWidth = getPreferredW();
        if(contentPaneStyle.getBorder() != null) {
            prefWidth = Math.max(contentPaneStyle.getBorder().getMinimumWidth(), prefWidth);
            prefHeight = Math.max(contentPaneStyle.getBorder().getMinimumHeight(), prefHeight);
        }
        
        Form f = Display.getInstance().getCurrent();
        int availableHeight = f.getLayeredPane().getHeight();
        int availableWidth = f.getLayeredPane().getWidth();
        int width = Math.min(availableWidth, prefWidth);
        int x = 0;
        int y = 0;

        boolean showPortrait = Display.getInstance().isPortrait();

        // if we don't have enough space then disregard device orientation
        if(showPortrait) {
            if(availableHeight < (availableWidth - rect.getWidth()) / 2) {
                showPortrait = false;
            }
        } else {
            if(availableHeight / 2 > availableWidth - rect.getWidth()) {
                showPortrait = true;
            }
        }
        if(showPortrait) {
            if(width < availableWidth) {
                int idealX = rect.getX() - width / 2 + rect.getSize().getWidth() / 2;

                // if the ideal position is less than 0 just use 0
                if(idealX > 0) {
                    // if the idealX is too far to the right just align to the right
                    if(idealX + width > availableWidth) {
                        x = availableWidth - width;
                    } else {
                        x = idealX;
                    }
                }
            }
            if(rect.getY() < availableHeight / 2) {
                // popup downwards
                y = rect.getY();
                int height = Math.min(prefHeight, availableHeight - y);
                show(y, Math.max(0, availableHeight - height - y), x, Math.max(0, availableWidth - width - x));
            } else {
                // popup upwards
                int height = Math.min(prefHeight, availableHeight - (availableHeight - rect.getY()));
                y = rect.getY() + rect.getHeight() + - height;
                show(y, Math.max(0, availableHeight - height - y), x, Math.max(0, availableWidth - width - x));
            }
        } else {
            int height = Math.min(prefHeight, availableHeight);
            if(height < availableHeight) {
                int idealY = rect.getY() - height / 2 + rect.getSize().getHeight() / 2;

                // if the ideal position is less than 0 just use 0
                if(idealY > 0) {
                    // if the idealY is too far up just align to the top
                    if(idealY + height > availableHeight) {
                        y = availableHeight - height;
                    } else {
                        y = idealY;
                    }
                }
            }
            
            
            if(prefWidth > rect.getX()) {
                // popup right
                x = rect.getX() + rect.getSize().getWidth();
                if(x + prefWidth > availableWidth){
                    x = availableWidth - prefWidth;
                }
                
                width = Math.min(prefWidth, availableWidth - x);
                show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x));
            } else {
                // popup left
                width = Math.min(prefWidth, availableWidth - (availableWidth - rect.getX()));
                x = rect.getX() - width;
                show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x));
            }
        }

        /*if(restoreArrow) {
            contentPaneStyle.getBorder().clearImageBorderSpecialTile();
        }*/
    }

    /**
     * Simple setter to set the Dialog uiid
     *
     * @param uiid the id for the dialog
     */
    public void setDialogUIID(String uiid){
        getContentPane().setUIID(uiid);
    }

    /**
     * Returns the uiid of the dialog
     *
     * @return the uiid of the dialog
     */
    public String getDialogUIID(){
        return getContentPane().getUIID();
    }

    /**
     * Simple getter to get the Dialog Style
     * 
     * @return the style of the dialog
     */
    public Style getDialogStyle(){
        return getContentPane().getStyle();
    }
}
