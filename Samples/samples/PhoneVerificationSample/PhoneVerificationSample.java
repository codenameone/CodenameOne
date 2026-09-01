/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.samples;

import com.codename1.components.OtpField;
import com.codename1.components.PhoneNumberField;
import com.codename1.components.PhoneVerification;
import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UITimer;

/// Phone number verification, end to end, against a fake server.
///
/// There is nothing to configure: `sendCode` accepts any number and pretends to
/// send `123456` a second later, and `verifyCode` accepts that code and refuses
/// every other one. The point of the sample is the client half -- the segmented
/// code field, the resend countdown, the way back to a mistyped number, and the
/// one-time-code hint that lets the platform offer the code from a real message.
///
/// On a device, put a real code screen beside this one: send yourself an SMS
/// containing a six digit code while the code stage is showing, and the platform
/// offers it above the keyboard (iOS) or through autofill (Android). Nothing in
/// this sample reads messages.
public class PhoneVerificationSample {

    private static final String FAKE_CODE = "123456";

    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        showFlow();
    }

    private void showFlow() {
        Form f = new Form("Verify a phone number", BoxLayout.y());
        f.add(new SpanLabel("The server here is fake: any number is accepted, and the code is "
                + FAKE_CODE + "."));

        PhoneVerification verify = new PhoneVerification();
        verify.setResendDelay(10);
        verify.setCodeSender(new PhoneVerification.CodeSender() {
            public void sendCode(String e164Number, PhoneVerification.Response response) {
                // a real sender posts to its own server; the delay is here so the
                // disabled button and the countdown are visible
                UITimer.timer(1000, false, new Runnable() {
                    public void run() {
                        response.succeeded();
                    }
                });
            }
        });
        verify.setCodeVerifier(new PhoneVerification.CodeVerifier() {
            public void verifyCode(String e164Number, String code, PhoneVerification.Response response) {
                UITimer.timer(600, false, new Runnable() {
                    public void run() {
                        if (FAKE_CODE.equals(code)) {
                            response.succeeded();
                        } else {
                            response.failed("That code is not " + FAKE_CODE);
                        }
                    }
                });
            }
        });
        verify.addVerifiedListener(e -> showVerified(verify.getPhoneNumber()));
        f.add(verify);

        f.add(new Label(" "));
        f.add(new Label("The pieces on their own"));
        f.add(bareOtpField());
        f.add(barePhoneField());
        f.show();
    }

    /// The code field used without the flow around it.
    private com.codename1.ui.Container bareOtpField() {
        OtpField otp = new OtpField(6);
        Label read = new Label("");
        otp.addCompleteListener(e -> read.setText("complete: " + otp.getText()));
        Button clear = new Button("Clear");
        clear.addActionListener(e -> otp.clear());
        return BoxLayout.encloseY(new Label("OtpField"), otp, read, clear);
    }

    /// The number field used without the flow around it, and a plain TextField
    /// carrying the same hint the OtpField carries.
    private com.codename1.ui.Container barePhoneField() {
        PhoneNumberField phone = new PhoneNumberField();
        Label read = new Label("");
        Button show = new Button("Read as E.164");
        show.addActionListener(e -> read.setText(String.valueOf(phone.getE164())));
        TextField plainCode = new TextField("", "Code in a plain field", 6,
                TextArea.NUMERIC | TextArea.ONE_TIME_CODE);
        return BoxLayout.encloseY(new Label("PhoneNumberField"), phone, show, read,
                new Label("TextArea.ONE_TIME_CODE on a plain TextField"), plainCode);
    }

    private void showVerified(String number) {
        ToastBar.showMessage("Verified " + number, com.codename1.ui.FontImage.MATERIAL_CHECK);
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
        if (current instanceof com.codename1.ui.Dialog) {
            ((com.codename1.ui.Dialog) current).dispose();
            current = Display.getInstance().getCurrent();
        }
    }

    public void destroy() {
    }
}
