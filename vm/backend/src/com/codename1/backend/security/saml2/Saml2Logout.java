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
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.pki.Certificate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/// SAML Single Logout, front channel: the `LogoutRequest` and
/// `LogoutResponse` this server sends, and the checks on the ones it receives.
///
/// Every logout message is signed both ways. One this server sends is signed
/// with the registration's signing key, so a registration without one cannot
/// take part in Single Logout; one it receives must carry a signature that
/// verifies under the identity provider's certificates -- in the query for the
/// HTTP-Redirect binding, enveloped in the XML for HTTP-POST. An unsigned
/// logout request is refused, since anyone could otherwise end anyone's
/// session.
///
/// SOAP (back channel) logout is not supported, as it is not by Spring
/// Security.
public final class Saml2Logout {
    /// `urn:oasis:names:tc:SAML:2.0:status:Success`.
    public static final String SUCCESS = Saml2ResponseValidator.SUCCESS;
    /// The status of an answer to a request that names someone else.
    public static final String REQUESTER = "urn:oasis:names:tc:SAML:2.0:status:Requester";

    /// The largest message read: the encoded parameter, and its inflated XML.
    static final int MAX_ENCODED = 256 * 1024;
    static final int MAX_INFLATED = 512 * 1024;

    private Saml2Logout() {
    }

    /// A logout message this server received, after every check passed.
    public static final class Received {
        private final boolean request;
        private final String id;
        private final String inResponseTo;
        private final String nameId;
        private final List<String> sessionIndexes;
        private final String status;
        private final String relayState;

        Received(boolean request, String id, String inResponseTo, String nameId,
                 List<String> sessionIndexes, String status, String relayState) {
            this.request = request;
            this.id = id;
            this.inResponseTo = inResponseTo;
            this.nameId = nameId;
            this.sessionIndexes = Collections.unmodifiableList(sessionIndexes);
            this.status = status;
            this.relayState = relayState;
        }

        /// True for a `LogoutRequest`, false for a `LogoutResponse`.
        public boolean isRequest() {
            return request;
        }

        public String getId() {
            return id;
        }

        /// A response's `InResponseTo`; null for a request.
        public String getInResponseTo() {
            return inResponseTo;
        }

        /// A request's NameID; null for a response.
        public String getNameId() {
            return nameId;
        }

        /// A request's session indexes; empty when it names none, which means
        /// every session of that user.
        public List<String> getSessionIndexes() {
            return sessionIndexes;
        }

        /// A response's status code; null for a request.
        public String getStatus() {
            return status;
        }

        public String getRelayState() {
            return relayState;
        }
    }

    /// The `LogoutRequest` that ends `principal`'s session at the identity
    /// provider.
    ///
    /// @param entityId this server's entity ID, resolved
    /// @throws IllegalStateException when the registration has no logout
    /// address or no signing key
    public static Saml2Message request(RelyingPartyRegistration registration,
            Saml2AuthenticatedPrincipal principal, String entityId, String relayState, long now)
            throws IOException {
        String destination = registration.getAssertingPartySingleLogoutServiceLocation();
        byte[] key = registration.getSigningKey();
        if (destination == null || key == null) {
            throw new IllegalStateException(registration.getRegistrationId() + ": Single "
                    + "Logout needs the identity provider's logout address and a signing key");
        }
        String id = "LRQ" + Saml2AuthenticationRequest.hex(Crypto.randomBytes(20));
        StringBuilder xml = new StringBuilder(768);
        xml.append("<samlp:LogoutRequest xmlns:samlp=\"").append(Saml2ResponseValidator.PROTOCOL)
                .append("\" xmlns:saml=\"").append(Saml2ResponseValidator.ASSERTION)
                .append("\" ID=\"").append(id).append("\" Version=\"2.0\" IssueInstant=\"")
                .append(Saml2AuthenticationRequest.instant(now))
                .append("\" Destination=\"").append(Xml.escape(destination))
                .append("\"><saml:Issuer>").append(Xml.escape(entityId)).append("</saml:Issuer>");
        boolean post = RelyingPartyRegistration.POST.equals(
                registration.getAssertingPartySingleLogoutServiceBinding());
        if (post) {
            xml.append(XmlSignature.SIGNATURE_MARK);
        }
        xml.append("<saml:NameID");
        attribute(xml, "Format", principal.getNameIdFormat());
        attribute(xml, "NameQualifier", principal.getNameQualifier());
        attribute(xml, "SPNameQualifier", principal.getSpNameQualifier());
        xml.append('>').append(Xml.escape(principal.getName())).append("</saml:NameID>");
        for (String index : principal.getSessionIndexes()) {
            xml.append("<samlp:SessionIndex>").append(Xml.escape(index))
                    .append("</samlp:SessionIndex>");
        }
        xml.append("</samlp:LogoutRequest>");
        return Saml2Message.encode("SAMLRequest", xml.toString(), id,
                registration.getAssertingPartySingleLogoutServiceBinding(), destination,
                relayState, key);
    }

