/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.ui.accessibility;

/// A deterministic accessibility audit finding.
public final class AccessibilityIssue {
    public enum Severity { ERROR, WARNING }

    private final Severity severity;
    private final String code;
    private final String message;
    private final long nodeId;

    public AccessibilityIssue(Severity severity, String code, String message, long nodeId) {
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.nodeId = nodeId;
    }

    public Severity getSeverity() { return severity; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public long getNodeId() { return nodeId; }

    @Override
    public String toString() {
        return severity + " " + code + " [node " + nodeId + "]: " + message;
    }
}
