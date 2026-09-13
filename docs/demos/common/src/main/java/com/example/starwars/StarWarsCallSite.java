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
package com.example.starwars;

import com.codename1.components.ToastBar;
import com.codename1.io.graphql.GraphQLResponse;
import com.codename1.io.graphql.GraphQLSubscription;

/// Call sites for the `@GraphQLClient` interface `cn1:generate-graphql` emits.
/// Included by the developer guide's `generate-graphql` appendix.
class StarWarsCallSite {

    // tag::appendix-goal-generate-graphql-java-002[]
    void heroName(String bearerToken) {
        StarWarsApi api = StarWarsApi.of("https://swapi.example.com/graphql");

        api.heroName(Episode.EMPIRE, bearerToken, response -> {
            // isOk() classifies the GraphQL payload, not the trip. A call that
            // never reached the server arrives with getResponseCode() == 0, an
            // empty errors array and null data -- so isOk() is true. Check the
            // HTTP code before believing it.
            boolean reachedTheServer = response.getResponseCode() >= 200
                    && response.getResponseCode() <= 299;
            if (!reachedTheServer || !response.isOk()) {
                ToastBar.showErrorMessage(response.getResponseErrorMessage());
            } else if (response.getData() == null || response.getData().hero() == null) {
                // And an error-free answer still need not have found anything:
                // the schema declares hero as Character, not Character!.
                ToastBar.showInfoMessage("No hero for that episode");
            } else {
                ToastBar.showInfoMessage(response.getData().hero().name());
            }
        });
    }
    // end::appendix-goal-generate-graphql-java-002[]

    // tag::appendix-goal-generate-graphql-java-003[]
    GraphQLSubscription watchReviews(String bearerToken) {
        StarWarsApi api = StarWarsApi.of("https://swapi.example.com/graphql");

        return api.onReview(Episode.JEDI, bearerToken,
                new GraphQLSubscription.Handler<OnReviewData>() {
            @Override
            public void onNext(GraphQLResponse<OnReviewData> response) {
                // onError is for the end of the stream. A per-field failure
                // arrives here instead, as a next payload whose errors array
                // is non-empty and whose data may be partial or absent.
                if (response.hasErrors()) {
                    ToastBar.showErrorMessage(response.getResponseErrorMessage());
                }
                OnReviewData data = response.getData();
                if (data != null && data.reviewAdded() != null) {
                    ToastBar.showInfoMessage(data.reviewAdded().stars() + " stars");
                }
            }

            @Override
            public void onError(GraphQLResponse<OnReviewData> response) {
                ToastBar.showErrorMessage(response.getResponseErrorMessage());
            }

            @Override
            public void onComplete() {
            }
        });
        // Hold on to the handle and call cancel() to end the stream --
        // leaving the form is the usual place to do it.
    }
    // end::appendix-goal-generate-graphql-java-003[]
}
