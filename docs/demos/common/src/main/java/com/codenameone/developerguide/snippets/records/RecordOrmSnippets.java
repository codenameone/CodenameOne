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
package com.codenameone.developerguide.snippets.records;

import com.codename1.orm.Ref;
import com.codename1.orm.repository.Repositories;
import com.codename1.orm.session.Session;
import java.util.List;
import java.util.Optional;

public class RecordOrmSnippets {
    public void save(Session session) {
        // tag::record-save[]
        Notes notes = Repositories.create(Notes.class, session);
        session.beginTransaction();
        Note draft = NoteBuilder.create().title("Groceries").score(3).build();
        Note saved = notes.save(draft);       // saved.id() is the generated key
        Note changed = notes.save(NoteBuilder.of(saved).score(5).build());
        session.commitTransaction();
        // end::record-save[]
    }

    public void read(Session session, long id) {
        // tag::record-read[]
        Purchase p = session.find(Purchase.class, id);
        Object customerKey = p.customer().id();          // no query
        boolean hasOne = p.customer().isPresent();       // no query
        String name = p.customer().get().name();         // reads the customer now
        String reviewer = p.reviewer().map(Customer::name).orElse("nobody");
        int lines = p.lines().size();                    // reads the lines now
        // end::record-read[]
    }

    public void move(Session session, Purchase p, long adaId) {
        // tag::record-move[]
        Customer ada = session.find(Customer.class, adaId);
        Purchase moved = session.merge(PurchaseBuilder.of(p)
                .customer(Ref.of(ada))
                .reviewer(Optional.empty())
                .build());
        // end::record-move[]
    }

    public void venues(Session session, Venue venue) {
        // tag::record-venue[]
        List<Venue> local = session.query(Venue.class)
                .eq("home.city", "Springfield").list();

        Venue moved = session.merge(VenueBuilder.of(venue)
                .home(new Address("2 Elm", "Springfield", new Geo(1.5, 2.5)))
                .build());
        // end::record-venue[]
    }
}
