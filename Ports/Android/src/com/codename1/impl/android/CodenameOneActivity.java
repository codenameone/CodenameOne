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
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;

import java.util.Vector;

public class CodenameOneActivity extends Activity {




    private Menu menu;
    private boolean nativeMenu = false;
    private IntentResultListener intentResultListener;
    private IntentResultListener defaultResultListener;
    private boolean waitingForResult, waitingForPermissionResult;
    private boolean background;
    private Vector intentResult = new Vector();
    boolean requestForPermission = false;

    private IBillingSupport billingSupport;

    private PowerManager.WakeLock wakeLock;

    /**
     * Overriden by stub, returns the user application instance.
     */
    protected Object getApp() {
        return null;
    }

    boolean wasPurchased(String item) {
        if (billingSupport != null) {
            return billingSupport.wasPurchased(item);
        }
        return false;
    }

    void purchase(final String item) {
        if (billingSupport != null) billingSupport.purchase(item);
    }


    void subscribe(final String item) {
        if (billingSupport != null) billingSupport.subscribe(item);
    }


    protected IBillingSupport createBillingSupport() {
        return null;
    }

    private IBillingSupport getBillingSupport() {
        if (billingSupport == null) {
            billingSupport = createBillingSupport();
        }
        return billingSupport;
    }


    @Override
    protected void onResume() {
        super.onResume();
        AndroidImplementation.setActivity(this);
        AndroidNativeUtil.onResume();
        if (isBillingEnabled() && getBillingSupport() != null) {
            billingSupport.consumeAndAcknowlegePurchases();
        }
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
                getBillingSupport().initBilling();


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
            getBillingSupport().onDestroy();
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
            if (currentForm == null || Toolbar.isGlobalToolbar()) {
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



    String getPayload() {
        return "";
    }







    public boolean isConsumable(String item) {
        if (getBillingSupport() != null) {
            return getBillingSupport().isConsumable(item);
        }
        return false;
    }

    Product[] getProducts(String[] skus){
        if (getBillingSupport() != null) {
            return getBillingSupport().getProducts(skus, false);
        }
        return new Product[0];
    }
    

    


    public void setRequestForPermission(boolean requestForPermission) {
        this.requestForPermission = requestForPermission;
    }

    public void setWaitingForPermissionResult(boolean waitingForPermissionResult) {
        this.waitingForPermissionResult = waitingForPermissionResult;
    }

    public boolean isWaitingForPermissionResult() {
        return waitingForPermissionResult;
    }

    public boolean isRequestForPermission() {
        return requestForPermission;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(grantResults != null || grantResults.length == 0) {
            requestForPermission = false;
            return;
        }
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
