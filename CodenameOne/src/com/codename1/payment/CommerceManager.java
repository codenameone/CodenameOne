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

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.ConsentMode;
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

    // The production key of the Codename One Commerce service, base64 of the
    // raw 32 byte Ed25519 key. This constant is the one place it goes. While
    // it is empty and no PROP_PUBLIC_KEY is configured, no signed-envelope
    // answer of the cloud is trusted: they are ignored, nothing is cached,
    // the manager reports itself degraded and answers from the store
    // directly. An unsigned answer is never taken, since anything between
    // the device and the service could have written it.
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

    /// The category of every analytics event commerce emits; the one
    /// [Purchase] uses for `purchase` and `purchase_initiated`.
    private static final String ANALYTICS_CATEGORY = "commerce";
    /// The experiment and variant last reported as an exposure.
    private static final String PREF_EXPOSURE = "cn1$commerce$experimentExposure";
    /// How long the context of a started purchase waits for its receipt. A
    /// receipt of the same product after this is taken to be another
    /// purchase and is not labelled.
    private static final long PURCHASE_CONTEXT_LIFETIME = 10L * 60L * 1000L;
    private static final Runnable INVITE_OBSERVER = new Runnable() {
        @Override
        public void run() {
            getInstance().inviteArrived();
        }
    };

    private static CommerceManager instance;
    // Offering id to {placement, experiment, variant} of the last offerings
    // answer that carried it. Replaced as a whole, never changed in place.
    private Map offeringContext = new HashMap();
    private PurchaseContext pendingPurchase;
    private boolean invitesHooked;
    private boolean hookingInvites;
    private boolean refreshing;

    private final Map entitlementCache = new HashMap();
    private final EventDispatcher customerInfoListeners = new EventDispatcher();
    // Store products by SKU, as the store last listed them.
    private final Map productCache = new HashMap();
    // Paywall events not sent yet, each a JSON object as text. Touched on the
    // EDT only.
    private final List pendingEvents = new ArrayList();
    private boolean eventFlushScheduled;
    private boolean degraded;

    // Set once the missing public key was logged, so it is said once.
    private boolean noKeyLogged;

    // The envelope of the last answer postSigned verified, as the JSON text
    // that is cached: the cache keeps what was signed, never a parsed copy,
    // and verifies it again when it is loaded.
    private String lastEnvelope;
    // The billing portal last fetched, and the customer it was fetched for.
    private String portalUrl;
    private String portalUser;
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
    /// This id is commerce's own. It is independent of the user id of
    /// `Analytics.setUserId`, which stays whatever the application set:
    /// logging in or out here never changes it, and an application that
    /// wants its analytics under the same id sets it there as well.
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
        Analytics.setAttributionObserver(null);
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
        hookInvites();
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
                storeEnvelope(key);
            } catch (Throwable t) {
                Log.p("CommerceManager: could not store the offerings: " + t);
            }
            fetchProducts(result);
        } else {
            result = new Offerings(null);
            Map stored = loadEnvelope(key);
            if (stored != null) {
                result = new Offerings(stored);
            }
        }
        attachProducts(result);
        rememberContext(result, placement);
        reportExposure(result);
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
        if (pkg == null) {
            return;
        }
        if (isWebPurchase(pkg)) {
            purchaseOnWeb(pkg);
            return;
        }
        if (pkg.getProductId() == null) {
            return;
        }
        // Read by Purchase when it reports purchase_initiated and purchase,
        // so those events say which offering and package were bought.
        pendingPurchase = new PurchaseContext(pkg.getProductId(),
                contextParams(pkg.getOfferingId(), pkg.getId(), pkg.getProductId()));
        if (pkg.isSubscription()) {
            subscribe(pkg.getProductId());
        } else {
            purchase(pkg.getProductId());
        }
    }

    /// Whether [#purchase(CommercePackage)] sells a package through a web
    /// checkout: the package has no product of this platform's store, and
    /// the platform has no store to sell one -- the browser and the desktop,
    /// where [Purchase] supports neither managed payments nor subscriptions.
    ///
    /// #### Parameters
    ///
    /// - `pkg`: the package asked about
    ///
    /// #### Returns
    ///
    /// true when buying the package opens a hosted checkout page
    public boolean isWebPurchase(CommercePackage pkg) {
        if (pkg == null || !isCloudEnabled()) {
            return false;
        }
        String productId = pkg.getProductId();
        if (productId != null && productId.length() > 0) {
            return false;
        }
        try {
            Purchase store = Purchase.getInAppPurchase();
            return store == null || !(store.isManagedPaymentSupported() || store.isSubscriptionSupported());
        } catch (Throwable t) {
            // a port with no store object at all has no native store
            return true;
        }
    }

    /// Asks the cloud for a hosted checkout page that sells a package for
    /// its web price.
    ///
    /// Blocking network call -- invoke off the EDT.
    ///
    /// #### Parameters
    ///
    /// - `pkg`: the package to sell
    ///
    /// - `successUrl`: where the checkout sends the customer after paying,
    ///   or null for the service's own page
    ///
    /// - `cancelUrl`: where the checkout sends a customer who gave up, or
    ///   null for the service's own page
    ///
    /// #### Returns
    ///
    /// the address of the checkout page, or null when the cloud is not
    /// enabled in this build, could not be reached, or has no web price for
    /// the package
    public String getWebPurchaseUrl(CommercePackage pkg, String successUrl, String cancelUrl) {
        if (pkg == null || !isCloudEnabled()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        appendField(sb, "offeringId", pkg.getOfferingId());
        sb.append(',');
        appendField(sb, "packageId", pkg.getId());
        if (successUrl != null) {
            sb.append(',');
            appendField(sb, "successUrl", successUrl);
        }
        if (cancelUrl != null) {
            sb.append(',');
            appendField(sb, "cancelUrl", cancelUrl);
        }
        return urlOf(postSigned("/web-purchase", sb.toString()));
    }

    /// Sells a package through a hosted checkout page, opened with
    /// [Display#execute(String)]. The customer pays outside the
    /// application, so nothing reports the outcome here: once the
    /// application is back in the foreground the caller runs [#refresh()]
    /// off the EDT, and the customer information listeners hear what was
    /// bought. [com.codename1.payment.paywall.PaywallView] does that itself.
    ///
    /// May be called on the EDT, which keeps dispatching events while the
    /// cloud is asked for the page.
    ///
    /// #### Parameters
    ///
    /// - `pkg`: the package to sell
    ///
    /// #### Returns
    ///
    /// true when a checkout page was opened, false when there was none
    public boolean purchaseOnWeb(final CommercePackage pkg) {
        final String[] url = new String[1];
        blocking(new Runnable() {
            @Override
            public void run() {
                url[0] = getWebPurchaseUrl(pkg, null, null);
            }
        });
        if (url[0] == null) {
            return false;
        }
        Display.getInstance().execute(url[0]);
        if (pkg != null) {
            // Purchase has no part in a web checkout, so its start is
            // reported here, under the same name and category.
            try {
                Map<String, Object> params = contextParams(pkg.getOfferingId(), pkg.getId(), pkg.getProductId());
                params.put("store", "web");
                if (pkg.getProductId() != null) {
                    params.put("sku", pkg.getProductId());
                }
                Analytics.autoEvent("purchase_initiated", ANALYTICS_CATEGORY, params);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
        return true;
    }

    /// Buys a subscription under one of its App Store promotional offers.
    /// The cloud signs the offer for this customer and the store is then
    /// asked to sell the product with that signature, as
    /// [Purchase#purchase(String, PromotionalOffer)] does.
    ///
    /// May be called on the EDT, which keeps dispatching events while the
    /// cloud is asked for the signature.
    ///
    /// #### Parameters
    ///
    /// - `productId`: the subscription product
    ///
    /// - `offerId`: the promotional offer of that product, as App Store
    ///   Connect names it
    ///
    /// #### Returns
    ///
    /// true when the store was asked to sell the offer; false when no
    /// signature is available -- the platform's store has no promotional
    /// offers, the cloud is not enabled or it refused -- in which case
    /// nothing was bought
    public boolean purchaseWithPromotionalOffer(final String productId, final String offerId) {
        if (productId == null || offerId == null || !isCloudEnabled() || !promotionalOffersSupported()) {
            return false;
        }
        final ApplePromotionalOffer[] offer = new ApplePromotionalOffer[1];
        blocking(new Runnable() {
            @Override
            public void run() {
                offer[0] = fetchPromotionalOffer(productId, offerId);
            }
        });
        if (offer[0] == null) {
            return false;
        }
        try {
            Purchase.getInAppPurchase().purchase(productId, offer[0]);
        } catch (RuntimeException e) {
            // thrown by a store that does not sell promotional offers
            Log.p("CommerceManager: the store refused the promotional offer: " + e);
            return false;
        }
        return true;
    }

    /// Promotional offers are an App Store feature.
    private static boolean promotionalOffersSupported() {
        String platform = Display.getInstance().getPlatformName();
        return "ios".equalsIgnoreCase(platform) || "mac".equalsIgnoreCase(platform);
    }

    /// The signed promotional offer of a product, or null when the cloud
    /// gave no usable signature. Blocking.
    private ApplePromotionalOffer fetchPromotionalOffer(String productId, String offerId) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "productId", productId);
        sb.append(',');
        appendField(sb, "offerId", offerId);
        Map payload = postSigned("/promo-offer-signature", sb.toString());
        if (payload == null || !asBoolean(payload.get("accepted"))) {
            return null;
        }
        String keyId = CustomerInfo.asString(payload.get("keyId"));
        String offerNonce = CustomerInfo.asString(payload.get("nonce"));
        String signature = CustomerInfo.asString(payload.get("signature"));
        if (keyId == null || offerNonce == null || signature == null
                || keyId.length() == 0 || offerNonce.length() == 0 || signature.length() == 0) {
            return null;
        }
        ApplePromotionalOffer offer = new ApplePromotionalOffer();
        offer.setOfferIdentifier(offerId);
        offer.setKeyIdentifier(keyId);
        offer.setNonce(offerNonce);
        offer.setSignature(signature);
        offer.setTimestamp(CustomerInfo.asLong(payload.get("timestamp")));
        return offer;
    }

    /// Runs a network call to its end: through `invokeAndBlock` on the EDT,
    /// directly on any other thread.
    private static void blocking(Runnable r) {
        Display d = Display.getInstance();
        if (d.isEdt()) {
            d.invokeAndBlock(r);
        } else {
            r.run();
        }
    }

    /// The `url` of an accepted answer, or null.
    private static String urlOf(Map payload) {
        if (payload == null || !asBoolean(payload.get("accepted"))) {
            return null;
        }
        String url = CustomerInfo.asString(payload.get("url"));
        return url == null || url.length() == 0 ? null : url;
    }

    /// The page where the customer manages the subscription, or null when
    /// there is none. Never touches the network: for a subscription bought
    /// on the web it is the billing portal [#fetchManagementUrl()] last
    /// answered, and otherwise -- or before that was ever asked -- the
    /// store's page from the last
    /// [customer information][#getCustomerInfo()].
    public String getManagementUrl() {
        CustomerInfo info = getCustomerInfo();
        String user = getAppUserId();
        if (portalUrl != null && paidOnWeb(info) && user.equals(portalUser)) {
            return portalUrl;
        }
        return info.getManagementUrl();
    }

    /// The page where the customer manages the subscription, asking the
    /// cloud for a billing portal when the newest active entitlement -- the
    /// one that expires last -- was bought on the web (`stripe` or
    /// `paddle`). A portal address is short lived, so it is asked for when
    /// it is about to be opened. For a store subscription this is
    /// [#getManagementUrl()] and nothing is asked.
    ///
    /// Blocking network call -- invoke off the EDT.
    ///
    /// #### Returns
    ///
    /// the address, or null when there is none
    public String fetchManagementUrl() {
        CustomerInfo info = getCustomerInfo();
        if (isCloudEnabled() && paidOnWeb(info)) {
            String url = urlOf(postSigned("/web-portal", null));
            if (url != null) {
                portalUrl = url;
                portalUser = getAppUserId();
                return url;
            }
        }
        return getManagementUrl();
    }

    /// Whether the active entitlement that expires last was bought on the
    /// web. One that never expires counts as the last only when no other is
    /// active.
    static boolean paidOnWeb(CustomerInfo info) {
        EntitlementInfo newest = null;
        for (EntitlementInfo e : info.getActiveEntitlements()) {
            if (newest == null || e.getExpiresDate() > newest.getExpiresDate()) {
                newest = e;
            }
        }
        if (newest == null) {
            return false;
        }
        String store = newest.getStore();
        return "stripe".equalsIgnoreCase(store) || "paddle".equalsIgnoreCase(store);
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

    /// Reports a commerce event to the analytics providers the application
    /// registered with `Analytics`, in the category `commerce`.
    /// [com.codename1.payment.paywall.PaywallView]
    /// and the customer center call this for what they show; an application
    /// that draws its own paywall can call it with the same names so its
    /// reports read the same.
    ///
    /// The event carries the parameters `offering`, `package` and `product`
    /// for the values that are not null, `placement` when the offering was
    /// fetched for a placement, and `experiment` and `variant` when the
    /// offering was chosen by an experiment. Nothing is sent without the
    /// user's analytics consent, and the call does nothing at all when no
    /// provider is registered. It never throws and never blocks.
    ///
    /// #### Parameters
    ///
    /// - `name`: the event name, such as `paywall_view`
    ///
    /// - `offeringId`: the offering concerned, or null
    ///
    /// - `packageId`: the package concerned, or null
    ///
    /// - `productId`: the store product concerned, or null
    ///
    /// - `key`: the name of one more parameter, or null
    ///
    /// - `value`: the value of that parameter, or null to leave it out
    public void analyticsEvent(String name, String offeringId, String packageId, String productId,
            String key, String value) {
        if (name == null) {
            return;
        }
        try {
            Map<String, Object> params = contextParams(offeringId, packageId, productId);
            if (key != null && value != null) {
                params.put(key, value);
            }
            Analytics.autoEvent(name, ANALYTICS_CATEGORY, params);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private Map<String, Object> contextParams(String offeringId, String packageId, String productId) {
        Map<String, Object> params = new HashMap<String, Object>();
        putParam(params, "offering", offeringId);
        putParam(params, "package", packageId);
        putParam(params, "product", productId);
        Object context = offeringId == null ? null : offeringContext.get(offeringId);
        if (context instanceof String[]) {
            String[] c = (String[]) context;
            putParam(params, "placement", c[0]);
            putParam(params, "experiment", c[1]);
            putParam(params, "variant", c[2]);
        }
        return params;
    }

    private static void putParam(Map<String, Object> params, String key, String value) {
        if (value != null && value.length() > 0) {
            params.put(key, value);
        }
    }

    /// Remembers which placement and experiment each offering of an answer
    /// belongs to. The experiment chose the current offering, so only that
    /// one is labelled with it.
    private void rememberContext(Offerings result, String placement) {
        Map next = new HashMap(offeringContext);
        Offering current = result.getCurrent();
        for (Offering o : result.getAll()) {
            if (o.getId() == null) {
                continue;
            }
            boolean chosen = current != null && o.getId().equals(current.getId());
            String[] context = new String[3];
            context[0] = placement;
            if (chosen) {
                context[1] = result.getExperimentId();
                context[2] = result.getExperimentVariant();
            }
            next.put(o.getId(), context);
        }
        offeringContext = next;
    }

    /// Reports `experiment_exposure` the first time this install is seen in
    /// an experiment's variant. Remembered only once it could be sent, so an
    /// exposure met before the user consented is reported after.
    private static void reportExposure(Offerings result) {
        try {
            String id = result.getExperimentId();
            if (id == null || !analyticsActive()) {
                return;
            }
            String variant = result.getExperimentVariant();
            String pair = id + "/" + (variant == null ? "" : variant);
            if (pair.equals(Preferences.get(PREF_EXPOSURE, null))) {
                return;
            }
            Preferences.set(PREF_EXPOSURE, pair);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("experiment", id);
            putParam(params, "variant", variant);
            Analytics.autoEvent("experiment_exposure", ANALYTICS_CATEGORY, params);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Whether analytics would send an event now: a provider is registered
    /// and the user's consent covers analytics.
    private static boolean analyticsActive() {
        AnalyticsConsent c = Analytics.getConsent();
        boolean allowed;
        if (Analytics.getConsentMode() == ConsentMode.OPT_OUT) {
            allowed = c == null || c.isAnalytics();
        } else {
            allowed = c != null && c.isAnalytics();
        }
        return allowed && !Analytics.getProviders().isEmpty();
    }

    /// The analytics client id of this install, which the cloud uses to join
    /// a customer to the install's analytics and invite records. Null -- and
    /// then no request carries one -- when the application registered no
    /// analytics provider or the user's consent does not cover analytics, so
    /// an id is never created for an application that does not use analytics
    /// and never sent for a user who opted out.
    static String analyticsClientId() {
        try {
            if (!analyticsActive()) {
                return null;
            }
            String id = Analytics.clientId();
            return id == null || id.length() == 0 ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void appendClientId(StringBuilder sb) {
        String id = analyticsClientId();
        if (id != null) {
            sb.append(',');
            appendField(sb, "analyticsClientId", id);
        }
    }

    /// Adds the context of a purchase started by
    /// [#purchase(CommercePackage)] to the parameters of the analytics event
    /// [Purchase] is about to send for the same product. A context is used
    /// for one purchase: the completing event takes it, and it lapses by
    /// itself so a later purchase of the product is not labelled with it.
    ///
    /// #### Parameters
    ///
    /// - `sku`: the product of the event
    ///
    /// - `params`: the parameters to add to
    ///
    /// - `consume`: true for the event that completes the purchase
    static void addPurchaseContext(String sku, Map<String, Object> params, boolean consume) {
        // No try block here: the loop below casts, and a cast must never
        // sit under a handler that could be taken for its failure.
        CommerceManager cm = instance;
        if (cm == null) {
            return;
        }
        PurchaseContext pending = cm.pendingPurchase;
        if (pending == null || sku == null || params == null || !sku.equals(pending.sku)) {
            return;
        }
        boolean lapsed = System.currentTimeMillis() - pending.time > PURCHASE_CONTEXT_LIFETIME;
        if (consume || lapsed) {
            cm.pendingPurchase = null;
        }
        if (lapsed) {
            return;
        }
        for (Map.Entry<String, Object> e : pending.params.entrySet()) {
            if (!params.containsKey(e.getKey())) {
                params.put(e.getKey(), e.getValue());
            }
        }
    }

    /// Moves the start of the pending purchase context back in time.
    static void agePurchaseContextForTests(long millis) {
        CommerceManager cm = instance;
        if (cm != null && cm.pendingPurchase != null) {
            cm.pendingPurchase = new PurchaseContext(cm.pendingPurchase.sku, cm.pendingPurchase.params,
                    cm.pendingPurchase.time - millis);
        }
    }

    /// What [#purchase(CommercePackage)] knew about a purchase it started.
    private static final class PurchaseContext {
        final String sku;
        final Map<String, Object> params;
        final long time;

        PurchaseContext(String sku, Map<String, Object> params) {
            this(sku, params, System.currentTimeMillis());
        }

        PurchaseContext(String sku, Map<String, Object> params, long time) {
            this.sku = sku;
            this.params = params;
            this.time = time;
        }
    }

    /// Registers, once, for the invite attribution of this install. A
    /// referral reward is granted by the cloud, so when the attribution is
    /// known the customer is fetched again and the reward appears without the
    /// application asking. The registration goes through `Analytics`, not
    /// the invite package: it leaves the application's own invite listener
    /// alone, and commerce does not make an application depend on invites.
    private void hookInvites() {
        if (invitesHooked) {
            return;
        }
        invitesHooked = true;
        hookingInvites = true;
        try {
            Analytics.setAttributionObserver(INVITE_OBSERVER);
        } catch (Throwable t) {
            Log.e(t);
        } finally {
            hookingInvites = false;
        }
    }

    private void inviteArrived() {
        // Registering from inside a refresh answers at once when the
        // attribution is already known, and that refresh is the fetch.
        if (!isCloudEnabled() || (hookingInvites && refreshing)) {
            return;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
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
        refreshing = true;
        try {
            refreshNow();
        } finally {
            refreshing = false;
        }
    }

    private void refreshNow() {
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
            storeEnvelope(storageKey(STORAGE_CUSTOMER_PREFIX, getAppUserId()));
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
        if (isCloudEnabled()) {
            hookInvites();
        }
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
        Map stored = loadEnvelope(storageKey(STORAGE_CUSTOMER_PREFIX, id));
        if (stored != null) {
            customerInfo = new CustomerInfo(stored);
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
    /// cannot be replayed to a later request. An answer that is unsigned or
    /// wrongly signed is ignored, and while no public key is known at all
    /// every answer is: the request is not even sent.
    private Map postSigned(String path, String extraFields) {
        lastEnvelope = null;
        String url = endpoint();
        if (url == null) {
            return null;
        }
        byte[] key = publicKey();
        if (key == null) {
            if (!noKeyLogged) {
                noKeyLogged = true;
                Log.p("CommerceManager: no public key for the commerce service is known ("
                        + PROP_PUBLIC_KEY + "), so its answers cannot be verified and are not used;"
                        + " answering from the store directly");
            }
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
        Object signature = envelope.get("signature");
        if (!(signature instanceof String) || !verify(nonce, text, (String) signature, key)) {
            Log.p("CommerceManager: ignored an answer of " + path + " with a missing or bad signature");
            return null;
        }
        String sigText = (String) signature;
        Object keyIdValue = envelope.get("keyId");
        String keyId = keyIdValue instanceof String ? (String) keyIdValue : null;
        try {
            Map parsed = JSONParser.parseJSON(text);
            StringBuilder sb = new StringBuilder("{");
            appendField(sb, "nonce", nonce);
            sb.append(',');
            appendField(sb, "payload", text);
            sb.append(',');
            appendField(sb, "signature", sigText);
            if (keyId != null) {
                sb.append(',');
                appendField(sb, "keyId", keyId);
            }
            sb.append('}');
            lastEnvelope = sb.toString();
            return parsed;
        } catch (Throwable t) {
            Log.p("CommerceManager" + path + " answered an unreadable payload: " + t);
            return null;
        }
    }

    /// Stores the envelope of the answer postSigned verified last under a
    /// storage entry name.
    private void storeEnvelope(String name) {
        if (lastEnvelope == null) {
            return;
        }
        try {
            Storage.getInstance().writeObject(name, lastEnvelope);
        } catch (Throwable t) {
            Log.p("CommerceManager: could not store " + name + ": " + t);
        }
    }

    /// The payload of a stored envelope whose signature still verifies
    /// against the current key, or null. An entry that does not verify, was
    /// written in an older format, or cannot be read is deleted.
    private static Map loadEnvelope(String name) {
        Storage st = Storage.getInstance();
        if (!st.exists(name)) {
            return null;
        }
        try {
            Object stored = st.readObject(name);
            byte[] key = publicKey();
            if (key != null && stored instanceof String) {
                Map env = JSONParser.parseJSON((String) stored);
                Object nonce = env.get("nonce");
                Object payload = env.get("payload");
                Object signature = env.get("signature");
                if (nonce instanceof String && payload instanceof String && signature instanceof String
                        && verify((String) nonce, (String) payload, (String) signature, key)) {
                    return JSONParser.parseJSON((String) payload);
                }
            }
        } catch (Throwable t) {
            Log.p("CommerceManager: could not read " + name + ": " + t);
        }
        try {
            st.deleteStorageFile(name);
        } catch (Throwable t) {
            Log.p("CommerceManager: could not delete " + name + ": " + t);
        }
        return null;
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
        appendClientId(sb);
        if (extraFields != null && extraFields.length() > 0) {
            sb.append(',').append(extraFields);
        }
        sb.append('}');
        return sb.toString();
    }

    /// Posts one receipt to the cloud /validate endpoint, folds the returned
    /// entitlements into the cache, and returns {degraded} (or null when there
    /// is no answer to trust).
    ///
    /// With a public key the request carries a nonce and the answer is the
    /// signed envelope [#postSigned(String, String)] verifies, so an answer
    /// forged on the network applies nothing. Without a key the request is
    /// still sent, so the service records the purchase and the revenue of an
    /// invite, but its unsigned answer is not used: the store-direct fallback
    /// answers instead.
    private Boolean[] validateReceipt(Receipt r) {
        String url = endpoint();
        if (url == null || r == null) {
            return null;
        }
        if (publicKey() == null) {
            postForJson(url + "/validate", buildRequest(r));
            return null;
        }
        Map result = postSigned("/validate", receiptFields(r));
        if (result == null) {
            return null;
        }
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

    /// The unsigned request of a client that knows no public key: no nonce,
    /// so the service answers in the old unsigned shape.
    private String buildRequest(Receipt r) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendField(sb, "buildKey", Display.getInstance().getProperty(PROP_BUILD_KEY, ""));
        sb.append(',');
        appendField(sb, "packageName", packageName());
        sb.append(',');
        appendField(sb, "appUserId", getAppUserId());
        sb.append(',');
        sb.append(receiptFields(r));
        appendClientId(sb);
        sb.append('}');
        return sb.toString();
    }

    /// The fields that describe one receipt, without the enclosing braces.
    private static String receiptFields(Receipt r) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "store", storeOf(r));
        sb.append(',');
        // On StoreKit 2 the order data carries the signed transaction JWS; the
        // server decodes/normalizes it. Other stores send their own receipt.
        appendField(sb, "signedTransaction", r.getOrderData());
        sb.append(',');
        appendField(sb, "productId", r.getSku());
        return sb.toString();
    }

    /// The store name the service knows a receipt's store code by. Package
    /// private so the mapping can be tested without a network round trip.
    static String storeOf(Receipt r) {
        String code = r.getStoreCode();
        if (Receipt.STORE_CODE_ITUNES.equals(code)) {
            // The Mac App Store reports the same store code and is a store
            // of its own to the service, with its own products and keys.
            return "mac".equalsIgnoreCase(Display.getInstance().getPlatformName()) ? "mac" : "apple";
        }
        if (Receipt.STORE_CODE_PLAY.equals(code)) {
            return "google";
        }
        if (Receipt.STORE_CODE_AMAZON.equals(code)) {
            // Named rather than left to the fall through below: the service
            // routes on this word, so it must not move if the code ever does.
            return "amazon";
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
