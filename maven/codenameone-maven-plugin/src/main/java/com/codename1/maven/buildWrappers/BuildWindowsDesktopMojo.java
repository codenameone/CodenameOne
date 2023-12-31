package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name="buildWindowsDesktop", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildWindowsDesktopMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "javase";
    }

    @Override
    protected String getBuildTarget() {
        return "windows-desktop";
    }
}
