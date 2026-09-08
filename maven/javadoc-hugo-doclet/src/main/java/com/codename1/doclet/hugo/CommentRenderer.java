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

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.RawTextTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import java.util.List;
import javax.lang.model.element.Element;

/**
 * Turns a documentation comment body into the markdown that goes into the page.
 *
 * <p>For this codebase the job is mostly to get out of the way. 1972 of the 2024
 * core sources use markdown documentation comments, and JDK 23 and later hand a
 * markdown comment to a doclet as a single {@code RawTextTree} of kind
 * {@link DocTree.Kind#MARKDOWN} holding the author's text verbatim. That text is
 * copied straight through and rendered by the same goldmark that renders the
 * rest of the site, so a code fence, a table or a list in a comment looks like a
 * code fence, a table or a list anywhere else on the site. Deliberately no
 * markdown library is involved here: a second implementation could only
 * disagree with the one that actually renders the page.
 *
 * <p>What does need translating is the inline tags, which markdown has no
 * spelling for. They are converted to markdown links against the real resolved
 * element, so an unqualified {@code {@link #paintDirty()}} lands on the right
 * page.
 */
final class CommentRenderer {

    /** Resolves a documented element to its site URL, or null when it has no page. */
    interface Links {
        String url(Element target);
    }

    private final DocTrees trees;
    private final Links links;

    CommentRenderer(DocTrees trees, Links links) {
        this.trees = trees;
        this.links = links;
    }

    /**
     * Renders a run of documentation nodes.
     *
     * @param nodes the nodes, typically a comment's full body or one block tag's content
     * @param path  the path of the enclosing comment, used to resolve references;
     *              references are left as plain text when this is null
     * @return markdown
     */
    String render(List<? extends DocTree> nodes, DocTreePath path) {
        StringBuilder out = new StringBuilder();
        for (DocTree node : nodes) {
            append(out, node, path);
        }
        return out.toString();
    }

    private void append(StringBuilder out, DocTree node, DocTreePath path) {
        switch (node.getKind()) {
            case MARKDOWN -> out.append(((RawTextTree) node).getContent());
            case TEXT -> out.append(((TextTree) node).getBody());
            // {@code} and {@literal} are both LiteralTree; only the kind differs.
            case CODE -> appendCode(out, ((LiteralTree) node).getBody().getBody());
            case LITERAL -> out.append(escape(((LiteralTree) node).getBody().getBody()));
            case ENTITY -> out.append("&").append(((EntityTree) node).getName()).append(";");
            case LINK -> appendLink(out, (LinkTree) node, path, true);
            case LINK_PLAIN -> appendLink(out, (LinkTree) node, path, false);
            // START_ELEMENT, END_ELEMENT and anything else a legacy comment can hold
            // are passed through as written. Nothing in CodenameOne/src still uses a
            // /** */ comment, so this is the tail: Ports/CLDC11 and whatever a future
            // contributor writes by hand.
            default -> out.append(node.toString());
        }
    }

    /** Wraps text in enough backticks that its own backticks survive. */
    private void appendCode(StringBuilder out, String body) {
        // A {@code} spanning lines is a code block, and the javadoc idiom for one
        // is <pre>{@code ... }</pre>. Rendering it as an inline span put the whole
        // example on one line between backticks.
        if (body.indexOf('\n') >= 0) {
            String fence = "```";
            while (body.contains(fence)) {
                fence += "`";
            }
            out.append('\n').append(fence).append('\n')
                    .append(body.strip())
                    .append('\n').append(fence).append('\n');
            return;
        }
        int longest = 0;
        int run = 0;
        for (int i = 0; i < body.length(); i++) {
            run = body.charAt(i) == '`' ? run + 1 : 0;
            longest = Math.max(longest, run);
        }
        String fence = "`".repeat(longest + 1);
        out.append(fence);
        // A code span whose content starts or ends with a backtick needs padding
        // spaces, which markdown strips again when it renders.
        if (body.startsWith("`") || body.endsWith("`")) {
            out.append(' ').append(body).append(' ');
        } else {
            out.append(body);
        }
        out.append(fence);
    }

    private void appendLink(StringBuilder out, LinkTree link, DocTreePath path, boolean code) {
        ReferenceTree reference = link.getReference();
        String label = render(link.getLabel(), path).strip();
        if (label.isEmpty()) {
            label = defaultLabel(reference);
        }

        String url = resolve(reference, path);
        String text = code ? "`" + label + "`" : label;
        if (url == null) {
            out.append(text);
            return;
        }
        out.append('[').append(text).append("](").append(markdownUrl(url)).append(')');
    }

    /** The URL a reference points at, or null when it resolves to nothing we publish. */
    private String resolve(ReferenceTree reference, DocTreePath path) {
        if (reference == null || path == null) {
            return null;
        }
        DocTreePath referencePath = DocTreePath.getPath(path, reference);
        if (referencePath == null) {
            return null;
        }
        Element target = trees.getElement(referencePath);
        return target == null ? null : links.url(target);
    }

    /**
     * The text javadoc shows when a link carries no explicit label: the reference
     * with its package qualifiers dropped and its leading {@code #} removed.
     */
    private static String defaultLabel(ReferenceTree reference) {
        if (reference == null) {
            return "";
        }
        String signature = reference.getSignature().strip();
        int hash = signature.indexOf('#');
        String type = hash < 0 ? signature : signature.substring(0, hash);
        String member = hash < 0 ? "" : signature.substring(hash + 1);

        String simpleType = type;
        int lastDot = -1;
        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (c == '(') {
                break;
            }
            // Only a dot that separates a lower case package segment from what
            // follows is a qualifier; Outer.Inner must keep its dot.
            if (c == '.' && i + 1 < type.length() && Character.isLowerCase(type.charAt(i > 0 ? i - 1 : 0))) {
                lastDot = i;
            }
        }
        if (lastDot >= 0) {
            simpleType = type.substring(lastDot + 1);
        }

        if (member.isEmpty()) {
            return simpleType;
        }
        return simpleType.isEmpty() ? member : simpleType + "." + member;
    }

    /**
     * Makes a URL safe to sit in a markdown link destination.
     *
     * <p>A constructor's fragment is {@code #<init>(...)}, and markdown reads
     * angle brackets in a destination as a delimiter of its own, so
     * {@code [x](/p/T.html#<init>(int))} comes out mangled -- the one link on the
     * site that pointed at a constructor from inside a comment arrived as
     * {@code &amp;lt;init&gt;}. Percent encoding them is transparent to the
     * browser, which decodes a fragment before matching it against an id.
     *
     * <p>Parentheses are deliberately left alone: they are balanced in every
     * signature javadoc emits, and CommonMark allows balanced parentheses in a
     * destination.
     */
    private static String markdownUrl(String url) {
        return url.replace("<", "%3C").replace(">", "%3E").replace(" ", "%20");
    }

    /** Escapes the characters markdown would otherwise treat as syntax. */
    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ("\\`*_{}[]()#+-.!<>|~".indexOf(c) >= 0) {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }
}
