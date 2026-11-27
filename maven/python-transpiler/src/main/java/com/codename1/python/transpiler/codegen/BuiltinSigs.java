/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.transpiler.codegen;

import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.Constant;

import java.util.List;

/// What the runtime's Java builtins return, for the inference.
///
/// A builtin written in Java (`_cn1re.pattern_search`, a `re.Match`'s
/// `group`) is opaque to the pass, which would give its result no type and
/// every value computed from it none either. Its result type is fixed by
/// what the Java code does, so it is written down here, keyed by module and
/// function (or builtin type and method), as a function of the argument
/// types. A result the arguments do not determine is null: the pass keeps
/// the value untyped. Typed code converts the generic result to the type
/// given here with a checked conversion, so an entry that is wrong raises
/// rather than misreading a value.
///
/// A `re.Match` carries the type of the string it matched in its name
/// (`re.Match[str]`, `re.Match[bytes]`): its groups are slices of that.
final class BuiltinSigs {
    static final String MATCH_STR = "re.Match[str]";

    private BuiltinSigs() {
    }

    /// The type `q` (a dotted module function, or `type.method`) returns
    /// for these positional argument types, the receiver first for a
    /// method found on a class; null when not known.
    static Ty ret(String q, List<Ty> args, Call c) {
        switch (q) {
            case "_cn1re.pattern_match":
            case "_cn1re.pattern_search":
            case "_cn1re.pattern_fullmatch":
                // (pattern, string, pos, endpos): a match of `string`, or None.
                return args.size() >= 2 && args.get(1).k == Ty.K.STR && !args.get(1).opt
                        ? Ty.obj(MATCH_STR).orNone() : null;
            case "_cn1re.sub":
                // (prog, pattern, repl, string, count, subn): the new string,
                // or with `subn` true the (string, count) tuple.
                return args.size() == 6 && args.get(3).k == Ty.K.STR && !args.get(3).opt
                        && c.args.get(5) instanceof Constant && ((Constant) c.args.get(5)).kind == Constant.Kind.FALSE
                        ? Ty.STR : null;
            case MATCH_STR + ".group":
                // One group (or the whole match): its text, None when it did
                // not take part. More than one is a tuple.
                return args.size() <= 1 ? Ty.STR.orNone() : null;
            case MATCH_STR + ".start":
            case MATCH_STR + ".end":
                return Ty.INT;
            default:
                return null;
        }
    }
}
