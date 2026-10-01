/*
 * Copyright (c) 2021, 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ant.SortedProperties;
import com.codename1.build.BuildArtifact;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.builders.BuildRequest;
import com.codename1.builders.Executor;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Mojo that uses the CodenameOneBuildClient to send builds to the CodenameOne build server.
 *
 * It also supports a few local build targets, such as "ios-source", which generates an Xcode project,
 * and "android-source", which generates an Android gradle project.
 *
 * <p>The build itself lives in {@link AppBuilder}, in the build engine, so the Gradle plugin runs
 * the same code. This class answers AppBuilder's questions from the Maven model: the build
 * directory, the resolved artifacts, the session's command-line properties, attaching outputs.</p>
 */
@Mojo(name="build", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
// NOTE: deliberately NOT @Execute(phase = PACKAGE). The build goal is always
// bound to the package phase by the project poms (see the desktop_build / win /
// ios / android / javascript profiles), so package runs before this goal within
// the normal lifecycle. Adding @Execute here additionally forked a *second*
// package lifecycle, which on Maven 3.9+ re-ran jar:jar against the already
// attached (empty) per-module artifact and aborted the build with "You have to
// use a classifier to attach supplemental artifacts...". The fork was redundant
// for the only supported invocation (mvn package), so it is removed. Invoking
// the goal bare (mvn cn1:build) without a prior package is not a supported flow.
public class CN1BuildMojo extends AbstractCN1Mojo {

    public static final String BUILD_TARGET_XCODE_PROJECT = Executor.BUILD_TARGET_XCODE_PROJECT;
    public static final String BUILD_TARGET_ANDROID_PROJECT = Executor.BUILD_TARGET_ANDROID_PROJECT;
    public static final String BUILD_TARGET_WINDOWS_NATIVE = Executor.BUILD_TARGET_WINDOWS_NATIVE;
    public static final String BUILD_TARGET_WINDOWS_NATIVE_PROJECT = Executor.BUILD_TARGET_WINDOWS_NATIVE_PROJECT;
    public static final String BUILD_TARGET_MAC_NATIVE_PROJECT = Executor.BUILD_TARGET_MAC_NATIVE_PROJECT;
    public static final String BUILD_TARGET_MAC_NATIVE = Executor.BUILD_TARGET_MAC_NATIVE;
    public static final String BUILD_TARGET_MAC_NATIVE_LOCAL = Executor.BUILD_TARGET_MAC_NATIVE_LOCAL;
    public static final String BUILD_TARGET_LINUX_NATIVE = Executor.BUILD_TARGET_LINUX_NATIVE;

    static final String DESKTOP_RUNTIME_BINARIES_ARTIFACT_ID = AppBuilder.DESKTOP_RUNTIME_BINARIES_ARTIFACT_ID;

    /**
     * The target platform.  E.g. javase, javascript, ios, android, win
     */
    @Parameter(property = "codename1.platform", required = true)
    private String platform;

    /**
     * The build target, corresponding to ANT build targets in build-template.xml.  E.g. javascript,
     * mac-os-x-desktop, windows-desktop, ios-device, ios-device-release, android-device, war
     */
    @Parameter(property = "codename1.buildTarget", required = true, defaultValue = "${codename1.defaultBuildTarget}")
    private String buildTarget;

    /**
     * Flag to indicate whether to use an automated build or not.
     */
    @Parameter(property = "automated", defaultValue = "false")
    private boolean automated;

    /**
     * Stop once the jar the build would send has been assembled and checked,
     * without submitting anything or building locally. The jar is left at
     * target/&lt;finalName&gt;-&lt;buildTarget&gt;-jar-with-dependencies.jar. This is
     * what lets CI inspect the real upload for every target with no account
     * and no network: maven/integration-tests/cn1app-staged-jar-test.sh.
     */
    @Parameter(property = "codename1.stageOnly", defaultValue = "false")
    private boolean stageOnly;

    /**
     * Maven's own resolver, to ask what the application needs without the desktop runtime
     * aggregator (see {@link #computeNeededWithoutDesktopRuntime()}).
     */
    @Component
    private org.eclipse.aether.RepositorySystem resolverSystem;

    /**
     * Flag of whether to open the xcode/android studio project.
     */
    @Parameter(property = "open", defaultValue = "true")
    private boolean open;

    @Override
    protected String helpStep() {
        return "build_submit";
    }

    @Override
    protected String helpAction() {
        StringBuilder sb = new StringBuilder("mvn package");
        if (platform != null && platform.trim().length() > 0) {
            sb.append(" -Dcodename1.platform=").append(platform.trim());
        }
        if (buildTarget != null && buildTarget.trim().length() > 0) {
            sb.append(" -Dcodename1.buildTarget=").append(buildTarget.trim());
        }
        return sb.toString();
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        MavenAppBuilder builder = new MavenAppBuilder();
        builder.platform = platform;
        builder.buildTarget = buildTarget;
        builder.automated = automated;
        builder.stageOnly = stageOnly;
        builder.open = open;
        try {
            builder.execute();
        } catch (BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        } catch (BuildExecutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /**
     * {@link AppBuilder} over this mojo's {@link MavenProjectHost}. The annotation merge,
     * the command-line overlay and the updater go through the mojo's own methods, which
     * know about the reactor (which module produced a classpath element) and the session.
     */
    private final class MavenAppBuilder extends AppBuilder {
        MavenAppBuilder() {
            super(projectHost());
        }

        @Override
        protected void updateCodenameOne(boolean force) throws BuildExecutionException {
            try {
                CN1BuildMojo.this.updateCodenameOne(force);
            } catch (MojoExecutionException ex) {
                throw new BuildExecutionException(ex.getMessage(), ex);
            }
        }

        @Override
        protected void mergeAnnotationBuildHints(Properties target, List<String> classpathElements)
                throws BuildFailureException {
            try {
                CN1BuildMojo.this.mergeAnnotationBuildHints(target, classpathElements);
            } catch (MojoFailureException ex) {
                throw new BuildFailureException(ex.getMessage(), ex);
            }
        }

        @Override
        protected void overlayCommandLineBuildHints(Properties target) {
            CN1BuildMojo.this.overlayCommandLineBuildHints(target);
        }
    }

    @Override
    protected Set<String> neededWithoutDesktopRuntimeKeys() {
        return computeNeededWithoutDesktopRuntime();
    }

    private Set<String> neededWithoutDesktopRuntime;
    private boolean neededWithoutDesktopRuntimeComputed;

    /**
     * What the application's compile dependencies need once the desktop runtime aggregator is
     * taken away, collected (poms only, nothing downloaded) the same way Maven resolved the
     * project. The roots are the compile scope dependencies, because only compile scope
     * reaches the staged jar. {@code null} when the collection fails: nothing is then stripped
     * as desktop runtime, since an upload that is too large fails loudly and one missing
     * classes does not.
     *
     * <p>Known limit, left deliberately: nodes below the roots are not filtered by scope, so
     * an artifact a library needs only at runtime counts as needed. That can only keep an
     * artifact that could have been stripped -- an upload too large, which the build client
     * refuses loudly -- never drop one the application needs.</p>
     */
    private Set<String> computeNeededWithoutDesktopRuntime() {
        if (neededWithoutDesktopRuntimeComputed) {
            return neededWithoutDesktopRuntime;
        }
        neededWithoutDesktopRuntimeComputed = true;
        try {
            RepositorySystemSession repoSession = getSession().getRepositorySession();
            ArtifactTypeRegistry types = repoSession.getArtifactTypeRegistry();
            CollectRequest request = new CollectRequest();
            request.setRootArtifact(RepositoryUtils.toArtifact(project.getArtifact()));
            request.setRepositories(project.getRemoteProjectRepositories());
            for (Dependency dependency : project.getDependencies()) {
                String scope = dependency.getScope();
                if (AppBuilder.isDesktopRuntimeAggregator(dependency.getGroupId(), dependency.getArtifactId())
                        || (scope != null && scope.length() > 0 && !"compile".equals(scope))) {
                    continue;
                }
                request.addDependency(withoutDesktopRuntime(RepositoryUtils.toDependency(dependency, types)));
            }
            DependencyManagement management = project.getDependencyManagement();
            if (management != null) {
                for (Dependency dependency : management.getDependencies()) {
                    request.addManagedDependency(RepositoryUtils.toDependency(dependency, types));
                }
            }
            CollectResult result = resolverSystem.collectDependencies(repoSession, request);
            Set<String> keys = new HashSet<String>();
            addDependencyKeys(result.getRoot(), keys);
            neededWithoutDesktopRuntime = keys;
        } catch (Exception ex) {
            getLog().warn("Could not determine which dependencies need the desktop runtime binaries ("
                    + DESKTOP_RUNTIME_BINARIES_ARTIFACT_ID + ") for another reason, so none of them are "
                    + "left out of the staged jar. The upload may be too large: " + ex);
            neededWithoutDesktopRuntime = null;
        }
        return neededWithoutDesktopRuntime;
    }

    /**
     * The dependency with the desktop runtime aggregator excluded from everything below it.
     * Skipping the aggregator where the project declares it is not enough: a library can
     * bring it in transitively, and the collection would then find it again through that
     * library and report all of ffmpeg as needed. Exclusions apply at every depth.
     */
    static org.eclipse.aether.graph.Dependency withoutDesktopRuntime(org.eclipse.aether.graph.Dependency dependency) {
        List<Exclusion> exclusions = new ArrayList<Exclusion>(dependency.getExclusions());
        exclusions.add(new Exclusion(GROUP_ID, DESKTOP_RUNTIME_BINARIES_ARTIFACT_ID, "*", "*"));
        return dependency.setExclusions(exclusions);
    }

    private static void addDependencyKeys(DependencyNode node, Set<String> keys) {
        if (node == null) {
            return;
        }
        org.eclipse.aether.artifact.Artifact a = node.getArtifact();
        if (a != null && node.getDependency() != null) {
            keys.add(AppBuilder.dependencyKey(a.getGroupId(), a.getArtifactId(), a.getClassifier()));
        }
        for (DependencyNode child : node.getChildren()) {
            addDependencyKeys(child, keys);
        }
    }

    // ---- The build's decisions, kept reachable under their old names ----------------------
    // They live in AppBuilder now; these are what the tests (and anything else that asked
    // CN1BuildMojo) call.

    static boolean isLocalJavascriptBuild(String buildTarget) {
        return AppBuilder.isLocalJavascriptBuild(buildTarget);
    }

    static boolean isSuppliedByBuildServer(String groupId, String artifactId, String buildTarget) {
        return AppBuilder.isSuppliedByBuildServer(groupId, artifactId, buildTarget);
    }

    static boolean isStrippedFromStagedJar(String groupId, String artifactId, String scope, String buildTarget) {
        return AppBuilder.isStrippedFromStagedJar(groupId, artifactId, scope, buildTarget);
    }

    static boolean isStrippedAsDesktopRuntime(String dependencyKey, Set<String> neededElsewhere) {
        return AppBuilder.isStrippedAsDesktopRuntime(dependencyKey, neededElsewhere);
    }

    static String dependencyKey(String groupId, String artifactId, String classifier) {
        return AppBuilder.dependencyKey(groupId, artifactId, classifier);
    }

    static boolean mayReuseStagedJar(String recorded, String current, long jarModified, long sessionStart) {
        return AppBuilder.mayReuseStagedJar(recorded, current, jarModified, sessionStart);
    }

    static void discardStagedJar(File jar) throws MojoExecutionException {
        try {
            AppBuilder.discardStagedJar(jar);
        } catch (BuildExecutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    static String describeStagedInputs(List<File> jarsToMerge) {
        return AppBuilder.describeStagedInputs(jarsToMerge);
    }

    static boolean isDesktopRuntimeBinary(String groupId, String artifactId, List<String> dependencyTrail) {
        return AppBuilder.isDesktopRuntimeBinary(groupId, artifactId, dependencyTrail);
    }

    static boolean isNativeMacOsTarget(String buildTarget) {
        return AppBuilder.isNativeMacOsTarget(buildTarget);
    }

    static String hardenPlatformForBuildTarget(String buildTarget) {
        return AppBuilder.hardenPlatformForBuildTarget(buildTarget);
    }

    static boolean allAppleHardeningSlicesOptedOut(Properties settings) {
        return AppBuilder.allAppleHardeningSlicesOptedOut(settings);
    }

    static boolean hardeningReducesToOff(Properties settings, String level) {
        return AppBuilder.hardeningReducesToOff(settings, level);
    }

    static boolean hardeningReducesToOff(Properties settings, String level, String platform) {
        return AppBuilder.hardeningReducesToOff(settings, level, platform);
    }

    static boolean hardeningRequestsAnyTransform(Properties settings, String level) {
        return AppBuilder.hardeningRequestsAnyTransform(settings, level);
    }

    static boolean hardeningRequestsAnyTransform(Properties settings, String level, String platform) {
        return AppBuilder.hardeningRequestsAnyTransform(settings, level, platform);
    }

    static void mirrorSecondaryEntryPointsToBuildArgs(Properties props) {
        AppBuilder.mirrorSecondaryEntryPointsToBuildArgs(props);
    }

    static void resolveAppExtensionBuildTypeQualifiers(Properties props, String buildTarget) {
        AppBuilder.resolveAppExtensionBuildTypeQualifiers(props, buildTarget);
    }

    static String roleSuffixFor(String base, String extension,
            java.util.Map<String, java.util.Set<String>> basesByExtension) {
        return AppBuilder.roleSuffixFor(base, extension, basesByExtension);
    }

    static String roleSuffixOf(String base) {
        return AppBuilder.roleSuffixOf(base);
    }

    private static boolean isLocalBuildTarget(String buildTarget) {
        return AppBuilder.isLocalBuildTarget(buildTarget);
    }

    private static void putSecondaryEntryPointArguments(BuildRequest r, Properties props) {
        AppBuilder.putSecondaryEntryPointArguments(r, props);
    }

    private SortedProperties mergeRequiredProperties(String libraryName, Properties libProps, Properties projectProps)
            throws Exception {
        return AppBuilder.mergeRequiredProperties(libraryName, libProps, projectProps);
    }
}
