/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.crash;

import com.codename1.testing.AbstractTest;

public class PiiScrubberTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        PiiScrubber s = new PiiScrubber();

        // Default email rule keeps first 3 chars of local part and the full domain.
        String scrubbed = s.scrubMessage("user joe.smith@example.com just hit a wall");
        assertTrue(scrubbed.contains("joe***@example.com"),
                "email local part must be truncated to first 3 chars: " + scrubbed);
        assertFalse(scrubbed.contains("smith"),
                "scrubbed message must not contain trailing local-part characters: " + scrubbed);

        // Local parts shorter than 3 chars are preserved verbatim plus mask.
        String shortMail = s.scrubMessage("a@example.com");
        assertTrue(shortMail.contains("a***@example.com"), "short local part: " + shortMail);

        // Long digit runs collapse to [num].
        String digits = s.scrubMessage("order 1234567890 failed");
        assertTrue(digits.contains("[num]"), "digit run replaced: " + digits);
        assertFalse(digits.contains("1234567890"), "raw digits removed: " + digits);

        // URLs are NOT scrubbed by default (the developer can override).
        String url = s.scrubMessage("see https://example.com/path for details");
        assertTrue(url.contains("https://example.com/path"),
                "URLs preserved by default: " + url);

        // Subclass override extends behaviour.
        PiiScrubber strict = new PiiScrubber() {
            @Override
            public String scrubMessage(String message) {
                String base = super.scrubMessage(message);
                return base == null ? null : base.replace("https://example.com", "[url]");
            }
        };
        String stricter = strict.scrubMessage("see https://example.com/path");
        assertTrue(stricter.contains("[url]"),
                "subclass override applied on top of default: " + stricter);

        // Null in, null out.
        assertTrue(s.scrubMessage(null) == null);

        return true;
    }
}
