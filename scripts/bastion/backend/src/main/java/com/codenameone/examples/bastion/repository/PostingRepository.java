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
package com.codenameone.examples.bastion.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bastion.domain.Posting;

import java.util.List;

/// The legs of every entry. Only ever inserted.
@Repository(entity = Posting.class, key = String.class)
public interface PostingRepository extends CrudRepository<Posting, String> {
    List<Posting> findByEntryId(String entryId);

    List<Posting> findTop100ByAccountIdOrderByCreatedAtDescIdDesc(String accountId);

    /// What the postings say an account holds; null when it has none.
    @Query("select sum(p.amount) from Posting p where p.accountId = :id")
    Long total(@Param("id") String accountId);

    /// What all the postings in one asset add up to. Zero, always, or the
    /// ledger is broken.
    @Query("select sum(p.amount) from Posting p where p.asset = :asset")
    Long assetTotal(@Param("asset") String asset);
}
