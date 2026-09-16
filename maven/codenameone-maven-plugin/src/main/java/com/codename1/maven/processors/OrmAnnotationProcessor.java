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

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
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
        if (cls.isAbstract() || cls.isInterface()) {
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
        if (!hasPublicNoArgConstructor(cls)) {
            ctx.error(cls, "@Entity class " + cls.getBinaryName()
                    + " must declare a public no-arg constructor");
            return;
        }

        EntityClass ec = new EntityClass();
        ec.binaryName = cls.getBinaryName();
        ec.simpleName = simpleName(cls.getBinaryName());
        ec.packageName = packageOf(cls.getBinaryName());
        ec.daoSimpleName = ec.simpleName + (backend ? BACKEND_DAO_SUFFIX : "Cn1Dao");
        ec.daoBinaryName = (ec.packageName.length() == 0)
                ? ec.daoSimpleName
                : ec.packageName + "." + ec.daoSimpleName;
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
            // The EXPLICIT type, separately: the client flavour needs a type for
            // every column and defaults it to SQLite's, while the backend one
            // needs to know whether the developer named one at all -- a column
            // with no @Column(type) is typed by the dialect, which is what keeps
            // the entity portable.
            pf.explicitSqlType = (colType == null || colType.length() == 0) ? null : colType;
            pf.nullable = nullable;

            pf.dialectKind = dialectKind(pf.kind);
            pf.boxed = isBoxed(pf.kind);
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

            AnnotationValues idAnn = f.getAnnotation(ID_DESC);
            if (idAnn != null) {
                pf.isId = true;
                pf.autoIncrement = idAnn.getBoolOrDefault("autoIncrement", true);
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
        if (backend) {
            // BEFORE the emptiness check, because the entities need not be in
            // this module at all. An entity is the one class both halves of an
            // application own, so the natural place for it is a module the app
            // and the server both depend on -- and then the backend module's own
            // compiled classes hold none of them.
            scanClasspathEntities(ctx);
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
                    backend ? generateBackendDaoSource(ec) : generateDaoSource(ec));
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
        sb.append("        com.codename1.orm.EntityManager.registerDao(new ").append(ec.daoSimpleName).append("());\n");
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
        for (String element : ctx.getCompileClasspath()) {
            File file = new File(element);
            if (file.isDirectory()) {
                scanDirectoryForEntities(file, file, ctx);
            } else if (file.isFile()) {
                scanArchiveForEntities(file, ctx);
            }
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
                if (!name.endsWith(".class") || skipPackage(name)) {
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
        String field = "e." + f.fieldName;
        switch (f.kind.kind) {
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
        String field = "e." + f.fieldName;
        String values = ORM + "Values.";
        switch (f.kind.kind) {
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
        String packageName;
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
