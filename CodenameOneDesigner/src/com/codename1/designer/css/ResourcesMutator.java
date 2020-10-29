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
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.processing.Result;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EditableResources;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;


/**
 *
 * @author shannah
 */
public class ResourcesMutator {
    private boolean multiImage = true;
    private final EditableResources res;
    private Set<Integer> includedDensities = new HashSet<Integer>();
    public static final int DEFAULT_TARGET_DENSITY = com.codename1.ui.Display.DENSITY_VERY_HIGH;
    
    int targetDensity = DEFAULT_TARGET_DENSITY;
    private Map<String,ImageProcessor> imageProcessors = new HashMap<>();
    private String themeName = "Theme";
    
    public ResourcesMutator(EditableResources res, int targetDensity, double minDpi, double maxDpi) {
        this.res = res;
        this.targetDensity = targetDensity;
        if (res.getTheme(themeName) == null) {
            res.setTheme(themeName, new Hashtable());
        }
        if (minDpi < 160 && maxDpi >= 160) {
            includedDensities.add(Display.DENSITY_LOW);
        }
        if (minDpi <= 160 && maxDpi > 160) {
            includedDensities.add(Display.DENSITY_MEDIUM);
        }
        if (minDpi <= 240 && maxDpi > 240) {
            includedDensities.add(Display.DENSITY_HIGH);
        }
        if (minDpi <= 320 && maxDpi >= 320) {
            includedDensities.add(Display.DENSITY_VERY_HIGH);
        }
        if (minDpi <= 480 && maxDpi >= 480) {
            includedDensities.add(Display.DENSITY_HD);
        }
        if (minDpi <= 640 && maxDpi >= 640) {
            includedDensities.add(Display.DENSITY_2HD);
        }
        if (minDpi <= 60 && maxDpi >= 60){
            includedDensities.add(Display.DENSITY_VERY_LOW);
        }
    }
    
