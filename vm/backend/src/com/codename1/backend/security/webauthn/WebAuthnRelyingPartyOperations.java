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
package com.codename1.backend.security.webauthn;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.security.Clock;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The two passkey ceremonies, as the relying party performs them: making the
/// options a client starts from, and verifying what the authenticator
/// answered. Registration follows section 7.1 of the WebAuthn specification
/// and sign-in section 7.2.
///
/// `http.webAuthn(...)` makes one and puts it behind the endpoints. An
/// application that serves the ceremonies itself uses it directly:
///
/// ```java
/// WebAuthnRelyingPartyOperations passkeys = new WebAuthnRelyingPartyOperations(
///         new PublicKeyCredentialRpEntity("example.com", "Example"),
///         Arrays.asList("https://example.com"), userEntities, credentials);
///
/// PublicKeyCredentialCreationOptions options =
///         passkeys.createPublicKeyCredentialCreationOptions("ada");
/// // ... keep options.toMap() for this user, send it, and with the answer:
/// CredentialRecord made = passkeys.registerCredential(options, answer, "Ada's phone");
/// ```
///
/// What this class does not do is keep a ceremony's options between its two
/// requests, give a challenge a lifetime, or see that it is used once: the
/// caller holds the options and hands them back. The endpoints of
/// `http.webAuthn(...)` keep them in the session, for five minutes, and take
/// them out before they look at the answer.
///
/// ## What is verified
///
/// Both ceremonies: that the client data is of the right ceremony, carries
/// this challenge, and names an origin among those allowed -- compared as
/// text, in full -- and no frame of another origin; that the authenticator
/// answered for this relying party; that the user was present, and verified
/// when the options required it; that the backup flags are possible.
///
/// Registration: that the credential's key is ES256 or RS256; the
/// attestation, which must be `none`, or `packed` signed by the credential's
/// own key -- any other is refused by name unless
/// [#setAllowUnverifiedAttestation]; and that no credential has this id.
///
/// Sign-in: that the credential is registered and, when the user said who
/// they are first, is one of theirs; that the user the answer names owns it;
/// the signature, over the authenticator data and the hash of the client
/// data; that the signature counter moved forward; that the credential is as
/// eligible for backup as when it was made.
///
/// A refusal is a [WebAuthnException] whose reason says which of these it was.
public final class WebAuthnRelyingPartyOperations {
    /// Told when a signature counter did not advance.
    public interface SignatureCounterListener {
        /// The counter `received` is not greater than the one in `record`,
        /// which means two authenticators hold the credential's private key --
        /// it was copied -- or one is broken. The sign-in has been refused.
        void signatureCounterRegressed(CredentialRecord record, long received);
    }

    /// A verified sign-in.
    public static final class Assertion {
        private final PublicKeyCredentialUserEntity user;
        private final CredentialRecord credential;
        private final boolean userVerified;

        Assertion(PublicKeyCredentialUserEntity user, CredentialRecord credential,
                  boolean userVerified) {
            this.user = user;
            this.credential = credential;
            this.userVerified = userVerified;
        }

        /// The user the credential belongs to.
        public PublicKeyCredentialUserEntity getUser() {
            return user;
        }

        /// The credential, as stored after this sign-in.
        public CredentialRecord getCredential() {
            return credential;
        }

        /// Whether the authenticator verified the user for this sign-in: a
        /// PIN, a fingerprint, a face -- a second factor beside holding it.
        public boolean isUserVerified() {
            return userVerified;
        }
    }

    private static final int MAX_CLIENT_DATA = 16384;

    private final PublicKeyCredentialRpEntity rp;
    private final Set<String> allowedOrigins;
    private final PublicKeyCredentialUserEntityRepository users;
    private final UserCredentialRepository credentials;
    private final byte[] rpIdHash;
    private String userVerification = "preferred";
    private String residentKey = "required";
    private String authenticatorAttachment;
    private long timeoutMillis = 300000;
    private boolean allowUnverifiedAttestation;
    private boolean allowCrossOrigin;
    private Clock clock = Clock.SYSTEM;
    private SignatureCounterListener counterListener;

