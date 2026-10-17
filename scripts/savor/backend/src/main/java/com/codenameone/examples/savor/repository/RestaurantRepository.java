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
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.savor.domain.Restaurant;

import java.util.List;

/// The restaurants, approved or not.
@Repository(entity = Restaurant.class, key = String.class)
public interface RestaurantRepository extends CrudRepository<Restaurant, String> {
    /// The approved restaurants inside a box, for the caller to measure and sort.
    @Query("select r from Restaurant r where r.status = :status "
            + "and r.lat between :south and :north and r.lng between :west and :east")
    List<Restaurant> inBox(@Param("status") String status, @Param("south") double south,
            @Param("north") double north, @Param("west") double west, @Param("east") double east);

    List<Restaurant> findByStatusOrderByCreatedAtDesc(String status);

    @Query("select r from Restaurant r order by r.createdAt desc")
    List<Restaurant> newest(PageRequest page);

    Restaurant findFirstByApplicantOrderByCreatedAtDesc(String applicant);

    long countByStatus(String status);

    /// Adds a rating to a restaurant's running total, in the database.
    @Modifying
    @Query("update Restaurant r set r.ratingTotal = r.ratingTotal + :stars, "
            + "r.ratingCount = r.ratingCount + 1 where r.id = :id")
    int addRating(@Param("id") String id, @Param("stars") long stars);
}
