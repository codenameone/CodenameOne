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
package com.codenameone.examples.bazaar.checkout;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.BuyNowDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.UpsellDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Payment;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreUpsell;
import com.codenameone.examples.bazaar.domain.UpsellOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreUpsellRepository;
import com.codenameone.examples.bazaar.repository.UpsellOrderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.List;

/// Post-purchase upsell and one-tap "Buy now".
///
/// A store names one product ([#set]); the order-placed page of a signed-in
/// shopper offers it ([#offer]) and one tap takes it ([#accept]). The add-on
/// is a separate small order -- the paid original is never touched -- placed
/// through [Checkouts#place] like any checkout, so stock is reserved, tax and
/// shipping are quoted and the card is authorized the normal way, with the
/// payment method the original order used. Both orders carry a note naming
/// the other, and `upsell_order` keeps the link so the offer is taken once.
///
/// [#buyNow] is the same single-line order from a product page, to a saved
/// address with the account's default card.
@Component
public class Upsells {
    private final StoreUpsellRepository upsells;
    private final UpsellOrderRepository taken;
    private final StoreOrderRepository orders;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final PaymentRepository payments;
    private final Carts carts;
    private final Checkouts checkouts;
    private final StoreAccess access;
    private final Orders orderService;

    public Upsells(StoreUpsellRepository upsells, UpsellOrderRepository taken,
            StoreOrderRepository orders, ProductRepository products, VariantRepository variants,
            PaymentRepository payments, Carts carts, Checkouts checkouts, StoreAccess access,
            Orders orderService) {
        this.upsells = upsells;
        this.taken = taken;
        this.orders = orders;
        this.products = products;
        this.variants = variants;
        this.payments = payments;
        this.carts = carts;
        this.checkouts = checkouts;
        this.access = access;
        this.orderService = orderService;
    }

    /// The store's upsell product, `productId` empty for none.
    public UpsellDto get(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        StoreUpsell u = upsells.findById(storeId);
        return new UpsellDto(u == null ? "" : u.getProductId(), "", 0, 0);
    }

