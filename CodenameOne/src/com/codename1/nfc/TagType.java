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
package com.codename1.nfc;

/// Tag technologies that a discovered [Tag] may support. A single tag
/// typically reports several entries -- e.g. a MIFARE Classic 1K tag returns
/// `[NFC_A, MIFARE_CLASSIC, NDEF]` -- so callers use [Tag#supports(TagType)]
/// to decide which API to call.
///
/// Not every platform exposes every technology:
///
/// - **Android** -- exposes the full set via `android.nfc.tech.*`.
/// - **iOS** -- Core NFC exposes only `NDEF`, `ISO_DEP` (ISO 14443-4 / ISO
///   7816), `NFC_F` (FeliCa), and `MIFARE_ULTRALIGHT` (as a subset of
///   `NFCMiFareTag`). MIFARE Classic is intentionally not supported by
///   Apple and reports as `NFC_A` only.
/// - **JavaSE simulator** -- all values are emulated; the
///   Simulate -> NFC menu lets you set the tech list per virtual tag.
public enum TagType {
    /// Tag carries an NDEF message that can be read by [Tag#readNdef()] and
    /// (if writable) updated by [Tag#writeNdef(NdefMessage)]. The vast
    /// majority of consumer-facing NFC tags include this technology.
    NDEF,

    /// ISO 14443-4 / ISO 7816-4 -- contact-less smart cards, EMV payment
    /// cards, ePassports, government ID. Use [IsoDep#transceive(byte[])] to
    /// send APDUs. Available on Android via `IsoDep` and on iOS via
    /// `NFCTagReaderSession` with `NFCISO7816Tag`.
    ISO_DEP,

    /// NXP MIFARE Classic 1K/4K. Block-level read/write with key A/B
    /// authentication via [MifareClassic]. **Android-only** -- iOS
    /// intentionally does not expose this technology.
    MIFARE_CLASSIC,

    /// NXP MIFARE Ultralight / Ultralight C / NTAG (NTAG213/215/216).
    /// Page-level read/write via [MifareUltralight]. Supported on both
    /// Android (`MifareUltralight`) and iOS (`NFCMiFareTag`).
    MIFARE_ULTRALIGHT,

    /// NFC Forum Type 2 (low-level NFC-A / ISO 14443-3A). Use [NfcA] for raw
    /// transceive.
    NFC_A,

    /// NFC Forum Type 4B (ISO 14443-3B). **Android-only**.
    NFC_B,

    /// FeliCa (JIS X 6319-4) -- Japanese transit / payment cards. Use
    /// [NfcF] for raw transceive. Supported on Android and on iOS 13+ via
    /// `NFCFeliCaTag`. App must declare its system codes in the iOS plist
    /// `com.apple.developer.nfc.readersession.felica.systemcodes`.
    NFC_F,

    /// ISO 15693 -- vicinity cards used in libraries, ski-lift passes, blood
    /// bags. **Android-only**.
    NFC_V
}
