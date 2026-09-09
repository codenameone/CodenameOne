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
package com.codename1.doclet.hugo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * Renders the Codename One API as Hugo content instead of as a standalone HTML
 * site.
 *
 * <p>The website used to embed the standard doclet's output by copying the whole
 * generated tree into {@code static/}, prefixing every selector of javadoc's
 * stylesheet with a scoping class, and fetching deep pages into a {@code div}
 * with a script that faked {@code window.pathtoroot} and re-enabled the search
 * box javadoc had disabled. Themed pages fought a stylesheet that was never
 * built to be themed, so dark mode broke.
 *
 * <p>This doclet emits a page per type as a Hugo content file whose front matter
 * is the API model and whose prose is markdown. The site's own templates render
 * it, so the API pages are the same pages as the rest of the site -- same theme,
 * same dark mode, same typography, same search. The standard doclet still runs
 * alongside this one to produce the downloadable zip.
 *
 * <p>The generated URLs match the standard doclet's exactly, fragments included;
 * see {@link Refs}.
 */
public final class HugoDoclet implements Doclet {

    private Reporter reporter;
    private Path contentRoot;
    private Path searchIndex;

    private DocletEnvironment environment;
    private Elements elements;
    private Types types;
    private Refs refs;
    private DocReader docReader;
    private TypeNames typeNames;

    /** Every type we publish a page for, by qualified name. */
    private final Map<String, TypeElement> documented = new LinkedHashMap<>();
    /** Direct subtypes, keyed by the qualified name of the supertype. */
    private final Map<String, List<TypeElement>> subtypes = new LinkedHashMap<>();
    /** Rows for the search index. */
    private final List<Map<String, Object>> searchRows = new ArrayList<>();

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return "HugoDoclet";
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(
                new SimpleOption("-d", "<dir>", "Hugo content directory to generate into",
                        value -> contentRoot = Path.of(value)),
                new SimpleOption("--search-index", "<file>", "JSON search index to write",
                        value -> searchIndex = Path.of(value)));
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        if (contentRoot == null) {
            reporter.print(javax.tools.Diagnostic.Kind.ERROR, "-d is required");
            return false;
        }
        this.environment = environment;
        this.elements = environment.getElementUtils();
        this.types = environment.getTypeUtils();
        this.refs = new Refs(types);

        CommentRenderer.Links links = this::urlOf;
        this.typeNames = new TypeNames(links);
        this.docReader = new DocReader(environment.getDocTrees(), elements, types,
                new CommentRenderer(environment.getDocTrees(), links));

        try {
            index(environment);
            for (TypeElement type : documented.values()) {
                writeTypePage(type);
            }
            writePackagePages();
            writeOverview();
            writeSearchIndex();
        } catch (IOException failure) {
            reporter.print(javax.tools.Diagnostic.Kind.ERROR,
                    "failed to write Hugo content: " + failure + "\n" + stackTrace(failure));
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- indexing

    /**
     * Collects the types that get a page, before anything is rendered.
     *
     * <p>Link resolution needs the whole set up front: a comment on the first type
     * processed can reference the last, and a reference to a type we do not
     * publish has to render as text rather than as a link into a 404.
     */
    private void index(DocletEnvironment environment) {
        for (Element element : environment.getIncludedElements()) {
            if (element instanceof TypeElement type) {
                collectType(type);
            }
        }
        for (TypeElement type : documented.values()) {
            recordSubtypeEdges(type);
        }
    }

    /**
     * Records this type under its nearest published ancestors.
     *
     * <p>Stopping at a direct parent that happens to have no page breaks the
     * graph: the package private AbstractVisionAnalyzer sits between
     * VisionAnalyzer and all seven of its public implementations, and every one
     * of them was invisible from the interface's page. An unpublished type is
     * walked through rather than treated as the end of the line.
     */
    private void recordSubtypeEdges(TypeElement type) {
        // directSupertypes() hands an interface java.lang.Object, which the
        // language does not, so seeding from it made every interface a known
        // subtype of Object: the Object page listed 1406 of them, AdCallback and
        // OnUserEarnedRewardListener included. allSupertypes() already skips it.
        Deque<TypeMirror> queue = new ArrayDeque<>(realSupertypes(type.asType()));
        Set<String> visited = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            TypeMirror supertype = queue.removeFirst();
            if (!(supertype instanceof DeclaredType declared)
                    || !(declared.asElement() instanceof TypeElement parent)
                    || !visited.add(parent.getQualifiedName().toString())) {
                continue;
            }
            if (documented.containsKey(parent.getQualifiedName().toString())) {
                subtypes.computeIfAbsent(parent.getQualifiedName().toString(),
                        key -> new ArrayList<>()).add(type);
            } else {
                // Filtered at every hop, not only the first. HTMLCallback reaches
                // Object through the package private CSSParserCallback, so
                // filtering the seed alone still left it a subtype of Object.
                queue.addAll(realSupertypes(supertype));
            }
        }
    }

    /**
     * The supertypes a type actually has.
     *
     * <p>{@code directSupertypes()} hands an interface {@code java.lang.Object},
     * which the language does not: an interface extends only its
     * superinterfaces. Seeding the subtype graph from that made every interface
     * a known subtype of Object, and the Object page listed 1406 of them.
     *
     * <p>Filtered at every hop rather than only at the seed, because an
     * interface reaches Object through an unpublished one it is walked through:
     * com.codename1.ui.html.HTMLCallback survived the first attempt that way.
     *
     * <p>There is no unit test for this. java.lang.Object comes from
     * Ports/CLDC11 and cannot be part of the doclet's own fixture, so the edge
     * it is about is never recorded there and any assertion passes with the fix
     * removed. The evidence is the full run: Object's known subtypes went from
     * 1406 to 1089, and the number of them that are interfaces from 314 to 0.
     */
    private List<TypeMirror> realSupertypes(TypeMirror type) {
        boolean isInterface = type instanceof DeclaredType declared
                && isInterfaceLike(declared.asElement());
        // An annotation type has no supertypes worth publishing. It implicitly
        // extends java.lang.annotation.Annotation, and the standard reference
        // says so nowhere: no superinterface line, no inherited members, no
        // mention of Annotation at all on the page. Reporting it put four
        // methods under "Inherited methods" that nobody calls that way.
        if (type instanceof DeclaredType annotation
                && annotation.asElement().getKind() == ElementKind.ANNOTATION_TYPE) {
            return List.of();
        }
        List<TypeMirror> out = new ArrayList<>();
        for (TypeMirror supertype : types.directSupertypes(type)) {
            if (isInterface && supertype instanceof DeclaredType declared
                    && declared.asElement() instanceof TypeElement element
                    && element.getQualifiedName().contentEquals("java.lang.Object")) {
                continue;
            }
            out.add(supertype);
        }
        return out;
    }

    /**
     * Whether a type is an interface in the language's sense.
     *
     * <p>ANNOTATION_TYPE is its own ElementKind and is not INTERFACE, so a bare
     * comparison against INTERFACE quietly excludes every annotation: they were
     * still listed as known subtypes of Object, 78 of them, and their pages
     * claimed to inherit clone(), wait() and the rest. An annotation interface
     * inherits none of that.
     */
    private static boolean isInterfaceLike(Element element) {
        return element.getKind() == ElementKind.INTERFACE
                || element.getKind() == ElementKind.ANNOTATION_TYPE;
    }

