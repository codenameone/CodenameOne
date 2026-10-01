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
package com.codename1.maven;

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.List;

/**
 * Translates a backend module to C and compiles it to a native binary:
 * `mvn cn1:backend-package`.
 *
 * The counterpart to {@link BackendRunMojo}. That one runs the module on this JVM
 * in a couple of seconds and is what a developer uses; this one produces the
 * artifact that deploys -- a single executable with no runtime to install, which
 * is what makes a scratch container the size of the binary and a Lambda cold start
 * a process exec.
 *
 * What it does, in order:
 *
 * 1. Compiles the module's sources together with the backend runtime's SHARED and
 *    PARPARVM halves against the ParparVM JavaAPI as the BOOTCLASSPATH. That last
 *    part is the important one: the bootclasspath IS the server-safe surface, so a
 *    reference to something the translated runtime does not have fails here, in
 *    the IDE and in the build, rather than at link time or in production.
 * 2. Runs the translator over the result, with the runtime's C sources already in
 *    the source root -- they have to be there BEFORE it runs, because a native's
 *    Java method is kept alive by its C symbol being present.
 * 3. Compiles the generated C, either with the host compiler or, for a named
 *    Linux target, in a container.
 *
 * The compiler flags are not negotiable and are documented at the call site:
 * generated C relies on wrapping arithmetic, and clang -O3 miscompiles it without
 * them.
 */
