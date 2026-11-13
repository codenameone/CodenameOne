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
package com.codename1.backend.document;

import com.codename1.impl.orm.EntityModel;
import com.codename1.impl.orm.Values;
import com.codename1.orm.session.PersistenceException;
import com.codename1.orm.session.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// A [Query] over a document collection. Comparisons, `LIKE`, `IN`, null tests,
/// one sort field and paging are supported; joins, fetches and element tests
/// throw [UnsupportedOperationException], because a document store has no join.
public final class DocumentQuery<T> implements Query<T> {
    private final DocumentSession session;
    private final EntityModel<T> model;
    private final List criteria = new ArrayList();
    private String sort;
    private boolean ascending = true;
    private int offset;
    private int limit = -1;
    private boolean empty;

    DocumentQuery(DocumentSession session, EntityModel<T> model) {
        this.session = session;
        this.model = model;
    }

    private String column(String field) {
        return DocumentSession.column(model, model.index(field));
    }

    /// A value as the store holds it for `field`: a key column is stored in its
    /// storage form (see [DocumentSession]), every other one in its own type.
    private Object stored(String field, Object value) {
        int i = model.index(field);
        // Compared against what toDocument(m, entity) wrote, so the value goes
        // through the same attribute conversion first.
        if (model.mapping(i).length() > 0) {
            value = model.parameter(i, value);
        }
        return model.attributes()[i].id ? Values.storage(value)
                : DocumentSession.toDocument(value);
    }

    private DocumentQuery<T> add(String field, int op, Object value) {
        criteria.add(new Criterion(column(field), op, stored(field, value)));
        return this;
    }

    @Override
    public Query<T> join(String path) {
        throw new UnsupportedOperationException("A document store has no joins");
    }

    @Override
    public Query<T> leftJoin(String path) {
        throw new UnsupportedOperationException("A document store has no joins");
    }

    @Override
    public Query<T> fetch(String field) {
        throw new UnsupportedOperationException("A document entity has no relationships");
    }

    @Override
    public Query<T> containsElement(String field, Object value) {
        throw new UnsupportedOperationException("A document entity has no collections");
    }

    @Override
    public Query<T> eq(String field, Object value) {
        return add(field, Criterion.EQ, value);
    }

    @Override
    public Query<T> ne(String field, Object value) {
        return add(field, Criterion.NE, value);
    }

    @Override
    public Query<T> gt(String field, Object value) {
        return add(field, Criterion.GT, value);
    }

    @Override
    public Query<T> ge(String field, Object value) {
        return add(field, Criterion.GE, value);
    }

    @Override
    public Query<T> lt(String field, Object value) {
        return add(field, Criterion.LT, value);
    }

    @Override
    public Query<T> le(String field, Object value) {
        return add(field, Criterion.LE, value);
    }

    @Override
    public Query<T> like(String field, String pattern) {
        return add(field, Criterion.LIKE, pattern);
    }

    @Override
    public Query<T> isNull(String field) {
        return add(field, Criterion.EQ, null);
    }

    @Override
    public Query<T> isNotNull(String field) {
        return add(field, Criterion.NE, null);
    }

    @Override
    public Query<T> in(String field, Object... values) {
        if (values == null || values.length == 0) {
            empty = true;
            return this;
        }
        Object[] stored = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            stored[i] = stored(field, values[i]);
        }
        criteria.add(new Criterion(column(field), Criterion.IN, stored));
        return this;
    }

    @Override
    public Query<T> orderBy(String field, boolean ascending) {
        this.sort = column(field);
        this.ascending = ascending;
        return this;
    }

    @Override
    public Query<T> limit(int value) {
        this.limit = value;
        return this;
    }

    @Override
    public Query<T> offset(int value) {
        this.offset = value;
        return this;
    }

    private List<T> run(int max) {
        List<T> out = new ArrayList<T>();
        if (empty) {
            return out;
        }
        try {
            List docs = session.run(model, criteria, sort, ascending, offset, max);
            for (Object eachDoc : docs) {
                out.add(session.fromDocument(model, (java.util.Map) eachDoc));
            }
            return out;
        } catch (IOException err) {
            throw new PersistenceException("Query failed: " + model.type().getName(), err);
        }
    }

    @Override
    public List<T> list() {
        return run(limit);
    }

    @Override
    public T first() {
        List<T> one = run(limit == 0 ? 0 : 1);
        return one.isEmpty() ? null : one.get(0);
    }

    @Override
    public long count() {
        if (empty) {
            return 0;
        }
        try {
            return session.count(model, criteria);
        } catch (IOException err) {
            throw new PersistenceException("Count failed: " + model.type().getName(), err);
        }
    }
}
