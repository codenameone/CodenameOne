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
package com.codenameone.examples.bazaar.alerts;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.mail.Mail;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.PreorderDto;
import com.codenameone.examples.bazaar.api.StockAlertDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.Preorder;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.StockAlert;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.marketing.Shoppers;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.PreorderRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StockAlertRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Back-in-stock alerts and pre-orders.
///
/// An alert is `PENDING` until its variant has stock again; the job then
/// moves it `PENDING -> SENDING` (one conditional update, so two job runs
/// never both claim it), sends the mail, and settles it `SENT`. A send that
/// fails goes back to `PENDING` for the next pass. A sent alert is never
/// sent again: the shopper asked to hear once.
@Component
public class StockAlerts {
    public static final String PENDING = "PENDING";
    public static final String SENDING = "SENDING";
    public static final String SENT = "SENT";
    public static final String EMAIL = "EMAIL";
    public static final String NOW = "NOW";
    public static final String AUTHORIZE = "AUTHORIZE";
    /// Open alerts one shopper may hold in a store.
    static final int MAX_PER_SHOPPER = 50;

    private final StockAlertRepository alerts;
    private final PreorderRepository preorders;
    private final VariantRepository variants;
    private final ProductRepository products;
    private final InventoryLevelRepository levels;
    private final Emails emails;
    private final StoreAccess access;
    private final Shoppers shoppers;

    public StockAlerts(StockAlertRepository alerts, PreorderRepository preorders,
            VariantRepository variants, ProductRepository products, InventoryLevelRepository levels,
            Emails emails, StoreAccess access, Shoppers shoppers) {
        this.alerts = alerts;
        this.preorders = preorders;
        this.variants = variants;
        this.products = products;
        this.levels = levels;
        this.emails = emails;
        this.access = access;
        this.shoppers = shoppers;
    }

    /// Units of a variant free to sell across every location; a variant that
    /// does not track inventory counts as always in stock.
    @Transactional(readOnly = true)
    public int available(Variant v) {
        if (!v.isTrackInventory()) {
            return Integer.MAX_VALUE;
        }
        int total = 0;
        List<InventoryLevel> rows = levels.findByVariantId(v.getId());
        for (int iter = 0; iter < rows.size(); iter++) {
            total += Math.max(0, rows.get(iter).getOnHand() - rows.get(iter).getCommitted());
        }
        return total;
    }

    Variant variant(String storeId, String variantId) {
        Variant v = variantId == null ? null : variants.findById(variantId);
        if (v == null || !storeId.equals(v.getStoreId())) {
            throw new ResponseStatusException(404, "No such variant");
        }
        return v;
    }

    /// Signs the shopper up to hear when a sold-out variant is back. Asking
    /// twice keeps the one alert.
    @Transactional(rollbackFor = IOException.class)
    public StockAlertDto subscribe(String username, String storeId, StockAlertDto in)
            throws IOException {
        shoppers.store(storeId);
        if (in == null) {
            throw new ResponseStatusException(400, "Choose a variant");
        }
        Variant v = variant(storeId, in.variantId());
        if (in.trap() != null && in.trap().length() > 0) {
            // A bot filled the field a person never sees: answered like a
            // success, so it learns nothing, and dropped.
            return new StockAlertDto("", v.getId(), "", EMAIL, PENDING, 0, "");
        }
        String email = Tokens.fold(in.email() == null || in.email().length() == 0 ? username : in.email());
        if (!Mail.isAddress(email) || email.length() > 254) {
            throw new ResponseStatusException(400, "Give an e-mail address");
        }
        if (available(v) > 0) {
            throw new ResponseStatusException(409, "It is in stock now");
        }
        List<StockAlert> same = alerts.findByVariantIdAndEmailAndChannel(v.getId(), email, EMAIL);
        for (int iter = 0; iter < same.size(); iter++) {
            if (PENDING.equals(same.get(iter).getStatus())) {
                return dto(same.get(iter));
            }
        }
        if (username != null) {
            int open = 0;
            List<StockAlert> mine = alerts.findByStoreIdOrderByCreatedAtDesc(storeId);
            for (int iter = 0; iter < mine.size(); iter++) {
                if (username.equals(mine.get(iter).getUsername())
                        && PENDING.equals(mine.get(iter).getStatus())) {
                    open++;
                }
            }
            if (open >= MAX_PER_SHOPPER) {
                throw new ResponseStatusException(429, "That is a lot of alerts already");
            }
        }
        StockAlert a = new StockAlert();
        a.setId(Ids.next());
        a.setStoreId(storeId);
        a.setVariantId(v.getId());
        a.setEmail(email);
        a.setUsername(username == null ? "" : username);
        a.setChannel(EMAIL);
        a.setStatus(PENDING);
        a.setCreatedAt(Instant.now());
        alerts.insert(a);
        return dto(a);
    }

    static StockAlertDto dto(StockAlert a) {
        return new StockAlertDto(a.getId(), a.getVariantId(), a.getEmail(), a.getChannel(),
                a.getStatus(), a.getCreatedAt().toEpochMilli(), "");
    }

