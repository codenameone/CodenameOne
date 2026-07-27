/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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

    @Test
    void appendsEachRequiredAiFrameworkIndependently() throws Exception {
        Method method = IPhoneBuilder.class.getDeclaredMethod(
                "appendFrameworks", String.class, String[].class);
        method.setAccessible(true);
        String value = (String) method.invoke(null, "Vision.framework",
                new String[] {"Vision.framework", "CoreImage.framework",
                        "CoreVideo.framework"});
        assertEquals("Vision.framework;CoreImage.framework;CoreVideo.framework",
                value);

        value = (String) method.invoke(null,
                "CoreML.framework;Accelerate.framework",
                new String[] {"CoreML.framework", "Metal.framework",
                        "Accelerate.framework"});
        assertEquals("CoreML.framework;Accelerate.framework;Metal.framework",
                value);

        value = (String) method.invoke(null, "Vision.framework",
                new String[] {"NaturalLanguage.framework"});
        assertEquals("Vision.framework;NaturalLanguage.framework", value);

        value = (String) method.invoke(null, "ThirdPartyVision.framework",
                new String[] {"Vision.framework"});
        assertEquals("ThirdPartyVision.framework;Vision.framework", value);
    }

    @Test
    void dependencyFloorRaisesExplicitDeploymentTarget() throws Exception {
        IPhoneBuilder builder = new IPhoneBuilder();
        Method addTarget = IPhoneBuilder.class.getDeclaredMethod(
                "addMinDeploymentTarget", String.class);
        addTarget.setAccessible(true);
        addTarget.invoke(builder, "15.5");

        Method getTarget = IPhoneBuilder.class.getDeclaredMethod(
                "getDeploymentTarget", BuildRequest.class);
        getTarget.setAccessible(true);
        String target = (String) getTarget.invoke(builder,
                requestWithArgs("ios.deployment_target", "14.0"));

        assertEquals("15.5", target);
    }

    private BuildRequest requestWithArgs(String... kvPairs) {
        BuildRequest out = new BuildRequest();
        for (int i = 0; i < kvPairs.length; i += 2) {
            out.putArgument(kvPairs[i], kvPairs[i + 1]);
        }
        return out;
    }
}
