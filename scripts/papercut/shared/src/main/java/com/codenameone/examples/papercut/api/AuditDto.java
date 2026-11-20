/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// One entry of the admin audit log.
@Mapped
public record AuditDto(String id, String admin, String username, String action, String reason, long createdAt) {
}
