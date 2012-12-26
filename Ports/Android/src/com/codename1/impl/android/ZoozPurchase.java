/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
