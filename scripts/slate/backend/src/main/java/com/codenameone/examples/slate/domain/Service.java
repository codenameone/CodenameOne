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

/// Something that can be booked: a cut, an hour of tutoring, a consultation.
///
/// Its time on the schedule is the buffer before, its minutes and the buffer
/// after; the client is shown only the minutes. `capacity` above 1 makes it a
/// group (a class, a workshop) that several clients book into the same slot.
/// `providersNeeded` above 1 needs that many providers at once (a two-person
/// consultation). A `resourceKind` needs one free resource of that kind at the
/// location (a chair, a room).
@Entity
public class Service {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(80)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(1000)")
    private String description = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String category = "";

    @Column(nullable = false)
    private int minutes;

    @Column(nullable = false)
    private long priceCents;

    @Column(nullable = false)
    private int bufferBefore;

    @Column(nullable = false)
    private int bufferAfter;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int providersNeeded;

    @Column(nullable = false, type = "VARCHAR(40)")
    private String resourceKind = "";

    @Column(nullable = false)
    private boolean online;

    @Column(nullable = false)
    private int minNoticeMinutes;

    @Column(nullable = false)
    private int depositPercent;

    @Column(nullable = false, type = "VARCHAR(8)")
    private String color = "";

    /// A question asked when booking, or empty.
    @Column(nullable = false, type = "VARCHAR(255)")
    private String intakePrompt = "";

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int sortOrder;

    public Service() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public int getBufferBefore() {
        return bufferBefore;
    }

    public void setBufferBefore(int bufferBefore) {
        this.bufferBefore = bufferBefore;
    }

    public int getBufferAfter() {
        return bufferAfter;
    }

    public void setBufferAfter(int bufferAfter) {
        this.bufferAfter = bufferAfter;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getProvidersNeeded() {
        return providersNeeded;
    }

    public void setProvidersNeeded(int providersNeeded) {
        this.providersNeeded = providersNeeded;
    }

    public String getResourceKind() {
        return resourceKind;
    }

    public void setResourceKind(String resourceKind) {
        this.resourceKind = resourceKind;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public int getMinNoticeMinutes() {
        return minNoticeMinutes;
    }

    public void setMinNoticeMinutes(int minNoticeMinutes) {
        this.minNoticeMinutes = minNoticeMinutes;
    }

    public int getDepositPercent() {
        return depositPercent;
    }

    public void setDepositPercent(int depositPercent) {
        this.depositPercent = depositPercent;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIntakePrompt() {
        return intakePrompt;
    }

    public void setIntakePrompt(String intakePrompt) {
        this.intakePrompt = intakePrompt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
