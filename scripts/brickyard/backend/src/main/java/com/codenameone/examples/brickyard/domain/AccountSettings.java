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
package com.codenameone.examples.brickyard.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// Per-account settings the server enforces or keeps for every device:
/// avatar colours, the parental spending limit (-1 is none) and chat.
@Entity
public class AccountSettings {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(190)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String avatar = "";

    @Column(nullable = false)
    private long spendLimit;

    @Column(nullable = false)
    private long spent;

    @Column(nullable = false)
    private long spentMonth;

    @Column(nullable = false)
    private int chatEnabled;

    public AccountSettings() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public long getSpendLimit() {
        return spendLimit;
    }

    public void setSpendLimit(long spendLimit) {
        this.spendLimit = spendLimit;
    }

    public long getSpent() {
        return spent;
    }

    public void setSpent(long spent) {
        this.spent = spent;
    }

    public long getSpentMonth() {
        return spentMonth;
    }

    public void setSpentMonth(long spentMonth) {
        this.spentMonth = spentMonth;
    }

    public int getChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(int chatEnabled) {
        this.chatEnabled = chatEnabled;
    }
}
