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
package com.codename1.wearable;

/// Receives the answer to a message that asked for one.
///
/// Exactly one of the two methods is called, on the EDT. A reply is not guaranteed: the peer may be
/// asleep, out of range, or running a version of your app that does not know the path you sent.
public interface WearableReplyHandler {

    /// Called with the peer's answer.
    ///
    /// #### Parameters
    ///
    /// - `reply`: the peer's response, on the same path as the request
    void replyReceived(WearableMessage reply);

    /// Called when no answer could be obtained.
    ///
    /// #### Parameters
    ///
    /// - `message`: a description of what went wrong, suitable for a log rather than a UI
    void replyFailed(String message);
}
