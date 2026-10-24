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
package com.codenameone.examples.reel.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.reel.domain.ModerationEvent;

import java.util.List;

/// The record of what the admins did to each account.
@Repository(entity = ModerationEvent.class, key = String.class)
public interface ModerationEventRepository extends CrudRepository<ModerationEvent, String> {
    /// When the newest event of an account was written, or null when it has
    /// none.
    @Query("select max(e.createdAt) from ModerationEvent e where e.username = :username")
    Long newest(@Param("username") String username);

    /// An account's last hundred events, newest first.
    List<ModerationEvent> findTop100ByUsernameOrderByCreatedAtDescIdAsc(String username);

    int deleteByUsername(String username);
}
