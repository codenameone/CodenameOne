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
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.savor.domain.PaymentMethod;

import java.util.List;

/// The cards accounts saved.
@Repository(entity = PaymentMethod.class, key = String.class)
public interface PaymentMethodRepository extends CrudRepository<PaymentMethod, String> {
    /// An account's saved cards, oldest first.
    List<PaymentMethod> findByUsernameOrderByCreatedAtAscIdAsc(String username);

    long countByUsername(String username);

    /// The card an account pays with by default, or null when that is cash.
    PaymentMethod findFirstByUsernameAndPreferredTrue(String username);

    /// One of an account's cards, or null when the id is not a card of theirs.
    PaymentMethod findFirstByIdAndUsername(String id, String username);

    /// When the newest of an account's cards was added, or null with none.
    @Query("select max(m.createdAt) from PaymentMethod m where m.username = :username")
    Long newestAt(@Param("username") String username);

    int deleteByUsername(String username);
}
