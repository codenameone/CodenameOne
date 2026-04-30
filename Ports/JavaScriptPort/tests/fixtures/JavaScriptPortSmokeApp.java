/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
import com.codename1.impl.platform.js.JavaScriptPortHost;

public class JavaScriptPortSmokeApp {
    public static void main(String[] args) {
        int result = 0;
        result += JavaScriptPortHost.bootstrap(1);
        result += JavaScriptPortHost.resourceThemeChecksum(7);
        result += JavaScriptPortHost.networkFetchStatus(11);
        result += JavaScriptPortHost.storageWriteRead(3, 109);
        result += JavaScriptPortHost.databaseWriteRead(5, 113);
        result += JavaScriptPortHost.browserNavigateAndEval(13);
        result += JavaScriptPortHost.mediaPlayAndQuery(17);
        result += JavaScriptPortHost.dispatchPointer(19, 23);
        System.exit(result);
    }
}
