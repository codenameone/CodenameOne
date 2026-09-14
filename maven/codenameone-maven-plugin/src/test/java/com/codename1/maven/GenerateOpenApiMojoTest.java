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
package com.codename1.maven;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Drives the OpenAPI codegen against an inline JSON spec to verify model +
/// `@RestClient` emission shape. Both the Java 17 records path and the Java 8
/// classes path are exercised.
class GenerateOpenApiMojoTest {

    private static final String SAMPLE_SPEC =
            "{"
            + "\"openapi\":\"3.0.0\","
            + "\"info\":{\"title\":\"Petstore\",\"version\":\"1.0\"},"
            + "\"paths\":{"
            + "  \"/pet/{petId}\":{"
            + "    \"get\":{"
            + "      \"tags\":[\"Pet\"],"
            + "      \"operationId\":\"getPetById\","
            + "      \"parameters\":[{\"name\":\"petId\",\"in\":\"path\",\"required\":true,"
            + "        \"schema\":{\"type\":\"integer\",\"format\":\"int64\"}}],"
            + "      \"responses\":{\"200\":{"
            + "        \"description\":\"ok\","
            + "        \"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Pet\"}}}"
            + "      }}"
            + "    }"
            + "  },"
            + "  \"/pets\":{"
            + "    \"get\":{"
            + "      \"tags\":[\"Pet\"],"
            + "      \"operationId\":\"findPets\","
            + "      \"responses\":{\"200\":{"
            + "        \"description\":\"ok\","
            + "        \"content\":{\"application/json\":{\"schema\":{"
            + "          \"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/Pet\"}"
            + "        }}}"
            + "      }}"
            + "    }"
            + "  }"
            + "},"
            + "\"components\":{\"schemas\":{"
            + "  \"Pet\":{"
            + "    \"type\":\"object\","
            + "    \"properties\":{"
            + "      \"id\":{\"type\":\"integer\",\"format\":\"int64\"},"
            + "      \"name\":{\"type\":\"string\"}"
            + "    }"
            + "  },"
            + "  \"Cat\":{"
            + "    \"type\":\"object\","
            + "    \"properties\":{"
            + "      \"id\":{\"type\":\"integer\",\"format\":\"int64\"},"
            + "      \"name\":{\"type\":\"string\"}"
            + "    }"
            + "  }"
            + "}}"
            + "}";

    @Test
    void emitsRecordsAndRestClientInterface(@TempDir Path tmp) throws Exception {
        Map<String, Object> doc = parse(SAMPLE_SPEC);
        File out = tmp.toFile();
        GenerateOpenApiMojo.Generator gen = new GenerateOpenApiMojo.Generator(
                doc, "com.example.petstore", out, /*overwrite*/ true,
                /*emitRecords*/ true, new SystemStreamLog());
        gen.run();

        File petJava = new File(out, "com/example/petstore/model/Pet.java");
        assertTrue(petJava.exists(), "expected Pet.java at " + petJava);
        String petSrc = readString(petJava);
        assertTrue(petSrc.contains("@Mapped"), "Pet should be @Mapped");
        assertTrue(petSrc.contains("public record Pet("), "Pet should be a record on Java 17 target");
        assertTrue(petSrc.contains("@JsonProperty(\"id\") Long id"),
                "Pet record should declare @JsonProperty(\"id\") Long id; was:\n" + petSrc);
        assertTrue(petSrc.contains("@JsonProperty(\"name\") String name"),
                "Pet record should declare name; was:\n" + petSrc);

        // Cat is structurally identical to Pet and should collapse to Pet --
        // i.e. Cat.java should NOT be emitted as a separate record.
        File catJava = new File(out, "com/example/petstore/model/Cat.java");
        assertFalse(catJava.exists(),
                "Cat is structurally identical to Pet -- expected schema unification to drop it");

        File petApi = new File(out, "com/example/petstore/PetApi.java");
        assertTrue(petApi.exists(), "expected PetApi.java at " + petApi);
        String apiSrc = readString(petApi);
        assertTrue(apiSrc.contains("@RestClient"), "PetApi should carry @RestClient");
        assertTrue(apiSrc.contains("public interface PetApi"), "PetApi must be an interface");
        assertTrue(apiSrc.contains("@GET(\"/pet/{petId}\")"),
                "getPetById method should carry @GET(\"/pet/{petId}\"); was:\n" + apiSrc);
        assertTrue(apiSrc.contains("@Path(\"petId\") Long petId"),
                "getPetById path param shape; was:\n" + apiSrc);
        assertTrue(apiSrc.contains("@Header(\"Authorization\") String bearerToken"),
                "bearerToken header shape; was:\n" + apiSrc);
        assertTrue(apiSrc.contains("OnComplete<Response<com.example.petstore.model.Pet>> callback"),
                "callback shape; was:\n" + apiSrc);
        assertTrue(apiSrc.contains("OnComplete<Response<java.util.List<com.example.petstore.model.Pet>>> callback"),
                "findPets list-of-Pet response; was:\n" + apiSrc);
        assertTrue(apiSrc.contains("static PetApi of(String baseUrl)"),
                "static of(...) factory must be emitted");
        assertTrue(apiSrc.contains("RestClients.create(PetApi.class, baseUrl)"),
                "of(...) factory should delegate to RestClients.create");
    }

