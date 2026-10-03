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
package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;
import android.util.AttributeSet;

import com.codename1.androidcompat.runtime.CompiledAttributeSet;
import com.codename1.androidcompat.runtime.XmlNode;

/// Loads animations, layout animations and interpolators from compiled
/// `res/anim` and `res/interpolator` XML, constructing each element's class
/// directly (no reflection), and offers the stock slide animations.
public class AnimationUtils {

    private AnimationUtils() {
    }

    /// The time base animations run on.
    public static long currentAnimationTimeMillis() {
        return SystemClock.uptimeMillis();
    }

    public static Animation loadAnimation(Context context, int id) {
        XmlNode node = xml(context, id, "animation");
        return createAnimationFromXml(context, node, null);
    }

    private static XmlNode xml(Context context, int id, String what) {
        try {
            return context.getResources().getXmlNode(id);
        } catch (Resources.NotFoundException e) {
            Resources.NotFoundException rnf = new Resources.NotFoundException(
                    "Can't load " + what + " resource ID #0x" + Integer.toHexString(id), e);
            throw rnf;
        }
    }

    private static Animation createAnimationFromXml(Context c, XmlNode node, AnimationSet parent) {
        AttributeSet attrs = new CompiledAttributeSet(node);
        String name = node.tag;
        Animation anim;
        if (name.equals("set")) {
            AnimationSet set = new AnimationSet(c, attrs);
            for (XmlNode child : node.children) {
                createAnimationFromXml(c, child, set);
            }
            anim = set;
        } else if (name.equals("alpha")) {
            anim = new AlphaAnimation(c, attrs);
        } else if (name.equals("scale")) {
            anim = new ScaleAnimation(c, attrs);
        } else if (name.equals("rotate")) {
            anim = new RotateAnimation(c, attrs);
        } else if (name.equals("translate")) {
            anim = new TranslateAnimation(c, attrs);
        } else {
            throw new RuntimeException("Unknown animation name: " + name);
        }
        if (parent != null) {
            parent.addAnimation(anim);
        }
        return anim;
    }

    public static LayoutAnimationController loadLayoutAnimation(Context context, int id) {
        XmlNode node = xml(context, id, "layout animation");
        String name = node.tag;
        if (name.equals("layoutAnimation")) {
            return new LayoutAnimationController(context, new CompiledAttributeSet(node));
        }
        throw new RuntimeException("Unknown layout animation name: " + name);
    }

    public static Animation makeInAnimation(Context c, boolean fromLeft) {
        Animation a = slide(c, fromLeft ? -0.5f : 0.5f, 0f, 0f, 1f);
        a.setInterpolator(new DecelerateInterpolator());
        a.setStartTime(currentAnimationTimeMillis());
        return a;
    }

    public static Animation makeOutAnimation(Context c, boolean toRight) {
        Animation a = slide(c, 0f, toRight ? 0.5f : -0.5f, 1f, 0f);
        a.setInterpolator(new AccelerateInterpolator());
        a.setStartTime(currentAnimationTimeMillis());
        return a;
    }

    public static Animation makeInChildBottomAnimation(Context c) {
        AnimationSet set = new AnimationSet(true);
        TranslateAnimation t = new TranslateAnimation(Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0,
                Animation.RELATIVE_TO_PARENT, 1f, Animation.ABSOLUTE, 0);
        t.setDuration(duration(c, android.R.integer.config_mediumAnimTime, 400));
        set.addAnimation(t);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(duration(c, android.R.integer.config_mediumAnimTime, 400));
        set.addAnimation(fade);
        set.setInterpolator(new AccelerateInterpolator());
        set.setStartTime(currentAnimationTimeMillis());
        return set;
    }

    /// The framework's slide_in/slide_out: a move by a fraction of the
    /// parent's width, faded at the same time.
    private static Animation slide(Context c, float fromX, float toX, float fromAlpha, float toAlpha) {
        long d = duration(c, android.R.integer.config_mediumAnimTime, 400);
        AnimationSet set = new AnimationSet(true);
        TranslateAnimation t = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, fromX,
                Animation.RELATIVE_TO_PARENT, toX, Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0);
        t.setDuration(d);
        set.addAnimation(t);
        AlphaAnimation fade = new AlphaAnimation(fromAlpha, toAlpha);
        fade.setDuration(d);
        set.addAnimation(fade);
        return set;
    }

    private static long duration(Context c, int id, long def) {
        try {
            return c.getResources().getInteger(id);
        } catch (RuntimeException e) {
            return def;
        }
    }

    public static Interpolator loadInterpolator(Context context, int id) {
        XmlNode node = xml(context, id, "interpolator");
        return createInterpolatorFromXml(context, node);
    }

    private static Interpolator createInterpolatorFromXml(Context c, XmlNode node) {
        AttributeSet attrs = new CompiledAttributeSet(node);
        String name = node.tag;
        if (name.equals("linearInterpolator")) {
            return new LinearInterpolator();
        } else if (name.equals("accelerateInterpolator")) {
            return new AccelerateInterpolator(c, attrs);
        } else if (name.equals("decelerateInterpolator")) {
            return new DecelerateInterpolator(c, attrs);
        } else if (name.equals("accelerateDecelerateInterpolator")) {
            return new AccelerateDecelerateInterpolator();
        } else if (name.equals("cycleInterpolator")) {
            return new CycleInterpolator(c, attrs);
        } else if (name.equals("anticipateInterpolator")) {
            return new AnticipateInterpolator(c, attrs);
        } else if (name.equals("overshootInterpolator")) {
            return new OvershootInterpolator(c, attrs);
        } else if (name.equals("anticipateOvershootInterpolator")) {
            return new AnticipateOvershootInterpolator(c, attrs);
        } else if (name.equals("bounceInterpolator")) {
            return new BounceInterpolator();
        } else if (name.equals("pathInterpolator")) {
            return new PathInterpolator(c, attrs);
        }
        throw new RuntimeException("Unknown interpolator name: " + name);
    }
}
