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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;

public class CodenameOneActivity extends Activity {
    private Menu menu;
    private boolean nativeMenu = false;
    private IntentResultListener intentResultListener;
    private boolean waitingForResult;
    
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
            MenuItem item = menu.add(Menu.NONE, n, Menu.NONE, command.getCommandName());

            Image icon = command.getIcon();
            if (icon != null) {
                Bitmap b = (Bitmap) icon.getImage();
                // Using BitmapDrawable with resources, to use device density (from 1.6 and above).
                BitmapDrawable d = new BitmapDrawable(b);
                item.setIcon(d);
            }
        }

        return nativeMenu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        Form currentForm = Display.getInstance().getCurrent();
        if (currentForm == null) {
            return false;
        }

        int commandIndex = item.getItemId();
        Command command = currentForm.getCommand(commandIndex);
        ActionEvent actionEvent = new ActionEvent(command);

        // Protect ourselves from commands that misbehave. A crash here will crash the entire application
        try {
            currentForm.dispatchCommand(command, actionEvent);
        } catch (Throwable e) {
            Log.e("CodenameOneActivity.onOptionsItemSelected", e.toString() + Log.getStackTraceString(e));
        }

        return true;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(intentResultListener != null){
            intentResultListener.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void setIntentResultListener(IntentResultListener l) {
        this.intentResultListener = l;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        waitingForResult = true;
        super.startActivityForResult(intent, requestCode);
    }

    public boolean isWaitingForResult() {
        return waitingForResult;
    }

    protected void setWaitingForResult(boolean waitingForResult) {
        this.waitingForResult = waitingForResult;
    }

            
    
    
}
