package com.example.wallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.lang.ref.WeakReference;

// tag::walletBridgeActivity[]
public class WalletBridgeActivity extends Activity {
    private static WeakReference<WalletBridgeActivity> active;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        active = new WeakReference<>(this);

        Intent in = getIntent();
        String caller = getCallingPackage();
        String payload = in == null ? null : in.getStringExtra("payload");

        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            finish();
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra("wallet_payload", payload);
        launch.putExtra("wallet_caller", caller == null ? "" : caller);
        startActivity(launch);
        // Do not finish yet. CN1 will finish this activity via native callback once verification completes.
    }

    @Override
    protected void onDestroy() {
        WalletBridgeActivity current = getActive();
        if (current == this) {
            active = null;
        }
        super.onDestroy();
    }

    public static WalletBridgeActivity getActive() {
        return active == null ? null : active.get();
    }
}
// end::walletBridgeActivity[]
