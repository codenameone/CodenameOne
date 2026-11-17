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
package com.codenameone.developerguide.backend.graphql;

import com.codename1.backend.annotations.Argument;
import com.codename1.backend.annotations.BatchMapping;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.MutationMapping;
import com.codename1.backend.annotations.QueryMapping;
import com.codename1.backend.annotations.SubscriptionMapping;
import com.codename1.backend.graphql.GraphQLStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

// tag::backend-graphql-controller[]
@Controller
public class BookController {
    private final List<Book> books = new CopyOnWriteArrayList<Book>();
    private final List<GraphQLStream.Emitter> listeners =
            new CopyOnWriteArrayList<GraphQLStream.Emitter>();

    @QueryMapping
    public Book book(@Argument("id") String id) {
        for (Book b : books) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }

    @QueryMapping
    public List<Book> books() {
        return new ArrayList<Book>(books);
    }

    @MutationMapping
    public Book addBook(@Argument("input") BookInput input) {
        Book b = new Book(String.valueOf(books.size() + 1), input.title, input.authorId);
        books.add(b);
        for (GraphQLStream.Emitter e : listeners) {
            e.next(b);
        }
        return b;
    }

    // One call for the authors of every book in the list, not one per book.
    @BatchMapping
    public Map<Book, Author> author(List<Book> books) {
        Map<Book, Author> out = new HashMap<Book, Author>();
        for (Book b : books) {
            out.put(b, new Author(b.authorId, "Author " + b.authorId));
        }
        return out;
    }

    @SubscriptionMapping
    public GraphQLStream bookAdded() {
        return emitter -> {
            listeners.add(emitter);
            return () -> listeners.remove(emitter);
        };
    }
}
// end::backend-graphql-controller[]
