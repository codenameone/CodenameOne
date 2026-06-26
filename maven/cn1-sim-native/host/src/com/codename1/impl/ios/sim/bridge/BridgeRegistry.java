/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.impl.ios.sim.bridge;

/**
 * The rendezvous point between the parent universe (which owns the native
 * bindings, the window and the skin) and the child universe (the isolated
 * app). This class is loaded exactly once - by the parent - and the child
 * loader delegates this package upward, so statics here are shared across
 * both universes.
 */
public final class BridgeRegistry {
    private static volatile RenderBridge bridge;
    private static volatile InputSink inputSink;

    private BridgeRegistry() {
    }

    /**
     * Installed by the parent before the child universe boots.
     */
    public static void setBridge(RenderBridge b) {
        bridge = b;
    }

    /**
     * Read by the child universe's implementation on startup.
     */
    public static RenderBridge getBridge() {
        return bridge;
    }

    /**
     * Installed by the child universe; the parent routes window input here
     * with app-local coordinates.
     */
    public static void setInputSink(InputSink sink) {
        inputSink = sink;
    }

    public static InputSink getInputSink() {
        return inputSink;
    }

    private static volatile ToolsBridge toolsBridge;

    /**
     * Installed by the shell universe; the child implementation reports tool
     * data (network activity etc.) here.
     */
    public static void setToolsBridge(ToolsBridge t) {
        toolsBridge = t;
    }

    public static ToolsBridge getToolsBridge() {
        return toolsBridge;
    }

    private static volatile RenderBridge shellBridge;

    /**
     * The shell universe's full-window bridge - swapped when the window
     * resizes (rotation, skin change).
     */
    public static void setShellBridge(RenderBridge b) {
        shellBridge = b;
    }

    public static RenderBridge getShellBridge() {
        return shellBridge;
    }

    private static volatile MenuDispatcher menuDispatcher;

    /**
     * Registered by whichever universe pushed the current native menu.
     */
    public static void setMenuDispatcher(MenuDispatcher d) {
        menuDispatcher = d;
    }

    public static MenuDispatcher getMenuDispatcher() {
        return menuDispatcher;
    }

    /* ---- simulated conditions shared across universes ----------------------- */

    public static final int NETWORK_REGULAR = 0;
    public static final int NETWORK_SLOW = 1;
    public static final int NETWORK_DISCONNECTED = 2;

    private static volatile int networkCondition = NETWORK_REGULAR;

    /**
     * Set by the shell's Network menu; honored by the app universe's
     * networking implementation.
     */
    public static void setNetworkCondition(int condition) {
        networkCondition = condition;
    }

    public static int getNetworkCondition() {
        return networkCondition;
    }

    private static volatile double simulatedLatitude = 40.714353;
    private static volatile double simulatedLongitude = -74.005973;

    /**
     * Set by the shell's location tool; read by the app universe's location
     * manager.
     */
    public static void setSimulatedLocation(double latitude, double longitude) {
        simulatedLatitude = latitude;
        simulatedLongitude = longitude;
    }

    public static double getSimulatedLatitude() {
        return simulatedLatitude;
    }

    public static double getSimulatedLongitude() {
        return simulatedLongitude;
    }

    private static volatile String appArg;

    /**
     * Set by the shell's "Send App Argument" tool; surfaces in the app
     * universe as the AppArg display property.
     */
    public static void setAppArg(String arg) {
        appArg = arg;
    }

    public static String getAppArg() {
        return appArg;
    }

    private static volatile ChildControl childControl;

    /**
     * Registered by the app universe's implementation; the shell's tool menu
     * sends control commands (Debug EDT, Clean Storage...) through it.
     */
    public static void setChildControl(ChildControl c) {
        childControl = c;
    }

    public static ChildControl getChildControl() {
        return childControl;
    }

    /* ---- skin metadata shared with the app universe ------------------------- */

    private static volatile String skinPlatformName = "ios";
    private static volatile byte[] embeddedThemeRes;
    private static volatile int[] safeAreaPortrait;
    private static volatile int[] safeAreaLandscape;
    private static volatile int unscaledScreenWidth;
    private static volatile int unscaledScreenHeight;
    private static volatile String nativeThemePref = "auto";

    /**
     * Published by the skin loader: the platform the skin simulates, the
     * skin's embedded native theme resource and the safe areas (screen
     * coordinates of the UNSCALED portrait screen; the app universe scales
     * by its own display size).
     */
    public static void setSkinInfo(String platformName, byte[] themeRes,
            int[] safePortrait, int[] safeLandscape, int screenW, int screenH) {
        skinPlatformName = platformName;
        embeddedThemeRes = themeRes;
        safeAreaPortrait = safePortrait;
        safeAreaLandscape = safeLandscape;
        unscaledScreenWidth = screenW;
        unscaledScreenHeight = screenH;
    }

    public static String getSkinPlatformName() {
        return skinPlatformName;
    }

    public static byte[] getEmbeddedThemeRes() {
        return embeddedThemeRes;
    }

    /** @return {x, y, w, h} in unscaled portrait-screen coordinates, or null */
    public static int[] getSafeAreaPortrait() {
        return safeAreaPortrait;
    }

    /** @return {x, y, w, h} in unscaled landscape-screen coordinates, or null */
    public static int[] getSafeAreaLandscape() {
        return safeAreaLandscape;
    }

    public static int getUnscaledScreenWidth() {
        return unscaledScreenWidth;
    }

    public static int getUnscaledScreenHeight() {
        return unscaledScreenHeight;
    }

