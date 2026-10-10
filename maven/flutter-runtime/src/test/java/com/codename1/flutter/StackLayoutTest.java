/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter;

import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Positioned;
import com.codename1.flutter.widgets.Stack;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Stack/Positioned layout math per Flutter's RenderStack, driven headless.
 */
class StackLayoutTest {

    private RenderElement mountAndLayout(Widget root, BoxConstraints constraints) {
        BuildOwner owner = new BuildOwner();
        RenderHost host = new RenderHost();
        FlutterUI.mount(root, host, owner);
        RenderElement r = host.rootRenderElement();
        r.layout(constraints);
        r.position(0, 0);
        return r;
    }

    @Test
    void stackSizesToBiggestNonPositionedChildUnderLooseConstraints() {
        Stack stack = new Stack();
        stack.children(DartList.of((Widget) new ProbeBox(100, 50), new ProbeBox(60, 120)));

        RenderElement root = mountAndLayout(stack, BoxConstraints.loose(400, 600));
        assertEquals(new Size(100, 120), root.size());
    }

    @Test
    void stackExpandsUnderTightConstraints() {
        Stack stack = new Stack();
        stack.children(DartList.of((Widget) new ProbeBox(10, 10)));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        assertEquals(new Size(400, 600), root.size());
    }

    @Test
    void expandFitForcesNonPositionedChildrenToFillTheStack() {
        // The gallery's study card: a Stack(fit: expand) over an image that also declares
        // its own height. Flutter's tight constraints win, so the image covers the card;
        // loosening instead left it at its own size, framed by the surface behind it.
        Stack stack = new Stack();
        stack.fit(StackFit.expand);
        stack.children(DartList.of((Widget) new ProbeBox(100, 240)));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(296, 208));
        assertEquals(new Size(296, 208), root.size());
        assertEquals(new Size(296, 208), root.renderChildren().get(0).size(),
                "expand must override the child's own size");
    }

    @Test
    void looseFitLeavesTheChildItsOwnSize() {
        // Same tree, default fit - the contrast that makes the case above meaningful.
        Stack stack = new Stack();
        stack.children(DartList.of((Widget) new ProbeBox(100, 240)));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(296, 208));
        assertEquals(new Size(296, 208), root.size());
        assertEquals(new Size(100, 208), root.renderChildren().get(0).size());
    }

    @Test
    void passthroughFitHandsTheChildTheStacksOwnConstraints() {
        Stack stack = new Stack();
        stack.fit(StackFit.passthrough);
        stack.children(DartList.of((Widget) new ProbeBox(100, 50)));

        // Min constraints reach the child untouched, unlike loose.
        RenderElement root = mountAndLayout(stack, new BoxConstraints(200, 400, 120, 600));
        assertEquals(new Size(200, 120), root.renderChildren().get(0).size());
    }

    @Test
    void expandUnderAnUnboundedAxisDegradesToLooseRatherThanGoingInfinite() {
        Stack stack = new Stack();
        stack.fit(StackFit.expand);
        stack.children(DartList.of((Widget) new ProbeBox(100, 50)));

        // Unbounded height: there is no biggest to be tight to.
        RenderElement root = mountAndLayout(stack,
                new BoxConstraints(0, 400, 0, Double.POSITIVE_INFINITY));
        assertEquals(new Size(400, 50), root.renderChildren().get(0).size());
        assertEquals(new Size(400, 50), root.size());
    }

    @Test
    void positionedChildrenIgnoreTheFit() {
        // fit only governs NON-positioned children - a Positioned child still resolves
        // against its own insets.
        Stack stack = new Stack();
        stack.fit(StackFit.expand);
        Positioned p = new Positioned();
        p.left(10.0);
        p.top(20.0);
        p.child(new ProbeBox(30, 30));
        stack.children(DartList.of((Widget) p, new ProbeBox(50, 50)));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        RenderElement positioned = root.renderChildren().get(0);
        assertEquals(new Size(30, 30), positioned.size());
        assertEquals(10, positioned.x());
        assertEquals(20, positioned.y());
        assertEquals(new Size(400, 600), root.renderChildren().get(1).size());
    }

    @Test
    void stackWithOnlyPositionedChildrenExpandsToBoundedAxes() {
        Stack stack = new Stack();
        Positioned p = new Positioned();
        p.left(10.0);
        p.top(10.0);
        p.child(new ProbeBox(30, 30));
        stack.children(DartList.of((Widget) p));

        RenderElement root = mountAndLayout(stack, BoxConstraints.loose(400, 600));
        assertEquals(new Size(400, 600), root.size());
    }

    @Test
    void nonPositionedChildrenArePlacedByStackAlignment() {
        Stack stack = new Stack();
        stack.alignment(Alignment.center);
        stack.children(DartList.of((Widget) new ProbeBox(100, 50)));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        RenderElement kid = root.renderChildren().get(0);
        assertEquals(150, kid.x());
        assertEquals(275, kid.y());

        Stack bottomRight = new Stack();
        bottomRight.alignment(Alignment.bottomRight);
        bottomRight.children(DartList.of((Widget) new ProbeBox(100, 50)));
        RenderElement root2 = mountAndLayout(bottomRight, BoxConstraints.tight(400, 600));
        RenderElement kid2 = root2.renderChildren().get(0);
        assertEquals(300, kid2.x());
        assertEquals(550, kid2.y());
    }

    @Test
    void positionedLeftTopInsetsPlaceTheChild() {
        Stack stack = new Stack();
        Positioned p = new Positioned();
        p.left(10.0);
        p.top(20.0);
        p.child(new ProbeBox(30, 40));
        stack.children(DartList.of((Widget) new ProbeBox(400, 600), p));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        RenderElement positioned = root.renderChildren().get(1);
        assertEquals(10, positioned.x());
        assertEquals(20, positioned.y());
        assertEquals(new Size(30, 40), positioned.size());
    }

    @Test
    void positionedRightBottomInsetsResolveAgainstStackBounds() {
        Stack stack = new Stack();
        Positioned p = new Positioned();
        p.right(10.0);
        p.bottom(5.0);
        p.child(new ProbeBox(30, 40));
        stack.children(DartList.of((Widget) p));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        RenderElement positioned = root.renderChildren().get(0);
        assertEquals(360, positioned.x());
        assertEquals(555, positioned.y());
    }

    @Test
    void opposingInsetsTightenTheAxis() {
        Stack stack = new Stack();
        Positioned p = new Positioned();
        p.left(10.0);
        p.right(10.0);
        p.top(0.0);
        p.child(new ProbeBox(30, 40));
        stack.children(DartList.of((Widget) p));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        RenderElement positioned = root.renderChildren().get(0);
        // left+right force the width to 400 - 10 - 10.
        assertEquals(380.0, positioned.size().width());
        assertEquals(10, positioned.x());
        assertEquals(0, positioned.y());
    }

    @Test
    void explicitExtentPlusOneInsetPlacesFromTheOppositeEdge() {
        Stack stack = new Stack();
        Positioned p = new Positioned();
        p.width(50.0);
        p.right(0.0);
        p.top(10.0);
        p.height(20.0);
        p.child(new ProbeBox(5, 5));
        stack.children(DartList.of((Widget) p));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        RenderElement positioned = root.renderChildren().get(0);
        assertEquals(new Size(50, 20), positioned.size());
        assertEquals(350, positioned.x());
        assertEquals(10, positioned.y());
    }

    @Test
    void unresolvedPositionedAxisFallsBackToAlignment() {
        Stack stack = new Stack();
        stack.alignment(Alignment.center);
        Positioned p = new Positioned();
        p.left(10.0);
        p.child(new ProbeBox(30, 40));
        stack.children(DartList.of((Widget) p));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        RenderElement positioned = root.renderChildren().get(0);
        assertEquals(10, positioned.x());
        // vertical axis unresolved: centered
        assertEquals(280, positioned.y());
    }

    @Test
    void zOrderMatchesChildOrder() {
        Stack stack = new Stack();
        stack.children(DartList.of((Widget) new ProbeBox(50, 50), new ProbeBox(60, 60), new ProbeBox(70, 70)));

        RenderElement root = mountAndLayout(stack, BoxConstraints.tight(400, 600));
        List<RenderElement> kids = root.renderChildren();
        // renderChildren is tree order == paint order (later on top)
        assertEquals(50.0, kids.get(0).size().width());
        assertEquals(60.0, kids.get(1).size().width());
        assertEquals(70.0, kids.get(2).size().width());
    }
}
