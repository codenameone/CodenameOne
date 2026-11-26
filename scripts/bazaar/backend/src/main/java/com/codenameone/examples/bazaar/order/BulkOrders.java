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
package com.codenameone.examples.bazaar.order;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BulkActionDto;
import com.codenameone.examples.bazaar.api.BulkActionResultDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderNoteDto;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// One action on many orders from the order list: fulfil, buy labels, print
/// packing slips, mark paid, archive, tag. Each order is done on its own and
/// answered on its own, so one that cannot be (already paid, nothing to send)
/// is reported and the rest still go through. Every order goes through the
/// same call a single order would, with the same access check.
@Component
public class BulkOrders {
    public static final String FULFIL = "FULFIL";
    public static final String LABELS = "LABELS";
    public static final String PACKING_SLIPS = "PACKING_SLIPS";
    public static final String MARK_PAID = "MARK_PAID";
    public static final String ARCHIVE = "ARCHIVE";
    public static final String UNARCHIVE = "UNARCHIVE";
    public static final String TAG = "TAG";
    /// Most orders in one action.
    public static final int MAX = 200;

    private final StoreAccess access;
    private final Orders orders;
    private final Fulfillments fulfillments;
    private final StoreOrderRepository orderRows;

    public BulkOrders(StoreAccess access, Orders orders, Fulfillments fulfillments,
            StoreOrderRepository orderRows) {
        this.access = access;
        this.orders = orders;
        this.fulfillments = fulfillments;
        this.orderRows = orderRows;
    }

    public List<BulkActionResultDto> run(String username, String storeId, BulkActionDto req)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        if (req == null || req.orderIds() == null || req.orderIds().isEmpty()) {
            throw new ResponseStatusException(400, "Choose at least one order");
        }
        if (req.orderIds().size() > MAX) {
            throw new ResponseStatusException(400, "At most " + MAX + " orders at once");
        }
        String action = req.action() == null ? "" : req.action();
        String tag = req.tag() == null ? "" : req.tag().trim();
        if (TAG.equals(action) && (tag.length() == 0 || tag.length() > 40
                || tag.indexOf(',') >= 0)) {
            throw new ResponseStatusException(400, "A tag is 1 to 40 characters, with no comma");
        }
        if (!FULFIL.equals(action) && !LABELS.equals(action) && !PACKING_SLIPS.equals(action)
                && !MARK_PAID.equals(action) && !ARCHIVE.equals(action)
                && !UNARCHIVE.equals(action) && !TAG.equals(action)) {
            throw new ResponseStatusException(400, "No such action");
        }
        List<BulkActionResultDto> out = new ArrayList<BulkActionResultDto>();
        List<String> slips = new ArrayList<String>();
        for (int iter = 0; iter < req.orderIds().size(); iter++) {
            String id = req.orderIds().get(iter);
            StoreOrder o = id == null || id.length() > 32 ? null : orderRows.findById(id);
            if (o == null || !storeId.equals(o.getStoreId())) {
                out.add(new BulkActionResultDto(id, 0, false, "No such order"));
                continue;
            }
            try {
                String done = one(username, storeId, o, action, tag);
                if (PACKING_SLIPS.equals(action)) {
                    slips.add(o.getId());
                }
                out.add(new BulkActionResultDto(o.getId(), o.getNumber(), true, done));
            } catch (ResponseStatusException refused) {
                if (refused.getStatus() == 403) {
                    throw refused;
                }
                out.add(new BulkActionResultDto(o.getId(), o.getNumber(), false,
                        refused.getMessage()));
            }
        }
        if (!slips.isEmpty()) {
            // One document for every slip: the answer's rows all point at it.
            String url = slipsUrl(storeId, slips);
            for (int iter = 0; iter < out.size(); iter++) {
                BulkActionResultDto r = out.get(iter);
                if (r.ok()) {
                    out.set(iter, new BulkActionResultDto(r.orderId(), r.number(), true, url));
                }
            }
        }
        return out;
    }

    /// The document holding the packing slips of `orderIds`.
    public static String slipsUrl(String storeId, List<String> orderIds) {
        StringBuilder ids = new StringBuilder();
        for (int iter = 0; iter < orderIds.size(); iter++) {
            ids.append(iter == 0 ? "" : ",").append(orderIds.get(iter));
        }
        return "/api/merchant/stores/" + storeId + "/bulk-orders/packing-slips.pdf?orders=" + ids;
    }

    private String one(String username, String storeId, StoreOrder o, String action, String tag)
            throws IOException {
        if (FULFIL.equals(action)) {
            int n = fulfillments.fulfilAll(username, storeId, o.getId()).size();
            return n == 1 ? "Shipped" : "Shipped in " + n + " parcels";
        }
        if (LABELS.equals(action)) {
            List<String> waiting = fulfillments.needingLabels(storeId, o.getId());
            if (waiting.isEmpty()) {
                throw new ResponseStatusException(409, "No parcel is waiting for a label");
            }
            for (int iter = 0; iter < waiting.size(); iter++) {
                fulfillments.buyLabel(username, storeId, waiting.get(iter),
                        new LabelRequestDto(null));
            }
            return waiting.size() == 1 ? "Label bought" : waiting.size() + " labels bought";
        }
        if (PACKING_SLIPS.equals(action)) {
            access.requireStaff(username, storeId, StoreAccess.ANYONE);
            return "Packing slip";
        }
        if (MARK_PAID.equals(action)) {
            orders.markPaid(username, storeId, o.getId());
            return "Marked paid";
        }
        if (ARCHIVE.equals(action) || UNARCHIVE.equals(action)) {
            orders.archive(username, storeId, o.getId(), ARCHIVE.equals(action));
            return ARCHIVE.equals(action) ? "Archived" : "Unarchived";
        }
        OrderDto now = orders.detail(username, storeId, o.getId());
        String tags = now.tags() == null ? "" : now.tags().trim();
        if (!hasTag(tags, tag)) {
            String joined = tags.length() == 0 ? tag : tags + ", " + tag;
            if (joined.length() > 500) {
                throw new ResponseStatusException(409, "The order has too many tags");
            }
            orders.note(username, storeId, o.getId(), new OrderNoteDto(null, joined));
        }
        return "Tagged " + tag;
    }

    /// Whether the comma separated `tags` already holds `tag`, ignoring ASCII
    /// case.
    public static boolean hasTag(String tags, String tag) {
        int start = 0;
        for (int iter = 0; iter <= tags.length(); iter++) {
            if (iter == tags.length() || tags.charAt(iter) == ',') {
                if (tags.substring(start, iter).trim().equalsIgnoreCase(tag)) {
                    return true;
                }
                start = iter + 1;
            }
        }
        return false;
    }
}
