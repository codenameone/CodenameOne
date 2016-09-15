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
 * Floating action buttons are a material design element used to promote a special action in a Form.
 * They are represented as a floating circle with a flat icon floating above the UI typically in the bottom right
 * area.
 *
 * @author Chen
 */
public class FloatingActionButton extends Button {

    private List<FloatingActionButton> subMenu;

    private String text;
    private int shadowOpacity = 100;
    private Dialog current;

    /**
     * Constructor
     *
     * @param icon one of the FontImage.MATERIAL_* constants
     * @param text the text of the sub FloatingActionButton
     * @param size the size in millimeters
     */
    protected FloatingActionButton(char icon, String text, float size) {
        FontImage image = FontImage.createMaterial(icon, "FloatingActionButton", size);
        setIcon(image);
        setText("");
        this.text = text;
        setUIID("FloatingActionButton");
        Style all = getAllStyles();
        all.setAlignment(CENTER);
        updateBorder();
    }

    private void updateBorder() {
        getUnselectedStyle().setBorder(RoundBorder.create().
                color(getUnselectedStyle().getBgColor()).
                shadowOpacity(shadowOpacity));
        getSelectedStyle().setBorder(RoundBorder.create().
                color(getSelectedStyle().getBgColor()).
                shadowOpacity(shadowOpacity));
        getPressedStyle().setBorder(RoundBorder.create().
                color(getPressedStyle().getBgColor()).
                shadowOpacity(shadowOpacity));
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
    }    
    
    /**
     * a factory method to create a FloatingActionButton.
     *
     * @param icon one of the FontImage.MATERIAL_* constants
     * @return a FloatingActionButton instance
     */
    public static FloatingActionButton createFAB(char icon) {
        return new FloatingActionButton(icon, null, 3.8f);
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
        return new Dimension(getIcon().getWidth() * 11 / 4, getIcon().getHeight() * 11 / 4);
    }

    /**
     * This is a utility method to bind the FAB to a given Container, it will return a new container to add or will
     * use the layered pane if the container is a content pane.
     *
     * @param cnt the Container to add the FAB to
     * @return a new Container that contains the cnt and the FAB on top or null in the case of a content pane
     */
    public Container bindFabToContainer(Container cnt) {
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
    public Container bindFabToContainer(Container cnt, int orientation, int valign) {
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
        this.text = text;
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