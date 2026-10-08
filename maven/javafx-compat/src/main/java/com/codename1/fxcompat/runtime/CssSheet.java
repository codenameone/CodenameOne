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
package com.codename1.fxcompat.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.codename1.compat.jdk.ResourceNames;
import com.codename1.compat.jdk.Resources;
import com.codename1.fxcompat.runtime.css.CssDeclaration;
import com.codename1.fxcompat.runtime.css.CssSelector;
import com.codename1.fxcompat.runtime.css.CssSheetData;
import com.codename1.fxcompat.runtime.css.CssValue;

import javafx.css.PseudoClass;

/// A compiled style sheet as [CssEngine] matches against it: the rules of
/// the sheet and of everything it imports, in cascade order, indexed by
/// what the rightmost compound of each selector requires.
///
/// #### The index
///
/// A rule can only match a node that satisfies its subject compound, so
/// each rule is filed under ONE thing that compound requires -- its id if
/// it has one, else its first style class, else its type, else nothing --
/// and a node only looks at the rules filed under its own id, its own
/// classes, its own type, and the few filed under nothing. The rest of the
/// sheet is never visited.
///
/// #### What an ancestor can change
///
/// [#ancestorIds], [#ancestorClasses], [#ancestorTypes] and
/// [#ancestorPseudos] hold every name some rule requires of a compound that
/// is NOT its subject. A node whose id, classes, type and states are in
/// none of them cannot change which rules its descendants match, which is
/// what lets the engine restyle one node without walking its subtree.
final class CssSheet {

    /// One declaration, with what the engine caches for it.
    static final class Decl {
        final String name;
        final int id;
        final int kind;
        final CssValue value;
        final boolean important;
        /// The object a value that depends on no node decodes to, once made.
        Object decoded;

        Decl(CssDeclaration d) {
            this.name = d.name();
            this.id = CssEngine.propertyId(d.name());
            this.kind = d.kind();
            this.value = d.value();
            this.important = d.important();
        }
    }

    /// One selector and the declarations it brings.
    static final class Rule {
        final CssSelector selector;
        /// `pseudo[i][j]` is pseudo-class `j` of compound `i`.
        final PseudoClass[][] pseudo;
        final Decl[] decls;
        final int order;

        Rule(CssSelector selector, Decl[] decls, int order) {
            this.selector = selector;
            this.decls = decls;
            this.order = order;
            pseudo = new PseudoClass[selector.size()][];
            for (int i = 0; i < pseudo.length; i++) {
                pseudo[i] = new PseudoClass[selector.pseudoCount(i)];
                for (int j = 0; j < pseudo[i].length; j++) {
                    pseudo[i][j] = PseudoClass.getPseudoClass(selector.pseudo(i, j));
                }
            }
        }
    }

    private static final Rule[] NO_RULES = new Rule[0];

    final HashMap<String, Rule[]> byId = new HashMap<String, Rule[]>();
    final HashMap<String, Rule[]> byClass = new HashMap<String, Rule[]>();
    final HashMap<String, Rule[]> byType = new HashMap<String, Rule[]>();
    Rule[] universal = NO_RULES;
    final HashSet<String> ancestorIds = new HashSet<String>();
    final HashSet<String> ancestorClasses = new HashSet<String>();
    final HashSet<String> ancestorTypes = new HashSet<String>();
    PseudoClass[] ancestorPseudos = new PseudoClass[0];
    private int ruleCount;

    private CssSheet() {
    }

    /// How many rules the sheet holds, its imports included.
    int size() {
        return ruleCount;
    }

    /// Reads the compiled form of the style sheet at `path`, a resource
    /// path without a leading slash, with everything it imports. Answers
    /// `null` when the application ships no compiled sheet for that path.
    static CssSheet load(String path) throws IOException {
        CssSheet sheet = new CssSheet();
        ArrayList<Rule> rules = new ArrayList<Rule>();
        if (!sheet.read(path, rules, new HashSet<String>())) {
            return null;
        }
        sheet.index(rules);
        return sheet;
    }

