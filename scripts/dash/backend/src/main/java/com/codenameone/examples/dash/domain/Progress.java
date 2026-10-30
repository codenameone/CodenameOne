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

/// A player's progress: experience, chests, lifetime counters behind the
/// achievements, and today's quest counters. One row per account, created the
/// first time anything asks for it.
@Entity
public class Progress {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false)
    private int xp;

    @Column(nullable = false)
    private boolean tutorialDone;

    @Column(nullable = false)
    private int chests;

    @Column(nullable = false)
    private int chestsOpened;

    @Column(nullable = false)
    private int races;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int metres;

    @Column(nullable = false)
    private int pickups;

    @Column(nullable = false)
    private int podiums;

    @Column(nullable = false)
    private int streak;

    @Column(nullable = false)
    private int bestStreak;

    @Column(nullable = false, type = "VARCHAR(500)")
    private String achievements = "";

    @Column(nullable = false)
    private long questDay;

    @Column(nullable = false)
    private int quest0;

    @Column(nullable = false)
    private int quest1;

    @Column(nullable = false)
    private int quest2;

    @Column(nullable = false)
    private int claimed;

    public Progress() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public boolean isTutorialDone() {
        return tutorialDone;
    }

    public void setTutorialDone(boolean tutorialDone) {
        this.tutorialDone = tutorialDone;
    }

    public int getChests() {
        return chests;
    }

    public void setChests(int chests) {
        this.chests = chests;
    }

    public int getChestsOpened() {
        return chestsOpened;
    }

    public void setChestsOpened(int chestsOpened) {
        this.chestsOpened = chestsOpened;
    }

    public int getRaces() {
        return races;
    }

    public void setRaces(int races) {
        this.races = races;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getMetres() {
        return metres;
    }

    public void setMetres(int metres) {
        this.metres = metres;
    }

    public int getPickups() {
        return pickups;
    }

    public void setPickups(int pickups) {
        this.pickups = pickups;
    }

    public int getPodiums() {
        return podiums;
    }

    public void setPodiums(int podiums) {
        this.podiums = podiums;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public String getAchievements() {
        return achievements;
    }

    public void setAchievements(String achievements) {
        this.achievements = achievements;
    }

    public long getQuestDay() {
        return questDay;
    }

    public void setQuestDay(long questDay) {
        this.questDay = questDay;
    }

    public int getQuest0() {
        return quest0;
    }

    public void setQuest0(int quest0) {
        this.quest0 = quest0;
    }

    public int getQuest1() {
        return quest1;
    }

    public void setQuest1(int quest1) {
        this.quest1 = quest1;
    }

    public int getQuest2() {
        return quest2;
    }

    public void setQuest2(int quest2) {
        this.quest2 = quest2;
    }

    public int getClaimed() {
        return claimed;
    }

    public void setClaimed(int claimed) {
        this.claimed = claimed;
    }
}
