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
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Floating action buttons are a material design element used to promote a special action in a Form.
 * They are represented as a floating circle with a flat icon floating above the UI typically in the bottom right
 * area.</p>
 * 
 * <p>
 * Simple use cases include just the button as a standalone:
 * </p>
 * <script src="https://gist.github.com/codenameone/f6820a6b0c781e5bb5ffa8004c5b5f2e.js"></script>

* <p>
 * The button can also nest sub actions
 * </p>
 * <script src="https://gist.github.com/codenameone/aa4180054368f61176c55979010d757b.js"></script>
 * <img src="http://www.codenameone.com/img/blog/floating-action.png" alt="Floating Button" />
 *
 * @author Chen
 */
public class FloatingActionButton extends Button {

    /**
     * The default icon size for the fab icon in millimeters 
     * @return the fabDefaultSize
     */
    public static float getIconDefaultSize() {
        return fabDefaultSize;
    }

    /**
     * The default icon size for the fab icon in millimeters 
     * @param aFabDefaultSize the fabDefaultSize to set
     */
    public static void setIconDefaultSize(float aFabDefaultSize) {
        fabDefaultSize = aFabDefaultSize;
    }

    private List<FloatingActionButton> subMenu;

    private String text;
    private int shadowOpacity = 100;
    private Dialog current;
    private boolean rectangle;
    private boolean isBadge;
    
    /**
     * The default icon size for the fab
     */
    private static float fabDefaultSize = 3.8f;

    private float sizeMm = fabDefaultSize;
    
    /**
     * Constructor
     *
     * @param icon one of the FontImage.MATERIAL_* constants
     * @param text the text of the sub FloatingActionButton
     * @param size the size in millimeters
     */
    protected FloatingActionButton(char icon, String text, float size) {
        FontImage image = FontImage.createMaterial(icon, "FloatingActionButton", size);
        sizeMm = size;
        setIcon(image);
        setText("");
        this.text = text;
        setUIID("FloatingActionButton");
        Style all = getAllStyles();
        all.setAlignment(CENTER);
        updateBorder();
    }
        
    /**
     * This constructor is used by text badges
     */
    private FloatingActionButton(String text) {
        super.setText(text);
        rectangle = true;
        shadowOpacity = 0;
        setUIID("Badge");
        updateBorder();
        isBadge = true;
    }
    
    private void updateBorder() {
        getUnselectedStyle().setBorder(RoundBorder.create().
                color(getUnselectedStyle().getBgColor()).
                shadowOpacity(shadowOpacity).rectangle(rectangle));
        getSelectedStyle().setBorder(RoundBorder.create().
                color(getSelectedStyle().getBgColor()).
                shadowOpacity(shadowOpacity).rectangle(rectangle));
        getPressedStyle().setBorder(RoundBorder.create().
                color(getPressedStyle().getBgColor()).
                shadowOpacity(shadowOpacity).rectangle(rectangle));
    }
        
    /**
     * We override this method to track style changes to the background color and map them to the border
     * 
     * {@inheritDoc}
     */
    @Override
    public void styleChanged(String propertyName, Style source) {
        if(propertyName.equals(Style.BG_COLOR)) {
            updateBorder();
        }
        if(getIcon() instanceof FontImage && propertyName.equals(Style.FG_COLOR)) {
            FontImage i = (FontImage)getIcon();
            FontImage image = FontImage.createMaterial(i.getText().charAt(0), "FloatingActionButton", sizeMm);
            setIcon(image);
        }
    }    
    
    /**
     * Creates a text badge
     * @param text the text of the badge
     * @return a badge component
     */
    public static FloatingActionButton createBadge(String text) {
        return new FloatingActionButton(text);
    }
    
    /**
     * a factory method to create a FloatingActionButton.
     *
     * @param icon one of the FontImage.MATERIAL_* constants
     * @return a FloatingActionButton instance
     */
    public static FloatingActionButton createFAB(char icon) {
        return new FloatingActionButton(icon, null, fabDefaultSize);
    }

    /**
     * Adds a sub FAB to the FloatingActionButton instance. Once pressed all its
     * sub FAB's are displayed.
     *
     * @param icon one of the FontImage.MATERIAL_* constants
     * @param text the text of the sub FloatingActionButton
     *
     * @return a FloatingActionButton instance for the sub FAB added
     */
    public FloatingActionButton createSubFAB(char icon, String text) {
        FloatingActionButton sub = new FloatingActionButton(icon, text, 2.8f);
        if (subMenu == null) {
            subMenu = new ArrayList<FloatingActionButton>();
        }
        subMenu.add(sub);
        return sub;
    }

