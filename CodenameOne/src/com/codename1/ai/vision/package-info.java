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
/// Vendor-neutral on-device vision APIs for still images and live camera frames.
///
/// <p>The automatic backend uses Apple Vision/Core Image on iOS and Mac
/// Catalyst and ML Kit on Android. Unsupported ports report that through
/// {@code isSupported()}. Optional backends are selected with
/// {@link com.codename1.ai.vision.VisionBackends}.</p>
///
/// <p>Each analyzer is a separate build-time feature. Referencing one analyzer
/// causes the builder to retain only its platform adapter and native
/// dependency. {@link com.codename1.ai.vision.VisionPipeline} safely copies
/// callback-owned camera frames and drops stale pending frames under load.</p>
package com.codename1.ai.vision;
