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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The class-scan rules that decide what the macOS build compiles and what its
 * signature asks for.
 */
public class MacOSNativeBuilderScanTest {

    /**
     * One rule serves two decisions -- the microphone entitlement and whether
     * the recorder is compiled at all -- so it is worth pinning on its own.
     * Written twice they would drift, and the drift ships an application
     * holding an entitlement with no recorder behind it.
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
}
