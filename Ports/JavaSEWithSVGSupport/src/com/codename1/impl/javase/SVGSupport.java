/*
 * Copyright (c) 2021, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SVGSupport {
    public static Object createSVGImage(String baseURL, byte[] data) throws IOException {
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
    
    static SVG scaleSVG(SVG s, int w, int h) {
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
}
