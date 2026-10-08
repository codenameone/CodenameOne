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
package com.codename1.androidcompat.runtime;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.List;

/// The process-wide state of an Android application running on Codename
/// One: the generated [AndroidApp], the `Application`, its base context,
/// focus and broadcasts. Installed once by [AndroidLifecycle].
public final class AndroidRuntime {

    private static AndroidRuntime instance;

    private final AndroidApp app;
    private ContextImpl baseContext;
    private Application application;
    private View focused;
    private boolean launched;
    private final List<Object[]> receivers = new ArrayList<Object[]>();

    private AndroidRuntime(AndroidApp app) {
        this.app = app;
    }

    public static AndroidRuntime getInstance() {
        return instance;
    }

    /// Installs the runtime for `app` and creates the `Application`.
    public static AndroidRuntime install(AndroidApp app) {
        if (instance != null) {
            return instance;
        }
        AndroidRuntime rt = new AndroidRuntime(app);
        instance = rt;
        ResourceManager.get().setAppTable(app.getTableName());
        rt.baseContext = new ContextImpl(rt);
        rt.application = app.createApplication();
        rt.application.attach(rt.baseContext);
        rt.baseContext.setApplication(rt.application);
        rt.application.onCreate();
        return rt;
    }

    /// Starts the launcher activity, once.
    public void launch() {
        if (launched) {
            return;
        }
        launched = true;
        AndroidApp.ActivityInfo info = app.launcherActivity();
        if (info == null) {
            throw new IllegalStateException("The manifest declares no activity");
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClass(application, info.type);
        ActivityThread.startActivity(application, intent, -1);
    }

    public boolean isLaunched() {
        return launched;
    }

    public AndroidApp getApp() {
        return app;
    }

    public Application getApplication() {
        return application;
    }

    public Context getBaseContext() {
        return baseContext;
    }

    public String getPackageName() {
        return app.getPackageName();
    }

    public String getVersionName() {
        return app.getVersionName();
    }

    public int getVersionCode() {
        return app.getVersionCode();
    }

    public CharSequence getApplicationLabel() {
        if (app.getAppLabel() != null) {
            return app.getAppLabel();
        }
        if (app.getAppLabelRes() != 0) {
            return baseContext.getResources().getText(app.getAppLabelRes());
        }
        return Display.getInstance().getProperty("AppName", app.getPackageName());
    }

    /// The theme of the top activity, else the application's.
    public Resources.Theme currentTheme() {
        Activity a = ActivityThread.getTopActivity();
        return a != null ? a.getTheme() : baseContext.getTheme();
    }

    public View getFocusedView() {
        return focused;
    }

    public void setFocusedView(View v) {
        focused = v;
    }

    /// Constructs a view by layout tag through the generated factory, or
    /// returns null when the application's layouts never named it.
    public View createView(String tag, Context context, AttributeSet attrs) {
        int index = app.viewIndex(tag);
        return index < 0 ? null : app.createView(index, context, attrs);
    }

    /// Dispatches touches entering a root view: through its activity when it
    /// is an activity's decor, so `Activity.dispatchTouchEvent` overrides work.
    public static void dispatchTouch(ViewGroup root, MotionEvent ev) {
        Activity a = activityOf(root.getContext());
        if (a != null && a.getWindow() != null && a.getWindow().getDecorView() == root) {
            a.dispatchTouchEvent(ev);
        } else {
            root.dispatchTouchEvent(ev);
        }
    }

    public static Activity activityOf(Context c) {
        while (c != null) {
            if (c instanceof Activity) {
                return (Activity) c;
            }
            if (c instanceof ContextWrapper) {
                c = ((ContextWrapper) c).getBaseContext();
            } else {
                return null;
            }
        }
        return null;
    }

    /// `android:onClick`: calls the named method on the view's activity.
    public boolean dispatchOnClick(View v, String method) {
        Object target = activityOf(v.getContext());
        if (target == null) {
            target = v.getContext();
        }
        if (app.dispatchOnClick(target, method, v)) {
            return true;
        }
        throw new IllegalStateException("Could not find method " + method + "(View) in a parent or ancestor Context"
                + " for android:onClick attribute defined on view class " + v.getClass().getName());
    }

    /// A menu item's `android:onClick`: calls the named `name(MenuItem)`
    /// method on the activity the menu was inflated for (the context itself
    /// when it wraps none), and answers what the method returned; a void one
    /// counts as handled. The handler is found by the build, never by
    /// reflection, so a missing one is reported when the item is selected
    /// rather than when the menu is inflated.
    public boolean dispatchMenuOnClick(Context context, String method, android.view.MenuItem item) {
        Object target = activityOf(context);
        if (target == null) {
            target = context;
        }
        int r = app.dispatchMenuOnClick(target, method, item);
        if (r < 0) {
            throw new IllegalStateException("Couldn't resolve menu item onClick handler " + method
                    + "(MenuItem) in class " + (target == null ? "null" : target.getClass().getName()));
        }
        return r != 0;
    }

    // ------------------------------------------------------------ implicit intents

    public boolean canResolve(Intent intent) {
        if (Intent.ACTION_CHOOSER.equals(intent.getAction())) {
            Intent inner = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            return inner != null && canResolve(inner);
        }
        if (intent.getComponentClass() != null) {
            return app.activityInfo(intent.getComponentClass()) != null;
        }
        if (intent.getComponent() != null) {
            return app.activityInfo(intent.getComponent().getClassName()) != null;
        }
        String action = intent.getAction();
        if (action == null) {
            return false;
        }
        if (app.activityForIntent(intent) != null) {
            return true;
        }
        // The same test startActivity() applies, without acting on it, so a
        // `resolveActivity(pm) != null` guard never passes an intent whose
        // start then throws ActivityNotFoundException (a dial without a
        // number, a chooser without an intent).
        return platformHandle(intent, false);
    }

    /// The package Android reports for an implicit intent the runtime hands
    /// to the platform (a URL to the browser, a number to the dialer).
    public static final String PLATFORM_HANDLER_PACKAGE = "android";

    /// What `Intent.resolveActivity` answers: the activity that would run, or
    /// null when [#canResolve(Intent)] says nothing can. An implicit intent
    /// has no component of its own, and answering null for it made the usual
    /// `resolveActivity(pm) != null` guard drop every browser, dialer, mail
    /// and share intent.
    public android.content.ComponentName resolveComponent(Intent intent) {
        if (Intent.ACTION_CHOOSER.equals(intent.getAction())) {
            Intent inner = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            return inner == null ? null : resolveComponent(inner);
        }
        if (!canResolve(intent)) {
            return null;
        }
        if (intent.getComponent() != null) {
            return intent.getComponent();
        }
        if (intent.getComponentClass() != null) {
            return new android.content.ComponentName(app.getPackageName(), intent.getComponentClass().getName());
        }
        AndroidApp.ActivityInfo info = app.activityForIntent(intent);
        if (info != null) {
            return new android.content.ComponentName(app.getPackageName(), info.className);
        }
        return new android.content.ComponentName(PLATFORM_HANDLER_PACKAGE, intent.getAction());
    }

    /// Hands an implicit intent to the platform: URLs to the browser (or the
    /// app registered for them), phone numbers to the dialer, mail and text
    /// to the share sheet. Returns false when nothing can take it.
    public boolean handleImplicitIntent(Intent intent) {
        return platformHandle(intent, true);
    }

    /// Whether the platform takes `intent`, handing it over when `perform`
    /// is true. One method answers both [#canResolve(Intent)] and
    /// [#handleImplicitIntent(Intent)], so the two cannot disagree.
    private boolean platformHandle(Intent intent, boolean perform) {
        String action = intent.getAction();
        Uri data = intent.getData();
        if (Intent.ACTION_CHOOSER.equals(action)) {
            Intent inner = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            return inner != null && platformHandle(inner, perform);
        }
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            if (!perform) {
                return true;
            }
            String scheme = data.getScheme();
            if ("tel".equals(scheme)) {
                Display.getInstance().dial(data.getSchemeSpecificPart());
                return true;
            }
            if ("mailto".equals(scheme)) {
                return sendMail(data, intent);
            }
            Display.getInstance().execute(data.toString());
            return true;
        }
        if ((Intent.ACTION_DIAL.equals(action) || Intent.ACTION_CALL.equals(action)) && data != null) {
            if (perform) {
                Display.getInstance().dial(data.getSchemeSpecificPart());
            }
            return true;
        }
        if (Intent.ACTION_SENDTO.equals(action) && data != null) {
            if ("mailto".equals(data.getScheme())) {
                return !perform || sendMail(data, intent);
            }
            if ("sms".equals(data.getScheme()) || "smsto".equals(data.getScheme())) {
                if (!perform) {
                    return true;
                }
                try {
                    Display.getInstance().sendSMS(data.getSchemeSpecificPart(), intent.getStringExtra("sms_body"), true);
                    return true;
                } catch (java.io.IOException e) {
                    return false;
                }
            }
        }
        if (Intent.ACTION_SEND.equals(action)) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String message = text == null ? (subject == null ? "" : subject) : text;
            Object stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream instanceof Uri) {
                // The file or image being shared. Only file URIs name a file
                // here (there is no content provider); anything else cannot be
                // shared, and saying so beats a share sheet without the file.
                String path = sharedFilePath((Uri) stream);
                if (path == null) {
                    return false;
                }
                if (!perform) {
                    return true;
                }
                String type = intent.getType();
                if (type != null && type.startsWith("image/")) {
                    Display.getInstance().share(message, path, type);
                } else {
                    Display.getInstance().share(path, null, type);
                }
                return true;
            }
            if (perform) {
                Display.getInstance().share(message, null, intent.getType());
            }
            return true;
        }
        return false;
    }

    /// The file system path of a shared stream, or null when it does not name
    /// a file the runtime can open.
    static String sharedFilePath(Uri uri) {
        String s = uri.toString();
        if ("file".equals(uri.getScheme())) {
            String authority = uri.getEncodedAuthority() == null ? "" : uri.getAuthority();
            String path = uri.getPath();
            return "file://" + authority + (path == null ? "" : path);
        }
        if (s.startsWith("/")) {
            return uri.getPath();
        }
        return null;
    }

    private boolean sendMail(Uri data, Intent intent) {
        String to = data.getSchemeSpecificPart();
        int q = to.indexOf('?');
        if (q >= 0) {
            to = to.substring(0, q);
        }
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (subject == null) {
            subject = data.getQueryParameter("subject");
        }
        String body = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (body == null) {
            body = data.getQueryParameter("body");
        }
        com.codename1.messaging.Message m = new com.codename1.messaging.Message(body == null ? "" : body);
        Display.getInstance().sendMessage(new String[] {to}, subject == null ? "" : subject, m);
        return true;
    }

    public Intent getLaunchIntent() {
        AndroidApp.ActivityInfo info = app.launcherActivity();
        if (info == null) {
            return null;
        }
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClass(application, info.type);
        return i;
    }

    /// The last activity finished: Android returns to the home screen.
    public void onLastActivityFinished() {
        if (!launched) {
            // Activities a Codename One application started itself (the
            // runtime was installed, not launched): finishing the last one
            // returns to the form that was showing, as leaving an embedded
            // screen should, instead of ending the host application.
            com.codename1.ui.Form host = hostForm;
            hostForm = null;
            returningTo = host;
            if (host instanceof com.codename1.ui.Dialog) {
                // A dialog is put back without a modal loop of its own:
                // showBack() would start one inside finish() and block it
                // until the dialog closes, under the loop of the show()
                // that is still waiting for it.
                ((com.codename1.ui.Dialog) host).showModeless();
            } else if (host != null) {
                host.showBack();
            }
            return;
        }
        launched = false;
        hostForm = null;
        if (!Display.getInstance().minimizeApplication()) {
            CN.exitApplication();
        }
    }

    private com.codename1.ui.Form hostForm;

    /// The host form the last finished activity went back to. An activity
    /// started while that transition still runs sees the departing activity's
    /// form as current, and returns here too.
    private com.codename1.ui.Form returningTo;

    /// Records the form showing when the first activity starts: in a hosting
    /// Codename One application, the form to return to after the last one.
    public void noteHostForm(com.codename1.ui.Form current) {
        if (launched || hostForm != null) {
            return;
        }
        if (current instanceof ActivityForm) {
            hostForm = returningTo;
        } else {
            hostForm = current;
        }
    }

    // ------------------------------------------------------------ broadcasts

    public void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        receivers.add(new Object[] {receiver, filter});
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        for (int i = receivers.size() - 1; i >= 0; i--) {
            if (receivers.get(i)[0] == receiver) {
                receivers.remove(i);
            }
        }
    }

    /// Delivered later, on the main queue, so the intent is copied now: the
    /// broadcast is the one sent even if the caller reuses or changes the
    /// intent afterwards, as on Android, which hands it to the system
    /// before `sendBroadcast` returns.
    public void sendBroadcast(final Context from, final Intent intent) {
        Intent snapshot = new Intent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            snapshot.replaceExtras(extras.deepCopy());
        }
        CN.callSerially(new Broadcast(new ArrayList<Object[]>(receivers), from, snapshot));
    }

    /// Delivers one broadcast to the receivers registered when it was sent.
    private static final class Broadcast implements Runnable {
        private final List<Object[]> receivers;
        private final Context from;
        private final Intent intent;

        Broadcast(List<Object[]> receivers, Context from, Intent intent) {
            this.receivers = receivers;
            this.from = from;
            this.intent = intent;
        }

        /// A receiver gets the broadcast only when its filter matches whole --
        /// action, data, MIME type and categories -- as an activity's filter
        /// must for an implicit start: a receiver registered for `ACTION_VIEW`
        /// on one scheme takes no other `ACTION_VIEW` broadcast. Unlike an
        /// activity start, no `CATEGORY_DEFAULT` is added.
        @Override
        public void run() {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            for (Object[] r : receivers) {
                if (r[1] instanceof IntentFilter && r[0] instanceof BroadcastReceiver
                        && ((IntentFilter) r[1]).match(action, intent.getType(), intent.getScheme(),
                        intent.getData(), intent.getCategories(), null) >= 0) {
                    ((BroadcastReceiver) r[0]).onReceive(from, intent);
                }
            }
        }
    }
}
