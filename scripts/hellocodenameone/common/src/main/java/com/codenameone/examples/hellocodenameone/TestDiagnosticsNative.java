package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;

public interface TestDiagnosticsNative extends NativeInterface {
    void dumpNativeThreads(String reason);
    void failFastWithNativeThreadDump(String reason);
}
