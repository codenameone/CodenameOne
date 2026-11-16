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

import com.codename1.backend.security.pki.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Decides whether a SAML 2.0 `Response` an identity provider posted signs a
/// user in, following the Web Browser SSO profile (SAML 2.0 profiles,
/// section 4.1.4) and Spring Security's defaults.
///
/// A response is accepted only when all of this holds:
///
/// - It is a `samlp:Response` of version 2.0 with a `Success` status,
///   addressed (`Destination`, when present) to this server's ACS address and
///   issued (when it names an issuer) by the registration's identity provider.
/// - It answers the AuthnRequest this browser sent (`InResponseTo`), or it is
///   unsolicited and answers nothing. One that names a request when none was
///   sent from this browser is refused.
/// - It holds exactly one `Assertion`. An `EncryptedAssertion` is refused as
///   unsupported: there is no RSA decryption in this runtime.
/// - The response, the assertion, or both, carry a signature that verifies
///   under [XmlSignature]'s rules with one of the registration's certificates.
///   A signature that is present and does not verify refuses the response even
///   when the other one does.
/// - The assertion is issued by the identity provider, its `Conditions` hold
///   now (within the clock skew), and every `AudienceRestriction` names this
///   server's entity ID.
/// - Its subject has a bearer `SubjectConfirmation` whose data is for this
///   ACS address, unexpired, and for this request when it names one.
/// - It has an `AuthnStatement`.
/// - Its ID has not been accepted before (a replay), for as long as it is valid.
///
/// The replay cache is per process. Behind a load balancer the same assertion
/// could be accepted once by each server: keep the assertion lifetime short at
/// the identity provider, or route a browser to one server.
public final class Saml2ResponseValidator {
    static final String PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";
    static final String ASSERTION = "urn:oasis:names:tc:SAML:2.0:assertion";
    static final String SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";
    static final String BEARER = "urn:oasis:names:tc:SAML:2.0:cm:bearer";

    /// The most assertion IDs kept against replay.
    static final int REPLAY_CAPACITY = 100000;

    private final Map<String, Long> seen = new HashMap<String, Long>();

