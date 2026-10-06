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
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.RequestSecurity;
import java.util.List;

/// The security layer of one server: its chains, in order, and what the request
/// path asks of them. The server holds it as a [RequestSecurity].
final class FilterChainEngine implements RequestSecurity {
    /// What the end of a chain answers for a WebSocket handshake it admits.
    private static final HttpServer.Response ADMITTED = HttpServer.Response.text(200, "");

    private final SecurityFilterChain[] chains;
    private final boolean tls;

    FilterChainEngine(Config config, List chains, boolean tls) {
        this.chains = new SecurityFilterChain[chains.size()];
        for (int iter = 0 ; iter < this.chains.length ; iter++) {
            Object chain = chains.get(iter);
            if (!(chain instanceof SecurityFilterChain)) {
                throw new IllegalArgumentException("Not a SecurityFilterChain: " + chain);
            }
            this.chains[iter] = (SecurityFilterChain) chain;
            if (iter > 0 && this.chains[iter - 1] instanceof DefaultSecurityFilterChain
                    && ((DefaultSecurityFilterChain) this.chains[iter - 1]).getRequestMatcher()
                    == AnyRequestMatcher.INSTANCE) { //NOPMD CompareObjectsWithEquals - the singleton
                throw new IllegalStateException("A filter chain that matches any request has "
                        + "already been configured, which means that this filter chain ["
                        + chain + "] will never be invoked. Please use "
                        + "`HttpSecurity#securityMatcher` to ensure that there is only one "
                        + "filter chain configured for 'any request' and that the 'any "
                        + "request' filter chain is published last.");
            }
        }
        this.tls = tls;
        com.codename1.impl.backend.security.SecuritySupport.developmentProfile(
                config != null && config.isDevelopmentProfile());
    }

    private SecurityFilterChain match(HttpServer.Request request) {
        for (SecurityFilterChain chain : chains) {
            if (chain.matches(request)) {
                return chain;
            }
        }
        return null;
    }

    @Override
    public HttpServer.Response serve(HttpServer.Request request, Next next)
            throws Exception {
        SecurityFilterChain chain = begin(request);
        if (chain == null) {
            if (SecurityExchange.of(request) != null) {
                return Responses.status(400, "Bad Request");
            }
            return next.route(request);
        }
        return run(chain, request, new Routing(next));
    }

    /// The end of a chain for an HTTP request: the application's routers.
    private static final class Routing implements FilterChain {
        private final Next next;

        Routing(Next next) {
            this.next = next;
        }

        @Override
        public HttpServer.Response doFilter(HttpServer.Request request) throws Exception {
            return next.route(request);
        }
    }

    /// The end of a chain for a WebSocket handshake: nothing left to refuse it.
    private static final FilterChain ADMIT = new Admitting();

    private static final class Admitting implements FilterChain {
        @Override
        public HttpServer.Response doFilter(HttpServer.Request request) {
            return ADMITTED;
        }
    }

    @Override
    public int upgrade(HttpServer.Request request) throws Exception {
        SecurityFilterChain chain = begin(request);
        if (chain == null) {
            return SecurityExchange.of(request) != null ? 400 : 0;
        }
        HttpServer.Response response = run(chain, request, ADMIT);
        if (response == ADMITTED) { //NOPMD CompareObjectsWithEquals - the marker itself
            return 0;
        }
        int status = response == null ? 404 : response.getStatus();
        if (status >= 300 && status < 400) {
            // A redirect to a login page: a socket cannot follow one, and what
            // it means is "sign in first".
            return 401;
        }
        // A filter that answered the handshake itself did not admit it.
        return status < 400 ? 403 : status;
    }

    /// Starts serving `request`: the chain that guards it, or null -- with the
    /// exchange left in place when the request is refused outright, and removed
    /// when no chain claims it.
    private SecurityFilterChain begin(HttpServer.Request request) {
        // Over TLS this server terminated, or TLS a trusted proxy did: see
        // HttpServer.Request.isSecure.
        SecurityExchange exchange = new SecurityExchange(request, tls || request.isSecure());
        SecurityExchange.enter(exchange);
        if (!wellFormed(SecurityExchange.path(request))) {
            return null;
        }
        SecurityFilterChain chain = match(request);
        if (chain == null) {
            SecurityExchange.leave();
            return null;
        }
        exchange.setFilterChain(chain);
        return chain;
    }

    private HttpServer.Response run(SecurityFilterChain chain, HttpServer.Request request,
                                    FilterChain end) throws Exception {
        try {
            return new VirtualFilterChain(chain.getFilters(), end).doFilter(request);
        } catch (AuthenticationException err) {
            return outside(chain, request, err, 401, "Unauthorized");
        } catch (AccessDeniedException err) {
            return outside(chain, request, err, 403, "Forbidden");
        } catch (ServiceBusyException busy) {
            // Turned away before it cost anything: the client is told to come
            // back, and nothing about why it would have been refused.
            return Responses.status(503, "Service Unavailable")
                    .header("Retry-After", String.valueOf(busy.getRetryAfterSeconds()));
        }
    }

    /// A security exception no filter of the chain translated: thrown by a
    /// filter that runs before the translating one, or by a chain without it.
    private static HttpServer.Response outside(SecurityFilterChain chain,
            HttpServer.Request request, RuntimeException err, int status, String text)
            throws Exception {
        for (SecurityFilter filter : chain.getFilters()) {
            if (filter instanceof ExceptionTranslationFilter) {
                return ((ExceptionTranslationFilter) filter).translate(request, err);
            }
        }
        return Responses.status(status, text);
    }

