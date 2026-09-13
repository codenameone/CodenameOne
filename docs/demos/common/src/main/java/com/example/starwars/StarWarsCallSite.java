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
            // A GraphQL response can carry data and errors at once, so isOk()
            // reports whether the errors array came back empty rather than
            // whether there is anything in getData().
            if (response.isOk()) {
                ToastBar.showInfoMessage(response.getData().hero().name());
            } else {
                ToastBar.showErrorMessage(response.getResponseErrorMessage());
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
                ToastBar.showInfoMessage(response.getData().reviewAdded().stars() + " stars");
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
