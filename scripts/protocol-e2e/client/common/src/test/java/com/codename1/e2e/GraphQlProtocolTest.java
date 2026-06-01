package com.codename1.e2e;

import com.codename1.e2e.graphql.AddReviewData;
import com.codename1.e2e.graphql.Episode;
import com.codename1.e2e.graphql.HeroData;
import com.codename1.e2e.graphql.StarWarsApi;
import com.codename1.io.graphql.GraphQLResponse;
import com.codename1.testing.AbstractTest;
import com.codename1.util.OnComplete;

/** End-to-end GraphQL query + mutation round-trips against the test server. */
public class GraphQlProtocolTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        StarWarsApi api = StarWarsApi.of(E2eSupport.baseUrl() + "/graphql");

        // --- query ---
        final HeroData[] hero = new HeroData[1];
        final boolean[] heroOk = { false };
        final boolean[] heroDone = { false };
        api.hero("Luke", null, new OnComplete<GraphQLResponse<HeroData>>() {
            public void completed(GraphQLResponse<HeroData> r) {
                heroOk[0] = r.isOk();
                hero[0] = r.getData();
                heroDone[0] = true;
            }
        });
        E2eSupport.await(heroDone);
        assertTrue(heroDone[0], "GraphQL query timed out");
        assertTrue(heroOk[0], "GraphQL query reported errors");
        assertNotNull(hero[0], "GraphQL data null");
        assertNotNull(hero[0].hero, "GraphQL hero null");
        assertEqual("Luke", hero[0].hero.name, "GraphQL hero.name");
        assertEqual("Hello, Luke!", hero[0].hero.greeting, "GraphQL hero.greeting");

        // --- mutation (enum + int variables) ---
        final AddReviewData[] review = new AddReviewData[1];
        final boolean[] reviewOk = { false };
        final boolean[] reviewDone = { false };
        api.addReview(Episode.JEDI, 5, null, new OnComplete<GraphQLResponse<AddReviewData>>() {
            public void completed(GraphQLResponse<AddReviewData> r) {
                reviewOk[0] = r.isOk();
                review[0] = r.getData();
                reviewDone[0] = true;
            }
        });
        E2eSupport.await(reviewDone);
        assertTrue(reviewDone[0], "GraphQL mutation timed out");
        assertTrue(reviewOk[0], "GraphQL mutation reported errors");
        assertNotNull(review[0], "GraphQL mutation data null");
        assertNotNull(review[0].addReview, "GraphQL addReview null");
        assertEqual(5, review[0].addReview.stars.intValue(), "GraphQL addReview.stars");
        assertEqual("JEDI rated 5", review[0].addReview.commentary, "GraphQL addReview.commentary");
        return true;
    }

    @Override
    public int getTimeoutMillis() {
        return 90000;
    }
}
