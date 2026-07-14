/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.help;

/**
 * The outcome of submitting a {@link ToolingHelpReport}. Captures which of the two
 * response paths applies so the UI layer can render the right message without
 * re-deriving the logic.
 *
 * <ul>
 *   <li>{@link Outcome#EMAILED} &mdash; the report went through <em>and</em> carried an
 *       email, so support will reply by email (Path A).</li>
 *   <li>{@link Outcome#NO_EMAIL} &mdash; the report went through but had no email. The
 *       caller opens the browser to {@link #getChatDeepLink()} ({@code chatUrl#t=token});
 *       that token-gated page fetches the report, posts the summary + a "Full report"
 *       link into a live Crisp chat, and opens it (Path B).</li>
 *   <li>{@link Outcome#UNREACHABLE} &mdash; we could not reach support (offline / timeout /
 *       non-2xx). Degrade to self-serve help &mdash; never a dead end.</li>
 * </ul>
 */
public final class ToolingHelpResponse {

    public enum Outcome {
        EMAILED,
        NO_EMAIL,
        UNREACHABLE
    }

    private final Outcome outcome;
    private final Integer ticketId;
    private final String token;
    private final String email;
    private final String chatUrl;
    private final String helpArticleUrl;

    ToolingHelpResponse(Outcome outcome, Integer ticketId, String token, String email,
                        String chatUrl, String helpArticleUrl) {
        this.outcome = outcome;
        this.ticketId = ticketId;
        this.token = token;
        this.email = email;
        this.chatUrl = chatUrl;
        this.helpArticleUrl = helpArticleUrl;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    /** True when the report actually reached support (Path A or B), false when unreachable. */
    public boolean wasDelivered() {
        return outcome != Outcome.UNREACHABLE;
    }

    /** Server-issued ticket reference, or null if unknown (offline, or server omitted it). */
    public Integer getTicketId() {
        return ticketId;
    }

    /** Opaque token that lets the chat page fetch this report; null when unknown/offline. */
    public String getToken() {
        return token;
    }

    /** The email the reply will go to (Path A only); may be null/empty otherwise. */
    public String getEmail() {
        return email;
    }

    /** Where the live chat widget loads. Never null. */
    public String getChatUrl() {
        return chatUrl;
    }

    /**
     * The chat URL with the report token appended as a fragment ({@code chatUrl#t=token}).
     * This is what the no-email path opens (and what an optional "Chat live" from the email
     * path opens). Falls back to the bare chat URL when no token is available. Never null.
     */
    public String getChatDeepLink() {
        if (token != null && token.length() > 0 && chatUrl != null) {
            return chatUrl + "#t=" + token;
        }
        return chatUrl;
    }

    /** Self-serve article for the failed step (unreachable fallback only). Never null. */
    public String getHelpArticleUrl() {
        return helpArticleUrl;
    }
}
