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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The class-scan rules that decide what the macOS build compiles and what its
 * signature asks for.
 */
public class MacOSNativeBuilderScanTest {

    /**
     * Excluding the vision classes as CALLERS must not hide an application that
     * USES them.
     *
     * <p>CodeScanner and VisionCameraView open an AVFoundation session through
     * Camera.open() from inside the framework, so they are excluded as callers --
     * otherwise their presence in every build grants every build the camera. The
     * other half of that decision is recognising an application's own reference
     * to them; without it, an application whose only camera use is a code
     * scanner shipped with no NSCameraUsageDescription and was denied the
     * instant the scanner started.</p>
     */
    @Test
    public void anApplicationUsingACodeScannerGetsTheCamera() {
        assertTrue("a code scanner is a camera user",
                MacOSNativeBuilder.applicationUsesCameraBackedVision(
                        "com/example/MyForm", "com/codename1/ai/vision/CodeScanner"));
        assertTrue("its nested types name it too",
                MacOSNativeBuilder.applicationUsesCameraBackedVision(
                        "com/example/MyForm", "com/codename1/ai/vision/CodeScanner$Session"));
        assertTrue(MacOSNativeBuilder.applicationUsesCameraBackedVision(
                "com/example/MyForm", "com/codename1/ai/vision/VisionCameraView"));

        assertFalse("the framework talking to itself is in every build",
                MacOSNativeBuilder.applicationUsesCameraBackedVision(
                        "com/codename1/ai/vision/VisionPipeline",
                        "com/codename1/ai/vision/CodeScanner"));

        assertFalse("an analyzer that opens no camera is not a camera user",
                MacOSNativeBuilder.applicationUsesCameraBackedVision(
                        "com/example/MyForm", "com/codename1/ai/vision/VisionImage"));
    }

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
     * Display.getBluetooth() has to reach the ENTITLEMENTS, not only the define.
     *
     * <p>The call names com.codename1.bluetooth in its return type alone, and
     * neither scan reads return types -- so the capability scan, which keys on
     * the invoked owner, cannot see it however it is written. The native-feature
     * scan catches it with an explicit Display test and enables CoreBluetooth.
     * An application that reaches Bluetooth only that way therefore linked the
     * framework and shipped with no entitlement and no usage description, and
     * macOS denied it at first use in any sandboxed or hardened build.</p>
     *
     * <p>Carried from the one scan that can see it rather than re-derived here,
     * which is what the calendar and push answers already do and for the reason
     * their comments give: detecting a thing twice is how the entitlement and
     * the compiled-in code come to disagree.</p>
     */
    @Test
    public void bluetoothThroughDisplayIsSeenByTheFeatureScanThatCanSeeIt() {
        assertTrue("the feature scan is the one that recognises the Display call",
                MacOSNativeBuilder.reachesBluetoothViaDisplay(
                        "com/codename1/ui/Display", "getBluetooth"));
        assertTrue("CN is the same API under another name",
                MacOSNativeBuilder.reachesBluetoothViaDisplay(
                        "com/codename1/ui/CN", "getBluetooth"));

        assertFalse("another Display getter is not Bluetooth",
                MacOSNativeBuilder.reachesBluetoothViaDisplay(
                        "com/codename1/ui/Display", "getProperty"));
        assertFalse("and the name only counts on Display",
                MacOSNativeBuilder.reachesBluetoothViaDisplay(
                        "com/example/MyForm", "getBluetooth"));
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
        // requestPermissions is NOT decided here any more -- it is decided by
        // its argument, in requestsMicrophonePermission. Both hooks fire for the
        // same call and the argument-aware one can only SET the flag, so a
        // match here would grant the microphone before the argument was read.
        assertFalse("requestPermissions is decided by its argument",
                MacOSNativeBuilder.applicationOpensCameraMicrophone(
                        "com/example/MyForm",
                        "com/codename1/camera/Camera", "requestPermissions"));
        assertTrue("asking for audio still declares the microphone",
                MacOSNativeBuilder.requestsMicrophonePermission(
                        "com/example/MyForm", "com/codename1/camera/Camera",
                        "requestPermissions", Boolean.TRUE));

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

    /**
     * An application is free to call the review API without the
     * com.codename1.appreview facade, and both spellings are public API. The
     * package scan cannot see either, so missing them compiles the native
     * request out while the Java callback still reports success.
     */
    @Test
    public void theDirectReviewEntryPointsCount() {
        assertTrue(MacOSNativeBuilder.requestsNativeReview(
                "com/codename1/ui/CN", "requestNativeInAppReview"));
        assertTrue(MacOSNativeBuilder.requestsNativeReview(
                "com/codename1/ui/Display", "requestNativeInAppReview"));
    }

    /**
     * Matched on the exact name rather than a substring: an application method
     * of its own called requestNativeInAppReviewLater must not enable StoreKit,
     * and neither must an unrelated call on some other class.
     */
    @Test
    public void nothingElseCountsAsAReviewRequest() {
        assertFalse(MacOSNativeBuilder.requestsNativeReview(
                "com/codename1/ui/CN", "requestNativeInAppReviewLater"));
        assertFalse(MacOSNativeBuilder.requestsNativeReview(
                "com/example/MyApp", "requestNativeInAppReview"));
        assertFalse(MacOSNativeBuilder.requestsNativeReview(null, "requestNativeInAppReview"));
        assertFalse(MacOSNativeBuilder.requestsNativeReview("com/codename1/ui/CN", null));
    }

    /// Camera.requestPermissions is camera use, not only microphone use.
    ///
    /// It is the one call the documentation tells an application to make BEFORE
    /// touching a camera, and it was the one call that did not earn it
    /// NSCameraUsageDescription. MacCameraImpl answers it by asking AVFoundation
    /// for video authorization, and asking without that key in the bundle does
    /// not fail the request -- macOS terminates the process.
    @Test
    public void requestingCameraPermissionCountsAsUsingTheCamera() {
        assertTrue(MacOSNativeBuilder.applicationOpensCameraSession(
                "com/example/MyApp", "com/codename1/camera/Camera", "requestPermissions"));
        // The microphone follows the argument rather than the call, so an
        // unresolved value still declares it and an explicit false does not.
        assertTrue(MacOSNativeBuilder.requestsMicrophonePermission(
                "com/example/MyApp", "com/codename1/camera/Camera",
                "requestPermissions", null));
        assertFalse(MacOSNativeBuilder.requestsMicrophonePermission(
                "com/example/MyApp", "com/codename1/camera/Camera",
                "requestPermissions", Boolean.FALSE));
    }

    /// The framework's own callers are still excluded, which is the whole point
    /// of taking the caller: com.codename1.ai.vision calls these entry points in
    /// every build, so counting them would put a camera privacy string in every
    /// macOS application ever produced.
    @Test
    public void theFrameworksOwnPermissionRequestStillDoesNotCount() {
        assertFalse(MacOSNativeBuilder.applicationOpensCameraSession(
                "com/codename1/ai/vision/CodeScanner",
                "com/codename1/camera/Camera", "requestPermissions"));
    }

    /// Util's crypto delegates count, and all nine of them.
    ///
    /// They reach the same natives com.codename1.security does, and the package
    /// test cannot see them. secureRandomBytes is the one that matters most:
    /// its disabled native returns void and leaves the caller's buffer alone,
    /// so an application asking for random bytes with the suite compiled out
    /// gets the zeros it passed in and no error at all.
    @Test
    public void theUtilCryptoDelegatesCount() {
        String[] crypto = {
            "secureRandomBytes", "aesEncrypt", "aesDecrypt", "rsaEncrypt", "rsaDecrypt",
            "cryptoSign", "cryptoVerify", "generateRsaKeyPair", "generateSymmetricKey"
        };
        for (String m : crypto) {
            assertTrue("Util." + m + " reaches the crypto natives and must enable them",
                    MacOSNativeBuilder.usesUtilCrypto("com/codename1/io/Util", m));
        }
    }

    /// The listening entry points, which is what
    /// com.apple.security.network.server authorises.
    ///
    /// Regression: this used to be tested by looking for a
    /// com.codename1.io.ServerSocket class that does not exist in the
    /// repository, so the flag was never set and a sandboxed build of an
    /// application calling Socket.listen() had its bind denied.
    @Test
    public void listeningOnASocketNeedsTheServerEntitlement() {
        for (String m : new String[] {"listen", "listenLoopback"}) {
            assertTrue("Socket." + m + " listens and needs the server entitlement",
                    MacOSNativeBuilder.listensOnASocket("com/codename1/io/Socket", m));
        }
    }

    /// Dialling out is not listening: com.apple.security.network.client already
    /// covers it, and inbound authority an application never uses has to be
    /// justified at review.
    @Test
    public void dialingOutIsNotListening() {
        for (String m : new String[] {"connect", "getInputStream", "isSupported", "disconnect"}) {
            assertFalse("Socket." + m + " does not listen",
                    MacOSNativeBuilder.listensOnASocket("com/codename1/io/Socket", m));
        }
        assertFalse(MacOSNativeBuilder.listensOnASocket("com/example/MySocket", "listen"));
        assertFalse(MacOSNativeBuilder.listensOnASocket(null, "listen"));
        assertFalse(MacOSNativeBuilder.listensOnASocket("com/codename1/io/Socket", null));
    }

    /// Util is a big class of unrelated helpers, so the match is by exact name:
    /// copying a stream or reading a UTF string must not drag CommonCrypto into
    /// an application that never asked for it.
    @Test
    public void theRestOfUtilDoesNotCount() {
        for (String m : new String[] {"copy", "readToString", "cleanup", "encodeUrl", "sleep"}) {
            assertFalse("Util." + m + " is not crypto",
                    MacOSNativeBuilder.usesUtilCrypto("com/codename1/io/Util", m));
        }
        assertFalse(MacOSNativeBuilder.usesUtilCrypto("com/example/MyUtil", "secureRandomBytes"));
        assertFalse(MacOSNativeBuilder.usesUtilCrypto(null, "secureRandomBytes"));
        assertFalse(MacOSNativeBuilder.usesUtilCrypto("com/codename1/io/Util", null));
    }

    /// Cancelling a notification is using notifications.
    ///
    /// An update whose only notification code withdraws one scheduled by an
    /// earlier version names nothing in com.codename1.notifications, and with
    /// the define off the cancel compiles away and the notification still
    /// fires.
    @Test
    public void cancellingALocalNotificationCounts() {
        assertTrue(MacOSNativeBuilder.usesNotifications(
                "com/codename1/ui/Display", "cancelLocalNotification"));
        assertTrue(MacOSNativeBuilder.usesNotifications(
                "com/codename1/ui/Display", "requestNotificationPermission"));
        assertFalse(MacOSNativeBuilder.usesNotifications(
                "com/codename1/ui/Display", "setProperty"));
    }

    /// A vision application gets the camera and NOT the microphone.
    ///
    /// VisionCameraView.start() opens its session with captureAudio(false) and
    /// CodeScanner delegates to that view, so every session these classes open
    /// is silent. Declaring the microphone anyway is a privacy string and an
    /// audio-input entitlement the application has to justify at review for a
    /// device it never opens.
    @Test
    public void aVisionApplicationIsNotAMicrophoneUser() {
        assertTrue("a scanner application still needs the camera",
                MacOSNativeBuilder.applicationUsesCameraBackedVision(
                        "com/example/MyApp", "com/codename1/ai/vision/CodeScanner"));
        // The microphone predicate is about the low level Camera entry points,
        // and naming a vision class is not one of them.
        assertFalse("naming a vision class must not take the microphone",
                MacOSNativeBuilder.applicationOpensCameraMicrophone(
                        "com/example/MyApp", "com/codename1/ai/vision/CodeScanner", "start"));
    }

    /// The generalised matcher finds any setting, still bounded to its own line.
    ///
    /// ARCHS and ENABLE_HARDENED_RUNTIME reach the project only through this,
    /// because their xcodebuild overrides live in buildChannel() and mac-source
    /// returns before it runs.
    @Test
    public void anySettingAssignmentIsMatchedAndBounded() {
        // VALID_ARCHS is in the sample deliberately: "ARCHS" is a suffix of it,
        // and without a leading word boundary the matcher rewrote that line too.
        String body = "\t\t\t\tARCHS = \"$(ARCHS_STANDARD)\";\n"
                + "\t\t\t\tVALID_ARCHS = \"$(ARCHS_STANDARD)\";\n"
                + "\t\t\t\tENABLE_HARDENED_RUNTIME = YES;\n"
                + "\t\t\t\tSDKROOT = macosx;\n";
        java.util.Set<String> archs = MacOSNativeBuilder.settingAssignments(body, "ARCHS");
        assertEquals("VALID_ARCHS must not be matched as ARCHS", 1, archs.size());
        assertEquals("ARCHS = \"$(ARCHS_STANDARD)\";", archs.iterator().next());
        // And asking for VALID_ARCHS still finds it.
        assertEquals(1, MacOSNativeBuilder.settingAssignments(body, "VALID_ARCHS").size());

        java.util.Set<String> hardened =
                MacOSNativeBuilder.settingAssignments(body, "ENABLE_HARDENED_RUNTIME");
        assertEquals(1, hardened.size());
        assertEquals("ENABLE_HARDENED_RUNTIME = YES;", hardened.iterator().next());
    }

    /// A setting the template no longer carries yields nothing, which is what
    /// makes the builder report it instead of claiming success.
    @Test
    public void anAbsentSettingYieldsNothing() {
        assertTrue(MacOSNativeBuilder.settingAssignments("SDKROOT = macosx;\n", "ARCHS").isEmpty());
        assertTrue(MacOSNativeBuilder.settingAssignments(null, "ARCHS").isEmpty());
        assertTrue(MacOSNativeBuilder.settingAssignments("ARCHS = arm64;", null).isEmpty());
    }

    /// The deployment-target helper still behaves, since it now delegates.
    @Test
    public void theDeploymentTargetHelperStillMatches() {
        java.util.Set<String> found = MacOSNativeBuilder.deploymentTargetAssignments(
                "\t\t\t\tMACOSX_DEPLOYMENT_TARGET = 13.0;\n\t\t\t\tSDKROOT = macosx;\n");
        assertEquals(1, found.size());
        assertEquals("MACOSX_DEPLOYMENT_TARGET = 13.0;", found.iterator().next());
    }

    /// The DEFAULT arch is what broke the build, so it is what this pins.
    ///
    /// getArch() answers "arm64 x86_64" when no hint is set, and a value with a
    /// space written bare into a pbxproj makes the old-style plist parser
    /// abandon the file -- "missing semicolon in dictionary" -- which failed
    /// every macOS build. The value under test before was x86_64, a single
    /// token that is legal bare, so the path every unhinted build takes was
    /// never exercised.
    @Test
    public void aPbxprojValueIsQuotedWhenTheFormatRequiresIt() {
        assertEquals("\"arm64 x86_64\"", MacOSNativeBuilder.quotePbxprojValue("arm64 x86_64"));
        assertEquals("\"$(ARCHS_STANDARD)\"", MacOSNativeBuilder.quotePbxprojValue("$(ARCHS_STANDARD)"));
        // Bare identifiers stay bare, so the file keeps the shape Xcode wrote.
        assertEquals("x86_64", MacOSNativeBuilder.quotePbxprojValue("x86_64"));
        assertEquals("NO", MacOSNativeBuilder.quotePbxprojValue("NO"));
        assertEquals("13.0", MacOSNativeBuilder.quotePbxprojValue("13.0"));
        // An already-quoted value is not quoted twice.
        assertEquals("\"arm64 x86_64\"", MacOSNativeBuilder.quotePbxprojValue("\"arm64 x86_64\""));
        assertNull(MacOSNativeBuilder.quotePbxprojValue(null));
    }

    /// Enumerating cameras is not opening a microphone.
    ///
    /// getCameras() and getDefault() run a discovery session and nothing else,
    /// so a bundle that only enumerated devices was carrying
    /// NSMicrophoneUsageDescription and the audio-input entitlement.
    @Test
    public void enumeratingCamerasDoesNotTakeTheMicrophone() {
        String app = "com/example/MyApp";
        String cam = "com/codename1/camera/Camera";
        assertFalse(MacOSNativeBuilder.applicationOpensCameraMicrophone(app, cam, "getCameras"));
        assertFalse(MacOSNativeBuilder.applicationOpensCameraMicrophone(app, cam, "getDefault"));
        // open() still does: CameraSessionOptions.captureAudio starts true.
        assertTrue(MacOSNativeBuilder.applicationOpensCameraMicrophone(app, cam, "open"));
        // requestPermissions must NOT match here. Executor calls this hook and
        // the boolean-aware one for the same call, and the latter can only set
        // the flag -- so matching here granted the microphone before the
        // argument was read, and requestPermissions(false, cb) still declared
        // it.
        assertFalse("requestPermissions is decided by its argument alone",
                MacOSNativeBuilder.applicationOpensCameraMicrophone(app, cam, "requestPermissions"));
        // And they are still CAMERA users, which is a separate question.
        assertTrue(MacOSNativeBuilder.applicationOpensCameraSession(app, cam, "getCameras"));
    }

    /// requestPermissions is decided by its argument, and an unreadable one
    /// counts as asking.
    @Test
    public void theMicrophonePermissionFollowsItsArgument() {
        String app = "com/example/MyApp";
        String cam = "com/codename1/camera/Camera";
        assertFalse("requestPermissions(false, cb) asks only for video",
                MacOSNativeBuilder.requestsMicrophonePermission(app, cam, "requestPermissions",
                        Boolean.FALSE));
        assertTrue(MacOSNativeBuilder.requestsMicrophonePermission(app, cam, "requestPermissions",
                Boolean.TRUE));
        assertTrue("an unresolved argument must count as asking",
                MacOSNativeBuilder.requestsMicrophonePermission(app, cam, "requestPermissions",
                        null));
        // Other methods are not this predicate's business.
        assertFalse(MacOSNativeBuilder.requestsMicrophonePermission(app, cam, "open", null));
    }
}
