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
import com.codename1.payment.Purchase;
import com.codename1.ui.Display;
import com.zooz.android.lib.CheckoutActivity;

/**
 *
 * @author Chen
 */
public class ZoozPurchase extends Purchase implements IntentResultListener, Runnable {

    private Activity activity;
    
    private String purchaseId = null;
    
    private boolean completed = false;
    
    public ZoozPurchase(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean isManualPaymentSupported() {
        return true;
    }
    
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
        // start ZooZCheckoutActivity and wait to the activity result.
        activity.startActivityForResult(intent, ZOOZ_PAYMENT);
        
        Display.getInstance().invokeAndBlock(this);
        return purchaseId;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK){
            purchaseId = data.getStringExtra(CheckoutActivity.ZOOZ_TRANSACTION_ID);
        }else{
            if (data != null){
                //failed to purchase - the purchaseId will be null
                Log.d("Codename One", data.getStringExtra(CheckoutActivity.ZOOZ_ERROR_MSG));
            }
        }
        completed = true;
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
