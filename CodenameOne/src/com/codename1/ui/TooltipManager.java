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
package com.codename1.ui;

import com.codename1.components.InteractionDialog;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/**
 * Central management for tooltips, this class can be derived/customized
 * to override the default tooltip behavior.
 *
 * @author Shai Almog
 */
public class TooltipManager {
    private static TooltipManager instance;

    private int tooltipShowDelay = 500;
    
    private InteractionDialog currentTooltip;
    private UITimer pendingTooltip; 
    private Component currentComponent;
    private String dialogUIID = "TooltipDialog";
    private String textUIID = "Tooltip";
    
    static TooltipManager getInstance() {
        return instance;
    }
    
    /**
     * Enables the tooltip manager and default tooltip behavior
     */
    public static void enableTooltips() {
        instance = new TooltipManager();
    }
    
    /**
     * Enables the tooltip manager with a custom subclass
     * 
     * @param custom customized subclass of this class
     */
    public static void enableTooltips(TooltipManager custom) {
        instance = custom;
    }
    
    /**
     * Default tooltip manager
     */
    protected TooltipManager() {
    }
 
    /**
     * Invoked to dispose the current tooltip when the pointer moves
     */
    protected void clearTooltip() {
        if(currentTooltip != null) {
            currentTooltip.dispose();
            currentTooltip = null;
        }
        if(pendingTooltip != null) {
            pendingTooltip.cancel();
            pendingTooltip = null;
        }
        currentComponent = null;
    }
    
    /**
     * Gets ready to show the tooltip, this method implements the delay
     * before the actual showing of the tooltip. It's invoked internally
     * by the framework
     * 
     * @param tip the tooltip text
     * @param cmp the component
     */
    protected void prepareTooltip(final String tip, final Component cmp) {
        if(currentComponent == cmp) {
            return;
        }
        clearTooltip();
        currentComponent = cmp;
        pendingTooltip = new UITimer(new Runnable() {
            @Override
            public void run() {
                showTooltip(tip, cmp);
                pendingTooltip = null;
            }
        });
        Form f = cmp.getComponentForm();
        if(f != null) {
            pendingTooltip.schedule(tooltipShowDelay, false, f);
        }
    }
    
    /**
     * Shows the actual tooltip, this is invoked when the time for the tooltip
     * elapses. It shows the tooltip UI immediately
     * 
     * @param tip the tooltip text
     * @param cmp the component
     */
    protected void showTooltip(final String tip, final Component cmp) {
        currentTooltip = new InteractionDialog(new BorderLayout());
        
        TextArea text = new TextArea(tip);
        text.setGrowByContent(true);
        text.setEditable(false);
        text.setFocusable(false);
        text.setActAsLabel(true);
        text.setUIID(textUIID);
        currentTooltip.setUIID(dialogUIID);
        currentTooltip.setDialogUIID("Container");
        currentTooltip.add(BorderLayout.CENTER, text);
        currentTooltip.setAnimateShow(false);
        currentTooltip.showPopupDialog(cmp, true);
    }

    /**
     * The time in milliseconds between the pointer stopping and the showing
     * of the tooltip
     * 
     * @return the tooltipShowDelay
     */
    public int getTooltipShowDelay() {
        return tooltipShowDelay;
    }

    /**
     * The time in milliseconds between the pointer stopping and the showing
     * of the tooltip
     * 
     * @param tooltipShowDelay the tooltipShowDelay to set
     */
    public void setTooltipShowDelay(int tooltipShowDelay) {
        this.tooltipShowDelay = tooltipShowDelay;
    }

    /**
     * UIID of the tooltip dialog
     * 
     * @return the dialogUIID
     */
    public String getDialogUIID() {
        return dialogUIID;
    }

    /**
     * UIID of the tooltip dialog
     * @param dialogUIID the dialogUIID to set
     */
    public void setDialogUIID(String dialogUIID) {
        this.dialogUIID = dialogUIID;
    }

    /**
     * UIID of the tooltip text body
     * @return the textUIID
     */
    public String getTextUIID() {
        return textUIID;
    }

    /**
     * UIID of the tooltip text body
     * @param textUIID the textUIID to set
     */
    public void setTextUIID(String textUIID) {
        this.textUIID = textUIID;
    }
}
