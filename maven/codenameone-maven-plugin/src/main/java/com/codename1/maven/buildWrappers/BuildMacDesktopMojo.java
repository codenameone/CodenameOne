package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name="buildMacDesktop", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildMacDesktopMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "javase";
    }

    @Override
    protected String getBuildTarget() {
        return "mac-os-x-desktop";
    }
}
