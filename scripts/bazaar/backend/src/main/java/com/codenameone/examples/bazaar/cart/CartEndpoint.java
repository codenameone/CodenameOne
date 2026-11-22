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

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.CartApiServer;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CodeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;

/// The server's half of `CartApi`. The shop paths take guests, so the caller
/// is optional everywhere but [#merge].
@Component
public class CartEndpoint implements CartApiServer {
    private final Carts carts;

    public CartEndpoint(Carts carts) {
        this.carts = carts;
    }

    @Override
    public CartDto get(String storeId, String token) throws Exception {
        return carts.get(storeId, Codes.callerOrNull(), token);
    }

    @Override
    public CartDto add(String storeId, String token, CartLineChangeDto body) throws Exception {
        return carts.add(storeId, Codes.callerOrNull(), token, body);
    }

    @Override
    public CartDto update(String storeId, String lineId, String token, CartLineChangeDto body)
            throws Exception {
        return carts.update(storeId, Codes.callerOrNull(), token, lineId,
                body == null ? 0 : body.quantity());
    }

    @Override
    public CartDto remove(String storeId, String lineId, String token) throws Exception {
        return carts.update(storeId, Codes.callerOrNull(), token, lineId, 0);
    }

    @Override
    public CartDto applyDiscount(String storeId, String token, CodeDto body) throws Exception {
        return carts.applyDiscount(storeId, Codes.callerOrNull(), token,
                body == null ? null : body.code());
    }

    @Override
    public CartDto removeDiscount(String storeId, String token) throws Exception {
        return carts.removeDiscount(storeId, Codes.callerOrNull(), token);
    }

    @Override
    public CartDto applyGiftCard(String storeId, String token, CodeDto body) throws Exception {
        return carts.applyGiftCard(storeId, Codes.callerOrNull(), token,
                body == null ? null : body.code());
    }

    @Override
    public CartDto useStoreCredit(String storeId, String token) throws Exception {
        return carts.useStoreCredit(storeId, signedIn(), token, true);
    }

    @Override
    public CartDto stopStoreCredit(String storeId, String token) throws Exception {
        return carts.useStoreCredit(storeId, signedIn(), token, false);
    }

    /// Store credit belongs to an account: a guest has none to use.
    private static String signedIn() {
        String me = Codes.callerOrNull();
        if (me == null) {
            throw new com.codename1.backend.ResponseStatusException(401,
                    "Sign in to use your store credit");
        }
        return me;
    }

    @Override
    public CartDto removeGiftCard(String storeId, String token) throws Exception {
        return carts.removeGiftCard(storeId, Codes.callerOrNull(), token);
    }

    @Override
    public CartDto estimate(String storeId, String token, EstimateDto body) throws Exception {
        return carts.estimate(storeId, Codes.callerOrNull(), token, body);
    }

    @Override
    public CartDto merge(String storeId, String token) throws Exception {
        return carts.merge(storeId, Caller.name(), token);
    }
}
