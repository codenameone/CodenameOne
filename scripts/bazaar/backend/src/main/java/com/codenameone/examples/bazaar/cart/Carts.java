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
package com.codenameone.examples.bazaar.cart;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.notify.ShopperLanguage;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.CartLine;
import com.codenameone.examples.bazaar.domain.GiftCard;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.repository.CartLineRepository;
import com.codenameone.examples.bazaar.repository.CartRepository;
import com.codenameone.examples.bazaar.repository.GiftCardRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// Carts: one per signed-in shopper and store, or a guest's found by its
/// token.
///
/// Every method takes the shopper's `username`, null for a guest, and the
/// guest's `token`, null when they have none yet. A signed-in shopper's token
/// is ignored here; [#merge] is how a guest cart joins the account. Every
/// answer is the whole cart priced again ([Quoter]), so the app never adds
/// up anything itself.
@Component
public class Carts {
    /// The most units of one variant a cart holds.
    public static final int MAX_QUANTITY = 999;
    /// The most lines a cart holds.
    public static final int MAX_LINES = 100;

    private final CartRepository carts;
    private final CartLineRepository lines;
    private final VariantRepository variants;
    private final StoreRepository stores;
    private final GiftCardRepository giftCards;
    private final Quoter quoter;
    private final com.codenameone.examples.bazaar.catalog.Schedules schedules;

    public Carts(CartRepository carts, CartLineRepository lines, VariantRepository variants,
            StoreRepository stores, GiftCardRepository giftCards, Quoter quoter,
            com.codenameone.examples.bazaar.catalog.Schedules schedules) {
        this.schedules = schedules;
        this.carts = carts;
        this.lines = lines;
        this.variants = variants;
        this.stores = stores;
        this.giftCards = giftCards;
        this.quoter = quoter;
    }

    public CartDto get(String storeId, String username, String token) throws IOException {
        return quoter.price(cart(storeId, username, token), null, null, username).toDto();
    }

    public CartDto add(String storeId, String username, String token, CartLineChangeDto change)
            throws IOException {
        Cart cart = addLine(storeId, username, token, change);
        return quoter.price(cart, null, null, username).toDto();
    }

    @Transactional(rollbackFor = IOException.class)
    private Cart addLine(String storeId, String username, String token, CartLineChangeDto change)
            throws IOException {
        if (change == null || change.variantId() == null || change.quantity() <= 0) {
            throw new ResponseStatusException(400, "Say what to add and how many");
        }
        Cart cart = cart(storeId, username, token);
        Variant variant = variants.findById(change.variantId());
        if (variant == null || !storeId.equals(variant.getStoreId())
                || schedules.isVariantHidden(variant.getId(),
                    com.codenameone.examples.bazaar.catalog.Schedules.ONLINE)) {
            // Outside its online store window a product cannot be found, so
            // it cannot be added either.
            throw new ResponseStatusException(404, "No such item");
        }
        List<CartLine> have = lines.findByCartId(cart.getId());
        for (int iter = 0; iter < have.size(); iter++) {
            CartLine line = have.get(iter);
            if (line.getVariantId().equals(variant.getId())) {
                line.setQuantity(Math.min(MAX_QUANTITY, line.getQuantity() + change.quantity()));
                lines.save(line);
                touch(cart);
                return cart;
            }
        }
        if (have.size() >= MAX_LINES) {
            throw new ResponseStatusException(409, "The cart is full");
        }
        CartLine line = new CartLine();
        line.setId(Ids.next());
        line.setCartId(cart.getId());
        line.setVariantId(variant.getId());
        line.setQuantity(Math.min(MAX_QUANTITY, change.quantity()));
        line.setAddedAt(Instant.now());
        lines.insert(line);
        touch(cart);
        return cart;
    }

    public CartDto update(String storeId, String username, String token, String lineId,
            int quantity) throws IOException {
        Cart cart = setQuantity(storeId, username, token, lineId, quantity);
        return quoter.price(cart, null, null, username).toDto();
    }

