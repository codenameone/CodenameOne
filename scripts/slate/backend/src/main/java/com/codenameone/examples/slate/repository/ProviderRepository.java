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
package com.codenameone.examples.slate.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.slate.domain.Provider;

import java.util.List;

@Repository(entity = Provider.class, key = String.class)
public interface ProviderRepository extends CrudRepository<Provider, String> {
    List<Provider> findByActiveOrderByDisplayNameAsc(boolean active);

    @Query("select t from Provider t order by t.displayName asc")

    List<Provider> everyByName();

    Provider findFirstByFeedToken(String feedToken);

    /// Holds the provider's row until the transaction ends. Every booking that
    /// takes this provider, and every change to their hours, holds it first,
    /// so the rules that count their day -- the most per day, the break --
    /// are checked against the bookings that are really there.
    @Modifying
    @Query("update Provider p set p.active = p.active where p.username = :username")
    int hold(@Param("username") String username);
}
