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

/// A way to get an order to a zone and what it costs.
@Entity
public class ShippingRate {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String zoneId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String kind = "";

    @Column(nullable = false)
    private long priceCents;

    @Column(nullable = false)
    private long minValue;

    @Column(nullable = false)
    private long maxValue;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String carrier = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String service = "";

    @Column(nullable = false)
    private int etaDaysMin;

    @Column(nullable = false)
    private int etaDaysMax;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String locationId = "";

    @Column(nullable = false)
    private int radiusMeters;

    @Column(nullable = false)
    private int position;

    /// CARRIER rates: hundredths of a percent added to the carrier's price.
    @Column(nullable = false)
    private int markupBps;

    /// CARRIER rates: the price when the carrier cannot answer; 0 = none.
    @Column(nullable = false)
    private long fallbackCents;

    public ShippingRate() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public long getMinValue() {
        return minValue;
    }

    public void setMinValue(long minValue) {
        this.minValue = minValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getEtaDaysMin() {
        return etaDaysMin;
    }

    public void setEtaDaysMin(int etaDaysMin) {
        this.etaDaysMin = etaDaysMin;
    }

    public int getEtaDaysMax() {
        return etaDaysMax;
    }

    public void setEtaDaysMax(int etaDaysMax) {
        this.etaDaysMax = etaDaysMax;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public int getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(int radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getMarkupBps() {
        return markupBps;
    }

    public void setMarkupBps(int markupBps) {
        this.markupBps = markupBps;
    }

    public long getFallbackCents() {
        return fallbackCents;
    }

    public void setFallbackCents(long fallbackCents) {
        this.fallbackCents = fallbackCents;
    }
}