    /// @param allowedOrigins the origins a ceremony may run on, each exactly as
    /// a client reports it: `https://example.com`, with a port when it is not
    /// the default and no path; and for an Android application,
    /// `android:apk-key-hash:` followed by the base64url SHA-256 of its signing
    /// certificate
    public WebAuthnRelyingPartyOperations(PublicKeyCredentialRpEntity rp,
            Collection<String> allowedOrigins, PublicKeyCredentialUserEntityRepository users,
            UserCredentialRepository credentials) {
        if (rp == null || users == null || credentials == null) {
            throw new IllegalArgumentException("The relying party, the user entities and the "
                    + "credentials are required");
        }
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed origin is required: the "
                    + "address the ceremonies run on, such as https://" + rp.getId());
        }
        Set<String> origins = new LinkedHashSet<String>();
        for (String origin : allowedOrigins) {
            origins.add(requireOrigin(origin));
        }
        this.rp = rp;
        this.allowedOrigins = origins;
        this.users = users;
        this.credentials = credentials;
        this.rpIdHash = Crypto.sha256(utf8(rp.getId()));
    }

    /// An origin is compared with what a client reports character for
    /// character, so one that could never match is refused here rather than
    /// refusing every ceremony later.
    private static String requireOrigin(String origin) {
        if (origin == null || origin.length() == 0) {
            throw new IllegalArgumentException("An allowed origin cannot be empty");
        }
        boolean web = origin.startsWith("https://") || origin.startsWith("http://");
        if (web) {
            int host = origin.indexOf("://") + 3;
            if (host == origin.length() || origin.indexOf('/', host) >= 0
                    || origin.indexOf('?', host) >= 0 || origin.indexOf('#', host) >= 0) {
                throw new IllegalArgumentException("An allowed origin is a scheme, a host and "
                        + "at most a port, with no path and no trailing slash: " + origin);
            }
        } else if (origin.indexOf(':') <= 0) {
            throw new IllegalArgumentException("An allowed origin starts with its scheme: "
                    + origin);
        }
        return origin;
    }

    /// Whether the authenticator must verify the user -- `required` -- should
    /// when it can -- `preferred`, the default -- or need not: `discouraged`.
    public void setUserVerification(String userVerification) {
        this.userVerification = oneOf(userVerification, "userVerification");
    }

    /// Whether a new credential must be one a sign-in can find without being
    /// told the user: `required`, the default, which is what makes it a
    /// passkey; `preferred`; or `discouraged`.
    public void setResidentKey(String residentKey) {
        this.residentKey = oneOf(residentKey, "residentKey");
    }

    private static String oneOf(String value, String what) {
        if (!"required".equals(value) && !"preferred".equals(value)
                && !"discouraged".equals(value)) {
            throw new IllegalArgumentException(what + " is required, preferred or discouraged: "
                    + value);
        }
        return value;
    }

    /// `platform` for the device's own authenticator, `cross-platform` for a
    /// security key, or null -- the default -- for either.
    public void setAuthenticatorAttachment(String authenticatorAttachment) {
        if (authenticatorAttachment != null && !"platform".equals(authenticatorAttachment)
                && !"cross-platform".equals(authenticatorAttachment)) {
            throw new IllegalArgumentException("authenticatorAttachment is platform, "
                    + "cross-platform or null: " + authenticatorAttachment);
        }
        this.authenticatorAttachment = authenticatorAttachment;
    }

    /// The timeout the options carry, in milliseconds; five minutes unless set.
    public void setTimeoutMillis(long timeoutMillis) {
        if (timeoutMillis < 1000) {
            throw new IllegalArgumentException("A ceremony needs at least a second");
        }
        this.timeoutMillis = timeoutMillis;
    }

    /// Whether a registration whose attestation this server cannot verify --
    /// `packed` with a certificate chain, `tpm`, `apple`, `android-key`,
    /// `fido-u2f` and the rest -- is accepted as if it carried none. Off
    /// unless set: such a registration is refused with a message that names
    /// the format.
    ///
    /// Accepting one loses nothing a server that asks for no attestation
    /// relies on: the statement vouches for the authenticator's make, and the
    /// credential is verified at every sign-in either way. It is off so that
    /// a format nobody looked at is noticed rather than waved through.
    public void setAllowUnverifiedAttestation(boolean allowUnverifiedAttestation) {
        this.allowUnverifiedAttestation = allowUnverifiedAttestation;
    }

    /// Whether a ceremony may run inside a frame of another origin than the
    /// page around it. Off unless set.
    public void setAllowCrossOrigin(boolean allowCrossOrigin) {
        this.allowCrossOrigin = allowCrossOrigin;
    }

    /// The clock records are dated from; for tests.
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /// What is told of a signature counter that did not advance.
    public void setSignatureCounterListener(SignatureCounterListener listener) {
        this.counterListener = listener;
    }

    public PublicKeyCredentialRpEntity getRp() {
        return rp;
    }

    /// The user entities this was made with.
    public PublicKeyCredentialUserEntityRepository getUserEntities() {
        return users;
    }

    /// The credentials this was made with.
    public UserCredentialRepository getUserCredentials() {
        return credentials;
    }

    // -------------------------------------------------------- registration

    /// The options for `username` to make a passkey with. Gives the user a
    /// handle if they have none, and lists the credentials they have so that
    /// an authenticator holding one of them makes no second.
    public PublicKeyCredentialCreationOptions createPublicKeyCredentialCreationOptions(
            String username) {
        if (username == null || username.length() == 0) {
            throw new IllegalArgumentException("A passkey is registered for a user");
        }
        PublicKeyCredentialUserEntity user = users.findByUsername(username);
        if (user == null) {
            user = users.save(new PublicKeyCredentialUserEntity(username, random(32), username));
        }
        List<byte[]> exclude = new ArrayList<byte[]>();
        List<List<String>> transports = new ArrayList<List<String>>();
        for (CredentialRecord record : credentials.findByUserId(user.getId())) {
            exclude.add(record.getCredentialId());
            transports.add(record.getTransports());
        }
        return new PublicKeyCredentialCreationOptions(rp, user, random(32), timeoutMillis, exclude,
                transports, authenticatorAttachment, residentKey, userVerification);
    }

    /// Verifies the answer to `options` and stores the credential.
    ///
    /// @param options what the client was given, as kept since
    /// @param credential the client's answer, parsed: the WebAuthn
    /// `RegistrationResponseJSON`
    /// @param label what the user calls the passkey; may be null
    /// @return the credential as stored
    /// @throws WebAuthnException when the answer is refused
    public CredentialRecord registerCredential(PublicKeyCredentialCreationOptions options,
                                               Map credential, String label) {
        if (options == null) {
            throw new WebAuthnException(WebAuthnException.NO_CHALLENGE,
                    "No registration was waiting for this answer");
        }
        Map response = response(credential);
        byte[] clientData = clientData(response, "webauthn.create", options.getChallenge());
        byte[] attestation = bytes(response.get("attestationObject"), "attestationObject",
                Cbor.MAX_BYTES);
        Object decoded = Cbor.decode(attestation);
        if (!(decoded instanceof Map)) {
            throw malformed("The attestation object is not a map");
        }
        Object format = ((Map) decoded).get("fmt");
        Object statement = ((Map) decoded).get("attStmt");
        Object raw = ((Map) decoded).get("authData");
        if (!(format instanceof String) || !(statement instanceof Map)
                || !(raw instanceof byte[])) {
            throw malformed("The attestation object has no fmt, attStmt or authData");
        }
        byte[] authData = (byte[]) raw;
        AuthenticatorData data = new AuthenticatorData(authData);
        flags(data, options.getUserVerification());
        if (data.key == null || data.credentialId == null) {
            throw new WebAuthnException(WebAuthnException.MALFORMED_AUTHENTICATOR_DATA,
                    "The authenticator data of a registration carries no credential");
        }
        byte[] named = optionalBytes(credential.get("rawId") != null ? credential.get("rawId")
                : credential.get("id"), "rawId");
        if (named != null && !Crypto.equalsConstantTime(named, data.credentialId)) {
            throw malformed("The answer names another credential than the one it attests");
        }
        attestation((String) format, (Map) statement, data.key, authData, clientData);
        long now = clock.currentTimeMillis();
        CredentialRecord record = CredentialRecord.builder().credentialId(data.credentialId)
                .userEntityUserId(options.getUser().getId())
                .algorithm(data.key.getAlgorithm()).publicKey(data.key.getPublicKey())
                .signatureCount(data.signCount).uvInitialized(data.has(AuthenticatorData.UV))
                .backupEligible(data.has(AuthenticatorData.BE))
                .backupState(data.has(AuthenticatorData.BS))
                .transports(transports(response.get("transports")))
                .label(label(label)).created(now).lastUsed(now).build();
        // One step: of two answers with one id, the second finds it taken.
        if (!credentials.save(record)) {
            throw new WebAuthnException(WebAuthnException.CREDENTIAL_EXISTS,
                    "A credential with this id is registered already");
        }
        return record;
    }

    private void attestation(String format, Map statement, CoseKey key, byte[] authData,
                             byte[] clientData) {
        if ("none".equals(format)) {
            if (!statement.isEmpty()) {
                throw new WebAuthnException(WebAuthnException.ATTESTATION_INVALID,
                        "An attestation of format none carries a statement");
            }
            return;
        }
        boolean self = "packed".equals(format) && statement.get("x5c") == null
                && statement.get("ecdaaKeyId") == null;
        if (!self) {
            if (allowUnverifiedAttestation) {
                return;
            }
            throw new WebAuthnException(WebAuthnException.UNSUPPORTED_ATTESTATION,
                    "The attestation is of format \"" + printable(format, 32) + "\""
                    + ("packed".equals(format) ? " with a certificate chain" : "")
                    + ", which this server does not verify. It verifies none and packed self "
                    + "attestation; the options ask for none. To accept such a registration as "
                    + "if it carried no attestation, call allowUnverifiedAttestation(true).");
        }
        // Self attestation: the credential's own key signs for itself.
        Object alg = statement.get("alg");
        Object signature = statement.get("sig");
        if (!(alg instanceof Long) || ((Long) alg).longValue() != key.getAlgorithm()
                || !(signature instanceof byte[])) {
            throw new WebAuthnException(WebAuthnException.ATTESTATION_INVALID, "The packed "
                    + "attestation names another algorithm than the credential's, or has no "
                    + "signature");
        }
        if (!key.verify(signed(authData, clientData), (byte[]) signature)) {
            throw new WebAuthnException(WebAuthnException.ATTESTATION_INVALID,
                    "The packed attestation is not signed by the credential it attests");
        }
    }

    // ------------------------------------------------------------- sign-in

    /// The options to sign in with.
    ///
    /// @param username the user, when they said who they are first: the
    /// options then list their credentials, and only one of those is accepted.
    /// Null for a sign-in that names nobody, which any passkey of this relying
    /// party may answer. A name with no passkey gets the same options as null.
    public PublicKeyCredentialRequestOptions createCredentialRequestOptions(String username) {
        List<byte[]> allow = new ArrayList<byte[]>();
        List<List<String>> transports = new ArrayList<List<String>>();
        byte[] userId = null;
        if (username != null && username.length() > 0) {
            PublicKeyCredentialUserEntity user = users.findByUsername(username);
            if (user != null) {
                for (CredentialRecord record : credentials.findByUserId(user.getId())) {
                    allow.add(record.getCredentialId());
                    transports.add(record.getTransports());
                }
                if (!allow.isEmpty()) {
                    userId = user.getId();
                }
            }
        }
        return new PublicKeyCredentialRequestOptions(random(32), timeoutMillis, rp.getId(), allow,
                transports, userVerification, userId);
    }

    /// Verifies the answer to `options`, and records the sign-in on the
    /// credential.
    ///
    /// @param options what the client was given, as kept since
    /// @param credential the client's answer, parsed: the WebAuthn
    /// `AuthenticationResponseJSON`
    /// @throws WebAuthnException when the answer is refused
    public Assertion authenticate(PublicKeyCredentialRequestOptions options, Map credential) {
        if (options == null) {
            throw new WebAuthnException(WebAuthnException.NO_CHALLENGE,
                    "No sign-in was waiting for this answer");
        }
        Map response = response(credential);
        byte[] credentialId = bytes(credential.get("rawId") != null ? credential.get("rawId")
                : credential.get("id"), "rawId", 1023);
        List<byte[]> allowed = options.getAllowCredentials();
        if (!allowed.isEmpty()) {
            boolean listed = false;
            for (byte[] id : allowed) {
                listed |= Crypto.equalsConstantTime(id, credentialId);
            }
            if (!listed) {
                throw new WebAuthnException(WebAuthnException.CREDENTIAL_NOT_ALLOWED,
                        "The credential is not one of those the sign-in was started for");
            }
        }
        CredentialRecord record = credentials.findByCredentialId(credentialId);
        if (record == null) {
            throw new WebAuthnException(WebAuthnException.UNKNOWN_CREDENTIAL,
                    "No credential with this id is registered");
        }
        byte[] owner = record.getUserEntityUserId();
        byte[] handle = optionalBytes(response.get("userHandle"), "userHandle");
        byte[] expected = options.getUserId();
        if (expected != null) {
            if (!Crypto.equalsConstantTime(expected, owner)) {
                throw new WebAuthnException(WebAuthnException.USER_MISMATCH,
                        "The credential belongs to another user than the sign-in was started for");
            }
        } else if (handle == null) {
            // Nobody said who they are, so the answer has to.
            throw new WebAuthnException(WebAuthnException.USER_MISMATCH,
                    "The answer names no user, and the sign-in was started for nobody");
        }
        if (handle != null && !Crypto.equalsConstantTime(handle, owner)) {
            throw new WebAuthnException(WebAuthnException.USER_MISMATCH,
                    "The credential belongs to another user than the answer names");
        }
        byte[] clientData = clientData(response, "webauthn.get", options.getChallenge());
        byte[] authData = bytes(response.get("authenticatorData"), "authenticatorData",
                Cbor.MAX_BYTES);
        byte[] signature = bytes(response.get("signature"), "signature", 4096);
        AuthenticatorData data = new AuthenticatorData(authData);
        flags(data, options.getUserVerification());
        if (data.has(AuthenticatorData.BE) != record.isBackupEligible()) {
            throw new WebAuthnException(WebAuthnException.BACKUP_STATE_INVALID, "The credential "
                    + "was " + (record.isBackupEligible() ? "" : "not ") + "eligible for backup "
                    + "when it was registered, and the authenticator now says otherwise");
        }
        CoseKey key = CoseKey.of(record.getAlgorithm(), record.getPublicKey());
        if (!key.verify(signed(authData, clientData), signature)) {
            throw new WebAuthnException(WebAuthnException.SIGNATURE_INVALID,
                    "The signature is not this credential's signature of this sign-in");
        }
        long stored = record.getSignatureCount();
        if ((data.signCount != 0 || stored != 0) && data.signCount <= stored) {
            throw regressed(record, data.signCount);
        }
        boolean verified = data.has(AuthenticatorData.UV);
        long now = clock.currentTimeMillis();
        boolean backedUp = data.has(AuthenticatorData.BS);
        // Decided again where it is stored, in one statement: the same
        // assertion arriving twice at once passes the comparison above twice.
        if (!credentials.advance(credentialId, data.signCount,
                record.isUvInitialized() || verified, backedUp, now)) {
            CredentialRecord current = credentials.findByCredentialId(credentialId);
            if (current == null) {
                throw new WebAuthnException(WebAuthnException.UNKNOWN_CREDENTIAL,
                        "The credential was removed while it signed in");
            }
            throw regressed(current, data.signCount);
        }
        PublicKeyCredentialUserEntity user = users.findById(owner);
        if (user == null) {
            throw new WebAuthnException(WebAuthnException.ACCOUNT_UNAVAILABLE,
                    "The credential belongs to a user handle nobody has");
        }
        return new Assertion(user, CredentialRecord.from(record).signatureCount(data.signCount)
                .uvInitialized(record.isUvInitialized() || verified).backupState(backedUp)
                .lastUsed(now).build(), verified);
    }

    private WebAuthnException regressed(CredentialRecord record, long received) {
        SignatureCounterListener listener = counterListener;
        if (listener != null) {
            listener.signatureCounterRegressed(record, received);
        }
        return new WebAuthnException(WebAuthnException.COUNTER_REGRESSION, "The signature "
                + "counter is " + received + " and was " + record.getSignatureCount()
                + ": the credential may have been copied");
    }

    // ------------------------------------------------------- what both do

    private void flags(AuthenticatorData data, String verification) {
        if (!Crypto.equalsConstantTime(rpIdHash, data.rpIdHash)) {
            throw new WebAuthnException(WebAuthnException.RP_ID_MISMATCH,
                    "The authenticator answered for another relying party than " + rp.getId());
        }
        if (!data.has(AuthenticatorData.UP)) {
            throw new WebAuthnException(WebAuthnException.USER_NOT_PRESENT,
                    "The authenticator did not see the user");
        }
        if ("required".equals(verification) && !data.has(AuthenticatorData.UV)) {
            throw new WebAuthnException(WebAuthnException.USER_NOT_VERIFIED,
                    "The ceremony required the user to be verified, and they were not");
        }
    }

    /// The client data, verified: its bytes, which are what is signed.
    private byte[] clientData(Map response, String type, byte[] challenge) {
        byte[] json = bytes(response.get("clientDataJSON"), "clientDataJSON", MAX_CLIENT_DATA);
        Map parsed;
        try {
            parsed = Json.parseObject(new String(json, "UTF-8"));
        } catch (IOException err) {
            parsed = null;
        } catch (RuntimeException err) {
            parsed = null;
        }
        if (parsed == null) {
            throw malformed("The client data is not a JSON object");
        }
        Object is = parsed.get("type");
        if (!type.equals(is)) {
            throw new WebAuthnException(WebAuthnException.WRONG_TYPE, "The client data is of "
                    + "type " + (is instanceof String ? printable((String) is, 32) : "nothing")
                    + ", and this ceremony is " + type);
        }
        Object sent = parsed.get("challenge");
        byte[] answered = sent instanceof String ? Base64Url.decode((String) sent) : null;
        if (answered == null || !Crypto.equalsConstantTime(challenge, answered)) {
            throw new WebAuthnException(WebAuthnException.CHALLENGE_MISMATCH,
                    "The client answered another challenge than the one it was given");
        }
        Object origin = parsed.get("origin");
        if (!(origin instanceof String) || !allowedOrigins.contains(origin)) {
            throw new WebAuthnException(WebAuthnException.ORIGIN_MISMATCH, "The ceremony ran on "
                    + (origin instanceof String ? printable((String) origin, 200) : "no origin")
                    + ", which is not among the allowed origins " + allowedOrigins);
        }
        if (Boolean.TRUE.equals(parsed.get("crossOrigin")) && !allowCrossOrigin) {
            throw new WebAuthnException(WebAuthnException.CROSS_ORIGIN,
                    "The ceremony ran in a frame of another origin than the page around it");
        }
        return json;
    }

    /// What an authenticator signs: its data, then the hash of the client's.
    private static byte[] signed(byte[] authData, byte[] clientData) {
        byte[] hash = Crypto.sha256(clientData);
        byte[] out = new byte[authData.length + hash.length];
        System.arraycopy(authData, 0, out, 0, authData.length);
        System.arraycopy(hash, 0, out, authData.length, hash.length);
        return out;
    }

    private static Map response(Map credential) {
        if (credential == null) {
            throw malformed("The answer is not a credential");
        }
        Object type = credential.get("type");
        if (type != null && !"public-key".equals(type)) {
            throw malformed("The answer is not a public-key credential");
        }
        Object response = credential.get("response");
        if (!(response instanceof Map)) {
            throw malformed("The credential has no response");
        }
        return (Map) response;
    }

    private static byte[] bytes(Object encoded, String what, int most) {
        byte[] decoded = optionalBytes(encoded, what);
        if (decoded == null) {
            throw malformed("The answer has no " + what);
        }
        if (decoded.length > most) {
            throw malformed("The " + what + " is " + decoded.length + " bytes, and at most "
                    + most + " are read");
        }
        return decoded;
    }

    /// Null when absent or empty; refused when present and not base64url.
    private static byte[] optionalBytes(Object encoded, String what) {
        if (encoded == null) {
            return null;
        }
        if (!(encoded instanceof String)) {
            throw malformed("The " + what + " is not text");
        }
        String text = (String) encoded;
        if (text.length() == 0) {
            return null;
        }
        // Three bytes for four characters: bounded before it is decoded.
        if (text.length() > (Cbor.MAX_BYTES / 3 + 1) * 4) {
            throw malformed("The " + what + " is too long");
        }
        byte[] decoded = Base64Url.decode(text);
        if (decoded == null) {
            throw malformed("The " + what + " is not base64url");
        }
        return decoded.length == 0 ? null : decoded;
    }

    private static List<String> transports(Object listed) {
        List<String> out = new ArrayList<String>();
        if (listed instanceof List) {
            for (Object transport : (List) listed) {
                if (transport instanceof String && out.size() < 8) {
                    String name = (String) transport;
                    if (name.length() > 0 && name.length() <= 32
                            && name.equals(printable(name, 32)) && name.indexOf(',') < 0
                            && !out.contains(name)) {
                        out.add(name);
                    }
                }
            }
        }
        return out;
    }

    private static String label(String label) {
        if (label == null) {
            return "";
        }
        String trimmed = label.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    /// `text` cut to `most` characters, with anything but printable ASCII
    /// replaced: what a client sent, made fit for a log line.
    private static String printable(String text, int most) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0 ; iter < text.length() && iter < most ; iter++) {
            char c = text.charAt(iter);
            out.append(c > ' ' && c < 127 && c != '"' ? c : '?');
        }
        return out.toString();
    }

    private static WebAuthnException malformed(String message) {
        return new WebAuthnException(WebAuthnException.MALFORMED, message);
    }

    private static byte[] random(int length) {
        try {
            return Crypto.randomBytes(length);
        } catch (IOException err) {
            throw new IllegalStateException("The system has no random bytes to give: "
                    + err.getMessage(), err);
        }
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
