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
package com.codename1.continuity.spi;

import java.util.Map;

/// The framework's inbound seam, handed to every `ContinuityBridge` during initialization. Ports
/// call it when the platform delivers something; they never dispatch to application code
/// themselves.
///
/// Both methods may be called from any thread, including before the application has a form on
/// screen: on Apple platforms a continuation can cold-launch the app, and the operating system
/// hands it over while the virtual machine is still starting. The framework holds such a delivery
/// until there is somewhere to show it, so implementations must not try to do that themselves.
public interface ContinuityCallback {
    /// A continuation arrived from one of the user's other devices.
    ///
    /// #### Parameters
    ///
    /// - `activityType`: the reverse-DNS type it arrived under
    /// - `userInfo`: the payload, as strings, numbers, booleans, lists and maps of those
    ///
    /// #### Returns
    ///
    /// true when the application claimed it, so the port can answer the platform honestly rather
    /// than swallowing activities this app never published
    boolean continuationReceived(String activityType, Map<String, Object> userInfo);

    /// The synced store changed underneath the app, because another of the user's devices wrote
    /// to it. Carries no values: the framework re-reads what it needs.
    void syncedStoreChanged();
}
