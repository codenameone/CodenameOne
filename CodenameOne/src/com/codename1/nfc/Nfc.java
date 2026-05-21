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

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

/// Entry point for the Codename One NFC API -- read and write NDEF
/// messages, exchange APDUs with smart cards, and host-emulate as a
/// contactless card. Obtain the platform implementation via
/// [#getInstance()]; the returned subclass is owned by the active port.
///
/// #### Quick start: Read an NDEF URI
///
/// ```java
/// Nfc nfc = Nfc.getInstance();
/// if (!nfc.canRead()) {
///     // device has no NFC or it is disabled
///     return;
/// }
/// nfc.readTag(new NfcReadOptions()
///         .setNdefOnly(true)
///         .setAlertMessage("Hold near the poster"))
///    .onResult((tag, err) -> {
///         if (err != null) {
///             return;
///         }
///         tag.readNdef().onResult((msg, e) -> {
///             if (e == null) {
///                 String url = msg.getFirstRecord().getUriPayload();
///                 // launch / display url
///             }
///         });
///    });
/// ```
///
/// #### Quick start: Send an APDU to a smart card
///
/// ```java
/// nfc.readTag(new NfcReadOptions()
///         .setTechFilter(TagType.ISO_DEP)
///         .setIsoSelectAids(myAid))
///    .onResult((tag, err) -> {
///         if (err != null) return;
///         IsoDep iso = tag.getIsoDep();
///         if (iso == null) return;
///         iso.transceive(myCommandApdu).onResult((resp, e) -> {
///             if (ApduResponse.isSuccess(resp)) { ... }
///         });
///    });
/// ```
///
/// #### Quick start: Host card emulation
///
/// ```java
/// class MyService extends HostCardEmulationService {
///     public String[] getAids() { return new String[] { "F0010203040506" }; }
///     public byte[] processCommand(byte[] apdu) {
///         return ApduResponse.withStatus(new byte[] { 'O', 'K' },
///                 ApduResponse.SW_SUCCESS);
///     }
/// }
/// Nfc.getInstance().registerHostCardEmulationService(new MyService());
/// ```
///
/// #### Platform support
///
/// - **Android** -- `NfcAdapter` foreground dispatch / reader-mode +
///   `HostApduService` for HCE. Both manifest entries are auto-injected
///   by the Maven plugin and the build daemon when this class is
///   referenced.
/// - **iOS** -- `Core NFC` (`NFCNDEFReaderSession`,
///   `NFCTagReaderSession`) for reading; `CardSession` (iOS 17.4+, EU
///   only) for HCE. The `NFCReaderUsageDescription` plist entry and the
///   relevant entitlements are auto-injected by IPhoneBuilder.
/// - **JavaSE simulator** -- the Simulate -> NFC menu lets you tap a
///   virtual tag, edit its NDEF payload, and fire APDUs at any registered
///   [HostCardEmulationService].
/// - **All other platforms (desktop deploy, JavaScript, ...)** -- this
///   base class is returned as-is and reports the device as unsupported;
///   every operation completes with [NfcError#NOT_AVAILABLE].
public class Nfc {

    /// Ports construct subclasses. Application code obtains the active
    /// instance via [#getInstance()].
    protected Nfc() {
    }

    /// Returns the platform-specific singleton owned by the current port.
    /// On ports that do not implement NFC this returns a base [Nfc]
    /// instance whose methods report the device as unsupported; calling
    /// code never needs a `null` check or a platform-specific `if`.
    public static Nfc getInstance() {
        Nfc n = Display.getInstance().getNfc();
        return n != null ? n : DEFAULT;
    }

    private static final Nfc DEFAULT = new Nfc();

    /// `true` when NFC hardware is present, regardless of whether it is
    /// currently enabled. Combine with [#canRead()] to drive UI
    /// affordances. Returns `false` on the fallback base class.
    public boolean isSupported() {
        return false;
    }

    /// `true` when NFC is supported AND currently enabled (Android setting
    /// toggle on, iOS Core NFC available). Defaults to `false`.
    public boolean canRead() {
        return false;
    }

    /// `true` when NFC writing is supported on this device. On Android
    /// this mirrors [#canRead()]; on iOS, writing requires iOS 13+ and
    /// the `NFCReaderUsageDescription` plist entry. Defaults to `false`.
    public boolean canWrite() {
        return false;
    }

    /// `true` when this device can act as a host-emulated contactless
    /// card. Android requires `FEATURE_NFC_HOST_CARD_EMULATION`; iOS 17.4
    /// + EU only with the HCE entitlement. Defaults to `false`.
    public boolean canHostEmulate() {
        return false;
    }

