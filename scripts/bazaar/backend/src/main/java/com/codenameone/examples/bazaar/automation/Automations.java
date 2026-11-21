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
package com.codenameone.examples.bazaar.automation;

import com.codename1.appkit.server.push.PushSender;
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AuditDto;
import com.codenameone.examples.bazaar.api.AutomationDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import com.codenameone.examples.bazaar.api.ValidationDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.Automation;
import com.codenameone.examples.bazaar.domain.AutomationRun;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.PurchaseLine;
import com.codenameone.examples.bazaar.domain.PurchaseOrder;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.Supplier;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.push.PushDevices;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.repository.AutomationRepository;
import com.codenameone.examples.bazaar.repository.AutomationRunRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.PurchaseLineRepository;
import com.codenameone.examples.bazaar.repository.PurchaseOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.repository.SupplierRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.webhook.Webhooks;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Store automations: saving and checking them, and running them when a
/// store event happens.
///
/// [#fire] runs every active automation of the store whose trigger is the
/// event's: from each trigger node it walks the edges, a condition choosing
/// its `yes` or its `no` port, and performs each action in turn. Every
/// action leaves an [AuditEvent] whose subject is the automation, which is
/// its activity log. A `wait.minutes` step stores the rest of the walk as an
/// [AutomationRun] that [#resumeDue] carries on.
///
/// An event's context is a flat map of strings: `storeId`, `orderId`,
/// `customerId`, `email`, `totalCents`, `channel`, `country`,
/// `productIds` (comma separated), `variantId`, `locationId`, `cartId`.
/// Automations run after the change that fired them committed; one that
/// fails is logged and never undoes that change.
@Component
public class Automations {
    public static final String WAITING = "WAITING";
    public static final String DONE = "DONE";
    static final int MAX_STEPS = 200;

    private final AutomationRepository automations;
    private final AutomationRunRepository runs;
    private final AuditEventRepository audits;
    private final StoreOrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final InventoryLevelRepository levels;
    private final DiscountRepository discounts;
    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository purchaseOrders;
    private final PurchaseLineRepository purchaseLines;
    private final StoreStaffRepository staff;
    private final PushSender push;
    private final PushDevices devices;
    private final Webhooks webhooks;
    private final StoreAccess access;
    private final Emails emails;
    private final com.codenameone.examples.bazaar.notify.ShopperLanguage language;

    public Automations(AutomationRepository automations, AutomationRunRepository runs,
            AuditEventRepository audits, StoreOrderRepository orders,
            CustomerRepository customers, ProductRepository products, VariantRepository variants,
            InventoryLevelRepository levels, DiscountRepository discounts,
            SupplierRepository suppliers, PurchaseOrderRepository purchaseOrders,
            PurchaseLineRepository purchaseLines, StoreStaffRepository staff, PushSender push,
            PushDevices devices, Webhooks webhooks, StoreAccess access, Emails emails,
            com.codenameone.examples.bazaar.notify.ShopperLanguage language) {
        this.language = language;
        this.emails = emails;
        this.automations = automations;
        this.runs = runs;
        this.audits = audits;
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.variants = variants;
        this.levels = levels;
        this.discounts = discounts;
        this.suppliers = suppliers;
        this.purchaseOrders = purchaseOrders;
        this.purchaseLines = purchaseLines;
        this.staff = staff;
        this.push = push;
        this.devices = devices;
        this.webhooks = webhooks;
        this.access = access;
    }

    // ---------------------------------------------------------------- running

