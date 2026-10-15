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
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.RideEvent;

import java.util.List;

/// What happened to each ride: one row for every state it came into.
@Repository(entity = RideEvent.class, key = String.class)
public interface RideEventRepository extends CrudRepository<RideEvent, String> {
    long countByActorAndState(String actor, RideState state);

    /// How many offers each driver has accepted, as `{driver, count}`.
    @Query("select e.actor, count(e) from RideEvent e where e.state = :accepted "
            + "group by e.actor")
    List<Object[]> countByActor(@Param("accepted") RideState accepted);

    /// When each ride asked for since `since` came into `state`: `{rideId,
    /// createdAt}`. The entities have no relationship to join along, so the
    /// rides are named by a nested query.
    @Query("select e.rideId, e.createdAt from RideEvent e where e.state = :state "
            + "and e.rideId in (select r.id from Ride r where r.requestedAt >= :since)")
    List<Object[]> reached(@Param("since") long since, @Param("state") RideState state);
}
