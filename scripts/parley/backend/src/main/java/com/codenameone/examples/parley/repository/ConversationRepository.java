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
package com.codenameone.examples.parley.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.parley.domain.Conversation;

import java.time.Instant;
import java.util.List;

/// Conversations of every kind.
@Repository(entity = Conversation.class, key = String.class)
public interface ConversationRepository extends CrudRepository<Conversation, String> {
    Conversation findByDirectKey(String directKey);

    List<Conversation> findByIdIn(List<String> ids);

    long countByKind(String kind);

    @Query("select c.kind, count(c) from Conversation c group by c.kind")
    List<Object[]> byKind();

    /// Makes the rest of the transaction take its turn behind any other
    /// changing the same conversation, as [ProfileRepository#hold] does for an
    /// account.
    @Modifying
    @Query("update Conversation c set c.createdAt = c.createdAt where c.id = :id")
    int hold(@Param("id") String id);

    @Modifying
    @Query("update Conversation c set c.lastActivityAt = :at where c.id = :id and c.lastActivityAt < :at")
    int touched(@Param("id") String id, @Param("at") Instant at);
}
