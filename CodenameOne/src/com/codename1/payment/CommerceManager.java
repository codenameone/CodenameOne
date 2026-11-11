/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.payment;

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.io.NetworkEvent;
import com.codename1.io.rest.ErrorCodeHandler;
import com.codename1.io.rest.RequestBuilder;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.Rest;
import com.codename1.security.Ed25519;
import com.codename1.security.SecureRandom;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/// Client facade for the Codename One Commerce service. It does not replace
/// the {@link Purchase} API -- it wraps it: purchases still go through the
/// platform store, and this class adds server-side validation and a
/// store-agnostic <em>entitlement</em> check ("does this user have pro?")
/// instead of per-SKU receipt juggling.
///
/// <p>Typical use:
/// <pre>
///   CommerceManager cm = CommerceManager.getInstance();
///   cm.setAppUserId(myAccountId);          // optional; stable per user
///   cm.subscribe("pro_monthly");           // delegates to Purchase
///   // later, off the EDT:
///   cm.refresh();                          // validate receipts with the cloud
///   if (cm.isEntitled("pro")) { ... }      // entitlement, not SKU
/// </pre>
///
/// <p><b>Graceful degradation.</b> When the cloud is unreachable, or the
/// developer's account is over its monthly validated-volume cap, the cloud
/// stops validating and {@link #isDegraded()} becomes true. Entitlement checks
/// then fall back to the store-direct signal (the platform's own receipt), so
/// a paying user is never locked out -- exactly the server-side behaviour,
/// mirrored on the device.
///
/// <p>The cloud endpoint, build key and package are stamped into the build by
/// the Codename One build server when commerce is enabled
/// ({@code codename1.arg.commerce.cloud.enabled=true}); when they are absent
/// this class is inert and every call is a safe no-op that defers to
/// {@link Purchase}.
public final class CommerceManager {

    private static final String PROP_ENABLED = "commerce.cloud.enabled";
    private static final String PROP_ENDPOINT = "commerce.cloud.endpoint";
    private static final String DEFAULT_ENDPOINT = "https://cloud.codenameone.com/api/v2/commerce";

    // Standard build-stamped properties, shared with crash/analytics: every
    // cloud build bakes these, so commerce needs no build-daemon wiring of its
    // own (it works off the same build_key the analytics provider sends).
    private static final String PROP_BUILD_KEY = "build_key";
    private static final String PROP_PACKAGE = "package_name";

    private static final String PREF_APP_USER_ID = "cn1$commerce$appUserId";

    // Display property holding the service's Ed25519 public key, base64 of
    // the raw 32 bytes. It wins over DEFAULT_PUBLIC_KEY, so a self-hosted or
    // staging service can be verified with its own key.
    private static final String PROP_PUBLIC_KEY = "commerce.cloud.publicKey";

    // The production key of the Codename One Commerce service. Empty until it
    // is provisioned: while no key is known at all, unsigned answers are
    // accepted, exactly as /validate answers always were.
    private static final String DEFAULT_PUBLIC_KEY = "";

    // Storage entry holding the last accepted customer payload, one per app
    // user id.
    private static final String STORAGE_CUSTOMER_PREFIX = "CN1CommerceCustomer_";

    // Storage entry holding the last accepted offerings payload, one per app
    // user id and placement.
    private static final String STORAGE_OFFERINGS_PREFIX = "CN1CommerceOfferings_";

    private static final String HEX = "0123456789abcdef";

    // Seeded with the time, and with an object's identity so two installs
    // started in the same millisecond still draw different ids.
    private static final Random RANDOM =
            new Random(System.currentTimeMillis() ^ ((long) new Object().hashCode() << 32));

    private static CommerceManager instance;

    private final Map entitlementCache = new HashMap();
    private final EventDispatcher customerInfoListeners = new EventDispatcher();
    // Store products by SKU, as the store last listed them.
    private final Map productCache = new HashMap();
    // Paywall events not sent yet, each a JSON object as text. Touched on the
    // EDT only.
    private final List pendingEvents = new ArrayList();
    private boolean eventFlushScheduled;
    private boolean degraded;
    private String appUserId;

    // The last customer the cloud answered with, or the stored one.
    private CustomerInfo customerInfo = new CustomerInfo(null);
    // The app user id customerInfo was loaded for; null before first use.
    private String customerLoadedFor;

