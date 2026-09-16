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

import com.codename1.backend.DataSource;
import com.codename1.backend.orm.Dao;
import com.codename1.backend.orm.EntityManager;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/** The Backend chapter's ORM examples, compiled so they cannot drift. */
public final class OrmSnippets {

    private OrmSnippets() {
    }

    public static long store(EntityManager em) throws IOException {
// tag::backend-orm-dao[]
Dao<Reminder> reminders = em.dao(Reminder.class);

Reminder reminder = new Reminder();
reminder.title = "renew the certificate";
reminder.due = new Date();
reminders.insert(reminder);          // reminder.id is now the generated key

Reminder stored = reminders.findById(Long.valueOf(reminder.id));
stored.done = true;
reminders.update(stored);
// end::backend-orm-dao[]
        return reminder.id;
    }

    public static List<Reminder> query(EntityManager em) throws IOException {
// tag::backend-orm-query[]
List<Reminder> overdue = em.dao(Reminder.class).query()
        .eq("done", Boolean.FALSE)
        .lt("due", new Date())
        .orderBy("due", true)
        .limit(20)
        .list();
// end::backend-orm-query[]
        return overdue;
    }

    public static void transaction(EntityManager em) throws Exception {
// tag::backend-orm-transaction[]
em.transaction(new EntityManager.Work() {
    public Object run(EntityManager tx) throws Exception {
        Dao<Reminder> reminders = tx.dao(Reminder.class);
        Reminder first = reminders.findById(Long.valueOf(1));
        first.done = true;
        reminders.update(first);
        reminders.insert(follower(first));
        return null;
    }
});
// end::backend-orm-transaction[]
    }

    public static EntityManager open(DataSource pool) throws IOException {
// tag::backend-orm-open[]
EntityManager em = EntityManager.open(pool);
// end::backend-orm-open[]
        return em;
    }

    private static Reminder follower(Reminder previous) {
        Reminder out = new Reminder();
        out.title = previous.title + " (again)";
        out.due = new Date(previous.due.getTime() + 86400000L);
        return out;
    }
}
