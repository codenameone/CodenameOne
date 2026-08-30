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
package com.codename1.impl.android.call;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/// The ringing screen a self-managed calling app gets no system UI for.
///
/// Android arbitrates a self-managed call, routes its audio and writes the
/// call log, but draws nothing. The port answers that with a call-category
/// notification carrying a full-screen intent -- and a full-screen intent is
/// only as good as the activity it launches.
///
/// It used to launch the application's LAUNCHER activity. That is not a
/// ringing screen and Android does not make it one: the user arrived at
/// whatever form the app happens to open with, holding no Answer button,
/// while the notification that did hold one had been replaced by this
/// full-screen launch rather than shown alongside it. On a locked device it
/// was worse still, because an ordinary launcher activity has no
/// `showWhenLocked`, so it opened BEHIND the keyguard and the user saw
/// nothing at all until they unlocked.
///
/// So the port draws the screen it promised. Deliberately plain: a caller
/// line and two buttons, sized in code because this class ships inside the
/// port and has no resources of its own -- the same constraint that makes
/// the notification borrow the application icon. An app that wants its own
/// ringing UI still has one: it gets the session through the ordinary
/// listeners and can present whatever it likes on top.
///
/// Answer and Decline go through [CN1CallNotifications#answer] and
/// [CN1CallNotifications#decline], which is the same path the notification
/// actions take -- one implementation of "what answering means", so the two
/// surfaces cannot come to disagree.
public class CN1IncomingCallActivity extends Activity {

    /// The call this screen is ringing for.
    static final String EXTRA_CALL_ID = CN1ConnectionService.EXTRA_CALL_ID;

    private String callId;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        showOverKeyguard();
        callId = getIntent() == null ? null
                : getIntent().getStringExtra(EXTRA_CALL_ID);
        if (callId == null || CN1ConnectionService.find(callId) == null) {
            // The call went away between the notification being posted and
            // this screen opening -- the caller hung up, or the user answered
            // from the notification. Finishing rather than showing a screen
            // for a call nobody can answer.
            finish();
            return;
        }
        setContentView(buildUi());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        // SINGLE_TOP, so a second notification for the same call reuses this
        // instance; without this the screen would keep ringing for the call
        // it was opened with.
        setIntent(intent);
        String id = intent == null ? null
                : intent.getStringExtra(EXTRA_CALL_ID);
        if (id != null) {
            callId = id;
        }
    }

    /// Puts this window in front of the lock screen and lights the display.
    ///
    /// Both halves are needed and neither is the default. Without the
    /// keyguard flags the activity opens behind the lock screen, which is
    /// exactly where a ringing call must not be; without the screen-on flag
    /// it rings to a dark display.
    private void showOverKeyguard() {
        if (Build.VERSION.SDK_INT >= 27) {
            // The supported way from Oreo MR1. Reflective because this port
            // compiles against an older android.jar and these two arrived in
            // API 27; the flags below are deprecated from the same release,
            // so calling the methods where they exist is what keeps this
            // working as the deprecation tightens.
            if (invokeBoolean("setShowWhenLocked", true)
                    && invokeBoolean("setTurnScreenOn", true)) {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                dismissKeyguard();
                return;
            }
            // Fall through when a platform claims 27 without the methods,
            // which is not one this can reason about; the flags still work.
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    private boolean invokeBoolean(String method, boolean value) {
        try {
            java.lang.reflect.Method m =
                    Activity.class.getMethod(method, boolean.class);
            m.invoke(this, Boolean.valueOf(value));
            return true;
        } catch (Exception unavailable) {
            return false;
        }
    }

    /// Asks the system to take a non-secure keyguard down.
    ///
    /// Only meaningful where there is no PIN or pattern; a secure keyguard
    /// stays up and the call is answered over it, which is the platform's
    /// decision to make rather than this one's.
    private void dismissKeyguard() {
        // TESTED with instanceof, and OUTSIDE the try. The cast used to sit
        // inside it, where catch (Exception) swallows ClassCastException --
        // and ParparVM does not throw one for a failed cast, so a handler
        // written to catch it never runs. This class is Android-only today
        // and the rule is about iOS, but the check is a ratchet over the
        // whole port for a reason: the next reader copies the shape, not the
        // platform it was written for.
        Object service =
                getSystemService(android.content.Context.KEYGUARD_SERVICE);
        if (!(service instanceof android.app.KeyguardManager)) {
            return;
        }
        android.app.KeyguardManager km = (android.app.KeyguardManager) service;
        try {
            java.lang.reflect.Method m = km.getClass().getMethod(
                    "requestDismissKeyguard", Activity.class,
                    Class.forName("android.app.KeyguardManager$"
                            + "KeyguardDismissCallback"));
            m.invoke(km, this, null);
        } catch (Exception unavailable) {
            // API 26+, and best effort even there: a call that rings over a
            // keyguard is still answerable.
        }
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView who = new TextView(this);
        who.setText(callerLabel());
        who.setTextColor(Color.WHITE);
        who.setTextSize(28);
        who.setGravity(Gravity.CENTER);
        root.addView(who);

        TextView what = new TextView(this);
        what.setText(CN1CallNotifications.label(this,
                "android.call.incomingTitle", "Incoming call"));
        what.setTextColor(Color.LTGRAY);
        what.setTextSize(16);
        what.setGravity(Gravity.CENTER);
        what.setPadding(0, pad / 2, 0, pad * 2);
        root.addView(what);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        Button decline = new Button(this);
        decline.setText(CN1CallNotifications.label(this,
                "android.call.declineLabel", "Decline"));
        decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CN1CallNotifications.decline(callId);
                finish();
            }
        });
        buttons.addView(decline);

        Button answer = new Button(this);
        answer.setText(CN1CallNotifications.label(this,
                "android.call.answerLabel", "Answer"));
        answer.setPadding(pad, 0, 0, 0);
        answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CN1CallNotifications.answer(callId);
                finish();
            }
        });
        buttons.addView(answer);
        root.addView(buttons);
        return root;
    }

    private String callerLabel() {
        CN1Connection c = CN1ConnectionService.find(callId);
        String name = c == null ? null : c.callerLabel();
        if (name != null && name.length() > 0) {
            return name;
        }
        return CN1CallNotifications.label(this, "android.call.incomingTitle",
                "Incoming call");
    }

    /// Closes this screen when the call it rings for is over.
    ///
    /// Called by the connection rather than polled: a call answered from the
    /// notification, from a car head unit or by the far end hanging up leaves
    /// this window in front of the lock screen with nothing behind it.
    static void dismissFor(String callId) {
        CN1IncomingCallActivity a = current;
        if (a != null && callId != null && callId.equals(a.callId)) {
            a.finish();
        }
    }

    /// The instance on screen, if any. Written on the main thread by the
    /// lifecycle callbacks and read by dismissFor, which the connection
    /// calls from wherever Telecom answered it.
    private static volatile CN1IncomingCallActivity current;

    @Override
    protected void onStart() {
        super.onStart();
        current = this;
    }

    @Override
    protected void onDestroy() {
        if (current == this) {
            current = null;
        }
        super.onDestroy();
    }
}
