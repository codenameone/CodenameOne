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

import com.codename1.backend.Crypto;
import java.io.IOException;

/// An AuthnRequest on its way to the identity provider: the request itself,
/// encoded for its binding, and how the browser is sent there.
public final class Saml2AuthenticationRequest {
    private final Saml2Message message;

    private Saml2AuthenticationRequest(Saml2Message message) {
        this.message = message;
    }

    /// Makes the AuthnRequest for `registration`.
    ///
    /// @param acsLocation this server's ACS address, resolved
    /// @param entityId this server's entity ID, resolved
    /// @param relayState what the identity provider hands back unchanged
    /// @param now the issue instant, in milliseconds
    public static Saml2AuthenticationRequest create(RelyingPartyRegistration registration,
            String acsLocation, String entityId, String relayState, long now) throws IOException {
        String id = "ARQ" + hex(Crypto.randomBytes(20));
        String destination = registration.getSingleSignOnServiceLocation();
        boolean post = RelyingPartyRegistration.POST.equals(
                registration.getSingleSignOnServiceBinding());
        byte[] key = registration.getSigningKey();
        StringBuilder xml = new StringBuilder(512);
        xml.append("<samlp:AuthnRequest xmlns:samlp=\"").append(Saml2ResponseValidator.PROTOCOL)
                .append("\" xmlns:saml=\"").append(Saml2ResponseValidator.ASSERTION)
                .append("\" ID=\"").append(id).append("\" Version=\"2.0\" IssueInstant=\"")
                .append(instant(now)).append("\" Destination=\"").append(Xml.escape(destination))
                .append("\" AssertionConsumerServiceURL=\"").append(Xml.escape(acsLocation))
                .append("\" ProtocolBinding=\"").append(RelyingPartyRegistration.POST)
                .append("\"><saml:Issuer>").append(Xml.escape(entityId)).append("</saml:Issuer>");
        if (post && key != null) {
            xml.append(XmlSignature.SIGNATURE_MARK);
        }
        if (registration.getNameIdFormat() != null) {
            xml.append("<samlp:NameIDPolicy Format=\"")
                    .append(Xml.escape(registration.getNameIdFormat()))
                    .append("\" AllowCreate=\"true\"/>");
        }
        xml.append("</samlp:AuthnRequest>");
        return new Saml2AuthenticationRequest(Saml2Message.encode("SAMLRequest", xml.toString(),
                id, registration.getSingleSignOnServiceBinding(), destination, relayState, key));
    }

    /// The request's ID, which the response must name in `InResponseTo`.
    public String getId() {
        return message.getId();
    }

    /// [RelyingPartyRegistration#REDIRECT] or [RelyingPartyRegistration#POST].
    public String getBinding() {
        return message.getBinding();
    }

    public String getLocation() {
        return message.getLocation();
    }

    /// The `SAMLRequest` parameter: deflated and base64 for the redirect
    /// binding, base64 alone for the POST binding.
    public String getSamlRequest() {
        return message.getValue();
    }

    public String getRelayState() {
        return message.getRelayState();
    }

    /// The address to send the browser to, for the redirect binding.
    public String getRedirectUrl() {
        return message.getRedirectUrl();
    }

    /// A page that posts the request to the identity provider, for the POST binding.
    public String getPostPage() {
        return message.getPostPage();
    }

    /// `millis` as an `xs:dateTime` in UTC, to the second.
    static String instant(long millis) {
        long seconds = Math.floorDiv(millis, 1000L);
        long days = Math.floorDiv(seconds, 86400L);
        long rest = seconds - days * 86400L;
        // Howard Hinnant's civil_from_days.
        long z = days + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp + (mp < 10 ? 3 : -9);
        if (m <= 2) {
            y++;
        }
        StringBuilder out = new StringBuilder(20);
        pad(out, y, 4).append('-');
        pad(out, m, 2).append('-');
        pad(out, d, 2).append('T');
        pad(out, rest / 3600, 2).append(':');
        pad(out, (rest / 60) % 60, 2).append(':');
        pad(out, rest % 60, 2).append('Z');
        return out.toString();
    }

    static String hex(byte[] bytes) {
        String digits = "0123456789abcdef";
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(digits.charAt((b >> 4) & 0xf)).append(digits.charAt(b & 0xf));
        }
        return out.toString();
    }

    private static StringBuilder pad(StringBuilder out, long value, int width) {
        String s = Long.toString(value);
        for (int i = s.length(); i < width; i++) {
            out.append('0');
        }
        return out.append(s);
    }
}
