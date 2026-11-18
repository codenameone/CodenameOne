/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.graphql;

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.security.AccessDeniedException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphQLTest {
    static final String SDL =
        "\"The root\" type Query {\n"
      + "  book(id: ID!): Book\n"
      + "  books(genre: Genre, first: Int = 2): [Book!]!\n"
      + "  search(text: String!): [SearchResult]\n"
      + "  strict: Book!\n"
      + "  failing: String\n"
      + "  forbidden: String\n"
      + "  missing: String\n"
      + "  echo(input: BookInput!): String\n"
      + "  old: String @deprecated(reason: \"use book\")\n"
      + "}\n"
      + "type Mutation { add(title: String!): Book! }\n"
      + "type Subscription { ticks(count: Int!): Int! }\n"
      + "enum Genre { FICTION SCIENCE }\n"
      + "interface Node { id: ID! }\n"
      + "type Book implements Node { id: ID! title: String genre: Genre pages: Int\n"
      + "  author: Author! }\n"
      + "type Author implements Node { id: ID! name: String! }\n"
      + "union SearchResult = Book | Author\n"
      + "input BookInput { title: String! pages: Int = 100 tags: [String] }\n";

    static Map<String, Object> book(String id, String title, String genre, int pages) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("title", title);
        m.put("genre", genre);
        m.put("pages", Integer.valueOf(pages));
        return m;
    }

    final List<Map<String, Object>> books = new ArrayList<Map<String, Object>>(Arrays.asList(
            book("1", "Dune", "FICTION", 412),
            book("2", "Cosmos", "SCIENCE", 365),
            book("3", "Emma", "FICTION", 474)));

    GraphQL.Builder builder() {
        return GraphQL.newGraphQL(GraphQLSchema.parse(SDL))
            .fetcher("Query", "book", env -> {
                for (Map<String, Object> b : books) {
                    if (b.get("id").equals(env.getArgument("id"))) {
                        return b;
                    }
                }
                return null;
            })
            .fetcher("Query", "books", env -> {
                List<Object> out = new ArrayList<Object>();
                for (Map<String, Object> b : books) {
                    if (env.getArgument("genre") == null || b.get("genre").equals(env.getArgument("genre"))) {
                        out.add(b);
                    }
                }
                int first = ((Integer) env.getArgument("first")).intValue();
                return out.subList(0, Math.min(first, out.size()));
            })
            .fetcher("Query", "search", env -> {
                Map<String, Object> author = new LinkedHashMap<String, Object>();
                author.put("__typename", "Author");
                author.put("id", "a1");
                author.put("name", "Herbert");
                return Arrays.asList(books.get(0), author);
            })
            .fetcher("Query", "strict", env -> null)
            .fetcher("Query", "failing", env -> {
                throw new IllegalStateException("database password is hunter2");
            })
            .fetcher("Query", "forbidden", env -> {
                throw new AccessDeniedException("no");
            })
            .fetcher("Query", "missing", env -> {
                throw new ResponseStatusException(404, "No such thing");
            })
            .fetcher("Query", "echo", env -> Json.write(env.getArgument("input")))
            .fetcher("Book", "author", env -> {
                Map<String, Object> a = new LinkedHashMap<String, Object>();
                a.put("id", "a-" + ((Map) env.getSource()).get("id"));
                a.put("name", "Writer of " + ((Map) env.getSource()).get("title"));
                return a;
            })
            .fetcher("Mutation", "add", env -> {
                Map<String, Object> b = book(String.valueOf(books.size() + 1),
                        (String) env.getArgument("title"), "FICTION", 1);
                books.add(b);
                return b;
            })
            .fetcher("Subscription", "ticks", env -> {
                final int count = ((Integer) env.getArgument("count")).intValue();
                return (GraphQLStream) emitter -> {
                    for (int i = 0; i < count; i++) {
                        emitter.next(Integer.valueOf(i));
                    }
                    emitter.complete();
                    return null;
                };
            })
            .typeResolver("Node", value -> (String) ((Map) value).get("__typename"))
            .typeResolver("SearchResult", value -> ((Map) value).containsKey("title") ? "Book" : null);
    }

    String run(String query) {
        return Json.write(builder().build().execute(query));
    }

    String run(String query, Map<String, Object> variables) {
        return Json.write(builder().build().execute(query, null, variables, null));
    }

    @Test
    void queriesWithArgumentsAliasesAndNestedResolvers() {
        assertEquals("{\"data\":{\"a\":{\"title\":\"Dune\",\"author\":{\"name\":\"Writer of Dune\"}},"
                + "\"b\":{\"title\":\"Cosmos\",\"genre\":\"SCIENCE\"}}}",
                run("{ a: book(id: \"1\") { title author { name } } b: book(id: 2) { title genre } }"));
    }

    @Test
    void argumentDefaultsEnumsAndLists() {
        assertEquals("{\"data\":{\"books\":[{\"id\":\"1\"},{\"id\":\"3\"}]}}",
                run("{ books(genre: FICTION) { id } }"));
        assertEquals("{\"data\":{\"books\":[{\"id\":\"1\"}]}}", run("{ books(first: 1) { id } }"));
    }

    @Test
    void variablesFragmentsAndDirectives() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("id", "3");
        vars.put("withPages", Boolean.FALSE);
        assertEquals("{\"data\":{\"book\":{\"__typename\":\"Book\",\"title\":\"Emma\"}}}",
                run("query Q($id: ID!, $withPages: Boolean!) { book(id: $id) { __typename ...F "
                        + "pages @include(if: $withPages) } } fragment F on Book { title }", vars));
    }

    @Test
    void inputObjectsAreCoercedWithDefaults() {
        assertEquals("{\"data\":{\"echo\":\"{\\\"title\\\":\\\"X\\\",\\\"pages\\\":100,\\\"tags\\\":[\\\"a\\\"]}\"}}",
                run("{ echo(input: {title: \"X\", tags: \"a\"}) }"));
        Map<String, Object> vars = new HashMap<String, Object>();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("pages", Long.valueOf(3));
        vars.put("in", input);
        String r = run("query($in: BookInput!) { echo(input: $in) }", vars);
        assertTrue(r.startsWith("{\"errors\":[{\"message\":\"Variable \\\"$in\\\" got invalid value.title"), r);
        assertTrue(r.indexOf("\"data\"") < 0, r);
    }

    @Test
    void unionsAndInterfacesResolveTheirObjectType() {
        assertEquals("{\"data\":{\"search\":[{\"__typename\":\"Book\",\"title\":\"Dune\"},"
                + "{\"__typename\":\"Author\",\"name\":\"Herbert\"}]}}",
                run("{ search(text: \"x\") { __typename ... on Book { title } ... on Author { name } } }"));
    }

    @Test
    void nullInNonNullFieldPropagatesToTheRoot() {
        String r = run("{ strict { id } book(id: 1) { id } }");
        assertTrue(r.startsWith("{\"errors\":[{\"message\":\"Cannot return null for non-nullable field \\\"strict\\\".\""), r);
        assertTrue(r.endsWith("\"data\":null}"), r);
    }

    @Test
    void resolverErrorsAreClassifiedAndInternalDetailNeverLeaks() {
        String r = run("{ failing forbidden missing book(id: 1) { title } }");
        assertTrue(r.indexOf("hunter2") < 0, r);
        assertTrue(r.indexOf("\"message\":\"INTERNAL_ERROR for failing\"") > 0, r);
        assertTrue(r.indexOf("{\"message\":\"Forbidden\",\"locations\":[{\"line\":1,\"column\":11}],"
                + "\"path\":[\"forbidden\"],\"extensions\":{\"classification\":\"FORBIDDEN\"}}") > 0, r);
        assertTrue(r.indexOf("\"message\":\"No such thing\"") > 0, r);
        assertTrue(r.endsWith("\"data\":{\"failing\":null,\"forbidden\":null,\"missing\":null,"
                + "\"book\":{\"title\":\"Dune\"}}}"), r);
    }

    @Test
    void invalidDocumentsAreRefusedBeforeAnythingRuns() {
        String[][] cases = {
            {"{ nope }", "Cannot query field \\\"nope\\\" on type \\\"Query\\\"."},
            {"{ book { id } }", "argument \\\"id\\\" of type \\\"ID!\\\" is required"},
            {"{ book(id: 1) }", "must have a selection of subfields"},
            {"{ book(id: 1) { title { x } } }", "must not have a selection"},
            {"{ books(genre: POETRY) { id } }", "does not exist in \\\"Genre\\\" enum"},
            {"query($x: Int) { book(id: $x) { id } }", "used in position expecting type \\\"ID!\\\""},
            {"{ book(id: $y) { id } }", "Variable \\\"$y\\\" is not defined."},
            {"query($u: Int) { failing }", "Variable \\\"$u\\\" is never used"},
            {"{ ...Missing }", "Unknown fragment \\\"Missing\\\"."},
            {"{ book(id: 1) { ...A } } fragment A on Book { ...B } fragment B on Book { ...A }", "within itself"},
            {"{ book(id: 1) { ... on Author { name } } }", "can never be of type \\\"Author\\\""},
            {"{ a: failing a: forbidden }", "are different fields"},
            {"{ failing @bogus }", "Unknown directive"},
            {"subscription { ticks(count: 1) ticks2: ticks(count: 2) }", "must select only one top level field"},
            {"{ book(id: 1) { id ", "Syntax Error"},
        };
        for (String[] c : cases) {
            String r = run(c[0]);
            assertTrue(r.startsWith("{\"errors\":") && r.indexOf("\"data\"") < 0 && r.indexOf(c[1]) > 0,
                    c[0] + " -> " + r);
        }
    }

    @Test
    void mutationsRunInOrder() {
        GraphQL g = builder().build();
        assertEquals("{\"data\":{\"a\":{\"id\":\"4\"},\"b\":{\"id\":\"5\"}}}",
                Json.write(g.execute("mutation { a: add(title: \"A\") { id } b: add(title: \"B\") { id } }")));
    }

    @Test
    void depthIsLimited() {
        String r = Json.write(builder().maxDepth(2).build().execute("{ book(id: 1) { author { name } } }"));
        assertTrue(r.indexOf("exceeds the maximum depth of 2") > 0, r);
    }

    @Test
    void introspectionDescribesTheSchema() {
        String r = run("{ __schema { queryType { name } mutationType { name } } "
                + "__type(name: \"Book\") { kind interfaces { name } fields { name type { kind ofType { name } } } } "
                + "q: __type(name: \"Query\") { description fields { name } all: fields(includeDeprecated: true) { name isDeprecated deprecationReason } } }");
        assertTrue(r.startsWith("{\"data\":{\"__schema\":{\"queryType\":{\"name\":\"Query\"},\"mutationType\":{\"name\":\"Mutation\"}},"), r);
        assertTrue(r.indexOf("{\"name\":\"author\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"name\":\"Author\"}}}") > 0, r);
        assertTrue(r.indexOf("\"interfaces\":[{\"name\":\"Node\"}]") > 0, r);
        assertTrue(r.indexOf("\"description\":\"The root\"") > 0, r);
        assertTrue(r.indexOf("{\"name\":\"old\",\"isDeprecated\":true,\"deprecationReason\":\"use book\"}") > 0, r);
        int plain = r.indexOf("\"fields\":[", r.indexOf("\"q\":"));
        assertTrue(r.indexOf("\"old\"", plain) > r.indexOf("\"all\""), "deprecated hidden by default: " + r);
        String off = Json.write(builder().introspection(false).build().execute("{ __schema { types { name } } }"));
        assertTrue(off.indexOf("Cannot query field \\\"__schema\\\"") > 0, off);
    }

    @Test
    void theFullIntrospectionQueryRuns() {
        String q = "query IntrospectionQuery { __schema { queryType { name } mutationType { name } "
                + "subscriptionType { name } types { ...FullType } directives { name description "
                + "locations args { ...InputValue } } } } fragment FullType on __Type { kind name "
                + "description fields(includeDeprecated: true) { name description args { ...InputValue } "
                + "type { ...TypeRef } isDeprecated deprecationReason } inputFields { ...InputValue } "
                + "interfaces { ...TypeRef } enumValues(includeDeprecated: true) { name description "
                + "isDeprecated deprecationReason } possibleTypes { ...TypeRef } } fragment InputValue "
                + "on __InputValue { name description type { ...TypeRef } defaultValue } fragment "
                + "TypeRef on __Type { kind name ofType { kind name ofType { kind name ofType { kind "
                + "name } } } }";
        String r = run(q);
        assertTrue(r.startsWith("{\"data\":{\"__schema\":"), r.substring(0, Math.min(400, r.length())));
        assertTrue(r.indexOf("\"defaultValue\":\"100\"") > 0);
        assertTrue(r.indexOf("\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Book\"") > 0);
    }

    @Test
    void subscriptionsStreamOneResultPerEvent() {
        final List<String> seen = new ArrayList<String>();
        builder().build().subscribe("subscription { ticks(count: 3) }", null, null, null,
                new GraphQL.SubscriptionListener() {
                    public void next(Map<String, Object> result) {
                        seen.add(Json.write(result));
                    }

                    public void error(Map<String, Object> result) {
                        seen.add("error " + Json.write(result));
                    }

                    public void complete() {
                        seen.add("complete");
                    }
                });
        assertEquals(Arrays.asList("{\"data\":{\"ticks\":0}}", "{\"data\":{\"ticks\":1}}",
                "{\"data\":{\"ticks\":2}}", "complete"), seen);
        String overHttp = run("subscription { ticks(count: 1) }");
        assertTrue(overHttp.indexOf("served over WebSocket") > 0, overHttp);
    }

    @Test
    void theSchemaIsChecked() {
        assertThrows(GraphQLException.class, () -> GraphQLSchema.parse("type Query { a: Nope }"));
        assertThrows(GraphQLException.class, () -> GraphQLSchema.parse("type Foo { a: Int }"));
        assertThrows(GraphQLException.class, () -> GraphQLSchema.parse(
                "interface I { a: Int } type Query implements I { b: Int }"));
        assertThrows(IllegalArgumentException.class, () -> GraphQL.newGraphQL(GraphQLSchema.parse(SDL))
                .fetcher("Query", "nope", env -> null));
        GraphQLSchema extended = GraphQLSchema.parse("type Query { a: Int }", "extend type Query { b: Int }");
        assertTrue(extended.hasField("Query", "b"));
    }

    @Test
    void customScalarsCoerceBothWays() {
        GraphQL g = GraphQL.newGraphQL(GraphQLSchema.parse("scalar Upper type Query { shout(s: Upper): Upper }"))
                .scalar("Upper", new GraphQLScalar() {
                    public Object serialize(Object value) {
                        return "<" + value + ">";
                    }

                    public Object parseValue(Object input) {
                        if (!(input instanceof String)) {
                            throw new GraphQLException("Upper takes a string");
                        }
                        return input + "!";
                    }
                })
                .fetcher("Query", "shout", env -> env.getArgument("s"))
                .build();
        assertEquals("{\"data\":{\"shout\":\"<hi!>\"}}", Json.write(g.execute("{ shout(s: \"hi\") }")));
        assertTrue(Json.write(g.execute("{ shout(s: 1) }")).indexOf("Upper takes a string") > 0);
    }

    @Test
    void batchLoadersResolveAWholeListInOneCall() {
        final List<Integer> calls = new ArrayList<Integer>();
        GraphQL g = GraphQL.newGraphQL(GraphQLSchema.parse(SDL))
                .fetcher("Query", "books", env -> books)
                .fetcher("Query", "book", env -> books.get(0))
                .batchLoader("Book", "author", (sources, env) -> {
                    calls.add(Integer.valueOf(sources.size()));
                    Map<Object, Object> out = new HashMap<Object, Object>();
                    for (Object b : sources) {
                        Map<String, Object> a = new LinkedHashMap<String, Object>();
                        a.put("id", "a");
                        a.put("name", "By " + ((Map) b).get("title"));
                        out.put(b, a);
                    }
                    return out;
                })
                .build();
        assertEquals("{\"data\":{\"books\":[{\"author\":{\"name\":\"By Dune\"}},"
                + "{\"author\":{\"name\":\"By Cosmos\"}},{\"author\":{\"name\":\"By Emma\"}}]}}",
                Json.write(g.execute("{ books(first: 5) { author { name } } }")));
        assertEquals(Arrays.asList(Integer.valueOf(3)), calls);
        assertEquals("{\"data\":{\"book\":{\"author\":{\"name\":\"By Dune\"}}}}",
                Json.write(g.execute("{ book(id: 1) { author { name } } }")));
        assertThrows(IllegalArgumentException.class, () -> GraphQL.newGraphQL(GraphQLSchema.parse(SDL))
                .fetcher("Book", "author", env -> null).batchLoader("Book", "author", (s, e) -> null));
    }

    @Test
    void boundsNestingAndFieldCountAndAllowsExtensionsBeforeTheirType() throws Exception {
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            deep.append("{a");
        }
        Map<String, Object> r = builder().build().execute(deep.toString());
        assertTrue(Json.write(r).contains("nests deeper than"), Json.write(r));
        StringBuilder wide = new StringBuilder("{");
        for (int i = 0; i < 30; i++) {
            wide.append(" a").append(i).append(": book(id: 1) { title }");
        }
        wide.append(" }");
        r = builder().maxFields(50).build().execute(wide.toString());
        assertTrue(Json.write(r).contains("selects more than 50 fields"), Json.write(r));
        assertNull(r.get("data"));
        StringBuilder chain = new StringBuilder("{ ...F0 }");
        for (int i = 0; i < 40; i++) {
            chain.append(" fragment F").append(i).append(" on Query { ...F").append(i + 1)
                    .append(" ...F").append(i + 1).append(" }");
        }
        chain.append(" fragment F40 on Query { __typename }");
        long start = System.currentTimeMillis();
        r = builder().build().execute(chain.toString());
        assertTrue(Json.write(r).contains("selects more than"), Json.write(r));
        assertTrue(System.currentTimeMillis() - start < 5000, "a doubling fragment chain is linear");
        GraphQLSchema s = GraphQLSchema.parse("extend type Query { b: String }", "type Query { a: String }");
        assertNotNull(s);
        assertThrows(GraphQLException.class, () -> GraphQLSchema.parse("extend type Nope { b: String }"));
    }

    // ------------------------------------------------------------- HTTP

    static HttpServer.Response http(GraphQLEndpoint e, String method, String target, String type,
                                    String body) throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        if (type != null) {
            headers.put("Content-Type", type);
        }
        return e.handle(HttpServer.Request.of(method, target, headers,
                body == null ? null : body.getBytes("UTF-8")));
    }

    static String body(HttpServer.Response r) throws Exception {
        return new String(r.getBody(), "UTF-8");
    }

    @Test
    void servesPostAndGetAndRefusesMutationsOverGet() throws Exception {
        GraphQLEndpoint e = GraphQLEndpoint.of(builder().build(), "/graphql", null);
        assertNull(http(e, "POST", "/other", "application/json", "{}"));
        HttpServer.Response r = http(e, "POST", "/graphql", "application/json",
                "{\"query\":\"query($id: ID!) { book(id: $id) { title } }\",\"variables\":{\"id\":\"2\"}}");
        assertEquals(200, r.getStatus());
        assertEquals("{\"data\":{\"book\":{\"title\":\"Cosmos\"}}}", body(r));
        assertTrue(r.getContentType().startsWith("application/graphql-response+json"), r.getContentType());
        // A text/plain POST is a CORS simple request; refusing it stops CSRF.
        assertEquals(415, http(e, "POST", "/graphql", "text/plain",
                "{\"query\":\"{ book(id: 1) { title } }\"}").getStatus());
        assertEquals(415, http(e, "POST", "/graphql", null, "{\"query\":\"{ a }\"}").getStatus());
        r = http(e, "POST", "/graphql", "application/graphql", "{ book(id: 1) { title } }");
        assertEquals("{\"data\":{\"book\":{\"title\":\"Dune\"}}}", body(r));
        r = http(e, "GET", "/graphql?query=%7B%20book(id%3A%203)%20%7B%20title%20%7D%20%7D", null, null);
        assertEquals("{\"data\":{\"book\":{\"title\":\"Emma\"}}}", body(r));
        r = http(e, "GET", "/graphql?query=mutation%7Badd(title%3A%22x%22)%7Bid%7D%7D", null, null);
        assertEquals(405, r.getStatus());
        assertEquals(400, http(e, "POST", "/graphql", "application/json", "not json").getStatus());
        assertEquals(400, http(e, "POST", "/graphql", "application/json", "{\"query\":1}").getStatus());
        assertEquals(405, http(e, "PUT", "/graphql", null, null).getStatus());
        assertEquals(3, books.size());
    }
}
