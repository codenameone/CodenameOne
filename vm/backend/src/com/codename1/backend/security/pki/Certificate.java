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
package com.codename1.backend.security.pki;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.codename1.backend.Crypto;

/// An X.509 v3 certificate, read from DER or PEM.
///
/// ```java
/// Certificate cert = Certificate.fromPem(pem);
/// cert.getSubject().get("CN");          // "api.example.com"
/// cert.getDnsNames();                   // ["api.example.com", "*.example.com"]
/// cert.isValidAt(System.currentTimeMillis());
/// ```
///
/// Reading checks the structure, not the trust: a certificate is only as good
/// as the chain [CertificateValidator] builds for it.
public final class Certificate {
    static final String BASIC_CONSTRAINTS = "2.5.29.19"; //NOPMD AvoidUsingHardCodedIP - an OID
    static final String KEY_USAGE = "2.5.29.15"; //NOPMD AvoidUsingHardCodedIP - an OID
    static final String EXTENDED_KEY_USAGE = "2.5.29.37"; //NOPMD AvoidUsingHardCodedIP - an OID
    static final String SUBJECT_ALT_NAME = "2.5.29.17"; //NOPMD AvoidUsingHardCodedIP - an OID
    static final String SUBJECT_KEY_ID = "2.5.29.14"; //NOPMD AvoidUsingHardCodedIP - an OID
    static final String AUTHORITY_KEY_ID = "2.5.29.35"; //NOPMD AvoidUsingHardCodedIP - an OID

    /// The extended key usage of a TLS server certificate.
    public static final String SERVER_AUTH = "1.3.6.1.5.5.7.3.1"; //NOPMD AvoidUsingHardCodedIP - an OID
    /// The extended key usage of a TLS client certificate.
    public static final String CLIENT_AUTH = "1.3.6.1.5.5.7.3.2"; //NOPMD AvoidUsingHardCodedIP - an OID
    static final String ANY_EXTENDED_KEY_USAGE = "2.5.29.37.0"; //NOPMD AvoidUsingHardCodedIP - an OID

    /// `keyUsage` bit: digitalSignature.
    public static final int DIGITAL_SIGNATURE = 0x80;
    /// `keyUsage` bit: keyEncipherment.
    public static final int KEY_ENCIPHERMENT = 0x20;
    /// `keyUsage` bit: keyAgreement.
    public static final int KEY_AGREEMENT = 0x08;
    /// `keyUsage` bit: keyCertSign.
    public static final int KEY_CERT_SIGN = 0x04;
    /// `keyUsage` bit: cRLSign.
    public static final int CRL_SIGN = 0x02;

    private final byte[] der;
    private final byte[] tbs;
    private final byte[] signatureAlgorithm;
    private final byte[] signature;
    private final int version;
    private final byte[] serial;
    private final DistinguishedName issuer;
    private final DistinguishedName subject;
    private final long notBefore;
    private final long notAfter;
    private final byte[] publicKey;
    private final List<String> criticalExtensions = new ArrayList<String>();
    private boolean ca;
    private int pathLength = -1;
    private int keyUsage = -1;
    private List<String> extendedKeyUsage;
    private final List<String> dnsNames = new ArrayList<String>();
    private final List<String> ipAddresses = new ArrayList<String>();
    private final List<String> emails = new ArrayList<String>();
    private final List<String> uris = new ArrayList<String>();
    private byte[] subjectKeyId;
    private byte[] authorityKeyId;

    private Certificate(byte[] der) throws IOException {
        this.der = der.clone();
        try {
            Asn1.Reader cert = new Asn1.Reader(this.der).open(Asn1.SEQUENCE);
            tbs = cert.raw(Asn1.SEQUENCE);
            signatureAlgorithm = cert.raw(Asn1.SEQUENCE);
            signature = cert.bits();
            cert.finish();

            Asn1.Reader body = new Asn1.Reader(tbs).open(Asn1.SEQUENCE);
            Asn1.Reader explicitVersion = body.openOptional(Asn1.explicit(0));
            version = explicitVersion == null ? 1 : explicitVersion.smallInteger() + 1;
            serial = body.unsignedInteger();
            byte[] innerAlgorithm = body.raw(Asn1.SEQUENCE);
            if (!Asn1.same(innerAlgorithm, signatureAlgorithm)) {
                throw new CertificateException("The certificate's two signature algorithm fields "
                        + "differ");
            }
            issuer = DistinguishedName.decode(body.raw(Asn1.SEQUENCE));
            Asn1.Reader validity = body.open(Asn1.SEQUENCE);
            notBefore = validity.time();
            notAfter = validity.time();
            subject = DistinguishedName.decode(body.raw(Asn1.SEQUENCE));
            publicKey = body.raw(Asn1.SEQUENCE);
            if (body.peek() == Asn1.implicit(1)) {
                body.skip();
            }
            if (body.peek() == Asn1.implicit(2)) {
                body.skip();
            }
            Asn1.Reader extensions = body.openOptional(Asn1.explicit(3));
            if (extensions != null) {
                if (version != 3) {
                    throw new CertificateException("Extensions in a v" + version + " certificate");
                }
                readExtensions(extensions.open(Asn1.SEQUENCE));
            }
            body.finish();
        } catch (CertificateException err) {
            throw err;
        } catch (IOException err) {
            throw new CertificateException("Not an X.509 certificate: " + err.getMessage(), err);
        }
    }

