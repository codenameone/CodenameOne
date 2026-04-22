/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.ui.util;

import com.codename1.designer.DataEditor;
import com.codename1.designer.FontEditor;
import com.codename1.designer.ImageMultiEditor;
import com.codename1.designer.ImageRGBEditor;
import com.codename1.designer.L10nEditor;
import com.codename1.designer.MultiImageSVGEditor;
import com.codename1.designer.ResourceEditorView;
import com.codename1.designer.ThemeEditor;
import com.codename1.designer.TimelineEditor;
import com.codename1.designer.UserInterfaceEditor;
import com.codename1.impl.javase.JavaSEPortWithSVGSupport;
import com.codename1.ui.Container;
import com.codename1.ui.Image;
import com.codename1.ui.animations.Timeline;
import com.codename1.ui.util.xml.comps.ComponentEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.swing.JComponent;

/**
 * Designer-side subclass of EditableResources that wires the GUI hooks.
 * EditableResources itself lives in the css-compiler module with clean
 * dependencies so the native-themes build can use it without pulling in
 * JavaSE / JavaFX / CEF / Designer GUI classes.
 */
public class EditableResourcesEditor extends EditableResources {

    public EditableResourcesEditor() {
        super();
    }

    public EditableResourcesEditor(InputStream input) throws IOException {
        super(input);
    }

    @Override
    protected byte[] persistUIContainer(Container cnt) {
        return UserInterfaceEditor.persistContainer(cnt, this);
    }

    @Override
    protected Container loadUIContainerFromXml(ComponentEntry uiXMLData) {
        UIBuilderOverride uib = new UIBuilderOverride();
        return uib.createInstance(uiXMLData, this);
    }

    @Override
    protected void writeUIXml(Container cnt, FileOutputStream dest) throws IOException {
        Writer w = new OutputStreamWriter(dest, "UTF-8");
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        StringBuilder bld = new StringBuilder();
        UserInterfaceEditor.persistToXML(cnt, cnt, bld, this, "");
        w.write(bld.toString());
        w.flush();
    }

    @Override
    protected void onOpenFileComplete() {
        ThemeEditor.resetThemeLoaded();
    }

    @Override
    protected EditableResources getRuntimeNativeTheme() {
        return (EditableResources) JavaSEPortWithSVGSupport.getNativeTheme();
    }

    @Override
    protected File getLoadedFile() {
        File override = super.getLoadedFile();
        if (override != null) {
            return override;
        }
        return ResourceEditorView.getLoadedFile();
    }

    /**
     * Opens a GUI editor for the named resource. Only available on the editor
     * subclass because the editors themselves live in the Designer module.
     */
    public JComponent getResourceEditor(String name, ResourceEditorView view) {
        byte magic = getResourceType(name);
        switch (magic) {
            case MAGIC_IMAGE:
            case MAGIC_IMAGE_LEGACY:
                Image i = getImage(name);
                if (getResourceObject(name) instanceof MultiImage) {
                    ImageMultiEditor tl = new ImageMultiEditor(this, name, view);
                    tl.setImage((MultiImage) getResourceObject(name));
                    return tl;
                }
                if (i instanceof Timeline) {
                    TimelineEditor tl = new TimelineEditor(this, name, view);
                    tl.setImage((Timeline) i);
                    return tl;
                }
                if (i.isSVG()) {
                    MultiImageSVGEditor img = new MultiImageSVGEditor(this, name);
                    img.setImage(i);
                    return img;
                }
                ImageRGBEditor img = new ImageRGBEditor(this, name, view);
                img.setImage(i);
                return img;
            case MAGIC_TIMELINE:
                TimelineEditor tl = new TimelineEditor(this, name, view);
                tl.setImage((Timeline) getImage(name));
                return tl;
            case MAGIC_THEME:
            case MAGIC_THEME_LEGACY:
                ThemeEditor theme = new ThemeEditor(this, name, getTheme(name), view);
                return theme;
            case MAGIC_FONT:
            case MAGIC_FONT_LEGACY:
            case MAGIC_INDEXED_FONT_LEGACY:
                FontEditor fonts = new FontEditor(this, getFont(name), name);
                return fonts;
            case MAGIC_DATA:
                DataEditor data = new DataEditor(this, name);
                return data;
            case MAGIC_UI:
                UserInterfaceEditor uie = new UserInterfaceEditor(name, this, view.getProjectGeneratorSettings(), view);
                return uie;
            case MAGIC_L10N:
                L10nEditor l10n = new L10nEditor(this, name);
                return l10n;
            default:
                throw new IllegalArgumentException("Unrecognized magic number: " + Integer.toHexString(magic & 0xff));
        }
    }
}
