/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author shannah
 */
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
