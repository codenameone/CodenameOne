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
import com.codename1.gpu.Environment;
import com.codename1.gpu.Texture;
import com.codename1.gpu.Material;
import com.codename1.gpu.VertexFormat;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shaders.*;
import com.codename1.impl.gpu.GlslShaderGenerator;
import com.codename1.impl.ios.IOSMetalShaderGenerator;
import com.codename1.impl.windows.HlslShaderGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// Writes every library shader, and the post composite, in all three
/// shading languages into the directory given, for check-gpu-shaders.sh to
/// hand to the real compilers.
public class ShaderDump {
    public static void main(String[] args) throws IOException {
        String dir = args[0];
        Map<String, ShaderMaterial> m = new LinkedHashMap<String, ShaderMaterial>();
        m.put("Water.ocean", Water.ocean());
        m.put("Water.lake", Water.lake());
        m.put("Water.river", Water.river());
        m.put("Terrain.layered", Terrain.layered());
        m.put("Terrain.rock", Terrain.rock());
        m.put("Foliage.wind", Foliage.wind());
        m.put("Foliage.leaves", Foliage.leaves());
        m.put("Foliage.grass", Foliage.grass());
        m.put("Surface.paintedMetal", Surface.paintedMetal());
        m.put("Surface.rust", Surface.rust());
        m.put("Surface.wood", Surface.wood());
        m.put("Surface.concrete", Surface.concrete());
        m.put("Surface.glass", Surface.glass());
        m.put("Surface.ice", Surface.ice());
        m.put("Surface.fabric", Surface.fabric());
        m.put("Surface.carPaint", Surface.carPaint());
        m.put("Surface.paintSplats", Surface.paintSplats());
        m.put("Toon.cel", Toon.cel());
        m.put("Toon.hatching", Toon.hatching());
        m.put("Toon.lowPoly", Toon.lowPoly());
        m.put("Fx.dissolve", Fx.dissolve());
        m.put("Fx.hologram", Fx.hologram());
        m.put("Fx.forceField", Fx.forceField());
        m.put("Fx.beam", Fx.beam());
        m.put("Fx.fire", Fx.fire());
        m.put("Fx.smoke", Fx.smoke());
        m.put("Fx.portal", Fx.portal());
        m.put("Fx.xray", Fx.xray());
        m.put("Fx.pulse", Fx.pulse());
        m.put("Liquid.lava", Liquid.lava());
        m.put("Liquid.toxic", Liquid.toxic());
        m.put("Liquid.caustics", Liquid.caustics());
        m.put("Sky.gradient", Sky.gradient());
        Environment env = ScenePresets.harbourDusk().getEnvironment();
        for (Map.Entry<String, ShaderMaterial> e : m.entrySet()) {
            Material mat = new Material(Material.Type.PBR).setShader(e.getValue());
            GlslShaderGenerator g = new GlslShaderGenerator(mat, VertexFormat.POSITION_NORMAL_TEXCOORD, env, true);
            write(dir + "/" + e.getKey() + ".vert", "#version 100\n" + g.getVertexSource());
            write(dir + "/" + e.getKey() + ".frag", "#version 100\n" + g.getFragmentSource());
            write(dir + "/" + e.getKey() + ".metal",
                    new IOSMetalShaderGenerator(mat, VertexFormat.POSITION_NORMAL_TEXCOORD, env, true).getSource());
            write(dir + "/" + e.getKey() + ".hlsl",
                    new HlslShaderGenerator(mat, VertexFormat.POSITION_NORMAL_TEXCOORD, env, true).getSource());
            if (GlslShaderGenerator.needsOwnDepthProgram(mat)) {
                // the depth-only caster program of a material that moves or cuts its surface
                String d = dir + "/" + e.getKey() + ".depth";
                GlslShaderGenerator dg = GlslShaderGenerator.depthOnly(mat, VertexFormat.POSITION_NORMAL_TEXCOORD);
                write(d + ".vert", "#version 100\n" + dg.getVertexSource());
                write(d + ".frag", "#version 100\n" + dg.getFragmentSource());
                write(d + ".metal",
                        IOSMetalShaderGenerator.depthOnly(mat, VertexFormat.POSITION_NORMAL_TEXCOORD).getSource());
                write(d + ".hlsl",
                        HlslShaderGenerator.depthOnly(mat, VertexFormat.POSITION_NORMAL_TEXCOORD).getSource());
            }
        }
        // image based lighting: the built-in lit types and one library shader
        Environment ibl = ScenePresets.harbourDusk().getEnvironment().setEnvironmentMap(new Texture(128, 384));
        Material[] lit = {new Material(Material.Type.PBR), new Material(Material.Type.PHONG),
                new Material(Material.Type.LAMBERT), new Material(Material.Type.PBR).setShader(Water.ocean())};
        for (int i = 0; i < lit.length; i++) {
            String b = dir + "/ibl" + i;
            GlslShaderGenerator g = new GlslShaderGenerator(lit[i], VertexFormat.POSITION_NORMAL_TEXCOORD, ibl, true);
            write(b + ".vert", "#version 100\n" + g.getVertexSource());
            write(b + ".frag", "#version 100\n" + g.getFragmentSource());
            write(b + ".metal", new IOSMetalShaderGenerator(lit[i], VertexFormat.POSITION_NORMAL_TEXCOORD, ibl, true).getSource());
            write(b + ".hlsl", new HlslShaderGenerator(lit[i], VertexFormat.POSITION_NORMAL_TEXCOORD, ibl, true).getSource());
        }
        write(dir + "/post.frag", "#version 100\n" + GlslShaderGenerator.compositeFragment());
        write(dir + "/post.metal", IOSMetalShaderGenerator.postSource());
        write(dir + "/post.hlsl", HlslShaderGenerator.postCompositeSource());
    }

    private static void write(String path, String text) throws IOException {
        FileWriter w = new FileWriter(path);
        try {
            w.write(text);
        } finally {
            w.close();
        }
    }
}
