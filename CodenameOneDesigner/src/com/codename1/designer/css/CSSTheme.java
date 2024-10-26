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
package com.codename1.designer.css;

import com.codename1.io.JSONParser;
import com.codename1.io.Util;
import com.codename1.processing.Result;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.EditorTTFFont;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationAccessor;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.plaf.CSSBorder;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EditableResourcesForCSS;
import com.codename1.ui.util.EditableResources;
import com.codename1.ui.util.Resources;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.w3c.css.sac.AttributeCondition;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.DocumentHandler;
import org.w3c.css.sac.ElementSelector;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.css.sac.Parser;
import org.w3c.css.sac.SACMediaList;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
import org.w3c.css.sac.SimpleSelector;
import org.w3c.css.sac.helpers.ParserFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.flute.parser.ParseException;

/**
 *
 * @author shannah
 */
public class CSSTheme {
    private boolean refreshImages;
    URL baseURL;
    File cssFile = new File("test.css");
    File resourceFile = new File("test.css.res");
    Element anyNodeStyle = new Element();
    Map<String,Element> elements = new LinkedHashMap<String,Element>();
    Map<String,LexicalUnit> constants = new LinkedHashMap<String,LexicalUnit>();
    EditableResources res;
    private String themeName = "Theme";
    private ImagesMetadata imagesMetadata = new ImagesMetadata();
    
    private List<FontFace> fontFaces = new ArrayList<FontFace>();
    public static final int DEFAULT_TARGET_DENSITY = com.codename1.ui.Display.DENSITY_HD;
    public static final String[] supportedNativeBorderTypes = new String[]{
        "none",
        "line",
        "bevel",
        "etched",
        "solid"
    };
    
    public static boolean isBorderTypeNativelySupported(String type) {
        for (String str : supportedNativeBorderTypes) {
            if (str.equals(type)) {
                return true;
            }
        }
        return false;
    }
    
    private int targetDensity = DEFAULT_TARGET_DENSITY;
    private double minDpi = 120;
    private double maxDpi = 480;
    private double currentDpi = 480;
    
    
    private static class XYVal {
        double x;
        double y;
        String axis;
        boolean valid;
    }
    
    static boolean isGradient(LexicalUnit background) {
        if (background != null) {
            if (background.getLexicalUnitType() == LexicalUnit.SAC_FUNCTION && "linear-gradient".equals(background.getFunctionName())) {
                return true;
            } else if (background.getLexicalUnitType() == LexicalUnit.SAC_FUNCTION && "radial-gradient".equals(background.getFunctionName())) {
                return true;
            }
        }
        return false;
        
    }
    
    class ImageMetadata {
        private String imageName;
        int sourceDpi;

        public ImageMetadata(String imageName, int sourceDpi) {
            this.imageName = imageName;
            this.sourceDpi = sourceDpi;
        }
        
        
        
    }
    
    
    class ImagesMetadata {
        private Map<String,ImageMetadata> images = new LinkedHashMap<>();
        
        public void addImageMetadata(ImageMetadata data) {
            images.put(data.imageName, data);
        }
        
        public ImageMetadata get(String name) {
            return images.get(name);
        }
        
         void load(Resources res) throws IOException {
            images.clear();
            String metadataStr = (String)res.getTheme(themeName).getOrDefault("@imageMetadata", "{}");
            JSONParser parser = new JSONParser();
            Map metadataMap = parser.parseJSON(new StringReader(metadataStr));
            for (String key : (Set<String>)metadataMap.keySet()) {
                Map data = (Map)metadataMap.get(key);
                int sourceDpi = (int)Math.round((data.containsKey("sourceDpi") ? ((Number)data.get("sourceDpi")).intValue() : 
                        currentDpi));
                String name = (String)data.get("imageName");
                ImageMetadata md = new ImageMetadata(name, sourceDpi);
                addImageMetadata(md);
            }
        }
         
        void store(EditableResources res) {
            Map map = new LinkedHashMap();
            for (String key : images.keySet()) {
                ImageMetadata md = images.get(key);
                Map data = new LinkedHashMap();
                data.put("imageName", md.imageName);
                data.put("sourceDpi", md.sourceDpi);
                map.put(md.imageName, data);
            }
            res.setThemeProperty(themeName, "@imageMetadata", Result.fromContent(map).toString());
        }
    }
    
    static class CN1Gradient {
        /**
         * One of {@link Style#BACKGROUND_GRADIENT_LINEAR_HORIZONTAL}, {@link Style#BACKGROUND_GRADIENT_LINEAR_VERTICAL}, or
         * {@link Style#BACKGROUND_GRADIENT_RADIAL}.
         */
        int type;
        
        int startColor;
        int endColor;
        float gradientX;
        float gradientY;
        float size;
        byte bgTransparency;
        
        String reason;
        
        /**
         * Flag to indicate whether this gradient is valid
         * Only gradients that can be completely reproduced using CN1
         * are valid.   E.g. if the opacity of the start color is different than
         * the end color, or there are multiple stages, or other parameters
         * that can't be expressed as a CN1 gradient style, then this gradient won't 
         * be used and a 9-piece border will be generated as a fallback.
         */
        boolean valid;
        
        void parse(ScaledUnit background) {
            if (background != null) {
                if (background.getLexicalUnitType() == LexicalUnit.SAC_FUNCTION && "linear-gradient".equals(background.getFunctionName())) {
                    parseLinearGradient(background);
                } else if (background.getLexicalUnitType() == LexicalUnit.SAC_FUNCTION && "radial-gradient".equals(background.getFunctionName())) {
                    parseRadialGradient(background);
                }
            }
        }
        
        private XYVal parseTransformCoordVal(String val) {
            XYVal out = new XYVal();
            switch (val) {
                case "center" : out.x = 0.5; out.y = 0.5; break;
                case "left" : out.x = 0; out.y = -1; out.axis = "x"; break;
                case "right" : out.x = 1; out.y = -1; out.axis = "x"; break;
                case "bottom" : out.x = -1;; out.y = 1; out.axis = "y"; break;
                case "top" : out.x = -1; out.y = 0; out.axis = "y"; break;
                default: return out;
            }
            out.valid = true;
            return out;
        }
        
        private XYVal parseTransformCoordVal(double val, boolean x) {
            XYVal out = new XYVal();
            if (x) {
                out.y = 0.5;
                out.x = val / 100;
                out.axis = "x";
            } else {
                out.y = val / 100;
                out.x = 0.5;
                out.axis = "y";
            }
            out.valid = true;
            return out;
        }
        
        
        
        private void parseRadialGradient(ScaledUnit background) {
            if (background == null || background.getLexicalUnitType() != LexicalUnit.SAC_FUNCTION || !"radial-gradient".equals(background.getFunctionName())) {
                return;
            }
            
            ScaledUnit params = (ScaledUnit)background.getParameters();
            if (params == null) {
                reason = "No parameters found in radial-gradient() function [1]";
                return;
            }
            
            ScaledUnit param1 = params;
            ScaledUnit param2 = null;
            double relX = 0.5;
            double relY = 0.5;
            double relSize = 1.0;
            int step = 0;
            while (param1 != null) {
                // Parse the first parameter of radial-gradient
                switch (param1.getLexicalUnitType()) {
                    case LexicalUnit.SAC_IDENT: {
                        switch (param1.getStringValue()) {
                            case "circle": {
                                if (step != 0) {
                                    reason = "Unrecognized syntax for radial gradient [2]";
                                    return;
                                }
                                step++;
                                ScaledUnit nex = (ScaledUnit)param1.getNextLexicalUnit();
                                if (nex == null) {
                                    reason = "Unrecognized syntax for radial gradient [3]";
                                    return;
                                }
                                if (nex.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                                    // It's a circle at center
                                    param1 = null;
                                    param2 = (ScaledUnit)nex.getNextLexicalUnit();
                                    break; 
                                }
                                param1 = nex;
                                //reason = "Unsupported parameter following 'circle' in radial gradient.";
                                //return;
                                break;
                            }
                            case "closest-side" : {
                                // This is the only extent value that is supported.
                                step++;
                                param1 = (ScaledUnit)param1.getNextLexicalUnit();
                                if (param1 != null && param1.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                                    param1 = null;
                                }
                                break;
                                
                            }
                            case "at" : {
                                ScaledUnit nex = (ScaledUnit)param1.getNextLexicalUnit();
                                if (nex == null) {
                                    reason = "Invalid syntax for radial-gradient position.  'at' followed by nothing. [4]";
                                    return;
                                }
                                if (nex.getLexicalUnitType() == LexicalUnit.SAC_IDENT || nex.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                                    XYVal val, val2;
                                    if (nex.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                                        val = parseTransformCoordVal(nex.getNumericValue(), true);
                                    } else {
                                        val = parseTransformCoordVal(nex.getStringValue());
                                    }
                                    if (!val.valid) {
                                        reason = "Invalid position coordinate for radial-gradient. [5]";
                                        return;
                                    }
                                    
                                    
                                    
                                    if ("x".equals(val.axis)) {
                                        relX = val.x;
                                    } else if ("y".equals(val.axis)) {
                                        relY = val.y;
                                    } else {
                                        relX = val.x;
                                        relY = val.y;
                                    }
                                    
                                    nex = (ScaledUnit)nex.getNextLexicalUnit();
                                    if (nex == null) {
                                        reason = "Invalid radial-gradient syntax.  No color information provided. [6]";
                                        return;
                                    }
                                    
                                    if (nex.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                                        // we have our position.
                                        param2 = (ScaledUnit)nex.getNextLexicalUnit();
                                        param1 = null;
                                        break;
                                    }
                                    
                                    if (nex.getLexicalUnitType() == LexicalUnit.SAC_IDENT || nex.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                                        if (nex.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                                            val2 = parseTransformCoordVal(nex.getNumericValue(), "y".equals(val.axis));
                                        } else {
                                            val2 = parseTransformCoordVal(nex.getStringValue());
                                        }
                                        
                                        if ("x".equals(val2.axis)) {
                                            relX = val2.x;
                                        } else if ("y".equals(val2.axis)) {
                                            relY = val2.y;
                                        } else if ("x".equals(val.axis)) {
                                            relY = val2.y;
                                        } else if ("y".equals(val.axis)) {
                                            relX = val2.x;
                                        } else {
                                            relY = val2.y;
                                        }
                                    }
                                    
                                    nex = (ScaledUnit)nex.getNextLexicalUnit();
                                    
                                    if (nex == null || nex.getLexicalUnitType() != LexicalUnit.SAC_OPERATOR_COMMA) {
                                        reason = "Invalid radial-gradient syntax.  No color information provided. [7]";
                                        return;
                                    }
                                    
                                    param1 = null;
                                    param2 = (ScaledUnit) nex.getNextLexicalUnit();
                                    break;
                                    
                                    
                                }
                                break;
                            }
                            
                                
                            
                            default:
                                reason = "Unsupported syntax for radial-gradient. ("+param1.getStringValue()+") [8]";
                                return;

                        }
                    }
                }
            }
            if (param2 == null) {
                reason = "No color information provided (param2==null) [9]";
                return;
            }
            
            Integer color1 = null;
            Integer color2 = null;
            int alpha = 0;
            
            
            while (param2 != null) {
                // parse 2nd param of radial-gradient
                if (color1 == null) {
                    color1 = getColorInt(param2);
                    alpha = getColorAlphaInt(param2);
                    ScaledUnit nex = (ScaledUnit)param2.getNextLexicalUnit();
                    if (nex == null) {
                        reason = "Invalid radial gradient syntax.  Missing 2nd color [11]";
                        return;
                    }
                    
                    if (nex.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                        if (Math.abs(nex.getNumericValue()) > 0.0001) {
                            reason = "CN1 native gradients must have start color at 0%.  Falling back to use image background. [12]";
                            return;
                        }
                        nex = (ScaledUnit)nex.getNextLexicalUnit();
                    }
                    if (nex == null || nex.getLexicalUnitType()!= LexicalUnit.SAC_OPERATOR_COMMA) {
                        reason = "Invalid radial gradient syntax.  Missing 2nd color";
                        return;
                    }
                    param2 = (ScaledUnit)nex.getNextLexicalUnit();
                } else if (color2 == null) {
                    color2 = getColorInt(param2);
                    int alpha2 = getColorAlphaInt(param2);
                    if (alpha2 != alpha) {
                        reason = "Codename One only native supports gradients with the same alpha for start and end colors [13]";
                        return;
                    }
                    ScaledUnit nex = (ScaledUnit)param2.getNextLexicalUnit();
                    if (nex == null) {
                        break;
                    } else if (nex.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                        relSize = nex.getNumericValue() / 100;
                    }
                    
                    nex = (ScaledUnit)nex.getNextLexicalUnit();
                    
                    // This should be the last parameter
                    if (nex != null) {
                        reason = "Only radial gradients with two color stops are supported natively in CN1.  Falling back to image background. [14]";
                        return;
                    }
                    param2 = null;
                }
                
            }
            
            if (color1 == null || color2 == null) {
                reason = "Failed to set either start or end color when parsing radial-gradient [15]";
                return;
            }
            
            type = Style.BACKGROUND_GRADIENT_RADIAL;
            gradientX = 1-(float)relX;
            gradientY = 1-(float)relY;
            this.bgTransparency = (byte)alpha;
            this.startColor = color1;
            this.endColor = color2;
            this.size = (float)relSize;
            this.valid = true;
            
            
        }
        
        
        
        private void parseLinearGradient(ScaledUnit background) {
            
            ScaledUnit linearGradientFunc = null;
            int gradientType = 0;
            boolean reverse = false;
            Integer startColor = null;
            Integer endColor = null;
            int alpha = -1;
            
            loop: while (background != null) {
                switch (background.getLexicalUnitType()) {
                    case LexicalUnit.SAC_FUNCTION:
                        if ("linear-gradient".equals(background.getFunctionName())) {
                            linearGradientFunc = background;
                            break loop;
                        }
                }
                background = (ScaledUnit)background.getNextLexicalUnit();
            }
            
            if (linearGradientFunc == null) {
                return;
            }
            
            ScaledUnit params = (ScaledUnit)linearGradientFunc.getParameters();
            if (params == null) {
                return;
            }
            
            
            ScaledUnit param1 = params;
            switch (param1.getLexicalUnitType()) {
                case LexicalUnit.SAC_IDENT: {
                    String identVal = param1.getStringValue();
                    if ("to".equals(identVal)) {
                        param1 = (ScaledUnit)param1.getNextLexicalUnit();
                        if (param1 == null) {
                            reason = "Invalid linear-gradient syntax.  'to' must be followed by a side.";
                            return;
                        }
                        if (param1.getLexicalUnitType() == LexicalUnit.SAC_IDENT) {
                            String sideStr = param1.getStringValue();
                            switch (sideStr) {
                                case "top" : {
                                    gradientType = Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL;
                                    reverse = true;
                                    break;
                                    
                                }
                                case "bottom" : {
                                    gradientType = Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL;
                                    break;
                                }
                                case "left" : {
                                    gradientType = Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL;
                                    reverse = true;
                                    break;
                                }
                                case "right": {
                                    gradientType = Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL;
                                    break;
                                }
                                default :
                                    reason = "Unrecognized side identifier '"+sideStr+"'.  Expected top, left, bottom, or right";
                                    return;
      
                            }
                        } else {
                            reason = "Unsupported syntax following to ident in linear-gradient.";
                            return;
                        }
                    } else {
                        reason = "Unsupported syntax for first parameter of linear-gradient.  Only 'to' and 'degree' values are supported";
                        return;
                    }
                    
                    break;
                }
                
                
                case LexicalUnit.SAC_RADIAN:
                case LexicalUnit.SAC_DEGREE: {
                    double degreeVal = params.getNumericValue();
                    degreeVal = (params.getLexicalUnitType() ==  LexicalUnit.SAC_RADIAN) ?
                            (degreeVal * 180.0/Math.PI) : degreeVal;

                    if (Math.abs(degreeVal) < 0.0001) {
                        gradientType = Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL;
                        reverse = true;  //bottom to top
                    } else if (Math.abs(degreeVal - 90) < 0.0001 ) {
                        gradientType = Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL;
                    } else if (Math.abs(degreeVal - 180) < 0.0001) {
                        gradientType = Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL;
                    } else if (Math.abs(degreeVal - 270) < 0.0001) {
                        gradientType = Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL;
                        reverse = true;
                    } else {
                        reason = "Only 0, 90, 180, and 270 degrees supported.";
                        return;
                    }
                    break;

                }
                default:
                    System.err.println("Expected degree.  Found lexical type" +param1.getLexicalUnitType());
                    System.err.println("Currently only degree and radian parameters supported for first arg of linear-gradient");
                    // We only support degree and radian first param right now.
                    return; 

            }

            ScaledUnit param2 = (ScaledUnit)param1.getNextLexicalUnit();
            if (param2 == null) {
                return;
            }
            
            if (param2.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                param2 = (ScaledUnit)param2.getNextLexicalUnit();
            }
            if (param2 == null) {
                return;
            }

            switch (param2.getLexicalUnitType()) {
                case LexicalUnit.SAC_IDENT:
                case LexicalUnit.SAC_FUNCTION:
                case LexicalUnit.SAC_RGBCOLOR:
                case LexicalUnit.SAC_ATTR:
                    if (reverse) {
                        endColor = getColorInt(param2);
                    } else {
                        startColor = getColorInt(param2);
                    }
                    alpha = getColorAlphaInt(param2);
                break;
                default:
                    System.err.println("Expected color for 2nd parameter of linear-gradient");
                    return;
            }
            
            ScaledUnit param3 = (ScaledUnit)param2.getNextLexicalUnit();
            if (param3 == null) {
                return;
            }
            if (param3.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                param3 = (ScaledUnit)param3.getNextLexicalUnit();
            }
            if (param3 == null) {
                return;
            }
            
            switch (param3.getLexicalUnitType()) {
                case LexicalUnit.SAC_IDENT:
                case LexicalUnit.SAC_FUNCTION:
                case LexicalUnit.SAC_RGBCOLOR:
                case LexicalUnit.SAC_ATTR:
                    if (reverse) {
                        startColor = getColorInt(param3);
                    } else {
                        endColor = getColorInt(param3);
                    }
                    int alpha2 = getColorAlphaInt(param3);
                    if (alpha2 != alpha) {
                        // Alphas don't match so the gradient can't be expressed using CN1 gradients.
                        reason = "alphas of start and end colors don't match";
                        return;
                    }
                    
                break;
                    
                    
                default: 
                    System.err.println("Error processing background rule "+background+" for selector "+currentId);
                    System.err.println("Expecting color for param 3 of linear-gradient but found "+param3+" of type "+param3.getLexicalUnitType());
                    System.err.println("This gradient will need to be generated as an image border, which will slow down CSS compilation.");
                    //new RuntimeException("Error processing background rule "+background).printStackTrace(System.err);
                    return;
            }
           
            
            this.startColor = startColor;
            this.endColor = endColor;
            this.type = gradientType;
            this.bgTransparency = (byte)alpha;
            this.valid = true;
            /*
            Object[] out = new Object[] {
                startColor,
                endColor,
                0f,
                0f,
                0f
            };
            
            */
            
        }
        
        Object[] getThemeBgGradient() {
            if (!valid) {
                return null;
            }
            return new Object[]{
                startColor,
                endColor,
                gradientX,
                gradientY,
                size
            };
        }
        
        byte getBgTransparency() {
            if (valid) {
                return 0;
            }
            return bgTransparency;
        }
        
    }
    
    //private int currentScreenWidth=1080;
    //private int currentScreenHeight=1920;
    
    /*
    LexicalUnit px(LexicalUnit val) {
        switch (val.getLexicalUnitType()) {
            case LexicalUnit.SAC_CENTIMETER:
            case LexicalUnit.SAC_MILLIMETER:
            case LexicalUnit.SAC_PIXEL:
                
        }
        return val;
    }
    */
    /*
    double px(double val) {
        switch (targetDensity) {
            
            case com.codename1.ui.Display.DENSITY_MEDIUM:
                return val;

            // Generate High Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_HIGH:
                return val*480.0/320.0;

            // Generate Very High Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_VERY_HIGH:
                return val*2.0;

            // Generate HD Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_HD:
                return val*1080.0/320.0;

            // Generate HD560 Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_560:
                return val*1500.0/320.0;

            // Generate HD2 Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_2HD:
                return val*2000.0/320.0;

            // Generate 4k Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_4K:
                return val*2500.0/320.0;
                
        }
        throw new RuntimeException("Unsupported density");
    }
    */
    public class FontFace {
        File fontFile;
        LexicalUnit fontFamily;
        LexicalUnit src;
        LexicalUnit fontWeight;
        LexicalUnit fontStretch;
        LexicalUnit fontStyle;
        
        URL getURL() {
            try {
                if (src == null) {
                    return null;
                }

                switch (src.getLexicalUnitType()) {
                    case LexicalUnit.SAC_URI:
                        String url = src.getStringValue();
                        if (url.startsWith("github://")) {
                            //url(https://raw.githubusercontent.com/google/fonts/master/ofl/sourcesanspro/SourceSansPro-Light.ttf);
                            url = "https://raw.githubusercontent.com/" + url.substring(9).replace("/blob/master/", "/master/");
                            return new URL(url);
                        }
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            return new URL(url);
                        } else {
                            return new URL(baseURL, url);
                        }
                }
                return null;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        
        File getFontFile() {
            if (fontFile == null) {
                try {
                    URL url = getURL();
                    if (url == null) {
                        return null;
                    }
                    File parentDir = cssFile.getParentFile();
                    if (url.getProtocol().startsWith("http")) {
                        // If it is remote, check so see if we've already downloaded
                        // the font to the current directory.
                        String fontName = java.net.URLDecoder.decode(url.getPath(), "UTF-8");
                        
                        if (fontName.indexOf("/") != -1) {
                            fontName = fontName.substring(fontName.lastIndexOf("/")+1);
                        }
                        File tmpFontFile = new File(parentDir, fontName);
                        if (tmpFontFile.exists()) {
                            fontFile = tmpFontFile;
                        } else {
                            InputStream is = url.openStream();
                            FileOutputStream fos = new FileOutputStream(tmpFontFile);
                            Util.copy(is, fos);
                            Util.cleanup(is);
                            Util.cleanup(fos);
                            fontFile = tmpFontFile;
                        }
                    } else if ("file".equals(url.getProtocol())){
                        fontFile = new File(url.toURI());
                        
                    }
                } catch (Exception ex) {
                    Logger.getLogger(CSSTheme.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
            }
            
            if (fontFile != null && resourceFile != null) {
                File srcDir = resourceFile.getParentFile();
                File deployFontFile = new File(srcDir, fontFile.getName());
                if (!deployFontFile.exists()) {
                    try (FileInputStream in = new FileInputStream(fontFile); FileOutputStream out = new FileOutputStream(deployFontFile)) {
                        Util.copy(in, out);
                        Util.cleanup(out);

                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        throw new RuntimeException(ioe);
                    }
                }
            }
            
            return fontFile;
        }
    }
    
    public FontFace createFontFace() {
        FontFace f = new FontFace();
        fontFaces.add(f);
        return f;
    }
    
    private static class ScaledUnit implements LexicalUnit {
        LexicalUnit src;
        double dpi=144;
        int screenWidth=640;
        int screenHeight=960;
        CN1Gradient gradient;

        LexicalUnit next, prev, param;
        
        
        ScaledUnit(LexicalUnit src, double dpi, int screenWidth, int screenHeight) {
            this.src = src;
            this.dpi = dpi;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            
        }





        public void setParameters(LexicalUnit params) {
            this.param = params;
        }

        private boolean nextLexicalUnitNull;
        public void setNextLexicalUnit(LexicalUnit next) {
            this.next = next;
            nextLexicalUnitNull = (next == null);
        }

        private boolean prevLexicalUnitNull;
        public void setPrevLexicalUnit(LexicalUnit prev) {

            this.prev = prev;
            prevLexicalUnitNull = (prev == null);

        }

        /**
         * If this lexical unit is a gradient function and can be expressed as a CN1 gradient
         * this will return that gradient.  Otherwise it will return null.
         * @return 
         */
        public CN1Gradient getCN1Gradient() {
            
            if (gradient == null && isGradient(src)) {
                            
                gradient = new CN1Gradient();
                gradient.parse(this);
                if (!gradient.valid && gradient.reason != null) {
                    System.err.println("Selector with id "+currentId+" Gradient not valid: "+gradient.reason);
                }
               
            }
            if (gradient != null && gradient.valid) {
                return gradient;
            }
            return null;
        }
        
        public boolean isCN1Gradient() {
            CN1Gradient g = getCN1Gradient();
            return (g != null && g.valid);
        }
        
        
        @Override
        public short getLexicalUnitType() {
            return src.getLexicalUnitType();
        }

        @Override
        public LexicalUnit getNextLexicalUnit() {
            if (next != null || nextLexicalUnitNull) return next;
            LexicalUnit nex = src.getNextLexicalUnit();

            LexicalUnit out = nex == null ? null : new ScaledUnit(nex, dpi, screenWidth, screenHeight);
            if (out != null) next = out;
            return out;
        }

        private static boolean hasCycle(LexicalUnit lu, ScaledUnit su) {
            if (lu == su) return true;
            if (su.src instanceof ScaledUnit) {
                return hasCycle(lu, (ScaledUnit)su.src);
            }
            return false;
        }
        
        public ScaledUnit getNextNumericUnit() {
            ScaledUnit nex = (ScaledUnit)getNextLexicalUnit();
            while (nex != null) {
                switch (nex.getLexicalUnitType()) {
                    case LexicalUnit.SAC_INTEGER:
                    case LexicalUnit.SAC_REAL:
                        return nex;
                }
                nex = (ScaledUnit)nex.getNextLexicalUnit();
            }
            return null;
        }

        @Override
        public LexicalUnit getPreviousLexicalUnit() {
            if (this.prev != null || prevLexicalUnitNull) return this.prev;
            LexicalUnit prev = src.getPreviousLexicalUnit();
            LexicalUnit out =  prev == null ? null : new ScaledUnit(prev, dpi, screenWidth, screenHeight);
            if (out != null) this.prev = out;
            return out;
        }

        @Override
        public int getIntegerValue() {
            return src.getIntegerValue();
        }

        @Override
        public float getFloatValue() {
            return src.getFloatValue();
        }
        
        public double getNumericValue() {
            switch (src.getLexicalUnitType()) {
                case LexicalUnit.SAC_INTEGER:
                    return src.getIntegerValue();
                default:
                    return src.getFloatValue();
            }
        }

        @Override
        public String getDimensionUnitText() {
            return src.getDimensionUnitText();
        }

        @Override
        public String getFunctionName() {
            return src.getFunctionName();
        }

        @Override
        public LexicalUnit getParameters() {
            if (this.param != null) return this.param;
            LexicalUnit param = src.getParameters();

            LexicalUnit out =  param == null ? null :  new ScaledUnit(param, dpi, screenWidth, screenHeight);
            if (out != null) this.param = out;
            return out;
        }

        String renderAsCSSValue(double targetDpi, int targetScreenWidth, int targetScreenHeight) {
            ScaledUnit lu = this;
                if (lu == null) {
                return "";
            }
            switch (lu.getLexicalUnitType()) {
                case LexicalUnit.SAC_MILLIMETER:
                case LexicalUnit.SAC_CENTIMETER:
                    return (lu.getFloatValue()*dpi/targetDpi)+lu.getDimensionUnitText();
                case LexicalUnit.SAC_POINT:
                    return (lu.getFloatValue()*dpi/targetDpi)+"px";
                case LexicalUnit.SAC_PIXEL:
                case LexicalUnit.SAC_PERCENTAGE:
                case LexicalUnit.SAC_DEGREE:
                    return lu.getFloatValue() + lu.getDimensionUnitText();
                case LexicalUnit.SAC_URI:
                    return "url("+lu.getStringValue()+")";

                case LexicalUnit.SAC_FUNCTION: {
                    StringBuilder sb = new StringBuilder();
                    String fname = lu.getFunctionName();
                    if ("cn1rgb".equals(fname)) {
                        fname = "rgb";
                    } else if ("cn1rgba".equals(fname)) {
                        fname = "rgba";
                    }
                    sb.append(fname).append("(");
                    ScaledUnit val = (ScaledUnit)lu.getParameters();
                    //sb.append(String.valueOf(val));
                    boolean empty = true;
                    while (val != null) {
                        empty = false;
                        sb.append(val.renderAsCSSValue(targetDpi, targetScreenWidth, targetScreenHeight)).append(" ");
                        val = (ScaledUnit)val.getNextLexicalUnit();
                    }
                    if (!empty) {
                        sb.setLength(sb.length()-1);
                    }
                    sb.append(")");
                    return sb.toString();
                }

                case LexicalUnit.SAC_OPERATOR_COMMA:
                    return ",";

                case LexicalUnit.SAC_OPERATOR_SLASH:
                    return "/";

                case LexicalUnit.SAC_IDENT:
                    return lu.getStringValue();

                case LexicalUnit.SAC_STRING_VALUE:
                    return lu.getStringValue();

                case LexicalUnit.SAC_RGBCOLOR:
                    StringBuilder sb = new StringBuilder();
                    sb.append("rgb(");
                    ScaledUnit val = (ScaledUnit)lu.getParameters();
                    while (val != null) {

                        sb.append(val.renderAsCSSValue(targetDpi, targetScreenWidth, targetScreenHeight));
                        val = (ScaledUnit)val.getNextLexicalUnit();
                    }
                    sb.append(")");
                    return sb.toString();
                case LexicalUnit.SAC_INTEGER:
                    return String.valueOf(lu.getIntegerValue());
                case LexicalUnit.SAC_REAL:
                    return String.valueOf(lu.getFloatValue());
                default:
                    String unitText = null;
                    try {
                        unitText = lu.getDimensionUnitText();

                    } catch (Exception ex) {
                        break;
                    }
                    if (unitText != null) {
                        if ("rem".equals(unitText)) {
                            return lu.getFloatValue()+"rem";
                        } else if ("vw".equals(unitText)) {
                            return lu.getFloatValue()+"vw";

                        } else if ("vh".equals(unitText)) {
                            return lu.getFloatValue()+"vh";

                        } else if ("vmin".equals(unitText)) {
                            return lu.getFloatValue()+"vmin";

                        } else if ("vmax".equals(unitText)) {
                            return lu.getFloatValue()+"vmax";

                        }
                    }




            }
            throw new RuntimeException("Unsupported lex unit type "+lu.getLexicalUnitType());
        }
        
        @Override
        public String getStringValue() {
            
            return src.getStringValue();
        }

        @Override
        public LexicalUnit getSubValues() {
            LexicalUnit sv = src.getSubValues();
            return sv == null ? null : new ScaledUnit(sv, dpi, screenWidth, screenHeight);
        }
        
        public int getPixelValue() {
            return getPixelValue(dpi, screenWidth, screenHeight);
        }
        
        public int getPixelValue(int baseWidth, int baseHeight) {
            return getPixelValue(dpi, baseWidth, baseHeight);
        }
        
        public int getPixelValue(double targetDpi) {
            return getPixelValue(targetDpi, screenWidth, screenHeight);
        }
        
        public float getMMValue(double targetDpi) {
            return getMMValue(targetDpi, screenWidth, screenHeight);
        }
        
        public float getMMValue() {
            return getMMValue(dpi);
        }
        public float getMMValue(double targetDpi, int baseWidth, int baseHeight) {
            switch (src.getLexicalUnitType()) {
                case LexicalUnit.SAC_POINT:
                    return (float)this.getNumericValue() / 72f * 25.4f;
                case LexicalUnit.SAC_PIXEL:
                    return (float)(this.getNumericValue() * 25.4/targetDpi);
                case LexicalUnit.SAC_MILLIMETER:
                    return (float)this.getNumericValue();
                case LexicalUnit.SAC_CENTIMETER:
                    return (float)this.getNumericValue() * 10;
                case LexicalUnit.SAC_INTEGER:
                case LexicalUnit.SAC_REAL:
                    return (float)(this.getNumericValue() * 25.4/targetDpi);
                case LexicalUnit.SAC_PERCENTAGE:
                    return (float)((((double)baseWidth) * this.getNumericValue() / 100.0) * 25.4/targetDpi);
                    
            }
            throw new RuntimeException("Cannot get mm value for type "+src);
        }
        
        public int getPixelValue(double targetDpi, int baseWidth, int baseHeight) {
            switch (src.getLexicalUnitType()) {
                case LexicalUnit.SAC_POINT:
                    return (int)(this.getNumericValue() * targetDpi / 160.0);
                case LexicalUnit.SAC_PIXEL:
                    return (int)this.getNumericValue();
                case LexicalUnit.SAC_MILLIMETER:
                    return (int)(this.getNumericValue() * targetDpi / 25.4);
                case LexicalUnit.SAC_CENTIMETER:
                    return (int)(this.getNumericValue() * targetDpi / 2.54);
                case LexicalUnit.SAC_INTEGER:
                case LexicalUnit.SAC_REAL:
                    return (int)this.getNumericValue();
                case LexicalUnit.SAC_PERCENTAGE:
                    return (int)(((double)baseWidth) * this.getNumericValue() / 100.0);
                    
            }
            throw new RuntimeException("Cannot get pixel value for type "+src);
        }

        @Override
        public String toString() {

            return src.toString();
        }
        
        
        
        
    }
    
    private class PixelUnit implements LexicalUnit {

        float val;
        
        PixelUnit(float val) {
            this.val = val;
        }
        
        @Override
        public short getLexicalUnitType() {
            return LexicalUnit.SAC_PIXEL;
        }

        @Override
        public LexicalUnit getNextLexicalUnit() {
            return null;
        }

        @Override
        public LexicalUnit getPreviousLexicalUnit() {
            return null;
        }

        @Override
        public int getIntegerValue() {
            return (int)val;
        }

        @Override
        public float getFloatValue() {
            return val;
        }

        @Override
        public String getDimensionUnitText() {
            return "px";
        }

        @Override
        public String getFunctionName() {
            return null;
        }

        @Override
        public LexicalUnit getParameters() {
            return null;
        }

        @Override
        public String getStringValue() {
            return null;
        }

        @Override
        public LexicalUnit getSubValues() {
            return null;
        }
        
    }
    
    
    String renderAsCSSString(LexicalUnit lu) {
        if (lu == null) {
            return "none";
        }
        return ((ScaledUnit)lu).renderAsCSSValue(160, 640, 960);
    }
    
    
    
    String renderCSSProperty(String property, Map<String, LexicalUnit> styles) {
        if (property.contains("padding") || property.contains("margin")) {
            return "";
        }
        if ("opacity".equals(property)) {
            // We don't render opacity.  We let CN1 handle this using the opacity
            // style property.
            return "";
        }
        if (property.startsWith("cn1-")) {
            switch (property) {
                case "cn1-border-bottom-left-radius-x":
                    return "border-radius: "+renderAsCSSString(styles.get("cn1-border-top-left-radius-x")) + " " +
                            renderAsCSSString(styles.get("cn1-border-top-right-radius-x")) + " " +
                            renderAsCSSString(styles.get("cn1-border-bottom-right-radius-x")) + " " +
                            renderAsCSSString(styles.get("cn1-border-bottom-left-radius-x")) + " / " +
                            renderAsCSSString(styles.get("cn1-border-top-left-radius-y")) + " " +
                            renderAsCSSString(styles.get("cn1-border-top-right-radius-y")) + " " +
                            renderAsCSSString(styles.get("cn1-border-bottom-right-radius-y")) + " " +
                            renderAsCSSString(styles.get("cn1-border-bottom-left-radius-y"));
                           
                case "cn1-box-shadow-h" :
                    LexicalUnit h = styles.get("cn1-box-shadow-h");
                    if (h == null) {
                        return "";
                    }
                    if ("none".equals(h.getStringValue())) {
                        return "box-shadow: none";
                    } else {
                        return "box-shadow: " +
                                renderAsCSSString(styles.get("cn1-box-shadow-h")) + " " +
                                renderAsCSSString(styles.get("cn1-box-shadow-v")) + " " +
                                (((h = styles.get("cn1-box-shadow-blur")) != null) ? (renderAsCSSString(h) + " ") : "") +
                                (((h = styles.get("cn1-box-shadow-spread")) != null) ? (renderAsCSSString(h) + " ") : "") +
                                (((h = styles.get("cn1-box-shadow-color")) != null) ? (renderAsCSSString(h) + " ") : "") +
                                (((h = styles.get("cn1-box-shadow-inset")) != null && "inset".equals(h.getStringValue())) ? (renderAsCSSString(h) + " ") : "");
                                
                                
                    }
            }
            return "";
        } else {
            
            switch (property) {
                case "width": {
                    LexicalUnit value = styles.get(property);
                    
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_PERCENTAGE:
                            return property + ":"+ (int)(value.getFloatValue() / 100f * 640f) + "px";
                    }
                    break;
                }
                case "height": {
                    LexicalUnit value = styles.get(property);
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_PERCENTAGE:
                            return property + ":"+ (int)(value.getFloatValue() / 100f * 960f) + "px";
                    }
                    break;
                }
                
                case "border-image-slice": {
                    StringBuilder sb = new StringBuilder();
                    sb.append("border-image-slice: ");
                    LexicalUnit value = styles.get(property);
                    boolean first = true;
                    while (value != null) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" ");
                        }
                        switch (value.getLexicalUnitType()) {
                            case LexicalUnit.SAC_PERCENTAGE:
                                sb.append(value.getFloatValue()+"%");
                                break;
                            case LexicalUnit.SAC_PIXEL:
                                sb.append(value.getFloatValue()+"px");
                        }
                        value = value.getNextLexicalUnit();
                    }
                    if (first) {
                        sb.append("none");
                    }
                    return sb.toString();
                }
            }
            
