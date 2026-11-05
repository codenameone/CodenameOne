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

/// A calendar a provider connected, whose busy time keeps clients from booking
/// over it.
///
/// DEVICE is a calendar on the provider's phone, which the app reads and sends
/// as busy time; the server never sees the events. GOOGLE and MICROSOFT are
/// read by the server itself, with the refresh token the provider granted, so
/// the schedule stays right while the phone is off. ICS is a published
/// calendar address the server fetches.
@Entity
public class CalendarLink {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String kind = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String label = "";

    /// The calendars read, comma separated; empty is the primary one.
    @Column(nullable = false, type = "VARCHAR(1000)")
    private String calendars = "";

    /// Where confirmed appointments are written, or empty for nowhere.
    @Column(nullable = false, type = "VARCHAR(255)")
    private String writeCalendar = "";

    @Column(nullable = false, type = "VARCHAR(2000)")
    private String credential = "";

    @Column(nullable = false)
    private Instant lastSyncedAt = Instant.EPOCH;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String lastError = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    public CalendarLink() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCalendars() {
        return calendars;
    }

    public void setCalendars(String calendars) {
        this.calendars = calendars;
    }

    public String getWriteCalendar() {
        return writeCalendar;
    }

    public void setWriteCalendar(String writeCalendar) {
        this.writeCalendar = writeCalendar;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
