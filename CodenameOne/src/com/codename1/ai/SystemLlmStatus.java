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
package com.codename1.ai;

/// What the operating system's own language model can do on this device, for
/// one [SystemLlmFeature]. Returned by [SystemLlmClient#getStatus(SystemLlmFeature)].
public enum SystemLlmStatus {
    /// The OS, the device or this build cannot offer the feature. Older OS
    /// versions, devices without enough memory, the simulator and the desktop
    /// all answer this.
    UNSUPPORTED,
    /// The device supports the feature but the user has turned it off, for
    /// example Apple Intelligence disabled in Settings.
    DISABLED,
    /// Supported, but the model is not on the device yet. Call
    /// [SystemLlmClient#download(SystemLlmFeature)].
    DOWNLOADABLE,
    /// The model is being downloaded or prepared by the OS. Ask again later.
    DOWNLOADING,
    /// Ready to use.
    AVAILABLE;

    /// @return `true` for [#AVAILABLE]
    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    /// @return `true` when the feature will work on this device once the model
    /// is present: [#AVAILABLE], [#DOWNLOADABLE] or [#DOWNLOADING]
    public boolean isSupported() {
        return this == AVAILABLE || this == DOWNLOADABLE || this == DOWNLOADING;
    }
}
