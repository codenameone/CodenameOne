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

/// Someone who goes out to sites: their skills, where they start each day, and
/// the limits they set on their own week. `username` is the account that signs
/// in as them, empty for a technician who has none. Coordinates are degrees.
@Entity
public class Technician {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(40)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(4)")
    private String initials = "";

    @Column(nullable = false)
    private int color;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String skills = "";

    @Column(nullable = false)
    private double homeLat;

    @Column(nullable = false)
    private double homeLng;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String homeLabel = "";

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(nullable = false)
    private long seenAt;

    @Column(nullable = false)
    private int maxDriveMinutes;

    @Column(nullable = false)
    private int regularMinutesPerWeek;

    @Column(nullable = false)
    private int maxMinutesPerWeek;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, type = "VARCHAR(500)")
    private String certifications = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String territories = "";

    @Column(nullable = false)
    private int maxJobsPerDay = 8;

    @Column(nullable = false)
    private int maxMinutesPerDay = 600;

    @Column(nullable = false, type = "VARCHAR(40)")
    private String rateId = "";

    public Technician() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public double getHomeLat() {
        return homeLat;
    }

    public void setHomeLat(double homeLat) {
        this.homeLat = homeLat;
    }

    public double getHomeLng() {
        return homeLng;
    }

    public void setHomeLng(double homeLng) {
        this.homeLng = homeLng;
    }

    public String getHomeLabel() {
        return homeLabel;
    }

    public void setHomeLabel(String homeLabel) {
        this.homeLabel = homeLabel;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public long getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(long seenAt) {
        this.seenAt = seenAt;
    }

    public int getMaxDriveMinutes() {
        return maxDriveMinutes;
    }

    public void setMaxDriveMinutes(int maxDriveMinutes) {
        this.maxDriveMinutes = maxDriveMinutes;
    }

    public int getRegularMinutesPerWeek() {
        return regularMinutesPerWeek;
    }

    public void setRegularMinutesPerWeek(int regularMinutesPerWeek) {
        this.regularMinutesPerWeek = regularMinutesPerWeek;
    }

    public int getMaxMinutesPerWeek() {
        return maxMinutesPerWeek;
    }

    public void setMaxMinutesPerWeek(int maxMinutesPerWeek) {
        this.maxMinutesPerWeek = maxMinutesPerWeek;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCertifications() {
        return certifications;
    }

    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public String getTerritories() {
        return territories;
    }

    public void setTerritories(String territories) {
        this.territories = territories;
    }

    public int getMaxJobsPerDay() {
        return maxJobsPerDay;
    }

    public void setMaxJobsPerDay(int maxJobsPerDay) {
        this.maxJobsPerDay = maxJobsPerDay;
    }

    public int getMaxMinutesPerDay() {
        return maxMinutesPerDay;
    }

    public void setMaxMinutesPerDay(int maxMinutesPerDay) {
        this.maxMinutesPerDay = maxMinutesPerDay;
    }

    public String getRateId() {
        return rateId;
    }

    public void setRateId(String rateId) {
        this.rateId = rateId;
    }
}