    /// Reads a certificate from its DER.
    public static Certificate decode(byte[] der) throws IOException {
        if (der == null) {
            throw new CertificateException("No certificate was given");
        }
        return new Certificate(der);
    }

    /// The first `CERTIFICATE` block of `pem`.
    public static Certificate fromPem(String pem) throws IOException {
        List<Certificate> all = listFromPem(pem);
        if (all.isEmpty()) {
            throw new CertificateException("The PEM text holds no CERTIFICATE block");
        }
        return all.get(0);
    }

    /// Every `CERTIFICATE` block of `pem`, in order, as a full-chain file holds
    /// them: the leaf first, then its issuers.
    public static List<Certificate> listFromPem(String pem) throws IOException {
        List<Certificate> out = new ArrayList<Certificate>();
        for (byte[] block : Pem.decode(pem, Pem.CERTIFICATE)) {
            out.add(new Certificate(block));
        }
        return out;
    }

    private void readExtensions(Asn1.Reader list) throws IOException {
        List<String> seen = new ArrayList<String>();
        while (list.hasMore()) {
            Asn1.Reader ext = list.open(Asn1.SEQUENCE);
            String oid = ext.oid();
            boolean critical = ext.peek() == Asn1.BOOLEAN && ext.bool();
            byte[] value = ext.read(Asn1.OCTET_STRING);
            ext.finish();
            if (seen.contains(oid)) {
                throw new CertificateException("The extension " + oid + " appears twice");
            }
            seen.add(oid);
            if (critical) {
                criticalExtensions.add(oid);
            }
            Asn1.Reader in = new Asn1.Reader(value);
            if (BASIC_CONSTRAINTS.equals(oid)) {
                Asn1.Reader bc = in.open(Asn1.SEQUENCE);
                ca = bc.peek() == Asn1.BOOLEAN && bc.bool();
                if (bc.peek() == Asn1.INTEGER) {
                    pathLength = bc.smallInteger();
                }
                bc.finish();
            } else if (KEY_USAGE.equals(oid)) {
                byte[] bits = in.read(Asn1.BIT_STRING);
                keyUsage = bits.length > 1 ? bits[1] & 0xff : 0;
            } else if (EXTENDED_KEY_USAGE.equals(oid)) {
                Asn1.Reader eku = in.open(Asn1.SEQUENCE);
                extendedKeyUsage = new ArrayList<String>();
                while (eku.hasMore()) {
                    extendedKeyUsage.add(eku.oid());
                }
            } else if (SUBJECT_ALT_NAME.equals(oid)) {
                readGeneralNames(in.open(Asn1.SEQUENCE));
            } else if (SUBJECT_KEY_ID.equals(oid)) {
                subjectKeyId = in.read(Asn1.OCTET_STRING);
            } else if (AUTHORITY_KEY_ID.equals(oid)) {
                Asn1.Reader aki = in.open(Asn1.SEQUENCE);
                if (aki.peek() == Asn1.implicit(0)) {
                    authorityKeyId = aki.read(Asn1.implicit(0));
                }
            }
        }
    }

    private void readGeneralNames(Asn1.Reader names) throws IOException {
        while (names.hasMore()) {
            int tag = names.peek();
            if (tag == Asn1.implicit(2)) {
                dnsNames.add(Asn1.ascii(names.read(tag)));
            } else if (tag == Asn1.implicit(7)) {
                ipAddresses.add(ipText(names.read(tag)));
            } else if (tag == Asn1.implicit(1)) {
                emails.add(Asn1.ascii(names.read(tag)));
            } else if (tag == Asn1.implicit(6)) {
                uris.add(Asn1.ascii(names.read(tag)));
            } else {
                names.skip();
            }
        }
    }

