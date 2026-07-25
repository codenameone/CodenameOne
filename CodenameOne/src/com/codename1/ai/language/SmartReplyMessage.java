/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

/** One message supplied to the on-device smart-reply model. */
public final class SmartReplyMessage {
    private final String text;
    private final String participantId;
    private final boolean localUser;
    private final long timestampMillis;

    public SmartReplyMessage(String text, String participantId,
                             boolean localUser, long timestampMillis) {
        this.text = text == null ? "" : text;
        this.participantId = participantId;
        this.localUser = localUser;
        this.timestampMillis = timestampMillis;
    }

    public String getText() {
        return text;
    }

    public String getParticipantId() {
        return participantId;
    }

    public boolean isLocalUser() {
        return localUser;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
