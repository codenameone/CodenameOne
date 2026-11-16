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

/// One job a schedule run moved, and why.
@Entity
public class ScheduleChange {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(48)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String runId = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String jobId = "";

    @Column(nullable = false, type = "VARCHAR(160)")
    private String jobTitle = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String fromTech = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String toTech = "";

    @Column(nullable = false)
    private long fromStart;

    @Column(nullable = false)
    private long toStart;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String reason = "";

    public ScheduleChange() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getFromTech() {
        return fromTech;
    }

    public void setFromTech(String fromTech) {
        this.fromTech = fromTech;
    }

    public String getToTech() {
        return toTech;
    }

    public void setToTech(String toTech) {
        this.toTech = toTech;
    }

    public long getFromStart() {
        return fromStart;
    }

    public void setFromStart(long fromStart) {
        this.fromStart = fromStart;
    }

    public long getToStart() {
        return toStart;
    }

    public void setToStart(long toStart) {
        this.toStart = toStart;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
