/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.tools;

import java.util.HashSet;
import java.util.Set;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.HTMLLinkElement;

/**
 *
 * @author shannah
 */
public class CSSTool {
    private Set<String> loaded = new HashSet<String>();
    private CSSTool(){
        
    }
    
    private static CSSTool instance;
    public static CSSTool getInstance() {
        if (instance == null) {
            instance = new CSSTool();
        }
        return instance;
    }
    
    
    public void load(String... urls) {
        for(String url : urls) {
            if (loaded.contains(url)) {
                continue;
            }
            HTMLLinkElement el = (HTMLLinkElement)Window.current().getDocument().createElement("link");
            el.setRel("stylesheet");
            el.setType("text/css");
            el.setHref(url);
            Window.current().getDocument().getHead().appendChild(el);
        }
    }
}
