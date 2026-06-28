/*
 * Codename One - client-side secret fetching backed by SecureStorage.
 *
 * Pulls a developer-defined secret (e.g. a Google Maps API key) from the
 * Codename One Cloud vault at runtime and caches it in the platform keychain,
 * so the value never has to be baked into the app binary.
 */
package com.codename1.security;

import com.codename1.io.Log;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.Rest;
import com.codename1.ui.Display;
import java.util.Map;

/// Runtime access to the secrets your app needs (API keys such as a Google Maps
/// key) WITHOUT shipping them inside the binary, where they are trivially
/// extractable. A secret is defined once in the Codename One Cloud vault (the
/// *Secrets* page of the build console), fetched on demand over TLS, and cached
/// in the device keychain via [SecureStorage] -- so you can rotate the value
/// server-side without reshipping the app.
///
/// ```java
/// // off the EDT (the first call may hit the network):
/// String mapsKey = Secrets.get("googlemaps.key");
/// ```
///
/// Only secrets the developer published to the app-readable namespace are
/// served to a device; server-only credentials (such as the App Store / Google
/// Play keys used by commerce validation) are never reachable from the client.
/// On the simulator or a local (non-cloud) build there is no build key, so
/// [#get(String)] returns the cached value if any and otherwise `null`.
public final class Secrets {

    private static final String CACHE_PREFIX = "cn1secret.";
    private static final String PROP_ENDPOINT = "secrets.cloud.endpoint";
    private static final String PROP_BUILD_KEY = "build_key";
    private static final String PROP_PACKAGE = "package_name";
    private static final String DEFAULT_ENDPOINT = "https://cloud.codenameone.com/api/v2/secrets";

    private Secrets() {
    }

    /// The secret named `name` -- from the keychain cache when present,
    /// otherwise fetched from the cloud vault and cached. Returns `null` when
    /// it is unavailable (undefined, not app-readable, or offline with no
    /// cached copy). The first fetch blocks on the network, so call this off
    /// the EDT.
    public static String get(String name) {
        String cached = SecureStorage.getInstance().get(CACHE_PREFIX + name);
        if (cached != null) {
            return cached;
        }
        return refresh(name);
    }

    /// As [#get(String)] but returns `defaultValue` when the secret is absent.
    public static String get(String name, String defaultValue) {
        String v = get(name);
        return v == null ? defaultValue : v;
    }

    /// Force a fresh fetch from the cloud vault, replacing the keychain cache.
    /// Returns the new value, or `null` when unavailable.
    public static String refresh(String name) {
        String v = fetch(name);
        if (v != null) {
            SecureStorage.getInstance().set(CACHE_PREFIX + name, v);
        }
        return v;
    }

    /// Drop the cached copy of `name` from the keychain; the next [#get(String)]
    /// re-fetches it.
    public static void clear(String name) {
        SecureStorage.getInstance().remove(CACHE_PREFIX + name);
    }

    private static String fetch(String name) {
        String key = Display.getInstance().getProperty(PROP_BUILD_KEY, "");
        if (key == null || key.length() == 0) {
            return null; // simulator / local build: no cloud secrets
        }
        String url = Display.getInstance().getProperty(PROP_ENDPOINT, DEFAULT_ENDPOINT);
        if (url == null || url.length() == 0) {
            return null;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            field(sb, "buildKey", key);
            sb.append(',');
            field(sb, "packageName", Display.getInstance().getProperty(PROP_PACKAGE, ""));
            sb.append(',');
            field(sb, "name", name);
            sb.append('}');
            Response resp = Rest.post(url + "/fetch").jsonContent().body(sb.toString()).getAsJsonMap();
            if (resp.getResponseCode() < 200 || resp.getResponseCode() >= 300) {
                return null;
            }
            Object data = resp.getResponseData();
            if (data instanceof Map) {
                Object val = ((Map) data).get("value");
                return val == null ? null : val.toString();
            }
        } catch (Throwable t) {
            Log.p("Secrets.fetch failed: " + t);
        }
        return null;
    }

    private static void field(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append('"').append(':');
        if (value == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append('"');
    }
}
