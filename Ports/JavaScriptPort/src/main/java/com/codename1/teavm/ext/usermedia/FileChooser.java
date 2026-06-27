/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.ext.usermedia;
import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.util.Base64;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import java.io.IOException;
import java.io.OutputStream;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLButtonElement;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLInputElement;
import static com.codename1.impl.html5.HTML5Implementation.scaleCoord;
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
    
    // Number of files the user selected. Overridden in port.js on the worker
    // port to read el.files.length on the MAIN thread (fileEl is a host-ref
    // proxy in the worker, so the inline el.files is empty there).
    @JSBody(params={"el"}, script="return el.files ? el.files.length : 0;")
    private static native int nativeSelectedFileCount(HTMLInputElement el);

    // Returns "name\n<base64-bytes>" for the file at the given index, or null.
    // The worker port overrides this in port.js to read the bytes via a host
    // FileReader. The @JSBody body is the TeaVM main-thread fallback (it returns
    // the name only -- synchronous byte access isn't possible there).
    @JSBody(params={"el", "index"}, script="var f=el.files&&el.files[index]; return f?((f.name||'')+'\\n'):null;")
    private static native String nativeSelectedFile(HTMLInputElement el, int index);

    private static int captureSeq = 0;

    /**
     * Reads the {@code count} selected files (bytes pulled from the main thread)
     * and writes each to FileSystemStorage, returning the resulting paths.
     */
    private String[] readSelectedFiles(int count) {
        if (count <= 0) {
            return null;
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String[] paths = new String[count];
        for (int i = 0; i < count; i++) {
            String packed = nativeSelectedFile(fileEl, i);
            if (packed == null) {
                return null;
            }
            int nl = packed.indexOf('\n');
            String name = nl >= 0 ? packed.substring(0, nl) : "";
            String b64 = nl >= 0 ? packed.substring(nl + 1) : packed;
            if (name.length() == 0) {
                name = "capture" + i;
            }
            String path = fs.getAppHomePath() + "cn1capture-" + (captureSeq++) + "-" + name;
            OutputStream os = null;
            try {
                byte[] bytes = Base64.decode(b64.getBytes());
                os = fs.openOutputStream(path);
                os.write(bytes);
            } catch (IOException ex) {
                Log.e(ex);
                return null;
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ignore) {
                    }
                }
            }
            paths[i] = path;
        }
        return paths;
    }
    
    
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
                // Run synchronously on the current (green) thread via run() rather
                // than start(): a java.lang.Thread never executes on the
                // single-threaded HTML5 worker, so the response listener (and the
                // invokeAndBlock'd Capture.capturePhoto() that waits on it) would
                // otherwise hang forever. The body only does cooperative,
                // host-bridge work, so it's safe inline.
                //
                // The selected <input>.files live on the MAIN thread; on the
                // worker port fileEl is a host-ref proxy with no real .files, so
                // we read the chosen file bytes through the host bridge
                // (nativeSelectedFileCount / nativeSelectedFile) and write them to
                // FileSystemStorage as ordinary files the app can open normally.
                new Thread() {
                    public void run() {
                        if (clickBtn != null) {
                            clickBtn.getParentNode().removeChild(clickBtn);
                            clickBtn = null;
                        }
                        int count = nativeSelectedFileCount(fileEl);
                        final String[] paths = readSelectedFiles(count);
                        if (fileEl.getParentNode() != null) {
                            fileEl.getParentNode().removeChild(fileEl);
                        }
                        if (count <= 0 || paths == null) {
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    response.actionPerformed(new ActionEvent(null));
                                }
                            });
                        } else if (multiple) {
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    response.actionPerformed(new ActionEvent(paths));
                                }
                            });
                        } else {
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    response.actionPerformed(new ActionEvent(paths[0]));
                                }
                            });
                        }
                    }
                }.run();
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
        btn.getStyle().setProperty("width", ""+scaleCoord(HTML5Implementation.displayWidthPx())+"px");
        btn.getStyle().setProperty("height", ""+scaleCoord(HTML5Implementation.displayHeightPx())+"px");
        btn.getStyle().setProperty("padding", "0");
        btn.getStyle().setProperty("margin", "0");
        //btn.getStyle().setProperty("font-size", "2em");
        btn.getStyle().setProperty("opacity", "0.85");
        HTML5Implementation.attachImageToElement(
                (HTML5Implementation.NativeImage)nativeButton.toImage().getImage(), btn,
                scaleCoord(nativeButton.getWidth())+"px", scaleCoord(nativeButton.getHeight())+"px");

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
