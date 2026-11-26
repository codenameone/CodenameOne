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

/// One buyable combination of a product's options, with its own SKU, barcode and price.
@Entity
public class Variant {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String productId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String title = "";

    @Column(nullable = false, type = "VARCHAR(80)")
    private String option1 = "";

    @Column(nullable = false, type = "VARCHAR(80)")
    private String option2 = "";

    @Column(nullable = false, type = "VARCHAR(80)")
    private String option3 = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String sku = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String barcode = "";

    @Column(nullable = false)
    private long priceCents;

    @Column(nullable = false)
    private long compareAtCents;

    @Column(nullable = false)
    private long costCents;

    @Column(nullable = false)
    private int weightGrams;

    @Column(nullable = false)
    private boolean taxable;

    @Column(nullable = false)
    private boolean trackInventory;

    @Column(nullable = false)
    private boolean allowBackorder;

    @Column(nullable = false)
    private int position;

    /// Delivered as a download: needs no shipping.
    @Column(nullable = false)
    private boolean digital;

    /// Harmonized System code for customs; empty to use the product's.
    @Column(nullable = false, type = "VARCHAR(16)")
    private String hsCode = "";

    /// Two letter country of origin for customs.
    @Column(nullable = false, type = "VARCHAR(2)")
    private String originCountry = "";

    /// The measure sold, in thousandths of [#unitName]: 750000 for 750 ml.
    @Column(nullable = false)
    private long unitQuantity;

    /// The measure's unit, such as `ml`, `g` or `m`.
    @Column(nullable = false, type = "VARCHAR(8)")
    private String unitName = "";

    /// The quantity a unit price is shown per, such as 1 for "per 1 l".
    @Column(nullable = false)
    private int unitReference;

    /// The unit the unit price is shown per.
    @Column(nullable = false, type = "VARCHAR(8)")
    private String unitReferenceName = "";

    public Variant() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOption1() {
        return option1;
    }

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public String getOption2() {
        return option2;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public String getOption3() {
        return option3;
    }

    public void setOption3(String option3) {
        this.option3 = option3;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public long getCompareAtCents() {
        return compareAtCents;
    }

    public void setCompareAtCents(long compareAtCents) {
        this.compareAtCents = compareAtCents;
    }

    public long getCostCents() {
        return costCents;
    }

    public void setCostCents(long costCents) {
        this.costCents = costCents;
    }

    public int getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(int weightGrams) {
        this.weightGrams = weightGrams;
    }

    public boolean isTaxable() {
        return taxable;
    }

    public void setTaxable(boolean taxable) {
        this.taxable = taxable;
    }

    public boolean isTrackInventory() {
        return trackInventory;
    }

    public void setTrackInventory(boolean trackInventory) {
        this.trackInventory = trackInventory;
    }

    public boolean isAllowBackorder() {
        return allowBackorder;
    }

    public void setAllowBackorder(boolean allowBackorder) {
        this.allowBackorder = allowBackorder;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
    public boolean isDigital() {
        return digital;
    }

    public void setDigital(boolean digital) {
        this.digital = digital;
    }

    public String getHsCode() {
        return hsCode;
    }

    public void setHsCode(String hsCode) {
        this.hsCode = hsCode;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public long getUnitQuantity() {
        return unitQuantity;
    }

    public void setUnitQuantity(long unitQuantity) {
        this.unitQuantity = unitQuantity;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public int getUnitReference() {
        return unitReference;
    }

    public void setUnitReference(int unitReference) {
        this.unitReference = unitReference;
    }

    public String getUnitReferenceName() {
        return unitReferenceName;
    }

    public void setUnitReferenceName(String unitReferenceName) {
        this.unitReferenceName = unitReferenceName;
    }
}
