/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.l10n;

/**
 * A factory for a set of localized resources, mirroring Flutter's
 * {@code LocalizationsDelegate<T>}. Opaque marker in this runtime; the type
 * parameter {@code T} (the resource type the delegate loads) exists so
 * transpiled {@code LocalizationsDelegate<GalleryLocalizations>} type arguments
 * resolve.
 *
 * @param <T> the localized-resources type this delegate produces
 */
public class LocalizationsDelegate<T> {

    /**
     * Loads the localized resources for {@code locale}. Generated delegates
     * override this with a {@code SynchronousFuture} of the resource instance;
     * the base returns null so opaque runtime delegates (material/cupertino/
     * widgets globals) are simply skipped by the MaterialApp load pass.
     */
    public dart.async.Future<T> load(com.codename1.flutter.Locale locale) {
        return null;
    }
}
