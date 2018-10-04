/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.designer.css;

import com.codename1.io.Util;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.EditorTTFFont;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationAccessor;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EditableResourcesForCSS;
import com.codename1.ui.util.EditableResources;
import java.io.ByteArrayInputStream;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
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

/**
 *
 * @author shannah
 */
public class CSSTheme {
    
    URL baseURL;
    File cssFile = new File("test.css");
    File resourceFile = new File("test.css.res");
    Element anyNodeStyle = new Element();
    Map<String,Element> elements = new HashMap<String,Element>();
    Map<String,LexicalUnit> constants = new HashMap<String,LexicalUnit>();
    EditableResources res;
    private String themeName = "Theme";
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
            //System.out.println("Step 2");
            ScaledUnit params = (ScaledUnit)linearGradientFunc.getParameters();
            if (params == null) {
                return;
            }
            //System.out.println("Step 3");
            
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
            //System.out.println("Step 4");
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
            //System.out.println("Stemp 5");
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
            //System.out.println("Step 6");
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
                    System.err.println("Expecting color for param 3 of linear-gradient");
                    return;
            }
           
            //System.out.println("Step 8");
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
            System.out.println("Gradient: "+Arrays.toString(out));
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
                    //File cssFile = new File(url.toURI());
                    File parentDir = cssFile.getParentFile();
                    if (url.getProtocol().startsWith("http")) {
                        // If it is remote, check so see if we've already downloaded
                        // the font to the current directory.
                        String fontName = url.getPath();
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
        
        
        ScaledUnit(LexicalUnit src, double dpi, int screenWidth, int screenHeight) {
            this.src = src;
            this.dpi = dpi;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            
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
                if (!gradient.valid) {
                    System.err.println("Gradient not valid: "+gradient.reason);
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
            LexicalUnit nex = src.getNextLexicalUnit();
            
            return nex == null ? null : new ScaledUnit(nex, dpi, screenWidth, screenHeight);
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
            LexicalUnit prev = src.getPreviousLexicalUnit();
            return prev == null ? null : new ScaledUnit(prev, dpi, screenWidth, screenHeight);
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
            LexicalUnit param = src.getParameters();
            return param == null ? null :  new ScaledUnit(param, dpi, screenWidth, screenHeight);
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
                    sb.append(lu.getFunctionName()).append("(");
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
                }
                case "height": {
                    LexicalUnit value = styles.get(property);
                    switch (value.getLexicalUnitType()) {
                        case LexicalUnit.SAC_PERCENTAGE:
                            return property + ":"+ (int)(value.getFloatValue() / 100f * 960f) + "px";
                    }
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
        sb.append("<!doctype html>\n<html><base href=\""+baseURL.toExternalForm()+"\"/> <head><style type=\"text/css\">body {padding:0; margin:0} div.element {margin: 0 !important; padding: 0 !important; }</style></head><body>");
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
        Map<String,String> out = new HashMap<String,String>();
        for (String id : elements.keySet()) {
            Element el = elements.get(id);
            //System.out.println("Checksum("+id+") is "+el.getChecksum());
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
        try (ObjectInputStream fis = new ObjectInputStream(new FileInputStream(input))) {
            return (Map<String,String>)fis.readObject();
        }
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
            if (derive != null && isModified(derive)) {
                return true;
            }
            if (unselectedDerive != null && isModified(unselectedDerive)) {
                return true;
            }
            if (selectedDerive != null && isModified(selectedDerive)) {
                return true;
            }
            if (pressedDerive != null && isModified(pressedDerive)) {
                return true;
            }
            if (disabledDerive != null && isModified(disabledDerive)) {
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
    
    public Map<String, CacheStatus> calculateSelectorCacheStatus(File cachedFile) throws IOException {
        try {
            Map<String,String> current = calculateSelectorChecksums();
            Map<String,String> previous = loadSelectorChecksums(cachedFile);
            HashMap<String, CacheStatus> out = new HashMap<String, CacheStatus>();
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
        for (String id : elements.keySet()) {
            if (!isModified(id)) {
                continue;
            }
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
            
            res.setThemeProperty(themeName, unselId+".padding", el.getThemePadding(unselectedStyles));
            res.setThemeProperty(themeName, unselId+".padUnit", el.getThemePaddingUnit(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#padding", el.getThemePadding(selectedStyles));
            res.setThemeProperty(themeName, selId+"#padUnit", el.getThemePaddingUnit(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#padding", el.getThemePadding(pressedStyles));
            res.setThemeProperty(themeName, pressedId+"#padUnit", el.getThemePaddingUnit(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#padding", el.getThemePadding(disabledStyles));
            res.setThemeProperty(themeName, disabledId+"#padUnit", el.getThemePaddingUnit(disabledStyles));
            
            res.setThemeProperty(themeName, unselId+".margin", el.getThemeMargin(unselectedStyles));
            res.setThemeProperty(themeName, unselId+".marUnit", el.getThemeMarginUnit(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#margin", el.getThemeMargin(selectedStyles));
            res.setThemeProperty(themeName, selId+"#marUnit", el.getThemeMarginUnit(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#margin", el.getThemeMargin(pressedStyles));
            res.setThemeProperty(themeName, pressedId+"#marUnit", el.getThemeMarginUnit(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#margin", el.getThemeMargin(disabledStyles));
            res.setThemeProperty(themeName, disabledId+"#marUnit", el.getThemeMarginUnit(disabledStyles));
            
            
            res.setThemeProperty(themeName, unselId+".fgColor", el.getThemeFgColor(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#fgColor", el.getThemeFgColor(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#fgColor", el.getThemeFgColor(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#fgColor", el.getThemeFgColor(disabledStyles));
            res.setThemeProperty(themeName, unselId+".bgColor", el.getThemeBgColor(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#bgColor", el.getThemeBgColor(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#bgColor", el.getThemeBgColor(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#bgColor", el.getThemeBgColor(disabledStyles));
            
            res.setThemeProperty(themeName, unselId+".transparency", el.getThemeTransparency(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#transparency", el.getThemeTransparency(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#transparency", el.getThemeTransparency(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#transparency", el.getThemeTransparency(disabledStyles));
            
            
            res.setThemeProperty(themeName, unselId+".align", el.getThemeAlignment(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#align", el.getThemeAlignment(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#align", el.getThemeAlignment(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#align", el.getThemeAlignment(disabledStyles));
            
            res.setThemeProperty(themeName, unselId+".font", el.getThemeFont(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#font", el.getThemeFont(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#font", el.getThemeFont(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#font", el.getThemeFont(disabledStyles));
            
            res.setThemeProperty(themeName, unselId+".textDecoration", el.getThemeTextDecoration(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#textDecoration", el.getThemeTextDecoration(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#textDecoration", el.getThemeTextDecoration(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#textDecoration", el.getThemeTextDecoration(disabledStyles));
            
            res.setThemeProperty(themeName, unselId+".bgGradient", el.getThemeBgGradient(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#bgGradient", el.getThemeBgGradient(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#bgGradient", el.getThemeBgGradient(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#bgGradient", el.getThemeBgGradient(disabledStyles));
            
            res.setThemeProperty(themeName, unselId+".bgType", el.getThemeBgType(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#bgType", el.getThemeBgType(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#bgType", el.getThemeBgType(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#bgType", el.getThemeBgType(disabledStyles));
            
            res.setThemeProperty(themeName, unselId+".derive", el.getThemeDerive(unselectedStyles, ""));
            res.setThemeProperty(themeName, selId+"#derive", el.getThemeDerive(selectedStyles, ".sel"));
            res.setThemeProperty(themeName, pressedId+"#derive", el.getThemeDerive(pressedStyles, ".press"));
            res.setThemeProperty(themeName, disabledId+"#derive", el.getThemeDerive(disabledStyles, ".dis"));
            
            res.setThemeProperty(themeName, unselId+".opacity", el.getThemeOpacity(unselectedStyles));
            res.setThemeProperty(themeName, selId+"#opacity", el.getThemeOpacity(selectedStyles));
            res.setThemeProperty(themeName, pressedId+"#opacity", el.getThemeOpacity(pressedStyles));
            res.setThemeProperty(themeName, disabledId+"#opacity", el.getThemeOpacity(disabledStyles));
            
           //System.out.println("Checking if background image is here for "+unselectedStyles);
           if (el.hasBackgroundImage(unselectedStyles) && !el.requiresBackgroundImageGeneration(unselectedStyles) && !el.requiresImageBorder(unselectedStyles)) {
               //System.out.println("Getting background image... it is here"); 
               Image[] imageId = getBackgroundImages(unselectedStyles);
                if (imageId != null && imageId.length > 0) {
                    
                    res.setThemeProperty(themeName, unselId+".bgImage", imageId[0]);
                }
            }
            if (el.hasBackgroundImage(selectedStyles) && !el.requiresBackgroundImageGeneration(selectedStyles) && !el.requiresImageBorder(selectedStyles)) {
                Image[] imageId = getBackgroundImages(selectedStyles);
                if (imageId != null && imageId.length > 0) {
                    
                    res.setThemeProperty(themeName, selId+"#bgImage", imageId[0]);
                }
            }
            
            if (el.hasBackgroundImage(pressedStyles) && !el.requiresBackgroundImageGeneration(pressedStyles) && !el.requiresImageBorder(pressedStyles)) {
                Image[] imageId = getBackgroundImages(pressedStyles);
                if (imageId != null && imageId.length > 0) {
                    
                    res.setThemeProperty(themeName, pressedId+"#bgImage", imageId[0]);
                }
            }
            if (el.hasBackgroundImage(disabledStyles) && !el.requiresBackgroundImageGeneration(disabledStyles) && !el.requiresImageBorder(disabledStyles)) {
                Image[] imageId = getBackgroundImages(disabledStyles);
                if (imageId != null && imageId.length > 0) {
                    
                    res.setThemeProperty(themeName, disabledId+"#bgImage", imageId[0]);
                }
            }
            
            if (!el.requiresImageBorder(unselectedStyles) && !el.requiresBackgroundImageGeneration(unselectedStyles)) {
                res.setThemeProperty(themeName, unselId+".border", el.getThemeBorder(unselectedStyles));
            }
            if (!el.requiresImageBorder(selectedStyles) && !el.requiresBackgroundImageGeneration(selectedStyles)) {
                res.setThemeProperty(themeName, selId+"#border", el.getThemeBorder(selectedStyles));
            }
            if (!el.requiresImageBorder(pressedStyles) && !el.requiresBackgroundImageGeneration(pressedStyles)) {
                res.setThemeProperty(themeName, pressedId+"#border", el.getThemeBorder(pressedStyles));
            }
            if (!el.requiresImageBorder(disabledStyles) && !el.requiresBackgroundImageGeneration(disabledStyles)) {
                res.setThemeProperty(themeName, disabledId+"#border", el.getThemeBorder(disabledStyles));
            }
            
            
            
            
        }
        
        for (String constantKey : constants.keySet()) {
            LexicalUnit lu = constants.get(constantKey);

            if (lu.getLexicalUnitType() == LexicalUnit.SAC_STRING_VALUE || lu.getLexicalUnitType() == LexicalUnit.SAC_IDENT) {
                if (constantKey.endsWith("Image")) {
                    // We have an image
                    Image im = res.getImage(lu.getStringValue());
                    if (im == null) {
                        im = getResourceImage(lu.getStringValue());
                    }
                    if (im == null) {
                        //System.out.println(Arrays.toString(res.getImageResourceNames()));
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
        }


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
            //System.out.println("Referenced names "+referencedImageNames);
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
        
        //System.out.println("Unused images: "+images);
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
    
    private Map<String,Image> loadedImages = new HashMap<String,Image>();
    
    
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
            //System.out.println("xDPI: -");
        }
        if (nodes.getLength() > 0) {
            nodes = root.getElementsByTagName("VerticalPixelSize");
            IIOMetadataNode dpcHeight = (IIOMetadataNode) nodes.item(0);
            NamedNodeMap nnm = dpcHeight.getAttributes();
            Node item = nnm.item(0);
            yDPI = Math.round(25.4f / Float.parseFloat(item.getNodeValue()) / 0.45f);
            //System.out.println("yDPI: " + yDPI);
        } else {
            //System.out.println("yDPI: -");
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
                int i=1;
                while (res.getImage(imageIdStr) != null) {
                    if (i == 1) {
                        imageIdStr += "_"+(++i);
                    } else {
                        imageIdStr = imageIdStr.substring(0, imageIdStr.lastIndexOf("_")) + "_"+(++i);
                    }
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
            if (styles.containsKey("cn1-source-dpi")) {
                //System.out.println("Using cn1-source-dpi "+styles.get("cn1-source-dpi").getFloatValue());
                double densityVal = ((ScaledUnit)styles.get("cn1-source-dpi")).getNumericValue();
                if (Math.abs(densityVal) < 0.5) {
                    resm.targetDensity = 0;
                } else {
                    resm.targetDensity = getDensityForDpi(densityVal);
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
            }
            
            //System.out.println("Target density for image is "+resm.targetDensity);
            
            //System.out.println("Loading image from "+url+" with density "+resm.targetDensity);
            Image im = resm.storeImage(encImg, imageIdStr, false);
            //System.out.println("Finished storing image "+url);
            //System.out.println("Storing image "+url+" at id "+imageIdStr);
            loadedImages.put(url, im);
            
            
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
            res.openFile(new FileInputStream(resourceFile));
        }
    }
    
    public static interface WebViewProvider {
        WebView getWebView();
    }
    
    public void createImageBorders(WebViewProvider webviewProvider) {
        if (res == null) {
            res = new EditableResourcesForCSS(resourceFile);
        }
        ArrayList<Border> borders = new ArrayList<Border>();
        
        ResourcesMutator resm = new ResourcesMutator(res, Display.DENSITY_VERY_HIGH, minDpi, maxDpi);
        resm.targetDensity = targetDensity;
        
        
        List<Runnable> onComplete = new ArrayList<Runnable>();
        for (String id : elements.keySet()) {
            if (!isModified(id)) {
                //System.out.println("id "+id+" not modified in createImageBorders");
                continue;
            } else {
                //System.out.println("id "+id +" IS modified in createImageBorders");
            }
            Element e = (Element)elements.get(id);
            
            Element unselected = e.getUnselected();
            Map<String,LexicalUnit> unselectedStyles = (Map<String,LexicalUnit>)unselected.getFlattenedStyle();
            Border b = unselected.createBorder(unselectedStyles);
            Border unselectedBorder = b;
            if (e.requiresImageBorder(unselectedStyles)) {
                if (!borders.contains(b)) {
                    borders.add(b);
                    resm.addImageProcessor(id, (img) -> {
                        
                        Insets insets = unselected.getImageBorderInsets(unselectedStyles, img.getWidth(), img.getHeight());
                        //System.out.println("Creating 9 piece for image "+img.getWidth()+", "+img.getHeight()+" with insets "+insets.top+", "+insets.right+","+insets.bottom+","+insets.left);
                        resm.targetDensity = getSourceDensity(unselectedStyles, resm.targetDensity);
                        com.codename1.ui.plaf.Border border = resm.create9PieceBorder(img, id, (int)insets.top, (int)insets.right, (int)insets.bottom, (int)insets.left);
                        resm.put(id+".border", border);
                        unselectedBorder.border = border;
                        resm.targetDensity = targetDensity;
                    });
                } else {
                    onComplete.add(()->{
                        resm.put(id+".border", borders.get(borders.indexOf(unselectedBorder)).border);
                    });
                    
                }
            } else if (e.requiresBackgroundImageGeneration(unselectedStyles)) {
                if (!borders.contains(b)) {
                    borders.add(b);
                    resm.addImageProcessor(id, (img) -> {
                        int i = 1;
                        while(res.containsResource(id + "_" + i + ".png")) {
                            i++;
                        }
                        String prefix = id + "_" + i + ".png";
                        //System.out.println("Generating image "+prefix);
                        resm.targetDensity = getSourceDensity(unselectedStyles, resm.targetDensity);
                        Image im = resm.storeImage(EncodedImage.create(ResourcesMutator.toPngOrJpeg(img)), prefix, false);
                        unselectedBorder.image = im;
                        resm.put(id+".bgImage", im);
                        resm.targetDensity = targetDensity;
                        //resm.put(id+".press#bgType", Style.B)
                    });
                } else {
                    onComplete.add(()->{
                        resm.put(id+".bgImage", unselectedBorder.image);
                    });
                }
            }
            
            Element selected = e.getSelected();
            Map<String,LexicalUnit> selectedStyles = (Map<String,LexicalUnit>)selected.getFlattenedStyle();
            b = selected.createBorder(selectedStyles);
            Border selectedBorder = b;
            if (e.requiresImageBorder(selectedStyles)) {
                if (!borders.contains(b)) {
                    borders.add(b);
                    resm.addImageProcessor(id+".sel", (img) -> {
                        Insets insets = selected.getImageBorderInsets(selectedStyles, img.getWidth(), img.getHeight());
                        resm.targetDensity = getSourceDensity(selectedStyles, resm.targetDensity);
                        com.codename1.ui.plaf.Border border = resm.create9PieceBorder(img, id, (int)insets.top, (int)insets.right, (int)insets.bottom, (int)insets.left);
                        resm.put(id+".sel#border", border);
                        selectedBorder.border = border;
                        resm.targetDensity = targetDensity;
                    });
                } else {
                    onComplete.add(()-> {
                        resm.put(id+".sel#border", borders.get(borders.indexOf(selectedBorder)).border);
                    });
                    
                }
            } else if (e.requiresBackgroundImageGeneration(selectedStyles)) {
                if (!borders.contains(b)) {
                    borders.add(b);
                    resm.addImageProcessor(id+".sel", (img) -> {
                        int i = 1;
                        while(res.containsResource(id + "_" + i + ".png")) {
                            i++;
                        }
                        String prefix = id + "_" + i + ".png";
                        //System.out.println("Generating image "+prefix);
                        resm.targetDensity = getSourceDensity(selectedStyles, resm.targetDensity);
                        Image im = resm.storeImage(EncodedImage.create(ResourcesMutator.toPngOrJpeg(img)), prefix, false);
                        selectedBorder.image = im;
                        resm.put(id+".sel#bgImage", im);
                        //resm.put(id+".press#bgType", Style.B)
                        resm.targetDensity = targetDensity;
                    });
                } else {
                    onComplete.add(()->{
                        resm.put(id+".sel#bgImage", selectedBorder.image);
                    });
                }
            }
            
            Element pressed = e.getPressed();
            Map<String,LexicalUnit> pressedStyles = (Map<String,LexicalUnit>)pressed.getFlattenedStyle();
            //System.out.println("Pressed styles "+pressedStyles);
            b = pressed.createBorder(pressedStyles);
            Border pressedBorder = b;
            if (e.requiresImageBorder(pressedStyles)) {
                if (!borders.contains(b)) {
                    borders.add(b);
                    resm.addImageProcessor(id+".press", (img) -> {
                        Insets insets = pressed.getImageBorderInsets(pressedStyles, img.getWidth(), img.getHeight());
                        //System.out.println("Getting pressed images with insets "+insets);
                        resm.targetDensity = getSourceDensity(pressedStyles, resm.targetDensity);
                        com.codename1.ui.plaf.Border border = resm.create9PieceBorder(img, id, (int)insets.top, (int)insets.right, (int)insets.bottom, (int)insets.left);
                        
                        resm.put(id+".press#border", border);
                        pressedBorder.border = border;
                        resm.targetDensity = targetDensity;
                    });
                } else {
                    onComplete.add(()-> {
                        resm.put(id+".press#border", borders.get(borders.indexOf(pressedBorder)).border);
                    });
                    
                }
            } else if (e.requiresBackgroundImageGeneration(pressedStyles)) {
                if (!borders.contains(b)) {
                    borders.add(b);
                    resm.addImageProcessor(id+".press", (img) -> {
                        int i = 1;
                        while(res.containsResource(id + "_" + i + ".png")) {
                            i++;
                        }
                        String prefix = id + "_" + i + ".png";
                        resm.targetDensity = getSourceDensity(pressedStyles, resm.targetDensity);
                        Image im = resm.storeImage(EncodedImage.create(ResourcesMutator.toPngOrJpeg(img)), prefix, false);
                        pressedBorder.imageId = prefix;
                        resm.put(id+".press#bgImage", im/*res.findId(im, true)*/);
                        resm.targetDensity = targetDensity;
                        //resm.put(id+".press#bgType", Style.B)
                    });
                } else {
                    onComplete.add(()->{
                        resm.put(id+".press#bgImage", res.findId(pressedBorder.imageId, true));
                    });
                }
            }
            
            Element disabled = e.getDisabled();
            Map<String,LexicalUnit> disabledStyles = (Map<String,LexicalUnit>)disabled.getFlattenedStyle();
            //System.out.println(id+" disabled "+disabledStyles);
            b = disabled.createBorder(disabledStyles);
            Border disabledBorder = b;
            if (e.requiresImageBorder(disabledStyles)) {
                if (!borders.contains(b)) {
                    
                    borders.add(b);
                    resm.addImageProcessor(id+".dis", (img) -> {
                        Insets disabledInsets = disabled.getImageBorderInsets(disabledStyles, img.getWidth(), img.getHeight());
            
                        resm.targetDensity = getSourceDensity(disabledStyles, resm.targetDensity);
                        com.codename1.ui.plaf.Border border = resm.create9PieceBorder(img, id, (int)disabledInsets.top, (int)disabledInsets.right, (int)disabledInsets.bottom, (int)disabledInsets.left);
                        disabledBorder.border = border;
                        resm.put(id+".dis#border", border);
                        resm.targetDensity = targetDensity;
                    });
                } else {
                    onComplete.add(()-> {
                        resm.put(id+".dis#border", borders.get(borders.indexOf(disabledBorder)).border);
                    });
                    
                }
            } else if (e.requiresBackgroundImageGeneration(disabledStyles)) {
                if (!borders.contains(b)) {
                    borders.add(b);
                    resm.addImageProcessor(id+".dis", (img) -> {
                        int i = 1;
                        while(res.containsResource(id + "_" + i + ".png")) {
                            i++;
                        }
                        String prefix = id + "_" + i + ".png";
                        resm.targetDensity = getSourceDensity(disabledStyles, resm.targetDensity);
                        Image im = resm.storeImage(EncodedImage.create(ResourcesMutator.toPngOrJpeg(img)), prefix, false);
                        disabledBorder.image = im;
                        resm.put(id+".dis#bgImage", im);
                        resm.targetDensity = targetDensity;
                        //resm.put(id+".press#bgType", Style.B)
                    });
                } else {
                    onComplete.add(()->{
                        resm.put(id+".dis#bgImage", disabledBorder.image);
                    });
                }
            }
            
        }
        //System.out.println(generateCaptureHtml());
        if (requiresCaptureHtml()) {
            resm.createScreenshots(webviewProvider.getWebView(), generateCaptureHtml(), this.baseURL.toExternalForm());
        }
        for (Runnable r : onComplete) {
            r.run();
        }
        //System.out.println(res.getTheme("Theme"));
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
               
        
        boolean isStyleNativelySupported() {
            return this.styleTop == null || isBorderTypeNativelySupported(this.styleTop);
        }
        
        boolean isBorderLineOrNone() {
            return styleTop == null || "none".equals(styleTop) || "line".equals(styleTop) || "solid".equals(styleTop);
        }
        
        public boolean canBeAchievedWithUnderlineBorder(Map<String,LexicalUnit> styles) {
            if (this.hasGradient() || !isBorderLineOrNone() || !isNone(backgroundImageUrl) || hasBoxShadow()) {
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
        
        public boolean canBeAchievedWithRoundRectBorder(Map<String,LexicalUnit> styles) {
            //System.out.println("Checking if we can achieve with background image generation "+styles);
            if (hasUnequalBorders() || this.hasGradient() || !isBorderLineOrNone() || !isNone(backgroundImageUrl) || hasBoxShadow()) {
                //System.out.println("Failed test 1");
                //System.out.println("unequalBorders? "+hasUnequalBorders());
                //System.out.println("Has gradient? "+hasGradient());
                //System.out.println("BorderLineOrNone? "+isBorderLineOrNone());
                //System.out.println("Background Image URL: "+backgroundImageUrl);
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
            boolean topLeft=false;
            boolean topRight = false;
            boolean bottomLeft = false;
            boolean bottomRight = false;
            for (String cornerStyle : radiusAtts) {
                ScaledUnit u = (ScaledUnit)styles.get(cornerStyle);
                if (u != null && u.getPixelValue() != 0) {
                    if (cornerStyle.indexOf("top-left") != -1) {
                        topLeft = true;
                    }
                    if (cornerStyle.indexOf("top-right") != -1) {
                        topRight = true;
                    }
                    if (cornerStyle.indexOf("bottom-left") != -1) {
                        bottomLeft = true;
                    }
                    if (cornerStyle.indexOf("bottom-right") != -1) {
                        bottomRight = true;
                    }
                    if (val != null && val.getPixelValue() != u.getPixelValue()) {
                        // We have more than one non-zero corner radius
                        //System.out.println("Failed corner test");
                        return false;
                        
                    }
                    val = u;
                }
            }
            
            
            if (topLeft != topRight || bottomLeft != bottomRight) {
                // Resource files don't currently support topLeftMode, topRightMode, bottomLeftMode, and bottomRightMode
                // so we need to fall back to image borders if the left and right have different radii
                return false;
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
                        ///System.out.println("Failed the width test");
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
                        //System.out.println("Failed the color test");
                        return false;
                    } 
                    borderColorSet = true;
                    colorInt = getColorInt(uColor);
                    alphaInt = getColorAlphaInt(uColor);
                }
            }
            
            //System.out.println("Can be achieved");
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
                    && eq(borderColorLeft, b.borderColorLeft);
            
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
                    + styleTop + styleRight + styleBottom + styleLeft;
        }
        
        public boolean hasBorderRadius() {
            return !isZero(borderRadius);
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
            return !isNone(boxShadow);
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
        Map properties = new HashMap();
        
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
            //System.out.println(sb);
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
            //System.out.println("Trying to get box shadow: "+boxShadow);
            
            if (isNone(boxShadow)) {
                return i;
            }
            
            //System.out.println("It is not none "+boxShadow);
            
            ScaledUnit insetUnit = boxShadow;
            while (insetUnit != null) {
                if ("inset".equals(insetUnit.getStringValue())) {
                    //System.out.println("it is inset");
                    return i;
                }
                insetUnit = (ScaledUnit)insetUnit.getNextLexicalUnit();
            }
            
            
            double hShadow = boxShadow.getPixelValue();
            //System.out.println("hShadow is "+hShadow);
            boxShadow = (ScaledUnit)boxShadow.getNextLexicalUnit();
            
            double vShadow = boxShadow.getPixelValue();
            boxShadow = (ScaledUnit)boxShadow.getNextLexicalUnit();
            
            double blur = 0;
            
            if (boxShadow != null) {
                blur = boxShadow.getPixelValue();
                boxShadow = (ScaledUnit)boxShadow.getNextLexicalUnit();
            }
            double spread = 0;
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
            Map styles = new HashMap();
            styles.putAll(getFlattenedStyle());
            
            return ""+getBoxShadowPadding(styles);
        }
        
        String generateStyleCSS() {
            StringBuilder sb = new StringBuilder();
            Map styles = new HashMap();
            styles.putAll(getFlattenedStyle());
            
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
                String property = (String)key;
                LexicalUnit value = (LexicalUnit)styles.get(key);
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
        }
        
        void setParent(String name) {
            Element parentEl = getElementByName(name);
            Element self = this;
            if (this.isSelectedStyle() || this.isDisabledStyle() || this.isDisabledStyle() || this.isUnselectedStyle()) {
                self = this.parent;
            }
            //System.out.println("Setting parent of "+self+" to "+name);
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
            Map out = new HashMap();
            
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
            Map out = new HashMap();
            
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
            Map out = new HashMap();
            
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
            Map out = new HashMap();
            
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
            Map out = new HashMap();
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
//            //System.out.println("Trying to get gradient type");
//            LexicalUnit background = styles.get("background");
//            if (background == null) {
//                return -1;
//            }
//            //System.out.println("Step 1");
//            int gradientType = -1;
//            if (background.getFunctionName() != null && background.getFunctionName().equals("linear-gradient")) {
//                ScaledUnit params = (ScaledUnit)background.getParameters();
//                if (params == null) {
//                    return -1;
//                }
//                //System.out.println("Step 2");
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
//                //System.out.println("Step 3");
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
//                //System.out.println("gradientType so far is "+gradientType);
//                //System.out.println("Params is "+params);
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
//                //System.out.println("Step 4");
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
//            //System.out.println("And gradient type is "+gradientType);
//            return gradientType;
//        
//        
//        }
        
        Map style = new HashMap();
        
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
            //System.out.println("Border insets "+i);
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
                
                
                return i;
            }
            
            if (!isNone(borderRadiusTopLeftX) || !isNone(borderRadiusBottomLeftX)) {
                i.left = Math.max(getPixelValue(borderRadiusTopLeftX), getPixelValue(borderRadiusBottomLeftX)) + 
                        borderInsets.left + boxShadowInsets.left + boxShadowInsets.right + 1;
            } else {
            
                i.left = borderInsets.left + boxShadowInsets.left + boxShadowInsets.right + 1;
            }
            if (!isNone(borderRadiusTopRightX) || !isNone(borderRadiusBottomRightX)) {
                i.right = Math.max(getPixelValue(borderRadiusTopRightX), getPixelValue(borderRadiusBottomRightX)) + 
                        borderInsets.right + boxShadowInsets.right + boxShadowInsets.left + 1;
            } else {
                i.right = borderInsets.right + boxShadowInsets.left + boxShadowInsets.right + 1;
            }
            if (!isNone(borderRadiusTopLeftY) || !isNone(borderRadiusTopRightY)) {
                i.top = Math.max(getPixelValue(borderRadiusTopLeftY), getPixelValue(borderRadiusTopRightY)) + 
                        borderInsets.top + boxShadowInsets.top + boxShadowInsets.bottom + 1;
            } else {
            
                i.top = borderInsets.top + boxShadowInsets.top + boxShadowInsets.bottom + 1;
            }
            if (!isNone(borderRadiusBottomLeftY) || !isNone(borderRadiusBottomRightY)) {
                i.bottom = Math.max(getPixelValue(borderRadiusBottomLeftY), getPixelValue(borderRadiusBottomRightY)) + 
                        borderInsets.bottom + boxShadowInsets.bottom + boxShadowInsets.top + 1;
            } else {
            
                i.bottom = borderInsets.bottom + boxShadowInsets.bottom + boxShadowInsets.top + 1;
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
            
            //System.out.println("Insets for 9-piece border: "+i);
            //i.top = 10;
            //i.bottom = 10;
            //i.left = 10;
            //i.right = 10;
            return i;
        }
        
        public String getThemeFgColor(Map<String,LexicalUnit> style) {
            LexicalUnit color = style.get("color");
            return getColorString(color);
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
            if (b.canBeAchievedWithRoundRectBorder(style) || b.canBeAchievedWithUnderlineBorder(style)) {
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
            if (b.canBeAchievedWithRoundRectBorder(style) || b.canBeAchievedWithUnderlineBorder(style)) {
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
                            /*
                            int ttfFontSize = 1; // medium
                            float actualSize = 14f;
                            switch (iFontSizeType) {
                                case Font.SIZE_SMALL:
                                    ttfFontSize = 0;
                                    actualSize = 11f;
                                    break;
                                case Font.SIZE_LARGE:
                                    ttfFontSize = 2;
                                    actualSize = 20f;
                                    break;
                            }
                            // Check for a more specific font size
                            if (fontSize != null) {
                                switch (fontSize.getLexicalUnitType()) {
                                    case LexicalUnit.SAC_MILLIMETER:
                                        ttfFontSize = 3;
                                        actualSize = fontSize.getFloatValue();
                                        break;
                                    case LexicalUnit.SAC_PIXEL:
                                    case LexicalUnit.SAC_POINT:
                                        ttfFontSize = 4;
                                        actualSize = fontSize.getFloatValue();
                                        break;

                                    case LexicalUnit.SAC_CENTIMETER:
                                        ttfFontSize = 3;
                                        actualSize = fontSize.getFloatValue()*10f;
                                        break;
                                    case LexicalUnit.SAC_INCH:
                                        ttfFontSize = 3;
                                        actualSize = fontSize.getFloatValue()*25f;
                                        break;
                                    case LexicalUnit.SAC_EM:
                                        ttfFontSize = 4;
                                        actualSize = fontSize.getFloatValue()* 14f;
                                        break;
                                    case LexicalUnit.SAC_PERCENTAGE:
                                        ttfFontSize = 4;
                                        actualSize = fontSize.getFloatValue() /100f * 14f;
                                        break;

                                }
                            }*/
                            FontFace face = findFontFace(fontFamily.getStringValue());
                            if (face != null) {
                                ttfFontFile = face.getFontFile();
                                /*
                                if (fontFile != null) {
                                    Font sys = Font.createSystemFont(iFontFace,iFontStyle, iFontSizeType);
                                    //System.out.println("TTF Font "+fontFile+" "+ttfFontSize + " " +actualSize + " "+sys);
                                    ttfFont = new EditorTTFFont(fontFile, ttfFontSize, actualSize, sys);
                                    break loop;
                                }
                                */
                            } else {
                                
                                if(fontFamily.getStringValue().startsWith("native:")) {
                                    ttfFontName = fontFamily.getStringValue();
                                    //Font sys = Font.createSystemFont(iFontFace,iFontStyle, iFontSizeType);
                                    //ttfFont = new EditorTTFFont(fontFamily.getStringValue(), ttfFontSize, actualSize, sys);
                                    //break loop;
                                }
                            }
                        }   
                            
                    }
                }
                fontFamily = fontFamily.getNextLexicalUnit();
            }
           
            if (ttfFontFile != null) {
                return new EditorTTFFont(ttfFontFile, ttfSizeType, ttfSize, Font.createSystemFont(sysFace, sysStyle, sysSize));
            } else {
                return new EditorTTFFont(ttfFontName, ttfSizeType, ttfSize, Font.createSystemFont(sysFace, sysStyle, sysSize));
            }
            /*
            if (ttfFont != null) {
                return ttfFont;
            } else {
                return Font.createSystemFont(iFontFace,iFontStyle, iFontSizeType);
            }*/
            
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
            if ("rgba".equals(bgColor.getFunctionName())) {
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



            //System.out.println("Round border: "+out.getShadowX()+", "+out.getShadowY()+", "+out.getShadowSpread()+", "+out.getShadowOpacity());
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
                //System.out.println("Checking corner style "+cornerStyle+" with value "+u);
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
            //System.out.println("TopLeftRadius is : "+topLeftRadius+" isZero? "+isZero(topLeftRadius));
            //System.out.println("BottomRight Radius is : "+bottomRightRadius+" isZero? "+isZero(bottomRightRadius));
            if (!isZero(bottomLeftRadius) && !isZero(bottomRightRadius) && isZero(topLeftRadius) && isZero(topRightRadius)) {
                out.bottomOnlyMode(true);
            } else if (isZero(bottomLeftRadius) && isZero(bottomRightRadius) && !isZero(topLeftRadius) && !isZero(topRightRadius)) {
                out.topOnlyMode(true);
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



            //System.out.println("Round border: "+out.getShadowX()+", "+out.getShadowY()+", "+out.getShadowSpread()+", "+out.getShadowOpacity());
            return out;
        }
        public com.codename1.ui.plaf.Border getThemeBorder(Map<String,LexicalUnit> styles) {
            Border b = this.createBorder(styles);
            LexicalUnit cn1BackgroundType = styles.get("cn1-background-type");
            
            if (cn1BackgroundType != null && usesRoundBorder(styles)) {
                return createRoundBorder(styles);
            }
            if (b.hasBorderRadius()) {
                return createRoundRectBorder(styles);
            }
            if (b.canBeAchievedWithUnderlineBorder(styles)) {
                return createUnderlineBorder(styles);
            }
            if (b.hasUnequalBorders()) {
                //System.out.println("We have unequal borders");
                return com.codename1.ui.plaf.Border.createCompoundBorder(
                        getThemeBorder(styles, "top"),
                        getThemeBorder(styles, "bottom"),
                        getThemeBorder(styles, "left"),
                        getThemeBorder(styles, "right")
                );    
            } else {
                //System.out.println("Web have equal borders");
                return getThemeBorder(styles, "top");
            }
        }
        
        
        
        
        public String getThemeDerive(Map<String,LexicalUnit> styles, String suffix) {
            
            LexicalUnit derive = styles.get("cn1-derive");
            //System.out.println("Cn1 derive "+derive);
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
                //    System.out.println("Creating dashed border with thickness "+thickness+" and color "+color);
                //    com.codename1.ui.plaf.Border br =  com.codename1.ui.plaf.Border.createDashedBorder(thickness, color);
                //    System.out.println("Dashed border created "+br);
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
        //System.out.println("Applying property "+property);
        switch (property) {
            
            case "opacity" : {
                style.put("opacity", value);
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
                //System.out.println("In cn1-derive");
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
                //System.out.println("Setting background");
                
                while (value != null) {
                    //System.out.println(value);
                    //System.out.println(value.getLexicalUnitType());
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
                            //System.out.println("Setting background color "+value);
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
                                apply(style, "cn1-border-type", units.get(0));
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
                            if ("rgba".equals(value.getFunctionName())) {
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
    
    Element getElementForSelector(Selector sel) {
        switch (sel.getSelectorType()) {
            case Selector.SAC_ANY_NODE_SELECTOR :
                return anyNodeStyle;
            case Selector.SAC_ELEMENT_NODE_SELECTOR : {
                ElementSelector esel = (ElementSelector)sel;
                return getElementByName(esel.getLocalName());
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
                        Element parent = getElementForSelector(esel);
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
    
    public void apply(Selector sel, String property, LexicalUnit value) {
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
        Element el = getElementForSelector(sel);
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
    
    private IntValue getIntValue(LexicalUnit value) {
        
        IntValue out = new IntValue();
        
        if (value == null) {
            return out;
        }
        
        int pixelValue = 0;
        byte unit = 0;
        switch (value.getLexicalUnitType()) {
            case LexicalUnit.SAC_PIXEL :
            case LexicalUnit.SAC_POINT :
            case LexicalUnit.SAC_INTEGER:
                unit = (byte)0;
                pixelValue = Math.round(value.getFloatValue());
                break;
            case LexicalUnit.SAC_MILLIMETER :
                unit = (byte)2;
                pixelValue = Math.round(value.getFloatValue());
                break;
            case LexicalUnit.SAC_CENTIMETER :
                unit = (byte) 2;
                pixelValue = Math.round(value.getFloatValue() * 10);
                break;
            default :
                throw new RuntimeException("Unsupported unit for inset "+value.getDimensionUnitText());
        }
        
        out.value = pixelValue;
        out.unit = unit;
        return out;
            
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
                throw new RuntimeException("Unsupported unit for inset "+value.getDimensionUnitText());
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
    
    private Insets getInsets(LexicalUnit value) {
        Insets i = new Insets();
        int index = Insets.TOP;
        while (value != null) {
            int pixelValue = 0;
            byte unit = 0;
            switch (value.getLexicalUnitType()) {
                case LexicalUnit.SAC_PIXEL :
                case LexicalUnit.SAC_POINT :
                    unit = (byte)0;
                    pixelValue = Math.round(value.getFloatValue());
                    break;
                case LexicalUnit.SAC_MILLIMETER :
                    unit = (byte)2;
                    pixelValue = Math.round(value.getFloatValue());
                    break;
                case LexicalUnit.SAC_CENTIMETER :
                    unit = (byte) 2;
                    pixelValue = Math.round(value.getFloatValue() * 10);
                    break;
                default :
                    throw new RuntimeException("Unsupported unit for inset "+value.getDimensionUnitText());
            }
            
            switch (index) {
                case Insets.TOP :
                    i.set(Insets.TOP, pixelValue, unit);
                    i.set(Insets.RIGHT, pixelValue, unit);
                    i.set(Insets.BOTTOM, pixelValue, unit);
                    i.set(Insets.LEFT, pixelValue, unit);
                    break;
                    
                case Insets.RIGHT :
                    i.set(Insets.LEFT, pixelValue, unit);
                    i.set(Insets.RIGHT, pixelValue, unit);
                    break;
                case Insets.BOTTOM :
                    i.set(Insets.BOTTOM, pixelValue, unit);
                    break;
                case Insets.LEFT :
                    i.set(Insets.LEFT, pixelValue, unit);
                    
            }
            
            index++;
            value = value.getNextLexicalUnit();
        }
        return i;
    }
    
    /*
    Border getBorder(LexicalUnit lu) {
        int width = 1;
        
        while (lu != null) {
            switch (lu.getLexicalUnitType()) {
                case LexicalUnit.SAC_PIXEL :
                    
            }
        }
    }
    */
    
    Color getColor(LexicalUnit color) {
        String str =getColorString(color);
        return Color.web("#"+str);
        
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
            if ("rgba".equals(bgColor.getFunctionName())) {
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
    
    static String getColorString(LexicalUnit color) {
        
        if (color == null) {
                return null;
            }
            switch (color.getLexicalUnitType()) {
                case LexicalUnit.SAC_IDENT:
                case LexicalUnit.SAC_RGBCOLOR: {
                    
                    //System.out.println("Lex type "+color.getLexicalUnitType());
                    //System.out.println("Color: "+color.getStringValue());
                    //System.out.println(color);
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
                    //System.out.println("Decoding color "+colorStr);
                    Color c = Color.web(colorStr);
                    return String.format( "%02X%02X%02X",
                    (int)( c.getRed() * 255 ),
                    (int)( c.getGreen() * 255 ),
                    (int)( c.getBlue() * 255 ) );
                    
                }
                case LexicalUnit.SAC_FUNCTION: {
                    if ("rgba".equals(color.getFunctionName())) {
                        /*
                        System.out.println("RGBA value " + color + " "+color.getClass());
                        ScaledUnit param = (ScaledUnit)color.getParameters();
                        System.out.println("Params "+param);
                        System.out.println(param.getLexicalUnitType());
                        
                        double r = param.getNumericValue();
                        param = (ScaledUnit)param.getNextNumericUnit();
                        
                        double g = param.getNumericValue();
                        param = (ScaledUnit)param.getNextNumericUnit();
                        
                        double b = param.getNumericValue();
                        param = (ScaledUnit)param.getNextNumericUnit();
                        
                        double a = param.getNumericValue();
                        System.out.println(r+","+g+","+b+","+a);
                        */
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
    
    public static CSSTheme load(URL uri) throws IOException {
        try {
            System.setProperty("org.w3c.css.sac.parser", "org.w3c.flute.parser.Parser");
            InputSource source = new InputSource();
            InputStream stream = uri.openStream();
            
            source.setCharacterStream(new InputStreamReader(stream, "UTF-8"));
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
                
                SelectorList currSelectors;
                FontFace currFontFace;
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
                    
                }
                
                @Override
                public void endMedia(SACMediaList sacml) throws CSSException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                    
                    lu = new ScaledUnit(lu, theme.currentDpi, theme.getPreviewScreenWidth(), theme.getPreviewScreenHeight());
                    if (currFontFace != null) {
                        switch (string) {
                            case "font-family" :
                                currFontFace.fontFamily = lu;
                                break;
                            case "font-style" :
                                currFontFace.fontStyle = lu;
                                break;
                            case "font-stretch" :
                                currFontFace.fontStretch = lu;
                                break;
                            case "src" :
                                currFontFace.src = lu;
                                break;
                            case "font-weight" :
                                currFontFace.fontWeight = lu;
                                break;
                        }
                    } else {

                        int len = currSelectors.getLength();
                        for (int i=0; i<len; i++) {
                            Selector sel = currSelectors.item(i);
                            theme.apply(sel, string, lu);

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
