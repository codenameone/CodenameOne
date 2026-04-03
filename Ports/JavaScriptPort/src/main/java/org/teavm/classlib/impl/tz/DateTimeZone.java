/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.classlib.impl.tz;

public class DateTimeZone {
    private final String id;
    
    public DateTimeZone(String id) {
        this.id = id;
    }
    
    public String getID() {
        return id;
    }
    
    public long getOffset(long instant) {
        return 0;
    }
    
    public static DateTimeZone forID(String id) {
        return new DateTimeZone(id);
    }
}