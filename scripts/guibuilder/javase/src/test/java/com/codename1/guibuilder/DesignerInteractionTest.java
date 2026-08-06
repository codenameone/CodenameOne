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

package com.codename1.guibuilder;

import com.codename1.guibuilder.model.GuiDocument;
import com.codename1.guibuilder.project.ProjectBinding;
import com.codename1.guibuilder.ui.ComponentPreviewFactory;
import com.codename1.mcp.MCP;
import com.codename1.ui.Component;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.xml.Element;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DesignerInteractionTest {
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    @Test
    void aFixedGuidedWidthNeverFreezesTheThemeDerivedHeight() {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"fixed\" text=\"Primary action\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"300\"/>"
                + "<component type=\"Button\" name=\"natural\" text=\"Primary action\"/>"
                + "</component>");
        Container rendered = (Container) ComponentPreviewFactory.create(document.root(), null, handler());
        Component fixed = rendered.getComponentAt(0);
        Component natural = rendered.getComponentAt(1);

        fixed.getAllStyles().setPadding(24, 24, 10, 10);
        natural.getAllStyles().setPadding(24, 24, 10, 10);

        assertEquals(300, fixed.getPreferredW());
        assertEquals(natural.getPreferredH(), fixed.getPreferredH(),
                "fixing one axis must leave the other axis responsive to UIID/CSS metrics");
        assertFalse(fixed.hasFixedPreferredSize(),
                "the designer must not use deprecated setPreferredW/H or preferredSizeStr state");
    }

    @Test
    void boxSpacerKeepsTheExactSiblingAsItsHitTarget() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\"><component type=\"Label\" name=\"a\"/><component type=\"Label\" name=\"b\"/></component>");
        Element b = document.components().get(2);
        CodenameOneGUIBuilder builder = builder(document);
        Container host = new Container(new LayeredLayout());
        host.setWidth(400);
        host.setHeight(400);
        Label spacer = new Label();
        spacer.putClientProperty("gui.dropTargetElement", b);
        spacer.setX(20);
        spacer.setY(40);
        spacer.setWidth(200);
        spacer.setHeight(24);
        host.add(spacer);

        assertSame(b, builder.elementAt(host, 30, 45),
                "the animated gap must not turn a precise insertion into a parent/end drop");
    }

    @Test
    void aNewPressSelectsAnotherComponentWithoutRequiringAnyDrag() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"first\" text=\"First\" layeredInsets=\"30px auto auto 30px\"/>"
                + "<component type=\"Button\" name=\"second\" text=\"Second\" layeredInsets=\"150px auto auto 300px\"/>"
                + "</component>");
        Element first = document.components().get(1);
        Element second = document.components().get(2);
        Container preview = (Container) render(document, 700, 400);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(700); canvas.setHeight(400); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component firstPreview = componentForElement(preview, first);
        Component secondPreview = componentForElement(preview, second);
        document.select(first);

        builder.handleDesignerPointerPressed(secondPreview.getAbsoluteX() + secondPreview.getWidth() / 2,
                secondPreview.getAbsoluteY() + secondPreview.getHeight() / 2);
        assertSame(second, document.selected(), "selection must change on press, before drag threshold");

        // A missed release must not lock the designer onto the old component. The next press is a
        // new gesture and immediately selects its own hit target.
        builder.handleDesignerPointerPressed(firstPreview.getAbsoluteX() + firstPreview.getWidth() / 2,
                firstPreview.getAbsoluteY() + firstPreview.getHeight() / 2);
        assertSame(first, document.selected());
    }

    @Test
    void modifierPressBuildsAMultiSelectionWithoutMovingAnything() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"first\" text=\"First\" layeredInsets=\"30px auto auto 30px\"/>"
                + "<component type=\"Button\" name=\"second\" text=\"Second\" layeredInsets=\"150px auto auto 300px\"/>"
                + "</component>");
        Element first = document.components().get(1);
        Element second = document.components().get(2);
        Container preview = (Container) render(document, 700, 400);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(700); canvas.setHeight(400); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component firstPreview = componentForElement(preview, first);
        Component secondPreview = componentForElement(preview, second);
        String before = document.toXml();

        builder.handleDesignerPointerPressed(firstPreview.getAbsoluteX() + 10, firstPreview.getAbsoluteY() + 10, false);
        builder.handleDesignerPointerPressed(secondPreview.getAbsoluteX() + 10, secondPreview.getAbsoluteY() + 10, true);

        Map<String, Object> state = builder.mcpState(0);
        assertEquals(java.util.Arrays.asList("first", "second"), state.get("selectedComponents"));
        assertSame(first, document.selected(), "modifier-click must preserve the original sizing reference");
        builder.handleDesignerPointerPressed(firstPreview.getAbsoluteX() + 10, firstPreview.getAbsoluteY() + 10, false);
        assertEquals(java.util.Arrays.asList("first", "second"), builder.mcpState(0).get("selectedComponents"),
                "pressing a selected member must preserve the group so it can become the drag handle");
        assertSame(first, document.selected());
        assertEquals(before, document.toXml(), "selection must never mutate layout data");
    }

    @Test
    void sameWidthStretchesEverySelectionMemberToTheStableReferenceWidth() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"reference\" text=\"Reference\" layeredInsets=\"30px auto auto 30px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"320\"/>"
                + "<component type=\"Button\" name=\"small\" text=\"Small\" layeredInsets=\"150px auto auto 30px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"120\"/>"
                + "<component type=\"Button\" name=\"medium\" text=\"Medium\" layeredInsets=\"270px auto auto 30px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"180\"/>"
                + "</component>");
        Element reference = document.components().get(1);
        Element small = document.components().get(2);
        Element medium = document.components().get(3);
        Container preview = (Container) render(document, 760, 520);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(760); canvas.setHeight(520); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component referencePreview = componentForElement(preview, reference);
        Component smallPreview = componentForElement(preview, small);
        Component mediumPreview = componentForElement(preview, medium);
        String originalXml = document.toXml();

        builder.handleDesignerPointerPressed(referencePreview.getAbsoluteX() + referencePreview.getWidth() / 2,
                referencePreview.getAbsoluteY() + referencePreview.getHeight() / 2, false);
        builder.handleDesignerPointerPressed(smallPreview.getAbsoluteX() + smallPreview.getWidth() / 2,
                smallPreview.getAbsoluteY() + smallPreview.getHeight() / 2, true);
        builder.handleDesignerPointerPressed(mediumPreview.getAbsoluteX() + mediumPreview.getWidth() / 2,
                mediumPreview.getAbsoluteY() + mediumPreview.getHeight() / 2, true);
        assertSame(reference, document.selected());

        builder.applySelectionAction("matchWidth");

        Container matched = (Container) render(document, 760, 520);
        assertEquals(320, componentForElement(matched, reference).getWidth(), 1);
        assertEquals(320, componentForElement(matched, small).getWidth(), 1);
        assertEquals(320, componentForElement(matched, medium).getWidth(), 1);
        assertEquals("reference", small.getAttribute("guidedMatchWidth"));
        assertEquals("reference", medium.getAttribute("guidedMatchWidth"));
        assertTrue(document.undo());
        assertEquals(originalXml, document.toXml(), "same-size must be one atomic undo step");
        assertFalse(document.canUndo());
    }

    @Test
    void groupedGuidedDropTranslatesEverySelectedRectangleAndIsOneUndoStep() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"first\" text=\"First\" layeredInsets=\"30px auto auto 30px\"/>"
                + "<component type=\"Button\" name=\"second\" text=\"Second\" layeredInsets=\"150px auto auto 300px\"/>"
                + "</component>");
        Element first = document.components().get(1);
        Element second = document.components().get(2);
        Container preview = (Container) render(document, 800, 500);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(800); canvas.setHeight(500); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component firstPreview = componentForElement(preview, first);
        Component secondPreview = componentForElement(preview, second);
        int[] firstBefore = bounds(firstPreview);
        int[] secondBefore = bounds(secondPreview);
        String xmlBefore = document.toXml();
        builder.handleDesignerPointerPressed(firstPreview.getAbsoluteX() + 10, firstPreview.getAbsoluteY() + 10, false);
        builder.handleDesignerPointerPressed(secondPreview.getAbsoluteX() + 10, secondPreview.getAbsoluteY() + 10, true);

        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = document.root(); plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = firstPreview.getAbsoluteX() + 80;
        plan.snapY = firstPreview.getAbsoluteY() + 55;
        plan.snapW = firstPreview.getWidth(); plan.snapH = firstPreview.getHeight();
        assertTrue(builder.applyGroupedGuidedDrop(first, plan));

        Container moved = (Container) render(document, 800, 500);
        int[] firstAfter = bounds(componentForElement(moved, first));
        int[] secondAfter = bounds(componentForElement(moved, second));
        assertEquals(firstAfter[0] - firstBefore[0], secondAfter[0] - secondBefore[0]);
        assertEquals(firstAfter[1] - firstBefore[1], secondAfter[1] - secondBefore[1]);
        assertEquals(firstBefore[2], firstAfter[2]);
        assertEquals(firstBefore[3], firstAfter[3]);
        assertEquals(secondBefore[2], secondAfter[2]);
        assertEquals(secondBefore[3], secondAfter[3]);
        assertTrue(document.canUndo());
        assertTrue(document.undo());
        assertEquals(xmlBefore, document.toXml(), "the whole group move must undo atomically");
        assertFalse(document.canUndo(), "group movement must contribute exactly one undo entry");
    }

    @Test
    void groupedDragPreviewShowsEveryMemberAndMatchesTheAtomicCommit() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"first\" text=\"First\" layeredInsets=\"40px auto auto 50px\"/>"
                + "<component type=\"Button\" name=\"second\" text=\"Second\" layeredInsets=\"170px auto auto 310px\"/>"
                + "</component>");
        Element first = document.components().get(1);
        Element second = document.components().get(2);
        Container preview = (Container) render(document, 800, 500);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(800); canvas.setHeight(500); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component firstPreview = componentForElement(preview, first);
        Component secondPreview = componentForElement(preview, second);
        builder.handleDesignerPointerPressed(firstPreview.getAbsoluteX() + 10, firstPreview.getAbsoluteY() + 10, false);
        builder.handleDesignerPointerPressed(secondPreview.getAbsoluteX() + 10, secondPreview.getAbsoluteY() + 10, true);

        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = document.root(); plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = firstPreview.getAbsoluteX() + 95;
        plan.snapY = firstPreview.getAbsoluteY() + 60;
        plan.snapW = firstPreview.getWidth(); plan.snapH = firstPreview.getHeight();
        String originalXml = document.toXml();

        CodenameOneGUIBuilder.GuidedSimulation simulation = builder.simulateGroupedGuidedDrop(first, plan);

        assertNotNull(simulation);
        assertEquals(originalXml, document.toXml(), "a group glass preview must not mutate the document");
        assertEquals(2, simulation.items.size(), "every selected component needs a moving glass rectangle");
        com.codename1.guibuilder.ui.DragGuideOverlay.GlassItem predictedFirst = simulation.items.stream()
                .filter(item -> "first".equals(item.name)).findFirst().orElseThrow();
        com.codename1.guibuilder.ui.DragGuideOverlay.GlassItem predictedSecond = simulation.items.stream()
                .filter(item -> "second".equals(item.name)).findFirst().orElseThrow();
        assertEquals(predictedFirst.newX - predictedFirst.oldX, predictedSecond.newX - predictedSecond.oldX);
        assertEquals(predictedFirst.newY - predictedFirst.oldY, predictedSecond.newY - predictedSecond.oldY);
        assertTrue(predictedFirst.active);
        assertFalse(predictedSecond.active);

        assertTrue(builder.applyGroupedGuidedDrop(first, plan));
        Container committed = (Container) render(document, 800, 500);
        Component committedFirst = componentForElement(committed, first);
        Component committedSecond = componentForElement(committed, second);
        assertEquals(predictedFirst.newX, committedFirst.getAbsoluteX(), 2);
        assertEquals(predictedFirst.newY, committedFirst.getAbsoluteY(), 2);
        assertEquals(predictedSecond.newX, committedSecond.getAbsoluteX(), 2);
        assertEquals(predictedSecond.newY, committedSecond.getAbsoluteY(), 2);
    }

    @Test
    void groupedGuidedDropPreservesInternalBindingsAndLeavesOutsideDependentsInPlace() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Label\" name=\"outsideAnchor\" text=\"Outside\" layeredInsets=\"20px auto auto 20px\"/>"
                + "<component type=\"Button\" name=\"primary\" text=\"Primary\" layeredInsets=\"24px auto auto 0px\" "
                + "guidedReferences=\"outsideAnchor|-|-|outsideAnchor\" guidedReferencePositions=\"1 0 0 0\"/>"
                + "<component type=\"Button\" name=\"secondary\" text=\"Same width\" layeredInsets=\"12px 0px auto 0px\" "
                + "guidedReferences=\"primary|primary|-|primary\" guidedReferencePositions=\"1 0 0 0\" "
                + "guidedHorizontalSize=\"match\" guidedMatchWidth=\"primary\"/>"
                + "<component type=\"Label\" name=\"outsideDependent\" text=\"Stay\" layeredInsets=\"18px auto auto 0px\" "
                + "guidedReferences=\"secondary|-|-|secondary\" guidedReferencePositions=\"1 0 0 0\"/>"
                + "</component>");
        Element primary = document.components().get(2);
        Element secondary = document.components().get(3);
        Element outsideDependent = document.components().get(4);
        Container preview = (Container) render(document, 900, 650);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(900); canvas.setHeight(650); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component primaryPreview = componentForElement(preview, primary);
        Component secondaryPreview = componentForElement(preview, secondary);
        int[] dependentBefore = bounds(componentForElement(preview, outsideDependent));
        builder.handleDesignerPointerPressed(primaryPreview.getAbsoluteX() + 10, primaryPreview.getAbsoluteY() + 10, false);
        builder.handleDesignerPointerPressed(secondaryPreview.getAbsoluteX() + 10, secondaryPreview.getAbsoluteY() + 10, true);

        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = document.root(); plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = primaryPreview.getAbsoluteX() + 120;
        plan.snapY = primaryPreview.getAbsoluteY() + 80;
        plan.snapW = primaryPreview.getWidth(); plan.snapH = primaryPreview.getHeight();
        assertTrue(builder.applyGroupedGuidedDrop(primary, plan));

        assertEquals("primary", secondary.getAttribute("guidedMatchWidth"));
        assertTrue(secondary.getAttribute("guidedReferences").contains("primary"),
                "relationships entirely inside the selection must survive a group move");
        assertFalse(primary.getAttribute("guidedReferences").contains("outsideAnchor"),
                "the group root must detach from positioning references outside the selection");
        Container moved = (Container) render(document, 900, 650);
        assertArrayEquals(dependentBefore, bounds(componentForElement(moved, outsideDependent)),
                "an unselected dependent must be rebased rather than pulled along with the group");
        assertEquals(componentForElement(moved, primary).getWidth(), componentForElement(moved, secondary).getWidth(), 1);
    }

    @Test
    void layeredDropPersistsTheGuidedRectangleAndRerendersThere() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\"><component type=\"Label\" name=\"card\" text=\"Card\" layeredInsets=\"0% 80% 80% 0%\"/></component>");
        Element card = document.components().get(1);
        CodenameOneGUIBuilder builder = builder(document);
        Container canvas = new Container(new LayeredLayout());
        Container parentPreview = new Container(new LayeredLayout());
        parentPreview.putClientProperty("gui.element", document.root());
        parentPreview.setX(50);
        parentPreview.setY(70);
        parentPreview.setWidth(500);
        parentPreview.setHeight(400);
        Label source = new Label("Card");
        source.putClientProperty("gui.element", card);
        source.setWidth(100);
        source.setHeight(80);
        parentPreview.add(source);
        canvas.add(parentPreview);
        set(builder, "canvasHost", canvas);

        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document;
        plan.target = document.root();
        plan.parent = document.root();
        plan.layout = "LayeredLayout";
        plan.valid = true;
        plan.snapX = 150;
        plan.snapY = 150;
        assertTrue(builder.applyDropPlan(card, plan, plan.snapX, plan.snapY));
        assertEquals("78px 298px 238px 98px", card.getAttribute("layeredInsets"), document.toXml());

        Component rendered = ComponentPreviewFactory.create(document.root(), card, handler());
        rendered.setWidth(500);
        rendered.setHeight(400);
        ((Container) rendered).layoutContainer();
        Component renderedCard = ((Container) rendered).getComponentAt(0);
        assertEquals(100, renderedCard.getX(), 2, "rerendered left edge must match the guide");
        assertEquals(80, renderedCard.getY(), 2, "rerendered top edge must match the guide");
        assertEquals(100, renderedCard.getWidth(), 5, "drop must preserve the guided width within percentage rounding");
        assertEquals(80, renderedCard.getHeight(), 5, "drop must preserve the guided height within percentage rounding");
    }

    @Test
    void layeredGuideUsesPointerGrabOffsetAndCommittedBoundsMatchIt() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\"><component type=\"Button\" name=\"card\" text=\"Card\" layeredInsets=\"10px 390px 310px 10px\"/></component>");
        Element card = document.components().get(1);
        CodenameOneGUIBuilder builder = builder(document);
        Container canvas = new Container(new LayeredLayout());
        Container parentPreview = new Container(new LayeredLayout());
        parentPreview.putClientProperty("gui.element", document.root());
        parentPreview.setX(50);
        parentPreview.setY(70);
        parentPreview.setWidth(500);
        parentPreview.setHeight(400);
        Button source = new Button("Card");
        source.putClientProperty("gui.element", card);
        source.setWidth(100);
        source.setHeight(80);
        parentPreview.add(source);
        canvas.add(parentPreview);
        set(builder, "canvasHost", canvas);
        set(builder, "designerGrabOffsetX", 30);
        set(builder, "designerGrabOffsetY", 20);

        CodenameOneGUIBuilder.DropPlan guide = builder.planDrop(card, document.root(), source, 293, 267);
        assertNotNull(guide);
        assertTrue(builder.applyDropPlan(card, guide, 293, 267));
        Component rendered = render(document, 500, 400);
        Component renderedCard = componentForElement((Container) rendered, card);

        assertEquals(guide.snapX - parentPreview.getAbsoluteX(), renderedCard.getX(), 2,
                "the committed left edge must be the guide left edge, not the raw pointer");
        assertEquals(guide.snapY - parentPreview.getAbsoluteY(), renderedCard.getY(), 2);
        assertEquals(source.getWidth(), renderedCard.getWidth(), 2);
        assertEquals(source.getHeight(), renderedCard.getHeight(), 2);
    }

    @Test
    void dockingToTheSurfaceEdgeReflowsWhenTheSurfaceWidthChanges() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"card\" text=\"Card\" layeredInsets=\"40px auto auto 40px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"140\"/>"
                + "</component>");
        Element card = document.components().get(1);
        Container preview = (Container) render(document, 600, 320);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(600); canvas.setHeight(320); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component source = componentForElement(preview, card);
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = document.root(); plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = 600 - source.getWidth(); plan.snapY = source.getAbsoluteY();
        plan.snapW = source.getWidth(); plan.snapH = source.getHeight();
        plan.horizontalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapX, "dock right", null, "parentEnd");
        plan.verticalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapY, null, null, null);
        assertTrue(builder.applyDropPlan(card, plan, plan.snapX, plan.snapY));

        Container narrow = (Container) render(document, 600, 320);
        Container wide = (Container) render(document, 900, 320);
        Component narrowCard = componentForElement(narrow, card);
        Component wideCard = componentForElement(wide, card);
        assertEquals(narrowCard.getWidth(), wideCard.getWidth());
        assertEquals(300, wideCard.getX() - narrowCard.getX(), 1,
                "right docking must reflow by the surface width delta, not preserve an absolute x");
        assertEquals(600, narrowCard.getX() + narrowCard.getWidth(), 4);
        assertEquals(900, wideCard.getX() + wideCard.getWidth(), 4);
    }

    @Test
    void shortGuidedDragOverItsOwnPixelsTargetsTheParentInsteadOfCancelling() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"card\" layeredInsets=\"20px auto auto 20px\"/>"
                + "</component>");
        Element card = document.components().get(1);
        CodenameOneGUIBuilder builder = builder(document);

        assertSame(document.root(), builder.normalizeDesignerDropTarget(card, card));
    }

    @Test
    void guidedReferencesRemainResponsiveAndMatchWidthAcrossCanvasSizes() {
        GuiDocument document = document("<component type=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"anchor\" text=\"Anchor\" layeredInsets=\"20px auto auto 20px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"180\"/>"
                + "<component type=\"Button\" name=\"linked\" text=\"Linked\" layeredInsets=\"70px -40px auto 40px\" guidedHorizontalSize=\"match\" guidedReferences=\"-|anchor|-|anchor\" guidedReferencePositions=\"0 0 0 0\"/>"
                + "</component>");
        Element anchor = document.components().get(1);
        Element linked = document.components().get(2);

        Container narrow = (Container) render(document, 420, 260);
        Component narrowAnchor = componentForElement(narrow, anchor);
        Component narrowLinked = componentForElement(narrow, linked);
        assertEquals(narrowAnchor.getWidth(), narrowLinked.getWidth(), 1);
        assertEquals(40, narrowLinked.getX() - narrowAnchor.getX(), 1,
                "same-size links may retain an independent position offset");

        Container wide = (Container) render(document, 760, 260);
        Component wideAnchor = componentForElement(wide, anchor);
        Component wideLinked = componentForElement(wide, linked);
        assertEquals(wideAnchor.getWidth(), wideLinked.getWidth(), 1,
                "same width must be a relationship, not a one-time pixel copy");
        assertEquals(40, wideLinked.getX() - wideAnchor.getX(), 1);
    }

    @Test
    void baselineConstraintAlignsDifferentTextComponentsExactly() {
        GuiDocument document = document("<component type=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Label\" name=\"caption\" text=\"Caption\" layeredInsets=\"40px auto auto 20px\"/>"
                + "<component type=\"Button\" name=\"action\" text=\"Action\" layeredInsets=\"baseline auto auto 140px\" guidedReferences=\"caption|-|-|-\" guidedReferencePositions=\"0 0 0 0\"/>"
                + "</component>");
        Element caption = document.components().get(1);
        Element action = document.components().get(2);
        Container rendered = (Container) render(document, 500, 220);
        Component renderedCaption = componentForElement(rendered, caption);
        Component renderedAction = componentForElement(rendered, action);

        int captionBaseline = renderedCaption.getY() + renderedCaption.getBaseline(renderedCaption.getWidth(), renderedCaption.getHeight());
        int actionBaseline = renderedAction.getY() + renderedAction.getBaseline(renderedAction.getWidth(), renderedAction.getHeight());
        assertEquals(captionBaseline, actionBaseline, 1);
    }

    @Test
    void centerAnchorsCenterComponentsRatherThanTheirLeftEdges() {
        GuiDocument document = document("<component type=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"centered\" text=\"Centered\" layeredInsets=\"50% auto auto 50%\" guidedHorizontalAnchor=\"0.5\" guidedVerticalAnchor=\"0.5\"/>"
                + "</component>");
        Element centered = document.components().get(1);
        Container rendered = (Container) render(document, 600, 400);
        Component component = componentForElement(rendered, centered);
        assertEquals(300, component.getX() + component.getWidth() / 2, 2);
        assertEquals(200, component.getY() + component.getHeight() / 2, 2);
    }

    @Test
    void selectingAComponentNeverChangesLayoutMetrics() {
        GuiDocument document = document("<component type=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"first\" text=\"First\" layeredInsets=\"20px auto auto 20px\"/>"
                + "<component type=\"Button\" name=\"second\" text=\"Second\" layeredInsets=\"12px auto auto 0px\" guidedReferences=\"first|-|-|first\" guidedReferencePositions=\"1 0 0 0\"/>"
                + "</component>");
        Element first = document.components().get(1);
        Element second = document.components().get(2);
        Container unselected = (Container) render(document, 600, 300);
        Component selectedRender = ComponentPreviewFactory.create(document.root(), first, handler());
        selectedRender.setWidth(600); selectedRender.setHeight(300);
        layoutNested((Container) selectedRender);

        Component beforeFirst = componentForElement(unselected, first);
        Component afterFirst = componentForElement((Container) selectedRender, first);
        Component beforeSecond = componentForElement(unselected, second);
        Component afterSecond = componentForElement((Container) selectedRender, second);
        assertArrayEquals(new int[]{beforeFirst.getX(), beforeFirst.getY(), beforeFirst.getWidth(), beforeFirst.getHeight()},
                new int[]{afterFirst.getX(), afterFirst.getY(), afterFirst.getWidth(), afterFirst.getHeight()});
        assertArrayEquals(new int[]{beforeSecond.getX(), beforeSecond.getY(), beforeSecond.getWidth(), beforeSecond.getHeight()},
                new int[]{afterSecond.getX(), afterSecond.getY(), afterSecond.getWidth(), afterSecond.getHeight()});
    }

    @Test
    void resizeSnapCopiesTheSizeWithoutInventingADurableBinding() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"anchor\" text=\"Anchor\" layeredInsets=\"20px auto auto 20px\" guidedPreferredWidth=\"160\"/>"
                + "<component type=\"Button\" name=\"resized\" text=\"Resize\" layeredInsets=\"80px auto auto 220px\"/>"
                + "</component>");
        Element anchor = document.components().get(1);
        Element resized = document.components().get(2);
        Container preview = (Container) render(document, 600, 300);
        CodenameOneGUIBuilder builder = builder(document);
        Container canvas = new Container(new LayeredLayout());
        canvas.add(preview);
        set(builder, "canvasHost", canvas);
        Component resizedPreview = componentForElement(preview, resized);
        CodenameOneGUIBuilder.ResizePlan plan = new CodenameOneGUIBuilder.ResizePlan(
                resizedPreview.getAbsoluteX(), resizedPreview.getAbsoluteY(),
                componentForElement(preview, anchor).getWidth(), resizedPreview.getHeight());
        plan.matchWidth = anchor;

        builder.commitGuidedResize(resized, preview, plan, 2);

        assertEquals("fixed", resized.getAttribute("guidedHorizontalSize"));
        assertNull(resized.getAttribute("guidedMatchWidth"));
        assertEquals(String.valueOf(componentForElement(preview, anchor).getWidth()),
                resized.getAttribute("guidedPreferredWidth"));
        Container rerendered = (Container) render(document, 800, 300);
        assertEquals(componentForElement(rerendered, anchor).getWidth(),
                componentForElement(rerendered, resized).getWidth(), 1);
    }

    @Test
    void resizingASelectedReferencePreviewsAndResizesTheWholeGroupInUnison() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"reference\" text=\"Reference\" layeredInsets=\"30px auto auto 30px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"220\"/>"
                + "<component type=\"Button\" name=\"second\" text=\"Second\" layeredInsets=\"150px auto auto 30px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"140\"/>"
                + "<component type=\"Button\" name=\"third\" text=\"Third\" layeredInsets=\"270px auto auto 30px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"170\"/>"
                + "</component>");
        Element reference = document.components().get(1);
        Element second = document.components().get(2);
        Element third = document.components().get(3);
        Container preview = (Container) render(document, 760, 520);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(760); canvas.setHeight(520); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component referencePreview = componentForElement(preview, reference);
        Component secondPreview = componentForElement(preview, second);
        Component thirdPreview = componentForElement(preview, third);
        builder.handleDesignerPointerPressed(referencePreview.getAbsoluteX() + referencePreview.getWidth() / 2,
                referencePreview.getAbsoluteY() + referencePreview.getHeight() / 2, false);
        builder.handleDesignerPointerPressed(secondPreview.getAbsoluteX() + secondPreview.getWidth() / 2,
                secondPreview.getAbsoluteY() + secondPreview.getHeight() / 2, true);
        builder.handleDesignerPointerPressed(thirdPreview.getAbsoluteX() + thirdPreview.getWidth() / 2,
                thirdPreview.getAbsoluteY() + thirdPreview.getHeight() / 2, true);
        String originalXml = document.toXml();
        CodenameOneGUIBuilder.ResizePlan plan = new CodenameOneGUIBuilder.ResizePlan(
                referencePreview.getAbsoluteX(), referencePreview.getAbsoluteY(), 360, referencePreview.getHeight());

        CodenameOneGUIBuilder.GuidedSimulation simulation = builder.simulateGuidedResize(
                reference, preview, referencePreview, plan, 2);

        assertNotNull(simulation);
        assertEquals(originalXml, document.toXml());
        assertEquals(3, simulation.items.stream().filter(item -> item.newW == 360).count(),
                "the glass preview must show every selected component at the reference width");
        assertEquals(Component.E_RESIZE_CURSOR, builder.designerResizeCursorAt(
                referencePreview.getAbsoluteX() + referencePreview.getWidth(),
                referencePreview.getAbsoluteY() + referencePreview.getHeight() / 2));
        assertEquals(Component.SE_RESIZE_CURSOR, builder.designerResizeCursorAt(
                referencePreview.getAbsoluteX() + referencePreview.getWidth(),
                referencePreview.getAbsoluteY() + referencePreview.getHeight()));
        assertEquals(Component.DEFAULT_CURSOR, builder.designerResizeCursorAt(
                referencePreview.getAbsoluteX() + referencePreview.getWidth() / 2,
                referencePreview.getAbsoluteY() + referencePreview.getHeight() / 2));

        builder.commitGuidedSelectionResize(reference, preview, plan, 2);
        Container resized = (Container) render(document, 760, 520);
        assertEquals(360, componentForElement(resized, reference).getWidth(), 1);
        assertEquals(360, componentForElement(resized, second).getWidth(), 1);
        assertEquals(360, componentForElement(resized, third).getWidth(), 1);
        assertEquals("reference", second.getAttribute("guidedMatchWidth"));
        assertEquals("reference", third.getAttribute("guidedMatchWidth"));
        assertTrue(document.undo());
        assertEquals(originalXml, document.toXml(), "group resize must undo atomically");
        assertFalse(document.canUndo());
    }

    @Test
    void resizeSimulationShowsDependentCascadeWithoutMutatingTheDocument() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"primary\" text=\"Primary\" layeredInsets=\"20px auto auto 20px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"160\"/>"
                + "<component type=\"Button\" name=\"linked\" text=\"Linked\" layeredInsets=\"16px 0px auto 0px\" guidedHorizontalSize=\"match\" guidedMatchWidth=\"primary\" guidedReferences=\"primary|primary|-|primary\" guidedReferencePositions=\"1 0 0 0\"/>"
                + "</component>");
        Element primary = document.components().get(1);
        Container preview = (Container) render(document, 600, 320);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(600); canvas.setHeight(320); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component primaryPreview = componentForElement(preview, primary);
        String originalXml = document.toXml();
        CodenameOneGUIBuilder.ResizePlan plan = new CodenameOneGUIBuilder.ResizePlan(
                primaryPreview.getAbsoluteX(), primaryPreview.getAbsoluteY(), 240, primaryPreview.getHeight());

        CodenameOneGUIBuilder.GuidedSimulation simulation = builder.simulateGuidedResize(
                primary, preview, primaryPreview, plan, 2);

        assertNotNull(simulation);
        assertEquals(originalXml, document.toXml(), "a live preview must not touch the real document or undo history");
        assertTrue(simulation.changedNames.contains("primary"));
        assertTrue(simulation.changedNames.contains("linked"), "the explicit width dependent must be shown changing");
        assertTrue(simulation.links.stream().anyMatch(link -> "primary".equals(link.from) && "linked".equals(link.to)));
        com.codename1.guibuilder.ui.DragGuideOverlay.GlassItem linked = simulation.items.stream()
                .filter(item -> "linked".equals(item.name)).findFirst().orElseThrow();
        assertEquals(240, linked.newW, 2);
    }

    @Test
    void dragSimulationAndCommitProduceTheSameGuidedRectangle() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"card\" text=\"Card\" layeredInsets=\"20px auto auto 20px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"140\"/>"
                + "</component>");
        Element card = document.components().get(1);
        Container preview = (Container) render(document, 600, 320);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(600); canvas.setHeight(320); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component source = componentForElement(preview, card);
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = document.root(); plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = 280; plan.snapY = 150; plan.snapW = source.getWidth(); plan.snapH = source.getHeight();
        plan.horizontalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapX, null, null, null);
        plan.verticalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapY, null, null, null);
        String originalXml = document.toXml();

        CodenameOneGUIBuilder.GuidedSimulation simulation = builder.simulateGuidedDrop(card, plan, preview, source);
        assertNotNull(simulation);
        assertEquals(originalXml, document.toXml());
        com.codename1.guibuilder.ui.DragGuideOverlay.GlassItem predicted = simulation.items.stream()
                .filter(item -> "card".equals(item.name)).findFirst().orElseThrow();

        assertTrue(builder.applyDropPlan(card, plan, plan.snapX, plan.snapY));
        Container committed = (Container) render(document, 600, 320);
        Component committedCard = componentForElement(committed, card);
        assertEquals(predicted.newX, committedCard.getAbsoluteX(), 2);
        assertEquals(predicted.newY, committedCard.getAbsoluteY(), 2);
        assertEquals(predicted.newW, committedCard.getWidth(), 2);
        assertEquals(predicted.newH, committedCard.getHeight(), 2);
    }

    @Test
    void draggingAComponentIntoFreeSpaceTearsAwayItsIncomingRelationships() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"anchor\" text=\"Anchor\" layeredInsets=\"20px auto auto 20px\" guidedPreferredWidth=\"160\" guidedHorizontalSize=\"fixed\"/>"
                + "<component type=\"Button\" name=\"linked\" text=\"Linked\" layeredInsets=\"16px 0px auto 0px\" guidedHorizontalSize=\"match\" guidedMatchWidth=\"anchor\" guidedReferences=\"anchor|anchor|-|anchor\" guidedReferencePositions=\"1 0 0 0\"/>"
                + "</component>");
        Element linked = document.components().get(2);
        Container preview = (Container) render(document, 700, 400);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(700); canvas.setHeight(400); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component source = componentForElement(preview, linked);
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = document.root(); plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = 420; plan.snapY = 260; plan.snapW = source.getWidth(); plan.snapH = source.getHeight();
        plan.horizontalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapX, null, null, null);
        plan.verticalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapY, null, null, null);
        String before = document.toXml();

        CodenameOneGUIBuilder.GuidedSimulation simulation = builder.simulateGuidedDrop(linked, plan, preview, source);

        assertEquals(before, document.toXml());
        Element simulatedLinked = simulation.document.components().get(2);
        assertEquals("fixed", simulatedLinked.getAttribute("guidedHorizontalSize"));
        assertNull(simulatedLinked.getAttribute("guidedMatchWidth"));
        assertEquals("-|-|-|-", simulatedLinked.getAttribute("guidedReferences"));
        assertTrue(simulation.summary.contains("detaches from anchor"));
        assertTrue(simulation.links.stream().anyMatch(link -> link.detached
                && "anchor".equals(link.from) && "linked".equals(link.to)));

        assertTrue(builder.applyDropPlan(linked, plan, plan.snapX, plan.snapY));
        assertEquals("fixed", linked.getAttribute("guidedHorizontalSize"));
        assertNull(linked.getAttribute("guidedMatchWidth"));
        assertEquals("-|-|-|-", linked.getAttribute("guidedReferences"));
    }

    @Test
    void snappingBackToTheSameReferenceKeepsTheExplicitRelationship() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"anchor\" text=\"Anchor\" layeredInsets=\"20px auto auto 20px\" guidedPreferredWidth=\"160\" guidedHorizontalSize=\"fixed\"/>"
                + "<component type=\"Button\" name=\"linked\" text=\"Linked\" layeredInsets=\"16px 0px auto 0px\" guidedHorizontalSize=\"match\" guidedMatchWidth=\"anchor\" guidedReferences=\"anchor|anchor|-|anchor\" guidedReferencePositions=\"1 0 0 0\"/>"
                + "</component>");
        Element anchor = document.components().get(1);
        Element linked = document.components().get(2);
        Container preview = (Container) render(document, 700, 400);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(700); canvas.setHeight(400); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component source = componentForElement(preview, linked);
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = document.root(); plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = componentForElement(preview, anchor).getAbsoluteX();
        plan.snapY = 220; plan.snapW = source.getWidth(); plan.snapH = source.getHeight();
        plan.horizontalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapX, "align start", anchor, "alignStart");
        plan.verticalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapY, null, null, null);

        CodenameOneGUIBuilder.GuidedSimulation simulation = builder.simulateGuidedDrop(linked, plan, preview, source);
        Element simulatedLinked = simulation.document.components().get(2);
        assertEquals("match", simulatedLinked.getAttribute("guidedHorizontalSize"));
        assertEquals("anchor", simulatedLinked.getAttribute("guidedMatchWidth"));
        assertFalse(simulation.links.stream().anyMatch(link -> link.detached));
    }

    @Test
    void movingSameWidthBelowItsDependentActionRebasesTheCycleWithoutBouncingAnything() throws Exception {
        GuiDocument document = document("<component name=\"Guided\" type=\"Form\" layout=\"LayeredLayout\">"
                + "<component name=\"primary\" type=\"Button\" layeredInsets=\"80px auto auto 40px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"300\" text=\"Primary\"/>"
                + "<component name=\"secondary\" type=\"Button\" layeredInsets=\"12px 0px auto 0px\" guidedReferences=\"primary|primary|-|primary\" guidedReferencePositions=\"1 0 0 0\" guidedHorizontalSize=\"match\" guidedMatchWidth=\"primary\" text=\"Same width\"/>"
                + "<component name=\"baselineLabel\" type=\"Label\" layeredInsets=\"18px auto auto 0px\" guidedReferences=\"secondary|-|-|secondary\" guidedReferencePositions=\"1 0 0 0\" text=\"Baseline aligned\"/>"
                + "<component name=\"baselineAction\" type=\"Button\" layeredInsets=\"baseline auto auto 12px\" guidedReferences=\"baselineLabel|-|-|baselineLabel\" guidedReferencePositions=\"0 0 0 1\" text=\"Action\"/>"
                + "</component>");
        Element secondary = document.components().get(2);
        Element baselineLabel = document.components().get(3);
        Element action = document.components().get(4);
        Container preview = (Container) render(document, 900, 600);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(900); canvas.setHeight(600); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component source = componentForElement(preview, secondary);
        Component labelPreview = componentForElement(preview, baselineLabel);
        Component actionPreview = componentForElement(preview, action);
        int[] oldSource = bounds(source);
        int[] oldLabel = bounds(labelPreview);
        int[] oldAction = bounds(actionPreview);
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = action; plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = actionPreview.getAbsoluteX() + actionPreview.getWidth() - source.getWidth();
        plan.snapY = actionPreview.getAbsoluteY() + actionPreview.getHeight() + 12;
        plan.snapW = source.getWidth(); plan.snapH = source.getHeight();
        plan.horizontalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapX, "align end with Action", action, "alignEnd");
        plan.verticalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapY, "space after Action", action, "after");

        CodenameOneGUIBuilder.GuidedSimulation simulation = builder.simulateGuidedDrop(secondary, plan, preview, source);
        Container proposed = (Container) render(simulation.document, 900, 600);
        Component proposedSecondary = componentForElement(proposed, simulation.document.components().get(2));
        Component proposedLabel = componentForElement(proposed, simulation.document.components().get(3));
        Component proposedAction = componentForElement(proposed, simulation.document.components().get(4));

        assertEquals(oldSource[2], proposedSecondary.getWidth(), 1,
                "a position drag must preserve width: " + simulation.document.toXml());
        assertEquals(oldSource[3], proposedSecondary.getHeight(), 1, "a position drag must preserve height");
        assertEquals(oldAction[0] + oldAction[2], proposedSecondary.getX() + proposedSecondary.getWidth(), 1,
                "the selected right-edge rule must be the rendered result; source margins="
                        + source.getStyle().getMarginLeftNoRTL() + "," + source.getStyle().getMarginRightNoRTL()
                        + " target margins=" + actionPreview.getStyle().getMarginLeftNoRTL() + ","
                        + actionPreview.getStyle().getMarginRightNoRTL());
        assertTrue(proposedSecondary.getY() >= oldAction[1] + oldAction[3],
                "the selected below-Action rule must be the rendered result");
        assertArrayEquals(oldLabel, bounds(proposedLabel), "the baseline label must be rebased in place");
        assertArrayEquals(oldAction, bounds(proposedAction), "Action must not bounce when it becomes the new anchor");
        assertFalse(simulation.changedNames.contains("baselineLabel"));
        assertFalse(simulation.changedNames.contains("baselineAction"));
        assertTrue(simulation.summary.contains("keeps in place baselineLabel from secondary"));
        assertTrue(simulation.links.stream().anyMatch(link -> link.detached
                && "secondary".equals(link.from) && "baselineLabel".equals(link.to)));

        assertTrue(builder.applyDropPlan(secondary, plan, plan.snapX, plan.snapY));
        Container committed = (Container) render(document, 900, 600);
        assertArrayEquals(bounds(proposedSecondary), bounds(componentForElement(committed, secondary)));
        assertArrayEquals(oldLabel, bounds(componentForElement(committed, baselineLabel)));
        assertArrayEquals(oldAction, bounds(componentForElement(committed, action)));

        Container wider = (Container) render(document, 1200, 700);
        Component widerSecondary = componentForElement(wider, secondary);
        Component widerAction = componentForElement(wider, action);
        assertEquals(widerAction.getX() + widerAction.getWidth(),
                widerSecondary.getX() + widerSecondary.getWidth(), 1);
        assertTrue(widerSecondary.getY() >= widerAction.getY() + widerAction.getHeight());
    }

    @Test
    void aligningPrimaryWithItsTransitiveDependentPreservesEveryUnaffectedRenderedRectangle() throws Exception {
        GuiDocument document = guidedChainDocument();
        Element primary = document.components().get(1);
        Element secondary = document.components().get(2);
        Element baselineLabel = document.components().get(3);
        Element baselineAction = document.components().get(4);
        Container preview = (Container) render(document, 900, 700);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(900); canvas.setHeight(700); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);

        Component primaryPreview = componentForElement(preview, primary);
        Component secondaryPreview = componentForElement(preview, secondary);
        Component labelPreview = componentForElement(preview, baselineLabel);
        Component actionPreview = componentForElement(preview, baselineAction);
        // Reproduce the live theme geometry: the relationship currently renders Secondary
        // taller than its nominal preferred height, and the downstream chain follows it.
        secondaryPreview.setHeight(secondaryPreview.getHeight() + 40);
        labelPreview.setY(labelPreview.getY() + 40);
        actionPreview.setY(actionPreview.getY() + 40);
        int[] oldPrimary = bounds(primaryPreview);
        int[] oldSecondary = bounds(secondaryPreview);
        int[] oldLabel = bounds(labelPreview);
        int[] oldAction = bounds(actionPreview);

        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = document.root(); plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = actionPreview.getAbsoluteX();
        plan.snapY = actionPreview.getAbsoluteY() + actionPreview.getHeight() + 80;
        plan.snapW = primaryPreview.getWidth(); plan.snapH = primaryPreview.getHeight();
        plan.horizontalSnap = new CodenameOneGUIBuilder.SnapResult(
                plan.snapX, "align start with baselineAction", baselineAction, "alignStart");
        plan.verticalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapY, null, null, null);

        CodenameOneGUIBuilder.GuidedSimulation simulation =
                builder.simulateGuidedDrop(primary, plan, preview, primaryPreview);
        assertNotNull(simulation);
        Container proposed = (Container) render(simulation.document, 900, 700);
        Component proposedPrimary = componentForElement(proposed, simulation.document.components().get(1));
        Component proposedSecondary = componentForElement(proposed, simulation.document.components().get(2));
        Component proposedLabel = componentForElement(proposed, simulation.document.components().get(3));
        Component proposedAction = componentForElement(proposed, simulation.document.components().get(4));

        assertEquals(oldPrimary[2], proposedPrimary.getWidth(), 1, "position-only drag changed Primary width");
        assertEquals(oldPrimary[3], proposedPrimary.getHeight(), 1, "position-only drag changed Primary height");
        assertArrayEquals(oldSecondary, bounds(proposedSecondary), "cycle rebase resized Secondary");
        assertArrayEquals(oldLabel, bounds(proposedLabel), "Secondary resize moved Baseline Label");
        assertArrayEquals(oldAction, bounds(proposedAction), "Secondary resize moved Action");
        assertFalse(simulation.changedNames.contains("secondary"));
        assertFalse(simulation.changedNames.contains("baselineLabel"));
        assertFalse(simulation.changedNames.contains("baselineAction"));

        assertTrue(builder.applyDropPlan(primary, plan, plan.snapX, plan.snapY));
        Container committed = (Container) render(document, 900, 700);
        assertArrayEquals(bounds(proposedPrimary), bounds(componentForElement(committed, primary)));
        assertArrayEquals(oldSecondary, bounds(componentForElement(committed, secondary)));
        assertArrayEquals(oldLabel, bounds(componentForElement(committed, baselineLabel)));
        assertArrayEquals(oldAction, bounds(componentForElement(committed, baselineAction)));
    }

    @Test
    void movingPrimaryBelowSameWidthKeepsTheEntireDownstreamChainVisible() throws Exception {
        assertPrimarySecondaryPlacementKeepsEverythingVisible(true);
    }

    @Test
    void movingSameWidthBelowPrimaryKeepsTheEntireDownstreamChainVisible() throws Exception {
        assertPrimarySecondaryPlacementKeepsEverythingVisible(false);
    }

    private void assertPrimarySecondaryPlacementKeepsEverythingVisible(boolean movePrimary) throws Exception {
        GuiDocument document = guidedChainDocument();
        Element primary = document.components().get(1);
        Element secondary = document.components().get(2);
        Element dragged = movePrimary ? primary : secondary;
        Element anchor = movePrimary ? secondary : primary;
        Container preview = (Container) render(document, 900, 600);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(900); canvas.setHeight(600); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        Component source = componentForElement(preview, dragged);
        Component anchorPreview = componentForElement(preview, anchor);
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document; plan.target = anchor; plan.parent = document.root();
        plan.layout = "LayeredLayout"; plan.valid = true;
        plan.snapX = anchorPreview.getAbsoluteX();
        plan.snapY = anchorPreview.getAbsoluteY() + anchorPreview.getHeight() + 8;
        plan.snapW = source.getWidth(); plan.snapH = source.getHeight();
        plan.horizontalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapX, "align start", anchor, "alignStart");
        plan.verticalSnap = new CodenameOneGUIBuilder.SnapResult(plan.snapY, "space after", anchor, "after");

        CodenameOneGUIBuilder.GuidedSimulation simulation = builder.simulateGuidedDrop(dragged, plan, preview, source);
        assertNotNull(simulation);
        Container proposed = (Container) render(simulation.document, 900, 600);
        assertAllComponentsVisible(simulation.document, proposed);

        assertTrue(builder.applyDropPlan(dragged, plan, plan.snapX, plan.snapY));
        for (int i = 0; i < document.components().size(); i++) {
            assertEquals(simulation.document.components().get(i).getAttribute("name"),
                    document.components().get(i).getAttribute("name"),
                    "a positional guided drag must not silently change stacking order");
        }
        Container committed = (Container) render(document, 900, 600);
        assertAllComponentsVisible(document, committed);
        for (int i = 1; i < document.components().size(); i++) {
            Component expected = componentForElement(proposed, simulation.document.components().get(i));
            Component actual = componentForElement(committed, document.components().get(i));
            assertArrayEquals(bounds(expected), bounds(actual), document.components().get(i).getAttribute("name"));
        }
    }

    private static GuiDocument guidedChainDocument() {
        return document("<component name=\"Guided\" type=\"Form\" layout=\"LayeredLayout\">"
                + "<component name=\"primary\" type=\"Button\" layeredInsets=\"80px auto auto 40px\" guidedHorizontalSize=\"fixed\" guidedPreferredWidth=\"300\" text=\"Primary action\"/>"
                + "<component name=\"secondary\" type=\"Button\" layeredInsets=\"12px 0px auto 0px\" guidedReferences=\"primary|primary|-|primary\" guidedReferencePositions=\"1 0 0 0\" guidedHorizontalSize=\"match\" guidedMatchWidth=\"primary\" text=\"Same width\"/>"
                + "<component name=\"baselineLabel\" type=\"Label\" layeredInsets=\"18px auto auto 0px\" guidedReferences=\"secondary|-|-|secondary\" guidedReferencePositions=\"1 0 0 0\" text=\"Baseline aligned\"/>"
                + "<component name=\"baselineAction\" type=\"Button\" layeredInsets=\"baseline auto auto 12px\" guidedReferences=\"baselineLabel|-|-|baselineLabel\" guidedReferencePositions=\"0 0 0 1\" text=\"Action\"/>"
                + "</component>");
    }

    private static void assertAllComponentsVisible(GuiDocument document, Container rendered) {
        for (int i = 1; i < document.components().size(); i++) {
            Element element = document.components().get(i);
            Component component = componentForElement(rendered, element);
            assertNotNull(component, element.getAttribute("name") + " must still render");
            assertTrue(component.isVisible(), element.getAttribute("name") + " must remain visible");
            assertTrue(component.getWidth() > 1 && component.getHeight() > 1,
                    element.getAttribute("name") + " collapsed to " + java.util.Arrays.toString(bounds(component)));
            assertTrue(component.getAbsoluteX() + component.getWidth() > rendered.getAbsoluteX()
                            && component.getAbsoluteY() + component.getHeight() > rendered.getAbsoluteY()
                            && component.getAbsoluteX() < rendered.getAbsoluteX() + rendered.getWidth()
                            && component.getAbsoluteY() < rendered.getAbsoluteY() + rendered.getHeight(),
                    element.getAttribute("name") + " moved outside the surface: " + java.util.Arrays.toString(bounds(component)));
        }
    }

    @Test
    void borderReplacementSwapsConstraintsAndSurvivesSerialization() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"BorderLayout\"><component type=\"Button\" name=\"west\" text=\"WEST\" layoutConstraint=\"West\"/><component type=\"SpanLabel\" name=\"center\" text=\"A very long center description that would normally request most of the available width when displaced into an edge, but must never collapse the replacement center component to zero width\" layoutConstraint=\"Center\"/><component type=\"Button\" name=\"east\" text=\"EAST\" layoutConstraint=\"East\"/></component>");
        Element west = document.components().get(1);
        Element center = document.components().get(2);
        CodenameOneGUIBuilder builder = builder(document);
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document;
        plan.target = center;
        plan.parent = document.root();
        plan.occupied = center;
        plan.layout = "BorderLayout";
        plan.constraint = "Center";
        plan.valid = true;

        assertTrue(builder.applyDropPlan(west, plan, 0, 0));
        assertEquals("Center", west.getAttribute("layoutConstraint"), document.toXml());
        assertEquals("West", center.getAttribute("layoutConstraint"));
        GuiDocument reparsed = GuiDocument.parse("Form.gui", document.toXml());
        assertEquals("Center", reparsed.components().get(1).getAttribute("layoutConstraint"));
        assertEquals("West", reparsed.components().get(2).getAttribute("layoutConstraint"));

        Component rendered = ComponentPreviewFactory.create(reparsed.root(), null, handler());
        rendered.setWidth(600);
        rendered.setHeight(400);
        ((Container) rendered).layoutContainer();
        Component renderedWestButton = componentForElement((Container) rendered, reparsed.components().get(1));
        Component renderedFormerCenter = componentForElement((Container) rendered, reparsed.components().get(2));
        assertNotNull(renderedWestButton);
        assertNotNull(renderedFormerCenter);
        assertTrue(renderedWestButton.isVisible() && renderedWestButton.getWidth() > 0 && renderedWestButton.getHeight() > 0,
                "the component moved into Center must not vanish");
        assertTrue(renderedFormerCenter.isVisible() && renderedFormerCenter.getWidth() > 0 && renderedFormerCenter.getHeight() > 0,
                "the displaced Center component must remain visible in West");
        BorderLayout renderedLayout = (BorderLayout) ((Container) rendered).getLayout();
        assertSame(renderedWestButton, renderedLayout.getCenter(), "the dragged WEST component must own CENTER");
        assertSame(renderedFormerCenter, renderedLayout.getWest(), "the displaced CENTER component must own WEST");
        assertTrue(renderedFormerCenter.getX() + renderedFormerCenter.getWidth() <= renderedWestButton.getX(),
                "Border regions must be adjacent, never stacked behind each other");
        assertTrue(renderedWestButton.getWidth() >= 100,
                "a long component displaced into WEST must be capped so CENTER cannot vanish");
    }

    @Test
    void inlineEditorTeardownCommitsModelAndVisiblePreview() throws Exception {
        GuiDocument document = document("<component type=\"Form\"><component type=\"Label\" name=\"title\" text=\"Old\"/></component>");
        Element title = document.components().get(1);
        CodenameOneGUIBuilder builder = builder(document);
        Container canvas = new Container();
        Label preview = new Label("Old");
        preview.putClientProperty("gui.element", title);
        canvas.add(preview);
        set(builder, "canvasHost", canvas);

        builder.commitInlineValue(title, "text", "Edited in place");

        assertEquals("Edited in place", title.getAttribute("text"));
        assertEquals("Edited in place", preview.getText());
        assertTrue(document.isModified());
    }

    @Test
    void everySupportedLayoutUsesAnExplicitPlacementAdapter() throws Exception {
        CodenameOneGUIBuilder builder = builder(document("<component type=\"Form\"/>"));
        assertEquals("BorderPlacementAdapter", builder.placementAdapterName("BorderLayout"));
        assertEquals("LayeredPlacementAdapter", builder.placementAdapterName("LayeredLayout"));
        assertEquals("BoxPlacementAdapter", builder.placementAdapterName("BoxLayout"));
        assertEquals("GridPlacementAdapter", builder.placementAdapterName("GridLayout"));
        assertEquals("TablePlacementAdapter", builder.placementAdapterName("TableLayout"));
        assertEquals("FlowPlacementAdapter", builder.placementAdapterName("FlowLayout"));
    }

    @Test
    void borderCenterCannotConsumeReachableEastAndWestDropBands() {
        assertEquals("West", CodenameOneGUIBuilder.borderEdgeRegion(100, 100, 600, 500, 90, 40, 175, 350));
        assertEquals("East", CodenameOneGUIBuilder.borderEdgeRegion(100, 100, 600, 500, 90, 40, 625, 350));
        assertNull(CodenameOneGUIBuilder.borderEdgeRegion(100, 100, 600, 500, 90, 40, 400, 350));
        assertEquals("North", CodenameOneGUIBuilder.borderEdgeRegion(100, 100, 600, 500, 90, 40, 400, 120));
        assertEquals("South", CodenameOneGUIBuilder.borderEdgeRegion(100, 100, 600, 500, 90, 40, 400, 580));
    }

    @Test
    void numericLayoutValuesRejectNonNumbersAndOverflow() {
        assertNull(CodenameOneGUIBuilder.parseInteger("three"));
        assertNull(CodenameOneGUIBuilder.parseInteger("2.5"));
        assertNull(CodenameOneGUIBuilder.parseInteger(""));
        assertEquals(-1, CodenameOneGUIBuilder.parseInteger("-1"));
        assertEquals(12, CodenameOneGUIBuilder.parseInteger("12"));
    }

    @Test
    void boxLayoutXDropReordersAndRendersInHorizontalOrder() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"BoxLayout\" boxLayoutAxis=\"X\">"
                + "<component type=\"Button\" name=\"a\" text=\"A\"/><component type=\"Button\" name=\"b\" text=\"B\"/>"
                + "<component type=\"Button\" name=\"c\" text=\"C\"/></component>");
        Element a = document.components().get(1);
        Element c = document.components().get(3);
        CodenameOneGUIBuilder builder = builder(document);
        assertTrue(builder.applyDropPlan(c, sequentialPlan(document, document.root(), a, "BoxLayout", false), 0, 0));
        assertSame(c, document.root().getChildAt(0));
        Component rendered = render(document, 600, 180);
        Component renderedC = componentForElement((Container) rendered, c);
        Component renderedA = componentForElement((Container) rendered, a);
        assertTrue(renderedC.getX() < renderedA.getX());
    }

    @Test
    void gridLayoutDropReordersWithoutOverlappingOrLosingCells() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"GridLayout\" gridLayoutRows=\"2\" gridLayoutColumns=\"2\">"
                + "<component type=\"Button\" name=\"a\"/><component type=\"Button\" name=\"b\"/>"
                + "<component type=\"Button\" name=\"c\"/><component type=\"Button\" name=\"d\"/></component>");
        Element a = document.components().get(1);
        Element d = document.components().get(4);
        CodenameOneGUIBuilder builder = builder(document);
        assertTrue(builder.applyDropPlan(d, sequentialPlan(document, document.root(), a, "GridLayout", false), 0, 0));
        Component rendered = render(document, 600, 400);
        java.util.Set<String> cells = new java.util.HashSet<>();
        for (int i = 1; i < document.components().size(); i++) {
            Component component = componentForElement((Container) rendered, document.components().get(i));
            assertTrue(component.getWidth() > 0 && component.getHeight() > 0);
            cells.add(component.getX() + ":" + component.getY());
        }
        assertEquals(4, cells.size());
    }

    @Test
    void tableLayoutDropAssignsUniqueCellsAndExpandsRowsInsteadOfHidingOverflow() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"TableLayout\" tableLayoutRows=\"1\" tableLayoutColumns=\"2\">"
                + "<component type=\"Button\" name=\"a\"/><component type=\"Button\" name=\"b\"/>"
                + "<component type=\"Button\" name=\"c\"/></component>");
        Element a = document.components().get(1);
        Element c = document.components().get(3);
        CodenameOneGUIBuilder builder = builder(document);
        assertTrue(builder.applyDropPlan(c, sequentialPlan(document, document.root(), a, "TableLayout", false), 0, 0));
        assertEquals("2", document.root().getAttribute("tableLayoutRows"));
        java.util.Set<String> modelCells = new java.util.HashSet<>();
        for (int i = 1; i < document.components().size(); i++) {
            Element child = document.components().get(i);
            modelCells.add(child.getAttribute("tableRow") + ":" + child.getAttribute("tableColumn"));
        }
        assertEquals(3, modelCells.size());
        Component rendered = render(document, 600, 400);
        for (int i = 1; i < document.components().size(); i++) {
            Component component = componentForElement((Container) rendered, document.components().get(i));
            assertTrue(component.isVisible() && component.getWidth() > 0 && component.getHeight() > 0);
        }
    }

    @Test
    void tableMoveEarlierReassignsCellsAndChangesRenderedOrder() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"TableLayout\" tableLayoutRows=\"1\" tableLayoutColumns=\"3\">"
                + "<component type=\"Button\" name=\"a\" tableRow=\"0\" tableColumn=\"0\"/>"
                + "<component type=\"Button\" name=\"b\" tableRow=\"0\" tableColumn=\"1\"/>"
                + "<component type=\"Button\" name=\"c\" tableRow=\"0\" tableColumn=\"2\"/></component>");
        Element b = document.components().get(2);
        Element c = document.components().get(3);
        document.select(c);
        CodenameOneGUIBuilder builder = builder(document);

        assertTrue(builder.reorderSelectedInParent(-1));
        assertEquals("1", c.getAttribute("tableColumn"));
        assertEquals("2", b.getAttribute("tableColumn"));
        Component rendered = render(document, 600, 200);
        assertTrue(componentForElement((Container) rendered, c).getX()
                < componentForElement((Container) rendered, b).getX());
    }

    @Test
    void aTableDropLandsInTheCellUnderThePointerAndLeavesEveryOtherCellAlone() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"TableLayout\" tableLayoutRows=\"2\" tableLayoutColumns=\"2\">"
                + "<component type=\"Button\" name=\"a\" text=\"A\" tableRow=\"0\" tableColumn=\"0\"/>"
                + "<component type=\"Button\" name=\"b\" text=\"B\" tableRow=\"0\" tableColumn=\"1\"/>"
                + "<component type=\"Button\" name=\"c\" text=\"C\" tableRow=\"1\" tableColumn=\"0\"/></component>");
        Element a = document.components().get(1);
        Element b = document.components().get(2);
        Element c = document.components().get(3);
        CodenameOneGUIBuilder builder = builder(document);
        Container canvas = canvasFor(document, builder, 400, 400);

        // Drop c into the empty cell (row 1, column 1): the lower right quadrant.
        Component source = componentForElement(canvas, c);
        CodenameOneGUIBuilder.DropPlan plan = builder.planDrop(c, document.root(), source, 300, 300);
        assertNotNull(plan);
        assertTrue(builder.applyDropPlan(c, plan, 300, 300));

        assertEquals("1", c.getAttribute("tableRow"), document.toXml());
        assertEquals("1", c.getAttribute("tableColumn"), document.toXml());
        assertEquals("0:0", a.getAttribute("tableRow") + ":" + a.getAttribute("tableColumn"),
                "a was not dragged, so its cell must not move: " + document.toXml());
        assertEquals("0:1", b.getAttribute("tableRow") + ":" + b.getAttribute("tableColumn"),
                "b was not dragged, so its cell must not move: " + document.toXml());
    }

    @Test
    void aTableDropNeverStacksTwoComponentsInOneCell() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"TableLayout\" tableLayoutRows=\"2\" tableLayoutColumns=\"2\">"
                + "<component type=\"Button\" name=\"a\" text=\"A\" tableRow=\"0\" tableColumn=\"0\"/>"
                + "<component type=\"Button\" name=\"b\" text=\"B\" tableRow=\"0\" tableColumn=\"1\"/>"
                + "<component type=\"Button\" name=\"c\" text=\"C\" tableRow=\"1\" tableColumn=\"0\"/>"
                + "<component type=\"Button\" name=\"d\" text=\"D\" tableRow=\"1\" tableColumn=\"1\"/></component>");
        Element a = document.components().get(1);
        Element d = document.components().get(4);
        CodenameOneGUIBuilder builder = builder(document);
        Container canvas = canvasFor(document, builder, 400, 400);

        // Drop d onto a's occupied cell: the two must swap, not overlap.
        Component source = componentForElement(canvas, d);
        CodenameOneGUIBuilder.DropPlan plan = builder.planDrop(d, document.root(), source, 100, 100);
        assertNotNull(plan);
        assertTrue(builder.applyDropPlan(d, plan, 100, 100));

        assertEquals("0:0", d.getAttribute("tableRow") + ":" + d.getAttribute("tableColumn"), document.toXml());
        assertEquals("1:1", a.getAttribute("tableRow") + ":" + a.getAttribute("tableColumn"),
                "the displaced component must take the vacated cell: " + document.toXml());
        assertEquals(4, distinctTableCells(document).size(), document.toXml());
    }

    @Test
    void aComponentDraggedBetweenTwoNestedContainersReparentsAndRenders() throws Exception {
        GuiDocument document = nestedColumnsDocument();
        Element leftGrid = document.components().get(2);
        Element rightActions = document.components().get(7);
        Element nestedA = document.components().get(3);
        Element nestedAction = document.components().get(9);
        CodenameOneGUIBuilder builder = builder(document);
        Container canvas = canvasFor(document, builder, 700, 500);

        Component target = componentForElement(canvas, nestedAction);
        assertNotNull(target, "the demo must render the right hand column");
        Component source = componentForElement(canvas, nestedA);
        int x = target.getAbsoluteX() + target.getWidth() / 2;
        int y = target.getAbsoluteY() + target.getHeight() - 2;

        CodenameOneGUIBuilder.DropPlan plan = builder.planDrop(nestedA, builder.elementAt(canvas, x, y), source, x, y);
        assertNotNull(plan, "hovering the far column must produce a drop plan");
        assertSame(rightActions, plan.parent, "the drop must target the hovered container, not the drag source's");
        assertTrue(builder.applyDropPlan(nestedA, plan, x, y), document.toXml());

        assertSame(rightActions, document.parentOf(nestedA), document.toXml());
        assertEquals(3, document.componentsIn(leftGrid).size(),
                "the source container must actually give the child up: " + document.toXml());
        Container reRendered = canvasFor(document, builder, 700, 500);
        Component moved = componentForElement(reRendered, nestedA);
        assertNotNull(moved, "the moved component must still render: " + document.toXml());
        assertTrue(moved.getWidth() > 0 && moved.getHeight() > 0,
                "the moved component must not collapse to zero size: " + document.toXml());
        Component column = componentForElement(reRendered, rightActions);
        assertTrue(moved.getAbsoluteX() >= column.getAbsoluteX()
                        && moved.getAbsoluteX() < column.getAbsoluteX() + column.getWidth(),
                "the moved component must render inside its new parent");
    }

    @Test
    void aNestedContainerCanBeDraggedWholeWithoutLosingItsChildren() throws Exception {
        GuiDocument document = nestedColumnsDocument();
        Element contentColumns = document.components().get(1);
        Element leftGrid = document.components().get(2);
        Element rightActions = document.components().get(7);
        CodenameOneGUIBuilder builder = builder(document);
        Container canvas = canvasFor(document, builder, 700, 500);

        Component source = componentForElement(canvas, leftGrid);
        Component target = componentForElement(canvas, rightActions);
        int x = target.getAbsoluteX() + target.getWidth() - 2;
        int y = target.getAbsoluteY() + target.getHeight() / 2;
        CodenameOneGUIBuilder.DropPlan plan = builder.planDrop(leftGrid, rightActions, source, x, y);
        assertNotNull(plan);
        assertTrue(builder.applyDropPlan(leftGrid, plan, x, y), document.toXml());

        assertEquals(4, document.componentsIn(leftGrid).size(),
                "moving a container must carry its whole subtree: " + document.toXml());
        assertNotSame(contentColumns, document.parentOf(leftGrid));
        Container reRendered = canvasFor(document, builder, 700, 500);
        for (Element child : document.componentsIn(leftGrid)) {
            Component preview = componentForElement(reRendered, child);
            assertNotNull(preview, child.getAttribute("name") + " vanished: " + document.toXml());
        }
    }

    @Test
    void undoAfterAReparentingDragRestoresTheExactTreeAndOrder() throws Exception {
        GuiDocument document = nestedColumnsDocument();
        String before = document.toXml();
        Element leftGrid = document.components().get(2);
        Element rightActions = document.components().get(7);
        Element nestedA = document.components().get(3);
        CodenameOneGUIBuilder builder = builder(document);
        canvasFor(document, builder, 700, 500);

        assertTrue(builder.applyDropPlan(nestedA,
                sequentialPlan(document, rightActions, rightActions, "BoxLayout", true), 0, 0));
        assertSame(rightActions, document.parentOf(nestedA));
        assertTrue(document.undo());

        assertEquals(before, document.toXml(), "undo must restore the document byte for byte");
        List<Element> grid = GuiDocument.componentsIn(findByName(document, "leftGrid"));
        assertEquals(List.of("nestedA", "nestedB", "nestedC", "nestedD"), namesOf(grid),
                "undo must restore sibling order, not just membership");
    }

    @Test
    void repeatedEditAndUndoCyclesNeverDriftTheDocument() throws Exception {
        GuiDocument document = nestedColumnsDocument();
        String before = document.toXml();
        CodenameOneGUIBuilder builder = builder(document);
        for (int i = 0; i < 4; i++) {
            canvasFor(document, builder, 700, 500);
            Element rightActions = findByName(document, "rightActions");
            Element nestedA = findByName(document, "nestedA");
            assertTrue(builder.applyDropPlan(nestedA,
                    sequentialPlan(document, rightActions, rightActions, "BoxLayout", true), 0, 0),
                    "cycle " + i + " could not move the component");
            assertTrue(document.undo(), "cycle " + i + " could not undo");
            assertEquals(before, document.toXml(), "the document drifted on cycle " + i);
        }
    }

    private static Element findByName(GuiDocument document, String name) {
        for (Element element : document.components()) {
            if (name.equals(element.getAttribute("name"))) return element;
        }
        return null;
    }

    private static List<String> namesOf(List<Element> elements) {
        List<String> names = new java.util.ArrayList<>();
        for (Element element : elements) names.add(element.getAttribute("name"));
        return names;
    }

    private static String value(Element element, String attribute, String fallback) {
        String raw = element == null ? null : element.getAttribute(attribute);
        return raw == null ? fallback : raw;
    }

    /**
     * Indented exactly like the .gui files the editor loads from disk. The indentation is not
     * cosmetic: XMLParser turns it into whitespace text nodes, so a document read from a project
     * has interleaved non-component children that a document built from one long line does not.
     */
    private static GuiDocument nestedColumnsDocument() {
        return document("<component type=\"Form\" layout=\"BorderLayout\" title=\"Nested hierarchy\" name=\"NestedLayoutsForm\">\n"
                + "    <component type=\"Container\" name=\"contentColumns\" layout=\"BoxLayout\" boxLayoutAxis=\"X\" layoutConstraint=\"Center\">\n"
                + "        <component type=\"Container\" name=\"leftGrid\" layout=\"GridLayout\" gridLayoutRows=\"2\" gridLayoutColumns=\"2\">\n"
                + "            <component type=\"Button\" name=\"nestedA\" text=\"A\" />\n"
                + "            <component type=\"Button\" name=\"nestedB\" text=\"B\" />\n"
                + "            <component type=\"Button\" name=\"nestedC\" text=\"C\" />\n"
                + "            <component type=\"Button\" name=\"nestedD\" text=\"D\" />\n"
                + "        </component>\n"
                + "        <component type=\"Container\" name=\"rightActions\" layout=\"BoxLayout\" boxLayoutAxis=\"Y\">\n"
                + "            <component type=\"Label\" name=\"nestedDescription\" text=\"Drag components between both nested containers.\" />\n"
                + "            <component type=\"Button\" name=\"nestedAction\" text=\"Action\" />\n"
                + "        </component>\n"
                + "    </component>\n"
                + "</component>");
    }

    private static java.util.Set<String> distinctTableCells(GuiDocument document) {
        java.util.Set<String> cells = new java.util.HashSet<>();
        for (int i = 1; i < document.components().size(); i++) {
            Element child = document.components().get(i);
            cells.add(child.getAttribute("tableRow") + ":" + child.getAttribute("tableColumn"));
        }
        return cells;
    }

    private static Container canvasFor(GuiDocument document, CodenameOneGUIBuilder builder, int width, int height)
            throws Exception {
        Container preview = (Container) render(document, width, height);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(width);
        canvas.setHeight(height);
        canvas.add(preview);
        canvas.layoutContainer();
        layoutNested(canvas);
        set(builder, "canvasHost", canvas);
        return canvas;
    }

    @Test
    void nestedGridPlacementMovesOnlyTheSelectedBranch() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"BoxLayout\"><component type=\"Container\" name=\"grid\" layout=\"GridLayout\" gridLayoutRows=\"1\" gridLayoutColumns=\"2\">"
                + "<component type=\"Label\" name=\"inside\"/></component><component type=\"Button\" name=\"outside\"/></component>");
        Element grid = document.components().get(1);
        Element inside = document.components().get(2);
        Element outside = document.components().get(3);
        CodenameOneGUIBuilder builder = builder(document);
        assertTrue(builder.applyDropPlan(outside, sequentialPlan(document, grid, inside, "GridLayout", true), 0, 0));
        assertSame(grid, document.parentOf(outside));
        assertSame(document.root(), document.parentOf(grid));
        Component rendered = render(document, 600, 400);
        Component outsidePreview = componentForElement((Container) rendered, outside);
        assertNotNull(outsidePreview);
        assertTrue(outsidePreview.getWidth() > 0 && outsidePreview.getHeight() > 0);
    }

    private static CodenameOneGUIBuilder builder(GuiDocument document) throws Exception {
        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        set(builder, "document", document);
        return builder;
    }

    @Test
    void staleElementFromAnotherFormCannotEnterTheActiveDocument() throws Exception {
        GuiDocument oldForm = document("<component type=\"Form\" name=\"Old\"><component type=\"Button\" name=\"stale\"/></component>");
        GuiDocument activeForm = document("<component type=\"Form\" name=\"Active\" layout=\"BoxLayout\"><component type=\"Label\" name=\"only\"/></component>");
        Element stale = oldForm.components().get(1);
        Element target = activeForm.components().get(1);
        CodenameOneGUIBuilder builder = builder(activeForm);
        String before = activeForm.toXml();
        CodenameOneGUIBuilder.DropPlan forged = sequentialPlan(activeForm, activeForm.root(), target, "BoxLayout", true);

        assertFalse(builder.applyDropPlan(stale, forged, 0, 0));
        assertEquals(before, activeForm.toXml());
        assertFalse(builder.isActiveDocumentElement(stale));
        assertTrue(builder.isActiveDocumentElement(target));
    }

    @Test
    void boxXPreviewIsActuallyScrollableAndDragAtRightEdgeAdvancesIt() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"BoxLayout\" boxLayoutAxis=\"X\" scrollableX=\"true\">"
                + "<component type=\"Button\" name=\"a\" text=\"A very wide first button\"/>"
                + "<component type=\"Button\" name=\"b\" text=\"A very wide second button\"/>"
                + "<component type=\"Button\" name=\"c\" text=\"A very wide third button\"/>"
                + "<component type=\"Button\" name=\"d\" text=\"A very wide fourth button\"/></component>");
        Container preview = (Container) render(document, 280, 160);
        assertTrue(preview.isScrollableX(), "the sample must overflow horizontally");
        Container canvas = new Container(new BorderLayout());
        canvas.setWidth(280);
        canvas.setHeight(160);
        canvas.add(BorderLayout.CENTER, preview);
        canvas.layoutContainer();
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);

        builder.autoScrollDuringDrag(preview.getAbsoluteX() + preview.getWidth() - 2,
                preview.getAbsoluteY() + preview.getHeight() / 2);

        assertTrue(preview.getScrollX() > 0, "holding a drag at the right edge must reveal later siblings");
    }

    @Test
    void previewComponentsExposeStableAccessibilityIdentifiers() {
        GuiDocument document = document("<component type=\"Form\" name=\"LoginForm\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"primaryAction\" text=\"Sign in\"/>"
                + "</component>");
        Container preview = (Container) render(document, 600, 400);
        Component button = componentForElement(preview, document.components().get(1));

        assertNotNull(button);
        assertEquals("guibuilder.preview.LoginForm", preview.getSemantics().getIdentifier());
        assertEquals("guibuilder.preview.primaryAction", button.getSemantics().getIdentifier());
        assertTrue(button.getSemantics().getLabel().contains("primaryAction"));
    }

    @Test
    void mcpPublishesStructuredDesignerStateAndLiveActions() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Guided\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"primary\" text=\"Primary action\" layeredInsets=\"40px auto auto 40px\"/>"
                + "</component>");
        Container preview = (Container) render(document, 640, 420);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(640); canvas.setHeight(420);
        canvas.getAllStyles().setPadding(11, 13, 17, 19);
        canvas.add(preview);
        ((LayeredLayout) canvas.getLayout()).setInsets(preview, "0 0 0 0");
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "binding", ProjectBinding.parse("projectDir=/tmp/project\nguiDir=/tmp/project/src/main/guibuilder"));
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        set(builder, "guiFiles", new java.util.ArrayList<String>(java.util.Arrays.asList("Form.gui")));
        com.codename1.guibuilder.ui.DragGuideOverlay overlay =
                new com.codename1.guibuilder.ui.DragGuideOverlay();
        canvas.add(overlay);
        ((LayeredLayout) canvas.getLayout()).setInsets(overlay, "0 0 0 0");
        canvas.layoutContainer();
        document.select(document.components().get(1));
        Component selectedPreview = componentForElement(preview, document.components().get(1));
        overlay.showSelection(selectedPreview);
        int[] paintCoordinates = overlay.selectionPaintLocalBounds();
        assertEquals(selectedPreview.getAbsoluteX() - canvas.getAbsoluteX(), paintCoordinates[0],
                "selection X must be expressed in the parent Graphics coordinate system");
        assertEquals(selectedPreview.getAbsoluteY() - canvas.getAbsoluteY(), paintCoordinates[1],
                "selection Y must be expressed in the parent Graphics coordinate system");
        assertTrue(overlay.getX() > 0 || overlay.getY() > 0,
                "the regression needs a padded parent so overlay-local and paint coordinates differ");
        set(builder, "dragGuideOverlay", overlay);
        GuiBuilderMcpController controller = new GuiBuilderMcpController(builder);
        set(builder, "mcpController", controller);
        controller.register();
        controller.record("test_action", java.util.Collections.<String, Object>singletonMap("detail", "live"));

        Map<String, Object> state = builder.mcpState(controller.latestSequence());
        assertEquals("Form", state.get("activeForm"));
        assertEquals("primary", state.get("selected"));
        List<?> components = (List<?>) state.get("components");
        assertEquals(2, components.size());
        assertEquals("primary", ((Map<?, ?>) components.get(1)).get("name"));
        assertEquals("guibuilder.preview.primary",
                ((Map<?, ?>) components.get(1)).get("accessibilityIdentifier"));
        assertEquals(((Map<?, ?>) components.get(1)).get("bounds"), state.get("selectionPaintBounds"),
                "MCP must expose the exact absolute pixels painted by the selection overlay");

        String tools = MCP.getServer().handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
        assertTrue(tools.contains("guibuilder_state"));
        assertTrue(tools.contains("guibuilder_drag"));
        assertTrue(tools.contains("guibuilder_actions"));
        String actions = MCP.getServer().handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{"
                        + "\"name\":\"guibuilder_actions\",\"arguments\":{\"afterSequence\":0}}}");
        assertTrue(actions.contains("test_action"));
        assertTrue(actions.contains("live"));
    }

    @Test
    void mcpAdditiveSelectionUsesJsonBooleansAndPublishesTheWholeGroup() throws Exception {
        GuiDocument document = document("<component type=\"Form\" name=\"Guided\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"first\" text=\"First\"/>"
                + "<component type=\"Button\" name=\"second\" text=\"Second\"/></component>");
        Container preview = (Container) render(document, 640, 420);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(640); canvas.setHeight(420); canvas.add(preview);
        CodenameOneGUIBuilder builder = builder(document);
        set(builder, "canvasHost", canvas);
        set(builder, "previewRoot", preview);
        GuiBuilderMcpController controller = new GuiBuilderMcpController(builder);
        set(builder, "mcpController", controller);
        controller.register();

        MCP.getServer().handleMessage("{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"tools/call\",\"params\":{"
                + "\"name\":\"guibuilder_select\",\"arguments\":{\"component\":\"first\"}}}");
        String additive = MCP.getServer().handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"tools/call\",\"params\":{"
                        + "\"name\":\"guibuilder_select\",\"arguments\":{\"component\":\"second\",\"additive\":true}}}");

        assertTrue(additive.contains("\\\"selectedComponents\\\":[\\\"first\\\",\\\"second\\\"]"), additive);
    }

    private static CodenameOneGUIBuilder.DropPlan sequentialPlan(GuiDocument document, Element parent, Element target, String layout, boolean after) {
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document;
        plan.parent = parent;
        plan.target = target;
        plan.layout = layout;
        plan.after = after;
        plan.valid = true;
        return plan;
    }

    private static Component render(GuiDocument document, int width, int height) {
        Component rendered = ComponentPreviewFactory.create(document.root(), null, handler());
        rendered.setWidth(width);
        rendered.setHeight(height);
        ((Container) rendered).layoutContainer();
        layoutNested((Container) rendered);
        return rendered;
    }

    private static void layoutNested(Container container) {
        container.layoutContainer();
        for (int i = 0; i < container.getComponentCount(); i++) {
            if (container.getComponentAt(i) instanceof Container) layoutNested((Container) container.getComponentAt(i));
        }
    }

    private static GuiDocument document(String xml) {
        return GuiDocument.parse("Form.gui", xml);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = CodenameOneGUIBuilder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static ComponentPreviewFactory.SelectionHandler handler() {
        return new ComponentPreviewFactory.SelectionHandler() {
            public void selected(Element element) { }
            public void dragPressed(Element element, Component source, int x, int y) { }
            public boolean isDragActive() { return false; }
            public void editContent(Element element) { }
        };
    }

    private static Component componentForElement(Container root, Element element) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            if (component.getClientProperty("gui.element") == element) return component;
            if (component instanceof Container) {
                Component nested = componentForElement((Container) component, element);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static int[] bounds(Component component) {
        return new int[]{component.getAbsoluteX(), component.getAbsoluteY(), component.getWidth(), component.getHeight()};
    }
}
