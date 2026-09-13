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
            // isOk() reports that the errors array came back empty, which is
            // not the same as the query having found anything: the schema
            // declares hero as Character, not Character!, so a successful
            // response can still carry a null selection.
            if (!response.isOk()) {
                ToastBar.showErrorMessage(response.getResponseErrorMessage());
            } else if (response.getData() == null || response.getData().hero() == null) {
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
