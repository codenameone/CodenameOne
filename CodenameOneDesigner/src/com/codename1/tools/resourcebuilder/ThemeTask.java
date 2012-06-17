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

import com.codename1.designer.AddThemeEntry;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.util.EditableResources;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;

/**
 * Allows us to convert a theme into the resource file
 *
 * @author Shai Almog
 */
public class ThemeTask extends ResourceTask implements ThemeTaskConstants {
    
    private File file;

    private Map<String, Integer> borderTypes = new HashMap<String, Integer>();
    
    public ThemeTask() {
        borderTypes.put("NULL", -1);
        borderTypes.put("EMPTY", TYPE_EMPTY);
        borderTypes.put("LINE", TYPE_LINE);
        borderTypes.put("ROUNDED", TYPE_ROUNDED);
        borderTypes.put("ETCHED_LOWERED", TYPE_ETCHED_LOWERED);
        borderTypes.put("ETCHED_RAISED", TYPE_ETCHED_RAISED);
        borderTypes.put("ETCHED", TYPE_ETCHED_RAISED);
        borderTypes.put("BEVEL_RAISED", TYPE_BEVEL_RAISED);
        borderTypes.put("BEVEL_LOWERED", TYPE_BEVEL_LOWERED);
        borderTypes.put("BEVEL", TYPE_BEVEL_RAISED);
        borderTypes.put("IMAGE", TYPE_IMAGE);
    }
    

    public File getSrc() {
        return file;
    }
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public void addToResources(EditableResources e) throws IOException {
        Properties p = new Properties();
        FileInputStream f = new FileInputStream(file);
        p.load(f);
        f.close();
        e.setTheme(getName(), new Hashtable());
        for(Object keyObject : p.keySet()) {
            String key = (String)keyObject;
            String value = p.getProperty(key);

            // if this is a simple numeric value
            if(key.endsWith("Color")) {
                e.setThemeProperty(getName(), key, value);
                continue;
            }

            // if this is a short numeric value
            if(key.endsWith("transparency")) {
                e.setThemeProperty(getName(), key, value);
                continue;
            }

            // if this is padding or margin
            if(key.endsWith("padding") || key.endsWith("margin")) {
                // remove spaces
                value = value.replaceAll(" ", "");
                e.setThemeProperty(getName(), key, value);
            }

            // if this is a font
            if(key.endsWith("font")) {
                if(value.indexOf('{') > -1) {
                    throw new BuildException("Plain system fonts and the Bitmap/System syntax are no longer supported, use the Font task and specify the system attribute");
                }
                if(!e.containsResource(value)) {
                    throw new BuildException("You must define a <font> tag with the name matching the name defined in " + key);
                }
                e.setThemeProperty(getName(), key, e.getFont(value));
            }

            // cmp.border=Rounded(3,3);Rounded(4,4);Etched
            if(key.endsWith("border")) {
                e.setThemeProperty(getName(), key, createBorder(value, e));
                continue;
            }

            // if this is a background image
            if(key.endsWith("bgImage")) {
                e.setThemeProperty(getName(), key, e.getImage(value));
                continue;
            }

            if(key.endsWith("bgType")) {
                setBgType(value, e, key);
                continue;
            }

            if(key.endsWith("bgGradient")) {
                StringTokenizer tok = new StringTokenizer(value, ",; ");
                int a = Integer.valueOf(tok.nextToken(), 16);
                int b = Integer.valueOf(tok.nextToken(), 16);
                if(tok.hasMoreTokens()) {
                    e.setThemeProperty(getName(), key, new Object[] {
                        new Integer(a), new Integer(b),
                        Float.valueOf(tok.nextToken()),
                        Float.valueOf(tok.nextToken()),
                        Float.valueOf(tok.nextToken())
                    });
                    continue;
                }

                e.setThemeProperty(getName(), key, new Object[] {
                    new Integer(a), new Integer(b),
                    new Float(0), new Float(0)
                });
            }
        }
    }

