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
package com.codenameone.examples.skirmish.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import java.time.Instant;

/// An engagement pipeline: a query over the players and what to do for each
/// one who matches. See `social.Pipelines`.
@Entity
public class Pipeline {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String name = "";

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, type = "VARCHAR(500)")
    private String conditions = "";

    @Column(nullable = false, type = "VARCHAR(10)")
    private String action = "";

    @Column(nullable = false, type = "VARCHAR(60)")
    private String title = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String body = "";

    @Column(nullable = false, type = "VARCHAR(20)")
    private String screen = "";

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int cooldownDays;

    @Column(nullable = false)
    private int reached;

    @Column(nullable = false)
    private Instant lastRun = Instant.EPOCH;

    @Column(nullable = false, type = "VARCHAR(190)")
    private String createdBy = "";

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getCooldownDays() {
        return cooldownDays;
    }

    public void setCooldownDays(int cooldownDays) {
        this.cooldownDays = cooldownDays;
    }

    public int getReached() {
        return reached;
    }

    public void setReached(int reached) {
        this.reached = reached;
    }

    public Instant getLastRun() {
        return lastRun;
    }

    public void setLastRun(Instant lastRun) {
        this.lastRun = lastRun;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
