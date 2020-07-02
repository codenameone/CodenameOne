// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;

import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.misc.BoolRef;

public class JSDialogHandler extends CefJSDialogHandlerAdapter {
    @Override
    public boolean onJSDialog(CefBrowser browser, String origin_url, JSDialogType dialog_type,
            String message_text, String default_prompt_text, CefJSDialogCallback callback,
            BoolRef suppress_message) {
        
        switch (dialog_type) {
            case JSDIALOGTYPE_ALERT:
                CN.callSeriallyAndWait(new Runnable() {
                    public void run() {
                        Dialog.show(origin_url+" says", message_text, "OK", null);
                    }
                });
                return true;
        }
        
        if (message_text.equalsIgnoreCase("Never displayed")) {
            suppress_message.set(true);
            System.out.println(
                    "The " + dialog_type + " from origin \"" + origin_url + "\" was suppressed.");
            System.out.println(
                    "   The content of the suppressed dialog was: \"" + message_text + "\"");
        }
        return false;
    }
}
