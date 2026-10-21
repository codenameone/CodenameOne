/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.security;

import com.codename1.security.DeviceIntegrity;
import com.codename1.security.SecureStorage;
import com.codename1.security.TapjackingPolicy;
import com.codename1.security.hardening.Hardening;
import com.codename1.security.shield.AppShield;
import com.codename1.security.shield.PinSet;
import com.codename1.security.vault.Protection;
import com.codename1.security.vault.ProtectionReport;
import com.codename1.ui.CN;
import com.codenameone.examples.bastion.AppConfig;
import com.codenameone.examples.bastion.net.Guard;
import com.codenameone.examples.bastion.net.Session;

import java.util.ArrayList;
import java.util.List;

/// What the Security Center lists: every protection the app has, and whether
/// it is really in force on this device, in this build, right now.
///
/// Honesty is the point. A protection that only a cloud build applies (code
/// hardening, attestation) says so instead of claiming to be on; one the
/// simulator cannot provide (a hardware keystore, secure windows) says that;
/// a development exemption in use is a warning, in red.
public final class SecurityStatus {
    /// In force.
    public static final int ACTIVE = 0;
    /// Not in force on this device: the simulator or desktop cannot provide it.
    public static final int SIMULATOR = 1;
    /// Applied only by a build from the Codename One build server (or with an
    /// entitlement that build carries).
    public static final int CLOUD_BUILD = 2;
    /// Off, or weakened, and the user should know.
    public static final int WARNING = 3;
    /// Not set up yet (no device key registered, say).
    public static final int OFF = 4;

    /// The hardening build hints declared in `codenameone_settings.properties`,
    /// as `name=value`. The app cannot read its own build hints at run time,
    /// so they are mirrored here; `SecurityStatusTest` fails when the two
    /// disagree.
    public static final String[] DECLARED_HINTS = {
        "cn1.hardenLevel=aggressive",
        "ios.detectJailbreak=true",
        "ios.blockScreenshotsOnEnterBackground=true",
        "android.playIntegrity=true",
    };

    /// One line of the Security Center.
    public static final class Item {
        public final String group;
        public final String name;
        public final int state;
        public final String detail;

        Item(String group, String name, int state, String detail) {
            this.group = group;
            this.name = name;
            this.state = state;
            this.detail = detail;
        }
    }

    private SecurityStatus() {
    }

    public static List<Item> items() {
        List<Item> out = new ArrayList<Item>();
        boolean sim = CN.isSimulator() || CN.isDesktop();
        boolean android = "and".equals(CN.getPlatformName());
        boolean ios = "ios".equals(CN.getPlatformName());
        String server = Session.serverUrl() == null ? AppConfig.serverUrl() : Session.serverUrl();
        boolean dev = AppConfig.isDevelopmentServer(server);
        Guard guard = Guard.get();

        // ------------------------------------------------------------ network
        String g = "Network";
        out.add(dev
                ? new Item(g, "No cleartext", WARNING, "Development server " + server
                        + " is exempt: plain HTTP to this machine only")
                : new Item(g, "No cleartext", ACTIVE, "Every request is https; http is refused"
                        + refusals(guard)));
        out.add(dev
                ? new Item(g, "Host allow-list", WARNING, "Exempt while the development server is in use")
                : new Item(g, "Host allow-list", ACTIVE, "Only " + join(AppConfig.ALLOWED_HOSTS)));
        out.add(pins(g, guard, dev));
        PinSet ota = AppShield.getPinSet();
        out.add(ota.isEmpty()
                ? new Item(g, "Over-the-air pins", CLOUD_BUILD,
                        "Published by the attestation service; none in this build")
                : new Item(g, "Over-the-air pins", ACTIVE, ota.hostCount() + " hosts, version "
                        + ota.getVersion() + (ota.isStale() ? " (refresh due)" : "")));
        out.add(AppShield.isProtected()
                ? new Item(g, "App attestation token", ACTIVE, "AppShield engine "
                        + AppShield.getEngineName() + ": " + AppShield.getStatus())
                : new Item(g, "App attestation token", CLOUD_BUILD,
                        "AppShield needs the enterprise engine in a cloud build; inert here"));

        // ---------------------------------------------------------- the keys
        g = "Keys and sign-in";
        out.add(storage(g, sim));
        out.add(DeviceKey.deviceId() != null
                ? new Item(g, "Device signing key", ACTIVE, "P-256, registered as "
                        + DeviceKey.deviceId() + "; signs every payment")
                : new Item(g, "Device signing key", OFF,
                        "Not registered: payments that need a signature are refused"));
        out.add(new Item(g, "Idempotency keys", ACTIVE,
                "Each money action carries one, so a retry never pays twice"));
        out.add(new Item(g, "Second factor and passkeys", ACTIVE, Session.passkeysSupported()
                ? "Authenticator codes and passkeys" : "Authenticator codes (no passkeys on this platform)"));

        // ------------------------------------------------------ app and device
        g = "This device";
        out.add(Lock.biometricsAvailable()
                ? new Item(g, "Biometric lock", ACTIVE, "On start, on return and when idle")
                : new Item(g, "Biometric lock", sim ? SIMULATOR : WARNING,
                        "No biometrics enrolled: the lock asks for the password instead"));
        out.add(new Item(g, "Auto-lock", ACTIVE, "After " + (AppConfig.IDLE_LOCK_MILLIS / 1000)
                + "s idle or " + (AppConfig.BACKGROUND_LOCK_MILLIS / 1000) + "s in the background"));
        out.add(integrity(g, sim));
        out.add(DeviceIntegrity.isAttestationSupported()
                ? new Item(g, "Platform attestation", ACTIVE,
                        android ? "Play Integrity" : "App Attest")
                : new Item(g, "Platform attestation", CLOUD_BUILD,
                        "Play Integrity / App Attest need a device build"));
        out.add(DeviceIntegrity.hasUntrustedAccessibilityService()
                ? new Item(g, "Accessibility services", WARNING, "Can read the screen: "
                        + join(DeviceIntegrity.getEnabledAccessibilityServices()))
                : new Item(g, "Accessibility services", ACTIVE, "None that can read the screen"));
        out.add(android && DeviceIntegrity.getTapjackingPolicy() == TapjackingPolicy.BLOCK
                ? new Item(g, "Tapjacking", ACTIVE, "Touches through overlays are dropped")
                : new Item(g, "Tapjacking", SIMULATOR, "Policy set; only Android has overlays"));
        out.add(sim
                ? new Item(g, "Screenshot blocking", SIMULATOR,
                        "Set on sensitive screens; the simulator cannot block captures")
                : new Item(g, "Screenshot blocking", ACTIVE, ios
                        ? "Sensitive screens, and the app switcher card"
                        : "Sensitive screens and the recent-apps card"));
        out.add(new Item(g, "Clipboard clearing", ACTIVE, "Copied numbers are wiped after "
                + (AppConfig.CLIPBOARD_CLEAR_MILLIS / 1000) + "s"));

        // ------------------------------------------------------------- build
        g = "Build hardening";
        out.add(Hardening.isHardened()
                ? new Item(g, "Code hardening", ACTIVE, "Level " + Hardening.getLevel()
                        + ", mapping " + Hardening.getMappingId())
                : new Item(g, "Code hardening", CLOUD_BUILD,
                        "Declared " + hint("cn1.hardenLevel") + "; applied by the build server"));
        out.add(new Item(g, "Jailbreak exit (iOS)", ios && !sim ? ACTIVE : CLOUD_BUILD,
                "ios.detectJailbreak=" + hint("ios.detectJailbreak")));
        out.add(new Item(g, "App switcher blanking (iOS)", ios && !sim ? ACTIVE : CLOUD_BUILD,
                "ios.blockScreenshotsOnEnterBackground="
                        + hint("ios.blockScreenshotsOnEnterBackground")));
        out.add(new Item(g, "Play Integrity (Android)", android && !sim ? ACTIVE : CLOUD_BUILD,
                "android.playIntegrity=" + hint("android.playIntegrity")));
        return out;
    }

