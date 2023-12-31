package com.codename1.maven.buildWrappers;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

/**
 * Base class for build wrapper Mojos.
 *
 * Wrapper mojos were created to make it easier to build Codename One targets
 * directly in IDEs like IntelliJ without having to provide any properties or profiles.
 *
 * The user will see "buildAndroid", "buildIos" and "buildIosXcodeProject" goals, etc..
 * listed in the Maven sidebar, and they can just double-click them to invoke them
 * directly.
 */
public abstract class AbstractBuildWrapperMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.isExecutionRoot()) {
            getLog().info("Skipping execution for non-root project");
            return;
        }
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom.xml"));
        request.setGoals(Collections.singletonList("package"));

        Properties properties = new Properties();
        properties.setProperty("skipTests", "true");
        properties.setProperty("codename1.platform", getPlatform());
        if (getBuildTarget() != null) {
            properties.setProperty("codename1.buildTarget", getBuildTarget());
        }
        request.setProperties(properties);

        if (buildExecutableJar()) {
            request.setProfiles(Collections.singletonList("executable-jar"));
        }

        Invoker invoker = new DefaultInvoker();

        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoFailureException("Failed to build project with exit code " + result.getExitCode());
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Failed to invoke Maven", e);
        }
    }

    protected abstract String getPlatform();
    protected abstract String getBuildTarget();

    protected boolean buildExecutableJar() {
        return false;
    }

}