    /// The `LogoutResponse` answering the identity provider's request `inResponseTo`.
    ///
    /// @param status [#SUCCESS], or another status code
    public static Saml2Message response(RelyingPartyRegistration registration,
            String inResponseTo, String status, String entityId, String relayState, long now)
            throws IOException {
        String destination = registration.getAssertingPartySingleLogoutServiceResponseLocation();
        byte[] key = registration.getSigningKey();
        if (destination == null || key == null) {
            throw new IllegalStateException(registration.getRegistrationId() + ": Single "
                    + "Logout needs the identity provider's logout address and a signing key");
        }
        String id = "LRS" + Saml2AuthenticationRequest.hex(Crypto.randomBytes(20));
        StringBuilder xml = new StringBuilder(640);
        xml.append("<samlp:LogoutResponse xmlns:samlp=\"").append(Saml2ResponseValidator.PROTOCOL)
                .append("\" xmlns:saml=\"").append(Saml2ResponseValidator.ASSERTION)
                .append("\" ID=\"").append(id).append("\" Version=\"2.0\" IssueInstant=\"")
                .append(Saml2AuthenticationRequest.instant(now))
                .append("\" Destination=\"").append(Xml.escape(destination))
                .append("\" InResponseTo=\"").append(Xml.escape(inResponseTo))
                .append("\"><saml:Issuer>").append(Xml.escape(entityId)).append("</saml:Issuer>");
        boolean post = RelyingPartyRegistration.POST.equals(
                registration.getAssertingPartySingleLogoutServiceBinding());
        if (post) {
            xml.append(XmlSignature.SIGNATURE_MARK);
        }
        xml.append("<samlp:Status><samlp:StatusCode Value=\"").append(Xml.escape(status))
                .append("\"/></samlp:Status></samlp:LogoutResponse>");
        return Saml2Message.encode("SAMLResponse", xml.toString(), id,
                registration.getAssertingPartySingleLogoutServiceBinding(), destination,
                relayState, key);
    }

    private static void attribute(StringBuilder xml, String name, String value) {
        if (value != null) {
            xml.append(' ').append(name).append("=\"").append(Xml.escape(value)).append('"');
        }
    }