    public void addImageProcessor(String id, ImageProcessor proc) {
        imageProcessors.put(id, proc);
    }
    
    
    public com.codename1.ui.EncodedImage storeImage(com.codename1.ui.EncodedImage img, String prefix) {
        return storeImage(img, prefix, true);
    }
    public com.codename1.ui.EncodedImage storeImage(com.codename1.ui.EncodedImage img, String prefix, boolean addIndex) {
        int i = 1;
        while(res.containsResource(prefix + "_" + i + ".png")) {
            i++;
        }

        float ratioWidth = 0;
        int multiVal = targetDensity;
        if (!multiImage) {
            multiVal = 0;
        }
        switch(multiVal) {
            // Generate RGB Image
            case 0:
                if (addIndex) {
                    res.setImage(prefix + "_" + i + ".png", img);
                } else {
                    res.setImage(prefix, img);
                }
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

        if (addIndex) {
            res.setMultiImage(prefix + "_" + i + ".png", multi);
        } else {
            //System.out.println("Setting multiimage at "+prefix+" to "+multi);
            res.setMultiImage(prefix, multi);
        }
        return multi.getBest();
    }


    public static byte[] toPngOrJpeg(BufferedImage b) {
        if (hasAlpha(b)) {
            return toPng(b);
        } else {
            byte[] png = toPng(b);
            byte[] jpeg = toJpeg(b);
            if (png.length <= jpeg.length * 2) {
                return png;
            } else {
                return jpeg;
            }
        }
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
    
    public static boolean hasAlpha(BufferedImage b) {
        
        if (!b.getColorModel().hasAlpha()) {
            //System.out.println("The image color model has not alpha");
            return false;
        }
        int[] rgb = b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth());
        for (int px : rgb) {
            int alpha = (px & 0xff000000) >>> 24;
            if (alpha != 0xff) {
                //System.out.println(px + " / " + (alpha));
                //System.out.println("The image has alpha!!");
                return true;
           }
        }
        //System.out.println("The image has not alpha");
        return false;
    }
    
    public static byte[] toJpeg(BufferedImage b) {
        BufferedImage buffer = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] rgb = b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth());
        buffer.setRGB(0, 0, b.getWidth(), b.getHeight(), rgb, 0, b.getWidth());
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(buffer, "jpeg", bo);
            bo.close();
            return bo.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        
    }
    
    public static EditableResources.MultiImage scaleMultiImage(int fromDPI, int toDPI, int scaledWidth, int scaledHeight, EditableResources.MultiImage multi) {
        //try {
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
            byte[] bytes = toPngOrJpeg(scaled);
            //ByteArrayOutputStream output = new ByteArrayOutputStream();
            //ImageIO.write(scaled, "png", output);
            //output.close();
            //byte[] bytes = output.toByteArray();
            com.codename1.ui.EncodedImage encoded = com.codename1.ui.EncodedImage.create(bytes);
            newImage.getInternalImages()[toOffset] = encoded;
            return newImage;
        //} catch (IOException ex) {
        //    ex.printStackTrace();
        //        
        //}
        //return null;
    }
    
    private static BufferedImage getScaledInstance(BufferedImage img,
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
    
    public com.codename1.ui.plaf.Border createRoundBorder(int arcWidth, int arcHeight, int color, boolean outline) {
        com.codename1.ui.plaf.Border b = com.codename1.ui.plaf.Border.createRoundBorder(arcWidth, arcHeight, color, outline);
        return b;
    }
    
    public com.codename1.ui.plaf.Border create9PieceBorder(BufferedImage img, String prefix, int top, int right, int bottom, int left) {
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
        
        com.codename1.ui.EncodedImage topLeftCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(topLeft));
        com.codename1.ui.EncodedImage topRightCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(topRight));
        com.codename1.ui.EncodedImage bottomLeftCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(bottomLeft));
        com.codename1.ui.EncodedImage bottomRightCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(bottomRight));
        com.codename1.ui.EncodedImage centerCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(center));
        com.codename1.ui.EncodedImage topImageCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(topImage));
        com.codename1.ui.EncodedImage bottomImageCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(bottomImage));
        com.codename1.ui.EncodedImage leftImageCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(leftImage));
        com.codename1.ui.EncodedImage rightImageCodenameOne = com.codename1.ui.EncodedImage.create(toPngOrJpeg(rightImage));
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
        return b;
    }
    
    private BrowserComponent web;
    private boolean screenshotsComplete;
    private final Object screenshotsLock = new Object();
   
    
    /**
     * A callback function triggered inside capture.js Javascript file to take a screenshot
     * of a particular rectangular region in the web view.  This would generally be the bounds
     * of an element that we are generating a screenshot for.
     * @param id THe ID of the element;
     * @param x The x coordinate in browser space
     * @param y The y coordinate in browser space
     * @param w The width
     * @param h The height
     */
    public void createScreenshotCallback(String id, int x, int y, int w, int h) {
        //System.out.println("In createScreenshotsCallback("+id+","+x+","+y+","+w+","+h);
        // There are 3 possibilities:
        // 1. There is no registered image processor with ID=id -> call captureScreenshots()
        // 2. There is a registered image processor with ID=id, but image generate fails -> call captureScreenshots()
        // 3. There is a registered image proessor with ID=id.  -> call captureScreenshots()
        //
        // Notice in all cases we just call captureScreenshots() again.  The capture.js page will automatically
        // increment to the next element in the list.  When done, it will fire finishedCaptureScreenshotsCallback().
        CN.callSerially(()->{
            if (imageProcessors.containsKey(id)) {
                double ratio = 1.0;
                try {
                    // Create a Snapshotter to snap a screenshot of the given bounds
                    // in the webview.
                    WebviewSnapshotter snapper = new WebviewSnapshotter(web);
                    snapper.setBounds(x, y, w, h);
                    snapper.snapshot(()-> {
                        BufferedImage img = snapper.getImage();

                        imageProcessors.get(id).process(img);
                        CN.callSerially(()-> {
                            web.execute("window.captureScreenshots()");
                        });

                    });
                } catch (Throwable t) {
                    Log.p("Failed to create snapshot for UIID "+id+": "+t.getMessage());
                    Log.e(t);
                    CN.callSerially(()-> {
                        web.execute("window.captureScreenshots()");
                    });
                }
            } else {
                CN.callSerially(()-> {
                    web.execute("window.captureScreenshots()");
                });
            }
            
        });
    }
    
    public void finishedCaptureScreenshotsCallback() {
        //System.out.println("In finished screen cap");
        screenshotsComplete = true;
        synchronized(screenshotsLock) {
            screenshotsLock.notifyAll();
        }
    }
    
    private SimpleWebServer webServer;
    
    private void startWebServer(File docRoot) throws IOException {
        if (webServer != null) {
            throw new IllegalStateException("Cannot start webserver.  It is already running");
        }
        
        webServer = new SimpleWebServer(0, docRoot);
        webServer.start();
    }
    
    ActionListener loadListener;
    
    /**
     * Creates screenshots for the given HTML which was generated by {@link CSSTheme#generateCaptureHtml() }.
     * @param web The web View used to render the HTML
     * @param html The HTML to render in the webview.  Generated by {@link CSSTheme#generateCaptureHtml() }
     * @param baseURL The BaseURL - general points to the CSS file location.  Used for loading resources from relative paths.
     */
    public void createScreenshots(BrowserComponent web, String html, String baseURL) {
        //System.out.println("in createScreenshots");
        try {
            File baseURLFile = new File(new URL(baseURL).toURI());
            if (!baseURLFile.isDirectory()) {
                baseURLFile = baseURLFile.getParentFile();
            }
            startWebServer(baseURLFile);
            boolean waitForServerResult[] = new boolean[1];
            CN.invokeAndBlock(new Runnable() {
                public void run() {
                    waitForServerResult[0] = webServer.waitForServer(2000);
               
                }
            });
            if (!waitForServerResult[0]) {
                throw new RuntimeException("Failed to start webserver after 2 seconds");
            }
            
        } catch (Exception ex) {
            throw new RuntimeException("Failed to start local webserver for creating screenshots", ex);
        }
        long timeout = 50000;
        //String captureSrc = this.getClass().getResource("capture.js").toExternalForm();
        String captureJS = null;
        try {
            captureJS = Util.readToString(this.getClass().getResourceAsStream("capture.js"));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read capture.js file.", ex);
        }
        //final String modifiedHtml = html.replace("</body>", /*"<script src=\"https://code.jquery.com/jquery-2.1.4.min.js\">"
        //        + */"</script><script src=\""+captureSrc+"\"></script></body>");
        final String modifiedHtml = html.replace("</head>", /*"<script src=\"https://code.jquery.com/jquery-2.1.4.min.js\">"
                + */"<script>\n"+captureJS+"\n</script></head>");
        this.web = web;
        screenshotsComplete = false;
        CN.callSerially(() -> {
            
            loadListener = (ActionEvent evt) -> {
                if (webServer != null) {
                    webServer.stop();
                    webServer = null;
                }
                web.removeWebEventListener(BrowserComponent.onLoad, loadListener);
                try {
                    //System.out.println("In onLoad event");
                    // Use reflection to retrieve the WebEngine's private 'page' field.
                    web.addJSCallback("window.app = window.app || {}; window.app.createScreenshotCallback = function(id, x, y, w, h) {"
                            + "callback.onSuccess(JSON.stringify({id:id, x:x, y:y, w:w, h:h}));"
                            + "};", res -> {
                        try {
                            Result data = Result.fromContent(new StringReader(res.toString()), Result.JSON);

                            createScreenshotCallback(
                                    data.getAsString("id"),
                                    data.getAsInteger("x"),
                                    data.getAsInteger("y"),
                                    data.getAsInteger("w"),
                                    data.getAsInteger("h"));
                        } catch (Exception ex) {
                            Log.p("Failed to parse input to createScreenshotsCallback");
                            Log.e(ex);
                        }

                    });
                    web.addJSCallback("window.app.finishedCaptureScreenshotsCallback = function() {"
                    + "callback.onSuccess(null);"
                    + "};", res -> {
                        finishedCaptureScreenshotsCallback();
                    });
                    web.execute("$(document).ready(function(){ captureScreenshots();});");
                    //web.getEngine().executeScript("window.onload = function(){window.app.ready()};");
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(CN1CSSCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }  catch (SecurityException ex) {
                    Logger.getLogger(CN1CSSCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }

            };
                

            
            web.addWebEventListener(BrowserComponent.onLoad, loadListener);
            //CN.setProperty("cef.setPage.useDataURI", "true");
            webServer.route("/index.html", ()->{
                try {
                    return modifiedHtml.getBytes("UTF-8");
                } catch (Exception ex) {
                    Log.e(ex);
                    try {
                        return ("Error: "+ex.getMessage()).getBytes("UTF-8");
                    } catch (Exception ex2) {
                        Log.e(ex2);
                        return new byte[0];
                    }
                            
                }
            });
            web.setURL("http://localhost:"+webServer.getPort()+"/index.html");
            
        });
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                long startTime = System.currentTimeMillis();
                while (!screenshotsComplete && System.currentTimeMillis() - startTime < timeout) {
                    synchronized(screenshotsLock) {
                        try {
                            screenshotsLock.wait(timeout);
                        } catch (Exception ex){}
                    }
                }
            }
        });
        
        if (!screenshotsComplete) {
            throw new RuntimeException("Failed to create screenshots for HTML "+html+".  Timeout reached.  Likely there was a problem initializing the browser component.");
        }
        this.web = null;
        this.loadListener = null;
        
    }
    
    BufferedImage createHtmlScreenshot(BrowserComponent web, String html) {
        final boolean[] complete = new boolean[1];
        final Object lock = new Object();
        final BufferedImage[] img = new BufferedImage[1];
        
        
        Runnable webpageLoadedCallback = () -> {
            img[0] = (BufferedImage)web.captureScreenshot().get().getImage();
            complete[0] = true;
            synchronized(lock) {
                lock.notify();
            }
            
        };
        CN.callSerially(()->{
            web.setPage(html, "");
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
    
    public static interface ImageProcessor {
        public void process(BufferedImage img);
    }
    
    public void put(String property, Object value) {
        
        res.setThemeProperty(themeName, property, value);
    }
    
    public Object get(String property) {
        return res.getTheme(themeName).get(property);
    }
    
    public void log(String msg) {
        System.out.println(msg);
    }
    
    public void setMultiImage(boolean mi) {
        this.multiImage = mi;
    }
    
    public boolean getMultiImage() {
        return multiImage;
    }
    
}
