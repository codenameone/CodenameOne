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
package com.codenameone.examples.bazaar;

import com.codename1.appkit.server.security.AccountSecurity;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;
import com.codenameone.examples.bazaar.carrier.CarrierProvider;
import com.codenameone.examples.bazaar.carrier.EasyPostCarrier;
import com.codenameone.examples.bazaar.carrier.MisconfiguredCarrier;
import com.codenameone.examples.bazaar.carrier.SimulatedCarrier;
import com.codenameone.examples.bazaar.geo.Geocoder;
import com.codenameone.examples.bazaar.geo.PhotonGeocoder;
import com.codenameone.examples.bazaar.geo.StubGeocoder;
import com.codenameone.examples.bazaar.pay.MisconfiguredPayments;
import com.codenameone.examples.bazaar.pay.PaymentProvider;
import com.codenameone.examples.bazaar.pay.SimulatedPayments;
import com.codenameone.examples.bazaar.pay.StripePayments;
import com.codenameone.examples.bazaar.trade.VatRegistry;
import com.codenameone.examples.bazaar.trade.ViesRegistry;
import com.codename1.appkit.server.push.CloudPushSender;
import com.codename1.appkit.server.push.ConsolePushSender;
import com.codename1.appkit.server.push.MisconfiguredPushSender;
import com.codename1.appkit.server.push.PushSender;
import com.codename1.appkit.server.push.RecordingPushSender;
import com.codenameone.examples.bazaar.sms.ConsoleSmsSender;
import com.codenameone.examples.bazaar.sms.MessageBirdSmsSender;
import com.codenameone.examples.bazaar.sms.MisconfiguredSmsSender;
import com.codenameone.examples.bazaar.sms.SmsSender;
import com.codenameone.examples.bazaar.sms.TwilioSmsSender;
import com.codenameone.examples.bazaar.sms.VonageSmsSender;

import java.io.IOException;

