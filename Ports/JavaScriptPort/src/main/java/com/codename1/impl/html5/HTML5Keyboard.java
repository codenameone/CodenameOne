/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.impl.VirtualKeyboardInterface;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;

/**
 *
 * @author shannah
 */
public class HTML5Keyboard implements VirtualKeyboardInterface {
    private static boolean virtualKeyboardOpen;
    
    public static void install() {
        Display.getInstance().setDefaultVirtualKeyboard(new HTML5Keyboard());
        if (HTML5Implementation.isAndroid_()) {
            initVirtualKeyboardDetector_(new VirtualKeyboardVisibleHandlerImpl(), new VirtualKeyboardHiddenHandlerImpl());
        }
    }

    @Override
    public void setInputType(int i) {
        Log.p("setInputType() not supported in this keyboard");
    }

    @Override
    public String getVirtualKeyboardName() {
        return "HTML5 Virtual Keybaord";
    }

    @Override
    public void showKeyboard(boolean show) {
        if (show) {
            if (isVirtualKeyboardShowing()) {
                return;
            }
            Form f = CN.getCurrentForm();
            if (f != null) {
                Component focused = f.getFocused();
                if (focused != null && focused.isEditable() && focused.isEnabled() && focused.isVisible() && !focused.isEditing()) {
                    focused.startEditingAsync();
                }
            }
        } else {
            if (!isVirtualKeyboardShowing()) {
                return;
            }
            HTML5Implementation.getInstance().stopTextEditing();
        }
    }

    @Override
    public boolean isVirtualKeyboardShowing() {
        if (HTML5Implementation.isAndroid_()) {
            return virtualKeyboardOpen;
        } else if (HTML5Implementation.isIOS()) {
            return HTML5Implementation.getInstance().isNativeInputFieldFocused();
        } else {
            return false;
        }
    }
    
    
    @JSBody(params={"onVisible", "onHidden"}, script="virtualKeyboardDetector.init( { recentlyFocusedTimeoutDuration: 3000 } ); "
            + "virtualKeyboardDetector.on( 'virtualKeyboardVisible', onVisible ); "
            + "virtualKeyboardDetector.on( 'virtualKeyboardHidden', onHidden );")
    private native static void initVirtualKeyboardDetector_(VirtualKeyboardVisibleHandler onVisible, VirtualKeyboardHiddenHandler onHidden);
    
    @JSFunctor
    private static interface VirtualKeyboardVisibleHandler extends JSObject {
        public void onVisible();
    }
    private static class VirtualKeyboardVisibleHandlerImpl implements VirtualKeyboardVisibleHandler {
        @Override
        public void onVisible() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    
                    virtualKeyboardOpen = true;
                    Display.getInstance().fireVirtualKeyboardEvent(true);
                }
            }).start();
        }
    }
    
    @JSFunctor
    private static interface VirtualKeyboardHiddenHandler extends JSObject {
        public void onHidden();
    }
    private static class VirtualKeyboardHiddenHandlerImpl implements VirtualKeyboardHiddenHandler {
        
        @Override
        public void onHidden() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    virtualKeyboardOpen = false;
                    Display.getInstance().fireVirtualKeyboardEvent(false);
                }
            }).start();
        }
    }
    
    
}
