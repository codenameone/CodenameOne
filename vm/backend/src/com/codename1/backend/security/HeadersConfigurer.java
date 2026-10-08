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

import com.codename1.backend.HttpServer;
import java.util.ArrayList;
import java.util.List;

/// The security headers a chain puts on its responses. On by default:
///
/// ```java
/// X-Content-Type-Options: nosniff
/// X-XSS-Protection: 0
/// Cache-Control: no-cache, no-store, max-age=0, must-revalidate
/// Pragma: no-cache
/// Expires: 0
/// X-Frame-Options: DENY
/// Strict-Transport-Security: max-age=31536000 ; includeSubDomains
/// ```
///
/// The cache headers are left out when the handler set any of the three
/// itself, and the last header is sent only on a response to a request that
/// arrived over TLS this server terminated. A header the handler set is never
/// replaced.
///
/// ```java
/// http.headers(headers -> headers
///         .frameOptions(frame -> frame.sameOrigin())
///         .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")));
/// ```
public final class HeadersConfigurer extends SecurityConfigurer {
    private final FrameOptionsConfig frameOptions = new FrameOptionsConfig();
    private final HstsConfig hsts = new HstsConfig();
    private final ContentSecurityPolicyConfig contentSecurityPolicy = new ContentSecurityPolicyConfig();
    private final Toggle contentTypeOptions = new Toggle();
    private final Toggle xssProtection = new Toggle();
    private final Toggle cacheControl = new Toggle();
    private final List<HeaderWriter> writers = new ArrayList<HeaderWriter>();

    HeadersConfigurer() {
    }

    /// Turns every default off, leaving what is configured after this call.
    public HeadersConfigurer defaultsDisabled() {
        frameOptions.mode = null;
        hsts.enabled = false;
        contentTypeOptions.enabled = false;
        xssProtection.enabled = false;
        cacheControl.enabled = false;
        return this;
    }

    /// `X-Frame-Options`: whether a page may be shown in a frame.
    public HeadersConfigurer frameOptions(Customizer<FrameOptionsConfig> frameOptionsCustomizer) {
        if (frameOptions.mode == null) {
            frameOptions.mode = "DENY";
        }
        frameOptionsCustomizer.customize(frameOptions);
        return this;
    }

    /// `Strict-Transport-Security`.
    public HeadersConfigurer httpStrictTransportSecurity(Customizer<HstsConfig> hstsCustomizer) {
        hsts.enabled = true;
        hstsCustomizer.customize(hsts);
        return this;
    }

    /// `Content-Security-Policy`; not sent unless configured here.
    public HeadersConfigurer contentSecurityPolicy(
            Customizer<ContentSecurityPolicyConfig> contentSecurityCustomizer) {
        contentSecurityCustomizer.customize(contentSecurityPolicy);
        return this;
    }

    /// `X-Content-Type-Options: nosniff`.
    public HeadersConfigurer contentTypeOptions(Customizer<Toggle> contentTypeOptionsCustomizer) {
        contentTypeOptions.enabled = true;
        contentTypeOptionsCustomizer.customize(contentTypeOptions);
        return this;
    }

    /// `X-XSS-Protection: 0`, which tells an old browser to leave its faulty
    /// filter off.
    public HeadersConfigurer xssProtection(Customizer<Toggle> xssCustomizer) {
        xssProtection.enabled = true;
        xssCustomizer.customize(xssProtection);
        return this;
    }

    /// The headers that keep a response out of caches.
    public HeadersConfigurer cacheControl(Customizer<Toggle> cacheControlCustomizer) {
        cacheControl.enabled = true;
        cacheControlCustomizer.customize(cacheControl);
        return this;
    }

    /// One more writer, run after the built-in ones.
    public HeadersConfigurer addHeaderWriter(HeaderWriter headerWriter) {
        if (headerWriter == null) {
            throw new IllegalArgumentException("headerWriter cannot be null");
        }
        writers.add(headerWriter);
        return this;
    }

    @Override
    public void configure(HttpSecurity http) {
        List<HeaderWriter> all = new ArrayList<HeaderWriter>();
        all.add(new Defaults(contentTypeOptions.enabled, xssProtection.enabled,
                cacheControl.enabled, frameOptions.mode, hsts.enabled ? hsts.value() : null,
                contentSecurityPolicy.directives, contentSecurityPolicy.reportOnly
                ? "Content-Security-Policy-Report-Only" : "Content-Security-Policy"));
        all.addAll(writers);
        http.addFilter(new HeaderWriterFilter(all), HttpSecurity.ORDER_HEADERS);
    }

