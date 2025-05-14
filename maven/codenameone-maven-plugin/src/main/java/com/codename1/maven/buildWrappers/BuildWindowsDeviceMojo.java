package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name="buildWindowsUWP", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildWindowsDeviceMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "win";
    }

    @Override
    protected String getBuildTarget() {
        return "windows-device";
    }
}
