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

package com.codenameone.examples.bazaar.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// One variant on an order, at the price it was sold for.
@Entity
public class OrderLine {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String orderId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String productId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String variantId = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String title = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String variantTitle = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String sku = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String photoId = "";

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int fulfilledQty;

    @Column(nullable = false)
    private int refundedQty;

    @Column(nullable = false)
    private long priceCents;

    @Column(nullable = false)
    private long discountCents;

    @Column(nullable = false)
    private long taxCents;

    /// The rate the line was taxed at in basis points; -1 for a line sold
    /// before rates were kept per line.
    @Column(nullable = false)
    private long taxBp = -1;

    @Column(nullable = false)
    private int weightGrams;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String locationId = "";

    @Column(nullable = false)
    private boolean preorder;

    /// Units of `quantity` sold past zero: the variant sells when out of
    /// stock and these were not on hand when the order was placed.
    @Column(nullable = false)
    private int backorderedQty;

    public OrderLine() {
    }

    public int getBackorderedQty() {
        return backorderedQty;
    }

    public void setBackorderedQty(int backorderedQty) {
        this.backorderedQty = backorderedQty;
    }

    public boolean isPreorder() {
        return preorder;
    }

    public void setPreorder(boolean preorder) {
        this.preorder = preorder;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVariantTitle() {
        return variantTitle;
    }

    public void setVariantTitle(String variantTitle) {
        this.variantTitle = variantTitle;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getFulfilledQty() {
        return fulfilledQty;
    }

    public void setFulfilledQty(int fulfilledQty) {
        this.fulfilledQty = fulfilledQty;
    }

    public int getRefundedQty() {
        return refundedQty;
    }

    public void setRefundedQty(int refundedQty) {
        this.refundedQty = refundedQty;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public void setDiscountCents(long discountCents) {
        this.discountCents = discountCents;
    }

    public long getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(long taxCents) {
        this.taxCents = taxCents;
    }

    public long getTaxBp() {
        return taxBp;
    }

    public void setTaxBp(long taxBp) {
        this.taxBp = taxBp;
    }

    public int getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(int weightGrams) {
        this.weightGrams = weightGrams;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
}
