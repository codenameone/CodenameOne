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

/// What an account chose in Settings. A missing row reads as the defaults.
@Entity
public class Preference {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(8)")
    private String theme = "";

    @Column(nullable = false)
    private boolean notifyAssignments;

    @Column(nullable = false)
    private boolean notifyScheduleChanges;

    @Column(nullable = false)
    private boolean calendarSync;

    @Column(nullable = false)
    private boolean aiAssist;

    public Preference() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isNotifyAssignments() {
        return notifyAssignments;
    }

    public void setNotifyAssignments(boolean notifyAssignments) {
        this.notifyAssignments = notifyAssignments;
    }

    public boolean isNotifyScheduleChanges() {
        return notifyScheduleChanges;
    }

    public void setNotifyScheduleChanges(boolean notifyScheduleChanges) {
        this.notifyScheduleChanges = notifyScheduleChanges;
    }

    public boolean isCalendarSync() {
        return calendarSync;
    }

    public void setCalendarSync(boolean calendarSync) {
        this.calendarSync = calendarSync;
    }

    public boolean isAiAssist() {
        return aiAssist;
    }

    public void setAiAssist(boolean aiAssist) {
        this.aiAssist = aiAssist;
    }
}