    private CommerceManager() {
        appUserId = Preferences.get(PREF_APP_USER_ID, null);
    }

    /// The shared instance.
    public static synchronized CommerceManager getInstance() {
        if (instance == null) {
            instance = new CommerceManager();
        }
        return instance;
    }

    /// Whether cloud commerce is active in this build. True for any cloud build
    /// (one that carries a {@code build_key}) unless the developer explicitly
    /// disabled it with {@code codename1.commerce.cloud.enabled=false}. In the
    /// simulator / a local build there is no build key, so this is false and
    /// every call safely defers to {@link Purchase}.
    public boolean isCloudEnabled() {
        if ("false".equalsIgnoreCase(Display.getInstance().getProperty(PROP_ENABLED, "true"))) {
            return false;
        }
        String key = Display.getInstance().getProperty(PROP_BUILD_KEY, "");
        return key != null && key.length() > 0 && endpoint() != null;
    }

    /// True after a {@link #refresh()} that could not get a server-validated
    /// answer -- the cloud was unreachable or the account is over its monthly
    /// cap. Entitlement checks fall back to the store-direct signal while this
    /// is true.
    public boolean isDegraded() {
        return degraded;
    }

    /// Sets the stable per-user id the cloud keys entitlements on. Pass your
    /// own account id once the user signs in; if never set, an anonymous id is
    /// generated and persisted so entitlements survive restarts on the device.
    public void setAppUserId(String id) {
        appUserId = id;
        if (id != null) {
            Preferences.set(PREF_APP_USER_ID, id);
        }
    }

    /// The current (explicit or generated-anonymous) app user id.
    public String getAppUserId() {
        if (appUserId == null) {
            // The "anon-" prefix is a contract with the service: grants of an
            // id that starts with it always move to the account the user
            // later logs in to. The random part keeps two installs made in
            // the same millisecond apart, which the identity hash of this
            // object -- the same on every launch of some VMs -- did not.
            appUserId = "anon-" + Long.toString(System.currentTimeMillis(), 36)
                    + "-" + Long.toString(RANDOM.nextLong() & Long.MAX_VALUE, 36);
            Preferences.set(PREF_APP_USER_ID, appUserId);
        }
        return appUserId;
    }

    // ---- identity --------------------------------------------------------------

    /// Tells the cloud that the current customer is now known as
    /// `newAppUserId`, typically when the user signs in to the application's
    /// own account. Purchases made under an anonymous id move to the account,
    /// so a subscription bought before signing in, or on another platform
    /// under the same account, unlocks here.
    ///
    /// Blocking network call -- invoke off the EDT. The id is switched only
    /// when the cloud accepted the change; on failure the manager is
    /// [degraded][#isDegraded()], the previous id stays, and the call can be
    /// repeated. When the cloud is not enabled in this build the id is
    /// switched locally and nothing else happens.
    ///
    /// #### Parameters
    ///
    /// - `newAppUserId`: the application's stable id for the user
    ///
    /// #### Returns
    ///
    /// true if the id was switched
    public boolean logIn(String newAppUserId) {
        if (newAppUserId == null || newAppUserId.length() == 0) {
            return false;
        }
        if (!isCloudEnabled()) {
            setAppUserId(newAppUserId);
            return true;
        }
        ensureCustomerLoaded();
        String previous = getAppUserId();
        if (newAppUserId.equals(previous)) {
            return fetchCustomer(false);
        }
        StringBuilder extra = new StringBuilder();
        appendField(extra, "newAppUserId", newAppUserId);
        Map payload = postSigned("/identify", extra.toString());
        if (payload == null || !accepted(payload)) {
            degraded = true;
            return false;
        }
        deleteStoredCustomer(previous);
        setAppUserId(newAppUserId);
        customerLoadedFor = newAppUserId;
        adoptCustomer(payload, true);
        degraded = asBoolean(payload.get("degraded"));
        return true;
    }

    /// Forgets the current customer: a new anonymous id is generated and the
    /// customer information becomes empty, so the next user of the device
    /// does not inherit the previous one's entitlements. Listeners are told.
    /// Nothing is sent to the cloud; call [#refresh()] afterwards to pick up
    /// what the store account on the device still owns.
    public void logOut() {
        if (appUserId != null) {
            deleteStoredCustomer(appUserId);
        }
        appUserId = null;
        customerLoadedFor = getAppUserId();
        customerInfo = new CustomerInfo(null);
        entitlementCache.clear();
        degraded = false;
        fireCustomerInfoChanged();
    }

