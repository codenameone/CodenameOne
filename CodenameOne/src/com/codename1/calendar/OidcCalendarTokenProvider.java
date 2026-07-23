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
package com.codename1.calendar;

import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import java.time.Instant;

/// In-memory adapter around an application-configured `OidcClient`. Updated
/// tokens are reported to the app; this class never persists them itself.
public final class OidcCalendarTokenProvider implements CalendarTokenProvider {

    public interface TokenListener {

        void tokensUpdated(OidcTokens tokens);
    }

    private final OidcClient client;

    private OidcTokens tokens;

    private TokenListener listener;

    private AsyncResource<OidcTokens> refreshInFlight;

    public OidcCalendarTokenProvider(OidcClient client, OidcTokens initialTokens) {
        if (client == null) {
            throw new IllegalArgumentException("client required");
        }
        this.client = client;
        this.tokens = initialTokens;
    }

    public synchronized OidcCalendarTokenProvider setTokenListener(TokenListener listener) {
        this.listener = listener;
        return this;
    }

    public synchronized void setTokens(OidcTokens value) {
        tokens = value;
    }

    @Override
    public AsyncResource<CalendarAuthToken> getToken(String[] scopes, boolean forceRefresh) {
        final AsyncResource<CalendarAuthToken> out = new AsyncResource<CalendarAuthToken>();
        final OidcTokens current;
        final AsyncResource<OidcTokens> refresh;
        boolean startRefresh = false;
        synchronized (this) {
            current = tokens;
            if (current == null) {
                out.error(new CalendarException(CalendarError.AUTHENTICATION_REQUIRED, "No OAuth tokens supplied"));
                return out;
            }
            if (!forceRefresh && !current.isExpiringWithin(90)) {
                out.complete(convert(current));
                return out;
            }
            if (current.getRefreshToken() == null) {
                out.error(new CalendarException(CalendarError.AUTHENTICATION_REQUIRED, "OAuth token expired and has no refresh token"));
                return out;
            }
            if (refreshInFlight == null) {
                refreshInFlight = new AsyncResource<OidcTokens>();
                startRefresh = true;
            }
            refresh = refreshInFlight;
        }
        if (startRefresh) {
            final AsyncResource<OidcTokens> operation = refresh;
            final AsyncResource<OidcTokens> started;
            try {
                started = client.refresh(current.getRefreshToken());
            } catch (Throwable error) {
                synchronized (this) {
                    if (refreshInFlight == operation) {
                        refreshInFlight = null;
                    }
                }
                operation.error(error);
                return mapped(operation, out);
            }
            started.ready(new SuccessCallback<OidcTokens>() {

                @Override
                public void onSucess(OidcTokens fresh) {
                    TokenListener currentListener;
                    synchronized (OidcCalendarTokenProvider.this) {
                        tokens = fresh;
                        refreshInFlight = null;
                        currentListener = listener;
                    }
                    if (currentListener != null) {
                        currentListener.tokensUpdated(fresh);
                    }
                    operation.complete(fresh);
                }
            }).except(new SuccessCallback<Throwable>() {

                @Override
                public void onSucess(Throwable error) {
                    synchronized (OidcCalendarTokenProvider.this) {
                        refreshInFlight = null;
                    }
                    operation.error(error);
                }
            });
        }
        return mapped(refresh, out);
    }

    private AsyncResource<CalendarAuthToken> mapped(AsyncResource<OidcTokens> refresh,
            final AsyncResource<CalendarAuthToken> out) {
        refresh.ready(new SuccessCallback<OidcTokens>() {

            @Override
            public void onSucess(OidcTokens fresh) {
                out.complete(convert(fresh));
            }
        }).except(new SuccessCallback<Throwable>() {

            @Override
            public void onSucess(Throwable error) {
                out.error(error);
            }
        });
        return out;
    }

    private static CalendarAuthToken convert(OidcTokens t) {
        return new CalendarAuthToken(t.getAccessToken(), t.getExpiresAt() == null ? null : Instant.ofEpochMilli(t.getExpiresAt().getTime()), t.getScope());
    }
}
