package com.codename1.ads.admob;

public class ProbeActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        com.codename1.impl.android.AndroidNativeUtil.activity = this;
    }
}
