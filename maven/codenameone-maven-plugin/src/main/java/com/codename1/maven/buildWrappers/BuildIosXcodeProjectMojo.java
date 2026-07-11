package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.Arrays;
import java.util.List;

@Mojo(name="buildIosXcodeProject", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildIosXcodeProjectMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "ios";
    }

    @Override
    protected String getBuildTarget() {
        return "ios-source";
    }

    @Override
    protected List<String> getGoals() {
        // Incremental native source generation can leave an Xcode project with
        // stale VM output.  Always regenerate it from a clean Maven build.
        return Arrays.asList("clean", "package");
    }
}
