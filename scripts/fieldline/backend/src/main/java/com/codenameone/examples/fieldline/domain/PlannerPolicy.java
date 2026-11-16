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

/// The company's planning rules, one row: the meal break every technician
/// takes, how the planner weighs what it trades off, and the SLA a new job
/// gets by priority. Admins edit it on the Rules page; every run reads it.
@Entity
public class PlannerPolicy {
    public static final String ID = "default";

    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(40)")
    private String id = ID;

    public PlannerPolicy() {
    }

    @Column(nullable = false)
    private int breakMinutes = 30;

    @Column(nullable = false)
    private int breakAfterMinutes = 180;

    @Column(nullable = false)
    private int breakBeforeMinutes = 300;

    @Column(nullable = false)
    private boolean strictTerritories;

    @Column(nullable = false)
    private int travelWeight = 10;

    @Column(nullable = false)
    private int overtimeWeight = 15;

    @Column(nullable = false)
    private int latenessWeight = 30;

    @Column(nullable = false)
    private int continuityWeight = 300;

    @Column(nullable = false)
    private int balanceWeight = 40;

    @Column(nullable = false)
    private int territoryWeight = 400;

    @Column(nullable = false)
    private int urgentSlaHours = 4;

    @Column(nullable = false)
    private int routineSlaHours = 72;

    @Column(nullable = false)
    private int rushAmFrom = 7 * 60;

    @Column(nullable = false)
    private int rushAmTo = 9 * 60 + 30;

    @Column(nullable = false)
    private int rushPmFrom = 16 * 60;

    @Column(nullable = false)
    private int rushPmTo = 18 * 60 + 30;

    @Column(nullable = false)
    private int rushExtraPct = 25;

    @Column(nullable = false)
    private int timeRoundingMinutes = 15;

    @Column(nullable = false)
    private int autoClockOutHours = 12;

    @Column(nullable = false)
    private long updatedAt;

    public int getBreakMinutes() {
        return breakMinutes;
    }

    public void setBreakMinutes(int breakMinutes) {
        this.breakMinutes = breakMinutes;
    }

    public int getBreakAfterMinutes() {
        return breakAfterMinutes;
    }

    public void setBreakAfterMinutes(int breakAfterMinutes) {
        this.breakAfterMinutes = breakAfterMinutes;
    }

    public int getBreakBeforeMinutes() {
        return breakBeforeMinutes;
    }

    public void setBreakBeforeMinutes(int breakBeforeMinutes) {
        this.breakBeforeMinutes = breakBeforeMinutes;
    }

    public boolean isStrictTerritories() {
        return strictTerritories;
    }

    public void setStrictTerritories(boolean strictTerritories) {
        this.strictTerritories = strictTerritories;
    }

    public int getTravelWeight() {
        return travelWeight;
    }

    public void setTravelWeight(int travelWeight) {
        this.travelWeight = travelWeight;
    }

    public int getOvertimeWeight() {
        return overtimeWeight;
    }

    public void setOvertimeWeight(int overtimeWeight) {
        this.overtimeWeight = overtimeWeight;
    }

    public int getLatenessWeight() {
        return latenessWeight;
    }

    public void setLatenessWeight(int latenessWeight) {
        this.latenessWeight = latenessWeight;
    }

    public int getContinuityWeight() {
        return continuityWeight;
    }

    public void setContinuityWeight(int continuityWeight) {
        this.continuityWeight = continuityWeight;
    }

    public int getBalanceWeight() {
        return balanceWeight;
    }

    public void setBalanceWeight(int balanceWeight) {
        this.balanceWeight = balanceWeight;
    }

    public int getTerritoryWeight() {
        return territoryWeight;
    }

    public void setTerritoryWeight(int territoryWeight) {
        this.territoryWeight = territoryWeight;
    }

    public int getUrgentSlaHours() {
        return urgentSlaHours;
    }

    public void setUrgentSlaHours(int urgentSlaHours) {
        this.urgentSlaHours = urgentSlaHours;
    }

    public int getRoutineSlaHours() {
        return routineSlaHours;
    }

    public void setRoutineSlaHours(int routineSlaHours) {
        this.routineSlaHours = routineSlaHours;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRushAmFrom() {
        return rushAmFrom;
    }

    public void setRushAmFrom(int rushAmFrom) {
        this.rushAmFrom = rushAmFrom;
    }

    public int getRushAmTo() {
        return rushAmTo;
    }

    public void setRushAmTo(int rushAmTo) {
        this.rushAmTo = rushAmTo;
    }

    public int getRushPmFrom() {
        return rushPmFrom;
    }

    public void setRushPmFrom(int rushPmFrom) {
        this.rushPmFrom = rushPmFrom;
    }

    public int getRushPmTo() {
        return rushPmTo;
    }

    public void setRushPmTo(int rushPmTo) {
        this.rushPmTo = rushPmTo;
    }

    public int getRushExtraPct() {
        return rushExtraPct;
    }

    public void setRushExtraPct(int rushExtraPct) {
        this.rushExtraPct = rushExtraPct;
    }

    public int getTimeRoundingMinutes() {
        return timeRoundingMinutes;
    }

    public void setTimeRoundingMinutes(int timeRoundingMinutes) {
        this.timeRoundingMinutes = timeRoundingMinutes;
    }

    public int getAutoClockOutHours() {
        return autoClockOutHours;
    }

    public void setAutoClockOutHours(int autoClockOutHours) {
        this.autoClockOutHours = autoClockOutHours;
    }
}
