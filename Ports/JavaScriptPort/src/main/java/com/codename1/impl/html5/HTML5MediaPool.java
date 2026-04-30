/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import static com.codename1.impl.html5.HTML5Implementation._log;
import com.codename1.impl.html5.JSOImplementations.HTMLAudioElement;
import com.codename1.impl.html5.JSOImplementations.HTMLMediaElement;
import java.util.ArrayList;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.HTMLElement;

/**
 *
 * @author shannah
 */
public class HTML5MediaPool {
    private static int nextIndex=1;
    private final Window window = Window.current();
    private int maxSize=3;
    private final ArrayList<JSOImplementations.HTMLVideoElement> videoPool = new ArrayList<>();
    private final ArrayList<JSOImplementations.HTMLAudioElement> audioPool = new ArrayList<>();
    private final ArrayList<CleanupListener> cleanupListeners = new ArrayList<>();
    public static abstract class CleanupListener {
        private final HTMLElement el;
        
        public CleanupListener(HTMLElement el) {
            this.el = el;
        }
        
        
        
        public abstract void run(HTMLElement el);
    }
    
   public void addCleanupListener(CleanupListener r) {
       cleanupListeners.add(r);
   }
   
   
    
    public JSOImplementations.HTMLVideoElement createVideoElement() {
        if (videoPool.isEmpty()) {
            return (JSOImplementations.HTMLVideoElement)window.getDocument().createElement("video");
        } else {
            return videoPool.remove(0);
        }
    }
    
    @JSBody(params={}, script="if (window._unlockedAudioPool){ "
            + "var el = window._unlockedAudioPool.pop(); "
            + "if (el) return el; "
            + "else return null;"
            + "} else { return null;}")
    private static native JSOImplementations.HTMLAudioElement getAudioElementFromNativePool();
    
    public JSOImplementations.HTMLAudioElement createAudioElement() {
        _log("HTML5MediaPool#createAudioElement");
        if (audioPool.isEmpty()) {
            _log("Pool is empty. Checking native pool");
            JSOImplementations.HTMLAudioElement out = getAudioElementFromNativePool();
           
            if (out != null) {
                if (out.getAttribute("cn1-audio-id") == null) {
                    out.setAttribute("cn1-audio-id", ""+(nextIndex++));
                }
                _log("Returning audio element from native pool with audio ID "+out.getAttribute("cn1-audio-id"));
                return out;
            }
            out =  (JSOImplementations.HTMLAudioElement)window.getDocument().createElement("audio");
            out.setAttribute("cn1-audio-id", ""+(nextIndex++));
            _log("No audio element found in native pool.  Audio Element created with ID: "+out.getAttribute("cn1-audio-id"));
            return out;
        } else {
            
            JSOImplementations.HTMLAudioElement out = audioPool.remove(0);
            _log("Creating audio element from pool with ID "+out.getAttribute("cn1-audio-id"));
            return out;
        }
    }
    
    private void cleanup(HTMLElement el) {
        ArrayList<CleanupListener> tmp = new ArrayList<CleanupListener>();
        for (CleanupListener l : cleanupListeners) {
            if (l.el == el) {
                tmp.add(l);
                l.run(el);
            }
        }
        cleanupListeners.removeAll(tmp);
    }
    
    
    public static boolean isUnlocked(HTMLMediaElement el) {
        return "true".equals(el.getAttribute("data-cn1-unlocked"));
    }
    
    
    public static void markUnlocked(HTMLMediaElement el) {
        el.setAttribute("data-cn1-unlocked", "true");
        
    }
    
    public static void markLocked(HTMLMediaElement el) {
        el.removeAttribute("data-cn1-unlocked");
    }
    
    private void returnAudioElement(JSOImplementations.HTMLAudioElement el) {
        cleanup(el);
        _log("HTML5MediaPool#returnAudioElement "+el.getAttribute("cn1-audio-id"));
        if (audioPool.size() < maxSize && isUnlocked(el)) {
            
            audioPool.add(el);
            _log("Audio element with ID "+el.getAttribute("cn1-audio-id")+" returned to pool. Pool size now "+audioPool.size());
        } else {
            _log("Audio element with ID "+el.getAttribute("cn1-audio-id")+" not returned to pool. Unlocked="+isUnlocked(el)+", pool size="+audioPool.size());
        }
    }
    
    private void returnVideoElement(JSOImplementations.HTMLVideoElement el) {
        cleanup(el);
        if (videoPool.size() < maxSize && isUnlocked(el)) {
            videoPool.add(el);
        }
    }
    
    @JSBody(params={"el"}, script="return el.tagName")
    private native static String getTagName(HTMLElement el);
    
    public void returnMediaElement(JSOImplementations.HTMLMediaElement el) {
        if (getTagName(el).toLowerCase().equals("video")) {
            returnVideoElement((JSOImplementations.HTMLVideoElement)el);
        } else if (getTagName(el).toLowerCase().equals("audio")) {
            returnAudioElement((JSOImplementations.HTMLAudioElement)el);
        } else {
            _log("Failed to return media element to pool because tag name unsupported: "+getTagName(el));
        }
    }
    
    public static String getMediaID(HTMLMediaElement el) {
        if (getTagName(el).toLowerCase().equals("video")) {
            return "";
        } else {
            String out = el.getAttribute("cn1-audio-id");
            if (out == null) out = "";
            return out;
        }
    }
}
