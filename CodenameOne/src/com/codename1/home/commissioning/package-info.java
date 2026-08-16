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

/// Adding a new Matter accessory to the user's home.
///
/// Start from
/// [com.codename1.home.SmartHome#getCommissioner()], which never returns
/// `null` and answers
/// [com.codename1.home.commissioning.Commissioner#isSupported()] `false`
/// where there is nothing behind it.
///
/// #### Its own package on purpose
///
/// The build server decides what native machinery an app gets by scanning its
/// bytecode for package prefixes, and commissioning is much more expensive
/// than the rest of this API: on iOS it needs the `MatterSupport` framework,
/// a `com.apple.developer.matter.allow-setup-payload` entitlement, an app
/// group and a whole generated app-extension target, and on Android it adds
/// the Play services home dependency and Bluetooth and local-network
/// permissions.
///
/// An app that reads its lights and never adds an accessory should get none of
/// that. Because the scanner matches on a package prefix and has no way to
/// express an exclusion, the only way to make that separation possible is for
/// the package boundary to *be* the permission boundary -- so commissioning
/// lives here rather than next to [com.codename1.home.SmartHome].
///
/// #### What commissioning does not promise
///
/// Adding an accessory puts it in the user's ecosystem. Whether your app can
/// then *control* it is a different question, and on Android with Play
/// services alone the answer is no. Check
/// [com.codename1.home.commissioning.CommissioningResult#wasCommissionedToThisApp()]
/// rather than assuming the returned accessory id is usable.
package com.codename1.home.commissioning;
