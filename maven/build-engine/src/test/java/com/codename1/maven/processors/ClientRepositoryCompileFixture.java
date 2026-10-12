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
package com.codename1.maven.processors;

import com.codename1.build.SystemStreamLog;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Runs in a JVM whose classpath contains no backend artifact.
public final class ClientRepositoryCompileFixture {
    private ClientRepositoryCompileFixture() {
    }

    public static void main(String[] args) throws Exception {
        try {
            Class.forName("com.codename1.backend.annotations.Generated");
            throw new AssertionError("Backend artifact leaked into client compilation");
        } catch (ClassNotFoundException expected) {
            // This is an ordinary client build.
        }
        File classes = new File(args[0]);
        Map<String,String> sources = new LinkedHashMap<String,String>();
        sources.put("client.Note", "package client; @com.codename1.annotations.Entity public class Note { @com.codename1.annotations.Id public long id; }");
        sources.put("client.Notes", "package client; @com.codename1.orm.repository.Repository(entity=Note.class,key=Long.class) public interface Notes extends com.codename1.orm.repository.CrudRepository<Note,Long> {}");
        sources.put("client.Caller", "package client; public class Caller { public static Notes open(com.codename1.orm.session.Session s) { return com.codename1.orm.repository.Repositories.create(Notes.class,s); } }");
        JavaSourceCompiler.compile(sources, classes, Collections.<File>emptyList());
        for (int pass = 0; pass < 2; pass++) {
            Map<String,AnnotatedClass> index = ClassScanner.scan(classes);
            ProcessorContext context = new ProcessorContext(classes, classes, index, new SystemStreamLog());
            OrmAnnotationProcessor processor = new OrmAnnotationProcessor();
            processor.start(context);
            for (AnnotatedClass type : index.values()) {
                if (!type.getClassAnnotations().isEmpty()) {
                    processor.processClass(type, context);
                }
            }
            processor.finish(context);
            if (context.hasErrors()) {
                throw new AssertionError(context.getErrors().toString());
            }
        }
        if (!new File(classes,"client/NotesCn1Repository.class").isFile()
                || !new File(classes,"client/NotesCn1Repository$Factory.class").isFile()) {
            throw new AssertionError("Client repository was not generated");
        }
    }
}
