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

/// One value in a portable shader: a constant, a uniform, a built-in input
/// such as the world position, a texture sample, or an operation on other
/// nodes. Nodes are immutable and can be shared; a node used twice is
/// computed once in the generated program.
///
/// A graph is plain Java, so it is checked by the compiler, needs no parser
/// on the device and prints as GLSL, Metal or HLSL depending on the backend.
/// The types follow the shading languages' own rules: two operands of an
/// arithmetic operation have the same type, or one of them is a `FLOAT` that
/// is applied to every component of the other.
///
/// ```java
/// ShaderNode uv = ShaderNode.uv().mul(4f).add(ShaderNode.time().mul(0.05f));
/// ShaderNode wave = ShaderNode.sin(uv.x().mul(6.2831f)).mul(0.5f).add(0.5f);
/// ShaderNode color = ShaderNode.mix(ShaderNode.vec3(0.1f, 0.3f, 0.5f),
///         ShaderNode.vec3(0.6f, 0.8f, 0.9f), wave);
/// ```
///
/// The built-in inputs that a stage can read are listed on each factory.
/// Reading a fragment input from the `VERTEX_OFFSET` hook, or the reverse,
/// fails when the material is compiled.
public final class ShaderNode {
    /// What a node is. Package visible for the emitters.
    static final int CONST = 0;
    static final int UNIFORM = 1;
    static final int BUILTIN = 2;
    static final int SAMPLE = 3;
    static final int BINARY = 4;
    static final int CALL = 5;
    static final int SWIZZLE = 6;
    static final int CONSTRUCT = 7;
    static final int NEGATE = 8;
    static final int SELECT = 9;

    private final int kind;
    private final ShaderType type;
    private final String name;
    private final float[] values;
    private final ShaderNode[] args;

    private ShaderNode(int kind, ShaderType type, String name, float[] values, ShaderNode[] args) {
        this.kind = kind;
        this.type = type;
        this.name = name;
        this.values = values;
        this.args = args;
    }

    // ---------------------------------------------------------------- inputs

    /// A float constant.
    ///
    /// #### Parameters
    ///
    /// - `v`: the value
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public static ShaderNode f(float v) {
        return new ShaderNode(CONST, ShaderType.FLOAT, null, new float[]{v}, null);
    }

    /// A two component constant.
    ///
    /// #### Parameters
    ///
    /// - `x`: the first component
    ///
    /// - `y`: the second component
    ///
    /// #### Returns
    ///
    /// a `VEC2` node
    public static ShaderNode vec2(float x, float y) {
        return new ShaderNode(CONST, ShaderType.VEC2, null, new float[]{x, y}, null);
    }

    /// A three component constant.
    ///
    /// #### Parameters
    ///
    /// - `x`: the first component
    ///
    /// - `y`: the second component
    ///
    /// - `z`: the third component
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode vec3(float x, float y, float z) {
        return new ShaderNode(CONST, ShaderType.VEC3, null, new float[]{x, y, z}, null);
    }

    /// A four component constant.
    ///
    /// #### Parameters
    ///
    /// - `x`: the first component
    ///
    /// - `y`: the second component
    ///
    /// - `z`: the third component
    ///
    /// - `w`: the fourth component
    ///
    /// #### Returns
    ///
    /// a `VEC4` node
    public static ShaderNode vec4(float x, float y, float z, float w) {
        return new ShaderNode(CONST, ShaderType.VEC4, null, new float[]{x, y, z, w}, null);
    }

