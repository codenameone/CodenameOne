/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.ext.usermedia;
import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.teavm.jso.io.Blob;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.core.JSArray;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLButtonElement;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLInputElement;
import static com.codename1.impl.html5.HTML5Implementation.scaleCoord;
import com.codename1.impl.html5.tools.ImageTool;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.FontImage;
/**
 *
 * @author shannah
 */
public class FileChooser {

    
    private HTMLInputElement fileEl;
    private boolean multiple;
    private final ActionListener response;
    private final String accept;
    private final String capture;
    private boolean fixImageOrientation;
    
    public FileChooser(ActionListener response, String accept, String capture) {
        this.response = response;
        this.accept = accept;
        this.capture = capture;
    }
    
    @JSBody(params={}, script="return jQuery('<input type=\"file\"/>').get(0);")
    private static native HTMLInputElement createFileInput_();
    
    @JSBody(params={}, script="return jQuery('<input type=\"file\" multiple/>').get(0);")
    private static native HTMLInputElement createMultiFileInput_();
    
    @JSBody(params={"el"}, script="return el.files;")
    private static native JSArray getFiles(HTMLInputElement el);
    
    
    //@JSBody(params={"f"}, script="return f.name;")
    //private static native String getFileName(JSObject file);
    
   
    private HTMLInputElement createFileInput() {
        fileEl = multiple ? createMultiFileInput_() : createFileInput_();
        if (accept != null) {
            fileEl.setAttribute("accept", accept);
        }
        if (capture != null) {
            fileEl.setAttribute("capture", capture);
        }
        fileEl.addEventListener("change", new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                new Thread() {
                    public void run() {
                        if (clickBtn != null) {
                            clickBtn.getParentNode().removeChild(clickBtn);
                            clickBtn = null;
                        }
                        JSArray files = getFiles(fileEl);
                        fileEl.getParentNode().removeChild(fileEl);
                        if (files.getLength() == 0) {
                            
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    response.actionPerformed(new ActionEvent(null));
                                }
                            });

                        } else {
                            if (multiple) {
                                int len = files.getLength();
                                final String[] paths = new String[len];
                                for (int i=0; i<len; i++) {
                                    Blob blob = (Blob)files.get(i);
                                    if (isFixImageOrientation()) {
                                        blob = ImageTool.resetImageOrientation(blob);
                                    }
                                    paths[i] = HTML5Implementation.createTempFile(blob);
                                }
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        response.actionPerformed(new ActionEvent(paths));
                                    }
                                });
                                    
                            } else {
                                Blob file = (Blob)files.get(0);
                                if (isFixImageOrientation()) {
                                     file = ImageTool.resetImageOrientation(file);
                                }
                                final String path = HTML5Implementation.createTempFile(file);
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        response.actionPerformed(new ActionEvent(path));
                                    }
                                });
                                
                            }
                            /*
                            FileSystemStorage fs = FileSystemStorage.getInstance();
                            String home = fs.getAppHomePath();
                            String uploads = home + fs.getFileSystemSeparator() + "cn1-gallery-uploads";
                            if (!fs.exists(uploads)) {
                                fs.mkdir(uploads);
                            }
                            if (uploads.lastIndexOf(fs.getFileSystemSeparator()) == uploads.length()-1) {
                                uploads = uploads.substring(0, uploads.length()-1);
                            }
                            final String filePath = uploads + fs.getFileSystemSeparator() + getFileName(file);

                            try {
                                OutputStream fos = fs.openOutputStream(filePath);
                                InputStream fis = BlobUtil.openInputStream(file);
                                Util.copy(fis, fos);
                                Util.cleanup(fos);
                                Util.cleanup(fis);
                                fileEl.getParentNode().removeChild(fileEl);
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        response.actionPerformed(new ActionEvent(filePath));
                                    }
                                });
                            } catch (IOException ex) {
                                System.out.println(ex.getMessage());
                                fileEl.getParentNode().removeChild(fileEl);
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        
                                        response.actionPerformed(new ActionEvent(null));
                                    }
                                });
                            }
                            */
                            
                        }
                    }
                }.start();
            }
            
        });
        return fileEl;
        
    }
    
    
    HTMLButtonElement clickBtn;
    
    public void showDialog() {
        Log.p("Showing file dialog");
        createFileInput();
        
        final Window win = Window.current();
        EventListener l = new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                fileEl.click();
                if (clickBtn != null) {
                    clickBtn.getParentNode().removeChild(clickBtn);
                    clickBtn = null;
                }
            }
            
        };
        //((HTMLCanvasElement)win.getDocument().getElementById("codenameone-canvas")).getStyle().setProperty("cursor", "pointer");
        //((HTMLCanvasElement)win.getDocument().getElementById("codenameone-canvas")).addEventListener("click", l);
        HTMLCanvasElement canvas = (HTMLCanvasElement)win.getDocument().getElementById("codenameone-canvas");
        win.getDocument().getBody().appendChild(fileEl);
        String browseText = "Click to browse...";
        Button nativeButton = new Button();
        if (this.capture != null) {
            browseText = "Click to Access Camera";
            nativeButton.setText(browseText);
            nativeButton.setMaterialIcon(FontImage.MATERIAL_CAMERA);
        } else {
            nativeButton.setText(browseText);
            nativeButton.setMaterialIcon(FontImage.MATERIAL_FILE_UPLOAD);
        }
        nativeButton.setWidth(nativeButton.getPreferredW());
        nativeButton.setHeight(nativeButton.getPreferredH());
        
        HTMLButtonElement btn = showButton("", l);
        btn.getStyle().setProperty("position", "absolute");
        btn.getStyle().setProperty("top", "0");
        btn.getStyle().setProperty("left", "0");
        btn.getStyle().setProperty("width", ""+scaleCoord(canvas.getWidth())+"px");
        btn.getStyle().setProperty("height", ""+scaleCoord(canvas.getHeight())+"px");
        btn.getStyle().setProperty("padding", "0");
        btn.getStyle().setProperty("margin", "0");
        //btn.getStyle().setProperty("font-size", "2em");
        btn.getStyle().setProperty("opacity", "0.85");
        HTMLCanvasElement cv = ((HTML5Implementation.NativeImage)nativeButton.toImage().getImage()).getMutableGraphics().getCanvas();
        cv.getStyle().setProperty("width", scaleCoord(nativeButton.getWidth())+"px");
        cv.getStyle().setProperty("height", scaleCoord(nativeButton.getHeight())+"px");
        btn.appendChild(cv);
        
        clickBtn = btn;
        
        // WARNING:  While it is tempting to try to programmatically try to click the button 
        // to initiate the file dialog without user intervention, this is NOT advisable as 
        // some browsers block this file dialog opening and there's no good way to detect
        // when this happens.  So, unfortunately, the best thing to do here is just
        // suck it up and require the user to click twice.
        //btn.click();
        
        
    }
    
    @JSBody(params={"label","l"}, script="return jQuery('<button class=\"btn btn-default\">'+label+'</button>').click(l).appendTo(jQuery('body')).get(0);")
    private native static HTMLButtonElement showButton(String label, EventListener l);

    /**
     * @return the multiple
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * @param multiple the multiple to set
     */
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }
    
    /**
     * @return the fixImageOrientation
     */
    public boolean isFixImageOrientation() {
        return fixImageOrientation;
    }

    /**
     * @param fixImageOrientation the fixImageOrientation to set
     */
    public void setFixImageOrientation(boolean fixImageOrientation) {
        this.fixImageOrientation = fixImageOrientation;
    }
    
}
