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
package com.codenameone.examples.fieldline;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;
import com.codenameone.examples.fieldline.ai.ClaudeClient;
import com.codenameone.examples.fieldline.geo.Geocoder;
import com.codenameone.examples.fieldline.geo.PhotonGeocoder;
import com.codenameone.examples.fieldline.geo.StubGeocoder;
import com.codename1.appkit.server.push.CloudPushSender;
import com.codename1.appkit.server.push.ConsolePushSender;
import com.codename1.appkit.server.push.MisconfiguredPushSender;
import com.codename1.appkit.server.push.PushSender;
import com.codename1.appkit.server.push.RecordingPushSender;

import java.io.IOException;

/// The services this server reaches outside itself for, each behind an
/// interface and each chosen here from the settings.
///
/// Each is named by one setting -- `fieldline.push.provider`,
/// `fieldline.geocoder` and `fieldline.ai.provider` -- and, when that is not
/// set, a development profile (dev, test, local) gets the one that needs no
/// account. Any other profile gets the real provider, and when its settings
/// are missing the server still starts and logs a configuration error naming
/// each missing setting.
///
/// Every setting is also read from the environment, in upper case with `_`
/// for each `.` (`FIELDLINE_AI_KEY`), which is where secrets belong.
@Configuration
public class Services {
    /// The model behind the assistant: photo diagnosis, report drafting,
    /// manual lookups and the dispatcher's natural-language re-planning.
    ///
    /// `fieldline.ai.provider` is `anthropic` or `demo`. Unset, it is
    /// `anthropic` when `fieldline.ai.key` (`FIELDLINE_AI_KEY`) holds a key and
    /// `demo` otherwise. `demo` answers from rules written into this server --
    /// a fixed diagnosis per kind of asset, a report assembled from the
    /// checklist, an assistant that understands a handful of phrasings -- so
    /// the template is whole without an account, and the tests are
    /// repeatable. Every answer says which of the two produced it.
    ///
    /// `fieldline.ai.model` overrides the model, `claude-opus-5-5` by default.
    @Bean
    public ClaudeClient claude(
            @Value("${fieldline.ai.provider:}") String chosen,
            @Value("${fieldline.ai.key:}") String key,
            @Value("${fieldline.ai.model:claude-opus-5-5}") String model,
            @Value("${fieldline.ai.url:https://api.anthropic.com}") String url) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : key.trim().length() > 0 ? "anthropic" : "demo";
        if ("anthropic".equalsIgnoreCase(kind)) {
            if (key.trim().length() == 0) {
                System.err.println("Configuration error: " + unset("AI", "anthropic",
                        "fieldline.ai.key") + "; answering from the demo rules");
                return ClaudeClient.demo();
            }
            return new ClaudeClient(url.trim(), key.trim(), model.trim());
        }
        if (!"demo".equalsIgnoreCase(kind)) {
            System.err.println("Configuration error: fieldline.ai.provider is '" + kind
                    + "', which is neither anthropic nor demo; answering from the demo rules");
        }
        return ClaudeClient.demo();
    }

    /// Photon, at `fieldline.geocoder.url`. `fieldline.geocoder=stub` selects the
    /// one that knows ten places and asks nobody, and it is what the `test`
    /// profile gets unless told otherwise.
    @Bean
    public Geocoder geocoder(Config config, @Value("${fieldline.geocoder:}") String chosen,
            @Value("${fieldline.geocoder.url:https://photon.komoot.io}") String address)
            throws IOException {
        String kind = chosen.length() > 0 ? chosen
                : "test".equals(config.getProfile()) ? "stub" : "photon";
        if ("stub".equals(kind)) {
            return new StubGeocoder();
        }
        return new PhotonGeocoder(address);
    }

    // tag::fieldline-push-choice[]
    /// `fieldline.push.provider` is one of:
    ///
    /// - `cloud`: the Codename One push service. It needs `fieldline.push.app`,
    ///   the app's push id, and `fieldline.push.key`, a server key with the push
    ///   scope; both are in the Push section of the Codename One console.
    /// - `console`: prints each notification, on a development profile only.
    /// - `recording`: keeps each notification for the tests to read, on a
    ///   development profile only.
    ///
    /// Unset, it is `recording` on the `test` profile, `console` on the other
    /// development profiles and `cloud` otherwise. A server whose push settings
    /// are missing still starts and still serves jobs: it logs a configuration
    /// error at start-up and again for every notification it could not send.
    @Bean
    public PushSender pushSender(Config config,
            @Value("${fieldline.push.provider:}") String chosen,
            @Value("${fieldline.push.app:}") String app,
            @Value("${fieldline.push.key:}") String key,
            @Value("${fieldline.push.url:}") String url) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "recording"
                : config.isDevelopmentProfile() ? "console" : "cloud";
        if ("console".equalsIgnoreCase(kind) || "recording".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile()) {
                return new MisconfiguredPushSender("fieldline.push.provider=" + kind
                        + " sends nothing and is refused on the '" + config.getProfile()
                        + "' profile; set it to cloud");
            }
            return "console".equalsIgnoreCase(kind) ? new ConsolePushSender()
                    : new RecordingPushSender();
        }
        if (!"cloud".equalsIgnoreCase(kind)) {
            return new MisconfiguredPushSender("fieldline.push.provider is '" + kind
                    + "', which is none of cloud, console or recording");
        }
        String missing = missing("fieldline.push.", new String[] {"app", app, "key", key});
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
    // end::fieldline-push-choice[]

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
