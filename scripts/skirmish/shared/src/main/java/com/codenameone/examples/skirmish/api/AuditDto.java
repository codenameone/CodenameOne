package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// One entry of the admin audit log.
@Mapped
public record AuditDto(String id, String admin, String username, String action, String reason, long createdAt) {
}
