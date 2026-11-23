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

/// A shop on the platform: its storefront address, currency and look.
@Entity
public class Store {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String handle = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String tagline = "";

    @Column(nullable = false, type = "VARCHAR(2000)")
    private String description = "";

    @Column(nullable = false, type = "VARCHAR(3)")
    private String currency = "";

    @Column(nullable = false)
    private int accentColor;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String logoPhotoId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String heroPhotoId = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String ownerUsername = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String status = "";

    @Column(nullable = false)
    private boolean taxInclusive;

    @Column(nullable = false, type = "VARCHAR(190)")
    private String email = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String phone = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String address = "";

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(nullable = false, type = "TEXT")
    private String policies = "";

    /// Search synonym groups, one group a line, words split by commas: a
    /// search for any word of a group also finds the others.
    @Column(nullable = false, type = "VARCHAR(2000)")
    private String searchSynonyms = "";

    /// The pre-launch password; empty leaves the storefront open. Kept as
    /// typed, like a door code: it guards a launch date, not an account.
    @Column(nullable = false, type = "VARCHAR(100)")
    private String storefrontPassword = "";

    /// The store's own hostname (`shop.example.com`), lower case, or null
    /// to serve only under `/s/{handle}`. Unique across stores.
    @Column(type = "VARCHAR(253)")
    private String customDomain;

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    public Store() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
    }

    public String getLogoPhotoId() {
        return logoPhotoId;
    }

    public void setLogoPhotoId(String logoPhotoId) {
        this.logoPhotoId = logoPhotoId;
    }

    public String getHeroPhotoId() {
        return heroPhotoId;
    }

    public void setHeroPhotoId(String heroPhotoId) {
        this.heroPhotoId = heroPhotoId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isTaxInclusive() {
        return taxInclusive;
    }

    public void setTaxInclusive(boolean taxInclusive) {
        this.taxInclusive = taxInclusive;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getPolicies() {
        return policies;
    }

    public void setPolicies(String policies) {
        this.policies = policies;
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

    public String getSearchSynonyms() {
        return searchSynonyms;
    }

    public void setSearchSynonyms(String searchSynonyms) {
        this.searchSynonyms = searchSynonyms == null ? "" : searchSynonyms;
    }

    public String getStorefrontPassword() {
        return storefrontPassword;
    }

    public void setStorefrontPassword(String storefrontPassword) {
        this.storefrontPassword = storefrontPassword == null ? "" : storefrontPassword;
    }

    public String getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain == null || customDomain.length() == 0 ? null
                : customDomain;
    }
}
