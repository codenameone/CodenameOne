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

package com.codenameone.examples.bazaar.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Repository;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Modifying;
import com.codenameone.examples.bazaar.domain.Cart;
import java.util.List;
import java.time.Instant;

/// Reads and writes [Cart] rows.
@Repository(entity = Cart.class, key = String.class)
public interface CartRepository extends CrudRepository<Cart, String> {
    Cart findByToken(String token);

    Cart findByStoreIdAndUsername(String storeId, String username);

    List<Cart> findByStoreIdOrderByUpdatedAtDesc(String storeId);

    List<Cart> findByStoreIdAndCreatedAtGreaterThanEqual(String storeId, Instant since);

    /// Carts nobody has been reminded of, idle since before `before`.
    List<Cart> findByReminderSentAtAndUpdatedAtLessThan(Instant never, Instant before);

    /// Marks a cart reminded, once: answers 1 for the caller that did it.
    @Modifying(clearAutomatically = true)
    @Query("update Cart c set c.reminderSentAt = :at where c.id = :id and c.reminderSentAt = :never")
    int claimReminder(@Param("id") String id, @Param("never") Instant never, @Param("at") Instant at);
}
