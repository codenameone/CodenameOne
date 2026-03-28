package com.codename1.impl.platform.js;

public final class VMHost {
    private VMHost() {
    }

    public static native int echoInt(int value);

    public static native int getLastEventCode();

    public static native int pollEventCode();
}
