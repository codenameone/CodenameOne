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
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.PaymentSetup;

/// The card setups accounts began with the payment processor.
@Repository(entity = PaymentSetup.class, key = String.class)
public interface PaymentSetupRepository extends CrudRepository<PaymentSetup, String> {
    /// A card setup an account began, or null.
    PaymentSetup findFirstByIdAndUsername(String id, String username);

    /// Marks a setup finished, if it is not yet.
    ///
    /// @return 1 for the one caller that finished it
    @Modifying
    @Query("update PaymentSetup s set s.completed = TRUE where s.id = :id and s.completed = FALSE")
    int claim(@Param("id") String id);

    /// How many setups an account began after `since` and has not finished.
    long countByUsernameAndCompletedFalseAndCreatedAtGreaterThan(String username, long since);

    int deleteByUsername(String username);

    /// Deletes the setups that saved their card.
    int deleteByCompletedTrue();

    /// Deletes the setups begun before `before`, finished or not.
    int deleteByCreatedAtLessThan(long before);
}