    static String ipText(byte[] address) throws IOException {
        if (address.length == 4) {
            return (address[0] & 0xff) + "." + (address[1] & 0xff) + "." + (address[2] & 0xff)
                    + "." + (address[3] & 0xff);
        }
        if (address.length == 16) {
            StringBuilder out = new StringBuilder();
            for (int iter = 0 ; iter < 16 ; iter += 2) {
                if (iter > 0) {
                    out.append(':');
                }
                out.append(Integer.toHexString(((address[iter] & 0xff) << 8)
                        | (address[iter + 1] & 0xff)));
            }
            return out.toString();
        }
        throw new CertificateException("An IP address of " + address.length + " bytes");
    }

    /// The DER this certificate was read from.
    public byte[] getEncoded() {
        return der.clone();
    }

    /// The certificate as one PEM block.
    public String toPem() {
        return Pem.encode(Pem.CERTIFICATE, der);
    }

    /// 1, 2 or 3.
    public int getVersion() {
        return version;
    }

    /// The serial number as lower case hex, without leading zeros.
    public String getSerialNumber() {
        String hex = Asn1.hex(serial);
        int skip = 0;
        while (skip < hex.length() - 1 && hex.charAt(skip) == '0') {
            skip++;
        }
        return hex.substring(skip);
    }

    /// Who signed this certificate.
    public DistinguishedName getIssuer() {
        return issuer;
    }

    /// Whom this certificate names.
    public DistinguishedName getSubject() {
        return subject;
    }

    /// The start of the validity period, in milliseconds since the epoch.
    public long getNotBefore() {
        return notBefore;
    }

    /// The end of the validity period, in milliseconds since the epoch.
    public long getNotAfter() {
        return notAfter;
    }

    /// True when `millis` is inside the validity period, both ends included.
    public boolean isValidAt(long millis) {
        return millis >= notBefore && millis <= notAfter;
    }

    /// The subject's public key, as SubjectPublicKeyInfo DER: what `Crypto.verify`
    /// and `Crypto.ecdh` take.
    public byte[] getPublicKey() {
        return publicKey.clone();
    }

    /// The [Crypto] algorithm the issuer signed with (`RS256`, `PS256`, `ES384`...).
    ///
    /// @throws CertificateException when the algorithm is outside the profile
    public String getSignatureAlgorithm() throws IOException {
        return SignatureAlgorithms.forIdentifier(signatureAlgorithm);
    }

    /// Whether the basic constraints extension says this is a CA.
    public boolean isCertificateAuthority() {
        return ca;
    }

    /// The basic constraints path length, or -1 when it has none.
    public int getPathLength() {
        return pathLength;
    }

    /// The first byte of `keyUsage` ([#DIGITAL_SIGNATURE], [#KEY_CERT_SIGN]...),
    /// or -1 when the certificate has no key usage extension and so allows
    /// every use.
    public int getKeyUsage() {
        return keyUsage;
    }

    /// The extended key usage OIDs ([#SERVER_AUTH], [#CLIENT_AUTH]...), or null
    /// when the certificate has no such extension and so allows every purpose.
    public List<String> getExtendedKeyUsage() {
        return extendedKeyUsage == null ? null : Collections.unmodifiableList(extendedKeyUsage);
    }

    /// The DNS names of the subject alternative name extension.
    public List<String> getDnsNames() {
        return Collections.unmodifiableList(dnsNames);
    }

    /// The IP addresses of the subject alternative name extension, IPv4 dotted
    /// and IPv6 as eight uncompressed hex groups.
    public List<String> getIpAddresses() {
        return Collections.unmodifiableList(ipAddresses);
    }

    /// The e-mail addresses of the subject alternative name extension.
    public List<String> getEmailAddresses() {
        return Collections.unmodifiableList(emails);
    }

    /// The URIs of the subject alternative name extension.
    public List<String> getUris() {
        return Collections.unmodifiableList(uris);
    }

    /// The OIDs of the extensions marked critical.
    public List<String> getCriticalExtensions() {
        return Collections.unmodifiableList(criticalExtensions);
    }

    byte[] subjectKeyId() {
        return subjectKeyId;
    }

    byte[] authorityKeyId() {
        return authorityKeyId;
    }

    /// True when the subject and the issuer are the same name: a root, or a
    /// self-issued certificate.
    public boolean isSelfIssued() {
        return subject.equals(issuer);
    }

