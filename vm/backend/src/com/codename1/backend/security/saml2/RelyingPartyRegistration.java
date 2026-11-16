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
import java.util.Collections;
import java.util.List;

/// One identity provider this server accepts SAML 2.0 sign-ins from, and how
/// this server presents itself to it.
///
/// ```java
/// RelyingPartyRegistration okta = RelyingPartyRegistration.withRegistrationId("okta")
///     .assertingPartyEntityId("http://www.okta.com/exk...")
///     .singleSignOnServiceLocation("https://corp.okta.com/app/.../sso/saml")
///     .verificationCertificate(Certificate.fromPem(oktaCertPem))
///     .build();
/// ```
///
/// or from the identity provider's metadata, with
/// [RelyingPartyRegistrations#fromMetadataLocation] or [RelyingPartyRegistrations#fromMetadata]. `{baseUrl}` and
/// `{registrationId}` in the two addresses of this side are filled in from the
/// request; give whole addresses in production, because `{baseUrl}` is read
/// from the `Host` the browser sent.
public final class RelyingPartyRegistration {
    /// The HTTP-Redirect binding.
    public static final String REDIRECT = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";
    /// The HTTP-POST binding.
    public static final String POST = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

    private final String registrationId;
    private final String entityId;
    private final String assertionConsumerServiceLocation;
    private final byte[] signingKey;
    private final Certificate signingCertificate;
    private final String nameIdFormat;
    private final String assertingPartyEntityId;
    private final String singleSignOnServiceLocation;
    private final String singleSignOnServiceBinding;
    private final boolean wantAuthnRequestsSigned;
    private final List<Certificate> verificationCertificates;
    private final boolean sha1Allowed;
    private final long clockSkewMillis;
    private final List<byte[]> decryptionKeys;
    private final List<Certificate> decryptionCertificates;
    private final String singleLogoutServiceLocation;
    private final String singleLogoutServiceResponseLocation;
    private final String assertingPartySingleLogoutServiceLocation;
    private final String assertingPartySingleLogoutServiceResponseLocation;
    private final String assertingPartySingleLogoutServiceBinding;

    private RelyingPartyRegistration(Builder b) {
        this.registrationId = b.registrationId;
        this.entityId = b.entityId;
        this.assertionConsumerServiceLocation = b.assertionConsumerServiceLocation;
        this.signingKey = b.signingKey;
        this.signingCertificate = b.signingCertificate;
        this.nameIdFormat = b.nameIdFormat;
        this.assertingPartyEntityId = b.assertingPartyEntityId;
        this.singleSignOnServiceLocation = b.singleSignOnServiceLocation;
        this.singleSignOnServiceBinding = b.singleSignOnServiceBinding;
        this.wantAuthnRequestsSigned = b.wantAuthnRequestsSigned;
        this.verificationCertificates = Collections.unmodifiableList(
                new ArrayList<Certificate>(b.verificationCertificates));
        this.sha1Allowed = b.sha1Allowed;
        this.clockSkewMillis = b.clockSkewMillis;
        List<byte[]> keys = new ArrayList<byte[]>();
        for (byte[] k : b.decryptionKeys) {
            keys.add(k.clone());
        }
        this.decryptionKeys = Collections.unmodifiableList(keys);
        this.decryptionCertificates = Collections.unmodifiableList(
                new ArrayList<Certificate>(b.decryptionCertificates));
        this.singleLogoutServiceLocation = b.singleLogoutServiceLocation;
        this.singleLogoutServiceResponseLocation = b.singleLogoutServiceResponseLocation;
        this.assertingPartySingleLogoutServiceLocation = b.apSloLocation;
        this.assertingPartySingleLogoutServiceResponseLocation = b.apSloResponseLocation;
        this.assertingPartySingleLogoutServiceBinding = b.apSloBinding;
    }

    public static Builder withRegistrationId(String registrationId) {
        return new Builder(registrationId);
    }

