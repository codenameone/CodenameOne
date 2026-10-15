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
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.PhoneCode;

/// The code an account was texted to prove its phone number. An account has at
/// most one.
@Repository(entity = PhoneCode.class, key = String.class)
public interface PhoneCodeRepository extends CrudRepository<PhoneCode, String> {
    /// Takes one of the guesses an account's code allows. The database does
    /// the sum and applies the limit in the same statement, so of any number
    /// of guesses sent at once only as many as are left are let through;
    /// nothing is read and written back.
    ///
    /// The guess is taken of the code whose hash is `codeHash`, and of no
    /// other: a new code sent between reading the hash and taking the guess
    /// has another hash, and the update changes nothing.
    ///
    /// The code before it, when one is kept, is guessed at by the same guess,
    /// so its count goes up too; its own limit is applied by the caller.
    ///
    /// @return 1 for a guess taken, 0 when none is left or there is no code
    @Modifying
    @Query("update PhoneCode c set c.attempts = c.attempts + 1, "
            + "c.previousAttempts = c.previousAttempts + 1 "
            + "where c.username = :username and c.codeHash = :hash and c.attempts < :most")
    int takeGuess(@Param("username") String username, @Param("hash") String codeHash,
            @Param("most") int most);

    int deleteByUsername(String username);
}
