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
package com.codenameone.examples.fieldline.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// A pay rate technicians are costed at: an hourly rate, the uplift for
/// overtime and weekend hours, and the fee an urgent call-out adds.
@Entity
public class LabourRate {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(40)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(80)")
    private String name = "";

    @Column(nullable = false)
    private int hourlyCents;

    @Column(nullable = false)
    private int overtimePct = 150;

    @Column(nullable = false)
    private int weekendPct = 200;

    @Column(nullable = false)
    private int calloutCents;

    public LabourRate() {
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

    public int getHourlyCents() {
        return hourlyCents;
    }

    public void setHourlyCents(int hourlyCents) {
        this.hourlyCents = hourlyCents;
    }

    public int getOvertimePct() {
        return overtimePct;
    }

    public void setOvertimePct(int overtimePct) {
        this.overtimePct = overtimePct;
    }

    public int getWeekendPct() {
        return weekendPct;
    }

    public void setWeekendPct(int weekendPct) {
        this.weekendPct = weekendPct;
    }

    public int getCalloutCents() {
        return calloutCents;
    }

    public void setCalloutCents(int calloutCents) {
        this.calloutCents = calloutCents;
    }
}
