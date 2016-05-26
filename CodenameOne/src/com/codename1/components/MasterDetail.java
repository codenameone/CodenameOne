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
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Master-detail utility class simplifying the process of defining a master/detail
 *
 * @author Shai Almog
 */
public class MasterDetail {
    /**
     * @deprecated this was a half baked idea that made it into the public API
     */
    public static void bindTabletLandscapeMaster(final Form rootForm, Container parentContainer, Component landscapeUI, final Component portraitUI, final String commandTitle, Image commandIcon) {
        landscapeUI.setHideInPortrait(true);
        parentContainer.addComponent(BorderLayout.WEST, landscapeUI);

        final Command masterCommand = new Command(commandTitle, commandIcon) {
            public void actionPerformed(ActionEvent ev) {
                Dialog dlg = new Dialog();
                dlg.setLayout(new BorderLayout());
                dlg.setDialogUIID("Container");
                dlg.getContentPane().setUIID("Container");
                Container titleArea = new Container(new BorderLayout());
                dlg.addComponent(BorderLayout.NORTH, titleArea);
                titleArea.setUIID("TitleArea");
                Label title = new Label(commandTitle);
                titleArea.addComponent(BorderLayout.CENTER, title);
                title.setUIID("Title");
                Container body = new Container(new BorderLayout());
                body.setUIID("Form");
                body.addComponent(BorderLayout.CENTER, portraitUI);
                dlg.setTransitionInAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, false, 250));
                dlg.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 250));
                dlg.addComponent(BorderLayout.CENTER, body);
                dlg.setDisposeWhenPointerOutOfBounds(true);
                dlg.showStetched(BorderLayout.WEST, true);
                dlg.removeComponent(portraitUI);
            }
        };
        if(Display.getInstance().isPortrait()) {
            if(rootForm.getCommandCount() > 0) {
                rootForm.addCommand(masterCommand, 1);
            } else {
                rootForm.addCommand(masterCommand);                
            }
        }
        rootForm.addOrientationListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(portraitUI.getParent() != null) {
                    Form f = Display.getInstance().getCurrent();
                    if(f instanceof Dialog) {
                        ((Dialog)f).dispose();
                    }
                }
                if(Display.getInstance().isPortrait()) {
                    rootForm.addCommand(masterCommand, 1);
                } else {
                    rootForm.removeCommand(masterCommand);
                    rootForm.revalidate();
                }
            }
        });        
    }
}
