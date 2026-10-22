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
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.parley.domain.PhoneCode;

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
    /// Empties the session, so the code is read again after it with the guess
    /// counted.
    ///
    /// @return 1 for a guess taken, 0 when none is left or there is no code
    @Modifying(clearAutomatically = true)
    @Query("update PhoneCode c set c.attempts = c.attempts + 1, "
            + "c.previousAttempts = c.previousAttempts + 1 "
            + "where c.username = :username and c.codeHash = :hash and c.attempts < :most")
    int takeGuess(@Param("username") String username, @Param("hash") String codeHash,
            @Param("most") int most);

    int deleteByUsername(String username);
}
