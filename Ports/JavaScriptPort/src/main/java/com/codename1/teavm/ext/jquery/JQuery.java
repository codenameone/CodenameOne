/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.ext.jquery;

import java.util.Map;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.core.JSBoolean;

/**
 *
 * @author shannah
 */
public class JQuery {
    
    @JSFunctor
    public static interface Callback extends JSObject {
        void callback();
    }
    
    private static interface JQueryImpl extends JSObject {
        JSObject get(int index);
        JQueryImpl css(String key, String val);
        JQueryImpl css(JSObject obj);
        JQueryImpl append(JSObject obj);
        JQueryImpl on(String evt, Callback cb);
        JQueryImpl remove();
    }
    
    @JSBody(params={"sel", "root"}, script="return jQuery(sel, root)")
    private native static JQueryImpl __(String sel, JSObject root);
    
    @JSBody(params={"sel"}, script="return jQuery(sel)")
    private native static JQueryImpl __(String sel);
    
    @JSBody(params={"el"}, script="return jQuery(el)")
    private native static JQueryImpl __(JSObject el);
    
    @JSBody(params={}, script="return {}")
    private native static JSObject createObject();
    
    @JSBody(params={"o", "key", "value"}, script="o[key] = value")
    private native static void setValue(JSObject o, String key, JSObject value);
    @JSBody(params={"o", "key", "value"}, script="o[key] = value")
    private native static void setValue(JSObject o, String key, String value);
    @JSBody(params={"o", "key", "value"}, script="o[key] = value")
    private native static void setValue(JSObject o, String key, double value);
    @JSBody(params={"o", "key", "value"}, script="o[key] = value")
    private native static void setValue(JSObject o, String key, int value);
    
    
    
    public static JSObject createObject(Map<String,Object> values) {
        JSObject o = createObject();
        for (String key : values.keySet()) {
            Object value = values.get(key);
            if (value instanceof String) {
                setValue(o, key, (String)value);
            } else if (value instanceof Integer) {
                setValue(o, key, (int)value);
            } else if (value instanceof Double) {
                setValue(o, key, (double)value);
            } else {
                setValue(o, key, (JSObject)value);
            }
        }
        return o;
    }
    
    public static JSObject createObject(String[] vals) {
        JSObject o = createObject();
        int len = vals.length;
        for (int i=0; i<len; i+=2) {
            setValue(o, vals[i], vals[i+1]);
        }
        return o;
    }
    
    public static void extendObject(JSObject o, String key, boolean val) {
        setValue(o, key, JSBoolean.valueOf(val));
    }
    
    private final JQueryImpl peer;
    
    public JQuery(String sel) {
        peer = __(sel);
    }
    
    public JQuery(String sel, JSObject root) {
        peer = __(sel, root);
    }
    
    public JQuery(JSObject el) {
        peer = __(el);
    }
    
    public JSObject get(int index) {
        return peer.get(index);
    }
    
    public JQuery css(String key, String val) {
        peer.css(key, val);
        return this;
    }
    
    public JQuery css(Map<String, String> vals) {
        for (String key : vals.keySet()) {
            peer.css(key, vals.get(key));
        }
        return this;
    }
    
    public JQuery css(String[] vals) {
        int len = vals.length;
        for (int i=0; i<len; i+=2) {
            css(vals[i], vals[i+1]);
            
        }
        return this;
    }
    
    public JQuery append(JSObject obj) {
        peer.append(obj);
        return this;
    }
    
    public JQuery append(JQuery el) {
        peer.append(el.peer);
        return this;
    }
    
    public static JQuery create(String sel, JSObject root) {
        return new JQuery(sel, root);
    }
    
    public static JQuery create(String sel) {
        return new JQuery(sel);
    }
    
    public static JQuery create(JSObject el) {
        return new JQuery(el);
    }
    
    public static JQuery create(JQuery el) {
        return el;
    }
    
   public JQuery on(String evt, Callback callback) {
       peer.on(evt, callback);
       return this;
   }
   
   public JQuery remove() {
       peer.remove();
       return this;
   }
    
    
    
}
