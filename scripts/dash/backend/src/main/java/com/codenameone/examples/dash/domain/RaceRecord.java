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
package com.codenameone.examples.dash.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import java.time.Instant;

/// A finished race: the level, who ran, where they placed, and the replay that
/// reproduces every runner. Lists are comma separated, in seat order; a bot's
/// username is empty.
@Entity
public class RaceRecord {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(160)")
    private String level = "";

    @Column(nullable = false, type = "VARCHAR(1200)")
    private String players = "";

    @Column(nullable = false, type = "VARCHAR(300)")
    private String names = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String places = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String scores = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String trophies = "";

    @Column(nullable = false)
    private int runners;

    @Column(nullable = false)
    private int humans;

    @Column(nullable = false)
    private int ticks;

    @Column(nullable = false)
    private Instant endedAt = Instant.EPOCH;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String rulesVersion = "";

    @Column(nullable = false)
    private String replay = "";

    public RaceRecord() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPlayers() {
        return players;
    }

    public void setPlayers(String players) {
        this.players = players;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getPlaces() {
        return places;
    }

    public void setPlaces(String places) {
        this.places = places;
    }

    public String getScores() {
        return scores;
    }

    public void setScores(String scores) {
        this.scores = scores;
    }

    public String getTrophies() {
        return trophies;
    }

    public void setTrophies(String trophies) {
        this.trophies = trophies;
    }

    public int getRunners() {
        return runners;
    }

    public void setRunners(int runners) {
        this.runners = runners;
    }

    public int getHumans() {
        return humans;
    }

    public void setHumans(int humans) {
        this.humans = humans;
    }

    public int getTicks() {
        return ticks;
    }

    public void setTicks(int ticks) {
        this.ticks = ticks;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getRulesVersion() {
        return rulesVersion;
    }

    public void setRulesVersion(String rulesVersion) {
        this.rulesVersion = rulesVersion;
    }

    public String getReplay() {
        return replay;
    }

    public void setReplay(String replay) {
        this.replay = replay;
    }
}
