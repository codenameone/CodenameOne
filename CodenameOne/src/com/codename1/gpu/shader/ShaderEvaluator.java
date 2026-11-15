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

import com.codename1.util.MathUtil;
import java.util.HashMap;
import java.util.Map;

/// Evaluates `ShaderNode` graphs on the CPU, with the same results the GPU
/// computes from the same graph. Uses:
///
/// - **Gameplay that follows a shader.** A boat that rides the waves a
///   vertex offset draws evaluates the same graph at its own position, so it
///   floats on the water the player sees rather than on an approximation.
/// - **The software renderer**, which has no shader compiler.
/// - **Tests** of a graph's values.
///
/// ```java
/// import com.codename1.gpu.shader.*;
/// import com.codename1.gpu.shaders.Water;
///
/// ShaderMaterial water = Water.ocean();
/// float now = 2.5f, boatX = 3f, boatZ = -4f;
/// ShaderEvaluator ev = new ShaderEvaluator(water)
///         .setInput("time", now)
///         .setInput("position", boatX, 0f, boatZ);
/// float lift = ev.evaluate(water.get(ShaderMaterial.Hook.VERTEX_OFFSET))[1];
/// ```
///
/// Inputs are the names of the `ShaderNode` built ins (`position`, `normal`,
/// `uv`, `worldPos`, `worldNormal`, `viewDir`, `cameraPos`, `sunDir`,
/// `sunColor`, `time`, `baseColor`); an input never set reads as zero.
/// Uniforms are read from the material at each evaluation, so a changed
/// value needs nothing else. A graph is compiled to a flat program the first
/// time it is evaluated and kept; evaluating it again allocates nothing
/// except through `evaluate(ShaderNode)`, which returns a new array.
///
/// An evaluator is not thread safe.
public final class ShaderEvaluator {
    /// Reads a material texture for the evaluator.
    public interface TextureSampler {
        /// Samples a texture.
        ///
        /// #### Parameters
        ///
        /// - `slot`: the texture's slot in its `ShaderMaterial`
        ///
        /// - `u`: the horizontal coordinate
        ///
        /// - `v`: the vertical coordinate
        ///
        /// - `out`: receives red, green, blue and alpha, 0 to 1
        void sample(int slot, float u, float v, float[] out);
    }

    private static final String[] INPUTS = {
        "position", "normal", "uv", "worldPos", "worldNormal", "viewDir",
        "cameraPos", "sunDir", "sunColor", "time", "baseColor"
    };

    // operation codes of a compiled instruction
    private static final int OP_CONST = 0;
    private static final int OP_INPUT = 1;
    private static final int OP_UNIFORM = 2;
    private static final int OP_SAMPLE = 3;
    private static final int OP_ADD = 4;
    private static final int OP_SUB = 5;
    private static final int OP_MUL = 6;
    private static final int OP_DIV = 7;
    private static final int OP_NEGATE = 8;
    private static final int OP_SWIZZLE = 9;
    private static final int OP_CONSTRUCT = 10;
    private static final int OP_SELECT = 11;
    private static final int OP_SIN = 12;
    private static final int OP_COS = 13;
    private static final int OP_ABS = 14;
    private static final int OP_FLOOR = 15;
    private static final int OP_FRACT = 16;
    private static final int OP_SQRT = 17;
    private static final int OP_EXP = 18;
    private static final int OP_NORMALIZE = 19;
    private static final int OP_LENGTH = 20;
    private static final int OP_DISTANCE = 21;
    private static final int OP_DOT = 22;
    private static final int OP_CROSS = 23;
    private static final int OP_REFLECT = 24;
    private static final int OP_MIN = 25;
    private static final int OP_MAX = 26;
    private static final int OP_POW = 27;
    private static final int OP_ATAN2 = 28;
    private static final int OP_CLAMP = 29;
    private static final int OP_MIX = 30;
    private static final int OP_SMOOTHSTEP = 31;
    private static final int OP_STEP = 32;

    private static final String[] CALLS = {
        "sin", "cos", "abs", "floor", "fract", "sqrt", "exp", "normalize", "length", "distance", "dot",
        "cross", "reflect", "min", "max", "pow", "atan2", "clamp", "mix", "smoothstep", "step"
    };

    // one compiled graph: instructions in dependency order, each writing four
    // floats of the register file at its own index
    private static final class Program {
        int[] op = new int[16];
        int[] comps = new int[16];
        int[][] args = new int[16][];
        int[] aux = new int[16];
        float[][] consts = new float[16][];
        int count;
        float[] regs;
    }

    private final ShaderMaterial material;
    private final float[] inputs = new float[INPUTS.length * 4];
    private final Map<ShaderNode, Program> programs = new HashMap<ShaderNode, Program>();
    private final float[] texel = new float[4];
    private TextureSampler sampler;

