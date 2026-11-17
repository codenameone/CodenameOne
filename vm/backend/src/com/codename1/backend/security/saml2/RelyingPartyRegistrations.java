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

import com.codename1.backend.Config;
import com.codename1.backend.Web;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.pki.Certificate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Makes [RelyingPartyRegistration]s from an identity provider's metadata and
/// from the configuration.
///
/// In `application.properties`, as Spring Boot spells them under
/// `spring.security.saml2.relyingparty`:
///
/// ```
/// cn1.security.saml2.relyingparty.registration.okta.assertingparty.metadata-uri=https://corp.okta.com/app/exk.../sso/saml/metadata
/// ```
///
/// or, without metadata:
///
/// ```
/// cn1.security.saml2.relyingparty.registration.adfs.assertingparty.entity-id=http://adfs.corp.example/adfs/services/trust
/// cn1.security.saml2.relyingparty.registration.adfs.assertingparty.singlesignon.url=https://adfs.corp.example/adfs/ls/
/// cn1.security.saml2.relyingparty.registration.adfs.assertingparty.singlesignon.binding=POST
/// cn1.security.saml2.relyingparty.registration.adfs.assertingparty.verification.certificate-location=/etc/app/adfs.crt
/// ```
///
/// Each registration may also set `entity-id`, `acs.location`,
/// `name-id-format`, `sha1-allowed`, `clock-skew-seconds`,
/// `assertingparty.singlesignon.sign-request`,
/// `signing.private-key-location` with `signing.certificate-location`,
/// `decryption.private-key-location` with `decryption.certificate-location`
/// (comma separated lists, in pairs, for a rollover),
/// `assertingparty.singlelogout.url`, `.response-url` and `.binding`, and
/// `singlelogout.url` and `singlelogout.response-url` for this side.
/// A setting beside `metadata-uri` overrides what the metadata says.
///
/// Metadata is trusted as it is fetched -- over HTTPS, or from a file the
/// operator put there -- and a signature on it is not checked, as Spring
/// Security does not by default. Its certificates are what every response
/// is then held to, so the address must be one only the identity provider
/// controls.
public final class RelyingPartyRegistrations {
    /// The prefix of every registration's settings.
    public static final String REGISTRATION = "cn1.security.saml2.relyingparty.registration.";

    static final String MD = "urn:oasis:names:tc:SAML:2.0:metadata";

    private RelyingPartyRegistrations() {
    }

    /// A builder filled in from the identity provider's metadata at `location`:
    /// an `https:` or `http:` address, or a file.
    public static RelyingPartyRegistration.Builder fromMetadataLocation(String location,
            String registrationId) throws IOException {
        String xml;
        if (location.startsWith("https://") || location.startsWith("http://")) {
            Web.Result result = Web.get(location);
            if (!result.isSuccess()) {
                throw new IOException("GET " + location + " answered " + result.getStatus());
            }
            xml = result.getBodyAsString();
        } else {
            xml = KeyFiles.read(location);
        }
        return fromMetadata(xml, registrationId);
    }

