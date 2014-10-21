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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.codename1.payment.Product;
import com.codename1.payment.Purchase;
import com.codename1.payment.PurchaseCallback;
import com.codename1.ui.Display;
// ZOOZMARKER_START
import com.zooz.android.lib.CheckoutActivity;
// ZOOZMARKER_END

/**
 *
 * @author Chen
 */
public class ZoozPurchase extends Purchase implements IntentResultListener, Runnable {

    private Activity activity;
    
    private String purchaseId = null;
    
    private boolean completed = false;
    private boolean hasMarket;
    private String currency;
    private double amount;
    private String failMessage;
    
    public ZoozPurchase() {
        activity = AndroidImplementation.activity;
        hasMarket= AndroidImplementation.hasAndroidMarket(activity);
    }

    @Override
    public boolean isManagedPaymentSupported() {
        return hasMarket;
    }

    @Override
    public boolean wasPurchased(String sku) {
        return ((CodenameOneActivity)activity).wasPurchased(sku);
    }

    @Override
    public void purchase(String sku) {
        ((CodenameOneActivity)activity).purchase(sku);
    }

    @Override
    public void subscribe(String sku) {
        ((CodenameOneActivity)activity).subscribe(sku);
    }

    @Override
    public boolean isSubscriptionSupported() {
        //return ((CodenameOneActivity)activity).isSubscriptionSupported();
        return true;
    }

    @Override
    public boolean isUnsubscribeSupported() {
        return false;
    }
                
    @Override
    public boolean isManualPaymentSupported() {
        return true;
    }
    
    public boolean isItemListingSupported() {
        return true;
    }
    
    public Product[] getProducts(String[] skus) {
        return ((CodenameOneActivity)activity).getProducts(skus);
    }
    
    
    // ZOOZMARKER_START    
    @Override
    public String pay(double amount, String currency) {
        Intent intent = new Intent(activity, CheckoutActivity.class);

        String zoozAppKey = Display.getInstance().getProperty("ZoozAppKey", "");
        boolean isSandBox = Display.getInstance().getProperty("ZoozSandBox", "true").equals("true");
        // send merchant credential, app_key as given in the registration
        intent.putExtra(CheckoutActivity.ZOOZ_APP_KEY, zoozAppKey);
        intent.putExtra(CheckoutActivity.ZOOZ_AMOUNT, amount);
        intent.putExtra(CheckoutActivity.ZOOZ_CURRENCY_CODE, currency);
        intent.putExtra(CheckoutActivity.ZOOZ_IS_SANDBOX, isSandBox);
        String zoozInvoice = Display.getInstance().getProperty("ZoozInvoice", null);
        if(zoozInvoice != null) {
            intent.putExtra(CheckoutActivity.ZOOZ_INVOICE, zoozInvoice);
        }
        // start ZooZCheckoutActivity and wait to the activity result.
        activity.startActivityForResult(intent, ZOOZ_PAYMENT);
        
        Display.getInstance().invokeAndBlock(this);
        
        // use call serially so the purchase callback happens on the 
        // next EDT loop AFTER the value was returned 
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                CodenameOneActivity cn = (CodenameOneActivity)activity;
                PurchaseCallback pc = cn.getPurchaseCallback();
                if(pc != null) {
                    if(failMessage != null) {
                        pc.paymentFailed(purchaseId, failMessage);
                    } else {
                        pc.paymentSucceeded(purchaseId, ZoozPurchase.this.amount, ZoozPurchase.this.currency);
                    }
                }
            }
        });
        return purchaseId;
    }
    // ZOOZMARKER_END

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // ZOOZMARKER_START    
        if(resultCode == Activity.RESULT_OK){
            failMessage = null;
            purchaseId = data.getStringExtra(CheckoutActivity.ZOOZ_TRANSACTION_ID);
            amount = data.getDoubleExtra(CheckoutActivity.ZOOZ_AMOUNT, -1);
            currency = data.getStringExtra(CheckoutActivity.ZOOZ_CURRENCY_CODE);
        }else{
            failMessage = "";
            if (data != null){
                //failed to purchase - the purchaseId will be null
                Log.d("Codename One", data.getStringExtra(CheckoutActivity.ZOOZ_ERROR_MSG));
                purchaseId = data.getStringExtra(CheckoutActivity.ZOOZ_TRANSACTION_ID);
                failMessage = data.getStringExtra(CheckoutActivity.ZOOZ_ERROR_MSG);
                if(failMessage == null) {
                    failMessage = "";
                }
            }
        }
        completed = true;
        synchronized(this) {
            notify();
        }
    // ZOOZMARKER_END
    }
    
    @Override
    public synchronized void run() {
            while(!completed) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                }
            }
    }
    
    
    
}
