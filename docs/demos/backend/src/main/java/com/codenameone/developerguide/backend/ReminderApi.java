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
package com.codenameone.developerguide.backend;

import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.orm.Dao;
import com.codename1.backend.orm.EntityManager;

import java.io.IOException;
import java.util.List;

/** The Backend chapter's injected-controller example, compiled so it cannot drift. */
// tag::backend-orm-controller[]
@RestController
@RequestMapping("/reminders")
public class ReminderApi {
    private final Dao<Reminder> reminders;

    /** The entry point calls this one because it is the one declared. */
    public ReminderApi(EntityManager entities) {
        this.reminders = entities.dao(Reminder.class);
    }

    @GetMapping
    public List<Reminder> outstanding() throws IOException {
        return reminders.query().eq("done", Boolean.FALSE).orderBy("due", true).list();
    }
}
// end::backend-orm-controller[]
