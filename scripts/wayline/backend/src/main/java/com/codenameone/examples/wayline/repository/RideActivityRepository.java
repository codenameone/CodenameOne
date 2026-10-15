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
import com.codenameone.examples.wayline.domain.RideActivity;

/// The Live Activities riders follow their rides in, by ride.
@Repository(entity = RideActivity.class, key = String.class)
public interface RideActivityRepository extends CrudRepository<RideActivity, String> {
    int deleteByRideId(String rideId);

    int deleteByUsername(String username);

    /// Records that a state went out, unless one went out after `before`:
    /// 1 for the caller who gets to send, 0 for anyone else at the same moment.
    @Modifying
    @Query("update RideActivity a set a.sentAt = :now "
            + "where a.rideId = :rideId and a.sentAt < :before")
    int claim(@Param("rideId") String rideId, @Param("now") long now,
            @Param("before") long before);
}
