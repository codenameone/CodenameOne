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

import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestBody;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.validation.Min;
import com.codename1.backend.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/** The controller half of the validation example. */
@RestController
public class ValidatedNotes {
    private final List<NoteView> notes = new ArrayList<NoteView>();

    // tag::backend-web-validation-controller[]
    @PostMapping("/notes")
    public NoteView create(@Valid @RequestBody NewNote note) {
        NoteView saved = new NoteView();
        saved.id = notes.size() + 1;
        saved.title = note.title;
        notes.add(saved);
        return saved;
    }

    @GetMapping("/notes")
    public List<NoteView> page(@RequestParam("page") @Min(1) int page) {
        int from = Math.min((page - 1) * 20, notes.size());
        return new ArrayList<NoteView>(notes.subList(from, Math.min(from + 20, notes.size())));
    }
    // end::backend-web-validation-controller[]
}