            return property + ":"+renderAsCSSString(styles.get(property));
        }
    }
    
    public String getHtmlPreview() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html>\n<html><body>");
        for (String name : elements.keySet()) {
            Element el = (Element)elements.get(name);
            sb.append("<h1>").append(name).append("</h1>")
                    .append(el.getHtmlPreview())
                    .append("<h2>::Unselected</h2>")
                    .append(el.getUnselected().getHtmlPreview())
                    .append("<h2>::Selected</h2>")
                    .append(el.getSelected().getHtmlPreview())
                    .append("<h2>::Pressed</h2>")
                    .append(el.getPressed().getHtmlPreview())
                    .append("<h2>::Disabled</h2>")
                    .append(el.getDisabled().getHtmlPreview())
                    .append("<hr/>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
    
    public boolean requiresCaptureHtml() {
        for (String name : elements.keySet()) {
            if (!isModified(name)) {
                continue;
            }
            
            
            Element el = (Element)elements.get(name);
            Map unselectedStyle = el.getUnselected().getFlattenedStyle();
            if (el.requiresBackgroundImageGeneration(unselectedStyle) || el.requiresImageBorder(unselectedStyle)) {
                return true;
            }
            Map selectedStyle = el.getSelected().getFlattenedStyle();
            if (el.requiresBackgroundImageGeneration(selectedStyle) || el.requiresImageBorder(selectedStyle)) {
                return true;
            }
            Map pressedStyle = el.getPressed().getFlattenedStyle();   
            if (el.requiresBackgroundImageGeneration(pressedStyle) || el.requiresImageBorder(pressedStyle)) {
                return true;
            }
            Map disabledStyle = el.getDisabled().getFlattenedStyle();
            if (el.requiresBackgroundImageGeneration(disabledStyle) || el.requiresImageBorder(disabledStyle)) {
                return true;
            }
                    
        }
        return false;
    }
    
    public String generateCaptureHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html>\n<html>"
                + "<head><style type=\"text/css\">* {background-color: transparent;} "
                + "body {padding:0; margin:0} "
                + "div.element {margin: 0 !important; padding: 0 !important; }"
                + "</style></head><body>");
        for (String name : elements.keySet()) {
            if (!isModified(name)) {
                continue;
            }
            
            
            Element el = (Element)elements.get(name);
            Map unselectedStyle = el.getUnselected().getFlattenedStyle();
            if (el.requiresBackgroundImageGeneration(unselectedStyle) || el.requiresImageBorder(unselectedStyle)) {
                sb.append(el.getUnselected().getEmptyHtmlWithId(name, unselectedStyle));
            }
            Map selectedStyle = el.getSelected().getFlattenedStyle();
            if (el.requiresBackgroundImageGeneration(selectedStyle) || el.requiresImageBorder(selectedStyle)) {
                sb.append(el.getSelected().getEmptyHtmlWithId(name+".sel", selectedStyle));
            }
            Map pressedStyle = el.getPressed().getFlattenedStyle();   
            if (el.requiresBackgroundImageGeneration(pressedStyle) || el.requiresImageBorder(pressedStyle)) {
                sb.append(el.getPressed().getEmptyHtmlWithId(name+".press", pressedStyle));
            }
            Map disabledStyle = el.getDisabled().getFlattenedStyle();
            if (el.requiresBackgroundImageGeneration(disabledStyle) || el.requiresImageBorder(disabledStyle)) {
                sb.append(el.getDisabled().getEmptyHtmlWithId(name+".dis", disabledStyle));
            }
                    
        }
        sb.append("</body></html>");
        return sb.toString();
    }
    
    public Map<String,String> calculateSelectorChecksums() {
        Map<String,String> out = new LinkedHashMap<String,String>();
        for (String id : elements.keySet()) {
            Element el = elements.get(id);
            
            out.put(id, el.getChecksum());
        }
       
        return out;
    }
    
    public void saveSelectorChecksums(File output) throws FileNotFoundException, IOException {
        
        try (ObjectOutputStream fos = new ObjectOutputStream(new FileOutputStream(output))) {
            fos.writeObject(calculateSelectorChecksums());
        }
    }
    
    public Map<String, String> loadSelectorChecksums(File input) throws FileNotFoundException, IOException, ClassNotFoundException {
        Map<String,String> out;
        try (ObjectInputStream fis = new ObjectInputStream(new FileInputStream(input))) {
            out = (Map<String,String>)fis.readObject();
        }
        
        
        return out;
        
    }
    
    public static enum CacheStatus {
        UNCHANGED,
        MODIFIED,
        DELETED,
        ADDED
    }
    
    Map<String,CacheStatus> selectorCacheStatus;
    File selectorCacheFile;
    
    public void loadSelectorCacheStatus(File cachedFile) throws IOException {
        if (!cachedFile.equals(selectorCacheFile)) {
            selectorCacheStatus = calculateSelectorCacheStatus(cachedFile);
            selectorCacheFile = cachedFile;
        }
        
    }
    
    public Map<String,CacheStatus> getCacheStatus(File cachedFile) throws IOException {
        loadSelectorCacheStatus(cachedFile);
        return selectorCacheStatus;
    }
    
    /**
     * Checks if the given element ID has been modified since the last cache
     * @param id
     * @return 
     */
    public boolean isModified(String id) {
        if (selectorCacheStatus != null) {
            CacheStatus status = selectorCacheStatus.get(id);
            if (status == null) {
                return true;
            }
            switch (status) {
                case MODIFIED:
                case ADDED:
                    return true;
            }
            Element el = elements.get(id);
            String derive = el.getThemeDerive(el.getFlattenedStyle(), "");
            String unselectedDerive = el.getThemeDerive(el.getFlattenedUnselectedStyle(), "");
            String selectedDerive = el.getThemeDerive(el.getFlattenedSelectedStyle(), "");
            String pressedDerive = el.getThemeDerive(el.getFlattenedPressedStyle(), "");
            String disabledDerive = el.getThemeDerive(el.getFlattenedDisabledStyle(), "");
            if (derive != null && selectorCacheStatus.containsKey(derive) &&  isModified(derive)) {
                
                return true;
            }
            if (unselectedDerive != null && selectorCacheStatus.containsKey(unselectedDerive) && isModified(unselectedDerive)) {
                
                return true;
            }
            if (selectedDerive != null && selectorCacheStatus.containsKey(selectedDerive) && isModified(selectedDerive)) {
                return true;
            }
            if (pressedDerive != null && selectorCacheStatus.containsKey(pressedDerive) && isModified(pressedDerive)) {
                return true;
            }
            if (disabledDerive != null && selectorCacheStatus.containsKey(disabledDerive) && isModified(disabledDerive)) {
                return true;
            }
            
            return false;
        }
        return true;
    }
    
    public Set<String> getDeletedElements() {
        HashSet<String> out = new HashSet<String>();
        
        if (selectorCacheStatus != null) {
            for (String id : selectorCacheStatus.keySet()) {
                if (CacheStatus.DELETED.equals(selectorCacheStatus.get(id))) {
                    out.add(id);
                }
            }
        }
        return out;
    }
    

    
    private static String str(LexicalUnit lu, String defaultVal) {
        if (lu == null) {
            return defaultVal;
        }
        ScaledUnit su = (ScaledUnit)lu;
        double numVal = su.getNumericValue();
        String num = numVal+"";
        if (Math.ceil(numVal) == Math.floor(numVal)) {
            num = ((int)numVal)+"";
        }
        String unitText = "";
        try {
            unitText = lu.getDimensionUnitText();
        } catch (Exception ex){} // This might throw an exception if there was no unit given, but we don't care.
        switch (lu.getLexicalUnitType()) {
            case LexicalUnit.SAC_MILLIMETER:
                return num+"mm";

            case LexicalUnit.SAC_INTEGER:
            case LexicalUnit.SAC_REAL:
                return num+"";
            case LexicalUnit.SAC_POINT:
                return num+"pt";

            case LexicalUnit.SAC_PIXEL:
                return ((int)Math.round(su.getNumericValue()))+"px";


            case LexicalUnit.SAC_INCH:
                return num+"in";
            case LexicalUnit.SAC_EM:
                return num+"em";


        }
        return num+unitText;
    }
    
    public Map<String, CacheStatus> calculateSelectorCacheStatus(File cachedFile) throws IOException {
        try {
            Map<String,String> current = calculateSelectorChecksums();
            Map<String,String> previous = loadSelectorChecksums(cachedFile);
            HashMap<String, CacheStatus> out = new LinkedHashMap<String, CacheStatus>();
            if (previous == null) {
                for (String id : current.keySet()) {
                    out.put(id, CacheStatus.ADDED);
                }
                return out;
            } else {
                for (String id : current.keySet()) {
                    if (!previous.containsKey(id)) {
                        out.put(id, CacheStatus.ADDED);
                    } else if (previous.get(id).equals(current.get(id))) {
                        out.put(id, CacheStatus.UNCHANGED);
                    } else {
                        out.put(id, CacheStatus.MODIFIED);
                    }
                }
                for (String id : previous.keySet()) {
                    if (!current.containsKey(id)) {
                        out.put(id, CacheStatus.DELETED);
                    }
                }
                return out;
            }
        } catch (ClassNotFoundException ex) {
            
            throw new RuntimeException(ex);
        }
        
    }
    
    public void updateResources() {
        // TODO:  We need to remove stale theme entries
        // https://github.com/codenameone/CodenameOne/issues/2698
        
        for (String id : elements.keySet()) {
            if (!isModified(id)) {
                continue;
            }
            String currToken = "";
            try {
                Element el = elements.get(id);
                Map<String,LexicalUnit> unselectedStyles = el.getUnselected().getFlattenedStyle();
                Map<String,LexicalUnit> selectedStyles = el.getSelected().getFlattenedStyle();
                Map<String,LexicalUnit> pressedStyles  = el.getPressed().getFlattenedStyle();
                Map<String,LexicalUnit> disabledStyles = el.getDisabled().getFlattenedStyle();

                Element selected = el.getSelected();
                String selId = id+".sel";
                String unselId = id;
                String pressedId = id+".press";
                String disabledId = id+".dis";
                currToken = "padding";
                res.setThemeProperty(themeName, unselId+".padding", el.getThemePadding(unselectedStyles));
                currToken = "padUnit";
                res.setThemeProperty(themeName, unselId+".padUnit", el.getThemePaddingUnit(unselectedStyles));
                currToken = "selected padding";
                res.setThemeProperty(themeName, selId+"#padding", el.getThemePadding(selectedStyles));
                currToken = "selected padUnit";
                res.setThemeProperty(themeName, selId+"#padUnit", el.getThemePaddingUnit(selectedStyles));
                currToken = "pressed padding";
                res.setThemeProperty(themeName, pressedId+"#padding", el.getThemePadding(pressedStyles));
                currToken = "pressed padUnit";
                res.setThemeProperty(themeName, pressedId+"#padUnit", el.getThemePaddingUnit(pressedStyles));
                currToken = "disabled padding";
                res.setThemeProperty(themeName, disabledId+"#padding", el.getThemePadding(disabledStyles));
                currToken = "disabled padUnit";
                res.setThemeProperty(themeName, disabledId+"#padUnit", el.getThemePaddingUnit(disabledStyles));

                currToken = "margin";
                res.setThemeProperty(themeName, unselId+".margin", el.getThemeMargin(unselectedStyles));
                currToken = "marUnit";
                res.setThemeProperty(themeName, unselId+".marUnit", el.getThemeMarginUnit(unselectedStyles));
                currToken = "selected margin";
                res.setThemeProperty(themeName, selId+"#margin", el.getThemeMargin(selectedStyles));
                currToken = "selected marUnit";
                res.setThemeProperty(themeName, selId+"#marUnit", el.getThemeMarginUnit(selectedStyles));
                currToken = "pressed margin";
                res.setThemeProperty(themeName, pressedId+"#margin", el.getThemeMargin(pressedStyles));
                currToken = "pressed marUnit";
                res.setThemeProperty(themeName, pressedId+"#marUnit", el.getThemeMarginUnit(pressedStyles));
                currToken = "disabled margin";
                res.setThemeProperty(themeName, disabledId+"#margin", el.getThemeMargin(disabledStyles));
                currToken = "disabled marUnit";
                res.setThemeProperty(themeName, disabledId+"#marUnit", el.getThemeMarginUnit(disabledStyles));

                currToken = "elevation";
                if (unselectedStyles.containsKey("elevation")) {
                    res.setThemeProperty(themeName, unselId + ".elevation", el.getThemeElevation(unselectedStyles));
                }
                currToken = "selected elevation";
                if (selectedStyles.containsKey("elevation")) {
                    res.setThemeProperty(themeName, selId + "#elevation", el.getThemeElevation(selectedStyles));
                }
                currToken = "pressed elevation";
                if (pressedStyles.containsKey("elevation")) {
                    res.setThemeProperty(themeName, pressedId + "#elevation", el.getThemeElevation(pressedStyles));
                }
                currToken = "disabled elevation";
                if (disabledStyles.containsKey("elevation")) {
                    res.setThemeProperty(themeName, disabledId + "#elevation", el.getThemeElevation(disabledStyles));
                }

                currToken = "iconGap";
                float gap = el.getThemeIconGap(unselectedStyles);
                if (gap < 0) {
                    res.setThemeProperty(themeName, unselId+".iconGap", null);
                    currToken = "selected iconGap";
                    res.setThemeProperty(themeName, selId+"#iconGap", null);
                    currToken = "pressed iconGap";
                    res.setThemeProperty(themeName, pressedId+"#iconGap", null);
                    currToken = "disabled iconGap";
                    res.setThemeProperty(themeName, disabledId+"#iconGap", null);

                    currToken = "iconGapUnit";
                    res.setThemeProperty(themeName, unselId+".iconGapUnit", null);
                    currToken = "selected iconGapUnit";
                    res.setThemeProperty(themeName, selId+"#iconGapUnit", null);
                    currToken = "pressed iconGapUnit";
                    res.setThemeProperty(themeName, pressedId+"#iconGapUnit", null);
                    currToken = "disabled iconGapUnit";
                    res.setThemeProperty(themeName, disabledId+"#iconGapUnit", null);
                } else {
                    res.setThemeProperty(themeName, unselId+".iconGap", gap);
                    currToken = "selected iconGap";
                    res.setThemeProperty(themeName, selId+"#iconGap", gap);
                    currToken = "pressed iconGap";
                    res.setThemeProperty(themeName, pressedId+"#iconGap", gap);
                    currToken = "disabled iconGap";
                    res.setThemeProperty(themeName, disabledId+"#iconGap", gap);

                    currToken = "iconGapUnit";
                    byte gapUnit = el.getThemeIconGapUnit(unselectedStyles);
                    res.setThemeProperty(themeName, unselId+".iconGapUnit", gapUnit);
                    currToken = "selected iconGapUnit";
                    res.setThemeProperty(themeName, selId+"#iconGapUnit", gapUnit);
                    currToken = "pressed iconGapUnit";
                    res.setThemeProperty(themeName, pressedId+"#iconGapUnit", gapUnit);
                    currToken = "disabled iconGapUnit";
                    res.setThemeProperty(themeName, disabledId+"#iconGapUnit", gapUnit);
                }


                currToken = "surface";
                if (unselectedStyles.containsKey("surface")) {
                    res.setThemeProperty(themeName, unselId + ".surface", el.getThemeSurface(unselectedStyles));
                }
                currToken = "selected surface";
                if (selectedStyles.containsKey("surface")) {
                    res.setThemeProperty(themeName, selId + "#surface", el.getThemeSurface(selectedStyles));
                }
                currToken = "pressed surface";
                if (pressedStyles.containsKey("surface")) {
                    res.setThemeProperty(themeName, pressedId + "#surface", el.getThemeSurface(pressedStyles));
                }
                currToken = "disabled surface";
                if (disabledStyles.containsKey("surface")) {
                    res.setThemeProperty(themeName, disabledId + "#surface", el.getThemeSurface(disabledStyles));
                }


                currToken = "fgColor";
                res.setThemeProperty(themeName, unselId+".fgColor", el.getThemeFgColor(unselectedStyles));
                currToken = "selected fgColor";
                res.setThemeProperty(themeName, selId+"#fgColor", el.getThemeFgColor(selectedStyles));
                currToken = "pressed fgColor";
                res.setThemeProperty(themeName, pressedId+"#fgColor", el.getThemeFgColor(pressedStyles));
                currToken = "disabled fgColor";
                res.setThemeProperty(themeName, disabledId+"#fgColor", el.getThemeFgColor(disabledStyles));

                currToken = "fgAlpha";
                res.setThemeProperty(themeName, unselId+".fgAlpha", el.getThemeFgAlpha(unselectedStyles));
                currToken = "selected fgAlpha";
                res.setThemeProperty(themeName, selId+"#fgAlpha", el.getThemeFgAlpha(selectedStyles));
                currToken = "pressed fgAlpha";
                res.setThemeProperty(themeName, pressedId+"#fgAlpha", el.getThemeFgAlpha(pressedStyles));
                currToken = "disabled fgAlpha";
                res.setThemeProperty(themeName, disabledId+"#fgAlpha", el.getThemeFgAlpha(disabledStyles));

                currToken = "bgColor";
                res.setThemeProperty(themeName, unselId+".bgColor", el.getThemeBgColor(unselectedStyles));
                currToken = "selected bgColor";
                res.setThemeProperty(themeName, selId+"#bgColor", el.getThemeBgColor(selectedStyles));
                currToken = "pressed bgColor";
                res.setThemeProperty(themeName, pressedId+"#bgColor", el.getThemeBgColor(pressedStyles));
                currToken = "disabled bgColor";
                res.setThemeProperty(themeName, disabledId+"#bgColor", el.getThemeBgColor(disabledStyles));

                currToken = "transparency";
                res.setThemeProperty(themeName, unselId+".transparency", el.getThemeTransparency(unselectedStyles));
                currToken = "selected transparency";
                res.setThemeProperty(themeName, selId+"#transparency", el.getThemeTransparency(selectedStyles));
                currToken = "pressed transparency";
                res.setThemeProperty(themeName, pressedId+"#transparency", el.getThemeTransparency(pressedStyles));
                currToken = "disabled transparency";
                res.setThemeProperty(themeName, disabledId+"#transparency", el.getThemeTransparency(disabledStyles));

                currToken = "align";
                res.setThemeProperty(themeName, unselId+".align", el.getThemeAlignment(unselectedStyles));
                currToken = "selected align";
                res.setThemeProperty(themeName, selId+"#align", el.getThemeAlignment(selectedStyles));
                currToken = "pressed align";
                res.setThemeProperty(themeName, pressedId+"#align", el.getThemeAlignment(pressedStyles));
                currToken = "disabled align";
                res.setThemeProperty(themeName, disabledId+"#align", el.getThemeAlignment(disabledStyles));

                currToken = "font";
                res.setThemeProperty(themeName, unselId+".font", el.getThemeFont(unselectedStyles));
                currToken = "selected font";
                res.setThemeProperty(themeName, selId+"#font", el.getThemeFont(selectedStyles));
                currToken = "pressed font";
                res.setThemeProperty(themeName, pressedId+"#font", el.getThemeFont(pressedStyles));
                currToken = "disabled font";
                res.setThemeProperty(themeName, disabledId+"#font", el.getThemeFont(disabledStyles));
                currToken = "textDecoration";
                res.setThemeProperty(themeName, unselId+".textDecoration", el.getThemeTextDecoration(unselectedStyles));
                currToken = "selected textDecoration";
                res.setThemeProperty(themeName, selId+"#textDecoration", el.getThemeTextDecoration(selectedStyles));
                currToken = "pressed textDecoration";
                res.setThemeProperty(themeName, pressedId+"#textDecoration", el.getThemeTextDecoration(pressedStyles));
                currToken = "disabled textDecoration";
                res.setThemeProperty(themeName, disabledId+"#textDecoration", el.getThemeTextDecoration(disabledStyles));
                currToken = "bgGradient";
                res.setThemeProperty(themeName, unselId+".bgGradient", el.getThemeBgGradient(unselectedStyles));
                currToken = "selected bgGradient";
                res.setThemeProperty(themeName, selId+"#bgGradient", el.getThemeBgGradient(selectedStyles));
                currToken = "pressed bgGradient";
                res.setThemeProperty(themeName, pressedId+"#bgGradient", el.getThemeBgGradient(pressedStyles));
                currToken = "disabled bgGradient";
                res.setThemeProperty(themeName, disabledId+"#bgGradient", el.getThemeBgGradient(disabledStyles));

                currToken = "bgType";
                res.setThemeProperty(themeName, unselId+".bgType", el.getThemeBgType(unselectedStyles));
                currToken = "selected bgType";
                res.setThemeProperty(themeName, selId+"#bgType", el.getThemeBgType(selectedStyles));
                currToken = "pressed bgType";
                res.setThemeProperty(themeName, pressedId+"#bgType", el.getThemeBgType(pressedStyles));
                currToken = "disabled bgType";
                res.setThemeProperty(themeName, disabledId+"#bgType", el.getThemeBgType(disabledStyles));
                currToken = "derive";
                res.setThemeProperty(themeName, unselId+".derive", el.getThemeDerive(unselectedStyles, ""));
                currToken = "selected derive";
                res.setThemeProperty(themeName, selId+"#derive", el.getThemeDerive(selectedStyles, ".sel"));
                currToken = "pressed derive";
                res.setThemeProperty(themeName, pressedId+"#derive", el.getThemeDerive(pressedStyles, ".press"));
                currToken = "disabled derive";
                res.setThemeProperty(themeName, disabledId+"#derive", el.getThemeDerive(disabledStyles, ".dis"));

                currToken = "opacity";
                res.setThemeProperty(themeName, unselId+".opacity", el.getThemeOpacity(unselectedStyles));
                currToken = "selected opacity";
                res.setThemeProperty(themeName, selId+"#opacity", el.getThemeOpacity(selectedStyles));
                currToken = "pressed opacity";
                res.setThemeProperty(themeName, pressedId+"#opacity", el.getThemeOpacity(pressedStyles));
                currToken = "disabled opacity";
                res.setThemeProperty(themeName, disabledId+"#opacity", el.getThemeOpacity(disabledStyles));

               
               currToken = "bgImage";
               if (el.hasBackgroundImage(unselectedStyles) && !el.requiresBackgroundImageGeneration(unselectedStyles) && !el.requiresImageBorder(unselectedStyles)) {
                   
                   Image[] imageId = getBackgroundImages(unselectedStyles);
                    if (imageId != null && imageId.length > 0) {

                        res.setThemeProperty(themeName, unselId+".bgImage", imageId[0]);
                    }
                }
               currToken = "selected bgImage";
                if (el.hasBackgroundImage(selectedStyles) && !el.requiresBackgroundImageGeneration(selectedStyles) && !el.requiresImageBorder(selectedStyles)) {
                    Image[] imageId = getBackgroundImages(selectedStyles);
                    if (imageId != null && imageId.length > 0) {

                        res.setThemeProperty(themeName, selId+"#bgImage", imageId[0]);
                    }
                }

                currToken = "pressed bgImage";
                if (el.hasBackgroundImage(pressedStyles) && !el.requiresBackgroundImageGeneration(pressedStyles) && !el.requiresImageBorder(pressedStyles)) {
                    Image[] imageId = getBackgroundImages(pressedStyles);
                    if (imageId != null && imageId.length > 0) {

                        res.setThemeProperty(themeName, pressedId+"#bgImage", imageId[0]);
                    }
                }
                currToken = "disabled bgImage";
                if (el.hasBackgroundImage(disabledStyles) && !el.requiresBackgroundImageGeneration(disabledStyles) && !el.requiresImageBorder(disabledStyles)) {
                    Image[] imageId = getBackgroundImages(disabledStyles);
                    if (imageId != null && imageId.length > 0) {

                        res.setThemeProperty(themeName, disabledId+"#bgImage", imageId[0]);
                    }
                }

                currToken = "border";
                if (!el.requiresImageBorder(unselectedStyles) && !el.requiresBackgroundImageGeneration(unselectedStyles)) {
                    res.setThemeProperty(themeName, unselId+".border", el.getThemeBorder(unselectedStyles));
                }
                currToken = "selected border";
                if (!el.requiresImageBorder(selectedStyles) && !el.requiresBackgroundImageGeneration(selectedStyles)) {
                    res.setThemeProperty(themeName, selId+"#border", el.getThemeBorder(selectedStyles));
                }
                currToken = "pressed border";
                if (!el.requiresImageBorder(pressedStyles) && !el.requiresBackgroundImageGeneration(pressedStyles)) {
                    res.setThemeProperty(themeName, pressedId+"#border", el.getThemeBorder(pressedStyles));
                }
                currToken = "disabled border";
                if (!el.requiresImageBorder(disabledStyles) && !el.requiresBackgroundImageGeneration(disabledStyles)) {
                    res.setThemeProperty(themeName, disabledId+"#border", el.getThemeBorder(disabledStyles));
                }

            
            } catch (RuntimeException t) {
                System.err.println("An error occurred while updating resources for UIID "+id+".  Processing property "+currToken);
                throw t;
            }
            
        }
        res.setThemeProperty(themeName, "@PopupDialogArrowBool", "false");
        for (String constantKey : constants.keySet()) {
            try {
                LexicalUnit lu = constants.get(constantKey);
                if (lu.getLexicalUnitType() == LexicalUnit.SAC_STRING_VALUE || lu.getLexicalUnitType() == LexicalUnit.SAC_IDENT) {
                    if (constantKey.endsWith("Image")) {
                        // We have an image
                        Image im = res.getImage(lu.getStringValue());
                        if (im == null) {
                            im = getResourceImage(lu.getStringValue());
                        }
                        if (im == null) {
                            System.err.println("Error processing file "+this.baseURL);
                            throw new RuntimeException("Failed to set constant value "+constantKey+" to value "+ lu.getStringValue()+" because no such image was found in the resource file");
                        }
                        res.setThemeProperty(themeName, "@"+constantKey, im);


                    } else {
                        res.setThemeProperty(themeName, "@"+constantKey, lu.getStringValue());
                    }
                } else if (lu.getLexicalUnitType() == LexicalUnit.SAC_INTEGER) {
                    res.setThemeProperty(themeName, "@"+constantKey, String.valueOf(((ScaledUnit)lu).getIntegerValue()));
                }
            } catch (RuntimeException t) {
                System.err.println("\nAn error occurred processing constant key "+constantKey);
                throw t;
            }
        }

        imagesMetadata.store(res);

        Map<String,Object> theme = res.getTheme(themeName);
        HashSet<String> keys = new HashSet<String>();
        keys.addAll(theme.keySet());
        Set<String> deletedIds = getDeletedElements();
        for (String key : keys) {
            if (key.startsWith("Default.")) {
                res.setThemeProperty(themeName, key.substring(key.indexOf(".")+1), theme.get(key));
            } else {
                for (String delId : deletedIds) {
                    if (key.startsWith(delId+".") || key.startsWith(delId+"#")) {
                        res.setThemeProperty(themeName, key, null);
                    }
                }
            }
        }
        
        
        // Get rid of unused images now
        deleteUnusedImages();
       
        
        
    }



    
    private Set<String> referencedImageNames;
    
    private boolean isImageReferencedInCSS(String imageName) {
        if (referencedImageNames == null) {
            referencedImageNames = new HashSet<String>();
            for (String id : elements.keySet()) {
                Element el = elements.get(id);
                for (Object o : el.style.values()) {
                    ScaledUnit su = (ScaledUnit)o;
                    while (su != null) {
                        switch (su.getLexicalUnitType()) {
                            case LexicalUnit.SAC_STRING_VALUE:
                                referencedImageNames.add(su.getStringValue());

                                break;
                            case LexicalUnit.SAC_URI: {
                                String sv = su.getStringValue();
                                if (sv.contains("/")) {
                                    sv = sv.substring(sv.lastIndexOf("/")+1);
                                }
                                referencedImageNames.add(sv);
                                break;
                            }

                        }
                        su = (ScaledUnit)su.getNextLexicalUnit();
                    }
                }
            }
        }
        return referencedImageNames.contains(imageName);
    }
    
    public void deleteUnusedImages() {
        Vector<String> images = new Vector<String>();
        for(String img : res.getImageResourceNames()) {
            if(!isInUse(img)) {
                images.add(img);
            }
        }
        
        for (String im : images) {
            res.remove(im);
        }
    }
    
    private boolean isInUse(String imageName) {
        if (isImageReferencedInCSS(imageName)) {
            return true;
        }
        Object multi = res.getResourceObject(imageName);
        if(multi instanceof EditableResources.MultiImage) {
            EditableResources.MultiImage m = (EditableResources.MultiImage)multi;
            for(com.codename1.ui.Image i : m.getInternalImages()) {
                if(isInUse(i)) {
                    return true;
                }
            }
            return false;
        }
        com.codename1.ui.Image resourceValue = res.getImage(imageName);
        return isInUse(resourceValue);
    }
    
    private boolean isInUse(com.codename1.ui.Image resourceValue) {
        for(String themeName : res.getThemeResourceNames()) {
            Hashtable theme = res.getTheme(themeName);
            if(theme.values().contains(resourceValue)) {
                return true;
            }
            // we need to check the existance of image borders to replace images there...
            for(Object v : theme.values()) {
                if(v instanceof com.codename1.ui.plaf.Border) {
                    com.codename1.ui.plaf.Border b = (com.codename1.ui.plaf.Border)v;
                    // BORDER_TYPE_IMAGE
                    if(Accessor.getType(b) == Accessor.TYPE_IMAGE || Accessor.getType(b) == Accessor.TYPE_IMAGE_HORIZONTAL ||
                            Accessor.getType(b) == Accessor.TYPE_IMAGE_VERTICAL || Accessor.getType(b) == Accessor.TYPE_IMAGE_SCALED) {
                        com.codename1.ui.Image[] images = Accessor.getImages(b);
                        for(int i = 0 ; i < images.length ; i++) {
                            if(images[i] == resourceValue) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        // check if a timeline is making use of said image and replace it
        for(String image : res.getImageResourceNames()) {
            com.codename1.ui.Image current = res.getImage(image);
            if(current instanceof com.codename1.ui.animations.Timeline) {
                com.codename1.ui.animations.Timeline time = (com.codename1.ui.animations.Timeline)current;
                for(int iter = 0 ; iter < time.getAnimationCount() ; iter++) {
                    com.codename1.ui.animations.AnimationObject o = time.getAnimation(iter);
                    if(AnimationAccessor.getImage(o) == resourceValue) {
                        return true;
                    }
                }
            }
        }

        

        return false;
    }
    
    private Map<String,Image> loadedImages = new LinkedHashMap<String,Image>();
    
    
    public int[] getDpi(EncodedImage im) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(im.getImageData()));
        Iterator it = ImageIO.getImageReaders(iis);
        if (!it.hasNext()) {
            System.err.println("No reader for this format");
            return null;
        }
        ImageReader reader = (ImageReader) it.next();
        reader.setInput(iis);

        IIOMetadata meta = reader.getImageMetadata(0);
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree("javax_imageio_1.0");
        NodeList nodes = root.getElementsByTagName("HorizontalPixelSize");
        int xDPI = -1;
        int yDPI = -1;
        if (nodes.getLength() > 0) {
            IIOMetadataNode dpcWidth = (IIOMetadataNode) nodes.item(0);
            NamedNodeMap nnm = dpcWidth.getAttributes();
            Node item = nnm.item(0);
            xDPI = Math.round(25.4f / Float.parseFloat(item.getNodeValue()) / 0.45f);
            
        } else {
        }
        if (nodes.getLength() > 0) {
            nodes = root.getElementsByTagName("VerticalPixelSize");
            IIOMetadataNode dpcHeight = (IIOMetadataNode) nodes.item(0);
            NamedNodeMap nnm = dpcHeight.getAttributes();
            Node item = nnm.item(0);
            yDPI = Math.round(25.4f / Float.parseFloat(item.getNodeValue()) / 0.45f);
        }

        return new int[]{xDPI, yDPI};
    }
    
    public int getDensityForDpi(double maxDpi) {
        if (maxDpi <= 120) {
            return com.codename1.ui.Display.DENSITY_LOW;
        }
        if (maxDpi <= 160) {
                return com.codename1.ui.Display.DENSITY_MEDIUM;
            } else if (maxDpi <= 320) {
                return com.codename1.ui.Display.DENSITY_VERY_HIGH;
            } else if (maxDpi <= 200) {
                return com.codename1.ui.Display.DENSITY_560;
            } else if (maxDpi <= 480) {
                return com.codename1.ui.Display.DENSITY_HD;
            } else {
                return com.codename1.ui.Display.DENSITY_2HD;
            }
    }
    
    public int getImageDensity(EncodedImage im) {
        try {
            int[] dpis = getDpi(im);
            int maxDpi = 160;
            if (dpis != null && dpis[0] > 0 && dpis[0] > maxDpi) {
                maxDpi = dpis[0];
            }
            if (dpis != null && dpis[1] > 0 && dpis[1] > maxDpi) {
                maxDpi = dpis[1];
            }
            
            return getDensityForDpi(maxDpi);
            
        } catch (Exception ex) {
            return com.codename1.ui.Display.DENSITY_MEDIUM;
        }
    }
    
    public Image[] getBackgroundImages(Map<String,LexicalUnit> styles) {
        ScaledUnit bgImage = (ScaledUnit)styles.get("background-image");
        List<Image> out = new ArrayList<Image>();
        while (bgImage != null) {
            Image im = getBackgroundImage(styles, bgImage);
            if (im != null) {
                out.add(im);
            }
            bgImage = (ScaledUnit)bgImage.getNextLexicalUnit();
            while (bgImage != null && bgImage.getLexicalUnitType() != LexicalUnit.SAC_URI) {
                bgImage = (ScaledUnit)bgImage.getNextLexicalUnit();
            }
        }
        return out.toArray(new Image[out.size()]);
    }
    
    public Image getBackgroundImage(Map<String,LexicalUnit> styles) {
        return getBackgroundImage(styles, (ScaledUnit)styles.get("background-image"));
    }
    
    public Image getResourceImage(String imageName) {
        if (new File(cssFile, "res").exists()) {
            File res = new File(cssFile, "res");
            
            File preferredThemeDir = new File(res, themeName);
            if (preferredThemeDir.exists()) {
                Image i = null;
                try {
                    i = getResourceImage(imageName, preferredThemeDir);
                    if (i != null) {
                        return i;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            
            for (File d : res.listFiles()) {
                if (d.isDirectory()) {
                   
                    Image i = null;
                    try {
                        i = getResourceImage(imageName, d);
                        if (i!=null) {
                            return i;
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                    }
                    
                }
            }
        
        }
        if (new File(cssFile.getParentFile(), "res").exists()) {
            File res = new File(cssFile.getParentFile(), "res");
            File preferredThemeDir = new File(res, themeName);
            if (preferredThemeDir.exists()) {
                Image i = null;
                try {
                    i = getResourceImage(imageName, preferredThemeDir);
                    if (i != null) {
                        return i;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            for (File d : res.listFiles()) {
                if (d.isDirectory()) {
                   
                    Image i = null;
                    try {
                        i = getResourceImage(imageName, d);
                        if (i!=null) {
                            return i;
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                    }
                    
                }
            }
        
        }
        
        if (new File(cssFile.getParentFile().getParentFile(), "res").exists()) {
            File res = new File(cssFile.getParentFile().getParentFile(), "res");
            File preferredThemeDir = new File(res, themeName);
            if (preferredThemeDir.exists()) {
                Image i = null;
                try {
                    i = getResourceImage(imageName, preferredThemeDir);
                    if (i != null) {
                        return i;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            for (File d : res.listFiles()) {
                if (d.isDirectory()) {
                   
                    Image i = null;
                    try {
                        i = getResourceImage(imageName, d);
                        if (i!=null) {
                            return i;
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                    }
                    
                }
            }
        
        }
        return null;
    }
    
    public Image getResourceImage(String imageName, File baseDir)  {
        
        try {
            if (res.containsResource(imageName)) {
                return res.getImage(imageName);
            } else {
                File imageFolder = new File(baseDir, imageName );
                if (imageFolder.exists()) {
                    EditableResources.MultiImage multi = new EditableResources.MultiImage();
                    ArrayList<Integer> dpis = new ArrayList<Integer>();
                    ArrayList<EncodedImage> encodedImages = new ArrayList<EncodedImage>();
                    
                    //File largestVersion = null;
                    //long largestSize = 0;
                    for (File f : imageFolder.listFiles()) {
                        int density  = Display.DENSITY_MEDIUM;
                        switch (f.getName()) {
                            case "2hd.png":
                                density = Display.DENSITY_2HD; break;
                            case "4k.png":
                                density = Display.DENSITY_4K; break;
                            case "560.png":
                                density = Display.DENSITY_560; break;
                            case "hd.png":
                                density = Display.DENSITY_HD;break;
                            case "high.png":
                                density = Display.DENSITY_HIGH; break;
                            case "low.png":
                                density = Display.DENSITY_LOW; break;
                            case "medium.png":
                                density = Display.DENSITY_MEDIUM; break;
                            case "veryhigh.png":
                                density = Display.DENSITY_VERY_HIGH; break;
                            case "verylow.png":
                                density = Display.DENSITY_VERY_LOW; break;


                        }
                        InputStream is = f.toURL().openStream();
                        EncodedImage encImg = EncodedImage.create(is);
                        is.close();
                        dpis.add(density);
                        encodedImages.add(encImg);
                        
                    }
                    int[] iDpis = new int[dpis.size()];
                    int ctr = 0;
                    for (Integer i : dpis) {
                        iDpis[ctr++] = i;
                    }
                    
                    multi.setDpi(iDpis);
                    multi.setInternalImages(encodedImages.toArray(new EncodedImage[encodedImages.size()]));
                    
                    res.setMultiImage(imageName, multi);
                    loadedImages.put(imageFolder.toURL().toString(), multi.getBest());
                    return multi.getBest();
                    
                }
            }
            
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }
    
    private boolean isFileURL(URL url) {
        return "file".equals(url.getProtocol()) && (url.getHost() == null || "".equals(url.getHost()));
    }
    
    public Image getBorderImage(Map<String,LexicalUnit> styles) {
        return getBackgroundImage(styles, (ScaledUnit)styles.get("border-image"));
    }
    
    
    
    public Image getBackgroundImage(Map<String,LexicalUnit> styles, ScaledUnit bgImage)  {
        try {
            //ScaledUnit bgImage = (ScaledUnit)styles.get("background-image");
            if (bgImage == null) {
                return null;
            }
            String url = bgImage.getStringValue();
            String fileName = url;
            if (fileName.indexOf("/") != -1) {
                fileName = fileName.substring(fileName.lastIndexOf("/")+1);
            }
            
            if (loadedImages.containsKey(url)) {
                return loadedImages.get(url);
            }
            
            LexicalUnit imageId = styles.get("cn1-image-id");
            String imageIdStr = fileName;
            
            if (imageId != null) {
                imageIdStr = imageId.getStringValue();
            } else {
                /*
                int i=1;
                while (res.getImage(imageIdStr) != null) {
                    
                    if (i == 1) {
                        imageIdStr += "_"+(++i);
                    } else {
                        imageIdStr = imageIdStr.substring(0, imageIdStr.lastIndexOf("_")) + "_"+(++i);
                    }
                }
                */
            }
            Image resimg = res.getImage(imageIdStr);
            Integer defaultSourceDpi = null;
            if (constants.containsKey("defaultSourceDPIInt")) {
                Object v = constants.get("defaultSourceDPIInt");
                if (v instanceof String) {
                    defaultSourceDpi = Integer.parseInt((String)v);

                } else if (v instanceof Number) {
                    defaultSourceDpi = ((Number) v).intValue();
                } else if (v instanceof ScaledUnit) {
                    ScaledUnit su = (ScaledUnit)v;
                    defaultSourceDpi = su.getIntegerValue();

                } else {
                    throw new IllegalArgumentException("defaultSourceDPIInt constant should be a String or a number but found "+v.getClass());
                }
            }
            if (resimg != null) {
                ImageMetadata md = imagesMetadata.get(imageIdStr);
                int srcDpi = (int)currentDpi;
                if (defaultSourceDpi != null) {
                    srcDpi = defaultSourceDpi;
                }

                if (styles.containsKey("cn1-source-dpi")) {
                
                    srcDpi  = (int)((ScaledUnit)styles.get("cn1-source-dpi")).getNumericValue();
                }
                if (refreshImages || (md != null && md.sourceDpi != srcDpi)) {
                    //
                    res.remove(imageIdStr);
                } else {
                    
                    loadedImages.put(imageIdStr, resimg);
                    return res.getImage(imageIdStr);
                }
            }
            
            
            URL imgURL = null;
            if (url.startsWith("http://") || url.startsWith("https://")) {
                imgURL = new URL(url);
            } else {
                imgURL = new URL(baseURL, url);
            }
            
            if (false && isFileURL(imgURL)) {
                // This section is switched off because loading multi-images via url() 
                // will cause unexpected results in cases where image borders are generated.
                // In order for this approach to work, we need take into account multi-images when
                // producing snapshots in the webview so that the correct size of image is used.
                // You can still load multi-images as theme constants.
                // See https://github.com/codenameone/CodenameOne/issues/2569#issuecomment-426730539
                File imgDir = new File(imgURL.toURI());
                if (imgDir.isDirectory()) {
                    try {
                        Image im = getResourceImage(imgDir.getName(), imgDir.getParentFile());
                        if (im != null) {
                            loadedImages.put(url, im);
                            return im;
                        }
                    } catch (Throwable t) {
                        System.err.println("Failed to load Multi-image from "+imgURL);
                        t.printStackTrace();
                        throw t;
                    }
                    
                }
            }
            
            InputStream is = imgURL.openStream();
            EncodedImage encImg = EncodedImage.create(is);
            
            is.close();
            
            
            ResourcesMutator resm = new ResourcesMutator(res, com.codename1.ui.Display.DENSITY_VERY_HIGH, minDpi, maxDpi);
            int[] dpis = getDpi(encImg);
            int sourceDpi = (int)Math.round(currentDpi);
            
            
            if (styles.containsKey("cn1-source-dpi")) {
                double densityVal = ((ScaledUnit) styles.get("cn1-source-dpi")).getNumericValue();
                sourceDpi = (int) Math.round(densityVal);
                if (Math.abs(densityVal) < 0.5) {
                    resm.targetDensity = 0;
                } else {
                    resm.targetDensity = getDensityForDpi(densityVal);
                }
            } else if (defaultSourceDpi != null) {
                sourceDpi = defaultSourceDpi;
                if (Math.abs(sourceDpi) < 0.5) {
                    resm.targetDensity = 0;
                } else {
                    resm.targetDensity = getDensityForDpi(sourceDpi);
                }
            } else if (dpis[0] > 0) {
                resm.targetDensity = getImageDensity(encImg);
            } else {
                
                resm.targetDensity = getDensityForDpi(bgImage.dpi);
            }
            
            if (styles.containsKey("cn1-densities")) {
                ScaledUnit densities = (ScaledUnit)styles.get("cn1-densities");
                if (densities.getLexicalUnitType() == LexicalUnit.SAC_IDENT && "none".equals(densities.getStringValue())) {
                    // Not a multi-image
                    resm.setMultiImage(false);
                    
                }
            } else if (sourceDpi == 0) {
                resm.setMultiImage(false);
            }
            
            
            

            Image im = resm.storeImage(encImg, imageIdStr, false);
            im.setImageName(imageIdStr);
            loadedImages.put(url, im);
            ImageMetadata md = new ImageMetadata(imageIdStr, sourceDpi);
            imagesMetadata.addImageMetadata(md);
            
            
            return im;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public int getSourceDensity(Map<String,LexicalUnit> style, int defaultValue) {
        if (style.containsKey("cn1-source-dpi")) {
            return getDensityForDpi(((ScaledUnit)style.get("cn1-source-dpi")).getNumericValue());
        } else {
            return defaultValue;
        }
    }
    
    public void loadResourceFile() throws IOException {
        if ( resourceFile != null && resourceFile.exists()) {
            if (res == null) {
                res = new EditableResourcesForCSS(resourceFile);
            }
            try {
                res.openFile(new FileInputStream(resourceFile));
            } catch (IOException ex) {
                System.err.println("Failed to load resource file from "+resourceFile);
                throw ex;
            }
            imagesMetadata.load(res);
        }
    }
    
    public static interface WebViewProvider {
        com.codename1.ui.BrowserComponent getWebView();
    }
    private static String currentId;
    public void createImageBorders(WebViewProvider webviewProvider) {
        if (res == null) {
            res = new EditableResourcesForCSS(resourceFile);
        }
        ArrayList<Border> borders = new ArrayList<Border>();
        
        ResourcesMutator resm = new ResourcesMutator(res, Display.DENSITY_VERY_HIGH, minDpi, maxDpi);
        resm.targetDensity = targetDensity;
        
        
        List<Runnable> onComplete = new ArrayList<Runnable>();
        for (String id : elements.keySet()) {
            try {
                if (!isModified(id)) {
                    continue;
                }
                Element e = (Element) elements.get(id);

                Element unselected = e.getUnselected();
                Map<String, LexicalUnit> unselectedStyles = (Map<String, LexicalUnit>) unselected.getFlattenedStyle();
                Border b = unselected.createBorder(unselectedStyles);
                Border unselectedBorder = b;
                currentId = id;
                if (e.requiresImageBorder(unselectedStyles)) {
                    if (!borders.contains(b)) {
                        borders.add(b);
                        resm.addImageProcessor(id, (img) -> {

                            Insets insets = unselected.getImageBorderInsets(unselectedStyles, img.getWidth(), img.getHeight());
                            resm.targetDensity = getSourceDensity(unselectedStyles, resm.targetDensity);
                            com.codename1.ui.plaf.Border border = resm.create9PieceBorder(img, id, (int) insets.top, (int) insets.right, (int) insets.bottom, (int) insets.left);
                            resm.put(id + ".border", border);
                            unselectedBorder.border = border;
                            resm.targetDensity = targetDensity;
                        });
                    } else {
                        onComplete.add(() -> {
                            resm.put(id + ".border", borders.get(borders.indexOf(unselectedBorder)).border);
                        });

                    }
                } else if (e.requiresBackgroundImageGeneration(unselectedStyles)) {
                    if (!borders.contains(b)) {
                        borders.add(b);
                        resm.addImageProcessor(id, (img) -> {
                            int i = 1;
                            while (res.containsResource(id + "_" + i + ".png")) {
                                i++;
                            }
                            String prefix = id + "_" + i + ".png";
                            resm.targetDensity = getSourceDensity(unselectedStyles, resm.targetDensity);
                            Image im = resm.storeImage(EncodedImage.create(ResourcesMutator.toPngOrJpeg(img)), prefix, false);
                            unselectedBorder.image = im;
                            resm.put(id + ".bgImage", im);
                            resm.targetDensity = targetDensity;
                            //resm.put(id+".press#bgType", Style.B)
                        });
                    } else {
                        onComplete.add(() -> {
                            resm.put(id + ".bgImage", unselectedBorder.image);
                        });
                    }
                }

                Element selected = e.getSelected();
                Map<String, LexicalUnit> selectedStyles = (Map<String, LexicalUnit>) selected.getFlattenedStyle();
                b = selected.createBorder(selectedStyles);
                Border selectedBorder = b;
                if (e.requiresImageBorder(selectedStyles)) {
                    if (!borders.contains(b)) {
                        borders.add(b);
                        resm.addImageProcessor(id + ".sel", (img) -> {
                            Insets insets = selected.getImageBorderInsets(selectedStyles, img.getWidth(), img.getHeight());
                            resm.targetDensity = getSourceDensity(selectedStyles, resm.targetDensity);
                            com.codename1.ui.plaf.Border border = resm.create9PieceBorder(img, id, (int) insets.top, (int) insets.right, (int) insets.bottom, (int) insets.left);
                            resm.put(id + ".sel#border", border);
                            selectedBorder.border = border;
                            resm.targetDensity = targetDensity;
                        });
                    } else {
                        onComplete.add(() -> {
                            resm.put(id + ".sel#border", borders.get(borders.indexOf(selectedBorder)).border);
                        });

                    }
                } else if (e.requiresBackgroundImageGeneration(selectedStyles)) {
                    if (!borders.contains(b)) {
                        borders.add(b);
                        resm.addImageProcessor(id + ".sel", (img) -> {
                            int i = 1;
                            while (res.containsResource(id + "_" + i + ".png")) {
                                i++;
                            }
                            String prefix = id + "_" + i + ".png";

                            resm.targetDensity = getSourceDensity(selectedStyles, resm.targetDensity);
                            Image im = resm.storeImage(EncodedImage.create(ResourcesMutator.toPngOrJpeg(img)), prefix, false);
                            selectedBorder.image = im;
                            resm.put(id + ".sel#bgImage", im);
                            //resm.put(id+".press#bgType", Style.B)
                            resm.targetDensity = targetDensity;
                        });
                    } else {
                        onComplete.add(() -> {
                            resm.put(id + ".sel#bgImage", selectedBorder.image);
                        });
                    }
                }

                Element pressed = e.getPressed();
                Map<String, LexicalUnit> pressedStyles = (Map<String, LexicalUnit>) pressed.getFlattenedStyle();

                b = pressed.createBorder(pressedStyles);
                Border pressedBorder = b;
                if (e.requiresImageBorder(pressedStyles)) {
                    if (!borders.contains(b)) {
                        borders.add(b);
                        resm.addImageProcessor(id + ".press", (img) -> {
                            Insets insets = pressed.getImageBorderInsets(pressedStyles, img.getWidth(), img.getHeight());

                            resm.targetDensity = getSourceDensity(pressedStyles, resm.targetDensity);
                            com.codename1.ui.plaf.Border border = resm.create9PieceBorder(img, id, (int) insets.top, (int) insets.right, (int) insets.bottom, (int) insets.left);

                            resm.put(id + ".press#border", border);
                            pressedBorder.border = border;
                            resm.targetDensity = targetDensity;
                        });
                    } else {
                        onComplete.add(() -> {
                            resm.put(id + ".press#border", borders.get(borders.indexOf(pressedBorder)).border);
                        });

                    }
                } else if (e.requiresBackgroundImageGeneration(pressedStyles)) {
                    if (!borders.contains(b)) {
                        borders.add(b);
                        resm.addImageProcessor(id + ".press", (img) -> {
                            int i = 1;
                            while (res.containsResource(id + "_" + i + ".png")) {
                                i++;
                            }
                            String prefix = id + "_" + i + ".png";
                            resm.targetDensity = getSourceDensity(pressedStyles, resm.targetDensity);
                            Image im = resm.storeImage(EncodedImage.create(ResourcesMutator.toPngOrJpeg(img)), prefix, false);
                            pressedBorder.imageId = prefix;
                            resm.put(id + ".press#bgImage", im/*res.findId(im, true)*/);
                            resm.targetDensity = targetDensity;
                            //resm.put(id+".press#bgType", Style.B)
                        });
                    } else {
                        onComplete.add(() -> {
                            resm.put(id + ".press#bgImage", res.findId(pressedBorder.imageId, true));
                        });
                    }
                }

                Element disabled = e.getDisabled();
                Map<String, LexicalUnit> disabledStyles = (Map<String, LexicalUnit>) disabled.getFlattenedStyle();

                b = disabled.createBorder(disabledStyles);
                Border disabledBorder = b;
                if (e.requiresImageBorder(disabledStyles)) {
                    if (!borders.contains(b)) {

                        borders.add(b);
                        resm.addImageProcessor(id + ".dis", (img) -> {
                            Insets disabledInsets = disabled.getImageBorderInsets(disabledStyles, img.getWidth(), img.getHeight());

                            resm.targetDensity = getSourceDensity(disabledStyles, resm.targetDensity);
                            com.codename1.ui.plaf.Border border = resm.create9PieceBorder(img, id, (int) disabledInsets.top, (int) disabledInsets.right, (int) disabledInsets.bottom, (int) disabledInsets.left);
                            disabledBorder.border = border;
                            resm.put(id + ".dis#border", border);
                            resm.targetDensity = targetDensity;
                        });
                    } else {
                        onComplete.add(() -> {
                            resm.put(id + ".dis#border", borders.get(borders.indexOf(disabledBorder)).border);
                        });

                    }
                } else if (e.requiresBackgroundImageGeneration(disabledStyles)) {
                    if (!borders.contains(b)) {
                        borders.add(b);
                        resm.addImageProcessor(id + ".dis", (img) -> {
                            int i = 1;
                            while (res.containsResource(id + "_" + i + ".png")) {
                                i++;
                            }
                            String prefix = id + "_" + i + ".png";
                            resm.targetDensity = getSourceDensity(disabledStyles, resm.targetDensity);
                            Image im = resm.storeImage(EncodedImage.create(ResourcesMutator.toPngOrJpeg(img)), prefix, false);
                            disabledBorder.image = im;
                            resm.put(id + ".dis#bgImage", im);
                            resm.targetDensity = targetDensity;
                            //resm.put(id+".press#bgType", Style.B)
                        });
                    } else {
                        onComplete.add(() -> {
                            resm.put(id + ".dis#bgImage", disabledBorder.image);
                        });
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("An exception occurred while processing the image border for element "+id, ex);
            }
        }
        
        if (requiresCaptureHtml()) {
            resm.createScreenshots(webviewProvider.getWebView(), generateCaptureHtml(), this.baseURL.toExternalForm());
        }
        for (Runnable r : onComplete) {
            r.run();
        }
        
    }
    
    public void save(File outputFile) throws IOException {
         DataOutputStream resFile = new DataOutputStream(new FileOutputStream(outputFile));
         res.save(resFile);
         resFile.close();
    }
    
    
    
    private class Border {
        com.codename1.ui.plaf.Border border;
        String imageId;
        Image image;
        
        String thicknessTop,
                thicknessRight,
                thicknessBottom,
                thicknessLeft,
                styleTop,
                styleRight,
                styleBottom,
                styleLeft;
                
        
        String backgroundImageUrl,
                backgroundRepeat,
                borderRadius,
                boxShadow,
                gradient;
        
        String backgroundColor,
                borderColorTop,
                borderColorRight,
                borderColorBottom,
                borderColorLeft;
        
        String borderImage,
                borderImageSlice;
               
        
        boolean isStyleNativelySupported() {
            return this.styleTop == null || isBorderTypeNativelySupported(this.styleTop);
        }
        
        boolean isBorderLineOrNone() {
            return styleTop == null || "none".equals(styleTop) || "line".equals(styleTop) || "solid".equals(styleTop);
        }
        
        public boolean canBeAchievedWithUnderlineBorder(Map<String,LexicalUnit> styles) {
            if (this.hasGradient() || !isBorderLineOrNone() || !isNone(backgroundImageUrl) || hasBoxShadow() || hasBorderImage()) {
                return false;
            }
            ScaledUnit topThickness = (ScaledUnit)styles.get("border-top-width");
            ScaledUnit leftThickness = (ScaledUnit)styles.get("border-left-width");
            ScaledUnit rightThickness = (ScaledUnit)styles.get("border-right-width");
            ScaledUnit bottomThickness = (ScaledUnit)styles.get("border-bottom-width");
            ScaledUnit[] sideUnits = new ScaledUnit[]{topThickness, leftThickness, rightThickness};
            String[] sideStyles = new String[]{styleTop, styleLeft, styleRight};
            
            for (int i=0; i<3; i++) {
                ScaledUnit u = sideUnits[i];
                String s = sideStyles[i];
                if (u != null && u.getPixelValue() != 0 && !(s == null || "none".equals(s))) {
                    return false;
                }
            }
            
            if (bottomThickness != null && bottomThickness.getPixelValue() != 0 && ("line".equals(styleBottom) || "solid".equals(styleBottom))) {
                LexicalUnit color = styles.get("border-bottom-color");
                if (color != null && getColorAlphaInt(color) != 255) {
                    return false;
                }
                return true;
            }
            return false;
        }
        
        
       
        /**
         * Eventually we hope to support all borders with the CSSBorder class, but for now,
         * we are introducing its usage gradually to just cover the common cases.  One common
         * case is where there is a bottom border.
         * @param styles
         * @return 
         */
        public boolean canBeAchievedWithCSSBorder(Map<String,LexicalUnit> styles) {
            if (this.hasGradient() || !isNone(backgroundImageUrl) || hasBoxShadow()) {
                return false;
            }
            return true;
        }
        
        public boolean canBeAchievedWithRoundRectBorder(Map<String,LexicalUnit> styles) {
            if (hasUnequalBorders() || this.hasGradient() || !isBorderLineOrNone() || !isNone(backgroundImageUrl) || hasBoxShadow() || hasBorderImage()) {
                return false;
            }
            
            String prefix = "cn1-border";
            String[] corners = new String[]{"top-left", "top-right", "bottom-left", "bottom-right"};
            String[] xy = new String[]{"x", "y"};
            
            String[] radiusAtts = new String[8];
            int i =0;
            for (String axis : xy) {
                for (String corner : corners) {
                    radiusAtts[i++] = prefix+"-"+corner+"-radius-"+axis;
                }
            }
            
            ScaledUnit val = null;
            for (String cornerStyle : radiusAtts) {
                ScaledUnit u = (ScaledUnit)styles.get(cornerStyle);
                if (u != null && u.getPixelValue() != 0) {
                    if (val != null && val.getPixelValue() != u.getPixelValue()) {
                        // We have more than one non-zero corner radius
                        
                        return false;
                        
                    }
                    val = u;
                }
            }
            
            // All corners are the same, so we can proceed to the next step.
            
            prefix = "border";
            String[] sides = new String[]{"top", "right", "bottom", "left"};
            
            String[] widthAtts = new String[4];
            String[] colorAtts = new String[4];
            i=0;
            for (String side : sides) {
                widthAtts[i] = "border-"+side+"-width";
                colorAtts[i++] = "border-"+side+"-color";
            }
            
            boolean borderColorSet=false;
            boolean borderWidthSet=false;
            int borderWidth=0;
            int colorInt=0;
            int alphaInt=0;
            
            for (String widthAtt : widthAtts) {
                ScaledUnit uWidth = (ScaledUnit)styles.get(widthAtt);
                if (uWidth != null) {
                    if (borderWidthSet && uWidth.getPixelValue() != borderWidth) {
                        
                        return false;
                    }
                    borderWidthSet = true;
                    borderWidth = uWidth.getPixelValue();
                }
            }
            
            for (String colorAtt : colorAtts) {
                LexicalUnit uColor = styles.get(colorAtt);
                if (uColor != null) {
                    if (borderColorSet && (getColorInt(uColor) != colorInt || getColorAlphaInt(uColor) != alphaInt)) {
                        
                        return false;
                    } 
                    borderColorSet = true;
                    colorInt = getColorInt(uColor);
                    alphaInt = getColorAlphaInt(uColor);
                }
            }
            
            
            // We should be able to achieve this with a roundrect border
            return true;
            
            
            
            
        }
        
        
        public boolean equals(Object o) {
            Border b = (Border)o;
            return eq(borderRadius, b.borderRadius)
                    && eq(thicknessTop, b.thicknessTop)
                    && eq(thicknessRight, b.thicknessRight)
                    && eq(thicknessBottom, b.thicknessBottom)
                    && eq(thicknessLeft, b.thicknessLeft)
                    && eq(styleTop, b.styleTop)
                    && eq(styleRight, b.styleRight)
                    && eq(styleBottom, b.styleBottom)
                    && eq(styleLeft, b.styleLeft)
                    && eq(gradient, b.gradient)
                    && eq(boxShadow, b.boxShadow)
                    && (backgroundImageUrl == null ? b.backgroundImageUrl == null : backgroundImageUrl.equals(b.backgroundImageUrl))
                    && (backgroundRepeat == null ? b.backgroundRepeat == null : backgroundRepeat.equals(b.backgroundRepeat))
                    && eq(backgroundColor, b.backgroundColor)
                    && eq(borderColorTop, b.borderColorTop)
                    && eq(borderColorRight, b.borderColorRight)
                    && eq(borderColorBottom, b.borderColorBottom)
                    && eq(borderColorLeft, b.borderColorLeft)
                    && eq(borderImage, b.borderImage)
                    && eq(borderImageSlice, b.borderImageSlice);
            
        }
        /*
        public boolean requiresImageBorder() {
            return !eq(borderColorTop, borderColorRight)
                    || !eq(borderColorTop, borderColorBottom)
                    || !eq(borderColorTop, borderColorLeft)
                    || !isNone(gradient)
                    || !isNone(boxShadow)
                    || !isZero(borderRadius)
                    || !eq(thicknessTop, thicknessRight)
                    || !eq(thicknessTop, thicknessBottom)
                    || !eq(thicknessTop, thicknessLeft)
                    || !eq(styleTop, styleRight)
                    || !eq(styleTop, styleBottom)
                    || !eq(styleTop, styleLeft);
        }
        */
        public String toString() {
            return borderColorTop + " " +borderColorRight + borderColorBottom + borderColorLeft +
                    gradient + boxShadow + borderRadius + thicknessTop + thicknessRight + thicknessBottom + thicknessLeft
                    + styleTop + styleRight + styleBottom + styleLeft + borderImage + borderImageSlice;
        }
        
        public boolean hasBorderRadius() {
            String br = borderRadius;
            if (br != null) {
                if (br.indexOf(":") > 0) {
                    br = br.substring(br.indexOf(":")+1).trim();
                }
                
            }
            if (br == null || br.isEmpty()) {
                return false;
            }
            
            return !isZero(br);
        }
        
        public boolean hasGradient() {
            return !isNone(gradient);
        }
        
        public boolean hasUnequalBorders() {
            return !eq(thicknessTop, thicknessRight)
                    || !eq(thicknessTop, thicknessBottom)
                    || !eq(thicknessTop, thicknessLeft)
                    || !eq(styleTop, styleRight)
                    || !eq(styleTop, styleBottom)
                    || !eq(styleTop, styleLeft)
                    || !eq(borderColorTop, borderColorRight)
                    || !eq(borderColorTop, borderColorBottom)
                    || !eq(borderColorTop, borderColorLeft);
        }
        
        public boolean hasBoxShadow() {
            String bs = boxShadow;
            if (bs != null) {
                if (bs.indexOf(":") > 0) {
                    bs = bs.substring(bs.indexOf(":")+1).trim();
                }
            }
            return !isNone(bs);
        }
        
        public boolean hasBorderImage() {
            String bs = borderImage;
            if (bs != null) {
                if (bs.indexOf(":") > 0) {
                    bs = bs.substring(bs.indexOf(":")+1).trim();
                }
            }
            return !isNone(bs);
        }

        
    }
    
    
    
    private static boolean eq(Object o1, Object o2) {
        return o1 == null ? o1 == null : o1.equals(o2);
    }
    
    private static boolean isNone(Object o) {
        
        return o == null || "".equals(o) || "none".equals(o);
    }
    
    private static boolean isZero(String o) {
        if (o == null || "".equals(o)) {
            return true;
        }
        
        o = o.replaceAll("[^0-9]", "");
        return o.matches("^0*$");
    }
    
    private static boolean isZero(ScaledUnit o) {
        return o == null || "none".equals(o.getStringValue()) ||  o.getNumericValue() == 0 ;
    }
    
    private static String hashString(String message, String algorithm) {

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));

            return convertByteArrayToHexString(hashedBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new RuntimeException(
                    "Could not generate hash from String", ex);
        }
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }
    
    public static String generateMD5(String message) {
        return hashString(message, "MD5");
    }

    public static String generateSHA1(String message) {
        return hashString(message, "SHA-1");
    }

    public static String generateSHA256(String message) {
        return hashString(message, "SHA-256");
    }
    
    public class Element {
        Element parent = anyNodeStyle;
        Map properties = new LinkedHashMap();
        
        Element unselected;
        Element selected;
        Element pressed;
        Element disabled;
        
        public String getChecksum() {
            StringBuilder sb = new StringBuilder();
            sb.append("STYLE=").append(this.getFlattenedStyle())
                    .append(";UNSELECTED=").append(this.getFlattenedUnselectedStyle())
                    .append(";SELECTED=").append(this.getFlattenedSelectedStyle())
                    .append(";PRESSED=").append(this.getFlattenedPressedStyle())
                    .append(";DISABLED=").append(this.getFlattenedDisabledStyle());
            
            return generateMD5(sb.toString());
        }
        
        Insets getBoxShadowPadding(Map<String, LexicalUnit> style) {
            Insets i = new Insets();
            ScaledUnit boxShadow = (ScaledUnit)style.get("cn1-box-shadow-h");
            ScaledUnit tmp = boxShadow;
            while (tmp != null) {
                tmp = (ScaledUnit)tmp.getPreviousLexicalUnit();
                if (tmp != null) {
                    boxShadow = tmp;
                }
            }

            if (isNone(boxShadow)) {
                return i;
            }

            ScaledUnit insetUnit = boxShadow;
            while (insetUnit != null) {
                if ("inset".equals(insetUnit.getStringValue())) {
                    return i;
                }
                insetUnit = (ScaledUnit)insetUnit.getNextLexicalUnit();
            }

            double hShadow = boxShadow.getPixelValue();
            boxShadow = (ScaledUnit)boxShadow.getNextLexicalUnit();

            double vShadow = 0;
            if (boxShadow == null) {
                boxShadow = (ScaledUnit)style.get("cn1-box-shadow-v");
            }
            if (boxShadow != null) {
                vShadow = boxShadow.getPixelValue();
                boxShadow = (ScaledUnit)boxShadow.getNextLexicalUnit();
            }
            
            double blur = 0;
            if (boxShadow == null) {
                boxShadow = (ScaledUnit)style.get("cn1-box-shadow-blur");
            }
            if (boxShadow != null) {
                blur = boxShadow.getPixelValue();
                boxShadow = (ScaledUnit)boxShadow.getNextLexicalUnit();
            }
            double spread = 0;
            if (boxShadow == null) {
                boxShadow = (ScaledUnit)style.get("cn1-box-shadow-spread");
            }
            if (boxShadow != null) {
                spread = boxShadow.getPixelValue();
            }
            
            i.top = Math.max(0,(int)Math.ceil(spread - vShadow + blur/2));
            i.left = Math.max(0, (int)Math.ceil(spread - hShadow + blur/2));
            i.bottom = Math.max(0, (int)Math.ceil(spread + vShadow + blur/2));
            i.right = Math.max(0, (int)Math.ceil(spread + hShadow + blur/2));

            return i;
        }
        
        String generateBoxShadowPaddingString() {
            StringBuilder sb = new StringBuilder();
            Map styles = new LinkedHashMap();
            styles.putAll(getFlattenedStyle());
            
            return ""+getBoxShadowPadding(styles);
        }
        
        String generateStyleCSS() {
            Map styles = new LinkedHashMap();
            styles.putAll(getFlattenedStyle());
            try {
                StringBuilder sb = new StringBuilder();


                if (this.requiresImageBorder(styles)) {
                    if (styles.get("min-height") != null) {
                        styles.put("height", styles.get("min-height"));
                    }
                    if (styles.get("min-width") != null) {
                        styles.put("width", styles.get("min-width"));
                    }
                }

                if (styles.get("height") == null) {
                    styles.put("height", new ScaledUnit(new PixelUnit(100), 320, 640, 960));
                }
                //styles.put("margin", new ScaledUnit(new PixelUnit(1), 144, 640, 960));

                for (Object key : styles.keySet()) {
                    String property = (String) key;
                    LexicalUnit value = (LexicalUnit) styles.get(key);
                    String prop = renderCSSProperty(property, styles);
                    if (!prop.isEmpty()) {
                        sb.append(prop).append(";");
                    }

                }

                Insets shadowInset = getBoxShadowPadding(styles);
                if (shadowInset.top > 0) {
                    sb.append("margin-top: ").append(shadowInset.top).append("px !important;");
                }
                if (shadowInset.left > 0) {
                    sb.append("margin-left: ").append(shadowInset.left).append("px !important;");
                }
                if (shadowInset.right > 0) {
                    sb.append("margin-right: ").append(shadowInset.right).append("px !important;");
                }
                if (shadowInset.bottom > 0) {
                    sb.append("margin-bottom: ").append(shadowInset.bottom).append("px !important;");
                }

                //sb.append("border-top-right-radius: 10px / 20px;");
                return sb.toString();
            } catch (Exception ex) {
                System.err.println("Failed to generate style CSS for style: " + styles + ".  Message was "+ex.getMessage());
                throw ex;
            }
        }
        
        void setParent(String name) {
            Element parentEl = getElementByName(name);
            Element self = this;
            if (this.isSelectedStyle() || this.isDisabledStyle() || this.isDisabledStyle() || this.isUnselectedStyle()) {
                self = this.parent;
            }
            
            self.parent = parentEl;
        }
        
        String getHtmlPreview() {
            StringBuilder sb = new StringBuilder();
            sb.append("<div style=\"").append(generateStyleCSS()).append("\">Lorem Ipsum</div>");
            return sb.toString();
        }
        
        public String getEmptyHtmlWithId(String id, Map<String,LexicalUnit> style) {
            StringBuilder sb = new StringBuilder();
            String generateImage = (this.requiresBackgroundImageGeneration(style) || this.requiresImageBorder(style)) ? "true" : "false";

            sb.append("<div id=\""+id+"\" class=\"element\" style=\"").append(generateStyleCSS())
                    .append("\" data-box-shadow-padding=\"").append(generateBoxShadowPaddingString()).append("\"")
                    .append(" data-generate-image=\"").append(generateImage).append("\"")
                    .append("></div>");
            return sb.toString();
        }
        
        Map getFlattenedSelectedStyle() {
            Map out = new LinkedHashMap();
            
            LinkedList<Map> stack = new LinkedList<Map>();
            Element el = this;
            if (!el.isSelectedStyle()) {
                el = el.getSelected();
            }
            while (el != null) {
                stack.push(el.style);
                
                el = el.parent.parent;
                if (el != null) {
                    el = el.getSelected();
                }
            }
            
            while (!stack.isEmpty()) {
                out.putAll(stack.pop());
            }
            return out;
        }
        
        Map getFlattenedPressedStyle() {
            Map out = new LinkedHashMap();
            
            LinkedList<Map> stack = new LinkedList<Map>();
            Element el = this;
            if (!el.isPressedStyle()) {
                el = el.getPressed();
            }
            while (el != null) {
                stack.push(el.style);
                
                el = el.parent.parent;
                if (el != null) {
                    el = el.getPressed();
                }
            }
            
            while (!stack.isEmpty()) {
                out.putAll(stack.pop());
            }
            return out;
        }
        
        Map getFlattenedUnselectedStyle() {
            Map out = new LinkedHashMap();
            
            LinkedList<Map> stack = new LinkedList<Map>();
            Element el = this;
            if (!el.isUnselectedStyle()) {
                el = el.getUnselected();
            }
            while (el != null) {
                stack.push(el.style);
                
                el = el.parent.parent;
                if (el != null) {
                    el = el.getUnselected();
                }
            }
            
            while (!stack.isEmpty()) {
                out.putAll(stack.pop());
            }
            return out;
        }
        
        Map getFlattenedDisabledStyle() {
            Map out = new LinkedHashMap();
            
            LinkedList<Map> stack = new LinkedList<Map>();
            Element el = this;
            if (!el.isDisabledStyle()) {
                el = el.getDisabled();
            }
            while (el != null) {
                stack.push(el.style);
                
                el = el.parent.parent;
                if (el != null) {
                    el = el.getDisabled();
                }
            }
            
            while (!stack.isEmpty()) {
                out.putAll(stack.pop());
            }
            return out;
        }
        
        Map getFlattenedStyle() {
            Map out = new LinkedHashMap();
            if (this.isSelectedStyle()) {
                if (parent != null) {
                    out.putAll(parent.getFlattenedStyle());
                }
                out.putAll(getFlattenedSelectedStyle());
                
            } else if (this.isUnselectedStyle()) {
                if (parent != null) {
                    out.putAll(parent.getFlattenedStyle());
                }
                out.putAll(getFlattenedUnselectedStyle());
            } else if (this.isDisabledStyle()) {
                if (parent != null) {
                    out.putAll(parent.getFlattenedStyle());
                }
                out.putAll(getFlattenedDisabledStyle());
            } else if (this.isPressedStyle()) {
                if (parent != null) {
                    out.putAll(parent.getFlattenedStyle());
                }
                out.putAll(getFlattenedPressedStyle());
            } else {
                if (parent != null) {
                    out.putAll(parent.getFlattenedStyle());
                }
                out.putAll(style);
            }
            return Collections.unmodifiableMap(out);
        }
        
        
        
        Element getUnselected() {
            if (unselected == null) {
                unselected = new Element();
                unselected.parent = this;
            }
            return unselected;
        }
        
        Element getSelected() {
            if (selected == null) {
                selected = new Element();
                selected.parent = this;
                //selected.style.putAll(getUnmodifiableStyle());
                
            }
            return selected;
        }
        
        
        
        Element getPressed() {
            if (pressed == null) {
                pressed = new Element();
                pressed.parent = this;
                //pressed.style.putAll(getUnmodifiableStyle());
                
            }
            return pressed;
        }
        
        Element getDisabled() {
            if (disabled == null) {
                disabled = new Element();
                disabled.parent = this;
                //disabled.style.putAll(style);
            }
            return disabled;
        }
        
        void put(String key, Object value) {
            style.put(key, value);
        }
        
        Object get(String key) {
            Object res = style.get(key);
            if (res == null || parent != null) {
                return parent.get(key);
            }
            return null;
        }
        
        Object getUnselectedValue(String key) {
            return getUnselectedValue(key, false);
        }
        
        Object getUnselectedValue(String key, boolean includeDefault) {
            if (isUnselectedStyle()) {
                Object res = style.get(key);
                if (res != null) {
                    return res;
                }
            }
            if (unselected != null) {
                Object res = unselected.style.get(key);
                if (res != null) {
                    return res;
                }
            }
            if (parent != null) {
                return parent.getUnselectedValue(key);
            }
            if (includeDefault) {
                return get(key);
            }
            return null;
        }
        
        
        Object getSelectedValue(String key) {
            return getSelectedValue(key, false);
        }
        
        Object getSelectedValue(String key, boolean includeDefault) {
            if (isSelectedStyle()) {
                Object res = style.get(key);
                if (res != null) {
                    return res;
                }
            }
            if (selected != null) {
                Object res = selected.style.get(key);
                if (res != null) {
                    return res;
                }
            }
            if (parent != null) {
                return parent.getSelectedValue(key);
            }
            if (includeDefault) {
                return get(key);
            }
            return null;
        }
        
        Object getPressedValue(String key) {
            return getPressedValue(key, false);
        }
        
        /**
         * Gets the pressed value for a property.  Will check the parent's 
         * disabled value if this element has none for the specified prop.
         * 
         * @param key
         * @return 
         */
        Object getPressedValue(String key, boolean includeDefault) {
            if (isPressedStyle()) {
                Object res = style.get(key);
                if (res != null) {
                    return res;
                }
            }
            if (pressed != null) {
                Object res = pressed.style.get(key);
                if (res != null) {
                    return res;
                }
            }
            if (parent != null) {
                return parent.getPressedValue(key);
            }
            
            if (includeDefault) {
                return get(key);
            }
            return null;
        }
        
        Object getDisabledValue(String key) {
            return getDisabledValue(key, false);
        }
        
        Object getDisabledValue(String key, boolean includeDefault) {
            if (isDisabledStyle()) {
                Object res = style.get(key);
                if (res != null) {
                    return res;
                }
            }
            if (disabled != null) {
                Object res = disabled.style.get(key);
                if (res != null) {
                    return res;
                }
            }
            if (parent != null) {
                return parent.getDisabledValue(key);
            }
            if (includeDefault) {
                return get(key);
            }
            
            return null;
        }
        
        boolean isUnselectedStyle() {
            return parent != null && parent.unselected == this;
        }
        
        boolean isSelectedStyle() {
            return parent != null && parent.selected == this;
        }
        
        boolean isPressedStyle() {
            return parent != null && parent.pressed == this;
            
        }
        
        boolean isDisabledStyle() {
            return parent != null && parent.disabled == this;
        }
        
        
        /**
         * Will either return a valid gradient type or -1 if the gradient
         * can't be achieved using codenameone gradient background types.
         * @param styles
         * @return 
         */
//        int tryGetCN1GradientType(Map<String, LexicalUnit> styles) {
//            LexicalUnit background = styles.get("background");
//            if (background == null) {
//                return -1;
//            }
//            int gradientType = -1;
//            if (background.getFunctionName() != null && background.getFunctionName().equals("linear-gradient")) {
//                ScaledUnit params = (ScaledUnit)background.getParameters();
//                if (params == null) {
//                    return -1;
//                }
//                switch (params.getLexicalUnitType()) {
//                    case LexicalUnit.SAC_DEGREE:
//                    case LexicalUnit.SAC_RADIAN:
//                        
//                        double dval = params.getNumericValue();
//                        if (params.getLexicalUnitType() == LexicalUnit.SAC_RADIAN) {
//                            dval = dval * 180.0 / Math.PI;
//                        }
//                        int ival = (int)dval;
//                        
//                        if (ival != 0 && ival != 90 && ival != 180 && ival != 270) {
//                            System.err.println("Background gradient wrong degrees");
//                            return -1;
//                        }
//                        if (ival == 0 || ival == 180) {
//                            gradientType = Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL;
//                        } else {
//                            gradientType = Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL;
//                        }
//                    break;
//                    default:
//                        System.err.println("Background gradient first parameter needs to be degree or radian or CN1 can't handle it.  Using image background");
//                        return -1;
//                }
//                
//                
//                params = (ScaledUnit)params.getNextLexicalUnit();
//                if (params == null) {
//                    
//                    return -1;
//                }
//                
//                if (params.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
//                    params = (ScaledUnit)params.getNextLexicalUnit();
//                }
//                if (params == null) {
//                    return -1;
//                }
//                switch (params.getLexicalUnitType()) {
//                    case LexicalUnit.SAC_FUNCTION:
//                    case LexicalUnit.SAC_IDENT:
//                    case LexicalUnit.SAC_RGBCOLOR:
//                    case LexicalUnit.SAC_ATTR:
//                    
//                        break;
//                    default:
//                        System.err.println("2nd param for linear-gradient needs to be a color but found "+params+" type "+params.getLexicalUnitType());
//                        return -1;
//                }

//                params = (ScaledUnit)params.getNextLexicalUnit();
//                if (params == null) {
//                    
//                    return -1;
//                }
//                if (params.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
//                    params = (ScaledUnit)params.getNextLexicalUnit();
//                }
//                if (params == null) {
//                    return -1;
//                }

//                switch (params.getLexicalUnitType()) {
//                    case LexicalUnit.SAC_FUNCTION:
//                    case LexicalUnit.SAC_IDENT:
//                    case LexicalUnit.SAC_RGBCOLOR:
//                    case LexicalUnit.SAC_ATTR:
//                        break;
//                    default:
//                        return -1;
//                }
//                
//            }

//            return gradientType;
//        
//        
//        }
        
        Map style = new LinkedHashMap();
        
        Border createBorder(Map<String,LexicalUnit> styles) {
            Border b = new Border();
            
            b.borderColorTop = renderAsCSSString("border-top-color", styles);
            b.borderColorRight = renderAsCSSString("border-right-color", styles);
            b.borderColorBottom = renderAsCSSString("border-bottom-color", styles);
            b.borderColorLeft = renderAsCSSString("border-left-color", styles);
            b.styleBottom = renderAsCSSString("border-bottom-style", styles);
            b.styleLeft = renderAsCSSString("border-left-style", styles);
            b.styleTop = renderAsCSSString("border-top-style", styles);
            b.styleRight = renderAsCSSString("border-right-style", styles);
            b.thicknessTop = renderAsCSSString("border-top-width", styles);
            b.thicknessRight = renderAsCSSString("border-right-width", styles);
            b.thicknessBottom = renderAsCSSString("border-bottom-width", styles);
            b.thicknessLeft = renderAsCSSString("border-left-width", styles);
            b.backgroundColor = renderAsCSSString("background-color", styles);
            b.backgroundImageUrl = renderAsCSSString("background-image", styles);
            b.backgroundRepeat = renderAsCSSString("background-repeat", styles);
            b.borderRadius = renderCSSProperty("cn1-border-bottom-left-radius-x", styles);
            b.boxShadow = renderCSSProperty("cn1-box-shadow-h", styles);
            b.borderImage = renderCSSProperty("border-image", styles);
            b.borderImageSlice = renderCSSProperty("border-image-slice", styles);
            
            LexicalUnit background = styles.get("background");
            while (background != null) {
                if (background.getFunctionName() != null && background.getFunctionName().contains("gradient")) {
                    b.gradient = renderAsCSSString(background);
                }
                background = background.getNextLexicalUnit();
            }
            return b;
        }
        
        
        Insets getOnlyBorderInsets(Map<String,LexicalUnit> styles) {
            ScaledUnit borderLeftWidth = (ScaledUnit)styles.get("border-left-width");
            ScaledUnit borderTopWidth = (ScaledUnit)styles.get("border-top-width");
            ScaledUnit borderRightWidth = (ScaledUnit)styles.get("border-right-width");
            ScaledUnit borderBottomWidth = (ScaledUnit)styles.get("border-bottom-width");
            
            Insets i = new Insets();
            if (!isNone(borderLeftWidth)) {
                i.left = borderLeftWidth.getPixelValue();
            }
            if (!isNone(borderRightWidth)) {
                i.right = borderRightWidth.getPixelValue();
            }
            if (!isNone(borderTopWidth)) {
                i.top = borderTopWidth.getPixelValue();
            }
            if (!isNone(borderBottomWidth)) {
                i.bottom = borderBottomWidth.getPixelValue();
            }
            
            return i;
        }
        
        Insets getImageBorderInsets(Map<String,LexicalUnit> styles, int width, int height) {
            // Case 1:  Solid background color
            ScaledUnit cn19Patch = (ScaledUnit)styles.get("cn1-9patch");
            
            
            LexicalUnit boxShadowH = styles.get("cn1-box-shadow-h");
            LexicalUnit borderRadiusBottomLeftX = styles.get("cn1-border-bottom-left-radius-x");
            LexicalUnit borderRadiusBottomLeftY = styles.get("cn1-border-bottom-left-radius-y");
            LexicalUnit borderRadiusBottomRightY = styles.get("cn1-border-bottom-right-radius-y");
            LexicalUnit borderRadiusBottomRightX = styles.get("cn1-border-bottom-right-radius-x");
            LexicalUnit borderRadiusTopRightX = styles.get("cn1-border-top-right-radius-x");
            LexicalUnit borderRadiusTopRightY = styles.get("cn1-border-top-right-radius-y");
            LexicalUnit borderRadiusTopLeftY = styles.get("cn1-border-top-left-radius-y");
            LexicalUnit borderRadiusTopLeftX = styles.get("cn1-border-top-left-radius-x");
            
            //LexicalUnit borderLeftWidth = styles.get("border-left-width");
            //LexicalUnit borderTopWidth = styles.get("border-top-width");
            //LexicalUnit borderRightWidth = styles.get("border-right-width");
            //LexicalUnit borderBottomWidth = styles.get("border-bottom-width");
            
            Insets i = new Insets();
            
            if (cn19Patch != null) {
                i.top = (int)cn19Patch.getNumericValue();
                cn19Patch = (ScaledUnit)cn19Patch.getNextLexicalUnit();
                i.right = (int)cn19Patch.getNumericValue();
                cn19Patch = (ScaledUnit)cn19Patch.getNextLexicalUnit();
                i.bottom = (int)cn19Patch.getNumericValue();
                cn19Patch = (ScaledUnit)cn19Patch.getNextLexicalUnit();
                i.left = (int)cn19Patch.getNumericValue();
                return i;
            }
            
            
            LexicalUnit background = styles.get("background");
            
           
            
            
            //if (isNone(background)) {
            //    i.bottom = Math.max(getPixelValue(borderRadiusBottomLeftY), getPixelValue(borderRadiusBottomRightY)) + 5;
            //    i.top = Math.max(getPixelValue(borderRadiusTopLeftY), getPixelValue(borderRadiusTopRightY)) + 5;
            //    
            //}
            
            boolean hasBackgroundGradient = false;
            boolean horizontalGradient = false;
            LexicalUnit tmpUnit = background;
            while (tmpUnit != null) {
                if (tmpUnit.getLexicalUnitType() == LexicalUnit.SAC_FUNCTION) {
                    if ("linear-gradient".equals(tmpUnit.getFunctionName())) {
                        hasBackgroundGradient = true;
                        LexicalUnit gradientParams = tmpUnit.getParameters();
                        while (gradientParams != null) {
                            if ("right".equals(gradientParams.getStringValue()) || "left".equals(gradientParams.getStringValue())) {
                                horizontalGradient = true;
                                break;
                            }
                            gradientParams = gradientParams.getNextLexicalUnit();
                        }
                        break;
                    }
                    tmpUnit = tmpUnit.getNextLexicalUnit();
                }
            }
            
            Insets boxShadowInsets = getBoxShadowPadding(styles);
            Insets borderInsets = getOnlyBorderInsets(styles);
            
            if (hasBackgroundGradient) {
                if (horizontalGradient) {
                    i.left = width/2-1;
                    i.right = i.left;
                    i.top = 1+borderInsets.top + boxShadowInsets.top + boxShadowInsets.bottom;
                    i.bottom =1 + borderInsets.bottom + boxShadowInsets.bottom + boxShadowInsets.top;
                } else {
                    i.top = height/2-1;
                    i.bottom = i.top;
                    i.left = 1 + borderInsets.left + boxShadowInsets.left + boxShadowInsets.right;
                    i.right = 1 + borderInsets.right + boxShadowInsets.right + boxShadowInsets.left;
                }
                
                
                //return i;
            }
            
            if (!isNone(borderRadiusTopLeftX) || !isNone(borderRadiusBottomLeftX)) {
                i.left = Math.max(i.left, Math.max(getPixelValue(borderRadiusTopLeftX), getPixelValue(borderRadiusBottomLeftX)) + 
                        borderInsets.left + boxShadowInsets.left + boxShadowInsets.right + 1);
            } else {
            
                i.left = Math.max(i.left, borderInsets.left + boxShadowInsets.left + boxShadowInsets.right + 1);
            }
            if (!isNone(borderRadiusTopRightX) || !isNone(borderRadiusBottomRightX)) {
                i.right = Math.max(i.right, Math.max(getPixelValue(borderRadiusTopRightX), getPixelValue(borderRadiusBottomRightX)) + 
                        borderInsets.right + boxShadowInsets.right + boxShadowInsets.left + 1);
            } else {
                i.right = Math.max(i.right, borderInsets.right + boxShadowInsets.left + boxShadowInsets.right + 1);
            }
            if (!isNone(borderRadiusTopLeftY) || !isNone(borderRadiusTopRightY)) {
                i.top = Math.max(i.top, Math.max(getPixelValue(borderRadiusTopLeftY), getPixelValue(borderRadiusTopRightY)) + 
                        borderInsets.top + boxShadowInsets.top + boxShadowInsets.bottom + 1);
            } else {
            
                i.top = Math.max(i.top, borderInsets.top + boxShadowInsets.top + boxShadowInsets.bottom + 1);
            }
            if (!isNone(borderRadiusBottomLeftY) || !isNone(borderRadiusBottomRightY)) {
                i.bottom = Math.max(i.bottom, Math.max(getPixelValue(borderRadiusBottomLeftY), getPixelValue(borderRadiusBottomRightY)) + 
                        borderInsets.bottom + boxShadowInsets.bottom + boxShadowInsets.top + 1);
            } else {
            
                i.bottom = Math.max(i.bottom, borderInsets.bottom + boxShadowInsets.bottom + boxShadowInsets.top + 1);
            }
            
            //i.top = Math.max(getPixelValue(borderTopWidth), i.top);
            //i.bottom = Math.max(getPixelValue(borderBottomWidth), i.bottom);
            
            if (i.left <= 0) {
                i.left = 1;
            }
            
            if (i.right <= 0) {
                i.right = 1;
            }
            
            if (i.top <= 0) {
                i.top = 1;
            }
            
            if (i.bottom <= 0) {
                i.bottom = 1;
            }
            
            if (i.top + i.bottom >= height) {
                i.top = height/2-1;
                i.bottom = height/2-1;
            }
            if (i.left + i.right >= width) {
                i.left = width/2-1;
                i.right = width/2-1;
            }
            
            
            //i.top = 10;
            //i.bottom = 10;
            //i.left = 10;
            //i.right = 10;
            return i;
        }
        
        public String getThemeFgColor(Map<String,LexicalUnit> style) {
            LexicalUnit color = style.get("color");
            return getColorString(color, false);
        }
        
        public String getThemeBgColor(Map<String,LexicalUnit> style) {
            ScaledUnit color = (ScaledUnit)style.get("background-color");
            return getColorString(color);
        }
        
        
        public Object[] getThemeBgGradient(Map<String, LexicalUnit> style) {
            if (requiresImageBorder(style) || requiresBackgroundImageGeneration(style)) {
                return null;
            }
            
            ScaledUnit background = (ScaledUnit)style.get("background");
            if (background != null && background.isCN1Gradient()) {
                return background.getCN1Gradient().getThemeBgGradient();
            }
            
            return null;
            
                    
        }
        public boolean hasFilter(Map<String,LexicalUnit> style) {
            
            return false;
        }
        
        private boolean usesRoundBorder(Map<String, LexicalUnit> style) {
            LexicalUnit backgroundType = style.get("cn1-background-type");
            return (backgroundType != null) && 
                    ("cn1-round-border".equals(backgroundType.getStringValue()) || 
                        "cn1-pill-border".equals(backgroundType.getStringValue())
                    );
        }
        
        private boolean isPillPorder(Map<String, LexicalUnit> style) {
            LexicalUnit backgroundType = style.get("cn1-background-type");
            return (backgroundType != null) && "cn1-pill-border".equals(backgroundType.getStringValue());
        }
        
        public boolean requiresBackgroundImageGeneration(Map<String,LexicalUnit> style) {
            /*
            LexicalUnit backgroundType = style.get("cn1-background-type");
            if (backgroundType != null) {
                if ( backgroundType.getStringValue().startsWith("cn1-image") && !backgroundType.getStringValue().endsWith("border")) {
                    
                    
                    return true;
                }
            }
            */
            ScaledUnit background  = (ScaledUnit) style.get("background");
            boolean isCN1Gradient = background != null && background.isCN1Gradient();
            Border b = createBorder(style);
            if (b.canBeAchievedWithRoundRectBorder(style) || b.canBeAchievedWithUnderlineBorder(style) || b.canBeAchievedWithCSSBorder(style)) {
                // If we can do this with a roundrect border
                // then we don't need background image
                return false;
            }
            LexicalUnit backgroundType = style.get("cn1-background-type");
            if (usesRoundBorder(style)) {
                return false;
            }
            
            if (b.hasBorderRadius() || (b.hasGradient() && !isCN1Gradient) || b.hasBoxShadow() || hasFilter(style) || b.hasUnequalBorders() || !b.isStyleNativelySupported() || usesPointUnitsInBorder(style)) {
                // We might need to generate a background image
                // We first need to determine if this can be done with a 9-piece border
                // or if we'll need to stretch it.
                // Heuristics:
                // If the width or height is specified in percent
                // then this looks like a case for a generated image.
                LexicalUnit width = style.get("width");
                LexicalUnit height = style.get("height");
                if (width != null && width.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                    return true;
                }
                if (height != null && height.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                    return true;
                }
                
                if (backgroundType != null) {
                    
                    if ( backgroundType.getStringValue().startsWith("cn1-image") && !backgroundType.getStringValue().endsWith("border")) {


                        return true;
                    }
                }
                
                if (isGradient(background) && !requiresImageBorder(style)) {
                    return true;
                }
            }
            
            return false;
            
        }
        
        public boolean usesPointUnitsInBorder(Map<String,LexicalUnit> styles, String side) {
            ScaledUnit topWidth = (ScaledUnit)styles.get("border-"+side+"-width");
            if (topWidth == null) {
                return false;
            }
            return topWidth.getLexicalUnitType() == LexicalUnit.SAC_POINT;
        }
        
        
        public boolean usesPointUnitsInBorder(Map<String,LexicalUnit> styles) {
            for (String side : new String[]{"top", "right", "bottom", "left"}) {
                if (usesPointUnitsInBorder(styles, side)) {
                    return true;
                }
            }
            return false;
        }
        
        public boolean requiresImageBorder(Map<String,LexicalUnit> style) {
            ScaledUnit background = (ScaledUnit)style.get("background");
            boolean isCN1Gradient = background != null && background.isCN1Gradient();
            LexicalUnit backgroundType = style.get("cn1-background-type");
            if (backgroundType != null && "cn1-image-border".equals(backgroundType.getStringValue())) {
                return true;
            } else if (backgroundType != null && backgroundType.getStringValue().startsWith("cn1-image")) {
                return false;
            } else if (usesRoundBorder(style)) {
                
                return false;
            }
            
            if (!isNone(style.get("cn1-9patch"))) {
                return true;
            }
            
            Border b = this.createBorder(style);
            if (b.canBeAchievedWithRoundRectBorder(style) || b.canBeAchievedWithUnderlineBorder(style) || b.canBeAchievedWithCSSBorder(style)) {
                // If we can do it with a RoundRectBorder, then we don't need to generate an imageborder
                return false;
            }
            
            if (b.hasBorderRadius() || (b.hasGradient() && !isCN1Gradient) || b.hasBoxShadow() || hasFilter(style) || b.hasUnequalBorders() || !b.isStyleNativelySupported() || usesPointUnitsInBorder(style)) {
                LexicalUnit width = style.get("width");
                LexicalUnit height = style.get("height");
                
                if (width != null && width.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                    return false;
                }
                if (height != null && height.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                    return false;
                }
                if (isGradient(background) && isNone(style.get("border")) && !b.hasBorderRadius() && !b.hasBoxShadow() && !b.hasUnequalBorders() && !usesPointUnitsInBorder(style)) {
                    // This is just a gradient... it should be rendered just as an image background
                    return false;
                }
                return true;
            }
            return false;
        }
        
        public boolean hasBackgroundImage(Map<String,LexicalUnit> style) {
            LexicalUnit bgImage = style.get("background-image");
            return bgImage != null && bgImage.getLexicalUnitType() == LexicalUnit.SAC_URI;
        }
        
        
        
        public Byte getThemeBgType(Map<String,LexicalUnit> style) {
            LexicalUnit value = style.get("cn1-background-type");
            ScaledUnit background = (ScaledUnit)style.get("background");
            boolean isCN1Gradient = background != null && background.isCN1Gradient();
            if (value == null) {
                // Not explicitly specified so we must use some heuristics here
                if (!requiresImageBorder(style) && !requiresBackgroundImageGeneration(style) && isCN1Gradient) {
                    return (byte)background.getCN1Gradient().type;
                }
                if (!requiresBackgroundImageGeneration(style) &&  hasBackgroundImage(style)) {    
                    LexicalUnit repeat = style.get("background-repeat");
                    if (repeat != null) {
                        switch (repeat.getStringValue()) {
                            case "repeat" :
                                return Style.BACKGROUND_IMAGE_TILE_BOTH;
                            case "repeat-x" :
                                return Style.BACKGROUND_IMAGE_TILE_HORIZONTAL;
                            case "repeat-y" :
                                return Style.BACKGROUND_IMAGE_TILE_VERTICAL;
                            case "no-repeat" : 
                            default : {
                                LexicalUnit bgSize = style.get("background-size");
                                if (bgSize != null) {
                                    switch (bgSize.getLexicalUnitType()) {
                                        case LexicalUnit.SAC_PERCENTAGE: {
                                            ScaledUnit width = (ScaledUnit)bgSize;
                                            ScaledUnit height = (ScaledUnit)bgSize.getNextLexicalUnit();
                                            if (width.getNumericValue() > 99 && (height == null || height.getNumericValue() > 99 || "auto".equals(height.getStringValue()))) {
                                                // This is clearly asking us to stretch the 
                                                // image
                                                return Style.BACKGROUND_IMAGE_SCALED;
                                            }
                                            break;
                                        }
                                        
                                        case LexicalUnit.SAC_IDENT :
                                            switch (bgSize.getStringValue()) {
                                                case "cover" :
                                                    return Style.BACKGROUND_IMAGE_SCALED_FILL;
                                                case "contains" :
                                                    return Style.BACKGROUND_IMAGE_SCALED_FIT;
                                            }
                                            
                                        break;
                                            
                                    }
                                }
                                
                                LexicalUnit bgPosition1 = style.get("background-position");
                                
                                if (bgPosition1 != null) {
                                    LexicalUnit bgPosition2 = bgPosition1.getNextLexicalUnit();
                                    
                                    String val1 = bgPosition1.getStringValue();
                                    String val2 = bgPosition2 == null ? "auto" : bgPosition2.getStringValue();
                                    
                                    switch (val1) {
                                        case "left" :
                                            switch (val2) {
                                                case "top" :
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT;
                                                
                                                case "bottom" :
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT;
                                                default:
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_LEFT;
                                            }
                                        
                                        
                                        case "right" : 
                                            switch (val2) {
                                                case "top" :
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT;
                                                case "bottom" :
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT;
                                                default :
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_RIGHT;
                                            }
                                        
                                        default :
                                            switch (val2) {
                                                case "top" :
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_TOP;
                                                case "bottom" :
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM;
                                                default:
                                                    return Style.BACKGROUND_IMAGE_ALIGNED_CENTER;
                                            }
                                    }
                                }
                                
                                
                            }
                                
                                    
                        }
                    }  else {
                        return Style.BACKGROUND_IMAGE_TILE_BOTH;
                    }
                } else if (requiresBackgroundImageGeneration(style)) {
                    LexicalUnit bgSize = style.get("background-size");
                    if (bgSize != null) {
                        switch (bgSize.getLexicalUnitType()) {
                            case LexicalUnit.SAC_PERCENTAGE: {
                                ScaledUnit width = (ScaledUnit)bgSize;
                                ScaledUnit height = (ScaledUnit)bgSize.getNextLexicalUnit();
                                if (width.getNumericValue() > 99 && (height == null || height.getNumericValue() > 99 || "auto".equals(height.getStringValue()))) {
                                    // This is clearly asking us to stretch the 
                                    // image
                                    return Style.BACKGROUND_IMAGE_SCALED;
                                }
                                break;
                            }

                            case LexicalUnit.SAC_IDENT :
                                switch (bgSize.getStringValue()) {
                                    case "cover" :
                                        return Style.BACKGROUND_IMAGE_SCALED_FILL;
                                    case "contains" :
                                        return Style.BACKGROUND_IMAGE_SCALED_FIT;
                                }

                            break;

                        }
                    } else {
                        return Style.BACKGROUND_IMAGE_SCALED;
                    }
                } 
                return null;
                
            }
            
            switch (value.getStringValue()) {
                case "cn1-background-gradient-linear-horizontal":
                    return Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL;
                case "cn1-background-gradient-linear-vertical":
                    return Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL;
                case "cn1-image-scaled":
                    return Style.BACKGROUND_IMAGE_SCALED;
                case "cn1-image-scaled-fill":
                    return Style.BACKGROUND_IMAGE_SCALED_FILL;
                case "cn1-image-scaled-fit":
                    return Style.BACKGROUND_IMAGE_SCALED_FIT;
                case "cn1-image-tile-both" :
                    return Style.BACKGROUND_IMAGE_TILE_BOTH;
                case "cn1-image-tile-valign-left" :
                    return Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT;
                case "cn1-image-tile-valign-center" :
                    return Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER;
                case "cn1-image-tile-valign-right" :
                    return Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT;
                case "cn1-image-tile-halign-top" :
                    return Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP;
                case "cn1-image-tile-halign-center" :
                    return Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER;
                case "cn1-image-tile-halign-bottom" :
                    return Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM;
                case "cn1-image-align-top":
                    return Style.BACKGROUND_IMAGE_ALIGNED_TOP;
                case "cn1-image-align-bottom" :
                    return Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM;
                case "cn1-image-align-right" :
                    return Style.BACKGROUND_IMAGE_ALIGNED_RIGHT;
                case "cn1-image-align-left" :
                    return Style.BACKGROUND_IMAGE_ALIGNED_LEFT;
                case "cn1-image-align-center" :
                    return Style.BACKGROUND_IMAGE_ALIGNED_CENTER;
                case "cn1-image-align-top-left" :
                    return Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT;
                case "cn1-image-align-top-right" :
                    return Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT;
                case "cn1-image-align-bottom-left" :
                    return Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT;
                case "cn1-image-align-bottom-right" :
                    return Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT;
                case "cn1-image-border":
                case "cn1-round-border":
                case "cn1-pill-border":
                case "cn1-none" :
                case "none":
                    return Style.BACKGROUND_NONE;
                default:
                    throw new RuntimeException("Unsupported bg type "+value);
            }
        }
        public String getThemePadding(Map<String,LexicalUnit> style) {
            if (!style.containsKey("padding-left") && !style.containsKey("padding-top") && !style.containsKey("padding-bottom") && !style.containsKey("padding-right")) {
                return null;
            }
            Insets i = getInsets("padding", style);
            String out = i.top + ","+i.bottom+","+i.left+","+i.right;
            return out;
        }
        
        public byte[] getThemePaddingUnit(Map<String,LexicalUnit> style) {
            if (!style.containsKey("padding-left") && !style.containsKey("padding-top") && !style.containsKey("padding-bottom") && !style.containsKey("padding-right")) {
                return null;
            }
            Insets i = getInsets("padding", style);
            return new byte[]{i.topUnit, i.leftUnit, i.bottomUnit, i.rightUnit};
        }
        
        public String getThemeMargin(Map<String,LexicalUnit> style) {
            if (!style.containsKey("margin-left") && !style.containsKey("margin-top") && !style.containsKey("margin-bottom") && !style.containsKey("margin-right")) {
                return null;
            }
            Insets i = getInsets("margin", style);
            return i.top + ","+i.bottom+","+i.left+","+i.right;
        }

        public int getThemeElevation(Map<String, LexicalUnit> style) {
            if (style.containsKey("elevation")) {
                return ((ScaledUnit)style.get("elevation")).getIntegerValue();
            }
            return 0;
        }

        public float getThemeIconGap(Map<String, LexicalUnit> style) {
            if (style.containsKey("icon-gap")) {
                return ((ScaledUnit)style.get("icon-gap")).getFloatValue();
            }
            return -1;
        }

        public byte getThemeIconGapUnit(Map<String, LexicalUnit> style) {
            if (style.containsKey("icon-gap")) {
                FloatValue value = getFloatValue(style.get("icon-gap"));
                return value.unit;
            }
            return -1;
        }

        public boolean getThemeSurface(Map<String, LexicalUnit> style) {
            if (style.containsKey("surface")) {
                ScaledUnit su = (ScaledUnit)style.get("surface");
                if ("true".equalsIgnoreCase(su.getStringValue())) return true;
                if (su.getIntegerValue() != 0) return true;
            }
            return false;
        }
        
        public byte[] getThemeMarginUnit(Map<String,LexicalUnit> style) {
            if (!style.containsKey("margin-left") && !style.containsKey("margin-top") && !style.containsKey("margin-bottom") && !style.containsKey("margin-right")) {
                return null;
            }
            Insets i = getInsets("margin", style);
            return new byte[]{i.topUnit, i.leftUnit, i.bottomUnit, i.rightUnit};
        }
        
        public Integer getThemeAlignment(Map<String,LexicalUnit> style) {
            LexicalUnit value = style.get("text-align");
            if (value == null) {
                return null;
            }
            
            switch (value.getLexicalUnitType()) {
                case LexicalUnit.SAC_IDENT:
                    switch (value.getStringValue()) {
                        case "center" :
                            return Component.CENTER;
                        case "left" :
                            return Component.LEFT;
                        case "right":
                            return Component.RIGHT;
                        default :
                            throw new RuntimeException("Unsupported alignment "+value);
                            
                    }
                
                default:
                    throw new RuntimeException("Unsupported lexical unit type for text-align "+value.getLexicalUnitType());
            }
            
            
        }
        
        FontFace findFontFace(String family) {
            for (FontFace f : fontFaces) {
                if (f.fontFamily != null && family.equals(f.fontFamily.getStringValue())) {
                    return f;
                }
            }
            return null;
        }
        
        private static final int TTF_SIZE_TYPE_PIXELS=-1;
        private static final int TTF_SIZE_TYPE_SMALL=0;
        private static final int TTF_SIZE_TYPE_MEDIUM=1;
        private static final int TTF_SIZE_TYPE_LARGE=2;
        private static final int TTF_SIZE_TYPE_MM=3;
        private static final int TTF_SIZE_TYPE_REM=5;
        private static final int TTF_SIZE_TYPE_VW=6;
        private static final int TTF_SIZE_TYPE_VH=7;
        private static final int TTF_SIZE_TYPE_VMIN=8;
        private static final int TTF_SIZE_TYPE_VMAX=9;
        
        public com.codename1.ui.Font getThemeFont(Map<String,LexicalUnit> styles) {
            LexicalUnit fontFamily = styles.get("font-family");
            ScaledUnit fontSize = (ScaledUnit)styles.get("font-size");
            LexicalUnit fontStyle = styles.get("font-style");
            LexicalUnit fontWeight = styles.get("font-weight");
            
            File ttfFontFile = null;
            String ttfFontName = "native:MainRegular";
            int ttfSizeType = TTF_SIZE_TYPE_MEDIUM;
            float ttfSize = 0;
            int sysFace = Font.FACE_SYSTEM;
            int sysStyle = Font.STYLE_PLAIN;
            int sysSize = Font.SIZE_MEDIUM;
            
            
            if (fontFamily == null && fontSize == null && fontStyle == null && fontWeight == null) {
                return null;
            }
            
            int iFontFace = Font.FACE_SYSTEM;
            int iFontStyle = Font.STYLE_PLAIN;
            int iFontSizeType = Font.SIZE_MEDIUM;
            
            EditorTTFFont ttfFont = null;
            
            loop : while (fontSize != null) {
                outer: switch (fontSize.getLexicalUnitType()) {
                    case LexicalUnit.SAC_IDENT: {
                        switch (fontSize.getStringValue().toLowerCase()) {
                            case "small":
                            case "x-small":
                            case "xx-small":
                                iFontSizeType = Font.SIZE_SMALL;
                                ttfSizeType = TTF_SIZE_TYPE_SMALL;
                                sysSize = Font.SIZE_SMALL;
                                break outer;
                            case "large":
                            case "x-large":
                            case "xx-large":
                                iFontSizeType = Font.SIZE_LARGE;
                                ttfSizeType = TTF_SIZE_TYPE_LARGE;
                                sysSize = Font.SIZE_LARGE;
                                break outer;
                                
                        }
                    }
                    case LexicalUnit.SAC_PIXEL:
                        ttfSizeType = TTF_SIZE_TYPE_PIXELS;
                        ttfSize = fontSize.getIntegerValue();
                        if (ttfSize == 0) {
                            ttfSize = fontSize.getFloatValue();
                        }
                        break;
                        
                    case LexicalUnit.SAC_POINT:
                        ttfSizeType = TTF_SIZE_TYPE_MM;
                        ttfSize = (float)fontSize.getNumericValue() / 72f * 25.4f;
                        break;
                    case LexicalUnit.SAC_MILLIMETER:
                        ttfSizeType = TTF_SIZE_TYPE_MM;
                        ttfSize = (float)fontSize.getNumericValue();
                        break;
                        
                    case LexicalUnit.SAC_CENTIMETER:
                        ttfSizeType = TTF_SIZE_TYPE_MM;
                        ttfSize = (float)fontSize.getNumericValue() * 10;
                        break;
                    case LexicalUnit.SAC_INCH:
                        ttfSizeType = TTF_SIZE_TYPE_MM;
                        ttfSize = (float)fontSize.getNumericValue() * 25.4f;
                        break; 
                        
                    case LexicalUnit.SAC_PERCENTAGE:
                        
                        if (ttfSizeType == TTF_SIZE_TYPE_PIXELS) {
                            ttfSize = ttfSize * (float)fontSize.getNumericValue() / 100f;
                        } else if (ttfSizeType == TTF_SIZE_TYPE_MM) {
                            ttfSize = ttfSize * (float)fontSize.getNumericValue() / 100f;
                        } else if (ttfSizeType == TTF_SIZE_TYPE_MEDIUM) {
                            ttfSizeType = TTF_SIZE_TYPE_MM;
                            ttfSize = 3f * (float)fontSize.getNumericValue() / 100f;
                        } else if (ttfSizeType == TTF_SIZE_TYPE_SMALL) {
                            ttfSizeType = TTF_SIZE_TYPE_MM;
                            ttfSize = 2f * (float)fontSize.getNumericValue() / 100f;
                        } else if (ttfSizeType == TTF_SIZE_TYPE_LARGE) {
                            ttfSizeType = TTF_SIZE_TYPE_MM;
                            ttfSize = 4f * (float)fontSize.getNumericValue() / 100f;
                        }
                        break;
                    default:
                        String unitText = null;
                        try {
                            unitText = fontSize.getDimensionUnitText();

                        } catch (Exception ex) {
                            System.err.println("Warning: No dimension unit supplied for fontSize "+fontSize);
                            ex.printStackTrace();
                        }
                        if (unitText != null) {
                            if ("rem".equals(unitText)) {
                                ttfSizeType = TTF_SIZE_TYPE_REM;
                                ttfSize = (float) fontSize.getNumericValue();
                            } else if ("vw".equals(unitText)) {
                                ttfSizeType = TTF_SIZE_TYPE_VW;
                                ttfSize = (float) fontSize.getNumericValue();

                            } else if ("vh".equals(unitText)) {
                                ttfSizeType = TTF_SIZE_TYPE_VH;
                                ttfSize = (float) fontSize.getNumericValue();

                            } else if ("vmin".equals(unitText)) {
                                ttfSizeType = TTF_SIZE_TYPE_VMIN;
                                ttfSize = (float) fontSize.getNumericValue();

                            } else if ("vmax".equals(unitText)) {
                                ttfSizeType = TTF_SIZE_TYPE_VMAX;
                                ttfSize = (float) fontSize.getNumericValue();

                            }
                        }
                        
                        
                }
                fontSize = (ScaledUnit)fontSize.getNextLexicalUnit();
            }
            //fontSize = styles.get("font-size");
            loop : while (fontStyle != null) {
                switch (fontStyle.getStringValue()) {
                    case "normal" :
                        iFontStyle = Font.STYLE_PLAIN;
                        sysStyle = Font.STYLE_PLAIN;
                        break loop;
                    case "italic" :
                    case "oblique" :
                        iFontStyle = Font.STYLE_ITALIC;
                        sysStyle = Font.STYLE_ITALIC;
                        ttfFontName = "native:ItalicRegular";
                        break loop;
                        
                }
                fontStyle = fontStyle.getNextLexicalUnit();
            }
            
            loop : while (fontWeight != null) {
                switch (fontWeight.getStringValue()) {
                    case "bold" :
                        iFontStyle = Font.STYLE_BOLD;
                        sysStyle = sysStyle | Font.STYLE_BOLD;
                        if ((sysStyle & Font.STYLE_ITALIC) != 0) {
                            ttfFontName = "native:ItalicBold";
                        } else {
                            ttfFontName = "native:MainBold";
                        }
                        break loop;
                        
                }
                fontWeight = fontWeight.getNextLexicalUnit();
            }
            
            loop : while (fontFamily != null) {
                if (fontFamily.getStringValue() != null) {
                    
                    
                    switch (fontFamily.getStringValue().toLowerCase()) {
                        case "sans-serif" :
                        case "serif" :
                        case "times" :
                        case "courier" :
                        case "arial" :
                        case "cursive" :
                        case "fantasy" :
                            iFontFace = Font.FACE_SYSTEM;
                            break loop;
                        case "monospace" :
                            iFontFace = Font.FACE_MONOSPACE;
                            break loop;
                        default : {
                            
                            FontFace face = findFontFace(fontFamily.getStringValue());
                            if (face != null) {
                                ttfFontFile = face.getFontFile();
                                
                            } else {
                                
                                if(fontFamily.getStringValue().startsWith("native:")) {
                                    ttfFontName = fontFamily.getStringValue();
                                    
                                }
                            }
                        }   
                            
                    }
                }
                fontFamily = fontFamily.getNextLexicalUnit();
            }
            if (iFontFace == Font.FACE_MONOSPACE) {
                return Font.createSystemFont(iFontFace, iFontStyle, sysSize);
            } else {
                if (ttfFontFile != null) {
                    return new EditorTTFFont(ttfFontFile, ttfSizeType, ttfSize, Font.createSystemFont(sysFace, sysStyle, sysSize));
                } else {
                    return new EditorTTFFont(ttfFontName, ttfSizeType, ttfSize, Font.createSystemFont(sysFace, sysStyle, sysSize));
                }
            }
            
            
        }
        
        
        
        public int mm2px(float mm) {
            int out = (int)Math.ceil(mm / 25.4f * 72f);
            if (out == 0 && mm > 0) {
                out = 1;
            } else if (out == 0 && mm < 0) {
                out = -1;
            }
            return out;
        }
        
        public float mm2in(float mm) {
            return mm / 25.4f;
        }
        
        public float in2mm(float inches) {
            return inches * 25.4f;
        }
        
        public float px2mm(int px) {
            return px / 72f * 25.4f;
        }
        
        public float pt2mm(float pt) {
            return pt / 72f * 25.4f;
        }
        
        public int in2px(float inches) {
            int out =  (int)Math.round(inches * 72);
            if (out == 0 && inches > 0) {
                out = 1;
            } else if (out == 0 && inches < 0) {
                out = -1;
            }
            return out;
        }
        
        
        public String getThemeOpacity(Map<String,LexicalUnit> styles) {
            if (styles.get("opacity") != null && !requiresImageBorder(styles) && !requiresBackgroundImageGeneration(styles)) {
                double opacity = ((ScaledUnit)styles.get("opacity")).getNumericValue();
                return ""+(int)(opacity * 255.0);
            }
            return null;
        }

        public Integer getThemeFgAlpha(Map<String,LexicalUnit> styles) {
            LexicalUnit fgColor = styles.get("color");
            if (fgColor == null) {
                return null;
            }
            while (fgColor != null) {
                if ("transparent".equals(fgColor.getStringValue())) {
                    return 0;
                }
                if ("rgb".equals(fgColor.getFunctionName()) || "cn1rgb".equals(fgColor.getFunctionName())) {
                    return null;
                } if ("rgba".equals(fgColor.getFunctionName()) || "cn1rgba".equals(fgColor.getFunctionName())) {
                    ScaledUnit r = (ScaledUnit)fgColor.getParameters();
                    ScaledUnit g = r.getNextNumericUnit();
                    ScaledUnit b = g.getNextNumericUnit();
                    ScaledUnit a = b.getNextNumericUnit();

                    return (int)(a.getNumericValue()*255.0);
                } else {
                    return null;
                }
                //bgColor = bgColor.getNextLexicalUnit();
            }
            return null;
        }

        public String getThemeTransparency(Map<String,LexicalUnit> styles) {
            
            ScaledUnit background = (ScaledUnit)styles.get("background");
            if (!requiresBackgroundImageGeneration(styles) && !requiresImageBorder(styles) && background != null && background.isCN1Gradient()) {
                return String.valueOf(background.getCN1Gradient().getBgTransparency());
            }
            
            LexicalUnit cn1BgType = styles.get("cn1-background-type");
            if (cn1BgType != null && "none".equals(cn1BgType.getStringValue()) && !styles.containsKey("background-color")) {
                return "0";
            }
            
            if (requiresImageBorder(styles)) {
                // If there is an image border, we don't want background color messing things up
                return "0";
            }
            
        LexicalUnit bgColor = styles.get("background-color");
        if (bgColor == null) {
            return null;
        }
        while (bgColor != null) {
            if ("transparent".equals(bgColor.getStringValue())) {
                return "0";
            }
            if ("rgb".equals(bgColor.getFunctionName()) || "cn1rgb".equals(bgColor.getFunctionName())) {
                return "255";
            } if ("rgba".equals(bgColor.getFunctionName()) || "cn1rgba".equals(bgColor.getFunctionName())) {
                ScaledUnit r = (ScaledUnit)bgColor.getParameters();
                ScaledUnit g = r.getNextNumericUnit();
                ScaledUnit b = g.getNextNumericUnit();
                ScaledUnit a = b.getNextNumericUnit();
                
                return String.valueOf((int)(a.getNumericValue()*255.0));
            } else {
                return "255";
            }
            //bgColor = bgColor.getNextLexicalUnit();
        }
        
        return null;
    }
     
        private int getShadowSpreadPx(com.codename1.ui.plaf.Border b) {
            if (b instanceof RoundBorder) {
                return ((RoundBorder)b).getShadowSpread();
            }
            if (b instanceof RoundRectBorder) {
                return Display.getInstance().convertToPixels(((RoundRectBorder)b).getShadowSpread());
            }
            return 0;
        }
        
        private float calculateShadowRatio(com.codename1.ui.plaf.Border out, boolean spreadMM, float spreadMMVal, ScaledUnit value) {
            float val = (float)value.getNumericValue();
            if (val == 0 || getShadowSpreadPx(out) == 0) {
                // leave alone
                if (val == 0) {
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_REAL:
                        case LexicalUnit.SAC_INTEGER:
                            break;
                        default:
                            val += 0.5;
                    }
                }
            } else {
                switch (value.getLexicalUnitType()) {
                    case LexicalUnit.SAC_REAL:
                    case LexicalUnit.SAC_INTEGER:
                        //out.shadowX((int)val);
                        break;
                    case LexicalUnit.SAC_PIXEL: {
                        if (spreadMM) {
                            val = -px2mm((int)val) / spreadMMVal / 2;
                        } else {
                            val = -val / getShadowSpreadPx(out) / 2;
                        }
                        val += 0.5;
                        break;
                    }
                    case LexicalUnit.SAC_POINT: {
                        if (spreadMM) {
                            val = -pt2mm(val) / spreadMMVal / 2;
                        } else {
                            val = -val / getShadowSpreadPx(out) / 2;
                        }
                        val += 0.5;
                        break;

                    }   
                    case LexicalUnit.SAC_MILLIMETER: {

                        if (spreadMM) {
                            val = -val/spreadMMVal / 2;
                        } else {
                            val = -mm2px(val)/getShadowSpreadPx(out) / 2;
                        }
                        val += 0.5;
                        break;
                    }  
                    case LexicalUnit.SAC_CENTIMETER: {
                        if (spreadMM) {
                            val = -val/spreadMMVal*10f / 2;
                        } else {
                            val = -mm2px(val*10f)/getShadowSpreadPx(out) /2 ;
                        }
                        val += 0.5;
                        break;
                    }
                    case LexicalUnit.SAC_INCH:{
                        if (spreadMM) {
                            val = -in2mm(val)/spreadMMVal / 2;
                        } else {
                            val = -in2px(val)/getShadowSpreadPx(out) / 2;
                        }
                        val += 0.5;
                        break;
                    }
                    default:
                        System.err.println("In file "+baseURL);
                        System.err.println("Unsupported unit for cn1-box-shadow-h: "+value.getLexicalUnitType()+". Setting shadowX to 0");
                        val = 0;
                }
            }
            return val;
        }
        
        
        private com.codename1.ui.plaf.Border createRoundBorder(Map<String,LexicalUnit> styles) {
            // We create a round border
            LexicalUnit backgroundColor = styles.get("background-color");

            LexicalUnit borderColor = styles.get("border-top-color");
            ScaledUnit borderWidth = (ScaledUnit)styles.get("border-top-width");


            com.codename1.ui.plaf.RoundBorder out = RoundBorder.create();
            if (isPillPorder(styles)) {
                out.rectangle(true);
            } else {
                out.rectangle(false);
            }
            if (borderWidth != null) {
                switch (borderWidth.getLexicalUnitType()) {
                    case LexicalUnit.SAC_MILLIMETER:
                        out.stroke((float)borderWidth.getNumericValue(), true);
                        break;
                    case LexicalUnit.SAC_INTEGER:
                    case LexicalUnit.SAC_REAL:
                    case LexicalUnit.SAC_POINT:
                        out.stroke((float)borderWidth.getNumericValue() * 25.4f / 72f, true);
                        break;
                    case LexicalUnit.SAC_PIXEL:
                        out.stroke((int)borderWidth.getNumericValue(), false);
                        break;
                    case LexicalUnit.SAC_CENTIMETER:
                        out.stroke((float)borderWidth.getNumericValue() * 10, true);
                        break;
                    case LexicalUnit.SAC_INCH:
                        out.stroke((float)borderWidth.getNumericValue() * 25.4f, true);
                        break;
                    default:
                        System.err.println("In file "+baseURL);
                        System.err.println("Invalid border width unit " + borderWidth.getLexicalUnitType()+". Setting border width to 1");
                        out.stroke(1, false);

                }
            } else {
                out.stroke(1, false);
            }

            if (backgroundColor != null) {
                out.color(getColorInt(backgroundColor));
                Integer alpha = getColorAlphaInt(backgroundColor);
                if (alpha != null) {
                    out.opacity(alpha);
                } else {
                    out.opacity(255);
                }
            } else {
                out.opacity(0);
            }

            if (borderColor != null) {
                out.strokeColor(getColorInt(borderColor));
                Integer alpha = getColorAlphaInt(borderColor);
                if (alpha != null) {
                    out.strokeOpacity(alpha);
                } else {
                    out.strokeOpacity(255);
                }
            } else {
                out.strokeOpacity(0);
            }

            /*

            apply(style, "cn1-box-shadow-inset", value);
                apply(style, "cn1-box-shadow-color", value);
                apply(style, "cn1-box-shadow-spread", value);
                apply(style, "cn1-box-shadow-blur", value);
                apply(style, "cn1-box-shadow-h", value);
                apply(style, "cn1-box-shadow-v", value);
            */


            ScaledUnit shadowSpread = (ScaledUnit)styles.get("cn1-box-shadow-spread");
            boolean spreadMM = false;
            float spreadMMVal = 0f;
            if (shadowSpread != null) {
                switch (shadowSpread.getLexicalUnitType()) {

                    case LexicalUnit.SAC_PIXEL:
                        out.shadowSpread((int)shadowSpread.getNumericValue());
                        break;
                    case LexicalUnit.SAC_MILLIMETER:
                        spreadMMVal = (float)Math.max(1, Math.round(shadowSpread.getNumericValue()));
                        spreadMM = true;
                        out.shadowSpread((int)spreadMMVal, true);
                        break;
                    case LexicalUnit.SAC_INCH:
                        spreadMMVal = (float)Math.max(1, Math.round(in2mm((float)shadowSpread.getNumericValue())));
                        spreadMM = true;
                        out.shadowSpread((int)spreadMMVal, true);
                        break;
                    case LexicalUnit.SAC_CENTIMETER:
                        spreadMMVal = (float)Math.max(1, Math.round(10*shadowSpread.getNumericValue()));
                        spreadMM = true;
                        out.shadowSpread((int)spreadMMVal, true);
                        break;
                    case LexicalUnit.SAC_POINT:
                        spreadMMVal = (float)Math.max(1, Math.round(pt2mm((float)shadowSpread.getNumericValue())));
                        spreadMM = true;
                        out.shadowSpread((int)spreadMMVal, true);
                        break;
                    default:
                        System.err.println("In file "+baseURL);
                        System.err.println("Unsupported unit for cn1-box-shadow-spread: "+shadowSpread.getLexicalUnitType()+". Setting shadow spread to 0");
                        out.shadowSpread(0);


                }
            }

            ScaledUnit shadowH = (ScaledUnit)styles.get("cn1-box-shadow-h");
            if (shadowH != null) {
                out.shadowX(calculateShadowRatio(out, spreadMM, spreadMMVal, shadowH));
            }
            ScaledUnit shadowV = (ScaledUnit)styles.get("cn1-box-shadow-v");
            if (shadowV != null) {
                out.shadowY(calculateShadowRatio(out, spreadMM, spreadMMVal, shadowV));
            }

            ScaledUnit boxShadowBlur = (ScaledUnit)styles.get("cn1-box-shadow-blur");
            if (boxShadowBlur != null) {
                out.shadowBlur(-calculateShadowRatio(out, spreadMM, spreadMMVal, boxShadowBlur));
            }

            LexicalUnit shadowColor = styles.get("cn1-box-shadow-color");
            if (shadowColor != null) {
                System.err.println("In file "+baseURL);
                System.err.println("Shadow color not supported for background type cn1-round-border.  Ignoring RGB Portion  Only using Alpha");
                Integer alpha = getColorAlphaInt(shadowColor);
                if (alpha != null) {
                    out.shadowOpacity(alpha);
                }

            }



            
            return out;
        }
        
        private ScaledUnit getBorderRadius(Map<String,LexicalUnit> styles, String corner) {
            return (ScaledUnit)styles.get("cn1-border-"+corner+"-radius-x");
        }
        
        
        
        private ScaledUnit getBorderRadius(Map<String,LexicalUnit> styles) {
            String prefix = "cn1-border";
            String[] corners = new String[]{"top-left", "top-right", "bottom-left", "bottom-right"};
            String[] xy = new String[]{"x", "y"};
            
            String[] radiusAtts = new String[8];
            int i =0;
            for (String axis : xy) {
                for (String corner : corners) {
                    radiusAtts[i++] = prefix+"-"+corner+"-radius-"+axis;
                }
            }
            
            
            for (String cornerStyle : radiusAtts) {
                ScaledUnit u = (ScaledUnit)styles.get(cornerStyle);
                
                if (u != null && u.getPixelValue() != 0) {
                    return u;
                }
            }
            return null;
        }
        
        private com.codename1.ui.plaf.Border createUnderlineBorder(Map<String,LexicalUnit> styles) {
            LexicalUnit borderColor = styles.get("border-bottom-color");
            ScaledUnit borderWidth = (ScaledUnit)styles.get("border-bottom-width");
            float borderWidthF=-1;
            int borderWidthI=-1;
            if (borderWidth != null) {
                switch (borderWidth.getLexicalUnitType()) {
                    case LexicalUnit.SAC_MILLIMETER:
                        borderWidthF = (float)borderWidth.getNumericValue();
                        break;
                    case LexicalUnit.SAC_INTEGER:
                    case LexicalUnit.SAC_REAL:
                    case LexicalUnit.SAC_POINT:
                        borderWidthF = (float)borderWidth.getNumericValue() * 25.4f / 72f;
                        break;
                    case LexicalUnit.SAC_PIXEL:
                        borderWidthI = (int)borderWidth.getNumericValue();
                        break;
                    case LexicalUnit.SAC_CENTIMETER:
                        borderWidthF = (float)borderWidth.getNumericValue() * 10;
                        break;
                    case LexicalUnit.SAC_INCH:
                        borderWidthF = (float)borderWidth.getNumericValue() * 25.4f;
                        break;
                    default:
                        borderWidthI = 1;

                }
                
            } else {
                borderWidthI = 1;
            }
            if (borderColor != null) {
                int borderColorI = getColorInt(borderColor);
                if (borderWidthI > 0) {
                    return com.codename1.ui.plaf.Border.createUnderlineBorder(borderWidthI, borderColorI);
                } else {
                    return com.codename1.ui.plaf.Border.createUnderlineBorder(borderWidthF, borderColorI);
                }
            } else {
                if (borderWidthI > 0) {
                    return com.codename1.ui.plaf.Border.createUnderlineBorder(borderWidthI);
                } else {
                    return com.codename1.ui.plaf.Border.createUnderlineBorder(borderWidthF);
                }
            }
        }
        
        
        
        private com.codename1.ui.plaf.Border createCSSBorder(Map<String,LexicalUnit> styles) {
            CSSBorder out = new CSSBorder(res);
            ScaledUnit topLeftRadius = getBorderRadius(styles, "top-left");
            ScaledUnit topRightRadius = getBorderRadius(styles, "top-right");
            ScaledUnit bottomLeftRadius = getBorderRadius(styles, "bottom-left");
            ScaledUnit bottomRightRadius = getBorderRadius(styles, "bottom-right");
            StringBuilder radiusString = new StringBuilder();
            if (topLeftRadius != null) {
                radiusString.append(str(topLeftRadius, "0")).append(" ")
                        .append(str(topRightRadius, "0")).append(" ")
                        .append(str(bottomRightRadius, "0")).append(" ")
                        .append(str(bottomLeftRadius, "0"));
                out.borderRadius(radiusString.toString());
            }
            String[] borderColors = new String[]{
                "border-top-color",
                "border-right-color",
                "border-bottom-color",
                "border-left-color",
            };
            StringBuilder colorString = new StringBuilder();
            for (String colorKey : borderColors) {
                LexicalUnit borderColor = styles.get(colorKey);
                if (borderColor != null) {
                    colorString.append(getARGBHexString(borderColor)).append(" ");
                } else {
                    colorString.append("transparent ");
                }
                
            }
            
            if (colorString.length() > 0) {
                String str = colorString.toString().trim();
                if (str.length() > 0) {
                    out.borderColor(str);
                }
            }
            String[] borderWidths = new String[]{
                "border-top-width",
                "border-right-width",
                "border-bottom-width",
                "border-left-width",
            };
            StringBuilder widthString = new StringBuilder();
            for (String widthKey: borderWidths) {
                LexicalUnit borderWidth = styles.get(widthKey);
                widthString.append(str(borderWidth, "0")).append(" ");
                
            }
            
            if (widthString.length() > 0) {
                String str = widthString.toString().trim();
                if (str.length() > 0) {
                    out.borderWidth(str);
                }
            }
            String[] borderStyles = new String[]{
                "border-top-style",
                "border-right-style",
                "border-bottom-style",
                "border-left-style",
            };
            LexicalUnit borderStyle = styles.get("border-top-style");
            StringBuilder styleString = new StringBuilder();
            for (String styleKey : borderStyles) {
                borderStyle = styles.get(styleKey);
                if (borderStyle != null) {
                    styleString.append(borderStyle.getStringValue()).append(" ");
                } else {
                    styleString.append("none").append(" ");
                }
                
            }
            
            if (styleString.length() > 0) {
                String str = styleString.toString().trim();
                if (str.length() > 0) {
                    out.borderStyle(str);
                }
            }
            
            LexicalUnit bgColor = styles.get("background-color");
            if (bgColor != null) {
                out.backgroundColor(getARGBHexString(bgColor));
            }
            
            ScaledUnit borderImage = (ScaledUnit)styles.get("border-image");
            
            if (!isNone(borderImage)) {
                Image img = getBorderImage(styles);
                ScaledUnit sliceUnit = (ScaledUnit)styles.get("border-image-slice");
                List<Double> slices = new ArrayList<>();
                while (!isNone(sliceUnit)) {
                    
                    
                    if (sliceUnit.getLexicalUnitType() == LexicalUnit.SAC_PERCENTAGE) {
                        slices.add(sliceUnit.getNumericValue() / 100.0);
                    } else {
                        
                    }
                    sliceUnit = (ScaledUnit)sliceUnit.getNextLexicalUnit();
                    
                }
                
                if (slices.isEmpty()) {
                    slices.add(0.4);
                }
                double[] slicesArr = new double[slices.size()];
                for (int i=0; i<slices.size(); i++) {
                    slicesArr[i] = slices.get(i).doubleValue();
                }
                out.borderImageWithName(img.getImageName(), slicesArr);
                
            }
            
            return out;
                
        }
        
        private com.codename1.ui.plaf.Border createRoundRectBorder(Map<String,LexicalUnit> styles) {
            // We create a round border
            //LexicalUnit backgroundColor = styles.get("background-color");

            LexicalUnit borderColor = styles.get("border-top-color");
            ScaledUnit borderWidth = (ScaledUnit)styles.get("border-top-width");


            com.codename1.ui.plaf.RoundRectBorder out = RoundRectBorder.create();
            
            ScaledUnit radius = getBorderRadius(styles);
            if (radius != null) {
                out.cornerRadius(radius.getMMValue());
            } else {
                out.cornerRadius(0);
            }
            
            ScaledUnit topLeftRadius = getBorderRadius(styles, "top-left");
            ScaledUnit topRightRadius = getBorderRadius(styles, "top-right");
            ScaledUnit bottomLeftRadius = getBorderRadius(styles, "bottom-left");
            ScaledUnit bottomRightRadius = getBorderRadius(styles, "bottom-right");
            
            
            //if (!isZero(bottomLeftRadius) && !isZero(bottomRightRadius) && isZero(topLeftRadius) && isZero(topRightRadius)) {
            //    out.bottomOnlyMode(true);
            //} else if (isZero(bottomLeftRadius) && isZero(bottomRightRadius) && !isZero(topLeftRadius) && !isZero(topRightRadius)) {
            //    out.topOnlyMode(true);
            //}
            out.topLeftMode(!isZero(topLeftRadius));
            out.topRightMode(!isZero(topRightRadius));
            out.bottomRightMode(!isZero(bottomRightRadius));
            out.bottomLeftMode(!isZero(bottomLeftRadius));
            
            
            
            if (borderWidth != null) {
                switch (borderWidth.getLexicalUnitType()) {
                    case LexicalUnit.SAC_MILLIMETER:
                        out.stroke((float)borderWidth.getNumericValue(), true);
                        break;
                    case LexicalUnit.SAC_INTEGER:
                    case LexicalUnit.SAC_REAL:
                    case LexicalUnit.SAC_POINT:
                        out.stroke((float)borderWidth.getNumericValue() * 25.4f / 72f, true);
                        break;
                    case LexicalUnit.SAC_PIXEL:
                        out.stroke((int)borderWidth.getNumericValue(), false);
                        break;
                    case LexicalUnit.SAC_CENTIMETER:
                        out.stroke((float)borderWidth.getNumericValue() * 10, true);
                        break;
                    case LexicalUnit.SAC_INCH:
                        out.stroke((float)borderWidth.getNumericValue() * 25.4f, true);
                        break;
                    default:
                        System.err.println("In file "+baseURL);
                        System.err.println("Invalid border width unit " + borderWidth.getLexicalUnitType()+". Setting border width to 1");
                        out.stroke(1, false);

                }
            } else {
                out.stroke(1, false);
            }

            

            if (borderColor != null) {
                out.strokeColor(getColorInt(borderColor));
                Integer alpha = getColorAlphaInt(borderColor);
                if (alpha != null) {
                    out.strokeOpacity(alpha);
                } else {
                    out.strokeOpacity(255);
                }
            } else {
                out.strokeOpacity(0);
            }

            
            /*

            apply(style, "cn1-box-shadow-inset", value);
                apply(style, "cn1-box-shadow-color", value);
                apply(style, "cn1-box-shadow-spread", value);
                apply(style, "cn1-box-shadow-blur", value);
                apply(style, "cn1-box-shadow-h", value);
                apply(style, "cn1-box-shadow-v", value);
            */


            ScaledUnit shadowSpread = (ScaledUnit)styles.get("cn1-box-shadow-spread");
            boolean spreadMM = false;
            float spreadMMVal = 0f;
            if (shadowSpread != null) {
                switch (shadowSpread.getLexicalUnitType()) {

                    case LexicalUnit.SAC_PIXEL:
                        out.shadowSpread((int)shadowSpread.getNumericValue());
                        break;
                    case LexicalUnit.SAC_MILLIMETER:
                        spreadMMVal = (float)Math.max(1, Math.round(shadowSpread.getNumericValue()));
                        spreadMM = true;
                        out.shadowSpread((float)spreadMMVal);
                        break;
                    case LexicalUnit.SAC_INCH:
                        spreadMMVal = (float)Math.max(1, Math.round(in2mm((float)shadowSpread.getNumericValue())));
                        spreadMM = true;
                        out.shadowSpread((float)spreadMMVal);
                        break;
                    case LexicalUnit.SAC_CENTIMETER:
                        spreadMMVal = (float)Math.max(1, Math.round(10*shadowSpread.getNumericValue()));
                        spreadMM = true;
                        out.shadowSpread((float)spreadMMVal);
                        break;
                    case LexicalUnit.SAC_POINT:
                        spreadMMVal = (float)Math.max(1, Math.round(pt2mm((float)shadowSpread.getNumericValue())));
                        spreadMM = true;
                        out.shadowSpread((float)spreadMMVal);
                        break;
                    default:
                        System.err.println("In file "+baseURL);
                        System.err.println("Unsupported unit for cn1-box-shadow-spread: "+shadowSpread.getLexicalUnitType()+". Setting shadow spread to 0");
                        out.shadowSpread(0);


                }
            }

            ScaledUnit shadowH = (ScaledUnit)styles.get("cn1-box-shadow-h");
            if (shadowH != null) {
                out.shadowX(calculateShadowRatio(out, spreadMM, spreadMMVal, shadowH));
            }
            ScaledUnit shadowV = (ScaledUnit)styles.get("cn1-box-shadow-v");
            if (shadowV != null) {
                out.shadowY(calculateShadowRatio(out, spreadMM, spreadMMVal, shadowV));
            }

            ScaledUnit boxShadowBlur = (ScaledUnit)styles.get("cn1-box-shadow-blur");
            if (boxShadowBlur != null) {
                out.shadowBlur(-calculateShadowRatio(out, spreadMM, spreadMMVal, boxShadowBlur));
            }

            LexicalUnit shadowColor = styles.get("cn1-box-shadow-color");
            if (shadowColor != null) {
                System.err.println("In file "+baseURL);
                System.err.println("Shadow color not supported for background type cn1-round-border.  Ignoring RGB Portion  Only using Alpha");
                Integer alpha = getColorAlphaInt(shadowColor);
                if (alpha != null) {
                    out.shadowOpacity(alpha);
                }

            }



            
            return out;
        }
        public com.codename1.ui.plaf.Border getThemeBorder(Map<String,LexicalUnit> styles) {
            Border b = this.createBorder(styles);
            LexicalUnit cn1BackgroundType = styles.get("cn1-background-type");
            
            
            if (cn1BackgroundType != null && usesRoundBorder(styles)) {
                return createRoundBorder(styles);
            }
            if (b.canBeAchievedWithCSSBorder(styles) && (b.hasBorderImage() || b.hasUnequalBorders() || b.hasBorderRadius()) ) {
                return createCSSBorder(styles);
            }
            if (b.canBeAchievedWithRoundRectBorder(styles) && b.hasBorderRadius()) {
                return createRoundRectBorder(styles);
            }
            //if (b.canBeAchievedWithUnderlineBorder(styles)) {
            //    return createUnderlineBorder(styles);
            //}
            if (b.hasUnequalBorders()) {
                
                return com.codename1.ui.plaf.Border.createCompoundBorder(
                        getThemeBorder(styles, "top"),
                        getThemeBorder(styles, "bottom"),
                        getThemeBorder(styles, "left"),
                        getThemeBorder(styles, "right")
                );    
            } else {
                
                return getThemeBorder(styles, "top");
            }
        }
        
        
        
        
        public String getThemeDerive(Map<String,LexicalUnit> styles, String suffix) {
            
            LexicalUnit derive = styles.get("cn1-derive");
            
            if (derive != null) {
                return derive.getStringValue()+suffix;
            }
            return null;
        }
               
        
        public com.codename1.ui.plaf.Border getThemeBorder(Map<String,LexicalUnit> styles, String side) {
            
            ScaledUnit topColor = (ScaledUnit)styles.get("border-"+side+"-color");
            ScaledUnit topStyle = (ScaledUnit)styles.get("border-"+side+"-style");
            ScaledUnit topWidth = (ScaledUnit)styles.get("border-"+side+"-width");
            
            if (topStyle == null) {
                return null;
            }
            
            int color = 0;
            
            if (topColor != null) {
                color = getColorInt(topColor);
            }
            
            int thickness = 1;
            if (topWidth != null) {
                switch (topWidth.getLexicalUnitType()) {
                    case LexicalUnit.SAC_PIXEL:
                    case LexicalUnit.SAC_POINT:
                        thickness = (int)topWidth.getFloatValue();
                        break;
                    case LexicalUnit.SAC_INTEGER:
                        thickness = (int)topWidth.getIntegerValue();
                        break;
                    case LexicalUnit.SAC_MILLIMETER:
                        thickness = (int)(topWidth.getFloatValue()*10f);
                        break;
                    case LexicalUnit.SAC_PERCENTAGE:
                        thickness = (int)(topWidth.getFloatValue()*topWidth.screenWidth/100f);
                        break;
                }
            }
            
            
            
            //com.codename1.ui.plaf.Border out;
            switch (topStyle.getStringValue()) {
                case "none" :
                case "hidden" :
                case "inherit":
                case "initial":
                    return com.codename1.ui.plaf.Border.createEmpty();
                //case "dotted" :
                //    return com.codename1.ui.plaf.Border.createDottedBorder(thickness, color);
                //case "dashed" :
                
                //    com.codename1.ui.plaf.Border br =  com.codename1.ui.plaf.Border.createDashedBorder(thickness, color);
                
                //    return br;
                case "etched" :
                    return com.codename1.ui.plaf.Border.createEtchedLowered(0xffffff, color);
                case "solid" :
                    return com.codename1.ui.plaf.Border.createLineBorder(thickness, color);
                //case "double":
                //    return com.codename1.ui.plaf.Border.createDoubleBorder(thickness, color);
                //case "groove":
                //    return com.codename1.ui.plaf.Border.createGrooveBorder(thickness, color);
                //case "ridge":
                //    return com.codename1.ui.plaf.Border.createRidgeBorder(thickness, color);
                //case "inset":
                //    return com.codename1.ui.plaf.Border.createInsetBorder(thickness, color);
                //case "outset":
                //    return com.codename1.ui.plaf.Border.createOutsetBorder(thickness, color);
                default:
                    throw new RuntimeException("Unsupported border type "+topStyle+" for side "+side);
            }
        }
        
        public Byte getThemeTextDecoration(Map<String,LexicalUnit> style) {
            LexicalUnit value = style.get("text-decoration");
            if (value == null) {
                return null;
            }
            
            switch (value.getLexicalUnitType()) {
                case LexicalUnit.SAC_IDENT :
                    switch (value.getStringValue()) {
                        case "underline" :
                            return Style.TEXT_DECORATION_UNDERLINE;
                        case "overline" :
                            return Style.TEXT_DECORATION_OVERLINE;
                        case "line-through" :
                            return Style.TEXT_DECORATION_STRIKETHRU;
                        case "none" :
                            return Style.TEXT_DECORATION_NONE;
                        case "cn1-3d" :
                            return Style.TEXT_DECORATION_3D;
                        case "cn1-3d-lowered" :
                            return Style.TEXT_DECORATION_3D_LOWERED;
                        case "cn1-3d-shadow-north" :
                            return Style.TEXT_DECORATION_3D_SHADOW_NORTH;
                        default :
                            throw new RuntimeException("Unsupported text decoration value "+value);
                    }
                default :
                    throw new RuntimeException("Unsupported lexical unit type for text-decoration "+value.getLexicalUnitType());
            }
        }
    }
    
    
    int getPixelValue(LexicalUnit value) {
        if (isNone(value)){
            return 0;
        }
        return ((ScaledUnit)value).getPixelValue();
        
    }
    
    int getPixelValue(LexicalUnit value, double targetDpi) {
        if (isNone(value)){
            return 0;
        }
        return ((ScaledUnit)value).getPixelValue(targetDpi);
        
    }
    
    
    private static boolean isNone(LexicalUnit value) {
        if (value == null || "none".equals(value.getStringValue())) {
            return true;
        }
        return false;
    }
    
    String renderAsCSSString(String property, Map<String,LexicalUnit> styles) {
        return renderAsCSSString(styles.get(property));
    }
    
    
    public void apply(Element style, String property, LexicalUnit value) {
        
        switch (property) {
            case "refresh-images":
                refreshImages = true;
                break;
                
            case "opacity" : {
                style.put("opacity", value);
                break;
            }

            case "elevation" : {
                style.put("elevation", value);
                break;
            }

            case "icon-gap" : {
                style.put("icon-gap", value);
                break;
            }

            case "surface" : {
                style.put("surface", value);
                break;
            }
                
            case "cn1-9patch" : {
                style.put("cn1-9patch", value);
                break;
            }
        
            case "cn1-source-dpi" : {
                style.put("cn1-source-dpi", value);
                break;
            }
            
            case "cn1-derive" : {
                
                String parentName = value.getStringValue();
                style.setParent(value.getStringValue());
                style.put("cn1-derive", value);
                break;
            }
                
            
            case "font" : {
                boolean doneFontStyle = false;
                boolean doneFontFamily = false;
                boolean doneFontSize = false;
                while (value != null) {
                    if (value.getStringValue() != null) {
                        switch (value.getStringValue().toLowerCase()) {
                            case "italic":
                            case "normal":
                            case "oblique" :
                                if (!doneFontStyle) {
                                    apply(style, "font-style", value);
                                    doneFontStyle = true;
                                }
                                break;
                            case "serif" :
                            case "sans-serif" :
                            case "monospace" :
                                if (!doneFontFamily) {
                                    apply(style, "font-family", value);
                                    doneFontFamily = true;
                                }
                                break;
                            case "medium" :
                            case "xx-small" :
                            case "x-small" :
                            case "small" :
                            case "large" :
                            case "x-large" :
                            case "xx-large" :
                            case "smaller" :
                            case "larger" :
                                if (!doneFontSize) {
                                    apply(style, "font-size", value);
                                    doneFontSize = true;
                                }
                                break;
                                
                        }
                        
                    } else {
                        switch (value.getLexicalUnitType()) {
                            case LexicalUnit.SAC_PIXEL:
                            case LexicalUnit.SAC_POINT:
                            case LexicalUnit.SAC_MILLIMETER:
                            case LexicalUnit.SAC_INTEGER :
                            case LexicalUnit.SAC_PERCENTAGE:
                            case LexicalUnit.SAC_EM:
                            case LexicalUnit.SAC_REAL:
                            case LexicalUnit.SAC_INCH:
                            case LexicalUnit.SAC_CENTIMETER:
                                if (!doneFontSize) {
                                    apply(style, "font-size", value);
                                    doneFontSize = true;
                                }
                                break;
                            case LexicalUnit.SAC_STRING_VALUE:
                                if (!doneFontFamily) {
                                    apply(style, "font-family", value);
                                    doneFontFamily = true;
                                }
                            break;
                        }
                    }
                    value = value.getNextLexicalUnit();
                }
                break;
            }
            case "font-family" :
                style.put("font-family", value);
                break;
            case "font-size" :
                style.put("font-size", value);
                break;
                
            case "font-style" :
                style.put("font-style", value);
                break;
                
            case "font-weight" :
                style.put("font-weight", value);
                break;
            
            case "margin-top" :
                style.put("margin-top", value);
                break;
                
            case "margin-left" :
                style.put("margin-left", value);
                break;
            case "margin-right" :
                style.put("margin-right", value);
                break;
            case "margin-bottom" :
                style.put("margin-bottom", value);
                break;
            
            case "margin" : {
                List<LexicalUnit> units = new ArrayList<LexicalUnit>();
                while (value != null) {
                    units.add(value);
                    value = value.getNextLexicalUnit();
                }
                switch (units.size()) {
                    case 1 :
                        apply(style, "margin-top", units.get(0));
                        apply(style, "margin-right", units.get(0));
                        apply(style, "margin-bottom", units.get(0));
                        apply(style, "margin-left", units.get(0));
                        break;
                    case 2 :
                        apply(style, "margin-top", units.get(0));
                        apply(style, "margin-bottom", units.get(0));
                        apply(style, "margin-right", units.get(1));
                        apply(style, "margin-left", units.get(1));
                        break;
                    case 3 :
                        apply(style, "margin-top", units.get(0));
                        apply(style, "margin-right", units.get(1));
                        apply(style, "margin-left", units.get(1));
                        apply(style, "margin-bottom", units.get(2));
                        break;
                    case 4 :
                        apply(style, "margin-top", units.get(0));
                        apply(style, "margin-right", units.get(1));
                        apply(style, "margin-bottom", units.get(2));
                        apply(style, "margin-left", units.get(3));
                        break;
                    default :
                        throw new RuntimeException("Unsupported number of units in margin property "+units.size());
                                
                }
                break;
                /*
                Insets i = getInsets(value);
                style.put("margin",i.top + "," + i.bottom + ","+i.left+","+i.right);
                style.put("marUnit", new byte[]{i.topUnit, i.bottomUnit, i.leftUnit, i.rightUnit});
                break;
                */
            }
            
            case "padding-top" :
                style.put("padding-top", value);
                break;
                
            case "padding-left" :
                style.put("padding-left", value);
                break;
            case "padding-right" :
                style.put("padding-right", value);
                break;
            case "padding-bottom" :
                style.put("padding-bottom", value);
                break;
            
            case "padding" : {
                List<LexicalUnit> units = new ArrayList<LexicalUnit>();
                while (value != null) {
                    units.add(value);
                    value = value.getNextLexicalUnit();
                }
                switch (units.size()) {
                    case 1 :
                        apply(style, "padding-top", units.get(0));
                        apply(style, "padding-right", units.get(0));
                        apply(style, "padding-bottom", units.get(0));
                        apply(style, "padding-left", units.get(0));
                        break;
                    case 2 :
                        apply(style, "padding-top", units.get(0));
                        apply(style, "padding-bottom", units.get(0));
                        apply(style, "padding-right", units.get(1));
                        apply(style, "padding-left", units.get(1));
                        break;
                    case 3 :
                        apply(style, "padding-top", units.get(0));
                        apply(style, "padding-right", units.get(1));
                        apply(style, "padding-left", units.get(1));
                        apply(style, "padding-bottom", units.get(2));
                        break;
                    case 4 :
                        apply(style, "padding-top", units.get(0));
                        apply(style, "padding-right", units.get(1));
                        apply(style, "padding-bottom", units.get(2));
                        apply(style, "padding-left", units.get(3));
                        break;
                    default :
                        throw new RuntimeException("Unsupported number of units in padding property "+units.size());
                                
                }
                break;
                /*
                Insets i = getInsets(value);
                style.put("margin",i.top + "," + i.bottom + ","+i.left+","+i.right);
                style.put("marUnit", new byte[]{i.topUnit, i.bottomUnit, i.leftUnit, i.rightUnit});
                break;
                */
            }
            /*
            case "padding" : {
                Insets i = getInsets(value);
                style.put("padding", i.top+","+i.bottom+","+i.left+","+i.right);
                style.put("padUnit", new byte[]{i.topUnit, i.bottomUnit, i.leftUnit, i.rightUnit});
                break;
            }
            */
            
            case "color" : {
                /*
                Color c= getColor(value);
                style.put("fgColor",String.format( "#%02X%02X%02X",
                    (int)( c.getRed() * 255 ),
                    (int)( c.getGreen() * 255 ),
                    (int)( c.getBlue() * 255 ) ));
                break;
                */
                style.put("color", value);
                break;
            }
            
            case "border-image" : {
                style.put("border-image", value);
                break;
            }
            
            case "border-image-slice" : {
                style.put("border-image-slice" , value);
                break;
            }
            
            case "background-image" : {
                style.put("background-image", value);
                break;
            }
            
            case "background-color" : {
                style.put("background-color", value);
                break;
            }
            
            case "background-repeat" : {
                style.put("background-repeat", value);
                break;
            }
            
            case "width" : {
                style.put("width", value);
                break;
            }
            
            case "height" : {
                style.put("height", value);
                break;
            }
            
            case "cn1-image-id" : {
                style.put("cn1-image-id", value);
                break;
            }
            
            case "min-width" : {
                style.put("min-width", value);
                break;
            }
            
            case "min-height" : {
                style.put("min-height", value);
                break;
            }
            
            case "background-size" : {
                style.put("background-size", value);
                break;
            }
            
            case "text-align" : {
                style.put("text-align", value);
                break;
            }
            
            case "text-decoration" : {
                style.put("text-decoration", value);
                break;
            }
            
            case "cn1-background-type" : {
                style.put("cn1-background-type", value);
                break;
            }
            
            case "background" : {
                
                
                while (value != null) {
                    
                    
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_IDENT:
                            switch (value.getStringValue()) {
                                //{ "IMAGE_SCALED", "IMAGE_SCALED_FILL", "IMAGE_SCALED_FIT", "IMAGE_TILE_BOTH", "IMAGE_TILE_VERTICAL_ALIGN_LEFT", "IMAGE_TILE_VERTICAL_ALIGN_CENTER", "IMAGE_TILE_VERTICAL_ALIGN_RIGHT", "IMAGE_TILE_HORIZONTAL_ALIGN_TOP", "IMAGE_TILE_HORIZONTAL_ALIGN_CENTER", "IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM", "IMAGE_ALIGNED_TOP", "IMAGE_ALIGNED_BOTTOM", "IMAGE_ALIGNED_LEFT", "IMAGE_ALIGNED_RIGHT", "IMAGE_ALIGNED_TOP_LEFT", "IMAGE_ALIGNED_TOP_RIGHT", "IMAGE_ALIGNED_BOTTOM_LEFT", "IMAGE_ALIGNED_BOTTOM_RIGHT", "IMAGE_ALIGNED_CENTER", "GRADIENT_LINEAR_HORIZONTAL", "GRADIENT_LINEAR_VERTICAL", "GRADIENT_RADIAL", "NONE" }));
                                case "cn1-image-scaled":
                                case "cn1-image-scaled-fill":
                                case "cn1-image-scaled-fit":
                                case "cn1-image-tile-both" :
                                case "cn1-image-tile-valign-left" :
                                case "cn1-image-tile-valign-center" :
                                case "cn1-image-tile-valign-right" :
                                case "cn1-image-tile-halign-top" :
                                case "cn1-image-tile-halign-center" :
                                case "cn1-image-tile-halign-bottom" :
                                case "cn1-image-align-top" :
                                case "cn1-image-align-bottom" :
                                case "cn1-image-align-left" :
                                case "cn1-image-align-right" :
                                case "cn1-image-align-center" :
                                case "cn1-image-align-top-left" :
                                case "cn1-image-align-top-right" :
                                case "cn1-image-align-bottom-left" :
                                case "cn1-image-align-bottom-right" :
                                case "cn1-image-border":
                                case "cn1-none" :
                                case "cn1-round-border":
                                case "cn1-pill-border":
                                case "none" :
                                    apply(style, "cn1-background-type", value);
                                    break; 
                                
                            }
                        
                        // no break here because ident could be a color too 
                        // so we let proceed to next (RGB_COLOR).
                        case LexicalUnit.SAC_RGBCOLOR :
                            
                            apply(style, "background-color", value);
                            break;
                        case LexicalUnit.SAC_URI :
                            apply(style, "background-image", value);
                            break;
                        case LexicalUnit.SAC_FUNCTION :
                            switch (value.getFunctionName()) {
                                case "linear-gradient" :
                                case "radial-gradient" :
                                    style.put("background", value);
                                    break;
                                default:
                                    throw new RuntimeException("Unsupported function in background property");
                                    
                            }
                        break;
                        
                        default :
                            throw new RuntimeException("Unsupported lexical type for background "+value.getLexicalUnitType());
                    }
                    value = value.getNextLexicalUnit();
                }
                break;
            }
            case "border-style-top":
            case "border-top-style" : {
                style.put("border-top-style", value);
                break;
            }
            case "border-style-right":
            case "border-right-style" : {
                style.put("border-right-style", value);
                break;
            }
            case "border-style-bottom":
            case "border-bottom-style" : {
                style.put("border-bottom-style", value);
                break;
            }
            case "border-style-left":
            case "border-left-style" : {
                style.put("border-left-style", value);
                break;
            }
            case "border-width-top":
            case "border-top-width" : {
                style.put("border-top-width", value);
                break;
            }
            case "border-width-right":
            case "border-right-width" : {
                style.put("border-right-width", value);
                break;
            }
            case "border-width-bottom":
            case "border-bottom-width" : {
                style.put("border-bottom-width", value);
                break;
            }
            case "border-width-left":
            case "border-left-width" : {
                style.put("border-left-width", value);
                break;
            }
            case "border-color-top":
            case "border-top-color" : {
                style.put("border-top-color", value);
                break;
            }
            case "border-color-right":
            case "border-right-color" : {
                style.put("border-right-color", value);
                break;
            }
            case "border-color-bottom":
            case "border-bottom-color" : {
                style.put("border-bottom-color", value);
                break;
            }
            case "border-color-left":
            case "border-left-color" : {
                style.put("border-left-color", value);
                break;
            }
            
            case "border-style" : {
                List<LexicalUnit> units = new ArrayList<LexicalUnit>();
                while (value != null) {
                    units.add(value);
                    value = value.getNextLexicalUnit();
                }
                switch (units.size()) {
                    case 1 :
                        if (units.get(0).getLexicalUnitType() == LexicalUnit.SAC_IDENT) {
                            if ("cn1-round-border".equals(units.get(0).getStringValue())
                                    || "cn1-pill-border".equals(units.get(0).getStringValue())) {
                                apply(style, "cn1-background-type", units.get(0));
                                break;
                            }
                        }
                        apply(style, "border-style-top", units.get(0));
                        apply(style, "border-style-left", units.get(0));
                        apply(style, "border-style-bottom", units.get(0));
                        apply(style, "border-style-right", units.get(0));
                        break;
                    case 2 :
                        apply(style, "border-style-top", units.get(0));
                        apply(style, "border-style-bottom", units.get(0));
                        apply(style, "border-style-right", units.get(1));
                        apply(style, "border-style-left", units.get(1));
                        break;
                    case 3 :
                        apply(style, "border-style-top", units.get(0));
                        apply(style, "border-style-right", units.get(1));
                        apply(style, "border-style-left", units.get(1));
                        apply(style, "border-style-bottom", units.get(2));
                        break;
                    case 4 :
                        apply(style, "border-style-top", units.get(0));
                        apply(style, "border-style-left", units.get(3));
                        apply(style, "border-style-bottom", units.get(2));
                        apply(style, "border-style-right", units.get(1));
                        break;
                    default:
                        throw new RuntimeException("Unsupported number of units for border-style "+units.size());
                }
                break;
            }
            
            case "border-width" : {
                List<LexicalUnit> units = new ArrayList<LexicalUnit>();
                while (value != null) {
                    units.add(value);
                    value = value.getNextLexicalUnit();
                }
                switch (units.size()) {
                    case 1 :
                        apply(style, "border-width-top", units.get(0));
                        apply(style, "border-width-left", units.get(0));
                        apply(style, "border-width-bottom", units.get(0));
                        apply(style, "border-width-right", units.get(0));
                        break;
                    case 2 :
                        apply(style, "border-width-top", units.get(0));
                        apply(style, "border-width-bottom", units.get(0));
                        apply(style, "border-width-right", units.get(1));
                        apply(style, "border-width-left", units.get(1));
                        break;
                    case 3 :
                        apply(style, "border-width-top", units.get(0));
                        apply(style, "border-width-right", units.get(1));
                        apply(style, "border-width-left", units.get(1));
                        apply(style, "border-width-bottom", units.get(2));
                        break;
                    case 4 :
                        apply(style, "border-width-top", units.get(0));
                        apply(style, "border-width-left", units.get(3));
                        apply(style, "border-width-bottom", units.get(2));
                        apply(style, "border-width-right", units.get(1));
                        break;
                    default:
                        throw new RuntimeException("Unsupported number of units for border-width "+units.size());
                }
                break;
            }
            
            case "border-color" : {
                List<LexicalUnit> units = new ArrayList<LexicalUnit>();
                while (value != null) {
                    units.add(value);
                    value = value.getNextLexicalUnit();
                }
                switch (units.size()) {
                    case 1 :
                        apply(style, "border-color-top", units.get(0));
                        apply(style, "border-color-left", units.get(0));
                        apply(style, "border-color-bottom", units.get(0));
                        apply(style, "border-color-right", units.get(0));
                        break;
                    case 2 :
                        apply(style, "border-color-top", units.get(0));
                        apply(style, "border-color-bottom", units.get(0));
                        apply(style, "border-color-right", units.get(1));
                        apply(style, "border-color-left", units.get(1));
                        break;
                    case 3 :
                        apply(style, "border-color-top", units.get(0));
                        apply(style, "border-color-right", units.get(1));
                        apply(style, "border-color-left", units.get(1));
                        apply(style, "border-color-bottom", units.get(2));
                        break;
                    case 4 :
                        apply(style, "border-color-top", units.get(0));
                        apply(style, "border-color-left", units.get(3));
                        apply(style, "border-color-bottom", units.get(2));
                        apply(style, "border-color-right", units.get(1));
                        break;
                    default:
                        throw new RuntimeException("Unsupported number of units for border-style "+units.size());
                }
                break;
            }
            
            case "border-top" : {
                while (value != null) {
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_FUNCTION:
                        case LexicalUnit.SAC_RGBCOLOR :
                            apply(style, "border-color-top", value);
                            break;
                        case LexicalUnit.SAC_CENTIMETER :
                        case LexicalUnit.SAC_MILLIMETER :
                        case LexicalUnit.SAC_PIXEL :
                        case LexicalUnit.SAC_POINT :
                            apply(style, "border-width-top", value);
                            break;
                        case LexicalUnit.SAC_IDENT :
                            switch (value.getStringValue()) {
                                case "cn1-round-border":
                                case "cn1-pill-border":
                                    apply(style, "cn1-background-type", value);
                                    break;
                                default:
                                    try {
                                        Color.web(value.getStringValue());
                                        apply(style, "border-color-top", value);
                                    } catch (IllegalArgumentException ex) {
                                        apply(style, "border-style-top", value);
                                    }
                                    
                            }
                            
                            break;
                        default :
                            throw new RuntimeException("Unsupported lexical unit in border-top: "+value.getLexicalUnitType());
                    }
                    value = value.getNextLexicalUnit();
                }
                break;
            }
            
            case "border-right" : {
                while (value != null) {
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_FUNCTION:
                        case LexicalUnit.SAC_RGBCOLOR :
                            apply(style, "border-color-right", value);
                            break;
                        case LexicalUnit.SAC_CENTIMETER :
                        case LexicalUnit.SAC_MILLIMETER :
                        case LexicalUnit.SAC_PIXEL :
                        case LexicalUnit.SAC_POINT :
                            apply(style, "border-width-right", value);
                            break;
                        case LexicalUnit.SAC_IDENT :
                            try {
                                Color.web(value.getStringValue());
                                apply(style, "border-color-right", value);
                            } catch (IllegalArgumentException ex) {
                                apply(style, "border-style-right", value);
                            }
                            break;
                        default :
                            throw new RuntimeException("Unsupported lexical unit in border-right: "+value.getLexicalUnitType());
                    }
                    value = value.getNextLexicalUnit();
                }
                break;
            }
            
            case "border-bottom" : {
                while (value != null) {
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_FUNCTION:
                        case LexicalUnit.SAC_RGBCOLOR :
                            apply(style, "border-color-bottom", value);
                            break;
                        case LexicalUnit.SAC_CENTIMETER :
                        case LexicalUnit.SAC_MILLIMETER :
                        case LexicalUnit.SAC_PIXEL :
                        case LexicalUnit.SAC_POINT :
                            apply(style, "border-width-bottom", value);
                            break;
                        case LexicalUnit.SAC_IDENT :
                            try {
                                Color.web(value.getStringValue());
                                apply(style, "border-color-bottom", value);
                            } catch (IllegalArgumentException ex) {
                                apply(style, "border-style-bottom", value);
                            }
                        break;
                        default :
                            throw new RuntimeException("Unsupported lexical unit in border-bottom: "+value.getLexicalUnitType());
                    }
                    value = value.getNextLexicalUnit();
                }
                break;
            }
            
            case "border-left" : {
                while (value != null) {
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_FUNCTION:
                        case LexicalUnit.SAC_RGBCOLOR :
                            apply(style, "border-color-left", value);
                            break;
                        case LexicalUnit.SAC_CENTIMETER :
                        case LexicalUnit.SAC_MILLIMETER :
                        case LexicalUnit.SAC_PIXEL :
                        case LexicalUnit.SAC_POINT :
                            apply(style, "border-width-left", value);
                            break;
                        case LexicalUnit.SAC_IDENT :
                            try {
                                Color.web(value.getStringValue());
                                apply(style, "border-color-left", value);
                            } catch (IllegalArgumentException ex) {
                                apply(style, "border-style-left", value);
                            }
                            break;
                        default :
                            throw new RuntimeException("Unsupported lexical unit in border-left: "+value.getLexicalUnitType());
                    }
                    value = value.getNextLexicalUnit();
                }
                break;
            }
                
            case "border" : {
                apply(style, "border-top", value);
                apply(style, "border-right", value);
                apply(style, "border-bottom", value);
                apply(style, "border-left", value);
                break;
            }
            
            case "cn1-border-top-left-radius-x" :
                style.put("cn1-border-top-left-radius-x", value);
                break;
            case "cn1-border-bottom-left-radius-x" :
                style.put("cn1-border-bottom-left-radius-x", value);
                break;
            case "cn1-border-top-right-radius-x" :
                style.put("cn1-border-top-right-radius-x", value);
                break;
            case "cn1-border-bottom-right-radius-x" : 
                style.put("cn1-border-bottom-right-radius-x", value);
                break;
            case "cn1-border-top-left-radius-y" :
                style.put("cn1-border-top-left-radius-y", value);
                break;
            case "cn1-border-bottom-left-radius-y" :
                style.put("cn1-border-bottom-left-radius-y", value);
                break;
            case "cn1-border-top-right-radius-y" :
                style.put("cn1-border-top-right-radius-y", value);
                break;
            case "cn1-border-bottom-right-radius-y" : 
                style.put("cn1-border-bottom-right-radius-y", value);
                break;
            case "border-top-left-radius" :
                apply(style, "cn1-border-top-left-radius-x", value);
                apply(style, "cn1-border-top-left-radius-y", value);
                break;
            case "border-bottom-left-radius" :
                apply(style, "cn1-border-bottom-left-radius-x", value);
                apply(style, "cn1-border-bottom-left-radius-y", value);
                break;
            case "border-top-right-radius" :
                apply(style, "cn1-border-top-right-radius-x", value);
                apply(style, "cn1-border-top-right-radius-y", value);
                break;
            case "border-bottom-right-radius" : 
                apply(style, "cn1-border-bottom-right-radius-x", value);
                apply(style, "cn1-border-bottom-right-radius-y", value);
                break;
                
            case "border-radius" : {
                List<LexicalUnit> xUnits = new ArrayList<LexicalUnit>();
                List<LexicalUnit> yUnits = new ArrayList<LexicalUnit>();
                List<LexicalUnit> tmpUnits = xUnits;
                while (value != null) {
                    if (value.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_SLASH) {
                        tmpUnits = yUnits;
                        value = value.getNextLexicalUnit();
                        continue;
                    }
                    tmpUnits.add(value);
                    value = value.getNextLexicalUnit();
                }
                switch (xUnits.size()) {
                    case 1 :
                        apply(style, "cn1-border-top-left-radius-x", xUnits.get(0));
                        apply(style, "cn1-border-top-right-radius-x", xUnits.get(0));
                        apply(style, "cn1-border-bottom-left-radius-x", xUnits.get(0));
                        apply(style, "cn1-border-bottom-right-radius-x", xUnits.get(0));
                        break;
                    case 2 :
                        apply(style, "cn1-border-top-left-radius-x", xUnits.get(0));
                        apply(style, "cn1-border-top-right-radius-x", xUnits.get(1));
                        apply(style, "cn1-border-bottom-left-radius-x", xUnits.get(1));
                        apply(style, "cn1-border-bottom-right-radius-x", xUnits.get(0));
                        break;
                    case 3 :
                        apply(style, "cn1-border-top-left-radius-x", xUnits.get(0));
                        apply(style, "cn1-border-top-right-radius-x", xUnits.get(1));
                        apply(style, "cn1-border-bottom-left-radius-x", xUnits.get(1));
                        apply(style, "cn1-border-bottom-right-radius-x", xUnits.get(2));
                        break;
                        
                    case 4 :
                        apply(style, "cn1-border-top-left-radius-x", xUnits.get(0));
                        apply(style, "cn1-border-top-right-radius-x", xUnits.get(1));
                        apply(style, "cn1-border-bottom-left-radius-x", xUnits.get(3));
                        apply(style, "cn1-border-bottom-right-radius-x", xUnits.get(2));
                        break;
                        
                    default:
                        throw new RuntimeException("Unsupported number of x units for border radius : "+xUnits.size());
                        
                }
                
                if (yUnits.isEmpty()) {
                    yUnits = xUnits;
                }
                
                switch (yUnits.size()) {
                    case 1 :
                        apply(style, "cn1-border-top-left-radius-y", yUnits.get(0));
                        apply(style, "cn1-border-top-right-radius-y", yUnits.get(0));
                        apply(style, "cn1-border-bottom-left-radius-y", yUnits.get(0));
                        apply(style, "cn1-border-bottom-right-radius-y", yUnits.get(0));
                        break;
                    case 2 :
                        apply(style, "cn1-border-top-left-radius-y", yUnits.get(0));
                        apply(style, "cn1-border-top-right-radius-y", yUnits.get(1));
                        apply(style, "cn1-border-bottom-left-radius-y", yUnits.get(1));
                        apply(style, "cn1-border-bottom-right-radius-y", yUnits.get(0));
                        break;
                    case 3 :
                        apply(style, "cn1-border-top-left-radius-y", yUnits.get(0));
                        apply(style, "cn1-border-top-right-radius-y", yUnits.get(1));
                        apply(style, "cn1-border-bottom-left-radius-y", yUnits.get(1));
                        apply(style, "cn1-border-bottom-right-radius-y", yUnits.get(2));
                        break;
                        
                    case 4 :
                        apply(style, "cn1-border-top-left-radius-y", yUnits.get(0));
                        apply(style, "cn1-border-top-right-radius-y", yUnits.get(1));
                        apply(style, "cn1-border-bottom-left-radius-y", yUnits.get(3));
                        apply(style, "cn1-border-bottom-right-radius-y", yUnits.get(2));
                        break;
                        
                    default:
                        throw new RuntimeException("Unsupported number of y units for border radius : "+yUnits.size());
                }
            }
            break;
            
                
            case "cn1-box-shadow-h" : {
                style.put("cn1-box-shadow-h", value);
                break;
            }
            
            case "cn1-box-shadow-v" : {
                style.put("cn1-box-shadow-v", value);
                break;
            }
            
            case "cn1-box-shadow-blur" : {
                style.put("cn1-box-shadow-blur", value);
                break;
            }
            
            case "cn1-box-shadow-spread" : {
                style.put("cn1-box-shadow-spread", value);
                break;
            }
            
            case "cn1-box-shadow-color" : {
                style.put("cn1-box-shadow-color", value);
                break;
            }
            
            case "cn1-box-shadow-inset" : {
                style.put("cn1-box-shadow-inset", value);
                break;
            }
            
            case "box-shadow" : {
                
                if ("none".equals(value.getStringValue())) {
                    apply(style, "cn1-box-shadow-inset", value);
                    apply(style, "cn1-box-shadow-color", value);
                    apply(style, "cn1-box-shadow-spread", value);
                    apply(style, "cn1-box-shadow-blur", value);
                    apply(style, "cn1-box-shadow-h", value);
                    apply(style, "cn1-box-shadow-v", value);
                    break;
                }
                
                int i = 0;
                String[] params = {"cn1-box-shadow-h", "cn1-box-shadow-v", "cn1-box-shadow-blur", "cn1-box-shadow-spread"};
                boolean colorSet = false;
                while (value != null) {
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_IDENT :
                            if ("inset".equals(value.getStringValue())) {
                                apply(style, "cn1-box-shadow-inset", value);
                            } else {
                                apply(style, "cn1-box-shadow-color", value);
                                colorSet = true;
                            }
                        break;
                            
                        case LexicalUnit.SAC_PIXEL:
                        case LexicalUnit.SAC_MILLIMETER:
                        case LexicalUnit.SAC_PERCENTAGE:
                        case LexicalUnit.SAC_CENTIMETER:
                        case LexicalUnit.SAC_EM:
                        case LexicalUnit.SAC_POINT:
                        case LexicalUnit.SAC_INTEGER:
                        case LexicalUnit.SAC_REAL:
                            apply(style, params[i++], value);
                            break;
                            
                        case LexicalUnit.SAC_RGBCOLOR:
                            apply(style, "cn1-box-shadow-color", value);
                            colorSet = true;
                            break;
                        case LexicalUnit.SAC_FUNCTION:
                            if ("rgba".equals(value.getFunctionName()) || "cn1rgba".equals(value.getFunctionName()) || "cn1rgb".equals(value.getFunctionName())) {
                                apply(style, "cn1-box-shadow-color", value);
                            } else {
                                throw new RuntimeException("Unrecognized function when parsing box-shadow "+value.getFunctionName());
                            }
                        break;
                        default:
                            throw new RuntimeException("Unsupported lexical unit type in box shadow "+value.getLexicalUnitType());
                        
                    }
                    value = value.getNextLexicalUnit();
                }
                
                break;
            }
            
            default :
                throw new RuntimeException("Unsupported CSS property "+property);
            
                
        }
    }
    
    Element getElementByName(String name) {
        if (!elements.containsKey(name)) {
            Element el = new Element();
            el.parent = anyNodeStyle;
            elements.put(name, el);
        }
        return (Element)elements.get(name);
    }
    
    Element getElementForSelector(String media, Selector sel) {
        switch (sel.getSelectorType()) {
            case Selector.SAC_ANY_NODE_SELECTOR :
                return anyNodeStyle;
            case Selector.SAC_ELEMENT_NODE_SELECTOR : {
                ElementSelector esel = (ElementSelector)sel;
                if (media != null && !media.isEmpty()) {
                    return getElementByName(media+"-"+esel.getLocalName());
                } else {
                    return getElementByName(esel.getLocalName());
                }
            }
            case Selector.SAC_CONDITIONAL_SELECTOR : {
                ConditionalSelector csel = (ConditionalSelector)sel;
                SimpleSelector simple = csel.getSimpleSelector();
                switch (simple.getSelectorType()) {
                    case Selector.SAC_ANY_NODE_SELECTOR : {
                        Element parent = anyNodeStyle;
                        switch (csel.getCondition().getConditionType()) {
                            case Condition.SAC_CLASS_CONDITION :
                                AttributeCondition clsCond = (AttributeCondition)csel.getCondition();
                                switch (clsCond.getLocalName()) {
                                    case "selected" :
                                        return parent.getSelected();
                                    case "unselected" :
                                        return parent.getUnselected();
                                    case "pressed" :
                                        return parent.getPressed();
                                    case "disabled" :
                                        return parent.getDisabled();
                                    default :
                                        throw new RuntimeException("Unsupported style class "+clsCond.getLocalName());
                                }
                            default :
                                throw new RuntimeException("Unsupported CSS condition type "+csel.getCondition().getConditionType());
                        }
                    }
                    case Selector.SAC_ELEMENT_NODE_SELECTOR : {
                        ElementSelector esel = (ElementSelector)simple;
                        Element parent = getElementForSelector(media, esel);
                        switch (csel.getCondition().getConditionType()) {
                            case Condition.SAC_CLASS_CONDITION :
                                AttributeCondition clsCond = (AttributeCondition)csel.getCondition();
                                
                                switch (clsCond.getValue()) {
                                    case "selected" :
                                        return parent.getSelected();
                                    case "unselected" :
                                        return parent.getUnselected();
                                    case "pressed" :
                                        return parent.getPressed();
                                    case "disabled" :
                                        return parent.getDisabled();
                                    default :
                                        throw new RuntimeException("Unsupported style class "+clsCond.getValue());
                                }
                            default :
                                throw new CSSException("Unsupported CSS condition type "+csel.getCondition().getConditionType()+" for "+csel.getSimpleSelector());
                                //throw new RuntimeException("Unsupported CSS condition type "+csel.getCondition().getConditionType());
                        }
                    }
                    default :
                        throw new RuntimeException("Unsupported selector type "+simple.getSelectorType());
                }
                
                
            }
            default :
                throw new RuntimeException("Unsupported selector type "+sel.getSelectorType());
        }
    }
    
    public void apply(String media, Selector sel, String property, LexicalUnit value) {
        if (sel.getSelectorType() == Selector.SAC_CONDITIONAL_SELECTOR) {
            ConditionalSelector csel = (ConditionalSelector)sel;
            if (csel.getCondition().getConditionType() == Condition.SAC_ID_CONDITION) {
                AttributeCondition acond = (AttributeCondition)csel.getCondition();
                if ("Device".equalsIgnoreCase(acond.getValue())) {
                    switch (property) {
                        case "min-resolution" :
                            minDpi = ((ScaledUnit)value).getNumericValue();
                            break;
                        case "max-resolution" :
                            maxDpi = ((ScaledUnit)value).getNumericValue();
                            break;
                        case "resolution" :
                            currentDpi = ((ScaledUnit)value).getNumericValue();
                            break;
                    }
                    return;
                }
                if ("Constants".equalsIgnoreCase(acond.getValue())) {
                    constants.put(property, value);
                    return;
                }
            }
        }
        Element el = getElementForSelector(media, sel);
        apply(el, property, value);
        
    }
    
    private class Insets {
        static final int TOP=0;
        static final int RIGHT=1;
        static final int BOTTOM=2;
        static final int LEFT=3;
        
        float top, right, left, bottom;
        byte topUnit, rightUnit, leftUnit, bottomUnit;
        
        void set(int index, float value, byte unit) {
            switch (index) {
                case TOP:
                    top = value;
                    topUnit = unit;
                    break;
                case RIGHT:
                    right = value;
                    rightUnit = unit;
                    break;
                case BOTTOM:
                    bottom = value;
                    bottomUnit = unit;
                    break;
                case LEFT:
                    left = value;
                    leftUnit = unit;
                    break;
                default:
                    throw new RuntimeException("Invalid index "+index);
            }
        }
        
        public String toString() {
            return top+","+right+","+bottom+","+left;
                    
        }
    }
    
    
    
    private class IntValue {
        int value;
        byte unit;
    }
    
    private class FloatValue {
        float value;
        byte unit;
    }
    

    private FloatValue getFloatValue(LexicalUnit value) {
        
        FloatValue out = new FloatValue();
        
        if (value == null) {
            return out;
        }
        
        float fvalue = 0;
        byte unit = 0;
        switch (value.getLexicalUnitType()) {
            case LexicalUnit.SAC_PIXEL :
            case LexicalUnit.SAC_POINT :
            case LexicalUnit.SAC_INTEGER:
                unit = (byte)0;
                fvalue = Math.round(value.getFloatValue());
                break;
            case LexicalUnit.SAC_MILLIMETER :
                unit = (byte)2;
                fvalue = value.getFloatValue();
                break;
            case LexicalUnit.SAC_CENTIMETER :
                unit = (byte) 2;
                fvalue = value.getFloatValue() * 10;
                break;
            default :
                String unitText = null;
                try {
                    unitText = value.getDimensionUnitText();

                } catch (Exception ex) {

                    ex.printStackTrace();
                    throw new RuntimeException("No unit provided for "+value+" when parsing inset.", ex);
                }

                if ("rem".equals(unitText)) {
                    unit = (byte) Style.UNIT_TYPE_REM;
                    fvalue = value.getFloatValue();
                } else if ("vw".equals(unitText)) {
                    unit = Style.UNIT_TYPE_VW;
                    fvalue = value.getFloatValue();
                } else if ("vh".equals(unitText)) {
                    unit = Style.UNIT_TYPE_VH;
                    fvalue = value.getFloatValue();
                } else if ("vmin".equals(unitText)) {
                    unit = Style.UNIT_TYPE_VMIN;
                    fvalue = value.getFloatValue();
                } else if ("vmax".equals(unitText)) {
                    unit = Style.UNIT_TYPE_VMAX;
                    fvalue = value.getFloatValue();
                } else {
                    throw new RuntimeException("Unsupported unit for inset " + unitText);
                }

        }
        
        out.value = fvalue;
        out.unit = unit;
        return out;
            
    }
    
    private Insets getInsets(String key, Map<String,LexicalUnit> style) {
        Insets i = new Insets();
        
        LexicalUnit value = style.get(key+"-top");
        FloatValue top = getFloatValue(value);
        FloatValue right = getFloatValue(style.get(key+"-right"));
        FloatValue bottom = getFloatValue(style.get(key+"-bottom"));
        FloatValue left = getFloatValue(style.get(key+"-left"));
        
        i.top = top.value;
        i.topUnit = top.unit;
        i.right = right.value;
        i.rightUnit = right.unit;
        i.bottom = bottom.value;
        i.bottomUnit = bottom.unit;
        i.left = left.value;
        i.leftUnit = left.unit;
        return i;
            
    }
    


    static int getColorInt(LexicalUnit color) {
        String str = getColorString(color);
        return Integer.valueOf(str, 16);
    }
    
    static public Integer getColorAlphaInt(LexicalUnit bgColor) {
        if (bgColor == null) {
            return null;
        }
        while (bgColor != null) {
            if ("transparent".equals(bgColor.getStringValue())) {
                return 0;
            }
            if ("rgb".equals(bgColor.getFunctionName()) || "cn1rgb".equals(bgColor.getFunctionName())) {
                return 255;
            }
            if ("rgba".equals(bgColor.getFunctionName()) || "cn1rgba".equals(bgColor.getFunctionName())) {
                ScaledUnit r = (ScaledUnit)bgColor.getParameters();
                ScaledUnit g = r.getNextNumericUnit();
                ScaledUnit b = g.getNextNumericUnit();
                ScaledUnit a = b.getNextNumericUnit();

                return (int)(a.getNumericValue()*255.0);
            } else {
                return 255;
            }

        }

        return null;
    }
    
    static String leftPad(String str, int len) {
        while (str.length() < len) {
            str = "0" + str;
        }
        return str;
    }
    
    static String getARGBHexString(LexicalUnit color) {
        return "#" + leftPad(Integer.toHexString(getColorInt(color)), 6) + leftPad(Integer.toHexString(getColorAlphaInt(color)), 2);
    }

    static String getColorString(LexicalUnit color) {
        return getColorString(color, false);
    }
    static String getColorString(LexicalUnit color, boolean premultiplied) {
        if (color == null) return null;
        switch (color.getLexicalUnitType()) {
            case LexicalUnit.SAC_IDENT:
            case LexicalUnit.SAC_STRING_VALUE:
                String colorStr = color.getStringValue();
                if ("none".equals(colorStr)) {
                    return null;
                }
                if (colorStr.startsWith("attr(")) {
                    colorStr = colorStr.substring(colorStr.indexOf("(")+1, colorStr.lastIndexOf(")"));
                }
                if (colorStr.startsWith("color")) {
                    colorStr = colorStr.replace("color", "rgb");
                }
                Color c = Color.web(colorStr);
                return String.format( "%02X%02X%02X",
                        (int)( c.getRed() * 255 ),
                        (int)( c.getGreen() * 255 ),
                        (int)( c.getBlue() * 255 ) );

            case LexicalUnit.SAC_RGBCOLOR: {
                ScaledUnit red = (ScaledUnit)color.getParameters();
                ScaledUnit green = (ScaledUnit)red.getNextLexicalUnit();
                if (green.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA)
                    green = (ScaledUnit)green.getNextLexicalUnit();
                ScaledUnit blue = (ScaledUnit)green.getNextLexicalUnit();
                if (blue.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                    blue = (ScaledUnit)blue.getNextLexicalUnit();
                }
                return String.format( "%02X%02X%02X",
                        red.getIntegerValue(),
                        green.getIntegerValue(),
                        blue.getIntegerValue());

            }
            case LexicalUnit.SAC_FUNCTION: {
                if ("cn1rgb".equals(color.getFunctionName()) || "rgb".equals(color.getFunctionName()) || "cn1rgba".equals(color.getFunctionName()) || "rgba".equals(color.getFunctionName())) {
                    // If the value didn't have alpha, then we don't premultiply
                    if ("cn1rgb".equals(color.getFunctionName()) || "rgb".equals(color.getFunctionName())) premultiplied = false;
                    ScaledUnit red = (ScaledUnit)color.getParameters();
                    ScaledUnit green = (ScaledUnit)red.getNextLexicalUnit();
                    if (green == null) {
                        throw new RuntimeException("Failed to parse color "+color+".  Received null value for green parameter: "+color);
                    }
                    if (green.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                        green = (ScaledUnit) green.getNextLexicalUnit();
                    }
                    if (green == null) {
                        throw new RuntimeException("Failed to parse color "+color+".  Received null value for green parameter: "+color);
                    }
                    ScaledUnit blue = (ScaledUnit)green.getNextLexicalUnit();
                    if (blue == null) {
                        throw new RuntimeException("Failed to parse color "+color+".  Received null value for blue parameter: "+color);
                    }
                    if (blue.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                        blue = (ScaledUnit)blue.getNextLexicalUnit();
                    }
                    if (blue == null) {
                        throw new RuntimeException("Failed to parse color "+color+".  Received null value for blue parameter: "+color);
                    }


                    ScaledUnit alpha = null;
                    if (premultiplied) {


                        alpha = (ScaledUnit) blue.getNextLexicalUnit();
                        if (alpha == null) {
                            throw new RuntimeException("Failed to parse color " + color + ". Received null alpha parameter:" + color);
                        }
                        if (alpha.getLexicalUnitType() == LexicalUnit.SAC_OPERATOR_COMMA) {
                            alpha = (ScaledUnit) alpha.getNextLexicalUnit();
                        }
                        if (alpha == null) {
                            throw new RuntimeException("Failed to parse color " + color + ". Received null alpha parameter:" + color);
                        }
                    }
                    int r = (int)(red.getIntegerValue() * (premultiplied?(1-alpha.getNumericValue()):1));
                    int g = (int)(green.getIntegerValue() * (premultiplied?(1-alpha.getNumericValue()):1));
                    int b = (int)(blue.getIntegerValue() * (premultiplied?(1-alpha.getNumericValue()):1));


                    return String.format( "%02X%02X%02X",
                            r,
                            g,
                            b);
                }
            }
            default:
                throw new RuntimeException("Unsupported color type "+color.getLexicalUnitType());
        }
    }

    static String getColorString_old(LexicalUnit color) {
        
        if (color == null) {
                return null;
            }
            switch (color.getLexicalUnitType()) {
                case LexicalUnit.SAC_IDENT:
                case LexicalUnit.SAC_RGBCOLOR: {
                    String colorStr = color.getStringValue();
                    if (colorStr == null) {
                        colorStr = ""+color;
                        colorStr = colorStr.replace("color", "rgb");
                    }
                    if (colorStr.startsWith("attr(")) {
                        colorStr = colorStr.substring(colorStr.indexOf("(")+1, colorStr.lastIndexOf(")"));
                    }
                    if (colorStr.startsWith("color")) {
                        colorStr = colorStr.replace("color", "rgb");
                    }
                    if ("none".equals(colorStr)) {
                        return null;
                    }
                    
                    Color c = Color.web(colorStr);
                    return String.format( "%02X%02X%02X",
                    (int)( c.getRed() * 255 ),
                    (int)( c.getGreen() * 255 ),
                    (int)( c.getBlue() * 255 ) );
                    
                }
                case LexicalUnit.SAC_FUNCTION: {
                    if ("rgba".equals(color.getFunctionName()) || "cn1rgba".equals(color.getFunctionName()) || "cn1rgb".equals(color.getFunctionName())) {
                       
                        Color c = Color.web(color+"");
                        return String.format( "%02X%02X%02X",
                            (int)( c.getRed() * 255 ),
                            (int)( c.getGreen() * 255 ),
                            (int)( c.getBlue() * 255 ) );
                    }
                }
                default: 
                    throw new RuntimeException("Unsupported color type "+color.getLexicalUnitType());
            }
        
    }
    
    private int getPreviewScreenWidth() {
        return (int)currentDpi * 2;
    }
    
    private int getPreviewScreenHeight() {
        return (int)currentDpi * 3;
    }
    
    private static List<String> getMediaPrefixes(SACMediaList l) {
        
        int len = l == null ? 0 : l.getLength();
        if (len == 0) {
            ArrayList<String> out = new ArrayList<String>();
            out.add("");
            return out;
        }
        ArrayList<String> out = new ArrayList<String>();
        ArrayList<String> platforms = new ArrayList<String>();
        ArrayList<String> densities = new ArrayList<String>();
        ArrayList<String> types = new ArrayList<String>();
        for (int i=0; i<len; i++) {
            String key = l.item(i);
            if (key.startsWith("platform-")) {
                platforms.add(key);
            } else if (key.startsWith("density-")) {
                densities.add(key);
            } else if (key.startsWith("device-")) {
                types.add(key);
            }
        }
        if (!platforms.isEmpty()) {
            for (String platform : platforms) {
                if (densities.isEmpty()) {
                    out.add(platform);
                } else {
                    for (String density : densities) {
                        out.add(platform+"-"+density);
                    }
                }
            }
        } else {
            if (densities.isEmpty()) {
                out.add("");
            } else {
                for (String density : densities) {
                    out.add(density);
                }
            }
        }
        if (!types.isEmpty()) {
            int outLen = out.size();
            for (String type: types) {
                for (int i=0; i<outLen; i++) {
                    String curr = out.get(i);
                    if (curr.isEmpty()) {
                        out.set(i, type);
                    } else {
                        out.set(i, type+"-"+curr);
                    }
                }
            }
        }
        return out;
    }
    
    public static CSSTheme load(URL uri) throws IOException {
        try {
            System.setProperty("org.w3c.css.sac.parser", "org.w3c.flute.parser.Parser");
            InputSource source = new InputSource();
            InputStream stream = uri.openStream();
            String stringContents = Util.readToString(stream);

            // The flute parser chokes on properties beginning with -- so we need to replace these with cn1 prefix
            // for CSS variable support.
            stringContents = stringContents.replaceAll("([\\(\\W])(--[a-zA-Z0-9\\-]+)", "$1cn1$2");

            // Flute chokes on embedded var() functions inside an rgb or rgba function.  Hoping to support it by changing the
            // function name to cn1rgb() and cn1rgba() respectively.
            stringContents = stringContents.replaceAll("\\brgb\\(", "cn1rgb(");
            stringContents = stringContents.replaceAll("\\brgba\\(", "cn1rgba(");

            
            source.setCharacterStream(new CharArrayReader(stringContents.toCharArray()));
            source.setURI(uri.toString());
            source.setEncoding("UTF-8");
            ParserFactory parserFactory = new ParserFactory();
            
            Parser parser = parserFactory.makeParser();
            final CSSTheme theme = new CSSTheme();
            theme.baseURL = uri;
            parser.setErrorHandler(new ErrorHandler() {

                @Override
                public void warning(CSSParseException csspe) throws CSSException {
                    
                    System.out.println("CSS Warning: "+csspe.getLocalizedMessage()+" on line "+csspe.getLineNumber()+" col: "+csspe.getColumnNumber()+" of file "+csspe.getURI());
                }

                @Override
                public void error(CSSParseException csspe) throws CSSException {
                    System.out.println("CSS Error: "+csspe.getLocalizedMessage()+" on line "+csspe.getLineNumber()+" col: "+csspe.getColumnNumber()+" of file "+csspe.getURI());
                }

                @Override
                public void fatalError(CSSParseException csspe) throws CSSException {
                    System.out.println("CSS Fatal Error: "+csspe.getLocalizedMessage()+" on line "+csspe.getLineNumber()+" col: "+csspe.getColumnNumber()+" of file "+csspe.getURI());
                }
                
            });
            //parser.setLocale(Locale.getDefault());
            parser.setDocumentHandler(new DocumentHandler() {
                Map<String,LexicalUnit> variables = new LinkedHashMap<>();
                SelectorList currSelectors;
                FontFace currFontFace;
                SACMediaList currMediaList;
                //double currentTargetDpi = 320;
                //double currentMinDpi = 120;
                //double currentMaxDpi = 640;
                //int currentScreenWidth = 1280;
                //int currentScreenHeight = 1920;
                
                @Override
                public void startDocument(InputSource is) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public void endDocument(InputSource is) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public void comment(String string) throws CSSException {
                    
                    
                }
                
                @Override
                public void ignorableAtRule(String string) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public void namespaceDeclaration(String string, String string1) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public void importStyle(String string, SACMediaList sacml, String string1) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public void startMedia(SACMediaList sacml) throws CSSException {
                    currMediaList = sacml;
                }
                
                @Override
                public void endMedia(SACMediaList sacml) throws CSSException {
                    currMediaList = null;
                }
                
                @Override
                public void startPage(String string, String string1) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public void endPage(String string, String string1) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public void startFontFace() throws CSSException {
                    currFontFace = theme.createFontFace();
                }
                
                @Override
                public void endFontFace() throws CSSException {
                    currFontFace = null;
                }
                
                @Override
                public void startSelector(SelectorList sl) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    currSelectors = sl;
                    
                }
                
                @Override
                public void endSelector(SelectorList sl) throws CSSException {
                    currSelectors = null;
                }
                
                @Override
                public void property(String string, LexicalUnit lu, boolean bln) throws CSSException {
                    try {
                        property_(string, lu, bln);
                    } catch (Throwable t) {
                        if (t instanceof CSSException) {
                            throw (CSSException)t;
                        } else {
                            System.out.println("Exception occurred while parsing property "+string+" "+lu);
                            t.printStackTrace();
                            
                            throw new ParseException(t.getMessage());
                        }
                    }
                }

                private ScaledUnit last(LexicalUnit lu) {

                    while (lu.getNextLexicalUnit() != null) {
                        lu = lu.getNextLexicalUnit();
                    }
                    return (lu instanceof ScaledUnit) ? (ScaledUnit)lu : new ScaledUnit(lu, theme.currentDpi, theme.getPreviewScreenWidth(), theme.getPreviewScreenHeight());
                }

                /**
                 * Evaluates a LexicalUnit in the current parser position.  This will expand any variables.  It will
                 * continue to evaluate the next lexical unit, until it reaches the end of the current lexical unit chain.
                 * @param lu The lexical unit to evaluate.
                 * @return
                 * @throws CSSException
                 */
                private ScaledUnit evaluate(LexicalUnit lu) throws CSSException {
                    if (lu.getLexicalUnitType() == LexicalUnit.SAC_FUNCTION && "var".equals(lu.getFunctionName())) {
                        LexicalUnit parameters = lu.getParameters();
                        String varname = parameters.getStringValue();

                        LexicalUnit varVal = variables.get(varname);
                        ScaledUnit su;
                        if (varVal == null && parameters.getNextLexicalUnit() != null) {
                            varVal = parameters.getNextLexicalUnit();
                            su = evaluate(new ScaledUnit(varVal, theme.currentDpi, theme.getPreviewScreenWidth(), theme.getPreviewScreenHeight()));

                        } else if (varVal == null) {
                            su = new ScaledUnit(lu, theme.currentDpi, theme.getPreviewScreenWidth(), theme.getPreviewScreenHeight());
                        } else {
                            su = evaluate(new ScaledUnit(varVal, theme.currentDpi, theme.getPreviewScreenWidth(), theme.getPreviewScreenHeight()));
                        }
                        // Evaluate the variable value in case it also includes other variables that need to be evaluated.
                        //ScaledUnit su = evaluate(new ScaledUnit(varVal, theme.currentDpi, theme.getPreviewScreenWidth(), theme.getPreviewScreenHeight()));
                        LexicalUnit toAppend = lu.getNextLexicalUnit();
                        ScaledUnit last = last(su);
                        if (toAppend != null) {
                            toAppend = evaluate(toAppend);
                            last.setNextLexicalUnit(toAppend);
                            ((ScaledUnit) toAppend).setPrevLexicalUnit(last);
                        } else {
                            last.setNextLexicalUnit(null);
                        }
                        return su;
                    } else {

                        ScaledUnit su = new ScaledUnit(lu, theme.currentDpi, theme.getPreviewScreenWidth(), theme.getPreviewScreenHeight());
                        LexicalUnit nex = su.getNextLexicalUnit();
                        if (su.getParameters() != null) {
                            su.setParameters(evaluate(su.getParameters()));
                        }
                        if (nex != null) {


                            ScaledUnit snex = evaluate(nex);
                            su.setNextLexicalUnit(snex);
                            snex.setPrevLexicalUnit(su);

                        }
                        return su;

                    }


                }

                private void property_(String string, LexicalUnit _lu, boolean bln) throws CSSException {
                    if (string.startsWith("cn1--")) {
                        
                        variables.put(string, _lu);
                        return;
                    }

                    ScaledUnit su = evaluate(_lu);
                    if (currFontFace != null) {
                        switch (string) {
                            case "font-family" :
                                currFontFace.fontFamily = su;
                                break;
                            case "font-style" :
                                currFontFace.fontStyle = su;
                                break;
                            case "font-stretch" :
                                currFontFace.fontStretch = su;
                                break;
                            case "src" :
                                currFontFace.src = su;
                                break;
                            case "font-weight" :
                                currFontFace.fontWeight = su;
                                break;
                        }
                    } else {

                        int len = currSelectors.getLength();
                        for (int i=0; i<len; i++) {
                            Selector sel = currSelectors.item(i);
                            if (currMediaList != null) {
                                for (String mediaPrefix : getMediaPrefixes(currMediaList)) {
                                    theme.apply(mediaPrefix, sel, string, su);
                                }
                            } else {
                                theme.apply(null, sel, string, su);
                            }

                        }
                    }
                }
                
            });
            
            parser.parseStyleSheet(source);
            stream.close();
            return theme;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(CSSTheme.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CSSTheme.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(CSSTheme.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            if (ex.getMessage().contains("encoding properties")) {
                // This error always happens and there doesn't seem to be a way to fix it... so let's just hide
                // it .  Doesn't seem to hurt anything.
            } else {
                //Logger.getLogger(CSSTheme.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ClassCastException ex) {
            Logger.getLogger(CSSTheme.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static void main(String[] args) throws Exception {
        CSSTheme theme = CSSTheme.load(CSSTheme.class.getResource("test.css"));
        
    }
    
    
    
}
