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

import com.codename1.annotations.Column;
import com.codename1.annotations.DbTransient;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/**
 * An entity on the shared contract, annotated exactly as an app's own would be.
 *
 * <p>It sits beside the @RestClient interface on purpose: this is the class both
 * halves of an application own. The app stores rows of it in its local SQLite
 * file through the dao the client build generates, and the server stores rows of
 * it in whatever the deployment named through the dao the backend build
 * generates. One source file, two daos, and neither of them written by hand.
 *
 * <p>The columns are chosen to cover what the engines disagree about: a generated
 * key (an identity column on PostgreSQL, AUTOINCREMENT on SQLite, AUTO_INCREMENT
 * on MySQL), a boolean and a date (integers everywhere, so one decoding path
 * reads them), a blob (BYTEA, BLOB, LONGBLOB), a boxed field that can be null,
 * and a MIXED-CASE column name, which only survives PostgreSQL because the
 * generated SQL quotes every identifier.
 */
@Entity(table = "cn1_notes")
public class Note {
    @Id
    public long id;

    @Column(nullable = false)
    public String title;

    public String body;

    public int views;

    public boolean pinned;

    public double score;

    @Column(name = "createdAt")
    public java.util.Date created;

    public byte[] payload;

    /** Boxed, so the column can be null and come back as null. */
    public Long revision;

    /**
     * The narrow integrals, which are an INTEGER column on every engine: what
     * comes back is a Long, and reading it into these fields narrows. A value
     * outside their range is refused rather than wrapped.
     */
    public short rank;

    public byte flags;

    /** Stored in the same REAL column a double is, and narrowed on the way in. */
    public float weight;

    /**
     * The boxed forms, each of which can be null. Every scalar type an entity
     * can hold appears in this class exactly so that one run of ormcheck
     * answers for all of them on all three engines: a type nobody listed here
     * is a type nobody tested, which is how a char field that could not be
     * inserted on PostgreSQL at all went unnoticed.
     */
    public Integer priority;

    public Boolean archived;

    public Double weightAgain;

    public Short rankAgain;

    public Byte flagsAgain;

    public Float weightNarrow;

    /** A primitive char, which is stored as one-character text. */
    public char initial;

    /**
     * And its boxed form, which the generator reads through a DIFFERENT
     * conversion: nullable, so it cannot fall back to a default character.
     */
    public Character grade;

    /** Never stored: the ORM leaves it alone and so does the table. */
    @DbTransient
    public String cached;

    public Note() {
    }
}
