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
package com.codename1.ui.util.xml;


/**
 * A JAXB XML object for loading the resource file into RAM
 *
 * @author Shai Almog
 */
public class ResourceFileXML {
    private int majorVersion;

    private int minorVersion;
    
    private Theme[] theme;
    
    private Ui[] ui;

    private LegacyFont[] legacyFont;

    private Data[] data;

    private Image[] image;

    private L10n[] l10n;

    private boolean useXmlUI;
    
    /**
     * @return the majorVersion
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    /**
     * @return the minorVersion
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    /**
     * @return the theme
     */
    public Theme[] getTheme() {
        return theme;
    }

    public void setTheme(Theme[] theme) {
        this.theme = theme;
    }

    /**
     * @return the ui
     */
    public Ui[] getUi() {
        return ui;
    }

    public void setUi(Ui[] ui) {
        this.ui = ui;
    }

    /**
     * @return the legacyFont
     */
    public LegacyFont[] getLegacyFont() {
        return legacyFont;
    }

    public void setLegacyFont(LegacyFont[] legacyFont) {
        this.legacyFont = legacyFont;
    }

    /**
     * @return the data
     */
    public Data[] getData() {
        return data;
    }

    public void setData(Data[] data) {
        this.data = data;
    }

    /**
     * @return the image
     */
    public Image[] getImage() {
        return image;
    }

    public void setImage(Image[] image) {
        this.image = image;
    }

    /**
     * @return the l10n
     */
    public L10n[] getL10n() {
        return l10n;
    }

    public void setL10n(L10n[] l10n) {
        this.l10n = l10n;
    }

    /**
     * @return the useXmlUI
     */
    public boolean isUseXmlUI() {
        return useXmlUI;
    }

    /**
     * @param useXmlUI the useXmlUI to set
     */
    public void setUseXmlUI(boolean useXmlUI) {
        this.useXmlUI = useXmlUI;
    }
}
