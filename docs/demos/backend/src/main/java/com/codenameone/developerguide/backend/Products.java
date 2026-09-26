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
package com.codenameone.developerguide.backend;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.DeleteMapping;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestBody;
import com.codename1.backend.annotations.RequestHeader;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.ResponseStatus;
import com.codename1.backend.annotations.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// tag::backend-web-mappings[]
@RestController
@RequestMapping("/api/products")
public class Products {
    private final Map<Long, Map<String, Object>> products =
            new LinkedHashMap<Long, Map<String, Object>>();

    @GetMapping("/{id}")                                       // GET /api/products/42
    public synchronized Map<String, Object> get(@PathVariable("id") long id) {
        return products.get(Long.valueOf(id));                 // null answers 404
    }

    @GetMapping("/search")                                     // GET /api/products/search?q=mug
    public synchronized List<Map<String, Object>> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> p : products.values()) {
            if (out.size() < limit
                    && (query == null || String.valueOf(p.get("name")).contains(query))) {
                out.add(p);
            }
        }
        return out;
    }

    @PostMapping                                               // POST /api/products
    @ResponseStatus(201)
    public synchronized Map<String, Object> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        Long id = Long.valueOf(products.size() + 1);
        Map<String, Object> product = new LinkedHashMap<String, Object>(body);
        product.put("id", id);
        products.put(id, product);
        return product;
    }

    @DeleteMapping("/{id}")                                    // void answers 204
    public synchronized void delete(@PathVariable("id") long id) {
        products.remove(Long.valueOf(id));
    }

    @GetMapping("/{id}/label")
    public HttpServer.Response label(HttpServer.Request request, @PathVariable("id") long id) {
        byte[] text = ("product " + id).getBytes();
        return request.respond(200, "text/plain; charset=utf-8", text);
    }
}
// end::backend-web-mappings[]
