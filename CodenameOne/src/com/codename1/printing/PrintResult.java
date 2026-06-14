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
package com.codename1.printing;

/// Outcome of a print request initiated through [Printer] or
/// [com.codename1.ui.Display#print].
///
/// Status semantics:
///
/// - `COMPLETED`: the print flow finished and the job was handed to the
///   printing system. Platforms generally can't observe the physical
///   printer, so this means "queued/sent", not "paper came out".
/// - `CANCELLED`: the user dismissed the print dialog without printing.
/// - `FAILED`: the job could not be started or the printing system
///   reported an error. `getError()` may carry a short, platform-supplied
///   description.
///
/// Some platforms cannot distinguish cancellation from completion once the
/// native dialog takes over; those report `COMPLETED` on a best-effort
/// basis.
///
/// Instances are immutable. Construct through the static factories.
public final class PrintResult {

    /// The print job was handed to the platform printing system.
    public static final int STATUS_COMPLETED = 1;

    /// User dismissed/cancelled the print dialog.
    public static final int STATUS_CANCELLED = 2;

    /// The print job could not be completed.
    public static final int STATUS_FAILED = 3;

    private final int status;
    private final String error;

    private PrintResult(int status, String error) {
        this.status = status;
        this.error = error;
    }

    /// Build a `COMPLETED` result.
    public static PrintResult completed() {
        return new PrintResult(STATUS_COMPLETED, null);
    }

    /// Build a `CANCELLED` result.
    public static PrintResult cancelled() {
        return new PrintResult(STATUS_CANCELLED, null);
    }

    /// Build a `FAILED` result with an optional platform message.
    public static PrintResult failed(String message) {
        return new PrintResult(STATUS_FAILED, message);
    }

    /// Numeric status: one of [#STATUS_COMPLETED], [#STATUS_CANCELLED], [#STATUS_FAILED].
    public int getStatus() {
        return status;
    }

    /// True iff status == [#STATUS_COMPLETED].
    public boolean isCompleted() {
        return status == STATUS_COMPLETED;
    }

    /// True iff status == [#STATUS_CANCELLED].
    public boolean isCancelled() {
        return status == STATUS_CANCELLED;
    }

    /// True iff status == [#STATUS_FAILED].
    public boolean isFailed() {
        return status == STATUS_FAILED;
    }

    /// Platform-supplied message when [#isFailed] is true, otherwise null.
    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        switch (status) {
            case STATUS_COMPLETED:
                return "PrintResult{COMPLETED}";
            case STATUS_CANCELLED:
                return "PrintResult{CANCELLED}";
            case STATUS_FAILED:
                return "PrintResult{FAILED " + error + "}";
            default:
                return "PrintResult{?}";
        }
    }
}
