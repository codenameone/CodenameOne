/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.jso.util;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;

/**
 *
 * @author shannah
 */
public class JSNumberFormat {
    
    private IntlNumberFormat fmt;
    private String style;
    private String currency;
    private final String locale;
    
    public JSNumberFormat(String locale) {
        this.locale = locale;
        
    }
    
    
    @JSBody(params={}, script="return !(window.Intl===undefined)")
    private static native boolean _intlSupported();
    
    private IntlNumberFormat fmt() {
        if (fmt == null) {
            JSObject opts = createOpts();
            
            if (getStyle() != null) {
                setOpt(opts, "style", getStyle());
            }
            
            if (getCurrency() != null) {
                setOpt(opts, "currency", getCurrency());
            }
            
            fmt = (IntlNumberFormat)createIntlNumberFormat(locale, opts);
        }
        return fmt;
    }
    
    @JSBody(params={"locales","opts"}, script="return new Intl.NumberFormat(locales, opts)")
    private static native JSObject createIntlNumberFormat(String locales, JSObject opts);
    
    @JSBody(params={"o","key","value"}, script="o[key] = value")
    private static native void setOpt(JSObject o, String key, String value);
    
    @JSBody(params={}, script="return {}")
    private static native JSObject createOpts();

    /**
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * @return the currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * @param currency the currency to set
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    private static interface IntlNumberFormat extends JSObject {
        public String format(int n);
        
        public String format(double n);
        
        @JSProperty
        public JSObject getResolvedOptions();
        
    }
    
    public String format(int n) {
        if (_intlSupported()) {
            return fmt().format(n);
        } else {
            return String.valueOf(n);
        }
    }
    
    public String format(double n) {
        if (_intlSupported()) {
            return fmt().format(n);
        } else {
            return String.valueOf(n);
        }
    }
    
    
}
