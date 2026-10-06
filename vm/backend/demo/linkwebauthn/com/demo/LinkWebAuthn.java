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
package com.demo;

import com.codename1.backend.Backend;
import com.codename1.backend.Base64Url;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.webauthn.CredentialRecord;
import com.codename1.backend.security.webauthn.InMemoryPublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.InMemoryUserCredentialRepository;
import com.codename1.backend.security.webauthn.PublicKeyCredentialCreationOptions;
import com.codename1.backend.security.webauthn.PublicKeyCredentialRequestOptions;
import com.codename1.backend.security.webauthn.PublicKeyCredentialRpEntity;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntity;
import com.codename1.backend.security.webauthn.WebAuthnException;
import com.codename1.backend.security.webauthn.WebAuthnRelyingPartyOperations;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.WiringEnvironment;
import com.codename1.impl.backend.security.SecuritySupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A server people sign in to with a passkey: one chain with `webAuthn`, a
/// user store of its own and passkeys kept in memory. Its binary must hold no
/// login form, no password hashing, no token verification, no second factor,
/// no API keys, no rate limiter and nothing that keeps a table -- and it must
/// hold the native that verifies a signature, and not the one that makes one.
///
/// It cannot make a key, so it cannot answer a challenge of its own. What it
/// verifies instead is the registration and the sign-in of one credential
/// from the Web Authentication specification's test vectors (W3C WebAuthn
/// Level 3, section 16.2, "ES256 Credential with No Attestation"): bytes made
/// elsewhere, read by the CBOR reader and verified by the signature native as
/// they are translated here.
public final class LinkWebAuthn {
    private static final String REG_CHALLENGE =
            "00c30fb78531c464d2b6771dab8d7b603c01162f2fa486bea70f283ae556e130";
    private static final String REG_CLIENT_DATA =
            "7b2274797065223a22776562617574686e2e637265617465222c226368616c6c656e6765223a2241"
            + "4d4d507434557878475453746e63647134313759447742466938767049612d7077386f4f75565734"
            + "5441222c226f726967696e223a2268747470733a2f2f6578616d706c652e6f7267222c2263726f73"
            + "734f726967696e223a66616c73652c22657874726144617461223a22636c69656e74446174614a53"
            + "4f4e206d617920626520657874656e6465642077697468206164646974696f6e616c206669656c64"
            + "7320696e20746865206675747572652c207375636820617320746869733a20426b5165446a646354"
            + "427258426941774a544c453551227d";
    private static final String ATTESTATION_OBJECT =
            "a363666d74646e6f6e656761747453746d74a068617574684461746158a4bfabc37432958b063360"
            + "d3ad6461c9c4735ae7f8edd46592a5e0f01452b2e4b559000000008446ccb9ab1db374750b2367ff"
            + "6f3a1f0020f91f391db4c9b2fde0ea70189cba3fb63f579ba6122b33ad94ff3ec330084be4a50102"
            + "03262001215820afefa16f97ca9b2d23eb86ccb64098d20db90856062eb249c33a9b672f26df6122"
            + "5820930a56b87a2fca66334b03458abf879717c12cc68ed73290af2e2664796b9220";
    private static final String CREDENTIAL_ID =
            "f91f391db4c9b2fde0ea70189cba3fb63f579ba6122b33ad94ff3ec330084be4";
    private static final String AUTH_CHALLENGE =
            "39c0e7521417ba54d43e8dc95174f423dee9bf3cd804ff6d65c857c9abf4d408";
    private static final String AUTHENTICATOR_DATA =
            "bfabc37432958b063360d3ad6461c9c4735ae7f8edd46592a5e0f01452b2e4b51900000000";
    private static final String AUTH_CLIENT_DATA =
            "7b2274797065223a22776562617574686e2e676574222c226368616c6c656e6765223a224f63446e"
            + "55685158756c5455506f334a5558543049393770767a7a59425039745a6368587961763031416722"
            + "2c226f726967696e223a2268747470733a2f2f6578616d706c652e6f7267222c2263726f73734f72"
            + "6967696e223a66616c73657d";
    private static final String SIGNATURE =
            "3046022100f50a4e2e4409249c4a853ba361282f09841df4dd4547a13a87780218deffcd38022100"
            + "8480ac0f0b93538174f575bf11a1dd5d78c6e486013f937295ea13653e331e87";

    private LinkWebAuthn() {
    }

