package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;

public interface TestDiagnosticsNative extends NativeInterface {
    void enableNativeCrashSignalLogging(String reason);
    void dumpNativeThreads(String reason);
    void failFastWithNativeThreadDump(String reason);
}
