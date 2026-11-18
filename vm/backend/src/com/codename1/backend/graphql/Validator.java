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
package com.codename1.backend.graphql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The specification's validation rules, run before a document executes.
///
/// A document that fails any of them is not executed at all: the response has
/// only `errors`, as the specification requires. Covered: operation names,
/// lone anonymous operations, single-field subscriptions, field existence on
/// the parent type, leaf and composite selections, known and required
/// arguments, argument literal types, known and possible fragments, fragment
/// cycles, unused fragments, defined, unique, used and type-compatible
/// variables, known directives, and conflicting fields under one response key.
/// One rule is the server's own: the depth limit.
final class Validator {
    private final GraphQLSchema schema;
    private final Parser.Document doc;
    private final Values values;
    private final boolean introspection;
    private final int maxDepth;
    private final int maxFields;
    /// Fields validated so far, every fragment spread expanded where it is
    /// used: what executing the document would resolve, at most.
    private int fieldCount;
    private final List<GraphQLException> errors = new ArrayList<GraphQLException>();

    // Per operation.
    private Map<String, Parser.VariableDef> variableDefs;
    private Set<String> usedVariables;
    private Set<String> usedFragments = new HashSet<String>();
    private final Set<String> cycleChecked = new HashSet<String>();
    private final Set<String> cyclic = new HashSet<String>();

    Validator(GraphQLSchema schema, Parser.Document doc, Values values, boolean introspection,
              int maxDepth, int maxFields) {
        this.schema = schema;
        this.maxFields = maxFields;
        this.doc = doc;
        this.values = values;
        this.introspection = introspection;
        this.maxDepth = maxDepth;
    }