    /// Runs the store's active automations triggered by `trigger`; answers
    /// how many ran.
    public int fire(String storeId, String trigger, Map<String, String> context) {
        List<Automation> mine;
        try {
            mine = byTrigger(storeId, trigger);
        } catch (RuntimeException failed) {
            System.err.println("[automation] could not list " + trigger + ": " + failed);
            return 0;
        }
        int ran = 0;
        for (int iter = 0; iter < mine.size(); iter++) {
            Automation a = mine.get(iter);
            if (!a.isActive()) {
                continue;
            }
            try {
                AutomationGraph graph = AutomationGraph.parse(a.getGraphJson());
                List<AutomationGraph.Node> triggers = graph.triggers();
                Map<String, String> ctx = new HashMap<String, String>(context);
                ctx.put("storeId", storeId);
                for (int t = 0; t < triggers.size(); t++) {
                    if (trigger.equals(triggers.get(t).type)) {
                        walk(a, graph, graph.next(triggers.get(t), null), ctx);
                    }
                }
                counted(a.getId());
                ran++;
            } catch (IOException | RuntimeException failed) {
                note(storeId, a.getId(), "AUTOMATION_FAILED", Codes.cut(String.valueOf(failed), 500));
            }
        }
        return ran;
    }

    @Transactional(readOnly = true)
    private List<Automation> byTrigger(String storeId, String trigger) {
        return automations.findByStoreIdAndTriggerKind(storeId, trigger);
    }

    @Transactional(rollbackFor = IOException.class)
    private void counted(String automationId) throws IOException {
        Automation a = automations.findById(automationId);
        if (a != null) {
            a.setRuns(a.getRuns() + 1);
            a.setLastRunAt(Instant.now());
            automations.save(a);
        }
    }

    /// Performs `start` and everything after it, breadth first; a wait
    /// stores its branch and stops it.
    private void walk(Automation a, AutomationGraph graph, List<AutomationGraph.Node> start,
            Map<String, String> ctx) throws IOException {
        List<AutomationGraph.Node> queue = new ArrayList<AutomationGraph.Node>(start);
        int steps = 0;
        while (!queue.isEmpty() && steps++ < MAX_STEPS) {
            AutomationGraph.Node n = queue.remove(0);
            if (NodeCatalog.isCondition(n.type)) {
                boolean yes = test(n, ctx);
                queue.addAll(graph.next(n, yes ? NodeCatalog.YES : NodeCatalog.NO));
                continue;
            }
            if (NodeCatalog.WAIT_MINUTES.equals(n.type)) {
                long minutes = Math.max(1, AutomationGraph.number(n, "minutes", 60));
                waitAt(a, n.id, ctx, System.currentTimeMillis() + minutes * 60000L);
                continue;
            }
            if (NodeCatalog.isTrigger(n.type)) {
                continue;
            }
            try {
                act(a, n, ctx);
            } catch (IOException | RuntimeException failed) {
                note(ctx.get("storeId"), a.getId(), "AUTOMATION_FAILED",
                        Codes.cut(n.type + ": " + failed, 500));
            }
            queue.addAll(graph.next(n, null));
        }
    }

    @Transactional(rollbackFor = IOException.class)
    private void waitAt(Automation a, String nodeId, Map<String, String> ctx, long resumeAt)
            throws IOException {
        AutomationRun run = new AutomationRun();
        run.setId(Ids.next());
        run.setAutomationId(a.getId());
        run.setStoreId(a.getStoreId());
        run.setNodeId(Codes.cut(nodeId, 64));
        run.setContext(Json.write(ctx));
        run.setStatus(WAITING);
        run.setResumeAt(Instant.ofEpochMilli(resumeAt));
        run.setCreatedAt(Instant.now());
        runs.insert(run);
        record(a.getStoreId(), a.getId(), "AUTOMATION_WAIT", "Waiting until " + Instant.ofEpochMilli(resumeAt));
    }

    /// Carries on the runs whose wait is over.
    @Scheduled(fixedDelay = 60000, initialDelay = 50000, lock = "bazaar-automation-waits")
    public void resume() throws IOException {
        resumeDue(System.currentTimeMillis());
    }

    /// [#resume] as of `now`; answers how many runs carried on.
    public int resumeDue(long now) throws IOException {
        List<AutomationRun> due = waitingRead(now);
        int resumed = 0;
        for (int iter = 0; iter < due.size(); iter++) {
            AutomationRun run = due.get(iter);
            if (!claimRun(run.getId())) {
                continue;
            }
            Automation a = automationRead(run.getAutomationId());
            if (a == null || !a.isActive()) {
                continue;
            }
            AutomationGraph graph = AutomationGraph.parse(a.getGraphJson());
            AutomationGraph.Node at = graph.node(run.getNodeId());
            if (at == null) {
                continue;
            }
            Map<String, String> ctx = new HashMap<String, String>();
            Map stored = Json.parseObject(run.getContext());
            for (Object key : stored.keySet()) {
                ctx.put(String.valueOf(key), String.valueOf(stored.get(key)));
            }
            walk(a, graph, graph.next(at, null), ctx);
            resumed++;
        }
        return resumed;
    }

