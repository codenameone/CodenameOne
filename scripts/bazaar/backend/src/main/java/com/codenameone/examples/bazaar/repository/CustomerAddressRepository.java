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
import com.codenameone.examples.bazaar.domain.CustomerAddress;
import java.util.List;

/// Reads and writes [CustomerAddress] rows.
@Repository(entity = CustomerAddress.class, key = String.class)
public interface CustomerAddressRepository extends CrudRepository<CustomerAddress, String> {
    List<CustomerAddress> findByUsername(String username);

    List<CustomerAddress> findByCustomerId(String customerId);

    /// Moves every row of one customer to another (a merge).
    @com.codename1.orm.repository.Modifying(clearAutomatically = true)
    @com.codename1.orm.repository.Query("update CustomerAddress x set x.customerId = :to where x.customerId = :from")
    int moveCustomer(@com.codename1.orm.repository.Param("from") String from,
            @com.codename1.orm.repository.Param("to") String to);
}
