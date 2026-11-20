/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import java.time.Instant;

/// A player's game data beyond the sign-in record: wallet, bests per mode, papers and moderation state.
@Entity
public class Profile {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(40)")
    private String displayName = "";

    @Column(nullable = false)
    private int inkCoins;

    @Column(nullable = false)
    private int erasers;

    @Column(nullable = false)
    private long bestClassic;

    @Column(nullable = false)
    private long bestFold;

    @Column(nullable = false)
    private long bestDaily;

    @Column(nullable = false)
    private int runs;

    @Column(nullable = false, type = "VARCHAR(40)")
    private String quill = "";

    @Column(nullable = false, type = "VARCHAR(1000)")
    private String owned = "";

    @Column(nullable = false)
    private boolean flagged;

    @Column(nullable = false)
    private Instant suspendedUntil = Instant.EPOCH;

    @Column(nullable = false)
    private boolean banned;

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant lastSeen = Instant.EPOCH;

    public Profile() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getInkCoins() {
        return inkCoins;
    }

    public void setInkCoins(int inkCoins) {
        this.inkCoins = inkCoins;
    }

    public int getErasers() {
        return erasers;
    }

    public void setErasers(int erasers) {
        this.erasers = erasers;
    }

    public long getBestClassic() {
        return bestClassic;
    }

    public void setBestClassic(long bestClassic) {
        this.bestClassic = bestClassic;
    }

    public long getBestFold() {
        return bestFold;
    }

    public void setBestFold(long bestFold) {
        this.bestFold = bestFold;
    }

    public long getBestDaily() {
        return bestDaily;
    }

    public void setBestDaily(long bestDaily) {
        this.bestDaily = bestDaily;
    }

    public int getRuns() {
        return runs;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }

    public String getQuill() {
        return quill;
    }

    public void setQuill(String quill) {
        this.quill = quill;
    }

    public String getOwned() {
        return owned;
    }

    public void setOwned(String owned) {
        this.owned = owned;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public Instant getSuspendedUntil() {
        return suspendedUntil;
    }

    public void setSuspendedUntil(Instant suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
}
