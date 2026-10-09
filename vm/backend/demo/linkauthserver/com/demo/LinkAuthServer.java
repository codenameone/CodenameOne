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
import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.WiringEnvironment;
import com.codename1.impl.backend.security.SecuritySupport;
import java.util.ArrayList;
import java.util.List;

/// A server that issues tokens: one chain with `authorizationServer`, two
/// clients kept in memory and a signing key made at start. Its binary must
/// hold no sign-in through another provider, no login form, no bcrypt, no user
/// store, no API keys, no rate limiter, no second factor and nothing that
/// keeps a table.
public final class LinkAuthServer {
    private LinkAuthServer() {
    }

    /// Client secrets as this fixture stores them. An application gives the
    /// encoder it hashes passwords with; this one is here so that the server
    /// links none of those.
    private static final class Reversed implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return new StringBuilder(rawPassword.toString()).reverse().toString();
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }

    public static void main(String[] args) throws Exception {
        final PasswordEncoder encoder = new Reversed();
        final List<Jwk> keyList = new ArrayList<Jwk>();
        keyList.add(AuthorizationServerKeys.usable(Jwk.ofPrivateKey(Crypto.generateRsaKey(2048)),
                "the generated key"));
        final JwkSource keys = AuthorizationServerKeys.of(keyList);
        final InMemoryRegisteredClientRepository clients = new InMemoryRegisteredClientRepository(
                RegisteredClient.withId("1").clientId("service")
                        .clientSecret(encoder.encode("service-secret"))
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("api").build(),
                RegisteredClient.withId("2").clientId("app")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                        .redirectUri("com.acme.app:/oauth2redirect").scope("openid").build());
        Backend.Builder builder = Backend.builder().quiet().host("127.0.0.1").port(0);
        BackendAccess.get().application(builder, new LinkApp() {
            @Override
            void chains(WiringEnvironment environment) {
                HttpSecurity http = SecuritySupport.http(environment.getConfig(), new Object[0]);
                http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/open").permitAll()
                            .anyRequest().authenticated())
                    .authorizationServer(as -> as
                            .settings(AuthorizationServerSettings.builder()
                                    .issuer("https://id.example.com").build())
                            .registeredClientRepository(clients).jwkSource(keys)
                            .clientSecretEncoder(encoder));
                environment.registerSecurityFilterChain(http.build(), 1);
            }
        });
        BackendAccess.get().security(builder);
        Backend backend = builder.start();
        int open = LinkApp.status(backend, "GET", "/open");
        int metadata = LinkApp.status(backend, "GET", "/.well-known/openid-configuration");
        int jwks = LinkApp.status(backend, "GET", "/oauth2/jwks");
        // A token signed with the key: the signature native is in this binary.
        HttpServer.Response issued = LinkApp.send(backend, "POST", "/oauth2/token",
                "grant_type=client_credentials&client_id=service&client_secret=service-secret"
                + "&scope=api");
        int token = BackendAccess.get().status(issued);
        String body = new String(BackendAccess.get().body(issued), "UTF-8");
        boolean signed = body.indexOf("\"access_token\":\"") >= 0
                && body.indexOf("\"token_type\":\"Bearer\"") >= 0;
        int wrong = BackendAccess.get().status(LinkApp.send(backend, "POST", "/oauth2/token",
                "grant_type=client_credentials&client_id=service&client_secret=nope"));
        int device = BackendAccess.get().status(LinkApp.send(backend, "POST",
                "/oauth2/device_authorization", "client_id=app&scope=openid"));
        // Nobody is signed in, and this is not a browser.
        int authorize = LinkApp.status(backend, "GET", "/oauth2/authorize?response_type=code"
                + "&client_id=app&redirect_uri=com.acme.app%3A%2Foauth2redirect&scope=openid"
                + "&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
                + "&code_challenge_method=S256");
        int elsewhere = LinkApp.status(backend, "GET", "/home");
        backend.stop();
        System.out.println("LINKCHECK authserver open=" + open + " metadata=" + metadata
                + " jwks=" + jwks + " token=" + token + " signed=" + signed + " wrong=" + wrong
                + " device=" + device + " authorize=" + authorize + " elsewhere=" + elsewhere);
        System.out.println(open == 200 && metadata == 200 && jwks == 200 && token == 200 && signed
                && wrong == 401 && device == 200 && authorize == 401 && elsewhere == 403
                ? "LINKCHECK OK" : "LINKCHECK FAILED");
    }
}