    @Override
    public void decorate(HttpServer.Request request, HttpServer.Response response) {
        if (response == null) {
            return;
        }
        SecurityExchange exchange = SecurityExchange.of(request);
        // After the request's thread state is gone -- the server's own 404 and
        // 500 -- the chain is worked out again; a request refused as malformed
        // belongs to none.
        SecurityFilterChain chain = exchange != null ? exchange.getFilterChain()
                : wellFormed(SecurityExchange.path(request)) ? match(request) : null;
        if (chain == null) {
            return;
        }
        ResponseHeaders headers = new ResponseHeaders(response, tls || request.isSecure());
        List<String[]> recorded = exchange == null ? null : exchange.responseHeaders();
        if (recorded != null) {
            for (String[] header : recorded) {
                if ("add".equals(header[2])) {
                    BackendAccess.get().addHeader(response, header[0], header[1]);
                } else {
                    headers.set(header[0], header[1]);
                }
            }
        }
        for (SecurityFilter filter : chain.getFilters()) {
            if (filter instanceof HeaderWriterFilter) {
                ((HeaderWriterFilter) filter).write(request, headers);
            }
        }
    }

    @Override
    public Object enter() {
        SecurityContext context = SecurityContextHolder.peek();
        SecurityExchange exchange = SecurityExchange.current();
        // Nothing, on a thread that is only serving: no allocation for a request.
        return context == null && exchange == null ? null : new Object[] {context, exchange};
    }

    @Override
    public void leave(Object entered) {
        if (entered == null) {
            SecurityContextHolder.clearContext();
            SecurityExchange.leave();
            return;
        }
        // The thread was somebody before this request: a test running as a user
        // and sending its requests in process, or a request that dispatched
        // another. It goes back to exactly that.
        Object[] previous = (Object[]) entered;
        if (previous[0] == null) {
            SecurityContextHolder.clearContext();
        } else {
            SecurityContextHolder.setContext((SecurityContext) previous[0]);
        }
        SecurityExchange.enter((SecurityExchange) previous[1]);
    }

    /// Whether `path` is one this layer will judge. A path written to look like
    /// one thing to a rule and another to whatever serves it is refused before
    /// either sees it: a `;` parameter, a backslash, an empty or dot segment, an
    /// encoded slash, backslash, percent or NUL, or a control character.
    static boolean wellFormed(String path) {
        if (path.length() == 0 || path.charAt(0) != '/') {
            // OPTIONS * and a CONNECT authority match no path rule; let the
            // routers answer them as they always have.
            return true;
        }
        int length = path.length();
        for (int iter = 0 ; iter < length ; iter++) {
            char c = path.charAt(iter);
            if (c < 0x20 || c == 0x7f || c == ';' || c == '\\') {
                return false;
            }
            if (c == '/' && iter + 1 < length) {
                char next = path.charAt(iter + 1);
                if (next == '/') {
                    return false;
                }
                if (next == '.') {
                    int end = iter + 2;
                    if (end < length && path.charAt(end) == '.') {
                        end++;
                    }
                    if (end == length || path.charAt(end) == '/') {
                        return false;
                    }
                }
            }
            if (c == '%' && iter + 2 < length) {
                char hi = path.charAt(iter + 1);
                char lo = path.charAt(iter + 2);
                boolean slash = hi == '2' && (lo == 'f' || lo == 'F');
                boolean backslash = hi == '5' && (lo == 'c' || lo == 'C');
                boolean percent = hi == '2' && lo == '5';
                boolean nul = hi == '0' && lo == '0';
                boolean dot = hi == '2' && (lo == 'e' || lo == 'E');
                boolean semicolon = hi == '3' && (lo == 'b' || lo == 'B');
                if (slash || backslash || percent || nul || dot || semicolon) {
                    return false;
                }
            }
        }
        return true;
    }

    /// The filters of a chain, then the application: what a filter sees as the
    /// rest of the chain.
    private static final class VirtualFilterChain implements FilterChain {
        private final List<SecurityFilter> filters;
        private final FilterChain end;
        private int position;

        VirtualFilterChain(List<SecurityFilter> filters, FilterChain end) {
            this.filters = filters;
            this.end = end;
        }

        @Override
        public HttpServer.Response doFilter(HttpServer.Request request) throws Exception {
            if (position == filters.size()) {
                return end.doFilter(request);
            }
            return filters.get(position++).doFilter(request, this);
        }
    }

    /// The server's copy of a response, as a [HeaderWriter] sees it.
    private static final class ResponseHeaders implements HeaderWriter.Headers {
        private final HttpServer.Response response;
        private final boolean secure;

        ResponseHeaders(HttpServer.Response response, boolean secure) {
            this.response = response;
            this.secure = secure;
        }

        @Override
        public boolean contains(String name) {
            return BackendAccess.get().hasHeader(response, name);
        }

        @Override
        public void set(String name, String value) {
            BackendAccess.get().setHeader(response, name, value);
        }

        @Override
        public void setIfAbsent(String name, String value) {
            if (!contains(name)) {
                BackendAccess.get().setHeader(response, name, value);
            }
        }

        @Override
        public boolean isSecure() {
            return secure;
        }

        @Override
        public int getStatus() {
            return response.getStatus();
        }
    }
}