    /// Attaches attributes to the current customer in the cloud, where
    /// targeting rules and the console can read them. A null value deletes
    /// the attribute; any other value is sent as its string form.
    ///
    /// Blocking network call -- invoke off the EDT. Safe no-op when the cloud
    /// is not enabled in this build.
    ///
    /// #### Parameters
    ///
    /// - `attributes`: attribute names to values
    ///
    /// #### Returns
    ///
    /// true if the cloud accepted the attributes
    public boolean setAttributes(Map attributes) {
        if (!isCloudEnabled() || attributes == null) {
            return false;
        }
        String url = endpoint();
        StringBuilder extra = new StringBuilder();
        extra.append("\"attributes\":{");
        boolean first = true;
        for (Object o : attributes.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            if (e.getKey() == null) {
                continue;
            }
            if (!first) {
                extra.append(',');
            }
            first = false;
            Object v = e.getValue();
            appendField(extra, String.valueOf(e.getKey()), v == null ? null : v.toString());
        }
        extra.append('}');
        try {
            Response resp = quiet(Rest.post(url + "/attributes"))
                    .body(baseRequest(newNonce(), extra.toString())).getAsString();
            return resp.getResponseCode() >= 200 && resp.getResponseCode() < 300;
        } catch (Throwable t) {
            Log.p("CommerceManager.setAttributes failed: " + t);
            return false;
        }
    }

    /// Asks the store to restore the purchases of the store account on this
    /// device, when the platform supports that. The restored receipts reach
    /// the cloud through the receipt synchronization that follows, which
    /// refreshes this manager by itself.
    public void restore() {
        Purchase p = Purchase.getInAppPurchase();
        if (p != null && p.isRestoreSupported()) {
            p.restore();
        }
    }

    // ---- customer information --------------------------------------------------

    /// The last customer information the cloud answered with. Never null:
    /// before any answer it is an empty customer.
    ///
    /// The last accepted answer is stored on the device, so this answers
    /// after a cold start and with no network. A stored entitlement is
    /// reported exactly as it was stored, even when its expiry date has
    /// passed by the device's clock: the device clock is the user's to set,
    /// so only the service's clock decides when an entitlement ends, and it
    /// does so on the next [#refresh()].
    public CustomerInfo getCustomerInfo() {
        ensureCustomerLoaded();
        return customerInfo;
    }

    /// Adds a listener that is called on the EDT whenever the customer
    /// information changes. The event's source is the new [CustomerInfo].
    public void addCustomerInfoListener(ActionListener l) {
        customerInfoListeners.addListener(l);
    }

    /// Removes a listener added with [#addCustomerInfoListener(ActionListener)].
    public void removeCustomerInfoListener(ActionListener l) {
        customerInfoListeners.removeListener(l);
    }

    private void fireCustomerInfoChanged() {
        if (customerInfoListeners.hasListeners()) {
            // The dispatcher moves the call to the EDT when this is another
            // thread, which it normally is: refresh() blocks on the network.
            customerInfoListeners.fireActionEvent(new ActionEvent(customerInfo));
        }
    }

