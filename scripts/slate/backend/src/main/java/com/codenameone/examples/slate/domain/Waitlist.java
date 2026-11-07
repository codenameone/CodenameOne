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

/// A client waiting for a time that is full. When one comes free that suits
/// them, it is held for them and they are told.
@Entity
public class Waitlist {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String serviceId = "";

    /// Empty for anyone.
    @Column(nullable = false, type = "VARCHAR(190)")
    private String provider = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String client = "";

    @Column(nullable = false)
    private long fromDay;

    @Column(nullable = false)
    private long toDay;

    @Column(nullable = false)
    private int fromMinute;

    @Column(nullable = false)
    private int toMinute;

    @Column(nullable = false, type = "VARCHAR(12)")
    private String state = "";

    /// The held appointment offered, while OFFERED.
    @Column(nullable = false, type = "VARCHAR(32)")
    private String offered = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    public Waitlist() {
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public long getFromDay() {
        return fromDay;
    }

    public void setFromDay(long fromDay) {
        this.fromDay = fromDay;
    }

    public long getToDay() {
        return toDay;
    }

    public void setToDay(long toDay) {
        this.toDay = toDay;
    }

    public int getFromMinute() {
        return fromMinute;
    }

    public void setFromMinute(int fromMinute) {
        this.fromMinute = fromMinute;
    }

    public int getToMinute() {
        return toMinute;
    }

    public void setToMinute(int toMinute) {
        this.toMinute = toMinute;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getOffered() {
        return offered;
    }

    public void setOffered(String offered) {
        this.offered = offered;
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
