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
package com.codenameone.developerguide.backend.advice;

import com.codename1.backend.annotations.ExceptionHandler;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.ResponseStatus;
import com.codename1.backend.annotations.RestController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// tag::backend-web-exception-handler[]
@RestController
public class NotesWithHandler {
    private final Map<Long, NoteView> notes = new HashMap<Long, NoteView>();

    @GetMapping("/notes/{id}")
    public NoteView note(@PathVariable("id") long id) {
        NoteView note = notes.get(id);
        if (note == null) {
            throw new NoteMissing(id);
        }
        return note;
    }

    @ExceptionHandler(NoteMissing.class)
    @ResponseStatus(404)
    public Map<String, String> missing(NoteMissing e) {
        return Collections.singletonMap("error", e.getMessage());
    }
}
// end::backend-web-exception-handler[]
