/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.ext.usermedia;

import com.codename1.components.SpanLabel;
import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.impl.html5.HTML5Implementation.NativeImage;
import static com.codename1.impl.html5.HTML5Implementation.scaleCoord;

import com.codename1.io.Log;
import com.codename1.teavm.jso.util.EventUtil;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import java.io.IOException;

import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.canvas.CanvasImageSource;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;


import com.codename1.html5.js.JSBody;

import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.browser.TimerHandler;
import com.codename1.html5.js.core.JSFunction;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLDocument;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.html5.js.dom.HTMLInputElement;
import com.codename1.html5.js.dom.HTMLVideoElement;

/**
 *
 * @author shannah
 */
public class PhotoCapture {
    
    private static final String DIALOG_CLASS = "cn1-image-capture-dialog";
            
    
    private int camResolutionWidth=-1;
    private int camResolutionHeight=-1;
    
    private HTMLVideoElement videoEl;
    private Window win;
    
    public PhotoCapture() {

        
    }
    
    @JSBody(params={"err"}, script="return err.name")
    private static native String errorMessage(JSObject err);
    
    private static interface MediaStream extends JSObject {
        //void stop();
    }
    
    private static MediaStream getMediaStream() throws IOException {
        final Object lock = new Object();
        final JSObject[] error = new JSObject[1];
        final MediaStream[] streamOut = new MediaStream[1];
        
        UserMediaCallback callback = new UserMediaCallback() {

            @Override
            public void onSuccess(JSObject stream) {
                streamOut[0] = (MediaStream)stream;
                new Thread() {

                    @Override
                    public void run() {
                        synchronized(lock) {
                            lock.notifyAll();
                        }
                    }
                    
                }.start();
            }
          
        };
        
        ErrorCallback onError = new ErrorCallback() {

            @Override
            public void onError(JSObject err) {
                error[0] = err;
                new Thread() {

                    @Override
                    public void run() {
                        synchronized(lock) {
                            lock.notifyAll();
                        }
                    }
                    
                }.start();
                
            }
            
        };
        
        getUserMedia_(callback, onError);
        
        while (error[0] == null && streamOut[0] == null) {
            try {
                synchronized(lock) {
                    lock.wait(1000);
                }
            } catch (InterruptedException ex) {
                Log.e(ex);
            }
        }
        
        if (error[0] != null) {
            throw new IOException(errorMessage(error[0]));
        }
        return streamOut[0];
    }
    
    @JSBody(params={"stream"}, script="return URL.createObjectURL(stream)")
    private static native String createObjectURL_(MediaStream stream);
    
    private static interface HTMLVideoElementEx extends HTMLVideoElement {
        @JSProperty
        public void setSrcObject(MediaStream stream);
    }
    
    private static HTMLVideoElement getVideoElement(MediaStream stream) {
        final HTMLVideoElement el = (HTMLVideoElement)Window.current().getDocument().createElement("video");
        el.setAutoplay(true);
        el.setMuted(true);
        el.setAttribute("playsinline", "");
        el.setAttribute("controls", "");
        Window.setTimeout(new TimerHandler() {
            @Override
            public void onTimer() {
                el.removeAttribute("controls");
            }
            
        }, 1);
        //el.setSrc(createObjectURL_(stream));
        
        ((HTMLVideoElementEx)el).setSrcObject(stream);
        return el;
    }
    
    private static int[] findVideoSize(final HTMLVideoElement video) throws IOException {
        if (video.getVideoHeight() == 0 || video.getVideoWidth() == 0) {
            final Object lock = new Object();
            
            JSFunction handle = EventUtil.addEventListener(video, "playing", new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    if (video.getVideoHeight() != 0 && video.getVideoWidth() != 0) {
                        new Thread() {

                            @Override
                            public void run() {
                                synchronized (lock) {
                                    lock.notifyAll();
                                }
                            }
                             
                        }.start();
                    }
                }
                
            });
            
            int retryCount = 0;
            
            while (retryCount++ < 10 && video.getVideoHeight() == 0) {
                try {
                    synchronized(lock) {
                        lock.wait(100);
                    }
                    video.pause();
                    video.play();
                } catch (InterruptedException ex) {
                    Log.e(ex);
                }
            }
            
            EventUtil.removeEventListener(video, "playing", handle);
            
