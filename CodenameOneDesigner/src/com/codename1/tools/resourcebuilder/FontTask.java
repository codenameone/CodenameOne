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

package com.codename1.tools.resourcebuilder;

import com.codename1.ui.EditorFont;
import com.codename1.ui.Font;
import com.codename1.ui.util.EditableResources;
import java.awt.RenderingHints;
import java.io.File;
import org.apache.tools.ant.BuildException;

/**
 * Task to create a bitmap font file
 *
 * @author Shai Almog
 */
public class FontTask extends ResourceTask {
    private String charset = " ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,;:!@/\\*()[]{}|#$%^&<>?'\"";
    private File src;
    private int size = 12;
    private boolean bold;
    private boolean italic;
    private boolean trueType = true;
    private boolean antiAliasing = true;
    private String logicalName;
    private boolean createBitmap = true;
    private int systemFace = Font.FACE_SYSTEM;
    private int systemSize = Font.SIZE_MEDIUM;
    private int systemStyle = Font.STYLE_PLAIN;
    
    @Override
    public void execute() throws BuildException {
        super.execute();
    }
    
    
    public String getIdentity() {
        if(bold) {
            if(italic) {
                return "-bolditalic-";
            } else {
                return "-bold-";
            }
        } else {
            if(italic) {
                return "-italic-";
            } else {
                return "-plain-";
            }
        }
    }

    public static String generateSystemString(Font f) {
        StringBuilder font = new StringBuilder();
        if((f.getFace() & Font.FACE_MONOSPACE) != 0) {
            font.append("FACE_MONOSPACE | ");
        } else {
            if((f.getFace() & Font.FACE_PROPORTIONAL) != 0) {
                font.append("FACE_PROPORTIONAL | ");
            } else {
                font.append("FACE_SYSTEM | ");
            }
        }
        if((f.getStyle() & Font.STYLE_BOLD) != 0) {
            font.append("STYLE_BOLD | ");
        } else {
            if((f.getStyle() & Font.STYLE_ITALIC) != 0) {
                font.append("STYLE_ITALIC | ");
            } else {
                font.append("STYLE_PLAIN | ");
            }
        }
        if((f.getSize() & Font.SIZE_LARGE) != 0) {
            font.append("SIZE_LARGE");
        } else {
            if((f.getSize() & Font.SIZE_SMALL) != 0) {
                font.append("SIZE_SMALL");
            } else {
                font.append("SIZE_MEDIUM");
            }
        }
        return font.toString();
    }

    private void parseSystemFont(String fontName) {
        fontName = fontName.toUpperCase();
        if(fontName.indexOf("FACE_MONOSPACE") > -1) {
            systemFace = Font.FACE_MONOSPACE;
        } else {
            if(fontName.indexOf("FACE_PROPORTIONAL") > -1) {
                systemFace = Font.FACE_PROPORTIONAL;
            } else {
                systemFace = Font.FACE_SYSTEM;
            }
        }
        if(fontName.indexOf("STYLE_BOLD") > -1) {
            systemStyle = Font.STYLE_BOLD;
        } else {
            if(fontName.indexOf("STYLE_ITALIC") > -1) {
                systemStyle = Font.STYLE_ITALIC;
            } else {
                systemStyle = Font.STYLE_PLAIN;
            }
        }
        if(fontName.indexOf("SIZE_LARGE") > -1) {
            systemSize = Font.SIZE_LARGE;
        } else {
            if(fontName.indexOf("SIZE_SMALL") > -1) {
                systemSize = Font.SIZE_SMALL;
            } else {
                systemSize = Font.SIZE_MEDIUM;
            }
        }
    }
    
    public String getFontName() {
        if(logicalName != null) {
            return logicalName.replace('-', '_');
        }
        String name = src.getName();
        name = name.substring(0, name.lastIndexOf('.')).replace('-', '_');
        return name;
    }
    
    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public File getSrc() {
        return src;
    }

    public void setSrc(File src) {
        this.src = src;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isTrueType() {
        return trueType;
    }

    public void setTrueType(boolean trueType) {
        this.trueType = trueType;
    }

    public boolean isAntiAliasing() {
        return antiAliasing;
    }

    public void setAntiAliasing(boolean antiAliasing) {
        this.antiAliasing = antiAliasing;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
        if(getName() == null) {
            setName(logicalName);
        }
    }

    @Override
    public void addToResources(EditableResources e) {
        if(logicalName == null) {
            logicalName = "Arial" + getIdentity() + size;
            createBitmap = false;
        } else {
            if(logicalName.indexOf('-') < 0) {
                logicalName += getIdentity() + size;
            }
        }
        Object aa;
        if(antiAliasing) {
            aa = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
        } else {
            aa = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        }
        EditorFont f = new EditorFont(Font.createSystemFont(systemFace, systemStyle, systemSize),
                null, logicalName, createBitmap, aa, charset);
        e.setFont(getName(), f);
    }

    /**
     * @return the createBitmap
     */
    public boolean isCreateBitmap() {
        return createBitmap;
    }

    /**
     * @param createBitmap the createBitmap to set
     */
    public void setCreateBitmap(boolean createBitmap) {
        this.createBitmap = createBitmap;
    }

    /**
     * @param system the system to set
     */
    public void setSystem(String system) {
        parseSystemFont(system);
    }

}
