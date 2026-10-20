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
package com.codenameone.developerguide.snippets.orm;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import com.codename1.annotations.db.ElementCollection;
import com.codename1.annotations.db.JoinTable;
import com.codename1.annotations.db.PrePersist;
import com.codename1.annotations.db.Temporal;
import com.codename1.annotations.db.TemporalType;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/** Entity mapped over tables that existed before the application. */
// tag::existing-schema-entity[]
@Entity(table = "legacy_customer")
public class LegacyCustomer {
    @Id @Column(name = "customer_no")
    private long id; // <1>

    @Column(name = "full_name", nullable = false)
    private String name;

    @Temporal(TemporalType.TIMESTAMP) @Column(name = "signed_up")
    private Date signedUp; // <2>

    @Temporal(TemporalType.DATE)
    private Date birthday;

    @Column(name = "last_seen")
    private Date lastSeen; // <3>

    @ElementCollection(column = "tag", positionColumn = "")
    @JoinTable(name = "legacy_customer_tag", joinColumn = "customer_no")
    private Set<String> tags = new HashSet<String>(); // <4>

    @PrePersist
    private void defaults() { // <5>
        if (signedUp == null) {
            signedUp = new Date();
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getSignedUp() {
        return signedUp;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Set<String> getTags() {
        return tags;
    }
}
// end::existing-schema-entity[]
