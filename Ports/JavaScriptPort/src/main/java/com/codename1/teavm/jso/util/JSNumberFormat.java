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
