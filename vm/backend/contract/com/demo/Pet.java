/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/**
 * A DTO on the shared contract. Public fields, public no-arg constructor: the
 * generated codec reads and writes them by name, with no reflection - ParparVM has
 * none worth relying on and Codename One obfuscates, so a runtime name lookup
 * would fail in exactly the builds that matter.
 */
public class Pet {
    public long id;
    public String name;
    public String species;
    public double weight;
    public boolean good;
    /** A DTO-typed collection: read and written through the Tag codec. */
    public java.util.List<Tag> tags;

    public Pet() {
    }
}
