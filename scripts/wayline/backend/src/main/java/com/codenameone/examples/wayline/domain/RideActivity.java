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
package com.codenameone.examples.wayline.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import java.time.Instant;

/// The Live Activity a rider's phone shows a ride in, while the ride lasts.
@Entity
public class RideActivity {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String rideId = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    /// The id the phone's platform gave the activity.
    @Column(nullable = false, type = "VARCHAR(190)")
    private String activityId = "";

    /// When the server last sent it a state, for spacing out the updates.
    @Column(nullable = false)
    private Instant sentAt = Instant.EPOCH;

    /// When its ride ended, 0 while the ride lasts. An ended activity is kept
    /// until the phone has been told to end it.
    @Column(nullable = false)
    private Instant endedAt = Instant.EPOCH;

    public RideActivity() {
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