    @Transactional(readOnly = true)
    public List<StockAlertDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<StockAlert> rows = alerts.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<StockAlertDto> out = new ArrayList<StockAlertDto>();
        for (int iter = 0; iter < rows.size() && iter < 500; iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    // ----------------------------------------------------------------- job

    @Scheduled(fixedDelay = 60000, initialDelay = 45000, lock = "bazaar-stock-alerts")
    public void job() {
        try {
            tick();
        } catch (IOException | RuntimeException failed) {
            System.err.println("stock alerts: " + failed);
        }
    }

    /// Sends every pending alert whose variant has stock again. Answers how
    /// many went out; a second call right after answers 0.
    public int tick() throws IOException {
        int sent = 0;
        List<StockAlert> open = alerts.findByStatus(PENDING);
        List<String> inStock = new ArrayList<String>();
        List<String> outOfStock = new ArrayList<String>();
        for (int iter = 0; iter < open.size(); iter++) {
            StockAlert a = open.get(iter);
            String vid = a.getVariantId();
            if (outOfStock.contains(vid)) {
                continue;
            }
            if (!inStock.contains(vid)) {
                Variant v = variants.findById(vid);
                if (v == null || available(v) <= 0) {
                    outOfStock.add(vid);
                    continue;
                }
                inStock.add(vid);
            }
            if (send(a)) {
                sent++;
            }
        }
        return sent;
    }

    boolean send(StockAlert a) throws IOException {
        if (alerts.move(a.getId(), PENDING, SENDING) != 1) {
            return false;
        }
        boolean ok;
        try {
            Variant v = variants.findById(a.getVariantId());
            Product p = v == null ? null : products.findById(v.getProductId());
            Store s = shoppers.store(a.getStoreId());
            String title = p == null ? "Your item" : p.getTitle()
                    + (v.getTitle().length() > 0 && !"Default".equals(v.getTitle()) ? " (" + v.getTitle() + ")" : "");
            String path = "/s/" + s.getHandle() + (p == null ? "" : "/products/" + p.getHandle());
            ok = emails.backInStock(a.getStoreId(), a.getEmail(), title, path);
        } catch (IOException | RuntimeException failed) {
            alerts.move(a.getId(), SENDING, PENDING);
            throw failed;
        }
        settle(a.getId(), ok);
        return ok;
    }

    @Transactional(rollbackFor = IOException.class)
    void settle(String alertId, boolean sent) {
        StockAlert a = alerts.findById(alertId);
        a.setStatus(SENT);
        a.setSentAt(Instant.now());
        if (!sent) {
            // An address that cannot take mail is never going to.
            a.setStatus("UNDELIVERABLE");
        }
        alerts.save(a);
    }

    // ----------------------------------------------------------- preorders

    @Transactional(readOnly = true)
    public PreorderDto preorderOf(String storeId, String variantId) {
        shoppers.store(storeId);
        Variant v = variant(storeId, variantId);
        Preorder p = preorders.findByVariantId(v.getId());
        return p == null ? new PreorderDto(v.getId(), false, 0, AUTHORIZE, 0, 0, "") : dto(p);
    }

    static PreorderDto dto(Preorder p) {
        return new PreorderDto(p.getVariantId(), p.isEnabled(), p.getShipDate().toEpochMilli(),
                p.getChargeMode(), p.getLimitQty(), p.getIncomingQty(), p.getNote());
    }

    @Transactional(readOnly = true)
    public List<PreorderDto> preorders(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<Preorder> rows = preorders.findByStoreId(storeId);
        List<PreorderDto> out = new ArrayList<PreorderDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    /// Opens or closes pre-orders on a variant: when it ships, whether the
    /// card is charged now or only held, how many may be pre-ordered, and
    /// the stock on its way.
    @Transactional(rollbackFor = IOException.class)
    public PreorderDto savePreorder(String username, String storeId, String variantId,
            PreorderDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Variant v = variant(storeId, variantId);
        if (in == null || in.limitQty() < 0 || in.incomingQty() < 0 || in.limitQty() > 1000000
                || in.incomingQty() > 1000000) {
            throw new ResponseStatusException(400, "Those pre-order numbers are out of range");
        }
        if (in.enabled() && in.shipDate() <= System.currentTimeMillis()) {
            throw new ResponseStatusException(400, "A pre-order ships in the future");
        }
        Preorder p = preorders.findByVariantId(v.getId());
        boolean fresh = p == null;
        if (fresh) {
            p = new Preorder();
            p.setId(Ids.next());
            p.setStoreId(storeId);
            p.setVariantId(v.getId());
            p.setCreatedAt(Instant.now());
        }
        p.setEnabled(in.enabled());
        p.setShipDate(Tokens.at(in.shipDate()));
        p.setChargeMode(NOW.equals(in.chargeMode()) ? NOW : AUTHORIZE);
        p.setLimitQty(in.limitQty());
        p.setIncomingQty(in.incomingQty());
        p.setNote(Codes.cut(in.note(), 200));
        if (fresh) {
            preorders.insert(p);
        } else {
            preorders.save(p);
        }
        return dto(p);
    }

    /// Units of `v` that may still be sold ahead of stock, for the quote:
    /// the pre-order limit, or the incoming stock when no limit is set.
    @Transactional(readOnly = true)
    public int preorderable(String variantId) {
        Preorder p = preorders.findByVariantId(variantId);
        if (p == null || !p.isEnabled() || p.getShipDate().toEpochMilli() <= System.currentTimeMillis()) {
            return 0;
        }
        return p.getLimitQty() > 0 ? p.getLimitQty() : p.getIncomingQty();
    }
}