    @Transactional(readOnly = true)
    private List<AutomationRun> waitingRead(long now) {
        List<AutomationRun> all = runs.findByStatusOrderByResumeAtAsc(WAITING);
        List<AutomationRun> due = new ArrayList<AutomationRun>();
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).getResumeAt().toEpochMilli() <= now) {
                due.add(all.get(iter));
            }
        }
        return due;
    }

    @Transactional(rollbackFor = IOException.class)
    private boolean claimRun(String runId) throws IOException {
        AutomationRun run = runs.findById(runId);
        if (run == null || !WAITING.equals(run.getStatus())) {
            return false;
        }
        run.setStatus(DONE);
        runs.save(run);
        return true;
    }

    @Transactional(readOnly = true)
    private Automation automationRead(String id) {
        return automations.findById(id);
    }

    // ------------------------------------------------------------- conditions

    @Transactional(readOnly = true)
    private boolean test(AutomationGraph.Node n, Map<String, String> ctx) {
        String type = n.type;
        if (NodeCatalog.IF_TOTAL_OVER.equals(type)) {
            return number(ctx.get("totalCents")) > AutomationGraph.number(n, "amount", 0);
        }
        if (NodeCatalog.IF_CHANNEL.equals(type)) {
            return AutomationGraph.param(n, "channel").equalsIgnoreCase(text(ctx.get("channel")));
        }
        if (NodeCatalog.IF_COUNTRY.equals(type)) {
            return hasTag(AutomationGraph.param(n, "country"), text(ctx.get("country")));
        }
        if (NodeCatalog.IF_CUSTOMER_TAG.equals(type)) {
            Customer c = customer(ctx);
            return c != null && hasTag(c.getTags(), AutomationGraph.param(n, "tag"));
        }
        if (NodeCatalog.IF_PRODUCT_TAG.equals(type)) {
            String tag = AutomationGraph.param(n, "tag");
            List<String> ids = Catalog.tags(text(ctx.get("productIds")));
            Variant v = ctx.get("variantId") == null ? null : variants.findById(ctx.get("variantId"));
            if (v != null) {
                ids.add(v.getProductId());
            }
            for (int iter = 0; iter < ids.size(); iter++) {
                Product p = products.findById(ids.get(iter));
                if (p != null && hasTag(p.getTags(), tag)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /// Whether the comma-separated `tags` hold `tag`, ignoring case.
    static boolean hasTag(String tags, String tag) {
        if (tag == null || tag.trim().length() == 0) {
            return false;
        }
        List<String> all = Catalog.tags(tags);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).equalsIgnoreCase(tag.trim())) {
                return true;
            }
        }
        return false;
    }

    /// `tags` with `tag` added unless it is there, within `max` characters.
    static String addTag(String tags, String tag, int max) {
        if (hasTag(tags, tag)) {
            return tags;
        }
        String joined = tags == null || tags.trim().length() == 0 ? tag.trim()
                : tags.trim() + "," + tag.trim();
        return joined.length() > max ? tags : joined;
    }

    // ---------------------------------------------------------------- actions

    @Transactional(rollbackFor = IOException.class)
    private void act(Automation a, AutomationGraph.Node n, Map<String, String> ctx)
            throws IOException {
        String storeId = a.getStoreId();
        String type = n.type;
        if (NodeCatalog.TAG_ORDER.equals(type)) {
            StoreOrder o = ctx.get("orderId") == null ? null : orders.findById(ctx.get("orderId"));
            if (o == null || !storeId.equals(o.getStoreId())) {
                record(storeId, a.getId(), "AUTOMATION_SKIPPED", type + ": the event has no order");
                return;
            }
            String tag = AutomationGraph.param(n, "tag");
            o.setTags(addTag(o.getTags(), tag, 500));
            o.setUpdatedAt(Instant.now());
            orders.save(o);
            record(storeId, a.getId(), "TAG_ORDER", "Order #" + o.getNumber() + " tagged " + tag);
        } else if (NodeCatalog.TAG_CUSTOMER.equals(type)) {
            Customer c = customer(ctx);
            if (c == null) {
                record(storeId, a.getId(), "AUTOMATION_SKIPPED", type + ": the event has no customer");
                return;
            }
            String tag = AutomationGraph.param(n, "tag");
            c.setTags(addTag(c.getTags(), tag, 500));
            c.setUpdatedAt(Instant.now());
            customers.save(c);
            record(storeId, a.getId(), "TAG_CUSTOMER", name(c) + " tagged " + tag);
        } else if (NodeCatalog.NOTIFY_STAFF.equals(type)) {
            notifyStaff(a, n, ctx);
        } else if (NodeCatalog.EMAIL_CUSTOMER.equals(type)) {
            String to = text(ctx.get("email"));
            Customer c = to.length() == 0 ? customer(ctx) : null;
            if (c != null) {
                to = c.getEmail();
            }
            String l = to.length() == 0 ? null : AutomationGraph.variantLanguage(n,
                    language.shopper(storeId, c == null ? null : c.getUsername(), to),
                    "subject", "message");
            String subject = AutomationGraph.param(n, "subject", l);
            String sent = "To " + to + ": " + subject;
            if (to.length() == 0) {
                sent = "No address to write to";
            } else {
                try {
                    emails.custom(storeId, to, subject, AutomationGraph.param(n, "message", l));
                } catch (IOException failed) {
                    sent = "Not sent to " + to + ": " + failed.getMessage();
                }
            }
            record(storeId, a.getId(), "EMAIL_CUSTOMER", sent);
        } else if (NodeCatalog.DISCOUNT_CODE.equals(type)) {
            createCode(a, n, ctx);
        } else if (NodeCatalog.INVENTORY_REORDER.equals(type)) {
            reorder(a, n, ctx);
        } else if (NodeCatalog.WEBHOOK_SEND.equals(type)) {
            boolean queued = webhooks.queueTo(storeId, AutomationGraph.param(n, "webhookId"),
                    "automation/" + Codes.cut(a.getTriggerKind(), 40), payload(a.getName(), ctx))
                    != null;
            record(storeId, a.getId(), "WEBHOOK_SEND", queued ? "Delivery queued"
                    : "No such active webhook");
        }
    }

    /// What a `webhook.send` step posts: the event's context and the
    /// automation's name.
    static String payload(String automation, Map<String, String> ctx) {
        Map<String, String> out = new HashMap<String, String>(ctx);
        out.put("automation", automation);
        return Json.write(out);
    }

    /// Pushes the step's message to every member of staff, each in their
    /// account's language when the merchant translated it
    /// ([ShopperLanguage#staff]).
    private void notifyStaff(Automation a, AutomationGraph.Node n, Map<String, String> ctx)
            throws IOException {
        String message = AutomationGraph.param(n, "message");
        String body = message.length() == 0 ? a.getName() : message;
        StoreOrder o = ctx.get("orderId") == null ? null : orders.findById(ctx.get("orderId"));
        String order = o == null ? "" : " (#" + o.getNumber() + ")";
        if (o != null) {
            body = body + " (order #" + o.getNumber() + ")";
        }
        List<StoreStaff> team = staff.findByStoreId(a.getStoreId());
        int reached = 0;
        for (int iter = 0; iter < team.size(); iter++) {
            String l = message.length() == 0 ? null : AutomationGraph.variantLanguage(n,
                    language.staff(team.get(iter).getUsername()), "message");
            String mine = l == null ? body : AutomationGraph.param(n, "message", l) + order;
            try {
                List<PushTarget> to = devices.targets(team.get(iter).getUsername());
                if (!to.isEmpty()) {
                    push.send(to, PushNotification.builder().title(a.getName()).body(mine)
                            .data("kind", "automation").data("automationId", a.getId()).build(),
                            null);
                    reached++;
                }
            } catch (RuntimeException failed) {
                // One unreachable phone does not stop the rest.
            }
        }
        record(a.getStoreId(), a.getId(), "NOTIFY_STAFF", Codes.cut(body + " -- " + reached
                + " of " + team.size() + " staff reached", 500));
    }

    private void createCode(Automation a, AutomationGraph.Node n, Map<String, String> ctx)
            throws IOException {
        boolean fixed = "FIXED".equalsIgnoreCase(AutomationGraph.param(n, "kind"));
        long amount = AutomationGraph.number(n, "amount", 1000);
        if (amount <= 0 || !fixed && amount > 10000) {
            record(a.getStoreId(), a.getId(), "AUTOMATION_SKIPPED", "The discount amount is out of range");
            return;
        }
        String code = "AUTO-" + randomCode(8);
        Discount d = new Discount();
        d.setId(Ids.next());
        d.setStoreId(a.getStoreId());
        d.setCode(code);
        d.setTitle(Codes.cut(a.getName(), 120));
        d.setKind(fixed ? "FIXED" : "PERCENT");
        d.setAmount(amount);
        d.setAppliesTo(DiscountEngine.ALL);
        d.setUsageLimit(1);
        d.setOncePerCustomer(true);
        d.setActive(true);
        d.setStartsAt(Instant.now());
        d.setCreatedAt(Instant.now());
        discounts.insert(d);
        ctx.put("discountCode", code);
        record(a.getStoreId(), a.getId(), "DISCOUNT_CODE", "Made code " + code
                + (ctx.get("email") == null ? "" : " for " + ctx.get("email")));
    }

    private void reorder(Automation a, AutomationGraph.Node n, Map<String, String> ctx)
            throws IOException {
        String storeId = a.getStoreId();
        Variant v = ctx.get("variantId") == null ? null : variants.findById(ctx.get("variantId"));
        String locationId = text(ctx.get("locationId"));
        if (v == null || !storeId.equals(v.getStoreId()) || locationId.length() == 0) {
            record(storeId, a.getId(), "AUTOMATION_SKIPPED", "inventory.reorder: no low variant");
            return;
        }
        // One draft at a time: a variant already on a draft is left alone, so
        // every sale below the reorder point does not draft another.
        List<PurchaseLine> history = purchaseLines.findByVariantId(v.getId());
        String supplierId = "";
        long newest = -1;
        for (int iter = 0; iter < history.size(); iter++) {
            PurchaseOrder po = purchaseOrders.findById(history.get(iter).getPurchaseOrderId());
            if (po == null || !storeId.equals(po.getStoreId())) {
                continue;
            }
            if ("DRAFT".equals(po.getStatus())) {
                record(storeId, a.getId(), "AUTOMATION_SKIPPED", v.getSku()
                        + " is already on draft purchase order #" + po.getNumber());
                return;
            }
            if (po.getCreatedAt().toEpochMilli() > newest) {
                newest = po.getCreatedAt().toEpochMilli();
                supplierId = po.getSupplierId();
            }
        }
        if (supplierId.length() == 0) {
            // Never bought before: the store's first supplier.
            List<Supplier> all = suppliers.findByStoreIdOrderByNameAsc(storeId);
            if (all.isEmpty()) {
                record(storeId, a.getId(), "AUTOMATION_SKIPPED", "The store has no supplier to order "
                        + v.getSku() + " from");
                return;
            }
            supplierId = all.get(0).getId();
        }
        InventoryLevel level = levels.findByVariantIdAndLocationId(v.getId(), locationId);
        int available = level == null ? 0 : level.getOnHand() - level.getCommitted();
        int point = level == null ? 0 : level.getReorderPoint();
        long qty = AutomationGraph.number(n, "quantity", 0);
        if (qty <= 0) {
            qty = Math.max(1, point * 2 - available);
        }
        // Written here rather than through Stock, which admits only staff: the
        // automation acts for the store, not for a signed-in member of it.
        PurchaseOrder po = new PurchaseOrder();
        po.setId(Ids.next());
        po.setStoreId(storeId);
        po.setNumber((int) purchaseOrders.countByStoreId(storeId) + 1001);
        po.setSupplierId(supplierId);
        po.setLocationId(locationId);
        po.setStatus("DRAFT");
        po.setNote(Codes.cut("Drafted by automation " + a.getName(), 500));
        po.setTotalCents(v.getCostCents() * qty);
        po.setCreatedAt(Instant.now());
        po.setUpdatedAt(Instant.now());
        purchaseOrders.insert(po);
        PurchaseLine line = new PurchaseLine();
        line.setId(Ids.next());
        line.setPurchaseOrderId(po.getId());
        line.setVariantId(v.getId());
        line.setQuantity((int) qty);
        line.setCostCents(v.getCostCents());
        purchaseLines.insert(line);
        record(storeId, a.getId(), "INVENTORY_REORDER", "Drafted purchase order #" + po.getNumber()
                + " for " + qty + " x " + v.getSku());
    }

    private Customer customer(Map<String, String> ctx) {
        String id = ctx.get("customerId");
        if (id != null && id.length() > 0) {
            Customer c = customers.findById(id);
            if (c != null && c.getStoreId().equals(ctx.get("storeId"))) {
                return c;
            }
        }
        String user = ctx.get("username");
        return user == null || user.length() == 0 ? null
                : customers.findByStoreIdAndUsername(ctx.get("storeId"), user);
    }

    private static String name(Customer c) {
        String n = (c.getFirstName() + " " + c.getLastName()).trim();
        return n.length() > 0 ? n : c.getEmail().length() > 0 ? c.getEmail() : c.getUsername();
    }

    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    static String randomCode(int length) throws IOException {
        byte[] random = Crypto.randomBytes(length);
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < length; iter++) {
            out.append(CODE_CHARS.charAt((random[iter] & 0xff) % CODE_CHARS.length()));
        }
        return out.toString();
    }

    private void record(String storeId, String automationId, String action, String detail)
            throws IOException {
        AuditEvent e = new AuditEvent();
        e.setId(Ids.next());
        e.setStoreId(storeId == null ? "" : storeId);
        e.setUsername("automation");
        e.setAction(Codes.cut(action, 64));
        e.setSubject(automationId);
        e.setDetail(Codes.cut(detail, 500));
        e.setCreatedAt(Instant.now());
        audits.insert(e);
    }

    /// [#record] in a transaction of its own, for a failure noted after the
    /// one that failed rolled back.
    @Transactional(rollbackFor = IOException.class)
    private void note(String storeId, String automationId, String action, String detail) {
        try {
            record(storeId, automationId, action, detail);
        } catch (IOException | RuntimeException ignored) {
            System.err.println("[automation] " + action + " " + detail);
        }
    }

    private static long number(String text) {
        try {
            return text == null || text.length() == 0 ? 0 : Long.parseLong(text);
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    private static String text(String text) {
        return text == null ? "" : text.trim();
    }

    // -------------------------------------------------------------- merchant

    public List<NodeTypeDto> catalog(String username, String storeId) throws IOException {
        requireRead(username, storeId);
        return NodeCatalog.all();
    }

    @Transactional(readOnly = true)
    private void requireRead(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
    }

    @Transactional(readOnly = true)
    public List<AutomationDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<Automation> all = automations.findByStoreIdOrderByNameAsc(storeId);
        List<AutomationDto> out = new ArrayList<AutomationDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(dto(all.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public AutomationDto get(String username, String storeId, String automationId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return dto(mine(storeId, automationId));
    }

    public ValidationDto validate(String username, String storeId, AutomationDto wanted)
            throws IOException {
        requireRead(username, storeId);
        return check(wanted == null ? null : wanted.graphJson());
    }

    /// What is wrong with a graph.
    public static ValidationDto check(String graphJson) {
        AutomationGraph graph = AutomationGraph.parse(graphJson);
        List<String> errors = graph.validate();
        return new ValidationDto(errors.isEmpty(), errors, graph.triggerKind());
    }

    @Transactional(rollbackFor = IOException.class)
    public AutomationDto create(String username, String storeId, AutomationDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Automation a = new Automation();
        a.setId(Ids.next());
        a.setStoreId(storeId);
        apply(a, wanted);
        automations.insert(a);
        record(storeId, a.getId(), "AUTOMATION_CREATED", username + " made " + a.getName());
        return dto(a);
    }

    @Transactional(rollbackFor = IOException.class)
    public AutomationDto save(String username, String storeId, String automationId,
            AutomationDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Automation a = mine(storeId, automationId);
        apply(a, wanted);
        automations.save(a);
        record(storeId, a.getId(), "AUTOMATION_SAVED", username + " saved " + a.getName());
        return dto(a);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<AutomationDto> remove(String username, String storeId, String automationId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Automation a = mine(storeId, automationId);
        List<AutomationRun> waiting = runs.findByAutomationId(automationId);
        for (int iter = 0; iter < waiting.size(); iter++) {
            runs.delete(waiting.get(iter));
        }
        automations.delete(a);
        return list(username, storeId);
    }

    @Transactional(readOnly = true)
    public List<AuditDto> activity(String username, String storeId, String automationId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        mine(storeId, automationId);
        List<AuditEvent> all = audits.findByStoreIdAndSubjectOrderByCreatedAtDesc(storeId,
                automationId);
        List<AuditDto> out = new ArrayList<AuditDto>();
        for (int iter = 0; iter < all.size() && iter < 200; iter++) {
            AuditEvent e = all.get(iter);
            out.add(new AuditDto(e.getId(), e.getUsername(), e.getAction(), e.getSubject(),
                    e.getDetail(), e.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    private Automation mine(String storeId, String automationId) {
        Automation a = automationId == null ? null : automations.findById(automationId);
        if (a == null || !a.getStoreId().equals(storeId)) {
            throw new ResponseStatusException(404, "No such automation");
        }
        return a;
    }

    /// Takes the name, the graph and whether it is on. An active automation
    /// must validate; an inactive one is a draft and may be incomplete.
    private static void apply(Automation a, AutomationDto wanted) {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String name = wanted.name() == null ? "" : wanted.name().trim();
        if (name.length() == 0 || name.length() > 120) {
            throw new ResponseStatusException(400, "A name is 1 to 120 characters");
        }
        String json = wanted.graphJson() == null ? "" : wanted.graphJson();
        if (json.length() > 200000) {
            throw new ResponseStatusException(400, "The graph is too large");
        }
        ValidationDto v = check(json);
        if (wanted.active() && !v.valid()) {
            throw new ResponseStatusException(400, v.errors().get(0));
        }
        a.setName(name);
        a.setGraphJson(json);
        a.setTriggerKind(Codes.cut(v.triggerKind(), 64));
        a.setActive(wanted.active());
        a.setUpdatedAt(Instant.now());
    }

    static AutomationDto dto(Automation a) {
        return new AutomationDto(a.getId(), a.getName(), a.getTriggerKind(), a.getGraphJson(),
                a.isActive(), a.getRuns(), a.getLastRunAt().toEpochMilli(),
                a.getUpdatedAt().toEpochMilli());
    }

    // ------------------------------------------------------------------ demo

    /// The two demo automations of a store, unless it has automations
    /// already: orders over $200 tag the order and the customer VIP; stock
    /// running low tells the staff and drafts a purchase order.
    @Transactional(rollbackFor = IOException.class)
    public void seedDemo(String storeId) throws IOException {
        if (!automations.findByStoreIdOrderByNameAsc(storeId).isEmpty()) {
            return;
        }
        demo(storeId, "VIP orders over $200", DemoGraphs.vip(20000));
        demo(storeId, "Low stock: notify and reorder", DemoGraphs.lowStock());
    }

    private void demo(String storeId, String name, String json) throws IOException {
        Automation a = new Automation();
        a.setId(Ids.next());
        a.setStoreId(storeId);
        apply(a, new AutomationDto(null, name, "", json, true, 0, 0, 0));
        automations.insert(a);
    }
}
