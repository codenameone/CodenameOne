package com.codename1.ads.admob;

// No Java runtime is needed to measure a native banner. No ad requests are sent.
public final class AdMobCallback {
    public static final int LOADED = 1;
    public static final int FAILED = 2;
    public static final int SHOWN = 3;
    public static final int SHOW_FAILED = 4;
    public static final int DISMISSED = 5;
    public static final int IMPRESSION = 6;
    public static final int CLICKED = 7;
    public static final int REWARD = 8;
    public static final int CONSENT_COMPLETE = 9;
    public static void fire(int handle, int event, int code, String message, String rewardType, int amount) {}
}
