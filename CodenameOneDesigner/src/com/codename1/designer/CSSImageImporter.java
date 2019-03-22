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
package com.codename1.designer;

import com.codename1.designer.css.CN1CSSCLI;
import com.codename1.io.Util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A tool to import images into resources files.  Used for CSS projects, when adding images
 * via the GUI builder (i.e. the command line designer with -img or -mimg) options.
 * @author shannah
 */
public class CSSImageImporter {
    
    /**
     * codenameone_settings.properties
     */
    private Properties codenameOneSettings;
    private File resourceFile, imageFile;
    
    /**
     * The source DPI of the imported image.  0 to just import the image - not multiimage.
     * E.g. 160, 320, 480, etc...
     */
    private int sourceDpi=0;
    
    /**
     * Creates a CSS Importer
     * @param resourceFile The .res file to import to.
     * @param imageFile The image file to import.
     * @param sourceDpi The source DPI.  0 for just regular import.  160, 320, 480, etc.. for multiimage.
     */
    public CSSImageImporter(File resourceFile, File imageFile, int sourceDpi) {
        this.resourceFile = resourceFile;
        this.imageFile = imageFile;
        this.sourceDpi = sourceDpi;
        
    }
    
    
    
    private File getResourceFile() {
        return resourceFile;
    }
    
    private File getSrcDir() {
        return resourceFile.getParentFile();
    }
    
    private File getProjectDir() {
        return getSrcDir().getParentFile();
    }
    
    private File getCSSDir() {
        return new File(getProjectDir(), "css");
    }
    
    private File getCSSFile() {
        return new File(getCSSDir(), "theme.css");
    }
    
    
    private File getImagesDir() {
        return new File(getCSSDir(), "images");
    }
    
    
    
    private File getCodenameOneSettingsFile() {
        return new File(getProjectDir(), "codenameone_settings.properties");
    }
    
    private Properties loadCodenameOneSettings() throws IOException {
        Properties out = new Properties();
        try (FileInputStream fis = new FileInputStream(getCodenameOneSettingsFile())) {
            out.load(fis);
        }
        return out;
        
    }
    
    private Properties getCodenameOneSettings() {
        if (codenameOneSettings == null) {
            throw new IllegalStateException("CodenameOneSettings not initialized");
        }
        return codenameOneSettings;
    }
    
    public boolean checkIfCSSProject() throws IOException {
        if (codenameOneSettings == null) {
            codenameOneSettings = loadCodenameOneSettings();
        }
        return isCSSProject();
    }
    
    private boolean isCSSProject() {
        
        return "true".equals(getCodenameOneSettings().getProperty("codename1.cssTheme"));
    }
    
    private String readCSSFileToString() throws IOException {
        try (FileInputStream fis = new FileInputStream(getCSSFile())) {
            return Util.readToString(fis, "UTF-8");
        }
    }
    
    private void writeStringToCSSFile(String str) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getCSSFile())) {
            fos.write(str.getBytes("UTF-8"));
        }
    }
    
    private void appendCSS(String cssContent) throws IOException {
        String css = readCSSFileToString();
        css += "\n" + cssContent;
        writeStringToCSSFile(css);
        
    }
    
    private void copyFile(File src, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(src)) {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                Util.copy(fis, fos);
            }
        }
    }
    
    private File copyImageToCSSDirectory() throws IOException {
        getImagesDir().mkdirs();
        File dest = new File(getImagesDir(), imageFile.getName());
        int dotPos = imageFile.getName().lastIndexOf(".");
        String base;
        String ext;
        if (dotPos > 0) {
            base = imageFile.getName().substring(0, dotPos);
            ext = imageFile.getName().substring(dotPos);
        } else {
            base = imageFile.getName();
            ext = "";
        }
        int index = 2;
        while (dest.exists()) {
            dest = new File(dest.getParentFile(), base + "-"+index+ext);
            index++;
        }
        
        copyFile(imageFile, dest);
        return dest;
    }
    
    
    /**
     * Do the import.  This does all the work.  Copies the image, adds the CSS,
     * and regenerates the .res file from the CSS file.
     * NOTE:  This calls System.exit when done.
     * @throws IOException 
     */
    public void doImportAndExit() throws IOException {
        if (codenameOneSettings == null) {
            codenameOneSettings = loadCodenameOneSettings();
        }
        if (!isCSSProject()) {
            throw new IllegalStateException("This is not a CSS project.  Cannot use CSS import");
        }
        
        File destImage = copyImageToCSSDirectory();
        StringBuilder css = new StringBuilder();
        css.append("import-").append(System.currentTimeMillis()).append("{\n")
                .append("  background-image: url(images/").append(destImage.getName()).append(");\n")
                .append("  cn1-source-dpi: ").append(sourceDpi).append(";\n")
                .append("}\n");
        appendCSS(css.toString());
        try {
            CN1CSSCLI.main(new String[]{
                getCSSFile().getAbsolutePath(),
                getResourceFile().getAbsolutePath()
            });
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException)ex;
            }
            throw new RuntimeException("Failed to compile CSS file.", ex);
        }
        
        
        
        
    }
    
    
    
}
