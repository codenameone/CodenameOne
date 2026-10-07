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
package com.codename1.backend.security;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.oauth2.core.Converter;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtAudienceValidator;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtDecoders;
import com.codename1.backend.security.oauth2.jwt.JwtIssuerValidator;
import com.codename1.backend.security.oauth2.jwt.JwtTimestampValidator;
import com.codename1.backend.security.oauth2.jwt.JwtValidators;
import com.codename1.backend.security.oauth2.jwt.RemoteJwkSet;
import com.codename1.backend.security.oauth2.jwt.SupplierJwtDecoder;
import com.codename1.backend.security.oauth2.server.resource.BearerTokenAccessDeniedHandler;
import com.codename1.backend.security.oauth2.server.resource.BearerTokenAuthenticationEntryPoint;
import com.codename1.backend.security.oauth2.server.resource.BearerTokenResolver;
import com.codename1.backend.security.oauth2.server.resource.DefaultBearerTokenResolver;
import com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/// Sign-in with a bearer token on each request: the routes under the chain are
/// an OAuth 2.0 resource server, and the token is a JWT.
///
/// ```java
/// @Bean
/// SecurityFilterChain api(HttpSecurity http) {
///     http.securityMatcher("/api/**")
///         .authorizeHttpRequests(auth -> auth
///                 .requestMatchers("/api/orders/**").hasAuthority("SCOPE_orders:read")
///                 .anyRequest().authenticated())
///         .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
///     return http.build();
/// }
/// ```
///
/// With nothing more said, the tokens are verified by the application's
/// [JwtDecoder] bean, or else as these properties describe:
///
/// | Property | Meaning |
/// |---|---|
/// | `cn1.security.oauth2.resourceserver.jwt.issuer-uri` | The issuer. Its metadata names the keys, and every token's `iss` must be it. |
/// | `cn1.security.oauth2.resourceserver.jwt.jwk-set-uri` | Where the keys are, when the issuer publishes no metadata. |
/// | `cn1.security.oauth2.resourceserver.jwt.public-key-location` | A PEM file holding the one public key. |
/// | `cn1.security.oauth2.resourceserver.jwt.jws-algorithms` | The algorithms accepted, separated by commas; `RS256` unless set. |
/// | `cn1.security.oauth2.resourceserver.jwt.audiences` | What this server is called in a token's `aud`, separated by commas. Set it: without it a token the issuer made for another application is accepted here. |
///
/// A request authenticated by its token is not asked for a CSRF token: a
/// browser does not attach an `Authorization` header to a request another site
/// made it send, which is the whole of what CSRF protection is against. No
/// session is started for it either.
///
/// A token is refused with 401 and the reason in `WWW-Authenticate`; a good
/// token that does not grant enough, with 403 and `insufficient_scope`. See
/// [BearerTokenAuthenticationEntryPoint] and [BearerTokenAccessDeniedHandler].
public final class OAuth2ResourceServerConfigurer extends SecurityConfigurer {
    private static final String PREFIX = "cn1.security.oauth2.resourceserver.jwt.";
    /// The issuer whose tokens are accepted.
    public static final String ISSUER_URI = PREFIX + "issuer-uri";
    /// The address of the issuer's JSON Web Key Set.
    public static final String JWK_SET_URI = PREFIX + "jwk-set-uri";
    /// A PEM file holding the public key tokens are verified with.
    public static final String PUBLIC_KEY_LOCATION = PREFIX + "public-key-location";
    /// The signature algorithms accepted, separated by commas.
    public static final String JWS_ALGORITHMS = PREFIX + "jws-algorithms";
    /// The audiences a token must name one of, separated by commas.
    public static final String AUDIENCES = PREFIX + "audiences";

    private BearerTokenResolver bearerTokenResolver;
    private AuthenticationEntryPoint authenticationEntryPoint =
            new BearerTokenAuthenticationEntryPoint();
    private AccessDeniedHandler accessDeniedHandler = new BearerTokenAccessDeniedHandler();
    private String realmName;
    private AuthenticationManagerResolver authenticationManagerResolver;
    private JwtConfigurer jwt;

    OAuth2ResourceServerConfigurer() {
    }

