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
package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;

/// Drives a full-screen native modal that only supports portrait -- the shape
/// of the iOS camera (`UIImagePickerController`) -- so a test can rotate the
/// device while it covers the Codename One view and then dismiss it
/// (issue #6080). Every call returns at once; the work runs on the main
/// thread, and the getters answer the state captured by the previous call.
public interface NativeModalRotationNative extends NativeInterface {
    /// Presents the portrait-only modal over the Codename One view.
    boolean presentPortraitOnlyModal();

    /// Dismisses it, animated, as the camera dismisses itself.
    boolean dismissModal();

    /// True while the modal is presented.
    boolean isModalPresented();

    /// The Codename One view's current width in pixels, as UIKit sees it.
    int getViewPixelWidth();

    /// The Codename One view's current height in pixels, as UIKit sees it.
    int getViewPixelHeight();
}
