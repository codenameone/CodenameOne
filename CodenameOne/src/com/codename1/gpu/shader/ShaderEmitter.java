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
package com.codename1.gpu.shader;

import java.util.HashMap;
import java.util.Map;

/// Prints `ShaderNode` graphs as statements of one shading language. This is
/// the part of a backend's shader generator that custom materials need;
/// applications do not call it.
///
/// A backend creates one emitter per shader stage, calls `emit` for each hook
/// it splices in, and uses the returned expression where the engine's own
/// value would have gone. Every node that is not a constant or an input
/// becomes one temporary, so a node shared by several hooks is computed once.
public final class ShaderEmitter {
    /// The shading languages a graph prints as.
    public enum Dialect {
        /// GLSL ES 1.00: OpenGL ES 2, WebGL 1 and desktop GL.
        GLSL,
        /// The Metal Shading Language.
        MSL,
        /// HLSL shader model 4 and later.
        HLSL
    }

    /// Supplies the backend's spelling of the inputs a graph reads.
    public interface Inputs {
        /// Returns the expression for a built-in input such as `worldPos`, or
        /// null when the stage being emitted does not have it.
        ///
        /// #### Parameters
        ///
        /// - `name`: the built-in's name, as the `ShaderNode` factories use it
        ///
        /// #### Returns
        ///
        /// the expression, or null
        String builtin(String name);

        /// Returns the expression for slot `slot` of the material's uniform
        /// array, a four float value of which the graph reads the first
        /// `components`.
        ///
        /// #### Parameters
        ///
        /// - `slot`: the uniform slot
        ///
        /// #### Returns
        ///
        /// the expression of the whole vec4
        String uniform(int slot);

        /// Returns the expression sampling custom texture `slot` at `uv`, or
        /// null when the stage cannot sample.
        ///
        /// #### Parameters
        ///
        /// - `slot`: the texture slot
        ///
        /// - `uv`: the coordinate expression
        ///
        /// #### Returns
        ///
        /// the expression, or null
        String sample(int slot, String uv);
    }

    private final Dialect dialect;
    private final ShaderMaterial material;
    private final Inputs inputs;
    private final StringBuilder out; // NOPMD AvoidStringBufferField - the caller's source buffer; an emitter lives for one generation
    private final String indent;
    private final String prefix;
    private final Map<ShaderNode, String> emitted = new HashMap<ShaderNode, String>();
    private int next;

    /// Creates an emitter that appends statements to `out`.
    ///
    /// #### Parameters
    ///
    /// - `dialect`: the language to print
    ///
    /// - `material`: the material whose uniforms and textures the graphs read
    ///
    /// - `inputs`: the backend's spelling of the inputs
    ///
    /// - `out`: where the temporaries are declared
    ///
    /// - `indent`: the indentation of each statement
    ///
    /// - `prefix`: a prefix for temporary names, distinct per stage
    public ShaderEmitter(Dialect dialect, ShaderMaterial material, Inputs inputs, StringBuilder out,
            String indent, String prefix) {
        this.dialect = dialect;
        this.material = material;
        this.inputs = inputs;
        this.out = out;
        this.indent = indent;
        this.prefix = prefix;
    }

    /// Declares whatever temporaries `node` needs and returns the expression
    /// that holds its value.
    ///
    /// #### Parameters
    ///
    /// - `node`: the graph
    ///
    /// #### Returns
    ///
    /// an expression of the node's type
    public String emit(ShaderNode node) {
        String done = emitted.get(node);
        if (done != null) {
            return done;
        }
        String expr = expression(node);
        int kind = node.getKind();
        if (kind == ShaderNode.CONST || kind == ShaderNode.BUILTIN || kind == ShaderNode.UNIFORM) {
            emitted.put(node, expr);
            return expr;
        }
        String name = prefix + (next++);
        out.append(indent).append(typeName(node.getType())).append(' ').append(name)
                .append(" = ").append(expr).append(";\n");
        emitted.put(node, name);
        return name;
    }

