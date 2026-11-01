/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.imagepicker;

import com.codename1.flutter.plugins.crossfile.XFile;

import dart.async.Future;

/**
 * image_picker_platform_interface's {@code ImagePickerPlatform}. There is one
 * implementation on every Codename One target, so {@code instance} starts as an
 * {@link ImagePickerAndroid} and an app that installs its own replaces it, as the plugin
 * allows. The pick goes through {@link ImagePicker}.
 */
public abstract class ImagePickerPlatform {

    /** {@code ImagePickerPlatform.instance}, read and written as a static field. */
    public static ImagePickerPlatform instance = new ImagePickerAndroid();

    protected ImagePickerPlatform() {
    }

    public Future<XFile> getImageFromSource(ImageSource source, Object options) {
        return new ImagePicker().pickImage(source, null, null, null, null, null);
    }

    public Future<Object> retrieveLostData() {
        return new ImagePicker().retrieveLostData();
    }
}
