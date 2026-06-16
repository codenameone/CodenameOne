package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Build wrapper for the native Linux (ParparVM -&gt; CMake/Ninja, GTK3/Cairo)
 * cloud target. Produces a standalone Linux ELF ({@code linux-device}) with no
 * JVM dependency -- the Linux analog of the iOS/Windows device builds, distinct
 * from packaging the app as an executable jar that runs on a JVM.
 */
@Mojo(name="buildLinuxDevice", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildLinuxDeviceMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "linux";
    }

    @Override
    protected String getBuildTarget() {
        return "linux-device";
    }
}
