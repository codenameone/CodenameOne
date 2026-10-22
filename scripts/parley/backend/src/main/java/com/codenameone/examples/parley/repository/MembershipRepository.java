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
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.parley.domain.Membership;

import java.util.List;

/// Who is in which conversation.
@Repository(entity = Membership.class, key = String.class)
public interface MembershipRepository extends CrudRepository<Membership, String> {
    List<Membership> findByConversationId(String conversationId);

    List<Membership> findByUsername(String username);

    Membership findByConversationIdAndUsername(String conversationId, String username);

    long countByConversationId(String conversationId);

    long countByUsername(String username);

    long countByConversationIdAndRole(String conversationId, String role);

    int deleteByConversationId(String conversationId);

    int deleteByUsername(String username);

    List<Membership> findByConversationIdIn(List<String> conversationIds);
}
