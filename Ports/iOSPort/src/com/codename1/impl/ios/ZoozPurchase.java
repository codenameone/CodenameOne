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
package com.codename1.impl.ios;

import com.codename1.payment.Product;
import com.codename1.payment.Purchase;
import com.codename1.payment.PurchaseCallback;
import com.codename1.ui.Display;

/**
 * Implementation of the purchase API 
 *
 * @author Shai Almog
 */
class ZoozPurchase extends Purchase implements Runnable {

    private String purchaseId = null;
    private static final Object LOCK = new Object();
    private static boolean completed = false;
    private IOSNative nativeInstance;
    private IOSImplementation ioImpl;
    private static String transactionId;
    private static double amount;
    private static String errorMessage;
    private PurchaseCallback callback;
    public ZoozPurchase(IOSImplementation ioImpl, IOSNative nativeInstance, PurchaseCallback callback) {
        this.nativeInstance = nativeInstance;
        this.ioImpl = ioImpl;
        this.callback = callback;
    }

    public boolean isManagedPaymentSupported() {
        return nativeInstance.canMakePayments();
    }

    public boolean isItemListingSupported() {
        return true;
    }

    static void paymentSuccessWithResponse(String t, float total) {
        transactionId = t;
        amount = total;
        completed = true;
        synchronized(LOCK) {
            LOCK.notify();
        }
    }

    static void paymentCanceledOrFailed(String error) {
        transactionId = null;
        errorMessage = error;
        completed = true;
        synchronized(LOCK) {
            LOCK.notify();
        }
    }

    public Product[] getProducts(String[] skus) {
        final Product[] p = new Product[skus.length];
        for(int iter = 0 ; iter < p.length ; iter++) {
            p[iter] = new Product();
        }
        nativeInstance.fetchProducts(skus, p);

        // wait for request to complete
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while(p[p.length - 1].getDisplayName() == null) {
                    try {
                        Thread.currentThread().sleep(10);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        return p;
    }

    public boolean wasPurchased(String sku) {
        return ioImpl.getPurchased().contains(sku);
    }

    public void purchase(String sku) {
        nativeInstance.purchase(sku);
    }


    /*public void subscribe(String sku) {
        purchase(sku);
    }*/


    public boolean isSubscriptionSupported() {
        return false;
    }

    public boolean isUnsubscribeSupported() {
        return false;
    }

    @Override
    public boolean isManualPaymentSupported() {
        return true;
    }
    
    @Override
    public String pay(double amount, final String currency, String invoiceNumber) {
        String zoozAppKey = Display.getInstance().getProperty("ZoozAppKey", "");
        boolean isSandBox = Display.getInstance().getProperty("ZoozSandBox", "true").equals("true");
        nativeInstance.zoozPurchase(amount, currency, zoozAppKey, isSandBox, invoiceNumber);
        Display.getInstance().invokeAndBlock(this);
        // use call serially so the purchase callback happens on the 
        // next EDT loop AFTER the value was returned 
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if(callback != null) {
                    if(errorMessage != null) {
                        callback.paymentFailed(purchaseId, errorMessage);
                    } else {
                        // iOS port doesn't return the currency in its callback
                        callback.paymentSucceeded(purchaseId, ZoozPurchase.this.amount, currency);
                    }
                }
            }
        });
        return transactionId;
    }
    
    @Override
    public String pay(double amount, String currency) {
        return pay(amount, currency, "N/A");
    }
    
    @Override
    public void run() {
        synchronized(LOCK) {
            while(!completed) {
                try {
                    LOCK.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }
    
    @Override
    public boolean isRestoreSupported() {
        return true;
    }
    
    @Override
    public void restore() {
        nativeInstance.restorePurchases();
    }
}
