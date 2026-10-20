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
package com.codenameone.examples.savor;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;
import com.codenameone.examples.savor.geo.Geocoder;
import com.codenameone.examples.savor.geo.PhotonGeocoder;
import com.codenameone.examples.savor.geo.StubGeocoder;
import com.codenameone.examples.savor.pay.MisconfiguredPayments;
import com.codenameone.examples.savor.pay.PaymentProvider;
import com.codenameone.examples.savor.pay.SimulatedPayments;
import com.codenameone.examples.savor.pay.StripePayments;
import com.codenameone.examples.savor.push.CloudPushSender;
import com.codenameone.examples.savor.push.ConsolePushSender;
import com.codenameone.examples.savor.push.MisconfiguredPushSender;
import com.codenameone.examples.savor.push.PushSender;
import com.codenameone.examples.savor.push.RecordingPushSender;
import com.codenameone.examples.savor.sms.ConsoleSmsSender;
import com.codenameone.examples.savor.sms.MessageBirdSmsSender;
import com.codenameone.examples.savor.sms.MisconfiguredSmsSender;
import com.codenameone.examples.savor.sms.SmsSender;
import com.codenameone.examples.savor.sms.TwilioSmsSender;
import com.codenameone.examples.savor.sms.VonageSmsSender;

import java.io.IOException;