    List<GraphQLException> validate() {
        Set<String> names = new HashSet<String>();
        for (Parser.Operation op : doc.operations) {
            if (op.name == null && doc.operations.size() > 1) {
                error("This anonymous operation must be the only defined operation.", op);
            }
            if (op.name != null && !names.add(op.name)) {
                error("There can be only one operation named \"" + op.name + "\".", op);
            }
        }
        for (Parser.Fragment f : doc.fragments.values()) {
            SchemaType t = schema.type(f.typeCondition);
            if (t == null) {
                error("Unknown type \"" + f.typeCondition + "\".", f);
            } else if (!t.isComposite()) {
                error("Fragment \"" + f.name + "\" cannot condition on non composite type \""
                        + f.typeCondition + "\".", f);
            }
            checkCycles(f, new ArrayList<String>());
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        for (Parser.Operation op : doc.operations) {
            validateOperation(op);
        }
        for (Parser.Fragment f : doc.fragments.values()) {
            // Past the field budget the walk stopped early, so a fragment it
            // never reached is not "unused".
            if (fieldCount <= maxFields && !usedFragments.contains(f.name)) {
                error("Fragment \"" + f.name + "\" is never used.", f);
            }
        }
        return errors;
    }

    /// Depth first with a done set, so every fragment is walked once: without
    /// it a chain of fragments each spreading the next twice is exponential.
    private void checkCycles(Parser.Fragment f, List<String> stack) {
        if (stack.contains(f.name)) {
            if (cyclic.add(f.name)) {
                error("Cannot spread fragment \"" + f.name + "\" within itself.", f);
            }
            return;
        }
        if (!cycleChecked.add(f.name)) {
            return;
        }
        stack.add(f.name);
        List<Parser.FragmentSpread> spreads = new ArrayList<Parser.FragmentSpread>();
        spreads(f.selections, spreads);
        for (Parser.FragmentSpread s : spreads) {
            Parser.Fragment next = doc.fragments.get(s.name);
            if (next != null && errors.isEmpty()) {
                checkCycles(next, stack);
            }
        }
        stack.remove(stack.size() - 1);
    }

    private static void spreads(List<Parser.Selection> sels, List<Parser.FragmentSpread> out) {
        if (sels == null) {
            return;
        }
        for (Parser.Selection s : sels) {
            if (s instanceof Parser.FragmentSpread) {
                out.add((Parser.FragmentSpread) s);
            } else if (s instanceof Parser.InlineFragment) {
                spreads(((Parser.InlineFragment) s).selections, out);
            } else {
                spreads(((Parser.Field) s).selections, out);
            }
        }
    }

    private void validateOperation(Parser.Operation op) {
        SchemaType root = schema.rootType(op.type);
        if (root == null) {
            error("Schema is not configured for " + op.type + "s.", op);
            return;
        }
        variableDefs = new LinkedHashMap<String, Parser.VariableDef>();
        usedVariables = new HashSet<String>();
        for (Parser.VariableDef v : op.variables) {
            if (variableDefs.containsKey(v.name)) {
                error("There can be only one variable named \"$" + v.name + "\".", v);
                continue;
            }
            variableDefs.put(v.name, v);
            SchemaType t = schema.type(v.type.namedType());
            if (t == null) {
                error("Unknown type \"" + v.type.namedType() + "\".", v);
            } else if (!t.isInput()) {
                error("Variable \"$" + v.name + "\" cannot be non-input type \"" + v.type
                        + "\".", v);
            } else if (v.hasDefault) {
                try {
                    values.coerceLiteral(v.defaultValue, v.type, null,
                            "Variable \"$" + v.name + "\" default value");
                } catch (GraphQLException e) {
                    error(e.getMessage(), v);
                }
            }
        }
        // A fixed table, not toUpperCase(): that folds by the default locale.
        directives(op.directives, "query".equals(op.type) ? "QUERY"
                : "mutation".equals(op.type) ? "MUTATION" : "SUBSCRIPTION", op);
        if ("subscription".equals(op.type)) {
            Map<String, List<Parser.Field>> fields = new LinkedHashMap<String, List<Parser.Field>>();
            collect(root, op.selections, fields, new HashSet<String>());
            if (fields.size() != 1) {
                error((op.name == null ? "Anonymous Subscription" : "Subscription \"" + op.name
                        + "\"") + " must select only one top level field.", op);
            }
        }
        selectionSet(root, op.selections, 1, new HashSet<String>());
        for (Map.Entry<String, Parser.VariableDef> e : variableDefs.entrySet()) {
            String v = e.getKey();
            if (!usedVariables.contains(v)) {
                error("Variable \"$" + v + "\" is never used"
                        + (op.name == null ? "." : " in operation \"" + op.name + "\"."),
                        e.getValue());
            }
        }
    }

    /// Validates one selection set. `fragmentsOnPath` stops re-validating a
    /// fragment within its own expansion; cycles were reported already.
    private void selectionSet(SchemaType parent, List<Parser.Selection> sels, int depth,
                              Set<String> fragmentsOnPath) {
        if (fieldCount > maxFields) {
            // Over budget: expanding further is the work the budget refuses.
            return;
        }
        if (depth > maxDepth) {
            error("The query exceeds the maximum depth of " + maxDepth + ".", sels.get(0));
            return;
        }
        Map<String, Parser.Field> byKey = new HashMap<String, Parser.Field>();
        Map<String, SchemaType> parentOfKey = new HashMap<String, SchemaType>();
        conflicts(parent, sels, byKey, parentOfKey, new HashSet<String>());
        for (Parser.Selection s : sels) {
            if (fieldCount > maxFields) {
                return;
            }
            if (s instanceof Parser.Field) {
                field(parent, (Parser.Field) s, depth, fragmentsOnPath);
            } else if (s instanceof Parser.InlineFragment) {
                Parser.InlineFragment f = (Parser.InlineFragment) s;
                directives(f.directives, "INLINE_FRAGMENT", f);
                SchemaType t = parent;
                if (f.typeCondition != null) {
                    t = schema.type(f.typeCondition);
                    if (t == null) {
                        error("Unknown type \"" + f.typeCondition + "\".", f);
                        continue;
                    }
                    if (!t.isComposite()) {
                        error("Fragment cannot condition on non composite type \""
                                + f.typeCondition + "\".", f);
                        continue;
                    }
                    if (!overlap(parent, t)) {
                        error("Fragment cannot be spread here as objects of type \""
                                + parent.name + "\" can never be of type \"" + t.name + "\".", f);
                        continue;
                    }
                }
                selectionSet(t, f.selections, depth, fragmentsOnPath);
            } else {
                Parser.FragmentSpread sp = (Parser.FragmentSpread) s;
                directives(sp.directives, "FRAGMENT_SPREAD", sp);
                Parser.Fragment f = doc.fragments.get(sp.name);
                if (f == null) {
                    error("Unknown fragment \"" + sp.name + "\".", sp);
                    continue;
                }
                usedFragments.add(f.name);
                SchemaType t = schema.type(f.typeCondition);
                if (!overlap(parent, t)) {
                    error("Fragment \"" + f.name + "\" cannot be spread here as objects of type \""
                            + parent.name + "\" can never be of type \"" + t.name + "\".", sp);
                    continue;
                }
                if (fragmentsOnPath.add(f.name)) {
                    directives(f.directives, "FRAGMENT_DEFINITION", f);
                    selectionSet(t, f.selections, depth, fragmentsOnPath);
                    fragmentsOnPath.remove(f.name);
                }
            }
        }
    }

    /// Two fields under one response key, on one parent type, must be the same
    /// field with the same arguments -- the core of OverlappingFieldsCanBeMerged.
    private void conflicts(SchemaType parent, List<Parser.Selection> sels,
                           Map<String, Parser.Field> byKey, Map<String, SchemaType> parentOfKey,
                           Set<String> seenFragments) {
        for (Parser.Selection s : sels) {
            if (s instanceof Parser.Field) {
                Parser.Field f = (Parser.Field) s;
                String key = f.responseKey();
                Parser.Field other = byKey.get(key);
                if (other == null) {
                    byKey.put(key, f);
                    parentOfKey.put(key, parent);
                } else if (parentOfKey.get(key) == parent // NOPMD CompareObjectsWithEquals - one instance per type
                        || !parent.kind.equals(SchemaType.OBJECT)
                        || !parentOfKey.get(key).kind.equals(SchemaType.OBJECT)) {
                    if (!other.name.equals(f.name)) {
                        error("Fields \"" + key + "\" conflict because \"" + other.name
                                + "\" and \"" + f.name + "\" are different fields.", f);
                    } else if (!Values.literalToPlain(other.arguments)
                            .equals(Values.literalToPlain(f.arguments))) {
                        error("Fields \"" + key + "\" conflict because they have differing "
                                + "arguments.", f);
                    }
                }
            } else if (s instanceof Parser.InlineFragment) {
                Parser.InlineFragment f = (Parser.InlineFragment) s;
                SchemaType t = f.typeCondition == null ? parent : schema.type(f.typeCondition);
                if (t != null) {
                    conflicts(t, f.selections, byKey, parentOfKey, seenFragments);
                }
            } else {
                Parser.Fragment f = doc.fragments.get(((Parser.FragmentSpread) s).name);
                if (f != null && seenFragments.add(f.name)) {
                    SchemaType t = schema.type(f.typeCondition);
                    if (t != null) {
                        conflicts(t, f.selections, byKey, parentOfKey, seenFragments);
                    }
                }
            }
        }
    }

    private boolean overlap(SchemaType a, SchemaType b) {
        if (a == b) { // NOPMD CompareObjectsWithEquals - one instance per type
            return true;
        }
        Set<SchemaType> pa = possible(a);
        for (SchemaType t : possible(b)) {
            if (pa.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private Set<SchemaType> possible(SchemaType t) {
        Set<SchemaType> s = new HashSet<SchemaType>();
        if (SchemaType.OBJECT.equals(t.kind)) {
            s.add(t);
        } else {
            s.addAll(schema.possibleTypes(t));
        }
        return s;
    }

    private void field(SchemaType parent, Parser.Field f, int depth, Set<String> fragmentsOnPath) {
        fieldCount++;
        if (fieldCount > maxFields) {
            if (fieldCount == maxFields + 1) {
                // Aliases and repeated fragment spreads multiply what one
                // short document asks for; stop counting at the first excess.
                error("The query selects more than " + maxFields + " fields.", f);
            }
            return;
        }
        directives(f.directives, "FIELD", f);
        if ("__typename".equals(f.name)) {
            leaf(f, "__typename", "String!");
            noArguments(f);
            return;
        }
        FieldDef def = parent.fields.get(f.name);
        if (def == null && introspection && parent == schema.rootType("query")) { // NOPMD CompareObjectsWithEquals - one instance per type
            if ("__schema".equals(f.name)) {
                def = META_SCHEMA;
            } else if ("__type".equals(f.name)) {
                def = META_TYPE;
            }
        }
        if (def == null) {
            error("Cannot query field \"" + f.name + "\" on type \"" + parent.name + "\".", f);
            return;
        }
        arguments(def.args, f.arguments, "field \"" + parent.name + "." + f.name + "\"", f);
        SchemaType t = schema.type(def.type.namedType());
        if (t.isLeaf()) {
            leaf(f, f.name, def.type.toString());
        } else if (f.selections == null) {
            error("Field \"" + f.name + "\" of type \"" + def.type
                    + "\" must have a selection of subfields. Did you mean \"" + f.name
                    + " { ... }\"?", f);
        } else {
            selectionSet(t, f.selections, depth + 1, fragmentsOnPath);
        }
    }

    private void leaf(Parser.Field f, String name, String type) {
        if (f.selections != null) {
            error("Field \"" + name + "\" must not have a selection since type \"" + type
                    + "\" has no subfields.", f);
        }
    }

    private void noArguments(Parser.Field f) {
        for (String a : f.arguments.keySet()) {
            error("Unknown argument \"" + a + "\" on field \"" + f.name + "\".", f);
        }
    }

    private void arguments(Map<String, InputValueDef> defs, Map<String, Object> given,
                           String owner, Parser.Node at) {
        for (Map.Entry<String, Object> e : given.entrySet()) {
            InputValueDef d = defs.get(e.getKey());
            if (d == null) {
                error("Unknown argument \"" + e.getKey() + "\" on " + owner + ".", at);
                continue;
            }
            value(e.getValue(), d.type, d.hasDefault, "Argument \"" + e.getKey() + "\" of "
                    + owner, at);
        }
        for (InputValueDef d : defs.values()) {
            if (d.type.isNonNull() && !d.hasDefault && !given.containsKey(d.name)) {
                error(cap(owner) + " argument \"" + d.name + "\" of type \"" + d.type
                        + "\" is required, but it was not provided.", at);
            }
        }
    }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /// Checks a literal against `type`: variables by their declared type, and
    /// everything else by coercing it with no variables in scope.
    private void value(Object v, TypeRef type, boolean locationHasDefault, String where,
                       Parser.Node at) {
        List<Parser.Variable> vars = new ArrayList<Parser.Variable>();
        variablesIn(v, vars);
        if (v instanceof Parser.Variable) {
            variableUsage((Parser.Variable) v, type, locationHasDefault, at);
            return;
        }
        for (Parser.Variable var : vars) {
            usedVariables.add(var.name);
            if (!variableDefs.containsKey(var.name)) {
                error("Variable \"$" + var.name + "\" is not defined.", at);
                return;
            }
        }
        if (vars.isEmpty()) {
            try {
                values.coerceLiteral(v, type, null, where);
            } catch (GraphQLException e) {
                error(e.getMessage(), at);
            }
        } else {
            // A nested variable: check the shape around it, and each variable
            // where it sits.
            nested(v, type, where, at);
        }
    }

    private void nested(Object v, TypeRef type, String where, Parser.Node at) {
        if (v instanceof Parser.Variable) {
            variableUsage((Parser.Variable) v, type, false, at);
            return;
        }
        TypeRef nn = type.nullable();
        if (nn.kind == TypeRef.LIST && v instanceof List) {
            for (Object o : (List) v) {
                nested(o, nn.ofType, where, at);
            }
            return;
        }
        SchemaType t = nn.kind == TypeRef.NAMED ? schema.type(nn.name) : null;
        if (t != null && SchemaType.INPUT_OBJECT.equals(t.kind) && v instanceof Map) {
            Map m = (Map) v;
            for (Object o : m.entrySet()) {
                Map.Entry e = (Map.Entry) o;
                Object k = e.getKey();
                InputValueDef f = t.inputFields.get(String.valueOf(k));
                if (f == null) {
                    error(where + ": field \"" + k + "\" is not defined by type \"" + t.name
                            + "\".", at);
                } else {
                    nested(e.getValue(), f.type, where + "." + k, at);
                }
            }
            for (InputValueDef f : t.inputFields.values()) {
                if (f.type.isNonNull() && !f.hasDefault && !m.containsKey(f.name)) {
                    error(where + ": field \"" + t.name + "." + f.name + "\" of required type \""
                            + f.type + "\" was not provided.", at);
                }
            }
            return;
        }
        try {
            values.coerceLiteral(v, type, null, where);
        } catch (GraphQLException e) {
            error(e.getMessage(), at);
        }
    }

    private void variableUsage(Parser.Variable var, TypeRef location, boolean locationHasDefault,
                               Parser.Node at) {
        usedVariables.add(var.name);
        Parser.VariableDef def = variableDefs.get(var.name);
        if (def == null) {
            error("Variable \"$" + var.name + "\" is not defined.", at);
            return;
        }
        TypeRef varType = def.type;
        if (location.isNonNull() && !varType.isNonNull()
                && ((def.hasDefault && def.defaultValue != null) || locationHasDefault)) {
            location = location.ofType;
        }
        if (!varType.compatibleWith(location)) {
            error("Variable \"$" + var.name + "\" of type \"" + varType
                    + "\" used in position expecting type \"" + location + "\".", at);
        }
    }

    private static void variablesIn(Object v, List<Parser.Variable> out) {
        if (v instanceof Parser.Variable) {
            out.add((Parser.Variable) v);
        } else if (v instanceof List) {
            for (Object o : (List) v) {
                variablesIn(o, out);
            }
        } else if (v instanceof Map) {
            for (Object o : ((Map) v).values()) {
                variablesIn(o, out);
            }
        }
    }

    private void directives(List<Parser.Directive> ds, String location, Parser.Node at) {
        if (ds == null) {
            return;
        }
        Set<String> seen = new HashSet<String>();
        for (Parser.Directive d : ds) {
            DirectiveDef def = schema.directives.get(d.name);
            if (def == null) {
                error("Unknown directive \"@" + d.name + "\".", at);
                continue;
            }
            if (!def.locations.contains(location)) {
                error("Directive \"@" + d.name + "\" may not be used on " + location + ".", at);
            }
            if (!def.repeatable && !seen.add(d.name)) {
                error("The directive \"@" + d.name + "\" can only be used once at this "
                        + "location.", at);
            }
            arguments(def.args, d.arguments, "directive \"@" + d.name + "\"", at);
        }
    }

    /// Collects the fields of a selection set as execution would, ignoring
    /// directives, for the single-root-field rule.
    private void collect(SchemaType parent, List<Parser.Selection> sels,
                         Map<String, List<Parser.Field>> out, Set<String> seen) {
        for (Parser.Selection s : sels) {
            if (s instanceof Parser.Field) {
                Parser.Field f = (Parser.Field) s;
                List<Parser.Field> l = out.get(f.responseKey());
                if (l == null) {
                    l = new ArrayList<Parser.Field>();
                    out.put(f.responseKey(), l);
                }
                l.add(f);
            } else if (s instanceof Parser.InlineFragment) {
                collect(parent, ((Parser.InlineFragment) s).selections, out, seen);
            } else {
                Parser.Fragment f = doc.fragments.get(((Parser.FragmentSpread) s).name);
                if (f != null && seen.add(f.name)) {
                    collect(parent, f.selections, out, seen);
                }
            }
        }
    }

    private void error(String message, Parser.Node at) {
        errors.add(new GraphQLException(message, at == null ? 0 : at.line,
                at == null ? 0 : at.column));
    }

    /// `__schema: __Schema!`, which every query root has when introspection is on.
    static final FieldDef META_SCHEMA = new FieldDef();
    /// `__type(name: String!): __Type`.
    static final FieldDef META_TYPE = new FieldDef();

    static {
        META_SCHEMA.name = "__schema";
        META_SCHEMA.type = TypeRef.nonNull(TypeRef.named("__Schema"));
        META_TYPE.name = "__type";
        META_TYPE.type = TypeRef.named("__Type");
        InputValueDef n = new InputValueDef();
        n.name = "name";
        n.type = TypeRef.nonNull(TypeRef.named("String"));
        META_TYPE.args.put("name", n);
    }
}