    /// The built-in headers, as this chain has them configured.
    private static final class Defaults implements HeaderWriter {
        private final boolean nosniff;
        private final boolean xss;
        private final boolean noCache;
        private final String frame;
        private final String strict;
        private final String policy;
        private final String policyHeader;

        Defaults(boolean nosniff, boolean xss, boolean noCache, String frame, String strict,
                 String policy, String policyHeader) {
            this.nosniff = nosniff;
            this.xss = xss;
            this.noCache = noCache;
            this.frame = frame;
            this.strict = strict;
            this.policy = policy;
            this.policyHeader = policyHeader;
        }

        @Override
        public void writeHeaders(HttpServer.Request request, Headers response) {
            if (nosniff) {
                response.setIfAbsent("X-Content-Type-Options", "nosniff");
            }
            if (xss) {
                response.setIfAbsent("X-XSS-Protection", "0");
            }
            if (noCache && response.getStatus() != 304 && !response.contains("Cache-Control")
                    && !response.contains("Pragma") && !response.contains("Expires")) {
                response.set("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                response.set("Pragma", "no-cache");
                response.set("Expires", "0");
            }
            if (strict != null && response.isSecure()) {
                response.setIfAbsent("Strict-Transport-Security", strict);
            }
            if (frame != null) {
                response.setIfAbsent("X-Frame-Options", frame);
            }
            if (policy != null) {
                response.setIfAbsent(policyHeader, policy);
            }
        }
    }

    /// A header that is either sent or not.
    public static final class Toggle {
        private boolean enabled = true;

        private Toggle() {
        }

        /// Does not send the header.
        public void disable() {
            enabled = false;
        }
    }

    /// `X-Frame-Options`.
    public static final class FrameOptionsConfig {
        private String mode = "DENY";

        private FrameOptionsConfig() {
        }

        /// No page may be framed. The default.
        public FrameOptionsConfig deny() {
            mode = "DENY";
            return this;
        }

        /// A page may be framed by a page of the same origin.
        public FrameOptionsConfig sameOrigin() {
            mode = "SAMEORIGIN";
            return this;
        }

        /// Does not send the header.
        public void disable() {
            mode = null;
        }
    }

    /// `Strict-Transport-Security`.
    public static final class HstsConfig {
        private boolean enabled = true;
        private long maxAge = 31536000L;
        private boolean includeSubDomains = true;
        private boolean preload;

        private HstsConfig() {
        }

        /// How long a browser insists on TLS; a year unless set.
        public HstsConfig maxAgeInSeconds(long maxAgeInSeconds) {
            if (maxAgeInSeconds < 0) {
                throw new IllegalArgumentException("maxAgeInSeconds must be non-negative");
            }
            this.maxAge = maxAgeInSeconds;
            return this;
        }

        public HstsConfig includeSubDomains(boolean includeSubDomains) {
            this.includeSubDomains = includeSubDomains;
            return this;
        }

        public HstsConfig preload(boolean preload) {
            this.preload = preload;
            return this;
        }

        /// Does not send the header.
        public void disable() {
            enabled = false;
        }

        private String value() {
            return "max-age=" + maxAge + (includeSubDomains ? " ; includeSubDomains" : "")
                    + (preload ? " ; preload" : "");
        }
    }

    /// `Content-Security-Policy`.
    public static final class ContentSecurityPolicyConfig {
        private String directives;
        private boolean reportOnly;

        private ContentSecurityPolicyConfig() {
        }

        /// The policy, as the header carries it: `default-src 'self'`.
        public ContentSecurityPolicyConfig policyDirectives(String policyDirectives) {
            if (policyDirectives == null || policyDirectives.length() == 0) {
                throw new IllegalArgumentException("policyDirectives cannot be null or empty");
            }
            for (int iter = 0 ; iter < policyDirectives.length() ; iter++) {
                char c = policyDirectives.charAt(iter);
                if (c < 0x20 || c > 0x7e) {
                    throw new IllegalArgumentException("policyDirectives is printable ASCII on "
                            + "one line");
                }
            }
            this.directives = policyDirectives;
            return this;
        }

        /// Sends the policy as `Content-Security-Policy-Report-Only`.
        public ContentSecurityPolicyConfig reportOnly() {
            this.reportOnly = true;
            return this;
        }
    }
}
