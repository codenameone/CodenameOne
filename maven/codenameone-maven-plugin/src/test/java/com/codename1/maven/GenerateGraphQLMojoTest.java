/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.maven;

import com.codename1.maven.GraphQLOperationModel.Document;
import com.codename1.maven.GraphQLSchemaModel.Schema;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Drives the GraphQL SDL + operation parsers and generator against an
/// inline Star Wars fixture, verifying both the precise operations mode
/// and the schema-only quick-start mode.
class GenerateGraphQLMojoTest {

    private static final String SCHEMA =
            "\"\"\" the root \"\"\"\n"
            + "schema { query: Query mutation: Mutation subscription: Subscription }\n"
            + "type Query {\n"
            + "  hero(episode: Episode): Character\n"
            + "  review(episode: Episode!): Review\n"
            + "  version: String\n"
            + "}\n"
            + "type Mutation { createReview(episode: Episode!, review: ReviewInput!): Review }\n"
            + "type Subscription { reviewAdded(episode: Episode!): Review }\n"
            + "type Character {\n"
            + "  id: ID!\n"
            + "  name: String!\n"
            + "  friends: [Character]\n"
            + "  appearsIn: [Episode!]!\n"
            + "}\n"
            + "type Review { stars: Int! commentary: String }\n"
            + "input ReviewInput { stars: Int! commentary: String favoriteColor: ColorInput }\n"
            + "input ColorInput { red: Int! green: Int! blue: Int! }\n"
            + "enum Episode { NEWHOPE EMPIRE JEDI }\n";

    private static final String OPERATIONS =
            "query HeroName($episode: Episode) {\n"
            + "  hero(episode: $episode) { ...HeroFields friends { name } }\n"
            + "}\n"
            + "fragment HeroFields on Character { name }\n"
            + "mutation AddReview($ep: Episode!, $review: ReviewInput!) {\n"
            + "  createReview(episode: $ep, review: $review) { stars commentary }\n"
            + "}\n"
            + "subscription OnReview($ep: Episode!) {\n"
            + "  reviewAdded(episode: $ep) { stars }\n"
            + "}\n";

    private Schema schema() {
        return new GraphQLSchemaModel.Parser(SCHEMA, "schema.graphqls").parse();
    }

    private Document ops() {
        return new GraphQLOperationModel.Parser(OPERATIONS, "ops.graphql").parse();
    }

    @Test
    void operationsModeEmitsRecordsAndInterface(@TempDir Path tmp) throws Exception {
        File out = tmp.toFile();
        new GenerateGraphQLMojo.Generator(schema(), ops(), "com.example.sw", out, true,
                /*emitRecords*/ true, "https://api/graphql", "StarWarsApi", 2,
                new SystemStreamLog()).run();

        String hero = read(out, "com/example/sw/HeroNameData.java");
        assertTrue(hero.contains("public record HeroNameData("), "data root record; was:\n" + hero);
        assertTrue(hero.contains("HeroNameData_Hero hero"), "nested object field; was:\n" + hero);

        String heroHero = read(out, "com/example/sw/HeroNameData_Hero.java");
        assertTrue(heroHero.contains("String name"), "fragment field flattened in; was:\n" + heroHero);
        assertTrue(heroHero.contains("List<HeroNameData_Hero_Friends> friends"),
                "list of nested objects; was:\n" + heroHero);
        assertTrue(heroHero.contains("import java.util.List;"), "List import present");

        String review = read(out, "com/example/sw/ReviewInput.java");
        assertTrue(review.contains("@Mapped"), "input is @Mapped; was:\n" + review);
        assertTrue(review.contains("Integer stars"), "Int! -> Integer; was:\n" + review);
        assertTrue(review.contains("ColorInput favoriteColor"), "nested input class; was:\n" + review);
        assertTrue(new File(out, "com/example/sw/ColorInput.java").exists(),
                "nested input ColorInput emitted");

        String episode = read(out, "com/example/sw/Episode.java");
        assertTrue(episode.contains("public enum Episode"), "enum emitted; was:\n" + episode);
        assertTrue(episode.contains("NEWHOPE"), "enum values; was:\n" + episode);

        String api = read(out, "com/example/sw/StarWarsApi.java");
        assertTrue(api.contains("@GraphQLClient(\"https://api/graphql\")"), "client anno; was:\n" + api);
        assertTrue(api.contains("query HeroName($episode: Episode) { hero(episode: $episode) "
                        + "{ ...HeroFields friends { name } } }"),
                "minified operation document embedded; was:\n" + api);
        assertTrue(api.contains("fragment HeroFields on Character { name }"),
                "referenced fragment appended to document; was:\n" + api);
        assertTrue(api.contains("@Var(\"episode\") Episode episode"), "typed enum variable; was:\n" + api);
        assertTrue(api.contains("@Header(\"Authorization\") String bearerToken"),
                "bearer token param; was:\n" + api);
        assertTrue(api.contains("OnComplete<GraphQLResponse<HeroNameData>> callback"),
                "query callback type; was:\n" + api);
        assertTrue(api.contains("@Var(\"review\") ReviewInput review"),
                "input-object variable param; was:\n" + api);
        assertTrue(api.contains("GraphQLSubscription onReview("),
                "subscription returns handle; was:\n" + api);
        assertTrue(api.contains("GraphQLSubscription.Handler<OnReviewData> handler"),
                "subscription handler param; was:\n" + api);
        assertTrue(api.contains("static StarWarsApi of(String endpoint)"), "of factory; was:\n" + api);
        assertTrue(api.contains("GraphQLClients.create(StarWarsApi.class, endpoint)"),
                "of delegates to registry");
    }