    /// Builds a sheet from data already in memory; for tests and tools.
    static CssSheet of(CssSheetData data) {
        CssSheet sheet = new CssSheet();
        ArrayList<Rule> rules = new ArrayList<Rule>();
        sheet.add(data, rules);
        sheet.index(rules);
        return sheet;
    }

    private boolean read(String path, ArrayList<Rule> rules, HashSet<String> seen) throws IOException {
        if (!seen.add(path)) {
            // A sheet that imports itself, directly or not: once is enough.
            return true;
        }
        InputStream in = Resources.open(CssSheetData.compiledName(ResourceNames.flatName(path)));
        if (in == null) {
            return false;
        }
        CssSheetData data;
        try {
            data = CssSheetData.read(in);
        } finally {
            in.close();
        }
        // An import comes before the rules of the sheet that imports it,
        // so the importing sheet wins a tie.
        for (int i = 0; i < data.importCount(); i++) {
            if (!read(data.importPath(i), rules, seen)) {
                CssEngine.warnOnce("The style sheet " + path + " imports " + data.importPath(i)
                        + ", which was not compiled into the application");
            }
        }
        add(data, rules);
        return true;
    }

    private void add(CssSheetData data, ArrayList<Rule> rules) {
        Decl[][] blocks = new Decl[data.blockCount()][];
        for (int b = 0; b < blocks.length; b++) {
            blocks[b] = new Decl[data.declarationCount(b)];
            for (int i = 0; i < blocks[b].length; i++) {
                blocks[b][i] = new Decl(data.declaration(b, i));
            }
        }
        for (int i = 0; i < data.ruleCount(); i++) {
            rules.add(new Rule(data.selector(i), blocks[data.blockOf(i)], rules.size()));
        }
    }

    private void index(ArrayList<Rule> rules) {
        ruleCount = rules.size();
        HashMap<String, ArrayList<Rule>> ids = new HashMap<String, ArrayList<Rule>>();
        HashMap<String, ArrayList<Rule>> classes = new HashMap<String, ArrayList<Rule>>();
        HashMap<String, ArrayList<Rule>> types = new HashMap<String, ArrayList<Rule>>();
        ArrayList<Rule> any = new ArrayList<Rule>();
        ArrayList<PseudoClass> pseudos = new ArrayList<PseudoClass>();
        for (int r = 0; r < rules.size(); r++) {
            Rule rule = rules.get(r);
            CssSelector s = rule.selector;
            if (s.id(0) != null) {
                file(ids, s.id(0), rule);
            } else if (s.classCount(0) > 0) {
                file(classes, s.styleClass(0, 0), rule);
            } else if (s.type(0) != null) {
                file(types, s.type(0), rule);
            } else {
                any.add(rule);
            }
            for (int c = 1; c < s.size(); c++) {
                if (s.id(c) != null) {
                    ancestorIds.add(s.id(c));
                }
                if (s.type(c) != null) {
                    ancestorTypes.add(s.type(c));
                }
                for (int j = 0; j < s.classCount(c); j++) {
                    ancestorClasses.add(s.styleClass(c, j));
                }
                for (int j = 0; j < rule.pseudo[c].length; j++) {
                    if (!pseudos.contains(rule.pseudo[c][j])) {
                        pseudos.add(rule.pseudo[c][j]);
                    }
                }
            }
        }
        freeze(ids, byId);
        freeze(classes, byClass);
        freeze(types, byType);
        universal = any.toArray(NO_RULES);
        ancestorPseudos = pseudos.toArray(new PseudoClass[pseudos.size()]);
    }

    private static void file(HashMap<String, ArrayList<Rule>> into, String key, Rule rule) {
        ArrayList<Rule> list = into.get(key);
        if (list == null) {
            list = new ArrayList<Rule>();
            into.put(key, list);
        }
        list.add(rule);
    }

    private static void freeze(HashMap<String, ArrayList<Rule>> from, HashMap<String, Rule[]> into) {
        for (java.util.Map.Entry<String, ArrayList<Rule>> e : from.entrySet()) {
            into.put(e.getKey(), e.getValue().toArray(NO_RULES));
        }
    }
}
