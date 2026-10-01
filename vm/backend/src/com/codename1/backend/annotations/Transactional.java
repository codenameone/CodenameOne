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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Runs this method -- or every public method of this class -- in a database
/// transaction.
///
/// The build rewrites the compiled method: its body moves into a private method
/// of its own, and the method keeps its name and wraps that body in a begin and
/// a commit, with a rollback on failure. Everything is decided at build time --
/// the propagation, the rules below, the exception types -- and written in as
/// constants and `instanceof` tests, so there is nothing to interpret on the
/// call.
///
/// Because the method itself is rewritten rather than wrapped by a proxy, the
/// transaction applies however it is called: from another bean, from `this`,
/// from a private caller, or on an object the application built with `new`.
/// That is the one place this differs from Spring, where a call through `this`
/// silently skips the transaction.
///
/// Rollback follows Spring's rule: an unchecked exception or an `Error` rolls
/// back, a checked exception commits, and [#rollbackFor] and [#noRollbackFor]
/// adjust that. When several listed types match the thrown one, the most
/// specific wins.
///
/// Everything done through the server's [com.codename1.backend.DataSource] --
/// its own methods, an entity manager's daos, the transaction's
/// `com.codename1.orm.session.Session` -- joins the transaction on the calling
/// thread. A connection is borrowed only when the transaction first needs one.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Transactional {
    /// What to do about a transaction that is already open.
    Propagation propagation() default Propagation.REQUIRED;
    /// A hint that the method only reads. Engines that support a read-only
    /// transaction get one, which lets them skip work and refuse writes.
    boolean readOnly() default false;
    /// Seconds the transaction may take before its commit is refused and it is
    /// rolled back. Zero or less means no limit.
    int timeout() default -1;
    /// Exception types that roll back, in addition to unchecked ones.
    Class[] rollbackFor() default {};
    /// Exception types that commit even though they would roll back.
    Class[] noRollbackFor() default {};
}
