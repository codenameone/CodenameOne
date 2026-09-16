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

import com.codename1.annotations.Column;
import com.codename1.annotations.DbTransient;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/**
 * The Backend chapter's entity example, compiled so it cannot drift.
 *
 * <p>Deliberately the same annotations the SQLite ORM chapter puts on a class an
 * app stores locally: an entity is the one class both halves of an application
 * own, and what decides which database it lands in is the module it is compiled
 * in rather than anything written here.
 */
// tag::backend-orm-entity[]
@Entity(table = "reminders")
public class Reminder {
    @Id public long id;

    @Column(nullable = false) public String title;

    public java.util.Date due;

    public boolean done;

    @DbTransient public String cachedLabel;   // never stored

    public Reminder() {
    }
}
// end::backend-orm-entity[]
