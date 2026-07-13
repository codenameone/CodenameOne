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
package com.codenameone.developerguide.snippets;

import com.codename1.ui.Button;
import com.codename1.ui.AccessibilityColorVisionDeficiency;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityAssertions;
import com.codename1.ui.accessibility.AccessibilityCheckedState;
import com.codename1.ui.accessibility.AccessibilityChildProvider;
import com.codename1.ui.accessibility.AccessibilityCollectionInfo;
import com.codename1.ui.accessibility.AccessibilityCollectionItemInfo;
import com.codename1.ui.accessibility.AccessibilityGrouping;
import com.codename1.ui.accessibility.AccessibilityInspector;
import com.codename1.ui.accessibility.AccessibilityLiveRegion;
import com.codename1.ui.accessibility.AccessibilityNode;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityRange;
import com.codename1.ui.accessibility.AccessibilityRole;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Compiled source snippets for the accessibility semantics guide chapter. */
public class AccessibilitySemanticsSnippets {

    public void configureButton() {
        // tag::accessibility-semantics-button[]
        Button save = new Button("Save");
        save.getSemantics()
                .setHint("Saves the edited profile")
                .setIdentifier("profile-save");
        // end::accessibility-semantics-button[]
    }

    public void configureRoles(Component wifiSwitch, Label title) {
        // tag::accessibility-semantics-roles[]
        wifiSwitch.getSemantics()
                .setRole(AccessibilityRole.SWITCH)
                .setLabel("Wi-Fi")
                .setChecked(AccessibilityCheckedState.CHECKED)
                .setEnabled(Boolean.TRUE)
                .setHint("Double tap to turn Wi-Fi off");

        title.getSemantics()
                .setHeadingLevel(2)
                .setIdentifier("network-heading");
        // end::accessibility-semantics-roles[]
    }

    public void configureRange(Component rating) {
        // tag::accessibility-semantics-range[]
        rating.getSemantics()
                .setRole(AccessibilityRole.SLIDER)
                .setLabel("Rating")
                .setRange(new AccessibilityRange(0, 5, 3, 1, "3 out of 5"));
        // end::accessibility-semantics-range[]
    }

    public void configureAction(Component card) {
        // tag::accessibility-semantics-action[]
        card.getSemantics().addAction(new AccessibilityAction(
                "archive",
                "Archive message",
                new AccessibilityAction.Handler() {
                    public boolean perform(Component component, Object argument) {
                        archiveMessage();
                        return true;
                    }
                }
        ));
        // end::accessibility-semantics-action[]
    }

    public void configureGrouping(Component summaryCard) {
        // tag::accessibility-semantics-grouping[]
        summaryCard.getSemantics()
                .setRole(AccessibilityRole.GENERIC)
                .setGrouping(AccessibilityGrouping.MERGE_DESCENDANTS);
        // end::accessibility-semantics-grouping[]
    }

    public void configureVirtualChildren(Component chart, final List<ChartPoint> points) {
        // tag::accessibility-semantics-virtual[]
        chart.getSemantics().setChildProvider(new AccessibilityChildProvider() {
            public List<AccessibilityNode> getAccessibilityChildren(Component owner) {
                List<AccessibilityNode> result = new ArrayList<AccessibilityNode>();
                for (ChartPoint point : points) {
                    AccessibilityNode node = new AccessibilityNode("point-" + point.getId());
                    node.setRole(AccessibilityRole.IMAGE)
                            .setLabel(point.getLabel())
                            .setValue(point.getFormattedValue())
                            .setBounds(point.getBounds());
                    result.add(node);
                }
                return result;
            }
        });
        // end::accessibility-semantics-virtual[]
    }

    public void configureTraversal(Component cancel, Component continueButton, Component help) {
        // tag::accessibility-semantics-traversal[]
        cancel.getSemantics().setSortKey(1);
        continueButton.getSemantics().setSortKey(2);
        help.getSemantics().setTraversalAfter(continueButton);
        // end::accessibility-semantics-traversal[]
    }

