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
        // "win", not "windows". This value activates the module profile in the
        // generated project's root pom, and that profile matches the value the
        // win module itself declares -- which is "win". Passing "windows"
        // matched no profile, so the win module never entered the reactor and
        // the wrapper's nested build reported success having produced nothing.
        // Nothing else reads the platform as "windows"; the build TARGET stays
        // "windows-device", which is a separate namespace.
        return "win";
    }

    @Override
    protected String getBuildTarget() {
        return "windows-device";
    }
}
