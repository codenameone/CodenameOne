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
package com.codename1.backend.security.saml2;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Verifies the enveloped XML signature of one SAML element, as the SAML 2.0
/// core specification's section 5 profiles XML Signature, and nothing wider.
///
/// What makes it safe is what it refuses to do:
///
/// - It never looks an element up by ID. The signature must be a direct child
///   of the element it signs and its one `Reference` must name that element's
///   own `ID`. The element the application reads afterwards is therefore the
///   element whose bytes were digested -- the defence against every signature
///   wrapping attack, which all work by making those two different.
/// - The key is never taken from the message. `KeyInfo` is ignored; only the
///   certificates the registration was configured with are tried.
/// - Transforms are enveloped-signature and exclusive C14N, in either order,
///   and nothing else: no XPath, no XSLT, no inclusive C14N, no comments.
/// - SHA-1, for the digest or the signature, only when the registration opts
///   in for an identity provider that cannot do better.
final class XmlSignature {
    static final String DSIG = "http://www.w3.org/2000/09/xmldsig#";
    static final String ENVELOPED = DSIG + "enveloped-signature";
    static final String RSA_SHA1 = DSIG + "rsa-sha1";
    static final String SHA1 = DSIG + "sha1";
    static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    static final String RSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";
    static final String RSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";
    static final String ECDSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";
    static final String ECDSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384";
    static final String SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";
    static final String SHA384 = "http://www.w3.org/2001/04/xmldsig-more#sha384";
    static final String SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512";

    private XmlSignature() {
    }

    /// The `Signature` child of `signed`, or null when it has none.
    static Xml.Element signatureOf(Xml.Element signed) {
        return signed.child(DSIG, "Signature");
    }

