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

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;


/**
 * A component to allow a user to enter their signature.  This is just a button that, when pressed,
 * will pop up a dialog where the user can draw their signature with their finger.  The user
 * is given the option to save/reset/cancel the signature.  On save, the {@link #signatureImage} property
 * will be set with a full-size image of the signature, and the "icon" on the button will show a thumbnail of
 * the image.
 * 
 * <h3>Example Usage</h3>
 * 
 * <script src="https://gist.github.com/shannah/d118e34e2cf390acf93d.js"></script>
 * 
 * <h3>Screenshots</h3>
 * 
 * <p><img src="https://www.codenameone.com/img/developer-guide/components-signature1.png" alt="Signature Component button"/></p>
 * <p><img src="https://www.codenameone.com/img/developer-guide/components-signature2.png" alt="Signature Component dialog"/></p>
 * 
 * <h3>Video Demo</h3>
 * 
 * <iframe width="640" height="480" src="https://www.youtube.com/embed/6PXKLyeeFb8" frameborder="0" allowfullscreen></iframe>
 * 
 * <p>Source available <a href="https://github.com/codenameone/codenameone-demos/SignatureComponentDemo">here</a></p>.
 * 
 * @author shannah
 * @since 3.4
 */
public class SignatureComponent extends Container {
    
    private Image signatureImage;
    private final Button lead;
    private final EventDispatcher eventDispatcher = new EventDispatcher();
    private final Font xFont;
    
    /**
     * We need to use a animation on this field to detect when the signature image changes
     * so that the icon can be correctly scaled to be shown on the button.
     */
    private final Animation iconAnimation = new Animation() {

        @Override
        public boolean animate() {
            if (signatureImage != null && signatureImage.animate()) {
                Style s = getStyle();
                Image icon = signatureImage;
                if (icon.getWidth() > getWidth() - s.getHorizontalPadding() || icon.getHeight() > getHeight() - s.getVerticalPadding()) {
                    icon = signatureImage.scaledSmallerRatio(
                            getWidth() - s.getHorizontalPadding(), 
                            getHeight() - s.getVerticalPadding()
                    );
                }
                lead.setIcon(icon);
                repaint();
                return true;
            }
            return false;
        }

        @Override
        public void paint(Graphics g) {
            
        } 
    };
    
    /**
     * Adds a listener to be notified when the signature image is changed.
     * @param l 
     */
    public void addActionListener(ActionListener l) {
        eventDispatcher.addListener(l);
    }
    
    /**
     * Removes a listener from being notified when signature image is changed.
     * @param l 
     */
    public void removeActionListener(ActionListener l) {
        eventDispatcher.removeListener(l);
    }
    
    /**
     * Fires an event to all listeners to notify them that the signature image has
     * been changed.
     */
    protected void fireActionEvent() {
        eventDispatcher.fireActionEvent(new ActionEvent(this));
    }
    
    /**
     * Overridden to register the icon animation when the field is added to the form.
     */
    @Override
    protected void initComponent() {
        super.initComponent(); 
        getComponentForm().registerAnimated(iconAnimation);
    }

    /**
     * Overridden to deregister the icon animation when the field is removed from the form.
     */
    @Override
    protected void deinitialize() {
        getComponentForm().deregisterAnimated(iconAnimation);
        super.deinitialize();
    }
    
    private String localize(String key, String defaultVal) {
        return UIManager.getInstance().localize(key, defaultVal);
    }
    