    /// Performs a single tag-read session. Resolves with the discovered
    /// [Tag] (call [Tag#readNdef()] or one of the technology accessors)
    /// or fails with an [NfcException]. The base class fails immediately
    /// with [NfcError#NOT_AVAILABLE].
    ///
    /// Cancel an in-flight read via [#stopRead()].
    public AsyncResource<Tag> readTag(NfcReadOptions options) {
        AsyncResource<Tag> r = new AsyncResource<Tag>();
        r.error(new NfcException(NfcError.NOT_AVAILABLE,
                "NFC is not available on this platform"));
        return r;
    }

    /// Convenience for `readTag(new NfcReadOptions().setNdefOnly(true))`
    /// followed by [Tag#readNdef()]. Resolves with the parsed NDEF
    /// message, or fails with an [NfcException].
    public AsyncResource<NdefMessage> readNdef(NfcReadOptions options) {
        AsyncResource<NdefMessage> chained = new AsyncResource<NdefMessage>();
        chainReadNdef(readTag(options), chained);
        return chained;
    }

    // The anonymous callbacks live inside a static method so they don't
    // capture a synthetic outer-Nfc reference (SpotBugs
    // SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static void chainReadNdef(AsyncResource<Tag> source,
            final AsyncResource<NdefMessage> chained) {
        source.ready(new SuccessCallback<Tag>() {
            public void onSucess(Tag tag) {
                if (tag == null) {
                    chained.error(new NfcException(NfcError.TAG_LOST,
                            "tag-read produced no tag"));
                    return;
                }
                tag.readNdef().ready(new SuccessCallback<NdefMessage>() {
                    public void onSucess(NdefMessage msg) {
                        chained.complete(msg);
                    }
                }).except(new SuccessCallback<Throwable>() {
                    public void onSucess(Throwable err) {
                        chained.error(err);
                    }
                });
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable err) {
                chained.error(err);
            }
        });
    }

    /// Convenience writer -- opens a tag-read session, writes the given
    /// message and resolves with `true`. Fails with
    /// [NfcError#READ_ONLY] for locked tags and with
    /// [NfcError#CAPACITY_EXCEEDED] when the message is too large.
    public AsyncResource<Boolean> writeNdef(NfcReadOptions options,
            NdefMessage message) {
        AsyncResource<Boolean> chained = new AsyncResource<Boolean>();
        chainWriteNdef(readTag(options), message, chained);
        return chained;
    }

    // Static so the anonymous callbacks don't carry a synthetic outer-Nfc
    // reference (SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static void chainWriteNdef(AsyncResource<Tag> source,
            final NdefMessage message,
            final AsyncResource<Boolean> chained) {
        source.ready(new SuccessCallback<Tag>() {
            public void onSucess(Tag tag) {
                if (tag == null) {
                    chained.error(new NfcException(NfcError.TAG_LOST,
                            "tag-read produced no tag"));
                    return;
                }
                tag.writeNdef(message).ready(new SuccessCallback<Boolean>() {
                    public void onSucess(Boolean result) {
                        chained.complete(result);
                    }
                }).except(new SuccessCallback<Throwable>() {
                    public void onSucess(Throwable err) {
                        chained.error(err);
                    }
                });
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable err) {
                chained.error(err);
            }
        });
    }

    /// Cancels any in-flight [#readTag(NfcReadOptions)] /
    /// [#readNdef(NfcReadOptions)] / [#writeNdef(NfcReadOptions, NdefMessage)]
    /// call. The pending `AsyncResource` completes with
    /// [NfcError#USER_CANCELED].
    ///
    /// #### Returns
    ///
    /// `true` when a call was cancelled; `false` when no session was
    /// pending. Always `false` on the fallback base class.
    public boolean stopRead() {
        return false;
    }

    /// Registers a long-running tag-discovery listener -- useful on
    /// Android reader-mode where multiple tags can be tapped in
    /// succession. Each new tag calls [NfcListener#tagDiscovered(Tag)]
    /// from the EDT.
    ///
    /// Ports that do not support multi-shot reading fall back to a
    /// single-shot [#readTag(NfcReadOptions)] each time -- iOS for
    /// example dismisses the system sheet after the first tag and
    /// re-prompts.
    ///
    /// No-op on the fallback base class.
    public void addTagListener(NfcListener listener) {
    }

    /// Removes a listener previously added via
    /// [#addTagListener(NfcListener)]. No-op on the fallback base class.
    public void removeTagListener(NfcListener listener) {
    }

    /// Registers a Host Card Emulation service. Only one service may be
    /// registered per app, and only the AIDs reported by
    /// [HostCardEmulationService#getAids()] are routed to it.
    ///
    /// On Android the platform routing tables are populated from the
    /// service's manifest entry; the Codename One Maven plugin and
    /// BuildDaemon auto-generate that entry from the AIDs at build time.
    /// At runtime this method simply hands the live instance to the port
    /// so APDUs can be dispatched.
    ///
    /// No-op on the fallback base class.
    public void registerHostCardEmulationService(
            HostCardEmulationService service) {
    }

    /// Removes a previously registered HCE service. No-op on the fallback
    /// base class.
    public void unregisterHostCardEmulationService() {
    }
}
