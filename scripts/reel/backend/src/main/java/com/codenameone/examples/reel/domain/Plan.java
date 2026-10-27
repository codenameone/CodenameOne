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
package com.codenameone.examples.reel.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// A subscription plan.
@Entity
public class Plan {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String code = "";

    @Column(nullable = false, type = "VARCHAR(60)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String description = "";

    @Column(nullable = false)
    private long priceCents;

    @Column(nullable = false, type = "VARCHAR(3)")
    private String currency = "";

    @Column(nullable = false, type = "VARCHAR(8)")
    private String billingInterval = "";

    @Column(nullable = false)
    private int maxStreams;

    @Column(nullable = false, type = "VARCHAR(8)")
    private String maxQuality = "";

    @Column(nullable = false)
    private boolean adSupported;

    @Column(nullable = false)
    private boolean downloads;

    @Column(nullable = false)
    private int trialDays;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int position;

    public Plan() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBillingInterval() {
        return billingInterval;
    }

    public void setBillingInterval(String billingInterval) {
        this.billingInterval = billingInterval;
    }

    public int getMaxStreams() {
        return maxStreams;
    }

    public void setMaxStreams(int maxStreams) {
        this.maxStreams = maxStreams;
    }

    public String getMaxQuality() {
        return maxQuality;
    }

    public void setMaxQuality(String maxQuality) {
        this.maxQuality = maxQuality;
    }

    public boolean isAdSupported() {
        return adSupported;
    }

    public void setAdSupported(boolean adSupported) {
        this.adSupported = adSupported;
    }

    public boolean isDownloads() {
        return downloads;
    }

    public void setDownloads(boolean downloads) {
        this.downloads = downloads;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(int trialDays) {
        this.trialDays = trialDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
