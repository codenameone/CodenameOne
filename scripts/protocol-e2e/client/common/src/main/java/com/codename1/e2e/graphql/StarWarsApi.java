// Mirrors the output of cn1:generate-graphql for the e2e schema + operations.
package com.codename1.e2e.graphql;

import com.codename1.annotations.graphql.GraphQLClient;
import com.codename1.annotations.graphql.Mutation;
import com.codename1.annotations.graphql.Query;
import com.codename1.annotations.graphql.Var;
import com.codename1.annotations.rest.Header;
import com.codename1.io.graphql.GraphQLClients;
import com.codename1.io.graphql.GraphQLResponse;
import com.codename1.util.OnComplete;

@GraphQLClient("http://localhost:8080/graphql")
public interface StarWarsApi {

    @Query("query Hero($name: String) { hero(name: $name) { name greeting } }")
    void hero(@Var("name") String name,
              @Header("Authorization") String bearerToken,
              OnComplete<GraphQLResponse<HeroData>> callback);

    @Mutation("mutation AddReview($episode: Episode!, $stars: Int!) { addReview(episode: $episode, stars: $stars) { stars commentary } }")
    void addReview(@Var("episode") Episode episode, @Var("stars") int stars,
                   @Header("Authorization") String bearerToken,
                   OnComplete<GraphQLResponse<AddReviewData>> callback);

    static StarWarsApi of(String endpoint) {
        return GraphQLClients.create(StarWarsApi.class, endpoint);
    }
}