    private Border createBorder(String border, EditableResources e) {
        int type = borderName(border);
        StringTokenizer tokenizer = new StringTokenizer(border, "(),;");
        tokenizer.nextToken();
        switch(type) {
            case TYPE_EMPTY:
                return Border.createEmpty();
            case TYPE_LINE:
                // use theme colors?
                int thinkness = Integer.parseInt(tokenizer.nextToken());
                if(!tokenizer.hasMoreTokens()) {
                    return Border.createLineBorder(thinkness);
                } else {
                    return Border.createLineBorder(thinkness, Integer.valueOf(tokenizer.nextToken(), 16));
                }
            case TYPE_ROUNDED:
                // use theme colors?
                int arcWidth = Integer.parseInt(tokenizer.nextToken());
                int arcHeight = Integer.parseInt(tokenizer.nextToken());
                if(!tokenizer.hasMoreTokens()) {
                    return Border.createRoundBorder(arcWidth, arcHeight);
                } else {
                    return Border.createRoundBorder(arcWidth, arcHeight, Integer.valueOf(tokenizer.nextToken(), 16));
                }
            case TYPE_ETCHED_RAISED:
                // use theme colors?
                if(!tokenizer.hasMoreTokens()) {
                    return Border.createEtchedRaised();
                } else {
                    return Border.createEtchedRaised(Integer.valueOf(tokenizer.nextToken(), 16) , Integer.valueOf(tokenizer.nextToken(), 16));
                }
            case TYPE_ETCHED_LOWERED:
                // use theme colors?
                if(!tokenizer.hasMoreTokens()) {
                    return Border.createEtchedLowered();
                } else {
                    return Border.createEtchedLowered(Integer.valueOf(tokenizer.nextToken(), 16) , Integer.valueOf(tokenizer.nextToken(), 16));
                }
            case TYPE_BEVEL_LOWERED:
                // use theme colors?
                if(!tokenizer.hasMoreTokens()) {
                    return Border.createBevelLowered();
                } else {
                    return Border.createBevelLowered(Integer.valueOf(tokenizer.nextToken(), 16),
                            Integer.valueOf(tokenizer.nextToken(), 16),
                            Integer.valueOf(tokenizer.nextToken(), 16),
                            Integer.valueOf(tokenizer.nextToken(), 16));
                }
            case TYPE_BEVEL_RAISED:
                // use theme colors?
                if(!tokenizer.hasMoreTokens()) {
                    return Border.createBevelRaised();
                } else {
                    return Border.createBevelRaised(Integer.valueOf(tokenizer.nextToken(), 16),
                            Integer.valueOf(tokenizer.nextToken(), 16),
                            Integer.valueOf(tokenizer.nextToken(), 16),
                            Integer.valueOf(tokenizer.nextToken(), 16));
                }
            case TYPE_IMAGE:
                List<String> images = new ArrayList<String>();
                while(tokenizer.hasMoreTokens()) {
                    images.add(tokenizer.nextToken());
                }
                int resourceCount = images.size();
                if(resourceCount != 2 && resourceCount != 3 && resourceCount != 8 && resourceCount != 9) {
                    System.out.println("Illegal resource count for image border: " + resourceCount);
                    while(images.size() > 2) {
                        images.remove(images.size() - 1);
                    }
                }
                switch(resourceCount) {
                    case 2:
                        return Border.createImageBorder(e.getImage(images.get(0)), e.getImage(images.get(1)), null);
                    case 3:
                        return Border.createImageBorder(e.getImage(images.get(0)), e.getImage(images.get(1)),
                                e.getImage(images.get(2)));
                    case 8:
                        return Border.createImageBorder(e.getImage(images.get(0)), e.getImage(images.get(1)),
                                e.getImage(images.get(2)), e.getImage(images.get(3)),
                                e.getImage(images.get(4)), e.getImage(images.get(5)),
                                e.getImage(images.get(6)), e.getImage(images.get(7)),
                                null);
                    default:
                        return Border.createImageBorder(e.getImage(images.get(0)), e.getImage(images.get(1)),
                                e.getImage(images.get(2)), e.getImage(images.get(3)),
                                e.getImage(images.get(4)), e.getImage(images.get(5)),
                                e.getImage(images.get(6)), e.getImage(images.get(7)),
                                e.getImage(images.get(8)));
                }
        }
        throw new BuildException("Illegal border: " + border);
    }

    private int borderName(String name) {
        Integer val = borderTypes.get(new StringTokenizer(name, "(),;").nextToken());
        if(val == null) {
            System.out.println("Unrecognized border type: " + name);
            System.out.println("Supported types include: " + borderTypes.values());
            throw new RuntimeException("Unrecognized border type: " + name);
        }
        return val.intValue();
    }

    private void setBgType(String value, EditableResources e, String key) {
        for (int i = 0; i < AddThemeEntry.BACKGROUND_STRINGS.length; i++) {
            if (AddThemeEntry.BACKGROUND_STRINGS[i].equalsIgnoreCase(value)) {
                e.setThemeProperty(getName(), key, AddThemeEntry.BACKGROUND_VALUES[i]);
                return;
            }
        }
        throw new BuildException("Illegal bgType value: " + value);
    }
}
