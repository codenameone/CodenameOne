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
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Build-time `@Entity` processor. For every entity class it generates one
/// `XxxCn1Dao` and registers it through a generated `cn1app.DaoBootstrap`.
///
/// It has TWO flavours, because the same annotations describe the same entity
/// on both sides of an application and the two sides have different databases
/// under them:
///
/// - a module compiled against the Codename One core gets a dao over
///   `com.codename1.db.Database`, reading columns through `Row` / `Cursor` --
///   the same surface `SQLMap` uses internally, but without runtime
///   `putClientProperty` plumbing;
/// - a module compiled against the server-side backend runtime gets a
///   `com.codename1.backend.orm.EntityDefinition`, from which the runtime
///   builds statements for whichever of SQLite, PostgreSQL or MySQL the
///   connection turns out to be.
///
/// The flavour is detected from the compile classpath and can be forced with
/// `-Dcn1.backendOrm=true|false`. The entity source is identical either way,
/// which is the point: one class, stored in the app's SQLite file and in the
/// server's PostgreSQL.
public final class OrmAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String ENTITY_DESC = "Lcom/codename1/annotations/Entity;";
    public static final String ID_DESC = "Lcom/codename1/annotations/Id;";
    public static final String COLUMN_DESC = "Lcom/codename1/annotations/Column;";
    public static final String DB_TRANSIENT_DESC = "Lcom/codename1/annotations/DbTransient;";
    /// The marker every generated source here carries, so that the class this
    /// processor wrote on a previous pass is not read as a name collision on
    /// the next one. See wouldReplaceAnExistingClass.
    private static final String GENERATED_DESC = "Lcom/codename1/backend/annotations/Generated;";

    static final String BOOTSTRAP_BINARY = "cn1app.DaoBootstrap";
    static final String BOOTSTRAP_SIMPLE = "DaoBootstrap";
    static final String BOOTSTRAP_PACKAGE = "cn1app";

    /// The server-side names, which are deliberately NOT the client's.
    ///
    /// An entity can live in a module both halves of an application depend on,
    /// and that module's own build generates the CLIENT dao into its jar. If the
    /// backend flavour reused the name, that jar and this module's output would
    /// each hold a class of the same name -- and the one javac happened to pick
    /// references `com.codename1.db.Database`, which the server runtime does not
    /// have. Different names mean both daos can sit on one classpath, which is
    /// exactly what a shared entity produces.
    static final String BACKEND_DAO_SUFFIX = "Cn1BackendDao";
    static final String BACKEND_BOOTSTRAP_BINARY = "cn1app.BackendDaoBootstrap";
    static final String BACKEND_BOOTSTRAP_SIMPLE = "BackendDaoBootstrap";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(ENTITY_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    private final TreeMap<String, EntityClass> accepted = new TreeMap<String, EntityClass>();

    /// Whether this module's daos go over the server-side backend runtime
    /// rather than over the Codename One core. Settled once in [#start].
    private boolean backend;

    /// Set to skip the detection in [#start]. The native packaging goal knows
    /// the answer and the classpath cannot tell it: `cn1:backend-package`
    /// deliberately compiles against the JavaAPI with the runtime EXCLUDED from
    /// the classpath it passes here, so a detection reading that classpath
    /// concludes "not a backend module" for the one build that most certainly
    /// is one.
    private Boolean forcedFlavour;
    private Set<String> dependencyOverlays = Collections.emptySet();

    /** Runs after every processor has written its classes, including generated callers. */
    public void enhance(ProcessorContext ctx) throws ProcessingException {
        try { OrmEnhancer.enhance(accepted,ctx); }
        catch(IOException error) { throw new ProcessingException("ORM enhancement failed: "+error.getMessage(),error); }
    }

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
        try { dependencyOverlays=OrmEnhancer.prepare(ctx); }
        catch(IOException error) { throw new ProcessingException("Could not refresh enhanced dependencies",error); }
        if (forcedFlavour != null) {
            backend = forcedFlavour.booleanValue();
            return;
        }
        // BOTH RUNTIMES ON ONE CLASSPATH IS REFUSED, not guessed at.
        //
        // The classpath answers this question by which Database is on it, and
        // when both are it answers nothing -- so the old fallback quietly chose
        // the CLIENT flavour. For a backend module that pulls the core in
        // through a shared library that is the wrong half: the pass emits
        // Cn1Dao and DaoBootstrap instead of backend entity definitions, and a
        // generated backend entry point then references a BackendDaoBootstrap
        // nothing wrote, or starts with no entities registered at all. It also
        // made the two passes disagree, because backend-package forces the
        // backend flavour and the ordinary process-annotations pass did not.
        //
        // Refusing is the honest answer to an ambiguous classpath, and it is
        // one property away from being unambiguous. The shared-contract module
        // in this repository already sets it, which is what showed that the
        // classpath cannot decide this on its own.
        //
        // AFTER the property, not before it: -Dcn1.backendOrm is exactly the
        // answer this refusal asks for, so checking the classpath first refuses
        // the very builds that had already supplied it. That is not theoretical
        // -- it failed this repository's own contract build, which passes the
        // property on the command line.
        // ONLY true OR false. Anything else is refused rather than read as one of
        // them: "true".equalsIgnoreCase(v) makes every other spelling mean FALSE,
        // so -Dcn1.backendOrm=ture silently selected the client flavour AND
        // skipped the ambiguity guard below, which is the one case that guard
        // exists for. In a backend module carrying both runtimes the client
        // sources then compile, because the core classes really are there, and
        // the packaged server comes out with none of its backend registrations --
        // a typo, a green build, and a server that does nothing.
        String settled = System.getProperty(BACKEND_ORM_PROPERTY);
        if (settled != null && settled.length() > 0) {
            if ("true".equalsIgnoreCase(settled)) {
                backend = true;
                return;
            }
            if ("false".equalsIgnoreCase(settled)) {
                backend = false;
                return;
            }
            ctx.error("-D" + BACKEND_ORM_PROPERTY + "=" + settled + " is not a value "
                    + "this understands. It selects which ORM the @Entity classes in "
                    + "this module are stored through, and the only answers are true "
                    + "for the server-side backend and false for the client.");
            return;
        }
        if (onCompileClasspath(ctx, BACKEND_DATABASE)
                && onCompileClasspath(ctx, CLIENT_DATABASE)) {
            ctx.error("this module has both the Codename One core and the "
                    + "server-side backend on its compile classpath, so which ORM "
                    + "its @Entity classes are stored through cannot be read from "
                    + "the classpath. Set -Dcn1.backendOrm=true for a server module "
                    + "or -Dcn1.backendOrm=false for a client one.");
            return;
        }
        backend = isBackendModule(ctx);
    }

    /// Generates server-side daos whatever the classpath looks like. See
    /// [#forcedFlavour].
    public void setBackendFlavour(boolean serverSide) {
        this.forcedFlavour = Boolean.valueOf(serverSide);
    }

    /// Settles the flavour outright when the classpath cannot. Read in one
    /// place, [#start], so there is one spelling of what it accepts.
    private static final String BACKEND_ORM_PROPERTY = "cn1.backendOrm";

    /// The two classes whose presence tells the runtimes apart.
    private static final String BACKEND_DATABASE = "com/codename1/backend/Database.class";
    private static final String CLIENT_DATABASE = "com/codename1/db/Database.class";

    /// Which runtime this module's entities are stored through.
    ///
    /// Reached only when `-Dcn1.backendOrm` was NOT set -- [#start] settles that
    /// case, and validates it -- and only when the classpath is unambiguous,
    /// which [#start] also checks. Reading the property here as well was one
    /// spelling of "true" in two places, and the second one had no way to
    /// report a value it did not understand.
    ///
    /// The backend flavour when the backend's `Database` is on the
    /// compile classpath and the core's is not. A client module has the core
    /// and not the backend, so it takes the other branch and nothing about
    /// existing projects changes. A module with BOTH never reaches here --
    /// [#start] refuses it rather than letting this pick a half.
    private static boolean isBackendModule(ProcessorContext ctx) {
        return onCompileClasspath(ctx, BACKEND_DATABASE)
                && !onCompileClasspath(ctx, CLIENT_DATABASE);
    }

    /// Whether a class file is on the compile classpath, as a directory entry or
    /// a jar entry. Nothing is loaded and nothing is parsed: a build must not run
    /// a dependency's static initialisers to answer a question about its shape.
    private static boolean onCompileClasspath(ProcessorContext ctx, String entryName) {
        for (String element : ctx.getCompileClasspath()) {
            File file = new File(element);
            if (file.isDirectory()) {
                if (new File(file, entryName.replace('/', File.separatorChar)).isFile()) {
                    return true;
                }
                continue;
            }
            if (!file.isFile()) {
                continue;
            }
            try {
                ZipFile zip = new ZipFile(file);
                try {
                    if (zip.getEntry(entryName) != null) {
                        return true;
                    }
                } finally {
                    zip.close();
                }
            } catch (IOException unreadable) {
                // An unreadable entry is not an answer. Nothing to report: this
                // question is asked of every classpath element, and most of them
                // are legitimately not archives.
                continue;
            }
        }
        return false;
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        processClass(cls, ctx, true);
    }

    /// @param fromThisModule whether `cls` came out of this module's own
    ///     `target/classes`, and so must still have a source file behind it.
    ///     False for the classpath scan, whose entities live in a DEPENDENCY and
    ///     legitimately have no source under these roots.
    private void processClass(AnnotatedClass cls, ProcessorContext ctx, boolean fromThisModule)
            throws ProcessingException {
        if (cls.isSynthetic()) return;
        if (fromThisModule && dependencyOverlays.contains(cls.getBinaryName())) return;
        AnnotationValues entityAnn = cls.getClassAnnotation(ENTITY_DESC);
        if (entityAnn == null) return;
        // A DELETED ENTITY LEAVES ITS CLASS FILE BEHIND. Maven does not clean
        // target/classes between incremental builds, so an entity whose source was
        // deleted or renamed is still there, still annotated, and is still accepted
        // here -- which keeps `accepted` non-empty, so finish() never reaches
        // removeAStaleBootstrap and regenerates the dao and the bootstrap entry for
        // a class the developer removed. The backend then registers it and can
        // recreate its table, and nothing says why until someone runs mvn clean.
        //
        // The same check RestControllerAnnotationProcessor uses for the same
        // problem, and it is conservative in the right direction: only a class it
        // can positively show has no source is skipped, so a live one is never
        // dropped.
        //
        // NOT applied to the classpath scan. An entity is the one class both halves
        // of an application own, so the natural place for it is a module they both
        // depend on -- and those classes have no source under THIS module's roots
        // by construction. Filtering them here would delete the main use case.
        if (fromThisModule && !BuildHintAnnotationProcessor.hasBackingSource(
                cls, ctx.getCompileSourceRoots(), ctx.getSourceEncoding())) {
            return;
        }
        if (cls.isInterface()) {
            ctx.error(cls, "@Entity requires a concrete class; " + cls.getBinaryName()
                    + " is abstract or an interface");
            return;
        }
        // A NESTED ENTITY IS REFUSED, and the refusal is the whole of the support
        // rather than a missing feature nobody noticed.
        //
        // A public static member class can perfectly well have a public no-arg
        // constructor, so this processor accepted one -- and then wrote its
        // BINARY name, p.Outer$Note, into every type reference the dao emits.
        // javac reads that as a top-level identifier and cannot resolve it, so
        // the generated source did not compile for a class already approved.
        //
        // Using the source form fixes that one layer and uncovers two more: the
        // dao's own name inherits the "$", and the translated header it produces
        // is named from a different spelling, so the bootstrap fails to build --
        // and flattening THAT leaves the nested entity class itself with no
        // header emitted at all. Measured, in that order, each one behind the
        // last. Supporting nested entities properly is a translator change, not
        // a processor one, so this says so instead of generating output that
        // cannot be built.
        // getSourceName answers the binary name for a top-level class and the
        // dotted form for a nested one, decided from the InnerClasses attribute
        // rather than from the dollar sign -- which matters, because a dollar is
        // legal in a top-level name and the two spellings are not interchangeable.
        if (!cls.getSourceName().equals(cls.getBinaryName())) {
            ctx.error(cls, "@Entity " + cls.getBinaryName() + " is a nested class. "
                    + "Entities have to be top-level: the generated dao refers to the "
                    + "entity by name, and a nested one cannot be named from the "
                    + "top-level source the generator writes. Move it into a file of "
                    + "its own.");
            return;
        }
        if (!cls.isAbstract() && !hasPublicNoArgConstructor(cls)) {
            ctx.error(cls, "@Entity class " + cls.getBinaryName()
                    + " must declare a public no-arg constructor");
            return;
        }

        EntityClass ec = new EntityClass();
        ec.binaryName = cls.getBinaryName();ec.abstractClass=cls.isAbstract();ec.parent=cls.getSuperInternalName();
        ec.simpleName = simpleName(cls.getBinaryName());
        ec.packageName = packageOf(cls.getBinaryName());
        ec.daoSimpleName = ec.simpleName + (backend ? BACKEND_DAO_SUFFIX : "Cn1Dao");
        ec.daoBinaryName = (ec.packageName.length() == 0)
                ? ec.daoSimpleName
                : ec.packageName + "." + ec.daoSimpleName;
        Object indexes=entityAnn.get("indexes");
        if(indexes instanceof List) for(Object index:(List)indexes) ec.indexes.add((AnnotationValues)index);
        String table = entityAnn.getString("table");
        ec.tableName = (table == null || table.length() == 0) ? ec.simpleName : table;
        if (backend && tooLongForAnEngine(ec.tableName)) {
            ctx.error(cls, "@Entity " + ec.binaryName + " is stored in a table named '"
                    + ec.tableName + "', which is " + utf8Length(ec.tableName) + " bytes. "
                    + "MySQL refuses an identifier over 64 and PostgreSQL silently "
                    + "TRUNCATES past 63, so the name has to fit in "
                    + MAX_IDENTIFIER_BYTES + ".");
            return;
        }

        String[] events={"PrePersist","PostPersist","PreUpdate","PostUpdate","PreRemove","PostRemove","PostLoad"};
        for (MethodInfo method : persistentMethods(cls,ctx)) for (int event=0;event<events.length;event++) {
            if (method.getAnnotations().containsKey("Lcom/codename1/annotations/db/"+events[event]+";")) {
                if (!method.isPublic() || !"()V".equals(method.getDescriptor()) || method.isStatic()) {
                    ctx.error(cls,"Lifecycle callback must be a public non-static void method with no parameters: "+method.getName());
                } else ec.callbacks[event]=ec.callbacks[event]==null?method.getName():ec.callbacks[event]+"(); e."+method.getName();
            }
        }

        for (FieldPath fieldPath : persistentFields(cls,ec,ctx,"",new LinkedHashSet<String>())) {
            FieldInfo f=fieldPath.field;
            if (f.isStatic()) continue;
            if (f.getName().startsWith("this$")) continue;
            if (f.getAnnotation(DB_TRANSIENT_DESC) != null) continue;
            if (!f.isPublic()) {
                for(String annotation:f.getAnnotations().keySet()) {
                    if(annotation.equals(ID_DESC) || annotation.equals(COLUMN_DESC) || annotation.equals("Lcom/codename1/annotations/db/Version;")
                        || annotation.equals("Lcom/codename1/annotations/db/OneToOne;") || annotation.equals("Lcom/codename1/annotations/db/OneToMany;")
                        || annotation.equals("Lcom/codename1/annotations/db/ManyToOne;") || annotation.equals("Lcom/codename1/annotations/db/ManyToMany;")
                        || annotation.matches("Lcom/codename1/annotations/db/(ElementCollection|Convert|GeneratedValue|JoinColumn|JoinTable|MapKey|MapKeyColumn|OrderBy|OrderColumn);"))
                        ctx.error(cls,"Persistent annotated fields currently require public access: "+f.getName());
                }
                continue;
            }
            RelationField relationship = relation(f, ec, ctx);
            if (relationship != null) {
                if(fieldPath.prefix.length()>0) { ctx.error(cls,"Relationships inside embeddables are not supported: "+fieldPath.path);continue; }
                relationship.declaringType=fieldPath.declaringType;relationship.index=ec.relations.size(); ec.relations.add(relationship); continue; }
            PersistedField pf = new PersistedField();
            pf.primitive = f.getDescriptor().length()==1;
            pf.javaType = org.objectweb.asm.Type.getType(f.getDescriptor()).getClassName();
            pf.fieldName = fieldPath.path;pf.declaringType=fieldPath.declaringType;
            pf.embeddedParent = fieldPath.prefix;
            pf.version = f.getAnnotation("Lcom/codename1/annotations/db/Version;") != null;
            pf.kind = PropertyTypeKind.of(f);
            if(f.isFinal() && pf.kind.kind!=PropertyTypeKind.Kind.PROPERTY) ctx.error(cls,"Persistent fields must be writable: "+pf.fieldName);
            AnnotationValues conversion=f.getAnnotation("Lcom/codename1/annotations/db/Convert;");
            if(conversion!=null) {
                Object converter=conversion.get("converter"),storage=conversion.get("storageType");
                if(!(converter instanceof org.objectweb.asm.Type)) { ctx.error(cls,"Missing converter for "+pf.fieldName);continue; }
                pf.converter=((org.objectweb.asm.Type)converter).getClassName();pf.domainType=org.objectweb.asm.Type.getType(f.getDescriptor()).getClassName();
                pf.kind=PropertyTypeKind.scalar(storage instanceof org.objectweb.asm.Type?((org.objectweb.asm.Type)storage).getClassName():"java.lang.String");
                AnnotatedClass converterClass=findType(pf.converter,ctx);
                if(converterClass==null || !hasPublicNoArgConstructor(converterClass)) ctx.error(cls,"Converter requires a public no-arg constructor: "+pf.converter);
                if(f.getAnnotation(ID_DESC)!=null || pf.version || ec.embeddedId!=null && pf.fieldName.startsWith(ec.embeddedId+".")) ctx.error(cls,"Identifier and version fields cannot declare converters");
            }
            AnnotatedClass enumClass = pf.kind.kind == PropertyTypeKind.Kind.REFERENCE ? findType(pf.kind.binaryName,ctx) : null;
            if (pf.kind.kind == PropertyTypeKind.Kind.REFERENCE && enumClass != null && enumClass.isEnum())
                pf.kind = PropertyTypeKind.enumType(pf.kind.binaryName);
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
            if (backend && pf.kind.kind == PropertyTypeKind.Kind.PROPERTY) {
                // Property lives in com.codename1.properties, which is part of
                // the client core and not of the server runtime. Saying so here
                // beats a generated class that does not compile.
                ctx.error(cls, "@Entity field " + ec.binaryName + "." + f.getName()
                        + " is a Property, which the server-side runtime does not have. "
                        + "Use a plain field for an entity a backend module stores.");
                continue;
            }
            AnnotationValues col = f.getAnnotation(COLUMN_DESC);
            pf.unique=col!=null && col.getBoolOrDefault("unique",false);
            String colName = null;
            String colType = null;
            boolean nullable = true;
            if (col != null) {
                colName = col.getString("name");
                colType = col.getString("type");
                nullable = col.getBoolOrDefault("nullable", true);
            }
            pf.columnName = (colName == null || colName.length() == 0) ? pf.fieldName.replace('.', '_') : fieldPath.prefix.replace('.', '_')+colName;
            pf.sqlType = (colType == null || colType.length() == 0) ? defaultSqlType(pf.kind) : colType;
            // The EXPLICIT type, separately: the client flavour needs a type for
            // every column and defaults it to SQLite's, while the backend one
            // needs to know whether the developer named one at all -- a column
            // with no @Column(type) is typed by the dialect, which is what keeps
            // the entity portable.
            pf.explicitSqlType = (colType == null || colType.length() == 0) ? null : colType;
            pf.nullable = nullable;

            pf.dialectKind = dialectKind(pf.kind);
            pf.boxed = isBoxed(pf.kind);
            if (pf.version) {
                if (pf.kind.kind != PropertyTypeKind.Kind.INT && pf.kind.kind != PropertyTypeKind.Kind.LONG) {
                    ctx.error(cls, "@Version requires an int or long field: " + pf.fieldName);
                }
                if (f.getAnnotation(ID_DESC) != null) ctx.error(cls, "@Version cannot also be @Id: " + pf.fieldName);
                for (PersistedField previous : ec.fields) {
                    if (previous.version) ctx.error(cls, "An entity can have only one @Version field");
                }
                pf.nullable = false;
            }
            if (backend && isJavaPrimitive(pf)) {
                // A PRIMITIVE CANNOT HOLD NULL, so the column it is stored in
                // must not allow one: a nullable column read into an int gave
                // the field 0 where the row held nothing, and the two are the
                // same bits to everything downstream. Declared here rather than
                // only refused on the way in, so the database enforces it too.
                //
                // Backend only. The client flavour writes a SQLite schema that
                // already exists in shipped applications, and CREATE TABLE IF
                // NOT EXISTS would leave those alone while new installs got a
                // different one -- a split worth making on its own and not as a
                // side effect of this.
                if (col != null && col.getBoolOrDefault("nullable", false)) {
                    ctx.error(cls, "@Entity field " + ec.binaryName + "." + f.getName()
                            + " is a primitive and is marked @Column(nullable=true), which "
                            + "cannot both be true: a primitive has no null to read. Declare "
                            + "the field as its boxed type.");
                    continue;
                }
                pf.nullable = false;
            }

            if (backend && pf.kind.kind == PropertyTypeKind.Kind.CHAR
                    && pf.explicitSqlType != null) {
                // A char is stored as its UTF-16 CODE UNIT, in an integer
                // column, so the value bound for 'x' is 120. Declared onto a
                // text column -- @Column(type = "CHAR(1)") is the natural way to
                // meet an existing schema -- PostgreSQL then refuses 120 as too
                // long for CHAR(1) and MySQL truncates or refuses it. The write
                // and the declaration would disagree.
                //
                // Refused rather than guessed at: deciding whether an arbitrary
                // type string is textual means parsing CHAR(1), VARCHAR(4),
                // TEXT, citext and whatever else three dialects accept, and
                // being wrong there writes the wrong shape into a column that
                // takes it. The same reasoning refuses @Column(type) on a
                // generated key above.
                //
                // Values.asCodeUnitObject still READS text, which is not
                // inconsistent: a table this ORM did not create can have a
                // CHAR(1) where the entity has a char, and reading it is the one
                // direction that can be done unambiguously.
                ctx.error(cls, "@Entity field " + ec.binaryName + "." + f.getName()
                        + " is a char and declares @Column(type=\"" + pf.explicitSqlType
                        + "\"). A char is stored as its UTF-16 code unit in an integer "
                        + "column, so a text type here would be written a number. Drop the "
                        + "type, or declare the field as a String to store one-character "
                        + "text.");
                continue;
            }

            if (backend && tooLongForAnEngine(pf.columnName)) {
                ctx.error(cls, "@Entity field " + ec.binaryName + "." + f.getName()
                        + " maps to a column named '" + pf.columnName + "', which is "
                        + utf8Length(pf.columnName) + " bytes. MySQL refuses an identifier "
                        + "over 64 and PostgreSQL silently TRUNCATES past 63, so the name "
                        + "has to fit in " + MAX_IDENTIFIER_BYTES + ".");
                continue;
            }

            if(fieldPath.prefix.length()>0 && (col==null || col.getBoolOrDefault("nullable",true))) pf.nullable=true;
            AnnotationValues idAnn = f.getAnnotation(ID_DESC);
            if (idAnn != null || (ec.embeddedId!=null && pf.fieldName.startsWith(ec.embeddedId+"."))) {
                pf.isId = true;
                pf.autoIncrement = idAnn!=null && idAnn.getBoolOrDefault("autoIncrement", true);
                AnnotationValues generator=f.getAnnotation("Lcom/codename1/annotations/db/GeneratedValue;");
                if(generator!=null) {
                    Object strategy=generator.get("strategy");
                    String name=strategy instanceof String[]?((String[])strategy)[1]:"IDENTITY";
                    ec.generation="UUID".equals(name)?1:"SEQUENCE".equals(name)?2:"TABLE".equals(name)?3:0;
                    ec.generator=generator.getStringOrDefault("generator","");
                    if(ec.generator.length()==0) ec.generator=ec.tableName+"_"+pf.columnName;
                    pf.autoIncrement=ec.generation==0;
                    if(ec.generation==1 && pf.kind.kind!=PropertyTypeKind.Kind.STRING) ctx.error(cls,"UUID generation requires a String identifier");
                    if(ec.generation>1 && pf.kind.kind!=PropertyTypeKind.Kind.INT && pf.kind.kind!=PropertyTypeKind.Kind.LONG)
                        ctx.error(cls,"Sequence/table generation requires an int or long identifier");
                }
                if (backend && pf.autoIncrement && !isGeneratableKey(pf.kind)) {
                    // A DATABASE COUNTS. Every other type fails somewhere the
                    // build cannot see: a String key is refused by SQLite, whose
                    // AUTOINCREMENT is legal only after INTEGER PRIMARY KEY; a
                    // byte[] one commits the insert and then throws reading the
                    // generated Long back; a boolean one turns every key into
                    // true, so the first row's id is 1 and every later update and
                    // delete targets it.
                    ctx.error(cls, "@Id on " + ec.binaryName + "." + f.getName()
                            + " is autoIncrement and the field is " + pf.kind.binaryName
                            + "; a database generates integer keys, and wide enough ones. "
                            + "Use int or long (or their boxed forms) -- byte and short run "
                            + "out after 127 and 32,767 rows and the key then narrows to a "
                            + "negative number -- or @Id(autoIncrement = false) and assign "
                            + "the key yourself.");
                }
                if (backend && pf.autoIncrement && pf.explicitSqlType != null) {
                    // A GENERATED KEY'S TYPE IS THE DIALECT'S, and the three
                    // spell it differently enough that an explicit one cannot be
                    // written through: SQLite's AUTOINCREMENT is legal only after
                    // the exact words INTEGER PRIMARY KEY -- that is what makes
                    // the column a rowid alias -- while PostgreSQL wants
                    // GENERATED BY DEFAULT AS IDENTITY and MySQL AUTO_INCREMENT.
                    // Splicing BIGINT UNSIGNED into those produces a syntax error
                    // on one engine and a different schema on another. Refused
                    // here rather than discarded in Table.declaration, which is
                    // what happened before and left @Column(type) quietly broken
                    // on exactly one column. An ASSIGNED key keeps its declared
                    // type, because there nothing else has to agree with it.
                    ctx.error(cls, "@Id on " + ec.binaryName + "." + f.getName()
                            + " is autoIncrement and declares @Column(type=\""
                            + pf.explicitSqlType + "\"). A generated key's column type is "
                            + "the engine's to choose, and the three spell it differently. "
                            + "Drop the type, or use @Id(autoIncrement = false) and assign "
                            + "the key yourself, where the type is honoured.");
                }
                if (backend && !pf.autoIncrement && pf.dialectKind == KIND_BLOB) {
                    // And a blob is not a key on MySQL at all: it indexes one
                    // only by a prefix whose length it has to be given, so the
                    // generated CREATE TABLE is refused there and accepted by the
                    // other two. The same entity would build in development and
                    // fail in production.
                    ctx.error(cls, "@Id on " + ec.binaryName + "." + f.getName()
                            + " is a byte[], which MySQL cannot make a primary key without "
                            + "a prefix length. Use a String or an integer key.");
                }
                if(ec.idField==null) ec.idField=pf;
                ec.idFields.add(pf);pf.nullable=false;
            }
            ec.fields.add(pf);
        }

        if (ec.idField == null) {
            ctx.error(cls, "@Entity " + ec.binaryName + " requires at least one @Id field");
            return;
        }
        if(ec.idFields.size()>1) for(PersistedField field:ec.idFields) {
            if(field.autoIncrement || ec.generation!=0) ctx.error(cls,"Composite identifiers must be assigned; use @Id(autoIncrement=false)");
            if(field.dialectKind==KIND_BLOB) ctx.error(cls,"Composite identifiers cannot contain binary components: "+field.fieldName);
        }
        // TWO FIELDS, ONE COLUMN. Every statement then names the column twice:
        // the CREATE TABLE is refused for a duplicate column, and against a
        // schema somebody else created -- where the ORM creates nothing -- a read
        // loads the same value into both fields and a write stores whichever the
        // generated order put last. Caught here, where both field names can be
        // said out loud.
        for (int i = 0; i < ec.fields.size(); i++) {
            for (int j = i + 1; j < ec.fields.size(); j++) {
                PersistedField a = ec.fields.get(i);
                PersistedField b = ec.fields.get(j);
                // IGNORING CASE: SQLite and MySQL treat "name" and "NAME" as
                // one column even quoted, while PostgreSQL keeps them apart, so
                // a mapping only PostgreSQL accepts is not portable and the
                // strictest engine decides. equalsIgnoreCase is locale
                // independent; toLowerCase is not.
                if (a.columnName.equalsIgnoreCase(b.columnName)) {
                    ctx.error(cls, "@Entity " + ec.binaryName + " maps both " + a.fieldName
                            + " and " + b.fieldName + " to the column '" + a.columnName
                            + "'. Give one of them a @Column(name) of its own, or mark it "
                            + "@DbTransient.");
                }
            }
        }
        accepted.put(ec.binaryName, ec);
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        {
            // BEFORE the emptiness check, because the entities need not be in
            // this module at all. An entity is the one class both halves of an
            // application own, so the natural place for it is a module the app
            // and the server both depend on -- and then the backend module's own
            // compiled classes hold none of them.
            scanClasspathEntities(ctx);
        }
        resolveRelations(ctx);
        for(EntityClass entity:accepted.values()) for(PersistedField field:entity.fields) field.declaredRequired=!field.nullable;
        resolveHierarchies(ctx);
        validateRequiredIdentityCycles(ctx);
        for(EntityClass entity:accepted.values()) {
            Set<String> fields=new LinkedHashSet<String>(),columns=new LinkedHashSet<String>(),indexNames=new LinkedHashSet<String>();
            for(PersistedField field:entity.fields) {
                if(!fields.add(field.fieldName)) ctx.error("Duplicate persistent Java path on "+entity.binaryName+": "+field.fieldName);
                if(!columns.add(field.columnName.toLowerCase(java.util.Locale.ROOT))) ctx.error("Duplicate column on "+entity.binaryName+": "+field.columnName);
            }
            for(AnnotationValues index:entity.indexes) {
                String name=index.getStringOrDefault("name","");
                if(name.length()>0 && (tooLongForAnEngine(name) || !indexNames.add(name.toLowerCase(java.util.Locale.ROOT)))) ctx.error("Invalid or duplicate index name: "+name);
                Object indexed=index.get("fields");
                if(!(indexed instanceof List) || ((List)indexed).isEmpty()) ctx.error("Index needs mapped fields: "+name);
                else for(Object field:(List)indexed) if(!fields.contains(field)) ctx.error("Unknown index field: "+field);
            }
        }
        Map<String,String> schemaTables=new LinkedHashMap<String,String>(),schemaIndexes=new LinkedHashMap<String,String>();
        boolean tableGenerators=false;
        for(EntityClass entity:accepted.values()) {
            String owner=entity.hierarchyRoot==null?entity.binaryName:entity.hierarchyRoot;
            claimSchemaName(schemaTables,entity.tableName,owner,ctx);
            tableGenerators|=entity.generation>1;
            for(PersistedField field:entity.fields) if(backend && tooLongForAnEngine(field.columnName)) ctx.error("Column name exceeds the portable limit: "+field.columnName);
        }
        for(EntityClass entity:accepted.values()) for(RelationField relation:entity.relations) if(relation.many && relation.mappedBy.length()==0) {
            String owner=(entity.hierarchyRoot==null?entity.binaryName:entity.hierarchyRoot)+"."+relation.field;
            claimSchemaName(schemaTables,relation.table,owner,ctx);
            Set<String> columns=new LinkedHashSet<String>();
            List<String> physical=new ArrayList<String>();
            for(PersistedField key:entity.idFields) physical.add(relation.ownerColumn+(entity.idFields.size()==1?"":"_"+key.columnName));
            if(relation.element) {
                physical.add(relation.targetColumn);physical.add(relation.orderColumn);if(relation.mapKey.length()>0) physical.add(relation.mapKey);
            } else {
                EntityClass target=accepted.get(relation.target);
                if(target!=null) for(PersistedField key:target.idFields) physical.add(relation.targetColumn+(target.idFields.size()==1?"":"_"+key.columnName));
                if(relation.orderColumn.length()>0) physical.add(relation.orderColumn);
            }
            for(String column:physical) if(column.length()==0 || !columns.add(column.toLowerCase(java.util.Locale.ROOT)) || backend && tooLongForAnEngine(column)) ctx.error("Invalid or duplicate collection column: "+owner+"."+column);
        }
        // Runtime creates indexes once per physical table, including all three
        // implicit sources: unique columns, owning to-one FKs and join tables.
        Set<String> indexedTables=new LinkedHashSet<String>(),indexedCollections=new LinkedHashSet<String>();
        for(EntityClass entity:accepted.values()) {
            if(indexedTables.add(entity.tableName.toLowerCase(java.util.Locale.ROOT)))
                claimEntityIndexes(schemaIndexes,entity,ctx);
            for(RelationField relation:entity.relations) if(relation.many && !relation.element && relation.mappedBy.length()==0
                    && indexedCollections.add(relation.table.toLowerCase(java.util.Locale.ROOT))) {
                EntityClass target=accepted.get(relation.target);
                if(target!=null) claimIndex(schemaIndexes,"",relation.unique,relation.table,
                        joinColumnNames(relation.targetColumn,target),ctx);
            }
        }
        if(tableGenerators && schemaTables.containsKey("cn1_orm_sequences")) ctx.error("cn1_orm_sequences is reserved for identifier generation");
        for(String name:schemaIndexes.keySet()) {
            if(schemaTables.containsKey(name) || tableGenerators && "cn1_orm_sequences".equals(name))
                ctx.error("Index name conflicts with table: "+name);
        }
        for(EntityClass entity:accepted.values()) if(entity.generation==2) {
            String name=entity.generator.toLowerCase(java.util.Locale.ROOT);
            if(tooLongForAnEngine(entity.generator)) ctx.error("Sequence name exceeds the portable limit: "+entity.generator);
            if(schemaTables.containsKey(name) || schemaIndexes.containsKey(name) || "cn1_orm_sequences".equals(name))
                ctx.error("Sequence name conflicts with schema object: "+entity.generator);
        }
        if (ctx.hasErrors()) return;
        if (accepted.isEmpty()) {
            removeAStaleBootstrap(ctx);
            return;
        }

        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (EntityClass ec : accepted.values()) {
            if (backend && wouldReplaceAnExistingClass(ec.daoBinaryName,
                    "dao for " + ec.binaryName, ctx)) {
                continue;
            }
            sources.put(ec.daoBinaryName,
                    ec.hierarchyRoot!=null?generateHierarchyRegistration(ec,backend):backend ? generateBackendDaoSource(ec) : generateDaoSource(ec));
            sources.put(ec.binaryName + (backend ? "Cn1BackendModel" : "Cn1Model"), generateModelSource(ec, backend));
        }
        if (ctx.hasErrors()) return;
        // THE BOOTSTRAP IS NOT CHECKED, and the difference from the daos above
        // is whose namespace the name sits in. A dao is named after the
        // developer's entity and lands in the developer's package, so
        // NoteCn1BackendDao is a name they could reasonably have chosen and a
        // collision there is theirs. The bootstrap has a fixed name in `cn1app`,
        // a package this processor generates into and nothing else does -- a
        // class there is squatting on a reserved name, not colliding with one.
        //
        // And checking it costs something real: target/classes from a build
        // before the marker existed holds an unmarked bootstrap, so the check
        // would fail the first build after an upgrade with a message telling the
        // developer to rename a class they never wrote. It carries the marker
        // regardless, which is what makes its provenance readable.
        sources.put(backend ? BACKEND_BOOTSTRAP_BINARY : BOOTSTRAP_BINARY,
                generateBootstrapSource(accepted.values(), backend));
        try {
            java.util.List<java.io.File> cp = new java.util.ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            // AND THE COMPILE CLASSPATH. The dao names its entity, and the entity
            // need not be in this module: a class both halves of an application
            // share lives in a module both depend on, and then it is only ever on
            // the classpath. With the output directory alone the generated source
            // failed to compile on "cannot find symbol" naming the entity.
            for (String element : ctx.getCompileClasspath()) {
                cp.add(new java.io.File(element));
            }
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
            OrmEnhancer.enhance(accepted, ctx);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated dao sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size() + " @Entity "
                + (backend ? "server-side " : "") + "dao(s) + "
                + (backend ? BACKEND_BOOTSTRAP_BINARY : BOOTSTRAP_BINARY));
    }

    // ---------------------------------------------------------------
    // Source generation
    // ---------------------------------------------------------------

    private AnnotatedClass findType(String binary,ProcessorContext ctx) {
        String internal=binary.replace('.','/');AnnotatedClass found=ctx.lookup(internal);
        if(found!=null && !dependencyOverlays.contains(binary)) return found;
        for(String path:ctx.getCompileClasspath()) {
            File file=new File(path);
            try {
                if(file.isDirectory()) {
                    File type=new File(file,internal+".class");
                    if(type.isFile()) {
                        InputStream in=new java.io.FileInputStream(type);
                        try { return ClassScanner.readClass(in,type); } finally { in.close(); }
                    }
                } else if(file.isFile()) {
                    ZipFile zip=new ZipFile(file);
                    try { ZipEntry entry=zip.getEntry(internal+".class");if(entry!=null) return readEntry(zip,entry,file,ctx); }
                    finally { zip.close(); }
                }
            } catch(IOException error) { ctx.error("Cannot inspect "+binary+": "+error.getMessage()); }
            catch(ProcessingException error) { ctx.error("Cannot inspect "+binary+": "+error.getMessage()); }
        }
        return null;
    }

    private void claimSchemaName(Map<String,String> names,String name,String owner,ProcessorContext ctx) {
        if(name.length()==0 || backend && tooLongForAnEngine(name)) { ctx.error("Invalid schema name: "+name);return; }
        String previous=names.put(name.toLowerCase(java.util.Locale.ROOT),owner);
        if(previous!=null && !previous.equals(owner)) ctx.error("Schema name '"+name+"' is shared by "+previous+" and "+owner);
    }

    private List<String> joinColumnNames(String prefix,EntityClass target) {
        List<String> columns=new ArrayList<String>();
        for(PersistedField key:target.idFields) columns.add(prefix+(target.idFields.size()==1?"":"_"+key.columnName));
        return columns;
    }

    private void claimEntityIndexes(Map<String,String> names,EntityClass entity,ProcessorContext ctx) {
        for(RelationField relation:entity.relations) if(relation.column>=0) {
            EntityClass target=accepted.get(relation.target);
            if(target!=null) {
                List<String> columns=new ArrayList<String>();
                for(int i=0;i<target.idFields.size();i++) columns.add(entity.fields.get(relation.column+i).columnName);
                claimIndex(names,"",relation.unique,entity.tableName,columns,ctx);
            }
        }
        for(AnnotationValues index:entity.indexes) {
            Object fields=index.get("fields");
            if(!(fields instanceof List)) continue;
            List<String> columns=new ArrayList<String>();
            for(Object name:(List)fields) {
                for(PersistedField field:entity.fields) if(field.fieldName.equals(name)) { columns.add(field.columnName);break; }
            }
            // Invalid field references have already been reported above.
            if(columns.isEmpty() || columns.size()!=((List)fields).size()) continue;
            claimIndex(names,index.getStringOrDefault("name",""),index.getBoolOrDefault("unique",false),entity.tableName,columns,ctx);
        }
        for(PersistedField field:entity.fields) if(field.unique)
            claimIndex(names,"",true,entity.tableName,java.util.Collections.singletonList(field.columnName),ctx);
    }

    private void claimIndex(Map<String,String> names,String name,boolean unique,String table,List<String> columns,ProcessorContext ctx) {
        if(name.length()==0) {
            // Keep identical to SessionImpl.constraintName, using physical SQL
            // columns (including every composite-key component), not Java paths.
            StringBuilder key=new StringBuilder(table);
            for(String column:columns) key.append('/').append(column);
            name="cn1_"+(unique?"unique":"index")+"_"+Integer.toHexString(key.toString().hashCode());
        }
        String previous=names.put(name.toLowerCase(java.util.Locale.ROOT),table);
        if(tooLongForAnEngine(name) || previous!=null) ctx.error("Invalid or duplicate index name: "+name);
    }

    private List<MethodInfo> persistentMethods(AnnotatedClass cls,ProcessorContext ctx) {
        List<MethodInfo> result=new ArrayList<MethodInfo>();
        String parent=cls.getSuperInternalName();
        if(parent!=null && !"java/lang/Object".equals(parent)) {
            AnnotatedClass base=findType(parent.replace('/','.'),ctx);
            if(base!=null && (base.getClassAnnotation("Lcom/codename1/annotations/db/MappedSuperclass;")!=null || base.getClassAnnotation(ENTITY_DESC)!=null))
                result.addAll(persistentMethods(base,ctx));
        }
        Set<String> events=new LinkedHashSet<String>();
        for(MethodInfo method:cls.getMethods()) {
            for(int i=result.size()-1;i>=0;i--) if(result.get(i).getName().equals(method.getName()) && result.get(i).getDescriptor().equals(method.getDescriptor())) result.remove(i);
            for(String annotation:method.getAnnotations().keySet()) {
                if(annotation.matches("Lcom/codename1/annotations/db/(PrePersist|PostPersist|PreUpdate|PostUpdate|PreRemove|PostRemove|PostLoad);"))
                    if(!events.add(annotation)) ctx.error(cls,"Duplicate lifecycle callback: "+annotation);
            }
            result.add(method);
        }
        return result;
    }

    private List<FieldPath> persistentFields(AnnotatedClass cls,EntityClass entity,ProcessorContext ctx,
                                             String prefix,Set<String> visiting) {
        List<FieldPath> result=new ArrayList<FieldPath>();
        if(!visiting.add(cls.getBinaryName())) { ctx.error("Recursive embedded value: "+cls.getBinaryName());return result; }
        String parent=cls.getSuperInternalName();
        if(parent!=null && !"java/lang/Object".equals(parent)) {
            AnnotatedClass base=findType(parent.replace('/','.'),ctx);
            if(base!=null && (base.getClassAnnotation("Lcom/codename1/annotations/db/MappedSuperclass;")!=null || base.getClassAnnotation(ENTITY_DESC)!=null))
                result.addAll(persistentFields(base,entity,ctx,prefix,visiting));
        }
        for(FieldInfo field:cls.getFields()) {
            String path=prefix+field.getName();
            for(FieldPath inherited:result) {
                if(inherited.field.isPublic() && !inherited.declaringType.equals(cls.getBinaryName())
                        && (inherited.path.equals(path) || inherited.path.startsWith(path+"."))) {
                    ctx.error("Hidden inherited persistent field: "+cls.getBinaryName()+"."+path);
                }
            }
            if(field.isStatic() || field.getAnnotation(DB_TRANSIENT_DESC)!=null) continue;
            if(field.getAnnotation("Lcom/codename1/annotations/db/Embedded;")!=null || field.getAnnotation("Lcom/codename1/annotations/db/EmbeddedId;")!=null) {
                if(field.getAnnotation("Lcom/codename1/annotations/db/EmbeddedId;")!=null) {
                    if(entity.embeddedId!=null) ctx.error("Multiple embedded identifiers");
                    entity.embeddedId=path;
                }
                if(!field.isPublic() || field.isFinal()) { ctx.error("Embedded field must be public and writable: "+path);continue; }
                String type=org.objectweb.asm.Type.getType(field.getDescriptor()).getClassName();
                AnnotatedClass embedded=findType(type,ctx);
                if(embedded==null || embedded.getClassAnnotation("Lcom/codename1/annotations/db/Embeddable;")==null
                        || !hasPublicNoArgConstructor(embedded)) { ctx.error("Embedded value requires @Embeddable and a public no-arg constructor: "+path);continue; }
                entity.embedded.put(path,type);
                for(FieldPath nested:persistentFields(embedded,entity,ctx,path+".",visiting)) result.add(new FieldPath(nested.field,nested.path,nested.prefix,cls.getBinaryName()));
            } else result.add(new FieldPath(field,path,prefix,cls.getBinaryName()));
        }
        visiting.remove(cls.getBinaryName());return result;
    }

    private RelationField relation(FieldInfo field, EntityClass owner, ProcessorContext ctx) {
        String prefix="Lcom/codename1/annotations/db/";
        AnnotationValues annotation=null; String kind=null;
        for(String candidate:new String[]{"ManyToOne","OneToOne","OneToMany","ManyToMany","ElementCollection"}) {
            AnnotationValues value=field.getAnnotation(prefix+candidate+";");
            if(value!=null) {
                if(annotation!=null) ctx.error("Multiple relationship annotations on "+owner.binaryName+"."+field.getName());
                annotation=value;kind=candidate;
            }
        }
        if(annotation==null) return null;
        if(field.isFinal()) ctx.error("Relationship fields must be writable: "+owner.binaryName+"."+field.getName());
        RelationField relation=new RelationField(); relation.field=field.getName();relation.descriptor=field.getDescriptor();
        relation.element="ElementCollection".equals(kind);relation.many=relation.element || kind.endsWith("ToMany");relation.unique="OneToOne".equals(kind) || "OneToMany".equals(kind);relation.kind=kind;
        relation.mappedBy=annotation.getStringOrDefault("mappedBy","");
        AnnotationValues mapKey=field.getAnnotation(prefix+"MapKey;"),orderColumn=field.getAnnotation(prefix+"OrderColumn;"),orderBy=field.getAnnotation(prefix+"OrderBy;");
        relation.mapKey=mapKey==null?"":mapKey.getStringOrDefault("name","");
        relation.orderColumn=orderColumn==null?"":orderColumn.getStringOrDefault("name","list_position");
        if(orderColumn!=null && relation.orderColumn.length()==0) relation.orderColumn="list_position";
        relation.orderBy=orderBy==null?"":orderBy.getStringOrDefault("value","");
        boolean map="Ljava/util/Map;".equals(field.getDescriptor());
        if(relation.element) {
            AnnotationValues keyColumn=field.getAnnotation(prefix+"MapKeyColumn;");
            if(map) { relation.mapKey=keyColumn==null?"map_key":keyColumn.getStringOrDefault("name","map_key");if(relation.mapKey.length()==0) relation.mapKey="map_key"; }
            if(relation.orderColumn.length()==0) relation.orderColumn="element_position";
            if(relation.orderBy.length()>0 || mapKey!=null) ctx.error("Scalar elements use OrderColumn/MapKeyColumn: "+relation.field);
        }
        if(map && relation.mapKey.length()==0) ctx.error("Map relationships require @MapKey(name=...): "+relation.field);
        if(!map && mapKey!=null) ctx.error("MapKey requires a Map field: "+relation.field);
        if(orderColumn!=null && (!"Ljava/util/List;".equals(field.getDescriptor()) || relation.mappedBy.length()>0)) ctx.error("OrderColumn requires an owning List with a join table: "+relation.field);
        if(orderColumn!=null && orderBy!=null) ctx.error("Choose OrderBy or OrderColumn: "+relation.field);
        if(!relation.many && (mapKey!=null || orderColumn!=null || orderBy!=null)) ctx.error("Collection metadata requires a to-many relationship");
        // Owning lists need an occurrence key even without explicit ordering;
        // an owner/target primary key cannot store repeated links.
        if(relation.many && !relation.element && relation.mappedBy.length()==0
                && "Ljava/util/List;".equals(field.getDescriptor()) && relation.orderColumn.length()==0)
            relation.orderColumn="list_position";
        relation.orphan=annotation.getBoolOrDefault("orphanRemoval",false);
        if(("ManyToMany".equals(kind) || "ManyToOne".equals(kind)) && relation.orphan)
            ctx.error("orphanRemoval requires OneToOne or OneToMany: "+owner.binaryName+"."+relation.field);
        if("ManyToOne".equals(kind) && relation.mappedBy.length()>0) ctx.error("ManyToOne must own its join column");
        relation.lazy=relation.many;
        Object fetch=annotation.get("fetch");if(fetch instanceof String[]) relation.lazy="LAZY".equals(((String[])fetch)[1]);
        Object cascades=annotation.get("cascade");
        if(cascades instanceof List) for(Object cascade:(List)cascades) {
            String op=((String[])cascade)[1];
            if("ALL".equals(op)) relation.cascade=31;
            else if("PERSIST".equals(op)) relation.cascade|=1;
            else if("MERGE".equals(op)) relation.cascade|=2;
            else if("REMOVE".equals(op)) relation.cascade|=4;
            else if("REFRESH".equals(op)) relation.cascade|=8;
            else if("DETACH".equals(op)) relation.cascade|=16;
        }
        relation.target=org.objectweb.asm.Type.getType(field.getDescriptor()).getClassName();
        if(relation.many) {
            String signature=field.getSignature();
            int start=signature==null?-1:signature.indexOf("<L");
            int end=start<0?-1:signature.indexOf(';',start);
            if(start<0 || end<0) ctx.error("Relationship needs a concrete collection element type: "+owner.binaryName+"."+relation.field);
            else {
                if(map) { relation.mapKeyType=signature.substring(start+2,end).replace('/','.');start=end;end=signature.indexOf(';',start+2); }
                if(end<0) ctx.error("Map requires a concrete entity value type: "+relation.field);
                else relation.target=signature.substring(start+2,end).replace('/','.');
            }
            if(!"Ljava/util/List;".equals(field.getDescriptor()) && !"Ljava/util/Set;".equals(field.getDescriptor()) && !map)
                ctx.error("Relationship collection must use List, Set or Map: "+owner.binaryName+"."+relation.field);
        }
        AnnotationValues join=field.getAnnotation(prefix+"JoinColumn;");
        relation.columnName=join==null?relation.field+"_id":join.getStringOrDefault("name",relation.field+"_id");
        if(relation.columnName.length()==0) relation.columnName=relation.field+"_id";
        relation.nullable=annotation.getBoolOrDefault("optional",true) && (join==null || join.getBoolOrDefault("nullable",true));
        AnnotationValues table=field.getAnnotation(prefix+"JoinTable;");
        relation.table=table==null?owner.tableName+"_"+relation.field:table.getStringOrDefault("name",owner.tableName+"_"+relation.field);
        relation.ownerColumn=table==null?"owner_id":table.getStringOrDefault("joinColumn","owner_id");
        relation.targetColumn=table==null?"target_id":table.getStringOrDefault("inverseJoinColumn","target_id");
        if(relation.table.length()==0) relation.table=owner.tableName+"_"+relation.field;
        if(relation.ownerColumn.length()==0) relation.ownerColumn="owner_id";
        if(relation.targetColumn.length()==0) relation.targetColumn="target_id";
        if(relation.mappedBy.length()>0 && (join!=null || table!=null)) ctx.error("Inverse associations must not declare join columns or tables: "+relation.field);
        if(relation.many && join!=null) ctx.error("Use mappedBy or JoinTable for a to-many association: "+relation.field);
        if(!relation.many && table!=null) ctx.error("To-one associations use JoinColumn: "+relation.field);
        if(relation.element) {
            relation.targetColumn=annotation.getStringOrDefault("column","element_value");
            if(map && (field.getSignature()==null || !field.getSignature().contains("<Ljava/lang/String;"))) ctx.error("Scalar maps require String keys: "+relation.field);
            PropertyTypeKind scalar=PropertyTypeKind.scalar(relation.target);
            if(scalar.kind==PropertyTypeKind.Kind.UNSUPPORTED || scalar.kind==PropertyTypeKind.Kind.BYTE_ARRAY) ctx.error("ElementCollection requires a supported scalar value: "+relation.target);
        }
        return relation;
    }

    private static String boxedDomainType(String type) {
        String[] primitive={"boolean","byte","short","int","long","float","double","char"};
        String[] boxed={"Boolean","Byte","Short","Integer","Long","Float","Double","Character"};
        for(int i=0;i<primitive.length;i++) if(primitive[i].equals(type)) return "java.lang."+boxed[i];
        return type;
    }

    private void resolveRelations(ProcessorContext ctx) {
        for(EntityClass owner:accepted.values()) for(RelationField relation:owner.relations) {
            if(relation.element) continue;
            EntityClass target=accepted.get(relation.target);
            if(target==null) { ctx.error("Relationship target is not an available @Entity: "+relation.target);continue; }
            if(relation.mapKey.length()>0) {
                boolean found=false;for(PersistedField field:target.fields) if(field.fieldName.equals(relation.mapKey) && field.relation==null) {
                    found=true;
                    String domain=field.kind.kind==PropertyTypeKind.Kind.PROPERTY?field.kind.elementBinaryName:field.javaType;
                    if(!relation.mapKeyType.equals(boxedDomainType(domain))) ctx.error("MapKey generic must match target domain type: "+relation.field);
                }
                if(!found) ctx.error("MapKey must name a target basic field: "+relation.mapKey);
            }
            if(relation.orderBy.length()>0) for(String clause:relation.orderBy.split(",")) {
                String[] parts=clause.trim().split("\\s+");boolean found=false;
                for(PersistedField field:target.fields) if(field.fieldName.equals(parts[0])) found=true;
                if(!found || parts.length>2 || parts.length==2 && !"ASC".equalsIgnoreCase(parts[1]) && !"DESC".equalsIgnoreCase(parts[1])) ctx.error("Invalid OrderBy: "+clause);
            }
            if(relation.mappedBy.length()>0) {
                RelationField inverse=null;
                for(RelationField candidate:target.relations) if(candidate.field.equals(relation.mappedBy)) inverse=candidate;
                if(inverse==null || accepted.get(inverse.target)==null || !descends(owner,accepted.get(inverse.target)) || inverse.mappedBy.length()>0
                    || !("OneToMany".equals(relation.kind)?"ManyToOne":relation.kind).equals(inverse.kind))
                    ctx.error("Invalid mappedBy for "+owner.binaryName+"."+relation.field);
            } else if(!relation.many) {
                relation.column=owner.fields.size();
                for(int part=0;part<target.idFields.size();part++) {
                    PersistedField key=target.idFields.get(part);
                    PersistedField fk=new PersistedField();
                    fk.declaringType=relation.declaringType;
                    fk.fieldName=relation.field+(target.idFields.size()==1?"":"."+key.fieldName);
                    fk.columnName=relation.columnName+(target.idFields.size()==1?"":"_"+key.columnName);
                    fk.kind=key.kind;fk.dialectKind=key.dialectKind;fk.boxed=key.boxed;fk.sqlType=key.sqlType;
                    fk.explicitSqlType=key.explicitSqlType;fk.nullable=relation.nullable;fk.relation=relation;fk.relationPart=part;
                    owner.fields.add(fk);
                }
            }
        }
    }

    private boolean descends(EntityClass child,EntityClass ancestor) {
        EntityClass current=child;
        while(current!=null) {
            if(current==ancestor) return true;
            current=current.parent==null?null:accepted.get(current.parent.replace('/','.'));
        }
        return false;
    }
    private void validateRequiredIdentityCycles(ProcessorContext ctx) {
        for(EntityClass entity:accepted.values()) {
            if(entity.idFields.size()==1 && entity.idFields.get(0).autoIncrement
                    && hasRequiredPath(entity,entity,new LinkedHashSet<EntityClass>())) {
                ctx.error("Required relationship cycle with identity-generated keys: "+entity.binaryName
                    +". Use preallocated identifiers or make a link nullable.");
            }
        }
    }

    private boolean hasRequiredPath(EntityClass current,EntityClass goal,Set<EntityClass> visited) {
        if(!visited.add(current)) return false;
        for(RelationField relation:current.relations) {
            // Use the actual foreign-key nullability after hierarchy resolution.
            if(relation.column<0 || current.fields.get(relation.column).nullable) continue;
            EntityClass target=accepted.get(relation.target);
            if(target==null) continue;
            for(EntityClass candidate:accepted.values()) {
                if(!descends(candidate,target)) continue;
                if(candidate==goal || hasRequiredPath(candidate,goal,visited)) return true;
            }
        }
        return false;
    }

    private void resolveHierarchies(ProcessorContext ctx) {
        for(EntityClass root:accepted.values()) {
            AnnotatedClass definition=findType(root.binaryName,ctx);
            AnnotationValues inheritance=definition.getClassAnnotation("Lcom/codename1/annotations/db/Inheritance;");
            if(inheritance==null) continue;
            if(root.parent!=null && accepted.containsKey(root.parent.replace('/','.'))) { ctx.error("Inheritance must be declared on the entity root: "+root.binaryName);continue; }
            String discriminator=inheritance.getStringOrDefault("discriminatorColumn","entity_type");
            List<EntityClass> family=new ArrayList<EntityClass>();family.add(root);
            for(EntityClass child:accepted.values()) if(child!=root && descends(child,root)) family.add(child);
            Map<String,PersistedField> fields=new LinkedHashMap<String,PersistedField>();Map<String,RelationField> relations=new LinkedHashMap<String,RelationField>();
            Set<String> values=new LinkedHashSet<String>();List<AnnotationValues> inheritedIndexes=new ArrayList<AnnotationValues>();
            for(EntityClass member:family) {
                inheritedIndexes.addAll(member.indexes);member.hierarchyRoot=root.binaryName;member.tableName=root.tableName;
                member.generation=root.generation;member.generator=root.generator;
                AnnotationValues tag=findType(member.binaryName,ctx).getClassAnnotation("Lcom/codename1/annotations/db/DiscriminatorValue;");
                member.discriminatorValue=tag==null?member.simpleName:tag.getStringOrDefault("value",member.simpleName);
                if(!member.discriminatorValue.matches("[A-Za-z0-9_$]{1,63}") || !values.add(member.discriminatorValue)) ctx.error("Invalid or duplicate discriminator: "+member.discriminatorValue);
                for(PersistedField field:member.fields) {
                    if(member!=root && (field.isId || field.version)) {
                        boolean inherited=false;
                        for(PersistedField rootField:root.fields) {
                            if(rootField.fieldName.equals(field.fieldName) && rootField.declaringType.equals(field.declaringType)
                                    && rootField.isId==field.isId && rootField.version==field.version) inherited=true;
                        }
                        if(!inherited) ctx.error("Identifier and version fields must be declared on or inherited by the hierarchy root: "+field.declaringType+"."+field.fieldName);
                    }
                    PersistedField prior=fields.get(field.fieldName);
                    if(prior==null) {
                        if(member!=root && !field.isId && !field.version) field.nullable=true;
                        fields.put(field.fieldName,field);
                    } else if(!prior.declaringType.equals(field.declaringType)) ctx.error("Hidden inherited persistent field: "+member.binaryName+"."+field.fieldName);
                }
                for(RelationField relation:member.relations) {
                    RelationField prior=relations.get(relation.field);
                    if(prior==null) relations.put(relation.field,relation);
                    else if(!prior.declaringType.equals(relation.declaringType))
                        ctx.error("Conflicting inherited relationship: "+prior.declaringType+"."+relation.field+" and "+relation.declaringType+"."+relation.field);
                }
            }
            if(fields.containsKey("__cn1_discriminator")) {
                ctx.error("Reserved persistent field name: __cn1_discriminator");continue;
            }
            PersistedField tag=new PersistedField();tag.fieldName="__cn1_discriminator";tag.columnName=discriminator;
            tag.kind=PropertyTypeKind.scalar("java.lang.String");tag.dialectKind=KIND_TEXT;tag.sqlType="TEXT";tag.discriminator=true;
            fields.put(tag.fieldName,tag);
            List<PersistedField> all=new ArrayList<PersistedField>(fields.values());
            int index=0;for(RelationField relation:relations.values()) {
                relation.index=index++;relation.column=-1;
                for(int i=0;i<all.size();i++) if(all.get(i).relation!=null && all.get(i).relation.field.equals(relation.field)) {
                    if(relation.column<0) relation.column=i;all.get(i).relation=relation;
                }
            }
            for(EntityClass member:family) for(EntityClass candidate:family) if(descends(candidate,member)) {
                for(PersistedField field:candidate.fields) member.queryFields.add(field.fieldName);
                for(RelationField relation:candidate.relations) member.queryRelations.add(relation.field);
            }
            for(EntityClass member:family) {
                member.fields.clear();member.fields.addAll(all);member.relations.clear();member.relations.addAll(relations.values());member.indexes.clear();member.indexes.addAll(inheritedIndexes);
                for(EntityClass descendant:family) if(!descendant.abstractClass && descends(descendant,member)) member.discriminators.add(descendant.discriminatorValue);
            }
        }
        for(EntityClass member:accepted.values()) if(member.hierarchyRoot==null && (member.abstractClass || member.parent!=null && accepted.containsKey(member.parent.replace('/','.'))))
            ctx.error("Entity inheritance requires @Inheritance on the root: "+member.binaryName);
    }
    private static String generateHierarchyRegistration(EntityClass entity,boolean backend) {
        return (entity.packageName.length()==0?"":"package "+entity.packageName+";\n")
            +(backend?"@com.codename1.backend.annotations.Generated\n":"")
            +"public final class "+entity.daoSimpleName+" { public static void register() { com.codename1.impl.orm.Models.register(new "+entity.simpleName+(backend?"Cn1BackendModel":"Cn1Model")+"()); }}\n";
    }
    private static String storageConversion(String type,String value) {
        String method="java.lang.Integer".equals(type)?"asIntObject":"java.lang.Long".equals(type)?"asLongObject":
            "java.lang.Short".equals(type)?"asShortObject":"java.lang.Byte".equals(type)?"asByteObject":
            "java.lang.Double".equals(type)?"asDoubleObject":"java.lang.Float".equals(type)?"asFloatObject":
            "java.lang.Boolean".equals(type)?"asBooleanObject":"java.lang.Character".equals(type)?"asCodeUnitObject":
            "java.util.Date".equals(type)?"asDate":"byte[]".equals(type)?"asBytes":"asString";
        return "com.codename1.impl.orm.Values."+method+"("+value+")";
    }
    private static String embeddedNullCondition(String path) {
        StringBuilder condition=new StringBuilder();
        int dot=path.indexOf('.');
        while(dot>=0) {
            condition.append("e.").append(path.substring(0,dot)).append(" == null || ");
            dot=path.indexOf('.',dot+1);
        }
        return condition.append("e.").append(path).append(" == null").toString();
    }

    private static String generateModelSource(EntityClass ec, boolean backend) {
        String pkg = "com.codename1.impl.orm.";
        String simple = ec.simpleName + (backend ? "Cn1BackendModel" : "Cn1Model");
        StringBuilder sb = new StringBuilder();
        if (!ec.packageName.isEmpty()) sb.append("package ").append(ec.packageName).append(";\n");
        sb.append("public final class ").append(simple).append(" extends ").append(pkg)
          .append("EntityModel<").append(ec.binaryName).append("> {\n")
          .append("  private static final ").append(pkg).append("Attribute[] ATTRS = {\n");
        for (int i = 0; i < ec.fields.size(); i++) {
            PersistedField f = ec.fields.get(i);
            int kind = f.dialectKind;
            if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY) {
                String type=f.kind.elementBinaryName;
                kind="java.lang.String".equals(type)?KIND_TEXT:
                    "java.lang.Double".equals(type) || "java.lang.Float".equals(type)?KIND_REAL:
                    "java.lang.Integer".equals(type) || "java.lang.Short".equals(type) || "java.lang.Byte".equals(type) || "java.lang.Character".equals(type)?KIND_INTEGER:KIND_BIGINT;
            }
            sb.append("    new ").append(pkg).append("Attribute(\"").append(escape(f.fieldName))
              .append("\", \"").append(escape(f.columnName)).append("\", ").append(kind)
              .append(", ").append(f.isId).append(", ").append(f.isId && f.autoIncrement)
              .append(", ").append(f.nullable).append(", ").append(f.version).append(", ")
              .append(f.explicitSqlType==null?"null":"\""+escape(f.explicitSqlType)+"\"").append(")")
              .append(i + 1 < ec.fields.size() ? ",\n" : "\n");
        }
        sb.append("  };\n  public Class<").append(ec.binaryName).append("> type() { return ")
          .append(ec.binaryName).append(".class; }\n")
          .append("  public String table() { return \"").append(escape(ec.tableName)).append("\"; }\n")
          .append("  public ").append(pkg).append("Attribute[] attributes() { return ATTRS.clone(); }\n")
          .append("  public ").append(ec.binaryName).append(" create() { ").append(ec.abstractClass?"throw new IllegalStateException(\"Abstract entity\");":"return new "+ec.binaryName+"();").append(" }\n")
          .append("  public Object get(").append(ec.binaryName).append(" e, int index) {\n    switch(index) {\n");
        for (int i = 0; i < ec.fields.size(); i++) {
            PersistedField f = ec.fields.get(i);
            int accessStart=sb.length();
            if(f.discriminator) { sb.append("case ").append(i).append(": return \"").append(escape(ec.discriminatorValue)).append("\";\n");continue; }
            sb.append("    case ").append(i).append(": return ");
            if(f.embeddedParent!=null && f.embeddedParent.length()>0) {
                String parent=f.embeddedParent.substring(0,f.embeddedParent.length()-1);
                sb.append(embeddedNullCondition(parent)).append(" ? null : ");
            }
            if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY) {
                sb.append(pkg).append("Values.storage(e.").append(f.fieldName).append(".get())");
            }
            else emitBackendRead(sb, f);
            sb.append(";\n");
            if(ec.hierarchyRoot!=null) {
                String access=sb.substring(accessStart).replaceAll("(?<![\\w.$])e\\.",java.util.regex.Matcher.quoteReplacement("(("+f.declaringType+")(Object)e)."));sb.setLength(accessStart);
                sb.append(access.replace("return ","if(!((Object)e instanceof "+f.declaringType+")) return null; return "));
            }
        }
        sb.append("    default: throw new IllegalArgumentException(\"Unknown attribute\");\n    }\n  }\n")
          .append("  public void set(").append(ec.binaryName).append(" e, int index, Object value) {\n")
          .append("    try { switch(index) {\n");
        for (int i = 0; i < ec.fields.size(); i++) {
            PersistedField f = ec.fields.get(i);
            int accessStart=sb.length();
            if(f.discriminator) { sb.append("case ").append(i).append(": return;\n");continue; }
            sb.append("    case ").append(i).append(": ");
            if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY) {
                String type=f.kind.elementBinaryName;
                String convert="java.lang.Integer".equals(type)?"asIntObject":"java.lang.Long".equals(type)?"asLongObject":
                    "java.lang.Short".equals(type)?"asShortObject":"java.lang.Byte".equals(type)?"asByteObject":
                    "java.lang.Double".equals(type)?"asDoubleObject":"java.lang.Float".equals(type)?"asFloatObject":
                    "java.lang.Boolean".equals(type)?"asBooleanObject":"java.lang.Character".equals(type)?"asCodeUnitObject":"java.util.Date".equals(type)?"asDate":"asString";
                sb.append("e.").append(f.fieldName).append(".set(").append(pkg).append("Values.").append(convert).append("(value));");
            } else {
                StringBuilder setter = new StringBuilder();
                emitBackendWrite(setter, f);
                sb.append(setter.toString().replace(ORM, pkg));
            }
            sb.append(" return;\n");
            if(ec.hierarchyRoot!=null) {
                String access=sb.substring(accessStart).replaceAll("(?<![\\w.$])e\\.",java.util.regex.Matcher.quoteReplacement("(("+f.declaringType+")(Object)e)."));sb.setLength(accessStart);
                sb.append(access.replace("case "+i+": ","case "+i+": if(!((Object)e instanceof "+f.declaringType+")) return; "));
            }
        }
        sb.append("    default: throw new IllegalArgumentException(\"Unknown attribute\");\n")
          .append("    }} catch(Exception ex) { throw new com.codename1.orm.session.")
          .append("PersistenceException(ex.getMessage(),ex); }\n  }\n");
        sb.append("public Object project(int index,Object value) { try { switch(index) {\n");
        for(int i=0;i<ec.fields.size();i++) {
            PersistedField field=ec.fields.get(i);
            if(field.relation!=null || field.discriminator) continue;
            String expression;
            if(field.kind.kind==PropertyTypeKind.Kind.PROPERTY) {
                expression=storageConversion(field.kind.elementBinaryName,"value");
            } else {
                StringBuilder setter=new StringBuilder();emitBackendWrite(setter,field);
                String assignment=setter.toString();expression=assignment.substring(assignment.indexOf(" = ")+3,assignment.length()-1);
            }
            sb.append("case ").append(i).append(": return ");
            if(field.converter==null) sb.append("value==null?null:");
            sb.append(expression.replace(ORM,pkg)).append(";\n");
        }
        sb.append("default:return super.project(index,value);}} catch(Exception error) { throw new com.codename1.orm.session.PersistenceException(\"Invalid scalar projection\",error);}}\n");
        sb.append("public Object domainValue(").append(ec.binaryName).append(" e,int index) { switch(index) {\n");
        for(int i=0;i<ec.fields.size();i++) {
            PersistedField field=ec.fields.get(i);
            if(field.relation!=null || field.discriminator) continue;
            sb.append("case ").append(i).append(": ");
            if(ec.hierarchyRoot!=null) sb.append("if(!((Object)e instanceof ").append(field.declaringType).append(")) return null; ");
            String access="e."+field.fieldName;
            if(ec.hierarchyRoot!=null) access="(("+field.declaringType+")(Object)e)."+field.fieldName;
            sb.append("return ").append(access).append(field.kind.kind==PropertyTypeKind.Kind.PROPERTY?".get()":"").append(";\n");
        }
        sb.append("default:return get(e,index);}}\n");
        sb.append("  public ").append(pkg).append("Relationship[] relationships() { return new ")
          .append(pkg).append("Relationship[] {\n");
        for (RelationField relation : ec.relations) {
            sb.append("new ").append(pkg).append("Relationship(\"").append(escape(relation.field)).append("\", ")
              .append(relation.target).append(".class, ").append(relation.many).append(", ").append(relation.lazy)
              .append(", ").append(relation.column).append(", ").append('"').append(escape(relation.mappedBy)).append("\", ").append('"')
              .append(escape(relation.table)).append("\", ").append('"').append(escape(relation.ownerColumn)).append("\", ").append('"')
              .append(escape(relation.targetColumn)).append("\", ").append(relation.cascade).append(", ").append(relation.orphan).append(", ").append(relation.unique).append(", \"").append(escape(relation.mapKey)).append("\", \"").append(escape(relation.orderColumn)).append("\", \"").append(escape(relation.orderBy)).append("\", ").append(relation.element).append("),\n");
        }
        sb.append("}; }\n  public Object relation(").append(ec.binaryName).append(" e, int index) { switch(index) {\n");
        for (RelationField relation : ec.relations) {
            sb.append("case ").append(relation.index).append(": return ");
            if(ec.hierarchyRoot!=null) sb.append("!((Object)e instanceof ").append(relation.declaringType).append(") ? null : ((").append(relation.declaringType).append(")(Object)e).");
            else sb.append("e.");
            sb.append(relation.field).append(";\n");
        }
        sb.append("default: throw new IllegalArgumentException(); }}\n  public void relation(").append(ec.binaryName).append(" e, int index, Object value) { switch(index) {\n");
        for (RelationField relation : ec.relations) {
            String type = org.objectweb.asm.Type.getType(relation.descriptor).getClassName();
            sb.append("case ").append(relation.index).append(": ");
            if(ec.hierarchyRoot!=null) sb.append("if(!((Object)e instanceof ").append(relation.declaringType).append(")) return; ((").append(relation.declaringType).append(")(Object)e).");
            else sb.append("e.");
            sb.append(relation.field).append(" = ");
            if ("java.util.Map".equals(type) && relation.element) sb.append("(java.util.Map)value");
            else if ("java.util.Map".equals(type)) sb.append("com.codename1.impl.orm.Models.mapBy((java.util.Collection)value,").append(relation.target).append(".class,\"").append(escape(relation.mapKey)).append("\")");
            else if ("java.util.Set".equals(type)) sb.append("value == null ? null : new java.util.LinkedHashSet((java.util.Collection)value)");
            else sb.append("(").append(type).append(")value");
            sb.append("; return;\n");
        }
        sb.append("default: throw new IllegalArgumentException(); }}\n");
        if(!ec.embedded.isEmpty()) {
            sb.append("  public boolean requiresSession() { return true; }\n")
              .append("  public void read(").append(ec.binaryName).append(" e,Object[] values) {\n");
            for(Map.Entry<String,String> embedded:ec.embedded.entrySet()) {
                String path=embedded.getKey();int lastDot=path.lastIndexOf('.');
                if(lastDot>=0) sb.append("if(!(").append(embeddedNullCondition(path.substring(0,lastDot))).append(")) {\n");
                sb.append("e.").append(path).append(" = (");boolean first=true;
                for(int i=0;i<ec.fields.size();i++) if(ec.fields.get(i).fieldName.startsWith(path+".")) {
                    if(!first) sb.append(" && ");first=false;sb.append("values[").append(i).append("] == null");
                }
                if(first) sb.append("true");
                sb.append(") ? null : new ").append(embedded.getValue()).append("();\n");
                if(lastDot>=0) sb.append("}\n");
            }
            for(int i=0;i<ec.fields.size();i++) {
                PersistedField field=ec.fields.get(i);
                if(field.embeddedParent!=null && field.embeddedParent.length()>0 && !ec.embedded.containsKey(field.embeddedParent.substring(0,field.embeddedParent.length()-1))) continue;
                if(field.embeddedParent!=null && field.embeddedParent.length()>0)
                    sb.append("if(!(").append(embeddedNullCondition(field.embeddedParent.substring(0,field.embeddedParent.length()-1))).append(")) ");
                sb.append("set(e,").append(i).append(",values[").append(i).append("]);\n");
            }
            sb.append("}\n");
        }
        sb.append("public ").append(pkg).append("Index[] indexes() { return new ").append(pkg).append("Index[]{");
        for(AnnotationValues index:ec.indexes) {
            sb.append("new ").append(pkg).append("Index(\"").append(escape(index.getStringOrDefault("name",""))).append("\", ")
                .append(index.getBoolOrDefault("unique",false));
            Object fields=index.get("fields");if(fields instanceof List) for(Object field:(List)fields) sb.append(",\"").append(escape((String)field)).append("\"");
            sb.append("),");
        }
        for(PersistedField field:ec.fields) if(field.unique)
            sb.append("new ").append(pkg).append("Index(\"\",true,\"").append(escape(field.fieldName)).append("\"),");
        sb.append("};}\n");
        sb.append("public String mapping(int index) { switch(index) {\n");
        for(int i=0;i<ec.fields.size();i++) {
            PersistedField field=ec.fields.get(i);
            String mapping=field.converter!=null?"converter:"+field.converter+":"+field.domainType
                :field.kind.kind==PropertyTypeKind.Kind.ENUM?"enum:"+field.kind.binaryName:null;
            if(mapping!=null) sb.append("case ").append(i).append(": return \"").append(escape(mapping)).append("\";\n");
        }
        sb.append("default:return super.mapping(index);}}\n");
        boolean converters=false;for(PersistedField field:ec.fields) if(field.converter!=null) converters=true;
        if(converters) {
            if(ec.embedded.isEmpty()) sb.append("public boolean requiresSession() { return true; }\n");
            sb.append("public Object parameter(int index,Object value) { switch(index) {\n");
            for(int i=0;i<ec.fields.size();i++) {
                PersistedField field=ec.fields.get(i);
                if(field.converter!=null) sb.append("case ").append(i).append(": if(value!=null && !")
                    .append(boxedDomainType(field.domainType)).append(".class.isInstance(value)) throw new IllegalArgumentException(\"Converter input requires ")
                    .append(escape(field.domainType)).append("\"); return ").append(pkg).append("Values.storage(new ").append(field.converter)
                    .append("().toDatabase((").append(field.domainType).append(")value));\n");
            }
            sb.append("default:return super.parameter(index,value);}}\n");
        }
        if(ec.embeddedId!=null) {
            String type=ec.embedded.get(ec.embeddedId);
            sb.append("public Object[] keyValues(Object key) { if(key instanceof ").append(type)
                .append(") { ").append(type).append(" value=(").append(type).append(")key; return super.keyValues(new Object[]{");
            for(int i=0;i<ec.idFields.size();i++) {
                if(i>0) sb.append(',');sb.append("value.").append(ec.idFields.get(i).fieldName.substring(ec.embeddedId.length()+1));
            }
            sb.append("}); } return super.keyValues(key); }\n");
        }
        if(ec.generation!=0) sb.append("public int generation() { return ").append(ec.generation)
          .append("; }\npublic String generator() { return ").append('"').append(escape(ec.generator)).append("\"; }\n");
        if(ec.hierarchyRoot!=null) {
            sb.append("public Class hierarchyRoot() { return ").append(ec.hierarchyRoot).append(".class; }\n")
                .append("public int discriminatorIndex() { return ").append(ec.fields.size()-1).append("; }\n")
                .append("public String discriminatorValue() { return \"").append(escape(ec.discriminatorValue)).append("\"; }\n")
                .append("public String[] discriminatorValues() { return new String[]{");
            for(String value:ec.discriminators) sb.append('"').append(escape(value)).append("\",");
            sb.append("}; }\npublic boolean hasRelationship(").append(ec.binaryName).append(" e,int index) { switch(index) {");
            for(RelationField relation:ec.relations) sb.append("case ").append(relation.index).append(": return (Object)e instanceof ").append(relation.declaringType).append(';');
            sb.append("default:return false;} }\n");
            sb.append("public boolean required(").append(ec.binaryName).append(" e,int index) { switch(index) {");
            for(int i=0;i<ec.fields.size();i++) {
                PersistedField field=ec.fields.get(i);
                if(field.declaredRequired) sb.append("case ").append(i).append(": return (Object)e instanceof ").append(field.declaringType).append(';');
            }
            sb.append("default:return false;} }\n");
        }
        sb.append("public boolean required(int index) { switch(index) {");
        for(int i=0;i<ec.fields.size();i++) if(ec.fields.get(i).declaredRequired) sb.append("case ").append(i).append(": return true;");
        sb.append("default:return false;} }\n");
        if(ec.hierarchyRoot!=null) {
            sb.append("public boolean queryAttribute(int index) { switch(index) {");
            for(int i=0;i<ec.fields.size();i++) if(ec.queryFields.contains(ec.fields.get(i).fieldName)) sb.append("case ").append(i).append(": return true;");
            sb.append("default:return false;} }\n");
            sb.append("public boolean queryRelationship(int index) { switch(index) {");
            for(RelationField relation:ec.relations) if(ec.queryRelations.contains(relation.field)) sb.append("case ").append(relation.index).append(": return true;");
            sb.append("default:return false;} }\n");
        }
        sb.append("public boolean primitive(int index) { switch(index) {");
        for(int i=0;i<ec.fields.size();i++) if(ec.fields.get(i).primitive) sb.append("case ").append(i).append(": return true;");
        sb.append("default:return false;} }\n");
        sb.append("public boolean counter(int index) { switch(index) {");
        for(int i=0;i<ec.fields.size();i++) {
            PersistedField field=ec.fields.get(i);
            if(field.converter==null && field.relation==null
                    && (field.kind.kind==PropertyTypeKind.Kind.INT || field.kind.kind==PropertyTypeKind.Kind.LONG
                        || field.kind.kind==PropertyTypeKind.Kind.PROPERTY && ("java.lang.Integer".equals(field.kind.elementBinaryName)
                            || "java.lang.Long".equals(field.kind.elementBinaryName))))
                sb.append("case ").append(i).append(": return true;");
        }
        sb.append("default:return false;} }\n");
        sb.append("  public void lifecycle(").append(ec.binaryName).append(" e,int event) { switch(event) {\n");
        for (int event=0;event<ec.callbacks.length;event++) if(ec.callbacks[event]!=null)
            sb.append("case ").append(event).append(": e.").append(ec.callbacks[event]).append("(); return;\n");
        sb.append("default: return; }}\n}\n");
        return sb.toString();
    }

    private static String generateDaoSource(EntityClass ec) {
        StringBuilder sb = new StringBuilder(4096);
        if (ec.packageName.length() > 0) {
            sb.append("package ").append(ec.packageName).append(";\n\n");
        }
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(ec.daoSimpleName)
                .append(" implements com.codename1.orm.Dao<").append(ec.binaryName).append("> {\n\n");

        // Public static register() hook -- the bootstrap class calls
        // this once per generated dao at app start; the call triggers
        // this class's <clinit> and installs the dao in EntityManager.
        sb.append("    public static void register() {\n");
        sb.append("        com.codename1.impl.orm.Models.register(new ").append(ec.simpleName)
          .append(ec.daoSimpleName.endsWith(BACKEND_DAO_SUFFIX) ? "Cn1BackendModel" : "Cn1Model").append("());\n");
        sb.append("        com.codename1.impl.orm.DaoRegistry.registerFactory(").append(ec.binaryName)
          .append(".class, new com.codename1.impl.orm.DaoFactory<").append(ec.binaryName).append(">() {\n")
          .append("            public com.codename1.orm.Dao<").append(ec.binaryName).append("> create() { return new ")
          .append(ec.daoSimpleName).append("(); }\n        });\n");
        sb.append("    }\n\n");

        sb.append("    public ").append(ec.daoSimpleName).append("() {\n");
        sb.append("    }\n\n");

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

    /// Reads the compile classpath for `@Entity` classes and processes them as
    /// though they were this module's own.
    ///
    /// Only in the backend flavour. A client module's classpath is the whole
    /// Codename One core and then some, and its entities are its own; a backend
    /// module's is the runtime plus whatever the application shares between its
    /// halves, which is measured in hundreds of classes.
    ///
    /// The dao is generated into THIS module either way. The jar it was read
    /// from is somebody else's build output and is not written to.
    private void scanClasspathEntities(ProcessorContext ctx) throws ProcessingException {
        // THIS MODULE'S OWN OUTPUT IS NOT A DEPENDENCY, and it is on this list:
        // Maven's getCompileClasspathElements puts target/classes first. Scanned
        // here it re-enters processClass with fromThisModule false, which is what
        // turns off the backing-source check -- so a class whose .java was
        // deleted or renamed, deliberately skipped moments earlier as an orphan,
        // is accepted on the second pass as somebody else's entity. The stale dao
        // and bootstrap come back, and a removed table can be recreated, until a
        // mvn clean removes the class file. The module's own classes have already
        // been offered to processClass with the check in place; there is nothing
        // here to find a second time.
        File ownOutput = canonical(ctx.getOutputClassDir());
        for (String element : ctx.getCompileClasspath()) {
            File file = new File(element);
            if (file.isDirectory()) {
                if (ownOutput != null && ownOutput.equals(canonical(file))) {
                    continue;
                }
                scanDirectoryForEntities(file, file, ctx);
            } else if (file.isFile()) {
                scanArchiveForEntities(file, ctx);
            }
        }
    }

    /// Canonical form for comparing two paths, or null when it cannot be taken.
    ///
    /// Compared canonically because the two spellings come from different places
    /// -- the mojo's configured output directory and Maven's classpath list --
    /// and need not match character for character for the same directory.
    private static File canonical(File file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getCanonicalFile();
        } catch (IOException unreadable) {
            return file.getAbsoluteFile();
        }
    }

    private void scanDirectoryForEntities(File root, File dir, ProcessorContext ctx)
            throws ProcessingException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                scanDirectoryForEntities(root, child, ctx);
                continue;
            }
            // THE ENTRY NAME, not the file name. This passed child.getName(),
            // which is "Note.class" and never contains a '/', so not one of
            // skipPackage's prefixes could match and the whole rule was dead on
            // this side -- the same dependency was scanned differently depending
            // on whether Maven resolved it as a directory or as a jar. That is
            // the exact shape of the org/ bug recorded on skipPackage, which was
            // fixed there and left here.
            String entryName = root.toPath().relativize(child.toPath()).toString()
                    .replace(File.separatorChar, '/');
            if (!child.getName().endsWith(".class") || skipPackage(entryName)) {
                continue;
            }
            AnnotatedClass cls;
            try {
                cls = ClassScanner.readClass(child);
            } catch (ProcessingException unreadable) {
                // A class file this cannot parse is not an entity. Said out loud
                // rather than swallowed, because the OTHER reading -- that an
                // entity was skipped -- would be invisible.
                ctx.getLog().debug("cn1: skipping unreadable class " + child + ": "
                        + unreadable.getMessage());
                continue;
            }
            // OUTSIDE the catch: an error while ACCEPTING an entity is the
            // build's business, and catching it here would drop an entity with a
            // message nobody sees.
            consider(cls, ctx);
        }
    }

    private void scanArchiveForEntities(File archive, ProcessorContext ctx)
            throws ProcessingException {
        ZipFile zip;
        try {
            zip = new ZipFile(archive);
        } catch (IOException notAnArchive) {
            // A classpath entry that is not a readable archive -- a missing jar,
            // a resources directory named like one. Not an error here: javac
            // already has an opinion about it.
            ctx.getLog().debug("cn1: not a readable archive, not scanned for entities: "
                    + archive);
            return;
        }
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                // Backend bytecode targets Java 8, so only the base entries of a
                // multi-release JAR are visible to the generated daos.
                if (!name.endsWith(".class") || name.startsWith("META-INF/versions/")
                        || skipPackage(name)) {
                    continue;
                }
                AnnotatedClass cls = readEntry(zip, entry, archive, ctx);
                if (cls != null) {
                    // Outside the read, for the reason the directory scan gives.
                    consider(cls, ctx);
                }
            }
        } finally {
            try {
                zip.close();
            } catch (IOException err) {
                ctx.getLog().debug("cn1: could not close " + archive + ": " + err.getMessage());
            }
        }
    }

    /// One class out of an archive, or null when it cannot be read.
    private static AnnotatedClass readEntry(ZipFile zip, ZipEntry entry, File archive,
                                            ProcessorContext ctx) {
        InputStream in = null;
        try {
            in = zip.getInputStream(entry);
            return ClassScanner.readClass(in, archive);
        } catch (IOException unreadable) {
            ctx.getLog().debug("cn1: skipping unreadable entry " + entry.getName() + " in "
                    + archive + ": " + unreadable.getMessage());
            return null;
        } catch (ProcessingException unreadable) {
            ctx.getLog().debug("cn1: skipping unparseable entry " + entry.getName() + " in "
                    + archive + ": " + unreadable.getMessage());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException err) {
                    ctx.getLog().debug("cn1: could not close " + entry.getName() + ": "
                            + err.getMessage());
                }
            }
        }
    }

    /// Packages no application entity can be in, skipped so a scan does not read
    /// every class of the runtime to learn it has no annotations.
    ///
    /// Deliberately SHORT. An earlier version skipped `org/` and `kotlin/` as
    /// well, which reads as "libraries" and is not: `org.example` is one of the
    /// most ordinary package names an application has, and an entity there,
    /// shipped in a jar, was silently never given a dao -- while the same class
    /// worked when Maven resolved the dependency as a directory, because only
    /// the archive scan consulted this. A few thousand extra class headers cost
    /// milliseconds; a skipped entity costs a server that fails on its first
    /// query.
    ///
    /// What is left cannot hold an application's entity at all: the JDK's own
    /// namespaces, which no application may add a class to.
    ///
    /// `com/codename1/backend/` and `com/codename1/annotations/` USED to be here
    /// as well, on the reasoning that they are the runtime's own. That is the
    /// same reasoning `org/` was removed for: a package belonging to a library
    /// is not a package an application cannot put a class in, and a shared
    /// entity squatting one of them was dropped from a jar while the identical
    /// class was accepted from a reactor directory. The annotation is what
    /// decides whether a class is an entity, so let it decide -- the runtime
    /// declares no `@Entity` of its own for this to pick up, and reading a few
    /// hundred more class headers costs milliseconds against a server that fails
    /// on its first query.
    private static boolean skipPackage(String entryName) {
        return entryName.startsWith("java/") || entryName.startsWith("javax/");
    }

    /// Runs one class found on the classpath through the same acceptance the
    /// module's own classes get. Classes without `@Entity` fall out inside.
    private void consider(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls == null || accepted.containsKey(cls.getBinaryName())) {
            return;
        }
        processClass(cls, ctx, false);
    }

    /// The kind constants on `com.codename1.backend.sql.Dialect`, mirrored here
    /// because the plugin cannot depend on the backend runtime: it GENERATES
    /// against it. The generated source names the constants rather than these
    /// numbers, so a change on that side is a compile error in the generated
    /// code rather than a silently wrong column type -- these are only what the
    /// processor's own validation compares.
    static final int KIND_TEXT = 0;
    static final int KIND_INTEGER = 1;
    static final int KIND_BIGINT = 2;
    static final int KIND_REAL = 3;
    static final int KIND_BLOB = 4;
    static final int KIND_BOOLEAN = 5;
    static final int KIND_TIMESTAMP = 6;

    private static final String ORM = "com.codename1.backend.orm.";

    /// The portable column kind for a field's Java type.
    ///
    /// Note what is NOT here: a date is a `TIMESTAMP` kind, which every engine
    /// stores as epoch milliseconds in an integer column rather than as its own
    /// timestamp type. A native timestamp comes back as text in the server's own
    /// DateStyle and time zone, so the column that looked more correct is the one
    /// that does not round-trip the same way on three engines.
    private static int dialectKind(PropertyTypeKind k) {
        switch (k.kind) {
            case INT: case SHORT: case BYTE:
                return KIND_INTEGER;
            case LONG:
                return KIND_BIGINT;
            case DOUBLE: case FLOAT:
                return KIND_REAL;
            case BOOLEAN:
                return KIND_BOOLEAN;
            case DATE:
                return KIND_TIMESTAMP;
            case BYTE_ARRAY:
                return KIND_BLOB;
            case CHAR:
                // The CODE UNIT, not one-character text. A char field that was
                // never assigned holds '\0', and PostgreSQL refuses a NUL inside
                // a text value -- so a text column made an untouched char field
                // impossible to insert there while it worked on SQLite. See
                // Values.asCodeUnit. The client dao keeps its own TEXT mapping:
                // it talks to a SQLite file, which has neither problem, and its
                // schema is already written.
                return KIND_INTEGER;
            case STRING:
            default:
                return KIND_TEXT;
        }
    }

    /// Whether a DATABASE can generate a key this field can hold.
    ///
    /// Integers, and wide enough ones: `byte` and `short` are integers and were
    /// accepted by an earlier version of this check, but a generated key leaves
    /// their range after 127 and 32,767 rows. The generated setter then NARROWS
    /// what came back -- silently, and to a negative number -- so the entity
    /// carries a key no row has and every later update and delete either misses
    /// or hits a row that belongs to something else. A table reaching 128 rows
    /// is not an edge case.
    private static boolean isGeneratableKey(PropertyTypeKind kind) {
        return kind.kind == PropertyTypeKind.Kind.INT || kind.kind == PropertyTypeKind.Kind.LONG;
    }

    /// The source spelling of a kind, so the generated class references the
    /// constant instead of the number behind it.
    private static String kindConstant(int kind) {
        switch (kind) {
            case KIND_INTEGER: return "com.codename1.backend.sql.Dialect.INTEGER";
            case KIND_BIGINT: return "com.codename1.backend.sql.Dialect.BIGINT";
            case KIND_REAL: return "com.codename1.backend.sql.Dialect.REAL";
            case KIND_BLOB: return "com.codename1.backend.sql.Dialect.BLOB";
            case KIND_BOOLEAN: return "com.codename1.backend.sql.Dialect.BOOLEAN";
            case KIND_TIMESTAMP: return "com.codename1.backend.sql.Dialect.TIMESTAMP";
            default: return "com.codename1.backend.sql.Dialect.TEXT";
        }
    }

    /// Whether a field can hold null, which is what separates `int` from
    /// `Integer` and decides both halves of the generated access.
    /// The longest identifier every engine stores whole.
    ///
    /// SQLite has no limit worth naming, MySQL refuses one over 64 characters,
    /// and PostgreSQL TRUNCATES past 63 bytes rather than refusing -- which is
    /// the worse of the two, because two long names that differ only past the
    /// cut become one table and nothing says so. Measured: a 70-character table
    /// name is accepted by SQLite, accepted-and-truncated by PostgreSQL, and
    /// refused by MySQL with "Identifier name ... is too long".
    ///
    /// So the tightest limit is the limit, checked in BYTES because
    /// PostgreSQL's is a byte count and a non-ASCII name spends more than one
    /// per character.
    static final int MAX_IDENTIFIER_BYTES = 63;

    private static boolean tooLongForAnEngine(String identifier) {
        return identifier != null && utf8Length(identifier) > MAX_IDENTIFIER_BYTES;
    }

    /// UTF-8 length without encoding the string, so no charset has to be named.
    private static int utf8Length(String identifier) {
        int total = 0;
        for (int iter = 0; iter < identifier.length(); iter++) {
            char c = identifier.charAt(iter);
            if (c < 0x80) {
                total += 1;
            } else if (c < 0x800) {
                total += 2;
            } else if (c >= 0xd800 && c <= 0xdbff) {
                // A surrogate PAIR is one code point in four bytes, counted at
                // the high half so the low half adds nothing.
                total += 4;
            } else if (c >= 0xdc00 && c <= 0xdfff) {
                total += 0;
            } else {
                total += 3;
            }
        }
        return total;
    }

    /// Whether the field is a Java PRIMITIVE, which is the set that cannot hold
    /// null and therefore the set whose columns are declared NOT NULL.
    ///
    /// Not `!isBoxed`: that asks whether the binary name contains a dot, which
    /// `byte[]` does not -- so a blob column was declared NOT NULL and every
    /// entity with an unset one failed to insert. The kind is what carries the
    /// answer, and it has to be paired with `boxed` because Integer and int
    /// share a kind.
    private static boolean isJavaPrimitive(PersistedField f) {
        if (f.boxed) {
            return false;
        }
        switch (f.kind.kind) {
            case INT: case LONG: case SHORT: case BYTE:
            case CHAR: case DOUBLE: case FLOAT: case BOOLEAN:
                return true;
            default:
                return false;
        }
    }

    private static boolean isBoxed(PropertyTypeKind k) {
        String binary = k.binaryName;
        if (binary == null) {
            return false;
        }
        return binary.indexOf('.') >= 0;
    }

    /// The server-side dao: an `EntityDefinition` describing the table and
    /// reading and writing the entity's fields by index.
    ///
    /// There is no SQL in here, which is the difference that matters. The
    /// statements are built by the runtime from this description and the
    /// connection's dialect, so ONE generated class serves SQLite, PostgreSQL
    /// and MySQL -- and the development loop against a file and the production
    /// deployment against a server run the same generated code.
    private static String generateBackendDaoSource(EntityClass ec) {
        StringBuilder sb = new StringBuilder(4096);
        if (ec.packageName.length() > 0) {
            sb.append("package ").append(ec.packageName).append(";\n\n");
        }
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(ec.daoSimpleName)
          .append(" extends ").append(ORM).append("EntityDefinition {\n\n");

        sb.append("    private static final ").append(ORM).append("ColumnDefinition[] COLUMNS = {\n");
        for (int i = 0; i < ec.fields.size(); i++) {
            PersistedField f = ec.fields.get(i);
            sb.append("        new ").append(ORM).append("ColumnDefinition(\"")
              .append(escape(f.fieldName)).append("\", \"").append(escape(f.columnName))
              .append("\", ").append(kindConstant(f.dialectKind)).append(", ")
              .append(f.nullable).append(", ");
            if (f.explicitSqlType == null) {
                sb.append("null");
            } else {
                sb.append('"').append(escape(f.explicitSqlType)).append('"');
            }
            sb.append(", ").append(f.isId).append(", ")
              .append(f.isId && f.autoIncrement).append(")")
              .append(i + 1 < ec.fields.size() ? ",\n" : "\n");
        }
        sb.append("    };\n\n");

        // The hook the generated bootstrap calls. Same name and same shape as
        // the client flavour's, so one bootstrap source serves both.
        sb.append("    public static void register() {\n");
        sb.append("        com.codename1.impl.orm.Models.register(new ").append(ec.simpleName)
          .append(ec.daoSimpleName.endsWith(BACKEND_DAO_SUFFIX) ? "Cn1BackendModel" : "Cn1Model").append("());\n");
        sb.append("        ").append(ORM).append("EntityManager.register(new ")
          .append(ec.daoSimpleName).append("());\n");
        sb.append("    }\n\n");

        sb.append("    public ").append(ec.daoSimpleName).append("() {\n    }\n\n");

        sb.append("    public Class type() {\n");
        sb.append("        return ").append(ec.binaryName).append(".class;\n");
        sb.append("    }\n\n");

        sb.append("    public String table() {\n");
        sb.append("        return \"").append(escape(ec.tableName)).append("\";\n");
        sb.append("    }\n\n");

        sb.append("    public ").append(ORM).append("ColumnDefinition[] columns() {\n");
        sb.append("        return COLUMNS;\n");
        sb.append("    }\n\n");

        sb.append("    public Object newInstance() {\n");
        sb.append("        return new ").append(ec.binaryName).append("();\n");
        sb.append("    }\n\n");

        sb.append("    public Object get(Object entity, int index) {\n");
        sb.append("        ").append(ec.binaryName).append(" e = (").append(ec.binaryName)
          .append(")entity;\n");
        sb.append("        switch(index) {\n");
        for (int i = 0; i < ec.fields.size(); i++) {
            sb.append("            case ").append(i).append(": return ");
            emitBackendRead(sb, ec.fields.get(i));
            sb.append(";\n");
        }
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");

        sb.append("    public void set(Object entity, int index, Object value)\n");
        sb.append("            throws java.io.IOException {\n");
        sb.append("        ").append(ec.binaryName).append(" e = (").append(ec.binaryName)
          .append(")entity;\n");
        sb.append("        switch(index) {\n");
        for (int i = 0; i < ec.fields.size(); i++) {
            sb.append("            case ").append(i).append(": ");
            emitBackendWrite(sb, ec.fields.get(i));
            sb.append(" return;\n");
        }
        sb.append("        }\n");
        sb.append("    }\n");

        sb.append("}\n");
        return sb.toString();
    }

    /// A field as a bound parameter: Long, Double, String, byte[] or null,
    /// which is the set `Database` binds and returns on every engine.
    private static void emitBackendRead(StringBuilder sb, PersistedField f) {
        String field = "e." + (f.relation==null?f.fieldName:f.relation.field);
        if (f.relation != null) {
            sb.append("com.codename1.impl.orm.Models.foreignKey(e, ").append(f.relation.index)
              .append(", ").append(field).append(", ").append(f.relationPart).append(")");
            return;
        }
        if(f.converter!=null) {
            sb.append("com.codename1.impl.orm.Values.storage(new ").append(f.converter).append("().toDatabase(").append(field).append("))");return;
        }
        switch (f.kind.kind) {
            case ENUM:
                sb.append(field).append(" == null ? null : ").append(field).append(".name()");
                return;
            case STRING:
            case BYTE_ARRAY:
                sb.append(field);
                return;
            case INT: case LONG: case SHORT: case BYTE:
                if (f.boxed) {
                    sb.append(field).append(" == null ? null : Long.valueOf(")
                      .append(field).append(".longValue())");
                } else {
                    sb.append("Long.valueOf(").append(field).append(")");
                }
                return;
            case DOUBLE: case FLOAT:
                if (f.boxed) {
                    sb.append(field).append(" == null ? null : Double.valueOf(")
                      .append(field).append(".doubleValue())");
                } else {
                    sb.append("Double.valueOf(").append(field).append(")");
                }
                return;
            case BOOLEAN:
                if (f.boxed) {
                    sb.append(field).append(" == null ? null : Long.valueOf(")
                      .append(field).append(".booleanValue() ? 1L : 0L)");
                } else {
                    sb.append("Long.valueOf(").append(field).append(" ? 1L : 0L)");
                }
                return;
            case CHAR:
                if (f.boxed) {
                    sb.append(field).append(" == null ? null : Long.valueOf(")
                      .append(field).append(".charValue())");
                } else {
                    sb.append("Long.valueOf(").append(field).append(")");
                }
                return;
            case DATE:
                sb.append(field).append(" == null ? null : Long.valueOf(")
                  .append(field).append(".getTime())");
                return;
            default:
                // UNREACHABLE by construction: every kind an entity can persist
                // has a case above, and the rest are refused in consider()
                // before they reach here. It throws rather than falling back
                // because the fallback that reads naturally -- bind null, assign
                // nothing -- is silent DATA LOSS: a field kind added to the enum
                // later would be written as NULL and read back as its default,
                // on a build with no error in it. A crash names the field.
                throw new IllegalStateException("no server-side binding for field "
                        + f.fieldName + " of kind " + f.kind.kind);
        }
    }

    /// Deletes the bootstrap left behind when the last `@Entity` goes away.
    ///
    /// Returning early with nothing to generate leaves the bootstrap from the
    /// PREVIOUS run in the output directory, and the generated entry point asks
    /// whether that file exists -- deliberately, because entities can come from
    /// a jar rather than from this module, so the file is the only answer for
    /// them. The stale one then made a server register definitions for entities
    /// that are gone: it reopens the datasource, recreates tables the developer
    /// removed, or fails against fields that no longer exist, and only
    /// `mvn clean` explains it.
    ///
    /// Only OUR output is removed, by the same marker the collision check reads.
    /// A bootstrap written before that marker existed is left alone, which is
    /// the behaviour this replaces and costs one clean.
    ///
    /// The generated DAOS are deliberately left: nothing references one once the
    /// bootstrap that named it is gone, and the translator drops a class nothing
    /// references. It is the bootstrap that gets INVOKED, so it is the one whose
    /// staleness is visible at runtime.
    ///
    /// Backend flavour only. The client bootstrap has the same shape and has
    /// shipped that way; changing it is a decision to make on its own.
    private void removeAStaleBootstrap(ProcessorContext ctx) {
        if (!backend) {
            return;
        }
        AnnotatedClass existing = ctx.lookup(BACKEND_BOOTSTRAP_BINARY.replace('.', '/'));
        if (existing == null || !existing.getClassAnnotations().containsKey(GENERATED_DESC)) {
            return;
        }
        File stale = new File(ctx.getOutputClassDir(),
                BACKEND_BOOTSTRAP_BINARY.replace('.', File.separatorChar) + ".class");
        if (stale.isFile() && stale.delete()) {
            ctx.getLog().info("cn1: removed " + BACKEND_BOOTSTRAP_BINARY
                    + ", which this module no longer has any @Entity for");
        }
    }

    /// Refuses to generate over a class the project already has.
    ///
    /// What is compiled here lands in the same output directory, so a name that
    /// already exists is simply overwritten -- silently, because what is
    /// generated compiles perfectly well and javac never sees the two as
    /// duplicates: the existing one is a classpath class, not a second source.
    ///
    /// The name being taken is not enough to refuse. An incremental build runs
    /// process-classes again without a clean and finds the dao this processor
    /// wrote on the first pass, so an unconditional lookup reports our own
    /// output as a collision and every second build fails. The generated
    /// sources carry `@Generated` for exactly this reason; a real user-defined
    /// collision has no marker and is still refused. The daos have carried the
    /// marker since they were first generated, so no existing output is read as
    /// a collision by this. The same shape as
    /// RestServerAnnotationProcessor and RestControllerAnnotationProcessor,
    /// which learned it the same way.
    ///
    /// Backend only, deliberately: the client flavour has shipped without this
    /// check and adding it there would turn a working build into a failing one
    /// for anyone who already collided, which is a change to make on its own.
    private boolean wouldReplaceAnExistingClass(String binaryName, String what,
            ProcessorContext ctx) {
        AnnotatedClass existing = ctx.lookup(binaryName.replace('.', '/'));
        if (existing == null) {
            return false;
        }
        if (existing.getClassAnnotations().containsKey(GENERATED_DESC)) {
            return false;
        }
        ctx.error(binaryName + " already exists, and the " + what + " generated here would "
                + "replace it. Rename that class, or rename the entity the name is "
                + "derived from.");
        return true;
    }

    /// A primitive field's column value, refused when it is SQL NULL.
    ///
    /// The conversions take a fallback and would answer it, so a null column
    /// read into an int gave the field 0 -- indistinguishable from a row that
    /// really holds 0. The tables this ORM creates declare such a column NOT
    /// NULL, so this is what answers for the ones it did not create.
    private static String required(PersistedField f) {
        return ORM + "Values.required(value, \"" + f.fieldName + "\")";
    }

    /// A column value into a field, through the tolerant conversions in
    /// `Values`: the same column is a Long from one engine and exact text from
    /// another, and neither is the field's type.
    private static void emitBackendWrite(StringBuilder sb, PersistedField f) {
        if(f.converter!=null) {
            String conversion=storageConversion(f.kind.binaryName,"value");
            sb.append("e.").append(f.fieldName).append(" = new ").append(f.converter).append("().fromDatabase(").append(conversion).append(");");return;
        }
        String field = "e." + (f.relation==null?f.fieldName:f.relation.field);
        String values = ORM + "Values.";
        if (f.relation != null) return;
        switch (f.kind.kind) {
            case ENUM:
                sb.append(field).append(" = value == null ? null : ").append(f.kind.binaryName).append(".valueOf(").append(values).append("asString(value));");
                return;
            case STRING:
                sb.append(field).append(" = ").append(values).append("asString(value);");
                return;
            case INT:
                sb.append(field).append(" = ").append(values)
                  .append(f.boxed ? "asIntObject(value);" : "asInt(" + required(f) + ", 0);");
                return;
            case LONG:
                sb.append(field).append(" = ").append(values)
                  .append(f.boxed ? "asLongObject(value);" : "asLong(" + required(f) + ", 0L);");
                return;
            case SHORT:
                sb.append(field).append(" = ").append(values)
                  .append(f.boxed ? "asShortObject(value);" : "asShort(" + required(f) + ", (short)0);");
                return;
            case BYTE:
                sb.append(field).append(" = ").append(values)
                  .append(f.boxed ? "asByteObject(value);" : "asByte(" + required(f) + ", (byte)0);");
                return;
            case DOUBLE:
                sb.append(field).append(" = ").append(values)
                  .append(f.boxed ? "asDoubleObject(value);" : "asDouble(" + required(f) + ", 0);");
                return;
            case FLOAT:
                sb.append(field).append(" = ").append(values)
                  .append(f.boxed ? "asFloatObject(value);" : "asFloat(" + required(f) + ", 0);");
                return;
            case BOOLEAN:
                sb.append(field).append(" = ").append(values)
                  .append(f.boxed ? "asBooleanObject(value);" : "asBoolean(" + required(f) + ", false);");
                return;
            case CHAR:
                // asCodeUnit, not asString: reading the boxed form through
                // asString kept the first character of whatever a blob happened
                // to decode to, while the primitive char field beside it refused
                // the same row. Both read the integer the column holds.
                if (f.boxed) {
                    sb.append(field).append(" = ").append(values).append("asCodeUnitObject(value);");
                } else {
                    sb.append(field).append(" = ").append(values)
                      .append("asCodeUnit(").append(required(f)).append(", '\\0');");
                }
                return;
            case DATE:
                sb.append(field).append(" = ").append(values).append("asDate(value);");
                return;
            case BYTE_ARRAY:
                sb.append(field).append(" = ").append(values).append("asBytes(value);");
                return;
            default:
                // The other half of the same rule; see emitBackendRead.
                throw new IllegalStateException("no server-side read for field "
                        + f.fieldName + " of kind " + f.kind.kind);
        }
    }

    private static String generateBootstrapSource(Iterable<EntityClass> classes, boolean backend) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(BOOTSTRAP_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("///\n");
        if (backend) {
            sb.append("/// Server-side dao bootstrap. The generated entry point\n");
            sb.append("/// constructs this before it starts listening; a hand-written\n");
            sb.append("/// main writes `new cn1app.BackendDaoBootstrap();` itself.\n");
            sb.append("///\n");
            sb.append("/// It exists because nothing may look a dao up by name: the\n");
            sb.append("/// translator drops a class nothing references, so the direct\n");
            sb.append("/// references below are what keep the generated daos alive.\n");
        } else {
            sb.append("/// SQLite dao bootstrap. The iOS / Android per-build application\n");
            sb.append("/// stub instantiates this class before Display.init (the build\n");
            sb.append("/// server probes the project zip for it and emits the install line\n");
            sb.append("/// conditionally); JavaSEPort.postInit picks it up via\n");
            sb.append("/// Class.forName for the simulator and desktop runs.\n");
        }
        sb.append("@SuppressWarnings({\"all\"})\n");
        String simple = backend ? BACKEND_BOOTSTRAP_SIMPLE : BOOTSTRAP_SIMPLE;
        if (backend) {
            // The collision check reads this: without it the bootstrap written
            // on the first pass looks like an application class on the second.
            sb.append("@com.codename1.backend.annotations.Generated\n");
        }
        sb.append("public final class ").append(simple).append(" {\n");
        sb.append("    public ").append(simple).append("() {\n");
        for (EntityClass ec : classes) {
            sb.append("        ").append(ec.daoBinaryName).append(".register();\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String packageOf(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? "" : binary.substring(0, dot);
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
        if(f.converter!=null) {
            sb.append("com.codename1.impl.orm.Values.storage(new ").append(f.converter).append("().toDatabase(").append(inst).append('.').append(f.fieldName).append("))");return;
        }
        if (f.relation != null) {
            sb.append("com.codename1.impl.orm.Models.foreignKey(").append(inst).append(", ")
              .append(f.relation.index).append(", ").append(inst).append('.').append(f.relation.field).append(", ").append(f.relationPart).append(")");
            return;
        }
        switch (f.kind.kind) {
            case ENUM:
                sb.append(inst).append('.').append(f.fieldName).append(" == null ? null : ")
                  .append(inst).append('.').append(f.fieldName).append(".name()");
                return;
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
        if(f.converter!=null) {
            String read=row+(f.kind.kind==PropertyTypeKind.Kind.BYTE_ARRAY?".getBlob(":".getString(")+idx+")";
            sb.append(inst).append('.').append(f.fieldName).append(" = new ").append(f.converter).append("().fromDatabase(").append(storageConversion(f.kind.binaryName,read)).append(");\n");return;
        }
        if (f.relation != null) return;
        switch (f.kind.kind) {
            case ENUM:
                sb.append("                String _enum = ").append(row).append(".getString(").append(idx).append(");\n")
                  .append("                ").append(inst).append('.').append(f.fieldName).append(" = _enum == null ? null : ")
                  .append(f.kind.binaryName).append(".valueOf(_enum);\n");
                return;
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
        } else if ("java.lang.Character".equals(elem)) {
            sb.append(inst).append('.').append(f.fieldName).append(".set(com.codename1.impl.orm.Values.asCodeUnitObject(").append(row).append(".getString(").append(idx).append(")));\n");
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

    static final class FieldPath {
        final FieldInfo field;final String path,prefix,declaringType;
        FieldPath(FieldInfo field,String path,String prefix,String declaringType) { this.field=field;this.path=path;this.prefix=prefix;this.declaringType=declaringType; }
    }

    static final class RelationField {
        String field, descriptor, target, mappedBy, columnName, table, ownerColumn, targetColumn,kind,declaringType;
        String mapKey="",orderColumn="",orderBy="",mapKeyType="";
        boolean many,lazy,orphan,nullable,unique,element;
        int cascade,index,column=-1;
    }

    static final class EntityClass {
        String binaryName;
        String parent,hierarchyRoot,discriminatorValue;
        boolean abstractClass;
        final List<String> discriminators=new ArrayList<String>();
        final Set<String> queryFields=new LinkedHashSet<String>(),queryRelations=new LinkedHashSet<String>();
        String packageName;
        String simpleName;
        String daoBinaryName;
        String daoSimpleName;
        String tableName;
        int generation;
        String generator;
        PersistedField idField;
        String embeddedId;
        final List<PersistedField> idFields=new ArrayList<PersistedField>();
        final Map<String,String> embedded=new LinkedHashMap<String,String>();
        final String[] callbacks=new String[7];
        final List<AnnotationValues> indexes=new ArrayList<AnnotationValues>();
        final List<RelationField> relations = new ArrayList<RelationField>();
        final List<PersistedField> fields = new ArrayList<PersistedField>();
    }

    static final class PersistedField {
        String fieldName;
        String embeddedParent;
        String converter,domainType,declaringType;
        boolean discriminator,primitive;
        String javaType;
        String columnName;
        String sqlType;
        boolean nullable;
        boolean declaredRequired;
        boolean isId;
        boolean autoIncrement;
        boolean version;
        boolean unique;
        RelationField relation;
        int relationPart;
        PropertyTypeKind kind;
        /// @Column(type) as the developer wrote it, or null when absent.
        String explicitSqlType;
        /// The portable column kind, as one of the constants on
        /// `com.codename1.backend.sql.Dialect`. Backend flavour only.
        int dialectKind;
        /// Whether the field's type is a boxed reference rather than a
        /// primitive, which is what decides whether it can hold null.
        boolean boxed;
    }
}
