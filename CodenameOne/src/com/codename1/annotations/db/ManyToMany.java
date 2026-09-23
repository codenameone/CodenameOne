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
package com.codename1.annotations.db;

import java.lang.annotation.*;
/// Maps entity collection membership through a join table or an inverse mappedBy field.
/// Owning Lists store occurrences in a list_position column by default, allowing
/// repeated links. OrderColumn can override its name; OrderBy controls load order.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface ManyToMany {
    /// Controls relationship initialization.
    /// @return fetch policy; EAGER for to-one and LAZY for collections by default
    FetchType fetch() default FetchType.LAZY;
    /// Chooses operations to propagate to related entities.
    /// @return cascade operations; empty by default
    CascadeType[] cascade() default {};
    /// Names the owning Java relationship field on the target entity.
    /// @return owning field name, or empty for the owning side
    String mappedBy() default "";
}