            if (video.getVideoHeight() == 0) {
                throw new IOException("Failed to find video size");
            }
            
        }
        return new int[] {video.getVideoWidth(), video.getVideoHeight()};
    }
    
    private static HTMLCanvasElement createSnapshot(HTMLVideoElement video) throws IOException {
        HTMLCanvasElement el = (HTMLCanvasElement)Window.current().getDocument().createElement("canvas");
        findVideoSize(video);
        el.setAttribute("width", String.valueOf(video.getVideoWidth()));
        el.setAttribute("height", String.valueOf(video.getVideoHeight()));
        CanvasRenderingContext2D ctx = (CanvasRenderingContext2D)el.getContext("2d");
        ctx.drawImage((CanvasImageSource)video, 0, 0, video.getVideoWidth(), video.getVideoHeight());
        return el;
    }
    
    public static boolean isSupported() {
        return supportsUserMedia_();
    }
    
    @JSBody(params={}, script="return navigator.getUserMedia !== undefined")
    private native static boolean supportsUserMedia_(); 
    
    @JSBody(params={"stream"}, script="if (stream==null) {return;} else if (stream.stop !== undefined) { stream.stop();} else {stream.getTracks()[0].stop();}")
    private native static void stopStream(MediaStream stream);
    
    private static HTMLCanvasElement setStyleSize(HTMLCanvasElement cv, Component cmp) {
        
            cv.getStyle().setProperty("width", scaleCoord(cmp.getWidth())+"px");
            cv.getStyle().setProperty("height", scaleCoord(cmp.getHeight())+"px");
            return cv;
    }
    
    public HTMLCanvasElement showDialog() {
        if (!supportsUserMedia_()) {
            final Dialog d = new Dialog("Capture Not Supported Yet");
                SpanLabel popupBody = new SpanLabel("Image capture is not yet supported on this device");
                d.setLayout(new BorderLayout());
                d.addComponent(BorderLayout.CENTER, popupBody);
                d.addComponent(BorderLayout.SOUTH, new Button(new Command("OK"){ 

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        d.setVisible(false);
                        d.dispose();
                    }

                }));
                d.show();
            return null;
        }
        try {
            MediaStream stream = getMediaStream();
            final HTMLVideoElement video = getVideoElement(stream);
            video.setAttribute("class", "cn1-image-capture-video");
            HTMLDocument doc = Window.current().getDocument();
            
            final HTMLElement dialog = doc.createElement("div");
            dialog.setAttribute("class", DIALOG_CLASS + " state-stream");
            
            dialog.appendChild(video);
            
            String buttonSize = "5x";
            if (Display.getInstance().getDisplayWidth() / HTML5Implementation.unscaleCoord(1) < 640) {
                buttonSize = "2x";
            }
            
            Button nativeCancelButton = new Button();
            Button nativeUseButton = new Button();
            Button nativeDontUseButton = new Button();
            Button nativeSnapshotButton = new Button();
            $(nativeCancelButton, nativeUseButton, nativeDontUseButton, nativeSnapshotButton).selectAllStyles()
                    .setFontSizeMillimeters(12f);
            
            nativeCancelButton.setMaterialIcon(FontImage.MATERIAL_CANCEL);
            nativeCancelButton.setWidth(nativeCancelButton.getPreferredW());
            nativeCancelButton.setHeight(nativeCancelButton.getPreferredH());
            
            HTMLInputElement cancelButton = createButton("cancel-btn", "");
            cancelButton.appendChild(setStyleSize(((NativeImage)nativeCancelButton.toImage().getImage()).getMutableGraphics().getCanvas(), nativeCancelButton));
           
            
            nativeUseButton.setMaterialIcon(FontImage.MATERIAL_CHECK_CIRCLE);
            nativeUseButton.setWidth(nativeUseButton.getPreferredW());
            nativeUseButton.setHeight(nativeUseButton.getPreferredH());
            
            HTMLInputElement useButton = createButton("use-btn", "");
            useButton.appendChild(setStyleSize(((NativeImage)nativeUseButton.toImage().getImage()).getMutableGraphics().getCanvas(), nativeUseButton));
            
            
            nativeDontUseButton.setMaterialIcon(FontImage.MATERIAL_DELETE);
            nativeDontUseButton.setWidth(nativeDontUseButton.getPreferredW());
            nativeDontUseButton.setHeight(nativeDontUseButton.getPreferredH());
            HTMLInputElement dontUseButton = createButton("dontuse-btn", "");
            dontUseButton.appendChild(setStyleSize(((NativeImage)nativeDontUseButton.toImage().getImage()).getMutableGraphics().getCanvas(), nativeDontUseButton));
            
            
            nativeSnapshotButton.setMaterialIcon(FontImage.MATERIAL_CAMERA);
            nativeSnapshotButton.setWidth(nativeSnapshotButton.getPreferredW());
            nativeSnapshotButton.setHeight(nativeSnapshotButton.getPreferredH());
            HTMLInputElement snapshotButton = createButton("capture-btn", "");
            snapshotButton.appendChild(setStyleSize(((HTML5Implementation.NativeImage)nativeSnapshotButton.toImage().getImage()).getMutableGraphics().getCanvas(), nativeSnapshotButton));
            
            
            HTMLElement buttons = doc.createElement("div");
            buttons.setAttribute("class", "buttons");
            
            buttons.appendChild(cancelButton);
            buttons.appendChild(snapshotButton);
            buttons.appendChild(useButton);
            buttons.appendChild(dontUseButton);
            dialog.appendChild(buttons);
            
            final Object lock = new Object();
            final IOException[] error = new IOException[1];
            final HTMLCanvasElement[] selectedSnapshot = new HTMLCanvasElement[1];
            final HTMLCanvasElement[] currentSnapshot = new HTMLCanvasElement[1];
            final boolean[] cancelled = new boolean[1];
            
            snapshotButton.addEventListener("click", new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    new Thread() {

                        @Override
                        public void run() {
                            try {
                                HTMLCanvasElement canvas = createSnapshot(video);
                                dialog.insertBefore(canvas, video);
                                dialog.setAttribute("class",DIALOG_CLASS + " state-accept-snapshot");
                                currentSnapshot[0] = canvas;
                                
                            } catch (IOException ex) {
                                error[0] = ex;
                                synchronized(lock) {
                                    lock.notifyAll();
                                }
                            }
                        }
                        
                    }.start();  
                }
                
            }, false);
            
            
            cancelButton.addEventListener("click", new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    new Thread() {

                        @Override
                        public void run() {
                            cancelled[0] = true;
                            synchronized(lock) {
                                lock.notifyAll();
                            }
                        }
                        
                    }.start();
                }

               
            }, false);
            
            
            useButton.addEventListener("click", new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    new Thread() {

                        @Override
                        public void run() {
                            selectedSnapshot[0] = currentSnapshot[0];
                            synchronized(lock) {
                                lock.notifyAll();
                            }
                        }
                        
                    }.start();
                }
                
            }, false);
            
            dontUseButton.addEventListener("click", new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    new Thread() {

                        @Override
                        public void run() {
                            currentSnapshot[0].getParentNode().removeChild(currentSnapshot[0]);
                            currentSnapshot[0] = null;
                            dialog.setAttribute("class", DIALOG_CLASS + " state-stream");
                        }
                        
                    }.start();
                }
                
            });
            
            Log.p("Appending dialog");
            doc.getBody().appendChild(dialog);
            
            while (!cancelled[0] && error[0] == null && selectedSnapshot[0] == null) {
                try {
                    synchronized(lock) {
                        lock.wait(1000);
                    }
                } catch (InterruptedException ex) {
                    Log.e(ex);
                }
            }
            
            doc.getBody().removeChild(dialog);
            stopStream(stream);
            
            if (selectedSnapshot[0] != null) {
                return selectedSnapshot[0];
            } else if (cancelled[0]) {
                return null;
            } else if (error[0] != null) {
                Log.e(error[0]);
                return null;
            } else {
                throw new RuntimeException("Undefined behaviour in capturing photo");
            }
            
        } catch (IOException ex) {
            Log.e(ex);
        }
        return null;
    }
    
    
    
    @JSBody(params={"onSuccess", "onError"}, script="navigator.getUserMedia({video:true, audio:false}, onSuccess, onError)")
    private native static void getUserMedia_(UserMediaCallback onSuccess, ErrorCallback onError);
          
    @JSFunctor        
    private static interface UserMediaCallback extends JSObject{
        void onSuccess(JSObject stream);
    }
    
    @JSFunctor
    private static interface ErrorCallback extends JSObject {
        void onError(JSObject error);
    }
    
    
    private HTMLInputElement createButton(String cssClass, String label) {
        return HTML5Implementation.createButton(cssClass, label);
    }
    
}
