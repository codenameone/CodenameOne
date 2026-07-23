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

import com.codename1.io.JSONParser;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/// Shared authenticated HTTP behavior for online calendar sources. Tokens and
/// credentials remain app-owned; the source only asks for a token when needed.
public abstract class OAuthCalendarSource extends CalendarSource {

    private final CalendarTokenProvider tokenProvider;

    private final CalendarHttpTransport transport;

    private final String[] scopes;

    private CalendarAuthorizationStatus authorizationStatus = CalendarAuthorizationStatus.NOT_DETERMINED;

    protected OAuthCalendarSource(String id, String displayName, CalendarTokenProvider tokenProvider, CalendarHttpTransport transport, String[] scopes) {
        super(id, displayName);
        if (tokenProvider == null) {
            throw new IllegalArgumentException("tokenProvider required");
        }
        this.tokenProvider = tokenProvider;
        this.transport = transport == null ? new DefaultCalendarHttpTransport() : transport;
        this.scopes = scopes == null ? new String[0] : (String[]) scopes.clone();
    }

    @Override
    public synchronized CalendarAuthorizationStatus getAuthorizationStatus(CalendarAccess access) {
        return authorizationStatus;
    }

    private synchronized CalendarAuthorizationStatus setAuthorizationStatus(CalendarAuthorizationStatus status) {
        authorizationStatus = status;
        return status;
    }

    @Override
    public AsyncResource<CalendarAuthorizationStatus> requestAuthorization(CalendarAccess access) {
        final AsyncResource<CalendarAuthorizationStatus> out = new AsyncResource<CalendarAuthorizationStatus>();
        tokenProvider.getToken(scopes, false).ready(new SuccessCallback<CalendarAuthToken>() {

            @Override
            public void onSucess(CalendarAuthToken token) {
                out.complete(setAuthorizationStatus(CalendarAuthorizationStatus.FULL));
            }
        }).except(new SuccessCallback<Throwable>() {

            @Override
            public void onSucess(Throwable error) {
                setAuthorizationStatus(CalendarAuthorizationStatus.DENIED);
                out.error(authentication(error));
            }
        });
        return out;
    }

    protected final AsyncResource<Map<String, Object>> json(String method, String url, Map<String, Object> body, Map<String, String> headers) {
        final AsyncResource<Map<String, Object>> out = new AsyncResource<Map<String, Object>>();
        send(method, url, body == null ? null : JSONParser.toJson(body), "application/json", headers, false, new ResponseCallback() {

            @Override
            public void complete(CalendarHttpResponse response) {
                if (response.getBody() == null || response.getBody().length() == 0) {
                    out.complete(Collections.<String, Object>emptyMap());
                    return;
                }
                try {
                    out.complete(JSONParser.parseJSON(response.getBody()));
                } catch (IOException ex) {
                    out.error(new CalendarException(CalendarError.MALFORMED_RESPONSE, "Invalid provider JSON", response.getStatusCode(), ex));
                }
            }

            @Override
            public void error(Throwable error) {
                out.error(error);
            }
        });
        return out;
    }

    protected final AsyncResource<CalendarHttpResponse> raw(String method, String url, String body, String contentType, Map<String, String> headers) {
        final AsyncResource<CalendarHttpResponse> out = new AsyncResource<CalendarHttpResponse>();
        send(method, url, body, contentType, headers, false, new ResponseCallback() {

            @Override
            public void complete(CalendarHttpResponse response) {
                out.complete(response);
            }

            @Override
            public void error(Throwable error) {
                out.error(error);
            }
        });
        return out;
    }

    private void send(final String method, final String url, final String body, final String contentType, final Map<String, String> headers, final boolean refreshed, final ResponseCallback callback) {
        tokenProvider.getToken(scopes, refreshed).ready(new SuccessCallback<CalendarAuthToken>() {

            @Override
            public void onSucess(CalendarAuthToken token) {
                CalendarHttpRequest request = new CalendarHttpRequest(method, url).setBody(body).header("Authorization", "Bearer " + token.getAccessToken()).header("Accept", "application/json");
                if (body != null && contentType != null) {
                    request.header("Content-Type", contentType);
                }
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        request.header(entry.getKey(), entry.getValue());
                    }
                }
                transport.execute(request).ready(new SuccessCallback<CalendarHttpResponse>() {

                    public void onSucess(CalendarHttpResponse response) {
                        if (response.getStatusCode() == 401 && !refreshed) {
                            send(method, url, body, contentType, headers, true, callback);
                        } else if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                            callback.complete(response);
                        } else {
                            callback.error(httpError(response));
                        }
                    }
                }).except(new SuccessCallback<Throwable>() {

                    public void onSucess(Throwable error) {
                        callback.error(error);
                    }
                });
            }
        }).except(new SuccessCallback<Throwable>() {

            @Override
            public void onSucess(Throwable error) {
                callback.error(authentication(error));
            }
        });
    }

    private CalendarException httpError(CalendarHttpResponse response) {
        int code = response.getStatusCode();
        CalendarError type = code == 401 ? CalendarError.AUTHENTICATION_REQUIRED : code == 403 ? CalendarError.PERMISSION_DENIED : code == 404 ? CalendarError.NOT_FOUND : code == 409 || code == 412 ? CalendarError.CONFLICT : code == 429 ? CalendarError.RATE_LIMITED : code >= 500 ? CalendarError.NETWORK : CalendarError.INVALID_ARGUMENT;
        return new CalendarException(type, "Calendar provider returned HTTP " + code, code, null);
    }

    private CalendarException authentication(Throwable error) {
        if (error instanceof CalendarException) {
            return (CalendarException) error;
        }
        return new CalendarException(CalendarError.AUTHENTICATION_REQUIRED, error == null ? "Calendar authentication failed" : error.getMessage(), error);
    }

    private interface ResponseCallback {

        void complete(CalendarHttpResponse response);

        void error(Throwable error);
    }
}