//
// The packaging itself is BackendPackager, in the build engine, which the Gradle
// plugin's backendPackage task runs too. This mojo supplies the Maven answers: the
// compiler plugin's encoding, the repository to resolve the runtime from, and the
// artifact id the binary is named after.
// Forks the lifecycle up to compile first, so `mvn cn1:backend-package` on its own does
// the obvious thing on a clean checkout instead of failing on an empty
// target/classes.
@Execute(phase = LifecyclePhase.COMPILE)
@Mojo(name = "backend-package", requiresDependencyResolution = ResolutionScope.COMPILE)
public class BackendPackageMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    @Component
    private RepositorySystem repositorySystem;

    /**
     * The class whose main() becomes the program's entry point.
     *
     * Optional. A module whose server is written as `@RestController` classes has no
     * main of its own -- one is generated from them -- and naming a class that does
     * not exist is worse than leaving this out.
     */
    @Parameter(property = "cn1.backend.mainClass")
    private String mainClass;

    /**
     * Where the binary goes. Defaults to target/&lt;artifactId&gt;.
     */
    @Parameter(property = "cn1.backend.output")
    private File output;

    /**
     * A Linux deployment target -- musl-x86_64, musl-arm64, glibc-x86_64,
     * glibc-arm64 -- built in a container. Omitted, it compiles for this machine
     * with the host compiler, which is what a developer wants and what CI checks.
     */
    @Parameter(property = "cn1.backend.target")
    private String target;

    /**
     * The JDK whose javac compiles for translation and whose java runs the
     * translator. Defaults to the JDK running Maven.
     *
     * Any JDK 8 or newer works; see {@link #resolveJdk()} for why this is no
     * longer pinned to a JDK 8.
     */
    @Parameter(property = "cn1.backend.jdk")
    private String jdkHome;

    /**
     * The JDK 8 this goal used to require.
     *
     * Kept, and still honoured, because projects and CI jobs are setting it. It is
     * no longer a requirement: with neither this nor cn1.backend.jdk set, the JDK
     * running Maven is what compiles and translates.
     */
    @Parameter(property = "cn1.backend.jdk8", defaultValue = "${env.JDK_8_HOME}")
    private String jdk8Home;

    /** Extra flags for the C compiler. */
    @Parameter(property = "cn1.backend.cflags")
    private String cflags;

    /**
     * Whether to link the bundled SQLite engine. Off saves about 2MB in a service
     * that talks to PostgreSQL or MySQL instead, which speak their wire protocols
     * with no engine linked at all.
     */
    @Parameter(property = "cn1.backend.sqlite", defaultValue = "true")
    private boolean sqlite;

    /**
     * Whether a failed cast throws.
     *
     * On by default here and off for app targets, which is the one place the
     * server build departs from the mobile one deliberately: on a phone a bad cast
     * costs one user a crash, and on a server the object with the wrong type
     * arrived from the network, so reading its fields as another type kills every
     * connection the process was serving.
     */
    @Parameter(property = "cn1.backend.checkedCasts", defaultValue = "true")
    private boolean checkedCasts;

    /**
     * Whether the packaged server carries the development MCP tools. Off by
     * default: they read the database and call the server on an agent's behalf,
     * and a production binary should not contain them at all -- not merely have
     * them switched off. {@code cn1:backend} runs the JVM build, which has them.
     */
    @Parameter(property = "cn1.backend.devTools", defaultValue = "false")
    private boolean devTools;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            packager().execute();
        } catch (BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        } catch (BuildExecutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /** The engine packager, answering from this mojo's project and parameters. */
    private BackendPackager packager() {
        BackendPackager p = new BackendPackager(new MavenModuleHost(project, MavenLog.of(getLog()))) {
            @Override
            protected String binaryName() {
                return project.getArtifactId();
            }

            @Override
            protected File processedResourcesDirectory() {
                return new File(project.getBuild().getOutputDirectory());
            }

            @Override
            protected boolean declaresResources() {
                return !project.getBuild().getResources().isEmpty();
            }

            @Override
            protected String sourceEncoding() {
                return BackendPackageMojo.this.sourceEncoding();
            }

            @Override
            protected File resolve(String groupId, String artifactId, String version, String classifier)
                    throws BuildExecutionException {
                try {
                    return BackendPackageMojo.this.resolve(groupId, artifactId, version, classifier);
                } catch (MojoExecutionException ex) {
                    throw new BuildExecutionException(ex.getMessage(), ex);
                }
            }
        };
        return p.mainClass(mainClass).output(output).target(target).jdk(jdkHome, jdk8Home).cflags(cflags)
                .sqlite(sqlite).checkedCasts(checkedCasts).devTools(devTools);
    }

    /**
     * The encoding this module's sources are actually written in.
     *
     * <p>These are the SAME sources the lifecycle has already compiled, so
     * reading them differently here is a second, disagreeing compilation of one
     * tree: a module that declares another encoding either fails packaging on
     * bytes javac accepted a phase earlier, or -- worse, because nothing says so
     * -- translates string literals and identifiers that are not the ones the JVM
     * build produced.
     *
     * <p>Resolved the way the compiler plugin resolves it, most specific first:
     * an explicit &lt;encoding&gt; on maven-compiler-plugin, then
     * project.build.sourceEncoding, and UTF-8 only when the module says nothing.
     * The platform default is deliberately not the last resort -- it makes the
     * build depend on the machine that runs it, which is the reason Maven warns
     * about it.
     */
    private String sourceEncoding() {
        Plugin compiler = project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
        if (compiler != null && compiler.getConfiguration() instanceof Xpp3Dom) {
            Xpp3Dom encoding = ((Xpp3Dom) compiler.getConfiguration()).getChild("encoding");
            if (encoding != null && encoding.getValue() != null) {
                String declared = encoding.getValue().trim();
                // A configuration that is still a property reference is one Maven
                // would have interpolated; an uninterpolated ${...} names no
                // charset, so fall through rather than hand javac a literal.
                if (declared.length() > 0 && declared.indexOf("${") < 0) {
                    return declared;
                }
            }
        }
        String property = project.getProperties() == null ? null
                : project.getProperties().getProperty("project.build.sourceEncoding");
        if (property != null && property.trim().length() > 0) {
            return property.trim();
        }
        return "UTF-8";
    }

    private File resolve(String groupId, String artifactId, String version, String classifier)
            throws MojoExecutionException {
        Artifact artifact = repositorySystem.createArtifactWithClassifier(
                groupId, artifactId, version, "jar", classifier);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        if (!result.isSuccess() || artifact.getFile() == null) {
            throw new MojoExecutionException("Could not resolve " + groupId + ":"
                    + artifactId + ":" + version + ":" + classifier);
        }
        return artifact.getFile();
    }

    // ---- Kept under their old names for the tests; see BackendPackager. ----

    private File resolveJdk() throws MojoFailureException {
        try {
            return packager().resolveJdk();
        } catch (BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    private File requireEightOrNewer(File home) throws MojoFailureException {
        try {
            return packager().requireEightOrNewer(home);
        } catch (BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    private static boolean dropsSourceEight(MojoFailureException err) {
        return BackendPackager.dropsSourceEight(new BuildFailureException(err.getMessage()));
    }

    private List<String> stageDependencyClasses(List<String> classpath, File staged, File nativeSources)
            throws MojoExecutionException {
        try {
            return packager().stageDependencyClasses(classpath, staged, nativeSources);
        } catch (BuildExecutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    static final String SOURCE_EIGHT_REMOVED_HINT = BackendPackager.SOURCE_EIGHT_REMOVED_HINT;

    static int parseJavacVersion(String output) {
        return BackendPackager.parseJavacVersion(output);
    }

    static List<String> withoutRuntime(List<String> classpath, File runtime, String ownOutput) {
        return BackendPackager.withoutRuntime(classpath, runtime, ownOutput);
    }

    static List<String> hostLibraryFlags(List<String> openssl, List<String> nghttp2) {
        return BackendPackager.hostLibraryFlags(openssl, nghttp2);
    }
}
