/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.sql;

import com.codename1.backend.ConcurrencyFailureException;
import com.codename1.backend.DataAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which engine errors mean "another transaction got there first".
 *
 * <p>The difference matters to the caller and to nobody else: such a failure is
 * answered by running the transaction again or by saying that someone else made
 * the change, and any other failure is a fault. An error wrongly recognized is
 * retried for ever, and one wrongly missed is a 500 for the loser of an ordinary
 * race.
 */
class ConcurrencyFailureTest {

    @Test
    @DisplayName("MySQL and MariaDB: a deadlock, a lock wait, NOWAIT and a changed record")
    void recognizesMySqlFamilyConflicts() {
        assertTrue(MySql.isConcurrencyFailure(1213, "40001"));
        assertTrue(MySql.isConcurrencyFailure(1205, "HY000"));
        assertTrue(MySql.isConcurrencyFailure(3572, "HY000"));
        // MariaDB, holding a REPEATABLE READ transaction to its snapshot.
        assertTrue(MySql.isConcurrencyFailure(1020, "HY000"));
        // A number this client does not know, in the standard's rollback class.
        assertTrue(MySql.isConcurrencyFailure(1637, "40000"));
        // An error packet from before SQLSTATEs carries none.
        assertTrue(MySql.isConcurrencyFailure(1213, ""));
    }

    @Test
    @DisplayName("MySQL and MariaDB: a duplicate key or a syntax error is not a conflict")
    void leavesOtherMySqlErrorsAlone() {
        assertFalse(MySql.isConcurrencyFailure(1062, "23000"));
        assertFalse(MySql.isConcurrencyFailure(1064, "42000"));
        assertFalse(MySql.isConcurrencyFailure(1146, "42S02"));
        assertFalse(MySql.isConcurrencyFailure(1045, ""));
        assertFalse(MySql.isConcurrencyFailure(2000, null));
    }

    @Test
    @DisplayName("PostgreSQL: a serialization failure, a deadlock and an unavailable lock")
    void recognizesPostgresConflicts() {
        assertTrue(Postgres.isConcurrencyFailure("40001"));
        assertTrue(Postgres.isConcurrencyFailure("40P01"));
        assertTrue(Postgres.isConcurrencyFailure("55P03"));
    }

    @Test
    @DisplayName("PostgreSQL: a unique violation, an aborted transaction or no code is not a conflict")
    void leavesOtherPostgresErrorsAlone() {
        assertFalse(Postgres.isConcurrencyFailure("23505"));
        assertFalse(Postgres.isConcurrencyFailure("25P02"));
        assertFalse(Postgres.isConcurrencyFailure("42601"));
        // Rolled back for a constraint checked at commit: running it again
        // fails again.
        assertFalse(Postgres.isConcurrencyFailure("40002"));
        assertFalse(Postgres.isConcurrencyFailure(null));
    }

    @Test
    @DisplayName("a conflict is found under whatever wrapped it")
    void findsTheConflictInAChainOfCauses() {
        ConcurrencyFailureException conflict = new ConcurrencyFailureException("deadlock");
        assertTrue(ConcurrencyFailureException.isCauseOf(conflict));
        // What an ORM session throws: unchecked, with the failure as its cause.
        assertTrue(ConcurrencyFailureException.isCauseOf(
                new RuntimeException("flush failed", new IOException("wrapped", conflict))));
        // It is a data access failure, so a transaction rolls back for it.
        assertTrue(DataAccessException.class.isInstance(conflict));
    }

    @Test
    @DisplayName("any other failure is not a conflict, and neither is none")
    void answersFalseForEverythingElse() {
        assertFalse(ConcurrencyFailureException.isCauseOf(null));
        assertFalse(ConcurrencyFailureException.isCauseOf(new DataAccessException("syntax")));
        assertFalse(ConcurrencyFailureException.isCauseOf(
                new RuntimeException("flush failed", new IOException("the connection dropped"))));
    }

    @Test
    @DisplayName("a chain of causes that loops ends the search")
    void survivesACycleOfCauses() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);
        assertFalse(ConcurrencyFailureException.isCauseOf(first));
    }
}
