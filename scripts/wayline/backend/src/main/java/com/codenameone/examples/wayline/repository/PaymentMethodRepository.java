/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.PaymentMethod;

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
