package com.example.e2eserver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL transport, backed by Spring for GraphQL. The CN1 @GraphQLClient
 * generated from schema.graphqls posts queries/mutations to /graphql.
 */
@Controller
public class GraphQlGreetingController {

    public record Hero(String name, String greeting) { }

    public record Review(int stars, String commentary) { }

    public enum Episode { NEWHOPE, EMPIRE, JEDI }

    @QueryMapping
    public Hero hero(@Argument String name) {
        String n = (name == null || name.isEmpty()) ? "world" : name;
        return new Hero(n, "Hello, " + n + "!");
    }

    @MutationMapping
    public Review addReview(@Argument Episode episode, @Argument int stars) {
        return new Review(stars, episode + " rated " + stars);
    }
}