    /// Checks `signature`, a child of `signed`, against `keys`.
    ///
    /// @param keys SubjectPublicKeyInfo DER, any of which may have signed it
    /// @throws Saml2AuthenticationException when it does not verify, for any reason
    static void verify(Xml.Element signed, Xml.Element signature, List<byte[]> keys,
                       boolean allowSha1) {
        if (signature.parent != signed) { // NOPMD CompareObjectsWithEquals - the same node, not an equal one
            throw refused("The signature is not enveloped in the element it signs");
        }
        String id = signed.attribute("ID");
        if (id == null || id.length() == 0) {
            throw refused("The signed element has no ID");
        }
        Xml.Element signedInfo = required(signature, "SignedInfo");
        Xml.Element c14nMethod = required(signedInfo, "CanonicalizationMethod");
        if (!ExcC14n.ALGORITHM.equals(c14nMethod.attribute("Algorithm"))) {
            throw refused("SignedInfo is canonicalized with "
                    + c14nMethod.attribute("Algorithm") + "; only exclusive C14N is accepted");
        }
        String sigAlg = required(signedInfo, "SignatureMethod").attribute("Algorithm");
        String cryptoAlg = signatureAlgorithm(sigAlg, allowSha1);
        List<Xml.Element> references = signedInfo.children(DSIG, "Reference");
        if (references.size() != 1) {
            throw refused("A SAML signature has exactly one reference, this one has "
                    + references.size());
        }
        Xml.Element reference = references.get(0);
        if (!("#" + id).equals(reference.attribute("URI"))) {
            throw refused("The signature references another element than the one it is in");
        }
        Set<String> inclusive = Collections.<String>emptySet();
        boolean enveloped = false;
        boolean c14n = false;
        Xml.Element transforms = reference.child(DSIG, "Transforms");
        if (transforms != null) {
            List<Xml.Element> list = transforms.elements();
            for (Xml.Element t : list) {
                String alg = t.attribute("Algorithm");
                if (!t.is(DSIG, "Transform")) {
                    throw refused("Unexpected " + t.localName + " in Transforms");
                }
                if (ENVELOPED.equals(alg) && !enveloped) {
                    enveloped = true;
                } else if (ExcC14n.ALGORITHM.equals(alg) && !c14n) {
                    c14n = true;
                    inclusive = prefixList(t);
                } else {
                    throw refused("The transform " + alg + " is not accepted");
                }
            }
        }
        if (!enveloped) {
            // Without it the digest would be over bytes that include the
            // signature, which no signer produces.
            throw refused("The reference has no enveloped-signature transform");
        }
        byte[] expected = base64(required(reference, "DigestValue").textOnly());
        String digestAlg = required(reference, "DigestMethod").attribute("Algorithm");
        byte[] actual = digest(digestAlg, ExcC14n.canonicalize(signed, signature, inclusive),
                allowSha1);
        if (expected == null || !Crypto.equalsConstantTime(expected, actual)) {
            throw refused("The digest of the signed element does not match");
        }
        byte[] signedBytes = ExcC14n.canonicalize(signedInfo, null, prefixList(c14nMethod));
        byte[] value = base64(required(signature, "SignatureValue").textOnly());
        if (value == null) {
            throw refused("The signature value is not base64");
        }
        if (keys.isEmpty()) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                    "No verification certificate is configured for the identity provider");
        }
        for (byte[] key : keys) {
            if (verifyWith(cryptoAlg, key, signedBytes, value)) {
                return;
            }
        }
        throw refused("The signature does not verify with any of the identity provider's keys");
    }

    /// Signs the element of `document` whose `ID` is `id`, with an enveloped
    /// RSA-SHA256 signature put where `document` has the comment
    /// `<!--SIGNATURE-->` -- which must be inside that element, where its schema
    /// allows a signature (after `Issuer`).
    ///
    /// @param privateKey an RSA key, PKCS#8 DER
    /// @return the signed document
    static String sign(String document, String id, byte[] privateKey) throws IOException {
        int at = document.indexOf(SIGNATURE_MARK);
        if (at < 0) {
            throw new IOException("The document has no signature placeholder");
        }
        Xml.Element root = Xml.parse(document);
        Xml.Element target = byId(root, id);
        if (target == null) {
            throw new IOException("No element has the ID " + id);
        }
        // The placeholder is a comment, so the tree has no trace of it and this
        // is the digest of the element as it will be without its signature.
        String digest = Base64.encode(Crypto.sha256(ExcC14n.canonicalize(target, null,
                Collections.<String>emptySet())));
        String signedInfo = "<ds:SignedInfo xmlns:ds=\"" + DSIG + "\">"
                + "<ds:CanonicalizationMethod Algorithm=\"" + ExcC14n.ALGORITHM + "\"></ds:CanonicalizationMethod>"
                + "<ds:SignatureMethod Algorithm=\"" + RSA_SHA256 + "\"></ds:SignatureMethod>"
                + "<ds:Reference URI=\"#" + Xml.escape(id) + "\"><ds:Transforms>"
                + "<ds:Transform Algorithm=\"" + ENVELOPED + "\"></ds:Transform>"
                + "<ds:Transform Algorithm=\"" + ExcC14n.ALGORITHM + "\"></ds:Transform>"
                + "</ds:Transforms><ds:DigestMethod Algorithm=\"" + SHA256 + "\"></ds:DigestMethod>"
                + "<ds:DigestValue>" + digest + "</ds:DigestValue></ds:Reference></ds:SignedInfo>";
        byte[] canonical = ExcC14n.canonicalize(Xml.parse(signedInfo), null,
                Collections.<String>emptySet());
        String value = Base64.encode(Crypto.sign(Crypto.RS256, privateKey, canonical));
        String signature = "<ds:Signature xmlns:ds=\"" + DSIG + "\">"
                // Inside Signature the ds prefix is declared already.
                + signedInfo.replace("<ds:SignedInfo xmlns:ds=\"" + DSIG + "\">", "<ds:SignedInfo>")
                + "<ds:SignatureValue>" + value + "</ds:SignatureValue></ds:Signature>";
        return document.substring(0, at) + signature
                + document.substring(at + SIGNATURE_MARK.length());
    }

    /// Where [#sign] puts the signature.
    static final String SIGNATURE_MARK = "<!--SIGNATURE-->";

    private static Xml.Element byId(Xml.Element e, String id) {
        if (id.equals(e.attribute("ID"))) {
            return e;
        }
        for (Xml.Element child : e.elements()) {
            Xml.Element found = byId(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    static boolean verifyWith(String alg, byte[] key, byte[] data, byte[] value) {
        try {
            byte[] sig = value;
            if (alg.startsWith("ES")) {
                // XML Signature writes r and s side by side; the runtime takes DER.
                if (!Der.EC.equals(Der.publicKeyType(key))) {
                    return false;
                }
                sig = Der.ecdsaJoseToDer(value);
            } else if (!Der.RSA.equals(Der.publicKeyType(key))) {
                return false;
            }
            return Crypto.verify(alg, key, data, sig);
        } catch (IOException err) {
            // A key of another kind or curve than the algorithm: not this key.
            return false;
        }
    }

    static String signatureAlgorithm(String uri, boolean allowSha1) {
        if (RSA_SHA256.equals(uri)) {
            return Crypto.RS256;
        }
        if (RSA_SHA384.equals(uri)) {
            return Crypto.RS384;
        }
        if (RSA_SHA512.equals(uri)) {
            return Crypto.RS512;
        }
        if (ECDSA_SHA256.equals(uri)) {
            return Crypto.ES256;
        }
        if (ECDSA_SHA384.equals(uri)) {
            return Crypto.ES384;
        }
        if (RSA_SHA1.equals(uri) && allowSha1) {
            return Crypto.RS1;
        }
        throw refused("The signature algorithm " + uri + " is not accepted");
    }

    static byte[] digest(String uri, byte[] data, boolean allowSha1) {
        if (SHA256.equals(uri)) {
            return Crypto.sha256(data);
        }
        if (SHA384.equals(uri)) {
            return Crypto.sha384(data);
        }
        if (SHA512.equals(uri)) {
            return Crypto.sha512(data);
        }
        if (SHA1.equals(uri) && allowSha1) {
            return Crypto.sha1(data);
        }
        throw refused("The digest algorithm " + uri + " is not accepted");
    }

    private static Set<String> prefixList(Xml.Element transform) {
        Xml.Element in = transform.child(ExcC14n.ALGORITHM, "InclusiveNamespaces");
        if (in == null) {
            return Collections.<String>emptySet();
        }
        Set<String> out = new HashSet<String>();
        String list = in.attribute("PrefixList");
        if (list != null) {
            int start = 0;
            for (int i = 0; i <= list.length(); i++) {
                if (i == list.length() || list.charAt(i) == ' ' || list.charAt(i) == '\t'
                        || list.charAt(i) == '\n') {
                    if (i > start) {
                        out.add(list.substring(start, i));
                    }
                    start = i + 1;
                }
            }
        }
        return out;
    }

    /// Base64 with the line breaks and spaces XML writers put in it removed.
    static byte[] base64(String text) {
        StringBuilder b = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                b.append(c);
            }
        }
        return Base64.decode(b.toString());
    }

    private static Xml.Element required(Xml.Element parent, String name) {
        Xml.Element e = parent.child(DSIG, name);
        if (e == null) {
            throw refused("The signature has no " + name);
        }
        return e;
    }

    private static Saml2AuthenticationException refused(String message) {
        return new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_SIGNATURE, message);
    }
}
