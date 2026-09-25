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
package com.codenameone.developerguide.snippets;

import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import com.codename1.annotations.db.ManyToOne;
import com.codename1.orm.EntityManager;
import com.codename1.orm.session.Session;
import java.util.List;

public class ManagedOrmSnippets {
    @Entity
    public static class Customer {
        @Id public long id;
        public String name;
    }

    @Entity
    public static class Order {
        @Id public long id;
        @ManyToOne public Customer customer;
    }

    public void updateCustomer(EntityManager em, long customerId) {
        // tag::managed-orm-unit-of-work[]
        Session session = em.openSession();
        try {
            session.beginTransaction();
            Customer customer = session.find(Customer.class, customerId);
            customer.name = "Updated name";
            session.commitTransaction();
        } finally {
            session.close();
        }
        // end::managed-orm-unit-of-work[]
    }

    public void builderQuery(Session session) {
        // tag::managed-orm-builder-query[]
        List<Order> orders = session.query(Order.class)
            .eq("customer.name", "Alice")
            .orderBy("id", true)
            .limit(20)
            .fetch("customer")
            .list();
        // end::managed-orm-builder-query[]
    }

    public void jpqlQuery(Session session) {
        // tag::managed-orm-jpql-query[]
        List<Order> orders = session.createQuery(
            "select o from Order o where o.customer.name = :name order by o.id",
            Order.class).setParameter("name", "Alice").list();
        // end::managed-orm-jpql-query[]
    }

}
