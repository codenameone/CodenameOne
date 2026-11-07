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
    private static final String ROW = "[Ljava/lang/Object;";
    private final OrmAnnotationProcessor.EntityClass entity;
    private final String entityType;
    private final String keyType;
    /// Whether the method method() last generated only reads.
    private boolean readOnly;
    /// `@Transactional` on the repository interface, or null.
    private AnnotationValues typeTransactional;
    /// `@Transactional` on the method being generated, or null.
    private AnnotationValues methodTransactional;

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
        generator.typeTransactional = repository.getClassAnnotation(BackendBeans.TRANSACTIONAL);
        StringBuilder methods = new StringBuilder();
        Set<String> custom = new HashSet<String>();
        for (MethodInfo method : repository.getMethods()) {
            if (method.isStatic() || method.isSynthetic()) continue;
            // A default method is the interface's own code and is inherited as it is;
            // it reaches the generated methods through `this`, as in Spring Data.
            if (!method.isAbstract()) continue;
            generator.methodTransactional = method.getAnnotation(BackendBeans.TRANSACTIONAL);
            try { methods.append(generator.method(method)); custom.add(method.getName()); }
            catch (IllegalArgumentException error) { throw invalid(method.getName() + ": " + error.getMessage()); }
        }
        String e = generator.entityType, k = generator.keyType;
        String list = "java.util.List<" + e + ">";
        // {name, return type, parameter type or "", body, read-only}. Every one,
        // like every declared method, becomes a private body and a public
        // wrapper that runs it REQUIRED (see wrap).
        String[][] defaults = {
            {"findById", e, k, "return session.find(" + e + ".class,p0);", "true"},
            {"save", e, e, "return session.merge(p0);", "false"},
            {"insert", "void", e, "session.persist(p0);", "false"},
            {"deleteById", "void", k, "requireTransaction(); " + e + " value=session.find(" + e + ".class,p0); if(value!=null) session.remove(value);", "false"},
            {"findAll", list, "", "return session.query(" + e + ".class).list();", "true"},
            {"count", "long", "", "return session.query(" + e + ".class).count();", "true"},
            {"existsById", "boolean", k, "return session.find(" + e + ".class,p0)!=null;", "true"},
            {"delete", "void", e, "requireTransaction(); if(p0==null) throw new IllegalArgumentException(\"entity is null\"); if(session.contains(p0)) { session.remove(p0); return; } " + e + " value=session.merge(p0); if(value!=null) session.remove(value);", "false"},
            {"deleteAll", "void", "", "requireTransaction(); for(" + e + " value : session.query(" + e + ".class).list()) session.remove(value);", "false"},
            {"deleteAll", "void", "Iterable<? extends " + e + ">", "requireTransaction(); if(p0==null) throw new IllegalArgumentException(\"entities is null\"); for(" + e + " value : p0) delete$cn1Tx(value);", "false"},
            {"saveAll", list, "Iterable<? extends " + e + ">", "if(p0==null) throw new IllegalArgumentException(\"entities is null\"); " + list + " out=new java.util.ArrayList<" + e + ">(); for(" + e + " value : p0) out.add(session.merge(value)); return out;", "false"},
            {"findAllById", list, "Iterable<" + k + ">", "if(p0==null) throw new IllegalArgumentException(\"ids is null\"); " + list + " out=new java.util.ArrayList<" + e + ">(); for(" + k + " id : p0) { " + e + " value=session.find(" + e + ".class,id); if(value!=null) out.add(value); } return out;", "true"},
            {"flush", "void", "", "session.flush();", "false"}
        };
        generator.methodTransactional = null;
        for (String[] method : defaults) {
            if (custom.contains(method[0])) continue;
            String[] params = method[2].length() == 0 ? new String[0] : new String[] {method[2]};
            methods.append(generator.wrap(method[1], method[0], params, method[3], Boolean.parseBoolean(method[4])));
        }
        int dot = repository.getBinaryName().lastIndexOf('.');
        String simple = repository.getBinaryName().substring(dot + 1) + "Cn1Repository";
        return (dot < 0 ? "" : "package " + repository.getBinaryName().substring(0, dot) + ";\n")
                + "@com.codename1.impl.orm.GeneratedRepository public final class " + simple + " implements " + repository.getBinaryName() + " {\n"
                + "public static void register() { com.codename1.impl.orm.RepositoryFactories.register(" + repository.getBinaryName() + ".class,new Factory()); }\n"
                + "private static final class Factory implements com.codename1.impl.orm.RepositoryFactories.Factory<" + repository.getBinaryName() + "> { public " + repository.getBinaryName() + " create(com.codename1.orm.session.Session session) { return new " + simple + "(session); } }\n"
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
        AnnotationValues modifyingValues = method.getAnnotation(PREFIX + "Modifying;");
        boolean modifying = modifyingValues != null;
        // Spring Data's default: the session is left as it is, so an entity the
        // caller read before the statement is still managed and a change made to it
        // afterwards is still written. clearAutomatically empties it after the
        // statement. flushAutomatically needs no code: executeUpdate always flushes
        // first (see Modifying.flushAutomatically).
        boolean clearAfter = modifying && modifyingValues.getBoolOrDefault("clearAutomatically", false);
        if (method.getSignature() != null && method.getSignature().startsWith("<")) throw invalid("Generic methods are unsupported");
        if (lock != null) {
            if (!name.equals("findById") || args.length != 1 || !args[0].getClassName().equals(keyType)
                    || !result.getClassName().equals(entityType) || query != null || modifying) {
                throw invalid("@Lock is supported only on an entity-returning findById(key) override");
            }
            String[] mode = (String[]) lock.get("value");
            // Not read-only: PostgreSQL refuses SELECT ... FOR UPDATE in a
            // read-only transaction.
            return wrap(entityType, name, types(args), "return session.find(" + entityType + ".class,p0,com.codename1.orm.session.LockMode." + mode[1] + ");", false);
        }
        if (name.equals("findById") || name.equals("save") || name.equals("insert") || name.equals("deleteById")
                || name.equals("findAll") || name.equals("count") || name.equals("existsById")
                || name.equals("delete") || name.equals("deleteAll") || name.equals("saveAll")
                || name.equals("findAllById") || name.equals("flush")) {
            throw invalid("Use the inherited CRUD method; only findById may be redeclared with @Lock");
        }
        boolean list = result.getClassName().equals("java.util.List");
        // Optional<T> is a single result that may be absent, as Spring Data
        // reads it: the query's first row wrapped, or Optional.empty().
        boolean optional = result.getClassName().equals("java.util.Optional");
        String scalar = result.getClassName();
        if (list || optional) {
            String container = list ? "List" : "Optional";
            String sig = method.getSignature();
            String marker = ")Ljava/util/" + container + "<";
            int pos = sig == null ? -1 : sig.indexOf(marker);
            if (pos < 0 || !sig.endsWith(">;")) throw invalid("Use " + container + "<ConcreteType>, not a raw " + container + " or wildcard");
            String item = sig.substring(pos + marker.length(), sig.length() - 2);
            // A row of several values -- `select a.x, count(a) ... group by a.x` --
            // is an Object[], as Spring Data returns it; checked below to be a
            // declared JPQL select.
            if (!item.equals(ROW) && (!item.startsWith("L") || item.indexOf('<') >= 0 || !item.endsWith(";"))) {
                throw invalid(container + " needs a concrete element class");
            }
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
                if (nativeSql && !nativeParameter(args[i])) {
                    throw invalid("Native query parameters must be scalar values or byte[] BLOBs: " + args[i].getClassName());
                }
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
                    if (suffix.equals("True") || suffix.equals("False")) {
                        OrmAnnotationProcessor.PersistedField property = property(field);
                        if (property.dialectKind != OrmAnnotationProcessor.KIND_BOOLEAN
                                || !domainType(property).equals("java.lang.Boolean")) {
                            throw invalid(suffix + " requires a Boolean property with Boolean storage: " + field);
                        }
                    }
                    where.append("e.").append(field).append(' ').append(operator);
                    if (!suffix.equals("IsNull") && !suffix.equals("IsNotNull") && !suffix.equals("True") && !suffix.equals("False")) {
                        if (names.size() >= values) throw invalid("Missing argument for " + field);
                        checkArgument(field, suffix, args[names.size()], method, names.size());
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
        boolean row = scalar.equals("java.lang.Object[]");
        if (row && (query == null || nativeSql)) throw invalid("Object[] rows come from a declared JPQL @Query select");
        boolean mutation = modifying || delete;
        if (mutation && (!scalar.equals("int") && !scalar.equals("void") || list || optional || page >= 0)) throw invalid("Modifying queries return int or void without pagination");
        if (optional && (count || exists)) throw invalid("countBy returns long; existsBy returns boolean");
        if (!mutation && result.getSort() != Type.OBJECT && !row && !count && !exists) throw invalid("Declared scalar selects must use boxed return types for SQL NULL");
        if (page >= 0 && limit > 0) throw invalid("Do not combine PageRequest with Top/First");
        String valueType = mutation ? "Object" : count || exists ? "Long" : scalar;
        if (nativeSql && !mutation && !scalar.equals("java.lang.String") && !scalar.equals("java.lang.Long")
                && !scalar.equals("java.lang.Integer") && !scalar.equals("java.lang.Double") && !scalar.equals("java.lang.Boolean")) {
            throw invalid("Native repository selects return String, Long, Integer, Double or Boolean (or List thereof)");
        }
        readOnly = !mutation;
        String create = "session." + (nativeSql ? "createNativeQuery" : "createQuery") + "(" + quote(statement) + "," + valueType + ".class)";
        String returned = list ? "java.util.List<" + scalar + ">" : optional ? "java.util.Optional<" + scalar + ">" : scalar;
        StringBuilder body = new StringBuilder();
        body.append("com.codename1.orm.session.JpqlQuery<").append(valueType).append("> q=").append(create).append(';');
        for (int i = 0; i < names.size(); i++) body.append("q.setParameter(").append(quote(names.get(i))).append(",p").append(i).append(");");
        if (page >= 0) body.append("q.offset(p").append(page).append(".getOffset()).limit(p").append(page).append(".getSize());");
        if (limit > 0) body.append("q.limit(").append(limit).append(");");
        body.append(mutation && clearAfter ? scalar.equals("void") ? "q.executeUpdate();session.clear();"
                        : "int changed=q.executeUpdate();session.clear();return changed;"
                : mutation ? scalar.equals("void") ? "q.executeUpdate();" : "return q.executeUpdate();"
                : count ? "return q.first().longValue();" : exists ? "return q.first().longValue()!=0;"
                : list ? "return q.list();" : optional ? "return java.util.Optional.ofNullable(q.first());"
                : "return q.first();");
        return wrap(returned, name, types(args), body.toString(), readOnly);
    }
    private void checkArgument(String field, String suffix, Type argument, MethodInfo method, int parameter) {
        OrmAnnotationProcessor.PersistedField property = property(field);
        String expected = domainType(property);
        if (suffix.equals("In")) {
            String element;
            if (argument.getSort() == Type.ARRAY) {
                Type component = Type.getType(argument.getDescriptor().substring(1));
                if (component.getSort() != Type.OBJECT && component.getSort() != Type.ARRAY) {
                    throw invalid("In requires an object array, not a primitive array: " + field);
                }
                element = component.getClassName();
            } else {
                String type = argument.getClassName();
                if (!type.equals("java.lang.Iterable") && !type.equals("java.util.List")
                        && !type.equals("java.util.Collection") && !type.equals("java.util.Set")) {
                    throw invalid("In requires an Iterable, Collection, List, Set or object array argument");
                }
                element = collectionElement(method, parameter, argument);
            }
            if (!expected.equals(element)) throw invalid("In elements for " + field + " must be " + expected + ", got " + element);
            return;
        }
        if (!expected.equals(boxed(argument.getClassName()))) throw invalid("Argument for " + field + " must be " + expected);
        if (suffix.equals("Like") && (!expected.equals("java.lang.String") || property.dialectKind != OrmAnnotationProcessor.KIND_TEXT)) {
            throw invalid("Like requires a String property with text storage");
        }
    }
    private static boolean nativeParameter(Type argument) {
        if (argument.getDescriptor().equals("[B")) return true;
        String type = boxed(argument.getClassName());
        return type.equals("java.lang.String") || type.equals("java.lang.Boolean")
                || type.equals("java.lang.Character") || type.equals("java.lang.Byte")
                || type.equals("java.lang.Short") || type.equals("java.lang.Integer")
                || type.equals("java.lang.Long") || type.equals("java.lang.Float")
                || type.equals("java.lang.Double") || type.equals("java.util.Date")
                || type.equals("java.math.BigInteger") || type.equals("java.math.BigDecimal");
    }
    private static String collectionElement(MethodInfo method, int parameter, Type argument) {
        final List<org.objectweb.asm.signature.SignatureWriter> parameters =
                new ArrayList<org.objectweb.asm.signature.SignatureWriter>();
        if (method.getSignature() != null) {
            new org.objectweb.asm.signature.SignatureReader(method.getSignature()).accept(
                    new org.objectweb.asm.signature.SignatureVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override public org.objectweb.asm.signature.SignatureVisitor visitParameterType() {
                            org.objectweb.asm.signature.SignatureWriter writer = new org.objectweb.asm.signature.SignatureWriter();
                            parameters.add(writer);
                            return writer;
                        }
                    });
        }
        String signature = parameter < parameters.size() ? parameters.get(parameter).toString() : "";
        String prefix = "L" + argument.getInternalName() + "<";
        if (signature.startsWith(prefix) && signature.endsWith(">;")) {
            String element = signature.substring(prefix.length(), signature.length() - 2);
            // Accept only a concrete class or array; raw, wildcard, variable and
            // nested generic element types cannot prove the mapped domain type.
            if (element.matches("(?:L[^;<]+;|\\[+(?:L[^;<]+;|[ZBCSIJFD]))")) {
                return Type.getType(element).getClassName();
            }
        }
        throw invalid("In collections require a concrete element type matching the property (no raw or wildcard collections)");
    }
    private OrmAnnotationProcessor.PersistedField property(String name) {
        for (OrmAnnotationProcessor.PersistedField property : entity.fields) {
            if (property.fieldName.equals(name)) return property;
        }
        throw invalid("Unknown scalar property " + name);
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
    /// Propagation's constants in ordinal order, as the backend numbers them.
    private static final List<String> PROPAGATIONS = java.util.Arrays.asList(
            "REQUIRED", "SUPPORTS", "MANDATORY", "REQUIRES_NEW", "NOT_SUPPORTED", "NEVER", "NESTED");
    private static String[] types(Type[] args) {
        String[] out = new String[args.length];
        for (int i = 0; i < args.length; i++) out[i] = args[i].getClassName();
        return out;
    }
    /// A public method that runs `body` REQUIRED -- joining the caller's
    /// transaction or opening its own, read-only for a finder -- as Spring Data
    /// runs every repository method. The body is a private `name$cn1Tx` beside
    /// it so that its own returns need no rewriting. On the client nothing is
    /// installed and the bracket costs two static calls; on the backend a
    /// repository bound to the injected session needs no @Transactional caller,
    /// which a UserDetailsService otherwise reported as bad credentials.
    private String wrap(String result, String name, String[] params, String body, boolean readOnly) {
        // @Transactional on the method, else on the interface, replaces the
        // default, as Spring Data honours it on a repository; its readOnly
        // stands as written, so a declared one can make a finder writable.
        AnnotationValues tx = methodTransactional != null ? methodTransactional : typeTransactional;
        int propagation = 0, timeout = -1;
        String isolation = "DEFAULT";
        if (tx != null) {
            readOnly = tx.getBoolOrDefault("readOnly", false);
            String[] p = (String[]) tx.get("propagation");
            if (p != null) propagation = PROPAGATIONS.indexOf(p[1]);
            String[] i = (String[]) tx.get("isolation");
            if (i != null) isolation = i[1];
            Object t = tx.get("timeout");
            if (t instanceof Integer) timeout = ((Integer) t).intValue();
            if (tx.get("rollbackFor") != null || tx.get("noRollbackFor") != null) {
                throw invalid("@Transactional rollbackFor/noRollbackFor is not supported on a repository; every exception rolls the call back");
            }
        }
        StringBuilder decl = new StringBuilder(), call = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) { decl.append(','); call.append(','); }
            decl.append(params[i]).append(" p").append(i);
            call.append('p').append(i);
        }
        boolean isVoid = result.equals("void");
        return "private " + result + " " + name + "$cn1Tx(" + decl + ") {" + body + "}\n"
                + "public " + result + " " + name + "(" + decl + ") { Object cn1Tx=com.codename1.impl.orm.RepositoryTransactions.begin(session,"
                + propagation + "," + readOnly + "," + timeout + "," + quote(isolation) + "); boolean cn1Ok=false; try { "
                + (isVoid ? name + "$cn1Tx(" + call + "); cn1Ok=true;" : result + " cn1Out=" + name + "$cn1Tx(" + call + "); cn1Ok=true; return cn1Out;")
                + " } finally { com.codename1.impl.orm.RepositoryTransactions.end(cn1Tx,cn1Ok); } }\n";
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
