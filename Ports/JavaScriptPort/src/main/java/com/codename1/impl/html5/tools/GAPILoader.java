/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.tools;

import com.codename1.impl.html5.tools.ScriptTool;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;

/**
 *
 * @author shannah
 */
public class GAPILoader {
    private Set<String> loaded = new HashSet<String>();
    
    private class LoadStatus {
        private boolean success;
        private boolean fail;
        private String error;
        
        public boolean isComplete() {
            return success || fail;
        }
            
    }

    private static GAPILoader instance;
    public static GAPILoader getInstance() {
        if (instance == null) {
            instance = new GAPILoader();
        }
        return instance;
    }
    
    
    private GAPILoader() {
        
    }
    
    @JSFunctor
    private static interface NoArgCallback extends JSObject {
        public void callback();
    }
    
    @JSBody(params={"lib", "callback"}, script="gapi.load(lib, callback);")
    private native static void load(String lib, NoArgCallback callback);
    
    public void loadAndWait(String lib) throws IOException {
        
        if (loaded.contains(lib)) {
            return;
        }
        ScriptTool.getInstance().require("https://apis.google.com/js/platform.js");
        final LoadStatus loadStatus = new LoadStatus();
        load(lib, new NoArgCallback() {
            @Override
            public void callback() {
                new Thread(){
                        public void run() {
                            loadStatus.success = true;
                            synchronized(loadStatus){
                                loadStatus.notifyAll();
                            }
                        }
                }.start();
            }
            
        });
        
        while (!loadStatus.isComplete()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    Util.wait(loadStatus);
                }
                
            });
        }
        
        loaded.add(lib);
    }
}
