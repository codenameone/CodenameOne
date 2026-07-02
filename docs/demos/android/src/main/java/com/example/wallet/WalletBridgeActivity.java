package com.example.wallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

// tag::walletBridgeActivity[]
public class WalletBridgeActivity extends Activity {
    private static WalletBridgeActivity active;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        active = this;

        Intent in = getIntent();
        String caller = getCallingPackage();

        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            finish();
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra("wallet_payload", in.getStringExtra("payload"));
        launch.putExtra("wallet_caller", caller == null ? "" : caller);
        startActivity(launch);
        // Do not finish yet. CN1 will finish this activity via native callback once verification completes.
    }

    @Override
    protected void onDestroy() {
        if (active == this) {
            active = null;
        }
        super.onDestroy();
    }

    public static WalletBridgeActivity getActive() {
        return active;
    }
}
// end::walletBridgeActivity[]
