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

/// The platform seam of the external surfaces framework. Application code never touches this
/// package -- ports implement `SurfaceBridge` and return it from
/// `CodenameOneImplementation.getSurfaceBridge()`.
///
/// #### Port implementer notes
///
/// - **Persist everything.** Surfaces render while the app process is dead. Published timeline
///   JSON and image blobs must be written where the platform renderer can reach them without the
///   VM: the app group container on iOS (`timeline.json` plus `<name>.png` files under
///   `cn1surfaces/<kindId>/`), the files directory on Android, storage on desktop. Writes should
///   be atomic (write-rename) because the renderer may read concurrently.
/// - **Actions flow through the framework.** Decode your platform payload (deep link, intent
///   extras, window click) and call `com.codename1.surfaces.Surfaces.dispatchAction(source,
///   actionId, params)`; it handles EDT marshaling and cold-start queuing. The canonical deep
///   link form on URL-based platforms is
///   `cn1surface://a?src=<source>&id=<actionId>&p=<url-encoded JSON params>`.
/// - **State-only updates.** `updateLiveActivity` ships just a state map; re-interpolate the
///   `${key}` placeholders of the descriptor persisted at start time and re-render locally.
/// - **Degrade honestly.** Return false from the `is...Supported` queries rather than presenting
///   a broken surface; the public API turns into a documented no-op.
package com.codename1.surfaces.spi;
