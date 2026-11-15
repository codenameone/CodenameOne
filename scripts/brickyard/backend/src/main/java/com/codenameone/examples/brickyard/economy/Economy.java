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

import com.codenameone.examples.brickyard.account.Limits;
import com.codename1.appkit.server.Caller;
import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.Roles;
import com.codenameone.examples.brickyard.api.CatalogItemDto;
import com.codenameone.examples.brickyard.api.JobDto;
import com.codenameone.examples.brickyard.api.OwnershipDto;
import com.codenameone.examples.brickyard.api.PayoutDto;
import com.codenameone.examples.brickyard.api.PlayReportDto;
import com.codenameone.examples.brickyard.api.PlusDto;
import com.codenameone.examples.brickyard.api.ProductDto;
import com.codenameone.examples.brickyard.api.ReceiptDto;
import com.codenameone.examples.brickyard.api.StoreNotificationDto;
import com.codenameone.examples.brickyard.api.WalletDto;
import com.codenameone.examples.brickyard.domain.CatalogItem;
import com.codenameone.examples.brickyard.domain.Experience;
import com.codenameone.examples.brickyard.domain.Ownership;
import com.codenameone.examples.brickyard.domain.PayoutRequest;
import com.codenameone.examples.brickyard.domain.PlayTime;
import com.codenameone.examples.brickyard.domain.PlusSubscription;
import com.codenameone.examples.brickyard.domain.Product;
import com.codenameone.examples.brickyard.domain.StorePurchase;
import com.codenameone.examples.brickyard.moderation.Audit;
import com.codenameone.examples.brickyard.moderation.Sanctions;
import com.codenameone.examples.brickyard.repository.AccountRepository;
import com.codenameone.examples.brickyard.repository.CatalogItemRepository;
import com.codenameone.examples.brickyard.repository.ExperienceRepository;
import com.codenameone.examples.brickyard.repository.OwnershipRepository;
import com.codenameone.examples.brickyard.repository.PayoutRequestRepository;
import com.codenameone.examples.brickyard.repository.PlayTimeRepository;
import com.codenameone.examples.brickyard.repository.PlusSubscriptionRepository;
import com.codenameone.examples.brickyard.repository.ProductRepository;
import com.codenameone.examples.brickyard.repository.StorePurchaseRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Everything Bricks buy: store packs and Plus, game passes and developer
/// products, catalog items; and what creators earn: sales, engagement
/// payouts for Plus play time, and payout requests.
@Component
public class Economy {
    public static final String PLUS_SKU = "plus.monthly";
    public static final long PLUS_STIPEND = 450;
    public static final long MONTH = 30 * Clock.DAY;
    /// The creator's share of a sale, in percent; the rest is the platform's fee.
    public static final int CREATOR_SHARE = 70;
    /// Bricks a creator earns per minute a Plus subscriber plays their experience.
    public static final long PAYOUT_PER_PLUS_MINUTE = 1;
    public static final long MINIMUM_PAYOUT = 1000;
    /// The most play one report may claim: an engine reports at least hourly.
    public static final long MAX_REPORT_SECONDS = 3600;

    private final Wallet wallet;
    private final ReceiptValidator receipts;
    private final Sanctions sanctions;
    private final Audit audit;
    private final StorePurchaseRepository purchases;
    private final ProductRepository products;
    private final OwnershipRepository owned;
    private final CatalogItemRepository catalog;
    private final PlusSubscriptionRepository plus;
    private final PlayTimeRepository play;
    private final PayoutRequestRepository payouts;
    private final ExperienceRepository experiences;
    private final AccountRepository accounts;
    /// The parental spending limit, checked on every Bricks purchase.
    private final Limits limits;