    /// A builder filled in from the identity provider's metadata document.
    ///
    /// The document is an `EntityDescriptor`, or an `EntitiesDescriptor`
    /// holding exactly one identity provider.
    public static RelyingPartyRegistration.Builder fromMetadata(String metadata,
            String registrationId) throws IOException {
        Xml.Element root;
        try {
            root = Xml.parse(metadata);
        } catch (Saml2AuthenticationException err) {
            throw new IOException("The metadata is not XML this server reads: "
                    + err.getMessage(), err);
        }
        Xml.Element entity = null;
        Xml.Element idp = null;
        List<Xml.Element> candidates = new ArrayList<Xml.Element>();
        if (root.is(MD, "EntityDescriptor")) {
            candidates.add(root);
        } else if (root.is(MD, "EntitiesDescriptor")) {
            candidates.addAll(root.children(MD, "EntityDescriptor"));
        } else {
            throw new IOException("The metadata is a " + root.localName
                    + ", not an EntityDescriptor");
        }
        for (Xml.Element candidate : candidates) {
            Xml.Element d = candidate.child(MD, "IDPSSODescriptor");
            if (d != null) {
                if (idp != null) {
                    throw new IOException("The metadata describes more than one identity "
                            + "provider; give the registration one of them");
                }
                entity = candidate;
                idp = d;
            }
        }
        if (idp == null) {
            throw new IOException("The metadata describes no identity provider");
        }
        RelyingPartyRegistration.Builder b = RelyingPartyRegistration
                .withRegistrationId(registrationId)
                .assertingPartyEntityId(entity.attribute("entityID"))
                .wantAuthnRequestsSigned("true".equals(idp.attribute("WantAuthnRequestsSigned")));
        String redirect = null;
        String post = null;
        for (Xml.Element sso : idp.children(MD, "SingleSignOnService")) {
            if (RelyingPartyRegistration.REDIRECT.equals(sso.attribute("Binding"))
                    && redirect == null) {
                redirect = sso.attribute("Location");
            } else if (RelyingPartyRegistration.POST.equals(sso.attribute("Binding"))
                    && post == null) {
                post = sso.attribute("Location");
            }
        }
        if (redirect != null) {
            b.singleSignOnServiceLocation(redirect).singleSignOnServiceBinding(
                    RelyingPartyRegistration.REDIRECT);
        } else if (post != null) {
            b.singleSignOnServiceLocation(post).singleSignOnServiceBinding(
                    RelyingPartyRegistration.POST);
        } else {
            throw new IOException("The identity provider offers no HTTP-Redirect or HTTP-POST "
                    + "single sign-on service");
        }
        String sloRedirect = null;
        String sloRedirectResponse = null;
        String sloPost = null;
        String sloPostResponse = null;
        for (Xml.Element slo : idp.children(MD, "SingleLogoutService")) {
            String binding = slo.attribute("Binding");
            if (RelyingPartyRegistration.REDIRECT.equals(binding) && sloRedirect == null) {
                sloRedirect = slo.attribute("Location");
                sloRedirectResponse = slo.attribute("ResponseLocation");
            } else if (RelyingPartyRegistration.POST.equals(binding) && sloPost == null) {
                sloPost = slo.attribute("Location");
                sloPostResponse = slo.attribute("ResponseLocation");
            }
        }
        if (sloRedirect != null) {
            b.assertingPartySingleLogoutServiceLocation(sloRedirect)
                    .assertingPartySingleLogoutServiceResponseLocation(sloRedirectResponse)
                    .assertingPartySingleLogoutServiceBinding(RelyingPartyRegistration.REDIRECT);
        } else if (sloPost != null) {
            b.assertingPartySingleLogoutServiceLocation(sloPost)
                    .assertingPartySingleLogoutServiceResponseLocation(sloPostResponse)
                    .assertingPartySingleLogoutServiceBinding(RelyingPartyRegistration.POST);
        }
        int keys = 0;
        for (Xml.Element kd : idp.children(MD, "KeyDescriptor")) {
            String use = kd.attribute("use");
            if (use != null && !"signing".equals(use)) {
                continue;
            }
            Xml.Element info = kd.child(XmlSignature.DSIG, "KeyInfo");
            Xml.Element data = info == null ? null : info.child(XmlSignature.DSIG, "X509Data");
            if (data == null) {
                continue;
            }
            for (Xml.Element cert : data.children(XmlSignature.DSIG, "X509Certificate")) {
                byte[] der = XmlSignature.base64(cert.textOnly());
                if (der == null) {
                    throw new IOException("A signing certificate in the metadata is not base64");
                }
                b.verificationCertificate(Certificate.decode(der));
                keys++;
            }
        }
        if (keys == 0) {
            throw new IOException("The metadata names no signing certificate");
        }
        return b;
    }

