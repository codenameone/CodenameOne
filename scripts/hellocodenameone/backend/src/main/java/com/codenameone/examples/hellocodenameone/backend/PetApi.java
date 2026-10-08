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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.DeleteMapping;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.PutMapping;
import com.codename1.backend.annotations.RequestBody;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.ResponseStatus;
import com.codename1.backend.annotations.RestController;

import java.util.List;

/// The typed API: what the app's generated @RestClient calls, with DTO bodies the
/// build writes codecs for on both sides.
@RestController
@RequestMapping("/api/pets")
public class PetApi {
    private final PetStore store;

    public PetApi(PetStore store) {
        this.store = store;
    }

    @GetMapping
    public List<Pet> list() {
        return store.all();
    }

    @GetMapping("/{id}")
    public Pet get(@PathVariable("id") long id) {
        return store.get(id);
    }

    @PostMapping
    @ResponseStatus(201)
    public Pet create(@RequestBody Pet pet) {
        return store.add(pet);
    }

    @PutMapping("/{id}")
    public Pet replace(@PathVariable("id") long id, @RequestBody Pet pet) {
        return store.replace(id, pet);
    }

    @DeleteMapping("/{id}")
    public HttpServer.Response delete(@PathVariable("id") long id) {
        return store.remove(id) ? HttpServer.Response.empty(204, null, null)
                : HttpServer.Response.text(404, "no pet " + id);
    }
}
