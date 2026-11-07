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
package com.codenameone.examples.slate.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import java.time.Instant;

/// A booking, from the moment a slot is held to its end.
///
/// HELD keeps the slot for `holdExpiresAt` while the client finishes; then
/// CONFIRMED, and finally COMPLETED, NO_SHOW or CANCELLED. `blockStart` and
/// `blockEnd` are the time it takes off the schedule, buffers included;
/// `startsAt` and `endsAt` are what the client is told.
@Entity
public class Appointment {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String serviceId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String locationId = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String client = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String clientName = "";

    @Column(nullable = false)
    private Instant startsAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant endsAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant blockStart = Instant.EPOCH;

    @Column(nullable = false)
    private Instant blockEnd = Instant.EPOCH;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String state = "";

    @Column(nullable = false)
    private Instant holdExpiresAt = Instant.EPOCH;

    @Column(nullable = false)
    private long priceCents;

    @Column(nullable = false)
    private long depositCents;

    @Column(nullable = false, type = "VARCHAR(3)")
    private String currency = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String methodId = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String paymentRef = "";

    /// NONE, PENDING (to authorize nearer the day), AUTHORIZED, CAPTURED, RELEASED or FAILED.
    @Column(nullable = false, type = "VARCHAR(16)")
    private String paymentState = "";

    @Column(nullable = false, type = "VARCHAR(1000)")
    private String note = "";

    /// For a group service: the service, provider and start every seat in that slot shares.
    @Column(nullable = false, type = "VARCHAR(120)")
    private String groupKey = "";

    @Column(nullable = false)
    private boolean online;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String meetingUrl = "";

    /// Names the appointment's public calendar file, for guests.
    @Column(nullable = false, type = "VARCHAR(32)")
    private String shareToken = "";

    @Column(nullable = false)
    private Instant remindedAt = Instant.EPOCH;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String rescheduledFrom = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String cancelReason = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    public Appointment() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public Instant getBlockStart() {
        return blockStart;
    }

    public void setBlockStart(Instant blockStart) {
        this.blockStart = blockStart;
    }

    public Instant getBlockEnd() {
        return blockEnd;
    }

    public void setBlockEnd(Instant blockEnd) {
        this.blockEnd = blockEnd;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(Instant holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public long getDepositCents() {
        return depositCents;
    }

    public void setDepositCents(long depositCents) {
        this.depositCents = depositCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMethodId() {
        return methodId;
    }

    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public String getPaymentState() {
        return paymentState;
    }

    public void setPaymentState(String paymentState) {
        this.paymentState = paymentState;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public Instant getRemindedAt() {
        return remindedAt;
    }

    public void setRemindedAt(Instant remindedAt) {
        this.remindedAt = remindedAt;
    }

    public String getRescheduledFrom() {
        return rescheduledFrom;
    }

    public void setRescheduledFrom(String rescheduledFrom) {
        this.rescheduledFrom = rescheduledFrom;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
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
}
