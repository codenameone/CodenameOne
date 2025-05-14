package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name="buildJavascript", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildJavascriptMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "javascript";
    }

    @Override
    protected String getBuildTarget() {
        return "javascript";
    }
}
