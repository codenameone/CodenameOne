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
package com.codenameone.examples.brickyard.moderation;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.AuditDto;
import com.codenameone.examples.brickyard.domain.AuditLog;
import com.codenameone.examples.brickyard.repository.AuditLogRepository;
import java.io.IOException;

/// The audit log: every staff action writes one row, in the transaction of the action.
@Component
public class Audit {
    private final AuditLogRepository log;

    public Audit(AuditLogRepository log) {
        this.log = log;
    }

    public AuditDto record(String action, String target, String detail) throws IOException {
        AuditLog a = new AuditLog();
        a.setId(Ids.next());
        a.setActor(Caller.name());
        a.setAction(action);
        a.setTarget(target == null ? "" : clip(target, 190));
        a.setDetail(detail == null ? "" : clip(detail, 400));
        a.setCreatedAt(Clock.now());
        log.insert(a);
        return toDto(a);
    }

    static String clip(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    public static AuditDto toDto(AuditLog a) {
        return new AuditDto(a.getActor(), a.getAction(), a.getTarget(), a.getDetail(), a.getCreatedAt());
    }
}
