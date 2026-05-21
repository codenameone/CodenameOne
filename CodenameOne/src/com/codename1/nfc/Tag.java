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

import com.codename1.util.AsyncResource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/// A tag discovered by [Nfc#readTag(NfcReadOptions)] or
/// [Nfc#addTagListener(NfcListener)]. The lifetime of a `Tag` is tied to the
/// reader session that produced it: once the tag leaves the field (or the
/// caller closes the session) all subsequent calls on this instance fail
/// with [NfcError#TAG_LOST].
///
/// Apps inspect the tag via [#getTypes()] to learn which technologies are
/// available and call one of:
///
/// - [#readNdef()] / [#writeNdef(NdefMessage)] for any [TagType#NDEF] tag
/// - [#getIsoDep()] for ISO-7816 smart cards / payment / passport
/// - [#getMifareClassic()] / [#getMifareUltralight()] for NXP MIFARE
/// - [#getNfcA()] / [#getNfcB()] / [#getNfcF()] / [#getNfcV()] for raw
///   low-level transceive
///
/// A `Tag` may return `null` from a technology accessor when the underlying
/// tag does not support that technology (consult [#supports(TagType)]
/// first). Ports subclass `Tag` to provide the native transceive
/// implementation -- application code never instantiates `Tag` directly.
public abstract class Tag {

    private final Set<TagType> types;
    private final byte[] id;

    /// Subclasses populate the technology list and (when known) the tag's
    /// hardware UID. Pass an empty array for `id` if the platform does not
    /// expose one.
    protected Tag(Set<TagType> types, byte[] id) {
        Set<TagType> copy = new HashSet<TagType>();
        if (types != null) {
            copy.addAll(types);
        }
        this.types = Collections.unmodifiableSet(copy);
        if (id == null) {
            id = new byte[0];
        }
        this.id = new byte[id.length];
        System.arraycopy(id, 0, this.id, 0, id.length);
    }

    /// Technologies advertised by this tag. Always at least one entry on a
    /// discovered tag; an empty set indicates a tag the platform could not
    /// classify (rare).
    public final Set<TagType> getTypes() {
        return types;
    }

    /// Convenience: `getTypes().contains(t)`.
    public final boolean supports(TagType t) {
        return types.contains(t);
    }

    /// Tag's hardware identifier (`NFCA.uid` on iOS / `Tag.getId()` on
    /// Android). Defensively copied. Returns an empty array if the platform
    /// did not surface a UID.
    public final byte[] getId() {
        byte[] out = new byte[id.length];
        System.arraycopy(id, 0, out, 0, id.length);
        return out;
    }

    /// Reads the NDEF message currently stored on this tag. Fails with
    /// [NfcError#UNSUPPORTED_TAG] if [#supports(TagType)] of [TagType#NDEF]
    /// is `false`.
    public AsyncResource<NdefMessage> readNdef() {
        AsyncResource<NdefMessage> r = new AsyncResource<NdefMessage>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "this tag does not implement readNdef()"));
        return r;
    }

    /// Writes (or overwrites) the NDEF message on this tag. Fails with
    /// [NfcError#READ_ONLY] if the tag has been locked, and with
    /// [NfcError#CAPACITY_EXCEEDED] when the serialised message is larger
    /// than [#getMaxNdefSize()]. Default implementation reports
    /// [NfcError#UNSUPPORTED_TAG].
    public AsyncResource<Boolean> writeNdef(NdefMessage message) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "this tag does not implement writeNdef()"));
        return r;
    }

    /// Permanently locks the tag's NDEF area against future writes. Not all
    /// tags expose this operation -- on those the call fails with
    /// [NfcError#UNSUPPORTED_TAG]. **Irreversible** -- a locked tag cannot
    /// be re-armed.
    public AsyncResource<Boolean> makeReadOnly() {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                "this tag does not implement makeReadOnly()"));
        return r;
    }

    /// Largest NDEF message size (in bytes) that fits on this tag. Returns
    /// `-1` when the platform does not expose the figure (iOS Core NFC
    /// hides it on non-NDEF-formatted tags).
    public int getMaxNdefSize() {
        return -1;
    }

    /// `true` when the tag's NDEF area is writable. Defaults to `false` on
    /// the base class.
    public boolean isWritable() {
        return false;
    }

    /// Convenience: `true` when [#readNdef()] returns at least one
    /// [NdefRecord] right now. Defaults to `supports(TagType.NDEF)`.
    public boolean hasNdef() {
        return supports(TagType.NDEF);
    }

    /// Returns an [IsoDep] view of this tag for ISO 7816 / EMV / passport
    /// APDU exchange, or `null` if the tag does not advertise
    /// [TagType#ISO_DEP].
    public IsoDep getIsoDep() {
        return null;
    }

    /// Returns a [MifareClassic] view of this tag, or `null` when not
    /// supported. iOS always returns `null` for MIFARE Classic.
    public MifareClassic getMifareClassic() {
        return null;
    }

    /// Returns a [MifareUltralight] view, or `null` when not supported.
    public MifareUltralight getMifareUltralight() {
        return null;
    }

    /// Raw NFC-A (ISO 14443-3A) transceive view, or `null`.
    public NfcA getNfcA() {
        return null;
    }

    /// Raw NFC-B (ISO 14443-3B) transceive view, or `null`. Android-only.
    public NfcB getNfcB() {
        return null;
    }

    /// Raw FeliCa (JIS X 6319-4) transceive view, or `null`.
    public NfcF getNfcF() {
        return null;
    }

    /// Raw ISO 15693 / vicinity transceive view, or `null`. Android-only.
    public NfcV getNfcV() {
        return null;
    }
}