    public void configureLiveRegion(Label status) {
        // tag::accessibility-semantics-live[]
        status.getSemantics()
                .setRole(AccessibilityRole.ALERT)
                .setLiveRegion(AccessibilityLiveRegion.POLITE);
        status.setText("Upload complete");
        // end::accessibility-semantics-live[]
    }

    public void configureCollection(Component grid, Component cell) {
        // tag::accessibility-semantics-collection[]
        grid.getSemantics().setCollectionInfo(
                new AccessibilityCollectionInfo(20, 3, false,
                        AccessibilityCollectionInfo.SELECTION_MULTIPLE));

        cell.getSemantics().setCollectionItemInfo(
                new AccessibilityCollectionItemInfo(
                        4, 1, 2, 1, 5, 20, 1, false));
        // end::accessibility-semantics-collection[]
    }

    public String inspect(Form myForm, int screenX, int screenY) {
        // tag::accessibility-semantics-inspection[]
        AccessibilityTreeSnapshot tree =
                AccessibilityInspector.snapshot(myForm);

        AccessibilityAssertions.assertNoErrors(tree);
        AccessibilityAssertions.assertNoUnlabeledInteractiveNodes(tree);

        String diagnosticJson = tree.toJson();
        AccessibilityNodeSnapshot hit = tree.getNodeAt(screenX, screenY);
        // end::accessibility-semantics-inspection[]
        return diagnosticJson + (hit == null ? "" : hit.getLabel());
    }

    public void adaptToAccessibilityPreferences(Component animatedChart, Label connectionStatus) {
        // tag::accessibility-preferences[]
        if (CN.isHighContrastEnabled()) {
            animatedChart.setUIID("HighContrastChart");
        }

        // Important state always uses text or an icon as well as color. The
        // preference is an extra signal, not permission to use color alone.
        if (CN.isDifferentiateWithoutColorEnabled()
                || CN.getColorVisionDeficiency() != AccessibilityColorVisionDeficiency.NONE) {
            connectionStatus.setText("Disconnected: action required");
        }

        if (CN.isReduceMotionEnabled()) {
            animatedChart.putClientProperty("animate", Boolean.FALSE);
        }
        if (CN.isReduceTransparencyEnabled()) {
            animatedChart.setUIID("OpaqueChart");
        }
        // end::accessibility-preferences[]
    }

    public void repairCustomControl(Component iconOnlyButton) {
        // tag::accessibility-repair[]
        iconOnlyButton.getSemantics()
                .setRole(AccessibilityRole.BUTTON)
                .setLabel("Delete message")
                .setHint("Removes this message from the conversation")
                .setIdentifier("message-delete")
                .addAction(new AccessibilityAction(
                        AccessibilityAction.ACTIVATE,
                        null,
                        new AccessibilityAction.Handler() {
                            public boolean perform(Component component, Object argument) {
                                deleteMessage();
                                return true;
                            }
                        }));
        // end::accessibility-repair[]
    }

    public void assertAccessibleForm(Form form) {
        // tag::accessibility-regression-assertions[]
        AccessibilityTreeSnapshot tree = AccessibilityInspector.snapshot(form);
        AccessibilityAssertions.assertNoErrors(tree);
        AccessibilityAssertions.assertNoUnlabeledInteractiveNodes(tree);

        AccessibilityNodeSnapshot save = tree.getNodeByIdentifier("profile-save");
        assert save.getRole() == AccessibilityRole.BUTTON;
        assert save.getAction(AccessibilityAction.ACTIVATE) != null;
        // end::accessibility-regression-assertions[]
    }

    private void archiveMessage() {
    }

    private void deleteMessage() {
    }

    /** Minimal model contract used by the virtual-descendant example. */
    public interface ChartPoint {
        String getId();
        String getLabel();
        String getFormattedValue();
        Rectangle getBounds();
    }
}
