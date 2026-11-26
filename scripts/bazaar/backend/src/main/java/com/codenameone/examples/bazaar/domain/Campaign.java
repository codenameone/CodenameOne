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

/// An e-mail or SMS campaign to a segment, with an A/B subject test.
@Entity
public class Campaign {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(8)")
    private String channel = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String audienceId = "";

    @Column(name = "subject_a", nullable = false, type = "VARCHAR(200)")
    private String subjectA = "";

    @Column(name = "subject_b", nullable = false, type = "VARCHAR(200)")
    private String subjectB = "";

    @Column(nullable = false, type = "TEXT")
    private String body = "";

    @Column(nullable = false)
    private int abPercent;

    @Column(nullable = false)
    private int winnerAfterMinutes;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String status = "";

    @Column(nullable = false)
    private Instant scheduledAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant testSentAt = Instant.EPOCH;

    @Column(nullable = false, type = "VARCHAR(1)")
    private String winner = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String colour = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    public Campaign() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getAudienceId() {
        return audienceId;
    }

    public void setAudienceId(String audienceId) {
        this.audienceId = audienceId;
    }

    public String getSubjectA() {
        return subjectA;
    }

    public void setSubjectA(String subjectA) {
        this.subjectA = subjectA;
    }

    public String getSubjectB() {
        return subjectB;
    }

    public void setSubjectB(String subjectB) {
        this.subjectB = subjectB;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getAbPercent() {
        return abPercent;
    }

    public void setAbPercent(int abPercent) {
        this.abPercent = abPercent;
    }

    public int getWinnerAfterMinutes() {
        return winnerAfterMinutes;
    }

    public void setWinnerAfterMinutes(int winnerAfterMinutes) {
        this.winnerAfterMinutes = winnerAfterMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getTestSentAt() {
        return testSentAt;
    }

    public void setTestSentAt(Instant testSentAt) {
        this.testSentAt = testSentAt;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
