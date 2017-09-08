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
package com.codename1.ui.plaf;

import com.codename1.io.Util;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import static com.codename1.ui.plaf.Border.createDashedBorder;
import static com.codename1.ui.plaf.Border.createDottedBorder;
import static com.codename1.ui.plaf.Border.createHorizonalImageBorder;
import static com.codename1.ui.plaf.Border.createImageBorder;
import static com.codename1.ui.plaf.Border.createImageSplicedBorder;
import static com.codename1.ui.plaf.Border.createLineBorder;
import static com.codename1.ui.plaf.Border.createUnderlineBorder;
import static com.codename1.ui.plaf.Border.createVerticalImageBorder;
import com.codename1.ui.util.Resources;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that parses inline style strings into values that can be used directly by
 * {@link UIManager}.
 * @author shannah
 */
class StyleParser {
    private static final byte UNIT_INHERIT=99;
    
    private static class Value {
        byte unit;
        double value;
        
    }
    
    
    
    private static HashMap<String,String> tmpMap = new HashMap<String,String>();
    
    
    static Map<String,String> parseString(Map<String,String> out, String str) {
        String[] rules = Util.split(str, ";");
        for (String rule : rules) {
            rule = rule.trim();
            if (rule.length() == 0) {
                continue;
            }
            int pos = rule.indexOf(":");
            if (pos == -1) {
                continue;
            }
            String key = rule.substring(0, pos);
            out.put(key, rule.substring(pos+1));

        }
        return out;
    }
    
