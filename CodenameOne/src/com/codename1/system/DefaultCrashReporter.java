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
package com.codename1.system;

import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import java.util.TimerTask;

/**
 * A default implementation of the crash reporter that instantly sends the crash
 * data to the server.
 *
 * @author Shai Almog
 */
public class DefaultCrashReporter implements CrashReport {
    private boolean promptUser = false;
    private static String errorText = "The application encountered an error, do you wish to report it?";
    private static String sendButtonText = "Send";
    private static String dontSendButtonText = "Don't Send";
    private static String checkboxText = "Don't show this dialog again";

    /**
     * The text for the user prompt dialog
     * @return the errorText
     */
    public static String getErrorText() {
        return errorText;
    }

    /**
     * The text for the user prompt dialog
     * @param aErrorText the errorText to set
     */
    public static void setErrorText(String aErrorText) {
        errorText = aErrorText;
    }

    /**
     * The text for the user prompt dialog
     * @return the sendButtonText
     */
    public static String getSendButtonText() {
        return sendButtonText;
    }

    /**
     * The text for the user prompt dialog
     * @param aSendButtonText the sendButtonText to set
     */
    public static void setSendButtonText(String aSendButtonText) {
        sendButtonText = aSendButtonText;
    }

    /**
     * The text for the user prompt dialog
     * @return the dontSendButtonText
     */
    public static String getDontSendButtonText() {
        return dontSendButtonText;
    }

    /**
     * The text for the user prompt dialog
     * @param aDontSendButtonText the dontSendButtonText to set
     */
    public static void setDontSendButtonText(String aDontSendButtonText) {
        dontSendButtonText = aDontSendButtonText;
    }

    /**
     * The text for the user prompt dialog
     * @return the checkboxText
     */
    public static String getCheckboxText() {
        return checkboxText;
    }

    /**
     * The text for the user prompt dialog
     * @param aCheckboxText the checkboxText to set
     */
    public static void setCheckboxText(String aCheckboxText) {
        checkboxText = aCheckboxText;
    }

    private DefaultCrashReporter() {}
    
    /**
     * Installs a crash reporter within the system
     * @param promptUser indicates whether the user should be prompted on crash reporting
     * @param frequency the frequency with which we send the log to the server in debug mode in minutes
     * frequency must be at least 1. Any lower level automatically disables this feature
     */
    public static void init(boolean promptUser, int frequency) {
        if(Preferences.get("$CN1_crashBlocked", false) || Log.getReportingLevel() == Log.REPORTING_NONE) {
            return;
        }
        if(Preferences.get("$CN1_pendingCrash", false)) {
            // we must have crashed during a report, send it.
            Log.sendLog();
            Preferences.set("$CN1_pendingCrash", false);
        }
        if(Log.getReportingLevel() == Log.REPORTING_DEBUG && frequency > 0) {
            java.util.Timer t = new java.util.Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    Log.sendLog();
                }
            }, frequency * 60000, frequency * 60000);
        }
        DefaultCrashReporter d = new DefaultCrashReporter();
        d.promptUser = promptUser && Preferences.get("$CN1_prompt", true);
        Display.getInstance().setCrashReporter(d);
    }

    /**
     * @inheritDoc
     */
    public void exception(Throwable t) {
        Preferences.set("$CN1_pendingCrash", true);
        if(promptUser) {
            Dialog error = new Dialog("Error");
            error.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            TextArea txt = new TextArea(errorText);
            txt.setEditable(false);
            txt.setUIID("DialogBody");
            error.addComponent(txt);
            CheckBox cb = new CheckBox(checkboxText);
            cb.setUIID("DialogBody");
            error.addComponent(cb);
            Container grid = new Container(new GridLayout(1, 2));
            error.addComponent(grid);
            Command ok = new Command(sendButtonText);
            Command dont = new Command(dontSendButtonText);
            Button send = new Button(ok);
            Button dontSend = new Button(dont);
            grid.addComponent(send);
            grid.addComponent(dontSend);
            Command result = error.showPacked(BorderLayout.CENTER, true);
            if(result == dont) {
                if(cb.isSelected()) {
                    Preferences.set("$CN1_crashBlocked", true);
                }
                Preferences.set("$CN1_pendingCrash", false);
                return;
            } else {
                if(cb.isSelected()) {
                    Preferences.set("$CN1_prompt", false);
                }
            }
        }
        Log.sendLog();
        Preferences.set("$CN1_pendingCrash", false);
    }
}
