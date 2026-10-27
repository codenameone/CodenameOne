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
package com.codename1.media;

/// A content protection scheme a [StreamRequest] can ask the platform player to
/// honour. Not every platform plays every scheme; ask
/// [MediaManager#isDrmSupported(DrmScheme)] first and pick the strongest scheme the
/// device answers yes to.
///
/// A streaming service usually packages each title once with CENC and serves it
/// with whichever scheme the requesting device supports: FairPlay to Apple
/// devices, Widevine to Android, and HLS AES-128 where neither is available.
public enum DrmScheme {
    /// HLS segment encryption with the key fetched from the `URI` of the
    /// playlist's `#EXT-X-KEY` tag. Every native player that plays HLS decrypts it,
    /// so it works on the most devices. The key reaches the client in the clear
    /// (over TLS), which makes it a deterrent rather than hardware-backed
    /// protection: guard the key URL with a short-lived, per-session token.
    AES_128,

    /// The W3C ClearKey key system: CENC-encrypted media with keys delivered as a
    /// JSON Web Key set. The same packaging as Widevine without a license vendor,
    /// which makes it the scheme to develop and test a license server against.
    CLEARKEY,

    /// Google Widevine, the protection Android devices implement, with
    /// hardware-backed keys (L1) on most phones.
    WIDEVINE,

    /// Apple FairPlay Streaming, the protection iOS, iPadOS and tvOS implement.
    /// Needs the application certificate from Apple's FairPlay program, set with
    /// [DrmConfig#setCertificateUrl(String)].
    FAIRPLAY
}
