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

/// The business this server books for, and the rules every booking follows
/// unless a service says otherwise. There is one row, `main`.
///
/// The vertical -- SALON, TUTOR, TRAINER, CONSULTANT, CLINIC -- changes only
/// what things are called and the demo data; the booking rules are the same
/// for all of them.
@Entity
public class Business {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String vertical = "";

    @Column(nullable = false, type = "VARCHAR(1000)")
    private String about = "";

    /// The zone the business's days are counted in, and its default for a location.
    @Column(nullable = false, type = "VARCHAR(64)")
    private String timeZone = "";

    @Column(nullable = false, type = "VARCHAR(3)")
    private String currency = "";

    /// How far apart the start times offered are.
    @Column(nullable = false)
    private int stepMinutes;

    /// How long before it starts a booking can still be made.
    @Column(nullable = false)
    private int minNoticeMinutes;

    /// How far ahead a booking can be made.
    @Column(nullable = false)
    private int horizonDays;

    /// How long a slot is kept for someone who is still booking it.
    @Column(nullable = false)
    private int holdMinutes;

    /// Cancelling later than this before the start forfeits the deposit.
    @Column(nullable = false)
    private int cancelHours;

    /// Moving a booking later than this before the start is refused.
    @Column(nullable = false)
    private int rescheduleHours;

    /// The part of the price authorized on the card when booking.
    @Column(nullable = false)
    private int depositPercent;

    /// The part of the price taken when the client does not come.
    @Column(nullable = false)
    private int noShowFeePercent;

    /// The least time between two bookings of one client.
    @Column(nullable = false)
    private int clientGapMinutes;

    /// The most bookings one client may hold on one day; 0 is no limit.
    @Column(nullable = false)
    private int clientPerDay;

    /// A slot that would leave a provider a free gap shorter than this, and not empty, is not offered.
    @Column(nullable = false)
    private int minUsableGapMinutes;

    /// ANY offers every free start; TIGHT offers only starts that touch a booking or the edge of a shift, when there is one.
    @Column(nullable = false, type = "VARCHAR(16)")
    private String packing = "";

    /// How long before the start the client is reminded.
    @Column(nullable = false)
    private int reminderHours;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    public Business() {
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

    public String getVertical() {
        return vertical;
    }

    public void setVertical(String vertical) {
        this.vertical = vertical;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getStepMinutes() {
        return stepMinutes;
    }

    public void setStepMinutes(int stepMinutes) {
        this.stepMinutes = stepMinutes;
    }

    public int getMinNoticeMinutes() {
        return minNoticeMinutes;
    }

    public void setMinNoticeMinutes(int minNoticeMinutes) {
        this.minNoticeMinutes = minNoticeMinutes;
    }

    public int getHorizonDays() {
        return horizonDays;
    }

    public void setHorizonDays(int horizonDays) {
        this.horizonDays = horizonDays;
    }

    public int getHoldMinutes() {
        return holdMinutes;
    }

    public void setHoldMinutes(int holdMinutes) {
        this.holdMinutes = holdMinutes;
    }

    public int getCancelHours() {
        return cancelHours;
    }

    public void setCancelHours(int cancelHours) {
        this.cancelHours = cancelHours;
    }

    public int getRescheduleHours() {
        return rescheduleHours;
    }

    public void setRescheduleHours(int rescheduleHours) {
        this.rescheduleHours = rescheduleHours;
    }

    public int getDepositPercent() {
        return depositPercent;
    }

    public void setDepositPercent(int depositPercent) {
        this.depositPercent = depositPercent;
    }

    public int getNoShowFeePercent() {
        return noShowFeePercent;
    }

    public void setNoShowFeePercent(int noShowFeePercent) {
        this.noShowFeePercent = noShowFeePercent;
    }

    public int getClientGapMinutes() {
        return clientGapMinutes;
    }

    public void setClientGapMinutes(int clientGapMinutes) {
        this.clientGapMinutes = clientGapMinutes;
    }

    public int getClientPerDay() {
        return clientPerDay;
    }

    public void setClientPerDay(int clientPerDay) {
        this.clientPerDay = clientPerDay;
    }

    public int getMinUsableGapMinutes() {
        return minUsableGapMinutes;
    }

    public void setMinUsableGapMinutes(int minUsableGapMinutes) {
        this.minUsableGapMinutes = minUsableGapMinutes;
    }

    public String getPacking() {
        return packing;
    }

    public void setPacking(String packing) {
        this.packing = packing;
    }

    public int getReminderHours() {
        return reminderHours;
    }

    public void setReminderHours(int reminderHours) {
        this.reminderHours = reminderHours;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
