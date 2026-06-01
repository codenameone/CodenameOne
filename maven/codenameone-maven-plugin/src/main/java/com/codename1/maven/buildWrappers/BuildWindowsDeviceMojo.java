package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Build wrapper for the native Windows (ParparVM -> clang-cl) target. Distinct
 * from {@link BuildWindowsDesktopMojo}, which bundles the JVM/JavaSE app
 * ({@code windows-desktop}); this produces a native Win32 executable
 * ({@code windows-device}), the Windows analog of the iOS device build.
 */
@Mojo(name="buildWindowsDevice", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildWindowsDeviceMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "windows";
    }

    @Override
    protected String getBuildTarget() {
        return "windows-device";
    }
}