    /// Checks `response`, the decoded XML of a `SAMLResponse` parameter.
    ///
    /// @param acsLocation this server's ACS address for the registration, resolved
    /// @param entityId this server's entity ID for the registration, resolved
    /// @param inResponseTo the ID of the AuthnRequest this browser sent; null
    /// when it sent none
    /// @param now the time to judge validity at, in milliseconds
    /// @return who signed in
    /// @throws Saml2AuthenticationException when the response is refused
    public Saml2AuthenticatedPrincipal validate(byte[] response,
            RelyingPartyRegistration registration, String acsLocation, String entityId,
            String inResponseTo, long now) {
        Xml.Element root = Xml.parse(response);
        if (!root.is(PROTOCOL, "Response")) {
            throw Saml2AuthenticationException.malformed("Not a SAML Response but a "
                    + root.localName);
        }
        if (!"2.0".equals(root.attribute("Version"))) {
            throw Saml2AuthenticationException.malformed("Not a SAML 2.0 response");
        }
        String destination = root.attribute("Destination");
        if (destination != null && !destination.equals(acsLocation)) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_DESTINATION,
                    "The response is addressed to " + destination + ", not " + acsLocation);
        }
        String idp = registration.getAssertingPartyEntityId();
        Xml.Element issuer = root.child(ASSERTION, "Issuer");
        if (issuer != null && !idp.equals(issuer.textOnly().trim())) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_ISSUER,
                    "The response is issued by " + issuer.textOnly().trim());
        }
        Xml.Element status = root.child(PROTOCOL, "Status");
        Xml.Element code = status == null ? null : status.child(PROTOCOL, "StatusCode");
        String statusValue = code == null ? null : code.attribute("Value");
        if (!SUCCESS.equals(statusValue)) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_RESPONSE,
                    "The identity provider answered " + statusValue);
        }
        String answers = root.attribute("InResponseTo");
        checkInResponseTo(answers, inResponseTo);
        List<byte[]> keys = new ArrayList<byte[]>();
        for (Certificate c : registration.getVerificationCertificates()) {
            keys.add(c.getPublicKey());
        }
        // The response's own signature first: when there is one it covers an
        // encrypted assertion as the ciphertext, before anything is decrypted.
        boolean signed = false;
        Xml.Element responseSignature = XmlSignature.signatureOf(root);
        if (responseSignature != null) {
            XmlSignature.verify(root, responseSignature, keys, registration.isSha1Allowed());
            signed = true;
        }
        List<Xml.Element> assertions = root.children(ASSERTION, "Assertion");
        List<Xml.Element> encrypted = root.children(ASSERTION, "EncryptedAssertion");
        if (assertions.size() + encrypted.size() != 1) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_ASSERTION,
                    "A response must hold exactly one assertion, this one holds "
                    + (assertions.size() + encrypted.size()));
        }
        Xml.Element assertion = assertions.isEmpty()
                ? XmlEncryption.decrypt(encrypted.get(0), registration.getDecryptionKeys())
                : assertions.get(0);
        Xml.Element assertionSignature = XmlSignature.signatureOf(assertion);
        if (assertionSignature != null) {
            XmlSignature.verify(assertion, assertionSignature, keys,
                    registration.isSha1Allowed());
            signed = true;
        }
        if (!signed) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_SIGNATURE,
                    "Neither the response nor its assertion is signed");
        }
        return assertion(assertion, registration, acsLocation, entityId, inResponseTo, now);
    }

    private static void checkInResponseTo(String answers, String sent) {
        if (answers == null) {
            return;
        }
        if (sent == null) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_IN_RESPONSE_TO,
                    "The response answers a request this browser did not send");
        }
        if (!answers.equals(sent)) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_IN_RESPONSE_TO,
                    "The response answers another request than this browser's");
        }
    }

    private Saml2AuthenticatedPrincipal assertion(Xml.Element a,
            RelyingPartyRegistration registration, String acsLocation, String entityId,
            String inResponseTo, long now) {
        if (!"2.0".equals(a.attribute("Version"))) {
            throw invalid("Not a SAML 2.0 assertion");
        }
        String id = a.attribute("ID");
        if (id == null || id.length() == 0) {
            throw invalid("The assertion has no ID");
        }
        Xml.Element issuer = a.child(ASSERTION, "Issuer");
        if (issuer == null
                || !registration.getAssertingPartyEntityId().equals(issuer.textOnly().trim())) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_ISSUER,
                    "The assertion is not issued by " + registration.getAssertingPartyEntityId());
        }
        long skew = registration.getClockSkewMillis();
        long expires = Long.MAX_VALUE;
        Xml.Element conditions = a.child(ASSERTION, "Conditions");
        if (conditions != null) {
            String notBefore = conditions.attribute("NotBefore");
            if (notBefore != null && now + skew < time(notBefore)) {
                throw invalid("The assertion is not valid yet");
            }
            String notOnOrAfter = conditions.attribute("NotOnOrAfter");
            if (notOnOrAfter != null) {
                expires = time(notOnOrAfter);
                if (now - skew >= expires) {
                    throw invalid("The assertion has expired");
                }
            }
            for (Xml.Element restriction : conditions.children(ASSERTION, "AudienceRestriction")) {
                boolean ours = false;
                for (Xml.Element audience : restriction.children(ASSERTION, "Audience")) {
                    if (entityId.equals(audience.textOnly().trim())) {
                        ours = true;
                    }
                }
                if (!ours) {
                    throw invalid("The assertion is for another audience than " + entityId);
                }
            }
        }
        Xml.Element subject = a.child(ASSERTION, "Subject");
        Xml.Element nameId = subject == null ? null : subject.child(ASSERTION, "NameID");
        if (nameId == null || nameId.textOnly().trim().length() == 0) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.SUBJECT_NOT_FOUND,
                    "The assertion names no subject");
        }
        boolean confirmed = false;
        for (Xml.Element sc : subject.children(ASSERTION, "SubjectConfirmation")) {
            if (!BEARER.equals(sc.attribute("Method"))) {
                continue;
            }
            Xml.Element data = sc.child(ASSERTION, "SubjectConfirmationData");
            if (data == null) {
                continue;
            }
            String recipient = data.attribute("Recipient");
            String notOnOrAfter = data.attribute("NotOnOrAfter");
            String answers = data.attribute("InResponseTo");
            if (recipient == null || !recipient.equals(acsLocation) || notOnOrAfter == null) {
                continue;
            }
            long until = time(notOnOrAfter);
            if (now - skew >= until) {
                continue;
            }
            String notBefore = data.attribute("NotBefore");
            if (notBefore != null && now + skew < time(notBefore)) {
                continue;
            }
            if (answers != null && (inResponseTo == null || !answers.equals(inResponseTo))) {
                continue;
            }
            confirmed = true;
            expires = Math.min(expires, until);
        }
        if (!confirmed) {
            throw invalid("No bearer subject confirmation is for this server, now");
        }
        List<Xml.Element> authn = a.children(ASSERTION, "AuthnStatement");
        if (authn.isEmpty()) {
            throw invalid("The assertion has no AuthnStatement");
        }
        List<String> sessionIndexes = new ArrayList<String>();
        for (Xml.Element s : authn) {
            String index = s.attribute("SessionIndex");
            if (index != null) {
                sessionIndexes.add(index);
            }
        }
        Map<String, List<String>> attributes = new LinkedHashMap<String, List<String>>();
        for (Xml.Element statement : a.children(ASSERTION, "AttributeStatement")) {
            for (Xml.Element attribute : statement.children(ASSERTION, "Attribute")) {
                String name = attribute.attribute("Name");
                if (name == null) {
                    continue;
                }
                List<String> values = attributes.get(name);
                if (values == null) {
                    values = new ArrayList<String>();
                    attributes.put(name, values);
                }
                for (Xml.Element value : attribute.children(ASSERTION, "AttributeValue")) {
                    values.add(value.textOnly());
                }
            }
        }
        remember(id, expires == Long.MAX_VALUE ? now + 5 * 60 * 1000L : expires + skew, now);
        return new Saml2AuthenticatedPrincipal(nameId.textOnly().trim(),
                nameId.attribute("Format"), attributes, sessionIndexes,
                registration.getRegistrationId(), nameId.attribute("NameQualifier"),
                nameId.attribute("SPNameQualifier"));
    }

    /// Records `id` as used until `until`; refuses one already used.
    private void remember(String id, long until, long now) {
        synchronized (seen) {
            Long was = seen.get(id);
            if (was != null && was.longValue() > now) {
                throw invalid("The assertion " + id + " has been used already");
            }
            if (seen.size() >= REPLAY_CAPACITY) {
                for (Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator();
                        it.hasNext();) {
                    if (it.next().getValue().longValue() <= now) {
                        it.remove();
                    }
                }
                if (seen.size() >= REPLAY_CAPACITY) {
                    // Refusing a sign-in beats forgetting an ID that may come back.
                    throw new Saml2AuthenticationException(
                            Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                            "Too many unexpired assertions to remember");
                }
            }
            seen.put(id, Long.valueOf(until));
        }
    }

    private static Saml2AuthenticationException invalid(String message) {
        return new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_ASSERTION, message);
    }

    /// An `xs:dateTime` in milliseconds since the epoch. SAML requires UTC,
    /// but an explicit offset is accepted; a time with no zone is refused.
    static long time(String value) {
        String v = value.trim();
        try {
            if (v.length() < 20 || v.charAt(4) != '-' || v.charAt(7) != '-'
                    || v.charAt(10) != 'T' || v.charAt(13) != ':' || v.charAt(16) != ':') {
                throw new NumberFormatException();
            }
            int year = Integer.parseInt(v.substring(0, 4));
            int month = Integer.parseInt(v.substring(5, 7));
            int day = Integer.parseInt(v.substring(8, 10));
            int hour = Integer.parseInt(v.substring(11, 13));
            int minute = Integer.parseInt(v.substring(14, 16));
            int second = Integer.parseInt(v.substring(17, 19));
            int at = 19;
            long millis = 0;
            if (v.charAt(at) == '.') {
                int start = ++at;
                while (at < v.length() && v.charAt(at) >= '0' && v.charAt(at) <= '9') {
                    at++;
                }
                String frac = (v.substring(start, at) + "000").substring(0, 3);
                millis = Integer.parseInt(frac);
            }
            long offset;
            char z = v.charAt(at);
            if (z == 'Z' && at == v.length() - 1) {
                offset = 0;
            } else if ((z == '+' || z == '-') && v.length() == at + 6 && v.charAt(at + 3) == ':') {
                offset = (Integer.parseInt(v.substring(at + 1, at + 3)) * 60L
                        + Integer.parseInt(v.substring(at + 4, at + 6))) * 60000L;
                if (z == '-') {
                    offset = -offset;
                }
            } else {
                throw new NumberFormatException();
            }
            if (month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || minute > 59
                    || second > 60) {
                throw new NumberFormatException();
            }
            long days = daysFromCivil(year, month, day);
            return ((days * 24 + hour) * 60 + minute) * 60000L + second * 1000L + millis - offset;
        } catch (RuntimeException err) {
            throw Saml2AuthenticationException.malformed("Not a dateTime: " + value, err);
        }
    }

    /// Days since 1970-01-01 of a proleptic Gregorian date (Howard Hinnant's algorithm).
    private static long daysFromCivil(int year, int month, int day) {
        int y = month <= 2 ? year - 1 : year;
        int era = (y >= 0 ? y : y - 399) / 400;
        int yoe = y - era * 400;
        int doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097L + doe - 719468;
    }
}
