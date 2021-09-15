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

import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import static com.codename1.ui.Component.BOTTOM;
import static com.codename1.ui.Component.LEFT;
import static com.codename1.ui.Component.RIGHT;
import static com.codename1.ui.Component.TOP;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
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
    private final Container contentPane;
    private boolean animateShow = true;
    private boolean repositionAnimation = true;
    private boolean disposed;
    private boolean disposeWhenPointerOutOfBounds;
    
    /**
     * Whether the interaction dialog uses the form layered pane of the regular layered pane
     */
    private boolean formMode;
    
    /**
     * Default constructor with no title
     */
    public InteractionDialog() {
        super(new BorderLayout());
        contentPane = new Container();
        init();
    }

    /**
     * Default constructor with layout
     * @param l layout
     */
    public InteractionDialog(Layout l) {
        super(new BorderLayout());
        contentPane = new Container(l);
        init();
    }
    
    /**
     * Constructor with dialog title
     * 
     * @param title the title of the dialog
     */
    public InteractionDialog(String title) {
        super(new BorderLayout());
        contentPane = new Container();
        this.title.setText(title);
        init();
    }

    /**
     * Constructor with dialog title
     * 
     * @param title the title of the dialog
     * @param l the layout for the content pane
     */
    public InteractionDialog(String title, Layout l) {
        super(new BorderLayout());
        contentPane = new Container(l);
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

    @Override
    protected void initComponent() {
        super.initComponent();
        installPointerOutOfBoundsListeners();
    }
    
    
    
    
    
    /**
     * This flag indicates if the dialog should be disposed if a pointer 
     * released event occurred out of the dialog content.
     * 
     * @param disposeWhenPointerOutOfBounds
     */
    public void setDisposeWhenPointerOutOfBounds(boolean disposeWhenPointerOutOfBounds) {
        this.disposeWhenPointerOutOfBounds = disposeWhenPointerOutOfBounds;
    }

    /**
     * This flag indicates if the dialog should be disposed if a pointer
     * released event occurred out of the dialog content.
     *
     * @return  true if the dialog should dispose
     */
    public boolean isDisposeWhenPointerOutOfBounds() {
        return disposeWhenPointerOutOfBounds;
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
    
    private void cleanupLayer(Form f) {
        if(formMode) {
            Container c = (Container)f.getFormLayeredPane(InteractionDialog.class, true);
            c.removeAll();
            c.remove();
        }        
    }
    
    private Container getLayeredPane(Form f) {
        //return f.getLayeredPane();
        Container c;
        if(formMode) {
            c = (Container)f.getFormLayeredPane(InteractionDialog.class, true);
        } else {
            c = (Container)f.getLayeredPane(InteractionDialog.class, true);
        }
        if (!(c.getLayout() instanceof LayeredLayout)) {
            c.setLayout(new LayeredLayout());
        }
        
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deinitialize() {
        super.deinitialize();
        if(disposed) {
            Form f = getComponentForm();
            if(f != null) {
                if (pressedListener != null) {
                    f.removePointerPressedListener(pressedListener);
                }
                if (releasedListener != null) {
                    f.removePointerReleasedListener(releasedListener);
                }
                Container pp = getLayeredPane(f);
                Container p = getParent();
                remove();
                if (p.getComponentCount() == 0) {
                    p.remove();
                }
                //pp.removeAll();
                pp.revalidateLater();
                cleanupLayer(f);
            }
        }
    }
    
    public void resize(final int top, final int bottom, final int left, final int right) {
        if (!disposed) {
            final Form f = Display.getInstance().getCurrent();
            
            Style unselectedStyle = getUnselectedStyle();

            unselectedStyle.setMargin(TOP, Math.max(0, top));
            unselectedStyle.setMargin(BOTTOM, Math.max(0, bottom));
            unselectedStyle.setMargin(LEFT, Math.max(0, left));
            unselectedStyle.setMargin(RIGHT, Math.max(0, right));
            unselectedStyle.setMarginUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});

            getParent().setX(getX());
            getParent().setY(getY());
            setX(0);
            setY(0);
            getParent().setWidth(getWidth());
            getParent().setHeight(getHeight());
            
            getLayeredPane(f).animateLayout(getUIManager().getThemeConstant("interactionDialogSpeedInt", 400));
        }
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
        getUnselectedStyle().setOpacity(255);
        disposed = false;
        Form f = Display.getInstance().getCurrent();
        Style unselectedStyle = getUnselectedStyle();
        
        unselectedStyle.setMargin(TOP, top);
        unselectedStyle.setMargin(BOTTOM, bottom);
        unselectedStyle.setMargin(LEFT, left);
        unselectedStyle.setMargin(RIGHT, right);
        unselectedStyle.setMarginUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});
        
        // might occur when showing the dialog twice...
        remove();
        
        // We issue a revalidate in case this is the first time the layered pane 
        // appears in the form.  Without this, the "show" animation won't work 
        // the first time.
        getLayeredPane(f).revalidate();
        
        getLayeredPane(f).addComponent(BorderLayout.center(this));
        if(animateShow) {
            int x = left + (f.getWidth() - right - left) / 2;
            int y = top + (f.getHeight() - bottom - top) / 2;
            if(repositionAnimation) {
                getParent().setX(x);
                getParent().setY(y);
                getParent().setWidth(1);
                getParent().setHeight(1);
            } else {
                getParent().setX(getX());
                getParent().setY(getY());
                setX(0);
                setY(0);
                getParent().setWidth(getWidth());
                getParent().setHeight(getHeight());
            }
            getLayeredPane(f).animateLayout(getUIManager().getThemeConstant("interactionDialogSpeedInt", 400));
        } else {
            //getLayeredPane(f).revalidate();
            f.revalidateWithAnimationSafety();
        }
        /*
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
        */
    }
    
    
    /**
     * Removes the interaction dialog from view
     */
    public void dispose() {
        disposed = true;
        Container p = getParent();
        if(p != null) {
            Form f = p.getComponentForm();
            if(f != null) {
                if(animateShow) {
                    if(repositionAnimation) {
                        setX(getX() + getWidth() / 2);
                        setY(getY() + getHeight()/ 2);
                        setWidth(1);
                        setHeight(1);
                    }
                    p.animateUnlayoutAndWait(getUIManager().getThemeConstant("interactionDialogSpeedInt", 400), 100);
                }
                Container pp = getLayeredPane(f);
                remove();
                if (p.getComponentCount() == 0) {
                    p.remove();
                }
                //p.remove();
                //pp.removeAll();
                
                pp.revalidate();
                cleanupLayer(f);
            } else {
                p.remove();
            }
        }
    }

    /**
     * Removes the interaction dialog from view with an animation to the left
     */
    public void disposeToTheLeft() {
        disposeTo(Component.LEFT);
    }
    
    /**
     * Removes the interaction dialog from view with an animation to the bottom
     */
    public void disposeToTheBottom() {
        disposeTo(Component.BOTTOM);
    }
    
    /**
     * Removes the interaction dialog from view with an animation to the bottom
     * @param onFinish Callback called when dispose animation is complete.
     */
    public void disposeToTheBottom(Runnable onFinish) {
        disposeTo(Component.BOTTOM, onFinish);
    }
    
    /**
     * Removes the interaction dialog from view with an animation to the top
     */
    public void disposeToTheTop() {
        disposeTo(Component.TOP);
    }
    
    /**
     * Removes the interaction dialog from view with an animation to the right
     */
    public void disposeToTheRight() {
        disposeTo(Component.RIGHT);
    }
    
    
    private void disposeTo(int direction) {
        disposeTo(direction, null);
    }
    
    private void disposeTo(int direction, final Runnable onFinish) {
        disposed = true;
        final Container p = getParent();
        if(p != null) {
            final Form f = p.getComponentForm();
            if(f != null) {
                switch (direction) {
                    case Component.LEFT:
                        setX(-getWidth());
                        break;
                    case Component.TOP:
                        setY(-getHeight());
                        break;
                    case Component.RIGHT:
                        setX(Display.getInstance().getDisplayWidth());
                        break;
                    case Component.BOTTOM:
                        setY(Display.getInstance().getDisplayHeight());
                        break;
                        
                }
                
                if(animateShow) {
                    p.animateUnlayout(getUIManager().getThemeConstant("interactionDialogSpeedInt", 400), 255, new Runnable() {
                        public void run() {
                            if(p.getParent() != null) {
                                Container pp = getLayeredPane(f);
                                remove();
                                p.remove();
                                pp.removeAll();
                                pp.revalidate();
                                cleanupLayer(f);
                            } 
                            if (onFinish != null) {
                                onFinish.run();
                            }
                        }
                    });
                } else {
                    p.revalidate();
                    Container pp = getLayeredPane(f);
                    remove();
                    p.remove();
                    pp.removeAll();
                    pp.revalidate();
                    if (onFinish != null) {
                        onFinish.run();
                    }
                }
            } else {
                remove();
                if (onFinish != null) {
                    onFinish.run();
                }
            }
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

    
    
    private boolean pressedOutOfBounds;
    private ActionListener pressedListener;
    private ActionListener releasedListener;
    private void installPointerOutOfBoundsListeners() {
        
        final Form f = getComponentForm();
        if (f != null) {
            if (pressedListener == null) {
                pressedListener = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (disposed) {
                            f.removePointerPressedListener(pressedListener);
                            f.removePointerReleasedListener(releasedListener);
                            return;
                        }
                        pressedOutOfBounds = disposeWhenPointerOutOfBounds && 
                                !getContentPane().containsOrOwns(evt.getX(), evt.getY()) &&
                                !getTitleComponent().containsOrOwns(evt.getX(), evt.getY())
                                ;
                        if (pressedOutOfBounds && disposeWhenPointerOutOfBounds) {
                            evt.consume();
                        }
                    }
                };
            }
            if (releasedListener == null) {
                releasedListener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (disposed) {
                            f.removePointerPressedListener(pressedListener);
                            f.removePointerReleasedListener(releasedListener);
                            return;
                        }
                        if (disposeWhenPointerOutOfBounds && 
                                pressedOutOfBounds && 
                                !getContentPane().containsOrOwns(evt.getX(), evt.getY()) &&
                                !getTitleComponent().containsOrOwns(evt.getX(), evt.getY())) {
                            evt.consume();
                            f.removePointerPressedListener(pressedListener);
                            f.removePointerReleasedListener(releasedListener);
                            dispose();
                        }
                    }
                };
            }
            f.addPointerPressedListener(pressedListener);
            f.addPointerReleasedListener(releasedListener);
            
        }
    }
    
    /**
     * A popup dialog is shown with the context of a component and  its selection. You should use {@link #setDisposeWhenPointerOutOfBounds(boolean)} to make it dispose
     * when the user clicks outside the bounds of the popup. It can optionally provide an arrow in the theme to point at the context component. The popup
     * dialog has the {@literal PopupDialog} style by default.
     *
     * @param c the context component which is used to position the dialog and can also be pointed at
     */
    public void showPopupDialog(Component c) {
        showPopupDialog(c, Display.getInstance().isPortrait());
    }    
    
    /**
     * A popup dialog is shown with the context of a component and  its selection. You should use {@link #setDisposeWhenPointerOutOfBounds(boolean)} to make it dispose
     * when the user clicks outside the bounds of the popup. It can optionally provide an arrow in the theme to point at the context component. The popup
     * dialog has the {@literal PopupDialog} style by default.
     *
     * @param c the context component which is used to position the dialog and can also be pointed at
     * @param bias biases the dialog to appear above/below or to the sides.
     *          This is ignored if there isn't enough space
     */
    public void showPopupDialog(Component c, boolean bias) {
        Form f = c== null ? null : c.getComponentForm();
        if (f != null) {
            if (!formMode && !f.getContentPane().contains(c)) {
                setFormMode(true);
            }
        }
        disposed = false;
        getUnselectedStyle().setOpacity(255);
        Rectangle componentPos = c.getSelectedRect();
        componentPos.setX(componentPos.getX() - c.getScrollX());
        componentPos.setY(componentPos.getY() - c.getScrollY());
        setOwner(c);
        showPopupDialog(componentPos, bias);
    }
    
    /**
     * A popup dialog is shown with the context of a component and  its selection. You should use {@link #setDisposeWhenPointerOutOfBounds(boolean)} to make it dispose
     * when the user clicks outside the bounds of the popup.  It can optionally provide an arrow in the theme to point at the context component. The popup
     * dialog has the {@literal PopupDialog} style by default.
     *
     * @param rect the screen rectangle to which the popup should point
     */
    public void showPopupDialog(Rectangle rect) {
        showPopupDialog(rect, Display.getInstance().isPortrait());
    }

    /**
     * A popup dialog is shown with the context of a component and  its selection. You should use {@link #setDisposeWhenPointerOutOfBounds(boolean)} to make it dispose
     * when the user clicks outside the bounds of the popup.  It can optionally provide an arrow in the theme to point at the context component. The popup
     * dialog has the {@literal PopupDialog} style by default.
     *
     * @param rect the screen rectangle to which the popup should point
     * @param bias biases the dialog to appear above/below or to the sides.
     *          This is ignored if there isn't enough space
     */
    public void showPopupDialog(Rectangle rect, boolean bias) {
        Form f = Display.getInstance().getCurrent();
        Rectangle origRect = rect;
        rect = new Rectangle(rect);
        rect.setX(rect.getX() - getLayeredPane(f).getAbsoluteX());
        rect.setY(rect.getY() - getLayeredPane(f).getAbsoluteY());
        disposed = false;
        pressedOutOfBounds = false;
        getUnselectedStyle().setOpacity(255);
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
        if((dialogTitle != null || dialogTitle.length() == 0) && manager.isThemeConstant("hideEmptyTitleBool", true)) {
            boolean b = getTitle().length() > 0;
            titleArea.setVisible(b);
            getTitleComponent().setVisible(b);
            if(!b && manager.isThemeConstant("shrinkPopupTitleBool", true)) {
                getTitleComponent().setPreferredSize(new Dimension(0,0));
                getTitleComponent().getStyle().setBorder(null);
                titleArea.setPreferredSize(new Dimension(0,0));
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
        } else {
            Border border = contentPaneStyle.getBorder();
            if(border != null) {
                border.setTrackComponent(origRect);
            }
        }
        calcPreferredSize();
        int prefHeight = getPreferredH();
        int prefWidth = getPreferredW();
        if(contentPaneStyle.getBorder() != null) {
            prefWidth = Math.max(contentPaneStyle.getBorder().getMinimumWidth(), prefWidth);
            prefHeight = Math.max(contentPaneStyle.getBorder().getMinimumHeight(), prefHeight);
        }
        
        
        int availableHeight = getLayeredPane(f).getParent().getHeight();
        if (availableHeight == 0) {
            availableHeight = CN.getDisplayHeight();
        }
        int availableWidth =getLayeredPane(f).getParent().getWidth();
        if (availableWidth == 0) {
            availableWidth = CN.getDisplayWidth();
        }
        int width = Math.min(availableWidth, prefWidth);
        setWidth(width);
        setShouldCalcPreferredSize(true);
        revalidate();
        prefHeight = getPreferredH();
        
        int x = 0;
        int y = 0;

        boolean showPortrait = bias;

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
            if(rect.getY() + rect.getHeight() < availableHeight / 2) {
                // popup downwards
                y = rect.getY() + rect.getHeight();
                int height = Math.min(prefHeight, Math.max(0, availableHeight - y));
                padOrientation(contentPaneStyle, TOP, 1);
                show(Math.max(0, y), Math.max(0, availableHeight - height - y),
                        Math.max(0, x), Math.max(0, availableWidth - width - x));
                padOrientation(contentPaneStyle, TOP, -1);
            } else if (rect.getY() > availableHeight / 2){
                // popup upwards
                int height = Math.min(prefHeight, rect.getY());
                y = rect.getY() - height;
                padOrientation(contentPaneStyle, BOTTOM, 1);
                show(y, Math.max(0, availableHeight - rect.getY()), x, Math.max(0, availableWidth - width - x));
                padOrientation(contentPaneStyle, BOTTOM, -1);
            } else if (rect.getY() < availableHeight / 2) {
                // popup over aligned with top of rect, but inset a few mm
                y = rect.getY() + CN.convertToPixels(3);
                
                int height = Math.min(prefHeight, availableHeight - y);
                padOrientation(contentPaneStyle, BOTTOM, 1);
                show(y, Math.max(0, availableHeight - height - y), 
                        Math.max(0, x), Math.max(0, availableWidth - width - x));
                padOrientation(contentPaneStyle, BOTTOM, -1);
            } else {
                // popup over aligned with bottom of rect but inset a few mm
                y = Math.max(0, rect.getY() + rect.getHeight() - CN.convertToPixels(3) - prefHeight);
                int height = prefHeight;
                padOrientation(contentPaneStyle, TOP, 1);
                show(y, Math.max(0, availableHeight - height - y), 
                        Math.max(0, x), Math.max(0, availableWidth - width - x));
                padOrientation(contentPaneStyle, TOP, -1);
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
            
            if(prefWidth <  availableWidth - rect.getX() - rect.getWidth()) {
                // popup right
                x = rect.getX() + rect.getWidth();
                
                
                width = Math.min(prefWidth, availableWidth - x);
                show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x));
            } else if (prefWidth < rect.getX()) {
                x = rect.getX() - prefWidth;
                width = prefWidth;
                show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x));
            } else {
                // popup left
                width = Math.min(prefWidth, availableWidth - (availableWidth - rect.getX()));
                x = rect.getX() - width;
                show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x));
            }
        }
    }
    
    
    private void padOrientation(Style s, int orientation, int padding) {
        byte[] b = s.getPaddingUnit();
        byte unit = b == null ? Style.UNIT_TYPE_PIXELS : s.getPaddingUnit()[orientation];
        if(unit != Style.UNIT_TYPE_DIPS) {
            padding = Display.getInstance().convertToPixels(padding);
        }
        s.setPadding(orientation, s.getPaddingValue(isRTL(), 
                orientation) + padding);
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

    /**
     * Repositions the component so the animation will "grow/shrink" when showing/disposing
     * @return the repositionAnimation
     */
    public boolean isRepositionAnimation() {
        return repositionAnimation;
    }

    /**
     * Repositions the component so the animation will "grow/shrink" when showing/disposing
     * @param repositionAnimation the repositionAnimation to set
     */
    public void setRepositionAnimation(boolean repositionAnimation) {
        this.repositionAnimation = repositionAnimation;
    }

    /**
     * Whether the interaction dialog uses the form layered pane of the regular layered pane
     * @return the formMode
     */
    public boolean isFormMode() {
        return formMode;
    }

    /**
     * Whether the interaction dialog uses the form layered pane of the regular layered pane
     * @param formMode the formMode to set
     */
    public void setFormMode(boolean formMode) {
        this.formMode = formMode;
    }
}
