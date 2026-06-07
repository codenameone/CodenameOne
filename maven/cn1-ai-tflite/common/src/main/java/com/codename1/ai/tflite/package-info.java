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

/// TensorFlow Lite on-device inference.
///
/// Loads a `.tflite` model and runs inference against `float[]` inputs.
/// Bridges to `TensorFlowLiteObjC` on iOS and `org.tensorflow:tensorflow-lite`
/// on Android.
///
/// The single public class in this package is [Interpreter], which exposes
/// the feature via static methods returning
/// [com.codename1.util.AsyncResource]. A package-private
/// `NativeInterpreter` interface holds the platform contract; iOS Obj-C and
/// Android Java implementations live in `nativeios.zip` / `nativeand.zip`
/// inside the cn1lib bundle. References to `Interpreter.*` are recognised
/// by the Codename One build server's `AiDependencyTable`, which
/// auto-injects the matching CocoaPod / Swift Package / Android Gradle
/// dep / `Info.plist` usage strings / Android permissions on every
/// build -- no manual build hints required.
package com.codename1.ai.tflite;
