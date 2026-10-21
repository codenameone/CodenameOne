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
package com.demo;

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

/**
 * Native date and time columns, a value-keyed tag table and a private callback,
 * so the translated server runs the same @Temporal conversion and woven accessors
 * the JVM does.
 */
@Entity(table = "appointment")
public class Appointment {
    @Id
    private long id;
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "starts_at")
    private Date startsAt;
    @Temporal(TemporalType.DATE)
    private Date day;
    private Date reminded;
    @ElementCollection(column = "label", positionColumn = "")
    @JoinTable(name = "appointment_label")
    private Set<String> labels = new HashSet<String>();
    private String status;

    @PrePersist
    private void defaults() {
        if (status == null) {
            status = "booked";
        }
    }

    public long getId() {
        return id;
    }

    public Date getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Date startsAt) {
        this.startsAt = startsAt;
    }

    public Date getDay() {
        return day;
    }

    public void setDay(Date day) {
        this.day = day;
    }

    public Date getReminded() {
        return reminded;
    }

    public void setReminded(Date reminded) {
        this.reminded = reminded;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public String getStatus() {
        return status;
    }
}