    private static final String ENUM_SPEC =
            "{"
            + "\"openapi\":\"3.0.0\","
            + "\"info\":{\"title\":\"Enums\",\"version\":\"1.0\"},"
            + "\"paths\":{},"
            + "\"components\":{\"schemas\":{"
            + "  \"Status\":{\"type\":\"string\",\"enum\":[\"available\",\"pending\",\"sold\"]},"
            + "  \"Shipping\":{\"type\":\"string\",\"enum\":[\"next-day\",\"two-day\"]},"
            + "  \"Pet\":{\"type\":\"object\",\"properties\":{"
            + "     \"id\":{\"type\":\"integer\",\"format\":\"int64\"},"
            + "     \"status\":{\"$ref\":\"#/components/schemas/Status\"},"
            + "     \"shipping\":{\"$ref\":\"#/components/schemas/Shipping\"}"
            + "  }}"
            + "}}}";

    @Test
    void emitsJavaEnumsForStringEnumSchemas(@TempDir Path tmp) throws Exception {
        Map<String, Object> doc = parse(ENUM_SPEC);
        File out = tmp.toFile();
        new GenerateOpenApiMojo.Generator(
                doc, "com.example.petstore", out, true, /*emitRecords*/ true,
                new SystemStreamLog()).run();

        // Generatable enum -> Java enum whose constants equal the wire values.
        File statusJava = new File(out, "com/example/petstore/model/Status.java");
        assertTrue(statusJava.exists(), "expected Status.java enum");
        String statusSrc = readString(statusJava);
        assertTrue(statusSrc.contains("public enum Status"), "Status should be a Java enum; was:\n" + statusSrc);
        assertTrue(statusSrc.contains("available") && statusSrc.contains("pending")
                && statusSrc.contains("sold"), "Status values; was:\n" + statusSrc);
        assertFalse(statusSrc.contains("@Mapped"), "a plain enum is not @Mapped");

        // The owning model references the enum type for the status property...
        String petSrc = readString(new File(out, "com/example/petstore/model/Pet.java"));
        assertTrue(petSrc.contains("com.example.petstore.model.Status status"),
                "Pet.status should be typed as the generated enum; was:\n" + petSrc);

        // ...but the hyphenated enum can't be a Java constant, so it degrades
        // to String and no enum file is emitted.
        assertFalse(new File(out, "com/example/petstore/model/Shipping.java").exists(),
                "non-identifier enum values should degrade to String (no Shipping.java)");
        assertTrue(petSrc.contains("String shipping"),
                "Pet.shipping should degrade to String; was:\n" + petSrc);
    }

    @Test
    void emitsClassesOnJava8Target(@TempDir Path tmp) throws Exception {
        Map<String, Object> doc = parse(SAMPLE_SPEC);
        File out = tmp.toFile();
        GenerateOpenApiMojo.Generator gen = new GenerateOpenApiMojo.Generator(
                doc, "com.example.petstore", out, true,
                /*emitRecords*/ false, new SystemStreamLog());
        gen.run();

        String petSrc = readString(new File(out, "com/example/petstore/model/Pet.java"));
        assertTrue(petSrc.contains("public class Pet {"), "Pet should be a class on Java 8 target");
        assertTrue(petSrc.contains("public Long id;"), "Pet class should declare Long id field");
        assertTrue(petSrc.contains("public String name;"), "Pet class should declare String name field");
        assertTrue(petSrc.contains("public Pet() {}"), "Pet class should have a public no-arg ctor");
    }

