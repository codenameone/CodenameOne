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
package com.codenameone.examples.reel.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import java.time.Instant;

/// A movie, a series, a course or a creator's video, with its listing and moderation state.
@Entity
public class Title {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String kind = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String name = "";

    @Column(nullable = false, type = "TEXT")
    private String synopsis = "";

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, type = "VARCHAR(8)")
    private String rating = "";

    /// What earned the rating -- `violence`, `language` -- comma separated.
    @Column(nullable = false, type = "VARCHAR(255)")
    private String advisories = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String genres = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String tags = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String creator = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String access = "";

    @Column(nullable = false)
    private long rentalPriceCents;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String status = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String moderationNote = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String posterUrl = "";

    @Column(nullable = false, type = "VARCHAR(255)")
    private String backdropUrl = "";

    @Column(nullable = false)
    private boolean certificate;

    @Column(nullable = false)
    private int passPercent;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean ageRestricted;

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant publishedAt = Instant.EPOCH;

    /// How many of its videos the viewers following it have been told about.
    @Column(nullable = false)
    private int announcedVideos;

    @Column(nullable = false)
    private long ratingSum;

    @Column(nullable = false)
    private int ratingCount;

    @Column(nullable = false)
    private long viewCount;

    public Title() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getAdvisories() {
        return advisories;
    }

    public void setAdvisories(String advisories) {
        this.advisories = advisories;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public long getRentalPriceCents() {
        return rentalPriceCents;
    }

    public void setRentalPriceCents(long rentalPriceCents) {
        this.rentalPriceCents = rentalPriceCents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getModerationNote() {
        return moderationNote;
    }

    public void setModerationNote(String moderationNote) {
        this.moderationNote = moderationNote;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public void setBackdropUrl(String backdropUrl) {
        this.backdropUrl = backdropUrl;
    }

    public boolean isCertificate() {
        return certificate;
    }

    public void setCertificate(boolean certificate) {
        this.certificate = certificate;
    }

    public int getPassPercent() {
        return passPercent;
    }

    public void setPassPercent(int passPercent) {
        this.passPercent = passPercent;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isAgeRestricted() {
        return ageRestricted;
    }

    public void setAgeRestricted(boolean ageRestricted) {
        this.ageRestricted = ageRestricted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public long getRatingSum() {
        return ratingSum;
    }

    public void setRatingSum(long ratingSum) {
        this.ratingSum = ratingSum;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public int getAnnouncedVideos() {
        return announcedVideos;
    }

    public void setAnnouncedVideos(int announcedVideos) {
        this.announcedVideos = announcedVideos;
    }
}
