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
package android.database.sqlite;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.CancellationSignal;

import java.util.Map;

/// Builds SELECT statements from a table list, a projection map and
/// optional WHERE fragments, as content providers do.
public class SQLiteQueryBuilder {

    private Map<String, String> mProjectionMap;
    private String mTables = "";
    private StringBuilder mWhereClause;
    private boolean mDistinct;
    private SQLiteDatabase.CursorFactory mFactory;
    private boolean mStrict;

    public SQLiteQueryBuilder() {
    }

    public void setDistinct(boolean distinct) {
        mDistinct = distinct;
    }

    public boolean isDistinct() {
        return mDistinct;
    }

    public String getTables() {
        return mTables;
    }

    public void setTables(String inTables) {
        mTables = inTables;
    }

    /// Adds to the WHERE clause; fragments are concatenated, so supply the
    /// AND or OR yourself. The whole is parenthesised and ANDed with the
    /// selection a query passes.
    public void appendWhere(CharSequence inWhere) {
        if (mWhereClause == null) {
            mWhereClause = new StringBuilder(inWhere.length() + 16);
        }
        mWhereClause.append(inWhere);
    }

    public void appendWhereEscapeString(String inWhere) {
        if (mWhereClause == null) {
            mWhereClause = new StringBuilder(inWhere.length() + 16);
        }
        DatabaseUtils.appendEscapedSQLString(mWhereClause, inWhere);
    }

    public void appendWhereStandalone(CharSequence inWhere) {
        if (mWhereClause == null) {
            mWhereClause = new StringBuilder(inWhere.length() + 16);
        }
        if (mWhereClause.length() > 0) {
            mWhereClause.append(" AND ");
        }
        mWhereClause.append('(').append(inWhere).append(')');
    }

    public void setProjectionMap(Map<String, String> columnMap) {
        mProjectionMap = columnMap;
    }

    public Map<String, String> getProjectionMap() {
        return mProjectionMap;
    }

    public void setCursorFactory(SQLiteDatabase.CursorFactory factory) {
        mFactory = factory;
    }

    public SQLiteDatabase.CursorFactory getCursorFactory() {
        return mFactory;
    }

    public void setStrict(boolean flag) {
        mStrict = flag;
    }

    public boolean isStrict() {
        return mStrict;
    }

    public static String buildQueryString(boolean distinct, String tables, String[] columns, String where,
                                          String groupBy, String having, String orderBy, String limit) {
        if (isEmpty(groupBy) && !isEmpty(having)) {
            throw new IllegalArgumentException("HAVING clauses are only permitted when using a groupBy clause");
        }
        if (!isEmpty(limit) && !isValidLimit(limit)) {
            throw new IllegalArgumentException("invalid LIMIT clauses:" + limit);
        }
        StringBuilder query = new StringBuilder(120);
        query.append("SELECT ");
        if (distinct) {
            query.append("DISTINCT ");
        }
        if (columns != null && columns.length != 0) {
            appendColumns(query, columns);
        } else {
            query.append("* ");
        }
        query.append("FROM ");
        query.append(tables);
        appendClause(query, " WHERE ", where);
        appendClause(query, " GROUP BY ", groupBy);
        appendClause(query, " HAVING ", having);
        appendClause(query, " ORDER BY ", orderBy);
        appendClause(query, " LIMIT ", limit);
        return query.toString();
    }

    /// Android accepts `n` or `n,m`, digits and whitespace only.
    private static boolean isValidLimit(String limit) {
        int n = limit.length();
        int i = 0;
        for (int part = 0; part < 2; part++) {
            while (i < n && limit.charAt(i) == ' ') {
                i++;
            }
            int start = i;
            while (i < n && limit.charAt(i) >= '0' && limit.charAt(i) <= '9') {
                i++;
            }
            if (i == start) {
                return false;
            }
            while (i < n && limit.charAt(i) == ' ') {
                i++;
            }
            if (i == n) {
                return true;
            }
            if (part == 0 && limit.charAt(i) == ',') {
                i++;
            } else {
                return false;
            }
        }
        return false;
    }

    private static void appendClause(StringBuilder s, String name, String clause) {
        if (!isEmpty(clause)) {
            s.append(name);
            s.append(clause);
        }
    }