    /// An RGB colour constant from a packed `0xRRGGBB` (the alpha byte is
    /// ignored), in the 0 to 1 range.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the colour
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode color(int rgb) {
        return vec3(((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f);
    }

    /// Builds a vector from nodes whose components add up to 2, 3 or 4, as
    /// `vec3(v.xy, 1.0)` does in GLSL.
    ///
    /// #### Parameters
    ///
    /// - `parts`: the nodes, in order
    ///
    /// #### Returns
    ///
    /// a vector node
    public static ShaderNode vec(ShaderNode... parts) {
        int n = 0;
        for (ShaderNode part : parts) {
            n += part.type.getComponents();
        }
        if (n < 2 || n > 4) {
            throw new IllegalArgumentException("vec() needs 2 to 4 components, got " + n);
        }
        ShaderNode[] copy = new ShaderNode[parts.length];
        System.arraycopy(parts, 0, copy, 0, parts.length);
        return new ShaderNode(CONSTRUCT, ShaderType.ofComponents(n), null, null, copy);
    }

    /// A value the application sets per draw with
    /// `ShaderMaterial.setUniform`. It must be declared on the material with
    /// `ShaderMaterial.declareUniform` (the library shaders do that for
    /// you).
    ///
    /// #### Parameters
    ///
    /// - `name`: a name made of letters, digits and underscores
    ///
    /// - `type`: its type
    ///
    /// #### Returns
    ///
    /// a node of that type
    public static ShaderNode uniform(String name, ShaderType type) {
        checkName(name);
        return new ShaderNode(UNIFORM, type, name, null, null);
    }

    /// Samples a texture declared on the material with
    /// `ShaderMaterial.declareTexture`. Works in the fragment stage only.
    ///
    /// #### Parameters
    ///
    /// - `textureName`: the declared name
    ///
    /// - `uv`: the `VEC2` coordinate
    ///
    /// #### Returns
    ///
    /// a `VEC4` texel
    public static ShaderNode sample(String textureName, ShaderNode uv) {
        checkName(textureName);
        requireType(uv, ShaderType.VEC2, "sample uv");
        return new ShaderNode(SAMPLE, ShaderType.VEC4, textureName, null, new ShaderNode[]{uv});
    }

    /// Reads the frame in a custom full screen pass
    /// (`com.codename1.gpu.PostProcess.addPass`): the scene, or the output
    /// of the pass before. Means nothing on an ordinary material.
    ///
    /// #### Parameters
    ///
    /// - `uv`: the `VEC2` place on screen, 0,0 top left; usually
    ///   `screenUv()` or an offset from it
    ///
    /// #### Returns
    ///
    /// a `VEC4` colour
    public static ShaderNode sceneColor(ShaderNode uv) {
        return sample(com.codename1.gpu.PostProcess.SCENE_COLOR, uv);
    }

    /// The place of the pixel on screen in a custom full screen pass, from
    /// 0,0 top left to 1,1 bottom right. In a pass it is the same as `uv()`.
    ///
    /// #### Returns
    ///
    /// a `VEC2` node
    public static ShaderNode screenUv() {
        return uv();
    }

    private static ShaderNode builtin(String name, ShaderType type) {
        return new ShaderNode(BUILTIN, type, name, null, null);
    }

    /// The vertex position in model space. Vertex stage.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode position() {
        return builtin("position", ShaderType.VEC3);
    }

    /// The vertex normal in model space, or up when the mesh has none.
    /// Vertex stage.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode normal() {
        return builtin("normal", ShaderType.VEC3);
    }

    /// The first texture coordinate, or zero when the mesh has none. Both
    /// stages.
    ///
    /// #### Returns
    ///
    /// a `VEC2` node
    public static ShaderNode uv() {
        return builtin("uv", ShaderType.VEC2);
    }

