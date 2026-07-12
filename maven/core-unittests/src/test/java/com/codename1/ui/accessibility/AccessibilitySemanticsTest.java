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
package com.codename1.ui.accessibility;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.TextField;
import com.codename1.ui.list.DefaultListModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccessibilitySemanticsTest extends UITestBase {

    @FormTest
    void infersRolesStatesValuesAndActionsForBuiltInControls() {
        Form form = form();
        Button button = place(form, new Button("Save"), 10, 10, 100, 30);
        CheckBox checkbox = place(form, new CheckBox("Remember me"), 10, 50, 140, 30);
        checkbox.setSelected(true);
        Slider slider = place(form, new Slider(), 10, 90, 200, 30);
        slider.setEditable(true);
        slider.setMinValue(10);
        slider.setMaxValue(50);
        slider.setProgress(30);
        TextField field = place(form, new TextField("hello"), 10, 130, 200, 35);

        AccessibilityTreeSnapshot tree = AccessibilityInspector.snapshot(form);
        AccessibilityNodeSnapshot buttonNode = find(tree, button);
        AccessibilityNodeSnapshot checkNode = find(tree, checkbox);
        AccessibilityNodeSnapshot sliderNode = find(tree, slider);
        AccessibilityNodeSnapshot fieldNode = find(tree, field);

        assertEquals(AccessibilityRole.BUTTON, buttonNode.getRole());
        assertEquals("Save", buttonNode.getLabel());
        assertNotNull(buttonNode.getAction(AccessibilityAction.ACTIVATE));
        assertEquals(AccessibilityRole.CHECKBOX, checkNode.getRole());
        assertEquals(AccessibilityCheckedState.CHECKED, checkNode.getChecked());
        assertEquals(30d, sliderNode.getRange().getCurrent());
        assertNotNull(sliderNode.getAction(AccessibilityAction.INCREMENT));
        assertEquals(AccessibilityRole.TEXT_FIELD, fieldNode.getRole());
        assertEquals("hello", fieldNode.getLabel());
        assertNotNull(fieldNode.getAction(AccessibilityAction.SET_TEXT));
    }

    @FormTest
    void explicitSemanticsOverrideInferredValuesAndLegacyTextMapsToLabel() {
        Form form = form();
        Button button = place(form, new Button("Visual"), 10, 10, 100, 30);
        button.setAccessibilityText("Legacy");
        assertEquals("Legacy", button.getSemantics().getLabel());
        button.getSemantics().setLabel("Semantic").setRole(AccessibilityRole.SWITCH)
                .setChecked(AccessibilityCheckedState.MIXED).setHint("Changes mode");

        AccessibilityNodeSnapshot node = find(AccessibilityInspector.snapshot(form), button);
        assertEquals("Semantic", node.getLabel());
        assertEquals("Semantic", button.getAccessibilityText());
        assertEquals(AccessibilityRole.SWITCH, node.getRole());
        assertEquals(AccessibilityCheckedState.MIXED, node.getChecked());
        assertEquals("Changes mode", node.getHint());
    }

    @FormTest
    void groupingCanMergeOrPromoteDescendants() {
        Form form = form();
        Container merged = place(form, new Container(), 0, 0, 200, 100);
        Label first = place(merged, new Label("First"), 0, 0, 100, 30);
        place(merged, new Label("Second"), 0, 35, 100, 30);
        merged.getSemantics().setRole(AccessibilityRole.GENERIC)
                .setGrouping(AccessibilityGrouping.MERGE_DESCENDANTS);

        AccessibilityTreeSnapshot tree = AccessibilityInspector.snapshot(form);
        AccessibilityNodeSnapshot mergedNode = find(tree, merged);
        assertTrue(mergedNode.getLabel().contains("First"));
        assertTrue(mergedNode.getLabel().contains("Second"));
        assertTrue(mergedNode.getChildIds().isEmpty());
        assertNull(findOrNull(tree, first));

        merged.getSemantics().setGrouping(AccessibilityGrouping.EXCLUDE);
        tree = AccessibilityInspector.snapshot(form);
        assertNull(findOrNull(tree, merged));
        assertNotNull(findOrNull(tree, first));
    }

    @FormTest
    void sortKeysProduceExplicitTraversalOrder() {
        Form form = form();
        Button second = place(form, new Button("Second"), 0, 0, 100, 30);
        Button first = place(form, new Button("First"), 0, 40, 100, 30);
        second.getSemantics().setSortKey(2);
        first.getSemantics().setSortKey(1);

        AccessibilityTreeSnapshot tree = AccessibilityInspector.snapshot(form);
        AccessibilityNodeSnapshot formNode = find(tree, form);
        int firstIndex = formNode.getChildIds().indexOf(Long.valueOf(find(tree, first).getId()));
        int secondIndex = formNode.getChildIds().indexOf(Long.valueOf(find(tree, second).getId()));
        assertTrue(firstIndex >= 0 && secondIndex >= 0 && firstIndex < secondIndex);
    }

    @FormTest
    void rendererBackedListsExposeStableVirtualCollectionItems() {
        Form form = form();
        com.codename1.ui.List<String> list = new com.codename1.ui.List<String>(
                new DefaultListModel<String>("Alpha", "Beta", "Gamma"));
        place(form, list, 0, 0, 200, 160);
        list.setSelectedIndex(1);

        AccessibilityTreeSnapshot first = AccessibilityInspector.snapshot(form);
        AccessibilityNodeSnapshot listNode = find(first, list);
        assertEquals(3, listNode.getChildIds().size());
        AccessibilityNodeSnapshot beta = first.getNode(listNode.getChildIds().get(1).longValue());
        assertTrue(beta.getLabel().contains("Beta"));
        assertEquals(Boolean.TRUE, beta.getSelected());
        assertEquals(2, beta.getCollectionItemInfo().getPositionInSet());
        long stableId = beta.getId();

        list.setSelectedIndex(2);
        AccessibilityTreeSnapshot second = AccessibilityInspector.snapshot(form);
        AccessibilityNodeSnapshot secondList = find(second, list);
        assertEquals(stableId, secondList.getChildIds().get(1).longValue());
        assertEquals(Boolean.FALSE, second.getNode(stableId).getSelected());
    }

    @FormTest
    void auditsInvalidRangesAndUnlabeledCustomActions() {
        Form form = form();
        Component custom = place(form, new Label(), 0, 0, 100, 30);
        custom.getSemantics().setRole(AccessibilityRole.SLIDER)
                .setRange(new AccessibilityRange(10, 5, 20, -1))
                .addAction(new AccessibilityAction("custom", null,
                        new AccessibilityAction.Handler() {
                            public boolean perform(Component component, Object argument) { return true; }
                        }));

        List<AccessibilityIssue> issues = AccessibilityAssertions.audit(AccessibilityInspector.snapshot(form));
        assertTrue(hasIssue(issues, "invalid-range"));
        assertTrue(hasIssue(issues, "unlabeled-custom-action"));
        assertThrows(AssertionError.class,
                () -> AccessibilityAssertions.assertNoErrors(AccessibilityInspector.snapshot(form)));
    }

    private Form form() {
        Form form = new Form("Accessible form");
        form.setWidth(400);
        form.setHeight(600);
        form.getContentPane().setWidth(400);
        form.getContentPane().setHeight(600);
        return form;
    }

    private <T extends Component> T place(Container parent, T component, int x, int y, int w, int h) {
        parent.add(component);
        component.setX(x);
        component.setY(y);
        component.setWidth(w);
        component.setHeight(h);
        return component;
    }

    private AccessibilityNodeSnapshot find(AccessibilityTreeSnapshot tree, Component component) {
        AccessibilityNodeSnapshot node = findOrNull(tree, component);
        assertNotNull(node, "No semantic node for " + component);
        return node;
    }

    private AccessibilityNodeSnapshot findOrNull(AccessibilityTreeSnapshot tree, Component component) {
        for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
            if (node.getComponent() == component && node.getVirtualKey() == null) return node;
        }
        return null;
    }

    private boolean hasIssue(List<AccessibilityIssue> issues, String code) {
        for (AccessibilityIssue issue : issues) if (code.equals(issue.getCode())) return true;
        return false;
    }
}
