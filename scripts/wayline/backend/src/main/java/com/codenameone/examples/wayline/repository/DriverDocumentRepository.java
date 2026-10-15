/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.wayline.domain.DriverDocument;

import java.util.List;

/// The documents of applications to drive, keyed by
/// `Identifier.of(username, kind)`.
///
/// A document's row carries the picture, as Base64 text that can run to a
/// couple of megabytes. Only `findById` reads it. Everything that wants to know
/// which documents there are and how they stand selects those fields alone, so
/// listing two hundred applications does not pull two hundred sets of
/// photographs through the server.
@Repository(entity = DriverDocument.class, key = Identifier.class)
public interface DriverDocumentRepository extends CrudRepository<DriverDocument, Identifier> {
    /// Which documents an account has uploaded, as `{kind, status,
    /// uploadedAt}`, without the pictures.
    @Query("select d.kind, d.status, d.uploadedAt from DriverDocument d "
            + "where d.username = :username")
    List<Object[]> summaries(@Param("username") String username);

    /// The same for several accounts at once, as `{username, kind, status,
    /// uploadedAt}`.
    @Query("select d.username, d.kind, d.status, d.uploadedAt from DriverDocument d "
            + "where d.username in :names")
    List<Object[]> summaries(@Param("names") List<String> usernames);

    /// Puts a new picture in place of a stored document's, and answers how
    /// many rows that changed: 0 says there was none of that kind to replace.
    /// A statement, so that the picture being replaced is never read.
    @Modifying
    @Query("update DriverDocument d set d.contentType = :type, d.status = :status, "
            + "d.uploadedAt = :at, d.data = :data where d.username = :username and d.kind = :kind")
    int replace(@Param("username") String username, @Param("kind") String kind,
            @Param("type") String contentType, @Param("status") String status,
            @Param("at") long at, @Param("data") String data);

    /// Sets the status of all of an account's documents.
    @Modifying
    @Query("update DriverDocument d set d.status = :status where d.username = :username")
    int markAll(@Param("username") String username, @Param("status") String status);

    int deleteByUsername(String username);
}
