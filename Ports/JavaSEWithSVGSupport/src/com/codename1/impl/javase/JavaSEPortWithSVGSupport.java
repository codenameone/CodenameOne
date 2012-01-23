/*
 * Copyright (c) 2012, Codename One. All rights reserved.
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
package com.codename1.impl.javase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Subclass of the AWT port which adds SVG support to the resource editor
 *
 * @author Shai Almog
 */
public class JavaSEPortWithSVGSupport extends JavaSEPort {
    
    @Override
    public boolean isSVGSupported() {
        return true;
    }
    
    @Override
    public Object createSVGImage(String baseURL, byte[] data) throws IOException {
        try {
            SVG s = new SVG();
            s.setBaseURL(baseURL);
            s.setSvgData(data);
            org.apache.batik.transcoder.image.PNGTranscoder t = new org.apache.batik.transcoder.image.PNGTranscoder();
            org.apache.batik.transcoder.TranscoderInput i = new org.apache.batik.transcoder.TranscoderInput(new ByteArrayInputStream(s.getSvgData()));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            org.apache.batik.transcoder.TranscoderOutput o = new org.apache.batik.transcoder.TranscoderOutput(bo);
            t.transcode(i, o);
            bo.close();
            s.setImg(ImageIO.read(new ByteArrayInputStream(bo.toByteArray())));
            return s;
        } catch (org.apache.batik.transcoder.TranscoderException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
    }

    /**
     * @inheritDoc
     */
    public Object getSVGDocument(Object svgImage) {
        return svgImage;
    }

    private SVG scaleSVG(SVG s, int w, int h) {
        try {
            SVG dest = new SVG();
            dest.setBaseURL(s.getBaseURL());
            dest.setSvgData(s.getSvgData());
            org.apache.batik.transcoder.image.PNGTranscoder t = new org.apache.batik.transcoder.image.PNGTranscoder();
            org.apache.batik.transcoder.TranscoderInput i = new org.apache.batik.transcoder.TranscoderInput(new ByteArrayInputStream(s.getSvgData()));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            org.apache.batik.transcoder.TranscoderOutput o = new org.apache.batik.transcoder.TranscoderOutput(bo);
            t.addTranscodingHint(org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_WIDTH, new Float(w));
            t.addTranscodingHint(org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_HEIGHT, new Float(h));
            t.transcode(i, o);
            bo.close();
            dest.setImg(ImageIO.read(new ByteArrayInputStream(bo.toByteArray())));
            return dest;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        if(nativeImage instanceof SVG) {
            super.getRGB(((SVG) nativeImage).getImg(), arr, offset, x, y, width, height);
            return;
        }
        super.getRGB(nativeImage, arr, offset, x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public int getImageWidth(Object i) {
        if(i instanceof SVG) {
            return ((SVG) i).getImg().getWidth();
        }
        return super.getImageWidth(i);
    }

    /**
     * @inheritDoc
     */
    public int getImageHeight(Object i) {
        if(i instanceof SVG) {
            return ((SVG) i).getImg().getHeight();
        }
        return super.getImageHeight(i);
    }

    /**
     * @inheritDoc
     */
    public Object scale(Object nativeImage, int width, int height) {
        if(nativeImage instanceof SVG) {
            return scaleSVG(((SVG)nativeImage), width, height);
        }
        return super.scale(nativeImage, width, height);
    }
    
    /**
     * @inheritDoc
     */
    public void drawImage(Object graphics, Object img, int x, int y) {
        if(img instanceof SVG) {
            drawSVGImage(graphics, (SVG)img, x, y);
        } else {
            super.drawImage(graphics, img, x, y);
        }
    }

    private void drawSVGImage(Object graphics, SVG img, int x, int y) {
        super.drawImage(graphics,img.getImg(), x, y);
    }

    /**
     * @inheritDoc
     */
    public void exitApplication() {
        java.awt.Component c = getCanvas();
        while(c != null) {
            if(c instanceof java.awt.Frame) {
                java.awt.Frame frm = (java.awt.Frame)c;
                if(frm.getMenuBar() != null) {
                    // this is a simulator window an not the Codename One designer
                    System.exit(0);
                }
                return;
            }
            c = c.getParent();
        }
    }
}
