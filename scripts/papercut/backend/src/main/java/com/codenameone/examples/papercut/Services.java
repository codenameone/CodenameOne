/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;
import com.codename1.appkit.server.push.CloudPushSender;
import com.codename1.appkit.server.push.ConsolePushSender;
import com.codename1.appkit.server.push.MisconfiguredPushSender;
import com.codename1.appkit.server.push.PushSender;
import com.codename1.appkit.server.push.RecordingPushSender;
import com.codenameone.examples.papercut.shop.MisconfiguredReceipts;
import com.codenameone.examples.papercut.shop.ReceiptValidator;
import com.codenameone.examples.papercut.shop.SimulatedReceipts;
import com.codenameone.examples.papercut.shop.StoreReceipts;

/// The services this server reaches outside itself for, each behind an
/// interface and each chosen here from the settings. A development profile
/// (dev, test, local) gets the stand-in that needs no account; any other
/// profile gets the real one, and when its settings are missing the server
/// still starts and fails every call with a message naming them. It never
/// quietly falls back to a stand-in.
///
/// Every setting is also read from the environment, in upper case with `_`
/// for each `.` (`PAPERCUT_IAP_APPLE_SECRET`), which is where secrets belong.
@Configuration
public class Services {
    // tag::papercut-receipt-choice[]
    /// `papercut.iap.provider` is one of:
    ///
    /// - `store`: receipts are checked with Apple (`papercut.iap.apple.secret`)
    ///   and Google Play (`papercut.iap.google.package` and
    ///   `papercut.iap.google.token`). A store with no settings refuses its
    ///   receipts; the other keeps working.
    /// - `simulated`: every receipt is accepted, on a development profile only.
    ///
    /// Unset, it is `simulated` on a development profile and `store` otherwise.
    @Bean
    public ReceiptValidator receipts(Config config,
            @Value("${papercut.iap.provider:}") String chosen,
            @Value("${papercut.iap.apple.secret:}") String appleSecret,
            @Value("${papercut.iap.apple.bundle:}") String appleBundle,
            @Value("${papercut.iap.google.package:}") String googlePackage,
            @Value("${papercut.iap.google.token:}") String googleToken) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "simulated" : "store";
        if ("simulated".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredReceipts("papercut.iap.provider=simulated grants every "
                        + "purchase for free and is refused on the '" + config.getProfile()
                        + "' profile; set it to store");
            }
            return new SimulatedReceipts();
        }
        if (!"store".equalsIgnoreCase(kind)) {
            return new MisconfiguredReceipts("papercut.iap.provider is '" + kind
                    + "', which is neither store nor simulated");
        }
        if (appleSecret.trim().length() == 0 && (googlePackage.trim().length() == 0
                || googleToken.trim().length() == 0)) {
            return new MisconfiguredReceipts("in-app purchases are not configured; set "
                    + "papercut.iap.apple.secret, or papercut.iap.google.package and "
                    + "papercut.iap.google.token");
        }
        if (appleSecret.trim().length() > 0 && appleBundle.trim().length() == 0) {
            // Without it, a genuine receipt from any other app could buy coins.
            return new MisconfiguredReceipts("papercut.iap.apple.secret is set without "
                    + "papercut.iap.apple.bundle, the bundle id receipts must belong to");
        }
        return new StoreReceipts(appleSecret.trim(), appleBundle.trim(), googlePackage.trim(), googleToken.trim());
    }
    // end::papercut-receipt-choice[]

    // tag::papercut-push-choice[]
    /// `papercut.push.provider` is one of:
    ///
    /// - `cloud`: the Codename One push service. It needs `papercut.push.app`,
    ///   the app's push id, and `papercut.push.key`, a server key with the push
    ///   scope; both are in the Push section of the Codename One console.
    /// - `console`: prints each notification, on a development profile only.
    /// - `recording`: keeps each notification for the tests to read, on a
    ///   development profile only.
    ///
    /// Unset, it is `recording` on the `test` profile, `console` on the other
    /// development profiles and `cloud` otherwise. A server whose push settings
    /// are missing still starts and still serves deliveries: it logs a configuration
    /// error at start-up and again for every notification it could not send.
    @Bean
    public PushSender pushSender(Config config,
            @Value("${papercut.push.provider:}") String chosen,
            @Value("${papercut.push.app:}") String app,
            @Value("${papercut.push.key:}") String key,
            @Value("${papercut.push.url:}") String url) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "recording"
                : config.isDevelopmentProfile() ? "console" : "cloud";
        if ("console".equalsIgnoreCase(kind) || "recording".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredPushSender("papercut.push.provider=" + kind
                        + " sends nothing and is refused on the '" + config.getProfile()
                        + "' profile; set it to cloud");
            }
            return "console".equalsIgnoreCase(kind) ? new ConsolePushSender()
                    : new RecordingPushSender();
        }
        if (!"cloud".equalsIgnoreCase(kind)) {
            return new MisconfiguredPushSender("papercut.push.provider is '" + kind
                    + "', which is none of cloud, console or recording");
        }
        String missing = missing("papercut.push.", new String[] {"app", app, "key", key});
        if (missing.length() > 0) {
            return new MisconfiguredPushSender(unset("push", "cloud", missing));
        }
        try {
            CloudPush push = new CloudPush(app.trim(), key.trim());
            return new CloudPushSender(url.trim().length() > 0 ? push.withEndpoint(url.trim())
                    : push);
        } catch (IllegalArgumentException err) {
            return new MisconfiguredPushSender("push provider 'cloud' is misconfigured: "
                    + err.getMessage());
        }
    }
    // end::papercut-push-choice[]

    /// The names, after `prefix`, of the settings in `namesAndValues` whose
    /// value is empty, separated by commas.
    private static String missing(String prefix, String[] namesAndValues) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter + 1 < namesAndValues.length; iter += 2) {
            if (namesAndValues[iter + 1] == null || namesAndValues[iter + 1].trim().length() == 0) {
                if (out.length() > 0) {
                    out.append(", ");
                }
                out.append(prefix).append(namesAndValues[iter]);
            }
        }
        return out.toString();
    }

    private static String unset(String service, String provider, String missing) {
        return service + " provider '" + provider + "' is not configured; set " + missing;
    }
}
