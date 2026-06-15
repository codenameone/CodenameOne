package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Build wrapper for the native Mac (ParparVM Catalyst slice) cloud target. Distinct
 * from {@link BuildMacDesktopMojo}, which bundles the JVM/JavaSE app
 * ({@code mac-os-x-desktop}); this produces a native Mac {@code .app}
 * ({@code mac-os-x-native}), the Mac analog of the iOS/Windows device builds. It
 * rides the iOS pipeline with {@code macNative.enabled} (set by the build mojo for
 * this target), so the platform is {@code ios}.
 *
 * <p>Filled a uniformity gap: {@code windows-device} had a wrapper mojo + IDE entry
 * but the equivalent native-Mac target was reachable only by hand.</p>
 */
@Mojo(name="buildMacNative", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildMacNativeMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "ios";
    }

    @Override
    protected String getBuildTarget() {
        return "mac-os-x-native";
    }
}
