/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.jso.util;


import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;

/**
 *
 * @author shannah
 */
public class JSDateFormat  {

    private IntlDateTimeFormat fmt;
    
    private final String locales;
    
    public static final String DATE_LONG="longdate";
    public static final String DATE_MEDIUM="mediumdate";
    public static final String DATE_SHORT="shortdate";
    public static final String DATETIME_LONG="longdatetime";
    public static final String DATETIME_MEDIUM="mediumdatetime";
    public static final String DATETIME_SHORT="shortdatetime";
    public static final String TIME_SHORT="shorttime";
            
    
    private String style = DATETIME_LONG;
    
    private String dateFormat;
    
    public JSDateFormat(String locales) {
        this.locales = locales;
        
    }
    
    @JSBody(params={}, script="return !(window.Intl===undefined)")
    private static native boolean _intlSupported();
    
    @JSBody(params={"ms"}, script="return new Date(ms);")
    private static native JSObject toDate(double ms);
    
    private static JSObject toDate(Date dt) {
        return toDate((double)dt.getTime());
    }
    
    private IntlDateTimeFormat fmt() {
        if (fmt == null) {
            JSObject opts = createOpts();
            
            if (DATE_LONG.equals(style)) {
                
            }
            
            fmt = (IntlDateTimeFormat)createIntlDateTimeFormat(locales, opts);
        }
        return fmt;
    }
    
    private boolean isTimeOnly() {
        return TIME_SHORT.equals(style);
    }
    
    private boolean isDateOnly() {
        return DATE_SHORT.equals(style) || DATE_LONG.equals(style) || DATE_MEDIUM.equals(style);
    }
    
    @JSBody(params={"locales","opts"}, script="return new Intl.DateTimeFormat(locales, opts)")
    private static native JSObject createIntlDateTimeFormat(String locales, JSObject opts);
    
    @JSBody(params={"o","key","value"}, script="o[key] = value")
    private static native void setOpt(JSObject o, String key, String value);
    
    @JSBody(params={}, script="return {}")
    private static native JSObject createOpts();
    
    @JSBody(params={"d"}, script="return d.toLocaleString()")
    private static native String _toLocaleString(JSObject d);
    
    @JSBody(params={"d"}, script="return d.toLocaleTimeString()")
    private static native String _toLocaleTimeString(JSObject d);
    
    @JSBody(params={"d"}, script="return d.toLocaleDateString()")
    private static native String _toLocaleDateString(JSObject d);
    
    
    public String format(Date d) {
        if (isTimeOnly()) {
            return _toLocaleTimeString(toDate(d));
        } else if (isDateOnly()) {
            return _toLocaleDateString(toDate(d));
        } else {
            return _toLocaleString(toDate(d));
        }
            
        
    }
    
   

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

   
    public static interface IntlDateTimeFormat extends JSObject {
        public String format(JSObject d);
        
        @JSProperty
        public JSObject getResolvedOptions();
               
    }
    
    public static interface IntlDateTimeFormatOptions extends JSObject {
        public static final String WEEKDAY_NARROW = "narrow";
        public static final String WEEKDAY_SHORT = "short";
        public static final String WEEKDAY_LONG = "long";
        
        public static final String ERA_NARROW = "narrow";
        public static final String ERA_SHORT = "short";
        public static final String ERA_LONG = "long";
        
        public static final String YEAR_NUMERIC = "numeric";
        public static final String YEAR_2_DIGIT = "2-digit";
        
        public static final String MONTH_NARROW = "narrow";
        public static final String MONTH_SHORT = "short";
        public static final String MONTH_NUMERIC = "numeric";
        public static final String MONTH_2_DIGIT = "2-digit";
        
        public static final String DAY_NUMERIC = "numeric";
        public static final String DAY_2_DIGIT = "2-digit";
        
        public static final String HOUR_NUMERIC = "numeric";
        public static final String HOUR_2_DIGIT = "2-digit";
        
        public static final String MINUTE_NUMERIC = "numeric";
        public static final String MINUTE_2_DIGIT = "2-digit";
        
        public static final String SECOND_NUMERIC = "numeric";
        public static final String SECOND_2_DIGIT = "2-digit";
        
        public static final String TIMEZONE_SHORT = "short";
        public static final String TIMEZONE_LONG = "long";
        
        @JSProperty
        public String getLocaleMatcher();
        
        @JSProperty
        public void setLocaleMatcher(String s);
        
        @JSProperty
        public String getTimeZone();
        
        @JSProperty
        public void setTimeZone(String tz);
        
        @JSProperty
        public boolean isHour12();
        
        @JSProperty
        public void setHour12(boolean hour12);
        
        @JSProperty
        public String getFormatMatcher();
        
        @JSProperty
        public void setFormatMatcher(String matcher);
        
        @JSProperty
        public String getWeekday();
        
        @JSProperty
        public void setWeekday(String format);
        
        @JSProperty
        public String getEra();
        
        @JSProperty
        public void setEra(String format);
        
        @JSProperty
        public String getYear();
        
        @JSProperty
        public void setYear(String format);
        
        @JSProperty
        public String getMonth();
        
        @JSProperty
        public void setMonth(String format);
        
        @JSProperty
        public String getDay();
        
        @JSProperty
        public void setDay(String format);
        
        @JSProperty
        public String getHour();
        
        @JSProperty
        public String setHour(String format);
        
        @JSProperty
        public String getMinute();
        
        @JSProperty
        public void setMinute(String format);
        
        @JSProperty
        public String getTimeZoneName();
        
        @JSProperty
        public String setTimeZoneName(String format);
 
    }
    
    
}
