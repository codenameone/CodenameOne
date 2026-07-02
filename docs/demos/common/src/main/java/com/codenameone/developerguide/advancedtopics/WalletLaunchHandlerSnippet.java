package com.codenameone.developerguide.advancedtopics;

import com.codename1.ui.Display;

/**
 * Compilable launch-handling snippets for Android wallet handoff examples.
 */
public class WalletLaunchHandlerSnippet {
    // tag::walletLaunchStart[]
    public void start() {
        Display d = Display.getInstance();
        String action = d.getProperty("android.intent.action", null);
        if ("com.example.wallet.ACTION_VERIFY".equals(action)) {
            boolean callerVerified = "true".equals(d.getProperty("android.intent.caller.verified", "false"));
            String payload = d.getProperty("android.intent.extra.wallet_payload", d.getProperty("AppArg", null));
            String caller = d.getProperty("android.intent.caller", "");

            if (!callerVerified || !isAllowedCaller(caller)) {
                failClosed(); // return declined/canceled via bridge completion API
                return;
            }

            verifyWalletPayload(payload, caller);
            return;
        }
        showMainForm();
    }
    // end::walletLaunchStart[]

    private boolean isAllowedCaller(String caller) {
        return caller != null && caller.length() > 0;
    }

    private void failClosed() {
        // Complete the native bridge with a declined/canceled result.
    }

    private void verifyWalletPayload(String payload, String caller) {
        // Run authentication + back end verification, then call native bridge completion API.
    }

    private void showMainForm() {
        // Show the normal application UI.
    }
}
