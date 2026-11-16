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

/// A visit to a site. Its plan -- technician, start, the drive before it, its
/// place in the day -- is written by the scheduler; its progress by the
/// technician. Times are epoch milliseconds, 0 for none.
@Entity
public class Job {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(40)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(160)")
    private String title = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String kind = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String status = "";

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false, type = "VARCHAR(40)")
    private String siteId = "";

    @Column(nullable = false, type = "VARCHAR(1000)")
    private String description = "";

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String skills = "";

    @Column(nullable = false)
    private long windowStart;

    @Column(nullable = false)
    private long windowEnd;

    @Column(nullable = false, type = "VARCHAR(40)")
    private String techId = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String previousTechId = "";

    @Column(nullable = false)
    private long plannedStart;

    @Column(nullable = false)
    private long plannedEnd;

    @Column(nullable = false)
    private int travelBefore;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private long actualStart;

    @Column(nullable = false)
    private long actualEnd;

    @Column(nullable = false, type = "VARCHAR(2000)")
    private String notes = "";

    @Column(nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private long updatedAt;

    @Column(nullable = false)
    private long dueAt;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String certifications = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String excludedTechs = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String afterJobId = "";

    @Column(nullable = false, type = "VARCHAR(160)")
    private String unassignedReason = "";

    public Job() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(long windowStart) {
        this.windowStart = windowStart;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getTechId() {
        return techId;
    }

    public void setTechId(String techId) {
        this.techId = techId;
    }

    public String getPreviousTechId() {
        return previousTechId;
    }

    public void setPreviousTechId(String previousTechId) {
        this.previousTechId = previousTechId;
    }

    public long getPlannedStart() {
        return plannedStart;
    }

    public void setPlannedStart(long plannedStart) {
        this.plannedStart = plannedStart;
    }

    public long getPlannedEnd() {
        return plannedEnd;
    }

    public void setPlannedEnd(long plannedEnd) {
        this.plannedEnd = plannedEnd;
    }

    public int getTravelBefore() {
        return travelBefore;
    }

    public void setTravelBefore(int travelBefore) {
        this.travelBefore = travelBefore;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public long getActualStart() {
        return actualStart;
    }

    public void setActualStart(long actualStart) {
        this.actualStart = actualStart;
    }

    public long getActualEnd() {
        return actualEnd;
    }

    public void setActualEnd(long actualEnd) {
        this.actualEnd = actualEnd;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getDueAt() {
        return dueAt;
    }

    public void setDueAt(long dueAt) {
        this.dueAt = dueAt;
    }

    public String getCertifications() {
        return certifications;
    }

    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public String getExcludedTechs() {
        return excludedTechs;
    }

    public void setExcludedTechs(String excludedTechs) {
        this.excludedTechs = excludedTechs;
    }

    public String getAfterJobId() {
        return afterJobId;
    }

    public void setAfterJobId(String afterJobId) {
        this.afterJobId = afterJobId;
    }

    public String getUnassignedReason() {
        return unassignedReason;
    }

    public void setUnassignedReason(String unassignedReason) {
        this.unassignedReason = unassignedReason;
    }
}