    /// Reads and checks a logout message that arrived by HTTP-Redirect.
    ///
    /// @param rawQuery the query exactly as it arrived, still percent encoded:
    /// the signature is over those bytes
    /// @param sloLocation this server's logout address for the registration, resolved
    /// @throws Saml2AuthenticationException when it is refused
    public static Received readRedirect(RelyingPartyRegistration registration, String rawQuery,
            String sloLocation, long now) {
        String param = null;
        String value = null;
        String relayState = null;
        String sigAlg = null;
        String signature = null;
        int start = 0;
        while (start <= rawQuery.length()) {
            int end = rawQuery.indexOf('&', start);
            if (end < 0) {
                end = rawQuery.length();
            }
            String pair = rawQuery.substring(start, end);
            int eq = pair.indexOf('=');
            String name = eq < 0 ? pair : pair.substring(0, eq);
            String raw = eq < 0 ? "" : pair.substring(eq + 1);
            if ("SAMLRequest".equals(name) || "SAMLResponse".equals(name)) {
                if (param != null) {
                    throw refused("The query carries more than one SAML message");
                }
                param = name;
                value = raw;
            } else if ("RelayState".equals(name)) {
                relayState = once(relayState, raw, name);
            } else if ("SigAlg".equals(name)) {
                sigAlg = once(sigAlg, raw, name);
            } else if ("Signature".equals(name)) {
                signature = once(signature, raw, name);
            }
            start = end + 1;
        }
        if (param == null) {
            throw refused("The query carries no SAML message");
        }
        if (sigAlg == null || signature == null) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_SIGNATURE,
                    "The logout message is not signed");
        }
        if (value.length() > MAX_ENCODED) {
            throw refused("The logout message is too large");
        }
        StringBuilder signed = new StringBuilder(value.length() + 128);
        signed.append(param).append('=').append(value);
        if (relayState != null) {
            signed.append("&RelayState=").append(relayState);
        }
        signed.append("&SigAlg=").append(sigAlg);
        String alg = XmlSignature.signatureAlgorithm(OAuth2Parameters.decode(sigAlg),
                registration.isSha1Allowed());
        byte[] sig = Base64.decode(OAuth2Parameters.decode(signature));
        if (sig == null) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_SIGNATURE,
                    "The logout message's signature is not base64");
        }
        byte[] data = OAuth2Parameters.utf8(signed.toString());
        boolean verified = false;
        for (Certificate c : registration.getVerificationCertificates()) {
            if (XmlSignature.verifyWith(alg, c.getPublicKey(), data, sig)) {
                verified = true;
                break;
            }
        }
        if (!verified) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_SIGNATURE,
                    "The logout message's signature does not verify under the identity "
                    + "provider's certificates");
        }
        byte[] deflated = Base64.decode(strip(OAuth2Parameters.decode(value)));
        if (deflated == null) {
            throw refused("The logout message is not base64");
        }
        Xml.Element root = Xml.parse(inflate(deflated));
        return check(root, "SAMLRequest".equals(param), registration, sloLocation,
                relayState == null ? null : OAuth2Parameters.decode(relayState), now);
    }

    /// Reads and checks a logout message that arrived by HTTP-POST.
    ///
    /// @param parameter `SAMLRequest` or `SAMLResponse`
    /// @param value the posted parameter, base64
    /// @throws Saml2AuthenticationException when it is refused
    public static Received readPost(RelyingPartyRegistration registration, String parameter,
            String value, String relayState, String sloLocation, long now) {
        if (value.length() > MAX_ENCODED) {
            throw refused("The logout message is too large");
        }
        byte[] xml = Base64.decode(strip(value));
        if (xml == null) {
            throw refused("The logout message is not base64");
        }
        Xml.Element root = Xml.parse(xml);
        Xml.Element signature = XmlSignature.signatureOf(root);
        if (signature == null) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_SIGNATURE,
                    "The logout message is not signed");
        }
        List<byte[]> keys = new ArrayList<byte[]>();
        for (Certificate c : registration.getVerificationCertificates()) {
            keys.add(c.getPublicKey());
        }
        XmlSignature.verify(root, signature, keys, registration.isSha1Allowed());
        return check(root, "SAMLRequest".equals(parameter), registration, sloLocation,
                relayState, now);
    }

    private static Received check(Xml.Element root, boolean request,
            RelyingPartyRegistration registration, String sloLocation, String relayState,
            long now) {
        String name = request ? "LogoutRequest" : "LogoutResponse";
        if (!root.is(Saml2ResponseValidator.PROTOCOL, name)) {
            throw refused("Expected a " + name + ", not a " + root.localName);
        }
        if (!"2.0".equals(root.attribute("Version"))) {
            throw refused("Not a SAML 2.0 message");
        }
        String id = root.attribute("ID");
        if (id == null || id.length() == 0) {
            throw refused("The " + name + " has no ID");
        }
        Xml.Element issuer = root.child(Saml2ResponseValidator.ASSERTION, "Issuer");
        if (issuer == null || !registration.getAssertingPartyEntityId()
                .equals(issuer.textOnly().trim())) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_ISSUER,
                    "The " + name + " is not issued by "
                    + registration.getAssertingPartyEntityId());
        }
        String destination = root.attribute("Destination");
        if (destination == null || !destination.equals(sloLocation)) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_DESTINATION,
                    "The " + name + " is addressed to " + destination + ", not " + sloLocation);
        }
        long skew = registration.getClockSkewMillis();
        String issued = root.attribute("IssueInstant");
        if (issued == null) {
            throw refused("The " + name + " has no IssueInstant");
        }
        long at = Saml2ResponseValidator.time(issued);
        // A logout message is answered at once; one minted long ago, or in the
        // future, is a replay or a broken clock either way.
        if (at > now + skew || at < now - skew - 5 * 60 * 1000L) {
            throw refused("The " + name + " was issued at " + issued
                    + ", too far from now");
        }
        if (!request) {
            Xml.Element status = root.child(Saml2ResponseValidator.PROTOCOL, "Status");
            Xml.Element code = status == null ? null
                    : status.child(Saml2ResponseValidator.PROTOCOL, "StatusCode");
            String value = code == null ? null : code.attribute("Value");
            if (value == null) {
                throw refused("The LogoutResponse has no status");
            }
            return new Received(false, id, root.attribute("InResponseTo"), null,
                    new ArrayList<String>(), value, relayState);
        }
        String notOnOrAfter = root.attribute("NotOnOrAfter");
        if (notOnOrAfter != null && now - skew >= Saml2ResponseValidator.time(notOnOrAfter)) {
            throw refused("The LogoutRequest has expired");
        }
        Xml.Element nameId = root.child(Saml2ResponseValidator.ASSERTION, "NameID");
        if (nameId == null) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.SUBJECT_NOT_FOUND,
                    "The LogoutRequest names no one in the clear; an encrypted NameID is not "
                    + "supported");
        }
        List<String> indexes = new ArrayList<String>();
        for (Xml.Element s : root.children(Saml2ResponseValidator.PROTOCOL, "SessionIndex")) {
            indexes.add(s.textOnly().trim());
        }
        return new Received(true, id, null, nameId.textOnly().trim(), indexes, null,
                relayState);
    }

    private static String once(String current, String value, String name) {
        if (current != null) {
            throw refused("The query carries " + name + " twice");
        }
        return value;
    }

    private static String strip(String encoded) {
        StringBuilder b = new StringBuilder(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                b.append(c);
            }
        }
        return b.toString();
    }

    /// Raw DEFLATE, bounded: a small message that inflates past the cap is a bomb.
    private static byte[] inflate(byte[] deflated) {
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(deflated);
            ByteArrayOutputStream out = new ByteArrayOutputStream(deflated.length * 4);
            byte[] buffer = new byte[4096];
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw refused("The logout message is not complete DEFLATE data");
                }
                out.write(buffer, 0, n);
                if (out.size() > MAX_INFLATED) {
                    throw refused("The logout message inflates past "
                            + MAX_INFLATED + " bytes");
                }
            }
            return out.toByteArray();
        } catch (DataFormatException err) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.MALFORMED_RESPONSE_DATA,
                    "The logout message is not DEFLATE data", err);
        } finally {
            inflater.end();
        }
    }

    private static Saml2AuthenticationException refused(String message) {
        return Saml2AuthenticationException.malformed(message);
    }
}
