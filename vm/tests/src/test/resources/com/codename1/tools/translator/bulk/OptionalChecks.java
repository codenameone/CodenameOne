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

import java.util.NoSuchElementException;
import java.util.Optional;

/// java.util.Optional.
public final class OptionalChecks {
    private OptionalChecks() {
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("Optional.presentValue", () -> {
            Optional<String> o = Optional.of("cn1");
            Bulk.p("isPresent", o.isPresent());
            Bulk.p("get", o.get());
            Bulk.p("orElse", o.orElse("x"));
            Bulk.p("map", o.map(String::length).get());
            Bulk.p("filterTrue", o.filter(s -> s.startsWith("c")).isPresent());
            Bulk.p("filterFalse", o.filter(s -> s.startsWith("z")).isPresent());
            Bulk.p("hashCode", o.hashCode());
            Bulk.p("toString", o.toString());
            Bulk.p("equalsOf", o.equals(Optional.of("cn1")));
        });
        Bulk.check("Optional.emptyValue", () -> {
            Optional<String> o = Optional.ofNullable(null);
            Bulk.p("isPresent", o.isPresent());
            Bulk.p("orElse", o.orElse("x"));
            Bulk.p("orElseGet", o.orElseGet(() -> "y"));
            Bulk.p("hashCode", o.hashCode());
            Bulk.p("toString", o.toString());
            Bulk.p("equalsEmpty", o.equals(Optional.empty()));
            Bulk.pThrown("get", NoSuchElementException.class, o::get);
            Bulk.pThrown("ofNull", NullPointerException.class, () -> Optional.of(null));
        });
    }
}
