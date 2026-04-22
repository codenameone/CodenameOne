/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;
import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.compat.java.util.Objects;
import com.codename1.components.InteractionDialog;
import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.db.Database;
import com.codename1.teavm.io.ArrayBufferInputStream;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.CodenameOneThread;
import com.codename1.impl.html5.JSOImplementations.CN1Native;
import com.codename1.impl.html5.JSOImplementations.HTMLIFrameElement;
import com.codename1.impl.html5.JSOImplementations.HTMLMediaElement;
import com.codename1.impl.html5.JSOImplementations.ImageExt;
import com.codename1.impl.html5.JSOImplementations.KeyEvent;
import com.codename1.impl.html5.JSOImplementations.Navigator;
import com.codename1.impl.html5.JSOImplementations.TextElement;
import com.codename1.impl.html5.JSOImplementations.WheelEvent;
import com.codename1.impl.html5.JSOImplementations.WindowExt;
import com.codename1.impl.html5.JSOImplementations.WindowLocation;
import com.codename1.impl.html5.components.ContextMenu;
import com.codename1.impl.html5.database.DatabaseImpl;
import com.codename1.impl.html5.graphics.ClipRect;
import com.codename1.impl.html5.graphics.ExecutableOp;
import com.codename1.impl.html5.videojs.JSVideoCaptureConstraintsCompiler;
import com.codename1.impl.html5.videojs.VideoJS;


import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.messaging.Message;
import com.codename1.push.PushCallback;
import com.codename1.teavm.ext.localforage.LocalForage;
import com.codename1.teavm.ext.localforage.LocalForage.ItemSavedListener;
import com.codename1.teavm.ext.usermedia.PhotoCapture;
import com.codename1.teavm.ext.websql.WebSQL;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.teavm.io.BlobUtil;
import com.codename1.teavm.jso.io.Blob;
import com.codename1.teavm.jso.io.FileList;
import com.codename1.teavm.jso.util.EventUtil;
import com.codename1.teavm.jso.util.JSDateFormat;
import com.codename1.teavm.jso.util.JSNumberFormat;
import com.codename1.ui.Accessor;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.BrowserWindow;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import static com.codename1.ui.CN.invokeAndBlock;
import com.codename1.ui.Component;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.Sheet;
import com.codename1.ui.Stroke;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.TextSelection;
import com.codename1.ui.Transform;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.MessageEvent;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UITimer;
import com.codename1.util.AsyncResource;
import com.codename1.util.EasyThread;
import com.codename1.util.FailureCallback;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Date;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import com.codename1.html5.interop.SuppressSyncErrors;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.ajax.ReadyStateChangeHandler;
import com.codename1.html5.js.ajax.XMLHttpRequest;
import com.codename1.html5.js.browser.TimerHandler;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.canvas.CanvasPattern;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.canvas.ImageData;
import com.codename1.html5.js.core.JSArray;
import com.codename1.html5.js.core.JSFunction;
import com.codename1.html5.js.core.JSNumber;
import com.codename1.html5.js.core.JSString;
import com.codename1.html5.js.dom.CSSStyleDeclaration;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.MouseEvent;
import com.codename1.html5.js.dom.HTMLButtonElement;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLDocument;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.html5.js.dom.HTMLImageElement;
import com.codename1.html5.js.dom.HTMLInputElement;
import com.codename1.html5.js.dom.HTMLTextAreaElement;
import com.codename1.html5.js.typedarrays.ArrayBuffer;
import com.codename1.html5.js.typedarrays.Float64Array;
import com.codename1.html5.js.typedarrays.Uint8Array;
import com.codename1.html5.js.typedarrays.Uint8ClampedArray;

/**
 *
 * @author shannah
 */
public class HTML5Implementation extends CodenameOneImplementation {
    private L10NManager l10n;
    private int density;
    private static final String STORAGE_KEY_PREFIX = JavaScriptRuntimeFacade.STORAGE_KEY_PREFIX;
    private static final String FILE_SYSTEM_PREFIX = JavaScriptRuntimeFacade.FILE_SYSTEM_PREFIX;
    private static HTML5Implementation instance;
    private boolean shiftKeyDown;
    private BufferedGraphics graphics;
    Window window;
    private HTMLCanvasElement canvas;
    private HTMLCanvasElement scratchBuffer;
    HTMLCanvasElement outputCanvas;
    private final JavaScriptRenderingBackend renderingBackend = new BrowserDomRenderingBackend();
    private EventListener onMouseDown, onMouseUp, onTouchStart, onTouchEnd, onMouseMove, onTouchMove, hitTest, onPaste;
    
    // This event listener can be assigned to listen to native mouse events
    // and handle them directly.
    private EventListener nativeEventListener;
    
    private JSFunction onMouseMoveHandle, onTouchMoveHandle, onPointerMoveHandle;
    private NativeFont defaultFont;
    private String pendingTextChanges;
    private TextArea currentEditingField;
    private HTMLInputElement currentInputField;
    private boolean editingStartingUp;
    private static double devicePixelRatio=-1;
    private EasyThread nativeEdt;
    
    // Used for key press/release.  We record the last char code
    // in keypressed because the charcode isn't passed to keydown and keyup
    // listeners -- only keypressed.
    private int lastCharCode;
    
    private List<ActionListener> mouseUpListeners = new ArrayList<ActionListener>();
    
    private int defaultFileSystemSize=104857600;  // 100 Megs

    final Object editingLock=new Object();

    private Form _getCurrent() {
        return getCurrentForm();
    }
        
    
    private JavaScriptAnimationFrameCallback animationFrameHandler;

    private JavaScriptRenderQueueState<ExecutableOp> pendingDisplay=new JavaScriptRenderQueueState<ExecutableOp>();
    
    
    
    
    /**
     * Used to transform URLs that are to be fetched using a network connection
     * to use a proxy. 
     */
    private URLProxifier urlProxifier;

    
    private String photosPath="/photos";

    private class BrowserDomRenderingBackend implements JavaScriptRenderingBackend {
        @Override
        public HTMLCanvasElement createCanvas(int width, int height) {
            HTMLCanvasElement canvas = (HTMLCanvasElement)window.getDocument().createElement("canvas");
            canvas.setWidth(width);
            canvas.setHeight(height);
            return canvas;
        }

        @Override
        public HTMLImageElement createImageElement() {
            return (HTMLImageElement)window.getDocument().createElement("img");
        }

        @Override
        public HTMLImageElement createCrossOriginImageElement(String sourceUrl) {
            HTMLImageElement image = createImageElement();
            image.setAttribute("crossorigin", "anonymous");
            image.setSrc(sourceUrl);
            return image;
        }

        @Override
        public HTMLImageElement createBlobImageElement(Blob blob) {
            return createCrossOriginImageElement(BlobUtil.createObjectURL(blob));
        }

        @Override
        public HTML5Graphics createGraphics(HTML5Implementation implementation, HTMLCanvasElement canvas) {
            return new HTML5Graphics(implementation, canvas);
        }

        @Override
        public CanvasRenderingContext2D getContext(HTMLCanvasElement canvas) {
            return (CanvasRenderingContext2D)canvas.getContext("2d");
        }

        @Override
        public void drawLoadedImage(CanvasRenderingContext2D context, HTMLImageElement image, int x, int y, int width, int height) {
            context.drawImage(image, x, y, width, height);
        }

        @Override
        public void drawMutableSurface(CanvasRenderingContext2D context, HTMLCanvasElement canvas, int x, int y, int width, int height) {
            context.drawImage(canvas, x, y, width, height);
        }

        @Override
        public CanvasPattern createLoadedImagePattern(CanvasRenderingContext2D context, HTMLImageElement image) {
            return context.createPattern(image, "repeat");
        }

        @Override
        public CanvasPattern createMutableSurfacePattern(CanvasRenderingContext2D context, HTMLCanvasElement canvas) {
            return context.createPattern(canvas, "repeat");
        }

        @Override
        public ImageData readLoadedImageData(HTMLImageElement image, int x, int y, int width, int height) {
            HTMLCanvasElement canvas = createCanvas(width, height);
            CanvasRenderingContext2D context = getContext(canvas);
            context.drawImage(image, x, y, width, height, 0, 0, width, height);
            return context.getImageData(0, 0, width, height);
        }

        @Override
        public ImageData readMutableSurfaceData(HTMLCanvasElement canvas, int x, int y, int width, int height) {
            return getContext(canvas).getImageData(x, y, width, height);
        }

        @Override
        public void writeImageData(HTMLCanvasElement canvas, ImageData imageData, int width, int height) {
            CanvasRenderingContext2D context = getContext(canvas);
            context.clearRect(0, 0, width, height);
            context.putImageData(imageData, 0, 0, 0, 0, width, height);
        }

        @Override
        public void scaleLoadedImageToCanvas(HTMLCanvasElement canvas, HTMLImageElement image, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
            getContext(canvas).drawImage(image, 0, 0, sourceWidth, sourceHeight, 0, 0, targetWidth, targetHeight);
        }

        @Override
        public void scaleMutableSurfaceToCanvas(HTMLCanvasElement canvas, HTMLCanvasElement sourceCanvas, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
            getContext(canvas).drawImage(sourceCanvas, 0, 0, sourceWidth, sourceHeight, 0, 0, targetWidth, targetHeight);
        }

        @Override
        public Blob toImageBlob(HTMLCanvasElement canvas, String mimeType, float quality) throws IOException {
            return BlobUtil.canvasToBlob(canvas, mimeType, quality);
        }

        @Override
        public void repaintCurrentForm() {
            Form current = Display.getInstance().getCurrent();
            if (current != null) {
                current.repaint();
            }
        }
    }
    
    /**
     * We don't have an API yet to auto-detect the device's camera dimensions
     * so we'll specify some hard defaults here and possibly abstract it further later.
     * This article is useful on this topic:
     * https://webrtchacks.com/how-to-figure-out-webrtc-camera-resolutions/
     */
    private int cameraWidth=960;
    
    private int cameraHeight=720;

    @Override
    public boolean isShiftKeyDown() {
        return shiftKeyDown;
    }

    MouseEvent lastMouseEvent;
    
    @Override
    public boolean isRightMouseButtonDown() {
        if (lastMouseEvent !=null) {
            return lastMouseEvent.getButton() == 2;
        }
        return false;
    }
    
    
    
    /**
     * @return the urlProxifier
     */
    public URLProxifier getUrlProxifier() {
        return urlProxifier;
    }

    /**
     * @param urlProxifier the urlProxifier to set
     */
    public void setUrlProxifier(URLProxifier urlProxifier) {
        this.urlProxifier = urlProxifier;
    }
    
    private final JavaScriptPointerSessionState pointerState = new JavaScriptPointerSessionState();
    
    public int getLastTouchUpX() {
        return pointerState.getLastTouchUpX();
    }
    
    public int getLastTouchUpY() {
        return pointerState.getLastTouchUpY();
    }
    
    private HashSet<Integer> keysDown = new HashSet<Integer>();
    
    @JSBody(params={}, script="window.onbeforeunload=function(){return 'Leaving or refreshing the page may cause you to lose unsaved data.';}")
    private native static void installBeforeUnload();
    
    @JSBody(params={}, script="return window.onbeforeunload")
    private native static JSObject getBeforeUnloadHandler();
    
    @JSBody(params={"handler"}, script="window.onbeforeunload=handler")
    private native static void setBeforeUnloadHandler(JSObject handler);
    
    private int getClientX(MouseEvent evt) {
        int x = evt.getClientX();
        if (x == -1) {
            return x;
        }
        
        return (int)(x * getDevicePixelRatio());
    }
    
    private int getClientY(MouseEvent evt) {
        int y = evt.getClientY();
        if (y == -1) {
            return y;
        }
        return (int)((y + getScrollY_()) * getDevicePixelRatio());
    }
    
    private boolean hitTest(int x, int y) {
         if (outputCanvas != null) {
            CanvasRenderingContext2D ctx = (CanvasRenderingContext2D)outputCanvas.getContext("2d");
            if (ctx != null) {
                try {
                    ImageData p = ctx.getImageData(x, y, 1, 1);
                    int pixelLen = p.getData().getLength();
                    if (pixelLen == 4) {
                        if (p.getData().get(3) == 0) {
                            return false;
                        }
                    }
                } catch (Exception ex){}
            }
        }
                
        return true;       
                
    }
    
    
    @JSBody(params={"event"}, script="cn1CopyEventToNativePeers(event);")
    private native static void copyEventsToNativePeers(Event event);
    
    @JSBody(params={"str"}, script="if (window.cn1Debug) console.log(str);")
    native static void _debug(String str);
    
    @JSBody(params={"obj"}, script="if (window.cn1Debug) console.log(obj);")
    native static void _debugObj(JSObject obj);
    @JSBody(params={"obj"}, script="console.log(obj);")
    public native static void _logObj(JSObject obj);
    
    /**
     * Container for all peer components.
     */
    HTMLElement peersContainer;
    
    
    /**
     * Meant to be like the Runnable interface, but as a pure JS
     * function
     */
    @JSFunctor
    public static interface JSRunnable extends JSObject {
        public void run();
    }
    
    /**
     * Callbacks that are run 300ms after a pointer press event from inside the pointer 
     * handler to perform things that require user interaction. These are necessary for things
     * like movies that *cannot* be played programmatically because they require user interaction
     * on platforms like Android and iOS.  
     * 
     */
    private JSArray backSideHooks = JSArray.create();
    public void addBacksideHook(JSRunnable r) {
        backSideHooks.push(r);
    }
    
    // Count the number of backside hook calls that are queued up
    private int backsideHooksSemaphore = 0;
    
    /**
     * Checks to see if there is a pending callback for a backside hook.
     * I.e. If you add a backside hook to be executed RIGHT now, will be be executed
     * in this batch, or will it need to wait for another user interaction.
     * @return 
     */
    public boolean isBacksideHookAvailable() {
        return backsideHooksIntervalHandle != 0 || backsideHooksSemaphore > 0;
    }
    
    /**
     * Backside hooks are just a tricky way to execute actions in response to user actions.
     * Some things, like playing media, can only happen in response to a user interaction
     * on mobile devices.  Unfortunately, CN1 uses its own event thread so none of the events
     * technically happen in response to a user event - so far as the browser is concerned.
     * So we add some hooks that are run in a setTimeout() inside the actual native event handlers
     * where we can queue actions that be be performed.
     * @param timeout 
     */
    private void runBacksideHooksInTimeout(int timeout) {
        backsideHooksSemaphore++;
        //_log("Incrementing backsideHooksSemaphore: "+backsideHooksSemaphore);
        Window.setTimeout(new TimerHandler() {
            @Override
            public void onTimer() {
                backsideHooksSemaphore--;
                //_log("Decrementing backsideHooksSemaphore: "+backsideHooksSemaphore);
                runBacksideHooks();
            }
        }, timeout);
    }
    
    @JSBody(params={}, script="while (window.cn1NativeBacksideHooks.length > 0) {"
            + "  var f = window.cn1NativeBacksideHooks.shift();"
            + "  try {f();} catch (e){console.log(e);}"
            + "}")
    private native static void runPendingNativeBacksideHooks();
    
    private int backsideHooksIntervalTimeout;
    private int backsideHooksIntervalHandle;
    
    private void startBacksideHooksInterval() {
        if (backsideHooksIntervalTimeout > 0) {
            startBacksideHooksInterval(backsideHooksIntervalTimeout);
        }
    }
    
    private void startBacksideHooksInterval(int interval) {
        if (backsideHooksIntervalHandle != 0) {
            Window.clearInterval(backsideHooksIntervalHandle);
            backsideHooksIntervalHandle = 0;
        }
        backsideHooksIntervalHandle  = Window.setInterval(new TimerHandler() {
            @Override
            public void onTimer() {
                runBacksideHooks();
            }
            
        }, interval);
    }
    
    private void stopBacksideHooksInterval() {
        if (backsideHooksIntervalHandle != 0) {
            Window.clearInterval(backsideHooksIntervalHandle);
            backsideHooksIntervalHandle = 0;
        }
    }
    
    /**
     * Runs all of the pending backside hooks.
     */
    public void runBacksideHooks() {
        runPendingNativeBacksideHooks();
        while (backSideHooks.getLength() > 0) {
            JSRunnable r = (JSRunnable)backSideHooks.shift();
            r.run();
        }
    }
    
    private static int safariBacksideHookDelay;
    
    private static int safariBacksideHookDelay() {
        if (safariBacksideHookDelay == 0) {
            safariBacksideHookDelay = _safariBacksideHookDelay();
            if (safariBacksideHookDelay == 0) {
                
                // Based on my experiments, iOS 13 is far more forgiving for the backside hook
                // delay time.  So we set a default of 300, which seems to work.
                // iOS 12 - not so forgiving.  We set at 75.
                if (!isIOS() || isIOS13()) {
                    // Desktop safari, and iOS devices on 13+ we give a 300 delay.
                    safariBacksideHookDelay = 300;
                } else {
                    safariBacksideHookDelay = 75;
                }
            }
        }
        return safariBacksideHookDelay;
    }
    
    @JSBody(params={}, script="var delay=window.getParameterByName('cn1SafariBacksideHookDelay'); if (delay) return parseInt(delay); return 0;")
    private native static int _safariBacksideHookDelay();
    
    private void installBacksideHooksInUserInteraction() {
        if (isIOS() || isSafari()) {
            debugLog("Installing backside hooks with delay "+safariBacksideHookDelay());
            runBacksideHooksInTimeout(safariBacksideHookDelay());
        } else {
            startBacksideHooksInterval();
            runBacksideHooksInTimeout(300);
            runBacksideHooksInTimeout(1500);
            runBacksideHooksInTimeout(5000);
        }
    }
    
    /**
     * Flag that is set on mousedown or touchstart in the cn1 canvas so that 
     * it knows to not pass events to rest of native peers until after mouseup/touchend
     */
    

    
    private class NativeOverlay {
        HTMLInputElement el;
        Component cmp;
        NativeOverlay(Component cmp) {
            this.cmp = cmp;
        }
        
        void uninstall() {
            if (el != null) {
                window.getDocument().getBody().removeChild((HTMLInputElement)el);
            }
        }
        
        void update() {
            
        }
        
        void updateIfMovedAndFocused() {
            
        }
        
        void updateNativeEditorText(String text) {
            
        }
    }

    @Override
    public void updateNativeEditorText(Component c, String text) {
        if (c.getNativeOverlay() != null) {
            NativeOverlay o = (NativeOverlay)c.getNativeOverlay();
            o.updateNativeEditorText(text);
        }
    }
    
    
    
    private class TextAreaNativeOverlay extends NativeOverlay {
        TextArea ta;
        FocusListener focusListener;
        DataChangedListener dataChangedListener;
        boolean donePressed;
        Thread monitorThread;

        @Override
        void uninstall() {
            super.uninstall();
            if (focusListener != null) {
                ta.removeFocusListener(focusListener);
                focusListener = null;
            }
            if (dataChangedListener != null && ta instanceof TextField) {
                ((TextField)ta).removeDataChangedListener(dataChangedListener);
                dataChangedListener = null;
            }
        }
        
        private void startMonitorThread() {
            if (monitorThread == null) {
                monitorThread = new Thread(new Runnable() {
                    public void run() {
                        while (jQuery_is_(inputEl, ":focus")) {
                            callSerially(new Runnable() {

                                @Override
                                public void run() {
                                    
                                }
                                
                            });
                        }
                    }
                });
            }
        }
        
        
        
        
        TextAreaNativeOverlay(TextArea taIn) {
            super(taIn);
            this.ta = taIn;
            final HTMLInputElement inputEl;
            if (!ta.isSingleLineTextArea()){
                inputEl = (HTMLInputElement)window.getDocument().createElement("textarea");
                isEditingSingleLine = true;

            } else {
                inputEl = (HTMLInputElement)window.getDocument().createElement("input");
                inputEl.setType("text");
                isEditingSingleLine = false;

            }
            
            el = inputEl;
                
            inputEl.setAttribute("class", "cn1-edit-string");
            inputEl.getStyle().setProperty("outline", "none");  // for chrome
            
            String inputType = "text";
            if (ta.isSingleLineTextArea()) {
                
                switch (ta.getConstraint()) {
                    case TextArea.PASSWORD:
                        inputType = "password";
                        break;
                    case TextArea.EMAILADDR:
                        inputType = "email";
                        break;
                    case TextArea.NUMERIC:
                        inputType = "number";
                        break;
                    case TextArea.PHONENUMBER:
                        inputType = "tel";
                        break;
                    case TextArea.URL:
                        inputType = "url";
                        break;
                    
                }
                inputEl.setAttribute("type", inputType);
                
                
            }
            
            
            
            inputEl.addEventListener("keydown", new EventListener() {

                @Override
                public void handleEvent(final Event evt) {
                    KeyEvent kevt = (KeyEvent)evt;
                    switch (kevt.getKeyCode()) {
                        case 9 : // tab
                        case 11 : // vertical tab
                        case 10 : // lf 
                        case 13 : // cr
                            if (ta.isSingleLineTextArea() || kevt.getKeyCode() == 9 || kevt.getKeyCode() == 11) {
                                evt.preventDefault();
                                evt.stopPropagation();
                            }
                            break;
                        default:
                            
                    }
                    callSerially(new Runnable() {
                        public void run() {
                            final KeyEvent kevt = (KeyEvent)evt;
                            switch (kevt.getKeyCode()) {
                                case 9 : // tab
                                case 11 : // vertical tab
                                case 10 : // lf
                                case 13 : // cr
                                {
                                    if (!ta.isSingleLineTextArea() && kevt.getKeyCode() != 9 && kevt.getKeyCode() != 11) {
                                        // We don't do any special handling for multiline text fields.
                                            return;
                                        }
                                    donePressed = true;
                                    inputEl.blur();
                                    break;
                                }
                            }
                           
                        }
                    });
                }

            });
            

            focusListener = new FocusListener() {

                @Override
                public void focusGained(Component cmpnt) {
                    
                    
                    if (!jQuery_is_(inputEl, ":focus")) {
                        inputEl.focus();
                    }
                    Font f = ta.getSelectedStyle().getFont();
                    if (f != null) {
                        NativeFont nf = (NativeFont)f.getNativeFont();
                        inputEl.getStyle().setProperty("font",nf.getScaledCSS());
                    }
                    inputEl.setValue(ta.getText());
                    
                }

                @Override
                public void focusLost(Component cmpnt) {
                    if (jQuery_is_(inputEl, ":focus")) {
                        inputEl.blur();
                    }
                }
                
            };
            
            ta.addFocusListener(focusListener);
            
            inputEl.addEventListener("input", new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    callSerially(new Runnable() {

                        @Override
                        public void run() {
                            String old = ta.getText();
                            String value = inputEl.getValue();
                            if (old == null && value != null || old != null && !old.equals(value)) {
                                ta.setText(value);
                                ta.repaint();
                            }
                        }
                        
                    });
                }
             
            }, true);
            
