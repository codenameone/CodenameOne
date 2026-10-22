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
package com.codenameone.examples.parley;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;
import com.codenameone.examples.parley.push.CloudPushSender;
import com.codenameone.examples.parley.push.ConsolePushSender;
import com.codenameone.examples.parley.push.MisconfiguredPushSender;
import com.codenameone.examples.parley.push.PushSender;
import com.codenameone.examples.parley.push.RecordingPushSender;
import com.codenameone.examples.parley.sms.ConsoleSmsSender;
import com.codenameone.examples.parley.sms.MessageBirdSmsSender;
import com.codenameone.examples.parley.sms.MisconfiguredSmsSender;
import com.codenameone.examples.parley.sms.SmsSender;
import com.codenameone.examples.parley.sms.TwilioSmsSender;
import com.codenameone.examples.parley.sms.VonageSmsSender;


/// The services this server reaches outside itself for, each behind an
/// interface and each chosen here from the settings.
///
/// Each is named by one setting -- `parley.sms.provider` and
/// `parley.push.provider` -- and, when that is not set, a development profile
/// (dev, test, local) gets the one that needs no account: codes and
/// notifications printed on the console. Any other profile gets the real provider,
/// and when its settings are missing the server still starts, logs a
/// configuration error naming each missing setting, and fails every call to
/// that provider with the same message. It never quietly falls back to a
/// development stand-in.
///
/// Every setting is also read from the environment, in upper case with `_`
/// for each `.` (`PARLEY_SMS_TWILIO_TOKEN`), which is where secrets belong.
@Configuration
public class Services {
    /// `parley.sms.provider` is one of:
    ///
    /// - `twilio`: `parley.sms.twilio.sid`, `.token` and `.from`
    /// - `vonage`: `parley.sms.vonage.key`, `.secret` and `.from`
    /// - `messagebird`: `parley.sms.messagebird.key` and `.originator`
    /// - `console`: prints the message, on a development profile only
    ///
    /// Unset, it is `console` on a development profile and `twilio` otherwise.
    @Bean
    public SmsSender smsSender(Config config,
            @Value("${parley.sms.provider:}") String chosen,
            @Value("${parley.sms.twilio.sid:}") String twilioSid,
            @Value("${parley.sms.twilio.token:}") String twilioToken,
            @Value("${parley.sms.twilio.from:}") String twilioFrom,
            @Value("${parley.sms.vonage.key:}") String vonageKey,
            @Value("${parley.sms.vonage.secret:}") String vonageSecret,
            @Value("${parley.sms.vonage.from:}") String vonageFrom,
            @Value("${parley.sms.messagebird.key:}") String birdKey,
            @Value("${parley.sms.messagebird.originator:}") String birdOriginator) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : config.isDevelopmentProfile() ? "console" : "twilio";
        if ("console".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredSmsSender("parley.sms.provider=console only prints codes "
                        + "and is refused on the '" + config.getProfile() + "' profile; set it to "
                        + "twilio, vonage or messagebird");
            }
            return new ConsoleSmsSender();
        }
        if ("twilio".equalsIgnoreCase(kind)) {
            String missing = missing("parley.sms.twilio.", new String[] {"sid", twilioSid,
                "token", twilioToken, "from", twilioFrom});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "twilio",
                    missing)) : new TwilioSmsSender(twilioSid, twilioToken, twilioFrom);
        }
        // tag::parley-sms-choice[]
        if ("vonage".equalsIgnoreCase(kind)) {
            String missing = missing("parley.sms.vonage.", new String[] {"key", vonageKey,
                "secret", vonageSecret, "from", vonageFrom});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "vonage",
                    missing)) : new VonageSmsSender(vonageKey, vonageSecret, vonageFrom);
        }
        // end::parley-sms-choice[]
        if ("messagebird".equalsIgnoreCase(kind)) {
            String missing = missing("parley.sms.messagebird.", new String[] {"key", birdKey,
                "originator", birdOriginator});
            return missing.length() > 0 ? new MisconfiguredSmsSender(unset("SMS", "messagebird",
                    missing)) : new MessageBirdSmsSender(birdKey, birdOriginator);
        }
        return new MisconfiguredSmsSender("parley.sms.provider is '" + kind + "', which is "
                + "none of twilio, vonage, messagebird or console");
    }

    // tag::parley-push-choice[]
    /// `parley.push.provider` is one of:
    ///
    /// - `cloud`: the Codename One push service. It needs `parley.push.app`,
    ///   the app's push id, and `parley.push.key`, a server key with the push
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
            @Value("${parley.push.provider:}") String chosen,
            @Value("${parley.push.app:}") String app,
            @Value("${parley.push.key:}") String key,
            @Value("${parley.push.url:}") String url) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "recording"
                : config.isDevelopmentProfile() ? "console" : "cloud";
        if ("console".equalsIgnoreCase(kind) || "recording".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredPushSender("parley.push.provider=" + kind
                        + " sends nothing and is refused on the '" + config.getProfile()
                        + "' profile; set it to cloud");
            }
            return "console".equalsIgnoreCase(kind) ? new ConsolePushSender()
                    : new RecordingPushSender();
        }
        if (!"cloud".equalsIgnoreCase(kind)) {
            return new MisconfiguredPushSender("parley.push.provider is '" + kind
                    + "', which is none of cloud, console or recording");
        }
        String missing = missing("parley.push.", new String[] {"app", app, "key", key});
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
    // end::parley-push-choice[]

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
