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
package com.codename1.impl.home;

import com.codename1.home.commissioning.CommissioningRequest;
import com.codename1.home.commissioning.CommissioningResult;
import com.codename1.util.AsyncResource;

/// How `com.codename1.home.commissioning.Commissioner` reaches the machinery
/// in `com.codename1.home.SmartHome` without either package having to make
/// commissioning part of its public surface.
///
/// The two live in different packages **on purpose** -- the build server
/// decides what native machinery an app gets by scanning for package prefixes,
/// and commissioning costs an entire extra Xcode target -- so they cannot
/// simply share a package-private method. This is the seam.
///
/// `SmartHome` implements it privately; nothing else should.
public interface CommissioningGateway {

    /// The ordinal of the
    /// `com.codename1.home.commissioning.CommissioningStyle` this backend
    /// uses.
    ///
    /// #### Returns
    ///
    /// the style ordinal
    int getCommissioningStyle();

    /// Runs a commissioning flow.
    ///
    /// #### Parameters
    ///
    /// - `request`: what to add and where
    ///
    /// #### Returns
    ///
    /// the result, delivered on the EDT
    AsyncResource<CommissioningResult> commission(
            CommissioningRequest request);

    /// Opens the platform's ecosystem app.
    ///
    /// #### Returns
    ///
    /// `true` when the app was opened
    boolean openEcosystemApp();
}
