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

import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLScriptElement;

/**
 *
 * @author shannah
 */
public class ScriptTool {
    
    private Set<String> loadedScripts = new HashSet<String>();
    private Set<String> failedLoads = new HashSet<String>();
    
    private void ScriptTool() {
        
    }
    
    private static ScriptTool instance;
    public static ScriptTool getInstance() {
        if (instance == null) {
            instance = new ScriptTool();
        }
        return instance;
    }
    
    /**
     * Loads the scripts at the specified URLs in the order specified, and waits until they are all
     * loaded before proceeding.  This is synchronous. If called from the EDT
     * it will use invokeAndBlock to perform this safely. 
     * @param scripts
     * @throws IOException 
     */
    public void requireOrdered(final String... scripts) throws IOException {
        if (CN.isEdt()) {
            final IOException[] exOut = new IOException[1];
            CN.invokeAndBlock(new Runnable() {
                public void run() {
                    try {
                        requireOrdered(scripts);
                    } catch (IOException ex) {
                        exOut[0] = ex;
                    }
                }
            });
            if (exOut[0] != null) {
                throw exOut[0];
            }
            return;
        }
        final List<IOException> loadErrors = new ArrayList<IOException>();
        for (String script : scripts) {
            if (loadedScripts.contains(script) || failedLoads.contains(script)) {
                continue;
            }
            final Script scriptObj = new Script(script);
            try {
                scriptObj.load();
                loadedScripts.add(scriptObj.src);
            } catch (IOException ex) {
                loadErrors.add(ex);
                failedLoads.add(scriptObj.src);
            }
        }
        
        if (!loadErrors.isEmpty()) {
            throw loadErrors.get(0);
        }
        
    }
    
    
    /**
     * Loads the scripts at the specified URLs, and waits until they are all
     * loaded before proceeding.  This is synchronous. If called from the EDT
     * it will use invokeAndBlock to perform this safely. 
     * @param scripts
     * @throws IOException 
     */
    public void require(final String... scripts) throws IOException {
        if (CN.isEdt()) {
            final IOException[] exOut = new IOException[1];
            CN.invokeAndBlock(new Runnable() {
                public void run() {
                    try {
                        require(scripts);
                    } catch (IOException ex) {
                        exOut[0] = ex;
                    }
                }
            });
            if (exOut[0] != null) {
                throw exOut[0];
            }
            return;
        }
        List<Thread> threads = new ArrayList<Thread>();
        final List<IOException> loadErrors = new ArrayList<IOException>();
        for (String script : scripts) {
            if (loadedScripts.contains(script) || failedLoads.contains(script)) {
                continue;
            }
            final Script scriptObj = new Script(script);
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        scriptObj.load();
                        loadedScripts.add(scriptObj.src);
                    } catch (IOException ex) {
                        loadErrors.add(ex);
                        failedLoads.add(scriptObj.src);
                    }
                }
            });
            threads.add(t);
            t.start();
            
        }
        for (Thread t : threads) {
            synchronized(this) {
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Failed to load scripts "+Arrays.toString(scripts)+" because loading was interrupted", ex);
                }
            }
                
        }
        
        if (!loadErrors.isEmpty()) {
            throw loadErrors.get(0);
        }
        
    }
    
    public static class Script {
        private String src;
        
        public Script(String src) {
            this.src = src;
        }
    
        private void load() throws IOException {
            final boolean[] complete = new boolean[1];
            final boolean[] success = new boolean[1];
            HTMLScriptElement script = (HTMLScriptElement)Window.current().getDocument().createElement("script");
            onError(script, new EventListener() {
                @Override
                public void handleEvent(Event evt) {
                    new Thread(new Runnable() {
                        public void run() {
                            Log.p("Load "+src+" resulted in error");
                            synchronized(complete) {
                                complete[0] = true;
                                complete.notify();
                            }
                        }
                    }).start();
                }

            });
            onLoad(script, new EventListener() {
                @Override
                public void handleEvent(Event evt) {
                    new Thread(new Runnable() {
                        public void run() {
                            Log.p("Load "+src+" successful");
                            synchronized(complete) {
                                complete[0] = true;
                                success[0] = true;
                                complete.notify();
                            }
                        }
                    }).start();
                }

            });
            Window.current().getDocument().getHead().appendChild(script);
            script.setSrc(src);
            while (!complete[0]) {
                synchronized(complete) {
                    Util.wait(complete);
                }
            }


            if (!success[0]) {
                throw new IOException("Failed to load script "+src);
            }

        }
    }
    
    @JSBody(params={"script","l"}, script="script.onload=l;")
    private native static void onLoad(HTMLScriptElement script, EventListener l);
    
    @JSBody(params={"script","l"}, script="script.onerror=l;")
    private native static void onError(HTMLScriptElement script, EventListener l);
    
}
