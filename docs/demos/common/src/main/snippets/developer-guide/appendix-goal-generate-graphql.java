// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::appendix-goal-generate-graphql-java-001[]
@GraphQLClient("https://api.example.com/graphql")
public interface StarWarsApi {

    @Query("query HeroName($episode: Episode) { hero(episode: $episode) { name } }")
    void heroName(@Var("episode") Episode episode,
                  @Header("Authorization") String bearerToken,
                  OnComplete<GraphQLResponse<HeroNameData>> callback);

    @Mutation("mutation AddReview($ep: Episode!, $review: ReviewInput!) { createReview(episode: $ep, review: $review) { stars } }")
    void addReview(@Var("ep") Episode ep, @Var("review") ReviewInput review,
                   @Header("Authorization") String bearerToken,
                   OnComplete<GraphQLResponse<AddReviewData>> callback);

    @Subscription("subscription OnReview($ep: Episode!) { reviewAdded(episode: $ep) { stars } }")
    GraphQLSubscription onReview(@Var("ep") Episode ep,
                                 @Header("Authorization") String bearerToken,
                                 GraphQLSubscription.Handler<OnReviewData> handler);

    static StarWarsApi of(String endpoint) {
        return GraphQLClients.create(StarWarsApi.class, endpoint);
    }
}
// end::appendix-goal-generate-graphql-java-001[]

// tag::appendix-goal-generate-graphql-java-002[]
StarWarsApi api = StarWarsApi.of("https://api.example.com/graphql");
api.heroName(Episode.JEDI, "Bearer " + token, response -> {
    if (response.isOk()) {
        renderHero(response.getData().hero().name());
    } else {
        showError(response.getResponseErrorMessage());
    }
});
// end::appendix-goal-generate-graphql-java-002[]

// tag::appendix-goal-generate-graphql-java-003[]
GraphQLSubscription sub = api.onReview(Episode.EMPIRE, "Bearer " + token,
        new GraphQLSubscription.Handler<OnReviewData>() {
            public void onNext(GraphQLResponse<OnReviewData> r) {
                addReview(r.getData().reviewAdded().stars());
            }
            public void onError(GraphQLResponse<OnReviewData> r) {
                showError(r.getResponseErrorMessage());
            }
            public void onComplete() { }
        });
// later:
sub.cancel();
// end::appendix-goal-generate-graphql-java-003[]