    /// Names the store's upsell product, or none for an empty id.
    public UpsellDto set(String username, String storeId, UpsellDto upsell) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        String productId = upsell == null || upsell.productId() == null ? ""
                : upsell.productId().trim();
        StoreUpsell kept = upsells.findById(storeId);
        if (productId.length() == 0) {
            if (kept != null) {
                upsells.deleteById(storeId);
            }
            return get(username, storeId);
        }
        Product p = products.findById(productId);
        if (p == null || !storeId.equals(p.getStoreId())) {
            throw new ResponseStatusException(404, "No such product");
        }
        if (kept == null) {
            StoreUpsell u = new StoreUpsell();
            u.setStoreId(storeId);
            u.setProductId(productId);
            upsells.insert(u);
        } else {
            kept.setProductId(productId);
            upsells.save(kept);
        }
        return get(username, storeId);
    }

    /// The offer on `orderId` for its own shopper: the product, or the add-on
    /// order's number once taken. Null when there is nothing to offer -- no
    /// upsell set, the product is not for sale, it is already in the order,
    /// or the order was not paid with a saved card.
    public UpsellDto offer(String username, String storeId, String orderId) throws IOException {
        StoreOrder order = own(username, storeId, orderId);
        UpsellOrder done = taken.findById(order.getId());
        if (done != null) {
            StoreOrder addOn = orders.findById(done.getOrderId());
            return new UpsellDto("", "", 0, addOn == null ? 0 : addOn.getNumber());
        }
        Variant v = offered(storeId);
        if (v == null || method(order) == null) {
            return null;
        }
        Product p = products.findById(v.getProductId());
        return new UpsellDto(p.getId(), p.getTitle(), v.getPriceCents(), 0);
    }

    /// Takes the offer on `orderId`: a new order for one unit of the upsell
    /// product, same address, same saved card. A second tap answers the same
    /// add-on order.
    public OrderDto accept(String username, String storeId, String orderId) throws IOException {
        StoreOrder order = own(username, storeId, orderId);
        UpsellOrder done = taken.findById(order.getId());
        if (done != null) {
            return checkouts.placedBefore(username, "upsell-" + order.getId());
        }
        Variant v = offered(storeId);
        String method = method(order);
        if (v == null || method == null) {
            throw new ResponseStatusException(409, "This offer is no longer available");
        }
        OrderDto source = orderService.detailOf(order.getId());
        OrderDto placed = single(username, storeId, v.getId(), 1, source.address(), method,
                order.getEmail(), "upsell-" + order.getId(),
                "Added after checkout to order #" + order.getNumber());
        UpsellOrder link = new UpsellOrder();
        link.setOriginalId(order.getId());
        link.setOrderId(placed.id());
        link.setStoreId(storeId);
        taken.insert(link);
        StoreOrder fresh = orders.findById(order.getId());
        String note = fresh.getNote() == null ? "" : fresh.getNote();
        String line = "Add-on order #" + placed.number() + " placed after checkout";
        fresh.setNote(note.length() == 0 ? line : cut(note + "\n" + line));
        orders.save(fresh);
        return placed;
    }

    /// One tap from a product page: a single-line order to a saved address
    /// with the default card, never touching the shopper's cart.
    public OrderDto buyNow(String username, String storeId, BuyNowDto buy) throws IOException {
        if (username == null) {
            throw new ResponseStatusException(401, "Sign in to buy in one tap");
        }
        if (buy == null || buy.variantId() == null || buy.address() == null) {
            throw new ResponseStatusException(400, "Say what to buy and where it goes");
        }
        Variant v = variants.findById(buy.variantId());
        if (v == null || !storeId.equals(v.getStoreId())) {
            throw new ResponseStatusException(404, "No such product");
        }
        String key = buy.idempotencyKey() == null || buy.idempotencyKey().trim().length() == 0
                ? null : "buy-now-" + buy.idempotencyKey().trim();
        return single(username, storeId, v.getId(), Math.max(1, buy.quantity()), buy.address(),
                "", username, key, "");
    }

    private OrderDto single(String username, String storeId, String variantId, int quantity,
            AddressDto address, String method, String email, String key, String note)
            throws IOException {
        // A cart of its own, so the shopper's cart is left as it is.
        Cart scratch = carts.cart(storeId, null, null);
        carts.add(storeId, null, scratch.getToken(), new CartLineChangeDto(variantId, quantity));
        Cart cart = carts.cart(storeId, null, scratch.getToken());
        // No rate id: the quote picks the cheapest delivery to that address.
        return checkouts.place(username, storeId, key, new PlaceOrderDto(address, null, method,
                email, note, key, 0, null), cart, null, null, null);
    }

    private StoreOrder own(String username, String storeId, String orderId) throws IOException {
        StoreOrder order = orderId == null ? null : orders.findById(orderId);
        if (username == null || order == null || !storeId.equals(order.getStoreId())
                || !username.equals(order.getUsername())) {
            throw new ResponseStatusException(404, "No such order");
        }
        return order;
    }

    private Variant offered(String storeId) throws IOException {
        StoreUpsell u = upsells.findById(storeId);
        Product p = u == null ? null : products.findById(u.getProductId());
        if (p == null || !Catalog.ACTIVE.equals(p.getStatus())) {
            return null;
        }
        List<Variant> vs = variants.findByProductIdOrderByPositionAsc(p.getId());
        return vs.isEmpty() ? null : vs.get(0);
    }

    /// The saved card the order was paid with, or null for a typed card, a
    /// wallet or a manual method -- those cannot be charged again unasked.
    private String method(StoreOrder order) throws IOException {
        Payment p = payments.findById(order.getId());
        return p == null || p.getMethodId() == null || p.getMethodId().length() == 0 ? null
                : p.getMethodId();
    }

    private static String cut(String text) {
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }
}
