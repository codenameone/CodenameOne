package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Build wrapper for the native Windows ("win32") target: a real Win32 executable
 * produced by ParparVM -&gt; clang-cl, the Windows analog of the iOS device build.
 *
 * <p>This is the user-facing goal for the cloud "win32" build target. A regular
 * (release) build produces two binaries -- x64 and arm64, optimized and stripped;
 * a debug build (the {@code windows.debug} build hint) produces a single x64 exe
 * with symbols. The build runs in the Linux build cloud's Android/JavaScript
 * daemon group.</p>
 *
 * <p>Distinct from {@link BuildWindowsDesktopMojo}, which bundles the JVM/JavaSE
 * app ({@code windows-desktop}). {@link BuildWindowsDeviceMojo} is the legacy goal
 * name for the same {@code windows-device} build target.</p>
 */
@Mojo(name="buildWin32", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildWin32Mojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "windows";
    }

    @Override
    protected String getBuildTarget() {
        return "windows-device";
    }
}
