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

/// Validates a certificate chain against trust anchors the application names.
///
/// ```java
/// CertificateValidator validator = new CertificateValidator(
///         Certificate.listFromPem(KeyFiles.read("/etc/app/partner-ca.pem")))
///         .forHost("partner.example.com");
/// List<Certificate> path = validator.validate(Certificate.listFromPem(presented));
/// ```
///
/// There is no default trust store: nothing is trusted unless it is given. A
/// path is built from the first certificate, the leaf, through the other
/// certificates given (in any order) to one of the anchors, and every link is
/// checked:
///
/// - each certificate is signed by the next, within the signature profile of
///   [Certificate#getSignatureAlgorithm];
/// - each certificate, the leaf and the intermediates, is inside its validity
///   period at the validation time;
/// - each issuer below the anchor is a CA by its basic constraints, has
///   `keyCertSign` when it carries a key usage, and its path length is not
///   exceeded;
/// - no certificate carries a critical extension this validator does not
///   understand (name constraints and certificate policies among them);
/// - when [#forHost] or [#forPurpose] is set, the leaf matches.
///
/// An anchor is trusted as given: its signature, validity and constraints are
/// not checked, as RFC 5280 treats an anchor as a name and a key. Revocation
/// (CRL, OCSP) is not checked.
public final class CertificateValidator {
    private static final int MAX_PATH = 10;

    private final List<Certificate> anchors;
    private long at = -1;
    private String host;
    private String purpose;

    /// A validator that trusts `anchors` and nothing else.
    public CertificateValidator(List<Certificate> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            throw new IllegalArgumentException("At least one trust anchor is required");
        }
        this.anchors = new ArrayList<Certificate>(anchors);
    }

    /// Validates at `millis` instead of the current time.
    public CertificateValidator at(long millis) {
        this.at = millis;
        return this;
    }

    /// Requires the leaf to be for `host`, by [Certificate#matchesHost], and to
    /// allow [Certificate#SERVER_AUTH] when it restricts its purposes.
    public CertificateValidator forHost(String host) {
        this.host = host;
        if (purpose == null) {
            purpose = Certificate.SERVER_AUTH;
        }
        return this;
    }

    /// Requires the leaf to allow an extended key usage, such as
    /// [Certificate#CLIENT_AUTH] for a client certificate. A leaf with no
    /// extended key usage extension allows every purpose.
    public CertificateValidator forPurpose(String extendedKeyUsage) {
        this.purpose = extendedKeyUsage;
        return this;
    }

    /// Validates `chain`, the leaf first.
    ///
    /// @return the path from the leaf to the anchor, both included
    /// @throws CertificateException naming the certificate that failed and why
    public List<Certificate> validate(List<Certificate> chain) throws IOException {
        if (chain == null || chain.isEmpty()) {
            throw new CertificateException("No certificate was presented");
        }
        long now = at >= 0 ? at : System.currentTimeMillis();
        Certificate leaf = chain.get(0);
        checkLeaf(leaf);
        List<Certificate> pool = new ArrayList<Certificate>(chain.subList(1, chain.size()));
        List<Certificate> path = new ArrayList<Certificate>();
        Certificate current = leaf;
        int intermediates = 0;
        while (true) {
            if (anchors.contains(current)) {
                path.add(current);
                return Collections.unmodifiableList(path);
            }
            checkCommon(current, now);
            if (!path.isEmpty()) {
                checkIssuer(current, intermediates);
                if (!current.isSelfIssued()) {
                    intermediates++;
                }
            }
            path.add(current);
            if (path.size() > MAX_PATH) {
                throw new CertificateException("The chain is longer than " + MAX_PATH
                        + " certificates");
            }
            Certificate anchor = issuerAmong(current, anchors);
            if (anchor != null) {
                path.add(anchor);
                return Collections.unmodifiableList(path);
            }
            Certificate next = issuerAmong(current, pool);
            if (next == null) {
                throw new CertificateException("No trusted issuer for " + describe(current)
                        + ": its issuer " + current.getIssuer() + " is neither a trust anchor "
                        + "nor a certificate in the chain that signed it");
            }
            pool.remove(next);
            current = next;
        }
    }

    /// The certificate in `candidates` whose subject is `cert`'s issuer and
    /// whose key verifies `cert`'s signature.
    private static Certificate issuerAmong(Certificate cert, List<Certificate> candidates)
            throws IOException {
        IOException failure = null;
        for (Certificate candidate : candidates) {
            if (!candidate.getSubject().equals(cert.getIssuer()) || candidate.equals(cert)) {
                continue;
            }
            byte[] want = cert.authorityKeyId();
            byte[] have = candidate.subjectKeyId();
            if (want != null && have != null && !Asn1.same(want, have)) {
                continue;
            }
            try {
                if (cert.isSignedBy(candidate.getPublicKey())) {
                    return candidate;
                }
            } catch (IOException err) {
                failure = err;
            }
        }
        if (failure != null) {
            throw new CertificateException("Could not check the signature on " + describe(cert)
                    + ": " + failure.getMessage(), failure);
        }
        return null;
    }

    private void checkLeaf(Certificate leaf) throws CertificateException {
        if (host != null && !leaf.matchesHost(host)) {
            throw new CertificateException(describe(leaf) + " is not for " + host + "; it names "
                    + leaf.getDnsNames() + " " + leaf.getIpAddresses());
        }
        List<String> eku = leaf.getExtendedKeyUsage();
        if (purpose != null && eku != null && !eku.contains(purpose)
                && !eku.contains(Certificate.ANY_EXTENDED_KEY_USAGE)) {
            throw new CertificateException(describe(leaf) + " does not allow the purpose "
                    + purpose + "; it allows " + eku);
        }
    }

    private static void checkCommon(Certificate cert, long now) throws CertificateException {
        if (now < cert.getNotBefore()) {
            throw new CertificateException(describe(cert) + " is not valid until "
                    + Asn1.formatTime(cert.getNotBefore()));
        }
        if (now > cert.getNotAfter()) {
            throw new CertificateException(describe(cert) + " expired at "
                    + Asn1.formatTime(cert.getNotAfter()));
        }
        for (String oid : cert.getCriticalExtensions()) {
            if (!Certificate.BASIC_CONSTRAINTS.equals(oid) && !Certificate.KEY_USAGE.equals(oid)
                    && !Certificate.EXTENDED_KEY_USAGE.equals(oid)
                    && !Certificate.SUBJECT_ALT_NAME.equals(oid)) {
                throw new CertificateException(describe(cert) + " carries the critical extension "
                        + oid + ", which this validator does not process");
            }
        }
    }

    private static void checkIssuer(Certificate cert, int below) throws CertificateException {
        if (!cert.isCertificateAuthority()) {
            throw new CertificateException(describe(cert) + " issued a certificate but is not a "
                    + "CA: its basic constraints do not say cA");
        }
        if (cert.getKeyUsage() >= 0 && (cert.getKeyUsage() & Certificate.KEY_CERT_SIGN) == 0) {
            throw new CertificateException(describe(cert) + " issued a certificate but its key "
                    + "usage does not include keyCertSign");
        }
        if (cert.getPathLength() >= 0 && below > cert.getPathLength()) {
            throw new CertificateException(describe(cert) + " allows " + cert.getPathLength()
                    + " intermediate CAs below it, and the chain has " + below);
        }
    }

    private static String describe(Certificate cert) {
        return "the certificate for " + cert.getSubject();
    }
}
