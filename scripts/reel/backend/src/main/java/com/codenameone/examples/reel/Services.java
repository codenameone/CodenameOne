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
package com.codenameone.examples.reel;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;
import com.codenameone.examples.reel.moderation.ContentClassifier;
import com.codenameone.examples.reel.pay.MisconfiguredPayments;
import com.codenameone.examples.reel.playback.HttpLicenseProxy;
import com.codenameone.examples.reel.playback.LicenseProvider;
import com.codenameone.examples.reel.playback.Licenses;
import com.codenameone.examples.reel.repository.VideoKeyRepository;
import com.codenameone.examples.reel.settings.Settings;
import com.codenameone.examples.reel.pay.PaymentProvider;
import com.codenameone.examples.reel.pay.SimulatedPayments;
import com.codenameone.examples.reel.pay.StripePayments;
import com.codenameone.examples.reel.push.CloudPushSender;
import com.codenameone.examples.reel.push.ConsolePushSender;
import com.codenameone.examples.reel.push.MisconfiguredPushSender;
import com.codenameone.examples.reel.push.PushSender;
import com.codenameone.examples.reel.push.RecordingPushSender;

import java.util.ArrayList;
import java.util.List;

/// The services this server reaches outside itself for, each behind an
/// interface and each chosen here from the settings.
///
/// Each is named by one setting -- `reel.payments.provider` and
/// `reel.push.provider` -- and, when that is not set, a development profile
/// (dev, test, local) gets the one that needs no account: notifications
/// printed on the console, cards simulated. Any other profile gets the real provider,
/// and when its settings are missing the server still starts, logs a
/// configuration error naming each missing setting, and fails every call to
/// that provider with the same message. It never quietly falls back to a
/// development stand-in.
///
/// Every setting is also read from the environment, in upper case with `_`
/// for each `.` (`REEL_PAYMENTS_STRIPE_SECRET`), which is where secrets belong.
@Configuration
public class Services {
    // tag::reel-payment-choice[]
    /// `reel.payments.provider` is one of:
    ///
    /// - `stripe`: [StripePayments], an untested stub over Stripe's REST API.
    ///   It needs `reel.payments.stripe.secret`.
    /// - `simulated`: test card numbers, and no money moves.
    ///
    /// Unset, it is `simulated` on a development profile and `stripe` otherwise.
    ///
    /// With Stripe a card is typed into Stripe's own page, which then sends the
    /// browser back to `/pay/return` on this server. That address has to be the
    /// one a phone can reach, so it is `reel.public.url` when that is set
    /// and the issuer -- the public address of this server, which a deployment
    /// sets anyway -- when it is not.
    @Bean
    public PaymentProvider paymentProvider(Config config,
            @Value("${reel.payments.provider:}") String chosen,
            @Value("${reel.payments.stripe.secret:}") String secret,
            @Value("${reel.public.url:}") String publicUrl,
            @Value("${" + SecurityConfig.ISSUER_KEY + ":}") String issuer) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "simulated" : "stripe";
        if ("simulated".equalsIgnoreCase(kind)) {
            return new SimulatedPayments();
        }
        if (!"stripe".equalsIgnoreCase(kind)) {
            return new MisconfiguredPayments(kind, "reel.payments.provider is '" + kind
                    + "', which is neither stripe nor simulated");
        }
        String address = publicUrl.length() > 0 ? publicUrl : issuer;
        String missing = missing("", new String[] {"reel.payments.stripe.secret", secret,
            "reel.public.url (or " + SecurityConfig.ISSUER_KEY + ")", address});
        if (missing.length() > 0) {
            return new MisconfiguredPayments("stripe", unset("payments", "stripe", missing));
        }
        while (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return new StripePayments(secret, address + "/pay/return");
    }
    // end::reel-payment-choice[]

    // tag::reel-push-choice[]
    /// `reel.push.provider` is one of:
    ///
    /// - `cloud`: the Codename One push service. It needs `reel.push.app`,
    ///   the app's push id, and `reel.push.key`, a server key with the push
    ///   scope; both are in the Push section of the Codename One console.
    /// - `console`: prints each notification, on a development profile only.
    /// - `recording`: keeps each notification for the tests to read, on a
    ///   development profile only.
    ///
    /// Unset, it is `recording` on the `test` profile, `console` on the other
    /// development profiles and `cloud` otherwise. A server whose push settings
    /// are missing still starts and still serves video: it logs a configuration
    /// error at start-up and again for every notification it could not send.
    @Bean
    public PushSender pushSender(Config config,
            @Value("${reel.push.provider:}") String chosen,
            @Value("${reel.push.app:}") String app,
            @Value("${reel.push.key:}") String key,
            @Value("${reel.push.url:}") String url) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "recording"
                : config.isDevelopmentProfile() ? "console" : "cloud";
        if ("console".equalsIgnoreCase(kind) || "recording".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredPushSender("reel.push.provider=" + kind
                        + " sends nothing and is refused on the '" + config.getProfile()
                        + "' profile; set it to cloud");
            }
            return "console".equalsIgnoreCase(kind) ? new ConsolePushSender()
                    : new RecordingPushSender();
        }
        if (!"cloud".equalsIgnoreCase(kind)) {
            return new MisconfiguredPushSender("reel.push.provider is '" + kind
                    + "', which is none of cloud, console or recording");
        }
        String missing = missing("reel.push.", new String[] {"app", app, "key", key});
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
    // end::reel-push-choice[]

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

    /// The license servers. ClearKey and AES-128 are answered by Reel itself
    /// from the keys in `video_key`; Widevine and FairPlay need a vendor
    /// license service (a DRM provider's proxy endpoint), so each is offered
    /// only when its URL is configured.
    @Bean
    public Licenses licenses(VideoKeyRepository keys, Settings settings,
            @Value("${reel.drm.widevine.url:}") String widevineUrl,
            @Value("${reel.drm.widevine.header:}") String widevineHeader,
            @Value("${reel.drm.fairplay.url:}") String fairplayUrl,
            @Value("${reel.drm.fairplay.header:}") String fairplayHeader,
            @Value("${reel.drm.fairplay.certificate:}") String fairplayCertificate) {
        LicenseProvider widevine = widevineUrl.trim().length() == 0 ? null
                : new HttpLicenseProxy("WIDEVINE", widevineUrl.trim(), widevineHeader, null);
        LicenseProvider fairplay = fairplayUrl.trim().length() == 0 ? null
                : new HttpLicenseProxy("FAIRPLAY", fairplayUrl.trim(), fairplayHeader, fairplayCertificate.trim());
        return new Licenses(keys, settings, widevine, fairplay);
    }

    /// No classifier: see [ContentClassifier] for plugging one in.
    @Bean
    public ContentClassifier contentClassifier() {
        return new ContentClassifier() {
            @Override
            public List<String> classify(String name, String synopsis, List<String> tags) {
                return new ArrayList<String>();
            }
        };
    }

    private static String unset(String service, String provider, String missing) {
        return service + " provider '" + provider + "' is not configured; set " + missing;
    }
}
