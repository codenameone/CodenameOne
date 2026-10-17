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
package com.codenameone.examples.savor.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.savor.domain.CourierDocument;

import java.util.List;

/// The documents of applications to drive, keyed by
/// `Identifier.of(username, kind)`.
///
/// A document's row carries the picture, as Base64 text that can run to a
/// couple of megabytes. Only `findById` reads it. Everything that wants to know
/// which documents there are and how they stand selects those fields alone, so
/// listing two hundred applications does not pull two hundred sets of
/// photographs through the server.
@Repository(entity = CourierDocument.class, key = Identifier.class)
public interface CourierDocumentRepository extends CrudRepository<CourierDocument, Identifier> {
    /// Which documents an account has uploaded, as `{kind, status,
    /// uploadedAt}`, without the pictures.
    @Query("select d.kind, d.status, d.uploadedAt from CourierDocument d "
            + "where d.username = :username")
    List<Object[]> summaries(@Param("username") String username);

    /// The same for several accounts at once, as `{username, kind, status,
    /// uploadedAt}`.
    @Query("select d.username, d.kind, d.status, d.uploadedAt from CourierDocument d "
            + "where d.username in :names")
    List<Object[]> summaries(@Param("names") List<String> usernames);

    /// Puts a new picture in place of a stored document's, and answers how
    /// many rows that changed: 0 says there was none of that kind to replace.
    /// A statement, so that the picture being replaced is never read.
    @Modifying(clearAutomatically = true)
    @Query("update CourierDocument d set d.contentType = :type, d.status = :status, "
            + "d.uploadedAt = :at, d.data = :data where d.username = :username and d.kind = :kind")
    int replace(@Param("username") String username, @Param("kind") String kind,
            @Param("type") String contentType, @Param("status") String status,
            @Param("at") long at, @Param("data") String data);

    /// Sets the status of all of an account's documents.
    @Modifying(clearAutomatically = true)
    @Query("update CourierDocument d set d.status = :status where d.username = :username")
    int markAll(@Param("username") String username, @Param("status") String status);

    int deleteByUsername(String username);
}
