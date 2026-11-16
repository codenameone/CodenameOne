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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

/// A SAML protocol message on its way to the identity provider -- an
/// AuthnRequest, a LogoutRequest or a LogoutResponse -- encoded for its
/// binding, and how the browser is sent there.
///
/// With a signing key, the HTTP-Redirect binding signs the query (section
/// 3.4.4.1 of the bindings) and the HTTP-POST binding signs the XML.
public final class Saml2Message {
    private final String id;
    private final String binding;
    private final String location;
    private final String parameter;
    private final String value;
    private final String relayState;
    private final String sigAlg;
    private final String signature;

    private Saml2Message(String id, String binding, String location, String parameter,
            String value, String relayState, String sigAlg, String signature) {
        this.id = id;
        this.binding = binding;
        this.location = location;
        this.parameter = parameter;
        this.value = value;
        this.relayState = relayState;
        this.sigAlg = sigAlg;
        this.signature = signature;
    }

    /// Encodes `xml`, whose root has the ID `id`.
    ///
    /// @param parameter `SAMLRequest` or `SAMLResponse`
    /// @param xml the message; for the POST binding with a key it holds
    /// [XmlSignature#SIGNATURE_MARK] where the signature goes
    /// @param key the PKCS#8 key to sign with; null to send it unsigned
    static Saml2Message encode(String parameter, String xml, String id, String binding,
            String location, String relayState, byte[] key) throws IOException {
        if (RelyingPartyRegistration.POST.equals(binding)) {
            String document = key != null ? XmlSignature.sign(xml, id, key) : xml;
            return new Saml2Message(id, binding, location, parameter,
                    Base64.encode(OAuth2Parameters.utf8(document)), relayState, null, null);
        }
        String encoded = Base64.encode(deflate(OAuth2Parameters.utf8(xml)));
        if (key == null) {
            return new Saml2Message(id, RelyingPartyRegistration.REDIRECT, location, parameter,
                    encoded, relayState, null, null);
        }
        String alg = XmlSignature.RSA_SHA256;
        StringBuilder signed = new StringBuilder();
        signed.append(parameter).append('=').append(OAuth2Parameters.encode(encoded));
        if (relayState != null) {
            signed.append("&RelayState=").append(OAuth2Parameters.encode(relayState));
        }
        signed.append("&SigAlg=").append(OAuth2Parameters.encode(alg));
        String sig = Base64.encode(Crypto.sign(Crypto.RS256, key,
                OAuth2Parameters.utf8(signed.toString())));
        return new Saml2Message(id, RelyingPartyRegistration.REDIRECT, location, parameter,
                encoded, relayState, alg, sig);
    }

    /// The message's ID, which an answer must name in `InResponseTo`.
    public String getId() {
        return id;
    }

    /// [RelyingPartyRegistration#REDIRECT] or [RelyingPartyRegistration#POST].
    public String getBinding() {
        return binding;
    }

    public String getLocation() {
        return location;
    }

    /// `SAMLRequest` or `SAMLResponse`.
    public String getParameterName() {
        return parameter;
    }

    /// The parameter's value: deflated and base64 for the redirect binding,
    /// base64 alone for the POST binding.
    public String getValue() {
        return value;
    }

    public String getRelayState() {
        return relayState;
    }

    /// The address to send the browser to, for the redirect binding.
    public String getRedirectUrl() {
        StringBuilder url = new StringBuilder(location);
        url.append(location.indexOf('?') < 0 ? '?' : '&');
        url.append(parameter).append('=').append(OAuth2Parameters.encode(value));
        if (relayState != null) {
            url.append("&RelayState=").append(OAuth2Parameters.encode(relayState));
        }
        if (signature != null) {
            url.append("&SigAlg=").append(OAuth2Parameters.encode(sigAlg))
                    .append("&Signature=").append(OAuth2Parameters.encode(signature));
        }
        return url.toString();
    }

    /// A page that posts the message to the identity provider, for the POST binding.
    public String getPostPage() {
        StringBuilder page = new StringBuilder(1024);
        page.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head><meta charset=\"utf-8\">")
                .append("<title>Continuing</title></head>\n")
                .append("<body onload=\"document.forms[0].submit()\">\n<form method=\"post\" action=\"")
                .append(Xml.escape(location)).append("\">\n")
                .append("<input type=\"hidden\" name=\"").append(parameter).append("\" value=\"")
                .append(Xml.escape(value)).append("\">\n");
        if (relayState != null) {
            page.append("<input type=\"hidden\" name=\"RelayState\" value=\"")
                    .append(Xml.escape(relayState)).append("\">\n");
        }
        page.append("<noscript><button type=\"submit\">Continue</button></noscript>\n")
                .append("</form>\n</body>\n</html>\n");
        return page.toString();
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int n = deflater.deflate(buffer);
            out.write(buffer, 0, n);
        }
        deflater.end();
        return out.toByteArray();
    }
}