    /// Called by [Purchase] when a receipt synchronization completed
    /// successfully: the stores' receipts have changed, so the cloud is
    /// asked again. The refresh blocks on the network and therefore runs on
    /// a background thread, never on the EDT this is called from.
    static void receiptsSynchronized() {
        final CommerceManager cm = getInstance();
        if (!cm.isCloudEnabled()) {
            return;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                cm.refresh();
            }
        });
    }

    /// Drops the shared instance so a test starts from a cold one.
    static synchronized void resetForTests() {
        instance = null;
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

    /// Whether the user currently holds {@code entitlementId}. Uses the last
    /// cloud-validated answer when available; otherwise (cloud disabled or
    /// degraded) falls back to the store-direct signal, treating the
    /// entitlement id as a subscription SKU so a paying user keeps access.
    public boolean isEntitled(String entitlementId) {
        if (entitlementId == null) {
            return false;
        }
        ensureCustomerLoaded();
        // A receipt validated after the last customer answer is newer than
        // that answer; an accepted customer answer empties this cache.
        Object cached = entitlementCache.get(entitlementId);
        if (cached instanceof Boolean) {
            return ((Boolean) cached).booleanValue();
        }
        EntitlementInfo known = customerInfo.getEntitlement(entitlementId);
        if (known != null) {
            return known.isActive();
        }
        // Store-direct fallback: the platform's own receipt is authoritative
        // enough to keep a paying user unlocked when we have no cloud answer.
        try {
            return Purchase.getInAppPurchase().isSubscribed(entitlementId);
        } catch (Throwable t) {
            return false;
        }
    }

    /// Snapshot of the entitlement ids currently considered active.
    public List getActiveEntitlements() {
        ensureCustomerLoaded();
        List out = new ArrayList();
        for (Object o : entitlementCache.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Object v = e.getValue();
            if (v instanceof Boolean && ((Boolean) v).booleanValue()) {
                out.add(e.getKey());
            }
        }
        for (EntitlementInfo e : customerInfo.getActiveEntitlements()) {
            if (!entitlementCache.containsKey(e.getId())) {
                out.add(e.getId());
            }
        }
        return out;
    }

    // ---- offerings ----------------------------------------------------------------

    /// The offerings the cloud configured for this customer. Same as
    /// [#getOfferings(String)] with no placement.
    ///
    /// #### Returns
    ///
    /// the offerings; never null, and empty when there is nothing to answer
    /// from
    public Offerings getOfferings() {
        return getOfferings(null);
    }

    /// The offerings the cloud configured for this customer at a placement,
    /// a named place in the application (`"onboarding"`, `"settings"`) that
    /// the console can give an offering of its own.
    ///
    /// Blocking network call -- invoke off the EDT. The last accepted answer
    /// is stored on the device and is what this returns when the cloud
    /// cannot be reached. After a fresh answer the store is asked for the
    /// products of the packages, when the platform lists products, and each
    /// is attached to its [CommercePackage].
    ///
    /// #### Parameters
    ///
    /// - `placement`: the placement, or null for the default offerings
    ///
    /// #### Returns
    ///
    /// the offerings; never null, and empty when the cloud is not enabled in
    /// this build or there is nothing to answer from
    public Offerings getOfferings(String placement) {
        if (!isCloudEnabled()) {
            return new Offerings(null);
        }
        String key = storageKey(STORAGE_OFFERINGS_PREFIX,
                getAppUserId() + "/" + (placement == null ? "" : placement));
        Display d = Display.getInstance();
        StringBuilder extra = new StringBuilder();
        appendField(extra, "platform", d.getPlatformName());
        String language = null;
        String country = null;
        try {
            language = d.getLocalizationManager().getLanguage();
            country = d.getLocalizationManager().getLocale();
        } catch (Throwable t) {
            // a port with no localization answers the default offerings
        }
        extra.append(',');
        appendField(extra, "country", country);
        extra.append(',');
        appendField(extra, "appVersion", d.getProperty("AppVersion", null));
        extra.append(',');
        String locale = language;
        if (language != null && country != null && country.length() > 0) {
            locale = language + "_" + country;
        }
        appendField(extra, "locale", locale);
        if (placement != null) {
            extra.append(',');
            appendField(extra, "placement", placement);
        }
        Map payload = postSigned("/offerings", extra.toString());
        Offerings result;
        if (payload != null && accepted(payload)) {
            result = new Offerings(payload);
            try {
                Storage.getInstance().writeObject(key, JSONParser.mapToJson(result.toMap()));
            } catch (Throwable t) {
                Log.p("CommerceManager: could not store the offerings: " + t);
            }
            fetchProducts(result);
        } else {
            result = new Offerings(null);
            try {
                Object stored = Storage.getInstance().readObject(key);
                if (stored instanceof String && ((String) stored).length() > 0) {
                    result = new Offerings(JSONParser.parseJSON((String) stored));
                }
            } catch (Throwable t) {
                Log.p("CommerceManager: could not read the stored offerings: " + t);
            }
        }
        attachProducts(result);
        return result;
    }

    /// Asks the store for the products of every package, when the platform
    /// lists products. A store that fails to answer leaves the packages
    /// without a product, and they are then shown with their own text.
    private void fetchProducts(Offerings offerings) {
        List skus = new ArrayList();
        for (Offering o : offerings.getAll()) {
            for (CommercePackage p : o.getPackages()) {
                if (p.getProductId() != null && !skus.contains(p.getProductId())) {
                    skus.add(p.getProductId());
                }
            }
        }
        if (skus.isEmpty()) {
            return;
        }
        try {
            Purchase store = Purchase.getInAppPurchase();
            if (store == null || !store.isItemListingSupported()) {
                return;
            }
            String[] arr = new String[skus.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = String.valueOf(skus.get(i));
            }
            // A store that answers null lands in the catch below, like any
            // other store failure.
            for (Product p : store.getProducts(arr)) {
                if (p != null && p.getSku() != null) {
                    productCache.put(p.getSku(), p);
                }
            }
        } catch (Throwable t) {
            Log.p("CommerceManager: the store did not list the products: " + t);
        }
    }

    private void attachProducts(Offerings offerings) {
        for (Offering o : offerings.getAll()) {
            for (CommercePackage p : o.getPackages()) {
                Object product = productCache.get(p.getProductId());
                if (product instanceof Product) {
                    p.setProduct((Product) product);
                }
            }
        }
    }

    /// Buys a package through the platform store: a subscription for every
    /// package that [is one][CommercePackage#isSubscription()], a one time
    /// purchase otherwise. The outcome arrives the way every store purchase
    /// does, and through the customer information listeners once the cloud
    /// validated it.
    ///
    /// #### Parameters
    ///
    /// - `pkg`: the package to buy; null or a package with no product does
    ///   nothing
    public void purchase(CommercePackage pkg) {
        if (pkg == null || pkg.getProductId() == null) {
            return;
        }
        if (pkg.isSubscription()) {
            subscribe(pkg.getProductId());
        } else {
            purchase(pkg.getProductId());
        }
    }

    /// The page where the customer manages the subscription with the store,
    /// from the last [customer information][#getCustomerInfo()], or null
    /// when there is none.
    public String getManagementUrl() {
        return getCustomerInfo().getManagementUrl();
    }

    /// The customer center the console configured for this application, as
    /// parsed JSON: the `paths` to offer, the support address and the cancel
    /// reasons. [com.codename1.payment.center.CustomerCenter] presents it.
    ///
    /// Blocking network call -- invoke off the EDT.
    ///
    /// #### Returns
    ///
    /// the configuration, or null when the cloud is not enabled in this
    /// build, could not be reached or refused to answer
    public Map getCustomerCenterConfiguration() {
        if (!isCloudEnabled()) {
            return null;
        }
        Map payload = postSigned("/customer-center", null);
        if (payload == null || !asBoolean(payload.get("accepted"))) {
            return null;
        }
        return payload;
    }

    // ---- events --------------------------------------------------------------------

    /// Reports a paywall event to the cloud, where the console charts the
    /// funnel of each offering and paywall. Same as
    /// [#logEvent(String, String, String, String, String)] with no reason.
    ///
    /// #### Parameters
    ///
    /// - `type`: the event, such as `IMPRESSION`, `PURCHASE_START`,
    ///   `PURCHASE` or `CLOSE`
    ///
    /// - `offeringId`: the offering shown, or null
    ///
    /// - `paywallId`: the paywall shown, or null
    ///
    /// - `packageId`: the package concerned, or null
    public void logEvent(String type, String offeringId, String paywallId, String packageId) {
        logEvent(type, offeringId, paywallId, packageId, null);
    }

    /// Reports an event to the cloud. The call returns at once on any
    /// thread: events are collected and sent together from a background
    /// thread, and an event that cannot be sent is dropped. Reporting never
    /// delays or fails a purchase. Safe no-op when the cloud is not enabled
    /// in this build.
    ///
    /// #### Parameters
    ///
    /// - `type`: the event, such as `IMPRESSION` or `CANCEL_REASON`
    ///
    /// - `offeringId`: the offering shown, or null
    ///
    /// - `paywallId`: the paywall shown, or null
    ///
    /// - `packageId`: the package concerned, or null
    ///
    /// - `reason`: the id of the reason chosen, for a `CANCEL_REASON` event,
    ///   or null
    public void logEvent(String type, String offeringId, String paywallId, String packageId, String reason) {
        if (type == null || !isCloudEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendField(sb, "type", type);
        sb.append(',');
        appendField(sb, "offeringId", offeringId);
        sb.append(',');
        appendField(sb, "paywallId", paywallId);
        sb.append(',');
        appendField(sb, "packageId", packageId);
        if (reason != null) {
            sb.append(',');
            appendField(sb, "reason", reason);
        }
        sb.append(",\"time\":").append(System.currentTimeMillis()).append('}');
        final String event = sb.toString();
        // The queue belongs to the EDT, so no two threads ever touch it.
        if (Display.getInstance().isEdt()) {
            queueEvent(event);
        } else {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    queueEvent(event);
                }
            });
        }
    }

    private void queueEvent(String event) {
        pendingEvents.add(event);
        if (eventFlushScheduled) {
            return;
        }
        eventFlushScheduled = true;
        // Sent on the next EDT cycle, so the events of one user action --
        // a purchase start right after an impression -- travel together.
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                final String body = drainEvents();
                if (body == null) {
                    return;
                }
                Display.getInstance().scheduleBackgroundTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            quiet(Rest.post(endpoint() + "/events")).body(body).getAsString();
                        } catch (Throwable t) {
                            // analytics only: dropped
                        }
                    }
                });
            }
        });
    }

    /// Empties the event queue into the body of one `/events` request.
    ///
    /// #### Returns
    ///
    /// the request body, or null when nothing is waiting
    String drainEvents() {
        eventFlushScheduled = false;
        if (pendingEvents.isEmpty() || endpoint() == null) {
            pendingEvents.clear();
            return null;
        }
        StringBuilder events = new StringBuilder("\"events\":[");
        for (int i = 0; i < pendingEvents.size(); i++) {
            if (i > 0) {
                events.append(',');
            }
            events.append(pendingEvents.get(i));
        }
        events.append(']');
        pendingEvents.clear();
        return baseRequest(newNonce(), events.toString());
    }

    // ---- cloud refresh -------------------------------------------------------

    /// Validates the device's current receipts with the cloud, then asks the
    /// cloud what the customer is entitled to and takes that answer as the
    /// entitlement state. The customer is asked even when the device holds no
    /// receipt at all, which is what unlocks a purchase made on another
    /// platform or granted from the console.
    ///
    /// Blocking network call -- invoke off the EDT. Safe no-op when the cloud
    /// is not enabled in this build. On any failure the manager flips to
    /// {@link #isDegraded() degraded} and leaves the previous (or
    /// store-direct) answers in place.
    public void refresh() {
        if (!isCloudEnabled()) {
            return;
        }
        ensureCustomerLoaded();
        List receipts = null;
        boolean receiptsFailed = false;
        try {
            receipts = Purchase.getInAppPurchase().getReceipts();
        } catch (Throwable t) {
            receiptsFailed = true;
        }
        boolean anyReceipt = false;
        boolean anySuccess = false;
        boolean anyDegraded = false;
        if (receipts != null) {
            for (Object o : receipts) {
                if (!(o instanceof Receipt)) {
                    continue;
                }
                anyReceipt = true;
                Boolean[] flags = validateReceipt((Receipt) o);
                if (flags != null) {
                    anySuccess = true;
                    if (flags[0].booleanValue()) {
                        anyDegraded = true;
                    }
                }
            }
        }
        boolean customerOk = fetchCustomer(false);
        degraded = !customerOk || degraded || receiptsFailed || anyDegraded || (anyReceipt && !anySuccess);
    }

    /// Asks the cloud for the current customer and adopts the answer. Leaves
    /// `degraded` as the answer itself reported it, or true when there was no
    /// acceptable answer.
    private boolean fetchCustomer(boolean forceEvent) {
        Map payload = postSigned("/customer", null);
        if (payload == null || !accepted(payload)) {
            degraded = true;
            return false;
        }
        adoptCustomer(payload, forceEvent);
        degraded = asBoolean(payload.get("degraded"));
        return true;
    }

    /// An answer the cloud explicitly refused to give (`accepted` false) is
    /// not an entitlement state; one that does not say is taken as given.
    private static boolean accepted(Map payload) {
        Object a = payload.get("accepted");
        return a == null || asBoolean(a);
    }

    private void adoptCustomer(Map payload, boolean forceEvent) {
        CustomerInfo next = new CustomerInfo(payload);
        boolean changed = forceEvent || !next.sameState(customerInfo);
        customerInfo = next;
        // The customer answer is the entitlement state; what single receipts
        // said before it is superseded.
        entitlementCache.clear();
        try {
            Storage.getInstance().writeObject(storageKey(STORAGE_CUSTOMER_PREFIX, getAppUserId()), JSONParser.mapToJson(next.toMap()));
        } catch (Throwable t) {
            Log.p("CommerceManager: could not store the customer: " + t);
        }
        if (changed) {
            fireCustomerInfoChanged();
        }
    }

    /// Loads the stored customer of the current app user id the first time
    /// it is needed, and again after the id changed under it, so a cold
    /// start answers from the last accepted answer before any network call.
    ///
    /// Nothing is expired here. An entitlement whose `expiresDate` is behind
    /// the device clock is still reported as stored: the service's clock
    /// decides, at the next refresh, and the device's is not trusted to.
    private void ensureCustomerLoaded() {
        String id = getAppUserId();
        if (id.equals(customerLoadedFor)) {
            return;
        }
        boolean hadCustomer = customerLoadedFor != null;
        customerLoadedFor = id;
        customerInfo = new CustomerInfo(null);
        if (hadCustomer) {
            // Another user's receipts answered these.
            entitlementCache.clear();
        }
        try {
            Object stored = Storage.getInstance().readObject(storageKey(STORAGE_CUSTOMER_PREFIX, id));
            if (stored instanceof String && ((String) stored).length() > 0) {
                customerInfo = new CustomerInfo(JSONParser.parseJSON((String) stored));
            }
        } catch (Throwable t) {
            Log.p("CommerceManager: could not read the stored customer: " + t);
        }
    }

    private void deleteStoredCustomer(String id) {
        try {
            Storage.getInstance().deleteStorageFile(storageKey(STORAGE_CUSTOMER_PREFIX, id));
        } catch (Throwable t) {
            Log.p("CommerceManager: could not delete the stored customer: " + t);
        }
    }

    /// A storage entry name for an app user id. The id is the application's
    /// and may hold characters a file name cannot, so those are replaced and
    /// the id's hash keeps two ids that then look alike apart.
    private static String storageKey(String prefix, String id) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-';
            sb.append(safe ? c : '_');
        }
        sb.append('_').append(Integer.toHexString(id.hashCode()));
        return sb.toString();
    }

    // ---- signed envelope -------------------------------------------------------

    /// Posts a request to one of the endpoints that answer with a signed
    /// envelope, `{"payload": "<JSON text>", "signature": "<base64>"}`, and
    /// returns the parsed payload, or null when there is no answer to trust.
    ///
    /// The signature is Ed25519 over the UTF-8 bytes of the request's nonce,
    /// a line feed and the payload text, so an answer recorded earlier
    /// cannot be replayed to a later request. When a public key is known, an
    /// answer that is unsigned or wrongly signed is ignored. When none is
    /// known, an unsigned answer is accepted.
    private Map postSigned(String path, String extraFields) {
        String url = endpoint();
        if (url == null) {
            return null;
        }
        String nonce = newNonce();
        Object data = postForJson(url + path, baseRequest(nonce, extraFields));
        // The casts stay outside any try block: a failed cast does not throw
        // on every platform, so each type is tested and never caught.
        if (!(data instanceof Map)) {
            return null;
        }
        Map envelope = (Map) data;
        Object payload = envelope.get("payload");
        if (!(payload instanceof String)) {
            return null;
        }
        String text = (String) payload;
        byte[] key = publicKey();
        if (key != null) {
            Object signature = envelope.get("signature");
            if (!(signature instanceof String) || !verify(nonce, text, (String) signature, key)) {
                Log.p("CommerceManager: ignored an answer of " + path + " with a missing or bad signature");
                return null;
            }
        }
        try {
            return JSONParser.parseJSON(text);
        } catch (Throwable t) {
            Log.p("CommerceManager" + path + " answered an unreadable payload: " + t);
            return null;
        }
    }

    /// Posts a JSON body and returns the parsed JSON answer, or null for an
    /// error status or no answer at all.
    private static Object postForJson(String url, String body) {
        try {
            Response resp = quiet(Rest.post(url)).body(body).getAsJsonMap();
            if (resp.getResponseCode() < 200 || resp.getResponseCode() >= 300) {
                return null;
            }
            return resp.getResponseData();
        } catch (Throwable t) {
            Log.p("CommerceManager: " + url + " failed: " + t);
            return null;
        }
    }

    private static boolean verify(String nonce, String payload, String signature, byte[] key) {
        try {
            if (key.length != Ed25519.PUBLIC_KEY_LENGTH) {
                return false;
            }
            byte[] sig = Base64.decode(signature.getBytes("UTF-8"));
            if (sig == null || sig.length != Ed25519.SIGNATURE_LENGTH) {
                return false;
            }
            return Ed25519.verify((nonce + "\n" + payload).getBytes("UTF-8"), sig, key);
        } catch (Throwable t) {
            return false;
        }
    }

    /// The service's public key, or null when none is known. A key that is
    /// configured but unreadable is returned as an empty array, which no
    /// signature verifies against: a broken key must not quietly turn
    /// verification off.
    private static byte[] publicKey() {
        String k = Display.getInstance().getProperty(PROP_PUBLIC_KEY, DEFAULT_PUBLIC_KEY);
        if (k == null || k.length() == 0) {
            k = DEFAULT_PUBLIC_KEY;
        }
        if (k.length() == 0) {
            return null;
        }
        try {
            byte[] decoded = Base64.decode(k.getBytes("UTF-8"));
            return decoded == null ? new byte[0] : decoded;
        } catch (Throwable t) {
            return new byte[0];
        }
    }

    /// A JSON request that reports failure through its response and nothing
    /// else. Left alone, a request that meets an error status or a dead
    /// connection puts the framework's "Retry / Cancel" dialog on screen, and
    /// these run in the background where the user asked for nothing.
    private static RequestBuilder quiet(RequestBuilder request) {
        return request.jsonContent()
                .onErrorCodeString(new ErrorCodeHandler<String>() {
                    @Override
                    public void onError(Response<String> errorData) {
                        // the caller reads the response code
                    }
                })
                .onError(new ActionListener<NetworkEvent>() {
                    @Override
                    public void actionPerformed(NetworkEvent evt) {
                        // the caller sees no response, or the exception
                    }
                });
    }

    /// A fresh nonce for one request, 128 random bits in hex.
    private static String newNonce() {
        byte[] b;
        try {
            b = SecureRandom.bytes(16);
        } catch (Throwable t) {
            // A port with no secure source still gets a nonce that differs
            // per request, which is what tells answers apart.
            b = new byte[16];
            RANDOM.nextBytes(b);
        }
        StringBuilder sb = new StringBuilder(32);
        for (byte v : b) {
            sb.append(HEX.charAt((v >> 4) & 0xf));
            sb.append(HEX.charAt(v & 0xf));
        }
        return sb.toString();
    }

    /// The fields every request of the newer endpoints carries, plus the
    /// caller's own, as a JSON object.
    private String baseRequest(String nonce, String extraFields) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendField(sb, "buildKey", Display.getInstance().getProperty(PROP_BUILD_KEY, ""));
        sb.append(',');
        appendField(sb, "packageName", packageName());
        sb.append(',');
        appendField(sb, "appUserId", getAppUserId());
        sb.append(',');
        appendField(sb, "nonce", nonce);
        if (extraFields != null && extraFields.length() > 0) {
            sb.append(',').append(extraFields);
        }
        sb.append('}');
        return sb.toString();
    }

    /// Posts one receipt to the cloud /validate endpoint, folds the returned
    /// entitlements into the cache, and returns {degraded} (or null on
    /// failure).
    private Boolean[] validateReceipt(Receipt r) {
        String url = endpoint();
        if (url == null || r == null) {
            return null;
        }
        Object data = postForJson(url + "/validate", buildRequest(r));
        if (!(data instanceof Map)) {
            return null;
        }
        Map result = (Map) data;
        applyEntitlements(result.get("entitlements"));
        boolean deg = asBoolean(result.get("degraded"));
        return new Boolean[] { deg ? Boolean.TRUE : Boolean.FALSE };
    }

    private void applyEntitlements(Object entitlements) {
        if (!(entitlements instanceof Map)) {
            return;
        }
        Map ents = (Map) entitlements;
        for (Object o : ents.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Object view = e.getValue();
            boolean active = false;
            if (view instanceof Map) {
                active = asBoolean(((Map) view).get("active"));
            }
            entitlementCache.put(String.valueOf(e.getKey()), active ? Boolean.TRUE : Boolean.FALSE);
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
                default:
                    if (c < 0x20) {
                        // Any other control character is illegal raw in JSON.
                        sb.append('\\').append('u').append("00")
                                .append(HEX.charAt((c >> 4) & 0xf))
                                .append(HEX.charAt(c & 0xf));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
