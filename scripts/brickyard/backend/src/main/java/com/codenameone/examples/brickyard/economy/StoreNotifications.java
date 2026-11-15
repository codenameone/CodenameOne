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
package com.codenameone.examples.brickyard.economy;

import com.codename1.backend.Config;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestBody;
import com.codename1.backend.annotations.RequestHeader;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codenameone.examples.brickyard.api.StoreNotificationDto;
import com.codenameone.examples.brickyard.api.WalletDto;

/// Where the store's server-to-server purchase notifications arrive, after
/// whatever receives Apple's signed payload or Google's Pub/Sub push has
/// reduced them to a [StoreNotificationDto]. No player signs in here, so the
/// sender proves itself with the shared `brickyard.iap.notification-secret`
/// in `X-Store-Notification-Secret`; without one configured, only a
/// development or test server takes notifications. Even then nothing is
/// taken on trust: [Economy#notified] checks the transaction with the store.
@RestController
public class StoreNotifications {
    public static final String PATH = "/api/store/notifications";
    public static final String SECRET_HEADER = "X-Store-Notification-Secret";
    /// Where App Store Server Notifications V2 are sent, with `?secret=`.
    public static final String APPLE_PATH = PATH + "/apple";
    /// Where the Pub/Sub push subscription for Google Play sends, with `?secret=`.
    public static final String GOOGLE_PATH = PATH + "/google";

    private final Economy economy;
    private final String secret;
    private final boolean dev;

    public StoreNotifications(Economy economy, Config config,
            @Value("${brickyard.iap.notification-secret:}") String secret) {
        this.economy = economy;
        this.secret = secret.trim();
        this.dev = config.isDevelopmentProfile() || "test".equalsIgnoreCase(config.getProfile());
    }

    @PostMapping(PATH)
    public WalletDto notify(@RequestHeader(value = SECRET_HEADER, required = false) String presented,
            @RequestBody StoreNotificationDto notification) throws Exception {
        if (secret.length() == 0) {
            if (!dev) {
                throw new ResponseStatusException(503,
                        "Store notifications are not configured: set brickyard.iap.notification-secret");
            }
        } else if (!OAuth2Parameters.equalsConstantTime(secret, presented)) {
            throw new ResponseStatusException(401, "Not the store");
        }
        return economy.notified(notification);
    }

    /// An App Store Server Notification; a refund takes the Bricks back.
    @PostMapping(APPLE_PATH)
    public String apple(@RequestParam(value = "secret", required = false) String presented,
            @RequestBody String body) throws Exception {
        check(presented);
        return refund(StoreRefunds.apple(body));
    }

    /// A Google Play notification pushed by Pub/Sub; a voided purchase takes the Bricks back.
    @PostMapping(GOOGLE_PATH)
    public String google(@RequestParam(value = "secret", required = false) String presented,
            @RequestBody String body) throws Exception {
        check(presented);
        return refund(StoreRefunds.google(body));
    }

    /// Answers 200 for anything handled or deliberately ignored, so the store
    /// does not retry it; a refund of a purchase never seen here is ignored.
    private String refund(String purchaseId) throws Exception {
        if (purchaseId == null || !economy.isRefundable(purchaseId)) {
            return "ignored";
        }
        economy.refund(purchaseId, "store refund");
        return "refunded";
    }

    private void check(String presented) {
        if (secret.length() == 0) {
            if (!dev) {
                throw new ResponseStatusException(503,
                        "Store notifications are not configured: set brickyard.iap.notification-secret");
            }
        } else if (!OAuth2Parameters.equalsConstantTime(secret, presented)) {
            throw new ResponseStatusException(401, "Not the store");
        }
    }
}