    /**
     * The Native Theme menu selection: "auto", "embedded" or a bundled theme
     * resource name such as iOSModernTheme.
     */
    public static void setNativeThemePref(String pref) {
        nativeThemePref = pref;
    }

    public static String getNativeThemePref() {
        return nativeThemePref;
    }

    // the Test Recorder, registered by the input router
    private static volatile RecorderControl recorderControl;

    public static void setRecorderControl(RecorderControl r) {
        recorderControl = r;
    }

    public static RecorderControl getRecorderControl() {
        return recorderControl;
    }

    // accessibility text-scale multiplier (Simulate > Larger Text); the app
    // universe's impl returns it from getLargerTextScale() so core scales fonts
    private static volatile float largerTextScale = 1.0f;

    public static void setLargerTextScale(float scale) {
        largerTextScale = scale;
    }

    public static float getLargerTextScale() {
        return largerTextScale;
    }

    // Simulate > Biometric Simulation state (read by the app universe's
    // simulated Biometrics; set directly by the shell menu since BridgeRegistry
    // is shared across universes). Outcome is a SimOutcome enum name.
    private static volatile boolean biometricAvailable = false;
    private static volatile boolean biometricFaceEnrolled = false;
    private static volatile boolean biometricTouchEnrolled = false;
    private static volatile boolean biometricIrisEnrolled = false;
    private static volatile String biometricOutcome = "SUCCEED";

    public static void setBiometricAvailable(boolean v) { biometricAvailable = v; }
    public static boolean isBiometricAvailable() { return biometricAvailable; }
    public static void setBiometricFaceEnrolled(boolean v) { biometricFaceEnrolled = v; }
    public static boolean isBiometricFaceEnrolled() { return biometricFaceEnrolled; }
    public static void setBiometricTouchEnrolled(boolean v) { biometricTouchEnrolled = v; }
    public static boolean isBiometricTouchEnrolled() { return biometricTouchEnrolled; }
    public static void setBiometricIrisEnrolled(boolean v) { biometricIrisEnrolled = v; }
    public static boolean isBiometricIrisEnrolled() { return biometricIrisEnrolled; }
    public static void setBiometricOutcome(String v) { biometricOutcome = v; }
    public static String getBiometricOutcome() { return biometricOutcome; }

    // Simulate > In App Purchase capabilities (read by the simulated Purchase)
    private static volatile boolean iapManualSupported = false;
    private static volatile boolean iapManagedSupported = false;
    private static volatile boolean iapSubscriptionSupported = false;
    private static volatile boolean iapRefundSupported = false;

    public static void setIapManualSupported(boolean v) { iapManualSupported = v; }
    public static boolean isIapManualSupported() { return iapManualSupported; }
    public static void setIapManagedSupported(boolean v) { iapManagedSupported = v; }
    public static boolean isIapManagedSupported() { return iapManagedSupported; }
    public static void setIapSubscriptionSupported(boolean v) { iapSubscriptionSupported = v; }
    public static boolean isIapSubscriptionSupported() { return iapSubscriptionSupported; }
    public static void setIapRefundSupported(boolean v) { iapRefundSupported = v; }
    public static boolean isIapRefundSupported() { return iapRefundSupported; }

    // dark-mode override for the Simulate menu: "auto", "dark" or "light"
    private static volatile String darkMode = "auto";

    public static void setDarkMode(String mode) {
        darkMode = mode;
    }

    public static String getDarkMode() {
        return darkMode;
    }

    private static volatile int fontSizeSmall;
    private static volatile int fontSizeMedium;
    private static volatile int fontSizeLarge;
    private static volatile int skinPpi;
    private static volatile double renderScale = 1.0;

    /**
     * The skin's font metrics in UNSCALED device pixels plus its pixel
     * density; the app universe multiplies by the render scale.
     */
    public static void setFontMetrics(int small, int medium, int large, int ppi) {
        fontSizeSmall = small;
        fontSizeMedium = medium;
        fontSizeLarge = large;
        skinPpi = ppi;
    }

    public static int getFontSizeSmall() {
        return fontSizeSmall;
    }

    public static int getFontSizeMedium() {
        return fontSizeMedium;
    }

    public static int getFontSizeLarge() {
        return fontSizeLarge;
    }

    public static int getSkinPpi() {
        return skinPpi;
    }

    /**
     * The factor the app's screen is scaled by relative to the device's real
     * resolution - published by the launcher whenever the skin is (re)applied.
     */
    public static void setRenderScale(double scale) {
        renderScale = scale;
    }

    public static double getRenderScale() {
        return renderScale;
    }

    private static volatile EditingCallback editingCallback;

    /**
     * Registered by the universe starting a native text-editing session;
     * consumed (cleared) when the session commits.
     */
    public static void setEditingCallback(EditingCallback cb) {
        editingCallback = cb;
    }

    public static EditingCallback takeEditingCallback() {
        EditingCallback cb = editingCallback;
        editingCallback = null;
        return cb;
    }

    /** Non-consuming peek for mid-session text updates. */
    public static EditingCallback getEditingCallback() {
        return editingCallback;
    }

    private static volatile PickCallback pickCallback;

    /** Registered before opening the native file panel; consumed on pick. */
    public static void setPickCallback(PickCallback cb) {
        pickCallback = cb;
    }

    public static PickCallback takePickCallback() {
        PickCallback cb = pickCallback;
        pickCallback = null;
        return cb;
    }

    private static volatile boolean appPaused;

    /**
     * Pause App: while set the app universe's flushes are dropped and the
     * shell paints a paused overlay over the screen region.
     */
    public static void setAppPaused(boolean paused) {
        appPaused = paused;
    }

    public static boolean isAppPaused() {
        return appPaused;
    }
}