    /// Appends the non-null columns separated by commas, with a trailing
    /// space.
    public static void appendColumns(StringBuilder s, String[] columns) {
        int n = columns.length;
        for (int i = 0; i < n; i++) {
            String column = columns[i];
            if (column != null) {
                if (i > 0) {
                    s.append(", ");
                }
                s.append(column);
            }
        }
        s.append(' ');
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs,
                        String groupBy, String having, String sortOrder) {
        return query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder, null, null);
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs,
                        String groupBy, String having, String sortOrder, String limit) {
        return query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder, limit, null);
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs,
                        String groupBy, String having, String sortOrder, String limit,
                        CancellationSignal cancellationSignal) {
        if (mTables == null) {
            return null;
        }
        String sql = buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
        return db.rawQueryWithFactory(mFactory, sql, selectionArgs, SQLiteDatabase.findEditTable(mTables),
                cancellationSignal);
    }

    public String buildQuery(String[] projectionIn, String selection, String groupBy, String having,
                             String sortOrder, String limit) {
        String[] projection = computeProjection(projectionIn);
        StringBuilder where = new StringBuilder();
        boolean hasBaseWhereClause = mWhereClause != null && mWhereClause.length() > 0;
        if (hasBaseWhereClause) {
            where.append('(');
            where.append(mWhereClause.toString());
            where.append(')');
        }
        if (selection != null && selection.length() > 0) {
            if (hasBaseWhereClause) {
                where.append(" AND ");
            }
            where.append('(');
            where.append(selection);
            where.append(')');
        }
        return buildQueryString(mDistinct, mTables, projection, where.toString(), groupBy, having, sortOrder, limit);
    }

    @Deprecated
    public String buildQuery(String[] projectionIn, String selection, String[] selectionArgs, String groupBy,
                             String having, String sortOrder, String limit) {
        return buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
    }

    public String buildUnionSubQuery(String typeDiscriminatorColumn, String[] unionColumns,
                                     java.util.Set<String> columnsPresentInTable, int computedColumnsOffset,
                                     String typeDiscriminatorValue, String selection, String groupBy, String having) {
        int unionColumnsCount = unionColumns.length;
        String[] projectionIn = new String[unionColumnsCount];
        for (int i = 0; i < unionColumnsCount; i++) {
            String unionColumn = unionColumns[i];
            if (unionColumn.equals(typeDiscriminatorColumn)) {
                projectionIn[i] = "'" + typeDiscriminatorValue + "' AS " + typeDiscriminatorColumn;
            } else if (i <= computedColumnsOffset || columnsPresentInTable.contains(unionColumn)) {
                projectionIn[i] = unionColumn;
            } else {
                projectionIn[i] = "NULL AS " + unionColumn;
            }
        }
        return buildQuery(projectionIn, selection, groupBy, having, null, null);
    }

    public String buildUnionQuery(String[] subQueries, String sortOrder, String limit) {
        StringBuilder query = new StringBuilder(128);
        int subQueryCount = subQueries.length;
        String unionOperator = mDistinct ? " UNION " : " UNION ALL ";
        for (int i = 0; i < subQueryCount; i++) {
            if (i > 0) {
                query.append(unionOperator);
            }
            query.append(subQueries[i]);
        }
        appendClause(query, " ORDER BY ", sortOrder);
        appendClause(query, " LIMIT ", limit);
        return query.toString();
    }

    private String[] computeProjection(String[] projectionIn) {
        if (projectionIn != null && projectionIn.length > 0) {
            if (mProjectionMap != null) {
                String[] projection = new String[projectionIn.length];
                int length = projectionIn.length;
                for (int i = 0; i < length; i++) {
                    String userColumn = projectionIn[i];
                    String column = mProjectionMap.get(userColumn);
                    if (column != null) {
                        projection[i] = column;
                        continue;
                    }
                    if (!mStrict && (containsAs(userColumn) || userColumn.equals("*"))) {
                        projection[i] = userColumn;
                        continue;
                    }
                    throw new IllegalArgumentException("Invalid column " + projectionIn[i]);
                }
                return projection;
            }
            return projectionIn;
        } else if (mProjectionMap != null) {
            java.util.ArrayList<String> out = new java.util.ArrayList<String>();
            for (Map.Entry<String, String> entry : mProjectionMap.entrySet()) {
                if (entry.getKey().equals("_count")) {
                    continue;
                }
                out.add(entry.getValue());
            }
            return out.toArray(new String[out.size()]);
        }
        return null;
    }

    private static boolean containsAs(String column) {
        int n = column.length();
        for (int i = 0; i + 4 <= n; i++) {
            if (column.regionMatches(true, i, " AS ", 0, 4)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
}
