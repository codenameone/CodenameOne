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
package com.demo;

import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/**
 * An entity whose ONLY persisted field is a key the database generates.
 *
 * <p>Legal, and the case the obvious insert cannot express: with no columns to
 * name, the construction builds "INSERT INTO t () VALUES ()", which SQLite and
 * PostgreSQL both refuse -- and MySQL is the one engine that wants exactly that
 * and has no DEFAULT VALUES form at all. One entity, three spellings, which is
 * why it is checked against all three rather than reasoned about.
 */
@Entity(table = "cn1_tickets")
public class Ticket {
    @Id
    public long id;

    public Ticket() {
    }
}
