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
package com.codename1.health;

/// Receives health-store changes even after the app's process has been
/// killed and relaunched. Registered by class with
/// [HealthStore#subscribe(SubscriptionRequest,Class)], mirroring
/// `LocationManager.setBackgroundLocationListener(Class)`.
///
/// #### Requirements on the implementing class
///
/// It must be a **public top-level class with a public no-argument
/// constructor**. The framework instantiates it fresh for every delivery,
/// including after a cold relaunch, so do not keep state in fields and do
/// not assume any of your app's initialization has run.
///
/// #### What you may do inside
///
/// You are called on the EDT, but on iOS there may be no visible UI at all
/// -- the OS woke the app purely to hand you data. Check
/// `Display.getInstance().isMinimized()` before touching anything visual,
/// and keep the work small: [HealthChangeBatch#getDeadlineMillis()] is
/// around five seconds on a background relaunch. Accumulate into
/// `Preferences` or `Storage` and do the real work on next foreground.
///
/// ```java
/// public class StepWatcher implements HealthBackgroundListener {
///     public void healthDataChanged(HealthChangeBatch batch) {
///         if (batch.isResyncRequired()) {
///             Preferences.set("stepsWatermark", 0L);
///             return;
///         }
///         long added = 0;
///         for (HealthSample s : batch.getAdded()) {
///             added += (long) ((QuantitySample) s).getValue(HealthUnit.COUNT);
///         }
///         Preferences.set("pendingSteps",
///                 Preferences.get("pendingSteps", 0L) + added);
///     }
/// }
/// ```
///
/// #### Keep the class reachable
///
/// The class is instantiated by name, so nothing in your code refers to it
/// directly and a dead-code eliminator may strip it. Reference it
/// somewhere reachable -- the `subscribe` call itself is enough -- exactly
/// as background location requires.
public interface HealthBackgroundListener {

    /// Called with the changes accumulated since the last delivery.
    void healthDataChanged(HealthChangeBatch batch);
}
