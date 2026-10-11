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

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.MethodInfo;
import org.objectweb.asm.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Generates direct Session calls; no runtime proxies, reflection or scanning. */
final class RepositoryGenerator {
    static final String PREFIX = "Lcom/codename1/orm/repository/";
    static final String REPOSITORY = PREFIX + "Repository;";
    private static final String BASE = "com/codename1/orm/repository/CrudRepository";
    private static final String PAGE = "com.codename1.orm.repository.PageRequest";
    private final OrmAnnotationProcessor.EntityClass entity;
    private final String entityType;
    private final String keyType;

    private RepositoryGenerator(OrmAnnotationProcessor.EntityClass entity, String keyType) {
        this.entity = entity;
        this.entityType = entity.binaryName;
        this.keyType = keyType;
    }
    static String generate(AnnotatedClass repository, Map<String, OrmAnnotationProcessor.EntityClass> entities) {
        if (!repository.isInterface() || !repository.isPublic() || repository.getBinaryName().contains("$")) {
            throw invalid("@Repository requires a public top-level interface");
        }
        AnnotationValues ann = repository.getClassAnnotation(REPOSITORY);
        Type entityType = (Type) ann.get("entity"), key = (Type) ann.get("key");
        if (entityType == null || key == null || key.getSort() != Type.OBJECT) {
            throw invalid("@Repository needs explicit entity and boxed key class literals");
        }
        OrmAnnotationProcessor.EntityClass entity = entities.get(entityType.getClassName());
        if (entity == null) throw invalid("Repository entity is not a generated @Entity: " + entityType.getClassName());
        String expectedKey = entity.embeddedId != null ? entity.embedded.get(entity.embeddedId) : entity.idFields.size() != 1
                ? "com.codename1.orm.session.Identifier" : domainType(entity.idFields.get(0));
        if (!key.getClassName().equals(expectedKey)) throw invalid("Repository key must be " + expectedKey);
        String signature = "Ljava/lang/Object;L" + BASE + "<" + entityType.getDescriptor() + key.getDescriptor() + ">;";
        if (repository.getInterfaceInternalNames().size() != 1
                || !repository.getInterfaceInternalNames().get(0).equals(BASE)
                || !signature.equals(repository.getSignature())) {
            throw invalid("Extend CrudRepository<" + entityType.getClassName() + ", " + key.getClassName()
                    + "> directly; generic repositories and additional superinterfaces are unsupported");
        }
        RepositoryGenerator generator = new RepositoryGenerator(entity, key.getClassName());
        StringBuilder methods = new StringBuilder();
        Set<String> custom = new HashSet<String>();
        for (MethodInfo method : repository.getMethods()) {
            if (method.isStatic() || method.isSynthetic()) continue;
            if (!method.isAbstract()) throw invalid(method.getName() + ": default methods are unsupported");
            try { methods.append(generator.method(method)); custom.add(method.getName()); }
            catch (IllegalArgumentException error) { throw invalid(method.getName() + ": " + error.getMessage()); }
        }
        String e = generator.entityType, k = generator.keyType;
        String[][] defaults = {
            {"findById", "public " + e + " findById(" + k + " id) { return session.find(" + e + ".class,id); }"},
            {"save", "public " + e + " save(" + e + " entity) { return session.merge(entity); }"},
            {"insert", "public void insert(" + e + " entity) { session.persist(entity); }"},
            {"deleteById", "public void deleteById(" + k + " id) { requireTransaction(); " + e + " value=session.find(" + e + ".class,id); if(value!=null) session.remove(value); }"},
            {"findAll", "public java.util.List<" + e + "> findAll() { return session.query(" + e + ".class).list(); }"},
            {"count", "public long count() { return session.query(" + e + ".class).count(); }"},
            {"existsById", "public boolean existsById(" + k + " id) { return session.find(" + e + ".class,id)!=null; }"}
        };
        for (String[] method : defaults) if (!custom.contains(method[0])) methods.append(method[1]).append('\n');
        int dot = repository.getBinaryName().lastIndexOf('.');
        String simple = repository.getBinaryName().substring(dot + 1) + "Cn1Repository";
        return (dot < 0 ? "" : "package " + repository.getBinaryName().substring(0, dot) + ";\n")
                + "@com.codename1.backend.annotations.Generated public final class " + simple + " implements " + repository.getBinaryName() + " {\n"
                + "private final com.codename1.orm.session.Session session;\n"
                + "public " + simple + "(com.codename1.orm.session.Session session) { if(session==null) throw new IllegalArgumentException(\"session is null\"); this.session=session; "
                + " }\n"
                + "private void requireTransaction() { if(!session.isTransactionActive() || session.isRollbackOnly()) throw new com.codename1.orm.session.PersistenceException(\"Repository write requires an active usable transaction\"); }\n"
                + methods + "}\n";
    }
    private String method(MethodInfo method) {
        Type[] args = Type.getArgumentTypes(method.getDescriptor());
        Type result = Type.getReturnType(method.getDescriptor());
        String name = method.getName();
        AnnotationValues query = method.getAnnotation(PREFIX + "Query;"), lock = method.getAnnotation(PREFIX + "Lock;");
        boolean modifying = method.getAnnotation(PREFIX + "Modifying;") != null;
        if (method.getSignature() != null && method.getSignature().startsWith("<")) throw invalid("Generic methods are unsupported");
        if (lock != null) {
            if (!name.equals("findById") || args.length != 1 || !args[0].getClassName().equals(keyType)
                    || !result.getClassName().equals(entityType) || query != null || modifying) {
                throw invalid("@Lock is supported only on an entity-returning findById(key) override");
            }
            String[] mode = (String[]) lock.get("value");
            return declaration(method, args, entityType) + "return session.find(" + entityType + ".class,p0,com.codename1.orm.session.LockMode." + mode[1] + ");}\n";
        }
        if (name.equals("findById") || name.equals("save") || name.equals("insert") || name.equals("deleteById")
                || name.equals("findAll") || name.equals("count") || name.equals("existsById")) {
            throw invalid("Use the inherited CRUD method; only findById may be redeclared with @Lock");
        }
        boolean list = result.getClassName().equals("java.util.List");
        String scalar = result.getClassName();
        if (list) {
            String sig = method.getSignature();
            int pos = sig == null ? -1 : sig.indexOf(")Ljava/util/List<");
            if (pos < 0 || !sig.endsWith(">;")) throw invalid("Use List<ConcreteType>, not a raw List or wildcard");
            String item = sig.substring(pos + ")Ljava/util/List<".length(), sig.length() - 2);
            if (!item.startsWith("L") || item.indexOf('<') >= 0 || !item.endsWith(";")) throw invalid("List needs a concrete element class");
            scalar = Type.getType(item).getClassName();
        }
        int page = -1;
        int values = args.length;
        for (int i = 0; i < args.length; i++) if (args[i].getClassName().equals(PAGE)) {
            if (page >= 0 || i != args.length - 1 || !list || modifying) throw invalid("PageRequest must be the last parameter of a list select");
            page = i; values--;
        }
        List<String> names = new ArrayList<String>();
        String statement;
        boolean nativeSql = query != null && query.getBoolOrDefault("nativeSql", false);
        boolean count = false, exists = false, delete = false;
        int limit = -1;
        if (query != null) {
            statement = query.getString("value");
            if (statement == null || statement.trim().length() == 0) throw invalid("@Query must contain a statement");
            String upper = statement.trim().toUpperCase(java.util.Locale.ROOT);
            boolean mutation = upper.matches("(?s)^(UPDATE|DELETE)\\s.*");
            if (mutation != modifying) throw invalid("UPDATE/DELETE requires @Modifying; SELECT must not use @Modifying");
            for (int i = 0; i < values; i++) {
                AnnotationValues param = method.getParameterAnnotations().size() > i
                        ? method.getParameterAnnotations().get(i).get(PREFIX + "Param;") : null;
                String binding = param == null ? null : param.getString("value");
                if (binding == null || !binding.matches("[A-Za-z_][A-Za-z0-9_]*") || names.contains(binding)) {
                    throw invalid("Every declared query argument needs a unique @Param(\"name\")");
                }
                names.add(binding);
            }
            Set<String> used = parameters(statement);
            if (!used.equals(new HashSet<String>(names))) throw invalid("@Param names must match query parameters exactly: " + used);
        } else {
            count = name.startsWith("countBy"); exists = name.startsWith("existsBy"); delete = name.startsWith("deleteBy");
            String tail;
            if (count || exists || delete) tail = name.substring(name.indexOf("By") + 2);
            else if (name.startsWith("findBy")) tail = name.substring(6);
            else if (name.matches("find(First|Top)[0-9]*By.+")) {
                int by = name.indexOf("By");
                String digits = name.substring(name.startsWith("findFirst") ? 9 : 7, by);
                limit = digits.length() == 0 ? 1 : Integer.parseInt(digits);
                if (limit <= 0) throw invalid("Top/First limit must be positive");
                tail = name.substring(by + 2);
            } else throw invalid("Unsupported method name; use find/count/exists/deleteBy or @Query");
            if (modifying) throw invalid("Derived deletes do not need @Modifying");
            String ordering = "";
            int order = tail.indexOf("OrderBy");
            if (order >= 0) {
                if (count || exists || delete) throw invalid("Only find methods support OrderBy");
                ordering = order(tail.substring(order + 7)); tail = tail.substring(0, order);
            }
            if (tail.length() == 0) throw invalid("Derived query needs a field predicate");
            StringBuilder where = new StringBuilder();
            for (String or : tail.split("Or(?=[A-Z])", -1)) {
                if (where.length() > 0) where.append(" OR ");
                where.append('(');
                int begin = where.length();
                for (String term : or.split("And(?=[A-Z])", -1)) {
                    if (where.length() > begin) where.append(" AND ");
                    String suffix = "", operator = "=";
                    String[][] operations = {{"IsNotNull","IS NOT NULL"},{"IsNull","IS NULL"},{"GreaterThanEqual",">="},{"LessThanEqual","<="},
                        {"GreaterThan",">"},{"LessThan","<"},{"Not","<>"},{"Like","LIKE"},{"In","IN"},{"True","= TRUE"},{"False","= FALSE"}};
                    for (String[] operation : operations) if (term.endsWith(operation[0])) { suffix=operation[0];operator=operation[1];break; }
                    String field = field(term.substring(0, term.length() - suffix.length()));
                    where.append("e.").append(field).append(' ').append(operator);
                    if (!suffix.equals("IsNull") && !suffix.equals("IsNotNull") && !suffix.equals("True") && !suffix.equals("False")) {
                        if (names.size() >= values) throw invalid("Missing argument for " + field);
                        checkArgument(field, suffix, args[names.size()]);
                        String binding = "p" + names.size(); names.add(binding);
                        where.append(" :").append(binding);
                    }
                }
                where.append(')');
            }
            statement = (delete ? "DELETE FROM " : count || exists ? "SELECT COUNT(e) FROM " : "SELECT e FROM ")
                    + entityType + " e WHERE " + where + ordering;
            if (names.size() != values) throw invalid("Derived predicate expects " + names.size() + " value arguments, got " + values);
            if (delete && (page >= 0 || !scalar.equals("int") && !scalar.equals("void"))) throw invalid("Derived delete returns int or void and cannot be paged");
            if (count && !scalar.equals("long") || exists && !scalar.equals("boolean")) throw invalid("countBy returns long; existsBy returns boolean");
            if (!delete && !count && !exists && !scalar.equals(entityType)) throw invalid("Derived find returns entity or List<entity>");
        }
        boolean mutation = modifying || delete;
        if (mutation && (!scalar.equals("int") && !scalar.equals("void") || list || page >= 0)) throw invalid("Modifying queries return int or void without pagination");
        if (!mutation && result.getSort() != Type.OBJECT && !count && !exists) throw invalid("Declared scalar selects must use boxed return types for SQL NULL");
        if (page >= 0 && limit > 0) throw invalid("Do not combine PageRequest with Top/First");
        String valueType = mutation ? "Object" : count || exists ? "Long" : scalar;
        if (nativeSql && !mutation && !scalar.equals("java.lang.String") && !scalar.equals("java.lang.Long")
                && !scalar.equals("java.lang.Integer") && !scalar.equals("java.lang.Double") && !scalar.equals("java.lang.Boolean")) {
            throw invalid("Native repository selects return String, Long, Integer, Double or Boolean (or List thereof)");
        }
        String create = "session." + (nativeSql ? "createNativeQuery" : "createQuery") + "(" + quote(statement) + "," + valueType + ".class)";
        StringBuilder body = new StringBuilder(declaration(method, args, list ? "java.util.List<" + scalar + ">" : scalar));
        body.append("com.codename1.orm.session.JpqlQuery<").append(valueType).append("> q=").append(create).append(';');
        for (int i = 0; i < names.size(); i++) body.append("q.setParameter(").append(quote(names.get(i))).append(",p").append(i).append(");");
        if (page >= 0) body.append("q.offset(p").append(page).append(".getOffset()).limit(p").append(page).append(".getSize());");
        if (limit > 0) body.append("q.limit(").append(limit).append(");");
        body.append(mutation ? scalar.equals("void") ? "q.executeUpdate();" : "return q.executeUpdate();"
                : count ? "return q.first().longValue();" : exists ? "return q.first().longValue()!=0;"
                : list ? "return q.list();" : "return q.first();");
        return body.append("}\n").toString();
    }
    private void checkArgument(String field, String suffix, Type argument) {
        if (suffix.equals("In")) {
            String type = argument.getClassName();
            if (!(argument.getSort() == Type.ARRAY && argument.getElementType().getSort() == Type.OBJECT)
                    && !type.equals("java.lang.Iterable") && !type.equals("java.util.List")
                    && !type.equals("java.util.Collection") && !type.equals("java.util.Set")) {
                throw invalid("In requires an Iterable, Collection, List, Set or object array argument");
            }
            return;
        }
        for (OrmAnnotationProcessor.PersistedField property : entity.fields) if (property.fieldName.equals(field)) {
            String expected = domainType(property);
            if (!expected.equals(boxed(argument.getClassName()))) throw invalid("Argument for " + field + " must be " + expected);
            if (suffix.equals("Like") && !expected.equals("java.lang.String")) throw invalid("Like requires a String property");
            return;
        }
    }
    private String field(String token) {
        if (token.length() == 0) throw invalid("Missing property name");
        String path = Character.toLowerCase(token.charAt(0)) + token.substring(1);
        for (OrmAnnotationProcessor.PersistedField f : entity.fields) if (f.fieldName.equals(path) && f.relation == null) return path;
        throw invalid("Unknown scalar property " + path + " on " + entityType + "; use @Query for relationship paths");
    }
    private String order(String token) {
        StringBuilder sql = new StringBuilder(" ORDER BY ");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(.+?)(Asc|Desc)(?=[A-Z]|$)").matcher(token);
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end) throw invalid("Invalid OrderBy suffix");
            if (end > 0) sql.append(", ");
            sql.append("e.").append(field(matcher.group(1))).append(matcher.group(2).equals("Asc") ? " ASC" : " DESC");
            end = matcher.end();
        }
        if (end != token.length() || end == 0) throw invalid("OrderBy fields need explicit Asc or Desc");
        return sql.toString();
    }
    private static Set<String> parameters(String sql) {
        Set<String> names = new HashSet<String>();
        char quote = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) i++;
                    else quote = 0;
                }
            } else if (c == '\'' || c == '"') quote = c;
            else if (c == ':' && i + 1 < sql.length() && sql.charAt(i + 1) == ':') i++;
            else if (c == ':') {
                int start = ++i;
                while (i < sql.length() && Character.isJavaIdentifierPart(sql.charAt(i))) i++;
                if (start == i) throw invalid("Expected parameter name after colon");
                names.add(sql.substring(start, i--));
            }
        }
        return names;
    }
    private static String declaration(MethodInfo method, Type[] args, String result) {
        StringBuilder text = new StringBuilder("public ").append(result).append(' ').append(method.getName()).append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) text.append(',');
            text.append(args[i].getClassName()).append(" p").append(i);
        }
        return text.append(") {").toString();
    }
    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    private static String domainType(OrmAnnotationProcessor.PersistedField property) {
        return boxed(property.domainType != null ? property.domainType
                : property.kind.kind == PropertyTypeKind.Kind.PROPERTY ? property.kind.elementBinaryName : property.javaType);
    }
    private static String boxed(String type) {
        String[] primitive={"long","int","short","byte","boolean","double","float","char"};
        String[] boxed={"Long","Integer","Short","Byte","Boolean","Double","Float","Character"};
        for(int i=0;i<primitive.length;i++) if(primitive[i].equals(type)) return "java.lang."+boxed[i];
        return type;
    }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
