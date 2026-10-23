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

/// An ad campaign, its creative and its budget.
@Entity
public class AdCampaign {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String advertiser = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String creativeUrl = "";

    @Column(nullable = false)
    private int durationSeconds;

    @Column(nullable = false)
    private int skippableAfterSeconds;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String clickUrl = "";

    @Column(nullable = false)
    private long cpmCents;

    @Column(nullable = false)
    private long budgetCents;

    @Column(nullable = false)
    private long spentMillicents;

    @Column(nullable = false)
    private long impressions;

    @Column(nullable = false)
    private long clicks;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String genres = "";

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private long startsAt;

    @Column(nullable = false)
    private long endsAt;

    @Column(nullable = false)
    private long createdAt;

    public AdCampaign() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdvertiser() {
        return advertiser;
    }

    public void setAdvertiser(String advertiser) {
        this.advertiser = advertiser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreativeUrl() {
        return creativeUrl;
    }

    public void setCreativeUrl(String creativeUrl) {
        this.creativeUrl = creativeUrl;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getSkippableAfterSeconds() {
        return skippableAfterSeconds;
    }

    public void setSkippableAfterSeconds(int skippableAfterSeconds) {
        this.skippableAfterSeconds = skippableAfterSeconds;
    }

    public String getClickUrl() {
        return clickUrl;
    }

    public void setClickUrl(String clickUrl) {
        this.clickUrl = clickUrl;
    }

    public long getCpmCents() {
        return cpmCents;
    }

    public void setCpmCents(long cpmCents) {
        this.cpmCents = cpmCents;
    }

    public long getBudgetCents() {
        return budgetCents;
    }

    public void setBudgetCents(long budgetCents) {
        this.budgetCents = budgetCents;
    }

    public long getSpentMillicents() {
        return spentMillicents;
    }

    public void setSpentMillicents(long spentMillicents) {
        this.spentMillicents = spentMillicents;
    }

    public long getImpressions() {
        return impressions;
    }

    public void setImpressions(long impressions) {
        this.impressions = impressions;
    }

    public long getClicks() {
        return clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = clicks;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(long startsAt) {
        this.startsAt = startsAt;
    }

    public long getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(long endsAt) {
        this.endsAt = endsAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
