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
package com.codename1.tools.javac;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One compilation: parses the sources, enters and attributes them, and generates
 * class files. Holds the state the phases share.
 */
final class Compiler {
    final Symtab symtab;
    final Types types;
    final Log log = new Log();
    final Enter enter;
    final Attr attr;
    /** Java language level the source is checked against. */
    int sourceLevel = 21;
    /** While positive, errors are counted but not reported (speculative attribution). */
    int quiet;
    private int quietErrors;
    private final List<Object[]> captures = new ArrayList<Object[]>();
    final List<Tree.CompilationUnit> units = new ArrayList<Tree.CompilationUnit>();

    Compiler(ClassLibrary library) {
        this.symtab = new Symtab(library);
        this.types = new Types(symtab);
        this.enter = new Enter(this);
        this.attr = new Attr(this);
    }

    void error(Tree.CompilationUnit unit, int pos, String message) {
        if (quiet > 0) {
            quietErrors++;
            return;
        }
        if (unit == null) {
            log.diagnostics.add(new Diagnostic("<unknown>", 0, 0, message, true));
            log.errorCount++;
            return;
        }
        // One message per position: the same mistake reached twice (a re-attributed constant) reports once.
        for (Diagnostic d : log.diagnostics) {
            if (d.error && d.file.equals(unit.source.name) && d.line == unit.source.line(pos)
                    && d.column == unit.source.column(pos) && d.message.equals(message)) {
                return;
            }
        }
        log.error(unit.source, pos, message);
    }

    void warning(Tree.CompilationUnit unit, int pos, String message) {
        if (quiet > 0 || unit == null) {
            return;
        }
        log.warning(unit.source, pos, message);
    }

    int errorCount() {
        return log.errorCount + quietErrors;
    }

    /** A local read from inside a lambda or inner class: checked for effective finality after attribution. */
    void recordCapture(VarSymbol v, Tree.CompilationUnit unit, int pos, boolean fromLambda) {
        if (quiet > 0) {
            return;
        }
        captures.add(new Object[]{v, unit, Integer.valueOf(pos), Boolean.valueOf(fromLambda)});
    }

    private void checkCaptures() {
        for (Object[] c : captures) {
            VarSymbol v = (VarSymbol) c[0];
            if (!v.isEffectivelyFinal()) {
                error((Tree.CompilationUnit) c[1], ((Integer) c[2]).intValue(), "local variables referenced from "
                        + (((Boolean) c[3]).booleanValue() ? "a lambda expression" : "an inner class")
                        + " must be final or effectively final");
            }
        }
    }

    /** {owner, name, desc, toOwner, toName}: see JavaCompiler.redirectCall. */
    final List<String[]> redirects = new ArrayList<String[]>();
    /** Imports every compilation unit gets after its own ({@code a.b.*} or {@code a.b.C}). */
    final List<String> defaultImports = new ArrayList<String>();
    /** Errors reported while parsing, before any name was resolved. */
    int syntaxErrors;
    /** Script specs by source, for sources added as scripts. */
    final Map<Source, ScriptSpec> scripts = new java.util.IdentityHashMap<Source, ScriptSpec>();

    /** Parses, attributes and (when error free) generates; returns class files by internal name. */
    Map<String, byte[]> compile(List<Source> sources) {
        for (Source s : sources) {
            Tree.CompilationUnit unit;
            try {
                // The lexer tokenizes eagerly in the constructor, so a lexical error
                // (an unclosed string, say, while the user is still typing) surfaces
                // here; it has already been logged at its position.
                JavaSourceParser parser = new JavaSourceParser(s, log);
                parser.script = scripts.get(s);
                unit = parser.parseCompilationUnit();
            } catch (CompileError e) {
                unit = null;
            }
            if (unit != null) {
                for (String d : defaultImports) {
                    Tree.Import imp = new Tree.Import();
                    imp.pos = 0;
                    if (d.startsWith("static ")) {
                        imp.isStatic = true;
                        d = d.substring(7);
                    }
                    imp.onDemand = d.endsWith(".*");
                    imp.name = imp.onDemand ? d.substring(0, d.length() - 2) : d;
                    unit.imports.add(imp);
                }
                units.add(unit);
            }
        }
        Map<String, byte[]> out = new LinkedHashMap<String, byte[]>();
        syntaxErrors = log.errorCount;
        if (log.errorCount > 0) {
            return out;
        }
        try {
            enter.enterUnits(units);
            for (int i = 0; i < enter.sourceClasses.size(); i++) {
                ClassSymbol c = enter.sourceClasses.get(i);
                if (c.declEnv == null) {
                    attr.attribClass(c);
                }
            }
            // Flow first: it decides which locals declared without an initializer are
            // effectively final (assigned only while definitely unassigned), which the
            // capture check reads.
            if (log.errorCount == 0) {
                Flow flow = new Flow(this);
                for (int i = 0; i < enter.sourceClasses.size(); i++) {
                    flow.checkClass(enter.sourceClasses.get(i));
                }
            }
            checkCaptures();
        } catch (CompileError e) {
            error(null, 0, e.getMessage());
        }
        if (log.errorCount > 0) {
            return out;
        }
        Gen gen = new Gen(this);
        try {
            for (int i = 0; i < enter.sourceClasses.size(); i++) {
                ClassSymbol c = enter.sourceClasses.get(i);
                byte[] bytes = gen.genClass(c);
                if (bytes != null) {
                    out.put(c.internalName, bytes);
                }
            }
        } catch (CompileError e) {
            // "code too large" and the like: a diagnostic, never an exception to the caller.
            error(null, 0, e.getMessage());
        }
        if (log.errorCount > 0) {
            out.clear();
        }
        return out;
    }
}
