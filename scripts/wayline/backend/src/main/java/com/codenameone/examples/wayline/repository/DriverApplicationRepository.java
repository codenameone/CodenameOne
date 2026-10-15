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
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.DriverApplication;

import java.util.List;

/// Applications to drive, one per account at most, keyed by the account.
///
/// Every move of an application from one status to another is one conditional
/// statement answering how many rows it changed, so that of two callers moving
/// the same application out of the same status the count is 1 for one of them
/// and 0 for the other. Each empties the session: an application read before
/// it has to be read again.
@Repository(entity = DriverApplication.class, key = String.class)
public interface DriverApplicationRepository extends CrudRepository<DriverApplication, String> {
    /// The applications of these accounts; none for a name that made none.
    List<DriverApplication> findByUsernameIn(List<String> usernames);

    /// The applications of every account that has a driver's state.
    @Query("select a from DriverApplication a where a.username in "
            + "(select d.username from DriverState d)")
    List<DriverApplication> ofDrivers();

    /// The name and status of every application, as `{username, status}`.
    @Query("select a.username, a.status from DriverApplication a")
    List<Object[]> statuses();

    long countByStatus(String status);

    /// Applications of every status, the latest handed in first. Only those
    /// of accounts that still have a profile, as the list names the
    /// applicant.
    @Query("select a from DriverApplication a where a.username in "
            + "(select p.username from Profile p) order by a.submittedAt desc, a.username asc")
    List<DriverApplication> newest(PageRequest page);

    /// Applications in one status, the longest waiting first; only those of
    /// accounts that still have a profile.
    @Query("select a from DriverApplication a where a.status = :status "
            + "and a.username in (select p.username from Profile p) "
            + "order by a.submittedAt asc, a.username asc")
    List<DriverApplication> inStatus(@Param("status") String status, PageRequest page);

    /// Moves an application from status `from` to `to`, recording the review.
    @Modifying
    @Query("update DriverApplication a set a.status = :to, a.rejectionReason = :reason, "
            + "a.reviewedAt = :at, a.reviewedBy = :by "
            + "where a.username = :username and a.status = :from")
    int move(@Param("username") String username, @Param("from") String from,
            @Param("to") String to, @Param("reason") String reason, @Param("at") long at,
            @Param("by") String by);

    /// Marks an application `approved` unless it already is.
    @Modifying
    @Query("update DriverApplication a set a.status = :to, a.rejectionReason = '', "
            + "a.reviewedAt = :at, a.reviewedBy = :by "
            + "where a.username = :username and a.status <> :to")
    int approveUnlessApproved(@Param("username") String username,
            @Param("to") String approved, @Param("at") long at, @Param("by") String by);

    /// Hands an application in: from the status it was read in to `to`, with
    /// the last rejection's reason cleared.
    @Modifying
    @Query("update DriverApplication a set a.status = :to, a.submittedAt = :at, "
            + "a.rejectionReason = '' where a.username = :username and a.status = :from")
    int submit(@Param("username") String username, @Param("from") String from,
            @Param("to") String to, @Param("at") long at);

    /// Makes the rest of the transaction wait its turn behind any other that
    /// holds, or moves, the same application.
    ///
    /// @return 1, or 0 for an account that has no application yet
    @Modifying
    @Query("update DriverApplication a set a.status = a.status where a.username = :username")
    int hold(@Param("username") String username);

    int deleteByUsername(String username);
}