    /// The position in world space: in the vertex stage before the
    /// `VERTEX_OFFSET` hook moves it, in the fragment stage after. Both
    /// stages.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode worldPos() {
        return builtin("worldPos", ShaderType.VEC3);
    }

    /// The interpolated surface normal in world space, normalized, before the
    /// `NORMAL` hook replaces it. Fragment stage.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode worldNormal() {
        return builtin("worldNormal", ShaderType.VEC3);
    }

    /// The unit direction from the surface toward the camera. Fragment stage.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode viewDir() {
        return builtin("viewDir", ShaderType.VEC3);
    }

    /// The camera position in world space. Both stages.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode cameraPos() {
        return builtin("cameraPos", ShaderType.VEC3);
    }

    /// The unit direction toward the light (the sun). Both stages.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode sunDir() {
        return builtin("sunDir", ShaderType.VEC3);
    }

    /// The light colour. Both stages.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode sunColor() {
        return builtin("sunColor", ShaderType.VEC3);
    }

    /// The index of the copy `GraphicsDevice.drawInstanced` is drawing, 0
    /// for an ordinary draw. Both stages. Reading it (or `instanceRandom`,
    /// `instanceColor`) declares the uniform it needs on the material.
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public static ShaderNode instanceId() {
        return uniform(ShaderMaterial.INSTANCE, ShaderType.VEC4).x();
    }

    /// A random number in 0 to 1 that stays the same for each copy drawn by
    /// `GraphicsDevice.drawInstanced` (and 0 otherwise): vary the size, tint
    /// or sway phase of a field of grass or a crowd with it. Both stages.
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public static ShaderNode instanceRandom() {
        return uniform(ShaderMaterial.INSTANCE, ShaderType.VEC4).y();
    }

    /// The colour `GraphicsDevice.drawInstanced` gives the copy being drawn,
    /// white when it gives none. Both stages.
    ///
    /// #### Returns
    ///
    /// a `VEC4` node
    public static ShaderNode instanceColor() {
        return uniform(ShaderMaterial.INSTANCE_COLOR, ShaderType.VEC4);
    }

    /// The clock in seconds that `GraphicsDevice.setTime` sets; the scene
    /// renderer advances it every frame. Both stages.
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public static ShaderNode time() {
        return builtin("time", ShaderType.FLOAT);
    }

    /// The material colour times its texture, before the `BASE_COLOR` hook
    /// replaces it. Fragment stage.
    ///
    /// #### Returns
    ///
    /// a `VEC4` node
    public static ShaderNode baseColor() {
        return builtin("baseColor", ShaderType.VEC4);
    }

    // ------------------------------------------------------------ arithmetic

    private static ShaderType arithmetic(ShaderNode a, ShaderNode b, String op) {
        if (a.type == b.type) {
            return a.type;
        }
        if (a.type == ShaderType.FLOAT) {
            return b.type;
        }
        if (b.type == ShaderType.FLOAT) {
            return a.type;
        }
        throw new IllegalArgumentException("Cannot " + op + " " + a.type + " and " + b.type);
    }

    private static ShaderNode binary(String op, ShaderNode a, ShaderNode b) {
        return new ShaderNode(BINARY, arithmetic(a, b, op), op, null, new ShaderNode[]{a, b});
    }

    /// Returns this plus `o`.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other operand
    ///
    /// #### Returns
    ///
    /// the sum
    public ShaderNode add(ShaderNode o) {
        return binary("+", this, o);
    }

    /// Returns this plus a constant.
    ///
    /// #### Parameters
    ///
    /// - `o`: the constant
    ///
    /// #### Returns
    ///
    /// the sum
    public ShaderNode add(float o) {
        return binary("+", this, f(o));
    }

    /// Returns this minus `o`.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other operand
    ///
    /// #### Returns
    ///
    /// the difference
    public ShaderNode sub(ShaderNode o) {
        return binary("-", this, o);
    }

    /// Returns this minus a constant.
    ///
    /// #### Parameters
    ///
    /// - `o`: the constant
    ///
    /// #### Returns
    ///
    /// the difference
    public ShaderNode sub(float o) {
        return binary("-", this, f(o));
    }

    /// Returns this times `o`, component by component.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other operand
    ///
    /// #### Returns
    ///
    /// the product
    public ShaderNode mul(ShaderNode o) {
        return binary("*", this, o);
    }

    /// Returns this times a constant.
    ///
    /// #### Parameters
    ///
    /// - `o`: the constant
    ///
    /// #### Returns
    ///
    /// the product
    public ShaderNode mul(float o) {
        return binary("*", this, f(o));
    }

    /// Returns this divided by `o`, component by component.
    ///
    /// #### Parameters
    ///
    /// - `o`: the divisor
    ///
    /// #### Returns
    ///
    /// the quotient
    public ShaderNode div(ShaderNode o) {
        return binary("/", this, o);
    }

    /// Returns this divided by a constant.
    ///
    /// #### Parameters
    ///
    /// - `o`: the divisor
    ///
    /// #### Returns
    ///
    /// the quotient
    public ShaderNode div(float o) {
        return binary("/", this, f(o));
    }

    /// Returns minus this.
    ///
    /// #### Returns
    ///
    /// the negation
    public ShaderNode negate() {
        return new ShaderNode(NEGATE, type, null, null, new ShaderNode[]{this});
    }

    /// Returns `1 - this`, the usual way to invert a mask.
    ///
    /// #### Returns
    ///
    /// the complement
    public ShaderNode oneMinus() {
        return binary("-", f(1f), this);
    }

    // -------------------------------------------------------------- swizzles

    /// Picks components by name, as GLSL's `v.xz` does: one to four of
    /// `x`, `y`, `z`, `w` (or `r`, `g`, `b`, `a`).
    ///
    /// #### Parameters
    ///
    /// - `components`: the component names
    ///
    /// #### Returns
    ///
    /// a node with as many components as named
    public ShaderNode swizzle(String components) {
        int n = components.length();
        if (n < 1 || n > 4) {
            throw new IllegalArgumentException("A swizzle names 1 to 4 components: " + components);
        }
        StringBuilder canon = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int idx = componentIndex(components.charAt(i));
            if (idx >= type.getComponents()) {
                throw new IllegalArgumentException("Component " + components.charAt(i) + " is not in a " + type);
            }
            canon.append("xyzw".charAt(idx));
        }
        return new ShaderNode(SWIZZLE, ShaderType.ofComponents(n), canon.toString(), null, new ShaderNode[]{this});
    }

    private static int componentIndex(char c) {
        switch (c) {
            case 'x':
            case 'r':
                return 0;
            case 'y':
            case 'g':
                return 1;
            case 'z':
            case 'b':
                return 2;
            case 'w':
            case 'a':
                return 3;
            default:
                throw new IllegalArgumentException("Not a component name: " + c);
        }
    }

    /// Returns the first component.
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public ShaderNode x() {
        return swizzle("x");
    }

    /// Returns the second component.
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public ShaderNode y() {
        return swizzle("y");
    }

    /// Returns the third component.
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public ShaderNode z() {
        return swizzle("z");
    }

    /// Returns the fourth component.
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public ShaderNode w() {
        return swizzle("w");
    }

    /// Returns the first three components, the colour of a texel.
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public ShaderNode rgb() {
        return swizzle("xyz");
    }

    /// Returns the first two components.
    ///
    /// #### Returns
    ///
    /// a `VEC2` node
    public ShaderNode xy() {
        return swizzle("xy");
    }

    /// Returns the first and third components, the ground plane of a world
    /// position.
    ///
    /// #### Returns
    ///
    /// a `VEC2` node
    public ShaderNode xz() {
        return swizzle("xz");
    }

    // ------------------------------------------------------------- functions

    private static ShaderNode call(String fn, ShaderType type, ShaderNode... a) {
        return new ShaderNode(CALL, type, fn, null, a);
    }

    private static ShaderNode same(String fn, ShaderNode a) {
        return call(fn, a.type, a);
    }

    /// Returns `sin(a)`, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: radians
    ///
    /// #### Returns
    ///
    /// the sine
    public static ShaderNode sin(ShaderNode a) {
        return same("sin", a);
    }

    /// Returns `cos(a)`, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: radians
    ///
    /// #### Returns
    ///
    /// the cosine
    public static ShaderNode cos(ShaderNode a) {
        return same("cos", a);
    }

    /// Returns the absolute value, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: the value
    ///
    /// #### Returns
    ///
    /// the absolute value
    public static ShaderNode abs(ShaderNode a) {
        return same("abs", a);
    }

    /// Returns the largest integer not greater than `a`, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: the value
    ///
    /// #### Returns
    ///
    /// the floor
    public static ShaderNode floor(ShaderNode a) {
        return same("floor", a);
    }

    /// Returns `a - floor(a)`, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: the value
    ///
    /// #### Returns
    ///
    /// the fractional part
    public static ShaderNode fract(ShaderNode a) {
        return same("fract", a);
    }

    /// Returns the square root, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: a non negative value
    ///
    /// #### Returns
    ///
    /// the square root
    public static ShaderNode sqrt(ShaderNode a) {
        return same("sqrt", a);
    }

    /// Returns `e` raised to `a`, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: the exponent
    ///
    /// #### Returns
    ///
    /// the exponential
    public static ShaderNode exp(ShaderNode a) {
        return same("exp", a);
    }

    /// Returns the unit vector in the direction of `a`.
    ///
    /// #### Parameters
    ///
    /// - `a`: a non zero vector
    ///
    /// #### Returns
    ///
    /// the normalized vector
    public static ShaderNode normalize(ShaderNode a) {
        return same("normalize", a);
    }

    /// Returns `a` clamped to 0 to 1, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: the value
    ///
    /// #### Returns
    ///
    /// the saturated value
    public static ShaderNode saturate(ShaderNode a) {
        return clamp(a, 0f, 1f);
    }

    /// Returns the length of a vector.
    ///
    /// #### Parameters
    ///
    /// - `a`: the vector
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public static ShaderNode length(ShaderNode a) {
        return call("length", ShaderType.FLOAT, a);
    }

    /// Returns the distance between two points.
    ///
    /// #### Parameters
    ///
    /// - `a`: a point
    ///
    /// - `b`: another point of the same type
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public static ShaderNode distance(ShaderNode a, ShaderNode b) {
        requireSame(a, b, "distance");
        return call("distance", ShaderType.FLOAT, a, b);
    }

    /// Returns the dot product.
    ///
    /// #### Parameters
    ///
    /// - `a`: a vector
    ///
    /// - `b`: another vector of the same type
    ///
    /// #### Returns
    ///
    /// a `FLOAT` node
    public static ShaderNode dot(ShaderNode a, ShaderNode b) {
        requireSame(a, b, "dot");
        return call("dot", ShaderType.FLOAT, a, b);
    }

    /// Returns the cross product of two `VEC3` nodes.
    ///
    /// #### Parameters
    ///
    /// - `a`: a vector
    ///
    /// - `b`: another vector
    ///
    /// #### Returns
    ///
    /// a `VEC3` node
    public static ShaderNode cross(ShaderNode a, ShaderNode b) {
        requireType(a, ShaderType.VEC3, "cross");
        requireType(b, ShaderType.VEC3, "cross");
        return call("cross", ShaderType.VEC3, a, b);
    }

    /// Returns the reflection of incident direction `i` about normal `n`.
    ///
    /// #### Parameters
    ///
    /// - `i`: the incident direction
    ///
    /// - `n`: the unit normal
    ///
    /// #### Returns
    ///
    /// a node of the type of `i`
    public static ShaderNode reflect(ShaderNode i, ShaderNode n) {
        requireSame(i, n, "reflect");
        return call("reflect", i.type, i, n);
    }

    /// Returns the smaller of two values, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: a value
    ///
    /// - `b`: another value
    ///
    /// #### Returns
    ///
    /// the minimum
    public static ShaderNode min(ShaderNode a, ShaderNode b) {
        // Metal has no mixed vector/scalar overloads, so the scalar is widened
        ShaderType t = arithmetic(a, b, "min");
        return call("min", t, widen(a, t), widen(b, t));
    }

    /// Returns the larger of two values, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: a value
    ///
    /// - `b`: another value
    ///
    /// #### Returns
    ///
    /// the maximum
    public static ShaderNode max(ShaderNode a, ShaderNode b) {
        // Metal has no mixed vector/scalar overloads, so the scalar is widened
        ShaderType t = arithmetic(a, b, "max");
        return call("max", t, widen(a, t), widen(b, t));
    }

    /// Returns `a` raised to `b`, per component. `a` must not be negative.
    ///
    /// #### Parameters
    ///
    /// - `a`: the base
    ///
    /// - `b`: the exponent, of the type of `a` or a `FLOAT`
    ///
    /// #### Returns
    ///
    /// the power
    public static ShaderNode pow(ShaderNode a, ShaderNode b) {
        return call("pow", a.type, a, widen(b, a.type));
    }

    /// Returns `a` raised to a constant.
    ///
    /// #### Parameters
    ///
    /// - `a`: the base, not negative
    ///
    /// - `b`: the exponent
    ///
    /// #### Returns
    ///
    /// the power
    public static ShaderNode pow(ShaderNode a, float b) {
        return pow(a, f(b));
    }

    /// Returns `a` modulo `b` with the sign of `b`, as GLSL's `mod` does
    /// (HLSL's `fmod` differs for negative values, so this is printed as
    /// `a - b * floor(a / b)` everywhere).
    ///
    /// #### Parameters
    ///
    /// - `a`: the dividend
    ///
    /// - `b`: the divisor
    ///
    /// #### Returns
    ///
    /// the remainder
    public static ShaderNode mod(ShaderNode a, ShaderNode b) {
        return a.sub(b.mul(floor(a.div(b))));
    }

    /// Returns the angle of the vector (x, y) in radians, -pi to pi.
    ///
    /// #### Parameters
    ///
    /// - `y`: the second coordinate
    ///
    /// - `x`: the first coordinate
    ///
    /// #### Returns
    ///
    /// the angle
    public static ShaderNode atan2(ShaderNode y, ShaderNode x) {
        ShaderType t = arithmetic(y, x, "atan2");
        return call("atan2", t, widen(y, t), widen(x, t));
    }

    /// Returns `a` clamped between `lo` and `hi`, per component.
    ///
    /// #### Parameters
    ///
    /// - `a`: the value
    ///
    /// - `lo`: the lower bound
    ///
    /// - `hi`: the upper bound
    ///
    /// #### Returns
    ///
    /// the clamped value
    public static ShaderNode clamp(ShaderNode a, float lo, float hi) {
        return call("clamp", a.type, a, widen(f(lo), a.type), widen(f(hi), a.type));
    }

    /// Linear interpolation from `a` to `b` by `t`.
    ///
    /// #### Parameters
    ///
    /// - `a`: the value at 0
    ///
    /// - `b`: the value at 1, of the type of `a`
    ///
    /// - `t`: the weight, of the type of `a` or a `FLOAT`
    ///
    /// #### Returns
    ///
    /// the blend
    public static ShaderNode mix(ShaderNode a, ShaderNode b, ShaderNode t) {
        requireSame(a, b, "mix");
        return call("mix", a.type, a, b, widen(t, a.type));
    }

    /// Hermite step from 0 at `e0` to 1 at `e1`.
    ///
    /// #### Parameters
    ///
    /// - `e0`: the lower edge
    ///
    /// - `e1`: the upper edge
    ///
    /// - `x`: the value
    ///
    /// #### Returns
    ///
    /// a node of the type of `x`
    public static ShaderNode smoothstep(float e0, float e1, ShaderNode x) {
        return smoothstep(f(e0), f(e1), x);
    }

    /// Hermite step from 0 at `e0` to 1 at `e1`, with node edges.
    ///
    /// #### Parameters
    ///
    /// - `e0`: the lower edge
    ///
    /// - `e1`: the upper edge
    ///
    /// - `x`: the value
    ///
    /// #### Returns
    ///
    /// a node of the type of `x`
    public static ShaderNode smoothstep(ShaderNode e0, ShaderNode e1, ShaderNode x) {
        return call("smoothstep", x.type, widen(e0, x.type), widen(e1, x.type), x);
    }

    /// Returns 0 where `x` is below `edge` and 1 elsewhere.
    ///
    /// #### Parameters
    ///
    /// - `edge`: the edge
    ///
    /// - `x`: the value
    ///
    /// #### Returns
    ///
    /// a node of the type of `x`
    public static ShaderNode step(ShaderNode edge, ShaderNode x) {
        return call("step", x.type, widen(edge, x.type), x);
    }

    /// Returns `a` where `condition` is above 0.5, otherwise `b`. Both
    /// branches are computed; this is a select, not a branch.
    ///
    /// #### Parameters
    ///
    /// - `condition`: a `FLOAT`, usually a `step`
    ///
    /// - `a`: the value when true
    ///
    /// - `b`: the value when false, of the type of `a`
    ///
    /// #### Returns
    ///
    /// the selected value
    public static ShaderNode select(ShaderNode condition, ShaderNode a, ShaderNode b) {
        requireType(condition, ShaderType.FLOAT, "select condition");
        requireSame(a, b, "select");
        return new ShaderNode(SELECT, a.type, null, null, new ShaderNode[]{condition, a, b});
    }

    // ---------------------------------------------------------- normal maps

    /// Decodes a texel of a tangent space normal map (the usual blue-ish
    /// image) into a direction, scaling its slope by `strength`.
    ///
    /// #### Parameters
    ///
    /// - `texel`: a `VEC4` read with `sample`
    ///
    /// - `strength`: 1 for the map as authored, 0 for flat
    ///
    /// #### Returns
    ///
    /// a `VEC3` tangent space normal, not normalized
    public static ShaderNode unpackNormal(ShaderNode texel, float strength) {
        requireType(texel, ShaderType.VEC4, "unpackNormal");
        ShaderNode xy = texel.xy().mul(2f).sub(1f).mul(strength);
        return vec(xy, texel.z().mul(2f).sub(1f));
    }

    /// Bends the world space surface normal by a tangent space normal, for
    /// the `NORMAL` hook. The tangent frame is derived from the surface
    /// normal and the world axes, so it needs no tangents in the mesh and is
    /// the same on every backend: on ground and water, tangent `x` runs
    /// along world `x` and `y` along world `z`; on walls it runs around the
    /// vertical. That suits tiling detail, noise and terrain maps. A map
    /// painted for one model's UV layout reads correctly where that layout
    /// follows the world axes.
    ///
    /// #### Parameters
    ///
    /// - `tangentNormal`: a `VEC3`, such as `unpackNormal`'s
    ///
    /// #### Returns
    ///
    /// the unit world space normal
    public static ShaderNode perturbNormal(ShaderNode tangentNormal) {
        requireType(tangentNormal, ShaderType.VEC3, "perturbNormal");
        ShaderNode n = normalize(worldNormal());
        // Up is a poor reference on a level face; fall back to +z there
        ShaderNode flat = step(f(0.999f), abs(n.y()));
        ShaderNode ref = select(flat, vec3(0f, 0f, 1f), vec3(0f, 1f, 0f));
        ShaderNode t = normalize(cross(ref, n));
        ShaderNode b = cross(n, t);
        return normalize(t.mul(tangentNormal.x()).add(b.mul(tangentNormal.y()))
                .add(n.mul(tangentNormal.z())));
    }

    /// Blends two tangent space normals so the detail rides on the base
    /// rather than averaging it flat ("whiteout" blend).
    ///
    /// #### Parameters
    ///
    /// - `base`: a `VEC3` tangent space normal
    ///
    /// - `detail`: another
    ///
    /// #### Returns
    ///
    /// the combined `VEC3`, not normalized
    public static ShaderNode blendNormals(ShaderNode base, ShaderNode detail) {
        requireType(base, ShaderType.VEC3, "blendNormals");
        requireType(detail, ShaderType.VEC3, "blendNormals");
        return vec(base.xy().add(detail.xy()), base.z().mul(detail.z()));
    }

    // ------------------------------------------------------------- utilities

    /// Repeats a `FLOAT` into a vector of `t`, or returns the node itself
    /// when it already has that type.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    ///
    /// - `t`: the target type
    ///
    /// #### Returns
    ///
    /// a node of type `t`
    public static ShaderNode widen(ShaderNode n, ShaderType t) {
        if (n.type == t) {
            return n;
        }
        if (n.type != ShaderType.FLOAT) {
            throw new IllegalArgumentException("Cannot use a " + n.type + " as a " + t);
        }
        ShaderNode[] parts = new ShaderNode[t.getComponents()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = n;
        }
        return new ShaderNode(CONSTRUCT, t, null, null, parts);
    }

    private static void requireSame(ShaderNode a, ShaderNode b, String what) {
        if (a.type != b.type) {
            throw new IllegalArgumentException(what + " needs two " + a.type + " values, got " + b.type);
        }
    }

    static void requireType(ShaderNode a, ShaderType t, String what) {
        if (a.type != t) {
            throw new IllegalArgumentException(what + " must be a " + t + ", not a " + a.type);
        }
    }

    static void checkName(String name) {
        if (name == null || name.length() == 0 || name.length() > 32) {
            throw new IllegalArgumentException("A shader name has 1 to 32 characters");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
                    || (i > 0 && c >= '0' && c <= '9');
            if (!ok) {
                throw new IllegalArgumentException("Not a valid shader name: " + name);
            }
        }
    }

    /// Returns the type of the value.
    ///
    /// #### Returns
    ///
    /// the type
    public ShaderType getType() {
        return type;
    }

    int getKind() {
        return kind;
    }

    String getName() {
        return name;
    }

    float[] getValues() {
        return values;
    }

    int getArgCount() {
        return args == null ? 0 : args.length;
    }

    ShaderNode getArg(int i) {
        return args[i];
    }
}