    /// The registrations the configuration declares; empty when it declares none.
    public static List<RelyingPartyRegistration> fromConfig(Config config) throws IOException {
        List<String> ids = new ArrayList<String>();
        for (Object key : config.keys()) {
            String name = String.valueOf(key);
            if (name.startsWith(REGISTRATION)) {
                int dot = name.indexOf('.', REGISTRATION.length());
                String id = dot < 0 ? null : name.substring(REGISTRATION.length(), dot);
                if (id != null && id.length() > 0 && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        List<RelyingPartyRegistration> out = new ArrayList<RelyingPartyRegistration>();
        for (String id : ids) {
            out.add(fromConfig(config, id));
        }
        return out;
    }

    private static RelyingPartyRegistration fromConfig(Config config, String id)
            throws IOException {
        String p = REGISTRATION + id + ".";
        String ap = p + "assertingparty.";
        String metadata = config.get(ap + "metadata-uri");
        RelyingPartyRegistration.Builder b = metadata != null
                ? fromMetadataLocation(metadata, id)
                : RelyingPartyRegistration.withRegistrationId(id);
        String v = config.get(ap + "entity-id");
        if (v != null) {
            b.assertingPartyEntityId(v);
        }
        v = config.get(ap + "singlesignon.url");
        if (v != null) {
            b.singleSignOnServiceLocation(v);
        }
        v = config.get(ap + "singlesignon.binding");
        if (v != null) {
            if ("POST".equalsIgnoreCase(v)) {
                b.singleSignOnServiceBinding(RelyingPartyRegistration.POST);
            } else if ("REDIRECT".equalsIgnoreCase(v)) {
                b.singleSignOnServiceBinding(RelyingPartyRegistration.REDIRECT);
            } else {
                throw new IOException(ap + "singlesignon.binding must be REDIRECT or POST, not " + v);
            }
        }
        v = config.get(ap + "singlesignon.sign-request");
        if (v != null) {
            b.wantAuthnRequestsSigned("true".equalsIgnoreCase(v));
        }
        v = config.get(ap + "singlelogout.url");
        if (v != null) {
            b.assertingPartySingleLogoutServiceLocation(v);
        }
        v = config.get(ap + "singlelogout.response-url");
        if (v != null) {
            b.assertingPartySingleLogoutServiceResponseLocation(v);
        }
        v = config.get(ap + "singlelogout.binding");
        if (v != null) {
            if ("POST".equalsIgnoreCase(v)) {
                b.assertingPartySingleLogoutServiceBinding(RelyingPartyRegistration.POST);
            } else if ("REDIRECT".equalsIgnoreCase(v)) {
                b.assertingPartySingleLogoutServiceBinding(RelyingPartyRegistration.REDIRECT);
            } else {
                throw new IOException(ap + "singlelogout.binding must be REDIRECT or POST, not " + v);
            }
        }
        v = config.get(ap + "verification.certificate-location");
        if (v != null) {
            for (String path : split(v)) {
                for (Certificate c : Certificate.listFromPem(KeyFiles.read(path))) {
                    b.verificationCertificate(c);
                }
            }
        }
        v = config.get(p + "entity-id");
        if (v != null) {
            b.entityId(v);
        }
        v = config.get(p + "acs.location");
        if (v != null) {
            b.assertionConsumerServiceLocation(v);
        }
        v = config.get(p + "name-id-format");
        if (v != null) {
            b.nameIdFormat(v);
        }
        v = config.get(p + "sha1-allowed");
        if (v != null) {
            b.sha1Allowed("true".equalsIgnoreCase(v));
        }
        v = config.get(p + "clock-skew-seconds");
        if (v != null) {
            try {
                b.clockSkewMillis(Long.parseLong(v.trim()) * 1000L);
            } catch (NumberFormatException err) {
                throw new IOException(p + "clock-skew-seconds is not a number: " + v, err);
            }
        }
        String key = config.get(p + "signing.private-key-location");
        String cert = config.get(p + "signing.certificate-location");
        if (key != null || cert != null) {
            if (key == null || cert == null) {
                throw new IOException(p + "signing needs both private-key-location and "
                        + "certificate-location");
            }
            b.signingCredential(KeyFiles.readPrivateKey(key),
                    Certificate.fromPem(KeyFiles.read(cert)));
        }
        v = config.get(p + "singlelogout.url");
        if (v != null) {
            b.singleLogoutServiceLocation(v);
        }
        v = config.get(p + "singlelogout.response-url");
        if (v != null) {
            b.singleLogoutServiceResponseLocation(v);
        }
        key = config.get(p + "decryption.private-key-location");
        cert = config.get(p + "decryption.certificate-location");
        if (key != null || cert != null) {
            if (key == null || cert == null) {
                throw new IOException(p + "decryption needs both private-key-location and "
                        + "certificate-location");
            }
            List<String> keys = split(key);
            List<String> certs = split(cert);
            if (keys.size() != certs.size()) {
                throw new IOException(p + "decryption names " + keys.size() + " keys and "
                        + certs.size() + " certificates; give them in pairs");
            }
            for (int i = 0; i < keys.size(); i++) {
                b.decryptionCredential(KeyFiles.readPrivateKey(keys.get(i)),
                        Certificate.fromPem(KeyFiles.read(certs.get(i))));
            }
        }
        try {
            return b.build();
        } catch (IllegalStateException err) {
            throw new IOException(err.getMessage(), err);
        }
    }

    private static List<String> split(String list) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= list.length(); i++) {
            if (i == list.length() || list.charAt(i) == ',') {
                String part = list.substring(start, i).trim();
                if (part.length() > 0) {
                    out.add(part);
                }
                start = i + 1;
            }
        }
        return out;
    }
}
