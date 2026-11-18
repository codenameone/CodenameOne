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
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codenameone.examples.brickyard.api.StoreNotificationDto;
import com.codenameone.examples.brickyard.api.WalletDto;
import java.io.IOException;

/// Where the store's server-to-server purchase notifications arrive, after
/// whatever receives Apple's signed payload or Google's Pub/Sub push has
/// reduced them to a [StoreNotificationDto], and the stores' own refund
/// notifications, which the stores' signatures authenticate (see [#apple] and
/// [#google]). No player signs in to [#notify], so the
/// sender proves itself with the shared `brickyard.iap.notification-secret`
/// in `X-Store-Notification-Secret`; without one configured, only a
/// development or test server takes notifications. Even then nothing is
/// taken on trust: [Economy#notified] checks the transaction with the store.
@RestController
public class StoreNotifications {
    public static final String PATH = "/api/store/notifications";
    public static final String SECRET_HEADER = "X-Store-Notification-Secret";
    /// Where App Store Server Notifications V2 are sent; Apple's signature authenticates them.
    public static final String APPLE_PATH = PATH + "/apple";
    /// Where the Pub/Sub push subscription for Google Play sends, with an OIDC token.
    public static final String GOOGLE_PATH = PATH + "/google";

    private final Economy economy;
    private final String secret;
    private final boolean dev;
    private AppleSignedData appleSigned;
    private String appleBundle;
    private PubSubPushAuth googleAuth;

    public StoreNotifications(Economy economy, Config config,
            @Value("${brickyard.iap.notification-secret:}") String secret,
            @Value("${brickyard.iap.apple.root-ca:}") String appleRoot,
            @Value("${brickyard.iap.apple.bundle-id:}") String bundleId,
            @Value("${brickyard.iap.google.push-audience:}") String pushAudience,
            @Value("${brickyard.iap.google.push-service-account:}") String pushServiceAccount)
            throws IOException {
        this.economy = economy;
        this.secret = secret.trim();
        this.dev = config.isDevelopmentProfile() || "test".equalsIgnoreCase(config.getProfile());
        if (appleRoot.trim().length() > 0 && bundleId.trim().length() > 0) {
            appleSigned = AppleSignedData.fromRootFile(appleRoot.trim());
            appleBundle = bundleId.trim();
        }
        if (pushAudience.trim().length() > 0) {
            googleAuth = PubSubPushAuth.google(pushAudience.trim(), pushServiceAccount.trim());
        }
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

    /// An App Store Server Notification; a refund takes the Bricks back. The
    /// payload is trusted only once [AppleSignedData] has checked Apple's
    /// signature and chain against the pinned root, and it names this app.
    @PostMapping(APPLE_PATH)
    public String apple(@RequestBody String body) throws Exception {
        AppleSignedData signed = appleSigned;
        if (signed == null) {
            unconfigured("App Store notifications are not configured: set brickyard.iap.apple.root-ca "
                    + "and brickyard.iap.apple.bundle-id");
            return refund(StoreRefunds.apple(body, null, null));
        }
        String purchase;
        try {
            purchase = StoreRefunds.apple(body, signed, appleBundle);
        } catch (IOException notApple) {
            throw new ResponseStatusException(401, "Not signed by Apple: " + notApple.getMessage());
        }
        return refund(purchase);
    }

    /// A Google Play notification pushed by Pub/Sub; a voided purchase takes
    /// the Bricks back. The push is trusted only with Google's OIDC token for
    /// this endpoint's audience, checked by [PubSubPushAuth].
    @PostMapping(GOOGLE_PATH)
    public String google(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String body) throws Exception {
        PubSubPushAuth auth = googleAuth;
        if (auth == null) {
            unconfigured("Google Play notifications are not configured: set "
                    + "brickyard.iap.google.push-audience");
        } else {
            try {
                auth.check(authorization);
            } catch (IOException notGoogle) {
                throw new ResponseStatusException(401, "Not pushed by Google: " + notGoogle.getMessage());
            }
        }
        return refund(StoreRefunds.google(body));
    }

    /// Replaces what the store endpoints trust: the pinned Apple root (DER) and
    /// bundle id, and the keys, audience and optional service account of a
    /// Pub/Sub push. A null root or key source leaves that store unconfigured.
    /// Tests use it to trust a chain and keys of their own.
    public void trust(byte[] appleRoot, String bundleId, JwkSource googleKeys, String pushAudience,
            String pushServiceAccount) {
        appleSigned = appleRoot == null ? null : new AppleSignedData(appleRoot);
        appleBundle = bundleId;
        googleAuth = googleKeys == null ? null : new PubSubPushAuth(googleKeys, pushAudience, pushServiceAccount);
    }

    /// Without a store's settings only a development or test server reads its
    /// notifications, unchecked; anything else answers 503.
    private void unconfigured(String message) {
        if (!dev) {
            throw new ResponseStatusException(503, message);
        }
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
}