    /// The tokens are JWTs, verified here; see [JwtConfigurer].
    public OAuth2ResourceServerConfigurer jwt(Customizer<JwtConfigurer> customizer) {
        if (jwt == null) {
            jwt = new JwtConfigurer();
        }
        customizer.customize(jwt);
        return this;
    }

    /// Where the token is looked for in a request; the `Authorization` header
    /// unless set. See [DefaultBearerTokenResolver].
    public OAuth2ResourceServerConfigurer bearerTokenResolver(BearerTokenResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("bearerTokenResolver cannot be null");
        }
        this.bearerTokenResolver = resolver;
        return this;
    }

    /// The realm the challenges name: `WWW-Authenticate: Bearer realm="..."`.
    /// None unless set. It applies to the entry point and the access-denied
    /// handler this configurer brings, not to ones given in their place.
    public OAuth2ResourceServerConfigurer realmName(String realmName) {
        if (realmName == null || realmName.length() == 0) {
            throw new IllegalArgumentException("realmName cannot be empty");
        }
        this.realmName = realmName;
        return this;
    }

    /// What answers a request whose token is missing or refused.
    public OAuth2ResourceServerConfigurer authenticationEntryPoint(AuthenticationEntryPoint entryPoint) {
        if (entryPoint == null) {
            throw new IllegalArgumentException("authenticationEntryPoint cannot be null");
        }
        this.authenticationEntryPoint = entryPoint;
        return this;
    }

    /// What answers a request whose token does not grant enough.
    public OAuth2ResourceServerConfigurer accessDeniedHandler(AccessDeniedHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("accessDeniedHandler cannot be null");
        }
        this.accessDeniedHandler = handler;
        return this;
    }

    /// Chooses what authenticates each request's token, in place of [#jwt]: a
    /// [com.codename1.backend.security.oauth2.server.resource.JwtIssuerAuthenticationManagerResolver]
    /// for a server that trusts several issuers.
    public OAuth2ResourceServerConfigurer authenticationManagerResolver(
            AuthenticationManagerResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("authenticationManagerResolver cannot be null");
        }
        this.authenticationManagerResolver = resolver;
        return this;
    }

    BearerTokenResolver resolver() {
        if (bearerTokenResolver == null) {
            BearerTokenResolver shared = getBuilder().getSharedObject(BearerTokenResolver.class);
            bearerTokenResolver = shared != null ? shared : new DefaultBearerTokenResolver();
        }
        return bearerTokenResolver;
    }

    @Override
    public void init(HttpSecurity http) {
        if (jwt == null && authenticationManagerResolver == null) {
            throw new IllegalStateException("oauth2ResourceServer() needs to be told how tokens "
                    + "are verified: call jwt(...), or authenticationManagerResolver(...)");
        }
        if (jwt != null && authenticationManagerResolver != null) {
            throw new IllegalStateException("oauth2ResourceServer() was given both jwt(...) and "
                    + "authenticationManagerResolver(...); it takes one");
        }
        if (realmName != null) {
            if (authenticationEntryPoint instanceof BearerTokenAuthenticationEntryPoint) {
                ((BearerTokenAuthenticationEntryPoint) authenticationEntryPoint)
                        .setRealmName(realmName);
            }
            if (accessDeniedHandler instanceof BearerTokenAccessDeniedHandler) {
                ((BearerTokenAccessDeniedHandler) accessDeniedHandler).setRealmName(realmName);
            }
        }
        RequestMatcher carriesToken = new CarriesToken(resolver());
        CsrfConfigurer csrf = http.getConfigurer(CsrfConfigurer.class);
        if (csrf != null) {
            csrf.ignoringRequestMatchers(carriesToken);
        }
        ExceptionHandlingConfigurer handling = http.getConfigurer(ExceptionHandlingConfigurer.class);
        if (handling != null) {
            // By the token it carries, or by a token having been accepted for
            // it some other way -- a test's jwt() sends none -- so that both
            // are refused the same.
            RequestMatcher byToken = RequestMatchers.anyOf(carriesToken, new AcceptedToken());
            handling.defaultAuthenticationEntryPointFor(authenticationEntryPoint, byToken);
            handling.defaultAccessDeniedHandlerFor(accessDeniedHandler, byToken);
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        AuthenticationManagerResolver managers = authenticationManagerResolver;
        if (managers == null) {
            managers = new Fixed(jwt.manager(http));
        }
        http.addFilter(new BearerTokenAuthenticationFilter(managers, resolver(),
                authenticationEntryPoint), HttpSecurity.ORDER_BEARER_TOKEN);
    }

    /// Matches a request whose user is who a bearer token said.
    private static final class AcceptedToken implements RequestMatcher {
        @Override
        public boolean matches(HttpServer.Request request) {
            SecurityContext context = SecurityContextHolder.peek();
            return context != null && context.getAuthentication()
                    instanceof com.codename1.backend.security.oauth2.server.resource
                            .JwtAuthenticationToken;
        }

        @Override
        public String toString() {
            return "AcceptedBearerToken";
        }
    }

    /// Matches a request that carries a bearer token.
    private static final class CarriesToken implements RequestMatcher {
        private final BearerTokenResolver resolver;

        CarriesToken(BearerTokenResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public boolean matches(HttpServer.Request request) {
            try {
                return resolver.resolve(request) != null;
            } catch (OAuth2AuthenticationException malformed) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "BearerToken";
        }
    }

    /// One manager, whatever the request.
    private static final class Fixed implements AuthenticationManagerResolver {
        private final AuthenticationManager manager;

        Fixed(AuthenticationManager manager) {
            this.manager = manager;
        }

        @Override
        public AuthenticationManager resolve(HttpServer.Request request) {
            return manager;
        }
    }

    /// How the JWTs of a resource server are verified and read.
    ///
    /// ```java
    /// http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
    ///         .decoder(DefaultJwtDecoder.withPublicKey(key).build())
    ///         .jwtAuthenticationConverter(converter)));
    /// ```
    public static final class JwtConfigurer {
        private JwtDecoder decoder;
        private AuthenticationManager authenticationManager;
        private Converter<Jwt, ? extends AbstractAuthenticationToken> converter;

        JwtConfigurer() {
        }

        /// The decoder, in place of the application's bean and the properties.
        public JwtConfigurer decoder(JwtDecoder decoder) {
            if (decoder == null) {
                throw new IllegalArgumentException("decoder cannot be null");
            }
            this.decoder = decoder;
            return this;
        }

        /// Verifies with the keys published at this address, accepting RS256.
        public JwtConfigurer jwkSetUri(String jwkSetUri) {
            this.decoder = DefaultJwtDecoder.withJwkSetUri(jwkSetUri).build();
            return this;
        }

        /// What makes an authentication of a verified token: its authorities
        /// and its name. See
        /// [com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationConverter].
        public JwtConfigurer jwtAuthenticationConverter(
                Converter<Jwt, ? extends AbstractAuthenticationToken> converter) {
            if (converter == null) {
                throw new IllegalArgumentException("jwtAuthenticationConverter cannot be null");
            }
            this.converter = converter;
            return this;
        }

        /// What authenticates a token, in place of everything else here.
        public JwtConfigurer authenticationManager(AuthenticationManager authenticationManager) {
            if (authenticationManager == null) {
                throw new IllegalArgumentException("authenticationManager cannot be null");
            }
            this.authenticationManager = authenticationManager;
            return this;
        }

        @SuppressWarnings("unchecked")
        AuthenticationManager manager(HttpSecurity http) {
            if (authenticationManager != null) {
                return authenticationManager;
            }
            JwtDecoder use = decoder;
            if (use == null) {
                use = http.uniqueSharedObject(JwtDecoder.class,
                        "The JWT resource server", "oauth2ResourceServer().jwt().decoder(...)");
            }
            if (use == null) {
                use = fromConfig(http.getConfig());
            }
            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(use);
            Converter<Jwt, ? extends AbstractAuthenticationToken> convert = converter;
            if (convert == null) {
                convert = http.getSharedObject(
                        com.codename1.backend.security.oauth2.server.resource
                                .JwtAuthenticationConverter.class);
            }
            if (convert != null) {
                provider.setJwtAuthenticationConverter(convert);
            }
            return new ProviderManager(provider);
        }
    }

    /// The decoder `cn1.security.oauth2.resourceserver.jwt.*` describes.
    static JwtDecoder fromConfig(Config config) {
        String issuer;
        String jwkSetUri;
        String keyLocation;
        final List<String> audiences;
        final JwsAlgorithm[] algorithms;
        try {
            issuer = config == null ? null : config.get(ISSUER_URI);
            jwkSetUri = config == null ? null : config.get(JWK_SET_URI);
            keyLocation = config == null ? null : config.get(PUBLIC_KEY_LOCATION);
            audiences = list(config == null ? null : config.get(AUDIENCES));
            algorithms = algorithms(list(config == null ? null : config.get(JWS_ALGORITHMS)));
        } catch (IOException err) {
            throw new IllegalStateException(err.getMessage(), err);
        }
        if (jwkSetUri != null && jwkSetUri.length() > 0) {
            DefaultJwtDecoder.Builder builder = DefaultJwtDecoder.withJwkSetUri(jwkSetUri);
            if (algorithms != null) {
                builder.jwsAlgorithms(algorithms);
            }
            DefaultJwtDecoder decoder = builder.build();
            decoder.setJwtValidator(validators(issuer, audiences));
            return decoder;
        }
        if (issuer != null && issuer.length() > 0) {
            final String from = issuer;
            // When the first token arrives, not now: an issuer that is down
            // must not stop this server from starting.
            return new SupplierJwtDecoder(new Supplier<JwtDecoder>() {
                @Override
                public JwtDecoder get() {
                    // With the configured algorithms, as the two branches beside this
                    // one have them. Discovery used to decide alone: a server set to
                    // accept ES256 only went on accepting every algorithm its issuer's
                    // metadata listed, as long as the issuer was all it was given.
                    JwtDecoder made = JwtDecoders.fromIssuerLocation(from, RemoteJwkSet.WEB, algorithms);
                    if (made instanceof DefaultJwtDecoder) {
                        ((DefaultJwtDecoder) made).setJwtValidator(validators(from, audiences));
                    }
                    return made;
                }
            });
        }
        if (keyLocation != null && keyLocation.length() > 0) {
            byte[] key;
            try {
                key = KeyFiles.readPublicKey(keyLocation);
            } catch (IOException err) {
                throw new IllegalStateException(PUBLIC_KEY_LOCATION + ": " + err.getMessage(), err);
            }
            DefaultJwtDecoder.Builder builder = DefaultJwtDecoder.withPublicKey(key);
            if (algorithms != null) {
                builder.jwsAlgorithms(algorithms);
            }
            DefaultJwtDecoder decoder = builder.build();
            decoder.setJwtValidator(validators(null, audiences));
            return decoder;
        }
        throw new IllegalStateException("oauth2ResourceServer().jwt() needs a way to verify "
                + "tokens, and this application has none. Declare a JwtDecoder bean, call "
                + "decoder(...) or jwkSetUri(...) on the jwt() configurer, or set " + ISSUER_URI
                + ", " + JWK_SET_URI + " or " + PUBLIC_KEY_LOCATION + ".");
    }

    @SuppressWarnings("unchecked")
    private static OAuth2TokenValidator<Jwt> validators(String issuer, List<String> audiences) {
        List<OAuth2TokenValidator<Jwt>> all = new ArrayList<OAuth2TokenValidator<Jwt>>();
        all.add(new JwtTimestampValidator());
        if (issuer != null && issuer.length() > 0) {
            all.add(new JwtIssuerValidator(issuer));
        }
        if (!audiences.isEmpty()) {
            all.add(new JwtAudienceValidator(audiences.toArray(new String[audiences.size()])));
        }
        return JwtValidators.createDefaultWithValidators(all.toArray(new OAuth2TokenValidator[all.size()]));
    }

    private static JwsAlgorithm[] algorithms(List<String> names) {
        if (names.isEmpty()) {
            return null;
        }
        JwsAlgorithm[] out = new JwsAlgorithm[names.size()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = SignatureAlgorithm.from(names.get(iter));
            if (out[iter] == null) {
                throw new IllegalStateException(JWS_ALGORITHMS + " names " + names.get(iter)
                        + ", which is not one of RS256, RS384, RS512, PS256, ES256 and ES384");
            }
        }
        return out;
    }

    /// The values between commas, trimmed, without the empty ones.
    private static List<String> list(String value) {
        List<String> out = new ArrayList<String>();
        if (value == null) {
            return out;
        }
        int start = 0;
        while (start <= value.length()) {
            int comma = value.indexOf(',', start);
            int end = comma < 0 ? value.length() : comma;
            String one = value.substring(start, end).trim();
            if (one.length() > 0) {
                out.add(one);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return out;
    }
}
