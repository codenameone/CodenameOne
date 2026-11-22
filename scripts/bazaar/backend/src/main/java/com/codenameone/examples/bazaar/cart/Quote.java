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

import com.codenameone.examples.bazaar.api.AppliedDiscountDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.GiftCard;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.ShippingRate;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.pricing.TaxCalculator;

import java.util.ArrayList;
import java.util.List;

/// A cart priced: what [Quoter] worked out, kept whole so that checkout
/// places exactly what the shopper was shown.
public final class Quote {
    /// One cart line with what it was priced from.
    public static final class Line {
        public String cartLineId;
        public Variant variant;
        public Product product;
        public PriceLine price;
        /// Units the store can supply; [Integer#MAX_VALUE] when untracked.
        public int availableQty;
        /// Sold ahead of stock: nothing is reserved for it at checkout.
        public boolean preorder;
        /// More than is on hand, of a variant that keeps selling when out of
        /// stock: it ships when the shelf is filled again.
        public boolean backordered;

        public boolean available() {
            return availableQty >= price.quantity;
        }
    }

    public Store store;
    public Cart cart;
    public final List<Line> lines = new ArrayList<Line>();
    public DiscountEngine.Result discounts;
    public List<ShippingCalculator.Option> options = new ArrayList<ShippingCalculator.Option>();
    public ShippingCalculator.Option shipping;
    public TaxCalculator.Result tax;
    public GiftCard giftCard;
    public long subtotalCents;
    public long shippingCents;
    public long totalCents;
    public long giftCardCents;
    /// The tax exemption certificate that took the tax off, or null.
    public String exemptCertificate;
    /// In a tax-inclusive store, the tax inside the prices that an exemption
    /// took off the total; counted with the discounts.
    public long exemptCents;
    /// The import duties a buyer may pay on delivery (Markets#dutyEstimate):
    /// shown, never charged.
    public long dutyCents;
    public final List<String> problems = new ArrayList<String>();

    /// False when every line is a digital variant: nothing to ship, so no
    /// address, delivery option or shipping charge.
    public boolean shippingRequired() {
        for (int iter = 0; iter < lines.size(); iter++) {
            if (!lines.get(iter).variant.isDigital()) {
                return true;
            }
        }
        return lines.isEmpty();
    }

    public List<PriceLine> priceLines() {
        List<PriceLine> out = new ArrayList<PriceLine>();
        for (int iter = 0; iter < lines.size(); iter++) {
            out.add(lines.get(iter).price);
        }
        return out;
    }

    public long discountCents() {
        return (discounts == null ? 0 : discounts.totalCents) + exemptCents;
    }

    public long taxCents() {
        return tax == null ? 0 : tax.totalCents();
    }

    public long dueCents() {
        return totalCents - giftCardCents;
    }

    /// The code of the first discount applied that has one, or empty.
    public String discountCode() {
        if (discounts != null) {
            for (int iter = 0; iter < discounts.applied.size(); iter++) {
                String code = discounts.applied.get(iter).discount.getCode();
                if (code != null && code.length() > 0) {
                    return code;
                }
            }
        }
        return "";
    }

    public CartDto toDto() {
        List<CartLineDto> out = new ArrayList<CartLineDto>();
        for (int iter = 0; iter < lines.size(); iter++) {
            Line line = lines.get(iter);
            out.add(new CartLineDto(line.cartLineId, line.variant.getId(), line.product.getId(),
                    line.product.getTitle(), line.variant.getTitle(), line.variant.getSku(),
                    line.product.getPhotoId(), line.price.quantity, line.price.unitCents,
                    line.price.discountCents, line.price.taxCents, line.available(),
                    line.availableQty == Integer.MAX_VALUE ? line.price.quantity + 999
                            : Math.max(0, line.availableQty), line.preorder,
                    line.backordered));
        }
        List<AppliedDiscountDto> applied = new ArrayList<AppliedDiscountDto>();
        if (discounts != null) {
            for (int iter = 0; iter < discounts.applied.size(); iter++) {
                DiscountEngine.Applied a = discounts.applied.get(iter);
                applied.add(new AppliedDiscountDto(a.discount.getId(), a.discount.getCode(),
                        a.discount.getTitle(), a.amountCents, a.freeShipping));
            }
        }
        List<ShippingOptionDto> opts = new ArrayList<ShippingOptionDto>();
        for (int iter = 0; iter < options.size(); iter++) {
            opts.add(option(options.get(iter)));
        }
        return new CartDto(cart.getId(), store.getId(), cart.getToken(), store.getCurrency(), out,
                cart.getDiscountCode(), cart.getGiftCardCode(), subtotalCents, discountCents(),
                shippingCents, taxCents(), store.isTaxInclusive(), totalCents, giftCardCents,
                dueCents(), discounts != null && discounts.freeShipping, applied, opts,
                shipping == null ? null : shipping.id, problems, shippingRequired());
    }

    static ShippingOptionDto option(ShippingCalculator.Option option) {
        ShippingRate r = option.rate;
        return new ShippingOptionDto(option.id, option.name, r.getKind(), option.priceCents,
                option.etaDaysMin, option.etaDaysMax, r.getLocationId());
    }
}
