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

/// A single NDEF (NFC Data Exchange Format) record. An NDEF tag carries one
/// [NdefMessage] which contains one or more `NdefRecord`s.
///
/// Most apps construct records via the typed factories:
/// [#createText(String, String)] for human-readable text,
/// [#createUri(String)] for URIs (the most common payload -- launches the
/// associated app on the device), [#createMime(String, byte[])] for binary
/// MIME payloads, and [#createApplicationRecord(String)] for the special
/// Android Application Record (AAR) that pins a tag to a specific package.
///
/// The low-level constructor [#NdefRecord(byte, byte[], byte[], byte[])] is
/// available for vendor / external-type records.
///
/// Records are immutable -- modify them by building a new instance.
public final class NdefRecord {

    /// TNF (Type Name Format) -- record contains no payload.
    public static final byte TNF_EMPTY = 0x00;
    /// TNF -- type is one of the NFC Forum well-known types (e.g. `T`, `U`,
    /// `Sp`). See [#rtdText()], [#rtdUri()].
    public static final byte TNF_WELL_KNOWN = 0x01;
    /// TNF -- type is a MIME media type (RFC 2046).
    public static final byte TNF_MIME_MEDIA = 0x02;
    /// TNF -- type is an absolute URI.
    public static final byte TNF_ABSOLUTE_URI = 0x03;
    /// TNF -- external type, namespaced as `domain:type` (e.g.
    /// `android.com:pkg`).
    public static final byte TNF_EXTERNAL_TYPE = 0x04;
    /// TNF -- record is unknown / unparsed.
    public static final byte TNF_UNKNOWN = 0x05;
    /// TNF -- continuation of a chunked record (rare).
    public static final byte TNF_UNCHANGED = 0x06;

    // RTD bytes are kept as private fields so external callers can't
    // mutate the shared instance (SpotBugs MS_PKGPROTECT); the public
    // accessor methods below each return a fresh defensive copy.
    private static final byte[] RTD_TEXT = new byte[] { 'T' };
    private static final byte[] RTD_URI = new byte[] { 'U' };
    private static final byte[] RTD_SMART_POSTER = new byte[] { 'S', 'p' };
    private static final byte[] RTD_ANDROID_APP = new byte[] { 'a', 'n', 'd',
            'r', 'o', 'i', 'd', '.', 'c', 'o', 'm', ':', 'p', 'k', 'g' };

    /// Record Type Definition (RTD) for well-known text records. Returns
    /// a defensive copy so callers cannot mutate the shared constant.
    public static byte[] rtdText() {
        return clone(RTD_TEXT);
    }

    /// RTD for well-known URI records.
    public static byte[] rtdUri() {
        return clone(RTD_URI);
    }

    /// RTD for SmartPoster (URI + title).
    public static byte[] rtdSmartPoster() {
        return clone(RTD_SMART_POSTER);
    }

    /// RTD for Android Application Record (external type
    /// `android.com:pkg`).
    public static byte[] rtdAndroidApp() {
        return clone(RTD_ANDROID_APP);
    }

    private final byte tnf;
    private final byte[] type;
    private final byte[] id;
    private final byte[] payload;

    /// Constructs a record from its raw NDEF fields. Most callers should
    /// prefer one of the typed factories below.
    ///
    /// #### Parameters
    ///
    /// - `tnf`: one of the `TNF_*` constants
    /// - `type`: type field; meaning depends on `tnf` (RTD value, MIME type
    ///   string bytes, ...). Must not be `null` -- pass an empty array
    ///   for [#TNF_EMPTY] / [#TNF_UNKNOWN]
    /// - `id`: optional record id; pass an empty array if unused
    /// - `payload`: record payload; must not be `null`
    public NdefRecord(byte tnf, byte[] type, byte[] id, byte[] payload) {
        if (type == null) {
            type = new byte[0];
        }
        if (id == null) {
            id = new byte[0];
        }
        if (payload == null) {
            payload = new byte[0];
        }
        this.tnf = tnf;
        this.type = clone(type);
        this.id = clone(id);
        this.payload = clone(payload);
    }

    /// One of the `TNF_*` constants.
    public byte getTnf() {
        return tnf;
    }

    /// Raw type field. Defensively copied -- mutating the returned array
    /// does not affect the record.
    public byte[] getType() {
        return clone(type);
    }

    /// Raw record id. Empty when no id was assigned.
    public byte[] getId() {
        return clone(id);
    }

    /// Raw payload bytes. Defensively copied.
    public byte[] getPayload() {
        return clone(payload);
    }

