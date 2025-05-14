// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import java.awt.EventQueue;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.misc.BoolRef;

public class JSDialogHandler extends CefJSDialogHandlerAdapter {
    @Override
    public boolean onJSDialog(final CefBrowser browser, final String origin_url, final JSDialogType dialog_type,
            final String message_text, final String default_prompt_text, final CefJSDialogCallback callback,
            BoolRef suppress_message) {
        
        switch (dialog_type) {
            case JSDIALOGTYPE_ALERT:
                CN.callSerially(new Runnable() {
                    public void run() {
                        final Sheet sheet = new Sheet(null, origin_url+" says");
                        sheet.setPosition(com.codename1.ui.layouts.BorderLayout.CENTER);
                        SpanLabel lbl = new SpanLabel(message_text);
                        sheet.getContentPane().setLayout(new BorderLayout());
                        sheet.getContentPane().add(BorderLayout.CENTER, lbl);
                        sheet.addCloseListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        callback.Continue(true, "");
                                        
                                    }
                                });
                            }
                        });
                        Button btn = new Button("OK");
                        btn.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                sheet.back();
                            }
                        });
                        sheet.getContentPane().add(BorderLayout.SOUTH, btn);
                        sheet.show();
                        
                        //Dialog.show(origin_url+" says", message_text, "OK", null);
                        //callback.Continue(true, "");
                    }
                });
                return true;
                
            case JSDIALOGTYPE_CONFIRM:
                CN.callSerially(new Runnable() {
                    public void run() {
                        final Sheet sheet = new Sheet(null, origin_url+" says");
                        sheet.setPosition(com.codename1.ui.layouts.BorderLayout.CENTER);
                        SpanLabel lbl = new SpanLabel(message_text);
                        sheet.getContentPane().setLayout(new BorderLayout());
                        sheet.getContentPane().add(BorderLayout.CENTER, lbl);
                        final boolean[] callbackComplete = new boolean[1];
                        sheet.addCloseListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        if (callbackComplete[0]) {
                                            return;
                                        }
                                        callbackComplete[0] = true;
                                        callback.Continue(false, "");
                                        
                                    }
                                });
                            }
                        });
                        Button okBtn = new Button("OK");
                        okBtn.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        if (callbackComplete[0]) {
                                            return;
                                        }
                                        callbackComplete[0] = true;
                                        callback.Continue(true, "");
                                        
                                    }
                                });
                                sheet.back();
                                
                            }
                        });
                        
                        Button cancelBtn = new Button("Cancel");
                        cancelBtn.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                
                                sheet.back();
                                
                            }
                        });
                        
                        
                        sheet.getContentPane().add(BorderLayout.SOUTH, GridLayout.encloseIn(2, okBtn, cancelBtn));
                        sheet.show();
                        
                        //Dialog.show(origin_url+" says", message_text, "OK", null);
                        //callback.Continue(true, "");
                    }
                });
                return true;
                
            case JSDIALOGTYPE_PROMPT:
                 CN.callSerially(new Runnable() {
                    public void run() {
                        final Sheet sheet = new Sheet(null, origin_url+" says");
                        sheet.setPosition(com.codename1.ui.layouts.BorderLayout.CENTER);
                        SpanLabel lbl = new SpanLabel(message_text);
                        sheet.getContentPane().setLayout(new BorderLayout());
                        
                        final TextField input = new TextField();
                        if (default_prompt_text != null) {
                            input.setText(default_prompt_text);
                        }
                        sheet.getContentPane().add(BorderLayout.CENTER, BoxLayout.encloseY(lbl, input));
                        final boolean[] callbackComplete = new boolean[1];
                        sheet.addCloseListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        if (callbackComplete[0]) {
                                            return;
                                        }
                                        callbackComplete[0] = true;
                                        callback.Continue(false, null);
                                        
                                    }
                                });
                            }
                        });
                        Button okBtn = new Button("OK");
                        okBtn.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        if (callbackComplete[0]) {
                                            return;
                                        }
                                        callbackComplete[0] = true;
                                        callback.Continue(true, input.getText());
                                        
                                    }
                                });
                                sheet.back();
                                
                            }
                        });
                        
                        Button cancelBtn = new Button("Cancel");
                        cancelBtn.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                
                                sheet.back();
                                
                            }
                        });
                        
                        
                        sheet.getContentPane().add(BorderLayout.SOUTH, GridLayout.encloseIn(2, okBtn, cancelBtn));
                        sheet.show();
                        input.requestFocus();
                        
                        //Dialog.show(origin_url+" says", message_text, "OK", null);
                        //callback.Continue(true, "");
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
