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
package androidx.constraintlayout.widget;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.View;

import com.codename1.androidcompat.testing.AndroidTestSupport;

import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/// ConstraintLayout's placement on a 1000x1000 parent, for the cases real
/// layouts lean on. The expected positions are what ConstraintLayout computes
/// on Android for the same constraints.
public class ConstraintLayoutTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private Context context;
    private ConstraintLayout parent;

    @Before
    public void setUp() {
        context = AndroidTestSupport.context();
        parent = new ConstraintLayout(context);
    }

    private View child(int width, int height, ConstraintLayout.LayoutParams lp) {
        View v = new View(context);
        v.setId(View.generateViewId());
        v.setMinimumWidth(width);
        v.setMinimumHeight(height);
        parent.addView(v, lp);
        return v;
    }

    private static ConstraintLayout.LayoutParams params(int width, int height) {
        return new ConstraintLayout.LayoutParams(width, height);
    }

    private void layout(int width, int height) {
        parent.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, parent.getMeasuredWidth(), parent.getMeasuredHeight());
    }

    private static void centerIn(ConstraintLayout.LayoutParams lp) {
        lp.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
    }

    @Test
    public void centersAChildConstrainedToBothEdges() {
        ConstraintLayout.LayoutParams lp = params(200, 100);
        centerIn(lp);
        View v = child(200, 100, lp);
        layout(1000, 1000);
        assertEquals(400, v.getLeft());
        assertEquals(450, v.getTop());
        assertEquals(200, v.getWidth());
    }

    @Test
    public void biasMovesTheChildBetweenItsConstraints() {
        ConstraintLayout.LayoutParams lp = params(200, 100);
        centerIn(lp);
        lp.horizontalBias = 0.3f;
        View v = child(200, 100, lp);
        layout(1000, 1000);
        assertEquals(240, v.getLeft());
    }

    @Test
    public void matchConstraintFillsBetweenMargins() {
        ConstraintLayout.LayoutParams lp = params(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, 100);
        centerIn(lp);
        lp.leftMargin = 50;
        lp.rightMargin = 50;
        View v = child(10, 100, lp);
        layout(1000, 1000);
        assertEquals(50, v.getLeft());
        assertEquals(900, v.getWidth());
    }

    @Test
    public void dimensionRatioDerivesTheHeight() {
        ConstraintLayout.LayoutParams lp = params(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        lp.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.dimensionRatio = "16:9";
        View v = child(10, 10, lp);
        layout(1000, 1000);
        assertEquals(1000, v.getWidth());
        assertEquals(563, v.getHeight(), 1);
        assertEquals(0, v.getTop());
    }

    private View[] chain(int style, float[] weights, int width) {
        View[] views = new View[3];
        ConstraintLayout.LayoutParams[] lps = new ConstraintLayout.LayoutParams[3];
        for (int i = 0; i < 3; i++) {
            lps[i] = params(width, 100);
            lps[i].topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            if (weights != null) {
                lps[i].horizontalWeight = weights[i];
            }
            views[i] = child(width < 0 ? 10 : width, 100, lps[i]);
        }
        lps[0].horizontalChainStyle = style;
        lps[0].leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        lps[0].rightToLeft = views[1].getId();
        lps[1].leftToRight = views[0].getId();
        lps[1].rightToLeft = views[2].getId();
        lps[2].leftToRight = views[1].getId();
        lps[2].rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        layout(1000, 1000);
        return views;
    }

    @Test
    public void spreadChainSharesTheFreeSpace() {
        View[] v = chain(ConstraintLayout.LayoutParams.CHAIN_SPREAD, null, 100);
        assertEquals(175, v[0].getLeft());
        assertEquals(450, v[1].getLeft());
        assertEquals(725, v[2].getLeft());
    }

    @Test
    public void spreadInsideChainPinsTheEnds() {
        View[] v = chain(ConstraintLayout.LayoutParams.CHAIN_SPREAD_INSIDE, null, 100);
        assertEquals(0, v[0].getLeft());
        assertEquals(450, v[1].getLeft());
        assertEquals(900, v[2].getLeft());
    }

    @Test
    public void packedChainCentersTheGroup() {
        View[] v = chain(ConstraintLayout.LayoutParams.CHAIN_PACKED, null, 100);
        assertEquals(350, v[0].getLeft());
        assertEquals(450, v[1].getLeft());
        assertEquals(550, v[2].getLeft());
    }

    @Test
    public void weightedChainDividesTheWidth() {
        View[] v = chain(ConstraintLayout.LayoutParams.CHAIN_SPREAD, new float[] {1, 1, 2},
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        assertEquals(250, v[0].getWidth());
        assertEquals(250, v[1].getWidth());
        assertEquals(500, v[2].getWidth());
        assertEquals(500, v[2].getLeft());
    }

    @Test
    public void guidelinePercentPositionsItsDependents() {
        Guideline g = new Guideline(context);
        g.setId(View.generateViewId());
        ConstraintLayout.LayoutParams glp = params(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        glp.orientation = ConstraintLayout.LayoutParams.VERTICAL;
        glp.guidePercent = 0.3f;
        parent.addView(g, glp);
        ConstraintLayout.LayoutParams lp = params(100, 100);
        lp.leftToLeft = g.getId();
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        View v = child(100, 100, lp);
        layout(1000, 1000);
        assertEquals(300, v.getLeft());
    }

    @Test
    public void barrierFollowsTheWidestReferencedView() {
        ConstraintLayout.LayoutParams a = params(300, 100);
        a.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        a.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        View va = child(300, 100, a);
        ConstraintLayout.LayoutParams b = params(500, 100);
        b.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        b.topToBottom = va.getId();
        View vb = child(500, 100, b);
        Barrier barrier = new Barrier(context);
        barrier.setId(View.generateViewId());
        barrier.setType(Barrier.END);
        barrier.setReferencedIds(new int[] {va.getId(), vb.getId()});
        parent.addView(barrier, params(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT));
        ConstraintLayout.LayoutParams c = params(100, 100);
        c.leftToRight = barrier.getId();
        c.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        View vc = child(100, 100, c);
        layout(1000, 1000);
        assertEquals(500, vc.getLeft());
    }

    @Test
    public void goneMarginAppliesWhenTheTargetIsGone() {
        ConstraintLayout.LayoutParams a = params(300, 100);
        a.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        a.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        View va = child(300, 100, a);
        va.setVisibility(View.GONE);
        ConstraintLayout.LayoutParams b = params(100, 100);
        b.leftToRight = va.getId();
        b.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        b.leftMargin = 10;
        b.goneLeftMargin = 77;
        View vb = child(100, 100, b);
        layout(1000, 1000);
        assertEquals(77, vb.getLeft());
    }

    @Test
    public void wrapContentParentWrapsItsChildren() {
        ConstraintLayout.LayoutParams a = params(300, 100);
        a.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        a.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        View va = child(300, 100, a);
        ConstraintLayout.LayoutParams b = params(200, 150);
        b.leftToRight = va.getId();
        b.topToBottom = va.getId();
        b.leftMargin = 20;
        child(200, 150, b);
        parent.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST));
        assertEquals(520, parent.getMeasuredWidth());
        assertEquals(250, parent.getMeasuredHeight());
    }

    @Test
    public void constraintSetMovesAViewWhenApplied() {
        ConstraintLayout.LayoutParams lp = params(100, 100);
        lp.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        View v = child(100, 100, lp);
        layout(1000, 1000);
        assertEquals(0, v.getLeft());
        ConstraintSet set = new ConstraintSet();
        set.clone(parent);
        set.clear(v.getId(), ConstraintSet.LEFT);
        set.connect(v.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
        set.applyTo(parent);
        layout(1000, 1000);
        assertEquals(900, v.getLeft());
    }

    @Test
    public void groupSetsTheVisibilityOfItsMembers() {
        ConstraintLayout.LayoutParams a = params(100, 100);
        a.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        a.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        View va = child(100, 100, a);
        ConstraintLayout.LayoutParams b = params(100, 100);
        b.leftToRight = va.getId();
        b.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        View vb = child(100, 100, b);
        Group group = new Group(context);
        group.setId(View.generateViewId());
        group.setReferencedIds(new int[] {va.getId(), vb.getId()});
        group.setVisibility(View.GONE);
        parent.addView(group, params(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT));
        // A group applies its visibility when it is attached, as in an
        // activity's layout, and whenever it changes afterwards.
        parent.dispatchAttachedToWindow(true);
        layout(1000, 1000);
        assertEquals(View.GONE, va.getVisibility());
        assertEquals(View.GONE, vb.getVisibility());
        group.setVisibility(View.VISIBLE);
        assertEquals(View.VISIBLE, va.getVisibility());
    }
}
