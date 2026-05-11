package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;

public interface StatusBarTapDiagnosticNative extends NativeInterface {
    /// Fires the same path the iOS scrollViewShouldScrollToTop: delegate runs:
    /// bump the native counter and synthesize a pointer event at
    /// (displayWidth/2, 0). On platforms that don't have the iOS proxy view
    /// the impl falls back to dispatching the pointer event to the current
    /// Form so the screenshot test produces the same visual progression.
    boolean simulateStatusBarTap();

    int getTapCount();
}
