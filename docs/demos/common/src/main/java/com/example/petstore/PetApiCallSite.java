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
package com.example.petstore;

import com.codename1.components.ToastBar;

/// Call site for the `@RestClient` interface `cn1:generate-openapi` emits.
/// Included by the developer guide's `generate-openapi` appendix.
class PetApiCallSite {

    // tag::appendix-goal-generate-openapi-java-003[]
    void loadPet(String bearerToken) {
        PetApi api = PetApi.of("https://petstore3.swagger.io/api/v3");

        api.getPetById(10L, bearerToken, response -> {
            // Two things have to be settled before the payload is a Pet.
            // The generated impl passes null when the request never
            // completed, and on an error status it forwards the raw
            // Response<String> under this type -- so on that path
            // getResponseData() is the error body, not a Pet, and
            // touching it as one is a cast that does not throw on iOS.
            if (response == null) {
                ToastBar.showErrorMessage("The request did not complete");
            } else if (response.getResponseCode() < 200 || response.getResponseCode() > 299) {
                ToastBar.showErrorMessage(response.getResponseErrorMessage());
            } else if (response.getResponseData() == null) {
                ToastBar.showErrorMessage("The server returned no pet");
            } else {
                ToastBar.showInfoMessage(response.getResponseData().name());
            }
        });
    }
    // end::appendix-goal-generate-openapi-java-003[]
}
