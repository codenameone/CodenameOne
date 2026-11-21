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
import java.time.Instant;

/// An order, from the online store, a register or a draft a merchant wrote. Amounts are cents in the store's currency.
@Entity
public class StoreOrder {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false)
    private int number;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String customerId = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String email = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String phone = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String channel = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String status = "";

    @Column(nullable = false, type = "VARCHAR(24)")
    private String financial = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String fulfilment = "";

    @Column(nullable = false)
    private long subtotalCents;

    @Column(nullable = false)
    private long discountCents;

    @Column(nullable = false)
    private long shippingCents;

    @Column(nullable = false)
    private long taxCents;

    @Column(nullable = false)
    private long totalCents;

    @Column(nullable = false)
    private long refundedCents;

    /// A tip taken with the card at the counter: paid, but not a sale -- no
    /// tax, no discount, no commission, and outside [#getTotalCents].
    @Column(nullable = false)
    private long tipCents;

    @Column(nullable = false, type = "VARCHAR(3)")
    private String currency = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String discountCode = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String shipName = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String shipLine1 = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String shipLine2 = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String shipCity = "";

    @Column(nullable = false, type = "VARCHAR(80)")
    private String shipRegion = "";

    @Column(nullable = false, type = "VARCHAR(20)")
    private String shipPostal = "";

    @Column(nullable = false, type = "VARCHAR(2)")
    private String shipCountry = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String shipPhone = "";

    @Column(nullable = false)
    private double shipLat;

    @Column(nullable = false)
    private double shipLng;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String shippingRateId = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String shippingTitle = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String locationId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String registerId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String shiftId = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String staffUsername = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String clientId = "";

    @Column(nullable = false, type = "VARCHAR(1000)")
    private String note = "";

    @Column(nullable = false, type = "VARCHAR(500)")
    private String tags = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant closedAt = Instant.EPOCH;

    public StoreOrder() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFinancial() {
        return financial;
    }

    public void setFinancial(String financial) {
        this.financial = financial;
    }

    public String getFulfilment() {
        return fulfilment;
    }

    public void setFulfilment(String fulfilment) {
        this.fulfilment = fulfilment;
    }

    public long getSubtotalCents() {
        return subtotalCents;
    }

    public void setSubtotalCents(long subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public void setDiscountCents(long discountCents) {
        this.discountCents = discountCents;
    }

    public long getShippingCents() {
        return shippingCents;
    }

    public void setShippingCents(long shippingCents) {
        this.shippingCents = shippingCents;
    }

    public long getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(long taxCents) {
        this.taxCents = taxCents;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    public long getTipCents() {
        return tipCents;
    }

    public void setTipCents(long tipCents) {
        this.tipCents = tipCents;
    }

    public long getRefundedCents() {
        return refundedCents;
    }

    public void setRefundedCents(long refundedCents) {
        this.refundedCents = refundedCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
    }

    public String getShipName() {
        return shipName;
    }

    public void setShipName(String shipName) {
        this.shipName = shipName;
    }

    public String getShipLine1() {
        return shipLine1;
    }

    public void setShipLine1(String shipLine1) {
        this.shipLine1 = shipLine1;
    }

    public String getShipLine2() {
        return shipLine2;
    }

    public void setShipLine2(String shipLine2) {
        this.shipLine2 = shipLine2;
    }

    public String getShipCity() {
        return shipCity;
    }

    public void setShipCity(String shipCity) {
        this.shipCity = shipCity;
    }

    public String getShipRegion() {
        return shipRegion;
    }

    public void setShipRegion(String shipRegion) {
        this.shipRegion = shipRegion;
    }

    public String getShipPostal() {
        return shipPostal;
    }

    public void setShipPostal(String shipPostal) {
        this.shipPostal = shipPostal;
    }

    public String getShipCountry() {
        return shipCountry;
    }

    public void setShipCountry(String shipCountry) {
        this.shipCountry = shipCountry;
    }

    public String getShipPhone() {
        return shipPhone;
    }

    public void setShipPhone(String shipPhone) {
        this.shipPhone = shipPhone;
    }

    public double getShipLat() {
        return shipLat;
    }

    public void setShipLat(double shipLat) {
        this.shipLat = shipLat;
    }

    public double getShipLng() {
        return shipLng;
    }

    public void setShipLng(double shipLng) {
        this.shipLng = shipLng;
    }

    public String getShippingRateId() {
        return shippingRateId;
    }

    public void setShippingRateId(String shippingRateId) {
        this.shippingRateId = shippingRateId;
    }

    public String getShippingTitle() {
        return shippingTitle;
    }

    public void setShippingTitle(String shippingTitle) {
        this.shippingTitle = shippingTitle;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getStaffUsername() {
        return staffUsername;
    }

    public void setStaffUsername(String staffUsername) {
        this.staffUsername = staffUsername;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
