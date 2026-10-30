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
import java.time.Instant;

/// One device of an account, with the public keys it published. Private keys
/// never leave the device; the server hands these out so others can open
/// sessions with it.
@Entity
public class Device {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String name = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String platform = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String identityKey = "";

    @Column(nullable = false)
    private int signedPreKeyId;

    @Column(nullable = false, type = "VARCHAR(200)")
    private String signedPreKey = "";

    @Column(nullable = false, type = "VARCHAR(200)")
    private String signedPreKeySignature = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant lastSeenAt = Instant.EPOCH;

    public Device() {
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public void setIdentityKey(String identityKey) {
        this.identityKey = identityKey;
    }

    public int getSignedPreKeyId() {
        return signedPreKeyId;
    }

    public void setSignedPreKeyId(int signedPreKeyId) {
        this.signedPreKeyId = signedPreKeyId;
    }

    public String getSignedPreKey() {
        return signedPreKey;
    }

    public void setSignedPreKey(String signedPreKey) {
        this.signedPreKey = signedPreKey;
    }

    public String getSignedPreKeySignature() {
        return signedPreKeySignature;
    }

    public void setSignedPreKeySignature(String signedPreKeySignature) {
        this.signedPreKeySignature = signedPreKeySignature;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
