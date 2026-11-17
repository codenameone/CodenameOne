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
package com.codenameone.examples.brickyard.push;

import com.codename1.appkit.server.push.CloudPushSender;
import com.codename1.appkit.server.push.ConsolePushSender;
import com.codename1.appkit.server.push.MisconfiguredPushSender;
import com.codename1.appkit.server.push.PushSender;
import com.codename1.appkit.server.push.RecordingPushSender;
import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.CloudPush;

/// `brickyard.push.provider` is `cloud` (the Codename One push service, with
/// `brickyard.push.app` -- the app's push id, the same value as the app's
/// `PUSH_APP_KEY` -- and `brickyard.push.key`, a server key with the push
/// scope), `console` or `recording` (development profiles only). Unset, it is
/// `recording` on the test profile, `console` on other development profiles
/// and `cloud` otherwise. Missing settings never stop the server: sends are
/// logged as misconfigured instead.
@Configuration
public class PushConfig {
    @Bean
    public PushSender pushSender(Config config,
            @Value("${brickyard.push.provider:}") String chosen,
            @Value("${brickyard.push.app:}") String app,
            @Value("${brickyard.push.key:}") String key) {
        String kind = chosen.trim().length() > 0 ? chosen.trim()
                : "test".equals(config.getProfile()) ? "recording"
                : config.isDevelopmentProfile() ? "console" : "cloud";
        if ("console".equalsIgnoreCase(kind) || "recording".equalsIgnoreCase(kind)) {
            if (!config.isDevelopmentProfile() && !"test".equals(config.getProfile())) {
                return new MisconfiguredPushSender("brickyard.push.provider=" + kind
                        + " is refused on the '" + config.getProfile() + "' profile; set it to cloud");
            }
            return "console".equalsIgnoreCase(kind) ? new ConsolePushSender() : new RecordingPushSender();
        }
        if (!"cloud".equalsIgnoreCase(kind)) {
            return new MisconfiguredPushSender("brickyard.push.provider is '" + kind
                    + "', which is none of cloud, console or recording");
        }
        if (app.trim().length() == 0 || key.trim().length() == 0) {
            return new MisconfiguredPushSender("brickyard.push.app and brickyard.push.key must be set for cloud push");
        }
        try {
            return new CloudPushSender(new CloudPush(app.trim(), key.trim()));
        } catch (IllegalArgumentException err) {
            return new MisconfiguredPushSender("push provider 'cloud' is misconfigured: " + err.getMessage());
        }
    }
}
