/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.tools;

import com.codename1.teavm.io.BlobUtil;
import com.codename1.teavm.jso.io.Blob;
import java.io.IOException;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.dom.HTMLCanvasElement;

/**
 *
 * @author shannah
 */
public class ImageTool {
    @JSFunctor
    private static interface ImageOrientationCallback extends JSObject {
        void orientationResult(int orientation);
    } 
    
    @JSBody(params={"file", "callback"}, script="window.cn1GetImageOrientation(file, callback)")
    private native static void getOrientation_(Blob file, ImageOrientationCallback callback);
    
    /**
     * Gets image orientation from EXIF data.
     * @param file 
     * @return 
     */
    public static int getOrientation(Blob file) {
        final boolean[] complete = new boolean[1];
        final int[] orientationOut = new int[1];
        getOrientation_(file, new ImageOrientationCallback() {
            @Override
            public void orientationResult(final int orientation) {
                new Thread(new Runnable() {
                    public void run() {
                        orientationOut[0] = orientation;
                        synchronized(complete) {
                            complete[0] = true;
                            complete.notify();
                        }
                    }
                }).start();
            }
        });
        while (!complete[0]) {
            synchronized(complete) {
                try {
                    complete.wait();
                } catch (Throwable t){}
            }
        }
        return orientationOut[0];
    }
    
    
    
   @JSFunctor
    private static interface ResetImageOrientationCallback extends JSObject {
        void orientationResult(HTMLCanvasElement canvas);
    } 
    
    @JSBody(params={"imageBlob", "srcOrientation", "callback"}, script="window.cn1ResetImageOrientation(imageBlob, srcOrientation, callback)")
    private native static void resetImageOrientation_(Blob imageBlob, int srcOrientation, ResetImageOrientationCallback callback);
    
    @JSBody(params={"blob"}, script="if (blob.type) {return blob.type;} else {return '';}")
    private native static String getBlobType_(Blob blob);
    public static String guessMimetype(Blob img) {
        String type = getBlobType_(img);
        return type;
    }
    
    /**
     * Gets image orientation from EXIF data.
     * @param file 
     * @return 
     */
    public static Blob resetImageOrientation(final Blob imageBlob) {
        int srcOrientation = getOrientation(imageBlob);
        final boolean[] complete = new boolean[1];
        final Blob[] blobOut = new Blob[1];
        resetImageOrientation_(imageBlob, srcOrientation, new ResetImageOrientationCallback() {
            @Override
            public void orientationResult(final HTMLCanvasElement canvas) {
                new Thread(new Runnable() {
                    public void run() {
                        String mimetype = guessMimetype(imageBlob);
                        if (mimetype.isEmpty()) {
                            mimetype = "image/jpeg";
                        }
                        Blob newData;
                        try {
                            newData = BlobUtil.canvasToBlob(canvas, mimetype, 8);
                        } catch (IOException ex) {
                            newData = imageBlob;
                        }
                        blobOut[0] = newData;
                        synchronized(complete) {
                            complete[0] = true;
                            complete.notify();
                        }
                    }
                }).start();
            }
        });
        while (!complete[0]) {
            synchronized(complete) {
                try {
                    complete.wait();
                } catch (Throwable t){}
            }
        }
        return blobOut[0];
    }
    
    
    
}
