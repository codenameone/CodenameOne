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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import com.codename1.payment.PurchaseCallback;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class CodenameOneActivity extends Activity {
    private Menu menu;
    private boolean nativeMenu = false;
    private IntentResultListener intentResultListener;
    private IntentResultListener defaultResultListener;
    private boolean waitingForResult;
    private boolean background;
    private Vector intentResult = new Vector();
    /**
     * The SharedPreferences key for recording whether we initialized the
     * database.  If false, then we perform a RestoreTransactions request
     * to get all the purchases for this user.
     */
    private static final String BILLING_DB_INITIALIZED = "billing_db_initialized";
    private CN1PurchaseObserver cnPurchaseObserver;
    private BillingService billing;
    private PurchaseDatabase purchaseDB;
    
    private final Object lock = new Object();
    private Set<String> ownedItems;
    private boolean inAppBillingSupported = false;
    private boolean subscriptionSupported = false;
    
    private PowerManager.WakeLock wakeLock;
    
    /**
     * Each product in the catalog is either MANAGED or UNMANAGED.  MANAGED
     * means that the product can be purchased only once per user (such as a new
     * level in a game). The purchase is remembered by Android Market and
     * can be restored if this application is uninstalled and then
     * re-installed. UNMANAGED is used for products that can be used up and
     * purchased multiple times (such as poker chips). It is up to the
     * application to keep track of UNMANAGED products for the user.
     */
    private enum Managed { MANAGED, UNMANAGED }
    
    /**
     * If the database has not been initialized, we send a
     * RESTORE_TRANSACTIONS request to Android Market to get the list of purchased items
     * for this user. This happens if the application has just been installed
     * or the user wiped data. We do not want to do this on every startup, rather, we want to do
     * only when the database needs to be initialized.
     */
    private void restoreDatabase() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(BILLING_DB_INITIALIZED, false);
        if (!initialized) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    billing.restoreTransactions();
                }
            });
        }
    }

    public boolean isInAppBillingSupported() {
        return inAppBillingSupported;
    }
    
    public boolean isSubscriptionSupported() {
        return subscriptionSupported;
    }
    
    private void addItem(String item) {
        synchronized(lock) {
            if(ownedItems == null) {
                ownedItems = new HashSet<String>();
            }
            ownedItems.add(item);
        }
    }
    
    private void removeItem(String item) {
        synchronized(lock) {
            if(ownedItems != null) {
                ownedItems.remove(item);
            }
        }
    }
    
    /**
     * Overriden by stub, returns the user application instance.
     */
    protected Object getApp() {
        return null;
    }
    
    boolean wasPurchased(String item) {
        synchronized(lock) {
            return ownedItems != null ? ownedItems.contains(item) : false;
        }
    }
    
    void purchase(final String item) {
        waitingForResult = true;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                billing.requestPurchase(item, Consts.ITEM_TYPE_INAPP, null);
            }
        });    
    }

    void subscribe(final String item) {
        if(subscriptionSupported) {
            waitingForResult = true;
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    billing.requestPurchase(item, Consts.ITEM_TYPE_SUBSCRIPTION, null);
                }
            });
        }
    }
    
    public PurchaseCallback getPurchaseCallback() {
        Object app = getApp();
        PurchaseCallback pc = app instanceof PurchaseCallback ? (PurchaseCallback)app : null;
        return pc;
    }

    /**
     * A {@link PurchaseObserver} is used to get callbacks when Android Market sends
     * messages to this application so that we can update the UI.
     */
    private class CN1PurchaseObserver extends PurchaseObserver {
        public CN1PurchaseObserver(Handler handler) {
            super(CodenameOneActivity.this, handler);
        }

        @Override
        public void onBillingSupported(boolean supported, String type) {
            if(type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
                if (supported) {
                    restoreDatabase();
                    inAppBillingSupported = true;
                }
            } else if(type.equals(Consts.ITEM_TYPE_SUBSCRIPTION)) {
                if(supported) {
                    restoreDatabase();
                    inAppBillingSupported = true;
                    subscriptionSupported = true;
                }
                else {
                    CodenameOneActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            billing.checkBillingSupported(Consts.ITEM_TYPE_INAPP);
                        }
                    });
                }
            }
        }
        
        @Override
        public void onPurchaseStateChange(Consts.PurchaseState purchaseState, String itemId,
                int quantity, long purchaseTime, String developerPayload) {
            if (Consts.DEBUG) {
                Log.i("CodeNameOne", "onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);
            }
            PurchaseCallback pc = getPurchaseCallback();
            if(purchaseState == Consts.PurchaseState.PURCHASED) {
                addItem(itemId);
                if(pc != null) {
                    pc.itemPurchased(itemId);
                }
            } else if(purchaseState == Consts.PurchaseState.REFUNDED || purchaseState == Consts.PurchaseState.CANCELED || purchaseState == Consts.PurchaseState.EXPIRED) {
                if(quantity <= 0) {
                    removeItem(itemId);
                }
                if(pc != null) {
                    pc.itemRefunded(itemId);
                }
            }
}

        @Override
        public void onRequestPurchaseResponse(final BillingService.RequestPurchase request,
                final Consts.ResponseCode responseCode) {
            Object app = getApp();
            if(app != null && app instanceof PurchaseCallback) {
                final PurchaseCallback pc = (PurchaseCallback)app;
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        if (responseCode == Consts.ResponseCode.RESULT_OK) {
                            // purchase was successfully sent to server
                        } else if (responseCode == Consts.ResponseCode.RESULT_USER_CANCELED) {
                            // user canceled purchase
                            pc.itemPurchaseError(request.mProductId, "Canceled");
                        } else {
                            // purchase failed
                            pc.itemPurchaseError(request.mProductId, responseCode.name());
                        }
                    }
                });
            }
        }

        @Override
        public void onRestoreTransactionsResponse(BillingService.RestoreTransactions request,
                Consts.ResponseCode responseCode) {
            if (responseCode == Consts.ResponseCode.RESULT_OK) {
                // completed RestoreTransactions request
                // Update the shared preferences so that we don't perform
                // a RestoreTransactions again.
                SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(BILLING_DB_INITIALIZED, true);
                edit.commit();
            } else {
                // RestoreTransactions error
            }
        }
    }

    
    @Override
    protected void onResume() {
        super.onResume();
        waitingForResult = false;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
            getActionBar().hide();
        }
        
        try {
            if(isBillingEnabled()) {
                Handler mHandler = new Handler();
                cnPurchaseObserver = new CN1PurchaseObserver(mHandler);
                billing = new BillingService();
                billing.setContext(this);

                purchaseDB = new PurchaseDatabase(this);

                // Check if billing is supported.
                ResponseHandler.register(cnPurchaseObserver);
                billing.checkBillingSupported(Consts.ITEM_TYPE_SUBSCRIPTION);
            }
        } catch(Throwable t) {
            // might happen if billing permissions are missing
            System.out.print("This exception is totally valid and here only for debugging purposes");
            t.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(isBillingEnabled()) {
            ResponseHandler.register(cnPurchaseObserver);
            initializeOwnedItems();
            //purchaseDB.enumerateHistory("com.tspx.tvportal.payment.upgrade");
        }
    }

     /**
     * Creates a background thread that reads the database and initializes the
     * set of owned items.
     */
    private void initializeOwnedItems() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doInitializeOwnedItems();
            }
        }).start();
    }
    
    /**
     * Reads the set of purchased items from the database in a background thread
     * and then adds those items to the set of owned items
     */
    private void doInitializeOwnedItems() {
        Cursor cursor = purchaseDB.queryAllPurchasedItems();
        if (cursor == null) {
            return;
        }

        Set<String> newItems = new HashSet<String>();
        try {
            int productIdCol = cursor.getColumnIndexOrThrow(
                    PurchaseDatabase.PURCHASED_PRODUCT_ID_COL);
            int quantityCol = cursor.getColumnIndexOrThrow(PurchaseDatabase.PURCHASED_QUANTITY_COL);
            while (cursor.moveToNext()) {
                int quanity = cursor.getInt(quantityCol);
                if(quanity > 0) {
                    String productId = cursor.getString(productIdCol);
                    newItems.add(productId);
                }
            }
        } finally {
            cursor.close();
        }

        if(newItems.size() > 0) {
            synchronized(lock) {
                if(ownedItems != null) {
                    ownedItems.addAll(newItems);
                }
                else {
                    ownedItems = new HashSet<String>(newItems);
                }
            }
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        background = true;
        if(isBillingEnabled()) {
            ResponseHandler.unregister(cnPurchaseObserver);
        }
        unlockScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBillingEnabled()) {
            purchaseDB.close();
            billing.unbind();
        }
        unlockScreen();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.menu = menu;
        // By returning true we signal let Android know that we want the menu
        // to be displayed
        return nativeMenu;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.clear();

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
            String txt = currentForm.getUIManager().localize(command.getCommandName(), command.getCommandName());
            MenuItem item = menu.add(Menu.NONE, n, Menu.NONE, txt);
            
            Image icon = command.getIcon();
            if (icon != null) {
                Bitmap b = (Bitmap) icon.getImage();
                // Using BitmapDrawable with resources, to use device density (from 1.6 and above).
                BitmapDrawable d = new BitmapDrawable(b);
                item.setIcon(d);
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
        if(item.getItemId() == android.R.id.home){
            cmd = currentForm.getBackCommand();
            if(cmd == null){
                return false;
            }
        }
        
        int commandIndex = item.getItemId();
        if(cmd == null){
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
                } catch (Throwable e) {
                    Log.e("CodenameOneActivity.onOptionsItemSelected", e.toString() + Log.getStackTraceString(e));
                }
            }
        });

        return true;
    }
    
    protected void fireIntentResult() {
        if(intentResult.size() > 0){
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
        IntentResult response = new IntentResult(requestCode, resultCode, data);
        intentResult.add(response);
    }
    
    public void setIntentResultListener(IntentResultListener l) {
        this.intentResultListener = l;
    }

    public void setDefaultIntentResultListener(IntentResultListener l) {
        this.defaultResultListener = l;
    }
    
    public void restoreIntentResultListener(){
        setIntentResultListener(defaultResultListener);
    }
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        waitingForResult = true;
        intentResult = new Vector();
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startActivity(Intent intent) {
        waitingForResult = true;
        super.startActivity(intent);
    }
    
    

    public boolean isWaitingForResult() {
        return waitingForResult;
    }

    protected void setWaitingForResult(boolean waitingForResult) {
        this.waitingForResult = waitingForResult;
    }
    
    public boolean isBackground(){
        return background;
    }

            
    public void registerForPush(String key) {
        Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
        registrationIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0)); // boilerplate
        registrationIntent.putExtra("sender", key);
        startService(registrationIntent);
    }
    
    public void stopReceivingPush() {
        Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
        unregIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0));
        startService(unregIntent);
    }
    
    
    public void lockScreen(){
        unlockScreen();
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK | android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP | android.os.PowerManager.ON_AFTER_RELEASE, "Codename One");
        } catch (Exception excp) {
            excp.printStackTrace();
        }
        if(wakeLock != null){
            wakeLock.acquire();
        }
    }
    
    public void unlockScreen(){
        if(wakeLock != null && wakeLock.isHeld()){
            wakeLock.release();
            wakeLock = null;
        }
    }
    
    
}
