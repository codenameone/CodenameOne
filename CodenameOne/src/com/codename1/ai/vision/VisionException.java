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
package com.codename1.ai.vision;

/// Failure reported by an on-device vision backend.
///
/// The code separates "this device will never do this" from "this attempt
/// failed", which usually calls for different UI:
///
/// ```java
/// recognizer.process(image)
///     .ready(result -> textArea.setText(result.getText()))
///     .except(error -> {
///         if (error instanceof VisionException
///                 && ((VisionException) error).getCode()
///                         == VisionException.UNSUPPORTED) {
///             // Permanent: hide the feature rather than offering a retry.
///             scanButton.setVisible(false);
///             return;
///         }
///         ToastBar.showErrorMessage("Could not read that image");
///         Log.e(error);
///     });
/// ```
///
/// Prefer {@code isSupported()} on the analyzer to reaching this state at all;
/// {@link #UNSUPPORTED} is the answer to a question that could have been asked
/// before the screen was shown.
public class VisionException extends RuntimeException {
    public static final int UNSUPPORTED = 1;
    public static final int INVALID_IMAGE = 2;
    public static final int MODEL_UNAVAILABLE = 3;
    public static final int CANCELLED = 4;
    public static final int BACKEND_ERROR = 5;

    private final int code;

    /// Creates a classified vision failure.
    /// @param code portable failure code defined by this class
    /// @param message user-readable failure description
    public VisionException(int code, String message) {
        super(message);
        this.code = code;
    }

    /// Creates a classified vision failure with its native or port cause.
    /// @param code portable failure code defined by this class
    /// @param message user-readable failure description
    /// @param cause originating detector or image-conversion error
    public VisionException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /// @return one of this class's portable failure-code constants
    public int getCode() {
        return code;
    }
}
