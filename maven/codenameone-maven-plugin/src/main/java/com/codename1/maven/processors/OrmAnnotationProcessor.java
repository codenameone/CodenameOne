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

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Build-time `@Entity` processor. For every entity class it generates one
/// `XxxDao` and registers it with `EntityManager` through a generated
/// `DaosIndex`. Generated daos issue prepared SQL through
/// `com.codename1.db.Database` and read columns through `Row` / `Cursor` --
/// the same surface `SQLMap` uses internally, but without runtime
/// `putClientProperty` plumbing.
public final class OrmAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String ENTITY_DESC = "Lcom/codename1/annotations/Entity;";
    public static final String ID_DESC = "Lcom/codename1/annotations/Id;";
    public static final String COLUMN_DESC = "Lcom/codename1/annotations/Column;";
    public static final String DB_TRANSIENT_DESC = "Lcom/codename1/annotations/DbTransient;";

    static final String GENERATED_PACKAGE = "com.codename1.orm.generated";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(ENTITY_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    private final TreeMap<String, EntityClass> accepted = new TreeMap<String, EntityClass>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.isSynthetic()) return;
        AnnotationValues entityAnn = cls.getClassAnnotation(ENTITY_DESC);
        if (entityAnn == null) return;
        if (cls.isAbstract() || cls.isInterface()) {
            ctx.error(cls, "@Entity requires a concrete class; " + cls.getBinaryName()
                    + " is abstract or an interface");
            return;
        }
        if (!hasPublicNoArgConstructor(cls)) {
            ctx.error(cls, "@Entity class " + cls.getBinaryName()
                    + " must declare a public no-arg constructor");
            return;
        }

        EntityClass ec = new EntityClass();
        ec.binaryName = cls.getBinaryName();
        ec.simpleName = simpleName(cls.getBinaryName());
        ec.daoSimpleName = ec.simpleName + "Dao";
        ec.daoBinaryName = GENERATED_PACKAGE + "." + ec.daoSimpleName;
        String table = entityAnn.getString("table");
        ec.tableName = (table == null || table.length() == 0) ? ec.simpleName : table;

        for (FieldInfo f : cls.getFields()) {
            if (f.isStatic()) continue;
            if (f.getName().startsWith("this$")) continue;
            if (f.getAnnotation(DB_TRANSIENT_DESC) != null) continue;
            if (!f.isPublic()) continue; // accessor-style entities are v2
            PersistedField pf = new PersistedField();
            pf.fieldName = f.getName();
            pf.kind = PropertyTypeKind.of(f);
            if (pf.kind.kind == PropertyTypeKind.Kind.REFERENCE
                    || pf.kind.kind == PropertyTypeKind.Kind.LIST
                    || pf.kind.kind == PropertyTypeKind.Kind.LIST_PROPERTY) {
                // Relationships are out of scope for v1; flag once and move on.
                ctx.error(cls, "@Entity field " + ec.binaryName + "." + f.getName()
                        + " maps to a nested object or list; relationships are not yet "
                        + "supported. Use @DbTransient and persist the foreign key manually.");
                continue;
            }
            if (pf.kind.kind == PropertyTypeKind.Kind.UNSUPPORTED) {
                ctx.error(cls, "@Entity field " + ec.binaryName + "." + f.getName()
                        + " has an unsupported type (descriptor " + f.getDescriptor() + ")");
                continue;
            }
            AnnotationValues col = f.getAnnotation(COLUMN_DESC);
            String colName = null;
            String colType = null;
            boolean nullable = true;
            if (col != null) {
                colName = col.getString("name");
                colType = col.getString("type");
                nullable = col.getBoolOrDefault("nullable", true);
            }
            pf.columnName = (colName == null || colName.length() == 0) ? pf.fieldName : colName;
            pf.sqlType = (colType == null || colType.length() == 0) ? defaultSqlType(pf.kind) : colType;
            pf.nullable = nullable;

            AnnotationValues idAnn = f.getAnnotation(ID_DESC);
            if (idAnn != null) {
                pf.isId = true;
                pf.autoIncrement = idAnn.getBoolOrDefault("autoIncrement", true);
                if (ec.idField != null) {
                    ctx.error(cls, "@Entity " + ec.binaryName
                            + " has more than one @Id field");
                    continue;
                }
                ec.idField = pf;
            }
            ec.fields.add(pf);
        }

        if (ec.idField == null) {
            ctx.error(cls, "@Entity " + ec.binaryName + " requires exactly one @Id field");
            return;
        }
        accepted.put(ec.binaryName, ec);
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) return;
        if (accepted.isEmpty()) return;

        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (EntityClass ec : accepted.values()) {
            sources.put(ec.daoBinaryName, generateDaoSource(ec));
        }
        sources.put(GENERATED_PACKAGE + ".DaosIndex", generateIndexSource(accepted.values()));
        try {
            java.util.List<java.io.File> cp = new java.util.ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated dao sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size()
                + " @Entity dao(s) under " + GENERATED_PACKAGE);
    }

    // ---------------------------------------------------------------
    // Source generation
    // ---------------------------------------------------------------

    private static String generateDaoSource(EntityClass ec) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("package ").append(GENERATED_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(ec.daoSimpleName)
                .append(" implements com.codename1.orm.Dao<").append(ec.binaryName).append("> {\n\n");

        sb.append("    private com.codename1.db.Database db;\n\n");

        sb.append("    public Class<").append(ec.binaryName).append("> type() {\n");
        sb.append("        return ").append(ec.binaryName).append(".class;\n");
        sb.append("    }\n\n");

        sb.append("    public String tableName() {\n");
        sb.append("        return \"").append(escape(ec.tableName)).append("\";\n");
        sb.append("    }\n\n");

        sb.append("    public void attach(com.codename1.db.Database db) {\n");
        sb.append("        this.db = db;\n");
        sb.append("    }\n\n");

        // createTable
        sb.append("    public void createTable() throws java.io.IOException {\n");
        sb.append("        db.execute(\"CREATE TABLE IF NOT EXISTS ").append(escape(ec.tableName)).append(" (")
                .append(buildCreateColumnsSql(ec)).append(")\");\n");
        sb.append("    }\n\n");

        sb.append("    public void dropTable() throws java.io.IOException {\n");
        sb.append("        db.execute(\"DROP TABLE IF EXISTS ").append(escape(ec.tableName)).append("\");\n");
        sb.append("    }\n\n");

        // insert
        sb.append("    public void insert(").append(ec.binaryName).append(" e) throws java.io.IOException {\n");
        List<PersistedField> insertCols = new ArrayList<PersistedField>();
        for (PersistedField f : ec.fields) {
            // Skip auto-increment id column so SQLite assigns the key.
            if (f.isId && f.autoIncrement) continue;
            insertCols.add(f);
        }
        StringBuilder cols = new StringBuilder();
        StringBuilder qmarks = new StringBuilder();
        for (int i = 0; i < insertCols.size(); i++) {
            if (i > 0) { cols.append(", "); qmarks.append(", "); }
            cols.append(insertCols.get(i).columnName);
            qmarks.append("?");
        }
        sb.append("        Object[] _p = new Object[").append(insertCols.size()).append("];\n");
        for (int i = 0; i < insertCols.size(); i++) {
            sb.append("        _p[").append(i).append("] = ");
            emitFieldRead(sb, insertCols.get(i), "e");
            sb.append(";\n");
        }
        sb.append("        db.execute(\"INSERT INTO ").append(escape(ec.tableName))
                .append(" (").append(escape(cols.toString())).append(") VALUES (")
                .append(qmarks).append(")\", _p);\n");
        // Auto-id back-fill.
        if (ec.idField.autoIncrement) {
            sb.append("        com.codename1.db.Cursor _c = db.executeQuery(\"SELECT last_insert_rowid()\");\n");
            sb.append("        try {\n");
            sb.append("            if (_c.next()) {\n");
            sb.append("                long _id = _c.getRow().getLong(0);\n");
            emitIdAssign(sb, ec.idField, "e", "_id");
            sb.append("            }\n");
            sb.append("        } finally { _c.close(); }\n");
        }
        sb.append("    }\n\n");

        // update
        sb.append("    public void update(").append(ec.binaryName).append(" e) throws java.io.IOException {\n");
        List<PersistedField> updateCols = new ArrayList<PersistedField>();
        for (PersistedField f : ec.fields) {
            if (f.isId) continue;
            updateCols.add(f);
        }
        StringBuilder setSql = new StringBuilder();
        for (int i = 0; i < updateCols.size(); i++) {
            if (i > 0) setSql.append(", ");
            setSql.append(updateCols.get(i).columnName).append(" = ?");
        }
        sb.append("        Object[] _p = new Object[").append(updateCols.size() + 1).append("];\n");
        for (int i = 0; i < updateCols.size(); i++) {
            sb.append("        _p[").append(i).append("] = ");
            emitFieldRead(sb, updateCols.get(i), "e");
            sb.append(";\n");
        }
        sb.append("        _p[").append(updateCols.size()).append("] = ");
        emitFieldRead(sb, ec.idField, "e");
        sb.append(";\n");
        sb.append("        db.execute(\"UPDATE ").append(escape(ec.tableName)).append(" SET ")
                .append(escape(setSql.toString())).append(" WHERE ").append(ec.idField.columnName)
                .append(" = ?\", _p);\n");
        sb.append("    }\n\n");

        // delete
        sb.append("    public void delete(").append(ec.binaryName).append(" e) throws java.io.IOException {\n");
        sb.append("        db.execute(\"DELETE FROM ").append(escape(ec.tableName)).append(" WHERE ")
                .append(ec.idField.columnName).append(" = ?\", new Object[]{ ");
        emitFieldRead(sb, ec.idField, "e");
        sb.append(" });\n");
        sb.append("    }\n\n");

        // findById
        sb.append("    public ").append(ec.binaryName).append(" findById(Object id) throws java.io.IOException {\n");
        sb.append("        com.codename1.db.Cursor _c = db.executeQuery(\"SELECT * FROM ")
                .append(escape(ec.tableName)).append(" WHERE ").append(ec.idField.columnName)
                .append(" = ?\", new Object[]{ id });\n");
        sb.append("        try {\n");
        sb.append("            if (_c.next()) return readRow(_c);\n");
        sb.append("            return null;\n");
        sb.append("        } finally { _c.close(); }\n");
        sb.append("    }\n\n");

        // findAll
        sb.append("    public java.util.List<").append(ec.binaryName).append("> findAll() throws java.io.IOException {\n");
        sb.append("        com.codename1.db.Cursor _c = db.executeQuery(\"SELECT * FROM ")
                .append(escape(ec.tableName)).append("\");\n");
        sb.append("        java.util.ArrayList<").append(ec.binaryName).append("> _out = new java.util.ArrayList<").append(ec.binaryName).append(">();\n");
        sb.append("        try {\n");
        sb.append("            while (_c.next()) _out.add(readRow(_c));\n");
        sb.append("        } finally { _c.close(); }\n");
        sb.append("        return _out;\n");
        sb.append("    }\n\n");

        // find(where, params)
        sb.append("    public java.util.List<").append(ec.binaryName).append("> find(String where, Object... params) throws java.io.IOException {\n");
        sb.append("        String _sql = \"SELECT * FROM ").append(escape(ec.tableName)).append("\";\n");
        sb.append("        if (where != null && where.length() > 0) _sql = _sql + \" WHERE \" + where;\n");
        sb.append("        com.codename1.db.Cursor _c = db.executeQuery(_sql, params);\n");
        sb.append("        java.util.ArrayList<").append(ec.binaryName).append("> _out = new java.util.ArrayList<").append(ec.binaryName).append(">();\n");
        sb.append("        try {\n");
        sb.append("            while (_c.next()) _out.add(readRow(_c));\n");
        sb.append("        } finally { _c.close(); }\n");
        sb.append("        return _out;\n");
        sb.append("    }\n\n");

        // readRow helper
        sb.append("    private ").append(ec.binaryName).append(" readRow(com.codename1.db.Cursor _c) throws java.io.IOException {\n");
        sb.append("        ").append(ec.binaryName).append(" e = new ").append(ec.binaryName).append("();\n");
        sb.append("        com.codename1.db.Row _r = _c.getRow();\n");
        for (PersistedField f : ec.fields) {
            sb.append("        try {\n");
            sb.append("            int _idx = _c.getColumnIndex(\"").append(escape(f.columnName)).append("\");\n");
            sb.append("            if (_idx >= 0) {\n");
            emitFieldWrite(sb, f, "e", "_r", "_idx");
            sb.append("            }\n");
            sb.append("        } catch (java.io.IOException _ex) { /* column missing -- skip */ }\n");
        }
        sb.append("        return e;\n");
        sb.append("    }\n");

        sb.append("}\n");
        return sb.toString();
    }

    private static String generateIndexSource(Iterable<EntityClass> classes) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(GENERATED_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("public final class DaosIndex {\n");
        sb.append("    public DaosIndex() {\n");
        for (EntityClass ec : classes) {
            sb.append("        com.codename1.orm.EntityManager.registerDao(new ").append(ec.daoSimpleName).append("());\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildCreateColumnsSql(EntityClass ec) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ec.fields.size(); i++) {
            PersistedField f = ec.fields.get(i);
            if (i > 0) sb.append(", ");
            sb.append(f.columnName).append(' ');
            if (f.isId) {
                if (f.autoIncrement) {
                    sb.append("INTEGER PRIMARY KEY AUTOINCREMENT");
                } else {
                    sb.append(f.sqlType).append(" PRIMARY KEY");
                }
            } else {
                sb.append(f.sqlType);
                if (!f.nullable) sb.append(" NOT NULL");
            }
        }
        return sb.toString();
    }

    private static String defaultSqlType(PropertyTypeKind k) {
        if (k.kind == PropertyTypeKind.Kind.PROPERTY) {
            return defaultSqlTypeForBinary(k.elementBinaryName);
        }
        switch (k.kind) {
            case INT: case LONG: case SHORT: case BYTE: case BOOLEAN: case DATE:
                return "INTEGER";
            case DOUBLE: case FLOAT:
                return "REAL";
            case BYTE_ARRAY:
                return "BLOB";
            case CHAR: case STRING:
            default:
                return "TEXT";
        }
    }

    private static String defaultSqlTypeForBinary(String binary) {
        if ("java.lang.String".equals(binary)) return "TEXT";
        if ("java.lang.Integer".equals(binary) || "java.lang.Long".equals(binary)
                || "java.lang.Short".equals(binary) || "java.lang.Byte".equals(binary)
                || "java.lang.Boolean".equals(binary) || "java.util.Date".equals(binary)) {
            return "INTEGER";
        }
        if ("java.lang.Double".equals(binary) || "java.lang.Float".equals(binary)) {
            return "REAL";
        }
        return "TEXT";
    }

    private static void emitFieldRead(StringBuilder sb, PersistedField f, String inst) {
        switch (f.kind.kind) {
            case STRING: case BYTE_ARRAY:
                // Strings go through Database#execute(String, Object...) as
                // String params; byte[] is passed through unchanged for the
                // platforms that support blob binding (the others raise the
                // documented Database "Blobs aren't supported" error).
                sb.append(inst).append('.').append(f.fieldName);
                return;
            case INT: case LONG: case SHORT: case BYTE:
                sb.append("Long.valueOf(").append(inst).append('.').append(f.fieldName).append(")");
                return;
            case CHAR:
                sb.append("String.valueOf(").append(inst).append('.').append(f.fieldName).append(")");
                return;
            case DOUBLE: case FLOAT:
                sb.append("Double.valueOf(").append(inst).append('.').append(f.fieldName).append(")");
                return;
            case BOOLEAN:
                sb.append("Long.valueOf(").append(inst).append('.').append(f.fieldName).append(" ? 1L : 0L)");
                return;
            case DATE:
                sb.append(inst).append('.').append(f.fieldName).append(" == null ? null : Long.valueOf(")
                        .append(inst).append('.').append(f.fieldName).append(".getTime())");
                return;
            case PROPERTY:
                emitPropertyRead(sb, f, inst);
                return;
            default:
                sb.append("null");
        }
    }

    private static void emitPropertyRead(StringBuilder sb, PersistedField f, String inst) {
        String elem = f.kind.elementBinaryName;
        if ("java.lang.String".equals(elem)) {
            sb.append(inst).append('.').append(f.fieldName).append(".get()");
        } else if ("java.lang.Integer".equals(elem) || "java.lang.Long".equals(elem)
                || "java.lang.Short".equals(elem) || "java.lang.Byte".equals(elem)) {
            sb.append(inst).append('.').append(f.fieldName).append(".get() == null ? null : Long.valueOf(((Number) ")
                    .append(inst).append('.').append(f.fieldName).append(".get()).longValue())");
        } else if ("java.lang.Double".equals(elem) || "java.lang.Float".equals(elem)) {
            sb.append(inst).append('.').append(f.fieldName).append(".get() == null ? null : Double.valueOf(((Number) ")
                    .append(inst).append('.').append(f.fieldName).append(".get()).doubleValue())");
        } else if ("java.lang.Boolean".equals(elem)) {
            sb.append(inst).append('.').append(f.fieldName).append(".get() == null ? null : Long.valueOf(Boolean.TRUE.equals(")
                    .append(inst).append('.').append(f.fieldName).append(".get()) ? 1L : 0L)");
        } else if ("java.util.Date".equals(elem)) {
            sb.append(inst).append('.').append(f.fieldName).append(".get() == null ? null : Long.valueOf(((java.util.Date) ")
                    .append(inst).append('.').append(f.fieldName).append(".get()).getTime())");
        } else {
            sb.append(inst).append('.').append(f.fieldName).append(".get() == null ? null : String.valueOf(")
                    .append(inst).append('.').append(f.fieldName).append(".get())");
        }
    }

    private static void emitFieldWrite(StringBuilder sb, PersistedField f, String inst,
                                       String row, String idx) {
        switch (f.kind.kind) {
            case STRING:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = ").append(row).append(".getString(").append(idx).append(");\n");
                return;
            case INT:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = ").append(row).append(".getInteger(").append(idx).append(");\n");
                return;
            case LONG:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = ").append(row).append(".getLong(").append(idx).append(");\n");
                return;
            case SHORT:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = ").append(row).append(".getShort(").append(idx).append(");\n");
                return;
            case BYTE:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = (byte) ").append(row).append(".getInteger(").append(idx).append(");\n");
                return;
            case CHAR:
                sb.append("                String _s = ").append(row).append(".getString(").append(idx).append(");\n");
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = (_s == null || _s.length() == 0) ? '\\0' : _s.charAt(0);\n");
                return;
            case DOUBLE:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = ").append(row).append(".getDouble(").append(idx).append(");\n");
                return;
            case FLOAT:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = ").append(row).append(".getFloat(").append(idx).append(");\n");
                return;
            case BOOLEAN:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = ").append(row).append(".getInteger(").append(idx).append(") != 0;\n");
                return;
            case DATE:
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = new java.util.Date(").append(row).append(".getLong(").append(idx).append("));\n");
                return;
            case BYTE_ARRAY:
                // Most cn1 ports do not support getBlob universally; fall back
                // to base64 over getString -- the insert path uses execute()
                // which throws on byte[] anyway. Future enhancement.
                sb.append("                String _b64 = ").append(row).append(".getString(").append(idx).append(");\n");
                sb.append("                ").append(inst).append('.').append(f.fieldName)
                        .append(" = (_b64 == null) ? null : com.codename1.util.Base64.decode(_b64.getBytes());\n");
                return;
            case PROPERTY:
                emitPropertyWrite(sb, f, inst, row, idx);
                return;
            default:
                return;
        }
    }

    private static void emitPropertyWrite(StringBuilder sb, PersistedField f, String inst,
                                          String row, String idx) {
        String elem = f.kind.elementBinaryName;
        if ("java.lang.String".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(")
                    .append(row).append(".getString(").append(idx).append("));\n");
        } else if ("java.lang.Integer".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(Integer.valueOf(")
                    .append(row).append(".getInteger(").append(idx).append(")));\n");
        } else if ("java.lang.Long".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(Long.valueOf(")
                    .append(row).append(".getLong(").append(idx).append(")));\n");
        } else if ("java.lang.Short".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(Short.valueOf(")
                    .append(row).append(".getShort(").append(idx).append(")));\n");
        } else if ("java.lang.Byte".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(Byte.valueOf((byte) ")
                    .append(row).append(".getInteger(").append(idx).append(")));\n");
        } else if ("java.lang.Double".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(Double.valueOf(")
                    .append(row).append(".getDouble(").append(idx).append(")));\n");
        } else if ("java.lang.Float".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(Float.valueOf(")
                    .append(row).append(".getFloat(").append(idx).append(")));\n");
        } else if ("java.lang.Boolean".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(Boolean.valueOf(")
                    .append(row).append(".getInteger(").append(idx).append(") != 0));\n");
        } else if ("java.util.Date".equals(elem)) {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set(new java.util.Date(")
                    .append(row).append(".getLong(").append(idx).append(")));\n");
        } else {
            sb.append("                ").append(inst).append('.').append(f.fieldName).append(".set((")
                    .append(elem).append(") ").append(row).append(".getString(").append(idx).append("));\n");
        }
    }

    private static void emitIdAssign(StringBuilder sb, PersistedField id, String inst, String idVar) {
        switch (id.kind.kind) {
            case LONG:
                sb.append("                ").append(inst).append('.').append(id.fieldName)
                        .append(" = ").append(idVar).append(";\n");
                return;
            case INT:
                sb.append("                ").append(inst).append('.').append(id.fieldName)
                        .append(" = (int) ").append(idVar).append(";\n");
                return;
            case SHORT:
                sb.append("                ").append(inst).append('.').append(id.fieldName)
                        .append(" = (short) ").append(idVar).append(";\n");
                return;
            case STRING:
                sb.append("                ").append(inst).append('.').append(id.fieldName)
                        .append(" = String.valueOf(").append(idVar).append(");\n");
                return;
            case PROPERTY:
                String elem = id.kind.elementBinaryName;
                if ("java.lang.Long".equals(elem)) {
                    sb.append("                ").append(inst).append('.').append(id.fieldName)
                            .append(".set(Long.valueOf(").append(idVar).append("));\n");
                } else if ("java.lang.Integer".equals(elem)) {
                    sb.append("                ").append(inst).append('.').append(id.fieldName)
                            .append(".set(Integer.valueOf((int) ").append(idVar).append("));\n");
                } else if ("java.lang.String".equals(elem)) {
                    sb.append("                ").append(inst).append('.').append(id.fieldName)
                            .append(".set(String.valueOf(").append(idVar).append("));\n");
                }
                return;
            default:
                return;
        }
    }

    private static boolean hasPublicNoArgConstructor(AnnotatedClass cls) {
        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() && m.isPublic() && "()V".equals(m.getDescriptor())) {
                return true;
            }
        }
        return false;
    }

    private static String simpleName(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? binary : binary.substring(dot + 1);
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.toString();
    }

    // ---------------------------------------------------------------
    // Accumulator types
    // ---------------------------------------------------------------

    static final class EntityClass {
        String binaryName;
        String simpleName;
        String daoBinaryName;
        String daoSimpleName;
        String tableName;
        PersistedField idField;
        final List<PersistedField> fields = new ArrayList<PersistedField>();
    }

    static final class PersistedField {
        String fieldName;
        String columnName;
        String sqlType;
        boolean nullable;
        boolean isId;
        boolean autoIncrement;
        PropertyTypeKind kind;
    }
}
