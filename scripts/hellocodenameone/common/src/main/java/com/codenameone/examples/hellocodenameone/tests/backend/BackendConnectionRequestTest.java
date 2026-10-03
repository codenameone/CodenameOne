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
package com.codenameone.examples.hellocodenameone.tests.backend;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;

import java.io.IOException;
import java.io.InputStream;

/// `ConnectionRequest` itself against the backend: a raw body, response headers,
/// error statuses read rather than thrown, redirects followed and not, cookies and
/// a server session kept across requests, and a timeout.
public class BackendConnectionRequestTest extends BackendClientTest {
    @Override
    protected void defineSteps() {
        step(() -> {
            final int id = currentStep();
            Probe probe = new Probe(url("/api/echo"), "POST") {
                @Override
                protected void answered() {
                    if (expect(code == 200, "raw post answered " + code)
                            && expect(text().indexOf("\"body\":\"plain body\"") >= 0,
                            "raw post echoed " + text())
                            && expect("echo".equals(probeHeader), "X-Probe was " + probeHeader)) {
                        proceed(id);
                    }
                }
            };
            probe.setContentType("text/plain");
            probe.setRequestBody("plain body");
            NetworkManager.getInstance().addToQueue(probe);
        });
        step(() -> {
            final int id = currentStep();
            NetworkManager.getInstance().addToQueue(new Probe(url("/api/status/503"), "GET") {
                @Override
                protected void answered() {
                    if (expect(code == 503, "503 read as " + code)
                            && expect("status 503".equals(text()), "503 body " + text())) {
                        proceed(id);
                    }
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            NetworkManager.getInstance().addToQueue(new Probe(url("/api/redirect/2"), "GET") {
                @Override
                protected void answered() {
                    if (expect(code == 200 && "landed".equals(text()),
                            "followed redirects to " + code + " " + text())) {
                        proceed(id);
                    }
                }
            });
        });
        if (!isBrowser()) {
            // The browser follows a redirect before a page's code can see it, so
            // not following one is something only the native ports can do.
            step(() -> {
                final int id = currentStep();
                Probe probe = new Probe(url("/api/redirect/1"), "GET") {
                    @Override
                    protected void answered() {
                        if (expect(code == 302, "an unfollowed redirect answered " + code)) {
                            proceed(id);
                        }
                    }
                };
                probe.setFollowRedirects(false);
                NetworkManager.getInstance().addToQueue(probe);
            });
            // Cookies and the session they carry. On the browser both are the
            // browser's: a cross-origin request carries no cookies unless the page
            // asks for credentials, which is browser policy, not Codename One's.
            step(() -> {
                final int id = currentStep();
                NetworkManager.getInstance().addToQueue(
                        new Probe(url("/api/cookie/set?name=flavor&value=oat"), "GET") {
                            @Override
                            protected void answered() {
                                if (expect(code == 200, "cookie set answered " + code)) {
                                    proceed(id);
                                }
                            }
                        });
            });
            step(() -> {
                final int id = currentStep();
                NetworkManager.getInstance().addToQueue(
                        new Probe(url("/api/cookie/read?name=flavor"), "GET") {
                            @Override
                            protected void answered() {
                                if (expect("oat".equals(text()), "the cookie came back as " + text())) {
                                    proceed(id);
                                }
                            }
                        });
            });
            step(() -> sessionCount(1));
            step(() -> sessionCount(2));
        }
        step(() -> {
            final int id = currentStep();
            Probe probe = new Probe(url("/api/slow?ms=4000"), "GET") {
                @Override
                protected void answered() {
                    expect(false, "a request past its timeout answered " + code + " " + text());
                }

                @Override
                protected void failed(Exception err) {
                    proceed(id);
                }
            };
            probe.setTimeout(1000);
            probe.setReadTimeout(1000);
            NetworkManager.getInstance().addToQueue(probe);
        });
    }

    private int sessionBase = -1;

    /// Asks for the session's counter: the first call fixes the base, the next must
    /// be one more -- which only holds when the session cookie went back.
    private void sessionCount(final int call) {
        final int id = currentStep();
        NetworkManager.getInstance().addToQueue(new Probe(url("/api/session/count"), "GET") {
            @Override
            protected void answered() {
                int value;
                try {
                    value = Integer.parseInt(text().trim());
                } catch (NumberFormatException err) {
                    expect(false, "session count was " + text());
                    return;
                }
                if (call == 1) {
                    sessionBase = value;
                    proceed(id);
                } else if (expect(value == sessionBase + 1, "the session counter went from "
                        + sessionBase + " to " + value + ": the session cookie was not sent")) {
                    proceed(id);
                }
            }
        });
    }

    /// A request that reads its answer whatever the status, and reports once. An
    /// inner class, so a request that fails reports through the test instead of
    /// throwing on a thread nothing catches -- which left the test hanging.
    abstract class Probe extends ConnectionRequest implements ReportsOwnErrors {
        int code;
        byte[] data;
        String probeHeader;
        private boolean reported;

        Probe(String url, String method) {
            setUrl(url);
            setHttpMethod(method);
            setPost(!"GET".equals(method) && !"HEAD".equals(method) && !"DELETE".equals(method));
            setReadResponseForErrors(true);
            // NOT fail-silent: NetworkManager hands a fail-silent request's exception
            // to nobody -- not even an overridden handleException -- so a timeout or
            // a refused connection ended the request with no callback at all. The
            // overrides below are what keep the default error dialog away.
            setDuplicateSupported(true);
        }

        @Override
        protected void readHeaders(Object connection) throws IOException {
            probeHeader = getHeader(connection, "X-Probe");
        }

        @Override
        protected void handleErrorResponseCode(int responseCode, String message) {
            code = responseCode;
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            if (code == 0) {
                code = getResponseCode();
            }
            data = com.codename1.io.Util.readInputStream(input);
        }

        @Override
        protected void postResponse() {
            if (code == 0) {
                code = getResponseCode();
            }
            if (!reported) {
                reported = true;
                answered();
            }
        }

        @Override
        protected void handleException(final Exception err) {
            // Called on the NETWORK thread, unlike postResponse: the step runner
            // and the test's completion belong on the EDT. Android tolerated the
            // network-thread call; the browser port left the test unfinished.
            com.codename1.ui.CN.callSerially(new Runnable() {
                public void run() {
                    if (!reported) {
                        reported = true;
                        failed(err);
                    }
                }
            });
        }

        String text() {
            try {
                return data == null ? "" : new String(data, "UTF-8");
            } catch (java.io.UnsupportedEncodingException err) {
                return "";
            }
        }

        protected abstract void answered();

        protected void failed(Exception err) {
            failStep("request to " + getUrl() + " failed: " + err);
        }
    }
}
