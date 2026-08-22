/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.purchasetest;

import com.codename1.payment.Purchase;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.annotations.buildhints.*;

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
@Android(licenseKey = "CN1TESTPLACEHOLDERKEYNOTFORPRODUCTIONxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxIDAQAB", useAndroidX = true)
@Ios(applicationQueriesSchemes = {"cydia"}, newStorageLocation = true, uiscene = true)
@IosPrivacy(cameraUsageDescription = "Used by the CI smoke test to verify the com.codename1.camera native bridge compiles. The app never opens a camera session.")
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
