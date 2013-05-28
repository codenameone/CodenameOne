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
package com.codename1.maker;

import com.codename1.ui.Container;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.util.Hashtable;

/**
 * A Maker plugin allows developers to build extensions to the Codename One Maker tool, it
 * is implemented as an abstract class to allow future extensibility.
 *
 * @author Shai Almog
 */
public abstract class MakerPlugin {
    private String title;
    private String description;
    private EncodedImage icon;
    private Hashtable<String, Object> data;
    
    /**
     * A unique identifier for this specific plugin
     * @return the package of the plugin in the form of com.companyname.pluginname etc.
     */
    public abstract String getPackageName();
    
    /**
     * Returns the email for the developer used in registration to the Codename One build server
     * @return the email for the developer used in registration to the Codename One build server
     */
    public abstract String getDeveloperEmailId();

    /**
     * Returns the user displayable name of the plugin
     * @return the user displayable name of the plugin
     */
    public abstract String getPluginName();

    /**
     * The title that the user has set to this plugin
     * @return the title
     */
    public final String getTitle() {
        return title;
    }

    /**
     * The title that the user has set to this plugin
     * @param title the title to set
     */
    public final void setTitle(String title) {
        this.title = title;
    }

    /**
     * The description that the user has set to this plugin
     * @return the description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * The description that the user has set to this plugin
     * @param description the description to set
     */
    public final void setDescription(String description) {
        this.description = description;
    }

    /**
     * The icon that the user has set to this plugin
     * @return the icon
     */
    public final EncodedImage getIcon() {
        return icon;
    }

    /**
     * The icon that the user has set to this plugin
     * @param icon the icon to set
     */
    public final void setIcon(EncodedImage icon) {
        this.icon = icon;
    }

    /**
     * The meta data associated with the given plugin based on the definitions in the accompanying plugin descriptor file
     * @param data the data
     */
    public final void setMetaData(Hashtable<String, Object> data) {
        this.data = data;
    }

    /**
     * The meta data associated with the given plugin based on the definitions in the accompanying plugin descriptor file
     * 
     * @return the data
     */
    public final Hashtable<String, Object> getMetaData() {
        return data;
    }
    
    /**
     * Creates a container version of the UI that will be added to the flow based on maker logic
     * @return the embedded UI
     */
    public abstract Container createEmbeddedUI();
    
    /**
     * Creates a standalone version of the UI that will be shown by the maker tool, notice that 
     * back functionality will be added by the maker tool and should not be incorporated into the form!
     * The default implementation just used the embedded container version. Normally developers don't need
     * to override this method unless they need specific behavior for a form
     * @return the standalone UI
     */
    public Form createStandaloneUI() {
        Form f = new Form(getTitle());
        f.getContentPane().setScrollable(false);
        f.getContentPane().setLayout(new BorderLayout());
        f.addComponent(BorderLayout.CENTER, createEmbeddedUI());
        return f;
    }
}
