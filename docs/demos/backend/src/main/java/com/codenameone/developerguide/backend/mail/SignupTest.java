/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.developerguide.backend.mail;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.test.BackendTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// tag::backend-mail-test[]
@BackendTest(properties = "cn1.mail.from=Acme <no-reply@example.com>")
class SignupTest {
    @Autowired
    private Signup signup;

    @Autowired
    private MailSender mail;

    @BeforeEach
    void emptyInbox() throws Exception {
        mail.clearSent();
    }

    @Test
    void aNewAccountIsWelcomed() throws Exception {
        signup.register("Ada", "ada@example.com");

        assertEquals(1, mail.sent().size());
        Mail welcome = mail.sent().get(0);
        assertEquals("ada@example.com", welcome.getTo().get(0));
        assertEquals("Welcome, Ada", welcome.getSubject());
        assertTrue(welcome.getText().contains("https://example.com/login"));
    }
}
// end::backend-mail-test[]
