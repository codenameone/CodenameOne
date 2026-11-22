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

/// A stored-value card, sold or issued as store credit.
@Entity
public class GiftCard {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String code = "";

    @Column(nullable = false, type = "VARCHAR(4)")
    private String lastFour = "";

    @Column(nullable = false)
    private long initialCents;

    @Column(nullable = false)
    private long balanceCents;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String customerId = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String note = "";

    @Column(nullable = false)
    private Instant expiresAt = Instant.EPOCH;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    /// Where the card is mailed, with the message, on [#getDeliverAt]; empty for none.
    @Column(nullable = false, type = "VARCHAR(190)")
    private String recipientEmail = "";

    @Column(nullable = false, type = "VARCHAR(80)")
    private String recipientName = "";

    @Column(nullable = false, type = "VARCHAR(500)")
    private String message = "";

    /// When the card is mailed to the recipient; `Instant.EPOCH` for at once.
    @Column(nullable = false)
    private Instant deliverAt = Instant.EPOCH;

    /// When it was mailed; `Instant.EPOCH` while it waits.
    @Column(nullable = false)
    private Instant deliveredAt = Instant.EPOCH;

    public GiftCard() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLastFour() {
        return lastFour;
    }

    public void setLastFour(String lastFour) {
        this.lastFour = lastFour;
    }

    public long getInitialCents() {
        return initialCents;
    }

    public void setInitialCents(long initialCents) {
        this.initialCents = initialCents;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public void setBalanceCents(long balanceCents) {
        this.balanceCents = balanceCents;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getDeliverAt() {
        return deliverAt;
    }

    public void setDeliverAt(Instant deliverAt) {
        this.deliverAt = deliverAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