    public Economy(Wallet wallet, ReceiptValidator receipts, Sanctions sanctions, Audit audit,
            StorePurchaseRepository purchases, ProductRepository products, OwnershipRepository owned,
            CatalogItemRepository catalog, PlusSubscriptionRepository plus, PlayTimeRepository play,
            PayoutRequestRepository payouts, ExperienceRepository experiences, AccountRepository accounts,
            Limits limits) {
        this.limits = limits;
        this.wallet = wallet;
        this.receipts = receipts;
        this.sanctions = sanctions;
        this.audit = audit;
        this.purchases = purchases;
        this.products = products;
        this.owned = owned;
        this.catalog = catalog;
        this.plus = plus;
        this.play = play;
        this.payouts = payouts;
        this.experiences = experiences;
        this.accounts = accounts;
    }

    /// The Bricks a store SKU grants, or 0 for Plus, or -1 for an unknown SKU.
    static long packBricks(String sku) {
        if ("bricks.400".equals(sku)) {
            return 400;
        }
        if ("bricks.1000".equals(sku)) {
            return 1100;
        }
        if (PLUS_SKU.equals(sku)) {
            return 0;
        }
        return -1;
    }

    /// Redeems a receipt the app sent. See [#credit] for why it counts once.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto redeem(String username, ReceiptDto r) throws IOException {
        sanctions.requireNotBanned(username);
        if (r == null || r.store() == null || r.sku() == null) {
            throw new ResponseStatusException(400, "A receipt needs a store and a SKU");
        }
        credit(username, r.store(), r.sku(), r.transactionId(), r.receipt());
        return wallet.describe(username);
    }

    /// A purchase the store reported to the server itself. The account the
    /// app attached to the purchase is credited, after the same check with
    /// the store and under the same key as [#redeem], so a purchase that
    /// arrives both ways -- the store's notification and the app's receipt,
    /// in either order -- is credited once. A ban does not stop it: the
    /// player has paid, and a refund is staff's call.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto notified(StoreNotificationDto n) throws IOException {
        if (n == null || n.store() == null || n.sku() == null || n.account() == null) {
            throw new ResponseStatusException(400, "A notification needs a store, a SKU and the account");
        }
        if (accounts.findById(n.account()) == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        credit(n.account(), n.store(), n.sku(), n.transactionId(), n.receipt());
        return wallet.describe(n.account());
    }

    /// The store's name as the purchase key spells it: a receipt that says
    /// "Apple" is the same purchase as one that says "apple".
    static String storeKey(String store) {
        String s = Text.required(store, 16, "the store").trim();
        if (AppleReceiptValidator.STORE.equalsIgnoreCase(s)) {
            return AppleReceiptValidator.STORE;
        }
        if (GooglePlayValidator.STORE.equalsIgnoreCase(s)) {
            return GooglePlayValidator.STORE;
        }
        return s;
    }

    /// Credits a purchase once: it is keyed by the store and the transaction
    /// id the store vouched for (never the one the caller claimed), so the
    /// same purchase arriving again, by either path, is answered and not
    /// credited twice. The key is the purchase row's primary key, so two
    /// arriving at the same moment cannot both insert it.
    private void credit(String username, String store, String sku, String transactionId, String receipt)
            throws IOException {
        long bricks = packBricks(sku);
        if (bricks < 0) {
            throw new ResponseStatusException(400, "No such product");
        }
        String key = storeKey(store);
        String txn = receipts.verify(key, sku, transactionId, receipt);
        if (txn == null) {
            throw new ResponseStatusException(402, "The store did not accept that receipt");
        }
        String id = key + "|" + txn;
        if (id.length() > 400) {
            throw new ResponseStatusException(400, "That transaction id is too long");
        }
        StorePurchase existing = purchases.findById(id);
        if (existing != null) {
            if (!existing.getUsername().equals(username)) {
                throw new ResponseStatusException(409, "That receipt belongs to another account");
            }
            return;
        }
        StorePurchase p = new StorePurchase();
        p.setId(id);
        p.setUsername(username);
        p.setSku(sku);
        p.setBricks(PLUS_SKU.equals(sku) ? PLUS_STIPEND : bricks);
        p.setCreatedAt(Clock.now());
        purchases.insert(p);
        if (PLUS_SKU.equals(sku)) {
            PlusSubscription s = plus.findById(username);
            long now = Clock.now();
            boolean fresh = s == null;
            if (fresh) {
                s = new PlusSubscription();
                s.setId(username);
            }
            s.setExpiresAt(Math.max(now, s.getExpiresAt()) + MONTH);
            s.setLastStipendAt(now);
            if (fresh) {
                plus.insert(s);
            } else {
                plus.save(s);
            }
            wallet.post(username, PLUS_STIPEND, "PLUS_STIPEND", id);
        } else {
            wallet.post(username, bricks, "PURCHASE", id);
        }
    }

    public boolean isPlus(String username) {
        PlusSubscription s = plus.findById(username);
        return s != null && s.getExpiresAt() > Clock.now();
    }

    public PlusDto plusOf(String username) {
        PlusSubscription s = plus.findById(username);
        return new PlusDto(s == null ? 0 : s.getExpiresAt(), isPlus(username) ? 1 : 0);
    }

    @Transactional(rollbackFor = IOException.class)
    public ProductDto createProduct(String experienceId, ProductDto request) throws IOException {
        Experience e = experience(experienceId);
        Access.requireOwner(e);
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to create");
        }
        String kind = "PASS".equalsIgnoreCase(request.kind()) ? "PASS"
                : "PRODUCT".equalsIgnoreCase(request.kind()) ? "PRODUCT" : null;
        if (kind == null) {
            throw new ResponseStatusException(400, "A product is a PASS or a PRODUCT");
        }
        if (request.price() < 1 || request.price() > 1000000) {
            throw new ResponseStatusException(400, "A price is between 1 and 1000000 Bricks");
        }
        Product p = new Product();
        p.setId(Ids.next());
        p.setExperienceId(e.getId());
        p.setKind(kind);
        p.setName(Text.required(request.name(), 80, "a name").trim());
        p.setPrice(request.price());
        p.setCreatedAt(Clock.now());
        products.insert(p);
        return toDto(p);
    }

    public List<ProductDto> products(String experienceId) {
        List<ProductDto> out = new ArrayList<ProductDto>();
        for (Product p : products.onSale(experienceId)) {
            out.add(toDto(p));
        }
        return out;
    }

    /// Renames or reprices a product; only the experience's owner may.
    @Transactional(rollbackFor = IOException.class)
    public ProductDto updateProduct(String productId, ProductDto request) throws IOException {
        Product p = ownedProduct(productId);
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to change");
        }
        if (request.name() != null && request.name().trim().length() > 0) {
            p.setName(Text.required(request.name(), 80, "a name").trim());
        }
        if (request.price() != 0) {
            if (request.price() < 1 || request.price() > 1000000) {
                throw new ResponseStatusException(400, "A price is between 1 and 1000000 Bricks");
            }
            p.setPrice(request.price());
        }
        products.save(p);
        return toDto(p);
    }

    /// Takes a product off sale. Nothing is deleted: what players bought
    /// stays in their inventory and the engine still answers that they own it.
    @Transactional(rollbackFor = IOException.class)
    public ProductDto deactivateProduct(String productId) throws IOException {
        Product p = ownedProduct(productId);
        p.setActive(0);
        products.save(p);
        return toDto(p);
    }

    private Product ownedProduct(String productId) {
        Product p = productId == null ? null : products.findById(productId);
        if (p == null || p.getActive() == 0) {
            throw new ResponseStatusException(404, "No such product");
        }
        Access.requireOwner(experience(p.getExperienceId()));
        return p;
    }

    /// A game pass is bought once; a developer product any number of times.
    /// The buyer pays the price, the creator is credited their share.
    @Transactional(rollbackFor = IOException.class)
    public OwnershipDto buyProduct(String username, String productId) throws IOException {
        sanctions.requireNotBanned(username);
        Product p = productId == null ? null : products.findById(productId);
        if (p == null || p.getActive() == 0) {
            throw new ResponseStatusException(404, "No such product");
        }
        Experience e = experience(p.getExperienceId());
        if (e.getPublishedVersion() < 1) {
            throw new ResponseStatusException(404, "That experience is not published");
        }
        boolean pass = "PASS".equals(p.getKind());
        if (pass && count(username, p.getId()) > 0) {
            throw new ResponseStatusException(409, "You already own that pass");
        }
        Ownership o = ownership(pass ? username + "|" + p.getId() : Ids.next(), username, p.getId(),
                e.getId(), p.getKind(), p.getPrice());
        limits.spend(username, p.getPrice());
        wallet.post(username, -p.getPrice(), "BUY_" + p.getKind(), o.getId());
        wallet.post(e.getOwner(), p.getPrice() * CREATOR_SHARE / 100, "SALE", o.getId());
        owned.insert(o);
        return new OwnershipDto(p.getId(), username, count(username, p.getId()));
    }

    private static Ownership ownership(String id, String username, String productId, String experienceId,
            String kind, long price) {
        Ownership o = new Ownership();
        o.setId(id);
        o.setUsername(username);
        o.setProductId(productId);
        o.setExperienceId(experienceId);
        o.setKind(kind);
        o.setPrice(price);
        o.setCreatedAt(Clock.now());
        return o;
    }

    public int count(String username, String productId) {
        return owned.owned(username, productId).size();
    }

    /// What the engine's MarketplaceService asks: does this player own this?
    public OwnershipDto owns(String username, String productId) {
        Access.requireEngine();
        return new OwnershipDto(productId, username, username == null || productId == null ? 0
                : count(username, productId));
    }

    public List<OwnershipDto> inventory(String username) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (Ownership o : owned.ofUser(username)) {
            Integer c = counts.get(o.getProductId());
            counts.put(o.getProductId(), c == null ? 1 : c + 1);
        }
        List<OwnershipDto> out = new ArrayList<OwnershipDto>();
        for (Map.Entry<String, Integer> c : counts.entrySet()) {
            out.add(new OwnershipDto(c.getKey(), username, c.getValue()));
        }
        return out;
    }

    public List<CatalogItemDto> catalog() {
        List<CatalogItemDto> out = new ArrayList<CatalogItemDto>();
        for (CatalogItem c : catalog.findAll()) {
            out.add(new CatalogItemDto(c.getId(), c.getName(), c.getSlot(), c.getPrice()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public CatalogItemDto addCatalogItem(CatalogItemDto item) throws IOException {
        if (item == null || item.price() < 1) {
            throw new ResponseStatusException(400, "A catalog item needs a price");
        }
        CatalogItem c = new CatalogItem();
        c.setId(Ids.next());
        c.setName(Text.required(item.name(), 80, "a name").trim());
        c.setSlot(Text.required(item.slot(), 32, "a slot").trim());
        c.setPrice(item.price());
        c.setCreatedAt(Clock.now());
        catalog.insert(c);
        audit.record("CATALOG_ADD", c.getId(), c.getName());
        return new CatalogItemDto(c.getId(), c.getName(), c.getSlot(), c.getPrice());
    }

    @Transactional(rollbackFor = IOException.class)
    public OwnershipDto buyItem(String username, String itemId) throws IOException {
        sanctions.requireNotBanned(username);
        CatalogItem c = itemId == null ? null : catalog.findById(itemId);
        if (c == null) {
            throw new ResponseStatusException(404, "No such item");
        }
        if (count(username, c.getId()) > 0) {
            throw new ResponseStatusException(409, "You already own that item");
        }
        Ownership o = ownership(username + "|" + c.getId(), username, c.getId(), "", "ITEM", c.getPrice());
        limits.spend(username, c.getPrice());
        wallet.post(username, -c.getPrice(), "BUY_ITEM", o.getId());
        owned.insert(o);
        return new OwnershipDto(c.getId(), username, 1);
    }

    /// Records Plus play time; play by a player without Plus, or by the
    /// creator in their own experience, earns nothing and is not kept.
    @Transactional(rollbackFor = IOException.class)
    public PlayReportDto reportPlay(PlayReportDto r) throws IOException {
        Access.requireEngine();
        if (r == null || r.seconds() < 1 || r.seconds() > MAX_REPORT_SECONDS) {
            throw new ResponseStatusException(400, "A report is 1 to " + MAX_REPORT_SECONDS + " seconds");
        }
        Experience e = experience(r.experienceId());
        if (r.username() == null || !isPlus(r.username()) || r.username().equals(e.getOwner())) {
            return new PlayReportDto(e.getId(), r.username(), 0);
        }
        PlayTime p = new PlayTime();
        p.setId(Ids.next());
        p.setExperienceId(e.getId());
        p.setUsername(r.username());
        p.setSeconds(r.seconds());
        p.setReportedAt(Clock.now());
        play.insert(p);
        return new PlayReportDto(e.getId(), r.username(), r.seconds());
    }

    /// The engagement payout job: sums unpaid Plus seconds per experience,
    /// credits each creator per whole minute and marks the rows paid.
    @Transactional(rollbackFor = IOException.class)
    public JobDto payEngagement() throws IOException {
        Map<String, Long> seconds = new LinkedHashMap<String, Long>();
        long rows = 0;
        for (PlayTime p : play.unpaid(0)) {
            Long s = seconds.get(p.getExperienceId());
            seconds.put(p.getExperienceId(), (s == null ? 0 : s) + p.getSeconds());
            p.setPaidOut(1);
            play.save(p);
            rows++;
        }
        long total = 0;
        for (Map.Entry<String, Long> s : seconds.entrySet()) {
            Experience e = experiences.findById(s.getKey());
            long bricks = s.getValue() / 60 * PAYOUT_PER_PLUS_MINUTE;
            if (e != null && bricks > 0) {
                wallet.post(e.getOwner(), bricks, "ENGAGEMENT", e.getId());
                total += bricks;
            }
        }
        audit.record("JOB", "engagement-payouts", rows + " rows, " + total + " Bricks");
        return new JobDto("engagement-payouts", rows, total);
    }

    /// The stipend job: a live subscription is paid once per month.
    @Transactional(rollbackFor = IOException.class)
    public JobDto payStipends() throws IOException {
        long now = Clock.now();
        long n = 0;
        for (PlusSubscription s : plus.findAll()) {
            if (s.getExpiresAt() > now && s.getLastStipendAt() + MONTH <= now) {
                s.setLastStipendAt(now);
                plus.save(s);
                wallet.post(s.getId(), PLUS_STIPEND, "PLUS_STIPEND", "monthly");
                n++;
            }
        }
        audit.record("JOB", "plus-stipends", n + " stipends");
        return new JobDto("plus-stipends", n, n * PLUS_STIPEND);
    }

    /// A creator asks to cash out; the Bricks are held (debited) until an admin decides.
    @Transactional(rollbackFor = IOException.class)
    public PayoutDto requestPayout(String username, PayoutDto request) throws IOException {
        if (!Caller.hasRole(Roles.CREATOR)) {
            throw new ResponseStatusException(403, "Only creators can request payouts");
        }
        sanctions.requireNotBanned(username);
        if (request == null || request.bricks() < MINIMUM_PAYOUT) {
            throw new ResponseStatusException(400, "A payout is at least " + MINIMUM_PAYOUT + " Bricks");
        }
        PayoutRequest p = new PayoutRequest();
        p.setId(Ids.next());
        p.setUsername(username);
        p.setBricks(request.bricks());
        p.setStatus("PENDING");
        p.setCreatedAt(Clock.now());
        wallet.post(username, -request.bricks(), "PAYOUT_HOLD", p.getId());
        payouts.insert(p);
        return toDto(p);
    }

    public List<PayoutDto> pendingPayouts() {
        List<PayoutDto> out = new ArrayList<PayoutDto>();
        for (PayoutRequest p : payouts.withStatus("PENDING")) {
            out.add(toDto(p));
        }
        return out;
    }

    /// A creator's own payout requests, newest first.
    public List<PayoutDto> payoutsOf(String username) {
        List<PayoutDto> out = new ArrayList<PayoutDto>();
        for (PayoutRequest p : payouts.ofUser(username)) {
            out.add(toDto(p));
        }
        return out;
    }

    /// APPROVE keeps the hold (the money leaves the platform); REJECT returns the Bricks.
    @Transactional(rollbackFor = IOException.class)
    public PayoutDto decidePayout(String id, String decision, String reason) throws IOException {
        PayoutRequest p = id == null ? null : payouts.findById(id);
        if (p == null) {
            throw new ResponseStatusException(404, "No such payout request");
        }
        if (!"PENDING".equals(p.getStatus())) {
            throw new ResponseStatusException(409, "That payout was already decided");
        }
        boolean approve = "APPROVE".equalsIgnoreCase(decision);
        if (!approve && !"REJECT".equalsIgnoreCase(decision)) {
            throw new ResponseStatusException(400, "Decide APPROVE or REJECT");
        }
        p.setStatus(approve ? "APPROVED" : "REJECTED");
        p.setDecidedBy(Caller.name());
        payouts.save(p);
        if (!approve) {
            wallet.post(p.getUsername(), p.getBricks(), "PAYOUT_RETURNED", p.getId());
        }
        audit.record("PAYOUT_" + p.getStatus(), p.getUsername(), p.getBricks() + " Bricks; " + reason);
        return toDto(p);
    }

    /// A staff grant (positive) or revocation (negative); a revocation never overdraws.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto adjust(String username, long amount, String reason) throws IOException {
        if (amount == 0) {
            throw new ResponseStatusException(400, "Grant or revoke a number of Bricks");
        }
        String why = Text.required(reason, 400, "a reason");
        wallet.post(username, amount, amount > 0 ? "STAFF_GRANT" : "STAFF_REVOKE", Caller.name());
        audit.record(amount > 0 ? "GRANT" : "REVOKE", username, amount + " Bricks; " + why);
        return wallet.describe(username);
    }

    /// Refunds a store purchase: the Bricks it granted are taken back, even
    /// below zero, since the money is going back to the player.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto refund(String purchaseId, String reason) throws IOException {
        StorePurchase p = purchaseId == null ? null : purchases.findById(purchaseId);
        if (p == null) {
            throw new ResponseStatusException(404, "No such purchase");
        }
        if (p.getRefunded() != 0) {
            throw new ResponseStatusException(409, "That purchase was already refunded");
        }
        p.setRefunded(1);
        purchases.save(p);
        wallet.post(p.getUsername(), -p.getBricks(), "REFUND", p.getId(), true);
        if (PLUS_SKU.equals(p.getSku())) {
            PlusSubscription s = plus.findById(p.getUsername());
            if (s != null) {
                s.setExpiresAt(Math.max(Clock.now(), s.getExpiresAt() - MONTH));
                plus.save(s);
            }
        }
        audit.record("REFUND", p.getUsername(), p.getId() + "; " + Text.required(reason, 300, "a reason"));
        return wallet.describe(p.getUsername());
    }

    private Experience experience(String id) {
        Experience e = id == null ? null : experiences.findById(id);
        if (e == null) {
            throw new ResponseStatusException(404, "No such experience");
        }
        return e;
    }

    static ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getExperienceId(), p.getKind(), p.getName(), p.getPrice());
    }

    static PayoutDto toDto(PayoutRequest p) {
        return new PayoutDto(p.getId(), p.getUsername(), p.getBricks(), p.getStatus());
    }
}