    private static byte[] hex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) Integer.parseInt(hex.substring(iter * 2, iter * 2 + 2), 16);
        }
        return out;
    }

    private static String b64(String hex) {
        return Base64Url.encode(hex(hex));
    }

    private static Map credential(String clientData, String name, String value, String more,
                                  String moreValue) {
        Map response = new HashMap();
        response.put("clientDataJSON", b64(clientData));
        response.put(name, b64(value));
        if (more != null) {
            response.put(more, b64(moreValue));
        }
        Map credential = new HashMap();
        credential.put("id", b64(CREDENTIAL_ID));
        credential.put("rawId", b64(CREDENTIAL_ID));
        credential.put("type", "public-key");
        credential.put("response", response);
        return credential;
    }

    public static void main(String[] args) throws Exception {
        final UserDetailsService users = new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                return User.withUsername(username).password("{none}").roles("USER").build();
            }
        };
        final InMemoryPublicKeyCredentialUserEntityRepository handles =
                new InMemoryPublicKeyCredentialUserEntityRepository();
        final InMemoryUserCredentialRepository credentials =
                new InMemoryUserCredentialRepository();
        List origins = new ArrayList();
        origins.add("https://example.org");
        final WebAuthnRelyingPartyOperations passkeys = new WebAuthnRelyingPartyOperations(
                new PublicKeyCredentialRpEntity("example.org", "Example"), origins, handles,
                credentials);
        Backend.Builder builder = Backend.builder().quiet().host("127.0.0.1").port(0);
        BackendAccess.get().application(builder, new LinkApp() {
            @Override
            void chains(WiringEnvironment environment) {
                HttpSecurity http = SecuritySupport.http(environment.getConfig(),
                        new Object[] {users});
                http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/open").permitAll()
                            .anyRequest().authenticated())
                    .csrf(csrf -> csrf.disable())
                    .webAuthn(w -> w.relyingPartyOperations(passkeys));
                environment.registerSecurityFilterChain(http.build(), 1);
            }
        });
        BackendAccess.get().security(builder);
        Backend backend = builder.start();
        int open = LinkApp.status(backend, "GET", "/open");
        // The endpoints: options for anybody, a sign-in that is refused, and
        // registration for nobody who has not signed in.
        HttpServer.Response options = LinkApp.send(backend, "POST",
                "/webauthn/authenticate/options", "{}", "Content-Type", "application/json");
        int asked = BackendAccess.get().status(options);
        String sent = new String(BackendAccess.get().body(options), "UTF-8");
        boolean rp = sent.indexOf("\"rpId\":\"example.org\"") >= 0
                && sent.indexOf("\"challenge\":\"") >= 0;
        int refused = LinkApp.status(backend, "POST", "/login/webauthn");
        int register = LinkApp.status(backend, "POST", "/webauthn/register/options");
        int home = LinkApp.status(backend, "GET", "/home");
        backend.stop();

        // The ceremonies, over the specification's vector.
        PublicKeyCredentialUserEntity ada = handles.save(new PublicKeyCredentialUserEntity("ada",
                new byte[] {1, 2, 3, 4}, null));
        Map rpMap = new HashMap();
        rpMap.put("id", "example.org");
        rpMap.put("name", "Example");
        Map userMap = new HashMap();
        userMap.put("id", Base64Url.encode(ada.getId()));
        userMap.put("name", "ada");
        Map selection = new HashMap();
        selection.put("residentKey", "required");
        selection.put("userVerification", "preferred");
        Map creation = new HashMap();
        creation.put("rp", rpMap);
        creation.put("user", userMap);
        creation.put("challenge", b64(REG_CHALLENGE));
        creation.put("authenticatorSelection", selection);
        CredentialRecord made = passkeys.registerCredential(
                PublicKeyCredentialCreationOptions.fromMap(creation),
                credential(REG_CLIENT_DATA, "attestationObject", ATTESTATION_OBJECT, null, null),
                "vector");
        boolean registered = b64(CREDENTIAL_ID).equals(Base64Url.encode(made.getCredentialId()))
                && credentials.findByUserId(ada.getId()).size() == 1;

        Map allowed = new HashMap();
        allowed.put("type", "public-key");
        allowed.put("id", b64(CREDENTIAL_ID));
        List allow = new ArrayList();
        allow.add(allowed);
        Map request = new HashMap();
        request.put("challenge", b64(AUTH_CHALLENGE));
        request.put("rpId", "example.org");
        request.put("userVerification", "preferred");
        request.put("allowCredentials", allow);
        request.put("userId", Base64Url.encode(ada.getId()));
        String verified = passkeys.authenticate(PublicKeyCredentialRequestOptions.fromMap(request),
                credential(AUTH_CLIENT_DATA, "authenticatorData", AUTHENTICATOR_DATA, "signature",
                        SIGNATURE)).getUser().getName();
        // And the same with one bit of the signature changed.
        String last = SIGNATURE.substring(SIGNATURE.length() - 1);
        String forged = SIGNATURE.substring(0, SIGNATURE.length() - 1)
                + ("7".equals(last) ? "6" : "7");
        String why;
        try {
            passkeys.authenticate(PublicKeyCredentialRequestOptions.fromMap(request),
                    credential(AUTH_CLIENT_DATA, "authenticatorData", AUTHENTICATOR_DATA,
                            "signature", forged));
            why = "accepted";
        } catch (WebAuthnException err) {
            why = err.getReason();
        }
        System.out.println("LINKCHECK webauthn open=" + open + " options=" + asked + " rp=" + rp
                + " refused=" + refused + " register=" + register + " home=" + home
                + " registered=" + registered + " verified=" + verified + " forged=" + why);
        System.out.println(open == 200 && asked == 200 && rp && refused == 401
                && register == 403 && home == 403 && registered && "ada".equals(verified)
                && "signature_invalid".equals(why) ? "LINKCHECK OK" : "LINKCHECK FAILED");
    }
}
