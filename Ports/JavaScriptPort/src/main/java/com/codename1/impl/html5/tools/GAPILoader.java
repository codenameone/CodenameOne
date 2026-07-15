/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
