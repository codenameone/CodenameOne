/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
import com.codename1.ui.*;
import com.codename1.payment.*;
import com.codename1.social.*;

/**
 * The subsystems the runtime mocks: a purchase and a social login.
 *
 * Written against the ordinary APIs, exactly as an application would, because
 * the point of the mocks is that application code needs no knowledge of them.
 */
public class MockProbe {
    public static void main(String[] a) {
        String purchaseResult;
        try {
            Purchase p = Purchase.getInAppPurchase();
            if (p == null) {
                purchaseResult = "getInAppPurchase returned null";
            } else {
                Product[] products = p.getProducts(new String[]{"com.example.pro"});
                p.purchase("com.example.pro");
                purchaseResult = "managed=" + p.isManagedPaymentSupported()
                        + " listing=" + p.isItemListingSupported()
                        + " price=" + (products.length > 0 ? products[0].getLocalizedPrice() : "none")
                        + " owned=" + p.wasPurchased("com.example.pro");
                p.refund("com.example.pro");
                purchaseResult = purchaseResult + " afterRefund=" + p.wasPurchased("com.example.pro");
            }
        } catch (Throwable t) {
            purchaseResult = "threw " + t.getClass().getName() + ": " + t.getMessage();
        }
        System.out.println("PROBE MockProbe purchase: " + purchaseResult);

        String loginResult;
        try {
            FacebookConnect fb = FacebookConnect.getInstance();
            loginResult = "instance=" + fb.getClass().getName()
                    + " native=" + fb.isNativeLoginSupported();
            fb.doLogin();
        } catch (Throwable t) {
            loginResult = "threw " + t.getClass().getName() + ": " + t.getMessage();
        }
        System.out.println("PROBE MockProbe login: " + loginResult);

        new Form("Mocks").show();
    }
}
