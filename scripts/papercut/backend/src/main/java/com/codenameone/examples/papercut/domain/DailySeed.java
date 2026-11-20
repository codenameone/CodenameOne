/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import java.time.Instant;

/// An admin's pick of seed for one UTC day, keyed by the date (`2026-11-20`).
@Entity
public class DailySeed {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(10)")
    private String day = "";

    @Column(nullable = false)
    private long seed;

    @Column(nullable = false, type = "VARCHAR(190)")
    private String setBy = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    public DailySeed() {
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public String getSetBy() {
        return setBy;
    }

    public void setSetBy(String setBy) {
        this.setBy = setBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
