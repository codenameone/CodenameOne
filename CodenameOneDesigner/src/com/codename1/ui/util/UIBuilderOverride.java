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

package com.codename1.ui.util;

import com.codename1.designer.ActionCommand;
import com.codename1.designer.UserInterfaceEditor;

/**
 * Extends the UIBuilder from CodenameOne to provide a callback on loading
 *
 * @author Shai Almog
 */
public class UIBuilderOverride extends UIBuilder {
    private UserInterfaceEditor ui;
    private String baseFormName;
    public UIBuilderOverride(UserInterfaceEditor ui) {
        this.ui = ui;
        registerCustom();
    }

    public static void registerCustom() {
        registerCustomComponent("Table", com.codename1.ui.table.Table.class);
        registerCustomComponent("MediaPlayer", com.codename1.components.MediaPlayer.class);
        registerCustomComponent("ContainerList", com.codename1.ui.list.ContainerList.class);
        registerCustomComponent("ComponentGroup", com.codename1.ui.ComponentGroup.class);
        registerCustomComponent("Tree", com.codename1.ui.tree.Tree.class);
        registerCustomComponent("HTMLComponent", com.codename1.ui.html.HTMLComponent.class);
        registerCustomComponent("RSSReader", com.codename1.components.RSSReader.class);
        registerCustomComponent("FileTree", com.codename1.components.FileTree.class);
        registerCustomComponent("WebBrowser", com.codename1.components.WebBrowser.class);
        registerCustomComponent("NumericSpinner", com.codename1.ui.spinner.NumericSpinner.class);
        registerCustomComponent("DateSpinner", com.codename1.ui.spinner.DateSpinner.class);
        registerCustomComponent("TimeSpinner", com.codename1.ui.spinner.TimeSpinner.class);
        registerCustomComponent("DateTimeSpinner", com.codename1.ui.spinner.DateTimeSpinner.class);
        registerCustomComponent("GenericSpinner", com.codename1.ui.spinner.GenericSpinner.class);
        registerCustomComponent("LikeButton", com.codename1.facebook.ui.LikeButton.class);
        registerCustomComponent("InfiniteProgress", com.codename1.components.InfiniteProgress.class);
        registerCustomComponent("MultiButton", com.codename1.components.MultiButton.class);
        registerCustomComponent("SpanButton", com.codename1.components.SpanButton.class);
        registerCustomComponent("Ads", com.codename1.components.Ads.class);
        registerCustomComponent("MapComponent", com.codename1.maps.MapComponent.class);
        registerCustomComponent("MultiList", com.codename1.ui.list.MultiList.class);
        registerCustomComponent("ShareButton", com.codename1.components.ShareButton.class);
        registerCustomComponent("OnOffSwitch", com.codename1.components.OnOffSwitch.class);
        registerCustomComponent("ImageViewer", com.codename1.components.ImageViewer.class);
    }

    void modifyingProperty(com.codename1.ui.Component c, int p) {
        if(ui != null) {
            ui.setPropertyModified(c, p);
        }
    }

    void modifyingCustomProperty(com.codename1.ui.Component c, String name) {
        if(ui != null) {
            ui.setCustomPropertyModified(c, name);
        }
    }

    public com.codename1.ui.Command createCommandImpl(String commandName, com.codename1.ui.Image icon, int commandId, String action, boolean isBack, String argument) {
        return new ActionCommand(commandName, icon, commandId, action, isBack, argument);
    }

    public static void setIgnorBaseForm(boolean b) {
        ignorBaseForm  = b;
    }

    void initBaseForm(String formName) {
        this.baseFormName = formName;
    }

    /**
     * @return the baseFormName
     */
    public String getBaseFormName() {
        return baseFormName;
    }

    /**
     * @param baseFormName the baseFormName to set
     */
    public void setBaseFormName(String baseFormName) {
        this.baseFormName = baseFormName;
    }

    protected void postCreateComponent(com.codename1.ui.Component c) {
        c.setPropertyValue("$designMode", Boolean.TRUE);
    }
}
