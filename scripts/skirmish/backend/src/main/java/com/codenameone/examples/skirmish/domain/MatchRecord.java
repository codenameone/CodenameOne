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

/// A finished match: who played, how it ended, and the replay that reproduces it.
@Entity
public class MatchRecord {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String player0 = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String player1 = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String name0 = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String name1 = "";

    @Column(nullable = false)
    private int result;

    @Column(nullable = false)
    private int crowns0;

    @Column(nullable = false)
    private int crowns1;

    @Column(nullable = false)
    private int trophies0;

    @Column(nullable = false)
    private int trophies1;

    @Column(nullable = false)
    private int ticks;

    @Column(nullable = false)
    private Instant endedAt = Instant.EPOCH;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String rulesVersion = "";

    @Column(nullable = false)
    private String replay = "";

    public MatchRecord() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayer0() {
        return player0;
    }

    public void setPlayer0(String player0) {
        this.player0 = player0;
    }

    public String getPlayer1() {
        return player1;
    }

    public void setPlayer1(String player1) {
        this.player1 = player1;
    }

    public String getName0() {
        return name0;
    }

    public void setName0(String name0) {
        this.name0 = name0;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getCrowns0() {
        return crowns0;
    }

    public void setCrowns0(int crowns0) {
        this.crowns0 = crowns0;
    }

    public int getCrowns1() {
        return crowns1;
    }

    public void setCrowns1(int crowns1) {
        this.crowns1 = crowns1;
    }

    public int getTrophies0() {
        return trophies0;
    }

    public void setTrophies0(int trophies0) {
        this.trophies0 = trophies0;
    }

    public int getTrophies1() {
        return trophies1;
    }

    public void setTrophies1(int trophies1) {
        this.trophies1 = trophies1;
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
