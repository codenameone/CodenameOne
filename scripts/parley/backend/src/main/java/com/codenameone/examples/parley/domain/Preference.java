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
package com.codenameone.examples.parley.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// How one person wants the app, and what they let others see. A row exists
/// once they have saved something; until then the defaults are the server's.
@Entity
public class Preference {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(8)")
    private String theme = "";

    @Column(nullable = false)
    private boolean notifyMessages;

    @Column(nullable = false)
    private boolean notifyGroups;

    @Column(nullable = false)
    private boolean notifyCalls;

    @Column(nullable = false)
    private boolean readReceipts;

    @Column(nullable = false)
    private boolean typingIndicators;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String lastSeen = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String profilePhoto = "";

    @Column(nullable = false)
    private int disappearingSeconds;

    @Column(nullable = false)
    private boolean screenSecurity;

    @Column(nullable = false)
    private boolean appLock;

    public Preference() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isNotifyMessages() {
        return notifyMessages;
    }

    public void setNotifyMessages(boolean notifyMessages) {
        this.notifyMessages = notifyMessages;
    }

    public boolean isNotifyGroups() {
        return notifyGroups;
    }

    public void setNotifyGroups(boolean notifyGroups) {
        this.notifyGroups = notifyGroups;
    }

    public boolean isNotifyCalls() {
        return notifyCalls;
    }

    public void setNotifyCalls(boolean notifyCalls) {
        this.notifyCalls = notifyCalls;
    }

    public boolean isReadReceipts() {
        return readReceipts;
    }

    public void setReadReceipts(boolean readReceipts) {
        this.readReceipts = readReceipts;
    }

    public boolean isTypingIndicators() {
        return typingIndicators;
    }

    public void setTypingIndicators(boolean typingIndicators) {
        this.typingIndicators = typingIndicators;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public int getDisappearingSeconds() {
        return disappearingSeconds;
    }

    public void setDisappearingSeconds(int disappearingSeconds) {
        this.disappearingSeconds = disappearingSeconds;
    }

    public boolean isScreenSecurity() {
        return screenSecurity;
    }

    public void setScreenSecurity(boolean screenSecurity) {
        this.screenSecurity = screenSecurity;
    }

    public boolean isAppLock() {
        return appLock;
    }

    public void setAppLock(boolean appLock) {
        this.appLock = appLock;
    }
}