    /// Creates an evaluator for the graphs of a material.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material whose uniforms and textures the graphs
    ///   read, or null when they read none
    public ShaderEvaluator(ShaderMaterial material) {
        this.material = material;
    }

    /// Sets a built-in input.
    ///
    /// #### Parameters
    ///
    /// - `name`: the built-in's name, such as `worldPos`
    ///
    /// - `values`: its components; missing ones are zero
    ///
    /// #### Returns
    ///
    /// this evaluator
    public ShaderEvaluator setInput(String name, float... values) {
        int i = inputIndex(name);
        if (i < 0) {
            throw new IllegalArgumentException("Unknown shader input: " + name);
        }
        for (int c = 0; c < 4; c++) {
            inputs[i * 4 + c] = c < values.length ? values[c] : 0f;
        }
        return this;
    }

    /// Sets what reads the material's textures. Without one every texture
    /// reads as opaque white.
    ///
    /// #### Parameters
    ///
    /// - `sampler`: the sampler, or null
    ///
    /// #### Returns
    ///
    /// this evaluator
    public ShaderEvaluator setTextureSampler(TextureSampler sampler) {
        this.sampler = sampler;
        return this;
    }

    /// Evaluates a graph.
    ///
    /// #### Parameters
    ///
    /// - `node`: the graph
    ///
    /// #### Returns
    ///
    /// a new array of four floats; components past the node's type are zero
    public float[] evaluate(ShaderNode node) {
        float[] out = new float[4];
        evaluate(node, out);
        return out;
    }

    /// Evaluates a graph into an array, without allocating.
    ///
    /// #### Parameters
    ///
    /// - `node`: the graph
    ///
    /// - `out`: receives the value; at least as long as the node's type,
    ///   and up to four floats are written
    public void evaluate(ShaderNode node, float[] out) {
        Program p = programs.get(node);
        if (p == null) {
            p = new Program();
            compile(p, node, new HashMap<ShaderNode, Integer>());
            p.regs = new float[p.count * 4];
            programs.put(node, p);
        }
        run(p);
        int r = (p.count - 1) * 4;
        int n = Math.min(out.length, 4);
        for (int i = 0; i < n; i++) {
            out[i] = p.regs[r + i];
        }
    }

    /// Evaluates a `FLOAT` graph.
    ///
    /// #### Parameters
    ///
    /// - `node`: the graph
    ///
    /// #### Returns
    ///
    /// its value
    public float evaluateFloat(ShaderNode node) {
        evaluate(node, texel);
        return texel[0];
    }