    @Test
    void respectsOverwriteFalseAndPreservesUserEdits(@TempDir Path tmp) throws Exception {
        Map<String, Object> doc = parse(SAMPLE_SPEC);
        File out = tmp.toFile();
        File apiDir = new File(out, "com/example/petstore");
        if (!apiDir.exists() && !apiDir.mkdirs()) throw new IOException("mkdirs");
        File apiFile = new File(apiDir, "PetApi.java");
        Files.write(apiFile.toPath(), "// hand-edited".getBytes(StandardCharsets.UTF_8));

        GenerateOpenApiMojo.Generator gen = new GenerateOpenApiMojo.Generator(
                doc, "com.example.petstore", out, /*overwrite*/ false,
                true, new SystemStreamLog());
        gen.run();

        String apiSrc = readString(apiFile);
        assertTrue(apiSrc.startsWith("// hand-edited"),
                "overwrite=false should preserve user edits; was:\n" + apiSrc);
    }

    /// The Swagger Petstore case: Tag and Category have the same shape, so one
    /// is unified away, and Pet references both. Property types are resolved
    /// before unification runs, so the surviving reference used to name the
    /// dropped class and the emitted model did not compile.
    @Test
    void unifiedAwaySchemaIsNotLeftDanglingInAReference(@TempDir Path tmp) throws Exception {
        String spec =
                "{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"t\",\"version\":\"1\"},"
                + "\"paths\":{\"/pet\":{\"get\":{\"tags\":[\"Pet\"],\"operationId\":\"getPet\","
                + "  \"responses\":{\"200\":{\"description\":\"ok\",\"content\":{\"application/json\":"
                + "  {\"schema\":{\"$ref\":\"#/components/schemas/Pet\"}}}}}}}},"
                + "\"components\":{\"schemas\":{"
                + "  \"Category\":{\"type\":\"object\",\"properties\":{"
                + "    \"id\":{\"type\":\"integer\",\"format\":\"int64\"},\"name\":{\"type\":\"string\"}}},"
                + "  \"Tag\":{\"type\":\"object\",\"properties\":{"
                + "    \"id\":{\"type\":\"integer\",\"format\":\"int64\"},\"name\":{\"type\":\"string\"}}},"
                + "  \"Pet\":{\"type\":\"object\",\"properties\":{"
                + "    \"category\":{\"$ref\":\"#/components/schemas/Category\"},"
                + "    \"tags\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/Tag\"}}}}"
                + "}}}";
        File out = tmp.toFile();
        new GenerateOpenApiMojo.Generator(parse(spec), "com.example.petstore", out,
                true, /*emitRecords*/ true, new SystemStreamLog()).run();

        File tag = new File(out, "com/example/petstore/model/Tag.java");
        File category = new File(out, "com/example/petstore/model/Category.java");
        assertTrue(category.exists(), "Category is the canonical shape and must be emitted");
        assertFalse(tag.exists(), "Tag is structurally identical and should unify away");

        String petSrc = readString(new File(out, "com/example/petstore/model/Pet.java"));
        assertFalse(petSrc.contains("model.Tag"),
                "Pet must not reference the class that was unified away; was:\n" + petSrc);
        assertTrue(petSrc.contains("java.util.List<com.example.petstore.model.Category> tags"),
                "the tags property should retype to the surviving class; was:\n" + petSrc);
    }

    @Test
    void parseJavaVersionHandlesShapes() {
        org.junit.jupiter.api.Assertions.assertEquals(8, GenerateOpenApiMojo.parseJavaVersion("1.8"));
        org.junit.jupiter.api.Assertions.assertEquals(8, GenerateOpenApiMojo.parseJavaVersion(null));
        org.junit.jupiter.api.Assertions.assertEquals(11, GenerateOpenApiMojo.parseJavaVersion("11"));
        org.junit.jupiter.api.Assertions.assertEquals(17, GenerateOpenApiMojo.parseJavaVersion("17"));
        org.junit.jupiter.api.Assertions.assertEquals(21, GenerateOpenApiMojo.parseJavaVersion("21-LTS"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static Map<String, Object> parse(String json) throws IOException {
        return new com.codename1.io.JSONParser().parseJSON(new StringReader(json));
    }

    private static String readString(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
