/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.classlib.impl.tz;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public final class DateTimeZoneProvider {
    private DateTimeZoneProvider() {}
    
    @JSBody(params = {}, script = "return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'")
    public static native String detect();
    
    public static DateTimeZone getDefault() {
        return new DateTimeZone(detect());
    }
    
    @JSBody(params = {}, script = "return true")
    public static native boolean timeZoneDetectionEnabled();
}