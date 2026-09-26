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
package com.codename1.backend;

/**
 * A transaction could not begin, commit or roll back the way a
 * {@code @Transactional} method asked.
 *
 * <p>Unchecked, as Spring's is: a woven method keeps the signature its author
 * wrote, and that signature cannot be made to declare a failure the author never
 * saw. The subclasses name the cases a caller might want to tell apart.
 */
public class TransactionException extends RuntimeException {
    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * A transaction was rolled back although its own method returned normally,
     * because a method that joined it failed and marked it rollback-only.
     *
     * <p>Thrown rather than returning quietly, because the outer method's caller
     * would otherwise believe its work was committed.
     */
    public static class UnexpectedRollback extends TransactionException {
        public UnexpectedRollback(String message) {
            super(message);
        }
    }

    /** A MANDATORY method was called with no transaction, or a NEVER one inside one. */
    public static class IllegalState extends TransactionException {
        public IllegalState(String message) {
            super(message);
        }
    }

    /** The transaction ran past its {@code timeout} and was rolled back. */
    public static class TimedOut extends TransactionException {
        public TimedOut(String message) {
            super(message);
        }
    }
}