    /// Builds a well-known TEXT (`T`) record per NFC Forum RTD-Text 1.0.
    /// The text is UTF-8 encoded; the BCP-47 language tag (e.g. `"en"`,
    /// `"ja"`) goes in the leading status byte block.
    ///
    /// #### Parameters
    ///
    /// - `languageCode`: BCP-47 language tag, `null` defaults to `"en"`.
    ///   Must be ASCII and at most 63 bytes long
    /// - `text`: the text payload, UTF-8 (the spec also allows UTF-16; this
    ///   factory always writes UTF-8)
    public static NdefRecord createText(String languageCode, String text) {
        if (languageCode == null || languageCode.length() == 0) {
            languageCode = "en";
        }
        if (text == null) {
            text = "";
        }
        byte[] langBytes = toUtf8(languageCode);
        byte[] textBytes = toUtf8(text);
        if (langBytes.length > 63) {
            throw new IllegalArgumentException("language code too long");
        }
        byte[] payload = new byte[1 + langBytes.length + textBytes.length];
        payload[0] = (byte) (langBytes.length & 0x3F);
        System.arraycopy(langBytes, 0, payload, 1, langBytes.length);
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.length,
                textBytes.length);
        return new NdefRecord(TNF_WELL_KNOWN, RTD_TEXT, null, payload);
    }

    /// Builds a well-known URI (`U`) record. Common URI prefixes
    /// (`http://www.`, `https://www.`, `tel:`, `mailto:`, ...) are replaced
    /// with the one-byte abbreviation codes defined in NFC Forum RTD-URI
    /// 1.0 to save tag space.
    public static NdefRecord createUri(String uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        byte prefix = 0;
        String tail = uri;
        for (int i = 1; i < URI_PREFIXES.length; i++) {
            if (uri.startsWith(URI_PREFIXES[i])) {
                prefix = (byte) i;
                tail = uri.substring(URI_PREFIXES[i].length());
                break;
            }
        }
        byte[] tailBytes = toUtf8(tail);
        byte[] payload = new byte[1 + tailBytes.length];
        payload[0] = prefix;
        System.arraycopy(tailBytes, 0, payload, 1, tailBytes.length);
        return new NdefRecord(TNF_WELL_KNOWN, RTD_URI, null, payload);
    }

    /// Builds a MIME media record (TNF = [#TNF_MIME_MEDIA]). Use for binary
    /// payloads -- images, small structured data, application/vnd.*.
    public static NdefRecord createMime(String mimeType, byte[] payload) {
        if (mimeType == null || mimeType.length() == 0) {
            throw new IllegalArgumentException("mimeType must not be empty");
        }
        return new NdefRecord(TNF_MIME_MEDIA, toAscii(mimeType), null,
                payload);
    }

    /// Builds an external-type record (TNF = [#TNF_EXTERNAL_TYPE]). The
    /// `domain:type` string is encoded as ASCII and stored in the type
    /// field; the payload is passed through verbatim.
    ///
    /// External types are the recommended way to ship custom data on a tag
    /// without colliding with NFC Forum well-known types.
    public static NdefRecord createExternal(String domain, String type,
            byte[] payload) {
        if (domain == null || type == null) {
            throw new IllegalArgumentException("domain/type required");
        }
        String composed = domain.toLowerCase() + ":" + type.toLowerCase();
        return new NdefRecord(TNF_EXTERNAL_TYPE, toAscii(composed), null,
                payload);
    }

    /// Builds an Android Application Record (AAR). When a tag carrying an
    /// AAR is tapped on Android, the OS launches the named package
    /// instead of offering the user a chooser. Honoured only on Android --
    /// iOS ignores AARs.
    ///
    /// #### Parameters
    ///
    /// - `packageName`: e.g. `"com.example.app"`
    public static NdefRecord createApplicationRecord(String packageName) {
        if (packageName == null || packageName.length() == 0) {
            throw new IllegalArgumentException("packageName required");
        }
        return new NdefRecord(TNF_EXTERNAL_TYPE, RTD_ANDROID_APP, null,
                toAscii(packageName));
    }

    /// Convenience: decodes a [#createText(String, String)] payload back to
    /// its text content. Returns `null` if the record is not a well-known
    /// text record.
    public String getTextPayload() {
        if (tnf != TNF_WELL_KNOWN || !equalsBytes(type, RTD_TEXT)) {
            return null;
        }
        if (payload.length < 1) {
            return null;
        }
        int langLen = payload[0] & 0x3F;
        if (langLen + 1 > payload.length) {
            return null;
        }
        return fromUtf8(payload, 1 + langLen, payload.length - 1 - langLen);
    }

    /// Convenience: decodes a [#createUri(String)] payload back to its full
    /// URI (re-expanding the leading prefix code). Returns `null` if the
    /// record is not a recognised URI record.
    public String getUriPayload() {
        if (payload.length < 1) {
            return null;
        }
        int prefix = payload[0] & 0xFF;
        if (tnf == TNF_WELL_KNOWN && equalsBytes(type, RTD_URI)) {
            String p = prefix < URI_PREFIXES.length ? URI_PREFIXES[prefix]
                    : "";
            return p + fromUtf8(payload, 1, payload.length - 1);
        }
        if (tnf == TNF_ABSOLUTE_URI) {
            return fromUtf8(type, 0, type.length);
        }
        return null;
    }

    private static byte[] clone(byte[] in) {
        byte[] out = new byte[in.length];
        System.arraycopy(in, 0, out, 0, in.length);
        return out;
    }

    private static boolean equalsBytes(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] toUtf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 is required by JLS to be present on every JVM, so this
            // branch is unreachable. Throw rather than fall back to the
            // platform default encoding (SpotBugs DM_DEFAULT_ENCODING).
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static byte[] toAscii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            // US-ASCII is required by JLS to be present on every JVM.
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static String fromUtf8(byte[] data, int offset, int length) {
        try {
            return new String(data, offset, length, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 is required by JLS to be present on every JVM.
            throw new RuntimeException(e.toString(), e);
        }
    }

    static final String[] URI_PREFIXES = new String[] {
            "",
            "http://www.",
            "https://www.",
            "http://",
            "https://",
            "tel:",
            "mailto:",
            "ftp://anonymous:anonymous@",
            "ftp://ftp.",
            "ftps://",
            "sftp://",
            "smb://",
            "nfs://",
            "ftp://",
            "dav://",
            "news:",
            "telnet://",
            "imap:",
            "rtsp://",
            "urn:",
            "pop:",
            "sip:",
            "sips:",
            "tftp:",
            "btspp://",
            "btl2cap://",
            "btgoep://",
            "tcpobex://",
            "irdaobex://",
            "file://",
            "urn:epc:id:",
            "urn:epc:tag:",
            "urn:epc:pat:",
            "urn:epc:raw:",
            "urn:epc:",
            "urn:nfc:"
    };
}
