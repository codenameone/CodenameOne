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
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.wayline.domain.RidePassed;

import java.util.List;

/// The drivers who passed on a ride or let its offer lapse, keyed by
/// `Identifier.of(rideId, driver)`.
@Repository(entity = RidePassed.class, key = Identifier.class)
public interface RidePassedRepository extends CrudRepository<RidePassed, Identifier> {
    List<RidePassed> findByRideId(String rideId);

    long countByDriver(String driver);

    /// How many offers each driver has passed on or let lapse, as `{driver,
    /// count}`.
    @Query("select p.driver, count(p.rideId) from RidePassed p group by p.driver")
    List<Object[]> countByDriver();
}
