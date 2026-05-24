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
package com.codename1.share;

/// Outcome of a share request initiated through [com.codename1.ui.Display#share]
/// or [com.codename1.components.ShareButton].
///
/// Status semantics:
///
/// - `SHARED_TO`: the user picked a target and the system accepted the
///   share. `getPackageName()` returns the chosen target's identifier:
///   the Android package name (e.g. `com.whatsapp`) or, on iOS,
///   the `UIActivityType` (e.g. `com.apple.UIKit.activity.PostToTwitter`).
/// - `DISMISSED`: the user cancelled the share sheet without picking a
///   target.
/// - `FAILED`: the share could not be completed. `getError()` may carry a
///   short, platform-supplied description (no stack traces).
///
/// Instances are immutable. Construct through the static factories.
public final class ShareResult {

    /// Status of a [ShareResult].
    public static final int STATUS_SHARED_TO = 1;

    /// User dismissed/cancelled the share sheet.
    public static final int STATUS_DISMISSED = 2;

    /// Share could not be completed.
    public static final int STATUS_FAILED = 3;

    private final int status;
    private final String packageName;
    private final String error;

    private ShareResult(int status, String packageName, String error) {
        this.status = status;
        this.packageName = packageName;
        this.error = error;
    }

    /// Build a `SHARED_TO` result.
    ///
    /// `packageName` is the platform-specific identifier of the chosen
    /// target (Android package name or iOS `UIActivityType`). It may be
    /// `null` when the platform does not expose the selection (older
    /// Android versions, web share API).
    public static ShareResult sharedTo(String packageName) {
        return new ShareResult(STATUS_SHARED_TO, packageName, null);
    }

    /// Build a `DISMISSED` result.
    public static ShareResult dismissed() {
        return new ShareResult(STATUS_DISMISSED, null, null);
    }

    /// Build a `FAILED` result with an optional platform message.
    public static ShareResult failed(String message) {
        return new ShareResult(STATUS_FAILED, null, message);
    }

    /// Numeric status: one of [#STATUS_SHARED_TO], [#STATUS_DISMISSED], [#STATUS_FAILED].
    public int getStatus() {
        return status;
    }

    /// True iff status == [#STATUS_SHARED_TO].
    public boolean isSharedTo() {
        return status == STATUS_SHARED_TO;
    }

    /// True iff status == [#STATUS_DISMISSED].
    public boolean isDismissed() {
        return status == STATUS_DISMISSED;
    }

    /// True iff status == [#STATUS_FAILED].
    public boolean isFailed() {
        return status == STATUS_FAILED;
    }

    /// Chosen target identifier when the user picked a destination, otherwise null.
    public String getPackageName() {
        return packageName;
    }

    /// Platform-supplied message when [#isFailed] is true, otherwise null.
    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        switch (status) {
            case STATUS_SHARED_TO:
                return "ShareResult{SHARED_TO " + packageName + "}";
            case STATUS_DISMISSED:
                return "ShareResult{DISMISSED}";
            case STATUS_FAILED:
                return "ShareResult{FAILED " + error + "}";
            default:
                return "ShareResult{?}";
        }
    }
}
