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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The class-scan rules that decide what the macOS build compiles and what its
 * signature asks for.
 */
public class MacOSNativeBuilderScanTest {

    /**
     * The exclusion list has to match the framework it is excluding.
     *
     * <p>It is an exclusion of NAMES, so it decays silently in the worst
     * direction: a new framework class that opens a camera would put the
     * entitlement and the privacy string back into every macOS application, and
     * nothing would say so. This disassembles the built framework and fails when
     * the set of internal callers stops matching -- which is the moment the
     * decision has to be made again, not months later in a store review.</p>
     *
     * <p>Skipped rather than failed when the framework jar is not on the test
     * classpath, so this is a gate where it can be one and silent where it
     * cannot, rather than a spurious failure.</p>
     */
    @Test
    public void theExcludedFrameworkCallersAreTheOnesTheFrameworkActuallyHas() throws Exception {
        java.security.CodeSource source =
                com.codename1.camera.Camera.class.getProtectionDomain().getCodeSource();
        org.junit.Assume.assumeTrue("the framework jar is on the test classpath",
                source != null && source.getLocation() != null);
        java.io.File jar = new java.io.File(source.getLocation().toURI());
        org.junit.Assume.assumeTrue("the framework is packaged as a jar", jar.isFile());

        java.util.Set<String> found = new java.util.TreeSet<String>();
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar);
        try {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                final String owner =
                        entry.getName().substring(0, entry.getName().length() - ".class".length());
                java.io.InputStream in = zip.getInputStream(entry);
                try {
                    new org.objectweb.asm.ClassReader(in).accept(
                            new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                                @Override
                                public org.objectweb.asm.MethodVisitor visitMethod(int a, String n,
                                        String d, String sig, String[] ex) {
                                    return new org.objectweb.asm.MethodVisitor(
                                            org.objectweb.asm.Opcodes.ASM9) {
                                        @Override
                                        public void visitMethodInsn(int op, String o, String n2,
                                                String d2, boolean itf) {
                                            if (MacOSNativeBuilder.opensCameraSession(o, n2)) {
                                                found.add(owner);
                                            }
                                        }
                                    };
                                }
                            }, org.objectweb.asm.ClassReader.SKIP_FRAMES);
                } finally {
                    in.close();
                }
            }
        } finally {
            zip.close();
        }

        java.util.Set<String> unexcluded = new java.util.TreeSet<String>();
        for (String caller : found) {
            if (!MacOSNativeBuilder.isFrameworkCameraCaller(caller)) {
                unexcluded.add(caller);
            }
        }
        assertTrue("the framework gained a class that opens a camera: " + unexcluded
                        + ". Decide whether an application linking it genuinely needs the camera "
                        + "entitlement; if not, add it to FRAMEWORK_CAMERA_CALLERS.",
                unexcluded.isEmpty());
    }

    /**
     * A camera session opens the microphone unless the application says not to.
     *
     * <p>CameraSessionOptions.captureAudio is initialised to true and CN1Camera
     * adds an AVMediaTypeAudio input whenever it is set, so a plain
     * Camera.open() takes the microphone. Without this the bundle carried camera
     * metadata and no NSMicrophoneUsageDescription, and macOS terminates the
     * process the moment the session starts.</p>
     *
     * <p>The captureAudio(false) argument is not consulted even though the
     * scanner could see it: an application that disables audio on one session
     * and leaves another on its default would then be reported as using no
     * microphone and terminated on the second. Over-reporting is the recoverable
     * direction and macos.entitlements.device.microphone=false is the documented
     * way out of it.</p>
     */
    @Test
    public void aLowLevelCameraSessionTakesTheMicrophoneWithIt() {
        assertTrue("opening a session opens the default audio input",
                MacOSNativeBuilder.applicationOpensCameraMicrophone(
                        "com/example/MyForm", "com/codename1/camera/Camera", "open"));
        assertTrue("requestPermissions opens a probe session with the same options",
                MacOSNativeBuilder.applicationOpensCameraMicrophone(
                        "com/example/MyForm",
                        "com/codename1/camera/Camera", "requestPermissions"));

        assertFalse("the framework's own callers are in every build",
                MacOSNativeBuilder.applicationOpensCameraMicrophone(
                        "com/codename1/camera/Camera",
                        "com/codename1/camera/Camera", "open"));
        assertFalse(MacOSNativeBuilder.applicationOpensCameraMicrophone(
                "com/codename1/ai/vision/CodeScanner",
                "com/codename1/camera/Camera", "requestPermissions"));

        // And the recorder rule is untouched by it, so the define keeps its own
        // meaning: a camera session is not a reason to compile the recorder in.
        assertFalse("a camera session is not a media recorder",
                MacOSNativeBuilder.opensMicrophone(
                        "com/codename1/camera/Camera", "open"));
    }

    /**
     * The low level camera API counts, but only when the APPLICATION reaches it.
     *
     * <p>The tree this scan reads is the application merged with the framework,
     * so a framework class's own references are present in every build ever
     * made. Counting them would put the camera entitlement and the camera
     * privacy string into every macOS application; not counting the application
     * denies a documented API the entitlement it needs, and the app is refused
     * the first time it opens a camera. The caller is the only thing that
     * separates the two.</p>
     */
    @Test
    public void theLowLevelCameraApiCountsForTheApplicationAndNotForTheFramework() {
        assertTrue("an application opening a session opens a camera",
                MacOSNativeBuilder.applicationOpensCameraSession(
                        "com/example/MyForm", "com/codename1/camera/Camera", "open"));
        assertTrue(MacOSNativeBuilder.applicationOpensCameraSession(
                "com/example/MyForm", "com/codename1/camera/Camera", "getDefault"));
        assertTrue(MacOSNativeBuilder.applicationOpensCameraSession(
                "com/example/MyForm", "com/codename1/camera/Camera", "getCameras"));

        assertFalse("the framework's own code scanner is in every build",
                MacOSNativeBuilder.applicationOpensCameraSession(
                        "com/codename1/ai/vision/CodeScanner",
                        "com/codename1/camera/Camera", "open"));
        assertFalse("nested classes of one are the same class",
                MacOSNativeBuilder.applicationOpensCameraSession(
                        "com/codename1/ai/vision/CodeScanner$Session",
                        "com/codename1/camera/Camera", "open"));
        assertFalse("and so is this port's own modal capture",
                MacOSNativeBuilder.applicationOpensCameraSession(
                        "com/codename1/impl/mac/MacCameraCapture",
                        "com/codename1/camera/Camera", "open"));

        // An application under the framework's own namespace is still an
        // application: only the exact names are excluded, never a prefix.
        assertTrue("a namespace is not an exclusion",
                MacOSNativeBuilder.applicationOpensCameraSession(
                        "com/codename1/ai/vision/MyOwnScanner",
                        "com/codename1/camera/Camera", "open"));

        assertFalse("asking whether a camera exists opens none",
                MacOSNativeBuilder.applicationOpensCameraSession(
                        "com/example/MyForm", "com/codename1/camera/Camera", "isSupported"));
    }

    /**
     * The recorder rule, which is also most of the microphone entitlement rule.
     *
     * <p>It used to be the whole of it. A low level camera session opens the
     * microphone too, and that half is entitlement-only: the camera's audio
     * input is compiled under the camera define rather than under
     * INCLUDE_MICROPHONE_USAGE, so switching the recorder on for it would
     * compile in a backend nothing calls. An entitlement with no recorder behind
     * it is the correct outcome there, which is why the two decisions are no
     * longer one rule. See
     * {@link #aLowLevelCameraSessionTakesTheMicrophoneWithIt()}.</p>
     */
    @Test
    public void theMicrophoneRuleFollowsTheEntryPointThatOpensOne() {
        assertTrue("the recorder is what opens a microphone in com.codename1.media",
                MacOSNativeBuilder.opensMicrophone(
                        "com/codename1/media/MediaManager", "createMediaRecorder"));
        assertTrue(MacOSNativeBuilder.opensMicrophone(
                "com/codename1/capture/Capture", "captureAudio"));
        assertTrue("video capture records sound with the picture",
                MacOSNativeBuilder.opensMicrophone(
                        "com/codename1/capture/Capture", "captureVideo"));

        assertFalse("a photo opens no microphone",
                MacOSNativeBuilder.opensMicrophone(
                        "com/codename1/capture/Capture", "capturePhoto"));
        assertFalse("playback is not recording",
                MacOSNativeBuilder.opensMicrophone(
                        "com/codename1/media/MediaManager", "createMedia"));
        // The near miss that makes the package prefix load-bearing:
        // CameraSessionOptions.captureAudio(boolean) is a session flag on a
        // different class, and the screenshot sample calls it with false.
        assertFalse("captureAudio on a camera session is a flag, not a capture",
                MacOSNativeBuilder.opensMicrophone(
                        "com/codename1/camera/CameraSessionOptions", "captureAudio"));
        assertFalse(MacOSNativeBuilder.opensMicrophone(null, "captureAudio"));
        assertFalse(MacOSNativeBuilder.opensMicrophone("com/codename1/capture/Capture", null));
    }

    /**
     * Display carries its own overloads of the capture and recorder entry
     * points, and the scan sees the OWNER of the invocation -- so an application
     * that calls Display.createMediaRecorder names Display, not the media
     * package. Missing that left a recording application with no define, no
     * entitlement and no usage description.
     */
    @Test
    public void theDisplayOverloadsCountAsTheirFeature() {
        assertTrue(MacOSNativeBuilder.opensMicrophone(
                "com/codename1/ui/Display", "createMediaRecorder"));
        assertTrue(MacOSNativeBuilder.opensMicrophone(
                "com/codename1/ui/Display", "captureAudio"));
        assertTrue(MacOSNativeBuilder.opensMicrophone(
                "com/codename1/ui/Display", "captureVideo"));
        assertTrue(MacOSNativeBuilder.opensCamera("com/codename1/ui/Display", "capturePhoto"));
        assertTrue(MacOSNativeBuilder.opensCamera("com/codename1/ui/Display", "captureVideo"));
        assertTrue(MacOSNativeBuilder.opensCamera("com/codename1/capture/Capture", "capturePhoto"));

        assertFalse("a photo opens no microphone, whoever it was asked of",
                MacOSNativeBuilder.opensMicrophone("com/codename1/ui/Display", "capturePhoto"));
        assertFalse("audio opens no camera",
                MacOSNativeBuilder.opensCamera("com/codename1/ui/Display", "captureAudio"));
        // Display is called by every application in existence, so the method has
        // to carry the meaning -- matching on the class alone would declare the
        // camera and the microphone for every build ever made.
        assertFalse(MacOSNativeBuilder.opensMicrophone("com/codename1/ui/Display", "getWidth"));
        assertFalse(MacOSNativeBuilder.opensCamera("com/codename1/ui/Display", "getWidth"));
    }

    /**
     * The package test in usesClass covers an application that names a
     * notification type. This covers the one that does not: a lambda passed to
     * requestNotificationPermission compiles to an invokedynamic whose reported
     * owner is the app class holding the lambda body, so the functional
     * interface is never seen and the permission call is the only evidence.
     */
    @Test
    public void theNotificationRuleCatchesThePermissionEntryPoint() {
        assertTrue(MacOSNativeBuilder.usesNotifications(
                "com/codename1/ui/Display", "requestNotificationPermission"));

        assertFalse("a different Display method says nothing about notifications",
                MacOSNativeBuilder.usesNotifications("com/codename1/ui/Display", "getDisplayWidth"));
        assertFalse("the name alone is not enough -- it has to be Display's",
                MacOSNativeBuilder.usesNotifications(
                        "com/example/MyHelper", "requestNotificationPermission"));
        assertFalse(MacOSNativeBuilder.usesNotifications(null, "requestNotificationPermission"));
        assertFalse(MacOSNativeBuilder.usesNotifications("com/codename1/ui/Display", null));
    }

    /**
     * The deployment-target hint reaches the project file, not just the
     * xcodebuild command line.
     *
     * <p>The command line settles only the build this run performs, so a
     * source-only deliverable or an {@code includeSource} export used to hand
     * the customer a project still pinned to the template floor -- and the
     * symptom, an availability error, appears only once they open it.</p>
     */
    @Test
    public void theDeploymentTargetIsFoundInEveryBuildConfiguration() {
        String pbxproj = ""
                + "\t\t\tbuildSettings = {\n"
                + "\t\t\t\tMACOSX_DEPLOYMENT_TARGET = 11.0;\n"
                + "\t\t\t\tSDKROOT = macosx;\n"
                + "\t\t\t};\n"
                + "\t\t\tbuildSettings = {\n"
                + "\t\t\t\tMACOSX_DEPLOYMENT_TARGET = 11.0;\n"
                + "\t\t\t};\n";
        java.util.Set<String> found =
                MacOSNativeBuilder.deploymentTargetAssignments(pbxproj);
        assertEquals("both configurations carry the same literal, so one entry",
                1, found.size());
        assertTrue(found.contains("MACOSX_DEPLOYMENT_TARGET = 11.0;"));
    }

    /**
     * Two configurations that disagree are both rewritten. A project edited by
     * hand can carry a different floor per configuration, and replacing only
     * the first would leave Release pinned while Debug moved.
     */
    @Test
    public void configurationsThatDisagreeAreBothCollected() {
        java.util.Set<String> found = MacOSNativeBuilder.deploymentTargetAssignments(
                "MACOSX_DEPLOYMENT_TARGET = 11.0;\nMACOSX_DEPLOYMENT_TARGET = 12.3;\n");
        assertEquals(2, found.size());
    }

    /**
     * The match stops at the semicolon and cannot run past a line. Without
     * that bound the replacement would swallow whatever setting follows.
     */
    @Test
    public void theMatchIsBoundedToItsOwnAssignment() {
        java.util.Set<String> found = MacOSNativeBuilder.deploymentTargetAssignments(
                "\t\t\t\tMACOSX_DEPLOYMENT_TARGET = 13.0;\n\t\t\t\tSDKROOT = macosx;\n");
        assertEquals(1, found.size());
        assertEquals("MACOSX_DEPLOYMENT_TARGET = 13.0;", found.iterator().next());
    }

    /**
     * A template that no longer carries the setting yields nothing, which is
     * what makes the builder report it rather than claim success. Silence here
     * would be a project shipped on the wrong floor with a green build.
     */
    @Test
    public void aTemplateWithoutTheSettingYieldsNothing() {
        assertTrue(MacOSNativeBuilder.deploymentTargetAssignments(
                "\t\t\t\tSDKROOT = macosx;\n").isEmpty());
        assertTrue(MacOSNativeBuilder.deploymentTargetAssignments(null).isEmpty());
    }
}
