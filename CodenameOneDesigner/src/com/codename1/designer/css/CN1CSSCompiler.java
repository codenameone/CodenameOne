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


import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.util.EditableResources;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import org.w3c.css.sac.*;  
import org.w3c.css.sac.helpers.*;  
import java.net.*;  
import java.io.*;  
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;



/**
 *
 * @author shannah
 */
public class CN1CSSCompiler implements DocumentHandler {
    
    
    
    private Map<String, Selector> selectors = new HashMap<String,Selector>();
    
    class NinePieceBorder {
        int top, left, bottom, right, width, height;
        
        
    }
    
    
    
    
    
    

    public static final int DEFAULT_TARGET_DENSITY = com.codename1.ui.Display.DENSITY_VERY_HIGH;
    
    int targetDensity = DEFAULT_TARGET_DENSITY;
    Set<Integer> includedDensities = new HashSet<Integer>();
    
    File inputFile;
    
    File outputFile;
    
    //Properties props;
    Hashtable theme;
    SelectorList currSelectors;
    EditableResources res;
    
    static BrowserComponent web;
    static Runnable webpageLoadedCallback;
    static JFrame frm;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "true");
        try {
            //Display.init(null);
            JavaSEPort.setShowEDTViolationStacks(false);
            JavaSEPort.setShowEDTWarnings(false);
            frm = new JFrame("Placeholder");
            frm.setVisible(false);
            Display.init(frm.getContentPane());
            System.setProperty("org.w3c.css.sac.parser", "org.w3c.flute.parser.Parser");
            
        //Platform.runLater(new Runnable() {

        //        @Override
        //        public void run() {
            InputStream stream = null;  

            InputSource source = new InputSource();
            URL uri = CN1CSSCompiler.class.getResource("test.css");
            stream = uri.openStream();
            source.setByteStream(stream);
            source.setURI(uri.toString());
            ParserFactory parserFactory = new ParserFactory();
            Parser parser = parserFactory.makeParser();
            parser.setDocumentHandler(new CN1CSSCompiler());
            parser.parseStyleSheet(source);
            stream.close();
        } finally {
            
            frm.dispose();
            System.exit(0);
            //Display.getInstance().exitApplication();
            //Display.deinitialize();
        }

            
        
    
    
    
    }
    
    static final Set<Integer> defaultDensities = new HashSet<Integer>();
    static {
        defaultDensities.add(Display.DENSITY_LOW);
        defaultDensities.add(Display.DENSITY_MEDIUM);
        defaultDensities.add(Display.DENSITY_HIGH);
        defaultDensities.add(Display.DENSITY_VERY_HIGH);
        
    }
    
    public CN1CSSCompiler() {
        inputFile = new File("test.css");
        outputFile = new File("test.css.res");
        
        
        
        
        
        
    }
    

    @Override
    public void startDocument(InputSource is) throws CSSException {
        //props = new Properties();
        theme = new Hashtable();
        res = new EditableResources();
        includedDensities.clear();
        includedDensities.addAll(defaultDensities);
    }

    
    
    @Override
    public void endDocument(InputSource is) throws CSSException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        /*
                try {
            
            File tmpProps = File.createTempFile("theme", "properties");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(tmpProps);
                props.store(fos, "Updating properties");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Exception ex) {

                    }
                }
            }
            /(props);
            //CodenameOneTask cn1 = new CodenameOneTask();
            //cn1.setDest(outputFile);

            ThemeTask theme = new ThemeTask();
            theme.setDescription("Theme for "+inputFile.getName());
            theme.setFile(tmpProps);
            theme.setName(inputFile.getName());
            //cn1.addTheme(theme);
            //cn1.execute();
            
            EditableResources output = new EditableResources();
            
            theme.addToResources(output);
            System.out.println(output.getTheme(inputFile.getName()));
            

            DataOutputStream resFile = new DataOutputStream(new FileOutputStream(outputFile));
            output.save(resFile);
            resFile.close();
            System.out.println("Tmp props: "+tmpProps);
            //tmpProps.delete();
            
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
                */
        
        
        /*
        EditableResources output = new EditableResources();
        Hashtable theme = new Hashtable();
        for (String key : props.stringPropertyNames()) {
            theme.put(key, props.get(key));
        }
       
        output.setTheme("test.css", theme);
                */
        try {
            
            String html = "<html><head><style type='text/css'>div { background: linear-gradient( 45deg, blue, red ); width: 100%; height: 100%; border-radius: 20px; border: 1px solid blue;}</style>"
                    + "</head><body style='background: rgba(0,0,0,0);'><div>Hello</div></body></html>";
            BufferedImage testImage = createHtmlScreenshot(html);
            //BufferedImage testImage2 = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
            //Graphics2D g = testImage2.createGraphics();
            //g.setColor(java.awt.Color.RED);
            //g.fillRect(0, 0, 50, 500);
            //g.dispose();
            
            create9PieceBorder(testImage, "testborder", 20, 20, 20, 20);
            
            
            
            theme.put("uninitialized", true);
            //System.out.println(theme);
            res.setTheme("test", theme);
            //for (Object key : theme.keySet()) {
            //    res.setThemeProperty("test.css", (String)key, theme.get(key));
            //}
            
            //res.setThemeProperties("test.css", theme);
            //res.getTheme("test.css").putAll(theme);
            //System.out.println(res.getTheme("test"));
            DataOutputStream resFile = new DataOutputStream(new FileOutputStream(outputFile));
            res.save(resFile);
            //res.saveXML(outputFile);
            
            resFile.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        
        
    }

    @Override
    public void comment(String string) throws CSSException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void endFontFace() throws CSSException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void startSelector(SelectorList sl) throws CSSException {
        currSelectors = sl;
        int len = sl.getLength();
        for (int i=0; i<len; i++) {
            selectors.put(convertSelector(sl.item(i), ""), sl.item(i));
        }
    }

    
    
    
    @Override
    public void endSelector(SelectorList sl) throws CSSException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void property(String string, LexicalUnit lu, boolean bln) throws CSSException {
        int len = currSelectors.getLength();
        
        for (int i=0; i < len; i++) {
            Selector sel = currSelectors.item(i);
              
            if (sel instanceof ElementSelector) {
                
                     
                ElementSelector esel = (ElementSelector)sel;
                if ("margin".equals(string)) {
                    setMargin(esel, lu);
                } else if ("border".equals(string)) {
                    setBorder(esel, lu);
                }
                /*
                System.out.println(esel.getLocalName());
                System.out.println(string);
                System.out.println(lu);
                System.out.println(lu.getLexicalUnitType());
                System.out.println(lu.getParameters());
                System.out.println("Next: "+lu.getNextLexicalUnit());
                System.out.println("Float: "+lu.getFloatValue());
                */
            }
            
            
            
        }
    }
    
    private List<Integer> getMargin(Selector sel) {
        List<Integer> out = new ArrayList<Integer>();
        String marginStr = (String)theme.get(convertSelector(sel, "margin"));//, "0,0,0,0").split(",");
        if (marginStr == null) {
            marginStr = "0,0,0,0";
        }
        String[] marginArr = marginStr.split(",");
        for (String v : marginArr) {
            out.add(Integer.parseInt(v));
        }
        return out;
        
    }
    
    
    
    private byte[] getMarginUnits(Selector sel) {
        byte[] units = (byte[])theme.get(convertSelector(sel, "marUnit"));
        if (units == null) {
            units = new byte[]{0,0,0,0};
        }
        return units;
    }
    
    private void setBorder(Selector sel, LexicalUnit lu) {
        selectors.get(convertSelector(sel, null));
    }
    
    private void setMargin(Selector sel, LexicalUnit lu) {
        List<Integer> units = new ArrayList<Integer>();
        List<Integer> vals = new ArrayList<Integer>();
        UnitVal cssUnit = new UnitVal();
        UnitVal cn1Unit = new UnitVal();
        do {
            cssUnit.unit = lu.getLexicalUnitType();
            cssUnit.val = lu.getFloatValue();
            //System.out.println("Flval is "+lu.getFloatValue());
            //System.out.println("Val is "+cssUnit.val+ " for "+lu);
            convertUnitToCN1(cssUnit, cn1Unit);
            vals.add(Math.round(cn1Unit.val));
            units.add(cn1Unit.unit);
        } while ((lu = lu.getNextLexicalUnit()) != null );
        
        StringBuilder valsStr = new StringBuilder();
        //StringBuilder unitsStr = new StringBuilder();
        byte[] u = new byte[]{0,0,0,0};
        
        switch (vals.size()) {
            case 1:
                for (int i=0; i<4; i++) {
                    valsStr.append(vals.get(0)).append(",");
                    u[i] = (byte)(int)units.get(0);
                }
                valsStr.setLength(valsStr.length()-1);
                //unitsStr.setLength(unitsStr.length()-1);
                break;
            
                
        }
        
        theme.put(convertSelector(sel, "margin"), valsStr.toString());
        theme.put(convertSelector(sel, "marUnit"), u);
    }
    
    private class UnitVal {
        int unit;
        float val;
    }
    
    private void convertUnitToCN1(UnitVal in, UnitVal out) {
        switch (in.unit) {
            case LexicalUnit.SAC_MILLIMETER:
                out.unit = 2;
                out.val = in.val;
                break;
            case LexicalUnit.SAC_PIXEL:
                out.unit = 0;
                out.val = in.val;
                break;
            case LexicalUnit.SAC_POINT:
                out.unit = 2;
                out.val = in.val / 72;
                break;
            case LexicalUnit.SAC_PERCENTAGE:
                out.unit = 1;
                out.val = in.val;
                break;
            default:
                throw new RuntimeException("Unit type not handled "+in.unit);
        }
    }
    
    /**
     * Converts a selector from a CSS selector to a property in the CN1 theme
     * format.
     * @param sel The selector
     * @param key The property key (cn1 specific)
     * @return The cn1 property name.
     */
    private String convertSelector(Selector sel, String key) {
        StringBuilder out = new StringBuilder();
        switch (sel.getSelectorType()) {
            case Selector.SAC_ELEMENT_NODE_SELECTOR:
                return ((ElementSelector)sel).getLocalName() + "." + key;
                /*
            case Selector.SAC_CONDITIONAL_SELECTOR: {
                ConditionalSelector csel = (ConditionalSelector)sel;
                switch (csel.getCondition().getConditionType()) {
                    case Condition.SAC_PSEUDO_CLASS_CONDITION:
                        AttributeCondition acond = (AttributeCondition)csel.getCondition();
                        String name = acond.getLocalName();
                        if ("disabled".equals(name)) {
                            return convertSelector(csel.getSimpleSelector())+".disabled"
                        }
                }
            }*/
                
        }
       return "";
    }
    
    
    
    private void create9PieceBorder(BufferedImage img, String prefix, int top, int right, int bottom, int left) {
        //BufferedImage buff = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        //Graphics2D bg2d = buff.createGraphics();
        //bg2d.drawImage(img.getSubimage(get(cropLeft), get(cropTop), img.getWidth() - get(cropLeft) - get(cropRight),
        //            img.getHeight() - get(cropTop) - get(cropBottom)), get(cropLeft), get(cropTop), null);
        
        //bg2d.dispose();
        //img = buff;
        BufferedImage topLeft = img.getSubimage(0, 0, left, top);
        BufferedImage topRight = img.getSubimage(img.getWidth() - right, 0, right, top);
        BufferedImage bottomLeft = img.getSubimage(0, img.getHeight() - bottom, left, bottom);
        BufferedImage bottomRight = img.getSubimage(img.getWidth() - right, img.getHeight() - bottom, right, bottom);
        BufferedImage center = img.getSubimage(left, top, img.getWidth() - right - left, img.getHeight() - bottom - top);
        BufferedImage topImage = img.getSubimage(left, 0, img.getWidth() - left - right, top);
        BufferedImage bottomImage = img.getSubimage(left, img.getHeight() - bottom, img.getWidth() - left - right, bottom);
        BufferedImage leftImage = img.getSubimage(0, top, left, img.getHeight() - top - bottom);
        BufferedImage rightImage = img.getSubimage(img.getWidth() - right, top, right, img.getHeight() - top - bottom);

        // optimize the size of the center/top/left/bottom/right images which is a HUGE performance deterant
        if(center.getWidth() < 10 || center.getHeight() < 10) {
            center = getScaledInstance(center, Math.max(20, center.getWidth()), Math.max(20, center.getHeight()));
            topImage = getScaledInstance(topImage, Math.max(20, topImage.getWidth()), topImage.getHeight());
            leftImage = getScaledInstance(leftImage, leftImage.getWidth(), Math.max(20, leftImage.getHeight()));
            rightImage = getScaledInstance(rightImage, rightImage.getWidth(), Math.max(20, rightImage.getHeight()));
            bottomImage = getScaledInstance(bottomImage, Math.max(20, bottomImage.getWidth()), bottomImage.getHeight());
        }
        
        com.codename1.ui.EncodedImage topLeftCodenameOne = com.codename1.ui.EncodedImage.create(toPng(topLeft));
        com.codename1.ui.EncodedImage topRightCodenameOne = com.codename1.ui.EncodedImage.create(toPng(topRight));
        com.codename1.ui.EncodedImage bottomLeftCodenameOne = com.codename1.ui.EncodedImage.create(toPng(bottomLeft));
        com.codename1.ui.EncodedImage bottomRightCodenameOne = com.codename1.ui.EncodedImage.create(toPng(bottomRight));
        com.codename1.ui.EncodedImage centerCodenameOne = com.codename1.ui.EncodedImage.create(toPng(center));
        com.codename1.ui.EncodedImage topImageCodenameOne = com.codename1.ui.EncodedImage.create(toPng(topImage));
        com.codename1.ui.EncodedImage bottomImageCodenameOne = com.codename1.ui.EncodedImage.create(toPng(bottomImage));
        com.codename1.ui.EncodedImage leftImageCodenameOne = com.codename1.ui.EncodedImage.create(toPng(leftImage));
        com.codename1.ui.EncodedImage rightImageCodenameOne = com.codename1.ui.EncodedImage.create(toPng(rightImage));
        //String prefix = (String)applies.getAppliesTo().getModel().getElementAt(0);
        topLeftCodenameOne = storeImage(topLeftCodenameOne, prefix +"TopL");
        topRightCodenameOne = storeImage(topRightCodenameOne, prefix +"TopR");
        bottomLeftCodenameOne = storeImage(bottomLeftCodenameOne, prefix +"BottomL");
        bottomRightCodenameOne = storeImage(bottomRightCodenameOne, prefix +"BottomR");
        centerCodenameOne = storeImage(centerCodenameOne, prefix + "Center");
        topImageCodenameOne = storeImage(topImageCodenameOne, prefix + "Top");
        bottomImageCodenameOne = storeImage(bottomImageCodenameOne, prefix + "Bottom");
        leftImageCodenameOne = storeImage(leftImageCodenameOne, prefix + "Left");
        rightImageCodenameOne = storeImage(rightImageCodenameOne, prefix + "Right");
        com.codename1.ui.plaf.Border b = com.codename1.ui.plaf.Border.createImageBorder(topImageCodenameOne, bottomImageCodenameOne, leftImageCodenameOne,
                rightImageCodenameOne, topLeftCodenameOne, topRightCodenameOne,
                bottomLeftCodenameOne, bottomRightCodenameOne, centerCodenameOne);
        //Hashtable newTheme = new Hashtable(res.getTheme(theme));
        //for(int i = 0 ; i < applies.getAppliesTo().getModel().getSize() ; i++) {
        //    newTheme.put(applies.getAppliesTo().getModel().getElementAt(i), b);
        //}
        //((DefaultListModel)applies.getAppliesTo().getModel()).removeAllElements();
        //res.setTheme(theme, newTheme);
    }
    
    public static BufferedImage getScaledInstance(BufferedImage img,
                                           int targetWidth,
                                           int targetHeight)
    {
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        w = img.getWidth();
        h = img.getHeight();
        
        do {
            if (w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            } else {
                w = targetWidth;
            }

            if (h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            } else {
                h = targetHeight;
            }


            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
    
    private com.codename1.ui.EncodedImage storeImage(com.codename1.ui.EncodedImage img, String prefix) {
        int i = 1;
        while(res.containsResource(prefix + "_" + i + ".png")) {
            i++;
        }

        float ratioWidth = 0;
        int multiVal = DEFAULT_TARGET_DENSITY;
        switch(multiVal) {
            // Generate RGB Image
            case 0:
                res.setImage(prefix + "_" + i + ".png", img);
                return img;

            // Generate Medium Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_MEDIUM:
                //multiVal = com.codename1.ui.Display.DENSITY_MEDIUM;
                ratioWidth = 320;
                break;

            // Generate High Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_HIGH:
                ratioWidth = 480;
                //multiVal = com.codename1.ui.Display.DENSITY_HIGH;
                break;

            // Generate Very High Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_VERY_HIGH:
                ratioWidth = 640;
                //multiVal = com.codename1.ui.Display.DENSITY_VERY_HIGH;
                break;

            // Generate HD Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_HD:
                ratioWidth = 1080;
                //multiVal = com.codename1.ui.Display.DENSITY_HD;
                break;

            // Generate HD560 Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_560:
                ratioWidth = 1500;
                //multiVal = com.codename1.ui.Display.DENSITY_560;
                break;

            // Generate HD2 Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_2HD:
                ratioWidth = 2000;
                //multiVal = com.codename1.ui.Display.DENSITY_2HD;
                break;

            // Generate 4k Resolution MultiImage
            case com.codename1.ui.Display.DENSITY_4K:
                ratioWidth = 2500;
                //multiVal = com.codename1.ui.Display.DENSITY_4K;
                break;
        }
        EditableResources.MultiImage multi = new EditableResources.MultiImage();
        multi.setDpi(new int[] {multiVal});
        multi.setInternalImages(new com.codename1.ui.EncodedImage[] {img});
        if(includedDensities.contains(Display.DENSITY_LOW)) {
            float ratio = 240.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_LOW, w, h, multi);
        }

        if(includedDensities.contains(Display.DENSITY_VERY_LOW)) {
            float ratio = 176.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_VERY_LOW, w, h, multi);
        }

        if(includedDensities.contains(Display.DENSITY_MEDIUM)) {
            float ratio = 320.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_MEDIUM, w, h, multi);
        }

        if(includedDensities.contains(Display.DENSITY_HIGH)) {
            float ratio = 480.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_HIGH, w, h, multi);
        }

        if(includedDensities.contains(Display.DENSITY_VERY_HIGH)) {
            float ratio = 640.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_VERY_HIGH, w, h, multi);
        }

        if(includedDensities.contains(Display.DENSITY_HD)) {
            float ratio = 1080.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_HD, w, h, multi);
        }

        if(includedDensities.contains(Display.DENSITY_560)) {
            float ratio = 1500.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_560, w, h, multi);
        }

        if(includedDensities.contains(Display.DENSITY_2HD)) {
            float ratio = 2000.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_2HD, w, h, multi);
        }

        if(includedDensities.contains(Display.DENSITY_4K)) {
            float ratio = 2500.0f / ratioWidth;
            int w = Math.max((int)(img.getWidth() * ratio), 1);
            int h = Math.max((int)(img.getHeight() * ratio), 1);
            multi = scaleMultiImage(multiVal, com.codename1.ui.Display.DENSITY_4K, w, h, multi);
        }

        res.setMultiImage(prefix + "_" + i + ".png", multi);
        return multi.getBest();
    }


    public static byte[] toPng(BufferedImage b) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(b, "png", bo);
            bo.close();
            return bo.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static EditableResources.MultiImage scaleMultiImage(int fromDPI, int toDPI, int scaledWidth, int scaledHeight, EditableResources.MultiImage multi) {
        try {
            int[] dpis = multi.getDpi();
            com.codename1.ui.EncodedImage[] imgs = multi.getInternalImages();
            int fromOffset = -1;
            int toOffset = -1;
            for (int iter = 0; iter < dpis.length; iter++) {
                if (dpis[iter] == fromDPI) {
                    fromOffset = iter;
                }
                if (dpis[iter] == toDPI) {
                    toOffset = iter;
                }
            }
            if (fromOffset == -1) {
                return null;
            }
            EditableResources.MultiImage newImage = new EditableResources.MultiImage();
            if (toOffset == -1) {
                com.codename1.ui.EncodedImage[] newImages = new com.codename1.ui.EncodedImage[imgs.length + 1];
                System.arraycopy(imgs, 0, newImages, 0, imgs.length);
                toOffset = imgs.length;
                int[] newDpis = new int[dpis.length + 1];
                System.arraycopy(dpis, 0, newDpis, 0, dpis.length);
                newDpis[toOffset] = toDPI;
                newImage.setDpi(newDpis);
                newImage.setInternalImages(newImages);
            } else {
                com.codename1.ui.EncodedImage[] newImages = new com.codename1.ui.EncodedImage[imgs.length];
                System.arraycopy(multi.getInternalImages(), 0, newImages, 0, imgs.length);
                newImage.setDpi(dpis);
                newImage.setInternalImages(newImages);
            }
            com.codename1.ui.Image sourceImage = newImage.getInternalImages()[fromOffset];
            BufferedImage buffer = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            buffer.setRGB(0, 0, sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getRGB(), 0, sourceImage.getWidth());
            sourceImage.getRGB();
            sourceImage.getWidth();
            BufferedImage scaled = getScaledInstance(buffer, scaledWidth, scaledHeight);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", output);
            output.close();
            byte[] bytes = output.toByteArray();
            com.codename1.ui.EncodedImage encoded = com.codename1.ui.EncodedImage.create(bytes);
            newImage.getInternalImages()[toOffset] = encoded;
            return newImage;
        } catch (IOException ex) {
            ex.printStackTrace();
                
        }
        return null;
    }

    BufferedImage createHtmlScreenshot(String html) {
        final boolean[] complete = new boolean[1];
        final Object lock = new Object();
        final BufferedImage[] img = new BufferedImage[1];
        
        webpageLoadedCallback = new Runnable() {

            @Override
            public void run() {
                
                com.codename1.ui.Image wi = web.toImage();
                
                img[0] = (BufferedImage)wi.getImage();
                complete[0] = true;
                synchronized(lock) {
                    lock.notify();
                }
            }
            
        };
        CN.callSerially(new Runnable() {

            @Override
            public void run() {
                web.setPage(html, "");
            }
            
        });
        
        
        while (!complete[0]) {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(CN1CSSCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return img[0];
       
    }
    
}
