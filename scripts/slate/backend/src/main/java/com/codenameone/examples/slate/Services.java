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
package com.codenameone.examples.slate;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.pay.MisconfiguredPayments;
import com.codenameone.examples.slate.pay.PaymentProvider;
import com.codenameone.examples.slate.pay.SimulatedPayments;
import com.codenameone.examples.slate.pay.StripePayments;
import com.codenameone.examples.slate.push.CloudPushSender;
import com.codenameone.examples.slate.push.ConsolePushSender;
import com.codenameone.examples.slate.push.MisconfiguredPushSender;
import com.codenameone.examples.slate.push.PushSender;
import com.codenameone.examples.slate.push.RecordingPushSender;
import com.codenameone.examples.slate.sms.ConsoleSmsSender;
import com.codenameone.examples.slate.sms.MessageBirdSmsSender;
import com.codenameone.examples.slate.sms.MisconfiguredSmsSender;
import com.codenameone.examples.slate.sms.SmsSender;
import com.codenameone.examples.slate.sms.TwilioSmsSender;
import com.codenameone.examples.slate.sms.VonageSmsSender;
import com.codenameone.examples.slate.sync.CalendarSources;

import java.io.IOException;

/// The services this server reaches outside itself for, each behind an
/// interface and each chosen here from the settings.
///
/// Each is named by one setting -- `slate.sms.provider`,
/// `slate.payments.provider` and `slate.push.provider` -- and, when that is
/// not set, a development profile (dev, test, local) gets the one that needs no
/// account: codes and notifications printed on the console, cards simulated. Any other profile gets the real provider,
/// and when its settings are missing the server still starts, logs a
/// configuration error naming each missing setting, and fails every call to
/// that provider with the same message. It never quietly falls back to a
/// development stand-in.
///
/// Every setting is also read from the environment, in upper case with `_`
/// for each `.` (`SLATE_SMS_TWILIO_TOKEN`), which is where secrets belong.
@Configuration
public class Services {
    /// `slate.sms.provider` is one of:
    ///
    /// - `twilio`: `slate.sms.twilio.sid`, `.token` and `.from`
    /// - `vonage`: `slate.sms.vonage.key`, `.secret` and `.from`
    /// - `messagebird`: `slate.sms.messagebird.key` and `.originator`
    /// - `console`: prints the message, on a development profile only
    ///
    /// Unset, it is `console` on a development profile and `twilio` otherwise.
    @Bean
    public SmsSender smsSender(Config config,
            @Value("${slate.sms.provider:}") String chosen,
            @Value("${slate.sms.twilio.sid:}") String twilioSid,
            @Value("${slate.sms.twilio.token:}") String twilioToken,
            @Value("${slate.sms.twilio.from:}") String twilioFrom,
            @Value("${slate.sms.vonage.key:}") String vonageKey,
            @Value("${slate.sms.vonage.secret:}") String vonageSecret,
            @Value("${slate.sms.vonage.from:}") String vonageFrom,
            @Value("${slate.sms.messagebird.key:}") String birdKey,
            @Value("${slate.sms.messagebird.originator:}") String birdOriginator) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "console" : "twilio";
        if ("console".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredSmsSender("slate.sms.provider=console only prints codes "
                        + "and is refused on the '" + config.getProfile() + "' profile; set it to "
                        + "twilio, vonage or messagebird");
            }
            return new ConsoleSmsSender();
        }
        if ("twilio".equalsIgnoreCase(kind)) {
            String missing = missing("slate.sms.twilio.", new String[] {"sid", twilioSid,
                "token", twilioToken, "from", twilioFrom});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "twilio",
                    missing)) : new TwilioSmsSender(twilioSid, twilioToken, twilioFrom);
        }
        // tag::slate-sms-choice[]
        if ("vonage".equalsIgnoreCase(kind)) {
            String missing = missing("slate.sms.vonage.", new String[] {"key", vonageKey,
                "secret", vonageSecret, "from", vonageFrom});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "vonage",
                    missing)) : new VonageSmsSender(vonageKey, vonageSecret, vonageFrom);
        }
        // end::slate-sms-choice[]
        if ("messagebird".equalsIgnoreCase(kind)) {
            String missing = missing("slate.sms.messagebird.", new String[] {"key", birdKey,
                "originator", birdOriginator});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "messagebird",
                    missing)) : new MessageBirdSmsSender(birdKey, birdOriginator);
        }
        return new MisconfiguredSmsSender("slate.sms.provider is '" + kind + "', which is "
                + "none of twilio, vonage, messagebird or console");
    }

    // tag::slate-payment-choice[]
    /// `slate.payments.provider` is one of:
    ///
    /// - `stripe`: [StripePayments], an untested stub over Stripe's REST API.
    ///   It needs `slate.payments.stripe.secret`.
    /// - `simulated`: test card numbers, and no money moves.
    ///
    /// Unset, it is `simulated` on a development profile and `stripe` otherwise.
    ///
    /// With Stripe a card is typed into Stripe's own page, which then sends the
    /// browser back to `/pay/return` on this server. That address has to be the
    /// one a phone can reach, so it is `slate.public.url` when that is set
    /// and the issuer -- the public address of this server, which a deployment
    /// sets anyway -- when it is not.
    @Bean
    public PaymentProvider paymentProvider(Config config,
            @Value("${slate.payments.provider:}") String chosen,
            @Value("${slate.payments.stripe.secret:}") String secret,
            @Value("${slate.public.url:}") String publicUrl,
            @Value("${" + SecurityConfig.ISSUER_KEY + ":}") String issuer) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "simulated" : "stripe";
        if ("simulated".equalsIgnoreCase(kind)) {
            return new SimulatedPayments();
        }
        if (!"stripe".equalsIgnoreCase(kind)) {
            return new MisconfiguredPayments(kind, "slate.payments.provider is '" + kind
                    + "', which is neither stripe nor simulated");
        }
        String address = publicUrl.length() > 0 ? publicUrl : issuer;
        String missing = missing("", new String[] {"slate.payments.stripe.secret", secret,
            "slate.public.url (or " + SecurityConfig.ISSUER_KEY + ")", address});
        if (missing.length() > 0) {
            return new MisconfiguredPayments("stripe", unset("payments", "stripe", missing));
        }
        while (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return new StripePayments(secret, address + "/pay/return");
    }
    // end::slate-payment-choice[]

    // tag::slate-calendar-choice[]
    /// The calendars the server reads for providers.
    ///
    /// - Google: `slate.calendar.google.id` and `slate.calendar.google.secret`,
    ///   an OAuth client of type "Web application" with the Calendar API on and
    ///   the `calendar.freebusy` scope.
    /// - Microsoft: `slate.calendar.microsoft.id` and
    ///   `slate.calendar.microsoft.secret`, an Entra app registration with the
    ///   delegated `Calendars.ReadBasic` and `offline_access` permissions.
    /// - A published iCalendar feed needs nothing.
    ///
    /// On a development profile a kind with no client is simulated, so the
    /// demo connects calendars without an account anywhere. Elsewhere it is
    /// not offered. `slate.timezone` is the zone a feed's all-day events and
    /// the simulated busy time are read in.
    @Bean
    public CalendarSources calendarSources(Config config,
            @Value("${slate.calendar.google.id:}") String googleId,
            @Value("${slate.calendar.google.secret:}") String googleSecret,
            @Value("${slate.calendar.microsoft.id:}") String microsoftId,
            @Value("${slate.calendar.microsoft.secret:}") String microsoftSecret,
            @Value("${slate.timezone:Europe/London}") String zone) {
        return new CalendarSources(googleId.trim(), googleSecret.trim(), microsoftId.trim(),
                microsoftSecret.trim(), config.isDevelopmentProfile(), Catalog.zone(zone));
    }
    // end::slate-calendar-choice[]

    // tag::slate-push-choice[]
    /// `slate.push.provider` is one of:
    ///
    /// - `cloud`: the Codename One push service. It needs `slate.push.app`,
    ///   the app's push id, and `slate.push.key`, a server key with the push
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
            @Value("${slate.push.provider:}") String chosen,
            @Value("${slate.push.app:}") String app,
            @Value("${slate.push.key:}") String key,
            @Value("${slate.push.url:}") String url) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "recording"
                : config.isDevelopmentProfile() ? "console" : "cloud";
        if ("console".equalsIgnoreCase(kind) || "recording".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredPushSender("slate.push.provider=" + kind
                        + " sends nothing and is refused on the '" + config.getProfile()
                        + "' profile; set it to cloud");
            }
            return "console".equalsIgnoreCase(kind) ? new ConsolePushSender()
                    : new RecordingPushSender();
        }
        if (!"cloud".equalsIgnoreCase(kind)) {
            return new MisconfiguredPushSender("slate.push.provider is '" + kind
                    + "', which is none of cloud, console or recording");
        }
        String missing = missing("slate.push.", new String[] {"app", app, "key", key});
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
    // end::slate-push-choice[]

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