    /// Whether `issuerPublicKey` signed this certificate.
    ///
    /// @return false when the signature does not verify
    /// @throws IOException when the algorithm is outside the profile or the key
    /// is not of the kind it uses
    public boolean isSignedBy(byte[] issuerPublicKey) throws IOException {
        return Crypto.verify(getSignatureAlgorithm(), issuerPublicKey, tbs, signature);
    }

    /// Whether this certificate is for `host`: a DNS name, matched against the
    /// subject alternative names with a wildcard allowed for the leftmost label
    /// only, or an IP literal matched against the IP addresses. The common name
    /// is not consulted, as RFC 9525 and every browser require.
    public boolean matchesHost(String host) {
        if (host == null || host.length() == 0) {
            return false;
        }
        String ip = normalizeIp(host);
        if (ip != null) {
            for (String candidate : ipAddresses) {
                if (ip.equals(candidate)) {
                    return true;
                }
            }
            return false;
        }
        String want = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
        for (String pattern : dnsNames) {
            if (dnsMatches(pattern, want)) {
                return true;
            }
        }
        return false;
    }

    static boolean dnsMatches(String pattern, String host) {
        if (pattern.startsWith("*.")) {
            int dot = host.indexOf('.');
            if (dot <= 0) {
                return false;
            }
            String suffix = pattern.substring(2);
            // "*.com" style patterns name a whole public suffix; require two labels.
            return suffix.indexOf('.') > 0 && host.substring(dot + 1).equalsIgnoreCase(suffix);
        }
        return pattern.equalsIgnoreCase(host);
    }

    /// The form [#getIpAddresses] uses, or null when `host` is not an IP literal.
    private static String normalizeIp(String host) {
        String h = host;
        if (h.startsWith("[") && h.endsWith("]")) {
            h = h.substring(1, h.length() - 1);
        }
        if (h.indexOf(':') >= 0) {
            return normalizeIpv6(h);
        }
        String[] parts = Asn1.split(h, '.');
        if (parts.length != 4) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0 ; iter < 4 ; iter++) {
            int value = decimal(parts[iter]);
            if (value < 0 || value > 255) {
                return null;
            }
            if (iter > 0) {
                out.append('.');
            }
            out.append(value);
        }
        return out.toString();
    }

    private static String normalizeIpv6(String h) {
        String[] halves = splitOnce(h, "::");
        if (halves.length > 2) {
            return null;
        }
        String[] head = halves[0].length() == 0 ? new String[0] : Asn1.split(halves[0], ':');
        String[] tail = halves.length < 2 || halves[1].length() == 0 ? new String[0]
                : Asn1.split(halves[1], ':');
        int missing = 8 - head.length - tail.length;
        if (halves.length == 1 ? missing != 0 : missing < 1) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int written = 0;
        for (String group : head) {
            if (!appendGroup(out, group, written)) {
                return null;
            }
            written++;
        }
        for (int iter = 0 ; iter < (halves.length == 1 ? 0 : missing) ; iter++) {
            appendGroup(out, "0", written);
            written++;
        }
        for (String group : tail) {
            if (!appendGroup(out, group, written)) {
                return null;
            }
            written++;
        }
        return out.toString();
    }

    private static boolean appendGroup(StringBuilder out, String group, int index) {
        if (group.length() == 0 || group.length() > 4) {
            return false;
        }
        int value = 0;
        for (int iter = 0 ; iter < group.length() ; iter++) {
            int digit = Character.digit(group.charAt(iter), 16);
            if (digit < 0) {
                return false;
            }
            value = value * 16 + digit;
        }
        if (index > 0) {
            out.append(':');
        }
        out.append(Integer.toHexString(value));
        return true;
    }

    private static String[] splitOnce(String value, String sep) {
        int at = value.indexOf(sep);
        if (at < 0) {
            return new String[] {value};
        }
        if (value.indexOf(sep, at + 1) >= 0) {
            return new String[] {"", "", ""};
        }
        return new String[] {value.substring(0, at), value.substring(at + sep.length())};
    }

    private static int decimal(String part) {
        if (part.length() == 0 || part.length() > 3) {
            return -1;
        }
        int out = 0;
        for (int iter = 0 ; iter < part.length() ; iter++) {
            char c = part.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }

    /// Two certificates are equal when their DER is.
    @Override
    public boolean equals(Object other) {
        return other instanceof Certificate && Asn1.same(der, ((Certificate) other).der);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(der);
    }

    /// The subject, the issuer and the serial number, for a log line.
    @Override
    public String toString() {
        return "Certificate[subject=" + subject + ", issuer=" + issuer + ", serial="
                + getSerialNumber() + "]";
    }
}