    @Transactional(rollbackFor = IOException.class)
    private Cart setQuantity(String storeId, String username, String token, String lineId,
            int quantity) throws IOException {
        Cart cart = cart(storeId, username, token);
        CartLine line = lineId == null ? null : lines.findById(lineId);
        if (line == null || !cart.getId().equals(line.getCartId())) {
            throw new ResponseStatusException(404, "No such line");
        }
        if (quantity <= 0) {
            lines.delete(line);
        } else {
            line.setQuantity(Math.min(MAX_QUANTITY, quantity));
            lines.save(line);
        }
        touch(cart);
        return cart;
    }

    public CartDto applyDiscount(String storeId, String username, String token, String code)
            throws IOException {
        Cart cart = setCodes(storeId, username, token, Codes.cut(Codes.normalize(code), 64), null);
        return quoter.price(cart, null, null, username).toDto();
    }

    public CartDto removeDiscount(String storeId, String username, String token)
            throws IOException {
        Cart cart = setCodes(storeId, username, token, "", null);
        return quoter.price(cart, null, null, username).toDto();
    }

    /// Answers 404 for a code that is no gift card of this store's, so a
    /// typo is told at once; the balance check is rate limited like the
    /// lookup.
    public CartDto applyGiftCard(String storeId, String username, String token, String code)
            throws IOException {
        String clean = Codes.normalize(code);
        GiftCard card = clean.length() == 0 ? null : giftCards.findByCode(clean);
        if (card == null || !storeId.equals(card.getStoreId())) {
            throw new ResponseStatusException(404, "That is not a gift card of this store");
        }
        Cart cart = setCodes(storeId, username, token, null, clean);
        return quoter.price(cart, null, null, username).toDto();
    }

    public CartDto removeGiftCard(String storeId, String username, String token)
            throws IOException {
        Cart cart = setCodes(storeId, username, token, null, "");
        return quoter.price(cart, null, null, username).toDto();
    }

    @Transactional(rollbackFor = IOException.class)
    private Cart setCodes(String storeId, String username, String token, String discount,
            String gift) throws IOException {
        Cart cart = cart(storeId, username, token);
        if (discount != null) {
            cart.setDiscountCode(discount);
        }
        if (gift != null) {
            cart.setGiftCardCode(gift);
        }
        touch(cart);
        return cart;
    }

    public CartDto estimate(String storeId, String username, String token, EstimateDto estimate)
            throws IOException {
        Cart cart = cart(storeId, username, token);
        return quoter.price(cart, estimate == null ? null : estimate.address(),
                estimate == null ? null : estimate.shippingRateId(), username).toDto();
    }

    /// Moves the guest cart `token` into `username`'s cart at the store:
    /// quantities add up where both hold a variant, and the guest's codes are
    /// kept where the account's cart has none. The guest cart is deleted.
    public CartDto merge(String storeId, String username, String token) throws IOException {
        Cart cart = mergeInto(storeId, username, token);
        return quoter.price(cart, null, null, username).toDto();
    }

