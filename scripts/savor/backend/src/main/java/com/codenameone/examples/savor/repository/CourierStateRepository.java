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
package com.codenameone.examples.savor.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.domain.CourierState;

import java.util.List;

/// Couriers' live state: whether each is on line, where the car is, and the
/// rating.
@Repository(entity = CourierState.class, key = String.class)
public interface CourierStateRepository extends CrudRepository<CourierState, String> {
    /// Every account that has a state, those on line first and then the ones
    /// heard from most recently.
    @Query("select d from CourierState d order by d.online desc, d.lastSeen desc")
    List<CourierState> onLineFirst();

    /// The couriers on line, heard from since `fresh`, inside a box of
    /// latitude and longitude, whose application stands approved, who still
    /// have a profile, and who have no delivery in hand: one offer, or one delivery,
    /// at a time.
    ///
    /// The approval is read here, in the query, and not trusted to have been
    /// put right elsewhere: an approval withdrawn takes a courier out of
    /// matching at once, whatever their state still says. A state left behind
    /// by an account that is gone is not somebody to send a customer to.
    ///
    /// What the customer asked of the car and the courier is the caller's to
    /// apply to the few this answers.
    @Query("select d from CourierState d where d.online = TRUE and d.lastSeen >= :fresh "
            + "and d.lat between :south and :north and d.lng between :west and :east "
            + "and d.username in (select a.username from CourierApplication a "
            + "where a.status = :approved) "
            + "and d.username in (select p.username from Profile p) "
            + "and d.username not in (select r.courier from Delivery r where r.state in :withDelivery)")
    List<CourierState> freeInBox(@Param("fresh") long fresh, @Param("south") double south,
            @Param("north") double north, @Param("west") double west,
            @Param("east") double east, @Param("approved") String approved,
            @Param("withDelivery") DeliveryState[] withDelivery);

    /// Makes the rest of the transaction wait its turn behind any other that
    /// holds the same courier, as [ProfileRepository#hold] does for an
    /// account.
    ///
    /// @return 1, or 0 for a courier who has no state
    @Modifying
    @Query("update CourierState d set d.lastSeen = d.lastSeen where d.username = :username")
    int hold(@Param("username") String username);

    /// Counts a rating toward a courier's. Arithmetic done by the database, so
    /// two ratings arriving together are both counted.
    ///
    /// @return 1, or 0 for a courier who has no state to count it in
    @Modifying
    @Query("update CourierState d set d.ratingSum = d.ratingSum + :stars, "
            + "d.ratingCount = d.ratingCount + 1 where d.username = :username")
    int addRating(@Param("username") String username, @Param("stars") long stars);

    /// Takes a courier off line. Changes nothing for an account that never
    /// drove.
    @Modifying
    @Query("update CourierState d set d.online = FALSE where d.username = :username")
    int takeOffLine(@Param("username") String username);

    /// The couriers on line who reported in at or after `seenSince`.
    long countByOnlineTrueAndLastSeenGreaterThanEqual(long seenSince);

    int deleteByUsername(String username);
}
