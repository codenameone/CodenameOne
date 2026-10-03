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

import com.codename1.backend.annotations.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// The pets, in memory: the server is a test fixture and starts empty each run.
@Component
public class PetStore {
    private final Map pets = new TreeMap();
    private long nextId = 1;

    public synchronized Pet add(Pet pet) {
        Pet stored = copy(pet);
        stored.id = nextId++;
        pets.put(Long.valueOf(stored.id), stored);
        return copy(stored);
    }

    public synchronized Pet get(long id) {
        Pet pet = (Pet) pets.get(Long.valueOf(id));
        return pet == null ? null : copy(pet);
    }

    public synchronized List all() {
        List out = new ArrayList();
        for (Object pet : pets.values()) {
            out.add(copy((Pet) pet));
        }
        return out;
    }

    public synchronized Pet replace(long id, Pet pet) {
        if (!pets.containsKey(Long.valueOf(id))) {
            return null;
        }
        Pet stored = copy(pet);
        stored.id = id;
        pets.put(Long.valueOf(id), stored);
        return copy(stored);
    }

    public synchronized boolean remove(long id) {
        return pets.remove(Long.valueOf(id)) != null;
    }

    private static Pet copy(Pet pet) {
        Pet out = new Pet(pet.name, pet.species, pet.age);
        out.id = pet.id;
        out.vaccinated = pet.vaccinated;
        return out;
    }
}
