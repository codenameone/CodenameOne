/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.mail.ses;

import com.codename1.backend.Base64;
import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.aws.Credentials;
import com.codename1.backend.mail.MailAuthenticationException;
import com.codename1.backend.mail.MailException;
import com.codename1.backend.mail.MailSendException;
import com.codename1.backend.mail.MailTransport;
import com.codename1.backend.mail.Mailers;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/// Sends mail through Amazon SES, by its API rather than its SMTP endpoint:
/// one signed HTTPS request a mail, and no SMTP credentials to issue.
///
/// ```properties
/// cn1.mail.url=ses://eu-west-1
/// ```
///
/// Credentials are the machine's own -- the environment, a container role or
/// an instance role -- found as for every other AWS service here. The sender
/// has to be an identity SES has verified.
///
/// | Setting | Default | |
/// |---|---|---|
/// | `cn1.mail.ses.configuration-set` | | the configuration set a mail is sent with |
/// | `cn1.mail.ses.region` | `us-east-1` | the region to sign for, when the URL names a host and not a region |
///
/// The mail is handed over as it was encoded, so what arrives is what an SMTP
/// server would have been given. SES takes at most 50 recipients a request; a
/// mail to more is sent in several. `ses://localhost:4566` names an endpoint
/// instead of a region, for a stand-in on the same machine.
public final class SesTransport extends MailTransport {
    /// What SES accepts in one request.
    private static final int MAX_RECIPIENTS = 50;

    private final String host;
    private final String region;
    private final boolean secure;
    private final String configurationSet;
    private final Object credentialLock = new Object();
    private Credentials credentials;
    private final Credentials fixed;
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicBoolean closed = new AtomicBoolean();

    /// Opens `ses:` URLs; what the generated wiring registers.
    public static final class Factory implements Mailers.TransportFactory {
        @Override
        public MailTransport open(String url, Config config) throws IOException {
            return new SesTransport(url,
                    config == null ? null : config.get("cn1.mail.ses.region"),
                    config == null ? null : config.get("cn1.mail.ses.configuration-set"));
        }
    }

    /// A transport for `url`.
    ///
    /// - `region`: the region to sign for when `url` names a host; may be null
    /// - `configurationSet`: the configuration set to send with, or null
    public SesTransport(String url, String region, String configurationSet) throws IOException {
        this(url, region, configurationSet, null);
    }

    /// A transport for `url` that signs with `fixed` rather than with what the
    /// environment, the container or the instance provides -- for an account
    /// other than the one the server runs as. Credentials given this way are
    /// never refreshed, so they should not be ones that expire.
    ///
    /// - `region`: the region to sign for when `url` names a host; may be null
    /// - `configurationSet`: the configuration set to send with, or null
    /// - `fixed`: the credentials to sign with, or null to resolve them
    public SesTransport(String url, String region, String configurationSet, Credentials fixed)
            throws IOException {
        this.fixed = fixed;
        int scheme = url.indexOf("://");
        if (scheme < 0 || !"ses".equalsIgnoreCase(url.substring(0, scheme))) {
            throw new IOException("Not an ses:// URL");
        }
        String rest = url.substring(scheme + 3);
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            rest = rest.substring(0, slash);
        }
        if (rest.length() == 0) {
            throw new IOException("cn1.mail.url names no region; ses://eu-west-1 does");
        }
        if (rest.indexOf(':') < 0 && rest.indexOf('.') < 0) {
            this.region = rest;
            this.host = "email." + rest + ".amazonaws.com";
            this.secure = true;
        } else {
            this.region = region == null || region.trim().length() == 0 ? "us-east-1"
                    : region.trim();
            this.host = rest;
            String name = rest.indexOf(':') < 0 ? rest : rest.substring(0, rest.lastIndexOf(':'));
            this.secure = !"localhost".equalsIgnoreCase(name) && !"127.0.0.1".equals(name) // NOPMD AvoidUsingHardCodedIP - loopback is the one place plain HTTP is allowed
                    && !"[::1]".equals(name);
        }
        this.configurationSet = configurationSet == null
                || configurationSet.trim().length() == 0 ? null : configurationSet.trim();
    }

    @Override
    public String getName() {
        return "ses";
    }

    private Credentials credentials() throws IOException {
        if (fixed != null) {
            return fixed;
        }
        synchronized (credentialLock) {
            if (credentials == null || credentials.isExpiring(60000)) {
                credentials = Credentials.resolve();
            }
            return credentials;
        }
    }

    @Override
    public void send(String from, String[] recipients, byte[] mime) throws IOException {
        if (closed.get()) {
            throw new MailException("The mail transport is closed");
        }
        String data = Base64.encode(mime);
        for (int at = 0; at < recipients.length; at += MAX_RECIPIENTS) {
            List<String> to = new ArrayList<String>();
            for (int i = at; i < recipients.length && i < at + MAX_RECIPIENTS; i++) {
                to.add(recipients[i]);
            }
            try {
                post(from, to, data);
            } catch (MailSendException err) {
                if (at > 0 && !err.isPermanent()) {
                    // Some recipients have the mail already; trying it again
                    // whole would send it to them twice.
                    MailSendException partial = new MailSendException("The mail was sent to "
                            + at + " of " + recipients.length + " recipients: "
                            + err.getMessage(), err.getCode(), true);
                    partial.initCause(err);
                    throw partial;
                }
                throw err;
            } catch (IOException err) {
                if (at > 0) {
                    // The same, for a request that never got its answer.
                    MailSendException partial = new MailSendException("The mail was sent to "
                            + at + " of " + recipients.length + " recipients: "
                            + err.getMessage(), 0, true);
                    partial.initCause(err);
                    throw partial;
                }
                throw err;
            }
        }
    }

    private void post(String from, List<String> to, String data) throws IOException {
        Map<String, Object> destination = new LinkedHashMap<String, Object>();
        destination.put("ToAddresses", to);
        Map<String, Object> raw = new LinkedHashMap<String, Object>();
        raw.put("Data", data);
        Map<String, Object> content = new LinkedHashMap<String, Object>();
        content.put("Raw", raw);
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("FromEmailAddress", from);
        request.put("Destination", destination);
        request.put("Content", content);
        if (configurationSet != null) {
            request.put("ConfigurationSetName", configurationSet);
        }
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("content-type", "application/json");
        Web.Result r;
        try {
            r = Aws.send(credentials(), region, "ses", "POST", host,
                    "/v2/email/outbound-emails", null, headers,
                    WireBuffer.utf8(Json.write(request)), null, secure);
        } catch (IOException err) {
            healthy.set(false);
            throw err;
        }
        int status = r.getStatus();
        if (r.isSuccess()) {
            healthy.set(true);
            return;
        }
        if (status <= 0) {
            healthy.set(false);
            throw new MailException("SES could not be reached: " + r.getError());
        }
        healthy.set(status < 500);
        String body = r.getBodyAsString();
        String why = "SES refused the mail (" + status + "): "
                + (body == null ? r.getError() : body.length() > 300 ? body.substring(0, 300)
                : body);
        if (status == 401 || status == 403) {
            throw new MailAuthenticationException(why, status);
        }
        // Too many requests and a request that timed out are the two refusals
        // of the client's own that the same mail can get past later.
        throw new MailSendException(why, status, status < 500 && status != 429 && status != 408);
    }

    @Override
    public boolean isHealthy() {
        return healthy.get();
    }

    @Override
    public void close() {
        closed.set(true);
    }
}
