package com.codenameone.examples.purchasetest;

import com.codename1.payment.Purchase;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Minimal Codename One app dedicated to the In-App-Purchase e2e tests.
 *
 * It references com.codename1.payment.* so the platform builders compile the
 * IAP native bridge (iOS: defines CN1_USE_STOREKIT + links StoreKit;
 * Android: pulls in Play Billing), and installs a {@link RecordingReceiptStore}
 * so the iOS StoreKitTest and Android billing-bridge tests can assert that a
 * purchase reached the store. Kept separate from the hellocodenameone sample so
 * IAP wiring never ripples into the screenshot/notification CI workflows.
 */
public class PurchaseTestApp extends Lifecycle {
    @Override
    public void init(Object context) {
        super.init(context);
        try {
            Purchase.getInAppPurchase().setReceiptStore(new RecordingReceiptStore());
            // Drain anything enqueued before the store was installed (the
            // Android fake fires from the activity's onCreate, which can race
            // ahead of this init).
            Purchase.getInAppPurchase().synchronizeReceipts();
            System.out.println("CN1SS:IAP_DIAG installed=true");
        } catch (Throwable t) {
            System.out.println("CN1SS:IAP_DIAG:EXCEPTION " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    @Override
    public void runApp() {
        Form hi = new Form("Purchase Test", BoxLayout.y());
        hi.add(new Label("IAP e2e test app"));
        hi.show();
    }
}
