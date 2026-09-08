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

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Reads one element's documentation and merges the two ways this codebase
 * expresses it.
 *
 * <p>Both forms are live: 816 {@code @param} tags against 9068
 * {@code #### Parameters} headings, 1036 {@code @return} against 7224
 * {@code #### Returns}. The markdown form is parsed by {@link MarkdownSections}
 * and wins where the two overlap, because it is what the author most recently
 * wrote; block tags fill in anything it did not cover.
 *
 * <p>Two tags are dropped rather than rendered. {@code @since} and the
 * {@code #### Since} heading are dropped because Codename One does not publish
 * availability metadata at all -- {@code scripts/check-since-tags.sh} fails the
 * build over one in a source file, on the grounds that a guessed version is
 * worse than no version. {@code @hidden} is not dropped but obeyed: the element
 * disappears from the output entirely.
 */
final class DocReader {

    /** Guards against a cycle in overriding chains while resolving inherited docs. */
    private static final int MAX_INHERIT_DEPTH = 16;

    private final DocTrees trees;
    private final Elements elements;
    private final Types types;
    private final CommentRenderer renderer;

    /**
     * Memo of everything already read.
     *
     * <p>Not an optimisation of last resort: every element is read at least twice
     * over -- once to decide whether {@code @hidden} keeps it out of the index,
     * again to render it, and once more for each summary row that quotes it --
     * and reading a method walks its whole supertype chain looking for the
     * declaration it inherits from. Without this the generator re-walks the
     * hierarchy of roughly 1850 types several times each.
     */
    private final Map<Element, ElementDoc> cache = new HashMap<>();

    DocReader(DocTrees trees, Elements elements, Types types, CommentRenderer renderer) {
        this.trees = trees;
        this.elements = elements;
        this.types = types;
        this.renderer = renderer;
    }

    ElementDoc read(Element element) {
        ElementDoc cached = cache.get(element);
        if (cached != null) {
            return cached;
        }
        ElementDoc doc = read(element, 0);
        cache.put(element, doc);
        return doc;
    }

    private ElementDoc read(Element element, int depth) {
        ElementDoc doc = new ElementDoc();
        DocCommentTree comment = trees.getDocCommentTree(element);

        if (comment == null) {
            // An undocumented override still documents itself through its parent,
            // which is the behaviour every Java developer expects from javadoc.
            ElementDoc inherited = inherit(element, depth);
            ElementDoc result = inherited == null ? doc : inherited;
            markAnnotationDeprecation(element, result);
            return result;
        }

        DocTreePath path = pathOf(element, comment);
        // Goldmark drops raw HTML rather than rendering it, so the leftovers in
        // the comments that were converted to markdown have to be dealt with
        // before anything else reads the body.
        doc.description = LegacyHtml.convert(renderer.render(comment.getFullBody(), path));

        MarkdownSections.Result sections = MarkdownSections.parse(doc.description);
        doc.description = sections.description();
        doc.parameters.addAll(sections.parameters());
        doc.exceptions.addAll(sections.exceptions());
        doc.seeAlso.addAll(sections.seeAlso());
        doc.returns = sections.returns();
        if (sections.deprecated() != null) {
            doc.deprecated = true;
            doc.deprecatedText = sections.deprecated();
        }

        readBlockTags(comment, path, doc);
        markAnnotationDeprecation(element, doc);
        resolveInheritDoc(element, doc, depth);
        return doc;
    }

    /**
     * Marks an element deprecated because it is annotated, tag or no tag.
     *
     * <p>{@code @Deprecated} and {@code @deprecated} are independent: the
     * annotation is what the compiler warns on, the tag is what explains it, and
     * an API may carry either. 23 files here carry the annotation, and
     * {@code com.codename1.ui.util.MutableResouce} carries it with no tag at all,
     * so reading only the documentation lost its deprecated marking entirely
     * while the standard pages showed it.
     *
     * <p>Only ever sets the flag. A comment that documented a deprecation keeps
     * whatever text it gave.
     *
     * <p>Note the site marks 299 more members deprecated than the standard pages
     * do, and that is correct rather than a leak. Those carry a
     * {@code #### Deprecated} section, which the standard doclet renders as an
     * ordinary heading inside the description because it cannot see the
     * convention -- the same reason it shows no parameter tables. Measured: every
     * one of the 299 has deprecation text, and none is an undocumented override
     * inheriting the flag from its parent.
     */
    private void markAnnotationDeprecation(Element element, ElementDoc doc) {
        if (elements.isDeprecated(element)) {
            doc.deprecated = true;
        }
    }

    private void readBlockTags(DocCommentTree comment, DocTreePath path, ElementDoc doc) {
        for (DocTree tag : comment.getBlockTags()) {
            switch (tag.getKind()) {
                case PARAM -> {
                    ParamTree param = (ParamTree) tag;
                    // Type parameter documentation has no column in the rendered
                    // signature table, so it is folded into the description rather
                    // than silently dropped.
                    String name = param.getName().getName().toString();
                    String text = renderer.render(param.getDescription(), path).strip();
                    doc.addParameter(new MarkdownSections.NamedText(
                            param.isTypeParameter() ? "<" + name + ">" : name, text));
                }
                case RETURN -> {
                    if (doc.returns == null) {
                        doc.returns = renderer.render(((ReturnTree) tag).getDescription(), path).strip();
                    }
                }
                case THROWS, EXCEPTION -> {
                    ThrowsTree thrown = (ThrowsTree) tag;
                    doc.addException(new MarkdownSections.NamedText(
                            thrown.getExceptionName().getSignature(),
                            renderer.render(thrown.getDescription(), path).strip()));
                }
                case SEE -> {
                    String text = renderer.render(((SeeTree) tag).getReference(), path).strip();
                    if (!text.isEmpty()) {
                        doc.seeAlso.add(text);
                    }
                }
                case DEPRECATED -> {
                    doc.deprecated = true;
                    String text = renderer.render(((DeprecatedTree) tag).getBody(), path).strip();
                    if (!text.isEmpty()) {
                        doc.deprecatedText = text;
                    }
                }
                case HIDDEN -> doc.hidden = true;
                // @author, @version and @since carry no weight on a published page.
                // @since is dropped on purpose; see the class comment.
                default -> {
                    if (tag instanceof BlockTagTree named && "hidden".equals(named.getTagName())) {
                        doc.hidden = true;
                    }
                }
            }
        }
    }

    /**
     * Fills in whatever the element left to its parent.
     *
     * <p>Covers both spellings of the same intent: an explicit
     * {@code {@inheritDoc}} in the description, and a description, return or
     * parameter the override simply did not write.
     */
    private void resolveInheritDoc(Element element, ElementDoc doc, int depth) {
        boolean wantsDescription = containsInheritDoc(element) || doc.description.isBlank();
        // A marker inside a structured section is a request too. An override that
        // writes "#### Returns" with {@inheritDoc} under it leaves doc.returns
        // non-null, so testing only for null published the marker itself --
        // BubbleTransition.copy and FlipTransition.copy both showed a literal
        // {@inheritDoc} where the parent's text belonged.
        boolean wantsDetail = doc.returns == null
                || isInheritDoc(doc.returns)
                || hasUndocumentedParameter(element, doc);
        if (!wantsDescription && !wantsDetail) {
            return;
        }

        ElementDoc parent = inherit(element, depth);
        if (parent == null) {
            return;
        }

        if (wantsDescription) {
            if (doc.description.isBlank()) {
                doc.description = parent.description;
            } else {
                doc.description = doc.description.replace(INHERIT_DOC_MARKER, parent.description);
            }
        }
        if (doc.returns == null || isInheritDoc(doc.returns)) {
            doc.returns = parent.returns;
        }
        if (element instanceof ExecutableElement executable) {
            for (var parameter : executable.getParameters()) {
                String name = parameter.getSimpleName().toString();
                String own = doc.parameterText(name);
                if (own != null && !isInheritDoc(own)) {
                    continue;
                }
                String inherited = parent.parameterText(name);
                if (inherited != null) {
                    doc.parameters.removeIf(existing -> existing.name().equals(name));
                    doc.addParameter(new MarkdownSections.NamedText(name, inherited));
                } else if (own != null) {
                    // Nothing to inherit: drop the marker rather than publish it.
                    doc.parameters.removeIf(existing -> existing.name().equals(name));
                }
            }
        }
        if (doc.returns != null && isInheritDoc(doc.returns)) {
            doc.returns = null;
        }
        for (MarkdownSections.NamedText exception : parent.exceptions) {
            doc.addException(exception);
        }
    }

    private boolean hasUndocumentedParameter(Element element, ElementDoc doc) {
        if (!(element instanceof ExecutableElement executable)) {
            return false;
        }
        for (var parameter : executable.getParameters()) {
            if (doc.parameterText(parameter.getSimpleName().toString()) == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * The rendered form of {@code {@inheritDoc}}.
     *
     * <p>{@link CommentRenderer} has no element context, so it renders the tag
     * through its default branch as its own source text. That text is the marker
     * this class substitutes into, which keeps the renderer free of any
     * inheritance knowledge.
     */
    private static final String INHERIT_DOC_MARKER = "{@inheritDoc}";

    /** Whether a documented value is nothing but an inherit marker. */
    private static boolean isInheritDoc(String text) {
        return text != null && text.strip().equals(INHERIT_DOC_MARKER);
    }

    private boolean containsInheritDoc(Element element) {
        DocCommentTree comment = trees.getDocCommentTree(element);
        if (comment == null) {
            return false;
        }
        for (DocTree node : comment.getFullBody()) {
            if (node instanceof InheritDocTree) {
                return true;
            }
        }
        return false;
    }

    /** The documentation of the method this one overrides, or null when there is none. */
    private ElementDoc inherit(Element element, int depth) {
        if (depth >= MAX_INHERIT_DEPTH || !(element instanceof ExecutableElement method)) {
            return null;
        }
        ExecutableElement overridden = findOverridden(method);
        return overridden == null ? null : read(overridden, depth + 1);
    }

    /**
     * The first method this one overrides, searching superclasses before
     * interfaces, which is the order javadoc documents.
     */
    private ExecutableElement findOverridden(ExecutableElement method) {
        TypeElement owner = Refs.enclosingType(method);
        if (owner == null) {
            return null;
        }
        List<TypeElement> supertypes = new ArrayList<>();
        collectSupertypes(owner.asType(), supertypes, new ArrayList<>());
        for (TypeElement supertype : supertypes) {
            for (ExecutableElement candidate : ElementFilter.methodsIn(supertype.getEnclosedElements())) {
                if (elements.overrides(method, candidate, owner)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void collectSupertypes(TypeMirror type, List<TypeElement> out, List<String> seen) {
        for (TypeMirror supertype : types.directSupertypes(type)) {
            if (!(supertype instanceof DeclaredType declared)
                    || !(declared.asElement() instanceof TypeElement element)) {
                continue;
            }
            String name = element.getQualifiedName().toString();
            if (seen.contains(name)) {
                continue;
            }
            seen.add(name);
            out.add(element);
            collectSupertypes(supertype, out, seen);
        }
    }

    /** The comment's path, needed to resolve references, or null when unavailable. */
    private DocTreePath pathOf(Element element, DocCommentTree comment) {
        TreePath treePath = trees.getPath(element);
        return treePath == null ? null : new DocTreePath(treePath, comment);
    }
}