    @Transactional(rollbackFor = IOException.class)
    private Cart mergeInto(String storeId, String username, String token) throws IOException {
        if (username == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        Cart mine = cart(storeId, username, null);
        Cart guest = token == null || token.length() == 0 ? null : carts.findByToken(token);
        if (guest == null || guest.getUsername().length() > 0 || guest.getId().equals(mine.getId())
                || !storeId.equals(guest.getStoreId())) {
            return mine;
        }
        List<CartLine> from = lines.findByCartId(guest.getId());
        List<CartLine> into = lines.findByCartId(mine.getId());
        for (int iter = 0; iter < from.size(); iter++) {
            CartLine add = from.get(iter);
            CartLine same = null;
            for (int j = 0; j < into.size(); j++) {
                if (into.get(j).getVariantId().equals(add.getVariantId())) {
                    same = into.get(j);
                }
            }
            if (same != null) {
                same.setQuantity(Math.min(MAX_QUANTITY, same.getQuantity() + add.getQuantity()));
                lines.save(same);
                lines.delete(add);
            } else if (into.size() < MAX_LINES) {
                add.setCartId(mine.getId());
                lines.save(add);
                into.add(add);
            } else {
                lines.delete(add);
            }
        }
        if (mine.getDiscountCode().length() == 0) {
            mine.setDiscountCode(guest.getDiscountCode());
        }
        if (mine.getGiftCardCode().length() == 0) {
            mine.setGiftCardCode(guest.getGiftCardCode());
        }
        if (mine.getEmail().length() == 0) {
            mine.setEmail(guest.getEmail());
        }
        carts.delete(guest);
        touch(mine);
        return mine;
    }

    /// Empties a cart after its order was placed: the lines and the codes go.
    @Transactional(rollbackFor = IOException.class)
    public void clear(String cartId) throws IOException {
        lines.deleteByCartId(cartId);
        Cart cart = carts.findById(cartId);
        if (cart != null) {
            cart.setDiscountCode("");
            cart.setGiftCardCode("");
            touch(cart);
        }
    }

    /// The cart for the shopper, made when they have none.
    @Transactional(rollbackFor = IOException.class)
    public Cart cart(String storeId, String username, String token) throws IOException {
        Store store = storeId == null ? null : stores.findById(storeId);
        if (store == null || !Quoter.ACTIVE.equals(store.getStatus())) {
            throw new ResponseStatusException(404, "No such store");
        }
        Cart cart;
        if (username != null) {
            cart = carts.findByStoreIdAndUsername(storeId, username);
        } else {
            cart = token == null || token.length() == 0 ? null : carts.findByToken(token);
            if (cart != null && (cart.getUsername().length() > 0
                    || !storeId.equals(cart.getStoreId()))) {
                cart = null;
            }
        }
        if (cart == null) {
            cart = new Cart();
            cart.setId(Ids.next());
            cart.setStoreId(storeId);
            cart.setUsername(username == null ? "" : username);
            // Every cart gets a token, a signed-in one too: the column is
            // unique, and an empty one would be shared.
            cart.setToken(Ids.next());
            cart.setLanguage(ShopperLanguage.fromRequest());
            cart.setCreatedAt(Instant.now());
            cart.setUpdatedAt(Instant.now());
            carts.insert(cart);
        }
        return cart;
    }

    /// Units of `variantId` still sellable ahead of stock ([Quoter#preorderAllowance]).
    public int preorderAllowance(String variantId) throws IOException {
        return quoter.preorderAllowance(variantId);
    }

    /// The guest cart token a reminder's recovery `token` restores in a web
    /// session, or null when it names no cart of this store. A guest cart is
    /// answered as it is; a signed-in shopper's cart is never handed to a
    /// session by its token (the web cart is the guest one), so its lines are
    /// copied into a fresh guest cart instead and the account's cart is left
    /// untouched.
    @Transactional(rollbackFor = IOException.class)
    public String recover(String storeId, String token) throws IOException {
        Cart found = token == null || token.length() == 0 || token.length() > 64 ? null
                : carts.findByToken(token);
        if (found == null || !found.getStoreId().equals(storeId)) {
            return null;
        }
        if (found.getUsername().length() == 0) {
            return found.getToken();
        }
        Cart guest = cart(storeId, null, null);
        List<CartLine> have = lines.findByCartId(found.getId());
        for (int iter = 0; iter < have.size() && iter < MAX_LINES; iter++) {
            CartLine line = new CartLine();
            line.setId(Ids.next());
            line.setCartId(guest.getId());
            line.setVariantId(have.get(iter).getVariantId());
            line.setQuantity(have.get(iter).getQuantity());
            line.setAddedAt(Instant.now());
            lines.insert(line);
        }
        return guest.getToken();
    }

    private void touch(Cart cart) {
        cart.setUpdatedAt(Instant.now());
        // The language the shopper is filling it in, for its reminder.
        String language = ShopperLanguage.fromRequest();
        if (language.length() > 0) {
            cart.setLanguage(language);
        }
        carts.save(cart);
    }
}
