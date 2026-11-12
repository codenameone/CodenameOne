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

import com.codename1.payment.CommerceManager;
import com.codename1.payment.center.CustomerCenter;
import com.codename1.payment.paywall.PaywallView;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Runs once a purchase has synchronized: refreshes commerce, checks that a
 * build without cloud commerce answers the entitlement from the store-direct
 * signal, and builds the bundled paywall and the customer center so their code
 * is translated and linked on every target. Nothing here needs a network: a
 * local build carries no build key, so cloud commerce is off.
 *
 * The outcome goes to the {@link PurchaseTestSink} (read by the iOS XCTest) and
 * to a CN1SS:IAP:COMMERCE log line.
 */
final class CommerceCheck {
    private static boolean started;

    private CommerceCheck() {
    }

    /** Called on the EDT, from the receipt synchronization callback. */
    static void run(final PurchaseTestSink sink, final String sku) {
        if (started) {
            return;
        }
        started = true;
        // refresh() is a blocking call by contract, so it runs off the EDT.
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                final String state = checkManager(sku);
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        report(sink, state.length() > 0 ? state : checkUi());
                    }
                });
            }
        });
    }

    /** Empty when every check passed, else what failed. */
    private static String checkManager(String sku) {
        try {
            CommerceManager cm = CommerceManager.getInstance();
            cm.refresh();
            if (cm.isCloudEnabled()) {
                return "cloud commerce is enabled in a build with no build key";
            }
            if (cm.getCustomerInfo() == null) {
                return "getCustomerInfo() answered null";
            }
            if (!cm.isEntitled(sku)) {
                return "isEntitled(" + sku + ") is false after the purchase";
            }
            return "";
        } catch (Throwable t) {
            return "CommerceManager threw " + t.getClass().getName() + ": " + t.getMessage();
        }
    }

    private static String checkUi() {
        try {
            CommerceManager cm = CommerceManager.getInstance();
            Form f = new Form("Paywall", new BorderLayout());
            f.add(BorderLayout.CENTER, new PaywallView(cm.getOfferings().getCurrent()));
            f.show();
            CustomerCenter.show();
            return "";
        } catch (Throwable t) {
            return "paywall or customer center threw " + t.getClass().getName() + ": " + t.getMessage();
        }
    }

    private static void report(PurchaseTestSink sink, String failure) {
        String result = failure.length() == 0 ? "OK" : "FAIL " + failure;
        System.out.println("CN1SS:IAP:COMMERCE " + result);
        if (sink != null) {
            sink.recordCommerceResult(result);
        }
    }
}