    /// A builder holding everything this registration holds, to change some of it.
    public Builder mutate() {
        Builder b = new Builder(registrationId);
        b.entityId = entityId;
        b.assertionConsumerServiceLocation = assertionConsumerServiceLocation;
        b.signingKey = signingKey;
        b.signingCertificate = signingCertificate;
        b.nameIdFormat = nameIdFormat;
        b.assertingPartyEntityId = assertingPartyEntityId;
        b.singleSignOnServiceLocation = singleSignOnServiceLocation;
        b.singleSignOnServiceBinding = singleSignOnServiceBinding;
        b.wantAuthnRequestsSigned = wantAuthnRequestsSigned;
        b.verificationCertificates.addAll(verificationCertificates);
        b.sha1Allowed = sha1Allowed;
        b.clockSkewMillis = clockSkewMillis;
        for (int i = 0; i < decryptionKeys.size(); i++) {
            b.decryptionCredential(decryptionKeys.get(i), decryptionCertificates.get(i));
        }
        b.singleLogoutServiceLocation = singleLogoutServiceLocation;
        b.singleLogoutServiceResponseLocation = singleLogoutServiceResponseLocation;
        b.apSloLocation = assertingPartySingleLogoutServiceLocation;
        b.apSloResponseLocation = assertingPartySingleLogoutServiceResponseLocation;
        b.apSloBinding = assertingPartySingleLogoutServiceBinding;
        return b;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    /// This server's entity ID, possibly a template.
    public String getEntityId() {
        return entityId;
    }

    /// Where the identity provider posts its response, possibly a template.
    public String getAssertionConsumerServiceLocation() {
        return assertionConsumerServiceLocation;
    }

    /// The key AuthnRequests are signed with, PKCS#8 DER; null when they are not.
    public byte[] getSigningKey() {
        return signingKey == null ? null : signingKey.clone();
    }

    /// The certificate of [#getSigningKey], published in this server's metadata.
    public Certificate getSigningCertificate() {
        return signingCertificate;
    }

    /// The NameID format asked for; null to let the identity provider choose.
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public String getAssertingPartyEntityId() {
        return assertingPartyEntityId;
    }

    public String getSingleSignOnServiceLocation() {
        return singleSignOnServiceLocation;
    }

    /// [#REDIRECT] or [#POST].
    public String getSingleSignOnServiceBinding() {
        return singleSignOnServiceBinding;
    }

    public boolean isWantAuthnRequestsSigned() {
        return wantAuthnRequestsSigned;
    }

    /// The certificates whose keys may sign a response; several during a rollover.
    public List<Certificate> getVerificationCertificates() {
        return verificationCertificates;
    }

    /// Whether SHA-1 signatures and digests are accepted from this provider.
    public boolean isSha1Allowed() {
        return sha1Allowed;
    }

    /// How far apart the two clocks may be when validity times are checked.
    public long getClockSkewMillis() {
        return clockSkewMillis;
    }

    /// The PKCS#8 keys an encrypted assertion may be encrypted to, in the
    /// order they are tried; empty when assertions must arrive in the clear.
    public List<byte[]> getDecryptionKeys() {
        List<byte[]> out = new ArrayList<byte[]>(decryptionKeys.size());
        for (byte[] k : decryptionKeys) {
            out.add(k.clone());
        }
        return out;
    }

    /// The certificates of [#getDecryptionKeys], published in this server's
    /// metadata for the identity provider to encrypt to.
    public List<Certificate> getDecryptionCertificates() {
        return decryptionCertificates;
    }

    /// Where the identity provider sends logout requests and responses to this
    /// server, possibly a template.
    public String getSingleLogoutServiceLocation() {
        return singleLogoutServiceLocation;
    }

    /// Where the identity provider sends its logout responses, possibly a template.
    public String getSingleLogoutServiceResponseLocation() {
        return singleLogoutServiceResponseLocation;
    }

    /// The identity provider's logout address; null when it has none, and a
    /// sign-out then ends the session here alone.
    public String getAssertingPartySingleLogoutServiceLocation() {
        return assertingPartySingleLogoutServiceLocation;
    }

    /// Where this server sends its answer to the identity provider's logout
    /// request: the provider's `ResponseLocation`, or its logout address.
    public String getAssertingPartySingleLogoutServiceResponseLocation() {
        return assertingPartySingleLogoutServiceResponseLocation != null
                ? assertingPartySingleLogoutServiceResponseLocation
                : assertingPartySingleLogoutServiceLocation;
    }

    /// [#REDIRECT], the default, or [#POST].
    public String getAssertingPartySingleLogoutServiceBinding() {
        return assertingPartySingleLogoutServiceBinding;
    }

    /// Builds a [RelyingPartyRegistration].
    public static final class Builder {
        private final String registrationId;
        private String entityId = "{baseUrl}/saml2/service-provider-metadata/{registrationId}";
        private String assertionConsumerServiceLocation = "{baseUrl}/login/saml2/sso/{registrationId}";
        private byte[] signingKey;
        private Certificate signingCertificate;
        private String nameIdFormat;
        private String assertingPartyEntityId;
        private String singleSignOnServiceLocation;
        private String singleSignOnServiceBinding = REDIRECT;
        private boolean wantAuthnRequestsSigned;
        private final List<Certificate> verificationCertificates = new ArrayList<Certificate>();
        private boolean sha1Allowed;
        private long clockSkewMillis = 3 * 60 * 1000L;
        private final List<byte[]> decryptionKeys = new ArrayList<byte[]>();
        private final List<Certificate> decryptionCertificates = new ArrayList<Certificate>();
        private String singleLogoutServiceLocation = "{baseUrl}/logout/saml2/slo/{registrationId}";
        private String singleLogoutServiceResponseLocation = "{baseUrl}/logout/saml2/slo/{registrationId}";
        private String apSloLocation;
        private String apSloResponseLocation;
        private String apSloBinding = REDIRECT;

        private Builder(String registrationId) {
            if (registrationId == null || registrationId.length() == 0) {
                throw new IllegalArgumentException("registrationId cannot be empty");
            }
            for (int i = 0; i < registrationId.length(); i++) {
                char c = registrationId.charAt(i);
                if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                        || c == '-' || c == '_' || c == '.')) {
                    throw new IllegalArgumentException("registrationId " + registrationId
                            + " may hold only letters, digits, '-', '_' and '.'");
                }
            }
            this.registrationId = registrationId;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder assertionConsumerServiceLocation(String location) {
            this.assertionConsumerServiceLocation = location;
            return this;
        }

        /// Signs AuthnRequests with `privateKey` (PKCS#8 DER), and publishes
        /// `certificate` in this server's metadata.
        public Builder signingCredential(byte[] privateKey, Certificate certificate) {
            this.signingKey = privateKey == null ? null : privateKey.clone();
            this.signingCertificate = certificate;
            return this;
        }

        public Builder nameIdFormat(String nameIdFormat) {
            this.nameIdFormat = nameIdFormat;
            return this;
        }

        public Builder assertingPartyEntityId(String entityId) {
            this.assertingPartyEntityId = entityId;
            return this;
        }

        public Builder singleSignOnServiceLocation(String location) {
            this.singleSignOnServiceLocation = location;
            return this;
        }

        /// [#REDIRECT], the default, or [#POST].
        public Builder singleSignOnServiceBinding(String binding) {
            if (!REDIRECT.equals(binding) && !POST.equals(binding)) {
                throw new IllegalArgumentException("Unknown binding " + binding);
            }
            this.singleSignOnServiceBinding = binding;
            return this;
        }

        public Builder wantAuthnRequestsSigned(boolean want) {
            this.wantAuthnRequestsSigned = want;
            return this;
        }

        public Builder verificationCertificate(Certificate certificate) {
            if (certificate == null) {
                throw new IllegalArgumentException("certificate cannot be null");
            }
            this.verificationCertificates.add(certificate);
            return this;
        }

        /// Accepts SHA-1 signatures and digests. Off unless set: only for an
        /// identity provider that cannot sign with SHA-256, such as an old ADFS.
        public Builder sha1Allowed(boolean allowed) {
            this.sha1Allowed = allowed;
            return this;
        }

        /// Accepts assertions encrypted to `certificate`'s key, decrypting them
        /// with `privateKey` (PKCS#8 DER). Call again during a key rollover:
        /// each key is tried in turn, and every certificate is published.
        public Builder decryptionCredential(byte[] privateKey, Certificate certificate) {
            if (privateKey == null || certificate == null) {
                throw new IllegalArgumentException("A decryption credential needs a key and its "
                        + "certificate");
            }
            this.decryptionKeys.add(privateKey.clone());
            this.decryptionCertificates.add(certificate);
            return this;
        }

        public Builder singleLogoutServiceLocation(String location) {
            this.singleLogoutServiceLocation = location;
            return this;
        }

        public Builder singleLogoutServiceResponseLocation(String location) {
            this.singleLogoutServiceResponseLocation = location;
            return this;
        }

        /// The identity provider's logout address; leave unset for an identity
        /// provider without Single Logout.
        public Builder assertingPartySingleLogoutServiceLocation(String location) {
            this.apSloLocation = location;
            return this;
        }

        public Builder assertingPartySingleLogoutServiceResponseLocation(String location) {
            this.apSloResponseLocation = location;
            return this;
        }

        /// [#REDIRECT], the default, or [#POST].
        public Builder assertingPartySingleLogoutServiceBinding(String binding) {
            if (!REDIRECT.equals(binding) && !POST.equals(binding)) {
                throw new IllegalArgumentException("Unknown binding " + binding);
            }
            this.apSloBinding = binding;
            return this;
        }

        public Builder clockSkewMillis(long millis) {
            if (millis < 0) {
                throw new IllegalArgumentException("clockSkewMillis cannot be negative");
            }
            this.clockSkewMillis = millis;
            return this;
        }

        public RelyingPartyRegistration build() {
            if (assertingPartyEntityId == null || assertingPartyEntityId.length() == 0) {
                throw new IllegalStateException(registrationId
                        + ": the identity provider's entity ID is required");
            }
            if (singleSignOnServiceLocation == null) {
                throw new IllegalStateException(registrationId
                        + ": the identity provider's single sign-on address is required");
            }
            if (verificationCertificates.isEmpty()) {
                throw new IllegalStateException(registrationId + ": at least one verification "
                        + "certificate is required; a response nobody can verify is never accepted");
            }
            if (wantAuthnRequestsSigned && signingKey == null) {
                throw new IllegalStateException(registrationId + ": the identity provider wants "
                        + "signed requests, and no signing credential is configured");
            }
            return new RelyingPartyRegistration(this);
        }
    }
}
