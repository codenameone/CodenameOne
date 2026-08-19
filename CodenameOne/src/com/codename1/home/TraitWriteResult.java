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
package com.codename1.home;

/// What happened to one [TraitWrite].
///
/// #### A batch write partly succeeding is normal, not exceptional
///
/// "Turn off every light" against a home with a dead bulb in it is a request
/// that mostly worked. Failing the whole operation would be wrong -- the other
/// eight lights are off, and telling the caller nothing happened would have
/// them retry and flicker the house. So [SmartHome#write(java.util.List)]
/// resolves successfully with one of these per write, and the caller decides
/// what a partial result means for them.
///
/// The operation's own `AsyncResource` fails only when the request never
/// reached the platform at all -- unauthorized, not configured, malformed.
public final class TraitWriteResult {

    private final TraitWrite write;
    private final boolean applied;
    private final HomeError error;
    private final String errorMessage;

    private TraitWriteResult(TraitWrite write, boolean applied,
            HomeError error, String errorMessage) {
        this.write = write;
        this.applied = applied;
        this.error = error;
        this.errorMessage = errorMessage;
    }

    /// A write the accessory accepted.
    ///
    /// #### Parameters
    ///
    /// - `write`: the write that succeeded
    ///
    /// #### Returns
    ///
    /// the result
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `write` is `null`
    public static TraitWriteResult applied(TraitWrite write) {
        if (write == null) {
            throw new IllegalArgumentException("write is required");
        }
        return new TraitWriteResult(write, true, null, null);
    }

    /// A write the accessory did not accept.
    ///
    /// #### Parameters
    ///
    /// - `write`: the write that failed
    ///
    /// - `error`: why; `null` becomes [HomeError#UNKNOWN]
    ///
    /// - `message`: the platform's own text, or `null`
    ///
    /// #### Returns
    ///
    /// the result
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `write` is `null`
    public static TraitWriteResult failed(TraitWrite write, HomeError error,
            String message) {
        if (write == null) {
            throw new IllegalArgumentException("write is required");
        }
        return new TraitWriteResult(write, false,
                error == null ? HomeError.UNKNOWN : error, message);
    }

    /// The write this describes.
    ///
    /// #### Returns
    ///
    /// the write, never `null`
    public TraitWrite getWrite() {
        return write;
    }

    /// Whether the accessory accepted the write.
    ///
    /// Note what this does **not** claim: that the accessory has finished
    /// doing it. A covering that accepted "go to 40 percent" will be moving
    /// for some seconds afterwards, and a fan ramps. Watch
    /// [Trait#COVERING_MOTION] or subscribe to the trait if you need to know
    /// when it settles.
    ///
    /// #### Returns
    ///
    /// `true` when the write was accepted
    public boolean isApplied() {
        return applied;
    }

    /// Why the write failed.
    ///
    /// #### Returns
    ///
    /// the error, or `null` when it succeeded
    public HomeError getError() {
        return error;
    }

    /// The platform's own text for a failure.
    ///
    /// #### Returns
    ///
    /// the message, or `null`
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return write + (applied ? " ok" : " !" + error.name());
    }
}
