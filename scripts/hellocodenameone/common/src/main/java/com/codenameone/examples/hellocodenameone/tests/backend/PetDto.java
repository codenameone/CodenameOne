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
package com.codenameone.examples.hellocodenameone.tests.backend;

import com.codename1.annotations.Mapped;

/// The app's copy of the backend's Pet: the same fields, mapped to and from JSON by
/// the generated mapper. Kept separate from the server's class on purpose -- the
/// app and the server share a wire format, not a jar -- and the typed client test
/// is what holds the two together.
@Mapped
public class PetDto {
    public Long id;
    public String name;
    public String species;
    public Integer age;
    public Boolean vaccinated;

    public PetDto() {
    }

    public PetDto(String name, String species, int age) {
        this.name = name;
        this.species = species;
        this.age = Integer.valueOf(age);
        this.vaccinated = Boolean.FALSE;
    }
}
