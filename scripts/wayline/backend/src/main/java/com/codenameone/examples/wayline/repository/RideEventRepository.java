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
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.RideEvent;

import java.time.Instant;
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
    List<Object[]> reached(@Param("since") Instant since, @Param("state") RideState state);
}
