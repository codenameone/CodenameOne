/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.splat.play;

import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.Primitives;

/// An operator standing in a pool of light in a dark storage yard, marker in
/// hand, shifting their weight: the backdrop of the main menu and the loadout.
/// The menu is laid over the left of it, so the operator stands right of
/// centre.
// tag::splat-operator-stage[]
public class OperatorStage extends GameView {
    private final String operator;
    private String marker;
    private AnimatedModel op;
    private AnimatedModel gun;
    private float scale;
    private final Grip grip = new Grip();
    private final float[] turn = new float[16];
    private GraphicsDevice device;
    private float time;
    private final float offset;

    /// `operator` is a model name from [Models]; `marker` a marker id, or null
    /// for empty hands.
    public OperatorStage(String operator, String marker, float offset) {
        this.operator = operator;
        this.marker = marker;
        this.offset = offset;
        setName("operatorStage");
    }

    /// The operator the menu shows for a player's chosen look: the same
    /// player always gets the same one.
    public static String operatorFor(String look) {
        int h = look == null ? 0 : look.hashCode();
        return Models.operator(0, (h & 0x7fffffff) % Models.OPERATORS[0].length);
    }

    /// Swaps the marker in the operator's hand.
    public void setMarker(String id) {
        marker = id;
        if (device != null && gun != null) {
            gun.removeFrom(this);
            gun = null;
        }
        if (device != null && id != null) {
            gun = Models.create(device, Models.marker(id));
            float[] b = Models.bounds(gun);
            gun.setScale(0.34f / Math.max(0.01f, Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2]))));
            gun.addTo(this);
            if (op != null) {
                holdMarker();
            }
        }
    }

    private static final int DUSK = 0xff3a3f4a;

    @Override
    protected void onSetup(GraphicsDevice d) {
        device = d;
        setClearColor(0xff0d1014);
        getLight().setDirection(0.55f, -0.8f, -0.45f).setColor(0xffffd2a0).setAmbientColor(0xff4a525c)
                .setShadows(true).setShadowMap(1024, 6f).setShadowCenter(0f, 0f, -1f);
        // the arena's atmosphere at dusk: the same engine look as a match,
        // kept dark enough that the menu over it always reads
        getEnvironment().setFog(DUSK, 0.03f, 0.08f).setHemisphere(0xff55657a, 0xff2a2620)
                .setToneMapping(true).setExposure(1.05f)
                .setSky(0xff141d2c, DUSK, 0xffff9a50, 0.35f);
        setPostProcess(new PostProcess().setBloom(0.8f, 0.45f)
                .setGrade(1.1f, 0.85f, 0xff0a121a, 0xfffff0dc).setVignette(0.55f).setGrain(0.03f));
        getCamera().setPerspective(34f, 0.1f, 400f);
        Model sky = new Model(Primitives.sphere(d, 300f, 16, 24, true), new Material(Material.Type.SKYBOX));
        sky.getMaterial().getRenderState().setDepthWrite(false);
        sky.setPosition(0f, 1.35f, 5.2f);
        addModel(sky);
        getCamera().setPosition(0f, 1.35f, 5.2f);
        getCamera().setTarget(offset * 0.55f, 1.0f, 0f);
        Model floor = new Model(Primitives.quad(d, 1f), new Material(Material.Type.LAMBERT).setColor(0xff24282d));
        floor.setRotation(-90f, 0f, 0f);
        floor.setScale(40f, 40f, 1f);
        addModel(floor);
        prop("container", 2.13f, -1.2f, -3.4f, 8f);
        prop("container-small", 2.13f, 3.4f, -3.0f, -14f);
        prop("sandbags", 1.0f, -2.6f, -0.6f, 25f);
        prop("barrel", 1.0f, 2.2f, -1.2f, 0f);
        prop("streetlight", 7f, 3.8f, -4.5f, 200f);
        op = Models.create(d, operator);
        float[] b = Models.bounds(op);
        scale = 1.8f / Math.max(0.01f, b[4] - b[1]);
        op.setScale(scale);
        op.setPosition(offset, 0f, 0f);
        op.setRotation(0f, -18f, 0f);
        op.play(Models.IDLE, true);
        op.addTo(this);
        setMarker(marker);
        // pose now: the first frame can be drawn (and captured) before the
        // first update, and the rig's rest pose is an A with arms out
        op.update(0f);
        holdMarker();
    }

    private void prop(String name, float height, float x, float z, float deg) {
        AnimatedModel m = Models.create(device, name);
        float[] b = Models.bounds(m);
        float s = height / Math.max(0.01f, b[4] - b[1]);
        m.setScale(s);
        m.setRotation(0f, deg, 0f);
        m.setPosition(x, -b[1] * s, z);
        m.addTo(this);
    }

    @Override
    protected void update(double dt) {
        if (op == null) {
            return;
        }
        time += (float) dt;
        op.update((float) dt);
        // A slow drift of the camera, so the stage never looks like a still.
        getCamera().setPosition((float) Math.sin(time * 0.25f) * 0.25f, 1.35f, 5.2f);
        holdMarker();
    }

    private void holdMarker() {
        if (gun != null) {
            float[] t = turn;
            double r = Math.toRadians(op.getRotationY());
            t[0] = (float) Math.cos(r);
            t[2] = (float) -Math.sin(r);
            t[8] = (float) Math.sin(r);
            t[10] = (float) Math.cos(r);
            t[5] = 1f;
            t[15] = 1f;
            grip.place(op, scale, t, op.getX(), op.getY(), op.getZ(), gun, null);
        }
    }
}
// end::splat-operator-stage[]
