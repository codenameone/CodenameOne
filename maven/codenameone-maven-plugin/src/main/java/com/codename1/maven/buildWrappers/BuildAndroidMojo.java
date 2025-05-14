package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name="buildAndroid", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildAndroidMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "android";
    }

    @Override
    protected String getBuildTarget() {
        return "android-device";
    }
}
