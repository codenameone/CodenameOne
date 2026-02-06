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
package com.codename1.background;

import com.codename1.util.Callback;

/// An interface that can be implemented by an app's main class to support background fetch.
///
/// What is Background Fetch?
///
/// Background fetch is a mechanism whereby an application is granted permission by the operating system
/// to update its data periodically.  At times of the native platform's choosing, an app that supports background
/// fetch will be started up (in the background), and its `com.codename1.util.Callback)` method will be called.
///
/// Note: Since the app will be launched directly to the background, you cannot assume that the `start()` method has been
/// run prior to the `com.codename1.util.Callback)` method being called.
///
/// How to Implement Background Fetch
///
/// Apps that wish to implement background fetch must implement the `BackgroundFetch` interface
/// in their main class.  On iOS, you also need to include `fetch` in the list of background
/// modes (i.e. include "fetch" in the `ios.background_modes` build hint.)
///
/// In addition to implementing the `BackgroundFetch` interface, apps must explicitly set the background fetch interval
/// calling `com.codename1.ui.Display#setPreferredBackgroundFetchInterval(int)` at some point, usually in the `start()` or `init()` method.
///
/// Platform Support
///
/// Currently background fetch is supported on iOS, Android, and in the Simulator (simulated using timers when the app is paused).  You should
/// use the `com.codename1.ui.Display#isBackgroundFetchSupported()` method to find out if the current platform supports it.
///
/// Examples
///
/// ```java
/// package com.codename1.test.bgfetch;
///
/// import com.codename1.background.BackgroundFetch;
/// import com.codename1.components.SpanLabel;
/// import com.codename1.ui.Display;
/// import com.codename1.ui.Form;
/// import com.codename1.ui.Dialog;
/// import com.codename1.ui.Label;
/// import com.codename1.ui.plaf.UIManager;
/// import com.codename1.ui.util.Resources;
/// import com.codename1.io.Log;
/// import com.codename1.ui.Toolbar;
/// import com.codename1.util.Callback;
/// import java.io.IOException;
/// import java.util.ArrayList;
/// import java.util.Map;
/// import com.codename1.io.ConnectionRequest;
/// import com.codename1.io.NetworkManager;
/// import com.codename1.io.services.RSSService;
/// import java.util.List;
/// import com.codename1.ui.layouts.BoxLayout;
/// import com.codename1.io.services.RSSService;
/// import com.codename1.ui.Container;
///
/// /**
///  * A simple demo showing the use of the Background Fetch API.  This demo will load
///  * data from the Slashdot RSS feed while it is in the background.
///  *
///  * To test it out, put the app into the background (or select Pause App in the simulator)
///  * and wait 10 seconds.  Then open the app again. You should see that the data is loaded.
///  */
/// public class BackgroundFetchTest implements BackgroundFetch {
///
///     private Form current;
///     private Resources theme;
///     List records;
///
///     // Container to hold the list of records.
///     Container recordsContainer;
///
///     public void init(Object context) {
///         theme = UIManager.initFirstTheme("/theme");
///
///         // Enable Toolbar on all Forms by default
///         Toolbar.setGlobalToolbar(true);
///
///         // Pro only feature, uncomment if you have a pro subscription
///         // Log.bindCrashProtection(true);
///     }
///
///     public void start() {
///         if(current != null){
///             // Make sure we update the records as we are coming in from the
///             // background.
///             updateRecords();
///             current.show();
///             return;
///         }
///         Display d = Display.getInstance();
///
///         // This call is necessary to initialize background fetch
///         d.setPreferredBackgroundFetchInterval(10);
///
///         Form hi = new Form("Background Fetch Demo");
///         hi.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
///
///         Label supported = new Label();
///         if (d.isBackgroundFetchSupported()){
///             supported.setText("Background Fetch IS Supported");
///         } else {
///             supported.setText("Background Fetch is NOT Supported");
///         }
///
///         hi.addComponent(new Label("Records:"));
///         recordsContainer = new Container(new BoxLayout(BoxLayout.Y_AXIS));
///         //recordsContainer.setScrollableY(true);
///         hi.addComponent(recordsContainer);
///
///
///
///         hi.addComponent(supported);
///         updateRecords();
///         hi.show();
///     }
///
///     /**
///      * Update the UI with the records that are currently loaded.
///      */
///     private void updateRecords() {
///         recordsContainer.removeAll();
///         if (records != null) {
///             for (Map m : records) {
///                 recordsContainer.addComponent(new SpanLabel((String)m.get("title")));
///             }
///         } else {
///             recordsContainer.addComponent(new SpanLabel("Put the app in the background, wait 10 seconds, then open it again.  The app should background fetch some data from the Slashdot RSS feed and show it here."));
///         }
///         if (Display.getInstance().getCurrent() != null) {
///             Display.getInstance().getCurrent().revalidate();
///         }
///     }
///
///     public void stop() {
///         current = Display.getInstance().getCurrent();
///         if(current instanceof Dialog) {
///             ((Dialog)current).dispose();
///             current = Display.getInstance().getCurrent();
///         }
///     }
///
///     public void destroy() {
///     }
///
///     /**
///      * This method will be called in the background by the platform.  It will
///      * load the RSS feed.  Note:  This only runs when the app is in the background.
///      * @param deadline
///      * @param onComplete
///      */
/// @Override
///     public void performBackgroundFetch(long deadline, Callback onComplete) {
///         RSSService rss = new RSSService("http://rss.slashdot.org/Slashdot/slashdotMain");
///         NetworkManager.getInstance().addToQueueAndWait(rss);
///         records = rss.getResults();
///         System.out.println(records);
///         onComplete.onSucess(Boolean.TRUE);
///
///     }
///
/// }
/// ```
/// @author shannah
public interface BackgroundFetch {

    /// A callback that may be periodically called by the platform to allow the app to
    /// fetch data in the background.
    ///
    /// #### Parameters
    ///
    /// - `deadline`: The deadline (milliseconds since epoch) by which the fetch should be completed.  If not completed by this time, the app may be killed by the OS.  On iOS, there is a limit of 30 seconds to perform background fetches.
    ///
    /// - `onComplete`: Callback that **MUST** be called when the fetch is complete.  If it is not called, some platforms (e.g. iOS) may refuse to allow the app to perform background fetches in the future.
    ///
    /// #### See also
    ///
    /// - com.codename1.ui.Display.setPreferredBackgroundFetchInterval(int)
    ///
    /// - com.codename1.ui.Display.getPreferredBackgroundFetchInterval()
    ///
    /// - com.codename1.ui.Display.isBackgroundFetchSupported()
    void performBackgroundFetch(long deadline, Callback<Boolean> onComplete); // PMD Fix: UnnecessaryModifier removed

}