            inputEl.addEventListener("focus", new EventListener() {
                public void handleEvent(Event evt) {
                    callSerially(new Runnable() {
                        public void run() {
                            donePressed = false;
                            isEditing = true;
                            inputEl.getStyle().setProperty("color", HTML5Graphics.color(ta.getStyle().getFgColor()));
                    
                            Font f = ta.getSelectedStyle().getFont();
                            if (f != null) {
                                NativeFont nf = (NativeFont)f.getNativeFont();
                                inputEl.getStyle().setProperty("font",nf.getScaledCSS());
                            }
                            inputEl.setValue(ta.getText());
                            if (!ta.hasFocus()) {
                                ta.requestFocus();
                            }
                            if (!ta.isEditing()) {
                                ta.startEditingAsync();
                            }
                            ta.repaint();
                            
                            UITimer.timer(500, false, new Runnable() {

                                @Override
                                public void run() {
                                    int vkbHeight = getScrollY_();
                                    Form current = _getCurrent();
                                    if (current != null &&current.isFormBottomPaddingEditingMode()) {


                                        current.getContentPane().getUnselectedStyle().setPaddingUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});
                                        current.getContentPane().getUnselectedStyle().setPadding(Component.BOTTOM, unscaleCoord(vkbHeight));
                                        Window.current().scrollTo(0, 0);

                                        current.forceRevalidate();
                                    }
                                }
                                
                            });
                            
                            
                        }
                    });
                }
            }, true);
            
            inputEl.addEventListener("blur", new EventListener() {
                public void handleEvent(Event evt) {
                    callSerially(new Runnable() {
                        public void run() {
                            isEditing = false;
                            inputEl.getStyle().setProperty("color", "transparent");
                            ta.setText(inputEl.getValue());
                            ta.repaint();
                            Display.getInstance().onEditingComplete(ta, ta.getText());
                            if (donePressed && ta instanceof TextArea) {
                                ((TextArea)ta).fireDoneEvent();
                            }
                            donePressed = false;
                            
                            UITimer.timer(500, false, new Runnable() {
                                public void run() {
                                    Form current = Display.getInstance().getCurrent();
                                    if (current != null && current.isFormBottomPaddingEditingMode()) {
                                        current.getContentPane().getUnselectedStyle().setPaddingUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});
                                        current.getContentPane().getUnselectedStyle().setPadding(Component.BOTTOM, 0);
                                        current.forceRevalidate();
                                    }
                                }
                            });
                            
                        }
                    });
                }
            }, true);
            
            

            window.getDocument().getBody().appendChild(inputEl);
        }

        int lastX;
        int lastY;
        int lastW;
        int lastH;
        
        @Override
        void updateIfMovedAndFocused() {
            //if (ta.hasFocus()) {
                int newX = ta.getAbsoluteX();
                int newY = ta.getAbsoluteY();
                int newW = ta.getWidth();
                int newH = ta.getHeight();
                if (lastX != newX || lastY != newY || lastW != newW || lastH != newH) {
                    lastX = newX;
                    lastY = newY;
                    lastW = newW;
                    lastH = newH;
                    update();
                }
            //}
        }
        
        @Override
        void update() {
            super.update(); 
            final HTMLInputElement inputEl = (HTMLInputElement)el;
            CSSStyleDeclaration s = inputEl.getStyle();
            if (ta.isEditable() && ta.isVisible() && ta.getComponentForm().getComponentAt(ta.getAbsoluteX() + ta.getWidth()/2, ta.getAbsoluteY() + ta.getHeight()/2) == ta) {
                // We only want to respond to pointer events if the text field is editable, visible, and is not covered by another component.
                String propVal = s.getPropertyValue("pointer-events");
                if (!"auto".equals(propVal)) {
                    s.setProperty("pointer-events", "auto");
                }
                inputEl.removeAttribute("disabled");
            } else {
                String propVal = s.getPropertyValue("pointer-events");
                if (!"none".equals(propVal)) {
                    s.setProperty("pointer-events", "none");
                }
                inputEl.setAttribute("disabled", "true");
            }
            Style taStyle = cmp.getSelectedStyle();
            int paddingTop = taStyle.getPadding(Component.TOP);
            int paddingLeft = taStyle.getPadding(ta.isRTL(), Component.LEFT);
            int paddingRight = taStyle.getPadding(ta.isRTL(), Component.RIGHT);
            int paddingBottom = taStyle.getPadding(Component.BOTTOM);
            
            s.setProperty("padding-top", scaleCoord((double)paddingTop)+"px");
            s.setProperty("padding-left", scaleCoord((double)paddingLeft)+"px");
            s.setProperty("padding-bottom", scaleCoord((double)paddingBottom)+"px");
            s.setProperty("padding-right", scaleCoord((double)paddingRight)+"px");
            
            s.setProperty("display", "block");
            s.setProperty("top", scaleCoord((double)cmp.getAbsoluteY())+"px");
            s.setProperty("left", scaleCoord((double)cmp.getAbsoluteX())+"px");
            s.setProperty("width", scaleCoord((double)cmp.getWidth())+"px");
            s.setProperty("height", scaleCoord((double)cmp.getHeight())+"px");
            s.setProperty("border", "none");
            s.setProperty("margin", "0");
        }
        
        
    }

    @Override
    public void beforeComponentPaint(Component c, Graphics g) {
        super.beforeComponentPaint(c, g);
        Object overlay = c.getNativeOverlay();
        if (overlay != null) {
            NativeOverlay no = (NativeOverlay)overlay;
            no.updateIfMovedAndFocused();
        }
        
    }
    
    
    
    @Override
    public Object createNativeOverlay(Component cmp) {
        if (!useNativeOverlaysForTextFields()) {
            // we only do this for phones and tablets
            return null;
        }
        if (cmp instanceof TextArea) {
            return new TextAreaNativeOverlay((TextArea)cmp);
        }
        return null;
    }

    @Override
    public void hideNativeOverlay(Component cmp, Object nativeOverlay) {
        if (nativeOverlay != null) {
            ((NativeOverlay)nativeOverlay).uninstall();
        }
    }

    @Override
    public void updateNativeOverlay(Component cmp, Object nativeOverlay) {
        if (nativeOverlay != null) {
            ((NativeOverlay)nativeOverlay).update();
        }
    }
    
    private int currCursorType;
    
    private void setCursor(int cursorType) {
        if (currCursorType != cursorType) {
            currCursorType = cursorType;
            String cursorStr = "default";
            switch (cursorType) {
                case Component.HAND_CURSOR:
                    cursorStr = "pointer";
                    break;

                case Component.DEFAULT_CURSOR:
                    cursorStr = "default";
                    break;
                case Component.NE_RESIZE_CURSOR:
                    cursorStr = "ne-resize";
                    break;
                case Component.NW_RESIZE_CURSOR:
                    cursorStr = "nw-resize";
                    break;
                case Component.W_RESIZE_CURSOR:
                    cursorStr = "w-resize";
                    break;
                case Component.E_RESIZE_CURSOR:
                    cursorStr = "e-resize";
                    break;
                case Component.N_RESIZE_CURSOR:
                    cursorStr = "n-resize";
                    break;
                case Component.S_RESIZE_CURSOR:
                    cursorStr = "s-resize";
                    break;
                case Component.MOVE_CURSOR:
                    cursorStr = "move";
                    break;
                case Component.CROSSHAIR_CURSOR:
                    cursorStr = "crosshair";
                    break;
                case Component.TEXT_CURSOR:
                    cursorStr = "text";
                    break;
                case Component.WAIT_CURSOR:
                    cursorStr = "wait";
                    break;
                case Component.SW_RESIZE_CURSOR:
                    cursorStr = "sw-resize";
                    break;
                case Component.SE_RESIZE_CURSOR:
                    cursorStr = "se-resize";
                    break;
                

            }
            outputCanvas.getStyle().setProperty("cursor", cursorStr);
            canvas.getStyle().setProperty("cursor", cursorStr);
            peersContainer.getStyle().setProperty("cursor", cursorStr);
            window.getDocument().getBody().getStyle().setProperty("cursor", cursorStr);
        }
        
        
        
    }
    
    private int lastCanvasWidth;
    private int lastCanvasHeight;
    
    public HTML5Implementation(){
        __init();
    }
    private boolean inited;
    
    @JSBody(params={"evt"}, script="return ''+evt.detail")
    private static native String getEventDetailString(Event evt);
    
    @JSBody(params={"evt"}, script="if (evt.code){return evt.code}else{return 0}")
    private static native int getEventCode(Event evt);

    @JSBody(params={"message", "code"}, script="return new CustomEvent('cn1outbox', {detail:message, code:code});")
    static native Event createCNOutboxEvent(String message, int code);
    
    
    @JSBody(params={"type", "message", "code"}, script="return new CustomEvent(type, {detail:message, code:code});")
    static native Event createCustomEvent(String type, String message, int code);
    
    @Override
    public void postMessage(MessageEvent message) {
        Event evt = createCNOutboxEvent(message.getMessage(), message.getCode());
        Window.current().dispatchEvent(evt);
    }
    
    @JSBody(params={"evt", "mimeType"}, script="try {return evt.clipboardData.getData(mimeType)}catch(e){return ''}")
    private native static String getPasteEventData(Event evt, String mimeType);
    
    @JSBody(params={"evt"}, script="try {return evt.clipboardData.files;} catch(e){return null}")
    private native static FileList getPasteEventFileList(Event evt);
    
    private void firePasteEvent() {
        callSerially(new Runnable() {
            public void run() {
                Form f = CN.getCurrentForm();
                if (f == null) {
                    return;
                }
                f.dispatchPaste(new ActionEvent(f));
            }
        });
    }
    
    private void __init() {
        if (inited) {
            return;
        }
        inited = true;
        instance=this;
        window = Window.current();
        HTMLDocument document = window.getDocument();
        canvas = (HTMLCanvasElement)document.createElement("canvas");
        outputCanvas = (HTMLCanvasElement)document.getElementById("codenameone-canvas");
        outputCanvas.getStyle().setProperty("pointer-events", "none");
        peersContainer = (HTMLElement)document.createElement("div");
        peersContainer.setAttribute("id", "cn1-peers-container");
        outputCanvas.getParentNode().insertBefore(peersContainer, outputCanvas);
        
        nativeEdt = EasyThread.start("NativeEDT");
        
        
        //outputCanvas.getStyle().setProperty("opacity", "0.5");
        updateCanvasSize();
        defaultFont = (NativeFont)createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        graphics = new BufferedGraphics(this, canvas);
        
        // Normalize browser locale
        String blang = getBrowserLanguage();
        if (blang == null || blang.indexOf("-") == -1) {
            blang = "en-US";
        }
        String lang = blang.substring(0, blang.indexOf("-"));
        String country = blang.substring(blang.indexOf("-")+1);
        Locale.setDefault(new Locale(lang, country));
        final Display disp = Display.getInstance();
        String browserTz = null;
        if ((browserTz = getProperty("browser.timezone", null)) != null) {
            TimeZone.setDefault(TimeZone.getTimeZone(browserTz));
            //_log("Setting default timezone to "+TimeZone.getDefault().getDisplayName());
        }
        
        final EventListener cn1InboxListener = new EventListener() {
            public void handleEvent(Event evt) {
                final String detailString = getEventDetailString(evt);
                final int eventCode = getEventCode(evt);
                JavaScriptBrowserLifecycleCoordinator.handleInboxEvent(new JavaScriptBrowserLifecycleCoordinator.InboxHooks() {
                    public void run() {
                    }

                    @Override
                    public void stopPropagation() {
                        evt.stopPropagation();
                    }

                    @Override
                    public void preventDefault() {
                        evt.preventDefault();
                    }

                    @Override
                    public void callSerially(Runnable runnable) {
                        HTML5Implementation.this.callSerially(runnable);
                    }

                    @Override
                    public void dispatchMessage(String message, int code) {
                        Display.getInstance().dispatchMessage(new MessageEvent(CN.getCurrentForm(), message, code));
                    }
                }, detailString, eventCode);
            }
        };
        
        final EventListener popstateListener = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                JavaScriptBrowserLifecycleCoordinator.handlePopState(new JavaScriptBrowserLifecycleCoordinator.BackNavigationHooks() {
                    @Override
                    public void callSerially(Runnable runnable) {
                        HTML5Implementation.this.callSerially(runnable);
                    }

                    @Override
                    public void runBackCommand() {
                        Form f = getCurrentForm();
                        if (f != null && f.getBackCommand() != null) {
                            f.getBackCommand().actionPerformed(new ActionEvent(f, ActionEvent.Type.Other));
                        }
                    }
                });
            }
            
        };
        
        // Handle browser resizing.
        final EventListener resizeListener = new EventListener(){

            @Override
            public void handleEvent(final Event evt) {
                
                callSerially(new Runnable(){

                    @Override
                    public void run() {
                        JavaScriptBrowserInteractionCoordinator.handleResize(new JavaScriptBrowserInteractionCoordinator.ResizeHooks() {
                            @Override
                            public void waitForResizeStabilization() {
                                CN.invokeAndBlock(new Runnable() {
                                    @Override
                                    public void run() {
                                        Util.sleep(1);
                                    }
                                });
                            }

                            @Override
                            public void updateCanvasSize() {
                                HTML5Implementation.this.updateCanvasSize();
                            }

                            @Override
                            public void sizeChanged() {
                                HTML5Implementation.this.sizeChanged(canvas.getWidth(), canvas.getHeight());
                            }

                            @Override
                            public void revalidate() {
                                HTML5Implementation.this.revalidate();
                            }
                        });
                    }

                });
            }
        };
        final EventListener hoverListener = new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    final MouseEvent me = (MouseEvent)evt;
                    new Thread() {
                        public void run() {
                            final int x = getClientX(me);
                            final int y = getClientY(me);
                            JavaScriptBrowserInteractionCoordinator.handleHover(new JavaScriptBrowserInteractionCoordinator.HoverHooks() {
                                @Override
                                public void dispatchHover(int x, int y) {
                                    Display.getInstance().pointerHover(new int[]{x}, new int[]{y});
                                }

                                @Override
                                public void setCursor(int cursor) {
                                    HTML5Implementation.this.setCursor(cursor);
                                }

                                @Override
                                public void callSerially(Runnable runnable) {
                                    Display.getInstance().callSerially(runnable);
                                }
                            }, new JavaScriptBrowserInteractionCoordinator.CursorLocator() {
                                @Override
                                public boolean isCursorEnabled() {
                                    Form f = _getCurrent();
                                    return f != null && f.isEnableCursors();
                                }

                                @Override
                                public int resolveCursorAt(int x, int y) {
                                    Form f = _getCurrent();
                                    if (f == null || x < 0 || x >= f.getWidth() || y < 0 || y >= f.getHeight()) {
                                        return Component.DEFAULT_CURSOR;
                                    }
                                    Component cmp = f.getComponentAt(x, y);
                                    return cmp != null ? cmp.getCursor() : Component.DEFAULT_CURSOR;
                                }
                            }, x, y, Component.DEFAULT_CURSOR);
                        }
                    }.start();
                }

            };
        
        hitTest = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                MouseEvent me = (MouseEvent)evt;
                int x = getClientX(me);
                int y = getClientY(me);
                if (hitTest(x, y)) {
                    evt.preventDefault();
                    evt.stopPropagation();
                }
            }
            
        };
        
        onPaste = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                String plainText = getPasteEventData(evt, "text/plain");
                String htmlText = getPasteEventData(evt, "text/html");
                FileList files = getPasteEventFileList(evt);
                String[] filePaths = null;
                if (files != null) {
                    int len = files.getLength();
                    filePaths = new String[len];
                    for (int i=0; i<len; i++) {
                        Blob file = files.item(i);
                        filePaths[i] = createTempFile(file);
                    }
                }
                JavaScriptBrowserLifecycleCoordinator.handlePaste(new JavaScriptBrowserLifecycleCoordinator.PasteHooks() {
                    @Override
                    public void copyPlainText(String text) {
                        HTML5Implementation.super.copyToClipboard(text);
                    }

                    @Override
                    public void copyHtmlText(String html) {
                        HTML5Implementation.super.copyToClipboard(html);
                    }

                    @Override
                    public void copyFiles(String[] paths) {
                        com.codename1.io.File[] cn1Files = new com.codename1.io.File[paths.length];
                        for (int i = 0; i < paths.length; i++) {
                            cn1Files[i] = new com.codename1.io.File(paths[i]);
                        }
                        HTML5Implementation.super.copyToClipboard(cn1Files);
                    }

                    @Override
                    public void firePasteEvent() {
                        HTML5Implementation.this.firePasteEvent();
                    }
                }, plainText, htmlText, filePaths);
            }
            
        };
        JavaScriptEventWiring.registerDocumentEvents(new JavaScriptEventWiring.DocumentRegistrar() {
            @Override
            public void add(String eventName, Object listener) {
                window.getDocument().addEventListener(eventName, (EventListener) listener);
            }
        }, onPaste);
        
        onMouseDown = new EventListener(){

            @Override
            public void handleEvent(Event evt) {
                if (nativeEventListener != null) {
                    CancelableEvent cevt = (CancelableEvent)evt;
                    nativeEventListener.handleEvent(evt);
                    if (cevt.isDefaultPrevented()) {
                        return;
                    }
                }
                final MouseEvent me = (MouseEvent)evt;
                final int x = getClientX(me);
                final int y = getClientY(me);
                debugLog("In mouseDown");
                focusInputElement();
                JavaScriptInputCoordinator.PointerRoutingDecision routing = JavaScriptInputCoordinator.beginPointerRouting(
                        Accessor.getActivePeerCount() > 0 && paintNativePeersBehind(), hitTest(x, y));
                pointerState.setGrabbedDrag(routing.grabbedDrag());
                if (routing.shouldConsumeEvent()) {
                    evt.preventDefault();
                    evt.stopPropagation();
                }
                if (JavaScriptInputCoordinator.shouldIgnoreMousePress(pointerState.isTouchDown(), pointerState.isMouseDown(), evt.getTarget() == textField || evt.getTarget() == textArea)) {
                    debugLog("[mouseDown] touchIsDown");
                    if (pointerState.isTouchDown()) {
                        pointerState.setMouseDown(false);
                    }
                    return;
                }
                onMouseMoveHandle = EventUtil.addEventListener(peersContainer, "mousemove", onMouseMove, true);
                onPointerMoveHandle = EventUtil.addEventListener(peersContainer, "pointermove", onMouseMove, true);
                
                pointerState.setLastMousePosition(x, y);
                pointerState.setMouseDown(true);
                callSerially(new Runnable() {
                    public void run() {
                        
                        if (isEditing){
                            finishTextEditing();
                        } 
                    }
                });
                lastMouseEvent = me;
                installBacksideHooksInUserInteraction();
                nativeCallSerially(new Runnable() {
                    public void run() {
                        HTML5Implementation.this.pointerPressed(new int[]{x}, new int[]{y});
                    }
                });
                if (contextListenerActive && me.getButton() == 2) {
                    contextListener.handleEvent(me);
                }
                
                
                
            }
        };
        
       
        
        onMouseUp = new EventListener(){
            @Override
            public void handleEvent(Event evt) {
                if (nativeEventListener != null) {
                    CancelableEvent cevt = (CancelableEvent)evt;
                    nativeEventListener.handleEvent(evt);
                    if (cevt.isDefaultPrevented()) {
                        return;
                    }
                }
                debugLog("In mouseUp");
                MouseEvent me = (MouseEvent)evt;
                final int x = getClientX(me) == -1 ? pointerState.getLastMouseX() : getClientX(me);
                final int y = getClientY(me) == -1 ? pointerState.getLastMouseY() : getClientY(me);
                focusInputElement();
                if (pointerState.isGrabbedDrag()) {
                    evt.preventDefault();
                    evt.stopPropagation();
                }
                pointerState.setGrabbedDrag(false);
                
                // Prevent conflicts with touch events
                // Guard against mouseUp if the mouse isn't already dwon
                if (pointerState.isTouchDown()) {
                    debugLog("[mouseUp] touchIsDown");
                    pointerState.setMouseDown(false);
                    return;
                }
                
                if (!pointerState.isMouseDown()) {
                    return;
                }
                pointerState.setMouseDown(false);
                
                
                
                EventUtil.removeEventListener(peersContainer, "mousemove", onMouseMoveHandle, true);
                EventUtil.removeEventListener(peersContainer, "pointermove", onPointerMoveHandle, true);
                
                pointerState.setLastTouchUpPosition(x, y);
                installBacksideHooksInUserInteraction();
                nativeCallSerially(new Runnable() {
                    public void run() {
                        HTML5Implementation.this.pointerReleased(new int[]{x}, new int[]{y});
                    }
                });
                callSerially(new Runnable() {
                    public void run() {
                        for (ActionListener l : mouseUpListeners) {
                            l.actionPerformed(null);
                        }
                    }
                });
                
            }
        };
        
        onTouchStart = new EventListener(){
            @SuppressSyncErrors
            @Override
            public void handleEvent(Event evt) {
                if (nativeEventListener != null) {
                    CancelableEvent cevt = (CancelableEvent)evt;
                    nativeEventListener.handleEvent(evt);
                    if (cevt.isDefaultPrevented()) {
                        return;
                    }
                }
                debugLog("In touchStart");
                TouchEvent me = (TouchEvent)evt;
                JSArray<MouseEvent> touches = me.getTargetTouches();
                
                int len = touches.getLength();
                final int[] x = new int[len];
                final int[] y = new int[len];
                
                for (int i=0; i<len; i++){
                    x[i] = getClientX(touches.get(i));
                    y[i] = getClientY(touches.get(i));
                }
                pointerState.setTouchStart(x[0], y[0], currentTimeMillisecondsJS());
                
                focusInputElement();
                
                JavaScriptInputCoordinator.PointerRoutingDecision routing = JavaScriptInputCoordinator.beginPointerRouting(
                        Accessor.getActivePeerCount() > 0 && paintNativePeersBehind(), hitTest(x[0], y[0]));
                pointerState.setGrabbedDrag(routing.grabbedDrag());
                if (routing.shouldConsumeEvent()) {
                    evt.preventDefault();
                    evt.stopPropagation();
                }
                JavaScriptInputCoordinator.TouchStartDecision touchDecision = JavaScriptInputCoordinator.resolveTouchStart(
                        pointerState.isMouseDown(), pointerState.isTouchDown(), evt.getTarget() == textField || evt.getTarget() == textArea, isEditing && editingStartingUp);
                if (touchDecision.shouldIgnoreEvent()) {
                    return;
                }
                if (touchDecision.shouldCancelMouseTracking()) {
                    debugLog("[touchStart] mouseIsDown");
                    pointerState.setMouseDown(false);
                    EventUtil.removeEventListener(peersContainer, "mousemove", onMouseMoveHandle, true);
                    EventUtil.removeEventListener(peersContainer, "pointermove", onPointerMoveHandle, true);
                    pointerState.setTouchDown(false);
                }
                pointerState.setTouchDown(true);
                
                
                pointerState.setTouches(x, y);
                
                onTouchMoveHandle = EventUtil.addEventListener(peersContainer, "touchmove", onTouchMove, true);
                
                callSerially(new Runnable() {

                    @Override
                    public void run() {
                        if (isEditing){
                            if (currentEditingField != null) {
                                pendingTextChanges = currentEditingField.getText();
                            }
                            if (!editingStartingUp) {
                                finishTextEditing();
                            } else {
                                editingStartingUp = false;
                            }
                        }
                    }
                });
                if (touchDecision.shouldFirePointerPressed()) {
                    installBacksideHooksInUserInteraction();
                    nativeCallSerially(new Runnable() {

                        @Override
                        public void run() {
                            HTML5Implementation.this.pointerPressed(x, y);
                        }
                    });
                }
                
            }
            
        };
        
        
        onTouchEnd = new EventListener(){

            
            @SuppressSyncErrors
            @Override
            public void handleEvent(Event evt) {
                if (nativeEventListener != null) {
                    CancelableEvent cevt = (CancelableEvent)evt;
                    nativeEventListener.handleEvent(evt);
                    if (cevt.isDefaultPrevented()) {
                        return;
                    }
                }
                debugLog("In TouchEnd");
                // Guard against mouse event conflicts
                // Prevent from firing if touch was not down already.
                if (JavaScriptInputCoordinator.shouldIgnoreTouchRelease(pointerState.isMouseDown(), pointerState.isTouchDown())) {
                    debugLog("[touchEnd] mouseIsDown");
                    if (pointerState.isMouseDown()) {
                        pointerState.setTouchDown(false);
                    }
                    return;
                }
                pointerState.setTouchDown(false);
                //if (evt.getTarget() == textField || evt.getTarget() == textArea) {
                //    // We don't want to respond to touch events on teh native input
                //    // fields because it can result in some infinite looping behaviour.
                //    return;
                //}
                focusInputElement();
                if (pointerState.isGrabbedDrag()) {
                    evt.preventDefault();
                    evt.stopPropagation();
                }
                pointerState.setGrabbedDrag(false);
                
                TouchEvent me = (TouchEvent)evt;
                EventUtil.removeEventListener(peersContainer, "touchmove", onTouchMoveHandle, true); 
                installBacksideHooksInUserInteraction();
                nativeCallSerially(new Runnable() {
                    @Override
                    public void run() {
                        pointerState.setLastTouchUpPosition(pointerState.getTouchesX()[0], pointerState.getTouchesY()[0]);
                        HTML5Implementation.this.pointerReleased(pointerState.getTouchesX(), pointerState.getTouchesY()); 
                        
                    }
                });
                if (JavaScriptInputCoordinator.shouldCreatePreemptiveTextField(usePreemptiveNativeTextFieldApproach(), pointerState.getTouchStartTime(), currentTimeMillisecondsJS(), pointerState.getTouchStartX(), pointerState.getTouchStartY(), pointerState.getTouchesX()[0], pointerState.getTouchesY()[0])) {
                    // Hack for iOS only to anticipate clicking on a text field
                    createAndFocusTextFieldPreemptively(pointerState.getTouchesX()[0], pointerState.getTouchesY()[0]);
                }
                callSerially(new Runnable() {
                    @Override
                    public void run() {
                        for (ActionListener l : mouseUpListeners) {
                            l.actionPerformed(null);
                        }
                    }
                });
                
            }
            
        };
        
        onTouchMove = new EventListener(){
            
            @Override
            public void handleEvent(Event evt) {
                debugLog("in TouchMove");
                TouchEvent me = (TouchEvent)evt;
                JSArray<MouseEvent> touches = me.getTargetTouches();
                
                int len = touches.getLength();
                final int[] x = new int[len];
                final int[] y = new int[len];
                
                for (int i=0; i<len; i++){
                    x[i] = getClientX(touches.get(i));
                    y[i] = getClientY(touches.get(i));
                }
                
                
                //focusInputElement();
                if (pointerState.isGrabbedDrag()) {
                    evt.preventDefault();
                    evt.stopPropagation();
                }
                
                if (JavaScriptInputCoordinator.shouldCancelTouchMove(pointerState.isMouseDown())) {
                    pointerState.setTouchDown(false);
                    EventUtil.removeEventListener(peersContainer, "touchmove", onTouchMoveHandle, true);
                    return;
                }
                
                
                pointerState.setTouches(x, y);
                nativeCallSerially(new Runnable() {
                    @Override
                    public void run() {
                        HTML5Implementation.this.pointerDragged(x, y);  
                    }
                });
            }
        };
        
        onMouseMove = new EventListener(){
            
            @Override
            public void handleEvent(Event evt) {
                debugLog("In mouseMove");
                MouseEvent me = (MouseEvent)evt;
                final int x = getClientX(me);
                final int y = getClientY(me);
                if (pointerState.isGrabbedDrag()) {
                    evt.preventDefault();
                    evt.stopPropagation();
                }

                if (JavaScriptInputCoordinator.shouldCancelMouseMove(pointerState.isTouchDown())) {
                    pointerState.setMouseDown(false);
                    EventUtil.removeEventListener(peersContainer, "mousemove", onMouseMoveHandle, true);
                    EventUtil.removeEventListener(peersContainer, "pointermove", onPointerMoveHandle, true);
                    return;
                }
                
                
                pointerState.setLastMousePosition(x, y);
                nativeCallSerially(new Runnable() {

                    @Override
                    public void run() {
                        HTML5Implementation.this.pointerDragged(x, y);
                    }
                });
            }
            
        };
        /*
        for (String eventName : new String[]{
                "mousedown", "mouseup", "mouseout", "wheel", "mousemove"}) {
            
            window.addEventListener(eventName, new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    if (Accessor.getActivePeerCount() > 0 && paintNativePeersBehind()) {
                        
                        MouseEvent me = (MouseEvent)evt;
                        int x = unscaleCoord(me.getClientX());
                        int y = unscaleCoord(me.getClientY());
                        
                       if (!hitTest(x, y)) {
                            _debug("1.Failed hit test at "+x+","+y);
                            _debugObj(evt);
                            outputCanvas.getStyle().setProperty("pointer-events", "none");
                        } else {
                            _debug("2. Passed hit test at "+x+","+y);
                            _debugObj(evt);
                            outputCanvas.getStyle().setProperty("pointer-events", "auto");
                        }
                        
                    }
                }

            }, true);

            window.addEventListener(eventName, new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    if (Accessor.getActivePeerCount() > 0 && paintNativePeersBehind() || 
                            "none".equals(outputCanvas.getStyle().getPropertyValue("pointer-events"))) {
                        _debug("3. Restoring events");
                        _debugObj(evt);
                        outputCanvas.getStyle().setProperty("pointer-events", "auto");
                    }
                }

            }, false);
        }
        */
        /*
        window.addEventListener("touchstart", new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                if (Accessor.getActivePeerCount() > 0 && paintNativePeersBehind()) {
                    TouchEvent te = (TouchEvent)evt;
                    if (te.getTargetTouches().getLength() > 0) {
                        MouseEvent me = te.getTargetTouches().get(0);
                        int x = unscaleCoord(me.getClientX());
                        int y = unscaleCoord(me.getClientY());
                        boolean hitTestResult = false;
                        if (!hitTest(x, y)) {
                            if (pointerState.isCapturingEvents()) {
                                _debug("4. Failed hit test at "+x+","+y);
                                _debugObj(evt);
                                pointerState.setCapturingEvents(false);
                                outputCanvas.getStyle().setProperty("pointer-events", "none");
                                outputCanvas.blur();
                                evt.stopPropagation();
                                evt.preventDefault();
                            }
                        } else {
                            if (!pointerState.isCapturingEvents()) {
                                pointerState.setCapturingEvents(true);
                                
                                outputCanvas.getStyle().setProperty("pointer-events", "auto");
                                outputCanvas.focus();
                                evt.stopPropagation();
                                evt.preventDefault();
                            }
                            
                        }
                        
                      
                    }
                } else {
                    if (!pointerState.isCapturingEvents()) {
                        pointerState.setCapturingEvents(true);
                        outputCanvas.getStyle().setProperty("pointer-events", "auto");
                        outputCanvas.focus();
                        evt.stopPropagation();
                        evt.preventDefault();
                    }
                }
            }
                
        }, true);
        
        window.addEventListener("mousedown", new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                if (Accessor.getActivePeerCount() > 0 && paintNativePeersBehind()) {

                    MouseEvent me = (MouseEvent)evt;
                    int x = unscaleCoord(me.getClientX());
                    int y = unscaleCoord(me.getClientY());

                   if (!hitTest(x, y)) {
                        if (pointerState.isCapturingEvents()) {
                            pointerState.setCapturingEvents(false);
                            _debug("1.Failed hit test at "+x+","+y);
                            _debugObj(evt);
                            outputCanvas.getStyle().setProperty("pointer-events", "none");
                            outputCanvas.blur();
                            evt.stopPropagation();
                            evt.preventDefault();
                           
                            
                        }
                    } else {
                        if (!pointerState.isCapturingEvents()) {
                            pointerState.setCapturingEvents(true);
                            _debug("2. Passed hit test at "+x+","+y);
                            _debugObj(evt);
                            outputCanvas.getStyle().setProperty("pointer-events", "auto");
                            outputCanvas.focus();
                            evt.stopPropagation();
                            evt.preventDefault();
                        }
                    }

                } else {
                    if (!pointerState.isCapturingEvents()) {
                        pointerState.setCapturingEvents(true);
                        outputCanvas.getStyle().setProperty("pointer-events", "auto");
                        outputCanvas.focus();
                        evt.stopPropagation();
                        evt.preventDefault();
                    }
                    
                }
            }

        }, true);
        */
        final EventListener wheelListener = new EventListener() {

                @Override
                public void handleEvent(final Event evt) {
                    MouseEvent me = (MouseEvent)evt;
                    final int x = getClientX(me);
                    final int y = getClientY(me);
                    if (hitTest(x, y)) { 
                        evt.preventDefault();
                        evt.stopPropagation();
                    }
                    
                    new Thread() {

                        @Override
                        public void run() {
                            mouseWheelMoved((WheelEvent)evt);
                        }
                    }.start();
                }

            };
        JavaScriptEventWiring.registerPeerPointerEvents(new JavaScriptEventWiring.ElementRegistrar() {
            @Override
            public void add(String eventName, Object listener, boolean capture) {
                peersContainer.addEventListener(eventName, (EventListener) listener, capture);
            }
        }, !debugFlag("disableMousedown"), !debugFlag("disableMouseup"), !debugFlag("disableTouchstart"),
                !debugFlag("disableTouchend"), !debugFlag("disableWheel"), getWheelEventType(),
                onMouseDown, hitTest, onMouseUp, onTouchStart, onTouchEnd, wheelListener);
        
        /**
         *  The installbacksidehooks event is an event that can be triggered from native javascript to install
         *  backside hooks.   This may be necessary if the user is interacting with the page outside of the app, or
         * in a native widget - the interaction should consititute a user interaction, but because the touch event handler
         * isn't triggered (where the backside hooks are usually installed).
         * listener isn't being called, the 
         */
        final EventListener installBacksideHooksListener = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                JavaScriptBrowserLifecycleCoordinator.handleInstallBacksideHooks(new JavaScriptBrowserLifecycleCoordinator.BacksideHooks() {
                    @Override
                    public void installBacksideHooksInUserInteraction() {
                        HTML5Implementation.this.installBacksideHooksInUserInteraction();
                    }
                });
            }
            
        };
        
        final EventListener keydownListener = new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                final KeyEvent kevt = (KeyEvent) evt;
                JavaScriptKeyboardInteractionAdapter.handleKeyDown(new JavaScriptKeyboardInteractionAdapter.EditingState() {
                    @Override
                    public boolean isEditing() {
                        return isEditing;
                    }
                }, new JavaScriptKeyboardInteractionAdapter.BacksideHooks() {
                    @Override
                    public void installBacksideHooksInUserInteraction() {
                        HTML5Implementation.this.installBacksideHooksInUserInteraction();
                    }
                }, new JavaScriptKeyboardInteractionAdapter.KeyDispatch() {
                    @Override
                    public void preventDefault() {
                        evt.preventDefault();
                    }

                    @Override
                    public void nativeCallSerially(Runnable runnable) {
                        HTML5Implementation.this.nativeCallSerially(runnable);
                    }

                    @Override
                    public void callSerially(Runnable runnable) {
                        HTML5Implementation.this.callSerially(runnable);
                    }

                    @Override
                    public void setShiftKeyDown(boolean down) {
                        shiftKeyDown = down;
                    }

                    @Override
                    public void setLastCharCode(int code) {
                        lastCharCode = code;
                    }

                    @Override
                    public int translateKeyCode(JavaScriptKeyboardInteractionAdapter.KeyEventView event) {
                        return getCode(kevt);
                    }

                    @Override
                    public void keyPressed(int code) {
                        HTML5Implementation.this.keyPressed(code);
                    }

                    @Override
                    public void keyReleased(int code) {
                    }

                    @Override
                    public void editFocusedTextArea(JavaScriptKeyboardInteractionAdapter.KeyEventView event) {
                    }
                }, new JavaScriptKeyboardInteractionAdapter.KeyEventView() {
                    @Override
                    public int getKeyCode() {
                        return kevt.getKeyCode();
                    }

                    @Override
                    public int getCharCode() {
                        return kevt.getCharCode();
                    }

                    @Override
                    public boolean isShiftKey() {
                        return kevt.isShiftKey();
                    }
                });
                
            }
            
        };
        
        final EventListener keyupListener = new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                final KeyEvent kevt = (KeyEvent) evt;
                JavaScriptKeyboardInteractionAdapter.handleKeyUp(new JavaScriptKeyboardInteractionAdapter.BacksideHooks() {
                    @Override
                    public void installBacksideHooksInUserInteraction() {
                        HTML5Implementation.this.installBacksideHooksInUserInteraction();
                    }
                }, new JavaScriptKeyboardInteractionAdapter.KeyDispatch() {
                    @Override
                    public void preventDefault() {
                    }

                    @Override
                    public void nativeCallSerially(Runnable runnable) {
                        HTML5Implementation.this.nativeCallSerially(runnable);
                    }

                    @Override
                    public void callSerially(Runnable runnable) {
                        HTML5Implementation.this.callSerially(runnable);
                    }

                    @Override
                    public void setShiftKeyDown(boolean down) {
                        shiftKeyDown = down;
                    }

                    @Override
                    public void setLastCharCode(int code) {
                        lastCharCode = code;
                    }

                    @Override
                    public int translateKeyCode(JavaScriptKeyboardInteractionAdapter.KeyEventView event) {
                        return getCode(kevt);
                    }

                    @Override
                    public void keyPressed(int code) {
                    }

                    @Override
                    public void keyReleased(int code) {
                        HTML5Implementation.this.keyReleased(code);
                    }

                    @Override
                    public void editFocusedTextArea(JavaScriptKeyboardInteractionAdapter.KeyEventView event) {
                    }
                }, new JavaScriptKeyboardInteractionAdapter.KeyEventView() {
                    @Override
                    public int getKeyCode() {
                        return kevt.getKeyCode();
                    }

                    @Override
                    public int getCharCode() {
                        return kevt.getCharCode();
                    }

                    @Override
                    public boolean isShiftKey() {
                        return kevt.isShiftKey();
                    }
                }); 
                
            }
            
        };
        
        final EventListener keypressListener = new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                final KeyEvent kevt = (KeyEvent) evt;
                JavaScriptKeyboardInteractionAdapter.handleKeyPress(new JavaScriptKeyboardInteractionAdapter.EditingState() {
                    @Override
                    public boolean isEditing() {
                        return isEditing;
                    }
                }, new JavaScriptKeyboardInteractionAdapter.BacksideHooks() {
                    @Override
                    public void installBacksideHooksInUserInteraction() {
                        HTML5Implementation.this.installBacksideHooksInUserInteraction();
                    }
                }, new JavaScriptKeyboardInteractionAdapter.KeyDispatch() {
                    @Override
                    public void preventDefault() {
                    }

                    @Override
                    public void nativeCallSerially(Runnable runnable) {
                        HTML5Implementation.this.nativeCallSerially(runnable);
                    }

                    @Override
                    public void callSerially(Runnable runnable) {
                        HTML5Implementation.this.callSerially(runnable);
                    }

                    @Override
                    public void setShiftKeyDown(boolean down) {
                        shiftKeyDown = down;
                    }

                    @Override
                    public void setLastCharCode(int code) {
                        lastCharCode = code;
                    }

                    @Override
                    public int translateKeyCode(JavaScriptKeyboardInteractionAdapter.KeyEventView event) {
                        return getCode(kevt);
                    }

                    @Override
                    public void keyPressed(int code) {
                    }

                    @Override
                    public void keyReleased(int code) {
                    }

                    @Override
                    public void editFocusedTextArea(JavaScriptKeyboardInteractionAdapter.KeyEventView event) {
                        Form currentForm = Display.getInstance().getCurrent();
                        if (currentForm != null) {
                            Component cmp = currentForm.getFocused();
                            if (cmp != null && cmp instanceof TextArea) {
                                TextArea ta = (TextArea) cmp;
                                int charCode = event.getCharCode();
                                switch (event.getKeyCode()) {
                                    case 11:
                                    case 9 : { // tab
                                        if (event.isShiftKey()) {
                                            cmp = currentForm.getPreviousComponent(cmp);
                                        } else {
                                            cmp = currentForm.getNextComponent(cmp);
                                        }
                                        
                                        charCode = 0;
                                        break;
                                    }
                                    /*
                                    case 11 : { // vertical tab
                                        if (kevt.isShiftKey()) {
                                            cmp = currentForm.getNextFocusUp();
                                        } else {
                                            cmp = currentForm.getNextFocusDown();
                                        }
                                        cmp.requestFocus();
                                        charCode = 0;
                                        break;
                                    }
                                    */
                                    case 10 :
                                    case 13 : { // enter /new line
                                        // Let's just let editString handle this for now.
                                    }
                                }
                                
                                
                                if (cmp instanceof TextArea) {
                                    ta = (TextArea) cmp;
                                } else {
                                    //if (cmp != null) {
                                    //    cmp.requestFocus();
                                    //    cmp.startEditingAsync();
                                    //}
                                    return;
                                }
                                Display.getInstance().editString(cmp, ta.getMaxSize(), ta.getConstraint(), ta.getText(), charCode);
                                
                            } 
                            
                        }
                    }
                }, new JavaScriptKeyboardInteractionAdapter.KeyEventView() {
                    @Override
                    public int getKeyCode() {
                        return kevt.getKeyCode();
                    }

                    @Override
                    public int getCharCode() {
                        return kevt.getCharCode();
                    }

                    @Override
                    public boolean isShiftKey() {
                        return kevt.isShiftKey();
                    }
                }); 
                 
            }
        };
        JavaScriptEventWiring.registerCoreWindowEvents(new JavaScriptEventWiring.WindowRegistrar() {
            @Override
            public void add(String eventName, Object listener, boolean capture) {
                window.addEventListener(eventName, (EventListener) listener, capture);
            }
        }, !debugFlag("disableHover"), cn1InboxListener, popstateListener, resizeListener, hoverListener,
                installBacksideHooksListener, keydownListener, keyupListener, keypressListener);
        
        animationFrameHandler = new JavaScriptAnimationFrameCallback(this);
        if (debugFlag("__retainAnimationFrameCallback")) {
            animationFrameHandler.onAnimationFrame(0);
        }
        scheduleAnimationFrame();
        
    }

    @SuppressSyncErrors
    public void handleAnimationFrame(double time) {

        if (graphicsLocked){
            // If the graphics is locked, we don't do anything
            scheduleAnimationFrame();
            return;

        }

        drainPendingDisplayFrame();
        scheduleAnimationFrame();

    }

    private boolean drainPendingDisplayFrame() {
        JavaScriptRenderQueueState.FrameSnapshot<ExecutableOp> frame =
                JavaScriptRenderQueueCoordinator.beginFrame(new JavaScriptRenderQueueCoordinator.GraphicsLock() {
                    @Override
                    public void setGraphicsLocked(boolean locked) {
                        graphicsLocked = locked;
                    }
                }, pendingDisplay);

        if (frame.isEmpty()) {
            return false;
        }
        CanvasRenderingContext2D context = (CanvasRenderingContext2D)outputCanvas.getContext("2d");
        context.save();
        context.beginPath();
        context.rect(frame.getCropX(), frame.getCropY(), frame.getCropW(), frame.getCropH());
        context.clip();
        // Wipe the drain region before the ops repaint it. Each drain carries a full
        // paint for its crop (form/body/toolbar/overlay), so stale pixels must not
        // bleed through from the previous drain. Without this, title bars from prior
        // forms accumulated across tests because the new form's paint did not always
        // cover every pixel in the toolbar region.
        context.clearRect(frame.getCropX(), frame.getCropY(), frame.getCropW(), frame.getCropH());

        for (ExecutableOp op : frame.getOps()){
            op.execute(context);
        }
        ClipRect.resetClip(context, graphics.getClipState());
        context.restore();
        return true;
    }

    private void scheduleAnimationFrame() {
        requestAnimationFrameNative(animationFrameHandler);
    }

    private static native int requestAnimationFrameNative(JavaScriptAnimationFrameCallback handler);
    
    public static void callSerially(final Runnable r) {
        new Thread() {
            public void run() {
                Display.getInstance().callSerially(r);
            }
        }.start();
    }

    @Override
    protected int getDragAutoActivationThreshold() {
        return 1000000;
    }
    
    private boolean scrollWheeling;
    
    @Override
    public boolean isScrollWheeling() {
        return scrollWheeling;
    }
    
    //@JSBody(script="return new Date.now()")
    //private static native int currentTimeMillisecondsJS();
    private static long currentTimeMillisecondsJS() {
        return System.currentTimeMillis();
    }
    
   private int getCode(KeyEvent evt) {
        int code = evt.getKeyCode();
        if(code >= 'A' && code <= 'Z') {
            int charCode = evt.getCharCode();
            if (charCode == 0) {
                charCode = lastCharCode;
            }
            return charCode;
        }
        return code;
    }
   
   /**
     * @inheritDoc
     */
    public int getClearKeyCode() {
        return 46;
    }

    /**
     * @inheritDoc
     */
    public int getBackspaceKeyCode() {
        return 8;
    }

    /**
     * @inheritDoc
     */
    public int getBackKeyCode() {
        return 27;
    }

    @Override
    public void systemOut(String content) {
        consoleLog(content);
    }
    
    /**
     * @inheritDoc
     */
    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case 38:
                return Display.GAME_UP;
            case 40:
                return Display.GAME_DOWN;
            case 39:
                return Display.GAME_RIGHT;
            case 37:
                return Display.GAME_LEFT;
            case 13:
                return Display.GAME_FIRE;
        }
        return 0;
    }

    /**
     * @inheritDoc
     */
    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case Display.GAME_UP:
                return 38;
            case Display.GAME_DOWN:
                return 40;
            case Display.GAME_RIGHT:
                return 39;
            case Display.GAME_LEFT:
                return 37;
            case Display.GAME_FIRE:
                return 13;
        }
        return 0;
    }

       
    public void nativeCallSerially(final Runnable r) {
        if (!debugFlag("useNativeQueue")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    nativeEdt.run(r);
                }
            }).start();
        } else {
            new Thread(r).start();
        }
    }
    
    @JSBody(params={}, script="return window.cn1WheelMultiplier || 1.0")
    private static native double wheelMultiplier();
    
    public void mouseWheelMoved(WheelEvent e) {
        NormalizedWheelEvent ne = normalizeWheelEvent(e);
        
        //e.preventDefault();
        //if (!isEnabled()) {
        //    return;
        //}
        final int x = getClientX(e);
        final int y = getClientY(e);
        //debugLog("in mouseWheelMoved at ("+x+","+y+")");
        //if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            //requestFocus();
            //final int units = convertToPixels(e.getUnitsToScroll() * 5, true) * -1;
        //final int dx = -(int)(e.getDeltaX() * getDevicePixelRatio() * wheelMultiplier());
        //final int dy = -(int)(e.getDeltaY() * getDevicePixelRatio() * wheelMultiplier());
        final int dx = -(int)(ne.getPixelX() * getDevicePixelRatio() * wheelMultiplier());
        final int dy = -(int)(ne.getPixelY() * getDevicePixelRatio() * wheelMultiplier());
        //debugLog("dx="+dx+"; dy="+dy);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                scrollWheeling = true;
                Form f = getCurrentForm();
                if(f != null){
                    //Component cmp = f.getContentPane().getComponentAt(x, y);
                    Component cmp = f.getComponentAt(x, y);
                    if(cmp != null && cmp.isFocusable()) {
                        cmp.setFocusable(false);
                        f.pointerPressed(x, y);
                        f.pointerDragged(x + dx, y + dy / 4);
                        cmp.setFocusable(true);
                    } else {
                        f.pointerPressed(x, y);
                        f.pointerDragged(x + dx, y + dy / 4);
                    }
                }
            }
        });
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Form f = getCurrentForm();
                if(f != null){
                    //Component cmp = f.getContentPane().getComponentAt(x, y);
                    Component cmp = f.getComponentAt(x, y);
                    if(cmp != null && cmp.isFocusable()) {
                        cmp.setFocusable(false);
                        f.pointerDragged(x + dx, y + dy / 4 * 2);
                        cmp.setFocusable(true);
                    } else {
                        f.pointerDragged(x + dx, y + dy / 4 * 2);
                    }
                }
            }
        });
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Form f = getCurrentForm();
                if(f != null){
                    //Component cmp = f.getContentPane().getComponentAt(x, y);
                    Component cmp = f.getComponentAt(x, y);
                    if(cmp != null && cmp.isFocusable()) {
                        cmp.setFocusable(false);
                        f.pointerDragged(x + dx, y + dy / 4 * 3);
                        cmp.setFocusable(true);
                    } else {
                        f.pointerDragged(x + dx, y + dy / 4 * 3);
                    }
                }
            }
        });
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Form f = getCurrentForm();
                if(f != null){
                    //Component cmp = f.getContentPane().getComponentAt(x, y);
                    Component cmp = f.getComponentAt(x, y);
                    if(cmp != null && cmp.isFocusable()) {
                        cmp.setFocusable(false);
                        f.pointerDragged(x + dx, y + dy);
                        f.pointerReleased(x + dx, y + dy);
                        cmp.setFocusable(true);
                    } else {
                        f.pointerDragged(x + dx, y + dy);
                        f.pointerReleased(x + dx, y + dy);
                    }
                }
                scrollWheeling = false;
            }
        });
        
    }
    
    private void updateCanvasSize() {
        JavaScriptCanvasLayout.Dimensions dimensions = JavaScriptCanvasLayout.compute(
                window.getDocument().getBody().getClientWidth(),
                window.getInnerHeight(),
                getDevicePixelRatio());
        canvas.setWidth(dimensions.getBackingWidth());
        canvas.setHeight(dimensions.getBackingHeight());
        outputCanvas.setWidth(dimensions.getBackingWidth());
        outputCanvas.setHeight(dimensions.getBackingHeight());
        peersContainer.getStyle().setProperty("height", dimensions.getCssHeight() + "px");
        peersContainer.getStyle().setProperty("width", dimensions.getCssWidth() + "px");
        if (dimensions.getStyleWidth() != null) {
            outputCanvas.getStyle().setProperty("width", dimensions.getStyleWidth());
            outputCanvas.getStyle().setProperty("height", dimensions.getStyleHeight());
            canvas.getStyle().setProperty("width", dimensions.getStyleWidth());
            canvas.getStyle().setProperty("height", dimensions.getStyleHeight());
        }
    }

    public void revalidate() {
        Form f = getCurrentForm();
        if (f != null){
            f.revalidate();
    
            flushGraphics();
        }

    }
    
    public static void setMainClass(Object main) {
        JavaScriptBootstrapCoordinator.bindMainClass(main,
                new JavaScriptBootstrapCoordinator.PushCallbackRegistrar() {
                    @Override
                    public void register(PushCallback callback) {
                        setPushCallback(callback);
                    }
                },
                new JavaScriptBootstrapCoordinator.PushCallbackRegistrar() {
                    @Override
                    public void register(PushCallback callback) {
                        HTML5Push.setPushCallback(callback);
                    }
                });
    }

    public MouseEvent getLastMouseEvent() {
        return lastMouseEvent;
    }
    
    @Override
    public void init(Object m) {
        //__init();
        if (m instanceof Runnable) {
            //((Runnable)m).run();
        }
        if (JavaScriptInputCoordinator.shouldInstallKeyboard(isPhoneOrTablet_())) {
            HTML5Keyboard.install();
        }
        // Set the locale
        installBeforeUnload();
        
        
        Font.setDefaultFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
        Display d = Display.getInstance();
        final WindowExt win = (WindowExt)Window.current();
        final Navigator navigator = win.getNavigator();
        JavaScriptRuntimeEnvironment environment = createRuntimeEnvironment(win, navigator);
        JavaScriptInitializationAdapter.applyEnvironment(new JavaScriptInitializationAdapter.PropertySink() {
            @Override
            public void setProperty(String key, String value) {
                d.setProperty(key, value);
            }
        }, environment);
        setAppArg(JavaScriptInitializationAdapter.resolveAppArg(environment));
        JavaScriptInitializationAdapter.runPostInit(new JavaScriptInitializationAdapter.RuntimeHooks() {
            @Override
            public void setDragStartPercentage(int percentage) {
                HTML5Implementation.this.setDragStartPercentage(percentage);
            }

            @Override
            public void initVideoCaptureConstraints() {
                // On iOS we can't really use capture constraints yet anyways
                // https://github.com/collab-project/videojs-record/issues/181
                // https://github.com/collab-project/videojs-record/issues/332
                VideoCaptureConstraints.init(new JSVideoCaptureConstraintsCompiler());
            }

            @Override
            public void registerSaveBlobToFile() {
                HTML5Implementation.registerSaveBlobToFile();
            }

            @Override
            public void initGoogle() {
                // Optional Google integration is intentionally disabled in the
                // ParparVM runtime path until reflective class loading is supported.
            }
        }, isIOS());
    }

    private JavaScriptRuntimeEnvironment createRuntimeEnvironment(final WindowExt win, final Navigator navigator) {
        return new JavaScriptRuntimeEnvironment(
                navigator.getPlatform(),
                navigator.getUserAgent(),
                navigator.getLanguage(),
                navigator.getAppName(),
                navigator.getAppCodeName(),
                navigator.getAppVersion(),
                win.getCN1DeploymentType(),
                ((WindowLocation)Window.current().getLocation()).getHref()
        );
    }

    @JSBody(params={"msg"}, script="window.onbeforeunload=function(){return msg;}")
    private native static void setBeforeUnloadMessage(String msg);
    
    @JSBody(params={}, script="window.onbeforeunload=function(){}")
    private native static void removeBeforeUnload();
    
    @Override
    public void setPlatformHint(String key, String value) {
        if ("platformHint.javascript.beforeUnloadMessage".equalsIgnoreCase(key)) {
            if (value == null) {
                removeBeforeUnload();
            } else {
                setBeforeUnloadMessage(value);
            }
        }
        if ("platformHint.javascript.backsideHooksInterval".equalsIgnoreCase(key)) {
            if (value == null) {
                value = "0"; 
            }
            int intVal = Integer.parseInt(value);
            if (intVal < 0) {
                if (backsideHooksIntervalHandle != 0) {
                    Window.clearInterval(backsideHooksIntervalHandle);
                    backsideHooksIntervalHandle = 0;
                }
            }
            backsideHooksIntervalTimeout = intVal;
        }
    }

    
    
    @Override
    public String getProperty(String key, String defaultValue) {
        Window win = (Window)Window.current();
        
        //Display.getInstance().getProperty("os.gzip", "false")
        if ("os.gzip".equals(key)) {
            // Flag used to indicate that the browser takes care of GZipped 
            // connection responses seamlessly so GZIPInputStream doesn't 
            // need to do anything.
            return "true";
        }
        if ("browser.window.location.href".equals(key)) {
            return ((WindowLocation)Window.current().getLocation()).getHref();
        }
        if ("browser.window.location.search".equals(key)) {
            return Window.current().getLocation().getSearch();
        }
        if ("browser.window.location.host".equals(key)) {
            return Window.current().getLocation().getHost();
        }
        if ("browser.window.location.hash".equals(key)) {
            return Window.current().getLocation().getHash();
        }
        if ("browser.window.location.origin".equals(key)) {
            return ((WindowLocation)Window.current().getLocation()).getOrigin();
        }
        if ("browser.window.location.pathname".equals(key)) {
            return ((WindowLocation)Window.current().getLocation()).getPathname();
        }
        if ("browser.window.location.protocol".equals(key)) {
            return Window.current().getLocation().getProtocol();
        }
        if ("browser.window.location.port".equals(key)) {
            return Window.current().getLocation().getPort();
        }
        if ("browser.window.location.hostname".equals(key)) {
            return ((WindowLocation)Window.current().getLocation()).getHostname();
        }
        if ("browser.timezone".equals(key)) {
            String tz = detectTimezone();
            return tz != null ? tz : defaultValue;
        }
        if ("HTML5.platformName".equals(key)) {
            if (isAndroid_()) {
                return "and";
            } else if (isIOS()) {
                return "ios";
            } else if (isMac()) {
                return "mac";
            } else {
                return "win";
            }
        }
        return super.getProperty(key, defaultValue);
    }
    
    
    
    
    
    private HTMLCanvasElement getCanvasBuffer(int width, int height){
        scratchBuffer = JavaScriptCanvasImageBufferLifecycle.ensureScratchBuffer(scratchBuffer, width, height,
                new JavaScriptCanvasImageBufferLifecycle.ScratchCanvasFactory<HTMLCanvasElement>() {
                    @Override
                    public HTMLCanvasElement createScratchCanvas() {
                        return (HTMLCanvasElement)window.getDocument().createElement("canvas");
                    }
                }, new JavaScriptCanvasImageBufferLifecycle.CanvasSizeAccess<HTMLCanvasElement>() {
                    @Override
                    public int getWidth(HTMLCanvasElement canvas) {
                        return canvas.getWidth();
                    }

                    @Override
                    public int getHeight(HTMLCanvasElement canvas) {
                        return canvas.getHeight();
                    }

                    @Override
                    public void setWidth(HTMLCanvasElement canvas, int canvasWidth) {
                        canvas.setWidth(canvasWidth);
                    }

                    @Override
                    public void setHeight(HTMLCanvasElement canvas, int canvasHeight) {
                        canvas.setHeight(canvasHeight);
                    }
                });
        return scratchBuffer;
    }
    
    @Override
    public void installNativeTheme(){
    	try {
            String nativeTheme = Display.getInstance().getProperty("javascript.native.theme", isAndroid_() ? "/android_holo_light.res" : "/iOS7Theme.res");
            Log.p("[installNativeTheme] attempting to load theme from " + nativeTheme);
            Resources r = Resources.open(nativeTheme);
            Log.p("[installNativeTheme] loaded theme resources, theme names: " + java.util.Arrays.toString(r.getThemeResourceNames()));
            Hashtable tp = r.getTheme(r.getThemeResourceNames()[0]);
            
            tp.put("StatusBar.padding", "0,0,0,0");
            
            UIManager.getInstance().setThemeProps(tp);
            Log.p("[installNativeTheme] successfully installed theme");
            return;
    	} catch (IOException ex){
            Log.p("[installNativeTheme] IOException loading theme: " + (ex.getMessage() != null ? ex.getMessage() : "null"));
            Log.e(ex);
    	} catch (Exception ex) {
            Log.p("[installNativeTheme] Exception loading theme: " + ex.getClass().getName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "null"));
            Log.e(ex);
        }
        return;
    }

    @Override
    public boolean hasNativeTheme() {
        return true;
    }

    private int isDesktop = -1;

    @Override
    public boolean isDesktop() {

        if (isDesktop == -1) {
            String overrideVal = getParameterByName("isDesktop");
            if ("1".equals(overrideVal)) {
                isDesktop = 1;
            } else if ("0".equals(overrideVal)) {
                isDesktop = 0;
            } else {
                isDesktop = isPhoneOrTablet_() ? 0:1;
            }
        }
        return isDesktop==1;
    }

    
    @JSBody(params={"name"}, script="return window.cn1_debug_flags && window.cn1_debug_flags[name];" )
    private native static boolean debugFlag(String name);

    /**
     * Returns true if this is a mobile browser - and not a tablet.
     * @return 
     */
    // From http://stackoverflow.com/a/11381730/2935174
    @JSBody(params={}, script="var a = navigator.userAgent||navigator.vendor||window.opera; "
            + "return /(android|bb\\d+|meego).+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|\\-[a-w])|libw|lynx|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\\-|your|zeto|zte\\-/i.test(a.substr(0,4));")
    private native static boolean isPhone_();
    
    @JSBody(params={}, script="var a = navigator.userAgent||navigator.vendor||window.opera;"
            + "return /(android|bb\\d+|meego).+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\\.(browser|link)|vodafone|wap|windows ce|xda|xiino|android|ipad|playbook|silk/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|\\-[a-w])|libw|lynx|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\\-|your|zeto|zte\\-/i.test(a.substr(0,4));")
    native static boolean isPhoneOrTablet_();

    @JSBody(script="return (navigator.userAgent.toLowerCase().indexOf(\"android\") > -1)")
    public native static boolean isAndroid_();
    
    @Override
    public int getDeviceDensity() {
        if (dDensity == -1) {
            JavaScriptDisplayMetrics.FormFactor ff;
            if (isPhone_()) {
                ff = JavaScriptDisplayMetrics.FormFactor.PHONE;
            } else if (isPhoneOrTablet_()) {
                ff = JavaScriptDisplayMetrics.FormFactor.TABLET;
            } else {
                ff = JavaScriptDisplayMetrics.FormFactor.DESKTOP;
            }
            dDensity = JavaScriptDisplayMetrics.pickDensity(getDevicePixelRatio(), ff, getDensityOverride());
        }
        return dDensity;
    }
    
    
    private static interface NormalizedWheelEvent extends JSObject {
        @JSProperty
        double getPixelX();
        
        @JSProperty
        double getPixelY();
        
        @JSProperty
        double getSpinX();
        
        @JSProperty
        double getSpinY();
    }
    
    @JSBody(params={"evt"}, script="return window.cn1NormalizeWheel(evt)")
    private native static NormalizedWheelEvent normalizeWheelEvent(WheelEvent evt);
    
    @JSBody(params={}, script="return window.cn1NormalizeWheel.getEventType()")
    public native static String getWheelEventType();
    
    private double ppi=0;
    private int dDensity = -1;
    
    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        if (ppi == 0) {
            ppi = JavaScriptDisplayMetrics.pixelsPerMillimeter(getDeviceDensity());
        }
        return (int) Math.round(((float) dipCount) * ppi);
    }
    
    @JSBody(params={}, script="var ua = navigator.userAgent.toLowerCase(); \n" +
        "if (ua.indexOf('safari') != -1) { \n" +
        "  if (ua.indexOf('chrome') > -1) {\n" +
        "    return false;\n" +
        "  } else {\n" +
        "    return true;\n" +
        "  }\n" +
        "}\n"
            + "return false;")
    private static native boolean _isSafari();
    private static boolean isSafari;
    private static boolean isSafariChecked;
    public static boolean isSafari() {
        if (!isSafariChecked) {
            isSafariChecked = true;
            isSafari = _isSafari();
        }
        return isSafari;
    }
    
    @JSBody(params={}, script="return (/iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream) ||  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)")
    public static native boolean isIOS();
    
    @JSBody(params={}, script="return /Mac/.test(navigator.userAgent)")
    public static native boolean isMac();
    
    // From https://stackoverflow.com/questions/57599945/how-to-detect-ios-13-on-javascript
    @JSBody(params={}, script="return \"download\" in document.createElement(\"a\")")
    private static native boolean doesAnchorSupportDownload();
    
    private static boolean isIOS13() {
        return isIOS() && doesAnchorSupportDownload();
    }
    
    @JSBody(params={}, script="return (navigator.userAgent.match(/iPad/i) != null) ||  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)")
    private static native boolean isIPad();
    
    
    @JSBody(params={}, script="if (window.overridePixelRatio === undefined) {"
            + "    var ratioStr = getParameterByName('pixelRatio');"
            + "    if (ratioStr != '') {"
            + "        window.overridePixelRatio = parseFloat(ratioStr);"
            + "    } else {"
            + "        window.overridePixelRatio = 0;"
            + "    }"
            + "    if (window.cn1ScaleCoord === undefined){ window.cn1ScaleCoord = function(x) { return x===-1?-1:x/(window.overridePixelRatio || window.devicePixelRatio || 1.0);};}"
            + "    if (window.cn1UnscaleCoord === undefined){ window.cn1UnscaleCoord = function(x) { return x===-1?-1:x*(window.overridePixelRatio || window.devicePixelRatio || 1.0);};}"
            + "}"
            + "return window.overridePixelRatio || window.devicePixelRatio || 1.0")
    static native double getDevicePixelRatio_();
    
    
    @JSBody(params={"name"}, script="return getParameterByName(name);")
    static native String getParameterByName(String name);
    
    static double getDevicePixelRatio() {
        if (devicePixelRatio < 0) {
            devicePixelRatio = getDevicePixelRatio_();
        }
        return devicePixelRatio;
    }

    @Override
    public int getDisplayWidth() {
        return canvas.getWidth();
    }

    @Override
    public int getDisplayHeight() {
        return canvas.getHeight();
    }

    @Override
    public boolean isNativeInputSupported() {
        return true;
    }

    @Override
    public boolean isNativeInputImmediate() {
        return true;
    }

    @JSBody(params={"ext"}, script="switch (ext) {"
            + "case 'aac': return 'audio/x-aac';"
            + "case 'aif': return 'audio/x-aiff';"
            + "case 'uva': return 'audio/vnd.dece.audio';"
            + "case 'm3u': return 'audio/x-mpegurl';"
            + "case 'wma': return 'audio/x-ms-wma';"
            + "case 'mid': return 'audio/midi';"
            + "case 'mpga': return 'audio/mpeg';"
            + "case 'mp4a': return 'audio/mp4';"
            + "case 'mp3': return 'audio/mp3';"
            + "case 'oga': return 'audio/ogg';"
            + "case 'weba': return 'audio/webm';"
            + "case 'wav': return 'audio/x-wav';"
            + "case '3gp': return 'video/3gpp';"
            + "case '3g2': return 'video/3gpp2';"
            + "case 'avi': return 'video/x-msvideo';"
            + "case 'f4v': return 'video/x-f4v';"
            + "case 'flv': return 'video/xflv';"
            + "case 'h261': return 'video/h261';"
            + "case 'h263': return 'video/h263';"
            + "case 'h264': return 'video/h264';"
            + "case 'jpgv': return 'video/jpeg';"
            + "case 'm4v': return 'video/x-m4v';"
            + "case 'asf': return 'video/x-ms-asf';"
            + "case 'wm': return 'video/x-ms-wm';"
            + "case 'wmx': return 'video/x-mws-wmx';"
            + "case 'wmv': return 'video/x-ms-wmv';"
            + "case 'mpeg': return 'video/mpeg';"
            + "case 'mp4': return 'video/mp4';"
            + "case 'ogv': return 'video/ogg';"
            + "case 'webm': return 'video/webm';"
            + "case 'qt': return 'video/quicktime';"
            + "default: return 'video/mp4';"
            + "}")
    private static native String getMimeForExtension(String ext);
    
    private static String guessMime(String uri) {
        if (isTempFile(uri)) {
            Blob tmpFile = getTempFile(uri);
            if (tmpFile != null && tmpFile.getType() != null && !"".equals(tmpFile.getType())) {
                return tmpFile.getType();
            }
        }
        if (uri.indexOf("#") > 0) {
            uri = uri.substring(0, uri.indexOf("#"));
        }
        if (uri.indexOf("?") > 0) {
            uri = uri.substring(0, uri.indexOf("?"));
        }
        if (uri.lastIndexOf(".") > 0) {
            String ext = uri.substring(uri.lastIndexOf(".")+1);
            return getMimeForExtension(ext);
            
        }
        
        return "application/octet-stream";
        
    }
    
    @Override
    public Media createMedia(String uri, boolean isVideo, final Runnable onCompletion) throws IOException {
        return createMedia(uri, isVideo, null, onCompletion);
    }

    @Override
    public boolean isNativeVideoPlayerControlsIncluded() {
        return true;
    }

    @Override
    public AsyncResource<Media> createMediaAsync(InputStream stream, String mimeType, Runnable onCompletion) {
        final AsyncResource<Media> out = new AsyncResource<Media>();
        final HTML5Media media;
        try {
            media = (HTML5Media)createMedia(stream, mimeType, onCompletion, false);
        } catch (IOException ex) {
            out.error(ex);
            return out;
        }
        final boolean[] handled = new boolean[1];
        final HTMLMediaElement el = media.getMediaElement();
        final EventListener errorListener = new EventListener() {
            @Override
            public void handleEvent(final Event evt) {
                _logObj(((com.codename1.html5.js.dom.HTMLMediaElement)evt.getTarget()).getError());
                new Thread() {
                    public void run() {
                        _log("error event received loading stream");
                        
                        if (handled[0]) {
                            Log.p("WARNING: error event called after load events already handled when loading media from stream ");
                            return;
                        }
                        handled[0] = true;
                        out.error(new IOException("Failed to load media from stream"));
                    }
                }.start();
            }

        };
        el.addEventListener("error", errorListener);
        mediaPool().addCleanupListener(new HTML5MediaPool.CleanupListener(el) {
            @Override
            public void run(HTMLElement thisEl) {
                thisEl.removeEventListener("error", errorListener);
            }
        });
        final EventListener loadedMetadataListener = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                new Thread() {
                    public void run() {
                        _log("loadstart event received loading stream");
                        if (handled[0]) {
                            Log.p("WARNING: loadstart event called after load events already handled when loading media from stream ");
                            return;
                        }
                        handled[0] = true;
                        out.complete(media);
                    }
                }.start();
            }

        };
        el.addEventListener("loadedmetadata", loadedMetadataListener);
        mediaPool().addCleanupListener(new HTML5MediaPool.CleanupListener(el) {
            @Override
            public void run(HTMLElement thisEl) {
                thisEl.removeEventListener("loadedmetadata", loadedMetadataListener);
            }
            
        });
        
        return out;
    }

    
    
    @Override
    public AsyncResource<Media> createMediaAsync(final String uri, boolean video, Runnable onCompletion) {
        final AsyncResource<Media> out = new AsyncResource<Media>();
        final HTML5Media media;
        try {
            media = (HTML5Media)createMedia(uri, video, null, onCompletion, false);
        } catch (IOException ex) {
            out.error(ex);
            return out;
        }
        final boolean[] handled = new boolean[1];
        final HTMLMediaElement el = media.getMediaElement();
        final EventListener errorListener = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                _logObj(((com.codename1.html5.js.dom.HTMLMediaElement)evt.getTarget()).getError());
                new Thread() {
                    public void run() {
                        _log("error event received loading stream");
                        _log(uri);
                        if (handled[0]) {
                            Log.p("WARNING: error event called after load events already handled when loading media from uri "+uri);
                            return;
                        }
                        handled[0] = true;
                        out.error(new IOException("Failed to load media from "+uri));
                    }
                }.start();
            }

        };
        el.addEventListener("error", errorListener);
        final EventListener loadedMetaDataListener = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                new Thread() {
                    public void run() {
                        _log("loadstart received loading stream");
                        _log(uri);
                        if (handled[0]) {
                            Log.p("WARNING: loadstart event called after load events already handled when loading media from uri "+uri);
                            return;
                        }
                        handled[0] = true;
                        out.complete(media);
                    }
                }.start();
            }

        };
        el.addEventListener("loadedmetadata", loadedMetaDataListener);
        mediaPool().addCleanupListener(new HTML5MediaPool.CleanupListener(el) {
            @Override
            public void run(HTMLElement thisEl) {
                thisEl.removeEventListener("error", errorListener);
                thisEl.removeEventListener("loadedmetadata", loadedMetaDataListener);
            }
        });
        return out;
    }

    
    private HTML5MediaPool mediaPool;
    
    HTML5MediaPool mediaPool() {
        if (mediaPool == null) {
            mediaPool = new HTML5MediaPool();
        }
        return mediaPool;
    }
    
    private Media createMedia(String uri, boolean isVideo, String mime, final Runnable onCompletion) throws IOException {
        return createMedia(uri, isVideo, mime, onCompletion, true);
    }
    
    
    
    private Media createMedia(String uri, boolean isVideo, String mime, final Runnable onCompletion, boolean blocking) throws IOException {
        //_log("Creating media for "+uri);
        HTMLMediaElement el = null;
        if (isVideo){
            el = mediaPool().createVideoElement();
        } else {
            el = mediaPool().createAudioElement();
        }
        
        mime = mime == null ? guessMime(uri) : mime;
        if (isVideo && (mime == null || !mime.startsWith("video"))) {
            mime = "video/mp4";
        }
        if (!isVideo && (mime == null || !mime.startsWith("audio"))) {
            mime = "audio/wav";
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        //if (uri.indexOf("file://") == 0) {
        if (fs.exists(uri)) {
            mediaPool().returnMediaElement(el);
            _log("Opening media from file system "+uri);
            return createMedia(this.openFileInputStream(uri), mime, onCompletion, blocking);
            //ArrayBufferInputStream is = (ArrayBufferInputStream)this.openFileInputStream(uri);
            //String dataURL = arrayBufferToDataURL(is.getBuffer().getBuffer(), mime);
            //el.setSrc(dataURL);
        } else if (uri.indexOf("//") >= 0 || uri.indexOf("data:") == 0 || uri.indexOf("assets/") == 0) {
            _log("Opening media from uri "+uri);
            el.setSrc(uri);
        } else {
            _log("Opening media from resource stream "+uri);
            ArrayBufferInputStream is = (ArrayBufferInputStream)this.getResourceAsStream(null, uri);
            String dataURL = arrayBufferToDataURL(is.getBuffer().getBuffer(), mime);
            el.setSrc(dataURL);
        }
        if (blocking) {
            final boolean[] error = new boolean[1];
            final boolean[] complete = new boolean[1];
            final EventListener errorListener = new EventListener() {
                @Override
                public void handleEvent(Event evt) {
                    _logObj(((com.codename1.html5.js.dom.HTMLMediaElement)evt.getTarget()).getError());
                    new Thread() {
                        public void run() {
                            synchronized(complete) {
                                error[0] = true;
                                complete[0] = true;
                                complete.notify();
                            }
                        }
                    }.start();
                }

            };
            
            el.addEventListener("error", errorListener);
            mediaPool().addCleanupListener(new HTML5MediaPool.CleanupListener(el) {
                public void run(HTMLElement theEl) {
                    theEl.removeEventListener("error", errorListener);
                }
            });
            final EventListener loadedMetaDataListener = new EventListener() {
                @Override
                public void handleEvent(Event evt) {
                    new Thread() {
                        public void run() {
                            synchronized(complete) {
                                complete[0] = true;
                                complete.notify();
                            }
                        }
                    }.start();
                    
                }

            };
            el.addEventListener("loadedmetadata", loadedMetaDataListener);
            mediaPool().addCleanupListener(new HTML5MediaPool.CleanupListener(el) {
                public void run(HTMLElement theEl) {
                    theEl.removeEventListener("loadedmetadata", loadedMetaDataListener);
                }
            });
            while (!complete[0]) {
                invokeAndBlock(new Runnable() {
                    public void run() {
                        synchronized(complete) {
                            Util.wait(complete);
                        }
                    }

                });
            }
            if (error[0]) {
                throw new IOException("Failed to load media from uri "+uri);
            }
        }
        HTML5Media out = new HTML5Media(el, isVideo);
        if (onCompletion!=null){
            out.addCompletionHandler(onCompletion);
        }
        
        
        return out;
        
    }
    
    @Override
    public void addCompletionHandler(Media media, Runnable onCompletion) {
        
        super.addCompletionHandler(media, onCompletion); 
        if (media instanceof HTML5Media) {
            ((HTML5Media)media).addCompletionHandler(onCompletion);
        }
    }

    @Override
    public void removeCompletionHandler(Media media, Runnable onCompletion) {
        if (media instanceof HTML5Media) {
            ((HTML5Media)media).removeCompletionHandler(onCompletion);
        }
        super.removeCompletionHandler(media, onCompletion); 
    }

    
    
    @Override
    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return createMedia(stream, mimeType, onCompletion, true);
    }

    /**
     * Creates media.
     * @param stream InputStream to create media for
     * @param mimeType Mimetype of the media.  May be null.
     * @param onCompletion Callback to run on completion of playing media.
     * @param blocking Whether to use invokeAndBlock to block return until the media has either errored, or started loading.  Normal calls
     * to createMedia block so that errors result in an IOException being thrown.  Async wrappers will call this method with blocking=false
     * to that they can handle errors and returns asynchronously, without underlying baggage of invokeAndBlock
     * @return A Media element
     * @throws IOException If blocking=true and media fails to load (e.g. can't be found).
     */
    private Media createMedia(InputStream stream, String mimeType, Runnable onCompletion, boolean blocking) throws IOException {
        if (stream instanceof ArrayBufferInputStream){
            ArrayBufferInputStream bufStream = (ArrayBufferInputStream)stream;
            String src = bufStream.getSrc();
            if (src != null){
                return createMedia(src, mimeType.indexOf("video")!=-1, mimeType, onCompletion, blocking);
            }
            
            Blob blob = bufStream.getBlob();
            if (mimeType != null && !Objects.equals(blob.getType(), mimeType)) {
                blob = BlobUtil.toType(blob, mimeType);
            }
            URLBuilderFactory factory = (URLBuilderFactory)window;
            URLBuilder urlBuilder = factory.getURL();
        
            String objUrl = urlBuilder.createObjectURL(blob);
            return createMedia(objUrl, mimeType.indexOf("video")!=-1, mimeType, onCompletion, blocking);  
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Util.copy(stream, baos);
            Blob blob = BlobUtil.createBlob(baos.toByteArray(), mimeType);
            URLBuilderFactory factory = (URLBuilderFactory)window;
            URLBuilder urlBuilder = factory.getURL();
            String objUrl = urlBuilder.createObjectURL(blob);
            return createMedia(objUrl, mimeType.indexOf("video")!=-1, mimeType, onCompletion, blocking);
        }
    }

    @Override
    public boolean isGalleryTypeSupported(int type) {
        if (super.isGalleryTypeSupported(type)) {
            return true;
        }
        
        switch (type) {
            case Display.GALLERY_ALL_MULTI:
            case Display.GALLERY_IMAGE_MULTI:
            case Display.GALLERY_VIDEO_MULTI:
            case -9999:
            case -9998:
                return true;
        }
        return false;
    }

    
    
    @Override
    public void openGallery(ActionListener response, int type) {
        String accept = null;
        boolean multiple = false;
        switch (type) {
            case -9998:
            case Display.GALLERY_ALL_MULTI:
            case Display.GALLERY_IMAGE_MULTI:
            case Display.GALLERY_VIDEO_MULTI:
                multiple = true;
                break;
        }
        if (type == -9998) {
            type = -9999;
        }
        switch (type) {
            case Display.GALLERY_IMAGE:
            case Display.GALLERY_IMAGE_MULTI:
                accept = "image/*";
                break;
            case Display.GALLERY_VIDEO:
            case Display.GALLERY_VIDEO_MULTI:
                accept = "video/*";
                break;
            case -9999:
                accept = Display.getInstance().getProperty("javascript.openGallery.accept", null);
                break;
        }
        com.codename1.teavm.ext.usermedia.FileChooser chooser = new com.codename1.teavm.ext.usermedia.FileChooser(response, accept, null);
        chooser.setMultiple(multiple);
        if (accept != null && accept.startsWith("image/")) {
            chooser.setFixImageOrientation(true);
        }
        chooser.showDialog();
    }

    @Override
    public void captureVideo(ActionListener response) {
        new com.codename1.teavm.ext.usermedia.FileChooser(response, "video/*", "camcorder").showDialog();
    }

    @Override
    public void captureVideo(VideoCaptureConstraints constraints, final ActionListener response) {
        if (constraints == null || constraints.isNullConstraint() || isIOS()) {
            // There are no constraints here... just use the default video capture.
            
            // On iOS we can't really use capture constraints yet anyways
            // https://github.com/collab-project/videojs-record/issues/181
            // https://github.com/collab-project/videojs-record/issues/332
            
            captureVideo(response);
            return;
        }
        
        VideoJS.Options opts = VideoJS.newOptions();
        VideoJS.RecordOptions recordOptions = VideoJS.newRecordOptions();
        opts.setPlugins(VideoJS.newPlugins(recordOptions));
        VideoJS.MediaStreamConstraints videoConstraints = VideoJS.newMediaStreamConstraints();
        recordOptions.setVideo(videoConstraints);
        if (constraints.getHeight() != 0) {
            videoConstraints.setHeight(constraints.getHeight());
        }
        if (constraints.getWidth() != 0) {
            videoConstraints.setWidth(constraints.getWidth());
        }
        if (constraints.getMaxLength() != 0) {
            recordOptions.setMaxLength(constraints.getMaxLength());
        }
        try {
            final VideoJS videoJS = new VideoJS(null, opts);
            videoJS.addListener(new VideoJS.VideoListener() {
                @Override
                public void onDeviceError(String errorCode) {
                    videoJS.destroy();
                    Log.e(new IOException("Device error code: "+errorCode));
                    response.actionPerformed(new ActionEvent(null));
                }

                @Override
                public void onError(String message) {
                    videoJS.destroy();
                    Log.e(new IOException(message));
                    response.actionPerformed(new ActionEvent(null));
                }

                @Override
                public void onStartRecord() {
                    
                }

                @Override
                public void onFinishRecord(Blob recordedData) {
                    videoJS.destroy();
                    response.actionPerformed(new ActionEvent(HTML5Implementation.createTempFile(recordedData)));
                }
            });
        } catch (IOException ioe) {
            Log.e(ioe);
            Log.p("VideoJS is not loaded, using default captureVideo behaviour.  Add the javascript.includeVideoJS build hint in order to use capture video with video constraints support.");
            captureVideo(response);
        }
    }
    
    

    @Override
    public void captureAudio(ActionListener response) {
        //new com.codename1.teavm.ext.usermedia.FileChooser(response, "audio/*", "capture").showDialog();
        super.captureAudio(response);
    }

    @Override
    public String[] getAvailableRecordingMimeTypes() {
        return new String[]{"audio/wav"};
    }

    @Override
    public Media createMediaRecorder(String path, String mimeType) throws IOException {
        return createMediaRecorder(new MediaRecorderBuilder().path(path).mimeType(mimeType));
    }

    @Override
    public Media createMediaRecorder(MediaRecorderBuilder builder) throws IOException {
        return new HTML5MediaRecorder(builder);
    }

    
    
    
    
    
    @Override
    public void capturePhoto(final ActionListener response) {
        String defaultGetUserMedia = CN.isDesktop() ? "true" : "false";
        if (!PhotoCapture.isSupported() || Display.getInstance().getProperty("javascript.useGetUserMedia", defaultGetUserMedia).equals("false")) {
             com.codename1.teavm.ext.usermedia.FileChooser chooser = new com.codename1.teavm.ext.usermedia.FileChooser(response, "image/*", "camera");
             // On iOS we need to fix image orientation
             // https://github.com/codenameone/CodenameOne/issues/2694
             chooser.setFixImageOrientation(true);
             chooser.showDialog();
             return;
        }
        
        PhotoCapture capture = new PhotoCapture();
        HTMLCanvasElement photo = capture.showDialog();
        if (photo == null) {
            Display.getInstance().callSerially(new Runnable(){

                @Override
                public void run() {
                    response.actionPerformed(new ActionEvent(null));
                }

            });
            return;
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String imagePath = generateUniqueImagePath(fs, "photo", "png");
        try {

            OutputStream fos = fs.openOutputStream(imagePath);
            Blob blob = BlobUtil.canvasToBlob(photo, "image/png", 100);

            InputStream blobInput = BlobUtil.openInputStream(blob);
            Util.copy(blobInput, fos);
            Util.cleanup(blobInput);
            Util.cleanup(fos);

        } catch (IOException ex){
            //Log.e(ex);
            consoleLog("Error in capturePhoto");
            consoleLog(ex.getMessage());
            imagePath = null;
        }
        final String path = imagePath;
        Display.getInstance().callSerially(new Runnable(){

            @Override
            public void run() {
                response.actionPerformed(new ActionEvent(path));
            }

        });
        
    }
    
    
    public void capturePhoto_old(final ActionListener response) {
        WindowExt win = (WindowExt)window;
        CN1Native cn1 = win.getCn1();
        
        FileSystemStorage fs = FileSystemStorage.getInstance();
        
        
        
        cn1.capturePhoto(new JSOImplementations.CapturePhotoCallback() {

            @Override
            public void callback(final HTMLCanvasElement canvas) {
                
                new Thread(){
                    public void run(){
                        FileSystemStorage fs = FileSystemStorage.getInstance();
                        String imagePath = generateUniqueImagePath(fs, "photo", "png");
                        try {
                            
                            OutputStream fos = fs.openOutputStream(imagePath);
                            Blob blob = BlobUtil.canvasToBlob(canvas, "image/png", 100);
                            
                            InputStream blobInput = BlobUtil.openInputStream(blob);
                            Util.copy(blobInput, fos);
                            Util.cleanup(blobInput);
                            Util.cleanup(fos);
                            
                        } catch (IOException ex){
                            Log.e(ex);
                            imagePath = null;
                        }
                        final String path = imagePath;
                        Display.getInstance().callSerially(new Runnable(){

                            @Override
                            public void run() {
                                response.actionPerformed(new ActionEvent(path));
                            }
                            
                        });
                    }
                }.start();
            }
        }, cameraWidth, cameraHeight);
    }

    
    private String generateUniqueImagePath(FileSystemStorage fs, String baseName, String ext){
        if (!fs.exists(photosPath)) {
            fs.mkdir(photosPath);
        } 
        String prefix = baseName;
        String suffix = "";
        char sep = fs.getFileSystemSeparator();
        String out = photosPath+sep+prefix+suffix+"."+ext;
        while (fs.exists(out)){
            if ("".equals(suffix)){
                suffix="-1";
            } else {
                suffix = "-"+(Integer.parseInt(suffix.substring(1))+1);
            }
            out = photosPath+sep+prefix+suffix+"."+ext;
        }
        return out;
    }

    @JSBody(params={"el", "selector"}, script="return jQuery(el).is(selector)")
    private native static boolean jQuery_is_(JSObject el, String selector);
    
    @JSBody(params={"y"}, script="jQuery(window).scrollTop(y)")
    private native static void scrollToY(int y);
    
    @JSBody(params={}, script="return jQuery(window).scrollTop()")
    private native static int getScrollY_();
    
    @JSBody(params={}, script="jQuery(window).scroll()")
    private native static void scroll_();
    
    private boolean isEditing=false;
    
    /**
     * Text field that is used for editing.
     * Due to a bug in Chrome that kills performance if you add a text field, focus it,
     * and remove it, we create one field ONE TIME, then just show it and hide it.
     * DO NOT REMOVE THIS FIELD FROM THE DOM or it will kill canvas performance in
     * Chrome for some unexplicable reason.  Related to https://github.com/shannah/cn1-teavm-port/issues/31
     */
    private HTMLInputElement textField, textArea;
    private boolean isEditingSingleLine;
    private boolean doneEventFired;
    private boolean tabNext, tabPrev;
    
    private int vkbHeight;
    
    // To alleviate race conditions during editing we keep track of
    // the timestamps when each field is updated (i.e. the codenameone
    // text field vs the native text field
    //private long lastCN1InputTime, lastHTMLInputTime;
    
    
    private void safeSleep(final int millis) {
        Display.getInstance().invokeAndBlock(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException ex) {
                    //Log.e(ex);
                }
            }
            
        });
    }
    
    private void focusInputElement() {
        if (isEditing && currentInputField != null && !jQuery_is_(currentInputField, ":focus")) {
            currentInputField.focus();
        }
    }
    
    boolean isNativeInputFieldFocused() {
        return (isEditing && currentInputField != null && jQuery_is_(currentInputField, ":focus"));
    }
    
    @Override
    public boolean isEditingText(Component c) {
        NativeOverlay overlay = (NativeOverlay)c.getNativeOverlay();
        if (overlay != null && jQuery_is_(overlay.el, ":focus")) {
            return true;
            
        }
        return currentEditingField == c && isEditing;
        //return super.isEditingText(c); //To change body of generated methods, choose Tools | Templates.
    }

    

    
    
    @Override
    public boolean isNativeEditorVisible(Component c) {
        NativeOverlay overlay = (NativeOverlay)c.getNativeOverlay();
        if (overlay != null && jQuery_is_(overlay.el, ":focus")) {
            return true;
        }
        return currentEditingField == c && isEditing;
    }
    
    private int lastEditorTop,lastEditorLeft,lastEditorWidth,lastEditorHeight;
    
    
    /**
     * Scales a coordinate from CN1 space to DOM one space.
     * @param x
     * @return 
     */
    public static int scaleCoord(int x) {
        return (int)(x / getDevicePixelRatio());
    }
    
    public static double scaleCoord(double x) {
        return x / getDevicePixelRatio();
    }
    
    
    
    /**
     * Scales a coordinate from DOM  space to the CN1 space.
     * @param x
     * @return 
     */
    public static int unscaleCoord(int x) {
        return (int)(x * getDevicePixelRatio());
    }
    
    private void resizeNativeEditor() {
        if (isEditing && currentInputField != null && currentEditingField != null) {
            HTMLInputElement inputEl = currentInputField;
            TextArea ta = currentEditingField;
            Component cmp = ta;
            Style taStyle = ta.getStyle();
            
            int paddingTop = taStyle.getPadding(Component.TOP);;
            int paddingLeft = taStyle.getPadding(ta.isRTL(), Component.LEFT);
            int paddingRight = taStyle.getPadding(ta.isRTL(), Component.RIGHT);
            int paddingBottom = taStyle.getPadding(Component.BOTTOM);
            
            int newTop = scaleCoord(cmp.getAbsoluteY()+cmp.getScrollY());
            int newLeft = scaleCoord(cmp.getAbsoluteX()+cmp.getScrollX());
            int newWidth = scaleCoord(cmp.getWidth()-paddingLeft-paddingRight);
            int newHeight = scaleCoord(cmp.getHeight()-paddingTop-paddingBottom);
            
            if (lastEditorTop != newTop || lastEditorLeft != newLeft || lastEditorWidth != newWidth || lastEditorHeight != newHeight) {
                //String msg = "Resizing editor from "+lastEditorTop+","+lastEditorLeft+","+lastEditorWidth+","+lastEditorHeight+" to "+newTop+","+newLeft+","+newWidth+","+newHeight;
                //consoleLog(msg);
                inputEl.getStyle().setProperty("padding-top", scaleCoord((double)paddingTop)+"px");
                inputEl.getStyle().setProperty("padding-left", scaleCoord((double)paddingLeft)+"px");
                inputEl.getStyle().setProperty("padding-bottom", scaleCoord((double)paddingBottom)+"px");
                inputEl.getStyle().setProperty("padding-right", scaleCoord((double)paddingRight)+"px");
                inputEl.getStyle().setProperty("top", newTop+"px");
                inputEl.getStyle().setProperty("left", newLeft+"px");
                inputEl.getStyle().setProperty("width", newWidth+"px");
                inputEl.getStyle().setProperty("height", newHeight+"px");
                inputEl.getStyle().setProperty("border", "none");
                inputEl.getStyle().setProperty("margin", "0");
                
                lastEditorTop = newTop;
                lastEditorLeft = newLeft;
                lastEditorWidth = newWidth;
                lastEditorHeight = newHeight;
            }
        }
    }
    
    public static Runnable wrapOnEdt(final Runnable r) {
        return new Runnable() {

            @Override
            public void run() {
                callSerially(r);
            }
            
        };
    }

    public Runnable wrapOnNativeQueue(final Runnable r) {
        return new Runnable() {

            @Override
            public void run() {
                nativeCallSerially(r);
                
            }
            
        };
    }
        
    /**
     * Flag to indicate whether we use the preemptive native text field 
     * approach for text editing.  With this approach we create the native 
     * text field and focus is as soon as a native touch event occurs over a text
     * field - ON THE NATIVE THREAD - rather than waiting for the editString() method
     * which is triggered too late, and thus prevented (by iOS) from focusing any
     * text fields programmatically.  This approach is a hack for iOS only.
     * @return 
     */
    private static boolean usePreemptiveNativeTextFieldApproach() {
        return isIOS();
    }
    
    // For iOS it will only show the "next" button if there is a tabbable
    // native text field that can be focused next.
    // This will be used to place such a field as a dummy
    private HTMLInputElement dummyNextTextField, dummyPrevTextField;
    
    // On iOS, we can only focus a text field in an event that was triggered
    // by the user... this can't happen on the EDT so by the time a tapped text field
    // event on the EDT is processed, it's too late to do any focusing.
    // So we create a native text field preemptively and focus it - this will
    // be used and resized appropriately by editString() on the EDT
    private HTMLInputElement preemptiveFocusTextField;
     
    @JSBody(params={"el"}, script="jQuery(el).on('touchstart.preemptiveFocus', "
            + "    function() { this.focus();}"
            + "); "
            + "jQuery(el).trigger('touchstart'); "
            + "jQuery(el).off('.preemptiveFocus')")
    native static void triggerFocusIOS(JSObject el);
    
    private void triggerFocusIOSTextField(final Component target, final Component cmp, final Component nextFocus) {
        
    }
    
    
    
    /**
     * On iOS we can only trigger a text field to focus programmatically inside a limited set of event
     * handlers.  We can't initiate this from the EDT because it is too far removed from the original
     * touch event that triggered the "TextArea.startEditing()" call.  To work around this,
     * we intercept all touchend events, and see if their (x,y) coordinates are over a text area.
     * 
     * We then immediately create a native text field and focus it (for the keyboard).  Then when the editString()
     * method is called later from the EDT, it looks for this text field and works with that, rather than trying
     * to create a new one at that time.
     * 
     * NOTE: We can't use any synchronous stuff for this because it runs directly on Javascript native.  This will
     * cause warnings during compilation because there are many java classes that *could* use synchronous stuff
     * but we just have to be sure that they don't.   E.g. TeaVM may think that any use of java.util.List is synchronous
     * because of the Vector class, but we just have to ensure that there isn't actually any synchronized, locking, etc..
     * going on.
     * @param cmp 
     */
    // This is run on the native thread.  
    private void triggerFocusIOSCmp(final Component target) {
        final Component cmp;
        Component nextFocus = (Component)target.getClientProperty("$$focus");
        cmp = nextFocus == null ? target : nextFocus;
        
        if (cmp instanceof TextArea && cmp.isEnabled() && cmp.isEditable() && cmp.getComponentForm() != null) {
            TextArea ta = (TextArea)cmp;
            final Form.TabIterator tabber = cmp.getComponentForm().getTabIterator(cmp);
            if (!ta.isSingleLineTextArea()){
                preemptiveFocusTextField = (HTMLInputElement)window.getDocument().createElement("textarea");


            } else {
                preemptiveFocusTextField = (HTMLInputElement)window.getDocument().createElement("input");
                preemptiveFocusTextField.setType("text");

            }
            preemptiveFocusTextField.setAttribute("class", "cn1-edit-string preemptive");
            preemptiveFocusTextField.setTabIndex(2);
            
            window.getDocument().getBody().appendChild(preemptiveFocusTextField);

            if (dummyNextTextField != null) {
                window.getDocument().getBody().removeChild(dummyNextTextField);
                dummyNextTextField = null;
            }
            if (tabber.hasNext()) {
                Component next = tabber.getNext();
                dummyNextTextField = (HTMLInputElement)window.getDocument().createElement("input");
                dummyNextTextField.setAttribute("class", "cn1-edit-string dummy-next");
                dummyNextTextField.getStyle().setProperty("pointer-events", "none");
                dummyNextTextField.getStyle().setProperty("opacity", "0");
                CSSStyleDeclaration s = dummyNextTextField.getStyle();

                s.setProperty("top", scaleCoord(next.getAbsoluteY()+next.getScrollY())+"px");
                s.setProperty("left", scaleCoord(next.getAbsoluteX()+next.getScrollX())+"px");
                s.setProperty("width", scaleCoord(next.getWidth())+"px");
                s.setProperty("height", scaleCoord(next.getHeight())+"px");
                s.setProperty("border", "none");
                s.setProperty("margin", "0");
                s.setProperty("outline", "none");  // for chrome
                dummyNextTextField.setType("text");
                
                dummyNextTextField.setTabIndex(3);
                dummyNextTextField.addEventListener("focus", new EventListener() {

                    @SuppressSyncErrors
                    @Override
                    public void handleEvent(Event evt) {
                        nextEditPending = false;
                        Form f = _getCurrent();
                        if (f != null) {
                            final Component next = tabber.getNext();
                            nextEditPending = next instanceof TextArea;
                            if (next != null) {
                                triggerFocusIOSCmp(next);
                                
                                callSerially(new Runnable() {

                                    @Override
                                    public void run() {
                                        
                                        next.requestFocus();
                                        next.startEditingAsync();
                                    }

                                });
                            }
                        }
                        if (!nextEditPending) {
                            outputCanvas.focus();
                            window.getDocument().getBody().removeChild(dummyNextTextField);
                            dummyNextTextField = null;
                        }
                    }

                });
                
                preemptiveFocusTextField.addEventListener("focus", new EventListener() {
                    @SuppressSyncErrors
                    @Override
                    public void handleEvent(Event evt) {
                        if (isIOS()) {
                            new Thread(new Runnable() {
                                    public void run() {
                                        Display.getInstance().fireVirtualKeyboardEvent(true);
                                    }
                            }).start();
                            
                        }
                    }
                });
                
                preemptiveFocusTextField.addEventListener("blur", new EventListener() {
                    @SuppressSyncErrors
                    @Override
                    public void handleEvent(Event evt) {
                        if (isIOS()) {
                            new Thread(new Runnable() {
                                    public void run() {
                                        Display.getInstance().fireVirtualKeyboardEvent(false);
                                    }
                            }).start();
                            
                        }
                        if (dummyNextTextField != null) {
                            Form f = _getCurrent();
                            if (f != null) {
                                final Component next = tabber.getNext();
                                if (next != null) {
                                    CSSStyleDeclaration s = dummyNextTextField.getStyle();

                                    s.setProperty("top", scaleCoord(next.getAbsoluteY()+next.getScrollY())+"px");
                                    s.setProperty("left", scaleCoord(next.getAbsoluteX()+next.getScrollX())+"px");
                                    s.setProperty("width", scaleCoord(next.getWidth())+"px");
                                    s.setProperty("height", scaleCoord(next.getHeight())+"px");
                                    s.setProperty("border", "none");
                                    s.setProperty("margin", "0");
                                    s.setProperty("outline", "none");  // for chrome
                                    _logBounds(dummyNextTextField);

                                }

                            }
                        }
                    }

                });
                window.getDocument().getBody().appendChild(dummyNextTextField);
            }
            
            //----- prev start
            
            if (dummyPrevTextField != null) {
                window.getDocument().getBody().removeChild(dummyPrevTextField);
                dummyPrevTextField = null;
            }
            if (tabber.hasPrevious()) {
                Component prev = tabber.getPrevious();
                dummyPrevTextField = (HTMLInputElement)window.getDocument().createElement("input");
                dummyPrevTextField.setAttribute("class", "cn1-edit-string dummy-prev");
                dummyPrevTextField.getStyle().setProperty("pointer-events", "none");
                dummyPrevTextField.getStyle().setProperty("opacity", "0");
                CSSStyleDeclaration s = dummyPrevTextField.getStyle();

                s.setProperty("top", scaleCoord(prev.getAbsoluteY()+prev.getScrollY())+"px");
                s.setProperty("left", scaleCoord(prev.getAbsoluteX()+prev.getScrollX())+"px");
                s.setProperty("width", scaleCoord(prev.getWidth())+"px");
                s.setProperty("height", scaleCoord(prev.getHeight())+"px");
                s.setProperty("border", "none");
                s.setProperty("margin", "0");
                s.setProperty("outline", "none");  // for chrome
                dummyPrevTextField.setType("text");
                
                dummyPrevTextField.setTabIndex(1);
                dummyPrevTextField.addEventListener("focus", new EventListener() {

                    @SuppressSyncErrors
                    @Override
                    public void handleEvent(Event evt) {
                        prevEditPending = false;
                        Form f = _getCurrent();
                        if (f != null) {
                            final Component prev = tabber.getPrevious();
                            prevEditPending = prev instanceof TextArea;
                            if (prev != null) {
                                triggerFocusIOSCmp(prev);
                                
                                callSerially(new Runnable() {

                                    @Override
                                    public void run() {
                                        
                                        prev.requestFocus();
                                        prev.startEditingAsync();
                                    }

                                });
                            }
                        }
                        if (!prevEditPending) {
                            outputCanvas.focus();
                            window.getDocument().getBody().removeChild(dummyPrevTextField);
                            dummyPrevTextField = null;
                        }
                    }

                });
                preemptiveFocusTextField.addEventListener("blur", new EventListener() {
                    @SuppressSyncErrors
                    @Override
                    public void handleEvent(Event evt) {
                        if (dummyPrevTextField != null) {
                            Form f = _getCurrent();
                            if (f != null) {
                                final Component prev = tabber.getPrevious();
                                if (prev != null) {
                                    CSSStyleDeclaration s = dummyPrevTextField.getStyle();

                                    s.setProperty("top", scaleCoord(prev.getAbsoluteY()+prev.getScrollY())+"px");
                                    s.setProperty("left", scaleCoord(prev.getAbsoluteX()+prev.getScrollX())+"px");
                                    s.setProperty("width", scaleCoord(prev.getWidth())+"px");
                                    s.setProperty("height", scaleCoord(prev.getHeight())+"px");
                                    s.setProperty("border", "none");
                                    s.setProperty("margin", "0");
                                    s.setProperty("outline", "none");  // for chrome

                                }

                            }
                        }
                    }

                });
                window.getDocument().getBody().appendChild(dummyPrevTextField);
            }
            
            //----- prev end
            
            
            CSSStyleDeclaration st = preemptiveFocusTextField.getStyle();

            st.setProperty("top", scaleCoord(cmp.getAbsoluteY()+cmp.getScrollY())+"px");
            st.setProperty("left", scaleCoord(cmp.getAbsoluteX()+cmp.getScrollX())+"px");
            st.setProperty("width", scaleCoord(cmp.getWidth())+"px");
            st.setProperty("height", scaleCoord(cmp.getHeight())+"px");
            _logBounds(preemptiveFocusTextField);
            triggerFocusIOS(preemptiveFocusTextField);
        }
    }
    
    @JSBody(params={"el"}, script="console.log(el.getBoundingClientRect());")
    native static void _logBounds(HTMLInputElement el);
    
    // This is run on the native thread
    private void createAndFocusTextFieldPreemptively(int x, int y) {
        if (usePreemptiveNativeTextFieldApproach()) {
            Form f = _getCurrent();
            if (f != null) {
                triggerFocusIOSCmp(f.getComponentAt(x, y));
            }
            
        }
    }

    @Override
    public void stopTextEditing() {
        if (isEditing){
            if (currentEditingField != null) {
                pendingTextChanges = currentEditingField.getText();
            }
            if (!editingStartingUp) {
                finishTextEditing();
            } else {
                editingStartingUp = false;
            }
        }
    }

    private NativePicker activePicker;
    
    @Override
    public Object showNativePicker(int type, Component source, Object currentValue, Object data) {
        if (!isNativePickerTypeSupported(type)) {
            return super.showNativePicker(type, source, data, data);
        }
        if (activePicker != null) {
            throw new IllegalStateException("Attempt to show native picker while another picker is still active.");
        }
        activePicker = NativePicker.createNativePicker(type, source, currentValue, data);
        try {
            return activePicker.show();
        } finally {
            activePicker = null;
        }
        
    }

    @Override
    public boolean isNativePickerTypeSupported(int pickerType) {
        return NativePicker.isNativePickerTypeSupported(pickerType);
    }

    
    
    
    private HTMLInputElement inputEl;
    private String text;
    private DataChangedListener dataChangedListener;
    private Runnable editingCompleteCallback;
    private boolean nextEditPending, prevEditPending;
    
    
    @Override
    public void editString(final Component cmp, int maxSize, int constraint, final String origText, int initiatingKeycode) {
        if (cmp.getNativeOverlay() != null) {
            // If a native overlay exists then just use that native overlay
            NativeOverlay overlayEl = (NativeOverlay)cmp.getNativeOverlay();
            overlayEl.el.focus();
            return;
        }
        if (usePreemptiveNativeTextFieldApproach() && preemptiveFocusTextField == null) {
            // On iOS we depend on the text field to have been set up in the touchend native
            // event.  If it isn't set up, then we won't proceed.
            return;
        }
        if (isEditing) {
            return;
        }
        text = origText;
        isEditing=true;
        
        // This Hack is specifically to work around an issue on iOS Safari
        // where the "touch" event circumvents the editing process, causing
        // the user to tap 3 times to edit a text field.  With this hack
        // we have it down to 2 taps.
        editingStartingUp = true;
        
        Window.setTimeout(new TimerHandler() {

            @Override
            public void onTimer() {
                editingStartingUp = false;
            }
            
        }, 3000);
        inputEl = null;
        
        final Runnable cleanup = new Runnable() {
            public void run() {
                if (inputEl != null) {
                    // Hide the text field.  Don't get crafty and try to
                    // remove it due to bug in Chrome
                    // https://github.com/shannah/cn1-teavm-port/issues/31
                    inputEl.getStyle().setProperty("display", "none");
                    if ("password".equalsIgnoreCase(inputEl.getAttribute("type"))) {
                        inputEl.setAttribute("type", "text");
                    }
                    //outputCanvas.focus();
                    inputEl = null;
                }
                isEditing = false;
                Display.getInstance().onEditingComplete(cmp, text);
                if (doneEventFired && cmp instanceof TextArea) {
                    ((TextArea)cmp).fireDoneEvent();
                }
                if (tabNext) {
                    Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        Component focused = f.getNextComponent(cmp);
                        
                        if (focused != null) {
                            if (!(focused instanceof TextArea)) {
                                final Component fFocused = focused;
                                UITimer.timer(300, false, new Runnable() {
                                    public void run() {
                                        // This delay is necessary on Android 
                                        // to give it time to close the keyboard
                                        fFocused.requestFocus();
                                        fFocused.startEditingAsync();
                                        outputCanvas.focus();
                                        
                                    }
                                });
                                
                            } else {
                                focused.requestFocus();
                                focused.startEditingAsync();
                            }
                            
                        }

                    }
                } else if (tabPrev) {
                    Form f = Display.getInstance().getCurrent();
                    if (f != null) {

                        Component focused = f.getPreviousComponent(cmp);
                        
                        if (focused != null) {
                            if (!(focused instanceof TextArea)) {
                                final Component fFocused = focused;
                                UITimer.timer(300, false, new Runnable() {
                                    public void run() {
                                        // This delay is necessary on Android 
                                        // to give it time to close the keyboard
                                        fFocused.requestFocus();
                                        fFocused.startEditingAsync();
                                        outputCanvas.focus();
                                        
                                    }
                                });
                            } else {
                                focused.requestFocus();
                                focused.startEditingAsync();
                            }
                        }

                    }
                }
                doneEventFired = false;
                tabNext = false;
                tabPrev = false;
                pendingTextChanges = null;
            }
        };
        
        try {
            
            if (cmp == null) {

                throw new IllegalArgumentException("component is null");
            }

            if (!(cmp instanceof TextArea)) {
                throw new IllegalArgumentException("component must be instance of TextArea");
            }

            final TextArea ta = (TextArea)cmp;
            
            inputEl = ta.isSingleLineTextArea() ? textField : textArea;
            isEditingSingleLine = ta.isSingleLineTextArea();
            final boolean hasDoneListener = ta.getDoneListener() != null;
            if (inputEl == null || preemptiveFocusTextField != null) {
                if (preemptiveFocusTextField != null && inputEl != null) {
                    window.getDocument().getBody().removeChild(inputEl);
                }
                if (!ta.isSingleLineTextArea()){
                    if (preemptiveFocusTextField != null) {
                        inputEl = textArea = preemptiveFocusTextField;
                    } else {
                        inputEl = textArea = (HTMLInputElement)window.getDocument().createElement("textarea");
                    }
                    

                } else {
                    if (preemptiveFocusTextField != null) {
                        inputEl = textField = preemptiveFocusTextField;
                    } else {
                        inputEl = textField = (HTMLInputElement)window.getDocument().createElement("input");
                        inputEl.setType("text");
                    }
                    
                }
                
                
                inputEl.setAttribute("class", "cn1-edit-string");
                
                inputEl.addEventListener("keydown", new EventListener() {

                    @Override
                    public void handleEvent(final Event evt) {
                        final KeyEvent kevt = (KeyEvent)evt;
                        switch (((KeyEvent)evt).getKeyCode()) {
                            case 9 :
                            case 11 :
                            case 10 :
                            case 13 :
                                if (isEditingSingleLine || hasDoneListener || kevt.getKeyCode() == 9 || kevt.getKeyCode() == 11) {
                                    evt.preventDefault();
                                    evt.stopPropagation();
                                    nativeCallSerially(new Runnable() {
                                        public void run() {
                                            lastCharCode = 0;
                                        }
                                    });
                                    
                                }
                                
                                break;
                        }
                        callSerially(new Runnable() {
                            public void run() {
                                
                                switch (kevt.getKeyCode()) {
                                    case 9 : // tab
                                    case 11 : // vertical tab
                                    case 10 : // lf
                                    case 13 : // cr
                                    {
                                        if (!(isEditingSingleLine || hasDoneListener) && kevt.getKeyCode() != 9 && kevt.getKeyCode() != 11) {
                                            // We don't do any special handling for multiline text fields.
                                            return;
                                        }
                                        doneEventFired = true;
                                        boolean isNextButton = false;
                                        if (isPhoneOrTablet_()) {
                                            if (currentEditingField != null) {
                                                Form f = currentEditingField.getComponentForm();
                                                if (f != null) {
                                                    Component next = f.getNextComponent(currentEditingField);
                                                    if (next != null) {
                                                        isNextButton = true;
                                                    }
                                                }
                                            }
                                        }
                                        if (kevt.getKeyCode() == 9 || kevt.getKeyCode() == 11 || isNextButton ) {
                                            tabNext = !kevt.isShiftKey();
                                            tabPrev = !tabNext;
                                        }
                                        if (currentEditingField != null) {
                                            pendingTextChanges = currentEditingField.getText();
                                        }
                                        finishTextEditing();
                                    }
                                }
                                
                            }
                        });
                    }
                    
                });
                
                if (preemptiveFocusTextField == null) {
                    window.getDocument().getBody().appendChild(inputEl);
                } else {
                    preemptiveFocusTextField = null;
                }
            }
            currentInputField = inputEl;
            currentEditingField = ta;
            // Show the text field... 
            inputEl.getStyle().setProperty("display", "block");
            
            inputEl.setAttribute("maxlength", maxSize+"");
            inputEl.getStyle().setProperty("font", ((NativeFont)cmp.getStyle().getFont().getNativeFont()).getScaledCSS());
            inputEl.getStyle().setProperty("color", HTML5Graphics.color(cmp.getStyle().getFgColor()));

            final Style taStyle = ta.getStyle();
            Font font = taStyle.getFont();
            //int txty = ta.getAbsoluteY();
            //int txtx = ta.getAbsoluteX();
            int paddingTop = taStyle.getPadding(Component.TOP);;
            int paddingLeft = taStyle.getPadding(ta.isRTL(), Component.LEFT);
            int paddingRight = taStyle.getPadding(ta.isRTL(), Component.RIGHT);
            int paddingBottom = taStyle.getPadding(Component.BOTTOM);

            if (ta.isSingleLineTextArea()) {

                switch (ta.getVerticalAlignment()) {
                    case Component.BOTTOM:
                        paddingTop = ta.getHeight() - taStyle.getPadding(false, Component.BOTTOM) - font.getHeight();
                        break;
                    case Component.CENTER:
                        paddingTop = ta.getHeight() / 2 - font.getHeight() / 2;
                        break;
                    default:
                        paddingTop = taStyle.getPadding(false, Component.TOP);
                        break;
                }
            } else {
                paddingTop = taStyle.getPadding(false, Component.TOP);
            }

            inputEl.getStyle().setProperty("padding-top", scaleCoord((double)paddingTop)+"px");
            inputEl.getStyle().setProperty("padding-left", scaleCoord((double)paddingLeft)+"px");
            inputEl.getStyle().setProperty("padding-bottom", scaleCoord((double)paddingBottom)+"px");
            inputEl.getStyle().setProperty("padding-right", scaleCoord((double)paddingRight)+"px");
            inputEl.getStyle().setProperty("top", scaleCoord((double)(cmp.getAbsoluteY()+cmp.getScrollY()))+"px");
            inputEl.getStyle().setProperty("left", scaleCoord((double)(cmp.getAbsoluteX()+cmp.getScrollX()))+"px");
            inputEl.getStyle().setProperty("width", scaleCoord((double)(cmp.getWidth()-paddingLeft-paddingRight))+"px");
            inputEl.getStyle().setProperty("height", scaleCoord((double)(cmp.getHeight()-paddingTop-paddingBottom))+"px");
            inputEl.getStyle().setProperty("border", "none");
            inputEl.getStyle().setProperty("margin", "0");
            inputEl.getStyle().setProperty("outline", "none");  // for chrome
            
            int cnst = ta.getConstraint();
            String inputType = "text";
            if (ta.isSingleLineTextArea()) {
                
                switch (cnst) {
                    case TextArea.PASSWORD:
                        inputType = "password";
                        break;
                    case TextArea.EMAILADDR:
                        inputType = "email";
                        break;
                    case TextArea.NUMERIC:
                        inputType = "number";
                        break;
                    case TextArea.PHONENUMBER:
                        inputType = "tel";
                        break;
                    case TextArea.URL:
                        inputType = "url";
                        break;
                    
                }
                inputEl.setAttribute("type", inputType);
                
                
            }
            


            boolean valid = 
                (initiatingKeycode > 47 && initiatingKeycode < 58)   || // number keys
                initiatingKeycode == 32 || initiatingKeycode == 13   || // spacebar & return key(s) (if you want to allow carriage returns)
                (initiatingKeycode > 64 && initiatingKeycode < 91)   || // letter keys
                (initiatingKeycode > 95 && initiatingKeycode < 112)  || // numpad keys
                (initiatingKeycode > 185 && initiatingKeycode < 193) || // ;=,-./` (in order)
                (initiatingKeycode > 218 && initiatingKeycode < 223);   // [\]' (in order)
            
            switch (initiatingKeycode) {
                case 8 : { // backspace 
                    if (valid && text.length() > 0) {
                        text = text + (char)initiatingKeycode;
                    }
                    break;
                }

                case 10 :
                case 13 : { // newline
                    if (ta.isSingleLineTextArea()) {
                        Display.getInstance().onEditingComplete(cmp, text);
                        isEditing = false;
                        return;
                    } else if (valid) {
                        text = text + (char) initiatingKeycode;
                    }
                    break;
                }

                case 0 : // Null char
                    break;
                default : {
                    if (valid) {
                        text = text + (char) initiatingKeycode;
                    }
                }


            }
            


            inputEl.setValue(text);
            if ("text".equals(inputType)) {
                ((TextElement)inputEl).setSelectionRange(text.length(), text.length());
            }
            
            
            Form currentForm = Display.getInstance().getCurrent();
            if (currentForm != null) {
                if (currentForm.getFocused() != ta) {
                    ta.requestFocus();
                }
            }
            
            final FocusListener focusListener = new FocusListener() {

                @Override
                public void focusGained(Component cmpnt) {
                    
                }

                @Override
                public void focusLost(Component cmpnt) {
                    
                    finishTextEditing();
                }
                
            };
            
            
            
            dataChangedListener = null;
            
            
            final HTMLInputElement el = inputEl;
            dataChangedListener = new DataChangedListener() {

                @Override
                public void dataChanged(int i, int i1) {
                    String val = ta.getText();
                    if (val != null && !val.equals(el.getValue())) {
                        //lastCN1InputTime = System.currentTimeMillis();
                        el.setValue(val);
                        finishTextEditing();
                    }
                }
            };
            ta.addDataChangedListener(dataChangedListener);
            
            
            ta.addFocusListener(focusListener);
            
            
            
            ta.repaint();
            
            doneEventFired = false;
            tabNext = false;
            tabPrev = false;
            
            final HTMLInputElement finalInputEl = inputEl;
            // We need to resize the canvas whenever the soft keyboard is shown
            
            //inputEl.blur();
            
            // A runnable to help re-layout the form and text field to deal with
            // changes outside of our control (e.g. virtual keyboards causeing
            // scrolling or resizing.
            final Runnable layoutForm = new Runnable() {
                @Override
                public void run() {

                    //safeSleep(100);
                    // On iOS and Android the VKB causes the browser to scroll
                    // down to the field that is being edited if it would be 
                    // covered by the keyboard.
                    // We detect the scroll position and then add appropriate
                    // padding to the bottom of the form... then scroll up to the
                    // top again to compensate.
                    vkbHeight = getScrollY_();
                    Form current = Display.getInstance().getCurrent();
                    if (!current.isFormBottomPaddingEditingMode()) {
                        //We only re-layout the form if form bottom padding is enabled
                        return;
                    }
                    current.getContentPane().getUnselectedStyle().setPaddingUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});
                    current.getContentPane().getUnselectedStyle().setPadding(Component.BOTTOM, unscaleCoord(vkbHeight));


                    Display.getInstance().callSerially(new Runnable() {

                        @Override
                        public void run() {
                            Display.getInstance().getCurrent().forceRevalidate();
                            finalInputEl.getStyle().setProperty("top", scaleCoord(cmp.getAbsoluteY()+cmp.getScrollY())+"px");
                            finalInputEl.getStyle().setProperty("left", scaleCoord(cmp.getAbsoluteX()+cmp.getScrollX())+"px");
                            //safeSleep(100);
                            scrollToY(0);
                            Display.getInstance().getCurrent().forceRevalidate();
                        }

                    });


                }
            };
        
            // A handler for the input into the text field to fire data change listeners etc..
            EditingInputHandler inputHandler = new EditingInputHandler(ta, inputEl, layoutForm);
            
            final JSFunction inputHandle = EventUtil.addEventListener(inputEl, "input", inputHandler);
            
            // We need to listen for resize events because some platforms (e.g. MS Surface)
            // will resize the window to show the VKB.
            final JSFunction resizeHandle = EventUtil.addEventListener(window, "resize", new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    callSerially(new Runnable() {
                        public void run() {
                            safeSleep(50);
                            layoutForm.run();
                        }
                    });
                }
                
            });
            
            // On some platforms (Android and iOS) it can be very hard to 
            // track down the final scroll position of the page as a result
            // of showing the VKB.  For this reason we set an interval to 
            // continually check the scroll and focus every 100ms and 
            // adjust the layout accordingly.
            final int[] intervalCounter = new int[1];
            
            
            
            final int intervalHandle = Window.setInterval(new TimerHandler() {
                @Override
                public void onTimer() {
                    
                    if (!jQuery_is_(finalInputEl, ":focus")) {
                        finalInputEl.focus();
                    }
                    
                    if (getScrollY_() > 1) {
                        callSerially(layoutForm);
                    }
                    
                }
            }, 100);
            
            // We only want the interval to run for a maximum of 2 seconds
            // since that should be sufficient to detect any scroll changes
            // as a result of the VKB being shown.  Set a timer for
            // 2 seconds which just clears the interval.
            final boolean[] intervalCleared = new boolean[1];
            Window.setTimeout(new TimerHandler() {
                public void onTimer() {
                    if (!intervalCleared[0]) {
                        intervalCleared[0] = true;
                        Window.clearInterval(intervalHandle);
                    }
                }
            }, 2000);
            
            // In order to force the VKB to appear on touch devices we'll force
            // the input field to focus.
            // THIS DOESN' WORK ON IOS SAFARI since you can only programmatically
            // focus a field as a result of a user event....  But it does work
            // in every other browser/platform.
            // On iOS the user has to click the field twice for now.... once
            // to make it visible, and a second time to focus it.
            inputEl.focus();
            
            // This is where we block the EDT and wait for the editing lock to be
            // released.
            
            
            
            editingCompleteCallback = new Runnable() {
                public void run() {
                    try {     
                        // Fix for https://github.com/shannah/cn1-teavm-port/issues/48
                        // on iPad the invisible input field may retain focus and we can't
                        // seem to return focus to the canvas.
                        if (!usePreemptiveNativeTextFieldApproach() || (!nextEditPending && !prevEditPending)) {
                            
                            inputEl.blur();
                            outputCanvas.focus();
                        } else {
                            nextEditPending = false;
                            prevEditPending = false;
                        }

                        // Remove all of the event listeners that we added to the input field
                        // and text field before blocking.
                        
                        EventUtil.removeEventListener(inputEl, "input", inputHandle);
                        //EventUtil.removeEventListener(inputEl, "blur", blurHandle);
                        //EventUtil.removeEventListener(inputEl, "focus", focusHandle);
                        //EventUtil.removeEventListener(inputEl, "click", clickHandle);
                        EventUtil.removeEventListener(window, "resize", resizeHandle);
                        if (!intervalCleared[0]) {
                            intervalCleared[0] = true;
                            Window.clearInterval(intervalHandle);
                        }

                        ta.removeFocusListener(focusListener);
                        if (dataChangedListener != null && ta instanceof TextField) {
                            ((TextField)ta).removeDataChangeListener(dataChangedListener);
                        }

                        //if (lastHTMLInputTime > lastCN1InputTime) {
                            text = inputEl.getValue();
                        //} else {
                        //    text = ta.getText();
                        //}


                        Form current = Display.getInstance().getCurrent();
                        current.getContentPane().getUnselectedStyle().setPaddingUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});
                        current.getContentPane().getUnselectedStyle().setPadding(Component.BOTTOM, 0);
                        current.forceRevalidate();


                        if (pendingTextChanges != null && !pendingTextChanges.equals(ta.getText())) {
                            pendingTextChanges = null;
                            text = ta.getText();
                        }

                    } finally {
                        cleanup.run();
                    }
                }
            };
            /*
            Display.getInstance().invokeAndBlock(new Runnable(){

                @Override
                public void run() {
                    while (isEditing){
                        synchronized (editingLock){
                            try {
                                editingLock.wait(1000);
                            } catch (InterruptedException ex) {
                                //Log.e(ex);
                            }
                        }
                    }
                }

            });
            */
        } catch (Throwable t) {
            cleanup.run();
        }
            
        
        
    }
    
    private int inputIdCounter=0;
    
    public static HTML5Implementation getInstance() {
        return instance;
    }

    @Override
    public PeerComponent createNativePeer(Object nativeComponent) {
        return new HTML5Peer((HTMLElement)nativeComponent);
    }
    
    
    
    private static class EditingInputHandler implements EventListener {
        
        private final TextArea ta;
        private final HTMLInputElement el;
        private final int id;
        private Runnable layoutForm;
        
        EditingInputHandler(TextArea ta, HTMLInputElement el, Runnable layoutForm) {
            this.ta = ta;
            this.el = el;
            this.id = instance.inputIdCounter++;
            this.layoutForm = layoutForm;
        }
        
        @Override
        public void handleEvent(Event evt) {
            //instance.lastHTMLInputTime = System.currentTimeMillis();
            callSerially(new Runnable() {
                public void run() {
                    if (!ta.hasFocus()) {
                        // As long as we're typing in the field
                        // The CN1 text field should have focus.
                        ta.requestFocus();
                    }
                    String val = el.getValue();
                    if (val != null && !val.equals(ta.getText())) {
                        ta.setText(el.getValue());
                    }
                }
            });
            /*
            if (getScrollY_() > 1) {
                callSerially(layoutForm);
            }
            */
        }
        
    }
    /**
     * Flag to indicate whether we use overlay text fields on touch devices rather than
     * the "old" way of creating a native overlay on demand.
     * 
     * NOTE: Native overlays are completely disabled right now as they introduced problems.
     */
    private boolean useNativeOverlaysForTextFieldsOnTouchDevices=false;
    
    private boolean useNativeOverlaysForTextFields() {
        return useNativeOverlaysForTextFieldsOnTouchDevices && isPhoneOrTablet_();
    }
    
    private void finishTextEditing(){
        if (!useNativeOverlaysForTextFields()) {
            if (editingCompleteCallback != null) {
                Display.getInstance().callSerially(editingCompleteCallback);
                editingCompleteCallback = null;
            } else {
                isEditing=false;
            }
        }
        
    }
    
    /**
     * Since we are using requestAnimationFrame() we are running
     * the graphics output on a native thread so we don't have regular
     * locking... we need to make sure that they don't conflict.
     */
    boolean graphicsLocked;
    
    @Override
    public void flushGraphics(int x, int y, int width, int height) {
        JavaScriptRenderQueueCoordinator.waitUntilFlushable(new JavaScriptRenderQueueCoordinator.FlushBarrier() {
            @Override
            public boolean isGraphicsLocked() {
                return graphicsLocked;
            }

            @Override
            public void sleep(int millis) throws InterruptedException {
                Thread.sleep(millis);
            }
        }, pendingDisplay);
        
        List<ExecutableOp> flushedOps;
        synchronized(pendingDisplay){
            /*
            CanvasRenderingContext2D context = (CanvasRenderingContext2D)outputCanvas.getContext("2d");
            List<ExecutableOp> ops = graphics.flush(x, y, width, height);
            for (ExecutableOp op : ops){
                op.execute(context);
            }
            */
            flushedOps = graphics.flush(x, y, width, height);
            JavaScriptRenderQueueCoordinator.queueFlush(new JavaScriptRenderQueueCoordinator.GraphicsLock() {
                @Override
                public void setGraphicsLocked(boolean locked) {
                    graphicsLocked = locked;
                }
            }, pendingDisplay, flushedOps, x, y, width, height);
        }
        drainPendingDisplayFrame();
        if (isEditing) {
            resizeNativeEditor();
        }
        if (activePicker != null) {
            activePicker.resizeNativeElement();
        }
    	
       
    }

    @Override
    public void flushGraphics() {
    	flushGraphics(0,0,canvas.getWidth(), canvas.getHeight());
        
    }

    @Override
    public void screenshot(SuccessCallback<Image> callback) {
        if (callback == null) {
            return;
        }
        if (outputCanvas == null) {
            super.screenshot(callback);
            return;
        }
        flushGraphics();
        final int width = outputCanvas.getWidth();
        final int height = outputCanvas.getHeight();
        if (width <= 0 || height <= 0) {
            super.screenshot(callback);
            return;
        }
        drainPendingDisplayFrame();
        final CanvasRenderingContext2D context = (CanvasRenderingContext2D) outputCanvas.getContext("2d");
        final ImageData imageData = context.getImageData(0, 0, width, height);
        final Uint8ClampedArray data = imageData.getData();
        final int[] rgb = new int[width * height];
        JavaScriptImageDataAdapter.readRgbaToArgb(new JavaScriptImageDataAdapter.PixelReader() {
            @Override
            public int get(int index) {
                return data.get(index);
            }

            @Override
            public int length() {
                return data.getLength();
            }
        }, rgb, 0);
        callback.onSucess(Image.createImage(rgb, width, height));
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
    	NativeImage im = (NativeImage)nativeImage;
        if (im.img != null && !im.loaded) {
            im.load();
        }
        final ImageData[] imData = new ImageData[1];
        JavaScriptNativeImageAdapter.readPixels(im.getImageModel(), new JavaScriptNativeImageAdapter.PixelReadTarget() {
            @Override
            public void readLoadedImage() {
                imData[0] = renderingBackend.readLoadedImageData(im.img, x, y, width, height);
            }

            @Override
            public void readMutableSurface() {
                imData[0] = renderingBackend.readMutableSurfaceData(im.mutableGraphics.getCanvas(), x, y, width, height);
            }
        });
        if (imData[0] == null) {
            throw new RuntimeException("Failed to get RGB data.  Image not loaded " + nativeImage);
        }

        final Uint8ClampedArray dataArr = imData[0].getData();
        JavaScriptImageDataAdapter.readRgbaToArgb(new JavaScriptImageDataAdapter.PixelReader() {
            @Override
            public int get(int index) {
                return dataArr.get(index);
            }

            @Override
            public int length() {
                return dataArr.getLength();
            }
        }, arr, offset);
            
        
    }

    

    @Override
    public LocationManager getLocationManager() {
        return new HTML5LocationManager();
    }
    
    
    
    @Override
    public ImageIO getImageIO() {
        return new ImageIO(){

            @Override
            public void save(InputStream image, OutputStream response, String format, int width, int height, float quality) throws IOException {
                Image img = Image.createImage(image).scaled(width, height);
                if (width < 0) {
                    width = img.getWidth();
                }
                if (height < 0) {
                    height = img.getHeight();
                }
                //NativeImage nimg = (NativeImage)createImage(image);
                saveImage(img, response, format, quality);
            }

            private void saveImage(NativeImage nimg, OutputStream response, String format, int width, int height, float quality) throws IOException {
                HTMLCanvasElement canvas = renderingBackend.createCanvas(width, height);
                CanvasRenderingContext2D ctx = renderingBackend.getContext(canvas);
                nimg.draw(ctx, 0, 0, width, height);
                Blob blob = renderingBackend.toImageBlob(canvas, "image/"+format, quality);
                InputStream blobInput = BlobUtil.openInputStream(blob);
                Util.copy(blobInput, response);
                Util.cleanup(blobInput);
                
                
            }
            
            @Override
            protected void saveImage(Image image, OutputStream response, String format, float quality) throws IOException {
                
                saveImage((NativeImage)image.getImage(), response, format, image.getWidth(), image.getHeight(), quality);
            }

            @Override
            public boolean isFormatSupported(String format) {
                return ImageIO.FORMAT_JPEG.equals(format) || ImageIO.FORMAT_PNG.equals(format);
            }
            
        };
    }

    
    
    @Override
    public Object createImage(int[] rgb, int width, int height) {
        NativeImage img = new NativeImage();
        ImageData data = (ImageData)createImageData(rgb, width, height);
        JavaScriptCanvasImageBufferLifecycle.CanvasImageBuffer<HTMLCanvasElement, HTML5Graphics> buffer =
                JavaScriptCanvasImageBufferLifecycle.createBlankBuffer(width, height,
                        new JavaScriptCanvasImageBufferLifecycle.SizedCanvasFactory<HTMLCanvasElement>() {
                            @Override
                            public HTMLCanvasElement createCanvas(int canvasWidth, int canvasHeight) {
                                return renderingBackend.createCanvas(canvasWidth, canvasHeight);
                            }
                        }, new JavaScriptCanvasImageBufferLifecycle.GraphicsFactory<HTMLCanvasElement, HTML5Graphics>() {
                            @Override
                            public HTML5Graphics createGraphics(HTMLCanvasElement canvas) {
                                return renderingBackend.createGraphics(HTML5Implementation.this, canvas);
                            }

                            @Override
                            public void fillRect(HTML5Graphics graphics, int fillColor, int fillWidth, int fillHeight) {
                            }
                        });
        attachMutableImageSurface(img, buffer.getGraphics());
        renderingBackend.writeImageData(buffer.getCanvas(), data, width, height);
        //System.out.println("Created image from rgb "+img);
        
        return img;
        
    }

    @Override
    public boolean isAlphaGlobal() {
        return true;
    }

    @Override
    public boolean isAlphaMutableImageSupported() {
        return true;
    }
    
    
    
    
    
    private Object createImageData(int[] rgb, int width, int height){
        return createImageData(rgb, 0, width, height);
    }
    
    Object createImageData(int[] rgb, int offset, int width, int height) {
        final Uint8ClampedArray arr = Uint8ClampedArray.create(width*height*4);
        JavaScriptImageDataAdapter.writeArgbToRgba(rgb, offset, width, height, new JavaScriptImageDataAdapter.PixelWriter() {
            @Override
            public void set(int index, int value) {
                arr.set(index, value);
            }
        });
        ImageData d = graphics.getContext().createImageData(width, height);
        ((Uint8ClampedArraySetter)d.getData()).set(arr);
        return d;
        
    }

    private int isTablet = -1;

    @Override
    public boolean isTablet() {

        if (isTablet == -1) {
            String overrideVal = getParameterByName("isTablet");
            if ("1".equals(overrideVal)) {
                isTablet = 1;
            } else if ("0".equals(overrideVal)) {
                isTablet = 0;
            } else if (isPhone_()) {
                isTablet = 0;
            } else {
                // The mobile-browser regex above leaves headless Chromium
                // (and any desktop browser opened at a phone-sized viewport)
                // classified as a tablet. That kicks widgets into their
                // tablet layout path — Picker's tablet branch in particular
                // wraps the Spinner3D in PickerDialogTablet, which inflates
                // to fill the screen and pushes the date wheels off-canvas
                // at narrow viewports. Fall back to Material Design's sw600
                // breakpoint on the shortest viewport side in CSS px. The
                // display dimensions are already in native pixels so divide
                // by DPR to compare against a CSS-pixel threshold (without
                // this divide, a 375x667 retina viewport reports 750x1334
                // and trips the sw600 gate).
                double dpr = getDevicePixelRatio();
                if (dpr <= 0) dpr = 1.0;
                int minSide = (int) (Math.min(getDisplayWidth(), getDisplayHeight()) / dpr);
                isTablet = minSide >= 600 ? 1 : 0;
            }
        }
        return isTablet==1;
    }
    

    @JSBody(params={"url", "target"}, script="window.open(url, target)")
    private native static void windowOpen(String url, String target);

   
    @JSBody(params={"fileName", "blob"}, script="window.cn1SaveBlobHandler = function() {if (window.navigator && window.navigator.msSaveOrOpenBlob) {\n" +
        "    window.navigator.msSaveOrOpenBlob(blob, fileName);\n" +
        "}\n" +
        "else {\n" +
        "    var downloadLink = document.createElement('a');"
            + "downloadLink.href =  URL.createObjectURL(blob);\n"
            + "downloadLink.download = fileName;"
            + "document.body.appendChild(downloadLink);" +
        "    downloadLink.click();"
            + "document.body.removeChild(downloadLink);\n"
            + "window.cn1SaveBlobHandler = null;" +
        "}};")
    private static native void registerSaveBlobHandler(String fileName, Blob blob);
    
    @JSBody(params={"fileName", "dataUrl"}, script="window.cn1SaveBlobHandler = function() {if (window.navigator && window.navigator.msSaveOrOpenBlob) {\n" +
        "    var blob = window.Base64ToBlob(dataUrl);"
            + "window.navigator.msSaveOrOpenBlob(blob, fileName);\n" +
        "}\n" +
        "else {\n" +
        "    var downloadLink = document.createElement('a');"
            + "downloadLink.href =  dataUrl;\n"
            + "downloadLink.download = fileName;"
            + "document.body.appendChild(downloadLink);" +
        "    downloadLink.click();"
            + "document.body.removeChild(downloadLink);\n"
            + "window.cn1SaveBlobHandler = null;" +
        "}};")
    private static native void registerSaveBlobHandlerDataUrl(String fileName, String dataUrl);
    
    @JSBody(params={}, script="window.cn1SaveBlobHandler = null;")
    private static native void deregisterSaveBlobHandler();
    
    @JSBody(params={}, script="if (window.cn1SaveBlobHandler) window.cn1SaveBlobHandler();")
    private static native void fireSaveBlobHandler();
    
    
    public boolean paintNativePeersBehind() {
        return true;
    }

    @JSBody(params={"js"}, script="eval(js)")
    private native static void eval_(String js);
    
    @Override
    public void execute(String url) {
        
        if (url.startsWith("javascript:")) {
            String cmd = url.substring(url.indexOf(":")+1);
            eval_(cmd);
            return;
        }
        
        String fileName = null;
        boolean useBlobHandler = false;
        Button nativeButton = new Button();
        if (!url.startsWith("http:") && 
                !url.startsWith("http:") && 
                !url.startsWith("mailto:") && 
                !url.startsWith("data:")) {
            if (exists(url)) {
                try {
                    Blob blob = openFileAsBlob(url);
                    char sep = getFileSystemSeparator();
                    fileName = url;
                    if (fileName.indexOf(sep) >=0) {
                        fileName = fileName.substring(fileName.lastIndexOf(sep)+1);
                    }
                    //String dataUrl = blobToDataURL(blob);
                    //registerSaveBlobHandlerDataUrl(fileName, dataUrl);
                    registerSaveBlobHandler(fileName, blob);
                    useBlobHandler = true;

                } catch (IOException ex) {

                }
            }
        }
        
        final boolean fuseBlobHandler = useBlobHandler;
        
        String buttonText = null;
        //String icon = null;
        final String furl = url;
        if (useBlobHandler) {
            //popover.setContents("<button style='white-space:nowrap' onclick='window.cn1SaveBlobHandler();'><span style='font-size:3em;vertical-align:text-bottom;' class='glyphicon glyphicon-download'/><span style='font-size:2em;vertical-align:top;'> Download "+(fileName!=null?fileName:"File")+"</span></button>");
            buttonText = "Click to Download "+(fileName!=null?fileName:"File");
            nativeButton.setText(buttonText);
            nativeButton.setMaterialIcon(FontImage.MATERIAL_SAVE);
        } else if (url.startsWith("data:")) {
            registerSaveBlobHandlerDataUrl(fileName == null ? "download":fileName, url);
            //popover.setContents("<button style='white-space:nowrap' onclick='window.cn1SaveBlobHandler();'><span style='font-size:3em;vertical-align:text-bottom;' class='glyphicon glyphicon-download; font-size: '/><span style='font-size:2em;vertical-align:top;'> Download "+(fileName!=null?fileName:"File")+"</span></button>");
            buttonText = "Click to Download "+(fileName!=null?fileName:"File");
            nativeButton.setText(buttonText);
            nativeButton.setMaterialIcon(FontImage.MATERIAL_SAVE);
            //icon = "save-file";
        } else {
            //popover.setContents("<a href='"+url+"' target='_blank'>Open URL in New Window</a>");
            if (isBacksideHookAvailable()) {
                addBacksideHook(new JSRunnable() {
                    public void run() {
                        window.open(furl, "New Window");
                    }
                });
                
            } else {
                final Sheet sheet = new Sheet(null, "Open Link");
                SpanLabel l = new SpanLabel("Open "+url+" in a new window?");
                Button ok = new Button("OK");
                Button cancel = new Button("Cancel");
                ok.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        sheet.back();
                        CN.execute(furl);
                    }
                });
                cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        sheet.back();
                    }
                });
                sheet.getContentPane().setLayout(BoxLayout.y());
                sheet.getContentPane().add(BorderLayout.centerEastWest(l, FlowLayout.encloseIn(ok, cancel), null));
                sheet.show();
                
            }
            return;
            
        }
        if (isBacksideHookAvailable()) {
            addBacksideHook(new JSRunnable() {
                public void run() {
                    if (fuseBlobHandler) {
                        fireSaveBlobHandler();
                    } else {
                        _log("Opening URL in new window");
                        window.open(furl, "New Window");
                    }
                }
            });

        } else {
            final Sheet sheet = new Sheet(null, "Download file");
            String dlName = fileName == null ? "file" : fileName;
            SpanLabel l = new SpanLabel("Download "+dlName+" now?");
            Button ok = new Button("OK");
            Button cancel = new Button("Cancel");
            ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    sheet.back();
                    addBacksideHook(new JSRunnable() {
                        public void run() {
                            if (fuseBlobHandler) {
                                fireSaveBlobHandler();
                            } else {
                                window.open(furl, "New Window");
                            }
                        }
                    });
                }
            });
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    sheet.back();
                }
            });
            sheet.getContentPane().setLayout(BoxLayout.y());
            sheet.getContentPane().add(BorderLayout.centerEastWest(l, FlowLayout.encloseIn(ok, cancel), null));
            sheet.show();
        }
        
    }

    
    @Override
    public Boolean canExecute(String url) {
        return true;
    }

    class HTML5Image extends com.codename1.ui.Image {
        HTML5Image(NativeImage im) {
            super(im);
        }
    }

    @Override
    public boolean supportsNativeImageCache() {
        return true;
    }

    private void attachMutableImageSurface(final NativeImage image, HTML5Graphics graphics) {
        image.mutableGraphics = graphics;
        image.mutableGraphics.setMutationListener(new Runnable() {
            @Override
            public void run() {
                JavaScriptNativeImageAdapter.invalidatePatternCache(image.getImageModel());
            }
        });
    }

    
    
    @Override
    public void downloadImageToCache(String _url, final SuccessCallback<Image> onSuccess, final FailureCallback<Image> onFail) {
        if (urlProxifier != null){
            _url = urlProxifier.proxifyURL(_url);
        }
        final String url = _url;
        final NativeImage im = new NativeImage();
        im.img = renderingBackend.createCrossOriginImageElement(url);
        im.setSuppressRepaint(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                im.load();
                im.setSuppressRepaint(false);
                if (im.loaded) {
                    final HTML5Image cn1Im = new HTML5Image(im);
                    
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            onSuccess.onSucess(cn1Im);
                        }
                    });
                } else {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            onFail.onError(this, new IOException("Failed to load image from url "+url), 500, "Failed to load image from url "+url);
                        }
                    });
                }
            }
        }).start();
    }
    
    
    
    @Override
    public void downloadImageToStorage(String _url, final String fileName, final SuccessCallback<Image> onSuccess, final FailureCallback<Image> onFail) {
        if (urlProxifier != null){
            _url = urlProxifier.proxifyURL(_url);
        }
        final String url = _url;
        final NativeImage im = new NativeImage();
        im.img = renderingBackend.createCrossOriginImageElement(url);
        im.setSuppressRepaint(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                im.load();
                im.setSuppressRepaint(false);
                if (im.loaded) {
                    final HTML5Image cn1Im = new HTML5Image(im);
                    ImageIO imageIO = ImageIO.getImageIO();
                    OutputStream fos = null;
                    try {
                        fos = com.codename1.io.Storage.getInstance().createOutputStream(fileName);
                        imageIO.save(cn1Im, fos, ImageIO.FORMAT_PNG, 1f);
                    } catch (final IOException ex) {
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                onFail.onError(this, ex, 500, ex.getMessage());
                            }
                        });
                       
                        return;
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (Exception ex){}
                        }
                    }
                    
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            onSuccess.onSucess(cn1Im);
                        }
                    });
                } else {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            onFail.onError(this, new IOException("Failed to load image from url "+url), 500, "Failed to load image from url "+url);
                        }
                    });
                }
            }
        }).start();
        
    }
    
    @Override
    public void downloadImageToFileSystem(String _url, final String fileName, final SuccessCallback<Image> onSuccess, final FailureCallback<Image> onFail) {
        if (urlProxifier != null){
            _url = urlProxifier.proxifyURL(_url);
        }
        final String url = _url;
        final NativeImage im = new NativeImage();
        im.img = renderingBackend.createCrossOriginImageElement(url);
        im.setSuppressRepaint(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                im.load();
                im.setSuppressRepaint(false);
                if (im.loaded) {
                    final HTML5Image cn1Im = new HTML5Image(im);
                    ImageIO imageIO = ImageIO.getImageIO();
                    OutputStream fos = null;
                    try {
                        fos = FileSystemStorage.getInstance().openOutputStream(fileName);
                        imageIO.save(cn1Im, fos, ImageIO.FORMAT_PNG, 1f);
                        
                    } catch (final IOException ex) {
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                onFail.onError(this, ex, 500, ex.getMessage());
                            }
                        });
                       
                        return;
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (Exception ex){}
                        }
                    }
                    
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            onSuccess.onSucess(cn1Im);
                        }
                    });
                } else {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            onFail.onError(this, new IOException("Failed to load image from url "+url), 500, "Failed to load image from url "+url);
                        }
                    });
                } 
            }
        }).start();
        
    }

    
    
    @Override
    public Object createImage(String path) throws IOException {
        if (exists(path) && !isDirectory(path)){
            NativeImage im = new NativeImage();
            Blob blob = openFileAsBlob(path);
            im.img = renderingBackend.createBlobImageElement(blob);
            im.load();
            return im;
        } else {
            InputStream in = this.getResourceAsStream(getClass(), path);
            if (in == null) {
                throw new IOException("Resource not found. " + path);
            }
            try {
                return this.createImage(in);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignored) {
                        ;
                    }
                }
            }
        }
        //throw new IOException("Image could not be loaded from file "+path+" not found");
        
    }
    
    @JSBody(params={"o"}, script="console.log(o)")
    private static native void consoleLog(JSObject o);
    
    private static void debugLog(String str) {
        if (debugFlag("debugLog")) {
            consoleLog(str);
        }
    }
    
    private static void consoleLog(String str) {
        consoleLog(JSString.valueOf(str));
    }
    
    private static void consoleLog(int val) {
        consoleLog(JSNumber.valueOf(val));
    }

    @Override
    public Object createImage(InputStream i) throws IOException {
       int avail = i.available();
       int buffLen = 2048;
       if (avail>buffLen){
           buffLen = avail;
       }
       byte[] buffer = new byte[buffLen];
       ArrayList<byte[]> bytes = new ArrayList<>();
       int num = -1;
       int totalRead = 0;
       while ((num=i.read(buffer)) != -1){
           bytes.add(buffer);
           totalRead += num;
           buffer = new byte[buffLen];
       }
       
       byte[] fullBuff = new byte[totalRead];
       int pos=0;
       for (byte[] buff : bytes){
           System.arraycopy(buff, 0, fullBuff, pos, Math.min(buffLen, totalRead-pos));
           pos += buffLen;
       }
       return this.createImage(fullBuff, 0, totalRead);
       
    }

    
    
    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        JavaScriptCanvasImageBufferLifecycle.CanvasImageBuffer<HTMLCanvasElement, HTML5Graphics> buffer =
                JavaScriptCanvasImageBufferLifecycle.createMutableBuffer(width, height, fillColor,
                        new JavaScriptCanvasImageBufferLifecycle.SizedCanvasFactory<HTMLCanvasElement>() {
                            @Override
                            public HTMLCanvasElement createCanvas(int canvasWidth, int canvasHeight) {
                                return renderingBackend.createCanvas(canvasWidth, canvasHeight);
                            }
                        }, new JavaScriptCanvasImageBufferLifecycle.GraphicsFactory<HTMLCanvasElement, HTML5Graphics>() {
                            @Override
                            public HTML5Graphics createGraphics(HTMLCanvasElement canvas) {
                                return renderingBackend.createGraphics(HTML5Implementation.this, canvas);
                            }

                            @Override
                            public void fillRect(HTML5Graphics graphics, int color, int fillWidth, int fillHeight) {
                                graphics.setColorWithAlpha(color);
                                graphics.fillRect(0, 0, fillWidth, fillHeight);
                            }
                        });
        NativeImage img = new NativeImage();
        attachMutableImageSurface(img, buffer.getGraphics());
        return img;

    }

    @Override
    public boolean areMutableImagesFast() {
        return true;
    }
    
    

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        return createNativeImage(bytes, offset, len);
    }

    @Override
    public int getImageWidth(Object i) {
        return ((NativeImage)i).getWidth();
    }

    @Override
    public int getImageHeight(Object i) {
        return ((NativeImage)i).getHeight();
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        NativeImage img = (NativeImage)nativeImage;
        
        NativeImage scaled = new NativeImage();
        JavaScriptCanvasImageBufferLifecycle.CanvasImageBuffer<HTMLCanvasElement, HTML5Graphics> buffer =
                JavaScriptCanvasImageBufferLifecycle.createBlankBuffer(width, height,
                        new JavaScriptCanvasImageBufferLifecycle.SizedCanvasFactory<HTMLCanvasElement>() {
                            @Override
                            public HTMLCanvasElement createCanvas(int canvasWidth, int canvasHeight) {
                                return renderingBackend.createCanvas(canvasWidth, canvasHeight);
                            }
                        }, new JavaScriptCanvasImageBufferLifecycle.GraphicsFactory<HTMLCanvasElement, HTML5Graphics>() {
                            @Override
                            public HTML5Graphics createGraphics(HTMLCanvasElement canvas) {
                                return renderingBackend.createGraphics(HTML5Implementation.this, canvas);
                            }

                            @Override
                            public void fillRect(HTML5Graphics graphics, int fillColor, int fillWidth, int fillHeight) {
                            }
                        });
        scaled.width = buffer.getWidth();
        scaled.height = buffer.getHeight();
        attachMutableImageSurface(scaled, buffer.getGraphics());
        if (img.img != null && !img.loaded) {
            img.load();
        }
        if (img.img != null && img.loaded) {
            int srcW = img.img.getNaturalWidth();
            int srcH = img.img.getNaturalHeight();
            if (srcW >0 && srcH > 0) {
                renderingBackend.scaleLoadedImageToCanvas(buffer.getCanvas(), img.img, srcW, srcH, width, height);
            } else {
                String msg = "Failed to scale image because the width or height is non-positive. "+srcW+"x"+srcH;
                _log(msg);
            }
        } else if (img.mutableGraphics != null) {
            renderingBackend.scaleMutableSurfaceToCanvas(buffer.getCanvas(), img.mutableGraphics.getCanvas(), img.getWidth(), img.getHeight(), width, height);
        }
        
        
        return scaled;
    }

    public int getSoftkeyCount() {
        return 0;
    }

    public int[] getSoftkeyCode(int index) {
        return null;
    }

   

    @Override
    public boolean isTouchDevice() {
        return true;
    }

    @Override
    public boolean isMultiTouch() {
        return true;
    }

    @Override
    public int getKeyboardType() {
        return Display.KEYBOARD_TYPE_QWERTY;
    }

    
    
    
    @Override
    public int getColor(Object graphics) {
        return g(graphics).getColor();

    }

    private HTML5Graphics g(Object graphics){
        return (HTML5Graphics)graphics;
    }

    @Override
    public void setColor(Object graphics, int RGB) {
        g(graphics).setColor(RGB);
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        g(graphics).setAlpha(alpha);
    }

    @Override
    public int getAlpha(Object graphics) {
        return g(graphics).getAlpha();
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        if (font == null){
            font = this.getDefaultFont();
        }
        g(graphics).setFont((NativeFont)font);
    }

    @Override
    public int getClipX(Object graphics) {
        return g(graphics).getClipX();
    }

    @Override
    public int getClipY(Object graphics) {
        return g(graphics).getClipY();
    }

    @Override
    public int getClipWidth(Object graphics) {
        return g(graphics).getClipWidth();
    }

    @Override
    public int getClipHeight(Object graphics) {
        return g(graphics).getClipHeight();
    }

    @Override
    public void setClip(Object graphics, Shape shape) {
        g(graphics).setClip(shape);
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        g(graphics).setClip(x, y, width, height);
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        g(graphics).clipRect(x, y, width, height);
    }

    @Override
    public void pushClip(Object graphics) {
        // CodenameOneImplementation.pushClip is an empty stub ("NOt implemented
        // yet"). Without this override, every g.pushClip() was a no-op, which
        // meant g.popClip() left any intermediate clipRect/setClip(Shape) in
        // place - the Sheet/Picker rounded-panel clip never restored, the Clip
        // screenshot test lost its post-popClip green rect, and generally any
        // component that temporarily narrowed its clip contaminated its
        // siblings. Route to the HTML5Graphics clip stack so clips actually
        // unwind in LIFO order.
        g(graphics).pushClip();
    }

    @Override
    public void popClip(Object graphics) {
        g(graphics).popClip();
    }

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        g(graphics).drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int width, int height) {
        g(graphics).fillRect(x, y, width, height);
    }
    
    @Override
    public void clearRect(Object graphics, int x, int y, int width, int height) {
        g(graphics).clearRect(x, y, width, height);
    }
    

    @Override
    public void fillLinearGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        g(graphics).fillLinearGradient(x, y, width, height, startColor, endColor, horizontal);
    }

    @Override
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        g(graphics).fillPolygon(xPoints, yPoints, nPoints);
    }

    
    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        g(graphics).drawRect(x,y,width,height);
    }

    
    
    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        g(graphics).drawRoundRect(x,y,width,height,arcWidth,arcHeight);
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        g(graphics).fillRoundRect(x,y,width,height,arcWidth,arcHeight);
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        g(graphics).fillArc(x,y,width,height,startAngle, arcAngle);
    }

    @Override
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height) {
        fillRadialGradient(graphics, startColor, endColor, x, y, width, height, 0, 360);
    }
    @Override
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        g(graphics).fillRadialGradient(startColor, endColor, x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillRectRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height,
            float relativeX, float relativeY, float relativeSize) {
        g(graphics).fillRectRadialGradient(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
    }
    
    

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        g(graphics).drawArc(x,y,width,height,startAngle, arcAngle);
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        g(graphics).drawString(str, x, y);
    }

    @Override
    public void drawShape(Object graphics, Shape shape, Stroke stroke) {
        g(graphics).drawShape(shape, stroke); 
    }

    @Override
    public void fillShape(Object graphics, Shape shape) {
        g(graphics).fillShape(shape);
    }

    @Override
    public boolean isShapeClipSupported(Object graphics) {
        return true;
    }
    
    

    @Override
    public boolean isShapeSupported(Object graphics) {
        return true;
    }

    @Override
    public boolean isTransformSupported() {
        return true;
    }

    @Override
    public void concatenateTransform(Object t1, Object t2) {
        ((JSAffineTransform)t1).concatenate((JSAffineTransform)t2);
    }

    @Override
    public void copyTransform(Object src, Object dest) {
        ((JSAffineTransform)dest).copyFrom((JSAffineTransform)src);
    }

    @Override
    public Object makeTransformAffine(double m00, double m10, double m01, double m11, double m02, double m12) {
        JSAffineTransform t = JSAffineTransform.Factory.getTranslateInstance(0, 0);
        t.setTransform(m00, m10, m01, m11, m02, m12);
        return t;
    }

    @Override
    public void setTransformAffine(Object nativeTransform, double m00, double m10, double m01, double m11, double m02, double m12) {
        ((JSAffineTransform)nativeTransform).setTransform(m00, m10, m01, m11, m02, m12);
    }

    @Override
    public Object makeTransformIdentity() {
        return JSAffineTransform.Factory.getTranslateInstance(0, 0);
    }

    @Override
    public void setTransformIdentity(Object transform) {
        ((JSAffineTransform)transform).setToTranslation(0, 0);
    }
    
    @Override
    public Object makeTransformRotation(float angle, float x, float y, float z) {
        return JSAffineTransform.Factory.getRotateInstance(angle, x, y);
    }

    @Override
    public void setTransformRotation(Object nativeTransform, float angle, float x, float y, float z) {
        ((JSAffineTransform)nativeTransform).setToRotation(angle, x, y);
    }
    
    

    @Override
    public Object makeTransformScale(float scaleX, float scaleY, float scaleZ) {
        return JSAffineTransform.Factory.getScaleInstance(scaleX, scaleY);
    }

    @Override
    public void setTransformScale(Object nativeTransform, float scaleX, float scaleY, float scaleZ) {
        ((JSAffineTransform)nativeTransform).setToScale(scaleX, scaleY);
    }
    
    

    @Override
    public Object makeTransformTranslation(float translateX, float translateY, float translateZ) {
        return JSAffineTransform.Factory.getTranslateInstance(translateX, translateY);
    }

    @Override
    public void setTransformTranslation(Object nativeTransform, float translateX, float translateY, float translateZ) {
        if (nativeTransform == null) return;
        ((JSAffineTransform)nativeTransform).setToTranslation(translateX, translateY);
    }



    @Override
    public Object makeTransformInverse(Object nativeTransform) {
        if (nativeTransform == null) return null;
        return ((JSAffineTransform)nativeTransform).createInverse();
    }

    @Override
    public void setTransformInverse(Object nativeTransform) throws Transform.NotInvertibleException {
        if (nativeTransform == null) return;
        ((JSAffineTransform)nativeTransform).copyFrom((JSAffineTransform)makeTransformInverse(nativeTransform));
    }

    @Override
    public void transformTranslate(Object nativeTransform, float x, float y, float z) {
        if (nativeTransform == null) return;
        ((JSAffineTransform)nativeTransform).translate(x, y);
    }

    @Override
    public void transformScale(Object nativeTransform, float x, float y, float z) {
        if (nativeTransform == null) return;
        ((JSAffineTransform)nativeTransform).scale(x, y);
    }

    @Override
    public void transformRotate(Object nativeTransform, float angle, float x, float y, float z) {
        if (nativeTransform == null) return;
        ((JSAffineTransform)nativeTransform).rotate(angle, x, y);
    }

    @Override
    public boolean transformEqualsImpl(Transform t1, Transform t2) {
        if (t2 == null || t1 == null) {
            return t1 == t2;
        }
        JSAffineTransform at1 = (JSAffineTransform)t1.getNativeTransform();
        JSAffineTransform at2 = (JSAffineTransform)t2.getNativeTransform();
        if (at1 == null || at2 == null) {
            return at1 == at2;
        }
        return at1.isEqualTo(at2);
    }
    
    @Override
    public void transformPoint(Object nativeTransform, float[] in, float[] out) {
        if (nativeTransform == null) {
            // The JS port can reach this method with a null native backing in scene/
            // Node/SpinnerNode paths where Transform.getNativeTransform() hasn't been
            // materialized. iOS/Android never arrive here because their equivalent
            // Transform paths always keep a non-null native. Treat null as identity
            // (pass-through) so the spinner render path in ValidatorLightweightPicker
            // et al. doesn't crash the worker with a TypeError and block the suite.
            out[0] = in[0];
            out[1] = in[1];
            if (out.length > 2) {
                out[2] = in.length > 2 ? in[2] : 0;
            }
            return;
        }
        Float64Array jsIn = Float64Array.create(2);
        jsIn.set(0, in[0]);
        jsIn.set(1, in[1]);
        Float64Array jsOut = Float64Array.create(2);
        ((JSAffineTransform)nativeTransform).transform(jsIn, 0, jsOut, 0, 1);
        out[0] = (float)jsOut.get(0);
        out[1] = (float)jsOut.get(1);

    }
    
    @Override
    public void setTransform(Object graphics, Transform transform) {
        Transform existing = ((HTML5Graphics)graphics).getTransform();
        if (existing == null) {
            existing = transform==null ? Transform.makeIdentity() : transform.copy();
            ((HTML5Graphics)graphics).setTransform(existing);
        } else {
            if (transform == null) {
                existing.setIdentity();
            } else {
                existing.setTransform(transform);
            }
            ((HTML5Graphics)graphics).setTransformChanged();
            ((HTML5Graphics)graphics).applyTransform();
        }
    }

    @Override
    public Transform getTransform(Object graphics) {
       Transform existing = ((HTML5Graphics)graphics).getTransform();
       if (existing == null) {
           return Transform.makeIdentity();
       } else {
           return existing.copy();
       }       
    }

    @Override
    public void getTransform(Object nativeGraphics, Transform t) {
        Transform existing = ((HTML5Graphics)nativeGraphics).getTransform();
        if (existing == null) {
            t.setIdentity();
        } else {
            t.setTransform(existing);
        }
    }
    
    @Override
    public boolean isTransformSupported(Object graphics) {
        return true;
    }

    @Override
    public void rotate(Object nativeGraphics, float angle) {
        ((HTML5Graphics)nativeGraphics).rotate(angle);
    }

    @Override
    public void rotate(Object nativeGraphics, float angle, int pivotX, int pivotY) {
        ((HTML5Graphics)nativeGraphics).rotate(angle, pivotX, pivotY);
    }
    
    

    @Override
    public void shear(Object nativeGraphics, float x, float y) {
        //((HTML5Graphics)nativeGraphics).shear(x, y);
        throw new UnsupportedOperationException("Graphics.shear() not implemented yet");
    }

    @Override
    public void scale(Object nativeGraphics, float x, float y) {
        ((HTML5Graphics)nativeGraphics).scale(x, y);
    }

    @Override
    public boolean isAffineSupported() {
        return true;
    }

    @Override
    public void resetAffine(Object nativeGraphics) {
        ((HTML5Graphics)nativeGraphics).resetAffine();
    }
    
    

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        if (img == null) return;
        g(graphics).drawImage(img, x, y);
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y, int w, int h) {
        if (img == null) return;
        g(graphics).drawImage(img, x, y, w, h);
    }

    // Tried to implement tiling but it is creating artifacts... low priority
    // so leaving it for now.
    @Override
    public void tileImage(Object graphics, Object img, int x, int y, int w, int h) {
        if (img == null) return;
        g(graphics).tileImage(img, x, y, w, h);
    }

    
    
    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        
        g(graphics).drawRGB(rgbData, offset, x, y, w, h, processAlpha);
    }

    @Override
    public Object getNativeGraphics() {
        return graphics;
    }

    @Override
    public Object getNativeGraphics(Object image) {
        return ((NativeImage)image).mutableGraphics;
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return ((NativeFont)nativeFont).stringWidth(new String(ch, offset, length));
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        //return graphics.stringWidth(nativeFont, str);
        return ((NativeFont)nativeFont).stringWidth(str);
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        return ((NativeFont)nativeFont).charWidth(ch);
        //return stringWidth(nativeFont, ch+"");
    }
    
    

    @Override
    public int getHeight(Object nativeFont) {
        return ((NativeFont)nativeFont).fontHeight();

    }

    
    
    @Override
    public int getFontAscent(Object nativeFont) {
        return g(graphics).getFontAscent(nativeFont);
    }

    @Override
    public boolean isBaselineTextSupported() {
        return true;
    }
    
    

    @Override
    public int getFontDescent(Object nativeFont) {
        return g(graphics).getFontDescent(nativeFont);
    }

    @Override
    public boolean isNativeFontSchemeSupported() {
        return true;
    }
    
    
    

    @Override
    public Object getDefaultFont() {
        NativeFont f = new NativeFont();
        //f.css = defaultFont.css;
        f.face = defaultFont.face;
        f.size = defaultFont.size;
        f.style = defaultFont.style;
        f.height = defaultFont.height;
        f.ascent = defaultFont.ascent;
        f.fileName = defaultFont.fileName;
        f.fontName = defaultFont.fontName;
        return f;
    }

    
    @JSBody(params={"fontName", "dataUrl", "fontFormat"}, script="var newStyle = document.createElement('style');\n" +
"newStyle.appendChild(document.createTextNode(\"\\\n" +
"@font-face {\\\n" +
"    font-family: '\" + cn1_escape_single_quotes(fontName) + \"';\\\n" +
"    src: url('\" + cn1_escape_single_quotes(dataUrl) + \"') format('\" + cn1_escape_single_quotes(fontFormat) + \"');\\\n" +
"}\\\n" +
"\"));\n" +
"\n" +
"document.head.appendChild(newStyle);"
            + "WebFont.load({"
            + "custom:{families:[fontName]}, "
            + "active:function(){document.dispatchEvent(new CustomEvent('fontLoaded', {detail:{fontName:fontName, success:true}}));},"
            + "inactive:function(){document.dispatchEvent(new CustomEvent('fontLoaded', {detail:{fontName:fontName, success:false}}))}}); ")
    private native static void loadTrueTypeFont_(String fontName, String dataUrl, String fontFormat);
    
    private Set<String> loadedFonts = new HashSet<String>();
    
    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        String resolvedFontName = fontName == null || fontName.length() == 0 ? Font.NATIVE_MAIN_REGULAR : fontName;
        String resolvedFileName = fileName;
        if (resolvedFontName.indexOf("native:") != 0
                && resolvedFileName != null
                && resolvedFileName.length() > 0
                && !"null".equals(resolvedFileName)
                && !loadedFonts.contains(resolvedFontName)) {
            // Hand the bare filename to the host via the port.js native binding.
            // HTML5Implementation.getResourceAsStream rewrites relative resource
            // paths to "assets/..."; the host bridge mirrors that so the browser
            // can fetch the TTF directly into a FontFace without having to
            // transfer a ~500 KB ArrayBuffer through the worker bridge.
            // The existing arrayBufferToBase64 path runs through Window.current,
            // which is not reachable from the worker and truncates the data URL
            // to 26 chars, so the old byte->dataURL approach 100% failed to load
            // the font.
            loadTrueTypeFont_(resolvedFontName, resolvedFileName, "truetype");
            loadedFonts.add(resolvedFontName);
        }
        return createFallbackTrueTypeFont(resolvedFontName, resolvedFileName);

    }

    private NativeFont createFallbackTrueTypeFont(String fontName, String fileName) {
        NativeFont out = (NativeFont)createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        out.fontName = nativeFontName(fontName);
        out.fileName = fileName;
        if (fontName != null && fontName.startsWith("native:") && fontName.contains("Italic")) {
            out.style = Font.STYLE_ITALIC;
        }
        return out;
    }
    
    private String nativeFontName(String fontName) {
        if(fontName != null && fontName.startsWith("native:")) {
            if("native:MainThin".equals(fontName)) {
                return "native-MainThin";
            }
            if("native:MainLight".equals(fontName)) {
                return "native-MainLight";
            }
            if("native:MainRegular".equals(fontName)) {
                return "native-MainRegular";
            }
            
            if("native:MainBold".equals(fontName)) {
                return "native-MainBold";
            }
            
            if("native:MainBlack".equals(fontName)) {
                return "native-MainBlack";
            }
            
            if("native:ItalicThin".equals(fontName)) {
                return "native-ItalicThin";
            }
            
            if("native:ItalicLight".equals(fontName)) {
                return "native-ItalicLight";
            }
            
            if("native:ItalicRegular".equals(fontName)) {
                return "native-ItalicRegular";
            }
            
            if("native:ItalicBold".equals(fontName) || "native:ItalicBlack".equals(fontName)) {
                return "native-ItalicBold";
            }
        }            
        return fontName;
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        NativeFont f = (NativeFont)font;
        NativeFont f2 = new NativeFont();
        
        int fontstyle = Font.STYLE_PLAIN;
        if ((weight & Font.STYLE_BOLD) != 0) {
            fontstyle |= Font.STYLE_BOLD;
        }
        if ((weight & Font.STYLE_ITALIC) != 0) {
            fontstyle |= Font.STYLE_ITALIC;
        }
        f2.face = f.face;
        f2.size = f.size;
        f2.style = fontstyle;
        f2.height = size;
        
        f2.fontName = f.fontName;
        f2.fileName = f.fileName;
        return f2;
        
    }

    @Override
    public boolean isTrueTypeSupported() {
        return true;
    }

    
    @JSBody(params={}, script="var baseFont=window.getParameterByName('baseFont');"
            + "if (baseFont) return parseInt(baseFont); else return 0;")
    private native static int getBaseFontSize();
    
    @JSBody(params={}, script="var density=window.getParameterByName('density');"
            + "if (density) return parseInt(density); else return 0;")
    private native static int getDensityOverride();
    
    @Override
    public Object createFont(int face, int style, int size) {
        
        int height = getBaseFontSize();
        if (height == 0) {
            height = 16;
        }
        switch (getDeviceDensity()) {
            case Display.DENSITY_LOW:
            case Display.DENSITY_VERY_LOW:
                height = height/2; break;
            case Display.DENSITY_HIGH:
                height = height + height/2; break;
            case Display.DENSITY_VERY_HIGH:
                height = height * 2; break;
            case Display.DENSITY_HD:
                height = height * 3; break;
            case Display.DENSITY_560:
                height = height * 4; break;
            case Display.DENSITY_2HD:
                height = height * 5; break;
            case Display.DENSITY_4K:
                height = height * 6; break; 
        }
        int diff = height / 3;

        switch (size) {
            case Font.SIZE_SMALL:
                height -= diff;
                break;
            case Font.SIZE_LARGE:
                height += diff;
                break;
        }
        NativeFont f = new NativeFont();
        //f.css = height+"px Sans-serif";
        f.face = face;
        f.style = style;
        f.size = size;
        f.height = height;
        
        return f;
    }

    @Override
    public int getFace(Object nativeFont) {
        if (nativeFont == null){
            return Font.FACE_SYSTEM;
        };
        return ((NativeFont)nativeFont).face;
    }

    @Override
    public int getSize(Object nativeFont) {
        if (nativeFont == null){
            return Font.SIZE_MEDIUM;
        }
        return ((NativeFont)nativeFont).size;
    }

    @Override
    public int getStyle(Object nativeFont) {
        if (nativeFont == null){
            return Font.STYLE_PLAIN;
        }
        return ((NativeFont)nativeFont).style;
    }

    @Override
    public Object connect(String url, boolean read, boolean write, int timeout) throws IOException {
        return JavaScriptNetworkAdapter.connect(url, read, write, timeout,
                urlProxifier == null ? null : new JavaScriptNetworkAdapter.UrlTransformer() {
                    @Override
                    public String transform(String input) {
                        return urlProxifier.proxifyURL(input);
                    }
                },
                new JavaScriptNetworkAdapter.ConnectionFactory<NetworkConnection>() {
                    @Override
                    public NetworkConnection create(String targetUrl, boolean readConnection, boolean writeConnection, int connectionTimeout) throws IOException {
                        return new NetworkConnection(targetUrl, readConnection, writeConnection, connectionTimeout);
                    }
                });
    }
    
    
    
    
    
    
    
    

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        
        return connect(url, read, write, -1);
        
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
        JavaScriptNetworkAdapter.setHeader((JavaScriptNetworkAdapter.Connection) connection, key, val);
    }

    @Override
    public void setHttpMethod(Object connection, String method) throws IOException {
        JavaScriptNetworkAdapter.setHttpMethod((JavaScriptNetworkAdapter.Connection) connection, method);
    }

    
    
    @Override
    public int getContentLength(Object connection) {
        return JavaScriptNetworkAdapter.getContentLength((JavaScriptNetworkAdapter.Connection) connection);
    }

    @Override
    public OutputStream openFileOutputStream(String file) throws IOException {
        if (isTempFile(file)) {
            throw new IOException("Temp file writing not supported yet.");
        }
        if (isDirectory(file)) {
            throw new IOException("Failed to open output stream for "+file+" because it is a directory.");
        }
        return LocalForage.getInstance().openOutputStream(wrapFile(file), new ItemSavedListener(){

            @Override
            public void onSave(LocalForage.ItemSavedEvent evt) {
                FileInfo finfo = createFileInfo(evt.getSize(), System.currentTimeMillis());
                try {
                    LocalForage.getInstance().setItem(evt.getKey()+".cn1fileinfo", finfo);
                } catch (IOException ex) {
                    //Log.e(ex);
                    consoleLog("Error onSave in localForage");
                    consoleLog(ex.getMessage());
                }
            }
            
        });
    }
    

    
    
    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        return JavaScriptNetworkAdapter.openOutputStream(connection, new JavaScriptNetworkAdapter.FileOutputStreamProvider() {
            @Override
            public OutputStream openFileOutputStream(String file) throws IOException {
                return HTML5Implementation.this.openFileOutputStream(file);
            }
        });
    }

    @Override
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        throw new RuntimeException("getOutputStream with offset not supported yet");
    }

    @Override
    public boolean shouldWriteUTFAsGetBytes() {
        return true;
    }

    
    
    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        return JavaScriptNetworkAdapter.openInputStream(connection);
    }

    @Override
    public void cleanup(Object o) {
        super.cleanup(o);
        JavaScriptNetworkAdapter.cleanup(o);
    }

    
    
    @Override
    public void setPostRequest(Object connection, boolean p) {
        JavaScriptNetworkAdapter.setPostRequest((JavaScriptNetworkAdapter.Connection) connection, p);
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        return JavaScriptNetworkAdapter.getResponseCode((JavaScriptNetworkAdapter.Connection) connection);
    }

    @Override
    public String getResponseMessage(Object connection) throws IOException {
        return JavaScriptNetworkAdapter.getResponseMessage((JavaScriptNetworkAdapter.Connection) connection);
    }

    @Override
    public String getHeaderField(String name, Object connection) throws IOException {
        return JavaScriptNetworkAdapter.getHeaderField(name, (JavaScriptNetworkAdapter.Connection) connection);
    }

    @Override
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        return JavaScriptNetworkAdapter.getHeaderFieldNames((JavaScriptNetworkAdapter.Connection) connection);
    }

    @Override
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        return JavaScriptNetworkAdapter.getHeaderFields(name, (JavaScriptNetworkAdapter.Connection) connection);
    }

    private LocalForage getLocalForage(){
        return LocalForage.getInstance();
    }
    
    private String wrapStorageKey(String name){
        return JavaScriptRuntimeFacade.wrapStorageKey(name);
    }
    
    private String unwrapStorageKey(String name){
        return JavaScriptRuntimeFacade.unwrapStorageKey(name);
    }
    
    private String wrapFile(String path){
        return JavaScriptRuntimeFacade.wrapFile(path);
    }
    
    private String unwrapFile(String path){
        return JavaScriptRuntimeFacade.unwrapFile(path);
    }
    
    @Override
    public void deleteStorageFile(String name) {
        try {
            JavaScriptStorageAdapter.deleteStorageFile(createStorageBackend(), name);
        } catch (IOException ex){
            consoleLog("Error on deleteStorageFile "+name+", : "+ex.getMessage());
            
        }
    }

    
    @Override
    public OutputStream createStorageOutputStream(String name) throws IOException {
        return JavaScriptStorageAdapter.createStorageOutputStream(createStorageBackend(), name);
    }

    @Override
    public InputStream createStorageInputStream(String name) throws IOException {
        return JavaScriptStorageAdapter.createStorageInputStream(createStorageBackend(), name);
    }

    @Override
    public boolean storageFileExists(String name) {
        try {
            return JavaScriptStorageAdapter.storageFileExists(createStorageBackend(), name);
        } catch (IOException ex) {
            //Log.e(ex);
            consoleLog("Error checking storage for storageFileExists");
            consoleLog(ex.getMessage());
        }
        return false;
    }

    @Override
    public int getStorageEntrySize(String name) {
        try {
            return JavaScriptStorageAdapter.getStorageEntrySize(createStorageBackend(), name);
        } catch (IOException ex) {
            //Log.e(ex);
            return -1;
        }
    }
    
    

    @Override
    public String[] listStorageEntries() {
        try {
            return JavaScriptStorageAdapter.listStorageEntries(createStorageBackend());
        } catch (IOException ex) {
            //Log.e(ex);
            consoleLog("Error in listStorageEntries");
            consoleLog(ex.getMessage());
        } 
        return new String[]{};
    }

    private JavaScriptStorageAdapter.Backend createStorageBackend() {
        return new JavaScriptStorageAdapter.Backend() {
            @Override
            public void removeItem(String key) throws IOException {
                getLocalForage().removeItem(key);
            }

            @Override
            public OutputStream openOutputStream(String key) throws IOException {
                return getLocalForage().openOutputStream(key);
            }

            @Override
            public InputStream openInputStream(String key) throws IOException {
                return getLocalForage().openInputStream(key);
            }

            @Override
            public Object getItem(String key) throws IOException {
                return getLocalForage().getItem(key);
            }

            @Override
            public int getSize(String key) throws IOException {
                return getLocalForage().getSize(key);
            }

            @Override
            public String[] keys() throws IOException {
                return getLocalForage().keys();
            }
        };
    }

    @Override
    public String[] listFilesystemRoots() {
        return new String[]{"file:///"};
    }

    @Override
    public String getAppHomePath() {
        return listFilesystemRoots()[0];
    }
    
    

    @Override
    public String[] listFiles(String directory) throws IOException {
        String wrapped = stripTrailingSlash(wrapFile(directory))+"/";
        try {
            List<String> out = new ArrayList<>();
            String[] keys = getLocalForage().keys();
            for (String key : keys){
                if (!key.startsWith(wrapped) || key.endsWith(".cn1fileinfo")) {
                    continue;
                }
                String fileName = key;
                int pos = fileName.lastIndexOf("/");
                if (pos >= 0) {
                    fileName = fileName.substring(pos+1);
                }
                
                if (key.equals(wrapped + fileName)) {
                    if (fileName.endsWith(".cn1dir")) {
                        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                    }
                    out.add(fileName);
                }
            }

            return out.toArray(new String[out.size()]);
        } catch (IOException ex) {
            //Log.e(ex);
            consoleLog("Error in listFiles");
            consoleLog(ex.getMessage());
        } 
        return new String[]{};
    }

    @Override
    public long getRootSizeBytes(String root) {
        return -1;
    }

    @Override
    public long getRootAvailableSpace(String root) {
        return -1;
    }

    @Override
    public void mkdir(String directory) {
        String wrapped = stripTrailingSlash(wrapFile(directory))+".cn1dir";
        if (!this.exists(directory)) {
            try {
                LocalForage.getInstance().setItem(wrapped, "");
            } catch (IOException ex) {
                //Log.e(ex);
                consoleLog("error in mkdir");
                consoleLog(ex.getMessage());
            }
        } else {
            consoleLog("Directory "+directory+" already exists");
        }
    }

    @Override
    public void deleteFile(String file) {
        if (isTempFile(file)) {
            deleteTempFile(file);
            return;
        }
        String wrapped = stripTrailingSlash(wrapFile(file));
        boolean isDirectory = this.isDirectory(file);
        if (isDirectory) {
            if (isRootFile(file)) {
                consoleLog("Failed to delete file "+file+" because it is the root directory");
                return;
            }
            try {
                String[] children = listFiles(file);
                if (children.length != 0) {
                    consoleLog("Failed to delete directory "+file+" because it is not empty");
                    return;
                }
            } catch (IOException ex) {
                //Log.e(ex);
                consoleLog("Error in deleteFile");
                consoleLog(ex.getMessage());
                return;
            }
            try {
                consoleLog("Attempting to delete directory "+file+" - wrapped=>" + wrapped);
                LocalForage.getInstance().removeItem(wrapped+".cn1dir"); // actually delete the entry
            } catch (IOException ex) {
                consoleLog("Error in deleteFile");
                consoleLog(ex.getMessage());
            }
            return;
        }
        
        try {
            LocalForage lf = LocalForage.getInstance();
            
           lf.removeItem(wrapped);
           lf.removeItem(wrapped+".cn1fileinfo");
        } catch (IOException ex) {
            consoleLog("Error in deleteFile");
            consoleLog(ex.getMessage());
        }
        
    }

    @Override
    public boolean isHidden(String file) {
        return false;
    }

    @Override
    public void setHidden(String file, boolean h) {
        
    }

    /**
     * Schema for info object that stores info about a file in the file system.
     * These are stored as javascript objects under the name <filename>.cn1fileinfo
     * in the same directory as <filename>.
     */
    private static interface FileInfo extends JSObject {
        @JSProperty
        public double getFileLength();
        
        @JSProperty
        public double getLastModified();
        
        @JSProperty
        public void setFileLength(double len);
        
        @JSProperty
        public void setLastModified(double timestamp);
    }
    
    
    @JSBody(params={"fileLength", "lastModified"}, script="return {fileLength: fileLength, lastModified: lastModified}")
    private static native FileInfo createFileInfo(double len, double modified);
    
    @JSBody(params={"blob"}, script="window.cn1TmpFiles = window.cn1TmpFiles || []; return window.cn1TmpFiles.push(blob)-1;")
    private native static int createTempFile_(Blob blob);
    
    @JSBody(params={"index"}, script="window.cn1TmpFiles = window.cn1TmpFiles || []; return window.cn1TmpFiles[index];")
    private native static Blob getTempFile_(int index);
    
    @JSBody(params={"index"}, script="window.cn1TmpFiles = window.cn1TmpFiles || []; window.cn1TmpFiles[index] = null;")
    private static native void deleteTempFile_(int index);
    
    @JSBody(params={"blob"}, script="return blob.name || null;")
    private static native String getFileName(Blob blob);
    
    public static boolean isTempFile(String path) {
        return path.indexOf("tmp://") == 0;
    }
    
    private static int getTempFileIndex(String path) {
        int lastPos = path.lastIndexOf("/");
        if (lastPos < 0 || lastPos >= path.length()-1) return -1;
        int startPos = lastPos+1;
        int endPos = path.indexOf("-", startPos);
        if (endPos < 0) {
            endPos = path.length();
        }
        
        return Integer.parseInt(path.substring(startPos, endPos));
    }
    
    public static Blob getTempFile(String path) {
        int index = getTempFileIndex(path);
        if (index < 0) return null;
        return getTempFile_(index);
    }
    
    public static String createTempFile(Blob blob) {
        int index = createTempFile_(blob);
        String filename = getFileName(blob);
        return "tmp://"+index+(filename==null ? "" : ("-"+filename));
    }
    
    public static void deleteTempFile(String path) {
        int index = getTempFileIndex(path);
        if (index < 0) return;
        deleteTempFile_(index);
    }
    
    /**
     * Gets the FileInfo object for a given file.  Returns a file info object
     * with -1 length and -1 mod time if there was an error retrieving the info.
     * @param file The full path tot he file to get the info for.
     * @return 
     */
    private FileInfo getFileInfo(String file) {
        if (isTempFile(file)) {
            Blob b = getTempFile(file);
            if (b == null) {
                return createFileInfo(-1,-1);
            }
            return createFileInfo(b.getSize(), -1);
        }
        if (isDirectory(file)) {
            return getFileInfo(stripTrailingSlash(file)+".cn1dir");
        }
        String wrapped = stripTrailingSlash(wrapFile(file));
        try {
            return (FileInfo) LocalForage.getInstance().getItem(wrapped+".cn1fileinfo");
        } catch (IOException ex) {
             //Log.e(ex);
             return createFileInfo(-1, -1);
        }
    }
    
    @Override
    public long getFileLength(String file) {
        return (long) getFileInfo(file).getFileLength();
        
    }

    @Override
    public InputStream openFileInputStream(String file) throws IOException {
        if (isTempFile(file)) {
            Blob b = getTempFile(file);
            if (b == null) {
                throw new IOException("Failed to find temp file "+file);
            }
            return BlobUtil.openInputStream(b);
        }
        String wrapped = stripTrailingSlash(wrapFile(file));
        if (!exists(file)) {
            throw new IOException("Failed to open input stream for file "+file+" because it does not exist.");
        }
        
        if (isDirectory(file)) {
            throw new IOException("Failed to open input stream for file "+file+" because it is a directory.");
        }
        
        return LocalForage.getInstance().openInputStream(wrapped);
    }
    
    @JSBody(params={"o", "type"}, script="return (o instanceof window[type]);")
    private static native boolean instanceOf(JSObject o, String type);
    
    
    private Blob openFileAsBlob(String file) throws IOException {
        
        if (!exists(file)) {
            throw new IOException("Failed to open input stream for file "+file+" because it does not exist.");
        }
        
        if (isDirectory(file)) {
            throw new IOException("Failed to open input stream for file "+file+" because it is a directory.");
        }
        
        JSObject obj = null;
        if (isTempFile(file)) {
            obj = getTempFile(file);
        } else {
            String wrapped = stripTrailingSlash(wrapFile(file));
            obj = LocalForage.getInstance().getItem(wrapped);
        }
        if (instanceOf(obj, "Blob")) {
            return (Blob)obj;
        } else if (instanceOf(obj, "Uint8Array")) {
            return BlobUtil.createBlob((Uint8Array)obj, "application/octet-stream");
        } else {
            throw new IOException("File at "+file+" is not a blob");
        }
    }

    private static String stripTrailingSlash(String path) {
        return JavaScriptRuntimeFacade.stripTrailingSlash(path);
    }
    @Override
    public boolean isDirectory(String file) {
        if (isRootFile(file)) {
            return true;
        }
        if (isTempFile(file)) {
            return false;
        }
        try {
            String wrapped = stripTrailingSlash(wrapFile(file));
            
            return LocalForage.getInstance().getItem(wrapped+".cn1dir") != null;
        } catch (IOException ex) {
            //Log.e(ex);
            return false;
        }
    }

    @Override
    public boolean exists(String file) {
        if (isRootFile(file)) {
            return true;
        }
        if (isTempFile(file)) {
            return getTempFile(file) != null;
        }
        try {
            String wrapped = stripTrailingSlash(wrapFile(file));
            return LocalForage.getInstance().getItem(wrapped) != null || LocalForage.getInstance().getItem(wrapped+".cn1dir") != null;
        } catch (IOException ex) {
            //Log.e(ex);
            return false;
        }
    }

    private boolean isRootFile(String file) {
        return JavaScriptRuntimeFacade.isRootFile(file);
    }
    
    /**
     * 
     * @param file Full path to file to rename
     * @param newName New name (file nmame only)
     */
    @Override
    public void rename(String file, String newName) {
        rename(file, newName, true);
    }
    private void rename(String file, String newName, boolean checkParent) {
        if (newName.indexOf('/') < 0) {
            if(file.endsWith("/")) {
                file = file.substring(0, file.length() - 1);
            }
            int pos = file.lastIndexOf('/');
            if(pos > -1) {
                newName = file.substring(0, pos) + "/" + newName;
            }
        }
        if (isRootFile(file) || isRootFile(newName)) {
            System.out.println("Failed to rename file "+file+" to "+newName+". Cannot rename to root");
            return;
        }
        file = stripTrailingSlash(file);
        newName = stripTrailingSlash(newName);
        if (isTempFile(file)) {
            System.out.println("Cannot rename temp files");
            return;
        }
        if (exists(newName)) {
            System.out.println("Failed to rename "+file+" to "+newName+" because a file already exists with that name");
            return;
        }  
        if (!exists(file)) {
            System.out.println("Failed to rename "+file+" to "+newName+" because the source file does not exist.");
            return;
        }
        String parent = newName.substring(0, newName.lastIndexOf('/'));
        if (checkParent) {
            if (!isRootFile(parent) && !exists(parent)) {
                System.out.println("Cannot rename file "+file+" to "+newName+" because the parent directory "+parent+" does not exist");
                return;
            }
            
        }
        boolean isDirectory = isDirectory(file);
        String wrappedInput = stripTrailingSlash(wrapFile(file));
        String wrappedOutput = stripTrailingSlash(wrapFile(newName));
        if (isDirectory) {
            wrappedOutput += ".cn1dir";
            wrappedInput += ".cn1dir";
        }
        
        
        
        FileInfo finfo = getFileInfo(file);
        LocalForage forage = LocalForage.getInstance();
        try {
            
            if (isDirectory) {
                // We need to rename all of the children of directories explicitly
                // because the localforage database stores each file in its own row with full path.
                for (String child : listFiles(file)) {
                    rename(file + "/" + child, newName + "/" + child, false);
                }
            }
            
            JSObject contents = forage.getItem(wrappedInput);
            forage.setItem(wrappedOutput, contents);
            if (!isDirectory) {
                forage.setItem(wrappedOutput+".cn1fileinfo", finfo);
            }
            forage.removeItem(wrappedInput);
            if (!isDirectory) {
                forage.removeItem(wrappedInput+".cn1fileinfo");
            }
            
        } catch (IOException ex) {
            //Log.e(ex);
            consoleLog("Error in rename");
            consoleLog(ex.getMessage());
        }
        
        
    }

    @Override
    public char getFileSystemSeparator() {
        return '/';
    }

    @Override
    public String getPlatformName() {
        return "HTML5";
    }

    @JSBody(params={"str"}, script="return window.parseFloat(str)")
    private native static double _parseDouble(String str);
    
    @JSBody(params={"n"}, script="return n.toLocaleString()")
    private native static String _toLocaleString(double n);
    
    @JSBody(params={"n"}, script="return n.toLocaleString()")
    private native static String _toLocaleString(int n);
    
    @JSBody(params={"locale", "n"}, script="return n.toLocaleString(locale)")
    private native static String _toLocaleString(String locale, int n);
    
    @JSBody(params={"locale", "n"}, script="return n.toLocaleString(locale)")
    private native static String _toLocaleString(String locale, double n);
    
    
    @JSBody(params={}, script="var number=0;try {number.toLocaleString('i');} catch (e) { return e.name ==='RangeError';} return false")
    private native static boolean _toLocaleStringSupportsLocales();
    
    private static int toLocaleStringSupportsLocales=-1;
    
    private static boolean toLocaleStringSupportsLocales() {
        if (toLocaleStringSupportsLocales == -1) {
            toLocaleStringSupportsLocales = _toLocaleStringSupportsLocales() ? 1 : 0;
        }
        return toLocaleStringSupportsLocales == 1;
    }
    
    private static String toLocaleString(String locale, int n) {
        if (toLocaleStringSupportsLocales()) {
            return _toLocaleString(locale, n);
        } else {
            return _toLocaleString(n);
        }
    }
    
    private static String toLocaleString(String locale, double n) {
        if (toLocaleStringSupportsLocales()) {
            return _toLocaleString(locale, n);
        } else {
            return _toLocaleString(n);
        }
    }
    
    
    
    
    
    JSNumberFormat numberFormatter;
    Map<String, JSNumberFormat> currencyFormatters = new HashMap<String, JSNumberFormat>();
    Map<String, JSDateFormat> dateFormatters = new HashMap<String, JSDateFormat>();
    
    @JSBody(params={}, script="return navigator.language || navigator.browserLanguage")
    private static native String getBrowserLanguage();
    
    @JSBody(params={}, script="try { return Intl.DateTimeFormat().resolvedOptions().timeZone; } catch(e) { return null; }")
    private static native String detectTimezone();
   
    @Override
    public L10NManager getLocalizationManager() {
        if (l10n == null) {
            Locale l = Locale.getDefault();
            l10n = new L10NManager(l.getLanguage(), l.getCountry()) {
                
                private String thousandsSeparator;
                private String decimalSeparator;
                
                private String decimalSeparator() {
                    if (decimalSeparator == null) {
                        String formatted = this.format(2.1);
                        for (char c : formatted.toCharArray()) {
                           if (Character.isDigit(c)) {
                                
                            } else {
                                decimalSeparator = String.valueOf(c);
                                break;
                            }
                        }
                        if (decimalSeparator == null) {
                            throw new RuntimeException("Cannot find decimal separator");
                        }
                    }
                    return decimalSeparator;
                }
                
                private String thousandsSeparator() {
                    if (thousandsSeparator == null) {
                        String formatted = this.format(1000);
                        for (char c : formatted.toCharArray()) {
                            if (Character.isDigit(c)) {
                                
                            } else {
                                thousandsSeparator = String.valueOf(c);
                                break;
                            }
                        }
                        if (thousandsSeparator == null) {
                            thousandsSeparator = ",";
                        }
                    }
                    return thousandsSeparator;
                }
                
                private JSNumberFormat numberFormatter() {
                    if (numberFormatter == null) {
                        numberFormatter = new JSNumberFormat(this.getLocaleStr());
                    }
                    return numberFormatter;
                }
                
                private JSNumberFormat getCurrencyFormatter(String currency) {
                    JSNumberFormat fmt = currencyFormatters.get(currency+"-"+this.getLocaleStr());
                    if (fmt == null) {
                        fmt = new JSNumberFormat(this.getLocaleStr());
                        fmt.setStyle("currency");
                        fmt.setCurrency(currency);
                        currencyFormatters.put(currency+"-"+this.getLocaleStr(), fmt);
                    }
                    
                    return fmt;
                    
                }
                
                private JSDateFormat getDateFormatter(String style) {
                    JSDateFormat fmt = dateFormatters.get(style);
                    if (fmt == null) {
                        fmt = new JSDateFormat(this.getLocaleStr());
                        fmt.setStyle(style);
                        dateFormatters.put(this.getLocaleStr(), fmt);
                    }
                    return fmt;
                }
                
                @Override
                public String getLongMonthName(Date date) {
                    // The ParparVM Java API in vm/JavaAPI/src/java/text/SimpleDateFormat.java
                    // only exposes the no-arg and single-String constructors; the
                    // `(String, Locale)` overload isn't present in the minimal runtime
                    // and trying to use it produces a ReferenceError at worker runtime.
                    // Locale-sensitivity for month names flows through the getLocaleStr()
                    // path above via JSDateFormat, so dropping the locale here only
                    // affects the month-name fallback (English labels, matching the
                    // default locale everywhere these tests run).
                    java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMMM");
                    return fmt.format(date);
                }

                @Override
                public String getShortMonthName(Date date) {
                    java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMM");
                    return fmt.format(date);
                }
                
                private String getLocaleStr() {
                    Locale l = Locale.getDefault();
                    return l.getLanguage()+"-"+l.getCountry();
                }
                
                public double parseDouble(String localeFormattedDecimal) {
                    localeFormattedDecimal = StringUtil.replaceAll(localeFormattedDecimal, thousandsSeparator(), "");
                    localeFormattedDecimal = localeFormattedDecimal.replace(decimalSeparator().charAt(0), '.');
                    double out = _parseDouble(localeFormattedDecimal);
                    return out;
                    
                }
                
                public String format(int number) {
                    return _toLocaleString(number);
                }

                public String format(double number) {
                    return _toLocaleString(number);
                }

                public String formatCurrency(double currency) {
                    
                    return getCurrencyFormatter(Display.getInstance().getProperty("l10n.currency", "USD")).format(currency);
                }

                public String formatDateLongStyle(Date d) {
                    return getDateFormatter(JSDateFormat.DATE_LONG).format(d);
                }

                public String formatDateShortStyle(Date d) {
                    return getDateFormatter(JSDateFormat.DATE_SHORT).format(d);
                }

                public String formatDateTime(Date d) {
                    return getDateFormatter(JSDateFormat.DATETIME_LONG).format(d);
                }

                public String formatDateTimeMedium(Date d) {
                    return getDateFormatter(JSDateFormat.DATETIME_MEDIUM).format(d);
                }

                public String formatDateTimeShort(Date d) {
                    return getDateFormatter(JSDateFormat.DATETIME_SHORT).format(d);
                }

                public String getCurrencySymbol() {
                    return Display.getInstance().getProperty("l10n.currency.symbol", "$");
                }

                
                public void setLocale(String locale, String language) {
                    
                    super.setLocale(locale, language);
                    Locale l = new Locale(language, locale);
                    Locale.setDefault(l);
                }
            };
        }
        return l10n;
    }

    @Override
    public boolean postMessage(Object browserPeer, String message, String targetOrigin) {
        
        ((HTML5BrowserComponent)browserPeer).postMessage(message, targetOrigin);
        return true;
    }

    @Override
    public boolean installMessageListener(Object browserPeer) {
        ((HTML5BrowserComponent)browserPeer).installMessageListener();
        return true;
    }

    @Override
    public boolean uninstallMessageListener(Object browserPeer) {
        ((HTML5BrowserComponent)browserPeer).uninstallMessageListener();
        return true;
    }
    
    
   
    
    
    @Override
    public boolean isNativeBrowserComponentSupported() {
        if (!"false".equals(Display.getInstance().getProperty("javascript.nativeBrowser", "true"))) {
            return true;
        } else {
            return false;
        }
    }

    @JSBody(script="var el = jQuery('<iframe src=\"about:blank\"></iframe>').get(0); el.parentNode.removeChild(el);return el")
    private static native HTMLIFrameElement createBlankIFrame();
    
    private class SystemBrowserComponent extends BrowserComponent {
        
    }

    @Override
    protected BrowserComponent createSharedJavascriptContext() {
        return new SystemBrowserComponent();
    }
    
    
    
    
    @Override
    public PeerComponent createBrowserComponent(Object browserComponent) {
        if (browserComponent instanceof SystemBrowserComponent) {
            return new HTML5BrowserComponent(null, browserComponent);
        }
        // In ParparVM worker/host bridging, createElement("iframe") can be surfaced as a
        // generic HTMLElement wrapper. Keep this typed as HTMLElement to avoid strict cast
        // failures while still constructing the browser peer correctly.
        HTMLElement el = window.getDocument().createElement("iframe");
        //HTMLIFrameElement el = createBlankIFrame();
        
        HTML5BrowserComponent browser = new HTML5BrowserComponent(el, browserComponent);
        return browser;
    }

    @Override
    public Object createNativeBrowserWindow(String startURL) {
        return new HTML5BrowserWindow(startURL, "");
    }

    @Override
    public void nativeBrowserWindowShow(Object window) {
        ((HTML5BrowserWindow)window).show();
    }

    @Override
    public void nativeBrowserWindowCleanup(Object window) {
        ((HTML5BrowserWindow)window).cleanup();
    }

    @Override
    public void nativeBrowserWindowSetSize(Object window, int width, int height) {
        ((HTML5BrowserWindow)window).setSize(width, height);
    }

    @Override
    public void nativeBrowserWindowSetTitle(Object window, String title) {
        ((HTML5BrowserWindow)window).setTitle(title);
    }

    @Override
    public void nativeBrowserWindowHide(Object window) {
        ((HTML5BrowserWindow)window).hide();
    }

    @Override
    public void nativeBrowserWindowAddCloseListener(Object window, ActionListener l) {
        ((HTML5BrowserWindow)window).addCloseListener(l);
    }

    @Override
    public void nativeBrowserWindowRemoveCloseListener(Object window, ActionListener l) {
        ((HTML5BrowserWindow)window).removeCloseListener(l);
    }

    @Override
    public void addNativeBrowserWindowOnLoadListener(Object window, ActionListener l) {
        ((HTML5BrowserWindow)window).addLoadListener(l);
    }

    @Override
    public void removeNativeBrowserWindowOnLoadListener(Object window, ActionListener l) {
        ((HTML5BrowserWindow)window).removeLoadListener(l);
    }

    @Override
    public void nativeBrowserWindowEval(Object window, BrowserWindow.EvalRequest req) {
        ((HTML5BrowserWindow)window).eval(req);
    }
    
    
    
    
    
    
    
    
    
    

    @Override
    public void browserExecute(PeerComponent browserPeer, String javaScript) {
        ((HTML5BrowserComponent)browserPeer).execute(javaScript);
    }

    public boolean supportsBrowserExecuteAndReturnString(PeerComponent internal) {
        return true;
    }
    
    @Override
    public String browserExecuteAndReturnString(PeerComponent internal, String javaScript) {
        return ((HTML5BrowserComponent)internal).executeAndReturnString(javaScript);
    }

    @Override
    public boolean browserHasBack(PeerComponent browserPeer) {
        return ((HTML5BrowserComponent)browserPeer).hasBack();
    }

    @Override
    public void browserBack(PeerComponent browserPeer) {
        ((HTML5BrowserComponent)browserPeer).back();
    }
    
    

    @Override
    public boolean browserHasForward(PeerComponent browserPeer) {
        return ((HTML5BrowserComponent)browserPeer).hasForward();
    }

    @Override
    public void browserForward(PeerComponent browserPeer) {
        ((HTML5BrowserComponent)browserPeer).forward();
    }

    @Override
    public void browserClearHistory(PeerComponent browserPeer) {
        
    }

    
    
    @Override
    public void browserDestroy(PeerComponent internal) {
        
    }

    @Override
    public void browserStop(PeerComponent browserPeer) {
        
    }

    @Override
    public Object createSoftWeakRef(Object o) {
        if (Display.getInstance().getProperty("javascript.useES6WeakRefs", "true").equals("true")
                && isWeakMapSupported()
                && (o == null || o instanceof JSObject)) {
            if (o == null) {
                return new JSObjectWrapper();
            }
            JSObject key = createSoftWeakRefImpl((JSObject)o);
            JSObjectWrapper keyOut = new JSObjectWrapper();
            keyOut.o = key;
            return keyOut;
        } else {
            return super.createSoftWeakRef(o);
        }
    }

    @Override
    public Object extractHardRef(Object o) {
        if (Display.getInstance().getProperty("javascript.useES6WeakRefs", "true").equals("true") && isWeakMapSupported()) {
            if (o==null || !(o instanceof JSObjectWrapper)) {
                return null;
            }
            JSObjectWrapper w = (JSObjectWrapper)o;
            
            return w.o == null ? null : extractHardRefImpl(w.o);
        } else {
            return super.extractHardRef(o);
        }
    }
    
    
    
    private static class JSObjectWrapper {
        JSObject o;
    }
    
    @JSBody(params={}, script="return window.WeakMap !== undefined;")
    private static native boolean isWeakMapSupported();
    
    @JSBody(params={"o"}, script="var key={}; window.cn1GlobalWeakMap.set(key, o); return key;")
    private native static JSObject createSoftWeakRefImpl(JSObject o);
    
    @JSBody(params={"key"}, script="return window.cn1GlobalWeakMap.has(key) ? window.cn1GlobalWeakMap.get(key) : null")
    private native static JSObject extractHardRefImpl(JSObject key);
    
    @JSBody(params={}, script="return window.cn1IsPreview === true")
    private native static boolean isPreview_();

    @Override
    public void setBrowserPageInHierarchy(PeerComponent browserPeer, String url) throws IOException {
        if (url.length() > 0 && url.charAt(0) != '/') {
            url = "/" + url;
                   
        }
        setBrowserURL(browserPeer, "assets/cn1html"+url);
    }

    
    
    @Override
    public void setBrowserURL(PeerComponent browserPeer, String url) {
        if (url.startsWith("jar:")) {
            url = url.substring(6);
            while (url.indexOf("/") == 0) {
                url = url.substring(1);
            }
            
            String currPath = window.getLocation().getPathname();
            String dirPath = currPath;
            if (dirPath.indexOf("/") != -1){
                dirPath = dirPath.substring(0, dirPath.lastIndexOf("/"));
            }
            
            
            url = ((WindowLocation)window.getLocation()).getOrigin()+
                    dirPath + "/assets/" + url;
            
            if (isPreview_()) {
                try {
                    InputStream resource = Display.getInstance().getResourceAsStream(null, url.substring(url.lastIndexOf("/")));
                    String str = Util.readToString(resource);
                    Util.cleanup(resource);
                    setBrowserPage(browserPeer, str, url);
                    return;
                } catch (IOException ex) {
                    //Log.e(ex);
                    consoleLog("Error in setBrowserURL");
                    consoleLog(ex.getMessage());
                }
            }

            
        } else if (url.startsWith("file://")) {
            
        }
        ((HTML5BrowserComponent)browserPeer).setURL(url);
    }

    @Override
    public void setBrowserPage(PeerComponent browserPeer, String html, String baseUrl) {
        ((HTML5BrowserComponent)browserPeer).setPage(html, baseUrl);
    }

    @Override
    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
        ((HTML5BrowserComponent)browserPeer).setProperty(key, value);
    }

    @Override
    public String getBrowserURL(PeerComponent browserPeer) {
        return ((HTML5BrowserComponent)browserPeer).getURL();
    }

    @Override
    public String getBrowserTitle(PeerComponent browserPeer) {
        return "Untitled Page";
    }

    @Override
    public void browserReload(PeerComponent browserPeer) {
        ((HTML5BrowserComponent)browserPeer).reload();
    }
    
    private String[] mediaExtensions = new String[]{
      "mp4", "mpg", "mov", "aiff", "mp3", "mpeg"  
    };
    private boolean isMediaResource(String resource){
        for (String ext : mediaExtensions){
            if (resource.endsWith("."+ext)){
                return true;
            }
        }
        return false;
    }
    
    public InputStream getStream(String url){
        return getArrayBufferInputStream(url);
        
         
    }
    
    static String arrayBufferToDataURL(ArrayBuffer buf, String type){
        return "data:"+type+";base64,"+((WindowExt)Window.current()).arrayBufferToBase64(buf);
    }
    
    static String blobToDataURL(Blob blob){
        return BlobUtil.blobToBase64(blob);
    }
    /*
    static void blobToDataURL(Blob blob, final AsyncCallback<String> callback){
        ((WindowExt)JS.getGlobal()).BlobToBase64(blob, new DataURLCallback(){

            @Override
            public void callback(String str) {
                callback.complete(str);
            }
            
        });
    }
    */
    
    private String buildVersion;
    
    @JSBody(script="return (typeof document !== 'undefined' && document.documentElement) ? document.documentElement.getAttribute('data-cn1-app-version') : null")
    private native static String getBuildVersion_();
    
    public String getBuildVersion() {
        if (buildVersion == null) {
            buildVersion = getBuildVersion_();
        }
        if (buildVersion == null) {
            buildVersion = Display.getInstance().getProperty("AppVersion", "1.0");
        }
        return buildVersion;
    }

    @JSBody(script="try {history.pushState(\"jibberish\", null, null)} catch (e){console.log('history.pushState not supported. Back command will not work.')}")
    private native static void pushHistoryState();
    
    @Override
    public void setCurrentForm(Form f) {
        super.setCurrentForm(f);
        pushHistoryState();
        
    }
    
    
    
    public InputStream getArrayBufferInputStream(String url){
        String dataURL = ((WindowExt)window).getCn1().getBundledAssetAsDataURL(url);
        if (dataURL != null) {
            Blob blob = ((WindowExt)window).Base64ToBlob(dataURL);
            ArrayBufferInputStream out;
            try {
                out = new ArrayBufferInputStream(BlobUtil.toUint8Array(blob), "application/octet-stream");
                return out;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
        }
        
        if (isMediaResource(url)){
            ArrayBufferInputStream out = new ArrayBufferInputStream(Uint8Array.create(0), null);
            out.setSrc(url);
            return out;
        }
        final XMLHttpRequest req = XMLHttpRequest.create();
        if (url.indexOf("assets/") == 0 && url.indexOf("?") == -1) {
            url = url + "?v=" + getBuildVersion();
        }
        req.open("get", url, false);
        req.overrideMimeType("text/plain; charset=x-user-defined");
        req.send();

        Uint8Array responseBytes = toResponseBytes(req);
        if (responseBytes == null) {
            System.out.println(req.getAllResponseHeaders());
            System.out.println(req.getStatusText());
            System.out.println("Failed to load resource "+url);
            System.out.println("Status code was "+req.getStatus());
            return null;
        }
        
        ArrayBufferInputStream out = new ArrayBufferInputStream(responseBytes, req.getResponseType());
        return out;
    }

    private Uint8Array toResponseBytes(XMLHttpRequest req) {
        if ("arraybuffer".equals(req.getResponseType()) && req.getResponse() != null) {
            return Uint8Array.create((ArrayBuffer)req.getResponse());
        }
        String responseText = req.getResponseText();
        if (responseText == null) {
            return null;
        }
        Uint8Array out = Uint8Array.create(responseText.length());
        for (int i = 0; i < responseText.length(); i++) {
            out.set(i, (short)(responseText.charAt(i) & 0xff));
        }
        return out;
    }
        
    
    @JSBody(params={"resource"}, script="cn1LoadedFile(resource)")
    private native static void notifyProgressLoaderThatResourceIsLoaded(String resource);
    
    @Override
    public InputStream getResourceAsStream(Class cls, String resource)  {
        if (resource == null || resource.length() == 0 || "null".equals(resource)) {
            return null;
        }
        int lastSlash = resource.lastIndexOf("/");
        if ( lastSlash >= 0 ){
            resource = resource.substring(lastSlash+1);
        }
        // The ParparVM translator emits the app's own theme.res /
        // CN1Resource.res at the bundle root, while assets/ holds the merged
        // system themes (iOS7Theme.res, tzone_theme.res, etc.). The default
        // path below always prepends assets/, which meant
        // Resources.openLayered("/theme") used to return the big system
        // theme and the app's theme.css-compiled overrides (Style UIIDs like
        // TabsColorSync) were silently discarded. Try the root first for the
        // two known app-level bundle entries; everything else keeps the
        // assets/ prefix so iOS7Theme.res etc. still resolve correctly.
        if ("theme.res".equals(resource) || "CN1Resource.res".equals(resource)) {
            InputStream rootStream = getStream(resource);
            if (rootStream != null) {
                notifyProgressLoaderThatResourceIsLoaded(resource);
                return rootStream;
            }
        }
        if (!"icon.png".equals(resource)) {
            resource = "assets/"+resource;
        }
        InputStream out = getStream(resource);
        notifyProgressLoaderThatResourceIsLoaded(resource);
        return out;

    }

    @JSBody(script="jQuery(\"div#cn1-splash\").fadeOut(100, function(){ jQuery(this).remove(); });")
    private native static void hideSplash();
    
    @JSBody(script="window.setTimeout(function(){if (!window.loadServiceWorker) return; window.loadServiceWorker()}, 1000);")
    private native static void loadServiceWorker();
    
    @Override
    public void confirmControlView() {
        super.confirmControlView();
        hideSplash();
        loadServiceWorker();
    }
    
    public static void registerSaveBlobToFile() {
        BlobUtil.registerNativeBlobToFileConverter();
    }
    
    private NativeImage createNativeImage(byte[] bytes, int offset, int len){
        Uint8Array arr = Uint8Array.create(len);
        for (int i=0; i<len; i++){
            arr.set(i, bytes[i+offset]);
        }
        Blob blob = BlobUtil.createBlob(arr, "image/png");
        NativeImage nimg = new NativeImage();
        nimg.img = renderingBackend.createBlobImageElement(blob);
        nimg.load();
        return nimg;
    }

    @JSBody(params={"str"}, script="return encodeURIComponent(str)")
    private native static String encodeURIComponent(String str);
    
    @Override
    public void sendMessage(String[] recipients, String subject, Message msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("mailto:");
        boolean first = true;
        for (String recipient : recipients) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(recipient);
        }
        sb.append("?");
        if (subject != null) {
            sb.append("subject=").append(encodeURIComponent(subject));
        }
        if (msg != null && msg.getContent() != null) {
            sb.append("&body=").append(encodeURIComponent(msg.getContent()));
        }
        final JSObject beforeUnloadHandler = getBeforeUnloadHandler();
        removeBeforeUnload();
        Window.setTimeout(new TimerHandler() {
            @Override
            public void onTimer() {
                setBeforeUnloadHandler(beforeUnloadHandler);
            }
            
        }, 1000);
        Window.current().getLocation().replace(sb.toString());
        
    }

    
    
    @Override
    public boolean isScaledImageDrawingSupported() {
        return true;
    }
    
    
    
    public class NativeImage {
        HTMLImageElement img;
        int width;
        int height;
        boolean loaded;
        boolean error;
        final JavaScriptAsyncImageLoadCoordinator.State loadState = new JavaScriptAsyncImageLoadCoordinator.State();
        HTML5Graphics mutableGraphics;
        CanvasPattern pattern;
        boolean doNotRepaint = false;
        private final JavaScriptNativeImageAdapter.ImageModel imageModel = new JavaScriptNativeImageAdapter.ImageModel() {
            @Override
            public int getExplicitWidth() {
                return width;
            }

            @Override
            public int getExplicitHeight() {
                return height;
            }

            @Override
            public boolean hasLoadedImage() {
                return img != null && loaded;
            }

            @Override
            public int getLoadedImageWidth() {
                return img.getNaturalWidth();
            }

            @Override
            public int getLoadedImageHeight() {
                return img.getNaturalHeight();
            }

            @Override
            public boolean hasMutableSurface() {
                return mutableGraphics != null;
            }

            @Override
            public int getMutableSurfaceWidth() {
                return mutableGraphics.getCanvas().getWidth();
            }

            @Override
            public int getMutableSurfaceHeight() {
                return mutableGraphics.getCanvas().getHeight();
            }

            @Override
            public Object getPatternCache() {
                return pattern;
            }

            @Override
            public void setPatternCache(Object patternCache) {
                pattern = (CanvasPattern)patternCache;
            }
        };
        public HTMLImageElement getImg(){
            return img;
        }
        
        public HTML5Graphics getMutableGraphics() {
            return mutableGraphics;
        }

        JavaScriptNativeImageAdapter.ImageModel getImageModel() {
            return imageModel;
        }

        public void setSuppressRepaint(boolean suppress) {
            doNotRepaint = suppress;
            loadState.setSuppressRepaint(suppress);
        }
        
        public void load(){
            ImageExt imageExt = (ImageExt)img;
            if (img!=null && imageExt.isComplete()){
                if (JavaScriptAsyncImageLoadCoordinator.handleImmediateCompletion(loadState, img.getNaturalWidth(), img.getNaturalHeight())) {
                    loaded = loadState.isLoaded();
                    error = loadState.isError();
                    width = loadState.getWidth();
                    height = loadState.getHeight();
                    return;
                }
                loaded = loadState.isLoaded();
                error = loadState.isError();
            }
            if ( error || !loaded && img != null){
                final Object lock = new Object();
                JavaScriptAsyncImageLoadCoordinator.beginLoading(loadState);
                loaded = false;
                error = false;
                if (!loadState.areListenersInstalled()) {
                    loadState.setListenersInstalled(true);
                    img.addEventListener("load", new EventListener(){

                        @Override
                        public void handleEvent(Event evt) {
                            new Thread(){

                                @Override
                                public void run() {
                                    JavaScriptAsyncImageLoadCoordinator.handleLoad(loadState, img.getNaturalWidth(), img.getNaturalHeight());
                                    loaded = loadState.isLoaded();
                                    error = loadState.isError();
                                    width = loadState.getWidth();
                                    height = loadState.getHeight();
                                    if (JavaScriptAsyncImageLoadCoordinator.shouldRepaintOnLoad(loadState)) {
                                        Display.getInstance().callSerially(new Runnable() {
                                            @Override
                                            public void run() {
                                                renderingBackend.repaintCurrentForm();
                                            }
                                        });
                                    }
                                    synchronized(lock){
                                        lock.notifyAll();
                                    }
                                }
                                
                            }.start();
                        }
                        
                    }, false);
                    img.addEventListener("error", new EventListener(){

                        @Override
                        public void handleEvent(final Event evt) {
                            new Thread(){

                                @Override
                                public void run() {
                                    JavaScriptAsyncImageLoadCoordinator.handleError(loadState);
                                    loaded = loadState.isLoaded();
                                    error = loadState.isError();
                                    synchronized(lock){
                                        lock.notifyAll();
                                    }
                                }
                                
                            }.start();
                        }
                        
                    }, false);
                }
                loaded = loadState.isLoaded();
                error = loadState.isError();
               
            }
        }
        
        public int getWidth(){
            return JavaScriptNativeImageAdapter.resolveWidth(imageModel);
        }
        
        public int getHeight(){
            return JavaScriptNativeImageAdapter.resolveHeight(imageModel);
        }
        
        public void draw(CanvasRenderingContext2D ctx, int x, int y, int width, int height){
            JavaScriptNativeImageAdapter.draw(imageModel, new JavaScriptNativeImageAdapter.DrawTarget() {
                @Override
                public void drawLoadedImage(int drawX, int drawY, int drawWidth, int drawHeight) {
                    renderingBackend.drawLoadedImage(ctx, img, drawX, drawY, drawWidth, drawHeight);
                }

                @Override
                public void drawMutableSurface(int drawX, int drawY, int drawWidth, int drawHeight) {
                    renderingBackend.drawMutableSurface(ctx, mutableGraphics.getCanvas(), drawX, drawY, drawWidth, drawHeight);
                }
            }, x, y, width, height);
        }
        
        public void tile(CanvasRenderingContext2D ctx, int x, int y, int width, int height) {
            JavaScriptNativeImageAdapter.tile(imageModel, new JavaScriptNativeImageAdapter.TileTarget() {
                @Override
                public Object createLoadedImagePattern() {
                    return renderingBackend.createLoadedImagePattern(ctx, img);
                }

                @Override
                public Object createMutableSurfacePattern() {
                    return renderingBackend.createMutableSurfacePattern(ctx, mutableGraphics.getCanvas());
                }

                @Override
                public void paintPattern(Object pattern, int tileX, int tileY, int tileWidth, int tileHeight) {
                    ctx.setFillStyle((CanvasPattern)pattern);
                    ctx.save();
                    ctx.translate(tileX, tileY);
                    ctx.fillRect(0, 0, tileWidth, tileHeight);
                    ctx.restore();
                }
            }, x, y, width, height);
        }
        
        public void draw(CanvasRenderingContext2D ctx, int x, int y){
            draw(ctx, x, y, getWidth(), getHeight());
            
        }
            
    }
        
    
    interface URLBuilderFactory extends JSObject {
        @JSProperty
        URLBuilder getURL();
        
        @JSProperty 
        URLBuilder getWebkitURL();
        
        
    }
    
    interface URLBuilder extends JSObject {
        String createObjectURL(Blob blob);
    }
    
    
    
      
    
      
    
    interface TouchEvent extends Event {
        @JSProperty
        JSArray<MouseEvent> getTargetTouches();
        
        @JSProperty
        int getClientX();
        
        @JSProperty
        int getClientY();
        
    }


    interface Uint8ClampedArraySetter extends JSObject {
        void set(Uint8ClampedArray arr);
    }
    
    Map<String,Map<Character,Integer>> charWidthCache = new HashMap<String,Map<Character,Integer>>();
    Map<String,Map<String,Integer>> stringWidthCache = new HashMap<String,Map<String,Integer>>();
    private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghizjlmnopqrstuvwxyz12345678980";
    
    // Note that we use a special case for the font height of Material fonts to
    // try to optimize the result for FontImages and rotation.
    // https://github.com/codenameone/CodenameOne/issues/2631
    @JSBody(params={"fontStyle"}, script="window.cn1_font_height_cache = window.cn1_font_height_cache || {};\n"
            + "var style = String(fontStyle == null ? '' : fontStyle);\n"
            + "var result = window.cn1_font_height_cache[style];\n"
            + "if (result == null) {\n"
            + "  var match = /([0-9.]+)(?=pt|px)/.exec(style);\n"
            + "  var fontSize = match ? parseFloat(match[1]) : 16;\n"
            + "  if (style.indexOf('Material') !== -1) {\n"
            + "    result = fontSize;\n"
            + "  } else {\n"
            + "    result = Math.ceil(fontSize * 1.2);\n"
            + "  }\n"
            + "  window.cn1_font_height_cache[style] = result;\n"
            + "}\n"
            + "return result;")
    private native static double determineFontHeight(String fontStyle);
    
    @JSBody(params={"fontStyle"}, script="window.cn1_font_leading_cache = window.cn1_font_leading_cache || {};\n"
            + "var style = String(fontStyle == null ? '' : fontStyle);\n"
            + "var result = window.cn1_font_leading_cache[style];\n"
            + "if (result == null) {\n"
            + "  var match = /([0-9.]+)(?=pt|px)/.exec(style);\n"
            + "  var fontSize = match ? parseFloat(match[1]) : 16;\n"
            + "  var fontHeight = (style.indexOf('Material') !== -1) ? fontSize : Math.ceil(fontSize * 1.2);\n"
            + "  result = Math.max(0, fontHeight - fontSize);\n"
            + "  window.cn1_font_leading_cache[style] = result;\n"
            + "}\n"
            + "return result;")
    private native static double determineFontLeading(String fontStyle);
    
    @JSBody(params={}, script="return window.cn1_use_baseline_text_rendering || false;")
    public native static boolean useBaselineTextRendering();
    
    public class NativeFont {
        //String css;
        int face;
        int style;
        int size;
        int ascent;
        double height;
        String fileName;
        String fontName;
        
        String cssCached_, cssFontFamilyCached__;
        
        public int fontLeading() {
            return (int)Math.ceil(determineFontLeading(getCSS()));
        }
        
        public String getCSSFontFamily() {
            if (cssFontFamilyCached__ == null) {
                StringBuilder sb = new StringBuilder();
                if (fontName != null) {
                    if (fontName.startsWith("native-")) {
                        sb.append(fontFamily()).append(", ");
                    } else {
                        sb.append("'").append(fontName).append("'").append(", ");
                    }
                }
                switch (face) {
                    case Font.FACE_SYSTEM:
                        sb.append("sans-serif");
                        break;
                    case Font.FACE_PROPORTIONAL:
                        sb.append("serif");
                        break;
                    case Font.FACE_MONOSPACE:
                        sb.append("monospace");
                        break;
                        
                }
                cssFontFamilyCached__ = sb.toString();
            }
            return cssFontFamilyCached__;
        }
        
        private String fontStyle() {

   
            if ("native-ItalicThin".equals(fontName) || "native-ItalicLight".equals(fontName) || "native-ItalicRegular".equals(fontName) || "native-ItalicBold".equals(fontName) || (style & Font.STYLE_ITALIC) != 0) {
                return "italic";
            }
    
            return "";
        }
        
        private String fontFamily() {
            if (fontName.startsWith("native-")) {
                if ("native-MainThin".equals(fontName) || "native-ItalicThin".equals(fontName)) {
                    return "'HelveticaNeue-UltraLight', 'HelveticaNeue UltraLight', Sans-serif";
                }
                if ("native-MainLight".equals(fontName) || "native-ItalicLight".equals(fontName)) {
                    return "'HelveticaNeue-Thin', 'HelveticaNeue Thin', Sans-serif";
                }
                if ("native-MainRegular".equals(fontName) || "native-ItalicRegular".equals(fontName)) {
                    return "'HelveticaNeue-Medium', 'HelveticaNeue Medium', Sans-serif";
                }
                if ("native-MainBold".equals(fontName) ||  "native-ItalicBold".equals(fontName)) {
                    return "'HelveticaNeue-Bold', 'HelveticaNeue Bold', Sans-serif";
                }
                if ("native-MainBlack".equals(fontName) || "native-ItalicBlack".equals(fontName)) {
                    return "'HelveticaNeue-Black', 'HelveticaNeue Black', Sans-serif";
                }
            }
            return fontName;
        }
        
        private String fontWeight() {
            
            if ("native-MainThin".equals(fontName)) {
                /*font-weight:100; 
                font-stretch:condensed;*/
                return "100";
            }

            


            if ("native-MainLight".equals(fontName)) {
                /*font-weight:300; 
                font-stretch:condensed;*/
                return "300";
            }
    
            if ("native-MainRegular".equals(fontName)) {
                /*font-weight:500; 
                font-stretch:normal;*/
                return "500";
            }
    
            if ("native-MainBold".equals(fontName)) {
                /*font-weight:600; 
                font-stretch:normal;*/
                return "600";
   
            }
    
            if ("native-MainBlack".equals(fontName)) {
                /*font-weight:800; 
                font-stretch:condensed;*/
                return "800";
            }
   
            if ("native-ItalicThin".equals(fontName)) {
                    /*font-weight:100; 
                    font-stretch:condensed;
                    font-style: italic;*/
                return "100";
            }
    
            if ("native-ItalicLight".equals(fontName)) {
                return "300";
            }
    
            if ("native-ItalicRegular".equals(fontName)) {
                return "500";
            }
    
            if ("native-ItalicBold".equals(fontName)) {
                return "600";
            }
    
            if ((style & Font.STYLE_BOLD) != 0) {
                return "bold";
            }
            
            return "";
        }
        
        public String getCSS(){
            if (cssCached_ == null) {
                StringBuilder sb = new StringBuilder();
                //sb.append(height).append("px ");
                //if ((style & Font.STYLE_ITALIC) != 0 || fontName != null && fontName.contains("Italic")) {
                //    sb.append("italic ");
                //}
                sb.append(fontStyle()).append(" ");
                sb.append(fontWeight()).append(" ");
                //if ((style & Font.STYLE_BOLD) != 0 || fontName != null && fontName.contains("Bold")){
                //    sb.append("bold ");
                //}
                if (((int)height) ==0) {
                    height = defaultFont.height;
                }
                sb.append(height).append("px/1.0 ");
                sb.append(getCSSFontFamily());
                cssCached_ = sb.toString();
                
            }
            return cssCached_;
        }
        
        public String getScaledCSS(){
            
            StringBuilder sb = new StringBuilder();
            //sb.append(height).append("px ");
            //if ((style & Font.STYLE_ITALIC) != 0) {
            //    sb.append("italic ");
            //}
            sb.append(fontStyle()).append(" ");
            //if ((style & Font.STYLE_BOLD) != 0 ){
            //    sb.append("bold ");
            //}
            sb.append(fontWeight()).append(" ");
            if (((int)height) ==0) {
                height = defaultFont.height;
            }
            sb.append(scaleCoord(height)).append("px/1.0 ");
            sb.append(getCSSFontFamily());
            //if (fontName != null) {
            //    sb.append(fontName).append(", ");
            //}
            //switch (face) {
            //    case Font.FACE_SYSTEM:
            //        sb.append("sans-serif");
            //        break;
            //    case Font.FACE_PROPORTIONAL:
            //        sb.append("serif");
            //        break;
            //    case Font.FACE_MONOSPACE:
            //        sb.append("monospace");
            //        break;

            //}
            return sb.toString();
              
        }
        
        public String toString(){
            return getCSS()+" (Face: "+face+" style "+style+" size "+size+")";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NativeFont)) {
                return false;
            }
            NativeFont other = (NativeFont)obj;
            return face == other.face
                    && style == other.style
                    && size == other.size
                    && Double.doubleToLongBits(height) == Double.doubleToLongBits(other.height)
                    && java.util.Objects.equals(fileName, other.fileName)
                    && java.util.Objects.equals(fontName, other.fontName);
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = 31 * hash + face;
            hash = 31 * hash + style;
            hash = 31 * hash + size;
            long heightBits = Double.doubleToLongBits(height);
            hash = 31 * hash + (int)(heightBits ^ (heightBits >>> 32));
            hash = 31 * hash + (fileName != null ? fileName.hashCode() : 0);
            hash = 31 * hash + (fontName != null ? fontName.hashCode() : 0);
            return hash;
        }
        
        
        
        public int charWidth(char c){
            Map<Character,Integer> cache = charWidthCache.get(getCSS());
            if (cache == null){
                cache = new HashMap<Character,Integer>();
                charWidthCache.put(getCSS(), cache);
            }
            Character ch = new Character(c);
            Integer i = cache.get(ch);
            if (i != null){
                return i.intValue();
            }
            int w = graphics.charsWidth(this, new char[]{c},0,1);
            cache.put(ch, new Integer(w));
            return w;
        }
        
        public int stringWidth(String str){
            if (str.length() < 50){
                Map<String,Integer> cache = stringWidthCache.get(getCSS());
                if (cache == null){
                    cache = new HashMap<String,Integer>();
                    stringWidthCache.put(getCSS(), cache);
                }

                Integer i = cache.get(str);
                if (i != null){
                    return i.intValue();
                }
                int w = graphics.stringWidth(this, str);
                cache.put(str, new Integer(w));
                return w;
            } else {
                return graphics.stringWidth(this, str);
            }
        }
        
       
        public int fontHeight(){
            //return (int)Math.round(height);
            //return graphics.getFontHeight(this);
            return (int)Math.ceil(determineFontHeight(getCSS()));
            /*
            Integer h = fontHeightCache.get(css);
            if (h == null){
                int height = graphics.getFontHeight(this);
                h = new Integer(height);
                fontHeightCache.put(css, h);
            }
            return h.intValue();
                    */
              
            
        }
        
        public int fontAscent() {
            if (ascent == 0) {
                HTMLCanvasElement canvas = getCanvasBuffer(100,100);
                CanvasRenderingContext2D context = (CanvasRenderingContext2D)canvas.getContext("2d");
                String oldFont = context.getFont();
                context.setFont(((NativeFont)this).getCSS());
                //this.canvas.getStyle().setProperty("font", nativeFont+"");
                //ascent = (int)Math.round(((JSOImplementations.JSFontMetrics)context.measureText(alphabet)).getAscent());
                ascent = (int)((fontHeight()-fontLeading()) * measureAscent(getCSSFontFamily()));
                //context.setFont(oldFont);
                
            }
            return ascent;
        }
        
        
    }
    
    @JSBody(params={"fontFamily"}, script="var family = String(fontFamily == null ? '' : fontFamily);\n"
            + "if (typeof window.measureTextAscent === 'function') {\n"
            + "  return window.measureTextAscent(family);\n"
            + "}\n"
            + "if (family.indexOf('Material') !== -1) {\n"
            + "  return 0.86;\n"
            + "}\n"
            + "return 0.8;")
    native static double measureAscent(String fontFamily);
    @JSBody(params={"fontFamily"}, script="var family = String(fontFamily == null ? '' : fontFamily);\n"
            + "if (typeof window.measureTextDescent === 'function') {\n"
            + "  return window.measureTextDescent(family);\n"
            + "}\n"
            + "if (family.indexOf('Material') !== -1) {\n"
            + "  return 0.14;\n"
            + "}\n"
            + "return 0.2;")
    native static double measureDescent(String fontFamily);
    
    static void _log(String str){
        ((WindowExt)instance.window).getConsole().log(str);
    }
    
    
    @JSBody(params={"obj"}, script="console.log(obj)")
    native static void _logInt(int obj);
    
    
    


    @Override
    public Database openOrCreateDB(String databaseName) throws IOException {
        WebSQL.Database db = WebSQL.openDatabase(databaseName, "1.0", databaseName, defaultFileSystemSize);
        return new DatabaseImpl(db);
    }

    @Override
    public void startThread(String name, final Runnable r) {
        
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    r.run();
                } catch (Exception ex) {
                    CodenameOneThread.handleException(ex);
                }
            }
            
                
        }, name);
        t.start();
    }
    
    
    
    
    
    
    private static HTMLButtonElement clickBtn;
    
    public static void showButton(String text, final EventListener onClick) {
        final Window win = Window.current();
        EventListener l = new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                
                if (clickBtn != null) {
                    clickBtn.getParentNode().removeChild(clickBtn);
                    clickBtn = null;
                }
                onClick.handleEvent(evt);
            }
            
        };
        
        HTMLButtonElement btn = showButton_(text, l);
        btn.getStyle().setProperty("position", "absolute");
        btn.getStyle().setProperty("top", "0");
        btn.getStyle().setProperty("left", "0");
        btn.getStyle().setProperty("width", ""+scaleCoord(HTML5Implementation.instance.canvas.getWidth())+"px");
        btn.getStyle().setProperty("height", ""+scaleCoord(HTML5Implementation.instance.canvas.getHeight())+"px");
        btn.getStyle().setProperty("padding", "0");
        btn.getStyle().setProperty("margin", "0");
        btn.getStyle().setProperty("font-size", "2em");
        btn.getStyle().setProperty("opacity", "0.7");
        
        clickBtn = btn;
    }
    
    // Use jQuery's .text() to set the label — concatenating label into the
    // <button> markup would let any HTML tags or script inside label run as
    // markup (XSS on user-controlled text).
    @JSBody(params={"label","l"}, script=
            "return jQuery('<button class=\"btn btn-default\"></button>')"
            + ".text(label).click(l).appendTo(jQuery('body')).get(0);")
    private native static HTMLButtonElement showButton_(String label, EventListener l);

    @Override
    public void registerPush(Hashtable metaData, boolean noFallback) {
        HTML5Push.registerPush();
    }
    
    void _sendPushRegistrationError(String message, int errorCode) {
        sendPushRegistrationError(message, errorCode);
    }
    
    void _registerServerPush(String id) {
        if (registerServerPush(id, getApplicationKey(), (byte)10, "", getPackageName())) {
            super.sendRegisteredForPush(id);
        } else {
            super.sendPushRegistrationError("Failed to register server push", 0);
        }
    }
    
    void _pushReceived(String data) {
        super.pushReceived(data);
    }

    private static interface BeforeInstallPromptEvent extends Event {
        public void prompt();
        
        @JSProperty
        public UserChoiceResultPromise getUserChoice();
    }
    
    private static interface UserChoiceResultPromise extends JSObject {
        public void then(UserChoiceCallback callback);
    }
    
    @JSFunctor
    private static interface UserChoiceCallback extends JSObject {
        public void onResult(UserChoiceResult result);
    }
    
    private static interface UserChoiceResult extends JSObject {
        @JSProperty
        public String getOutcome();
    }
    
    private static final String USER_CHOICE_OUTCOME_ACCEPTED="accepted";
    
    private BeforeInstallPromptEvent deferredPromptForInstall;
    
    @Override
    public boolean canInstallOnHomescreen() {
        return deferredPromptForInstall != null;
    }

    @Override
    public boolean promptInstallOnHomescreen() {
        if (!canInstallOnHomescreen()) {
            return false;
        }
        final boolean[] res = new boolean[1];
        if (!CN.isEdt()) {
            
            CN.callSeriallyAndWait(new Runnable() {
                public void run() {
                    res[0] = promptInstallOnHomescreen();
                }
            });
            return res[0];
        }
        
        // Now we know we're on the EDT
        
        if (canInstallOnHomescreen()) {
            CN.invokeAndBlock(new Runnable() {
                public void run() {
                    final boolean[] result = new boolean[1];
                    final boolean[] complete = new boolean[1];
                    deferredPromptForInstall.prompt();
                    deferredPromptForInstall.getUserChoice().then(new UserChoiceCallback() {
                        @Override
                        public void onResult(final UserChoiceResult choice) {
                            new Thread(new Runnable() {
                                public void run() {
                                    deferredPromptForInstall = null;

                                    if (USER_CHOICE_OUTCOME_ACCEPTED.equals(choice.getOutcome())) {
                                        result[0] = true;
                                    }
                                    synchronized(complete) {
                                        complete[0] = true;
                                        complete.notifyAll();
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
                    res[0] = result[0];
                    
                }
                
            });
            
            
        }
        return res[0];
    }

    
    
    @Override
    public void onCanInstallOnHomescreen(final Runnable r) {
        Window.current().addEventListener("beforeinstallprompt", new EventListener<BeforeInstallPromptEvent>() {
            @Override
            public void handleEvent(BeforeInstallPromptEvent evt) {
                deferredPromptForInstall = evt;
                evt.preventDefault();
                new Thread(new Runnable() {
                    public void run() {
                        CN.callSerially(r);
                    }
                }).start();
            }
            
        }, true);
    }
    
    @JSBody(params={"onComplete"}, script="if (!document.body.requestFullscreen) return false; document.body.requestFullscreen().then(function(){onComplete(true);}).catch(function(err){onComplete(false)}); return true;")
    private native static boolean requestFullScreen_(RequestFullScreenCallback onComplete);

    @JSFunctor
    private static interface RequestFullScreenCallback extends JSObject {
        public void onComplete(boolean result);
    
    }
    
    // These run in the ParparVM worker context where `document` is undefined.
    // Guard against ReferenceError before reading document.* — OrientationLockScreenshotTest
    // invokes CN.lockOrientation(...) → HTML5Implementation.canForceOrientation() →
    // isInFullScreenMode() → isFullScreen_(), which otherwise crashes the worker and
    // blocks the rest of the suite.
    @JSBody(params={}, script="return (typeof document !== 'undefined' && document.fullscreenElement) ? true : false")
    private native static boolean isFullScreen_();

    @JSBody(params={}, script="return (typeof document !== 'undefined' && document.body && document.body.requestFullscreen) ? true : false")
    private native static boolean isFullScreenSupported_();

    @Override
    public boolean isFullScreenSupported() {
        return isFullScreenSupported_();
    }
    
    
    
    @Override
    public boolean requestFullScreen() {
        if (isFullScreen_()) return true;
        if (!isFullScreenSupported_()) return false;
        
        final boolean[] complete = new boolean[1];
        final boolean[] res = new boolean[1];
        Button goFullScreenBtn = new Button("Click to Enter Fullscreen Mode");
        goFullScreenBtn.setMaterialIcon(FontImage.MATERIAL_FULLSCREEN);
        EventListener l = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                requestFullScreen_(new RequestFullScreenCallback() {
                    @Override
                    public void onComplete(final boolean result) {
                        new Thread(new Runnable() {
                           public void run() {
                               res[0] = result;
                                complete[0] = true;
                                synchronized(complete) {
                                    try {
                                        complete.notifyAll();
                                    } catch (Throwable t) {

                                    }
                                }
                           }
                        }).start();

                    }
                });
            }
            
        };
        showNativeButton(goFullScreenBtn, l);
        
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                while (!complete[0]) {
                    synchronized(complete) {
                        try {
                            complete.wait();
                        } catch (Throwable t){}
                    }
                }
            
            }
        });
        Log.p("Result of full-screen request was "+res[0]);
        return res[0];
        
    }

    @JSBody(params={"onComplete"}, script="if (!document.exitFullscreen) {onComplete(false); return;} document.exitFullscreen().then(function(){onComplete(true)}).catch(function(e){onComplete(false);});")
    private native static void exitFullscreen_(RequestFullScreenCallback onComplete);
    
    @Override
    public boolean exitFullScreen() {
        if (!isInFullScreenMode()) return true;
        if (!isFullScreenSupported_()) return true;
        final boolean[] complete = new boolean[1];
        final boolean[] res = new boolean[1];
        exitFullscreen_(new RequestFullScreenCallback() {
            @Override
            public void onComplete(final boolean result) {
                new Thread(new Runnable() {
                    public void run() {
                        res[0] = result;
                         complete[0] = true;
                         synchronized(complete) {
                             try {
                                 complete.notifyAll();
                             } catch (Throwable t) {

                             }
                         }
                    }
                 }).start();
            }
            
        });
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                while (!complete[0]) {
                    synchronized(complete) {
                        try {
                            complete.wait();
                        } catch (Throwable t){}
                    }
                }
            
            }
        });
        return res[0];
    }
    
    

    @Override
    public boolean isInFullScreenMode() {
        return isFullScreen_();
    }
    
    private HTMLButtonElement nativeBtn;
    public void showNativeButton(Button nativeButton, final EventListener eventHandler) {
        EventListener l = new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                if (nativeBtn != null) {
                    nativeBtn.getParentNode().removeChild(nativeBtn);
                    nativeBtn = null;
                }
                eventHandler.handleEvent(evt);
                evt.preventDefault();
                evt.stopPropagation(); 
            }
        };
        HTMLButtonElement btn = showButton_("", l);
        btn.getStyle().setProperty("position", "absolute");
        btn.getStyle().setProperty("top", "0");
        btn.getStyle().setProperty("left", "0");
        btn.getStyle().setProperty("width", ""+scaleCoord(canvas.getWidth())+"px");
        btn.getStyle().setProperty("height", ""+scaleCoord(canvas.getHeight())+"px");
        btn.getStyle().setProperty("padding", "0");
        btn.getStyle().setProperty("margin", "0");
        //btn.getStyle().setProperty("font-size", "2em");
        btn.getStyle().setProperty("opacity", "0.85");
         
        //((NativeImage)im.getImage()).load();
        //_logObj(((NativeImage)im.getImage()).getImg());
        nativeButton.setWidth(nativeButton.getPreferredW());
        nativeButton.setHeight(nativeButton.getPreferredH());
        HTMLCanvasElement cv = ((HTML5Implementation.NativeImage)nativeButton.toImage().getImage()).getMutableGraphics().getCanvas();
        cv.getStyle().setProperty("width", scaleCoord(nativeButton.getWidth())+"px");
        cv.getStyle().setProperty("height", scaleCoord(nativeButton.getHeight())+"px");
        btn.appendChild(cv);
        //btn.appendChild(((NativeImage)(nativeButton.toImage().getImage())).mutableGraphics.getCanvas());
        //btn.click();
        nativeBtn = btn;
        
    }
    
    
    public static HTMLInputElement createButton(String cssClass, String label) {
        HTMLDocument d = (HTMLDocument)Window.current().getDocument();
        HTMLInputElement el = (HTMLInputElement)d.createElement("button");
        //HTMLElement i = d.createElement("i");
        //i.setAttribute("class", "fa "+fontAwesomeClass+" fa-"+size);
        el.setAttribute("class", cssClass);
        
        //HTMLElement span = d.createElement("span");
        el.appendChild(d.createTextNode(label));
        //el.appendChild(i);
        //el.appendChild(span);
        return el;
        
    }
    
    @JSBody(params={}, script="return (window.screen && window.screen.orientation && window.screen.orientation.lock) ? true : false")
    private native static boolean supportsScreenOrientation_();
    
    @JSBody(params={"type"}, script="window.screen.orientation.lock(type)")
    private native static void lockOrientation_(String type);

    @Override
    public boolean canForceOrientation() {
        return isInFullScreenMode() && supportsScreenOrientation_();
    }

    @Override
    public void lockOrientation(boolean portrait) {
        if (canForceOrientation()) {
            lockOrientation_(portrait ? "portrait" : "landscape");
        } else {
            Log.p("lockOrientation not supported currently.  lockOrientation is only supported in some devices, and only when running in full-screen mode.");
        }
    }

    @Override
    public void unlockOrientation() {
        if (canForceOrientation()) {
            lockOrientation_("any");
        }
    }
    
    @JSBody(params={}, script="return (location.protocol == 'https:' && navigator.share !== undefined)")
    private native static boolean isNavigatorShareSupported_();

    @Override
    public boolean isNativeShareSupported() {
        return isNavigatorShareSupported_();
    }

    @JSBody(params={"url"}, script="navigator.share({text:'', url:url})")
    private native static void shareURL_(String url);
    
    @JSBody(params={"text"}, script="navigator.share({text:text, url:''})")
    private native static void shareText_(String text);
    
    @JSBody(params={"text", "link"}, script="navigator.share({text:text, url:link})")
    private native static void shareTextAndLink_(String text, String link);
    
    @Override
    public void share(final String text, final String image, String mimeType, Rectangle sourceRect) {
        if (isNavigatorShareSupported_()) {
            confirmDialog("Confirm Share", "The application has requested to share some content.  Click continue to proceed to the sharing dialog.", FontImage.MATERIAL_SHARE, "Continue", "Cancel", new JSRunnable() {
                @Override
                public void run() {
                    if (text != null && image != null && (image.startsWith("http://") || image.startsWith("https://"))) {
                        shareTextAndLink_(text, image);
                        return;
                    }
                    if (text != null && (text.startsWith("http://") || text.startsWith("https://"))) {
                        if (text.indexOf(" ") != -1) {
                            String url = text.substring(0, text.indexOf(" "));
                            String message = text.substring(url.length()+1);
                            shareTextAndLink_(message, url);
                            return;
                        }
                        shareURL_(text);
                        return;
                    }
                    if (text != null) {
                        shareText_(text);
                        return;
                    }
                    if (image != null && (image.startsWith("http://") || image.startsWith("https://"))) {
                        shareURL_(image);
                        return;
                    }
                            
                }
            });
        } else {
            super.share(text, image, mimeType, sourceRect);
        }
    }
    
    
    private static interface CancelableEvent extends Event {
        @JSProperty
        public boolean isDefaultPrevented();
    }
    
    

    /**
     * Creates a native confirm dialog that will run the given onOk runnable if the user clicks OK.  This is 
     * handy for things that need to be triggered by user interaction.
     * @param title Dialog title
     * @param message Message
     * @param icon Icon for the dialog
     * @param ok OK button label
     * @param cancel Cancel button label
     * @param onOk Callback to be run on JS main thread when user clicks OK.
     * @return 
     */
    private boolean confirmDialog(String title, String message, char icon, String ok, String cancel, final JSRunnable onOk) {
        InteractionDialog dlg = new InteractionDialog();
        dlg.setLayout(new BorderLayout());
        dlg.setTitle(title);
        dlg.add(BorderLayout.CENTER, new SpanLabel(message));
        FontImage img = FontImage.createMaterial(icon, new Label().getStyle(), 15f);
        dlg.add(BorderLayout.WEST, new Label(img));
        final Button okBtn = new Button(ok);
        final Button cancelBtn = new Button(cancel);
        dlg.add(BorderLayout.SOUTH, FlowLayout.encloseRight(cancelBtn, okBtn));
        dlg.setAnimateShow(true);
        final int[] okBounds = new int[4];
        final int[] cancelBounds = new int[4];
        
        // Containually update the bounds of the ok and cancel buttons so that we know 
        // where they are for the native listener
        UITimer t = UITimer.timer(200, true, new Runnable() {
            @Override
            public void run() {
                copyBounds(okBtn, okBounds);
                copyBounds(cancelBtn, cancelBounds);
            }
            
        });
        
        dlg.showPopupDialog(CN.getCurrentForm());
        
        final boolean complete[] = new boolean[1];
        final boolean result[] = new boolean[1];
        EventListener clickListener = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                MouseEvent mevt = (MouseEvent)evt;
                int px = getClientX(mevt);
                int py = getClientY(mevt);

                if (contains(okBounds, px, py)) {
                    // This click was on the ok buttn
                    complete[0] = true;
                    result[0] = true;
                    if (onOk != null) {
                        onOk.run();
                    }
                    new Thread(new Runnable() {
                        public void run() {
                            synchronized(complete) {
                                complete.notify();
                            }
                        }
                    }).start();
                    
                }
                
                if (contains(cancelBounds, px, py)) {
                    // This click was on the ok buttn
                    complete[0] = true;
                    result[0] = false;
                    new Thread(new Runnable() {
                        public void run() {
                            synchronized(complete) {
                                complete.notify();
                            }
                        }
                    }).start();
                    
                }
            }
            
        };
        registerNativeClickHandler(clickListener);
        
        
        CN.invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (!complete[0]) {
                    synchronized(complete) {
                        try {
                            complete.wait();
                        } catch (Throwable t){}
                    }
                }
            }
            
        });
        dlg.disposeToTheBottom();
        t.cancel();
        unregisterNativeClickHandler(clickListener);
        return result[0];
    }
    
    private static void copyBounds(Component cmp, int[] destBounds) {
        destBounds[0] = cmp.getAbsoluteX();
        destBounds[1] = cmp.getAbsoluteY();
        destBounds[2] = cmp.getWidth();
        destBounds[3] = cmp.getHeight();
    }
    
    private static boolean contains(int[] bounds, int x, int y) {
        return bounds[0] <= x && bounds[2] + bounds[0] >= x && bounds[1] <= y && bounds[1] + bounds[3] >= y;
    }
    
    private void registerNativeClickHandler(final EventListener l) {
        nativeEventListener = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                if (evt.isCancelable()) {
                    evt.preventDefault();
                }
            }
            
        };
        Window.current().getDocument().getBody().addEventListener("pointerup", l);
        
    }
    
    private void unregisterNativeClickHandler(EventListener l) {
        nativeEventListener = null;
        //Window.current().removeEventListener("click", l);
        Window.current().getDocument().getBody().removeEventListener("pointerup", l);
    }

    private String selectedText;
    private ActionListener textSelectionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent t) {
            selectedText = ((TextSelection)t.getSource()).getSelectionAsText();
            outputCanvas.focus();
        }
            
    };

    @JSBody(params={"textArea", "valueForClipboard"}, script="var range = document.createRange();\n" +
"            range.selectNodeContents(textArea);\n" +
"\n" +
"            var selection = window.getSelection();\n" +
"            selection.removeAllRanges(); // remove previously selected ranges\n" +
"            selection.addRange(range);\n" +
"            textArea.setSelectionRange(0, valueForClipboard.length); ")
    private static native void selectTextAreaIOS(HTMLTextAreaElement textArea, String valueForClipboard);
    
    @Override
    public void copySelectionToClipboard(TextSelection sel) {
        if (selectedText == null || selectedText.isEmpty()) {
            return;
        }
        copyToClipboard(selectedText);
        
    }

    @JSBody(params={"name"}, script="return document.execCommand(name)")
    private native static boolean execCommand(String name);
    
    
    private class ClipboardCopyRequest{
        Object content;
        boolean triedBacksideHook, triedInSheet;
        ClipboardCopyRequest(Object content) {
            this.content = content;
        }
        
    }
    @JSBody(params={"command"}, script="if (!document.queryCommandEnabled) return true; return document.queryCommandEnabled(command);")
    private native static boolean queryCommandEnabled(String command);
    
    @Override
    public void copyToClipboard(Object obj) {
        final ClipboardCopyRequest request = (obj instanceof ClipboardCopyRequest) ? (ClipboardCopyRequest)obj : new ClipboardCopyRequest(obj);
        obj = request.content;
        super.copyToClipboard(obj);
        if (!(obj instanceof String)) {
            return;
        }
        String selectedText = (String)obj;
        HTMLDocument doc = Window.current().getDocument();
        HTMLTextAreaElement textArea = (HTMLTextAreaElement)doc.createElement("textarea");
        textArea.setAttribute("readonly", "");
        doc.getBody().appendChild(textArea);
        textArea.setValue(selectedText);
        if (isIOS()) {
            selectTextAreaIOS(textArea, selectedText);
        } else {
            textArea.select();
        }
        boolean res = queryCommandEnabled("copy");
        if (res) {
            execCommand("copy");
        }
        doc.getBody().removeChild(textArea);
        if (!res && !request.triedBacksideHook) {
            // The copy failed
            // let's try to add a backside hook
            if (isBacksideHookAvailable()) {
                addBacksideHook(new JSRunnable() {
                    @Override
                    public void run() {
                        request.triedBacksideHook = true;
                        copyToClipboard(request);
                    }
                });
            } else {
                request.triedBacksideHook = true;
                copyToClipboard(request);
            }
        } else if (!res && !request.triedInSheet) {
            // No backside hooks available
            callSerially(new Runnable() {
                @Override
                public void run() {
                    final Sheet sheet = new Sheet(Sheet.getCurrentSheet(), "Copy to Clipboard");
                    SpanLabel message = new SpanLabel(CN.getProperty("AppName", "This application")+" has requested to copy content to the system clipboard.");
                    sheet.getContentPane().setLayout(new BorderLayout());
                    Button copy = new Button("Allow");
                    Button cancel = new Button("Cancel");
                    sheet.getContentPane().add(BorderLayout.CENTER, message);
                    sheet.getContentPane().add(BorderLayout.SOUTH, GridLayout.encloseIn(2, cancel, copy));
                    copy.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent t) {
                            request.triedInSheet = true;
                            copyToClipboard(request);
                            sheet.back();
                        }
                    });
                    
                    cancel.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            sheet.back();
                        }
                    });
                    sheet.setPosition(BorderLayout.NORTH, BorderLayout.CENTER);
                    sheet.show();
                    
                }
                
            });
        } else if (!res) {
            ToastBar.showErrorMessage("Failed to copy to clipboard due to browser permission restrictions.", 5000);
        }
        
    }
    
    
    
    public String getSelectedText() {
        return selectedText;
    }
    
    @JSBody(params={"evt", "content"}, script="try { evt.clipboardData.setData('text/plain', content);} catch (e){}")
    private native static void setClipboardData(Event evt, String content);
    
    EventListener copyListener = new EventListener() {
        @SuppressSyncErrors
        public void handleEvent(Event evt) {
            if (selectedText == null || selectedText.isEmpty()) {
                return;
            }
            if (!jQuery_is_(outputCanvas, ":focus")) {
                return;
            }
            setClipboardData(evt, selectedText);
            evt.preventDefault();
        }
    };
    
    EventListener selectAll = new EventListener() {
        @SuppressSyncErrors
        public void handleEvent(Event evt) {
            if (jQuery_is_(outputCanvas, ":focus")) {
                callSerially(new Runnable() {
                    public void run() {
                        Form f = CN.getCurrentForm();
                        TextSelection sel = f.getTextSelection();
                        if (sel.isEnabled()) {
                            sel.selectAll();
                        }
                    }
                });
            }
        }
    };
    private boolean contextListenerActive;
    EventListener<MouseEvent> contextListener = new EventListener<MouseEvent>() {
        @SuppressSyncErrors
        public void handleEvent(final MouseEvent evt) {
            _log("In context listener");
            if (!jQuery_is_(outputCanvas, ":focus")) {
                return;
            }
            _log("focused");
            Form f = CN.getCurrentForm();
            if (f == null) {
                return;
            }
            _log("Form is there");
            TextSelection sel = f.getTextSelection();
            if (sel == null || !sel.isEnabled()) {
                return;
            }
            _log("Text selection is on");
            evt.preventDefault();
            callSerially(new Runnable() {
                public void run() {
                    _log("Showing context menu");
                    MouseEvent me = lastMouseEvent != null ? lastMouseEvent : evt;
                    ContextMenu.showAt(unscaleCoord(me.getClientX()) + CN.convertToPixels(2), unscaleCoord(me.getClientY()) + CN.convertToPixels(2));
                    
                }
            });
        }
    };
    
    @Override
    public void initializeTextSelection(TextSelection sel) {
        sel.addTextSelectionListener(textSelectionListener);
        HTMLDocument doc = Window.current().getDocument();
        doc.addEventListener("copy", copyListener);
        contextListenerActive = true;
        doc.addEventListener("contextmenu", contextListener);
        
        
        
    }

    @Override
    public void deinitializeTextSelection(TextSelection sel) {
        contextListenerActive = false;
        HTMLDocument doc = Window.current().getDocument();
        doc.removeEventListener("copy", copyListener);
        doc.removeEventListener("contextmenu", contextListener);
        sel.removeTextSelectionListener(textSelectionListener);
    }

    private class HeavyButton {
        private HTMLInputElement el;
        private Button btn;
        
        HeavyButton(Button btn, HTMLInputElement el) {
            this.el = el;
            this.btn = btn;
        }
    }
    
    @Override
    public Object createHeavyButton(Button btn) {
        HTMLInputElement nativeButton = createButton("heavy-btn", "");
        //nativeButton.appendChild(setStyleSize(((NativeImage)btn.toImage().getImage()).getMutableGraphics().getCanvas(), btn));
        return new HeavyButton(btn, nativeButton);
           
    }
    
    
    Map<ActionListener,EventListener> heavyListeners = new HashMap<ActionListener,EventListener>();
    public void addHeavyActionListener(Object peer, final ActionListener l) {
        HeavyButton hbtn = (HeavyButton)peer;
        final HTMLInputElement el = hbtn.el;
        EventListener eli = new EventListener() {
            @SuppressSyncErrors
            @Override
            public void handleEvent(Event evt) {
                l.actionPerformed(new ActionEvent(el));
            }
            
        };
        heavyListeners.put(l, eli);
        el.addEventListener("click", eli);
    }

    public void removeHeavyActionListener(Object peer, ActionListener l) {
        HeavyButton hbtn = (HeavyButton)peer;
        HTMLInputElement el = hbtn.el;
        EventListener eli = heavyListeners.get(l);
        if (eli != null) {
            el.removeEventListener("click", eli);
        }
    }

    public void updateHeavyButtonBounds(Object peer, int x, int y, int width, int height) {
        HeavyButton hbtn = (HeavyButton)peer;
        HTMLInputElement el = hbtn.el;
        el.getStyle().setProperty("top", scaleCoord(y)+"px");
        el.getStyle().setProperty("left", scaleCoord(x)+"px");
        el.getStyle().setProperty("width", scaleCoord(width)+"px");
        el.getStyle().setProperty("height", scaleCoord(height)+"px");
    }

    @Override
    public void initHeavyButton(Object peer) {
        HeavyButton hbtn = (HeavyButton)peer;
        HTMLInputElement el = hbtn.el;
        while (el.getFirstChild() != null) {
            el.removeChild(el.getFirstChild());
        }
        if (hbtn.btn.getWidth() == 0 || hbtn.btn.getHeight() == 0) {
            hbtn.btn.setWidth(hbtn.btn.getPreferredW());
            hbtn.btn.setHeight(hbtn.btn.getPreferredH());
        }
        if (hbtn.btn.getWidth() == 0 || hbtn.btn.getHeight() == 0) {
            
        } else {
            el.appendChild(setStyleSize(((NativeImage)hbtn.btn.toImage().getImage()).getMutableGraphics().getCanvas(), hbtn.btn));
        }
        Window.current().getDocument().getBody().appendChild(el);
    }

    @Override
    public void deinitializeHeavyButton(Object peer) {
        HeavyButton hbtn = (HeavyButton)peer;
        Window.current().getDocument().getBody().removeChild(hbtn.el);
        super.deinitializeHeavyButton(peer);
    }

    @Override
    public boolean requiresHeavyButtonForCopyToClipboard() {
        return true;
    }
    
    
    
    
    
    private static HTMLCanvasElement setStyleSize(HTMLCanvasElement cv, Component cmp) {
        
            cv.getStyle().setProperty("width", scaleCoord(cmp.getWidth())+"px");
            cv.getStyle().setProperty("height", scaleCoord(cmp.getHeight())+"px");
            return cv;
    }

    @JSBody(params={}, script="return (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)")
    private static native boolean isDarkMode_();
    
    @Override
    public Boolean isDarkMode() {
        return isDarkMode_();
    }
    
    

    
    
    
}