    @Override
    protected Dimension calcPreferredSize() {
        if(getIcon() != null) {
            return new Dimension(getIcon().getWidth() * 11 / 4, getIcon().getHeight() * 11 / 4);
        } 
        return super.calcPreferredSize();
    }
    
    /**
     * This is a utility method to bind the FAB to a given Container, it will return a new container to add or will
     * use the layered pane if the container is a content pane.
     *
     * @param cnt the Container to add the FAB to
     * @return a new Container that contains the cnt and the FAB on top or null in the case of a content pane
     */
    public Container bindFabToContainer(Component cnt) {
        return bindFabToContainer(cnt, Component.RIGHT, Component.BOTTOM);
    }

    /**
     * This is a utility method to bind the FAB to a given Container, it will return a new container to add or will
     * use the layered pane if the container is a content pane.
     *
     * @param cnt the Container to add the FAB to
     * @param orientation one of Component.RIGHT/LEFT/CENTER
     * @param valign one of Component.TOP/BOTTOM/CENTER
     *
     * @return a new Container that contains the cnt and the FAB on top or null in the case of a content pane
     */
    public Container bindFabToContainer(Component cnt, int orientation, int valign) {
        FlowLayout flow = new FlowLayout(orientation);
        flow.setValign(valign);

        Form f = cnt.getComponentForm();
        if(f != null && f.getContentPane() == cnt) {
            // special case for content pane installs the button directly on the content pane
            Container layers = f.getLayeredPane(getClass(), true);
            layers.setLayout(flow);
            layers.add(this);
            return null;
        }
        
        Container conUpper = new Container(flow);
        conUpper.add(this);
        return LayeredLayout.encloseIn(cnt, conUpper);
    }

    @Override
    public void setText(String text) {
        if(isBadge) {
            super.setText(text);
        }
        this.text = text;
    }

    @Override
    protected void fireActionEvent(int x, int y) {
        Form current = Display.getInstance().getCurrent();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
        }
        super.fireActionEvent(x, y);
    }
    
    

    @Override
    public void released(int x, int y) {
        super.released(x, y);

        if (current != null) {
            current.dispose();
            current = null;
        }
        //if this fab has sub fab's display them
        if (subMenu != null) {
            final Container con = createPopupContent(subMenu);
            Dialog d = new Dialog();
            d.setDialogUIID("Container");
            d.getContentPane().setUIID("Container");
            d.setLayout(new BorderLayout());
            d.add(BorderLayout.CENTER, con);
            for (FloatingActionButton next : subMenu) {
                next.current = d;
            }
            d.setTransitionInAnimator(CommonTransitions.createEmpty());
            d.setTransitionOutAnimator(CommonTransitions.createEmpty());
            for(Component c : con) {
                c.setVisible(false);
            }
            Form f = getComponentForm();
            int oldTint = f.getTintColor();
            f.setTintColor(0);
            d.setBlurBackgroundRadius(-1);
            d.addShowListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    for(Component c : con) {
                        c.setY(con.getHeight());
                        c.setVisible(true);
                    }
                    con.animateLayout(200);
                }
            });
            showPopupDialog(d);
            f.setTintColor(oldTint);
            for (FloatingActionButton next : subMenu) {
                next.remove();
            }
            con.removeAll();
        }
    }

    /**
     * Creates the popup content container to display on the dialog.
     *
     * @param fabs List of sub FloatingActionButton
     * @return a Container that contains all fabs
     */
    protected Container createPopupContent(List<FloatingActionButton> fabs) {
        Container con = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        for (FloatingActionButton next : subMenu) {
            next.setPreferredW(getWidth());
            Container c = new Container(new BorderLayout());
            Label txt = new Label(next.text);
            txt.setUIID("FloatingActionText");
            c.add(BorderLayout.CENTER, FlowLayout.encloseRight(txt));
            c.add(BorderLayout.EAST, next);
            con.add(c);
        }
        return con;
    }

    /**
     * Shows the popup Dialog with the sub FABs.
     *
     * @param dialog the Dialog with all sub FAB's Components
     */
    protected void showPopupDialog(Dialog dialog) {
        dialog.setPopupDirectionBiasPortrait(Boolean.TRUE);
        dialog.showPopupDialog(this);
    }

}