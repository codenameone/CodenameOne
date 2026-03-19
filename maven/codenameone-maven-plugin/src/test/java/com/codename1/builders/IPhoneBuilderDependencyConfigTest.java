package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IPhoneBuilderDependencyConfigTest {

    @Test
    void autoResolvesPodsOnly() throws Exception {
        BuildRequest request = requestWithArgs(
                "ios.dependencyManager", "auto"
        );

        IOSDependencyConfig config = IOSDependencyManager.resolve(request, "AFNetworking");
        assertEquals(IOSDependencyManager.COCOAPODS, config.mode);
        assertTrue(config.usesCocoaPods());
        assertFalse(config.usesSwiftPackages());
        assertEquals("AFNetworking", config.iosPods);
    }

    @Test
    void autoResolvesSpmOnly() throws Exception {
        BuildRequest request = requestWithArgs(
                "ios.dependencyManager", "auto",
                "ios.spm.packages", "swift-collections|https://github.com/apple/swift-collections.git|from:1.1.0",
                "ios.spm.products.swift-collections", "Collections"
        );

        IOSDependencyConfig config = IOSDependencyManager.resolve(request, "");
        assertEquals(IOSDependencyManager.SPM, config.mode);
        assertTrue(config.usesSwiftPackages());
        assertFalse(config.usesCocoaPods());
        assertEquals(1, config.swiftPackages.size());
        assertEquals("Collections", config.swiftPackages.get(0).products.get(0));
    }

    @Test
    void autoResolvesBothWhenBothHintFamiliesPresent() throws Exception {
        BuildRequest request = requestWithArgs(
                "ios.spm.packages", "swift-collections|https://github.com/apple/swift-collections.git|from:1.1.0",
                "ios.spm.products.swift-collections", "Collections"
        );

        IOSDependencyConfig config = IOSDependencyManager.resolve(request, "AFNetworking");
        assertEquals(IOSDependencyManager.BOTH, config.mode);
    }

    @Test
    void autoResolvesNoneWithoutDependencyHints() throws Exception {
        IOSDependencyConfig config = IOSDependencyManager.resolve(new BuildRequest(), "");
        assertEquals(IOSDependencyManager.NONE, config.mode);
        assertTrue(config.swiftPackages.isEmpty());
    }

    @Test
    void explicitSpmRequiresSpmPackages() {
        BuildRequest request = requestWithArgs("ios.dependencyManager", "spm");
        BuildException ex = assertThrows(BuildException.class, () -> IOSDependencyManager.resolve(request, ""));
        assertTrue(ex.getMessage().contains("ios.spm.packages"));
    }

    @Test
    void explicitCocoaPodsRequiresPods() {
        BuildRequest request = requestWithArgs("ios.dependencyManager", "cocoapods");
        BuildException ex = assertThrows(BuildException.class, () -> IOSDependencyManager.resolve(request, ""));
        assertTrue(ex.getMessage().contains("ios.pods"));
    }

    @Test
    void explicitBothRequiresBothHintFamilies() {
        BuildRequest request = requestWithArgs(
                "ios.dependencyManager", "both",
                "ios.spm.packages", "swift-collections|https://github.com/apple/swift-collections.git|from:1.1.0",
                "ios.spm.products.swift-collections", "Collections"
        );
        BuildException ex = assertThrows(BuildException.class, () -> IOSDependencyManager.resolve(request, ""));
        assertTrue(ex.getMessage().contains("both ios.pods and ios.spm.packages"));
    }

    @Test
    void parsesSupportedSwiftPackageRequirementsAndProducts() throws Exception {
        BuildRequest request = requestWithArgs(
                "ios.spm.packages",
                "pkg1|https://example.com/pkg1.git|from:1.2.3;" +
                        "pkg2|https://example.com/pkg2.git|exact:2.0.0;" +
                        "pkg3|https://example.com/pkg3.git|branch:main;" +
                        "pkg4|https://example.com/pkg4.git|revision:abc123;" +
                        "pkg5|https://example.com/pkg5.git|range:1.0.0..<2.0.0",
                "ios.spm.products.pkg1", "P1,P1Support",
                "ios.spm.products.pkg2", "P2",
                "ios.spm.products.pkg3", "P3",
                "ios.spm.products.pkg4", "P4",
                "ios.spm.products.pkg5", "P5"
        );

        List<SwiftPackageSpec> specs = SwiftPackageSpec.parse(request);
        assertEquals(5, specs.size());
        assertEquals("pkg1", specs.get(0).identity);
        assertEquals("https://example.com/pkg1.git", specs.get(0).url);
        assertEquals(2, specs.get(0).products.size());
        assertEquals("range:1.0.0..<2.0.0", specs.get(4).requirement);
    }

    @Test
    void rejectsMalformedSwiftPackageEntry() {
        BuildRequest request = requestWithArgs(
                "ios.spm.packages", "swift-collections|https://github.com/apple/swift-collections.git",
                "ios.spm.products.swift-collections", "Collections"
        );
        assertThrows(BuildException.class, () -> SwiftPackageSpec.parse(request));
    }

    @Test
    void rejectsSwiftPackageWithoutProducts() {
        BuildRequest request = requestWithArgs(
                "ios.spm.packages", "swift-collections|https://github.com/apple/swift-collections.git|from:1.1.0"
        );
        BuildException ex = assertThrows(BuildException.class, () -> SwiftPackageSpec.parse(request));
        assertTrue(ex.getMessage().contains("ios.spm.products.swift-collections"));
    }

    @Test
    void rejectsInvalidRangeRequirement() {
        BuildRequest request = requestWithArgs(
                "ios.spm.packages", "swift-collections|https://github.com/apple/swift-collections.git|range:1.1.0",
                "ios.spm.products.swift-collections", "Collections"
        );
        BuildException ex = assertThrows(BuildException.class, () -> SwiftPackageSpec.parse(request));
        assertTrue(ex.getMessage().contains("range"));
    }

    @Test
    void dependencyManagerHintParsingIsCaseInsensitive() throws Exception {
        assertEquals(IOSDependencyManager.SPM, IOSDependencyManager.fromHint("SpM"));
        assertEquals(IOSDependencyManager.AUTO, IOSDependencyManager.fromHint(""));
    }

    @Test
    void rejectsUnknownDependencyManagerHint() {
        assertThrows(BuildException.class, () -> IOSDependencyManager.fromHint("gradle"));
    }

    private BuildRequest requestWithArgs(String... kvPairs) {
        BuildRequest out = new BuildRequest();
        for (int i = 0; i < kvPairs.length; i += 2) {
            out.putArgument(kvPairs[i], kvPairs[i + 1]);
        }
        return out;
    }
}