    /**
     * Creates a new signature component.
     */
    public SignatureComponent() {
        setLayout(new BorderLayout());
        xFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
        lead = new Button() {

            @Override
            protected void paintBackground(Graphics g) {
                super.paintBackground(g);
                g.setColor(0x666666);
                Style s = getStyle();
                g.drawRect(
                        getX()+s.getPaddingLeftNoRTL(), 
                        getY()+s.getPaddingTop(),
                        getWidth()-s.getPaddingRightNoRTL()-s.getPaddingLeftNoRTL(),
                        getHeight()-s.getPaddingBottom() - s.getPaddingTop()
                );
                
                g.setFont(xFont);
                g.drawString("X", getX() + getStyle().getPaddingLeftNoRTL() + Display.getInstance().convertToPixels(3, true), getY() + getHeight() / 2);
            }

            
        };
        lead.setText(localize("SignatureComponent.LeadText","Press to sign"));
        lead.setUIID("SignatureButton");
        lead.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final Dialog dialog = new Dialog(localize("SignatureComponent.DialogTitle", "Sign Here"));
                final SignatureDialogBody sigBody = new SignatureDialogBody() {
                    @Override
                    protected void onCancel() {
                        super.onCancel();
                        dialog.dispose();
                    }
                };

                sigBody.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent sigDoneEvent) {
                        dialog.dispose();
                        setSignatureImage(sigBody.getValue());
                        fireActionEvent();

                    }
                });

                dialog.setLayout(new BorderLayout());
                dialog.addComponent(BorderLayout.CENTER, sigBody);
                dialog.setScrollable(false);
                dialog.setFocusable(false);
                dialog.show();
            }
        });
        addComponent(BorderLayout.CENTER, lead);
    }

    /**
     * Calculates the preferred size of the button itself (not the signature canvas... but the button you press to show the signature panel).
     * @return 
     */
    @Override
    protected Dimension calcPreferredSize() {
        int w = Display.getInstance().convertToPixels(75, true);
        int h = Display.getInstance().convertToPixels(40, true);
        return new Dimension(w, h);
    }
    
    /**
     * Sets the signature image for this field.  This will also scale the image and 
     * show it as the icon for the button.
     * @param img The image to set as the signature image.
     */
    public void setSignatureImage(Image img) {
        if (img != signatureImage) {
            signatureImage = img;
            lead.setText("");
            if (img != null) {
                int maxW = lead.getWidth() - lead.getStyle().getPaddingLeftNoRTL() - lead.getStyle().getPaddingRightNoRTL();
                int maxH = lead.getHeight() - lead.getStyle().getPaddingTop()- lead.getStyle().getPaddingBottom();
                
                Image icon = img;
                if (icon.getWidth() > maxW || icon.getHeight() > maxH) {
                    icon = icon.scaledSmallerRatio(maxW, maxH);
                }
                
                lead.setIcon(icon);
            } else {
                lead.setText(localize("SignatureComponent.LeadText","Press to sign"));
                lead.setIcon(null);
            }
        }
    }
    
    /**
     * Gets the image of the signature - or null if no signature has been drawn.
     * @return 
     */
    public Image getSignatureImage() {
        return signatureImage;
    }
    
    /**
     * Inner class with the actual body of the dialog for drawing the signature.  This dialog
     * is shown when the user clicks on the main button.
     * @author shannah
     */
    private class SignatureDialogBody extends Container {
        private Button doneButton;
        private Button resetButton;
        private Button cancelButton;
        private SignaturePanel signaturePanel;
        private final EventDispatcher eventDispatcher = new EventDispatcher();
        private Image value;

        public SignatureDialogBody() {
            setLayout(new BorderLayout());
            signaturePanel = new SignaturePanel();
            addComponent(BorderLayout.CENTER, signaturePanel);
            doneButton = new Button(localize("SignatureComponent.SaveButtonLabel", "Save"));
            resetButton = new Button(localize("SignatureComponent.ResetButtonLabel", "Reset"));
            cancelButton = new Button(localize("SignatureComponent.CancelButtonLabel", "Cancel"));

            doneButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (signaturePanel.path.getPointsSize() < 2) {
                        Dialog.show(
                                localize("SignatureComponent.ErrorDialog.SignatureRequired.Title", "Signature Required"), 
                                localize("SignatureComponent.ErrorDialog.SignatureRequired.Body", "Please draw your signature in the space provided."), 
                                localize("SignatureComponent.ErrorDialog.OK", "OK"), 
                                null
                        );
                        return;
                    }
                    value = signaturePanel.getImage();
                    eventDispatcher.fireActionEvent(new ActionEvent(this));

                }
            });

            resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    signaturePanel.clear();
                    repaint();
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    onCancel();
                }
            });

            addComponent(BorderLayout.SOUTH, GridLayout.encloseIn(3, cancelButton, resetButton, doneButton));
        }
        
        /**
         * Called when the cancel button is pressed.  Should be overridden by subclasses to 
         * actually do something (like close the dialog).
         */
        protected void onCancel() {
            
        }

        /**
         * Adds a listener to be informed when the "done" button is pressed and a new
         * signature has been saved as an image.
         * @param l 
         */
        public void addActionListener(ActionListener l) {
            eventDispatcher.addListener(l);
        }

        
        /**
         * @see #addActionListener(com.codename1.ui.events.ActionListener) 
         * @param l 
         */
        public void removeActionListener(ActionListener l) {
            eventDispatcher.removeListener(l);
        }

        /**
         * Overridden to automatically save the image of the current SignaturePanel
         * when the dialog is disposed.
         */
        @Override
        protected void deinitialize() {
            if (value == null) {
                // We want to cache the value when this field is being hidden.
                value = signaturePanel.getImage();
            }
            super.deinitialize();
        }

        /**
         * Gets the signature that was drawn, as an Image.  
         * @return 
         */
        public Image getValue() {
            if (value == null) {
                value = signaturePanel.getImage();
            }
            return value;
        }

        /**
         * The actual panel for drawing a signature.  This doesn't include any buttons (like done, reset, or cancel),
         * it merely provides the functionality to record the drawing of a signature.
         */
        private class SignaturePanel extends Component {

            private final GeneralPath path = new GeneralPath();
            private final Stroke stroke = new Stroke();
            private final Rectangle signatureRect = new Rectangle();
            private final Font xFont;
            
            SignaturePanel() {
                stroke.setLineWidth(Math.max(1, Display.getInstance().convertToPixels(1, true)/2));
                getAllStyles().setBgColor(0xffffff);
                getAllStyles().setBgTransparency(255);
                xFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
            }

            /**
             * Overridden to try to make this component as sensitive as possible to 
             * drag events.  If we don't do this, it requires a longer drag before the "drag" 
             * events will kick in.
             * @param x
             * @param y
             * @return 
             */
            @Override
            protected int getDragRegionStatus(int x, int y) {
                return Component.DRAG_REGION_LIKELY_DRAG_XY;
            }
            
            /**
             * 
             * @param g 
             */
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                
                g.setColor(0x666666);
                calcSignatureRect(signatureRect);
                g.drawRect(signatureRect.getX(), signatureRect.getY(), signatureRect.getWidth(), signatureRect.getHeight());
                g.drawString("X", signatureRect.getX() + Display.getInstance().convertToPixels(1, true), signatureRect.getY() + signatureRect.getHeight() / 2);
                paintSignature(g);
            }
            
            /**
             * Paints just the signature portion of the panel.  This is is reuised to
             * also create the image of the signature.
             * @param g 
             */
            private void paintSignature(Graphics g) {
                g.setColor(0x0);
                boolean oldAA = g.isAntiAliased();
                g.setAntiAliased(true);
                g.drawShape(path, stroke);
                g.setAntiAliased(oldAA);
            }
            
            /**
             * Calculates a rectangle (in parent component space) used for the drawn "rectangle" inside
             * which the user should draw their signature.  It tries to create a 16x9 rectangle that
             * fits inside the component with a bit of padding (3mm on each edge).
             * @param r Output variable.
             */
            private void calcSignatureRect(Rectangle r) {
                int w = getWidth() - Display.getInstance().convertToPixels(6, true);
                int h = (int)(w * 9.0 / 16.0);
                if (h > getHeight()) {
                    h = getHeight() - Display.getInstance().convertToPixels(6, false);
                    w = (int)(h * 16.0 / 9.0);
                }
                r.setX(getX() + (getWidth() - w) / 2);
                r.setY(getY() + (getHeight() - h)/2);
                r.setWidth(w);
                r.setHeight(h);
            }
            
            @Override
            protected Dimension calcPreferredSize() {
                Display d = Display.getInstance();
                return new Dimension(d.convertToPixels(100, true), d.convertToPixels(60, false));
            }
            
            @Override
            public void pointerPressed(int x, int y) {
                path.moveTo(x(x), y(y));
                
                value = null;
                repaint();
            }

            @Override
            public void pointerDragged(int x, int y) {
                path.lineTo(x(x), y(y));
                value = null;
                repaint();
            }

            @Override
            public void pointerReleased(int x, int y) {
                value = null;
                repaint();
            }

            /**
             * Converts an x coordinate from screen space, to parent component space.
             * @param x
             * @return 
             */
            private int x(int x) {
                return x - getParent().getAbsoluteX();
            }

            /**
             * Converts a y coordinate from screen space to parent component space.
             * @param y
             * @return 
             */
            private int y(int y) {
                return y - getParent().getAbsoluteY();
            }

            /**
             * Gets the currently drawn signature as an image.  This only includes the 
             * areas inside the {@link #signatureRect}
             * @return 
             */
            private Image getImage() {
                calcSignatureRect(signatureRect);
                
                Image img = Image.createImage(signatureRect.getWidth(), signatureRect.getHeight(), 0xffffff);
                Graphics g = img.getGraphics();
                g.translate(-signatureRect.getX(), -signatureRect.getY());
                paintSignature(g);
                return img;
            }

            /**
             * Resets the signature as a blank path.
             */
            private void clear() {
                path.reset();
            }
        }
    }
}