    static String parseMargin(Style baseStyle, String margin) {
        Value[] vals = parseTRBLValue(margin);
        StringBuilder sb = new StringBuilder();
        if (vals[Component.TOP].unit == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.margin[Component.TOP]);
        } else {
            sb.append(vals[Component.TOP].value);
        }
        sb.append(",");
        if (vals[Component.BOTTOM].unit == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.margin[Component.BOTTOM]);
        } else {
            sb.append(vals[Component.BOTTOM].value);
        }
        sb.append(",");
        if (vals[Component.LEFT].unit == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.margin[Component.LEFT]);
        } else {
            sb.append(vals[Component.LEFT].value);
        }
        sb.append(",");
        if (vals[Component.RIGHT].unit == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.margin[Component.RIGHT]);
        } else {
            sb.append(vals[Component.RIGHT].value);
        }
        return sb.toString();
    }
    
    static String parsePadding(Style baseStyle, String padding) {
        Value[] vals = parseTRBLValue(padding);
        StringBuilder sb = new StringBuilder();
        if (vals[Component.TOP].unit == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.padding[Component.TOP]);
        } else {
            sb.append(vals[Component.TOP].value);
        }
        sb.append(",");
        if (vals[Component.BOTTOM].unit == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.padding[Component.BOTTOM]);
        } else {
            sb.append(vals[Component.BOTTOM].value);
        }
        sb.append(",");
        if (vals[Component.LEFT].unit == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.padding[Component.LEFT]);
        } else {
            sb.append(vals[Component.LEFT].value);
        }
        sb.append(",");
        if (vals[Component.RIGHT].unit == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.padding[Component.RIGHT]);
        } else {
            sb.append(vals[Component.RIGHT].value);
        }
        return sb.toString();
    }
    
    
    
    static byte[] parsePaddingUnit(Style baseStyle, String padding) {
        Value[] vals = parseTRBLValue(padding);
        byte[] out = new byte[4];
        for (int i=0; i<4; i++) {
            if (vals[i].unit == UNIT_INHERIT) {
                out[i] = baseStyle == null ? Style.UNIT_TYPE_PIXELS : baseStyle.paddingUnit[i];
            } else {
                out[i] = vals[i].unit;
            }
        }
        return out;
    }
    
    static byte[] parseMarginUnit(Style baseStyle, String margin) {
        Value[] vals = parseTRBLValue(margin);
        byte[] out = new byte[4];
        for (int i=0; i<4; i++) {
            if (vals[i].unit == UNIT_INHERIT) {
                out[i] = baseStyle == null ? Style.UNIT_TYPE_PIXELS : baseStyle.marginUnit[i];
            } else {
                out[i] = vals[i].unit;
            }
        }
        return out;
    }
    
    static Integer parseAlignment(String alignment) {
        if (Character.isDigit(alignment.charAt(0))) {
            return Integer.parseInt(alignment);
        } else if ("center".equalsIgnoreCase(alignment)) {
            return Component.CENTER;
        } else if ("left".equalsIgnoreCase(alignment)) {
            return Component.LEFT;
        } else if ("right".equalsIgnoreCase(alignment)) {
            return Component.RIGHT;
        }
        return null;
    }
    
    private static int getPixelValue(String val) {
        Value v = parseSingleTRBLValue(val);
        switch (v.unit) {
            case Style.UNIT_TYPE_PIXELS:
                return (int)Math.round(v.value);
            case Style.UNIT_TYPE_DIPS:
                return Display.getInstance().convertToPixels((float)v.value);
            case Style.UNIT_TYPE_SCREEN_PERCENTAGE :
                return (int)Math.round(Display.getInstance().getDisplayWidth() * v.value / 100.0);
        }
        return 0;
    }
    
    static Border parseBorder(Resources theme, String args) {
        if (args == null) {
            return Border.createEmpty();
        }
        args = args.trim();
        if ("none".equals(args)) {
            return Border.createEmpty();
        }
        String[] parts1 = Util.split(args, " ");
        int plen = parts1.length;
        if (plen == 0) {
            return Border.createEmpty();
        }
        if (plen == 3) {
            String type = parts1[1];
            int color = Integer.parseInt(parts1[2], 16);
            int thickness = getPixelValue(parts1[0]);
            if (("solid".equals(type) || "line".equals(parts1[1]))) {
                return createLineBorder(thickness, color);
            } else if ("dashed".equals(type)) {
                return createDashedBorder(thickness, color);
            } else if ("dotted".equals(type)) {
                return createDottedBorder(thickness, color);
            } else if ("underline".equals(type)) {
                return createUnderlineBorder(thickness, color);
            }
        }
        
        if (plen == 10) {
            String type = parts1[0];
            if ("image".equals(type)) {
                return createImageBorder(
                        getImage(theme, parts1[1]), 
                        getImage(theme, parts1[2]), 
                        getImage(theme, parts1[3]), 
                        getImage(theme, parts1[4]),
                        getImage(theme, parts1[5]),
                        getImage(theme, parts1[6]),
                        getImage(theme, parts1[7]),
                        getImage(theme, parts1[8]),
                        getImage(theme, parts1[9])
                );
            }
        }
        
        if (plen == 4) {
            String type = parts1[0];
            if ("horizontalImage".equals(type)) {
                return createHorizonalImageBorder(getImage(theme, parts1[1]), getImage(theme, parts1[2]), getImage(theme, parts1[3]));
            } else if ("verticalImage".equals(type)) {
                return createVerticalImageBorder(getImage(theme, parts1[1]), getImage(theme, parts1[2]), getImage(theme, parts1[3]));
            } else if ("image".equals(type)) {
                return createImageBorder(getImage(theme, parts1[1]), getImage(theme, parts1[2]), getImage(theme, parts1[3]));
            }
        }
        
        if (plen == 6) {
            String type = parts1[0];
            if ("splicedImage".equals(type)) {
                Image im = getImage(theme, parts1[1]);
                return createImageSplicedBorder(im, Double.parseDouble(parts1[2]), Double.parseDouble(parts1[3]), Double.parseDouble(parts1[4]), Double.parseDouble(parts1[5]));
            }
        }
        
        return null;
        
    
    }
    
    private static Image getImage(Resources theme, String imageStr) {
        Image im = null;
                
        try {

            if (imageStr.startsWith("/")) {
                im = Image.createImage(imageStr);
            } else {
                im = theme.getImage((String) imageStr);
            }


        } catch (IOException ex) {
            System.out.println("failed to parse image");
        }
        return im;
    }
    
    static Integer parseTextDecoration(String decoration) {
        return null;
    }
    static Font parseFont(Style baseStyle, String font) {
        if (font == null || font.trim().length() == 0) {
            return null;
        }
        font = font.trim();
        String[] args = Util.split(font, " ");
        int len = args.length;
        if (len == 1) {
            String arg = args[0];
            if (Character.isDigit(arg.charAt(0))) {
                // This is a size
                int size = getPixelValue(arg);
                Font f = baseStyle.getFont();
                if (f == null) {
                    f = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR);
                }
                if (f == null) {
                    return Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

                }
                if (f.isTTFNativeFont()) {
                    return f.derive(size, 0);
                } else {
                    return f;
                }
            } else {
                // This must be a font name
                Font baseFont = baseStyle.getFont();

                float pixSize = baseFont != null ? baseFont.getPixelSize() : Display.getInstance().convertToPixels(3f);
                if (arg.indexOf("native:") == 0){
                    return Font.createTrueTypeFont(arg, arg).derive(pixSize, 0);
                } else if (arg.indexOf(".ttf") != -1) {
                    return Font.createTrueTypeFont(arg.substring(0, arg.length()-4), arg).derive(pixSize, 0);
                } else {
                    return Font.createTrueTypeFont(arg, arg+".ttf");
                }
            }
        } else if (len == 2) {
            String sizeArg = args[0];
            String fontNameArg = args[1];
            Font f = fontNameArg == null ? baseStyle.getFont() : Font.createTrueTypeFont(fontNameArg, fontNameArg+".ttf");
            if (f == null) {
                f = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR);
            }
            return f.derive(getPixelValue(sizeArg), 0);

        } else if (len == 3) {
            String sizeArg = args[0];
            String fontNameArg = args[1];
            String fontFileArg = args[2];
            Font f = fontNameArg == null ? baseStyle.getFont() : Font.createTrueTypeFont(fontNameArg, fontFileArg);
            if (f == null) {
                f = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR);
            }
            return f.derive(getPixelValue(sizeArg), 0);
        } else {
            throw new IllegalArgumentException("Failed to parse font");
        }
    }
    
    
    
    private static Value[] parseTRBLValue(String val) {
        Value[] out = new Value[4];
        String[] parts = Util.split(val, " ");
        int len = parts.length;
        switch (len) {
            case 1:
                Value v = parseSingleTRBLValue(parts[0]);
                for (int i=0; i<4; i++) {
                    out[i] = v;
                }
                return out;
            
            case 2: {
                Value v1 = parseSingleTRBLValue(parts[0]);
                Value v2 = parseSingleTRBLValue(parts[1]);
                out[Component.TOP] = out[Component.BOTTOM] = v1;
                out[Component.LEFT] = out[Component.RIGHT] = v2;
                return out;
            }
            case 3: {
                Value v1 = parseSingleTRBLValue(parts[0]);
                Value v2 = parseSingleTRBLValue(parts[1]);
                Value v3 = parseSingleTRBLValue(parts[2]);
                out[Component.TOP] = v1;
                out[Component.LEFT] = out[Component.RIGHT] = v2;
                out[Component.BOTTOM] = v3;
                return out;
            }
            case 4: {
                Value v1 = parseSingleTRBLValue(parts[0]);
                Value v2 = parseSingleTRBLValue(parts[1]);
                Value v3 = parseSingleTRBLValue(parts[2]);
                Value v4 = parseSingleTRBLValue(parts[3]);
                out[Component.TOP] = v1;
                out[Component.RIGHT] = v2;
                out[Component.BOTTOM] = v3;
                out[Component.LEFT] = v4;
                return out;
            }


        }
        return null;
    }

    private static Value parseSingleTRBLValue(String val) {
        val = val.trim();
        int plen = val.length();
        StringBuilder sb = new StringBuilder();
        Value out = new Value();
        boolean parsedValue = false;
        for (int i=0; i<plen; i++) {
            char c = val.charAt(i);
            if (!parsedValue && (c == '%' || c == 'm' || c == 'p' || c == 'i')) {
                out.value = (sb.length() > 0) ? Double.parseDouble(sb.toString()) : 0;
                parsedValue = true;
                sb.setLength(0);
                sb.append(c);
            } else {
                sb.append(c);
            }
        }

        if (parsedValue) {
            String unitStr = sb.toString();
            out.unit = "mm".equals(unitStr) ? Style.UNIT_TYPE_DIPS : 
                    "%".equals(unitStr) ? Style.UNIT_TYPE_SCREEN_PERCENTAGE : 
                    "inherit".equals(unitStr) ? UNIT_INHERIT : 
                    Style.UNIT_TYPE_PIXELS;
        } else {
            out.unit = Style.UNIT_TYPE_PIXELS;
            out.value = Double.parseDouble(sb.toString());
        }
        return out;

    }
    
    static Image parseBgImage(Resources theme, String val) {
        if (val == null || val.trim().length() == 0) {
            return null;
        }
        return getImage(theme, val);
    }
    
    static Integer parseBgType(String val) {
        if (val == null || val.length() == 0) {
            return null;
        }
        if (Character.isDigit(val.charAt(0))) {
            return Integer.parseInt(val);
        }
        if ("none".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_NONE;
        }
        if ("image_aligned_bottom".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM;
        } else if ("image_aligned_top".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_TOP;
        } else if ("image_aligned_bottom_right".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT;
        } else if ("image_aligned_bottom_left".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT;
        } else if ("image_aligned_top_right".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT;
        } else if ("image_aligned_top_left".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT;
        } else if ("image_aligned_left".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_LEFT;
        } else if ("image_aligned_right".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_RIGHT;
        } else if ("image_aligned_center".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_ALIGNED_CENTER;
        } else if ("image_scaled".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_SCALED;
        } else if ("image_scaled_fill".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_SCALED_FILL;
        } else if ("image_scaled_fit".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_SCALED_FIT;
        } else if ("image_tile_both".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_BOTH;
        } else if ("image_tile_horizontal".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_HORIZONTAL;
        } else if ("image_tile_vertical".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_VERTICAL;
        } else if ("image_tile_horizontal_align_bottom".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM;
        } else if ("image_tile_horizontal_align_top".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP;
        } else if ("image_tile_horizontal_align_center".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER;
        } else if ("image_tile_vertical_align_left".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT;
        } else if ("image_tile_vertical_align_right".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT;
        } else if ("image_tile_vertical_align_center".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER;
        } else if ("gradient_radial".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_GRADIENT_RADIAL;
        } else if ("gradient_linear_horizontal".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL;
        } else if ("gradient_linear_vertical".equalsIgnoreCase(val)) {
            return (int)Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL;
        } 
        return null;
    }
}
