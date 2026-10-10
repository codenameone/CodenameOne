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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Builds the Unity project of an application module: `src/main/unity`, laid
/// out like any Unity project (`Assets/`, `ProjectSettings/`).
///
/// Compiles the C# scripts with the .NET SDK, translates them to class files
/// in the classes directory, copies the images the scenes draw there, and
/// generates the scenes as Java source under `target/generated-sources/unity`,
/// which is added as a source root. From there the project is ordinary
/// Codename One code, and every target builds it. A silent no-op for a project
/// without `src/main/unity`: nothing is resolved and no .NET SDK is looked for.
///
/// The translator and the reference assemblies are not dependencies of the
/// plugin. They are resolved here, at the version of the application's
/// `codenameone-unity-compat` dependency, so the tool that translates the
/// scripts always matches the runtime they will run against.
@Mojo(name = "compile-unity", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileUnityMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.unity.sourceDir", defaultValue = "${project.basedir}/src/main/unity")
    private File unitySourceDir;

    @Parameter(property = "cn1.unity.outputDir", defaultValue = "${project.build.directory}/generated-sources/unity")
    private File unityOutputDir;

    /// The `dotnet` executable, or the directory it is in. Without it
    /// `DOTNET_ROOT` and then the `PATH` are searched.
    @Parameter(property = UnityProjectBuilder.DOTNET_PROPERTY)
    private String dotnet;

    /// Leaves the Unity project out of this build. What an earlier build
    /// installed in the classes directory stays there.
    @Parameter(property = "cn1.unity.skip", defaultValue = "false")
    private boolean skipUnity;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!UnityProjectBuilder.isUnityProject(unitySourceDir)) {
            return;
        }
        if (skipUnity) {
            // The application's own main class names the generated one, so
            // what an earlier build generated still has to be compiled.
            if (unityOutputDir != null && unityOutputDir.isDirectory()) {
                registerSourceRoot(unityOutputDir);
            }
            return;
        }
        Artifact runtime = runtimeArtifact(project.getArtifacts());
        File runtimeJar = runtime == null ? null : runtime.getFile();
        File references = null;
        List<File> tool = new ArrayList<File>();
        if (runtimeJar != null) {
            String version = runtime.getBaseVersion();
            references = single(resolve(UnityProjectBuilder.RUNTIME_ARTIFACT, version,
                    UnityProjectBuilder.REFERENCES_CLASSIFIER, false));
            tool = resolve(UnityProjectBuilder.TOOL_ARTIFACT, version, null, true);
        }
        List<File> roots = new ArrayList<File>();
        for (Object o : project.getCompileSourceRoots()) {
            roots.add(new File(o.toString()));
        }
        roots.add(new File(project.getBasedir(), "src/main/kotlin"));
        String pkg = null;
        String main = null;
        if (isCN1ProjectDir() && properties != null) {
            pkg = properties.getProperty("codename1.packageName");
            main = properties.getProperty("codename1.mainName");
        }
        UnityProjectBuilder builder = new UnityProjectBuilder(unitySourceDir, unityOutputDir,
                new File(project.getBuild().getOutputDirectory()), new File(project.getBuild().getDirectory()),
                runtimeJar, references, tool, dotnet, pkg, main, roots, MavenLog.of(getLog()));
        try {
            builder.run();
        } catch (com.codename1.builders.BuildException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        registerSourceRoot(unityOutputDir);
    }

    /// Compile and runtime scope: what `java -cp` needs to run a jar.
    private static final ScopeArtifactFilter RUNTIME_SCOPE = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

    static Artifact runtimeArtifact(Iterable<?> artifacts) {
        for (Object o : artifacts) {
            Artifact a = (Artifact) o;
            // The references are an artifact of the same id; the runtime is
            // the one without a classifier.
            if (UnityProjectBuilder.RUNTIME_ARTIFACT.equals(a.getArtifactId()) && a.getFile() != null
                    && (a.getClassifier() == null || a.getClassifier().length() == 0)) {
                return a;
            }
        }
        return null;
    }

    private static File single(List<File> files) {
        return files.isEmpty() ? null : files.get(0);
    }

    /// The jar of `com.codenameone:<artifactId>:<version>`, first, and with
    /// `transitive` its runtime dependencies after it.
    private List<File> resolve(String artifactId, String version, String classifier, boolean transitive)
            throws MojoFailureException {
        Artifact artifact = classifier == null
                ? repositorySystem.createArtifact("com.codenameone", artifactId, version, "jar")
                : repositorySystem.createArtifactWithClassifier("com.codenameone", artifactId, version, "jar",
                        classifier);
        ArtifactResolutionResult result = repositorySystem.resolve(new ArtifactResolutionRequest()
                .setLocalRepository(localRepository)
                .setRemoteRepositories(new ArrayList<org.apache.maven.artifact.repository.ArtifactRepository>(
                        remoteRepositories))
                .setResolveTransitively(transitive)
                // What the tool runs with, and nothing else. Unfiltered, this
                // resolver also returns the test and provided dependencies its
                // pom declares -- for the translator that is codenameone-core,
                // JUnit and everything JUnit needs. They were on the tool's
                // class path for no reason, and since the content of that class
                // path is part of what compiled output is matched by, the output
                // of one machine was good only beside a core jar with the same
                // bytes: a core built on Windows, whose text resources are
                // checked out with other line endings, refused every archive
                // made on Linux.
                .setCollectionFilter(RUNTIME_SCOPE)
                .setResolutionFilter(RUNTIME_SCOPE)
                // The legacy resolver does not read the session's offline flag
                // by itself; see AbstractCN1Mojo.offline.
                .setOffline(offline)
                .setArtifact(artifact));
        String what = "com.codenameone:" + artifactId + ":" + version + (classifier == null ? "" : ":" + classifier);
        String incomplete = resolutionFailure(result);
        if (incomplete != null || artifact.getFile() == null || !artifact.getFile().isFile()) {
            throw new MojoFailureException("src/main/unity holds a Unity project, and building it needs " + what
                    + ", which could not be resolved" + (incomplete == null ? "" : ": " + incomplete) + ".\n"
                    + "It is published alongside the " + UnityProjectBuilder.RUNTIME_ARTIFACT + " runtime this"
                    + " project depends on; run the build again with -U, or without -o if it is offline.");
        }
        List<File> files = new ArrayList<File>();
        files.add(artifact.getFile());
        if (transitive && result.getArtifacts() != null) {
            for (Artifact resolved : result.getArtifacts()) {
                File f = resolved.getFile();
                if (f != null && f.isFile() && !files.contains(f)) {
                    files.add(f);
                }
            }
        }
        return files;
    }
}
