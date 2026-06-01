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
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that MacNativeBuilder's entitlements writer mirrors the iOS
/// privacy plist defaults injected by `AiDependencyTable` into the
/// corresponding sandbox device entitlements. Sandboxed Mac apps that
/// touch the camera or microphone need explicit device entitlements,
/// not just an Info.plist usage description -- the OS refuses to open
/// AVCaptureSession otherwise.
class MacNativeBuilderEntitlementsTest {

    @Test
    void appStoreSandboxedAddsCameraAndMicEntitlementsWhenPlistDefaultsAreSet(@TempDir Path tmp)
            throws IOException {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.putArgument("macNative.enabled", "true");
        req.putArgument("macNative.distribution", "appStore");
        req.putArgument("macNative.teamId", "ABCDEFG123");
        // These are what AiDependencyTable injects when com.codename1.camera.*
        // is referenced anywhere in the user app.
        req.putArgument("ios.NSCameraUsageDescription", "Used to capture photos and video.");
        req.putArgument("ios.NSMicrophoneUsageDescription",
                "Used to capture audio for video recording.");

        String body = writeEntitlements(req, tmp, "MyApp");

        assertTrue(body.contains("<key>com.apple.security.app-sandbox</key>"),
                "appStore channel should be sandboxed by default");
        assertTrue(body.contains("<key>com.apple.security.device.camera</key>\n    <true/>"),
                "NSCameraUsageDescription should bring in com.apple.security.device.camera");
        assertTrue(body.contains("<key>com.apple.security.device.microphone</key>\n    <true/>"),
                "NSMicrophoneUsageDescription should bring in com.apple.security.device.microphone");
    }

    @Test
    void developerIdNotSandboxedSkipsDeviceEntitlements(@TempDir Path tmp) throws IOException {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.putArgument("macNative.enabled", "true");
        req.putArgument("macNative.distribution", "developerID");
        req.putArgument("macNative.teamId", "ABCDEFG123");
        req.putArgument("ios.NSCameraUsageDescription", "Used to capture photos.");

        String body = writeEntitlements(req, tmp, "MyApp");

        // DeveloperID builds aren't sandboxed by default, so the device
        // entitlements are irrelevant -- the OS prompt is driven entirely
        // by the Info.plist usage description.
        assertFalse(body.contains("com.apple.security.app-sandbox"));
        assertFalse(body.contains("com.apple.security.device.camera"),
                "Non-sandboxed builds don't need device entitlements");
    }

    @Test
    void sandboxedAppWithoutCameraPlistGetsNoCameraEntitlement(@TempDir Path tmp)
            throws IOException {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.putArgument("macNative.enabled", "true");
        req.putArgument("macNative.distribution", "appStore");
        req.putArgument("macNative.teamId", "ABCDEFG123");
        // No camera plist hint -- the app doesn't use the camera.

        String body = writeEntitlements(req, tmp, "MyApp");

        assertTrue(body.contains("<key>com.apple.security.app-sandbox</key>"));
        assertFalse(body.contains("com.apple.security.device.camera"));
        assertFalse(body.contains("com.apple.security.device.microphone"));
    }

    @Test
    void developerCanForceCameraEntitlementOn(@TempDir Path tmp) throws IOException {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.putArgument("macNative.enabled", "true");
        req.putArgument("macNative.distribution", "appStore");
        req.putArgument("macNative.teamId", "ABCDEFG123");
        // Even without the iOS plist hint, an explicit opt-in forces the
        // entitlement to be written. Useful when an app uses a 3rd-party
        // camera path the AiDependencyTable scanner doesn't recognise.
        req.putArgument("macNative.entitlements.device.camera", "true");

        String body = writeEntitlements(req, tmp, "MyApp");

        assertTrue(body.contains("com.apple.security.device.camera"));
    }

    @Test
    void developerCanForceCameraEntitlementOff(@TempDir Path tmp) throws IOException {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.putArgument("macNative.enabled", "true");
        req.putArgument("macNative.distribution", "appStore");
        req.putArgument("macNative.teamId", "ABCDEFG123");
        req.putArgument("ios.NSCameraUsageDescription", "Used to capture photos.");
        // Override the auto-detection: this app declared the iOS hint
        // for App Review purposes but doesn't actually open the Mac
        // camera, so we suppress the sandbox entitlement.
        req.putArgument("macNative.entitlements.device.camera", "false");

        String body = writeEntitlements(req, tmp, "MyApp");

        assertFalse(body.contains("com.apple.security.device.camera"),
                "Explicit opt-out should win over the auto-detection");
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private static String writeEntitlements(BuildRequest req, Path tmpDir, String mainClass)
            throws IOException {
        IPhoneBuilder owner = new IPhoneBuilder();
        MacNativeBuilder b = new MacNativeBuilder(owner);
        b.parseHints(req);
        File appSrcDir = tmpDir.toFile();
        b.writeEntitlements(req, appSrcDir);
        File ent = new File(appSrcDir, mainClass + ".entitlements");
        if (!ent.exists()) {
            throw new AssertionError("Entitlements file not written: " + ent);
        }
        return new String(Files.readAllBytes(ent.toPath()));
    }
}
