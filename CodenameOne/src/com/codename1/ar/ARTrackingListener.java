/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ar;

/// Observes changes to a session's overall tracking quality. Register through
/// `ARSession#addTrackingListener(ARTrackingListener)`; events are delivered
/// on the EDT.
public interface ARTrackingListener {
    /// Invoked on the EDT when the session's tracking state changes.
    ///
    /// #### Parameters
    ///
    /// - `session`: the session whose state changed
    ///
    /// - `state`: the new tracking state
    ///
    /// - `reason`: why tracking is degraded, or
    ///   `ARTrackingFailureReason#NONE` when it is not
    void trackingStateChanged(ARSession session, ARTrackingState state,
                              ARTrackingFailureReason reason);
}
