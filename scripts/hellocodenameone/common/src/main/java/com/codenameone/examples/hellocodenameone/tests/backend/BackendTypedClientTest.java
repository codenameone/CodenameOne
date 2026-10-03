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

import java.util.List;

/// The generated `@RestClient` against the backend's typed API: DTOs out and in,
/// path and query parameters, a header, a list, and the whole create-read-update-
/// delete cycle. The app and the server declare their own classes, so this is
/// where a field one side renamed shows up.
public class BackendTypedClientTest extends BackendClientTest {
    private PetsApi api;
    private Long created;

    @Override
    protected void defineSteps() {
        api = PetsApi.of(baseUrl());
        created = null;
        step(() -> {
            final int id = currentStep();
            api.hello("Grace", r -> {
                if (expect(r != null && "Hello, Grace".equals(r.getResponseData()),
                        "typed hello answered " + (r == null ? null : r.getResponseData()))) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            api.create(new PetDto("Rex", "dog", 3), r -> {
                PetDto pet = r == null ? null : r.getResponseData();
                if (expect(r != null && r.getResponseCode() == 201, "create answered "
                        + (r == null ? null : Integer.valueOf(r.getResponseCode())))
                        && expect(pet != null && pet.id != null && "Rex".equals(pet.name),
                        "created " + describe(pet))) {
                    created = pet.id;
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            api.get(created, r -> {
                PetDto pet = r == null ? null : r.getResponseData();
                if (expect(pet != null && "dog".equals(pet.species)
                        && pet.age != null && pet.age.intValue() == 3,
                        "read back " + describe(pet))) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            PetDto older = new PetDto("Rex", "dog", 4);
            older.vaccinated = Boolean.TRUE;
            api.replace(created, older, r -> {
                PetDto pet = r == null ? null : r.getResponseData();
                if (expect(pet != null && pet.age != null && pet.age.intValue() == 4
                        && Boolean.TRUE.equals(pet.vaccinated), "replaced " + describe(pet))) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            api.list(r -> {
                List<PetDto> pets = r == null ? null : r.getResponseData();
                boolean found = false;
                for (int iter = 0 ; pets != null && iter < pets.size() ; iter++) {
                    found |= created.equals(pets.get(iter).id);
                }
                if (expect(found, "the list did not hold pet " + created)) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            api.echo("typed", "header-value", r -> {
                String body = r == null ? null : r.getResponseData();
                if (expect(body != null && body.indexOf("\"q\":\"typed\"") >= 0
                        && body.indexOf("header-value") >= 0, "typed echo " + body)) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            api.delete(created, r -> {
                if (expect(r != null && r.getResponseCode() == 204, "delete answered "
                        + (r == null ? null : Integer.valueOf(r.getResponseCode())))) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            api.get(created, r -> {
                if (expect(r != null && r.getResponseCode() == 404, "a deleted pet answered "
                        + (r == null ? null : Integer.valueOf(r.getResponseCode())))) {
                    proceed(id);
                }
            });
        });
    }

    private static String describe(PetDto pet) {
        return pet == null ? "null" : "{id=" + pet.id + ", name=" + pet.name + ", species="
                + pet.species + ", age=" + pet.age + ", vaccinated=" + pet.vaccinated + "}";
    }
}
