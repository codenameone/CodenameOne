package com.codename1.impl.android;

public final class AndroidImplementation {
    public static void runOnUiThreadAndBlock(Runnable task) {
        // Instrumentation invokes createBanner on Android's UI thread.
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            throw new AssertionError("The probe must run on the UI thread");
        }
        task.run();
    }
}
