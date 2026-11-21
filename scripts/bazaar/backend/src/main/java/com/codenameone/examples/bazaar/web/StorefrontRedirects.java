/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.SecurityFilter;
import com.codenameone.examples.bazaar.content.Navigation;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.Stores;
import java.io.IOException;

/// Follows a store's permanent redirects (`content.Navigation`): a `GET` or
/// `HEAD` under `/s/{handle}/` that the storefront answered 404 is looked up
/// in that store's table and, when it moved, answered `301` to the new
/// address instead. Only a 404 is ever replaced, so a redirect can never
/// shadow a live page, and only the store named in the path is consulted,
/// so one store's table cannot move another's pages. Targets are paths on
/// this site (checked when the redirect is saved), so this is no open
/// redirect.
@Component
public class StorefrontRedirects {
    private final Navigation navigation;
    private final StoreRepository stores;

    public StorefrontRedirects(Navigation navigation, StoreRepository stores) {
        this.navigation = navigation;
        this.stores = stores;
    }

    /// The filter for the shop chain.
    public SecurityFilter filter() {
        return (request, chain) -> {
            HttpServer.Response response;
            try {
                response = chain.doFilter(request);
            } catch (com.codename1.backend.ResponseStatusException missing) {
                // A page that is not found may be thrown rather than answered.
                if (missing.getStatus() != 404 || !readable(request)) {
                    throw missing;
                }
                String to = target(request.getTarget());
                if (to == null) {
                    throw missing;
                }
                return moved(to);
            }
            // No response at all is an address no handler matched: the
            // server answers it 404 after the chain.
            if (response != null && response.getStatus() != 404 || !readable(request)) {
                return response;
            }
            String to = target(request.getTarget());
            if (to == null) {
                return response;
            }
            return moved(to);
        };
    }

    private static boolean readable(HttpServer.Request request) {
        return "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
    }

    static HttpServer.Response moved(String to) {
        return HttpServer.Response.text(301, "")
                .header("Location", to)
                .header("Cache-Control", "public, max-age=3600");
    }

    /// Where `target` moved, or null.
    public String target(String target) throws IOException {
        if (target == null || !target.startsWith("/s/") || target.length() > 400) {
            return null;
        }
        int q = target.indexOf('?');
        String path = q < 0 ? target : target.substring(0, q);
        int slash = path.indexOf('/', 3);
        if (slash < 0 || slash == path.length() - 1) {
            return null;
        }
        Store store = stores.findByHandle(path.substring(3, slash));
        if (store == null || !Stores.ACTIVE.equals(store.getStatus())) {
            return null;
        }
        return navigation.follow(store.getId(), path);
    }
}
