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
package com.codenameone.examples.wayline.account;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Session;

/// What closing or restricting an account does to the rows other parts of the
/// server keep under its name.
///
/// These are statements over whole sets of rows -- everything of one account in
/// a table -- and not the reading and writing of one kind of entity, which is
/// why they are here and not in the repositories of the packages that own the
/// tables. Each is a bulk statement, so whatever the session had loaded before
/// it is stale afterwards and must be read again before it is used.
@Component
public class AccountCleanupRepository {
    /// The states of a ride that somebody is still waiting on.
    private static final String[] UNDER_WAY = {"REQUESTED", "OFFERED", "ACCEPTED", "ARRIVED",
        "IN_PROGRESS"};

    private final Session session;

    public AccountCleanupRepository(Session session) {
        this.session = session;
    }

    /// Whether the account is the rider or the driver of a ride that has not
    /// ended.
    public boolean hasRideUnderWay(String username) {
        Long rides = session.createQuery("select count(r) from Ride r where "
                + "(r.rider = :username or r.driver = :username) and r.state in :states",
                Long.class).setParameter("username", username)
                .setParameter("states", UNDER_WAY).first();
        return rides != null && rides.longValue() > 0L;
    }

    /// Takes a driver off line, when the account stops being allowed to drive.
    /// Does nothing for an account that never drove.
    public void takeOffLine(String username) {
        session.createQuery("update DriverState d set d.online = :online "
                + "where d.username = :username").setParameter("online", Boolean.FALSE)
                .setParameter("username", username).executeUpdate();
    }

    /// Deletes what an account leaves in the tables that are keyed by it and
    /// belong to nobody's `forget`: its preferences, where it is paid, the
    /// hours it drove, its place on the map, a code it was texted and the
    /// tickets it held for the live connection.
    public void removeEverythingOf(String username) {
        delete("delete from Preference p where p.username = :username", username);
        delete("delete from PayoutAccount p where p.username = :username", username);
        delete("delete from DriverHours h where h.username = :username", username);
        delete("delete from DriverState d where d.username = :username", username);
        delete("delete from PhoneCode c where c.username = :username", username);
        delete("delete from LiveTicket t where t.username = :username", username);
    }

    private void delete(String statement, String username) {
        session.createQuery(statement).setParameter("username", username).executeUpdate();
    }
}