    private String expression(ShaderNode n) {
        switch (n.getKind()) {
            case ShaderNode.CONST:
                return constant(n.getType(), n.getValues());
            case ShaderNode.BUILTIN: {
                String b = inputs.builtin(n.getName());
                if (b == null) {
                    throw new IllegalArgumentException("ShaderNode." + n.getName()
                            + "() is not available in this stage");
                }
                return b;
            }
            case ShaderNode.UNIFORM: {
                int slot = material.getUniformSlot(n.getName());
                if (slot < 0) {
                    throw new IllegalArgumentException("Uniform " + n.getName()
                            + " is not declared on the material");
                }
                if (material.getUniformType(n.getName()) != n.getType()) {
                    throw new IllegalArgumentException("Uniform " + n.getName() + " is declared as a "
                            + material.getUniformType(n.getName()) + " but read as a " + n.getType());
                }
                String all = inputs.uniform(slot);
                return n.getType() == ShaderType.VEC4 ? all : all + "." + "xyzw".substring(0, n.getType().getComponents());
            }
            case ShaderNode.SAMPLE: {
                int slot = material.getTextureSlot(n.getName());
                if (slot < 0) {
                    throw new IllegalArgumentException("Texture " + n.getName()
                            + " is not declared on the material");
                }
                String s = inputs.sample(slot, emit(n.getArg(0)));
                if (s == null) {
                    throw new IllegalArgumentException("Textures cannot be sampled in this stage");
                }
                return s;
            }
            case ShaderNode.BINARY:
                return "(" + emit(n.getArg(0)) + " " + n.getName() + " " + emit(n.getArg(1)) + ")";
            case ShaderNode.NEGATE:
                return "(-" + emit(n.getArg(0)) + ")";
            case ShaderNode.SWIZZLE:
                return emit(n.getArg(0)) + "." + n.getName();
            case ShaderNode.SELECT:
                return "(" + emit(n.getArg(0)) + " > 0.5 ? " + emit(n.getArg(1)) + " : " + emit(n.getArg(2)) + ")";
            case ShaderNode.CONSTRUCT: {
                StringBuilder sb = new StringBuilder(typeName(n.getType())).append('(');
                for (int i = 0; i < n.getArgCount(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(emit(n.getArg(i)));
                }
                return sb.append(')').toString();
            }
            case ShaderNode.CALL: {
                StringBuilder sb = new StringBuilder(function(n.getName())).append('(');
                for (int i = 0; i < n.getArgCount(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(emit(n.getArg(i)));
                }
                return sb.append(')').toString();
            }
            default:
                throw new IllegalStateException("Unknown node kind " + n.getKind());
        }
    }

    private String constant(ShaderType t, float[] v) {
        if (t == ShaderType.FLOAT) {
            return literal(v[0]);
        }
        StringBuilder sb = new StringBuilder(typeName(t)).append('(');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(literal(v[i]));
        }
        return sb.append(')').toString();
    }

    /// Returns a float literal every dialect reads the same way.
    ///
    /// #### Parameters
    ///
    /// - `v`: a finite value
    ///
    /// #### Returns
    ///
    /// the literal
    public static String literal(float v) {
        if (Float.isNaN(v) || Float.isInfinite(v)) {
            throw new IllegalArgumentException("A shader constant must be finite: " + v);
        }
        String s = Float.toString(v);
        if (s.indexOf('.') < 0 && s.indexOf('E') < 0) {
            s = s + ".0";
        }
        return v < 0 ? "(" + s + ")" : s;
    }

    /// Returns the dialect's name of a type.
    ///
    /// #### Parameters
    ///
    /// - `t`: the type
    ///
    /// #### Returns
    ///
    /// the type name
    public String typeName(ShaderType t) {
        return typeName(dialect, t);
    }

    /// Returns a dialect's name of a type.
    ///
    /// #### Parameters
    ///
    /// - `d`: the dialect
    ///
    /// - `t`: the type
    ///
    /// #### Returns
    ///
    /// the type name
    public static String typeName(Dialect d, ShaderType t) {
        if (t == ShaderType.FLOAT) {
            return "float";
        }
        return (d == Dialect.GLSL ? "vec" : "float") + t.getComponents();
    }

    private String function(String fn) {
        if (dialect == Dialect.HLSL) {
            if ("fract".equals(fn)) {
                return "frac";
            }
            if ("mix".equals(fn)) {
                return "lerp";
            }
        }
        if (dialect == Dialect.GLSL && "atan2".equals(fn)) {
            return "atan";
        }
        return fn;
    }

    /// Returns true if any hook of the given stage reads the built-in input.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material, or null
    ///
    /// - `fragment`: true for the fragment stage
    ///
    /// - `builtin`: the built-in's name, such as `worldPos`
    ///
    /// #### Returns
    ///
    /// true if it is read
    public static boolean uses(ShaderMaterial material, boolean fragment, String builtin) {
        if (material == null) {
            return false;
        }
        ShaderMaterial.Hook[] all = ShaderMaterial.Hook.values();
        for (ShaderMaterial.Hook h : all) {
            ShaderNode g = material.get(h);
            if (g != null && h.isFragment() == fragment && reads(g, builtin, new HashMap<ShaderNode, Boolean>())) {
                return true;
            }
        }
        return false;
    }

    /// Returns true if any hook of either stage reads the built-in input.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material, or null
    ///
    /// - `builtin`: the built-in's name
    ///
    /// #### Returns
    ///
    /// true if it is read
    public static boolean uses(ShaderMaterial material, String builtin) {
        return uses(material, true, builtin) || uses(material, false, builtin);
    }

    private static boolean reads(ShaderNode n, String builtin, Map<ShaderNode, Boolean> seen) {
        if (seen.containsKey(n)) {
            return false;
        }
        seen.put(n, Boolean.TRUE);
        if (n.getKind() == ShaderNode.BUILTIN && builtin.equals(n.getName())) {
            return true;
        }
        for (int i = 0; i < n.getArgCount(); i++) {
            if (reads(n.getArg(i), builtin, seen)) {
                return true;
            }
        }
        return false;
    }

    /// Appends a structural description of a graph: equal for two graphs
    /// that print the same program. Shared nodes are written once and then
    /// referred to by number.
    ///
    /// #### Parameters
    ///
    /// - `root`: the graph
    ///
    /// - `sb`: where to append
    public static void describe(ShaderNode root, StringBuilder sb) {
        describe(root, sb, new HashMap<ShaderNode, Integer>());
    }

    private static void describe(ShaderNode n, StringBuilder sb, Map<ShaderNode, Integer> ids) {
        Integer id = ids.get(n);
        if (id != null) {
            sb.append('#').append(id.intValue());
            return;
        }
        ids.put(n, Integer.valueOf(ids.size()));
        sb.append(n.getKind()).append(n.getType().getComponents());
        if (n.getName() != null) {
            sb.append(n.getName());
        }
        float[] v = n.getValues();
        if (v != null) {
            for (float x : v) {
                sb.append(',').append(x);
            }
        }
        if (n.getArgCount() > 0) {
            sb.append('(');
            for (int i = 0; i < n.getArgCount(); i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                describe(n.getArg(i), sb, ids);
            }
            sb.append(')');
        }
    }
}
