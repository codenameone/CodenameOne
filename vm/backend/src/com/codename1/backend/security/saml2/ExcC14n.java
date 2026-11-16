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
package com.codename1.backend.security.saml2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Exclusive XML Canonicalization 1.0, without comments
/// (`http://www.w3.org/2001/10/xml-exc-c14n#`): the bytes an XML signature
/// over a SAML element is computed on.
///
/// Only an element subtree is ever canonicalized here -- the element a
/// reference names, or `SignedInfo` -- which is all SAML uses. Comments never
/// reach it, because [Xml] drops them, and that is the "without comments"
/// variant; the "with comments" one is refused by the verifier.
final class ExcC14n {
    static final String ALGORITHM = "http://www.w3.org/2001/10/xml-exc-c14n#";

    private ExcC14n() {
    }

    /// `root` canonicalized, leaving out `omit` (the enveloped signature) when
    /// it is not null.
    ///
    /// @param inclusive the prefixes of the `InclusiveNamespaces PrefixList`,
    /// treated as inclusive canonicalization treats every prefix; "#default"
    /// names the default namespace
    static byte[] canonicalize(Xml.Element root, Xml.Element omit, Set<String> inclusive) {
        StringBuilder out = new StringBuilder(2048);
        render(root, omit, inclusive, new HashMap<String, String>(), out);
        try {
            return out.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("No UTF-8 encoder", err);
        }
    }

    private static void render(Xml.Element e, Xml.Element omit, Set<String> inclusive,
                               Map<String, String> rendered, StringBuilder out) {
        // The namespaces this element visibly uses, and the inclusive ones in scope.
        TreeMap<String, String> emit = new TreeMap<String, String>();
        List<String> wanted = new ArrayList<String>();
        wanted.add(e.prefix);
        for (Xml.Attribute a : e.attributes) {
            if (a.prefix.length() > 0 && !"xml".equals(a.prefix)) {
                wanted.add(a.prefix);
            }
        }
        for (String p : inclusive) {
            String key = "#default".equals(p) ? "" : p;
            if (key.length() == 0 || e.lookup(key) != null) {
                wanted.add(key);
            }
        }
        Map<String, String> scope = new HashMap<String, String>(rendered);
        for (String p : wanted) {
            String ns = e.lookup(p);
            if (ns == null) {
                continue;
            }
            String before = rendered.get(p);
            if (p.length() == 0) {
                // xmlns="" only to undo a default an output ancestor declared.
                String was = before == null ? "" : before;
                if (!was.equals(ns)) {
                    emit.put(p, ns);
                    scope.put(p, ns);
                }
            } else if (!ns.equals(before)) {
                emit.put(p, ns);
                scope.put(p, ns);
            }
        }
        out.append('<').append(qname(e.prefix, e.localName));
        for (Map.Entry<String, String> d : emit.entrySet()) {
            out.append(' ').append(d.getKey().length() == 0 ? "xmlns" : "xmlns:" + d.getKey())
                    .append("=\"");
            escapeAttribute(d.getValue(), out);
            out.append('"');
        }
        // Attributes by namespace URI, then local name; no namespace first.
        TreeMap<String, Xml.Attribute> attrs = new TreeMap<String, Xml.Attribute>();
        for (Xml.Attribute a : e.attributes) {
            attrs.put(a.namespace + "\u0000" + a.localName, a);
        }
        for (Xml.Attribute a : attrs.values()) {
            out.append(' ').append(a.qualifiedName()).append("=\"");
            escapeAttribute(a.value, out);
            out.append('"');
        }
        out.append('>');
        for (Xml.Node n : e.children) {
            if (n == omit) { // NOPMD CompareObjectsWithEquals - the one node left out, by identity
                continue;
            }
            if (n instanceof Xml.Text) {
                escapeText(((Xml.Text) n).value, out);
            } else {
                render((Xml.Element) n, omit, inclusive, scope, out);
            }
        }
        out.append("</").append(qname(e.prefix, e.localName)).append('>');
    }

    private static String qname(String prefix, String local) {
        return prefix.length() == 0 ? local : prefix + ":" + local;
    }

    private static void escapeText(CharSequence s, StringBuilder out) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '\r':
                    out.append("&#xD;");
                    break;
                default:
                    out.append(c);
            }
        }
    }

    private static void escapeAttribute(String s, StringBuilder out) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\t':
                    out.append("&#x9;");
                    break;
                case '\n':
                    out.append("&#xA;");
                    break;
                case '\r':
                    out.append("&#xD;");
                    break;
                default:
                    out.append(c);
            }
        }
    }
}
