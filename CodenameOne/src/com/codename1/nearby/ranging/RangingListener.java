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
package com.codename1.nearby.ranging;

import com.codename1.nearby.NearbyException;

/// Receives everything a [RangingSession] has to say. Every method is called
/// on the EDT.
///
/// Most apps only care about [#updated]; extend [RangingAdapter] rather than
/// implementing the whole interface.
public interface RangingListener {

    /// A fresh measurement arrived. Expect these several times a second
    /// while the peer is in range, and expect individual fields to drop in
    /// and out -- see [RangingUpdate].
    ///
    /// #### Parameters
    ///
    /// - `update`: the measurement
    void updated(RangingUpdate update);

    /// The peer stopped being ranged. The session stays alive and will
    /// resume delivering updates if the peer comes back, so this is a cue
    /// to gray the UI out rather than to tear it down.
    ///
    /// #### Parameters
    ///
    /// - `reason`: why the peer went away
    void peerRemoved(RangingRemovalReason reason);

    /// The platform paused the session -- typically because the app went to
    /// the background without the entitlement that would let it keep
    /// ranging. No updates arrive until [#resumed] fires.
    void suspended();

    /// A suspended session started running again.
    void resumed();

    /// The session died and cannot be restarted. Any further call on it
    /// fails; prepare a new session if the feature is still wanted.
    ///
    /// #### Parameters
    ///
    /// - `error`: why the session ended
    void invalidated(NearbyException error);
}
