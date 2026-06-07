package com.example.e2eserver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/** GraphQL transport, backed by Spring for GraphQL (schema.graphqls). */
@Controller
public class GraphQlCatalogController {

    @QueryMapping
    public List<Catalog.Product> products() {
        return Catalog.all();
    }

    @QueryMapping
    public Catalog.Product product(@Argument String id) {
        return Catalog.byId(Long.parseLong(id));
    }

    @QueryMapping
    public List<Catalog.Product> productsByCategory(@Argument Catalog.Category category) {
        return Catalog.byCategory(category);
    }
}
