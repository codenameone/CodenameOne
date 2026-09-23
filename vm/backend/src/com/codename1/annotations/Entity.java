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
package com.codename1.annotations;

import com.codename1.annotations.db.Index;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a persistent entity, on the server exactly as on the client.
 *
 * <p>This is the SAME fully qualified name the Codename One core declares, and
 * that is deliberate rather than an accident of copying. An entity is the one
 * kind of class both halves of an application own: the app stores rows of it in
 * its local SQLite file and the server stores rows of it in PostgreSQL, and if
 * the two sides had to spell the annotation differently the class could not be a
 * single file in a shared module. Here it is, so it can.
 *
 * <p>The two declarations are kept identical, and nothing resolves either one at
 * run time -- the retention is CLASS, so what exists after compilation is the
 * descriptor string Lcom/codename1/annotations/Entity;, which the build-time
 * processor reads out of the bytecode. A module that somehow has both jars on
 * one compile classpath therefore resolves whichever javac finds first and
 * produces the same descriptor from either.
 *
 * <p>What differs is what the build GENERATES from it. In a module compiled
 * against the Codename One core the processor emits a dao over
 * com.codename1.db.Database; in a module compiled against this runtime it emits
 * one over {@link com.codename1.backend.Database}, whose SQL is built for
 * whichever engine the connection turns out to be. The annotation says what the
 * class is; the module says which database it is stored in.
 *
 * <pre>
 *   &#64;Entity(table = "notes")
 *   public class Note {
 *       &#64;Id public long id;
 *       &#64;Column(nullable = false) public String title;
 *       public String body;
 *       public Note() {
 *       }
 *   }
 * </pre>
 *
 * The class needs public fields and a public no-arg constructor, because the
 * generated dao reads and writes them directly: this runtime has no reflection,
 * so a name looked up at run time is a name that is not there.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Entity {
    /** SQL table name. Defaults to the simple class name when blank. */
    String table() default "";
    /// Additional indexes created with the entity table.
    /// @return index declarations, empty by default
    Index[] indexes() default {};
}