    private void collectType(TypeElement type) {
        if (isHidden(type) || !isVisible(type)) {
            return;
        }
        documented.put(type.getQualifiedName().toString(), type);
        for (TypeElement nested : ElementFilter.typesIn(type.getEnclosedElements())) {
            collectType(nested);
        }
    }

    /**
     * Whether an element carries {@code @hidden}, which removes it from the API
     * entirely.
     *
     * <p>Reads the block tags directly rather than going through
     * {@link DocReader}. Reading an element there renders its whole comment,
     * which resolves the links in it -- and this runs during indexing, while the
     * set of published types is still being filled in. The rendered text is then
     * cached, so a reference to a type indexed later was frozen as an unlinked
     * code span: com.codename1.annotations.AppIntent linked [IntentEntity] and
     * not [IntentParam], purely because of the order the two were reached.
     */
    private boolean isHidden(Element element) {
        return isHiddenByTag(element);
    }

    /**
     * The same question asked straight off the comment's block tags.
     *
     * <p>{@link #urlOf} cannot go through {@link DocReader}: reading an element
     * renders its comment, rendering resolves the links in it, and resolving a
     * link calls back here. Two elements referring to each other is enough to
     * recurse until the stack ends, and the whole generation dies with a
     * StackOverflowError rather than a message anyone could act on. This reads
     * the tags and renders nothing.
     */
    private boolean isHiddenByTag(Element element) {
        com.sun.source.doctree.DocCommentTree comment =
                environment.getDocTrees().getDocCommentTree(element);
        if (comment == null) {
            return false;
        }
        for (com.sun.source.doctree.DocTree tag : comment.getBlockTags()) {
            if (tag.getKind() == com.sun.source.doctree.DocTree.Kind.HIDDEN) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether an element is part of the published API.
     *
     * <p>The generator runs javadoc with {@code -protected}, so javadoc has already
     * filtered the source set; this is the belt to that braces, and it also keeps
     * package private nested types out of an otherwise public type's page.
     */
    private static boolean isVisible(Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC)
                || element.getModifiers().contains(Modifier.PROTECTED);
    }

    /** The page URL of an element, or null when it is not something we publish. */
    private String urlOf(Element target) {
        if (target == null) {
            return null;
        }
        if (target instanceof PackageElement pkg) {
            return environment.isIncluded(pkg) ? Refs.packageUrl(pkg) : null;
        }
        if (target instanceof TypeElement type) {
            return documented.containsKey(type.getQualifiedName().toString())
                    ? Refs.typeUrl(type) : null;
        }
        TypeElement owner = Refs.enclosingType(target);
        if (owner == null || !documented.containsKey(owner.getQualifiedName().toString())) {
            return null;
        }
        // A reference can resolve to a member the page does not render, and a
        // link to an anchor that was never written is a link that goes nowhere.
        // The generator runs javadoc with -protected, so a {@link #position}
        // landing on a private field -- com.codename1.maps.MarkerOptions has one
        // beside a public method of the same name -- produced exactly that.
        if (!isVisible(target) || isHiddenByTag(target)) {
            return null;
        }
        List<String> anchors = refs.anchors(target);
        return Refs.typeUrl(owner) + "#" + anchors.get(0);
    }

    // ------------------------------------------------------------ type pages

    private void writeTypePage(TypeElement type) throws IOException {
        ElementDoc doc = docReader.read(type);
        Map<String, Object> api = new LinkedHashMap<>();

        api.put("kind", kindOf(type));
        api.put("qualified", type.getQualifiedName().toString());
        api.put("simple", Refs.nestedDisplayName(type));
        // "public final enum E" and "public abstract annotation A" are not
        // declarations Java would accept: final is implicit on an enum and
        // abstract on an annotation, and the keyword is @interface.
        api.put("modifiers", TypeNames.declarationModifiers(type));
        api.put("keyword", keywordOf(type));
        // @Target and @Retention define how an annotation may be used at all, and
        // AppIntent published neither. Only annotations that ask to be documented
        // are shown, which is the rule javadoc follows.
        api.put("annotations", documentedAnnotations(type));
        api.put("typeParameters", typeNames.typeParameters(type.getTypeParameters()));

        PackageElement pkg = Refs.packageOf(type);
        api.put("package", Map.of(
                "name", pkg.getQualifiedName().toString(),
                "url", Refs.packageUrl(pkg)));

        api.put("inheritance", inheritanceChain(type));
        api.put("interfaces", interfacesOf(type));
        api.put("subclasses", subclassesOf(type));

        api.put("deprecated", doc.deprecated);
        api.put("deprecatedText", doc.deprecatedText);
        api.put("description", doc.description);
        api.put("warnings", List.copyOf(doc.warnings));
        api.put("seeAlso", seeAlsoRefs(doc, type));
        // Type parameter documentation has nowhere to sit in the declaration
        // string, so it was being read and then dropped. Exactly one tag in the
        // tree carries any text today (VisionCameraView<T>); the other two are
        // empty. Kept anyway, because silently discarding what an author wrote is
        // the defect, not the size of it.
        api.put("typeParameterDocs", typeParameterDocs(doc));

        // javadoc documents a package private supertype's members on the visible
        // subclass rather than dropping them, because the supertype has no page
        // to link to. com.codename1.ads.InterstitialAd is the case here: it
        // declares nothing itself and inherits everything from the package
        // private AbstractFullScreenAd, so treating those as merely "inherited"
        // left seven documented methods with no page anywhere on the site.
        List<TypeElement> hidden = undocumentedSupertypes(type);
        List<Element> owned = new ArrayList<>(type.getEnclosedElements());
        // A member is only promoted if no published ancestor already supplies it.
        // A package private interface can sit behind a documented class that
        // implements it, and then every descendant was being handed the
        // interface's abstract methods as its own: GameSceneView declared
        // "public abstract setup(GraphicsDevice)" though it is concrete and
        // inherits GameView's implementation, which is what the standard page
        // lists.
        Set<String> suppliedByDocumented = signaturesFromDocumentedSupertypes(type);
        for (TypeElement supertype : hidden) {
            for (Element member : supertype.getEnclosedElements()) {
                // Only an ABSTRACT one is dropped. An abstract method promoted
                // off an unpublished interface shows a concrete class declaring
                // something it does not declare -- GameSceneView read
                // "public abstract setup(GraphicsDevice)" while inheriting
                // GameView's implementation. A concrete one is the
                // implementation and has to stay: CompoundAnimation is
                // unpublished and overrides flush(), so UIMutation would lose the
                // method that actually runs.
                if (member instanceof ExecutableElement method
                        && method.getModifiers().contains(Modifier.ABSTRACT)
                        && suppliedByDocumented.contains(signatureKey(method))) {
                    continue;
                }
                owned.add(member);
            }
        }

        api.put("nested", nestedRows(type));
        // An enum constant is a field in the model and not one on the page: the
        // standard reference gives it its own summary and detail sections and no
        // field summary at all, where this rendered "public static final
        // BindAttr TEXT" under Fields.
        List<VariableElement> enumConstants = new ArrayList<>();
        List<VariableElement> plainFields = new ArrayList<>();
        for (VariableElement field : ElementFilter.fieldsIn(owned)) {
            (field.getKind() == ElementKind.ENUM_CONSTANT ? enumConstants : plainFields).add(field);
        }
        api.put("enumConstants", memberRows(enumConstants, type));
        api.put("fields", memberRows(plainFields, type));
        api.put("constructors", executableRows(
                ElementFilter.constructorsIn(type.getEnclosedElements()), type));
        api.put("methods", executableRows(dedupeBySignature(ElementFilter.methodsIn(owned)), type));
        api.put("inherited", inheritedMembers(type, hidden));
        // javadoc renders "Fields inherited from class X" beside the methods
        // block. Without it a subtype such as Label showed none of Component's
        // constants -- CENTER, TOP, the cursor values -- even though this page
        // links references to them.
        api.put("inheritedFields", inheritedFields(type, hidden));
        // A public nested type is addressable through a subtype, and javadoc
        // lists it: Dialog omitted Form.TabIterator entirely.
        api.put("inheritedNested", inheritedNested(type, hidden));

        Map<String, Object> frontMatter = new LinkedHashMap<>();
        frontMatter.put("title", Refs.nestedDisplayName(type));
        frontMatter.put("url", Refs.typeUrl(type));
        frontMatter.put("description", TypeNames.summary(doc.description));
        frontMatter.put("layout", "type");
        // No alias: the .html spelling redirects here on its own, and the
        // directory spelling IS this page now.
        frontMatter.put("aliases", List.of());
        frontMatter.put("javadoc", api);

        write(contentRoot.resolve(Refs.typeContentPath(type)), Json.write(frontMatter));
        addSearchRows(type, doc, owned);
    }

/**
     * Every supertype we publish no page for, anywhere in the hierarchy.
     *
     * <p>The walk continues through documented supertypes rather than stopping
     * at them, because an undocumented ancestor can sit behind a documented one:
     * the constants of the package private {@code CSSParserCallback} are
     * inherited by the public {@code HTMLCallback} and again by
     * {@code DefaultHTMLCallback}, and javadoc documents them on both, since
     * there is no page anywhere in the chain for an "inherited from" link to
     * point at. Only undocumented types are collected; a documented one is
     * walked through and then listed as inherited.
     */
    private List<TypeElement> undocumentedSupertypes(TypeElement type) {
        List<TypeElement> out = new ArrayList<>();
        collectUndocumented(type.asType(), new LinkedHashSet<>(), out);
        return out;
    }

    private void collectUndocumented(TypeMirror type, Set<String> visited, List<TypeElement> out) {
        // Through the same filter as every other walk, so an interface cannot
        // promote Object's members and an annotation promotes nothing at all.
        for (TypeMirror supertype : realSupertypes(type)) {
            if (!(supertype instanceof DeclaredType declared)
                    || !(declared.asElement() instanceof TypeElement element)
                    || !visited.add(element.getQualifiedName().toString())) {
                continue;
            }
            if (!documented.containsKey(element.getQualifiedName().toString())) {
                out.add(element);
            }
            collectUndocumented(supertype, visited, out);
        }
    }

    /**
     * The method signatures a published ancestor of this type already provides.
     *
     * <p>Those are inherited, and the page says so in its inherited block. What
     * they must not be is promoted a second time as the type's own members off
     * some unpublished interface further up.
     */
    private Set<String> signaturesFromDocumentedSupertypes(TypeElement type) {
        Set<String> out = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        for (TypeMirror supertype : allSupertypes(type.asType(), visited)) {
            if (!(supertype instanceof DeclaredType declared)
                    || !(declared.asElement() instanceof TypeElement element)
                    || !documented.containsKey(element.getQualifiedName().toString())) {
                continue;
            }
            // Only a published CLASS actually supplies the member. A published
            // interface merely declares it, and the implementation can still be
            // on the unpublished class in between: the vision analysers get
            // process() from the package private AbstractVisionAnalyzer while
            // VisionAnalyzer only names it, and suppressing on the interface
            // took the method off all seven pages.
            if (isInterfaceLike(element)) {
                continue;
            }
            for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
                // And only a CONCRETE one supplies anything. ComponentAnimation
                // declares flush() abstract and the package private
                // CompoundAnimation implements it, so UIMutation has to keep the
                // promoted implementation: an abstract declaration higher up is
                // not something a reader can call.
                if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                    continue;
                }
                out.add(signatureKey(method));
            }
        }
        return out;
    }

    /** Keeps the first declaration of each signature, so an override wins over what it overrides. */
    private List<ExecutableElement> dedupeBySignature(List<ExecutableElement> methods) {
        Set<String> seen = new LinkedHashSet<>();
        List<ExecutableElement> out = new ArrayList<>();
        for (ExecutableElement method : methods) {
            if (seen.add(signatureKey(method))) {
                out.add(method);
            }
        }
        return out;
    }

    /** The documented type parameters, as {@code <T>} entries lifted by the reader. */
    private List<Map<String, Object>> typeParameterDocs(ElementDoc doc) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (MarkdownSections.NamedText parameter : doc.parameters) {
            if (!parameter.name().startsWith("<") || parameter.text().isBlank()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", parameter.name());
            row.put("doc", parameter.text());
            out.add(row);
        }
        return out;
    }

    /** The declaration's annotations that carry {@code @Documented}. */
    private List<String> documentedAnnotations(TypeElement type) {
        List<String> out = new ArrayList<>();
        for (javax.lang.model.element.AnnotationMirror mirror : type.getAnnotationMirrors()) {
            Element annotation = mirror.getAnnotationType().asElement();
            if (!(annotation instanceof TypeElement declared)) {
                continue;
            }
            String name = declared.getQualifiedName().toString();
            // Deprecation already has a banner of its own on the page.
            if (name.equals("java.lang.Deprecated") || !isDocumented(declared)) {
                continue;
            }
            // javadoc writes @Retention(CLASS); the mirror's toString is fully
            // qualified, which buries the name it is worth showing.
            out.add(mirror.toString().replace("@" + name, "@" + declared.getSimpleName()));
        }
        return out;
    }

    private static boolean isDocumented(TypeElement annotation) {
        for (javax.lang.model.element.AnnotationMirror mirror : annotation.getAnnotationMirrors()) {
            Element element = mirror.getAnnotationType().asElement();
            if (element instanceof TypeElement declared
                    && declared.getQualifiedName().contentEquals("java.lang.annotation.Documented")) {
                return true;
            }
        }
        return false;
    }

    /** The keyword a declaration of this type would actually use. */
    private static String keywordOf(TypeElement type) {
        return switch (type.getKind()) {
            case INTERFACE -> "interface";
            case ENUM -> "enum";
            case ANNOTATION_TYPE -> "@interface";
            case RECORD -> "record";
            default -> "class";
        };
    }

    private String kindOf(TypeElement type) {
        return switch (type.getKind()) {
            case INTERFACE -> "interface";
            case ENUM -> "enum";
            case ANNOTATION_TYPE -> "annotation";
            case RECORD -> "record";
            default -> "class";
        };
    }

    /** Superclasses from the immediate parent outwards, the way javadoc stacks them. */
    private List<Map<String, Object>> inheritanceChain(TypeElement type) {
        List<Map<String, Object>> chain = new ArrayList<>();
        TypeMirror current = type.getSuperclass();
        Set<String> seen = new LinkedHashSet<>();
        while (current != null && current.getKind() == TypeKind.DECLARED) {
            DeclaredType declared = (DeclaredType) current;
            if (!(declared.asElement() instanceof TypeElement element)
                    || !seen.add(element.getQualifiedName().toString())) {
                break;
            }
            // A package private implementation class cannot be named by a
            // consumer and has no page, and the standard doclet leaves it out of
            // the tree: PoseDetector's hierarchy is Object then PoseDetector, not
            // Object then AbstractVisionAnalyzer<Pose> then PoseDetector. The
            // walk still goes through it, so the substitution it carries reaches
            // the next visible ancestor.
            if (documented.containsKey(element.getQualifiedName().toString())) {
                chain.add(typeNames.reference(current));
            }
            // Step through the mirror rather than the declaration. Asking the
            // element for its superclass answers with the type variables as
            // declared, so the substitution is lost one level up and everything
            // above it: com.codename1.io.Properties extends HashMap<String,
            // String>, and its ancestry read AbstractMap<K, V>, naming variables
            // that mean nothing there.
            current = superclassOf(current);
        }
        java.util.Collections.reverse(chain);
        return chain;
    }

    /** The superclass of a parameterized type, with its arguments substituted in. */
    private TypeMirror superclassOf(TypeMirror type) {
        for (TypeMirror supertype : realSupertypes(type)) {
            if (supertype instanceof DeclaredType declared
                    && !isInterfaceLike(declared.asElement())) {
                return supertype;
            }
        }
        return null;
    }

    /**
     * Every interface in the type's contract, the way the standard reference
     * lists them under "All Implemented Interfaces".
     *
     * <p>Reading only the declaration hides everything a class gets from its
     * superclass: CheckBox declares none of its own, so it was published with no
     * interfaces at all while it really carries ActionSource, TextHolder,
     * SelectableIconHolder, Animation and more.
     */
    private List<Map<String, Object>> interfacesOf(TypeElement type) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Deque<TypeMirror> queue = new ArrayDeque<>(realSupertypes(type.asType()));
        while (!queue.isEmpty()) {
            TypeMirror supertype = queue.removeFirst();
            if (!(supertype instanceof DeclaredType declared)
                    || !(declared.asElement() instanceof TypeElement element)) {
                continue;
            }
            boolean isInterface = isInterfaceLike(element);
            if (isInterface && !seen.add(element.getQualifiedName().toString())) {
                continue;
            }
            // A package private interface is not part of a contract a consumer
            // can use, has no page, and the standard reference leaves it out:
            // GameView listed SpriteRenderer.Updatable and Ads listed
            // CSSParserCallback, both unlinked. The traversal still goes through
            // them, so their public superinterfaces are still found.
            if (isInterface && documented.containsKey(element.getQualifiedName().toString())) {
                out.add(typeNames.reference(supertype));
            }
            queue.addAll(realSupertypes(supertype));
        }
        out.sort(Comparator.comparing(row -> String.valueOf(row.get("label"))));
        return out;
    }

    /**
     * The subtypes a page advertises.
     *
     * <p>Direct children for a class, which is javadoc's "Direct Known
     * Subclasses", and the whole subtype graph for an interface, which is its
     * "All Known Subinterfaces" and "All Known Implementing Classes". Listing
     * only direct children of an interface hides most of what a reader is
     * looking for: java.util.Collection named four types where the standard page
     * names Deque, NavigableSet, ArrayList, Vector and the rest.
     */
    private List<Map<String, Object>> subclassesOf(TypeElement type) {
        List<TypeElement> children = isInterfaceLike(type)
                ? transitiveSubtypes(type)
                : new ArrayList<>(subtypes.getOrDefault(type.getQualifiedName().toString(), List.of()));
        children.sort(Comparator.comparing(child -> child.getQualifiedName().toString()));

        List<Map<String, Object>> out = new ArrayList<>();
        for (TypeElement child : children) {
            out.add(Map.of("label", Refs.nestedDisplayName(child), "url", Refs.typeUrl(child)));
        }
        return out;
    }

    private List<TypeElement> transitiveSubtypes(TypeElement type) {
        List<TypeElement> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Deque<TypeElement> queue = new ArrayDeque<>(
                subtypes.getOrDefault(type.getQualifiedName().toString(), List.of()));
        while (!queue.isEmpty()) {
            TypeElement next = queue.removeFirst();
            if (!seen.add(next.getQualifiedName().toString())) {
                continue;
            }
            out.add(next);
            queue.addAll(subtypes.getOrDefault(next.getQualifiedName().toString(), List.of()));
        }
        return out;
    }

    private List<Map<String, Object>> nestedRows(TypeElement type) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (TypeElement nested : ElementFilter.typesIn(type.getEnclosedElements())) {
            if (!documented.containsKey(nested.getQualifiedName().toString())) {
                continue;
            }
            ElementDoc doc = docReader.read(nested);
            out.add(new LinkedHashMap<>(Map.of(
                    "name", Refs.nestedDisplayName(nested),
                    "url", Refs.typeUrl(nested),
                    "kind", kindOf(nested),
                    "summary", TypeNames.summary(doc.description))));
        }
        return out;
    }

    private List<Map<String, Object>> memberRows(List<VariableElement> fields, TypeElement owner) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (VariableElement field : fields) {
            if (!isVisible(field)) {
                continue;
            }
            ElementDoc doc = docReader.read(field);
            if (doc.hidden) {
                continue;
            }
            Map<String, Object> row = baseRow(field, doc, owner);
            boolean isEnumConstant = field.getKind() == ElementKind.ENUM_CONSTANT;
            row.put("enumConstant", isEnumConstant);
            if (isEnumConstant) {
                // "public static final BindAttr TEXT" is how the model spells it
                // and not how anyone writes or reads it.
                row.put("modifiers", "");
            }
            row.put("fieldType", isEnumConstant ? null : typeNames.reference(field.asType()));
            Object constant = field.getConstantValue();
            row.put("constant", Literals.of(constant));
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> executableRows(
            List<? extends ExecutableElement> members, TypeElement owner) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ExecutableElement member : members) {
            if (!isVisible(member)) {
                continue;
            }
            ElementDoc doc = docReader.read(member);
            if (doc.hidden) {
                continue;
            }
            Map<String, Object> row = baseRow(member, doc, owner);
            // As a member of THIS type, not as declared. A promoted method comes
            // from a supertype that may bind its type parameters here:
            // PoseDetector promotes AbstractVisionAnalyzer<T>.process and returns
            // AsyncResource<Pose>, though PoseDetector declares no T at all.
            ExecutableType asMember = asMemberOf(owner, member);
            row.put("typeParameters", typeNames.typeParameters(member.getTypeParameters()));
            row.put("returnType", member.getKind() == ElementKind.CONSTRUCTOR
                    ? null
                    : typeNames.reference(asMember == null
                            ? member.getReturnType() : asMember.getReturnType()));
            row.put("parameters", parameterRows(member, doc, asMember));
            row.put("throws", throwsRows(member, doc));
            // Only what the signature actually declares. The rows above also
            // carry exceptions a comment documented without declaring, which
            // belong in the prose but not in the declaration.
            List<Map<String, Object>> declaredThrows = new ArrayList<>();
            for (TypeMirror thrown : asMember == null
                    ? member.getThrownTypes() : asMember.getThrownTypes()) {
                declaredThrows.add(typeNames.reference(thrown));
            }
            row.put("declaredThrows", declaredThrows);
            row.put("returns", doc.returns);
            // An annotation element without its default reads as required when it
            // is not: IntentParam.required() defaults to true and
            // AppIntent.timeoutSeconds() to 20, and neither was shown anywhere.
            javax.lang.model.element.AnnotationValue fallback = member.getDefaultValue();
            row.put("defaultValue", fallback == null ? null : Literals.of(fallback.getValue()));
            out.add(row);
        }
        return out;
    }

    private Map<String, Object> baseRow(Element member, ElementDoc doc, TypeElement owner) {
        List<String> anchors = refs.anchors(member);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", displayName(member, owner));
        row.put("anchor", anchors.get(0));
        // Javadoc answers to both the declared and the erased spelling of a
        // signature containing a type variable, and links in the wild use both.
        row.put("anchors", anchors);
        row.put("modifiers", TypeNames.modifiers(member));
        row.put("deprecated", doc.deprecated);
        row.put("deprecatedText", doc.deprecatedText);
        row.put("description", doc.description);
        row.put("summary", TypeNames.summary(doc.description));
        // C: the page being written, not the member's declaring type. A member
        // promoted off a package private supertype is declared somewhere that has
        // no page, so resolving its references against that type produced links
        // into a file the generator deliberately never writes.
        row.put("seeAlso", seeAlsoRefs(doc, owner));
        row.put("warnings", List.copyOf(doc.warnings));
        return row;
    }

    /**
     * What a member is called on the page.
     *
     * <p>{@code getSimpleName()} answers {@code <init>} for a constructor, which
     * is the JVM's name for it and not something to show a reader: every class
     * page listed its constructors as {@code <init>(String)}. The anchor keeps
     * that spelling, because javadoc's fragment really is {@code <init>()}.
     */
    private static String displayName(Element member, TypeElement owner) {
        if (member.getKind() != ElementKind.CONSTRUCTOR) {
            return member.getSimpleName().toString();
        }
        TypeElement declaring = Refs.enclosingType(member);
        return (declaring == null ? owner : declaring).getSimpleName().toString();
    }

    /** The member's type as seen through the type whose page this is, or null. */
    private ExecutableType asMemberOf(TypeElement owner, ExecutableElement member) {
        if (owner == null || !(owner.asType() instanceof DeclaredType declared)) {
            return null;
        }
        try {
            return (ExecutableType) types.asMemberOf(declared, member);
        } catch (IllegalArgumentException notAMember) {
            // Not reachable from this type after all; the declaration stands.
            return null;
        }
    }

    private List<Map<String, Object>> parameterRows(ExecutableElement member, ElementDoc doc,
                                                    ExecutableType asMember) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<? extends VariableElement> parameters = member.getParameters();
        List<? extends TypeMirror> substituted =
                asMember == null ? null : asMember.getParameterTypes();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            String name = parameter.getSimpleName().toString();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            TypeMirror parameterType = substituted != null && i < substituted.size()
                    ? substituted.get(i) : parameter.asType();
            Map<String, Object> reference = typeNames.reference(parameterType);
            if (member.isVarArgs() && i == parameters.size() - 1) {
                // The declared type is an array; the source spelling is an ellipsis.
                String label = String.valueOf(reference.get("label"));
                reference.put("label", label.endsWith("[]")
                        ? label.substring(0, label.length() - 2) + "..." : label);
            }
            row.put("type", reference);
            String text = doc.parameterText(name);
            row.put("doc", text == null ? "" : text);
            out.add(row);
        }
        // Type parameter documentation has nowhere to sit in the signature table,
        // so it is carried separately rather than dropped.
        for (MarkdownSections.NamedText documented : doc.parameters) {
            if (documented.name().startsWith("<")) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", documented.name());
                // Not Map.of: it rejects a null value and took the whole
                // generation down with a NullPointerException the moment any
                // method documented a type parameter. Nothing in the framework
                // does today, which is the only reason this was not a crash.
                Map<String, Object> noType = new LinkedHashMap<>();
                noType.put("label", "");
                noType.put("url", null);
                row.put("type", noType);
                row.put("doc", documented.text());
                out.add(row);
            }
        }
        return out;
    }

    private List<Map<String, Object>> throwsRows(ExecutableElement member, ElementDoc doc) {
        List<Map<String, Object>> out = new ArrayList<>();
        // Matched on the simple name as well as the written one: a Throws section
        // routinely names java.io.IOException where the signature, having
        // imported it, declares IOException. Comparing the strings as written
        // listed the same exception twice, once with prose and once without.
        Set<String> named = new LinkedHashSet<>();
        for (MarkdownSections.NamedText documented : doc.exceptions) {
            named.add(documented.name());
            named.add(simpleName(documented.name()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", documented.name());
            row.put("url", exceptionUrl(member, documented.name()));
            row.put("doc", documented.text());
            out.add(row);
        }
        // A declared exception with no prose still belongs in the throws list.
        for (TypeMirror thrown : member.getThrownTypes()) {
            String label = typeNames.label(thrown);
            if (named.contains(label) || named.contains(simpleName(label))) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", label);
            row.put("url", typeNames.url(thrown));
            row.put("doc", "");
            out.add(row);
        }
        return out;
    }

    /**
     * The page for an exception a Throws section names.
     *
     * <p>The method's own throws clause is consulted first, because a simple
     * name is ambiguous across the API and picking the first global match sends
     * the reader somewhere else entirely: {@code java.text.Format.parseObject}
     * declares {@code java.text.ParseException} and documents it as
     * {@code ParseException}, which was resolving to
     * {@code com.codename1.l10n.ParseException}.
     */
    private String exceptionUrl(ExecutableElement member, String name) {
        String wanted = simpleName(name);
        for (TypeMirror thrown : member.getThrownTypes()) {
            if (thrown instanceof DeclaredType declared
                    && declared.asElement() instanceof TypeElement element
                    && (element.getQualifiedName().contentEquals(name)
                        || element.getSimpleName().contentEquals(wanted))) {
                return documented.containsKey(element.getQualifiedName().toString())
                        ? Refs.typeUrl(element) : null;
            }
        }
        return urlOfTypeNamed(name);
    }

    /** The last segment of a dotted name. */
    private static String simpleName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    /** Resolves a bare exception name from a markdown Throws bullet to a page. */
    private String urlOfTypeNamed(String name) {
        if (name.indexOf('.') > 0) {
            TypeElement exact = documented.get(name);
            return exact == null ? null : Refs.typeUrl(exact);
        }
        String suffix = "." + name;
        for (Map.Entry<String, TypeElement> entry : documented.entrySet()) {
            if (entry.getKey().endsWith(suffix)) {
                return Refs.typeUrl(entry.getValue());
            }
        }
        return null;
    }

    /**
     * See-also entries, resolved to links where they name something we publish.
     *
     * <p>These are markdown bullets rather than {@code @see} tags, so nothing has
     * resolved them and {@link SeeAlsoRef} has to read the reference out of the
     * text. Three shapes matter, in descending order of how often they occur:
     * a local member ({@code #drawRoundRect}), a qualified member
     * ({@code Display#supportsNativeImageCache()}) and a bare type name.
     */
    private List<Map<String, Object>> seeAlsoRefs(ElementDoc doc, TypeElement context) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String entry : doc.seeAlso) {
            SeeAlsoRef reference = SeeAlsoRef.parse(entry);
            Map<String, Object> row = new LinkedHashMap<>();
            if (!reference.isReference()) {
                // Prose, or a markdown link that is already a link. Flagged so the
                // template renders it as markdown; wrapping it in a code span
                // showed the nine MDN links as literal [text](url).
                row.put("label", reference.label());
                row.put("url", null);
                row.put("note", "");
                row.put("prose", true);
                out.add(row);
                continue;
            }

            TypeElement owner = reference.type().isEmpty()
                    ? context
                    : lookupType(reference.type(), context);
            String url = null;
            if (owner != null) {
                url = reference.member().isEmpty()
                        ? Refs.typeUrl(owner)
                        : memberUrl(owner, reference);
            }
            row.put("label", reference.text());
            row.put("url", url);
            row.put("prose", false);
            // The trailing sentence a third of these carry, kept beside the link
            // rather than folded into it: it is prose about the reference, not
            // part of the name being linked.
            row.put("note", reference.label());
            out.add(row);
        }
        return out;
    }

    /**
     * A documented type named either fully or by its simple name.
     *
     * <p>A simple name is ambiguous across the API and the first match in
     * iteration order is not an answer: {@code List} is both
     * {@code com.codename1.ui.List} and {@code java.util.List}, and
     * {@code java.util.AbstractList} referring to {@code List#size} was being
     * sent to the UI widget. The link resolves, so the internal link check
     * cannot see it -- only reading the page shows it is the wrong class.
     *
     * <p>The package the reference was written in decides, which is what the
     * language would do with an unqualified name.
     */
    private TypeElement lookupType(String name, TypeElement context) {
        TypeElement exact = documented.get(name);
        if (exact != null) {
            return exact;
        }
        if (context != null) {
            PackageElement pkg = Refs.packageOf(context);
            if (pkg != null) {
                TypeElement sibling = documented.get(pkg.getQualifiedName() + "." + name);
                if (sibling != null) {
                    return sibling;
                }
            }
        }
        String suffix = "." + name;
        for (Map.Entry<String, TypeElement> entry : documented.entrySet()) {
            if (entry.getKey().endsWith(suffix)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * The URL of the member a reference names, matched as precisely as the
     * reference allows.
     *
     * <p>Name alone is not enough. {@code #clear(int)} against a type that
     * declares {@code clear()} first would otherwise link to the wrong overload,
     * which is worse than not linking at all: the reader follows it and lands on
     * a method that is not the one the author meant. So an exact identifier match
     * is tried first, then the argument count, and only a reference that wrote no
     * parameter list at all falls back to the first member of that name.
     */
    private String memberUrl(TypeElement owner, SeeAlsoRef reference) {
        // A reference is written against the type the reader is looking at, but
        // the member is very often declared further up: "#CENTER" on Label means
        // Component.CENTER, and "#getEditingDelegate()" on Picker likewise. Over
        // the framework that is roughly half of the member references that named
        // something real, so searching only the enclosing type leaves them dead.
        List<Element> candidates = new ArrayList<>();
        collectNamed(owner, reference.member(), candidates);
        Set<String> visited = new LinkedHashSet<>();
        for (TypeMirror supertype : allSupertypes(owner.asType(), visited)) {
            if (supertype instanceof DeclaredType declared
                    && declared.asElement() instanceof TypeElement parent) {
                collectNamed(parent, reference.member(), candidates);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        if (reference.hasParameterList()) {
            String wanted = reference.member() + "(" + String.join(",", reference.parameters()) + ")";
            for (Element candidate : candidates) {
                if (refs.anchors(candidate).contains(wanted)) {
                    return anchorUrl(owner, candidate);
                }
            }
            // The reference may spell the types simply where the identifier spells
            // them fully -- Component#paintShadows(Graphics, int, int) against
            // paintShadows(com.codename1.ui.Graphics,int,int) -- so compare the
            // types by their simple names.
            //
            // Arity alone is not enough, and settling for it was worse than not
            // linking at all: Vector#remove(Object) resolved to remove(int), and
            // Arrays#sort(Object[], int, int) to the byte[] overload. Both send
            // the reader to a method the author did not mean.
            for (Element candidate : candidates) {
                if (candidate instanceof ExecutableElement executable
                        && sameParameterTypes(executable, reference.parameters())) {
                    return anchorUrl(owner, candidate);
                }
            }
            // A parameter list that matches no overload is a stale reference --
            // Transform documents "#setScale()" and declares only the two and
            // three argument forms. Linking to an arbitrary overload would hide
            // that and send the reader to a method the author did not mean, so it
            // stays unlinked, which is what the standard pages showed anyway.
            return null;
        }
        return anchorUrl(owner, candidates.get(0));
    }

    /**
     * Whether a method's parameters are the ones a reference names, compared by
     * simple type name so that an imported spelling matches a qualified one.
     */
    private boolean sameParameterTypes(ExecutableElement method, List<String> written) {
        List<? extends VariableElement> parameters = method.getParameters();
        if (parameters.size() != written.size()) {
            return false;
        }
        for (int i = 0; i < parameters.size(); i++) {
            String actual = simpleName(typeNames.label(parameters.get(i).asType()));
            String wanted = simpleName(written.get(i).strip());
            if (method.isVarArgs() && i == parameters.size() - 1) {
                // The declaration is an array; the reference may write either.
                actual = actual.endsWith("[]") ? actual.substring(0, actual.length() - 2) : actual;
                wanted = wanted.endsWith("...") ? wanted.substring(0, wanted.length() - 3)
                        : wanted.endsWith("[]") ? wanted.substring(0, wanted.length() - 2) : wanted;
            }
            if (!stripGenerics(actual).equals(stripGenerics(wanted))) {
                return false;
            }
        }
        return true;
    }

    /** Drops a type argument list, which a reference may or may not write. */
    private static String stripGenerics(String label) {
        int open = label.indexOf('<');
        if (open < 0) {
            return label;
        }
        int close = label.lastIndexOf('>');
        String tail = close >= 0 && close + 1 < label.length() ? label.substring(close + 1) : "";
        return label.substring(0, open) + tail;
    }

    private void collectNamed(TypeElement type, String name, List<Element> out) {
        for (Element member : type.getEnclosedElements()) {
            if (isVisible(member) && !(member instanceof TypeElement)
                    && member.getSimpleName().contentEquals(name)) {
                out.add(member);
            }
        }
    }

    /**
     * The page a member is addressed on.
     *
     * <p>An inherited member lives on the page of the type that declares it, the
     * way javadoc links it. The exception is a member promoted off an
     * undocumented supertype: that type has no page, so the member was rendered
     * onto the referring type and is addressed there.
     */
    private String anchorUrl(TypeElement referring, Element member) {
        TypeElement home = Refs.enclosingType(member);
        String anchor = refs.anchors(member).get(0);
        if (home != null && documented.containsKey(home.getQualifiedName().toString())) {
            return Refs.typeUrl(home) + "#" + anchor;
        }
        return Refs.typeUrl(referring) + "#" + anchor;
    }

    /**
     * Members a type gets from its supertypes, grouped by where they come from --
     * the "Methods inherited from" blocks javadoc renders.
     */
    private List<Map<String, Object>> inheritedMembers(TypeElement type, List<TypeElement> promoted) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> declared = new LinkedHashSet<>();
        // Bucketed by simple name so the override test below only ever compares
        // methods that could possibly be the same one.
        Map<String, List<ExecutableElement>> seenByName = new LinkedHashMap<>();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            declared.add(signatureKey(method));
            remember(seenByName, method);
        }
        Set<String> promotedNames = new LinkedHashSet<>();
        for (TypeElement supertype : promoted) {
            promotedNames.add(supertype.getQualifiedName().toString());
            for (ExecutableElement method : ElementFilter.methodsIn(supertype.getEnclosedElements())) {
                declared.add(signatureKey(method));
                remember(seenByName, method);
            }
        }

        Set<String> visited = new LinkedHashSet<>();
        for (TypeMirror supertype : allSupertypes(type.asType(), visited)) {
            if (!(supertype instanceof DeclaredType declaredType)
                    || !(declaredType.asElement() instanceof TypeElement parent)) {
                continue;
            }
            // Already rendered as this type's own members just above.
            if (promotedNames.contains(parent.getQualifiedName().toString())) {
                continue;
            }
            List<Map<String, Object>> members = new ArrayList<>();
            for (ExecutableElement method : ElementFilter.methodsIn(parent.getEnclosedElements())) {
                if (!isVisible(method) || !declared.add(signatureKey(method))) {
                    continue;
                }
                // The erased signature is not enough once generics are
                // substituted along the hierarchy: Enum.compareTo(E) erases to
                // compareTo(java.lang.Enum) and Comparable.compareTo(T) to
                // compareTo(java.lang.Object), so every enum listed compareTo
                // twice even though the first implements the second.
                if (overridesSomethingSeen(seenByName, method, type)) {
                    continue;
                }
                remember(seenByName, method);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", method.getSimpleName().toString());
                String url = urlOf(method);
                row.put("url", url);
                members.add(row);
            }
            if (members.isEmpty()) {
                continue;
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("from", Refs.nestedDisplayName(parent));
            group.put("url", documented.containsKey(parent.getQualifiedName().toString())
                    ? Refs.typeUrl(parent) : null);
            group.put("members", members);
            out.add(group);
        }
        return out;
    }

    /** The same grouping as {@link #inheritedMembers}, for nested types. */
    private List<Map<String, Object>> inheritedNested(TypeElement type, List<TypeElement> promoted) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> declared = new LinkedHashSet<>();
        for (TypeElement nested : ElementFilter.typesIn(type.getEnclosedElements())) {
            declared.add(nested.getSimpleName().toString());
        }
        Set<String> promotedNames = new LinkedHashSet<>();
        for (TypeElement supertype : promoted) {
            promotedNames.add(supertype.getQualifiedName().toString());
        }

        Set<String> visited = new LinkedHashSet<>();
        for (TypeMirror supertype : allSupertypes(type.asType(), visited)) {
            if (!(supertype instanceof DeclaredType declaredType)
                    || !(declaredType.asElement() instanceof TypeElement parent)
                    || promotedNames.contains(parent.getQualifiedName().toString())) {
                continue;
            }
            List<Map<String, Object>> members = new ArrayList<>();
            for (TypeElement nested : ElementFilter.typesIn(parent.getEnclosedElements())) {
                if (!isVisible(nested) || !declared.add(nested.getSimpleName().toString())
                        || !documented.containsKey(nested.getQualifiedName().toString())) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", Refs.nestedDisplayName(nested));
                row.put("url", Refs.typeUrl(nested));
                members.add(row);
            }
            if (members.isEmpty()) {
                continue;
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("from", Refs.nestedDisplayName(parent));
            group.put("url", documented.containsKey(parent.getQualifiedName().toString())
                    ? Refs.typeUrl(parent) : null);
            group.put("members", members);
            out.add(group);
        }
        return out;
    }

    /** The same grouping as {@link #inheritedMembers}, for fields. */
    private List<Map<String, Object>> inheritedFields(TypeElement type, List<TypeElement> promoted) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> declared = new LinkedHashSet<>();
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            declared.add(field.getSimpleName().toString());
        }
        Set<String> promotedNames = new LinkedHashSet<>();
        for (TypeElement supertype : promoted) {
            promotedNames.add(supertype.getQualifiedName().toString());
            for (VariableElement field : ElementFilter.fieldsIn(supertype.getEnclosedElements())) {
                declared.add(field.getSimpleName().toString());
            }
        }

        Set<String> visited = new LinkedHashSet<>();
        for (TypeMirror supertype : allSupertypes(type.asType(), visited)) {
            if (!(supertype instanceof DeclaredType declaredType)
                    || !(declaredType.asElement() instanceof TypeElement parent)
                    || promotedNames.contains(parent.getQualifiedName().toString())) {
                continue;
            }
            List<Map<String, Object>> members = new ArrayList<>();
            for (VariableElement field : ElementFilter.fieldsIn(parent.getEnclosedElements())) {
                if (!isVisible(field) || !declared.add(field.getSimpleName().toString())) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", field.getSimpleName().toString());
                row.put("url", urlOf(field));
                members.add(row);
            }
            if (members.isEmpty()) {
                continue;
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("from", Refs.nestedDisplayName(parent));
            group.put("url", documented.containsKey(parent.getQualifiedName().toString())
                    ? Refs.typeUrl(parent) : null);
            group.put("members", members);
            out.add(group);
        }
        return out;
    }

    private List<TypeMirror> allSupertypes(TypeMirror type, Set<String> visited) {
        List<TypeMirror> out = new ArrayList<>();
        // types.directSupertypes() hands an interface java.lang.Object, which the
        // language does not: an interface inherits none of its methods, and the
        // standard pages list none. Without this an interface page such as
        // SuccessCallback claimed ten inherited Object methods, protected
        // clone() among them.
        boolean isInterface = type instanceof DeclaredType declaredType
                && isInterfaceLike(declaredType.asElement());
        for (TypeMirror supertype : realSupertypes(type)) {
            if (!(supertype instanceof DeclaredType declared)
                    || !(declared.asElement() instanceof TypeElement element)
                    || !visited.add(element.getQualifiedName().toString())) {
                continue;
            }
            out.add(supertype);
            out.addAll(allSupertypes(supertype, visited));
        }
        return out;
    }

    private static void remember(Map<String, List<ExecutableElement>> seen, ExecutableElement method) {
        seen.computeIfAbsent(method.getSimpleName().toString(), key -> new ArrayList<>()).add(method);
    }

    /** Whether a method already listed implements or overrides this one. */
    private boolean overridesSomethingSeen(Map<String, List<ExecutableElement>> seen,
                                           ExecutableElement candidate, TypeElement type) {
        for (ExecutableElement earlier : seen.getOrDefault(
                candidate.getSimpleName().toString(), List.of())) {
            if (elements.overrides(earlier, candidate, type)) {
                return true;
            }
        }
        return false;
    }

    /** Name plus erased parameter types: what makes one method the same as another. */
    private String signatureKey(ExecutableElement method) {
        List<String> anchors = refs.anchors(method);
        return anchors.get(anchors.size() - 1);
    }

    // --------------------------------------------------------- package pages

    private void writePackagePages() throws IOException {
        Map<String, List<TypeElement>> byPackage = new TreeMap<>();
        for (TypeElement type : documented.values()) {
            // Nested types are listed on their outer type's page, exactly as javadoc
            // lists them, so only top level types get a row in the package summary.
            if (type.getEnclosingElement() instanceof TypeElement) {
                continue;
            }
            byPackage.computeIfAbsent(Refs.packageOf(type).getQualifiedName().toString(),
                    key -> new ArrayList<>()).add(type);
        }

        for (Map.Entry<String, List<TypeElement>> entry : byPackage.entrySet()) {
            PackageElement pkg = elements.getPackageElement(entry.getKey());
            if (pkg == null) {
                continue;
            }
            ElementDoc doc = docReader.read(pkg);
            List<TypeElement> members = entry.getValue();
            members.sort(Comparator.comparing(type -> type.getSimpleName().toString()));

            List<Map<String, Object>> rows = new ArrayList<>();
            for (TypeElement type : members) {
                rows.add(new LinkedHashMap<>(Map.of(
                        "name", Refs.nestedDisplayName(type),
                        "url", Refs.typeUrl(type),
                        "kind", kindOf(type),
                        "summary", TypeNames.summary(docReader.read(type).description))));
            }

            Map<String, Object> api = new LinkedHashMap<>();
            api.put("kind", "package");
            api.put("qualified", entry.getKey());
            // A package can be deprecated, and com.codename1.ui.layouts.mig is:
            // its comment warns not to rely on the integration in production.
            // DocReader lifts that out of the description, so omitting the fields
            // here dropped the warning off the page entirely.
            api.put("deprecated", doc.deprecated);
            api.put("deprecatedText", doc.deprecatedText);
            api.put("description", doc.description);
            api.put("types", rows);

            Map<String, Object> frontMatter = new LinkedHashMap<>();
            frontMatter.put("title", entry.getKey());
            frontMatter.put("url", Refs.packageUrl(pkg));
            frontMatter.put("description", TypeNames.summary(doc.description));
            frontMatter.put("layout", "package");
            // Deliberately no alias on the bare package directory. It would put an
            // index.html in com/codename1/ui/list/, which is the same directory as
            // the com.codename1.ui.List type page on a case insensitive filesystem,
            // and one silently overwrote the other. Nothing links to a bare package
            // directory anyway; the summary is what the guide and javadoc name.
            frontMatter.put("aliases", List.of());
            frontMatter.put("javadoc", api);

            write(contentRoot.resolve(Refs.packageContentPath(pkg)), Json.write(frontMatter));
        }
    }

    /** The API index at {@code /javadoc/}, listing every package. */
    private void writeOverview() throws IOException {
        Set<String> packageNames = new java.util.TreeSet<>();
        for (TypeElement type : documented.values()) {
            packageNames.add(Refs.packageOf(type).getQualifiedName().toString());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String name : packageNames) {
            PackageElement pkg = elements.getPackageElement(name);
            if (pkg == null) {
                continue;
            }
            rows.add(new LinkedHashMap<>(Map.of(
                    "name", name,
                    "url", Refs.packageUrl(pkg),
                    "summary", TypeNames.summary(docReader.read(pkg).description))));
        }

        Map<String, Object> api = new LinkedHashMap<>();
        api.put("kind", "overview");
        api.put("packages", rows);
        api.put("typeCount", documented.size());

        Map<String, Object> frontMatter = new LinkedHashMap<>();
        frontMatter.put("title", "API");
        frontMatter.put("url", "/javadoc/");
        frontMatter.put("description", "Codename One API reference");
        frontMatter.put("layout", "overview");
        // The website has linked the API from /api/ since 2015.
        frontMatter.put("aliases", List.of("/api/"));
        frontMatter.put("javadoc", api);

        write(contentRoot.resolve("_index.md"), Json.write(frontMatter));
    }

    // -------------------------------------------------------------- search

    private void addSearchRows(TypeElement type, ElementDoc doc, List<Element> owned) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("n", Refs.nestedDisplayName(type));
        row.put("p", Refs.packageOf(type).getQualifiedName().toString());
        row.put("u", Refs.typeUrl(type));
        row.put("k", kindOf(type));
        // Plain text: the results list escapes what it is given rather than
        // rendering it, which is right for a value that came out of a comment,
        // so markdown left here is displayed as its own source.
        row.put("s", TypeNames.plainSummary(doc.description));

        // Members are two strings each -- the label to show and the fragment to
        // jump to -- nested under their type rather than repeated as standalone
        // rows. The flat form, with a URL, a package and a summary per member,
        // came to 9.7MB over 31519 entries, which is not a file a browser should
        // download to answer one search. Grouping removes the repetition and
        // dropping member summaries removes the bulk; the type summaries stay,
        // because those are what a result list actually shows.
        // The same list the page renders, promoted members included. Scanning
        // only the type's own elements left InterstitialAd searchable by its
        // constructor alone, while its page showed load(), isLoaded() and show()
        // at anchors nothing could find.
        List<Object> members = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element member : owned) {
            if (!isVisible(member) || member instanceof TypeElement) {
                continue;
            }
            // Constructors are not inherited and the page renders only its own,
            // so a promoted one is a search hit pointing at an <init> fragment
            // that does not exist: ComponentAnimation.UIMutation was offering
            // CompoundAnimation's two constructors.
            if (member.getKind() == ElementKind.CONSTRUCTOR
                    && !type.equals(Refs.enclosingType(member))) {
                continue;
            }
            if (docReader.read(member).hidden) {
                continue;
            }
            String anchor = refs.anchors(member).get(0);
            if (!seen.add(anchor)) {
                continue;
            }
            String label = member instanceof ExecutableElement executable
                    ? displayName(member, type) + "(" + parameterLabels(executable) + ")"
                    : member.getSimpleName().toString();
            members.add(List.of(label, anchor));
        }
        row.put("m", members);
        searchRows.add(row);
    }

    /**
     * Parameter types as a reader would write them, for the search index.
     *
     * <p>The last parameter of a varargs method is an array in the model, so
     * asList(T... array) was offered as asList(T[]) -- and byte[]... as
     * byte[][], which says something different. 1198 labels carried the array
     * spelling and none carried an ellipsis.
     */
    private String parameterLabels(ExecutableElement executable) {
        List<VariableElement> parameters = new ArrayList<>(executable.getParameters());
        List<String> out = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            String label = typeNames.label(parameters.get(i).asType());
            if (executable.isVarArgs() && i == parameters.size() - 1 && label.endsWith("[]")) {
                label = label.substring(0, label.length() - 2) + "...";
            }
            out.add(label);
        }
        return String.join(", ", out);
    }

    private void writeSearchIndex() throws IOException {
        if (searchIndex == null) {
            return;
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("generator", "HugoDoclet");
        document.put("types", searchRows);
        write(searchIndex, Json.writeCompact(document));

        int memberCount = 0;
        for (Map<String, Object> row : searchRows) {
            Object members = row.get("m");
            if (members instanceof List<?> list) {
                memberCount += list.size();
            }
        }
        reporter.print(javax.tools.Diagnostic.Kind.NOTE,
                "Hugo javadoc: " + documented.size() + " types, "
                        + memberCount + " searchable members");
    }

    // --------------------------------------------------------------- output

    private static void write(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try {
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IOException("writing " + target + ": " + failure, failure);
        }
    }

    /** A doclet option that takes exactly one argument. */
    private record SimpleOption(String name, String parameters, String description,
                                java.util.function.Consumer<String> action) implements Option {
        @Override
        public int getArgumentCount() {
            return 1;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return List.of(name);
        }

        @Override
        public String getParameters() {
            return parameters;
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            action.accept(arguments.get(0));
            return true;
        }
    }

    static String stackTrace(Throwable failure) {
        StringWriter text = new StringWriter();
        failure.printStackTrace(new PrintWriter(text));
        return text.toString();
    }
}
