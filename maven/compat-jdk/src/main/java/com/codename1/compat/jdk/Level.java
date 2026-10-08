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
package com.codename1.compat.jdk;

/// `java.util.logging.Level` for the Codename One runtime: the standard
/// levels, and room for an application's own -- the constructor is
/// protected, as the JDK's is, so a class may extend this one to add them.
///
/// [Logger] writes through Codename One's `Log`, which has four levels; see
/// there for how these map onto them.
public class Level implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final Level OFF = new Level("OFF", Integer.MAX_VALUE);
    public static final Level SEVERE = new Level("SEVERE", 1000);
    public static final Level WARNING = new Level("WARNING", 900);
    public static final Level INFO = new Level("INFO", 800);
    public static final Level CONFIG = new Level("CONFIG", 700);
    public static final Level FINE = new Level("FINE", 500);
    public static final Level FINER = new Level("FINER", 400);
    public static final Level FINEST = new Level("FINEST", 300);
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE);

    private final String name;
    private final int value;
    private final String resourceBundleName;

    protected Level(String name, int value) {
        this(name, value, null);
    }

    protected Level(String name, int value, String resourceBundleName) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.value = value;
        this.resourceBundleName = resourceBundleName;
    }

    public String getName() {
        return name;
    }

    /// The name: a device carries no translations of the level names.
    public String getLocalizedName() {
        return name;
    }

    public String getResourceBundleName() {
        return resourceBundleName;
    }

    public final int intValue() {
        return value;
    }

    private static Level[] standard() {
        return new Level[] {OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL};
    }

    /// The standard level called `name`, in any case, or whose number
    /// `name` spells. A number that belongs to no standard level makes a
    /// level of its own, named by the number.
    public static Level parse(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        String trimmed = name.trim();
        Level[] all = standard();
        for (Level level : all) {
            if (level.name.equalsIgnoreCase(trimmed)) {
                return level;
            }
        }
        int number;
        try {
            number = Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad level \"" + name + "\"");
        }
        for (Level level : all) {
            if (level.value == number) {
                return level;
            }
        }
        return new Level(trimmed, number);
    }

    @Override
    public final String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Level && ((Level) other).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
