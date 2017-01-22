/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.android;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;
import com.codename1.payment.Product;
import com.codename1.payment.PurchaseCallback;
import com.codename1.payment.Receipt;
import com.codename1.payments.v3.IabException;
import com.codename1.payments.v3.IabHelper;
import com.codename1.payments.v3.IabResult;
import com.codename1.payments.v3.Inventory;
import com.codename1.payments.v3.Purchase;
import com.codename1.payments.v3.SkuDetails;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class CodenameOneActivity extends Activity {

    private Menu menu;
    private boolean nativeMenu = false;
    private IntentResultListener intentResultListener;
    private IntentResultListener defaultResultListener;
    private boolean waitingForResult;
    private boolean background;
    private Vector intentResult = new Vector();
    boolean requestForPermission = false;
    
    //private final Object lock = new Object();
    private Inventory inventory;

    IabHelper mHelper;
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) {
                return;
            }
            
            if (result.isFailure()) {
                return;
            }
            List ownedItems = inventory.getAllOwnedSkus();
            for (Iterator iterator = ownedItems.iterator(); iterator.hasNext();) {
                String sku = (String)iterator.next();
                if (!isConsumable(sku)) {
                    continue;
                }
                //if the client own consumable products they need to be consumed
                Purchase pur = inventory.getPurchase(sku);
                if(pur.getItemType().equals(IabHelper.ITEM_TYPE_INAPP)){
                    mHelper.consumeAsync(pur, mConsumeFinishedListener);                
                }
            }
            CodenameOneActivity.this.inventory = inventory;
        }
    };
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(final IabResult result, final String sku, final Purchase purchase) {
            // if we were disposed of in the meantime, quit.
            if (mHelper == null) {
                return;
            }
            final PurchaseCallback pc = getPurchaseCallback();
            
            if(result.isFailure()){
                if (pc != null) {
                    Display.getInstance().callSerially(new Runnable() {

                        @Override
                        public void run() {
                            pc.itemPurchaseError(sku, result.getMessage());
                        }
                    });
                    return;
                }                        
            }

            if (!verifyDeveloperPayload(purchase)) {
                return;
            }            

            if(result.isSuccess()){
                if (pc != null) {
                    Display.getInstance().callSerially(new Runnable() {

                        @Override
                        public void run() {
                            // Sandbox transactions have no order ID, so we'll make a dummy transaction ID
                            // in this case.
                            String transactionId = (purchase.getOrderId() == null || purchase.getOrderId().isEmpty()) ? 
                                    "play-sandbox-"+UUID.randomUUID().toString() : purchase.getOrderId();
                            String purchaseJsonStr = purchase.getOriginalJson();
                            try {
                                // In order to verify receipts, we'll need both the order data and the signature
                                // so we'll pack it all into a single JSON string.
                                JSONObject purchaseJson = new JSONObject(purchaseJsonStr);
                                JSONObject rootJson = new JSONObject();
                                rootJson.put("data", purchaseJson);
                                rootJson.put("signature", purchase.getSignature());
                                purchaseJsonStr = rootJson.toString();
                                
                            } catch (JSONException ex) {
                                Logger.getLogger(CodenameOneActivity.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            com.codename1.payment.Purchase.postReceipt(Receipt.STORE_CODE_PLAY, sku, transactionId, purchase.getPurchaseTime(), purchaseJsonStr);
                            pc.itemPurchased(sku);     
                        }
                    });
                    inventory.addPurchase(purchase);
                    //This is a temp hack to get the last purchase raw data
                    //The IAP API needs to be modified to support this on all platforms
                    Display.getInstance().setProperty("lastPurchaseData", purchase.getOriginalJson());
                }            
            }

            //check if this product is a non consumable product
            if (!isConsumable(sku)) {
                return;
            }
            if(purchase.getItemType().equals(IabHelper.ITEM_TYPE_INAPP)){
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);                
            }
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(final Purchase purchase, final IabResult result) {

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) {
                return;
            }

            if (result.isFailure()) {
                final PurchaseCallback pc = getPurchaseCallback();
                if (pc != null) {
                    Display.getInstance().callSerially(new Runnable() {

                        @Override
                        public void run() {
                            String sku = null;
                            if(purchase != null){
                                sku = purchase.getSku();
                            }
                            pc.itemPurchaseError(sku, result.getMessage());
                        }
                    });
                }
            }
            if(purchase != null){
                inventory.erasePurchase(purchase.getSku());
            }
        }
    };

    private PowerManager.WakeLock wakeLock;

    /**
     * Overriden by stub, returns the user application instance.
     */
    protected Object getApp() {
        return null;
    }

    boolean wasPurchased(String item) {
        if(inventory != null){
            return inventory.hasPurchase(item);
        }
        Display.getInstance().invokeAndBlock(new Runnable() {

            @Override
            public void run() {
                while(inventory == null){
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {                    
                    }
                }
            }
        });
        return inventory.hasPurchase(item);        
    }

    void purchase(final String item) {
        //waitingForResult = true;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mHelper.launchPurchaseFlow(CodenameOneActivity.this, item, IntentResultListener.PAYMENT,
                        mPurchaseFinishedListener, getPayload());
            }
        });
    }

    void subscribe(final String item) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mHelper.launchPurchaseFlow(CodenameOneActivity.this,
                        item, IabHelper.ITEM_TYPE_SUBS,
                        IntentResultListener.PAYMENT, mPurchaseFinishedListener, getPayload());
            }
        });
    }

    public PurchaseCallback getPurchaseCallback() {
        Object app = getApp();
        PurchaseCallback pc = app instanceof PurchaseCallback ? (PurchaseCallback) app : null;
        return pc;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidImplementation.setActivity(this);
        AndroidNativeUtil.onResume();
        background = false;
    }

    /**
     * Overriden by subclasses to return true if billing is supported on this
     * build
     *
     * @return false
     */
    protected boolean isBillingEnabled() {
        return false;
    }

    /**
     * Get the Android native Menu
     *
     * @return the Android Menu Object
     */
    public Menu getMenu() {
        return menu;
    }

    /**
     * This method will enable the Android native Menu system instead of the
     * regular Form Menu.
     *
     * @param enable
     */
    public void enableNativeMenu(boolean enable) {
        nativeMenu = enable;
    }

    @Override
    public void onBackPressed() {
        Display.getInstance().keyPressed(AndroidImplementation.DROID_IMPL_KEY_BACK);
        Display.getInstance().keyReleased(AndroidImplementation.DROID_IMPL_KEY_BACK);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidImplementation.setActivity(this);
        AndroidNativeUtil.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
            getActionBar().hide();
        }

        try {
            if (isBillingEnabled()) {
                String k = getBase64EncodedPublicKey();
                if(k.length() == 0){
                    Log.e("Codename One", "android.licenseKey base64 is not configured");
                }
                mHelper = new IabHelper(this, getBase64EncodedPublicKey());
                mHelper.enableDebugLogging(true);
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {

                        if (!result.isSuccess()) {
                            // Oh noes, there was a problem.
                            Log.e("Codename One", "Problem setting up in-app billing: " + result);
                            return;
                        }

                        // Have we been disposed of in the meantime? If so, quit.
                        if (mHelper == null) {
                            return;
                        }
                        
                        // IAB is fully set up. Now, let's get an inventory of stuff we own.
                        mHelper.queryInventoryAsync(mGotInventoryListener);                        
                    }
                });

            }
        } catch (Throwable t) {
            // might happen if billing permissions are missing
            System.out.print("This exception is totally valid and here only for debugging purposes");
            t.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        AndroidImplementation.clearAppArg();
        super.onStop();
        background = true;
        unlockScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AndroidNativeUtil.onDestroy();
        if (isBillingEnabled()) {
            if (mHelper != null) {
                mHelper.dispose();
                mHelper = null;
            }
        }
        unlockScreen();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.menu = menu;
        // By returning true we signal let Android know that we want the menu
        // to be displayed
        return nativeMenu && Display.isInitialized() && Display.getInstance().getCurrent() != null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AndroidNativeUtil.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        AndroidNativeUtil.onLowMemory();
    }

    @Override
    protected void onPause() {
        if (InPlaceEditView.isEditing()) {
            AndroidImplementation.stopEditing(true);
        }
        super.onPause();
        AndroidNativeUtil.onPause();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.clear();

        try {
            Form currentForm = Display.getInstance().getCurrent();
            if (currentForm == null) {
                return false;
            }

            int numCommands = currentForm.getCommandCount();

            // If there are no commands, there's nothing to put in the menu
            if (numCommands == 0) {
                return false;
            }

            // Build menu items from commands
            for (int n = 0; n < numCommands; n++) {
                Command command = currentForm.getCommand(n);
                if (command != null) {
                    String txt = currentForm.getUIManager().localize(command.getCommandName(), command.getCommandName());
                    MenuItem item = menu.add(Menu.NONE, n, Menu.NONE, txt);

                    Image icon = command.getIcon();
                    if (icon != null) {
                        Bitmap b = (Bitmap) icon.getImage();
                        // Using BitmapDrawable with resources, to use device density (from 1.6 and above).
                        BitmapDrawable d = new BitmapDrawable(getResources(), b);
                        item.setIcon(d);
                    }
                    if (!command.isEnabled()) {
                        item.setEnabled(false);
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 11 && command.getClientProperty("android:showAsAction") != null) {
                        String androidShowAsAction = command.getClientProperty("android:showAsAction").toString();
                        // From https://developer.android.com/guide/topics/resources/menu-resource.html
                        // "ifRoom" | "never" | "withText" | "always" | "collapseActionView"
                        if (androidShowAsAction.equalsIgnoreCase("ifRoom")) {
                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        } else if (androidShowAsAction.equalsIgnoreCase("never")) {
                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                        } else if (androidShowAsAction.equalsIgnoreCase("withText")) {
                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                        } else if (androidShowAsAction.equalsIgnoreCase("always")) {
                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        } else if (android.os.Build.VERSION.SDK_INT >= 14 && androidShowAsAction.equalsIgnoreCase("collapseActionView")) {
                            item.setShowAsAction(8); //MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
                        }
                    }
                }
            }
        } catch (Throwable t) {
        }

        return nativeMenu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        final Form currentForm = Display.getInstance().getCurrent();
        if (currentForm == null) {
            return false;
        }
        Command cmd = null;
        final boolean[] tmpProp = new boolean[1];
        if (item.getItemId() == android.R.id.home) {
            cmd = currentForm.getBackCommand();
            if (cmd == null) {
                return false;
            }
            cmd.putClientProperty("source", "ActionBar");
            tmpProp[0] = true;
        }

        int commandIndex = item.getItemId();
        if (cmd == null) {
            cmd = currentForm.getCommand(commandIndex);
        }
        final Command command = cmd;
        final ActionEvent actionEvent = new ActionEvent(command);

        //stop edit if the keybaord is open
        AndroidImplementation.stopEditing();
        // Protect ourselves from commands that misbehave. A crash here will crash the entire application
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    currentForm.dispatchCommand(command, actionEvent);
                    //remove the temp source property
                    if (tmpProp[0]) {
                        command.putClientProperty("source", null);
                    }
                } catch (Throwable e) {
                    Log.e("CodenameOneActivity.onOptionsItemSelected", e.toString() + Log.getStackTraceString(e));
                }
            }
        });

        return true;
    }

    protected void fireIntentResult() {
        if (intentResult.size() > 0) {
            final IntentResult response = (IntentResult) intentResult.get(0);
            if (intentResultListener != null && response != null) {
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        intentResultListener.onActivityResult(response.getRequestCode(),
                                response.getResultCode(),
                                response.getData());
                    }
                });
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //is this a payment result
        if (mHelper != null && mHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        IntentResult response = new IntentResult(requestCode, resultCode, data);
        intentResult.add(response);
    }

    public void setIntentResultListener(IntentResultListener l) {
        //if the activity is waiting for result don't override the intent listener
        if(waitingForResult){
            return;
        }
        this.intentResultListener = l;
        if (l != null && l != defaultResultListener) {
            waitingForResult = true;
        }
    }

    public void setDefaultIntentResultListener(IntentResultListener l) {
        this.defaultResultListener = l;
    }

    public void restoreIntentResultListener() {
        waitingForResult = false;
        setIntentResultListener(defaultResultListener);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        Bundle extra = intent.getExtras();
        if(extra != null && extra.containsKey("WaitForResult") && !extra.getBoolean("WaitForResult")){
            waitingForResult = false;            
        }else{
            waitingForResult = true;
        }
        intentResult = new Vector();
        if (InPlaceEditView.isEditing()) {
            AndroidImplementation.stopEditing(true);
        }
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startActivity(Intent intent) {
        Bundle extra = intent.getExtras();
        if(extra != null && extra.containsKey("WaitForResult") && !extra.getBoolean("WaitForResult")){
            waitingForResult = false;            
        }else{
            waitingForResult = true;
        }
        if (InPlaceEditView.isEditing()) {
            AndroidImplementation.stopEditing(true);
        }
        super.startActivity(intent);
    }

    public boolean isWaitingForResult() {
        return waitingForResult;
    }

    protected void setWaitingForResult(boolean waitingForResult) {
        this.waitingForResult = waitingForResult;
    }

    public boolean isBackground() {
        return background;
    }

    public void registerForPush(String key) {
        Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
        registrationIntent.setPackage("com.google.android.gms");
        registrationIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0)); // boilerplate
        registrationIntent.putExtra("sender", key);
        startService(registrationIntent);
    }

    public void stopReceivingPush() {
        Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
        unregIntent.setPackage("com.google.android.gms");
        unregIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0));
        startService(unregIntent);
    }

    public void lockScreen() {
        unlockScreen();
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK | android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP | android.os.PowerManager.ON_AFTER_RELEASE, "Codename One");
        } catch (Exception excp) {
            excp.printStackTrace();
        }
        if (wakeLock != null) {
            wakeLock.acquire();
        }
    }

    public void unlockScreen() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    public String getBase64EncodedPublicKey() {
        String key = Display.getInstance().getProperty("android.licenseKey", "");        
        return key;
    }

    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        return true;
    }

    String getPayload() {
        return "";
    }

    Product[] getProducts(String[] skus){
        return getProducts(skus, false);
    }
    
    Product[] getProducts(String[] skus, boolean fromCacheOnly){
        
        if(inventory != null){
            ArrayList pList = new ArrayList<Product>();
            ArrayList moreskusList = new ArrayList<Product>();
            for (int i = 0; i < skus.length; i++) {
                String sku = skus[i];
                if(inventory.hasDetails(sku)){
                    SkuDetails details = inventory.getSkuDetails(sku);
                    Product p = new Product();
                    p.setSku(sku);
                    p.setDescription(details.getDescription());
                    p.setDisplayName(details.getTitle());
                    p.setLocalizedPrice(details.getPrice());
                    pList.add(p);
                }else{
                    moreskusList.add(sku);
                }                
            }
            //if the inventory does not all the requestes sku make an update.
            if(moreskusList.size() > 0 && !fromCacheOnly){
                try {
                    inventory = mHelper.queryInventory(true, moreskusList);
                    return getProducts(skus, true);
                } catch (IabException ex) {
                    Logger.getLogger(CodenameOneActivity.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            Product [] products = new Product[pList.size()];
            products = (Product[]) pList.toArray(products);
            return products;
        }
        return null;
    }
    
    public boolean isConsumable(String sku){
        if (sku.endsWith("nonconsume")) {
            return false;
        }
        return true;
    }

    public void setRequestForPermission(boolean requestForPermission) {
        this.requestForPermission = requestForPermission;
    }

    public boolean isRequestForPermission() {
        return requestForPermission;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("Codename One", "PERMISSION_GRANTED");
        } else {
            // Permission Denied
            Toast.makeText(this, "Permission is denied", Toast.LENGTH_SHORT).show();
        }
        requestForPermission = false;
    }
    
    public boolean hasUI(){
        return true;
    }
}
