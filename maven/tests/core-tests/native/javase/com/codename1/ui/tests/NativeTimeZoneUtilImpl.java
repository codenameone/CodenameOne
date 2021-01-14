package com.codename1.ui.tests;

import java.util.TimeZone;

public class NativeTimeZoneUtilImpl implements com.codename1.ui.tests.NativeTimeZoneUtil{
    public void setDefaultTimeZone(String param) {
        TimeZone.setDefault(TimeZone.getTimeZone(param));
    }

    public boolean isSupported() {
        return true;
    }

}
