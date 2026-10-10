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
package com.example.gallery;

/**
 * A row of the data the list, the tables and the tree show.
 */
public class Planet {

    private final String name;
    private final double mass;
    private final int moons;
    private boolean visited;

    public Planet(String name, double mass, int moons) {
        this.name = name;
        this.mass = mass;
        this.moons = moons;
    }

    public String getName() {
        return name;
    }

    /** In Earth masses. */
    public double getMass() {
        return mass;
    }

    public int getMoons() {
        return moons;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public static Planet[] all() {
        return new Planet[] {
            new Planet("Mercury", 0.055, 0),
            new Planet("Venus", 0.815, 0),
            new Planet("Earth", 1.0, 1),
            new Planet("Mars", 0.107, 2),
            new Planet("Jupiter", 317.8, 95),
            new Planet("Saturn", 95.2, 146),
            new Planet("Uranus", 14.5, 28),
            new Planet("Neptune", 17.1, 16),
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