    @Test
    void operationsModeEmitsClassesOnJava8(@TempDir Path tmp) throws Exception {
        File out = tmp.toFile();
        new GenerateGraphQLMojo.Generator(schema(), ops(), "com.example.sw", out, true,
                /*emitRecords*/ false, "", "StarWarsApi", 2, new SystemStreamLog()).run();
        String hero = read(out, "com/example/sw/HeroNameData.java");
        assertTrue(hero.contains("public class HeroNameData {"), "class form on Java 8; was:\n" + hero);
        assertTrue(hero.contains("public HeroNameData_Hero hero;"), "public field; was:\n" + hero);
    }

    @Test
    void schemaOnlyModeAutoExpandsBoundedDepth(@TempDir Path tmp) throws Exception {
        File out = tmp.toFile();
        new GenerateGraphQLMojo.Generator(schema(), /*operations*/ null, "com.example.sw", out, true,
                true, "", "StarWarsApi", 2, new SystemStreamLog()).run();

        String api = read(out, "com/example/sw/StarWarsApi.java");
        assertTrue(api.contains("hero("), "root query field hero; was:\n" + api);
        assertTrue(api.contains("version("), "scalar root field version; was:\n" + api);
        assertTrue(api.contains("GraphQLSubscription reviewAdded("),
                "subscription root field; was:\n" + api);
        assertTrue(api.contains("hero(episode: $episode)"),
                "auto-selection wires the field argument; was:\n" + api);

        String heroData = read(out, "com/example/sw/HeroQueryData_Hero.java");
        assertTrue(heroData.contains("id") && heroData.contains("name"),
                "scalar fields expanded; was:\n" + heroData);
        assertTrue(heroData.contains("List<Episode> appearsIn"),
                "[Episode!]! expands to List<Episode> (enum now bindable); was:\n" + heroData);
        assertFalse(heroData.contains("friends"),
                "recursive Character field omitted at depth/cycle limit; was:\n" + heroData);
    }

    @Test
    void minifyStripsCommentsAndCollapsesWhitespace() {
        String in = "query Q {\n  # a comment\n  field\n}\n";
        assertEquals("query Q { field }", GenerateGraphQLMojo.Generator.minify(in));
    }

    @Test
    void minifyPreservesStringLiterals() {
        String in = "mutation { set(text: \"a  b\")  }";
        assertEquals("mutation { set(text: \"a  b\") }", GenerateGraphQLMojo.Generator.minify(in));
    }

    @Test
    void javaNameConvertsSnakeAndReserved() {
        assertEquals("fooBar", GenerateGraphQLMojo.Generator.javaName("foo_bar"));
        assertEquals("class_", GenerateGraphQLMojo.Generator.javaName("class"));
    }

    private static String read(File outDir, String rel) throws Exception {
        File f = new File(outDir, rel);
        assertTrue(f.exists(), "expected generated file " + rel);
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
