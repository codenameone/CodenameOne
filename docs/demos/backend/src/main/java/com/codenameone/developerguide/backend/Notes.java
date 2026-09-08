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

import com.codename1.backend.annotations.DeleteMapping;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestBody;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.ResponseStatus;
import com.codename1.backend.annotations.RestController;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// tag::backend-first-server[]
@RestController
@RequestMapping("/notes")
public class Notes {
    private final Map<Long, Map> store = new ConcurrentHashMap<Long, Map>();
    private final AtomicLong nextId = new AtomicLong(1);

    @GetMapping("/healthz")
    public String health() {
        return "ok";
    }

    @GetMapping("/{id}")
    public Map read(@PathVariable("id") long id) {
        return store.get(Long.valueOf(id));      // null becomes a 404
    }

    @GetMapping
    public List list(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        List page = new ArrayList();
        for (Map note : store.values()) {
            if (page.size() >= limit) {
                break;
            }
            page.add(note);
        }
        return page;
    }

    @PostMapping
    @ResponseStatus(201)
    public Map create(@RequestBody Map note) {
        long id = nextId.getAndIncrement();
        Map stored = new LinkedHashMap(note);
        stored.put("id", Long.valueOf(id));
        store.put(Long.valueOf(id), stored);
        return stored;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(204)
    public void delete(@PathVariable("id") long id) {
        store.remove(Long.valueOf(id));
    }
}
// end::backend-first-server[]