/// The services this server reaches outside itself for, each behind an
/// interface and each chosen here from the settings.
///
/// Each is named by one setting -- `bazaar.sms.provider`,
/// `bazaar.payments.provider` and `bazaar.push.provider` -- and, when that is
/// not set, a development profile (dev, test, local) gets the one that needs no
/// account: codes and notifications printed on the console, cards simulated. Any other profile gets the real provider,
/// and when its settings are missing the server still starts, logs a
/// configuration error naming each missing setting, and fails every call to
/// that provider with the same message. It never quietly falls back to a
/// development stand-in.
///
/// Every setting is also read from the environment, in upper case with `_`
/// for each `.` (`BAZAAR_SMS_TWILIO_TOKEN`), which is where secrets belong.
@Configuration
public class Services {
    /// `bazaar.sms.provider` is one of:
    ///
    /// - `twilio`: `bazaar.sms.twilio.sid`, `.token` and `.from`
    /// - `vonage`: `bazaar.sms.vonage.key`, `.secret` and `.from`
    /// - `messagebird`: `bazaar.sms.messagebird.key` and `.originator`
    /// - `console`: prints the message, on a development profile only
    ///
    /// Unset, it is `console` on a development profile and `twilio` otherwise.
    @Bean
    public SmsSender smsSender(Config config,
            @Value("${bazaar.sms.provider:}") String chosen,
            @Value("${bazaar.sms.twilio.sid:}") String twilioSid,
            @Value("${bazaar.sms.twilio.token:}") String twilioToken,
            @Value("${bazaar.sms.twilio.from:}") String twilioFrom,
            @Value("${bazaar.sms.vonage.key:}") String vonageKey,
            @Value("${bazaar.sms.vonage.secret:}") String vonageSecret,
            @Value("${bazaar.sms.vonage.from:}") String vonageFrom,
            @Value("${bazaar.sms.messagebird.key:}") String birdKey,
            @Value("${bazaar.sms.messagebird.originator:}") String birdOriginator) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "console" : "twilio";
        if ("console".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredSmsSender("bazaar.sms.provider=console only prints codes "
                        + "and is refused on the '" + config.getProfile() + "' profile; set it to "
                        + "twilio, vonage or messagebird");
            }
            return new ConsoleSmsSender();
        }
        if ("twilio".equalsIgnoreCase(kind)) {
            String missing = missing("bazaar.sms.twilio.", new String[] {"sid", twilioSid,
                "token", twilioToken, "from", twilioFrom});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "twilio",
                    missing)) : new TwilioSmsSender(twilioSid, twilioToken, twilioFrom);
        }
        // tag::bazaar-sms-choice[]
        if ("vonage".equalsIgnoreCase(kind)) {
            String missing = missing("bazaar.sms.vonage.", new String[] {"key", vonageKey,
                "secret", vonageSecret, "from", vonageFrom});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "vonage",
                    missing)) : new VonageSmsSender(vonageKey, vonageSecret, vonageFrom);
        }
        // end::bazaar-sms-choice[]
        if ("messagebird".equalsIgnoreCase(kind)) {
            String missing = missing("bazaar.sms.messagebird.", new String[] {"key", birdKey,
                "originator", birdOriginator});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "messagebird",
                    missing)) : new MessageBirdSmsSender(birdKey, birdOriginator);
        }
        return new MisconfiguredSmsSender("bazaar.sms.provider is '" + kind + "', which is "
                + "none of twilio, vonage, messagebird or console");
    }

    /// Photon, at `bazaar.geocoder.url`. `bazaar.geocoder=stub` selects the
    /// one that knows ten places and asks nobody, and it is what the `test`
    /// profile gets unless told otherwise.
    @Bean
    public Geocoder geocoder(Config config, @Value("${bazaar.geocoder:}") String chosen,
            @Value("${bazaar.geocoder.url:https://photon.komoot.io}") String address)
            throws IOException {
        String kind = chosen.length() > 0 ? chosen
                : "test".equals(config.getProfile()) ? "stub" : "photon";
        if ("stub".equals(kind)) {
            return new StubGeocoder();
        }
        return new PhotonGeocoder(address);
    }

    /// `bazaar.vat.registry` is `vies` -- the European Commission's service at
    /// `bazaar.vat.vies.url` -- or `none`, which checks the format only. Unset, it
    /// is `none` under the `test` profile and `vies` otherwise. VIES needs no key.
    @Bean
    public VatRegistry vatRegistry(Config config,
            @Value("${bazaar.vat.registry:}") String chosen,
            @Value("${bazaar.vat.vies.url:" + ViesRegistry.BASE + "}") String address) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "none" : "vies";
        return "none".equalsIgnoreCase(kind) ? VatRegistry.NONE : new ViesRegistry(address);
    }

    // tag::bazaar-payment-choice[]
    /// `bazaar.payments.provider` is one of:
    ///
    /// - `stripe`: [StripePayments], an untested stub over Stripe's REST API.
    ///   It needs `bazaar.payments.stripe.secret`.
    /// - `simulated`: test card numbers, and no money moves.
    ///
    /// Unset, it is `simulated` on a development profile and `stripe` otherwise.
    ///
    /// With Stripe a card is typed into Stripe's own page, which then sends the
    /// browser back to `/pay/return` on this server. That address has to be the
    /// one a phone can reach, so it is `bazaar.public.url` when that is set
    /// and the issuer -- the public address of this server, which a deployment
    /// sets anyway -- when it is not.
    @Bean
    public PaymentProvider paymentProvider(Config config,
            @Value("${bazaar.payments.provider:}") String chosen,
            @Value("${bazaar.payments.stripe.secret:}") String secret,
            @Value("${bazaar.public.url:}") String publicUrl,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "simulated" : "stripe";
        if ("simulated".equalsIgnoreCase(kind)) {
            return new SimulatedPayments();
        }
        if (!"stripe".equalsIgnoreCase(kind)) {
            return new MisconfiguredPayments(kind, "bazaar.payments.provider is '" + kind
                    + "', which is neither stripe nor simulated");
        }
        String address = publicUrl.length() > 0 ? publicUrl : issuer;
        String missing = missing("", new String[] {"bazaar.payments.stripe.secret", secret,
            "bazaar.public.url (or " + AccountSecurity.ISSUER_KEY + ")", address});
        if (missing.length() > 0) {
            return new MisconfiguredPayments("stripe", unset("payments", "stripe", missing));
        }
        while (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return new StripePayments(secret, address + "/pay/return");
    }
    // end::bazaar-payment-choice[]

    // tag::bazaar-push-choice[]
    /// `bazaar.push.provider` is one of:
    ///
    /// - `cloud`: the Codename One push service. It needs `bazaar.push.app`,
    ///   the app's push id, and `bazaar.push.key`, a server key with the push
    ///   scope; both are in the Push section of the Codename One console.
    /// - `console`: prints each notification, on a development profile only.
    /// - `recording`: keeps each notification for the tests to read, on a
    ///   development profile only.
    ///
    /// Unset, it is `recording` on the `test` profile, `console` on the other
    /// development profiles and `cloud` otherwise. A server whose push settings
    /// are missing still starts and still serves requests: it logs a configuration
    /// error at start-up and again for every notification it could not send.
    @Bean
    public PushSender pushSender(Config config,
            @Value("${bazaar.push.provider:}") String chosen,
            @Value("${bazaar.push.app:}") String app,
            @Value("${bazaar.push.key:}") String key,
            @Value("${bazaar.push.url:}") String url) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "recording"
                : config.isDevelopmentProfile() ? "console" : "cloud";
        if ("console".equalsIgnoreCase(kind) || "recording".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredPushSender("bazaar.push.provider=" + kind
                        + " sends nothing and is refused on the '" + config.getProfile()
                        + "' profile; set it to cloud");
            }
            return "console".equalsIgnoreCase(kind) ? new ConsolePushSender()
                    : new RecordingPushSender();
        }
        if (!"cloud".equalsIgnoreCase(kind)) {
            return new MisconfiguredPushSender("bazaar.push.provider is '" + kind
                    + "', which is none of cloud, console or recording");
        }
        String missing = missing("bazaar.push.", new String[] {"app", app, "key", key});
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
    // end::bazaar-push-choice[]

    /// `bazaar.carrier.provider` is `simulated` -- rates from weight and
    /// distance, labels with made-up tracking numbers, and scans that walk
    /// each parcel to the door one `bazaar.carrier.simulated.step.seconds`
    /// (default 75) at a time. Unset, it is `simulated` on a development
    /// profile; any other profile gets a [MisconfiguredCarrier], so labels are
    /// never printed for parcels nobody will collect.
    ///
    /// `easypost` is a real carrier account ([EasyPostCarrier]): it needs
    /// `bazaar.carrier.easypost.key` and `bazaar.carrier.easypost.webhook-secret`,
    /// and without both it is a [MisconfiguredCarrier] naming the missing one --
    /// a missing key never selects it. `bazaar.carrier.origin.{country,postal,city,
    /// region}` is the ship-from address, and `bazaar.carrier.easypost.poll.minutes`
    /// (default 30) how often a parcel is polled besides the webhook.
    @Bean
    public CarrierProvider carrierProvider(Config config,
            @Value("${bazaar.carrier.provider:}") String chosen,
            @Value("${bazaar.carrier.simulated.step.seconds:75}") int stepSeconds)
            throws IOException {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? SimulatedCarrier.NAME : "";
        if (SimulatedCarrier.NAME.equalsIgnoreCase(kind)) {
            return new SimulatedCarrier(Math.max(1, stepSeconds) * 1000L);
        }
        if (EasyPostCarrier.NAME.equalsIgnoreCase(kind)) {
            String key = config.get("bazaar.carrier.easypost.key", "").trim();
            String secret = config.get("bazaar.carrier.easypost.webhook-secret", "").trim();
            String missing = missing("bazaar.carrier.easypost.",
                    new String[] {"key", key, "webhook-secret", secret});
            if (missing.length() > 0) {
                return new MisconfiguredCarrier(EasyPostCarrier.NAME, "set " + missing);
            }
            int minutes = 30;
            try {
                minutes = Integer.parseInt(config.get("bazaar.carrier.easypost.poll.minutes",
                        "30").trim());
            } catch (NumberFormatException bad) {
                minutes = 30;
            }
            CarrierProvider.Place origin = new CarrierProvider.Place("", "",
                    config.get("bazaar.carrier.origin.city", ""), 0, 0,
                    config.get("bazaar.carrier.origin.postal", ""),
                    config.get("bazaar.carrier.origin.region", ""),
                    config.get("bazaar.carrier.origin.country", ""));
            return EasyPostCarrier.live(key, secret, origin, Math.max(1, minutes) * 60000L);
        }
        if (kind.length() == 0) {
            return new MisconfiguredCarrier("none", "carrier provider is not configured; this "
                    + "sample has no real carrier integration, so set bazaar.carrier.provider="
                    + "simulated or add a CarrierProvider");
        }
        return new MisconfiguredCarrier(kind, "bazaar.carrier.provider is '" + kind
                + "', which is neither simulated nor easypost");
    }

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
