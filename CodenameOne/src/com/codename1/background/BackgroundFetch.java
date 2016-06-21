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

/**
 * An interface that can be implemented by an app's main class to support background fetch.
 * 
 * <h4>What is Background Fetch?</h4>
 * 
 * <p>Background fetch is a mechanism whereby an application is granted permission by the operating system
 * to update its data periodically.  At times of the native platform's choosing, an app that supports background
 * fetch will be started up (in the background), and its {@link #performBackgroundFetch(long, com.codename1.util.Callback) } method will be called.</p>
 * 
 * <p>Note: Since the app will be launched directly to the background, you cannot assume that the {@code start()} method has been 
 * run prior to the {@link #performBackgroundFetch(long, com.codename1.util.Callback) } method being called.</p>
 * 
 * <h4>How to Implement Background Fetch</h4>
 * 
 * <p>Apps that wish to implement background fetch must implement the {@link BackgroundFetch} interface
 * in their main class.  On iOS, you also need to include <code>fetch</code> in the list of background 
 * modes (i.e. include "fetch" in the <code>ios.background_modes</code> build hint.)
 * </p>
 * 
 * <p>In addition to implementing the {@link BackgroundFetch} interface, apps must explicitly set the background fetch interval
 * calling {@link com.codename1.ui.Display#setPreferredBackgroundFetchInterval(int) } at some point, usually in the <code>start()</code> or <code>init()</code> method.</p>
 * 
 * <h4>Platform Support</h4>
 * 
 * <p>Currently background fetch is supported on iOS, Android, and in the Simulator (simulated using timers when the app is paused).  You should
 * use the {@link com.codename1.ui.Display#isBackgroundFetchSupported() } method to find out if the current platform supports it.</p>
 * 
 * <h4>Examples</h4>
 * 
 * <script src="https://gist.github.com/codenameone/ecccc5c6452d055359f12374b2db2644.js"></script>
 * @author shannah
 */
public interface BackgroundFetch {
    
    /**
     * A callback that may be periodically called by the platform to allow the app to 
     * fetch data in the background. 
     * @param deadline The deadline (milliseconds since epoch) by which the fetch should be completed.  If not completed by this time, the app may be killed by the OS.  On iOS, there is a limit of 30 seconds to perform background fetches.
     * @param onComplete Callback that <strong>MUST</strong> be called when the fetch is complete.  If it is not called, some platforms (e.g. iOS) may refuse to allow the app to perform background fetches in the future.
     * @see com.codename1.ui.Display.setPreferredBackgroundFetchInterval(int)
     * @see com.codename1.ui.Display.getPreferredBackgroundFetchInterval()
     * @see com.codename1.ui.Display.isBackgroundFetchSupported()
     */
    public void performBackgroundFetch(long deadline, Callback<Boolean> onComplete);
    
}