    private static int inputIndex(String name) {
        for (int i = 0; i < INPUTS.length; i++) {
            if (INPUTS[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static int callOp(String name) {
        for (int i = 0; i < CALLS.length; i++) {
            if (CALLS[i].equals(name)) {
                return OP_SIN + i;
            }
        }
        throw new IllegalArgumentException("Unknown shader function " + name);
    }

    private int compile(Program p, ShaderNode n, Map<ShaderNode, Integer> done) {
        Integer known = done.get(n);
        if (known != null) {
            return known.intValue();
        }
        int argc = n.getArgCount();
        int[] a = new int[argc];
        for (int i = 0; i < argc; i++) {
            a[i] = compile(p, n.getArg(i), done);
        }
        int op;
        int aux = 0;
        float[] k = null;
        switch (n.getKind()) {
            case ShaderNode.CONST:
                op = OP_CONST;
                k = n.getValues();
                break;
            case ShaderNode.BUILTIN:
                op = OP_INPUT;
                aux = inputIndex(n.getName());
                break;
            case ShaderNode.UNIFORM:
                op = OP_UNIFORM;
                aux = material == null ? -1 : material.getUniformSlot(n.getName());
                if (aux < 0) {
                    throw new IllegalArgumentException("Uniform " + n.getName() + " is not declared on the material");
                }
                break;
            case ShaderNode.SAMPLE:
                op = OP_SAMPLE;
                aux = material == null ? -1 : material.getTextureSlot(n.getName());
                if (aux < 0) {
                    throw new IllegalArgumentException("Texture " + n.getName() + " is not declared on the material");
                }
                break;
            case ShaderNode.BINARY: {
                String o = n.getName();
                op = "+".equals(o) ? OP_ADD : "-".equals(o) ? OP_SUB : "*".equals(o) ? OP_MUL : OP_DIV;
                break;
            }
            case ShaderNode.NEGATE:
                op = OP_NEGATE;
                break;
            case ShaderNode.SWIZZLE: {
                op = OP_SWIZZLE;
                String sw = n.getName();
                for (int i = 0; i < sw.length(); i++) {
                    aux |= "xyzw".indexOf(sw.charAt(i)) << (i * 2);
                }
                break;
            }
            case ShaderNode.CONSTRUCT:
                op = OP_CONSTRUCT;
                break;
            case ShaderNode.SELECT:
                op = OP_SELECT;
                break;
            case ShaderNode.CALL:
                op = callOp(n.getName());
                break;
            default:
                throw new IllegalStateException("Unknown node kind " + n.getKind());
        }
        int idx = p.count;
        if (idx == p.op.length) {
            int cap = idx * 2;
            int[] no = new int[cap];
            int[] nc = new int[cap];
            int[][] na = new int[cap][];
            int[] nx = new int[cap];
            float[][] nk = new float[cap][];
            System.arraycopy(p.op, 0, no, 0, idx);
            System.arraycopy(p.comps, 0, nc, 0, idx);
            System.arraycopy(p.args, 0, na, 0, idx);
            System.arraycopy(p.aux, 0, nx, 0, idx);
            System.arraycopy(p.consts, 0, nk, 0, idx);
            p.op = no;
            p.comps = nc;
            p.args = na;
            p.aux = nx;
            p.consts = nk;
        }
        p.op[idx] = op;
        p.comps[idx] = n.getType().getComponents();
        p.args[idx] = a;
        p.aux[idx] = aux;
        p.consts[idx] = k;
        p.count++;
        done.put(n, Integer.valueOf(idx));
        return idx;
    }

    private void run(Program p) {
        float[] r = p.regs;
        for (int i = 0; i < p.count; i++) {
            int d = i * 4;
            int n = p.comps[i];
            int[] a = p.args[i];
            switch (p.op[i]) {
                case OP_CONST: {
                    float[] k = p.consts[i];
                    for (int c = 0; c < 4; c++) {
                        r[d + c] = c < k.length ? k[c] : 0f;
                    }
                    break;
                }
                case OP_INPUT:
                    System.arraycopy(inputs, p.aux[i] * 4, r, d, 4);
                    break;
                case OP_UNIFORM:
                    System.arraycopy(material.getUniformValues(), p.aux[i] * 4, r, d, 4);
                    break;
                case OP_SAMPLE: {
                    int uv = a[0] * 4;
                    if (sampler != null) {
                        sampler.sample(p.aux[i], r[uv], r[uv + 1], texel);
                        System.arraycopy(texel, 0, r, d, 4);
                    } else {
                        r[d] = 1f;
                        r[d + 1] = 1f;
                        r[d + 2] = 1f;
                        r[d + 3] = 1f;
                    }
                    break;
                }
                case OP_SWIZZLE: {
                    int s = a[0] * 4;
                    int sw = p.aux[i];
                    // read first: the source may be this register's own input
                    float x = r[s + (sw & 3)];
                    float y = r[s + ((sw >> 2) & 3)];
                    float z = r[s + ((sw >> 4) & 3)];
                    float w = r[s + ((sw >> 6) & 3)];
                    r[d] = x;
                    r[d + 1] = n > 1 ? y : 0f;
                    r[d + 2] = n > 2 ? z : 0f;
                    r[d + 3] = n > 3 ? w : 0f;
                    break;
                }
                case OP_CONSTRUCT: {
                    int c = 0;
                    for (int src : a) {
                        int s = src * 4;
                        int m = p.comps[src];
                        for (int q = 0; q < m && c < 4; q++) {
                            r[d + c++] = r[s + q];
                        }
                    }
                    while (c < 4) {
                        r[d + c++] = 0f;
                    }
                    break;
                }
                case OP_SELECT: {
                    int s = (r[a[0] * 4] > 0.5f ? a[1] : a[2]) * 4;
                    System.arraycopy(r, s, r, d, 4);
                    break;
                }
                case OP_DOT:
                    r[d] = dot(r, a[0] * 4, a[1] * 4, p.comps[a[0]]);
                    zeroFrom(r, d, 1);
                    break;
                case OP_LENGTH: {
                    int s = a[0] * 4;
                    r[d] = (float) Math.sqrt(dot(r, s, s, p.comps[a[0]]));
                    zeroFrom(r, d, 1);
                    break;
                }
                case OP_DISTANCE: {
                    int s0 = a[0] * 4;
                    int s1 = a[1] * 4;
                    float sum = 0f;
                    for (int c = 0; c < p.comps[a[0]]; c++) {
                        float t = r[s0 + c] - r[s1 + c];
                        sum += t * t;
                    }
                    r[d] = (float) Math.sqrt(sum);
                    zeroFrom(r, d, 1);
                    break;
                }
                case OP_NORMALIZE: {
                    int s = a[0] * 4;
                    float len = (float) Math.sqrt(dot(r, s, s, n));
                    float inv = len > 0f ? 1f / len : 0f;
                    for (int c = 0; c < 4; c++) {
                        r[d + c] = c < n ? r[s + c] * inv : 0f;
                    }
                    break;
                }
                case OP_CROSS: {
                    int s0 = a[0] * 4;
                    int s1 = a[1] * 4;
                    float x = r[s0 + 1] * r[s1 + 2] - r[s0 + 2] * r[s1 + 1];
                    float y = r[s0 + 2] * r[s1] - r[s0] * r[s1 + 2];
                    float z = r[s0] * r[s1 + 1] - r[s0 + 1] * r[s1];
                    r[d] = x;
                    r[d + 1] = y;
                    r[d + 2] = z;
                    r[d + 3] = 0f;
                    break;
                }
                case OP_REFLECT: {
                    // i - 2 * dot(n, i) * n
                    int si = a[0] * 4;
                    int sn = a[1] * 4;
                    float k2 = 2f * dot(r, si, sn, n);
                    for (int c = 0; c < 4; c++) {
                        r[d + c] = c < n ? r[si + c] - k2 * r[sn + c] : 0f;
                    }
                    break;
                }
                default:
                    componentwise(p, i, r, d, n, a);
                    break;
            }
        }
    }

    // a per-component operation; a FLOAT operand of a BINARY op applies to
    // every component, as in the shading languages
    private static void componentwise(Program p, int i, float[] r, int d, int n, int[] a) {
        int op = p.op[i];
        for (int c = 0; c < 4; c++) {
            if (c >= n) {
                r[d + c] = 0f;
                continue;
            }
            float x = arg(p, r, a, 0, c);
            float v;
            switch (op) {
                case OP_ADD:
                    v = x + arg(p, r, a, 1, c);
                    break;
                case OP_SUB:
                    v = x - arg(p, r, a, 1, c);
                    break;
                case OP_MUL:
                    v = x * arg(p, r, a, 1, c);
                    break;
                case OP_DIV:
                    v = x / arg(p, r, a, 1, c);
                    break;
                case OP_NEGATE:
                    v = -x;
                    break;
                case OP_SIN:
                    v = (float) Math.sin(x);
                    break;
                case OP_COS:
                    v = (float) Math.cos(x);
                    break;
                case OP_ABS:
                    v = Math.abs(x);
                    break;
                case OP_FLOOR:
                    v = (float) Math.floor(x);
                    break;
                case OP_FRACT:
                    v = x - (float) Math.floor(x);
                    break;
                case OP_SQRT:
                    v = (float) Math.sqrt(x);
                    break;
                case OP_EXP:
                    v = (float) MathUtil.exp(x);
                    break;
                case OP_MIN:
                    v = Math.min(x, arg(p, r, a, 1, c));
                    break;
                case OP_MAX:
                    v = Math.max(x, arg(p, r, a, 1, c));
                    break;
                case OP_POW:
                    v = (float) MathUtil.pow(x, arg(p, r, a, 1, c));
                    break;
                case OP_ATAN2:
                    v = (float) MathUtil.atan2(x, arg(p, r, a, 1, c));
                    break;
                case OP_CLAMP:
                    v = Math.min(Math.max(x, arg(p, r, a, 1, c)), arg(p, r, a, 2, c));
                    break;
                case OP_MIX: {
                    float t = arg(p, r, a, 2, c);
                    v = x + (arg(p, r, a, 1, c) - x) * t;
                    break;
                }
                case OP_SMOOTHSTEP: {
                    float e1 = arg(p, r, a, 1, c);
                    float t = (arg(p, r, a, 2, c) - x) / (e1 - x);
                    t = t < 0f ? 0f : t > 1f ? 1f : t;
                    v = t * t * (3f - 2f * t);
                    break;
                }
                case OP_STEP:
                    v = arg(p, r, a, 1, c) < x ? 0f : 1f;
                    break;
                default:
                    throw new IllegalStateException("Unknown op " + op);
            }
            r[d + c] = v;
        }
    }

    private static float arg(Program p, float[] r, int[] a, int j, int c) {
        int s = a[j];
        return r[s * 4 + (p.comps[s] == 1 ? 0 : c)];
    }

    private static float dot(float[] r, int s0, int s1, int n) {
        float sum = 0f;
        for (int c = 0; c < n; c++) {
            sum += r[s0 + c] * r[s1 + c];
        }
        return sum;
    }

    private static void zeroFrom(float[] r, int d, int from) {
        for (int c = from; c < 4; c++) {
            r[d + c] = 0f;
        }
    }
}
