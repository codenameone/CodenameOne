/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */
package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates a typed CN1-subset-clean REST client from an OpenAPI 3.x
 * specification. Emits one {@code <Tag>Api.java} class per OpenAPI tag, each
 * with one method per operation. Generated code uses {@code Rest},
 * {@code JSONWriter}, and (once merged from the {@code pojo-annotation-frameworks}
 * branch) {@code @Mapped} DTOs -- never {@code java.net.http} or anything
 * else outside the CN1 JDK subset.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * <plugin>
 *     <groupId>com.codenameone</groupId>
 *     <artifactId>codenameone-maven-plugin</artifactId>
 *     <executions>
 *         <execution>
 *             <id>generate-openapi-client</id>
 *             <goals><goal>generate-openapi-client</goal></goals>
 *             <configuration>
 *                 <openapiSpec>https://server.example.com/api/openapi.json</openapiSpec>
 *                 <basePackage>com.example.api</basePackage>
 *                 <outputDirectory>${project.build.directory}/generated-sources/openapi</outputDirectory>
 *             </configuration>
 *         </execution>
 *     </executions>
 * </plugin>
 * }</pre>
 *
 * <h3>Status: SCAFFOLD ONLY</h3>
 *
 * The mojo is wired into the plugin but the underlying generator is not yet
 * implemented. The intended design is:
 *
 * <ol>
 *   <li>Resolve the spec via {@code ConnectionRequest}-style URL fetch (or
 *       read from local file when the configured value is a path).</li>
 *   <li>Parse the OpenAPI JSON using the framework's own {@code JSONParser}.</li>
 *   <li>For each {@code tag} in the spec, emit a {@code <Tag>Api.java} class
 *       under {@code <basePackage>} in {@code outputDirectory}.</li>
 *   <li>For each {@code operation} on a tag, emit a method with parameters
 *       mapped from {@code parameters} + {@code requestBody}.</li>
 *   <li>Use {@code Rest.<get|post|put|delete|patch>(url)
 *       .queryParam(...).header(...).body(JSONWriter.toJson(...))
 *       .fetchAsJsonMap(callback)} (or {@code fetchAsJsonList}, or
 *       {@code fetchAsMapped} once the binding framework lands) as the
 *       per-method body.</li>
 *   <li>For each {@code components/schemas} entry, emit a POJO under
 *       {@code <basePackage>.model} annotated with {@code @Mapped} +
 *       {@code @JsonProperty} fields (after the binding framework merge).
 *       Pre-merge: emit plain POJOs with hand-rolled {@code fromJson(Map)}
 *       static methods.</li>
 *   <li>Add the {@code outputDirectory} to the project's compile source
 *       roots so generated code is picked up by the compiler.</li>
 * </ol>
 *
 * <h3>Subset-compliance notes for the generator</h3>
 *
 * <ul>
 *   <li>Generated method bodies use only the CN1 JDK subset -- no
 *       {@code java.net.http}, no {@code Optional}, no {@code Stream}
 *       beyond what {@code java-runtime} ships.</li>
 *   <li>Date/time fields use {@code com.codename1.l10n.SimpleDateFormat}
 *       for parse/format, not {@code java.text.SimpleDateFormat} (partial)
 *       or {@code java.time.format.DateTimeFormatter} (also partial).</li>
 *   <li>{@code File} upload / {@code multipart/form-data} request bodies
 *       use {@code MultipartRequest} from CN1 core.</li>
 *   <li>Authentication is parameterised: the generator emits methods that
 *       take an optional {@code String bearerToken} parameter and attach it
 *       as an {@code Authorization} header. OAuth flows are out of scope
 *       (use {@code OidcClient} directly).</li>
 * </ul>
 *
 * <h3>Why this is a separate mojo, not a build extension</h3>
 *
 * The mojo runs once at {@code generate-sources}; the output is regular
 * Java that the standard compiler picks up. There is no need for a build
 * extension because the generated code has no runtime support beyond
 * what already ships in {@code codenameone-core}.
 */
@Mojo(name = "generate-openapi-client",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresDependencyResolution = ResolutionScope.COMPILE,
      threadSafe = true)
public class GenerateOpenApiClientMojo extends AbstractCN1Mojo {

    /// URL or local file path of the OpenAPI 3.x spec (JSON).
    @Parameter(property = "cn1.openapi.spec", required = true)
    private String openapiSpec;

    /// Base package for the generated client classes (e.g.
    /// `com.example.api`). The Api classes go directly under this package;
    /// schemas under `<basePackage>.model`.
    @Parameter(property = "cn1.openapi.basePackage", required = true)
    private String basePackage;

    /// Output directory for generated Java sources. Added to the project's
    /// compile-source-roots so the generated files are picked up by the
    /// compiler.
    @Parameter(property = "cn1.openapi.outputDirectory",
               defaultValue = "${project.build.directory}/generated-sources/openapi")
    private java.io.File outputDirectory;

    /// When `true`, regenerate every file even if it exists. Default is to
    /// skip files that haven't changed (by content hash) so the user can
    /// edit generated code locally if they wish.
    @Parameter(property = "cn1.openapi.overwrite", defaultValue = "true")
    private boolean overwrite;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException("Could not create output directory: " + outputDirectory);
        }
        java.util.Map<String, Object> spec;
        try {
            getLog().info("Loading OpenAPI spec from " + openapiSpec);
            spec = OpenApiCodegen.loadSpec(openapiSpec);
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Failed to load OpenAPI spec from " + openapiSpec + ": " + e.getMessage(), e);
        }
        if (spec == null || spec.isEmpty()) {
            throw new MojoExecutionException(
                    "OpenAPI spec parsed as empty/null. Is " + openapiSpec + " valid JSON?");
        }
        try {
            new OpenApiCodegen(outputDirectory, basePackage, getLog(), spec).generate();
        } catch (java.io.IOException e) {
            throw new MojoExecutionException(
                    "OpenAPI codegen failed: " + e.getMessage(), e);
        }
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
    }
}