    /// The declared value of build hint `name`, or "unset".
    public static String hint(String name) {
        for (String line : DECLARED_HINTS) {
            if (line.startsWith(name + "=")) {
                return line.substring(name.length() + 1);
            }
        }
        return "unset";
    }

    private static Item pins(String g, Guard guard, boolean dev) {
        boolean placeholder = false;
        for (String[] row : AppConfig.PINS) {
            for (int iter = 1; iter < row.length; iter++) {
                placeholder |= row[iter].startsWith("REPLACE");
            }
        }
        if (dev) {
            return new Item(g, "Certificate pinning", WARNING,
                    "Not applied to the development server (it has no certificate)");
        }
        if (placeholder) {
            return new Item(g, "Certificate pinning", WARNING,
                    "Placeholder pins: put the server's SPKI digests in AppConfig.PINS");
        }
        return new Item(g, "Certificate pinning", ACTIVE, AppConfig.PINS.length + " hosts pinned"
                + (guard == null || guard.pinFailureCount() == 0 ? ""
                        : ", " + guard.pinFailureCount() + " mismatches refused"));
    }

    private static Item storage(String g, boolean sim) {
        ProtectionReport report = SecureStorage.getInstance().protection();
        if (report.provides(Protection.HARDWARE_BACKED)) {
            return new Item(g, "Token and key storage", ACTIVE, "Hardware-backed keystore");
        }
        if (report.provides(Protection.OS_PROTECTED)) {
            return new Item(g, "Token and key storage", ACTIVE, "OS keystore");
        }
        return new Item(g, "Token and key storage", sim ? SIMULATOR : WARNING, sim
                ? "The simulator keeps them in a plain file"
                : "No keystore on this device: tokens are not kept between runs");
    }

    private static Item integrity(String g, boolean sim) {
        if (sim) {
            return new Item(g, "Root and jailbreak checks", SIMULATOR,
                    "Run on a phone; the simulator reports nothing");
        }
        if (DeviceIntegrity.isDeviceCompromised()) {
            return new Item(g, "Root and jailbreak checks", WARNING, "This device looks modified: "
                    + join(DeviceIntegrity.getCompromiseReasons())
                    + ". The bank will not move money from it.");
        }
        return new Item(g, "Root and jailbreak checks", ACTIVE, "No signs of root or jailbreak");
    }

    private static String refusals(Guard guard) {
        int n = guard == null ? 0 : guard.refusedCount();
        return n == 0 ? "" : " (" + n + " refused)";
    }

    private static String join(String[] values) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; values != null && iter < values.length; iter++) {
            if (iter > 0) {
                out.append(", ");
            }
            out.append(values[iter]);
        }
        return out.toString();
    }
}
