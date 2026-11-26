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
package com.codenameone.examples.bazaar.giftcard;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.GiftCardApiServer;
import com.codenameone.examples.bazaar.api.GiftCardBalanceDto;
import com.codenameone.examples.bazaar.api.GiftCardDto;

import java.util.List;

/// The server's half of `GiftCardApi`.
@Component
public class GiftCardEndpoint implements GiftCardApiServer {
    private final GiftCards cards;

    public GiftCardEndpoint(GiftCards cards) {
        this.cards = cards;
    }

    @Override
    public List<GiftCardDto> list(String storeId) throws Exception {
        return cards.list(Caller.name(), storeId);
    }

    @Override
    public GiftCardDto issue(String storeId, GiftCardDto body) throws Exception {
        return cards.issue(Caller.name(), storeId, body);
    }

    @Override
    public GiftCardDto disable(String storeId, String cardId) throws Exception {
        return cards.disable(Caller.name(), storeId, cardId);
    }

    @Override
    public GiftCardBalanceDto balance(String code) throws Exception {
        return cards.balance(code);
    }
}
