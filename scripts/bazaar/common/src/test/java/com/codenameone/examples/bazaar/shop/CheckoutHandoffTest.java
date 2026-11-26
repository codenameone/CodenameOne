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
package com.codenameone.examples.bazaar.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.CheckoutFieldDto;
import com.codenameone.examples.bazaar.api.CheckoutOptionsDto;
import com.codenameone.examples.bazaar.api.PaymentTypeDto;
import com.codenameone.examples.bazaar.api.TipOptionDto;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/// When the app hands a checkout to the hosted page, without a display.
class CheckoutHandoffTest {
    private static final String URL = "https://shop.example/s/demo/checkout";

    @Test
    void cardsOnlyStaysInTheApp() {
        CheckoutOptionsDto o = new CheckoutOptionsDto("simulated",
                Arrays.asList(new PaymentTypeDto("card", "Card", false)),
                Collections.<TipOptionDto>emptyList(), Collections.<CheckoutFieldDto>emptyList(),
                URL);
        assertFalse(CheckoutHandoff.offersMore(o));
        assertFalse(CheckoutHandoff.required(o));
        assertFalse(CheckoutHandoff.offersMore(null));
    }

    @Test
    void payLaterTipsAndQuestionsOfferTheHostedPage() {
        CheckoutOptionsDto klarna = new CheckoutOptionsDto("stripe",
                Arrays.asList(new PaymentTypeDto("card", "Card", false),
                    new PaymentTypeDto("klarna", "Klarna", true)),
                Collections.<TipOptionDto>emptyList(), Collections.<CheckoutFieldDto>emptyList(),
                URL);
        assertTrue(CheckoutHandoff.offersMore(klarna));
        CheckoutOptionsDto tips = new CheckoutOptionsDto("stripe",
                Collections.<PaymentTypeDto>emptyList(),
                Arrays.asList(new TipOptionDto(1, "15%", 300)),
                Collections.<CheckoutFieldDto>emptyList(), URL);
        assertTrue(CheckoutHandoff.offersMore(tips));
        CheckoutOptionsDto noUrl = new CheckoutOptionsDto("stripe",
                Collections.<PaymentTypeDto>emptyList(),
                Arrays.asList(new TipOptionDto(1, "15%", 300)),
                Collections.<CheckoutFieldDto>emptyList(), "");
        assertFalse(CheckoutHandoff.offersMore(noUrl));
    }

    @Test
    void aRequiredQuestionKeepsTheAppFromPlacing() {
        CheckoutOptionsDto o = new CheckoutOptionsDto("stripe",
                Collections.<PaymentTypeDto>emptyList(), Collections.<TipOptionDto>emptyList(),
                Arrays.asList(new CheckoutFieldDto("f1", "Gift message", "text", "", false),
                    new CheckoutFieldDto("f2", "Delivery window", "select", "AM\nPM", true)),
                URL);
        assertTrue(CheckoutHandoff.required(o));
        assertTrue(CheckoutHandoff.offersMore(o));
    }
}
