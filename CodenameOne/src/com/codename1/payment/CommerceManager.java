/*
 * Codename One - cross-platform commerce (subscription/IAP validation) SDK.
 *
 * Optional client facade over the existing Purchase API that adds cloud-side
 * receipt validation and store-agnostic entitlement checks backed by the
 * Codename One Commerce service. The framework itself is free; this talks to
 * the paid cloud service when the app is built with it enabled.
 */
package com.codename1.payment;

import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.Rest;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client facade for the Codename One Commerce service. It does not replace
 * the {@link Purchase} API -- it wraps it: purchases still go through the
 * platform store, and this class adds server-side validation and a
 * store-agnostic <em>entitlement</em> check ("does this user have pro?")
 * instead of per-SKU receipt juggling.
 *
 * <p>Typical use:
 * <pre>
 *   CommerceManager cm = CommerceManager.getInstance();
 *   cm.setAppUserId(myAccountId);          // optional; stable per user
 *   cm.subscribe("pro_monthly");           // delegates to Purchase
 *   // later, off the EDT:
 *   cm.refresh();                          // validate receipts with the cloud
 *   if (cm.isEntitled("pro")) { ... }      // entitlement, not SKU
 * </pre>
 *
 * <p><b>Graceful degradation.</b> When the cloud is unreachable, or the
 * developer's account is over its monthly validated-volume cap, the cloud
 * stops validating and {@link #isDegraded()} becomes true. Entitlement checks
 * then fall back to the store-direct signal (the platform's own receipt), so
 * a paying user is never locked out -- exactly the server-side behaviour,
 * mirrored on the device.
 *
 * <p>The cloud endpoint, build key and package are stamped into the build by
 * the Codename One build server when commerce is enabled
 * ({@code codename1.arg.commerce.cloud.enabled=true}); when they are absent
 * this class is inert and every call is a safe no-op that defers to
 * {@link Purchase}.
 */
public class CommerceManager {

    private static final String PROP_ENABLED = "codename1.commerce.cloud.enabled";
    private static final String PROP_ENDPOINT = "codename1.commerce.cloud.endpoint";
    private static final String DEFAULT_ENDPOINT = "https://cloud.codenameone.com/api/v2/commerce";

    // Standard build-stamped properties, shared with crash/analytics: every
    // cloud build bakes these, so commerce needs no build-daemon wiring of its
    // own (it works off the same build_key the analytics provider sends).
    private static final String PROP_BUILD_KEY = "build_key";
    private static final String PROP_PACKAGE = "package_name";

    private static final String PREF_APP_USER_ID = "cn1$commerce$appUserId";

    private static CommerceManager instance;

    private final Map entitlementCache = new HashMap();
    private boolean degraded;
    private String appUserId;

    private CommerceManager() {
        appUserId = Preferences.get(PREF_APP_USER_ID, null);
    }

    /** The shared instance. */
    public static synchronized CommerceManager getInstance() {
        if (instance == null) {
            instance = new CommerceManager();
        }
        return instance;
    }

    /**
     * Whether cloud commerce is active in this build. True for any cloud build
     * (one that carries a {@code build_key}) unless the developer explicitly
     * disabled it with {@code codename1.commerce.cloud.enabled=false}. In the
     * simulator / a local build there is no build key, so this is false and
     * every call safely defers to {@link Purchase}.
     */
    public boolean isCloudEnabled() {
        if ("false".equalsIgnoreCase(Display.getInstance().getProperty(PROP_ENABLED, "true"))) {
            return false;
        }
        String key = Display.getInstance().getProperty(PROP_BUILD_KEY, "");
        return key != null && key.length() > 0 && endpoint() != null;
    }

    /**
     * True after a {@link #refresh()} that could not get a server-validated
     * answer -- the cloud was unreachable or the account is over its monthly
     * cap. Entitlement checks fall back to the store-direct signal while this
     * is true.
     */
    public boolean isDegraded() {
        return degraded;
    }

    /**
     * Sets the stable per-user id the cloud keys entitlements on. Pass your
     * own account id once the user signs in; if never set, an anonymous id is
     * generated and persisted so entitlements survive restarts on the device.
     */
    public void setAppUserId(String id) {
        appUserId = id;
        if (id != null) {
            Preferences.set(PREF_APP_USER_ID, id);
        }
    }

    /** The current (explicit or generated-anonymous) app user id. */
    public String getAppUserId() {
        if (appUserId == null) {
            appUserId = "anon-" + Long.toString(System.currentTimeMillis(), 36)
                    + "-" + Integer.toString(Math.abs(hashCode()), 36);
            Preferences.set(PREF_APP_USER_ID, appUserId);
        }
        return appUserId;
    }

    // ---- purchase delegation (thin pass-through to Purchase) ----------------

    public void purchase(String sku) {
        Purchase.getInAppPurchase().purchase(sku);
    }

    public void subscribe(String sku) {
        Purchase.getInAppPurchase().subscribe(sku);
    }

    public void unsubscribe(String sku) {
        Purchase.getInAppPurchase().unsubscribe(sku);
    }

    // ---- entitlement checks --------------------------------------------------

    /**
     * Whether the user currently holds {@code entitlementId}. Uses the last
     * cloud-validated answer when available; otherwise (cloud disabled or
     * degraded) falls back to the store-direct signal, treating the
     * entitlement id as a subscription SKU so a paying user keeps access.
     */
    public boolean isEntitled(String entitlementId) {
        if (entitlementId == null) {
            return false;
        }
        Object cached = entitlementCache.get(entitlementId);
        if (cached instanceof Boolean) {
            return ((Boolean) cached).booleanValue();
        }
        // Store-direct fallback: the platform's own receipt is authoritative
        // enough to keep a paying user unlocked when we have no cloud answer.
        try {
            return Purchase.getInAppPurchase().isSubscribed(entitlementId);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Snapshot of the entitlement ids currently considered active. */
    public List getActiveEntitlements() {
        List out = new ArrayList();
        for (Object key : entitlementCache.keySet()) {
            Object v = entitlementCache.get(key);
            if (v instanceof Boolean && ((Boolean) v).booleanValue()) {
                out.add(key);
            }
        }
        return out;
    }

    // ---- cloud refresh -------------------------------------------------------

    /**
     * Validates the device's current receipts with the cloud and updates the
     * entitlement cache. Blocking network call -- invoke off the EDT. Safe
     * no-op when the cloud is not enabled in this build. On any failure the
     * manager flips to {@link #isDegraded() degraded} and leaves the previous
     * (or store-direct) answers in place.
     */
    public void refresh() {
        if (!isCloudEnabled()) {
            return;
        }
        List receipts;
        try {
            receipts = Purchase.getInAppPurchase().getReceipts();
        } catch (Throwable t) {
            degraded = true;
            return;
        }
        if (receipts == null || receipts.isEmpty()) {
            return;
        }
        boolean anySuccess = false;
        boolean anyDegraded = false;
        for (int i = 0; i < receipts.size(); i++) {
            Receipt r = (Receipt) receipts.get(i);
            Boolean[] flags = validateReceipt(r);
            if (flags != null) {
                anySuccess = true;
                if (flags[0].booleanValue()) {
                    anyDegraded = true;
                }
            }
        }
        degraded = anyDegraded || !anySuccess;
    }

    /**
     * Posts one receipt to the cloud /validate endpoint, folds the returned
     * entitlements into the cache, and returns {degraded} (or null on
     * failure).
     */
    private Boolean[] validateReceipt(Receipt r) {
        String url = endpoint();
        if (url == null || r == null) {
            return null;
        }
        String body = buildRequest(r);
        try {
            Response resp = Rest.post(url + "/validate").jsonContent().body(body).getAsJsonMap();
            if (resp == null || resp.getResponseCode() < 200 || resp.getResponseCode() >= 300) {
                return null;
            }
            Object data = resp.getResponseData();
            if (!(data instanceof Map)) {
                return null;
            }
            Map result = (Map) data;
            applyEntitlements(result.get("entitlements"));
            boolean deg = asBoolean(result.get("degraded"));
            return new Boolean[] { deg ? Boolean.TRUE : Boolean.FALSE };
        } catch (Throwable t) {
            Log.p("CommerceManager.validateReceipt failed: " + t);
            return null;
        }
    }

    private void applyEntitlements(Object entitlements) {
        if (!(entitlements instanceof Map)) {
            return;
        }
        Map ents = (Map) entitlements;
        for (Object key : ents.keySet()) {
            Object view = ents.get(key);
            boolean active = false;
            if (view instanceof Map) {
                active = asBoolean(((Map) view).get("active"));
            }
            entitlementCache.put(String.valueOf(key), active ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    private String buildRequest(Receipt r) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendField(sb, "buildKey", Display.getInstance().getProperty(PROP_BUILD_KEY, ""));
        sb.append(',');
        appendField(sb, "packageName", packageName());
        sb.append(',');
        appendField(sb, "appUserId", getAppUserId());
        sb.append(',');
        appendField(sb, "store", storeOf(r));
        sb.append(',');
        // On StoreKit 2 the order data carries the signed transaction JWS; the
        // server decodes/normalizes it. Other stores send their own receipt.
        appendField(sb, "signedTransaction", r.getOrderData());
        sb.append(',');
        appendField(sb, "productId", r.getSku());
        sb.append('}');
        return sb.toString();
    }

    private static String storeOf(Receipt r) {
        String code = r.getStoreCode();
        if (Receipt.STORE_CODE_ITUNES.equals(code)) {
            return "apple";
        }
        if (Receipt.STORE_CODE_PLAY.equals(code)) {
            return "google";
        }
        return code == null ? "" : code;
    }

    private String packageName() {
        return Display.getInstance().getProperty(PROP_PACKAGE, "");
    }

    private String endpoint() {
        String e = Display.getInstance().getProperty(PROP_ENDPOINT, DEFAULT_ENDPOINT);
        return e == null || e.length() == 0 ? null : e;
    }

    private static boolean asBoolean(Object o) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        return o != null && "true".equalsIgnoreCase(o.toString());
    }

    private static void appendField(StringBuilder sb, String key, String value) {
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
