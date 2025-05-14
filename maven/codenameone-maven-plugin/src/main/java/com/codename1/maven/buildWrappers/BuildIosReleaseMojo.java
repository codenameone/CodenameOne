package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name="buildIosRelease", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildIosReleaseMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "ios";
    }

    @Override
    protected String getBuildTarget() {
        return "ios-device-release";
    }
}