/// The services this server reaches outside itself for, each behind an
/// interface and each chosen here from the settings.
///
/// Each is named by one setting -- `savor.sms.provider`,
/// `savor.payments.provider` and `savor.push.provider` -- and, when that is
/// not set, a development profile (dev, test, local) gets the one that needs no
/// account: codes and notifications printed on the console, cards simulated. Any other profile gets the real provider,
/// and when its settings are missing the server still starts, logs a
/// configuration error naming each missing setting, and fails every call to
/// that provider with the same message. It never quietly falls back to a
/// development stand-in.
///
/// Every setting is also read from the environment, in upper case with `_`
/// for each `.` (`SAVOR_SMS_TWILIO_TOKEN`), which is where secrets belong.
@Configuration
public class Services {
    /// `savor.sms.provider` is one of:
    ///
    /// - `twilio`: `savor.sms.twilio.sid`, `.token` and `.from`
    /// - `vonage`: `savor.sms.vonage.key`, `.secret` and `.from`
    /// - `messagebird`: `savor.sms.messagebird.key` and `.originator`
    /// - `console`: prints the message, on a development profile only
    ///
    /// Unset, it is `console` on a development profile and `twilio` otherwise.
    @Bean
    public SmsSender smsSender(Config config,
            @Value("${savor.sms.provider:}") String chosen,
            @Value("${savor.sms.twilio.sid:}") String twilioSid,
            @Value("${savor.sms.twilio.token:}") String twilioToken,
            @Value("${savor.sms.twilio.from:}") String twilioFrom,
            @Value("${savor.sms.vonage.key:}") String vonageKey,
            @Value("${savor.sms.vonage.secret:}") String vonageSecret,
            @Value("${savor.sms.vonage.from:}") String vonageFrom,
            @Value("${savor.sms.messagebird.key:}") String birdKey,
            @Value("${savor.sms.messagebird.originator:}") String birdOriginator) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "console" : "twilio";
        if ("console".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredSmsSender("savor.sms.provider=console only prints codes "
                        + "and is refused on the '" + config.getProfile() + "' profile; set it to "
                        + "twilio, vonage or messagebird");
            }
            return new ConsoleSmsSender();
        }
        if ("twilio".equalsIgnoreCase(kind)) {
            String missing = missing("savor.sms.twilio.", new String[] {"sid", twilioSid,
                "token", twilioToken, "from", twilioFrom});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "twilio",
                    missing)) : new TwilioSmsSender(twilioSid, twilioToken, twilioFrom);
        }
        // tag::savor-sms-choice[]
        if ("vonage".equalsIgnoreCase(kind)) {
            String missing = missing("savor.sms.vonage.", new String[] {"key", vonageKey,
                "secret", vonageSecret, "from", vonageFrom});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "vonage",
                    missing)) : new VonageSmsSender(vonageKey, vonageSecret, vonageFrom);
        }
        // end::savor-sms-choice[]
        if ("messagebird".equalsIgnoreCase(kind)) {
            String missing = missing("savor.sms.messagebird.", new String[] {"key", birdKey,
                "originator", birdOriginator});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "messagebird",
                    missing)) : new MessageBirdSmsSender(birdKey, birdOriginator);
        }
        return new MisconfiguredSmsSender("savor.sms.provider is '" + kind + "', which is "
                + "none of twilio, vonage, messagebird or console");
    }

    /// Photon, at `savor.geocoder.url`. `savor.geocoder=stub` selects the
    /// one that knows ten places and asks nobody, and it is what the `test`
    /// profile gets unless told otherwise.
    @Bean
    public Geocoder geocoder(Config config, @Value("${savor.geocoder:}") String chosen,
            @Value("${savor.geocoder.url:https://photon.komoot.io}") String address)
            throws IOException {
        String kind = chosen.length() > 0 ? chosen
                : "test".equals(config.getProfile()) ? "stub" : "photon";
        if ("stub".equals(kind)) {
            return new StubGeocoder();
        }
        return new PhotonGeocoder(address);
    }

    // tag::savor-payment-choice[]
    /// `savor.payments.provider` is one of:
    ///
    /// - `stripe`: [StripePayments], an untested stub over Stripe's REST API.
    ///   It needs `savor.payments.stripe.secret`.
    /// - `simulated`: test card numbers, and no money moves.
    ///
    /// Unset, it is `simulated` on a development profile and `stripe` otherwise.
    ///
    /// With Stripe a card is typed into Stripe's own page, which then sends the
    /// browser back to `/pay/return` on this server. That address has to be the
    /// one a phone can reach, so it is `savor.public.url` when that is set
    /// and the issuer -- the public address of this server, which a deployment
    /// sets anyway -- when it is not.
    @Bean
    public PaymentProvider paymentProvider(Config config,
            @Value("${savor.payments.provider:}") String chosen,
            @Value("${savor.payments.stripe.secret:}") String secret,
            @Value("${savor.public.url:}") String publicUrl,
            @Value("${" + SecurityConfig.ISSUER_KEY + ":}") String issuer) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "simulated" : "stripe";
        if ("simulated".equalsIgnoreCase(kind)) {
            return new SimulatedPayments();
        }
        if (!"stripe".equalsIgnoreCase(kind)) {
            return new MisconfiguredPayments(kind, "savor.payments.provider is '" + kind
                    + "', which is neither stripe nor simulated");
        }
        String address = publicUrl.length() > 0 ? publicUrl : issuer;
        String missing = missing("", new String[] {"savor.payments.stripe.secret", secret,
            "savor.public.url (or " + SecurityConfig.ISSUER_KEY + ")", address});
        if (missing.length() > 0) {
            return new MisconfiguredPayments("stripe", unset("payments", "stripe", missing));
        }
        while (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return new StripePayments(secret, address + "/pay/return");
    }
    // end::savor-payment-choice[]

    // tag::savor-push-choice[]
    /// `savor.push.provider` is one of:
    ///
    /// - `cloud`: the Codename One push service. It needs `savor.push.app`,
    ///   the app's push id, and `savor.push.key`, a server key with the push
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
            @Value("${savor.push.provider:}") String chosen,
            @Value("${savor.push.app:}") String app,
            @Value("${savor.push.key:}") String key,
            @Value("${savor.push.url:}") String url) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "recording"
                : config.isDevelopmentProfile() ? "console" : "cloud";
        if ("console".equalsIgnoreCase(kind) || "recording".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredPushSender("savor.push.provider=" + kind
                        + " sends nothing and is refused on the '" + config.getProfile()
                        + "' profile; set it to cloud");
            }
            return "console".equalsIgnoreCase(kind) ? new ConsolePushSender()
                    : new RecordingPushSender();
        }
        if (!"cloud".equalsIgnoreCase(kind)) {
            return new MisconfiguredPushSender("savor.push.provider is '" + kind
                    + "', which is none of cloud, console or recording");
        }
        String missing = missing("savor.push.", new String[] {"app", app, "key", key});
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
    // end::savor-push-choice[]

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
